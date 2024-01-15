import os
import glob
import shutil
from pathlib import Path

from setup_app import paths
from setup_app.utils import base
from setup_app.static import AppType, InstallOption
from setup_app.config import Config
from setup_app.installers.jetty import JettyInstaller

Config.jans_lock_port = '8076'
Config.lock_message_provider_type = 'DISABLED'

class JansLockInstaller(JettyInstaller):

    source_files = [
                (os.path.join(Config.dist_jans_dir, 'jans-lock.war'), os.path.join(base.current_app.app_info['JANS_MAVEN'], 'maven/io/jans/jans-lock-server/{0}/jans-lock-server-{0}.war').format(base.current_app.app_info['jans_version'])),
                (os.path.join(Config.dist_jans_dir, 'jans-lock-service.jar'), os.path.join(base.current_app.app_info['JANS_MAVEN'], 'maven/io/jans/jans-lock-service/{0}/jans-lock-service-{0}.jar').format(base.current_app.app_info['jans_version'])),
                (os.path.join(Config.dist_app_dir, 'opa'), 'https://openpolicyagent.org/downloads/{}/opa_linux_amd64_static'.format(base.current_app.app_info['OPA_VERSION'])),
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

        self.output_dir = os.path.join(Config.output_dir, self.service_name)
        self.template_dir = os.path.join(Config.templateFolder, self.service_name)
        self.dynamic_conf_json = os.path.join(self.output_dir, 'dynamic-conf.json')
        self.error_json = os.path.join(self.output_dir, 'errors.json')
        self.static_conf_json = os.path.join(self.output_dir, 'static-conf.json')
        self.message_conf_json = os.path.join(self.output_dir, 'jans_message_conf.json')
        self.config_ldif = os.path.join(self.output_dir, 'config.ldif')
        self.opa_addr = 'localhost:8181'
        self.opa_dir = os.path.join(Config.opt_dir, 'opa')
        self.opa_bin_dir = os.path.join(self.opa_dir, 'bin')

    def install(self):

        if Config.get('install_jans_lock_as_server'):
            self.install_as_server()
            self.systemd_units.append('jans-lock')
        else:
            self.install_as_service()

        if Config.get('install_opa'):
            self.install_opa()

        if Config.rdbm_type == 'pgsql':
            self.dbUtils.set_oxAuthConfDynamic({'lockMessageConfig': {'enableIdTokenMessages': True, 'idTokenMessagesChannel': 'id_token'}})
            Config.lock_message_provider_type = 'POSTGRES'

    def install_as_server(self):
        self.installJettyService(self.jetty_app_configuration[self.service_name], True)
        self.logIt(f"Copying {self.source_files[0][0]} into jetty webapps folder...")
        self.copyFile(self.source_files[0][0], self.jetty_service_webapps)
        self.enable()

    def install_as_service(self):
        plugin_name = os.path.basename(self.source_files[1][0])
        self.logIt(f"Adding plugin {plugin_name} to jans-auth")
        self.copyFile(self.source_files[1][0], base.current_app.JansAuthInstaller.custom_lib_dir)
        plugin_class_path = os.path.join(base.current_app.JansAuthInstaller.custom_lib_dir, plugin_name)
        base.current_app.JansAuthInstaller.add_extra_class(plugin_class_path)
        self.chown(plugin_class_path, Config.jetty_user, Config.jetty_group)


    def render_import_templates(self):

        for tmp in (self.dynamic_conf_json, self.error_json, self.static_conf_json, self.message_conf_json):
            self.renderTemplateInOut(tmp, self.template_dir, self.output_dir)

        Config.templateRenderingDict['lock_dynamic_conf_base64'] = self.generate_base64_file(self.dynamic_conf_json, 1)
        Config.templateRenderingDict['lock_error_base64'] = self.generate_base64_file(self.error_json, 1)
        Config.templateRenderingDict['lock_static_conf_base64'] = self.generate_base64_file(self.static_conf_json, 1)

        self.renderTemplateInOut(self.config_ldif, self.template_dir, self.output_dir)

        ldif_files = [self.config_ldif]
        self.dbUtils.import_ldif(ldif_files)
        message_conf_json = self.readFile(self.message_conf_json)
        self.dbUtils.set_configuration('jansMessageConf', message_conf_json)


    def install_opa(self):
        opa_fn = 'opa'
        self.systemd_units.append(opa_fn)
        self.createDirs(self.opa_bin_dir)
        self.copyFile(self.source_files[2][0], self.opa_bin_dir)
        self.run([paths.cmd_chmod, '755', os.path.join(self.opa_bin_dir, opa_fn)])
        self.chown(self.opa_dir, Config.jetty_user, Config.jetty_group, recursive=True)
        self.enable(opa_fn)

    def installed(self):
        return os.path.exists(self.jetty_service_webapps) or os.path.exists(os.path.join(base.current_app.JansAuthInstaller.custom_lib_dir, os.path.basename(self.source_files[1][0])))
