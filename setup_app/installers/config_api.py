import os
import time
import glob
import json
import ruamel.yaml
import base64
import shutil

from string import Template

from setup_app import paths
from setup_app.static import AppType, InstallOption
from setup_app.utils import base
from setup_app.config import Config
from setup_app.installers.jetty import JettyInstaller
from setup_app.pylib.ldif4.ldif import LDIFWriter

class ConfigApiInstaller(JettyInstaller):

    def __init__(self):
        setattr(base.current_app, self.__class__.__name__, self)
        self.service_name = 'jans-config-api'
        self.needdb = True # we don't need backend connection in this class
        self.check_version = False #TODO: remove this when version format is changed to 1.0.0
        self.app_type = AppType.SERVICE
        self.install_type = InstallOption.OPTONAL
        self.install_var = 'installConfigApi'
        self.register_progess()


        self.templates_folder = os.path.join(Config.templateFolder, self.service_name)
        self.rs_protect_fn = os.path.join(Config.install_dir, 'setup_app/data/config-api-rs-protect.json')
        self.output_folder = os.path.join(Config.outputFolder,'jans-config-api')
        self.scope_ldif_fn = os.path.join(self.output_folder, 'scopes.ldif')
        self.clients_ldif_fn = os.path.join(self.output_folder, 'clients.ldif')
        self.dynamic_conf_json = os.path.join(self.output_folder, 'dynamic-conf.json')
        self.config_ldif_fn = os.path.join(self.output_folder, 'config.ldif')
        self.load_ldif_files = [self.config_ldif_fn, self.scope_ldif_fn]
        self.libDir = os.path.join(self.jetty_base, self.service_name, 'custom/libs/')

        self.source_files = [
                (os.path.join(Config.distJansFolder, 'jans-config-api.war'), 'https://maven.jans.io/maven/io/jans/jans-config-api-server/{0}/jans-config-api-server-{0}.war'.format(Config.oxVersion)),
                (os.path.join(Config.distJansFolder, 'scim-plugin.jar'), 'https://maven.jans.io/maven/io/jans/scim-plugin/{0}/scim-plugin-{0}-distribution.jar'.format(Config.oxVersion))
                ]

    def install(self):
        self.installJettyService(self.jetty_app_configuration[self.service_name], True)
        self.logIt("Copying fido.war into jetty webapps folder...")
        jettyServiceWebapps = os.path.join(self.jetty_base, self.service_name, 'webapps')
        self.copyFile(self.source_files[0][0], jettyServiceWebapps)
        self.war_for_jetty10(os.path.join(jettyServiceWebapps, os.path.basename(self.source_files[0][0])))
        self.copyFile(self.source_files[1][0], self.libDir)
        scim_plugin_path = os.path.join(self.libDir, os.path.basename(self.source_files[1][0]))
        self.add_extra_class(scim_plugin_path)
        self.enable()

    def installed(self):
        return os.path.exists(os.path.join(Config.jetty_base, self.service_name, 'start.ini'))


    def create_folders(self):
        for d in (self.output_folder,):
            if not os.path.exists(d):
                self.createDirs(d)

        self.run([paths.cmd_chown, '-R', 'jetty:jetty', os.path.join(Config.jetty_base, self.service_name)])


    def generate_configuration(self):

        try:
            config_api_swagger_yaml_fn = os.path.join(Config.install_dir, 'setup_app/data/jans-config-api-swagger.yaml')
            yml_str = self.readFile(config_api_swagger_yaml_fn)
            yml_str = yml_str.replace('\t', ' ')
            cfg_yml = ruamel.yaml.load(yml_str, ruamel.yaml.RoundTripLoader)
            scopes_def = cfg_yml['components']['securitySchemes']['oauth2']['flows']['clientCredentials']['scopes']
            scope_type = cfg_yml['components']['securitySchemes']['oauth2']['type']
        except:
            scopes_def = {}
            scope_type = 'oauth2'

        self.check_clients([('jca_client_id', '1800.')])

        if not Config.get('jca_client_pw'):
            Config.jca_client_pw = self.getPW()
            Config.jca_client_encoded_pw = self.obscure(Config.jca_client_pw)

        scopes = ''
        scope_ldif_fd = open(self.scope_ldif_fn, 'wb')
        ldif_scopes_writer = LDIFWriter(scope_ldif_fd, cols=1000)
        scopes = {}
        jansUmaScopes_all = [ 'inum=C4F7,ou=scopes,o=jans' ]

        if hasattr(base, 'ScimInstaller'):
            scim_scopes = base.current_app.ScimInstaller.create_user_scopes()
            jansUmaScopes_all += scim_scopes

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
                            'jansAttrs': [json.dumps({"spontaneousClientId":None, "spontaneousClientScopes":[], "showInConfigurationEndpoint": False})],
                        })

                jansUmaScopes.append(scopes[scope]['dn'])
                jansUmaScopes_all.append(scopes[scope]['dn'])

        scope_ldif_fd.close()

        createClient = True
        config_api_dn = 'inum={},ou=clients,o=jans'.format(Config.jca_client_id)
        if Config.installed_instance and self.dbUtils.search('ou=clients,o=jans', search_filter='(&(inum={})(objectClass=jansClnt))'.format(Config.jca_client_id)):
            createClient = False

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
                'jansRedirectURI': ['https://{}/admin-ui'.format(Config.hostname), 'http://localhost:4100']
                })

            clients_ldif_fd.close()
            self.load_ldif_files.append(self.clients_ldif_fn)

    def render_import_templates(self):

        Config.templateRenderingDict['configOauthEnabled'] = 'false' if base.argsp.disable_config_api_security else 'true'
        Config.templateRenderingDict['apiApprovedIssuer'] = base.argsp.approved_issuer or 'https://{}'.format(Config.hostname)

        oxauth_config_str = base64.decodestring(Config.templateRenderingDict['oxauth_config_base64'].encode())
        oxauth_config = json.loads(oxauth_config_str.decode())
        for param in ('issuer', 'openIdConfigurationEndpoint', 'introspectionEndpoint', 'tokenEndpoint', 'tokenRevocationEndpoint'):
            Config.templateRenderingDict[param] = oxauth_config[param]

        Config.templateRenderingDict['apiProtectionType'] = 'oauth2'
        Config.templateRenderingDict['endpointInjectionEnabled'] = 'false'
        Config.templateRenderingDict['httpSSSLCertificateFile'] = base.current_app.HttpdInstaller.httpdCertFn
        Config.templateRenderingDict['httpSSLCertificateKeyFile'] = base.current_app.HttpdInstaller.httpdKeyFn

        self.renderTemplateInOut(self.dynamic_conf_json, self.templates_folder, self.output_folder, pystring=True)
        Config.templateRenderingDict['config_api_dynamic_conf_base64'] = self.generate_base64_file(self.dynamic_conf_json, 1)
        self.renderTemplateInOut(self.config_ldif_fn, self.templates_folder, self.output_folder)

        self.dbUtils.import_ldif(self.load_ldif_files)


    def load_test_data(self):
        if not self.installed():
            return

        self.check_clients([('jca_test_client_id', '1802.')])

        result = self.dbUtils.search('ou=scopes,o=jans', search_filter='(&(inum=1800.*)(objectClass=jansScope))', fetchmany=True)
        scopes = []
        scopes_id_list = []

        for scope in result:
            if isinstance(scope, dict):
                if 'jansScope' in scope.get('objectClass', scope.get('objectclass',[])):
                    scopes.append('jansScope: ' + scope['dn'])
                    scopes_id_list.append(scope['jansId'])
            else:
                if 'jansScope' in scope[1]['objectClass']:
                    scopes.append('jansScope: ' +  scope[1]['dn'])
                    scopes_id_list.append(scope[1]['jansId'])

        stat_scope = 'jansScope: inum=C4F7,ou=scopes,o=jans'

        if not stat_scope in scopes:
            scopes.append(stat_scope)

        Config.templateRenderingDict['config_api_scopes'] = '\n'.join(scopes)
        Config.templateRenderingDict['config_api_scopes_list'] = ' '.join(scopes_id_list)

        if not Config.get('jca_test_client_pw'):
            Config.jca_test_client_pw = self.getPW()
            Config.jca_test_client_encoded_pw = self.obscure(Config.jca_test_client_pw)

        else:
            warning = "Test data for Config Api was allready loaded."
            self.logIt(warning)
            if Config.installed_instance:
                print(warning)
            return

        self.logIt("Loding Jans Config Api test data")

        if not base.argsp.t:
            self.render_templates_folder(os.path.join(Config.templateFolder, 'test/jans-config-api'))

        template_fn = os.path.join(Config.templateFolder, 'test/jans-config-api/data/jans-config-api.ldif')

        template_text = self.readFile(template_fn)
        rendered_text = self.fomatWithDict(template_text, self.merge_dicts(Config.__dict__, Config.templateRenderingDict))
        out_fn = os.path.join(self.output_folder, os.path.basename(template_fn))
        self.writeFile(out_fn, rendered_text)
        self.dbUtils.import_ldif([out_fn])


