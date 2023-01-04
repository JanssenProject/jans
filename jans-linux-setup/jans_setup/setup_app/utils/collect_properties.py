import os
import json
import zipfile
import re
import sys
import base64
import glob
import ldap3

from urllib.parse import urlparse
from ldap3.utils import dn as dnutils

from setup_app import paths
from setup_app import static
from setup_app.utils import base
from setup_app.config import Config
from setup_app.utils.db_utils import dbUtils
from setup_app.utils.setup_utils import SetupUtils
from setup_app.utils.properties_utils import propertiesUtils
from setup_app.pylib.jproperties import Properties
from setup_app.installers.jetty import JettyInstaller
from setup_app.installers.base import BaseInstaller

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
        oxauth_ConfigurationEntryDN = jans_prop['jansAuth_ConfigurationEntryDN']
        jans_ConfigurationDN = 'ou=configuration,o=jans'

        if Config.persistence_type in ('couchbase', 'sql', 'spanner'):
            ptype = 'rdbm' if Config.persistence_type in ('sql', 'spanner') else 'couchbase'
            Config.mapping_locations = { group: ptype for group in Config.couchbaseBucketDict }
            default_storage = Config.persistence_type


        if not Config.persistence_type in ('ldap', 'sql', 'spanner') and os.path.exists(Config.jansCouchebaseProperties):
            jans_cb_prop = base.read_properties_file(Config.jansCouchebaseProperties)

            Config.couchebaseClusterAdmin = jans_cb_prop['auth.userName']
            Config.encoded_cb_password = jans_cb_prop['auth.userPassword']
            Config.cb_password = self.unobscure(jans_cb_prop['auth.userPassword'])
            Config.couchbase_bucket_prefix = jans_cb_prop['bucket.default']

            Config.couchbase_hostname = jans_cb_prop['servers'].split(',')[0].strip()
            Config.encoded_couchbaseTrustStorePass = jans_cb_prop['ssl.trustStore.pin']
            Config.couchbaseTrustStorePass = self.unobscure(jans_cb_prop['ssl.trustStore.pin'])
            Config.cb_query_node = Config.couchbase_hostname
            Config.couchbase_buckets = [b.strip() for b in jans_cb_prop['buckets'].split(',')]

        if not Config.persistence_type in ('couchbase', 'sql') and os.path.exists(Config.ox_ldap_properties):
            jans_ldap_prop = base.read_properties_file(Config.ox_ldap_properties)
            Config.ldap_binddn = jans_ldap_prop['bindDN']
            Config.ldapPass = self.unobscure(jans_ldap_prop['bindPassword'])
