import binascii
import contextlib
import json
import logging.config
import re
from base64 import b64decode

import pem
from fqdn import FQDN
from marshmallow import INCLUDE
from marshmallow import Schema
from marshmallow import validates
from marshmallow import validates_schema
from marshmallow import ValidationError
from marshmallow.fields import Email
from marshmallow.fields import List
from marshmallow.fields import Str
from marshmallow.fields import Int
from marshmallow.fields import Method
from marshmallow.validate import ContainsOnly
from marshmallow.validate import Length
from marshmallow.validate import Predicate
from marshmallow.validate import Range

from settings import LOGGING_CONFIG

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("configurator")

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

    # these scopes are no longer needed; not removed for backward-compat
    "fido2",
    "casa",
    "scim",
)


class Certificate(Str):
    def _deserialize(self, value, attr, obj, **kwargs):
        super()._deserialize(value, attr, obj, **kwargs)

        try:
            certs = pem.parse(value)
        except AttributeError:
            certs = []

        if not certs:
            # try parsing base64-decoded
            certs = pem.parse(b64decode(value).decode())

        try:
            cert = str(certs[0])
        except IndexError:
            cert = ""
        return cert


class ParamSchema(Schema):
    class Meta:
        unknown = INCLUDE

    admin_pw = Str(required=True)

    city = Str(required=True)

    country_code = Str(
        validate=[
            Length(2, 2),
            Predicate(
                "isupper",
                error="Non-uppercased characters aren't allowed",
            ),
        ],
        required=True,
    )

    email = Email(required=True)

    hostname = Str(required=True)

    org_name = Str(required=True)

    state = Str(required=True)

    optional_scopes = List(
        Str(),
        validate=ContainsOnly(OPTIONAL_SCOPES),
        load_default=[],
    )

    # previously ldap_pw
    ldap_password = Str(load_default="", dump_default="")

    # previously sql_pw
    sql_password = Str(load_default="", dump_default="")

    # previously couchbase_pw
    couchbase_password = Str(load_default="", dump_default="")

    # previously couchbase_superuser_pw
    couchbase_superuser_password = Str(load_default="", dump_default="")

    couchbase_cert = Certificate(load_default="", dump_default="")

    auth_sig_keys = Str(load_default="")

    auth_enc_keys = Str(load_default="")

    salt = Str(load_default="", dump_default="")

    init_keys_exp = Int(
        validate=[
            Range(1),
        ],
        load_default=48,
        dump_default=48,
        strict=True,
    )

    google_credentials = Method(deserialize="mapping_or_string")

    def mapping_or_string(self, value):
        if not isinstance(value, (dict, str)):
            raise ValidationError("Value must be a mapping or string type")

        if isinstance(value, str) and value:
            # decode base64 string if necessary
            try:
                with contextlib.suppress(UnicodeDecodeError):
                    value = b64decode(value).decode()
            except binascii.Error as exc:
                raise ValidationError("Value isn't a mapping") from exc

            try:
                value = json.loads(value)
            except json.decoder.JSONDecodeError as exc:
                raise ValidationError("Value isn't a mapping") from exc

        # finalized type is a mapping
        return value

    @validates("hostname")
    def validate_fqdn(self, value):
        fqdn = FQDN(value)
        if not fqdn.is_valid:
            raise ValidationError("Invalid FQDN format.")

    @validates("admin_pw")
    def validate_password(self, value, **kwargs):
        if not PASSWD_RGX.search(value):
            raise ValidationError(
                "Must be at least 6 characters and include "
                "one uppercase letter, one lowercase letter, one digit, "
                "and one special character."
            )

    @validates_schema
    def validate_persistence_password(self, data, **kwargs):
        err = {}

        # map between scope, old attribute, and new attribute
        scope_attr_map = [
            ("ldap", [("ldap_pw", "ldap_password")]),
            ("sql", [("sql_pw", "sql_password")]),
            ("couchbase", [
                ("couchbase_pw", "couchbase_password"),
                ("couchbase_superuser_pw", "couchbase_superuser_password"),
            ]),
        ]

        for scope, attrs in scope_attr_map:
            for old_attr, new_attr in attrs:
                # note we don't enforce custom password validation as cloud-based
                # databases may use password that not conform to our policy
                # hence we simply check for empty password only
                if scope not in data["optional_scopes"]:
                    continue

                if old_attr in data:
                    logger.warning(
                        f"Found deprecated {old_attr!r}; please use {new_attr!r} instead. "
                        f"Note that the value of {new_attr!r} will be taken from {old_attr!r} for backward-compatibility."
                    )
                    data[new_attr] = data[old_attr]

                if data[new_attr] == "":
                    err[new_attr] = ["Empty password isn't allowed"]

        if err:
            raise ValidationError(err)

    @validates("salt")
    def validate_salt(self, value):
        if value and len(value) != 24:
            raise ValidationError("Length must be 24.")

        if value and not value.isalnum():
            raise ValidationError("Only alphanumeric characters are allowed")


def params_from_file(path):
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
        out = ParamSchema().load(docs)
    except ValidationError as exc:
        err = exc.messages
        code = 1
    return out, err, code
