import binascii
import contextlib
import json
import logging
import re
from base64 import b64decode
from contextlib import suppress

import pem
from fqdn import FQDN
from marshmallow import EXCLUDE
from marshmallow import post_load
from marshmallow import Schema
from marshmallow import validates
from marshmallow import ValidationError
from marshmallow.fields import Email
from marshmallow.fields import Integer
from marshmallow.fields import Nested
from marshmallow.fields import String
from marshmallow.validate import Length
from marshmallow.validate import OneOf
from marshmallow.validate import Predicate
from marshmallow.validate import Range
from sprig_aes import sprig_decrypt_aes

logger = logging.getLogger(__name__)

PASSWD_RGX = re.compile(
    r"^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*\W)[a-zA-Z0-9\S]{6,}$"
)


DEFAULT_SCOPES = (
    "auth",
    "config-api",
)

OPTIONAL_SCOPES = (
    "redis",
    "sql",
)

AUTH_SIG_KEYS = (
    "RS256",
    "RS384",
    "RS512",
    "ES256",
    "ES384",
    "ES512",
    "PS256",
    "PS384",
    "PS512",
)

AUTH_ENC_KEYS = (
    "RSA1_5",
    "RSA-OAEP",
    "ECDH-ES",
)


class CertKey(String):
    def _deserialize(self, value, attr, obj, **kwargs):
        super()._deserialize(value, attr, obj, **kwargs)

        # try:
        values = pem.parse(value)
        # except AttributeError:
        #     values = []

        if not values:
            # try parsing base64-decoded
            try:
                value = b64decode(value).decode()
            except (binascii.Error, UnicodeDecodeError) as exc:
                raise ValidationError("Invalid certificate or private key") from exc
            values = pem.parse(value)

        try:
            certkey = str(values[0])
        except IndexError:
            certkey = ""
        return certkey


