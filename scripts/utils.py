import contextlib
import base64
import json
import os
from urllib.parse import urlparse

from jans.pycloudlib.utils import as_boolean
from jans.pycloudlib.utils import encode_text
from jans.pycloudlib.utils import safe_render
from jans.pycloudlib.utils import generate_base64_contents


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
    oxtrust_config_generation = os.environ.get("CN_OXTRUST_CONFIG_GENERATION", True)
    passport_enabled = os.environ.get("CN_PASSPORT_ENABLED", False)
    radius_enabled = os.environ.get("CN_RADIUS_ENABLED", False)
    casa_enabled = os.environ.get("CN_CASA_ENABLED", False)
    saml_enabled = os.environ.get("CN_SAML_ENABLED", False)
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
        'auth_client_id': manager.config.get('auth_client_id'),
        'authClient_encoded_pw': manager.secret.get('authClient_encoded_pw'),
        'hostname': manager.config.get('hostname'),
        'idp_client_id': manager.config.get('idp_client_id'),
        'idpClient_encoded_pw': manager.secret.get('idpClient_encoded_pw'),
        'auth_openid_key_base64': manager.secret.get('auth_openid_key_base64'),
        # 'passport_rs_client_id': manager.config.get('passport_rs_client_id'),
        # 'passport_rs_client_base64_jwks': manager.secret.get('passport_rs_client_base64_jwks'),
        # 'passport_rs_client_cert_alias': manager.config.get('passport_rs_client_cert_alias'),
        # 'passport_rp_client_id': manager.config.get('passport_rp_client_id'),
        # 'passport_rp_client_base64_jwks': manager.secret.get('passport_rp_client_base64_jwks'),
        # "passport_rp_client_jks_fn": manager.config.get("passport_rp_client_jks_fn"),
        # "passport_rp_client_jks_pass": manager.secret.get("passport_rp_client_jks_pass"),
        # "encoded_ldap_pw": manager.secret.get('encoded_ldap_pw'),
        "encoded_admin_password": manager.secret.get('encoded_admin_password'),
        # 'passport_rp_ii_client_id': manager.config.get("passport_rp_ii_client_id"),

        'admin_email': manager.config.get('admin_email'),
        'shibJksFn': manager.config.get('shibJksFn'),
        'shibJksPass': manager.secret.get('shibJksPass'),
        'oxTrustConfigGeneration': str(as_boolean(oxtrust_config_generation)).lower(),
        'encoded_shib_jks_pw': manager.secret.get('encoded_shib_jks_pw'),
        'passport_rs_client_jks_fn': manager.config.get('passport_rs_client_jks_fn'),
        'passport_rs_client_jks_pass_encoded': manager.secret.get('passport_rs_client_jks_pass_encoded'),
        'shibboleth_version': manager.config.get('shibboleth_version'),
        'idp3Folder': manager.config.get('idp3Folder'),
        'ldap_site_binddn': manager.config.get('ldap_site_binddn'),

        "passport_resource_id": manager.config.get("passport_resource_id"),

        "gluu_radius_client_id": manager.config.get("gluu_radius_client_id"),
        "gluu_ro_encoded_pw": manager.secret.get("gluu_ro_encoded_pw"),
        # "super_gluu_ro_session_script": manager.config.get("super_gluu_ro_session_script"),
        # "super_gluu_ro_script": manager.config.get("super_gluu_ro_script"),
        # "enableRadiusScripts": "false",  # @TODO: enable it?
        # "gluu_ro_client_base64_jwks": manager.secret.get("gluu_ro_client_base64_jwks"),

        "jansPassportEnabled": str(as_boolean(passport_enabled)).lower(),
        "jansRadiusEnabled": str(as_boolean(radius_enabled)).lower(),
        "jansSamlEnabled": str(as_boolean(saml_enabled)).lower(),
        "jansScimEnabled": str(as_boolean(scim_enabled)).lower(),

        "pairwiseCalculationKey": manager.secret.get("pairwiseCalculationKey"),
        "pairwiseCalculationSalt": manager.secret.get("pairwiseCalculationSalt"),
        "default_openid_jks_dn_name": manager.config.get("default_openid_jks_dn_name"),
        "auth_openid_jks_pass": manager.secret.get("auth_openid_jks_pass"),
        "auth_legacyIdTokenClaims": manager.config.get("auth_legacyIdTokenClaims"),
        "passportSpTLSCert": manager.config.get("passportSpTLSCert"),
        "passportSpTLSKey": manager.config.get("passportSpTLSKey"),
        "auth_openidScopeBackwardCompatibility": manager.config.get("auth_openidScopeBackwardCompatibility"),
        "fido2ConfigFolder": manager.config.get("fido2ConfigFolder"),

        "admin_inum": manager.config.get("admin_inum"),
        "enable_scim_access_policy": str(as_boolean(scim_enabled) or as_boolean(passport_enabled)).lower(),
        "scim_client_id": manager.config.get("scim_client_id"),
        "scim_client_encoded_pw": manager.secret.get("scim_client_encoded_pw"),
        "casa_enable_script": str(as_boolean(casa_enabled)).lower(),
        "oxd_hostname": "localhost",
        "oxd_port": "8443",
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

    # finalize ctx
    return ctx


