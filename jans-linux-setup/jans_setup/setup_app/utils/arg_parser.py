import os
import argparse

from setup_app.version import __version__
from setup_app.utils import base

OPENBANKING_PROFILE = 'openbanking'
PROFILE = os.environ.get('JANS_PROFILE')

parser_description='''Use this script to configure your Jans Server and to add initial data required for
oxAuth and oxTrust to start. If setup.properties is found in this folder, these
properties will automatically be used instead of the interactive setup.
'''

parser = argparse.ArgumentParser(description=parser_description)
parser.add_argument('--version', action='version', version='%(prog)s ' + __version__)
parser.add_argument('-c', help="Use command line instead of tui", action='store_true')
parser.add_argument('-d', help="Installation directory")
parser.add_argument('-f', help="Specify setup.properties file")
parser.add_argument('-n', help="No interactive prompt before install starts. Run with -f", action='store_true')    
parser.add_argument('-N', '--no-httpd', help="No apache httpd server", action='store_true')
parser.add_argument('-u', help="Update hosts file with IP address / hostname", action='store_true')
parser.add_argument('-csx', help="Collect setup properties, save and exit", action='store_true')
rdbm_group = parser.add_mutually_exclusive_group()
rdbm_group.add_argument('-remote-rdbm', choices=['mysql', 'pgsql', 'spanner'], help="Enables using remote RDBM server")
rdbm_group.add_argument('-local-rdbm', choices=['mysql', 'pgsql'], help="Enables installing/configuring local RDBM server")
parser.add_argument('-ip-address', help="Used primarily by Apache httpd for the Listen directive")
parser.add_argument('-host-name', help="Internet-facing FQDN that is used to generate certificates and metadata.")
parser.add_argument('-org-name', help="Organization name field used for generating X.509 certificates")
parser.add_argument('-email', help="Email address for support at your organization used for generating X.509 certificates")
parser.add_argument('-city', help="City field used for generating X.509 certificates")
parser.add_argument('-state', help="State field used for generating X.509 certificates")
parser.add_argument('-country', help="Two letters country coude used for generating X.509 certificates")

parser.add_argument('-rdbm-user', help="RDBM username")
parser.add_argument('-rdbm-password', help="RDBM password")
parser.add_argument('-rdbm-port', help="RDBM port")
parser.add_argument('-rdbm-db', help="RDBM database")
parser.add_argument('-rdbm-host', help="RDBM host")
parser.add_argument('--reset-rdbm-db', help="Deletes all tables on target database. Warning! You will lose all data on target database.", action='store_true')

parser.add_argument('--shell', help="Drop into interactive shell before starting installation", action='store_true')
parser.add_argument('--dump-config-on-error', help="Dump configuration on error", action='store_true')
parser.add_argument('--no-progress', help="Use simple progress", action='store_true')
parser.add_argument('-admin-password', help="Used as the Administrator password")
parser.add_argument('-jans-max-mem', help="Total memory (in KB) to be used by Jannses Server")
parser.add_argument('-properties-password', help="Encoded setup.properties file password")
parser.add_argument('-approved-issuer', help="Api Approved Issuer")

parser.add_argument('--force-download', help="Force downloading files", action='store_true')
parser.add_argument('--download-exit', help="Download files and exits", action='store_true')
parser.add_argument('-jans-app-version', help="Version for Jannses applications")
parser.add_argument('-jans-build', help="Buid version for Janssen applications")
parser.add_argument('-setup-branch', help="Jannsen setup github branch", default='main')

parser.add_argument('--disable-config-api-security', help="Turn off oauth2 security validation for jans-config-api", action='store_true')
parser.add_argument('--cli-test-client', help="Use config api test client for CLI", action='store_true')
parser.add_argument('--import-ldif', help="Render ldif templates from directory and import them in Database")

parser.add_argument('-enable-script', action='append', help="inum of script to enable", required=False)
parser.add_argument('-disable-script', action='append', help="inum of script to enable", required=False)

