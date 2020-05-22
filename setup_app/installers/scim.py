import os
import glob
import shutil

from setup_app.config.config import Config
from setup_app.utils.setup_utils import SetupUtils

class ScimInstaller(SetupUtils):

    def __init__(self):
        pass


    def install(self):
        self.logIt("Copying scim.war into jetty webapps folder...")

        jettyServiceName = 'scim'
        self.installJettyService(self.jetty_app_configuration[jettyServiceName], True)

        jettyServiceWebapps = '%s/%s/webapps' % (self.jetty_base, jettyServiceName)
        self.copyFile('%s/scim.war' % self.distGluuFolder, jettyServiceWebapps)

        # don't send header to server
        self.set_jetty_param(jettyServiceName, 'jetty.httpConfig.sendServerVersion', 'false')


    def generate_scim_configuration(self):
        self.scim_rs_client_jks_pass = self.getPW()

        self.scim_rs_client_jks_pass_encoded = self.obscure(self.scim_rs_client_jks_pass)

        self.scim_rs_client_jwks = self.gen_openid_jwks_jks_keys(self.scim_rs_client_jks_fn, self.scim_rs_client_jks_pass)
        self.templateRenderingDict['scim_rs_client_base64_jwks'] = self.generate_base64_string(self.scim_rs_client_jwks, 1)

        self.scim_rp_client_jwks = self.gen_openid_jwks_jks_keys(self.scim_rp_client_jks_fn, self.scim_rp_client_jks_pass)
        self.templateRenderingDict['scim_rp_client_base64_jwks'] = self.generate_base64_string(self.scim_rp_client_jwks, 1)

