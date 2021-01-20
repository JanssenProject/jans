import os
import glob
import re
import configparser
import tarfile
import shutil 

from setup_app import paths
from setup_app.utils import base
from setup_app.static import AppType, InstallOption
from setup_app.config import Config
from setup_app.utils.setup_utils import SetupUtils
from setup_app.installers.base import BaseInstaller

class JansCliInstaller(BaseInstaller, SetupUtils):

    def __init__(self):
        self.service_name = 'jans-cli'
        self.needdb = False # we don't need backend connection in this class
        self.install_var = 'installJansCli'
        self.app_type = AppType.APPLICATION
        self.install_type = InstallOption.OPTONAL
        self.jans_cli_install_dir = os.path.join(Config.jansOptFolder, 'jans-cli')
        self.config_ini_fn = os.path.join(Config.jansOptFolder, 'jans-cli/config.ini')

        if not base.snap:
            self.register_progess()

        self.source_files = [
                (os.path.join(Config.distJansFolder, 'jans-cli.tgz'), 'https://api.github.com/repos/JanssenProject/jans-cli/tarball/main'.format(Config.oxVersion)),
                ]

    def install(self):

        self.logIt("Installing Jans Cli", pbar=self.service_name)

        #extract jans-cli tgz archieve
        cli_tar = tarfile.open(self.source_files[0][0])
        par_dir = cli_tar.firstmember.name
        cli_tar.extractall(Config.jansOptFolder)
        shutil.move(os.path.join(Config.jansOptFolder, par_dir), self.jans_cli_install_dir)
        cli_tar.close()

        self.run([paths.cmd_chmod, '+x', os.path.join(self.jans_cli_install_dir, 'config-cli.py')])

    def configure(self, options={}):
        config = configparser.ConfigParser()
        if os.path.exists(self.config_ini_fn):
            config.read(self.config_ini_fn)
        
        if not 'DEFAULT' in config:
            config['DEFAULT'] = {}
        
        if not 'debug' in config['DEFAULT']:
            config['DEFAULT']['debug'] = 'false'
        
        if not 'jans_host' in config['DEFAULT']:
            config['DEFAULT']['jans_host'] = Config.hostname
        
        for key_ in options:
            config['DEFAULT'][key_] = options[key_]

        if Config.installConfigApi:
            config['DEFAULT']['jca_client_id'] = Config.jca_client_id
            config['DEFAULT']['jca_client_secret_enc'] = Config.jca_client_encoded_pw
            
        if Config.installScimServer:    
            config['DEFAULT']['scim_client_id'] = Config.scim_client_id
            config['DEFAULT']['scim_client_secret_enc'] = Config.scim_client_encoded_pw

        with open(self.config_ini_fn, 'w') as f:
            config.write(f)

        
