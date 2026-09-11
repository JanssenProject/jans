import json
import logging.config
import os
from hashlib import md5
from urllib.parse import urlparse

from jans.pycloudlib.persistence.sql import doc_id_from_dn
from jans.pycloudlib.utils import exec_cmd

from settings import LOGGING_CONFIG
logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("jans-config-api")


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


def transform_url(url):
    auth_base_url = os.environ.get("CN_AUTH_BASE_URL") or os.environ.get("CN_AUTH_SERVER_URL") or ""

    if not auth_base_url:
        return url

    logger.info("Found base URL override for endpoints from CN_AUTH_BASE_URL or CN_AUTH_SERVER_URL (deprecated) environment variable; value=%s", auth_base_url)

    # handle bare URL (without scheme)
    if not any([
        auth_base_url.startswith("http://"),
        auth_base_url.startswith("https://"),
    ]):
        auth_base_url = f"http://{auth_base_url}"

    parse_result = urlparse(url)
    if parse_result.path.startswith("/.well-known"):
        path = f"/jans-auth{parse_result.path}"
    else:
        path = parse_result.path
    return f"{auth_base_url}{path}"


class URLModifier:
    def __init__(self, sql_client):
        self.client = sql_client

    def get_auth_config(self):
        dn = "ou=jans-auth,ou=configuration,o=jans"
        entry = self.client.get("jansAppConf", doc_id_from_dn(dn))
        return json.loads(entry["jansConfDyn"])

    def get_injected_urls(self):
        auth_config = self.get_auth_config()
        urls = (
            "issuer",
            "openIdConfigurationEndpoint",
            "introspectionEndpoint",
            "tokenEndpoint",
            "tokenRevocationEndpoint",
        )

        return {
            url: transform_url(auth_config[url])
            for url in urls
        }
