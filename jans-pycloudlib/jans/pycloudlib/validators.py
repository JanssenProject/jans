"""This module contains helpers to validate things."""

from jans.pycloudlib.persistence.utils import PERSISTENCE_TYPES
from jans.pycloudlib.persistence.utils import PERSISTENCE_SQL_DIALECTS
from jans.pycloudlib.persistence.utils import PersistenceMapper


def validate_persistence_type(type_: str) -> None:
    """Validate persistence type.

    Supported types:

    - `hybrid`
    - `sql`

    Args:
        type_: Persistence type.
    """
    if type_ not in PERSISTENCE_TYPES:
        types = ", ".join(PERSISTENCE_TYPES)

        raise ValueError(
            f"Unsupported persistence type {type_}; "
            f"please choose one of {types}"
        )


def validate_persistence_sql_dialect(dialect: str) -> None:
    """Validate SQL dialect.

    Supported dialects:

    - `mysql`

    Args:
        dialect: Dialect of SQL.
    """
    if dialect not in PERSISTENCE_SQL_DIALECTS:
        dialects = ", ".join(PERSISTENCE_SQL_DIALECTS)
        raise ValueError(
            f"Unsupported persistence sql dialects; "
            f"please choose one of {dialects}"
        )


def validate_persistence_hybrid_mapping() -> None:
    """Validate hybrid mapping."""
    mapper = PersistenceMapper()
    mapper.validate_hybrid_mapping()
