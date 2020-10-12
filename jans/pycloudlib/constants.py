"""
jans.pycloudlib.constants
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This module contains values that are not supposedly modified.
"""

PERSISTENCE_TYPES = (
    "ldap",
    "couchbase",
    "hybrid",
)

PERSISTENCE_LDAP_MAPPINGS = (
    "default",
    "user",
    "site",
    "cache",
    "token",
    "session",
)

COUCHBASE_MAPPINGS = {
    "default": {"bucket": "gluu", "mapping": ""},
    "user": {"bucket": "gluu_user", "mapping": "people, groups, authorizations"},
    "cache": {"bucket": "gluu_cache", "mapping": "cache"},
    "site": {"bucket": "gluu_site", "mapping": "cache-refresh"},
    "token": {"bucket": "gluu_token", "mapping": "tokens"},
    "session": {"bucket": "gluu_session", "mapping": "sessions"},
}
