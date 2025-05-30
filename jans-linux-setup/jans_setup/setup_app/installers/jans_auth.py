import os
import glob
import random
import string
import uuid
import shutil
import json
import tempfile
import configparser

from urllib.parse import urlparse

from setup_app import paths
from setup_app.utils import base
from setup_app.config import Config
from setup_app.installers.jetty import JettyInstaller
from setup_app.static import AppType, InstallOption, SetupProfiles

Config.jans_auth_port = '8081'

class JansAuthInstaller(JettyInstaller):

    source_files = [
                    (os.path.join(Config.dist_jans_dir, 'jans-auth.war'), os.path.join(base.current_app.app_info['JANS_MAVEN'], 'maven/io/jans/jans-auth-server/{0}/jans-auth-server-{0}.war'.format(base.current_app.app_info['jans_version']))),
                    (os.path.join(Config.dist_jans_dir, 'jans-auth-client-jar-with-dependencies.jar'), os.path.join(base.current_app.app_info['JANS_MAVEN'], 'maven/io/jans/jans-auth-client/{0}/jans-auth-client-{0}-jar-with-dependencies.jar'.format(base.current_app.app_info['jans_version']))),
                    (os.path.join(Config.dist_jans_dir, 'jans-fido2-client.jar'), os.path.join(base.current_app.app_info['JANS_MAVEN'], 'maven/io/jans/jans-fido2-client/{0}/jans-fido2-client-{0}.jar'.format(base.current_app.app_info['jans_version']))),
                    (os.path.join(Config.dist_app_dir, 'twilio.jar'), os.path.join(base.current_app.app_info['TWILIO_MAVEN'], '{0}/twilio-{0}.jar'.format(base.current_app.app_info['TWILIO_VERSION']))),
                    (os.path.join(Config.dist_jans_dir, 'jans-fido2-model.jar'), os.path.join(base.current_app.app_info['JANS_MAVEN'], 'maven/io/jans/jans-fido2-model/{0}/jans-fido2-model-{0}.jar'.format(base.current_app.app_info['jans_version']))),
                   ]

    def __init__(self):
        setattr(base.current_app, self.__class__.__name__, self)
        self.service_name = 'jans-auth'
        self.app_type = AppType.SERVICE
        self.install_type = InstallOption.OPTONAL
        self.install_var = 'install_jans_auth'
        self.register_progess()

        self.templates_folder = os.path.join(Config.templateFolder, self.service_name)
        self.output_folder = os.path.join(Config.output_dir, self.service_name)

        self.ldif_config = os.path.join(self.output_folder, 'configuration.ldif')
        self.ldif_role_scope_mappings = os.path.join(self.output_folder, 'role-scope-mappings.ldif')
        self.jans_auth_config_json = os.path.join(self.output_folder, 'jans-auth-config.json')
        self.jans_auth_static_conf_json = os.path.join(self.templates_folder, 'jans-auth-static-conf.json')
        self.jans_auth_error_json = os.path.join(self.templates_folder, 'jans-auth-errors.json')
        self.jans_auth_openid_jwks_fn = os.path.join(self.output_folder, 'jans-auth-keys.json')
        self.jans_auth_openid_jks_fn = os.path.join(Config.certFolder, 'jans-auth-keys.' + Config.default_store_type.lower())
        self.ldif_people = os.path.join(self.output_folder, 'people.ldif')
        self.agama_root = os.path.join(self.jetty_base, self.service_name, 'agama')
        self.custom_lib_dir = os.path.join(self.jetty_base, self.service_name, 'custom/libs/')

        if Config.profile == SetupProfiles.OPENBANKING:
            Config.enable_ob_auth_script = '0' if base.argsp.disable_ob_auth_script else '1'
            Config.jwks_uri = base.argsp.jwks_uri

    def install(self):
        self.make_pairwise_calculation_salt()
        self.install_jettyService(self.jetty_app_configuration[self.service_name], True)
        self.set_class_path([os.path.join(self.custom_lib_dir, '*')])
        self.external_libs()
        self.data_cleaner_crontab()
        self.setup_agama()
        if Config.persistence_type == 'ldap':
            self.populate_jans_db_auth()
        self.enable()

    def generate_configuration(self):
        if not Config.get('jans_auth_openid_jks_pass'):
            Config.jans_auth_openid_jks_pass = self.getPW()

        if not Config.get('admin_inum'):
            Config.admin_inum = str(uuid.uuid4())

        Config.encoded_admin_password = self.ldap_encode(Config.admin_password)

        self.logIt("Generating OAuth openid keys", pbar=self.service_name)

        jwks = self.gen_openid_jwks_jks_keys(
                    jks_path=self.jans_auth_openid_jks_fn,
                    jks_pwd=Config.jans_auth_openid_jks_pass,
                    key_expiration=2,
                    key_algs=Config.default_sig_key_algs,
                    enc_keys=Config.default_enc_key_algs
                    )
        self.write_openid_keys(self.jans_auth_openid_jwks_fn, jwks)

        if Config.get('use_external_key'):
            self.import_openbanking_key()

    def get_config_api_scopes(self):
        scopes_def = base.current_app.ConfigApiInstaller.get_scope_defs()
        scope_list = []

        for resource in scopes_def['resources']:

            for condition in resource.get('conditions', []):

                for scope in condition.get('scopes', []):
                    if scope.get('inum') and scope.get('name'):
                        scope_list.append(scope['name'])

        return scope_list


    def role_scope_mappings(self):

        role_scope_mappings_fn = os.path.join(self.templates_folder, 'role-scope-mappings.json')
        role_mapping = base.readJsonFile(role_scope_mappings_fn)

        scope_list = self.get_config_api_scopes()

        for api_role in role_mapping['rolePermissionMapping']:
            if api_role['role'] == 'api-admin':
                break

        for scope in scope_list:
            if scope not in api_role['permissions']:
                api_role['permissions'].append(scope)

        Config.templateRenderingDict['role_scope_mappings'] = json.dumps(role_mapping)


    def render_import_templates(self):

        self.role_scope_mappings()

        Config.templateRenderingDict['person_custom_object_class_list'] = '[]'

        templates = [self.jans_auth_config_json, self.ldif_people]

        for tmp in templates:
            self.renderTemplateInOut(tmp, self.templates_folder, self.output_folder)

        if Config.profile == SetupProfiles.OPENBANKING:
            base.extract_file(
                base.current_app.jans_zip,
                'jans-linux-setup/jans_setup/static/extension/introspection/introspection_role_based_scope.py',
                os.path.join(Config.extensionFolder, 'introspection/')
                )

        self.prepare_base64_extension_scripts()

        Config.templateRenderingDict['jans_auth_config_base64'] = self.generate_base64_ldap_file(self.jans_auth_config_json)
        Config.templateRenderingDict['jans_auth_static_conf_base64'] = self.generate_base64_ldap_file(self.jans_auth_static_conf_json)
        Config.templateRenderingDict['jans_auth_error_base64'] = self.generate_base64_ldap_file(self.jans_auth_error_json)
        Config.templateRenderingDict['jans_auth_openid_key_base64'] = self.generate_base64_ldap_file(self.jans_auth_openid_jwks_fn)

        self.ldif_scripts = os.path.join(Config.output_dir, 'scripts.ldif')
        self.renderTemplateInOut(self.ldif_scripts, Config.templateFolder, Config.output_dir)
        for temp in (self.ldif_config, self.ldif_role_scope_mappings):
            self.renderTemplateInOut(temp, self.templates_folder, self.output_folder)

        self.dbUtils.import_ldif([self.ldif_config, self.ldif_scripts, self.ldif_role_scope_mappings, self.ldif_people])

        if Config.profile == SetupProfiles.OPENBANKING:
            self.import_openbanking_certificate()

    def genRandomString(self, N):
        return ''.join(random.SystemRandom().choice(string.ascii_lowercase
                                                    + string.ascii_uppercase
                                                    + string.digits) for _ in range(N))

    def make_pairwise_calculation_salt(self, enforce=False):
        if not Config.get('pairwiseCalculationKey') or enforce:
            Config.pairwiseCalculationKey = self.genRandomString(random.randint(20,30))
        if not Config.get('pairwiseCalculationSalt') or enforce:
            Config.pairwiseCalculationSalt = self.genRandomString(random.randint(20,30))

    def copy_static(self):
        for conf_fn in ('duo_creds.json', 'gplus_client_secrets.json', 'super_gluu_creds.json',
                        'vericloud_jans_creds.json', 'cert_creds.json', 'otp_configuration.json'):

            src_fn = os.path.join(Config.install_dir, 'static/auth/conf', conf_fn)
            self.copyFile(src_fn, Config.certFolder)

    def import_openbanking_certificate(self):
        self.logIt("Importing openbanking ssl certificate")
        jans_auth_config_json = base.readJsonFile(self.jans_auth_config_json)
        jwks_uri = jans_auth_config_json['jwksUri']
        o = urlparse(jwks_uri)
        jwks_addr = o.netloc
        open_banking_cert = self.get_server_certificate(jwks_addr)
        alias = jwks_addr.replace('.', '_')

        with tempfile.TemporaryDirectory() as tmp_dir:
            tmp_fn = os.path.join(tmp_dir, jwks_addr+'.crt')
            self.writeFile(tmp_fn, open_banking_cert)
            self.run([Config.cmd_keytool, '-import', '-trustcacerts', '-keystore', 
                      Config.defaultTrustStoreFN, '-storepass', 'changeit', 
                      '-noprompt', '-alias', alias, '-file', tmp_fn])


    def import_openbanking_key(self):
        if not os.path.isfile(Config.ob_cert_fn):
            self.download_ob_cert(Config.ob_cert_fn)

        if os.path.isfile(Config.ob_key_fn) and os.path.isfile(Config.ob_cert_fn):
            self.import_key_cert_into_keystore('obsigning', self.jans_auth_openid_jks_fn, Config.jans_auth_openid_jks_pass, Config.ob_key_fn, Config.ob_cert_fn, Config.ob_alias)

    def external_libs(self):
        for extra_lib in (self.source_files[2][0], self.source_files[3][0], self.source_files[4][0]):
            self.copyFile(extra_lib, self.custom_lib_dir)
            extra_lib_path = os.path.join(self.custom_lib_dir, os.path.basename(extra_lib))
            self.chown(extra_lib_path, Config.jetty_user, Config.jetty_group)

    def setup_agama(self):
        self.createDirs(self.agama_root)
        for adir in ('fl', 'ftl', 'scripts'):
            self.createDirs(os.path.join(self.agama_root, adir))
        base.extract_from_zip(base.current_app.jans_zip, 'agama/misc', self.agama_root)
        self.chown(self.agama_root, Config.jetty_user, Config.jetty_group, recursive=True)

        tmp_dir = os.path.join(Config.templateFolder, 'jetty')
        src_xml = os.path.join(tmp_dir, 'agama_web_resources.xml')
        self.renderTemplateInOut(src_xml, tmp_dir, self.jetty_service_webapps)
        self.chown(os.path.join(self.jetty_service_webapps, os.path.basename(src_xml)), Config.jetty_user, Config.jetty_group)


    def populate_jans_db_auth(self):
        ldap_config = {
            'type': 'auth',
            'name': None,
            'level': 0,
            'priority': 1,
            'enabled': False,
            'version': 0, 
            'config': {
                'configId': 'auth_ldap_server', 
                'servers': [f'{Config.ldap_hostname}:{Config.ldaps_port}'],
                'maxConnections': 1000,
                'bindDN': f'{Config.ldap_binddn}',
                'bindPassword': f'{Config.ldap_bind_encoded_pw}',
                'useSSL': True,
                'baseDNs': ['ou=people,o=jans'],
                'primaryKey': 'uid', 
                'localPrimaryKey': 'uid',
                'useAnonymousBind': False,
                'enabled': False
            }
        }

        self.logIt(f"Populating jansDbAuth with {ldap_config}")
        self.dbUtils.set_configuration('jansDbAuth', json.dumps(ldap_config, indent=2), dn='ou=configuration,o=jans')


    def data_cleaner_crontab(self):

        cleaner_dir = '/opt/jans/data-cleaner'
        cleaner_config_fn = os.path.join(cleaner_dir, 'data-clean.ini')

        if not os.path.exists(cleaner_dir):
            self.createDirs(cleaner_dir)

        # copy files
        crontab_fn = 'jans-clean-data-crontab.py'
        cleaner_fn = 'clean-data.py'
        for fn in (crontab_fn, cleaner_fn):
            source = os.path.join(Config.staticFolder, 'auth/data_clean/', fn)
            target = os.path.join(cleaner_dir, fn)
            self.copyFile(source, target, backup=False)
            self.run([paths.cmd_chmod, '+x', target])

        crontab_lib = os.path.join(base.pylib_dir, 'crontab.py')
        self.copyFile(crontab_lib, cleaner_dir, backup=False)


        # scan tables to clean and write config file
        tables = []
        for schema_fn in Config.schema_files:
            schema = base.readJsonFile(schema_fn)
            for cls in schema['objectClasses']:
                if 'exp' in cls['may'] and 'del' in cls['may']:
                    tables.append(cls['names'][0])

        config = configparser.ConfigParser()
        config['main'] = { 'tables': ' '.join(tables) }
        with open(cleaner_config_fn, 'w') as configfile:
            config.write(configfile)

        # create crontab entry
        self.run([os.path.join(cleaner_dir, crontab_fn)])
