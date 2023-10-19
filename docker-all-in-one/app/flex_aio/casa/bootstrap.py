import contextlib
import json
import logging.config
import os
from uuid import uuid4
from string import Template
from functools import cached_property

from ldif import LDIFWriter

from jans.pycloudlib import get_manager
from jans.pycloudlib.persistence import CouchbaseClient
from jans.pycloudlib.persistence import LdapClient
from jans.pycloudlib.persistence import SpannerClient
from jans.pycloudlib.persistence import SqlClient
from jans.pycloudlib.persistence import doc_id_from_dn
from jans.pycloudlib.persistence import id_from_dn
from jans.pycloudlib.persistence.utils import PersistenceMapper
from jans.pycloudlib.utils import get_random_chars
from jans.pycloudlib.utils import encode_text
from jans.pycloudlib.utils import generate_base64_contents
from jans.pycloudlib.utils import as_boolean

from flex_aio.settings import LOGGING_CONFIG
from flex_aio.utils import get_template_paths
from flex_aio.utils import render_persistence_props
from flex_aio.utils import import_cert_to_truststore

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("casa")


def configure_logging():
    # default config
    config = {
        "casa_log_target": "STDOUT",
        "casa_log_level": "INFO",
        "timer_log_target": "FILE",
        "timer_log_level": "INFO",
        "log_prefix": "",
    }

    # pre-populate custom config; format is JSON string of ``dict``
    try:
        custom_config = json.loads(os.environ.get("GLUU_CASA_APP_LOGGERS", "{}"))
    except json.decoder.JSONDecodeError as exc:
        logger.warning(f"Unable to load logging configuration from environment variable; reason={exc}; fallback to defaults")
        custom_config = {}

    # ensure custom config is ``dict`` type
    if not isinstance(custom_config, dict):
        logger.warning("Invalid data type for GLUU_CASA_APP_LOGGERS; fallback to defaults")
        custom_config = {}

    # list of supported levels
    log_levels = ("OFF", "FATAL", "ERROR", "WARN", "INFO", "DEBUG", "TRACE",)

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
        "casa_log_target": "LOG_FILE",
        "timer_log_target": "TIMERS_FILE",
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
        as_boolean(os.environ.get("CN_ENABLE_STDOUT_LOG_PREFIX"))
    ]):
        config["log_prefix"] = "${sys:casa.log.console.prefix}%X{casa.log.console.group} - "

    src, dst = get_template_paths("/opt/jans/jetty/casa/resources/log4j2.xml")
    with open(src) as f:
        txt = f.read()

    tmpl = Template(txt)
    with open(dst, "w") as f:
        f.write(tmpl.safe_substitute(config))


def main():
    manager = get_manager()

    render_persistence_props(manager)
    import_cert_to_truststore(manager)
    configure_logging()

    persistence_setup = PersistenceSetup(manager)
    persistence_setup.import_ldif_files()


class PersistenceSetup:
    def __init__(self, manager):
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

    @cached_property
    def ctx(self):
        hostname = self.manager.config.get("hostname")

        ctx = {
            "hostname": hostname,
            "casa_redirect_uri": f"https://{hostname}/casa",
            "casa_redirect_logout_uri": f"https://{hostname}/casa/bye.zul",
            "casa_frontchannel_logout_uri": f"https://{hostname}/casa/autologout",
        }

        with open("/app/static/extension/person_authentication/Casa.py") as f:
            ctx["casa_person_authentication_script"] = generate_base64_contents(f.read())

        # Casa client
        ctx["casa_client_id"] = self.manager.config.get("casa_client_id")
        if not ctx["casa_client_id"]:
            ctx["casa_client_id"] = f"1902.{uuid4()}"
            self.manager.config.set("casa_client_id", ctx["casa_client_id"])

        ctx["casa_client_pw"] = self.manager.secret.get("casa_client_pw")
        if not ctx["casa_client_pw"]:
            ctx["casa_client_pw"] = get_random_chars()
            self.manager.secret.set("casa_client_pw", ctx["casa_client_pw"])

        ctx["casa_client_encoded_pw"] = self.manager.secret.get("casa_client_encoded_pw")
        if not ctx["casa_client_encoded_pw"]:
            ctx["casa_client_encoded_pw"] = encode_text(
                ctx["casa_client_pw"], self.manager.secret.get("encoded_salt"),
            ).decode()
            self.manager.secret.set("casa_client_encoded_pw", ctx["casa_client_encoded_pw"])

        # finalized contexts
        return ctx

    @cached_property
    def ldif_files(self):
        filenames = ["casa_config.ldif", "casa_client.ldif"]
        # add casa_person_authentication_script.ldif if there's no existing casa script in persistence to avoid error
        # java.lang.IllegalStateException: Duplicate key casa (attempted merging values 1 and 1)
        if not self._deprecated_script_exists():
            filenames.append("casa_person_authentication_script.ldif")

        # generate extra scopes
        self.generate_scopes_ldif()
        filenames.append("casa_scopes.ldif")

        return [f"/app/templates/{filename}" for filename in filenames]

    def _deprecated_script_exists(self):
        # deprecated Casa script DN
        id_ = "inum=BABA-CACA,ou=scripts,o=jans"

        # sql and spanner
        if self.persistence_type in ("sql", "spanner"):
            return bool(self.client.get("jansCustomScr", doc_id_from_dn(id_)))

        # couchbase
        if self.persistence_type == "couchbase":
            bucket = os.environ.get("CN_COUCHBASE_BUCKET_PREFIX", "jans")
            key = id_from_dn(id_)
            req = self.client.exec_query(
                f"SELECT META().id, {bucket}.* FROM {bucket} USE KEYS '{key}'"
            )
            with contextlib.suppress(IndexError):
                entry = req.json()["results"][0]
                return bool(entry["id"])

        # ldap
        return bool(self.client.get(id_))

    def import_ldif_files(self):
        for file_ in self.ldif_files:
            logger.info(f"Importing {file_}")
            self.client.create_from_ldif(file_, self.ctx)

    def generate_scopes_ldif(self):
        # prepare required scopes (if any)
        with open("/app/static/casa_scopes.json") as f:
            scopes = json.loads(f.read())

        with open("/app/templates/casa_scopes.ldif", "wb") as fd:
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


if __name__ == "__main__":
    main()
