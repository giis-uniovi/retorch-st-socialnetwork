#ifndef SOCIAL_NETWORK_MICROSERVICES_USERHANDLER_H
#define SOCIAL_NETWORK_MICROSERVICES_USERHANDLER_H

#include <bson/bson.h>
#include <libmemcached/memcached.h>
#include <libmemcached/util.h>
#include <mongoc.h>

#include <format>
#include <iomanip>
#include <iostream>
#include <jwt/jwt.hpp>
#include <nlohmann/json.hpp>
#include <random>
#include <string>

#include "../../gen-cpp/SocialGraphService.h"
#include "../../gen-cpp/UserService.h"
#include "../../gen-cpp/social_network_types.h"
#include "../../third_party/PicoSHA2/picosha2.h"
#include "../ClientPool.h"
#include "../ThriftClient.h"
#include "../logger.h"
#include "../tracing.h"
#include "../utils_id.h"
#include "../utils_mongodb.h"

static constexpr int MONGODB_TIMEOUT_MS = 100;

namespace social_network {

using std::chrono::duration_cast;
using std::chrono::milliseconds;
using std::chrono::seconds;
using std::chrono::system_clock;

std::string GenRandomString(const int len) {
  static const std::string alphanum =
      "0123456789"
      "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
      "abcdefghijklmnopqrstuvwxyz";
  std::random_device rd;
  std::uniform_int_distribution<int> dist(
      0, static_cast<int>(alphanum.length() - 1));
  std::string s;
  for (int i = 0; i < len; ++i) {
    s += alphanum[dist(rd)];
  }
  return s;
}

class UserHandler : public UserServiceIf {
 public:
  UserHandler(std::mutex *, const std::string &, const std::string &,
              memcached_pool_st *, mongoc_client_pool_t *,
              ClientPool<ThriftClient<SocialGraphServiceClient>> *);
  ~UserHandler() override = default;
  void RegisterUser(int64_t, const std::string &, const std::string &,
                    const std::string &, const std::string &,
                    const std::map<std::string, std::string, std::less<>> &) override;
  void RegisterUserWithId(int64_t, const std::string &, const std::string &,
                          const std::string &, const std::string &, int64_t,
                          const std::map<std::string, std::string, std::less<>> &) override;

  void ComposeCreatorWithUserId(
      Creator &, int64_t, int64_t, const std::string &,
      const std::map<std::string, std::string, std::less<>> &) override;
  void ComposeCreatorWithUsername(
      Creator &, int64_t, const std::string &,
      const std::map<std::string, std::string, std::less<>> &) override;
  void Login(std::string &, int64_t, const std::string &, const std::string &,
             const std::map<std::string, std::string, std::less<>> &) override;
  int64_t GetUserId(int64_t, const std::string &,
                    const std::map<std::string, std::string, std::less<>> &) override;

 private:
  std::string _machine_id;
  std::string _secret;
  std::mutex *_thread_lock;
  memcached_pool_st *_memcached_client_pool;
  mongoc_client_pool_t *_mongodb_client_pool;
  ClientPool<ThriftClient<SocialGraphServiceClient>> *_social_graph_client_pool;

  // Helper: insert new user document into MongoDB collection.
  // Returns true on success, throws ServiceException on error.
  static bool InsertUserDocument(mongoc_collection_t *collection,
                                 const std::string &username,
                                 int64_t user_id,
                                 const std::string &first_name,
                                 const std::string &last_name,
                                 const std::string &password,
                                 opentracing::Span *parent_span);

  // Helper: find a user document in MongoDB by username.
  // Pops a MongoDB client, queries the "user" collection, and invokes
  // |on_found| with the matched bson_t* document, or throws ServiceException
  // when the user is not found or a MongoDB error occurs.
  // The callback receives the document and must not retain a pointer to it
  // after it returns (the cursor is destroyed on exit).
  // |not_found_code| controls the error code used when the user is absent.
  template <typename Fn>
  void FindUserByUsername(const std::string &username,
                          opentracing::Span *parent_span,
                          ErrorCode::type not_found_code,
                          Fn on_found) const;

  // Helper: look up user_id from MongoDB by username.
  // Returns the user_id or throws ServiceException.
  int64_t LookupUserIdInMongoDB(const std::string &username,
                                opentracing::Span *parent_span) const;

