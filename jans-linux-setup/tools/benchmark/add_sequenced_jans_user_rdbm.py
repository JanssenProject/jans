# This script adds Jans users to rdbm backends

import sys
import uuid
import json
import random
import os
import hashlib
import base64
import psycopg2
import pymysql

# Set number of users to be created
N = 100

#Users ID's will start with this string, e.g., test_user1, test_user2, test_user3 ...
userId = 'test_user'

# All users will have this password
userSecret = 'test_user_password' 

# Set either pgsql or mysql
db_type = 'mysql'

# pgsql/mysql database settings #######
sql_host = 'localhost'
sql_db = 'jansdb'
sql_user = 'jans'
sql_password = 'w5IWm03tT7Za'
########################################


if db_type == 'pgsql':
    qchar = '"'
    schar = '\''
    conn = psycopg2.connect(user=sql_user,
                            password=sql_password,
                            host=sql_host,
                            port="5432",
                            database=sql_db)
    cur = conn.cursor()

elif db_type == 'mysql':
    qchar = '`'
    schar = '"'
    conn = pymysql.connect(host=sql_host,
                            user=sql_user,
                            password=sql_password,
                            database=sql_db,
                            )
    cur = conn.cursor()

def make_secret(password):

    salt = os.urandom(4)
    sha = hashlib.sha1(password.encode('utf-8'))
    sha.update(salt)
    digest_ = sha.digest()
    b64encoded = base64.b64encode(digest_+salt).decode('utf-8')
    encrypted_password = '{{SSHA}}{0}'.format(b64encoded)
    return encrypted_password

user_secret_ssha = make_secret(userSecret)


i = 0
while i < N:
    i += 1
    inum = str(uuid.uuid4()).upper()
    name = userId + str(i)
    sn = userId + '_sn' + str(i)

    dn='inum={},ou=people,o=jans'.format(inum)

    username = name
    cn = name + ' ' + sn

    attributes= (
            ('doc_id', inum),
            ('dn', dn),
             ('objectClass', 'jansPerson'),
             ('cn', cn),
             ('sn', sn),
             ('uid', username),
             ('inum', inum),
             ('jansStatus', 'active'),
             ('userPassword', user_secret_ssha),
             ('mail', username+'@jans.io'),
             ('displayName', cn),
             ('givenName', name),
            )


    sql_attribs = ['{0}{1}{0}'.format(qchar, a[0]) for a in attributes]
    sql_vals = ['{0}{1}{0}'.format(schar, a[1]) for a in attributes]
    sql_cmd = 'INSERT INTO {0}jansPerson{0} ({1}) values ({2})'.format(qchar, ','.join(sql_attribs), ','.join(sql_vals))

    if db_type in ('pgsql', 'mysql'):
        cur.execute(sql_cmd)
        conn.commit()
        print("Added", username)


if db_type in ('pgsql', 'mysql'):
    conn.close()
