#!/usr/bin/python3

import readline
import os
import sys
import time
import glob
import inspect
import zipfile
import shutil
import traceback
import code
import site
import warnings
import select

from pathlib import Path
from queue import Queue

warnings.filterwarnings("ignore")

uname_cmd = shutil.which('uname')
cpu_arch = os.popen(uname_cmd + ' -m').read().strip()

if cpu_arch != 'x86_64':
    print("Janssen Linux setup supports only x86_64 architecture. Detected architecture was {}.".format(cpu_arch))
    sys.exit()

__STATIC_SETUP_DIR__ = '/opt/jans/jans-setup/'
queue = Queue()

dir_path = os.path.dirname(os.path.realpath(__file__))
sys.path.append(dir_path)

profile_fn = os.path.join(dir_path, 'profile')
if os.path.exists(profile_fn):
    with open(profile_fn) as f:
        profile = f.read().strip()
else:
    profile = 'jans'

os.environ['LC_ALL'] = 'C'
os.environ['JANS_PROFILE'] = profile
from setup_app.utils import arg_parser

argsp = arg_parser.get_parser()


# first import paths and make changes if necassary
from setup_app import paths

def ami_packaged():
    if '.shiv' in __file__:
        return True
    my_path = Path(__file__).parent
    for p in site.getsitepackages():
        try:
            rp = my_path.relative_to(p)
            if rp:
                return True
        except ValueError:
            pass

    return False

paths.IAMPACKAGED = ami_packaged()

# for example change log file location:
# paths.LOG_FILE = '/tmp/my.log'

if paths.IAMPACKAGED:
    paths.LOG_DIR = os.path.join(__STATIC_SETUP_DIR__, 'logs')
    paths.LOG_FILE = os.path.join(paths.LOG_DIR, 'setup.log')
    paths.LOG_ERROR_FILE = os.path.join(paths.LOG_DIR, 'setup_error.log')
    paths.LOG_OS_CHANGES_FILE = os.path.join(paths.LOG_DIR, 'os-changes.log')

    if not os.path.exists(__STATIC_SETUP_DIR__):
        os.makedirs(__STATIC_SETUP_DIR__)
    if not os.path.exists(paths.LOG_DIR):
        os.makedirs(paths.LOG_DIR)

from setup_app import static

if argsp.encode_salt and len(argsp.encode_salt) != 24:
    print("{}Length of encde salt should be 24 characters.{}".format(static.colors.DANGER, static.colors.ENDC))
    sys.exit()

# second import module base, this makes some initial settings
from setup_app.utils import base
base.current_app.profile = profile

# we will access args via base module
base.argsp = argsp

if 'SETUP_BRANCH' not in base.current_app.app_info:
    base.current_app.app_info['SETUP_BRANCH'] = argsp.setup_branch

if argsp.java_version:
    base.current_app.app_info['AMAZON_CORRETTO_VERSION'] = argsp.java_version

if base.argsp.jans_app_version:
    base.current_app.app_info['jans_version'] = base.argsp.jans_app_version
else:
    base.current_app.app_info['jans_version'] = base.current_app.app_info['JANS_APP_VERSION'] + base.current_app.app_info['JANS_BUILD']


# download pre-required apps
from setup_app import downloads
downloads.download_apps()

sys.path.insert(0, base.pylib_dir)

from setup_app.utils.package_utils import packageUtils

packageUtils.check_and_install_packages()

if argsp.download_exit:
    downloads.download_all()
    sys.exit()

from setup_app.messages import msg
from setup_app.config import Config
from setup_app.utils.progress import jansProgress

from setup_app.setup_options import get_setup_options
from setup_app.utils import printVersion

from setup_app.test_data_loader import TestDataLoader
from setup_app.utils.properties_utils import propertiesUtils
from setup_app.utils.setup_utils import SetupUtils
from setup_app.utils.collect_properties import CollectProperties

from setup_app.installers.jans import JansInstaller
from setup_app.installers.httpd import HttpdInstaller
from setup_app.installers.jre import JreInstaller
from setup_app.installers.jetty import JettyInstaller
from setup_app.installers.jython import JythonInstaller
from setup_app.installers.jans_auth import JansAuthInstaller

if base.current_app.profile == 'jans':
    from setup_app.installers.scim import ScimInstaller
    from setup_app.installers.fido import FidoInstaller
    from setup_app.installers.jans_link import JansLinkInstaller
    from setup_app.installers.jans_keycloak_link import JansKCLinkInstaller
    from setup_app.installers.jans_casa import CasaInstaller

    from setup_app.installers.jans_saml import JansSamlInstaller
    from setup_app.installers.jans_lock import JansLockInstaller

