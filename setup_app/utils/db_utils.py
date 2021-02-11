import os
import re
import json
import ldap3
import pymysql
from ldap3.utils import dn as dnutils

from setup_app import static
from setup_app.config import Config
from setup_app.static import InstallTypes, BackendTypes, colors
from setup_app.utils import base
from setup_app.utils.cbm import CBM
from setup_app.utils import ldif_utils
from setup_app.utils.attributes import attribDataTypes


class DBUtils:

    processedKeys = []
    
    def bind(self, use_ssl=True):

        if Config.mappingLocations['default'] == 'ldap':
            self.moddb = BackendTypes.LDAP
        elif Config.mappingLocations['default'] == 'rdbm':
            if Config.rdbm_type == 'mysql':
                self.moddb = BackendTypes.MYSQL
        else:
            self.moddb = BackendTypes.COUCHBASE


        if not hasattr(self, 'ldap_conn'):
            for group in Config.mappingLocations:
                if Config.mappingLocations[group] == 'ldap':

                    ldap_server = ldap3.Server(Config.ldap_hostname, port=int(Config.ldaps_port), use_ssl=use_ssl)
                    self.ldap_conn = ldap3.Connection(
                                ldap_server,
                                user=Config.ldap_binddn,
                                password=Config.ldapPass,
                                )
                    base.logIt("Making LDAP Connection to host {}:{} with user {}".format(Config.ldap_hostname, Config.ldaps_port, Config.ldap_binddn))
                    self.ldap_conn.bind()
                    break
                    
        if not hasattr(self, 'mysql_conn'):
            for group in Config.mappingLocations:
                if Config.mappingLocations[group] == 'rdbm':
                    if Config.rdbm_type == 'mysql':
                        result = self.mysqlconnection()
                        if not result[0]:
                            print("{}FATAL: {}{}".format(colors.FAIL, result[1], colors.ENDC))
                        break

        self.set_cbm()
        self.default_bucket = Config.couchbase_bucket_prefix


    def mysqlconnection(self, log=True):
        self.read_jans_schema()
        base.logIt("Making MySQL Connection to {}:{}/{} with user {}".format(Config.rdbm_host, Config.rdbm_port, Config.rdbm_db, Config.rdbm_user))
        try:
            self.mysql_conn = pymysql.connect(
                            host=Config.rdbm_host,
                            user=Config.rdbm_user,
                            password=Config.rdbm_password,
                            database=Config.rdbm_db,
                            port=Config.rdbm_port,
                            charset='utf8mb4',
                            autocommit=True,
                            cursorclass=pymysql.cursors.DictCursor
                        )
            base.logIt("MySQL Connection was successful")
            self.cursor = self.mysql_conn.cursor()
            return True, self.mysql_conn
        except Exception as e:
            if log:
                base.logIt("Can't connect to MySQL server: {}".format(e.args[1]), True)
            return False, e.args[1]

    def read_jans_schema(self):
        self.jans_attributes = []
        for schema_fn_ in ('jans_schema.json', 'custom_schema.json'):
            schema_fn = os.path.join(Config.install_dir, 'schema', schema_fn_)
            schema = base.readJsonFile(schema_fn)
            self.jans_attributes += schema['attributeTypes']

        self.ldap_sql_data_type_mapping = base.readJsonFile(os.path.join(Config.static_rdbm_dir, 'ldap_sql_data_type_mapping.json'))
        self.sql_data_types = base.readJsonFile(os.path.join(Config.static_rdbm_dir, 'sql_data_types.json'))
        self.opendj_attributes_syntax = base.readJsonFile(os.path.join(Config.static_rdbm_dir, 'opendj_attributes_syntax.json'))


    def exec_rdbm_query(self, query, getresult=False):
        base.logIt("Executing {} Query: {}".format(Config.rdbm_type, query))
        if Config.rdbm_type == 'mysql':
            try:
                self.cursor.execute(query)
            except Exception as e:
                base.logIt("ERROR executing query {}".format(e.args))
                base.logIt("ERROR executing query {}".format(e.args), True)
            else:
                if getresult == 1:
                    return self.cursor.fetchone()
                elif getresult:
                    return self.cursor.fetchall()

    def set_cbm(self):
        self.cbm = CBM(Config.get('cb_query_node'), Config.get('couchebaseClusterAdmin'), Config.get('cb_password'))

    def get_oxAuthConfDynamic(self):
        if self.moddb == BackendTypes.LDAP:
            self.ldap_conn.search(
                        search_base='ou=jans-auth,ou=configuration,o=jans',
                        search_scope=ldap3.BASE,
                        search_filter='(objectClass=*)',
                        attributes=["jansConfDyn"]
                        )

            dn = self.ldap_conn.response[0]['dn']
            oxAuthConfDynamic = json.loads(self.ldap_conn.response[0]['attributes']['jansConfDyn'][0])
        
        elif self.moddb == BackendTypes.MYSQL:
            sql_cmd = 'SELECT dn, jansConfDyn FROM jansAppConf'
            dn = self.ldap_conn.response[0]['dn'] 
            result = self.exec_rdbm_query(sql_cmd, 2)
            
            for entry in result:
                if entry['jansConfDyn']:
                    oxAuthConfDynamic = json.loads(entry['jansConfDyn'])
                    dn = entry['dn']
                    break

        elif self.moddb == BackendTypes.COUCHBASE:
            n1ql = 'SELECT * FROM `{}` USE KEYS "configuration_jans-auth  "'.format(self.default_bucket)
            result = cbm.exec_query(n1ql)
            js = result.json()
            dn = js['results'][0][self.default_bucket]['dn']
            oxAuthConfDynamic = js['results'][0][self.default_bucket]['oxAuthConfDynamic']

        return dn, oxAuthConfDynamic


    def set_oxAuthConfDynamic(self, entries):
        if self.moddb == BackendTypes.LDAP:
            dn, oxAuthConfDynamic = self.get_oxAuthConfDynamic()
            oxAuthConfDynamic.update(entries)

            ldap_operation_result = self.ldap_conn.modify(
                    dn,
                    {"jansConfDyn": [ldap3.MODIFY_REPLACE, json.dumps(oxAuthConfDynamic, indent=2)]}
                    )
            self.log_ldap_result(ldap_operation_result)

        elif self.moddb == BackendTypes.MYSQL:
            dn, oxAuthConfDynamic = self.get_oxAuthConfDynamic()
            oxAuthConfDynamic.update(entries)
            sql_cmd = "UPDATE jansAppConf SET jansConfDyn='{}' WHERE dn='{}'".format(json.dumps(oxAuthConfDynamic, indent=2), dn)
            self.exec_rdbm_query(sql_cmd)

        elif self.moddb == BackendTypes.COUCHBASE:
            for k in entries:
                n1ql = 'UPDATE `{}` USE KEYS "configuration_jans-auth" SET {}={}'.format(self.default_bucket, k, entries[k])
                self.cbm.exec_query(n1ql)


    def enable_script(self, inum):
        if self.moddb == BackendTypes.LDAP:
            ldap_operation_result = self.ldap_conn.modify(
                    'inum={},ou=scripts,o=jans'.format(inum),
                    {"jansEnabled": [ldap3.MODIFY_REPLACE, 'true']}
                    )
            self.log_ldap_result(ldap_operation_result)
            
        elif self.moddb == BackendTypes.MYSQL:
            sql_cmd = "UPDATE jansCustomScr SET jansEnabled=1 WHERE dn='inum={},ou=scripts,o=jans'".format(inum)
            self.exec_rdbm_query(sql_cmd)

        elif self.moddb == BackendTypes.COUCHBASE:
            n1ql = 'UPDATE `{}` USE KEYS "scripts_{}" SET jansEnabled=true'.format(self.default_bucket, inum)
            self.cbm.exec_query(n1ql)

    def enable_service(self, service):
        if self.moddb == BackendTypes.LDAP:
            ldap_operation_result = self.ldap_conn.modify(
                'ou=configuration,o=jans',
                {service: [ldap3.MODIFY_REPLACE, 'true']}
                )
            self.log_ldap_result(ldap_operation_result)
        
        elif self.moddb == BackendTypes.MYSQL:
            sql_cmd = "UPDATE jansAppConf SET {}=1 WHERE dn='ou=configuration,o=jans'".format(service)
            self.exec_rdbm_query(sql_cmd)

        elif self.moddb == BackendTypes.COUCHBASE:
            n1ql = 'UPDATE `{}` USE KEYS "configuration" SET {}=true'.format(self.default_bucket, service)
            self.cbm.exec_query(n1ql)
        
    def set_configuration(self, component, value):
        
        if self.moddb == BackendTypes.LDAP:
            ldap_operation_result = self.ldap_conn.modify(
                'ou=configuration,o=jans',
                {component: [ldap3.MODIFY_REPLACE, value]}
                )
            self.log_ldap_result(ldap_operation_result)

        elif self.moddb == BackendTypes.MYSQL:
            sql_cmd = "UPDATE jansAppConf SET {}='{}' WHERE dn='ou=configuration,o=jans'".format(component, value)
            self.exec_rdbm_query(sql_cmd)
        
        elif self.moddb == BackendTypes.COUCHBASE:
            n1ql = 'UPDATE `{}` USE KEYS "configuration" SET {}={}'.format(self.default_bucket, component, value)
            self.cbm.exec_query(n1ql)


    def dn_exists(self, dn):
        base.logIt("Querying LDAP for dn {}".format(dn))
        return self.ldap_conn.search(search_base=dn, search_filter='(objectClass=*)', search_scope=ldap3.BASE, attributes=['*'])

    def dn_exists_rdbm(self, dn, table):
        return self.exec_rdbm_query("select dn from {} where dn='{}'".format(table, dn), 1)


    def search(self, search_base, search_filter='(objectClass=*)', search_scope=ldap3.LEVEL):
        base.logIt("Searching database for dn {} with filter {}".format(search_base, search_filter))
        backend_location = self.get_backend_location_for_dn(search_base)
        
        if backend_location == BackendTypes.LDAP:
            if self.ldap_conn.search(search_base=search_base, search_filter=search_filter, search_scope=search_scope, attributes=['*']):
                key, document = ldif_utils.get_document_from_entry(self.ldap_conn.response[0]['dn'], self.ldap_conn.response[0]['attributes'])
                return document

        if backend_location == BackendTypes.MYSQL:
            s_table = None
            filter_re = re.match('\((.*?)=(.*?)\)', search_filter)
            s_col, s_val = filter_re.groups()
            s_val = s_val.replace('*', '%')
            q_operator = 'LIKE' if '%' in s_val else '='
            result = None
            if s_val != '%':
                if s_col.lower() == 'objectclass':
                    s_table = s_val
            
            if not s_table:
            
                tbl_list = self.exec_rdbm_query("SELECT table_name FROM information_schema.tables WHERE table_schema = '{}'".format(Config.rdbm_db), 2)
                for tbl_ in tbl_list:
                    tbl = tbl_['TABLE_NAME']
                    if self.exec_rdbm_query("SHOW COLUMNS FROM `{}` LIKE '{}'".format(tbl, s_col), 1):
                        sql_cmd = 'SELECT * FROM {} WHERE (dn LIKE "%{}") AND ({} {} "{}")'.format(tbl, search_base, s_col, q_operator, s_val)
                        result = self.exec_rdbm_query(sql_cmd, 1)
                        if result:
                            break
            
            else:
                sql_cmd = 'SELECT * FROM {} WHERE (dn LIKE "%{}") AND ({} {} "{}")'.format(s_table, search_base, s_col, q_operator, s_val)
                result = self.exec_rdbm_query(sql_cmd, 1)

            return result

        if backend_location == BackendTypes.COUCHBASE:
            key = ldif_utils.get_key_from(search_base)
            bucket = self.get_bucket_for_key(key)

            if search_scope == ldap3.BASE:
                n1ql = 'SELECT * FROM `{}` USE KEYS "{}"'.format(bucket, key)
            else:
                parsed_dn = dnutils.parse_dn(search_filter.strip('(').strip(')'))
                attr = parsed_dn[0][0]
                val = parsed_dn[0][1]
                if '*' in val:
                    search_clause = 'LIKE "{}"'.format(val.replace('*', '%'))
                else:
                    search_clause = '="{}"'.format(val.replace('*', '%'))
                
                n1ql = 'SELECT * FROM `{}` WHERE `{}` {}'.format(bucket, attr, search_clause)
                
            result = self.cbm.exec_query(n1ql)
            if result.ok:
                data = result.json()
                if data.get('results'):
                    return data['results'][0][bucket]
            

    def add2strlist(self, client_id, strlist):
        value2 = []
        for v in strlist.split(','):
            if v.strip():
                value2.append(v.strip())
        value2.append(client_id)

        return  ','.join(value2)
    
    def add_client2script(self, script_inum, client_id):
        dn = 'inum={},ou=scripts,o=jans'.format(script_inum)

        backend_location = self.get_backend_location_for_dn(dn)

        if backend_location == BackendTypes.LDAP:
            if self.dn_exists(dn):
                for e in self.ldap_conn.response[0]['attributes'].get('jansConfProperty', []):
                    try:
                        jansConfProperty = json.loads(e)
                    except:
                        continue
                    if isinstance(jansConfProperty, dict) and jansConfProperty.get('value1') == 'allowed_clients':
                        if not client_id in jansConfProperty['value2']:
                            jansConfProperty['value2'] = self.add2strlist(client_id, jansConfProperty['value2'])
                            jansConfProperty_js = json.dumps(jansConfProperty)
                            ldap_operation_result = self.ldap_conn.modify(
                                dn,
                                {'jansConfProperty': [ldap3.MODIFY_DELETE, e]}
                                )
                            self.log_ldap_result(ldap_operation_result)
                            ldap_operation_result = self.ldap_conn.modify(
                                dn,
                                {'jansConfProperty': [ldap3.MODIFY_ADD, jansConfProperty_js]}
                                )
                            self.log_ldap_result(ldap_operation_result)

        elif backend_location == BackendTypes.MYSQL:
            if self.dn_exists_rdbm(dn, 'jansCustomScr'):
                sql_cmd = 'SELECT jansConfProperty from jansCustomScr WHERE dn="{}"'.format(dn)
                jansConfProperty_s = self.exec_rdbm_query(sql_cmd, 1)
                if jansConfProperty_s and jansConfProperty_s['jansConfProperty']:
                    jansConfProperty = json.loads(jansConfProperty_s['jansConfProperty'])
                else:
                    jansConfProperty = {'v': []}

                for oxconfigprop in jansConfProperty['v']:
                    if oxconfigprop.get('value1') == 'allowed_clients' and not client_id in oxconfigprop['value2']:
                        oxconfigprop['value2'] = self.add2strlist(client_id, oxconfigprop['value2'])
                        break
                else:
                    jansConfProperty['v'].append({'value1': 'allowed_clients', 'value2': client_id})

                sql_cmd = "UPDATE jansCustomScr SET jansConfProperty='{}' WHERE dn='{}'".format(
                                        json.dumps(jansConfProperty), dn)

                self.exec_rdbm_query(sql_cmd)

        elif backend_location == BackendTypes.COUCHBASE:
            bucket = self.get_bucket_for_dn(dn)
            n1ql = 'SELECT jansConfProperty FROM `{}` USE KEYS "scripts_{}"'.format(bucket, script_inum)
            result = self.cbm.exec_query(n1ql)
            js = result.json()

            oxConfigurationProperties = js['results'][0]['jansConfProperty']
            for i, oxconfigprop_str in enumerate(oxConfigurationProperties):
                oxconfigprop = json.loads(oxconfigprop_str)
                if oxconfigprop.get('value1') == 'allowed_clients' and not client_id in oxconfigprop['value2']:
                    oxconfigprop['value2'] = self.add2strlist(client_id, oxconfigprop['value2'])
                    oxConfigurationProperties[i] = json.dumps(oxconfigprop)
                    break
            else:
                return

            n1ql = 'UPDATE `{}` USE KEYS "scripts_{}" SET `jansConfProperty`={}'.format(bucket, script_inum, json.dumps(oxConfigurationProperties))
            self.cbm.exec_query(n1ql)            

    def get_key_prefix(self, key):
        n = key.find('_')
        return key[:n+1]

    def check_attribute_exists(self, key, attribute):
        bucket = self.get_bucket_for_key(key)
        n1ql = 'SELECT `{}` FROM `{}` USE KEYS "{}"'.format(attribute, bucket, key)
        result = self.cbm.exec_query(n1ql)
        if result.ok:
            data = result.json()
            r = data.get('results', [])
            if r and r[0].get(attribute):
                return r[0][attribute]


    def log_ldap_result(self, ldap_operation_result):
        if not ldap_operation_result:
            base.logIt("Ldap modify operation failed {}".format(str(self.ldap_conn.result)))
            base.logIt("Ldap modify operation failed {}".format(str(self.ldap_conn.result)), True)


    def get_attr_syntax(self, attrname):
        for jans_attr in self.jans_attributes:
            if attrname in jans_attr['names']:
                if jans_attr.get('multivalued'):
                    return 'JSON'
                return jans_attr['syntax']
        else:
            opendj_syntax = self.opendj_attributes_syntax.get(attrname)
            if opendj_syntax is None:
                opendj_syntax = '1.3.6.1.4.1.1466.115.121.1.15'

            return opendj_syntax

    def import_ldif(self, ldif_files, bucket=None, force=None):

        sql_data_fn = os.path.join(Config.outputFolder, Config.rdbm_type, 'jans_data.sql')

        for ldif_fn in ldif_files:
            base.logIt("Importing entries from " + ldif_fn)
            parser = ldif_utils.myLdifParser(ldif_fn)
            parser.parse()

            for dn, entry in parser.entries:
                backend_location = force if force else self.get_backend_location_for_dn(dn)
                if backend_location == BackendTypes.LDAP:
                    if 'add' in  entry and 'changetype' in entry:
                        base.logIt("LDAP modify add dn:{} entry:{}".format(dn, dict(entry)))
                        change_attr = entry['add'][0]
                        ldap_operation_result = self.ldap_conn.modify(dn, {change_attr: [(ldap3.MODIFY_ADD, entry[change_attr])]})
                        self.log_ldap_result(ldap_operation_result)

                    elif 'replace' in  entry and 'changetype' in entry:
                        base.logIt("LDAP modify replace dn:{} entry:{}".format(dn, dict(entry)))
                        change_attr = entry['replace'][0]
                        ldap_operation_result = self.ldap_conn.modify(dn, {change_attr: [(ldap3.MODIFY_REPLACE, [entry[change_attr][0]])]})
                        self.log_ldap_result(ldap_operation_result)

                    elif not self.dn_exists(dn):
                        base.logIt("Adding LDAP dn:{} entry:{}".format(dn, dict(entry)))
                        ldap_operation_result = self.ldap_conn.add(dn, attributes=entry)
                        self.log_ldap_result(ldap_operation_result)

                elif backend_location == BackendTypes.MYSQL:

                    dn_parsed = dnutils.parse_dn(dn)
                    rdn_name = dn_parsed[0][0]
                    doc_id = dn_parsed[0][1]
                    objectClass = entry.get('objectClass') or entry.get('objectclass')
                    if 'top' in objectClass:
                        objectClass.remove('top')
                    if  len(objectClass) == 1 and objectClass[0].lower() == 'organizationalunit':
                        continue

                    objectClass = objectClass[-1]
                    entry.pop(rdn_name)
                    if 'objectClass' in entry:
                        entry.pop('objectClass')
                    elif 'objectclass' in entry:
                        entry.pop('objectclass')

                    table_name = objectClass

                    if self.dn_exists_rdbm(dn, table_name):
                        base.logIt("DN {} exsits in {} skipping".format(dn, Config.rdbm_type))
                        continue

                    cols = ['`doc_id`', '`objectClass`', '`dn`']
                    vals = ['"{}"'.format(doc_id), '"{}"'.format(objectClass), '"{}"'.format(dn)]
                    for lkey in entry:
                        cols.append('`{}`'.format(lkey))
                        data_type = self.ldap_sql_data_type_mapping[self.get_attr_syntax(lkey)]
                        data_type = data_type[Config.rdbm_type]['type']

                        if data_type in ('SMALLINT', 'INT'):
                            if entry[lkey][0].lower() in ('1', 'on', 'true', 'yes'):
                                vals.append('1')
                            else:
                                vals.append('0')
                        elif data_type == 'DATETIME':
                            vals.append(entry[lkey])
                        elif data_type == 'JSON':
                            if lkey in ('jansConfProperty', 'jansModuleProperty'):
                                for i, k in enumerate(entry[lkey][:]):
                                    entry[lkey][i] = json.loads(k)
                            data_= "'{}'".format(json.dumps({'v':entry[lkey]}))
                            vals.append(data_)
                        else:
                            vals.append(json.dumps(entry[lkey][0]))

                    if 'add' in  entry and 'changetype' in entry:
                        # to be implemented
                        pass
                
                    elif 'replace' in  entry and 'changetype' in entry:
                        # to be implemented
                        pass

                    else:
                        sql_cmd = 'INSERT INTO {} ({}) VALUES ({});'.format(
                                    table_name,
                                    ', '.join(cols),
                                    ', '.join(vals)
                                    )

                    self.exec_rdbm_query(sql_cmd)

                    with open(sql_data_fn, 'a') as w:
                        w.write(sql_cmd+'\n')


                elif backend_location == BackendTypes.COUCHBASE:
                    if len(entry) < 3:
                        continue
                    key, document = ldif_utils.get_document_from_entry(dn, entry)
                    cur_bucket = bucket if bucket else self.get_bucket_for_dn(dn)
                    base.logIt("Addnig document {} to Couchebase bucket {}".format(key, cur_bucket))

                    n1ql_list = []

                    if 'changetype' in document:
                        if 'replace' in document:
                            attribute = document['replace']
                            n1ql_list.append('UPDATE `%s` USE KEYS "%s" SET `%s`=%s' % (cur_bucket, key, attribute, json.dumps(document[attribute])))
                        elif 'add' in document:
                            attribute = document['add']
                            result = self.check_attribute_exists(key, attribute)
                            data = document[attribute]
                            if result:
                                if isinstance(data, list):
                                    for d in data:
                                        n1ql_list.append('UPDATE `%s` USE KEYS "%s" SET `%s`=ARRAY_APPEND(`%s`, %s)' % (cur_bucket, key, attribute, attribute, json.dumps(d)))
                                else:
                                    n1ql_list.append('UPDATE `%s` USE KEYS "%s" SET `%s`=ARRAY_APPEND(`%s`, %s)' % (cur_bucket, key, attribute, attribute, json.dumps(data)))
                            else:
                                if attribute in attribDataTypes.listAttributes and not isinstance(data, list):
                                    data = [data]
                                n1ql_list.append('UPDATE `%s` USE KEYS "%s" SET `%s`=%s' % (cur_bucket, key, attribute, json.dumps(data)))
                    else:
                        n1ql_list.append('UPSERT INTO `%s` (KEY, VALUE) VALUES ("%s", %s)' % (cur_bucket, key, json.dumps(document)))

                    for q in n1ql_list:
                        self.cbm.exec_query(q)

    def import_schema(self, schema_file):
        if self.moddb == BackendTypes.LDAP:
            base.logIt("Importing schema {}".format(schema_file))
            parser = ldif_utils.myLdifParser(schema_file)
            parser.parse()
            for dn, entry in parser.entries:
                if 'changetype' in entry:
                    entry.pop('changetype')
                if 'add' in entry:
                    entry.pop('add')
                for entry_type in entry:
                    for e in entry[entry_type]:
                        base.logIt("Adding to schema, type: {}  value: {}".format(entry_type, e))
                        ldap_operation_result = self.ldap_conn.modify(dn, {entry_type: [ldap3.MODIFY_ADD, e]})
                        self.log_ldap_result(ldap_operation_result)
            #we need re-bind after schema operations
            self.ldap_conn.rebind()

    def get_group_for_key(self, key):
        key_prefix = self.get_key_prefix(key)
        for group in Config.couchbaseBucketDict:
            if key_prefix in Config.couchbaseBucketDict[group]['document_key_prefix']:
                break
        else:
            group = 'default'

        return group

    def get_bucket_for_key(self, key):
        group = self.get_group_for_key(key)
        if group == 'default':
            return Config.couchbase_bucket_prefix

        return Config.couchbase_bucket_prefix + '_' + group

    def get_bucket_for_dn(self, dn):
        key = ldif_utils.get_key_from(dn)
        return self.get_bucket_for_key(key)

    def get_backend_location_for_dn(self, dn):
        key = ldif_utils.get_key_from(dn)
        group = self.get_group_for_key(key)
    
        if Config.mappingLocations[group] == 'ldap':
            return static.BackendTypes.LDAP

        if Config.mappingLocations[group] == 'rdbm':
            if Config.rdbm_type == 'mysql':
                return static.BackendTypes.MYSQL
        
        if Config.mappingLocations[group] == 'couchbase':
            return static.BackendTypes.COUCHBASE


    def checkCBRoles(self, buckets=[]):
        
        self.cb_bucket_roles = ['bucket_admin', 'query_delete', 'query_select', 
                            'query_update', 'query_insert',
                            'query_manage_index']
        
        result = self.cbm.whoami()
        bc = buckets[:]
        bucket_roles = {}
        if 'roles' in result:
            
            for role in result['roles']:
                if role['role'] == 'admin':
                    Config.isCouchbaseUserAdmin = True
                    return True, None

                if not role['bucket_name'] in bucket_roles:
                    bucket_roles[role['bucket_name']] = []

                bucket_roles[role['bucket_name']].append(role['role'])

        for b_ in bc[:]:
            for r_ in self.cb_bucket_roles:
                if not r_ in bucket_roles[b_]:
                    break
            else:
                bc.remove(b_)

        if bc:
            return False, bc

        return True, None


    def __del__(self):
        try:
            self.ldap_conn.unbind()
            self.ready = False
        except:
            pass


dbUtils = DBUtils()
