import json
import logging.config
import os
import re
import typing as _t
from functools import cached_property
from string import Template
from urllib.parse import urlparse
from uuid import uuid4

from ldif import LDIFWriter

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
from jans.pycloudlib.persistence.couchbase import CouchbaseClient
from jans.pycloudlib.persistence.couchbase import id_from_dn
from jans.pycloudlib.persistence.ldap import LdapClient
from jans.pycloudlib.persistence.spanner import SpannerClient
from jans.pycloudlib.persistence.sql import SqlClient
from jans.pycloudlib.persistence.sql import doc_id_from_dn
from jans.pycloudlib.persistence.utils import PersistenceMapper
from jans.pycloudlib.utils import cert_to_truststore
from jans.pycloudlib.utils import generate_base64_contents
from jans.pycloudlib.utils import get_random_chars
from jans.pycloudlib.utils import encode_text
from jans.pycloudlib.utils import as_boolean

from settings import LOGGING_CONFIG
from plugins import AdminUiPlugin
from plugins import discover_plugins
from utils import get_config_api_scope_mapping

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

    persistence_setup = PersistenceSetup(manager)
    persistence_setup.import_ldif_files()

    plugins = discover_plugins()
    logger.info(f"Loaded config-api plugins: {plugins}")

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
        "audit_log_target": "FILE",
        "audit_log_level": "INFO",
        "log_prefix": "",
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
        "audit_log_target": "AUDIT_FILE",
    }

    for key, value in config.items():
        if not key.endswith("_target"):
            continue

        if value == "STDOUT":
            config[key] = "Console"
        else:
            config[key] = file_aliases[key]

    if as_boolean(custom_config.get("enable_stdout_log_prefix")):
        config["log_prefix"] = "${sys:log.console.prefix}%X{log.console.group} - "

    with open("/app/templates/log4j2.xml") as f:
        txt = f.read()

    logfile = "/opt/jans/jetty/jans-config-api/resources/log4j2.xml"
    tmpl = Template(txt)
    with open(logfile, "w") as f:
        f.write(tmpl.safe_substitute(config))


