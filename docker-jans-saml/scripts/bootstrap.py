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
from utils import get_kc_db_password

if _t.TYPE_CHECKING:  # pragma: no cover
    # imported objects for function type hint, completion, etc.
    # these won't be executed in runtime
    from jans.pycloudlib.manager import Manager


logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("jans-saml")

manager = get_manager()


def render_keycloak_conf():
    ctx = {
        "hostname": manager.config.get("hostname"),
        "db_password": get_kc_db_password(),
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
            "jans_idp_realm": "jans",
            "jans_idp_grant_type": "PASSWORD",
            "jans_idp_user_name": "jans",
            "idp_config_hostname": hostname,
            "idp_config_http_port": os.environ.get("CN_SAML_HTTP_PORT", "8083"),
        }

        # jans-idp contexts
        ctx["jans_idp_client_id"] = self.manager.config.get("jans_idp_client_id")
        if not ctx["jans_idp_client_id"]:
            ctx["jans_idp_client_id"] = f"jans-{uuid4()}"
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

        # keycloak credentials
        ctx["kc_admin_username"] = self.manager.config.get("kc_admin_username")
        if not ctx["kc_admin_username"]:
            ctx["kc_admin_username"] = "admin"
            self.manager.config.set("kc_admin_username", ctx["kc_admin_username"])

        ctx["kc_admin_password"] = self.manager.secret.get("kc_admin_password")
        if not ctx["kc_admin_password"]:
            ctx["kc_admin_password"] = get_random_chars()
            self.manager.secret.set("kc_admin_password", ctx["kc_admin_password"])

        # pre-populate contexts for clients
        ctx.update(self._get_clients_ctx())

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

    def _get_clients_ctx(self):
        ctx = {}
        encoded_salt = self.manager.secret.get("encoded_salt")

        for id_prefix, ctx_name in [
            # scim client for saml
            (2100, "saml_scim"),
            # KC client for saml
            (2101, "kc_saml_openid"),
            # KC scheduler API client
            (2102, "kc_scheduler_api"),
            # KC master auth client
            (2103, "kc_master_auth"),
        ]:
            client_id = f"{ctx_name}_client_id"
            client_pw = f"{ctx_name}_client_pw"
            client_encoded_pw = f"{ctx_name}_client_encoded_pw"

            ctx[client_id] = self.manager.config.get(client_id)
            if not ctx[client_id]:
                ctx[client_id] = f"{id_prefix}.{uuid4()}"
                self.manager.config.set(client_id, ctx[client_id])

            ctx[client_pw] = self.manager.secret.get(client_pw)
            if not ctx[client_pw]:
                ctx[client_pw] = get_random_chars()
                self.manager.secret.set(client_pw, ctx[client_pw])

            ctx[client_encoded_pw] = self.manager.secret.get(client_encoded_pw)
            if not ctx[client_encoded_pw]:
                ctx[client_encoded_pw] = encode_text(ctx[client_pw], encoded_salt).decode()
                self.manager.secret.set(client_encoded_pw, ctx[client_encoded_pw])
        return ctx


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
