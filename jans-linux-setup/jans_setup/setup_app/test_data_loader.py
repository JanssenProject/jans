import os
import sys
import glob
import time
import json
import socket
import urllib.request
import base64
import shutil

from setup_app import paths
from setup_app import static
from setup_app.utils import base
from setup_app.config import Config
from setup_app.utils.setup_utils import SetupUtils
from setup_app.installers.base import BaseInstaller
from setup_app.utils.ldif_utils import myLdifParser, schema2json
from setup_app.pylib.schema import ObjectClass
from setup_app.pylib.ldif4.ldif import LDIFWriter
from setup_app.pylib.jproperties import Properties

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

    def create_test_client_keystore(self):
        self.logIt("Creating client_keystore.p12")
        client_keystore_fn = os.path.join(Config.output_dir, 'test/jans-auth/client/client_keystore.p12')
        keys_json_fn =  os.path.join(Config.output_dir, 'test/jans-auth/client/keys_client_keystore.json')

        args = [Config.cmd_keytool, '-genkey', '-alias', 'dummy', '-keystore', 
                    client_keystore_fn, '-storepass', 'secret', '-keypass', 
                    'secret', '-dname', 
                    "'{}'".format(Config.default_openid_jks_dn_name),
                    '-storetype', 'PKCS12'
                    ]

        self.run(' '.join(args), shell=True)

        args = [Config.cmd_java, '-Dlog4j.defaultInitOverride=true',
                '-cp', Config.non_setup_properties['jans_auth_client_jar_fn'], Config.non_setup_properties['key_gen_path'],
                '-key_ops_type', 'ALL',
                '-keystore', client_keystore_fn,
                '-keypasswd', 'secret',
                '-sig_keys', Config.default_sig_key_algs,
                '-enc_keys', Config.default_enc_key_algs,
                '-dnname', "'{}'".format(Config.default_openid_jks_dn_name),
                '-expiration', '365','>', keys_json_fn]

        cmd = ' '.join(args)

        self.run(cmd, shell=True)

        self.copyFile(client_keystore_fn, os.path.join(Config.output_dir, 'test/jans-auth/server'))
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
                'jans-auth-server/agama/engine/src/test/resources/templates',
                target_dir
                )
        scripts_target = os.path.join(base.current_app.JansAuthInstaller.agama_root, 'scripts')
        base.extract_from_zip(
                base.current_app.jans_zip,
                'jans-auth-server/agama/engine/src/test/resources/libs',
                scripts_target
                )
        self.chown(base.current_app.JansAuthInstaller.agama_root, Config.jetty_user, Config.jetty_group, recursive=True)

        prop_src_fn = os.path.join(agama_out_dir, 'config-agama-test.properties')
        self.renderTemplateInOut(prop_src_fn, agama_temp_dir, os.path.join(Config.output_dir, 'test/jans-auth'))

        dn, jans_auth_conf_dynamic = self.dbUtils.get_jans_auth_conf_dynamic()
        agama_config=jans_auth_conf_dynamic["agamaConfiguration"].copy()
        agama_config['disableTCHV'] = True
        agama_config['enabled'] = True
        self.dbUtils.set_jans_auth_conf_dynamic({'agamaConfiguration': agama_config})
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

        Config.templateRenderingDict['config_jans_auth_test_ldap'] = '# Not available'

        config_jans_auth_test_properties = self.fomatWithDict(
            'server.name=%(hostname)s\nconfig.oxauth.issuer=http://localhost:80\nconfig.oxauth.contextPath=http://localhost:80\nconfig.oxauth.salt=%(encode_salt)s\nconfig.persistence.type=%(persistence_type)s\n\n',
            self.merge_dicts(Config.__dict__, Config.templateRenderingDict)
            )

        if Config.rdbm_type in ('mysql', 'pgsql'):
            if Config.rdbm_type == 'mysql':
                Config.templateRenderingDict['rdbm_schema_name'] = Config.rdbm_db
                Config.templateRenderingDict['rdbm_name_str'] = Config.rdbm_type
            else:
                Config.templateRenderingDict['rdbm_schema_name'] = 'public'
                Config.templateRenderingDict['rdbm_name_str'] = 'postgresql'

            template_text = self.readFile(os.path.join(self.template_base, 'jans-auth/server/config-jans-auth-test-sql.properties.nrnd'))
            rendered_text = self.fomatWithDict(template_text, self.merge_dicts(Config.__dict__, Config.templateRenderingDict))
            config_jans_auth_test_properties += '\n#sql\n' +  rendered_text

            self.logIt("Adding custom attributs and indexes")

            schema2json(
                    os.path.join(Config.templateFolder, 'test/jans-auth/schema/102-jans-auth_test.ldif'),
                    os.path.join(Config.output_dir, 'test/jans-auth/schema/')
                    )
            schema2json(
                    os.path.join(Config.templateFolder, 'test/scim-client/schema/103-scim_test.ldif'),
                    os.path.join(Config.output_dir, 'test/scim-client/schema/'),
                    )

            jans_auth_json_schema_fn =os.path.join(Config.output_dir, 'test/jans-auth/schema/102-jans-auth_test.json')
            scim_json_schema_fn = os.path.join(Config.output_dir, 'test/scim-client/schema/103-scim_test.json')
            jans_schema_json_files = [ jans_auth_json_schema_fn, scim_json_schema_fn ]

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
            self.dbUtils.rdm_automapper(force=True)

        self.writeFile(
            os.path.join(Config.output_dir, 'test/jans-auth/server/config-jans-auth-test.properties'),
            config_jans_auth_test_properties
            )

        ignoredirs = []

        if not Config.install_config_api:
            ignoredirs.append(os.path.join(self.template_base, 'jans-config-api'))

        self.render_templates_folder(self.template_base, ignoredirs=ignoredirs)

        if Config.get('jca_client_id') or Config.get('jca_test_client_id'):
            jans_auth_test_data_server_properties_fn = os.path.join(Config.output_dir, 'test/jans-auth/server/config-jans-auth-test-data.properties')
            jans_auth_test_data_server_properties = Properties()

            with open(jans_auth_test_data_server_properties_fn, 'rb') as f:
                jans_auth_test_data_server_properties.load(f, 'utf-8')

            keep_clients = jans_auth_test_data_server_properties["test.keep.clients"].data.split(',')
            keep_clients = [client_id.strip() for client_id in keep_clients]

            if Config.get('jca_client_id'):
                keep_clients.append(Config.jca_client_id)
            if Config.get('jca_test_client_id'):
                keep_clients.append(Config.jca_test_client_id)

            jans_auth_test_data_server_properties["test.keep.clients"] = ', '.join(keep_clients)

            with open(jans_auth_test_data_server_properties_fn, 'wb') as w:
                jans_auth_test_data_server_properties.store(w)

        self.logIt("Loading test ldif files")
        Config.pbar.progress(self.service_name, "Importing ldif files", False)

        ox_auth_test_ldif = os.path.join(Config.output_dir, 'test/jans-auth/data/jans-auth-test-data.ldif')
        ox_auth_test_user_ldif = os.path.join(Config.output_dir, 'test/jans-auth/data/jans-auth-test-data-user.ldif')

        scim_test_ldif = os.path.join(Config.output_dir, 'test/scim-client/data/scim-test-data.ldif')
        scim_test_user_ldif = os.path.join(Config.output_dir, 'test/scim-client/data/scim-test-data-user.ldif')

        ldif_files = (ox_auth_test_ldif, scim_test_ldif, ox_auth_test_user_ldif, scim_test_user_ldif)
        self.dbUtils.import_ldif(ldif_files)

        # Client keys deployment
        target_jwks_fn = os.path.join(base.current_app.HttpdInstaller.server_root, 'jans_test_client_keys.zip')
        base.download('https://github.com/JanssenProject/jans/raw/main/jans-auth-server/client/src/test/resources/jans_test_client_keys.zip', target_jwks_fn)
        shutil.unpack_archive(target_jwks_fn, base.current_app.HttpdInstaller.server_root)

        self.removeFile(target_jwks_fn)

        self.chown(os.path.join(base.current_app.HttpdInstaller.server_root, 'jans-auth-client'), base.current_app.HttpdInstaller.apache_user, base.current_app.HttpdInstaller.apache_group, recursive=True)

        Config.pbar.progress(self.service_name, "Updating jans auth config", False)
        jans_auth_conf_dynamic_changes = {
                                    'dynamicRegistrationCustomObjectClass':  'jansClntCustomAttributes',
                                    'dynamicRegistrationCustomAttributes': [ "jansTrustedClnt", "myCustomAttr1", "myCustomAttr2", "jansInclClaimsInIdTkn" ],
                                    'dynamicRegistrationExpirationTime': 86400,
                                    'grantTypesAndResponseTypesAutofixEnabled': True,
                                    'grantTypesSupportedByDynamicRegistration': [ "authorization_code", "implicit", "password", "client_credentials", "refresh_token", "urn:ietf:params:oauth:grant-type:uma-ticket", "urn:openid:params:grant-type:ciba", "urn:ietf:params:oauth:grant-type:device_code", "urn:ietf:params:oauth:grant-type:token-exchange" ],
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
                                    'forceIdTokenHintPresence': False,
                                    'introspectionScriptBackwardCompatibility': False,
                                    'allowSpontaneousScopes': True,
                                    'accessEvaluationAllowBasicClientAuthorization': True,
                                    'spontaneousScopeLifetime': 0,
                                    'tokenEndpointAuthMethodsSupported': [ 'client_secret_basic', 'client_secret_post', 'client_secret_jwt', 'private_key_jwt', 'tls_client_auth', 'self_signed_tls_client_auth', 'none' ],
                                    'sessionIdRequestParameterEnabled': True,
                                    'skipRefreshTokenDuringRefreshing': False,
                                    'featureFlags': ['unknown', 'health_check', 'userinfo', 'clientinfo', 'id_generation', 'registration', 'introspection', 'revoke_token', 'revoke_session', 'global_token_revocation', 'end_session', 'status_session', 'jans_configuration', 'ciba', 'uma', 'u2f', 'device_authz', 'stat', 'par', 'ssa', 'status_list', 'access_evaluation'],
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
                jans_auth_conf_dynamic_changes.update(datajs)
                self.logIt("jans_auth_conf_dynamic was updated with auto test ciba patch")

        self.dbUtils.set_jans_auth_conf_dynamic(jans_auth_conf_dynamic_changes)

        self.enable_cusom_scripts()

        # make scope offline_access as default
        self.dbUtils.set_configuration("jansDefScope", "true", "inum=C4F6,ou=scopes,o=jans")

        self.create_test_client_keystore()

        self.load_agama_test_data()

        # change super gluu credentials url for test
        super_gluu_creds_fn = os.path.join(Config.certFolder, 'super_gluu_creds.json')
        super_gluu_creds = base.readJsonFile(super_gluu_creds_fn)
        super_gluu_creds['jans']['server_uri'] = 'https://cloud-dev.gluu.cloud/scan/push-api-server'
        self.writeFile(super_gluu_creds_fn, json.dumps(super_gluu_creds, indent=2), backup=False)
        self.chown(super_gluu_creds_fn, Config.jetty_user, Config.root_user)

        Config.pbar.progress(self.service_name, "Restarting Services", False)

        # Disable token binding module
        if base.os_name in ('ubuntu18', 'ubuntu20'):
            self.run(['a2dismod', 'mod_token_binding'])
            self.restart('apache2')

        self.restart('jans-auth')

        if Config.install_scim_server:
            self.restart('jans-scim')

        if Config.install_fido2:
            self.restart('jans-fido2')

        if Config.install_config_api:
            self.restart('jans-config-api')

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
 
