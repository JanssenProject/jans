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

from setup_app.config import Config
from setup_app.utils.progress import gluuProgress


from setup_app.setup_options import get_setup_options
from setup_app.utils import printVersion

from setup_app.test_data_loader import TestDataLoader
from setup_app.utils.properties_utils import propertiesUtils
from setup_app.utils.setup_utils import SetupUtils

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


Config.hostname = 'c2.gluu.org'
Config.ip = '159.89.43.71'
Config.oxtrust_admin_password = 'Top!Secret-20'
Config.orgName = 'MyGluu'
Config.countryCode = 'GC'
Config.city = 'GluuCity'
Config.state = 'GluuState'
Config.admin_email = 'admin@mygluu.org'
Config.installPassport = True
Config.installFido2 = True
Config.installScimServer = True
Config.installSaml = True
Config.installOxd = True
Config.installPassport = True

if not GSA:
    print()
    print("Installing Gluu Server...\n\nFor more info see:\n  {}  \n  {}\n".format(paths.LOG_FILE, paths.LOG_ERROR_FILE))
    print("Detected OS     :  {} {}".format(base.os_type, base.os_version))
    print("Gluu Version    :  {}".format(Config.oxVersion))
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
elif os.path.isfile(Config.setup_properties_fn+'.enc'):
    base.logIt('%s Properties found!\n' % Config.setup_properties_fn+'.enc')
    setup_loaded = propertiesUtils.load_properties(Config.setup_properties_fn+'.enc')

if not Config.noPrompt and not GSA:
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
passportInstaller = PassportInstaller()
scimInstaller = ScimInstaller()
fidoInstaller = FidoInstaller()
samlInstaller = SamlInstaller()
oxdInstaller = OxdInstaller()
casaInstaller = CasaInstaller()
radiusInstaller = RadiusInstaller()

if argsp.t or argsp.x:
    testDataLoader = TestDataLoader()
    testDataLoader.passportInstaller = passportInstaller
    testDataLoader.scimInstaller = scimInstaller

if argsp.x:

    print("Loading test data")
    prop_file = os.path.join(Config.install_dir, 'setup.properties.last')

    if not os.path.exists(prop_file):
        prop_file += '.enc'
        if not os.path.exists(prop_file):
            print("setup.properties.last or setup.properties.last.enc were not found, exiting.")
            sys.exit(1)

    propertiesUtils.load_properties(prop_file)
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
        app_type = static.AppType.APPLICATION
        install_type = static.InstallOption.MONDATORY

gluuProgress.register(PostSetup)
gluuProgress.queue = queue

def do_installation():

    if not GSA:
        gluuProgress.start()

    try:
        gluuInstaller.configureSystem()
        gluuInstaller.make_salt()
        oxauthInstaller.make_salt()
        
        jettyInstaller.calculate_selected_aplications_memory()
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
        gluuInstaller.update_hostname()

        gluuInstaller.set_ulimits()
        gluuInstaller.copy_output()
        gluuInstaller.setup_init_scripts()

        # Installing gluu components

        if Config.wrends_install:
            openDjInstaller.start_installation()

        if Config.cb_install:
            couchbaseInstaller.start_installation()

        if Config.installHttpd:
            httpdinstaller.configure()

        if Config.installOxAuth:
            oxauthInstaller.start_installation()

        if Config.installOxAuthRP:
            oxauthInstaller.install_oxauth_rp()

        if Config.installFido2:
            fidoInstaller.start_installation()

        if Config.installOxTrust:
            oxtrustInstaller.start_installation()

        if Config.installScimServer:
            scimInstaller.start_installation()

        if Config.installSaml:
            samlInstaller.start_installation()

        if Config.installOxd:
            oxdInstaller.start_installation()

        if Config.installCasa:
            casaInstaller.start_installation()

        if Config.installPassport:
            passportInstaller.start_installation()

        # this will install only base
        radiusInstaller.start_installation()

        if Config.installGluuRadius:
            radiusInstaller.install_gluu_radius()

        gluuProgress.progress(PostSetup.service_name, "Saving properties")
        propertiesUtils.save_properties()

        for service in gluuProgress.services:
            if 'object' in service and service['app_type'] == static.AppType.SERVICE:
                gluuProgress.progress(PostSetup.service_name, "Starting {}".format(service['name'].title()))
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
    print("\n\n Gluu Server installation successful! Point your browser to https://%s\n\n" % Config.hostname)

else:
    GSA.do_installation = do_installation
    GSA.queue = queue
    GSA.run()

# we need this for progress write last line
time.sleep(2)
