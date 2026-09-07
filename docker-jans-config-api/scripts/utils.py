import json
from datetime import datetime
from datetime import UTC
from hashlib import md5

from jans.pycloudlib.utils import exec_cmd


def get_config_api_scope_mapping(path="/app/templates/jans-config-api/config-api-rs-protect.json"):
    scope_mapping = {}
    scope_levels = ["scopes", "groupScopes", "superScopes"]

    with open(path) as f:
        scope_defs = json.loads(f.read())

    for resource in scope_defs["resources"]:
        for condition in resource["conditions"]:
            for scope_level in scope_levels:
                scope_mapping.update({
                    scope["inum"]: {
                        "name": scope["name"],
                        "level": scope_level,
                    }
                    for scope in condition.get(scope_level, [])
                    if scope.get("inum") and scope.get("name")
                })
    return scope_mapping


def utcnow():
    return datetime.now(UTC)


def generalized_time_utc(dtime=None):
    """Calculate LDAP generalized time."""
    if not dtime:
        dtime = utcnow()
    return dtime.strftime("%Y%m%d%H%M%SZ")


def get_ads_project_base64(path):
    out, err, code = exec_cmd(f"base64 -w0 {path}")
    if code != 0:
        raise IOError(f"Unable to resolve contents of {path} as base64 strings; err={err.decode()}")
    return out.decode()


def get_ads_project_md5sum(path):
    with open(path, "rb") as f:
        return md5(f.read()).hexdigest()  # nosec: B324


AUI_AGAMA_PW_DEPLOYMENT_ID = "ab7aec3d-43f5-3c3f-81de-93a24dfd3f84"
AUI_AGAMA_PW_ARCHIVE = "/usr/share/java/admin-ui-plugin-agama-pw.gama"
