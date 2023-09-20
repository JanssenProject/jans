import ruamel.yaml


def parse_swagger_file(path="/app/templates/jans-scim/jans-scim-openapi.yaml"):
    with open(path) as f:
        txt = f.read()
    txt = txt.replace("\t", " ")
    return ruamel.yaml.load(txt, Loader=ruamel.yaml.RoundTripLoader)
