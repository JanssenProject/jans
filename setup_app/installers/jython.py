import os
import glob
import shutil

from setup_app.config.config import Config
from setup_app.utils.setup_utils import SetupUtils

class NodeInstaller(SetupUtils):

    def __init__(self):
        pass

    def installJython(self):
        self.logIt("Installing Jython")

        jython_installer_list = glob.glob(os.path.join(self.distAppFolder, 'jython-installer-*'))

        if not jython_installer_list:
            self.logIt("Jython installer not found in. Exiting...", True, True)

        jython_installer = max(jython_installer_list)
        jython_version_regex = re.search('jython-installer-(.*)\.jar', jython_installer)
        
        if not jython_version_regex:
            self.logIt("Jython installer not found in. Exiting...", True, True)

        jython_version = jython_version_regex.groups()[0]

        try:
            self.run(['rm', '-rf', '/opt*-%s' % jython_version])
            self.run([self.cmd_java, '-jar', jython_installer, '-v', '-s', '-d', '/opt/jython-%s' % jython_version, '-t', 'standard', '-e', 'ensurepip'])
        except:
            self.logIt("Error installing jython-installer-%s.jar" % jython_version)
            self.logIt(traceback.format_exc(), True)

        self.run([self.cmd_ln, '-sf', '/opt/jython-%s' % jython_version, self.jython_home])
        self.run([self.cmd_chown, '-R', 'root:root', '/opt/jython-%s' % jython_version])
        self.run([self.cmd_chown, '-h', 'root:root', self.jython_home])
