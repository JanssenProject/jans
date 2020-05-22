import os
import glob
import shutil

from setup_app.config.config import Config
from setup_app.utils.setup_utils import SetupUtils

class OpenDjInstaller(SetupUtils):

    def __init__(self):
        self.service_path = self.detect_service_path()


    def install(self):
        self.logIt("Running OpenDJ Setup")


        
        self.pbar.progress("opendj", "Extracting OpenDJ", False)
        self.extractOpenDJ()

        self.createLdapPw()
        
        try:
            self.pbar.progress("opendj", "OpenDJ: installing", False)
            if self.wrends_install == LOCAL:
                self.install_opendj()

                self.pbar.progress("opendj", "OpenDJ: preparing schema", False)
                self.prepare_opendj_schema()
                self.pbar.progress("opendj", "OpenDJ: setting up service", False)
                self.setup_opendj_service()

            if self.wrends_install:
                self.pbar.progress("opendj", "OpenDJ: configuring", False)
                self.configure_opendj()
                self.pbar.progress("opendj", "OpenDJ:  exporting certificate", False)
                self.export_opendj_public_cert()
                self.pbar.progress("opendj", "OpenDJ: creating indexes", False)
                self.index_opendj()
                self.pbar.progress("opendj", "OpenDJ: importing Ldif files", False)
                
                ldif_files = []

                if self.mappingLocations['default'] == 'ldap':
                    ldif_files += self.couchbaseBucketDict['default']['ldif']

                ldap_mappings = self.getMappingType('ldap')
  
                for group in ldap_mappings:
                    ldif_files +=  self.couchbaseBucketDict[group]['ldif']
  
                if not self.ldif_base in ldif_files:
                    ldif_files.insert(0, self.ldif_base)

                self.import_ldif_opendj(ldif_files)
                
                self.pbar.progress("opendj", "OpenDJ: post installation", False)
                if self.wrends_install == LOCAL:
                    self.post_install_opendj()
        except:
            self.logIt(traceback.format_exc(), True)

    def extractOpenDJ(self):        

        openDJArchive = max(glob.glob(os.path.join(self.distFolder, 'app/opendj-server-*4*.zip')))

        try:
            self.logIt("Unzipping %s in /opt/" % openDJArchive)
            self.run(['unzip', '-n', '-q', '%s' % (openDJArchive), '-d', '/opt/' ])
        except:
            self.logIt("Error encountered while doing unzip %s -d /opt/" % (openDJArchive))
            self.logIt(traceback.format_exc(), True)

        realLdapBaseFolder = os.path.realpath(self.ldapBaseFolder)
        self.run([self.cmd_chown, '-R', 'ldap:ldap', realLdapBaseFolder])

        if self.wrends_install == REMOTE:
            self.run(['ln', '-s', '/opt/opendj/template/config/', '/opt/opendj/config'])

    def create_user(self):
        self.createUser('ldap', Config.ldap_user_home)
        self.addUserToGroup('gluu', 'ldap')
        self.addUserToGroup('adm', 'ldap')

    def install_opendj(self):
        self.logIt("Running OpenDJ Setup")

        # Copy opendj-setup.properties so user ldap can find it in /opt/opendj
        setupPropsFN = os.path.join(Config.ldapBaseFolder, 'opendj-setup.properties')
        shutil.copy("%s/opendj-setup.properties" % Config.outputFolder, setupPropsFN)
        
        #TODO: Why we require this?
        #self.set_ownership()
        
        self.run(['chown', 'ldap:ldap', setupPropsFN])

        try:
            ldapSetupCommand = '%s/setup' % Config.ldapBaseFolder
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
            self.logIt(traceback.format_exc(), True)

        #Append self.jre_home to OpenDj java.properties        
        opendj_java_properties_fn = os.path.join(Config.ldapBaseFolder, 'config/java.properties')

        self.logIt("append self.jre_home to OpenDj %s" % opendj_java_properties_fn)
        with open(opendj_java_properties_fn,'a') as f:
            f.write('\ndefault.java-home={}\n'.format(Config.jre_home))

        try:
            self.logIt('Stopping opendj server')
            cmd = os.path.join(self.ldapBaseFolder, 'bin/stop-ds')
            self.run(['/bin/su','ldap', '-c', cmd], cwd='/opt/opendj/bin')
        except:
            self.logIt("Error stopping opendj", True)
            self.logIt(traceback.format_exc(), True)

    def post_install_opendj(self):
        try:
            os.remove(os.path.join(Config.ldapBaseFolder, 'opendj-setup.properties'))
        except:
            self.logIt("Error deleting OpenDJ properties. Make sure %s/opendj-setup.properties is deleted" % Config.ldapBaseFolder)
            self.logIt(traceback.format_exc(), True)

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
                          
        if self.mappingLocations['site'] == 'ldap':
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


        if (not Config.listenAllInterfaces) and (Config.wrends_install == LOCAL):
            config_changes.append(['set-connection-handler-prop', '--handler-name', '"LDAPS Connection Handler"', '--set', 'enabled:true', '--set', 'listen-address:127.0.0.1'])
            config_changes.append(['set-administration-connector-prop', '--set', 'listen-address:127.0.0.1'])
                          
        for changes in config_changes:
            cwd = os.path.join(Config.ldapBaseFolder, 'bin')
            dsconfigCmd = " ".join([
                                    Config.ldapDsconfigCommand,
                                    '--trustAll',
                                    '--no-prompt',
                                    '--hostname',
                                    Config.ldap_hostname,
                                    '--port',
                                    Config.ldap_admin_port,
                                    '--bindDN',
                                    '"%s"' % self.ldap_binddn,
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

    def import_ldif_template_opendj(self, ldif):
        self.logIt("Importing LDIF file '%s' into OpenDJ" % ldif)
        realInstallDir = os.path.realpath(self.outputFolder)

        ldif_file_fullpath = os.path.realpath(ldif)
        cwd = os.path.join(Config.ldapBaseFolder, 'bin')
        importParams = [
                          Config.loadLdifCommand,
                          '--hostname',
                          self.ldap_hostname,
                          '--port',
                          Config.ldap_admin_port,
                          '--bindDN',
                          '"%s"' % self.ldap_binddn,
                          '-j',
                          Config.ldapPassFn,
                          '--trustAll',
                          '--useSSL',
                          '--continueOnError',
                          '--filename',
                          ldif_file_fullpath,
                        ]

        importCmd = " ".join(importParams)
        
        # Check if there is no .pw file
        createPwFile = not os.path.exists(Config.ldapPassFn)
        if createPwFile:
            self.createLdapPw()
        
        self.run(['/bin/su',
                  'ldap',
                  '-c',
                  '%s' % importCmd], cwd=cwd)

        if createPwFile:
            Config.deleteLdapPw()

    def import_ldif_opendj(self, ldif_file_list=[]):

        #We won't load data to secondary cluster nodes
        if not Config.loadData:
            return
        
        if not ldif_file_list:
            self.logIt("Importing userRoot LDIF data")
        else:
            self.logIt("Importing LDIF File(s): " + ' '.join(ldif_file_list))

        if not ldif_file_list:
            ldif_file_list = Config.ldif_files
        
        for ldif_file_fn in ldif_file_list:
            ldif_file_fullpath = os.path.realpath(ldif_file_fn)
            cwd = os.path.join(self.ldapBaseFolder, 'bin')
            importParams = [
                              Config.loadLdifCommand,
                              '--hostname',
                              Config.ldap_hostname,
                              '--port',
                              Config.ldap_admin_port,
                              '--bindDN',
                              '"%s"' % self.ldap_binddn,
                              '-j',
                              Config.ldapPassFn,
                              '--trustAll',
                              '--useSSL',
                              '--continueOnError',
                              '--filename',
                              ldif_file_fullpath,
                            ]

            importCmd = " ".join(importParams)

            self.run(['/bin/su',
                      'ldap',
                      '-c',
                      '%s' % importCmd], cwd=cwd)

    def index_opendj_backend(self, backend):
        index_command = 'create-backend-index'
        cwd = os.path.join(self.ldapBaseFolder, 'bin')
        try:
            self.logIt("Running LDAP index creation commands for " + backend + " backend")
            # This json file contains a mapping of the required indexes.
            # [ { "attribute": "inum", "type": "string", "index": ["equality"] }, ...}

            with open(Config.openDjIndexJson) as f:
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
                                                 Config.ldapDsconfigCommand,
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
            self.logIt(traceback.format_exc(), True)

    def index_opendj(self):
        self.index_opendj_backend('userRoot')
        if Config.mappingLocations['site'] == 'ldap':
            self.index_opendj_backend('site')


    def prepare_opendj_schema(self):
        self.logIt("Copying OpenDJ schema")
        for schemaFile in self.openDjschemaFiles:
            self.copyFile(schemaFile, Config.openDjSchemaFolder)


        self.run([self.cmd_chmod, '-R', 'a+rX', Config.ldapBaseFolder])
        self.run([self.cmd_chown, '-R', 'ldap:ldap', Config.ldapBaseFolder])

    def setup_opendj_service(self):
        
        init_script_fn = '/etc/init.d/opendj'
        if (Config.os_type in ['centos', 'red', 'fedora'] and Config.os_initdaemon == 'systemd') or (Config.os_type+self.os_version in ('ubuntu18','debian9','debian10')):
            remove_init_script = True
            opendj_script_name = os.path.split(Config.opendj_service_centos7)[-1]
            opendj_dest_folder = "/etc/systemd/system"
            try:
                self.copyFile(Config.opendj_service_centos7, opendj_dest_folder)
                self.run([self.service_path, 'daemon-reload'])
                self.run([self.service_path, 'enable', 'opendj.service'])
                self.run([self.service_path, 'start', 'opendj.service'])
            except:
                self.logIt("Error copying script file %s to %s" % (opendj_script_name, opendj_dest_folder))
                self.logIt(traceback.format_exc(), True)
            if os.path.exists(init_script_fn):
                self.run(['rm', '-f', init_script_fn])
        else:
            self.run([Config.ldapDsCreateRcCommand, "--outputFile", "/etc/init.d/opendj", "--userName",  "ldap"])
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

    def enable(self):
        self.enable_service_at_start('opendj')
    
    def start(self):
        self.run([self.service_path, 'opendj', 'start'])
        
    def stop(self):
        self.run([self.service_path, 'opendj', 'stop'])
