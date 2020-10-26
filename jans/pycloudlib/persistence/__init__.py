import os

from jans.pycloudlib.persistence.couchbase import (  # noqa: F401
    render_couchbase_properties,
    sync_couchbase_truststore,
)
from jans.pycloudlib.persistence.hybrid import render_hybrid_properties  # noqa: F401
from jans.pycloudlib.persistence.ldap import (  # noqa: F401
    render_ldap_properties,
    sync_ldap_truststore,
)


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
            "namespace": os.environ.get("CN_NAMESPACE", "jans"),
        }
        f.write(rendered_txt)
