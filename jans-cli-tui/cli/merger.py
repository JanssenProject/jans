import glob
import ruamel.yaml
from ruamel.yaml.representer import RoundTripRepresenter
import jsonmerge
import json
from collections import OrderedDict

main_yaml_fn = 'jans-config-api-swagger-auto.yaml'

def read_yaml(yaml_fn):

    with open(yaml_fn) as f:
        return ruamel.yaml.load(f.read().replace('\t', ''), ruamel.yaml.RoundTripLoader)

main_doc = read_yaml(main_yaml_fn)

for yaml_fn in glob.glob('*.yaml'):
    if 'plugin' in yaml_fn:
        plugin_doc = read_yaml(yaml_fn)
        main_doc['tags'] += plugin_doc['tags']
        
        for key in ('paths', 'components'):
            main_doc[key] = jsonmerge.merge(main_doc[key], plugin_doc[key])
            

with open('jca.json', 'w') as w:
    json.dump(main_doc, w, indent=2)


class MyRepresenter(RoundTripRepresenter):
    pass

ruamel.yaml.add_representer(OrderedDict, MyRepresenter.represent_dict, representer=MyRepresenter)
yaml = ruamel.yaml.YAML()
yaml.Representer = MyRepresenter

with open('jca.yaml', 'w') as w:
    yaml.dump(main_doc, w)
