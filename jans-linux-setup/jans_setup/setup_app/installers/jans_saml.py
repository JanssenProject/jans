import os
import glob
import shutil

from setup_app import paths
from setup_app.utils import base
from setup_app.utils.package_utils import packageUtils
from setup_app.static import AppType, InstallOption
from setup_app.config import Config
from setup_app.installers.jetty import JettyInstaller
from setup_app.utils.ldif_utils import create_client_ldif


class JansSamlInstaller(JettyInstaller):

    install_var = 'install_jans_saml'
    setattr(Config, install_var + '_pre_released', True)

    source_files = [
        (os.path.join(Config.dist_jans_dir, 'jans-keycloak-storage-api.jar'), os.path.join(base.current_app.app_info['JANS_MAVEN'], 'maven/io/jans/jans-keycloak-storage-api/{0}/jans-keycloak-storage-api-{0}.jar').format(base.current_app.app_info['jans_version'])),
        (os.path.join(Config.dist_jans_dir, 'jans-scim-model.jar'), os.path.join(base.current_app.app_info['JANS_MAVEN'], 'maven/io/jans/jans-scim-model/{0}/jans-scim-model-{0}.jar').format(base.current_app.app_info['jans_version'])),
        (os.path.join(Config.dist_jans_dir, 'jans-keycloak-storage-api.zip'), os.path.join(base.current_app.app_info['JANS_MAVEN'], 'maven/io/jans/jans-keycloak-storage-api/{0}/jans-keycloak-storage-api-{0}.zip').format(base.current_app.app_info['jans_version'])),
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
        self.idp_config_http_port = '8083'
        self.output_folder = os.path.join(Config.output_dir, self.service_name)
        self.templates_folder = os.path.join(Config.templateFolder, self.service_name)
        self.ldif_config_fn = os.path.join(self.output_folder, 'configuration.ldif')
        self.config_json_fn = os.path.join(self.templates_folder, 'jans-saml-config.json')
        self.idp_config_providers_fn = os.path.join(self.templates_folder, 'jans-keycloak-storage-api.properties')
        self.clients_ldif_fn = os.path.join(self.output_folder, 'clients.ldif')

        # change this when we figure out this
        Config.keycloack_hostname = 'localhost'

        if not base.argsp.shell:
            self.extract_files()

    def install(self):
        """installation steps"""
        self.create_scim_client()
        self.copy_files()
        self.install_keycloack()

    def extract_files(self):
        base.extract_file(base.current_app.jans_zip, 'jans-keycloak/storage-api/src/main/resources/jans-keycloak-storage-api.properties', self.templates_folder)


    def render_import_templates(self):
        self.logIt("Preparing base64 encodings configuration files")
        self.renderTemplateInOut(self.config_json_fn, self.templates_folder, self.output_folder, pystring=True)
        Config.templateRenderingDict['saml_dynamic_conf_base64'] = self.generate_base64_ldap_file(
                os.path.join(
                    self.output_folder,
                    os.path.basename(self.config_json_fn)
                )
            )

        self.renderTemplateInOut(self.ldif_config_fn, self.templates_folder, self.output_folder)
        self.dbUtils.import_ldif([self.ldif_config_fn])


    def create_folders(self):
        for saml_dir in (self.idp_root_dir, self.idp_config_root_dir, self.idp_config_temp_meta_dir, self.idp_config_meta_dir,
                        self.idp_config_data_dir, self.idp_config_log_dir, self.idp_config_providers_dir,
                ):
            self.createDirs(saml_dir)

        self.chown(self.idp_root_dir, Config.jetty_user, Config.jetty_group, recursive=True)

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

        self.renderTemplateInOut(self.idp_config_providers_fn, self.templates_folder, self.idp_config_providers_dir, pystring=True)

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
        self.render_unit_file()
        self.enable()
