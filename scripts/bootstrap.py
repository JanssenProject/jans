import contextlib
import datetime
import json
import logging
import logging.config
import os
import time
from collections import OrderedDict

from ldap3 import BASE
from ldap3 import Connection
from ldap3 import Server
from ldap3 import SUBTREE
from ldap3.core.exceptions import LDAPSessionTerminatedByServerError
from ldap3.core.exceptions import LDAPSocketOpenError
from ldif3 import LDIFParser

from jans.pycloudlib import get_manager
from jans.pycloudlib.utils import decode_text
from jans.pycloudlib.utils import encode_text
from jans.pycloudlib.utils import safe_render
from jans.pycloudlib.utils import generate_base64_contents
from jans.pycloudlib.utils import as_boolean
from jans.pycloudlib.persistence.couchbase import get_couchbase_user
from jans.pycloudlib.persistence.couchbase import get_couchbase_superuser
from jans.pycloudlib.persistence.couchbase import get_couchbase_password
from jans.pycloudlib.persistence.couchbase import get_couchbase_superuser_password
from jans.pycloudlib.persistence.couchbase import CouchbaseClient

from settings import LOGGING_CONFIG

CN_CACHE_TYPE = os.environ.get("CN_CACHE_TYPE", "NATIVE_PERSISTENCE")
CN_REDIS_URL = os.environ.get('CN_REDIS_URL', 'localhost:6379')
CN_REDIS_TYPE = os.environ.get('CN_REDIS_TYPE', 'STANDALONE')
CN_REDIS_USE_SSL = os.environ.get("CN_REDIS_USE_SSL", False)
CN_REDIS_SSL_TRUSTSTORE = os.environ.get("CN_REDIS_SSL_TRUSTSTORE", "")
CN_REDIS_SENTINEL_GROUP = os.environ.get("CN_REDIS_SENTINEL_GROUP", "")

CN_MEMCACHED_URL = os.environ.get('CN_MEMCACHED_URL', 'localhost:11211')

CN_OXTRUST_CONFIG_GENERATION = os.environ.get("CN_OXTRUST_CONFIG_GENERATION", True)
CN_PERSISTENCE_TYPE = os.environ.get("CN_PERSISTENCE_TYPE", "couchbase")
CN_PERSISTENCE_LDAP_MAPPING = os.environ.get("CN_PERSISTENCE_LDAP_MAPPING", "default")
CN_LDAP_URL = os.environ.get("CN_LDAP_URL", "localhost:1636")

CN_PASSPORT_ENABLED = os.environ.get("CN_PASSPORT_ENABLED", False)
CN_RADIUS_ENABLED = os.environ.get("CN_RADIUS_ENABLED", False)
CN_CASA_ENABLED = os.environ.get("CN_CASA_ENABLED", False)
CN_SAML_ENABLED = os.environ.get("CN_SAML_ENABLED", False)
CN_SCIM_ENABLED = os.environ.get("CN_SCIM_ENABLED", False)
# CN_SCIM_TEST_MODE = os.environ.get("CN_SCIM_TEST_MODE", False)

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("entrypoint")


def get_key_from(dn):
    # for example: `"inum=29DA,ou=attributes,o=jans"`
    # becomes `["29DA", "attributes"]`
    dns = [i.split("=")[-1] for i in dn.split(",") if i != "o=jans"]
    dns.reverse()

    # the actual key
    return '_'.join(dns) or "_"


