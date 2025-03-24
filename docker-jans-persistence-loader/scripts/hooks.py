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
    ctx["jans_auth_openid_jks_fn"] = manager.config.get("auth_openid_jks_fn")
    return ctx


def transform_auth_dynamic_config_hook(conf, manager):
    should_update = False
    hostname = manager.config.get("hostname")
    auth_challenge_endpoint = f"https://{hostname}/jans-auth/restv1/authorize-challenge"

    # add missing top-level keys
    for missing_key, value in [
        ("redirectUrisRegexEnabled", True),
        ("accessTokenSigningAlgValuesSupported", [
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
            "PS512",
        ]),
        ("forceSignedRequestObject", False),
        ("grantTypesAndResponseTypesAutofixEnabled", False),
        ("useHighestLevelScriptIfAcrScriptNotFound", False),
        ("requestUriBlockList", ["localhost", "127.0.0.1"]),
        ("ssaConfiguration", {
            "ssaEndpoint": f"https://{hostname}/jans-auth/restv1/ssa",
            "ssaSigningAlg": "RS256",
            "ssaExpirationInDays": 30,
        }),
        ("blockWebviewAuthorizationEnabled", False),
        ("subjectIdentifiersPerClientSupported", ["mail", "uid"]),
        ("agamaConfiguration", {
            "enabled": True,
            "templatesPath": "/ftl",
            "scriptsPath": "/scripts",
            "serializerType": "KRYO",
            "maxItemsLoggedInCollections": 9,
            "pageMismatchErrorPage": "mismatch.ftl",
            "interruptionErrorPage": "timeout.ftl",
            "crashErrorPage": "crash.ftl",
            "finishedFlowPage": "finished.ftl",
            "defaultResponseHeaders": {
                "Cache-Control": "max-age=0, no-store",
            },
        }),
        ("authorizationChallengeEndpoint", auth_challenge_endpoint),
        ("archivedJwksUri", f"https://{hostname}/jans-auth/restv1/jwks/archived"),
        ("featureFlags", []),
        ("lockMessageConfig", {
            "enableTokenMessages": False,
            "tokenMessagesChannel": "jans_token"
        }),
        ("txTokenSigningAlgValuesSupported", [
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
        ]),
        ("txTokenEncryptionAlgValuesSupported", [
            "RSA1_5",
            "RSA-OAEP",
            "A128KW",
            "A256KW"
        ]),
        ("txTokenEncryptionEncValuesSupported", [
            "A128CBC+HS256",
            "A256CBC+HS512",
            "A128GCM",
            "A256GCM"
        ]),
        ("introspectionSigningAlgValuesSupported", [
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
        ]),
        ("introspectionEncryptionAlgValuesSupported", [
            "RSA1_5",
            "RSA-OAEP",
            "A128KW",
            "A256KW"
        ]),
        ("introspectionEncryptionEncValuesSupported", [
            "A128CBC+HS256",
            "A256CBC+HS512",
            "A128GCM",
            "A256GCM"
        ]),
        ("txTokenLifetime", 180),
        ("sessionIdCookieLifetime", 86400),
        ("tokenIndexAllocationBlockSize", 10),
        ("tokenIndexLimit", 10000000),
    ]:
        if missing_key not in conf:
            conf[missing_key] = value
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

    if "httpLoggingExcludePaths" not in conf:
        conf["httpLoggingExcludePaths"] = conf.pop("httpLoggingExludePaths", [])
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
        ("oxOpenIdConnectVersion", "jansOpenIdConnectVersion"),
        ("oxId", "jansId"),
    ]:
        if new_attr not in conf:
            conf[new_attr] = conf.pop(old_attr, None)
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
        os.environ.get("CN_PERSISTENCE_TYPE") == "sql",
        conf["personCustomObjectClassList"]
    ]):
        conf["personCustomObjectClassList"] = []
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

    # ensure agama_flow removed from authorizationRequestCustomAllowedParameters
    if "agama_flow" in [
        p["paramName"] for p in conf["authorizationRequestCustomAllowedParameters"]
    ]:
        conf["authorizationRequestCustomAllowedParameters"] = list(
            itertools.takewhile(lambda p: p["paramName"] != "agama_flow", conf["authorizationRequestCustomAllowedParameters"])
        )
        should_update = True

    # add missing agama-level keys
    for new_key, value in [
        # avoid setting agama configuration root dir based on java system variable
        ("rootDir", "/opt/jans/jetty/jans-auth/agama"),
        # add serializers
        ("serializeRules", {
            "JAVA": ["java", "sun", "com.sun", "jdk"],
            "KRYO": [],
        }),
        # add url mapping
        ("startEndUrlMapping", {
            "agama/agama.xhtml": "postlogin.htm",
            "agama/consent.xhtml": "agama/consent_authorize.htm",
        }),
    ]:
        if new_key not in conf["agamaConfiguration"]:
            conf["agamaConfiguration"][new_key] = value
            should_update = True

    # lockMessageConfig attribute rename
    for new_attr, old_attr, default_val in [
        ("enableTokenMessages", "enableIdTokenMessages", False),
        ("tokenMessagesChannel", "idTokenMessagesChannel", "jans_token"),
    ]:
        if new_attr not in conf["lockMessageConfig"]:
            conf["lockMessageConfig"][new_attr] = default_val
            conf["lockMessageConfig"].pop(old_attr, None)
            should_update = True

    # dynamicGrantTypeDefault changed to grantTypesSupportedByDynamicRegistration
    if "grantTypesSupportedByDynamicRegistration" not in conf:
        conf["grantTypesSupportedByDynamicRegistration"] = conf.pop("dynamicGrantTypeDefault", [])
        should_update = True

    for grant_type in [
        "authorization_code",
        "implicit",
        "client_credentials",
        "refresh_token",
        "urn:ietf:params:oauth:grant-type:uma-ticket",
        "urn:ietf:params:oauth:grant-type:device_code",
        "urn:ietf:params:oauth:grant-type:token-exchange",
        "password",
    ]:
        if grant_type not in conf["grantTypesSupportedByDynamicRegistration"]:
            conf["grantTypesSupportedByDynamicRegistration"].append(grant_type)
            should_update = True

    # featureflags
    for flag in [
        "health_check",
        "userinfo",
        "clientinfo",
        "id_generation",
        "registration",
        "introspection",
        "revoke_token",
        "revoke_session",
        "active_session",
        "end_session",
        "status_session",
        "jans_configuration",
        "ciba",
        "device_authz",
        "metric",
        "stat",
        "par",
        "ssa",
        "global_token_revocation",
        "status_list",
        "rate_limit",
        "access_evaluation",
    ]:
        if flag not in conf["featureFlags"]:
            conf["featureFlags"].append(flag)
            should_update = True

    # remove tx_token
    for attr in ["grantTypesSupported", "grantTypesSupportedByDynamicRegistration"]:
        if "tx_token" in conf[attr]:
            conf[attr].remove("tx_token")
            should_update = True

    if conf["authorizationChallengeEndpoint"] != auth_challenge_endpoint:
        conf["authorizationChallengeEndpoint"] = auth_challenge_endpoint
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
        return ["jans-auth/people.ldif"]

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
