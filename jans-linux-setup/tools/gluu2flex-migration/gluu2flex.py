import os
import sys
import json
import argparse
import datetime

from pathlib import Path
from collections import OrderedDict

cur_dir = os.path.dirname(os.path.realpath(__file__))
cur_path = Path(cur_dir)
par_path = cur_path.parent

if par_path.joinpath('setup_app').exists():
    setup_path = par_path
elif par_path.parent.joinpath('jans_setup/setup_app').exists():
    setup_path = par_path.parent.joinpath('jans_setup')
else:
    print("Can't determine setup path. Please follow gluu2filex migration instructions")
    sys.exit()

print(f"Setup path was found as {setup_path}")
sys.path.append(setup_path.as_posix())

from setup_app.utils.ldif_utils import myLdifParser
from setup_app.pylib.ldif4.ldif import LDIFParser, LDIFWriter

parser = argparse.ArgumentParser(description="This script migrates Gluu CE 4.x data to Jannsen")
argsp = parser.parse_args()

class BigLdifParser(LDIFParser):
    def __init__(self, ldif_file, op_function):
        self.ldif_file = ldif_file
        self.op_function = op_function

    def operate(self):
        with open(self.ldif_file, 'rb') as f:
            parser = LDIFParser(f)
            for dn, entry in parser.parse():
                for e in entry:
                    for i, v in enumerate(entry[e][:]):
                        if isinstance(v, bytes):
                            entry[e][i] = v.decode('utf-8')
                self.op_function(dn, entry)


def read_prop(prop_file):
    prop = {}
    with open(prop_file) as f:
        for l in f:
            ls = l.strip()
            if not ls or ls.startswith('#'):
                continue
            sep_l = [l.find(':'), l.find('=')]
            while -1 in sep_l:
                sep_l.remove(-1)
            if not sep_l:
                continue
            sep_n = min(sep_l)
            key = l[:sep_n].strip()
            val = l[sep_n+1:].strip()
            prop[key] = val

    return prop

host_vendor = None

for vendor in ('gluu', 'jans'):
    vendor_prop_fn = f'/etc/{vendor}/conf/{vendor}.properties'
    if os.path.exists(vendor_prop_fn):
        host_vendor = vendor
        salt_fn = f'/etc/{vendor}/conf/salt'
        salt = read_prop(salt_fn)['encodeSalt']

        if host_vendor == 'gluu':
            gluu_prop = read_prop(vendor_prop_fn)
            source_db_prop_file = f'/etc/{vendor}/conf/{vendor}-{gluu_prop["persistence.type"]}.properties'
            source_db_prop = read_prop(source_db_prop_file)
        break


print(f"Host vendor was determines as {host_vendor}")

if not host_vendor:
    print("Not running on Gluu or Jans host")
    sys.exit()