from setup_app.installers.config_api import ConfigApiInstaller
from setup_app.installers.jans_cli import JansCliInstaller
from setup_app.installers.rdbm import RDBMInstaller

if base.snap:
    try:
        open('/proc/mounts').close()
    except:
        print(
            "Please execute the following command\n  sudo snap connect jans-server:mount-observe :mount-observe\nbefore running setup. Exiting ...")
        sys.exit()

if paths.IAMPACKAGED:
    Config.output_dir = os.path.join(__STATIC_SETUP_DIR__, 'output')
    if not os.path.exists(Config.output_dir):
        os.makedirs(Config.output_dir)

# initialize config object
Config.init(paths.INSTALL_DIR)


if Config.profile != 'jans':
    argsp.t = False

if os.path.exists(Config.jans_properties_fn):
    Config.installed_instance = True

# we must initilize SetupUtils after initilizing Config
SetupUtils.init()

# get setup options from args
setupOptions = get_setup_options()

terminal_size = shutil.get_terminal_size()
tty_rows = terminal_size.lines
tty_columns = terminal_size.columns

# check if we are running in terminal
try:
    os.get_terminal_size()
except:
    argsp.no_progress = True

if not (argsp.n or Config.installed_instance):
    base.check_resources()

# pass progress indicator to Config object
Config.pbar = jansProgress

for key in setupOptions:
    setattr(Config, key, setupOptions[key])

jansInstaller = JansInstaller()
jansInstaller.initialize()

if not Config.installed_instance:
    print()

    print("Installing Janssen Server...\n\nFor more info see:\n  {}  \n  {}\n".format(paths.LOG_FILE, paths.LOG_ERROR_FILE))
    print("Profile         :  {}".format(Config.profile))
    print("Detected OS     :  {}".format(base.get_os_description()))
    print("Janssen Version :  {}".format(base.current_app.app_info['jans_version']))
    print("Detected init   :  {}".format(base.os_initdaemon))
    print("Detected Apache :  {}".format(base.determineApacheVersion()))
    print()

setup_loaded = {}
if setupOptions['setup_properties']:
    base.logIt('%s Properties found!\n' % setupOptions['setup_properties'])
    setup_loaded = propertiesUtils.load_properties(setupOptions['setup_properties'])
elif os.path.isfile(Config.setup_properties_fn):
    base.logIt('%s Properties found!\n' % Config.setup_properties_fn)
    setup_loaded = propertiesUtils.load_properties(Config.setup_properties_fn)
elif os.path.isfile(Config.setup_properties_fn + '.enc'):
    base.logIt('%s Properties found!\n' % Config.setup_properties_fn + '.enc')
    setup_loaded = propertiesUtils.load_properties(Config.setup_properties_fn + '.enc')

if argsp.import_ldif:
    if os.path.isdir(argsp.import_ldif):
        base.logIt("Found setup LDIF import directory {}".format(argsp.import_ldif))
    else:
        base.logIt("The custom LDIF import directory {} does not exist. Exiting...".format(argsp.import_ldif, True, True))

collectProperties = CollectProperties()
if os.path.exists(Config.jans_properties_fn):
    collectProperties.collect()
    collectProperties.save()

    if argsp.csx:
        print("Saving collected properties")
        collectProperties.save()
        sys.exit()

if not Config.noPrompt and not Config.installed_instance and not setup_loaded:
    propertiesUtils.promptForProperties()

propertiesUtils.check_properties()
# initialize installers, order is important!
jreInstaller = JreInstaller()
jettyInstaller = JettyInstaller()
jythonInstaller = JythonInstaller()
rdbmInstaller = RDBMInstaller()
httpdinstaller = HttpdInstaller()
jansAuthInstaller = JansAuthInstaller()
configApiInstaller = ConfigApiInstaller()

if Config.profile == 'jans':
    fidoInstaller = FidoInstaller()
    scimInstaller = ScimInstaller()
    casa_installer = CasaInstaller()
    jans_link_installer = JansLinkInstaller()
    jans_keycloak_link_installer = JansKCLinkInstaller()
    jans_saml_installer = JansSamlInstaller()
    jans_lock_installer = JansLockInstaller()

jansCliInstaller = JansCliInstaller()

rdbmInstaller.packageUtils = packageUtils

