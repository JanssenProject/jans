import os
import time
import glob
import json
import ruamel.yaml
import ldap3

from string import Template

from setup_app import paths
from setup_app.static import AppType, InstallOption, BackendTypes
from setup_app.utils import base
from setup_app.config import Config
from setup_app.utils.setup_utils import SetupUtils
from setup_app.installers.base import BaseInstaller
from setup_app.pylib.ldif4.ldif import LDIFWriter

class AdminUIInstaller(SetupUtils, BaseInstaller):

    def __init__(self):
        setattr(base.current_app, self.__class__.__name__, self)
        self.service_name = 'gluu-admin-ui'
        self.needdb = True
        self.app_type = AppType.SERVICE
        self.install_type = InstallOption.OPTONAL
        self.install_var = 'installAdminUI'
        self.register_progess()
        self.output_folder = os.path.join(Config.outputFolder,'gluu-admin-ui')
        self.clients_ldif_fn = os.path.join(self.output_folder, 'clients.ldif')
        self.root_dir = os.path.join(Config.jansOptFolder, 'gluu-admin-ui')
        self.gluuOxVersion = '5.0.0-SNAPSHOT'
        self.source_files = [
                (os.path.join(Config.distJansFolder, 'gluu-admin-ui-app.jar'), 'https://ox.gluu.org/maven/org/gluu/gluu-admin-ui-app/{0}/gluu-admin-ui-app-{0}.jar'.format(self.gluuOxVersion))
                ]
        self.load_ldif_files = []


    def install(self):
        self.download_files(downloads=[self.source_files[0][0]])
        self.copyFile(self.source_files[0][0], self.root_dir)

        self.generate_configuration()
        self.render_import_templates()

    def installed(self):
        return os.path.exists(self.root_dir)


    def create_folders(self):
        for d in (self.root_dir,self.output_folder):
            if not os.path.exists(d):
                self.createDirs(d)

        self.run([paths.cmd_chown, '-R', 'jetty:jetty', self.root_dir])


    def generate_configuration(self):

        self.check_clients([('admin_ui_client_id', '1901.')])

        if not Config.get('admin_ui_client_pw'):
            Config.admin_ui_client_pw = self.getPW(32)
            Config.admin_ui_client_encoded_pw = self.obscure(Config.admin_ui_client_pw)

        createClient = True
        config_api_dn = 'inum={},ou=clients,o=jans'.format(Config.admin_ui_client_id)
        if Config.installed_instance and self.dbUtils.search('ou=clients,o=jans', search_filter='(&(inum={})(objectClass=jansClnt))'.format(Config.admin_ui_client_id)):
            createClient = False

        if createClient:
            clients_ldif_fd = open(self.clients_ldif_fn, 'wb')
            ldif_clients_writer = LDIFWriter(clients_ldif_fd, cols=1000)
            ldif_clients_writer.unparse(
                config_api_dn, {
                'objectClass': ['top', 'jansClnt'],
                'del': ['false'],
                'displayName': ['Jans Admin UI Client'],
                'inum': [Config.admin_ui_client_id],
                'jansAccessTknAsJwt': ['false'],
                'jansAccessTknSigAlg': ['RS256'],
                'jansAppTyp': ['web'],
                'jansAttrs': ['{"tlsClientAuthSubjectDn":"","runIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims":false,"keepClientAuthorizationAfterExpiration":false,"allowSpontaneousScopes":false,"spontaneousScopes":[],"spontaneousScopeScriptDns":[],"backchannelLogoutUri":[],"backchannelLogoutSessionRequired":false,"additionalAudience":[],"postAuthnScripts":[],"consentGatheringScripts":[],"introspectionScripts":[],"rptClaimsScripts":[]}'],
                'jansClntSecret': [Config.admin_ui_client_encoded_pw],
                'jansDefAcrValues': ['simple_password_auth'],
                'jansDisabled': ['false'],
                'jansGrantTyp': ['authorization_code', 'refresh_token', 'client_credentials'],
                'jansIdTknSignedRespAlg': ['RS256'],
                'jansInclClaimsInIdTkn': ['false'],
                'jansLogoutSessRequired': ['false'],
                'jansPersistClntAuthzs': ['true'],
                'jansRequireAuthTime': ['false'],
                'jansRespTyp': ['code'],
                'jansRptAsJwt': ['false'],
                'jansPostLogoutRedirectURI': ['http://localhost:4100'],
                'jansRedirectURI': ['http://localhost:4100'],
                'jansLogoutURI': ['http://localhost:4100/logout'],
                'jansScope': ['inum=43F1,ou=scopes,o=jans','inum=6D90,ou=scopes,o=jans','inum=FOC4,ou=scopes,o=jans'],
                'jansSubjectTyp': ['pairwise'],
                'jansTknEndpointAuthMethod': ['client_secret_basic'],
                'jansTrustedClnt': ['false'],
                })

            clients_ldif_fd.close()
            self.load_ldif_files.append(self.clients_ldif_fn)

        admin_dn = 'inum={},ou=people,o=jans'.format(Config.admin_inum)
        backend_location = self.dbUtils.get_backend_location_for_dn(admin_dn)

        result = self.dbUtils.dn_exists(admin_dn)
        if result and not 'jansAdminUIRole' in result:
            if backend_location == BackendTypes.LDAP:
                ldap_operation_result = self.dbUtils.ldap_conn.modify(
                    admin_dn,
                    {'jansAdminUIRole': [ldap3.MODIFY_ADD, 'api-admin']})
                self.dbUtils.log_ldap_result(ldap_operation_result)

    def render_import_templates(self):
        self.dbUtils.import_ldif(self.load_ldif_files)


