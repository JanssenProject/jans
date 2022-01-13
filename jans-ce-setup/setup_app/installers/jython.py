import os
import glob
import re

from setup_app import paths
from setup_app.utils import base
from setup_app.static import AppType, InstallOption
from setup_app.config import Config
from setup_app.utils.setup_utils import SetupUtils
from setup_app.installers.base import BaseInstaller

class JythonInstaller(BaseInstaller, SetupUtils):

    def __init__(self):
        setattr(base.current_app, self.__class__.__name__, self)
        self.service_name = 'jython'
        self.install_var = 'installJython'
        self.app_type = AppType.APPLICATION
        self.install_type = InstallOption.MONDATORY
        if not base.snap:
            self.register_progess()

        self.needdb = False # we don't need backend connection in this class

    def install(self):

        jython_installer_list = glob.glob(os.path.join(Config.distAppFolder, 'jython-installer-*'))

        if not jython_installer_list:
            self.logIt("Jython installer not found in. Exiting...", True, True)

        jython_installer = max(jython_installer_list)
        jython_version_regex = re.search('jython-installer-(.*)\.jar', jython_installer)
        
        if not jython_version_regex:
            self.logIt("Jython installer not found in. Exiting...", True, True)

        jython_version = jython_version_regex.groups()[0]

        try:
            self.run(['rm', '-rf', '/opt*-%s' % jython_version])
            self.run([Config.cmd_java, '-jar', jython_installer, '-v', '-s', '-d', '/opt/jython-%s' % jython_version, '-t', 'standard', '-e', 'ensurepip'])
        except:
            self.logIt("Error installing jython-installer-%s.jar" % jython_version)

        self.run([paths.cmd_ln, '-sf', '/opt/jython-%s' % jython_version, Config.jython_home])
        self.run([paths.cmd_chown, '-R', 'root:root', '/opt/jython-%s' % jython_version])
        self.run([paths.cmd_chown, '-h', 'root:root', Config.jython_home])
