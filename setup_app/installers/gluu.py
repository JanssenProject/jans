import os
import time
import zipfile
import inspect
import base64
import traceback
import shutil
import re

from pathlib import Path

from setup_app import paths
from setup_app import static
from setup_app.config import Config
from setup_app.utils.setup_utils import SetupUtils
from setup_app.utils import base

class GluuInstaller(SetupUtils):

    def __repr__(self):

        try:
            txt = 'hostname'.ljust(30) + Config.hostname.rjust(35) + "\n"
            txt += 'orgName'.ljust(30) + Config.orgName.rjust(35) + "\n"
            txt += 'os'.ljust(30) + Config.os_type.rjust(35) + "\n"
            txt += 'city'.ljust(30) + Config.city.rjust(35) + "\n"
            txt += 'state'.ljust(30) + Config.state.rjust(35) + "\n"
            txt += 'countryCode'.ljust(30) + Config.countryCode.rjust(35) + "\n"
            txt += 'Applications max ram'.ljust(30) + str(Config.application_max_ram).rjust(35) + "\n"
            txt += 'Install oxAuth'.ljust(30) + repr(Config.installOxAuth).rjust(35) + "\n"
            txt += 'Install oxTrust'.ljust(30) + repr(Config.installOxTrust).rjust(35) + "\n"

            bc = []
            if Config.wrends_install:
                t_ = 'wrends'
                if Config.wrends_install == static.InstallTypes.REMOTE:
                    t_ += '[R]'
                bc.append(t_)
            if Config.cb_install:
                t_ = 'couchbase'
                if Config.cb_install == static.InstallTypes.REMOTE:
                    t_ += '[R]'
                bc.append(t_)

            if bc:
                bct = ', '.join(bc)
                txt += 'Backends'.ljust(30) + bct.rjust(35) + "\n"

            txt += 'Java Type'.ljust(30) + Config.java_type.rjust(35) + "\n"
            txt += 'Install Apache 2 web server'.ljust(30) + repr(Config.installHttpd).rjust(35) + "\n"
            txt += 'Install Fido2 Server'.ljust(30) + repr(Config.installFido2).rjust(35) + "\n"
            txt += 'Install Scim Server'.ljust(30) + repr(Config.installScimServer).rjust(35) + "\n"
            txt += 'Install Shibboleth SAML IDP'.ljust(30) + repr(Config.installSaml).rjust(35) + "\n"
            txt += 'Install oxAuth RP'.ljust(30) + repr(Config.installOxAuthRP).rjust(35) + "\n"
            txt += 'Install Passport '.ljust(30) + repr(Config.installPassport).rjust(35) + "\n"
            txt += 'Install Casa '.ljust(30) + repr(Config.installCasa).rjust(35) + "\n"
            txt += 'Install Oxd '.ljust(30) + repr(Config.installOxd).rjust(35) + "\n"
            txt += 'Install Gluu Radius '.ljust(30) + repr(Config.installGluuRadius).rjust(35) + "\n"
            return txt
        except:
            s = ""
            for key in list(Config.__dict__):
                if not key in ('__dict__',):
                    val = getattr(Config, key)
                    if not inspect.ismethod(val):
                        s = s + "%s\n%s\n%s\n\n" % (key, "-" * len(key), val)
            return s


    def initialize(self):
        Config.install_time_ldap = time.strftime('%Y%m%d%H%M%SZ', time.gmtime(time.time()))
        if not os.path.exists(Config.distFolder):
            print("Please ensure that you are running this script inside Gluu container.")
            sys.exit(1)

        #TO DO: uncomment later
        #Download oxauth-client-jar-with-dependencies
        #if not os.path.exists(Config.non_setup_properties['oxauth_client_jar_fn']):
        #    oxauth_client_jar_url = 'https://ox.gluu.org/maven/org/gluu/oxauth-client/{0}/oxauth-client-{0}-jar-with-dependencies.jar'.format(Config.oxVersion)
        #    self.logIt("Downloading {}".format(os.path.basename(oxauth_client_jar_url)))
        #    self.run(['wget', '-nv', oxauth_client_jar_url, '-O', Config.non_setup_properties['oxauth_client_jar_fn']])

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
        self.logIt("Configuring system", 'gluu')
        self.customiseSystem()
        self.createGroup('gluu')
        self.makeFolders()

        if Config.persistence_type == 'hybrid':
            self.writeHybridProperties()

    def makeFolders(self):
        # Allow write to /tmp
        self.run([paths.cmd_chmod, 'ga+w', "/tmp"])

        # Create these folder on all instances
        for folder in (Config.gluuOptFolder, Config.gluuOptBinFolder, Config.gluuOptSystemFolder,
                        Config.gluuOptPythonFolder, Config.configFolder, Config.certFolder,
                        Config.outputFolder, Config.osDefault):

            if not os.path.exists(folder):
                self.run([paths.cmd_mkdir, '-p', folder])

        if Config.installOxTrust | Config.installOxAuth:
            for folder in (Config.oxPhotosFolder, Config.oxTrustRemovedFolder, Config.oxTrustCacheRefreshFolder):
                self.run([paths.cmd_mkdir, '-m', '775', '-p', folder])
                self.run([paths.cmd_chown, '-R', 'root:gluu', folder])

        self.run([paths.cmd_chown, '-R', 'root:gluu', Config.certFolder])
        self.run([paths.cmd_chmod, '551', Config.certFolder])
        

    def customiseSystem(self):
        if Config.os_initdaemon == 'init':
            system_profile_update = Config.system_profile_update_init
        else:
            system_profile_update = Config.system_profile_update_systemd

        # Render customized part
        self.renderTemplate(system_profile_update)
        renderedSystemProfile = self.readFile(system_profile_update)

        # Read source file
        currentSystemProfile = self.readFile(Config.sysemProfile)

        # Write merged file
        self.backupFile(Config.sysemProfile)
        resultSystemProfile = "\n".join((currentSystemProfile, renderedSystemProfile))
        self.writeFile(Config.sysemProfile, resultSystemProfile)

        # Fix new file permissions
        self.run([paths.cmd_chmod, '644', Config.sysemProfile])

    def make_salt(self):
        self.logIt("Making salt", pbar='gluu')
        try:
            salt_text = 'encodeSalt = {}'.format(Config.encode_salt)
            self.writeFile(os.path.join(Config.configFolder,'salt'), salt_text)
        except:
            self.logIt("Error writing salt", True)
            self.logIt(traceback.format_exc(), True)
            sys.exit()

    def render_templates(self, templates=None):
        self.logIt("Rendering templates", pbar='gluu')

        if not templates:
            templates = Config.ce_templates

        if Config.persistence_type=='couchbase':
            Config.ce_templates[Config.ox_ldap_properties] = False

        for fullPath in templates:
            try:
                self.renderTemplate(fullPath)
            except:
                self.logIt("Error writing template %s" % fullPath, True)
                self.logIt(traceback.format_exc(), True)

    def render_custom_templates(self, fullPath):
        output_dir = fullPath + '.output'

        self.logIt("Rendering custom templates")
        self.logIt("Rendering custom templates from %s to %s" % (fullPath, output_dir))

        try:
            self.run([paths.cmd_mkdir, '-p', output_dir])
        except:
            self.logIt("Error creating output directory %s" % output_dir, True)
            self.logIt(traceback.format_exc(), True)

        try:
            for filename in self.get_filepaths(fullPath):
                self.renderTemplateInOut(filename, fullPath, output_dir)
        except:
            self.logIt("Error writing template %s" % fullPath, True)
            self.logIt(traceback.format_exc(), True)

    def render_configuration_template(self):
        self.logIt("Rendering configuration templates", pbar='gluu')

        try:
            self.renderTemplate(Config.ldif_configuration)
            self.renderTemplate(Config.ldif_fido2)
        except:
            self.logIt("Error writing template", True)
            self.logIt(traceback.format_exc(), True)


    def render_test_templates(self):
        self.logIt("Rendering test templates")

        testTepmplatesFolder = os.path.join(self.templateFolder, 'test')
        self.render_templates_folder(testTepmplatesFolder)

    def writeHybridProperties(self):

        ldap_mappings = self.getMappingType('ldap')
        couchbase_mappings = self.getMappingType('couchbase')
        
        for group in self.mappingLocations:
            if group == 'default':
                default_mapping = self.mappingLocations[group]
                break

        storages = set(self.mappingLocations.values())
        
        gluu_hybrid_roperties = [
                        'storages: {0}'.format(', '.join(storages)),
                        'storage.default: {0}'.format(default_mapping),
                        ]

        if ldap_mappings:
            gluu_hybrid_roperties.append('storage.ldap.mapping: {0}'.format(', '.join(ldap_mappings)))
            ldap_map_list = []
            for m in ldap_mappings:
                if m != 'default':
                    ldap_map_list.append(self.couchbaseBucketDict[m]['mapping'])
            gluu_hybrid_roperties.append('storage.ldap.mapping: {0}'.format(', '.join(ldap_map_list)))

        if couchbase_mappings:
            cb_map_list = []
            for m in couchbase_mappings:
                if m != 'default':
                    cb_map_list.append(self.couchbaseBucketDict[m]['mapping'])
            cb_map_str = ', '.join(cb_map_list)
            gluu_hybrid_roperties.append('storage.couchbase.mapping: {0}'.format(cb_map_str))

        self.gluu_hybrid_roperties_content = '\n'.join(gluu_hybrid_roperties)

        self.writeFile(self.gluu_hybrid_roperties, self.gluu_hybrid_roperties_content)


    def setup_init_scripts(self):
        self.logIt("Setting up init scripts", pbar='gluu')
        if base.os_initdaemon == 'initd':
            for init_file in Config.init_files:
                try:
                    script_name = os.path.split(init_file)[-1]
                    self.copyFile(init_file, "/etc/init.d")
                    self.run([paths.cmd_chmod, "755", "/etc/init.d/%s" % script_name])
                except:
                    self.logIt("Error copying script file %s to /etc/init.d" % init_file)
                    self.logIt(traceback.format_exc(), True)

        if base.clone_type == 'rpm':
            for service in Config.redhat_services:
                self.run(["/sbin/chkconfig", service, "on"])
        else:
            for service in Config.debian_services:
                self.run(["/usr/sbin/update-rc.d", service, 'defaults'])
                self.run(["/usr/sbin/update-rc.d", service, 'enable'])


    def calculate_aplications_memory(self, application_max_ram, jetty_app_configuration, installedComponents):
        self.logIt("Calculating memory setting for applications")

        allowedApplicationsMemory = {}

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

                Config.templateRenderingDict["%s_max_meta_mem" % applicationName] = applicationMemory - Config.templateRenderingDict["%s_max_heap_mem" % applicationName]

    def calculate_selected_aplications_memory(self):
        Config.pbar.progress("gluu", "Calculating application memory")

        installedComponents = []

        # Jetty apps
        if Config.installOxAuth:
            installedComponents.append(Config.jetty_app_configuration['oxauth'])

        if Config.installOxTrust:
            installedComponents.append(Config.jetty_app_configuration['identity'])

        if Config.installSaml:
            installedComponents.append(Config.jetty_app_configuration['idp'])

        if Config.installOxAuthRP:
            installedComponents.append(Config.jetty_app_configuration['oxauth-rp'])

        if Config.installCasa:
            installedComponents.append(Config.jetty_app_configuration['casa'])

        if Config.installPassport:
            installedComponents.append(Config.jetty_app_configuration['passport'])
            
        self.calculate_aplications_memory(Config.application_max_ram, Config.jetty_app_configuration, installedComponents)

    def copy_scripts(self):
        self.logIt("Copying script files", pbar='gluu')

        for script in Config.gluuScriptFiles:
            self.copyFile(script, Config.gluuOptBinFolder)

        self.logIt("Rendering encode.py")
        try:
            encode_script = self.readFile(os.path.join(Config.templateFolder, 'encode.py'))            
            encode_script = encode_script % self.merge_dicts(Config.__dict__, Config.templateRenderingDict)
            self.writeFile(os.path.join(Config.gluuOptBinFolder, 'encode.py'), encode_script)
        except:
            self.logIt("Error rendering encode script")
            self.logIt(traceback.format_exc(), True)

        self.run([paths.cmd_chmod, '-R', '700', Config.gluuOptBinFolder])


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

        self.run(['/bin/chmod', '-R', '644', Config.etc_hosts])

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
            self.logIt(traceback.format_exc(), True)


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
                    self.logIt(traceback.format_exc(), True)
