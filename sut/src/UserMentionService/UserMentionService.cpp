#include "../utils.h"
#include "../utils_memcached.h"
#include "../utils_service.h"
#include "../utils_thrift.h"
#include "UserMentionHandler.h"

using namespace social_network;

int main(int argc, char* argv[]) {
  json config_json;
  init_service_main("user-mention-service", config_json);

  int port          = config_json["user-mention-service"]["port"];
  int mongodb_conns  = config_json["user-mongodb"]["connections"];
  int memcached_conns = config_json["user-memcached"]["connections"];

  memcached_pool_st *memcached_client_pool;
  mongoc_client_pool_t *mongodb_client_pool;
  if (!init_and_validate_pools(config_json, "user", 32, memcached_conns,
                               mongodb_conns,
                               memcached_client_pool, mongodb_client_pool)) {
    return EXIT_FAILURE;
  }
  register_memcached_mongo_sigint_handler(memcached_client_pool,
                                          mongodb_client_pool);

  std::shared_ptr<TServerSocket> server_socket = get_server_socket(config_json, "0.0.0.0", port);

  start_thrift_server(
      std::make_shared<UserMentionServiceProcessor>(
          std::make_shared<UserMentionHandler>(
              memcached_client_pool, mongodb_client_pool)),
      server_socket,
      "Starting the user-mention-service server...");
}
