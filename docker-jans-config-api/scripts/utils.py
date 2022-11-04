import os

import ruamel.yaml


def parse_config_api_swagger(path="/app/static/jans-config-api-swagger-auto.yaml"):
    with open(path) as f:
        txt = f.read()
    txt = txt.replace("\t", " ")
    return ruamel.yaml.load(txt, Loader=ruamel.yaml.RoundTripLoader)


def generate_hex(size: int = 3):
    return os.urandom(size).hex().upper()
