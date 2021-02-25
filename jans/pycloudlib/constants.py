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
)

PERSISTENCE_LDAP_MAPPINGS = (
    "default",
    "user",
    "site",
    "cache",
    "token",
    "session",
)
