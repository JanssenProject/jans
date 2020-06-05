import re
import os
import json

from collections import OrderedDict

from setup_app.paths import DATA_DIR, INSTALL_DIR
from setup_app.static import InstallTypes
from setup_app.utils.printVersion import get_war_info
from setup_app.utils import base

class Config:

    @classmethod
    def get(self, attr, default=None):
        return getattr(self, attr) if hasattr(self, attr) else default


    @classmethod
    def determine_version(self):
        oxauth_info = get_war_info(os.path.join(self.distGluuFolder, 'oxauth.war'))
        self.oxVersion = oxauth_info['version']
        self.currentGluuVersion = re.search('([\d.]+)', oxauth_info['version']).group().strip('.')
        self.githubBranchName = oxauth_info['branch']

        self.ce_setup_zip = 'https://github.com/GluuFederation/community-edition-setup/archive/%s.zip' % self.githubBranchName

    @classmethod
    def init(self, install_dir=INSTALL_DIR):

        self.install_dir = install_dir
        self.thread_queue = None

        #create dummy progress bar in case not defined
        class DummyProgressBar:
            def __init__(self, *args):
                 pass

            def complete(self, *args):
                pass

            def progress(self, *args):
                pass

        self.pbar = DummyProgressBar()

        self.properties_password = None
        self.noPrompt = False

        self.distFolder = '/opt/dist'
        self.distAppFolder = os.path.join(self.distFolder, 'app')
        self.distGluuFolder = os.path.join(self.distFolder, 'gluu')
        self.distTmpFolder = os.path.join(self.distFolder, 'tmp')

        self.downloadWars = None
        self.templateRenderingDict = {
                                        'oxauthClient_2_inum': 'AB77-1A2B',
                                        'oxauthClient_3_inum': '3E20',
                                        'oxauthClient_4_inum': 'FF81-2D39',
                                        'idp_attribute_resolver_ldap.search_filter': '(|(uid=$requestContext.principalName)(mail=$requestContext.principalName))',
                                        'oxd_port': '8443',
                                     }

        # java commands
        self.jre_home = '/opt/jre'
        self.cmd_java = os.path.join(self.jre_home, 'bin/java')
        self.cmd_keytool = os.path.join(self.jre_home, 'bin/keytool')
        self.cmd_jar = os.path.join(self.jre_home, 'bin/jar')
        os.environ['OPENDJ_JAVA_HOME'] =  self.jre_home

        # Component ithversions
        self.apache_version = None
        self.opendj_version = None

        #passwords
        self.ldapPass = None
        self.oxtrust_admin_password = None
        self.encoded_admin_password = ''
        self.cb_password = None
        self.encoded_cb_password = ''

        #DB installation types
        self.wrends_install = InstallTypes.LOCAL
        self.cb_install = InstallTypes.NONE

        # Gluu components installation status
        self.loadData = True
        self.installOxAuth = True
        self.installOxTrust = True
        self.installHttpd = True
        self.installSaml = False
        self.installOxAuthRP = False
        self.installPassport = False
        self.installGluuRadius = False
        self.installScimServer = False
        self.installFido2 = False

        self.scimTestMode = 'false'

        self.enable_scim_access_policy = 'false'
        
        self.allowPreReleasedFeatures = False

        # backward compatibility
        self.os_type = base.os_type
        self.os_version = base.os_version
        self.os_initdaemon = base.os_initdaemon

        self.persistence_type = 'ldap'

        self.setup_properties_fn = os.path.join(self.install_dir, 'setup.properties')
        self.savedProperties = os.path.join(self.install_dir, 'setup.properties.last')

        self.gluuOptFolder = '/opt/gluu'
        self.gluuOptBinFolder = os.path.join(self.gluuOptFolder, 'bin')
        self.gluuOptSystemFolder = os.path.join(self.gluuOptFolder, 'system')
        self.gluuOptPythonFolder = os.path.join(self.gluuOptFolder, 'python')
        self.gluuBaseFolder = '/etc/gluu'
        self.configFolder = os.path.join(self.gluuBaseFolder, 'conf') 
        self.certFolder = '/etc/certs'
        
        self.gluu_properties_fn = os.path.join(self.configFolder,'gluu.properties')
        self.gluu_hybrid_roperties = os.path.join(self.configFolder, 'gluu-hybrid.properties')

        self.oxBaseDataFolder = '/var/gluu'
        self.cache_provider_type = 'NATIVE_PERSISTENCE'

        self.etc_hosts = '/etc/hosts'
        self.etc_hostname = '/etc/hostname'
        # OS /etc/default folder
        self.osDefault = '/etc/default'
        self.sysemProfile = '/etc/profile'
        self.node_home = '/opt/node'
        self.jython_home = '/opt/jython'

        self.java_type = 'jre'

        self.jetty_home = '/opt/jetty'
        self.jetty_base = os.path.join(self.gluuOptFolder, 'jetty')
        self.jetty_user_home = '/home/jetty'
        self.jetty_user_home_lib = os.path.join(self.jetty_user_home, 'lib')

        with open(os.path.join(DATA_DIR, 'jetty_app_configuration.json')) as f:
            self.jetty_app_configuration = json.load(f, object_pairs_hook=OrderedDict)

        self.app_custom_changes = {
            'jetty' : {
                'name' : 'jetty',
                'files' : [
                    {
                        'path' : os.path.join(self.jetty_home, 'etc/webdefault.xml'),
                        'replace' : [
                            {
                                'pattern' : r'(\<param-name\>dirAllowed<\/param-name\>)(\s*)(\<param-value\>)true(\<\/param-value\>)',
                                'update' : r'\1\2\3false\4'
                            }
                        ]
                    },
                    {
                        'path' : os.path.join(self.jetty_home, 'etc/jetty.xml'),
                        'replace' : [
                            {
                                'pattern' : '<New id="DefaultHandler" class="org.eclipse.jetty.server.handler.DefaultHandler"/>',
                                'update' : '<New id="DefaultHandler" class="org.eclipse.jetty.server.handler.DefaultHandler">\n\t\t\t\t <Set name="showContexts">false</Set>\n\t\t\t </New>'
                            }
                        ]
                    }
                ]
            }
        }

        self.hostname = None
        self.ip = None
        self.orgName = None
        self.countryCode = None
        self.city = None
        self.state = None
        self.admin_email = None
        self.encoded_ox_ldap_pw = None
        self.application_max_ram = int(base.current_mem_size * .83 * 1000) # 83% of physical memory
        self.encode_salt = None
        self.admin_inum = None

        self.ldapBaseFolderldapPass = None

        self.idp_client_id = None
        self.idpClient_pw = None
        self.idpClient_encoded_pw = None

        self.outputFolder = os.path.join(self.install_dir, 'output')
        self.templateFolder = os.path.join(self.install_dir, 'templates')
        self.staticFolder = os.path.join(self.install_dir, 'static')

        self.ldif_idp = os.path.join(self.outputFolder, 'oxidp.ldif')

        self.extensionFolder = os.path.join(self.staticFolder, 'extension')


        self.httpdKeyPass = None
        self.httpdKeyFn = os.path.join(self.certFolder, 'httpd.key')
        self.httpdCertFn = os.path.join(self.certFolder, 'httpd.crt')

        self.ldapTrustStoreFn = None
        self.encoded_ldapTrustStorePass = None

        self.ldapCertFn = self.opendj_cert_fn = os.path.join(self.certFolder, 'opendj.crt')
        self.ldapTrustStoreFn = self.opendj_p12_fn = os.path.join(self.certFolder, 'opendj.pkcs12')
        
        self.opendj_p12_pass = None

        self.ldap_binddn = 'cn=directory manager'
        self.ldap_hostname = 'localhost'
        self.couchbase_hostname = 'localhost'
        self.ldap_port = '1389'
        self.ldaps_port = '1636'
        self.ldap_admin_port = '4444'
        self.ldapBaseFolder = '/opt/opendj'

        self.ldapSetupCommand = os.path.join(self.ldapBaseFolder, 'setup')
        self.ldapDsconfigCommand = os.path.join(self.ldapBaseFolder, 'bin/dsconfig')
        self.ldapDsCreateRcCommand = os.path.join(self.ldapBaseFolder, 'bin/create-rc-script')
        self.ldapDsJavaPropCommand = os.path.join(self.ldapBaseFolder, 'bin/dsjavaproperties')
        self.importLdifCommand = os.path.join(self.ldapBaseFolder, 'bin/import-ldif')
        self.ldapModifyCommand = os.path.join(self.ldapBaseFolder, 'bin/ldapmodify')
        self.loadLdifCommand = self.ldapModifyCommand

        self.ldap_user_home = '/home/ldap'
        self.ldapPassFn = os.path.join(self.ldap_user_home, '.pw')
        self.ldap_backend_type = 'je'

        self.gluuScriptFiles = [
                            os.path.join(self.install_dir, 'static/scripts/logmanager.sh'),
                            os.path.join(self.install_dir, 'static/scripts/testBind.py')
                            ]

        self.openDjIndexJson = os.path.join(self.install_dir, 'static/opendj/index.json')
        self.openDjSchemaFolder = os.path.join(self.ldapBaseFolder, 'config/schema')
        self.openDjschemaFiles = [
                            os.path.join(self.install_dir, 'static/opendj/96-eduperson.ldif'),
                            os.path.join(self.install_dir, 'static/opendj/101-ox.ldif'),
                            os.path.join(self.install_dir, 'static/opendj/77-customAttributes.ldif')
                            ]

        self.opendj_init_file = os.path.join(self.install_dir, 'static/opendj/opendj')
        self.opendj_service_centos7 = os.path.join(self.install_dir, 'static/opendj/systemd/opendj.service')

        self.redhat_services = ['httpd', 'rsyslog']
        self.debian_services = ['apache2', 'rsyslog']

        self.apache_start_script = '/etc/init.d/httpd'

        self.defaultTrustStoreFN = os.path.join(self.jre_home, 'jre/lib/security/cacerts')
        self.defaultTrustStorePW = 'changeit'


        # Stuff that gets rendered; filename is necessary. Full path should
        # reflect final path if the file must be copied after its rendered.
        
        self.oxidp_config_json = os.path.join(self.outputFolder, 'oxidp-config.json')
        self.gluu_python_readme = os.path.join(self.gluuOptPythonFolder, 'libs/python.txt')
        self.ox_ldap_properties = os.path.join(self.configFolder, 'gluu-ldap.properties')
        
        self.apache2_conf = os.path.join(self.outputFolder, 'httpd.conf')
        self.apache2_ssl_conf = os.path.join(self.outputFolder, 'https_gluu.conf')
        self.apache2_24_conf = os.path.join(self.outputFolder, 'httpd_2.4.conf')
        self.apache2_ssl_24_conf = os.path.join(self.outputFolder, 'https_gluu.conf')
        self.ldif_base = os.path.join(self.outputFolder, 'base.ldif')
        self.ldif_attributes = os.path.join(self.outputFolder, 'attributes.ldif')
        self.ldif_scopes = os.path.join(self.outputFolder, 'scopes.ldif')
        self.ldif_clients = os.path.join(self.outputFolder, 'clients.ldif')
        self.ldif_people = os.path.join(self.outputFolder, 'people.ldif')
        self.ldif_groups = os.path.join(self.outputFolder, 'groups.ldif')
        
        self.ldif_metric = os.path.join(self.staticFolder, 'metric/o_metric.ldif')
        self.ldif_site = os.path.join(self.install_dir, 'static/cache-refresh/o_site.ldif')
        self.ldif_scripts = os.path.join(self.outputFolder, 'scripts.ldif')
        self.ldif_configuration = os.path.join(self.outputFolder, 'configuration.ldif')
        self.ldif_scim = os.path.join(self.outputFolder, 'scim.ldif')
        self.ldif_scim_clients = os.path.join(self.outputFolder, 'scim_clients.ldif')


        self.encode_script = os.path.join(self.gluuOptFolder, 'bin/encode.py')
        self.network = '/etc/sysconfig/network'
        self.system_profile_update_init = os.path.join(self.outputFolder, 'system_profile_init')
        self.system_profile_update_systemd = os.path.join(self.outputFolder, 'system_profile_systemd')

        ### rsyslog file customised for init.d
        self.rsyslogUbuntuInitFile = os.path.join(self.install_dir, 'static/system/ubuntu/rsyslog')
        self.ldap_setup_properties = os.path.join(self.templateFolder, 'opendj-setup.properties')


        # OpenID key generation default setting
        self.default_openid_jks_dn_name = 'CN=oxAuth CA Certificates'
        self.default_key_algs = 'RS256 RS384 RS512 ES256 ES384 ES512'
        self.default_key_expiration = 365

        # oxTrust SCIM configuration
        self.scim_rs_client_id = None
        self.scim_rs_client_jwks = None
        self.scim_rs_client_jks_fn = os.path.join(self.certFolder, 'scim-rs.jks')
        self.scim_rs_client_jks_pass = None
        self.scim_rs_client_jks_pass_encoded = None

        self.scim_rp_client_id = None
        self.scim_rp_client_jwks = None
        self.scim_rp_client_jks_fn = os.path.join(self.outputFolder, 'scim-rp.jks')
        self.scim_rp_client_jks_pass = 'secret'
        self.scim_resource_oxid = None


        self.post_messages = []


        #oxd install options
        self.installOxd = False
        self.oxd_server_https = ''
        self.oxd_package = base.determine_package(os.path.join(Config.distGluuFolder, 'oxd-server*.tgz'))
        self.oxd_use_gluu_storage = False
        self.generateOxdCertificate = False

        self.installCasa = False

        self.ldif_files = [self.ldif_base,
                           self.ldif_attributes,
                           self.ldif_scopes,
                           self.ldif_clients,
                           self.ldif_people,
                           self.ldif_groups,
                           self.ldif_site,
                           self.ldif_metric,
                           self.ldif_scripts,
                           self.ldif_configuration,
                           self.ldif_scim,
                           self.ldif_scim_clients,
                           self.ldif_idp,
                           ]


        self.ce_templates = {
                             self.gluu_python_readme: True,
                             self.oxidp_config_json: False,
                             self.ox_ldap_properties: True,
                             self.ldap_setup_properties: False,
                             self.apache2_conf: False,
                             self.apache2_ssl_conf: False,
                             self.apache2_24_conf: False,
                             self.apache2_ssl_24_conf: False,
                             self.etc_hostname: False,
                             self.ldif_base: False,
                             self.ldif_attributes: False,
                             self.ldif_scopes: False,
                             self.ldif_clients: False,
                             self.ldif_people: False,
                             self.ldif_groups: False,
                             self.ldif_scripts: False,
                             self.ldif_scim: False,
                             self.ldif_scim_clients: False,
                             self.ldif_idp: False,
                             self.network: False,
                             self.gluu_properties_fn: True,
                             }

        self.service_requirements = {
                        'opendj': ['', 70],
                        'oxauth': ['opendj', 72],
                        'fido2': ['opendj', 73],
                        'identity': ['opendj oxauth', 74],
                        'scim': ['opendj oxauth', 75],
                        'idp': ['opendj oxauth', 76],
                        'casa': ['opendj oxauth', 78],
                        'oxd-server': ['opendj oxauth', 80],
                        'passport': ['opendj oxauth', 82],
                        'oxauth-rp': ['opendj oxauth', 84],
                        'gluu-radius': ['opendj oxauth', 86],
                        }

        self.install_time_ldap = None


        self.couchbaseBucketDict = OrderedDict((
                        ('default', { 'ldif':[
                                            self.ldif_base, 
                                            self.ldif_attributes,
                                            self.ldif_scopes,
                                            self.ldif_scripts,
                                            self.ldif_configuration,
                                            self.ldif_scim,
                                            self.ldif_idp,
                                            self.ldif_clients,
                                            self.ldif_scim_clients,
                                            self.ldif_metric,
                                            ],
                                      'memory_allocation': 100,
                                      'mapping': '',
                                      'document_key_prefix': []
                                    }),

                        ('user',     {   'ldif': [
                                            self.ldif_people, 
                                            self.ldif_groups
                                            ],
                                        'memory_allocation': 300,
                                        'mapping': 'people, groups, authorizations',
                                        'document_key_prefix': ['groups_', 'people_', 'authorizations_'],
                                    }),

                        ('cache',    {   'ldif': [],
                                        'memory_allocation': 300,
                                        'mapping': 'cache',
                                        'document_key_prefix': ['cache_'],
                                    }),

                        ('site',     {   'ldif': [self.ldif_site],
                                        'memory_allocation': 100,
                                        'mapping': 'cache-refresh',
                                        'document_key_prefix': ['site_', 'cache-refresh_'],
                                        
                                    }),

                        ('token',   { 'ldif': [],
                                      'memory_allocation': 300,
                                      'mapping': 'tokens',
                                      'document_key_prefix': ['tokens_'],
                                    }),

                    ))

        self.mappingLocations = { group: 'ldap' for group in self.couchbaseBucketDict }  #default locations are OpenDJ
        self.non_setup_properties = {
            'oxauth_client_jar_fn': os.path.join(self.distGluuFolder, 'oxauth-client-jar-with-dependencies.jar')
                }
