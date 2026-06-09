#!/usr/bin/env python3
"""Render per-run integration-test profiles (``profiles/<fqdn>/``) for each module.

Invoked by ``.github/workflows/test-integration.yml`` once the all-in-one (AIO)
server and its database are up. Maven selects a profile via ``-Dcfg=<fqdn>`` and
filters ``profiles/<fqdn>/*.properties`` into the test resources, so this script
materialises those directories from the canonical templates under
``jans-linux-setup/jans_setup/templates/test``.

This is **pure string templating** (Python ``%``-formatting). Every crypto/dynamic
value (the persistence salt, the SCIM/config-api client credentials, the encoded
DB password) is extracted from the running AIO by the workflow and handed to this
script through environment variables, so no jans crypto code is needed here.

The fixed test-client inums and the ``<inum>-<host-label>`` secret derivation MUST
stay in sync with ``docker-jans-persistence-loader/scripts/test_data_setup.py``.
"""

import os
import shutil
import sys
from pathlib import Path

# .github/workflows/scripts/render_test_profiles.py -> repo root
REPO = Path(__file__).resolve().parents[3]
TEST_TEMPLATES = REPO / "jans-linux-setup" / "jans_setup" / "templates" / "test"

# Fixed inums for the auth/fido2 test clients (mirror test_data_setup.py /
# jans-linux-setup config.py). Their plaintext secret is "<inum>-<host-label>".
FIXED_CLIENT_INUMS = {
    "jans_auth_test_client_2_inum": "AB77-1A2B",
    "jans_auth_test_client_4_inum": "FF81-2D39",
    "jans_fido2_test_client_2_inum": "FF81-2D39",
}


def _env(name, required=True, default=None):
    value = os.environ.get(name, default)
    if required and not value:
        sys.exit(f"render_test_profiles: missing required env var {name}")
    return value


def _read_config_api_scopes():
    """The full config-api scope list is large + static; read it from the default profile."""
    path = REPO / "jans-config-api" / "profiles" / "default" / "config-api-test.properties"
    for line in path.read_text().splitlines():
        if line.startswith("test.scopes="):
            return line.split("=", 1)[1]
    sys.exit("render_test_profiles: could not find test.scopes in the default config-api profile")


def build_ctx():
    fqdn = _env("JANS_FQDN")
    host_label = fqdn.split(".")[0]

    ctx = {
        "hostname": fqdn,
        "encode_salt": _env("ENCODE_SALT"),
        "persistence_type": "sql",
        "server_time_zone": os.environ.get("SERVER_TIME_ZONE", "UTC"),
        # dynamic AIO values (extracted by the workflow)
        "scim_client_id": _env("SCIM_CLIENT_ID"),
        "scim_client_pw": _env("SCIM_CLIENT_PW"),
        "jca_client_id": _env("JCA_CLIENT_ID"),
        "jca_client_encoded_pw": _env("JCA_CLIENT_ENCODED_PW"),
        # config-api test client (pinned via CN_CONFIG_API_TEST_CLIENT_* on the AIO)
        "jca_test_client_id": _env("JCA_TEST_CLIENT_ID"),
        "jca_test_client_pw": _env("JCA_TEST_CLIENT_SECRET"),
        # RDBM connection (for the server-side persistence tests)
        "rdbm_name_str": _env("RDBM_NAME_STR"),       # mysql | postgresql
        "rdbm_db": _env("RDBM_DB"),
        "rdbm_schema_name": _env("RDBM_SCHEMA_NAME"),
        "rdbm_port": _env("RDBM_PORT"),
        "rdbm_user": _env("RDBM_USER"),
        "rdbm_password_enc": _env("RDBM_PASSWORD_ENC"),
        "config_api_scopes_list": _read_config_api_scopes(),
    }

    for inum_var, inum in FIXED_CLIENT_INUMS.items():
        prefix = inum_var[:-len("_inum")]
        ctx[inum_var] = inum
        ctx[f"{prefix}_pw"] = f"{inum}-{host_label}"

    return ctx


