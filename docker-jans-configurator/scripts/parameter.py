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
from marshmallow.validate import ContainsOnly
from marshmallow.validate import Length
from marshmallow.validate import Predicate

PASSWD_RGX = re.compile(
    r"^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*\W)[a-zA-Z0-9\S]{6,}$"
)

DEFAULT_SCOPES = (
    "auth",
    "config-api",
)

OPTIONAL_SCOPES = (
    "ldap",
    "scim",
    "fido2",
    "client-api",
    "couchbase",
    "redis",
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

    # see validate_fqdn for validation
    hostname = Str(required=True)

    org_name = Str(required=True)

    state = Str(required=True)

    optional_scopes = List(
        Str(),
        validate=ContainsOnly(OPTIONAL_SCOPES),
        missing=[],
    )

    # see validate_ldap_pw for validation
    ldap_pw = Str(missing="", default="")

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
                " and one special character."
            )

    @validates_schema
    def validate_ldap_pw(self, data, **kwargs):
        if "ldap" in data["optional_scopes"]:
            try:
                self.validate_password(data["ldap_pw"])
            except ValidationError as exc:
                raise ValidationError({"ldap_pw": exc.messages})


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
