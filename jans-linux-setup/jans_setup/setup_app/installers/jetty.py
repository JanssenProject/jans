import os
import glob
import re
import shutil
import xml.etree.ElementTree as ET

from setup_app import paths
from setup_app.utils import base
from setup_app.static import AppType, InstallOption
from setup_app.config import Config
from setup_app.utils.setup_utils import SetupUtils
from setup_app.installers.base import BaseInstaller

class JettyInstaller(BaseInstaller, SetupUtils):

    # let's borrow these variables from Config
    jetty_home = Config.jetty_home
    jetty_base = Config.jetty_base
    jetty_app_configuration = base.readJsonFile(os.path.join(paths.DATA_DIR, 'jetty_app_configuration.json'), ordered=True)

    def __init__(self):
        setattr(base.current_app, self.__class__.__name__, self)
        self.service_name = 'jetty'
        self.needdb = False # we don't need backend connection in this class
        self.install_var = 'installJetty'
        self.app_type = AppType.APPLICATION
        self.install_type = InstallOption.MONDATORY
        if not base.snap:
            self.register_progess()
        self.jetty_user_home = '/home/jetty'
        self.jetty_user_home_lib = os.path.join(self.jetty_user_home, 'lib')

        self.app_custom_changes = {
            'jetty' : {
                'name' : 'jetty',
                'files' : [
                    {
                        'path' : os.path.join(self.jetty_home, 'etc/webdefault.xml'),
                        'replace' : [
                            {
                                'pattern' : r'(\<param-name\>dirAllowed<\/param-name\>)(\s*)(\<param-value\>)true(\<\/param-value\>)',
                                'update' : r'\1\2\3false\4'
                            }
                        ]
                    },
                    {
                        'path' : os.path.join(self.jetty_home, 'etc/jetty.xml'),
                        'replace' : [
                            {
                                'pattern' : '<New id="DefaultHandler" class="org.eclipse.jetty.server.handler.DefaultHandler"/>',
                                'update' : '<New id="DefaultHandler" class="org.eclipse.jetty.server.handler.DefaultHandler">\n\t\t\t\t <Set name="showContexts">false</Set>\n\t\t\t </New>'
                            }
                        ]
                    }
                ]
            }
        }


    def install(self):

        self.createUser('jetty', self.jetty_user_home)
        self.addUserToGroup('jans', 'jetty')
        self.run([paths.cmd_mkdir, '-p', self.jetty_user_home_lib])

        jettyArchive, jetty_dist = self.get_jetty_info()

        jettyTemp = os.path.join(jetty_dist, 'temp')
        self.run([paths.cmd_mkdir, '-p', jettyTemp])
        self.run([paths.cmd_chown, '-R', 'jetty:jetty', jettyTemp])

        try:
            self.logIt("Extracting %s into /opt/jetty" % jettyArchive)
            self.run(['tar', '-xzf', jettyArchive, '-C', jetty_dist, '--no-xattrs', '--no-same-owner', '--no-same-permissions'])
        except:
            self.logIt("Error encountered while extracting archive %s" % jettyArchive)

        jettyDestinationPath = max(glob.glob(os.path.join(jetty_dist, '{}-*'.format(self.jetty_dist_string))))

        self.run([paths.cmd_ln, '-sf', jettyDestinationPath, self.jetty_home])
        self.run([paths.cmd_chmod, '-R', "755", "%s/bin/" % jettyDestinationPath])

        self.applyChangesInFiles(self.app_custom_changes['jetty'])

        self.run([paths.cmd_chown, '-R', 'jetty:jetty', jettyDestinationPath])
        self.run([paths.cmd_chown, '-h', 'jetty:jetty', self.jetty_home])

        self.run([paths.cmd_mkdir, '-p', self.jetty_base])
        self.run([paths.cmd_chown, '-R', 'jetty:jetty', self.jetty_base])

        jettyRunFolder = '/var/run/jetty'
        self.run([paths.cmd_mkdir, '-p', jettyRunFolder])
        self.run([paths.cmd_chmod, '-R', '775', jettyRunFolder])
        self.run([paths.cmd_chgrp, '-R', 'jetty', jettyRunFolder])

        self.run(['rm', '-rf', '/opt/jetty/bin/jetty.sh'])
        self.copyFile("%s/system/initd/jetty.sh" % Config.staticFolder, "%s/bin/jetty.sh" % self.jetty_home)
        self.run([paths.cmd_chown, '-R', 'jetty:jetty', "%s/bin/jetty.sh" % self.jetty_home])
        self.run([paths.cmd_chmod, '-R', '755', "%s/bin/jetty.sh" % self.jetty_home])

    def get_jetty_info(self):
        # first try latest versions
        self.jetty_dist_string = 'jetty-home'
        jetty_archive_list = glob.glob(os.path.join(Config.distAppFolder, '{}-*.tar.gz'.format(self.jetty_dist_string)))

        if not jetty_archive_list:
            self.logIt("Jetty archive not found in {}. Exiting...".format(Config.distAppFolder), True, True)

        jettyArchive = max(jetty_archive_list)

        jettyArchive_fn = os.path.basename(jettyArchive)
        jetty_regex = re.search('{}-(\d*\.\d*)'.format(self.jetty_dist_string), jettyArchive_fn)
        if not jetty_regex:
            self.logIt("Can't determine Jetty version", True, True)

        jetty_dist = '/opt/jetty-' + jetty_regex.groups()[0]
        Config.templateRenderingDict['jetty_dist'] = jetty_dist
        self.jetty_version_string = jetty_regex.groups()[0]

        return jettyArchive, jetty_dist

    @property
    def web_app_xml_fn(self):
        return os.path.join(self.jetty_base, self.service_name, 'webapps', self.service_name+'.xml')


    def installJettyService(self, serviceConfiguration, supportCustomizations=False, supportOnlyPageCustomizations=False):
        serviceName = serviceConfiguration['name']
        self.logIt("Installing jetty service %s..." % serviceName)

        self.get_jetty_info()
        jettyServiceBase = '%s/%s' % (self.jetty_base, serviceName)
        jettyModules = serviceConfiguration['jetty']['modules']
        jettyModulesList = jettyModules.split(',')

        jettyModulesList = [m.strip() for m in jettyModules.split(',')]
        if self.jetty_dist_string == 'jetty-home':
            if not 'cdi-decorate' in jettyModulesList:
                jettyModulesList.append('cdi-decorate')
            jettyModules = ','.join(jettyModulesList)

        if base.snap:
            Config.templateRenderingDict['jetty_dist'] = self.jetty_base
        else:
            # we need this, because this method may be called externally
            jettyArchive, jetty_dist = self.get_jetty_info()

        self.logIt("Preparing %s service base folders" % serviceName)
        self.run([paths.cmd_mkdir, '-p', jettyServiceBase])

        # Create ./ext/lib folder for custom libraries only if installed Jetty "ext" module
        if "ext" in jettyModulesList:
            self.run([paths.cmd_mkdir, '-p', "%s/lib/ext" % jettyServiceBase])

        # Create ./custom/pages and ./custom/static folders for custom pages and static resources, only if application supports them
        if supportCustomizations:
            if not os.path.exists("%s/custom" % jettyServiceBase):
                self.run([paths.cmd_mkdir, '-p', "%s/custom" % jettyServiceBase])
            self.run([paths.cmd_mkdir, '-p', "%s/custom/pages" % jettyServiceBase])

            if not supportOnlyPageCustomizations:
                self.run([paths.cmd_mkdir, '-p', "%s/custom/i18n" % jettyServiceBase])
                self.run([paths.cmd_mkdir, '-p', "%s/custom/static" % jettyServiceBase])
                self.run([paths.cmd_mkdir, '-p', "%s/custom/libs" % jettyServiceBase])

        self.logIt("Preparing %s service base configuration" % serviceName)
        jettyEnv = os.environ.copy()
        jettyEnv['PATH'] = '%s/bin:' % Config.jre_home + jettyEnv['PATH']

        self.run([Config.cmd_java, '-jar', '%s/start.jar' % self.jetty_home, 'jetty.home=%s' % self.jetty_home, 'jetty.base=%s' % jettyServiceBase, '--add-to-start=%s' % jettyModules], None, jettyEnv)
        self.run([paths.cmd_chown, '-R', 'jetty:jetty', jettyServiceBase])

        # make variables of this class accesible from Config
        self.update_rendering_dict()

        try:
            self.renderTemplateInOut(serviceName, '%s/jetty' % Config.templateFolder, '%s/jetty' % Config.outputFolder)
        except:
            self.logIt("Error rendering service '%s' defaults" % serviceName, True)

        jettyServiceConfiguration = '%s/jetty/%s' % (Config.outputFolder, serviceName)
        self.copyFile(jettyServiceConfiguration, Config.osDefault)
        self.run([paths.cmd_chown, 'root:root', os.path.join(Config.osDefault, serviceName)])

        # Render web eources file
        try:
            web_resources = '%s_web_resources.xml' % serviceName
            if os.path.exists('%s/jetty/%s' % (Config.templateFolder, web_resources)):
                self.renderTemplateInOut(web_resources, '%s/jetty' % Config.templateFolder, '%s/jetty' % Config.outputFolder)
                self.copyFile('%s/jetty/%s' % (Config.outputFolder, web_resources), "%s/%s/webapps" % (self.jetty_base, serviceName))
        except:
            self.logIt("Error rendering service '%s' web_resources.xml" % serviceName, True)

        # Render web context file
        try:
            web_context = '%s.xml' % serviceName
            if os.path.exists('%s/jetty/%s' % (Config.templateFolder, web_context)):
                self.renderTemplateInOut(web_context, '%s/jetty' % Config.templateFolder, '%s/jetty' % Config.outputFolder)
                self.copyFile('%s/jetty/%s' % (Config.outputFolder, web_context), "%s/%s/webapps" % (self.jetty_base, serviceName))
        except:
            self.logIt("Error rendering service '%s' context xml" % serviceName, True)

        initscript_fn = os.path.join(self.jetty_home, 'bin/jetty.sh')
        self.fix_init_scripts(serviceName, initscript_fn)

        if not base.snap:
            tmpfiles_base = '/usr/lib/tmpfiles.d'
            if Config.os_initdaemon == 'systemd' and os.path.exists(tmpfiles_base):
                self.logIt("Creating 'jetty.conf' tmpfiles daemon file")
                jetty_tmpfiles_src = '%s/jetty.conf.tmpfiles.d' % Config.templateFolder
                jetty_tmpfiles_dst = '%s/jetty.conf' % tmpfiles_base
                self.copyFile(jetty_tmpfiles_src, jetty_tmpfiles_dst)
                self.run([paths.cmd_chown, 'root:root', jetty_tmpfiles_dst])
                self.run([paths.cmd_chmod, '644', jetty_tmpfiles_dst])

            self.copyFile(os.path.join(self.jetty_home, 'bin/jetty.sh'), os.path.join(Config.distFolder, 'scripts', serviceName), backup=False)

        serviceConfiguration['installed'] = True

        # don't send header to server
        inifile = 'http.ini' if self.jetty_dist_string == 'jetty-home' else 'start.ini'
        self.set_jetty_param(serviceName, 'jetty.httpConfig.sendServerVersion', 'false', inifile=inifile)

        if base.snap:
            run_dir = os.path.join(jettyServiceBase, 'run')
            if not os.path.exists(run_dir):
                self.run([paths.cmd_mkdir, '-p', run_dir])

        self.write_webapps_xml()


    def set_jetty_param(self, jettyServiceName, jetty_param, jetty_val, inifile='start.ini'):

        self.logIt("Seeting jetty parameter {0}={1} for service {2}".format(jetty_param, jetty_val, jettyServiceName))

        path_list = [self.jetty_base, jettyServiceName, inifile]
        if inifile != 'start.ini':
            path_list.insert(-1, 'start.d')
        service_fn = os.path.join(*tuple(path_list))

        start_ini = self.readFile(service_fn)
        start_ini_list = start_ini.splitlines()
        param_ln = jetty_param + '=' + jetty_val

        for i, l in enumerate(start_ini_list[:]):
            if jetty_param in l and l[0]=='#':
                start_ini_list[i] = param_ln 
                break
            elif l.strip().startswith(jetty_param):
                start_ini_list[i] = param_ln
                break
        else:
            start_ini_list.append(param_ln)

        self.writeFile(service_fn, '\n'.join(start_ini_list), backup=False)

    def calculate_aplications_memory(self, application_max_ram, jetty_app_configuration, installedComponents):
        self.logIt("Calculating memory setting for applications")
        allowedApplicationsMemory = {}
        application_max_ram = int(application_max_ram)
        application_max_ram -= len(installedComponents) * 128

        retVal = True
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
        for applicationName, applicationConfiguration in jetty_app_configuration.items():
            if applicationName in allowedApplicationsMemory:
                applicationMemory = allowedApplicationsMemory.get(applicationName)
            else:
                # We uses this dummy value to render template properly of not installed application
                applicationMemory = 256

            Config.templateRenderingDict["%s_max_mem" % applicationName] = applicationMemory

            if 'jvm_heap_ration' in applicationConfiguration['memory']:
                jvmHeapRation = applicationConfiguration['memory']['jvm_heap_ration']

                minHeapMem = 256
                maxHeapMem = int(applicationMemory * jvmHeapRation)
                if maxHeapMem < minHeapMem:
                    minHeapMem = maxHeapMem

                Config.templateRenderingDict["%s_max_heap_mem" % applicationName] = maxHeapMem
                Config.templateRenderingDict["%s_min_heap_mem" % applicationName] = minHeapMem

                if maxHeapMem < 256 and applicationName in allowedApplicationsMemory:    
                    retVal = False

        return retVal


    def write_webapps_xml(self, jans_app_path=None, jans_apps=None):
        if not jans_app_path:
            jans_app_path = '/'+self.service_name
        if not jans_apps:
            jans_apps = self.service_name+'.war'

        web_apps_xml_fn = os.path.join(Config.templateFolder, 'jetty/jans-app.xml')
        web_apps_xml = self.readFile(web_apps_xml_fn)
        web_apps_xml = self.fomatWithDict(web_apps_xml, {'jans_app_path': jans_app_path, 'jans_apps': jans_apps})

        self.writeFile(self.web_app_xml_fn, web_apps_xml)

    def calculate_selected_aplications_memory(self):
        Config.pbar.progress("jans", "Calculating application memory")

        installedComponents = []

        # Jetty apps
        for config_var, service in [('installOxAuth', 'jans-auth'),
                                    ('installScimServer', 'jans-scim'),
                                    ('installFido2', 'jans-fido2'),
                                    ('installConfigApi', 'jans-config-api'),
                                    ('installEleven', 'jans-eleven')]:

            if Config.get(config_var) and service in self.jetty_app_configuration:
                installedComponents.append(self.jetty_app_configuration[service])

        return self.calculate_aplications_memory(Config.application_max_ram, self.jetty_app_configuration, installedComponents)

    def add_extra_class(self, class_path, xml_fn=None):
        if not xml_fn:
            xml_fn = self.web_app_xml_fn
        tree = ET.parse(xml_fn)
        root = tree.getroot()
        path_list = []

        for app_set in root.findall("Set"):
            if app_set.get('name') == 'extraClasspath':
                path_list = [cp.strip() for cp in app_set.text.split(',')]
                break
        else:
            app_set = ET.Element("Set")
            app_set.set('name', 'extraClasspath')

            root.append(app_set)

        for cp in class_path.split(','):
            if os.path.basename(cp) not in ','.join(path_list):
                path_list.append(cp.strip())

        app_set.text = ','.join(path_list)

        with open(xml_fn, 'wb') as f:
            f.write(b'<?xml version="1.0"  encoding="ISO-8859-1"?>\n')
            f.write(b'<!DOCTYPE Configure PUBLIC "-//Jetty//Configure//EN" "http://www.eclipse.org/jetty/configure_9_0.dtd">\n')
            f.write(ET.tostring(root, method='xml'))


    def get_plugins(self):
        plugins = []
        webapps_xml_fn = os.path.join(self.jetty_base, self.service_name, 'webapps', self.service_name+'.xml')

        if os.path.exists(webapps_xml_fn):

            tree = ET.parse(webapps_xml_fn)
            root = tree.getroot()

            for app_set in root.findall("Set"):
                if app_set.get('name') == 'extraClasspath':
                    for plugin_path in app_set.text.split(','):
                        base_name = os.path.basename(plugin_path)
                        n = base_name.rfind('plugin')
                        plugins.append(base_name[:n].rstrip('-'))

        return plugins

    def installed(self):
        return os.path.exists(os.path.join(Config.jetty_base, self.service_name, 'start.ini')) or os.path.exists(os.path.join(Config.jetty_base, self.service_name, 'start.d/server.ini'))
