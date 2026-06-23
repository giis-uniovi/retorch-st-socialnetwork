#ifndef SOCIAL_NETWORK_MICROSERVICES_TRANSPARENT_HASH_H
#define SOCIAL_NETWORK_MICROSERVICES_TRANSPARENT_HASH_H

#include <string>
#include <string_view>

namespace social_network {

// Heterogeneous hash so std::unordered_map<std::string, ...> can be used with a
// transparent comparator (std::equal_to<>) and looked up by std::string_view
// without constructing a temporary std::string.
struct TransparentStringHash {
  using is_transparent = void;
  size_t operator()(std::string_view sv) const noexcept {
    return std::hash<std::string_view>{}(sv);
  }
};

}  // namespace social_network

#endif  // SOCIAL_NETWORK_MICROSERVICES_TRANSPARENT_HASH_H
