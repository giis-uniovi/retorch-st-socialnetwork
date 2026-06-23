# -*- coding: UTF-8 -*-

import json
import os
import yaml

_CONFIG_PATH = '/social-network-microservices/config'


def config_thrift(tls):
    path = f'{_CONFIG_PATH}/service-config.json'
    with open(path) as f:
        j = json.loads(f.read())
    j['ssl']['enabled'] = tls
    with open(path, 'w') as f:
        f.write(json.dumps(j, indent=2))


def config_mongod(tls):
    path = f'{_CONFIG_PATH}/mongod.conf'
    with open(path) as f:
        y = yaml.safe_load(f.read())
    if tls:
        y['net']['tls']['mode'] = 'requireTLS'
        y['net']['tls']['certificateKeyFile'] = '/keys/server.pem'
    else:
        y['net']['tls']['mode'] = 'disabled'
        try:
            del y['net']['tls']['certificateKeyFile']
        except KeyError:
            pass
    with open(path, 'w') as f:
        f.write(yaml.dump(y, default_flow_style=False))


def config_redis(tls):
    path = f'{_CONFIG_PATH}/redis.conf'
    with open(path) as f:
        content = f.read()
    if tls:
        content = content.replace('port 6379', 'port 0')
        content = content.replace('tls-port 0', 'tls-port 6379')
    else:
        content = content.replace('port 0', 'port 6379')
        content = content.replace('tls-port 6379', 'tls-port 0')
    with open(path, 'w') as f:
        f.write(content)


tls = os.environ.get('TLS', '0').lower() not in ('0', 'false')

config_thrift(tls)
config_mongod(tls)
config_redis(tls)
