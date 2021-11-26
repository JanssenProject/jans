import json
import logging.config
import os
import re
from string import Template

from jans.pycloudlib import get_manager
from jans.pycloudlib.persistence import render_couchbase_properties
from jans.pycloudlib.persistence import render_base_properties
from jans.pycloudlib.persistence import render_hybrid_properties
from jans.pycloudlib.persistence import render_ldap_properties
from jans.pycloudlib.persistence import render_salt
from jans.pycloudlib.persistence import sync_couchbase_truststore
from jans.pycloudlib.persistence import sync_ldap_truststore
from jans.pycloudlib.persistence import render_sql_properties
from jans.pycloudlib.persistence import render_spanner_properties
from jans.pycloudlib.utils import cert_to_truststore
from jans.pycloudlib.utils import safe_render

from utils import get_injected_urls
from settings import LOGGING_CONFIG

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("entrypoint")


def render_app_properties(manager):
    client_id = manager.config.get("jca_client_id")
    client_encoded_pw = manager.secret.get("jca_client_encoded_pw")
    hostname = manager.config.get("hostname")

    approved_issuer = os.environ.get("CN_APPROVED_ISSUER") or f"https://{hostname}"

    ctx = {
        "jca_log_level": os.environ.get("CN_CONFIG_API_LOG_LEVEL", "INFO"),
        "jca_client_id": client_id,
        "jca_client_encoded_pw": client_encoded_pw,
        "approved_issuer": approved_issuer,
    }
    ctx.update(get_injected_urls())

    with open("/app/templates/application.properties.tmpl") as f:
        txt = safe_render(f.read(), ctx)

    with open("/opt/jans/jetty/jans-config-api/webapps/jans-config-api/WEB-INF/classes/application.properties", "w") as f:
        f.write(txt)


def main():
    manager = get_manager()
    persistence_type = os.environ.get("CN_PERSISTENCE_TYPE", "ldap")

    render_salt(manager, "/app/templates/salt.tmpl", "/etc/jans/conf/salt")
    render_base_properties("/app/templates/jans.properties.tmpl", "/etc/jans/conf/jans.properties")

    if persistence_type in ("ldap", "hybrid"):
        render_ldap_properties(
            manager,
            "/app/templates/jans-ldap.properties.tmpl",
            "/etc/jans/conf/jans-ldap.properties",
        )
        sync_ldap_truststore(manager)

    if persistence_type in ("couchbase", "hybrid"):
        render_couchbase_properties(
            manager,
            "/app/templates/jans-couchbase.properties.tmpl",
            "/etc/jans/conf/jans-couchbase.properties",
        )
        # need to resolve whether we're using default or user-defined couchbase cert
        sync_couchbase_truststore(manager)

    if persistence_type == "hybrid":
        render_hybrid_properties("/etc/jans/conf/jans-hybrid.properties")

    if persistence_type == "sql":
        render_sql_properties(
            manager,
            "/app/templates/jans-sql.properties.tmpl",
            "/etc/jans/conf/jans-sql.properties",
        )

    if persistence_type == "spanner":
        render_spanner_properties(
            manager,
            "/app/templates/jans-spanner.properties.tmpl",
            "/etc/jans/conf/jans-spanner.properties",
        )

    if not all([
        os.path.isfile("/etc/certs/web_https.crt"),
        os.path.isfile("/etc/certs/web_https.key"),
    ]):
        manager.secret.to_file("ssl_cert", "/etc/certs/web_https.crt")
        manager.secret.to_file("ssl_key", "/etc/certs/web_https.key")

    cert_to_truststore(
        "web_https",
        "/etc/certs/web_https.crt",
        "/usr/lib/jvm/default-jvm/jre/lib/security/cacerts",
        "changeit",
    )

    # render_app_properties(manager)

    modify_jetty_xml()
    modify_webdefault_xml()
    modify_server_ini()
    configure_logging()


def modify_jetty_xml():
    fn = "/opt/jetty/etc/jetty.xml"
    with open(fn) as f:
        txt = f.read()

    # disable contexts
    updates = re.sub(
        r'<New id="DefaultHandler" class="org.eclipse.jetty.server.handler.DefaultHandler"/>',
        r'<New id="DefaultHandler" class="org.eclipse.jetty.server.handler.DefaultHandler">\n\t\t\t\t <Set name="showContexts">false</Set>\n\t\t\t </New>',
        txt,
        flags=re.DOTALL | re.M,
    )

    with open(fn, "w") as f:
        f.write(updates)


def modify_webdefault_xml():
    fn = "/opt/jetty/etc/webdefault.xml"
    with open(fn) as f:
        txt = f.read()

    # disable dirAllowed
    updates = re.sub(
        r'(<param-name>dirAllowed</param-name>)(\s*)(<param-value>)true(</param-value>)',
        r'\1\2\3false\4',
        txt,
        flags=re.DOTALL | re.M,
    )

    with open(fn, "w") as f:
        f.write(updates)


def modify_server_ini():
    with open("/opt/jans/jetty/jans-config-api/start.d/server.ini", "a") as f:
        updates = "\n".join([
            # disable server version info
            "jetty.httpConfig.sendServerVersion=false",
        ])
        f.write(updates)


def configure_logging():
    # default config
    config = {
        "config_api_log_target": "STDOUT",
        "config_api_log_level": "INFO",
    }

    # pre-populate custom config; format is JSON string of ``dict``
    try:
        custom_config = json.loads(os.environ.get("CN_CONFIG_API_APP_LOGGERS", "{}"))
    except json.decoder.JSONDecodeError as exc:
        logger.warning(f"Unable to load logging configuration from environment variable; reason={exc}; fallback to defaults")
        custom_config = {}

    # ensure custom config is ``dict`` type
    if not isinstance(custom_config, dict):
        logger.warning("Invalid data type for CN_CONFIG_API_APP_LOGGERS; fallback to defaults")
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
        "config_api_log_target": "FILE",
    }
    for key, value in file_aliases.items():
        if config[key] == "FILE":
            config[key] = value

    logfile = "/opt/jans/jetty/jans-config-api/resources/log4j2.xml"
    with open(logfile) as f:
        txt = f.read()

    tmpl = Template(txt)
    with open(logfile, "w") as f:
        f.write(tmpl.safe_substitute(config))


if __name__ == "__main__":
    main()
