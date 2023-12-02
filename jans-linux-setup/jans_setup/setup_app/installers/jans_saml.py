import os
import glob
import shutil
import time
import socket
import tempfile
import uuid

from setup_app import paths
from setup_app.utils import base
from setup_app.utils.package_utils import packageUtils
from setup_app.static import AppType, InstallOption
from setup_app.config import Config
from setup_app.installers.jetty import JettyInstaller
from setup_app.utils.ldif_utils import create_client_ldif

# Config
Config.idp_config_http_port = '8083'
Config.idp_config_hostname = 'localhost'
Config.jans_idp_enabled = 'true'
Config.jans_idp_realm = 'jans-api'
Config.jans_idp_client_id = f'jans-api-{uuid.uuid4()}'
Config.jans_idp_client_secret = os.urandom(10).hex()
Config.jans_idp_grant_type = 'PASSWORD'
Config.jans_idp_user_name = 'jans-api'
Config.jans_idp_user_password = os.urandom(10).hex()
Config.jans_idp_idp_root_dir = os.path.join(Config.jansOptFolder, 'idp')
Config.jans_idp_idp_metadata_file_pattern = '%s-idp-metadata.xml'
Config.jans_idp_ignore_validation = 'true'
Config.jans_idp_idp_metadata_file = 'idp-metadata.xml'

# change this when we figure out this
Config.keycloack_hostname = 'localhost'


