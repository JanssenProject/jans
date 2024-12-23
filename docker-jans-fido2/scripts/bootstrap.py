import json
import logging.config
import os
import typing as _t
import uuid
from datetime import datetime
from datetime import UTC
from functools import cached_property
from string import Template

from jans.pycloudlib import get_manager
from jans.pycloudlib import wait_for_persistence
from jans.pycloudlib.persistence.hybrid import render_hybrid_properties
from jans.pycloudlib.persistence.sql import SqlClient
from jans.pycloudlib.persistence.sql import render_sql_properties
from jans.pycloudlib.persistence.sql import override_simple_json_property
from jans.pycloudlib.persistence.utils import PersistenceMapper
from jans.pycloudlib.persistence.utils import render_base_properties
from jans.pycloudlib.persistence.utils import render_salt
from jans.pycloudlib.utils import cert_to_truststore
from jans.pycloudlib.utils import generate_base64_contents
from jans.pycloudlib.utils import as_boolean
from jans.pycloudlib.utils import get_server_certificate

from settings import LOGGING_CONFIG

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("jans-fido2")

manager = get_manager()


def main():
    persistence_type = os.environ.get("CN_PERSISTENCE_TYPE", "sql")

    render_salt(manager, "/app/templates/salt", "/etc/jans/conf/salt")
    render_base_properties("/app/templates/jans.properties", "/etc/jans/conf/jans.properties")

    mapper = PersistenceMapper()
    persistence_groups = mapper.groups()

    if persistence_type == "hybrid":
        hybrid_prop = "/etc/jans/conf/jans-hybrid.properties"
        if not os.path.exists(hybrid_prop):
            render_hybrid_properties(hybrid_prop)

    if "sql" in persistence_groups:
        db_dialect = os.environ.get("CN_SQL_DB_DIALECT", "mysql")
        render_sql_properties(
            manager,
            f"/app/templates/jans-{db_dialect}.properties",
            "/etc/jans/conf/jans-sql.properties",
        )

    wait_for_persistence(manager)
    override_simple_json_property("/etc/jans/conf/jans-sql.properties")

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

    with manager.create_lock("fido2-setup"):
        persistence_setup = PersistenceSetup(manager)
        persistence_setup.import_ldif_files()


def configure_logging():
    # default config
    config = {
        "fido2_log_target": "STDOUT",
        "fido2_log_level": "INFO",
        "persistence_log_target": "FILE",
        "persistence_log_level": "INFO",
        "persistence_duration_log_target": "FILE",
        "persistence_duration_log_level": "INFO",
        "script_log_target": "FILE",
        "script_log_level": "INFO",
        "log_prefix": "",
    }

    # pre-populate custom config; format is JSON string of ``dict``
    try:
        custom_config = json.loads(os.environ.get("CN_FIDO2_APP_LOGGERS", "{}"))
    except json.decoder.JSONDecodeError as exc:
        logger.warning(f"Unable to load logging configuration from environment variable; reason={exc}; fallback to defaults")
        custom_config = {}

    # ensure custom config is ``dict`` type
    if not isinstance(custom_config, dict):
        logger.warning("Invalid data type for CN_FIDO2_APP_LOGGERS; fallback to defaults")
        custom_config = {}

    # list of supported levels; OFF is not supported
    log_levels = ("FATAL", "ERROR", "WARN", "INFO", "DEBUG", "TRACE",)

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
        "fido2_log_target": "FILE",
        "persistence_log_target": "FIDO2_PERSISTENCE_FILE",
        "persistence_duration_log_target": "FIDO2_PERSISTENCE_DURATION_FILE",
        "script_log_target": "FIDO2_SCRIPT_LOG_FILE",
    }
    for key, value in file_aliases.items():
        if config[key] == "FILE":
            config[key] = value

    if any([
        as_boolean(custom_config.get("enable_stdout_log_prefix")),
        as_boolean(os.environ.get("CN_ENABLE_STDOUT_LOG_PREFIX")),
    ]):
        config["log_prefix"] = "${sys:fido2.log.console.prefix}%X{fido2.log.console.group} - "

    with open("/app/templates/jans-fido2/log4j2.xml") as f:
        txt = f.read()

    logfile = "/opt/jans/jetty/jans-fido2/resources/log4j2.xml"
    tmpl = Template(txt)
    with open(logfile, "w") as f:
        f.write(tmpl.safe_substitute(config))


class PersistenceSetup:
    def __init__(self, manager) -> None:
        self.manager = manager

        client_classes = {
            "sql": SqlClient,
        }

        # determine persistence type
        mapper = PersistenceMapper()
        self.persistence_type = mapper.mapping["default"]

        # determine persistence client
        client_cls = client_classes.get(self.persistence_type)
        self.client = client_cls(manager)

    @cached_property
    def ctx(self) -> dict[str, _t.Any]:
        ctx = {
            "hostname": self.manager.config.get("hostname"),
            "fido2ConfigFolder": "/etc/jans/conf/fido2",
            "fido_document_certs_dir": "/etc/jans/conf/fido2/mds/cert",
            "fido_document_tocs_dir": "/etc/jans/conf/fido2/mds/toc",
        }

        # pre-populate fido2_dynamic_conf_base64
        with open("/app/templates/jans-fido2/dynamic-conf.json") as f:
            ctx["fido2_dynamic_conf_base64"] = generate_base64_contents(f.read() % ctx)

        # pre-populate static ctx
        for tmpl, ctx_name, mode in [
            ("/app/templates/jans-fido2/static-conf.json", "fido2_static_conf_base64", "r"),
            ("/app/templates/jans-fido2/jans-fido2-errors.json", "fido2_error_base64", "r"),
            ("/etc/jans/conf/fido2/mds/toc/toc.jwt", "fido_document_tocs_base64", "r"),
            ("/etc/jans/conf/fido2/mds/cert/root-r3.crt", "fido_document_certs_base64", "rb"),
        ]:
            with open(tmpl, mode) as f:
                ctx[ctx_name] = generate_base64_contents(f.read())

        # docs inum
        for inum in ["fido_document_certs_inum", "fido_document_tocs_inum"]:
            ctx[inum] = self.manager.config.get(inum)

            if not ctx[inum]:
                ctx[inum] = str(uuid.uuid4())
                self.manager.config.set(inum, ctx[inum])

        ctx["fido_document_creation_date"] = generalized_time_utc()

        # finalized ctx
        return ctx

    @cached_property
    def ldif_files(self) -> list[str]:
        return [
            "/app/templates/jans-fido2/fido2.ldif",
            "/app/templates/jans-fido2/docuemts.ldif",
        ]

    def import_ldif_files(self) -> None:
        for file_ in self.ldif_files:
            logger.info(f"Importing {file_}")
            self.client.create_from_ldif(file_, self.ctx)


def utcnow():
    return datetime.now(UTC)


def generalized_time_utc(dtime=None):
    """Calculate LDAP generalized time."""
    if not dtime:
        dtime = utcnow()
    return dtime.strftime("%Y%m%d%H%M%SZ")


if __name__ == "__main__":
    main()
