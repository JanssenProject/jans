import os
import sys
import glob
import time
import json
import socket
import ldap3
import urllib.request
import base64

from setup_app import paths
from setup_app import static
from setup_app.utils import base
from setup_app.config import Config
from setup_app.utils.setup_utils import SetupUtils
from setup_app.installers.base import BaseInstaller
from setup_app.utils.ldif_utils import myLdifParser, schema2json
from setup_app.pylib.schema import ObjectClass
from setup_app.pylib.ldif4.ldif import LDIFWriter


class TestDataLoader(BaseInstaller, SetupUtils):

    def __init__(self):
        setattr(base.current_app, self.__class__.__name__, self)
        self.service_name = 'test-data'
        self.pbar_text = "Loading" 
        self.needdb = True
        self.app_type = static.AppType.APPLICATION
        self.install_type = static.InstallOption.OPTONAL
        self.install_var = 'loadTestData'
        self.register_progess()

        self.template_base = os.path.join(Config.templateFolder, 'test')
        self.test_client_keystore_fn = os.path.join(Config.output_dir, 'test/jans-auth/client', self.get_client_test_keystore_fn('client_keystore'))
        Config.templateRenderingDict['test_client_keystore_base_fn'] = os.path.basename(self.test_client_keystore_fn)
        
    def create_test_client_keystore_(self):

        self.logIt("Creating {}".format(Config.templateRenderingDict['test_client_keystore_base_fn']))
        keys_json_fn =  os.path.join(Config.outputFolder, 'test/oxauth/client/keys_client_keystore.json')

        client_cmd = self.get_key_gen_client_provider_cmd()

        args = [Config.cmd_java, '-Dlog4j.defaultInitOverride=true',
                "-cp", client_cmd,
                Config.non_setup_properties['key_gen_path'],
                '-keystore', self.test_client_keystore_fn,
                '-keystore_type', Config.default_store_type,
                '-keypasswd', 'secret',
                '-sig_keys', Config.default_sig_key_algs,
                '-enc_keys', Config.default_enc_key_algs,
                '-dnname', "'{}'".format(Config.default_openid_dstore_dn_name),
                '-expiration', '365','>', keys_json_fn]

        cmd = ' '.join(args)

        self.run(cmd, shell=True)

        self.copyFile(self.test_client_keystore_fn, os.path.join(Config.outputFolder, 'test/oxauth/server'))
        self.copyFile(keys_json_fn, os.path.join(Config.outputFolder, 'test/oxauth/server'))

    def create_test_client_keystore(self):
        self.logIt("Creating {}".format(Config.templateRenderingDict['test_client_keystore_base_fn']))
#        self.logIt("Creating client_keystore.p12")
#        client_keystore_fn = os.path.join(Config.output_dir, 'test/jans-auth/client/client_keystore.p12')
#        client_keystore_fn = os.path.join(Config.output_dir, 'test/jans-auth/client', self.get_client_test_keystore_fn('client_keystore'))
        keys_json_fn =  os.path.join(Config.output_dir, 'test/jans-auth/client/keys_client_keystore.json')

#        args = [Config.cmd_keytool, '-genkey', '-alias', 'dummy', '-keystore', 
#                    client_keystore_fn, '-storepass', 'secret', '-keypass', 
#                    'secret', '-dname', 
#                    "'{}'".format(Config.default_openid_jks_dn_name),
#                    '-storetype', 'PKCS12'
#                    ]