#            Config.opendj_p12_pass = self.unobscure(jans_ldap_prop['ssl.trustStorePin'])
            Config.opendj_truststore_pass = self.unobscure(jans_ldap_prop['ssl.trustStorePin'])
            Config.ldap_hostname, Config.ldaps_port = jans_ldap_prop['servers'].split(',')[0].split(':')

        if not Config.persistence_type in ('couchbase', 'ldap') and os.path.exists(Config.jansRDBMProperties):
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

        if not Config.persistence_type in ('couchbase', 'ldap') and Config.get('jansSpannerProperties') and os.path.exists(Config.jansSpannerProperties):
            Config.rdbm_type = 'spanner'
            jans_spanner_prop = base.read_properties_file(Config.jansSpannerProperties)

            Config.spanner_project = jans_spanner_prop['connection.project']
            Config.spanner_instance = jans_spanner_prop['connection.instance']
            Config.spanner_database = jans_spanner_prop['connection.database']

            if 'connection.emulator-host' in jans_spanner_prop:
                Config.spanner_emulator_host = jans_spanner_prop['connection.emulator-host'].split(':')[0]
                Config.templateRenderingDict['spanner_creds'] = 'connection.emulator-host={}:9010'.format(Config.spanner_emulator_host)

            elif 'auth.credentials-file' in jans_spanner_prop:
                Config.google_application_credentials = jans_spanner_prop['auth.credentials-file']
                Config.templateRenderingDict['spanner_creds'] = 'auth.credentials-file={}'.format(Config.google_application_credentials)


        if Config.persistence_type in ['hybrid']:
             jans_hybrid_properties = base.read_properties_file(jans_hybrid_properties_fn)
             Config.mapping_locations = {'default': jans_hybrid_properties['storage.default']}
             storages = [ storage.strip() for storage in jans_hybrid_properties['storages'].split(',') ]

             for ml, m in (('user', 'people'), ('cache', 'cache'), ('site', 'cache-refresh'), ('token', 'tokens')):
                 for storage in storages:
                     if m in jans_hybrid_properties.get('storage.{}.mapping'.format(storage),[]):
                         Config.mapping_locations[ml] = storage

        if not Config.get('couchbase_bucket_prefix'):
            Config.couchbase_bucket_prefix = 'jans'


        # It is time to bind database
        dbUtils.bind()

        if dbUtils.session:
            dbUtils.rdm_automapper()

        result = dbUtils.search('ou=clients,o=jans', search_filter='(&(inum=1701.*)(objectClass=jansClnt))', search_scope=ldap3.SUBTREE)

        if result:
            Config.jans_radius_client_id = result['inum']
            Config.jans_ro_encoded_pw = result['jansClntSecret']
            Config.jans_ro_pw = self.unobscure(Config.jans_ro_encoded_pw)
    
            result = dbUtils.search('inum=5866-4202,ou=scripts,o=jans', search_scope=ldap3.BASE)
            if result:
                Config.enableRadiusScripts = result['jansEnabled']

            result = dbUtils.search('ou=clients,o=jans', search_filter='(&(inum=1402.*)(objectClass=jansClnt))', search_scope=ldap3.SUBTREE)
            if result:
                Config.oxtrust_requesting_party_client_id = result['inum']

        oxConfiguration = dbUtils.search(jans_ConfigurationDN, search_filter='(objectClass=jansAppConf)', search_scope=ldap3.BASE)
        if 'jansIpAddress' in oxConfiguration:
            Config.ip = oxConfiguration['jansIpAddress']

        if isinstance(oxConfiguration['jansCacheConf'], str):
            oxCacheConfiguration = json.loads(oxConfiguration['jansCacheConf'])
        else:
            oxCacheConfiguration = oxConfiguration['jansCacheConf']

        Config.cache_provider_type = str(oxCacheConfiguration['cacheProviderType'])

        # Other clients
        client_var_id_list = [
                    ('oxauth_client_id', '1001.'),
                    ('jca_client_id', '1800.', {'pw': 'jca_client_pw', 'encoded':'jca_client_encoded_pw'}),
                    ('jca_test_client_id', '1802.', {'pw': 'jca_test_client_pw', 'encoded':'jca_test_client_encoded_pw'}),
                    ('scim_client_id', '1201.', {'pw': 'scim_client_pw', 'encoded':'scim_client_encoded_pw'}),
                    ('admin_ui_client_id', '1901.', {'pw': 'admin_ui_client_pw', 'encoded': 'admin_ui_client_encoded_pw'}),
                    ]
        self.check_clients(client_var_id_list, create=False)

        result = dbUtils.search(
                        search_base='inum={},ou=clients,o=jans'.format(Config.get('oxauth_client_id', '-1')),
                        search_filter='(objectClass=jansClnt)',
                        search_scope=ldap3.BASE,
                        )
        if result and result.get('jansClntSecret'):
            Config.oxauthClient_encoded_pw = result['jansClntSecret']
            Config.oxauthClient_pw = self.unobscure(Config.oxauthClient_encoded_pw)

        dn_oxauth, oxAuthConfDynamic = dbUtils.get_oxAuthConfDynamic()

        o_issuer = urlparse(oxAuthConfDynamic['issuer'])
        Config.hostname = str(o_issuer.netloc)

        Config.oxauth_openidScopeBackwardCompatibility =  oxAuthConfDynamic.get('openidScopeBackwardCompatibility', False)

        if 'pairwiseCalculationSalt' in oxAuthConfDynamic:
            Config.pairwiseCalculationSalt =  oxAuthConfDynamic['pairwiseCalculationSalt']
        if 'legacyIdTokenClaims' in oxAuthConfDynamic:
            Config.oxauth_legacyIdTokenClaims = oxAuthConfDynamic['legacyIdTokenClaims']
        if 'pairwiseCalculationKey' in oxAuthConfDynamic:
            Config.pairwiseCalculationKey = oxAuthConfDynamic['pairwiseCalculationKey']
        if 'keyStoreFile' in oxAuthConfDynamic:
            Config.oxauth_openid_jks_fn = oxAuthConfDynamic['keyStoreFile']
        if 'keyStoreSecret' in oxAuthConfDynamic:
            Config.oxauth_openid_jks_pass = oxAuthConfDynamic['keyStoreSecret']

        httpd_crt_fn = '/etc/certs/httpd.crt'
        crt_fn = httpd_crt_fn if os.path.exists(httpd_crt_fn) else '/etc/certs/ob/server.crt'
        ssl_subj = self.get_ssl_subject(crt_fn)

        Config.countryCode = ssl_subj.get('countryName', '')
        Config.state = ssl_subj.get('stateOrProvinceName', '')
        Config.city = ssl_subj.get('localityName', '')
        Config.admin_email = ssl_subj.get('emailAddress', '')

         #this is not good, but there is no way to retreive password from ldap
        if not Config.get('admin_password'):
            if Config.get('ldapPass'):
                Config.admin_password = Config.ldapPass
            elif Config.get('cb_password'):
                Config.admin_password = Config.cb_password

        if not Config.get('orgName'):
            Config.orgName = ssl_subj.get('organizationName', '')

        for s in ['jansScimEnabled']:
            setattr(Config, s, oxConfiguration.get(s, False))

        application_max_ram = 3072

        default_dir = '/etc/default'
        usedRatio = 0.001
        oxauth_max_heap_mem = 0

        jetty_services = JettyInstaller.jetty_app_configuration

        for service in jetty_services:
            service_default_fn = os.path.join(default_dir, service)
            if os.path.exists(service_default_fn):
                usedRatio += jetty_services[service]['memory']['ratio']
                if service == 'jans-auth':
                    service_prop = base.read_properties_file(service_default_fn)
                    m = re.search('-Xmx(\d*)m', service_prop['JAVA_OPTIONS'])
                    oxauth_max_heap_mem = int(m.groups()[0])

        if oxauth_max_heap_mem:
            ratioMultiplier = 1.0 + (1.0 - usedRatio)/usedRatio
            applicationMemory = oxauth_max_heap_mem / jetty_services['jans-auth']['memory']['jvm_heap_ration']
            allowedRatio = jetty_services['jans-auth']['memory']['ratio'] * ratioMultiplier
            application_max_ram = int(round(applicationMemory / allowedRatio))

        Config.os_type = base.os_type
        Config.os_version = base.os_version

        if not Config.get('ip'):
            Config.ip = self.detect_ip()

        Config.install_scim_server = os.path.exists(os.path.join(Config.jetty_base, 'jans-scim/start.ini'))
        Config.installFido2 = os.path.exists(os.path.join(Config.jetty_base, 'jans-fido2/start.ini'))
        Config.installEleven = os.path.exists(os.path.join(Config.jetty_base, 'jans-eleven/start.ini'))
        Config.install_config_api = os.path.exists(os.path.join(Config.jansOptFolder, 'jans-config-api'))

    def save(self):
        if os.path.exists(Config.setup_properties_fn):
            self.backupFile(Config.setup_properties_fn)
        last_prop = Config.setup_properties_fn + '.last'
        if os.path.exists(last_prop):
            self.backupFile(last_prop)

        propertiesUtils.save_properties()
