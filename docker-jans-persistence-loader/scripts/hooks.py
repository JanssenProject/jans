"""Hooks are special callables that can be used to change the behavior
of default callables, i.e. change keystore in specific Janssen image.

Currently, hooks are meant to be overriden manually. In the future,
we can use specialized hooks/plugins system.
"""
import itertools
import os
import typing as _t

from jans.pycloudlib.persistence.utils import PersistenceMapper


def merge_auth_keystore_ctx_hook(manager, ctx: dict[str, _t.Any]) -> dict[str, _t.Any]:
    # maintain compatibility with upstream template
    ctx["oxauth_openid_jks_fn"] = manager.config.get("auth_openid_jks_fn")
    return ctx


def transform_auth_dynamic_config_hook(conf, manager):
    should_update = False

    if "redirectUrisRegexEnabled" not in conf:
        conf["redirectUrisRegexEnabled"] = True
        should_update = True

    if "accessTokenSigningAlgValuesSupported" not in conf:
        conf["accessTokenSigningAlgValuesSupported"] = [
            "none",
            "HS256",
            "HS384",
            "HS512",
            "RS256",
            "RS384",
            "RS512",
            "ES256",
            "ES384",
            "ES512",
            "ES512",
            "PS256",
            "PS384",
            "PS512"
        ]
        should_update = True

    if "forceSignedRequestObject" not in conf:
        conf["forceSignedRequestObject"] = False
        should_update = True

    if "grantTypesAndResponseTypesAutofixEnabled" not in conf:
        conf["grantTypesAndResponseTypesAutofixEnabled"] = False
        should_update = True

    if "sessionIdEnabled" in conf:
        conf.pop("sessionIdEnabled")
        should_update = True

    # assert the authorizationRequestCustomAllowedParameters contains dict values instead of string
    params_with_dict = list(itertools.takewhile(
        lambda x: isinstance(x, dict), conf["authorizationRequestCustomAllowedParameters"]
    ))
    if not params_with_dict:
        conf["authorizationRequestCustomAllowedParameters"] = [
            {"paramName": p[0], "returnInResponse": p[1]}
            for p in [
                ("customParam1", False),
                ("customParam2", False),
                ("customParam3", False),
                ("customParam4", True),
                ("customParam5", True),
            ]
        ]
        should_update = True

    if "useHighestLevelScriptIfAcrScriptNotFound" not in conf:
        conf["useHighestLevelScriptIfAcrScriptNotFound"] = False
        should_update = True

    if "httpLoggingExcludePaths" not in conf:
        conf["httpLoggingExcludePaths"] = conf.pop("httpLoggingExludePaths", [])
        should_update = True

    if "requestUriBlockList" not in conf:
        conf["requestUriBlockList"] = [
            "localhost",
            "127.0.0.1",
        ]
        should_update = True

    if "ssaConfiguration" not in conf:
        hostname = manager.config.get("hostname")
        conf["ssaConfiguration"] = {
            "ssaEndpoint": f"https://{hostname}/jans-auth/restv1/ssa",
            "ssaSigningAlg": "RS256",
            "ssaExpirationInDays": 30
        }
        should_update = True

    if "ssaCustomAttributes" not in conf["ssaConfiguration"]:
        conf["ssaConfiguration"]["ssaCustomAttributes"] = []
        should_update = True

    for grant_type in [
        "urn:ietf:params:oauth:grant-type:device_code",
        "urn:ietf:params:oauth:grant-type:token-exchange",
    ]:
        if grant_type not in conf["grantTypesSupported"]:
            conf["grantTypesSupported"].append(grant_type)
            should_update = True

    # change ox to jans
    for old_attr, new_attr in [
        ("oxElevenGenerateKeyEndpoint", "jansElevenGenerateKeyEndpoint"),
        ("oxElevenSignEndpoint", "jansElevenSignEndpoint"),
        ("oxElevenVerifySignatureEndpoint", "jansElevenVerifySignatureEndpoint"),
        ("oxElevenDeleteKeyEndpoint", "jansElevenDeleteKeyEndpoint"),
        ("oxElevenJwksEndpoint", "jansElevenJwksEndpoint"),
        ("oxOpenIdConnectVersion", "jansOpenIdConnectVersion"),
        ("oxId", "jansId"),
    ]:
        if new_attr not in conf:
            conf[new_attr] = conf.pop(old_attr, None)
            should_update = True

    if "blockWebviewAuthorizationEnabled" not in conf:
        conf["blockWebviewAuthorizationEnabled"] = False
        should_update = True

    if "dateFormatterPatterns" not in conf:
        # remove old config
        conf.pop("userInfoConfiguration", None)
        conf["dateFormatterPatterns"] = {
            "birthdate": "yyyy-MM-dd",
        }
        should_update = True

    if "persistIdToken" not in conf:
        conf["persistIdToken"] = conf.pop("persistIdTokenInLdap", False)
        should_update = True

    if "persistRefreshToken" not in conf:
        conf["persistRefreshToken"] = conf.pop("persistRefreshTokenInLdap", True)
        should_update = True

    if all([
        os.environ.get("CN_PERSISTENCE_TYPE") in ("sql", "spanner"),
        conf["personCustomObjectClassList"]
    ]):
        conf["personCustomObjectClassList"] = []
        should_update = True

    if "subjectIdentifiersPerClientSupported" not in conf:
        conf["subjectIdentifiersPerClientSupported"] = ["mail", "uid"]
        should_update = True

    if "agamaConfiguration" not in conf:
        conf["agamaConfiguration"] = {
            "enabled": False,
            "templatesPath": "/ftl",
            "scriptsPath": "/scripts",
            "serializerType": "KRYO",
            "maxItemsLoggedInCollections": 9,
            "pageMismatchErrorPage": "mismatch.ftl",
            "interruptionErrorPage": "timeout.ftl",
            "crashErrorPage": "crash.ftl",
            "finishedFlowPage": "finished.ftl",
            "bridgeScriptPage": "agama.xhtml",
            "defaultResponseHeaders": {
                "Cache-Control": "max-age=0, no-store",
            },
        }
        should_update = True

    if "interruptionTime" in conf["agamaConfiguration"]:
        conf["agamaConfiguration"].pop("interruptionTime", None)
        should_update = True

    # add Cache-Control and remove Expires, Content-Type
    if "Cache-Control" not in conf["agamaConfiguration"]["defaultResponseHeaders"]:
        conf["agamaConfiguration"]["defaultResponseHeaders"]["Cache-Control"] = "max-age=0, no-store"
        conf["agamaConfiguration"]["defaultResponseHeaders"].pop("Expires", None)
        conf["agamaConfiguration"]["defaultResponseHeaders"].pop("Content-Type", None)
        should_update = True

    for grant_type in [
        "urn:ietf:params:oauth:grant-type:device_code",
        "urn:ietf:params:oauth:grant-type:token-exchange",
    ]:
        if grant_type not in conf["dynamicGrantTypeDefault"]:
            conf["dynamicGrantTypeDefault"].append(grant_type)
            should_update = True

    # ensure agama_flow listed in authorizationRequestCustomAllowedParameters
    if "agama_flow" not in [
        p["paramName"] for p in conf["authorizationRequestCustomAllowedParameters"]
    ]:
        conf["authorizationRequestCustomAllowedParameters"].append({
            "paramName": "agama_flow", "returnInResponse": False,
        })
        should_update = True

    # avoid setting agama configuration root dir based on java system variable
    if "rootDir" not in conf["agamaConfiguration"]:
        conf["agamaConfiguration"]["rootDir"] = "/opt/jans/jetty/jans-auth/agama"
        should_update = True

    # return the conf and flag to determine whether it needs update or not
    return conf, should_update


def get_ldif_mappings_hook(group, optional_scopes=None):
    optional_scopes = optional_scopes or []

    def default_files():
        return [
            "base.ldif",
            "attributes.ldif",
            "scopes.ldif",
            "scripts.ldif",
            "configuration.ldif",
            "o_metric.ldif",
            "agama.ldif",
            "jans-auth/role-scope-mappings.ldif",
            "jans-cli/client.ldif",
            "jans-auth/configuration.ldif",
        ]

    def user_files():
        return [
            "jans-auth/people.ldif",
            "jans-auth/groups.ldif",
        ]

    def site_files():
        return ["o_site.ldif"]

    ldif_mappings = {
        "default": default_files(),
        "user": user_files(),
        "site": site_files(),
        "cache": [],
        "token": [],
        "session": [],
    }

    mapper = PersistenceMapper()
    ldif_mappings = {
        mapping: files for mapping, files in ldif_mappings.items()
        if mapping in mapper.groups()[group]
    }
    return ldif_mappings
