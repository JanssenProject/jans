import json
import logging.config
import os
from string import Template

from jans.pycloudlib import get_manager
from jans.pycloudlib.utils import cert_to_truststore
from jans.pycloudlib.utils import as_boolean
from jans.pycloudlib.utils import get_server_certificate


from settings import LOGGING_CONFIG

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("jans-kc-scheduler")

manager = get_manager()


def render_config_props():
    hostname = manager.config.get("hostname")

    ctx = {
        # config-api
        "api_url": f"https://{hostname}/jans-config-api",
        "token_endpoint": f"https://{hostname}/jans-auth/restv1/token",
        "client_id": manager.config.get("kc_scheduler_api_client_id"),
        "client_secret": manager.secret.get("kc_scheduler_api_client_pw"),
        "scopes": "",
        "auth_method": "basic",

        # keycloak-api
        "keycloak_admin_url": f"https://{hostname}/kc",
        "keycloak_admin_realm": "master",
        "keycloak_admin_username": manager.config.get("kc_admin_username"),
        "keycloak_admin_password": manager.secret.get("kc_admin_password"),
        "keycloak_client_id": "admin-cli",
    }

    # merge context from logging configuration
    ctx.update(configure_logging())

    with open("/app/templates/kc-scheduler/config.properties") as f:
        tmpl = Template(f.read())

    with open("/opt/kc-scheduler/conf/config.properties", "w") as f:
        f.write(tmpl.safe_substitute(ctx))


def configure_logging():
    config = {
        "scheduler_log_target": "STDOUT",
        "scheduler_log_level": "INFO",
        "http_client_log_level": "INFO",
        "http_wire_log_level": "INFO",
        "http_header_log_level": "INFO",
        "log_prefix": "",
    }

    # pre-populate custom config; format is JSON string of ``dict``
    try:
        custom_config = json.loads(os.environ.get("CN_KC_SCHEDULER_APP_LOGGERS", "{}"))
    except json.decoder.JSONDecodeError as exc:
        logger.warning(f"Unable to load logging configuration from environment variable; reason={exc}; fallback to defaults")
        custom_config = {}

    # ensure custom config is ``dict`` type
    if not isinstance(custom_config, dict):
        logger.warning("Invalid data type for CN_KC_SCHEDULER_APP_LOGGERS; fallback to defaults")
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

    if any([
        as_boolean(custom_config.get("enable_stdout_log_prefix")),
        as_boolean(os.environ.get("CN_ENABLE_STDOUT_LOG_PREFIX"))
    ]):
        config["log_prefix"] = "jans-kc-scheduler - "

    with open("/app/templates/kc-scheduler/logback.xml") as f:
        txt = f.read()

    with open("/opt/kc-scheduler/conf/logback.xml", "w") as f:
        f.write(txt)

    # emit context
    return config


def main():
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

    render_config_props()
    configure_logging()


if __name__ == "__main__":
    main()
