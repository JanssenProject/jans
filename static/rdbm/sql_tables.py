import os
import json

cur_dir = os.path.dirname(os.path.realpath(__file__))

sql_type = 'mysql'

with open(os.path.join(cur_dir, 'ldap_sql_data_type_mapping.json')) as f:
    ldap_sql_data_type_mapping = json.load(f)

with open(os.path.join(cur_dir, 'sql_data_types.json')) as f:
    sql_data_types = json.load(f)

with open(os.path.join(cur_dir, 'opendj_attributes_syntax.json')) as f:
    opendj_attributes_syntax = json.load(f)

def get_attr_syntax(attrname, attributes):
    for jans_attr in attributes:
        if attrname in jans_attr['names']:
            if jans_attr.get('multivalued'):
                return 'JSON'
            return jans_attr['syntax']
    else:
        opendj_syntax = opendj_attributes_syntax.get(attrname)
        if opendj_syntax is None:
            opendj_syntax = '1.3.6.1.4.1.1466.115.121.1.15'

        return opendj_syntax

if __name__ == "__main__": 

    w = open('jans_tables.sql', 'w')

    for jans_schema_fn in ('../../schema/jans_schema.json', '../../schema/custom_schema.json'):

        with open(jans_schema_fn) as f:
            jans_schema = json.load(f)

        for obj in jans_schema['objectClasses']:
            sql_tbl_name = obj['names'][0]
            sql_tbl_cols = []

            for attrname in obj['may']:
                if attrname in sql_data_types:
                    type_ = sql_data_types[attrname]
                    if type_[sql_type]['type'] == 'VARCHAR':
                        if type_[sql_type]['size'] <= 127:
                            data_type = 'VARCHAR({})'.format(type_[sql_type]['size'])
                        elif type_[sql_type]['size'] <= 255:
                            data_type = 'TINYTEXT'
                        else:
                            data_type = 'TEXT'
                    else:
                        data_type = type_[sql_type]['type']

                else:
                    attr_syntax = get_attr_syntax(attrname, jans_schema['attributeTypes'])
                    type_ = ldap_sql_data_type_mapping[attr_syntax]
                    if type_[sql_type]['type'] == 'VARCHAR':
                        data_type = 'VARCHAR({})'.format(type_[sql_type]['size'])
                    else:
                        data_type = type_[sql_type]['type']

                sql_tbl_cols.append('`{}` {}'.format(attrname, data_type))
                
            sql_cmd = 'CREATE TABLE `{}` (`id` int NOT NULL auto_increment, `doc_id` VARCHAR(48) NOT NULL UNIQUE, `objectClass` VARCHAR(48), dn VARCHAR(128), {}, PRIMARY KEY  (`id`, `doc_id`))'.format(sql_tbl_name, ', '.join(sql_tbl_cols))
            w.write(sql_cmd+';\n')
    w.close()