def get_bucket_mappings():
    prefix = os.environ.get("CN_COUCHBASE_BUCKET_PREFIX", "jans")
    bucket_mappings = OrderedDict({
        "default": {
            "bucket": prefix,
            "files": [
                "base.ldif",
                "attributes.ldif",
                "scopes.ldif",
                "scripts.ldif",
                "configuration.ldif",
                "jans-auth/configuration.ldif",
                "jans-auth/clients.ldif",
                "jans-fido2/configuration.ldif",
                "jans-scim/configuration.ldif",
                "jans-scim/scopes.ldif",
                "jans-scim/clients.ldif",
                "jans-config-api/scopes.ldif",
                "jans-config-api/clients.ldif",
                # "oxidp.ldif",
                # "passport.ldif",
                # "oxpassport-config.ldif",
                # "jans_radius_base.ldif",
                # "jans_radius_server.ldif",
                # "clients.ldif",
                "o_metric.ldif",
                # "jans_radius_clients.ldif",
                # "passport_clients.ldif",
                # "casa.ldif",
                # "scripts_casa.ldif",
            ],
            "mem_alloc": 100,
            "document_key_prefix": [],
        },
        "user": {
            "bucket": f"{prefix}_user",
            "files": [
                "people.ldif",
                "groups.ldif",
            ],
            "mem_alloc": 300,
            "document_key_prefix": ["groups_", "people_", "authorizations_"],
        },
        "site": {
            "bucket": f"{prefix}_site",
            "files": [
                "o_site.ldif",
            ],
            "mem_alloc": 100,
            "document_key_prefix": ["site_", "cache-refresh_"],
        },
        "token": {
            "bucket": f"{prefix}_token",
            "files": [],
            "mem_alloc": 300,
            "document_key_prefix": ["tokens_"],
        },
        "cache": {
            "bucket": f"{prefix}_cache",
            "files": [],
            "mem_alloc": 100,
            "document_key_prefix": ["cache_"],
        },
        "session": {
            "bucket": f"{prefix}_session",
            "files": [],
            "mem_alloc": 200,
            "document_key_prefix": [],
        },

    })

    if CN_PERSISTENCE_TYPE != "couchbase":
        bucket_mappings = OrderedDict({
            name: mapping for name, mapping in bucket_mappings.items()
            if name != CN_PERSISTENCE_LDAP_MAPPING
        })
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

        with open("/app/static/opendj/opendj_types.json") as f:
            attr_maps = json.loads(f.read())
            for type_, names in attr_maps.items():
                for name in names:
                    attrs[name] = {"type": type_, "multivalued": False}

        with open("/app/static/jans_schema.json") as f:
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


def render_ldif(src, dst, ctx):
    with open(src) as f:
        txt = f.read()

    with open(dst, "w") as f:
        f.write(safe_render(txt, ctx))


def get_jackrabbit_rmi_url():
    # backward-compat
    if "CN_JCA_RMI_URL" in os.environ:
        return os.environ["CN_JCA_RMI_URL"]

    # new style ENV
    rmi_url = os.environ.get("CN_JACKRABBIT_RMI_URL", "")
    if rmi_url:
        return rmi_url

    # fallback to default
    base_url = os.environ.get("CN_JACKRABBIT_URL", "http://localhost:8080")
    return f"{base_url}/rmi"


def get_jackrabbit_creds():
    username = os.environ.get("CN_JACKRABBIT_ADMIN_ID", "admin")
    password = ""

    password_file = os.environ.get(
        "CN_JACKRABBIT_ADMIN_PASSWORD_FILE",
        "/etc/jans/conf/jackrabbit_admin_password",
    )
    with contextlib.suppress(FileNotFoundError):
        with open(password_file) as f:
            password = f.read().strip()
    password = password or username
    return username, password


