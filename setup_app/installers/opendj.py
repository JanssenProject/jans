import os
import glob
import shutil
import ssl
import json
import ldap3

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
        self.pbar_text = "Installing OpenDJ"
        self.needdb = False # we don't need backend connection in this class
        self.app_type = AppType.SERVICE
        self.install_type = InstallOption.OPTONAL
        self.install_var = 'wrends_install'
        self.register_progess()

        self.openDjIndexJson = os.path.join(Config.install_dir, 'static/opendj/index.json')
        self.openDjSchemaFolder = os.path.join(Config.ldapBaseFolder, 'config/schema')
        self.openDjschemaFiles = glob.glob(os.path.join(Config.install_dir, 'static/opendj/*.ldif'))

        self.opendj_service_centos7 = os.path.join(Config.install_dir, 'static/opendj/systemd/opendj.service')
        self.ldapDsconfigCommand = os.path.join(Config.ldapBinFolder, 'dsconfig')
        self.ldapDsCreateRcCommand = os.path.join(Config.ldapBinFolder, 'create-rc-script')


    def install(self):
        self.logIt("Running OpenDJ Setup")

        if not base.snap:
            Config.pbar.progress(self.service_name, "Extracting OpenDJ", False)
            self.extractOpenDJ()

        self.createLdapPw()

        Config.pbar.progress(self.service_name, "Installing OpenDJ", False)
        if Config.wrends_install == InstallTypes.LOCAL:
            self.install_opendj()
            Config.pbar.progress(self.service_name, "Setting up OpenDJ service", False)
            self.setup_opendj_service()
            Config.pbar.progress(self.service_name, "Preparing OpenDJ schema", False)
            self.prepare_opendj_schema()

        # it is time to bind OpenDJ
        self.dbUtils.bind()

        if Config.wrends_install:
            Config.pbar.progress(self.service_name, "Creating OpenDJ backends", False)
            self.create_backends()
            Config.pbar.progress(self.service_name, "Configuring OpenDJ", False)
            self.configure_opendj()
            Config.pbar.progress(self.service_name, "Exporting OpenDJ certificate", False)
            self.export_opendj_public_cert()
            Config.pbar.progress(self.service_name, "Creating OpenDJ indexes", False)
            self.index_opendj()

            ldif_files = []

            if Config.mappingLocations['default'] == 'ldap':
                ldif_files += Config.couchbaseBucketDict['default']['ldif']

            ldap_mappings = self.getMappingType('ldap')

            for group in ldap_mappings:
                ldif_files +=  Config.couchbaseBucketDict[group]['ldif']

            Config.pbar.progress(self.service_name, "Importing ldif files to OpenDJ", False)
            if not Config.ldif_base in ldif_files:
                self.dbUtils.import_ldif([Config.ldif_base], force=BackendTypes.LDAP)

            self.dbUtils.import_ldif(ldif_files)

            Config.pbar.progress(self.service_name, "OpenDJ post installation", False)
            if Config.wrends_install == InstallTypes.LOCAL:
                self.post_install_opendj()

            self.enable()

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
        self.addUserToGroup('jans', 'ldap')
        self.addUserToGroup('adm', 'ldap')

    def install_opendj(self):
        self.logIt("Running OpenDJ Setup")

        #if base.snap and not os.path.exists(Config.ldapBaseFolder):
        #    self.run([paths.cmd_mkdir, Config.ldapBaseFolder])

        # Copy opendj-setup.properties so user ldap can find it in /opt/opendj
        setupPropsFN = os.path.join(Config.ldapBaseFolder, 'opendj-setup.properties')
        shutil.copy("%s/opendj-setup.properties" % Config.outputFolder, setupPropsFN)

        self.run([paths.cmd_chown, 'ldap:ldap', setupPropsFN])


        ldapSetupCommand = os.path.join(os.path.dirname(Config.ldapBinFolder), 'setup')

        setupCmd = " ".join([ldapSetupCommand,
                                '--no-prompt',
                                '--cli',
                                '--propertiesFilePath',
                                setupPropsFN,
                                '--acceptLicense'])
        if base.snap:
            self.run(setupCmd, shell=True)
        else:
            self.run(['/bin/su',
                          'ldap',
                          '-c',
                          setupCmd],
                          cwd='/opt/opendj',
                      )


        self.set_opendj_java_properties({
            'default.java-home': Config.jre_home,
            'start-ds.java-args': '-server -Xms{0}m -Xmx{0}m'.format(Config.opendj_max_ram),
            })

        try:
            self.logIt('Stopping opendj server')
            cmd = os.path.join(Config.ldapBinFolder, 'stop-ds')
            self.run(cmd, shell=True)
        except:
            self.logIt("Error stopping opendj", True)


    def set_opendj_java_properties(self, data):
        
        self.logIt("Setting OpenDJ params: {}".format(str(data)))

        opendj_java_properties_fn = os.path.join(Config.ldapBaseFolder, 'config/java.properties')
        opendj_java_properties = self.readFile(opendj_java_properties_fn)
        opendj_java_properties_list = opendj_java_properties.splitlines()

        for i, l in enumerate(opendj_java_properties_list[:]):
            ls = l.strip()
            if not ls or ls.startswith('#'):
                continue
            n = ls.find('=')
            if n > -1:
                jparam = ls[:n].strip()
                if jparam in data:
                    opendj_java_properties_list[i] = jparam + '=' + data.pop(jparam)

        for jparam in data:
            opendj_java_properties_list.append(jparam + '=' + data[jparam])

        self.writeFile(opendj_java_properties_fn, '\n'.join(opendj_java_properties_list))


    def post_install_opendj(self):
        try:
            os.remove(os.path.join(Config.ldapBaseFolder, 'opendj-setup.properties'))
        except:
            self.logIt("Error deleting OpenDJ properties. Make sure %s/opendj-setup.properties is deleted" % Config.ldapBaseFolder)


    def create_backends(self):
        backends = [
                    ['create-backend', '--backend-name', 'metric', '--set', 'base-dn:o=metric', '--type %s' % Config.ldap_backend_type, '--set', 'enabled:true', '--set', 'db-cache-percent:20'],
                    ]

        if Config.mappingLocations['site'] == 'ldap':
            backends.append(['create-backend', '--backend-name', 'site', '--set', 'base-dn:o=site', '--type %s' % Config.ldap_backend_type, '--set', 'enabled:true', '--set', 'db-cache-percent:20'])

        for changes in backends:
            cwd = os.path.join(Config.ldapBinFolder)
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
            if base.snap:
                self.run(dsconfigCmd, shell=True)
            else:
                self.run(['/bin/su',
                      'ldap',
                      '-c',
                      dsconfigCmd], cwd=cwd)


    def configure_opendj(self):
        self.logIt("Configuring OpenDJ")

        opendj_config = [
                ('ds-cfg-backend-id=userRoot,cn=Backends,cn=config', 'ds-cfg-db-cache-percent', '70', ldap3.MODIFY_REPLACE),
                ('cn=config', 'ds-cfg-single-structural-objectclass-behavior','accept', ldap3.MODIFY_REPLACE),
                ('cn=config', 'ds-cfg-reject-unauthenticated-requests', 'true', ldap3.MODIFY_REPLACE),
                ('cn=Default Password Policy,cn=Password Policies,cn=config', 'ds-cfg-allow-pre-encoded-passwords', 'true', ldap3.MODIFY_REPLACE),
                ('cn=Default Password Policy,cn=Password Policies,cn=config', 'ds-cfg-default-password-storage-scheme', 'cn=Salted SHA-512,cn=Password Storage Schemes,cn=config', ldap3.MODIFY_REPLACE),
                ('cn=File-Based Audit Logger,cn=Loggers,cn=config', 'ds-cfg-enabled', 'true', ldap3.MODIFY_REPLACE),
                ('cn=LDAP Connection Handler,cn=Connection Handlers,cn=config', 'ds-cfg-enabled', 'false', ldap3.MODIFY_REPLACE),
                ('cn=JMX Connection Handler,cn=Connection Handlers,cn=config', 'ds-cfg-enabled', 'false', ldap3.MODIFY_REPLACE),
                ('cn=Access Control Handler,cn=config', 'ds-cfg-global-aci', '(targetattr!="userPassword||authPassword||debugsearchindex||changes||changeNumber||changeType||changeTime||targetDN||newRDN||newSuperior||deleteOldRDN")(version 3.0; acl "Anonymous read access"; allow (read,search,compare) userdn="ldap:///anyone";)', ldap3.MODIFY_DELETE),        
            ]

        if (not Config.listenAllInterfaces) and (Config.wrends_install == InstallTypes.LOCAL):
            opendj_config.append(('cn=LDAPS Connection Handler,cn=Connection Handlers,cn=config', 'ds-cfg-listen-address', '127.0.0.1', ldap3.MODIFY_REPLACE))
            opendj_config.append(('cn=Administration Connector,cn=config', 'ds-cfg-listen-address', '127.0.0.1', ldap3.MODIFY_REPLACE))

        for dn, attr, val, change_type in opendj_config:
            self.logIt("Changing OpenDJ Configuration for {}".format(dn))
            self.dbUtils.ldap_conn.modify(
                    dn, 
                     {attr: [change_type, val]}
                    )
        #Create uniqueness for attrbiutes
        for attr in ('mail', 'uid'):
            self.logIt("Creating OpenDJ uniqueness for {}".format(attr))
            cn = 'Unique {} entry'.format(attr)
            self.dbUtils.ldap_conn.add(
                'cn={},cn=Plugins,cn=config'.format(cn),
                attributes={
                        'objectClass': ['top', 'ds-cfg-plugin', 'ds-cfg-unique-attribute-plugin'],
                        'ds-cfg-java-class': ['org.opends.server.plugins.UniqueAttributePlugin'],
                        'ds-cfg-enabled': ['true'],
                        'ds-cfg-plugin-type': ['postoperationadd', 'postoperationmodify', 'postoperationmodifydn', 'postsynchronizationadd', 'postsynchronizationmodify', 'postsynchronizationmodifydn', 'preoperationadd', 'preoperationmodify', 'preoperationmodifydn'],
                        'ds-cfg-type': [attr],
                        'cn': [cn],
                        'ds-cfg-base-dn': ['o=jans']
                        }
                )


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


    def index_opendj(self):

        self.logIt("Creating OpenDJ Indexes")

        with open(self.openDjIndexJson) as f:
            index_json = json.load(f)

        index_backends = ['userRoot']

        if Config.mappingLocations['site'] == 'ldap':
            index_backends.append('site')

        for attrDict in index_json:
            attr_name = attrDict['attribute']
            for backend in attrDict['backend']:
                if backend in index_backends:
                    dn = 'ds-cfg-attribute={},cn=Index,ds-cfg-backend-id={},cn=Backends,cn=config'.format(attrDict['attribute'], backend)
                    entry = {
                            'objectClass': ['top','ds-cfg-backend-index'],
                            'ds-cfg-attribute': [attrDict['attribute']],
                            'ds-cfg-index-type': attrDict['index'],
                            'ds-cfg-index-entry-limit': ['4000']
                            }
                    self.logIt("Creating Index {}".format(dn))
                    self.dbUtils.ldap_conn.add(dn, attributes=entry)


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
        if not base.snap:
            init_script_fn = '/etc/init.d/opendj'
            if (base.clone_type == 'rpm' and base.os_initdaemon == 'systemd') or base.deb_sysd_clone:
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

                if base.os_type in ['ubuntu', 'debian']:
                    self.run([paths.cmd_update_rc, "-f", "opendj", "remove"])

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
