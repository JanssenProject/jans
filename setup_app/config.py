import re
import os
import json

from collections import OrderedDict

from setup_app.paths import DATA_DIR, INSTALL_DIR
from setup_app.static import InstallTypes
from setup_app.utils.printVersion import get_war_info
from setup_app.utils.base import os_type, os_version, os_initdaemon

class Config:

    @classmethod
    def determine_version(self):
        oxauth_info = get_war_info(os.path.join(self.distGluuFolder, 'oxauth.war'))
        self.oxVersion = oxauth_info['version']
        self.currentGluuVersion = re.search('([\d.]+)', oxauth_info['version']).group().strip('.')
        self.githubBranchName = oxauth_info['branch']

        self.ce_setup_zip = 'https://github.com/GluuFederation/community-edition-setup/archive/%s.zip' % self.githubBranchName

    @classmethod
    def init(self, install_dir=INSTALL_DIR):

        self.install_dir = install_dir
        self.thread_queue = None

        #create dummy progress bar in case not defined
        class DummyProgressBar:
            def __init__(self, *args):
                 pass

            def complete(self, *args):
                pass

            def progress(self, *args):
                pass

        self.pbar = DummyProgressBar()

        self.properties_password = None
        self.noPrompt = False

        self.distFolder = '/opt/dist'
        self.distAppFolder = os.path.join(self.distFolder, 'app')
        self.distGluuFolder = os.path.join(self.distFolder, 'gluu')
        self.distTmpFolder = os.path.join(self.distFolder, 'tmp')

        self.downloadWars = None
        self.templateRenderingDict = {
                                        'oxauthClient_2_inum': 'AB77-1A2B',
                                        'oxauthClient_3_inum': '3E20',
                                        'oxauthClient_4_inum': 'FF81-2D39',
                                        'idp_attribute_resolver_ldap.search_filter': '(|(uid=$requestContext.principalName)(mail=$requestContext.principalName))',
                                        'oxd_port': '8443',
                                     }

        # java commands
        self.jre_home = '/opt/jre'
        self.cmd_java = os.path.join(self.jre_home, 'bin/java')
        self.cmd_keytool = os.path.join(self.jre_home, 'bin/keytool')
        self.cmd_jar = os.path.join(self.jre_home, 'bin/jar')
        os.environ['OPENDJ_JAVA_HOME'] =  self.jre_home

        # Component ithversions
        self.apache_version = None
        self.opendj_version = None

        #passwords
        self.ldapPass = None
        self.oxtrust_admin_password = None
        self.encoded_admin_password = ''
        self.cb_password = None
        self.encoded_cb_password = ''

        #DB installation types
        self.wrends_install = InstallTypes.LOCAL
        self.cb_install = InstallTypes.NONE

        # Gluu components installation status
        self.loadData = True
        self.installOxAuth = True
        self.installOxTrust = True
        self.installHttpd = True
        self.installSaml = False
        self.installOxAuthRP = False
        self.installPassport = False
        self.installGluuRadius = False
        self.installScimServer = False
        self.installFido2 = False

        self.gluuPassportEnabled = 'false'
        self.gluuRadiusEnabled = 'false'
        self.gluuSamlEnabled = 'false'
        self.scimTestMode = 'false'

        self.enable_scim_access_policy = 'false'
        
        self.allowPreReleasedFeatures = False

        # backward compatibility
        self.os_type = os_type
        self.os_version = os_version
        self.os_initdaemon = os_initdaemon

        self.persistence_type = 'ldap'
        self.shibboleth_version = 'v3'

        self.setup_properties_fn = os.path.join(self.install_dir, 'setup.properties')
        self.savedProperties = os.path.join(self.install_dir, 'setup.properties.last')

        self.gluuOptFolder = '/opt/gluu'
        self.gluuOptBinFolder = os.path.join(self.gluuOptFolder, 'bin')
        self.gluuOptSystemFolder = os.path.join(self.gluuOptFolder, 'system')
        self.gluuOptPythonFolder = os.path.join(self.gluuOptFolder, 'python')
        self.gluuBaseFolder = '/etc/gluu'
        self.configFolder = os.path.join(self.gluuBaseFolder, 'conf') 
        self.fido2ConfigFolder = os.path.join(self.configFolder, 'fido2')
        self.certFolder = '/etc/certs'
        
        self.gluu_properties_fn = os.path.join(self.configFolder,'gluu.properties')
        self.gluu_hybrid_roperties = os.path.join(self.configFolder, 'gluu-hybrid.properties')

        self.oxBaseDataFolder = '/var/gluu'
        self.oxPhotosFolder = '/var/gluu/photos'
        self.oxTrustRemovedFolder = '/var/gluu/identity/removed'
        self.oxTrustCacheRefreshFolder = '/var/gluu/identity/cr-snapshots'
        self.cache_provider_type = 'NATIVE_PERSISTENCE'

        self.etc_hosts = '/etc/hosts'
        self.etc_hostname = '/etc/hostname'
        # OS /etc/default folder
        self.osDefault = '/etc/default'
        self.sysemProfile = '/etc/profile'

        self.jython_home = '/opt/jython'

        self.node_home = '/opt/node'
        self.node_initd_script = os.path.join(self.install_dir, 'static/system/initd/node')
        self.node_base = os.path.join(self.gluuOptFolder, 'node')
        self.node_user_home = '/home/node'
        self.passport_initd_script = os.path.join(self.install_dir, 'static/system/initd/passport')

        self.java_type = 'jre'

        self.jetty_home = '/opt/jetty'
        self.jetty_base = os.path.join(self.gluuOptFolder, 'jetty')
        self.jetty_user_home = '/home/jetty'
        self.jetty_user_home_lib = os.path.join(self.jetty_user_home, 'lib')

        with open(os.path.join(DATA_DIR, 'jetty_app_configuration.json')) as f:
            self.jetty_app_configuration = json.load(f, object_pairs_hook=OrderedDict)

        self.app_custom_changes = {
            'jetty' : {
                'name' : 'jetty',
                'files' : [
                    {
                        'path' : os.path.join(self.jetty_home, 'etc/webdefault.xml'),
                        'replace' : [
                            {
                                'pattern' : r'(\<param-name\>dirAllowed<\/param-name\>)(\s*)(\<param-value\>)true(\<\/param-value\>)',
                                'update' : r'\1\2\3false\4'
                            }
                        ]
                    },
                    {
                        'path' : os.path.join(self.jetty_home, 'etc/jetty.xml'),
                        'replace' : [
                            {
                                'pattern' : '<New id="DefaultHandler" class="org.eclipse.jetty.server.handler.DefaultHandler"/>',
                                'update' : '<New id="DefaultHandler" class="org.eclipse.jetty.server.handler.DefaultHandler">\n\t\t\t\t <Set name="showContexts">false</Set>\n\t\t\t </New>'
                            }
                        ]
                    }
                ]
            }
        }

        self.idp3Folder = '/opt/shibboleth-idp'
        self.idp3MetadataFolder = os.path.join(self.idp3Folder, 'metadata')
        self.idp3MetadataCredentialsFolder = os.path.join(self.idp3MetadataFolder, 'credentials')
        self.idp3LogsFolder = os.path.join(self.idp3Folder, 'logs')
        self.idp3LibFolder = os.path.join(self.idp3Folder, 'lib')
        self.idp3ConfFolder = os.path.join(self.idp3Folder, 'conf')
        self.idp3ConfAuthnFolder = os.path.join(self.idp3Folder, 'conf/authn')
        self.idp3CredentialsFolder = os.path.join(self.idp3Folder, 'credentials')
        self.idp3WebappFolder = os.path.join(self.idp3Folder, 'webapp')
        self.couchbaseShibUserPassword = None

        self.hostname = None
        self.ip = None
        self.orgName = None
        self.countryCode = None
        self.city = None
        self.state = None
        self.admin_email = None
        self.encoded_ox_ldap_pw = None
        self.encoded_shib_jks_pw = None
        self.application_max_ram = 3072    # in MB
        self.encode_salt = None
        self.admin_inum = None

        self.ldapBaseFolderldapPass = None

        self.oxauth_client_id = None
        self.oxauthClient_pw = None
        self.oxauthClient_encoded_pw = None

        self.idp_client_id = None
        self.idpClient_pw = None
        self.idpClient_encoded_pw = None

        self.oxTrustConfigGeneration = None

        self.outputFolder = os.path.join(self.install_dir, 'output')
        self.templateFolder = os.path.join(self.install_dir, 'templates')
        self.staticFolder = os.path.join(self.install_dir, 'static')

        self.extensionFolder = os.path.join(self.staticFolder, 'extension')

        self.oxauth_error_json = os.path.join(self.staticFolder, 'oxauth/oxauth-errors.json')

        self.oxauth_openid_jwks_fn = os.path.join(self.outputFolder, 'oxauth-keys.json')
        self.oxauth_openid_jks_fn = os.path.join(self.certFolder, 'oxauth-keys.jks')
        self.oxauth_openid_jks_pass = None

        self.httpdKeyPass = None
        self.httpdKeyFn = os.path.join(self.certFolder, 'httpd.key')
        self.httpdCertFn = os.path.join(self.certFolder, 'httpd.crt')
        self.shibJksPass = None
        self.shibJksFn = os.path.join(self.certFolder, 'shibIDP.jks')

        self.ldapTrustStoreFn = None
        self.encoded_ldapTrustStorePass = None

        self.opendj_cert_fn = os.path.join(self.certFolder, 'opendj.crt')
        self.opendj_p12_fn = os.path.join(self.certFolder, 'opendj.pkcs12')
        self.opendj_p12_pass = None

        self.ldap_binddn = 'cn=directory manager'
        self.ldap_hostname = 'localhost'
        self.couchbase_hostname = 'localhost'
        self.ldap_port = '1389'
        self.ldaps_port = '1636'
        self.ldap_admin_port = '4444'
        self.ldapBaseFolder = '/opt/opendj'

        self.ldapSetupCommand = os.path.join(self.ldapBaseFolder, 'setup')
        self.ldapDsconfigCommand = os.path.join(self.ldapBaseFolder, 'bin/dsconfig')
        self.ldapDsCreateRcCommand = os.path.join(self.ldapBaseFolder, 'bin/create-rc-script')
        self.ldapDsJavaPropCommand = os.path.join(self.ldapBaseFolder, 'bin/dsjavaproperties')
        self.importLdifCommand = os.path.join(self.ldapBaseFolder, 'bin/import-ldif')
        self.ldapModifyCommand = os.path.join(self.ldapBaseFolder, 'bin/ldapmodify')
        self.loadLdifCommand = self.ldapModifyCommand

        self.ldap_user_home = '/home/ldap'
        self.ldapPassFn = os.path.join(self.ldap_user_home, '.pw')
        self.ldap_backend_type = 'je'

        self.gluuScriptFiles = [
                            os.path.join(self.install_dir, 'static/scripts/logmanager.sh'),
                            os.path.join(self.install_dir, 'static/scripts/testBind.py')
                            ]

        self.openDjIndexJson = os.path.join(self.install_dir, 'static/opendj/index.json')
        self.openDjSchemaFolder = os.path.join(self.ldapBaseFolder, 'config/schema')
        self.openDjschemaFiles = [
                            os.path.join(self.install_dir, 'static/opendj/96-eduperson.ldif'),
                            os.path.join(self.install_dir, 'static/opendj/101-ox.ldif'),
                            os.path.join(self.install_dir, 'static/opendj/77-customAttributes.ldif')
                            ]

        self.opendj_init_file = os.path.join(self.install_dir, 'static/opendj/opendj')
        self.opendj_service_centos7 = os.path.join(self.install_dir, 'static/opendj/systemd/opendj.service')

        self.redhat_services = ['httpd', 'rsyslog']
        self.debian_services = ['apache2', 'rsyslog']

        self.apache_start_script = '/etc/init.d/httpd'

        self.defaultTrustStoreFN = os.path.join(self.jre_home, 'jre/lib/security/cacerts')
        self.defaultTrustStorePW = 'changeit'

        self.passportSpKeyPass = None
        self.passportSpTLSCACert = os.path.join(self.certFolder, 'passport-sp.pem')
        self.passportSpTLSCert = os.path.join(self.certFolder, 'passport-sp.crt')
        self.passportSpTLSKey = os.path.join(self.certFolder, 'passport-sp.key')
        self.passportSpJksPass = None
        self.passportSpJksFn = os.path.join(self.certFolder, 'passport-sp.jks')


        # Stuff that gets rendered; filename is necessary. Full path should
        # reflect final path if the file must be copied after its rendered.
        self.passport_central_config_json = os.path.join(self.outputFolder, 'passport-central-config.json')
        self.oxauth_config_json = os.path.join(self.outputFolder, 'oxauth-config.json')
        self.oxtrust_config_json = os.path.join(self.outputFolder, 'oxtrust-config.json')
        self.oxtrust_cache_refresh_json = os.path.join(self.outputFolder, 'oxtrust-cache-refresh.json')
        self.oxtrust_import_person_json = os.path.join(self.outputFolder, 'oxtrust-import-person.json')
        self.oxidp_config_json = os.path.join(self.outputFolder, 'oxidp-config.json')
        self.gluu_python_base = os.path.join(self.gluuOptFolder, 'python')
        self.gluu_python_readme = os.path.join(self.gluuOptPythonFolder, 'libs/python.txt')
        self.ox_ldap_properties = os.path.join(self.configFolder, 'gluu-ldap.properties')
        self.oxauth_static_conf_json = os.path.join(self.outputFolder, 'oxauth-static-conf.json')
        self.oxTrust_log_rotation_configuration = os.path.join(self.gluuBaseFolder, 'conf/oxTrustLogRotationConfiguration.xml')
        self.apache2_conf = os.path.join(self.outputFolder, 'httpd.conf')
        self.apache2_ssl_conf = os.path.join(self.outputFolder, 'https_gluu.conf')
        self.apache2_24_conf = os.path.join(self.outputFolder, 'httpd_2.4.conf')
        self.apache2_ssl_24_conf = os.path.join(self.outputFolder, 'https_gluu.conf')
        self.ldif_base = os.path.join(self.outputFolder, 'base.ldif')
        self.ldif_attributes = os.path.join(self.outputFolder, 'attributes.ldif')
        self.ldif_scopes = os.path.join(self.outputFolder, 'scopes.ldif')
        self.ldif_clients = os.path.join(self.outputFolder, 'clients.ldif')
        self.ldif_people = os.path.join(self.outputFolder, 'people.ldif')
        self.ldif_groups = os.path.join(self.outputFolder, 'groups.ldif')
        
        self.ldif_metric = os.path.join(self.staticFolder, 'metric/o_metric.ldif')
        self.ldif_site = os.path.join(self.install_dir, 'static/cache-refresh/o_site.ldif')
        self.ldif_scripts = os.path.join(self.outputFolder, 'scripts.ldif')
        self.ldif_configuration = os.path.join(self.outputFolder, 'configuration.ldif')
        self.ldif_scim = os.path.join(self.outputFolder, 'scim.ldif')
        self.ldif_scim_clients = os.path.join(self.outputFolder, 'scim_clients.ldif')
        
        self.fido2_dynamic_conf_json = os.path.join(self.outputFolder, 'fido2-dynamic-conf.json')
        self.fido2_static_conf_json = os.path.join(self.outputFolder, 'fido2-static-conf.json')
        self.ldif_fido2 = os.path.join(self.outputFolder, 'fido2.ldif')
        
        self.lidf_oxtrust_api = os.path.join(self.outputFolder, 'oxtrust_api.ldif')
        self.ldif_oxtrust_api_clients = os.path.join(self.outputFolder, 'oxtrust_api_clients.ldif')

        self.ldif_scripts_casa = os.path.join(self.outputFolder, 'scripts_casa.ldif')
        self.passport_config = os.path.join(self.configFolder, 'passport-config.json')
        self.encode_script = os.path.join(self.gluuOptFolder, 'bin/encode.py')
        self.network = '/etc/sysconfig/network'
        self.system_profile_update_init = os.path.join(self.outputFolder, 'system_profile_init')
        self.system_profile_update_systemd = os.path.join(self.outputFolder, 'system_profile_systemd')

        self.staticIDP3FolderConf = os.path.join(self.install_dir, 'static/idp3/conf')
        self.staticIDP3FolderMetadata = os.path.join(self.install_dir, 'static/idp3/metadata')
        self.idp3_configuration_properties = 'idp.properties'
        self.idp3_configuration_ldap_properties = 'ldap.properties'
        self.idp3_configuration_saml_nameid = 'saml-nameid.properties'
        self.idp3_configuration_services = 'services.properties'
        self.idp3_configuration_password_authn = 'authn/password-authn-config.xml'
        self.idp3_metadata = 'idp-metadata.xml'
        self.data_source_properties = 'datasource.properties'

        ### rsyslog file customised for init.d
        self.rsyslogUbuntuInitFile = os.path.join(self.install_dir, 'static/system/ubuntu/rsyslog')
        self.ldap_setup_properties = os.path.join(self.templateFolder, 'opendj-setup.properties')

        # oxAuth/oxTrust Base64 configuration files
        self.pairwiseCalculationKey = None
        self.pairwiseCalculationSalt = None

        # OpenID key generation default setting
        self.default_openid_jks_dn_name = 'CN=oxAuth CA Certificates'
        self.default_key_algs = 'RS256 RS384 RS512 ES256 ES384 ES512'
        self.default_key_expiration = 365

        # oxTrust SCIM configuration
        self.scim_rs_client_id = None
        self.scim_rs_client_jwks = None
        self.scim_rs_client_jks_fn = os.path.join(self.certFolder, 'scim-rs.jks')
        self.scim_rs_client_jks_pass = None
        self.scim_rs_client_jks_pass_encoded = None

        self.scim_rp_client_id = None
        self.scim_rp_client_jwks = None
        self.scim_rp_client_jks_fn = os.path.join(self.outputFolder, 'scim-rp.jks')
        self.scim_rp_client_jks_pass = 'secret'
        self.scim_resource_oxid = None

        # oxTrust Api configuration
        self.api_rs_client_jks_fn = os.path.join(self.certFolder, 'api-rs.jks')
        self.api_rs_client_jks_pass = 'secret'
        self.api_rs_client_jwks = None
        self.api_rp_client_jks_fn = os.path.join(self.certFolder, 'api-rp.jks')
        self.api_rp_client_jks_pass = 'secret'
        self.api_rp_client_jwks = None

        self.oxtrust_resource_id = None
        self.oxtrust_requesting_party_client_id = None
        self.oxtrust_resource_server_client_id = None

        # oxPassport Configuration
        self.gluu_passport_base = os.path.join(self.node_base, 'passport')
        self.passport_oxtrust_config_fn = os.path.join(self.outputFolder, 'passport_oxtrust_config.son')
        self.ldif_passport_config = os.path.join(self.outputFolder, 'oxpassport-config.ldif')
        self.ldif_passport = os.path.join(self.outputFolder, 'passport.ldif')
        self.ldif_passport_clients = os.path.join(self.outputFolder, 'passport_clients.ldif')
        self.ldif_idp = os.path.join(self.outputFolder, 'oxidp.ldif')
        
        self.passport_rs_client_id = None
        self.passport_rs_client_jwks = None
        self.passport_rs_client_jks_fn = os.path.join(self.certFolder, 'passport-rs.jks')
        self.passport_rs_client_jks_pass = None
        self.passport_rs_client_jks_pass_encoded = None

        self.passport_rp_ii_client_id = None
        self.passport_rp_client_id = None
        self.passport_rp_client_jwks = None
        self.passport_rp_client_jks_fn = os.path.join(self.certFolder, 'passport-rp.jks')
        self.passport_rp_client_cert_alg = 'RS512'
        self.passport_rp_client_cert_alias = None
        self.passport_rp_client_cert_fn = os.path.join(self.certFolder, 'passport-rp.pem')
        self.passport_rp_client_jks_pass = 'secret'
        self.passport_resource_id = None
        
        self.oxauth_legacyIdTokenClaims = 'false'
        self.oxauth_openidScopeBackwardCompatibility =  'false'
        self.enableRadiusScripts = 'false'
        self.gluu_radius_client_id = None
        self.gluu_ro_pw = None
        self.gluu_ro_encoded_pw = None
        self.ox_radius_client_id = None
        self.oxRadiusClientIpAddress = None
        self.oxRadiusClientName = None
        self.oxRadiusClientSecret = None
        self.radius_dir = os.path.join(self.gluuOptFolder, 'radius')

        #definitions for couchbase
        self.couchebaseInstallDir = '/opt/couchbase/'
        self.couchebaseClusterAdmin = 'admin'
        self.isCouchbaseUserAdmin = False
        self.couchbasePackageFolder = os.path.join(self.distFolder, 'couchbase')
        self.couchbaseTrustStoreFn = os.path.join(self.certFolder, 'couchbase.pkcs12')
        self.couchbaseTrustStorePass = 'newsecret'
        self.n1qlOutputFolder = os.path.join(self.outputFolder,'n1ql')
        self.couchbaseIndexJson = os.path.join(self.install_dir, 'static/couchbase/index.json')
        self.couchbaseInitScript = os.path.join(self.install_dir, 'static/system/initd/couchbase-server')
        self.couchebaseCert = os.path.join(self.certFolder, 'couchbase.pem')
        self.gluuCouchebaseProperties = os.path.join(self.configFolder, 'gluu-couchbase.properties')
        self.couchbaseBuckets = []
        self.cbm = None
        self.cb_query_node = 0
        self.cb_bucket_roles = ['bucket_admin', 'query_delete', 'query_select', 
                            'query_update', 'query_insert',
                            'query_manage_index']
        self.post_messages = []
        self.couchbase_bucket_prefix = 'gluu'

        #oxd install options
        self.installOxd = False
        self.oxd_package = ''
        self.oxd_use_gluu_storage = False
        self.generateOxdCertificate = False

        #casa install options
        self.installCasa = False
        self.twilio_version = '7.17.0'
        self.jsmmp_version = '2.3.7'
        self.oxd_server_https = ''
        self.ldif_casa = os.path.join(self.outputFolder, 'casa.ldif')

        self.ldif_files = [self.ldif_base,
                           self.ldif_attributes,
                           self.ldif_scopes,
                           self.ldif_clients,
                           self.ldif_people,
                           self.ldif_groups,
                           self.ldif_site,
                           self.ldif_metric,
                           self.ldif_scripts,
                           self.ldif_configuration,
                           self.ldif_scim,
                           self.ldif_scim_clients,
                           self.ldif_idp,
                           self.lidf_oxtrust_api,
                           self.ldif_oxtrust_api_clients,
                           self.ldif_casa,
                           self.ldif_fido2,
                           ]


        self.ce_templates = {self.oxauth_config_json: False,
                             self.gluu_python_readme: True,
                             self.oxtrust_config_json: False,
                             self.oxtrust_cache_refresh_json: False,
                             self.oxtrust_import_person_json: False,
                             self.oxidp_config_json: False,
                             self.ox_ldap_properties: True,
                             self.oxauth_static_conf_json: False,
                             self.oxTrust_log_rotation_configuration: True,
                             self.ldap_setup_properties: False,
                             self.apache2_conf: False,
                             self.apache2_ssl_conf: False,
                             self.apache2_24_conf: False,
                             self.apache2_ssl_24_conf: False,
                             self.etc_hostname: False,
                             self.ldif_base: False,
                             self.ldif_attributes: False,
                             self.ldif_scopes: False,
                             self.ldif_clients: False,
                             self.ldif_people: False,
                             self.ldif_groups: False,
                             self.ldif_scripts: False,
                             self.ldif_scim: False,
                             self.ldif_scim_clients: False,
                             self.ldif_idp: False,
                             self.network: False,
                             self.ldif_scripts_casa: False,
                             self.lidf_oxtrust_api: False,
                             self.ldif_oxtrust_api_clients: False,
                             self.gluu_properties_fn: True,
                             self.data_source_properties: False,
                             self.ldif_casa: False,
                             self.fido2_dynamic_conf_json: False,
                             self.fido2_static_conf_json: False,
                             }

        self.service_requirements = {
                        'opendj': ['', 70],
                        'oxauth': ['opendj', 72],
                        'fido2': ['opendj', 73],
                        'identity': ['opendj oxauth', 74],
                        'scim': ['opendj oxauth', 75],
                        'idp': ['opendj oxauth', 76],
                        'casa': ['opendj oxauth', 78],
                        'oxd-server': ['opendj oxauth', 80],
                        'passport': ['opendj oxauth', 82],
                        'oxauth-rp': ['opendj oxauth', 84],
                        'gluu-radius': ['opendj oxauth', 86],
                        }

        self.install_time_ldap = None
        
        
        self.couchbaseBucketDict = OrderedDict((   
                        ('default', { 'ldif':[
                                            self.ldif_base, 
                                            self.ldif_attributes,
                                            self.ldif_scopes,
                                            self.ldif_scripts,
                                            self.ldif_configuration,
                                            self.ldif_scim,
                                            self.ldif_fido2,
                                            self.ldif_idp,
                                            self.lidf_oxtrust_api,
                                            self.ldif_clients,
                                            self.ldif_oxtrust_api_clients,
                                            self.ldif_scim_clients,
                                            self.ldif_metric,
                                            ],
                                      'memory_allocation': 100,
                                      'mapping': '',
                                      'document_key_prefix': []
                                    }),

                        ('user',     {   'ldif': [
                                            self.ldif_people, 
                                            self.ldif_groups
                                            ],
                                        'memory_allocation': 300,
                                        'mapping': 'people, groups, authorizations',
                                        'document_key_prefix': ['groups_', 'people_', 'authorizations_'],
                                    }),

                        ('cache',    {   'ldif': [],
                                        'memory_allocation': 300,
                                        'mapping': 'cache',
                                        'document_key_prefix': ['cache_'],
                                    }),

                        ('site',     {   'ldif': [self.ldif_site],
                                        'memory_allocation': 100,
                                        'mapping': 'cache-refresh',
                                        'document_key_prefix': ['site_', 'cache-refresh_'],
                                        
                                    }),

                        ('token',   { 'ldif': [],
                                      'memory_allocation': 300,
                                      'mapping': 'tokens',
                                      'document_key_prefix': ['tokens_'],
                                    }),

                    ))

        self.mappingLocations = { group: 'ldap' for group in self.couchbaseBucketDict }  #default locations are OpenDJ
        self.non_setup_properties = {
            'oxauth_client_jar_fn': os.path.join(self.distGluuFolder, 'oxauth-client-jar-with-dependencies.jar')
                }