def get_base_ctx(manager):
    redis_pw = manager.secret.get("redis_pw") or ""
    redis_pw_encoded = ""

    if redis_pw:
        redis_pw_encoded = encode_text(
            redis_pw,
            manager.secret.get("encoded_salt"),
        ).decode()

    doc_store_type = os.environ.get("CN_DOCUMENT_STORE_TYPE", "LOCAL")
    jca_user, jca_pw = get_jackrabbit_creds()

    jca_pw_encoded = encode_text(
        jca_pw,
        manager.secret.get("encoded_salt"),
    ).decode()

    ctx = {
        'cache_provider_type': CN_CACHE_TYPE,
        'redis_url': CN_REDIS_URL,
        'redis_type': CN_REDIS_TYPE,
        'redis_pw': redis_pw,
        'redis_pw_encoded': redis_pw_encoded,
        "redis_use_ssl": "{}".format(as_boolean(CN_REDIS_USE_SSL)).lower(),
        "redis_ssl_truststore": CN_REDIS_SSL_TRUSTSTORE,
        "redis_sentinel_group": CN_REDIS_SENTINEL_GROUP,
        'memcached_url': CN_MEMCACHED_URL,

        "document_store_type": doc_store_type,
        "jca_server_url": get_jackrabbit_rmi_url(),
        "jca_username": jca_user,
        "jca_pw": jca_pw,
        "jca_pw_encoded": jca_pw_encoded,

        'ldap_hostname': manager.config.get('ldap_init_host', "localhost"),
        'ldaps_port': manager.config.get('ldap_init_port', 1636),
        'ldap_binddn': manager.config.get('ldap_binddn'),
        'encoded_ox_ldap_pw': manager.secret.get('encoded_ox_ldap_pw'),
        'jetty_base': manager.config.get('jetty_base'),
        'orgName': manager.config.get('orgName'),
        'auth_client_id': manager.config.get('auth_client_id'),
        'authClient_encoded_pw': manager.secret.get('authClient_encoded_pw'),
        'hostname': manager.config.get('hostname'),
        'idp_client_id': manager.config.get('idp_client_id'),
        'idpClient_encoded_pw': manager.secret.get('idpClient_encoded_pw'),
        'auth_openid_key_base64': manager.secret.get('auth_openid_key_base64'),
        # 'passport_rs_client_id': manager.config.get('passport_rs_client_id'),
        # 'passport_rs_client_base64_jwks': manager.secret.get('passport_rs_client_base64_jwks'),
        # 'passport_rs_client_cert_alias': manager.config.get('passport_rs_client_cert_alias'),
        # 'passport_rp_client_id': manager.config.get('passport_rp_client_id'),
        # 'passport_rp_client_base64_jwks': manager.secret.get('passport_rp_client_base64_jwks'),
        # "passport_rp_client_jks_fn": manager.config.get("passport_rp_client_jks_fn"),
        # "passport_rp_client_jks_pass": manager.secret.get("passport_rp_client_jks_pass"),
        # "encoded_ldap_pw": manager.secret.get('encoded_ldap_pw'),
        "encoded_admin_password": manager.secret.get('encoded_admin_password'),
        # 'scim_rs_client_id': manager.config.get('scim_rs_client_id'),
        # 'scim_rs_client_base64_jwks': manager.secret.get('scim_rs_client_base64_jwks'),
        # 'scim_rs_client_cert_alias': manager.config.get("scim_rs_client_cert_alias"),
        # 'scim_rp_client_id': manager.config.get('scim_rp_client_id'),
        # 'scim_rp_client_base64_jwks': manager.secret.get('scim_rp_client_base64_jwks'),
        # 'scim_resource_oxid': manager.config.get('scim_resource_oxid'),
        # 'passport_rp_ii_client_id': manager.config.get("passport_rp_ii_client_id"),

        'admin_email': manager.config.get('admin_email'),
        'shibJksFn': manager.config.get('shibJksFn'),
        'shibJksPass': manager.secret.get('shibJksPass'),
        'oxTrustConfigGeneration': str(as_boolean(CN_OXTRUST_CONFIG_GENERATION)).lower(),
        'encoded_shib_jks_pw': manager.secret.get('encoded_shib_jks_pw'),
        # 'scim_rs_client_jks_fn': manager.config.get('scim_rs_client_jks_fn'),
        # 'scim_rs_client_jks_pass_encoded': manager.secret.get('scim_rs_client_jks_pass_encoded'),
        'passport_rs_client_jks_fn': manager.config.get('passport_rs_client_jks_fn'),
        'passport_rs_client_jks_pass_encoded': manager.secret.get('passport_rs_client_jks_pass_encoded'),
        'shibboleth_version': manager.config.get('shibboleth_version'),
        'idp3Folder': manager.config.get('idp3Folder'),
        'ldap_site_binddn': manager.config.get('ldap_site_binddn'),

        "passport_resource_id": manager.config.get("passport_resource_id"),

        "gluu_radius_client_id": manager.config.get("gluu_radius_client_id"),
        "gluu_ro_encoded_pw": manager.secret.get("gluu_ro_encoded_pw"),
        # "super_gluu_ro_session_script": manager.config.get("super_gluu_ro_session_script"),
        # "super_gluu_ro_script": manager.config.get("super_gluu_ro_script"),
        # "enableRadiusScripts": "false",  # @TODO: enable it?
        # "gluu_ro_client_base64_jwks": manager.secret.get("gluu_ro_client_base64_jwks"),

        "jansPassportEnabled": str(as_boolean(CN_PASSPORT_ENABLED)).lower(),
        "jansRadiusEnabled": str(as_boolean(CN_RADIUS_ENABLED)).lower(),
        "jansSamlEnabled": str(as_boolean(CN_SAML_ENABLED)).lower(),
        "jansScimEnabled": str(as_boolean(CN_SCIM_ENABLED)).lower(),

        "pairwiseCalculationKey": manager.secret.get("pairwiseCalculationKey"),
        "pairwiseCalculationSalt": manager.secret.get("pairwiseCalculationSalt"),
        "default_openid_jks_dn_name": manager.config.get("default_openid_jks_dn_name"),
        "auth_openid_jks_fn": manager.config.get("auth_openid_jks_fn"),
        "auth_openid_jks_pass": manager.secret.get("auth_openid_jks_pass"),
        "auth_legacyIdTokenClaims": manager.config.get("auth_legacyIdTokenClaims"),
        "passportSpTLSCert": manager.config.get("passportSpTLSCert"),
        "passportSpTLSKey": manager.config.get("passportSpTLSKey"),
        "auth_openidScopeBackwardCompatibility": manager.config.get("auth_openidScopeBackwardCompatibility"),
        "fido2ConfigFolder": manager.config.get("fido2ConfigFolder"),

        "admin_inum": manager.config.get("admin_inum"),
        "enable_scim_access_policy": str(as_boolean(CN_SCIM_ENABLED) or as_boolean(CN_PASSPORT_ENABLED)).lower(),
        # "scimTestMode": str(as_boolean(CN_SCIM_TEST_MODE)).lower(),
        # "scim_test_client_id": manager.config.get("scim_test_client_id"),
        # "encoded_scim_test_client_secret": encode_text(
        #     manager.secret.get("scim_test_client_secret"),
        #     manager.secret.get("encoded_salt"),
        # ).decode(),
        "scim_client_id": manager.config.get("scim_client_id"),
        "scim_client_encoded_pw": manager.secret.get("scim_client_encoded_pw"),
        "casa_enable_script": str(as_boolean(CN_CASA_ENABLED)).lower(),
        "oxd_hostname": "localhost",
        "oxd_port": "8443",
        "jca_client_id": manager.config.get("jca_client_id"),
        "jca_client_encoded_pw": manager.secret.get("jca_client_encoded_pw"),
    }
    return ctx


