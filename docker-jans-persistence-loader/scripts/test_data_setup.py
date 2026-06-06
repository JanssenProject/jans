"""Load integration-test data into the persistence backend.

This mirrors the relevant parts of jans-linux-setup's ``test_data_loader`` so the
Janssen integration test-suite can run against an all-in-one (AIO) deployment
without jenkins.jans.io. It is gated behind the ``CN_PERSISTENCE_LOAD_TEST_DATA``
environment variable (default ``false``) and only targets the SQL backend, which
is what the integration tests run on (MySQL/PGSQL).

The test schema templates and data LDIFs are vendored into the persistence-loader
image from ``jans-linux-setup/jans_setup/templates/test`` (see the Dockerfile).

Notes:

* The auth test-client secrets are derived deterministically as
  ``<inum>-<host-label>`` (matching ``crypto64.encode_test_passwords`` in
  jans-linux-setup) so the test-suite can recompute them without a shared secret.
* The config-api test client is handled by ``docker-jans-config-api`` bootstrap
  (which pins it via ``CN_CONFIG_API_TEST_CLIENT_*`` and grants it every scope on
  upgrade), so no config-api test data is loaded here.
* The loader is idempotent: it runs under a lock, skips columns/flags that are
  already present, and pycloudlib swallows duplicate-key inserts on restart.
"""

import json
import logging.config
import os
import re

from sqlalchemy import text
from sqlalchemy.exc import DatabaseError

from jans.pycloudlib.persistence.sql import SqlClient
from jans.pycloudlib.persistence.sql import doc_id_from_dn
from jans.pycloudlib.utils import as_boolean
from jans.pycloudlib.utils import encode_text

from settings import LOGGING_CONFIG
from utils import prepare_template_ctx

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("persistence-loader")

# root of the vendored ``templates/test`` tree (see Dockerfile)
TEST_TEMPLATE_BASE = "/app/templates/test"

# Fixed inums for the auth test clients (mirrors jans-linux-setup config.py).
TEST_AUTH_CLIENT_INUMS = {
    "jans_auth_test_client_2_inum": "AB77-1A2B",
    "jans_auth_test_client_3_inum": "3E20",
    "jans_auth_test_client_4_inum": "FF81-2D39",
}

# LDIF schema templates declaring the custom attributes used by the test data;
# parsed at runtime to add the matching SQL columns.
TEST_SCHEMA_FILES = (
    f"{TEST_TEMPLATE_BASE}/jans-auth/schema/102-jans-auth_test.ldif",
    f"{TEST_TEMPLATE_BASE}/scim-client/schema/103-scim_test.ldif",
)

# Test data LDIF templates, imported in dependency order (clients/devices before
# the users that reference them).
TEST_DATA_FILES = (
    f"{TEST_TEMPLATE_BASE}/jans-auth/data/jans-auth-test-data.ldif",
    f"{TEST_TEMPLATE_BASE}/scim-client/data/scim-test-data.ldif",
    f"{TEST_TEMPLATE_BASE}/jans-auth/data/jans-auth-test-data-user.ldif",
    f"{TEST_TEMPLATE_BASE}/scim-client/data/scim-test-data-user.ldif",
    f"{TEST_TEMPLATE_BASE}/jans-fido2/data/fido2-device-registration-test-data.ldif",
)

# Custom scripts enabled for the test-suite (mirrors test_data_loader).
TEST_SCRIPT_INUMS = ("2DAF-F995", "2DAF-F996", "4BBE-C6A8", "A51E-76DA", "0300-BA90")

# Scopes promoted to "default" for the test-suite.
TEST_DEFAULT_SCOPE_INUMS = ("C4F6", "7D91")

JANS_AUTH_CONFIG_DN = "ou=jans-auth,ou=configuration,o=jans"

