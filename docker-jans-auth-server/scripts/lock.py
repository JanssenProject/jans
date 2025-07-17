import json
import logging.config
import os
import typing as _t
from functools import cached_property
from string import Template
from uuid import uuid4

from ldif import LDIFWriter

from jans.pycloudlib import get_manager
from jans.pycloudlib.persistence.sql import SqlClient
from jans.pycloudlib.persistence.utils import PersistenceMapper
from jans.pycloudlib.utils import generate_base64_contents
from jans.pycloudlib.utils import as_boolean
from jans.pycloudlib.utils import encode_text
from jans.pycloudlib.utils import get_random_chars

from settings import LOGGING_CONFIG

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("jans-auth")

manager = get_manager()


def configure_lock_logging():
    # default config
    config = {
        "lock_log_target": "STDOUT",
        "lock_log_level": "INFO",
        "log_prefix": "",
    }

    # pre-populate custom config; format is JSON string of ``dict``
    try:
        custom_config = json.loads(os.environ.get("CN_LOCK_APP_LOGGERS", "{}"))
    except json.decoder.JSONDecodeError as exc:
        logger.warning(f"Unable to load logging configuration from environment variable; reason={exc}; fallback to defaults")
        custom_config = {}

    # ensure custom config is ``dict`` type
    if not isinstance(custom_config, dict):
        logger.warning("Invalid data type for CN_LOCK_APP_LOGGERS; fallback to defaults")
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
        "lock_log_target": "JANS_LOCK_FILE",
    }

    for key, value in config.items():
        if not key.endswith("_target"):
            continue

        if value == "STDOUT":
            config[key] = "Lock_Console"
        else:
            config[key] = file_aliases[key]

    if any([
        as_boolean(custom_config.get("enable_stdout_log_prefix")),
        as_boolean(os.environ.get("CN_ENABLE_STDOUT_LOG_PREFIX")),
    ]):
        config["log_prefix"] = "${sys:lock.log.console.prefix}%X{lock.log.console.group} - "

    with open("/app/templates/jans-lock/log4j2-lock.xml") as f:
        txt = f.read()

    logfile = "/opt/jans/jetty/jans-auth/resources/log4j2-lock.xml"
    tmpl = Template(txt)
    with open(logfile, "w") as f:
        f.write(tmpl.safe_substitute(config))


class LockPersistenceSetup:
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
        hostname = self.manager.config.get("hostname")
        ctx = {
            "hostname": hostname,
            "base_endpoint": "jans-auth",
            "jans_auth_token_endpoint": f"https://{hostname}/jans-auth/restv1/token",
        }

        # client
        ctx["lock_client_id"] = self.manager.config.get("lock_client_id")

        if not ctx["lock_client_id"]:
            ctx["lock_client_id"] = f"2200.{uuid4()}"
            self.manager.config.set("lock_client_id", ctx["lock_client_id"])

        ctx["lock_client_pw"] = self.manager.secret.get("lock_client_pw")

        if not ctx["lock_client_pw"]:
            ctx["lock_client_pw"] = get_random_chars()
            self.manager.secret.set("lock_client_pw", ctx["lock_client_pw"])

        ctx["lock_client_encoded_pw"] = self.manager.secret.get("lock_client_encoded_pw")

        if not ctx["lock_client_encoded_pw"]:
            ctx["lock_client_encoded_pw"] = encode_text(
                ctx["lock_client_pw"], self.manager.secret.get("encoded_salt"),
            ).decode()
            self.manager.secret.set("lock_client_encoded_pw", ctx["lock_client_encoded_pw"])

        # pre-populate lock_dynamic_conf_base64
        with open("/app/templates/jans-lock/dynamic-conf.json") as f:
            ctx["lock_dynamic_conf_base64"] = generate_base64_contents(f.read() % ctx)

        # pre-populate lock_static_conf_base64
        with open("/app/templates/jans-lock/static-conf.json") as f:
            ctx["lock_static_conf_base64"] = generate_base64_contents(f.read())

        # pre-populate lock_error_base64
        with open("/app/templates/jans-lock/errors.json") as f:
            ctx["lock_error_base64"] = generate_base64_contents(f.read())
        return ctx

    @cached_property
    def ldif_files(self) -> list[str]:
        filenames = ["config.ldif", "clients.ldif"]

        # generate extra scopes
        self.generate_scopes_ldif()
        filenames.append("scopes.ldif")

        return [f"/app/templates/jans-lock/{filename}" for filename in filenames]

    def import_ldif_files(self) -> None:
        for file_ in self.ldif_files:
            logger.info(f"Importing {file_}")
            self.client.create_from_ldif(file_, self.ctx)

    def generate_scopes_ldif(self):
        # prepare required scopes (if any)
        with open("/app/templates/jans-lock/scopes.json") as f:
            scopes = json.loads(f.read())

        with open("/app/templates/jans-lock/scopes.ldif", "wb") as fd:
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
