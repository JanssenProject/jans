import re
import os
import time
import pprint
import inspect
from collections import OrderedDict

from setup_app.paths import INSTALL_DIR
from setup_app.static import InstallTypes
from setup_app.utils.printVersion import get_war_info
from setup_app.utils import base

class Config:

    # we define statics here so that is is acessible without construction
    jansOptFolder = '/opt/jans'
    distFolder = '/opt/dist'
    jre_home = '/opt/jre'
    jansBaseFolder = '/etc/jans'
    certFolder = '/etc/certs'
    oxBaseDataFolder = '/var/jans'
    etc_hosts = '/etc/hosts'
    etc_hostname = '/etc/hostname'
    osDefault = '/etc/default'
    sysemProfile = '/etc/profile'
    jython_home = '/opt/jython'
    network = '/etc/sysconfig/network'
    jetty_home = '/opt/jetty'
    jetty_base = os.path.join(jansOptFolder, 'jetty')
    installed_instance = False
    profile = 'openbanking'

    @classmethod
    def get(self, attr, default=None):
        return getattr(self, attr) if hasattr(self, attr) else default


    @classmethod
    def determine_version(self):
        oxauth_info = get_war_info(os.path.join(self.distJansFolder, 'jans-auth.war'))
        self.oxVersion = oxauth_info['version']
        self.currentJansVersion = re.search('([\d.]+)', oxauth_info['version']).group().strip('.')
        self.githubBranchName = oxauth_info['branch']

        self.ce_setup_zip = 'https://github.com/JanssenProject/jans-setup/archive/%s.zip' % self.githubBranchName

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
    def init(self, install_dir=INSTALL_DIR):

        self.install_dir = install_dir
        self.thread_queue = None
        self.jetty_user = 'jetty'
        self.dump_config_on_error = False

        #create dummy progress bar that logs to file in case not defined
        progress_log_file = os.path.join(self.install_dir, 'logs', 'progress-bar.log')
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

        self.distAppFolder = os.path.join(self.distFolder, 'app')
        self.distJansFolder = os.path.join(self.distFolder, 'jans')
        self.distTmpFolder = os.path.join(self.distFolder, 'tmp')

        self.downloadWars = None
        self.templateRenderingDict = {}

        # openbanking
        self.use_external_key = True
        self.ob_key_fn = '/root/obsigning-axV5umCvTMBMjPwjFQgEvb_NO_UPLOAD.key'
        self.ob_cert_fn = '/root/obsigning.pem'
        self.ob_alias = 'GkwIzWy88xWSlcWnLiEc8ip9s2M'

        # java commands
        self.cmd_java = os.path.join(self.jre_home, 'bin/java')
        self.cmd_keytool = os.path.join(self.jre_home, 'bin/keytool')
        self.cmd_jar = os.path.join(self.jre_home, 'bin/jar')

        # Component ithversions
        self.apache_version = None

        #DB installation types
        self.rdbm_install = InstallTypes.LOCAL

        #rdbm
        self.rdbm_install_type = InstallTypes.NONE
        self.rdbm_type = 'mysql'
        self.rdbm_host = 'localhost'
        self.rdbm_port = 3306
        self.rdbm_db = 'jansdb'
        self.rdbm_user = 'jans'
        self.rdbm_password = None
        self.static_rdbm_dir = os.path.join(self.install_dir, 'static/rdbm')

        # Jans components installation status
        self.loadData = True
        self.installJans = True
        self.installJre = True
        self.installJetty = True
        self.installJython = True
        self.installOxAuth = True
        self.installHttpd = True
        self.installConfigApi = True
        self.installJansCli = True

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

        self.jans_properties_fn = os.path.join(self.configFolder,'jans.properties')

        self.cache_provider_type = 'NATIVE_PERSISTENCE'

        self.java_type = 'jre'

        self.hostname = None
        self.ip = None
        self.orgName = None
        self.countryCode = None
        self.city = None
        self.state = None
        self.admin_email = None
        self.encode_salt = None
        self.admin_inum = None

        self.jans_max_mem = int(base.current_mem_size * .85 * 1000) # 85% of physical memory
        self.application_max_ram = int(int(Config.jans_max_mem) * .8)

        self.outputFolder = os.path.join(self.install_dir, 'output')
        self.templateFolder = os.path.join(self.install_dir, 'templates')
        self.staticFolder = os.path.join(self.install_dir, 'static')

        self.extensionFolder = os.path.join(self.staticFolder, 'extension')


        self.jansScriptFiles = [
                            os.path.join(self.install_dir, 'static/scripts/logmanager.sh'),
                            os.path.join(self.install_dir, 'static/scripts/testBind.py')
                            ]

        self.redhat_services = ['httpd', 'rsyslog']
        self.debian_services = ['apache2', 'rsyslog']

        self.defaultTrustStoreFN = os.path.join(self.jre_home, 'jre/lib/security/cacerts')
        self.defaultTrustStorePW = 'changeit'


        # Stuff that gets rendered; filename is necessary. Full path should
        # reflect final path if the file must be copied after its rendered.
        
        self.jans_python_readme = os.path.join(self.jansOptPythonFolder, 'libs/python.txt')
        self.jansRDBMProperties = os.path.join(self.configFolder, 'jans-sql.properties')

        self.system_profile_update_systemd = os.path.join(self.outputFolder, 'system_profile_systemd')

        # OpenID key generation default setting
        self.default_openid_jks_dn_name = 'CN=Jans Auth CA Certificates'
        self.default_key_algs = 'RS256 RS384 RS512 ES256 ES384 ES512'
        self.default_key_expiration = 365
        self.staticKid = ''

        self.post_messages = []

        self.ce_templates = {
                             self.jans_python_readme: True,
                             self.etc_hostname: False,
                             self.network: False,
                             self.jans_properties_fn: True,
                             }

        self.non_setup_properties = {
            'oxauth_client_jar_fn': os.path.join(self.distJansFolder, 'jans-auth-client-jar-with-dependencies.jar')
                }

        Config.addPostSetupService = []

