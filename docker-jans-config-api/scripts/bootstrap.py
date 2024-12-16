import json
import logging.config
import os
import typing as _t
from functools import cached_property
from string import Template
from urllib.parse import urlparse
from uuid import uuid4

from ldif import LDIFWriter

from jans.pycloudlib import get_manager
from jans.pycloudlib import wait_for_persistence
from jans.pycloudlib.persistence.hybrid import render_hybrid_properties
from jans.pycloudlib.persistence.sql import doc_id_from_dn
from jans.pycloudlib.persistence.sql import SqlClient
from jans.pycloudlib.persistence.sql import render_sql_properties
from jans.pycloudlib.persistence.sql import override_simple_json_property
from jans.pycloudlib.persistence.utils import PersistenceMapper
from jans.pycloudlib.persistence.utils import render_base_properties
from jans.pycloudlib.persistence.utils import render_salt
from jans.pycloudlib.utils import cert_to_truststore
from jans.pycloudlib.utils import generate_base64_contents
from jans.pycloudlib.utils import get_random_chars
from jans.pycloudlib.utils import encode_text
from jans.pycloudlib.utils import as_boolean
from jans.pycloudlib.utils import get_server_certificate

from settings import LOGGING_CONFIG
from plugins import AdminUiPlugin
from plugins import discover_plugins
from utils import get_config_api_scope_mapping

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("jans-config-api")


def main():
    manager = get_manager()
    persistence_type = os.environ.get("CN_PERSISTENCE_TYPE", "sql")

    render_salt(manager, "/app/templates/salt", "/etc/jans/conf/salt")
    render_base_properties("/app/templates/jans.properties", "/etc/jans/conf/jans.properties")

    mapper = PersistenceMapper()
    persistence_groups = mapper.groups().keys()

    if persistence_type == "hybrid":
        hybrid_prop = "etc/jans/conf/jans-hybrid.properties"
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

    with manager.create_lock("config-api-setup"):
        persistence_setup = PersistenceSetup(manager)
        persistence_setup.import_ldif_files()

    plugins = discover_plugins()
    logger.info(f"Loaded config-api plugins: {plugins}")

    if "admin-ui" in plugins:
        admin_ui_plugin = AdminUiPlugin(manager)
        admin_ui_plugin.setup()
        configure_admin_ui_logging()

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
        "config_api_log_target": "STDOUT",
        "config_api_log_level": "INFO",
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

    if any([
        as_boolean(custom_config.get("enable_stdout_log_prefix")),
        as_boolean(os.environ.get("CN_ENABLE_STDOUT_LOG_PREFIX")),
    ]):
        config["log_prefix"] = "${sys:config_api.log.console.prefix}%X{config_api.log.console.group} - "

    with open("/app/templates/jans-config-api/log4j2.xml") as f:
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
        logger.warning("Invalid data type for CN_ADMIN_UI_PLUGIN_LOGGERS; fallback to defaults")
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
            config[key] = "AdminUI_Console"
        else:
            config[key] = file_aliases[key]

    if any([
        as_boolean(custom_config.get("enable_stdout_log_prefix")),
        as_boolean(os.environ.get("CN_ENABLE_STDOUT_LOG_PREFIX")),
    ]):
        config["log_prefix"] = "${sys:admin_ui.log.console.prefix}%X{admin_ui.log.console.group} - "

    with open("/app/templates/jans-config-api/log4j2-adminui.xml") as f:
        txt = f.read()

    tmpl = Template(txt)
    with open("/opt/jans/jetty/jans-config-api/custom/config/log4j2-adminui.xml", "w") as f:
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

    def get_auth_config(self):
        dn = "ou=jans-auth,ou=configuration,o=jans"
        entry = self.client.get("jansAppConf", doc_id_from_dn(dn))
        return json.loads(entry["jansConfDyn"])

    def transform_url(self, url):
        auth_server_url = os.environ.get("CN_AUTH_SERVER_URL", "")

        if not auth_server_url:
            return url

        parse_result = urlparse(url)
        if parse_result.path.startswith("/.well-known"):
            path = f"/jans-auth{parse_result.path}"
        else:
            path = parse_result.path
        return f"http://{auth_server_url}{path}"

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

        if token_server_url := os.environ.get("CN_TOKEN_SERVER_BASE_URL"):
            token_server_hostname = urlparse(token_server_url).hostname

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

        # test client
        ctx["test_client_id"] = self.manager.config.get("test_client_id")
        if not ctx["test_client_id"]:
            ctx["test_client_id"] = f"{uuid4()}"
            self.manager.config.set("test_client_id", ctx["test_client_id"])

        ctx["test_client_pw"] = self.manager.secret.get("test_client_pw")
        if not ctx["test_client_pw"]:
            ctx["test_client_pw"] = get_random_chars()
            self.manager.secret.set("test_client_pw", ctx["test_client_pw"])

        ctx["test_client_encoded_pw"] = self.manager.secret.get("test_client_encoded_pw")
        if not ctx["test_client_encoded_pw"]:
            ctx["test_client_encoded_pw"] = encode_text(
                ctx["test_client_pw"], self.manager.secret.get("encoded_salt"),
            ).decode()
            self.manager.secret.set("test_client_encoded_pw", ctx["test_client_encoded_pw"])

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
        logger.info("Missing scopes creation is enabled!")
        self.generate_scopes_ldif()

        files = ["config.ldif", "scopes.ldif", "clients.ldif", "scim-scopes.ldif", "testing-clients.ldif"]
        ldif_files = [f"/app/templates/jans-config-api/{file_}" for file_ in files]

        for file_ in ldif_files:
            logger.info(f"Importing {file_}")
            self.client.create_from_ldif(file_, self.ctx)


if __name__ == "__main__":
    main()
