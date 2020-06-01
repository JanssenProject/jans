import json
import ldap3

from setup_app.config import Config
from setup_app.utils import base

class LDAPUtils:

    def __init__(self, ldap_hostname=None, port=None, ldap_binddn=None, ldapPass=None, use_ssl=True):

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



    def get_oxAuthConfDynamic(self):

        self.ldap_conn.search(
                    search_base='o=gluu', 
                    search_scope=ldap3.SUBTREE,
                    search_filter='(objectClass=oxAuthConfiguration)',
                    attributes=["oxAuthConfDynamic"]
                    )

        dn = self.ldap_conn.response[0]['dn']
        oxAuthConfDynamic = json.loads(self.ldap_conn.response[0]['attributes']['oxAuthConfDynamic'][0])

        return dn, oxAuthConfDynamic


    def get_oxTrustConfApplication(self):
        self.ldap_conn.search(
                    search_base='o=gluu',
                    search_scope=ldap3.SUBTREE,
                    search_filter='(objectClass=oxTrustConfiguration)',
                    attributes=['oxTrustConfApplication']
                    )
        dn = self.ldap_conn.response[0]['dn']
        oxTrustConfApplication = json.loads(self.ldap_conn.response[0]['attributes']['oxTrustConfApplication'][0])

        return dn, oxTrustConfApplication


    def set_oxAuthConfDynamic(self, entries):
        dn, oxAuthConfDynamic = self.get_oxAuthConfDynamic()
        oxAuthConfDynamic.update(entries)

        self.ldap_conn.modify(
                dn,
                {"oxAuthConfDynamic": [ldap3.MODIFY_REPLACE, json.dumps(oxAuthConfDynamic, indent=2)]}
                )

    def set_oxTrustConfApplication(self, entries):
        dn, oxTrustConfApplication = self.get_oxTrustConfApplication()
        oxTrustConfApplication.update(entries)

        self.ldap_conn.modify(
                dn,
                {"oxTrustConfApplication": [ldap3.MODIFY_REPLACE, json.dumps(oxTrustConfApplication, indent=2)]}
                )

    def enable_script(self, inum):
        self.ldap_conn.modify(
                'inum={},ou=scripts,o=gluu'.format(inum),
                {"oxEnabled": [ldap3.MODIFY_REPLACE, 'true']}
                )
    
    def enable_service(self, service):
        self.ldap_conn.modify(
            'ou=configuration,o=gluu',
            {service: [ldap3.MODIFY_REPLACE, 'true']}
            )

    def set_configuration(self, component, value):
                self.ldap_conn.modify(
                'ou=configuration,o=gluu',
                {component: [ldap3.MODIFY_REPLACE, value]}
                )

    def dn_exists(self, dn):
        base.logIt("Querying LDAP for dn {}".format(dn))
        return self.ldap_conn.search(search_base=dn, search_filter='(objectClass=*)', search_scope=ldap3.BASE, attributes=['*'])

    def search(self, search_base, search_filter, search_scope=ldap3.LEVEL):
        return self.ldap_conn.search(search_base=search_base, search_filter=search_filter, search_scope=search_scope, attributes=['*'])

    def add_client2script(self, script_inum, client_id):
        dn = 'inum={},ou=scripts,o=gluu'.format(script_inum)
        if self.dn_exists(dn):
            for e in self.ldap_conn.response[0]['attributes'].get('oxConfigurationProperty', []):
                try:
                    oxConfigurationProperty = json.loads(e)
                except:
                    continue
                if isinstance(oxConfigurationProperty, dict) and oxConfigurationProperty.get('value1') == 'allowed_clients':
                    if not client_id in oxConfigurationProperty['value2']:
                        value2 = oxConfigurationProperty['value2'].split(',')
                        value2 = [v.strip() for v in value2]
                        value2.append(client_id)
                        oxConfigurationProperty['value2'] = ', '.join(value2)
                        oxConfigurationProperty_js = json.dumps(oxConfigurationProperty)
                        self.ldap_conn.modify(
                            dn,
                            {'oxConfigurationProperty': [ldap3.MODIFY_DELETE, e]}
                            )
                        self.ldap_conn.modify(
                            dn,
                            {'oxConfigurationProperty': [ldap3.MODIFY_ADD, oxConfigurationProperty_js]}
                            )

    def import_ldif(self, ldif_files):
        for ldif_fn in ldif_files:
            parser = base.myLdifParser(ldif_fn)
            parser.parse()
            for dn, entry in parser.entries:
                if not self.dn_exists(dn):
                    base.logIt("Adding LDAP dn:{} entry:{}".format(dn, dict(entry)))
                    self.ldap_conn.add(dn, attributes=entry)


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
        except:
            pass
