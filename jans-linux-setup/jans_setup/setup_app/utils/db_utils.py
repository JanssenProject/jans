import warnings
import sys
import os
import re
import json
import logging
import copy
import hashlib
import pymysql
import time

from types import MappingProxyType
from setup_app.pylib.parse_dn import parse_dn
from pathlib import PurePath

warnings.filterwarnings("ignore")


from setup_app import static
from setup_app.config import Config
from setup_app.static import InstallTypes, BackendTypes, colors, SearchScopes
from setup_app.utils import base
from setup_app.utils import ldif_utils
from setup_app.utils.attributes import attribDataTypes

my_path = PurePath(os.path.dirname(os.path.realpath(__file__)))
sys.path.append(my_path.parent.joinpath('pylib/sqlalchemy'))


import sqlalchemy
import sqlalchemy.orm
import sqlalchemy.ext.automap

SCRIPTS_DN_TMP = 'inum={},ou=scripts,o=jans'

class DBUtils:

    processedKeys = []
    Base = None
    session = None
    cbm = None
    mariadb = False
    rdbm_json_types = MappingProxyType({ 'mysql': {'type': 'JSON'}, 'pgsql': {'type': 'JSONB'} })
    jans_scopes = None

    def bind(self, force=False):

        setattr(base.current_app, self.__class__.__name__, self)
        self.mariadb = None
        base.logIt("Bind to database")

        logging.basicConfig(
                filename=os.path.join(Config.install_dir, 'logs', Config.rdbm_type + '.log'),
                level=logging.DEBUG,
                format='%(asctime)s %(levelname)s - %(message)s'
                )

        self.read_jans_schema()

        if not self.session or force:
            if Config.rdbm_type in ('mysql', 'pgsql'):
                base.logIt("Making {} Conncetion".format(Config.rdbm_type))
                result = self.sqlconnection()
                if not result[0]:
                    print("{}FATAL: {}{}".format(colors.FAIL, result[1], colors.ENDC))
                    sys.exit()

    def sqlconnection(self, log=True):
        base.logIt("Making {} Connection to {}:{}/{} with user {}".format(Config.rdbm_type.upper(), Config.rdbm_host, Config.rdbm_port, Config.rdbm_db, Config.rdbm_user))

        db_str = 'mysql+pymysql' if Config.rdbm_type == 'mysql' else 'postgresql+psycopg2'

        bind_uri = '{}://{}:{}@{}:{}/{}'.format(
                        db_str,
                        Config.rdbm_user,
                        Config.rdbm_password,
                        Config.rdbm_host,
                        Config.rdbm_port,
                        Config.rdbm_db,
                )

        if Config.rdbm_type == 'mysql':
            bind_uri += '?charset=utf8mb4'

        try:
            self.engine = sqlalchemy.create_engine(bind_uri)
            logging.basicConfig(filename=os.path.join(Config.install_dir, 'logs/sqlalchemy.log'))
            logging.getLogger('sqlalchemy.engine').setLevel(logging.INFO)
            Session = sqlalchemy.orm.sessionmaker(bind=self.engine)
            self.session = Session()
            self.metadata = sqlalchemy.MetaData()
            self.session.connection()

            base.logIt("{} Connection was successful".format(Config.rdbm_type.upper()))
            if Config.rdbm_type == 'mysql':
                self.set_mysql_version()
            return True, self.session

        except Exception as e:
            if log:
                base.logIt("Can't connect to {} server: {}".format(Config.rdbm_type.upper(), str(e), True))
            return False, e


    def set_mysql_version(self):
        # are we on MariDB?
        version_query = self.engine.execute(sqlalchemy.text('SELECT VERSION()'))
        version_query_result = version_query.fetchone()
        if version_query_result:
            self.mariadb = 'mariadb' in version_query_result[0].lower()

    @property
    def json_dialects_instance(self):
        return sqlalchemy.dialects.mysql.json.JSON if Config.rdbm_type == 'mysql' else sqlalchemy.dialects.postgresql.json.JSONB

    def read_jans_schema(self, others=[]):
        self.jans_attributes = []

        for schema_fn in Config.schema_files + others:
            schema = base.readJsonFile(schema_fn)
            self.jans_attributes += schema['attributeTypes']

        self.ldap_sql_data_type_mapping = base.readJsonFile(os.path.join(Config.static_rdbm_dir, 'ldap_sql_data_type_mapping.json'))
        self.sql_data_types = base.readJsonFile(os.path.join(Config.static_rdbm_dir, 'sql_data_types.json'))
        self.opendj_attributes_syntax = base.readJsonFile(os.path.join(Config.static_rdbm_dir, 'opendj_attributes_syntax.json'))

        for attr in attribDataTypes.listAttributes:
            if not attr in self.sql_data_types:
                self.sql_data_types[attr] = self.rdbm_json_types

    def exec_rdbm_query(self, query, getresult=False):
        base.logIt("Executing {} Query: {}".format(Config.rdbm_type, query))
        if Config.rdbm_type in ('mysql', 'pgsql'):
            try:
                qresult = self.session.execute(query)
                self.session.commit()
            except Exception as e:
                base.logIt("ERROR executing query {}".format(e.args))
                base.logIt("ERROR executing query {}".format(e.args), True)
            else:
                if getresult == 1:
                    return qresult.first()
                elif getresult:
                    return qresult.fetchall()

    def get_jans_auth_conf_dynamic(self):
        if Config.rdbm_type in ('mysql', 'pgsql'):
            result = self.search(search_base='ou=jans-auth,ou=configuration,o=jans', search_filter='(objectClass=jansAppConf)', search_scope=SearchScopes.BASE)
            dn = result['dn'] 
            jans_auth_conf_dynamic = json.loads(result['jansConfDyn'])

        return dn, jans_auth_conf_dynamic


    def set_jans_auth_conf_dynamic(self, entries):
        if Config.rdbm_type in ('mysql', 'pgsql'):
            dn, jans_auth_conf_dynamic = self.get_jans_auth_conf_dynamic()
            jans_auth_conf_dynamic.update(entries)
            sqlalchemyObj = self.get_sqlalchObj_for_dn(dn)
            sqlalchemyObj.jansConfDyn = json.dumps(jans_auth_conf_dynamic, indent=2)
            self.session.commit()


    def enable_script(self, inum, enable=True):
        base.logIt("Enabling script {}".format(inum))
        if Config.rdbm_type in ('mysql', 'pgsql'):
            dn = SCRIPTS_DN_TMP.format(inum)
            sqlalchemyObj = self.get_sqlalchObj_for_dn(dn)
            sqlalchemyObj.jansEnabled = 1 if enable else 0
            self.session.commit()


    def enable_service(self, service):
        if Config.rdbm_type in ('mysql', 'pgsql'):
            sqlalchemyObj = self.get_sqlalchObj_for_dn('ou=configuration,o=jans')
            setattr(sqlalchemyObj, service, 1)
            self.session.commit()


    def set_configuration(self, component, value, dn='ou=configuration,o=jans'):
        if Config.rdbm_type in ('mysql', 'pgsql'):
            typed_val = self.get_rdbm_val(component, value)
            result = self.get_sqlalchObj_for_dn(dn)
            table_name = result.objectClass
            sqlalchemy_table = self.Base.classes[table_name]
            sqlalchemyObj = self.session.query(sqlalchemy_table).filter(sqlalchemy_table.dn ==dn).first()
            cur_val = getattr(sqlalchemyObj, component)
            setattr(sqlalchemyObj, component, typed_val)
            self.session.commit()


    def dn_exists(self, dn):
        mapping_location = self.get_backend_location_for_dn(dn)

        if mapping_location in (BackendTypes.MYSQL, BackendTypes.PGSQL):
            base.logIt("Querying RDBM for dn {}".format(dn))
            result = self.get_sqlalchObj_for_dn(dn)
            if result:
                return result.__dict__
            return

        else:
            bucket = self.get_bucket_for_dn(dn)
            key = ldif_utils.get_key_from(dn)
            n1ql = 'SELECT * FROM `{}` USE KEYS "{}"'.format(bucket, key)
            result = self.cbm.exec_query(n1ql)
            if result.ok:
                data = result.json()
                if data.get('results'):
                    return data['results'][0][bucket]
            return


    def dn_exists_rdbm(self, dn, table):
        base.logIt("Checking dn {} exists in table {}".format(dn, table))
        backend_location = self.get_backend_location_for_dn(dn)
        sqlalchemy_table = self.Base.classes[table].__table__

        return self.session.query(sqlalchemy_table).filter(sqlalchemy_table.columns.dn == dn).first()


    def search(self, search_base, search_filter='(objectClass=*)', search_scope=SearchScopes.LEVEL, fetchmany=False):
        base.logIt("Searching database for dn {} with filter {}".format(search_base, search_filter))
        backend_location = self.get_backend_location_for_dn(search_base)

        if backend_location in (BackendTypes.MYSQL, BackendTypes.PGSQL):
            if self.Base is None:
                self.rdm_automapper()

            s_table = None
            where_clause = ''
            search_list = []

            if '&' in search_filter:
                re_match = re.match('\(&\((.*?)=(.*?)\)\((.*?)=(.*?)\)', search_filter)
                if re_match:
                    re_list = re_match.groups()
                    search_list.append((re_list[0], re_list[1]))
                    search_list.append((re_list[2], re_list[3]))
            else:
                re_match = re.match('\((.*?)=(.*?)\)', search_filter)

                if re_match:
                    re_list = re_match.groups()
                    search_list.append((re_list[0], re_list[1]))


            for col, val in search_list:
                if col.lower() == 'objectclass':
                    s_table = val
                    break

            if not s_table:
                return

            sqlalchemy_table = self.Base.classes[s_table]
            sqlalchemyQueryObject = self.session.query(sqlalchemy_table)

            for col, val in search_list:
                if val == '*':
                    continue

                if col.lower() != 'objectclass':
                    val = val.replace('*', '%')
                    sqlalchemyCol = getattr(sqlalchemy_table, col)
                    if '%' in val:
                        sqlalchemyQueryObject = sqlalchemyQueryObject.filter(sqlalchemyCol.like(val))
                    else:
                        sqlalchemyQueryObject = sqlalchemyQueryObject.filter(sqlalchemyCol == val)

            if search_scope == SearchScopes.BASE:
                sqlalchemyQueryObject = sqlalchemyQueryObject.filter(sqlalchemy_table.dn == search_base)
            else:
                sqlalchemyQueryObject = sqlalchemyQueryObject.filter(sqlalchemy_table.dn.like('%'+search_base))

            if fetchmany:
                result = sqlalchemyQueryObject.all()
                return [ (ldif_utils.get_key_from(item.dn), item.__dict__) for item in result ]

            else:
                result = sqlalchemyQueryObject.first()
                if result:
                    return result.__dict__



    def add2strlist(self, client_id, strlist):
        value2 = []
        for v in strlist.split(','):
            if v.strip():
                value2.append(v.strip())
        value2.append(client_id)

        return  ','.join(value2)


    def get_backend_location_for_dn(self, dn):
        if Config.rdbm_type == 'mysql':
            return static.BackendTypes.MYSQL
        elif Config.rdbm_type == 'pgsql':
            return static.BackendTypes.PGSQL

    def delete_dn(self, dn):
        if self.dn_exists(dn):
            backend_location = self.get_backend_location_for_dn(dn)

            if backend_location in (BackendTypes.MYSQL, BackendTypes.PGSQL):
                sqlalchemy_obj = self.get_sqlalchObj_for_dn(dn)
                if sqlalchemy_obj:
                    self.session.delete(sqlalchemy_obj)
                    self.session.commit()

    def add_client2script(self, script_inum, client_id):
        dn = SCRIPTS_DN_TMP.format(script_inum)

        backend_location = self.get_backend_location_for_dn(dn)

        if backend_location in (BackendTypes.MYSQL, BackendTypes.PGSQL):
            sqlalchemyObj = self.get_sqlalchObj_for_dn(dn)
            if sqlalchemyObj:
                if sqlalchemyObj.jansConfProperty:
                    jans_conf_property = copy.deepcopy(sqlalchemyObj.jansConfProperty)
                else:
                    jans_conf_property = []

                for i, oxconfigprop in enumerate(jans_conf_property[:]):
                    if isinstance(oxconfigprop, str):
                        oxconfigprop = json.loads(oxconfigprop)
                    if oxconfigprop.get('value1') == 'allowed_clients' and client_id not in oxconfigprop['value2']:
                        oxconfigprop['value2'] = self.add2strlist(client_id, oxconfigprop['value2'])
                        jans_conf_property[i] = json.dumps(oxconfigprop)
                        break
                else:
                    jans_conf_property.append(json.dumps({'value1': 'allowed_clients', 'value2': client_id}))

                sqlalchemyObj.jansConfProperty = jans_conf_property
                self.session.commit()


    def get_key_prefix(self, key):
        n = key.find('_')
        return key[:n+1]


    def get_attr_syntax(self, attrname):
        for jans_attr in self.jans_attributes:
            if attrname in jans_attr['names']:
                if jans_attr.get('multivalued'):
                    return 'JSON'
                return jans_attr['syntax']
        else:
            opendj_syntax = self.opendj_attributes_syntax.get(attrname)
            if opendj_syntax is None:
                opendj_syntax = '1.3.6.1.4.1.1466.115.121.1.15'

            return opendj_syntax

    def get_rootdn(self, dn):
        dn_parsed = parse_dn(dn)
        dn_parsed.pop(0)
        dnl=[]

        for dnp in dn_parsed:
            dnl.append('='.join(dnp[:2]))

        return ','.join(dnl)


    def rdm_automapper(self, force=False):
        if not force and self.Base:
            return

        base.logIt("Reflecting ORM tables")

        self.metadata.reflect(self.engine)
        self.Base = sqlalchemy.ext.automap.automap_base(metadata=self.metadata)
        self.Base.prepare()


        # fix JSON type for mariadb
        if Config.rdbm_type == 'mysql' and self.mariadb:
            for tbl in self.Base.classes:
                slq_query = self.engine.execute(sqlalchemy.text('SELECT CONSTRAINT_NAME from INFORMATION_SCHEMA.CHECK_CONSTRAINTS where TABLE_NAME="{}" and CHECK_CLAUSE like "%json_valid%"'.format(tbl.__table__.name)))
                slq_query_result = slq_query.fetchall()
                for col in slq_query_result:
                    tbl.__table__.columns[col[0]].type = sqlalchemy.dialects.mysql.json.JSON()

        base.logIt("Reflected tables {}".format(list(self.metadata.tables.keys())))

    def get_sqlalchObj_for_dn(self, dn):

        for tbl in self.Base.classes:
            result = self.session.query(tbl).filter(tbl.dn == dn).first()
            if result:
                return result

        for tbl in self.Base.classes:
            result = self.session.query(tbl).filter(tbl.dn.like('%'+dn)).first()
            if result:
                return result

    def table_exists(self, table):

        metadata = sqlalchemy.MetaData()
        try:
            metadata.reflect(self.engine, only=[table])
        except:
            pass

        return table in metadata

    def is_schema_rdbm_json(self, attrname):
        for attr in self.jans_attributes:
            if attrname in attr['names']:
                return attr.get('rdbm_json_column')

    def get_attr_sql_data_type(self, key):
        if key in self.sql_data_types:
            data_type = self.sql_data_types[key]
        elif self.is_schema_rdbm_json(key):
            return self.rdbm_json_types[Config.rdbm_type]['type']
        else:
            attr_syntax = self.get_attr_syntax(key)
            data_type = self.ldap_sql_data_type_mapping[attr_syntax]

        data_type = (data_type.get(Config.rdbm_type) or data_type['mysql'])['type']

        return data_type

    def get_rdbm_val(self, key, val, rdbm_type=None):

        data_type = self.get_attr_sql_data_type(key)
        val_ = val[0] if isinstance(val, list) or isinstance(val, tuple) else val

        if data_type in ('SMALLINT', 'BOOL', 'BOOLEAN'):
            if val_.lower() in ('1', 'on', 'true', 'yes', 'ok'):
                return 1 if data_type == 'SMALLINT' else True
            return 0 if data_type == 'SMALLINT' else False

        if data_type == 'INT':
            return int(val_)

        if data_type in ('DATETIME(3)', 'TIMESTAMP'):
            dval = val_.strip('Z')
            sep= ' '
            postfix = ''
            return "{}-{}-{}{}{}:{}:{}{}{}".format(dval[0:4], dval[4:6], dval[6:8], sep, dval[8:10], dval[10:12], dval[12:14], dval[14:17], postfix)

        if data_type in ('JSON', 'JSONB'):
            json_data = []
            for d in val:
                if d and isinstance(d, str):
                    try:
                        d = json.loads(d)
                    except Exception as e:
                        pass
                json_data.append(d)

            return json_data

        if data_type in ('ARRAY<STRING(MAX)>', 'JSONB'):
            return val

        return val_

    def get_clean_objcet_class(self, entry):

        objectClass = entry.get('objectClass') or entry.get('objectclass')

        if objectClass:
            if 'top' in objectClass:
                objectClass.remove('top')
            objectClass = objectClass[-1]

        return objectClass

    def get_doc_id_from_dn(self, dn):
        dn_parsed = parse_dn(dn)
        doc_id = dn_parsed[0][1]
        if doc_id == 'jans':
            doc_id = '_'
        return doc_id


    def get_sha_digest(self, val):
        msha = hashlib.sha256()
        msha.update(val.encode())
        return msha.digest().hex()

    def import_ldif(self, ldif_files, bucket=None, force=None):

        base.logIt("Importing ldif file(s): {} ".format(', '.join(ldif_files)))

        sql_data_fn = os.path.join(Config.output_dir, Config.rdbm_type, 'jans_data.sql')

        for ldif_fn in ldif_files:
            base.logIt("Importing entries from " + ldif_fn)
            parser = ldif_utils.myLdifParser(ldif_fn)
            parser.parse()

            for dn, entry in parser.entries:
                backend_location = force if force else self.get_backend_location_for_dn(dn)

                if backend_location in (BackendTypes.MYSQL, BackendTypes.PGSQL):
                    if self.Base is None:
                        self.rdm_automapper()

                    # TODO: inserting data to sub tables to be implemented for mysql and pgsql

                    if 'add' in  entry and 'changetype' in entry:
                        attribute = entry['add'][0]
                        new_val = entry[attribute]
                        sqlalchObj = self.get_sqlalchObj_for_dn(dn)

                        if sqlalchObj:
                            if isinstance(sqlalchObj.__table__.columns[attribute].type, self.json_dialects_instance):
                                cur_val = copy.deepcopy(getattr(sqlalchObj, attribute))
                                for val_ in new_val:
                                        cur_val.append(val_)
                                setattr(sqlalchObj, attribute, cur_val)
                            else:
                                setattr(sqlalchObj, attribute, new_val[0])

                            self.session.commit()

                        else:
                            base.logIt("Can't find current value for replacement of {}".format(str(entry)), True)
                            continue

                    elif 'replace' in entry and 'changetype' in entry:
                        attribute = entry['replace'][0]
                        new_val = self.get_rdbm_val(attribute, entry[attribute])
                        sqlalchObj = self.get_sqlalchObj_for_dn(dn)

                        if sqlalchObj:
                            setattr(sqlalchObj, attribute, new_val)
                            self.session.commit()
                        else:
                            base.logIt("Can't find current value for replacement of {}".format(str(entry)), True)
                            continue

                    else:
                        vals = {}
                        dn_parsed = parse_dn(dn)
                        rdn_name = dn_parsed[0][0]
                        objectClass = self.get_clean_objcet_class(entry)
                        if objectClass.lower() == 'organizationalunit':
                            continue

                        vals['doc_id'] = self.get_doc_id_from_dn(dn)
                        vals['dn'] = dn
                        vals['objectClass'] = objectClass

                        #entry.pop(rdn_name)
                        if 'objectClass' in entry:
                            entry.pop('objectClass')
                        elif 'objectclass' in entry:
                            entry.pop('objectclass')

                        table_name = objectClass

                        if self.dn_exists_rdbm(dn, table_name):
                            base.logIt("DN {} exsits in {} skipping".format(dn, Config.rdbm_type))
                            continue

                        for lkey in entry:
                            vals[lkey] = self.get_rdbm_val(lkey, entry[lkey])

                        sqlalchCls = self.Base.classes[table_name]

                        for col in sqlalchCls.__table__.columns:
                            if isinstance(col.type, self.json_dialects_instance) and not col.name in vals:
                                vals[col.name] = []

                        sqlalchObj = sqlalchCls()

                        for v in vals:
                            setattr(sqlalchObj, v, vals[v])

                        base.logIt("Adding {}".format(sqlalchObj.doc_id))
                        self.session.add(sqlalchObj)
                        self.session.commit()


    def import_templates(self, templates):

        base.logIt("Importing templates file(s): {} ".format(', '.join(templates)))

        sql_data_fn = os.path.join(Config.output_dir, Config.rdbm_type, 'jans_data.sql')

        for template in templates:
            base.logIt("Importing entries from " + template)
            entries = base.readJsonFile(template)
            for entry in entries:
                dn = entry['dn']
                if self.Base is None:
                    self.rdm_automapper()

                if 'add' in  entry and 'changetype' in entry:
                    attribute = entry['add']
                    new_val = entry[attribute]
                    sqlalchObj = self.get_sqlalchObj_for_dn(dn)

                    if sqlalchObj:
                        if isinstance(sqlalchObj.__table__.columns[attribute].type, self.json_dialects_instance):
                            cur_val = copy.deepcopy(getattr(sqlalchObj, attribute))
                            for val_ in new_val:
                                cur_val.append(val_)
                            setattr(sqlalchObj, attribute, cur_val)
                        else:
                            setattr(sqlalchObj, attribute, new_val)

                        self.session.commit()

                    else:
                        base.logIt("Can't find current value for replacement of {}".format(str(entry)), True)
                        continue

                elif 'replace' in entry and 'changetype' in entry:
                    attribute = entry['replace']
                    sqlalchObj = self.get_sqlalchObj_for_dn(dn)

                    if sqlalchObj:
                        setattr(sqlalchObj, attribute, entry[attribute])
                        self.session.commit()
                    else:
                        base.logIt("Can't find current value for replacement of {}".format(str(entry)), True)
                        continue

                else:
                    vals = {}
                    dn_parsed = parse_dn(dn)
                    rdn_name = dn_parsed[0][0]
                    objectClass = entry.get('objectClass') or entry.get('objectclass')

                    if objectClass and objectClass.lower() == 'organizationalunit':
                        continue

                    vals['doc_id'] = dn_parsed[0][1]
                    vals['objectClass'] = objectClass

                    #entry.pop(rdn_name)
                    if 'objectClass' in entry:
                        entry.pop('objectClass')
                    elif 'objectclass' in entry:
                        entry.pop('objectclass')

                    table_name = objectClass

                    if self.dn_exists_rdbm(dn, table_name):
                        base.logIt("DN {} exsits in {} skipping".format(dn, Config.rdbm_type))
                        continue

                    for lkey in entry:
                        vals[lkey] = entry[lkey]

                    sqlalchCls = self.Base.classes[table_name]

                    for col in sqlalchCls.__table__.columns:
                        if isinstance(col.type, self.json_dialects_instance) and not col.name in vals:
                            vals[col.name] = []

                    sqlalchObj = sqlalchCls()

                    for v in vals:
                        setattr(sqlalchObj, v, vals[v])

                    base.logIt("Adding {}".format(sqlalchObj.doc_id))
                    self.session.add(sqlalchObj)
                    self.session.commit()


    def get_scopes(self):
        result = self.search(
                    search_base='ou=scopes,o=jans',
                    search_filter='(objectClass=jansScope)',
                    search_scope=SearchScopes.LEVEL,
                    fetchmany=True)
        sopes = [ sope for _, sope in result ]

        return sopes

    def get_scope_by_jansid(self, jansid):
        if not self.jans_scopes:
            self.jans_scopes = self.get_scopes()

        for jans_scope in self.jans_scopes:
            if jans_scope['jansId'] == jansid:
                return jans_scope


dbUtils = DBUtils()
