"""
jans.pycloudlib.persistence.hybrid
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This module contains various helpers related to hybrid (LDAP + Couchbase) persistence.
"""

import os

from jans.pycloudlib.persistence.couchbase import get_couchbase_mappings
from jans.pycloudlib.persistence.couchbase import prefixed_couchbase_mappings


def render_hybrid_properties(dest: str) -> None:
    """Render file contains properties to connect to hybrid
    persistence, i.e. ``/etc/jans/conf/jans-hybrid.properties``.

    :params dest: Absolute path where generated file is located.
    """
    persistence_type = os.environ.get("CN_PERSISTENCE_TYPE", "couchbase")
    ldap_mapping = os.environ.get("CN_PERSISTENCE_LDAP_MAPPING", "default")

    if ldap_mapping == "default":
        default_storage = "ldap"
    else:
        default_storage = "couchbase"

    _couchbase_mappings = get_couchbase_mappings(persistence_type, ldap_mapping)

    couchbase_mappings = ", ".join([
        mapping["mapping"]
        for name, mapping in _couchbase_mappings.items()
        if mapping["mapping"] and name != ldap_mapping
    ])
    ldap_mappings = prefixed_couchbase_mappings().get(ldap_mapping, {}).get("mapping") or "default"

    out = "\n".join([
        "storages: ldap, couchbase",
        f"storage.default: {default_storage}",
        f"storage.ldap.mapping: {ldap_mappings}",
        f"storage.couchbase.mapping: {couchbase_mappings}",
    ])

    with open(dest, "w") as fw:
        fw.write(out)
