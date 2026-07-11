#!/usr/bin/env bash
# Generates the self-signed TLS material used only by the optional TLS/swarm
# deployments (docker-compose-tls.yml / docker-compose-swarm.yml).
#
# The private keys (server.key, server.pem) are intentionally NOT committed to
# version control. Run this script before bringing up a TLS-enabled deployment:
#
#     ./keys/generate-tls-keys.sh
#
# It (re)creates CA.pem, server.crt, server.key and server.pem in this folder.
set -euo pipefail

cd "$(dirname "$0")"

SUBJECT="/C=US/ST=NY/L=Ithaca/O=DeathStarBench/CN=socialnetwork"
DAYS=3650

# 1. Certificate authority
openssl genrsa -out ca.key 2048
openssl req -x509 -new -nodes -key ca.key -sha256 -days "$DAYS" \
    -subj "/C=US/ST=NY/L=Ithaca/O=DeathStarBench/CN=DeathStarBench-CA" \
    -out CA.pem

# 2. Server key + certificate signed by the CA
openssl genrsa -out server.key 2048
openssl req -new -key server.key -subj "$SUBJECT" -out server.csr
openssl x509 -req -in server.csr -CA CA.pem -CAkey ca.key -CAcreateserial \
    -days "$DAYS" -sha256 -out server.crt

# 3. Combined key+cert PEM (consumed by MongoDB's certificateKeyFile)
cat server.key server.crt > server.pem

# Clean up intermediate artifacts
rm -f ca.key server.csr CA.srl

echo "Generated CA.pem, server.crt, server.key and server.pem"
