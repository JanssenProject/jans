import os
import glob
import shutil
from pathlib import Path

from setup_app import paths
from setup_app.utils import base
from setup_app.static import AppType, InstallOption
from setup_app.config import Config
from setup_app.installers.jetty import JettyInstaller
from setup_app.utils.ldif_utils import myLdifParser, create_client_ldif

Config.jans_lock_port = '8076'
Config.lock_message_provider_type = 'DISABLED'
Config.lock_redis_host = 'localhost'
Config.lock_redis_port = '6379'


class JansLockInstaller(JettyInstaller):

    source_files = [
                (os.path.join(Config.dist_jans_dir, 'jans-lock.war'), os.path.join(base.current_app.app_info['JANS_MAVEN'], 'maven/io/jans/jans-lock-server/{0}/jans-lock-server-{0}.war').format(base.current_app.app_info['jans_version'])),
                (os.path.join(Config.dist_jans_dir, 'jans-lock-service.jar'), os.path.join(base.current_app.app_info['JANS_MAVEN'], 'maven/io/jans/jans-lock-service/{0}/jans-lock-service-{0}.jar').format(base.current_app.app_info['jans_version'])),

                (os.path.join(Config.dist_jans_dir, 'lock-plugin.jar'), os.path.join(base.current_app.app_info['JANS_MAVEN'], 'maven/io/jans/jans-config-api/plugins/lock-plugin/{0}/lock-plugin-{0}-distribution.jar').format(base.current_app.app_info['jans_version'])),
                (os.path.join(Config.dist_jans_dir, 'jans-lock-model.jar'), os.path.join(base.current_app.app_info['JANS_MAVEN'], 'maven/io/jans/jans-lock-model/{0}/jans-lock-model-{0}.jar'.format(base.current_app.app_info['jans_version']))),
                ]

    def __init__(self):
        setattr(base.current_app, self.__class__.__name__, self)
        self.service_name = 'jans-lock'
        self.needdb = True
        self.app_type = AppType.SERVICE
        self.install_type = InstallOption.OPTONAL
        self.install_var = 'install_jans_lock'
        self.register_progess()

        self.systemd_units = []
        self.set_provider_type = True
        self.output_dir = os.path.join(Config.output_dir, self.service_name)
        self.template_dir = os.path.join(Config.templateFolder, self.service_name)
        self.dynamic_conf_json = os.path.join(self.output_dir, 'dynamic-conf.json')
        self.error_json = os.path.join(self.output_dir, 'errors.json')
        self.static_conf_json = os.path.join(self.output_dir, 'static-conf.json')
        self.message_conf_json = os.path.join(self.output_dir, 'jans_message_conf.json')
        self.config_ldif = os.path.join(self.output_dir, 'config.ldif')
        self.base_endpoint = 'jans-lock' if Config.get('install_jans_lock_as_server') else 'jans-auth'
        self.clients_ldif_fn = os.path.join(self.output_dir, 'clients.ldif')

    def install(self):
        if Config.get('install_jans_lock_as_server'):
            self.install_as_server()
            self.systemd_units.append('jans-lock')
        else:
            self.install_as_service()

        self.create_client()

        if Config.persistence_type == 'sql' and Config.rdbm_type == 'pgsql':
            Config.lock_message_provider_type = 'POSTGRES'

        self.apache_lock_config()

    def create_client(self):

        _, jans_auth_config = self.dbUtils.get_jans_auth_conf_dynamic()
        Config.templateRenderingDict['jans_auth_token_endpoint'] = jans_auth_config['tokenEndpoint']

        jans_scopes = self.dbUtils.get_scopes()
        scope_openid = self.dbUtils.get_scope_by_jansid('openid')
        scopes = [ scope_openid['dn'] ]

        swagger_yaml_fn = base.extract_file(base.current_app.jans_zip, 'jans-config-api/plugins/docs/lock-plugin-swagger.yaml', Config.data_dir)
        swagger_yml = base.read_yaml_file(swagger_yaml_fn)
        config_scopes = swagger_yml['components']['securitySchemes']['oauth2']['flows']['clientCredentials']['scopes']

        for config_scope_id in config_scopes:
            config_scope = self.dbUtils.get_scope_by_jansid(config_scope_id)
            if config_scope:
                scopes.append(config_scope['dn'])

        lock_client_prefix = '2200.'
        check_result = self.check_clients([('lock_client_id', lock_client_prefix)])
        if check_result.get(lock_client_prefix) == -1:
            create_client_ldif(
                ldif_fn=self.clients_ldif_fn,
                client_id=Config.lock_client_id,
                encoded_pw=Config.lock_client_encoded_pw,
                scopes=scopes,
                redirect_uri=[f'https://{Config.hostname}/jans-lock'],
                display_name="Jans Lock Config Api Client"
                )

            self.dbUtils.import_ldif([self.clients_ldif_fn])

    def install_as_server(self):
        self.install_jettyService(self.jetty_app_configuration[self.service_name], True)
        self.logIt(f"Copying {self.source_files[0][0]} into jetty webapps folder...")
        self.copyFile(self.source_files[0][0], self.jetty_service_webapps)
        self.enable()

    def install_as_service(self):
        for plugin in (self.source_files[1][0], self.source_files[3][0]):
            plugin_name = os.path.basename(plugin)
            self.logIt(f"Adding plugin {plugin_name} to jans-auth")
            self.copyFile(plugin, base.current_app.JansAuthInstaller.custom_lib_dir)
            plugin_class_path = os.path.join(base.current_app.JansAuthInstaller.custom_lib_dir, plugin_name)
            self.chown(plugin_class_path, Config.jetty_user, Config.jetty_group)

    def render_import_templates(self):
        for tmp in (self.dynamic_conf_json, self.error_json, self.static_conf_json):
            self.renderTemplateInOut(tmp, self.template_dir, self.output_dir)

        Config.templateRenderingDict['lock_dynamic_conf_base64'] = self.generate_base64_file(self.dynamic_conf_json, 1)
        Config.templateRenderingDict['lock_error_base64'] = self.generate_base64_file(self.error_json, 1)
        Config.templateRenderingDict['lock_static_conf_base64'] = self.generate_base64_file(self.static_conf_json, 1)

        self.renderTemplateInOut(self.config_ldif, self.template_dir, self.output_dir)

        config_parser = myLdifParser(self.config_ldif)
        config_parser.parse()
        dn = config_parser.entries[0][0]

        if not self.dbUtils.dn_exists(dn):
            ldif_files = [self.config_ldif]
            self.dbUtils.import_ldif(ldif_files)

    def configure_message_conf(self):
        # this function is called in JansInstaller.post_install_tasks
        self.renderTemplateInOut(self.message_conf_json, self.template_dir, self.output_dir)
        message_conf_json = self.readFile(self.message_conf_json)
        self.dbUtils.set_configuration('jansMessageConf', message_conf_json)

    def apache_lock_config(self):
        apache_config = self.readFile(base.current_app.HttpdInstaller.https_jans_fn).splitlines()
        if Config.get('install_jans_lock_as_server'):
            proxy_context = 'jans-lock'
            proxy_port = Config.jans_lock_port
        else:
            proxy_port = Config.jans_auth_port
            proxy_context = 'jans-auth'

        jans_lock_well_known_proxy_pass = f'    ProxyPass   /.well-known/lock-server-configuration http://localhost:{proxy_port}/{proxy_context}/v1/configuration'
        jans_lock_well_known_proxy_pass += f'\n\n    <Location /jans-lock>\n     Header edit Set-Cookie ^((?!opbs|session_state).*)$ $1;HttpOnly\n     ProxyPass http://localhost:{proxy_port}/{proxy_context} retry=5 connectiontimeout=60 timeout=60\n     Order deny,allow\n     Allow from all\n    </Location>\n'


        proyx_pass_n = 0
        for i, l in enumerate(apache_config):
            if l.strip().startswith('ProxyErrorOverride') and l.strip().endswith('On'):
                proyx_pass_n = i

        apache_config.insert(proyx_pass_n-1, jans_lock_well_known_proxy_pass)
        self.writeFile(base.current_app.HttpdInstaller.https_jans_fn, '\n'.join(apache_config), backup=False)


    def installed(self):
        return os.path.exists(self.jetty_service_webapps) or os.path.exists(os.path.join(base.current_app.JansAuthInstaller.custom_lib_dir, os.path.basename(self.source_files[1][0])))


    def service_post_install_tasks(self):
        base.current_app.ConfigApiInstaller.install_plugin('lock')
