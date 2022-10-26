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
from jans.pycloudlib.persistence.utils import PersistenceMapper
from jans.pycloudlib.utils import cert_to_truststore

from settings import LOGGING_CONFIG
from plugins import AdminUiPlugin
from plugins import discover_plugins

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("entrypoint")


def main():
    manager = get_manager()
    persistence_type = os.environ.get("CN_PERSISTENCE_TYPE", "ldap")

    render_salt(manager, "/app/templates/salt.tmpl", "/etc/jans/conf/salt")
    render_base_properties("/app/templates/jans.properties.tmpl", "/etc/jans/conf/jans.properties")

    mapper = PersistenceMapper()
    persistence_groups = mapper.groups().keys()

    if persistence_type == "hybrid":
        render_hybrid_properties("/etc/jans/conf/jans-hybrid.properties")

    if "ldap" in persistence_groups:
        render_ldap_properties(
            manager,
            "/app/templates/jans-ldap.properties.tmpl",
            "/etc/jans/conf/jans-ldap.properties",
        )
        sync_ldap_truststore(manager)

    if "couchbase" in persistence_groups:
        render_couchbase_properties(
            manager,
            "/app/templates/jans-couchbase.properties.tmpl",
            "/etc/jans/conf/jans-couchbase.properties",
        )
        # need to resolve whether we're using default or user-defined couchbase cert
        sync_couchbase_truststore(manager)

    if "sql" in persistence_groups:
        db_dialect = os.environ.get("CN_SQL_DB_DIALECT", "mysql")

        render_sql_properties(
            manager,
            f"/app/templates/jans-{db_dialect}.properties.tmpl",
            "/etc/jans/conf/jans-sql.properties",
        )

    if "spanner" in persistence_groups:
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
        "/usr/java/latest/jre/lib/security/cacerts",
        "changeit",
    )

    modify_jetty_xml()
    modify_webdefault_xml()
    configure_logging()

    plugins = discover_plugins()
    logger.info(f"Loaded config-api plugins: {', '.join(plugins)}")
    modify_config_api_xml(plugins)

    if "admin-ui" in plugins:
        admin_ui_plugin = AdminUiPlugin(manager)
        admin_ui_plugin.setup()
        configure_admin_ui_logging()


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


def configure_logging():
    # default config
    config = {
        "config_api_log_target": "STDOUT",
        "config_api_log_level": "INFO",
        "persistence_log_target": "FILE",
        "persistence_log_level": "INFO",
        "persistence_duration_log_target": "FILE",
        "persistence_duration_log_level": "INFO",
        "ldap_stats_log_target": "FILE",
        "ldap_stats_log_level": "INFO",
        "script_log_target": "FILE",
        "script_log_level": "INFO",
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
        "persistence_log_target": "JANS_CONFIGAPI_PERSISTENCE_FILE",
        "persistence_duration_log_target": "JANS_CONFIGAPI_PERSISTENCE_DURATION_FILE",
        "ldap_stats_log_target": "JANS_CONFIGAPI_PERSISTENCE_LDAP_STATISTICS_FILE",
        "script_log_target": "JANS_CONFIGAPI_SCRIPT_LOG_FILE",
    }

    for key, value in config.items():
        if not key.endswith("_target"):
            continue

        if value == "STDOUT":
            config[key] = "Console"
        else:
            config[key] = file_aliases[key]

    logfile = "/opt/jans/jetty/jans-config-api/resources/log4j2.xml"
    with open(logfile) as f:
        txt = f.read()

    tmpl = Template(txt)
    with open(logfile, "w") as f:
        f.write(tmpl.safe_substitute(config))


def modify_config_api_xml(plugins=None):
    plugins = plugins or []
    fn = "/opt/jans/jetty/jans-config-api/webapps/jans-config-api.xml"

    with open(fn) as f:
        txt = f.read()

    with open(fn, "w") as f:
        ctx = {
            "extra_classpath": ",".join([f"./custom/libs/{plugin}-plugin.jar" for plugin in plugins])
        }
        f.write(txt % ctx)


def configure_admin_ui_logging():
    # default config
    config = {
        "admin_ui_log_target": "FILE",
        "admin_ui_log_level": "INFO",
        "admin_ui_audit_log_target": "FILE",
        "admin_ui_audit_log_level": "INFO",
    }

    # pre-populate custom config; format is JSON string of ``dict``
    try:
        custom_config = json.loads(os.environ.get("CN_ADMIN_UI_PLUGIN_LOGGERS", "{}"))
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
        "admin_ui_log_target": "ADMINUI-LOG",
        "admin_ui_audit_log_target": "ADMINUI-AUDIT",
    }

    for key, value in config.items():
        if not key.endswith("_target"):
            continue

        if value == "STDOUT":
            config[key] = "Console"
        else:
            config[key] = file_aliases[key]

    with open("/app/plugins/admin-ui/log4j2-adminui.xml.tmpl") as f:
        txt = f.read()

    tmpl = Template(txt)
    with open("/opt/jans/jetty/jans-config-api/custom/config/log4j2-adminui.xml", "w") as f:
        f.write(tmpl.safe_substitute(config))


if __name__ == "__main__":
    main()