if Config.installed_instance:

    if argsp.enable_script:
        print("Enabling scripts {}".format(', '.join(argsp.enable_script)))
        jansInstaller.enable_scripts(argsp.enable_script)
        sys.exit()

    if argsp.disable_script:
        print("Disabling scripts {}".format(', '.join(argsp.disable_script)))
        jansInstaller.enable_scripts(argsp.disable_script, enable=False)
        sys.exit()


    for service in jansProgress.services:
        setattr(Config, service['object'].install_var, service['object'].installed())

    if not argsp.shell:
        propertiesUtils.promptForProperties()

        #if not (argsp.t or argsp.x) and not Config.addPostSetupService:
        #    print("No service was selected to install. Exiting ...")
        #    sys.exit()

def print_or_log(msg):
    print(msg) if argsp.x else base.logIt(msg)


if Config.profile == 'jans':
    if argsp.t:
        testDataLoader = TestDataLoader()

    if argsp.t and argsp.x:
        print_or_log("Loading test data")
        testDataLoader.dbUtils.bind()
        configApiInstaller.load_test_data()
        testDataLoader.load_test_data()
        print_or_log("Test data loaded.")

    if not argsp.t and argsp.x and argsp.load_config_api_test:
        print_or_log("Loading Config Api Test data")
        configApiInstaller.load_test_data()
        print_or_log("Test data loaded. Exiting ...")

    if argsp.x:
        print("Exiting ...")
        sys.exit()

Config.install_jans_cli = Config.install_config_api or Config.install_scim_server

app_vars = locals().copy()

if argsp.shell:
    code.interact(local=locals())
    sys.exit()

if Config.profile == 'jans':
    # re-calculate memory usage
    Config.calculate_mem()

print()
print(jansInstaller)

base.current_app.proceed_installation = True

