"""
jans.pycloudlib.constants
~~~~~~~~~~~~~~~~~~~~~~~~~

This module contains values that are not supposedly modified.
"""

PERSISTENCE_TYPES = (
    "ldap",
    "couchbase",
    "hybrid",
    "sql",
    "spanner",
)

PERSISTENCE_LDAP_MAPPINGS = (
    "default",
    "user",
    "site",
    "cache",
    "token",
    "session",
)

# Supported SQL dialects
PERSISTENCE_SQL_DIALECTS = (
    "mysql",
    "pgsql",
)
