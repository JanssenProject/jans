import os
import glob
import shutil
import ruamel.yaml

from pathlib import Path

from setup_app.utils import base
from setup_app.static import AppType, InstallOption
from setup_app.config import Config
from setup_app.installers.jetty import JettyInstaller
from setup_app.pylib.ldif4.ldif import LDIFWriter

class ScimInstaller(JettyInstaller):

#    source_files = [
#            (os.path.join(Config.dist_jans_dir, 'jans-scim.war'), os.path.join(base.current_app.app_info['JANS_MAVEN'], 'maven/io/jans/jans-scim-server/{0}/jans-scim-server-{0}.war').format(base.current_app.app_info['ox_version'])),
#            ]

    source_files = [
            (os.path.join(Config.dist_jans_dir, 'jans-scim.war'), os.path.join(base.current_app.app_info['BASE_SERVER'], '_out/jans-scim-server-1.0.6-SNAPSHOT.war')),
            ]

    source_fips_files = [
                (os.path.join(Config.dist_jans_dir, 'jans-scim-fips'), os.path.join(base.current_app.app_info['BASE_SERVER'], '_out/jans-scim-server-fips.war')),
                ]

    def __init__(self):
        setattr(base.current_app, self.__class__.__name__, self)
        self.service_name = 'jans-scim'
        self.needdb = True
        self.app_type = AppType.SERVICE
        self.install_type = InstallOption.OPTONAL
        self.install_var = 'install_scim_server'
        self.register_progess()

        self.templates_folder = os.path.join(Config.templateFolder, self.service_name)
        self.output_folder = os.path.join(Config.output_dir, self.service_name)

        self.dynamic_config_fn = os.path.join(self.output_folder, 'dynamic-conf.json')
        self.static_config_fn = os.path.join(self.output_folder, 'static-conf.json')
        self.ldif_config_fn = os.path.join(self.output_folder, 'configuration.ldif')
        self.ldif_clients_fn = os.path.join(self.output_folder, 'clients.ldif')
        self.ldif_scopes_fn = os.path.join(self.output_folder, 'scopes.ldif')
        self.jans_scim_openapi_fn = os.path.join(Config.data_dir, 'jans-scim-openapi.yaml')

        if not base.argsp.shell:
            self.extract_files()


    def extract_files(self):
        base.extract_file(base.current_app.jans_zip, 'jans-scim/server/src/main/resources/jans-scim-openapi.yaml', Config.data_dir)


    def install(self):
        self.logIt("Copying scim.war into jetty webapps folder...")
        self.installJettyService(self.jetty_app_configuration[self.service_name], True)
        jettyServiceWebapps = os.path.join(self.jetty_base, self.service_name,  'webapps')

        src_file = self.source_files[0][0] if Config.profile != SetupProfiles.DISA_STIG else self.source_fips_files[0][0]
        self.copyFile(src_file, os.path.join(jettyServiceWebapps, '%s%s' % (self.service_name, Path(self.source_files[0][0]).suffix)))            

        self.enable()


    def create_scope(self, scope, inum_base='0001.'):
        result = self.check_scope(scope['jansId'][0])
        if result:
            return result

        if not os.path.exists(self.output_folder):
            os.makedirs(self.output_folder)

        scope['inum'] = [inum_base + '.' + os.urandom(3).hex().upper()]
        ldif_scope_fn = os.path.join(self.output_folder, '{}.ldif'.format(scope['inum'][0]))
        scope_ldif_fd = open(ldif_scope_fn, 'wb')
        scope_dn = 'inum={},ou=scopes,o=jans'.format(scope['inum'][0])
        ldif_scopes_writer = LDIFWriter(scope_ldif_fd, cols=1000)
        ldif_scopes_writer.unparse(scope_dn, scope)
        scope_ldif_fd.close()
        self.dbUtils.import_ldif([ldif_scope_fn])
        return scope_dn


    def create_user_scopes(self):
        # user read
        read_dn = self.create_scope({
                'objectClass': ['top', 'jansScope'],
                'jansId': ['https://jans.io/scim/users.read'],
                'jansScopeTyp': ['oauth'],
                'jansAttrs': ['{"spontaneousClientId":null,"spontaneousClientScopes":null,"showInConfigurationEndpoint":true}'], 
                'description': ['Query user resources'], 
                    'displayName': ['Scim users.read']
                }, '1200')

        # user write
        write_dn = self.create_scope({
                'objectClass': ['top', 'jansScope'],
                'jansId': ['https://jans.io/scim/users.write'],
                'jansScopeTyp': ['oauth'],
                'jansAttrs': ['{"spontaneousClientId":null,"spontaneousClientScopes":null,"showInConfigurationEndpoint":true}'], 
                'description': ['Modify user resources'], 
                    'displayName': ['Scim users.write']
                }, '1200')

        return [read_dn, write_dn]

    def generate_configuration(self):
        self.logIt("Generating {} configuration".format(self.service_name))
        yml_str = self.readFile(self.jans_scim_openapi_fn)
        yml_str = yml_str.replace('\t', ' ')
        cfg_yml = ruamel.yaml.load(yml_str, ruamel.yaml.RoundTripLoader)
        config_scopes = cfg_yml['components']['securitySchemes']['scim_oauth']['flows']['clientCredentials']['scopes']

        scope_ldif_fd = open(self.ldif_scopes_fn, 'wb')
        ldif_scopes_writer = LDIFWriter(scope_ldif_fd, cols=1000)

        scopes_dn = self.create_user_scopes()
        for scope in config_scopes:
            if scope in ('https://jans.io/scim/users.read', 'https://jans.io/scim/users.write'):
                continue
            inum = '1200.' + os.urandom(3).hex().upper()
            scope_dn = 'inum={},ou=scopes,o=jans'.format(inum)
            scopes_dn.append(scope_dn)
            display_name = 'Scim {}'.format(os.path.basename(scope))
            ldif_scopes_writer.unparse(
                    scope_dn, {
                                'objectclass': ['top', 'jansScope'],
                                'description': [config_scopes[scope]],
                                'displayName': [display_name],
                                'inum': [inum],
                                'jansId': [scope],
                                'jansScopeTyp': ['oauth'],
                                })

        scope_ldif_fd.close()

        client_ldif_fd = open(self.ldif_clients_fn, 'wb')
        client_scopes_writer = LDIFWriter(client_ldif_fd, cols=1000)

        self.check_clients([('scim_client_id', '1201.')])

        if not Config.get('scim_client_pw'):
            Config.scim_client_pw = self.getPW()
            Config.scim_client_encoded_pw = self.obscure(Config.scim_client_pw)

        scim_client_dn = 'inum={},ou=clients,o=jans'.format(Config.scim_client_id)
        client_scopes_writer.unparse(
                scim_client_dn, {
                'objectClass': ['top', 'jansClnt'],
                'displayName': ['SCIM client'],
                'jansAccessTknSigAlg': ['RS256'],
                'jansAppTyp': ['native'],
                'jansAttrs': ['{}'],
                'jansGrantTyp':	['client_credentials'],
                'jansScope': scopes_dn,
                'jansSubjectTyp': ['pairwise'],
                'jansTknEndpointAuthMethod': ['client_secret_basic'],
                'inum': [Config.scim_client_id],
                'jansClntSecret': [Config.scim_client_encoded_pw],
                'jansRedirectURI': ['https://{}/.well-known/scim-configuration'.format(Config.hostname)]
                })

        client_ldif_fd.close()

    def create_folders(self):
        self.createDirs(self.output_folder)

    def render_import_templates(self):

        self.renderTemplateInOut(self.dynamic_config_fn, self.templates_folder, self.output_folder)
        self.renderTemplateInOut(self.static_config_fn, self.templates_folder, self.output_folder)
        Config.templateRenderingDict['scim_dynamic_conf_base64'] = self.generate_base64_ldap_file(self.dynamic_config_fn)
        Config.templateRenderingDict['scim_static_conf_base64'] = self.generate_base64_ldap_file(self.static_config_fn)

        self.renderTemplateInOut(self.ldif_config_fn, self.templates_folder, self.output_folder)

        self.dbUtils.import_ldif([self.ldif_config_fn, self.ldif_scopes_fn, self.ldif_clients_fn])

    def update_backend(self):
        self.dbUtils.enable_service('jansScimEnabled')
