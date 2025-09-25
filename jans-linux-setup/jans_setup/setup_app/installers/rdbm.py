import os
import re
import io
import sys
import time
import sqlalchemy
import shutil
import random
import glob
import tempfile

from pathlib import Path
from string import Template
from setup_app import paths
from setup_app.static import AppType, InstallOption
from setup_app.config import Config
from setup_app.utils import base
from setup_app.static import InstallTypes, BackendTypes
from setup_app.installers.base import BaseInstaller
from setup_app.utils.setup_utils import SetupUtils
from setup_app.utils.package_utils import packageUtils

class RDBMInstaller(BaseInstaller, SetupUtils):

    source_files = []

    def __init__(self):
        setattr(base.current_app, self.__class__.__name__, self)
        self.needdb = False # we will connect later
        self.service_name = 'rdbm-server'
        self.app_type = AppType.APPLICATION
        self.install_type = InstallOption.OPTONAL
        self.install_var = 'rdbm_install'
        self.register_progess()
        self.output_dir = os.path.join(Config.output_dir, Config.rdbm_type)
        opendj_schema_desc_fn = os.path.join(Config.install_dir, 'schema/opendj_schema_descriptions.json')
        self.opendj_schema_descriptions = base.readJsonFile(opendj_schema_desc_fn)

    @property
    def qchar(self):
        return '`' if Config.rdbm_type in ('mysql',) else '"'

    def install(self):

        self.local_install()
        if Config.rdbm_install_type == InstallTypes.REMOTE:
            if base.argsp.reset_rdbm_db:
                self.reset_rdbm_db()

        self.prepare_jans_attributes()
        self.create_tables(Config.schema_files)
        self.import_ldif()
        self.create_indexes()
        self.create_unique_indexes()
        self.rdbmProperties()

    def prepare_jans_attributes(self):
        self.jans_attributes = []
        for schema_full_path in Config.schema_files:
            schema_ = base.readJsonFile(schema_full_path)
            self.jans_attributes += schema_.get('attributeTypes', [])

    def reset_rdbm_db(self):
        self.logIt("Resetting DB {}".format(Config.rdbm_db))
        self.dbUtils.metadata.reflect(self.dbUtils.engine)
        self.dbUtils.metadata.drop_all(self.dbUtils.engine)
        self.dbUtils.session.commit()
        self.dbUtils.metadata.clear()

    def fix_unit_file(self, service_name):
        unit_fn_ = self.run(['systemctl', 'show', '-P', 'FragmentPath', service_name])
        if unit_fn_ and unit_fn_.strip():
            unit_fn = unit_fn_.strip()
            if os.path.exists(unit_fn):
                unit_file_content = self.readFile(unit_fn.strip())
                unit_file_content_list = unit_file_content.splitlines()
                unit_content = False

                for i, l in enumerate(unit_file_content_list[:]):
                    if l.strip().lower() == '[unit]':
                        unit_content = True
                    if not l.strip() and unit_content:
                        unit_file_content_list.insert(i, 'Before=jans-auth.service')
                        break

                unit_file_content_list.append('')
                self.writeFile(unit_fn, '\n'.join(unit_file_content_list))
                self.run(['systemctl', 'daemon-reload'])


    def get_rdbm_pw(self):
        pws =  str(random.randint(10,99)) + random.choice('*_.<->') + self.getPW()
        pwsl = [s for s in pws]
        random.shuffle(pwsl)
        return ''.join(pwsl)

    def local_install(self):
        if not Config.rdbm_password:
            Config.rdbm_password = self.get_rdbm_pw()
        if not Config.rdbm_user:
            Config.rdbm_user = 'jans'

        if Config.rdbm_install_type == InstallTypes.LOCAL:
            base.argsp.n = True
            packageUtils.check_and_install_packages()

            if Config.rdbm_type == 'mysql':
                if base.os_type == 'suse':
                    self.restart('mysql')
                    self.fix_unit_file('mysql')
                    self.enable('mysql')
                    Config.backend_service = 'mysql.service'
                    for l in open('/var/log/mysql/mysqld.log'):
                        if 'A temporary password is generated for' in l:
                            n = l.find('root@localhost:')
                            mysql_tmp_root_passwd = l[n+15:].strip()
                            break
                    Config.mysql_root_password = self.get_rdbm_pw()
                    self.run(f'''mysql -u root -p'{mysql_tmp_root_passwd}' -e "ALTER USER 'root'@'localhost' IDENTIFIED BY '{Config.mysql_root_password}'" --connect-expired-password''', shell=True)

                elif base.clone_type == 'rpm':
                    self.restart('mysqld')
                    self.enable('mysqld')
                    Config.backend_service = 'mysqld.service'

                else:
                    Config.backend_service = 'mysql.service'

                result, conn = self.dbUtils.sqlconnection(log=False)
                user_passwd_str = f"-u root -p'{Config.mysql_root_password}' " if base.os_type == 'suse' else ''
                if not result:
                    sql_cmd_list = [
                        "CREATE DATABASE {}".format(Config.rdbm_db),
                        "CREATE USER '{}'@'localhost' IDENTIFIED BY '{}'".format(Config.rdbm_user, Config.rdbm_password),
                        "GRANT ALL PRIVILEGES ON {}.* TO '{}'@'localhost'".format(Config.rdbm_db, Config.rdbm_user),
                        ]
                    for cmd in sql_cmd_list:
                        self.run(f'mysql {user_passwd_str}-e "{cmd}"', shell=True)

                self.mysql_config()

                self.stop(Config.backend_service)
                self.start(Config.backend_service)
                self.enable(Config.backend_service)

            elif Config.rdbm_type == 'pgsql':
                if base.clone_type == 'rpm':
                    self.run(['postgresql-setup', 'initdb'])
                    Config.backend_service = 'postgresql.service'
                elif base.clone_type == 'deb':
                    Config.backend_service = 'postgresql.service'

                self.restart('postgresql')

                cmd_create_db = '''su - postgres -c "psql -U postgres -d postgres -c \\"CREATE DATABASE {};\\""'''.format(Config.rdbm_db)
                cmd_create_user = '''su - postgres -c "psql -U postgres -d postgres -c \\"CREATE USER {} WITH PASSWORD '{}';\\""'''.format(Config.rdbm_user, Config.rdbm_password)
                cmd_grant_previlages = '''su - postgres -c "psql -U postgres -d postgres -c \\"GRANT ALL PRIVILEGES ON DATABASE {} TO {};\\""'''.format(Config.rdbm_db, Config.rdbm_user)
                cmd_alter_db = f'''su - postgres -c "psql -U postgres -d postgres -c \\"ALTER DATABASE {Config.rdbm_db} OWNER TO {Config.rdbm_user};\\""'''

                for cmd in (cmd_create_db, cmd_create_user, cmd_grant_previlages, cmd_alter_db):
                    self.run(cmd, shell=True)

                self.postgresql_config()

                self.stop('postgresql')
                self.start('postgresql')
                self.enable('postgresql')

        self.dbUtils.bind(force=True)

    def mysql_config(self):
        if base.os_type == 'suse':
            conf_file = '/etc/my.cnf'
        elif base.clone_type == 'rpm':
            conf_file = '/etc/my.cnf.d/mysql-server.cnf'
        else:
            conf_file = '/etc/mysql/mysql.conf.d/mysqld.cnf'

        # enforce SSL
        conf_file_s = self.readFile(conf_file)
        conf_file_content = conf_file_s.splitlines()
        ssl_key_s = 'require_secure_transport'
        for i, l in enumerate(conf_file_content):
            if l.strip().startswith(ssl_key_s):
                conf_file_content[i] = f'{ssl_key_s} = ON'
                break
        else:
            conf_file_content.append(f'{ssl_key_s} = ON')

        self.writeFile(conf_file, '\n'.join(conf_file_content))

        mysql_data1_dir = '/var/lib/mysql'
        cert_fn = os.path.join(mysql_data1_dir, 'ca.pem')
        self.import_rootcert(cert_fn)


    def postgresql_config(self):

        hba_file_path_query = self.run('''su - postgres -c "psql -U postgres -d postgres -t -c \\"SHOW hba_file;\\""''', shell=True)
        if hba_file_path_query and hba_file_path_query.strip():
            hba_file_path = hba_file_path_query.strip()

            # Allow SSL connection from localhost
            hba_file_content_s = self.readFile(hba_file_path)
            hba_file_content = hba_file_content_s.splitlines()

            host_ssl_config = False
            for i, l in enumerate(hba_file_content):
                ls = l.strip()
                if ls.startswith('#'):
                    continue
                method_list = ls.split()
                if method_list:
                    if method_list[0] in ('host', 'hostssl'):
                        if method_list[1] == Config.rdbm_db and method_list[2] == Config.rdbm_user and method_list[4] == 'scram-sha-256':
                            host_ssl_config = True
                            continue
                        if method_list[1] in ('all', Config.rdbm_db):
                            hba_file_content[i] = '#' + ls

            if not host_ssl_config:
                # get password encryption type
                cmd_pwd_enc = '''su - postgres -c "psql -U postgres -d postgres -At -c \\"SHOW password_encryption;\\""'''
                std_out = self.run(cmd_pwd_enc, shell=True)
                password_encryption_type = std_out.strip()
                hba_file_content.append('\n# Added by Janssen setup')
                hba_file_content.append(f'hostssl    {Config.rdbm_db}    {Config.rdbm_user}    127.0.0.1/32    {password_encryption_type}')
                hba_file_content.append(f'hostssl    {Config.rdbm_db}    {Config.rdbm_user}    ::1/128    {password_encryption_type}')

            hba_file_content.append('')

            self.writeFile(hba_file_path, '\n'.join(hba_file_content))

            path_obj = Path(hba_file_path)
            conf_dir = path_obj.parent.as_posix()
            key_fn, crt_fn = self.gen_ca(ca_suffix='postgresql', cert_dir=conf_dir)
            self.import_rootcert(crt_fn)

            for fn in (key_fn, crt_fn):
                self.chown(fn, 'postgres', 'postgres')
                self.run([paths.cmd_chmod, '600', fn])

            conf_file = os.path.join(conf_dir, 'postgresql.conf')
            conf_file_s = self.readFile(conf_file)
            conf_file_content = conf_file_s.splitlines()
            key_value_dict = {'ssl': 'on', 'ssl_cert_file': crt_fn, 'ssl_key_file': key_fn}
            conf_status =  {k: False for k in key_value_dict}

            for i, l in enumerate(conf_file_content):
                if l.strip().startswith('#'):
                    continue
                n = l.find('=')
                if n > -1:
                    skey = l[:n-1].strip()
                    if skey in key_value_dict:
                        conf_file_content[i] = f"{skey} = '{key_value_dict[skey]}'"
                        conf_status[skey] = True

            for skey in conf_status:
                if not conf_status[skey]:
                    conf_file_content.append(f"{skey} = '{key_value_dict[skey]}'")

            self.writeFile(conf_file, '\n'.join(conf_file_content))

    def import_rootcert(self, cert_fn):
        self.import_cert_into_keystore(cert_fn, f'jans_{Config.rdbm_type}')

    def get_sql_col_type(self, attrname, table=None):

        if attrname in self.dbUtils.sql_data_types:
            type_ = self.dbUtils.sql_data_types.get(f'{table}:{attrname}',{}).get(Config.rdbm_type) or self.dbUtils.sql_data_types.get(f'{table}:{attrname}',{}).get('mysql') or self.dbUtils.sql_data_types[attrname].get(Config.rdbm_type) or self.dbUtils.sql_data_types[attrname]['mysql']
            if table in type_.get('tables', {}):
                type_ = type_['tables'][table]
            if 'size' in type_:
                data_type = '{}({})'.format(type_['type'], type_['size'])
            else:
                data_type = type_['type']

        elif self.dbUtils.is_schema_rdbm_json(attrname):
            return self.dbUtils.rdbm_json_types[Config.rdbm_type]['type']

        else:
            attr_syntax = self.dbUtils.get_attr_syntax(attrname)
            type_ = self.dbUtils.ldap_sql_data_type_mapping[attr_syntax].get(Config.rdbm_type) or self.dbUtils.ldap_sql_data_type_mapping[attr_syntax]['mysql']

            char_type = 'VARCHAR'

            if type_['type'] in char_type:
                if type_['size'] <= 127:
                    data_type = '{}({})'.format(char_type, type_['size'])
                elif type_['size'] <= 255:
                    data_type = 'TINYTEXT' if Config.rdbm_type == 'mysql' else 'TEXT'
                else:
                    data_type = 'TEXT'
            else:
                data_type = type_['type']

        return data_type


    def get_attr_description(self, attrname):
        for attrib in self.dbUtils.jans_attributes:
            if attrname in attrib['names']:
                return attrib.get('desc')

        return self.opendj_schema_descriptions.get(attrname, '')

    def get_col_def(self, attrname, sql_tbl_name):
        data_type = self.get_sql_col_type(attrname, sql_tbl_name)
        col_def = '{0}{1}{0} {2}'.format(self.qchar, attrname, data_type)

        if Config.rdbm_type == 'mysql':
            desc = self.get_attr_description(attrname)
            if desc:
                col_def += ' COMMENT "{}"'.format(desc)

        if Config.rdbm_type == 'mysql' and self.dbUtils.mariadb and data_type == 'JSON':
            col_def += ', CONSTRAINT {0}{1}{0} CHECK (JSON_VALID({0}{1}{0}))'.format(self.qchar, attrname)

        return col_def

    def create_tables(self, jans_schema_files):
        self.logIt("Creating tables for {}".format(jans_schema_files))
        tables = []
        all_schema = {}
        all_attribs = {}
        alter_table_sql_cmd = 'ALTER TABLE %s{}%s ADD {};' % (self.qchar, self.qchar)

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

            for s in obj['sup']:
                if s == 'top':
                    continue
                attr_list += all_schema[s]['may']

            cols_ = []
            col_comments = []
            for attrname in attr_list:
                if attrname in cols_:
                    continue

                cols_.append(attrname)
                col_def = self.get_col_def(attrname, sql_tbl_name) 
                sql_tbl_cols.append(col_def)

                if Config.rdbm_type == 'pgsql':
                    desc = self.get_attr_description(attrname)
                    if desc:
                        col_comments.append('''COMMENT ON COLUMN "{}"."{}" IS '{}';'''.format(sql_tbl_name, attrname, desc))

            if not self.dbUtils.table_exists(sql_tbl_name):
                doc_id_type = self.get_sql_col_type('doc_id', sql_tbl_name)
                if Config.rdbm_type == 'pgsql':
                    sql_cmd = 'CREATE TABLE "{}" (doc_id {} NOT NULL UNIQUE, "objectClass" VARCHAR(48), dn VARCHAR(128), {}, PRIMARY KEY (doc_id));'.format(sql_tbl_name, doc_id_type, ', '.join(sql_tbl_cols))
                else:
                    sql_cmd = 'CREATE TABLE `{}` (`doc_id` {} NOT NULL UNIQUE, `objectClass` VARCHAR(48), dn VARCHAR(128), {}, PRIMARY KEY (`doc_id`));'.format(sql_tbl_name, doc_id_type, ', '.join(sql_tbl_cols))
                self.dbUtils.exec_rdbm_query(sql_cmd)
                
                for comment_sql in col_comments:
                    self.dbUtils.exec_rdbm_query(comment_sql)
                    tables.append(comment_sql)

                tables.append(sql_cmd)

        for attrname in all_attribs:
            attr = all_attribs[attrname]
            if attr.get('sql', {}).get('add_table'):
                col_def = self.get_col_def(attrname, sql_tbl_name)
                sql_cmd = alter_table_sql_cmd.format(attr['sql']['add_table'], col_def)
                self.dbUtils.exec_rdbm_query(sql_cmd)
                tables.append(sql_cmd)

        self.writeFile(os.path.join(self.output_dir, 'jans_tables.sql'), '\n'.join(tables))


    def get_index_name(self, attrname):
        return re.sub(r'[^0-9a-zA-Z\s]+','_', attrname)


    def create_indexes(self):

        indexes = []

        sql_indexes_fn = os.path.join(Config.static_rdbm_dir, Config.rdbm_type + '_index.json')
        sql_indexes = base.readJsonFile(sql_indexes_fn)

        # read opendj indexes and add multivalued attributes to JSON indexing
        opendj_index = base.readJsonFile(os.path.join(Config.static_rdbm_dir, 'opendj_index.json'))
        opendj_index_list = [ atribute['attribute'] for atribute in opendj_index ]

        for attribute in self.jans_attributes:
            if attribute.get('multivalued'):
                for attr_name in attribute['names']:
                    if attr_name in opendj_index_list and attr_name not in sql_indexes['__common__']['fields']:
                        sql_indexes['__common__']['fields'].append(attr_name)

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
                        key_lenght = ''
                        if self.get_sql_col_type(attr.name, tblCls) == 'TEXT' and attr.name in sql_indexes.get(tblCls, {}).get('fields', []) + sql_indexes['__common__']['fields']:
                            key_lenght = '(255)'

                        sql_cmd = 'ALTER TABLE {0}.{1} ADD INDEX `{1}_{2}` (`{3}`{4});'.format(
                                    Config.rdbm_db,
                                    tblCls,
                                    ind_name,
                                    attr.name,
                                    key_lenght
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

    def create_unique_indexes(self):
        #Create uniqueness for columns jansPerson.uid and jansPerson.mail
        for table, column in (('jansPerson', 'mail'), ('jansPerson', 'uid')):
            if Config.rdbm_type in ('mysql',):
                sql_cmd = f'CREATE UNIQUE INDEX `{table.lower()}_{column.lower()}_unique_idx` ON `{table}` (`{column}`)'
            elif Config.rdbm_type == 'pgsql':
                sql_cmd = f'CREATE UNIQUE INDEX {table.lower()}_{column.lower()}_unique_idx ON "{table}"("{column}")'
            self.dbUtils.exec_rdbm_query(sql_cmd)


    def import_ldif(self):
        ldif_files = Config.ldif_files[:]

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

            self.dbUtils.import_ldif([Config.ldif_base], force=force)

        self.dbUtils.import_ldif(ldif_files)

    def rdbmProperties(self):

        pgsql_mysql_ssl_modes_mapping = {
            'disable': 'DISABLED',
            'require': 'REQUIRED',
            'verify-ca': 'VERIFY_CA',
            'verify-full': 'VERIFY_IDENTITY',
            }

        def set_sslmode(mode='verify-ca'):

            if mode in ('verify-ca', 'verify-full'):
                Config.rdbm_sslfactory = 'org.postgresql.ssl.DefaultJavaSSLFactory'

            if Config.rdbm_type == 'pgsql':
                Config.rdbm_sslmode = mode
            elif Config.rdbm_type == 'mysql':
                Config.rdbm_sslmode = pgsql_mysql_ssl_modes_mapping[mode]

        if Config.rdbm_install_type == InstallTypes.LOCAL:
            set_sslmode()

        if Config.rdbm_install_type == InstallTypes.REMOTE:
            if Config.get('rdbm_sslrootcert'):
                with tempfile.NamedTemporaryFile('w') as tmpfo:
                    tmpfo.write(Config.rdbm_sslrootcert)
                    tmpfo.flush()
                    self.import_rootcert(tmpfo.name)
                set_sslmode()
            else:
                set_sslmode('disable')

        Config.rdbm_enable_ssl = 'false' if Config.rdbm_sslmode == 'disable' else 'true' 

        Config.set_rdbm_schema()
        if Config.rdbm_type in ('pgsql', 'mysql'):
            Config.rdbm_password_enc = self.obscure(Config.rdbm_password)
            src_temp_fn = os.path.join(Config.templateFolder, 'jans-{}.properties'.format(Config.rdbm_type))
            targtet_fn = os.path.join(Config.configFolder, Config.jansRDBMProperties)
            rendered_tmp = self.render_template(src_temp_fn)
            self.writeFile(targtet_fn, rendered_tmp)


    def create_folders(self):
        self.createDirs(Config.static_rdbm_dir)


    def installed(self):
        # to be implemented
        return True

