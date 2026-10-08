import os
import sys
import argparse
import json
import base64
import tempfile

from collections import OrderedDict
from ldap3.utils import dn as dnutils

from setup_app.pylib.ldif4.ldif import LDIFWriter, LDIFParser

b64_encoded_field_descriptor = '@base64encodedfield-'
parser = argparse.ArgumentParser()
parser.add_argument("--infile", help="input ldif file", required=True)
parser.add_argument("--outfile", help="output ldif file")
argsp = parser.parse_args()



def resolve_user_path(path):
    resolved = os.path.realpath(path)
    for root in (os.getcwd(), os.path.expanduser('~'), tempfile.gettempdir()):
        root = os.path.realpath(root)
        if resolved.startswith(root + os.sep):
            return resolved
    print("{} must be under the working, home or temp directory".format(path))
    sys.exit(1)


ldif_file = resolve_user_path(argsp.infile)

if not os.path.exists(ldif_file):
    print("File {} does not exist".format(ldif_file))
    sys.exit()

out_file = resolve_user_path(argsp.outfile or ldif_file + '.jans')

with open('schema/jans_schema_mappings.json') as f:
    mapping = json.load(f, object_pairs_hook=OrderedDict)

with open('schema/mapping.json') as f:
    mapped_json = json.load(f, object_pairs_hook=OrderedDict)


mapping['objectClass']['gluuCustomPerson'] = 'jansCustomPerson'

#escape base64 encoded fields
b64_escaped_file = ldif_file +'.b64'
with open(b64_escaped_file, 'w') as w, open(ldif_file) as ldif_fd:
    for l in ldif_fd:
        n = l.find('::')
        if n > -1:
            a = l[:n]
            v = l[n+2:].strip()
            if v.startswith('%(') and v.endswith(')s'):
                v = b64_encoded_field_descriptor + v
                v = base64.b64encode(v.encode()).decode()
                l=a+'::'+v+'\n'
        w.write(l)



class myLdifParser(LDIFParser):
    def __init__(self, ldif_file):
        super().__init__(ldif_file)
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

ldif_parser = myLdifParser(b64_escaped_file)
ldif_parser.parse()

b64_escaped_out_file = out_file + '.b64'
with open(b64_escaped_out_file, 'wb') as out_ldif:

    ldif_writer = LDIFWriter(out_ldif, cols=10000)

    dn_coversions =[
        ('o', 'gluu', 'jans'),
        ('ou', 'oxauth', 'jans-auth'),
        ('ou', 'fido2', 'jans-fido2'),
        ('ou', 'scim', 'jans-scim'),
    
    ]

    for dn, entry in ldif_parser.entries:
        if 'OO11-BAFE' in entry.get('inum', []):
            continue

        new_entry = OrderedDict()
        for a in entry:
            if a != 'objectClass':
                if a in mapped_json['exclude']['attributeType']:
                    continue
                if a in opendj_attributes:
                    s = a
                elif a in ['changetype', 'add', 'replace']:
                    s = a
                else:
                    s = mapping['attribute'][a]

                new_entry[s] = entry[a]

        new_entry['objectClass'] = []
    
        for oc in entry.get('objectClass', []):
            if oc in mapped_json['exclude']['objectClass']:
                continue
            nn = mapping['objectClass'].get(oc, oc)
            new_entry['objectClass'].append(nn)

        new_dn_list = []
        for dne in dnutils.parse_dn(dn):
            k =  dne[0]
            for ot in mapping:
                for e in mapping[ot]:
                    if dne[0] == e:
                        k = mapping[ot][e]
                        break
            new_val = [k, dne[1]]
        
            for dnc in dn_coversions:
                if dnc[0] == dne[0] and dnc[1] == dne[1]:
                    new_val[1] = dnc[2]

            new_dn_list.append('='.join(new_val))

        new_dn = ','.join(new_dn_list)


        if 'Gluu' in new_entry.get('description', [''])[0]:
            new_entry['description'][0] = new_entry['description'][0].replace('Gluu', 'Janssen')
    
        if 'gluu' in new_entry.get('displayName', [''])[0]:
            new_entry['displayName'][0] = new_entry['displayName'][0].replace('gluu', 'janssen')
    

        ldif_writer.unparse(new_dn, new_entry)


 
#de-escape base64 encoded fields
with open(out_file, 'w') as w, open(b64_escaped_out_file) as b64_fd:
    for l in b64_fd:
        n = l.find(':')
        if n > -1:
            a = l[:n]
            v = l[n+1:].strip()
            if v.startswith(b64_encoded_field_descriptor):
                l = a+'::'+v[len(b64_encoded_field_descriptor):]+'\n'
                if 'person_authentication_supergluuexternalauthenticator' in l:
                    l = l.replace('person_authentication_supergluuexternalauthenticator', 'person_authentication_superjansexternalauthenticator')
        w.write(l)


os.remove(b64_escaped_file)
os.remove(b64_escaped_out_file)



print("Converted ldif was written to", out_file)
