import os
import sys
import argparse
import json
import base64

from collections import OrderedDict
from ldap3.utils import dn as dnutils

from setup_app.pylib.ldif4.ldif import LDIFWriter, LDIFParser

b64_encoded_field_descriptor = '@base64encodedfield-'
parser = argparse.ArgumentParser()
parser.add_argument("--infile", help="input ldif file", required=True)
parser.add_argument("--outfile", help="output ldif file")
argsp = parser.parse_args()

ldif_file = argsp.infile

if not os.path.exists(ldif_file):
    print("File {} does not exist".format(ldif_file))
    sys.exit()

if not argsp.outfile:
    out_file = ldif_file+'.jans'
else:
    out_file = argsp.outfile

with open('schema/mapping.json') as f:
    mapping = json.load(f, object_pairs_hook=OrderedDict)


#escape base64 encoded fields
b64_escaped_file = ldif_file +'.b64'
w = open(b64_escaped_file, 'w')
for l in open(ldif_file):
    n = l.find('::')
    if n > -1:
        a = l[:n]
        v = l[n+2:].strip()
        if v.startswith('%(') and v.endswith(')s'):
            v = b64_encoded_field_descriptor + v
            v = base64.b64encode(v.encode()).decode()
            l=a+'::'+v+'\n'
    w.write(l)
w.close()



class myLdifParser(LDIFParser):
    def __init__(self, ldif_file):
        self.ldif_file = ldif_file
        self.entries = []

    def parse(self):
        with open(self.ldif_file, 'rb') as f:
            parser = LDIFParser(f)
            #parser.decodebinary = False
            for dn, entry in parser.parse():
                for e in entry:
                    for i, v in enumerate(entry[e][:]):
                        if isinstance(v, bytes):
                            entry[e][i] = v.decode('utf-8')
                self.entries.append((dn, entry))

with open('schema/opendj_types.json') as f:
    opendj_types = json.load(f)

opendj_attributes = []
for k in opendj_types:
    opendj_attributes += opendj_types[k]

def do_replace(eVal):
    if eVal in opendj_attributes:
        return eVal
    for m in mapping['mappings']:
        if m in eVal:
            eVal = eVal.replace(m, mapping['mappings'][m])

    return eVal


ldif_parser = myLdifParser(b64_escaped_file)
ldif_parser.parse()

b64_escaped_out_file = out_file + '.b64'
out_ldif = open(b64_escaped_out_file, 'wb')

ldif_writer = LDIFWriter(out_ldif, cols=10000)


for dn, entry in ldif_parser.entries:
    new_entry = OrderedDict()
    for a in entry:
        s = do_replace(a)
        new_entry[s] = entry[a]
    new_entry['objectClass'] = [ do_replace(oc) for oc in new_entry['objectClass'] ]

    new_dn_list = []
    for dne in dnutils.parse_dn(dn):
        k =  do_replace(dne[0])
        new_val = [k, dne[1]]
        if new_val[1] == 'gluu':
            new_val[1] = 'jans'
        new_dn_list.append('='.join(new_val))

    new_dn = ','.join(new_dn_list)


    ldif_writer.unparse(new_dn, new_entry)

out_ldif.close()

 
#de-escape base64 encoded fields
w = open(out_file, 'w')
for l in open(b64_escaped_out_file):
    n = l.find(':')
    if n > -1:
        a = l[:n]
        v = l[n+1:].strip()
        if v.startswith(b64_encoded_field_descriptor):
            l=a+'::'+v[len(b64_encoded_field_descriptor):]+'\n'
    w.write(l)

w.close()

os.remove(b64_escaped_file)
os.remove(b64_escaped_out_file)

print("Converted ldif was written to", out_file)
