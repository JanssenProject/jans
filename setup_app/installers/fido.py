import os
import glob
import shutil

from setup_app import paths
from setup_app.config import Config
from setup_app.installers.jetty import JettyInstaller

class FidoInstaller(JettyInstaller):

    def __init__(self):
        self.service_name = 'fido2'
        self.pbar_text = "Installing fido2"


    def install(self):
        self.logIt("Copying fido.war into jetty webapps folder...")

        self.installJettyService(Config.jetty_app_configuration[self.service_name], True)

        jettyServiceWebapps = os.path.join(Config.jetty_base, self.service_name, 'webapps')
        self.copyFile(os.path.join(Config.distGluuFolder, 'fido2.war'), jettyServiceWebapps)


    def create_folders(self):
        for d in ('authenticator_cert', 'mds/cert', 'mds/toc', 'server_metadata'):
            dpath = os.path.join(Config.fido2ConfigFolder, d)
            self.run([paths.cmd_mkdir, '-p', dpath])

    def copy_static(self):
        # Fido2 authenticator certs
        target_dir = os.path.join(Config.fido2ConfigFolder, 'authenticator_cert')
        for f in ('yubico-u2f-ca-cert.crt', 'HyperFIDO_CA_Cert_V1.pem', 'HyperFIDO_CA_Cert_V2.pem'):
            src = os.path.join(Config.install_dir, 'static/auth/fido2/authenticator_cert/', f)
            self.copyFile(src, target_dir)

        # Fido2 MDS TOC cert
        self.copyFile(
            os.path.join(Config.install_dir, 'static/auth/fido2/mds_toc_cert/metadata-root-ca.cer'),
            os.path.join(Config.fido2ConfigFolder, 'mds/cert')
            )
