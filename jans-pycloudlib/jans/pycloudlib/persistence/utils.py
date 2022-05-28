"""
jans.pycloudlib.persistence.utils
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This module consists of common utilities to work with persistence.
"""

import json
import os


def render_salt(manager, src: str, dest: str) -> None:
    """Render file contains salt string.

    The generated file has the following contents:

    .. code-block:: text

        encode_salt = random-salt-string

    :params manager: An instance of :class:`~jans.pycloudlib.manager._Manager`.
    :params src: Absolute path to the template.
    :params dest: Absolute path where generated file is located.
    """
    encode_salt = manager.secret.get("encoded_salt")

    with open(src) as f:
        txt = f.read()

    with open(dest, "w") as f:
        rendered_txt = txt % {"encode_salt": encode_salt}
        f.write(rendered_txt)


def render_base_properties(src: str, dest: str) -> None:
    """Render file contains properties for Janssen Server.

    :params src: Absolute path to the template.
    :params dest: Absolute path where generated file is located.
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

#: Data mapping of persistence
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


def resolve_persistence_data_mapping() -> dict:
    """
    Resolve data mapping for persistence.

    The format of output is a key-value pair of data mapping name and its persistence type.

    Example of data mapping when using ``sql`` persistence type:

    .. codeblock:: python

        os.environ["CN_PERSISTENCE_TYPE"] = "sql"
        print(resolve_persistence_data_mapping())

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
        print(resolve_persistence_data_mapping())

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
    type_ = os.environ.get("CN_PERSISTENCE_TYPE")

    if type_ != "hybrid":
        return dict.fromkeys(PERSISTENCE_DATA_KEYS, type_)
    return json.loads(os.environ.get("CN_HYBRID_MAPPING", "{}"))
