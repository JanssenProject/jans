#!/usr/bin/python3

import readline
import os
import re
import sys
import json
import shutil
import warnings
import argparse
import socket

from queue import Queue

warnings.filterwarnings("ignore")
message_provider_types = ['POSTGRES', 'REDIS']
parser_description="Use this script to install Jans Lock Server"
parser = argparse.ArgumentParser(description=parser_description)
parser.add_argument('-n', help="No interactive prompt before install starts.", action='store_true')
parser.add_argument('--no-httpd', help="No apache httpd server", action='store_true')
parser.add_argument('-encode-salt', help="24 characters length string to be used for encoding passwords")
parser.add_argument('-ip-address', help="Used primarily by Apache httpd for the Listen directive")
parser.add_argument('-host-name', help="Internet-facing FQDN that is used to generate certificates and metadata.")
parser.add_argument('-org-name', help="Organization name field used for generating X.509 certificates")
parser.add_argument('-email', help="Email address for support at your organization used for generating X.509 certificates")
parser.add_argument('-city', help="City field used for generating X.509 certificates")
parser.add_argument('-state', help="State field used for generating X.509 certificates")
parser.add_argument('-country', help="Two letters country coude used for generating X.509 certificates")
parser.add_argument('-pgsql-host', help="PostgreSQL host")
parser.add_argument('-pgsql-user', help="PostgreSQL username", default="jans")
parser.add_argument('-pgsql-password', help="PostgreSQL password")
parser.add_argument('-pgsql-port', help="PostgreSQL port", default="5432")
parser.add_argument('-pgsql-db', help="PostgreSQL database", default="jansdb")
parser.add_argument('-setup-branch', help="Janssen setup github branch", default='main')
parser.add_argument('-message-provider-type', help="Message provider type", default=message_provider_types[0], choices=message_provider_types)
parser.add_argument('-redis-host', help="Redis host", default='localhost')
parser.add_argument('-redis-port', help="Redis port", default='6379')


argsp = parser.parse_args()
if argsp.encode_salt and len(argsp.encode_salt) != 24:
    print("Length of encde salt should be 24 characters.")
    sys.exit()

argsp.force_download = False
argsp.local_rdbm = False

__STATIC_SETUP_DIR__ = '/opt/jans/jans-setup/'
queue = Queue()

dir_path = os.path.dirname(os.path.realpath(__file__))
sys.path.append(dir_path)
os.environ['LC_ALL'] = 'C'

# first import paths and make changes if necassary
from setup_app import paths
from setup_app import static

# second import module base, this makes some initial settings
from setup_app.utils import base
base.current_app.profile = 'jans'

# we will access args via base module
base.argsp = argsp
base.current_app.app_info['SETUP_BRANCH'] = argsp.setup_branch
base.current_app.app_info['jans_version'] = base.current_app.app_info['JANS_APP_VERSION'] + base.current_app.app_info['JANS_BUILD']


# download pre-required apps
from setup_app import downloads
#downloads.download_apps()
downloads.download_sqlalchemy()
downloads.download_cryptography()
downloads.download_pyjwt()
downloads.download_pymysql()

sys.path.insert(0, base.pylib_dir)

from setup_app.utils.package_utils import packageUtils
packageUtils.check_and_install_packages()

from setup_app.messages import msg
from setup_app.config import Config
from setup_app.static import colors
from setup_app.utils.progress import jansProgress
from setup_app.utils.setup_utils import SetupUtils
from setup_app.utils.properties_utils import propertiesUtils
from setup_app.utils.db_utils import dbUtils
from setup_app.installers.jans import JansInstaller
from setup_app.installers.httpd import HttpdInstaller
from setup_app.installers.jre import JreInstaller
from setup_app.installers.jetty import JettyInstaller
from setup_app.installers.jans_auth import JansAuthInstaller
from setup_app.installers.rdbm import RDBMInstaller
from setup_app.installers.jans_lock import JansLockInstaller

# initialize config object
Config.init(paths.INSTALL_DIR)

# we must initilize SetupUtils after initilizing Config
SetupUtils.init()

if not argsp.n:
    propertiesUtils.prompt_for_http_cert_info()
else:
    Config.hostname = argsp.host_name
    Config.ip = argsp.ip_address
    Config.orgName = argsp.org_name
    Config.countryCode = argsp.country
    Config.city = argsp.city
    Config.state = argsp.state
    Config.admin_email = argsp.email

if not (argsp.pgsql_host or argsp.pgsql_password):
    argsp.encode_salt = propertiesUtils.getPrompt("Encode salt")
    argsp.pgsql_host = propertiesUtils.getPrompt("PostgreSQL host")
    argsp.pgsql_password = propertiesUtils.getPrompt("PostgreSQL password")

if not argsp.n:
    argsp.pgsql_user = propertiesUtils.getPrompt("PostgreSQL username", Config.get('rdbm_user'))
    argsp.pgsql_db = propertiesUtils.getPrompt("PostgreSQL database", Config.get('rdbm_db'))
    argsp.pgsql_port = propertiesUtils.getPrompt("PostgreSQL port", argsp.pgsql_port)
    message_provider_type = propertiesUtils.getPrompt(f"Message provider type [1]:{message_provider_types[0]} [2]:{message_provider_types[1]}", '1')
    if message_provider_type == '2':
        argsp.message_provider_type = message_provider_types[1]
        argsp.redis_host = propertiesUtils.getPrompt("Redis host", argsp.redis_host)
        argsp.redis_port = propertiesUtils.getPrompt("Redis port", argsp.redis_port)

