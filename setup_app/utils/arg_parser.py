import argparse

def arg_parser():
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
    ldap_group.add_argument('--disable-local-ldap', help="Disables installing local LDAP server", action='store_true')
    #ldap_group.add_argument('--install-local-wrends', help="Installs local WrenDS", action='store_true')

    rdbm_group = parser.add_mutually_exclusive_group()
    rdbm_group.add_argument('-remote-rdbm', choices=['mysql', 'pgsql', 'spanner'], help="Enables using remote RDBM server")
    rdbm_group.add_argument('-local-rdbm', choices=['mysql', 'pgsql'], help="Enables installing/configuring local RDBM server")

    parser.add_argument('-rdbm-user', help="RDBM username")
    parser.add_argument('-rdbm-password', help="RDBM password")
    parser.add_argument('-rdbm-port', help="RDBM port")
    parser.add_argument('-rdbm-db', help="RDBM database")
    parser.add_argument('-rdbm-host', help="RDBM host")

    parser.add_argument('--remote-couchbase', help="Enables using remote couchbase server", action='store_true')
    parser.add_argument('--local-couchbase', help="Enables installing couchbase server", action='store_true')
    parser.add_argument('-couchbase-admin-user', help="Couchbase admin user")
    parser.add_argument('-couchbase-admin-password', help="Couchbase admin user password")
    parser.add_argument('-couchbase-bucket-prefix', help="Set prefix for couchbase buckets", default='jans')

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
    parser.add_argument('--no-config-api', help="Do not install Jans Auth Config Api", action='store_true')
    parser.add_argument('--disable-config-api-security', help="Turn off oauth2 security validation for jans-config-api", action='store_true')

    #parser.add_argument('--install-oxd', help="Install Oxd Server", action='store_true')
    parser.add_argument('--no-scim', help="Do not install Scim Server", action='store_true')
    parser.add_argument('--no-fido2', help="Do not install Fido2 Server", action='store_true')
    parser.add_argument('--install-eleven', help="Install Eleven Server", action='store_true')
    #parser.add_argument('--oxd-use-jans-storage', help="Use Jans Storage for Oxd Server", action='store_true')
    parser.add_argument('--load-config-api-test', help="Load Config Api Test Data", action='store_true')

    #parser.add_argument('--generate-oxd-certificate', help="Generate certificate for oxd based on hostname", action='store_true')
    parser.add_argument('--shell', help="Drop into interactive shell before starting installation", action='store_true')
    parser.add_argument('-config-patch-creds', help="password:username for downloading auto test ciba password")
    parser.add_argument('--dump-config-on-error', help="Dump configuration on error", action='store_true')
    parser.add_argument('--no-progress', help="Use simple progress", action='store_true')

    # spanner options
    parser.add_argument('-spanner-project', help="Spanner project name")
    parser.add_argument('-spanner-instance', help="Spanner instance name")
    parser.add_argument('-spanner-database', help="Spanner database name")
    spanner_cred_group = parser.add_mutually_exclusive_group()
    spanner_cred_group.add_argument('-spanner-emulator-host', help="Use Spanner emulator host")
    spanner_cred_group.add_argument('-google-application-credentials', help="Path to Google application credentials json file")

    parser.add_argument('-approved-issuer', help="Api Approved Issuer")

    parser.add_argument('--install-admin-ui', help="Install Gluu Admin UI", action='store_true')

    argsp = parser.parse_args()

    return argsp
