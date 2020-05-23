import os
import time
import zipfile
import inspect
import base64
import traceback

from setup_app import paths
from setup_app import static
from setup_app.config import Config
from setup_app.utils.setup_utils import SetupUtils

class GluuInstaller(SetupUtils):

    def __init__(self):
        super().__init__()

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
        Config.pbar.progress("gluu", "Configuring system")
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
        Config.pbar.progress("gluu", "Making salt")
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
        self.logIt("Rendering configuration templates")

        try:
            self.renderTemplate(self.ldif_configuration)
            self.renderTemplate(self.ldif_fido2)
        except:
            self.logIt("Error writing template", True)
            self.logIt(traceback.format_exc(), True)

    def render_templates_folder(self, templatesFolder):
        self.logIt("Rendering templates folder: %s" % templatesFolder)

        coucbase_dict = self.couchbaseDict()

        for templateBase, templateDirectories, templateFiles in os.walk(templatesFolder):
            for templateFile in templateFiles:
                fullPath = '%s/%s' % (templateBase, templateFile)
                try:
                    self.logIt("Rendering test template %s" % fullPath)
                    # Remove ./template/ and everything left of it from fullPath
                    fn = re.match(r'(^.+/templates/)(.*$)', fullPath).groups()[1]
                    f = open(os.path.join(self.templateFolder, fn))
                    template_text = f.read()
                    f.close()

                    fullOutputFile = os.path.join(self.outputFolder, fn)
                    # Create full path to the output file
                    fullOutputDir = os.path.dirname(fullOutputFile)
                    if not os.path.exists(fullOutputDir):
                        os.makedirs(fullOutputDir)

                    self.backupFile(fullOutputFile)
                    newFn = open(fullOutputFile, 'w+')
                    newFn.write(template_text % self.merge_dicts(coucbase_dict, self.templateRenderingDict, self.__dict__))
                    newFn.close()
                except:
                    self.logIt("Error writing template %s" % fullPath, True)
                    self.logIt(traceback.format_exc(), True)

    def render_test_templates(self):
        self.logIt("Rendering test templates")

        testTepmplatesFolder = '%s/test/' % self.templateFolder
        self.render_templates_folder(testTepmplatesFolder)

    def render_node_templates(self):
        self.logIt("Rendering node templates")

        nodeTepmplatesFolder = '%s/node/' % self.templateFolder
        self.render_templates_folder(nodeTepmplatesFolder)

    def prepare_base64_extension_scripts(self):
        self.logIt("Preparing scripts", pbar='gluu')
        try:
            if not os.path.exists(Config.extensionFolder):
                return None

            for extensionType in os.listdir(Config.extensionFolder):
                extensionTypeFolder = os.path.join(Config.extensionFolder, extensionType)
                if not os.path.isdir(extensionTypeFolder):
                    continue

                for scriptFile in os.listdir(extensionTypeFolder):
                    scriptFilePath = os.path.join(extensionTypeFolder, scriptFile)
                    base64ScriptFile = self.generate_base64_file(scriptFilePath, 1)

                    # Prepare key for dictionary
                    extensionScriptName = '%s_%s' % (extensionType, os.path.splitext(scriptFile)[0])
                    extensionScriptName = extensionScriptName.lower()

                    Config.templateRenderingDict[extensionScriptName] = base64ScriptFile
                    self.logIt("Loaded script %s with type %s into %s" % (scriptFile, extensionType, extensionScriptName))

        except:
            self.logIt("Error loading scripts from %s" % Config.extensionFolder, True)
            self.logIt(traceback.format_exc(), True)


    def generate_base64_file(self, fn, num_spaces):
        self.logIt('Loading file %s' % fn)
        plain_file_b64encoded_text = None
        try:
            plain_file_text = self.readFile(fn, rmode='rb')
            plain_file_b64encoded_text = base64.b64encode(plain_file_text).decode('utf-8').strip()
        except:
            self.logIt("Error loading file", True)
            self.logIt(traceback.format_exc(), True)

        if num_spaces > 0:
            plain_file_b64encoded_text = self.reindent(plain_file_b64encoded_text, num_spaces)

        return plain_file_b64encoded_text

    def generate_base64_ldap_file(self, fn):
        return self.generate_base64_file(fn, 1)

    def generate_base64_configuration(self):
        self.templateRenderingDict['oxauth_config_base64'] = self.generate_base64_ldap_file(self.oxauth_config_json)
        self.templateRenderingDict['oxauth_static_conf_base64'] = self.generate_base64_ldap_file(self.oxauth_static_conf_json)
        self.templateRenderingDict['oxauth_error_base64'] = self.generate_base64_ldap_file(self.oxauth_error_json)
        self.templateRenderingDict['oxauth_openid_key_base64'] = self.generate_base64_ldap_file(self.oxauth_openid_jwks_fn)

        self.templateRenderingDict['fido2_dynamic_conf_base64'] = self.generate_base64_ldap_file(self.fido2_dynamic_conf_json)
        self.templateRenderingDict['fido2_static_conf_base64'] = self.generate_base64_ldap_file(self.fido2_static_conf_json)

        if self.installPassport:
            oxtrust_config = json.loads(self.readFile(self.oxtrust_config_json), object_pairs_hook=OrderedDict)
            passport_oxtrust_config = json.loads(self.readFile(self.passport_oxtrust_config_fn), object_pairs_hook=OrderedDict)
            oxtrust_config.update(passport_oxtrust_config)

            with open(self.oxtrust_config_json, 'w') as w:
                json.dump(oxtrust_config, w, indent=2)

        self.templateRenderingDict['oxtrust_config_base64'] = self.generate_base64_ldap_file(self.oxtrust_config_json);
        self.templateRenderingDict['oxtrust_cache_refresh_base64'] = self.generate_base64_ldap_file(self.oxtrust_cache_refresh_json)
        self.templateRenderingDict['oxtrust_import_person_base64'] = self.generate_base64_ldap_file(self.oxtrust_import_person_json)

        self.templateRenderingDict['oxidp_config_base64'] = self.generate_base64_ldap_file(self.oxidp_config_json)

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
        if self.os_initdaemon == 'initd':
            for init_file in self.init_files:
                try:
                    script_name = os.path.split(init_file)[-1]
                    self.copyFile(init_file, "/etc/init.d")
                    self.run([self.cmd_chmod, "755", "/etc/init.d/%s" % script_name])
                except:
                    self.logIt("Error copying script file %s to /etc/init.d" % init_file)
                    self.logIt(traceback.format_exc(), True)

        if self.os_type in ['centos', 'fedora']:
            for service in self.redhat_services:
                self.run(["/sbin/chkconfig", service, "on"])
        elif self.os_type in ['red']:
            for service in self.redhat_services:
                self.run(["/sbin/chkconfig", service, "on"])
        elif self.os_type in ['ubuntu', 'debian']:
            for service in self.debian_services:
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

    def encode_passwords(self):
        self.logIt("Encoding passwords", pbar='gluu')
        
        try:
            Config.encoded_oxtrust_admin_password = self.ldap_encode(Config.oxtrust_admin_password)
            Config.encoded_shib_jks_pw = self.obscure(Config.shibJksPass)
            if Config.ldapPass:
                Config.encoded_ox_ldap_pw = self.obscure(Config.ldapPass)
            if Config.cb_password:
                Config.encoded_cb_password = self.obscure(Config.cb_password)
            Config.encoded_opendj_p12_pass = self.obscure(Config.opendj_p12_pass)

            Config.oxauthClient_pw = self.getPW()
            Config.oxauthClient_encoded_pw = self.obscure(Config.oxauthClient_pw)

            Config.idpClient_pw = self.getPW()
            Config.idpClient_encoded_pw = self.obscure(Config.idpClient_pw)

            Config.encoded_couchbaseTrustStorePass = self.obscure(Config.couchbaseTrustStorePass)
        except:
            self.logIt("Error encoding passwords", True)
            self.logIt(traceback.format_exc(), True)

    def encode_test_passwords(self):
        self.logIt("Encoding test passwords", pbar='gluu')
        hostname = Config.hostname.split('.')[0]
        try:
            Config.templateRenderingDict['oxauthClient_2_pw'] = Config.templateRenderingDict['oxauthClient_2_inum'] + '-' + hostname
            Config.templateRenderingDict['oxauthClient_2_encoded_pw'] = self.obscure(Config.templateRenderingDict['oxauthClient_2_pw'])

            Config.templateRenderingDict['oxauthClient_3_pw'] =  Config.templateRenderingDict['oxauthClient_3_inum'] + '-' + hostname
            Config.templateRenderingDict['oxauthClient_3_encoded_pw'] = self.obscure(Config.templateRenderingDict['oxauthClient_3_pw'])

            Config.templateRenderingDict['oxauthClient_4_pw'] = Config.templateRenderingDict['oxauthClient_4_inum'] + '-' + hostname
            Config.templateRenderingDict['oxauthClient_4_encoded_pw'] = self.obscure(Config.templateRenderingDict['oxauthClient_4_pw'])
        except:
            self.logIt("Error encoding test passwords", True)
            self.logIt(traceback.format_exc(), True)

    def install_gluu_base(self):
        Config.ldapCertFn = Config.opendj_cert_fn
        Config.ldapTrustStoreFn = Config.opendj_p12_fn
        Config.encoded_ldapTrustStorePass = Config.encoded_opendj_p12_pass
        Config.oxTrustConfigGeneration = 'true' if Config.installSaml else 'false'
