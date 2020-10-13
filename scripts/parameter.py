import json
import re

import cerberus
from fqdn import FQDN

EMAIL_RGX = re.compile(
    r"^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+$"
)
PASSWD_RGX = re.compile(
    r"^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*\W)[a-zA-Z0-9\S]{6,}$"
)


def validate_hostname(field, value, error):
    fqdn = FQDN(value)
    if not fqdn.is_valid:
        error(field, "Invalid FQDN for hostname.")


def validate_email(field, value, error):
    if not EMAIL_RGX.match(value):
        error(field, "invalid email address")


def validate_admin_pw(field, value, error):
    msg = "Password must be at least 6 characters and include " \
          "one uppercase letter, one lowercase letter, one digit, " \
          " and one special character."

    if not PASSWD_RGX.search(value):
        error(field, msg)


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

    schema = {
        "hostname": {
            "type": "string",
            "required": True,
            "check_with": validate_hostname,
        },
        "email": {
            "type": "string",
            "required": True,
            "check_with": validate_email,
        },
        "admin_pw": {
            "type": "string",
            "required": True,
            "check_with": validate_admin_pw,
        },
        "ldap_pw": {
            "type": "string",
            "required": True,
            "check_with": validate_admin_pw,
        },
        "org_name": {
            "type": "string",
            "required": True,
            "empty": False,
        },
        "country_code": {
            "type": "string",
            "required": True,
            "minlength": 2,
            "maxlength": 2,
        },
        "state": {
            "type": "string",
            "required": True,
            "empty": False,
        },
        "city": {
            "type": "string",
            "required": True,
            "empty": False,
        },
    }

    validator = cerberus.Validator(schema)
    validator.allow_unknown = True

    if not validator.validate(docs):
        err = validator.errors
        code = 1
    else:
        out = docs
    return out, err, code
