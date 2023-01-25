import os
import glob
import shutil
import ssl
import json
import ldap3
import sys
import time

from pathlib import Path

from setup_app import paths
from setup_app.static import AppType, InstallOption
from setup_app.config import Config
from setup_app.utils import base
from setup_app.static import InstallTypes, BackendTypes, SetupProfiles, fapolicyd_rule_tmp
from setup_app.utils.setup_utils import SetupUtils
from setup_app.installers.base import BaseInstaller
from setup_app.utils.ldif_utils import myLdifParser
from setup_app.pylib.ldif4.ldif import LDIFWriter


class OpenDjInstaller(BaseInstaller, SetupUtils):

    opendj_link = 'https://maven.gluu.org/maven/org/gluufederation/opendj/opendj-server-legacy/{0}/opendj-server-legacy-{0}.zip'.format(base.current_app.app_info['OPENDJ_VERSION'])
    source_files = [
            (os.path.join(Config.dist_app_dir, os.path.basename(opendj_link)), opendj_link),
            ]

    def __init__(self):
        setattr(base.current_app, self.__class__.__name__, self)
        self.service_name = 'opendj'
        self.pbar_text = "Installing OpenDJ"
        self.needdb = False # we don't need backend connection in this class
        self.app_type = AppType.SERVICE
        self.install_type = InstallOption.OPTONAL
        self.install_var = 'opendj_install'
        self.register_progess()
        self.ldap_str = 'ldap'

        self.openDjIndexJson = os.path.join(Config.install_dir, 'static/opendj/index.json')
        self.openDjSchemaFolder = os.path.join(Config.ldap_base_dir, 'config/schema')

        self.unit_file = os.path.join(Config.install_dir, 'static/opendj/systemd/opendj.service')
        self.ldapDsconfigCommand = os.path.join(Config.ldap_bin_dir , 'dsconfig')
        self.ldapDsCreateRcCommand = os.path.join(Config.ldap_bin_dir , 'create-rc-script')

    def install(self):
        self.logIt("-------------------------------------------------- >>")
        self.logIt("Running OpenDJ Setup")

        if Config.profile == SetupProfiles.DISA_STIG:
            self.fips_provider = 'org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider'
            self.provider_path = '{}:{}'.format(Config.bc_fips_jar, Config.bcpkix_fips_jar)
            self.admin_alias = 'admin-cert'
            self.pass_param = '-storepass:file'
        
        if Config.profile == SetupProfiles.DISA_STIG:
            self.logIt("Config.opendj_truststore_format.lower() (init)  = %s" % Config.opendj_truststore_format.lower())
            self.logIt("Config.ldap_setup_properties (init)(1)  = %s" % Config.ldap_setup_properties)
            Config.ldap_setup_properties += '.' + Config.opendj_truststore_format.lower()
            self.logIt("Config.ldap_setup_properties (init)(2)  = %s" % Config.ldap_setup_properties)
            
        self.opendj_trusstore_setup_key_fn = os.path.join(Config.output_dir, 'opendj.keystore.pin')
        
        self.opendj_pck11_setup_key_fn = '/root/.keystore.pin'
        self.opendj_admin_truststore_fn = os.path.join(Config.ldap_base_dir, 'config', 'admin-truststore')
        self.opendj_key_store_password_fn = os.path.join(Config.ldap_base_dir, 'config', 'keystore.pin')

        self.logIt("Config.profile                          = %s" % Config.profile)

        self.logIt("Config.opendj_truststore_format.lower() = %s" % Config.opendj_truststore_format.lower())
        self.logIt("Config.ldap_setup_properties            = %s" % Config.ldap_setup_properties)
        self.logIt("self.opendj_trusstore_setup_key_fn      = %s" % self.opendj_trusstore_setup_key_fn)
        self.logIt("self.opendj_pck11_setup_key_fn          = %s" % self.opendj_pck11_setup_key_fn)
        self.logIt("self.opendj_admin_truststore_fn         = %s" % self.opendj_admin_truststore_fn)
        self.logIt("self.opendj_key_store_password_fn       = %s" % self.opendj_key_store_password_fn)

        if Config.profile == SetupProfiles.DISA_STIG:
            self.logIt("Config.bc_fips_jar          = %s" % Config.bc_fips_jar)
            self.logIt("Config.bcpkix_fips_jar      = %s" % Config.bcpkix_fips_jar)

            self.logIt("self.fips_provider          = %s" % self.fips_provider)
            self.logIt("self.provider_path          = %s" % self.provider_path)
            self.logIt("self.admin_alias            = %s" % self.admin_alias)
            self.logIt("self.pass_param             = %s" % self.pass_param)

        if not base.snap:
            Config.pbar.progress(self.service_name, "Extracting OpenDJ", False)
            self.extractOpenDJ()

        self.createLdapPw()

        Config.pbar.progress(self.service_name, "Installing OpenDJ", False)
        self.logIt("Installing OpenDJ")
        if Config.opendj_install == InstallTypes.LOCAL:
            self.install_opendj()
            Config.pbar.progress(self.service_name, "Setting up OpenDJ service", False)
            self.logIt("Setting up OpenDJ service")
            self.setup_opendj_service()
            Config.pbar.progress(self.service_name, "Preparing OpenDJ schema", False)
            self.logIt("Preparing OpenDJ schema")
            self.prepare_opendj_schema()

        # it is time to bind OpenDJ
        for i in range(1, 5):
            time.sleep(i*2)
            try:
                self.dbUtils.bind()
                self.logIt("LDAP Connection was successful")
                break
            except ldap3.core.exceptions.LDAPSocketOpenError:
                self.logIt("Failed to connect LDAP. Trying once more")
        else:
            self.logIt("Four attempt to connection to LDAP failed. Exiting ...", True, True)

        # it is time to bind OpenDJ
