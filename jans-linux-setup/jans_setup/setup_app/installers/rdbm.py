import os
import re
import sys
import time
import sqlalchemy
import shutil

from string import Template

from setup_app import paths
from setup_app.static import AppType, InstallOption
from setup_app.config import Config
from setup_app.utils import base
from setup_app.static import InstallTypes
from setup_app.installers.base import BaseInstaller
from setup_app.utils.setup_utils import SetupUtils
from setup_app.utils.package_utils import packageUtils


class RDBMInstaller(BaseInstaller, SetupUtils):

    source_files = [
                    (os.path.join(Config.dist_jans_dir, 'jans-orm-spanner-libs-distribution.zip'), os.path.join(base.current_app.app_info['JANS_MAVEN'], 'maven/io/jans/jans-orm-spanner-libs/{0}/jans-orm-spanner-libs-{0}-distribution.zip'.format(base.current_app.app_info['ox_version']))),
                    ]

    def __init__(self):
        setattr(base.current_app, self.__class__.__name__, self)
        self.needdb = False # we will connect later
        self.service_name = 'rdbm-server'
        self.app_type = AppType.APPLICATION
        self.install_type = InstallOption.OPTONAL
        self.install_var = 'rdbm_install'
        self.register_progess()
        self.output_dir = os.path.join(Config.output_dir, Config.rdbm_type)
        self.common_lib_dir = os.path.join(Config.jetty_base, 'common/libs/spanner')

    @property
    def qchar(self):
        return '`' if Config.rdbm_type in ('mysql', 'spanner') else '"'

    def install(self):
        if Config.rdbm_type == 'spanner':
            self.extract_libs()
        self.local_install()
        if Config.rdbm_install_type == InstallTypes.REMOTE and base.argsp.reset_rdbm_db:
            self.reset_rdbm_db()
        jans_schema_files = []
        self.jans_attributes = []
        for jans_schema_fn in ('jans_schema.json', 'custom_schema.json'):
            schema_full_path = os.path.join(Config.install_dir, 'schema', jans_schema_fn)
            jans_schema_files.append(schema_full_path)
            schema_ = base.readJsonFile(schema_full_path)
            self.jans_attributes += schema_.get('attributeTypes', [])

        self.create_tables(jans_schema_files)
        self.create_subtables()
        self.import_ldif()
        self.create_indexes()
        self.rdbmProperties()

    def reset_rdbm_db(self):
        self.logIt("Resetting DB {}".format(Config.rdbm_db))
        self.dbUtils.metadata.reflect(self.dbUtils.engine)
        self.dbUtils.metadata.drop_all(self.dbUtils.engine)
        self.dbUtils.session.commit()
        self.dbUtils.metadata.clear()

    def local_install(self):
        if not Config.rdbm_password:
            Config.rdbm_password = self.getPW()
        if not Config.rdbm_user:
            Config.rdbm_user = 'jans'

        if Config.rdbm_install_type == InstallTypes.LOCAL:
            base.argsp.n = True
            packageUtils.check_and_install_packages()

            if Config.rdbm_type == 'mysql':
                if base.os_type == 'suse':
                    self.restart('mariadb')
                    self.enable('mariadb')
                elif base.clone_type == 'rpm':
                    self.restart('mysqld')
                result, conn = self.dbUtils.mysqlconnection(log=False)
                if not result:
                    sql_cmd_list = [
                        "CREATE DATABASE {};\n".format(Config.rdbm_db),
                        "CREATE USER '{}'@'localhost' IDENTIFIED BY '{}';\n".format(Config.rdbm_user, Config.rdbm_password),
                        "GRANT ALL PRIVILEGES ON {}.* TO '{}'@'localhost';\n".format(Config.rdbm_db, Config.rdbm_user),
                        ]
                    for cmd in sql_cmd_list:
                        self.run("echo \"{}\" | mysql".format(cmd), shell=True)

            elif Config.rdbm_type == 'pgsql':
                if base.clone_type == 'rpm':
                    self.run(['postgresql-setup', 'initdb'])
                elif base.clone_type == 'deb':
                    self.run([paths.cmd_chmod, '640', '/etc/ssl/private/ssl-cert-snakeoil.key'])

                self.restart('postgresql')

                cmd_create_db = '''su - postgres -c "psql -U postgres -d postgres -c \\"CREATE DATABASE {};\\""'''.format(Config.rdbm_db)
                cmd_create_user = '''su - postgres -c "psql -U postgres -d postgres -c \\"CREATE USER {} WITH PASSWORD '{}';\\""'''.format(Config.rdbm_user, Config.rdbm_password)
                cmd_grant_previlages = '''su - postgres -c "psql -U postgres -d postgres -c \\"GRANT ALL PRIVILEGES ON DATABASE {} TO {};\\""'''.format(Config.rdbm_db, Config.rdbm_user)

                for cmd in (cmd_create_db, cmd_create_user, cmd_grant_previlages):
                    self.run(cmd, shell=True)

                if base.clone_type == 'rpm':
                    hba_file_path_query = self.run('''su - postgres -c "psql -U postgres -d postgres -t -c \\"SHOW hba_file;\\""''', shell=True)
                    if hba_file_path_query and hba_file_path_query.strip():
                        self.stop('postgresql')
                        hba_file_path = hba_file_path_query.strip()
                        hba_file_content = self.readFile(hba_file_path)
                        hba_file_content = 'host\t{0}\t{1}\t127.0.0.1/32\tmd5\nhost\t{0}\t{1}\t::1/128\tmd5\n'.format(Config.rdbm_db, Config.rdbm_user) + hba_file_content
                        self.writeFile(hba_file_path, hba_file_content)
                        self.start('postgresql')

            self.enable('postgresql')

        self.dbUtils.bind(force=True)

    def get_sql_col_type(self, attrname, table=None):

        if attrname in self.dbUtils.sql_data_types:
            type_ = self.dbUtils.sql_data_types[attrname].get(Config.rdbm_type) or self.dbUtils.sql_data_types[attrname]['mysql']
            if table in type_.get('tables', {}):
                type_ = type_['tables'][table]
            if 'size' in type_:
                data_type = '{}({})'.format(type_['type'], type_['size'])
            else:
                data_type = type_['type']
        else:
            attr_syntax = self.dbUtils.get_attr_syntax(attrname)
            type_ = self.dbUtils.ldap_sql_data_type_mapping[attr_syntax].get(Config.rdbm_type) or self.dbUtils.ldap_sql_data_type_mapping[attr_syntax]['mysql']

            char_type = 'STRING' if Config.rdbm_type == 'spanner' else 'VARCHAR'

            if type_['type'] in char_type:
                if type_['size'] <= 127:
                    data_type = '{}({})'.format(char_type, type_['size'])
                elif type_['size'] <= 255:
                    data_type = 'TINYTEXT' if Config.rdbm_type == 'mysql' else 'TEXT'
                else:
                    data_type = 'TEXT'
            else:
                data_type = type_['type']

        if data_type == 'TEXT' and Config.rdbm_type == 'spanner':
            data_type = 'STRING(MAX)'

        return data_type


    def get_col_def(self, attrname, sql_tbl_name):
        data_type = self.get_sql_col_type(attrname, sql_tbl_name)
        col_def = '{0}{1}{0} {2}'.format(self.qchar, attrname, data_type)
        if Config.rdbm_type == 'mysql' and data_type == 'JSON':
            col_def += ' comment "json"'
        return col_def

    def create_tables(self, jans_schema_files):
        self.logIt("Creating tables for {}".format(jans_schema_files))
        tables = []
        all_schema = {}
        all_attribs = {}
        column_add = 'COLUMN ' if Config.rdbm_type == 'spanner' else ''
        alter_table_sql_cmd = 'ALTER TABLE %s{}%s ADD %s{};' % (self.qchar, self.qchar, column_add)

        for jans_schema_fn in jans_schema_files:
            jans_schema = base.readJsonFile(jans_schema_fn)
            for obj in jans_schema['objectClasses']:
                all_schema[obj['names'][0]] = obj
            for attr in jans_schema['attributeTypes']:
                all_attribs[attr['names'][0]] = attr

        subtable_attrs = {}
        for stbl in self.dbUtils.sub_tables.get(Config.rdbm_type):
            subtable_attrs[stbl] = [ scol[0] for scol in self.dbUtils.sub_tables[Config.rdbm_type][stbl] ]

        for obj_name in all_schema:
            obj = all_schema[obj_name]

            if obj.get('sql', {}).get('ignore'):
                continue

            sql_tbl_name = obj['names'][0]
            sql_tbl_cols = []

            attr_list = obj['may']
            if 'sql' in obj:
                attr_list += obj['sql'].get('include',[])
                if 'includeObjectClass' in obj['sql']:
                    for incobjcls in obj['sql']['includeObjectClass']:
                        attr_list += all_schema[incobjcls]['may']

            for s in obj['sup']:
                if s == 'top':
                    continue
                attr_list += all_schema[s]['may']

            cols_ =[]
            for attrname in attr_list:
                if attrname in cols_:
                    continue

                if attrname in subtable_attrs.get(sql_tbl_name, []):
                    continue

                cols_.append(attrname)
                col_def = self.get_col_def(attrname, sql_tbl_name) 
                sql_tbl_cols.append(col_def)

            if not self.dbUtils.table_exists(sql_tbl_name):
                doc_id_type = self.get_sql_col_type('doc_id', sql_tbl_name)
                if Config.rdbm_type == 'pgsql':
                    sql_cmd = 'CREATE TABLE "{}" (doc_id {} NOT NULL UNIQUE, "objectClass" VARCHAR(48), dn VARCHAR(128), {}, PRIMARY KEY (doc_id));'.format(sql_tbl_name, doc_id_type, ', '.join(sql_tbl_cols))
                elif Config.rdbm_type == 'spanner':
                    sql_cmd = 'CREATE TABLE `{}` (`doc_id` {} NOT NULL, `objectClass` STRING(48), dn STRING(128), {}) PRIMARY KEY (`doc_id`)'.format(sql_tbl_name, doc_id_type, ', '.join(sql_tbl_cols))
                else:
                    sql_cmd = 'CREATE TABLE `{}` (`doc_id` {} NOT NULL UNIQUE, `objectClass` VARCHAR(48), dn VARCHAR(128), {}, PRIMARY KEY (`doc_id`));'.format(sql_tbl_name, doc_id_type, ', '.join(sql_tbl_cols))
                self.dbUtils.exec_rdbm_query(sql_cmd)
                tables.append(sql_cmd)

        for attrname in all_attribs:
            attr = all_attribs[attrname]
            if attr.get('sql', {}).get('add_table'):
                col_def = self.get_col_def(attrname, sql_tbl_name)
                sql_cmd = alter_table_sql_cmd.format(attr['sql']['add_table'], col_def)

                if Config.rdbm_type == 'spanner':
                    req = self.dbUtils.spanner_client.exec_sql(sql_cmd.strip(';'))
                else:
                    self.dbUtils.exec_rdbm_query(sql_cmd)
                tables.append(sql_cmd)

        self.writeFile(os.path.join(self.output_dir, 'jans_tables.sql'), '\n'.join(tables))

    def create_subtables(self):

        for subtable in self.dbUtils.sub_tables.get(Config.rdbm_type, {}):
            for sattr, sdt in self.dbUtils.sub_tables[Config.rdbm_type][subtable]:
                subtable_columns = []
                sql_cmd = 'CREATE TABLE `{0}_{1}` (`doc_id` STRING(64) NOT NULL, `dict_doc_id` STRING(64), `{1}` {2}) PRIMARY KEY (`doc_id`, `dict_doc_id`), INTERLEAVE IN PARENT `{0}` ON DELETE CASCADE'.format(subtable, sattr, sdt)
                self.dbUtils.spanner_client.exec_sql(sql_cmd)
                sql_cmd_index = 'CREATE INDEX `{0}_{1}Idx` ON `{0}_{1}` (`{1}`)'.format(subtable, sattr)
                self.dbUtils.spanner_client.exec_sql(sql_cmd_index)


    def get_index_name(self, attrname):
        return re.sub(r'[^0-9a-zA-Z\s]+','_', attrname)


    def create_indexes(self):

        indexes = []

        sql_indexes_fn = os.path.join(Config.static_rdbm_dir, Config.rdbm_type + '_index.json')
        sql_indexes = base.readJsonFile(sql_indexes_fn)

        # read opendj indexes and add multivalued attributes to JSON indexing
        opendj_index = base.readJsonFile(base.current_app.OpenDjInstaller.openDjIndexJson)
        opendj_index_list = [ atribute['attribute'] for atribute in opendj_index ]

        for attribute in self.jans_attributes:
            if attribute.get('multivalued'):
                for attr_name in attribute['names']:
                    if attr_name in opendj_index_list and attr_name not in sql_indexes['__common__']['fields']:
                        sql_indexes['__common__']['fields'].append(attr_name)

        if Config.rdbm_type == 'spanner':
            tables = self.dbUtils.spanner_client.get_tables()
            for tblCls in tables:
                tbl_fields = sql_indexes.get(tblCls, {}).get('fields', []) + sql_indexes['__common__']['fields']

                tbl_data = self.dbUtils.spanner_client.exec_sql('SELECT * FROM {} LIMIT 1'.format(tblCls))

                for attr in tbl_data.get('fields', []):
                    if attr['name'] == 'doc_id':
                        continue
                    attr_name = attr['name']
                    ind_name = self.get_index_name(attr['name'])
                    data_type = attr['type']

                    if data_type == 'ARRAY':
                        # How to index for ARRAY types in spanner?
                        pass

                    elif attr_name in tbl_fields:
                        sql_cmd = 'CREATE INDEX `{1}_{0}Idx` ON `{1}` (`{2}`)'.format(
                                    ind_name,
                                    tblCls,
                                    attr_name
                                )
                        self.dbUtils.spanner_client.exec_sql(sql_cmd)

                for i, custom_index in enumerate(sql_indexes.get(tblCls, {}).get('custom', [])):
                    sql_cmd = 'CREATE INDEX `{0}_CustomIdx{1}` ON {0} ({2})'.format(
                                    tblCls,
                                    i+1, 
                                    custom_index
                                )
                    self.dbUtils.spanner_client.exec_sql(sql_cmd)

        else:
            for tblCls in self.dbUtils.Base.classes.keys():
                tblObj = self.dbUtils.Base.classes[tblCls]()
                tbl_fields = sql_indexes.get(tblCls, {}).get('fields', []) +  sql_indexes['__common__']['fields']

                for attr in tblObj.__table__.columns:
                    if attr.name == 'doc_id':
                        continue
                    ind_name = self.get_index_name(attr.name)
                    data_type = self.get_sql_col_type(attr, tblCls)
                    data_type = data_type.replace('VARCHAR', 'CHAR')

                    if isinstance(attr.type, self.dbUtils.json_dialects_instance):

                        if attr.name in tbl_fields:
                            for i, ind_str in enumerate(sql_indexes['__common__']['JSON']):
                                tmp_str = Template(ind_str)
                                if Config.rdbm_type == 'mysql':
                                    sql_cmd = 'ALTER TABLE {0}.{1} ADD INDEX `{2}_json_{3}`(({4}));'.format(
                                            Config.rdbm_db,
                                            tblCls,
                                            ind_name,
                                            i+1,
                                            tmp_str.safe_substitute({'field':attr.name})
                                            )
                                    self.dbUtils.exec_rdbm_query(sql_cmd)
                                elif Config.rdbm_type == 'pgsql':
                                    sql_cmd ='CREATE INDEX ON "{}" {};'.format(
                                            tblCls,
                                            tmp_str.safe_substitute({'field':attr.name})
                                            )
                                    self.dbUtils.exec_rdbm_query(sql_cmd)


                    elif attr.name in tbl_fields:
                        if Config.rdbm_type == 'mysql':
                            sql_cmd = 'ALTER TABLE {0}.{1} ADD INDEX `{1}_{2}` (`{3}`);'.format(
                                        Config.rdbm_db,
                                        tblCls,
                                        ind_name,
                                        attr.name
                                    )
                            self.dbUtils.exec_rdbm_query(sql_cmd)
                        elif Config.rdbm_type == 'pgsql':
                            sql_cmd = 'CREATE INDEX ON "{}" ("{}");'.format(
                                        tblCls,
                                        attr.name
                                    )
                            self.dbUtils.exec_rdbm_query(sql_cmd)

                for i, custom_index in enumerate(sql_indexes.get(tblCls, {}).get('custom', [])):
                    if Config.rdbm_type == 'mysql':
                        sql_cmd = 'ALTER TABLE {0}.{1} ADD INDEX `{2}` ({3});'.format(
                                        Config.rdbm_db,
                                        tblCls,
                                        '{}_CustomIdx{}'.format(tblCls, i+1),
                                        custom_index
                                    )
                        self.dbUtils.exec_rdbm_query(sql_cmd)
                    elif Config.rdbm_type == 'pgsql':
                        sql_cmd = 'CREATE INDEX ON "{}" {};'.format(
                                    tblCls,
                                    custom_index
                                    )
                        self.dbUtils.exec_rdbm_query(sql_cmd)

    def import_ldif(self):
        ldif_files = []

        if Config.mapping_locations['default'] == 'rdbm':
            ldif_files += Config.couchbaseBucketDict['default']['ldif']

        ldap_mappings = self.getMappingType('rdbm')

        for group in ldap_mappings:
            ldif_files +=  Config.couchbaseBucketDict[group]['ldif']

        if Config.get('ldif_metric') in ldif_files:
            ldif_files.remove(Config.ldif_metric)

        if Config.get('ldif_site') in ldif_files:
            ldif_files.remove(Config.ldif_site)

        Config.pbar.progress(self.service_name, "Importing ldif files to {}".format(Config.rdbm_type), False)
        if Config.ldif_base not in ldif_files:
            if Config.rdbm_type == 'mysql':
                force = BackendTypes.MYSQL
            elif Config.rdbm_type == 'pgsql':
                force = BackendTypes.PGSQL
            elif Config.rdbm_type == 'spanner':
                force = BackendTypes.SPANNER
            self.dbUtils.import_ldif([Config.ldif_base], force=force)

        self.dbUtils.import_ldif(ldif_files)

    def rdbmProperties(self):
        if Config.rdbm_type in ('pgsql', 'mysql'):
            Config.rdbm_password_enc = self.obscure(Config.rdbm_password)
            src_temp_fn = os.path.join(Config.templateFolder, 'jans-{}.properties'.format(Config.rdbm_type))
            targtet_fn = os.path.join(Config.configFolder, Config.jansRDBMProperties)
            rendered_tmp = self.render_template(src_temp_fn)
            self.writeFile(targtet_fn, rendered_tmp)

        elif Config.rdbm_type == 'spanner':
            if Config.spanner_emulator_host:
                Config.templateRenderingDict['spanner_creds'] = 'connection.emulator-host={}:9010'.format(Config.spanner_emulator_host)
            else:
                auth_cred_target_fn = os.path.join(Config.configFolder, 'google_application_credentials.json')
                shutil.copy(Config.google_application_credentials, auth_cred_target_fn)
                Config.templateRenderingDict['spanner_creds'] = 'auth.credentials-file={}'.format(auth_cred_target_fn)

            self.renderTemplateInOut(Config.jansSpannerProperties, Config.templateFolder, Config.configFolder)

    def create_folders(self):
        self.createDirs(Config.static_rdbm_dir)

    def extract_libs(self):
        self.logIt("Extracting {}".format(self.source_files[0][0]))
        if not os.path.exists(self.common_lib_dir):
            self.createDirs(self.common_lib_dir)
        shutil.unpack_archive(self.source_files[0][0], self.common_lib_dir)
        self.chown(os.path.join(Config.jetty_base, 'common'), Config.jetty_user, Config.jetty_user, True)

    def installed(self):
        # to be implemented
        return True

