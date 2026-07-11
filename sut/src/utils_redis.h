#ifndef SOCIAL_NETWORK_MICROSERVICES_SRC_UTILS_REDIS_H_
#define SOCIAL_NETWORK_MICROSERVICES_SRC_UTILS_REDIS_H_

#include <sw/redis++/redis++.h>
#include <chrono>

using namespace sw::redis;
namespace social_network {

// Builds connection and pool options from the config JSON for a named Redis
// service (keys: <service_name>-redis.{addr,port,connections,timeout_ms,keepalive_ms}).
inline void BuildRedisOptions(
    const json &config_json,
    const std::string &config_key,
    ConnectionOptions &connection_options,
    ConnectionPoolOptions &pool_options
) {
  connection_options.host = config_json[config_key]["addr"];
  connection_options.port = config_json[config_key]["port"];

  if (config_json["ssl"]["enabled"]) {
    std::string ca_file = config_json["ssl"]["caPath"];
    connection_options.tls.enabled = true;
    connection_options.tls.cacert = ca_file.c_str();
  }

  pool_options.size = config_json[config_key]["connections"];
  pool_options.wait_timeout =
      std::chrono::milliseconds(config_json[config_key]["timeout_ms"]);
  pool_options.connection_lifetime =
      std::chrono::milliseconds(config_json[config_key]["keepalive_ms"]);
}

Redis init_redis_client_pool(
    const json &config_json,
    const std::string &service_name
) {
  ConnectionOptions connection_options;
  ConnectionPoolOptions pool_options;
  BuildRedisOptions(config_json, service_name + "-redis",
                    connection_options, pool_options);
  return Redis(connection_options, pool_options);
}

RedisCluster init_redis_cluster_client_pool(
    const json &config_json,
    const std::string &service_name
) {
  ConnectionOptions connection_options;
  ConnectionPoolOptions pool_options;
  BuildRedisOptions(config_json, service_name + "-redis",
                    connection_options, pool_options);
  return RedisCluster(connection_options, pool_options);
}

Redis init_redis_replica_client_pool(
    const json& config_json,
    const std::string& service_name
) {
  ConnectionOptions connection_options;
  ConnectionPoolOptions pool_options;
  BuildRedisOptions(config_json, service_name,
                    connection_options, pool_options);
  return Redis(connection_options, pool_options);
}

// ---------------------------------------------------------------------------
// Redis pool set: groups the four Redis pool pointers used by handlers that
// support standalone, cluster, and replica Redis deployments.
// ---------------------------------------------------------------------------

// Holds all four Redis pool pointer variants.  Exactly one of the three
// deployment modes should be active at construction time:
//   - standalone:   client != nullptr, replica == nullptr, primary == nullptr, cluster == nullptr
//   - replication:  client == nullptr, replica != nullptr, primary != nullptr, cluster == nullptr
//   - cluster:      client == nullptr, replica == nullptr, primary == nullptr, cluster != nullptr
struct RedisPoolSet {
  Redis        *client  = nullptr;
  Redis        *replica = nullptr;
  Redis        *primary = nullptr;
  RedisCluster *cluster = nullptr;

  // Returns true when the Redis replica/primary pair is active.
  [[nodiscard]] bool IsReplicationEnabled() const {
    return primary != nullptr || replica != nullptr;
  }

  // Factory helpers — one per deployment mode.

  static RedisPoolSet Standalone(Redis *pool) {
    return RedisPoolSet{pool, nullptr, nullptr, nullptr};
  }

  static RedisPoolSet Replication(Redis *replica_pool, Redis *primary_pool) {
    return RedisPoolSet{nullptr, replica_pool, primary_pool, nullptr};
  }

  static RedisPoolSet Cluster(RedisCluster *cluster_pool) {
    return RedisPoolSet{nullptr, nullptr, nullptr, cluster_pool};
  }
};

} // namespace social_network

#endif //SOCIAL_NETWORK_MICROSERVICES_SRC_UTILS_REDIS_H_
