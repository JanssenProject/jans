import os
import glob
import shutil

from setup_app import paths
from setup_app.utils import base
from setup_app.utils.package_utils import packageUtils
from setup_app.static import AppType, InstallOption
from setup_app.config import Config
from setup_app.installers.jetty import JettyInstaller


class JansLinkInstaller(JettyInstaller):

    source_files = [
            (os.path.join(Config.dist_jans_dir, 'jans-link.war'), os.path.join(base.current_app.app_info['JANS_MAVEN'], 'maven/io/jans/jans-link-server/{0}/jans-link-server-{0}.war').format(base.current_app.app_info['jans_version']))
            ]

    def __init__(self):
        setattr(base.current_app, self.__class__.__name__, self)
        self.service_name = 'jans-link'
        self.needdb = True
        self.app_type = AppType.SERVICE
        self.install_type = InstallOption.OPTONAL
        self.install_var = 'install_jans_link'
        self.register_progess()

        self.output_folder = os.path.join(Config.output_dir, self.service_name)
        self.templates_folder = os.path.join(Config.templateFolder, self.service_name)
        self.ldif_config_fn = os.path.join(self.output_folder, 'configuration.ldif')
        self.config_json_fn = os.path.join(self.templates_folder, 'jans-link-config.json')
        self.static_config_json_fn = os.path.join(self.templates_folder, 'jans-link-static-config.json')
        self.vendor_dir = '/var/jans/'
        self.snapshots_dir = os.path.join(self.vendor_dir, 'link-snapshots')

    def install(self):
        self.install_jettyService(self.jetty_app_configuration[self.service_name], True)
        self.copyFile(self.source_files[0][0], self.jetty_service_webapps)

        if Config.installed_instance and Config.install_config_api:
            base.current_app.ConfigApiInstaller.install_plugin('jans-link-plugin')

        self.enable()


    def render_import_templates(self):
        self.logIt("Preparing base64 encodings configuration files")
        self.renderTemplateInOut(self.config_json_fn, self.templates_folder, self.output_folder)
        Config.templateRenderingDict['jans_link_config_base64'] = self.generate_base64_ldap_file(
                os.path.join(
                    self.output_folder,
                    os.path.basename(self.config_json_fn)
                )
            )
        Config.templateRenderingDict['jans_link_static_conf_base64'] = self.generate_base64_ldap_file(self.static_config_json_fn)

        self.renderTemplateInOut(self.ldif_config_fn, self.templates_folder, self.output_folder)

        self.dbUtils.import_ldif([self.ldif_config_fn])

    def create_folders(self):
        self.createDirs(self.snapshots_dir)
        self.chown(self.vendor_dir, Config.jetty_user, Config.jetty_group, recursive=True)
