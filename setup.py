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

from setup_app.installers.gluu import GluuInstaller
from setup_app.installers.oxd import OxdInstaller
from setup_app.installers.httpd import HttpdInstaller
from setup_app.installers.jre import JreInstaller
thread_queue = None
istty = False

# initialize config object
Config.init(paths.INSTALL_DIR)
Config.determine_version()


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


#Config.hostname = 'idp.mygluu.org'
#Config.ip = '192.168.56.1'
Config.oxtrust_admin_password = 'Top!Secret-20'
Config.orgName = 'MyGluu'
Config.countryCode = 'GC'
Config.city = 'GluuCity'
Config.state = 'GluuState'
Config.admin_email = 'admin@mygluu.org'

print()
print("Installing Gluu Server...\n\nFor more info see:\n  {}  \n  {}\n".format(paths.LOG_FILE, paths.LOG_ERROR_FILE))
print("Detected OS     :  {} {}".format(base.os_type, base.os_version))
print("Gluu Version    :  {}".format(Config.oxVersion))
print("Detected init   :  {}".format(base.os_initdaemon))
print("Detected Apache :  {}".format(base.determineApacheVersion()))
print()


# We need OxdInstaller initilized before prompting properties, since oxd package is determined by OxdInstaller
oxdInstaller = OxdInstaller()

jreInstaller = JreInstaller()

propertiesUtils = PropertiesUtils()
propertiesUtils.promptForProperties()
propertiesUtils.check_properties()
print()

print(gluuInstaller)

proceed = True
if not Config.noPrompt:
    proceed_prompt = input('Proceed with these values [Y|n] ').lower().strip()
    if proceed_prompt and proceed_prompt[0] !='y':
        proceed = False


if proceed:

    Config.pbar.progress("gluu", "Configuring system")
    gluuInstaller.configureSystem()

    Config.pbar.progress("gluu", "Calculating application memory")
    gluuInstaller.calculate_selected_aplications_memory()

    jreInstaller.start_installation()

    """
    self.pbar.progress("jetty", "Installing Jetty")
    self.installJetty()
    self.pbar.progress("jython", "Installing Jython")
    self.installJython()
    self.pbar.progress("node", "Installing Node")
    self.installNode()
    self.pbar.progress("gluu", "Making salt")
    self.make_salt()
    self.pbar.progress("gluu", "Making oxauth salt")
    self.make_oxauth_salt()
    self.pbar.progress("scripts", "Copying scripts")
    self.copy_scripts()
    self.pbar.progress("gluu", "Encoding passwords")
    self.encode_passwords()
    self.pbar.progress("gluu", "Encoding test passwords")
    self.encode_test_passwords()
    
    if self.installPassport:
        self.generate_passport_configuration()
    
    self.pbar.progress("gluu", "Installing Gluu base")
    self.install_gluu_base()
    self.pbar.progress("gluu", "Preparing base64 extention scripts")
    self.prepare_base64_extension_scripts()
    self.pbar.progress("gluu", "Rendering templates")
    self.render_templates()
    self.pbar.progress("gluu", "Generating crypto")
    self.generate_crypto()
    self.pbar.progress("gluu","Generating oxauth openid keys")
    self.generate_oxauth_openid_keys()
    self.pbar.progress("gluu", "Generating base64 configuration")
    self.generate_base64_configuration()
    self.pbar.progress("gluu", "Rendering configuratipn template")
    self.render_configuration_template()
    self.pbar.progress("gluu", "Updating hostname")
    self.update_hostname()
    self.pbar.progress("gluu", "Setting ulimits")
    self.set_ulimits()
    self.pbar.progress("gluu", "Copying output")
    self.copy_output()
    self.pbar.progress("gluu", "Setting up init scripts")
    self.setup_init_scripts()
    self.pbar.progress("node", "Rendering node templates")
    self.render_node_templates()
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



#http_installer_obj = HttpdInstaller()
#http_installer_obj.configure()


    
