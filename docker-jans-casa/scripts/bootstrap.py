import json
import logging.config
import math
import os
import time
from uuid import uuid4
from string import Template
from functools import cached_property

from ldif import LDIFWriter

from jans.pycloudlib import get_manager
from jans.pycloudlib import wait_for_persistence
from jans.pycloudlib.persistence.couchbase import CouchbaseClient
from jans.pycloudlib.persistence.couchbase import id_from_dn
from jans.pycloudlib.persistence.couchbase import render_couchbase_properties
from jans.pycloudlib.persistence.couchbase import sync_couchbase_cert
from jans.pycloudlib.persistence.couchbase import sync_couchbase_password
from jans.pycloudlib.persistence.couchbase import sync_couchbase_truststore
from jans.pycloudlib.persistence.hybrid import render_hybrid_properties
from jans.pycloudlib.persistence.ldap import LdapClient
from jans.pycloudlib.persistence.ldap import render_ldap_properties
from jans.pycloudlib.persistence.ldap import sync_ldap_password
from jans.pycloudlib.persistence.ldap import sync_ldap_truststore
from jans.pycloudlib.persistence.spanner import render_spanner_properties
from jans.pycloudlib.persistence.spanner import SpannerClient
from jans.pycloudlib.persistence.spanner import sync_google_credentials
from jans.pycloudlib.persistence.sql import doc_id_from_dn
from jans.pycloudlib.persistence.sql import render_sql_properties
from jans.pycloudlib.persistence.sql import SqlClient
from jans.pycloudlib.persistence.sql import sync_sql_password
from jans.pycloudlib.persistence.utils import PersistenceMapper
from jans.pycloudlib.persistence.utils import render_base_properties
from jans.pycloudlib.persistence.utils import render_salt
from jans.pycloudlib.utils import cert_to_truststore
from jans.pycloudlib.utils import get_random_chars
from jans.pycloudlib.utils import encode_text
from jans.pycloudlib.utils import generate_base64_contents
from jans.pycloudlib.utils import as_boolean
from jans.pycloudlib.utils import get_server_certificate

from settings import LOGGING_CONFIG

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("jans-casa")

manager = get_manager()


def configure_logging():
    # default config
    config = {
        "casa_log_target": "STDOUT",
        "casa_log_level": "INFO",
        "timer_log_target": "FILE",
        "timer_log_level": "INFO",
        "log_prefix": "",
    }

    # pre-populate custom config; format is JSON string of ``dict``
    try:
        custom_config = json.loads(os.environ.get("CN_CASA_APP_LOGGERS", "{}"))
    except json.decoder.JSONDecodeError as exc:
        logger.warning(f"Unable to load logging configuration from environment variable; reason={exc}; fallback to defaults")
        custom_config = {}

    # ensure custom config is ``dict`` type
    if not isinstance(custom_config, dict):
        logger.warning("Invalid data type for CN_CASA_APP_LOGGERS; fallback to defaults")
        custom_config = {}

    # list of supported levels
    log_levels = ("OFF", "FATAL", "ERROR", "WARN", "INFO", "DEBUG", "TRACE",)

    # list of supported outputs
    log_targets = ("STDOUT", "FILE",)

    for k, v in custom_config.items():
        if k not in config:
            continue

        if k.endswith("_log_level") and v not in log_levels:
            logger.warning(f"Invalid {v} log level for {k}; fallback to defaults")
            v = config[k]

        if k.endswith("_log_target") and v not in log_targets:
            logger.warning(f"Invalid {v} log output for {k}; fallback to defaults")
            v = config[k]

        # update the config
        config[k] = v

    # mapping between the ``log_target`` value and their appenders
    file_aliases = {
        "casa_log_target": "LOG_FILE",
        "timer_log_target": "TIMERS_FILE",
    }
    for key, value in config.items():
        if not key.endswith("_target"):
            continue

        if value == "STDOUT":
            config[key] = "Console"
        else:
            config[key] = file_aliases[key]

    if any([
        as_boolean(custom_config.get("enable_stdout_log_prefix")),
        as_boolean(os.environ.get("CN_ENABLE_STDOUT_LOG_PREFIX")),
    ]):
        config["log_prefix"] = "${sys:casa.log.console.prefix}%X{casa.log.console.group} - "

    with open("/app/templates/jans-casa/log4j2.xml") as f:
        txt = f.read()

    logfile = "/opt/jans/jetty/jans-casa/resources/log4j2.xml"
    tmpl = Template(txt)
    with open(logfile, "w") as f:
        f.write(tmpl.safe_substitute(config))


