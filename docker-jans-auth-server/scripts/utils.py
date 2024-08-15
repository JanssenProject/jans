from ruamel.yaml import YAML


def parse_lock_swagger_file(path="/app/templates/jans-lock/lock-plugin-swagger.yaml"):
    with open(path) as f:
        txt = f.read()
    txt = txt.replace("\t", " ")
    return YAML(typ="rt").load(txt)
