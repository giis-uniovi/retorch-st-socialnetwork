FROM ubuntu:26.04 AS builder

ARG JAEGER_TRACING_VERSION=0.7.0

# Ubuntu 26.04 ships GCC 14 by default → full C++23 support.
# Install all C++ library dependencies available via apt.
RUN apt-get update && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
        build-essential \
        ca-certificates \
        cmake \
        curl \
        git \
        libboost-all-dev \
        libbson-dev \
        libevent-dev \
        libhiredis-dev \
        libicu-dev \
        libmemcached-dev \
        libmongoc-dev \
        libopentracing-dev \
        librabbitmq-dev \
        libsasl2-dev \
        libssl-dev \
        libthrift-dev \
        libyaml-cpp-dev \
        nlohmann-json3-dev \
        pkg-config \
        thrift-compiler \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

# Build redis-plus-plus with our custom patch (enables TLS + exposes shards pool).
# -DCMAKE_POLICY_VERSION_MINIMUM=3.5 is required because Ubuntu 26.04 ships cmake
# 3.31+ which removed backwards compatibility with cmake_minimum_required < 3.5.
RUN git clone --depth 1 --branch 1.3.12 \
        https://github.com/sewenew/redis-plus-plus.git /tmp/redis-plus-plus && \
    cd /tmp/redis-plus-plus && \
    sed -i '/Transaction transaction/i\\    ShardsPool* get_shards_pool(){\n        return _pool.get();\n    }\n' \
        src/sw/redis++/redis_cluster.h && \
    mkdir cmake-build && cd cmake-build && \
    cmake -DCMAKE_POLICY_VERSION_MINIMUM=3.5 \
          -DREDIS_PLUS_PLUS_USE_TLS=ON \
          -DREDIS_PLUS_PLUS_CXX_STANDARD=23 \
          -DREDIS_PLUS_PLUS_BUILD_TEST=OFF \
          -DHIREDIS_LIBRARY=/usr/lib/x86_64-linux-gnu/libhiredis.so \
          -DHIREDIS_HEADER=/usr/include \
          -DHIREDIS_TLS_LIBRARY=/usr/lib/x86_64-linux-gnu/libhiredis_ssl.so \
          .. && \
    make -j$(nproc) && make install && \
    rm -rf /tmp/redis-plus-plus && \
    ln -sf /usr/lib/x86_64-linux-gnu/libhiredis.so /usr/local/lib/libhiredis.a && \
    ln -sf /usr/lib/x86_64-linux-gnu/libhiredis_ssl.so /usr/local/lib/libhiredis_ssl.a

# Build AMQP-CPP (used by WriteHomeTimelineService)
RUN git clone --depth 1 \
        https://github.com/CopernicaMarketingSoftware/AMQP-CPP.git /tmp/amqpcpp && \
    cd /tmp/amqpcpp && \
    mkdir cmake-build && cd cmake-build && \
    cmake -DCMAKE_POLICY_VERSION_MINIMUM=3.5 \
          -DCMAKE_BUILD_TYPE=Release \
          -DAMQP-CPP_BUILD_SHARED=ON \
          .. && \
    make -j$(nproc) && make install && \
    rm -rf /tmp/amqpcpp

# jwt-cpp (arun11299/cpp-jwt, header-only) — copied from third_party to avoid network
COPY ./third_party/jwt-cpp/jwt /usr/local/include/jwt

# thrift/stdcxx.h was removed in Thrift 0.13+ but the gen-cpp files reference it.
# Provide a compatibility stub so the generated code compiles against newer Thrift.
RUN echo '#pragma once\n#include <memory>\n#include <functional>\n#include <iostream>' \
        > $(dpkg -L libthrift-dev | grep 'include/thrift$' | head -1)/stdcxx.h || \
    echo '#pragma once\n#include <memory>\n#include <functional>\n#include <iostream>' \
        > /usr/include/thrift/stdcxx.h

COPY ./ /social-network-microservices
WORKDIR /social-network-microservices

# Regenerate Thrift C++ bindings to match the installed Thrift runtime version
# and then build all services with GCC 14 / C++23
RUN thrift --gen cpp -out gen-cpp social_network.thrift \
    && sed -i 's/std::map<std::string, std::string>/std::map<std::string, std::string, std::less<>>/g' \
           gen-cpp/*.h gen-cpp/*.cpp \
    && cmake -DCMAKE_POLICY_VERSION_MINIMUM=3.5 \
             -DCMAKE_BUILD_TYPE=Debug \
             -DCMAKE_C_COMPILER=gcc \
             -DCMAKE_CXX_COMPILER=g++ \
             -DCMAKE_PREFIX_PATH="/usr/local;/usr;/usr/lib/x86_64-linux-gnu" \
             -S . -B build \
    && cmake --build build --parallel "$(nproc)" \
    && cmake --install build

FROM ubuntu:26.04

# Copy compiled C++ service binaries and their runtime libraries
COPY --from=builder /usr/local/bin/* /usr/local/bin/
COPY --from=builder /usr/local/lib/* /usr/local/lib/

# Install runtime dependencies
RUN apt-get update && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
        ca-certificates \
        libboost-filesystem1.90.0 \
        libboost-log1.90.0 \
        libboost-program-options1.90.0 \
        libboost-thread1.90.0 \
        libbson2-2 \
        libhiredis1.1 \
        libmemcached11 \
        libmemcachedutil2 \
        libmongoc2-2 \
        libopentracing1 \
        librabbitmq4 \
        libsasl2-2 \
        libssl3 \
        libthrift-0.22.0 \
        openssl \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

WORKDIR /social-network-microservices

# Run the services as a non-root user
RUN useradd --create-home --shell /usr/sbin/nologin appuser \
    && chown -R appuser:appuser /social-network-microservices
USER appuser
