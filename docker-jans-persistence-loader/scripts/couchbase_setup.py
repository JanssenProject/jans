import json
import logging.config
import os
import time
from pathlib import Path

from jans.pycloudlib.persistence.couchbase import CouchbaseClient

from settings import LOGGING_CONFIG
from utils import prepare_template_ctx
from utils import get_ldif_mappings

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("couchbase_setup")


def get_bucket_mappings(manager):
    prefix = os.environ.get("CN_COUCHBASE_BUCKET_PREFIX", "jans")
    bucket_mappings = {
        "default": {
            "bucket": prefix,
            "mem_alloc": 100,
            # "document_key_prefix": [],
        },
        "user": {
            "bucket": f"{prefix}_user",
            "mem_alloc": 300,
            # "document_key_prefix": ["groups_", "people_", "authorizations_"],
        },
        "site": {
            "bucket": f"{prefix}_site",
            "mem_alloc": 100,
            # "document_key_prefix": ["site_", "cache-refresh_"],
        },
        "token": {
            "bucket": f"{prefix}_token",
            "mem_alloc": 300,
            # "document_key_prefix": ["tokens_"],
        },
        "cache": {
            "bucket": f"{prefix}_cache",
            "mem_alloc": 100,
            # "document_key_prefix": ["cache_"],
        },
        "session": {
            "bucket": f"{prefix}_session",
            "mem_alloc": 200,
            # "document_key_prefix": [],
        },
    }

    optional_scopes = json.loads(manager.config.get("optional_scopes", "[]"))
    ldif_mappings = get_ldif_mappings("couchbase", optional_scopes)

    for name, files in ldif_mappings.items():
        bucket_mappings[name]["files"] = files

    # persistence_type = os.environ.get("CN_PERSISTENCE_TYPE", "ldap")
    # ldap_mapping = os.environ.get("CN_PERSISTENCE_LDAP_MAPPING", "default")
    # if persistence_type == "hybrid":
    #     bucket_mappings = {
    #         name: mapping for name, mapping in bucket_mappings.items()
    #         if name != ldap_mapping
    #     }
    return bucket_mappings


