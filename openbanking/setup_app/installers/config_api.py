import os
import time
import glob
import json
import ruamel.yaml
from string import Template

from setup_app import paths
from setup_app.static import AppType, InstallOption
from setup_app.utils import base
from setup_app.config import Config
from setup_app.utils.setup_utils import SetupUtils
from setup_app.installers.base import BaseInstaller

class ConfigApiInstaller(SetupUtils, BaseInstaller):

    def __init__(self):
        setattr(base.current_app, self.__class__.__name__, self)
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
        self.scope_json_fn = os.path.join(self.output_folder, 'scopes.json')
        self.clients_json_fn = os.path.join(self.output_folder, 'clients.json')
        self.load_files = [self.scope_json_fn]

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
            Config.jca_client_pw = self.getPW(32)
            Config.jca_client_encoded_pw = self.obscure(Config.jca_client_pw)

        jansUmaScopes_dn = []
        jansUmaScopes = []
        scopeIDs = []

        for scope in scopes_def:
            if Config.installed_instance and self.dbUtils.search('ou=scopes,o=jans', search_filter='(&(jansId={})(objectClass=jansScope))'.format(scope)):
                continue
            if scope in scopeIDs:
                continue
            scopeIDs.append(scope)

            inum = '1800.' + os.urandom(3).hex().upper()
            scope_dn = 'inum={},ou=scopes,o=jans'.format(inum)

            display_name = 'Config API scope {}'.format(scope)
            jansUmaScopes .append({
                        'dn': scope_dn,
                        'objectclass': 'jansScope',
                        'description': scopes_def[scope],
                        'displayName': display_name,
                        'inum': inum,
                        'jansDefScope': 0,
                        'jansId': scope,
                        'jansScopeTyp': scope_type,
                        'showInConfigurationEndpoint': 0,
                        'jansAttrs': json.dumps({"spontaneousClientId":None, "spontaneousClientScopes":None, "showInConfigurationEndpoint": False}),
                    })

            jansUmaScopes_dn.append(scope_dn)

        jansUmaScopes_json = json.dumps(jansUmaScopes, indent=2)
        self.writeFile(self.scope_json_fn, jansUmaScopes_json)

        createClient = True
        config_api_dn = 'inum={},ou=clients,o=jans'.format(Config.jca_client_id)
        if Config.installed_instance and self.dbUtils.search('ou=clients,o=jans', search_filter='(&(inum={})(objectClass=jansClnt))'.format(Config.jca_client_id)):
            createClient = False

        if createClient:
            client_json = json.dumps([{
                'dn': config_api_dn,
                'objectClass': 'jansClnt',
                'del': 0,
                'displayName': 'Jans Config Api Client',
                'inum': Config.jca_client_id,
                'jansAccessTknAsJwt': 0,
                'jansAccessTknSigAlg': 'RS256',
                'jansAppTyp': 'web',
                'jansAttrs': '{"tlsClientAuthSubjectDn":"","runIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims":false,"keepClientAuthorizationAfterExpiration":false,"allowSpontaneousScopes":false,"spontaneousScopes":[],"spontaneousScopeScriptDns":[],"backchannelLogoutUri":[],"backchannelLogoutSessionRequired":false,"additionalAudience":[],"postAuthnScripts":[],"consentGatheringScripts":[],"introspectionScripts":[],"rptClaimsScripts":[]}',
                'jansClntSecret': Config.jca_client_encoded_pw,
                'jansDisabled': 0,
                'jansGrantTyp': {'v': ['authorization_code', 'refresh_token', 'client_credentials']},
                'jansIdTknSignedRespAlg': 'RS256',
                'jansInclClaimsInIdTkn': 0,
                'jansLogoutSessRequired': 0,
                'jansPersistClntAuthzs': 1,
                'jansRequireAuthTime': 0,
                'jansRespTyp': {'v': ['code']},
                'jansRptAsJwt': 0,
                'jansScope': {'v': jansUmaScopes_dn},
                'jansSubjectTyp': 'pairwise',
                'jansTknEndpointAuthMethod': 'client_secret_basic',
                'jansTrustedClnt': 0,
                'jansRedirectURI': {'v': ['https://{}/admin-ui'.format(Config.hostname), 'http://localhost:4100']}
                }], indent=2)

            self.writeFile(self.clients_json_fn, client_json)
            self.load_files.append(self.clients_json_fn)

    def render_import_templates(self):
        Config.templateRenderingDict['apiApprovedIssuer'] = base.argsp.approved_issuer or Config.hostname
        oxauth_config = json.loads(json.loads(Config.templateRenderingDict['oxauth_config']))
        for param in ('issuer', 'openIdConfigurationEndpoint', 'introspectionEndpoint', 'tokenEndpoint', 'tokenRevocationEndpoint'):
            Config.templateRenderingDict[param] = oxauth_config[param]

        self.renderTemplateInOut(self.application_properties_tmp, self.templates_folder, self.output_folder, pystring=True)
        self.copyFile(os.path.join(self.output_folder, 'application.properties'), self.conf_dir)
        self.dbUtils.import_templates(self.load_files)


