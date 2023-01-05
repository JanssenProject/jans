"""This module contains various helpers related to LDAP persistence."""

from __future__ import annotations

import logging
import os
import time
import typing as _t
from tempfile import NamedTemporaryFile

from ldap3 import BASE
from ldap3 import Connection
from ldap3 import Server
from ldap3 import SUBTREE
from ldap3 import MODIFY_REPLACE as LDAP3_MODIFY_REPLACE
from ldap3 import MODIFY_DELETE as LDAP3_MODIFY_DELETE
from ldap3.core.exceptions import LDAPSessionTerminatedByServerError
from ldap3.core.exceptions import LDAPSocketOpenError
from ldif import LDIFParser

from jans.pycloudlib.utils import as_boolean
from jans.pycloudlib.utils import decode_text
from jans.pycloudlib.utils import safe_render

if _t.TYPE_CHECKING:  # pragma: no cover
    # imported objects for function type hint, completion, etc.
    # these won't be executed in runtime
    from jans.pycloudlib.manager import Manager


logger = logging.getLogger(__name__)


def render_ldap_properties(manager: Manager, src: str, dest: str) -> None:
    """Render file contains properties to connect to LDAP server.

    Args:
        manager: An instance of manager class.
        src: Absolute path to the template.
        dest: Absolute path where generated file is located.
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


def sync_ldap_truststore(manager: Manager, dest: str = "") -> None:
    """Pull secret contains base64-string contents of LDAP truststore and save it as a JKS file.

    Args:
        manager: An instance of manager class.
        dest: Absolute path where generated file is located.
    """
    dest = dest or manager.config.get("ldapTrustStoreFn")
    manager.secret.to_file(
        "ldap_pkcs12_base64", dest, decode=True, binary_mode=True,
    )


def extract_ldap_host(url: str) -> str:
    """Extract host part from a URL.

    Args:
        url: URL of LDAP server, i.e. ``localhost:1636``.
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
    """Thin client to interact with LDAP server.

    Args:
        manager: An instance of manager class.
        *args: Positional arguments (if any).
        **kwargs: Keyword arguments (if any).
    """

    # shortcut to ldap3.MODIFY_REPLACE
    MODIFY_REPLACE: _t.Final[str] = LDAP3_MODIFY_REPLACE

    # shortcut to ldap3.MODIFY_DELETE
    MODIFY_DELETE: _t.Final[str] = LDAP3_MODIFY_DELETE

    def __init__(self, manager: Manager, *args: _t.Any, **kwargs: _t.Any) -> None:
        host = kwargs.get("host", "") or os.environ.get("CN_LDAP_URL", "localhost:1636")
        # we only need host part as port will be resolved from env that controls
        # SSL or non-SSL connetion
        host = extract_ldap_host(host)
        user = kwargs.get("user") or manager.config.get("ldap_binddn")
        password = kwargs.get("password") or decode_text(
            manager.secret.get("encoded_ox_ldap_pw"),
            manager.secret.get("encoded_salt")
        )

        use_ssl = as_boolean(os.environ.get("CN_LDAP_USE_SSL", True))
        port = resolve_ldap_port()
        self.server = Server(host, port=port, use_ssl=use_ssl)
        self.conn = Connection(self.server, user, password)
        self.manager = manager

    def is_connected(self) -> bool:
        """Check whether client is connected by getting a simple entry."""
        return bool(self.get("", attributes=["1.1"]))

    def get(
        self,
        dn: str,
        filter_: str = "(objectClass=*)",
        attributes: _t.Union[list[str], None] = None,
    ) -> _t.Union[None, _t.Any]:
        """Get a single entry.

        Args:
            dn: Base DN.
            filter_: Search filter.
            attributes: List of returning attributes.
        """
        entries = self.search(dn, filter_=filter_, attributes=attributes, limit=1, scope=BASE)
        if not entries:
            return None
        return entries[0]

    def search(
        self,
        dn: str,
        filter_: str = "(objectClass=*)",
        attributes: _t.Union[list[str], None] = None,
        limit: int = 0,
        scope: str = "",
    ) -> _t.Any:
        """Search for entries.

        Args:
            dn: Base DN to start with.
            filter_: Search filter.
            attributes: List of returning attributes.
            limit: Number of returned entry.
            scope: Search scope.
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

    def delete(self, dn: str) -> tuple[bool, str]:
        """Delete entry.

        Args:
            dn: Base DN to modify.
        """
        with self.conn as conn:
            conn.delete(dn)
            deleted = bool(conn.result["description"] == "success")
            message = conn.result["message"]
            return deleted, message

    def add(self, dn: str, attributes: dict[str, _t.Any]) -> tuple[bool, str]:
        """Add new entry.

        Args:
            dn: New DN.
            attributes: Entry attributes.
        """
        with self.conn as conn:
            conn.add(dn, attributes=attributes)
            added = bool(conn.result["description"] == "success")
            message = conn.result["message"]
            return added, message

    def modify(self, dn: str, changes: dict[str, _t.Any]) -> tuple[bool, str]:
        """Modify entry.

        Args:
            dn: Base DN to modify.
            changes: Mapping of attributes.
        """
        with self.conn as conn:
            conn.modify(dn, changes)
            modified = bool(conn.result["description"] == "success")
            message = conn.result["message"]
            return modified, message

    def _add_entry(self, dn: str, attrs: dict[str, _t.Any]) -> None:
        """Create new entry.

        Args:
            dn: Base DN to add.
            attrs: Mapping of attributes to be added.
        """
        max_wait_time = 300
        sleep_duration = 10

        for _ in range(0, max_wait_time, sleep_duration):
            try:
                added, msg = self.add(dn, attributes=attrs)
                if not added and "name already exists" not in msg:
                    logger.warning(f"Unable to add entry with DN {dn}; reason={msg}")
                break
            except (LDAPSessionTerminatedByServerError, LDAPSocketOpenError) as exc:
                logger.warning(f"Unable to add entry with DN {dn}; reason={exc}; retrying in {sleep_duration} seconds")
            time.sleep(sleep_duration)

    def create_from_ldif(self, filepath: str, ctx: dict[str, _t.Any]) -> None:
        """Create entry with data loaded from an LDIF template file.

        Args:
            filepath: Path to LDIF template file.
            ctx: Key-value pairs of context that rendered into LDIF template file.
        """
        with open(filepath) as src, NamedTemporaryFile("w+") as dst:
            dst.write(safe_render(src.read(), ctx))
            # ensure rendered template is written
            dst.flush()

            with open(dst.name, "rb") as fd:
                parser = LDIFParser(fd)

                for dn, entry in parser.parse():
                    self._add_entry(dn, entry)
