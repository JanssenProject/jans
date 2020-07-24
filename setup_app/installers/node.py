import os
import glob

from setup_app import paths
from setup_app.static import AppType, InstallOption
from setup_app.config import Config
from setup_app.utils.setup_utils import SetupUtils
from setup_app.installers.base import BaseInstaller

class NodeInstaller(BaseInstaller, SetupUtils):

    node_base = os.path.join(Config.gluuOptFolder, 'node')
    templates_rendered = False

    def __init__(self):
        self.service_name = 'node'
        self.needdb = False # we don't need backend connection in this class
        self.install_var = 'installNode'
        self.app_type = AppType.APPLICATION
        self.install_type = InstallOption.MONDATORY
        self.register_progess()

        self.node_initd_script = os.path.join(Config.install_dir, 'static/system/initd/node')
        self.node_user_home = '/home/node'

    def install(self):

        node_archieve_list = glob.glob(os.path.join(Config.distAppFolder, 'node-*-linux-x64.tar.xz'))

        if not node_archieve_list:
            self.logIt("Can't find node archive", True, True)

        if not base.snap:
            self.createUser('node', self.node_user_home)
            self.addUserToGroup('gluu', 'node')

        nodeArchive = max(node_archieve_list)

        try:
            self.logIt("Extracting %s into /opt" % nodeArchive)
            self.run([paths.cmd_tar, '-xJf', nodeArchive, '-C', '/opt/', '--no-xattrs', '--no-same-owner', '--no-same-permissions'])
        except:
            self.logIt("Error encountered while extracting archive %s" % nodeArchive)

        nodeDestinationPath = max(glob.glob('/opt/node-*-linux-x64'))

        self.run([paths.cmd_ln, '-sf', nodeDestinationPath, Config.node_home])
        self.run([paths.cmd_chmod, '-R', "755", "%s/bin/" % nodeDestinationPath])

        self.render_templates()

        # Create temp folder
        self.run([paths.cmd_mkdir, '-p', "%s/temp" % Config.node_home])

        # Copy init.d script
        self.copyFile(self.node_initd_script, Config.gluuOptSystemFolder)
        self.run([paths.cmd_chmod, '-R', "755", "%s/node" % Config.gluuOptSystemFolder])

        self.run([paths.cmd_chown, '-R', 'node:node', nodeDestinationPath])
        self.run([paths.cmd_chown, '-h', 'node:node', Config.node_home])

        self.run([paths.cmd_mkdir, '-p', self.node_base])
        self.run([paths.cmd_chown, '-R', 'node:node', self.node_base])

    def render_templates(self):
        self.logIt("Rendering node templates")
        self.templates_rendered = True
        # make variables of this class accesible from Config
        self.update_rendering_dict()
        
        nodeTepmplatesFolder = os.path.join(Config.templateFolder, 'node')
        self.render_templates_folder(nodeTepmplatesFolder)

    def installNodeService(self, serviceName):
        self.logIt("Installing node service %s..." % serviceName)
        if not self.templates_rendered:
            self.render_templates()

        nodeServiceConfiguration = os.path.join(Config.outputFolder, 'node', serviceName)
        self.copyFile(nodeServiceConfiguration, Config.osDefault)
        self.run([paths.cmd_chown, 'root:root', os.path.join(Config.osDefault, serviceName)])

        if serviceName == 'passport':
            initscript_fn = os.path.join(Config.gluuOptSystemFolder, serviceName)
            self.fix_init_scripts(serviceName, initscript_fn)
        else:
            self.run([paths.cmd_ln, '-sf', '%s/node' % Config.gluuOptSystemFolder, '/etc/init.d/%s' % serviceName])
