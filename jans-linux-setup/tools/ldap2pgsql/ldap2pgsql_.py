#!/usr/bin/python3
import warnings
warnings.filterwarnings("ignore")
import os
import sys
import json
import argparse

from collections import OrderedDict
try:
    from ldap.schema.models import AttributeType, ObjectClass
except:
    print("This tool requires python3-ldap package. Please install and re-run")
    sys.exit()

parser = argparse.ArgumentParser(description="Jans LDAP to RDBM migrator script")
parser.add_argument('-remote-rdbm', choices=['mysql', 'pgsql'], help="Enables using remote RDBM server", default='mysql')
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
print('argsp_dict (1) = {}'.format(argsp_dict))
argsp_dict['rdbm_type'] = getattr(argsp, 'remote_rdbm')
print('argsp_dict (2) = {}'.format(argsp_dict))

sys.argv = [sys.argv[0]]

#sys.exit();

#result = input("This is experimental script. Continue anyway? [y/N]")
#if not result.lower().startswith('y'):
#    sys.exit()

#from setup_app.utils.arg_parser import arg_parser
#argsp = arg_parser()

from setup_app import paths
from setup_app import static

from setup_app.utils import base
base.argsp = argsp

base.current_app.profile = 'disa-stig'

from setup_app.config import Config
from setup_app.utils.collect_properties import CollectProperties
from setup_app.utils.setup_utils import SetupUtils
from setup_app.utils.properties_utils import PropertiesUtils
from setup_app.installers.jans import JansInstaller
from setup_app.utils.ldif_utils import myLdifParser
from setup_app.installers.rdbm import RDBMInstaller
from setup_app.pylib.ldif4.ldif import LDIFWriter

from setup_app.utils.db_utils import dbUtils

Config.init(paths.INSTALL_DIR)
#Config.determine_version()

print("Config.mapping_locations['default'] = {}".format(Config.mapping_locations['default']))
Config.mapping_locations['default'] = 'rdbm'
print("Config.mapping_locations['default'] = {}".format(Config.mapping_locations['default']))

for x in Config.mapping_locations.keys():
    Config.mapping_locations[x] = 'rdbm'
#    print(x)

#Config.mapping_locations = {'default': 'rdbm', 'user': 'ldap', 'site': 'ldap', 'cache': 'ldap', 'token': 'ldap', 'session': 'ldap'}

#Config.mapping_locations['default']

#SetupUtils.init()
#jansInstaller = JansInstaller()
#jansInstaller.initialize()
#collectProperties = CollectProperties()
#collectProperties.collect()
Config.installed_instance = True
#rdbmInstaller = RDBMInstaller()
#propertiesUtils = PropertiesUtils()

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

print('dbUtils.sqlconnection() ------------------------ >>')
result, conn = dbUtils.sqlconnection(log=False)
print('result = {}'.format(result))
print('conn = {}'.format(conn))
print('dbUtils.sqlconnection() ------------------------ <<')

print('dbUtils.bind() ------------------------ >>')
dbUtils.bind()
print('dbUtils.bind() ------------------------ <<')

print('argsp_dict[in_ldif_fpath] = {}'.format(argsp_dict['in_ldif_fpath']))

dbUtils.import_ldif([argsp_dict['in_ldif_fpath']])

print('sys.exit()')

sys.exit();

from collections import OrderedDict
from ldap.schema.models import AttributeType, ObjectClass

jansInstaller.createLdapPw()

current_ldif_fn = os.path.join(Config.output_dir, 'current_data.ldif')

print("Dumping all database from LDAP to {}. This may take a while...".format(current_ldif_fn))

jansInstaller.run(' '.join([
                        '/opt/opendj/bin/ldapsearch',
                        '-X', '-Z', '-D',
                        '"{}"'.format(Config.ldap_binddn),
                        '-j',
                        Config.ldapPassFn,
                        '-h',
                        Config.ldap_hostname,
                        '-p',
                        '1636',
                        '-b',
                        'o=jans',
                        'ObjectClass=*',
                        '>',
                        current_ldif_fn]), shell=True)

jansInstaller.deleteLdapPw()

