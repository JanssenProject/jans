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
from queue import Queue

#first import paths and make changes if necassary
from setup_app import paths

#for example change log file location:
#paths.LOG_FILE = '/tmp/my.log'

from setup_app import static

# second import module base, this makes some initial settings
from setup_app.utils import base

from setup_app.messages import msg
from setup_app.config import Config
from setup_app.utils.progress import gluuProgress


from setup_app.setup_options import get_setup_options
from setup_app.utils import printVersion

from setup_app.test_data_loader import TestDataLoader
from setup_app.utils.properties_utils import propertiesUtils
from setup_app.utils.setup_utils import SetupUtils
from setup_app.utils.collect_properties import CollectProperties

from setup_app.installers.gluu import GluuInstaller
from setup_app.installers.httpd import HttpdInstaller
from setup_app.installers.opendj import OpenDjInstaller
from setup_app.installers.couchbase import CouchbaseInstaller
from setup_app.installers.jre import JreInstaller
from setup_app.installers.jetty import JettyInstaller
from setup_app.installers.jython import JythonInstaller
from setup_app.installers.node import NodeInstaller
from setup_app.installers.oxauth import OxauthInstaller
from setup_app.installers.oxtrust import OxtrustInstaller
from setup_app.installers.scim import ScimInstaller
from setup_app.installers.passport import PassportInstaller
from setup_app.installers.fido import FidoInstaller
from setup_app.installers.saml import SamlInstaller
from setup_app.installers.radius import RadiusInstaller
from setup_app.installers.oxd import OxdInstaller
from setup_app.installers.casa import CasaInstaller




# initialize config object
Config.init(paths.INSTALL_DIR)
Config.determine_version()

# we must initilize SetupUtils after initilizing Config
SetupUtils.init()

# get setup options from args
argsp, setupOptions = get_setup_options()

terminal_size = shutil.get_terminal_size()
tty_rows=terminal_size.lines 
tty_columns = terminal_size.columns
queue = Queue()
GSA = None

if (not argsp.c) and sys.stdout.isatty() and (int(tty_rows) > 24) and (int(tty_columns) > 79):
    try:
        import npyscreen
    except:
        print("Can't start TUI, continuing command line")
    else:
        from setup_app.utils.tui import GSA
        on_tui = True

if not argsp.n and not GSA:
    base.check_resources()


# pass progress indicator to Config object
Config.pbar = gluuProgress


for key in setupOptions:
    setattr(Config, key, setupOptions[key])


gluuInstaller = GluuInstaller()
gluuInstaller.initialize()

"""
Config.hostname = 'snap.gluu.org'
Config.ip = '174.138.37.150'
Config.oxtrust_admin_password = 'Top!Secret-20'
Config.orgName = 'MyGluu'
Config.countryCode = 'GC'
Config.city = 'GluuCity'
Config.state = 'GluuState'
Config.admin_email = 'admin@mygluu.org'
Config.installPassport = False
Config.installFido2 = False
Config.installScimServer = False
Config.installSaml = False
Config.installOxd = False
Config.installPassport = False
"""

if not GSA:
    print()
    print("Installing Gluu Server...\n\nFor more info see:\n  {}  \n  {}\n".format(paths.LOG_FILE, paths.LOG_ERROR_FILE))
    print("Detected OS     :  {} {}".format(base.os_type, base.os_version))
    print("Gluu Version    :  {}".format(Config.oxVersion))
    print("Detected init   :  {}".format(base.os_initdaemon))
    print("Detected Apache :  {}".format(base.determineApacheVersion()))
    print()


if setupOptions['setup_properties']:
    base.logIt('%s Properties found!\n' % setupOptions['setup_properties'])
    setup_loaded = propertiesUtils.load_properties(setupOptions['setup_properties'])
elif os.path.isfile(Config.setup_properties_fn):
    base.logIt('%s Properties found!\n' % Config.setup_properties_fn)
    setup_loaded = propertiesUtils.load_properties(Config.setup_properties_fn)
