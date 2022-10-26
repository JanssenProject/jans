"""This module consists of common utilities to work with persistence."""

from __future__ import annotations

import json
import os
import typing as _t
from collections import defaultdict

if _t.TYPE_CHECKING:  # pragma: no cover
    # imported objects for function type hint, completion, etc.
    # these won't be executed in runtime
    from jans.pycloudlib.manager import Manager


def render_salt(manager: Manager, src: str, dest: str) -> None:
    """Render file contains salt string.

    The generated file has the following contents:

    ```py
    encode_salt = random-salt-string
    ```

    Args:
        manager: An instance of manager class.
        src: Absolute path to the template.
        dest: Absolute path where generated file is located.
    """
    encode_salt = manager.secret.get("encoded_salt")

    with open(src) as f:
        txt = f.read()

    with open(dest, "w") as f:
        rendered_txt = txt % {"encode_salt": encode_salt}
        f.write(rendered_txt)


def render_base_properties(src: str, dest: str) -> None:
    """Render file contains properties for Janssen Server.

    Args:
        src: Absolute path to the template.
        dest: Absolute path where generated file is located.
    """
    with open(src) as f:
        txt = f.read()

    with open(dest, "w") as f:
        rendered_txt = txt % {
            "persistence_type": os.environ.get("CN_PERSISTENCE_TYPE", "ldap"),
        }
        f.write(rendered_txt)


#: Supported persistence types.
PERSISTENCE_TYPES = (
    "ldap",
    "couchbase",
    "sql",
    "spanner",
    "hybrid",
)
"""Supported persistence types."""

PERSISTENCE_DATA_KEYS = (
    "default",
    "user",
    "site",
    "cache",
    "token",
    "session",
)
"""Data mapping of persistence, ordered by priority."""

PERSISTENCE_SQL_DIALECTS = (
    "mysql",
    "pgsql",
)
"""SQL dialects.

!!! warning
    The `pgsql` dialect is in experimental phase and may introduce bugs
    hence it is not recommended at the moment."""

RDN_MAPPING = {
    "default": "",
    "user": "people, groups, authorizations",
    "cache": "cache",
    "site": "cache-refresh",
    "token": "tokens",
    "session": "sessions",
}
"""Mapping of RDN (Relative Distinguished Name)."""


class PersistenceMapper:
    """This class creates persistence data mapping.

    Example of data mapping when using ``sql`` persistence type:

    ```py
    os.environ["CN_PERSISTENCE_TYPE"] = "sql"
    mapper = PersistenceMapper()
    mapper.validate_hybrid_mapping()
    print(mapper.mapping)
    ```

    The output will be:

    ```py
    {
        "default": "sql",
        "user": "sql",
        "site": "sql",
        "cache": "sql",
        "token": "sql",
        "session": "sql",
    }
    ```

    The same rule applies to any supported persistence types, except for ``hybrid``
    where each key can have different value. To customize the mapping, additional environment
    variable is required.

    ```py
    os.environ["CN_PERSISTENCE_TYPE"] = "hybrid"
    os.environ["CN_HYBRID_MAPPING"] = json.loads({
        "default": "sql",
        "user": "spanner",
        "site": "sql",
        "cache": "sql",
        "token": "sql",
        "session": "sql",
    })

    mapper = PersistenceMapper()
    mapper.validate_hybrid_mapping()
    print(mapper.mapping)
    ```

    The output will be:

    ```py
    {
        "default": "sql",
        "user": "spanner",
        "site": "sql",
        "cache": "sql",
        "token": "sql",
        "session": "sql",
    }
    ```

    Note that when using ``hybrid``, all mapping must be defined explicitly.
    """

    def __init__(self) -> None:
        self.type = os.environ.get("CN_PERSISTENCE_TYPE", "ldap")
        self._mapping: dict[str, str] = {}

    @property
    def mapping(self) -> dict[str, str]:
        """Pre-populate a key-value pair of persistence data (if empty).

        Example of pre-populated mapping:

        ```py
        {
            "default": "sql",
            "user": "spanner",
            "site": "sql",
            "cache": "sql",
            "token": "sql",
            "session": "sql",
        }
        ```
        """
        if not self._mapping:
            if self.type != "hybrid":
                self._mapping = dict.fromkeys(PERSISTENCE_DATA_KEYS, self.type)
            else:
                self._mapping = self.validate_hybrid_mapping()
        return self._mapping

    def groups(self) -> dict[str, list[str]]:
        """Pre-populate mapping groupped by persistence type.

        Example of pre-populated groupped mapping:

        ```py
        {
            "sql": ["cache", "default", "session"],
            "couchbase": ["user"],
            "spanner": ["token"],
            "ldap": ["site"],
        }
        ```
        """
        mapper = defaultdict(list)

        for k, v in self.mapping.items():
            mapper[v].append(k)
        return dict(sorted(mapper.items()))

    def groups_with_rdn(self) -> dict[str, list[str]]:
        """Pre-populate mapping groupped by persistence type and its values replaced by RDN.

        Example of pre-populated groupped mapping:

        ```py
        {
            "sql": ["cache", "", "sessions"],
            "couchbase": ["people, groups, authorizations"],
            "spanner": ["tokens"],
            "ldap": ["cache-refresh"],
        }
        ```
        """
        mapper = defaultdict(list)
        for k, v in self.mapping.items():
            mapper[v].append(RDN_MAPPING[k])
        return dict(sorted(mapper.items()))

    @classmethod
    def validate_hybrid_mapping(cls) -> dict[str, str]:
        """Validate the value of ``hybrid_mapping`` attribute."""
        mapping = json.loads(os.environ.get("CN_HYBRID_MAPPING", "{}"))

        # build whitelisted mapping based on supported PERSISTENCE_DATA_KEYS and PERSISTENCE_TYPES
        try:
            sanitized_mapping = {
                key: type_ for key, type_ in mapping.items()
                if key in PERSISTENCE_DATA_KEYS and type_ in PERSISTENCE_TYPES
            }
        except AttributeError:
            # likely not a dict
            raise ValueError(f"Invalid hybrid mapping {mapping}")

        if sorted(sanitized_mapping.keys()) != sorted(PERSISTENCE_DATA_KEYS):
            raise ValueError(f"Missing key(s) in hybrid mapping {mapping}")
        return sanitized_mapping
