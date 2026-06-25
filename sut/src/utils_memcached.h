#include <format>
#ifndef SOCIAL_NETWORK_MICROSERVICES_SRC_UTILS_MEMCACHED_H_
#define SOCIAL_NETWORK_MICROSERVICES_SRC_UTILS_MEMCACHED_H_

#include <signal.h>
#include <libmemcached/memcached.h>
#include <libmemcached/util.h>
#include <mongoc.h>
#include "utils_mongodb.h"

namespace social_network {

memcached_pool_st *init_memcached_client_pool(
    const json &config_json,
    const std::string &service_name,
    uint32_t min_size,
    uint32_t max_size
) {
  std::string addr = config_json[service_name + "-memcached"]["addr"];
  int port = config_json[service_name + "-memcached"]["port"];
  int use_binary_protocol = config_json[service_name + "-memcached"]["binary_protocol"];
  std::string config_str = std::format("--SERVER={}:{}", addr, port);
  auto memcached_client = memcached(config_str.c_str(), config_str.length());
  memcached_behavior_set(memcached_client, MEMCACHED_BEHAVIOR_NO_BLOCK, 1);
  memcached_behavior_set(memcached_client, MEMCACHED_BEHAVIOR_TCP_NODELAY, 1);
  if (use_binary_protocol == 1) {
    memcached_behavior_set(memcached_client, MEMCACHED_BEHAVIOR_BINARY_PROTOCOL, 1);
  }

  auto memcached_client_pool =
      memcached_pool_create(memcached_client, min_size, max_size);
  return memcached_client_pool;
}

// Destroys both pool types, guarding against nulls.
inline void destroy_memcached_and_mongodb_pools(
    memcached_pool_st *memcached_pool,
    mongoc_client_pool_t *mongodb_pool) {
  if (memcached_pool != nullptr) memcached_pool_destroy(memcached_pool);
  if (mongodb_pool != nullptr)   mongoc_client_pool_destroy(mongodb_pool);
}

// Shared SIGINT handler state for services that own both pools.
namespace detail {
  inline memcached_pool_st    *g_sigint_memcached_pool = nullptr;
  inline mongoc_client_pool_t *g_sigint_mongodb_pool   = nullptr;
}

[[noreturn]] inline void memcached_mongo_sigint_handler(int) {
  destroy_memcached_and_mongodb_pools(detail::g_sigint_memcached_pool,
                                      detail::g_sigint_mongodb_pool);
  std::exit(EXIT_SUCCESS);
}

// Registers the shared SIGINT handler and stores the pool pointers.
inline void register_memcached_mongo_sigint_handler(
    memcached_pool_st    *memcached_pool,
    mongoc_client_pool_t *mongodb_pool) {
  detail::g_sigint_memcached_pool = memcached_pool;
  detail::g_sigint_mongodb_pool   = mongodb_pool;
  signal(SIGINT, memcached_mongo_sigint_handler);
}

// Initialises and validates both pools in one call.
inline bool init_and_validate_pools(
    const json &config_json,
    const std::string &service_key,
    uint32_t memcached_min_size,
    uint32_t memcached_max_size,
    uint32_t mongodb_max_size,
    memcached_pool_st *&out_memcached_pool,
    mongoc_client_pool_t *&out_mongodb_pool) {
  out_memcached_pool = init_memcached_client_pool(
      config_json, service_key, memcached_min_size, memcached_max_size);
  out_mongodb_pool =
      init_mongodb_client_pool(config_json, service_key, mongodb_max_size);
  return (out_memcached_pool != nullptr) && (out_mongodb_pool != nullptr);
}

} // namespace social_network

#endif //SOCIAL_NETWORK_MICROSERVICES_SRC_UTILS_MEMCACHED_H_
