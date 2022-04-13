import contextlib
import base64
import json
import os
from pathlib import Path
from urllib.parse import urlparse
from uuid import uuid4

from ldap3.utils import dn as dnutils

from jans.pycloudlib.utils import as_boolean
from jans.pycloudlib.utils import encode_text
from jans.pycloudlib.utils import safe_render
from jans.pycloudlib.utils import generate_base64_contents
from jans.pycloudlib.utils import get_random_chars


def render_ldif(src, dst, ctx):
    with open(src) as f:
        txt = f.read()

    with open(dst, "w") as f:
        f.write(safe_render(txt, ctx))


def get_jackrabbit_creds():
    username = os.environ.get("CN_JACKRABBIT_ADMIN_ID", "admin")
    password = ""

    password_file = os.environ.get(
        "CN_JACKRABBIT_ADMIN_PASSWORD_FILE",
        "/etc/jans/conf/jackrabbit_admin_password",
    )
    with contextlib.suppress(FileNotFoundError):
        with open(password_file) as f:
            password = f.read().strip()
    password = password or username
    return username, password


def get_jackrabbit_rmi_url():
    # new style ENV
    rmi_url = os.environ.get("CN_JACKRABBIT_RMI_URL", "")
    if rmi_url:
        return rmi_url

    # fallback to default
    base_url = os.environ.get("CN_JACKRABBIT_URL", "http://localhost:8080")
    return f"{base_url}/rmi"


