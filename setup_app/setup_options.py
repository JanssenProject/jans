import os
import sys
import argparse

from setup_app.static import InstallTypes

def get_setup_options():

    parser_description='''Use setup.py to configure your Jans Server and to add initial data required for
    oxAuth and oxTrust to start. If setup.properties is found in this folder, these
    properties will automatically be used instead of the interactive setup.
    '''

    parser = argparse.ArgumentParser(description=parser_description)
    parser.add_argument('-c', help="Use command line instead of tui", action='store_true')
    parser.add_argument('-d', help="Installation directory")
    parser.add_argument('-f', help="Specify setup.properties file")
    parser.add_argument('-n', help="No interactive prompt before install starts. Run with -f", action='store_true')    
    parser.add_argument('-N', '--no-httpd', help="No apache httpd server", action='store_true')
    parser.add_argument('-u', help="Update hosts file with IP address / hostname", action='store_true')
    parser.add_argument('-w', help="Get the development head war files", action='store_true')
    parser.add_argument('-t', help="Load test data", action='store_true')
    parser.add_argument('-x', help="Load test data and exit", action='store_true')
    parser.add_argument('-csx', help="Collect setup properties, save and exit", action='store_true')
    parser.add_argument('-stm', '--enable-scim-test-mode', help="Enable Scim Test Mode", action='store_true')
    parser.add_argument('--allow-pre-released-features', help="Enable options to install experimental features, not yet officially supported", action='store_true')
    parser.add_argument('--import-ldif', help="Render ldif templates from directory and import them in LDAP")
    parser.add_argument('--listen_all_interfaces', help="Allow the LDAP server to listen on all server interfaces", action='store_true')

    ldap_group = parser.add_mutually_exclusive_group()
    ldap_group.add_argument('--remote-ldap', help="Enables using remote LDAP server", action='store_true')
    #ldap_group.add_argument('--install-local-wrends', help="Installs local WrenDS", action='store_true')

    parser.add_argument('--remote-couchbase', help="Enables using remote couchbase server", action='store_true')
    parser.add_argument('--no-data', help="Do not import any data to database backend, used for clustering", action='store_true')
    parser.add_argument('--no-jsauth', help="Do not install OAuth2 Authorization Server", action='store_true')
    parser.add_argument('-ip-address', help="Used primarily by Apache httpd for the Listen directive")
    parser.add_argument('-host-name', help="Internet-facing FQDN that is used to generate certificates and metadata.")
    parser.add_argument('-org-name', help="Organization name field used for generating X.509 certificates")
    parser.add_argument('-email', help="Email address for support at your organization used for generating X.509 certificates")
    parser.add_argument('-city', help="City field used for generating X.509 certificates")
    parser.add_argument('-state', help="State field used for generating X.509 certificates")
    parser.add_argument('-country', help="Two letters country coude used for generating X.509 certificates")
    parser.add_argument('-ldap-admin-password', help="Used as the LDAP directory manager password")
    parser.add_argument('-admin-password', help="Used as the Administrator password")
    parser.add_argument('-jans-max-mem', help="Total memory (in KB) to be used by Jannses Server")
    parser.add_argument('-properties-password', help="Encoded setup.properties file password")
    #parser.add_argument('--no-config-api', help="Do not install Jans Auth Config Api", action='store_true')
    #parser.add_argument('--install-oxd', help="Install Oxd Server", action='store_true')
    parser.add_argument('--no-scim', help="Do not install Scim Server", action='store_true')
    parser.add_argument('--no-fido2', help="Do not install Fido2 Server", action='store_true')
    parser.add_argument('--install-eleven', help="Install Eleven Server", action='store_true')
    #parser.add_argument('--oxd-use-jans-storage', help="Use Jans Storage for Oxd Server", action='store_true')
    parser.add_argument('-couchbase-bucket-prefix', help="Set prefix for couchbase buckets", default='jans')
    #parser.add_argument('--generate-oxd-certificate', help="Generate certificate for oxd based on hostname", action='store_true')
    parser.add_argument('--shell', help="Drop into interactive shell before starting installation", action='store_true')
    parser.add_argument('-config-patch-creds', help="password:username for downloading auto test ciba password")
    parser.add_argument('--dump-config-on-error', help="Dump configuration on error", action='store_true')
    parser.add_argument('--no-progress', help="Use simple progress", action='store_true')



    argsp = parser.parse_args()

    setupOptions = {
        'setup_properties': None,
        'noPrompt': False,
        'downloadWars': False,
        'installOxAuth': True,
        'installConfigApi': False,
        'installHTTPD': True,
        'installScimServer': True,
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

    if not argsp.remote_couchbase:
        setupOptions['wrends_install'] = InstallTypes.LOCAL
    else:
        setupOptions['wrends_install'] = InstallTypes.NONE
        setupOptions['cb_install'] = InstallTypes.REMOTE

    if argsp.no_jsauth:
        setupOptions['installOxAuth'] = False

    #if argsp.no_config_api:
    #    setupOptions['installConfigApi'] = False

    if argsp.no_scim:
        setupOptions['installScimServer'] = False

    if argsp.no_fido2:
        setupOptions['installFido2'] = False

    if argsp.install_eleven:
        setupOptions['installEleven'] = True

    if argsp.ip_address:
        setupOptions['ip'] = argsp.ip_address

    if argsp.host_name:
        setupOptions['hostname'] = argsp.host_name
        
    if argsp.org_name:
        setupOptions['orgName'] = argsp.org_name

    if argsp.email:
        setupOptions['admin_email'] = argsp.email

    if argsp.city:
        setupOptions['city'] = argsp.city

    if argsp.state:
        setupOptions['state'] = argsp.state

    if argsp.country:
        setupOptions['countryCode'] = argsp.country

    if argsp.jans_max_mem:
        setupOptions['jans_max_mem'] = argsp.jans_max_mem

    if argsp.ldap_admin_password:
        setupOptions['ldapPass'] = argsp.ldap_admin_password

    if argsp.admin_password:
        setupOptions['admin_password'] = argsp.admin_password
    elif argsp.ldap_admin_password:
        setupOptions['admin_password'] = argsp.ldap_admin_password

    if argsp.f:
        if os.path.isfile(argsp.f):
            setupOptions['setup_properties'] = argsp.f
            print("Found setup properties %s\n" % argsp.f)
        else:
            print("\nOoops... %s file not found for setup properties.\n" %argsp.f)

    setupOptions['noPrompt'] = argsp.n

    if argsp.no_httpd:
        setupOptions['installHTTPD'] = False

    if argsp.enable_scim_test_mode:
        setupOptions['scimTestMode'] = 'true'
    
    setupOptions['downloadWars'] = argsp.w
    setupOptions['loadTestData']  = argsp.t
    setupOptions['loadTestDataExit'] = argsp.x
    setupOptions['allowPreReleasedFeatures'] = argsp.allow_pre_released_features
    setupOptions['listenAllInterfaces'] = argsp.listen_all_interfaces
    setupOptions['couchbase_bucket_prefix'] = argsp.couchbase_bucket_prefix
    setupOptions['config_patch_creds'] = argsp.config_patch_creds
    setupOptions['dump_config_on_error'] = argsp.dump_config_on_error

    if argsp.remote_ldap:
        setupOptions['wrends_install'] = InstallTypes.REMOTE

    if argsp.no_data:
        setupOptions['loadData'] = False

    if argsp.remote_ldap:
        setupOptions['listenAllInterfaces'] = True

    #if argsp.oxd_use_jans_storage:
    #    setupOptions['oxd_use_jans_storage'] = True

    if argsp.import_ldif:
        if os.path.isdir(argsp.import_ldif):
            setupOptions['importLDIFDir'] = argsp.import_ldif
            print("Found setup LDIF import directory {}\n".format(argsp.import_ldif))
        else:
            print("The custom LDIF import directory {} does not exist. Exiting...".format(argsp.import_ldif))
            sys.exit(2)

    setupOptions['properties_password'] = argsp.properties_password

    return argsp, setupOptions
