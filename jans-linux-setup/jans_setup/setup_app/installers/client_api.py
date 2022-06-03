import os
import json

from setup_app.static import AppType, InstallOption
from setup_app.utils import base
from setup_app.config import Config
from setup_app.installers.jetty import JettyInstaller


class ClientApiInstaller(JettyInstaller):
    source_files = [
                (os.path.join(Config.dist_jans_dir, 'jans-client-api.war'), os.path.join(base.current_app.app_info['JANS_MAVEN'], 'maven/io/jans/jans-client-api-server/{0}/jans-client-api-server-{0}.war').format(base.current_app.app_info['ox_version'])),
                ]

    def __init__(self):
        setattr(base.current_app, self.__class__.__name__, self)
        self.service_name = 'jans-client-api'
        self.needdb = True # we don't need backend connection in this class
        self.app_type = AppType.SERVICE
        self.install_type = InstallOption.OPTONAL
        self.install_var = 'install_client_api'
        self.register_progess()

        self.templates_folder = os.path.join(Config.templateFolder, self.service_name)
        self.output_folder = os.path.join(Config.output_dir,self.service_name)
        self.jetty_dir = os.path.join(self.jetty_base, self.service_name)
        self.dynamic_conf_json = os.path.join(self.output_folder, 'dynamic-conf.json')
        self.config_ldif_fn = os.path.join(self.output_folder, 'configuration.ldif')
        self.load_ldif_files = [self.config_ldif_fn]
        self.data_dir = os.path.join(self.jetty_dir, 'data/rp_db')

    def install(self):

        self.installJettyService(self.jetty_app_configuration[self.service_name], True)
        self.logIt("Copying fido.war into jetty webapps folder...")
        jettyServiceWebapps = os.path.join(self.jetty_dir, 'webapps')
        self.copyFile(self.source_files[0][0], jettyServiceWebapps)

        self.enable()


    def generate_configuration(self):
        suffix = 'client_api'
        Config.templateRenderingDict['client_api_keystore_fn'] = os.path.join(Config.certFolder, suffix+'.jks')
        Config.client_api_key_pass = self.getPW()
        Config.templateRenderingDict['client_api_keystore_pw'] = Config.client_api_key_pass
        key_fn, csr_fn, cert_fn = self.gen_cert(suffix, Config.client_api_key_pass, 'jetty')
        self.import_key_cert_into_keystore(suffix, Config.templateRenderingDict['client_api_keystore_fn'], Config.client_api_key_pass, key_fn, cert_fn)

    def create_folders(self):
        for d in (self.output_folder, self.data_dir):
            if not os.path.exists(d):
                self.createDirs(d)

        self.chown(self.jetty_dir, Config.jetty_user, Config.jetty_group)

    def render_import_templates(self):
        Config.templateRenderingDict['client_api_crypto_provider_fn'] = os.path.join(Config.certFolder, 'client-api-jwks.keystore')
        self.renderTemplateInOut(self.dynamic_conf_json, self.templates_folder, self.output_folder)
        dynamic_conf_json = base.readJsonFile(self.dynamic_conf_json, ordered=True)
        if Config.client_api_storage_type == 'h2':
            storage_config = {'dbFileLocation': self.data_dir}
        else:
            storage_config = {
                                'baseDn': 'o=jans',
                                'type': Config.jans_properties_fn,
                                'connection': os.path.join(Config.configFolder, 'jans-{}.properties'.format(Config.persistence_type)),
                                "salt": Config.salt_fn
                             }
        dynamic_conf_json['storageConfiguration'] = storage_config
        dynamic_conf_json_str = json.dumps(dynamic_conf_json, indent=2)
        self.writeFile(self.dynamic_conf_json, dynamic_conf_json_str, backup=False)

        Config.templateRenderingDict['client_api_dynamic_conf_base64'] = self.generate_base64_file(self.dynamic_conf_json, 1)
        self.renderTemplateInOut(self.config_ldif_fn, self.templates_folder, self.output_folder)
        self.dbUtils.import_ldif(self.load_ldif_files)


