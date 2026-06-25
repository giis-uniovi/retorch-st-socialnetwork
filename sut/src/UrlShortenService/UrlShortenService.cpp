#include <mutex>

#include "../utils_memcached_service.h"
#include "UrlShortenHandler.h"

using namespace social_network;

int main(int argc, char *argv[]) {
  return run_memcached_mongo_service<UrlShortenServiceProcessor>(
      "url-shorten-service", "url-shorten", "url-shorten-mongodb",
      "url-shorten-memcached", "url-shorten", "shortened_url",
      [](memcached_pool_st *memcached_client_pool,
         mongoc_client_pool_t *mongodb_client_pool) {
        static std::mutex thread_lock;
        return std::make_shared<UrlShortenHandler>(
            memcached_client_pool, mongodb_client_pool, &thread_lock);
      });
}
