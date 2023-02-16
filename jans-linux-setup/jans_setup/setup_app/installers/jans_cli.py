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

    source_files = [
                (os.path.join(Config.dist_app_dir, 'pyjwt.zip'), base.current_app.app_info['PYJWT']),
                (os.path.join(Config.dist_app_dir, 'prompt_toolkit.zip'), base.current_app.app_info['PROMPT_TOOLKIT']),
                (os.path.join(Config.dist_app_dir, 'wcwidth.zip'), base.current_app.app_info['WCWIDTH']),
                (os.path.join(Config.dist_app_dir, 'pygments.zip'), base.current_app.app_info['PYGMENTS']),
                ]

    def __init__(self):
        setattr(base.current_app, self.__class__.__name__, self)
        self.service_name = 'jans-cli'
        self.needdb = False # we don't need backend connection in this class
        self.install_var = 'install_jans_cli'
        self.app_type = AppType.APPLICATION
        self.install_type = InstallOption.OPTONAL
        home_dir = Path.home()
        config_dir = home_dir.joinpath('.config')
        config_dir.mkdir(parents=True, exist_ok=True)

        self.output_folder = os.path.join(Config.output_dir, self.service_name)
        self.jans_cli_install_dir = os.path.join(Config.jansOptFolder, 'jans-cli')
        self.config_ini_fn = config_dir.joinpath('jans-cli.ini')
        self.ldif_client = os.path.join(self.output_folder, 'client.ldif')
        self.templates_folder = os.path.join(Config.templateFolder, self.service_name)
        self.pylib_dir = os.path.join(self.jans_cli_install_dir, 'cli', 'pylib')

        if not base.snap:
            self.register_progess()


    def install(self):

        self.logIt("Installing Jans Cli", pbar=self.service_name)

        # backup if exists
        if os.path.exists(self.jans_cli_install_dir):
            self.run(['mv', '-f', self.jans_cli_install_dir, self.jans_cli_install_dir+'_backup-{}'.format(time.ctime())])

        self.createDirs(self.pylib_dir)
        ops_dir = os.path.join(self.jans_cli_install_dir, 'cli', 'ops')
        self.createDirs(ops_dir)
        for opsd in ('jca', 'scim', 'auth'):
            self.createDirs(os.path.join(ops_dir, opsd))

        #extract jans-cli tgz archieve
        base.extract_from_zip(base.current_app.jans_zip, 'jans-cli-tui/cli_tui', self.jans_cli_install_dir)
        #extract pyDes from Jans archieve
        base.extract_file(base.current_app.jans_zip, 'jans-linux-setup/jans_setup/setup_app/pylib/pyDes.py', os.path.join(self.pylib_dir, 'pyDes.py'), ren=True)

        self.run([paths.cmd_ln, '-s', os.path.join(self.jans_cli_install_dir, 'cli', 'config_cli.py'), os.path.join(self.jans_cli_install_dir, 'config-cli.py')])
        self.run([paths.cmd_ln, '-s', os.path.join(self.jans_cli_install_dir, 'jans_cli_tui.py'), os.path.join(self.jans_cli_install_dir, 'config-cli-tui.py')])
        #self.run([paths.cmd_ln, '-s', os.path.join(self.jans_cli_install_dir, 'config_cli.py'), os.path.join(self.jans_cli_install_dir, 'scim-cli.py')])
        self.run([paths.cmd_chmod, '+x', os.path.join(self.jans_cli_install_dir, 'cli', 'config_cli.py')])
        self.run([paths.cmd_chmod, '+x', os.path.join(self.jans_cli_install_dir, 'jans_cli_tui.py')])


        #extract python libraries
        base.extract_from_zip(self.source_files[0][0], 'jwt', os.path.join(self.pylib_dir, 'jwt'))
        base.extract_from_zip(self.source_files[1][0], 'src/prompt_toolkit', os.path.join(self.pylib_dir, 'prompt_toolkit'))
        base.extract_from_zip(self.source_files[2][0], 'wcwidth', os.path.join(self.pylib_dir, 'wcwidth'))
        base.extract_from_zip(self.source_files[3][0], 'pygments', os.path.join(self.pylib_dir, 'pygments'))

        # extract yaml files
        base.extract_file(base.current_app.jans_zip, 'jans-config-api/docs/jans-config-api-swagger.yaml', os.path.join(ops_dir, 'jca', 'jans-config-api-swagger.yaml'), ren=True)
        for plugin_yaml_file in ('fido2-plugin-swagger.yaml', 'jans-admin-ui-plugin-swagger.yaml', 'scim-plugin-swagger.yaml', 'user-mgt-plugin-swagger.yaml'):
            base.extract_file(base.current_app.jans_zip, 'jans-config-api/plugins/docs/'+plugin_yaml_file, os.path.join(ops_dir, 'jca', plugin_yaml_file), ren=True)
        base.extract_file(base.current_app.jans_zip, 'jans-scim/server/src/main/resources/jans-scim-openapi.yaml', os.path.join(ops_dir, 'scim', 'scim.yaml'), ren=True)
        base.extract_file(base.current_app.jans_zip, 'jans-auth-server/docs/swagger.yaml', os.path.join(ops_dir, 'auth', 'swagger.yaml'), ren=True)


    def generate_configuration(self):
        self.check_clients([('tui_client_id', '2000.')])

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

        if Config.install_config_api:
            config['DEFAULT']['jca_client_id'] = Config.tui_client_id
            config['DEFAULT']['jca_client_secret_enc'] = Config.tui_client_encoded_pw
            if base.argsp.cli_test_client:
                config['DEFAULT']['jca_test_client_id'] = Config.jca_client_id
                config['DEFAULT']['jca_test_client_secret_enc'] = Config.jca_client_encoded_pw

        if Config.get('install_scim_server'):
            config['DEFAULT']['scim_client_id'] = Config.scim_client_id
            config['DEFAULT']['scim_client_secret_enc'] = Config.scim_client_encoded_pw

        config['DEFAULT']['jca_plugins'] = ','.join(base.current_app.ConfigApiInstaller.get_plugins())

        # clean old token and user data
        for prop in ('user_data', 'access_token_enc'):
            if prop in config['DEFAULT']:
                del config['DEFAULT'][prop]

        config.write(self.config_ini_fn.open('w'))
        self.config_ini_fn.chmod(0o600)


    def render_import_templates(self):
        self.renderTemplateInOut(self.ldif_client, self.templates_folder, self.output_folder)
        self.dbUtils.import_ldif([self.ldif_client])
