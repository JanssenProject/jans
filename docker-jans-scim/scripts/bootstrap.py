from __future__ import annotations

import json
import logging.config
import os
import typing as _t
from functools import cached_property
from string import Template
from uuid import uuid4

from ldif import LDIFWriter

from jans.pycloudlib import get_manager
from jans.pycloudlib import wait_for_persistence
from jans.pycloudlib.persistence.hybrid import render_hybrid_properties
from jans.pycloudlib.persistence.sql import render_sql_properties
from jans.pycloudlib.persistence.sql import SqlClient
from jans.pycloudlib.persistence.sql import override_simple_json_property
from jans.pycloudlib.persistence.utils import PersistenceMapper
from jans.pycloudlib.persistence.utils import render_base_properties
from jans.pycloudlib.persistence.utils import render_salt
from jans.pycloudlib.utils import cert_to_truststore
from jans.pycloudlib.utils import encode_text
from jans.pycloudlib.utils import generate_base64_contents
from jans.pycloudlib.utils import get_random_chars
from jans.pycloudlib.utils import as_boolean
from jans.pycloudlib.utils import get_server_certificate

from settings import LOGGING_CONFIG
from utils import parse_swagger_file

if _t.TYPE_CHECKING:  # pragma: no cover
    # imported objects for function type hint, completion, etc.
    # these won't be executed in runtime
    from jans.pycloudlib.manager import Manager


logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("jans-scim")

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

    with manager.create_lock("scim-setup"):
        persistence_setup = PersistenceSetup(manager)
        persistence_setup.import_ldif_files()


def configure_logging():
    # default config
    config = {
        "scim_log_target": "STDOUT",
        "scim_log_level": "INFO",
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
        custom_config = json.loads(os.environ.get("CN_SCIM_APP_LOGGERS", "{}"))
    except json.decoder.JSONDecodeError as exc:
        logger.warning(f"Unable to load logging configuration from environment variable; reason={exc}; fallback to defaults")
        custom_config = {}

    # ensure custom config is ``dict`` type
    if not isinstance(custom_config, dict):
        logger.warning("Invalid data type for CN_SCIM_APP_LOGGERS; fallback to defaults")
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
        "scim_log_target": "FILE",
        "persistence_log_target": "JANS_SCIM_PERSISTENCE_FILE",
        "persistence_duration_log_target": "JANS_SCIM_PERSISTENCE_DURATION_FILE",
        "script_log_target": "JANS_SCIM_SCRIPT_LOG_FILE",
    }
    for key, value in file_aliases.items():
        if config[key] == "FILE":
            config[key] = value

    if any([
        as_boolean(custom_config.get("enable_stdout_log_prefix")),
        as_boolean(os.environ.get("CN_ENABLE_STDOUT_LOG_PREFIX")),
    ]):
        config["log_prefix"] = "${sys:scim.log.console.prefix}%X{scim.log.console.group} - "

    with open("/app/templates/jans-scim/log4j2.xml") as f:
        txt = f.read()

    logfile = "/opt/jans/jetty/jans-scim/resources/log4j2.xml"
    tmpl = Template(txt)
    with open(logfile, "w") as f:
        f.write(tmpl.safe_substitute(config))


class PersistenceSetup:
    def __init__(self, manager: Manager) -> None:
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
        }

        # SCIM client
        ctx["scim_client_id"] = self.manager.config.get("scim_client_id")

        if not ctx["scim_client_id"]:
            ctx["scim_client_id"] = f"1201.{uuid4()}"
            self.manager.config.set("scim_client_id", ctx["scim_client_id"])

        ctx["scim_client_pw"] = self.manager.secret.get("scim_client_pw")

        if not ctx["scim_client_pw"]:
            ctx["scim_client_pw"] = get_random_chars()
            self.manager.secret.set("scim_client_pw", ctx["scim_client_pw"])

        ctx["scim_client_encoded_pw"] = self.manager.secret.get("scim_client_encoded_pw")

        if not ctx["scim_client_encoded_pw"]:
            ctx["scim_client_encoded_pw"] = encode_text(
                ctx["scim_client_pw"], self.manager.secret.get("encoded_salt"),
            ).decode()
            self.manager.secret.set("scim_client_encoded_pw", ctx["scim_client_encoded_pw"])

        # pre-populate scim_dynamic_conf_base64
        with open("/app/templates/jans-scim/dynamic-conf.json") as f:
            ctx["scim_dynamic_conf_base64"] = generate_base64_contents(f.read() % ctx)

        # pre-populate scim_static_conf_base64
        with open("/app/templates/jans-scim/static-conf.json") as f:
            ctx["scim_static_conf_base64"] = generate_base64_contents(f.read())

        return ctx

    @cached_property
    def ldif_files(self) -> list[str]:
        return [
            f"/app/templates/jans-scim/{file_}"
            for file_ in ["configuration.ldif", "scopes.ldif", "clients.ldif"]
        ]

    def import_ldif_files(self) -> None:
        # temporarily disable dynamic scopes creation
        # see https://github.com/JanssenProject/jans/issues/2869
        # self.generate_scopes_ldif()

        for file_ in self.ldif_files:
            logger.info(f"Importing {file_}")
            self.client.create_from_ldif(file_, self.ctx)

    def get_scope_jans_ids(self):
        if self.persistence_type == "sql":
            entries = self.client.search("jansScope", ["jansId"])
            return [entry["jansId"] for entry in entries]

    def generate_scopes_ldif(self):
        # jansId to compare to
        existing_jans_ids = self.get_scope_jans_ids()

        def generate_scim_scopes():
            swagger = parse_swagger_file()
            scopes = swagger["components"]["securitySchemes"]["scim_oauth"]["flows"]["clientCredentials"]["scopes"]

            generated_scopes = []
            for jans_id, desc in scopes.items():
                if jans_id in existing_jans_ids:
                    continue

                inum = f"1200.{generate_hex()}-{generate_hex()}"
                attrs = {
                    "description": [desc],
                    "displayName": [f"SCIM scope {jans_id}"],
                    "inum": [inum],
                    "jansAttrs": [json.dumps({"spontaneousClientScopes": None, "showInConfigurationEndpoint": True})],
                    "jansId": [jans_id],
                    "jansScopeTyp": ["oauth"],
                    "objectClass": ["top", "jansScope"],
                    "jansDefScope": ["false"],
                }
                generated_scopes.append(attrs)
            return generated_scopes

        # prepare required scopes (if any)
        scopes = []

        scim_scopes = generate_scim_scopes()
        scopes += scim_scopes

        with open("/app/templates/jans-scim/scopes.ldif", "wb") as fd:
            writer = LDIFWriter(fd, cols=1000)
            for scope in scopes:
                writer.unparse(f"inum={scope['inum'][0]},ou=scopes,o=jans", scope)


def generate_hex(size: int = 3):
    return os.urandom(size).hex().upper()


if __name__ == "__main__":
    main()
