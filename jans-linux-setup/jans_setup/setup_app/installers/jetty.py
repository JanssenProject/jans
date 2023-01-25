import os
import glob
import re
import shutil
import zipfile

import xml.etree.ElementTree as ET

from setup_app import paths
from setup_app.utils import base
from setup_app.static import AppType, InstallOption, SetupProfiles
from setup_app.config import Config
from setup_app.utils.setup_utils import SetupUtils
from setup_app.installers.base import BaseInstaller

NAME_STR = 'jetty'
WEBAPPS = 'webapps'

class JettyInstaller(BaseInstaller, SetupUtils):

    # let's borrow these variables from Config
    jetty_home = Config.jetty_home
    jetty_base = Config.jetty_base
    jetty_app_configuration = base.readJsonFile(os.path.join(paths.DATA_DIR, 'jetty_app_configuration.json'), ordered=True)

    jetty_link = 'https://repo1.maven.org/maven2/org/eclipse/jetty/jetty-home/{0}/jetty-home-{0}.tar.gz'.format(base.current_app.app_info['JETTY_VERSION'])
    source_files = [
            (os.path.join(Config.dist_app_dir, os.path.basename(jetty_link)), jetty_link),
            ]

    jetty_bin_sh_fn = os.path.join(jetty_home, 'bin/jetty.sh')

    def __init__(self):
        setattr(base.current_app, self.__class__.__name__, self)
        self.service_name = 'jetty'
        self.needdb = False # we don't need backend connection in this class
        self.install_var = 'installJetty'
        self.app_type = AppType.APPLICATION
        self.install_type = InstallOption.MANDATORY
        if not base.snap:
            self.register_progess()
        self.jetty_user_home = '/home/jetty'
        self.jetty_user_home_lib = os.path.join(self.jetty_user_home, 'lib')

        self.app_custom_changes = {
            NAME_STR : {
                'name' : NAME_STR,
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

        self.createUser(Config.jetty_user, self.jetty_user_home)
        self.addUserToGroup('jans', Config.jetty_user)
        self.run([paths.cmd_mkdir, '-p', self.jetty_user_home_lib])

        jettyArchive, jetty_dist = self.get_jetty_info()

        jettyTemp = os.path.join(jetty_dist, 'temp')
        self.run([paths.cmd_mkdir, '-p', jettyTemp])
        self.chown(jettyTemp, Config.jetty_user, Config.jetty_group, recursive=True)

        try:
            self.logIt("Extracting %s into /opt/jetty" % jettyArchive)
            self.run(['tar', '-xzf', jettyArchive, '-C', jetty_dist, '--no-xattrs', '--no-same-owner', '--no-same-permissions'])
        except:
            self.logIt("Error encountered while extracting archive %s" % jettyArchive)

        jettyDestinationPath = max(glob.glob(os.path.join(jetty_dist, '{}-*'.format(self.jetty_dist_string))))

        self.run([paths.cmd_ln, '-sf', jettyDestinationPath, self.jetty_home])
        self.run([paths.cmd_chmod, '-R', "755", "%s/bin/" % jettyDestinationPath])

        self.chown(jettyDestinationPath, Config.jetty_user, Config.jetty_group, recursive=True)

        self.applyChangesInFiles(self.app_custom_changes[NAME_STR])

        self.chown(jettyDestinationPath, Config.jetty_user, Config.jetty_group, recursive=True)
        self.run([paths.cmd_chown, '-h', '{}:{}'.format(Config.jetty_user, Config.jetty_group), self.jetty_home])

        self.run([paths.cmd_mkdir, '-p', self.jetty_base])
        self.chown(self.jetty_base, Config.jetty_user, Config.jetty_group, recursive=True)

        jettyRunFolder = '/var/run/jetty'
        self.run([paths.cmd_mkdir, '-p', jettyRunFolder])
        self.run([paths.cmd_chmod, '-R', '775', jettyRunFolder])
        self.run([paths.cmd_chgrp, '-R', Config.jetty_group, jettyRunFolder])

        self.run(['rm', '-rf', self.jetty_bin_sh_fn])
        self.copyFile("%s/system/initd/jetty.sh" % Config.staticFolder, self.jetty_bin_sh_fn)
        self.chown(self.jetty_bin_sh_fn, Config.jetty_user, Config.jetty_group, recursive=True)
        self.run([paths.cmd_chmod, '-R', '755', self.jetty_bin_sh_fn])
        
        self.chown(jetty_dist, Config.jetty_user, Config.jetty_group, recursive=True)

    def get_jetty_info(self):
        # first try latest versions
        self.jetty_dist_string = 'jetty-home'
        jetty_archive_list = glob.glob(os.path.join(Config.dist_app_dir, '{}-*.tar.gz'.format(self.jetty_dist_string)))

        if not jetty_archive_list:
            self.logIt("Jetty archive not found in {}. Exiting...".format(Config.dist_app_dir), True, True)

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
        return os.path.join(self.jetty_base, self.service_name, WEBAPPS, self.service_name+'.xml')


    def installJettyService(self, serviceConfiguration, supportCustomizations=False, supportOnlyPageCustomizations=False):
        service_name = serviceConfiguration['name']
        self.logIt("Installing jetty service %s..." % service_name)
        self.logIt("Deploying Jetty Service", pbar=service_name)
        self.get_jetty_info()
        jetty_service_base = '%s/%s' % (self.jetty_base, service_name)
        jetty_modules = serviceConfiguration[NAME_STR]['modules']

        jetty_modules_list = [m.strip() for m in jetty_modules.split(',')]
        if self.jetty_dist_string == 'jetty-home':
            if 'cdi-decorate' not in jetty_modules_list:
                jetty_modules_list.append('cdi-decorate')
            jetty_modules = ','.join(jetty_modules_list)

        if base.snap:
            Config.templateRenderingDict['jetty_dist'] = self.jetty_base
        else:
            # we need this, because this method may be called externally
            jettyArchive, jetty_dist = self.get_jetty_info()
            
        Config.templateRenderingDict['service_user'] = Config.jetty_user            

        self.logIt("Preparing %s service base folders" % service_name)
        self.run([paths.cmd_mkdir, '-p', jetty_service_base])

        # Create ./ext/lib folder for custom libraries only if installed Jetty "ext" module
        if "ext" in jetty_modules_list:
            self.run([paths.cmd_mkdir, '-p', "%s/lib/ext" % jetty_service_base])

        # Create ./custom/pages and ./custom/static folders for custom pages and static resources, only if application supports them
        if supportCustomizations:
            if not os.path.exists("%s/custom" % jetty_service_base):
                self.run([paths.cmd_mkdir, '-p', "%s/custom" % jetty_service_base])
            self.run([paths.cmd_mkdir, '-p', "%s/custom/pages" % jetty_service_base])

            if not supportOnlyPageCustomizations:
                self.run([paths.cmd_mkdir, '-p', "%s/custom/i18n" % jetty_service_base])
                self.run([paths.cmd_mkdir, '-p', "%s/custom/static" % jetty_service_base])
                self.run([paths.cmd_mkdir, '-p', "%s/custom/libs" % jetty_service_base])

        self.logIt("Preparing %s service base configuration" % service_name)
        jettyEnv = os.environ.copy()
        jettyEnv['PATH'] = '%s/bin:' % Config.jre_home + jettyEnv['PATH']

        self.run([Config.cmd_java, '-jar', '%s/start.jar' % self.jetty_home, 'jetty.home=%s' % self.jetty_home, 'jetty.base=%s' % jetty_service_base, '--add-module=%s' % jetty_modules], None, jettyEnv)
        self.chown(jetty_service_base, Config.jetty_user, Config.jetty_group, recursive=True)

        # make variables of this class accesible from Config
        self.update_rendering_dict()

        try:
            self.renderTemplateInOut(service_name, os.path.join(Config.templateFolder, NAME_STR), os.path.join(Config.output_dir, NAME_STR))
        except:
            self.logIt("Error rendering service '%s' defaults" % service_name, True)

        jetty_service_configuration = os.path.join(Config.output_dir, NAME_STR, service_name)
        self.copyFile(jetty_service_configuration, Config.os_default)
        self.chown(os.path.join(Config.os_default, service_name), Config.user_group)

        # Render web reources file
        try:
            web_resources = '%s_web_resources.xml' % service_name
            if os.path.exists(os.path.join(Config.templateFolder, NAME_STR, web_resources)):
                self.renderTemplateInOut(
                        web_resources,
                        os.path.join(Config.templateFolder, NAME_STR),
                        os.path.join(Config.output_dir, NAME_STR)
                        )
                self.copyFile(
                        os.path.join(Config.output_dir, NAME_STR, web_resources),
                        os.path.join(self.jetty_base, service_name, WEBAPPS)
                        )
        except:
            self.logIt("Error rendering service '%s' web_resources.xml" % service_name, True)

        # Render web context file
        try:
            web_context = '%s.xml' % service_name
            jetty_temp_dir = os.path.join(Config.templateFolder, 'jetty')
            if not os.path.exists(os.path.join(jetty_temp_dir, web_context)):
                web_context = 'default_webcontext.xml'

            self.renderTemplateInOut(
                    web_context,
                    os.path.join(Config.templateFolder, NAME_STR),
                    out_file=os.path.join(self.jetty_base, service_name, 'webapps/{}.xml'.format(service_name))
                )
        except:
            self.logIt("Error rendering service '%s' context xml" % service_name, True)

        if not base.snap:
            tmpfiles_base = '/usr/lib/tmpfiles.d'
            if Config.os_initdaemon == 'systemd' and os.path.exists(tmpfiles_base):
                self.logIt("Creating 'jetty.conf' tmpfiles daemon file")
                jetty_tmpfiles_src = '%s/jetty.conf.tmpfiles.d' % Config.templateFolder
                jetty_tmpfiles_dst = '%s/jetty.conf' % tmpfiles_base
                self.copyFile(jetty_tmpfiles_src, jetty_tmpfiles_dst)
                self.chown(jetty_tmpfiles_dst, Config.root_user, Config.root_group)
                self.run([paths.cmd_chmod, '644', jetty_tmpfiles_dst])

            self.copyFile(self.jetty_bin_sh_fn, os.path.join(Config.distFolder, 'scripts', service_name), backup=False)

        serviceConfiguration['installed'] = True

        # don't send header to server
        inifile = 'http.ini' if self.jetty_dist_string == 'jetty-home' else 'start.ini'
        self.set_jetty_param(service_name, 'jetty.httpConfig.sendServerVersion', 'false', inifile=inifile)

        if base.snap:
            run_dir = os.path.join(jetty_service_base, 'run')
            if not os.path.exists(run_dir):
                self.run([paths.cmd_mkdir, '-p', run_dir])

        self.write_webapps_xml()
        self.configure_extra_libs(self.source_files[0][0])

        if Config.profile == SetupProfiles.DISA_STIG:
            additional_rules = []
            self.fapolicyd_access(Config.templateRenderingDict['service_user'], jetty_service_base, additional_rules)

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
                                    ('install_scim_server', 'jans-scim'),
                                    ('installFido2', 'jans-fido2'),
                                    ('install_config_api', 'jans-config-api'),
                                    ('installEleven', 'jans-eleven'),
                                    ]:

            if Config.get(config_var) and service in self.jetty_app_configuration:
                installedComponents.append(self.jetty_app_configuration[service])

        return self.calculate_aplications_memory(Config.application_max_ram, self.jetty_app_configuration, installedComponents)

    def add_extra_class(self, class_path, xml_fn=None):
        current_plugins = self.get_plugins(xml_fn, paths=True)
        for cp in class_path.split(','):
            if os.path.basename(cp) not in ','.join(current_plugins):
                current_plugins.append(cp.strip())

        self.set_class_path(current_plugins, xml_fn)

    def set_class_path(self, paths, xml_fn=None):

        if not xml_fn:
            xml_fn = self.web_app_xml_fn

        tree = ET.parse(xml_fn)
        root = tree.getroot()

        for app_set in root.findall("Set"):
            if app_set.get('name') == 'extraClasspath':
                break
        else:
            app_set = ET.Element("Set")
            app_set.set('name', 'extraClasspath')

            root.append(app_set)

        app_set.text = ','.join(paths)

        with open(xml_fn, 'wb') as f:
            f.write(b'<?xml version="1.0" encoding="ISO-8859-1"?>\n')
            f.write(b'<!DOCTYPE Configure PUBLIC "-//Jetty//Configure//EN" "http://www.eclipse.org/jetty/configure_9_0.dtd">\n')
            f.write(ET.tostring(root, method='xml'))


    def get_plugins(self, xml_fn=None, paths=False):
        plugins = []
        if not xml_fn:
            xml_fn = self.web_app_xml_fn

        if os.path.exists(xml_fn):

            tree = ET.parse(xml_fn)
            root = tree.getroot()

            for app_set in root.findall("Set"):
                if app_set.get('name') == 'extraClasspath' and app_set.text:

                    path_list = app_set.text.split(',')
                    if paths:
                        return path_list

                    for plugin_path in path_list:
                        base_name = os.path.basename(plugin_path)
                        fname, fext = os.path.splitext(base_name)
                        plugins.append(fname.rstrip('plugin').rstrip('-'))

        return plugins

    def installed(self):
        return os.path.exists(os.path.join(Config.jetty_base, self.service_name, 'start.ini')) or os.path.exists(os.path.join(Config.jetty_base, self.service_name, 'start.d/server.ini'))

    def configure_extra_libs(self, target_war_fn):
        version_rec = re.compile('-(\d+)?\.')

        builtin_libs = []
        war_zip = zipfile.ZipFile(target_war_fn)
        for builtin_path in war_zip.namelist():
            if  builtin_path.endswith('.jar'):
                builtin_libs.append(os.path.basename( builtin_path))
        war_zip.close()

        def in_war(name):
            for fn in builtin_libs:
                if fn.startswith(name):
                     return fn

        common_lib_dir = None

        if Config.cb_install:
            common_lib_dir = base.current_app.CouchbaseInstaller.common_lib_dir

        elif Config.rdbm_install and Config.rdbm_type == 'spanner':
            common_lib_dir = base.current_app.RDBMInstaller.common_lib_dir

        if common_lib_dir:

            add_custom_lib_dir = []
            for extra_lib_fn in os.listdir(common_lib_dir):
                version_search = version_rec.search(extra_lib_fn)
                if version_search:
                    version_start_index = version_search.start()
                    name = extra_lib_fn[:version_start_index]
                    if not in_war(name):
                        add_custom_lib_dir.append(os.path.join(common_lib_dir, extra_lib_fn))

            self.add_extra_class(','.join(add_custom_lib_dir))
