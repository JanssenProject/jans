import os
import re
import glob
import ssl
import time
import json

from xml.etree import ElementTree

from setup_app import paths
from setup_app.static import AppType, InstallOption, SetupProfiles
from setup_app.utils import base
from setup_app.config import Config
from setup_app.utils.properties_utils import propertiesUtils
from setup_app.installers.jetty import JettyInstaller


class CasaInstaller(JettyInstaller):

    casa_dist_dir = os.path.join(Config.dist_jans_dir, 'casa')
    casa_github_repo = 'https://raw.githubusercontent.com/GluuFederation/flex/main/casa/'
    source_files = [
            (os.path.join(Config.dist_jans_dir, 'casa.war'), os.path.join(base.current_app.app_info['GLUU_MAVEN'], 'maven/org/gluu/casa/{0}/casa-{0}.war'.format(base.current_app.app_info['CASA_VERSION']))),
            (os.path.join(casa_dist_dir, 'casa_web_resources.xml'), os.path.join(casa_github_repo, 'extras/casa_web_resources.xml')),
            (os.path.join(casa_dist_dir, 'casa-config.jar'), os.path.join(base.current_app.app_info['GLUU_MAVEN'], 'maven/org/gluu/casa-config/{0}/casa-config-{0}.jar'.format(base.current_app.app_info['CASA_VERSION']))),
            (os.path.join(casa_dist_dir, 'jans-fido2-client.jar'), os.path.join(base.current_app.app_info['JANS_MAVEN'], 'maven/io/jans/jans-fido2-client/{0}{1}/jans-fido2-client-{0}{1}.jar'.format(base.current_app.app_info['JANS_APP_VERSION'], base.current_app.app_info['JANS_BUILD']))),
            (os.path.join(casa_dist_dir, 'twilio.jar'), 'https://repo1.maven.org/maven2/com/twilio/sdk/twilio/{0}/twilio-{0}.jar'.format(base.current_app.app_info['TWILIO_VERSION'])),
            (os.path.join(casa_dist_dir, 'pylib/Casa.py'), os.path.join(casa_github_repo, 'extras/Casa.py')),
            (os.path.join(casa_dist_dir, 'pylib/casa-external_fido2.py'), os.path.join(casa_github_repo, 'extras/casa-external_fido2.py')),
            (os.path.join(casa_dist_dir, 'pylib/casa-external_otp.py'), os.path.join(casa_github_repo, 'extras/casa-external_otp.py')),
            (os.path.join(casa_dist_dir, 'pylib/casa-external_super_gluu.py'), os.path.join(casa_github_repo, 'extras/casa-external_super_gluu.py')),
            (os.path.join(casa_dist_dir, 'pylib/casa-external_twilio_sms.py'), os.path.join(casa_github_repo, 'extras/casa-external_twilio_sms.py')),
            ]


    def __init__(self):
        setattr(base.current_app, self.__class__.__name__, self)
        self.service_name = 'casa'
        self.app_type = AppType.SERVICE
        self.install_type = InstallOption.OPTONAL
        self.install_var = 'install_casa'
        self.register_progess()

        self.templates_dir = os.path.join(Config.templateFolder, 'casa')
        self.output_dir = os.path.join(Config.output_dir, 'casa')
        self.jetty_service_dir = os.path.join(Config.jetty_base, self.service_name)
        self.jetty_service_webapps_dir = os.path.join(self.jetty_service_dir, 'webapps')
        self.jans_auth_custom_lib_dir = os.path.join(Config.jetty_base, base.current_app.JansAuthInstaller.service_name, 'custom/libs')
        self.py_lib_dir = os.path.join(Config.jansOptPythonFolder, 'libs')

    def install(self):

        self.installJettyService(self.jetty_app_configuration[self.service_name], True)
        self.copyFile(self.source_files[0][0], self.jetty_service_webapps_dir)
        self.copyFile(self.source_files[1][0], self.jetty_service_webapps_dir)
        self.chown(self.jetty_service_webapps_dir, Config.jetty_user, Config.jetty_group, recursive=True)

        self.enable()

    def generate_configuration(self):
        self.check_clients([('casa_client_id', '3000.')])

        if not Config.get('casa_client_encoded_pw'):
            Config.casa_client_encoded_pw = self.obscure(Config.casa_client_pw)


    def copy_static(self):
        self.logIt("Adding Fido2 Client lib, twillo and casa config to jans-auth")
        self.copyFile(self.source_files[2][0], self.jans_auth_custom_lib_dir)
        self.copyFile(self.source_files[3][0], self.jans_auth_custom_lib_dir)
        self.copyFile(self.source_files[4][0], self.jans_auth_custom_lib_dir)
        class_path = '{},{},{}'.format(
            os.path.join(self.jans_auth_custom_lib_dir, os.path.basename(self.source_files[2][0])),
            os.path.join(self.jans_auth_custom_lib_dir, os.path.basename(self.source_files[3][0])),
            os.path.join(self.jans_auth_custom_lib_dir, os.path.basename(self.source_files[4][0])),
            )
        base.current_app.JansAuthInstaller.add_extra_class(class_path)
        self.chown(self.jans_auth_custom_lib_dir, Config.jetty_user, Config.jetty_group, recursive=True)

        # copy casa scripts
        if not os.path.exists(self.py_lib_dir):
            os.makedirs(self.py_lib_dir)
        for fn in glob.glob(os.path.join(self.casa_dist_dir, 'pylib/*.py')):
            self.copyFile(fn, self.py_lib_dir)

        self.chown(self.py_lib_dir, Config.jetty_user, Config.jetty_group, recursive=True)


    def render_import_templates(self, import_script=True):

        casa_auth_script_fn = os.path.join(self.templates_dir, 'casa_person_authentication_script.ldif')
        base64_script_file = self.generate_base64_file(self.source_files[5][0], 1)
        Config.templateRenderingDict['casa_person_authentication_script'] = base64_script_file
        self.renderTemplateInOut(casa_auth_script_fn, self.templates_dir, self.output_dir)

        Config.templateRenderingDict['casa_redirect_uri'] = 'https://{}/casa'.format(Config.hostname)
        Config.templateRenderingDict['casa_redirect_logout_uri'] = 'https://{}/casa/bye.zul'.format(Config.hostname)
        Config.templateRenderingDict['casa_frontchannel_logout_uri'] = 'https://{}/casa/autologout'.format(Config.hostname)

        self.casa_client_fn = os.path.join(self.templates_dir, 'casa_client.ldif')
        self.casa_config_fn = os.path.join(self.templates_dir, 'casa_config.ldif')

        self.renderTemplateInOut(self.casa_client_fn, self.templates_dir, self.output_dir)
        self.renderTemplateInOut(self.casa_config_fn, self.templates_dir, self.output_dir)
        self.dbUtils.import_ldif([
                os.path.join(self.output_dir, os.path.basename(self.casa_client_fn)),
                os.path.join(self.output_dir, os.path.basename(self.casa_config_fn)),
                os.path.join(self.output_dir, os.path.basename(casa_auth_script_fn)),
                ])


    def create_folders(self):
        self.run([paths.cmd_mkdir, '-p', os.path.join(self.jetty_service_dir, 'static')])
        self.run([paths.cmd_mkdir, '-p', os.path.join(self.jetty_service_dir, 'plugins')])


    def update_backend(self):
        simple_auth_scr_inum = 'A51E-76DA'
        self.dbUtils.enable_script(simple_auth_scr_inum)
