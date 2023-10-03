import os
import glob
import shutil

from setup_app import paths
from setup_app.utils import base
from setup_app.utils.package_utils import packageUtils
from setup_app.static import AppType, InstallOption
from setup_app.config import Config
from setup_app.installers.jetty import JettyInstaller


class JansSamlInstaller(JettyInstaller):

    source_files = [
            ]

    def __init__(self):
        setattr(base.current_app, self.__class__.__name__, self)
        self.service_name = 'jans-saml'
        self.needdb = True
        self.app_type = AppType.SERVICE
        self.install_type = InstallOption.OPTONAL
        self.install_var = 'install_jans_saml'
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
        
        self.output_folder = os.path.join(Config.output_dir, self.service_name)
        self.templates_folder = os.path.join(Config.templateFolder, self.service_name)
        self.ldif_config_fn = os.path.join(self.output_folder, 'configuration.ldif')
        self.config_json_fn = os.path.join(self.templates_folder, 'jans-saml-config.json')

    def install(self):
        """installation steps"""

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
        for saml_dir in (self.idp_root_dir, self.idp_config_root_dir, self.idp_config_temp_meta_dir, self.idp_config_meta_dir
                ):
            self.createDirs(saml_dir)

        self.chown(self.idp_root_dir, Config.jetty_user, Config.jetty_group, recursive=True)
