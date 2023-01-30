import os
import glob
import shutil
from pathlib import Path

from setup_app import paths
from setup_app.utils import base
from setup_app.static import AppType, InstallOption
from setup_app.config import Config
from setup_app.installers.jetty import JettyInstaller

class FidoInstaller(JettyInstaller):

#    source_files = [
#                (os.path.join(Config.dist_jans_dir, 'jans-fido2.war'), os.path.join(base.current_app.app_info['JANS_MAVEN'], 'maven/io/jans/jans-fido2-server/{0}/jans-fido2-server-{0}.war').format(base.current_app.app_info['ox_version'])),
#                (os.path.join(Config.dist_app_dir, os.path.basename(base.current_app.app_info['APPLE_WEBAUTHN'])), base.current_app.app_info['APPLE_WEBAUTHN']),
#                (os.path.join(Config.dist_app_dir, 'fido2/mds/toc/toc.jwt'), 'https://mds.fidoalliance.org/'),
#                (os.path.join(Config.dist_app_dir, 'fido2/mds/cert/root-r3.crt'), 'https://secure.globalsign.com/cacert/root-r3.crt'),
#                ]

    source_files = [
                (os.path.join(Config.dist_jans_dir, 'jans-fido2.war'), os.path.join(base.current_app.app_info['BASE_SERVER'], '_out/fido2-server.war')),
                (os.path.join(Config.dist_app_dir, os.path.basename(base.current_app.app_info['APPLE_WEBAUTHN'])), base.current_app.app_info['APPLE_WEBAUTHN']),
                (os.path.join(Config.dist_app_dir, 'fido2/mds/toc/toc.jwt'), 'https://mds.fidoalliance.org/'),
                (os.path.join(Config.dist_app_dir, 'fido2/mds/cert/root-r3.crt'), 'https://secure.globalsign.com/cacert/root-r3.crt'),
                ]

    source_fips_files = [
                (os.path.join(Config.dist_jans_dir, 'jans-fido2-fips.war'), os.path.join(base.current_app.app_info['BASE_SERVER'], '_out/jans-fido2-server-fips.war')),
                ]

    def __init__(self):
        setattr(base.current_app, self.__class__.__name__, self)
        self.service_name = 'jans-fido2'
        self.needdb = True
        self.app_type = AppType.SERVICE
        self.install_type = InstallOption.OPTONAL
        self.install_var = 'installFido2'
        self.register_progess()

        self.fido2ConfigFolder = os.path.join(Config.configFolder, 'fido2')
        self.output_folder = os.path.join(Config.output_dir, 'jans-fido2')
        self.template_folder = os.path.join(Config.templateFolder, 'jans-fido2')
        self.fido2_dynamic_conf_json = os.path.join(self.output_folder, 'dynamic-conf.json')
        self.fido2_static_conf_json = os.path.join(self.output_folder, 'static-conf.json')
        self.ldif_fido2 = os.path.join(self.output_folder, 'fido2.ldif')

    def install(self):

        self.installJettyService(self.jetty_app_configuration[self.service_name], True)

        self.logIt("Copying fido.war into jetty webapps folder...")
        jettyServiceWebapps = os.path.join(self.jetty_base, self.service_name, 'webapps')

        src_file = self.source_files[0][0] if Config.profile != SetupProfiles.DISA_STIG else self.source_fips_files[0][0]
        self.copyFile(src_file, os.path.join(jettyServiceWebapps, '%s%s' % (self.service_name, Path(self.source_files[0][0]).suffix)))        

        self.enable()

    def render_import_templates(self):

        self.renderTemplateInOut(self.fido2_dynamic_conf_json, self.template_folder, self.output_folder)
        self.renderTemplateInOut(self.fido2_static_conf_json, self.template_folder, self.output_folder)

        Config.templateRenderingDict['fido2_dynamic_conf_base64'] = self.generate_base64_file(self.fido2_dynamic_conf_json, 1)
        Config.templateRenderingDict['fido2_static_conf_base64'] = self.generate_base64_file(self.fido2_static_conf_json, 1)

        self.renderTemplateInOut(self.ldif_fido2, self.template_folder, self.output_folder)

        self.write_webapps_xml()

        ldif_files = [self.ldif_fido2]
        self.dbUtils.import_ldif(ldif_files)


    def create_folders(self):
        for d in ('authenticator_cert', 'mds/cert', 'mds/toc', 'server_metadata'):
            dpath = os.path.join(self.fido2ConfigFolder, d)
            self.run([paths.cmd_mkdir, '-p', dpath])

    def copy_static(self):
        # Fido2 authenticator certs
        target_dir = os.path.join(self.fido2ConfigFolder, 'authenticator_cert')
        for f in ('yubico-u2f-ca-cert.crt', 'HyperFIDO_CA_Cert_V1.pem', 'HyperFIDO_CA_Cert_V2.pem'):
            src = os.path.join(Config.install_dir, 'static/fido2/authenticator_cert/', f)
            self.copyFile(src, target_dir)

        #copy fido2 server metadata
        src_dir = os.path.join(Config.install_dir, 'static/fido2/server_metadata')
        trgt_dir = os.path.join(self.fido2ConfigFolder, 'server_metadata')
        self.copy_tree(src_dir, trgt_dir, ignore='.dontdelete')

        # copy Apple_WebAuthn_Root_CA
        if os.path.exists(self.source_files[1][0]):
            target_dir = os.path.join(self.fido2ConfigFolder, 'apple')
            self.run([paths.cmd_mkdir, '-p', target_dir])
            self.copyFile(self.source_files[1][0], target_dir)

        # copy external files
        self.copy_tree(
                os.path.join(Config.dist_app_dir, 'fido2'),
                self.fido2ConfigFolder
            )
