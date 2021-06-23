import os

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
from jans.pycloudlib.utils import cert_to_truststore
from jans.pycloudlib.utils import safe_render

from utils import get_injected_urls


def render_app_properties(manager):
    client_id = manager.config.get("jca_client_id")
    client_encoded_pw = manager.secret.get("jca_client_encoded_pw")
    hostname = manager.config.get("hostname")

    approved_issuer = os.environ.get("CN_APPROVED_ISSUER") or f"https://{hostname}"

    ctx = {
        "jca_log_level": os.environ.get("CN_CONFIG_API_LOG_LEVEL", "INFO"),
        "jca_client_id": client_id,
        "jca_client_encoded_pw": client_encoded_pw,
        "approved_issuer": approved_issuer,
    }
    ctx.update(get_injected_urls())

    with open("/app/templates/application.properties.tmpl") as f:
        txt = safe_render(f.read(), ctx)

    with open("/opt/jans/jans-config-api/config/application.properties", "w") as f:
        f.write(txt)


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

    if persistence_type == "sql":
        render_sql_properties(
            manager,
            "/app/templates/jans-sql.properties.tmpl",
            "/etc/jans/conf/jans-sql.properties",
        )

    if persistence_type == "spanner":
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
        "/usr/lib/jvm/default-jvm/jre/lib/security/cacerts",
        "changeit",
    )

    render_app_properties(manager)


if __name__ == "__main__":
    main()
