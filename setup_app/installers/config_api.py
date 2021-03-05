import os
import time
import glob
import json
import uuid
import ruamel.yaml
import base64
from string import Template

from setup_app import paths
from setup_app.static import AppType, InstallOption
from setup_app.utils import base
from setup_app.config import Config
from setup_app.utils.setup_utils import SetupUtils
from setup_app.installers.base import BaseInstaller
from setup_app.pylib.ldif4.ldif import LDIFWriter

class ConfigApiInstaller(SetupUtils, BaseInstaller):

    def __init__(self):
        self.service_name = 'jans-config-api'
        self.needdb = True # we don't need backend connection in this class
        self.app_type = AppType.SERVICE
        self.install_type = InstallOption.OPTONAL
        self.install_var = 'installConfigApi'
        self.register_progess()

        self.root_dir = os.path.join(Config.jansOptFolder, 'jans-config-api')
        self.conf_dir = os.path.join(self.root_dir, 'config')
        self.log_dir = os.path.join(self.root_dir, 'logs')
        self.templates_folder = os.path.join(Config.templateFolder, self.service_name)
        self.application_properties_tmp = os.path.join(self.templates_folder, 'application.properties')
        self.rs_protect_fn = os.path.join(Config.install_dir, 'setup_app/data/config-api-rs-protect.json')
        self.output_folder = os.path.join(Config.outputFolder,'jans-config-api')
        self.scope_ldif_fn = os.path.join(self.output_folder, 'scopes.ldif')
        self.clients_ldif_fn = os.path.join(self.output_folder, 'clients.ldif')
        self.load_ldif_files = [self.scope_ldif_fn]

        self.source_files = [
                (os.path.join(Config.distJansFolder, 'jans-config-api-runner.jar'), 'https://maven.jans.io/maven/io/jans/jans-config-api/{0}/jans-config-api-{0}-runner.jar'.format(Config.oxVersion))
                ]

    def install(self):
        self.copyFile(self.source_files[0][0], self.root_dir)

        self.copyFile(
                os.path.join(Config.staticFolder, 'system/initd', self.service_name),
                os.path.join(Config.distFolder, 'scripts')
                )

        self.run([paths.cmd_chmod, '+x', os.path.join(Config.distFolder, 'scripts', self.service_name)])
        self.enable()

    def installed(self):
        return os.path.exists(self.root_dir)


    def create_folders(self):
        for d in (self.root_dir, self.conf_dir, self.log_dir, self.output_folder):
            if not os.path.exists(d):
                self.createDirs(d)

        self.run([paths.cmd_chown, '-R', 'jetty:jetty', self.root_dir])


    def generate_configuration(self):

        try:
            config_api_swagger_yaml_fn = os.path.join(Config.install_dir, 'setup_app/data/jans-config-api-swagger.yaml')
            yml_str = self.readFile(config_api_swagger_yaml_fn)
            yml_str = yml_str.replace('\t', ' ')
            cfg_yml = ruamel.yaml.load(yml_str, ruamel.yaml.RoundTripLoader)
            scopes_def = cfg_yml['components']['securitySchemes']['jans-auth']['flows']['clientCredentials']['scopes']
            scope_type = cfg_yml['components']['securitySchemes']['jans-auth']['type']
        except:
            scopes_def = {}
            scope_type = 'oauth2'

        self.check_clients([('jca_client_id', '1801.')])

        if not Config.get('jca_client_pw'):
            Config.jca_client_pw = self.getPW()
            Config.jca_client_encoded_pw = self.obscure(Config.jca_client_pw)

        scopes = ''
        scope_ldif_fd = open(self.scope_ldif_fn, 'wb')
        ldif_scopes_writer = LDIFWriter(scope_ldif_fd, cols=1000)
        scopes = {}
        jansUmaScopes_all = []


        for scope in scopes_def:

            jansUmaScopes = []

            if Config.installed_instance and self.dbUtils.search('ou=scopes,o=jans', search_filter='(&(jansId={})(objectClass=jansScope))'.format(scope)):
                continue

            if not scope in scopes:
                inum = '1800.' + os.urandom(3).hex().upper()
                scope_dn = 'inum={},ou=scopes,o=jans'.format(inum)
                scopes[scope] = {'dn': scope_dn}
                display_name = 'Config API scope {}'.format(scope)
                ldif_scopes_writer.unparse(
                        scope_dn, {
                            'objectclass': ['top', 'jansScope'],
                            'description': [scopes_def[scope]],
                            'displayName': [display_name],
                            'inum': [inum],
                            'jansDefScope': ['false'],
                            'jansId': [scope],
                            'jansScopeTyp': [scope_type],
                        })

                jansUmaScopes.append(scopes[scope]['dn'])
                jansUmaScopes_all.append(scopes[scope]['dn'])

        scope_ldif_fd.close()

        createClient = True
        config_api_dn = 'inum={},ou=clients,o=jans'.format(Config.jca_client_id)
        if Config.installed_instance and self.dbUtils.search('ou=clients,o=jans', search_filter='(&(inum={})(objectClass=jansClnt))'.format(Config.jca_client_id)):
            createClient = False

        jansUmaScopes_all += [
                        'inum=F0C4,ou=scopes,o=jans',
                        'inum=764C,ou=scopes,o=jans',
                        'inum=10B2,ou=scopes,o=jans'
                        ]

        if createClient:
            clients_ldif_fd = open(self.clients_ldif_fn, 'wb')
            ldif_clients_writer = LDIFWriter(clients_ldif_fd, cols=1000)
            ldif_clients_writer.unparse(
                config_api_dn, {
                'objectClass': ['top', 'jansClnt'],
                'del': ['false'],
                'displayName': ['Jans Config Api Client'],
                'inum': [Config.jca_client_id],
                'jansAccessTknAsJwt': ['false'],
                'jansAccessTknSigAlg': ['RS256'],
                'jansAppTyp': ['web'],
                'jansAttrs': ['{"tlsClientAuthSubjectDn":"","runIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims":false,"keepClientAuthorizationAfterExpiration":false,"allowSpontaneousScopes":false,"spontaneousScopes":[],"spontaneousScopeScriptDns":[],"backchannelLogoutUri":[],"backchannelLogoutSessionRequired":false,"additionalAudience":[],"postAuthnScripts":[],"consentGatheringScripts":[],"introspectionScripts":[],"rptClaimsScripts":[]}'],
                'jansClntSecret': [Config.jca_client_encoded_pw],
                'jansDisabled': ['false'],
                'jansGrantTyp': ['authorization_code', 'refresh_token', 'client_credentials'],
                'jansIdTknSignedRespAlg': ['RS256'],
                'jansInclClaimsInIdTkn': ['false'],
                'jansLogoutSessRequired': ['false'],
                'jansPersistClntAuthzs': ['true'],
                'jansRequireAuthTime': ['false'],
                'jansRespTyp': ['code'],
                'jansRptAsJwt': ['false'],
                'jansScope': jansUmaScopes_all,
                'jansSubjectTyp': ['pairwise'],
                'jansTknEndpointAuthMethod': ['client_secret_basic'],
                'jansTrustedClnt': ['false'],
                'jansRedirectURI': ['https://{}/admin-ui'.format(Config.hostname), 'http//:localhost:4100']
                })

            clients_ldif_fd.close()
            self.load_ldif_files.append(self.clients_ldif_fn)

    def render_import_templates(self):
        oxauth_config_str = base64.decodestring(Config.templateRenderingDict['oxauth_config_base64'].encode())
        oxauth_config = json.loads(oxauth_config_str.decode())
        for param in ('issuer', 'openIdConfigurationEndpoint', 'introspectionEndpoint', 'tokenEndpoint', 'tokenRevocationEndpoint'):
            Config.templateRenderingDict[param] = oxauth_config[param]

        self.renderTemplateInOut(self.application_properties_tmp, self.templates_folder, self.output_folder, pystring=True)
        self.copyFile(os.path.join(self.output_folder, 'application.properties'), self.conf_dir)
        self.dbUtils.import_ldif(self.load_ldif_files)


    def load_test_data(self):
        if self.dbUtils.dn_exists('inum=1801.test-client,ou=clients,o=jans'):
            warning = "Test data for Config Api was allready loaded."
            self.logIt(warning)
            if Config.installed_instance:
                print(warning)
        
        result = self.dbUtils.search('ou=scopes,o=jans', '(inum=1800.*)', fetchmany=True)
        scopes = []
        for scope in result:
            scopes.append('jansScope: ' + scope[1]['dn'])
        
        Config.templateRenderingDict['config_api_scopes'] = '\n'.join(scopes)
        template_fn = os.path.join(Config.templateFolder, 'test/jans-config-api/jans-config-api.ldif')
        template_text = self.readFile(template_fn)
        template_str = Template(template_text)
        rendered_text = template_str.substitute(**self.merge_dicts(Config.__dict__, Config.templateRenderingDict))
        out_fn = os.path.join(self.output_folder, 'test_jans-config-api.ldif')
        self.writeFile(out_fn, rendered_text)
        self.dbUtils.import_ldif([out_fn])
