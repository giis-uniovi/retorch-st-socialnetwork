#ifndef SOCIAL_NETWORK_MICROSERVICES_BSON_COMPAT_H
#define SOCIAL_NETWORK_MICROSERVICES_BSON_COMPAT_H

#include <bson/bson.h>

namespace social_network {

// bson 2.x removed the legacy bson_as_json(). The handlers serialise documents
// to JSON and then parse 64-bit fields as plain numbers, so we need the LEGACY
// mode (which emits int64 as bare numbers). Canonical/relaxed extended JSON wrap
// them as {"$numberLong": "..."} objects and break that parsing.
inline char *bson_as_json_legacy(const bson_t *bson, size_t *length) {
  bson_json_opts_t *opts =
      bson_json_opts_new(BSON_JSON_MODE_LEGACY, BSON_MAX_LEN_UNLIMITED);
  char *json = bson_as_json_with_opts(bson, length, opts);
  bson_json_opts_destroy(opts);
  return json;
}

}  // namespace social_network

#endif  // SOCIAL_NETWORK_MICROSERVICES_BSON_COMPAT_H
