import os
import time
import glob
import json
import ruamel.yaml
import base64
import shutil
import zipfile
from string import Template

from setup_app import paths
from setup_app.static import AppType, InstallOption
from setup_app.utils import base
from setup_app.config import Config
from setup_app.installers.jetty import JettyInstaller
from setup_app.pylib.ldif4.ldif import LDIFWriter

class ConfigApiInstaller(JettyInstaller):

    source_files = [
                (os.path.join(Config.dist_jans_dir, 'jans-config-api.war'), os.path.join(base.current_app.app_info['JANS_MAVEN'], 'maven/io/jans/jans-config-api-server/{0}/jans-config-api-server-{0}.war').format(base.current_app.app_info['ox_version'])),
                (os.path.join(Config.dist_jans_dir, 'facter'), 'https://raw.githubusercontent.com/GluuFederation/gluu-snap/master/facter/facter'),
                (os.path.join(Config.dist_jans_dir, 'scim-plugin.jar'), os.path.join(base.current_app.app_info['JANS_MAVEN'], 'maven/io/jans/jans-config-api/plugins/scim-plugin/{0}/scim-plugin-{0}-distribution.jar').format(base.current_app.app_info['ox_version'])),
                (os.path.join(Config.dist_jans_dir, 'user-mgt-plugin.jar'), os.path.join(base.current_app.app_info['JANS_MAVEN'], 'maven/io/jans/jans-config-api/plugins/user-mgt-plugin/{0}/user-mgt-plugin-{0}-distribution.jar').format(base.current_app.app_info['ox_version'])),
                (os.path.join(Config.dist_jans_dir, 'fido2-plugin.jar'), os.path.join(base.current_app.app_info['JANS_MAVEN'], 'maven/io/jans/jans-config-api/plugins/fido2-plugin/{0}/fido2-plugin-{0}-distribution.jar').format(base.current_app.app_info['ox_version'])),
                ]

    def __init__(self):
        setattr(base.current_app, self.__class__.__name__, self)
        self.service_name = 'jans-config-api'
        self.needdb = True # we don't need backend connection in this class
        self.app_type = AppType.SERVICE
        self.install_type = InstallOption.OPTONAL
        self.install_var = 'install_config_api'
        self.register_progess()


        self.templates_folder = os.path.join(Config.templateFolder, self.service_name)
        self.rs_protect_fn = os.path.join(Config.install_dir, 'setup_app/data/config-api-rs-protect.json')
        self.output_folder = os.path.join(Config.output_dir,'jans-config-api')
        self.scope_ldif_fn = os.path.join(self.output_folder, 'scopes.ldif')
        self.clients_ldif_fn = os.path.join(self.output_folder, 'clients.ldif')
        self.dynamic_conf_json = os.path.join(self.output_folder, 'dynamic-conf.json')
        self.config_ldif_fn = os.path.join(self.output_folder, 'config.ldif')
        self.load_ldif_files = [self.config_ldif_fn, self.scope_ldif_fn]
        self.libDir = os.path.join(self.jetty_base, self.service_name, 'custom/libs/')
        self.custom_config_dir = os.path.join(self.jetty_base, self.service_name, 'custom/config')

        if not base.argsp.shell:
            self.extract_files()

    def install(self):
        self.copyFile(self.source_files[1][0], '/usr/sbin')
        self.run([paths.cmd_chmod, '+x', '/usr/sbin/facter'])
        self.installJettyService(self.jetty_app_configuration[self.service_name], True)
        self.logIt("Copying fido.war into jetty webapps folder...")
        jettyServiceWebapps = os.path.join(self.jetty_base, self.service_name, 'webapps')
        self.copyFile(self.source_files[0][0], jettyServiceWebapps)

        self.copyFile(self.source_files[3][0], self.libDir)
        user_mgt_plugin_path = os.path.join(self.libDir, os.path.basename(self.source_files[3][0]))
        self.add_extra_class(user_mgt_plugin_path)

        if Config.install_scim_server:
            self.copyFile(self.source_files[2][0], self.libDir)
            scim_plugin_path = os.path.join(self.libDir, os.path.basename(self.source_files[2][0]))
            self.add_extra_class(scim_plugin_path)

        if Config.installFido2:
            self.copyFile(self.source_files[4][0], self.libDir)
            fido2_plugin_path = os.path.join(self.libDir, os.path.basename(self.source_files[4][0]))
            self.add_extra_class(fido2_plugin_path)

        self.enable()


    def extract_files(self):
        base.extract_file(base.current_app.jans_zip, 'jans-config-api/server/src/main/resources/log4j2.xml', self.custom_config_dir)
        base.extract_file(base.current_app.jans_zip, 'jans-config-api/docs/jans-config-api-swagger.yaml', Config.data_dir)
        base.extract_file(base.current_app.jans_zip, 'jans-config-api/server/src/main/resources/config-api-rs-protect.json', Config.data_dir)

    def create_folders(self):
        for d in (self.output_folder, self.custom_config_dir):
            if not os.path.exists(d):
                self.createDirs(d)

        self.run([paths.cmd_chown, '-R', 'jetty:jetty', os.path.join(Config.jetty_base, self.service_name)])


    def get_scope_defs(self):
        config_api_rs_protect_fn = os.path.join(Config.data_dir, 'config-api-rs-protect.json')
        scopes_def = base.readJsonFile(config_api_rs_protect_fn)
        return scopes_def

    def generate_configuration(self):

        scopes_def = self.get_scope_defs()

        scope_type = 'oauth'
        self.check_clients([('jca_client_id', '1800.')])

        if not Config.get('jca_client_pw'):
            Config.jca_client_pw = self.getPW()
            Config.jca_client_encoded_pw = self.obscure(Config.jca_client_pw)

        scope_ldif_fd = open(self.scope_ldif_fn, 'wb')
        ldif_scopes_writer = LDIFWriter(scope_ldif_fd, cols=1000)
        scopes = {}
        jansUmaScopes_all = [ 'inum=C4F7,ou=scopes,o=jans' ]

        if hasattr(base.current_app, 'ScimInstaller'):
            scim_scopes = base.current_app.ScimInstaller.create_user_scopes()
            jansUmaScopes_all += scim_scopes

        scope_levels = {'scopes':'1', 'groupScopes':'2', 'superScopes':'3'}

        for resource in scopes_def['resources']:

            for condition in resource.get('conditions', []):
                for scope_level in scope_levels:
                    for scope in (condition.get(scope_level, [])):

                        if not scope.get('inum'):
                            continue

                        if Config.installed_instance and self.dbUtils.search('ou=scopes,o=jans', search_filter='(&(jansId={})(objectClass=jansScope))'.format(scope['name'])):
                            continue

                        if not scope['name'] in scopes:
                            scope_dn = 'inum={},ou=scopes,o=jans'.format(scope['inum'])
                            scopes[scope['name']] = {'dn': scope_dn}
                            display_name = 'Config API scope {}'.format(scope['name'])
                            description = 'Config API {} scope {}'.format(scope_level, scope['name'])
                            ldif_dict = {
                                        'objectClass': ['top', 'jansScope'],
                                        'description': [description],
                                        'displayName': [display_name],
                                        'inum': [scope['inum']],
                                        'jansDefScope': ['false'],
                                        'jansId': [scope['name']],
                                        'jansScopeTyp': [scope_type],
                                        'jansAttrs': [json.dumps({"spontaneousClientId":None, "spontaneousClientScopes":[], "showInConfigurationEndpoint": False})],
                                    }
                            ldif_scopes_writer.unparse(scope_dn, ldif_dict)
                            jansUmaScopes_all.append(scope_dn)

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
                'jansAttrs': ['{"tlsClientAuthSubjectDn":"","runIntrospectionScriptBeforeJwtCreation":false,"keepClientAuthorizationAfterExpiration":false,"allowSpontaneousScopes":false,"spontaneousScopes":[],"spontaneousScopeScriptDns":[],"backchannelLogoutUri":[],"backchannelLogoutSessionRequired":false,"additionalAudience":[],"postAuthnScripts":[],"consentGatheringScripts":[],"introspectionScripts":[],"rptClaimsScripts":[]}'],
                'jansClntSecret': [Config.jca_client_encoded_pw],
                'jansDisabled': ['false'],
                'jansGrantTyp': ['authorization_code', 'refresh_token', 'client_credentials'],
                'jansIdTknSignedRespAlg': ['RS256'],
                'jansInclClaimsInIdTkn': ['false'],
                'jansLogoutSessRequired': ['false'],
                'jansPersistClntAuthzs': ['true'],
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

        oxauth_config_str = base64.decodebytes(Config.templateRenderingDict['oxauth_config_base64'].encode())
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


    def prepare_scope_list(self):

        scopes = []
        scopes_id_list = []
        result = self.dbUtils.search('ou=scopes,o=jans', search_filter='(&(inum=1800.*)(objectClass=jansScope))', fetchmany=True)

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


    def load_test_data(self):
        if not self.installed():
            return

        check_result = self.check_clients([('jca_test_client_id', '1802.')])

        self.prepare_scope_list()

        if check_result.get('1802.') == 1:
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

