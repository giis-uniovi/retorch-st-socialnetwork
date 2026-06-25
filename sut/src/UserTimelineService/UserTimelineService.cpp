#include "../../gen-cpp/social_network_types.h"
#include "../ClientPool.h"
#include "../logger.h"
#include "../tracing.h"
#include "../utils.h"
#include "../utils_mongodb.h"
#include "../utils_redis.h"
#include "../utils_service.h"
#include "../utils_thrift.h"
#include "UserTimelineHandler.h"

using namespace social_network;

int main(int argc, char *argv[]) {
  signal(SIGINT, simple_sigint_handler);

  bool redis_cluster_flag = false;
  json config_json;
  if (!init_service_main_with_redis(argc, argv, redis_cluster_flag,
                                    "user-timeline-service", config_json)) {
    return 0;
  }

  int port = config_json["user-timeline-service"]["port"];

  PostStorageClientConfig ps_cfg = read_post_storage_client_config(config_json);

  int mongodb_conns = config_json["user-timeline-mongodb"]["connections"];

  int redis_cluster_config_flag = config_json["user-timeline-redis"]["use_cluster"];
  int redis_replica_config_flag = config_json["user-timeline-redis"]["use_replica"];

  auto mongodb_client_pool =
      init_mongodb_client_pool(config_json, "user-timeline", mongodb_conns);
  if (mongodb_client_pool == nullptr) {
    return EXIT_FAILURE;
  }

  check_redis_conflict(redis_replica_config_flag, redis_cluster_config_flag,
                       redis_cluster_flag);

  ClientPool<ThriftClient<PostStorageServiceClient>> post_storage_client_pool(
      "post-storage-client", ps_cfg.addr, ps_cfg.port, 0,
      ps_cfg.connections, ps_cfg.timeout_ms, ps_cfg.keepalive_ms,
      config_json);

  if (create_mongodb_index_with_retry(mongodb_client_pool, "user-timeline", "user_id") != EXIT_SUCCESS) {
    return EXIT_FAILURE;
  }

  std::shared_ptr<TServerSocket> server_socket =
      get_server_socket(config_json, "0.0.0.0", port);

  if (redis_cluster_flag || redis_cluster_config_flag) {
    RedisCluster redis_client_pool =
        init_redis_cluster_client_pool(config_json, "user-timeline");
    start_thrift_server(
        std::make_shared<UserTimelineServiceProcessor>(
            std::make_shared<UserTimelineHandler>(
                &redis_client_pool, mongodb_client_pool,
                &post_storage_client_pool)),
        server_socket,
        "Starting the user-timeline-service server with Redis Cluster support...");
  } else if (redis_replica_config_flag) {
    Redis redis_replica_client_pool = init_redis_replica_client_pool(config_json, "redis-replica");
    Redis redis_primary_client_pool = init_redis_replica_client_pool(config_json, "redis-primary");
    start_thrift_server(
        std::make_shared<UserTimelineServiceProcessor>(
            std::make_shared<UserTimelineHandler>(
                &redis_replica_client_pool, &redis_primary_client_pool,
                mongodb_client_pool, &post_storage_client_pool)),
        server_socket,
        "Starting the user-timeline-service server with replicated Redis support...");
  } else {
    Redis redis_client_pool =
        init_redis_client_pool(config_json, "user-timeline");
    start_thrift_server(
        std::make_shared<UserTimelineServiceProcessor>(
            std::make_shared<UserTimelineHandler>(
                &redis_client_pool, mongodb_client_pool,
                &post_storage_client_pool)),
        server_socket,
        "Starting the user-timeline-service server...");
  }
}