Config.rdbm_host = argsp.pgsql_host
Config.rdbm_port = argsp.pgsql_port
Config.rdbm_db = argsp.pgsql_db
Config.rdbm_user = argsp.pgsql_user
Config.rdbm_password = argsp.pgsql_password
Config.lock_message_provider_type = argsp.message_provider_type
Config.lock_redis_host = argsp.redis_host
Config.lock_redis_port = argsp.redis_port
Config.install_jans_lock = True
Config.install_jans_lock_as_server = True
Config.install_opa = True

if Config.lock_message_provider_type == message_provider_types[1]:
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    try:
        print("Connecting to Redis server...")
        s.connect((Config.lock_redis_host, int(Config.lock_redis_port)))
    except Exception as e:
        print(f"{colors.DANGER}Can't connect to REDIS server.{colors.ENDC}")
        sys.exit()

result = dbUtils.sqlconnection()


if result[0]:
    print("Successfully connected to PostgreSQL")
else:
    print(f"{colors.DANGER}Fail to connect PostgreSQL at host {Config.rdbm_host}:{Config.rdbm_port} {colors.ENDC}")
    print("Reason is", ' '.join(result[1].args))
    print(f" * Please check if PostgreSQL is accepting remote connection.")
    print(f" * User {colors.BOLD}{Config.rdbm_user}{colors.ENDC} is allowed to connect from this host to database {colors.BOLD}{Config.rdbm_db}{colors.ENDC}.")
    sys.exit()

dbUtils.rdm_automapper()

# pass progress indicator to Config object
Config.pbar = jansProgress

jansInstaller = JansInstaller()
jansInstaller.initialize()
# initialize installers, order is important!
httpdinstaller = HttpdInstaller()
jreInstaller = JreInstaller()
jettyInstaller = JettyInstaller()
jans_lock_installer = JansLockInstaller()
RDBMInstaller.register_progess = lambda x: None
rdbmInstaller = RDBMInstaller()

Config.calculate_mem()

def clean_httpd_config():

    httpd_conf_base_name = os.path.basename(httpdinstaller.apache2_ssl_conf)
    httpd_conf_temp_fn = os.path.join(httpdinstaller.templates_folder, httpd_conf_base_name)
    httpd_conf_temp_org_fn = httpd_conf_temp_fn + '.org'
    if not os.path.exists(httpd_conf_temp_org_fn):
        shutil.copy(httpd_conf_temp_fn, httpd_conf_temp_org_fn)
    apache_config_str = httpdinstaller.readFile(httpd_conf_temp_org_fn)
    re_search_result = re.search(r"<Location /jans-lock>(.+?)</Location>", apache_config_str, flags=re.DOTALL)
    jans_lock_config = re_search_result.group()
    apache_config_lock_str = re.sub(r"<Location (.+?)>(.+?)</Location>", '', apache_config_str, flags=re.DOTALL)
    apache_config_lock_str = re.sub(r"<LocationMatch (.+?)>(.+?)</LocationMatch>", '', apache_config_lock_str, flags=re.DOTALL)
    apache_config_lock_str = re.sub(r"ProxyPass (.+?)\n", '', apache_config_lock_str)
    apache_config_lock_str = re.sub(r"<If (.+?)>(.+?)</If>", jans_lock_config, apache_config_lock_str, flags=re.DOTALL)
    httpdinstaller.writeFile(httpd_conf_temp_fn, apache_config_lock_str)

clean_httpd_config()

result = dbUtils.dn_exists('ou=configuration,o=jans')
if not result:
    print("Janssen was not configured on this PostgreSQL server")
    sys.exit()

if 'jansMessageConf' in result and result['jansMessageConf']:
    message_config_s = result['jansMessageConf']
    message_config = json.loads(message_config_s)
    current_message_provider_type = message_config['messageProviderType']
    if current_message_provider_type != 'DISABLED' and current_message_provider_type != Config.lock_message_provider_type:
        print(f"{colors.WARNING}Message provider type was set to {colors.BOLD}{current_message_provider_type}{colors.ENDC}. {colors.WARNING}NOT CHANING TO {colors.BOLD}{Config.lock_message_provider_type}{colors.ENDC}")
        Config.lock_message_provider_type = current_message_provider_type

base.current_app.proceed_installation = True
jansProgress.queue = queue
jansProgress.before_start()
jansProgress.start()
jettyInstaller.calculate_selected_aplications_memory()


jansInstaller.configureSystem()
jansInstaller.make_salt()
jansInstaller.renderTemplateInOut(Config.jans_properties_fn, Config.templateFolder, out_file=Config.jans_properties_fn)
rdbmInstaller.rdbmProperties()
jansInstaller.secure_files()

httpdinstaller.configure()
jreInstaller.start_installation()
jettyInstaller.start_installation()
jansInstaller.order_services()

jans_lock_installer.start_installation()
jans_lock_installer.configure_message_conf()

jans_lock_installer.chown(Config.jansBaseFolder, user=Config.jetty_user, group=Config.jetty_group, recursive=True)

print("Starting services")
jans_lock_installer.start()
jans_lock_installer.start('opa')
httpdinstaller.stop()
httpdinstaller.start()
print("Jans Lock installation was finished")
