import os
import glob
import json
import shutil
import datetime
import tempfile

from pathlib import Path

from setup_app import paths
from setup_app.utils import base
from setup_app.static import AppType, InstallOption
from setup_app.config import Config
from setup_app.installers.jetty import JettyInstaller
from setup_app.utils.ldif_utils import myLdifParser, create_client_ldif

Config.shibboleth_idp_port = '8086'
Config.shibboleth_idp_entity_id = ''


class ShibbolethInstaller(JettyInstaller):
    source_files = [
        (os.path.join(Config.dist_jans_dir, 'jans-shibboleth-idp-webapp.war'),
         os.path.join(base.current_app.app_info['JANS_MAVEN'],
                      'maven/io/jans/jans-shibboleth-idp-webapp/{0}/jans-shibboleth-idp-webapp-{0}.war').format(
             base.current_app.app_info['jans_version'])),
        (os.path.join(Config.dist_jans_dir, 'shibboleth-plugin.jar'),
         os.path.join(base.current_app.app_info['JANS_MAVEN'],
                      'maven/io/jans/jans-config-api/plugins/shibboleth-plugin/{0}/shibboleth-plugin-{0}-distribution.jar').format(
             base.current_app.app_info['jans_version'])),
    ]

    def __init__(self):
        setattr(base.current_app, self.__class__.__name__, self)
        self.service_name = 'jans-shibboleth-idp'
        self.needdb = True
        self.app_type = AppType.SERVICE
        self.install_type = InstallOption.OPTONAL
        self.install_var = 'install_jans_saml'
        self.register_progess()

        self.systemd_units = ['jans-shibboleth-idp']
        self.output_dir = os.path.join(Config.output_dir, self.service_name)
        self.template_dir = os.path.join(Config.templateFolder, self.service_name)
        self.shibboleth_home = '/opt/shibboleth-idp'
        self.dynamic_conf_json = os.path.join(self.output_dir, 'dynamic-conf.json')
        self.static_conf_json = os.path.join(self.output_dir, 'static-conf.json')
        self.config_ldif = os.path.join(self.output_dir, 'config.ldif')
        self.clients_ldif_fn = os.path.join(self.output_dir, 'clients.ldif')
        self.sp_metadata_dir = os.path.join(self.shibboleth_home, 'metadata')
        self.credentials_dir = os.path.join(self.shibboleth_home, 'credentials')

    def install(self):
        self.logIt("Installing Shibboleth IDP 5.1.6")

        self.create_directories()
        self.create_idp_client()
        self.configure_shibboleth()
        self.install_jetty_service()
        self.copy_static_files()
        self.import_config()

        self.enable()

    def create_directories(self):
        for d in [self.output_dir, self.shibboleth_home,
                  os.path.join(self.shibboleth_home, 'conf'),
                  os.path.join(self.shibboleth_home, 'conf/authn'),
                  self.credentials_dir, self.sp_metadata_dir,
                  os.path.join(self.shibboleth_home, 'logs')]:
            self.createDirs(d)

    def create_idp_client(self):
        self.logIt("Creating Shibboleth IDP OAuth client")

        _, jans_auth_config = self.dbUtils.get_jans_auth_conf_dynamic()
        Config.templateRenderingDict['jans_auth_token_endpoint'] = jans_auth_config['tokenEndpoint']

        jans_scopes = self.dbUtils.get_scopes()
        scope_openid = self.dbUtils.get_scope_by_jansid('openid')
        scope_profile = self.dbUtils.get_scope_by_jansid('profile')
        scope_email = self.dbUtils.get_scope_by_jansid('email')

        scopes = []
        for scope in [scope_openid, scope_profile, scope_email]:
            if scope:
                scopes.append(scope['dn'])

        shibboleth_client_prefix = '2300.'
        check_result = self.check_clients([('shibboleth_idp_client_id', shibboleth_client_prefix)])

        if check_result.get(shibboleth_client_prefix) == -1:
            redirect_uri = f'https://{Config.hostname}/idp/Authn/Jans/callback'

            create_client_ldif(
                ldif_fn=self.clients_ldif_fn,
                client_id=Config.shibboleth_idp_client_id,
                encoded_pw=Config.shibboleth_idp_client_encoded_pw,
                scopes=scopes,
                redirect_uri=[redirect_uri],
                display_name="Janssen Shibboleth IDP Client",
                grant_types=['authorization_code'],
                response_types=['code']
            )

            self.dbUtils.import_ldif([self.clients_ldif_fn])

    def configure_shibboleth(self):
        self.logIt("Configuring Shibboleth IDP")

        if not Config.shibboleth_idp_entity_id:
            Config.shibboleth_idp_entity_id = f'https://{Config.hostname}/idp/shibboleth'

        Config.templateRenderingDict['shibboleth_idp_entity_id'] = Config.shibboleth_idp_entity_id
        Config.templateRenderingDict['shibboleth_idp_scope'] = Config.hostname.split('.', 1)[
            -1] if '.' in Config.hostname else Config.hostname
        Config.templateRenderingDict['shibboleth_home'] = self.shibboleth_home

        self.generate_signing_keys()
        self.configure_idp_properties()
        self.configure_jans_authentication()

    def generate_signing_keys(self):
        self.logIt("Generating Shibboleth signing and encryption keys")

        signing_key_path = os.path.join(self.credentials_dir, 'idp-signing.key')
        signing_cert_path = os.path.join(self.credentials_dir, 'idp-signing.crt')
        encryption_key_path = os.path.join(self.credentials_dir, 'idp-encryption.key')
        encryption_cert_path = os.path.join(self.credentials_dir, 'idp-encryption.crt')
        sealer_keystore = os.path.join(self.credentials_dir, 'sealer.jks')

        signing_subject = f'/CN=Shibboleth Signing Certificate for {Config.hostname}'
        self.run([
            'openssl', 'req', '-new', '-x509', '-nodes',
            '-days', '3650', '-newkey', 'rsa:3072',
            '-keyout', signing_key_path, '-out', signing_cert_path,
            '-subj', signing_subject
        ])

        encryption_subject = f'/CN=Shibboleth Encryption Certificate for {Config.hostname}'
        self.run([
            'openssl', 'req', '-new', '-x509', '-nodes',
            '-days', '3650', '-newkey', 'rsa:3072',
            '-keyout', encryption_key_path, '-out', encryption_cert_path,
            '-subj', encryption_subject
        ])

        sealer_password = self.getPW()
        Config.shibboleth_sealer_password = sealer_password

        self.run([
            'keytool', '-genseckey', '-alias', 'sealer',
            '-keyalg', 'AES', '-keysize', '128',
            '-keystore', sealer_keystore,
            '-storepass', sealer_password, '-keypass', sealer_password,
            '-storetype', 'JCEKS'
        ])

    def configure_idp_properties(self):
        idp_properties_template = os.path.join(self.template_dir, 'idp.properties')
        idp_properties_output = os.path.join(self.shibboleth_home, 'conf', 'idp.properties')

        self.renderTemplateInOut(idp_properties_template, self.template_dir,
                                 os.path.join(self.shibboleth_home, 'conf'))

    def configure_jans_authentication(self):
        jans_properties_template = os.path.join(self.template_dir, 'jans.properties')
        jans_properties_output = os.path.join(self.shibboleth_home, 'conf', 'jans.properties')

        Config.templateRenderingDict['shibboleth_idp_client_id'] = Config.shibboleth_idp_client_id
        Config.templateRenderingDict['shibboleth_idp_client_secret'] = Config.shibboleth_idp_client_pw
        Config.templateRenderingDict['shibboleth_callback_uri'] = f'https://{Config.hostname}/idp/Authn/Jans/callback'

        self.renderTemplateInOut(jans_properties_template, self.template_dir,
                                 os.path.join(self.shibboleth_home, 'conf'))

    def install_jetty_service(self):
        self.logIt("Installing Shibboleth IDP Jetty service")

        self.installJettyService(self.jetty_app_configuration[self.service_name], True)

        war_file = os.path.join(Config.dist_jans_dir, 'jans-shibboleth-idp-webapp.war')
        webapps_dir = os.path.join(self.jetty_base, self.service_name, 'webapps')

        shutil.copy(war_file, os.path.join(webapps_dir, 'idp.war'))

    def copy_static_files(self):
        self.logIt("Copying Shibboleth static configuration files")

        static_dir = os.path.join(Config.install_dir, 'static', self.service_name)
        if os.path.exists(static_dir):
            for f in glob.glob(os.path.join(static_dir, '*')):
                shutil.copy(f, self.shibboleth_home)

    def import_config(self):
        self.logIt("Importing Shibboleth IDP configuration to database")

        config_ldif_template = os.path.join(self.template_dir, 'config.ldif')
        self.renderTemplateInOut(config_ldif_template, self.template_dir, self.output_dir)

        self.dbUtils.import_ldif([self.config_ldif])

    def create_folders(self):
        self.createDirs(self.output_dir)

    @property
    def jetty_app_configuration(self):
        return {
            self.service_name: {
                'memory': {'max_allowed_mb': 1024, 'metaspace_mb': 128, 'jvm_heap_ration': 0.7,
                           'min_jvm_heap_mem': 256},
                'jetty': {
                    'modules': 'server,deploy,webapp,http,resources,http-forwarded,console-capture,ee10-webapp,ee10-deploy'},
                'installed': False,
                'name': self.service_name
            }
        }

    def generate_configuration(self):
        self.check_clients([('shibboleth_idp_client_id', '2300.')])
