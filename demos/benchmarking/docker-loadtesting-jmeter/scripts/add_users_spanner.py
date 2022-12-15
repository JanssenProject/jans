"""

The following envvars are required:

- ``GOOGLE_APPLICATION_CREDENTIALS``: Path to JSON file contains
  Google credentials
- ``GOOGLE_PROJECT_ID``: (a.k.a Google project ID)
- ``GOOGLE_SPANNER_INSTANCE_ID``: Spanner instance ID
- ``GOOGLE_SPANNER_DATABASE_ID``: Spanner database ID
"""

import base64
import os
import hashlib
import uuid
import time
import logging
from google.cloud import spanner
from joblib import Parallel, delayed
from contextlib import suppress


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


logger = get_logger("spanner-user-loader")
cred_file = os.environ.get("GOOGLE_APPLICATION_CREDENTIALS", "/etc/certs/google-service-account.json")
user_number_starting_point = int(os.environ.get("USER_NUMBER_STARTING_POINT", 0))
user_number_ending_point = int(os.environ.get("USER_NUMBER_ENDING_POINT", 50000000))
user_split_parallel_threads = int(os.environ.get("USER_SPLIT_PARALLEL_THREADS", 20))
user_id_prefix = os.environ.get("TEST_USERS_PREFIX_STRING", "test_user")
logger.info("Starting to add users to Spanner. This will add user{} to user{} ".format(
    str(user_number_starting_point), str(user_number_ending_point)))
project_id = os.environ.get("GOOGLE_PROJECT_ID", "")
client = spanner.Client(project=project_id)
instance_id = os.environ.get("GOOGLE_SPANNER_INSTANCE_ID", "cn-test")
instance = client.instance(instance_id)

database_id = os.environ.get("GOOGLE_SPANNER_DATABASE_ID", "load")
database = instance.database(database_id)


def connected():
    """Check whether connection is alive by executing simple query.
    """

    cntr = 0
    with database.snapshot() as snapshot:
        result = snapshot.execute_sql("SELECT 1")
        with suppress(IndexError):
            row = list(result)[0]
            cntr = row[0]
    return cntr > 0


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
    columns = ['userPassword', 'mail', 'displayName', 'givenName', 'objectClass', 'dn',
               'cn', 'inum', 'doc_id', 'uid', 'jansStatus', 'sn']
    logger.info("-------------------")
    logger.info("Thread {} started!".format(str(interval)))
    logger.info("Preparing query {} started!".format(str(interval)))
    logger.info(time.ctime(time.time()))
    logger.info("-------------------")
    start = interval[0] + 1
    end = interval[1]
    while start <= end:
        values = []
        inum = str(uuid.uuid4())
        name = '{}{}'.format(user_id_prefix, int(start))
        sn = 'lastname{}'.format(int(start))
        dn = "inum={0},ou=people,o=jans".format(inum)
        cn = name + ' ' + sn
        people = [
            make_secret('topsecret' + str(int(start))),
            name + '@jans.io',
            name + ' ' + sn,
            name,
            "jansPerson",
            dn,
            cn,
            inum,
            inum,
            name,
            "active",
            sn
        ]
        values.append(people)
        try:
            with database.batch() as batch:
                batch.insert(table='jansPerson', columns=columns, values=values)
        except Exception as e:
            logger.error(e)
        start += 1
    logger.info("-------------------")
    logger.info(time.ctime(time.time()))
    logger.info("Thread {} Ended!".format(str(interval)))
    logger.info("-------------------")

def main():
    test_spanner_connection = connected()
    if not test_spanner_connection:
        raise Exception("Spanner backend is unreachable")
    # The transaction contains too many mutations. Insert and update operations count with the multiplicity of the
    # number of columns they affect. For example, inserting values into one key column and four non-key columns count
    # as five mutations total for the insert. Delete and delete range operations count as one mutation regardless of
    # the number of columns affected. The total mutation count includes any changes to indexes that the transaction
    # generates. Please reduce the number of writes, or use fewer indexes. (Maximum number: 20000)
    user_numbers_intervals = split_interval(user_number_starting_point, user_number_ending_point,
                                            user_split_parallel_threads)
    results = Parallel(n_jobs=-1, backend="multiprocessing")(
        map(delayed(load_users), user_numbers_intervals))


if __name__ == "__main__":
    main()
