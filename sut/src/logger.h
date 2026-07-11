#ifndef SOCIAL_NETWORK_MICROSERVICES_LOGGER_H
#define SOCIAL_NETWORK_MICROSERVICES_LOGGER_H

#include <boost/log/trivial.hpp>
#include <boost/log/utility/setup/console.hpp>
#include <boost/log/utility/setup/common_attributes.hpp>

#include <optional>
#include <source_location>
#include <string_view>

namespace social_network {

namespace detail {

// Returns a Boost.Log keyword argument for the given severity level.
// Extracted to its own function so the severity = level assignment (Boost.Log
// named-parameter idiom) is not embedded inside a larger expression (S1121).
inline auto make_severity_arg(boost::log::trivial::severity_level level) {
  auto kw_arg = (boost::log::keywords::severity = level);
  return kw_arg;
}

// Helper struct that captures source location at the call site and forwards
// streaming to a Boost.Log trivial logger record, replacing the __FILE__ /
// __LINE__ macros that triggered SonarCloud cpp:S6190.
template <boost::log::trivial::severity_level Level>
struct LogStream {
  boost::log::record rec;
  // Use optional so the ostream is only constructed when rec is valid.
  // Constructing record_ostream with an invalid record triggers a Boost
  // assertion, so we must not do it unconditionally.
  std::optional<boost::log::record_ostream> stream;

  explicit LogStream(std::source_location loc = std::source_location::current()) {
    auto sev_arg = make_severity_arg(Level);
    rec = boost::log::trivial::logger::get().open_record(sev_arg);
    if (rec) {
      stream.emplace(rec);
      std::string_view file(loc.file_name());
      auto slash = file.rfind('/');
      if (slash == std::string_view::npos) slash = file.rfind('\\');
      std::string_view basename = (slash == std::string_view::npos)
                                      ? file : file.substr(slash + 1);
      *stream << "(" << basename << ":" << loc.line() << ":"
              << loc.function_name() << ") ";
    }
  }

  LogStream(const LogStream &) = delete;
  LogStream &operator=(const LogStream &) = delete;

  ~LogStream() {
    if (rec) {
      stream->flush();
      boost::log::trivial::logger::get().push_record(std::move(rec));
    }
  }

  template <typename T>
  LogStream &operator<<(T &&val) {
    if (stream) *stream << std::forward<T>(val);
    return *this;
  }

  // Explicit overload needed to resolve templated manipulators like std::endl.
  // The single-template operator<<(T&&) cannot deduce template parameters for
  // std::endl because LogStream is not derived from std::basic_ostream. NOSONAR cpp:S5205
  LogStream &operator<<(std::ostream &(*manip)(std::ostream &)) { // NOSONAR cpp:S5205
    if (stream) *stream << manip;
    return *this;
  }
};

} // namespace detail

// LOG(severity) expands to a temporary LogStream whose constructor receives
// std::source_location::current() evaluated at the call site.  The temporary
// lives for the full statement, so chained << operators all go to the same
// record.  Usage is identical to the old macro:
//   LOG(info) << "message";
//   LOG(error) << "value=" << val;
#define LOG(severity)                                                           \
  ::social_network::detail::LogStream<                                         \
      ::boost::log::trivial::severity_level::severity>(                        \
      std::source_location::current())

void init_logger() {
  boost::log::register_simple_formatter_factory
      <boost::log::trivial::severity_level, char>("Severity");
  boost::log::add_common_attributes();
  auto fmt = (boost::log::keywords::format = "[%TimeStamp%] <%Severity%>: %Message%");
  boost::log::add_console_log(std::cerr, fmt);
  boost::log::core::get()->set_filter (
      boost::log::trivial::severity >= boost::log::trivial::info
  );
}


} //namespace social_network

#endif //SOCIAL_NETWORK_MICROSERVICES_LOGGER_H
