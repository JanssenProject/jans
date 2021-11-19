"""
jans.pycloudlib.persistence.ldap
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This module contains various helpers related to LDAP persistence.
"""

import os

from ldap3 import BASE
from ldap3 import Connection
from ldap3 import Server
from ldap3 import SUBTREE
from ldap3 import MODIFY_REPLACE
from ldap3 import MODIFY_DELETE

from jans.pycloudlib.utils import as_boolean
from jans.pycloudlib.utils import decode_text


def render_ldap_properties(manager, src: str, dest: str) -> None:
    """Render file contains properties to connect to LDAP server.

    :params manager: An instance of :class:`~jans.pycloudlib.manager._Manager`.
    :params src: Absolute path to the template.
    :params dest: Absolute path where generated file is located.
    """
    ldap_url = os.environ.get("CN_LDAP_URL", "localhost:1636")
    ldap_hostname = extract_ldap_host(ldap_url)
    ldaps_port = resolve_ldap_port()

    with open(src) as f:
        txt = f.read()

    with open(dest, "w") as f:
        rendered_txt = txt % {
            "ldap_binddn": manager.config.get("ldap_binddn"),
            "encoded_ox_ldap_pw": manager.secret.get("encoded_ox_ldap_pw"),
            "ldap_hostname": ldap_hostname,
            "ldaps_port": ldaps_port,
            "ldapTrustStoreFn": manager.config.get("ldapTrustStoreFn"),
            "encoded_ldapTrustStorePass": manager.secret.get(
                "encoded_ldapTrustStorePass"
            ),
            "ssl_enabled": str(as_boolean(
                os.environ.get("CN_LDAP_USE_SSL", True)
            )).lower(),
        }
        f.write(rendered_txt)


def sync_ldap_truststore(manager, dest: str = "") -> None:
    """Pull secret contains base64-string contents of LDAP truststore,
    and save it as a JKS file, i.e. ``/etc/certs/opendj.pkcs12``.

    :params manager: An instance of :class:`~jans.pycloudlib.manager._Manager`.
    :params dest: Absolute path where generated file is located.
    """
    dest = dest or manager.config.get("ldapTrustStoreFn")
    manager.secret.to_file(
        "ldap_pkcs12_base64", dest, decode=True, binary_mode=True,
    )


def extract_ldap_host(url: str) -> str:
    """
    Extract host part from a URL.

    :param url: URL of LDAP server, i.e. ``localhost:1636``.
    """

    return url.split(":", 1)[0]


def resolve_ldap_port() -> int:
    """Determine port being used based on selected SSL or non-SSL connection."""

    use_ssl = as_boolean(os.environ.get("CN_LDAP_USE_SSL", True))
    if use_ssl:
        port = 1636
    else:
        port = 1389
    return port


class LdapClient:
    """Thin client to interact with LDAP server."""

    # shortcut to ldap3.MODIFY_REPLACE
    MODIFY_REPLACE = MODIFY_REPLACE

    # shortcut to ldap3.MODIFY_DELETE
    MODIFY_DELETE = MODIFY_DELETE

    def __init__(self, manager, host="", user="", password=""):
        """
        Initialize instance.

        :params manager: An instance of :class:`~pygluu.containerlib.manager._Manager`.
        """

        host = host or os.environ.get("CN_LDAP_URL", "localhost:1636")
        # we only need host part as port will be resolved from env that controls
        # SSL or non-SSL connetion
        host = extract_ldap_host(host)
        user = user or manager.config.get("ldap_binddn")
        password = password or decode_text(
            manager.secret.get("encoded_ox_ldap_pw"),
            manager.secret.get("encoded_salt")
        )

        use_ssl = as_boolean(os.environ.get("CN_LDAP_USE_SSL", True))
        port = resolve_ldap_port()
        self.server = Server(host, port=port, use_ssl=use_ssl)
        self.conn = Connection(self.server, user, password)

    def is_connected(self):
        """
        Check whether client is connected by getting a simple entry.
        """
        return bool(self.get("", attributes=["1.1"]))

    def get(self, dn, filter_="(objectClass=*)", attributes=None):
        """
        Get a single entry.

        :param dn: Base DN.
        :param filter_: Search filter.
        :param attributes: List of returning attributes.
        """

        entries = self.search(dn, filter_=filter_, attributes=attributes, limit=1, scope=BASE)
        if not entries:
            return None
        return entries[0]

    def search(self, dn, filter_="(objectClass=*)", attributes=None, limit=0, scope=""):
        """
        Search for entries.

        :param dn: Base DN to start with.
        :param filter_: Search filter.
        :param attributes: List of returning attributes.
        """

        attributes = attributes or ["*"]
        scope = scope or SUBTREE
        with self.conn as conn:
            conn.search(
                search_base=dn,
                search_filter=filter_,
                search_scope=scope,
                attributes=attributes,
                size_limit=limit,
            )

            if not conn.entries:
                return []
            return conn.entries

    def delete(self, dn) -> tuple:
        """
        Delete entry.

        :param dn: Base DN to modify.
        """

        with self.conn as conn:
            conn.delete(dn)
            deleted = bool(conn.result["description"] == "success")
            message = conn.result["message"]
            return deleted, message

    def add(self, dn, attributes) -> tuple:
        """
        Add new entry.

        :param dn: New DN.
        :param changes: Entry attributes.
        """

        with self.conn as conn:
            conn.add(dn, attributes=attributes)
            added = bool(conn.result["description"] == "success")
            message = conn.result["message"]
            return added, message

    def modify(self, dn, changes) -> tuple:
        """
        Modify entry.

        :param dn: Base DN to modify.
        :param changes: Mapping of attributes.
        """

        with self.conn as conn:
            conn.modify(dn, changes)
            modified = bool(conn.result["description"] == "success")
            message = conn.result["message"]
            return modified, message