class SecretSchema(Schema):
    class Meta:
        unknown = EXCLUDE

    admin_password = String(
        required=True,
        metadata={
            "description": "Password for admin user",
        }
    )

    # previously sql_pw
    sql_password = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "Password for SQL (RDBMS) user",
        },
    )

    encoded_salt = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "Salt for encoding/decoding sensitive secret",
            "example": "hR8kBUtTxB25pDPCSHVRktAz",
        },
    )

    google_credentials = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "String contains Google application credentials",
            "example": "{\n  \"type\": \"service_account\",\n  \"project_id\": \"testing-project\"\n}\n",
        },
    )

    aws_credentials = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "String contains AWS shared credentials",
            "example": "[default]\naws_access_key_id = FAKE_ACCESS_KEY_ID\naws_secret_access_key = FAKE_SECRET_ACCESS_KEY\n",
        },
    )

    aws_config = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "String contains AWS config",
            "example": "[default]\nregion = us-west-1\n",
        },
    )

    aws_replica_regions = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "String contains AWS replica regions config",
            "example": "[{\"Region\": \"us-west-1\"}, {\"Region\": \"us-west-2\"}]\n",
        },
    )

    vault_role_id = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "Vault RoleID",
        },
    )

    vault_secret_id = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "Vault SecretID",
        },
    )

    kc_db_password = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "Password for Keycloak RDBMS user",
        },
    )

    admin_ui_client_encoded_pw = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "Encoded password for admin-ui client",
            "x-encoding": "3DES",
        },
    )

    admin_ui_client_pw = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "Password for admin-ui client",
        },
    )

    auth_jks_base64 = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "Private keys (keystore) of jans-auth",
        },
    )

    auth_openid_jks_pass = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "Password of jans-auth keystore",
        },
    )

    auth_openid_key_base64 = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "Public keys (JWKS) of jans-auth",
        },
    )

    casa_client_encoded_pw = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "Encoded password for jans-casa client",
            "x-encoding": "3DES",
        },
    )

    casa_client_pw = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "Password for jans-casa client",
        },
    )

    encoded_admin_password = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "Encoded password of admin",
            "x-encoding": "ldap_encode",
        },
    )

    jans_idp_client_secret = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "Client secret of jans-idp app",
        },
    )

    jans_idp_user_password = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "User password for jans-idp",
        },
    )

    jca_client_encoded_pw = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "Encoded password for jans-config-api client",
            "x-encoding": "3DES",
        },
    )

    jca_client_pw = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "Password for jans-config-api client",
        },
    )

    kc_admin_password = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "Admin password of Keycloak",
        },
    )

    kc_master_auth_client_encoded_pw = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "Client encoded secret of Keycloak master auth app",
            "x-encoding": "3DES",
        },
    )

    kc_master_auth_client_pw = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "Client secret of Keycloak master auth app",
        },
    )

    kc_saml_openid_client_encoded_pw = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "Client encoded secret of Keycloak SAML app",
            "x-encoding": "3DES",
        },
    )

    kc_saml_openid_client_pw = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "Client secret of Keycloak SAML app",
        },
    )

    kc_scheduler_api_client_encoded_pw = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "Client encoded secret of Keycloak scheduler API app",
            "x-encoding": "3DES",
        },
    )

    kc_scheduler_api_client_pw = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "Client secret of Keycloak scheduler API app",
        },
    )

    otp_configuration = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "OTP configuration string",
        },
    )

    pairwiseCalculationKey = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "Pairwise calculation key",
        },
    )

    pairwiseCalculationSalt = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "Pairwise calculation salt",
        },
    )

    scim_client_encoded_pw = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "Encoded password for jans-scim client",
            "x-encoding": "3DES",
        },
    )

    scim_client_pw = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "Password for jans-scim client",
        },
    )

    smtp_jks_base64 = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "Private keys (keystore) of SMTP",
        },
    )

    smtp_jks_pass = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "Password of SMTP keystore",
        },
    )

    smtp_jks_pass_enc = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "Encoded password of SMTP keystore",
        },
    )

    ssl_ca_cert = CertKey(
        load_default="",
        dump_default="",
        metadata={
            "description": "SSL certificate for CA",
        },
    )

    ssl_ca_key = CertKey(
        load_default="",
        dump_default="",
        metadata={
            "description": "SSL key for CA",
        },
    )

    ssl_cert = CertKey(
        load_default="",
        dump_default="",
        metadata={
            "description": "SSL certificate for the FQDN",
        },
    )

    ssl_csr = CertKey(
        load_default="",
        dump_default="",
        metadata={
            "description": "SSL certificate signing request for the FQDN",
        },
    )

    ssl_key = CertKey(
        load_default="",
        dump_default="",
        metadata={
            "description": "SSL key for the FQDN",
        },
    )

    super_gluu_creds = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "SuperGluu credentials string",
        },
    )

    test_client_encoded_pw = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "Encoded password for test client",
            "x-encoding": "3DES",
        },
    )

    test_client_pw = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "Password for test client",
        },
    )

    token_server_admin_ui_client_encoded_pw = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "Encoded password for token server client",
            "x-encoding": "3DES",
        },
    )

    token_server_admin_ui_client_pw = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "Password for token server client",
        },
    )

    tui_client_encoded_pw = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "Encoded password for TUI client",
            "x-encoding": "3DES",
        },
    )

    tui_client_pw = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "Password for TUI client",
        },
    )

    redis_password = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "Password for Redis user",
        },
    )

    @post_load
    def transform_data(self, in_data, **kwargs):
        # list of attrs that maybe base64 string and need to be decoded
        for attr in [
            "google_credentials",
            "aws_config",
            "aws_credentials",
            "aws_replica_regions",
        ]:
            with contextlib.suppress(UnicodeDecodeError, binascii.Error):
                in_data[attr] = b64decode(in_data.get(attr, "")).decode()
        return {k: v for k, v in in_data.items() if v}

    @validates("encoded_salt")
    def validate_salt(self, value):
        if value and len(value) != 24:
            raise ValidationError("Length must be 24")

        if value and not value.isalnum():
            raise ValidationError("Only alphanumeric characters are allowed")

    @validates("admin_password")
    def validate_password(self, value, **kwargs):
        if not PASSWD_RGX.search(value):
            raise ValidationError(
                "Must be at least 6 characters and include "
                "one uppercase letter, one lowercase letter, one digit, "
                "and one special character."
            )


