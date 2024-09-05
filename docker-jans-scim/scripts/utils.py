from ruamel.yaml import YAML


def parse_swagger_file(path="/app/templates/jans-scim/jans-scim-openapi.yaml"):
    with open(path) as f:
        txt = f.read()
    txt = txt.replace("\t", " ")
    return YAML(typ="rt").load(txt)
