#!/usr/bin/python3

import os
import sys
import shutil
import logging
import argparse
import subprocess
import configparser

from logging.handlers import RotatingFileHandler


parser = argparse.ArgumentParser(description="This script removes Janssen Server tokens")
parser.add_argument('-limit', help="Limit to delete entry per execution", type=int, default=1000)
parser.add_argument('--yes', help="For execute without prompt", action='store_true')
argsp = parser.parse_args()

cleaner_dir = '/opt/jans/data-cleaner'
log_dir = os.path.join(cleaner_dir, 'logs')
cleaner_tmp_dir = os.path.join(cleaner_dir, 'tmp')
cmd_fn = os.path.join(cleaner_tmp_dir, f'data-clean-{os.urandom(8).hex()}.sql')

if not os.path.exists(log_dir):
    os.makedirs(log_dir)

if not os.path.exists(cleaner_tmp_dir):
    os.makedirs(cleaner_tmp_dir)

my_logger = logging.getLogger('Janns Data Cleaner')
my_logger.setLevel(logging.DEBUG)
handler = RotatingFileHandler(os.path.join(log_dir, 'data-clean.log'), maxBytes=50*1024*1024, backupCount=10)
formatter=logging.Formatter('%(asctime)s %(levelname)s\t%(message)s')
handler.setFormatter(formatter)
my_logger.addHandler(handler)


config = configparser.ConfigParser()
config_fn = os.path.join(cleaner_dir, 'data-clean.ini')
if not os.path.exists(config_fn):
    my_logger.error("Config file %s not found", config_fn)
    sys.exit()

try:
    config.read(config_fn)
    tables = config['main']['tables'].split()
except Exception as e:
    my_logger.error(e)
    sys.exit()

clnt_last_access_interval = config['main'].get('cleanUpInactiveClientAfterHoursOfInactivity')

if not argsp.yes:
    print(f"This command will remove first {argsp.limit} entires of the following tables where expiration is before than now")
    print(', '.join(tables))
    response = input("Are you sure you want to do this? Type yes to approve. ")
    if response != 'yes':
        print("Exiting without doing anyting...")
        sys.exit()

def read_prop(prop_fn):
    prop_dict = {}
    with open(prop_fn) as f:
        for l in f:
            n = l.find('=')
            if n < 0:
                n = l.find(':')
            if n > 0:
                key = l[:n].strip()
                val = l[n+1:].strip()
                prop_dict[key] = val
    return prop_dict

jans_sql_prop = read_prop('/etc/jans/conf/jans-sql.properties')

connection_uri_list = jans_sql_prop['connection.uri'].split(':')
db_type = connection_uri_list[1]
db_name = connection_uri_list[3].split('/')[1].strip().split('?')[0]
db_host = connection_uri_list[2].strip('/')
db_port = connection_uri_list[3].split('/')[0].strip()
db_user = jans_sql_prop['auth.userName']
db_user_pw_enc = jans_sql_prop['auth.userPassword']
db_user_pw = os.popen(f'/opt/jans/bin/encode.py -D {db_user_pw_enc}').read().strip()

def run_command(cmd, env):
    my_logger.info('Executing %s', cmd)

    output = subprocess.run(cmd, env=env, shell=True, capture_output=True)
    if output.stdout:
        my_logger.debug(output.stdout.decode())
    if output.stderr:
        my_logger.error(output.stderr.decode())


if db_type == 'mysql':
    mysql_cmd = shutil.which('mysql')
    cmd = f'{mysql_cmd} -vv --user={db_user} --host={db_host} --port={db_port} {db_name} < {cmd_fn}'

    with open(cmd_fn, 'w') as w:
        for table in tables:
            sql_query = f'''DELETE FROM {table} WHERE del=TRUE AND exp < NOW() LIMIT {argsp.limit};\n'''
            w.write(sql_query)
            if table == 'jansClnt' and clnt_last_access_interval:
                sql_query = f'''DELETE FROM {table} WHERE del=TRUE AND jansLastAccessTime < DATE_SUB(NOW(), INTERVAL {clnt_last_access_interval} HOUR) LIMIT {argsp.limit};\n'''
                w.write(sql_query)
    run_command(cmd, env={'MYSQL_PWD': db_user_pw})
    os.remove(cmd_fn)

elif db_type == 'postgresql':
    with open(cmd_fn, 'w') as w:
        pgsql_cmd = shutil.which('psql')
        cmd = f'{pgsql_cmd} -a -b -e --user={db_user} --host={db_host} --port={db_port} --dbname={db_name} -f {cmd_fn}'
        for table in tables:
            sql_query = f'''DELETE FROM "{table}" WHERE "doc_id" IN (SELECT "doc_id" FROM "{table}" WHERE "del"=TRUE and "exp" < NOW() LIMIT {argsp.limit});\n'''
            w.write(sql_query)
            if table == 'jansClnt' and clnt_last_access_interval:
                sql_query = f'''DELETE FROM "{table}" WHERE "doc_id" IN (SELECT "doc_id" FROM "{table}" WHERE "del"=TRUE and "jansLastAccessTime" < (NOW() - INTERVAL '{clnt_last_access_interval}' HOUR) LIMIT {argsp.limit});\n'''
                w.write(sql_query)

    run_command(cmd, env={'PGPASSWORD': db_user_pw})
    os.remove(cmd_fn)

else:
    sys.stderr.write(f"Database {db_type} is not supported by this script.\n")
