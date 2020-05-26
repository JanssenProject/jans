import os
import glob
import shutil

from setup_app.config.config import Config
from setup_app.utils.setup_utils import SetupUtils

class FidoInstaller(SetupUtils):

    def __init__(self):
        pass


    def install(self):
        self.logIt("Copying fido.war into jetty webapps folder...")

        jettyServiceName = 'fido2'
        self.installJettyService(self.jetty_app_configuration[jettyServiceName], True)

        jettyServiceWebapps = '%s/%s/webapps' % (self.jetty_base, jettyServiceName)
        self.copyFile('%s/fido2.war' % self.distGluuFolder, jettyServiceWebapps)


    def create_folders(self):
        self.run([self.cmd_mkdir, '-p', self.fido2ConfigFolder])
        self.run([self.cmd_mkdir, '-p', '%s/%s' % (self.fido2ConfigFolder, '/authenticator_cert')])
        self.run([self.cmd_mkdir, '-p', '%s/%s' % (self.fido2ConfigFolder, '/mds/cert')])
        self.run([self.cmd_mkdir, '-p', '%s/%s' % (self.fido2ConfigFolder, '/mds/toc')])
        self.run([self.cmd_mkdir, '-p', '%s/%s' % (self.fido2ConfigFolder, '/server_metadata')])

    def copy_static(self):
        # Fido2 authenticator certs
        self.copyFile("%s/static/auth/fido2//authenticator_cert/yubico-u2f-ca-cert.crt" % self.install_dir, "%s/%s" % (self.fido2ConfigFolder, '/authenticator_cert'))
        self.copyFile("%s/static/auth/fido2//authenticator_cert/HyperFIDO_CA_Cert_V1.pem" % self.install_dir, "%s/%s" % (self.fido2ConfigFolder, '/authenticator_cert'))
        self.copyFile("%s/static/auth/fido2//authenticator_cert/HyperFIDO_CA_Cert_V2.pem" % self.install_dir, "%s/%s" % (self.fido2ConfigFolder, '/authenticator_cert'))

        # Fido2 MDS TOC cert
        self.copyFile("%s/static/auth/fido2//mds_toc_cert/metadata-root-ca.cer" % self.install_dir, "%s/%s" % (self.fido2ConfigFolder, '/mds/cert'))
