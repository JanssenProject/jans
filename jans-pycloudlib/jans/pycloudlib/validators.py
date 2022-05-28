"""
jans.pycloudlib.validators
~~~~~~~~~~~~~~~~~~~~~~~~~~

This module contains helpers to validate things.
"""

import json

from jans.pycloudlib.persistence.utils import PERSISTENCE_TYPES
from jans.pycloudlib.persistence.utils import PERSISTENCE_SQL_DIALECTS
from jans.pycloudlib.persistence.utils import PERSISTENCE_DATA_KEYS


def validate_persistence_type(type_: str) -> None:
    """
    Validates persistence type.

    :param type\\_: Persistence type.
    """

    if type_ not in PERSISTENCE_TYPES:
        types = ", ".join(PERSISTENCE_TYPES)

        raise ValueError(
            f"Unsupported persistence type {type_}; "
            f"please choose one of {types}"
        )


# deprecated function
def validate_persistence_ldap_mapping(type_: str, ldap_mapping: str):  # pragma: no cover
    pass


def validate_persistence_sql_dialect(dialect: str) -> None:
    """
    Validates SQL dialect.

    :param dialect: Dialect of SQL.
    """
    if dialect not in PERSISTENCE_SQL_DIALECTS:
        dialects = ", ".join(PERSISTENCE_SQL_DIALECTS)
        raise ValueError(
            f"Unsupported persistence sql dialects; "
            f"please choose one of {dialects}"
        )


def validate_persistence_hybrid_mapping(mapping: dict) -> None:
    # invalid JSON
    try:
        mapping = json.loads(mapping)
    except (json.decoder.JSONDecodeError, TypeError):
        raise ValueError(f"Invalid hybrid mapping {mapping}")

    # not a dict-like JSON
    if not isinstance(mapping, dict):
        raise ValueError(f"Invalid hybrid mapping {mapping}")

    # empty dict
    if not mapping:
        raise ValueError("Empty hybrid mapping")

    # check for missing keys
    missing_keys = [
        key for key in PERSISTENCE_DATA_KEYS
        if key not in mapping
    ]
    if missing_keys:
        raise ValueError(f"Missing keys {missing_keys} in hybrid mapping {mapping}")

    invalid_kv = {
        k: v for k, v in mapping.items()
        if v not in PERSISTENCE_TYPES
    }
    if invalid_kv:
        raise ValueError(f"Invalid key-pairs {invalid_kv} in hybrid mapping {mapping}")
