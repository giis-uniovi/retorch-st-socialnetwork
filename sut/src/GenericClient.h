#ifndef SOCIAL_NETWORK_MICROSERVICES_GENERICCLIENT_H
#define SOCIAL_NETWORK_MICROSERVICES_GENERICCLIENT_H

#include <string>
#include <string_view>
#include <chrono>

namespace social_network {

class GenericClient{
 public:
  virtual ~GenericClient() = default;
  virtual void Connect() = 0;
  virtual void Disconnect() = 0;
  virtual bool IsConnected() = 0;

  long connect_timestamp() const { return _connect_timestamp; }
  long keepalive_ms() const { return _keepalive_ms; }

 protected:
  GenericClient(const std::string &addr, int port)
      : _addr(addr), _port(port) {}

  const std::string &addr() const { return _addr; }
  int port() const { return _port; }
  void set_addr(std::string_view a) { _addr = std::string(a); }
  void set_port(int p) { _port = p; }
  void set_connect_timestamp(long t) { _connect_timestamp = t; }
  void set_keepalive_ms(long ms) { _keepalive_ms = ms; }

 private:
  long _connect_timestamp = 0;
  long _keepalive_ms = 0;
  std::string _addr;
  int _port;
};

} // namespace social_network

#endif //SOCIAL_NETWORK_MICROSERVICES_GENERICCLIENT_H
