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
        setattr(base.current_app, self.__class__.__name__, self)
        self.service_name = 'oxd-server'
        self.oxd_root = '/opt/oxd-server/'
        self.needdb = False # we don't need backend connection in this class
        self.app_type = AppType.SERVICE
        self.install_type = InstallOption.OPTONAL
        self.install_var = 'installOxd'
        self.register_progess()

        self.oxd_server_yml_fn = os.path.join(self.oxd_root, 'conf/oxd-server.yml')

    def install(self):
        self.logIt("Installing", pbar=self.service_name)
        self.run(['tar', '-zxf', Config.oxd_package, '--no-same-owner', '--strip-components=1', '-C', self.oxd_root])
        self.run(['chown', '-R', 'jetty:jetty', self.oxd_root])
        
        if base.snap:
            self.log_dir = os.path.join(base.snap_common, 'jans/oxd-server/log/')
        else:
            self.log_dir = '/var/log/oxd-server'
            service_file = os.path.join(self.oxd_root, 'oxd-server.service')
            if os.path.exists(service_file):
                self.run(['cp', service_file, '/lib/systemd/system'])
            else:
                self.run([Config.cmd_ln, service_file, '/etc/init.d/oxd-server'])

        if not os.path.exists(self.log_dir):
            self.run([paths.cmd_mkdir, self.log_dir])

        self.run([
                'cp', 
                os.path.join(Config.install_dir, 'static/oxd/oxd-server.default'), 
                os.path.join(Config.osDefault, 'oxd-server')
                ])

        self.log_file = os.path.join(self.log_dir, 'oxd-server.log')
        if not os.path.exists(self.log_file):
            open(self.log_file, 'w').close()

        if not base.snap:
            self.run(['chown', '-R', 'jetty:jetty', self.log_dir])

        for fn in glob.glob(os.path.join(self.oxd_root,'bin/*')):
            self.run([paths.cmd_chmod, '+x', fn])

        self.modify_config_yml()
        self.generate_keystore()

        self.enable()

    def modify_config_yml(self):
        self.logIt("Configuring", pbar=self.service_name)
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
            addr_list = [Config.ip]
            if base.snap:
                addr_list.append('127.0.0.1')
            oxd_yaml.insert(i, 'bind_ip_addresses',  addr_list)

        if Config.get('oxd_use_jans_storage'):

            oxd_yaml['storage_configuration'].pop('dbFileLocation')
            oxd_yaml['storage'] = 'jans_server_configuration'
            oxd_yaml['storage_configuration']['baseDn'] = 'o=jans'
            oxd_yaml['storage_configuration']['type'] = Config.jans_properties_fn
            oxd_yaml['storage_configuration']['connection'] = Config.ox_ldap_properties if Config.mapping_locations['default'] == 'ldap' else Config.jansCouchebaseProperties
            oxd_yaml['storage_configuration']['salt'] = os.path.join(Config.configFolder, "salt")

        if base.snap:
            for appenders in oxd_yaml['logging']['appenders']:
                if appenders['type'] == 'file':
                    appenders['currentLogFilename'] = self.log_file
                    appenders['archivedLogFilenamePattern'] = os.path.join(base.snap_common, 'jans/oxd-server/log/oxd-server-%d{yyyy-MM-dd}-%i.log.gz')

        yml_str = ruamel.yaml.dump(oxd_yaml, Dumper=ruamel.yaml.RoundTripDumper)
        self.writeFile(self.oxd_server_yml_fn, yml_str)


    def generate_keystore(self):
        self.logIt("Generating certificate", pbar=self.service_name)
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
        oxd_url = os.path.join(base.current_app.app_info['JANS_MAVEN'], 'maven/io/jans/oxd-server/{0}/oxd-server-{0}-distribution.zip').format(base.current_app.app_info['ox_version'])

        self.logIt("Downloading {} and preparing package".format(os.path.basename(oxd_url)))

        oxd_zip_fn = '/tmp/oxd-server.zip'
        oxd_tgz_fn = '/tmp/oxd-server.tgz' if base.snap else os.path.join(Config.dist_jans_dir, 'oxd-server.tgz')
        tmp_dir = os.path.join('/tmp', os.urandom(5).hex())
        oxd_tmp_dir = os.path.join(tmp_dir, 'oxd-server')

        self.run([paths.cmd_mkdir, '-p', oxd_tmp_dir])
        self.download_file(oxd_url, oxd_zip_fn)
        self.run([paths.cmd_unzip, '-qqo', oxd_zip_fn, '-d', oxd_tmp_dir])
        self.run([paths.cmd_mkdir, os.path.join(oxd_tmp_dir, 'data')])

        if not base.snap:
            service_file = 'oxd-server.init.d' if base.deb_sysd_clone else 'oxd-server.service'
            service_url = 'https://raw.githubusercontent.com/GluuFederation/community-edition-package/master/package/systemd/oxd-server.service'.format(base.current_app.app_info['ox_version'], service_file)
            self.download_file(service_url, os.path.join(oxd_tmp_dir, service_file))

        oxd_server_sh_url = 'https://raw.githubusercontent.com/GluuFederation/oxd/master/debian/oxd-server'
        self.download_file(oxd_server_sh_url, os.path.join(oxd_tmp_dir, 'bin/oxd-server'))

        self.run(['tar', '-zcf', oxd_tgz_fn, 'oxd-server'], cwd=tmp_dir)
        #self.run(['rm', '-r', '-f', tmp_dir])
        Config.oxd_package = oxd_tgz_fn

    def create_folders(self):
        if not os.path.exists(self.oxd_root):
            self.run([paths.cmd_mkdir, self.oxd_root])

    def import_oxd_certificate(self):
        oxd_yaml = self.get_yaml_config()
        oxd_alias = 'oxd_' + self.oxd_hostname.replace('.','_')
        oxd_cert_fn = os.path.join(Config.outputFolder, '{}.pem'.format(oxd_alias))
        # let's delete if alias exists
        self.delete_key(oxd_alias)
        store_alias = 'localhost' if self.oxd_hostname == 'localhost' else Config.hostname
        self.export_cert_from_store(store_alias, oxd_yaml['server']['applicationConnectors'][0]['keyStorePath'], self.oxd_keystore_passw, oxd_cert_fn)
        self.import_cert_to_java_truststore(oxd_alias, oxd_cert_fn)
