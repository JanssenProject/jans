import os
import glob
import shutil

from setup_app.config.config import Config
from setup_app.utils.setup_utils import SetupUtils

class JettyInstaller(SetupUtils):

    def __init__(self):
        pass

    def install(self):
        self.logIt("Installing jetty %s...")

        jetty_archive_list = glob.glob(os.path.join(self.distAppFolder, 'jetty-distribution-*.tar.gz'))

        if not jetty_archive_list:
            self.logIt("Jetty archive not found in {}. Exiting...".format(self.distAppFolder), True, True)

        jettyArchive = max(jetty_archive_list)

        jettyArchive_fn = os.path.basename(jettyArchive)
        jetty_regex = re.search('jetty-distribution-(\d*\.\d*)', jettyArchive_fn)
        
        if not jetty_regex:
            self.logIt("Can't determine Jetty version", True, True)

        jetty_dist = '/opt/jetty-' + jetty_regex.groups()[0]
        self.templateRenderingDict['jetty_dist'] = jetty_dist
        jettyTemp = os.path.join(jetty_dist, 'temp')
        self.run([self.cmd_mkdir, '-p', jettyTemp])
        self.run([self.cmd_chown, '-R', 'jetty:jetty', jettyTemp])

        try:
            self.logIt("Extracting %s into /opt/jetty" % jettyArchive)
            self.run(['tar', '-xzf', jettyArchive, '-C', jetty_dist, '--no-xattrs', '--no-same-owner', '--no-same-permissions'])
        except:
            self.logIt("Error encountered while extracting archive %s" % jettyArchive)
            self.logIt(traceback.format_exc(), True)


        jettyDestinationPath = max(glob.glob(os.path.join(jetty_dist, 'jetty-distribution-*')))

        self.run([self.cmd_ln, '-sf', jettyDestinationPath, self.jetty_home])
        self.run([self.cmd_chmod, '-R', "755", "%s/bin/" % jettyDestinationPath])

        self.applyChangesInFiles(self.app_custom_changes['jetty'])

        self.run([self.cmd_chown, '-R', 'jetty:jetty', jettyDestinationPath])
        self.run([self.cmd_chown, '-h', 'jetty:jetty', self.jetty_home])

        self.run([self.cmd_mkdir, '-p', self.jetty_base])
        self.run([self.cmd_chown, '-R', 'jetty:jetty', self.jetty_base])

        jettyRunFolder = '/var/run/jetty'
        self.run([self.cmd_mkdir, '-p', jettyRunFolder])
        self.run([self.cmd_chmod, '-R', '775', jettyRunFolder])
        self.run([self.cmd_chgrp, '-R', 'jetty', jettyRunFolder])

        self.run(['rm', '-rf', '/opt/jetty/bin/jetty.sh'])
        self.copyFile("%s/system/initd/jetty.sh" % self.staticFolder, "%s/bin/jetty.sh" % self.jetty_home)
        self.run([self.cmd_chown, '-R', 'jetty:jetty', "%s/bin/jetty.sh" % self.jetty_home])
        self.run([self.cmd_chmod, '-R', '755', "%s/bin/jetty.sh" % self.jetty_home])

    def create_user(self):
        self.createUser('jetty', self.jetty_user_home)
        self.addUserToGroup('gluu', 'jetty')

    def create_folders(self):
        self.run([self.cmd_mkdir, '-p', Config.jetty_user_home_lib])

    def installJettyService(self, serviceConfiguration, supportCustomizations=False, supportOnlyPageCustomizations=False):
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

            if not supportOnlyPageCustomizations:
                self.run([self.cmd_mkdir, '-p', "%s/custom/i18n" % jettyServiceBase])
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
        self.copyFile(jettyServiceConfiguration, self.osDefault)
        self.run([self.cmd_chown, 'root:root', os.path.join(self.osDefault, serviceName)])

        # Render web eources file
        try:
            web_resources = '%s_web_resources.xml' % serviceName
            if os.path.exists('%s/jetty/%s' % (self.templateFolder, web_resources)):
                self.renderTemplateInOut(web_resources, '%s/jetty' % self.templateFolder, '%s/jetty' % self.outputFolder)
                self.copyFile('%s/jetty/%s' % (self.outputFolder, web_resources), "%s/%s/webapps" % (self.jetty_base, serviceName))
        except:
            self.logIt("Error rendering service '%s' web_resources.xml" % serviceName, True)
            self.logIt(traceback.format_exc(), True)

        # Render web context file
        try:
            web_context = '%s.xml' % serviceName
            if os.path.exists('%s/jetty/%s' % (self.templateFolder, web_context)):
                self.renderTemplateInOut(web_context, '%s/jetty' % self.templateFolder, '%s/jetty' % self.outputFolder)
                self.copyFile('%s/jetty/%s' % (self.outputFolder, web_context), "%s/%s/webapps" % (self.jetty_base, serviceName))
        except:
            self.logIt("Error rendering service '%s' context xml" % serviceName, True)
            self.logIt(traceback.format_exc(), True)

        initscript_fn = os.path.join(self.jetty_home, 'bin/jetty.sh')
        self.fix_init_scripts(serviceName, initscript_fn)
        
        self.enable_service_at_start(serviceName)
        
        tmpfiles_base = '/usr/lib/tmpfiles.d'
        if self.os_initdaemon == 'systemd' and os.path.exists(tmpfiles_base):
            self.logIt("Creating 'jetty.conf' tmpfiles daemon file")
            jetty_tmpfiles_src = '%s/jetty.conf.tmpfiles.d' % self.templateFolder
            jetty_tmpfiles_dst = '%s/jetty.conf' % tmpfiles_base
            self.copyFile(jetty_tmpfiles_src, jetty_tmpfiles_dst)
            self.run([self.cmd_chown, 'root:root', jetty_tmpfiles_dst])
            self.run([self.cmd_chmod, '644', jetty_tmpfiles_dst])

        serviceConfiguration['installed'] = True

        # don't send header to server
        self.set_jetty_param(serviceName, 'jetty.httpConfig.sendServerVersion', 'false')

    def set_jetty_param(self, jettyServiceName, jetty_param, jetty_val):

        self.logIt("Seeting jetty parameter {0}={1} for service {2}".format(jetty_param, jetty_val, jettyServiceName))

        service_fn = os.path.join(self.jetty_base, jettyServiceName, 'start.ini')
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

        self.writeFile(service_fn, '\n'.join(start_ini_list))
