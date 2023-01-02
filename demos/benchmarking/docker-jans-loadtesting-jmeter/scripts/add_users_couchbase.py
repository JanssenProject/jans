import base64
import os
import hashlib
import uuid
import time
import logging
from couchbase.cluster import Cluster, ClusterOptions
from couchbase.cluster import PasswordAuthenticator
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


logger = get_logger("cb-user-loader")

user_number_starting_point = int(os.environ.get("USER_NUMBER_STARTING_POINT", 0))
user_number_ending_point = int(os.environ.get("USER_NUMBER_ENDING_POINT", 50000000))
user_split_parallel_threads = int(os.environ.get("USER_SPLIT_PARALLEL_THREADS", 20))
user_id_prefix = os.environ.get("TEST_USERS_PREFIX_STRING", "test_user")
logger.info("Starting to add users to Couchbase. This will add user{} to user{} ".format(
    str(user_number_starting_point), str(user_number_ending_point)))
couchbase_url = os.environ.get("COUCHBASE_URL", "cb.cbns.svc.cluster.local")
couchbase_pw = os.environ.get("COUCHBASE_PW", "Test1234#")

cluster = Cluster('couchbase://' + couchbase_url, ClusterOptions(PasswordAuthenticator('admin', couchbase_pw)))
cb = cluster.bucket('jans_user')


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
    while start <= end:
        inum = str(uuid.uuid4())
        name = '{}{}'.format(user_id_prefix, int(start))
        sn = 'lastname{}'.format(int(start))
        dn = "inum={0},ou=people,o=jans".format(inum)
        key = 'people_' + inum
        cn = name + ' ' + sn

        user_data = {
            "userPassword": make_secret('topsecret' + str(int(start))),
            "mail": name + '@jans.io',
            "displayName": name + ' ' + sn,
            "givenName": name,
            "objectClass": "jansPerson",
            "dn": dn,
            "cn": cn,
            "inum": inum,
            "uid": name,
            "jansStatus": "active",
            "sn": sn
        }
        try:
            cb.insert(key, user_data)
        except Exception as e:
            logger.error(e)
        start += 1
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
