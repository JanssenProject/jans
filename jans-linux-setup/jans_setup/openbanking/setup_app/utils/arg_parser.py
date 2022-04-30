import argparse

from setup_app.version import __version__
from setup_app.utils import base

parser_description='''Use setup.py to configure your Jans Server and to add initial data required for
Jans Auth and Jans Config Api to start. If setup.properties is found in this folder, these
properties will automatically be used instead of the interactive setup.
'''

parser = argparse.ArgumentParser(description=parser_description)
parser.add_argument('--version', action='version', version='%(prog)s ' + __version__)
parser.add_argument('-c', help="Use command line instead of tui", action='store_true')
parser.add_argument('-d', help="Installation directory")
parser.add_argument('-n', help="No interactive prompt before install starts", action='store_true')
parser.add_argument('-N', '--no-httpd', help="No apache httpd server", action='store_true')
parser.add_argument('-csx', help="Collect setup properties, save and exit", action='store_true')

rdbm_group = parser.add_mutually_exclusive_group()
rdbm_group.add_argument('-remote-rdbm', choices=['mysql', 'pgsql'], help="Enables using remote RDBM server")
rdbm_group.add_argument('-local-rdbm', choices=['mysql', 'pgsql'], help="Enables installing/configuring local RDBM server")

parser.add_argument('-rdbm-user', help="RDBM username")
parser.add_argument('-rdbm-password', help="RDBM password")
parser.add_argument('-rdbm-port', help="RDBM port")
parser.add_argument('-rdbm-db', help="RDBM database")
parser.add_argument('-rdbm-host', help="RDBM host")

parser.add_argument('-ip-address', help="Used primarily by Apache httpd for the Listen directive")
parser.add_argument('-host-name', help="Internet-facing FQDN that is used to generate certificates and metadata.")
parser.add_argument('-org-name', help="Organization name field used for generating X.509 certificates")
parser.add_argument('-email', help="Email address for support at your organization used for generating X.509 certificates")
parser.add_argument('-city', help="City field used for generating X.509 certificates")
parser.add_argument('-state', help="State field used for generating X.509 certificates")
parser.add_argument('-country', help="Two letters country coude used for generating X.509 certificates")
parser.add_argument('-jans-max-mem', help="Total memory (in KB) to be used by Jannses Server")
parser.add_argument('--disable-config-api-security', help="Turn off oauth2 security validation for jans-config-api", action='store_true')
parser.add_argument('--import-ldif', help="Render ldif templates from directory and import them in Database")

parser.add_argument('--shell', help="Drop into interactive shell before starting installation", action='store_true')
parser.add_argument('--no-progress', help="Use simple progress", action='store_true')

parser.add_argument('-approved-issuer', help="Api Approved Issuer")

# openbanking
parser.add_argument('--no-external-key', help="Don't use external key", action='store_true')
parser.add_argument('-ob-key-fn', help="Openbanking key filename")
parser.add_argument('-ob-cert-fn', help="Openbanking certificate filename")
parser.add_argument('-ob-alias', help="Openbanking key alias")
parser.add_argument('-static-kid', help="Openbanking static kid")
parser.add_argument('-jwks-uri', help="Openbanking jwksUri", default="https://keystore.openbankingtest.org.uk/0014H00001lFE7dQAG/axV5umCvTMBMjPwjFQgEvb.jwks")

parser.add_argument('--force-download', help="Force downloading files", action='store_true')
parser.add_argument('--download-exit', help="Download files and exits", action='store_true')
parser.add_argument('-jans-app-version', help="Version for Jannses applications")
parser.add_argument('-jans-build', help="Buid version for Janssen applications")
parser.add_argument('-setup-branch', help="Jannsen setup github branch", default='main')


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