class ConfigmapSchema(Schema):
    class Meta:
        unknown = EXCLUDE

    city = String(
        required=True,
        metadata={
            "description": "Locality name (.e.g city)",
            "example": "Austin",
        },
    )

    country_code = String(
        validate=[
            Length(2, 2),
            Predicate(
                "isupper",
                error="Non-uppercased characters aren't allowed",
            ),
        ],
        required=True,
        metadata={
            "description": "Country name (2 letter code)",
            "example": "US",
        },
    )

    admin_email = Email(
        required=True,
        metadata={
            "description": "Email address",
            "example": "support@jans.io",
        },
    )

    hostname = String(
        required=True,
        metadata={
            "description": "Fully qualified domain name (FQDN)",
            "example": "demoexample.jans.io",
        },
    )

    orgName = String(
        required=True,
        metadata={
            "description": "Organization name",
            "example": "Janssen",
        },
    )

    state = String(
        required=True,
        metadata={
            "description": "State or Province Name",
            "example": "TX",
        },
    )

    optional_scopes = String(
        load_default="[]",
        dump_default="[]",
        metadata={
            "description": "List of optional scopes of components as string",
            "example": json.dumps(OPTIONAL_SCOPES),
        },
    )

    auth_sig_keys = String(
        load_default=" ".join(AUTH_SIG_KEYS),
        dump_default=" ".join(AUTH_SIG_KEYS),
        metadata={
            "description": "Signature keys to generate",
            "example": " ".join(AUTH_SIG_KEYS),
        },
    )

    auth_enc_keys = String(
        load_default=" ".join(AUTH_ENC_KEYS),
        dump_default=" ".join(AUTH_ENC_KEYS),
        metadata={
            "description": "Encryption keys to generate",
            "example": " ".join(AUTH_ENC_KEYS),
        },
    )

    init_keys_exp = Integer(
        validate=[
            Range(1),
        ],
        load_default=48,
        dump_default=48,
        strict=True,
        metadata={
            "description": "Initial expiration time (in hours) for generated keys",
            "example": 24,
        },
    )

    admin_inum = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "Inum for admin user",
            "example": "631e2b84-1d3d-4f28-9a9a-026a25febf44",
        },
    )

    admin_ui_client_id = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "Client ID of admin-ui app",
            "example": "631e2b84-1d3d-4f28-9a9a-026a25febf44",
        },
    )

    casa_client_id = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "Client ID of jans-casa app",
            "example": "1902.66bc89a1-075f-4a18-9349-a2908c1040e6",
        },
    )

    jans_idp_client_id = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "Client ID of jans-idp app",
            "example": "jans-f13013e3-e4a7-4709-8b50-df459f489cd3",
        },
    )

    jca_client_id = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "Client ID of jans-config-api app",
            "example": "1800.ca41fad2-6ab6-46b1-b4a9-3387992a8cb0",
        },
    )

    scim_client_id = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "Client ID of jans-scim app",
            "example": "1201.dd7e7733-b548-45ee-aed1-74e7b4065801",
        },
    )

    tui_client_id = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "Client ID of jans-tui app",
            "example": "2000.4a67fad3-24cd-4d56-b5a3-7cfb2e9fbb05",
        },
    )

    test_client_id = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "Client ID of test app",
            "example": "174143d2-f7f6-4bda-baa0-a6a8fd01b77a",
        },
    )

    kc_master_auth_client_id = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "Client ID of Keycloak master auth app",
            "example": "2103.22abf39d-f78f-4fb0-871e-dcb80bc1e43c",
        },
    )

    kc_saml_openid_client_id = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "Client ID of Keycloak SAML OpenID app",
            "example": "2101.70394974-82ec-481e-9493-e96d3cf8072f",
        },
    )

    kc_scheduler_api_client_id = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "Client ID of Keycloak scheduler API app",
            "example": "2102.d424af33-2069-4803-8426-4787af5fd933",
        },
    )

    token_server_admin_ui_client_id = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "Client ID of token server app",
            "example": "631e2b84-1d3d-4f28-9a9a-026a25febf44",
        },
    )

    auth_key_rotated_at = String(
        load_default="",
        dump_default="",
        metadata={
            "description": "Timestamp of last auth keys regeneration",
            "example": "631e2b84-1d3d-4f28-9a9a-026a25febf44",
        },
    )

    auth_legacyIdTokenClaims = String(
        validate=[
            OneOf(["false", "true"]),
        ],
        load_default="false",
        dump_default="false",
        metadata={
            "description": "Enable legacy ID token claim",
            "example": "false",
        },
    )

    auth_openidScopeBackwardCompatibility = String(
        validate=[
            OneOf(["false", "true"]),
        ],
        load_default="false",
        dump_default="false",
        metadata={
            "description": "Enable backward-compat OpenID scope",
            "example": "false",
        },
    )

    # @TODO: change to HARDCODED value instead
    auth_openid_jks_fn = String(
        load_default="/etc/certs/auth-keys.jks",
        dump_default="/etc/certs/auth-keys.jks",
        metadata={
            "description": "Path to keystore file contains private keys for jans-auth",
        },
    )

    # @TODO: change to HARDCODED value instead
    auth_openid_jwks_fn = String(
        load_default="/etc/certs/auth-keys.json",
        dump_default="/etc/certs/auth-keys.json",
        metadata={
            "description": "Path to JSON file contains public keys for jans-auth",
        },
    )

    # @TODO: change to HARDCODED value instead
    default_openid_jks_dn_name = String(
        load_default="CN=Janssen Auth CA Certificates",
        dump_default="CN=Janssen Auth CA Certificates",
        metadata={
            "description": "CommonName for jans-auth CA certificate",
        },
    )

    kc_admin_username = String(
        load_default="admin",
        dump_default="admin",
        metadata={
            "description": "Admin username of Keycloak",
            "example": "admin",
        },
    )

    smtp_alias = String(
        load_default="smtp_sig_ec256",
        dump_default="smtp_sig_ec256",
        metadata={
            "description": "Alias for SMTP entry in truststore",
            "example": "smtp_sig_ec256",
        },
    )

    smtp_signing_alg = String(
        load_default="SHA256withECDSA",
        dump_default="SHA256withECDSA",
        metadata={
            "description": "SMTP signing algorithm",
            "example": "SHA256withECDSA",
        },
    )

    @validates("hostname")
    def validate_fqdn(self, value):
        if not FQDN(value).is_valid:
            raise ValidationError("Invalid FQDN format.")

    @post_load
    def transform_data(self, in_data, **kwargs):
        in_data["auth_sig_keys"] = transform_auth_keys(in_data["auth_sig_keys"], AUTH_SIG_KEYS)
        in_data["auth_enc_keys"] = transform_auth_keys(in_data["auth_enc_keys"], AUTH_ENC_KEYS)
        return {k: v for k, v in in_data.items() if v}

    @validates("optional_scopes")
    def validate_optional_scopes(self, value):
        try:
            scopes = json.loads(value)
        except json.decoder.JSONDecodeError:
            raise ValidationError("Invalid list type of optional scopes")

        if not isinstance(scopes, list):
            raise ValidationError("Unable to resolve optional scopes as list type")

        # if contain unrecognized scopes, raise error
        invalid_scopes = [
            scope for scope in scopes
            if scope not in OPTIONAL_SCOPES
        ]
        if invalid_scopes:
            raise ValidationError(
                f"Invalid optional scopes {','.join(invalid_scopes)}. "
                f"Choose one or more optional scopes from the following list: {','.join(OPTIONAL_SCOPES)}"
            )


