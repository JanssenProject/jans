import warnings
import sys
import os
import re
import json
import logging
import copy
import hashlib
import ldap3
import pymysql
from ldap3.utils import dn as dnutils
from pathlib import PurePath

warnings.filterwarnings("ignore")


from setup_app import static
from setup_app.config import Config
from setup_app.static import InstallTypes, BackendTypes, colors
from setup_app.utils import base
from setup_app.utils.cbm import CBM
from setup_app.utils import ldif_utils
from setup_app.utils.attributes import attribDataTypes
if base.current_app.profile == 'jans':
    from setup_app.utils.spanner import Spanner

my_path = PurePath(os.path.dirname(os.path.realpath(__file__)))
sys.path.append(my_path.parent.joinpath('pylib/sqlalchemy'))


import sqlalchemy
import sqlalchemy.orm
import sqlalchemy.ext.automap


class DBUtils:

    processedKeys = []
    Base = None
    session = None
    cbm = None

    def bind(self, use_ssl=True, force=False):

        setattr(base.current_app, self.__class__.__name__, self)

        base.logIt("Bind to database")

        logging.basicConfig(
                filename=os.path.join(Config.install_dir, 'logs', Config.rdbm_type + '.log'),
                level=logging.DEBUG,
                format='%(asctime)s %(levelname)s - %(message)s'
                )

        if Config.mapping_locations['default'] == 'ldap':
            self.moddb = BackendTypes.LDAP
        elif Config.mapping_locations['default'] == 'rdbm':
            self.read_jans_schema()
            if Config.rdbm_type == 'mysql':
                self.moddb = BackendTypes.MYSQL
            elif Config.rdbm_type == 'pgsql':
                self.moddb = BackendTypes.PGSQL
            elif Config.rdbm_type == 'spanner':
                self.moddb = BackendTypes.SPANNER
                self.spanner = Spanner()
        else:
            self.moddb = BackendTypes.COUCHBASE

        if not hasattr(self, 'ldap_conn') or force:
            for group in Config.mapping_locations:
                if Config.mapping_locations[group] == 'ldap':
                    base.logIt("Making LDAP Conncetion")
                    ldap_server = ldap3.Server(Config.ldap_hostname, port=int(Config.ldaps_port), use_ssl=use_ssl)
                    self.ldap_conn = ldap3.Connection(
                                ldap_server,
                                user=Config.ldap_binddn,
                                password=Config.ldapPass,
                                )
                    base.logIt("Making LDAP Connection to host {}:{} with user {}".format(Config.ldap_hostname, Config.ldaps_port, Config.ldap_binddn))
                    self.ldap_conn.bind()
                    break

        if not self.session or force:
            for group in Config.mapping_locations:
                if Config.mapping_locations[group] == 'rdbm':
                    if Config.rdbm_type in ('mysql', 'pgsql'):
                        base.logIt("Making {} Conncetion".format(Config.rdbm_type))
                        result = self.mysqlconnection()
                        if not result[0]:
                            print("{}FATAL: {}{}".format(colors.FAIL, result[1], colors.ENDC))
                        break

        self.set_cbm()
        self.default_bucket = Config.get('couchbase_bucket_prefix', 'jans')


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
            myconn = self.session.connection()

            base.logIt("{} Connection was successful".format(Config.rdbm_type.upper()))
            
            return True, self.session

        except Exception as e:
            if log:
                base.logIt("Can't connect to {} server: {}".format(Config.rdbm_type.upper(), str(e), True))
            return False, e

    @property
    def json_dialects_instance(self):
        return sqlalchemy.dialects.mysql.json.JSON if Config.rdbm_type == 'mysql' else sqlalchemy.dialects.postgresql.json.JSONB

    def mysqlconnection(self, log=True):
        return self.sqlconnection(log)

    def read_jans_schema(self, others=[]):
        self.jans_attributes = []

        for schema_fn_ in ['jans_schema.json', 'custom_schema.json'] + others:
            schema_fn = schema_fn_ if schema_fn_.startswith('/') else os.path.join(Config.install_dir, 'schema', schema_fn_)
            schema = base.readJsonFile(schema_fn)
            self.jans_attributes += schema['attributeTypes']

        self.ldap_sql_data_type_mapping = base.readJsonFile(os.path.join(Config.static_rdbm_dir, 'ldap_sql_data_type_mapping.json'))
        self.sql_data_types = base.readJsonFile(os.path.join(Config.static_rdbm_dir, 'sql_data_types.json'))
        self.opendj_attributes_syntax = base.readJsonFile(os.path.join(Config.static_rdbm_dir, 'opendj_attributes_syntax.json'))
        self.sub_tables = base.readJsonFile(os.path.join(Config.static_rdbm_dir, 'sub_tables.json'))

        for attr in attribDataTypes.listAttributes:
            if not attr in self.sql_data_types:
                self.sql_data_types[attr] = { 'mysql': {'type': 'JSON'}, 'pgsql': {'type': 'JSONB'}, 'spanner': {'type': 'ARRAY<STRING(MAX)>'} }

    def in_subtable(self, table, attr):
        if table in self.sub_tables[Config.rdbm_type]:
            for stbl in self.sub_tables[Config.rdbm_type][table]:
                if stbl[0] == attr:
                    return True

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
        elif Config.rdbm_type == 'spanner':
            if query.startswith('CREATE TABLE') or query.startswith('ALTER TABLE'):
                self.spanner.create_table(query.strip(';'))
            else:
                return self.spanner.exec_sql(query.strip(';'))

    def set_cbm(self):
        self.cbm = CBM(Config.get('cb_query_node'), Config.get('couchebaseClusterAdmin'), Config.get('cb_password'))

    def get_oxAuthConfDynamic(self):
        if self.moddb == BackendTypes.LDAP:
            self.ldap_conn.search(
                        search_base='ou=jans-auth,ou=configuration,o=jans',
                        search_scope=ldap3.BASE,
                        search_filter='(objectClass=*)',
                        attributes=["jansConfDyn"]
                        )

            dn = self.ldap_conn.response[0]['dn']
            oxAuthConfDynamic = json.loads(self.ldap_conn.response[0]['attributes']['jansConfDyn'][0])

        elif self.moddb in (BackendTypes.MYSQL, BackendTypes.PGSQL, BackendTypes.SPANNER):
            result = self.search(search_base='ou=jans-auth,ou=configuration,o=jans', search_filter='(objectClass=jansAppConf)', search_scope=ldap3.BASE)
            dn = result['dn'] 
            oxAuthConfDynamic = json.loads(result['jansConfDyn'])

        elif self.moddb == BackendTypes.COUCHBASE:
            n1ql = 'SELECT * FROM `{}` USE KEYS "configuration_jans-auth"'.format(self.default_bucket)
            result = self.cbm.exec_query(n1ql)
            js = result.json()
            dn = js['results'][0][self.default_bucket]['dn']
            oxAuthConfDynamic = js['results'][0][self.default_bucket]['jansConfDyn']

        return dn, oxAuthConfDynamic


    def set_oxAuthConfDynamic(self, entries):
        if self.moddb == BackendTypes.LDAP:
            dn, oxAuthConfDynamic = self.get_oxAuthConfDynamic()
            oxAuthConfDynamic.update(entries)

            ldap_operation_result = self.ldap_conn.modify(
                    dn,
                    {"jansConfDyn": [ldap3.MODIFY_REPLACE, json.dumps(oxAuthConfDynamic, indent=2)]}
                    )
            self.log_ldap_result(ldap_operation_result)

        elif self.moddb in (BackendTypes.MYSQL, BackendTypes.PGSQL):
            dn, oxAuthConfDynamic = self.get_oxAuthConfDynamic()
            oxAuthConfDynamic.update(entries)
            sqlalchemyObj = self.get_sqlalchObj_for_dn(dn)
            sqlalchemyObj.jansConfDyn = json.dumps(oxAuthConfDynamic, indent=2)
            self.session.commit()

        elif self.moddb in (BackendTypes.SPANNER,):
            dn, oxAuthConfDynamic = self.get_oxAuthConfDynamic()
            oxAuthConfDynamic.update(entries)
            doc_id = self.get_doc_id_from_dn(dn)

            self.spanner.update_data(table='jansAppConf', columns=['doc_id', 'jansConfDyn'], values=[[doc_id, json.dumps(oxAuthConfDynamic)]])

        elif self.moddb == BackendTypes.COUCHBASE:
            for k in entries:
                n1ql = 'UPDATE `{}` USE KEYS "configuration_jans-auth" SET jansConfDyn.{}={}'.format(self.default_bucket, k, json.dumps(entries[k]))
                self.cbm.exec_query(n1ql)


    def enable_script(self, inum, enable=True):
        base.logIt("Enabling script {}".format(inum))
        if self.moddb == BackendTypes.LDAP:
            ldap_operation_result = self.ldap_conn.modify(
                    'inum={},ou=scripts,o=jans'.format(inum),
                    {"jansEnabled": [ldap3.MODIFY_REPLACE, str(enable).lower()]}
                    )
            self.log_ldap_result(ldap_operation_result)

        elif self.moddb in (BackendTypes.MYSQL, BackendTypes.PGSQL):
            dn = 'inum={},ou=scripts,o=jans'.format(inum)
            sqlalchemyObj = self.get_sqlalchObj_for_dn(dn)
            sqlalchemyObj.jansEnabled = 1 if enable else 0
            self.session.commit()

        elif self.moddb == BackendTypes.SPANNER:
            dn = 'inum={},ou=scripts,o=jans'.format(inum)
            table = self.get_spanner_table_for_dn(dn)
            if table:
                self.spanner.update_data(table=table, columns=['doc_id', 'jansEnabled'], values=[[inum, enable]])

        elif self.moddb == BackendTypes.COUCHBASE:
            n1ql = 'UPDATE `{}` USE KEYS "scripts_{}" SET jansEnabled=true'.format(self.default_bucket, inum)
            self.cbm.exec_query(n1ql)

    def enable_service(self, service):
        if self.moddb == BackendTypes.LDAP:
            ldap_operation_result = self.ldap_conn.modify(
                'ou=configuration,o=jans',
                {service: [ldap3.MODIFY_REPLACE, 'true']}
                )
            self.log_ldap_result(ldap_operation_result)

        elif self.moddb in (BackendTypes.MYSQL, BackendTypes.PGSQL):
            sqlalchemyObj = self.get_sqlalchObj_for_dn('ou=configuration,o=jans')
            setattr(sqlalchemyObj, service, 1)
            self.session.commit()

        elif self.moddb == BackendTypes.SPANNER:
            self.spanner.update_data(table='jansAppConf', columns=['doc_id', service], values=[["configuration", True]])

        elif self.moddb == BackendTypes.COUCHBASE:
            n1ql = 'UPDATE `{}` USE KEYS "configuration" SET {}=true'.format(self.default_bucket, service)
            self.cbm.exec_query(n1ql)

    def set_configuration(self, component, value, dn='ou=configuration,o=jans'):
        if self.moddb == BackendTypes.LDAP:
            ldap_operation_result = self.ldap_conn.modify(
                dn,
                {component: [ldap3.MODIFY_REPLACE, value]}
                )
            self.log_ldap_result(ldap_operation_result)

        elif self.moddb in (BackendTypes.MYSQL, BackendTypes.PGSQL):
            result = self.get_sqlalchObj_for_dn(dn)
            table_name = result.objectClass
            sqlalchemy_table = self.Base.classes[table_name]
            sqlalchemyObj = self.session.query(sqlalchemy_table).filter(sqlalchemy_table.dn ==dn).first()
            cur_val = getattr(sqlalchemyObj, component)
            setattr(sqlalchemyObj, component, value)
            self.session.commit()

        elif self.moddb == BackendTypes.SPANNER:
            table = self.get_spanner_table_for_dn(dn)
            key = ldif_utils.get_key_from(dn)
            self.spanner.update_data(table=table, columns=["doc_id", component], values=[[key, value]])

        elif self.moddb == BackendTypes.COUCHBASE:
            n1ql = 'UPDATE `{}` USE KEYS "{}" SET {}={}'.format(key, self.default_bucket, component, value)
            self.cbm.exec_query(n1ql)


    def dn_exists(self, dn):
        mapping_location = self.get_backend_location_for_dn(dn)

        if mapping_location in (BackendTypes.MYSQL, BackendTypes.PGSQL):
            base.logIt("Querying RDBM for dn {}".format(dn))
            result = self.get_sqlalchObj_for_dn(dn)
            if result:
                return result.__dict__
            return

        elif mapping_location == BackendTypes.SPANNER:
            table = self.get_spanner_table_for_dn(dn)
            data = self.dn_exists_rdbm(dn, table)
            return self.spanner_to_dict(data)

        elif mapping_location == BackendTypes.LDAP:
            base.logIt("Querying LDAP for dn {}".format(dn))
            result = self.ldap_conn.search(search_base=dn, search_filter='(objectClass=*)', search_scope=ldap3.BASE, attributes=['*'])
            if result:
                key, document = ldif_utils.get_document_from_entry(self.ldap_conn.response[0]['dn'], self.ldap_conn.response[0]['attributes'])
                return document

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

        if backend_location == BackendTypes.SPANNER:
            result = self.spanner.exec_sql('SELECT * from {} WHERE dn="{}"'.format(table, dn))
            if result and 'rows' in result and result['rows']:
                return result
            return
        sqlalchemy_table = self.Base.classes[table].__table__
        return self.session.query(sqlalchemy_table).filter(sqlalchemy_table.columns.dn == dn).first()


    def spanner_to_dict(self, data):
        if not data or not'rows' in data:
            return {}

        n = len(data['rows'])
        retVal = []
        for j in range(n):
            row = data['rows'][j]
            row_dict = {}

            for i, field in enumerate(data['fields']):
                val = row[i]
                if val:
                    if field['type'] == 'INT64':
                        val = int(val)
                    row_dict[field['name']] = val
            if n > 1:
                retVal.append(row_dict)
            else:
                return row_dict

        return retVal


    def search(self, search_base, search_filter='(objectClass=*)', search_scope=ldap3.LEVEL, fetchmany=False):
        base.logIt("Searching database for dn {} with filter {}".format(search_base, search_filter))
        backend_location = self.get_backend_location_for_dn(search_base)

        if backend_location == BackendTypes.LDAP:
            if self.ldap_conn.search(search_base=search_base, search_filter=search_filter, search_scope=search_scope, attributes=['*']):
                if not fetchmany:
                    key, document = ldif_utils.get_document_from_entry(self.ldap_conn.response[0]['dn'], self.ldap_conn.response[0]['attributes'])
                    return document

                documents = []
                for result in self.ldap_conn.response:
                    key, document = ldif_utils.get_document_from_entry(result['dn'], result['attributes'])
                    documents.append((key, document))
                return documents

        if backend_location in (BackendTypes.MYSQL, BackendTypes.PGSQL, BackendTypes.SPANNER):
            if backend_location != BackendTypes.SPANNER and self.Base is None:
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

            if backend_location == BackendTypes.SPANNER:

                if fetchmany:
                    retVal = []
                else:
                    retVal = {}

                for col, val in search_list:
                    if val == '*':
                        continue
                    
                    if col.lower() == 'objectclass':
                        s_table = val
                    else:
                        val = val.replace('*', '%')
                        q_operator = 'LIKE' if '%' in val else '='
                        where_clause = 'AND {} {} "{}"'.format(col, q_operator, val)

                if not s_table:
                    return retVal

                if search_scope == ldap3.BASE:
                    dn_clause = 'dn = "{}"'.format(search_base)
                else:
                    dn_clause = 'dn LIKE "%{}"'.format(search_base)

                sql_cmd = 'SELECT * FROM {} WHERE ({}) {}'.format(s_table, dn_clause, where_clause)

                data = self.spanner.exec_sql(sql_cmd)

                if not data.get('rows'):
                    return retVal

                retVal = self.spanner_to_dict(data)
                if not fetchmany and isinstance(retVal, list):
                    retVal = retVal[0]

                return retVal

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

            if search_scope == ldap3.BASE:
                sqlalchemyQueryObject = sqlalchemyQueryObject.filter(sqlalchemy_table.dn == search_base)
            else:
                sqlalchemyQueryObject = sqlalchemyQueryObject.filter(sqlalchemy_table.dn.like('%'+search_base))

            if fetchmany:
                result = sqlalchemyQueryObject.all()
                return [ item.__dict__ for item in result ]

            else:
                result = sqlalchemyQueryObject.first()
                if result:
                    return result.__dict__


        if backend_location == BackendTypes.COUCHBASE:
            key = ldif_utils.get_key_from(search_base)
            bucket = self.get_bucket_for_key(key)

            if search_scope == ldap3.BASE:
                n1ql = 'SELECT * FROM `{}` USE KEYS "{}"'.format(bucket, key)
            else:

                if '&' in search_filter:
                    re_match = re.match('\(&\((.*?)\)\((.*?)\)\)', search_filter)
                    if re_match:
                        re_list = re_match.groups()
                        dn_to_parse = re_list[0] if 'objectclass' in re_list[1].lower() else re_list[1]
                else:
                    dn_to_parse = search_filter.strip('(').strip(')')

                parsed_dn = dnutils.parse_dn(dn_to_parse)
                attr = parsed_dn[0][0]
                val = parsed_dn[0][1]
                if '*' in val:
                    search_clause = 'LIKE "{}"'.format(val.replace('*', '%'))
                else:
                    search_clause = '="{}"'.format(val.replace('*', '%'))

                n1ql = 'SELECT * FROM `{}` WHERE `{}` {}'.format(bucket, attr, search_clause)

            result = self.cbm.exec_query(n1ql)
            if result.ok:
                data = result.json()
                if data.get('results'):
                    if fetchmany:
                        return [ item[bucket] for item in data['results'] ]
                    else:
                        return data['results'][0][bucket]


    def add2strlist(self, client_id, strlist):
        value2 = []
        for v in strlist.split(','):
            if v.strip():
                value2.append(v.strip())
        value2.append(client_id)

        return  ','.join(value2)

    def delete_dn(self, dn):
        if self.dn_exists(dn):
            backend_location = self.get_backend_location_for_dn(dn)

            if backend_location == BackendTypes.LDAP:
                def recursive_delete(dn):
                    self.ldap_conn.search(search_base=dn, search_filter='(objectClass=*)', search_scope=ldap3.LEVEL)
                    for entry in self.ldap_conn.response:
                        recursive_delete(entry['dn'])
                    self.ldap_conn.delete(dn)
                recursive_delete(dn)

            elif backend_location in (BackendTypes.MYSQL, BackendTypes.PGSQL):
                sqlalchemy_obj = self.get_sqlalchObj_for_dn(dn)
                if sqlalchemy_obj:
                    self.session.delete(sqlalchemy_obj)
                    self.session.commit()

            elif backend_location == BackendTypes.SPANNER:
                tbl = self.get_spanner_table_for_dn(dn)
                self.spanner.exec_sql('DELETE from {} WHERE dn="{}"'.format(tbl, dn))

            elif backend_location == BackendTypes.COUCHBASE:
                key = ldif_utils.get_key_from(dn)
                bucket =self.get_bucket_for_key(key)
                n1ql = 'DELETE FROM `{}` USE KEYS "{}"'.format(bucket, key)
                self.cbm.exec_query(n1ql)

    def add_client2script(self, script_inum, client_id):
        dn = 'inum={},ou=scripts,o=jans'.format(script_inum)

        backend_location = self.get_backend_location_for_dn(dn)

        if backend_location == BackendTypes.LDAP:
            if self.dn_exists(dn):
                for e in self.ldap_conn.response[0]['attributes'].get('jansConfProperty', []):
                    try:
                        jansConfProperty = json.loads(e)
                    except:
                        continue
                    if isinstance(jansConfProperty, dict) and jansConfProperty.get('value1') == 'allowed_clients':
                        if not client_id in jansConfProperty['value2']:
                            jansConfProperty['value2'] = self.add2strlist(client_id, jansConfProperty['value2'])
                            jansConfProperty_js = json.dumps(jansConfProperty)
                            ldap_operation_result = self.ldap_conn.modify(
                                dn,
                                {'jansConfProperty': [ldap3.MODIFY_DELETE, e]}
                                )
                            self.log_ldap_result(ldap_operation_result)
                            ldap_operation_result = self.ldap_conn.modify(
                                dn,
                                {'jansConfProperty': [ldap3.MODIFY_ADD, jansConfProperty_js]}
                                )
                            self.log_ldap_result(ldap_operation_result)

        elif backend_location in (BackendTypes.MYSQL, BackendTypes.PGSQL):
            sqlalchemyObj = self.get_sqlalchObj_for_dn(dn)
            if sqlalchemyObj:
                if sqlalchemyObj.jansConfProperty:
                    jansConfProperty = copy.deepcopy(sqlalchemyObj.jansConfProperty)
                else:
                    jansConfProperty = {'v': []}

                ox_configuration_property_list = jansConfProperty['v'] if Config.rdbm_type == 'mysql' else jansConfProperty

                for i, oxconfigprop in enumerate(ox_configuration_property_list[:]):
                    if isinstance(oxconfigprop, str):
                        oxconfigprop = json.loads(oxconfigprop)
                    if oxconfigprop.get('value1') == 'allowed_clients' and client_id not in oxconfigprop['value2']:
                        oxconfigprop['value2'] = self.add2strlist(client_id, oxconfigprop['value2'])
                        ox_configuration_property_list[i] = json.dumps(oxconfigprop)
                        break
                else:
                    ox_configuration_property_list.append(json.dumps({'value1': 'allowed_clients', 'value2': client_id}))

                sqlalchemyObj.jansConfProperty = jansConfProperty if BackendTypes.MYSQL else ox_configuration_property_list
                self.session.commit()


        elif backend_location == BackendTypes.SPANNER:
            data = self.spanner.exec_sql('SELECT jansConfProperty from jansCustomScr WHERE dn="{}"'.format(dn))
            jansConfProperty = []
            added = False
            spanner_data = self.spanner_to_dict(data)
            jansConfProperty = spanner_data.get('jansConfProperty', [])

            for i, oxconfigprop in enumerate(jansConfProperty):
                oxconfigpropjs = json.loads(oxconfigprop)
                if oxconfigpropjs.get('value1') == 'allowed_clients':
                    if client_id in oxconfigpropjs['value2']:
                        return
                    oxconfigpropjs['value2'] = self.add2strlist(client_id, oxconfigpropjs['value2'])
                    jansConfProperty[i] = json.dumps(oxconfigpropjs)
                    added = True
                    break

            if not added:
                jansConfProperty.append(json.dumps({'value1': 'allowed_clients', 'value2': client_id}))
            self.spanner.update_data(table='jansCustomScr', columns=['doc_id', 'jansConfProperty'], values=[[script_inum,  jansConfProperty]])

        elif backend_location == BackendTypes.COUCHBASE:
            bucket = self.get_bucket_for_dn(dn)
            n1ql = 'SELECT jansConfProperty FROM `{}` USE KEYS "scripts_{}"'.format(bucket, script_inum)
            result = self.cbm.exec_query(n1ql)
            js = result.json()

            oxConfigurationProperties = js['results'][0]['jansConfProperty']
            for i, oxconfigprop_str in enumerate(oxConfigurationProperties):
                oxconfigprop = json.loads(oxconfigprop_str)
                if oxconfigprop.get('value1') == 'allowed_clients' and not client_id in oxconfigprop['value2']:
                    oxconfigprop['value2'] = self.add2strlist(client_id, oxconfigprop['value2'])
                    oxConfigurationProperties[i] = json.dumps(oxconfigprop)
                    break
            else:
                return

            n1ql = 'UPDATE `{}` USE KEYS "scripts_{}" SET `jansConfProperty`={}'.format(bucket, script_inum, json.dumps(oxConfigurationProperties))
            self.cbm.exec_query(n1ql)

    def get_key_prefix(self, key):
        n = key.find('_')
        return key[:n+1]

    def check_attribute_exists(self, key, attribute):
        bucket = self.get_bucket_for_key(key)
        n1ql = 'SELECT `{}` FROM `{}` USE KEYS "{}"'.format(attribute, bucket, key)
        result = self.cbm.exec_query(n1ql)
        if result.ok:
            data = result.json()
            r = data.get('results', [])
            if r and r[0].get(attribute):
                return r[0][attribute]


    def log_ldap_result(self, ldap_operation_result):
        if not ldap_operation_result:
            base.logIt("Ldap modify operation failed {}".format(str(self.ldap_conn.result)))
            base.logIt("Ldap modify operation failed {}".format(str(self.ldap_conn.result)), True)


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
        dn_parsed = dnutils.parse_dn(dn)
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
        if Config.rdbm_type == 'mysql':
            for tbl in self.Base.classes:
                for col in tbl.__table__.columns:
                    if isinstance(col.type, sqlalchemy.dialects.mysql.LONGTEXT) and col.comment and col.comment.lower() == 'json':
                        col.type = sqlalchemy.dialects.mysql.json.JSON()

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
        if Config.rdbm_type == 'spanner':
            return table in self.spanner.get_tables()
        else:
            metadata = sqlalchemy.MetaData()
            try:
                metadata.reflect(self.engine, only=[table])
            except:
                pass

            return table in metadata

    def get_attr_sql_data_type(self, key):
        if key in self.sql_data_types:
            data_type = self.sql_data_types[key]
        else:
            attr_syntax = self.get_attr_syntax(key)
            data_type = self.ldap_sql_data_type_mapping[attr_syntax]

        data_type = (data_type.get(Config.rdbm_type) or data_type['mysql'])['type']

        return data_type

    def get_rdbm_val(self, key, val, rdbm_type=None):

        data_type = self.get_attr_sql_data_type(key)

        if data_type in ('SMALLINT', 'BOOL', 'BOOLEAN'):
            if val[0].lower() in ('1', 'on', 'true', 'yes', 'ok'):
                return 1 if data_type == 'SMALLINT' else True
            return 0 if data_type == 'SMALLINT' else False

        if data_type == 'INT':
            return int(val[0])

        if data_type in ('DATETIME(3)', 'TIMESTAMP'):
            dval = val[0].strip('Z')
            sep= 'T' if rdbm_type == 'spanner' else ' '
            postfix = 'Z' if rdbm_type == 'spanner' else ''
            return "{}-{}-{}{}{}:{}:{}{}{}".format(dval[0:4], dval[4:6], dval[6:8], sep, dval[8:10], dval[10:12], dval[12:14], dval[14:17], postfix)

        if data_type == 'JSON':
            json_data = {'v':[]}
            for d in val:
                json_data['v'].append(d)

            return json_data

        if data_type in ('ARRAY<STRING(MAX)>', 'JSONB'):
            return val

        return val[0]

    def get_clean_objcet_class(self, entry):

        objectClass = entry.get('objectClass') or entry.get('objectclass')

        if objectClass:
            if 'top' in objectClass:
                objectClass.remove('top')
            objectClass = objectClass[-1]

        return objectClass

    def get_doc_id_from_dn(self, dn):
        dn_parsed = dnutils.parse_dn(dn)
        doc_id = dn_parsed[0][1]
        if doc_id == 'jans':
            doc_id = '_'
        return doc_id

    def get_spanner_table_for_dn(self, dn):
        tables = self.spanner.get_tables()

        for table in tables:
            sql_cmd = 'SELECT doc_id FROM {} WHERE dn="{}"'.format(table, dn)
            result = self.spanner.exec_sql(sql_cmd)
            if result and 'rows' in result and result['rows']:
                return table

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
                if backend_location == BackendTypes.LDAP:
                    if 'add' in  entry and 'changetype' in entry:
                        base.logIt("LDAP modify add dn:{} entry:{}".format(dn, dict(entry)))
                        change_attr = entry['add'][0]
                        ldap_operation_result = self.ldap_conn.modify(dn, {change_attr: [(ldap3.MODIFY_ADD, entry[change_attr])]})
                        self.log_ldap_result(ldap_operation_result)

                    elif 'replace' in  entry and 'changetype' in entry:
                        base.logIt("LDAP modify replace dn:{} entry:{}".format(dn, dict(entry)))
                        change_attr = entry['replace'][0]
                        ldap_operation_result = self.ldap_conn.modify(dn, {change_attr: [(ldap3.MODIFY_REPLACE, [entry[change_attr][0]])]})
                        self.log_ldap_result(ldap_operation_result)

                    elif not self.dn_exists(dn):
                        base.logIt("Adding LDAP dn:{} entry:{}".format(dn, dict(entry)))
                        ldap_operation_result = self.ldap_conn.add(dn, attributes=entry)
                        self.log_ldap_result(ldap_operation_result)

                elif backend_location in (BackendTypes.MYSQL, BackendTypes.PGSQL):
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
                                    if Config.rdbm_type == 'mysql':
                                        cur_val['v'].append(val_)
                                    else:
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
                        dn_parsed = dnutils.parse_dn(dn)
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
                                vals[col.name] = {'v': []} if Config.rdbm_type == 'mysql' else []

                        sqlalchObj = sqlalchCls()

                        for v in vals:
                            setattr(sqlalchObj, v, vals[v])

                        base.logIt("Adding {}".format(sqlalchObj.doc_id))
                        self.session.add(sqlalchObj)
                        self.session.commit()


                elif backend_location == BackendTypes.SPANNER:

                    if 'add' in  entry and 'changetype' in entry:
                        table = self.get_spanner_table_for_dn(dn)
                        change_attr = entry['add'][0]
                        if table:
                            doc_id = self.get_doc_id_from_dn(dn)

                            if self.in_subtable(table, change_attr):
                                sub_table = '{}_{}'.format(table, change_attr)
                                for subval in entry[change_attr]:
                                    typed_val = self.get_rdbm_val(change_attr, subval, rdbm_type='spanner')
                                    dict_doc_id = self.get_sha_digest(typed_val)
                                    self.spanner.insert_data(table=sub_table, columns=['doc_id', 'dict_doc_id', change_attr], values=[[doc_id, typed_val, typed_val]])

                            else:
                                data = self.spanner.exec_sql('SELECT {} FROM {} WHERE doc_id="{}"'.format(entry['add'][0], table, doc_id))
                                if data.get('rows'):
                                    cur_data = []

                                    if 'rows' in data and data['rows'] and data['rows'][0] and data['rows'][0][0]:
                                        cur_data = data['rows'][0][0]
                                    
                                    for cur_val in entry[change_attr]:
                                        typed_val = self.get_rdbm_val(change_attr, cur_val, rdbm_type='spanner')
                                        cur_data.append(typed_val)

                                self.spanner.update_data(table=table, columns=['doc_id', change_attr], values=[[doc_id, cur_data]])

                    elif 'replace' in entry and 'changetype' in entry:
                        table = self.get_spanner_table_for_dn(dn)
                        doc_id = self.get_doc_id_from_dn(dn)
                        replace_attr = entry['replace'][0]
                        typed_val = self.get_rdbm_val(replace_attr, entry[replace_attr], rdbm_type='spanner')

                        if self.in_subtable(table, replace_attr):
                            sub_table = '{}_{}'.format(table, replace_attr)
                            # TODO: how to replace ?
                            #for subval in typed_val:
                            #    self.spanner.update_data(table=sub_table, columns=['doc_id', replace_attr], values=[[doc_id, subval]])
                        else:
                            self.spanner.update_data(table=table, columns=['doc_id', replace_attr], values=[[doc_id, typed_val]])

                    else:
                        vals = {}
                        dn_parsed = dnutils.parse_dn(dn)
                        rdn_name = dn_parsed[0][0]
                        objectClass = objectClass = self.get_clean_objcet_class(entry)
                        if objectClass.lower() == 'organizationalunit':
                            continue

                        doc_id = self.get_doc_id_from_dn(dn)
                        vals['doc_id'] = doc_id
                        vals['dn'] = dn
                        vals['objectClass'] = objectClass

                        if 'objectClass' in entry:
                            entry.pop('objectClass')
                        elif 'objectclass' in entry:
                            entry.pop('objectclass')

                        table_name = objectClass

                        subtable_data = []

                        for lkey in entry:
                            spanner_vals = self.get_rdbm_val(lkey, entry[lkey], rdbm_type='spanner')
                            if not self.in_subtable(table_name, lkey):
                                vals[lkey] = spanner_vals
                            else:
                                sub_table = '{}_{}'.format(table_name, lkey)
                                sub_table_columns = ['doc_id', 'dict_doc_id', lkey]
                                sub_table_values = []
                                for subtableval in spanner_vals:
                                    dict_doc_id = self.get_sha_digest(subtableval)
                                    sub_table_values.append([doc_id, dict_doc_id, subtableval])
                                subtable_data.append((sub_table, sub_table_columns, sub_table_values))

                        columns = [ *vals.keys() ]
                        values = [ vals[lkey] for lkey in columns ]

                        self.spanner.insert_data(table=table_name, columns=columns, values=[values])

                        for sdata in subtable_data:
                            self.spanner.insert_data(table=sdata[0], columns=sdata[1], values=sdata[2])

                elif backend_location == BackendTypes.COUCHBASE:
                    if len(entry) < 3:
                        continue
                    key, document = ldif_utils.get_document_from_entry(dn, entry)
                    cur_bucket = bucket if bucket else self.get_bucket_for_dn(dn)
                    base.logIt("Addnig document {} to Couchebase bucket {}".format(key, cur_bucket))

                    n1ql_list = []

                    if 'changetype' in document:
                        if 'replace' in document:
                            attribute = document['replace']
                            n1ql_list.append('UPDATE `%s` USE KEYS "%s" SET `%s`=%s' % (cur_bucket, key, attribute, json.dumps(document[attribute])))
                        elif 'add' in document:
                            attribute = document['add']
                            result = self.check_attribute_exists(key, attribute)
                            data = document[attribute]
                            if result:
                                if isinstance(data, list):
                                    for d in data:
                                        n1ql_list.append('UPDATE `%s` USE KEYS "%s" SET `%s`=ARRAY_APPEND(`%s`, %s)' % (cur_bucket, key, attribute, attribute, json.dumps(d)))
                                else:
                                    n1ql_list.append('UPDATE `%s` USE KEYS "%s" SET `%s`=ARRAY_APPEND(`%s`, %s)' % (cur_bucket, key, attribute, attribute, json.dumps(data)))
                            else:
                                if attribute in attribDataTypes.listAttributes and not isinstance(data, list):
                                    data = [data]
                                n1ql_list.append('UPDATE `%s` USE KEYS "%s" SET `%s`=%s' % (cur_bucket, key, attribute, json.dumps(data)))
                    else:
                        for k in document:
                            try:
                                kdata = json.loads(document[k])
                                if isinstance(kdata, dict):
                                    document[k] = kdata
                            except:
                                pass

                        n1ql_list.append('UPSERT INTO `%s` (KEY, VALUE) VALUES ("%s", %s)' % (cur_bucket, key, json.dumps(document)))

                    for q in n1ql_list:
                        self.cbm.exec_query(q)

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
                                cur_val['v'].append(val_)
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
                    dn_parsed = dnutils.parse_dn(dn)
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
                            vals[col.name] = {'v': []}

                    sqlalchObj = sqlalchCls()

                    for v in vals:
                        setattr(sqlalchObj, v, vals[v])

                    base.logIt("Adding {}".format(sqlalchObj.doc_id))
                    self.session.add(sqlalchObj)
                    self.session.commit()

    def import_schema(self, schema_file):
        if self.moddb == BackendTypes.LDAP:
            base.logIt("Importing schema {}".format(schema_file))
            parser = ldif_utils.myLdifParser(schema_file)
            parser.parse()
            for dn, entry in parser.entries:
                if 'changetype' in entry:
                    entry.pop('changetype')
                if 'add' in entry:
                    entry.pop('add')
                for entry_type in entry:
                    for e in entry[entry_type]:
                        base.logIt("Adding to schema, type: {}  value: {}".format(entry_type, e))
                        ldap_operation_result = self.ldap_conn.modify(dn, {entry_type: [ldap3.MODIFY_ADD, e]})
                        self.log_ldap_result(ldap_operation_result)
            #we need re-bind after schema operations
            self.ldap_conn.rebind()

    def get_group_for_key(self, key):
        key_prefix = self.get_key_prefix(key)
        for group in Config.get('couchbaseBucketDict', {}):
            if key_prefix in Config.couchbaseBucketDict[group]['document_key_prefix']:
                break
        else:
            group = 'default'

        return group

    def get_bucket_for_key(self, key):
        group = self.get_group_for_key(key)
        if group == 'default':
            return Config.couchbase_bucket_prefix

        return Config.couchbase_bucket_prefix + '_' + group

    def get_bucket_for_dn(self, dn):
        key = ldif_utils.get_key_from(dn)
        return self.get_bucket_for_key(key)

    def get_backend_location_for_dn(self, dn):
        key = ldif_utils.get_key_from(dn)
        group = self.get_group_for_key(key)

        if Config.mapping_locations[group] == 'ldap':
            return static.BackendTypes.LDAP

        if Config.mapping_locations[group] == 'rdbm':
            if Config.rdbm_type == 'mysql':
                return static.BackendTypes.MYSQL
            elif Config.rdbm_type == 'pgsql':
                return static.BackendTypes.PGSQL
            elif Config.rdbm_type == 'spanner':
                return static.BackendTypes.SPANNER

        if Config.mapping_locations[group] == 'couchbase':
            return static.BackendTypes.COUCHBASE


    def checkCBRoles(self, buckets=[]):

        self.cb_bucket_roles = ['bucket_admin', 'query_delete', 'query_select', 
                            'query_update', 'query_insert',
                            'query_manage_index']

        result = self.cbm.whoami()
        bc = buckets[:]
        bucket_roles = {}
        if 'roles' in result:

            for role in result['roles']:
                if role['role'] == 'admin':
                    Config.isCouchbaseUserAdmin = True
                    return True, None

                if not role['bucket_name'] in bucket_roles:
                    bucket_roles[role['bucket_name']] = []

                bucket_roles[role['bucket_name']].append(role['role'])

        for b_ in bc[:]:
            for r_ in self.cb_bucket_roles:
                if not r_ in bucket_roles[b_]:
                    break
            else:
                bc.remove(b_)

        if bc:
            return False, bc

        return True, None


    def __del__(self):
        try:
            self.ldap_conn.unbind()
            self.ready = False
        except:
            pass


dbUtils = DBUtils()
