import os
import glob
import shutil

from setup_app.config import Config
from setup_app.utils.setup_utils import SetupUtils
from setup_app.installers.base import BaseInstaller

class ScimInstaller(BaseInstaller, SetupUtils):

    def __init__(self):
        super().__init__()
        self.service_name = 'scim'
        self.pbar_text = "Installing Scim"


    def install(self):
        self.logIt("Copying scim.war into jetty webapps folder...")

        jettyServiceName = 'scim'
        self.installJettyService(self.jetty_app_configuration[jettyServiceName], True)

        jettyServiceWebapps = '%s/%s/webapps' % (self.jetty_base, jettyServiceName)
        self.copyFile('%s/scim.war' % self.distGluuFolder, jettyServiceWebapps)

        # don't send header to server
        self.set_jetty_param(jettyServiceName, 'jetty.httpConfig.sendServerVersion', 'false')


    def generate_configuration(self):
        Config.scim_rs_client_jks_pass = self.getPW()

        Config.scim_rs_client_jks_pass_encoded = self.obscure(Config.scim_rs_client_jks_pass)

        Config.scim_rs_client_jwks = self.gen_openid_jwks_jks_keys(Config.scim_rs_client_jks_fn, Config.scim_rs_client_jks_pass)
        Config.templateRenderingDict['scim_rs_client_base64_jwks'] = self.generate_base64_string(Config.scim_rs_client_jwks, 1)

        Config.scim_rp_client_jwks = self.gen_openid_jwks_jks_keys(Config.scim_rp_client_jks_fn, Config.scim_rp_client_jks_pass)
        Config.templateRenderingDict['scim_rp_client_base64_jwks'] = self.generate_base64_string(Config.scim_rp_client_jwks, 1)