#        self.dbUtils.bind()

        if Config.opendj_install:
            Config.pbar.progress(self.service_name, "Creating OpenDJ backends", False)
            self.logIt("Creating OpenDJ backends")
            self.create_backends()
            Config.pbar.progress(self.service_name, "Configuring OpenDJ", False)
            self.logIt("Configuring OpenDJ")
            self.configure_opendj()
            Config.pbar.progress(self.service_name, "Exporting OpenDJ certificate", False)
            self.logIt("Exporting OpenDJ certificate")
            self.export_opendj_public_cert()
            Config.pbar.progress(self.service_name, "Creating OpenDJ indexes", False)
            self.logIt("Creating OpenDJ indexes")            
            self.index_opendj()

            ldif_files = []

            if Config.mapping_locations['default'] == self.ldap_str:
                ldif_files += Config.couchbaseBucketDict['default']['ldif']

            ldap_mappings = self.getMappingType(self.ldap_str)

            for group in ldap_mappings:
                ldif_files +=  Config.couchbaseBucketDict[group]['ldif']

            Config.pbar.progress(self.service_name, "Importing ldif files to OpenDJ", False)
            self.logIt("Importing ldif files to OpenDJ")
            
            if not Config.ldif_base in ldif_files:
                self.dbUtils.import_ldif([Config.ldif_base], force=BackendTypes.LDAP)

            self.dbUtils.import_ldif(ldif_files)

            Config.pbar.progress(self.service_name, "OpenDJ post installation", False)
            self.logIt("OpenDJ post installation")
            
            if Config.opendj_install == InstallTypes.LOCAL:
                self.post_install_opendj()

            self.enable()
            
        self.logIt("-------------------------------------------------- <<")

    def extractOpenDJ(self):

        opendj_archive = max(glob.glob(os.path.join(Config.dist_app_dir, 'opendj-server-*4*.zip')))

        try:
            self.logIt("Unzipping %s in /opt/" % opendj_archive)
            self.run([paths.cmd_unzip, '-n', '-q', '%s' % (opendj_archive), '-d', '/opt/' ])
        except:
            self.logIt("Error encountered while doing unzip %s -d /opt/" % (opendj_archive))

        real_ldap_base_dir = os.path.realpath(Config.ldap_base_dir)
        self.chown(real_ldap_base_dir, Config.ldap_user, Config.ldap_group, recursive=True)

        if Config.opendj_install == InstallTypes.REMOTE:
            self.run([paths.cmd_ln, '-s', '/opt/opendj/template/config/', '/opt/opendj/config'])

    def create_user(self):
        self.createUser(Config.ldap_user, Config.ldap_user_home)
        self.addUserToGroup('jans', Config.ldap_group)
        self.addUserToGroup('adm', Config.ldap_group)

    def install_opendj(self):
        self.logIt("Running OpenDJ Setup")

