import base64
import os
import re
from urllib.parse import urlparse

from jans.pycloudlib import get_manager
from jans.pycloudlib.persistence import render_couchbase_properties
from jans.pycloudlib.persistence import render_base_properties
from jans.pycloudlib.persistence import render_hybrid_properties
from jans.pycloudlib.persistence import render_ldap_properties
from jans.pycloudlib.persistence import render_salt
from jans.pycloudlib.persistence import sync_couchbase_truststore
from jans.pycloudlib.persistence import sync_ldap_truststore
from jans.pycloudlib.persistence import render_sql_properties
from jans.pycloudlib.utils import cert_to_truststore
from jans.pycloudlib.utils import get_server_certificate
# from jans.pycloudlib.utils import as_boolean
from jans.pycloudlib.utils import generate_keystore

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

    # disable Jetty version info
    updates = re.sub(
        r'(<Set name="sendServerVersion"><Property name="jetty.httpConfig.sendServerVersion" deprecated="jetty.send.server.version" default=")true(" /></Set>)',
        r'\1false\2',
        updates,
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
        # sync_couchbase_cert(manager)
        sync_couchbase_truststore(manager)

    if persistence_type == "hybrid":
        render_hybrid_properties("/etc/jans/conf/jans-hybrid.properties")

    if persistence_type == "sql":
        render_sql_properties(
            manager,
            "/app/templates/jans-sql.properties.tmpl",
            "/etc/jans/conf/jans-sql.properties",
        )

    if not os.path.isfile("/etc/certs/web_https.crt"):
        manager.secret.to_file("ssl_cert", "/etc/certs/web_https.crt")

    cert_to_truststore(
        "web_https",
        "/etc/certs/web_https.crt",
        "/usr/lib/jvm/default-jvm/jre/lib/security/cacerts",
        "changeit",
    )

    # if not os.path.isfile("/etc/certs/idp-signing.crt"):
    #     manager.secret.to_file("idp3SigningCertificateText", "/etc/certs/idp-signing.crt")

    # manager.secret.to_file("passport_rp_jks_base64", "/etc/certs/passport-rp.jks",
    #                        decode=True, binary_mode=True)

    # manager.secret.to_file("api_rp_jks_base64", "/etc/certs/api-rp.jks",
    #                        decode=True, binary_mode=True)
    # with open(manager.config.get("api_rp_client_jwks_fn"), "w") as f:
    #     f.write(
    #         base64.b64decode(manager.secret.get("api_rp_client_base64_jwks")).decode(),
    #     )

    # manager.secret.to_file("api_rs_jks_base64", "/etc/certs/api-rs.jks",
    #                        decode=True, binary_mode=True)
    # with open(manager.config.get("api_rs_client_jwks_fn"), "w") as f:
    #     f.write(
    #         base64.b64decode(manager.secret.get("api_rs_client_base64_jwks")).decode(),
    #     )

    modify_jetty_xml()
    modify_webdefault_xml()

    ext_jwks_uri = os.environ.get("CN_EXT_SIGNING_JWKS_URI", "")

    if ext_jwks_uri:
        ext_cert = "/etc/certs/ext-signing.crt"
        ext_key = "/etc/certs/ext-signing.key"
        alias = os.environ.get("CN_EXT_SIGNING_ALIAS", "OpenBanking")

        parsed_url = urlparse(ext_jwks_uri)
        # uses hostname instead of netloc as netloc may have host:port format
        hostname = parsed_url.hostname

        # get port listed in netloc or fallback to port 443
        port = parsed_url.port or 443

        get_server_certificate(
            hostname,
            port,
            "/etc/certs/extjwksuri.crt"
        )

        cert_to_truststore(
            "OpenBankingJwksUri",
            "/etc/certs/extjwksuri.crt",
            "/usr/lib/jvm/default-jvm/jre/lib/security/cacerts",
            "changeit",
        )

        cert_to_truststore(
            alias,
            ext_cert,
            "/usr/lib/jvm/default-jvm/jre/lib/security/cacerts",
            "changeit",
        )

        generate_keystore(
            "ext-signing",
            manager.config.get("hostname"),
            manager.secret.get("auth_openid_jks_pass"),
            jks_fn="/etc/certs/ext-signing.jks",
            in_key=ext_key,
            in_cert=ext_cert,
            alias=alias,
        )
    else:
        # sync_enabled = as_boolean(os.environ.get("CN_SYNC_JKS_ENABLED", False))
        # if not sync_enabled:
        manager.secret.to_file(
            "auth_jks_base64",
            "/etc/certs/auth-keys.jks",
            decode=True,
            binary_mode=True,
        )
        with open("/etc/certs/auth-keys.json", "w") as f:
            f.write(base64.b64decode(manager.secret.get("auth_openid_key_base64")).decode())


if __name__ == "__main__":
    main()
