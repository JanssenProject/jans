from __future__ import annotations

import logging.config
import typing as _t
from functools import cached_property
from string import Template
from uuid import uuid4

from jans.pycloudlib import get_manager
from jans.pycloudlib.persistence.couchbase import CouchbaseClient
from jans.pycloudlib.persistence.ldap import LdapClient
from jans.pycloudlib.persistence.spanner import SpannerClient
from jans.pycloudlib.persistence.sql import SqlClient
from jans.pycloudlib.persistence.utils import PersistenceMapper
from jans.pycloudlib.utils import generate_base64_contents
from jans.pycloudlib.utils import encode_text
from jans.pycloudlib.utils import get_random_chars

from settings import LOGGING_CONFIG

if _t.TYPE_CHECKING:  # pragma: no cover
    # imported objects for function type hint, completion, etc.
    # these won't be executed in runtime
    from jans.pycloudlib.manager import Manager


logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("jans-saml")

manager = get_manager()


def render_keycloak_storage_api_props(ctx):
    with open("/app/templates/jans-saml/jans-keycloak-storage-api.properties") as f:
        tmpl = Template(f.read())

    with open("/opt/keycloak/providers/jans-keycloak-storage-api.properties", "w") as f:
        f.write(tmpl.safe_substitute(ctx))


def main():
    with manager.lock.create_lock("jans-saml-setup"):
        persistence_setup = PersistenceSetup(manager)
        persistence_setup.import_ldif_files()
        render_keycloak_storage_api_props(persistence_setup.ctx)


class PersistenceSetup:
    def __init__(self, manager: Manager) -> None:
        self.manager = manager

        client_classes = {
            "ldap": LdapClient,
            "couchbase": CouchbaseClient,
            "spanner": SpannerClient,
            "sql": SqlClient,
        }

        # determine persistence type
        mapper = PersistenceMapper()
        self.persistence_type = mapper.mapping["default"]

        # determine persistence client
        client_cls = client_classes.get(self.persistence_type)
        self.client = client_cls(manager)

    @cached_property
    def ctx(self) -> dict[str, _t.Any]:
        hostname = self.manager.config.get("hostname")

        ctx = {
            "hostname": hostname,
            "saml_enabled": "true",
            "idp_root_dir": "/opt/idp/configs",
            "config_generation": "true",
            "ignore_validation": "true",
            "idp_config_id": "keycloak",
            "idp_config_root_dir": "/opt/idp/configs/keycloak",
            "idp_config_enabled": "true",
            "idp_config_temp_meta_dir": "/opt/idp/configs/keycloak/temp_metadata",
            "idp_config_meta_dir": "/opt/idp/configs/keycloak/metadata",
        }

        # pre-populate jans_link_config_base64
        with open("/app/templates/jans-saml/jans-saml-config.json") as f:
            tmpl = Template(f.read())
            ctx["saml_dynamic_conf_base64"] = generate_base64_contents(tmpl.safe_substitute(ctx))

        # scim client for saml
        ctx["saml_scim_client_id"] = self.manager.config.get("saml_scim_client_id")
        if not ctx["saml_scim_client_id"]:
            ctx["saml_scim_client_id"] = f"2100.{uuid4()}"
            self.manager.config.set("saml_scim_client_id", ctx["saml_scim_client_id"])

        ctx["saml_scim_client_pw"] = self.manager.secret.get("saml_scim_client_pw")
        if not ctx["saml_scim_client_pw"]:
            ctx["saml_scim_client_pw"] = get_random_chars()
            self.manager.secret.set("saml_scim_client_pw", ctx["saml_scim_client_pw"])

        ctx["saml_scim_client_encoded_pw"] = self.manager.secret.get("saml_scim_client_encoded_pw")
        if not ctx["saml_scim_client_encoded_pw"]:
            ctx["saml_scim_client_encoded_pw"] = encode_text(
                ctx["saml_scim_client_pw"], self.manager.secret.get("encoded_salt"),
            ).decode()
            self.manager.secret.set("saml_scim_client_encoded_pw", ctx["saml_scim_client_encoded_pw"])

        # scopes for client
        ctx["scim_users_read_scope"] = "inum=1200.2B7428,ou=scopes,o=jans"
        ctx["scim_users_write_scope"] = "inum=1200.0A0198,ou=scopes,o=jans"

        # storage-api properties
        ctx["keycloack_hostname"] = "localhost"  # @FIXME: parametrize

        # finalized ctx
        return ctx

    @cached_property
    def ldif_files(self) -> list[str]:
        return [
            f"/app/templates/jans-saml/{file_}"
            for file_ in ["configuration.ldif", "clients.ldif"]
        ]

    def import_ldif_files(self) -> None:
        for file_ in self.ldif_files:
            logger.info(f"Importing {file_}")
            self.client.create_from_ldif(file_, self.ctx)


if __name__ == "__main__":
    main()