def merge_extension_ctx(ctx):
    basedir = "/app/static/extension"

    for ext_type in os.listdir(basedir):
        ext_type_dir = os.path.join(basedir, ext_type)

        for fname in os.listdir(ext_type_dir):
            filepath = os.path.join(ext_type_dir, fname)
            ext_name = "{}_{}".format(
                ext_type, os.path.splitext(fname)[0].lower()
            )

            with open(filepath) as fd:
                ctx[ext_name] = generate_base64_contents(fd.read())
    return ctx


def merge_radius_ctx(ctx):
    basedir = "/app/static/radius"
    file_mappings = {
        "super_gluu_ro_session_script": "super_gluu_ro_session.py",
        "super_gluu_ro_script": "super_gluu_ro.py",
    }

    for key, file_ in file_mappings.items():
        fn = os.path.join(basedir, file_)
        with open(fn) as f:
            ctx[key] = generate_base64_contents(f.read())
    return ctx


def merge_oxtrust_ctx(ctx):
    basedir = '/app/templates/oxtrust'
    file_mappings = {
        'oxtrust_cache_refresh_base64': 'oxtrust-cache-refresh.json',
        'oxtrust_config_base64': 'oxtrust-config.json',
        'oxtrust_import_person_base64': 'oxtrust-import-person.json',
    }

    for key, file_ in file_mappings.items():
        file_path = os.path.join(basedir, file_)
        with open(file_path) as fp:
            ctx[key] = generate_base64_contents(fp.read() % ctx)
    return ctx


def merge_auth_ctx(ctx):
    basedir = '/app/templates/jans-auth'
    file_mappings = {
        'auth_config_base64': 'dynamic-conf.json',
        'auth_static_conf_base64': 'static-conf.json',
        'auth_error_base64': 'errors.json',
    }

    for key, file_ in file_mappings.items():
        file_path = os.path.join(basedir, file_)
        with open(file_path) as fp:
            ctx[key] = generate_base64_contents(fp.read() % ctx)
    return ctx


