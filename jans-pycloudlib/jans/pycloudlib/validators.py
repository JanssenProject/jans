"""
jans.pycloudlib.validators
~~~~~~~~~~~~~~~~~~~~~~~~~~

This module contains helpers to validate things.
"""

from jans.pycloudlib.persistence.utils import PERSISTENCE_TYPES
from jans.pycloudlib.persistence.utils import PERSISTENCE_SQL_DIALECTS
from jans.pycloudlib.persistence.utils import PersistenceMapper


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
def validate_persistence_ldap_mapping(type_: str, ldap_mapping: str) -> None:
    import warnings

    warnings.warn(
        "'validate_persistence_ldap_mapping' function is now a no-op function "
        "and no longer required by hybrid persistence; "
        "use 'validate_persistence_hybrid_mapping' function instead",
        DeprecationWarning)


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


def validate_persistence_hybrid_mapping() -> None:
    """
    Validate hybrid mapping.
    """
    mapper = PersistenceMapper()
    mapper.validate_hybrid_mapping()
