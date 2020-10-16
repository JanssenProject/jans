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

conversions = []

def do_replace(eVal):
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


for obj in schema['objectClasses']:
    for e in ('oid',  'x_origin'):
        if e in obj:
            obj[e] = do_replace(obj[e])

    for lt in ('may', 'names'):
        for i in range(len((obj[lt]))):
            obj[lt][i] = do_replace(obj[lt][i])

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
    dump_dict = OrderedDict(conversions)
    print(json.dumps(dump_dict, indent=2))
