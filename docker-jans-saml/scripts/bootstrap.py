from __future__ import annotations

import base64
import logging.config
import os
import re
import shutil
import sys
import typing as _t
from zipfile import ZipFile
from functools import cached_property
from pathlib import Path
from string import Template
from uuid import uuid4

from jans.pycloudlib import get_manager
from jans.pycloudlib import wait_for_persistence
from jans.pycloudlib.persistence.hybrid import render_hybrid_properties
from jans.pycloudlib.persistence.sql import SqlClient
from jans.pycloudlib.persistence.sql import render_sql_properties
from jans.pycloudlib.persistence.sql import override_simple_json_property
from jans.pycloudlib.persistence.utils import PersistenceMapper
from jans.pycloudlib.persistence.utils import render_base_properties
from jans.pycloudlib.persistence.utils import render_salt
from jans.pycloudlib.utils import as_boolean
from jans.pycloudlib.utils import generate_base64_contents
from jans.pycloudlib.utils import encode_text
from jans.pycloudlib.utils import get_random_chars
from jans.pycloudlib.utils import exec_cmd

from settings import LOGGING_CONFIG

if _t.TYPE_CHECKING:  # pragma: no cover
    # imported objects for function type hint, completion, etc.
    # these won't be executed in runtime
    from jans.pycloudlib.manager import Manager


logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("jans-saml")

manager = get_manager()

LIB_METADATA_RE = re.compile(r"(?P<name>.*)-(?P<version>\d+.*)(?P<ext>\.jar)")


def render_keycloak_conf():
    hostname = manager.config.get("hostname")

    ctx = {
        "hostname": hostname,
        "kc_hostname": hostname,
        "kc_db_password": os.environ.get("KC_DB_PASSWORD") or manager.secret.get("kc_db_password"),
        "idp_config_http_port": os.environ.get("CN_SAML_HTTP_PORT", "8083"),
        "idp_config_http_host": os.environ.get("CN_SAML_HTTP_HOST", "0.0.0.0"),   # nosec: B104
        "idp_config_data_dir": "/opt/keycloak",
    }

    with open("/app/templates/jans-saml/keycloak.conf") as f:
        tmpl = f.read()

    with open("/opt/keycloak/conf/keycloak.conf", "w") as f:
        f.write(tmpl % ctx)


def main():
    persistence_type = os.environ.get("CN_PERSISTENCE_TYPE", "sql")

    render_salt(manager, "/app/templates/salt", "/etc/jans/conf/salt")
    render_base_properties("/app/templates/jans.properties", "/etc/jans/conf/jans.properties")

    mapper = PersistenceMapper()
    persistence_groups = mapper.groups().keys()

    if persistence_type == "hybrid":
        hybrid_prop = "/etc/jans/conf/jans-hybrid.properties"
        if not os.path.exists(hybrid_prop):
            render_hybrid_properties(hybrid_prop)

    if "sql" in persistence_groups:
        db_dialect = os.environ.get("CN_SQL_DB_DIALECT", "mysql")
        sql_prop = "/etc/jans/conf/jans-sql.properties"
        if not os.path.exists(sql_prop):
            render_sql_properties(manager, f"/app/templates/jans-{db_dialect}.properties", sql_prop)

    wait_for_persistence(manager)
    override_simple_json_property("/etc/jans/conf/jans-sql.properties")

    shutil.copyfile(
        "/app/templates/jans-saml/quarkus.properties",
        "/opt/keycloak/conf/quarkus.properties",
    )

    with manager.create_lock("saml-setup"):
        persistence_setup = PersistenceSetup(manager)
        persistence_setup.import_ldif_files()
        render_keycloak_conf()
        render_keycloak_creds()


class PersistenceSetup:
    def __init__(self, manager: Manager) -> None:
        self.manager = manager

        client_classes = {
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
            "keycloak_hostname": hostname,
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
    # Keycloak UI requires initial admin credentials (username + password) that configured using
    # KEYCLOAK_ADMIN and KEYCLOAK_ADMIN_PASSWORD env vars; note that exporting env vars via Python
    # os.environ wont work because the process wont alter the parent's environment, hence we create
    # credentials file in order to make shell script parse and pass the credentials via export command;
    # for security purpose, it's recommended to remove the credentials file after shell script finished
    # exporting the env vars
    creds_file = os.environ.get("CN_SAML_KC_ADMIN_CREDENTIALS_FILE", "/etc/jans/conf/kc_admin_creds")

    if not os.path.isfile(creds_file):
        with open(creds_file, "w") as f:
            username = manager.config.get("kc_admin_username")
            password = manager.secret.get("kc_admin_password")
            creds_bytes = f"{username}:{password}".encode()
            f.write(base64.b64encode(creds_bytes).decode())


def extract_common_libs(persistence_type):
    dist_file = f"/usr/share/java/{persistence_type}-libs.zip"

    # download if file is missing
    if not os.path.exists(dist_file):
        version = os.environ.get("CN_VERSION")
        download_url = f"https://jenkins.jans.io/maven/io/jans/jans-orm-{persistence_type}-libs/{version}/jans-orm-{persistence_type}-libs-{version}-distribution.zip"
        basename = os.path.basename(download_url)

        logger.info(f"Downloading {basename} as {dist_file}")

        out, err, code = exec_cmd(f"wget -q {download_url} -O {dist_file}")

        if code != 0:
            err = out or err
            logger.error(f"Unable to download {basename}; reason={err.decode()}")
            sys.exit(1)

    # list existing providers libs (these libs should not be overwritten)
    provider_libs = [
        LIB_METADATA_RE.search(path.name).groupdict()["name"]
        for path in Path("/opt/keycloak/providers").glob("*.jar")
    ]

    # extract common libs where it does not exist (basename-based) in providers directory
    with ZipFile(dist_file) as zf:
        extracted_members = [
            member for member in zf.namelist()
            if LIB_METADATA_RE.search(member).groupdict()["name"] not in provider_libs
        ]

        logger.info(f"Extracting {dist_file}")
        zf.extractall("/opt/keycloak/providers/", members=extracted_members)


if __name__ == "__main__":
    main()
