import os
import re
from pathlib import Path

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


def get_template_paths(path: str) -> tuple[str, str]:
    tmpl = Path("/app/templates").joinpath(path.lstrip("/"))
    return str(tmpl), path


def render_persistence_props(manager):
    persistence_type = os.environ.get("CN_PERSISTENCE_TYPE", "ldap")

    render_salt(manager, *get_template_paths("/etc/jans/conf/salt"))
    render_base_properties(*get_template_paths("/etc/jans/conf/jans.properties"))

    mapper = PersistenceMapper()
    persistence_groups = mapper.groups()

    if persistence_type == "hybrid":
        render_hybrid_properties("/etc/jans/conf/jans-hybrid.properties")

    if "ldap" in persistence_groups:
        render_ldap_properties(
            manager,
            *get_template_paths("/etc/jans/conf/jans-ldap.properties")
        )
        sync_ldap_truststore(manager)

    if "couchbase" in persistence_groups:
        render_couchbase_properties(
            manager,
            *get_template_paths("/etc/jans/conf/jans-couchbase.properties")
        )
        sync_couchbase_truststore(manager)

    if "sql" in persistence_groups:
        db_dialect = os.environ.get("CN_SQL_DB_DIALECT", "mysql")
        src, dst = get_template_paths(f"/etc/jans/conf/jans-{db_dialect}.properties")
        render_sql_properties(manager, src, dst.replace(db_dialect, "sql"))

    if "spanner" in persistence_groups:
        render_spanner_properties(
            manager,
            *get_template_paths("/etc/jans/conf/jans-spanner.properties")
        )


def import_cert_to_truststore(manager):
    if not os.path.isfile("/etc/certs/web_https.crt"):
        manager.secret.to_file("ssl_cert", "/etc/certs/web_https.crt")

    cert_to_truststore(
        "web_https",
        "/etc/certs/web_https.crt",
        "/opt/java/lib/security/cacerts",
        "changeit",
    )