class Gluu2FlexMigrator:

    def __init__(self):
        self.migration_source_path = cur_path.joinpath('migration_source')
        self.migration_target_path = cur_path.joinpath('migration_target')

        self.schema_mappging_path = setup_path.joinpath('schema', 'jans_schema_mappings.json')
        self.schema_mappging_dict = json.loads(self.schema_mappging_path.read_text())
        self.org_units = {
            'people': ['gluuPerson'],
            'groups': ['gluuGroup'],
            'scopes': ['oxAuthCustomScope'],
            'clients': ['oxAuthClient'],
            'attributes': ['gluuAttribute'],
            'scripts': ['oxCustomScript'],
            'configuration': ['gluuConfiguration', 'oxAuthConfiguration', 'oxTrustConfiguration', 'gluuApplicationConfiguration', 'oxApplicationConfiguration'],
            }
        self.source_ldif_paths = {}
        self.target_ldif_paths = {}

        self.source_ldif_path = self.migration_source_path.joinpath('ldif_files')
        self.target_ldif_path = self.migration_target_path.joinpath('ldif_files')

        for org_unit in self.org_units:
            self.source_ldif_paths[org_unit] = self.source_ldif_path.joinpath(org_unit+'.ldif')
            self.target_ldif_paths[org_unit] = self.target_ldif_path.joinpath(org_unit+'.ldif')

        self.target_ldif_paths['admin_group_modify'] = self.target_ldif_path.joinpath('admin_group_modify.ldif')

        # create target dirs
        self.source_ldif_path.mkdir(parents=True, exist_ok=True)
        self.target_ldif_path.mkdir(parents=True, exist_ok=True)

        self.json_data_fn = self.migration_source_path.joinpath('data.json')

        if host_vendor == 'jans':
            # check if source data exists
            source_list = list(self.source_ldif_paths.values())
            for sfn in source_list:
                if not os.path.exists(sfn):
                    print(f"File {sfn} is missing. Can't proceed")
                    sys.exit()

            self.gluu_data = json.loads(self.json_data_fn.read_text())

            if self.gluu_data['salt'] != salt:
                print("Encode salt is not same with Gluu. Please install Jannsen Server with Gluu salt.")
                sys.exit()


    def write_json_data(self):
        self.json_data_fn.write_text(json.dumps({'salt': salt}, indent=2))

    def dump_data(self):
        if 'bindPassword' in source_db_prop:
            source_db_prop['bindPassword'] = os.popen(f'/opt/{vendor}/bin/encode.py -D ' + source_db_prop['bindPassword']).read().strip()
            ldap_passwd_fn = cur_path.joinpath('.pw')
            ldap_passwd_fn.write_text(source_db_prop['bindPassword'])
            for org_unit in self.org_units:
                print(f"Dumping {org_unit} from LDAP")
                for obj_cls in self.org_units[org_unit]:
                    cmd = f'/opt/opendj/bin/ldapsearch -X -Z -D "{source_db_prop["bindDN"]}" -j {ldap_passwd_fn} -h localhost -p 1636 -b "ou={org_unit},o=gluu" "(objectClass={obj_cls})" >> {self.source_ldif_paths[org_unit]}'
                    print(f"Executing {cmd}")
                    os.system(cmd)

            ldap_passwd_fn.unlink()

        elif 'auth.userPassword' in source_db_prop:
            source_db_prop['auth.userPassword'] = os.popen(f'/opt/{vendor}/bin/encode.py -D ' + source_db_prop['auth.userPassword']).read().strip()
            connection_uri_spl = source_db_prop['connection.uri'].split(':')
            connection_uri_spl[2] = connection_uri_spl[2].strip('/')
            db_port, db_name = connection_uri_spl[3].split('?')[0].split('/')

            sys.path.append('/install/community-edition-setup/setup_app/pylib/')

            import sqlalchemy
            import sqlalchemy.orm
            import sqlalchemy.ext.automap
            import sqlalchemy.dialects.mysql
            import sqlalchemy.dialects.postgresql

            from ldif4.ldif import LDIFWriter

            json_dialects_instance = sqlalchemy.dialects.mysql.json.JSON if connection_uri_spl[1] == 'mysql' else sqlalchemy.dialects.postgresql.json.JSONB

            db_str = 'mysql+pymysql' if connection_uri_spl[1] == 'mysql' else 'postgresql+psycopg2'
    
            bind_uri = '{}://{}:{}@{}:{}/{}'.format(
                        db_str,
                        source_db_prop['auth.userName'],
                        source_db_prop['auth.userPassword'],
                        connection_uri_spl[2].strip('/'),
                        db_port,
                        db_name,
                )

            if connection_uri_spl[1] == 'mysql':
                bind_uri += '?charset=utf8mb4'

            engine = sqlalchemy.create_engine(bind_uri)
            Session = sqlalchemy.orm.sessionmaker(bind=engine)
            session = Session()
            metadata = sqlalchemy.MetaData()
            session.connection()

            metadata.reflect(engine)
            Base = sqlalchemy.ext.automap.automap_base(metadata=metadata)
            Base.prepare()

            for org_table in self.org_units:
                print(f"Dumping data from {connection_uri_spl[1]} table {org_table}")
                with open(self.source_ldif_paths[org_table], 'wb') as w:
                    ldifw = LDIFWriter(w, cols=100000)
                    for tbl in self.org_units[org_table]:
                        orm_query = session.query(Base.classes[tbl])
                        print("Executing ", orm_query.statement.compile(engine))
                        for row_obj in orm_query.all():
                                ldif_entry = {}
                                for col_obj in row_obj.__table__.columns:
                                    col_val = getattr(row_obj, col_obj.name)
                                    if col_val is None:
                                        continue
                                    
                                    if col_obj.name == 'dn':
                                        dn = col_val = getattr(row_obj, col_obj.name)
                                        continue
                                    elif col_obj.name == 'doc_id':
                                        continue

                                    if isinstance(col_obj.type, json_dialects_instance):
                                        if col_val:
                                            if connection_uri_spl[1] == 'mysql':
                                                ldif_entry[col_obj.name] = col_val.get('v', [])
                                            else:
                                                ldif_entry[col_obj.name] = col_val
                                    elif col_obj.type.python_type == datetime.datetime:
                                        microsecond = f'{col_val.microsecond:03d}'
                                        ldif_entry[col_obj.name] = [f'{col_val.year}{col_val.month:02d}{col_val.day:02d}{col_val.hour:02d}{col_val.minute:02d}{col_val.second:02d}.{microsecond[:3]}Z']
                                    elif isinstance(col_obj.type, sqlalchemy.dialects.mysql.types.SMALLINT) or isinstance(col_obj.type, sqlalchemy.dialects.postgresql.BOOLEAN):
                                        ldif_entry[col_obj.name] = [str(bool(col_val)).upper()]
                                    elif not isinstance(col_val, str):
                                        ldif_entry[col_obj.name] = [str(col_val)]
                                    else:
                                        ldif_entry[col_obj.name] = [col_val]

                                ldif_entry['objectClass'].insert(0, 'top')
                                ldifw.unparse(dn, ldif_entry)


    def map_object_class(self, object_class_list):
        return [ self.schema_mappging_dict['objectClass'].get(oc, oc) for oc in object_class_list ]


    def migrate_people(self):
        print("Mapping Users")

        jans_people_fd = self.target_ldif_paths['people'].open('wb')
        jans_people_ldif_writer = LDIFWriter(jans_people_fd, cols=1000)
        self.admin_ui = None

        def user_mapper(dn, entry):
            # we are not migrating admin
            if entry['uid'][0] == 'admin':
                self.admin_ui = entry['inum'][0]
                return

            new_entry = {}
            for key in entry:
                if key in ('oxTrustEmail', 'gluuSLAManager'):
                    continue
                if key == 'objectClass':
                    new_entry['objectClass'] = self.map_object_class(entry['objectClass'])
                elif key == 'memberOf':
                    new_entry['memberOf'] = [ group.replace('o=gluu', 'o=jans') for group in entry['memberOf'] ]
                else:
                    new_key = self.schema_mappging_dict['attribute'].get(key, key)
                    new_entry[new_key] = entry[key]

            jans_people_ldif_writer.unparse(f'inum={entry["inum"][0]},ou=people,o=jans', new_entry)

        user_parser = BigLdifParser(self.source_ldif_paths['people'], user_mapper)
        user_parser.operate()
        jans_people_fd.close()

    def migrate_groups(self):
        print("Mapping Groups")

        jans_group_fd = self.target_ldif_paths['groups'].open('wb')
        jans_group_ldif_writer = LDIFWriter(jans_group_fd, cols=1000)

        jans_group_admin_fd = self.target_ldif_paths['admin_group_modify'].open('wb')
        jans_group_admin_ldif_writer = LDIFWriter(jans_group_admin_fd, cols=1000)

        def group_mapper(dn, entry):
            # we don't migrate manager group, instead add members to existing manager group
            if entry['inum'][0] == '60B7':
                for member in entry['member']:
                    # admin should be omittid since we did not migrate admin
                    if self.admin_ui in member:
                        continue
                    jans_group_admin_ldif_writer.unparse('inum=60B7,ou=groups,o=jans', [(0, 'member', [member.replace('o=gluu', 'o=jans')])])
                return

            new_entry = {}
            for key in entry:
                if key in ('gluuGroupVisibility', 'owner'):
                    continue
                if key == 'objectClass':
                    new_entry['objectClass'] = self.map_object_class(entry['objectClass'])
                elif key == 'member':
                    new_entry['member'] = [ member.replace('o=gluu', 'o=jans') for member in entry['member'] ]
                elif key =='o': 
                    new_entry['o'] = ['jans']
                else:
                    new_key = self.schema_mappging_dict['attribute'].get(key, key)
                    new_entry[new_key] = entry[key]

            jans_group_ldif_writer.unparse(f'inum={entry["inum"][0]},ou=groups,o=jans', new_entry)

        group_parser = BigLdifParser(self.source_ldif_paths['groups'], group_mapper)
        group_parser.operate()

        jans_group_fd.close()
        jans_group_admin_fd.close()


    def migrate_scopes(self):
        print("Migrating Scopes")
        scopes_parser = myLdifParser(self.source_ldif_paths['scopes'])
        scopes_parser.parse()

        jans_scopes_fd = self.target_ldif_paths['scopes'].open('wb')
        jans_scopes_ldif_writer = LDIFWriter(jans_scopes_fd, cols=1000)

        for dn, entry in scopes_parser.entries:
            new_entry = {}
            for key in entry:
                ## CLAIMS ??
                if key == 'objectClass':
                    new_entry['objectClass'] = self.map_object_class(entry['objectClass'])
                else:
                    new_key = self.schema_mappging_dict['attribute'].get(key, key)
                    new_entry[new_key] = entry[key]
            jans_scopes_ldif_writer.unparse(f'inum={entry["inum"][0]},ou=scopes,o=jans', new_entry)

        jans_scopes_fd.close()

    def migrate_clients(self):

        print("Migrating Clients")
        clients_parser = myLdifParser(self.source_ldif_paths['clients'])
        clients_parser.parse()

        jans_clients_fd = self.target_ldif_paths['clients'].open('wb')
        jans_clients_ldif_writer = LDIFWriter(jans_clients_fd, cols=1000)

        for dn, entry in clients_parser.entries:
            new_entry = {}
            for key in entry:
                if key == 'objectClass':
                    new_entry['objectClass'] = self.map_object_class(entry['objectClass'])
                else:
                    if key in ('oxAuthRequireAuthTime',):
                        continue
                    new_key = self.schema_mappging_dict['attribute'].get(key, key)
                    new_entry[new_key] = entry[key]
            for i, scope in enumerate(new_entry.get('jansScope', [])):
                new_entry['jansScope'][i] = new_entry['jansScope'][i].replace(',o=gluu', ',o=jans')
            jans_clients_ldif_writer.unparse(f'inum={entry["inum"][0]},ou=clients,o=jans', new_entry)

        jans_clients_fd.close()

    def migrate_attributes(self):
        print("Migrating Attributes")
        attributes_parser = myLdifParser(self.source_ldif_paths['attributes'])
        attributes_parser.parse()

        jans_attributes_fd = self.target_ldif_paths['attributes'].open('wb')
        jans_attributes_ldif_writer = LDIFWriter(jans_attributes_fd, cols=1000)

        for dn, entry in attributes_parser.entries:
            new_entry = {}
            for key in entry:
                if key == 'objectClass':
                    new_entry['objectClass'] = self.map_object_class(entry['objectClass'])
                else:
                    new_key = self.schema_mappging_dict['attribute'].get(key, key)
                    new_entry[new_key] = entry[key]
            jans_attributes_ldif_writer.unparse(f'inum={entry["inum"][0]},ou=attributes,o=jans', new_entry)

        jans_attributes_fd.close()


    def migrate_script(self, source_script):
        migration_lib_paths = [
                ('org.gluu.oxauth.cert', 'io.jans.as.common.cert'),
                ('org.gluu.oxauth.service.common', 'io.jans.as.server.service'),
                ('org.gluu.oxtrust.model', 'io.jans.model'),
                ('org.gluu.persist', 'io.jans.orm'),
                ('org.gluu.oxauth.util', 'io.jans.as.server.util'),
                ('org.gluu.oxauth', 'io.jans.as'),
                ('org.gluu', 'io.jans'),

                # Objects
                ('GluuCustomAttribute', 'JansCustomAttribute'),
                ('TokenLdap', 'TokenEntity'),

                # JAVA
                ('javax.', 'jakarta.'),
                ]

        translated_script = ''
        for l in source_script.splitlines():
            if (l.startswith('from ') and ' import ' in l) or l.startswith('import '):


                if ('EncryptionService' in l):
                    l = l.replace('org.gluu.oxauth.service.common', 'io.jans.as.common.service.common')
                else:

                    for spath, tpath in migration_lib_paths:
                        if spath in l:
                            l = l.replace(spath, tpath)

                for class_name in ('SessionIdService', 'AuthenticationService', 'HttpService', 'UserService'):
                    if class_name in l:
                        l = l.replace('.service', '.server.service')
                        break

                if 'Identity' in l:
                    l = l.replace('.security', '.server.security')

            translated_script += l + '\n'

        return translated_script


    def migrate_scripts(self):
        print("Migrating Scripts")
        scripts_parser = myLdifParser(self.source_ldif_paths['scripts'])
        scripts_parser.parse()

        jans_scripts_fd = self.target_ldif_paths['scripts'].open('wb')
        jans_scripts_ldif_writer = LDIFWriter(jans_scripts_fd, cols=1000)

        for dn, entry in scripts_parser.entries:
            new_entry = {}
            for key in entry:
                if key == 'objectClass':
                    new_entry['objectClass'] = self.map_object_class(entry['objectClass'])
                else:
                    new_key = self.schema_mappging_dict['attribute'].get(key, key)
                    new_entry[new_key] = entry[key]

            if new_entry.get('jansScr'):
                new_entry['jansScr'][0] = self.migrate_script(new_entry['jansScr'][0])

            jans_scripts_ldif_writer.unparse(f'inum={entry["inum"][0]},ou=scripts,o=jans', new_entry)

        jans_scripts_fd.close()


    def prepare_jans_setup(self):

        profile = 'jans'
        os.environ['JANS_PROFILE'] = profile
        from setup_app import paths

        paths.LOG_DIR = os.path.join(setup_path.as_posix(), 'logs')
        paths.LOG_FILE = os.path.join(paths.LOG_DIR, 'gluu2flex_migration.log')
        paths.LOG_ERROR_FILE = os.path.join(paths.LOG_DIR, 'gluu2flex_migration_error.log')
        paths.LOG_OS_CHANGES_FILE = os.path.join(paths.LOG_DIR, 'gluu2flex_migration_os-changes.log')

        from setup_app.utils import base
        base.current_app.profile = profile
        from setup_app import downloads
        from setup_app.utils import arg_parser
        argsp = arg_parser.get_parser()
        base.argsp = argsp

        if base.argsp.jans_app_version:
            base.current_app.app_info['jans_version'] = base.argsp.jans_app_version
        else:
            base.current_app.app_info['jans_version'] = base.current_app.app_info['JANS_APP_VERSION'] + base.current_app.app_info['JANS_BUILD']

        print("Downloading Python dependencies")
        downloads.download_pymysql()
        downloads.download_sqlalchemy()
        downloads.download_cryptography()
        downloads.download_pyjwt()

        sys.path.insert(0, base.pylib_dir)
        from setup_app.config import Config
        from setup_app.utils.setup_utils import SetupUtils
        from setup_app.utils.collect_properties import CollectProperties

        Config.init(paths.INSTALL_DIR)
        Config.installed_instance = True
        collect_properties = CollectProperties()
        collect_properties.collect()

        from setup_app.utils.db_utils import dbUtils

        self.base = base
        self.Config = Config
        self.dbUtils = dbUtils

    def parse_gluu_configuration_ldif(self):
        print("Parsing Gluu configuration")
        self.gluu_configuration_parser = myLdifParser(self.source_ldif_paths['configuration'])
        self.gluu_configuration_parser.parse()


    def update_casa_config(self):
        casa_jans_dn = 'ou=casa,ou=configuration,o=jans'
        jans_casa_config_entry = self.dbUtils.dn_exists(casa_jans_dn)
        if jans_casa_config_entry:
            jans_casa_config = json.loads(jans_casa_config_entry['jansConfApp'])
            for dn, entry in self.gluu_configuration_parser.entries:
                if dn == 'ou=casa,ou=configuration,o=gluu':
                    gluu_casa_config = json.loads(entry['oxConfApplication'][0])
                    gluu_casa_config.pop('oxd_config', None)
                    gluu_casa_config.get('acr_plugin_mapping', {}).pop('u2f', None)
                    jans_casa_config.update(gluu_casa_config)
                    print("Updating Gluu/Flex Casa configuration")
                    jans_casa_config_str = json.dumps(jans_casa_config, indent=2)
                    self.dbUtils.set_configuration('jansConfApp', jans_casa_config_str, casa_jans_dn)

    def import_ldif_files(self):

        for org_unit in self.org_units:
            if org_unit in ('configuration',):
                continue
            org_unit_ldif_fn = self.target_ldif_paths[org_unit].as_posix()
            print(f"Impoting {org_unit_ldif_fn}")
            self.dbUtils.import_ldif([org_unit_ldif_fn])


migrator = Gluu2FlexMigrator()

if host_vendor == 'gluu':
    migrator.write_json_data()
    migrator.dump_data()

if host_vendor == 'jans':
    migrator.migrate_people()
    migrator.migrate_groups()
    migrator.migrate_scopes()
    migrator.migrate_clients()
    migrator.migrate_attributes()
    migrator.migrate_scripts()
    migrator.prepare_jans_setup()
    migrator.import_ldif_files()
    migrator.parse_gluu_configuration_ldif()
    migrator.update_casa_config()
