#!/usr/bin/python3
import os
import sys
import re
import argparse
from pathlib import Path

parser = argparse.ArgumentParser('This script loads ldif file to external db')
parser.add_argument('-salt', help="Salt to encode decode data", required=True)
parser.add_argument('-db-type', help="External database type", choices=['mysql'], required=True)
parser.add_argument('-rdbm-user', help="RDBM username", required=True)
parser.add_argument('-rdbm-password', help="RDBM password", required=True)
parser.add_argument('-rdbm-port', help="RDBM port", default=3306)
parser.add_argument('-rdbm-db', help="RDBM database", required=True)
parser.add_argument('-rdbm-host', help="RDBM host", required=True)
parser.add_argument('-template', help="Template to be loaded", required=True, action='append')

argsp, remaining= parser.parse_known_args()

other_argsp = {}

for arg in remaining:
    args = arg.lstrip('-')
    n = args.find('=')
    if n > 0:
        akey = args[:n].strip()
        aval = args[n+1:].strip()
        for qchar in ('"', "'"):
            if aval.startswith(qchar) and aval.endswith(qchar):
                aval = aval.strip(qchar)
    else:
        akey = args
        aval = True

    other_argsp[akey] = aval

def get_template_identifiers(tmp):
    return re.findall(r'\%\((.*?)\)s', tmp, re.MULTILINE)

identifiers = []
for tmp in argsp.template:
    if os.path.isfile(tmp):
        with open(tmp) as f:
            tmp_text = f.read()
        identifiers += get_template_identifiers(tmp_text)
    else:
        print("File {} not found.".format(tmp))
        sys.exit()

missing_idetifiers = []
for idf in identifiers:
    if idf not in other_argsp and idf not in missing_idetifiers:
        missing_idetifiers.append(idf)

if missing_idetifiers:
    print("Some identifiers appeared in template(s) is/are missing. Please provide the following identifiers as rgument with double dash.")
    print("For example --{}=somevalue".format(missing_idetifiers[0]))
    print("Missing identifier(s):", ', '.join(missing_idetifiers))
    sys.exit()

cur_path = Path(os.path.dirname(__file__))
setup_dir = cur_path.parent.joinpath('jans_setup').as_posix()
sys.path.append(setup_dir)

from setup_app import static
from setup_app.utils import base
base.current_app.profile = 'jans'
sys.path.insert(0, base.pylib_dir)
from setup_app import paths

paths.LOG_DIR = os.path.join(setup_dir, 'logs')
paths.LOG_FILE = os.path.join(paths.LOG_DIR, 'external-data-loader.log')
paths.LOG_ERROR_FILE = os.path.join(paths.LOG_DIR, 'external-data-loader-error.log')
paths.LOG_OS_CHANGES_FILE = os.path.join(paths.LOG_DIR, 'external-data-loader-os-changes.log')

from setup_app import static
from setup_app.config import Config
from setup_app.utils.setup_utils import SetupUtils
from setup_app.utils.db_utils import dbUtils

Config.init(paths.INSTALL_DIR)
SetupUtils.init()

Config.templateRenderingDict.update(other_argsp)
Config.rdbm_type = 'mysql'
Config.rdbm_host = argsp.rdbm_host
Config.rdbm_port = argsp.rdbm_port
Config.rdbm_db = argsp.rdbm_db
Config.rdbm_user = argsp.rdbm_user
Config.rdbm_password = argsp.rdbm_password
for grp in Config.mapping_locations:
    Config.mapping_locations[grp] = 'rdbm'



class ExternalDataLoader(SetupUtils):

    def __init__(self):
        self.output_dir = '/tmp/output'
        self.dbUtils = dbUtils
        self.load_ldif_list = []

    def render_templates(self):
        for tmp in argsp.template:
            print("Rendering", tmp)
            self.renderTemplateInOut(tmp, os.path.dirname(tmp), self.output_dir)
            self.load_ldif_list.append(os.path.join(self.output_dir, os.path.basename(tmp)))

    def bind_database(self):
        print("Connecting to database")
        self.dbUtils.bind()

    def load_templates(self):
        print("Importing rendered templates")
        self.dbUtils.import_ldif(self.load_ldif_list)

external_data_loader = ExternalDataLoader()
external_data_loader.render_templates()
external_data_loader.bind_database()
external_data_loader.load_templates()
