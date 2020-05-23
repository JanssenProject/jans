import os
import glob

from setup_app import paths
from setup_app.config import Config
from setup_app.utils.setup_utils import SetupUtils
from setup_app.installers.base import BaseInstaller


class OxtrustInstaller(BaseInstaller, SetupUtils):

    def __init__(self):
        super().__init__()
        self.service_name = 'identity'
        self.pbar_text = "Installing Oxtrust"
        self.oxtrust_war = 'https://ox.gluu.org/maven/org/gluu/oxtrust-server/%s/oxtrust-server-%s.war' % (Config.oxVersion, Config.oxVersion)

    def install(self):
        self.logIt("Copying identity.war into jetty webapps folder...")

        jettyServiceName = 'identity'
        self.installJettyService(self.jetty_app_configuration[jettyServiceName], True)

        jettyServiceWebapps = '%s/%s/webapps' % (self.jetty_base, jettyServiceName)
        self.copyFile('%s/identity.war' % self.distGluuFolder, jettyServiceWebapps)

        # don't send header to server
        self.set_jetty_param(jettyServiceName, 'jetty.httpConfig.sendServerVersion', 'false')

    def generate_api_configuration(self):
        Config.api_rs_client_jks_pass_encoded = self.obscure(Config.api_rs_client_jks_pass)
        Config.api_rs_client_jwks = self.gen_openid_jwks_jks_keys(Config.api_rs_client_jks_fn, Config.api_rs_client_jks_pass)
        Config.templateRenderingDict['api_rs_client_base64_jwks'] = self.generate_base64_string(Config.api_rs_client_jwks, 1)

        Config.api_rp_client_jks_pass_encoded = self.obscure(Config.api_rp_client_jks_pass)
        Config.api_rp_client_jwks = self.gen_openid_jwks_jks_keys(Config.api_rp_client_jks_fn, Config.api_rp_client_jks_pass)
        Config.templateRenderingDict['api_rp_client_base64_jwks'] = self.generate_base64_string(Config.api_rp_client_jwks, 1)


    def download_files(self):
        self.pbar.progress('oxauth', "Downloading oxTrust war file", False)
        self.run(['/usr/bin/wget', self.oxtrust_war, '--no-verbose', '--retry-connrefused', '--tries=10', '-O', '%s/identity.war' % self.distGluuFolder])
