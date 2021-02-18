import os
import re
import sys
import datetime

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
        self.create_indexes()
        self.import_ldif()
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

    def create_tables(self, jans_schema_files):

        tables = []

        for jans_schema_fn in jans_schema_files:
            jans_schema = base.readJsonFile(jans_schema_fn)

            for obj in jans_schema['objectClasses']:
                sql_tbl_name = obj['names'][0]
                sql_tbl_cols = []

                for attrname in obj['may']:
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
                    col_def = '`{}` {}'.format(attrname, data_type)
                    sql_tbl_cols.append(col_def)

                if self.dbUtils.table_exists(sql_tbl_name):
                    for tbl_col in sql_tbl_cols:
                        sql_cmd = 'ALTER TABLE `{}` ADD {};'.format(sql_tbl_name, tbl_col)
                        self.dbUtils.exec_rdbm_query(sql_cmd)
                        tables.append(sql_cmd)
                else:
                    sql_cmd = 'CREATE TABLE `{}` (`id` int NOT NULL auto_increment, `doc_id` VARCHAR(48) NOT NULL UNIQUE, `objectClass` VARCHAR(48), dn VARCHAR(128), {}, PRIMARY KEY  (`id`, `doc_id`));'.format(sql_tbl_name, ', '.join(sql_tbl_cols))
                    self.dbUtils.exec_rdbm_query(sql_cmd)
                    tables.append(sql_cmd)

        self.writeFile(os.path.join(self.output_dir, 'jans_tables.sql'), '\n'.join(tables))

    def create_indexes(self):
        indexes = []

        sql_indexes_fn = os.path.join(Config.static_rdbm_dir, 'sql_index.json')
        sql_indexes = base.readJsonFile(sql_indexes_fn)

        for table in sql_indexes[Config.rdbm_type]:
            for field in sql_indexes[Config.rdbm_type][table]['fields']:
                sql_cmd = 'ALTER TABLE {0}.{1} ADD INDEX `{1}_{2}` (`{3}`);'.format(
                                Config.rdbm_db,
                                table,
                                re.sub(r'[^0-9a-zA-Z\s]+','_', field),
                                field
                                )
                self.dbUtils.exec_rdbm_query(sql_cmd)
                indexes.append(sql_cmd)

            for i, custom in enumerate(sql_indexes[Config.rdbm_type][table]['custom']):
                sql_cmd = 'ALTER TABLE {0}.{1} ADD INDEX `{1}_{2}`(({3}));'.format(
                    Config.rdbm_db,
                    table,
                    i,
                    custom
                    )
                self.dbUtils.exec_rdbm_query(sql_cmd)
                indexes.append(sql_cmd)

        self.writeFile(os.path.join(self.output_dir, 'jans_indexes.sql'), '\n'.join(indexes))


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
