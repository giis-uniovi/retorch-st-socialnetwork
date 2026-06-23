#include <utility>

#ifndef SOCIAL_NETWORK_MICROSERVICES_TRACING_H
#define SOCIAL_NETWORK_MICROSERVICES_TRACING_H

#include <string>
#include <map>
#include <opentracing/propagation.h>
#include <opentracing/noop.h>
#include "logger.h"

namespace social_network {

using opentracing::expected;
using opentracing::string_view;

class TextMapReader : public opentracing::TextMapReader {
 public:
  explicit TextMapReader(const std::map<std::string, std::string, std::less<>> &text_map)
      : _text_map(text_map) {}

  expected<void> ForeachKey(
      std::function<expected<void>(string_view key, string_view value)> f)
  const override {
    for (const auto& [key, value] : _text_map) {
      auto result = f(key, value);
      if (!result) return result;
    }
    return {};
  }

 private:
  const std::map<std::string, std::string, std::less<>>& _text_map;
};

class TextMapWriter : public opentracing::TextMapWriter {
 public:
  explicit TextMapWriter(std::map<std::string, std::string, std::less<>> &text_map)
    : _text_map(text_map) {}

  expected<void> Set(string_view key, string_view value) const override {
    _text_map[key] = value;
    return {};
  }

 private:
  std::map<std::string, std::string, std::less<>>& _text_map;
};

// Initialise with a NoOp tracer so services compile and run without a Jaeger
// agent. Distributed traces are not collected but all service logic is intact.
void SetUpTracer(
    const std::string & /*config_file_path*/,
    const std::string & /*service*/) {
  opentracing::Tracer::InitGlobal(opentracing::MakeNoopTracer());
}

} //namespace social_network

#endif //SOCIAL_NETWORK_MICROSERVICES_TRACING_H
