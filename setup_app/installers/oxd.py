import os
import glob
import ruamel.yaml

from setup_app import paths
from setup_app.utils import base
from setup_app.config import Config
from setup_app.utils.setup_utils import SetupUtils
from setup_app.installers.base import BaseInstaller

class OxdInstaller(SetupUtils, BaseInstaller):

    def __init__(self):
        self.service_name = 'oxd-server'
        self.pbar_text = "Installing Oxd Server"
        self.oxd_root = '/opt/oxd-server/'


    def install(self):
        
        oxd_server_yml_fn = os.path.join(self.oxd_root, 'conf/oxd-server.yml')
        
        self.run(['tar', '-zxf', Config.oxd_package, '-C', '/opt'])
        self.run(['chown', '-R', 'jetty:jetty', self.oxd_root])
        
        service_file = os.path.join(self.oxd_root, 'oxd-server.service')
        if os.path.exists(service_file):
            self.run(['cp', service_file, '/lib/systemd/system'])
        else:
            self.run([Config.cmd_ln, service_file, '/etc/init.d/oxd-server'])

        self.run([
                'cp', 
                os.path.join(Config.install_dir, 'static/oxd/oxd-server.default'), 
                os.path.join(Config.osDefault, 'oxd-server')
                ])

        log_dir = '/var/log/oxd-server'
        if not os.path.exists(log_dir):
            self.run([paths.cmd_mkdir, log_dir])

        self.run(['chown', 'jetty:jetty', '/var/log/oxd-server'])

        for fn in glob.glob(os.path.join(self.oxd_root,'bin/*')):
            self.run([paths.cmd_chmod, '+x', fn])

        if Config.oxd_use_gluu_storage:
            oxd_server_yml_fn = os.path.join(self.oxd_root, 'conf/oxd-server.yml')
            yml_str = self.readFile(oxd_server_yml_fn)
            oxd_yaml = ruamel.yaml.load(yml_str, ruamel.yaml.RoundTripLoader)

            oxd_yaml['storage_configuration'].pop('dbFileLocation')
            oxd_yaml['storage'] = 'gluu_server_configuration'
            oxd_yaml['storage_configuration']['baseDn'] = 'o=gluu'
            oxd_yaml['storage_configuration']['type'] = Config.gluu_properties_fn
            oxd_yaml['storage_configuration']['connection'] = Config.ox_ldap_properties if self.mappingLocations['default'] == 'ldap' else Config.gluuCouchebaseProperties
            oxd_yaml['storage_configuration']['salt'] = os.path.join(self.configFolder, "salt")

            yml_str = ruamel.yaml.dump(oxd_yaml, Dumper=ruamel.yaml.RoundTripDumper)

            self.writeFile(oxd_server_yml_fn, yml_str)

        self.generate_keystore()

        self.enable()

    def generate_keystore(self):
        # generate oxd-server.keystore for the hostname
        self.run([
            paths.cmd_openssl,
            'req', '-x509', '-newkey', 'rsa:4096', '-nodes',
            '-out', '/tmp/oxd.crt',
            '-keyout', '/tmp/oxd.key',
            '-days', '3650',
            '-subj', '/C={}/ST={}/L={}/O={}/CN={}/emailAddress={}'.format(Config.countryCode, Config.state, Config.city, Config.orgName, Config.hostname, Config.admin_email),
            ])

        self.run([
            paths.cmd_openssl,
            'pkcs12', '-export',
            '-in', '/tmp/oxd.crt',
            '-inkey', '/tmp/oxd.key',
            '-out', '/tmp/oxd.p12',
            '-name', Config.hostname,
            '-passout', 'pass:example'
            ])

        self.run([
            Config.cmd_keytool,
            '-importkeystore',
            '-deststorepass', 'example',
            '-destkeypass', 'example',
            '-destkeystore', '/tmp/oxd.keystore',
            '-srckeystore', '/tmp/oxd.p12',
            '-srcstoretype', 'PKCS12',
            '-srcstorepass', 'example',
            '-alias', Config.hostname,
            ])

        oxd_keystore_fn = os.path.join(self.oxd_root, 'conf/oxd-server.keystore')
        self.run(['cp', '-f', '/tmp/oxd.keystore', oxd_keystore_fn])
        self.run([paths.cmd_chown, 'jetty:jetty', oxd_keystore_fn])
        
        for f in ('/tmp/oxd.crt', '/tmp/oxd.key', '/tmp/oxd.p12', '/tmp/oxd.keystore'):
            self.run([paths.cmd_rm, '-f', f])

