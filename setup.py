#!/usr/bin/python3

import readline
import os
import sys
import time
import glob
import inspect
import zipfile

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


from setup_app.utils.properties_utils import PropertiesUtils
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


thread_queue = None
istty = False

# initialize config object
Config.init(paths.INSTALL_DIR)
Config.determine_version()

# we must initilize SetupUtils after initilizing Config
SetupUtils.init()

# get setup options from args
argsp, setupOptions = get_setup_options()

try:
    tty_rows, tty_columns = os.popen('stty size', 'r').read().split()
    istty = True
except:
    tty_rows = 60
    tty_columns = 120


if (not argsp.c) and istty and (int(tty_rows) > 24) and (int(tty_columns) > 79):
    try:
        import npyscreen
    except:
        print("Can't start TUI, continuing command line")
    else:
        from setup_app import tui
        thread_queue = tui.queue

if not argsp.n and not thread_queue:
    base.check_resources()
        

# initilize progress bar and pass to Config object
Config.pbar = gluuProgress


for key in setupOptions:
    setattr(Config, key, setupOptions[key])


gluuInstaller = GluuInstaller()
gluuInstaller.initialize()


Config.hostname = 'idp.mygluu.org'
Config.ip = '192.168.56.1'
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

print()
print("Installing Gluu Server...\n\nFor more info see:\n  {}  \n  {}\n".format(paths.LOG_FILE, paths.LOG_ERROR_FILE))
print("Detected OS     :  {} {}".format(base.os_type, base.os_version))
print("Gluu Version    :  {}".format(Config.oxVersion))
print("Detected init   :  {}".format(base.os_initdaemon))
print("Detected Apache :  {}".format(base.determineApacheVersion()))
print()

propertiesUtils = PropertiesUtils()

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

if not Config.noPrompt:
    propertiesUtils.promptForProperties()

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


if proceed:
    gluuProgress.start()
    gluuInstaller.configureSystem()
    gluuInstaller.make_salt()
    oxauthInstaller.make_oxauth_salt()
    
    jettyInstaller.calculate_selected_aplications_memory()
    jreInstaller.start_installation()
    jettyInstaller.start_installation()
    jythonInstaller.start_installation()
    nodeInstaller.start_installation()

    gluuInstaller.copy_scripts()
    gluuInstaller.encode_passwords()

    if Config.loadTestData:
        gluuInstaller.encode_test_passwords()

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

# we need this for progress write last line
time.sleep(2)


    
