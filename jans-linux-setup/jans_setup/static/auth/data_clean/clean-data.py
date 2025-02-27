#!/usr/bin/python3

import os
import sys
import shutil
import argparse
import subprocess
import configparser

parser = argparse.ArgumentParser(description="This script removes Janssen Server tokens")
parser.add_argument('-limit', help="Limit to delete entry per execution", type=int, default=1000)
parser.add_argument('--yes', help="For execute without prompt", action='store_true')
argsp = parser.parse_args()

config = configparser.ConfigParser()
config.read('/etc/jans/data-clean.ini')
tables = config['main']['tables'].split()

if not argsp.yes:
    print(f"This command will remove first {argsp.limit} entires of the following tables where expiration is before than now")
    print(', '.join(tables))
    response = input("Are you sure you want to do this? Type yes to approve. ")
    if not response == 'yes':
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

if db_type == 'mysql':
    mysql_cmd = shutil.which('mysql')
    cmd = f'{mysql_cmd} --user={db_user} --host={db_host} --port={db_port} --password=$db_user_pw {db_name} -e '
    for table in tables:
        qcmd = cmd + f'"DELETE FROM {table} WHERE del=TRUE AND exp < NOW() LIMIT {argsp.limit};"'
        print(f"Executing command: {qcmd}")
        subprocess.run(qcmd, shell=True, env={'db_user_pw': db_user_pw})
