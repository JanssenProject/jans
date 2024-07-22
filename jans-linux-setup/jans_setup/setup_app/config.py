import re
import os
import time
import pprint
import inspect
import json
from collections import OrderedDict

from setup_app.paths import INSTALL_DIR, LOG_DIR
from setup_app.static import InstallTypes
from setup_app.utils.printVersion import get_war_info
from setup_app.utils import base

OPENBANKING_PROFILE = 'openbanking'

class Config:

    # we define statics here so that is is acessible without construction
    opt_dir = '/opt'
    jansOptFolder = '/opt/jans'
    distFolder = '/opt/dist'
    jre_home = '/opt/jre'
    jansBaseFolder = '/etc/jans'
    certFolder = '/etc/certs'
    oxBaseDataFolder = '/var/jans'
    etc_hosts = '/etc/hosts'
    etc_hostname = '/etc/hostname'
    os_default = '/etc/default'
    sysemProfile = '/etc/profile'
    jython_home = '/opt/jython'
    ldap_base_dir = '/opt/opendj'
    network = '/etc/sysconfig/network'
    jetty_home = '/opt/jetty'
    node_home = '/opt/node'
    unit_files_path = '/etc/systemd/system'
    output_dir = None
    jetty_base = os.path.join(jansOptFolder, 'jetty')
    dist_app_dir = os.path.join(distFolder, 'app')
    dist_jans_dir = os.path.join(distFolder, 'jans')

    installed_instance = False


    @classmethod
    def get(self, attr, default=None):
        return getattr(self, attr) if hasattr(self, attr) else default

    @classmethod
    def set_mapping_locations(self):
        ptype = 'rdbm' if self.persistence_type in ('sql', 'spanner') else self.persistence_type
        self.mapping_locations = { group: ptype for group in self.couchbaseBucketDict }

    @classmethod
    def dump(self, dumpFile=False):
        if self.dump_config_on_error:
            return

        myDict = {}
        for obj_name, obj in inspect.getmembers(self):
            obj_name = str(obj_name)
            if not obj_name.startswith('__') and (not callable(obj)):
                myDict[obj_name] = obj

        if dumpFile:
            fn = os.path.join(self.install_dir, 'config-'+time.ctime().replace(' ', '-'))
            with open(fn, 'w') as w:
                w.write(pprint.pformat(myDict, indent=2))
        else:
            pp = pprint.PrettyPrinter(indent=2)
            pp.pprint(myDict)

    @classmethod
    def calculate_mem(self):
        if Config.opendj_install:
            self.opendj_max_ram = int(int(Config.jans_max_mem) * .2) # 20% of mem_use
            self.application_max_ram = int(int(Config.jans_max_mem) * .8) # 80% of mem_use
        else:
            self.opendj_max_ram = 0
            self.application_max_ram = int(Config.jans_max_mem)

    @classmethod
    def init(self, install_dir=INSTALL_DIR):

        self.install_dir = install_dir
        self.data_dir = os.path.join(self.install_dir, 'setup_app/data')
        self.profile = base.current_app.profile 

        self.thread_queue = None
        self.jetty_user = self.jetty_group = 'jetty'
        self.root_user = self.root_group = 'root'
        self.ldap_user = self.ldap_group = 'ldap'
        self.backend_service = 'network.target'
        self.dump_config_on_error = False

        if not self.output_dir:
            self.output_dir = os.path.join(install_dir, 'output')

        self.ldap_bin_dir = os.path.join(self.ldap_base_dir, 'bin')
        if base.snap:
            self.ldap_base_dir = os.path.join(base.snap_common, 'opendj')
            self.jetty_user = 'root'

        self.default_store_type = 'PKCS12'

        #create dummy progress bar that logs to file in case not defined
        progress_log_file = os.path.join(LOG_DIR, 'progress-bar.log')
        class DummyProgress:

            services = []

            def register(self, installer):
                pass

            def before_start(self):
                pass

            def start(self):
                pass

            def progress(self, service_name, msg, incr=False):
                with open(progress_log_file, 'a') as w:
                    w.write("{}: {}\n".format(service_name, msg))

        self.pbar = DummyProgress()

        self.properties_password = None
        self.noPrompt = False

        self.dist_app_dir = os.path.join(self.distFolder, 'app')
        self.dist_jans_dir = os.path.join(self.distFolder, 'jans')
        self.distTmpFolder = os.path.join(self.distFolder, 'tmp')

        self.downloadWars = None
        self.templateRenderingDict = {
                                        'jans_auth_client_2_inum': 'AB77-1A2B',
                                        'jans_auth_client_3_inum': '3E20',
                                        'jans_auth_client_4_inum': 'FF81-2D39',
                                        'idp_attribute_resolver_ldap.search_filter': '(|(uid=$requestContext.principalName)(mail=$requestContext.principalName))',
                                        'server_time_zone': 'UTC' + time.strftime("%z"),
                                     }

        # java commands
        self.cmd_java = os.path.join(self.jre_home, 'bin/java')
        self.cmd_keytool = os.path.join(self.jre_home, 'bin/keytool')
        self.cmd_jar = os.path.join(self.jre_home, 'bin/jar')
        os.environ['OPENDJ_JAVA_HOME'] = self.jre_home

        if self.profile == OPENBANKING_PROFILE:
            self.use_external_key = True
            self.ob_key_fn = ''
            self.ob_cert_fn = ''
            self.ob_alias = ''
            self.static_kid = ''
            self.jwks_uri = ''

        # Component ithversions
        self.apache_version = None
        self.opendj_version = None

        #passwords
        self.ldapPass = None
        self.admin_password = ''
        self.cb_password = None
        self.encoded_cb_password = ''

        #DB installation types
        self.opendj_install = InstallTypes.NONE
        self.cb_install = InstallTypes.NONE
        self.rdbm_install = InstallTypes.LOCAL

        self.couchbase_buckets = []

        #rdbm
        self.rdbm_install_type = InstallTypes.LOCAL
        self.rdbm_type = 'pgsql'
        self.rdbm_host = 'localhost'
        self.rdbm_port = 3306
        self.rdbm_db = 'jansdb'
        self.rdbm_user = 'jans'
        self.rdbm_password = None
        self.rdbm_password_enc = ''
        self.static_rdbm_dir = os.path.join(self.install_dir, 'static/rdbm')

        #spanner
        self.spanner_project = 'jans-project'
        self.spanner_instance = 'jans-instance'
        self.spanner_database = 'jansdb' 
        self.spanner_emulator_host = None
        self.google_application_credentials = None

        # Jans components installation status
        self.loadData = True
        self.install_jans = True
        self.install_jre = True
        self.install_jetty = True
        self.install_jython = True
        self.install_jans_auth = True
        self.install_httpd = True
        self.install_scim_server = True
        self.install_fido2 = True
        self.install_config_api = True
        self.install_casa = False
        self.install_jans_cli = True
        self.install_jans_link = False
        self.loadTestData = False
        self.allowPreReleasedFeatures = False
        self.install_jans_saml = False
        self.install_jans_keycloak_link = False
        self.install_jans_lock = False
        self.install_opa = False

        # backward compatibility
        self.os_type = base.os_type
        self.os_version = base.os_version
        self.os_initdaemon = base.os_initdaemon

        self.persistence_type = 'sql'

        self.setup_properties_fn = os.path.join(self.install_dir, 'setup.properties')
        self.savedProperties = os.path.join(self.install_dir, 'setup.properties.last')

        self.jansOptBinFolder = os.path.join(self.jansOptFolder, 'bin')
        self.jansOptSystemFolder = os.path.join(self.jansOptFolder, 'system')
        self.jansOptPythonFolder = os.path.join(self.jansOptFolder, 'python')
        self.configFolder = os.path.join(self.jansBaseFolder, 'conf') 

        self.salt_fn = os.path.join(self.configFolder,'salt')
        self.jans_properties_fn = os.path.join(self.configFolder,'jans.properties')
        self.jans_hybrid_roperties_fn = os.path.join(self.configFolder, 'jans-hybrid.properties')

        self.cache_provider_type = 'NATIVE_PERSISTENCE'

        self.java_type = 'jre'

        self.hostname = None
        self.ip = None
        self.orgName = None
        self.countryCode = None
        self.city = None
        self.state = None
        self.admin_email = None
        self.ldap_bind_encoded_pw = None
        self.encode_salt = None
        self.admin_inum = None

        self.jans_max_mem = int(base.current_mem_size * .85 * 1000) # 85% of physical memory
        self.calculate_mem()

        self.templateFolder = os.path.join(self.install_dir, 'templates')
        self.staticFolder = os.path.join(self.install_dir, 'static')

        self.extensionFolder = os.path.join(self.staticFolder, 'extension')
        self.script_catalog_dir = os.path.join(self.install_dir, 'script_catalog')

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

        self.ldap_user_home = self.ldap_base_dir
        self.ldapPassFn = os.path.join(self.ldap_user_home, '.pw')
        self.ldap_backend_type = 'je'

        self.jansScriptFiles = [
                            os.path.join(self.install_dir, 'static/scripts/logmanager.sh'),
                            os.path.join(self.install_dir, 'static/scripts/testBind.py'),
                            os.path.join(self.install_dir, 'static/scripts/jans'),
                            ]

        self.redhat_services = ['httpd', 'rsyslog']
        self.debian_services = ['apache2', 'rsyslog']

        self.defaultTrustStoreFN = os.path.join(self.jre_home, 'jre/lib/security/cacerts')
        self.defaultTrustStorePW = 'changeit'

        # Stuff that gets rendered; filename is necessary. Full path should
        # reflect final path if the file must be copied after its rendered.

        self.jans_python_readme = os.path.join(self.jansOptPythonFolder, 'libs/python.txt')
        self.ox_ldap_properties = os.path.join(self.configFolder, 'jans-ldap.properties')
        self.jansCouchebaseProperties = os.path.join(self.configFolder, 'jans-couchbase.properties')
        self.jansRDBMProperties = os.path.join(self.configFolder, 'jans-sql.properties')
        self.jansSpannerProperties = os.path.join(self.configFolder, 'jans-spanner.properties')

        self.ldif_base = os.path.join(self.output_dir, 'base.ldif')
        self.ldif_attributes = os.path.join(self.output_dir, 'attributes.ldif')
        self.ldif_scopes = os.path.join(self.output_dir, 'scopes.ldif')
        self.ldif_agama = os.path.join(self.output_dir, 'agama.ldif')

        self.ldif_metric = os.path.join(self.staticFolder, 'metric/o_metric.ldif')
        self.ldif_site = os.path.join(self.install_dir, 'static/site/site.ldif')
        self.ldif_configuration = os.path.join(self.output_dir, 'configuration.ldif')

        self.system_profile_update_init = os.path.join(self.output_dir, 'system_profile_init')
        self.system_profile_update_systemd = os.path.join(self.output_dir, 'system_profile_systemd')

        ### rsyslog file customised for init.d
        self.rsyslogUbuntuInitFile = os.path.join(self.install_dir, 'static/system/ubuntu/rsyslog')
        self.ldap_setup_properties = os.path.join(self.templateFolder, 'opendj-setup.properties')

        # OpenID key generation default setting
        self.default_openid_jks_dn_name = 'CN=Jans Auth CA Certificates'
        if self.profile == OPENBANKING_PROFILE:
            self.default_key_algs = 'RS256 RS384 RS512 ES256 ES384 ES512'
        else:
            self.default_sig_key_algs = 'RS256 RS384 RS512 ES256 ES256K ES384 ES512 PS256 PS384 PS512'

        self.default_enc_key_algs = 'RSA1_5 RSA-OAEP ECDH-ES'
        self.default_key_expiration = 365

        self.smtp_jks_fn = os.path.join(self.certFolder, 'smtp-keys.' + self.default_store_type.lower())
        self.smtp_alias = 'smtp_sig_ec256'
        self.smtp_signing_alg = 'SHA256withECDSA'

        self.post_messages = []

        self.ldif_files = [self.ldif_base,
                           self.ldif_attributes,
                           self.ldif_scopes,
                           self.ldif_site,
                           self.ldif_metric,
                           self.ldif_configuration,
                           self.ldif_agama,
                           ]


        self.ce_templates = {
                            self.jans_python_readme: True,
                             self.etc_hostname: False,
                             self.network: False,
                             self.jans_properties_fn: True,
                             self.ldif_base: False,
                             self.ldif_attributes: False,
                             self.ldif_scopes: False,
                             self.ldif_agama: False,
                             }

        if self.profile != OPENBANKING_PROFILE:
            self.ce_templates[self.ox_ldap_properties] = True
            self.ce_templates[self.ldap_setup_properties] = False


        self.service_requirements = {
                        'opendj': ['', 70],
                        'jans-auth': ['opendj', 72],
                        'jans-fido2': ['opendj', 73],
                        'identity': ['opendj jans-auth', 74],
                        'jans-scim': ['opendj jans-auth', 75],
                        'idp': ['opendj jans-auth', 76],
                        'casa': ['opendj jans-auth', 78],
                        'passport': ['opendj jans-auth', 82],
                        'jans-auth-rp': ['opendj jans-auth', 84],
                        'jans-config-api': ['opendj jans-auth', 85],
                        }

        self.install_time_ldap = None


        self.couchbaseBucketDict = OrderedDict((
                        ('default', { 'ldif':[
                                            self.ldif_base, 
                                            self.ldif_attributes,
                                            self.ldif_scopes,
                                            self.ldif_configuration,
                                            self.ldif_metric,
                                            self.ldif_agama,
                                            ],
                                      'memory_allocation': 100,
                                      'mapping': '',
                                      'document_key_prefix': []
                                    }),

                        ('user',     {   'ldif': [],
                                        'memory_allocation': 300,
                                        'mapping': 'people, groups, authorizations',
                                        'document_key_prefix': ['groups_', 'people_', 'authorizations_'],
                                    }),

                        ('site',     {   'ldif': [self.ldif_site],
                                        'memory_allocation': 100,
                                        'mapping': 'jans-link',
                                        'document_key_prefix': ['site_', 'jans-link_'],
                                    }),

                        ('cache',    {   'ldif': [],
                                        'memory_allocation': 100,
                                        'mapping': 'cache',
                                        'document_key_prefix': ['cache_'],
                                    }),

                        ('token',   { 'ldif': [],
                                      'memory_allocation': 300,
                                      'mapping': 'tokens',
                                      'document_key_prefix': ['tokens_'],
                                    }),

                        ('session',   { 'ldif': [],
                                      'memory_allocation': 200,
                                      'mapping': 'sessions',
                                      'document_key_prefix': [],
                                    }),

                    ))

        self.mapping_locations = { group: 'rdbm' for group in self.couchbaseBucketDict }

        self.non_setup_properties = {
            'jans_auth_client_jar_fn': os.path.join(self.dist_jans_dir, 'jans-auth-client-jar-with-dependencies.jar')
                }

        Config.addPostSetupService = []
