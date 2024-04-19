import os
import re
import glob
import ssl
import time
import json

from xml.etree import ElementTree

from setup_app import paths
from setup_app.config import Config
from setup_app.pylib.ldif4.ldif import LDIFWriter
from setup_app.static import AppType, InstallOption, SetupProfiles
from setup_app.utils import base
from setup_app.utils.properties_utils import propertiesUtils
from setup_app.utils.ldif_utils import myLdifParser
from setup_app.installers.jetty import JettyInstaller

Config.casa_web_port = '8080'
CASA_GIT = 'https://raw.githubusercontent.com/JanssenProject/jans/main/jans-casa'

class CasaInstaller(JettyInstaller):

    client_id_prefix = '3000.'
    casa_dist_dir = os.path.join(Config.dist_jans_dir, 'jans_casa')
    source_files = [
            (os.path.join(casa_dist_dir, 'jans-casa.war'), os.path.join(base.current_app.app_info['JANS_MAVEN'], 'maven/io/jans/casa/{0}/casa-{0}.war').format(base.current_app.app_info['jans_version'])),
            (os.path.join(casa_dist_dir, 'jans-casa-config.jar'), os.path.join(base.current_app.app_info['JANS_MAVEN'], 'maven/io/jans/casa-config/{0}/casa-config-{0}.jar').format(base.current_app.app_info['jans_version'])),
            (os.path.join(casa_dist_dir, 'twilio.jar'), os.path.join(base.current_app.app_info['TWILIO_MAVEN'], '{0}/twilio-{0}.jar'.format(base.current_app.app_info['TWILIO_VERSION']))),
            (os.path.join(casa_dist_dir, 'jans-fido2-client.jar'), (os.path.join(base.current_app.app_info['JANS_MAVEN'], 'maven/io/jans/jans-fido2-client/{0}/jans-fido2-client-{0}.jar'.format(base.current_app.app_info['jans_version'])))),
            ]

    def __init__(self):
        setattr(base.current_app, self.__class__.__name__, self)
        self.service_name = 'jans-casa'
        self.app_type = AppType.SERVICE
        self.install_type = InstallOption.OPTONAL
        self.install_var = 'install_casa'
        self.register_progess()

        self.output_folder = os.path.join(Config.output_dir, self.service_name)
        self.templates_dir = os.path.join(Config.templateFolder, self.service_name)
        self.ldif_config_fn = os.path.join(self.output_folder, 'configuration.ldif')
        self.ldif_client_fn = os.path.join(self.output_folder, 'client.ldif')
        self.ldif_auth_script_fn = os.path.join(self.output_folder, 'person_authentication_script.ldif')
        self.config_json_fn = os.path.join(self.output_folder, 'casa-config.json')
        self.ldif_person_authentication_script_fn = os.path.join(self.output_folder, 'person_authentication_script.ldif')
        self.scopes_fn = os.path.join(self.templates_dir, 'scopes.json')
        self.pylib_dir = os.path.join(Config.jansOptPythonFolder, 'libs')


    def install(self):

        self.installJettyService(self.jetty_app_configuration[self.service_name], True)
        self.copyFile(self.source_files[0][0], self.jetty_service_webapps)

        base.extract_subdir(base.current_app.jans_zip, 'jans-casa/extras', self.pylib_dir)
        self.casa_scopes = self.create_scopes()
        self.add_plugins()
        self.enable()

    def add_plugins(self):
        jans_auth_web_app_xml = self.readFile(base.current_app.JansAuthInstaller.web_app_xml_fn)

        for plugin,_ in self.source_files[1:4]:
            plugin_name = os.path.basename(plugin)
            if plugin_name not in jans_auth_web_app_xml:
                plugin_class_path = os.path.join(base.current_app.JansAuthInstaller.custom_lib_dir, plugin_name)
                if not os.path.exists(plugin_class_path):
                    self.logIt("Adding plugin {} to jans-auth".format(plugin_name))
                    self.copyFile(plugin, base.current_app.JansAuthInstaller.custom_lib_dir)
                    self.chown(plugin_class_path, Config.jetty_user, Config.jetty_group)



    def generate_configuration(self):
        self.casa_scopes = self.create_scopes()

        self.check_clients([('casa_client_id', self.client_id_prefix)])

        if not Config.get('casa_client_pw'):
            Config.casa_client_pw = self.getPW()
            Config.casa_client_encoded_pw = self.obscure(Config.jca_client_pw)

    def render_import_templates(self):

        Config.templateRenderingDict['casa_redirect_uri'] = f'https://{Config.hostname}/{self.service_name}'
        Config.templateRenderingDict['casa_redirect_logout_uri'] = f'https://{Config.hostname}/{self.service_name}/bye.zul'
        Config.templateRenderingDict['casa_frontchannel_logout_uri'] = f'https://{Config.hostname}/{self.service_name}/autologout'

        # prepare casa scipt ldif
        base64_script_file = self.generate_base64_file(os.path.join(self.pylib_dir, 'Casa.py'), 1)
        Config.templateRenderingDict['casa_person_authentication_script'] = base64_script_file
        self.renderTemplateInOut(self.ldif_auth_script_fn, self.templates_dir, self.output_folder)

        self.renderTemplateInOut(self.config_json_fn, self.templates_dir, self.output_folder)
        Config.templateRenderingDict['casa_config_base64'] = self.generate_base64_file(self.config_json_fn, 1)


        self.renderTemplateInOut(self.ldif_client_fn, self.templates_dir, self.output_folder)
        self.renderTemplateInOut(self.ldif_config_fn, self.templates_dir, self.output_folder)

        casa_client_ldif_parser = myLdifParser(self.ldif_client_fn)
        casa_client_ldif_parser.parse()

        casa_client_ldif_parser.entries[0][1]['jansScope'] += self.casa_scopes
        with open(self.ldif_client_fn, 'wb') as w:
            casa_client_ldif_writer = LDIFWriter(w)
            casa_client_ldif_writer.unparse(casa_client_ldif_parser.entries[0][0], casa_client_ldif_parser.entries[0][1])

        self.dbUtils.import_ldif([self.ldif_client_fn, self.ldif_config_fn, self.ldif_auth_script_fn])


    def create_folders(self):
        self.createDirs(self.pylib_dir)
        for cdir in ('plugins', 'static'):
            self.createDirs(os.path.join(self.jetty_service_dir, cdir))

    def create_scopes(self):
        self.logIt("Creating Casa client scopes")
        scopes = base.readJsonFile(self.scopes_fn)
        casa_scopes_ldif_fn = os.path.join(self.output_folder, 'scopes.ldif')
        self.createDirs(self.output_folder)
        scope_ldif_fd = open(casa_scopes_ldif_fn, 'wb')
        scopes_list = []

        ldif_scopes_writer = LDIFWriter(scope_ldif_fd, cols=1000)

        for scope in scopes:
            scope_dn = 'inum={},ou=scopes,o=jans'.format(scope['inum'])
            scopes_list.append(scope_dn)
            ldif_dict = {
                        'objectClass': ['top', 'jansScope'],
                        'description': [scope['description']],
                        'displayName': [scope['displayName']],
                        'inum': [scope['inum']],
                        'jansDefScope': [str(scope['jansDefScope'])],
                        'jansId': [scope['jansId']],
                        'jansScopeTyp': [scope['jansScopeTyp']],
                        'jansAttrs': [json.dumps({"spontaneousClientId":None, "spontaneousClientScopes":[], "showInConfigurationEndpoint": False})],
                    }
            ldif_scopes_writer.unparse(scope_dn, ldif_dict)

        scope_ldif_fd.close()

        self.dbUtils.import_ldif([casa_scopes_ldif_fn])

        return scopes_list

    def service_post_setup(self):
        self.writeFile(os.path.join(self.jetty_service_dir, '.administrable'), '', backup=False)
        self.chown(self.pylib_dir, Config.jetty_user, Config.jetty_group, recursive=True)
        self.chown(self.jetty_service_dir, Config.jetty_user, Config.jetty_group, recursive=True)
