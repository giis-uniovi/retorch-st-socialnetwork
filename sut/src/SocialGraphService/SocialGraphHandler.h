#include <format>
#ifndef SOCIAL_NETWORK_MICROSERVICES_SOCIALGRAPHHANDLER_H
#define SOCIAL_NETWORK_MICROSERVICES_SOCIALGRAPHHANDLER_H

#include <bson/bson.h>
#include <mongoc.h>
#include <sw/redis++/redis++.h>

#include <chrono>
#include <future>
#include <iostream>
#include <string>
#include <thread>
#include <vector>

#include "../../gen-cpp/SocialGraphService.h"
#include "../../gen-cpp/UserService.h"
#include "../ClientPool.h"
#include "../ThriftClient.h"
#include "../logger.h"
#include "../tracing.h"
#include "../transparent_hash.h"
#include "../utils_mongodb.h"
#include "../utils_redis.h"

using namespace sw::redis;

namespace social_network {

using std::chrono::duration_cast;
using std::chrono::milliseconds;
using std::chrono::system_clock;

// ---------------------------------------------------------------------------
// RedisContext: alias for RedisPoolSet (from utils_redis.h) with accessor
// names matching the existing helper function signatures in this file.
// ---------------------------------------------------------------------------

// Re-expose RedisPoolSet fields under the legacy names used by the helper
// functions in this file.  This thin wrapper keeps the helper functions
// unchanged while allowing the class to carry a single RedisPoolSet member.
struct RedisContext {
  Redis        *client_pool;
  Redis        *replica_client_pool;
  Redis        *primary_client_pool;
  RedisCluster *cluster_client_pool;
  bool          replication_enabled;

