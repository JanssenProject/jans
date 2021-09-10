import os
import time
import zipfile
import inspect
import base64
import shutil
import re

from pathlib import Path

from setup_app import paths
from setup_app import static
from setup_app.utils import base
from setup_app.static import InstallTypes, AppType, InstallOption
from setup_app.config import Config
from setup_app.utils.setup_utils import SetupUtils
from setup_app.utils.progress import jansProgress
from setup_app.installers.base import BaseInstaller

class JansInstaller(BaseInstaller, SetupUtils):

    install_var = 'installJans'

    def __repr__(self):
        setattr(base.current_app, self.__class__.__name__, self)
        txt = ''
        #try:
        if 1:
            if not Config.installed_instance:
                txt += 'hostname'.ljust(30) + Config.hostname.rjust(35) + "\n"
                txt += 'orgName'.ljust(30) + Config.orgName.rjust(35) + "\n"
                txt += 'os'.ljust(30) + Config.os_type.rjust(35) + "\n"
                txt += 'city'.ljust(30) + Config.city.rjust(35) + "\n"
                txt += 'state'.ljust(30) + Config.state.rjust(35) + "\n"
                txt += 'countryCode'.ljust(30) + Config.countryCode.rjust(35) + "\n"
                txt += 'Applications max ram (MB)'.ljust(30) + str(Config.application_max_ram).rjust(35) + "\n"

                bc = []

                t_ = Config.rdbm_type
                if Config.rdbm_install_type == InstallTypes.REMOTE:
                    t_ += '[R]'
                bc.append(t_)


                if bc:
                    bct = ', '.join(bc)
                    txt += 'Backends'.ljust(30) + bct.rjust(35) + "\n"

                txt += 'Java Type'.ljust(30) + Config.java_type.rjust(35) + "\n"

            txt += 'Install Apache 2 web server'.ljust(30) + repr(Config.installHttpd).rjust(35) + (' *' if 'installHttpd' in Config.addPostSetupService else '') + "\n"
            txt += 'Install Auth Server'.ljust(30) + repr(Config.installOxAuth).rjust(35) + "\n"
            txt += 'Install Jans Auth Config Api'.ljust(30) + repr(Config.installConfigApi).rjust(35) + "\n"
            return txt

        """
        except:
            s = ""
            for key in list(Config.__dict__):
                if not key in ('__dict__',):
                    val = getattr(Config, key)
                    if not inspect.ismethod(val):
                        s = s + "%s\n%s\n%s\n\n" % (key, "-" * len(key), val)
            return s
        """

    def initialize(self):
        self.service_name = 'jans'
        self.app_type = AppType.APPLICATION
        self.install_type = InstallOption.MONDATORY
        jansProgress.register(self)

        #Download jans-auth-client-jar-with-dependencies
        if not os.path.exists(Config.non_setup_properties['oxauth_client_jar_fn']):
            oxauth_client_jar_url = 'https://ox.gluu.org/maven/org/gluu/jans-auth-client/{0}/jans-auth-client-{0}-jar-with-dependencies.jar'.format(Config.oxVersion)
            self.logIt("Downloading {}".format(os.path.basename(oxauth_client_jar_url)))
            base.download(oxauth_client_jar_url, Config.non_setup_properties['oxauth_client_jar_fn'])

        self.logIt("Determining key generator path")
        oxauth_client_jar_zf = zipfile.ZipFile(Config.non_setup_properties['oxauth_client_jar_fn'])

        for f in oxauth_client_jar_zf.namelist():
            if os.path.basename(f) == 'KeyGenerator.class':
                p, e = os.path.splitext(f)
                Config.non_setup_properties['key_gen_path'] = p.replace(os.path.sep, '.')
            elif os.path.basename(f) == 'KeyExporter.class':
                p, e = os.path.splitext(f)
                Config.non_setup_properties['key_export_path'] = p.replace(os.path.sep, '.')

        if (not 'key_gen_path' in Config.non_setup_properties) or (not 'key_export_path' in Config.non_setup_properties):
            self.logIt("Can't determine key generator and/or key exporter path form {}".format(Config.non_setup_properties['oxauth_client_jar_fn']), True, True)
        else:
            self.logIt("Key generator path was determined as {}".format(Config.non_setup_properties['key_export_path']))

    def configureSystem(self):
        self.logIt("Configuring system", 'jans')
        self.customiseSystem()
        if not base.snap:
            self.createGroup('jans')
        self.makeFolders()

        if Config.persistence_type == 'hybrid':
            self.writeHybridProperties()

    def makeFolders(self):
        # Create these folder on all instances
        for folder in (Config.jansOptFolder, Config.jansOptBinFolder, Config.jansOptSystemFolder,
                        Config.jansOptPythonFolder, Config.configFolder, Config.certFolder,
                        Config.outputFolder, Config.osDefault):

            if not os.path.exists(folder):
                self.run([paths.cmd_mkdir, '-p', folder])

        if not base.snap:
            self.run([paths.cmd_chown, '-R', 'root:jans', Config.certFolder])
            self.run([paths.cmd_chmod, '551', Config.certFolder])
            self.run([paths.cmd_chmod, 'ga+w', "/tmp"]) # Allow write to /tmp

    def customiseSystem(self):
        if not base.snap:
            if Config.os_initdaemon == 'init':
                system_profile_update = Config.system_profile_update_init
            else:
                system_profile_update = Config.system_profile_update_systemd

            # Render customized part
            self.renderTemplate(system_profile_update)
            renderedSystemProfile = self.readFile(system_profile_update)

            # Read source file
            currentSystemProfile = self.readFile(Config.sysemProfile)

            if not 'Added by Jans' in currentSystemProfile:
                # Write merged file
                self.backupFile(Config.sysemProfile)
                resultSystemProfile = "\n".join((currentSystemProfile, renderedSystemProfile))
                self.writeFile(Config.sysemProfile, resultSystemProfile)

                # Fix new file permissions
                self.run([paths.cmd_chmod, '644', Config.sysemProfile])

    def make_salt(self):
        if not Config.encode_salt:
            Config.encode_salt= self.getPW() + self.getPW()

        self.logIt("Making salt")
        salt_fn = os.path.join(Config.configFolder,'salt')

        try:
            salt_text = 'encodeSalt = {}'.format(Config.encode_salt)
            self.writeFile(salt_fn, salt_text)
        except:
            self.logIt("Error writing salt", True, True)

    def render_templates(self, templates=None):
        self.logIt("Rendering templates")

        if not templates:
            templates = Config.ce_templates

        for fullPath in templates:
            try:
                self.renderTemplate(fullPath)
            except:
                self.logIt("Error writing template %s" % fullPath, True)


    def setup_init_scripts(self):
        self.logIt("Setting up init scripts")
        if base.os_initdaemon == 'initd':
            for init_file in Config.init_files:
                try:
                    script_name = os.path.split(init_file)[-1]
                    self.copyFile(init_file, "/etc/init.d")
                    self.run([paths.cmd_chmod, "755", "/etc/init.d/%s" % script_name])
                except:
                    self.logIt("Error copying script file %s to /etc/init.d" % init_file)

        if base.clone_type == 'rpm':
            for service in Config.redhat_services:
                self.run(["/sbin/chkconfig", service, "on"])
        elif not base.snap:
            for service in Config.debian_services:
                self.run([paths.cmd_update_rc , service, 'defaults'])
                self.run([paths.cmd_update_rc, service, 'enable'])


    def copy_scripts(self):
        self.logIt("Copying script files")

        for script in Config.jansScriptFiles:
            self.copyFile(script, Config.jansOptBinFolder)

        self.logIt("Rendering encode.py")
        encode_script = self.readFile(os.path.join(Config.templateFolder, 'encode.py'))
        encode_script = encode_script % self.merge_dicts(Config.__dict__, Config.templateRenderingDict)
        self.writeFile(os.path.join(Config.jansOptBinFolder, 'encode.py'), encode_script)
        self.logIt("Error rendering encode script", True)

        if base.snap:
            pass
        else:
            print_version_scr_fn = os.path.join(Config.install_dir, 'setup_app/utils/printVersion.py')
            self.run(['cp', '-f', print_version_scr_fn , Config.jansOptBinFolder])
            self.run([paths.cmd_ln, '-s', 'printVersion.py' , 'show_version.py'], cwd=Config.jansOptBinFolder)

        for scr in Path(Config.jansOptBinFolder).glob('*'):
            scr_path = scr.as_posix()
            if base.snap and scr_path.endswith('.py'):
                scr_content = self.readFile(scr_path).splitlines()
                first_line = '#!' + paths.cmd_py3
                if scr_content[0].startswith('#!'):
                    scr_content[0] = first_line
                else:
                    scr_content.insert(0, first_line)
                self.writeFile(scr_path, '\n'.join(scr_content), backup=False)

            self.run([paths.cmd_chmod, '700', scr_path])

    def update_hostname(self):
        self.logIt("Copying hosts and hostname to final destination")

        if base.os_initdaemon == 'systemd' and base.clone_type == 'rpm':
            self.run(['/usr/bin/hostnamectl', 'set-hostname', Config.hostname])
        else:
            if Config.os_type in ['debian', 'ubuntu']:
                self.copyFile("%s/hostname" % Config.outputFolder, Config.etc_hostname)
                self.run(['/bin/chmod', '-f', '644', Config.etc_hostname])

            if Config.os_type in ['centos', 'red', 'fedora']:
                self.copyFile("%s/network" % Config.outputFolder, Config.network)

            self.run(['/bin/hostname', Config.hostname])

        if not os.path.exists(Config.etc_hosts):
            self.writeFile(Config.etc_hosts, '{}\t{}\n'.format(Config.ip, Config.hostname))
        else:
            hostname_file_content = self.readFile(Config.etc_hosts)
            with open(Config.etc_hosts,'w') as w:
                for l in hostname_file_content.splitlines():
                    if not Config.hostname in l.split():
                        w.write(l+'\n')

                w.write('{}\t{}\n'.format(Config.ip, Config.hostname))

        self.run([paths.cmd_chmod, '-R', '644', Config.etc_hosts])

    def set_ulimits(self):
        self.logIt("Setting ulimist")
        try:
            apache_user = 'apache' if base.clone_type == 'rpm' else 'www-data'

            #self.appendLine("ldap       soft nofile     131072", "/etc/security/limits.conf")
            #self.appendLine("ldap       hard nofile     262144", "/etc/security/limits.conf")
            self.appendLine("%s     soft nofile     131072" % apache_user, "/etc/security/limits.conf")
            self.appendLine("%s     hard nofile     262144" % apache_user, "/etc/security/limits.conf")
            self.appendLine("jetty      soft nofile     131072", "/etc/security/limits.conf")
            self.appendLine("jetty      hard nofile     262144", "/etc/security/limits.conf")
        except:
            self.logIt("Could not set limits.")


    def copy_output(self):
        self.logIt("Copying rendered templates to final destination")

        for dest_fn in list(Config.ce_templates.keys()):
            if Config.ce_templates[dest_fn]:
                fn = os.path.split(dest_fn)[-1]
                output_fn = os.path.join(Config.outputFolder, fn)
                try:
                    self.logIt("Copying %s to %s" % (output_fn, dest_fn))
                    dest_dir = os.path.dirname(dest_fn)
                    if not os.path.exists(dest_dir):
                        self.logIt("Created destination folder %s" % dest_dir)
                        os.makedirs(dest_dir)
                    self.backupFile(output_fn, dest_fn)
                    shutil.copyfile(output_fn, dest_fn)
                except:
                    self.logIt("Error writing %s to %s" % (output_fn, dest_fn), True)

    def post_install_tasks(self):

        self.run([paths.cmd_chown, '-R', 'jetty:root', Config.certFolder])
        self.run([paths.cmd_chmod, '-R', '660', Config.certFolder])
        self.run([paths.cmd_chmod, 'u+X', Config.certFolder])

        if not Config.installed_instance:
            cron_service = 'crond' if base.os_type in ['centos', 'red', 'fedora'] else 'cron'
            self.restart(cron_service)
