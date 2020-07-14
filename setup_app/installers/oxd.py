import os
import glob
import ruamel.yaml

from setup_app import paths
from setup_app.static import AppType, InstallOption
from setup_app.utils import base
from setup_app.config import Config
from setup_app.utils.setup_utils import SetupUtils
from setup_app.installers.base import BaseInstaller

class OxdInstaller(SetupUtils, BaseInstaller):

    def __init__(self):
        self.service_name = 'oxd-server'
        self.oxd_root = '/opt/oxd-server/'
        self.needdb = False # we don't need backend connection in this class
        self.app_type = AppType.SERVICE
        self.install_type = InstallOption.OPTONAL
        self.install_var = 'installOxd'
        self.register_progess()

        self.oxd_server_yml_fn = os.path.join(self.oxd_root, 'conf/oxd-server.yml')

    def install(self):

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

        log_file = os.path.join(log_dir, 'oxd-server.log')
        if not os.path.exists(log_file):
            open(log_file, 'w').close()

        self.run(['chown', '-R', 'jetty:jetty', '/var/log/oxd-server'])

        for fn in glob.glob(os.path.join(self.oxd_root,'bin/*')):
            self.run([paths.cmd_chmod, '+x', fn])

        self.modify_config_yml()
        self.generate_keystore()

        self.enable()

    def modify_config_yml(self):

        yml_str = self.readFile(self.oxd_server_yml_fn)
        oxd_yaml = ruamel.yaml.load(yml_str, ruamel.yaml.RoundTripLoader)

        if 'bind_ip_addresses' in oxd_yaml:
            oxd_yaml['bind_ip_addresses'].append(Config.ip)
        else:
            for i, k in enumerate(oxd_yaml):
                if k == 'storage':
                    break
            else:
                i = 1
            oxd_yaml.insert(i, 'bind_ip_addresses',  [Config.ip])

        if Config.get('oxd_use_gluu_storage'):


            oxd_yaml['storage_configuration'].pop('dbFileLocation')
            oxd_yaml['storage'] = 'gluu_server_configuration'
            oxd_yaml['storage_configuration']['baseDn'] = 'o=gluu'
            oxd_yaml['storage_configuration']['type'] = Config.gluu_properties_fn
            oxd_yaml['storage_configuration']['connection'] = Config.ox_ldap_properties if Config.mappingLocations['default'] == 'ldap' else Config.gluuCouchebaseProperties
            oxd_yaml['storage_configuration']['salt'] = os.path.join(Config.configFolder, "salt")

        yml_str = ruamel.yaml.dump(oxd_yaml, Dumper=ruamel.yaml.RoundTripDumper)
        self.writeFile(self.oxd_server_yml_fn, yml_str)


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

    def installed(self):
        return os.path.exists(self.oxd_server_yml_fn)


    def download_files(self, force=False):
        oxd_url = 'https://ox.gluu.org/maven/org/gluu/oxd-server/{0}/oxd-server-{0}-distribution.zip'.format(Config.oxVersion)

        self.logIt("Downloading {} and preparing package".format(os.path.basename(oxd_url)))
        
        oxd_zip_fn = '/tmp/oxd-server.zip'
        oxd_tmp_dir = '/tmp/oxd-server'

        self.run([paths.cmd_wget, '-nv', oxd_url, '-O', oxd_zip_fn])
        self.run([paths.cmd_unzip, '-qqo', '/tmp/oxd-server.zip', '-d', oxd_tmp_dir])
        self.run([paths.cmd_mkdir, os.path.join(oxd_tmp_dir,'data')])

        if base.os_name in ('ubuntu18','debian9'):
            default_url = 'https://raw.githubusercontent.com/GluuFederation/oxd/version_{}/debian/oxd-server-default'.format(Config.oxVersion)
            self.run([paths.cmd_wget, '-nv', default_url, '-O', os.path.join(oxd_tmp_dir, 'oxd-server-default')])

        service_file = 'oxd-server.init.d' if base.os_name in ('ubuntu18','debian9') else 'oxd-server.service'
        service_url = 'https://raw.githubusercontent.com/GluuFederation/community-edition-package/master/package/systemd/oxd-server.service'.format(Config.oxVersion, service_file)
        self.run(['wget', '-nv', service_url, '-O', os.path.join(oxd_tmp_dir, service_file)])

        oxd_server_sh_url = 'https://raw.githubusercontent.com/GluuFederation/oxd/version_{}/debian/oxd-server'.format(Config.oxVersion)
        self.run([paths.cmd_wget, '-nv', oxd_server_sh_url, '-O', os.path.join(oxd_tmp_dir, 'bin/oxd-server')])

        self.run(['tar', '-zcf', os.path.join(Config.distGluuFolder, 'oxd-server.tgz'), 'oxd-server'], cwd='/tmp')

        self.oxd_package = os.path.join(Config.distGluuFolder, 'oxd-server.tgz')