def get_base_ctx(manager):
    redis_pw = manager.secret.get("redis_pw") or ""
    redis_pw_encoded = ""

    if redis_pw:
        redis_pw_encoded = encode_text(
            redis_pw,
            manager.secret.get("encoded_salt"),
        ).decode()

    doc_store_type = os.environ.get("CN_DOCUMENT_STORE_TYPE", "LOCAL")
    jca_user, jca_pw = get_jackrabbit_creds()

    jca_pw_encoded = encode_text(
        jca_pw,
        manager.secret.get("encoded_salt"),
    ).decode()

    cache_type = os.environ.get("CN_CACHE_TYPE", "NATIVE_PERSISTENCE")
    redis_url = os.environ.get('CN_REDIS_URL', 'localhost:6379')
    redis_type = os.environ.get('CN_REDIS_TYPE', 'STANDALONE')
    redis_use_ssl = os.environ.get("CN_REDIS_USE_SSL", False)
    redis_ssl_truststore = os.environ.get("CN_REDIS_SSL_TRUSTSTORE", "")
    redis_sentinel_group = os.environ.get("CN_REDIS_SENTINEL_GROUP", "")
    memcached_url = os.environ.get('CN_MEMCACHED_URL', 'localhost:11211')
    casa_enabled = os.environ.get("CN_CASA_ENABLED", False)
    scim_enabled = os.environ.get("CN_SCIM_ENABLED", False)

    ctx = {
        'cache_provider_type': cache_type,
        'redis_url': redis_url,
        'redis_type': redis_type,
        'redis_pw': redis_pw,
        'redis_pw_encoded': redis_pw_encoded,
        "redis_use_ssl": "{}".format(as_boolean(redis_use_ssl)).lower(),
        "redis_ssl_truststore": redis_ssl_truststore,
        "redis_sentinel_group": redis_sentinel_group,
        'memcached_url': memcached_url,

        "document_store_type": doc_store_type,
        "jca_server_url": get_jackrabbit_rmi_url(),
        "jca_username": jca_user,
        "jca_pw": jca_pw,
        "jca_pw_encoded": jca_pw_encoded,

        'ldap_hostname': manager.config.get('ldap_init_host'),
        'ldaps_port': manager.config.get('ldap_init_port'),
        'ldap_binddn': manager.config.get('ldap_binddn'),
        "ldap_use_ssl": str(as_boolean(os.environ.get("CN_LDAP_USE_SSL", True))).lower(),
        'encoded_ox_ldap_pw': manager.secret.get('encoded_ox_ldap_pw'),
        'jetty_base': manager.config.get('jetty_base'),
        'orgName': manager.config.get('orgName'),
        'hostname': manager.config.get('hostname'),
        'idp_client_id': manager.config.get('idp_client_id'),
        'idpClient_encoded_pw': manager.secret.get('idpClient_encoded_pw'),
        'auth_openid_key_base64': manager.secret.get('auth_openid_key_base64'),
        # "encoded_ldap_pw": manager.secret.get('encoded_ldap_pw'),
        "encoded_admin_password": manager.secret.get('encoded_admin_password'),

        'admin_email': manager.config.get('admin_email'),
        'shibJksFn': manager.config.get('shibJksFn'),
        'shibJksPass': manager.secret.get('shibJksPass'),
        'encoded_shib_jks_pw': manager.secret.get('encoded_shib_jks_pw'),
        'shibboleth_version': manager.config.get('shibboleth_version'),
        'idp3Folder': manager.config.get('idp3Folder'),
        'ldap_site_binddn': manager.config.get('ldap_site_binddn'),

        "jansScimEnabled": str(as_boolean(scim_enabled)).lower(),

        "pairwiseCalculationKey": manager.secret.get("pairwiseCalculationKey"),
        "pairwiseCalculationSalt": manager.secret.get("pairwiseCalculationSalt"),
        "default_openid_jks_dn_name": manager.config.get("default_openid_jks_dn_name"),
        "auth_openid_jks_pass": manager.secret.get("auth_openid_jks_pass"),
        "auth_legacyIdTokenClaims": manager.config.get("auth_legacyIdTokenClaims"),
        "auth_openidScopeBackwardCompatibility": manager.config.get("auth_openidScopeBackwardCompatibility"),
        "fido2ConfigFolder": manager.config.get("fido2ConfigFolder"),

        "admin_inum": manager.config.get("admin_inum"),
        "scim_client_id": manager.config.get("scim_client_id"),
        "scim_client_encoded_pw": manager.secret.get("scim_client_encoded_pw"),
        "casa_enable_script": str(as_boolean(casa_enabled)).lower(),
        "jca_client_id": manager.config.get("jca_client_id"),
        "jca_client_encoded_pw": manager.secret.get("jca_client_encoded_pw"),
    }

    # JWKS URI
    jwks_uri = f"https://{ctx['hostname']}/jans-auth/restv1/jwks"
    auth_openid_jks_fn = manager.config.get("auth_openid_jks_fn")

    ext_jwks_uri = os.environ.get("CN_OB_EXT_SIGNING_JWKS_URI", "")
    if ext_jwks_uri:
        jwks_uri = ext_jwks_uri
        auth_openid_jks_fn = "/etc/certs/ob-ext-signing.jks"

    ctx["jwks_uri"] = jwks_uri
    ctx["auth_openid_jks_fn"] = auth_openid_jks_fn

    # static kid
    ctx["staticKid"] = os.environ.get("CN_OB_STATIC_KID", "")

    # WARNING:
    # - deprecate configs and secrets for admin_ui and token_server_admin_ui
    # - move the configs and secrets creation to configurator
    # - remove them on future release

    # admin-ui plugins
    ctx["admin_ui_client_id"] = manager.config.get("admin_ui_client_id")
    if not ctx["admin_ui_client_id"]:
        ctx["admin_ui_client_id"] = f"1901.{uuid4()}"
        manager.config.set("admin_ui_client_id", ctx["admin_ui_client_id"])

    ctx["admin_ui_client_pw"] = manager.secret.get("admin_ui_client_pw")
    if not ctx["admin_ui_client_pw"]:
        ctx["admin_ui_client_pw"] = get_random_chars()
        manager.secret.set("admin_ui_client_pw", ctx["admin_ui_client_pw"])

    ctx["admin_ui_client_encoded_pw"] = manager.secret.get("admin_ui_client_encoded_pw")
    if not ctx["admin_ui_client_encoded_pw"]:
        ctx["admin_ui_client_encoded_pw"] = encode_text(ctx["admin_ui_client_pw"], manager.secret.get("encoded_salt")).decode()
        manager.secret.set(
            "admin_ui_client_encoded_pw",
            ctx["admin_ui_client_encoded_pw"],
        )

    # token server client
    ctx["token_server_admin_ui_client_id"] = manager.config.get("token_server_admin_ui_client_id")
    if not ctx["token_server_admin_ui_client_id"]:
        ctx["token_server_admin_ui_client_id"] = f"1901.{uuid4()}"
        manager.config.set("token_server_admin_ui_client_id", ctx["token_server_admin_ui_client_id"])

    ctx["token_server_admin_ui_client_pw"] = manager.secret.get("token_server_admin_ui_client_pw")
    if not ctx["token_server_admin_ui_client_pw"]:
        ctx["token_server_admin_ui_client_pw"] = get_random_chars()
        manager.secret.set("token_server_admin_ui_client_pw", ctx["token_server_admin_ui_client_pw"])

    ctx["token_server_admin_ui_client_encoded_pw"] = manager.secret.get("token_server_admin_ui_client_encoded_pw")
    if not ctx["token_server_admin_ui_client_encoded_pw"]:
        ctx["token_server_admin_ui_client_encoded_pw"] = encode_text(
            ctx["token_server_admin_ui_client_pw"], manager.secret.get("encoded_salt"),
        ).decode()
        manager.secret.set(
            "token_server_admin_ui_client_encoded_pw",
            ctx["token_server_admin_ui_client_encoded_pw"],
        )

    # finalize ctx
    return ctx