#   Copy opendj-setup.properties so user ldap can find it in /opt/opendj
#   setup_props_fn = os.path.join(Config.ldap_base_dir, 'opendj-setup.properties')
#   shutil.copy("%s/opendj-setup.properties" % Config.output_dir, setup_props_fn)

        self.logIt("Config.templateRenderingDict = {}".format(Config.templateRenderingDict))

        Config.templateRenderingDict['opendj_pck11_setup_key_fn'] = self.opendj_pck11_setup_key_fn
        Config.templateRenderingDict['opendj_trusstore_setup_key_fn'] = self.opendj_trusstore_setup_key_fn

        self.renderTemplateInOut(Config.ldap_setup_properties, Config.templateFolder, Config.output_dir)
        
        ldap_setup_properties_dir, ldap_setup_properties_fn = os.path.split(Config.ldap_setup_properties)
        
        setup_props_output_fn = os.path.join(Config.output_dir, ldap_setup_properties_fn)
        setup_props_ldap_fn = os.path.join(Config.ldap_base_dir, ldap_setup_properties_fn)
        
        self.logIt("self.opendj_pck11_setup_key_fn = {}".format(self.opendj_pck11_setup_key_fn))
        self.logIt("self.opendj_trusstore_setup_key_fn = {}".format(self.opendj_trusstore_setup_key_fn))
        
        self.logIt("Config.ldap_setup_properties = {}".format(Config.ldap_setup_properties))
        
        self.logIt("Config.output_dir = {}".format(Config.output_dir))
        self.logIt("Config.ldap_base_dir = {}".format(Config.ldap_base_dir))        
        
        self.logIt("setup_props_output_fn = {}".format(setup_props_output_fn))
        self.logIt("setup_props_ldap_fn = {}".format(setup_props_ldap_fn))

        self.logIt("Config.opendj_trust_store_fn = {}".format(Config.opendj_trust_store_fn))
        
        self.logIt("Config.ldap_setup_properties = {}".format(Config.ldap_setup_properties))
        
        self.logIt("Config.certFolder = {}".format(Config.certFolder))

        self.chown(setup_props_output_fn, Config.ldap_user, Config.ldap_group)

        shutil.copy(setup_props_output_fn, setup_props_ldap_fn)
        
        self.chown(setup_props_ldap_fn, Config.ldap_user, Config.ldap_group)

        if Config.profile == SetupProfiles.DISA_STIG:
            self.generate_opendj_certs()

#        some_command = "touch"

#        some_command_exec = [some_command,
#                                './some_file.dat'
#                                ]

#        self.logIt("Running: {}".format(some_command_exec))
#        self.run_1(some_command_exec,
#                  useWait=True
#                  )

#        self.logIt("After Running: {}".format(some_command_exec))                  

#        some_command = "./_init.sh"

#        some_command_exec = [some_command,
#                                '/opt/opendj/some_file.1.dat',
#                                '/opt/opendj/some_file.2.dat'
#                                ]

#        self.logIt("Running: {}".format(some_command_exec))
#        self.run_1(some_command_exec,
#                  useWait=False
#                  )

#        self.logIt("After Running: {}".format(some_command_exec))

#        self.logIt("Before Reading file: {}".format('/opt/opendj/some_file.2.dat'))        

#        result_reading = self.readFile('/opt/opendj/some_file.2.dat')

