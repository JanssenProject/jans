import os
import sys
import uuid
import argparse

from setup_app import static, paths
from setup_app.version import __version__
from setup_app.utils import base
from setup_app.config import Config

OPENBANKING_PROFILE = 'openbanking'
PROFILE = os.environ.get('JANS_PROFILE')

parser_description='''Use this script to configure your Jans Server and to add initial data required for
Jans Auth and other Jans services to start. If setup.properties is found in this folder, these
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
parser.add_argument('-encode-salt', help="24 characters length string to be used for encoding passwords")

rdbm_group = parser.add_mutually_exclusive_group()
rdbm_group.add_argument('-remote-rdbm', choices=['mysql', 'pgsql'], help="Enables using remote RDBM server")
rdbm_group.add_argument('-local-rdbm', choices=['mysql', 'pgsql'], help="Enables installing/configuring local RDBM server", default='pgsql')
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
parser.add_argument('-disable-script', action='append', help="inum of script to disable", required=False)

parser.add_argument('-java-version', help="Version of Amazon Corretto", choices=['11', '17'], required=False)


if PROFILE != OPENBANKING_PROFILE:

    parser.add_argument('-stm', '--enable-scim-test-mode', help="Enable Scim Test Mode", action='store_true')
    parser.add_argument('-w', help="Get the development head war files", action='store_true')
    parser.add_argument('-t', help="Load test data", action='store_true')
    parser.add_argument('-x', help="Load test data and exit", action='store_true')
    parser.add_argument('--allow-pre-released-features', help="Enable options to install experimental features, not yet officially supported", action='store_true')

    parser.add_argument('--no-data', help="Do not import any data to database backend, used for clustering", action='store_true')
    parser.add_argument('--no-jsauth', help="Do not install OAuth2 Authorization Server", action='store_true')

    parser.add_argument('--no-config-api', help="Do not install Jans Auth Config Api", action='store_true')

    parser.add_argument('--no-scim', help="Do not install Scim Server", action='store_true')
    parser.add_argument('--no-fido2', help="Do not install Fido2 Server", action='store_true')
    parser.add_argument('--install-link', help="Install Link Server", action='store_true')
    parser.add_argument('--install-jans-keycloak-link', help="Install Keycloak Link Server", action='store_true')

    parser.add_argument('--with-casa', help="Install Jans Casa", action='store_true')
    parser.add_argument('--install-jans-saml', help="Install Jans KC", action='store_true')
    parser.add_argument('--install-jans-lock', help="Install Jans Lock", action='store_true')
    parser.add_argument('--install-opa', help="Install OPA", action='store_true')

    parser.add_argument('--load-config-api-test', help="Load Config Api Test Data", action='store_true')

    parser.add_argument('-config-patch-creds', help="password:username for downloading auto test ciba password")

    # test-client
    parser.add_argument('-test-client-id', help="ID of test client which has all available scopes. Must be in UUID format.")
    parser.add_argument('-test-client-pw', help="Secret for test client")
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
    argsp, others = parser.parse_known_args()
    if getattr(argsp, 'test_client_id', None):
        try:
            uuid.UUID(argsp.test_client_id)
        except:
            sys.stderr.write("{}-test-client-id should be in UUID format{}\n".format(static.colors.DANGER, static.colors.ENDC))
            sys.stderr.flush()
            sys.exit(2)

    return argsp
