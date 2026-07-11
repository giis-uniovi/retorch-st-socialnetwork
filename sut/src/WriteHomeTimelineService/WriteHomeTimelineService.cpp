
#include <cpp_redis/cpp_redis>
#include <csignal>
#include <mutex>
#include <set>
#include <sstream>
#include <thread>
#include <stop_token>

#include "../../gen-cpp/SocialGraphService.h"
#include "../../gen-cpp/social_network_types.h"
#include "../AmqpLibeventHandler.h"
#include "../ClientPool.h"
#include "../RedisClient.h"
#include "../ThriftClient.h"
#include "../logger.h"
#include "../tracing.h"
#include "../utils.h"

using namespace social_network;

[[noreturn]] void sigintHandler(int) { exit(EXIT_SUCCESS); }

static void GetFollowersWithErrorHandling(
    SocialGraphServiceClient *social_graph_client,
    ClientPool<ThriftClient<SocialGraphServiceClient>> *social_graph_client_pool,
    std::shared_ptr<ThriftClient<SocialGraphServiceClient>> &social_graph_client_wrapper,
    std::vector<int64_t> &followers_id, int64_t req_id, int64_t user_id,
    std::map<std::string, std::string, std::less<>> &writer_text_map) {
  try {
    social_graph_client->GetFollowers(followers_id, req_id, user_id,
                                      writer_text_map);
  } catch (...) {
    LOG(error) << "Failed to get followers from social-network-service";
    social_graph_client_pool->Remove(social_graph_client_wrapper);
    throw;
  }
}

void OnReceivedWorker(
    const AMQP::Message &msg,
    ClientPool<RedisClient> *redis_client_pool,
    ClientPool<ThriftClient<SocialGraphServiceClient>>
        *social_graph_client_pool) {
  try {
    json msg_json = json::parse(std::string(msg.body(), msg.bodySize()));

    std::map<std::string, std::string, std::less<>> carrier;
    for (const auto &[key, value] : msg_json["carrier"].items()) {
      carrier.try_emplace(key, value);
    }

    // Jaeger tracing
    TextMapReader span_reader(carrier);
    auto parent_span = opentracing::Tracer::Global()->Extract(span_reader);
    auto span = opentracing::Tracer::Global()->StartSpan(
        "write_home_timeline_server",
        {opentracing::ChildOf(parent_span->get())});

    // Extract information from rabbitmq messages
    int64_t user_id = msg_json["user_id"];
    int64_t req_id = msg_json["req_id"];
    int64_t post_id = msg_json["post_id"];
    int64_t timestamp = msg_json["timestamp"];
    std::vector<int64_t> user_mentions_id = msg_json["user_mentions_id"];

    // Find followers of the user
    auto followers_span = opentracing::Tracer::Global()->StartSpan(
        "get_followers_client", {opentracing::ChildOf(&span->context())});
    std::map<std::string, std::string, std::less<>> writer_text_map;
    TextMapWriter writer(writer_text_map);
    opentracing::Tracer::Global()->Inject(followers_span->context(), writer);

    auto social_graph_client_wrapper = social_graph_client_pool->Pop();
    if (!social_graph_client_wrapper) {
      ServiceException se;
      se.errorCode = ErrorCode::SE_THRIFT_CONN_ERROR;
      se.message = "Failed to connect to social-graph-service";
      throw se;
    }
    auto social_graph_client = social_graph_client_wrapper->GetClient();
    std::vector<int64_t> followers_id;
    GetFollowersWithErrorHandling(social_graph_client, social_graph_client_pool,
                                  social_graph_client_wrapper, followers_id,
                                  req_id, user_id, writer_text_map);
    social_graph_client_pool->Keepalive(social_graph_client_wrapper);
    followers_span->Finish();

    std::set<int64_t> followers_id_set(followers_id.begin(),
                                       followers_id.end());
    followers_id_set.insert(user_mentions_id.begin(), user_mentions_id.end());

    // Update Redis ZSet
    auto redis_span = opentracing::Tracer::Global()->StartSpan(
        "write_home_timeline_redis_update_client",
        {opentracing::ChildOf(&span->context())});
    auto redis_client_wrapper = redis_client_pool->Pop();
    if (!redis_client_wrapper) {
      ServiceException se;
      se.errorCode = ErrorCode::SE_REDIS_ERROR;
      se.message = "Cannot connect to Redis server";
      throw se;
    }
    auto redis_client = redis_client_wrapper->GetClient();
    std::vector<std::string> options{"NX"};
    std::string post_id_str = std::to_string(post_id);
    std::string timestamp_str = std::to_string(timestamp);
    std::multimap<std::string, std::string, std::less<>> value = {
        {timestamp_str, post_id_str}};

    for (auto &follower_id : followers_id_set) {
      redis_client->zadd(std::to_string(follower_id), options, value);
    }

    redis_client->sync_commit();
    redis_span->Finish();
    redis_client_pool->Keepalive(redis_client_wrapper);
  } catch (...) {
    LOG(error) << "OnReveived worker error";
    throw;
  }
}

