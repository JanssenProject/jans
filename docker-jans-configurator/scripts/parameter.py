import json
import re

from fqdn import FQDN
from marshmallow import EXCLUDE
from marshmallow import Schema
from marshmallow import validates
from marshmallow import validates_schema
from marshmallow import ValidationError
from marshmallow.fields import Email
from marshmallow.fields import List
from marshmallow.fields import Str
from marshmallow.fields import Int
from marshmallow.validate import ContainsOnly
from marshmallow.validate import Length
from marshmallow.validate import Predicate
from marshmallow.validate import Range

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


class ParamSchema(Schema):
    class Meta:
        unknown = EXCLUDE

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

    ldap_pw = Str(load_default="", dump_default="")

    sql_pw = Str(load_default="", dump_default="")

    couchbase_pw = Str(load_default="", dump_default="")

    couchbase_superuser_pw = Str(load_default="", dump_default="")

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
    def validate_ldap_pw(self, data, **kwargs):
        if "ldap" in data["optional_scopes"]:
            try:
                self.validate_password(data["ldap_pw"])
            except ValidationError as exc:
                raise ValidationError({"ldap_pw": exc.messages})

    @validates_schema
    def validate_ext_persistence_pw(self, data, **kwargs):
        err = {}
        scope_attr_map = [
            ("sql", "sql_pw"),
            ("couchbase", "couchbase_pw"),
        ]

        for scope, attr in scope_attr_map:
            # note we don't enforce custom password validation as cloud-based
            # databases may use password that not conform to our policy
            # hence we simply check for empty password only
            if scope in data["optional_scopes"] and data[attr] == "":
                err[attr] = ["Empty password isn't allowed"]

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
