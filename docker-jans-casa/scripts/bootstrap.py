import json
import logging.config
import os
from uuid import uuid4
from string import Template
from functools import cached_property

from ldif import LDIFWriter

from jans.pycloudlib import get_manager
from jans.pycloudlib import wait_for_persistence
from jans.pycloudlib.persistence.hybrid import render_hybrid_properties
from jans.pycloudlib.persistence.sql import doc_id_from_dn
from jans.pycloudlib.persistence.sql import render_sql_properties
from jans.pycloudlib.persistence.sql import SqlClient
from jans.pycloudlib.persistence.sql import override_simple_json_property
from jans.pycloudlib.persistence.utils import PersistenceMapper
from jans.pycloudlib.persistence.utils import render_base_properties
from jans.pycloudlib.persistence.utils import render_salt
from jans.pycloudlib.utils import cert_to_truststore
from jans.pycloudlib.utils import get_random_chars
from jans.pycloudlib.utils import encode_text
from jans.pycloudlib.utils import generate_base64_contents
from jans.pycloudlib.utils import as_boolean
from jans.pycloudlib.utils import get_server_certificate

from settings import LOGGING_CONFIG
from utils import generalized_time_utc
from utils import get_ads_project_base64
from utils import CASA_AGAMA_DEPLOYMENT_ID
from utils import CASA_AGAMA_ARCHIVE

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("jans-casa")

manager = get_manager()


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
        custom_config = json.loads(os.environ.get("CN_CASA_APP_LOGGERS", "{}"))
    except json.decoder.JSONDecodeError as exc:
        logger.warning(f"Unable to load logging configuration from environment variable; reason={exc}; fallback to defaults")
        custom_config = {}

    # ensure custom config is ``dict`` type
    if not isinstance(custom_config, dict):
        logger.warning("Invalid data type for CN_CASA_APP_LOGGERS; fallback to defaults")
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
        as_boolean(os.environ.get("CN_ENABLE_STDOUT_LOG_PREFIX")),
    ]):
        config["log_prefix"] = "${sys:casa.log.console.prefix}%X{casa.log.console.group} - "

    with open("/app/templates/jans-casa/log4j2.xml") as f:
        txt = f.read()

    logfile = "/opt/jans/jetty/jans-casa/resources/log4j2.xml"
    tmpl = Template(txt)
    with open(logfile, "w") as f:
        f.write(tmpl.safe_substitute(config))


def main():
    persistence_type = os.environ.get("CN_PERSISTENCE_TYPE", "sql")

    render_salt(manager, "/app/templates/salt", "/etc/jans/conf/salt")
    render_base_properties("/app/templates/jans.properties", "/etc/jans/conf/jans.properties")

    mapper = PersistenceMapper()
    persistence_groups = mapper.groups()

    if persistence_type == "hybrid":
        render_hybrid_properties("/etc/jans/conf/jans-hybrid.properties")

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

    with manager.create_lock("casa-setup"):
        persistence_setup = PersistenceSetup(manager)
        persistence_setup.import_ldif_files()

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


class PersistenceSetup:
    def __init__(self, manager):
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
    def ctx(self):
        hostname = self.manager.config.get("hostname")

        ctx = {
            "hostname": hostname,
            "casa_redirect_uri": f"https://{hostname}/jans-casa",
            "casa_redirect_logout_uri": f"https://{hostname}/jans-casa/bye.zul",
            "casa_frontchannel_logout_uri": f"https://{hostname}/jans-casa/autologout",
            "casa_agama_deployment_id": CASA_AGAMA_DEPLOYMENT_ID,
        }

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

        with open("/app/templates/jans-casa/casa-config.json") as f:
            ctx["casa_config_base64"] = generate_base64_contents(f.read() % ctx)

        ctx["jans_start_date"] = generalized_time_utc()
        ctx["ads_prj_assets_base64"] = get_ads_project_base64(CASA_AGAMA_ARCHIVE)

        # finalized contexts
        return ctx

    @cached_property
    def ldif_files(self):
        filenames = ["configuration.ldif", "client.ldif"]

        # generate extra scopes
        self.generate_scopes_ldif()
        filenames.append("scopes.ldif")

        return [f"/app/templates/jans-casa/{filename}" for filename in filenames]

    def _deprecated_script_exists(self):
        # deprecated Casa script DN
        id_ = "inum=BABA-CACA,ou=scripts,o=jans"
        return bool(self.client.get("jansCustomScr", doc_id_from_dn(id_)))

    def import_ldif_files(self):
        for file_ in self.ldif_files:
            logger.info(f"Importing {file_}")
            self.client.create_from_ldif(file_, self.ctx)

    def generate_scopes_ldif(self):
        # prepare required scopes (if any)
        with open("/app/templates/jans-casa/scopes.json") as f:
            scopes = json.loads(f.read())

        with open("/app/templates/jans-casa/scopes.ldif", "wb") as fd:
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
