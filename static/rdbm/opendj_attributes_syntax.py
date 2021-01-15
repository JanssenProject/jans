import glob
from ldif4.ldif import LDIFParser
from ldap.schema.models import AttributeType
import json

class myLdifParser(LDIFParser):
    def __init__(self, ldif_file):
        self.ldif_file = ldif_file
        self.entries = []

    def parse(self):
        with open(self.ldif_file, 'rb') as f:
            parser = LDIFParser(f)
            for dn, entry in parser.parse():
                for e in entry:
                    for i, v in enumerate(entry[e][:]):
                        if isinstance(v, bytes):
                            entry[e][i] = v.decode('utf-8')
                self.entries.append((dn, entry))


schema_names = {}

for schema_fn in glob.glob('*.ldif'):

    parser = myLdifParser(schema_fn)
    parser.parse()
    if 'attributeTypes' in parser.entries[0][1]:
        for a in parser.entries[0][1]['attributeTypes']:
            att = AttributeType(a)
            for n in att.names:
                schema_names[n] = att.syntax

with open('opendj_attributes_schema.json', 'w') as w:
    json.dump(schema_names, w, indent=2)
