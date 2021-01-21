import json

ldap_sql_data_type_mapping = {
    '1.3.6.1.4.1.1466.115.121.1.7': 'SMALLINT',
    '1.3.6.1.4.1.1466.115.121.1.12': 'VARCHAR(128)',
    '1.3.6.1.4.1.1466.115.121.1.15': 'VARCHAR(48)',
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
    'jansAttrs': 'TEXT',
    'jansDbAuth': 'TEXT',
    'jansScr': 'TEXT',
    'jansConfApp': 'TEXT',
    'jansCacheConf': 'TEXT',
    'jansDocStoreConf': 'TEXT',
    'jansConfDyn': 'TEXT',
    'jansConfErrors': 'TEXT',
    'jansConfStatic': 'TEXT',
    'jansConfWebKeys': 'TEXT',
    'userPassword': 'VARCHAR(48)',
    'jansClaim': 'VARCHAR(48)',
    'jansValidation': 'VARCHAR(48)',
    'jansId': 'VARCHAR(128)',
    'jansSAML2URI': 'VARCHAR(48)',
    'inum': 'VARCHAR(48)',
    'displayName': 'VARCHAR(128)',
    'jansConfProperty': 'VARCHAR(256)',
    'jansConfErrors': 'TEXT',
    'jansIdTknSignedRespAlg': 'VARCHAR(48)',
    'jansThemeColor': 'VARCHAR(48)',
    'jansScopeTyp': 'VARCHAR(48)',
    'givenName': 'VARCHAR(48)',
    'jansGrantTyp': 'VARCHAR(48)',
    'jansRespTyp': 'VARCHAR(48)',
    'jansAttrEditTyp': 'VARCHAR(48)',
    'jansScr': 'TEXT',
    'jansAttrOrigin': 'VARCHAR(48)',
    'dat': 'VARCHAR(48)',
    'description': 'TEXT',
    'jansScope': 'VARCHAR(48)',
    'jansAttrViewTyp': 'VARCHAR(48)',
    'attr': 'VARCHAR(128)',
    'jansTknEndpointAuthMethod': 'VARCHAR(48)',
    'jansClntSecret': 'VARCHAR(48)',
    'jansAttrs': 'TEXT',
    'jansCacheConf': 'TEXT',
    'sn': 'VARCHAR(48)',
    'jansClaimRedirectURI': 'VARCHAR(64)',
    'jansSubjectTyp': 'VARCHAR(48)',
    'jansLogoutURI': 'VARCHAR(48)',
    'jansAttrName': 'VARCHAR(48)',
    'jansAuthMode': 'VARCHAR(48)',
    'jansStatus': 'VARCHAR(48)',
    'ou': 'VARCHAR(48)',
    'jansAppTyp': 'VARCHAR(48)',
    'o': 'VARCHAR(48)',
    'jansPostLogoutRedirectURI': 'VARCHAR(64)',
    'member': 'VARCHAR(48)',
    'jansDbAuth': 'TEXT',
    'jansConfDyn': 'TEXT',
    'jansConfWebKeys': 'TEXT',
    'urn': 'VARCHAR(128)',
    'jansClaimName': 'VARCHAR(48)',
    'middleName': 'VARCHAR(48)',
    'jansProgLng': 'VARCHAR(48)',
    'jansConfStatic': 'TEXT',
    'jansSAML1URI': 'VARCHAR(64)',
    'jansDocStoreConf': 'VARCHAR(256)',
    'jansModuleProperty': 'VARCHAR(64)',
    'uid': 'VARCHAR(48)',
    'jansAttrTyp': 'VARCHAR(48)',
    'jansOrgShortName': 'VARCHAR(48)',
    'nickname': 'VARCHAR(48)',
    'jansAccessTknSigAlg': 'VARCHAR(48)',
    'jansRedirectURI': 'VARCHAR(128)',
    'jansScrTyp': 'VARCHAR(48)',
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
