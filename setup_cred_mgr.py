#!/usr/bin/python

# The MIT License (MIT)
#
# Copyright (c) 2014 Gluu
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.


import os.path
import Properties
import string
import time
import json
import traceback
import sys
import getopt

from setup import *

class SetupCredManager(object):
    def __init__(self, setup):

        self.setup = setup
        self.install_dir = self.setup.install_dir

        self.log = '%s/setup_cred_mgr.log' % self.install_dir
        self.logError = '%s/setup_cred_mgr_error.log' % self.install_dir

        self.setup_properties_fn = '%s/setup_cred_mgr.properties' % self.install_dir
        self.savedProperties = '%s/setup_cred_mgr.properties.last' % self.install_dir

        self.twilio_jar = 'twilio-7.17.0.jar'
        self.twilio_url = 'http://central.maven.org/maven2/com/twilio/sdk/twilio/7.17.0/%s' % self.twilio_jar

        self.application_max_ram = None    # in MB

        self.templateRenderingDict = {}

        # Gluu components installation status
        self.installCredManager = False

        self.jetty_app_configuration = {
            'cred-manager' : {'name' : 'cred-manager',
                           'jetty' : {'modules' : 'deploy,http,logging,jsp,http-forwarded'},
                           'memory' : {'ratio' : 1, "jvm_heap_ration" : 0.7, "max_allowed_mb" : 1024},
                           'installed' : False
                           }
        }

        self.oxd_hostname = None
        self.oxd_port = 8098

        self.cred_manager_config = '%s/cred-manager.json' % self.setup.outputFolder

        self.init_fixes = {
                'cred-manager' : {'src_pattern' : 'S*cred-manager',
                            'result_name' : 'S97cred-manager'
                           }
        }

    def __repr__(self):
        try:
            return 'Install Credentials manager '.ljust(30) + repr(self.installCredManager).rjust(35) + "\n"
        except:
            s = ""
            for key in self.__dict__.keys():
                val = self.__dict__[key]
                s = s + "%s\n%s\n%s\n\n" % (key, "-" * len(key), val)
            return s

    def check_properties(self):
        self.setup.logIt('Checking properties')
        if not self.application_max_ram:
            self.application_max_ram = 512

    def download_files(self):
        self.setup.logIt("Downloading files")
        if self.installCredManager:
            # Credential manager is not part of CE package. We need to download it if needed
            distCredManagerPath = '%s/%s' % (self.setup.distGluuFolder, "cred-manager.war")
            if not os.path.exists(distCredManagerPath):
                print "Downloading Credential manager war file..."
                self.setup.run(['/usr/bin/wget', self.setup.cred_manager_war, '--no-verbose', '--retry-connrefused', '--tries=10', '-O', distCredManagerPath])

            twilioJarPath = '%s/%s' % (self.setup.distGluuFolder, self.twilio_jar)
            if not os.path.exists(twilioJarPath):
                print "Downloading Twilio jar file..."
                self.setup.run(['/usr/bin/wget', self.twilio_url, '--no-verbose', '--retry-connrefused', '--tries=10', '-O', twilioJarPath])

    def install_cred_manager(self):
        credManagerWar = 'cred-manager.war'

        self.setup.logIt("Configuring Credentials manager...")
        self.setup.copyFile(self.setup.cred_manager_config, self.setup.configFolder)

        self.setup.logIt("Copying cred-manager.war into jetty webapps folder...")

        jettyServiceName = 'cred-manager'
        self.setup.installJettyService(self.jetty_app_configuration[jettyServiceName])

        jettyServiceWebapps = '%s/%s/webapps' % (self.setup.jetty_base, jettyServiceName)
        self.setup.copyFile('%s/cred-manager.war' % self.setup.distGluuFolder, jettyServiceWebapps)

        jettyServiceOxAuthCustomLibsPath = '%s/%s/%s' % (self.setup.jetty_base, "oxauth", "custom/libs")
        self.setup.copyFile('%s/%s' % ( self.setup.distGluuFolder, self.twilio_jar) , jettyServiceOxAuthCustomLibsPath)
        self.run([self.cmd_chown, '-R', 'jetty:jetty', jettyServiceOxAuthCustomLibsPath])

    def install_gluu_components(self):
        if self.installCredManager:
            self.install_cred_manager()

    def render_templates(self):
        self.setup.logIt("Rendering templates")
        
        # Merge dictionaries
        self.setup.templateRenderingDict = self.setup.merge_dicts(self.templateRenderingDict, self.setup.templateRenderingDict)

        # Render Jetty templates
        if self.installCredManager:
            try:
                self.setup.renderTemplateInOut("cred-manager", '%s/jetty' % self.setup.templateFolder, '%s/jetty' % self.setup.outputFolder)
                self.setup.renderTemplateInOut("cred-manager.json", self.setup.outputFolder, '%s/result' % self.setup.outputFolder)
            except:
                self.setup.logIt("Error writing Credentials manager templates", True)
                self.setup.logIt(traceback.format_exc(), True)

    def promptForProperties(self):
        self.application_max_ram = self.setup.getPrompt("Enter maximum RAM for applications in MB", '512')

        promptForCredManager = self.setup.getPrompt("Install Credentials Manager?", "Yes")[0].lower()
        if promptForCredManager == 'y':
            self.installCredManager = True

            self.oxd_hostname = self.setup.getPrompt("Enter oxd server hostname")
            self.oxd_port = self.setup.getPrompt("Enter oxd server port", 8098)
        else:
            self.installCredManager = False

        promptForMITLicense = self.setup.getPrompt("Do you acknowledge that use of the Credentials Manager is under the MIT license?","y|N")[0].lower()
        if promptForMITLicense == 'n':
            sys.exit(0)

    def save_properties(self):
        self.setup.save_properties()

    def load_properties(self, fn):
        self.setup.load_properties(fn)

    def start_services(self):
        # Jetty services
        # Iterate through all components and start installed
        for applicationName, applicationConfiguration in self.jetty_app_configuration.iteritems():
            if applicationConfiguration['installed']:
                self.setup.run_service_command(applicationName, 'start')

        # Restart oxAuth service to load new custom libs
        self.setup.run_service_command('oxauth', 'restart')

    def import_ldif_openldap(self):
        self.setup.logIt("Importing LDIF files into OpenLDAP")
        if self.installCredManager:
            ldif = self.setup.ldif_scripts_cred_manager

            self.setup.import_ldif_template(ldif)

    def calculate_aplications_memory(self):
        installedComponents = []

        # Jetty apps
        if self.installCredManager:
            installedComponents.append(self.jetty_app_configuration['cred-manager'])
            
        self.setup.calculate_aplications_memory(self.application_max_ram, self.jetty_app_configuration, installedComponents)

    def check_installed(self):
        return os.path.exists('%s/cred-manager.json' % self.setup.configFolder)

    def set_ownership(self):
        self.setup.set_ownership()

    def set_permissions(self):
        self.setup.set_permissions()

    def change_rc_links(self):
        self.setup.change_rc_links(self.init_fixes)

