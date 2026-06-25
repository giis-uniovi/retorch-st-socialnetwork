#ifndef SOCIAL_NETWORK_MICROSERVICES_SRC_UTILS_SERVICE_H_
#define SOCIAL_NETWORK_MICROSERVICES_SRC_UTILS_SERVICE_H_

#include <signal.h>

#include <memory>
#include <string>

#include <boost/program_options.hpp>
#include <nlohmann/json.hpp>
#include <thrift/protocol/TBinaryProtocol.h>
#include <thrift/server/TThreadedServer.h>
#include <thrift/transport/TBufferTransports.h>
#include <thrift/transport/TServerSocket.h>

#include "logger.h"
#include "tracing.h"
#include "utils.h"
#include "utils_mongodb.h"
#include "utils_thrift.h"

namespace social_network {

using json = nlohmann::json;

using apache::thrift::protocol::TBinaryProtocolFactory;
using apache::thrift::server::TThreadedServer;
using apache::thrift::transport::TFramedTransportFactory;
using apache::thrift::transport::TServerSocket;

// ---------------------------------------------------------------------------
// Signal handlers
// ---------------------------------------------------------------------------

// Minimal SIGINT handler for services that do not own heap-allocated pools.
// Register with: signal(SIGINT, simple_sigint_handler);
[[noreturn]] inline void simple_sigint_handler(int) { exit(EXIT_SUCCESS); }

// ---------------------------------------------------------------------------
// MongoDB helpers
// ---------------------------------------------------------------------------

// Pops a MongoDB client from the pool, creates a unique index on the given
// field with a retry-until-success loop, then pushes the client back.
// Returns EXIT_FAILURE if the client cannot be popped.
inline int create_mongodb_index_with_retry(
    mongoc_client_pool_t *mongodb_client_pool,
    const std::string &db_name,
    const std::string &index_field) {
  mongoc_client_t *mongodb_client = mongoc_client_pool_pop(mongodb_client_pool);
  if (!mongodb_client) {
    LOG(fatal) << "Failed to pop mongoc client";
    return EXIT_FAILURE;
  }
  bool r = false;
  while (!r) {
    r = CreateIndex(mongodb_client, db_name, index_field, true);
    if (!r) {
      LOG(error) << "Failed to create mongodb index, try again";
      sleep(1);
    }
  }
  mongoc_client_pool_push(mongodb_client_pool, mongodb_client);
  return EXIT_SUCCESS;
}

// ---------------------------------------------------------------------------
// Command-line argument helpers
// ---------------------------------------------------------------------------

// Parses the standard --redis-cluster / --help command-line options that are
// shared by SocialGraphService, HomeTimelineService, and UserTimelineService.
// Sets redis_cluster_flag to true when the option is present and true.
// Returns false if --help was requested (caller should return 0 immediately).
inline bool parse_redis_cluster_args(int argc, char *argv[],
                                     bool &redis_cluster_flag) {
  namespace po = boost::program_options;
  po::options_description desc("Options");
  desc.add_options()
      ("help", "produce help message")
      ("redis-cluster",
       po::value<bool>()->default_value(false)->implicit_value(true),
       "Enable redis cluster mode");

  po::variables_map vm;
  po::store(po::parse_command_line(argc, argv, desc), vm);
  po::notify(vm);

  if (vm.count("help")) {
    std::cout << desc << "\n";
    return false;
  }

  redis_cluster_flag = vm.count("redis-cluster") && vm["redis-cluster"].as<bool>();
  return true;
}

// ---------------------------------------------------------------------------
// Redis conflict check
// ---------------------------------------------------------------------------

// Validates that Redis Cluster and Redis Replica modes are not both active.
// Logs an error and calls exit(EXIT_FAILURE) if both are enabled.
inline void check_redis_conflict(int redis_replica_config_flag,
                                 int redis_cluster_config_flag,
                                 bool redis_cluster_flag) {
  if (redis_replica_config_flag &&
      (redis_cluster_config_flag || redis_cluster_flag)) {
    LOG(error) << "Can't start service when Redis Cluster and Redis Replica "
                  "are enabled at the same time";
    exit(EXIT_FAILURE);
  }
}

// ---------------------------------------------------------------------------
// Service main() bootstrapping helpers
// ---------------------------------------------------------------------------

// Bootstraps the common preamble for services that accept the --redis-cluster
// flag (SocialGraphService, HomeTimelineService, UserTimelineService).
// Calls init_logger(), parses argv for --redis-cluster, sets up the tracer,
// and loads the service-config.json.  Returns false when --help was requested
// (caller should immediately return 0); exits with EXIT_FAILURE when the
// config file cannot be loaded.
inline bool init_service_main_with_redis(int argc, char *argv[],
                                         bool &redis_cluster_flag,
                                         const std::string &tracer_name,
                                         json &config_json) {
  init_logger();
  if (!parse_redis_cluster_args(argc, argv, redis_cluster_flag)) {
    return false;
  }
  SetUpTracer("config/jaeger-config.yml", tracer_name);
  if (load_config_file("config/service-config.json", &config_json) != 0) {
    exit(EXIT_FAILURE);
  }
  return true;
}

// Bootstraps the common preamble for services that do NOT use redis-cluster
// args (PostStorageService, UrlShortenService, UserMentionService).
// Calls init_logger(), sets up the tracer, and loads service-config.json.
// Exits with EXIT_FAILURE when the config file cannot be loaded.
inline void init_service_main(const std::string &tracer_name,
                               json &config_json) {
  init_logger();
  SetUpTracer("config/jaeger-config.yml", tracer_name);
  if (load_config_file("config/service-config.json", &config_json) != 0) {
    exit(EXIT_FAILURE);
  }
}

// ---------------------------------------------------------------------------
// post-storage-service client configuration
// ---------------------------------------------------------------------------

// Connection parameters for the post-storage-service Thrift client pool,
// read from the service-config.json.  Shared by HomeTimelineService and
// UserTimelineService.
struct PostStorageClientConfig {
  std::string addr;
  int port;
  int connections;
  int timeout_ms;
  int keepalive_ms;
};

// Reads all post-storage-service connection parameters from config_json.
// Use the returned struct to construct a ClientPool<ThriftClient<PostStorageServiceClient>>.
inline PostStorageClientConfig read_post_storage_client_config(
    const json &config_json) {
  return {
      config_json["post-storage-service"]["addr"].get<std::string>(),
      config_json["post-storage-service"]["port"].get<int>(),
      config_json["post-storage-service"]["connections"].get<int>(),
      config_json["post-storage-service"]["timeout_ms"].get<int>(),
      config_json["post-storage-service"]["keepalive_ms"].get<int>()};
}

// ---------------------------------------------------------------------------
// Thrift server helpers
// ---------------------------------------------------------------------------

// Constructs and starts a TThreadedServer with standard framed/binary
// transport. The caller passes an already-constructed processor and a
// server socket obtained from get_server_socket(). This function never
// returns (server.serve() blocks until the process is killed).
template <typename Processor>
[[noreturn]] inline void start_thrift_server(
    std::shared_ptr<Processor> processor,
    std::shared_ptr<TServerSocket> server_socket,
    const std::string &log_message) {
  TThreadedServer server(
      processor,
      server_socket,
      std::make_shared<TFramedTransportFactory>(),
      std::make_shared<TBinaryProtocolFactory>());
  LOG(info) << log_message;
  server.serve();
  // server.serve() never returns for a running server; satisfy [[noreturn]].
  std::exit(EXIT_SUCCESS);
}

}  // namespace social_network

#endif  // SOCIAL_NETWORK_MICROSERVICES_SRC_UTILS_SERVICE_H_
