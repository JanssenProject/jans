import os
import glob
import shutil

from setup_app import paths
from setup_app.utils import base
from setup_app.static import AppType, InstallOption
from setup_app.config import Config
from setup_app.utils.setup_utils import SetupUtils
from setup_app.installers.base import BaseInstaller

class NodeInstaller(BaseInstaller, SetupUtils):

    """This installer provides node installtion for Jans server."""

    node_base = os.path.join(Config.jansOptFolder, 'node')
    templates_rendered = False

    def __init__(self):
        """Inits NodeInstaller instance."""
        self.service_name = 'node'
        self.needdb = False # we don't need backend connection in this class
        self.install_var = 'installNode'
        self.app_type = AppType.APPLICATION
        self.install_type = InstallOption.MANDATORY
        if not base.snap:
            self.register_progess()

        self.node_user_home = '/home/node'

    def install(self):

        node_archieve_list = glob.glob(os.path.join(Config.dist_app_dir, 'node-*-linux-x64.tar.xz'))

        if not node_archieve_list:
            self.logIt("Can't find node archive", True, True)

        if not base.snap:
            self.createUser('node', self.node_user_home)
            self.addUserToGroup('jans', 'node')

        node_archive = max(node_archieve_list)

        self.logIt(f"Extracting {node_archive} into {Config.opt_dir}")
        shutil.unpack_archive(node_archive, format='gztar', extract_dir=Config.opt_dir)

        nodeDestinationPath = max(glob.glob('/opt/node-*-linux-x64'))

        self.run([paths.cmd_ln, '-sf', nodeDestinationPath, Config.node_home])
        self.run([paths.cmd_chmod, '-R', "755", "%s/bin/" % nodeDestinationPath])

        # Create temp folder
        self.run([paths.cmd_mkdir, '-p', "%s/temp" % Config.node_home])

        self.run([paths.cmd_chown, '-R', 'node:node', nodeDestinationPath])
        self.run([paths.cmd_chown, '-h', 'node:node', Config.node_home])

        self.run([paths.cmd_mkdir, '-p', self.node_base])
        self.run([paths.cmd_chown, '-R', 'node:node', self.node_base])


    def installed(self):
        if os.path.islink(Config.node_home):
            node_target = os.readlink(Config.node_home)
            if not node_target.startswith('/'):
                node_target = os.path.join('/opt', node_target)
            if os.path.exists(node_target):
                return True