void HeartbeatSend(AmqpLibeventHandler &handler,
                   AMQP::TcpConnection &connection, int interval) {
  while (handler.GetIsRunning()) {
    LOG(debug) << "Heartbeat sent";
    connection.heartbeat();
    sleep(interval);
  }
}

void WorkerThread(
    std::string &addr, int port,
    ClientPool<RedisClient> *redis_client_pool,
    ClientPool<ThriftClient<SocialGraphServiceClient>>
        *social_graph_client_pool) {
  AmqpLibeventHandler handler;
  AMQP::TcpConnection connection(
      handler, AMQP::Address(addr, port, AMQP::Login("guest", "guest"), "/"));
  AMQP::TcpChannel channel(&connection);
  channel.onError([&handler](const char *message) {
    LOG(error) << "Channel error: " << message;
    handler.Stop();
  });
  channel.declareQueue("write-home-timeline", AMQP::durable)
      .onSuccess([&connection](const std::string &name, uint32_t messagecount,
                               uint32_t consumercount) {
        LOG(debug) << "Created queue: " << name;
      });
  channel.consume("write-home-timeline", AMQP::noack)
      .onReceived([redis_client_pool, social_graph_client_pool](
                      const AMQP::Message &msg, uint64_t tag, bool redelivered) {
        LOG(debug) << "Received: " << std::string(msg.body(), msg.bodySize());
        OnReceivedWorker(msg, redis_client_pool, social_graph_client_pool);
      });

  std::jthread heartbeat_thread(HeartbeatSend, std::ref(handler),
                               std::ref(connection), 30);
  handler.Start();
  LOG(debug) << "Closing connection.";
  connection.close();
}

int main(int argc, char *argv[]) {
  signal(SIGINT, sigintHandler);
  init_logger();

  SetUpTracer("config/jaeger-config.yml", "write-home-timeline-service");

  json config_json;
  if (load_config_file("config/service-config.json", &config_json) != 0) {
    exit(EXIT_FAILURE);
  }

  int n_workers = config_json["write-home-timeline-service"]["workers"];

  std::string rabbitmq_addr =
      config_json["write-home-timeline-rabbitmq"]["addr"];
  int rabbitmq_port = config_json["write-home-timeline-rabbitmq"]["port"];

  std::string redis_addr = config_json["home-timeline-redis"]["addr"];
  int redis_port = config_json["home-timeline-redis"]["port"];
  int redis_conns = config_json["home-timeline-redis"]["connections"];
  int redis_timeout = config_json["home-timeline-redis"]["timeout_ms"];
  int redis_keepalive = config_json["home-timeline-redis"]["keepalive_ms"];

  std::string social_graph_service_addr =
      config_json["social-graph-service"]["addr"];
  int social_graph_service_port = config_json["social-graph-service"]["port"];
  int social_graph_service_conns =
      config_json["social-graph-service"]["connections"];
  int social_graph_service_timeout =
      config_json["social-graph-service"]["timeout_ms"];
  int social_graph_service_keepalive =
      config_json["social-graph-service"]["keepalive_ms"];

  ClientPool<RedisClient> redis_client_pool("redis", redis_addr, redis_port, 0,
                                            redis_conns, redis_timeout,
                                            redis_keepalive, config_json);

  ClientPool<ThriftClient<SocialGraphServiceClient>> social_graph_client_pool(
      "social-graph-service", social_graph_service_addr,
      social_graph_service_port, 0, social_graph_service_conns,
      social_graph_service_timeout, social_graph_service_keepalive, config_json);

  std::vector<std::unique_ptr<std::jthread>> threads_ptr(n_workers);
  for (auto &thread_ptr : threads_ptr) {
    thread_ptr = std::make_unique<std::jthread>(
        WorkerThread, std::ref(rabbitmq_addr), rabbitmq_port,
        &redis_client_pool, &social_graph_client_pool);
  }
  for (auto &thread_ptr : threads_ptr) {
    thread_ptr->join();
  }

  return 0;
}
