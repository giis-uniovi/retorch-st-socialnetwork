/*
 * 64-bit Unique Id Generator
 *
 * ------------------------------------------------------------------------
 * |0| 11 bit machine ID |      40-bit timestamp         | 12-bit counter |
 * ------------------------------------------------------------------------
 *
 * 11-bit machine Id code by hasing the MAC address
 * 40-bit UNIX timestamp in millisecond precision with custom epoch
 * 12 bit counter which increases monotonically on single process
 *
 */

#include <mutex>

#include "../utils_service.h"
#include "UniqueIdHandler.h"

using namespace social_network;

int main(int argc, char *argv[]) {
  signal(SIGINT, simple_sigint_handler);
  json config_json;
  init_service_main("unique-id-service", config_json);

  int port = config_json["unique-id-service"]["port"];
  std::string netif = config_json["unique-id-service"]["netif"];

  std::string machine_id = GetMachineId(netif);
  if (machine_id.empty()) {
    exit(EXIT_FAILURE);
  }
  LOG(info) << "machine_id = " << machine_id;

  std::mutex thread_lock;
  std::shared_ptr<TServerSocket> server_socket =
      get_server_socket(config_json, "0.0.0.0", port);

  start_thrift_server(
      std::make_shared<UniqueIdServiceProcessor>(
          std::make_shared<UniqueIdHandler>(&thread_lock, machine_id)),
      server_socket, "Starting the unique-id-service server ...");
}