# jans-auth dynamic-config overrides required by the test-suite (mirrors
# test_data_loader.load_test_data).
AUTH_DYNAMIC_CONF_DELTA = {
    "dynamicRegistrationCustomObjectClass": "jansClntCustomAttributes",
    "dynamicRegistrationCustomAttributes": ["jansTrustedClnt", "myCustomAttr1", "myCustomAttr2", "jansInclClaimsInIdTkn"],
    "dynamicRegistrationExpirationTime": 86400,
    "grantTypesAndResponseTypesAutofixEnabled": True,
    "grantTypesSupportedByDynamicRegistration": ["authorization_code", "implicit", "password", "client_credentials", "refresh_token", "urn:ietf:params:oauth:grant-type:uma-ticket", "urn:openid:params:grant-type:ciba", "urn:ietf:params:oauth:grant-type:device_code", "urn:ietf:params:oauth:grant-type:token-exchange", "urn:ietf:params:oauth:grant-type:jwt-bearer"],
    "legacyIdTokenClaims": True,
    "authenticationFiltersEnabled": True,
    "clientAuthenticationFiltersEnabled": True,
    "keyRegenerationEnabled": True,
    "openidScopeBackwardCompatibility": False,
    "forceOfflineAccessScopeToEnableRefreshToken": False,
    "dynamicRegistrationPasswordGrantTypeEnabled": True,
    "cibaEnabled": True,
    "backchannelTokenDeliveryModesSupported": ["poll", "ping", "push"],
    "backchannelAuthenticationRequestSigningAlgValuesSupported": ["RS256", "RS384", "RS512", "ES256", "ES384", "ES512", "PS256", "PS384", "PS512"],
    "backchannelClientId": "123-123-123",
    "backchannelUserCodeParameterSupported": True,
    "tokenEndpointAuthSigningAlgValuesSupported": ["HS256", "HS384", "HS512", "RS256", "RS384", "RS512", "ES256", "ES384", "ES512", "PS256", "PS384", "PS512"],
    "userInfoSigningAlgValuesSupported": ["none", "HS256", "HS384", "HS512", "RS256", "RS384", "RS512", "ES256", "ES384", "ES512", "PS256", "PS384", "PS512"],
    "consentGatheringScriptBackwardCompatibility": False,
    "claimsParameterSupported": True,
    "grantTypesSupported": ["urn:openid:params:grant-type:ciba", "authorization_code", "urn:ietf:params:oauth:grant-type:uma-ticket", "urn:ietf:params:oauth:grant-type:device_code", "client_credentials", "implicit", "refresh_token", "password", "urn:ietf:params:oauth:grant-type:token-exchange", "urn:ietf:params:oauth:grant-type:jwt-bearer"],
    "idTokenSigningAlgValuesSupported": ["none", "HS256", "HS384", "HS512", "RS256", "RS384", "RS512", "ES256", "ES384", "ES512", "PS256", "PS384", "PS512"],
    "accessTokenSigningAlgValuesSupported": ["none", "HS256", "HS384", "HS512", "RS256", "RS384", "RS512", "ES256", "ES384", "ES512", "PS256", "PS384", "PS512"],
    "requestObjectSigningAlgValuesSupported": ["none", "HS256", "HS384", "HS512", "RS256", "RS384", "RS512", "ES256", "ES384", "ES512", "PS256", "PS384", "PS512"],
    "softwareStatementValidationClaimName": "jwks_uri",
    "softwareStatementValidationType": "jwks_uri",
    "umaGrantAccessIfNoPolicies": True,
    "rejectJwtWithNoneAlg": False,
    "removeRefreshTokensForClientOnLogout": True,
    "fapiCompatibility": False,
    "forceIdTokenHintPresence": False,
    "introspectionScriptBackwardCompatibility": False,
    "allowSpontaneousScopes": True,
    "accessEvaluationAllowBasicClientAuthorization": True,
    "spontaneousScopeLifetime": 0,
    "tokenEndpointAuthMethodsSupported": ["client_secret_basic", "client_secret_post", "client_secret_jwt", "private_key_jwt", "tls_client_auth", "self_signed_tls_client_auth", "none"],
    "sessionIdRequestParameterEnabled": True,
    "skipRefreshTokenDuringRefreshing": False,
    "featureFlags": ["unknown", "health_check", "userinfo", "clientinfo", "id_generation", "registration", "introspection", "revoke_token", "global_token_revocation", "end_session", "status_session", "jans_configuration", "ciba", "uma", "u2f", "device_authz", "stat", "par", "ssa", "status_list", "logout_status_jwt", "access_evaluation"],
    "loggingLevel": "TRACE",
}

_ATTR_NAME_RE = re.compile(r"NAME\s+'([^']+)'")
_ATTR_SYNTAX_RE = re.compile(r"SYNTAX\s+([\d.]+)")
_ATTR_ADD_TABLE_RE = re.compile(r"X-RDBM-ADD\s+'([^']+)'")


def _parse_test_schema(path):
    """Yield ``(attr_name, syntax_oid, table)`` tuples from an LDIF schema file."""
    if not os.path.isfile(path):
        logger.warning("Test schema file %s not found; skipping", path)
        return

    with open(path) as f:
        raw = f.read()

    # join LDIF line-continuations (continuation lines begin with whitespace)
    joined = re.sub(r"\n[ \t]+", " ", raw)

    for line in joined.splitlines():
        line = line.strip()
        if not line.startswith("attributeTypes:"):
            continue

        name = _ATTR_NAME_RE.search(line)
        syntax = _ATTR_SYNTAX_RE.search(line)
        table = _ATTR_ADD_TABLE_RE.search(line)
        if name and syntax and table:
            yield name.group(1), syntax.group(1), table.group(1)


