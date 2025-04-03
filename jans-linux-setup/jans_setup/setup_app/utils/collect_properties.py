import os
import json
import zipfile
import re
import sys
import base64
import glob

from urllib.parse import urlparse

from setup_app import paths
from setup_app import static
from setup_app.static import SearchScopes

from setup_app.utils import base
from setup_app.config import Config
from setup_app.utils.db_utils import dbUtils
from setup_app.utils.setup_utils import SetupUtils
from setup_app.utils.properties_utils import propertiesUtils
from setup_app.pylib.jproperties import Properties
from setup_app.installers.jetty import JettyInstaller
from setup_app.installers.base import BaseInstaller
from setup_app.installers.jans_casa import CasaInstaller

class CollectProperties(SetupUtils, BaseInstaller):


    def __init__(self):
        pass

    def collect(self):
        print("Please wait while collecting properties...")
        self.logIt("Previously installed instance. Collecting properties")
        salt_fn = os.path.join(Config.configFolder,'salt')
        if os.path.exists(salt_fn):
            salt_prop = base.read_properties_file(salt_fn)
            Config.encode_salt = salt_prop['encodeSalt']

        jans_prop = base.read_properties_file(Config.jans_properties_fn)
        Config.persistence_type = jans_prop['persistence.type']
        jans_auth_ConfigurationEntryDN = jans_prop['jansAuth_ConfigurationEntryDN']
        jans_ConfigurationDN = 'ou=configuration,o=jans'


        if os.path.exists(Config.jansRDBMProperties):
            jans_sql_prop = base.read_properties_file(Config.jansRDBMProperties)

            uri_re = re.match('jdbc:(.*?)://(.*?):(.*?)/(.*)', jans_sql_prop['connection.uri'])
            Config.rdbm_type, Config.rdbm_host, Config.rdbm_port, Config.rdbm_db = uri_re.groups()
            if '?' in Config.rdbm_db:
                Config.rdbm_db = Config.rdbm_db.split('?')[0]
            Config.rdbm_port = int(Config.rdbm_port)
            Config.rdbm_install_type = static.InstallTypes.LOCAL if Config.rdbm_host == 'localhost' else static.InstallTypes.REMOTE
            Config.rdbm_user = jans_sql_prop['auth.userName']
            Config.rdbm_password_enc = jans_sql_prop['auth.userPassword']
            Config.rdbm_password = self.unobscure(Config.rdbm_password_enc)
            if Config.rdbm_type == 'postgresql':
                Config.rdbm_type = 'pgsql'

        # It is time to bind database
        dbUtils.bind()

        if dbUtils.session:
            dbUtils.rdm_automapper()

        result = dbUtils.search('ou=clients,o=jans', search_filter='(&(inum=1701.*)(objectClass=jansClnt))', search_scope=SearchScopes.SUBTREE)

        oxConfiguration = dbUtils.search(jans_ConfigurationDN, search_filter='(objectClass=jansAppConf)', search_scope=SearchScopes.BASE)
        if 'jansIpAddress' in oxConfiguration:
            Config.ip = oxConfiguration['jansIpAddress']

        if isinstance(oxConfiguration['jansCacheConf'], str):
            oxCacheConfiguration = json.loads(oxConfiguration['jansCacheConf'])
        else:
            oxCacheConfiguration = oxConfiguration['jansCacheConf']

        Config.cache_provider_type = str(oxCacheConfiguration['cacheProviderType'])

        # Other clients
        client_var_id_list = [
                    ('jans_auth_client_id', '1001.'),
                    ('jca_client_id', '1800.', {'pw': 'jca_client_pw', 'encoded':'jca_client_encoded_pw'}),
                    ('jca_test_client_id', '1802.', {'pw': 'jca_test_client_pw', 'encoded':'jca_test_client_encoded_pw'}),
                    ('scim_client_id', '1201.', {'pw': 'scim_client_pw', 'encoded':'scim_client_encoded_pw'}),
                    ('admin_ui_client_id', '1901.', {'pw': 'admin_ui_client_pw', 'encoded': 'admin_ui_client_encoded_pw'}),
                    ('casa_client_id', CasaInstaller.client_id_prefix),
                    ('saml_scim_client_id', '2100.'),
                    ]
        self.check_clients(client_var_id_list, create=False)

        result = dbUtils.search(
                        search_base='inum={},ou=clients,o=jans'.format(Config.get('jans_auth_client_id', '-1')),
                        search_filter='(objectClass=jansClnt)',
                        search_scope=SearchScopes.BASE,
                        )
        if result and result.get('jansClntSecret'):
            Config.jans_auth_client_encoded_pw = result['jansClntSecret']
            Config.jans_auth_client_pw = self.unobscure(Config.jans_auth_client_encoded_pw)

        dn_jans_auth, jans_auth_conf_dynamic = dbUtils.get_jans_auth_conf_dynamic()

        o_issuer = urlparse(jans_auth_conf_dynamic['issuer'])
        Config.hostname = str(o_issuer.netloc)

        Config.jans_auth_openidScopeBackwardCompatibility =  jans_auth_conf_dynamic.get('openidScopeBackwardCompatibility', False)

        if 'pairwiseCalculationSalt' in jans_auth_conf_dynamic:
            Config.pairwiseCalculationSalt =  jans_auth_conf_dynamic['pairwiseCalculationSalt']
        if 'legacyIdTokenClaims' in jans_auth_conf_dynamic:
            Config.jans_auth_legacyIdTokenClaims = jans_auth_conf_dynamic['legacyIdTokenClaims']
        if 'pairwiseCalculationKey' in jans_auth_conf_dynamic:
            Config.pairwiseCalculationKey = jans_auth_conf_dynamic['pairwiseCalculationKey']
        if 'keyStoreFile' in jans_auth_conf_dynamic:
            Config.jans_auth_openid_jks_fn = jans_auth_conf_dynamic['keyStoreFile']
        if 'keyStoreSecret' in jans_auth_conf_dynamic:
            Config.jans_auth_openid_jks_pass = jans_auth_conf_dynamic['keyStoreSecret']

        httpd_crt_fn = '/etc/certs/httpd.crt'
        crt_fn = httpd_crt_fn if os.path.exists(httpd_crt_fn) else '/etc/certs/ob/server.crt'
        ssl_subj = self.get_ssl_subject(crt_fn)

        Config.countryCode = ssl_subj.get('countryName', '')
        Config.state = ssl_subj.get('stateOrProvinceName', '')
        Config.city = ssl_subj.get('localityName', '')
        Config.admin_email = ssl_subj.get('emailAddress', '')


        if not Config.get('orgName'):
            Config.orgName = ssl_subj.get('organizationName', '')

        for s in ['jansScimEnabled']:
            setattr(Config, s, oxConfiguration.get(s, False))

        application_max_ram = 3072

        default_dir = '/etc/default'
        usedRatio = 0.001
        jans_auth_max_heap_mem = 0

        jetty_services = JettyInstaller.jetty_app_configuration

        for service in jetty_services:
            service_default_fn = os.path.join(default_dir, service)
            if os.path.exists(service_default_fn):
                usedRatio += jetty_services[service]['memory']['ratio']
                if service == 'jans-auth':
                    service_prop = base.read_properties_file(service_default_fn)
                    m = re.search('-Xmx(\d*)m', service_prop['JAVA_OPTIONS'])
                    jans_auth_max_heap_mem = int(m.groups()[0])

        if jans_auth_max_heap_mem:
            ratioMultiplier = 1.0 + (1.0 - usedRatio)/usedRatio
            applicationMemory = jans_auth_max_heap_mem / jetty_services['jans-auth']['memory']['jvm_heap_ration']
            allowedRatio = jetty_services['jans-auth']['memory']['ratio'] * ratioMultiplier
            application_max_ram = int(round(applicationMemory / allowedRatio))

        Config.os_type = base.os_type
        Config.os_version = base.os_version

        if not Config.get('ip'):
            Config.ip = self.detect_ip()

        Config.install_scim_server = os.path.exists(os.path.join(Config.jetty_base, 'jans-scim/start.d'))
        Config.install_fido2 = os.path.exists(os.path.join(Config.jetty_base, 'jans-fido2/start.d'))
        Config.install_config_api = os.path.exists(os.path.join(Config.jansOptFolder, 'jans-config-api'))
        Config.install_link = os.path.exists(os.path.join(Config.jansOptFolder, 'jans-link'))
        Config.install_casa = os.path.exists(os.path.join(Config.jetty_base, 'casa/start.d'))
        Config.install_jans_keycloak_link = os.path.exists(os.path.join(Config.jetty_base, 'jans-keycloak-link/start.d'))

        # jans-idp config
        jans_idp_config_result = dbUtils.dn_exists("ou=jans-idp,ou=configuration,o=jans")
        if jans_idp_config_result:
            jans_idp_config = json.loads(jans_idp_config_result.get('jansConfDyn', '{}'))
            for config_var, json_prop in (
                    ('jans_idp_enabled', 'enabled'),
                    ('jans_idp_realm', 'realm'),
                    ('jans_idp_client_id', 'clientId'),
                    ('jans_idp_client_secret', 'clientSecret'),
                    ('jans_idp_grant_type', 'grantType'),
                    ('jans_idp_user_name', 'username'),
                    ('jans_idp_user_password', 'password'),
                    ('jans_idp_idp_root_dir', 'idpRootDir'),
                    ('jans_idp_idp_metadata_root_dir', 'idpMetadataRootDir'),
                    ('jans_idp_idp_metadata_temp_dir', 'idpMetadataTempDir'),
                    ('jans_idp_idp_metadata_file', 'idpMetadataFile'),
                    ('jans_idp_sp_metadata_root_dir', 'spMetadataRootDir'),
                    ('jans_idp_sp_metadata_temp_dir', 'spMetadataTempDir'),
                    ('jans_idp_ignore_validation', 'ignoreValidation')
                    ):
                if json_prop in jans_idp_config:
                    setattr(Config, config_var, jans_idp_config[json_prop])


    def save(self):
        if os.path.exists(Config.setup_properties_fn):
            self.backupFile(Config.setup_properties_fn)
        last_prop = Config.setup_properties_fn + '.last'
        if os.path.exists(last_prop):
            self.backupFile(last_prop)

        propertiesUtils.save_properties()