  // Construct from a RedisPoolSet so class methods can call
  //   RedisContext redis_ctx{_redis};
  // instead of listing all four pointers.
  explicit RedisContext(const RedisPoolSet &pools)
      : client_pool(pools.client),
        replica_client_pool(pools.replica),
        primary_client_pool(pools.primary),
        cluster_client_pool(pools.cluster),
        replication_enabled(pools.IsReplicationEnabled()) {}
};

// ---------------------------------------------------------------------------
// Free helper functions for Follow
// ---------------------------------------------------------------------------

static void UpdateFollowerEdgeInMongo(
    mongoc_client_pool_t *mongodb_client_pool,
    int64_t user_id,
    int64_t followee_id,
    int64_t timestamp,
    opentracing::Span *parent_span) {
  mongoc_client_t *mongodb_client = PopMongoClient(mongodb_client_pool);
  auto collection = GetMongoCollection(mongodb_client_pool, mongodb_client,
                                       "social-graph", "social-graph");

  bson_t *search_not_exist = BCON_NEW(
      "$and", "[", "{", "user_id", BCON_INT64(user_id), "}", "{",
      "followees", "{", "$not", "{", "$elemMatch", "{", "user_id",
      BCON_INT64(followee_id), "}", "}", "}", "}", "]");
  bson_t *update = BCON_NEW("$push", "{", "followees", "{", "user_id",
                            BCON_INT64(followee_id), "timestamp",
                            BCON_INT64(timestamp), "}", "}");
  bson_error_t error;
  bson_t reply;
  auto update_span = opentracing::Tracer::Global()->StartSpan(
      "mongo_update_client", {opentracing::ChildOf(&parent_span->context())});
  bool updated = mongoc_collection_find_and_modify(
      collection, search_not_exist, nullptr, update, nullptr, false,
      false, true, &reply, &error);
  if (!updated) {
    LOG(error) << "Failed to update social graph for user " << user_id
               << " to MongoDB: " << error.message;
    ServiceException se;
    se.errorCode = ErrorCode::SE_MONGODB_ERROR;
    se.message = error.message;
    bson_destroy(&reply);
    bson_destroy(update);
    bson_destroy(search_not_exist);
    mongoc_collection_destroy(collection);
    mongoc_client_pool_push(mongodb_client_pool, mongodb_client);
    throw se;
  }
  update_span->Finish();
  bson_destroy(&reply);
  bson_destroy(update);
  bson_destroy(search_not_exist);
  mongoc_collection_destroy(collection);
  mongoc_client_pool_push(mongodb_client_pool, mongodb_client);
}

static void UpdateFolloweeEdgeInMongo(
    mongoc_client_pool_t *mongodb_client_pool,
    int64_t user_id,
    int64_t followee_id,
    int64_t timestamp,
    opentracing::Span *parent_span) {
  mongoc_client_t *mongodb_client = PopMongoClient(mongodb_client_pool);
  auto collection = GetMongoCollection(mongodb_client_pool, mongodb_client,
                                       "social-graph", "social-graph");

  bson_t *search_not_exist =
      BCON_NEW("$and", "[", "{", "user_id", BCON_INT64(followee_id), "}",
               "{", "followers", "{", "$not", "{", "$elemMatch", "{",
               "user_id", BCON_INT64(user_id), "}", "}", "}", "}", "]");
  bson_t *update = BCON_NEW("$push", "{", "followers", "{", "user_id",
                            BCON_INT64(user_id), "timestamp",
                            BCON_INT64(timestamp), "}", "}");
  bson_error_t error;
  auto update_span = opentracing::Tracer::Global()->StartSpan(
      "social_graph_mongo_update_client",
      {opentracing::ChildOf(&parent_span->context())});
  bson_t reply;
  bool updated = mongoc_collection_find_and_modify(
      collection, search_not_exist, nullptr, update, nullptr, false,
      false, true, &reply, &error);
  if (!updated) {
    LOG(error) << "Failed to update social graph for user " << followee_id
               << " to MongoDB: " << error.message;
    ServiceException se;
    se.errorCode = ErrorCode::SE_MONGODB_ERROR;
    se.message = error.message;
    bson_destroy(update);
    bson_destroy(&reply);
    bson_destroy(search_not_exist);
    mongoc_collection_destroy(collection);
    mongoc_client_pool_push(mongodb_client_pool, mongodb_client);
    throw se;
  }
  update_span->Finish();
  bson_destroy(update);
  bson_destroy(&reply);
  bson_destroy(search_not_exist);
  mongoc_collection_destroy(collection);
  mongoc_client_pool_push(mongodb_client_pool, mongodb_client);
}

static void UpdateFollowRedis(
    const RedisContext &redis_ctx,
    int64_t user_id,
    int64_t followee_id,
    int64_t timestamp,
    opentracing::Span *parent_span) {
  auto redis_span = opentracing::Tracer::Global()->StartSpan(
      "social_graph_redis_update_client",
      {opentracing::ChildOf(&parent_span->context())});

  if (redis_ctx.client_pool) {
    auto pipe = redis_ctx.client_pool->pipeline(false);
    pipe.zadd(std::format("{}:followees", user_id),
              std::to_string(followee_id), timestamp, UpdateType::NOT_EXIST)
        .zadd(std::format("{}:followers", followee_id),
              std::to_string(user_id), timestamp, UpdateType::NOT_EXIST);
    try {
      auto replies = pipe.exec();
    } catch (const Error &err) {
      LOG(error) << err.what();
      throw err;
    }
  } else if (redis_ctx.replication_enabled) {
    auto pipe = redis_ctx.primary_client_pool->pipeline(false);
    pipe.zadd(std::format("{}:followees", user_id),
              std::to_string(followee_id), timestamp, UpdateType::NOT_EXIST)
        .zadd(std::format("{}:followers", followee_id),
              std::to_string(user_id), timestamp, UpdateType::NOT_EXIST);
    try {
      auto replies = pipe.exec();
    } catch (const Error &err) {
      LOG(error) << err.what();
      throw err;
    }
  } else {
    // Redis++ currently does not support pipeline with multiple
    //       hashtags in cluster mode.
    //       Currently, we send one request for each follower, which may
    //       incur some performance overhead. We are following the updates
    //       of Redis++ clients:
    //       https://github.com/sewenew/redis-plus-plus/issues/212
    try {
      redis_ctx.cluster_client_pool->zadd(
          std::format("{}:followees", user_id),
          std::to_string(followee_id), timestamp, UpdateType::NOT_EXIST);
      redis_ctx.cluster_client_pool->zadd(
          std::format("{}:followers", followee_id),
          std::to_string(user_id), timestamp, UpdateType::NOT_EXIST);
    } catch (const Error &err) {
      LOG(error) << err.what();
      throw err;
    }
  }

  redis_span->Finish();
}

// ---------------------------------------------------------------------------
// Free helper functions for Unfollow
// ---------------------------------------------------------------------------

// Remove a single edge from the social-graph collection.
// query_user_id: the user_id field value to match in the query.
// pull_field: the array field to $pull from ("followees" or "followers").
// pull_user_id: the user_id value to remove from that array.
static void RemoveEdgeInMongo(
    mongoc_client_pool_t *mongodb_client_pool,
    int64_t query_user_id,
    const char *pull_field,
    int64_t pull_user_id,
    opentracing::Span *parent_span) {
  mongoc_client_t *mongodb_client = PopMongoClient(mongodb_client_pool);
  auto collection = GetMongoCollection(mongodb_client_pool, mongodb_client,
                                       "social-graph", "social-graph");
  bson_t *query = bson_new();
  BSON_APPEND_INT64(query, "user_id", query_user_id);
  bson_t *update = BCON_NEW("$pull", "{", pull_field, "{", "user_id",
                            BCON_INT64(pull_user_id), "}", "}");
  bson_t reply;
  bson_error_t error;
  auto update_span = opentracing::Tracer::Global()->StartSpan(
      "social_graph_mongo_delete_client",
      {opentracing::ChildOf(&parent_span->context())});
  bool updated = mongoc_collection_find_and_modify(
      collection, query, nullptr, update, nullptr, false, false, true,
      &reply, &error);
  if (!updated) {
    LOG(error) << "Failed to delete social graph for user " << query_user_id
               << " to MongoDB: " << error.message;
    ServiceException se;
    se.errorCode = ErrorCode::SE_MONGODB_ERROR;
    se.message = error.message;
    bson_destroy(update);
    bson_destroy(query);
    bson_destroy(&reply);
    mongoc_collection_destroy(collection);
    mongoc_client_pool_push(mongodb_client_pool, mongodb_client);
    throw se;
  }
  update_span->Finish();
  bson_destroy(update);
  bson_destroy(query);
  bson_destroy(&reply);
  mongoc_collection_destroy(collection);
  mongoc_client_pool_push(mongodb_client_pool, mongodb_client);
}

static void RemoveFollowerEdgeInMongo(
    mongoc_client_pool_t *mongodb_client_pool,
    int64_t user_id,
    int64_t followee_id,
    opentracing::Span *parent_span) {
  RemoveEdgeInMongo(mongodb_client_pool, user_id, "followees", followee_id,
                    parent_span);
}

static void RemoveFolloweeEdgeInMongo(
    mongoc_client_pool_t *mongodb_client_pool,
    int64_t user_id,
    int64_t followee_id,
    opentracing::Span *parent_span) {
  RemoveEdgeInMongo(mongodb_client_pool, followee_id, "followers", user_id,
                    parent_span);
}

static void UpdateUnfollowRedis(
    const RedisContext &redis_ctx,
    int64_t user_id,
    int64_t followee_id,
    opentracing::Span *parent_span) {
  auto redis_span = opentracing::Tracer::Global()->StartSpan(
      "social_graph_redis_update_client",
      {opentracing::ChildOf(&parent_span->context())});

  std::string followee_key = std::format("{}:followees", user_id);
  std::string follower_key = std::format("{}:followers", followee_id);

  if (redis_ctx.client_pool) {
    auto pipe = redis_ctx.client_pool->pipeline(false);
    pipe.zrem(followee_key, std::to_string(followee_id))
        .zrem(follower_key, std::to_string(user_id));
    try {
      auto replies = pipe.exec();
    } catch (const Error &err) {
      LOG(error) << err.what();
      throw err;
    }
  } else if (redis_ctx.replication_enabled) {
    auto pipe = redis_ctx.primary_client_pool->pipeline(false);
    pipe.zrem(followee_key, std::to_string(followee_id))
        .zrem(follower_key, std::to_string(user_id));
    try {
      auto replies = pipe.exec();
    } catch (const Error &err) {
      LOG(error) << err.what();
      throw err;
    }
  } else {
    try {
      redis_ctx.cluster_client_pool->zrem(followee_key, std::to_string(followee_id));
      redis_ctx.cluster_client_pool->zrem(follower_key, std::to_string(user_id));
    } catch (const Error &err) {
      LOG(error) << err.what();
      throw err;
    }
  }

  redis_span->Finish();
}

// ---------------------------------------------------------------------------
// Shared Redis dispatch helpers (used by both followers and followees)
// ---------------------------------------------------------------------------

static void ReadFromRedisZSet(
    const RedisContext &redis_ctx,
    const std::string &key,
    std::vector<std::string> &out_str) {
  if (redis_ctx.client_pool) {
    redis_ctx.client_pool->zrange(key, 0, -1, std::back_inserter(out_str));
  } else if (redis_ctx.replication_enabled) {
    redis_ctx.replica_client_pool->zrange(key, 0, -1, std::back_inserter(out_str));
  } else {
    redis_ctx.cluster_client_pool->zrange(key, 0, -1, std::back_inserter(out_str));
  }
}

template <typename ScoreMap>
static void WriteToRedisZSet(
    const RedisContext &redis_ctx,
    const std::string &key,
    const ScoreMap &redis_zset) {
  if (redis_ctx.client_pool) {
    redis_ctx.client_pool->zadd(key, redis_zset.begin(), redis_zset.end());
  } else if (redis_ctx.replication_enabled) {
    redis_ctx.primary_client_pool->zadd(key, redis_zset.begin(), redis_zset.end());
  } else {
    redis_ctx.cluster_client_pool->zadd(key, redis_zset.begin(), redis_zset.end());
  }
}

// ---------------------------------------------------------------------------
// Free helper functions for GetFollowers
// ---------------------------------------------------------------------------

static void ReadFollowersFromMongo(
    mongoc_client_pool_t *mongodb_client_pool,
    const RedisContext &redis_ctx,
    int64_t user_id,
    const std::string &redis_key,
    std::vector<int64_t> &result,
    opentracing::Span *parent_span) {
  mongoc_client_t *mongodb_client = PopMongoClient(mongodb_client_pool);
  auto collection = GetMongoCollection(mongodb_client_pool, mongodb_client,
                                       "social-graph", "social-graph");
  bson_t *query = bson_new();
  BSON_APPEND_INT64(query, "user_id", user_id);
  auto find_span = opentracing::Tracer::Global()->StartSpan(
      "social_graph_mongo_find_client",
      {opentracing::ChildOf(&parent_span->context())});
  mongoc_cursor_t *cursor =
      mongoc_collection_find_with_opts(collection, query, nullptr, nullptr);
  const bson_t *doc;
  bool found = mongoc_cursor_next(cursor, &doc);
  if (found) {
    bson_iter_t iter_0;
    bson_iter_t iter_1;
    bson_iter_t user_id_child;
    bson_iter_t timestamp_child;
    int index = 0;
    std::unordered_map<std::string, double, TransparentStringHash, std::equal_to<>> redis_zset;
    bson_iter_init(&iter_0, doc);
    bson_iter_init(&iter_1, doc);

    while (bson_iter_find_descendant(
               &iter_0,
               std::format("followers.{}.user_id", index).c_str(),
               &user_id_child) &&
           BSON_ITER_HOLDS_INT64(&user_id_child) &&
           bson_iter_find_descendant(
               &iter_1,
               std::format("followers.{}.timestamp", index).c_str(),
               &timestamp_child) &&
           BSON_ITER_HOLDS_INT64(&timestamp_child)) {
      auto iter_user_id = bson_iter_int64(&user_id_child);
      auto iter_timestamp = bson_iter_int64(&timestamp_child);
      result.emplace_back(iter_user_id);
      redis_zset.try_emplace(std::to_string(iter_user_id), (double)iter_timestamp);
      bson_iter_init(&iter_0, doc);
      bson_iter_init(&iter_1, doc);
      index++;
    }
    find_span->Finish();
    bson_destroy(query);
    mongoc_cursor_destroy(cursor);
    mongoc_collection_destroy(collection);
    mongoc_client_pool_push(mongodb_client_pool, mongodb_client);

    auto redis_insert_span = opentracing::Tracer::Global()->StartSpan(
        "social_graph_redis_insert_client",
        {opentracing::ChildOf(&parent_span->context())});
    try {
      WriteToRedisZSet(redis_ctx, redis_key, redis_zset);
    } catch (const Error &err) {
      LOG(error) << err.what();
      throw err;
    }
    redis_insert_span->Finish();
  } else {
    LOG(warning) << "user_id: " << user_id << " not found";
    find_span->Finish();
    bson_destroy(query);
    mongoc_cursor_destroy(cursor);
    mongoc_collection_destroy(collection);
    mongoc_client_pool_push(mongodb_client_pool, mongodb_client);
  }
}

// ---------------------------------------------------------------------------
// Free helper functions for GetFollowees
// ---------------------------------------------------------------------------

static void ReadFolloweesFromMongo(
    mongoc_client_pool_t *mongodb_client_pool,
    const RedisContext &redis_ctx,
    int64_t user_id,
    std::vector<int64_t> &result,
    opentracing::Span *parent_span) {
  mongoc_client_t *mongodb_client = PopMongoClient(mongodb_client_pool);
  auto collection = GetMongoCollection(mongodb_client_pool, mongodb_client,
                                       "social-graph", "social-graph");
  bson_t *query = bson_new();
  BSON_APPEND_INT64(query, "user_id", user_id);
  auto find_span = opentracing::Tracer::Global()->StartSpan(
      "social_graph_mongo_find_client",
      {opentracing::ChildOf(&parent_span->context())});
  mongoc_cursor_t *cursor =
      mongoc_collection_find_with_opts(collection, query, nullptr, nullptr);
  const bson_t *doc;
  bool found = mongoc_cursor_next(cursor, &doc);
  if (!found) {
    ServiceException se;
    se.errorCode = ErrorCode::SE_THRIFT_HANDLER_ERROR;
    se.message = "Cannot find user_id in MongoDB.";
    bson_destroy(query);
    mongoc_cursor_destroy(cursor);
    mongoc_collection_destroy(collection);
    mongoc_client_pool_push(mongodb_client_pool, mongodb_client);
    throw se;
  }

  bson_iter_t iter_0;
  bson_iter_t iter_1;
  bson_iter_t user_id_child;
  bson_iter_t timestamp_child;
  int index = 0;

  bson_iter_init(&iter_0, doc);
  bson_iter_init(&iter_1, doc);

  std::multimap<std::string, double, std::less<>> redis_zset;

  while (bson_iter_find_descendant(
             &iter_0,
             std::format("followees.{}.user_id", index).c_str(),
             &user_id_child) &&
         BSON_ITER_HOLDS_INT64(&user_id_child) &&
         bson_iter_find_descendant(
             &iter_1,
             std::format("followees.{}.timestamp", index).c_str(),
             &timestamp_child) &&
         BSON_ITER_HOLDS_INT64(&timestamp_child)) {
    auto iter_user_id = bson_iter_int64(&user_id_child);
    auto iter_timestamp = bson_iter_int64(&timestamp_child);
    result.emplace_back(iter_user_id);

    redis_zset.emplace(std::to_string(iter_user_id), (double)iter_timestamp);
    bson_iter_init(&iter_0, doc);
    bson_iter_init(&iter_1, doc);
    index++;
  }

  find_span->Finish();
  bson_destroy(query);
  mongoc_cursor_destroy(cursor);
  mongoc_collection_destroy(collection);
  mongoc_client_pool_push(mongodb_client_pool, mongodb_client);

  std::string followees_redis_key = std::format("{}:followees", user_id);
  auto redis_insert_span = opentracing::Tracer::Global()->StartSpan(
      "social_graph_redis_insert_client",
      {opentracing::ChildOf(&parent_span->context())});
  try {
    WriteToRedisZSet(redis_ctx, followees_redis_key, redis_zset);
  } catch (const Error &err) {
    LOG(error) << err.what();
    throw err;
  }
  redis_insert_span->Finish();
}

// ---------------------------------------------------------------------------
// Class declaration
// ---------------------------------------------------------------------------

class SocialGraphHandler : public SocialGraphServiceIf {
 public:
  SocialGraphHandler(mongoc_client_pool_t *, Redis *,
                     ClientPool<ThriftClient<UserServiceClient>> *);
  SocialGraphHandler(mongoc_client_pool_t *, Redis *, Redis *,
      ClientPool<ThriftClient<UserServiceClient>>*);
  SocialGraphHandler(mongoc_client_pool_t *, RedisCluster *,
                     ClientPool<ThriftClient<UserServiceClient>> *);
  ~SocialGraphHandler() override = default;
  bool IsRedisReplicationEnabled();
  void GetFollowers(std::vector<int64_t> &, int64_t, int64_t,
                    const std::map<std::string, std::string, std::less<>> &) override;
  void GetFollowees(std::vector<int64_t> &, int64_t, int64_t,
                    const std::map<std::string, std::string, std::less<>> &) override;
  void Follow(int64_t, int64_t, int64_t,
              const std::map<std::string, std::string, std::less<>> &) override;
  void Unfollow(int64_t, int64_t, int64_t,
                const std::map<std::string, std::string, std::less<>> &) override;
  void FollowWithUsername(int64_t, const std::string &, const std::string &,
                          const std::map<std::string, std::string, std::less<>> &) override;
  void UnfollowWithUsername(
      int64_t, const std::string &, const std::string &,
      const std::map<std::string, std::string, std::less<>> &) override;
  void InsertUser(int64_t, int64_t,
                  const std::map<std::string, std::string, std::less<>> &) override;