def merge_oxidp_ctx(ctx):
    basedir = '/app/templates/oxidp'
    file_mappings = {
        'oxidp_config_base64': 'oxidp-config.json',
    }

    for key, file_ in file_mappings.items():
        file_path = os.path.join(basedir, file_)
        with open(file_path) as fp:
            ctx[key] = generate_base64_contents(fp.read() % ctx)
    return ctx


def merge_passport_ctx(ctx):
    basedir = '/app/templates/passport'
    file_mappings = {
        'passport_central_config_base64': 'passport-central-config.json',
    }

    for key, file_ in file_mappings.items():
        file_path = os.path.join(basedir, file_)
        with open(file_path) as fp:
            ctx[key] = generate_base64_contents(fp.read() % ctx)
    return ctx


def merge_fido2_ctx(ctx):
    basedir = '/app/templates/jans-fido2'
    file_mappings = {
        'fido2_dynamic_conf_base64': 'dynamic-conf.json',
        'fido2_static_conf_base64': 'static-conf.json',
    }

    for key, file_ in file_mappings.items():
        file_path = os.path.join(basedir, file_)
        with open(file_path) as fp:
            ctx[key] = generate_base64_contents(fp.read() % ctx)
    return ctx


def merge_scim_ctx(ctx):
    basedir = '/app/templates/jans-scim'
    file_mappings = {
        'scim_dynamic_conf_base64': 'dynamic-conf.json',
        'scim_static_conf_base64': 'static-conf.json',
    }

    for key, file_ in file_mappings.items():
        file_path = os.path.join(basedir, file_)
        with open(file_path) as fp:
            ctx[key] = generate_base64_contents(fp.read() % ctx)

    return ctx


def prepare_template_ctx(manager):
    ctx = get_base_ctx(manager)
    ctx = merge_extension_ctx(ctx)
    # ctx = merge_radius_ctx(ctx)
    ctx = merge_auth_ctx(ctx)
    # ctx = merge_oxtrust_ctx(ctx)
    # ctx = merge_oxidp_ctx(ctx)
    # ctx = merge_passport_ctx(ctx)
    ctx = merge_fido2_ctx(ctx)
    ctx = merge_scim_ctx(ctx)
    return ctx


class CouchbaseBackend(object):
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
            logger.error("Available quota on couchbase node is less than {} MB".format(min_mem))

        # always create `jans` bucket even when `default` mapping stored in LDAP
        if CN_PERSISTENCE_TYPE == "hybrid" and CN_PERSISTENCE_LDAP_MAPPING == "default":
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

    def import_ldif(self, bucket_mappings):
        ctx = prepare_template_ctx(self.manager)
        attr_processor = AttrProcessor()

        for _, mapping in bucket_mappings.items():
            for file_ in mapping["files"]:
                src = f"/app/templates/{file_}"
                dst = f"/app/tmp/{file_}"
                os.makedirs(os.path.dirname(dst), exist_ok=True)

                render_ldif(src, dst, ctx)
                parser = LDIFParser(open(dst, "rb"))

                query_file = f"/app/tmp/{file_}.n1ql"

                with open(query_file, "a+") as f:
                    for dn, entry in parser.parse():
                        if len(entry) <= 2:
                            continue

                        key = get_key_from(dn)
                        entry["dn"] = [dn]
                        entry = transform_entry(entry, attr_processor)
                        data = json.dumps(entry)
                        # using INSERT will cause duplication error, but the data is left intact
                        query = 'INSERT INTO `%s` (KEY, VALUE) VALUES ("%s", %s);\n' % (mapping["bucket"], key, data)
                        f.write(query)

                # exec query
                logger.info("Importing {} file into {} bucket (if needed)".format(file_, mapping["bucket"]))
                with open(query_file) as f:
                    for line in f:
                        query = line.strip()
                        if not query:
                            continue

                        req = self.client.exec_query(query)
                        if not req.ok:
                            logger.warning("Failed to execute query, reason={}".format(req.json()))

    def initialize(self):
        def is_initialized():
            persistence_type = os.environ.get("CN_PERSISTENCE_TYPE", "couchbase")
            ldap_mapping = os.environ.get("CN_PERSISTENCE_LDAP_MAPPING", "default")
            bucket_prefix = os.environ.get("CN_COUCHBASE_BUCKET_PREFIX", "jans")

            # only default and user buckets buckets that may have initial data;
            # these data also affected by LDAP mapping selection;
            bucket, key = bucket_prefix, "configuration_jans-auth"

            # if `hybrid` is selected and default mapping is stored in LDAP,
            # the default bucket won't have data, hence we check the user bucket instead
            if persistence_type == "hybrid" and ldap_mapping == "default":
                bucket, key = f"{bucket_prefix}_user", "groups_60B7"

            req = self.client.exec_query(
                f"SELECT objectClass FROM {bucket} USE KEYS $key",
                key=key,
            )

            if req.ok:
                data = req.json()
                return bool(data["results"])
            return False

        num_replica = int(os.environ.get("CN_COUCHBASE_INDEX_NUM_REPLICA", 0))
        num_indexer_nodes = len(self.client.get_index_nodes())

        if num_replica >= num_indexer_nodes:
            raise ValueError(f"Number of index replica ({num_replica}) must be less than available indexer nodes ({num_indexer_nodes})")

        self.index_num_replica = num_replica

        bucket_mappings = get_bucket_mappings()

        time.sleep(5)
        self.create_buckets(bucket_mappings)

        time.sleep(5)
        self.create_indexes(bucket_mappings)

        should_skip = as_boolean(
            os.environ.get("CN_PERSISTENCE_SKIP_EXISTING", True),
        )
        if should_skip and is_initialized():
            logger.info("Couchbase backend already initialized")
            return

        time.sleep(5)
        self.import_ldif(bucket_mappings)

        time.sleep(5)
        self.create_couchbase_shib_user()

    def create_couchbase_shib_user(self):
        self.client.create_user(
            'couchbaseShibUser',
            self.manager.secret.get("couchbase_shib_user_password"),
            'Shibboleth IDP',
            'query_select[*]',
        )


