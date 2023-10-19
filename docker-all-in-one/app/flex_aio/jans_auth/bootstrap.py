import json
import logging.config
import os
from string import Template

from jans.pycloudlib import get_manager
from jans.pycloudlib.utils import as_boolean

from flex_aio.settings import LOGGING_CONFIG
from flex_aio.jans_auth.hooks import get_auth_keys_hook
from flex_aio.utils import get_template_paths
from flex_aio.utils import render_persistence_props
from flex_aio.utils import import_cert_to_truststore

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("jans_auth")


def main():
    manager = get_manager()

    render_persistence_props(manager)
    import_cert_to_truststore(manager)
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
        "ldap_stats_log_target": "FILE",
        "ldap_stats_log_level": "INFO",
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
        "ldap_stats_log_target": "JANS_AUTH_PERSISTENCE_LDAP_STATISTICS_FILE",
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

    src, dst = get_template_paths("/opt/jans/jetty/jans-auth/resources/log4j2.xml")
    with open(src) as f:
        txt = f.read()

    tmpl = Template(txt)
    with open(dst, "w") as f:
        f.write(tmpl.safe_substitute(config))


if __name__ == "__main__":
    main()
