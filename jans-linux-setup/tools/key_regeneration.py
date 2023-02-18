#!/usr/bin/python3

import os
import shutil
import glob
import json
import subprocess
import sys
import zipfile
import argparse
import enum
import requests
import zipfile
import time
import ldap3
import base64
import bz2
import pymysql
import re
import urllib3


import xml.etree.ElementTree as ET
from types import ModuleType
from urllib.parse import urlparse
from requests.auth import HTTPBasicAuth

requests.packages.urllib3.disable_warnings()

pydes_b64 = b'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'
pyDes = ModuleType('mod')
exec(bz2.decompress(base64.b64decode(pydes_b64)).decode(), pyDes.__dict__)

_VENDOR_ = 'jans'
_AUTH_NAME_ = 'oxAuth' if _VENDOR_ == 'gluu' else 'jans-auth'

parser = argparse.ArgumentParser('This script removes current key and creates new key for {}.'.format(_AUTH_NAME_))
ldap_group = parser.add_mutually_exclusive_group()
ldap_group.add_argument('-expiration_hours', help="Keys expire in hours", type=int)
ldap_group.add_argument('-expiration', help="Keys expire in days", default=365, type=int)
ldap_group.add_argument('-auth-client', help="Path to {}-client-jar-with-dependencies.jar".format(_AUTH_NAME_.lower()))
ldap_group.add_argument('-data-dir', help="Directory to keep keys", default='/opt/{}/keys'.format(_VENDOR_))
argsp = parser.parse_args()


def jproperties_parser(prop_fn):

    def get_seperator_pos(l):
        for i,c in enumerate(l):
            if c in ':=':
                return i

    prop = {}

    if os.path.exists(prop_fn):
        with open(prop_fn) as f:
            content = f.read()
    else:
        content = prop_fn

    for l in content.splitlines():
        ls = l.strip()
        if ls and not ls.startswith('#'):
            n = get_seperator_pos(ls)
            if n:
                key = ls[:n].strip()
                val = ls[n+1:].strip()
                prop[key] = val

    return prop


def backup_file(fn):
    if os.path.exists(fn):
        file_list = glob.glob(fn+'.*')
        n = len(file_list) + 1
        shutil.move(fn, fn+'.'+str(n))

def run_command(args):
    if type(args) == type([]):
        cmd = ' '.join(args)
    else:
        cmd = args
    print("Executing command", cmd)
    p = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=True)
    result = p.communicate()
    return result


def unobscure(s, key):
    cipher = pyDes.triple_des(key)
    decrypted = cipher.decrypt(base64.b64decode(s), padmode=pyDes.PAD_PKCS5)
    return decrypted.decode()

class PersistenceType(enum.Enum):
    ldap = 1
    couchbase = 2
    sql = 3
    spanner = 4



class CBM:

    def __init__(self, host, admin, password):
        self.auth = HTTPBasicAuth(admin, password)
        self.n1ql_api = 'https://{}:18093/query/service'.format(host)

    def exec_query(self, query):
        print("Executing n1ql {}".format(query))
        data = {'statement': query}
        verify_ssl = False
        result = requests.post(self.n1ql_api, data=data, auth=self.auth, verify=verify_ssl)
        return result


class Spanner:
    def __init__(self, credentials):
        self.credentials = credentials
        self.get_session()


    def get_session(self):
        emulator_host = self.credentials.get('connection.emulator-host')
        if emulator_host:
            host, port = emulator_host.split(':')
            scheme = 'http'
            spanner_base_url = '{}://{}:9020/v1/'.format(scheme, host)
            session_url = os.path.join(
                spanner_base_url,
                'projects/{}/instances/{}/databases/{}/sessions'.format(
                        self.credentials['connection.project'],
                        self.credentials['connection.instance'],
                        self.credentials['connection.database']
                        )
                )

        req = requests.post(session_url)
        result = req.json()
        session = result['name']
        self.sessioned_url = os.path.join(spanner_base_url, session)


    def execute_sql(self, sql_cmd):

        post_data = {"sql": sql_cmd}
        req = requests.post(self.sessioned_url + ':executeSql', data=json.dumps(post_data))
        row_data = req.json()

        data = {}
        for i, field in enumerate(row_data['metadata']['rowType']['fields']):
            if field['type']['code'] == 'INT64':
                data[field['name']] = int(row_data['rows'][0][i])
            else:
                data[field['name']] = row_data['rows'][0][i]

        return data

    def put_data(self, table, columns, values):
        data_send = {
          'singleUseTransaction': {
            'readWrite': {}
          },
          'mutations': [
            {
              'update': {
                "table": table,
                "columns": columns,
                "values": values
              }
            }
          ]
        }

        req = requests.post(self.sessioned_url + ':commit', data=json.dumps(data_send))

        print(req.text)


    def delete_session(self):
        if hasattr(self, 'sessioned_url'):
            requests.delete(self.sessioned_url)

    def __del__(self):
        self.delete_session()


