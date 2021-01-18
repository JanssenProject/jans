import os
import glob
import shutil

from setup_app.utils import base
from setup_app.static import AppType, InstallOption
from setup_app.config import Config
from setup_app.installers.jetty import JettyInstaller

class ScimInstaller(JettyInstaller):

    def __init__(self):
        self.service_name = 'jans-scim'
        self.needdb = True
        self.app_type = AppType.SERVICE
        self.install_type = InstallOption.OPTONAL
        self.install_var = 'installScimServer'
        self.register_progess()

        self.source_files = [
                (os.path.join(Config.distJansFolder, 'jans-scim.war'), 'https://ox.gluu.org/maven/org/gluu/scim-server/{0}/scim-server-{0}.war'.format(Config.oxVersion))
                ]

    def install(self):
        self.logIt("Copying scim.war into jetty webapps folder...")

        self.installJettyService(self.jetty_app_configuration[self.service_name], True)

        self.templates_folder = os.path.join(Config.templateFolder, self.service_name)
        self.output_folder = os.path.join(Config.outputFolder, self.service_name)

        jettyServiceWebapps = os.path.join(self.jetty_base, self.service_name,  'webapps')
        self.copyFile(self.source_files[0][0], jettyServiceWebapps)

        self.dynamic_config_fn = os.path.join(self.output_folder, 'dynamic-conf.json')
        self.static_config_fn = os.path.join(self.output_folder, 'static-conf.json')
        self.ldif_config = os.path.join(self.output_folder, 'configuration.ldif')
        self.ldif_clients = os.path.join(self.output_folder, 'clients.ldif')

        self.enable()

    def installed(self):
        return os.path.exists(os.path.join(Config.jetty_base, self.service_name, 'start.ini'))

    def generate_configuration(self):
        self.logIt("Generating {} configuration".format(self.service_name))


    def render_import_templates(self):
        
        self.renderTemplateInOut(self.dynamic_config_fn, self.templates_folder, self.output_folder)
        self.renderTemplateInOut(self.static_config_fn, self.templates_folder, self.output_folder)
        Config.templateRenderingDict['scim_dynamic_conf_base64'] = self.generate_base64_ldap_file(self.dynamic_config_fn)
        Config.templateRenderingDict['scim_static_conf_base64'] = self.generate_base64_ldap_file(self.static_config_fn)

        self.renderTemplateInOut(self.ldif_config, self.templates_folder, self.output_folder)

        self.dbUtils.import_ldif([self.ldif_config])

        self.write_webapps_xml()

    def update_backend(self):
        self.dbUtils.enable_service('jansScimEnabled')
