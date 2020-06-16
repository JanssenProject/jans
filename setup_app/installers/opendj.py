import os
import glob
import shutil
import ssl
import json

from setup_app import paths
from setup_app.static import AppType, InstallOption
from setup_app.config import Config
from setup_app.utils import base
from setup_app.static import InstallTypes, BackendTypes
from setup_app.utils.setup_utils import SetupUtils
from setup_app.installers.base import BaseInstaller


class OpenDjInstaller(BaseInstaller, SetupUtils):

    def __init__(self):
        self.service_name = 'opendj'
        self.needdb = False # we don't need backend connection in this class
        self.app_type = AppType.SERVICE
        self.install_type = InstallOption.OPTONAL
        self.install_var = 'wrends_install'
        self.register_progess()

        self.openDjIndexJson = os.path.join(Config.install_dir, 'static/opendj/index.json')
        self.openDjSchemaFolder = os.path.join(Config.ldapBaseFolder, 'config/schema')
        self.openDjschemaFiles = glob.glob(os.path.join(Config.install_dir, 'static/opendj/*.ldif'))

        self.opendj_service_centos7 = os.path.join(Config.install_dir, 'static/opendj/systemd/opendj.service')
        self.ldapDsconfigCommand = os.path.join(Config.ldapBaseFolder, 'bin/dsconfig')
        self.ldapDsCreateRcCommand = os.path.join(Config.ldapBaseFolder, 'bin/create-rc-script')


    def install(self):
        self.logIt("Running OpenDJ Setup")

        Config.pbar.progress(self.service_name, "Extracting OpenDJ", False)
        self.extractOpenDJ()

        self.createLdapPw()
        
        try:
            Config.pbar.progress(self.service_name, "Installing OpenDJ", False)
            if Config.wrends_install == InstallTypes.LOCAL:
                self.install_opendj()
                Config.pbar.progress(self.service_name, "Setting up service", False)
                self.setup_opendj_service()
                Config.pbar.progress(self.service_name, "Preparing schema", False)
                self.prepare_opendj_schema()

            if Config.wrends_install:
                Config.pbar.progress(self.service_name, "Configuring OpenDJ", False)
                self.configure_opendj()
                Config.pbar.progress(self.service_name, "Exporting certificate", False)
                self.export_opendj_public_cert()
                Config.pbar.progress(self.service_name, "Creating indexes", False)
                self.index_opendj()

                ldif_files = []

                if Config.mappingLocations['default'] == 'ldap':
                    ldif_files += Config.couchbaseBucketDict['default']['ldif']

                ldap_mappings = self.getMappingType('ldap')
  
                for group in ldap_mappings:
                    ldif_files +=  Config.couchbaseBucketDict[group]['ldif']
  
                # Now bind ldap and import ldif files
                self.dbUtils.bind()
                Config.pbar.progress(self.service_name, "Importing ldif files", False)
                if not Config.ldif_base in ldif_files:
                    self.dbUtils.import_ldif([Config.ldif_base], force=BackendTypes.LDAP)

                self.dbUtils.import_ldif(ldif_files)

                Config.pbar.progress(self.service_name, "Post installation", False)
                if Config.wrends_install == InstallTypes.LOCAL:
                    self.post_install_opendj()
        except:
            self.logIt("Error installing opendj", True)

    def extractOpenDJ(self):        

        openDJArchive = max(glob.glob(os.path.join(Config.distFolder, 'app/opendj-server-*4*.zip')))

        try:
            self.logIt("Unzipping %s in /opt/" % openDJArchive)
            self.run([paths.cmd_unzip, '-n', '-q', '%s' % (openDJArchive), '-d', '/opt/' ])
        except:
            self.logIt("Error encountered while doing unzip %s -d /opt/" % (openDJArchive))

        realLdapBaseFolder = os.path.realpath(Config.ldapBaseFolder)
        self.run([paths.cmd_chown, '-R', 'ldap:ldap', realLdapBaseFolder])

        if Config.wrends_install == InstallTypes.REMOTE:
            self.run([paths.cmd_ln, '-s', '/opt/opendj/template/config/', '/opt/opendj/config'])

    def create_user(self):
        self.createUser('ldap', Config.ldap_user_home)
        self.addUserToGroup('gluu', 'ldap')
        self.addUserToGroup('adm', 'ldap')

    def install_opendj(self):
        self.logIt("Running OpenDJ Setup")

        # Copy opendj-setup.properties so user ldap can find it in /opt/opendj
        setupPropsFN = os.path.join(Config.ldapBaseFolder, 'opendj-setup.properties')
        shutil.copy("%s/opendj-setup.properties" % Config.outputFolder, setupPropsFN)

        self.run([paths.cmd_chown, 'ldap:ldap', setupPropsFN])

        try:
            ldapSetupCommand = os.path.join(Config.ldapBaseFolder, 'setup')
            setupCmd = " ".join([ldapSetupCommand,
                                '--no-prompt',
                                '--cli',
                                '--propertiesFilePath',
                                setupPropsFN,
                                '--acceptLicense'])
            self.run(['/bin/su',
                      'ldap',
                      '-c',
                      setupCmd],
                      cwd='/opt/opendj',
                      )
        except:
            self.logIt("Error running LDAP setup script", True)

        #Append self.jre_home to OpenDj java.properties        
        opendj_java_properties_fn = os.path.join(Config.ldapBaseFolder, 'config/java.properties')

        self.logIt("append self.jre_home to OpenDj %s" % opendj_java_properties_fn)
        with open(opendj_java_properties_fn,'a') as f:
            f.write('\ndefault.java-home={}\n'.format(Config.jre_home))

        try:
            self.logIt('Stopping opendj server')
            cmd = os.path.join(Config.ldapBaseFolder, 'bin/stop-ds')
            self.run(['/bin/su','ldap', '-c', cmd], cwd='/opt/opendj/bin')
        except:
            self.logIt("Error stopping opendj", True)

    def post_install_opendj(self):
        try:
            os.remove(os.path.join(Config.ldapBaseFolder, 'opendj-setup.properties'))
        except:
            self.logIt("Error deleting OpenDJ properties. Make sure %s/opendj-setup.properties is deleted" % Config.ldapBaseFolder)

    def configure_opendj(self):
        self.logIt("Configuring OpenDJ")

        opendj_prop_name = 'global-aci:\'(targetattr!="userPassword||authPassword||debugsearchindex||changes||changeNumber||changeType||changeTime||targetDN||newRDN||newSuperior||deleteOldRDN")(version 3.0; acl "Anonymous read access"; allow (read,search,compare) userdn="ldap:///anyone";)\''
        config_changes = [
                          ['set-backend-prop', '--backend-name', 'userRoot', '--set', 'db-cache-percent:70'],
                          ['set-global-configuration-prop', '--set', 'single-structural-objectclass-behavior:accept'],
                          ['set-password-policy-prop', '--policy-name', '"Default Password Policy"', '--set', 'allow-pre-encoded-passwords:true'],
                          ['set-log-publisher-prop', '--publisher-name', '"File-Based Audit Logger"', '--set', 'enabled:true'],
                          ['create-backend', '--backend-name', 'metric', '--set', 'base-dn:o=metric', '--type %s' % Config.ldap_backend_type, '--set', 'enabled:true', '--set', 'db-cache-percent:20'],
                          ]
                          
        if Config.mappingLocations['site'] == 'ldap':
            config_changes.append(['create-backend', '--backend-name', 'site', '--set', 'base-dn:o=site', '--type %s' % Config.ldap_backend_type, '--set', 'enabled:true', '--set', 'db-cache-percent:20'])

        config_changes += [
                          ['set-connection-handler-prop', '--handler-name', '"LDAP Connection Handler"', '--set', 'enabled:false'],
                          ['set-connection-handler-prop', '--handler-name', '"JMX Connection Handler"', '--set', 'enabled:false'],
                          ['set-access-control-handler-prop', '--remove', '%s' % opendj_prop_name],
                          ['set-global-configuration-prop', '--set', 'reject-unauthenticated-requests:true'],
                          ['set-password-policy-prop', '--policy-name', '"Default Password Policy"', '--set', 'default-password-storage-scheme:"Salted SHA-512"'],
                          ['create-plugin', '--plugin-name', '"Unique mail address"', '--type', 'unique-attribute', '--set enabled:true',  '--set', 'base-dn:o=gluu', '--set', 'type:mail'],
                          ['create-plugin', '--plugin-name', '"Unique uid entry"', '--type', 'unique-attribute', '--set enabled:true',  '--set', 'base-dn:o=gluu', '--set', 'type:uid'],
                          ['set-password-policy-prop', '--policy-name', '"Default Password Policy"', '--set', 'default-password-storage-scheme:"Salted SHA-512"'],
                          ]


        if (not Config.listenAllInterfaces) and (Config.wrends_install == InstallTypes.LOCAL):
            config_changes.append(['set-connection-handler-prop', '--handler-name', '"LDAPS Connection Handler"', '--set', 'enabled:true', '--set', 'listen-address:127.0.0.1'])
            config_changes.append(['set-administration-connector-prop', '--set', 'listen-address:127.0.0.1'])
                          
        for changes in config_changes:
            cwd = os.path.join(Config.ldapBaseFolder, 'bin')
            dsconfigCmd = " ".join([
                                    self.ldapDsconfigCommand,
                                    '--trustAll',
                                    '--no-prompt',
                                    '--hostname',
                                    Config.ldap_hostname,
                                    '--port',
                                    Config.ldap_admin_port,
                                    '--bindDN',
                                    '"%s"' % Config.ldap_binddn,
                                    '--bindPasswordFile',
                                    Config.ldapPassFn] + changes)
            self.run(['/bin/su',
                      'ldap',
                      '-c',
                      dsconfigCmd], cwd=cwd)

    def export_opendj_public_cert(self):
        # Load password to acces OpenDJ truststore
        self.logIt("Getting OpenDJ certificate")

        opendj_cert = ssl.get_server_certificate((Config.ldap_hostname, Config.ldaps_port))
        with open(Config.opendj_cert_fn,'w') as w:
            w.write(opendj_cert)

        # Convert OpenDJ certificate to PKCS12
        self.logIt("Importing OpenDJ certificate to truststore")
        self.run([Config.cmd_keytool,
                  '-importcert',
                  '-noprompt',
                  '-alias',
                  'server-cert',
                  '-file',
                  Config.opendj_cert_fn,
                  '-keystore',
                  Config.opendj_p12_fn,
                  '-storetype',
                  'PKCS12',
                  '-storepass',
                  Config.opendj_p12_pass
                  ])

        # Import OpenDJ certificate into java truststore
        self.logIt("Import OpenDJ certificate")

        self.run([Config.cmd_keytool, "-import", "-trustcacerts", "-alias", "%s_opendj" % Config.hostname, \
                  "-file", Config.opendj_cert_fn, "-keystore", Config.defaultTrustStoreFN, \
                  "-storepass", "changeit", "-noprompt"])


    def index_opendj_backend(self, backend):
        index_command = 'create-backend-index'
        cwd = os.path.join(Config.ldapBaseFolder, 'bin')
        try:
            self.logIt("Running LDAP index creation commands for " + backend + " backend")
            # This json file contains a mapping of the required indexes.
            # [ { "attribute": "inum", "type": "string", "index": ["equality"] }, ...}

            with open(self.openDjIndexJson) as f:
                index_json = json.load(f)

            for attrDict in index_json:
                attr_name = attrDict['attribute']
                index_types = attrDict['index']
                for index_type in index_types:
                    backend_names = attrDict['backend']
                    for backend_name in backend_names:
                        if (backend_name == backend):
                            self.logIt("Creating %s index for attribute %s" % (index_type, attr_name))
                            indexCmd = " ".join([
                                                 self.ldapDsconfigCommand,
                                                 index_command,
                                                 '--backend-name',
                                                 backend,
                                                 '--type',
                                                 'generic',
                                                 '--index-name',
                                                 attr_name,
                                                 '--set',
                                                 'index-type:%s' % index_type,
                                                 '--set',
                                                 'index-entry-limit:4000',
                                                 '--hostName',
                                                 Config.ldap_hostname,
                                                 '--port',
                                                 Config.ldap_admin_port,
                                                 '--bindDN',
                                                 '"%s"' % Config.ldap_binddn,
                                                 '-j', Config.ldapPassFn,
                                                 '--trustAll',
                                                 '--noPropertiesFile',
                                                 '--no-prompt'])
                            self.run(['/bin/su',
                                      'ldap',
                                      '-c',
                                      indexCmd], cwd=cwd)

        except:
            self.logIt("Error occured during backend " + backend + " LDAP indexing", True)

    def index_opendj(self):
        self.index_opendj_backend('userRoot')
        if Config.mappingLocations['site'] == 'ldap':
            self.index_opendj_backend('site')


    def prepare_opendj_schema(self):
        self.logIt("Copying OpenDJ schema")
        for schemaFile in self.openDjschemaFiles:
            self.copyFile(schemaFile, self.openDjSchemaFolder)

        self.run([paths.cmd_chmod, '-R', 'a+rX', Config.ldapBaseFolder])
        self.run([paths.cmd_chown, '-R', 'ldap:ldap', Config.ldapBaseFolder])
        
        self.logIt("Re-starting OpenDj after schema update")
        self.stop()
        self.start()

    def setup_opendj_service(self):

        init_script_fn = '/etc/init.d/opendj'
        if (base.clone_type == 'rpm' and base.os_initdaemon == 'systemd') or (base.os_name in ('ubuntu18','debian9','debian10')):
            remove_init_script = True
            opendj_script_name = os.path.basename(self.opendj_service_centos7)
            opendj_dest_folder = "/etc/systemd/system"
            try:
                self.copyFile(self.opendj_service_centos7, opendj_dest_folder)
            except:
                self.logIt("Error copying script file %s to %s" % (opendj_script_name, opendj_dest_folder))
            if os.path.exists(init_script_fn):
                self.run(['rm', '-f', init_script_fn])
        else:
            self.run([self.ldapDsCreateRcCommand, "--outputFile", "/etc/init.d/opendj", "--userName",  "ldap"])
            # Make the generated script LSB compliant
            lsb_str=(
                    '### BEGIN INIT INFO\n'
                    '# Provides:          opendj\n'
                    '# Required-Start:    $remote_fs $syslog\n'
                    '# Required-Stop:     $remote_fs $syslog\n'
                    '# Default-Start:     2 3 4 5\n'
                    '# Default-Stop:      0 1 6\n'
                    '# Short-Description: Start daemon at boot time\n'
                    '# Description:       Enable service provided by daemon.\n'
                    '### END INIT INFO\n'
                    )
            self.insertLinesInFile("/etc/init.d/opendj", 1, lsb_str)

            if self.os_type in ['ubuntu', 'debian']:
                self.run(["/usr/sbin/update-rc.d", "-f", "opendj", "remove"])

            self.fix_init_scripts('opendj', init_script_fn)

        self.reload_daemon()

    def installed(self):
        if os.path.exists(self.openDjSchemaFolder):
            wrends_install = InstallTypes.LOCAL
        elif os.path.exists(Config.opendj_p12_fn):
            wrends_install = InstallTypes.REMOTE
        else:
            wrends_install = 0

        return wrends_install
