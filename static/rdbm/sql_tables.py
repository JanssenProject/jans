import json

ldap_sql_data_type_mapping = {
    '1.3.6.1.4.1.1466.115.121.1.7': 'SMALLINT',
    '1.3.6.1.4.1.1466.115.121.1.12': 'VARCHAR(128)',
    '1.3.6.1.4.1.1466.115.121.1.15': 'TINYTEXT',
    '1.3.6.1.4.1.1466.115.121.1.24': 'DATETIME',
    '1.3.6.1.4.1.1466.115.121.1.27': 'INT',
    '1.3.6.1.4.1.1466.115.121.1.11': 'VARCHAR(2)', #country code
    '1.3.6.1.4.1.1466.115.121.1.26': 'VARCHAR(48)', #IA5 String
    '1.3.6.1.4.1.1466.115.121.1.40': 'BINARY', #octed string
    '1.3.6.1.4.1.1466.115.121.1.8': 'BLOB',
    '1.3.6.1.4.1.1466.115.121.1.50': 'VARCHAR(20)', #phone number
    '1.3.6.1.4.1.1466.115.121.1.22': 'VARCHAR(20)', #Facsimile Telephone Number
    '1.3.6.1.4.1.1466.115.121.1.14': 'VARCHAR(50)', #Delivery Method
    '1.3.6.1.4.1.1466.115.121.1.41': 'VARCHAR(128)', #Postal Address,
    'JSON': 'JSON'
    }

sql_data_types = {
    'description': 'TEXT', # some attribute descriptions are too long, change to TINYTEXT after shortening them
    'jansAttrs': 'JSON',
    'jansDbAuth': 'TEXT',
    'jansScr': 'TEXT',
    'jansConfApp': 'TEXT',
    'jansCacheConf': 'TEXT',
    'jansDocStoreConf': 'TEXT',
    'jansConfDyn': 'JSON',
    'jansConfErrors': 'TEXT',
    'jansConfStatic': 'TEXT',
    'jansConfWebKeys': 'TEXT',
    'userPassword': 'VARCHAR(48)',
    
}



with open('opendj_attributes_syntax.json') as f:
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
                    data_type = sql_data_types[attrname]
                else:
                    for ja in jans_schema['attributeTypes']:
                        if attrname in ja['names']:
                            if ja.get('sql_data_type'):
                                data_type = ja['sql_data_type']
                                break
                    else:
                        attr_syntax = get_attr_syntax(attrname, jans_schema['attributeTypes'])
                        data_type = ldap_sql_data_type_mapping[attr_syntax]

                sql_tbl_cols.append('`{}` {}'.format(attrname, data_type))
                
            sql_cmd = 'CREATE TABLE `{}` (`id` int NOT NULL auto_increment, `doc_id` VARCHAR(48) NOT NULL UNIQUE, `objectClass` VARCHAR(48), dn VARCHAR(128), {}, PRIMARY KEY  (`id`, `doc_id`))'.format(sql_tbl_name, ', '.join(sql_tbl_cols))
            w.write(sql_cmd+';\n')
    w.close()
