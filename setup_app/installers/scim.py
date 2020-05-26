import os
import glob
import shutil

from setup_app.config import Config
from setup_app.installers.jetty import JettyInstaller

class ScimInstaller(JettyInstaller):

    def __init__(self):
        self.service_name = 'scim'
        self.pbar_text = "Installing Scim"


    def install(self):
        self.logIt("Copying scim.war into jetty webapps folder...")

        self.installJettyService(Config.jetty_app_configuration[self.service_name], True)

        jettyServiceWebapps = os.path.join(Config.jetty_base, self.service_name,  'webapps')
        src_war = os.path.join(Config.distGluuFolder, 'scim.war')
        self.copyFile(src_war, jettyServiceWebapps)

        self.enable()

    def generate_configuration(self):
        Config.scim_rs_client_jks_pass = self.getPW()

        Config.scim_rs_client_jks_pass_encoded = self.obscure(Config.scim_rs_client_jks_pass)

        Config.scim_rs_client_jwks = self.gen_openid_jwks_jks_keys(Config.scim_rs_client_jks_fn, Config.scim_rs_client_jks_pass)
        Config.templateRenderingDict['scim_rs_client_base64_jwks'] = self.generate_base64_string(Config.scim_rs_client_jwks, 1)

        Config.scim_rp_client_jwks = self.gen_openid_jwks_jks_keys(Config.scim_rp_client_jks_fn, Config.scim_rp_client_jks_pass)
        Config.templateRenderingDict['scim_rp_client_base64_jwks'] = self.generate_base64_string(Config.scim_rp_client_jwks, 1)

