#ifndef SOCIAL_NETWORK_MICROSERVICES_SRC_UTILS_ID_H_
#define SOCIAL_NETWORK_MICROSERVICES_SRC_UTILS_ID_H_

#include <chrono>
#include <cstddef>
#include <format>
#include <fstream>
#include <sstream>
#include <string>

#include "logger.h"

// Custom Epoch (January 1, 2018 Midnight GMT = 2018-01-01T00:00:00Z)
static constexpr long long CUSTOM_EPOCH = 1514764800000LL;

namespace social_network {

using std::chrono::duration_cast;
using std::chrono::milliseconds;
using std::chrono::system_clock;

// Returns a per-thread monotonically increasing counter for a given timestamp.
// Aborts if the clock goes backwards.
static int GetCounter(int64_t timestamp) {
  static thread_local int64_t current_timestamp = -1;
  static thread_local int counter = 0;
  if (current_timestamp > timestamp) {
    LOG(fatal) << "Timestamps are not incremental.";
    exit(EXIT_FAILURE);
  }
  if (current_timestamp == timestamp) {
    return counter++;
  } else {
    current_timestamp = timestamp;
    counter = 0;
    return counter++;
  }
}

// Composes a Snowflake-style hex ID string from the given machine_id,
// current timestamp and per-thread counter, then converts it to int64_t.
inline int64_t ComposeSnowflakeId(const std::string &machine_id,
                                  std::mutex *thread_lock) {
  thread_lock->lock();
  int64_t timestamp =
      duration_cast<milliseconds>(system_clock::now().time_since_epoch())
          .count() -
      CUSTOM_EPOCH;
  int idx = GetCounter(timestamp);
  thread_lock->unlock();

  std::stringstream sstream;
  sstream << std::hex << timestamp;
  std::string timestamp_hex(sstream.str());

  if (timestamp_hex.size() > 10) {
    timestamp_hex.erase(0, timestamp_hex.size() - 10);
  } else if (timestamp_hex.size() < 10) {
    timestamp_hex = std::string(10 - timestamp_hex.size(), '0') + timestamp_hex;
  }

  sstream.clear();
  sstream.str(std::string());

  sstream << std::hex << idx;
  std::string counter_hex(sstream.str());

  if (counter_hex.size() > 3) {
    counter_hex.erase(0, counter_hex.size() - 3);
  } else if (counter_hex.size() < 3) {
    counter_hex = std::string(3 - counter_hex.size(), '0') + counter_hex;
  }

  std::string id_str = machine_id + timestamp_hex + counter_hex;
  return stoul(id_str, nullptr, 16) & 0x7FFFFFFFFFFFFFFF;
}

/*
 * The following code which obtains machine ID from machine's MAC address was
 * inspired from https://stackoverflow.com/a/16859693.
 *
 * MAC address is obtained from /sys/class/net/<netif>/address
 */
inline u_int16_t HashMacAddressPid(const std::string &mac) {
  u_int16_t hash = 0;
  std::string mac_pid = std::format("{}{}", mac, getpid());
  for (unsigned int i = 0; i < mac_pid.size(); i++) {
    hash += static_cast<u_int16_t>(
        std::to_integer<uint16_t>(static_cast<std::byte>(mac[i]))
        << ((i & 1) * 8));
  }
  return hash;
}

inline std::string GetMachineId(std::string &netif) {
  std::string mac_hash;

  std::string mac_addr_filename = "/sys/class/net/" + netif + "/address";
  std::ifstream mac_addr_file;
  mac_addr_file.open(mac_addr_filename);
  if (!mac_addr_file) {
    LOG(fatal) << "Cannot read MAC address from net interface " << netif;
    return "";
  }
  std::string mac;
  mac_addr_file >> mac;
  if (mac == "") {
    LOG(fatal) << "Cannot read MAC address from net interface " << netif;
    return "";
  }
  mac_addr_file.close();

  LOG(info) << "MAC address = " << mac;

  std::stringstream stream;
  stream << std::hex << HashMacAddressPid(mac);
  mac_hash = stream.str();

  if (mac_hash.size() > 3) {
    mac_hash.erase(0, mac_hash.size() - 3);
  } else if (mac_hash.size() < 3) {
    mac_hash = std::string(3 - mac_hash.size(), '0') + mac_hash;
  }
  return mac_hash;
}

}  // namespace social_network

#endif  // SOCIAL_NETWORK_MICROSERVICES_SRC_UTILS_ID_H_
