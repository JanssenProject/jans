import argparse

def arg_parser():
    parser_description='''Use setup.py to configure your Jans Server and to add initial data required for
    Jans Auth and Jans Config Api to start. If setup.properties is found in this folder, these
    properties will automatically be used instead of the interactive setup.
    '''

    parser = argparse.ArgumentParser(description=parser_description)
    parser.add_argument('-c', help="Use command line instead of tui", action='store_true')
    parser.add_argument('-d', help="Installation directory")
    parser.add_argument('-n', help="No interactive prompt before install starts", action='store_true')
    parser.add_argument('-N', '--no-httpd', help="No apache httpd server", action='store_true')

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

    parser.add_argument('--shell', help="Drop into interactive shell before starting installation", action='store_true')
    parser.add_argument('--no-progress', help="Use simple progress", action='store_true')

    parser.add_argument('-approved-issuer', help="Api Approved Issuer")

    # openbanking
    parser.add_argument('--no-external-key', help="Don't use external key", action='store_true')
    parser.add_argument('-ob-key-fn', help="Openbanking key filename")
    parser.add_argument('-ob-cert-fn', help="Openbanking certificate filename")
    parser.add_argument('-ob-alias', help="Openbanking key alias")
    parser.add_argument('-static-kid', help="Openbanking static kid")
    
    
    argsp = parser.parse_args()

    return argsp
