#include "../ClientPool.h"
#include "../logger.h"
#include "../tracing.h"
#include "../utils.h"
#include "../utils_redis.h"
#include "../utils_service.h"
#include "../utils_thrift.h"
#include "HomeTimelineHandler.h"

using namespace social_network;

int main(int argc, char *argv[]) {
  signal(SIGINT, simple_sigint_handler);

  bool redis_cluster_flag = false;
  json config_json;
  if (!init_service_main_with_redis(argc, argv, redis_cluster_flag,
                                    "home-timeline-service", config_json)) {
    return 0;
  }

  int port = config_json["home-timeline-service"]["port"];
  int redis_cluster_config_flag = config_json["home-timeline-redis"]["use_cluster"];
  int redis_replica_config_flag = config_json["home-timeline-redis"]["use_replica"];

  PostStorageClientConfig ps_cfg = read_post_storage_client_config(config_json);

  int social_graph_port = config_json["social-graph-service"]["port"];
  std::string social_graph_addr = config_json["social-graph-service"]["addr"];
  int social_graph_conns = config_json["social-graph-service"]["connections"];
  int social_graph_timeout = config_json["social-graph-service"]["timeout_ms"];
  int social_graph_keepalive =
      config_json["social-graph-service"]["keepalive_ms"];

  check_redis_conflict(redis_replica_config_flag, redis_cluster_config_flag,
                       redis_cluster_flag);

  ClientPool<ThriftClient<PostStorageServiceClient>> post_storage_client_pool(
      "post-storage-client", ps_cfg.addr, ps_cfg.port, 0,
      ps_cfg.connections, ps_cfg.timeout_ms, ps_cfg.keepalive_ms,
      config_json);

  ClientPool<ThriftClient<SocialGraphServiceClient>> social_graph_client_pool(
      "social-graph-client", social_graph_addr, social_graph_port, 0,
      social_graph_conns, social_graph_timeout, social_graph_keepalive,
      config_json);

  std::shared_ptr<TServerSocket> server_socket =
      get_server_socket(config_json, "0.0.0.0", port);

  if (redis_replica_config_flag) {
    Redis redis_replica_client_pool = init_redis_replica_client_pool(config_json, "redis-replica");
    Redis redis_primary_client_pool = init_redis_replica_client_pool(config_json, "redis-primary");
    start_thrift_server(
        std::make_shared<HomeTimelineServiceProcessor>(
            std::make_shared<HomeTimelineHandler>(&redis_replica_client_pool,
                                                  &redis_primary_client_pool,
                                                  &post_storage_client_pool,
                                                  &social_graph_client_pool)),
        server_socket,
        "Starting the home-timeline-service server with replicated Redis support...");
  } else if (redis_cluster_flag || redis_cluster_config_flag) {
    RedisCluster redis_cluster_client_pool =
        init_redis_cluster_client_pool(config_json, "home-timeline");
    start_thrift_server(
        std::make_shared<HomeTimelineServiceProcessor>(
            std::make_shared<HomeTimelineHandler>(&redis_cluster_client_pool,
                                                  &post_storage_client_pool,
                                                  &social_graph_client_pool)),
        server_socket,
        "Starting the home-timeline-service server with Redis Cluster support...");
  } else {
    Redis redis_client_pool =
        init_redis_client_pool(config_json, "home-timeline");
    start_thrift_server(
        std::make_shared<HomeTimelineServiceProcessor>(
            std::make_shared<HomeTimelineHandler>(&redis_client_pool,
                                                  &post_storage_client_pool,
                                                  &social_graph_client_pool)),
        server_socket,
        "Starting the home-timeline-service server...");
  }
}
