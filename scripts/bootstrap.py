import os

from jans.pycloudlib import get_manager
from jans.pycloudlib.persistence import render_couchbase_properties
from jans.pycloudlib.persistence import render_base_properties
from jans.pycloudlib.persistence import render_hybrid_properties
from jans.pycloudlib.persistence import render_ldap_properties
from jans.pycloudlib.persistence import render_salt
from jans.pycloudlib.persistence import sync_couchbase_truststore
from jans.pycloudlib.persistence import sync_ldap_truststore
from jans.pycloudlib.utils import as_boolean
from jans.pycloudlib.utils import get_server_certificate
from jans.pycloudlib.utils import cert_to_truststore


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

    # config-api requires /etc/certs/httpd.crt
    if not os.path.isfile("/etc/certs/httpd.crt"):
        if as_boolean(os.environ.get("CN_SSL_CERT_FROM_SECRETS", False)):
            manager.secret.to_file("ssl_cert", "/etc/certs/httpd.crt")
            manager.secret.to_file("ssl_key", "/etc/certs/httpd.key")
        else:
            get_server_certificate(manager.config.get("hostname"), 443, "/etc/certs/httpd.crt")

    cert_to_truststore(
        "web_https",
        "/etc/certs/httpd.crt",
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

    # modify_jetty_xml()
    # modify_webdefault_xml()

    # sync_enabled = as_boolean(os.environ.get("CN_SYNC_JKS_ENABLED", False))
    # if not sync_enabled:
    #     manager.secret.to_file(
    #         "oxauth_jks_base64",
    #         "/etc/certs/oxauth-keys.jks",
    #         decode=True,
    #         binary_mode=True,
    #     )
    #     with open("/etc/certs/oxauth-keys.json", "w") as f:
    #         f.write(base64.b64decode(manager.secret.get("oxauth_openid_key_base64")).decode())


if __name__ == "__main__":
    main()