print("Preparing custom schema from attributes")
schema = {'attributeTypes':[], 'objectClasses':[]}

for attr_s in parser.entries[0][1].get('attributeTypes', []):
    attr_obj = AttributeType(attr_s)
    attr_dict = {a:getattr(attr_obj, a) for a in ('desc', 'equality', 'names', 'oid', 'substr', 'syntax', 'x_origin') }
    attr_dict['x_origin'] = attr_dict['x_origin'][0]

    schema['attributeTypes'].append(attr_dict)

for obcls_s in parser.entries[0][1]['objectClasses']:
    obcls_obj = ObjectClass(obcls_s)
    obcls_dict = {a:getattr(obcls_obj, a) for a in ('may', 'names', 'oid', 'x_origin') }
    obcls_dict['x_origin'] = obcls_dict['x_origin'][0]
    schema['objectClasses'].append(obcls_dict)


current_attributes = jansInstaller.dbUtils.search("ou=attributes,o=jans", fetchmany=True)

with open(os.path.join(Config.install_dir, 'schema/custom_schema.json')) as f:
    jans_custom_schma = json.load(f)


for custom_ocl in jans_custom_schma['objectClasses']:
    if custom_ocl['names'][0] == 'jansCustomPerson':
        jans_custom_ocl_names = custom_ocl['may'][:]
        break


for cur_ocl in schema['objectClasses']:
    if cur_ocl['names'][0] == 'jansCustomPerson':
        for cur_anme in cur_ocl['may']:
            if not cur_anme in jans_custom_ocl_names:
                for cur_atr in schema['attributeTypes']:
                    if cur_atr['names'][0] == cur_anme:
                        for dn, entry in current_attributes:
                            if entry['jansAttrName'] == cur_anme:
                                    if entry.get('jansMultivaluedAttr'):
                                        cur_atr['multivalued'] = True

                        jans_custom_schma['attributeTypes'].append(cur_atr)
                        custom_ocl['may'].append(cur_anme)



current_custom_schema_fn = os.path.join(Config.install_dir, 'schema/custom_schema.json')

with open(current_custom_schema_fn, 'w') as w:
    json.dump(jans_custom_schma, w, indent=2)


schmema_files = [
    os.path.join(Config.install_dir, 'schema/jans_schema.json'),
    current_custom_schema_fn
    ]

rdbmInstaller.dbUtils.read_jans_schema()

for a in rdbm_config_params:
    if argsp_dict[a]:
        setattr(Config, a, argsp_dict[a])

Config.opendj_install = static.InstallTypes.NONE
Config.rdbm_install = static.InstallTypes.REMOTE

Config.mapping_locations = { group: 'rdbm' for group in Config.couchbaseBucketDict }

rdbmInstaller.dbUtils.bind(force=True)
propertiesUtils.set_persistence_type()

print("Creating RDBM tables")
rdbmInstaller.create_tables(schmema_files)

print("Re-formatting data. This will take a while...")
cur_data_parser = myLdifParser(current_ldif_fn)
cur_data_parser.parse()

current_ldif_tmp_fn = current_ldif_fn + '~'

with open(current_ldif_tmp_fn, 'wb') as w:
    ldif_writer = LDIFWriter(w, cols=10000)

    for dn, entry in cur_data_parser.entries:
        if 'jansAttr' in entry['objectClass']:
            entry['description'][0] = entry['description'][0][:768]
        ldif_writer.unparse(dn, entry)

os.rename(current_ldif_tmp_fn, current_ldif_fn)


print("Importing data into RDBM from {}. This may take a while ...".format(current_ldif_fn))
rdbmInstaller.dbUtils.import_ldif([current_ldif_fn])
print("Creating indexes...")
rdbmInstaller.create_indexes()

print("Writing Jans config properties")
rdbmInstaller.rdbmProperties()
jansInstaller.renderTemplateInOut(
                    Config.jans_properties_fn,
                    os.path.join(Config.install_dir, 'templates'),
                    Config.configFolder
                )

if os.path.exists(Config.ox_ldap_properties):
    os.rename(Config.ox_ldap_properties, Config.ox_ldap_properties+'.org')

print("Please disable opendj and restart services")
