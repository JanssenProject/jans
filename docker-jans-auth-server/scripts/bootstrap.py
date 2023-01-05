import base64
import json
import os
import re
from urllib.parse import urlparse
from contextlib import suppress
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
from jans.pycloudlib.utils import get_server_certificate
from jans.pycloudlib.utils import generate_keystore
from jans.pycloudlib.utils import as_boolean

from keystore_mod import modify_keystore_path

import logging.config
from settings import LOGGING_CONFIG

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("entrypoint")

manager = get_manager()


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


def main():
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
        # sync_couchbase_cert(manager)
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

    if not os.path.isfile("/etc/certs/web_https.crt"):
        manager.secret.to_file("ssl_cert", "/etc/certs/web_https.crt")

    cert_to_truststore(
        "web_https",
        "/etc/certs/web_https.crt",
        "/usr/java/latest/jre/lib/security/cacerts",
        "changeit",
    )

    # if not os.path.isfile("/etc/certs/idp-signing.crt"):
    #     manager.secret.to_file("idp3SigningCertificateText", "/etc/certs/idp-signing.crt")

    # manager.secret.to_file("passport_rp_jks_base64", "/etc/certs/passport-rp.jks",
    #                        decode=True, binary_mode=True)

    modify_jetty_xml()
    modify_webdefault_xml()
    configure_logging()

    ext_jwks_uri = os.environ.get("CN_OB_EXT_SIGNING_JWKS_URI", "")

    if ext_jwks_uri:
        # Open Banking external signing cert and key. Use for generating the PKCS12 and jks keystore
        ext_cert = "/etc/certs/ob-ext-signing.crt"
        ext_key = "/etc/certs/ob-ext-signing.key"
        ext_key_pin = "/etc/certs/ob-ext-signing.pin"

        # Open Banking transport signing cert and key. Use for generating the PKCS12 file.
        ob_transport_cert = "/etc/certs/ob-transport.crt"
        ob_transport_key = "/etc/certs/ob-transport.key"
        ob_transport_pin = "/etc/certs/ob-transport.pin"
        ob_transport_alias = os.environ.get("CN_OB_AS_TRANSPORT_ALIAS", "OpenBankingAsTransport")

        ob_ext_alias = os.environ.get("CN_OB_EXT_SIGNING_ALIAS", "OpenBanking")

        parsed_url = urlparse(ext_jwks_uri)
        # uses hostname instead of netloc as netloc may have host:port format
        hostname = parsed_url.hostname

        # get port listed in netloc or fallback to port 443
        port = parsed_url.port or 443

        get_server_certificate(
            hostname,
            port,
            "/etc/certs/obextjwksuri.crt"
        )

        cert_to_truststore(
            "OpenBankingJwksUri",
            "/etc/certs/obextjwksuri.crt",
            "/usr/java/latest/jre/lib/security/cacerts",
            "changeit",
        )

        cert_to_truststore(
            ob_ext_alias,
            ext_cert,
            "/usr/java/latest/jre/lib/security/cacerts",
            "changeit",
        )

        ext_key_passphrase = ""
        with suppress(FileNotFoundError):
            with open(ext_key_pin) as f:
                ext_key_passphrase = f.read().strip()

        generate_keystore(
            "ob-ext-signing",
            manager.config.get("hostname"),
            manager.secret.get("auth_openid_jks_pass"),
            jks_fn="/etc/certs/ob-ext-signing.jks",
            in_key=ext_key,
            in_cert=ext_cert,
            alias=ob_ext_alias,
            in_passwd=ext_key_passphrase,
        )

        if os.path.isfile(ob_transport_cert):
            cert_to_truststore(
                ob_transport_alias,
                ob_transport_cert,
                "/usr/java/latest/jre/lib/security/cacerts",
                "changeit",
            )

            ob_transport_passphrase = ""
            with suppress(FileNotFoundError):
                with open(ob_transport_pin) as f:
                    ob_transport_passphrase = f.read().strip()

            generate_keystore(
                "ob-transport",
                manager.config.get("hostname"),
                manager.secret.get("auth_openid_jks_pass"),
                jks_fn="/etc/certs/ob-transport.jks",
                in_key=ob_transport_key,
                in_cert=ob_transport_cert,
                alias=ob_transport_alias,
                in_passwd=ob_transport_passphrase,
            )

        keystore_path = "/etc/certs/ob-ext-signing.jks"
        jwks_uri = ext_jwks_uri
    else:
        manager.secret.to_file(
            "auth_jks_base64",
            "/etc/certs/auth-keys.jks",
            decode=True,
            binary_mode=True,
        )
        with open("/etc/certs/auth-keys.json", "w") as f:
            f.write(base64.b64decode(manager.secret.get("auth_openid_key_base64")).decode())

        keystore_path = "/etc/certs/auth-keys.jks"
        jwks_uri = f"https://{manager.config.get('hostname')}/jans-auth/restv1/jwks"

    # ensure we're using correct JKS file and JWKS uri
    modify_keystore_path(manager, keystore_path, jwks_uri)


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

    if as_boolean(custom_config.get("enable_stdout_log_prefix")):
        config["log_prefix"] = "${sys:log.console.prefix}%X{log.console.group} - "

    with open("/app/templates/log4j2.xml") as f:
        txt = f.read()

    logfile = "/opt/jans/jetty/jans-auth/resources/log4j2.xml"
    tmpl = Template(txt)
    with open(logfile, "w") as f:
        f.write(tmpl.safe_substitute(config))


if __name__ == "__main__":
    main()
