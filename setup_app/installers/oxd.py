import os
import glob

from setup_app.config import Config
from setup_app.utils.setup_utils import SetupUtils

class OxdInstaller(SetupUtils):

    def __init__(self):
        super().__init__()
        self.oxd_root = '/opt/oxd-server/'

        self.logIt("Determining oxd server package")
        oxd_package_list = glob.glob(os.path.join(Config.distGluuFolder, 'oxd-server*.tgz'))

        if oxd_package_list:
            Config.oxd_package = max(oxd_package_list)

        self.logIt("oxd server package was determined as " + Config.oxd_package)


    def generate_keystore(self):
        # generate oxd-server.keystore for the hostname
        self.run([
            self.opensslCommand,
            'req', '-x509', '-newkey', 'rsa:4096', '-nodes',
            '-out', '/tmp/oxd.crt',
            '-keyout', '/tmp/oxd.key',
            '-days', '3650',
            '-subj', '/C={}/ST={}/L={}/O={}/CN={}/emailAddress={}'.format(Config.countryCode, Config.state, Config.city, Config.orgName, Config.hostname, Config.admin_email),
            ])

        self.run([
            Config.opensslCommand,
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

        oxd_keystore_fn = os.path.join(Config.oxd_root, 'conf/oxd-server.keystore')
        self.run(['cp', '-f', '/tmp/oxd.keystore', oxd_keystore_fn])
        self.run(['chown', 'jetty:jetty', oxd_keystore_fn])
        
        for f in ('/tmp/oxd.crt', '/tmp/oxd.key', '/tmp/oxd.p12', '/tmp/oxd.keystore'):
            self.run(['rm', '-f', f])

    def install(self):
        self.logIt("Installing oxd server...")
        
        oxd_server_yml_fn = os.path.join(self.oxd_root, 'conf/oxd-server.yml')
        
        self.run(['tar', '-zxf', Config.oxd_package, '-C', '/opt'])
        self.run(['chown', '-R', 'jetty:jetty', self.oxd_root])
        
        service_file = os.path.join(self.oxd_root, 'oxd-server.service')
        if os.path.exists(service_file):
            self.run(['cp', service_file, '/lib/systemd/system'])
        else:
            self.run([Config.cmd_ln, service_file, '/etc/init.d/oxd-server'])
            self.run(['update-rc.d', 'oxd-server', 'defaults'])

        self.run([
                'cp', 
                os.path.join(Config.install_dir, 'static/oxd/oxd-server.default'), 
                os.path.join(Config.osDefault, 'oxd-server')
                ])

        self.run(['mkdir', '/var/log/oxd-server'])
        self.run(['chown', 'jetty:jetty', '/var/log/oxd-server'])

        for fn in glob.glob(os.path.join(self.oxd_root,'bin/*')):
            self.run(['chmod', '+x', fn])

        if Config.oxd_use_gluu_storage:
            oxd_server_yml_fn = os.path.join(self.oxd_root, 'conf/oxd-server.yml')
            yml_str = self.readFile(oxd_server_yml_fn)
            oxd_yaml = ruamel.yaml.load(yml_str, ruamel.yaml.RoundTripLoader)

            oxd_yaml['storage_configuration'].pop('dbFileLocation')

            oxd_yaml['storage'] = 'gluu_server_configuration'

            oxd_yaml['storage_configuration']['type'] = Config.gluu_properties_fn

            oxd_yaml['storage_configuration']['connection'] = Config.ox_ldap_properties \
                if self.mappingLocations['default'] == 'ldap' else Config.gluuCouchebaseProperties

            try:
                oxd_yaml.yaml_set_comment_before_after_key('server', '\nConnectors')
            except:
                pass
            
            yml_str = ruamel.yaml.dump(oxd_yaml, Dumper=ruamel.yaml.RoundTripDumper)

            self.writeFile(oxd_server_yml_fn, yml_str)

        self.generate_keystore()

    def import_oxd_certificate(self):

        # import_oxd_certificate2javatruststore:
        self.logIt("Importing oxd certificate")

        try:

            oxd_hostname, oxd_port = self.parse_url(self.oxd_server_https)
            if not oxd_port: oxd_port=8443

            oxd_cert = ssl.get_server_certificate((oxd_hostname, oxd_port))
            oxd_alias = 'oxd_' + oxd_hostname.replace('.','_')
            oxd_cert_tmp_fn = '/tmp/{}.crt'.format(oxd_alias)

            with open(oxd_cert_tmp_fn,'w') as w:
                w.write(oxd_cert)

            self.run([self.cmd_keytool, '-import', '-trustcacerts', '-keystore', 
                            '/opt/jre/jre/lib/security/cacerts', '-storepass', 'changeit', 
                            '-noprompt', '-alias', oxd_alias, '-file', oxd_cert_tmp_fn])

        except:
            self.logIt(traceback.format_exc(), True)


    def enable(self):
        self.enable_service_at_start('oxd-server')
