"""This module consists of common utilities to work with persistence."""

import json
import os
from collections import defaultdict
from typing import Dict


def render_salt(manager, src: str, dest: str) -> None:
    """Render file contains salt string.

    The generated file has the following contents:

    .. code-block:: text

        encode_salt = random-salt-string

    :param manager: An instance of :class:`~jans.pycloudlib.manager._Manager`.
    :param src: Absolute path to the template.
    :param dest: Absolute path where generated file is located.
    """
    encode_salt = manager.secret.get("encoded_salt")

    with open(src) as f:
        txt = f.read()

    with open(dest, "w") as f:
        rendered_txt = txt % {"encode_salt": encode_salt}
        f.write(rendered_txt)


def render_base_properties(src: str, dest: str) -> None:
    """Render file contains properties for Janssen Server.

    :param src: Absolute path to the template.
    :param dest: Absolute path where generated file is located.
    """
    with open(src) as f:
        txt = f.read()

    with open(dest, "w") as f:
        rendered_txt = txt % {
            "persistence_type": os.environ.get("CN_PERSISTENCE_TYPE", "ldap"),
        }
        f.write(rendered_txt)


#: Supported persistence types
PERSISTENCE_TYPES = (
    "ldap",
    "couchbase",
    "sql",
    "spanner",
    "hybrid",
)

#: Data mapping of persistence, ordered by priority
PERSISTENCE_DATA_KEYS = (
    "default",
    "user",
    "site",
    "cache",
    "token",
    "session",
)

#: Supported SQL dialects
PERSISTENCE_SQL_DIALECTS = (
    "mysql",
    "pgsql",
)

RDN_MAPPING = {
    "default": "",
    "user": "people, groups, authorizations",
    "cache": "cache",
    "site": "cache-refresh",
    "token": "tokens",
    "session": "sessions",
}


class PersistenceMapper:
    """
    This class creates persistence data mapping.

    Example of data mapping when using ``sql`` persistence type:

    .. codeblock:: python

        os.environ["CN_PERSISTENCE_TYPE"] = "sql"

        mapper = PersistenceMapper()
        mapper.validate_hybrid_mapping()
        print(mapper.mapping)

    The output will be:

    .. codeblock:: python

        {
            "default": "sql",
            "user": "sql",
            "site": "sql",
            "cache": "sql",
            "token": "sql",
            "session": "sql",
        }

    The same rule applies to any supported persistence types, except for ``hybrid``
    where each key can have different value. To customize the mapping, additional environment
    variable is required.

    .. codeblock:: python

        os.environ["CN_PERSISTENCE_TYPE"] = "hybrid"
        os.environ["CN_HYBRID_MAPPING"] = json.loads({
            "default": "sql", "user": "spanner", "site": "sql", "cache": "sql", "token": "sql", "session": "sql"
        })

        mapper = PersistenceMapper()
        mapper.validate_hybrid_mapping()
        print(mapper.mapping)

    The output will be:

    .. codeblock:: python

        {
            "default": "sql",
            "user": "spanner",
            "site": "sql",
            "cache": "sql",
            "token": "sql",
            "session": "sql",
        }

    Note that when using ``hybrid``, all mapping must be defined explicitly.
    """

    def __init__(self) -> None:
        self.type = os.environ.get("CN_PERSISTENCE_TYPE", "ldap")
        self._mapping = {}

    @property
    def mapping(self) -> Dict[str, str]:
        """Pre-populate a key-value pair of persistence data (if empty).

        Example of pre-populated mapping:

        .. codeblock:: python

            {
                "default": "sql",
                "user": "spanner",
                "site": "sql",
                "cache": "sql",
                "token": "sql",
                "session": "sql",
            }
        """
        if not self._mapping:
            if self.type != "hybrid":
                self._mapping = dict.fromkeys(PERSISTENCE_DATA_KEYS, self.type)
            else:
                self._mapping = self.validate_hybrid_mapping()
        return self._mapping

    def groups(self) -> Dict[str, list]:
        """Pre-populate mapping groupped by persistence type.

        Example of pre-populated groupped mapping:

        .. codeblock:: python

            {
                "sql": ["cache", "default", "session"],
                "couchbase": ["user"],
                "spanner": ["token"],
                "ldap": ["site"],
            }
        """
        mapper = defaultdict(list)

        for k, v in self.mapping.items():
            mapper[v].append(k)
        return dict(sorted(mapper.items()))

    def groups_with_rdn(self) -> Dict[str, list]:
        """Pre-populate mapping groupped by persistence type and its values replaced by RDN.

        Example of pre-populated groupped mapping:

        .. codeblock:: python

            {
                "sql": ["cache", "", "sessions"],
                "couchbase": ["people, groups, authorizations"],
                "spanner": ["tokens"],
                "ldap": ["cache-refresh"],
            }
        """
        mapper = defaultdict(list)
        for k, v in self.mapping.items():
            mapper[v].append(RDN_MAPPING[k])
        return dict(sorted(mapper.items()))

    @classmethod
    def validate_hybrid_mapping(cls) -> Dict[str, list]:
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
