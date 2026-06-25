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

// Extract the incoming trace context from carrier, start a child server span,
// and inject the context into writer_text_map for downstream calls.
// Returns the started span. Use span->Finish() before the handler returns.
inline std::unique_ptr<opentracing::Span> StartServerSpan(
    const std::string &operation_name,
    const std::map<std::string, std::string, std::less<>> &carrier,
    std::map<std::string, std::string, std::less<>> &writer_text_map) {
  TextMapReader reader(carrier);
  TextMapWriter writer(writer_text_map);
  auto parent_span = opentracing::Tracer::Global()->Extract(reader);
  auto span = opentracing::Tracer::Global()->StartSpan(
      operation_name, {opentracing::ChildOf(parent_span->get())});
  opentracing::Tracer::Global()->Inject(span->context(), writer);
  return span;
}

} //namespace social_network

#endif //SOCIAL_NETWORK_MICROSERVICES_TRACING_H
