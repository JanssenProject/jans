import os.path
import Properties
import random
import shutil
import socket
import string
import time
import uuid
import json
import traceback
import subprocess
import sys
import getopt
import hashlib
import re
import glob
import base64

class Setup(object):

    def __init__(self, install_dir=None):
        self.install_dir = '.'
        self.cmd_ln = '/bin/ln'
        self.cmd_chmod = '/bin/chmod'
        self.cmd_chown = '/bin/chown'
        self.cmd_chgrp = '/bin/chgrp'
        self.cmd_mkdir = '/bin/mkdir'
        self.cmd_rpm = '/bin/rpm'
        self.cmd_dpkg = '/usr/bin/dpkg'
        self.opensslCommand = '/usr/bin/openssl'
        self.gluuBaseFolder = '/etc/gluu'
        self.outputFolder = '%s/output' % self.install_dir
        self.jython_home = '/opt/jython'
        self.jre_home = '/opt/jre'
        self.cmd_java = '%s/bin/java' % self.jre_home
        self.cmd_keytool = '%s/bin/keytool' % self.jre_home
        self.cmd_jar = '%s/bin/jar' % self.jre_home
        self.jetty_dist = '/opt/jetty-9.3'
        # Can change this value below
        self.application_max_ram = 3072
        self.gluuOptFolder = '/opt/gluu'
        self.jetty_home = '/opt/jetty'
        self.templateRenderingDict = {}
        self.jetty_base = '%s/jetty' % self.gluuOptFolder

        self.jetty_app_configuration = {
            'oxauth' : {'name' : 'oxauth',
                        'jetty' : {'modules' : 'deploy,http,logging,jsp,servlets,ext,http-forwarded,websocket'},
                        'memory' : {'ratio' : 0.3, "jvm_heap_ration" : 0.7, "max_allowed_mb" : 4096},
                        'installed' : False
                        },
            'identity' : {'name' : 'identity',
                            'jetty' : {'modules' : 'deploy,http,logging,jsp,ext,http-forwarded,websocket'},
                            'memory' : {'ratio' : 0.2, "jvm_heap_ration" : 0.7, "max_allowed_mb" : 2048},
                            'installed' : False
                            },
            'idp' : {'name' : 'idp',
                        'jetty' : {'modules' : 'deploy,http,logging,jsp,http-forwarded'},
                        'memory' : {'ratio' : 0.2, "jvm_heap_ration" : 0.7, "max_allowed_mb" : 1024},
                        'installed' : False
                        },
            'asimba' : {'name' : 'asimba',
                        'jetty' : {'modules' : 'deploy,http,logging,jsp,http-forwarded'},
                        'memory' : {'ratio' : 0.1, "jvm_heap_ration" : 0.7, "max_allowed_mb" : 1024},
                        'installed' : False
                        },
            'oxauth-rp' : {'name' : 'oxauth-rp',
                            'jetty' : {'modules' : 'deploy,http,logging,jsp,http-forwarded,websocket'},
                            'memory' : {'ratio' : 0.1, "jvm_heap_ration" : 0.7, "max_allowed_mb" : 512},
                            'installed' : False
                            },
            'passport' : {'name' : 'passport',
                            'node' : {},
                            'memory' : {'ratio' : 0.1, "max_allowed_mb" : 1024},
                            'installed' : False
                            }
        }
        self.os_types = ['centos', 'redhat', 'fedora', 'ubuntu', 'debian']
        self.os_type = None
        self.distFolder = '/opt/dist'
        self.distGluuFolder = '%s/gluu' % self.distFolder

        self.log = '%s/setup.log' % self.install_dir
        self.logError = '%s/setup_error.log' % self.install_dir
        self.oxPhotosFolder = "/var/ox/photos"
        self.oxTrustRemovedFolder = "/var/ox/identity/removed"
        self.oxTrustCacheRefreshFolder = "/var/ox/identity/cr-snapshots"

        self.run([self.cmd_mkdir, '-m', '775', '-p', self.oxPhotosFolder])
        self.run([self.cmd_mkdir, '-m', '775', '-p', self.oxTrustRemovedFolder])
        self.run([self.cmd_mkdir, '-m', '775', '-p', self.oxTrustCacheRefreshFolder])

        self.run([self.cmd_chown, '-R', 'root:gluu', self.oxPhotosFolder])
        self.run([self.cmd_chown, '-R', 'root:gluu', self.oxTrustRemovedFolder])
        self.run([self.cmd_chown, '-R', 'root:gluu', self.oxTrustCacheRefreshFolder])
        self.templateFolder = '%s/templates' % self.install_dir
        self.installOxTrust = True

    def install_oxtrust(self):

        self.logIt("Copying oxauth.war into jetty webapps folder...")

        jettyServiceName = 'identity'
        self.installJettyService(self.jetty_app_configuration[jettyServiceName], True)

        jettyServiceWebapps = '%s/%s/webapps' % (self.jetty_base, jettyServiceName)
        self.copyFile('%s/identity.war' % self.distGluuFolder, jettyServiceWebapps)

    def installJettyService(self, serviceConfiguration, supportCustomizations=False):
        serviceName = serviceConfiguration['name']
        self.logIt("Installing jetty service %s..." % serviceName)
        jettyServiceBase = '%s/%s' % (self.jetty_base, serviceName)
        jettyModules = serviceConfiguration['jetty']['modules']
        jettyModulesList = jettyModules.split(',')

        self.logIt("Preparing %s service base folders" % serviceName)
        self.run([self.cmd_mkdir, '-p', jettyServiceBase])

        # Create ./ext/lib folder for custom libraries only if installed Jetty "ext" module
        if "ext" in jettyModulesList:
            self.run([self.cmd_mkdir, '-p', "%s/lib/ext" % jettyServiceBase])

        # Create ./custom/pages and ./custom/static folders for custom pages and static resources, only if application supports them
        if supportCustomizations:
            if not os.path.exists("%s/custom" % jettyServiceBase):
                self.run([self.cmd_mkdir, '-p', "%s/custom" % jettyServiceBase])
            self.run([self.cmd_mkdir, '-p', "%s/custom/pages" % jettyServiceBase])
            self.run([self.cmd_mkdir, '-p', "%s/custom/static" % jettyServiceBase])
            self.run([self.cmd_mkdir, '-p', "%s/custom/libs" % jettyServiceBase])

        self.logIt("Preparing %s service base configuration" % serviceName)
        jettyEnv = os.environ.copy()
        jettyEnv['PATH'] = '%s/bin:' % self.jre_home + jettyEnv['PATH']

        self.run([self.cmd_java, '-jar', '%s/start.jar' % self.jetty_home, 'jetty.home=%s' % self.jetty_home, 'jetty.base=%s' % jettyServiceBase, '--add-to-start=%s' % jettyModules], None, jettyEnv)
        self.run([self.cmd_chown, '-R', 'jetty:jetty', jettyServiceBase])

        try:
            self.renderTemplateInOut(serviceName, '%s/jetty' % self.templateFolder, '%s/jetty' % self.outputFolder)
        except:
            self.logIt("Error rendering service '%s' defaults" % serviceName, True)
            self.logIt(traceback.format_exc(), True)

        jettyServiceConfiguration = '%s/jetty/%s' % (self.outputFolder, serviceName)
        self.copyFile(jettyServiceConfiguration, "/etc/default")
        self.run([self.cmd_chown, 'root:root', "/etc/default/%s" % serviceName])

        try:
            web_resources = '%s_web_resources.xml' % serviceName
            if os.path.exists('%s/jetty/%s' % (self.templateFolder, web_resources)):
                self.renderTemplateInOut(web_resources, '%s/jetty' % self.templateFolder, '%s/jetty' % self.outputFolder)
                self.copyFile('%s/jetty/%s' % (self.outputFolder, web_resources), self.jetty_base+"/"+serviceName+"/webapps")
        except:
            self.logIt("Error rendering service '%s' web_resources.xml" % serviceName, True)
            self.logIt(traceback.format_exc(), True)

        self.copyFile('%s/bin/jetty.sh' % self.jetty_home, '/etc/init.d/%s' % serviceName)
        source_string = '# Provides:          jetty'
        target_string = '# Provides:          %s' % serviceName
        self.run(['sed', '-i', 's/^%s/%s/' % (source_string, target_string), '/etc/init.d/%s' % serviceName])

        # Enable service autoload on Gluu-Server startup
        if self.os_type in ['centos', 'fedora', 'redhat']:
            if self.os_initdaemon == 'systemd':
                self.run(["/usr/bin/systemctl", 'enable', serviceName])
            else:
                self.run(["/sbin/chkconfig", serviceName, "on"])
        elif self.os_type in ['ubuntu', 'debian']:
            self.run(["/usr/sbin/update-rc.d", serviceName, 'defaults', '60', '20'])

        serviceConfiguration['installed'] = True

    def fomatWithDict(self, text, dictionary):
        text = re.sub(r"%([^\(])", r"%%\1", text)
        text = re.sub(r"%$", r"%%", text)  # There was a % at the end?

        return text % dictionary
 
    def merge_dicts(self, *dict_args):
        result = {}
        for dictionary in dict_args:
            result.update(dictionary)

        return result

    def renderTemplateInOut(self, filePath, templateFolder, outputFolder):
        self.logIt("Rendering template %s" % filePath)
        fn = os.path.split(filePath)[-1]
        f = open(os.path.join(templateFolder, fn))
        template_text = f.read()
        f.close()

        # Create output folder if needed
        if not os.path.exists(outputFolder):
            os.makedirs(outputFolder)

        newFn = open(os.path.join(outputFolder, fn), 'w+')
        newFn.write(self.fomatWithDict(template_text, self.merge_dicts(self.__dict__, self.templateRenderingDict)))
        newFn.close()

    def run(self, args, cwd=None, env=None, useWait=False, shell=False):
        self.logIt('Running: %s' % ' '.join(args))
        try:
            p = subprocess.Popen(args, stdout=subprocess.PIPE, stderr=subprocess.PIPE, cwd=cwd, env=env, shell=shell)
            if useWait:
                code = p.wait()
                self.logIt('Run: %s with result code: %d' % (' '.join(args), code) )
            else:
                output, err = p.communicate()
                if output:
                    self.logIt(output)
                if err:
                    self.logIt(err, True)
        except:
            self.logIt("Error running command : %s" % " ".join(args), True)
            self.logIt(traceback.format_exc(), True)

    def logIt(self, msg, errorLog=False):
        if errorLog:
            f = open(self.logError, 'a')
            f.write('%s %s\n' % (time.strftime('%X %x'), msg))
            f.close()
        f = open(self.log, 'a')
        f.write('%s %s\n' % (time.strftime('%X %x'), msg))
        f.close()

    def detect_os_type(self):
        # TODO: Change this to support more distros. For example according to
        # http://unix.stackexchange.com/questions/6345/how-can-i-get-distribution-name-and-version-number-in-a-simple-shell-script
        distro_info = self.readFile('/etc/redhat-release', False)
        if distro_info == None:
            distro_info = self.readFile('/etc/os-release')

        if 'CentOS' in distro_info:
            return self.os_types[0]
        elif 'Red Hat' in distro_info:
            return self.os_types[1]
        elif 'Ubuntu' in distro_info:
            return self.os_types[3]
        elif 'Debian' in distro_info:
            return self.os_types[4]

        else:
            return self.choose_from_list(self.os_types, "Operating System")

    def readFile(self, inFilePath, logError=True):
        inFilePathText = None

        try:
            f = open(inFilePath)
            inFilePathText = f.read()
            f.close
        except:
            if logError:
                self.logIt("Error reading %s" % inFilePathText, True)
                self.logIt(traceback.format_exc(), True)

        return inFilePathText

    def writeFile(self, outFilePath, text):
        inFilePathText = None

        try:
            f = open(outFilePath, 'w')
            f.write(text)
            f.close()
        except:
            self.logIt("Error writing %s" % inFilePathText, True)
            self.logIt(traceback.format_exc(), True)

        return inFilePathText
        
    def detect_initd(self):
        return open(os.path.join('/proc/1/status'), 'r').read().split()[1]

    def copyFile(self, inFile, destFolder):
        try:
            shutil.copy(inFile, destFolder)
            self.logIt("Copied %s to %s" % (inFile, destFolder))
        except:
            self.logIt("Error copying %s to %s" % (inFile, destFolder), True)
            self.logIt(traceback.format_exc(), True)

    def prepare_base64_extension_scripts(self):
        try:
            if not os.path.exists(self.extensionFolder):
                return None

            for extensionType in os.listdir(self.extensionFolder):
                extensionTypeFolder = os.path.join(self.extensionFolder, extensionType)
                if not os.path.isdir(extensionTypeFolder):
                    continue

                for scriptFile in os.listdir(extensionTypeFolder):
                    scriptFilePath = os.path.join(extensionTypeFolder, scriptFile)
                    base64ScriptFile = self.generate_base64_file(scriptFilePath, 1)

                    # Prepare key for dictionary
                    extensionScriptName = '%s_%s' % (extensionType, os.path.splitext(scriptFile)[0])
                    extensionScriptName = extensionScriptName.decode('utf-8').lower()

                    self.templateRenderingDict[extensionScriptName] = base64ScriptFile
                    self.logIt("Loaded script %s with type %s into %s" % (scriptFile, extensionType, extensionScriptName))

        except:
            self.logIt("Error loading scripts from %s" % self.extensionFolder, True)
            self.logIt(traceback.format_exc(), True)


    def calculate_selected_aplications_memory(self):
        installedComponents = []

        # Jetty apps
        if self.installOxTrust:
            installedComponents.append(self.jetty_app_configuration['identity'])

        self.calculate_aplications_memory(self.application_max_ram, self.jetty_app_configuration, installedComponents)

    def calculate_aplications_memory(self, application_max_ram, jetty_app_configuration, installedComponents):
        self.logIt("Calculating memory setting for applications")

        allowedApplicationsMemory = {}

        usedRatio = 0.001
        for installedComponent in installedComponents:
            usedRatio += installedComponent['memory']['ratio']

        ratioMultiplier = 1.0 + (1.0 - usedRatio)/usedRatio

        for installedComponent in installedComponents:
            allowedRatio = installedComponent['memory']['ratio'] * ratioMultiplier
            allowedMemory = int(round(allowedRatio * int(application_max_ram)))

            if allowedMemory > installedComponent['memory']['max_allowed_mb']:
                allowedMemory = installedComponent['memory']['max_allowed_mb']

            allowedApplicationsMemory[installedComponent['name']] = allowedMemory

        # Iterate through all components into order to prepare all keys
        for applicationName, applicationConfiguration in jetty_app_configuration.iteritems():
            if applicationName in allowedApplicationsMemory:
                applicationMemory = allowedApplicationsMemory.get(applicationName)
            else:
                # We uses this dummy value to render template properly of not installed application
                applicationMemory = 256

            self.templateRenderingDict["%s_max_mem" % applicationName] = applicationMemory

            if 'jvm_heap_ration' in applicationConfiguration['memory']:
                jvmHeapRation = applicationConfiguration['memory']['jvm_heap_ration']

                minHeapMem = 256
                maxHeapMem = int(applicationMemory * jvmHeapRation)
                if maxHeapMem < minHeapMem:
                    minHeapMem = maxHeapMem

                self.templateRenderingDict["%s_max_heap_mem" % applicationName] = maxHeapMem
                self.templateRenderingDict["%s_min_heap_mem" % applicationName] = minHeapMem

                self.templateRenderingDict["%s_max_meta_mem" % applicationName] = applicationMemory - self.templateRenderingDict["%s_max_heap_mem" % applicationName]

if __name__ == '__main__':
    install = Setup()
    install.calculate_selected_aplications_memory()
    install.install_oxtrust()
