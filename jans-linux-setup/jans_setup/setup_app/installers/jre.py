import os
import glob
import shutil

from pathlib import Path

from setup_app import paths
from setup_app.utils import base
from setup_app.static import AppType, InstallOption
from setup_app.config import Config
from setup_app.utils.setup_utils import SetupUtils
from setup_app.installers.base import BaseInstaller
from setup_app.pylib.jproperties import Properties

class JreInstaller(BaseInstaller, SetupUtils):

    def __init__(self):
        setattr(base.current_app, self.__class__.__name__, self)
        self.service_name = 'jre'
        self.needdb = False # we don't need backend connection in this class
        self.install_var = 'installJre'
        self.app_type = AppType.APPLICATION
        self.install_type = InstallOption.MONDATORY
        if not base.snap:
            self.register_progess()

        self.open_jdk_archive_link = 'https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/jdk-11.0.9.1%2B1/OpenJDK11U-jdk_x64_linux_hotspot_11.0.9.1_1.tar.gz'
        self.amazon_corretto_link = 'https://corretto.aws/downloads/resources/{0}/amazon-corretto-{0}-linux-x64.tar.gz'.format(base.current_app.app_info['AMAZON_CORRETTO_VERSION'])


    def install(self):

        # we need to call download_files() unless it called by base installer
        if not Config.downloadWars:
            self.download_files()

        self.logIt("Installing server JRE {} ...".format(os.path.basename(self.jreArchive)))

        try:
            self.logIt("Extracting %s into /opt/" % os.path.basename(self.jreArchive))
            self.run([paths.cmd_tar, '-xzf', self.jreArchive, '-C', '/opt/', '--no-xattrs', '--no-same-owner', '--no-same-permissions'])
        except Exception as e:
            self.logIt("Error encountered while extracting archive %s" % self.jreArchive)

        if Config.java_type == 'jdk':
            jreDestinationPath = max(glob.glob('/opt/jdk-11*'))
        else:
            jreDestinationPath = max(glob.glob('/opt/amazon-corretto-*'))

        self.run([paths.cmd_ln, '-sf', jreDestinationPath, Config.jre_home])
        self.run([paths.cmd_chmod, '-R', "755", "%s/bin/" % jreDestinationPath])
        self.run([paths.cmd_chown, '-R', 'root:root', jreDestinationPath])
        self.run([paths.cmd_chown, '-h', 'root:root', Config.jre_home])

        if not os.path.exists('/opt/jre/jre'):
            self.run([paths.cmd_mkdir, '-p', '/opt/jre/jre'])
            self.run([paths.cmd_ln, '-s', '/opt/jre/lib', '/opt/jre/jre/lib'])

        if Config.java_type == 'jre':
            for jsfn in Path('/opt/jre').rglob('java.security'):
                self.run([paths.cmd_sed, '-i', '/^#crypto.policy=unlimited/s/^#//', jsfn._str])

        self.fix_java_security()

    def download_files(self, force=False, downloads=[]):
        download_link = self.open_jdk_archive_link if Config.java_type == 'jdk' else self.amazon_corretto_link
        jre_fn = os.path.basename(download_link)
        self.jreArchive = os.path.join(Config.distAppFolder, jre_fn)
        if not os.path.exists(self.jreArchive) or force and not (downloads and src_name not in downloads):
            self.logIt("Downloading " + jre_fn, pbar=self.service_name)
            base.download(download_link, self.jreArchive)

    def fix_java_security(self):
        # https://github.com/OpenIdentityPlatform/OpenDJ/issues/78
        java_security_fn = os.path.join(Config.jre_home, 'conf/security/java.security')

        p = Properties()
        with open(java_security_fn, 'rb') as f:
            p.load(f, 'utf-8')

        if not 'TLSv1.3' in p['jdk.tls.disabledAlgorithms'].data:
            java_security = self.readFile(java_security_fn).splitlines()
            for i, l in enumerate(java_security[:]):
                if l.strip().startswith('jdk.tls.disabledAlgorithms'):
                   n = l.find('=')
                   k = l[:n].strip()
                   v = l[n+1:].strip()
                   java_security[i] = k + '=' + 'TLSv1.3, ' + v + '\n'
                   break

        self.writeFile(java_security_fn, '\n'.join(java_security))
