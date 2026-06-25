#include "../utils.h"
#include "../utils_memcached.h"
#include "../utils_service.h"
#include "../utils_thrift.h"
#include "UrlShortenHandler.h"

using namespace social_network;

int main(int argc, char* argv[]) {
  json config_json;
  init_service_main("url-shorten-service", config_json);

  int port          = config_json["url-shorten-service"]["port"];
  int mongodb_conns  = config_json["url-shorten-mongodb"]["connections"];
  int memcached_conns = config_json["url-shorten-memcached"]["connections"];

  memcached_pool_st *memcached_client_pool;
  mongoc_client_pool_t *mongodb_client_pool;
  if (!init_and_validate_pools(config_json, "url-shorten", 32, memcached_conns,
                               mongodb_conns,
                               memcached_client_pool, mongodb_client_pool)) {
    return EXIT_FAILURE;
  }
  register_memcached_mongo_sigint_handler(memcached_client_pool,
                                          mongodb_client_pool);

  if (create_mongodb_index_with_retry(mongodb_client_pool, "url-shorten", "shortened_url") != EXIT_SUCCESS) {
    return EXIT_FAILURE;
  }

  std::mutex thread_lock;
  std::shared_ptr<TServerSocket> server_socket = get_server_socket(config_json, "0.0.0.0", port);

  start_thrift_server(
      std::make_shared<UrlShortenServiceProcessor>(
          std::make_shared<UrlShortenHandler>(
              memcached_client_pool, mongodb_client_pool, &thread_lock)),
      server_socket,
      "Starting the url-shorten-service server...");
}
