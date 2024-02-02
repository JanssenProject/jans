from __future__ import annotations

import base64
import logging.config
import os
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


def render_keycloak_conf():
    db_password = os.environ.get("KC_DB_PASSWORD", "")

    if not db_password:
        passwd_file = os.environ.get("CN_SAML_KC_DB_PASSWORD_FILE", "/etc/jans/conf/kc_db_password")

        try:
            with open(passwd_file) as f:
                db_password = f.read().strip()
        except FileNotFoundError as exc:
            raise ValueError(f"Unable to get password from {passwd_file}; reason={exc}")

    ctx = {
        "hostname": manager.config.get("hostname"),
        "db_password": db_password,
    }

    with open("/app/templates/jans-saml/keycloak.conf") as f:
        defaults = f.read()

    with open("/app/templates/jans-saml/keycloak.extra.conf") as f:
        extras = f.read()

    with open("/opt/keycloak/conf/keycloak.conf", "w") as f:
        tmpl = "\n".join([defaults, extras])
        f.write(tmpl % ctx)


def main():
    with manager.lock.create_lock("saml-setup"):
        persistence_setup = PersistenceSetup(manager)
        persistence_setup.import_ldif_files()
        render_keycloak_conf()
        render_keycloak_creds()


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
            "keycloack_hostname": hostname,
            "jans_idp_realm": "jans-api",
            "jans_idp_grant_type": "PASSWORD",
            "jans_idp_user_name": "jans-api",
        }

        # jans-idp contexts
        ctx["jans_idp_client_id"] = self.manager.config.get("jans_idp_client_id")
        if not ctx["jans_idp_client_id"]:
            ctx["jans_idp_client_id"] = f"jans-api-{uuid4()}"
            self.manager.config.set("jans_idp_client_id", ctx["jans_idp_client_id"])

        ctx["jans_idp_client_secret"] = self.manager.secret.get("jans_idp_client_secret")
        if not ctx["jans_idp_client_secret"]:
            ctx["jans_idp_client_secret"] = os.urandom(10).hex()
            self.manager.secret.set("jans_idp_client_secret", ctx["jans_idp_client_secret"])

        ctx["jans_idp_user_password"] = self.manager.secret.get("jans_idp_user_password")
        if not ctx["jans_idp_user_password"]:
            ctx["jans_idp_user_password"] = os.urandom(10).hex()
            self.manager.secret.set("jans_idp_user_password", ctx["jans_idp_user_password"])

        # pre-populate saml_dynamic_conf_base64
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

        # keycloak credentials
        ctx["kc_admin_username"] = self.manager.config.get("kc_admin_username")
        if not ctx["kc_admin_username"]:
            ctx["kc_admin_username"] = "admin"
            self.manager.config.set("kc_admin_username", ctx["kc_admin_username"])

        ctx["kc_admin_password"] = self.manager.secret.get("kc_admin_password")
        if not ctx["kc_admin_password"]:
            ctx["kc_admin_password"] = get_random_chars()
            self.manager.secret.set("kc_admin_password", ctx["kc_admin_password"])

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


def render_keycloak_creds():
    creds_file = os.environ.get("CN_SAML_KC_ADMIN_CREDENTIALS_FILE", "/etc/jans/conf/kc_admin_creds")

    if not os.path.isfile(creds_file):
        with open(creds_file, "w") as f:
            username = manager.config.get("kc_admin_username")
            password = manager.secret.get("kc_admin_password")
            creds_bytes = f"{username}:{password}".encode()
            f.write(base64.b64encode(creds_bytes).decode())


if __name__ == "__main__":
    main()
