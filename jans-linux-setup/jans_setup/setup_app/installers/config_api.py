import os
import time
import glob
import json
import base64
import shutil
import zipfile
from string import Template

from setup_app import paths
from setup_app.static import AppType, InstallOption
from setup_app.utils import base
from setup_app.utils.ldif_utils import myLdifParser, create_client_ldif
from setup_app.config import Config
from setup_app.installers.jetty import JettyInstaller
from setup_app.pylib.ldif4.ldif import LDIFWriter

class ConfigApiInstaller(JettyInstaller):

    source_files = [
                (os.path.join(Config.dist_jans_dir, 'jans-config-api.war'), os.path.join(base.current_app.app_info['JANS_MAVEN'], 'maven/io/jans/jans-config-api-server/{0}/jans-config-api-server-{0}.war').format(base.current_app.app_info['jans_version'])),
                (os.path.join(Config.dist_jans_dir, 'user-mgt-plugin.jar'), os.path.join(base.current_app.app_info['JANS_MAVEN'], 'maven/io/jans/jans-config-api/plugins/user-mgt-plugin/{0}/user-mgt-plugin-{0}-distribution.jar').format(base.current_app.app_info['jans_version'])),
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
        self.copy_facter_script()
        self.install_jettyService(self.jetty_app_configuration[self.service_name], True)
        self.logIt("Copying jans-config-api.war into jetty webapps folder...")
        jettyServiceWebapps = os.path.join(self.jetty_base, self.service_name, 'webapps')
        self.copyFile(self.source_files[0][0], jettyServiceWebapps)

        self.install_plugin('user-mgt')

        self.enable()

    def copy_facter_script(self):
        target_fn = '/usr/sbin/facter'
        self.copyFile(os.path.join(Config.staticFolder, 'scripts/facter'), target_fn)
        self.run([paths.cmd_chmod, '+x', target_fn])

    def install_plugin(self, plugin):
        current_plugins = self.get_plugins()

        if plugin in current_plugins:
            self.logIt(f"Jans Config Api plugin {plugin} exists, not installing")
            return

        plugin_fn = os.path.join(Config.dist_jans_dir, plugin+'-plugin.jar')

        if not os.path.exists(plugin_fn):
            self.logIt(f"Jans Config Api plugin file {plugin_fn} does not exist")
            return

        self.logIt("Installing Jans Config Api plugin {}".format(plugin))
        self.copyFile(plugin_fn, self.libDir)
        plugin_path = os.path.join(self.libDir, os.path.basename(plugin_fn))
        self.add_extra_class(plugin_path)
        self.chown(plugin_path, Config.jetty_user, Config.jetty_group)

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

        create_client = True
        if Config.installed_instance and self.dbUtils.search('ou=clients,o=jans', search_filter='(&(inum={})(objectClass=jansClnt))'.format(Config.jca_client_id)):
            create_client = False

        if create_client:
            create_client_ldif(
                ldif_fn=self.clients_ldif_fn,
                client_id=Config.jca_client_id,
                encoded_pw=Config.jca_client_encoded_pw,
                scopes=jansUmaScopes_all,
                redirect_uri=['https://{}/admin-ui'.format(Config.hostname), 'http://localhost:4100'],
                display_name="Jans Config Api Client"
                )

            self.load_ldif_files.append(self.clients_ldif_fn)


    def render_import_templates(self):

        Config.templateRenderingDict['configOauthEnabled'] = 'false' if base.argsp.disable_config_api_security else 'true'
        Config.templateRenderingDict['apiApprovedIssuer'] = base.argsp.approved_issuer or 'https://{}'.format(Config.hostname)

        _, jans_auth_config = self.dbUtils.get_jans_auth_conf_dynamic()
        for param in ('issuer', 'openIdConfigurationEndpoint', 'introspectionEndpoint', 'tokenEndpoint', 'tokenRevocationEndpoint'):
            Config.templateRenderingDict[param] = jans_auth_config[param]

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

    def update_jansservicemodule(self):
        # this function is called by jans.py: JansInstaller.post_install_tasks()
        self.logIt("Updating jansServiceModule for Config Api")

        # find configuration dn
        ldif_parser = myLdifParser(self.config_ldif_fn)
        ldif_parser.parse()
        for dn, _ in ldif_parser.entries:
            if 'ou=configuration' in dn:
                config_api_config_dn = dn
                break

        jans_service_modules = []

        for jans_service_dir in os.listdir(Config.jetty_base):
            if os.path.exists(os.path.join(Config.jetty_base, jans_service_dir, f'webapps/{jans_service_dir}.war')):
                jans_service_modules.append(jans_service_dir)

        self.logIt(f"Config Api jansConfDyn.assetMgtConfiguration.jansServiceModule list: {jans_service_modules}")

        configuration = self.dbUtils.dn_exists(config_api_config_dn)
        dynamic_configuration = json.loads(configuration['jansConfDyn'])
        dynamic_configuration['assetMgtConfiguration']['jansServiceModule'] = sorted(jans_service_modules)
        self.dbUtils.set_configuration('jansConfDyn', json.dumps(dynamic_configuration, indent=2), config_api_config_dn)