def transform_auth_keys(value, default_keys):
    keys = []

    for k in value.split():
        k = k.strip()
        if k not in default_keys:
            continue
        keys.append(k)

    # if empty, fallback to default
    keys = keys or default_keys
    return " ".join(keys)


class ConfigurationSchema(Schema):
    class Meta:
        unknown = EXCLUDE

    # contains secret schema
    _secret = Nested(SecretSchema, required=True)

    # contains configmap schema
    _configmap = Nested(ConfigmapSchema, required=True)


def load_schema_from_file(path, exclude_configmap=False, exclude_secret=False, key_file=""):
    """Loads schema from file."""
    out, err, code = maybe_encrypted_schema(path, key_file)

    if code != 0:
        return out, err, code

    # dont exclude attributes
    exclude_attrs = []

    # exclude configmap from loading mechanism
    if exclude_configmap:
        key = "_configmap"
        exclude_attrs = [key]
        out.pop(key, None)

    # exclude secret from loading mechanism
    if exclude_secret:
        key = "_secret"
        exclude_attrs = [key]
        out.pop(key, None)

    try:
        out = ConfigurationSchema().load(out, partial=exclude_attrs)
    except ValidationError as exc:
        err = exc.messages
        code = 1
    return out, err, code


def load_schema_key(path):
    try:
        with open(path) as f:
            key = f.read().strip()
    except FileNotFoundError:
        key = ""
    return key


def maybe_encrypted_schema(path, key_file):
    out, err, code = {}, {}, 0

    try:
        # read schema as raw string
        with open(path) as f:
            raw_txt = f.read()
    except FileNotFoundError as exc:
        err = {
            "error": f"Unable to load schema {path}",
            "reason": exc,
        }
        code = exc.errno
    else:
        if key := load_schema_key(key_file):
            # try to decrypt schema (if applicable)
            with suppress(ValueError):
                raw_txt = sprig_decrypt_aes(raw_txt, key)

        try:
            out = json.loads(raw_txt)
        except (json.decoder.JSONDecodeError, UnicodeDecodeError) as exc:
            err = {
                "error": f"Unable to decode JSON from {path}",
                "reason": exc,
            }
            code = 1

    # finalized results
    return out, err, code
