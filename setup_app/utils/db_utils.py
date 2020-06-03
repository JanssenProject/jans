import json
import ldap3

from setup_app.config import Config
from setup_app.utils import base
from setup_app.utils.cbm import CBM


# db types
LDAP  = 1
COUCH = 2

class DBUtils:
    ready = False
    processedKeys = []


    def bind(self, ldap_hostname=None, port=None, ldap_binddn=None, ldapPass=None, use_ssl=True):

        if Config.mappingLocations['default'] == 'ldap':
            self.moddb = LDAP


            if not ldap_hostname:
                ldap_hostname = Config.ldap_hostname

            if not port:
                port = int(Config.ldaps_port)

            if not ldap_binddn:
                ldap_binddn = Config.ldap_binddn
            
            if not ldapPass:
                ldapPass = Config.ldapPass

            ldap_server = ldap3.Server(ldap_hostname, port=port, use_ssl=use_ssl)
            self.ldap_conn = ldap3.Connection(
                        ldap_server,
                        user=ldap_binddn,
                        password=ldapPass,
                        )
            base.logIt("Making LDAP Connection to host {}:{} with user {}".format(ldap_hostname, port, ldap_binddn))
            self.ldap_conn.bind()
        else:
            self.moddb = COUCH
            self.cbm = CBM(Config.couchbase_hostname, Config.couchebaseClusterAdmin, Config.cb_password)
            self.default_bucket = Config.couchbase_bucket_prefix

        self.ready = True


    def get_oxAuthConfDynamic(self):
        if self.moddb == LDAP:
            self.ldap_conn.search(
                        search_base='o=gluu', 
                        search_scope=ldap3.SUBTREE,
                        search_filter='(objectClass=oxAuthConfiguration)',
                        attributes=["oxAuthConfDynamic"]
                        )

            dn = self.ldap_conn.response[0]['dn']
            oxAuthConfDynamic = json.loads(self.ldap_conn.response[0]['attributes']['oxAuthConfDynamic'][0])
        elif self.moddb == COUCH:
            n1ql = 'SELECT * FROM `{}` USE KEYS "configuration_oxauth"'.format(self.default_bucket)
            result = cbm.exec_query(n1ql)
            js = result.json()
            dn = js['results'][0][self.default_bucket]['dn']
            oxAuthConfDynamic = js['results'][0][self.default_bucket]['oxAuthConfDynamic']

        return dn, oxAuthConfDynamic


    def get_oxTrustConfApplication(self):
        if self.moddb == LDAP:
            self.ldap_conn.search(
                        search_base='o=gluu',
                        search_scope=ldap3.SUBTREE,
                        search_filter='(objectClass=oxTrustConfiguration)',
                        attributes=['oxTrustConfApplication']
                        )
            dn = self.ldap_conn.response[0]['dn']
            oxTrustConfApplication = json.loads(self.ldap_conn.response[0]['attributes']['oxTrustConfApplication'][0])
        elif self.moddb == COUCH:
            n1ql = 'SELECT * FROM `{}` USE KEYS "configuration_oxtrust"'.format(self.default_bucket)
            result = self.cbm.exec_query(n1ql)
            js = result.json()
            dn = js['results'][0][self.default_bucket]['dn']
            oxAuthConfDynamic = js['results'][0][self.default_bucket]['oxTrustConfApplication']

        return dn, oxTrustConfApplication


    def set_oxAuthConfDynamic(self, entries):
        if self.moddb == LDAP:
            dn, oxAuthConfDynamic = self.get_oxAuthConfDynamic()
            oxAuthConfDynamic.update(entries)

            self.ldap_conn.modify(
                    dn,
                    {"oxAuthConfDynamic": [ldap3.MODIFY_REPLACE, json.dumps(oxAuthConfDynamic, indent=2)]}
                    )
        elif self.moddb == COUCH:
            for k in entries:
                n1ql = 'UPDATE `{}` USE KEYS "configuration_oxauth" SET {}={}'.format(self.default_bucket, k, entries[k])
                self.cbm.exec_query(n1ql)


    def set_oxTrustConfApplication(self, entries):
        dn, oxTrustConfApplication = self.get_oxTrustConfApplication()
        oxTrustConfApplication.update(entries)

        if self.moddb == LDAP:
            self.ldap_conn.modify(
                    dn,
                    {"oxTrustConfApplication": [ldap3.MODIFY_REPLACE, json.dumps(oxTrustConfApplication, indent=2)]}
                    )
        elif self.moddb == COUCH:
            for k in entries:
                n1ql = 'UPDATE `{}` USE KEYS "configuration_oxtrust" SET {}={}'.format(self.default_bucket, k, entries[k])
                self.cbm.exec_query(n1ql)

    def enable_script(self, inum):
        if self.moddb == LDAP:
            self.ldap_conn.modify(
                    'inum={},ou=scripts,o=gluu'.format(inum),
                    {"oxEnabled": [ldap3.MODIFY_REPLACE, 'true']}
                    )
        elif self.moddb == COUCH:
            n1ql = 'UPDATE `{}` USE KEYS "scripts_{}" SET oxEnabled=true'.format(self.default_bucket, inum)
            self.cbm.exec_query(n1ql)

    def enable_service(self, service):
        if self.moddb == LDAP:
            self.ldap_conn.modify(
                'ou=configuration,o=gluu',
                {service: [ldap3.MODIFY_REPLACE, 'true']}
                )
        elif self.moddb == COUCH:
            n1ql = 'UPDATE `{}` USE KEYS "configuration" SET {}=true'.format(self.default_bucket, service)
            self.cbm.exec_query(n1ql)
        
    def set_configuration(self, component, value):
        
        if self.moddb == LDAP:
            self.ldap_conn.modify(
                'ou=configuration,o=gluu',
                {component: [ldap3.MODIFY_REPLACE, value]}
                )

        elif self.moddb == COUCH:
            n1ql = 'UPDATE `{}` USE KEYS "configuration" SET {}={}'.format(self.default_bucket, component, value)
            self.cbm.exec_query(n1ql)


    def dn_exists(self, dn):
        base.logIt("Querying LDAP for dn {}".format(dn))
        return self.ldap_conn.search(search_base=dn, search_filter='(objectClass=*)', search_scope=ldap3.BASE, attributes=['*'])

    def search(self, search_base, search_filter, search_scope=ldap3.LEVEL):
        return self.ldap_conn.search(search_base=search_base, search_filter=search_filter, search_scope=search_scope, attributes=['*'])


    def add2strlist(self, client_id, strlist):
        value2 = strlist.split(',')
        value2 = [v.strip() for v in value2]
        value2.append(client_id)
        return  ', '.join(value2)
    
    def add_client2script(self, script_inum, client_id):
        if self.moddb == LDAP:
            dn = 'inum={},ou=scripts,o=gluu'.format(script_inum)
            if self.dn_exists(dn):
                for e in self.ldap_conn.response[0]['attributes'].get('oxConfigurationProperty', []):
                    try:
                        oxConfigurationProperty = json.loads(e)
                    except:
                        continue
                    if isinstance(oxConfigurationProperty, dict) and oxConfigurationProperty.get('value1') == 'allowed_clients':
                        if not client_id in oxConfigurationProperty['value2']:
                            oxConfigurationProperty['value2'] = self.add2strlist(client_id, oxConfigurationProperty['value2'])
                            oxConfigurationProperty_js = json.dumps(oxConfigurationProperty)
                            self.ldap_conn.modify(
                                dn,
                                {'oxConfigurationProperty': [ldap3.MODIFY_DELETE, e]}
                                )
                            self.ldap_conn.modify(
                                dn,
                                {'oxConfigurationProperty': [ldap3.MODIFY_ADD, oxConfigurationProperty_js]}
                                )
        elif self.moddb == COUCH:
            n1ql = 'SELECT oxConfigurationProperty FROM `{}` USE KEYS "scripts_{}"'.format(self.default_bucket, script_inum)
            result = self.cbm.exec_query(n1ql)
            js = result.json()

            oxConfigurationProperties = js['results'][0]['oxConfigurationProperty']
            for oxconfigprop in oxConfigurationProperties:
                if oxconfigprop.get('value1') == 'allowed_clients' and not client_id in oxconfigprop['value2']:
                    oxconfigprop['value2'] = self.add2strlist(client_id, oxconfigprop['value2'])
                    n1ql = 'UPDATE `{}` USE KEYS "scripts_{}" SET oxConfigurationProperty={}'.format(self.default_bucket, script_inum, oxConfigurationProperties)
                    self.cbm.exec_query(n1ql)
                    break

    def checkIfAttributeExists(self, key, atribute,  documents):
        ka = key + '::' + atribute
        retVal = False

        if ka in self.processedKeys:
            return True
         
        for d in documents:
            if d[0] == key:
                if 'changetype' in d[1]:
                    continue
                if atribute in d[1]:
                    retVal = True
                else:
                    self.processedKeys.append(ka)
                    return True
                
        return retVal

    def import_ldif(self, ldif_files, bucket=None):
        if self.moddb == LDAP:
            for ldif_fn in ldif_files:
                parser = base.myLdifParser(ldif_fn)
                parser.parse()
                for dn, entry in parser.entries:
                    if not self.dn_exists(dn):
                        base.logIt("Adding LDAP dn:{} entry:{}".format(dn, dict(entry)))
                        self.ldap_conn.add(dn, attributes=entry)
        
        elif self.moddb == COUCH:

            key_prefixes = {}
            for cb in Config.couchbaseBucketDict:
                for prefix in Config.couchbaseBucketDict[cb]['document_key_prefix']:
                    key_prefixes[prefix] = cb
            
            for ldif in ldif_file_list:
                base.logIt("Importing ldif file %s to Couchebase bucket %s" % (ldif, bucket))
                documents = base.get_documents_from_ldif(ldif)

                for e in documents:
                    if bucket:
                        cur_bucket = bucket
                    else:
                        n_ = e[0].find('_')
                        document_key_prefix = e[0][:n_+1]
                        cur_bucket = Config.couchbase_bucket_prefix + '_' + key_prefixes[document_key_prefix] if document_key_prefix in key_prefixes else 'gluu'

                    n1ql = ''

                    if 'changetype' in e[1]:
                        if 'replace' in e[1]:
                            n1ql = 'UPDATE `%s` USE KEYS "%s" SET %s="%s";\n' % (cur_bucket, e[0], e[1]['replace'], e[1][e[1]['replace']])
                        elif 'add' in e[1]:
                            for m in e[1][e[1]['add']]:
                                if self.checkIfAttributeExists(e[0], e[1]['add'],  documents):
                                    n1ql += 'UPDATE `%s` USE KEYS "%s" SET %s=["%s"];\n' % (cur_bucket, e[0], e[1]['add'], m)
                                else:
                                    n1ql += 'UPDATE `%s` USE KEYS "%s" SET %s=ARRAY_APPEND(%s, "%s");\n' % (cur_bucket, e[0], e[1]['add'], e[1]['add'], m)
                    else:
                        n1ql = 'UPSERT INTO `%s` (KEY, VALUE) VALUES ("%s", %s);\n' % (cur_bucket, e[0], json.dumps(e[1]))

                    self.cbm.exec_query(n1ql)

    def import_schema(self, schema_file):
        base.logIt("Importing schema {}".format(schema_file))
        parser = base.myLdifParser(schema_file)
        parser.parse()
        for dn, entry in parser.entries:
            if 'changetype' in entry:
                entry.pop('changetype')
            if 'add' in entry:
                entry.pop('add')
            for entry_type in entry:
                for e in entry[entry_type]:
                    base.logIt("Adding to schema, type: {}  value: {}".format(entry_type, e))
                    self.ldap_conn.modify(dn, {entry_type: [ldap3.MODIFY_ADD, e]})

        #we need unbind and re-bind after schema operations
        self.ldap_conn.unbind()
        self.ldap_conn.bind()

    def __del__(self):
        try:
            self.ldap_conn.unbind()
            self.ready = False
        except:
            pass


dbUtils = DBUtils()