def main():
    persistence_type = os.environ.get("CN_PERSISTENCE_TYPE", "ldap")

    render_salt(manager, "/app/templates/salt", "/etc/jans/conf/salt")
    render_base_properties("/app/templates/jans.properties", "/etc/jans/conf/jans.properties")

    mapper = PersistenceMapper()
    persistence_groups = mapper.groups()

    if persistence_type == "hybrid":
        render_hybrid_properties("/etc/jans/conf/jans-hybrid.properties")

    if "ldap" in persistence_groups:
        render_ldap_properties(
            manager,
            "/app/templates/jans-ldap.properties",
            "/etc/jans/conf/jans-ldap.properties",
        )

        if as_boolean(os.environ.get("CN_LDAP_USE_SSL", "true")):
            sync_ldap_truststore(manager)
        sync_ldap_password(manager)

    if "couchbase" in persistence_groups:
        sync_couchbase_password(manager)
        render_couchbase_properties(
            manager,
            "/app/templates/jans-couchbase.properties",
            "/etc/jans/conf/jans-couchbase.properties",
        )

        if as_boolean(os.environ.get("CN_COUCHBASE_TRUSTSTORE_ENABLE", "true")):
            sync_couchbase_cert(manager)
            sync_couchbase_truststore(manager)

    if "sql" in persistence_groups:
        sync_sql_password(manager)
        db_dialect = os.environ.get("CN_SQL_DB_DIALECT", "mysql")
        render_sql_properties(
            manager,
            f"/app/templates/jans-{db_dialect}.properties",
            "/etc/jans/conf/jans-sql.properties",
        )

    if "spanner" in persistence_groups:
        render_spanner_properties(
            manager,
            "/app/templates/jans-spanner.properties",
            "/etc/jans/conf/jans-spanner.properties",
        )
        sync_google_credentials(manager)

    wait_for_persistence(manager)

    if not os.path.isfile("/etc/certs/web_https.crt"):
        if as_boolean(os.environ.get("CN_SSL_CERT_FROM_SECRETS", "true")):
            manager.secret.to_file("ssl_cert", "/etc/certs/web_https.crt")
        else:
            hostname = manager.config.get("hostname")
            logger.info(f"Pulling SSL certificate from {hostname}")
            get_server_certificate(hostname, 443, "/etc/certs/web_https.crt")

    cert_to_truststore(
        "web_https",
        "/etc/certs/web_https.crt",
        "/opt/java/lib/security/cacerts",
        "changeit",
    )

    configure_logging()

    with manager.lock.create_lock("casa-setup"):
        persistence_setup = PersistenceSetup(manager)
        persistence_setup.import_ldif_files()

    try:
        manager.secret.to_file(
            "smtp_jks_base64",
            "/etc/certs/smtp-keys.pkcs12",
            decode=True,
            binary_mode=True,
        )
    except ValueError:
        # likely secret is not created yet
        logger.warning("Unable to pull file smtp-keys.pkcs12 from secrets")