def configure_admin_ui_logging():
    # default config
    config = {
        "admin_ui_log_target": "FILE",
        "admin_ui_log_level": "INFO",
        "admin_ui_audit_log_target": "FILE",
        "admin_ui_audit_log_level": "INFO",
        "log_prefix": "",
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

    if as_boolean(custom_config.get("enable_stdout_log_prefix")):
        config["log_prefix"] = "${sys:log.console.prefix}%X{log.console.group} - "

    with open("/app/plugins/admin-ui/log4j2-adminui.xml") as f:
        txt = f.read()

    tmpl = Template(txt)
    with open("/opt/jans/jetty/jans-config-api/custom/config/log4j2-adminui.xml", "w") as f:
        f.write(tmpl.safe_substitute(config))


class PersistenceSetup:
    def __init__(self, manager) -> None:
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

    def get_auth_config(self):
        dn = "ou=jans-auth,ou=configuration,o=jans"

        # sql and spanner
        if self.persistence_type in ("sql", "spanner"):
            entry = self.client.get("jansAppConf", doc_id_from_dn(dn))
            return json.loads(entry["jansConfDyn"])

        # couchbase
        elif self.persistence_type == "couchbase":
            key = id_from_dn(dn)
            bucket = os.environ.get("CN_COUCHBASE_BUCKET_PREFIX", "jans")
            req = self.client.exec_query(
                f"SELECT META().id, {bucket}.* FROM {bucket} USE KEYS '{key}'"
            )
            attrs = req.json()["results"][0]
            return attrs["jansConfDyn"]

        # ldap
        else:
            entry = self.client.get(dn, attributes=["jansConfDyn"])
            return json.loads(entry.entry_attributes_as_dict["jansConfDyn"][0])

    def transform_url(self, url):
        auth_server_url = os.environ.get("CN_AUTH_SERVER_URL", "")

        if not auth_server_url:
            return url

        parse_result = urlparse(url)
        if parse_result.path.startswith("/.well-known"):
            path = f"/jans-auth{parse_result.path}"
        else:
            path = parse_result.path
        url = f"http://{auth_server_url}{path}"
        return url

    def get_injected_urls(self):
        auth_config = self.get_auth_config()

        urls = (
            "issuer",
            "openIdConfigurationEndpoint",
            "introspectionEndpoint",
            "tokenEndpoint",
            "tokenRevocationEndpoint",
        )

        return {
            url: self.transform_url(auth_config[url])
            for url in urls
        }

    @cached_property
    def ctx(self) -> dict[str, _t.Any]:
        hostname = self.manager.config.get("hostname")
        approved_issuer = [hostname]

        token_server_hostname = os.environ.get("CN_TOKEN_SERVER_BASE_HOSTNAME")
        if token_server_hostname and token_server_hostname not in approved_issuer:
            approved_issuer.append(token_server_hostname)

        ctx = {
            "hostname": hostname,
            "apiApprovedIssuer": ",".join([f'"https://{issuer}"' for issuer in approved_issuer]),
            "apiProtectionType": "oauth2",
            "endpointInjectionEnabled": "true",
            "configOauthEnabled": str(os.environ.get("CN_CONFIG_API_OAUTH_ENABLED") or True).lower(),
        }
        ctx.update(self.get_injected_urls())

        # Client
        ctx["jca_client_id"] = self.manager.config.get("jca_client_id")
        if not ctx["jca_client_id"]:
            ctx["jca_client_id"] = f"1800.{uuid4()}"
            self.manager.config.set("jca_client_id", ctx["jca_client_id"])

        ctx["jca_client_pw"] = self.manager.secret.get("jca_client_pw")
        if not ctx["jca_client_pw"]:
            ctx["jca_client_pw"] = get_random_chars()
            self.manager.secret.set("jca_client_pw", ctx["jca_client_pw"])

        ctx["jca_client_encoded_pw"] = self.manager.secret.get("jca_client_encoded_pw")
        if not ctx["jca_client_encoded_pw"]:
            ctx["jca_client_encoded_pw"] = encode_text(
                ctx["jca_client_pw"], self.manager.secret.get("encoded_salt"),
            ).decode()
            self.manager.secret.set("jca_client_encoded_pw", ctx["jca_client_encoded_pw"])

        # pre-populate config_api_dynamic_conf_base64
        with open("/app/templates/jans-config-api/dynamic-conf.json") as f:
            tmpl = Template(f.read())
            ctx["config_api_dynamic_conf_base64"] = generate_base64_contents(tmpl.substitute(**ctx))

        # finalize ctx
        return ctx

    def generate_scopes_ldif(self):
        # prepare required scopes (if any)
        scopes = []

        scope_mapping = get_config_api_scope_mapping()
        for inum, meta in scope_mapping.items():
            attrs = {
                "creatorAttrs": [json.dumps({})],
                "description": [f"Config API {meta['level']} {meta['name']}"],
                "displayName": [f"Config API {meta['name']}"],
                "inum": [inum],
                "jansAttrs": [json.dumps({"spontaneousClientScopes": None, "showInConfigurationEndpoint": True})],
                "jansId": [meta["name"]],
                "jansScopeTyp": ["oauth"],
                "objectClass": ["top", "jansScope"],
                "jansDefScope": ["false"],
            }
            scopes.append(attrs)

        with open("/app/templates/jans-config-api/scopes.ldif", "wb") as fd:
            writer = LDIFWriter(fd, cols=1000)
            for scope in scopes:
                writer.unparse(f"inum={scope['inum'][0]},ou=scopes,o=jans", scope)

    def import_ldif_files(self) -> None:
        # create missing scopes, saved as scopes.ldif (if enabled)
        if as_boolean(os.environ.get("CN_CONFIG_API_CREATE_SCOPES")):
            logger.info("Missing scopes creation is enabled!")
            self.generate_scopes_ldif()

        files = ["config.ldif", "scopes.ldif", "clients.ldif", "scim-scopes.ldif"]
        ldif_files = [f"/app/templates/jans-config-api/{file_}" for file_ in files]

        for file_ in ldif_files:
            logger.info(f"Importing {file_}")
            self.client.create_from_ldif(file_, self.ctx)


if __name__ == "__main__":
    main()
