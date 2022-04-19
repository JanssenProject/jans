import contextlib
import datetime
import json
import logging.config
import os
import time
from pathlib import Path

from ldif import LDIFParser

from jans.pycloudlib.persistence.couchbase import get_couchbase_user
from jans.pycloudlib.persistence.couchbase import get_couchbase_superuser
from jans.pycloudlib.persistence.couchbase import get_couchbase_password
from jans.pycloudlib.persistence.couchbase import get_couchbase_superuser_password
from jans.pycloudlib.persistence.couchbase import CouchbaseClient

from settings import LOGGING_CONFIG
from utils import prepare_template_ctx
from utils import render_ldif
from utils import get_ldif_mappings
from utils import id_from_dn

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("entrypoint")


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
    ldif_mappings = get_ldif_mappings(optional_scopes)

    for name, files in ldif_mappings.items():
        bucket_mappings[name]["files"] = files

    persistence_type = os.environ.get("CN_PERSISTENCE_TYPE", "ldap")
    ldap_mapping = os.environ.get("CN_PERSISTENCE_LDAP_MAPPING", "default")
    if persistence_type == "hybrid":
        bucket_mappings = {
            name: mapping for name, mapping in bucket_mappings.items()
            if name != ldap_mapping
        }
    return bucket_mappings


class AttrProcessor(object):
    def __init__(self):
        self._attrs = {}

    @property
    def syntax_types(self):
        return {
            '1.3.6.1.4.1.1466.115.121.1.7': 'boolean',
            '1.3.6.1.4.1.1466.115.121.1.27': 'integer',
            '1.3.6.1.4.1.1466.115.121.1.24': 'datetime',
        }

    def process(self):
        attrs = {}

        with open("/app/schema/opendj_types.json") as f:
            attr_maps = json.loads(f.read())
            for type_, names in attr_maps.items():
                for name in names:
                    attrs[name] = {"type": type_, "multivalued": False}

        with open("/app/schema/jans_schema.json") as f:
            schemas = json.loads(f.read()).get("attributeTypes", {})
            for schema in schemas:
                if schema.get("json"):
                    type_ = "json"
                elif schema["syntax"] in self.syntax_types:
                    type_ = self.syntax_types[schema["syntax"]]
                else:
                    type_ = "string"

                multivalued = schema.get("multivalued", False)
                for name in schema["names"]:
                    attrs[name] = {
                        "type": type_,
                        "multivalued": multivalued,
                    }

        # override `member`
        attrs["member"]["multivalued"] = True
        return attrs

    @property
    def attrs(self):
        if not self._attrs:
            self._attrs = self.process()
        return self._attrs

    def is_multivalued(self, name):
        return self.attrs.get(name, {}).get("multivalued", False)

    def get_type(self, name):
        return self.attrs.get(name, {}).get("type", "string")


def transform_values(name, values, attr_processor):
    def as_dict(val):
        return json.loads(val)

    def as_bool(val):
        return val.lower() in ("true", "yes", "1", "on")

    def as_int(val):
        try:
            val = int(val)
        except (TypeError, ValueError):
            pass
        return val

    def as_datetime(val):
        if '.' in val:
            date_format = '%Y%m%d%H%M%S.%fZ'
        else:
            date_format = '%Y%m%d%H%M%SZ'

        if not val.lower().endswith('z'):
            val += 'Z'

        dt = datetime.datetime.strptime(val, date_format)
        return dt.isoformat()

    callbacks = {
        "json": as_dict,
        "boolean": as_bool,
        "integer": as_int,
        "datetime": as_datetime,
    }

    type_ = attr_processor.get_type(name)
    callback = callbacks.get(type_)

    # maybe string
    if not callable(callback):
        return values
    return [callback(item) for item in values]


def transform_entry(entry, attr_processor):
    for k, v in entry.items():
        v = transform_values(k, v, attr_processor)

        if len(v) == 1 and attr_processor.is_multivalued(k) is False:
            entry[k] = v[0]

        if k != "objectClass":
            continue

        entry[k].remove("top")
        ocs = entry[k]

        for oc in ocs:
            remove_oc = any(["Custom" in oc, "jans" not in oc.lower()])
            if len(ocs) > 1 and remove_oc:
                ocs.remove(oc)
        entry[k] = ocs[0]
    return entry


class CouchbaseBackend:
    def __init__(self, manager):
        hostname = os.environ.get("CN_COUCHBASE_URL", "localhost")
        user = get_couchbase_superuser(manager) or get_couchbase_user(manager)

        password = ""
        with contextlib.suppress(FileNotFoundError):
            password = get_couchbase_superuser_password(manager)
        password = password or get_couchbase_password(manager)

        self.client = CouchbaseClient(hostname, user, password)
        self.manager = manager
        self.index_num_replica = 0
        self.attr_processor = AttrProcessor()

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
                        logger.warning("Failed to execute query, reason={}".format(error["msg"]))

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
        src = Path(path).resolve()

        # generated template will be saved under ``/app/tmp`` directory
        # examples:
        # - ``/app/templates/groups.ldif`` will be saved as ``/app/tmp/templates/groups.ldif``
        # - ``/app/custom_ldif/groups.ldif`` will be saved as ``/app/tmp/custom_ldif/groups.ldif``
        dst = Path("/app/tmp").joinpath(str(src).removeprefix("/app/")).resolve()

        # ensure directory for generated template is exist
        dst.parent.mkdir(parents=True, exist_ok=True)

        logger.info(f"Importing {src} file")
        render_ldif(src, dst, ctx)

        with open(dst, "rb") as fd:
            parser = LDIFParser(fd)

            for dn, entry in parser.parse():
                if len(entry) <= 2:
                    continue

                key = id_from_dn(dn)
                bucket = get_bucket_for_key(key)
                entry["dn"] = [dn]
                entry = transform_entry(entry, self.attr_processor)
                data = json.dumps(entry)

                # TODO: get the bucket based on key prefix
                # using INSERT will cause duplication error, but the data is left intact
                query = 'INSERT INTO `%s` (KEY, VALUE) VALUES ("%s", %s)' % (bucket, key, data)
                req = self.client.exec_query(query)

                if not req.ok:
                    logger.warning("Failed to execute query, reason={}".format(req.json()))


def get_bucket_for_key(key):
    bucket_prefix = os.environ.get("CN_COUCHBASE_BUCKET_PREFIX", "jans")

    cursor = key.find("_")
    key_prefix = key[:cursor + 1]

    if key_prefix in ("groups_", "people_", "authorizations_"):
        bucket = f"{bucket_prefix}_user"
    elif key_prefix in ("site_", "cache-refresh_"):
        bucket = f"{bucket_prefix}_site"
    elif key_prefix in ("tokens_",):
        bucket = f"{bucket_prefix}_token"
    elif key_prefix in ("cache_",):
        bucket = f"{bucket_prefix}_cache"
    else:
        bucket = bucket_prefix
    return bucket
