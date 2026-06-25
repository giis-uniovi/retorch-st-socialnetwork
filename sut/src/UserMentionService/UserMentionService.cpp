#include "../utils_service.h"
#include "UserMentionHandler.h"

using namespace social_network;

int main(int argc, char *argv[]) {
  return run_memcached_mongo_service<UserMentionServiceProcessor>(
      "user-mention-service", "user", "user-mongodb", "user-memcached",
      /*index_db=*/"", /*index_field=*/"",
      [](memcached_pool_st *memcached_client_pool,
         mongoc_client_pool_t *mongodb_client_pool) {
        return std::make_shared<UserMentionHandler>(memcached_client_pool,
                                                    mongodb_client_pool);
      });
}
