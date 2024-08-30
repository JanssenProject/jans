import json
import logging
import os
import typing as _t
from functools import cached_property

from ldap3 import Connection
from ldap3 import Server
from ldap3 import MODIFY_ADD
from ldap3 import MODIFY_REPLACE
from ldap3 import BASE

from jans.pycloudlib.lock.base_lock import BaseLock
from jans.pycloudlib.utils import as_boolean
from jans.pycloudlib.utils import get_password_from_file

logger = logging.getLogger(__name__)

_LOCK_OBJECTCLASS_SCHEMA = """( 1.3.6.1.4.1.48710.1.4.102 NAME 'jansOciLock'
  SUP ( top )
  STRUCTURAL
  MUST ( objectclass )
  MAY ( inum $ ou $ jansData )
  X-ORIGIN 'Jans - OCI lock objectclass'
  X-SCHEMA-FILE '200-ociLock.ldif' )
""".replace("\n", "").strip()


class LdapLock(BaseLock):
    def __init__(self):
        url = os.environ.get("CN_LDAP_URL", "localhost:1636")
        host = url.split(":", 1)[0]
        use_ssl = as_boolean(os.environ.get("CN_LDAP_USE_SSL", True))
        port = 1636 if use_ssl else 1389
        self._server = Server(host, port=port, use_ssl=use_ssl)

    @cached_property
    def connection(self):
        user = "cn=Directory Manager"

        password_file = os.environ.get("CN_LDAP_PASSWORD_FILE", "/etc/jans/conf/ldap_password")

        password = get_password_from_file(password_file)
        return Connection(self._server, user, password)

    def _prepare_schema(self):
        with self.connection as conn:
            # add new schema if not exist
            conn.modify(
                "cn=schema",
                {"objectClasses": [(MODIFY_ADD, [_LOCK_OBJECTCLASS_SCHEMA])]},
            )
            if conn.result["description"] not in ("success", "attributeOrValueExists"):
                raise RuntimeError(f"Unable to add custom schema for jansOciLock; reason={conn.result['message']}")

            org_name = "Janssen Project"

            # add base entry o=jans
            conn.add(
                "o=jans",
                attributes={
                    "objectClass": ["jansOrganization", "top"],
                    "description": ["Welcome to Janssen!"],
                    "displayName": [org_name],
                    "jansOrgShortName": [org_name],
                    "jansThemeColor": [166309],
                    "jansManagerGrp": ["inum=60B7,ou=groups,o=jans"],
                    "o": ["jans"],
                }
            )
            if conn.result["description"] not in ("success", "entryAlreadyExists"):
                raise RuntimeError(f"Unable to add base entry o=jans; reason={conn.result['message']}")

            # add base entry ou=oci-lock,o=jans
            conn.add(
                "ou=oci-lock,o=jans",
                attributes={
                    "objectClass": ["organizationalUnit", "top"],
                    "ou": ["oci-lock"],
                }
            )
            if conn.result["description"] not in ("success", "entryAlreadyExists"):
                raise RuntimeError(f"Unable to add base entry ou=oci-lock,o=jans; reason={conn.result['message']}")

    def get(self, key: str) -> dict[str, _t.Any]:
        self._prepare_schema()

        with self.connection as conn:
            conn.search(
                search_base=f"inum={key},ou=oci-lock,o=jans",
                search_filter="(objectClass=jansOciLock)",
                search_scope=BASE,
                attributes=["*"],
                size_limit=1,
            )

            if conn.entries:
                entry = conn.entries[0]
                return {"name": entry["inum"].values[0]} | json.loads(entry["jansData"][0])

            # found nothing
            return {}

    def post(self, key: str, owner: str, ttl: float, updated_at: str) -> bool:
        self._prepare_schema()

        with self.connection as conn:
            conn.add(
                f"inum={key},ou=oci-lock,o=jans",
                attributes={
                    "objectClass": ["jansOciLock", "top"],
                    "inum": [key],
                    "ou": ["oci-lock"],
                    "jansData": [json.dumps({"owner": owner, "ttl": ttl, "updated_at": updated_at})],
                },
            )

            match conn.result["description"]:
                case "entryAlreadyExists":
                    return False
                case "success":
                    return True
                case _:
                    raise RuntimeError(f"Unable to add entry; reason={conn.result['message']}")
        return False

    def put(self, key: str, owner: str, ttl: float, updated_at: str) -> bool:
        self._prepare_schema()

        with self.connection as conn:
            conn.modify(
                f"inum={key},ou=oci-lock,o=jans",
                {
                    "jansData": [
                        MODIFY_REPLACE,
                        json.dumps({"owner": owner, "ttl": ttl, "updated_at": updated_at}),
                    ],
                },
            )

            match conn.result["description"]:
                case "noSuchObject":
                    return False
                case "success":
                    return True
                case _:
                    raise RuntimeError(f"Unable to modify entry; reason={conn.result['message']}")
        return False

    def delete(self, key: str) -> bool:
        self._prepare_schema()

        with self.connection as conn:
            conn.delete(f"inum={key},ou=oci-lock,o=jans")

            match conn.result["description"]:
                case "noSuchObject":
                    return False
                case "success":
                    return True
                case _:
                    raise RuntimeError(f"Unable to delete entry; reason={conn.result['message']}")
        return False

    def connected(self) -> bool:
        """Check if connection is established.

        Returns:
            A boolean to indicate connection is established.
        """
        # wait for index being ready to avoid degraded state
        index_name = "del"
        backend = "userRoot"
        dn = f"ds-cfg-attribute={index_name},cn=Index,ds-cfg-backend-id={backend},cn=Backends,cn=config"

        with self.connection as conn:
            conn.search(
                search_base=dn,
                search_filter="(objectClass=*)",
                search_scope=BASE,
                attributes=["1.1"],
                size_limit=1,
            )

            match conn.result["description"]:
                case "success":
                    return True
                case _:
                    raise RuntimeError(f"Unable to check connection; reason={conn.result['message']}")

        # unable to connect
        return False
