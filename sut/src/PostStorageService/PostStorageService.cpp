#include "../utils_service.h"
#include "PostStorageHandler.h"

using namespace social_network;

int main(int argc, char *argv[]) {
  return run_memcached_mongo_service<PostStorageServiceProcessor>(
      "post-storage-service", "post-storage", "post-storage-mongodb",
      "post-storage-memcached", "post", "post_id",
      [](memcached_pool_st *memcached_client_pool,
         mongoc_client_pool_t *mongodb_client_pool) {
        return std::make_shared<PostStorageHandler>(memcached_client_pool,
                                                    mongodb_client_pool);
      });
}
