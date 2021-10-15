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
from pathlib import Path


class JansCliInstaller(BaseInstaller, SetupUtils):

    def __init__(self):
        setattr(base.current_app, self.__class__.__name__, self)
        self.service_name = 'jans-cli'
        self.needdb = False # we don't need backend connection in this class
        self.install_var = 'installJansCli'
        self.app_type = AppType.APPLICATION
        self.install_type = InstallOption.OPTONAL
        home_dir = Path.home()
        config_dir = home_dir.joinpath('.config')
        config_dir.mkdir(parents=True, exist_ok=True)

        self.jans_cli_install_dir = os.path.join(Config.jansOptFolder, 'jans-cli')
        self.config_ini_fn = config_dir.joinpath('jans-cli.ini')

        if not base.snap:
            self.register_progess()

        self.source_files = [
                (os.path.join(Config.distJansFolder, 'jans-cli.tgz'), 'https://api.github.com/repos/JanssenProject/jans-cli/tarball/main'.format(Config.oxVersion)),
                (os.path.join(Config.distJansFolder, 'jca-swagger-client.tgz'), 'https://ox.gluu.org/icrby8xcvbcv/cli-swagger/jca.tgz'),
                (os.path.join(Config.distJansFolder, 'scim-swagger-client.tgz'), 'https://ox.gluu.org/icrby8xcvbcv/cli-swagger/scim.tgz'),
                ]

    def install(self):

        self.logIt("Installing Jans Cli", pbar=self.service_name)

        #extract jans-cli tgz archieve
        cli_tar = tarfile.open(self.source_files[0][0])
        par_dir = cli_tar.firstmember.name
        tmp_dir = os.path.join(Config.outputFolder, 'jans-cli-' + os.urandom(5).hex())
        cli_tar.extractall(tmp_dir)
        shutil.move(os.path.join(tmp_dir, par_dir, 'cli'), self.jans_cli_install_dir)
        cli_tar.close()
        shutil.rmtree(tmp_dir)
        self.run([paths.cmd_ln, '-s', os.path.join(self.jans_cli_install_dir, 'config_cli.py'), os.path.join(self.jans_cli_install_dir, 'config-cli.py')])
        self.run([paths.cmd_ln, '-s', os.path.join(self.jans_cli_install_dir, 'config_cli.py'), os.path.join(self.jans_cli_install_dir, 'scim-cli.py')])
        self.run([paths.cmd_chmod, '+x', os.path.join(self.jans_cli_install_dir, 'config_cli.py')])

        for i, app_mod in enumerate(('jca', 'scim')):
            swagger_cli_dir = os.path.join(self.jans_cli_install_dir, app_mod)
            self.createDirs(swagger_cli_dir)
            init_fn = os.path.join(swagger_cli_dir, '__init__.py')
            self.writeFile(init_fn, '')
            shutil.unpack_archive(self.source_files[i+1][0], swagger_cli_dir)

    def configure(self, options={}):
        config = configparser.ConfigParser()
        if self.config_ini_fn.exists():
            config.read_file(self.config_ini_fn.open())

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

        if Config.get('installScimServer'):
            config['DEFAULT']['scim_client_id'] = Config.scim_client_id
            config['DEFAULT']['scim_client_secret_enc'] = Config.scim_client_encoded_pw

        config.write(self.config_ini_fn.open('w'))

