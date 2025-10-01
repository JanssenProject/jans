# This script adds Jans users to rdbm backends

import uuid
import os
import time
import hashlib
import base64
import psycopg2
import pymysql
import logging
from joblib import Parallel, delayed


def get_logger(name):
    log_format = '%(asctime)s - %(name)8s - %(levelname)5s - %(message)s'
    logging.basicConfig(level=logging.INFO,
                        format=log_format,
                        filename='setup.log',
                        filemode='w')
    console = logging.StreamHandler()
    console.setLevel(logging.INFO)
    console.setFormatter(logging.Formatter(log_format))
    logging.getLogger(name).addHandler(console)
    return logging.getLogger(name)


logger = get_logger("rdbm-user-loader")

user_number_starting_point = int(os.environ.get("USER_NUMBER_STARTING_POINT", 0))
user_number_ending_point = int(os.environ.get("USER_NUMBER_ENDING_POINT", 50000000))
user_split_parallel_threads = int(os.environ.get("USER_SPLIT_PARALLEL_THREADS", 20))
logger.info(f"Starting to add users to {os.environ.get('RDBMS_TYPE', 'mysql')}. "
            f"This will add user{str(user_number_starting_point)} to user{str(user_number_ending_point)} ")

user_id_prefix = os.environ.get("TEST_USERS_PREFIX_STRING", "test_user")
db_type = os.environ.get("RDBMS_TYPE", "mysql")
# pgsql/mysql database settings #######
sql_host = os.environ.get("RDBMS_HOST", "localhost")
sql_db = os.environ.get("RDBMS_DB", "jans")
sql_user = os.environ.get("RDBMS_USER", "jans")
sql_password = os.environ.get("RDBMS_PASSWORD", "")

if db_type == 'pgsql':
    qchar = '"'
    schar = '\''

elif db_type == 'mysql':
    qchar = '`'
    schar = '"'


def connect():
    conn = None
    cur = None
    if db_type == 'pgsql':
        conn = psycopg2.connect(user=sql_user,
                                password=sql_password,
                                host=sql_host,
                                port="5432",
                                database=sql_db)
        cur = conn.cursor()

    elif db_type == 'mysql':
        conn = pymysql.connect(host=sql_host,
                               user=sql_user,
                               password=sql_password,
                               database=sql_db,
                               )
        cur = conn.cursor()

    if conn:
        logger.info(f"Connected to backend {db_type}")
        return conn, cur
    else:
        logger.error(f"Could not connect to backend {db_type}")
        SystemExit(1)


def split_interval(start, end, num_of_parts):
    part_interval = (end - start) / num_of_parts
    parts = []
    marker = start

    for _ in range(num_of_parts):
        part = [marker, marker + part_interval]
        marker += part_interval
        parts.append(part)
    return parts


def make_secret(password):
    salt = os.urandom(4)
    sha = hashlib.sha1(password.encode('utf-8'))
    sha.update(salt)
    digest_ = sha.digest()
    b64encoded = base64.b64encode(digest_ + salt).decode('utf-8')
    encrypted_password = '{{SSHA}}{0}'.format(b64encoded)
    return encrypted_password


def load_users(interval):
    logger.info("-------------------")
    logger.info("Thread {} started!".format(str(interval)))
    logger.info(time.ctime(time.time()))
    logger.info("-------------------")
    start = interval[0] + 1
    end = interval[1]
    sql_cmds = []
    while start <= end:
        inum = str(uuid.uuid4()).upper()
        name = user_id_prefix + str(int(start))
        sn = user_id_prefix + '_sn' + str(int(start))
        dn = 'inum={},ou=people,o=jans'.format(inum)
        username = name
        cn = name + ' ' + sn
        attributes = (
            ('doc_id', inum),
            ('dn', dn),
            ('objectClass', 'jansPerson'),
            ('cn', cn),
            ('sn', sn),
            ('uid', username),
            ('inum', inum),
            ('jansStatus', 'active'),
            ('userPassword', make_secret('topsecret' + str(int(start)))),
            ('mail', username + '@jans.io'),
            ('displayName', cn),
            ('givenName', name),
        )

        sql_attribs = ['{0}{1}{0}'.format(qchar, a[0]) for a in attributes]
        sql_vals = ['{0}{1}{0}'.format(schar, a[1]) for a in attributes]
        sql_cmd = 'INSERT INTO {0}jansPerson{0} ({1}) values ({2})'.format(qchar, ','.join(sql_attribs),
                                                                           ','.join(sql_vals))
        sql_cmds.append(sql_cmd)
        start += 1
    conn, cur = connect()
    for cmd in sql_cmds:
        try:
            cur.execute(cmd)
            conn.commit()
        except Exception as e:
            logger.error(f"{cmd} did not execute!")
            logger.error(e)
    conn.close()
    logger.info("-------------------")
    logger.info(time.ctime(time.time()))
    logger.info("Thread {} Ended!".format(str(interval)))
    logger.info("-------------------")


def main():
    user_numbers_intervals = split_interval(user_number_starting_point, user_number_ending_point,
                                            user_split_parallel_threads)
    results = Parallel(n_jobs=-1, backend="multiprocessing")(
        map(delayed(load_users), user_numbers_intervals))


if __name__ == "__main__":
    main()