class CouchbaseBackend:
    def __init__(self, manager):
        self.client = CouchbaseClient(manager)
        self.manager = manager
        self.index_num_replica = 0

    def create_buckets(self, bucket_mappings, bucket_type="couchbase"):
        sys_info = self.client.get_system_info()

        if not sys_info:
            raise RuntimeError("Unable to get system info from Couchbase; aborting ...")

        ram_info = sys_info["storageTotals"]["ram"]

        total_mem = (ram_info['quotaTotalPerNode'] - ram_info['quotaUsedPerNode']) / (1024 * 1024)
        # the minimum memory is a sum of required buckets + minimum mem for `gluu` bucket
        min_mem = sum(value["mem_alloc"] for value in bucket_mappings.values()) + 100

        logger.info("Memory size per node for Couchbase buckets was determined as {} MB".format(total_mem))
        logger.info("Minimum memory size per node for Couchbase buckets was determined as {} MB".format(min_mem))

        if total_mem < min_mem:
            logger.warning("Available quota on couchbase node is less than {} MB".format(min_mem))

        persistence_type = os.environ.get("CN_PERSISTENCE_TYPE", "ldap")
        ldap_mapping = os.environ.get("CN_PERSISTENCE_LDAP_MAPPING", "default")

        # always create `jans` bucket even when `default` mapping stored in LDAP
        if persistence_type == "hybrid" and ldap_mapping == "default":
            memsize = 100

            logger.info("Creating bucket {0} with type {1} and RAM size {2}".format("jans", bucket_type, memsize))
            prefix = os.environ.get("CN_COUCHBASE_BUCKET_PREFIX", "jans")
            req = self.client.add_bucket(prefix, memsize, bucket_type)
            if not req.ok:
                logger.warning("Failed to create bucket {}; reason={}".format("jans", req.text))

        req = self.client.get_buckets()
        if req.ok:
            remote_buckets = tuple(bckt["name"] for bckt in req.json())
        else:
            remote_buckets = ()

        for _, mapping in bucket_mappings.items():
            if mapping["bucket"] in remote_buckets:
                continue

            memsize = int((mapping["mem_alloc"] / float(min_mem)) * total_mem)

            logger.info("Creating bucket {0} with type {1} and RAM size {2}".format(mapping["bucket"], bucket_type, memsize))
            req = self.client.add_bucket(mapping["bucket"], memsize, bucket_type)
            if not req.ok:
                logger.warning("Failed to create bucket {}; reason={}".format(mapping["bucket"], req.text))

    def create_indexes(self, bucket_mappings):
        buckets = [mapping["bucket"] for _, mapping in bucket_mappings.items()]
        prefix = os.environ.get("CN_COUCHBASE_BUCKET_PREFIX", "jans")

        with open("/app/static/couchbase/index.json") as f:
            txt = f.read().replace("!bucket_prefix!", prefix)
            indexes = json.loads(txt)

        for bucket in buckets:
            if bucket not in indexes:
                continue

            query_file = "/app/tmp/index_{}.n1ql".format(bucket)

            logger.info("Running Couchbase index creation for {} bucket (if not exist)".format(bucket))

            with open(query_file, "w") as f:
                index_list = indexes.get(bucket, {})
                index_names = []

                for index in index_list.get("attributes", []):
                    if '(' in ''.join(index):
                        attr_ = index[0]
                        index_name_ = index[0].replace('(', '_').replace(')', '_').replace('`', '').lower()
                        if index_name_.endswith('_'):
                            index_name_ = index_name_[:-1]
                        index_name = 'def_{0}_{1}'.format(bucket, index_name_)
                    else:
                        attr_ = ','.join(['`{}`'.format(a) for a in index])
                        index_name = "def_{0}_{1}".format(bucket, '_'.join(index))

                    f.write(
                        'CREATE INDEX %s ON `%s`(%s) USING GSI WITH {"defer_build":true,"num_replica": %s};\n' % (index_name, bucket, attr_, self.index_num_replica)
                    )

                    index_names.append(index_name)

                if index_names:
                    f.write('BUILD INDEX ON `%s` (%s) USING GSI;\n' % (bucket, ', '.join(index_names)))

                sic = 1
                for attribs, wherec in index_list.get("static", []):
                    attrquoted = []

                    for a in attribs:
                        if '(' not in a:
                            attrquoted.append('`{}`'.format(a))
                        else:
                            attrquoted.append(a)
                    attrquoteds = ', '.join(attrquoted)

                    f.write(
                        'CREATE INDEX `{0}_static_{1:02d}` ON `{0}`({2}) WHERE ({3}) WITH {{ "num_replica": {4} }}\n'.format(bucket, sic, attrquoteds, wherec, self.index_num_replica)
                    )
                    sic += 1

            # exec query
            with open(query_file) as f:
                for line in f:
                    query = line.strip()
                    if not query:
                        continue

                    req = self.client.exec_query(query)
                    if not req.ok:
                        # the following code should be ignored
                        # - 4300: index already exists
                        error = req.json()["errors"][0]
                        if error["code"] in (4300,):
                            continue
                        logger.warning(f"Failed to execute query, reason={error['msg'].strip()}")  # .format(error["msg"]))

    def import_builtin_ldif(self, bucket_mappings, ctx):
        for _, mapping in bucket_mappings.items():
            for file_ in mapping["files"]:
                self._import_ldif(f"/app/templates/{file_}", ctx)

    def initialize(self):
        num_replica = int(os.environ.get("CN_COUCHBASE_INDEX_NUM_REPLICA", 0))
        num_indexer_nodes = len(self.client.get_index_nodes())

        if num_replica >= num_indexer_nodes:
            raise ValueError(f"Number of index replica ({num_replica}) must be less than available indexer nodes ({num_indexer_nodes})")

        self.index_num_replica = num_replica

        bucket_mappings = get_bucket_mappings(self.manager)

        time.sleep(5)
        self.create_buckets(bucket_mappings)

        time.sleep(5)
        self.create_indexes(bucket_mappings)

        time.sleep(5)
        ctx = prepare_template_ctx(self.manager)
        self.import_builtin_ldif(bucket_mappings, ctx)
        self.import_custom_ldif(ctx)

        time.sleep(5)
        self.create_couchbase_shib_user()

    def create_couchbase_shib_user(self):
        self.client.create_user(
            'couchbaseShibUser',
            self.manager.secret.get("couchbase_shib_user_password"),
            'Shibboleth IDP',
            'query_select[*]',
        )

    def import_custom_ldif(self, ctx):
        custom_dir = Path("/app/custom_ldif")

        for file_ in custom_dir.rglob("*.ldif"):
            self._import_ldif(file_, ctx)

    def _import_ldif(self, path, ctx):
        logger.info(f"Importing {path} file")
        self.client.create_from_ldif(path, ctx)