def merge_extension_ctx(ctx):
    basedir = "/app/static/extension"

    if os.environ.get("CN_DISTRIBUTION", "default") == "openbanking":
        basedir = "/app/static/ob_extension"

    for ext_type in os.listdir(basedir):
        ext_type_dir = os.path.join(basedir, ext_type)

        for fname in os.listdir(ext_type_dir):
            filepath = os.path.join(ext_type_dir, fname)
            ext_name = "{}_{}".format(
                ext_type, os.path.splitext(fname)[0].lower()
            )

            with open(filepath) as fd:
                ctx[ext_name] = generate_base64_contents(fd.read())
    return ctx


def merge_auth_ctx(ctx):
    basedir = '/app/templates/jans-auth'
    file_mappings = {
        # 'auth_config_base64': 'dynamic-conf.json',
        'auth_static_conf_base64': 'static-conf.json',
        'auth_error_base64': 'errors.json',
    }

    if os.environ.get("CN_DISTRIBUTION", "default") == "openbanking":
        file_mappings["auth_config_base64"] = "dynamic-conf.ob.json"
    else:
        file_mappings["auth_config_base64"] = "dynamic-conf.json"

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


# def merge_radius_ctx(ctx):
#     basedir = "/app/static/radius"
#     file_mappings = {
#         "super_gluu_ro_session_script": "super_gluu_ro_session.py",
#         "super_gluu_ro_script": "super_gluu_ro.py",
#     }
#
#     for key, file_ in file_mappings.items():
#         fn = os.path.join(basedir, file_)
#         with open(fn) as f:
#             ctx[key] = generate_base64_contents(f.read())
#     return ctx


# def merge_oxtrust_ctx(ctx):
#     basedir = '/app/templates/oxtrust'
#     file_mappings = {
#         'oxtrust_cache_refresh_base64': 'oxtrust-cache-refresh.json',
#         'oxtrust_config_base64': 'oxtrust-config.json',
#         'oxtrust_import_person_base64': 'oxtrust-import-person.json',
#     }
#
#     for key, file_ in file_mappings.items():
#         file_path = os.path.join(basedir, file_)
#         with open(file_path) as fp:
#             ctx[key] = generate_base64_contents(fp.read() % ctx)
#     return ctx


# def merge_oxidp_ctx(ctx):
#     basedir = '/app/templates/oxidp'
#     file_mappings = {
#         'oxidp_config_base64': 'oxidp-config.json',
#     }
#
#     for key, file_ in file_mappings.items():
#         file_path = os.path.join(basedir, file_)
#         with open(file_path) as fp:
#             ctx[key] = generate_base64_contents(fp.read() % ctx)
#     return ctx


# def merge_passport_ctx(ctx):
#     basedir = '/app/templates/passport'
#     file_mappings = {
#         'passport_central_config_base64': 'passport-central-config.json',
#     }
#
#     for key, file_ in file_mappings.items():
#         file_path = os.path.join(basedir, file_)
#         with open(file_path) as fp:
#             ctx[key] = generate_base64_contents(fp.read() % ctx)
#     return ctx


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

    local_ctx = {
        "apiApprovedIssuer": os.environ.get("CN_CONFIG_API_APPROVED_ISSUER") or f"https://{ctx['hostname']}",
        "apiProtectionType": "oauth2",
        "jca_client_id": ctx["jca_client_id"],
        "jca_client_encoded_pw": ctx["jca_client_encoded_pw"],
        "endpointInjectionEnabled": "true",
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


def prepare_template_ctx(manager):
    ctx = get_base_ctx(manager)
    ctx = merge_extension_ctx(ctx)
    # ctx = merge_radius_ctx(ctx)
    ctx = merge_auth_ctx(ctx)
    ctx = merge_config_api_ctx(ctx)
    # ctx = merge_oxtrust_ctx(ctx)
    # ctx = merge_oxidp_ctx(ctx)
    # ctx = merge_passport_ctx(ctx)
    ctx = merge_fido2_ctx(ctx)
    ctx = merge_scim_ctx(ctx)
    return ctx


def get_ldif_mappings(optional_scopes=None):
    optional_scopes = optional_scopes or []
    dist = os.environ.get("CN_DISTRIBUTION", "default")

    def default_files():
        files = [
            "base.ldif",
            "attributes.ldif",
            "jans-config-api/scopes.ldif",
        ]

        if dist == "openbanking":
            files += [
                "scopes.ob.ldif",
                "scripts.ob.ldif",
                "configuration.ob.ldif",
                # "jans-config-api/scopes.ob.ldif",
                "jans-config-api/clients.ob.ldif",
            ]
        else:
            files += [
                "scopes.ldif",
                "scripts.ldif",
                "configuration.ldif",
                "o_metric.ldif",
                # "jans-config-api/scopes.ldif",
                "jans-config-api/clients.ldif",
            ]

        files += [
            "jans-config-api/configuration.ldif",
            "jans-auth/configuration.ldif",
            "jans-auth/clients.ldif",
        ]

        if "scim" in optional_scopes:
            files += [
                "jans-scim/configuration.ldif",
                "jans-scim/scopes.ldif",
                "jans-scim/clients.ldif",
            ]

        if "fido2" in optional_scopes:
            files += [
                "jans-fido2/configuration.ldif",
            ]
        return files

    def user_files():
        files = []

        if dist != "openbanking":
            files += [
                "people.ldif",
                "groups.ldif",
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
