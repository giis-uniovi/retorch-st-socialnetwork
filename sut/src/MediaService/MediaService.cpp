#include <signal.h>

#include "../utils.h"
#include "../utils_service.h"
#include "../utils_thrift.h"
#include "MediaHandler.h"

using namespace social_network;

[[noreturn]] void sigintHandler(int) { exit(EXIT_SUCCESS); }

int main(int argc, char *argv[]) {
  signal(SIGINT, sigintHandler);
  init_logger();
  SetUpTracer("config/jaeger-config.yml", "media-service");
  json config_json;
  if (load_config_file("config/service-config.json", &config_json) != 0) {
    exit(EXIT_FAILURE);
  }

  int port = config_json["media-service"]["port"];
  std::shared_ptr<TServerSocket> server_socket = get_server_socket(config_json, "0.0.0.0", port);

  start_thrift_server(
      std::make_shared<MediaServiceProcessor>(std::make_shared<MediaHandler>()),
      server_socket,
      "Starting the media-service server...");
}
