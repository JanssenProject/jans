import os
import re
import sys
import datetime
import sqlalchemy

from string import Template

from setup_app.static import AppType, InstallOption
from setup_app.config import Config
from setup_app.utils import base
from setup_app.static import InstallTypes
from setup_app.installers.base import BaseInstaller
from setup_app.utils.setup_utils import SetupUtils


class RDBMInstaller(BaseInstaller, SetupUtils):

    packageUtils = None

    def __init__(self):
        self.needdb = False # we will connect later
        self.service_name = 'rdbm-server'
        self.app_type = AppType.APPLICATION
        self.install_type = InstallOption.OPTONAL
        self.install_var = 'rdbm_install'
        self.register_progess()

        self.output_dir = os.path.join(Config.outputFolder, Config.rdbm_type)

    def install(self):
        self.local_install()
        jans_schema_files = []
        
        for jans_schema_fn in ('jans_schema.json', 'custom_schema.json'):
            jans_schema_files.append(os.path.join(Config.install_dir, 'schema', jans_schema_fn))

        self.create_tables(jans_schema_files)
        self.import_ldif()
        self.create_indexes()
        self.rdbmProperties()
        

    def local_install(self):
        if not Config.rdbm_password:
            Config.rdbm_password = self.getPW()
        if not Config.rdbm_user:
            Config.rdbm_user = 'jans'

        if Config.rdbm_install_type == InstallTypes.LOCAL:

            result, conn = self.dbUtils.mysqlconnection(log=False)
            if not result:
                sql_cmd_list = [
                    "CREATE DATABASE {};\n".format(Config.rdbm_db),
                    "CREATE USER '{}'@'localhost' IDENTIFIED BY '{}';\n".format(Config.rdbm_user, Config.rdbm_password),
                    "GRANT ALL PRIVILEGES ON {}.* TO '{}'@'localhost';\n".format(Config.rdbm_db, Config.rdbm_user),
                    ]
                for cmd in sql_cmd_list:
                    self.run("echo \"{}\" | mysql".format(cmd), shell=True)

        self.dbUtils.bind()

    def get_sql_col_type(self, attrname):
        if attrname in self.dbUtils.sql_data_types:
            type_ = self.dbUtils.sql_data_types[attrname]
            if type_[Config.rdbm_type]['type'] == 'VARCHAR':
                if type_[Config.rdbm_type]['size'] <= 127:
                    data_type = 'VARCHAR({})'.format(type_[Config.rdbm_type]['size'])
                elif type_[Config.rdbm_type]['size'] <= 255:
                    data_type = 'TINYTEXT'
                else:
                    data_type = 'TEXT'
            else:
                data_type = type_[Config.rdbm_type]['type']

        else:
            attr_syntax = self.dbUtils.get_attr_syntax(attrname)
            type_ = self.dbUtils.ldap_sql_data_type_mapping[attr_syntax]
            if type_[Config.rdbm_type]['type'] == 'VARCHAR':
                data_type = 'VARCHAR({})'.format(type_[Config.rdbm_type]['size'])
            else:
                data_type = type_[Config.rdbm_type]['type']

        return data_type

    def create_tables(self, jans_schema_files):

        tables = []
        all_schema = {}
        all_attribs = {}
        alter_table_sql_cmd = 'ALTER TABLE `{}` ADD {};'

        for jans_schema_fn in jans_schema_files:
            jans_schema = base.readJsonFile(jans_schema_fn)
            for obj in jans_schema['objectClasses']:
                all_schema[obj['names'][0]] = obj
            for attr in jans_schema['attributeTypes']:
                all_attribs[attr['names'][0]] = attr

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
            cols_ =[]
            for attrname in attr_list:
                if attrname in cols_:
                    continue

                cols_.append(attrname)
                data_type = self.get_sql_col_type(attrname)                
                col_def = '`{}` {}'.format(attrname, data_type)
                sql_tbl_cols.append(col_def)


            if self.dbUtils.table_exists(sql_tbl_name):
                for tbl_col in sql_tbl_cols:
                    self.dbUtils.exec_rdbm_query(alter_table_sql_cmd.format(sql_tbl_name, tbl_col))
                    tables.append(sql_cmd)
            else:
                sql_cmd = 'CREATE TABLE `{}` (`id` int NOT NULL auto_increment, `doc_id` VARCHAR(64) NOT NULL UNIQUE, `objectClass` VARCHAR(48), dn VARCHAR(128), {}, PRIMARY KEY  (`id`, `doc_id`));'.format(sql_tbl_name, ', '.join(sql_tbl_cols))
                self.dbUtils.exec_rdbm_query(sql_cmd)
                tables.append(sql_cmd)

        for attrname in all_attribs:
            attr = all_attribs[attrname]
            if attr.get('sql', {}).get('add_table'):
                data_type = self.get_sql_col_type(attrname)
                col_def = '`{}` {}'.format(attrname, data_type)
                sql_cmd = alter_table_sql_cmd.format(attr['sql']['add_table'], col_def)
                self.dbUtils.exec_rdbm_query(sql_cmd)
                tables.append(sql_cmd)

        self.writeFile(os.path.join(self.output_dir, 'jans_tables.sql'), '\n'.join(tables))

    def create_indexes(self):
        indexes = []

        sql_indexes_fn = os.path.join(Config.static_rdbm_dir, 'sql_index.json')
        sql_indexes = base.readJsonFile(sql_indexes_fn)

        cb_indexes = base.readJsonFile(base.current_app.couchbaseInstaller.couchbaseIndexJson)

        cb_fields = []

        for bucket in cb_indexes:
            bucket_indexes = cb_indexes[bucket]
            if 'attributes' in bucket_indexes:
                for atr_list in bucket_indexes['attributes']:
                    for field in atr_list: 
                        if not field in cb_fields:
                            cb_fields.append(field)


            if 'static' in bucket_indexes:
                for atr_list in bucket_indexes['static']:
                    for field in atr_list[0]: 
                        if not field in cb_fields and not '(' in field:
                            cb_fields.append(field)


        for tblCls in self.dbUtils.Base.classes.keys():
            tblObj = self.dbUtils.Base.classes[tblCls]()
            tbl_fields = sql_indexes.get(tblCls, {}).get('fields', []) +  sql_indexes['__common__']['fields'] + cb_fields
            for attr in tblObj.__table__.columns:
                if attr.name == 'doc_id':
                    continue
                ind_name = re.sub(r'[^0-9a-zA-Z\s]+','_', attr.name)
                if isinstance(attr.type, sqlalchemy.dialects.mysql.json.JSON):
                    for i, ind_str in enumerate(sql_indexes['__common__']['JSON']):
                        tmp_str = Template(ind_str)
                        sql_cmd = 'ALTER TABLE {0}.{1} ADD INDEX `{2}_json_{3}`(({4}));'.format(
                                    Config.rdbm_db,
                                    tblCls,
                                    ind_name,
                                    i+1,
                                    tmp_str.safe_substitute({'field':attr.name})
                                    )
                        self.dbUtils.exec_rdbm_query(sql_cmd)
                elif attr.name in tbl_fields:
                    sql_cmd = 'ALTER TABLE {0}.{1} ADD INDEX `{1}_{2}` (`{3}`);'.format(
                                Config.rdbm_db,
                                tblCls,
                                ind_name,
                                attr.name
                                )
                    self.dbUtils.exec_rdbm_query(sql_cmd)


    def import_ldif(self):
        ldif_files = []

        if Config.mappingLocations['default'] == 'rdbm':
            ldif_files += Config.couchbaseBucketDict['default']['ldif']

        ldap_mappings = self.getMappingType('rdbm')

        for group in ldap_mappings:
            ldif_files +=  Config.couchbaseBucketDict[group]['ldif']

        if Config.ldif_metric in ldif_files:
            ldif_files.remove(Config.ldif_metric)

        if Config.ldif_site in ldif_files:
            ldif_files.remove(Config.ldif_site)

        Config.pbar.progress(self.service_name, "Importing ldif files to {}".format(Config.rdbm_type), False)
        if not Config.ldif_base in ldif_files:
            self.dbUtils.import_ldif([Config.ldif_base], force=BackendTypes.MYSQL)

        self.dbUtils.import_ldif(ldif_files)

    def server_time_zone(self):
        my_time_zone = str(datetime.datetime.now(datetime.timezone(datetime.timedelta(0))).astimezone().tzinfo)
        if not my_time_zone == 'UTC':
            my_time_zone = 'GMT'+my_time_zone
        Config.templateRenderingDict['server_time_zone'] = my_time_zone

    def rdbmProperties(self):
        self.server_time_zone()
        Config.rdbm_password_enc = self.obscure(Config.rdbm_password)
        self.renderTemplateInOut(Config.jansRDBMProperties, Config.templateFolder, Config.configFolder)

    def create_folders(self):
        self.createDirs(Config.static_rdbm_dir)

    def installed(self):
        # to be implemented
        return True
