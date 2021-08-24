import os
import glob
import shutil
import ruamel.yaml

from setup_app.utils import base
from setup_app.static import AppType, InstallOption
from setup_app.config import Config
from setup_app.installers.jetty import JettyInstaller
from setup_app.pylib.ldif4.ldif import LDIFWriter

class ScimInstaller(JettyInstaller):

    def __init__(self):
        setattr(base.current_app, self.__class__.__name__, self)
        self.service_name = 'jans-scim'
        self.needdb = True
        self.app_type = AppType.SERVICE
        self.install_type = InstallOption.OPTONAL
        self.install_var = 'installScimServer'
        self.register_progess()

        self.source_files = [
                (os.path.join(Config.distJansFolder, 'jans-scim.war'), 'https://ox.gluu.org/maven/org/gluu/scim-server/{0}/scim-server-{0}.war'.format(Config.oxVersion)),
                (os.path.join(Config.install_dir, 'setup_app/data/jans-scim-openapi.yaml'), 'https://raw.githubusercontent.com/JanssenProject/jans-scim/master/server/src/main/resources/jans-scim-openapi.yaml'),
                ]

        self.templates_folder = os.path.join(Config.templateFolder, self.service_name)
        self.output_folder = os.path.join(Config.outputFolder, self.service_name)

        self.dynamic_config_fn = os.path.join(self.output_folder, 'dynamic-conf.json')
        self.static_config_fn = os.path.join(self.output_folder, 'static-conf.json')
        self.ldif_config_fn = os.path.join(self.output_folder, 'configuration.ldif')
        self.ldif_clients_fn = os.path.join(self.output_folder, 'clients.ldif')
        self.ldif_scopes_fn = os.path.join(self.output_folder, 'scopes.ldif')


    def install(self):
        self.logIt("Copying scim.war into jetty webapps folder...")

        self.installJettyService(self.jetty_app_configuration[self.service_name], True)


        jettyServiceWebapps = os.path.join(self.jetty_base, self.service_name,  'webapps')
        self.copyFile(self.source_files[0][0], jettyServiceWebapps)

        self.enable()

    def installed(self):
        return os.path.exists(os.path.join(Config.jetty_base, self.service_name, 'start.ini'))

    def generate_configuration(self):
        self.logIt("Generating {} configuration".format(self.service_name))
        yml_str = self.readFile(os.path.join(self.source_files[1][0]))
        yml_str = yml_str.replace('\t', ' ')
        cfg_yml = ruamel.yaml.load(yml_str, ruamel.yaml.RoundTripLoader)
        config_scopes = cfg_yml['components']['securitySchemes']['scim_oauth']['flows']['clientCredentials']['scopes']

        scope_ldif_fd = open(self.ldif_scopes_fn, 'wb')
        ldif_scopes_writer = LDIFWriter(scope_ldif_fd, cols=1000)

        scopes_dn = []
        for scope in config_scopes:
            #print(scopes[scope])

            inum = '1200.' + os.urandom(3).hex().upper()
            scope_dn = 'inum={},ou=scopes,o=jans'.format(inum)
            scopes_dn.append(scope_dn)
            display_name = 'Scim {}'.format(os.path.basename(scope))
            ldif_scopes_writer.unparse(
                    scope_dn, {
                                'objectclass': ['top', 'jansScope'],
                                'description': [config_scopes[scope]],
                                'displayName': [display_name],
                                'inum': [inum],
                                'jansId': [scope],
                                'jansScopeTyp': ['oauth'],
                                })

        scope_ldif_fd.close()

        client_ldif_fd = open(self.ldif_clients_fn, 'wb')
        client_scopes_writer = LDIFWriter(client_ldif_fd, cols=1000)

        self.check_clients([('scim_client_id', '1201.')])

        if not Config.get('scim_client_pw'):
            Config.scim_client_pw = self.getPW()
            Config.scim_client_encoded_pw = self.obscure(Config.scim_client_pw)

        scim_client_dn = 'inum={},ou=clients,o=jans'.format(Config.scim_client_id)
        client_scopes_writer.unparse(
                scim_client_dn, {
                'objectClass': ['top', 'jansClnt'],
                'displayName': ['SCIM client'],
                'jansAccessTknSigAlg': ['RS256'],
                'jansAppTyp': ['native'],
                'jansAttrs': ['{}'],
                'jansGrantTyp':	['client_credentials'],
                'jansScope': scopes_dn,
                'jansSubjectTyp': ['pairwise'],
                'jansTknEndpointAuthMethod': ['client_secret_basic'],
                'inum': [Config.scim_client_id],
                'jansClntSecret': [Config.scim_client_encoded_pw]
                })

        client_ldif_fd.close()

    def create_folders(self):
        self.createDirs(self.output_folder)

    def render_import_templates(self):

        self.renderTemplateInOut(self.dynamic_config_fn, self.templates_folder, self.output_folder)
        self.renderTemplateInOut(self.static_config_fn, self.templates_folder, self.output_folder)
        Config.templateRenderingDict['scim_dynamic_conf_base64'] = self.generate_base64_ldap_file(self.dynamic_config_fn)
        Config.templateRenderingDict['scim_static_conf_base64'] = self.generate_base64_ldap_file(self.static_config_fn)

        self.renderTemplateInOut(self.ldif_config_fn, self.templates_folder, self.output_folder)

        self.dbUtils.import_ldif([self.ldif_config_fn, self.ldif_scopes_fn, self.ldif_clients_fn])

        self.write_webapps_xml()

    def update_backend(self):
        self.dbUtils.enable_service('jansScimEnabled')
