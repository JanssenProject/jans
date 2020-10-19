import os
import sys
import json
import argparse

from collections import OrderedDict

parser = argparse.ArgumentParser()
parser.add_argument("--infile", help="input schema json", required=True)
parser.add_argument("--outfile", help="output schema json")
parser.add_argument("-dump", help="dump conversions", action="store_true")
argsp = parser.parse_args()


schema_file = argsp.infile

if not os.path.exists(schema_file):
    print("File {} does not exist".format(schema_file))
    sys.exit()

with open(schema_file) as f:
    schema = json.load(f, object_pairs_hook=OrderedDict)


with open('mapping.json') as f:
    mapping = json.load(f, object_pairs_hook=OrderedDict)

with open('opendj_types.json') as f:
    opendj_types = json.load(f, object_pairs_hook=OrderedDict)

opendj_attributes = []
for k in opendj_types:
    opendj_attributes += opendj_types[k]


conversions = []

def do_replace(eVal):
    if eVal in opendj_attributes:
        return eVal
    for m in mapping['mappings']:
        if m in eVal:
            eVal = eVal.replace(m, mapping['mappings'][m])

    return eVal

for attribute in schema['attributeTypes'][:]:

    skip = False
    for name in attribute['names']:
        if name in mapping['exclude']['attributeType']:
            schema['attributeTypes'].remove(attribute)
            skip = True
    if skip:
        continue

    for e in ('desc', 'equality', 'oid', 'x_origin'):
        if e in attribute:
            attribute[e] = do_replace(attribute[e])

    new_name_list = []
    for name in (attribute['names']):
        new = do_replace(name)
        if not new in new_name_list:
            conversions.append((name, new))
            new_name_list.append(new)

    attribute['names'] = new_name_list

obj_conversions = []
for obj in schema['objectClasses'][:]:
    skip = False
    for name in obj['names']:
        if name in mapping['exclude']['objectClass']:
            schema['objectClasses'].remove(obj)
            skip = True
    if skip:
        continue

    for e in ('oid',  'x_origin'):
        if e in obj:
            obj[e] = do_replace(obj[e])

    for lt in ('may', 'names'):
        new_list = []
        for name in obj[lt]:
            new = do_replace(name)
            if name != new:
                obj_conversions.append((name, new))
            if not new in new_list:
                new_list.append(new)
        obj[lt] = new_list

macrkeys = list(schema['oidMacros'].keys())
for macr in macrkeys:
    newname = do_replace(macr)
    schema['oidMacros'][newname] = schema['oidMacros'].pop(macr)
    schema['oidMacros'][newname] = do_replace(schema['oidMacros'][newname])

if argsp.outfile:
    with open(argsp.outfile, 'w') as w:
        json.dump(schema, w, indent=2)

if argsp.dump:
    conversions.sort()
    obj_conversions.sort()
    attr_dict = OrderedDict(conversions)
    obj_dict = OrderedDict(obj_conversions)
    
    print(json.dumps({'attribute': attr_dict, 'objectClass':obj_dict}, indent=2))

