import os
import glob
import uuid

from setup_app import paths
from setup_app.static import AppType, InstallOption
from setup_app.config import Config
from setup_app.installers.jetty import JettyInstaller

class OxtrustInstaller(JettyInstaller):

    def __init__(self):
        self.service_name = 'identity'
        self.app_type = AppType.SERVICE
        self.install_type = InstallOption.OPTONAL
        self.install_var = 'installOxTrust'
        self.register_progess()

        self.source_files = [
                ('identity.war', 'https://ox.gluu.org/maven/org/gluu/oxtrust-server/%s/oxtrust-server-%s.war' % (Config.oxVersion, Config.oxVersion))
                ]

        self.templates_folder = os.path.join(Config.templateFolder, 'oxtrust')
        self.output_folder = os.path.join(Config.outputFolder, 'oxtrust')

        self.oxPhotosFolder = '/var/gluu/photos'
        self.oxTrustRemovedFolder = '/var/gluu/identity/removed'
        self.oxTrustCacheRefreshFolder = '/var/gluu/identity/cr-snapshots'
        self.oxtrust_config_json = os.path.join(self.output_folder, 'oxtrust-config.json')
        self.oxtrust_cache_refresh_json = os.path.join(self.output_folder, 'oxtrust-cache-refresh.json')
        self.oxtrust_import_person_json = os.path.join(self.output_folder, 'oxtrust-import-person.json')
        self.oxTrust_log_rotation_configuration = os.path.join(Config.gluuBaseFolder, 'conf/oxTrustLogRotationConfiguration.xml')
        self.ldif_config = os.path.join(self.output_folder, 'configuration.ldif')
        self.lidf_oxtrust_api = os.path.join(self.output_folder, 'oxtrust_api.ldif')
        self.ldif_oxtrust_api_clients = os.path.join(self.output_folder, 'oxtrust_api_clients.ldif')

        self.ldif_scripts = os.path.join(Config.outputFolder, 'scripts.ldif')
        self.ldif_people = os.path.join(self.output_folder, 'people.ldif')
        self.ldif_groups = os.path.join(self.output_folder, 'groups.ldif')

        # oxAuth/oxTrust Base64 configuration files
        self.pairwiseCalculationKey = None
        self.pairwiseCalculationSalt = None

        # TODO: oxtrust-api installation
        # oxTrust Api configuration
        self.api_rs_client_jks_fn = os.path.join(Config.certFolder, 'api-rs.jks')
        self.api_rp_client_jks_fn = os.path.join(Config.certFolder, 'api-rp.jks')


    def install(self):
        self.logIt("Copying identity.war into jetty webapps folder...")

        self.installJettyService(self.jetty_app_configuration[self.service_name], True)

        jettyServiceWebapps = os.path.join(self.jetty_base, self.service_name, 'webapps')
        src_war = os.path.join(Config.distGluuFolder, 'identity.war')
        self.copyFile(src_war, jettyServiceWebapps)

        self.enable()

    def generate_api_configuration(self):

        # TODO: Question: Should we do if oxtrust_api installtion?
        if not Config.get('api_rs_client_jks_pass'):
            Config.api_rs_client_jks_pass = 'secret'
            Config.api_rs_client_jks_pass_encoded = self.obscure(Config.api_rs_client_jks_pass)
        self.api_rs_client_jwks = self.gen_openid_jwks_jks_keys(self.api_rs_client_jks_fn, Config.api_rs_client_jks_pass)
        Config.templateRenderingDict['api_rs_client_base64_jwks'] = self.generate_base64_string(self.api_rs_client_jwks, 1)

        if not Config.get('api_rp_client_jks_pass'):
            Config.api_rp_client_jks_pass = 'secret'
            Config.api_rp_client_jks_pass_encoded = self.obscure(Config.api_rp_client_jks_pass)
        self.api_rp_client_jwks = self.gen_openid_jwks_jks_keys(self.api_rp_client_jks_fn, Config.api_rp_client_jks_pass)
        Config.templateRenderingDict['api_rp_client_base64_jwks'] = self.generate_base64_string(self.api_rp_client_jwks, 1)


    def generate_configuration(self):

        self.generate_api_configuration()

        client_var_id_list = (
                    ('oxtrust_resource_server_client_id', '1401.'),
                    ('oxtrust_requesting_party_client_id', '1402.'),
                    )

        self.check_clients(client_var_id_list)
        self.check_clients([('oxtrust_resource_id', '1403.')], resource=True)

        if not Config.get('admin_inum'):
            Config.admin_inum = str(uuid.uuid4())


        Config.encoded_oxtrust_admin_password = self.ldap_encode(Config.oxtrust_admin_password)

        # We need oxauth cleint id and encoded password
        if not Config.get('oxauth_client_id'):
            result = self.dbUtils.search('ou=clients,o=gluu', '(inum=1001.*)')
            if result:
                Config.oxauth_client_id = result['inum']
                self.logIt("oxauth_client_id was found in backend as {}".format(Config.oxauth_client_id))

                Config.oxauthClient_encoded_pw = result['oxAuthClientSecret']
                self.logIt("oxauthClient_encoded_pw was found in backend as {}".format(Config.oxauthClient_encoded_pw))

        if not Config.get('oxauth_client_id'):
            self.logIt("FATAL: oxauth_client_id was neither found in config nor backend. Can't continue ...", True, True)


    def render_import_templates(self):

        for tmp in (self.oxtrust_config_json, self.oxtrust_cache_refresh_json, self.oxtrust_import_person_json):
            self.renderTemplateInOut(tmp, self.templates_folder, self.output_folder)

        Config.templateRenderingDict['oxtrust_config_base64'] = self.generate_base64_ldap_file(self.oxtrust_config_json)
        Config.templateRenderingDict['oxtrust_cache_refresh_base64'] = self.generate_base64_ldap_file(self.oxtrust_cache_refresh_json)
        Config.templateRenderingDict['oxtrust_import_person_base64'] = self.generate_base64_ldap_file(self.oxtrust_import_person_json)

        # TODO: Question: Should we do import lidf_oxtrust_api and ldif_oxtrust_api_clients in oxtrust_api installtion?
        ldif_files = [
                self.ldif_config,
                self.lidf_oxtrust_api,
                self.ldif_oxtrust_api_clients,
                self.ldif_people,
                self.ldif_groups
                ]

        for tmp in ldif_files:
            self.renderTemplateInOut(tmp, self.templates_folder, self.output_folder)
        
        # TODO: Question: Should we leave general scripts to oxtrustinstaller and move others to their own installers?
        self.prepare_base64_extension_scripts()
        self.renderTemplateInOut(self.ldif_scripts, Config.templateFolder, Config.outputFolder)
        ldif_files.append(self.ldif_scripts)

        self.dbUtils.import_ldif(ldif_files)


    def create_folders(self):

        for folder in (self.oxPhotosFolder, self.oxTrustRemovedFolder, self.oxTrustCacheRefreshFolder):
            self.run([paths.cmd_mkdir, '-m', '775', '-p', folder])
            self.run([paths.cmd_chown, '-R', 'root:gluu', folder])

    def installed(self):
        return os.path.exists(os.path.join(Config.jetty_base, self.service_name, 'start.ini'))
