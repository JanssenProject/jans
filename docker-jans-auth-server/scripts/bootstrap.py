import json
import logging.config
import os
import shutil
from pathlib import Path
from string import Template

from jans.pycloudlib import get_manager
from jans.pycloudlib import wait_for_persistence
from jans.pycloudlib.persistence.hybrid import render_hybrid_properties
from jans.pycloudlib.persistence.sql import render_sql_properties
from jans.pycloudlib.persistence.sql import override_simple_json_property
from jans.pycloudlib.persistence.utils import render_base_properties
from jans.pycloudlib.persistence.utils import render_salt
from jans.pycloudlib.persistence.utils import PersistenceMapper
from jans.pycloudlib.utils import as_boolean
from jans.pycloudlib.utils import cert_to_truststore
from jans.pycloudlib.utils import get_server_certificate

from settings import LOGGING_CONFIG
from hooks import get_auth_keys_hook
from lock import configure_lock_logging
from lock import LockPersistenceSetup

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("jans-auth")

manager = get_manager()


def main():
    persistence_type = os.environ.get("CN_PERSISTENCE_TYPE", "sql")

    render_salt(manager, "/app/templates/salt", "/etc/jans/conf/salt")
    render_base_properties("/app/templates/jans.properties", "/etc/jans/conf/jans.properties")

    mapper = PersistenceMapper()
    persistence_groups = mapper.groups().keys()

    if persistence_type == "hybrid":
        hybrid_prop = "/etc/jans/conf/jans-hybrid.properties"
        if not os.path.exists(hybrid_prop):
            render_hybrid_properties(hybrid_prop)

    if "sql" in persistence_groups:
        db_dialect = os.environ.get("CN_SQL_DB_DIALECT", "mysql")
        sql_prop = "/etc/jans/conf/jans-sql.properties"
        if not os.path.exists(sql_prop):
            render_sql_properties(manager, f"/app/templates/jans-{db_dialect}.properties", sql_prop)

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
    get_auth_keys_hook(manager)

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

    copy_builtin_libs()

    if as_boolean(os.environ.get("CN_LOCK_ENABLED", "false")):
        configure_lock_logging()

        with manager.create_lock("lock-setup"):
            persistence_setup = LockPersistenceSetup(manager)
            persistence_setup.import_ldif_files()


def configure_logging():
    # default config
    config = {
        "auth_log_target": "STDOUT",
        "auth_log_level": "INFO",
        "http_log_target": "FILE",
        "http_log_level": "INFO",
        "persistence_log_target": "FILE",
        "persistence_log_level": "INFO",
        "persistence_duration_log_target": "FILE",
        "persistence_duration_log_level": "INFO",
        "script_log_target": "FILE",
        "script_log_level": "INFO",
        "audit_log_target": "FILE",
        "audit_log_level": "INFO",
        "log_prefix": "",
    }

    # pre-populate custom config; format is JSON string of ``dict``
    try:
        custom_config = json.loads(os.environ.get("CN_AUTH_APP_LOGGERS", "{}"))
    except json.decoder.JSONDecodeError as exc:
        logger.warning(f"Unable to load logging configuration from environment variable; reason={exc}; fallback to defaults")
        custom_config = {}

    # ensure custom config is ``dict`` type
    if not isinstance(custom_config, dict):
        logger.warning("Invalid data type for CN_AUTH_APP_LOGGERS; fallback to defaults")
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
        "auth_log_target": "FILE",
        "http_log_target": "JANS_AUTH_HTTP_REQUEST_RESPONSE_FILE",
        "persistence_log_target": "JANS_AUTH_PERSISTENCE_FILE",
        "persistence_duration_log_target": "JANS_AUTH_PERSISTENCE_DURATION_FILE",
        "script_log_target": "JANS_AUTH_SCRIPT_LOG_FILE",
        "audit_log_target": "JANS_AUTH_AUDIT_LOG_FILE",
    }
    for key, value in file_aliases.items():
        if config[key] == "FILE":
            config[key] = value

    if any([
        as_boolean(custom_config.get("enable_stdout_log_prefix")),
        as_boolean(os.environ.get("CN_ENABLE_STDOUT_LOG_PREFIX"))
    ]):
        config["log_prefix"] = "${sys:auth.log.console.prefix}%X{auth.log.console.group} - "

    with open("/app/templates/jans-auth/log4j2.xml") as f:
        txt = f.read()

    logfile = "/opt/jans/jetty/jans-auth/resources/log4j2.xml"
    tmpl = Template(txt)
    with open(logfile, "w") as f:
        f.write(tmpl.safe_substitute(config))


def copy_builtin_libs():
    lock_enabled = as_boolean(os.environ.get("CN_LOCK_ENABLED", "false"))

    for src in Path("/opt/jans/jetty/jans-auth/_libs").glob("*.jar"):
        # skip jans-lock-service and jans-lock-model
        if lock_enabled is False and src.name.startswith("jans-lock"):
            continue

        dst = f"/opt/jans/jetty/jans-auth/custom/libs/{src.name}"
        shutil.copyfile(src, dst)


if __name__ == "__main__":
    main()
