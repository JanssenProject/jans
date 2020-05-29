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
from setup_app.utils.progress_bar import ProgressBar


from setup_app.setup_options import get_setup_options
from setup_app.utils import printVersion


from setup_app.utils.properties_utils import PropertiesUtils
from setup_app.utils.setup_utils import SetupUtils

from setup_app.installers.gluu import GluuInstaller
from setup_app.installers.oxd import OxdInstaller
from setup_app.installers.httpd import HttpdInstaller
from setup_app.installers.jre import JreInstaller
from setup_app.installers.jetty import JettyInstaller
from setup_app.installers.jython import JythonInstaller
from setup_app.installers.node import NodeInstaller
from setup_app.installers.oxauth import OxauthInstaller
from setup_app.installers.oxtrust import OxtrustInstaller
from setup_app.installers.scim import ScimInstaller
from setup_app.installers.passport import PassportInstaller
from setup_app.installers.opendj import OpenDjInstaller
from setup_app.installers.fido import FidoInstaller
from setup_app.installers.saml import SamlInstaller


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
Config.pbar = ProgressBar(cols=tty_columns, queue=thread_queue)


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


# initialize util classes
propertiesUtils = PropertiesUtils()
propertiesUtils.promptForProperties()
propertiesUtils.check_properties()


# initialize installers
jreInstaller = JreInstaller()
jettyInstaller = JettyInstaller()
jythonInstaller = JythonInstaller()
nodeInstaller = NodeInstaller()
oxauthInstaller = OxauthInstaller()
passportInstaller = PassportInstaller()
oxtrustInstaller = OxtrustInstaller()
scimInstaller = ScimInstaller()
openDjInstaller = OpenDjInstaller()
fidoInstaller = FidoInstaller()
samlInstaller = SamlInstaller()
oxdInstaller = OxdInstaller()

print()
print(gluuInstaller)

proceed = True
if not Config.noPrompt:
    proceed_prompt = input('Proceed with these values [Y|n] ').lower().strip()
    if proceed_prompt and proceed_prompt[0] !='y':
        proceed = False


if proceed:

    gluuInstaller.configureSystem()
    gluuInstaller.calculate_selected_aplications_memory()

    jreInstaller.start_installation()
    jettyInstaller.start_installation()
    jythonInstaller.start_installation()
    nodeInstaller.start_installation()
    gluuInstaller.make_salt()
    oxauthInstaller.make_oxauth_salt()
    gluuInstaller.copy_scripts()
    gluuInstaller.encode_passwords()
    gluuInstaller.encode_test_passwords()


    oxtrustInstaller.generate_api_configuration()
    scimInstaller.generate_configuration()

    Config.ldapCertFn = Config.opendj_cert_fn
    Config.ldapTrustStoreFn = Config.opendj_p12_fn
    Config.encoded_ldapTrustStorePass = Config.encoded_opendj_p12_pass
    Config.oxTrustConfigGeneration = 'true' if Config.installSaml else 'false'

    gluuInstaller.prepare_base64_extension_scripts()
    gluuInstaller.render_templates()
    gluuInstaller.generate_crypto()

    #TODO: Consider moving to oxauth installer
    gluuInstaller.generate_oxauth_openid_keys()

    gluuInstaller.generate_base64_configuration()
    gluuInstaller.render_configuration_template()
    gluuInstaller.update_hostname()

    gluuInstaller.set_ulimits()
    gluuInstaller.copy_output()
    gluuInstaller.setup_init_scripts()

    # Installing gluu components

    if Config.wrends_install:
        openDjInstaller.start_installation()

    if Config.installHttpd:
        httpdinstaller = HttpdInstaller()
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

    if Config.installPassport:
        passportInstaller.start_installation()

    """
    self.pbar.progress("gluu", "Installing Gluu components")
    self.install_gluu_components()
    self.pbar.progress("gluu", "Rendering test templates")
    self.render_test_templates()
    self.pbar.progress("gluu", "Copying static")
    self.copy_static()
    self.fix_systemd_script()
    self.pbar.progress("gluu", "Setting ownerships")
    self.set_ownership()
    self.pbar.progress("gluu", "Setting permissions")
    self.set_permissions()
    self.pbar.progress("gluu", "Starting services")
    self.start_services()
    self.pbar.progress("gluu", "Saving properties")
    self.save_properties()
    """





    
