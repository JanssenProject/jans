import warnings
import sys
import os
import re
import json
import logging
import copy
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

my_path = path = PurePath(os.path.dirname(os.path.realpath(__file__)))
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

        base.logIt("Bind to database")

        if Config.mappingLocations['default'] == 'ldap':
            self.moddb = BackendTypes.LDAP
        elif Config.mappingLocations['default'] == 'rdbm':
            self.read_jans_schema()
            if Config.rdbm_type == 'mysql':
                self.moddb = BackendTypes.MYSQL
        else:
            self.moddb = BackendTypes.COUCHBASE


        if not hasattr(self, 'ldap_conn') or force:
            for group in Config.mappingLocations:
                if Config.mappingLocations[group] == 'ldap':
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
            for group in Config.mappingLocations:
                if Config.mappingLocations[group] == 'rdbm':
                    if Config.rdbm_type == 'mysql':
                        base.logIt("Making MySql Conncetion")
                        result = self.mysqlconnection()
                        if not result[0]:
                            print("{}FATAL: {}{}".format(colors.FAIL, result[1], colors.ENDC))
                        break

        self.set_cbm()
        self.default_bucket = Config.couchbase_bucket_prefix


    def mysqlconnection(self, log=True):
        base.logIt("Making MySQL Connection to {}:{}/{} with user {}".format(Config.rdbm_host, Config.rdbm_port, Config.rdbm_db, Config.rdbm_user))

        bind_uri = 'mysql+pymysql://{}:{}@{}:{}/{}?charset=utf8mb4'.format(
                        Config.rdbm_user,
                        Config.rdbm_password,
                        Config.rdbm_host,
                        Config.rdbm_port,
                        Config.rdbm_db,
                )

        try:
            self.engine = sqlalchemy.create_engine(bind_uri)
            logging.basicConfig(filename=os.path.join(Config.install_dir, 'logs/sqlalchemy.log'))
            logging.getLogger('sqlalchemy.engine').setLevel(logging.INFO)
            Session = sqlalchemy.orm.sessionmaker(bind=self.engine)
            self.session = Session()
            self.metadata = sqlalchemy.MetaData()
            self.session.connection()
            base.logIt("MySQL Connection was successful")
            return True, self.session

        except Exception as e:
            if log:
                base.logIt("Can't connect to MySQL server: {}".format(str(e), True))
            return False, e

    def read_jans_schema(self, others=[]):
        self.jans_attributes = []

        for schema_fn_ in ['jans_schema.json', 'custom_schema.json'] + others:
            schema_fn = schema_fn_ if schema_fn_.startswith('/') else os.path.join(Config.install_dir, 'schema', schema_fn_)
            schema = base.readJsonFile(schema_fn)
            self.jans_attributes += schema['attributeTypes']

        self.ldap_sql_data_type_mapping = base.readJsonFile(os.path.join(Config.static_rdbm_dir, 'ldap_sql_data_type_mapping.json'))
        self.sql_data_types = base.readJsonFile(os.path.join(Config.static_rdbm_dir, 'sql_data_types.json'))
        self.opendj_attributes_syntax = base.readJsonFile(os.path.join(Config.static_rdbm_dir, 'opendj_attributes_syntax.json'))


    def exec_rdbm_query(self, query, getresult=False):
        base.logIt("Executing {} Query: {}".format(Config.rdbm_type, query))
        if Config.rdbm_type == 'mysql':
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

        elif self.moddb == BackendTypes.MYSQL:
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

        elif self.moddb == BackendTypes.MYSQL:
            dn, oxAuthConfDynamic = self.get_oxAuthConfDynamic()
            oxAuthConfDynamic.update(entries)
            sql_cmd = "UPDATE jansAppConf SET jansConfDyn='{}' WHERE dn='{}'".format(json.dumps(oxAuthConfDynamic, indent=2), dn)
            self.exec_rdbm_query(sql_cmd)

        elif self.moddb == BackendTypes.COUCHBASE:
            for k in entries:
                n1ql = 'UPDATE `{}` USE KEYS "configuration_jans-auth" SET jansConfDyn.{}={}'.format(self.default_bucket, k, json.dumps(entries[k]))
                self.cbm.exec_query(n1ql)


    def enable_script(self, inum):
        if self.moddb == BackendTypes.LDAP:
            ldap_operation_result = self.ldap_conn.modify(
                    'inum={},ou=scripts,o=jans'.format(inum),
                    {"jansEnabled": [ldap3.MODIFY_REPLACE, 'true']}
                    )
            self.log_ldap_result(ldap_operation_result)

        elif self.moddb == BackendTypes.MYSQL:
            sql_cmd = "UPDATE jansCustomScr SET jansEnabled=1 WHERE dn='inum={},ou=scripts,o=jans'".format(inum)
            self.exec_rdbm_query(sql_cmd)

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

        elif self.moddb == BackendTypes.MYSQL:
            sql_cmd = "UPDATE jansAppConf SET {}=1 WHERE dn='ou=configuration,o=jans'".format(service)
            self.exec_rdbm_query(sql_cmd)

        elif self.moddb == BackendTypes.COUCHBASE:
            n1ql = 'UPDATE `{}` USE KEYS "configuration" SET {}=true'.format(self.default_bucket, service)
            self.cbm.exec_query(n1ql)

    def set_configuration(self, component, value):
        if self.moddb == BackendTypes.LDAP:
            ldap_operation_result = self.ldap_conn.modify(
                'ou=configuration,o=jans',
                {component: [ldap3.MODIFY_REPLACE, value]}
                )
            self.log_ldap_result(ldap_operation_result)

        elif self.moddb == BackendTypes.MYSQL:
            result = self.get_sqlalchObj_for_dn('ou=configuration,o=jans')
            table_name = result.objectClass
            sqlalchemy_table = self.Base.classes[table_name]
            sqlalchemyObj = self.session.query(sqlalchemy_table).filter(sqlalchemy_table.dn =='ou=configuration,o=jans').first()
            cur_val = getattr(sqlalchemyObj, component)
            setattr(sqlalchemyObj, component, value)
            self.session.commit()

        elif self.moddb == BackendTypes.COUCHBASE:
            n1ql = 'UPDATE `{}` USE KEYS "configuration" SET {}={}'.format(self.default_bucket, component, value)
            self.cbm.exec_query(n1ql)


    def dn_exists(self, dn):
        if self.session:
            base.logIt("Querying RDBM for dn {}".format(dn))
            result = self.get_sqlalchObj_for_dn(dn)
            if result:
                return result.__dict__
            return

        elif self.cbm:
            bucket = self.get_bucket_for_dn(dn)
            key = ldif_utils.get_key_from(dn)
            n1ql = 'SELECT * FROM `{}` USE KEYS "{}"'.format(bucket, key)
            result = self.cbm.exec_query(n1ql)
            if result.ok:
                data = result.json()
                if data.get('results'):
                    return data['results'][0][bucket]
                    
            return

        base.logIt("Querying LDAP for dn {}".format(dn))
        return self.ldap_conn.search(search_base=dn, search_filter='(objectClass=*)', search_scope=ldap3.BASE, attributes=['*'])

    def dn_exists_rdbm(self, dn, table):
        sqlalchemy_table = self.Base.classes[table].__table__
        return self.session.query(sqlalchemy_table).filter(sqlalchemy_table).filter(sqlalchemy_table.columns.dn == dn).first()

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

        if backend_location == BackendTypes.MYSQL:
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

        elif backend_location == BackendTypes.MYSQL:
            sqlalchemyObj = self.dn_exists_rdbm(dn, 'jansCustomScr')
            if sqlalchemyObj:
                if sqlalchemyObj.jansConfProperty:
                    jansConfProperty = sqlalchemyObj.jansConfProperty
                else:
                    jansConfProperty = {'v': []}

                for oxconfigprop in jansConfProperty['v']:
                    if oxconfigprop.get('value1') == 'allowed_clients' and not client_id in oxconfigprop['value2']:
                        oxconfigprop['value2'] = self.add2strlist(client_id, oxconfigprop['value2'])
                        break
                else:
                    jansConfProperty['v'].append({'value1': 'allowed_clients', 'value2': client_id})

                sqlalchemyObj.jansConfProperty = jansConfProperty
                self.session.commit()

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
            metadata.reflect(engine, only=[table])
            return True
        except:
            pass

    def get_attr_sql_data_type(self, key):
        if key in self.sql_data_types:
            data_type = self.sql_data_types[key]
        else:
            attr_syntax = self.get_attr_syntax(key)
            data_type = self.ldap_sql_data_type_mapping[attr_syntax]
    
        data_type = data_type[Config.rdbm_type]['type']

        return data_type

    def get_rdbm_val(self, key, val):
        
        data_type = self.get_attr_sql_data_type(key)

        if data_type in ('SMALLINT',):
            if val[0].lower() in ('1', 'on', 'true', 'yes', 'ok'):
                return 1
            return 0

        if data_type == 'INT':
            return int(val[0])

        if data_type in ('DATETIME(3)',):
            dval = val[0].strip('Z')
            return "{}-{}-{} {}:{}:{}{}".format(dval[0:4], dval[4:6], dval[6:8], dval[8:10], dval[10:12], dval[12:14], dval[14:17])

        if data_type == 'JSON':
            json_data = {'v':[]}
            for d in val:
                json_data['v'].append(d)

            return json_data

        return val[0]


    def import_ldif(self, ldif_files, bucket=None, force=None):

        base.logIt("Importing ldif file(s): {} ".format(', '.join(ldif_files)))

        sql_data_fn = os.path.join(Config.outputFolder, Config.rdbm_type, 'jans_data.sql')

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

                elif backend_location == BackendTypes.MYSQL:
                    if self.Base is None:
                        self.rdm_automapper()

                    if 'add' in  entry and 'changetype' in entry:
                        attribute = entry['add'][0]
                        new_val = entry[attribute]
                        sqlalchObj = self.get_sqlalchObj_for_dn(dn)

                        if sqlalchObj:
                            if isinstance(sqlalchObj.__table__.columns[attribute].type, sqlalchemy.dialects.mysql.json.JSON):
                                cur_val = copy.deepcopy(getattr(sqlalchObj, attribute))
                                for val_ in new_val:
                                    cur_val['v'].append(val_)
                                setattr(sqlalchObj, attribute, cur_val)
                            else:
                                setattr(sqlalchObj, attribute, new_val[0])

                            self.session.commit()

                        else:
                            base.logIt("Can't find current value for repmacement of {}".replace(str(entry)), True)
                            continue

                    elif 'replace' in entry and 'changetype' in entry:
                        attribute = entry['replace'][0]
                        new_val = self.get_rdbm_val(attribute, entry[attribute])
                        sqlalchObj = self.get_sqlalchObj_for_dn(dn)

                        if sqlalchObj:
                            setattr(sqlalchObj, attribute, new_val)
                            self.session.commit()
                        else:
                            base.logIt("Can't find current value for repmacement of {}".replace(str(entry)), True)
                            continue

                    else:
                        vals = {}
                        dn_parsed = dnutils.parse_dn(dn)
                        rdn_name = dn_parsed[0][0]
                        objectClass = entry.get('objectClass') or entry.get('objectclass')

                        if objectClass:
                            if 'top' in objectClass:
                                objectClass.remove('top')
                            if  len(objectClass) == 1 and objectClass[0].lower() == 'organizationalunit':
                                continue
                            objectClass = objectClass[-1]

                        vals['doc_id'] = dn_parsed[0][1]
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
                            if isinstance(col.type, sqlalchemy.dialects.mysql.json.JSON) and not col.name in vals:
                                vals[col.name] = {'v': []}

                        sqlalchObj = sqlalchCls()

                        for v in vals:
                            setattr(sqlalchObj, v, vals[v])

                        base.logIt("Adding {}".format(sqlalchObj.doc_id))
                        self.session.add(sqlalchObj)
                        self.session.commit()


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
        for group in Config.couchbaseBucketDict:
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

        if Config.mappingLocations[group] == 'ldap':
            return static.BackendTypes.LDAP

        if Config.mappingLocations[group] == 'rdbm':
            if Config.rdbm_type == 'mysql':
                return static.BackendTypes.MYSQL
        
        if Config.mappingLocations[group] == 'couchbase':
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
