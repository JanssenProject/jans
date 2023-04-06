#!/usr/bin/python3

import warnings
warnings.filterwarnings("ignore")

import pydevd
import debugpy

import os
import sys
import json
import argparse

from setup_app import paths

from setup_app.utils import base

base.current_app.profile = 'disa-stig'

from setup_app.config import Config
from setup_app.utils.setup_utils import SetupUtils
from setup_app.utils.properties_utils import PropertiesUtils
from setup_app.utils.ldif_utils import myLdifParser
from setup_app.utils.db_utils import dbUtils

debugpy.listen(("0.0.0.0",5678));
debugpy.wait_for_client();

parser = argparse.ArgumentParser(description="Jans LDAP to RDBM migrator script")
parser.add_argument('-remote-rdbm', choices=['pgsql'], help="Enables using remote RDBM server", default='pgsql')
parser.add_argument('-rdbm-user', help="RDBM username",  required = True)
parser.add_argument('-rdbm-password', help="RDBM password",  required = True)
parser.add_argument('-rdbm-port', help="RDBM port", type=int)
parser.add_argument('-rdbm-db', help="RDBM database",  required = True)
parser.add_argument('-rdbm-host', help="RDBM host",  required = True)
parser.add_argument('-in-ldif-fpath', help="Input ldif file path",  required = True)
parser.add_argument('-j', help="Use Java existing on system", default=True, action='store_true', required = False )
parser.add_argument('-opendj-keystore-type', help="OpenDJ keystore type (Ony for 'disa-stig' profile)", choices=['pkcs11', 'bcfks'], default='bcfks', required = False)

argsp = parser.parse_args()
rdbm_config_params = ('rdbm_user', 'rdbm_password', 'rdbm_host', 'rdbm_db', 'rdbm_host', 'rdbm_port', 'in_ldif_fpath')
argsp_dict = { a: getattr(argsp, a) for a in rdbm_config_params }
argsp_dict['rdbm_type'] = getattr(argsp, 'remote_rdbm')

def main():

#    debugpy.breakpoint();

    base.argsp = argsp

    Config.init(paths.INSTALL_DIR)

    print("Config.mapping_locations['default'] = {}".format(Config.mapping_locations['default']))
    Config.mapping_locations['default'] = 'rdbm'
    print("Config.mapping_locations['default'] = {}".format(Config.mapping_locations['default']))

    for x in Config.mapping_locations.keys():
        Config.mapping_locations[x] = 'rdbm'

    Config.installed_instance = True

    print('Config.mapping_locations = {}'.format(Config.mapping_locations))

    Config.rdbm_type = argsp_dict['rdbm_type']
    Config.rdbm_host = argsp_dict['rdbm_host']
    Config.rdbm_port = argsp_dict['rdbm_port']
    Config.rdbm_db = argsp_dict['rdbm_db']
    Config.rdbm_user = argsp_dict['rdbm_user']
    Config.rdbm_password = argsp_dict['rdbm_password']

    print('Config.rdbm_type = {}'.format(Config.rdbm_type))
    print('Config.rdbm_host = {}'.format(Config.rdbm_host))
    print('Config.rdbm_port = {}'.format(Config.rdbm_port))
    print('Config.rdbm_db   = {}'.format(Config.rdbm_db))
    print('Config.rdbm_user = {}'.format(Config.rdbm_user))
    print('Config.rdbm_password = {}'.format(Config.rdbm_password))

    parser = myLdifParser(argsp_dict['in_ldif_fpath'])
    parser.parse()

    print('parser.ldif_file = {}'.format(parser.ldif_file))
    print('parser.entries = {}'.format(parser.entries))

    has_attr = hasattr(dbUtils, 'ldap_conn')

    print('has_attr (ldap_conn) = {}'.format(has_attr))

    print('Config.rdbm_type = {}'.format(Config.rdbm_type))

    debugpy.breakpoint();

    print('dbUtils.sqlconnection() ------------------------ >>')
    result, conn = dbUtils.sqlconnection(log=False)
    print('result = {}'.format(result))
    print('conn = {}'.format(conn))
    print('dbUtils.sqlconnection() ------------------------ <<')

    debugpy.breakpoint();

    print('dbUtils.bind() ------------------------ >>')
    dbUtils.bind()
    print('dbUtils.bind() ------------------------ <<')

    print('argsp_dict[in_ldif_fpath] = {}'.format(argsp_dict['in_ldif_fpath']))

    debugpy.breakpoint();

    dbUtils.import_ldif([argsp_dict['in_ldif_fpath']])
    
    print('return')
    return

if __name__ == "__main__":
    debugpy.breakpoint();
    base.logIt('jans_setup: main()')
    main()
