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
        self.needdb = True

        self.fido2ConfigFolder = os.path.join(Config.configFolder, 'fido2')
        self.output_folder = os.path.join(Config.outputFolder, 'fido2')
        self.template_folder = os.path.join(Config.templateFolder, 'fido2')
        self.fido2_dynamic_conf_json = os.path.join(self.output_folder, 'dynamic-conf.json')
        self.fido2_static_conf_json = os.path.join(self.output_folder, 'static-conf.json')
        self.ldif_fido2 = os.path.join(self.output_folder, 'fido2.ldif')

    def install(self):
        self.logIt("Copying fido.war into jetty webapps folder...")

        # make variables of this class accesible from Config
        Config.templateRenderingDict.update(self.__dict__)
        self.renderTemplateInOut(self.fido2_dynamic_conf_json, self.template_folder, self.output_folder)
        self.renderTemplateInOut(self.fido2_static_conf_json, self.template_folder, self.output_folder)

        Config.templateRenderingDict['fido2_dynamic_conf_base64'] = self.generate_base64_file(self.fido2_dynamic_conf_json, 1)
        Config.templateRenderingDict['fido2_static_conf_base64'] = self.generate_base64_file(self.fido2_static_conf_json, 1)
        
        self.renderTemplateInOut(self.ldif_fido2, self.template_folder, self.output_folder)

        ldif_files = (self.ldif_fido2,)

        if Config.mappingLocations['default'] == 'ldap':
            self.dbUtils.import_ldif(ldif_files)
        else:
            #TODO: implement for couchbase ???
            self.import_ldif_couchebase(ldif_files)

        self.installJettyService(Config.jetty_app_configuration[self.service_name], True)

        jettyServiceWebapps = os.path.join(Config.jetty_base, self.service_name, 'webapps')
        self.copyFile(os.path.join(Config.distGluuFolder, 'fido2.war'), jettyServiceWebapps)

        self.enable()

    def create_folders(self):
        for d in ('authenticator_cert', 'mds/cert', 'mds/toc', 'server_metadata'):
            dpath = os.path.join(self.fido2ConfigFolder, d)
            self.run([paths.cmd_mkdir, '-p', dpath])

    def copy_static(self):
        # Fido2 authenticator certs
        target_dir = os.path.join(self.fido2ConfigFolder, 'authenticator_cert')
        for f in ('yubico-u2f-ca-cert.crt', 'HyperFIDO_CA_Cert_V1.pem', 'HyperFIDO_CA_Cert_V2.pem'):
            src = os.path.join(Config.install_dir, 'static/auth/fido2/authenticator_cert/', f)
            self.copyFile(src, target_dir)

        # Fido2 MDS TOC cert
        self.copyFile(
            os.path.join(Config.install_dir, 'static/auth/fido2/mds_toc_cert/metadata-root-ca.cer'),
            os.path.join(self.fido2ConfigFolder, 'mds/cert')
            )
