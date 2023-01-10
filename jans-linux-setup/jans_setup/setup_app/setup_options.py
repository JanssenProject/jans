import os
import sys
import argparse

from setup_app.static import InstallTypes, SetupProfiles
from setup_app.utils import base

def get_setup_options():

    setupOptions = {
        'setup_properties': None,
        'noPrompt': False,
        'downloadWars': False,
        'installOxAuth': True,
        'install_config_api': True,
        'installHTTPD': True,
        'install_scim_server': True if base.current_app.profile == 'jans' else False,
        'installOxd': False,
        'installFido2': True,
        'installEleven': False,
        'loadTestData': False,
        'allowPreReleasedFeatures': False,
        'listenAllInterfaces': False,
        'loadTestDataExit': False,
        'loadData': True,
        'properties_password': None,
    }

    if not (getattr(base.argsp, 'remote_couchbase', None) or base.argsp.remote_rdbm or base.argsp.local_rdbm):
        setupOptions['opendj_install'] = InstallTypes.LOCAL
    else:
        setupOptions['opendj_install'] = InstallTypes.NONE

        if getattr(base.argsp, 'remote_couchbase', None):
            setupOptions['cb_install'] = InstallTypes.REMOTE

        if base.argsp.remote_rdbm:
            setupOptions['rdbm_install'] = True
            setupOptions['rdbm_install_type'] = InstallTypes.REMOTE
            setupOptions['rdbm_type'] = base.argsp.remote_rdbm
            if not base.argsp.remote_rdbm == 'spanner':
                setupOptions['rdbm_host'] = base.argsp.rdbm_host

        if base.argsp.local_rdbm:
            setupOptions['rdbm_install'] = True
            setupOptions['rdbm_install_type'] = InstallTypes.LOCAL
            setupOptions['rdbm_type'] = base.argsp.local_rdbm
            setupOptions['rdbm_host'] = 'localhost'

        if base.argsp.rdbm_port:
            setupOptions['rdbm_port'] = base.argsp.rdbm_port
        else:
            if setupOptions.get('rdbm_type') == 'pgsql':
                setupOptions['rdbm_port'] = 5432

        if base.argsp.rdbm_db:
            setupOptions['rdbm_db'] = base.argsp.rdbm_db
        if base.argsp.rdbm_user:
            setupOptions['rdbm_user'] = base.argsp.rdbm_user
        if base.argsp.rdbm_password:
            setupOptions['rdbm_password'] = base.argsp.rdbm_password

        if base.current_app.profile == 'jans':
            if base.argsp.spanner_project:
                setupOptions['spanner_project'] = base.argsp.spanner_project
            if base.argsp.spanner_instance:
                setupOptions['spanner_instance'] = base.argsp.spanner_instance
            if base.argsp.spanner_database:
                setupOptions['spanner_database'] = base.argsp.spanner_database
            if base.argsp.spanner_emulator_host:
                setupOptions['spanner_emulator_host'] = base.argsp.spanner_emulator_host
            if base.argsp.google_application_credentials:
                setupOptions['google_application_credentials'] = base.argsp.google_application_credentials


    if base.current_app.profile == 'jans':
        if base.argsp.disable_local_ldap:
            setupOptions['opendj_install'] = InstallTypes.NONE

        if base.argsp.local_couchbase:
            setupOptions['cb_install'] = InstallTypes.LOCAL

        setupOptions['couchbase_bucket_prefix'] = base.argsp.couchbase_bucket_prefix
        setupOptions['cb_password'] = base.argsp.couchbase_admin_password
        setupOptions['couchebaseClusterAdmin'] = base.argsp.couchbase_admin_user
        if base.argsp.couchbase_hostname:
            setupOptions['couchbase_hostname'] = base.argsp.couchbase_hostname

        if base.argsp.no_jsauth:
            setupOptions['installOxAuth'] = False

        if base.argsp.no_config_api:
            setupOptions['install_config_api'] = False

        if base.argsp.no_scim:
            setupOptions['install_scim_server'] = False

        if base.argsp.no_fido2:
            setupOptions['installFido2'] = False

        if base.argsp.install_eleven:
            setupOptions['installEleven'] = True

        if base.argsp.jans_max_mem:
            setupOptions['jans_max_mem'] = base.argsp.jans_max_mem

        if base.argsp.ldap_admin_password:
            setupOptions['ldapPass'] = base.argsp.ldap_admin_password

        if base.argsp.admin_password:
            setupOptions['admin_password'] = base.argsp.admin_password
        elif base.argsp.ldap_admin_password:
            setupOptions['admin_password'] = base.argsp.ldap_admin_password

        if base.argsp.f:
            if os.path.isfile(base.argsp.f):
                setupOptions['setup_properties'] = base.argsp.f
                print("Found setup properties %s\n" % base.argsp.f)
            else:
                print("\nOoops... %s file not found for setup properties.\n" %base.argsp.f)

        setupOptions['downloadWars'] = base.argsp.w
        setupOptions['loadTestData']  = base.argsp.t
        setupOptions['loadTestDataExit'] = base.argsp.x
        setupOptions['allowPreReleasedFeatures'] = base.argsp.allow_pre_released_features
        setupOptions['listenAllInterfaces'] = base.argsp.listen_all_interfaces
        setupOptions['config_patch_creds'] = base.argsp.config_patch_creds
        setupOptions['dump_config_on_error'] = base.argsp.dump_config_on_error

        if base.argsp.remote_ldap:
            setupOptions['opendj_install'] = InstallTypes.REMOTE

        if base.argsp.no_data:
            setupOptions['loadData'] = False

        if base.argsp.remote_ldap:
            setupOptions['listenAllInterfaces'] = True

        if base.argsp.import_ldif:
            if os.path.isdir(base.argsp.import_ldif):
                setupOptions['importLDIFDir'] = base.argsp.import_ldif
                print("Found setup LDIF import directory {}\n".format(base.argsp.import_ldif))
            else:
                print("The custom LDIF import directory {} does not exist. Exiting...".format(base.argsp.import_ldif))
                sys.exit(2)

        setupOptions['properties_password'] = base.argsp.properties_password

    if base.current_app.profile == SetupProfiles.OPENBANKING:
        setupOptions['opendj_install'] = InstallTypes.NONE
        setupOptions['static_kid'] = base.argsp.static_kid
        setupOptions['ob_key_fn'] = base.argsp.ob_key_fn
        setupOptions['ob_cert_fn'] = base.argsp.ob_cert_fn
        setupOptions['ob_alias'] = base.argsp.ob_alias
        setupOptions['jwks_uri'] = base.argsp.jwks_uri

        if base.argsp.no_external_key:
            setupOptions['use_external_key'] = False

    if base.argsp.ip_address:
        setupOptions['ip'] = base.argsp.ip_address

    if base.argsp.host_name:
        setupOptions['hostname'] = base.argsp.host_name
        
    if base.argsp.org_name:
        setupOptions['orgName'] = base.argsp.org_name

    if base.argsp.email:
        setupOptions['admin_email'] = base.argsp.email

    if base.argsp.city:
        setupOptions['city'] = base.argsp.city

    if base.argsp.state:
        setupOptions['state'] = base.argsp.state

    if base.argsp.country:
        setupOptions['countryCode'] = base.argsp.country

    setupOptions['noPrompt'] = base.argsp.n

    if base.argsp.no_httpd:
        setupOptions['installHTTPD'] = False

    if base.argsp.jans_auth_api_prefix:
        setupOptions['jans_auth_api_prefix'] = base.argsp.jans_auth_api_prefix
        if not setupOptions['jans_auth_api_prefix'].startswith('/'):
            setupOptions['jans_auth_api_prefix'] = '/' + setupOptions['jans_auth_api_prefix']
        if not setupOptions['jans_auth_api_prefix'].endswith('/'):
            setupOptions['jans_auth_api_prefix'] = setupOptions['jans_auth_api_prefix']+'/'

    return setupOptions