############################   Main Loop   #################################################

def print_help():
    print "\nUse setup_cred_mgr.py to configure Credentials Manager and to add initial data required for"
    print "start. If setup_cred_mgr.properties is found in this folder, these"
    print "properties will automatically be used instead of the interactive setup"
    print "Options:"
    print ""
    print "    -c   Install Credentials Manager"

def getOpts(argv, setupOptions):
    try:
        opts, args = getopt.getopt(argv, "c", [])
    except getopt.GetoptError:
        print_help()
        sys.exit(2)
    for opt, arg in opts:
        if opt == '-c':
            setupOptions['installCredManager'] = True

    return setupOptions

if __name__ == '__main__':
    setupOptions = {
        'install_dir': '.',
        'setup_properties': None,
        'noPrompt': False,
        'installCredManager': False
    }
    if len(sys.argv) > 1:
        setupOptions = getOpts(sys.argv[1:], setupOptions)

    setupObject = Setup(setupOptions['install_dir'])
    installObject = SetupCredManager(setupObject)

    # Configure log redirect
    setupObject.logError = installObject.logError
    setupObject.log = installObject.log

    if installObject.check_installed():
        print "\nThis instance already configured. If you need to install new one you should reinstall package first."
        sys.exit(2)

    installObject.installCredManager = setupOptions['installCredManager']

    # Get the OS and init type
    setupObject.os_type = setupObject.detect_os_type()
    setupObject.os_initdaemon = setupObject.detect_initd()

    print "\nInstalling Credentials Manager..."
    print "Detected OS  :  %s" % setupObject.os_type
    print "Detected init:  %s" % setupObject.os_initdaemon

    print "\nInstalling Credentials Manager...\n\nFor more info see:\n  %s  \n  %s\n" % (installObject.log, installObject.logError)
    print "\n** All clear text passwords contained in %s.\n" % installObject.savedProperties
    try:
        os.remove(installObject.log)
        setupObject.logIt('Removed %s' % installObject.log)
    except:
        pass
    try:
        os.remove(installObject.logError)
        setupObject.logIt('Removed %s' % installObject.logError)
    except:
        pass

    setupObject.logIt("Installing Credentials Manager", True)

    if setupOptions['setup_properties']:
        setupObject.logIt('%s Properties found!\n' % setupOptions['setup_properties'])
        installObject.load_properties(setupOptions['setup_properties'])
    elif os.path.isfile(installObject.setup_properties_fn):
        setupObject.logIt('%s Properties found!\n' % installObject.setup_properties_fn)
        installObject.load_properties(installObject.setup_properties_fn)
    else:
        setupObject.logIt("%s Properties not found. Interactive setup commencing..." % installObject.setup_properties_fn)
        installObject.promptForProperties()

    # Validate Properties
    installObject.check_properties()

    # Show to properties for approval
    print '\n%s\n' % `installObject`
    proceed = "NO"
    if not setupOptions['noPrompt']:
        proceed = raw_input('Proceed with these values [Y|n] ').lower().strip()
    if (setupOptions['noPrompt'] or not len(proceed) or (len(proceed) and (proceed[0] == 'y'))):
        try:
            installObject.download_files()
            installObject.calculate_aplications_memory()
            installObject.render_templates()
            installObject.install_gluu_components()
            installObject.set_ownership()
            installObject.set_permissions()
            installObject.import_ldif_openldap()
            installObject.start_services()
            installObject.change_rc_links()
            installObject.save_properties()

        except:
            setupObject.logIt("***** Error caught in main loop *****", True)
            setupObject.logIt(traceback.format_exc(), True)
        print "\n\n Credentials Manager installation successful! Point your browser to https://%s\n\n" % installObject.setup.hostname
    else:
        installObject.save_properties()
        print "Properties saved to %s. Change filename to %s if you want to re-use" % \
              (installObject.savedProperties, installObject.setup_properties_fn)

# END
