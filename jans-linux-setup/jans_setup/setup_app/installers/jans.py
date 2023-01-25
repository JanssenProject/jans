import os
import sys
import time
import zipfile
import inspect
import base64
import shutil
import re
import requests
import zipfile
import site

from pathlib import Path

from setup_app import paths
from setup_app import static
from setup_app.utils import base
from setup_app.static import InstallTypes, AppType, InstallOption
from setup_app.config import Config
from setup_app.utils.setup_utils import SetupUtils
from setup_app.utils.ldif_utils import create_client_ldif
from setup_app.utils.progress import jansProgress
from setup_app.installers.base import BaseInstaller

class JansInstaller(BaseInstaller, SetupUtils):

    install_var = 'installJans'

    def __repr__(self):
        setattr(base.current_app, self.__class__.__name__, self)
        txt = ''
        try:
            if not Config.installed_instance:
                txt += 'hostname'.ljust(30) + Config.hostname.rjust(35) + "\n"
                txt += 'orgName'.ljust(30) + Config.orgName.rjust(35) + "\n"
                txt += 'os'.ljust(30) + Config.os_type.rjust(35) + "\n"
                txt += 'city'.ljust(30) + Config.city.rjust(35) + "\n"
                txt += 'state'.ljust(30) + Config.state.rjust(35) + "\n"
                txt += 'countryCode'.ljust(30) + Config.countryCode.rjust(35) + "\n"
                txt += 'Applications max ram (MB)'.ljust(30) + str(Config.application_max_ram).rjust(35) + "\n"

                if Config.get('opendj_install') and Config.get('opendj_max_ram'):
                    txt += 'OpenDJ max ram (MB)'.ljust(30) + str(Config.opendj_max_ram).rjust(35) + "\n"

                bc = []

                if Config.get('opendj_install'):
                    t_ = 'opendj'
                    if Config.opendj_install == InstallTypes.REMOTE:
                        t_ += '[R]'
                    bc.append(t_)

                if Config.get('cb_install'):
                    t_ = 'couchbase'
                    if Config.cb_install == InstallTypes.REMOTE:
                        t_ += '[R]'
                    bc.append(t_)

                if Config.rdbm_install:
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
            txt += 'Install Jans Config API'.ljust(30) + repr(Config.install_config_api).rjust(35) + "\n"
            if Config.profile == 'jans':
                txt += 'Install Fido2 Server'.ljust(30) + repr(Config.installFido2).rjust(35) + (' *' if 'installFido2' in Config.addPostSetupService else '') + "\n"
                txt += 'Install Scim Server'.ljust(30) + repr(Config.install_scim_server).rjust(35) + (' *' if 'install_scim_server' in Config.addPostSetupService else '') + "\n"
                #txt += 'Install Oxd '.ljust(30) + repr(Config.installOxd).rjust(35) + (' *' if 'installOxd' in Config.addPostSetupService else '') + "\n"

            if Config.profile == 'jans' and Config.installEleven:
                txt += 'Install Eleven Server'.ljust(30) + repr(Config.installEleven).rjust(35) + (' *' if 'installEleven' in Config.addPostSetupService else '') + "\n"

            if base.argsp.t:
                txt += 'Load Test Data '.ljust(30) + repr( base.argsp.t).rjust(35) + "\n"

            return txt

        except Exception as e:
            self.logIt("ERROR: " + str(e), True)
            s = ""
            for key in list(Config.__dict__):
                if not key in ('__dict__',):
                    val = getattr(Config, key)
                    if not inspect.ismethod(val):
                        s = s + "%s\n%s\n%s\n\n" % (key, "-" * len(key), val)
            return s


    def initialize(self):
        self.service_name = 'jans'
        self.app_type = AppType.APPLICATION
        self.install_type = InstallOption.MANDATORY
        jansProgress.register(self)

        Config.install_time_ldap = time.strftime('%Y%m%d%H%M%SZ', time.gmtime(time.time()))
        if not os.path.exists(Config.distFolder):
            print("Please ensure that you are running this script inside Jans container.")
            sys.exit(1)

        #Download jans-auth-client-jar-with-dependencies
        if not os.path.exists(Config.non_setup_properties['oxauth_client_jar_fn']):
            oxauth_client_jar_url = os.path.join(base.current_app.app_info['JANS_MAVEN'], 'maven/io/jans/jans-auth-client/{0}/jans-auth-client-{0}-jar-with-dependencies.jar').format(base.current_app.app_info['ox_version'])
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

        self.extract_scripts()

    def configureSystem(self):
        self.logIt("Configuring system", 'jans')
        self.customiseSystem()
        if not base.snap:
            self.createGroup('jans')
        self.makeFolders()

        if Config.persistence_type == 'hybrid':
            self.writeHybridProperties()

        # set systemd start timeout to 5 mins
        systemd_conf_fn = '/etc/systemd/system.conf'
        systemd_conf = []

        for l in open(systemd_conf_fn):
            tl = l.strip('#').strip()
            if tl.startswith('DefaultTimeoutStartSec'):
                systemd_conf.append('DefaultTimeoutStartSec=300s\n')
            else:
                systemd_conf.append(l)

        self.writeFile(systemd_conf_fn, ''.join(systemd_conf))


    def makeFolders(self):
        # Create these folder on all instances
        for folder in (Config.jansOptFolder, Config.jansOptBinFolder, Config.jansOptSystemFolder,
                        Config.jansOptPythonFolder, Config.configFolder, Config.certFolder,
                        Config.output_dir, Config.os_default, os.path.join(Config.distFolder, 'scripts')):

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

        try:
            salt_text = 'encodeSalt = {}'.format(Config.encode_salt)
            self.writeFile(Config.salt_fn, salt_text)
        except:
            self.logIt("Error writing salt", True, True)

    def render_templates(self, templates=None):
        self.logIt("Rendering templates")

        if not templates:
            templates = Config.ce_templates

        if Config.persistence_type in ('couchbase', 'sql', 'spanner') and Config.get('ox_ldap_properties'):
            Config.ce_templates[Config.ox_ldap_properties] = False

        for fullPath in templates:
            try:
                self.renderTemplate(fullPath)
            except:
                self.logIt("Error writing template %s" % fullPath, True)


    def render_configuration_template(self):
        self.logIt("Rendering configuration templates")

        try:
            self.renderTemplate(Config.ldif_configuration)
        except:
            self.logIt("Error writing template", True)


    def render_test_templates(self):
        self.logIt("Rendering test templates")

        testTepmplatesFolder = os.path.join(self.templateFolder, 'test')
        self.render_templates_folder(testTepmplatesFolder)

    def writeHybridProperties(self):

        ldap_mappings = self.getMappingType('ldap')
        couchbase_mappings = self.getMappingType('couchbase')
        
        for group in Config.mapping_locations:
            if group == 'default':
                default_mapping = Config.mapping_locations[group]
                break

        storages = set(Config.mapping_locations.values())

        jans_hybrid_roperties = [
                        'storages: {0}'.format(', '.join(storages)),
                        'storage.default: {0}'.format(default_mapping),
                        ]

        if ldap_mappings:
            jans_hybrid_roperties.append('storage.ldap.mapping: {0}'.format(', '.join(ldap_mappings)))
            ldap_map_list = []
            for m in ldap_mappings:
                if m != 'default':
                    ldap_map_list.append(Config.couchbaseBucketDict[m]['mapping'])
            jans_hybrid_roperties.append('storage.ldap.mapping: {0}'.format(', '.join(ldap_map_list)))

        if couchbase_mappings:
            cb_map_list = []
            for m in couchbase_mappings:
                if m != 'default':
                    cb_map_list.append(Config.couchbaseBucketDict[m]['mapping'])
            cb_map_str = ', '.join(cb_map_list)
            jans_hybrid_roperties.append('storage.couchbase.mapping: {0}'.format(cb_map_str))

        jans_hybrid_roperties_content = '\n'.join(jans_hybrid_roperties)

        self.writeFile(Config.jans_hybrid_roperties_fn, jans_hybrid_roperties_content)


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
        self.run(['cp', '-f', os.path.join(Config.install_dir, 'setup_app/pylib/pyDes.py'), Config.jansOptBinFolder])

        self.logIt("Error rendering encode script", True)

        super_gluu_lisence_renewer_fn = os.path.join(Config.staticFolder, 'scripts', 'super_gluu_license_renewer.py')

        if base.snap:
            target_fn = os.path.join(Config.jansOptBinFolder, 'super_gluu_lisence_renewer.py')
            self.run(['cp', '-f', super_gluu_lisence_renewer_fn, target_fn])

        else:
            target_fn = '/etc/cron.daily/super_gluu_lisence_renewer'
            self.run(['cp', '-f', super_gluu_lisence_renewer_fn, target_fn])
            self.run([paths.cmd_chown, 'root:root', target_fn])
            self.run([paths.cmd_chmod, '+x', target_fn])

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
                self.copyFile("%s/hostname" % Config.output_dir, Config.etc_hostname)
                self.run(['/bin/chmod', '-f', '644', Config.etc_hostname])

            if Config.os_type in ['centos', 'red', 'fedora']:
                self.copyFile("%s/network" % Config.output_dir, Config.network)

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

            self.appendLine("ldap       soft nofile     131072", "/etc/security/limits.conf")
            self.appendLine("ldap       hard nofile     262144", "/etc/security/limits.conf")
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
                output_fn = os.path.join(Config.output_dir, fn)
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


    def render_custom_templates(self, ldif_dir):
        output_dir_p = Path(ldif_dir + '.output')
        self.logIt("Rendering custom templates from {} to {}".format(ldif_dir, output_dir_p))

        for p in Path(ldif_dir).rglob('*'):
            if p.is_file():
                out_file_p = output_dir_p.joinpath(p.relative_to(ldif_dir))
                if not out_file_p.parent.exists():
                    out_file_p.parent.mkdir(parents=True)
                try:
                    self.renderTemplateInOut(p.as_posix(), p.parent.as_posix(), out_file_p.parent.as_posix())
                except Exception:
                    self.logIt("Error writing template {}".format(out_file_p), True)


    def import_custom_ldif_dir(self, ldif_dir):
        ldif_dir = ldif_dir.rstrip('/')
        self.logIt("Importing Custom LDIF files", pbar='post-setup')
        self.render_custom_templates(ldif_dir)

        output_dir = ldif_dir + '.output'

        for p in Path(output_dir).rglob('*.ldif'):
            ldif = p.as_posix()
            self.logIt("Importing rendered custom ldif {}".format(ldif))
            try:
                self.dbUtils.import_ldif([ldif])
            except Exception:
                self.logIt("Error importing custom ldif file {}".format(ldif), True)


    def create_test_client(self):
        ldif_fn = self.clients_ldif_fn = os.path.join(Config.output_dir, 'test-client.ldif')
        client_pw = base.argsp.test_client_secret or self.getPW()
        encoded_pw = self.obscure(client_pw)
        trusted_client = base.argsp.test_client_trusted or 'false'

        if base.argsp.test_client_redirect_uri:
            redirect_uri = base.argsp.test_client_redirect_uri.split(',')
        else:
            redirect_uri = ['https://{}/admin-ui'.format(Config.hostname), 'http://localhost:4100']

        result = self.dbUtils.search('ou=scopes,o=jans', search_filter='(objectClass=jansScope)', fetchmany=True)
        scopes = [ scope[1]['dn'] for scope in result ]

        create_client_ldif(
                ldif_fn=ldif_fn,
                client_id=base.argsp.test_client_id,
                encoded_pw=encoded_pw,
                scopes=scopes,
                redirect_uri=redirect_uri,
                trusted_client=trusted_client
                )

        self.dbUtils.import_ldif([ldif_fn])

        Config.test_client_id = base.argsp.test_client_id
        Config.test_client_pw = client_pw
        Config.test_client_pw_encoded = encoded_pw
        Config.test_client_redirect_uri = redirect_uri
        Config.test_client_trusted_client = trusted_client
        Config.test_client_scopes = ' '.join(scopes)


    def post_install_before_saving_properties(self):

        if base.argsp.test_client_id:
            self.create_test_client()


    def post_install_tasks(self):

        self.deleteLdapPw()

        self.dbUtils.bind(force=True)

        if base.argsp.import_ldif:
            self.import_custom_ldif_dir(base.argsp.import_ldif)

        if base.snap:
            #write post-install.py script
            self.logIt("Writing snap-post-setup.py", pbar='post-setup')
            post_setup_script = self.readFile(os.path.join(Config.templateFolder, 'snap-post-setup.py'))
            
            for key, val in (('{{SNAP_NAME}}', os.environ['SNAP_NAME']),
                             ('{{SNAP_PY3}}', paths.cmd_py3),
                             ('{{SNAP}}', base.snap),
                             ('{{SNAP_COMMON}}', base.snap_common)
                             ):

                post_setup_script = post_setup_script.replace(key, val)

            post_setup_script_fn = os.path.join(Config.install_dir, 'snap-post-setup.py')
            with open(post_setup_script_fn, 'w') as w:
                w.write(post_setup_script)
            self.run([paths.cmd_chmod, '+x', post_setup_script_fn])

            if not Config.installed_instance:
                Config.post_messages.insert(0, "Please execute:\nsudo " + post_setup_script_fn)

            self.logIt("Setting permissions", pbar='post-setup')

            for crt_fn in Path(os.path.join(base.snap_common, 'etc/certs')).glob('*'):
                self.run([paths.cmd_chmod, '0600', crt_fn.as_posix()])

            for spath in ('jans', 'etc/jans/conf', 'opendj/db'):
                for gpath in Path(os.path.join(base.snap_common, spath)).rglob('*'):
                    if ('node_modules' in gpath.as_posix()) or ('jans/bin' in gpath.as_posix()) or ('jetty/temp' in gpath.as_posix()):
                        continue
                    chm_mode = '0755' if os.path.isdir(gpath.as_posix()) else '0600'
                    self.run([paths.cmd_chmod, chm_mode, gpath.as_posix()])

            self.add_yacron_job(
                    command = os.path.join(Config.jansOptBinFolder, 'super_gluu_lisence_renewer.py'), 
                    schedule = '0 2 * * *', # everyday at 2 am
                    name='super-gluu-license-renewer', 
                    args={'captureStderr': True}
                    )

            self.restart('yacron')

            self.writeFile(os.path.join(base.snap_common, 'etc/hosts.jans'), Config.ip + '\t' + Config.hostname)

        else:
            self.run([paths.cmd_chown, '-R', 'jetty:root', Config.certFolder])
            self.run([paths.cmd_chmod, '-R', '660', Config.certFolder])
            self.run([paths.cmd_chmod, 'u+X', Config.certFolder])

            self.chown(Config.jansBaseFolder, user=Config.jetty_user, group=Config.jetty_group, recursive=True)
            for p in Path(Config.jansBaseFolder).rglob("*"):
                if p.is_dir():
                    self.run([paths.cmd_chmod, '750', p.as_posix()])
                elif p.is_file():
                    self.run([paths.cmd_chmod, '640', p.as_posix()])

            if not Config.installed_instance:
                cron_service = 'crond' if base.os_type in ['centos', 'red', 'fedora'] else 'cron'
                self.restart(cron_service)


            # if we are running inside shiv package, copy site pacakages to /opt/dist/jans-setup-packages and add to sys path

            gluu_site_dir = '/opt/dist/jans-setup-packages'

            for p in sys.path:
                ps = str(p)
                if '/.shiv/' in ps and ps.endswith('site-packages'):
                    if not gluu_site_dir in sys.path:
                        if not os.path.exists(site.USER_SITE):
                            os.makedirs(site.USER_SITE)
                        with open(os.path.join(site.USER_SITE, 'jans_setup_site.pth'), 'w') as site_file:
                            site_file.write(gluu_site_dir)
                        self.logIt("Copying site packages to {}".format(gluu_site_dir))
                        shutil.copytree(p, gluu_site_dir, dirs_exist_ok=True)

        #enable scripts
        self.enable_scripts(base.argsp.enable_script)

    def enable_scripts(self, inums, enable=True):
        if inums:
            for inum in inums:
                self.dbUtils.enable_script(inum, enable)

    def extract_scripts(self):
        base.extract_from_zip(base.current_app.jans_zip, 'docs/script-catalog', Config.script_catalog_dir)