  // Helper: look up login credentials from MongoDB by username.
  // Populates password_stored, salt_stored, user_id_stored and login_json,
  // or throws ServiceException.
  void LookupLoginInMongoDB(const std::string &username,
                            std::string &password_stored,
                            std::string &salt_stored,
                            int64_t &user_id_stored,
                            json &login_json,
                            opentracing::Span *parent_span) const;

  // Helper: check whether username already exists in MongoDB and, if not,
  // insert the user document and register it in the social graph.
  // Throws ServiceException on any error.
  void RegisterUserCore(int64_t req_id,
                        const std::string &first_name,
                        const std::string &last_name,
                        const std::string &username,
                        const std::string &password,
                        int64_t user_id,
                        opentracing::Span *span);

  // Helper: fetch user_id from memcached for the given username.
  // Returns the cached value as a string, or an empty string when the entry
  // is not present. Throws ServiceException on a memcached error.
  std::string GetUserIdFromMemcached(const std::string &username,
                                     opentracing::Span *parent_span) const;
};

UserHandler::UserHandler(std::mutex *thread_lock, const std::string &machine_id,
                         const std::string &secret,
                         memcached_pool_st *memcached_client_pool,
                         mongoc_client_pool_t *mongodb_client_pool,
                         ClientPool<ThriftClient<SocialGraphServiceClient>>
                             *social_graph_client_pool) {
  _thread_lock = thread_lock;
  _machine_id = machine_id;
  _memcached_client_pool = memcached_client_pool;
  _mongodb_client_pool = mongodb_client_pool;
  _secret = secret;
  _social_graph_client_pool = social_graph_client_pool;
}

// static helper — inserts new user document; throws on error
bool UserHandler::InsertUserDocument(mongoc_collection_t *collection,
                                     const std::string &username,
                                     int64_t user_id,
                                     const std::string &first_name,
                                     const std::string &last_name,
                                     const std::string &password,
                                     opentracing::Span *parent_span) {
  bson_t *new_doc = bson_new();
  BSON_APPEND_INT64(new_doc, "user_id", user_id);
  BSON_APPEND_UTF8(new_doc, "first_name", first_name.c_str());
  BSON_APPEND_UTF8(new_doc, "last_name", last_name.c_str());
  BSON_APPEND_UTF8(new_doc, "username", username.c_str());
  std::string salt = GenRandomString(32);
  BSON_APPEND_UTF8(new_doc, "salt", salt.c_str());
  std::string password_hashed = picosha2::hash256_hex_string(password + salt);
  BSON_APPEND_UTF8(new_doc, "password", password_hashed.c_str());

  auto user_insert_span = opentracing::Tracer::Global()->StartSpan(
      "user_mongo_insert_client", {opentracing::ChildOf(&parent_span->context())});
  bson_error_t insert_error;
  bool ok = mongoc_collection_insert_one(collection, new_doc, nullptr, nullptr,
                                         &insert_error);
  user_insert_span->Finish();
  bson_destroy(new_doc);

  if (!ok) {
    LOG(error) << "Failed to insert user " << username
               << " to MongoDB: " << insert_error.message;
    ServiceException se;
    se.errorCode = ErrorCode::SE_THRIFT_HANDLER_ERROR;
    se.message =
        std::format("Failed to insert user {} to MongoDB: {}", username,
                    insert_error.message);
    throw se;
  }
  LOG(debug) << "User: " << username << " registered";
  return true;
}

// Helper: find a user document in MongoDB by username and invoke |on_found|
// with the matched document. Throws ServiceException when not found or on error.
template <typename Fn>
void UserHandler::FindUserByUsername(const std::string &username,
                                     opentracing::Span *parent_span,
                                     ErrorCode::type not_found_code,
                                     Fn on_found) const {
  mongoc_client_t *mongodb_client = PopMongoClient(_mongodb_client_pool);
  auto collection = GetMongoCollection(_mongodb_client_pool, mongodb_client,
                                       "user", "user");

  bson_t *query = bson_new();
  BSON_APPEND_UTF8(query, "username", username.c_str());

  auto find_span = opentracing::Tracer::Global()->StartSpan(
      "user_mongo_find_client", {opentracing::ChildOf(&parent_span->context())});
  mongoc_cursor_t *cursor =
      mongoc_collection_find_with_opts(collection, query, nullptr, nullptr);
  const bson_t *doc;
  bool found = mongoc_cursor_next(cursor, &doc);
  find_span->Finish();

  auto cleanup = [&]() {
    bson_destroy(query);
    mongoc_cursor_destroy(cursor);
    mongoc_collection_destroy(collection);
    mongoc_client_pool_push(_mongodb_client_pool, mongodb_client);
  };

  bson_error_t cursor_error;
  if (mongoc_cursor_error(cursor, &cursor_error)) {
    LOG(error) << cursor_error.message;
    cleanup();
    ServiceException se;
    se.errorCode = ErrorCode::SE_MONGODB_ERROR;
    se.message = cursor_error.message;
    throw se;
  }

  if (!found) {
    LOG(warning) << "User: " << username << " doesn't exist in MongoDB";
    cleanup();
    ServiceException se;
    se.errorCode = not_found_code;
    se.message = "User: " + username + " is not registered";
    throw se;
  }

  LOG(debug) << "User: " << username << " found in MongoDB";
  // Invoke the caller's extraction logic while the cursor + doc are still live.
  on_found(doc);
  cleanup();
}

// Helper: look up user_id from MongoDB by username
int64_t UserHandler::LookupUserIdInMongoDB(const std::string &username,
                                           opentracing::Span *parent_span) const {
  int64_t user_id = -1;
  FindUserByUsername(username, parent_span, ErrorCode::SE_THRIFT_HANDLER_ERROR,
                     [&](const bson_t *doc) {
    bson_iter_t iter;
    if (!bson_iter_init_find(&iter, doc, "user_id")) {
      LOG(error) << "user_id attribute of user " << username
                 << " was not found in the User object";
      ServiceException se;
      se.errorCode = ErrorCode::SE_THRIFT_HANDLER_ERROR;
      se.message = "user_id attribute of user: " + username +
                   " was not found in the User object";
      throw se;
    }
    user_id = bson_iter_value(&iter)->value.v_int64;
  });
  return user_id;
}

// Helper: look up login credentials from MongoDB by username
static void ExtractLoginFields(const bson_t *doc,
                               const std::string &username,
                               std::string &password_stored,
                               std::string &salt_stored,
                               int64_t &user_id_stored,
                               json &login_json) {
  LOG(debug) << "Username: " << username << " found in MongoDB";
  bson_iter_t iter_password;
  bson_iter_t iter_salt;
  bson_iter_t iter_user_id;
  if (bson_iter_init_find(&iter_password, doc, "password") &&
      bson_iter_init_find(&iter_salt, doc, "salt") &&
      bson_iter_init_find(&iter_user_id, doc, "user_id")) {
    password_stored = bson_iter_value(&iter_password)->value.v_utf8.str;
    salt_stored     = bson_iter_value(&iter_salt)->value.v_utf8.str;
    user_id_stored  = bson_iter_value(&iter_user_id)->value.v_int64;
    login_json["password"] = password_stored;
    login_json["salt"]     = salt_stored;
    login_json["user_id"]  = user_id_stored;
    return;
  }
  LOG(error) << "user: " << username << " entry is NOT complete";
  ServiceException se;
  se.errorCode = ErrorCode::SE_THRIFT_HANDLER_ERROR;
  se.message = "user: " + username + " entry is NOT complete";
  throw se;
}

void UserHandler::LookupLoginInMongoDB(const std::string &username,
                                       std::string &password_stored,
                                       std::string &salt_stored,
                                       int64_t &user_id_stored,
                                       json &login_json,
                                       opentracing::Span *parent_span) const {
  FindUserByUsername(username, parent_span, ErrorCode::SE_UNAUTHORIZED,
                     [&](const bson_t *doc) {
    ExtractLoginFields(doc, username, password_stored, salt_stored,
                       user_id_stored, login_json);
  });
}

// Helper: fetch user_id string from memcached, or return empty string if absent.
std::string UserHandler::GetUserIdFromMemcached(const std::string &username,
                                                opentracing::Span *parent_span) const {
  size_t user_id_size;
  uint32_t memcached_flags;
  memcached_return_t memcached_rc;

  memcached_st *memcached_client =
      memcached_pool_pop(_memcached_client_pool, true, &memcached_rc);
  if (!memcached_client) {
    LOG(warning) << "Failed to pop a client from memcached pool";
    return {};
  }

  auto id_get_span = opentracing::Tracer::Global()->StartSpan(
      "user_mmc_get_client", {opentracing::ChildOf(&parent_span->context())});
  std::unique_ptr<char, void (*)(char *)> guard(
      memcached_get(memcached_client, (username + ":user_id").c_str(),
                    (username + ":user_id").length(), &user_id_size,
                    &memcached_flags, &memcached_rc),
      [](char *p) { bson_free(p); });
  id_get_span->Finish();

  if (!guard && memcached_rc != MEMCACHED_NOTFOUND) {
    ServiceException se;
    se.errorCode = ErrorCode::SE_MEMCACHED_ERROR;
    se.message = memcached_strerror(memcached_client, memcached_rc);
    memcached_pool_push(_memcached_client_pool, memcached_client);
    throw se;
  }
  memcached_pool_push(_memcached_client_pool, memcached_client);

  return guard ? std::string(guard.get(), user_id_size) : std::string{};
}

// Helper: shared registration core used by both RegisterUser and RegisterUserWithId.
void UserHandler::RegisterUserCore(
    const int64_t req_id, const std::string &first_name,
    const std::string &last_name, const std::string &username,
    const std::string &password, const int64_t user_id,
    opentracing::Span *span) {
  mongoc_client_t *mongodb_client = PopMongoClient(_mongodb_client_pool);
  auto collection = GetMongoCollection(_mongodb_client_pool, mongodb_client,
                                       "user", "user");

  // Check if the username has existed in the database
  bson_t *query = bson_new();
  BSON_APPEND_UTF8(query, "username", username.c_str());
  mongoc_cursor_t *cursor =
      mongoc_collection_find_with_opts(collection, query, nullptr, nullptr);
  const bson_t *doc;
  bson_error_t query_error;
  bool found = mongoc_cursor_next(cursor, &doc);

  auto cleanup = [&]() {
    bson_destroy(query);
    mongoc_cursor_destroy(cursor);
    mongoc_collection_destroy(collection);
    mongoc_client_pool_push(_mongodb_client_pool, mongodb_client);
  };

  if (mongoc_cursor_error(cursor, &query_error)) {
    LOG(warning) << query_error.message;
    cleanup();
    ServiceException se;
    se.errorCode = ErrorCode::SE_MONGODB_ERROR;
    se.message = query_error.message;
    throw se;
  } else if (found) {
    LOG(warning) << "User " << username << " already existed.";
    cleanup();
    ServiceException se;
    se.errorCode = ErrorCode::SE_THRIFT_HANDLER_ERROR;
    se.message = "User " + username + " already existed";
    throw se;
  } else {
    try {
      InsertUserDocument(collection, username, user_id, first_name, last_name,
                         password, span);
    } catch (...) {
      cleanup();
      throw;
    }
  }
  cleanup();

  // Register with social graph service
  auto social_graph_client_wrapper = _social_graph_client_pool->Pop();
  if (!social_graph_client_wrapper) {
    ServiceException se;
    se.errorCode = ErrorCode::SE_THRIFT_CONN_ERROR;
    se.message = "Failed to connect to social-graph-service";
    throw se;
  }
  auto social_graph_client = social_graph_client_wrapper->GetClient();
  std::map<std::string, std::string, std::less<>> writer_text_map;
  try {
    social_graph_client->InsertUser(req_id, user_id, writer_text_map);
  } catch (...) {
    _social_graph_client_pool->Remove(social_graph_client_wrapper);
    LOG(error) << "Failed to insert user to social-graph-service";
    throw;
  }
  _social_graph_client_pool->Keepalive(social_graph_client_wrapper);
}

void UserHandler::RegisterUserWithId(
    const int64_t req_id, const std::string &first_name,
    const std::string &last_name, const std::string &username,
    const std::string &password, const int64_t user_id,
    const std::map<std::string, std::string, std::less<>> &carrier) {
  std::map<std::string, std::string, std::less<>> writer_text_map;
  auto span = StartServerSpan("register_user_withid_server", carrier,
                              writer_text_map);
  RegisterUserCore(req_id, first_name, last_name, username, password, user_id,
                   span.get());
  span->Finish();
}

void UserHandler::RegisterUser(
    const int64_t req_id, const std::string &first_name,
    const std::string &last_name, const std::string &username,
    const std::string &password,
    const std::map<std::string, std::string, std::less<>> &carrier) {
  std::map<std::string, std::string, std::less<>> writer_text_map;
  auto span = StartServerSpan("register_user_server", carrier, writer_text_map);

  // Compose user_id using the shared Snowflake ID generator
  int64_t user_id = ComposeSnowflakeId(_machine_id, _thread_lock);
  LOG(debug) << "The user_id of the request " << req_id << " is " << user_id;

  RegisterUserCore(req_id, first_name, last_name, username, password, user_id,
                   span.get());
  span->Finish();
}

void UserHandler::ComposeCreatorWithUsername(
    Creator &_return, const int64_t req_id, const std::string &username,
    const std::map<std::string, std::string, std::less<>> &carrier) {
  std::map<std::string, std::string, std::less<>> writer_text_map;
  auto span = StartServerSpan("compose_creator_server", carrier,
                              writer_text_map);

  std::string cached_id = GetUserIdFromMemcached(username, span.get());

  int64_t user_id = -1;
  bool cached = false;
  if (!cached_id.empty()) {
    cached = true;
    LOG(debug) << "Found user_id of username :" << username << " in Memcached";
    user_id = std::stoul(cached_id);
  } else {
    LOG(debug) << "user_id not cached in Memcached";
    user_id = LookupUserIdInMongoDB(username, span.get());
  }

  Creator creator;
  creator.username = username;
  creator.user_id = user_id;

  if (user_id != -1) {
    _return = creator;
  }

  if (user_id != -1 && !cached) {
    memcached_return_t memcached_rc;
    memcached_st *memcached_client =
        memcached_pool_pop(_memcached_client_pool, true, &memcached_rc);
    if (memcached_client) {
      auto id_set_span = opentracing::Tracer::Global()->StartSpan(
          "user_mmc_set_cilent", {opentracing::ChildOf(&span->context())});
      std::string user_id_str = std::to_string(user_id);
      memcached_rc =
          memcached_set(memcached_client, (username + ":user_id").c_str(),
                        (username + ":user_id").length(), user_id_str.c_str(),
                        user_id_str.length(), static_cast<time_t>(0),
                        static_cast<uint32_t>(0));
      id_set_span->Finish();
      if (memcached_rc != MEMCACHED_SUCCESS) {
        LOG(warning) << "Failed to set the user_id of user " << username
                     << " to Memcached: "
                     << memcached_strerror(memcached_client, memcached_rc);
      }
      memcached_pool_push(_memcached_client_pool, memcached_client);
    } else {
      LOG(warning) << "Failed to pop a client from memcached pool";
    }
  }
  span->Finish();
}

void UserHandler::ComposeCreatorWithUserId(
    Creator &_return, int64_t req_id, int64_t user_id,
    const std::string &username,
    const std::map<std::string, std::string, std::less<>> &carrier) {
  std::map<std::string, std::string, std::less<>> writer_text_map;
  auto span = StartServerSpan("compose_creator_server", carrier,
                              writer_text_map);

  Creator creator;
  creator.username = username;
  creator.user_id = user_id;

  _return = creator;

  span->Finish();
}

void UserHandler::Login(std::string &_return, int64_t req_id,
                        const std::string &username,
                        const std::string &password,
                        const std::map<std::string, std::string, std::less<>> &carrier) {
  std::map<std::string, std::string, std::less<>> writer_text_map;
  auto span = StartServerSpan("login_server", carrier, writer_text_map);

  size_t login_size;
  uint32_t memcached_flags;

  memcached_return_t memcached_rc;
  memcached_st *memcached_client =
      memcached_pool_pop(_memcached_client_pool, true, &memcached_rc);
  std::unique_ptr<char, void (*)(char *)> login_mmc_guard(nullptr, [](char *p) { bson_free(p); });
  char *login_mmc = nullptr;
  if (!memcached_client) {
    LOG(warning) << "Failed to pop a client from memcached pool";
  } else {
    auto get_login_span = opentracing::Tracer::Global()->StartSpan(
        "user_mmc_get_client", {opentracing::ChildOf(&span->context())});
    login_mmc_guard.reset(memcached_get(memcached_client,
                              (username + ":login").c_str(),
                              (username + ":login").length(), &login_size,
                              &memcached_flags, &memcached_rc));
    login_mmc = login_mmc_guard.get();
    get_login_span->Finish();
    if (!login_mmc && memcached_rc != MEMCACHED_NOTFOUND) {
      LOG(warning) << "Memcached error: "
                   << memcached_strerror(memcached_client, memcached_rc);
    }
    memcached_pool_push(_memcached_client_pool, memcached_client);
  }

  std::string password_stored;
  std::string salt_stored;
  int64_t user_id_stored = -1;
  bool cached = false;
  json login_json;

  if (login_mmc) {
    // Found in memcached
    LOG(debug) << "Found username: " << username << " in Memcached";
    login_json = json::parse(std::string(login_mmc, login_size));
    password_stored = login_json["password"];
    salt_stored = login_json["salt"];
    user_id_stored = login_json["user_id"];
    cached = true;
  } else {
    // If not cached in memcached
    LOG(debug) << "Username: " << username << " NOT cached in Memcached";
    LookupLoginInMongoDB(username, password_stored, salt_stored,
                         user_id_stored, login_json, span.get());
  }

  if (user_id_stored != -1 && !salt_stored.empty() &&
      !password_stored.empty()) {
    bool auth =
        picosha2::hash256_hex_string(password + salt_stored) == password_stored;
    if (auth) {
      auto user_id_str = std::to_string(user_id_stored);
      auto timestamp_str = std::to_string(
          duration_cast<seconds>(system_clock::now().time_since_epoch())
              .count());
      jwt::jwt_object obj{jwt::params::algorithm("HS256"),
                          jwt::params::secret(_secret),
                          jwt::params::payload({{"user_id", user_id_str},
                                                {"username", username},
                                                {"timestamp", timestamp_str},
                                                {"ttl", "3600"}})};
      _return = obj.signature();
    } else {
      ServiceException se;
      se.errorCode = ErrorCode::SE_UNAUTHORIZED;
      se.message = "Incorrect username or password";
      throw se;
    }
  } else {
    ServiceException se;
    se.errorCode = ErrorCode::SE_THRIFT_HANDLER_ERROR;
    se.message = "Username: " + username + " incomplete login information.";
    throw se;
  }

  if (!cached) {
    memcached_client =
        memcached_pool_pop(_memcached_client_pool, true, &memcached_rc);
    if (!memcached_client) {
      LOG(warning) << "Failed to pop a client from memcached pool";
    } else {
      auto set_login_span = opentracing::Tracer::Global()->StartSpan(
          "user_mmc_set_client", {opentracing::ChildOf(&span->context())});
      std::string login_str = login_json.dump();
      memcached_rc =
          memcached_set(memcached_client, (username + ":login").c_str(),
                        (username + ":login").length(), login_str.c_str(),
                        login_str.length(), 0, 0);
      set_login_span->Finish();
      if (memcached_rc != MEMCACHED_SUCCESS) {
        LOG(warning) << "Failed to set the login info of user " << username
                     << " to Memcached: "
                     << memcached_strerror(memcached_client, memcached_rc);
      }
      memcached_pool_push(_memcached_client_pool, memcached_client);
    }
  }
  span->Finish();
}

int64_t UserHandler::GetUserId(
    int64_t req_id, const std::string &username,
    const std::map<std::string, std::string, std::less<>> &carrier) {
  std::map<std::string, std::string, std::less<>> writer_text_map;
  auto span = StartServerSpan("get_user_id_server", carrier, writer_text_map);

  std::string cached_id = GetUserIdFromMemcached(username, span.get());

  int64_t user_id = -1;
  bool cached = false;
  if (!cached_id.empty()) {
    cached = true;
    LOG(debug) << "Found user_id of username :" << username << " in Memcached";
    user_id = std::stoul(cached_id);
  } else {
    // If not cached in memcached
    LOG(debug) << "user_id not cached in Memcached";
    user_id = LookupUserIdInMongoDB(username, span.get());
  }

  if (!cached) {
    memcached_return_t memcached_rc;
    memcached_st *memcached_client =
        memcached_pool_pop(_memcached_client_pool, true, &memcached_rc);
    if (!memcached_client) {
      LOG(warning) << "Failed to pop a client from memcached pool";
    } else {
      std::string user_id_str = std::to_string(user_id);
      auto set_login_span = opentracing::Tracer::Global()->StartSpan(
          "user_mmc_set_client", {opentracing::ChildOf(&span->context())});
      memcached_rc =
          memcached_set(memcached_client, (username + ":user_id").c_str(),
                        (username + ":user_id").length(), user_id_str.c_str(),
                        user_id_str.length(), 0, 0);
      set_login_span->Finish();
      if (memcached_rc != MEMCACHED_SUCCESS) {
        LOG(warning) << "Failed to set the login info of user " << username
                     << " to Memcached: "
                     << memcached_strerror(memcached_client, memcached_rc);
      }
      memcached_pool_push(_memcached_client_pool, memcached_client);
    }
  }

  span->Finish();
  return user_id;
}

}  // namespace social_network

#endif  // SOCIAL_NETWORK_MICROSERVICES_USERHANDLER_H