#        self.logIt("result_reading = {}".format(result_reading))

        ldap_setup_command = os.path.join(Config.ldap_base_dir, 'setup')

        setup_cmd = [ldap_setup_command,
                                '--no-prompt',
                                '--cli',
                                '--propertiesFilePath',
                                setup_props_ldap_fn,
                                '--acceptLicense',
                                '--doNotStart']
#        if base.snap:
#            self.run(setup_cmd, shell=True)
#        else:
#            self.run(['/bin/su',
#                          Config.ldap_user,
#                          '-c',
#                          setup_cmd],
#                          cwd='/opt/opendj',
#                      )

        self.logIt("Running: {}".format(setup_cmd))
        self.run_1(setup_cmd,
                  cwd='/opt/opendj',
                  env={'OPENDJ_JAVA_HOME': Config.jre_home},
                  useWait=True
                  )
                  
        self.logIt("post_setup_import...")
        self.post_setup_import()

        self.logIt("fix_opendj_java_properties...")
        self.fix_opendj_java_properties()

        self.logIt("set_opendj_java_properties...")
        self.set_opendj_java_properties({
            'default.java-home': Config.jre_home,
            'start-ds.java-args': '-server -Xms{0}m -Xmx{0}m'.format(Config.opendj_max_ram),
            })

        if Config.profile == SetupProfiles.DISA_STIG:
            self.fix_opendj_config()
            opendj_fapolicyd_rules = [
                    fapolicyd_rule_tmp.format(Config.ldap_user, Config.jre_home),
                    fapolicyd_rule_tmp.format(Config.ldap_user, Config.ldap_base_dir),
                    '# give access to opendj server',
                    ]

            self.apply_fapolicyd_rules(opendj_fapolicyd_rules)

        if Config.profile == SetupProfiles.DISA_STIG:
            # Restore SELinux Context
            self.run(['restorecon', '-rv', os.path.join(Config.ldap_base_dir, 'bin')])

        self.chown(Config.certFolder, Config.root_user, Config.jans_group)
        if os.path.exists(Config.opendj_trust_store_fn):
            self.chown(Config.opendj_trust_store_fn,  Config.root_user, Config.jans_group)
            self.run([paths.cmd_chmod, '660', Config.opendj_trust_store_fn])

        try:
            self.logIt('Stopping opendj server')
            cmd = os.path.join(Config.ldap_bin_dir , 'stop-ds')
            self.run(cmd, shell=True)
        except:
            self.logIt("Error stopping opendj", True)

    def set_opendj_java_properties(self, data):
        
        self.logIt("Setting OpenDJ params: {}".format(str(data)))

        opendj_java_properties_fn = os.path.join(Config.ldap_base_dir, 'config/java.properties')
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
        ldap_setup_properties_dir, ldap_setup_properties_fn = os.path.split(Config.ldap_setup_properties)
        setup_props_ldap_fn = os.path.join(Config.ldap_base_dir, ldap_setup_properties_fn)
        self.logIt("Removing: {}".format(setup_props_ldap_fn))
        try:
            os.remove(setup_props_ldap_fn)
        except:
            self.logIt("Error deleting OpenDJ properties. Make sure {} is deleted".format(setup_props_ldap_fn))

    def create_backends(self):
        backends = [
                    ['create-backend', '--backend-name', 'metric', '--set', 'base-dn:o=metric', '--type %s' % Config.ldap_backend_type, '--set', 'enabled:true', '--set', 'db-cache-percent:20'],
                    ]

        if Config.mapping_locations['site'] == 'ldap':
            backends.append(['create-backend', '--backend-name', 'site', '--set', 'base-dn:o=site', '--type %s' % Config.ldap_backend_type, '--set', 'enabled:true', '--set', 'db-cache-percent:20'])

        if Config.profile == SetupProfiles.DISA_STIG:
            dsconfig_cmd = [
                            self.ldapDsconfigCommand,
                            '--no-prompt',
                            '--hostname',
                            Config.ldap_hostname,
                            '--port',
                            Config.ldap_admin_port,
                            '--bindDN',
                            '"%s"' % Config.ldap_binddn,
                            '--bindPasswordFile', Config.ldapPassFn,
                            ]
            if Config.opendj_truststore_format.upper() == 'PKCS11':
                dsconfig_cmd += [
                            '--trustStorePath', self.opendj_admin_truststore_fn,
                            '--keyStorePassword', self.opendj_key_store_password_fn,
                            ]
        else:
            dsconfig_cmd = [
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
                            Config.ldapPassFn
                            ]

        self.logIt("Checking if LDAP admin interface is ready")
        ldap_server = ldap3.Server(Config.ldap_hostname, port=int(Config.ldap_admin_port), use_ssl=True) 
        ldap_conn = ldap3.Connection(ldap_server, user=Config.ldap_binddn, password=Config.ldapPass)
        for i in range(1, 5):
            time.sleep(i*2)
            try:
                ldap_conn.bind()
                break
            except ldap3.core.exceptions.LDAPSocketOpenError:
                self.logIt("Failed to connect LDAP admin port. Trying once more")
        else:
            self.logIt("Four attempt to connection to LDAP admin port failed. Exiting ...", True, True)

        for changes in backends:
            cwd = os.path.join(Config.ldap_bin_dir)
            self.run(' '.join(dsconfig_cmd + changes), shell=True, cwd=cwd, env={'OPENDJ_JAVA_HOME': Config.jre_home})

        # rebind after creating backends
        self.dbUtils.ldap_conn.unbind()
        self.dbUtils.ldap_conn.bind()

    def create_backends_src(self):
        backends = [
                    ['create-backend', '--backend-name', 'metric', '--set', 'base-dn:o=metric', '--type %s' % Config.ldap_backend_type, '--set', 'enabled:true', '--set', 'db-cache-percent:20'],
                    ]

        if Config.mapping_locations['site'] == self.ldap_str:
            backends.append(['create-backend', '--backend-name', 'site', '--set', 'base-dn:o=site', '--type %s' % Config.ldap_backend_type, '--set', 'enabled:true', '--set', 'db-cache-percent:20'])

        for changes in backends:
            cwd = os.path.join(Config.ldap_bin_dir )

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
                      Config.ldap_user,
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

        if (not Config.listenAllInterfaces) and (Config.opendj_install == InstallTypes.LOCAL):
            opendj_config.append(('cn=LDAPS Connection Handler,cn=Connection Handlers,cn=config', 'ds-cfg-listen-address', '127.0.0.1', ldap3.MODIFY_REPLACE))
            opendj_config.append(('cn=Administration Connector,cn=config', 'ds-cfg-listen-address', '127.0.0.1', ldap3.MODIFY_REPLACE))

        for dn, attr, val, change_type in opendj_config:
            self.logIt("Changing OpenDJ Configuration for {}".format(dn))
            self.dbUtils.ldap_conn.modify(
                    dn, 
                     {attr: [change_type, val]}
                    )
        #Create uniqueness for attrbiutes
        for attr, cn in (('mail', 'Unique mail address'), ('uid', 'Unique uid entry')):
            self.logIt("Creating OpenDJ uniqueness for {}".format(attr))
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

        if Config.profile != SetupProfiles.DISA_STIG:

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
                      Config.opendj_trust_store_fn,
                      '-storetype',
                      Config.opendj_truststore_format.upper(),
                      '-storepass',
                      Config.opendj_truststore_pass
                      ])

        # Import OpenDJ certificate into java truststore
        self.logIt("Import OpenDJ certificate")
        
        alias = '{}_opendj'.format(Config.hostname)        
        self.delete_key(alias)  
        self.import_cert_to_java_truststore(alias, Config.opendj_cert_fn)
        
