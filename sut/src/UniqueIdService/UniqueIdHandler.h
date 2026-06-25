#ifndef SOCIAL_NETWORK_MICROSERVICES_UNIQUEIDHANDLER_H
#define SOCIAL_NETWORK_MICROSERVICES_UNIQUEIDHANDLER_H

#include <iostream>
#include <mutex>
#include <string>

#include "../../gen-cpp/UniqueIdService.h"
#include "../../gen-cpp/social_network_types.h"
#include "../logger.h"
#include "../tracing.h"
#include "../utils_id.h"

namespace social_network {

class UniqueIdHandler : public UniqueIdServiceIf {
 public:
  ~UniqueIdHandler() override = default;
  UniqueIdHandler(std::mutex *, const std::string &);

  int64_t ComposeUniqueId(int64_t, PostType::type,
                          const std::map<std::string, std::string, std::less<>> &) override;

 private:
  std::mutex *_thread_lock;
  std::string _machine_id;
};

UniqueIdHandler::UniqueIdHandler(std::mutex *thread_lock,
                                 const std::string &machine_id)
    : _thread_lock(thread_lock), _machine_id(machine_id) {}

int64_t UniqueIdHandler::ComposeUniqueId(
    int64_t req_id, PostType::type post_type,
    const std::map<std::string, std::string, std::less<>> &carrier) {
  // Initialize a span
  TextMapReader reader(carrier);
  std::map<std::string, std::string, std::less<>> writer_text_map;
  TextMapWriter writer(writer_text_map);
  auto parent_span = opentracing::Tracer::Global()->Extract(reader);
  auto span = opentracing::Tracer::Global()->StartSpan(
      "compose_unique_id_server", {opentracing::ChildOf(parent_span->get())});
  opentracing::Tracer::Global()->Inject(span->context(), writer);

  int64_t post_id = ComposeSnowflakeId(_machine_id, _thread_lock);
  LOG(debug) << "The post_id of the request " << req_id << " is " << post_id;

  span->Finish();
  return post_id;
}

}  // namespace social_network

#endif  // SOCIAL_NETWORK_MICROSERVICES_UNIQUEIDHANDLER_H
