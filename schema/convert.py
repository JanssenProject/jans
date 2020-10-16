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
    mapping = json.load(f)

with open('opendj_types.json') as f:
    opendj_types = json.load(f)

opendj_attributes = []
for k in opendj_types:
    opendj_attributes += opendj_types[k]


conversions = []

def do_replace(eVal):
    if eVal in opendj_attributes:
        return eVal
    for m in mapping:
        if m in eVal:
            eVal = eVal.replace(m, mapping[m])

    return eVal

for attribute in schema['attributeTypes']:
    for e in ('desc', 'equality', 'oid', 'x_origin'):
        if e in attribute:
            attribute[e] = do_replace(attribute[e])

    for i in range(len((attribute['names']))):
        cur = attribute['names'][i]
        new = do_replace(cur)
        if cur != new:
            conversions.append((cur, new))
        attribute['names'][i] = new

obj_conversions = []
for obj in schema['objectClasses']:
    for e in ('oid',  'x_origin'):
        if e in obj:
            obj[e] = do_replace(obj[e])

    for lt in ('may', 'names'):
        for i in range(len((obj[lt]))):
            cur = obj[lt][i]
            new = do_replace(obj[lt][i])
            if cur != new:
                obj_conversions.append((cur, new))
                obj[lt][i] = new

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

