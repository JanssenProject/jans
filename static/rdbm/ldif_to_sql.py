import os
import sys
import json
from ldap3.utils import dn as dnutils

from pathlib import Path
cur_dir = os.path.dirname(os.path.realpath(__file__))
cur_path = Path(cur_dir)
sys.path.append(cur_path.parent.parent.joinpath('setup_app/pylib').as_posix())

from ldif4.ldif import LDIFParser
from sql_tables import ldap_sql_data_type_mapping, get_attr_syntax

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



schema_path = cur_path.parent.parent.joinpath('schema')

jans_attributes = []

for schema_fn in ('jans_schema.json', 'custom_schema.json'):
    schema_io = schema_path.joinpath(schema_fn).open()
    schema = json.load(schema_io)
    jans_attributes += schema['attributeTypes']

output_dir = '/tmp/output'

sql_data = open('jans_data.sql', 'w')

for ldif_path in Path(output_dir).glob('**/*.ldif'):
    ldif_fn = ldif_path.as_posix()
    parser = myLdifParser(ldif_fn)
    parser.parse()

    for dn, entry in parser.entries:
        dn_parsed = dnutils.parse_dn(dn)
        rdn_name = dn_parsed[0][0]
        doc_id = dn_parsed[0][1]
        objectClass = entry.get('objectClass') or entry.get('objectclass')
        objectClass = objectClass[-1]
        entry.pop(rdn_name)
        if 'objectClass' in entry:
            entry.pop('objectClass')
        elif 'objectclass' in entry:
            entry.pop('objectclass')

        table_name = objectClass.lower()

        cols = ['`doc_id`', '`objectClass`', '`dn`']
        vals = ['"{}"'.format(doc_id), '"{}"'.format(objectClass), '"{}"'.format(dn)]
        for lkey in entry:
            cols.append('`{}`'.format(lkey))
            data_type = ldap_sql_data_type_mapping[get_attr_syntax(lkey, jans_attributes)]
            
            if data_type in ('SMALLINT', 'INT'):
                if entry[lkey][0].lower() in ('1', 'on', 'true', 'yes'):
                    vals.append('1')
                else:
                    vals.append('0')
            elif data_type == 'DATETIME':
                vals.append(entry[lkey])
            elif data_type == 'JSON':
                vals.append("'{}'".format(json.dumps(entry[lkey])))
            else:
                vals.append(json.dumps(entry[lkey][0]))

        sql_cmd = 'INSERT INTO {} ({}) VALUES ({});\n'.format(
                    table_name,
                    ', '.join(cols),
                    ', '.join(vals)
                    )
        
        sql_data.write(sql_cmd)

sql_data.close()
    