def merge_extension_ctx(ctx):
    basedir = "/app/static/extension"

    if os.environ.get("CN_DISTRIBUTION", "default") == "openbanking":
        basedir = "/app/openbanking/static/extension"

    filepath = Path(basedir)
    for ext_path in filepath.glob("**/*.py"):
        if not ext_path.is_file():
            continue

        ext_name = f"{ext_path.parent.name.lower()}_{ext_path.stem.lower()}"
        ctx[ext_name] = generate_base64_contents(ext_path.read_text())
    return ctx


def merge_auth_ctx(ctx):
    basedir = '/app/templates/jans-auth'
    file_mappings = {
        'auth_static_conf_base64': 'jans-auth-static-conf.json',
        'auth_error_base64': 'jans-auth-errors.json',
        "auth_config_base64": "jans-auth-config.json",
    }

    if os.environ.get("CN_DISTRIBUTION", "default") == "openbanking":
        file_mappings["auth_config_base64"] = "jans-auth-config.ob.json"
    # else:
    #     file_mappings["auth_config_base64"] = "jans-auth-config.json"

    for key, file_ in file_mappings.items():
        file_path = os.path.join(basedir, file_)
        with open(file_path) as fp:
            ctx[key] = generate_base64_contents(fp.read() % ctx)
    return ctx


def merge_fido2_ctx(ctx):
    basedir = '/app/templates/jans-fido2'
    file_mappings = {
        'fido2_dynamic_conf_base64': 'dynamic-conf.json',
        'fido2_static_conf_base64': 'static-conf.json',
    }

    for key, file_ in file_mappings.items():
        file_path = os.path.join(basedir, file_)
        with open(file_path) as fp:
            ctx[key] = generate_base64_contents(fp.read() % ctx)
    return ctx


def merge_scim_ctx(ctx):
    basedir = '/app/templates/jans-scim'
    file_mappings = {
        'scim_dynamic_conf_base64': 'dynamic-conf.json',
        'scim_static_conf_base64': 'static-conf.json',
    }

    for key, file_ in file_mappings.items():
        file_path = os.path.join(basedir, file_)
        with open(file_path) as fp:
            ctx[key] = generate_base64_contents(fp.read() % ctx)
    return ctx


def merge_config_api_ctx(ctx):
    def transform_url(url):
        auth_server_url = os.environ.get("CN_AUTH_SERVER_URL", "")

        if not auth_server_url:
            return url

        parse_result = urlparse(url)
        if parse_result.path.startswith("/.well-known"):
            path = f"/jans-auth{parse_result.path}"
        else:
            path = parse_result.path
        url = f"http://{auth_server_url}{path}"
        return url

    def get_injected_urls():
        auth_config = json.loads(
            base64.b64decode(ctx["auth_config_base64"]).decode()
        )
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

    approved_issuer = [ctx["hostname"]]
    token_server_hostname = os.environ.get("CN_TOKEN_SERVER_BASE_HOSTNAME")
    if token_server_hostname and token_server_hostname not in approved_issuer:
        approved_issuer.append(token_server_hostname)

    local_ctx = {
        "apiApprovedIssuer": ",".join([f'"https://{issuer}"' for issuer in approved_issuer]),
        "apiProtectionType": "oauth2",
        "jca_client_id": ctx["jca_client_id"],
        "jca_client_encoded_pw": ctx["jca_client_encoded_pw"],
        "endpointInjectionEnabled": "true",
        "configOauthEnabled": str(os.environ.get("CN_CONFIG_API_OAUTH_ENABLED") or True).lower(),
    }
    local_ctx.update(get_injected_urls())

    basedir = '/app/templates/jans-config-api'
    file_mappings = {
        "config_api_dynamic_conf_base64": "dynamic-conf.json",
    }
    for key, file_ in file_mappings.items():
        file_path = os.path.join(basedir, file_)
        with open(file_path) as fp:
            ctx[key] = generate_base64_contents(fp.read() % local_ctx)
    return ctx


def merge_casa_ctx(manager, ctx):
    # Casa client
    ctx["casa_client_id"] = manager.config.get("casa_client_id")
    if not ctx["casa_client_id"]:
        ctx["casa_client_id"] = f"1902.{uuid4()}"
        manager.config.set("casa_client_id", ctx["casa_client_id"])

    ctx["casa_client_pw"] = manager.secret.get("casa_client_pw")
    if not ctx["casa_client_pw"]:
        ctx["casa_client_pw"] = get_random_chars()
        manager.secret.set("casa_client_pw", ctx["casa_client_pw"])

    ctx["casa_client_encoded_pw"] = manager.secret.get("casa_client_encoded_pw")
    if not ctx["casa_client_encoded_pw"]:
        ctx["casa_client_encoded_pw"] = encode_text(
            ctx["casa_client_pw"], manager.secret.get("encoded_salt"),
        ).decode()
        manager.secret.set("casa_client_encoded_pw", ctx["casa_client_encoded_pw"])

    return ctx


