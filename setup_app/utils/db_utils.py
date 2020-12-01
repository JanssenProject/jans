import os
import json
import ldap3
from ldap3.utils import dn as dnutils

from setup_app import static
from setup_app.config import Config
from setup_app.static import InstallTypes, BackendTypes
from setup_app.utils import base
from setup_app.utils.cbm import CBM
from setup_app.utils import ldif_utils
from setup_app.utils.attributes import attribDataTypes
 

class DBUtils:

    processedKeys = []

    def bind(self, use_ssl=True):

        if Config.mappingLocations['default'] == 'ldap':
            self.moddb = BackendTypes.LDAP
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

        self.set_cbm()
        self.default_bucket = Config.couchbase_bucket_prefix

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
        elif self.moddb == BackendTypes.COUCHBASE:
            n1ql = 'SELECT * FROM `{}` USE KEYS "configuration_jans-auth  "'.format(self.default_bucket)
            result = cbm.exec_query(n1ql)
            js = result.json()
            dn = js['results'][0][self.default_bucket]['dn']
            oxAuthConfDynamic = js['results'][0][self.default_bucket]['oxAuthConfDynamic']

        return dn, oxAuthConfDynamic


    def get_oxTrustConfApplication(self):
        if self.moddb == BackendTypes.LDAP:
            self.ldap_conn.search(
                        search_base='o=jans',
                        search_scope=ldap3.SUBTREE,
                        search_filter='(objectClass=oxTrustConfiguration)',
                        attributes=['oxTrustConfApplication']
                        )
            dn = self.ldap_conn.response[0]['dn']
            oxTrustConfApplication = json.loads(self.ldap_conn.response[0]['attributes']['oxTrustConfApplication'][0])
        elif self.moddb == BackendTypes.COUCHBASE:
            n1ql = 'SELECT * FROM `{}` USE KEYS "configuration_oxtrust"'.format(self.default_bucket)
            result = self.cbm.exec_query(n1ql)
            js = result.json()
            dn = js['results'][0][self.default_bucket]['dn']
            oxTrustConfApplication = json.loads(js['results'][0][self.default_bucket]['oxTrustConfApplication'])

        return dn, oxTrustConfApplication


    def set_oxAuthConfDynamic(self, entries):
        if self.moddb == BackendTypes.LDAP:
            dn, oxAuthConfDynamic = self.get_oxAuthConfDynamic()
            oxAuthConfDynamic.update(entries)

            ldap_operation_result = self.ldap_conn.modify(
                    dn,
                    {"jansConfDyn": [ldap3.MODIFY_REPLACE, json.dumps(oxAuthConfDynamic, indent=2)]}
                    )
            self.log_ldap_result(ldap_operation_result)

        elif self.moddb == BackendTypes.COUCHBASE:
            for k in entries:
                n1ql = 'UPDATE `{}` USE KEYS "configuration_jans-auth" SET {}={}'.format(self.default_bucket, k, entries[k])
                self.cbm.exec_query(n1ql)


    def set_oxTrustConfApplication(self, entries):
        dn, oxTrustConfApplication = self.get_oxTrustConfApplication()
        oxTrustConfApplication.update(entries)
        oxTrustConfApplication_js = json.dumps(oxTrustConfApplication, indent=2)

        if self.moddb == BackendTypes.LDAP:
            ldap_operation_result = self.ldap_conn.modify(
                    dn,
                    {"oxTrustConfApplication": [ldap3.MODIFY_REPLACE, oxTrustConfApplication_js]}
                    )
            self.log_ldap_result(ldap_operation_result)

        elif self.moddb == BackendTypes.COUCHBASE:
            n1ql = 'UPDATE `{}` USE KEYS "configuration_oxtrust" SET `oxTrustConfApplication`={}'.format(self.default_bucket, json.dumps(oxTrustConfApplication_js))
            self.cbm.exec_query(n1ql)

    def enable_script(self, inum):
        if self.moddb == BackendTypes.LDAP:
            ldap_operation_result = self.ldap_conn.modify(
                    'inum={},ou=scripts,o=jans'.format(inum),
                    {"jansEnabled": [ldap3.MODIFY_REPLACE, 'true']}
                    )
            self.log_ldap_result(ldap_operation_result)
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
        elif self.moddb == BackendTypes.COUCHBASE:
            n1ql = 'UPDATE `{}` USE KEYS "configuration" SET {}={}'.format(self.default_bucket, component, value)
            self.cbm.exec_query(n1ql)


    def dn_exists(self, dn):
        base.logIt("Querying LDAP for dn {}".format(dn))
        return self.ldap_conn.search(search_base=dn, search_filter='(objectClass=*)', search_scope=ldap3.BASE, attributes=['*'])

    def search(self, search_base, search_filter='(objectClass=*)', search_scope=ldap3.LEVEL):
        base.logIt("Searching database for dn {} with filter {}".format(search_base, search_filter))
        backend_location = self.get_backend_location_for_dn(search_base)
        
        if backend_location == BackendTypes.LDAP:
            if self.ldap_conn.search(search_base=search_base, search_filter=search_filter, search_scope=search_scope, attributes=['*']):
                key, document = ldif_utils.get_document_from_entry(self.ldap_conn.response[0]['dn'], self.ldap_conn.response[0]['attributes'])
                return document

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

    def import_ldif(self, ldif_files, bucket=None, force=None):

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
