import os
import glob
import re
import configparser
import tarfile
import shutil
import time

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

        self.output_folder = os.path.join(Config.outputFolder, self.service_name)
        self.jans_cli_install_dir = os.path.join(Config.jansOptFolder, 'jans-cli')
        self.config_ini_fn = config_dir.joinpath('jans-cli.ini')
        self.ldif_client = os.path.join(self.output_folder, 'client.ldif')
        self.templates_folder = os.path.join(Config.templateFolder, self.service_name)

        if not base.snap:
            self.register_progess()

        self.source_files = [
                (os.path.join(Config.distJansFolder, 'jans.zip'), 'https://github.com/JanssenProject/jans/archive/refs/heads/{}.zip'.format(Config.githubBranchName)),
                (os.path.join(Config.distJansFolder, 'jca-swagger-client.zip'), 'https://ox.gluu.org/icrby8xcvbcv/cli-swagger/jca_swagger_client.zip'),
                (os.path.join(Config.distJansFolder, 'scim-swagger-client.zip'), 'https://ox.gluu.org/icrby8xcvbcv/cli-swagger/scim_swagger_client.zip'),
                (os.path.join(Config.distAppFolder, 'pyjwt.zip'), 'https://github.com/jpadilla/pyjwt/archive/refs/tags/2.3.0.zip'),
                ]

    def install(self):

        self.logIt("Installing Jans Cli", pbar=self.service_name)

        # backup if exists
        if os.path.exists(self.jans_cli_install_dir):
            self.run(['mv', '-f', self.jans_cli_install_dir, self.jans_cli_install_dir+'_backup-{}'.format(time.ctime())])

        #extract jans-cli tgz archieve
        base.extract_from_zip(self.source_files[0][0], 'jans-cli/cli', self.jans_cli_install_dir)

        self.run([paths.cmd_ln, '-s', os.path.join(self.jans_cli_install_dir, 'config_cli.py'), os.path.join(self.jans_cli_install_dir, 'config-cli.py')])
        self.run([paths.cmd_ln, '-s', os.path.join(self.jans_cli_install_dir, 'config_cli.py'), os.path.join(self.jans_cli_install_dir, 'scim-cli.py')])
        self.run([paths.cmd_chmod, '+x', os.path.join(self.jans_cli_install_dir, 'config_cli.py')])

        base.extract_from_zip(self.source_files[1][0], 'jca', os.path.join(self.jans_cli_install_dir, 'jca'))
        base.extract_from_zip(self.source_files[2][0], 'scim', os.path.join(self.jans_cli_install_dir, 'scim'))

        #extract pyjwt from archieve
        base.extract_from_zip(self.source_files[3][0], 'jwt', os.path.join(self.jans_cli_install_dir, 'pylib/jwt'))


    def generate_configuration(self):
        self.check_clients([('role_based_client_id', '2000.')])

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
            config['DEFAULT']['jca_client_id'] = Config.role_based_client_id
            config['DEFAULT']['jca_client_secret_enc'] = Config.role_based_client_encoded_pw

        if Config.get('installScimServer'):
            config['DEFAULT']['scim_client_id'] = Config.scim_client_id
            config['DEFAULT']['scim_client_secret_enc'] = Config.scim_client_encoded_pw

        config['DEFAULT']['jca_plugins'] = ','.join(base.current_app.ConfigApiInstaller.get_plugins())

        config.write(self.config_ini_fn.open('w'))
        self.config_ini_fn.chmod(0o600)


    def render_import_templates(self):
        self.renderTemplateInOut(self.ldif_client, self.templates_folder, self.output_folder)
        self.dbUtils.import_ldif([self.ldif_client])
