#ifndef SOCIAL_NETWORK_MICROSERVICES_SRC_UTILS_MEMCACHED_SERVICE_H_
#define SOCIAL_NETWORK_MICROSERVICES_SRC_UTILS_MEMCACHED_SERVICE_H_

#include <memory>
#include <string>

#include "utils_memcached.h"
#include "utils_service.h"

namespace social_network {

// Runs the complete main() of a stateless Thrift service backed by a memcached
// pool and a MongoDB pool (PostStorageService, UrlShortenService and
// UserMentionService share this exact shape). The per-service differences are
// passed in: the config keys, an optional MongoDB index to create, and a
// factory that builds the concrete handler from the two pools.
//
//   make_handler: callable (memcached_pool_st*, mongoc_client_pool_t*)
//                 -> std::shared_ptr<Handler>
//
// If index_db is empty no index is created. Never returns on success
// (start_thrift_server blocks); returns EXIT_FAILURE on a startup error.
//
// This lives in its own header (rather than utils_service.h) so that services
// which do NOT use memcached are not forced to link against libmemcached.
template <typename Processor, typename MakeHandler>
inline int run_memcached_mongo_service(const std::string &service_name,
                                       const std::string &pool_name,
                                       const std::string &mongo_conns_key,
                                       const std::string &memcached_conns_key,
                                       const std::string &index_db,
                                       const std::string &index_field,
                                       MakeHandler make_handler) {
  json config_json;
  init_service_main(service_name, config_json);

  int port = config_json[service_name]["port"];
  int mongodb_conns = config_json[mongo_conns_key]["connections"];
  int memcached_conns = config_json[memcached_conns_key]["connections"];

  memcached_pool_st *memcached_client_pool;
  mongoc_client_pool_t *mongodb_client_pool;
  if (!init_and_validate_pools(config_json, pool_name, 32, memcached_conns,
                               mongodb_conns, memcached_client_pool,
                               mongodb_client_pool)) {
    return EXIT_FAILURE;
  }
  register_memcached_mongo_sigint_handler(memcached_client_pool,
                                          mongodb_client_pool);

  if (!index_db.empty() &&
      create_mongodb_index_with_retry(mongodb_client_pool, index_db,
                                      index_field) != EXIT_SUCCESS) {
    return EXIT_FAILURE;
  }

  std::shared_ptr<TServerSocket> server_socket =
      get_server_socket(config_json, "0.0.0.0", port);
  start_thrift_server(
      std::make_shared<Processor>(
          make_handler(memcached_client_pool, mongodb_client_pool)),
      server_socket, "Starting the " + service_name + " server...");
  return EXIT_SUCCESS;  // unreachable: start_thrift_server is [[noreturn]]
}

}  // namespace social_network

#endif  // SOCIAL_NETWORK_MICROSERVICES_SRC_UTILS_MEMCACHED_SERVICE_H_
