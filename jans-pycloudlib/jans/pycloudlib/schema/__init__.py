import binascii
import contextlib
import json
import logging
import re
from base64 import b64decode

import pem
from fqdn import FQDN
from marshmallow import EXCLUDE
from marshmallow import post_load
from marshmallow import Schema
from marshmallow import validates
from marshmallow import ValidationError
from marshmallow.fields import Email
from marshmallow.fields import Integer
from marshmallow.fields import List
from marshmallow.fields import Nested
from marshmallow.fields import String
from marshmallow.validate import ContainsOnly
from marshmallow.validate import Length
from marshmallow.validate import OneOf
from marshmallow.validate import Predicate
from marshmallow.validate import Range

logger = logging.getLogger(__name__)

PASSWD_RGX = re.compile(
    r"^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*\W)[a-zA-Z0-9\S]{6,}$"
)


DEFAULT_SCOPES = (
    "auth",
    "config-api",
)

OPTIONAL_SCOPES = (
    "ldap",
    "couchbase",
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

        try:
            values = pem.parse(value)
        except AttributeError:
            values = []

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

    admin_pw = String(
        required=True,
        description="Password for admin user",
    )

    # previously ldap_pw
    ldap_password = String(
        load_default="",
        dump_default="",
        description="Password for LDAP (OpenDJ) user",
    )

    ldap_truststore_pass = String(
        load_default="",
        dump_default="",
        description="Password for LDAP (OpenDJ) truststore",
    )

    ldap_ssl_cert = CertKey(
        load_default="",
        dump_default="",
        description="LDAP (OpenDJ) certificate",
    )

    ldap_ssl_key = CertKey(
        load_default="",
        dump_default="",
        description="LDAP (OpenDJ) private key",
    )

    # previously sql_pw
    sql_password = String(
        load_default="",
        dump_default="",
        description="Password for SQL (RDBMS) user",
    )

    # previously couchbase_pw
    couchbase_password = String(
        load_default="",
        dump_default="",
        description="Password for Couchbase user",
    )

    # previously couchbase_superuser_pw
    couchbase_superuser_password = String(
        load_default="",
        dump_default="",
        description="Password for Couchbase superuser",
    )

    couchbase_cert = CertKey(
        load_default="",
        dump_default="",
        description="Couchbase certificate",
    )

    encoded_salt = String(
        load_default="",
        dump_default="",
        description="Salt for encoding/decoding sensitive secret",
    )

    google_credentials = String(
        load_default="",
        dump_default="",
        description="String contains Google application credentials",
    )

    aws_credentials = String(
        load_default="",
        dump_default="",
        description="String contains AWS shared credentials",
    )

    aws_config = String(
        load_default="",
        dump_default="",
        description="String contains AWS config",
    )

    aws_replica_regions = String(
        load_default="",
        dump_default="",
        description="String contains AWS replica regions config",
    )

    vault_role_id = String(
        load_default="",
        dump_default="",
        description="Vault RoleID",
    )

    vault_secret_id = String(
        load_default="",
        dump_default="",
        description="Vault SecretID",
    )

    kc_db_password = String(
        load_default="",
        dump_default="",
        description="Password for Keycloak RDBMS user",
    )

    admin_ui_client_encoded_pw = String(
        load_default="",
        dump_default="",
        description="Encoded password for admin-ui client",
    )

    admin_ui_client_pw = String(
        load_default="",
        dump_default="",
        description="Password for admin-ui client",
    )

    auth_jks_base64 = String(
        load_default="",
        dump_default="",
        description="Private keys (keystore) of jans-auth",
    )

    auth_openid_jks_pass = String(
        load_default="",
        dump_default="",
        description="Password of jans-auth keystore",
    )

    auth_openid_key_base64 = String(
        load_default="",
        dump_default="",
        description="Public keys (JWKS) of jans-auth",
    )

    casa_client_encoded_pw = String(
        load_default="",
        dump_default="",
        description="Encoded password for jans-casa client",
    )

    casa_client_pw = String(
        load_default="",
        dump_default="",
        description="Password for jans-casa client",
    )

    encoded_admin_password = String(
        load_default="",
        dump_default="",
        description="LDAP-encoded password of admin",
    )

    encoded_ox_ldap_pw = String(
        load_default="",
        dump_default="",
        description="Encoded password for Bind DN",
    )

    encoded_ldapTrustStorePass = String(
        load_default="",
        dump_default="",
        description="Encoded password for LDAP (OpenDJ) truststore",
    )

    jans_idp_client_secret = String(
        load_default="",
        dump_default="",
        description="Client secret of jans-idp app",
    )

    jans_idp_user_password = String(
        load_default="",
        dump_default="",
        description="User password for jans-idp",
    )

    jca_client_encoded_pw = String(
        load_default="",
        dump_default="",
        description="Encoded password for jans-config-api client",
    )

    jca_client_pw = String(
        load_default="",
        dump_default="",
        description="Password for jans-config-api client",
    )

    kc_admin_password = String(
        load_default="admin",
        dump_default="admin",
        description="Admin password of Keycloak",
    )

    kc_master_auth_client_encoded_pw = String(
        load_default="",
        dump_default="",
        description="Client encoded secret of Keycloak master auth app",
    )

    kc_master_auth_client_pw = String(
        load_default="",
        dump_default="",
        description="Client secret of Keycloak master auth app",
    )

    kc_saml_openid_client_encoded_pw = String(
        load_default="",
        dump_default="",
        description="Client encoded secret of Keycloak SAML app",
    )

    kc_saml_openid_client_pw = String(
        load_default="",
        dump_default="",
        description="Client secret of Keycloak SAML app",
    )

    kc_scheduler_api_client_encoded_pw = String(
        load_default="",
        dump_default="",
        description="Client encoded secret of Keycloak scheduler API app",
    )

    kc_scheduler_api_client_pw = String(
        load_default="",
        dump_default="",
        description="Client secret of Keycloak scheduler API app",
    )

    ldap_pkcs12_base64 = String(
        load_default="",
        dump_default="",
        description="Private keys (keystore) of LDAP (OpenDJ)",
    )

    otp_configuration = String(
        load_default="",
        dump_default="",
        description="OTP configuration string",
    )

    pairwiseCalculationKey = String(
        load_default="",
        dump_default="",
        description="Pairwise calculation key",
    )

    pairwiseCalculationSalt = String(
        load_default="",
        dump_default="",
        description="Pairwise calculation salt",
    )

    saml_scim_client_encoded_pw = String(
        load_default="",
        dump_default="",
        description="Encoded password for test client",
    )

    saml_scim_client_pw = String(
        load_default="",
        dump_default="",
        description="Password for saml-scim client",
    )

    scim_client_encoded_pw = String(
        load_default="",
        dump_default="",
        description="Encoded password for jans-scim client",
    )

    scim_client_pw = String(
        load_default="",
        dump_default="",
        description="Password for jans-scim client",
    )

    smtp_jks_base64 = String(
        load_default="",
        dump_default="",
        description="Private keys (keystore) of SMTP",
    )

    smtp_jks_pass = String(
        load_default="",
        dump_default="",
        description="Password of SMTP keystore",
    )

    smtp_jks_pass_enc = String(
        load_default="",
        dump_default="",
        description="Encoded password of SMTP keystore",
    )

    ssl_ca_cert = CertKey(
        load_default="",
        dump_default="",
        description="SSL certificate for CA",
    )

    ssl_ca_key = CertKey(
        load_default="",
        dump_default="",
        description="SSL key for CA",
    )

    ssl_cert = CertKey(
        load_default="",
        dump_default="",
        description="SSL certificate for the FQDN",
    )

    ssl_csr = CertKey(
        load_default="",
        dump_default="",
        description="SSL certificate signing request for the FQDN",
    )

    ssl_key = CertKey(
        load_default="",
        dump_default="",
        description="SSL key for the FQDN",
    )

    super_gluu_creds = String(
        load_default="",
        dump_default="",
        description="SuperGluu credentials string",
    )

    test_client_encoded_pw = String(
        load_default="",
        dump_default="",
        description="Encoded password for test client",
    )

    test_client_pw = String(
        load_default="",
        dump_default="",
        description="Password for test client",
    )

    token_server_admin_ui_client_encoded_pw = String(
        load_default="",
        dump_default="",
        description="Encoded password for token server client",
    )

    token_server_admin_ui_client_pw = String(
        load_default="",
        dump_default="",
        description="Password for token server client",
    )

    tui_client_encoded_pw = String(
        load_default="",
        dump_default="",
        description="Encoded password for TUI client",
    )

    tui_client_pw = String(
        load_default="",
        dump_default="",
        description="Password for TUI client",
    )

    @post_load
    def transform_b64(self, in_data, **kwargs):
        # list of attrs that maybe base64 string and need to be decoded
        for attr in [
            "google_credentials",
            "aws_config",
            "aws_credentials",
            "aws_replica_regions",
        ]:
            with contextlib.suppress(UnicodeDecodeError, binascii.Error):
                in_data[attr] = b64decode(in_data[attr]).decode()
        return in_data

    @validates("encoded_salt")
    def validate_salt(self, value):
        if value and len(value) != 24:
            raise ValidationError("Length must be 24")

        if value and not value.isalnum():
            raise ValidationError("Only alphanumeric characters are allowed")

    @validates("admin_pw")
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
        description="Locality name (.e.g city)",
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
        description="Country name (2 letter code)"
    )

    admin_email = Email(
        required=True,
        description="Email address",
    )

    hostname = String(
        required=True,
        description="Fully qualified domain name (FQDN)",
    )

    orgName = String(
        required=True,
        description="Organization name",
    )

    state = String(
        required=True,
        description="State or Province Name",
    )

    # @TODO: change to string-based list
    optional_scopes = List(
        String(),
        validate=ContainsOnly(OPTIONAL_SCOPES),
        load_default=[],
        description="List of optional scopes of components",
    )

    auth_sig_keys = String(
        load_default=" ".join(AUTH_SIG_KEYS),
        dump_default=" ".join(AUTH_SIG_KEYS),
        description="Signature keys to generate",
    )

    auth_enc_keys = String(
        load_default=" ".join(AUTH_ENC_KEYS),
        dump_default=" ".join(AUTH_ENC_KEYS),
        description="Encryption keys to generate",
    )

    init_keys_exp = Integer(
        validate=[
            Range(1),
        ],
        load_default=48,
        dump_default=48,
        strict=True,
        description="Initial expiration time (in hours) for generated keys",
    )

    admin_inum = String(
        load_default="",
        dump_default="",
        description="Inum for admin user",
    )

    admin_ui_client_id = String(
        load_default="",
        dump_default="",
        description="Client ID of admin-ui app",
    )

    casa_client_id = String(
        load_default="",
        dump_default="",
        description="Client ID of jans-casa app",
    )

    jans_idp_client_id = String(
        load_default="",
        dump_default="",
        description="Client ID of jans-idp app",
    )

    jca_client_id = String(
        load_default="",
        dump_default="",
        description="Client ID of jans-config-api app",
    )

    scim_client_id = String(
        load_default="",
        dump_default="",
        description="Client ID of jans-scim app",
    )

    tui_client_id = String(
        load_default="",
        dump_default="",
        description="Client ID of jans-tui app",
    )

    test_client_id = String(
        load_default="",
        dump_default="",
        description="Client ID of test app",
    )

    saml_scim_client_id = String(
        load_default="",
        dump_default="",
        description="Client ID of saml-scim app",
    )

    kc_master_auth_client_id = String(
        load_default="",
        dump_default="",
        description="Client ID of Keycloak master auth app",
    )

    kc_saml_openid_client_id = String(
        load_default="",
        dump_default="",
        description="Client ID of Keycloak SAML OpenID app",
    )

    kc_scheduler_api_client_id = String(
        load_default="",
        dump_default="",
        description="Client ID of Keycloak scheduler API app",
    )

    token_server_admin_ui_client_id = String(
        load_default="",
        dump_default="",
        description="Client ID of token server app",
    )

    auth_key_rotated_at = String(
        load_default="",
        dump_default="",
        description="Timestamp of last auth keys regeneration",
    )

    auth_legacyIdTokenClaims = String(
        validate=[
            OneOf(["false", "true"]),
        ],
        load_default="false",
        dump_default="false",
        description="Enable legacy ID token claim",
    )

    auth_openidScopeBackwardCompatibility = String(
        validate=[
            OneOf(["false", "true"]),
        ],
        load_default="false",
        dump_default="false",
        description="Enable backward-compat OpenID scope",
    )

    # @TODO: change to HARDCODED value instead
    auth_openid_jks_fn = String(
        load_default="/etc/certs/auth-keys.jks",
        dump_default="/etc/certs/auth-keys.jks",
        description="Path to keystore file contains private keys for jans-auth",
    )

    # @TODO: change to HARDCODED value instead
    auth_openid_jwks_fn = String(
        load_default="/etc/certs/auth-keys.json",
        dump_default="/etc/certs/auth-keys.json",
        description="Path to JSON file contains public keys for jans-auth",
    )

    # @TODO: change to HARDCODED value instead
    ldapTrustStoreFn = String(
        load_default="/etc/certs/opendj.pkcs12",
        dump_default="/etc/certs/opendj.pkcs12",
        description="Path to keystore file used for connecting to LDAP (OpenDJ)",
    )

    # @TODO: change to HARDCODED value instead
    ldap_binddn = String(
        load_default="cn=Directory Manager",
        dump_default="cn=Directory Manager",
        description="Bind DN for LDAP (OpenDJ)",
    )

    # @TODO: change to HARDCODED value instead
    ldap_site_binddn = String(
        load_default="cn=Directory Manager",
        dump_default="cn=Directory Manager",
        description="Bind DN for LDAP (OpenDJ)",
    )

    # @TODO: change to HARDCODED value instead
    default_openid_jks_dn_name = String(
        load_default="CN=Janssen Auth CA Certificates",
        dump_default="CN=Janssen Auth CA Certificates",
        description="CommonName for jans-auth CA certificate",
    )

    kc_admin_username = String(
        load_default="admin",
        dump_default="admin",
        description="Admin username of Keycloak",
    )

    ldap_init_host = String(
        load_default="ldap",
        dump_default="ldap",
        description="Initial hostname for LDAP (OpenDJ)",
    )

    ldap_init_port = String(
        load_default="1636",
        dump_default="1636",
        description="Initial port for LDAP (OpenDJ)",
    )

    ldap_peers = String(
        load_default="",
        dump_default="",
        description="Mapping of LDAP (OpenDJ) peers contains host and its ports",
    )

    ldap_port = String(
        load_default="1389",
        dump_default="1389",
        description="Port for LDAP (OpenDJ)",
    )

    ldaps_port = String(
        load_default="1636",
        dump_default="1636",
        description="Secure port for LDAP (OpenDJ)",
    )

    smtp_alias = String(
        load_default="smtp_sig_ec256",
        dump_default="smtp_sig_ec256",
        description="Alias for SMTP entry in truststore",
    )

    smtp_signing_alg = String(
        load_default="SHA256withECDSA",
        dump_default="SHA256withECDSA",
        description="SMTP signing algorithm",
    )

    @validates("hostname")
    def validate_fqdn(self, value):
        if not FQDN(value).is_valid:
            raise ValidationError("Invalid FQDN format.")

    @post_load
    def transform_data(self, in_data, **kwargs):
        in_data["auth_sig_keys"] = transform_auth_keys(in_data["auth_sig_keys"], AUTH_SIG_KEYS)
        in_data["auth_enc_keys"] = transform_auth_keys(in_data["auth_enc_keys"], AUTH_ENC_KEYS)
        return in_data


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
    _secret = Nested(SecretSchema)

    # contains configmap schema
    _configmap = Nested(ConfigmapSchema)


def params_from_file(path):
    """Loads parameter from file."""
    out = {}
    err = {}
    code = 0

    try:
        with open(path) as f:
            docs = json.loads(f.read())
    except (IOError, ValueError) as exc:
        err = exc
        code = 1
        return out, err, code

    try:
        out = ConfigurationSchema().load(docs)
    except ValidationError as exc:
        err = exc.messages
        code = 1
    return out, err, code