#        self.run([Config.cmd_keytool, "-import", "-trustcacerts", "-alias", alias, \
#                  "-file", Config.opendj_cert_fn, "-keystore", Config.default_trust_store_fn, \
#                  "-storepass", "changeit", "-noprompt"])

    def index_opendj(self):

        self.logIt("Creating OpenDJ Indexes")

        with open(self.openDjIndexJson) as f:
            index_json = json.load(f)

        index_backends = ['userRoot']

#        if Config.mapping_locations['site'] == self.ldap_str:
#            index_backends.append('site')

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
        sys.path.append(os.path.join(Config.install_dir, 'schema'))
        import manager as schemaManager

        self.logIt("Creating OpenDJ schema")

        json_files =  glob.glob(os.path.join(Config.install_dir, 'schema/*.json'))
        for jsf in json_files:
            data = base.readJsonFile(jsf)
            if 'schemaFile' in data:
                out_file = os.path.join(Config.install_dir, 'static/opendj', data['schemaFile'])
                schemaManager.generate(jsf, 'opendj', out_file)

        opendj_schema_files = glob.glob(os.path.join(Config.install_dir, 'static/opendj/*.ldif'))
        for schema_file in opendj_schema_files:
            self.copyFile(schema_file, self.openDjSchemaFolder)

        self.run([paths.cmd_chmod, '-R', 'a+rX', Config.ldap_base_dir])
        self.chown(Config.ldap_base_dir, Config.ldap_user, Config.ldap_group, recursive=True)

        self.logIt("Re-starting OpenDj after schema update")
        self.stop()
        self.start()

    def setup_opendj_service(self):
        self.copyFile(self.unit_file, Config.unit_files_path)
        self.reload_daemon()

    def installed(self):
        if os.path.exists(self.openDjSchemaFolder):
            opendj_install = InstallTypes.LOCAL
        elif not os.path.exists(self.openDjSchemaFolder) and os.path.exists(Config.ox_ldap_properties):
            opendj_install = InstallTypes.REMOTE
