import os
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
        self.config_api_root = os.path.join(Config.jansOptFolder, 'config-api')
        self.needdb = False # we don't need backend connection in this class
        self.app_type = AppType.SERVICE
        self.install_type = InstallOption.OPTONAL
        self.install_var = 'installConfigApi'
        self.register_progess()

        self.source_files = [
                (os.path.join(Config.distJansFolder, 'jans-config-api-runner.jar'), 'https://maven.jans.io/maven/io/jans/jans-config-api/{0}/jans-config-api-{0}-runner.jar'.format(Config.oxVersion))
                ]

    def install(self):
        self.logIt("Installing", pbar=self.service_name)

        self.run([
                'cp', 
                self.source_files[0][0], 
                self.config_api_root
                ])

    def installed(self):
        return os.path.exists(self.config_api_root)

        
    def create_folders(self):
        if not os.path.exists(self.config_api_root):
            self.run([paths.cmd_mkdir, '-p', self.config_api_root])
    