class JansSamlInstaller(JettyInstaller):

    install_var = 'install_jans_saml'
    setattr(Config, install_var + '_pre_released', True)

    source_files = [
        (os.path.join(Config.dist_jans_dir, 'kc-jans-storage-plugin.jar'), os.path.join(base.current_app.app_info['JANS_MAVEN'], 'maven/io/jans/kc-jans-storage-plugin/{0}/kc-jans-storage-plugin-{0}.jar').format(base.current_app.app_info['jans_version'])),
        (os.path.join(Config.dist_jans_dir, 'jans-scim-model.jar'), os.path.join(base.current_app.app_info['JANS_MAVEN'], 'maven/io/jans/jans-scim-model/{0}/jans-scim-model-{0}.jar').format(base.current_app.app_info['jans_version'])),
        (os.path.join(Config.dist_jans_dir, 'kc-jans-storage-plugin-deps.zip'), os.path.join(base.current_app.app_info['JANS_MAVEN'], 'maven/io/jans/kc-jans-storage-plugin/{0}/kc-jans-storage-plugin-{0}-deps.zip').format(base.current_app.app_info['jans_version'])),
        (os.path.join(Config.dist_app_dir, 'keycloak.zip'), 'https://github.com/keycloak/keycloak/releases/download/{0}/keycloak-{0}.zip'.format(base.current_app.app_info['KC_VERSION'])),
        (os.path.join(Config.dist_jans_dir, 'kc-jans-authn-plugin.jar'), os.path.join(base.current_app.app_info['JANS_MAVEN'], 'maven/io/jans/kc-jans-authn-plugin/{0}/kc-jans-authn-plugin-{0}.jar').format(base.current_app.app_info['jans_version'])),
        (os.path.join(Config.dist_jans_dir, 'kc-jans-authn-plugin-deps.zip'), os.path.join(base.current_app.app_info['JANS_MAVEN'], 'maven/io/jans/kc-jans-authn-plugin/{0}/kc-jans-authn-plugin-{0}-deps.zip').format(base.current_app.app_info['jans_version'])),
            ]

    def __init__(self):
        setattr(base.current_app, self.__class__.__name__, self)
        self.service_name = 'jans-saml'
        self.needdb = True
        self.app_type = AppType.SERVICE
        self.install_type = InstallOption.OPTONAL
        self.systemd_units = ['kc']
        self.register_progess()

        self.saml_enabled = True
        self.config_generation = True
        self.ignore_validation = True
        self.idp_root_dir = os.path.join(Config.opt_dir, 'idp/configs/')

        # sample config
        self.idp_config_id = 'keycloak'
        self.idp_config_root_dir = os.path.join(self.idp_root_dir, self.idp_config_id)
        self.idp_config_enabled = 'true'
        self.idp_config_temp_meta_dir = os.path.join(self.idp_root_dir, self.idp_config_id, 'temp_metadata')
        self.idp_config_meta_dir = os.path.join(self.idp_root_dir, self.idp_config_id, 'metadata')

        self.idp_config_data_dir = os.path.join(Config.opt_dir, self.idp_config_id)
        self.idp_config_log_dir = os.path.join(self.idp_config_data_dir, 'logs')
        self.idp_config_providers_dir = os.path.join(self.idp_config_data_dir, 'providers')
        self.output_folder = os.path.join(Config.output_dir, self.service_name)
        self.templates_folder = os.path.join(Config.templateFolder, self.service_name)
        self.ldif_config_fn = os.path.join(self.output_folder, 'configuration.ldif')
        self.config_json_fn = os.path.join(self.templates_folder, 'jans-saml-config.json')
        self.idp_config_fn = os.path.join(self.templates_folder, 'keycloak.conf')
        self.clients_ldif_fn = os.path.join(self.output_folder, 'clients.ldif')

        Config.jans_idp_idp_metadata_root_dir = os.path.join(self.idp_config_root_dir, 'idp/metadata')
        Config.jans_idp_idp_metadata_temp_dir = os.path.join(self.idp_config_root_dir, 'idp/temp_metadata')
        Config.jans_idp_sp_metadata_root_dir = os.path.join(self.idp_config_root_dir, 'sp/metadata')
        Config.jans_idp_sp_metadata_temp_dir = os.path.join(self.idp_config_root_dir, 'sp/temp_metadata')
        self.jans_idp_ldif_config_fn = os.path.join(self.output_folder, 'jans-idp-configuration.ldif')
        self.jans_idp_config_json_fn = os.path.join(self.templates_folder, 'jans-idp-config.json')
 

    def install(self):
        """installation steps"""
        self.create_scim_client()
        self.copy_files()
        self.install_keycloack()
        self.config_api_idp_plugin_config()

    def render_import_templates(self):
        self.logIt("Preparing base64 encodings configuration files")

        self.renderTemplateInOut(self.config_json_fn, self.templates_folder, self.output_folder, pystring=True)
        Config.templateRenderingDict['saml_dynamic_conf_base64'] = self.generate_base64_ldap_file(
                os.path.join(self.output_folder,os.path.basename(self.config_json_fn))
            )
        self.renderTemplateInOut(self.ldif_config_fn, self.templates_folder, self.output_folder)

        self.renderTemplateInOut(self.jans_idp_config_json_fn, self.templates_folder, self.output_folder, pystring=True)
        Config.templateRenderingDict['jans_idp_dynamic_conf_base64'] = self.generate_base64_ldap_file(
                os.path.join(self.output_folder,os.path.basename(self.jans_idp_config_json_fn))
            )
        self.renderTemplateInOut(self.jans_idp_ldif_config_fn, self.templates_folder, self.output_folder)

        self.dbUtils.import_ldif([self.ldif_config_fn, self.jans_idp_ldif_config_fn])


    def create_folders(self):
        for saml_dir in (self.idp_root_dir, self.idp_config_root_dir, self.idp_config_temp_meta_dir, self.idp_config_meta_dir,
                        self.idp_config_data_dir, self.idp_config_log_dir, self.idp_config_providers_dir,
                        Config.jans_idp_idp_metadata_root_dir, Config.jans_idp_sp_metadata_root_dir, Config.jans_idp_sp_metadata_temp_dir,
                ):
            self.createDirs(saml_dir)

        self.chown(self.idp_root_dir, Config.jetty_user, Config.jetty_group, recursive=True)
        self.run([paths.cmd_chmod, '0760', saml_dir])

    def create_scim_client(self):
        result = self.check_clients([('saml_scim_client_id', '2100.')])
        if result.get('2100.') == -1:

            scopes = ['inum=F0C4,ou=scopes,o=jans']
            users_write_search_result = self.dbUtils.search('ou=scopes,o=jans', search_filter='(jansId=https://jans.io/scim/users.write)')
            if users_write_search_result:
                scopes.append(users_write_search_result['dn'])
            users_read_search_result = self.dbUtils.search('ou=scopes,o=jans', search_filter='(jansId=https://jans.io/scim/users.read)')
            if users_read_search_result:
                scopes.append(users_read_search_result['dn'])

            create_client_ldif(
                ldif_fn=self.clients_ldif_fn,
                client_id=Config.saml_scim_client_id,
                encoded_pw=Config.saml_scim_client_encoded_pw,
                scopes=scopes,
                redirect_uri=['https://{}/admin-ui'.format(Config.hostname), 'http://localhost:4100'],
                display_name="Jans SCIM Client for SAML",
                grant_types=['authorization_code', 'client_credentials', 'password', 'refresh_token'],
                authorization_methods=['client_secret_basic', 'client_secret_post']
                )

            self.dbUtils.import_ldif([self.clients_ldif_fn])


    def copy_files(self):
        self.copyFile(self.source_files[0][0], self.idp_config_providers_dir)
        self.copyFile(self.source_files[1][0], self.idp_config_providers_dir)
        base.unpack_zip(self.source_files[2][0], self.idp_config_providers_dir)


    def install_keycloack(self):
        self.logIt("Installing KC", pbar=self.service_name)
        base.unpack_zip(self.source_files[3][0], self.idp_config_data_dir, with_par_dir=False)
        self.copyFile(self.source_files[4][0], self.idp_config_providers_dir)
        base.unpack_zip(self.source_files[5][0], self.idp_config_providers_dir)
        self.update_rendering_dict()
        self.renderTemplateInOut(self.idp_config_fn, self.templates_folder, os.path.join(self.idp_config_data_dir, 'conf'))
        self.chown(self.idp_config_data_dir, Config.jetty_user, Config.jetty_group, recursive=True)


    def config_api_idp_plugin_config(self):

        # Render templates
        self.update_rendering_dict()
        jans_api_tmp_dir = os.path.join(self.templates_folder, 'kc_jans_api')
        jans_api_output_dir = os.path.join(self.output_folder, 'kc_jans_api')

        jans_api_openid_client_fn = 'jans.api-openid-client.json'
        jans_api_realm_fn = 'jans.api-realm.json'
        jans_api_user_fn = 'jans.api-user.json'
        
        self.idp_config_fn = os.path.join(self.templates_folder, 'keycloak.conf')
        self.config_json_fn = os.path.join(self.templates_folder, 'jans-saml-config.json')

        for tmp_fn in (jans_api_openid_client_fn, jans_api_realm_fn, jans_api_user_fn):
            self.renderTemplateInOut(os.path.join(jans_api_tmp_dir, tmp_fn), jans_api_tmp_dir, jans_api_output_dir, pystring=True)

        self.logIt("Starting KC for config api idp plugin configurations")
        self.start()
        #wait a while for KC to start
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        for i in range(15):
            self.logIt("Wait 2 seconds to KC started")
            time.sleep(2)
            try:
                self.logIt("Connecting KC")
                s.connect((Config.idp_config_hostname, int(Config.idp_config_http_port)))
                self.logIt("Successfully connected to KC")
                break
            except Exception:
                self.logIt("KC not ready")
        else:
            self.logIt("KC did not start in 30 seconds. Giving up configuration", errorLog=True)

        kcadm_cmd = '/opt/keycloak/bin/kcadm.sh'
        kcm_server_url = f'http://{Config.idp_config_hostname}:{Config.idp_config_http_port}/'

        with tempfile.TemporaryDirectory() as tmp_dir:
            kc_tmp_config = os.path.join(tmp_dir, 'kcadm-jans.config')
            self.run([kcadm_cmd, 'config', 'credentials', '--server', kcm_server_url, '--realm', 'master', '--user', 'admin', '--password', 'admin', '--config', kc_tmp_config])

            self.run([kcadm_cmd, 'config', 'credentials', '--server', kcm_server_url, '--realm', 'master', '--user', 'admin', '--password', Config.admin_password, '--config', kc_tmp_config])

            # Change default password
            self.run([kcadm_cmd, 'set-password', '-r', 'master', '--username', 'admin', '--new-password',  Config.admin_password, '--config', kc_tmp_config])

            # create realm
            self.run([kcadm_cmd, 'create', 'realms', '-f', os.path.join(jans_api_output_dir, jans_api_realm_fn),'--config', kc_tmp_config])


            # create client
            self.run([kcadm_cmd, 'create', 'clients', '-r', Config.jans_idp_realm, '-f', os.path.join(jans_api_output_dir, jans_api_openid_client_fn),'--config', kc_tmp_config])

            # create user and change password
            self.run([kcadm_cmd, 'create', 'users', '-r', Config.jans_idp_realm, '-f', os.path.join(jans_api_output_dir, jans_api_user_fn),'--config', kc_tmp_config])
            self.run([kcadm_cmd, 'set-password', '-r', Config.jans_idp_realm, '--username', Config.jans_idp_user_name, '--new-password', Config.jans_idp_user_password, '--config', kc_tmp_config])

            # assign roles to jans-api-user
            self.run([kcadm_cmd, 'add-roles', '-r', Config.jans_idp_realm, '--uusername', Config.jans_idp_user_name, '--cclientid', 'realm-management', '--rolename', 'manage-identity-providers', '--rolename', 'view-identity-providers', '--rolename', 'view-identity-providers', '--config', kc_tmp_config])
