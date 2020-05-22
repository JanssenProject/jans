import os
import glob
import shutil

from setup_app.config.config import Config
from setup_app.utils.setup_utils import SetupUtils

class NodeInstaller(SetupUtils):

    def __init__(self):
        pass

    def install(self):
        self.logIt("Installing node %s..." )

        node_archieve_list = glob.glob(os.path.join(self.distAppFolder, 'node-*-linux-x64.tar.xz'))

        if not node_archieve_list:
            self.logIt("Can't find node archive", True, True)

        nodeArchive = max(node_archieve_list)

        try:
            self.logIt("Extracting %s into /opt" % nodeArchive)
            self.run(['tar', '-xJf', nodeArchive, '-C', '/opt/', '--no-xattrs', '--no-same-owner', '--no-same-permissions'])
        except:
            self.logIt("Error encountered while extracting archive %s" % nodeArchive)
            self.logIt(traceback.format_exc(), True)

        nodeDestinationPath = max(glob.glob('/opt/node-*-linux-x64'))

        self.run([self.cmd_ln, '-sf', nodeDestinationPath, self.node_home])
        self.run([self.cmd_chmod, '-R', "755", "%s/bin/" % nodeDestinationPath])

        # Create temp folder
        self.run([self.cmd_mkdir, '-p', "%s/temp" % self.node_home])

        # Copy init.d script
        self.copyFile(self.node_initd_script, self.gluuOptSystemFolder)
        self.copyFile(self.passport_initd_script, self.gluuOptSystemFolder)
        self.run([self.cmd_chmod, '-R', "755", "%s/node" % self.gluuOptSystemFolder])
        self.run([self.cmd_chmod, '-R', "755", "%s/passport" % self.gluuOptSystemFolder])

        self.run([self.cmd_chown, '-R', 'node:node', nodeDestinationPath])
        self.run([self.cmd_chown, '-h', 'node:node', self.node_home])

        self.run([self.cmd_mkdir, '-p', self.node_base])
        self.run([self.cmd_chown, '-R', 'node:node', self.node_base])


    def installNodeService(self, serviceName):
        self.logIt("Installing node service %s..." % serviceName)

        nodeServiceConfiguration = '%s/node/%s' % (self.outputFolder, serviceName)
        self.copyFile(nodeServiceConfiguration, self.osDefault)
        self.run([self.cmd_chown, 'root:root', os.path.join(self.osDefault, serviceName)])

        if serviceName == 'passport':
            initscript_fn = os.path.join(self.gluuOptSystemFolder, serviceName)
            self.fix_init_scripts(serviceName, initscript_fn)
        else:
            self.run([self.cmd_ln, '-sf', '%s/node' % self.gluuOptSystemFolder, '/etc/init.d/%s' % serviceName])

    def create_user(self):
        self.createUser('node', self.node_user_home)
        self.addUserToGroup('gluu', 'node')