elif os.path.isfile(Config.setup_properties_fn+'.enc'):
    base.logIt('%s Properties found!\n' % Config.setup_properties_fn+'.enc')
    setup_loaded = propertiesUtils.load_properties(Config.setup_properties_fn+'.enc')


collectProperties = CollectProperties()
if os.path.exists(Config.gluu_properties_fn):
    collectProperties.collect()
    Config.installed_instance = True

    if argsp.csx:
        print("Saving collected properties")
        collectProperties.save()
        sys.exit()


if not Config.noPrompt and not GSA and not Config.installed_instance:
    propertiesUtils.promptForProperties()

if not GSA:
    propertiesUtils.check_properties()

# initialize installers, order is important!
jreInstaller = JreInstaller()
jettyInstaller = JettyInstaller()
jythonInstaller = JythonInstaller()
nodeInstaller = NodeInstaller()
openDjInstaller = OpenDjInstaller()
couchbaseInstaller = CouchbaseInstaller()
httpdinstaller = HttpdInstaller()
oxauthInstaller = OxauthInstaller()
oxtrustInstaller = OxtrustInstaller()
fidoInstaller = FidoInstaller()
scimInstaller = ScimInstaller()
samlInstaller = SamlInstaller()
oxdInstaller = OxdInstaller()
casaInstaller = CasaInstaller()
passportInstaller = PassportInstaller()
radiusInstaller = RadiusInstaller()

if Config.installed_instance:
    for installer in (openDjInstaller, couchbaseInstaller, httpdinstaller, 
                        oxauthInstaller, passportInstaller, scimInstaller, 
                        fidoInstaller, samlInstaller, oxdInstaller, 
                        casaInstaller, radiusInstaller):

        setattr(Config, installer.install_var, installer.installed())

    if not GSA:
        propertiesUtils.promptForProperties()

        if not Config.addPostSetupService:
            print("No service was selected to install. Exiting ...")
            sys.exit()

if argsp.t or argsp.x:
    testDataLoader = TestDataLoader()
    testDataLoader.passportInstaller = passportInstaller
    testDataLoader.scimInstaller = scimInstaller

if argsp.x:
    print("Loading test data")
    testDataLoader.dbUtils.bind()
    testDataLoader.createLdapPw()
    testDataLoader.load_test_data()
    testDataLoader.deleteLdapPw()
    print("Test data loaded. Exiting ...")
    sys.exit()


if not GSA:
    print()
    print(gluuInstaller)

    proceed = True
    if not Config.noPrompt:
        proceed_prompt = input('Proceed with these values [Y|n] ').lower().strip()
        if proceed_prompt and proceed_prompt[0] !='y':
            proceed = False

#register post setup progress
class PostSetup:
    service_name = 'post-setup'
    install_var = 'installPostSetup'
    app_type = static.AppType.APPLICATION
    install_type = static.InstallOption.MONDATORY

gluuProgress.register(PostSetup)
gluuProgress.queue = queue

