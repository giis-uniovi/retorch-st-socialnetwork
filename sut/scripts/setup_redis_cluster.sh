#! /bin/bash

while getopts n: flag
do
    case "${flag}" in
        n) name=${OPTARG};;
        *) echo "Usage: $0 -n <cluster-service-name>" >&2; exit 1;;
    esac
done

apt update
apt install -y dnsutils

ip_addrs=$(dig +short $name | tr '\n' ' ')
ip_addrs=${ip_addrs// /:6379 }

echo "ip_addrs: $ip_addrs"

redis-cli --cluster create $ip_addrs --cluster-replicas 0 --cluster-yes