#        self.run(' '.join(args), shell=True)

        args = [Config.cmd_java, '-Dlog4j.defaultInitOverride=true',
                '-cp', Config.non_setup_properties['jans_auth_client_jar_fn'], Config.non_setup_properties['key_gen_path'],
                '-keystore', self.test_client_keystore_fn,
                '-keypasswd', 'secret',
                '-sig_keys', Config.default_sig_key_algs,
                '-enc_keys', Config.default_enc_key_algs,
                '-dnname', "'{}'".format(Config.default_openid_jks_dn_name),
                '-expiration', '365','>', keys_json_fn]

        cmd = ' '.join(args)

        self.run(cmd, shell=True)

        self.copyFile(self.test_client_keystore_fn, os.path.join(Config.output_dir, 'test/jans-auth/server'))
        self.copyFile(keys_json_fn, os.path.join(Config.output_dir, 'test/jans-auth/server'))

    def enable_cusom_scripts(self):
        self.logIt("Enabling custom scripts")
        custom_scripts = ('2DAF-F995', '2DAF-F996', '4BBE-C6A8', 'A51E-76DA', '0300-BA90')
        for inum in custom_scripts:
            self.dbUtils.enable_script(inum)

    def load_agama_test_data(self):
        agama_temp_dir = os.path.join(self.template_base, 'agama')
        agama_out_dir = os.path.join(Config.output_dir, 'test/agama')

        ldif_fn = os.path.join(agama_out_dir, 'agama-test-data.ldif')
        self.renderTemplateInOut(ldif_fn, agama_temp_dir, agama_out_dir)
        self.dbUtils.import_ldif([ldif_fn])

        target_dir = os.path.join(base.current_app.JansAuthInstaller.agama_root, 'ftl')
        base.extract_from_zip(
                base.current_app.jans_zip,
                'agama/engine/src/test/resources/templates',
                target_dir
                )
        self.chown(target_dir, Config.jetty_user, Config.jetty_group, recursive=True)

        prop_src_fn = os.path.join(agama_out_dir, 'config-agama-test.properties')
        self.renderTemplateInOut(prop_src_fn, agama_temp_dir, os.path.join(Config.output_dir, 'test/jans-auth'))

        dn, oxauth_conf_dynamic = self.dbUtils.get_oxAuthConfDynamic()
        agama_config=oxauth_conf_dynamic["agamaConfiguration"].copy()
        agama_config['disableTCHV'] = True
        agama_config['enabled'] = True
        self.dbUtils.set_oxAuthConfDynamic({'agamaConfiguration': agama_config})
        self.dbUtils.enable_script('BADA-BADA')

    def load_test_data(self):
        Config.pbar.progress(self.service_name, "Loading Test Data", False)
        self.logIt("Re-binding database")
        self.dbUtils.bind(force=True)

        self.logIt("Checking Internet conncetion")
        socket.setdefaulttimeout(3)
        try:
            socket.socket(socket.AF_INET, socket.SOCK_STREAM).connect(("8.8.8.8", 443))
        except:
            self.logIt("Failed to connect 8.8.8.8:443.", True)
            print("Test data loader needs internet connection. Giving up ...")
            return

        if not base.current_app.ScimInstaller.installed():
            self.logIt("Scim was not installed. Installing")
            Config.install_scim_server = True
            base.current_app.ScimInstaller.start_installation()

        self.encode_test_passwords()

        Config.pbar.progress(self.service_name, "Rendering templates", False)
        self.logIt("Rendering test templates")

        if Config.rdbm_type == 'spanner':
            Config.rdbm_password_enc = ''

        Config.templateRenderingDict['config_oxauth_test_ldap'] = '# Not available'
        Config.templateRenderingDict['config_oxauth_test_couchbase'] = '# Not available'


        config_oxauth_test_properties = self.fomatWithDict(
            'server.name=%(hostname)s\nconfig.oxauth.issuer=http://localhost:80\nconfig.oxauth.contextPath=http://localhost:80\nconfig.oxauth.salt=%(encode_salt)s\nconfig.persistence.type=%(persistence_type)s\n\n',
            self.merge_dicts(Config.__dict__, Config.templateRenderingDict)
            )

        if self.getMappingType('ldap'):
            template_text = self.readFile(os.path.join(self.template_base, 'jans-auth/server/config-oxauth-test-ldap.properties.nrnd'))
            rendered_text = self.fomatWithDict(template_text, self.merge_dicts(Config.__dict__, Config.templateRenderingDict))            
            config_oxauth_test_properties += '#ldap\n' +  rendered_text

        if self.getMappingType('couchbase'):
            couchbaseDict = base.current_app.CouchbaseInstaller.couchbaseDict()
            template_text = self.readFile(os.path.join(self.template_base, 'jans-auth/server/config-oxauth-test-couchbase.properties.nrnd'))
            rendered_text = self.fomatWithDict(template_text, self.merge_dicts(Config.__dict__, Config.templateRenderingDict, couchbaseDict))
            config_oxauth_test_properties += '\n#couchbase\n' +  rendered_text


        if self.getMappingType('rdbm'):

            if Config.rdbm_type == 'spanner': 
                template_text = self.readFile(os.path.join(self.template_base, 'jans-auth/server/config-oxauth-test-spanner.properties.nrnd'))
                rendered_text = self.fomatWithDict(template_text, self.merge_dicts(Config.__dict__, Config.templateRenderingDict))
                config_oxauth_test_properties += '\n#spanner\n' +  rendered_text

            else:
                template_text = self.readFile(os.path.join(self.template_base, 'jans-auth/server/config-oxauth-test-sql.properties.nrnd'))
                rendered_text = self.fomatWithDict(template_text, self.merge_dicts(Config.__dict__, Config.templateRenderingDict))
                config_oxauth_test_properties += '\n#sql\n' +  rendered_text

            self.logIt("Adding custom attributs and indexes")

            schema2json(
                    os.path.join(Config.templateFolder, 'test/jans-auth/schema/102-oxauth_test.ldif'),
                    os.path.join(Config.output_dir, 'test/jans-auth/schema/')
                    )
            schema2json(
                    os.path.join(Config.templateFolder, 'test/scim-client/schema/103-scim_test.ldif'),
                    os.path.join(Config.output_dir, 'test/scim-client/schema/'),
                    )

            oxauth_json_schema_fn =os.path.join(Config.output_dir, 'test/jans-auth/schema/102-oxauth_test.json')
            scim_json_schema_fn = os.path.join(Config.output_dir, 'test/scim-client/schema/103-scim_test.json')
            jans_schema_json_files = [ oxauth_json_schema_fn, scim_json_schema_fn ]

            scim_schema = base.readJsonFile(scim_json_schema_fn)
            may_list = []

            for attribute in scim_schema['attributeTypes']:
                may_list += attribute['names']

            jansPerson = {
                        'kind': 'STRUCTURAL',
                        'may': may_list,
                        'must': ['objectclass'],
                        'names': ['jansPerson'],
                        'oid': 'jansObjClass',
                        'sup': ['top'],
                        'x_origin': 'Jans created objectclass'
                        }
            scim_schema['objectClasses'].append(jansPerson)

            with open(scim_json_schema_fn, 'w') as w:
                json.dump(scim_schema, w, indent=2)

            self.dbUtils.read_jans_schema(others=jans_schema_json_files)

            base.current_app.RDBMInstaller.create_tables(jans_schema_json_files)

            if Config.rdbm_type != 'spanner': 
                self.dbUtils.rdm_automapper(force=True)

        self.writeFile(
            os.path.join(Config.output_dir, 'test/jans-auth/server/config-oxauth-test.properties'),
            config_oxauth_test_properties
            )

        ignoredirs = []

        if not Config.install_config_api:
            ignoredirs.append(os.path.join(self.template_base, 'jans-config-api'))

        self.render_templates_folder(self.template_base, ignoredirs=ignoredirs)

        self.logIt("Loading test ldif files")
        Config.pbar.progress(self.service_name, "Importing ldif files", False)

        ox_auth_test_ldif = os.path.join(Config.output_dir, 'test/jans-auth/data/oxauth-test-data.ldif')
        ox_auth_test_user_ldif = os.path.join(Config.output_dir, 'test/jans-auth/data/oxauth-test-data-user.ldif')

        scim_test_ldif = os.path.join(Config.output_dir, 'test/scim-client/data/scim-test-data.ldif')
        scim_test_user_ldif = os.path.join(Config.output_dir, 'test/scim-client/data/scim-test-data-user.ldif')

        ldif_files = (ox_auth_test_ldif, scim_test_ldif, ox_auth_test_user_ldif, scim_test_user_ldif)
        self.dbUtils.import_ldif(ldif_files)

        apache_user = 'www-data' if base.clone_type == 'deb' else 'apache'

        # Client keys deployment
        #base.download('https://raw.githubusercontent.com/JanssenProject/jans-auth-server/master/client/src/test/resources/jans_test_client_keys.zip', '/var/www/html/jans_test_client_keys.zip')
        base.download('https://raw.githubusercontent.com/JanssenProject/jans/a970d88d81f920973f3ba812db97448f135090a9/jans-auth-server/client/src/test/resources/jans_test_client_keys.zip', '/var/www/html/jans_test_client_keys.zip')
        
        self.run([paths.cmd_unzip, '-o', '/var/www/html/jans_test_client_keys.zip', '-d', '/var/www/html/'])
        self.run([paths.cmd_rm, '-rf', 'jans_test_client_keys.zip'])
        self.run([paths.cmd_chown, '-R', 'root:'+apache_user, '/var/www/html/jans-auth-client'])
        self.run([paths.cmd_chmod, '-R', '660', '/var/www/html/jans-auth-client'])
        self.run([paths.cmd_chmod, 'ug+X', '/var/www/html/jans-auth-client'])

        Config.pbar.progress(self.service_name, "Updating oxauth config", False)
        oxAuthConfDynamic_changes = {
                                    'dynamicRegistrationCustomObjectClass':  'jansClntCustomAttributes',
                                    'dynamicRegistrationCustomAttributes': [ "jansTrustedClnt", "myCustomAttr1", "myCustomAttr2", "jansInclClaimsInIdTkn" ],
                                    'dynamicRegistrationExpirationTime': 86400,
                                    'grantTypesAndResponseTypesAutofixEnabled': True,
                                    'dynamicGrantTypeDefault': [ "authorization_code", "implicit", "password", "client_credentials", "refresh_token", "urn:ietf:params:oauth:grant-type:uma-ticket", "urn:openid:params:grant-type:ciba", "urn:ietf:params:oauth:grant-type:device_code", "urn:ietf:params:oauth:grant-type:token-exchange" ],
                                    'legacyIdTokenClaims': True,
                                    'authenticationFiltersEnabled': True,
                                    'clientAuthenticationFiltersEnabled': True,
                                    'keyRegenerationEnabled': True,
                                    'openidScopeBackwardCompatibility': False,
                                    'forceOfflineAccessScopeToEnableRefreshToken' : False,
                                    'dynamicRegistrationPasswordGrantTypeEnabled' : True,
                                    'cibaEnabled': True,
                                    'backchannelTokenDeliveryModesSupported': ["poll", "ping", "push"],
                                    'backchannelAuthenticationRequestSigningAlgValuesSupported': [ "RS256", "RS384", "RS512", "ES256", "ES384", "ES512", "PS256", "PS384", "PS512" ],
                                    'backchannelClientId': '123-123-123',
                                    'backchannelUserCodeParameterSupported': True,
                                    'tokenEndpointAuthSigningAlgValuesSupported': [ 'HS256', 'HS384', 'HS512', 'RS256', 'RS384', 'RS512', 'ES256', 'ES384', 'ES512', 'PS256', 'PS384', 'PS512' ],
                                    'userInfoSigningAlgValuesSupported': [ 'none', 'HS256', 'HS384', 'HS512', 'RS256', 'RS384', 'RS512', 'ES256', 'ES384', 'ES512', 'PS256', 'PS384', 'PS512' ],
                                    'consentGatheringScriptBackwardCompatibility': False,
                                    'claimsParameterSupported': True,
                                    'grantTypesSupported': [ 'urn:openid:params:grant-type:ciba', 'authorization_code', 'urn:ietf:params:oauth:grant-type:uma-ticket', 'urn:ietf:params:oauth:grant-type:device_code', 'client_credentials', 'implicit', 'refresh_token', 'password', 'urn:ietf:params:oauth:grant-type:token-exchange' ],
                                    'idTokenSigningAlgValuesSupported': [ 'none', 'HS256', 'HS384', 'HS512', 'RS256', 'RS384', 'RS512', 'ES256', 'ES384', 'ES512', 'PS256', 'PS384', 'PS512' ],
                                    'accessTokenSigningAlgValuesSupported': [ 'none', 'HS256', 'HS384', 'HS512', 'RS256', 'RS384', 'RS512', 'ES256', 'ES384', 'ES512', 'PS256', 'PS384', 'PS512' ],
                                    'requestObjectSigningAlgValuesSupported': [ 'none', 'HS256', 'HS384', 'HS512', 'RS256', 'RS384', 'RS512', 'ES256', 'ES384', 'ES512', 'PS256', 'PS384', 'PS512' ],
                                    'softwareStatementValidationClaimName': 'jwks_uri',
                                    'softwareStatementValidationType': 'jwks_uri',
                                    'umaGrantAccessIfNoPolicies': True,
                                    'rejectJwtWithNoneAlg': False,
                                    'removeRefreshTokensForClientOnLogout': True,
                                    'fapiCompatibility': False,
                                    'forceIdTokenHintPrecense': False,
                                    'introspectionScriptBackwardCompatibility': False,
                                    'allowSpontaneousScopes': True,
                                    'spontaneousScopeLifetime': 0,
                                    'tokenEndpointAuthMethodsSupported': [ 'client_secret_basic', 'client_secret_post', 'client_secret_jwt', 'private_key_jwt', 'tls_client_auth', 'self_signed_tls_client_auth', 'none' ],
                                    'sessionIdRequestParameterEnabled': True,
                                    'skipRefreshTokenDuringRefreshing': False,
                                    'featureFlags': ['unknown', 'health_check', 'userinfo', 'clientinfo', 'id_generation', 'registration', 'introspection', 'revoke_token', 'revoke_session', 'end_session', 'status_session', 'jans_configuration', 'ciba', 'uma', 'u2f', 'device_authz', 'stat', 'par', 'ssa'],
                                    'cleanServiceInterval':7200,
                                    'loggingLevel': 'TRACE',
                                    }

        if Config.get('config_patch_creds'):
            data = None
            datajs = None
            patch_url = os.path.join(base.current_app.app_info['JANS_MAVEN'], 'protected/jans-auth/jans-auth-test-config-patch.json')
            req = urllib.request.Request(patch_url)
            credentials = Config.get('config_patch_creds')
            encoded_credentials = base64.b64encode(credentials.encode('ascii'))
            req.add_header('Authorization', 'Basic %s' % encoded_credentials.decode("ascii"))
            self.logIt("Retreiving auto test ciba patch from " + patch_url)

            try:
                resp = urllib.request.urlopen(req)
                data = resp.read()
                self.logIt("Auto test ciba patch retreived")
            except:
                self.logIt("Can't retreive auto test ciba patch", True)

            if data:
                try:
                    datajs = json.loads(data.decode())
                except:
                    self.logIt("Can't decode json for auto test ciba patch", True)

            if datajs:
                oxAuthConfDynamic_changes.update(datajs)
                self.logIt("oxAuthConfDynamic was updated with auto test ciba patch")

        self.dbUtils.set_oxAuthConfDynamic(oxAuthConfDynamic_changes)

        self.enable_cusom_scripts()

        if self.dbUtils.moddb == static.BackendTypes.LDAP:
            # Update LDAP schema
            Config.pbar.progress(self.service_name, "Updating schema", False)
            openDjSchemaFolder = os.path.join(Config.ldap_base_dir, 'config/schema/')
            self.copyFile(os.path.join(Config.output_dir, 'test/jans-auth/schema/102-oxauth_test.ldif'), openDjSchemaFolder)
            self.copyFile(os.path.join(Config.output_dir, 'test/scim-client/schema/103-scim_test.ldif'), openDjSchemaFolder)

            schema_fn = os.path.join(openDjSchemaFolder, '77-customAttributes.ldif')

            obcl_parser = myLdifParser(schema_fn)
            obcl_parser.parse()

            for i, o in enumerate(obcl_parser.entries[0][1]['objectClasses']):
                objcl = ObjectClass(o)
                if 'jansCustomPerson' in objcl.tokens['NAME']:
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

            for test_schema in ('102-oxauth_test.ldif', '103-scim_test.ldif', '77-customAttributes.ldif'):
                self.run([paths.cmd_chown, '{0}:{0}'.format(Config.ldap_user), os.path.join(openDjSchemaFolder, test_schema)])

            self.logIt("Making opndj listen all interfaces")
            ldap_operation_result = self.dbUtils.ldap_conn.modify(
                    'cn=LDAPS Connection Handler,cn=Connection Handlers,cn=config', 
                     {'ds-cfg-listen-address': [ldap3.MODIFY_REPLACE, '0.0.0.0']}
                    )

            if not ldap_operation_result:
                    self.logIt("Ldap modify operation failed {}".format(str(self.ldap_conn.result)))
                    self.logIt("Ldap modify operation failed {}".format(str(self.ldap_conn.result)), True)

            self.dbUtils.ldap_conn.unbind()

            self.logIt("Re-starting opendj")
            self.restart('opendj')

            self.logIt("Re-binding opendj")
            # try 5 times to re-bind opendj
            for i in range(5):
                time.sleep(5)
                self.logIt("Try binding {} ...".format(i+1))
                bind_result = self.dbUtils.ldap_conn.bind()
                if bind_result:
                    self.logIt("Binding to opendj was successful")
                    break
                self.logIt("Re-try in 5 seconds")
            else:
                self.logIt("Re-binding opendj FAILED")
                sys.exit("Re-binding opendj FAILED")

            for atr in ('myCustomAttr1', 'myCustomAttr2'):

                dn = 'ds-cfg-attribute={},cn=Index,ds-cfg-backend-id={},cn=Backends,cn=config'.format(atr, 'userRoot')
                entry = {
                            'objectClass': ['top','ds-cfg-backend-index'],
                            'ds-cfg-attribute': [atr],
                            'ds-cfg-index-type': ['equality'],
                            'ds-cfg-index-entry-limit': ['4000']
                            }
                self.logIt("Creating Index {}".format(dn))
                ldap_operation_result = self.dbUtils.ldap_conn.add(dn, attributes=entry)
                if not ldap_operation_result:
                    self.logIt("Ldap modify operation failed {}".format(str(self.dbUtils.ldap_conn.result)))
                    self.logIt("Ldap modify operation failed {}".format(str(self.dbUtils.ldap_conn.result)), True)

        elif self.dbUtils.moddb in (static.BackendTypes.MYSQL, static.BackendTypes.PGSQL):
            pass

        elif self.dbUtils.moddb == static.BackendTypes.COUCHBASE:
            self.dbUtils.cbm.exec_query('CREATE INDEX def_{0}_myCustomAttr1 ON `{0}`(myCustomAttr1) USING GSI WITH {{"defer_build":true}}'.format(Config.couchbase_bucket_prefix))
            self.dbUtils.cbm.exec_query('CREATE INDEX def_{0}_myCustomAttr2 ON `{0}`(myCustomAttr2) USING GSI WITH {{"defer_build":true}}'.format(Config.couchbase_bucket_prefix))
            self.dbUtils.cbm.exec_query('BUILD INDEX ON `{0}` (def_{0}_myCustomAttr1, def_{0}_myCustomAttr2)'.format(Config.couchbase_bucket_prefix))

        if self.dbUtils.moddb == static.BackendTypes.LDAP:
            self.dbUtils.ldap_conn.bind()

            result = self.dbUtils.search('ou=configuration,o=jans', search_filter='(&(jansDbAuth=*)(objectClass=jansAppConf))', search_scope=ldap3.BASE)
            oxIDPAuthentication = json.loads(result['jansDbAuth'])
            oxIDPAuthentication['config']['servers'] = ['{0}:{1}'.format(Config.hostname, Config.ldaps_port)]
            oxIDPAuthentication_js = json.dumps(oxIDPAuthentication, indent=2)
            self.dbUtils.set_configuration('jansDbAuth', oxIDPAuthentication_js)

        self.create_test_client_keystore()

        self.load_agama_test_data()


        # Disable token binding module
        if base.os_name in ('ubuntu18', 'ubuntu20'):
            self.run(['a2dismod', 'mod_token_binding'])
            self.restart('apache2')

        self.restart('jans-auth')

        if Config.installEleven:
            eleven_tokens_package = os.path.join(Config.staticFolder, 'eleven/jans-eleven-tokens.tar.gz')
            target_dir = '/var/lib/softhsm/tokens/'
            if not os.path.exists(target_dir):
                os.makedirs(target_dir)
            self.run([paths.cmd_tar, '-zxf', eleven_tokens_package, '-C', target_dir])

        if Config.install_scim_server:
            self.restart('jans-scim')

        if Config.installFido2:
            self.restart('jans-fido2')

        if Config.install_config_api:
            self.restart('jans-config-api')

        if Config.installEleven:
            self.restart('jans-eleven')


        # Prepare for tests run
        #install_command, update_command, query_command, check_text = self.get_install_commands()
        #self.run_command(install_command.format('git'))
        #self.run([self.cmd_mkdir, '-p', 'jans-auth/Client/profiles/ce_test'])
        #self.run([self.cmd_mkdir, '-p', 'jans-auth/Server/profiles/ce_test'])
        # Todo: Download and unzip file test_data.zip from CE server.
        # Todo: Copy files from unziped folder test/jans-auth/client/* into jans-auth/Client/profiles/ce_test
        # Todo: Copy files from unziped folder test/jans-auth/server/* into jans-auth/Server/profiles/ce_test
        #self.run([self.cmd_keytool, '-import', '-alias', 'seed22.gluu.org_httpd', '-keystore', 'cacerts', '-file', '%s/httpd.crt' % self.certFolder, '-storepass', 'changeit', '-noprompt'])
        #self.run([self.cmd_keytool, '-import', '-alias', 'seed22.gluu.org_opendj', '-keystore', 'cacerts', '-file', '%s/opendj.crt' % self.certFolder, '-storepass', 'changeit', '-noprompt'])
 