def do_installation():

    if not GSA:
        gluuProgress.before_start()
        gluuProgress.start()

    try:
        jettyInstaller.calculate_selected_aplications_memory()

        if not Config.installed_instance:
            gluuInstaller.configureSystem()
            gluuInstaller.make_salt()
            oxauthInstaller.make_salt()

            if not base.snap:
                jreInstaller.start_installation()
                jettyInstaller.start_installation()
                jythonInstaller.start_installation()
                nodeInstaller.start_installation()

            gluuInstaller.copy_scripts()
            gluuInstaller.encode_passwords()

            oxtrustInstaller.generate_api_configuration()

            Config.ldapCertFn = Config.opendj_cert_fn
            Config.ldapTrustStoreFn = Config.opendj_p12_fn
            Config.encoded_ldapTrustStorePass = Config.encoded_opendj_p12_pass
            Config.oxTrustConfigGeneration = 'true' if Config.installSaml else 'false'

            gluuInstaller.prepare_base64_extension_scripts()
            gluuInstaller.render_templates()
            gluuInstaller.render_configuration_template()

            if not base.snap:
                gluuInstaller.update_hostname()
                gluuInstaller.set_ulimits()

            gluuInstaller.copy_output()
            gluuInstaller.setup_init_scripts()

            # Installing gluu components

            if Config.wrends_install:
                openDjInstaller.start_installation()

            if Config.cb_install:
                couchbaseInstaller.start_installation()


        if (Config.installed_instance and 'installHttpd' in Config.addPostSetupService) or (not Config.installed_instance and Config.installHttpd):
            httpdinstaller.configure()

        if (Config.installed_instance and 'installOxAuth' in Config.addPostSetupService) or (not Config.installed_instance and Config.installOxAuth):
            oxauthInstaller.start_installation()

        if (Config.installed_instance and 'installOxAuthRP' in Config.addPostSetupService) or (not Config.installed_instance and Config.installOxAuthRP):
            oxauthInstaller.install_oxauth_rp()

        if (Config.installed_instance and 'installOxTrust' in Config.addPostSetupService) or (not Config.installed_instance and Config.installOxTrust):
            oxtrustInstaller.start_installation()

        if (Config.installed_instance and 'installFido2' in Config.addPostSetupService) or (not Config.installed_instance and Config.installFido2):
            fidoInstaller.start_installation()

        if (Config.installed_instance and 'installScimServer' in Config.addPostSetupService) or (not Config.installed_instance and Config.installScimServer):
            scimInstaller.start_installation()

        if (Config.installed_instance and 'installSaml' in Config.addPostSetupService) or (not Config.installed_instance and Config.installSaml):
            samlInstaller.start_installation()

        if (Config.installed_instance and 'installOxd' in Config.addPostSetupService) or (not Config.installed_instance and Config.installOxd):
            oxdInstaller.start_installation()

        if (Config.installed_instance and 'installCasa' in Config.addPostSetupService) or (not Config.installed_instance and Config.installCasa):
            casaInstaller.start_installation()

        if (Config.installed_instance and 'installPassport' in Config.addPostSetupService) or (not Config.installed_instance and Config.installPassport):
            passportInstaller.start_installation()

        if not Config.installed_instance:
            # this will install only base
            radiusInstaller.start_installation()

        if (Config.installed_instance and 'installGluuRadius' in Config.addPostSetupService) or (not Config.installed_instance and Config.installGluuRadius):
            radiusInstaller.install_gluu_radius()

        gluuProgress.progress(PostSetup.service_name, "Saving properties")
        propertiesUtils.save_properties()
        time.sleep(2)

        for service in gluuProgress.services:
            if service['app_type'] == static.AppType.SERVICE:
                gluuProgress.progress(PostSetup.service_name, "Starting {}".format(service['name'].title()))
                time.sleep(2)
                service['object'].stop()
                service['object'].start()
                if service['name'] == 'oxauth' and Config.get('installOxAuthRP'):
                    gluuProgress.progress(PostSetup.service_name, "Starting Oxauth-rp")
                    service['object'].start('oxauth-rp')

        gluuProgress.progress(static.COMPLETED)

        if not GSA:
            print()
            for m in Config.post_messages:
                print(m)

    except:
        if GSA:
            gluuProgress.progress(static.ERROR  , str(traceback.format_exc()))

        base.logIt("FATAL", True, True)

if not GSA and proceed:
    do_installation()
    print('\n', static.colors.OKGREEN)
    msg_text = msg.post_installation if Config.installed_instance else msg.installation_completed.format(Config.hostname)
    print(msg_text)
    print('\n', static.colors.ENDC)
else:
    Config.thread_queue = queue
    GSA.do_installation = do_installation
    GSA.gluuInstaller = gluuInstaller
    GSA.jettyInstaller = jettyInstaller
    GSA.run()
    print('\033c')

# we need this for progress write last line
time.sleep(2)