class PersistenceSetup:
    def __init__(self, manager):
        self.manager = manager

        client_classes = {
            "ldap": LdapClient,
            "couchbase": CouchbaseClient,
            "spanner": SpannerClient,
            "sql": SqlClient,
        }

        # determine persistence type
        mapper = PersistenceMapper()
        self.persistence_type = mapper.mapping["default"]

        # determine persistence client
        client_cls = client_classes.get(self.persistence_type)
        self.client = client_cls(manager)

    @cached_property
    def ctx(self):
        hostname = self.manager.config.get("hostname")

        ctx = {
            "hostname": hostname,
            "casa_redirect_uri": f"https://{hostname}/jans-casa",
            "casa_redirect_logout_uri": f"https://{hostname}/jans-casa/bye.zul",
            "casa_frontchannel_logout_uri": f"https://{hostname}/jans-casa/autologout",
            "casa_agama_deployment_id": "202447d5-d44c-3125-b1f7-207cb33b6bf7",
        }

        # Casa client
        ctx["casa_client_id"] = self.manager.config.get("casa_client_id")
        if not ctx["casa_client_id"]:
            ctx["casa_client_id"] = f"1902.{uuid4()}"
            self.manager.config.set("casa_client_id", ctx["casa_client_id"])

        ctx["casa_client_pw"] = self.manager.secret.get("casa_client_pw")
        if not ctx["casa_client_pw"]:
            ctx["casa_client_pw"] = get_random_chars()
            self.manager.secret.set("casa_client_pw", ctx["casa_client_pw"])

        ctx["casa_client_encoded_pw"] = self.manager.secret.get("casa_client_encoded_pw")
        if not ctx["casa_client_encoded_pw"]:
            ctx["casa_client_encoded_pw"] = encode_text(
                ctx["casa_client_pw"], self.manager.secret.get("encoded_salt"),
            ).decode()
            self.manager.secret.set("casa_client_encoded_pw", ctx["casa_client_encoded_pw"])

        with open("/app/templates/jans-casa/casa-config.json") as f:
            ctx["casa_config_base64"] = generate_base64_contents(f.read() % ctx)

        # calculate start date
        ts = time.time()
        microseconds, _ = math.modf(ts)
        gm_ts = time.gmtime(ts)
        ctx["jans_start_date"] = time.strftime("%Y%m%d%H%M%S", gm_ts) + f"{microseconds:.3f}Z"[1:]

        # casa agama project (requires agama script to be enabled)
        with open("/usr/share/java/casa-agama-project.zip", "rb") as f:
            ctx["ads_prj_assets_base64"] = generate_base64_contents(f.read())

        # finalized contexts
        return ctx

    @cached_property
    def ldif_files(self):
        filenames = ["configuration.ldif", "client.ldif"]

        # generate extra scopes
        self.generate_scopes_ldif()
        filenames.append("scopes.ldif")

        return [f"/app/templates/jans-casa/{filename}" for filename in filenames]

    def _deprecated_script_exists(self):
        # deprecated Casa script DN
        id_ = "inum=BABA-CACA,ou=scripts,o=jans"

        # sql and spanner
        if self.persistence_type in ("sql", "spanner"):
            return bool(self.client.get("jansCustomScr", doc_id_from_dn(id_)))

        # couchbase
        if self.persistence_type == "couchbase":
            bucket = os.environ.get("CN_COUCHBASE_BUCKET_PREFIX", "jans")
            key = id_from_dn(id_)
            req = self.client.exec_query(
                f"SELECT META().id, {bucket}.* FROM {bucket} USE KEYS '{key}'"
            )
            try:
                entry = req.json()["results"][0]
                return bool(entry["id"])
            except IndexError:
                return False

        # ldap
        return bool(self.client.get(id_))

    def import_ldif_files(self):
        for file_ in self.ldif_files:
            logger.info(f"Importing {file_}")
            self.client.create_from_ldif(file_, self.ctx)

    def generate_scopes_ldif(self):
        # prepare required scopes (if any)
        with open("/app/templates/jans-casa/scopes.json") as f:
            scopes = json.loads(f.read())

        with open("/app/templates/jans-casa/scopes.ldif", "wb") as fd:
            writer = LDIFWriter(fd, cols=1000)

            for scope in scopes:
                writer.unparse(
                    f"inum={scope['inum']},ou=scopes,o=jans",
                    {
                        "objectClass": ["top", "jansScope"],
                        "description": [scope["description"]],
                        "displayName": [scope["displayName"]],
                        "inum": [scope["inum"]],
                        "jansDefScope": [str(scope["jansDefScope"]).lower()],
                        "jansId": [scope["jansId"]],
                        "jansScopeTyp": [scope["jansScopeTyp"]],
                        "jansAttrs": [json.dumps({
                            "spontaneousClientId": None,
                            "spontaneousClientScopes": [],
                            "showInConfigurationEndpoint": False,
                        })],
                    },
                )


if __name__ == "__main__":
    main()