#        elif os.path.exists(Config.opendj_p12_fn):
#            opendj_install = InstallTypes.REMOTE
        else:
            opendj_install = 0

        return opendj_install

    def generate_opendj_certs(self):
    
        self.logIt("generate_opendj_certs: -------------------------------------------------- >>")

        self.logIt("self.opendj_trusstore_setup_key_fn = %s" % self.opendj_trusstore_setup_key_fn)
#        self.logIt("Config.opendj_p12_pass = %s" % Config.opendj_p12_pass)
        self.logIt("Config.opendj_truststore_pass = %s" % Config.opendj_truststore_pass)

        self.writeFile(self.opendj_trusstore_setup_key_fn, Config.opendj_truststore_pass)
#        self.writeFile(self.opendj_trusstore_setup_key_fn, Config.opendj_p12_pass)

        keystore = Config.opendj_trust_store_fn if Config.opendj_truststore_format.upper() == 'BCFKS' else 'NONE'

        self.logIt("keystore = %s" % keystore)

        self.logIt("self.fips_provider = %s" % self.fips_provider)
        self.logIt("self.provider_path = %s" % self.provider_path)
        self.logIt("self.opendj_trusstore_setup_key_fn = %s" % self.opendj_trusstore_setup_key_fn)
        self.logIt("self.pass_param    = %s" % self.pass_param)
        self.logIt("Config.opendj_truststore_format.upper()    = %s" % Config.opendj_truststore_format.upper())

        # Generate keystore

        cmd_server_cert_gen = [
            Config.cmd_keytool, '-genkey',
            '-alias', 'server-cert',
            '-keyalg', 'rsa',
            '-dname', 'CN={},O=OpenDJ RSA Self-Signed Certificate'.format(Config.hostname),
            '-keystore', keystore,
            '-storetype', Config.opendj_truststore_format.upper(),
            '-validity', '3650',
            ]

        if Config.opendj_truststore_format.upper() == 'PKCS11':
            cmd_server_cert_gen += [
                '-storepass', 'changeit',
                   ]
        else:
            cmd_server_cert_gen += [
                 '-providername', 'BCFIPS',
                 '-provider', self.fips_provider,
                 '-providerpath',  self.provider_path,
                 '-keypass:file', self.opendj_trusstore_setup_key_fn,
                 self.pass_param, self.opendj_trusstore_setup_key_fn,
                 '-keysize', '2048',
                 '-sigalg', 'SHA256WITHRSA',
                    ]

        self.run(cmd_server_cert_gen)


        cmd_server_selfcert_gen = [
            Config.cmd_keytool, '-selfcert',
            '-alias', 'server-cert',
            '-keystore', keystore,
            '-storetype', Config.opendj_truststore_format.upper(),
            '-validity', '3650',
            ]

        if Config.opendj_truststore_format.upper() == 'PKCS11':
            cmd_server_selfcert_gen += [
                '-storepass', 'changeit'
                ]

        else:
            cmd_server_selfcert_gen += [
                '-providername', 'BCFIPS',
                '-provider', self.fips_provider,
                '-providerpath', self.provider_path,
                self.pass_param, self.opendj_trusstore_setup_key_fn,
                ]

        self.run(cmd_server_selfcert_gen)


        cmd_admin_cert_gen = [
                Config.cmd_keytool, '-genkey', 
                '-alias', self.admin_alias, 
                '-keyalg', 'rsa', 
                '-dname', 'CN={},O=Administration Connector RSA Self-Signed Certificate'.format(Config.hostname), 
                '-keystore', keystore, 
                '-storetype', Config.opendj_truststore_format.upper(),
                '-validity', '3650',
                ]


        if Config.opendj_truststore_format.upper() == 'PKCS11':
            cmd_admin_cert_gen += [
                '-storepass', 'changeit',
                   ]
        else:
            cmd_admin_cert_gen += [
                 '-providername', 'BCFIPS',
                 '-provider', self.fips_provider,
                 '-providerpath',  self.provider_path,
                 '-keypass:file', self.opendj_trusstore_setup_key_fn,
                 self.pass_param, self.opendj_trusstore_setup_key_fn,
                 '-keysize', '2048',
                 '-sigalg', 'SHA256WITHRSA',
                    ]
        self.run(cmd_admin_cert_gen)

        cmd_admin_selfcert_gen = [
                Config.cmd_keytool, '-selfcert',
                '-alias', self.admin_alias,
                '-keystore', keystore,
                '-storetype', Config.opendj_truststore_format.upper(),
                '-validity', '3650',
                ]

        if Config.opendj_truststore_format.upper() == 'PKCS11':
            cmd_admin_selfcert_gen += [
                '-storepass', 'changeit'
                ]

        else:
            cmd_admin_selfcert_gen += [
                '-providername', 'BCFIPS',
                '-provider', self.fips_provider,
                '-providerpath', self.provider_path,
                self.pass_param, self.opendj_trusstore_setup_key_fn,
                ]

        self.run(cmd_admin_selfcert_gen)
        
        self.logIt("generate_opendj_certs: -------------------------------------------------- <<")

    def post_setup_import(self):
        if Config.profile == SetupProfiles.DISA_STIG and Config.opendj_truststore_format.upper() == 'BCFKS':
            self.run([Config.cmd_keytool, '-importkeystore',
                    '-destkeystore', 'NONE',
                    '-deststoretype', 'PKCS11',
                    '-deststorepass', 'changeit',
                    '-srckeystore', '/opt/opendj/config/truststore',
                    '-srcstoretype', 'JKS',
                    '-srcstorepass:file', '/opt/opendj/config/keystore.pin',
                    '-noprompt'
                    ])

    def fix_opendj_config(self):
        if Config.opendj_truststore_format.upper() == 'PKCS11':
            src = os.path.join(Config.ldap_base_dir, 'config/truststore')
            dest = os.path.join(Config.ldap_base_dir, 'config/admin-truststore')
            if not os.path.exists(dest):
                self.run([paths.cmd_ln, '-s', src, dest])

        if Config.profile == SetupProfiles.DISA_STIG and Config.opendj_truststore_format.upper() == 'BCFKS':
            self.disa_stig_fixes()

    def disa_stig_fixes(self):
        self.logIt("Patching opendj config.ldif for BCFKS")

        opendj_admin_fn = os.path.join(Config.certFolder, 'opendj-admin.bcfks')

        self.copyFile(Config.opendj_trust_store_fn, opendj_admin_fn)
        self.run([paths.cmd_chmod, '660', opendj_admin_fn])
        self.chown(opendj_admin_fn, Config.root_user, Config.ldap_user)

        self.run([Config.cmd_keytool, '-delete',
                    '-alias', self.admin_alias,
                    '-storetype', Config.opendj_truststore_format.upper(),
                    '-providername', 'BCFIPS',
                    '-provider', self.fips_provider,
                    '-providerpath', self.provider_path,
                    '-keystore', Config.opendj_trust_store_fn,
                    self.pass_param, self.opendj_key_store_password_fn
                    ])

        self.run([Config.cmd_keytool, '-delete',
                    '-alias', 'server-cert',
                    '-storetype', Config.opendj_truststore_format.upper(),
                    '-providername', 'BCFIPS',
                    '-provider', self.fips_provider,
                    '-providerpath', self.provider_path,
                    '-keystore', opendj_admin_fn,
                    self.pass_param, self.opendj_key_store_password_fn
                    ])

        opendj_config_ldif_fn = os.path.join(Config.ldap_base_dir, 'config/config.ldif')

        parser = myLdifParser(opendj_config_ldif_fn)
        parser.parse()

        dsa_key = 'ds-cfg-key-store-file'
        dsa_val = '/etc/certs/opendj.bcfks'

        tmp_path = Path(opendj_config_ldif_fn + '.tmp')

        opendj_config_out = tmp_path.open('wb')
        ldif_writer = LDIFWriter(opendj_config_out, cols=10000)

        for dn, entry in parser.entries:
            if dn in ('cn=HTTP Connection Handler,cn=Connection Handlers,cn=config',
                      'cn=LDAP Connection Handler,cn=Connection Handlers,cn=config',
                      'cn=LDAPS Connection Handler,cn=Connection Handlers,cn=config'):

                if 'ds-cfg-ssl-cert-nickname' in entry and self.admin_alias in entry['ds-cfg-ssl-cert-nickname']:
                    entry['ds-cfg-ssl-cert-nickname'].remove(self.admin_alias)

            if dn == 'cn=Administration,cn=Key Manager Providers,cn=config' and dsa_key in entry:
                if dsa_val in entry[dsa_key]:
                    entry[dsa_key].remove(dsa_val)
                entry[dsa_key].append('/etc/certs/opendj-admin.bcfks')

            ldif_writer.unparse(dn, entry)

        opendj_config_out.close()
        tmp_path.rename(opendj_config_ldif_fn)

    def fix_opendj_java_properties(self):

        #Set memory and default.java-home in java.properties   
        opendj_java_properties_fn = os.path.join(Config.ldap_base_dir, 'config/java.properties')

        self.logIt("Setting memory and default.java-home in %s" % opendj_java_properties_fn)
        opendj_java_properties = self.readFile(opendj_java_properties_fn).splitlines()
        java_home_ln = 'default.java-home={}'.format(Config.jre_home)
        java_home_ln_w = False

        for i, l in enumerate(opendj_java_properties[:]):
            n = l.find('=')
            if n > -1:
                k = l[:n].strip()
                if k == 'default.java-home':
                    opendj_java_properties[i] = java_home_ln
                    java_home_ln_w = True
                if k == 'start-ds.java-args':
                    if os.environ.get('ce_ldap_xms') and os.environ.get('ce_ldap_xmx'):
                        opendj_java_properties[i] = 'start-ds.java-args=-server -Xms{}m -Xmx{}m -XX:+UseCompressedOops'.format(os.environ['ce_ldap_xms'], os.environ['ce_ldap_xmx'])

        if not java_home_ln_w:
            opendj_java_properties.append(java_home_ln)

        self.writeFile(opendj_java_properties_fn, '\n'.join(opendj_java_properties))

