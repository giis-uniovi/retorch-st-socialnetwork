#include "../utils.h"
#include "../utils_mongodb.h"
#include "../utils_redis.h"
#include "../utils_service.h"
#include "../utils_thrift.h"
#include "SocialGraphHandler.h"

using namespace social_network;

int main(int argc, char *argv[]) {
  signal(SIGINT, simple_sigint_handler);

  bool redis_cluster_flag = false;
  json config_json;
  if (!init_service_main_with_redis(argc, argv, redis_cluster_flag,
                                    "social-graph-service", config_json)) {
    return 0;
  }

  int port = config_json["social-graph-service"]["port"];

  int mongodb_conns = config_json["social-graph-mongodb"]["connections"];

  std::string user_addr = config_json["user-service"]["addr"];
  int user_port = config_json["user-service"]["port"];
  int user_conns = config_json["user-service"]["connections"];
  int user_timeout = config_json["user-service"]["timeout_ms"];
  int user_keepalive = config_json["user-service"]["keepalive_ms"];

  int redis_cluster_config_flag = config_json["social-graph-redis"]["use_cluster"];
  int redis_replica_config_flag = config_json["social-graph-redis"]["use_replica"];

  mongoc_client_pool_t *mongodb_client_pool =
      init_mongodb_client_pool(config_json, "social-graph", mongodb_conns);
  if (mongodb_client_pool == nullptr) {
    return EXIT_FAILURE;
  }

  check_redis_conflict(redis_replica_config_flag, redis_cluster_config_flag,
                       redis_cluster_flag);

  ClientPool<ThriftClient<UserServiceClient>> user_client_pool(
      "social-graph", user_addr, user_port, 0, user_conns, user_timeout,
      user_keepalive, config_json);

  if (create_mongodb_index_with_retry(mongodb_client_pool, "social-graph", "user_id") != EXIT_SUCCESS) {
    return EXIT_FAILURE;
  }

  std::shared_ptr<TServerSocket> server_socket =
      get_server_socket(config_json, "0.0.0.0", port);

  if (redis_cluster_flag || redis_cluster_config_flag) {
    RedisCluster redis_cluster_client_pool =
        init_redis_cluster_client_pool(config_json, "social-graph");
    start_thrift_server(
        std::make_shared<SocialGraphServiceProcessor>(
            std::make_shared<SocialGraphHandler>(mongodb_client_pool,
                                                 &redis_cluster_client_pool,
                                                 &user_client_pool)),
        server_socket,
        "Starting the social-graph-service server with Redis Cluster support...");
  } else if (redis_replica_config_flag) {
    Redis redis_replica_client_pool = init_redis_replica_client_pool(config_json, "redis-replica");
    Redis redis_primary_client_pool = init_redis_replica_client_pool(config_json, "redis-primary");
    start_thrift_server(
        std::make_shared<SocialGraphServiceProcessor>(
            std::make_shared<SocialGraphHandler>(
                mongodb_client_pool, &redis_replica_client_pool, &redis_primary_client_pool, &user_client_pool)),
        server_socket,
        "Starting the social-graph-service server with Redis replica support");
  } else {
    Redis redis_client_pool =
        init_redis_client_pool(config_json, "social-graph");
    start_thrift_server(
        std::make_shared<SocialGraphServiceProcessor>(
            std::make_shared<SocialGraphHandler>(
                mongodb_client_pool, &redis_client_pool, &user_client_pool)),
        server_socket,
        "Starting the social-graph-service server ...");
  }
}
