import os
import glob
import time
import json
import ldap3

from setup_app import paths
from setup_app import static
from setup_app.utils import base
from setup_app.config import Config
from setup_app.utils.setup_utils import SetupUtils
from setup_app.installers.base import BaseInstaller
from setup_app.utils.ldif_utils import myLdifParser
from setup_app.pylib.schema import ObjectClass
from setup_app.pylib.ldif4.ldif import LDIFWriter


class TestDataLoader(BaseInstaller, SetupUtils):

    passportInstaller = None
    scimInstaller = None

    def __init__(self):
        self.service_name = 'test-data'
        self.pbar_text = "Loading" 
        self.needdb = True
        self.app_type = static.AppType.SERVICE
        self.install_type = static.InstallOption.OPTONAL
        self.install_var = 'loadTestData'
        self.register_progess()

        self.template_base = os.path.join(Config.templateFolder, 'test')

    def create_test_client_keystore(self):
        self.logIt("Creating client_keystore.jks")
        client_keystore_fn = os.path.join(Config.outputFolder, 'test/oxauth/client/client_keystore.jks')
        keys_json_fn =  os.path.join(Config.outputFolder, 'test/oxauth/client/keys_client_keystore.json')

        args = [Config.cmd_keytool, '-genkey', '-alias', 'dummy', '-keystore', 
                    client_keystore_fn, '-storepass', 'secret', '-keypass', 
                    'secret', '-dname', 
                    "'{}'".format(Config.default_openid_jks_dn_name)
                    ]

        self.run(' '.join(args), shell=True)

        args = [Config.cmd_java, '-Dlog4j.defaultInitOverride=true',
                '-cp', Config.non_setup_properties['oxauth_client_jar_fn'], Config.non_setup_properties['key_gen_path'],
                '-keystore', client_keystore_fn,
                '-keypasswd', 'secret',
                '-sig_keys', Config.default_key_algs,
                '-enc_keys', Config.default_key_algs,
                '-dnname', "'{}'".format(Config.default_openid_jks_dn_name),
                '-expiration', '365','>', keys_json_fn]

        cmd = ' '.join(args)
        
        self.run(cmd, shell=True)

        self.copyFile(client_keystore_fn, os.path.join(Config.outputFolder, 'test/oxauth/server'))
        self.copyFile(keys_json_fn, os.path.join(Config.outputFolder, 'test/oxauth/server'))

    def load_test_data(self):

        if not self.scimInstaller.installed():
            self.logIt("Scim was not installed. Installing")
            Config.installScimServer = True
            self.scimInstaller.start_installation()

        self.encode_test_passwords()

        self.logIt("Rendering test templates")

        Config.templateRenderingDict['config_oxauth_test_ldap'] = '# Not available'
        Config.templateRenderingDict['config_oxauth_test_couchbase'] = '# Not available'

        if self.getMappingType('ldap'):
            template_text = self.readFile(os.path.join(self.template_base, 'oxauth/server/config-oxauth-test-ldap.properties.nrnd'))
            rendered_text = self.fomatWithDict(template_text, self.merge_dicts(Config.__dict__, Config.templateRenderingDict))
            Config.templateRenderingDict['config_oxauth_test_ldap'] = rendered_text

        if self.getMappingType('couchbase'):
            template_text = self.readFile(os.path.join(self.template_base, 'oxauth/server/config-oxauth-test-couchbase.properties.nrnd'))
            rendered_text = self.fomatWithDict(template_text, self.merge_dicts(Config.__dict__, Config.templateRenderingDict))
            Config.templateRenderingDict['config_oxauth_test_couchbase'] = rendered_text

        self.render_templates_folder(self.template_base)

        self.logIt("Loading test ldif files")

        ox_auth_test_ldif = os.path.join(Config.outputFolder, 'test/oxauth/data/oxauth-test-data.ldif')
        ox_auth_test_user_ldif = os.path.join(Config.outputFolder, 'test/oxauth/data/oxauth-test-data-user.ldif')
        
        scim_test_ldif = os.path.join(Config.outputFolder, 'test/scim-client/data/scim-test-data.ldif')
        scim_test_user_ldif = os.path.join(Config.outputFolder, 'test/scim-client/data/scim-test-data-user.ldif')

        ldif_files = (ox_auth_test_ldif, scim_test_ldif, ox_auth_test_user_ldif, scim_test_user_ldif)
        self.dbUtils.import_ldif(ldif_files)

        apache_user = 'www-data' if base.clone_type == 'deb' else 'apache'

        # Client keys deployment
        base.download('https://raw.githubusercontent.com/JansFederation/oxAuth/master/Client/src/test/resources/oxauth_test_client_keys.zip', '/var/www/html/oxauth_test_client_keys.zip')        
        self.run([paths.cmd_unzip, '-o', '/var/www/html/oxauth_test_client_keys.zip', '-d', '/var/www/html/'])
        self.run([paths.cmd_rm, '-rf', 'oxauth_test_client_keys.zip'])
        self.run([paths.cmd_chown, '-R', 'root:'+apache_user, '/var/www/html/oxauth-client'])


        oxAuthConfDynamic_changes = {
                                    'dynamicRegistrationCustomObjectClass':  'oxAuthClientCustomAttributes',
                                    'dynamicRegistrationCustomAttributes': [ "oxAuthTrustedClient", "myCustomAttr1", "myCustomAttr2", "oxIncludeClaimsInIdToken" ],
                                    'dynamicRegistrationExpirationTime': 86400,
                                    'dynamicGrantTypeDefault': [ "authorization_code", "implicit", "password", "client_credentials", "refresh_token", "urn:ietf:params:oauth:grant-type:uma-ticket" ],
                                    'legacyIdTokenClaims': True,
                                    'authenticationFiltersEnabled': True,
                                    'clientAuthenticationFiltersEnabled': True,
                                    'keyRegenerationEnabled': True,
                                    'openidScopeBackwardCompatibility': False,
                                    }

        custom_scripts = ('2DAF-F995', '2DAF-F996', '4BBE-C6A8')

        self.dbUtils.set_oxAuthConfDynamic(oxAuthConfDynamic_changes)
        
        
        # Enable custom scripts
        for inum in custom_scripts:
            self.dbUtils.enable_script(inum)


        if self.dbUtils.moddb == static.BackendTypes.LDAP:
            # Update LDAP schema
            openDjSchemaFolder = os.path.join(Config.ldapBaseFolder, 'config/schema/')
            self.copyFile(os.path.join(Config.outputFolder, 'test/oxauth/schema/102-oxauth_test.ldif'), openDjSchemaFolder)
            self.copyFile(os.path.join(Config.outputFolder, 'test/scim-client/schema/103-scim_test.ldif'), openDjSchemaFolder)

            schema_fn = os.path.join(openDjSchemaFolder, '77-customAttributes.ldif')

            obcl_parser = myLdifParser(schema_fn)
            obcl_parser.parse()

            for i, o in enumerate(obcl_parser.entries[0][1]['objectClasses']):
                objcl = ObjectClass(o)
                if 'gluuCustomPerson' in objcl.tokens['NAME']:
                    may_list = list(objcl.tokens['MAY'])
                    for a in ('scimCustomFirst','scimCustomSecond', 'scimCustomThird'):
                        if not a in may_list:
                            may_list.append(a)

                    objcl.tokens['MAY'] = tuple(may_list)
                    obcl_parser.entries[0][1]['objectClasses'][i] = objcl.getstr()

            tmp_fn = '/tmp/77-customAttributes.ldif'
            with open(tmp_fn, 'wb') as w:
                ldif_writer = LDIFWriter(w)
                for dn, entry in obcl_parser.entries:
                    ldif_writer.unparse(dn, entry)

            self.copyFile(tmp_fn, openDjSchemaFolder)
            cwd = os.path.join(Config.ldapBaseFolder, 'bin')
            dsconfigCmd = (
                '{} --trustAll --no-prompt --hostname {} --port {} '
                '--bindDN "{}" --bindPasswordFile /home/ldap/.pw set-connection-handler-prop '
                '--handler-name "LDAPS Connection Handler" --set listen-address:0.0.0.0'
                    ).format(
                        os.path.join(Config.ldapBaseFolder, 'bin/dsconfig'), 
                        Config.ldap_hostname, 
                        Config.ldap_admin_port,
                        Config.ldap_binddn
                    )
            
            self.run(['/bin/su', 'ldap', '-c', dsconfigCmd], cwd=cwd)

            self.dbUtils.ldap_conn.unbind()

            self.restart('opendj')
            #wait 10 seconds to start opendj
            time.sleep(10)

            for atr in ('myCustomAttr1', 'myCustomAttr2'):
                cmd = (
                    'create-backend-index --backend-name userRoot --type generic '
                    '--index-name {} --set index-type:equality --set index-entry-limit:4000 '
                    '--hostName {} --port {} --bindDN "{}" -j /home/ldap/.pw '
                    '--trustAll --noPropertiesFile --no-prompt'
                    ).format(
                        atr, 
                        Config.ldap_hostname,
                        Config.ldap_admin_port, 
                        Config.ldap_binddn
                    )

                dsconfigCmd = '{1} {2}'.format(Config.ldapBaseFolder, os.path.join(cwd, 'dsconfig'), cmd)
                self.run(['/bin/su', 'ldap', '-c', dsconfigCmd], cwd=cwd)

        else:
            self.dbUtils.cbm.exec_query('CREATE INDEX def_gluu_myCustomAttr1 ON `gluu`(myCustomAttr1) USING GSI WITH {"defer_build":true}')
            self.dbUtils.cbm.exec_query('CREATE INDEX def_gluu_myCustomAttr2 ON `gluu`(myCustomAttr2) USING GSI WITH {"defer_build":true}')
            self.dbUtils.cbm.exec_query('BUILD INDEX ON `gluu` (def_gluu_myCustomAttr1, def_gluu_myCustomAttr2)')

        self.dbUtils.ldap_conn.bind()

        result = self.dbUtils.search('ou=configuration,o=gluu', search_filter='(oxIDPAuthentication=*)', search_scope=ldap3.BASE)

        oxIDPAuthentication = json.loads(result['oxIDPAuthentication'])
        oxIDPAuthentication['config']['servers'] = ['{0}:{1}'.format(Config.hostname, Config.ldaps_port)]
        oxIDPAuthentication_js = json.dumps(oxIDPAuthentication, indent=2)
        self.dbUtils.set_configuration('oxIDPAuthentication', oxIDPAuthentication_js)

        self.create_test_client_keystore()

        # Disable token binding module
        if base.os_name in ('ubuntu18', 'ubuntu20'):
            self.run(['a2dismod', 'mod_token_binding'])
            self.restart('apache2')

        self.restart('oxauth')

        # Prepare for tests run
        #install_command, update_command, query_command, check_text = self.get_install_commands()
        #self.run_command(install_command.format('git'))
        #self.run([self.cmd_mkdir, '-p', 'oxAuth/Client/profiles/ce_test'])
        #self.run([self.cmd_mkdir, '-p', 'oxAuth/Server/profiles/ce_test'])
        # Todo: Download and unzip file test_data.zip from CE server.
        # Todo: Copy files from unziped folder test/oxauth/client/* into oxAuth/Client/profiles/ce_test
        # Todo: Copy files from unziped folder test/oxauth/server/* into oxAuth/Server/profiles/ce_test
        #self.run([self.cmd_keytool, '-import', '-alias', 'seed22.gluu.org_httpd', '-keystore', 'cacerts', '-file', '%s/httpd.crt' % self.certFolder, '-storepass', 'changeit', '-noprompt'])
        #self.run([self.cmd_keytool, '-import', '-alias', 'seed22.gluu.org_opendj', '-keystore', 'cacerts', '-file', '%s/opendj.crt' % self.certFolder, '-storepass', 'changeit', '-noprompt'])
 
