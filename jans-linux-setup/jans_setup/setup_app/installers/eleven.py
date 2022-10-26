import os
import glob
import shutil

from setup_app import paths
from setup_app.utils import base
from setup_app.utils.package_utils import packageUtils
from setup_app.static import AppType, InstallOption
from setup_app.config import Config
from setup_app.installers.jetty import JettyInstaller


class ElevenInstaller(JettyInstaller):

    source_files = [
            (os.path.join(Config.dist_jans_dir, 'jans-eleven.war'), os.path.join(base.current_app.app_info['JANS_MAVEN'], 'maven/io/jans/jans-eleven-server/{0}/jans-eleven-server-{0}.war').format(base.current_app.app_info['ox_version']))
            ]

    def __init__(self):
        setattr(base.current_app, self.__class__.__name__, self)
        self.service_name = 'jans-eleven'
        self.needdb = True
        self.app_type = AppType.SERVICE
        self.install_type = InstallOption.OPTONAL
        self.install_var = 'installEleven'
        self.register_progess()


        self.output_folder = os.path.join(Config.output_dir, 'jans-eleven')
        self.template_folder = os.path.join(Config.templateFolder, 'jans-eleven')
        self.eleven_conf_json = os.path.join(self.output_folder, 'jans-eleven.json')
    

    def install(self):
        # install softhsm
        if base.clone_type == 'rpm':
            self.softhsm_path = '/lib64/pkcs11/libsofthsm2.so'
            if not os.path.exists(self.softhsm_path):
                self.logIt("Installing softhsm", pbar=self.service_name)
                if base.os_version == '7':
                    packageUtils.installPackage('softhsm', remote=True)
                elif base.os_version == '8':
                    packageUtils.installPackage('http://repo.okay.com.mx/centos/8/x86_64/release/softhsm-2.4.0-2.el8.x86_64.rpm', remote=True)
        elif base.clone_type == 'deb':
            self.logIt("Installing softhsm", pbar=self.service_name)
            self.softhsm_path = '/usr/lib/softhsm/libsofthsm2.so'
            if not os.path.exists(self.softhsm_path):
                packageUtils.installPackage('softhsm2', remote=True)

        self.installJettyService(self.jetty_app_configuration[self.service_name], True)

        self.logIt("Copying {} into jetty webapps folder...".format(self.source_files[0][0]))
        jettyServiceWebapps = os.path.join(self.jetty_base, self.service_name, 'webapps')
        self.copyFile(self.source_files[0][0], jettyServiceWebapps)
        self.enable()

    def render_import_templates(self):
        self.renderTemplateInOut(self.eleven_conf_json, self.template_folder, self.output_folder)
        self.copyFile(self.eleven_conf_json, Config.configFolder)

    def create_folders(self):
        pass