class KeyRegenerator:

    def __init__(self):

        self.conf_dir = os.path.join('/etc', _VENDOR_, 'conf')

        # vendor specific definitions
        self.conf_dyn = 'jansConfDyn'
        self.conf_web_keys = 'jansConfWebKeys'
        self.conf_rev = 'jansRevision'
        self.conf_objc = 'jansAppConf'
        self.dnname = 'CN=Jans Auth CA Certificates'
        self.prop_dn = 'jansAuth_ConfigurationEntryDN'

        self.conf_keystore_secret = 'keyStoreSecret'
        self.key_regenerator_jar = '{}-client-jar-with-dependencies.jar'.format(_AUTH_NAME_.lower())

        self.java_cmd = '/opt/jre/bin/java'
        self.keytool_cmd = '/opt/jre/bin/keytool'
        if not os.path.exists(self.java_cmd):
            self.java_cmd = shutil.which('java')
            self.keytool_cmd = shutil.which('keytool')

        self.data_dir = argsp.data_dir
        if not os.path.exists(self.data_dir):
            os.makedirs(self.data_dir)

        self.keys_json_fn = os.path.join(self.data_dir, 'keys.json')
        store_ext = 'p12' if _VENDOR_ == 'jans' else 'pkcs12'
        self.keystore_fn = os.path.join(self.data_dir, '{}-keys.{}'.format(_AUTH_NAME_.lower(), store_ext))


        salt_fn = os.path.join(self.conf_dir, 'salt')
        salt_dict = jproperties_parser(salt_fn)
        self.salt = salt_dict['encodeSalt']

        self.find_auth_client_key_path()


        backup_file(self.keystore_fn)

        self.get_persistence_type()
        self.read_credidentials()


        getattr(self, 'obtain_data_{}'.format(self.persistence_type.name))()

        self.generate_keys()
        self.validate_keys()

        getattr(self, 'update_{}'.format(self.persistence_type.name))()


    def find_auth_client_key_path(self):
        if argsp.auth_client:
            self.client_jar_fn = argsp.auth_client
        else:
            cur_dir = os.path.dirname(__file__)
            
            cur_dir_jar = os.path.join(cur_dir, self.key_regenerator_jar)
            if os.path.exists(cur_dir_jar):
                self.client_jar_fn
            else:
                self.client_jar_fn = os.path.join('/opt/dist', _VENDOR_, self.key_regenerator_jar)

        if not os.path.exists(self.client_jar_fn):
            print("Can't find {}. Exiting ...".format(self.key_regenerator_jar))
            sys.exit()

        print("Determining {} key generator path".format(_AUTH_NAME_))
        # Determine oxauth key generator path
        oxauth_client_jar_zf = zipfile.ZipFile(self.client_jar_fn)
        for fn in oxauth_client_jar_zf.namelist():
            if os.path.basename(fn) == 'KeyGenerator.class':
                fp, ext = os.path.splitext(fn)
                self.key_gen_path = fp.replace('/','.')
                break
        else:
            print("Can't determine {}-client KeyGenerator path. Exiting...".format(_AUTH_NAME_.lower()))
            sys.exit()


    def get_sig_enc_algs(self, web_keys):
        self.sig_enc = {'sig':[], 'enc':[]}

        for key in web_keys['keys']:
            if '_sig_' in key['kid']:
                self.sig_enc['sig'].append(key['alg'])
            elif '_enc_' in key['kid']:
                self.sig_enc['enc'].append(key['alg'])


    def get_persistence_type(self):
        prop_fn = os.path.join(self.conf_dir, '{}.properties'.format(_VENDOR_))
        auth_properties = jproperties_parser(prop_fn)
        self.auth_config_dn = auth_properties[self.prop_dn]
        self.persistence_type = getattr(PersistenceType, auth_properties['persistence.type'])
        dn_s = self.auth_config_dn.split(',')
        self.doc_id = dn_s[0].split('=')[1]
        self.key = dn_s[1].split('=')[1] + '_' + self.doc_id


    def read_credidentials(self):
        prop_fn = os.path.join(self.conf_dir, '{}-{}.properties'.format(_VENDOR_, self.persistence_type.name))
        self.credidentials = jproperties_parser(prop_fn)


    def obtain_data_spanner(self):
        self.spanner = Spanner(self.credidentials)
        data = self.spanner.execute_sql('SELECT dn, {}, {}, {} from {} WHERE doc_id ="{}"'.format(self.conf_dyn, self.conf_web_keys, self.conf_rev, self.conf_objc, self.doc_id))

        ox_auth_conf_dynamic = json.loads(data[self.conf_dyn])
        self.key_store_secret = ox_auth_conf_dynamic[self.conf_keystore_secret]
        self.get_sig_enc_algs(json.loads(data[self.conf_web_keys]))
        self.revision = int(data[self.conf_rev]) + 1

    def obtain_data_ldap(self):
        ldap_password = unobscure(self.credidentials['bindPassword'], key=self.salt)
        ldap_host, ldap_port = self.credidentials['servers'].split(',')[0].split(':')

        server = ldap3.Server(ldap_host, port=int(ldap_port), use_ssl=True)
        self.ldap_conn = ldap3.Connection(server, user=self.credidentials['bindDN'], password=ldap_password)
        self.ldap_conn.bind()


        self.ldap_conn.search(
                    search_base=self.auth_config_dn,
                    search_scope=ldap3.BASE,
                    search_filter='(objectClass={})'.format(self.conf_objc),
                    attributes=[self.conf_dyn, self.conf_web_keys, self.conf_rev]
                    )

        result = self.ldap_conn.response
        attributes_s = 'attributes'
        ox_auth_conf_dynamic = json.loads(result[0][attributes_s][self.conf_dyn][0])
        self.key_store_secret = ox_auth_conf_dynamic[self.conf_keystore_secret]
        self.get_sig_enc_algs(json.loads(result[0][attributes_s][self.conf_web_keys][0]))
        self.revision = int(result[0][attributes_s][self.conf_rev][0])


    def obtain_data_sql(self):
        sql_type, sql_host, sql_port, sql_db = re.match(r'jdbc:(.*?):\/\/(.*?):(\d*?)\/(.*)$', self.credidentials['connection.uri']).groups()
        sql_db = sql_db.split('?')[0]
        sql_password = unobscure(self.credidentials['auth.userPassword'], key=self.salt)

        if sql_type == 'mysql':
        
            self.sql_conn = pymysql.connect(host=sql_host,
                             user=self.credidentials['auth.userName'],
                             password=sql_password,
                             database=sql_db,
                             charset='utf8mb4',
                             cursorclass=pymysql.cursors.DictCursor)

            self.cursor = self.sql_conn.cursor()
            sql = 'SELECT `{}`, `{}`, `{}` from `{}` WHERE `doc_id`=%s'.format(self.conf_dyn, self.conf_web_keys, self.conf_rev, self.conf_objc)
            self.cursor.execute(sql, (self.doc_id,))
            result = self.cursor.fetchone()

            ox_auth_conf_dynamic = json.loads(result[self.conf_dyn])
            self.key_store_secret = ox_auth_conf_dynamic[self.conf_keystore_secret]
            self.get_sig_enc_algs(json.loads(result[self.conf_web_keys]))
            self.revision = int(result[self.conf_rev])


    def obtain_data_couchbase(self):
        cb_host = self.credidentials['servers'].split(',')[0]
        cb_password = unobscure(self.credidentials['auth.userPassword'], key=self.salt)
        self.cbm = CBM(cb_host, self.credidentials['auth.userName'], cb_password)
        self.default_bucket = self.credidentials['bucket.default']
        result = self.cbm.exec_query('SELECT * FROM {} USE KEYS "{}"'.format(self.default_bucket, self.key))
        configuration_oxauth = result.json()
        results_s = 'results'
        self.key_store_secret = configuration_oxauth[results_s][0][self.default_bucket][self.conf_dyn][self.conf_keystore_secret]
        self.get_sig_enc_algs(configuration_oxauth[results_s][0][self.default_bucket][self.conf_web_keys])
        self.revision = configuration_oxauth[results_s][0][self.default_bucket][self.conf_rev]



    def generate_keys(self):

        if _VENDOR_ == 'jans':

            print("Creating empty JKS keystore")
            run_command([
                    self.keytool_cmd, '-genkey',
                    '-alias', 'dummy',
                    '-keystore', self.keystore_fn,
                    '-storepass', self.key_store_secret,
                    '-keypass', self.key_store_secret,
                    '-dname', '"{}"'.format(self.dnname)
                    ])

            print("Delete dummy key from JKS")
            run_command([
                    self.keytool_cmd, '-delete',
                    '-alias', 'dummy',
                    '-keystore', self.keystore_fn,
                    '-storepass', self.key_store_secret,
                    '-keypass', self.key_store_secret,
                    '-dname', '"{}"'.format(self.dnname)
                    ])

        print("Generating keys")
        args = [self.java_cmd, '-Dlog4j.defaultInitOverride=true',
                '-cp', self.client_jar_fn, self.key_gen_path,
                '-key_ops_type', 'ALL',
                '-keystore', self.keystore_fn,
                '-keypasswd', self.key_store_secret,
                '-sig_keys', ' '.join(self.sig_enc['sig']),
                '-enc_keys', ' '.join(self.sig_enc['enc']),
                '-dnname', '"{}"'.format(self.dnname)
                ]

        if argsp.expiration_hours:
            args += ['-expiration_hours', str(argsp.expiration_hours)]
        else:
            args += ['-expiration', str(argsp.expiration)]
            
        args += ['>', self.keys_json_fn]

        backup_file(self.keys_json_fn)

        run_command(args)

        with open(self.keys_json_fn) as f:
            self.keys_json = f.read()


    def validate_keys(self):

        print("Validating ... ")

        output = run_command([self.keytool_cmd, '-list', '-v',
                '-keystore', self.keystore_fn,
                '-storepass', self.key_store_secret,
                '|', 'grep', '"Alias name:"'
                ])


        jsk_aliases = []
        for l in output[0].splitlines():
            ls = l.decode().strip()
            n = ls.find(':')
            alias_name = ls[n+1:].strip()
            jsk_aliases.append(alias_name)


        keys = json.loads(self.keys_json)

        json_aliases = [ wkey['kid'] for wkey in keys['keys'] ]

        valid1 = True
        for alias_name in json_aliases:
            if alias_name not in jsk_aliases:
                print(keystore_fn, "does not contain", alias_name)
                valid1 = False

        valid2 = True
        for alias_name in jsk_aliases:
            if alias_name not in json_aliases:
                print(oxauth_keys_json_fn, "does not contain", alias_name)
                valid2 = False

        if valid1 and valid2:
            print("Content of {} and {} matches".format(self.keys_json_fn, self.keystore_fn))
        else:
            print("Validation failed, not updating db")
            sys.exit(1)

        # validation passed, we can copy keystore to /etc/certs
        run_command(['cp', '-f', self.keystore_fn, '/etc/certs'])


    def update_spanner(self):
        print("Updating Spanner db")
        self.spanner.put_data(self.conf_objc, ['doc_id', self.conf_rev, self.conf_web_keys], [[self.doc_id, str(self.revision+1), self.keys_json]])


    def update_ldap(self):

        print("LDAP modify", self.auth_config_dn)
        self.ldap_conn.modify(
                        self.auth_config_dn,
                        {
                            self.conf_web_keys: [ldap3.MODIFY_REPLACE, self.keys_json],
                            self.conf_rev: [ldap3.MODIFY_REPLACE, str(self.revision+1)]
                        }
                    )

        self.ldap_conn.unbind()


    def update_sql(self):
        print("Updating SQL db")
        sql = 'UPDATE `{}` SET `{}`=%s, `{}`=%s WHERE `doc_id`=%s'.format(self.conf_objc, self.conf_web_keys, self.conf_rev)
        self.cursor.execute(sql, (self.keys_json, self.revision+1, self.doc_id))
        self.sql_conn.commit()
        self.sql_conn.close()


    def update_couchbase(self):
        print("Updating Couchbase db")
        self.cbm.exec_query("UPDATE {0} USE KEYS '{1}' set {0}.{3}={2}".format(self.default_bucket, self.key, self.keys_json, self.conf_web_keys))
        self.cbm.exec_query("UPDATE {0} USE KEYS '{1}' set {0}.{3}={2}".format(self.default_bucket, self.key, self.revision+1, self.conf_rev))


key_regenerator = KeyRegenerator()
