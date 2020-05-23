import os
import glob
import random
import string

from setup_app import paths
from setup_app.config import Config
from setup_app.utils.setup_utils import SetupUtils
from setup_app.installers.base import BaseInstaller


class OxauthInstaller(SetupUtils, BaseInstaller):

    def __init__(self):
        super().__init__()
        self.service_name = 'oxauth'
        self.pbar_text = "Installing oxauth"
        self.oxauth_war = 'https://ox.gluu.org/maven/org/gluu/oxauth-server/%s/oxauth-server-%s.war' % (Config.oxVersion, Config.oxVersion)
        self.oxauth_rp_war = 'https://ox.gluu.org/maven/org/gluu/oxauth-rp/%s/oxauth-rp-%s.war' % (Config.oxVersion, Config.oxVersion)


    def install(self):
        self.logIt("Copying oxauth.war into jetty webapps folder...")

        jettyServiceName = 'oxauth'
        self.installJettyService(self.jetty_app_configuration[jettyServiceName], True)

        jettyServiceWebapps = '%s/%s/webapps' % (self.jetty_base, jettyServiceName)
        self.copyFile('%s/oxauth.war' % self.distGluuFolder, jettyServiceWebapps)


    def install_oxauth_rp(self):
        oxAuthRPWar = 'oxauth-rp.war'
        distOxAuthRpPath = '%s/%s' % (self.distGluuFolder, oxAuthRPWar)

        self.logIt("Copying oxauth-rp.war into jetty webapps folder...")

        jettyServiceName = 'oxauth-rp'
        self.installJettyService(self.jetty_app_configuration[jettyServiceName])

        jettyServiceWebapps = '%s/%s/webapps' % (self.jetty_base, jettyServiceName)
        self.copyFile('%s/oxauth-rp.war' % self.distGluuFolder, jettyServiceWebapps)

    def genRandomString(self, N):
        return ''.join(random.SystemRandom().choice(string.ascii_lowercase
                                                    + string.ascii_uppercase
                                                    + string.digits) for _ in range(N))
    def make_oxauth_salt(self):
        Config.pbar.progress("gluu", "Making oxauth salt")
        Config.pairwiseCalculationKey = self.genRandomString(random.randint(20,30))
        Config.pairwiseCalculationSalt = self.genRandomString(random.randint(20,30))

    def download_files(self):
        Config.pbar.progress('oxauth', "Downloading oxAuth war file")
        self.run([paths.cmd_wget, self.oxauth_war, '--no-verbose', '--retry-connrefused', '--tries=10', '-O', os.path.join(Config.distGluuFolder, 'oxauth.war')])
        
        if Config.installOxAuthRP:
            # oxAuth RP is not part of CE package. We need to download it if needed
            distOxAuthRpPath = os.path.join(Config.distGluuFolder, 'oxauth-rp.war')
            if not os.path.exists(distOxAuthRpPath):
                self.pbar.progress('oxauth', "Downloading oxAuth RP war file", False)
                self.run([paths.cmd_wget, self.oxauth_rp_war, '--no-verbose', '--retry-connrefused', '--tries=10', '-O', os.path.join(self.distGluuFolder,  'oxauth-rp.war')])