class TestDataLoader:
    def __init__(self, manager):
        self.manager = manager
        self.client = SqlClient(manager)

    def _resolve_data_type(self, syntax):
        """Resolve a SQL column type from an LDAP syntax OID.

        Mirrors ``SQLBackend.get_data_type`` (syntax branch) so the custom test
        columns get the correct type without the attribute being present in the
        loaded schema.
        """
        syntax_def = self.client.sql_data_types_mapping[syntax]
        type_ = syntax_def.get(self.client.dialect) or syntax_def["mysql"]

        if type_["type"] != "VARCHAR":
            return type_["type"]

        size = type_["size"]
        if size <= 127:
            return f"VARCHAR({size})"
        if size <= 255:
            return "TINYTEXT" if self.client.dialect == "mysql" else "TEXT"
        return "TEXT"

    def add_custom_columns(self):
        """Add the test-only custom attribute columns (idempotent)."""
        existing = self.client.get_table_mapping()
        added = False

        for schema_file in TEST_SCHEMA_FILES:
            for attr, syntax, table in _parse_test_schema(schema_file):
                if attr in existing.get(table, {}):
                    continue

                try:
                    data_type = self._resolve_data_type(syntax)
                except KeyError:
                    logger.warning("Unknown syntax %s for %s.%s; skipping", syntax, table, attr)
                    continue

                query = (
                    f"ALTER TABLE {self.client.quoted_id(table)} "
                    f"ADD {self.client.quoted_id(attr)} {data_type}"
                )
                logger.info("Adding test column %s.%s (%s)", table, attr, data_type)
                try:
                    with self.client.engine.connect() as conn:
                        with conn.begin():
                            conn.execute(text(query))
                    added = True
                except DatabaseError as exc:
                    # most likely the column already exists from a previous run
                    logger.warning("Could not add column %s.%s: %s", table, attr, exc)

        if added:
            # force metadata reload so subsequent inserts see the new columns
            self.client._metadata = None

    def build_ctx(self):
        ctx = prepare_template_ctx(self.manager)
        host_label = ctx["hostname"].split(".")[0]
        salt = self.manager.secret.get("encoded_salt")

        for inum_var, inum in TEST_AUTH_CLIENT_INUMS.items():
            prefix = inum_var[:-len("_inum")]
            pw = f"{inum}-{host_label}"
            ctx[inum_var] = inum
            ctx[f"{prefix}_pw"] = pw
            ctx[f"{prefix}_encoded_pw"] = encode_text(pw, salt).decode()

        return ctx

    def import_test_data(self, ctx):
        for path in TEST_DATA_FILES:
            if not os.path.isfile(path):
                logger.warning("Test data file %s not found; skipping", path)
                continue
            logger.info("Importing test data %s", path)
            self.client.create_from_ldif(path, ctx)

    def add_scim_password_grant(self):
        """Add the ``password`` grant to the SCIM test client."""
        scim_client_id = self.manager.config.get("scim_client_id")
        if not scim_client_id:
            return

        doc_id = doc_id_from_dn(f"inum={scim_client_id},ou=clients,o=jans")
        row = self.client.get("jansClnt", doc_id, ["jansGrantTyp"])
        if not row:
            return

        simple = self.client.use_simple_json
        grants = row["jansGrantTyp"] if simple else row["jansGrantTyp"]["v"]
        if not isinstance(grants, list):
            grants = [grants]

        if "password" in grants:
            return

        grants.append("password")
        value = grants if simple else {"v": grants}
        self.client.update("jansClnt", doc_id, {"jansGrantTyp": value})

    def update_auth_dynamic_conf(self):
        """Apply the test-suite jans-auth dynamic-config overrides."""
        doc_id = doc_id_from_dn(JANS_AUTH_CONFIG_DN)
        row = self.client.get("jansAppConf", doc_id, ["jansConfDyn", "jansRevision"])
        if not row:
            return

        conf = json.loads(row["jansConfDyn"])
        conf.update(AUTH_DYNAMIC_CONF_DELTA)
        self.client.update("jansAppConf", doc_id, {
            "jansConfDyn": json.dumps(conf),
            "jansRevision": (row.get("jansRevision") or 0) + 1,
        })

    def enable_test_scripts(self):
        for inum in TEST_SCRIPT_INUMS:
            doc_id = doc_id_from_dn(f"inum={inum},ou=scripts,o=jans")
            row = self.client.get("jansCustomScript", doc_id, ["jansEnabled", "jansRevision"])
            if not row or as_boolean(row.get("jansEnabled")):
                continue
            self.client.update("jansCustomScript", doc_id, {
                "jansEnabled": True,
                "jansRevision": (row.get("jansRevision") or 0) + 1,
            })

    def set_default_scopes(self):
        for inum in TEST_DEFAULT_SCOPE_INUMS:
            doc_id = doc_id_from_dn(f"inum={inum},ou=scopes,o=jans")
            row = self.client.get("jansScope", doc_id, ["jansDefScope"])
            if not row or as_boolean(row.get("jansDefScope")):
                continue
            self.client.update("jansScope", doc_id, {"jansDefScope": True})

    def load(self):
        logger.info("Loading integration-test data")
        self.add_custom_columns()
        ctx = self.build_ctx()
        self.import_test_data(ctx)
        self.add_scim_password_grant()
        self.update_auth_dynamic_conf()
        self.enable_test_scripts()
        self.set_default_scopes()
        logger.info("Integration-test data loaded")