def _write(path: Path, content: str):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content)
    print(f"rendered {path.relative_to(REPO)}")


def render():
    ctx = build_ctx()
    fqdn = ctx["hostname"]

    # (module profile root, destination filename, template source)
    templated = [
        ("jans-auth-server/client/profiles", "config-jans-auth-test-data.properties",
         TEST_TEMPLATES / "jans-auth" / "client" / "config-jans-auth-test-data.properties"),
        ("jans-auth-server/server/profiles", "config-build.properties",
         TEST_TEMPLATES / "jans-auth" / "server" / "config-build.properties"),
        ("jans-auth-server/server/profiles", "config-jans-auth.properties",
         TEST_TEMPLATES / "jans-auth" / "server" / "config-jans-auth.properties"),
        ("jans-auth-server/server/profiles", "config-jans-auth-test-data.properties",
         TEST_TEMPLATES / "jans-auth" / "server" / "config-jans-auth-test-data.properties"),
        ("jans-scim/client/profiles", "config-scim-test.properties",
         TEST_TEMPLATES / "scim-client" / "client" / "config-scim-test.properties"),
        ("jans-config-api/profiles", "config-api-test.properties",
         TEST_TEMPLATES / "jans-config-api" / "client" / "config-api-test.properties"),
    ]
    for prof_root, dest, template in templated:
        _write(REPO / prof_root / fqdn / dest, template.read_text() % ctx)

    # config-jans-auth-test.properties (SQL variant): the server-side tests connect
    # directly to persistence, so build the base + the rendered SQL-connection block.
    sql_block = (TEST_TEMPLATES / "jans-auth" / "server"
                 / "config-jans-auth-test-sql.properties.nrnd").read_text() % ctx
    base = (
        "server.name=%(hostname)s\n"
        "config.oxauth.issuer=https://%(hostname)s\n"
        "config.oxauth.contextPath=https://%(hostname)s\n"
        "config.oxauth.salt=%(encode_salt)s\n"
        "config.persistence.type=sql\n\n"
    ) % ctx
    generic = (
        "\nconfig.generic.configurationEntryDN=ou=jans_test,ou=configuration,o=jans\n"
        "config.generic.createLdapConfigurationEntryIfNotExist=true\n"
        "config.generic.certsDir=conf\n"
    )
    _write(REPO / "jans-auth-server/server/profiles" / fqdn / "config-jans-auth-test.properties",
           base + sql_block + generic)

    # config-api build properties (static)
    _write(REPO / "jans-config-api/profiles" / fqdn / "config-build.properties",
           "log4j.default.log.level=INFO\n")

    # fido2 client only needs the server name (its pom filters config-fido2-test.properties)
    _write(REPO / "jans-fido2/client/profiles" / fqdn / "config-fido2-test.properties",
           f"test.server.name={fqdn}\n")

    # agama-engine: render so the jans-auth-server reactor builds under -Dcfg=<fqdn>; the flow
    # tests also need the sample flows + an agama client deployed to the AS to pass (pending).
    _write(REPO / "jans-auth-server/agama/engine/profiles" / fqdn / "config-agama-test.properties",
           f"server=https://{fqdn}\nclientId={FIXED_CLIENT_INUMS['jans_auth_test_client_2_inum']}\n")

    # client keystores are copied verbatim from the committed default profiles
    for prof_root in ("jans-auth-server/client/profiles", "jans-auth-server/server/profiles"):
        src = REPO / prof_root / "default" / "client_keystore.p12"
        if src.is_file():
            dst = REPO / prof_root / fqdn / "client_keystore.p12"
            dst.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(src, dst)
            print(f"copied {src.relative_to(REPO)} -> {dst.relative_to(REPO)}")


if __name__ == "__main__":
    render()