def merge_jans_cli_ctx(manager, ctx):
    # WARNING:
    # - deprecated configs and secrets for role_based
    # - move the configs and secrets creation to configurator
    # - remove them on future release

    # jans-cli client
    ctx["role_based_client_id"] = manager.config.get("role_based_client_id")
    if not ctx["role_based_client_id"]:
        ctx["role_based_client_id"] = f"2000.{uuid4()}"
        manager.config.set("role_based_client_id", ctx["role_based_client_id"])

    ctx["role_based_client_pw"] = manager.secret.get("role_based_client_pw")
    if not ctx["role_based_client_pw"]:
        ctx["role_based_client_pw"] = get_random_chars()
        manager.secret.set("role_based_client_pw", ctx["role_based_client_pw"])

    ctx["role_based_client_encoded_pw"] = manager.secret.get("role_based_client_encoded_pw")
    if not ctx["role_based_client_encoded_pw"]:
        ctx["role_based_client_encoded_pw"] = encode_text(
            ctx["role_based_client_pw"], manager.secret.get("encoded_salt"),
        ).decode()
        manager.secret.set("role_based_client_encoded_pw", ctx["role_based_client_encoded_pw"])
    return ctx


def prepare_template_ctx(manager):
    opt_scopes = json.loads(manager.config.get("optional_scopes", "[]"))

    ctx = get_base_ctx(manager)
    ctx = merge_extension_ctx(ctx)
    ctx = merge_auth_ctx(ctx)
    ctx = merge_config_api_ctx(ctx)
    ctx = merge_fido2_ctx(ctx)
    ctx = merge_scim_ctx(ctx)
    ctx = merge_jans_cli_ctx(manager, ctx)

    if "casa" in opt_scopes:
        ctx = merge_casa_ctx(manager, ctx)
    return ctx


def get_ldif_mappings(optional_scopes=None):
    optional_scopes = optional_scopes or []
    dist = os.environ.get("CN_DISTRIBUTION", "default")

    def default_files():
        files = [
            "base.ldif",
            "jans-config-api/scopes.ldif",
        ]

        if dist == "openbanking":
            files += [
                "attributes.ob.ldif",
                "scopes.ob.ldif",
                "scripts.ob.ldif",
                "configuration.ob.ldif",
                "jans-config-api/clients.ob.ldif",
            ]
        else:
            files += [
                "attributes.ldif",
                "scopes.ldif",
                "scripts.ldif",
                "configuration.ldif",
                "o_metric.ldif",
                "jans-config-api/clients.ldif",
            ]

        files += [
            "jans-config-api/config.ldif",
            "jans-config-api/admin-ui-clients.ldif",
            "jans-auth/configuration.ldif",
            "jans-auth/role-scope-mappings.ldif",
            "jans-cli/client.ldif",
        ]

        if "scim" in optional_scopes:
            files += [
                "jans-scim/configuration.ldif",
                "jans-scim/scopes.ldif",
                "jans-scim/clients.ldif",
            ]

        if "fido2" in optional_scopes:
            files += [
                "jans-fido2/fido2.ldif",
            ]

        if "casa" in optional_scopes:
            files += [
                "gluu-casa/configuration.ldif",
                "gluu-casa/clients.ldif",
                "gluu-casa/scripts.ldif",
            ]
        return files

    def user_files():
        files = []

        if dist != "openbanking":
            files += [
                "jans-auth/people.ldif",
                "jans-auth/groups.ldif",
            ]
        return files

    def site_files():
        files = []

        if dist != "openbanking":
            files += [
                "o_site.ldif",
            ]
        return files

    ldif_mappings = {
        "default": default_files(),
        "user": user_files(),
        "site": site_files(),
        "cache": [],
        "token": [],
        "session": [],
    }
    return ldif_mappings


def doc_id_from_dn(dn):
    parsed_dn = dnutils.parse_dn(dn)
    doc_id = parsed_dn[0][1]

    if doc_id == "jans":
        doc_id = "_"
    return doc_id


def id_from_dn(dn):
    # for example: `"inum=29DA,ou=attributes,o=jans"`
    # becomes `["29DA", "attributes"]`
    dns = [i.split("=")[-1] for i in dn.split(",") if i != "o=jans"]
    dns.reverse()

    # the actual key
    return '_'.join(dns) or "_"