def main():

    if not Config.noPrompt:
        proceed_prompt = input('Proceed with these values [Y|n] ').lower().strip()
        if proceed_prompt and proceed_prompt[0] != 'y':
            base.current_app.proceed_installation = False

        if Config.rdbm_install_type == static.InstallTypes.LOCAL:
            packageUtils.check_and_install_packages()

    # register post setup progress
    class PostSetup:
        service_name = 'post-setup'
        install_var = 'installPostSetup'
        app_type = static.AppType.APPLICATION
        install_type = static.InstallOption.MANDATORY


    jansProgress.register(PostSetup)

    if not argsp.no_progress:
        jansProgress.queue = queue


    def do_installation():
        jansProgress.before_start()
        jansProgress.start()

        try:
            jettyInstaller.calculate_selected_aplications_memory()

            if not Config.installed_instance:
                jansInstaller.configureSystem()
                jansInstaller.make_salt()

                if not base.snap:
                    jreInstaller.start_installation()
                    jettyInstaller.start_installation()
                    jythonInstaller.start_installation()

                jansInstaller.generate_smtp_config()
                jansInstaller.copy_scripts()
                jansInstaller.encode_passwords()
                jansInstaller.render_templates()
                jansInstaller.render_configuration_template()

                if not base.snap:
                    jansInstaller.update_hostname()
                    jansInstaller.set_ulimits()

                jansInstaller.copy_output()
                jansInstaller.setup_init_scripts()

                if Config.rdbm_install:
                    rdbmInstaller.start_installation()

            jansInstaller.order_services()

            if (Config.installed_instance and 'install_httpd' in Config.addPostSetupService) or (
                    not Config.installed_instance and Config.install_httpd):
                httpdinstaller.configure()

            if (Config.installed_instance and 'install_jans_auth' in Config.addPostSetupService) or (
                    not Config.installed_instance and Config.install_jans_auth):
                jansAuthInstaller.start_installation()

            if (Config.installed_instance and configApiInstaller.install_var in Config.addPostSetupService) or (
                    not Config.installed_instance and Config.get(configApiInstaller.install_var)):
                configApiInstaller.start_installation()
                if argsp.t or getattr(argsp, 'load_config_api_test', None):
                    configApiInstaller.load_test_data()

            if Config.profile == 'jans':

                if (Config.installed_instance and 'install_fido2' in Config.addPostSetupService) or (
                        not Config.installed_instance and Config.install_fido2):
                    fidoInstaller.start_installation()

                if (Config.installed_instance and 'install_scim_server' in Config.addPostSetupService) or (
                        not Config.installed_instance and Config.install_scim_server):
                    scimInstaller.start_installation()

                if (Config.installed_instance and casa_installer.install_var in Config.addPostSetupService) or (
                        not Config.installed_instance and Config.get(casa_installer.install_var)):
                    casa_installer.start_installation()


                if (Config.installed_instance and jans_link_installer.install_var in Config.addPostSetupService) or (
                        not Config.installed_instance and Config.get(jans_link_installer.install_var)):
                    jans_link_installer.start_installation()

                if (Config.installed_instance and jans_keycloak_link_installer.install_var in Config.addPostSetupService) or (
                        not Config.installed_instance and Config.get(jans_keycloak_link_installer.install_var)):
                    jans_keycloak_link_installer.start_installation()

                if (Config.installed_instance and jansCliInstaller.install_var in Config.addPostSetupService) or (
                            not Config.installed_instance and Config.get(jansCliInstaller.install_var)):
                        jansCliInstaller.start_installation()
                        jansCliInstaller.configure()

                if (Config.installed_instance and jans_saml_installer.install_var in Config.addPostSetupService) or (
                        not Config.installed_instance and Config.get(jans_saml_installer.install_var)):
                    jans_saml_installer.start_installation()

                if (Config.installed_instance and jans_lock_installer.install_var in Config.addPostSetupService) or (
                        not Config.installed_instance and Config.get(jans_lock_installer.install_var)):
                    jans_lock_installer.start_installation()

            jansInstaller.post_install_before_saving_properties()
            jansProgress.progress(PostSetup.service_name, "Saving properties")
            propertiesUtils.save_properties()
            time.sleep(2)

            jansInstaller.post_install_tasks()

            jansInstaller.reload_daemon()
            for service in jansProgress.services:
                if service['app_type'] == static.AppType.SERVICE:
                    jansProgress.progress(PostSetup.service_name,
                                          "Starting {}".format(service['name'].replace('-', ' ').replace('_', ' ').title()))
                    time.sleep(2)
                    service['object'].enable()
                    service['object'].stop()
                    service['object'].start()

            if argsp.t:
                base.logIt("Loading test data")
                testDataLoader.load_test_data()

            jansProgress.progress(static.COMPLETED)

            print()
            for m in Config.post_messages:
                print(m)

        except:

            base.logIt("FATAL", True, True)


    if base.current_app.proceed_installation:
        do_installation()
        print('\n', static.colors.OKGREEN)
        if Config.install_config_api or Config.install_scim_server:
            if Config.install_config_api:
                msg.installation_completed += "To manage your Janssen Identity Provider:\n"
                msg.installation_completed += '/opt/jans/jans-cli/config-cli-tui.py'
                if base.current_app.profile == static.SetupProfiles.OPENBANKING:
                    ca_dir = os.path.join(Config.output_dir, 'CA')
                    crt_fn = os.path.join(ca_dir, 'client.crt')
                    key_fn = os.path.join(ca_dir, 'client.key')
                    msg.installation_completed += ' -CC {} -CK {}'.format(crt_fn, key_fn)
                msg.installation_completed +="\n"

        if Config.install_casa:
            msg.installation_completed += f"Browse https://{Config.hostname}/jans-casa\n"

        msg_text = msg.post_installation if Config.installed_instance else msg.installation_completed.format(
            Config.hostname)
        print(msg_text)
        print('\n', static.colors.ENDC)
        print(static.colors.DANGER)
        print(msg.setup_removal_warning)
        print(static.colors.ENDC, '\n')
        # we need this for progress write last line
        time.sleep(2)

        if not os.environ.get('FLEX_PRE_JANS'):
            if not argsp.n:
                print(f"\033[31m\033[1mDo you want to remove directories {Config.distFolder} and {__STATIC_SETUP_DIR__} [yes/N] \033[0m")

                i, o, e = select.select( [sys.stdin], [], [], 10 )
                romove_setup_files = False
                if i:
                    response = sys.stdin.readline().strip()
                    if response == 'yes':
                        romove_setup_files = True
                    elif response.startswith('y') and response != 'yes':
                        print("Please type \033[1m yes \033[0m to remove ")
                        i_, o_, e_ = select.select( [sys.stdin], [], [], 10 )
                        if i_:
                            response_ = sys.stdin.readline().strip()
                            if response_ == 'yes':
                                romove_setup_files = True

                if not romove_setup_files:
                    print (f"You can remove directories {Config.distFolder} and {__STATIC_SETUP_DIR__} anytime")
                else:
                    print(f"Removig {Config.distFolder}")
                    shutil.rmtree(Config.distFolder)
                    os.chdir(Path.home())
                    print(f"Removing {__STATIC_SETUP_DIR__}")
                    shutil.rmtree(__STATIC_SETUP_DIR__)

if __name__ == "__main__":
    main()
