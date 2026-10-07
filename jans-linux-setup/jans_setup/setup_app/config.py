import os
import time
import pprint
import inspect
from collections import OrderedDict

from setup_app.paths import INSTALL_DIR, LOG_DIR
from setup_app.static import InstallTypes
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
    def get(cls, attr, default=None):
        return getattr(cls, attr) if hasattr(cls, attr) else default

    @classmethod
    def dump(cls, dumpFile=False):
        if cls.dump_config_on_error:
            return

        myDict = {}
        for obj_name, obj in inspect.getmembers(cls):
            obj_name = str(obj_name)
            if not obj_name.startswith('__') and (not callable(obj)):
                myDict[obj_name] = obj

        if dumpFile:
            fn = os.path.join(cls.install_dir, 'config-'+time.ctime().replace(' ', '-'))
            with open(fn, 'w') as w:
                w.write(pprint.pformat(myDict, indent=2))
        else:
            pp = pprint.PrettyPrinter(indent=2)
            pp.pprint(myDict)

    @classmethod
    def calculate_mem(cls):
        cls.application_max_ram = int(Config.jans_max_mem)

    @classmethod
    def set_rdbm_schema(cls):
        if not cls.get('rdbm_schema'):
            cls.rdbm_schema = 'public' if cls.rdbm_type == 'pgsql' else cls.rdbm_db

    @classmethod
    def init(cls, install_dir=INSTALL_DIR):

        cls.install_dir = install_dir
        cls.data_dir = os.path.join(cls.install_dir, 'setup_app/data')
        cls.profile = base.current_app.profile 

        cls.thread_queue = None
        cls.jetty_user = cls.jetty_group = 'jetty'
        cls.root_user = cls.root_group = 'root'
        cls.backend_service = 'network.target'
        cls.dump_config_on_error = False

        if not cls.output_dir:
            cls.output_dir = os.path.join(install_dir, 'output')

        cls.default_store_type = 'PKCS12'

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

        cls.pbar = DummyProgress()

        cls.properties_password = None
        cls.noPrompt = False

        cls.dist_app_dir = os.path.join(cls.distFolder, 'app')
        cls.dist_jans_dir = os.path.join(cls.distFolder, 'jans')
        cls.distTmpFolder = os.path.join(cls.distFolder, 'tmp')
        cls.jans_scripts_dir = os.path.join(cls.jansOptFolder, 'scripts')

        cls.downloadWars = None
        cls.templateRenderingDict = {
                                        'jans_auth_test_client_2_inum': 'AB77-1A2B',
                                        'jans_auth_test_client_3_inum': '3E20',
                                        'jans_auth_test_client_4_inum': 'FF81-2D39',
                                        'jans_fido2_test_client_1_inum': 'AB77-1A2B',
                                        'jans_fido2_test_client_2_inum':'FF81-2D39',
                                        'idp_attribute_resolver_ldap.search_filter': '(|(uid=$requestContext.principalName)(mail=$requestContext.principalName))',
                                        'server_time_zone': 'UTC' + time.strftime("%z"),
                                     }

        # java commands
        cls.cmd_java = os.path.join(cls.jre_home, 'bin/java')
        cls.cmd_keytool = os.path.join(cls.jre_home, 'bin/keytool')
        cls.cmd_jar = os.path.join(cls.jre_home, 'bin/jar')

        if cls.profile == OPENBANKING_PROFILE:
            cls.use_external_key = True
            cls.ob_key_fn = ''
            cls.ob_cert_fn = ''
            cls.ob_alias = ''
            cls.static_kid = ''
            cls.jwks_uri = ''

        # Component ithversions
        cls.apache_version = None

        #passwords
        cls.admin_password = ''

        #DB installation types
        cls.rdbm_install = InstallTypes.LOCAL

        #rdbm
        cls.rdbm_install_type = InstallTypes.LOCAL
        cls.rdbm_type = 'pgsql'
        cls.rdbm_host = 'localhost'
        cls.rdbm_port = 3306
        cls.rdbm_db = 'jansdb'
        cls.rdbm_user = 'jans'
        cls.rdbm_password = None
        cls.rdbm_password_enc = ''
        cls.static_rdbm_dir = os.path.join(cls.install_dir, 'static/rdbm')
        cls.schema_files = [os.path.join(cls.install_dir, 'schema', schemma_fn) for schemma_fn in ('jans_schema.json', 'custom_schema.json')]
        cls.rdbm_sslmode = 'disable'
        cls.rdbm_sslfactory = 'org.postgresql.ssl.NonValidatingFactory'

        # Jans components installation status
        cls.loadData = True
        cls.install_jans = True
        cls.install_jre = True
        cls.install_jetty = True
        cls.install_jython = True
        cls.install_jans_auth = True
        cls.install_httpd = True
        cls.install_scim_server = True
        cls.install_fido2 = True
        cls.install_config_api = True
        cls.install_casa = False
        cls.install_jans_cli = True
        cls.install_link = False
        cls.loadTestData = False
        cls.allowPreReleasedFeatures = False
        cls.install_jans_shib = False
        cls.install_jans_lock = False
        cls.install_opa = False

        # backward compatibility
        cls.os_type = base.os_type
        cls.os_version = base.os_version
        cls.os_initdaemon = base.os_initdaemon

        cls.persistence_type = 'sql'

        cls.setup_properties_fn = os.path.join(cls.install_dir, 'setup.properties')
        cls.savedProperties = os.path.join(cls.install_dir, 'setup.properties.last')

        cls.jansOptBinFolder = os.path.join(cls.jansOptFolder, 'bin')
        cls.jansOptSystemFolder = os.path.join(cls.jansOptFolder, 'system')
        cls.jansOptPythonFolder = os.path.join(cls.jansOptFolder, 'python')
        cls.configFolder = os.path.join(cls.jansBaseFolder, 'conf') 

        cls.salt_fn = os.path.join(cls.configFolder,'salt')
        cls.jans_properties_fn = os.path.join(cls.configFolder,'jans.properties')
        cls.jans_hybrid_roperties_fn = os.path.join(cls.configFolder, 'jans-hybrid.properties')

        cls.cache_provider_type = 'NATIVE_PERSISTENCE'

        cls.java_type = 'jre'

        cls.hostname = None
        cls.ip = None
        cls.orgName = None
        cls.countryCode = None
        cls.city = None
        cls.state = None
        cls.admin_email = None
        cls.encode_salt = None
        cls.admin_inum = None

        cls.jans_max_mem = int(base.current_mem_size * .85 * 1000) # 85% of physical memory
        cls.calculate_mem()

        cls.templateFolder = os.path.join(cls.install_dir, 'templates')
        cls.staticFolder = os.path.join(cls.install_dir, 'static')

        cls.extensionFolder = os.path.join(cls.staticFolder, 'extension')
        cls.script_catalog_dir = os.path.join(cls.install_dir, 'script_catalog')

        cls.jansScriptFiles = [
                            os.path.join(cls.staticFolder, 'scripts/logmanager.sh'),
                            os.path.join(cls.staticFolder, 'scripts/jans'),
                            os.path.join(cls.staticFolder, 'scripts/jans_services_status.py'),
                            os.path.join(cls.staticFolder, 'scripts/get_agama_lab_projects.py'),
                            ]

        cls.defaultTrustStoreFN = os.path.join(cls.jre_home, 'jre/lib/security/cacerts')
        cls.defaultTrustStorePW = 'changeit'

        # Stuff that gets rendered; filename is necessary. Full path should
        # reflect final path if the file must be copied after its rendered.

        cls.jans_python_readme = os.path.join(cls.jansOptPythonFolder, 'libs/python.txt')
        cls.jansRDBMProperties = os.path.join(cls.configFolder, 'jans-sql.properties')

        cls.ldif_base = os.path.join(cls.output_dir, 'base.ldif')
        cls.ldif_attributes = os.path.join(cls.output_dir, 'attributes.ldif')
        cls.ldif_scopes = os.path.join(cls.output_dir, 'scopes.ldif')
        cls.ldif_agama = os.path.join(cls.output_dir, 'agama.ldif')

        cls.ldif_metric = os.path.join(cls.staticFolder, 'metric/o_metric.ldif')
        cls.ldif_site = os.path.join(cls.install_dir, 'static/site/site.ldif')
        cls.ldif_configuration = os.path.join(cls.output_dir, 'configuration.ldif')

        cls.system_profile_update_init = os.path.join(cls.output_dir, 'system_profile_init')
        cls.system_profile_update_systemd = os.path.join(cls.output_dir, 'system_profile_systemd')

        ### rsyslog file customised for init.d
        cls.rsyslogUbuntuInitFile = os.path.join(cls.install_dir, 'static/system/ubuntu/rsyslog')

        # OpenID key generation default setting
        cls.default_openid_jks_dn_name = 'CN=Jans Auth CA Certificates'
        if cls.profile == OPENBANKING_PROFILE:
            cls.default_sig_key_algs = 'RS256 RS384 RS512 ES256 ES384 ES512'
        else:
            cls.default_sig_key_algs = 'RS256 RS384 RS512 ES256 ES256K ES384 ES512 PS256 PS384 PS512'

        cls.default_enc_key_algs = 'RSA1_5 RSA-OAEP ECDH-ES'
        cls.default_key_expiration = 365

        cls.smtp_jks_fn = os.path.join(cls.certFolder, 'smtp-keys.' + cls.default_store_type.lower())
        cls.smtp_alias = 'smtp_sig_ec256'
        cls.smtp_signing_alg = 'SHA256withECDSA'

        cls.post_messages = []

        cls.ldif_files = [cls.ldif_base,
                           cls.ldif_attributes,
                           cls.ldif_scopes,
                           cls.ldif_site,
                           cls.ldif_metric,
                           cls.ldif_configuration,
                           cls.ldif_agama,
                           ]


        cls.ce_templates = {
                            cls.jans_python_readme: True,
                             cls.etc_hostname: False,
                             cls.network: False,
                             cls.jans_properties_fn: True,
                             cls.ldif_base: False,
                             cls.ldif_attributes: False,
                             cls.ldif_scopes: False,
                             cls.ldif_agama: False,
                             }


        cls.service_requirements = {
                        'jans-auth': ['network-online.target', 72],
                        'jans-fido2': ['network-online.target', 73],
                        'identity': ['jans-auth', 74],
                        'jans-scim': ['jans-auth', 75],
                        'idp': ['jans-auth', 76],
                        'casa': ['jans-auth', 78],
                        'passport': ['jans-auth', 82],
                        'jans-auth-rp': ['jans-auth', 84],
                        'jans-config-api': ['jans-auth', 85],
                        }


        cls.non_setup_properties = {
            'jans_auth_client_jar_fn': os.path.join(cls.dist_jans_dir, 'jans-auth-client-jar-with-dependencies.jar')
                }

        Config.addPostSetupService = []
