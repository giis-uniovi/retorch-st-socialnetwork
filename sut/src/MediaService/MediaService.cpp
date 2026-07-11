#include "../utils_service.h"
#include "MediaHandler.h"

using namespace social_network;

int main(int argc, char *argv[]) {
  signal(SIGINT, simple_sigint_handler);
  json config_json;
  init_service_main("media-service", config_json);

  int port = config_json["media-service"]["port"];
  std::shared_ptr<TServerSocket> server_socket =
      get_server_socket(config_json, "0.0.0.0", port);

  start_thrift_server(
      std::make_shared<MediaServiceProcessor>(std::make_shared<MediaHandler>()),
      server_socket, "Starting the media-service server...");
}
