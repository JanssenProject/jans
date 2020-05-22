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

from setup_app.config import Config, InstallTypes
from setup_app.utils.setup_utils import SetupUtils
from setup_app.utils.properties_utils import PropertiesUtils
from setup_app.utils import base


from setup_app.installers.oxd import OxdInstaller
from setup_app.installers.httpd import HttpdInstaller


cur_dir = os.path.dirname(os.path.realpath(__file__))

# initialize config object
Config.init(paths.INSTALL_DIR)
Config.determine_version()

# initialize setup utils
su = SetupUtils()

class GluuSetup(SetupUtils):

    def __init__(self):
        super().__init__()
    
    def __repr__(self):

        try:
            txt = 'hostname'.ljust(30) + Config.hostname.rjust(35) + "\n"
            txt += 'orgName'.ljust(30) + Config.orgName.rjust(35) + "\n"
            txt += 'os'.ljust(30) + Config.os_type.rjust(35) + "\n"
            txt += 'city'.ljust(30) + Config.city.rjust(35) + "\n"
            txt += 'state'.ljust(30) + Config.state.rjust(35) + "\n"
            txt += 'countryCode'.ljust(30) + Config.countryCode.rjust(35) + "\n"
            txt += 'Applications max ram'.ljust(30) + str(Config.application_max_ram).rjust(35) + "\n"
            txt += 'Install oxAuth'.ljust(30) + repr(Config.installOxAuth).rjust(35) + "\n"
            txt += 'Install oxTrust'.ljust(30) + repr(Config.installOxTrust).rjust(35) + "\n"

            bc = []
            if Config.wrends_install:
                t_ = 'wrends'
                if Config.wrends_install == InstallTypes.REMOTE:
                    t_ += '[R]'
                bc.append(t_)
            if Config.cb_install:
                t_ = 'couchbase'
                if Config.cb_install == InstallTypes.REMOTE:
                    t_ += '[R]'
                bc.append(t_)

            if bc:
                bct = ', '.join(bc)
                txt += 'Backends'.ljust(30) + bct.rjust(35) + "\n"

            txt += 'Java Type'.ljust(30) + Config.java_type.rjust(35) + "\n"
            txt += 'Install Apache 2 web server'.ljust(30) + repr(Config.installHttpd).rjust(35) + "\n"
            txt += 'Install Fido2 Server'.ljust(30) + repr(Config.installFido2).rjust(35) + "\n"
            txt += 'Install Scim Server'.ljust(30) + repr(Config.installScimServer).rjust(35) + "\n"
            txt += 'Install Shibboleth SAML IDP'.ljust(30) + repr(Config.installSaml).rjust(35) + "\n"
            txt += 'Install oxAuth RP'.ljust(30) + repr(Config.installOxAuthRP).rjust(35) + "\n"
            txt += 'Install Passport '.ljust(30) + repr(Config.installPassport).rjust(35) + "\n"
            txt += 'Install Casa '.ljust(30) + repr(Config.installCasa).rjust(35) + "\n"
            txt += 'Install Oxd '.ljust(30) + repr(Config.installOxd).rjust(35) + "\n"
            txt += 'Install Gluu Radius '.ljust(30) + repr(Config.installGluuRadius).rjust(35) + "\n"
            return txt
        except:
            s = ""
            for key in list(Config.__dict__):
                if not key in ('__dict__',):
                    val = getattr(Config, key)
                    if not inspect.ismethod(val):
                        s = s + "%s\n%s\n%s\n\n" % (key, "-" * len(key), val)
            return s


    def initialize(self):
        Config.install_time_ldap = time.strftime('%Y%m%d%H%M%SZ', time.gmtime(time.time()))
        if not os.path.exists(Config.distFolder):
            print("Please ensure that you are running this script inside Gluu container.")
            sys.exit(1)

        #TO DO: uncomment later
        #Download oxauth-client-jar-with-dependencies
        #if not os.path.exists(Config.non_setup_properties['oxauth_client_jar_fn']):
        #    oxauth_client_jar_url = 'https://ox.gluu.org/maven/org/gluu/oxauth-client/{0}/oxauth-client-{0}-jar-with-dependencies.jar'.format(Config.oxVersion)
        #    self.logIt("Downloading {}".format(os.path.basename(oxauth_client_jar_url)))
        #    self.run(['wget', '-nv', oxauth_client_jar_url, '-O', Config.non_setup_properties['oxauth_client_jar_fn']])

        self.logIt("Determining key generator path")
        oxauth_client_jar_zf = zipfile.ZipFile(Config.non_setup_properties['oxauth_client_jar_fn'])

        for f in oxauth_client_jar_zf.namelist():
            if os.path.basename(f) == 'KeyGenerator.class':
                p, e = os.path.splitext(f)
                Config.non_setup_properties['key_gen_path'] = p.replace(os.path.sep, '.')
            elif os.path.basename(f) == 'KeyExporter.class':
                p, e = os.path.splitext(f)
                Config.non_setup_properties['key_export_path'] = p.replace(os.path.sep, '.')

        if (not 'key_gen_path' in Config.non_setup_properties) or (not 'key_export_path' in Config.non_setup_properties):
            self.logIt("Can't determine key generator and/or key exporter path form {}".format(Config.non_setup_properties['oxauth_client_jar_fn']), True, True)
        else:
            self.logIt("Key generator path was determined as {}".format(Config.non_setup_properties['key_export_path']))


gs = GluuSetup()

#load properties
Config.properties_password = 'TopSecret'

pu = PropertiesUtils()
pu.load_properties(
            prop_file=os.path.join(paths.INSTALL_DIR, 'setup.properties.last.enc'),
            no_update=['log','logError','install_dir', 'savedProperties']
        )

print(Config.properties_password)
#Config.oxtrust_admin_password = 'TopSecret'
#pu.save_properties()

gs.initialize()
pu.check_properties()

print(gs)
#http_installer_obj = HttpdInstaller()
#http_installer_obj.configure()

#oxd = OxdInstaller()
print(Config.hostname)
#oxd.install()
#oxd.enable()
    
