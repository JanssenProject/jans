import os
import glob
import shutil

from setup_app.utils import base
from setup_app.static import AppType, InstallOption
from setup_app.config import Config
from setup_app.installers.jetty import JettyInstaller

class ScimInstaller(JettyInstaller):

    def __init__(self):
        self.service_name = 'scim'
        self.needdb = True
        self.app_type = AppType.SERVICE
        self.install_type = InstallOption.OPTONAL
        self.install_var = 'installScimServer'
        self.register_progess()

        self.source_files = [
                ('scim.war', 'https://ox.gluu.org/maven/org/gluu/scim-server/{0}/scim-server-{0}.war'.format(Config.oxVersion))
                ]

    def install(self):
        self.logIt("Copying scim.war into jetty webapps folder...")

        self.installJettyService(self.jetty_app_configuration[self.service_name], True)

        self.templates_folder = os.path.join(Config.templateFolder, self.service_name)
        self.output_folder = os.path.join(Config.outputFolder, self.service_name)

        jettyServiceWebapps = os.path.join(self.jetty_base, self.service_name,  'webapps')
        src_war = os.path.join(Config.distGluuFolder, 'scim.war')
        self.copyFile(src_war, jettyServiceWebapps)

        self.oxtrust_config_fn = os.path.join(self.output_folder, 'oxtrust_config.json')
        self.ldif_config = os.path.join(self.output_folder, 'configuration.ldif')
        self.ldif_clients = os.path.join(self.output_folder, 'clients.ldif')

        self.scim_rs_client_jks_fn = os.path.join(Config.certFolder, 'scim-rs.jks')
        self.scim_rp_client_jks_fn = os.path.join(Config.certFolder, 'scim-rp.jks')

        self.enable()

    def installed(self):
        return os.path.exists(os.path.join(Config.jetty_base, self.service_name, 'start.ini'))

    def generate_configuration(self):
        self.logIt("Generating {} configuration".format(self.service_name))
        client_var_id_list = (
                    ('scim_rs_client_id', '1201.'),
                    ('scim_rp_client_id', '1202.'),
                    )
        self.check_clients(client_var_id_list)
        self.check_clients([('scim_resource_oxid', '1203.')], resource=True)

        if not Config.get('scim_rs_client_jks_pass'):
            Config.scim_rs_client_jks_pass = self.getPW()
        
        Config.scim_rs_client_jks_pass_encoded = self.obscure(Config.scim_rs_client_jks_pass)

        if not Config.get('scim_rp_client_jks_pass'):
            Config.scim_rp_client_jks_pass = 'secret'

        Config.scimTestMode = Config.get('scimTestMode', 'false')
        Config.enable_scim_access_policy = 'true' if Config.installPassport else 'false'

        #backup current jks files if exists
        for jks_fn in (self.scim_rs_client_jks_fn, self.scim_rp_client_jks_fn):
            if os.path.exists(jks_fn):
                self.backupFile(jks_fn, move=True)

        Config.scim_rs_client_jwks = self.gen_openid_jwks_jks_keys(self.scim_rs_client_jks_fn, Config.scim_rs_client_jks_pass)
        Config.templateRenderingDict['scim_rs_client_base64_jwks'] = self.generate_base64_string(Config.scim_rs_client_jwks, 1)

        Config.scim_rp_client_jwks = self.gen_openid_jwks_jks_keys(self.scim_rp_client_jks_fn, Config.scim_rp_client_jks_pass)
        Config.templateRenderingDict['scim_rp_client_base64_jwks'] = self.generate_base64_string(Config.scim_rp_client_jwks, 1)

    def render_import_templates(self):
        self.renderTemplateInOut(self.ldif_config, self.templates_folder, self.output_folder)
        self.renderTemplateInOut(self.ldif_clients, self.templates_folder, self.output_folder)
        self.renderTemplateInOut(self.oxtrust_config_fn, self.templates_folder, self.output_folder)

        self.dbUtils.import_ldif([self.ldif_config, self.ldif_clients])

    def update_backend(self):
        oxtrust_config = base.readJsonFile(self.oxtrust_config_fn)
        self.dbUtils.set_oxTrustConfApplication(oxtrust_config)

        self.dbUtils.add_client2script('2DAF-F9A5', Config.scim_rp_client_id)
        self.dbUtils.add_client2script('2DAF-F995', Config.scim_rp_client_id)
        self.enable_service('gluuScimEnabled')
                
    
