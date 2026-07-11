#include "../utils_service.h"
#include "TextHandler.h"

using namespace social_network;

int main(int argc, char *argv[]) {
  signal(SIGINT, simple_sigint_handler);
  json config_json;
  init_service_main("text-service", config_json);

  int port = config_json["text-service"]["port"];

  std::string url_addr = config_json["url-shorten-service"]["addr"];
  int url_port = config_json["url-shorten-service"]["port"];
  int url_conns = config_json["url-shorten-service"]["connections"];
  int url_timeout = config_json["url-shorten-service"]["timeout_ms"];
  int url_keepalive = config_json["url-shorten-service"]["keepalive_ms"];

  std::string user_mention_addr = config_json["user-mention-service"]["addr"];
  int user_mention_port = config_json["user-mention-service"]["port"];
  int user_mention_conns = config_json["user-mention-service"]["connections"];
  int user_mention_timeout = config_json["user-mention-service"]["timeout_ms"];
  int user_mention_keepalive =
      config_json["user-mention-service"]["keepalive_ms"];

  ClientPool<ThriftClient<UrlShortenServiceClient>> url_client_pool(
      "url-shorten-service", url_addr, url_port, 0, url_conns, url_timeout,
      url_keepalive, config_json);

  ClientPool<ThriftClient<UserMentionServiceClient>> user_mention_pool(
      "user-mention-service", user_mention_addr, user_mention_port, 0,
      user_mention_conns, user_mention_timeout, user_mention_keepalive,
      config_json);

  std::shared_ptr<TServerSocket> server_socket =
      get_server_socket(config_json, "0.0.0.0", port);

  start_thrift_server(
      std::make_shared<TextServiceProcessor>(
          std::make_shared<TextHandler>(&url_client_pool, &user_mention_pool)),
      server_socket, "Starting the text-service server...");
}