if PROFILE != OPENBANKING_PROFILE:

    parser.add_argument('-stm', '--enable-scim-test-mode', help="Enable Scim Test Mode", action='store_true')
    parser.add_argument('-w', help="Get the development head war files", action='store_true')
    parser.add_argument('-t', help="Load test data", action='store_true')
    parser.add_argument('-x', help="Load test data and exit", action='store_true')
    parser.add_argument('--allow-pre-released-features', help="Enable options to install experimental features, not yet officially supported", action='store_true')
    parser.add_argument('--listen_all_interfaces', help="Allow the LDAP server to listen on all server interfaces", action='store_true')

    ldap_group = parser.add_mutually_exclusive_group()
    ldap_group.add_argument('--remote-ldap', help="Enables using remote LDAP server", action='store_true')
    ldap_group.add_argument('--disable-local-ldap', help="Disables installing local LDAP server", action='store_true')

    parser.add_argument('--remote-couchbase', help="Enables using remote couchbase server", action='store_true')
    parser.add_argument('--local-couchbase', help="Enables installing couchbase server", action='store_true')
    parser.add_argument('-couchbase-admin-user', help="Couchbase admin user")
    parser.add_argument('-couchbase-admin-password', help="Couchbase admin user password")
    parser.add_argument('-couchbase-bucket-prefix', help="Set prefix for couchbase buckets", default='jans')
    parser.add_argument('-couchbase-hostname', help="Remote couchbase server hostname")

    parser.add_argument('--no-data', help="Do not import any data to database backend, used for clustering", action='store_true')
    parser.add_argument('--no-jsauth', help="Do not install OAuth2 Authorization Server", action='store_true')
    parser.add_argument('-ldap-admin-password', help="Used as the LDAP directory manager password")
    parser.add_argument('--no-config-api', help="Do not install Jans Auth Config Api", action='store_true')

    parser.add_argument('--no-scim', help="Do not install Scim Server", action='store_true')
    parser.add_argument('--no-fido2', help="Do not install Fido2 Server", action='store_true')
    parser.add_argument('--install-eleven', help="Install Eleven Server", action='store_true')
    #parser.add_argument('--oxd-use-jans-storage', help="Use Jans Storage for Oxd Server", action='store_true')
    parser.add_argument('--load-config-api-test', help="Load Config Api Test Data", action='store_true')

    parser.add_argument('-config-patch-creds', help="password:username for downloading auto test ciba password")

    # spanner options
    parser.add_argument('-spanner-project', help="Spanner project name")
    parser.add_argument('-spanner-instance', help="Spanner instance name")
    parser.add_argument('-spanner-database', help="Spanner database name")
    spanner_cred_group = parser.add_mutually_exclusive_group()
    spanner_cred_group.add_argument('-spanner-emulator-host', help="Use Spanner emulator host")
    spanner_cred_group.add_argument('-google-application-credentials', help="Path to Google application credentials json file")

    # test-client
    parser.add_argument('-test-client-id', help="ID of test client which has all available scopes")
    parser.add_argument('-test-client-secret', help="Secret for test client")
    parser.add_argument('-test-client-redirect-uri', help="Redirect URI for test client")
    parser.add_argument('--test-client-trusted', help="Make test client trusted", action='store_true')

else:
    # openbanking
    parser.add_argument('--no-external-key', help="Don't use external key", action='store_true')
    parser.add_argument('-ob-key-fn', help="Openbanking key filename", default='/root/obsigning-axV5umCvTMBMjPwjFQgEvb_NO_UPLOAD.key')
    parser.add_argument('-ob-cert-fn', help="Openbanking certificate filename", default='/root/obsigning.pem')
    parser.add_argument('-ob-alias', help="Openbanking key alias", default='GkwIzWy88xWSlcWnLiEc8ip9s2M')
    parser.add_argument('-static-kid', help="Openbanking static kid")
    parser.add_argument('-jwks-uri', help="Openbanking jwksUri", default="https://keystore.openbankingtest.org.uk/0014H00001lFE7dQAG/axV5umCvTMBMjPwjFQgEvb.jwks")
    parser.add_argument('--disable-ob-auth-script', help="Disable Openbanking authentication script and use default backend", action='store_true')


def add_to_me(you):

    base.logIt("Adding actions from parser: '{}'".format(you.description))
    group = parser.add_argument_group(you.description)

    for action in you._actions:
        base.logIt("Adding action '{}'".format(' '.join(action.option_strings)))
        if isinstance(action, argparse._HelpAction):
            continue
        if isinstance(action, argparse._StoreTrueAction):
            arg = group.add_argument(*action.option_strings, action='store_true')
        else:
            arg = group.add_argument(*action.option_strings)

        arg.option_strings = action.option_strings
        arg.default = action.default
        arg.help = action.help
        arg.choices = action.choices
        arg.required = False
        arg.type = action.type


def get_parser():
    argsp = parser.parse_known_args()
    return argsp[0]