 private:
  int64_t _GetUserId(
      int64_t req_id, const std::string &name,
      const std::map<std::string, std::string, std::less<>> &writer_text_map);

  mongoc_client_pool_t *_mongodb_client_pool;
  RedisPoolSet _redis;
  ClientPool<ThriftClient<UserServiceClient>> *_user_service_client_pool;
};

SocialGraphHandler::SocialGraphHandler(
    mongoc_client_pool_t *mongodb_client_pool, Redis *redis_client_pool,
    ClientPool<ThriftClient<UserServiceClient>> *user_service_client_pool)
    : _mongodb_client_pool(mongodb_client_pool),
      _redis(RedisPoolSet::Standalone(redis_client_pool)),
      _user_service_client_pool(user_service_client_pool) {}

SocialGraphHandler::SocialGraphHandler(
    mongoc_client_pool_t *mongodb_client_pool,
    Redis *redis_replica_client_pool, Redis *redis_primary_client_pool,
    ClientPool<ThriftClient<UserServiceClient>> *user_service_client_pool)
    : _mongodb_client_pool(mongodb_client_pool),
      _redis(RedisPoolSet::Replication(redis_replica_client_pool,
                                       redis_primary_client_pool)),
      _user_service_client_pool(user_service_client_pool) {}

SocialGraphHandler::SocialGraphHandler(
    mongoc_client_pool_t *mongodb_client_pool,
    RedisCluster *redis_cluster_client_pool,
    ClientPool<ThriftClient<UserServiceClient>> *user_service_client_pool)
    : _mongodb_client_pool(mongodb_client_pool),
      _redis(RedisPoolSet::Cluster(redis_cluster_client_pool)),
      _user_service_client_pool(user_service_client_pool) {}

bool SocialGraphHandler::IsRedisReplicationEnabled() {
  return _redis.IsReplicationEnabled();
}

void SocialGraphHandler::Follow(
    int64_t req_id, int64_t user_id, int64_t followee_id,
    const std::map<std::string, std::string, std::less<>> &carrier) {
  std::map<std::string, std::string, std::less<>> writer_text_map;
  auto span = StartServerSpan("follow_server", carrier, writer_text_map);

  int64_t timestamp =
      duration_cast<milliseconds>(system_clock::now().time_since_epoch())
          .count();

  RedisContext redis_ctx(_redis);
  try {
    UpdateFollowRedis(redis_ctx, user_id, followee_id, timestamp, span.get());
    UpdateFollowerEdgeInMongo(_mongodb_client_pool, user_id, followee_id,
                              timestamp, span.get());
    UpdateFolloweeEdgeInMongo(_mongodb_client_pool, user_id, followee_id,
                              timestamp, span.get());
  } catch (const std::exception &e) {
    LOG(warning) << e.what();
    throw;
  }

  span->Finish();
}

void SocialGraphHandler::Unfollow(
    int64_t req_id, int64_t user_id, int64_t followee_id,
    const std::map<std::string, std::string, std::less<>> &carrier) {
  std::map<std::string, std::string, std::less<>> writer_text_map;
  auto span = StartServerSpan("unfollow_server", carrier, writer_text_map);

  RedisContext redis_ctx(_redis);
  UpdateUnfollowRedis(redis_ctx, user_id, followee_id, span.get());
  RemoveFollowerEdgeInMongo(_mongodb_client_pool, user_id, followee_id,
                            span.get());
  RemoveFolloweeEdgeInMongo(_mongodb_client_pool, user_id, followee_id,
                            span.get());

  span->Finish();
}

void SocialGraphHandler::GetFollowers(
    std::vector<int64_t> &_return, const int64_t req_id, const int64_t user_id,
    const std::map<std::string, std::string, std::less<>> &carrier) {
  std::map<std::string, std::string, std::less<>> writer_text_map;
  auto span = StartServerSpan("get_followers_server", carrier, writer_text_map);

  auto redis_span = opentracing::Tracer::Global()->StartSpan(
      "social_graph_redis_get_client",
      {opentracing::ChildOf(&span->context())});

  RedisContext redis_ctx(_redis);
  std::vector<std::string> followers_str;
  std::string followers_key = std::format("{}:followers", user_id);
  try {
    ReadFromRedisZSet(redis_ctx, followers_key, followers_str);
  } catch (const Error &err) {
    LOG(error) << err.what();
    throw err;
  }
  redis_span->Finish();

  // If user_id in the social graph Redis server, read from Redis
  if (!followers_str.empty()) {
    for (auto const &follower_str : followers_str) {
      _return.emplace_back(std::stoul(follower_str));
    }
  }
  // If user_id not in the social graph Redis server, read from MongoDB and
  // update Redis.
  else {
    ReadFollowersFromMongo(_mongodb_client_pool, redis_ctx, user_id,
                           followers_key, _return, span.get());
  }
  span->Finish();
}

void SocialGraphHandler::GetFollowees(
    std::vector<int64_t> &_return, const int64_t req_id, const int64_t user_id,
    const std::map<std::string, std::string, std::less<>> &carrier) {
  std::map<std::string, std::string, std::less<>> writer_text_map;
  auto span = StartServerSpan("get_followees_server", carrier, writer_text_map);

  auto redis_span = opentracing::Tracer::Global()->StartSpan(
      "social_graph_redis_get_client",
      {opentracing::ChildOf(&span->context())});

  RedisContext redis_ctx(_redis);
  std::vector<std::string> followees_str;
  std::string followees_key = std::format("{}:followees", user_id);
  try {
    ReadFromRedisZSet(redis_ctx, followees_key, followees_str);
  } catch (const Error &err) {
    LOG(error) << err.what();
    throw err;
  }
  redis_span->Finish();

  // If user_id in the social graph Redis server, read from Redis
  if (!followees_str.empty()) {
    for (auto const &followee_str : followees_str) {
      _return.emplace_back(std::stoul(followee_str));
    }
  }
  // If user_id not in the social graph Redis server, read from MongoDB and
  // update Redis.
  else {
    redis_span->Finish();
    ReadFolloweesFromMongo(_mongodb_client_pool, redis_ctx, user_id, _return,
                           span.get());
  }
  span->Finish();
}

void SocialGraphHandler::InsertUser(
    int64_t req_id, int64_t user_id,
    const std::map<std::string, std::string, std::less<>> &carrier) {
  std::map<std::string, std::string, std::less<>> writer_text_map;
  auto span = StartServerSpan("insert_user_server", carrier, writer_text_map);

  mongoc_client_t *mongodb_client = PopMongoClient(_mongodb_client_pool);
  auto collection = GetMongoCollection(_mongodb_client_pool, mongodb_client,
                                       "social-graph", "social-graph");

  bson_t *new_doc = BCON_NEW("user_id", BCON_INT64(user_id), "followers", "[",
                             "]", "followees", "[", "]");
  bson_error_t error;
  auto insert_span = opentracing::Tracer::Global()->StartSpan(
      "social_graph_mongo_insert_client",
      {opentracing::ChildOf(&span->context())});
  bool inserted = mongoc_collection_insert_one(collection, new_doc, nullptr,
                                               nullptr, &error);
  insert_span->Finish();
  if (!inserted) {
    LOG(error) << "Failed to insert social graph for user " << user_id
               << " to MongoDB: " << error.message;
    ServiceException se;
    se.errorCode = ErrorCode::SE_MONGODB_ERROR;
    se.message = error.message;
    bson_destroy(new_doc);
    mongoc_collection_destroy(collection);
    mongoc_client_pool_push(_mongodb_client_pool, mongodb_client);
    throw se;
  }
  bson_destroy(new_doc);
  mongoc_collection_destroy(collection);
  mongoc_client_pool_push(_mongodb_client_pool, mongodb_client);
  span->Finish();
}

int64_t SocialGraphHandler::_GetUserId(
    int64_t req_id, const std::string &name,
    const std::map<std::string, std::string, std::less<>> &writer_text_map) {
  auto user_client_wrapper = _user_service_client_pool->Pop();
  if (!user_client_wrapper) {
    ServiceException se;
    se.errorCode = ErrorCode::SE_THRIFT_CONN_ERROR;
    se.message = "Failed to connect to social-graph-service";
    throw se;
  }
  auto user_client = user_client_wrapper->GetClient();
  int64_t _return;
  try {
    _return = user_client->GetUserId(req_id, name, writer_text_map);
  } catch (...) {
    _user_service_client_pool->Remove(user_client_wrapper);
    LOG(error) << "Failed to get user_id from user-service";
    throw;
  }
  _user_service_client_pool->Keepalive(user_client_wrapper);
  return _return;
}

void SocialGraphHandler::FollowWithUsername(
    int64_t req_id, const std::string &user_name,
    const std::string &followee_name,
    const std::map<std::string, std::string, std::less<>> &carrier) {
  std::map<std::string, std::string, std::less<>> writer_text_map;
  auto span = StartServerSpan("follow_with_username_server", carrier,
                              writer_text_map);

  int64_t user_id;
  int64_t followee_id;
  try {
    user_id = _GetUserId(req_id, user_name, writer_text_map);
    followee_id = _GetUserId(req_id, followee_name, writer_text_map);
  } catch (const std::exception &e) {
    LOG(warning) << e.what();
    throw;
  }

  if (user_id >= 0 && followee_id >= 0) {
    Follow(req_id, user_id, followee_id, writer_text_map);
  }
  span->Finish();
}

void SocialGraphHandler::UnfollowWithUsername(
    int64_t req_id, const std::string &user_name,
    const std::string &followee_name,
    const std::map<std::string, std::string, std::less<>> &carrier) {
  std::map<std::string, std::string, std::less<>> writer_text_map;
  auto span = StartServerSpan("unfollow_with_username_server", carrier,
                              writer_text_map);

  int64_t user_id = _GetUserId(req_id, user_name, writer_text_map);
  int64_t followee_id = _GetUserId(req_id, followee_name, writer_text_map);

  if (user_id >= 0 && followee_id >= 0) {
    Unfollow(req_id, user_id, followee_id, writer_text_map);
  }
  span->Finish();
}

}  // namespace social_network

#endif  // SOCIAL_NETWORK_MICROSERVICES_SOCIALGRAPHHANDLER_H
