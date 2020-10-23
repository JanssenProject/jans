import os
import time
import glob
import ruamel.yaml

from setup_app import paths
from setup_app.static import AppType, InstallOption
from setup_app.utils import base
from setup_app.config import Config
from setup_app.utils.setup_utils import SetupUtils
from setup_app.installers.base import BaseInstaller

class ConfigApiInstaller(SetupUtils, BaseInstaller):

    def __init__(self):
        self.service_name = 'jans-config-api'
        self.needdb = False # we don't need backend connection in this class
        self.app_type = AppType.SERVICE
        self.install_type = InstallOption.OPTONAL
        self.install_var = 'installConfigApi'
        self.register_progess()

        self.root_dir = os.path.join(Config.jansOptFolder, 'config-api')
        self.conf_dir = os.path.join(self.root_dir, 'config')
        self.log_dir = os.path.join(self.root_dir, 'logs')
        self.temp_dir = os.path.join(Config.templateFolder, self.service_name)

        self.source_files = [
                (os.path.join(Config.distJansFolder, 'jans-config-api-runner.jar'), 'https://maven.jans.io/maven/io/jans/jans-config-api/{0}/jans-config-api-{0}-runner.jar'.format(Config.oxVersion))
                ]

    def install(self):
        self.logIt("Installing", pbar=self.service_name)

        self.copyFile(self.source_files[0][0], self.root_dir)

        self.copyFile(
                os.path.join(Config.templateFolder, self.service_name, 'application.properties'),
                self.conf_dir
                )

        self.copyFile(
                os.path.join(Config.staticFolder, 'system/initd', self.service_name),
                os.path.join(Config.distFolder, 'scripts')
                )

        self.run([paths.cmd_chmod, '+x', os.path.join(Config.distFolder, 'scripts', self.service_name)])



    def installed(self):
        return os.path.exists(self.config_api_root)

        
    def create_folders(self):
        for d in (self.root_dir, self.conf_dir, self.log_dir):
            if not os.path.exists(d):
                self.createDirs(d)

        self.run([paths.cmd_chown, '-R', 'jetty:jetty', self.root_dir])
                