class LDAPBackend(object):
    def __init__(self, manager):
        host = CN_LDAP_URL
        user = manager.config.get("ldap_binddn")
        password = decode_text(
            manager.secret.get("encoded_ox_ldap_pw"),
            manager.secret.get("encoded_salt"),
        ).decode()

        server = Server(host, port=1636, use_ssl=True)
        self.conn = Connection(server, user, password)
        self.manager = manager

    def check_indexes(self, mapping):
        if mapping == "site":
            index_name = "jansScrTyp"
            backend = "site"
        # elif mapping == "statistic":
        #     index_name = "jansMetricTyp"
        #     backend = "metric"
        else:
            index_name = "del"
            backend = "userRoot"

        dn = "ds-cfg-attribute={},cn=Index,ds-cfg-backend-id={}," \
             "cn=Backends,cn=config".format(index_name, backend)

        max_wait_time = 300
        sleep_duration = 10

        for _ in range(0, max_wait_time, sleep_duration):
            try:
                with self.conn as conn:
                    conn.search(
                        search_base=dn,
                        search_filter="(objectClass=*)",
                        search_scope=BASE,
                        attributes=["1.1"],
                        size_limit=1,
                    )
                    if conn.result["description"] == "success":
                        return
                    reason = conn.result["message"]
            except (LDAPSessionTerminatedByServerError, LDAPSocketOpenError) as exc:
                reason = exc

            logger.warning("Waiting for index to be ready; reason={}; "
                           "retrying in {} seconds".format(reason, sleep_duration))
            time.sleep(sleep_duration)

    def import_ldif(self):
        ldif_mappings = {
            "default": [
                "base.ldif",
                "attributes.ldif",
                "scopes.ldif",
                "scripts.ldif",
                "configuration.ldif",
                "jans-auth/configuration.ldif",
                "jans-auth/clients.ldif",
                "jans-fido2/configuration.ldif",
                "jans-scim/configuration.ldif",
                "jans-scim/scopes.ldif",
                "jans-scim/clients.ldif",
                "jans-config-api/scopes.ldif",
                "jans-config-api/clients.ldif",
                # "oxidp.ldif",
                # "passport.ldif",
                # "oxpassport-config.ldif",
                # "gluu_radius_base.ldif",
                # "gluu_radius_server.ldif",
                # "clients.ldif",
                "o_metric.ldif",
                # "gluu_radius_clients.ldif",
                # "passport_clients.ldif",
                # "casa.ldif",
                # "scripts_casa.ldif",
            ],
            "user": [
                "people.ldif",
                "groups.ldif",
            ],
            "site": [
                "o_site.ldif",
            ],
            "cache": [],
            "token": [],
            "session": [],
        }

        # hybrid means only a subsets of ldif are needed
        if CN_PERSISTENCE_TYPE == "hybrid":
            mapping = CN_PERSISTENCE_LDAP_MAPPING
            ldif_mappings = {mapping: ldif_mappings[mapping]}

            # # these mappings require `base.ldif`
            # opt_mappings = ("user", "token",)

            # `user` mapping requires `o=gluu` which available in `base.ldif`
            # if mapping in opt_mappings and "base.ldif" not in ldif_mappings[mapping]:
            if "base.ldif" not in ldif_mappings[mapping]:
                ldif_mappings[mapping].insert(0, "base.ldif")

        ctx = prepare_template_ctx(self.manager)

        for mapping, files in ldif_mappings.items():
            self.check_indexes(mapping)

            for file_ in files:
                logger.info(f"Importing {file_} file")
                src = f"/app/templates/{file_}"
                dst = f"/app/tmp/{file_}"
                os.makedirs(os.path.dirname(dst), exist_ok=True)

                render_ldif(src, dst, ctx)

                parser = LDIFParser(open(dst, "rb"))
                for dn, entry in parser.parse():
                    self.add_entry(dn, entry)

    def add_entry(self, dn, attrs):
        max_wait_time = 300
        sleep_duration = 10

        for _ in range(0, max_wait_time, sleep_duration):
            try:
                with self.conn as conn:
                    conn.add(dn, attributes=attrs)
                    if conn.result["result"] != 0:
                        logger.warning(f"Unable to add entry with DN {dn}; reason={conn.result['message']}")
                    return
            except (LDAPSessionTerminatedByServerError, LDAPSocketOpenError) as exc:
                logger.warning(f"Unable to add entry with DN {dn}; reason={exc}; retrying in {sleep_duration} seconds")
            time.sleep(sleep_duration)

    def initialize(self):
        def is_initialized():
            persistence_type = os.environ.get("CN_PERSISTENCE_TYPE", "ldap")
            ldap_mapping = os.environ.get("CN_PERSISTENCE_LDAP_MAPPING", "default")

            # a minimum service stack is having oxTrust, hence check whether entry
            # for oxTrust exists in LDAP
            default_search = ("ou=jans-auth,ou=configuration,o=jans",
                              "(objectClass=jansAppConf)")

            if persistence_type == "hybrid":
                # `cache` and `token` mapping only have base entries
                search_mapping = {
                    "default": default_search,
                    "user": ("inum=60B7,ou=groups,o=jans", "(objectClass=jansGrp)"),
                    "site": ("ou=cache-refresh,o=site", "(ou=people)"),
                    "cache": ("o=jans", "(ou=cache)"),
                    "token": ("ou=tokens,o=jans", "(ou=tokens)"),
                    "session": ("ou=sessions,o=jans", "(ou=sessions)"),
                }
                search = search_mapping[ldap_mapping]
            else:
                search = default_search

            with self.conn as conn:
                conn.search(
                    search_base=search[0],
                    search_filter=search[1],
                    search_scope=SUBTREE,
                    attributes=['objectClass'],
                    size_limit=1,
                )
                return bool(conn.entries)

        should_skip = as_boolean(
            os.environ.get("CN_PERSISTENCE_SKIP_EXISTING", True),
        )
        if should_skip and is_initialized():
            logger.info("LDAP backend already initialized")
            return

        self.import_ldif()


class HybridBackend(object):
    def __init__(self, manager):
        self.ldap_backend = LDAPBackend(manager)
        self.couchbase_backend = CouchbaseBackend(manager)

    def initialize(self):
        self.ldap_backend.initialize()
        self.couchbase_backend.initialize()


def main():
    manager = get_manager()

    backend_classes = {
        "ldap": LDAPBackend,
        "couchbase": CouchbaseBackend,
        "hybrid": HybridBackend,
    }

    # initialize the backend
    backend_cls = backend_classes.get(CN_PERSISTENCE_TYPE)
    if not backend_cls:
        raise ValueError("unsupported backend")

    backend = backend_cls(manager)
    backend.initialize()


if __name__ == "__main__":
    main()
