import binascii
import contextlib
import json
import logging
import re
from base64 import b64decode

import pem
from apispec import APISpec
from apispec.ext.marshmallow import MarshmallowPlugin
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
from marshmallow.validate import Predicate
from marshmallow.validate import Range

from jans.pycloudlib.version import __version__

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

    salt = String(
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

    @validates("salt")
    def validate_salt(self, value):
        if value and len(value) != 24:
            raise ValidationError("Length must be 24.")

        if value and not value.isalnum():
            raise ValidationError("Only alphanumeric characters are allowed.")

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

    email = Email(
        required=True,
        description="Email address",
    )

    hostname = String(
        required=True,
        description="Fully qualified domain name (FQDN)",
    )

    org_name = String(
        required=True,
        description="Organization name",
    )

    state = String(
        required=True,
        description="State or Province Name",
    )

    optional_scopes = List(
        String(),
        validate=ContainsOnly(OPTIONAL_SCOPES),
        load_default=[],
        description="List of optional scopes of components",
    )

    auth_sig_keys = String(
        load_default="",
        dump_default="",
        description="Signature keys to generate",
    )

    auth_enc_keys = String(
        load_default="",
        dump_default="",
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

    @validates("hostname")
    def validate_fqdn(self, value):
        if not FQDN(value).is_valid:
            raise ValidationError("Invalid FQDN format.")


class ConfigurationSchema(Schema):
    class Meta:
        unknown = EXCLUDE

    # contains secret schema
    _secret = Nested(SecretSchema)

    # contains configmap schema
    _configmap = Nested(ConfigmapSchema)


def get_schema_spec():
    spec = APISpec(
        title="Janssen cloud-native configuration",
        version=__version__,
        openapi_version="3.0.2",
        plugins=[MarshmallowPlugin()],
    )
    spec.components.schema("Configuration", schema=ConfigurationSchema)
    return spec.to_dict()


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
