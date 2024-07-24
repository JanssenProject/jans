import contextlib
import json
import os
import typing as _t
from pathlib import Path
from uuid import uuid4

from jans.pycloudlib.utils import as_boolean
from jans.pycloudlib.utils import encode_text
from jans.pycloudlib.utils import safe_render
from jans.pycloudlib.utils import generate_base64_contents
from jans.pycloudlib.utils import get_random_chars
from jans.pycloudlib.utils import exec_cmd

from hooks import merge_auth_keystore_ctx_hook


def render_ldif(src, dst, ctx):
    with open(src) as f:
        txt = f.read()

    with open(dst, "w") as f:
        f.write(safe_render(txt, ctx))


def get_jackrabbit_creds():
    username = os.environ.get("CN_JACKRABBIT_ADMIN_ID", "admin")
    password = ""  # nosec: B105

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
    redis_pw = manager.secret.get("redis_password") or ""
    redis_pw_encoded = ""

    if redis_pw:
        redis_pw_encoded = encode_text(
            redis_pw,
            manager.secret.get("encoded_salt"),
        ).decode()

    doc_store_type = os.environ.get("CN_DOCUMENT_STORE_TYPE", "DB")
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
    scim_enabled = os.environ.get("CN_SCIM_ENABLED", False)

    # finalize ctx
    return {
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
        'oxauth_openid_key_base64': manager.secret.get('auth_openid_key_base64'),
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
        # maintain compatibility with upstream template
        "oxauth_openid_jks_pass": manager.secret.get("auth_openid_jks_pass"),
        "auth_legacyIdTokenClaims": manager.config.get("auth_legacyIdTokenClaims"),
        "auth_openidScopeBackwardCompatibility": manager.config.get("auth_openidScopeBackwardCompatibility"),

        "admin_inum": manager.config.get("admin_inum"),
        "jca_client_id": manager.config.get("jca_client_id"),
        "jca_client_encoded_pw": manager.secret.get("jca_client_encoded_pw"),
    }


def merge_extension_ctx(ctx: dict[str, _t.Any]) -> dict[str, _t.Any]:
    """Merge extension script contexts into given contexts.

    :param ctx: A key-value pairs of existing contexts.
    :returns: Merged contexts.
    """
    basedirs = ["/app/static/extension", "/app/script-catalog"]

    for basedir in basedirs:
        filepath = Path(basedir)
        for ext_path in filepath.glob("**/*"):
            if not ext_path.is_file() or ext_path.suffix.lower() not in (".py", ".java"):
                continue

            ext_type = ext_path.relative_to(filepath).parent.as_posix().lower().replace(os.path.sep, "_").replace("-", "_")
            ext_name = ext_path.stem.lower()
            script_name = f"{ext_type}_{ext_name}"
            ctx[script_name] = generate_base64_contents(ext_path.read_text())
    return ctx


def merge_auth_ctx(ctx):
    if os.environ.get("CN_PERSISTENCE_TYPE") in ("sql", "spanner"):
        ctx["person_custom_object_class_list"] = "[]"
    else:
        ctx["person_custom_object_class_list"] = '["jansCustomPerson", "jansPerson"]'

    basedir = '/app/templates/jans-auth'
    file_mappings = {
        'oxauth_static_conf_base64': 'jans-auth-static-conf.json',
        'oxauth_error_base64': 'jans-auth-errors.json',
        "oxauth_config_base64": "jans-auth-config.json",
    }

    for key, file_ in file_mappings.items():
        file_path = os.path.join(basedir, file_)
        with open(file_path) as fp:
            ctx[key] = generate_base64_contents(fp.read() % ctx)

    # determine role scope mappings
    ctx["role_scope_mappings"] = json.dumps(get_role_scope_mappings())
    return ctx


def merge_jans_cli_ctx(manager, ctx):
    # jans-cli-tui client
    ctx["tui_client_id"] = manager.config.get("tui_client_id")
    if not ctx["tui_client_id"]:
        # migrate from old configs/secrets (if any)
        ctx["tui_client_id"] = manager.config.get("role_based_client_id", f"2000.{uuid4()}")
        manager.config.set("tui_client_id", ctx["tui_client_id"])

    ctx["tui_client_pw"] = manager.secret.get("tui_client_pw")
    if not ctx["tui_client_pw"]:
        # migrate from old configs/secrets (if any)
        ctx["tui_client_pw"] = manager.secret.get("role_based_client_pw", get_random_chars())
        manager.secret.set("tui_client_pw", ctx["tui_client_pw"])

    ctx["tui_client_encoded_pw"] = manager.secret.get("tui_client_encoded_pw")
    if not ctx["tui_client_encoded_pw"]:
        # migrate from old configs/secrets (if any)
        ctx["tui_client_encoded_pw"] = manager.secret.get(
            "role_based_client_encoded_pw",
            encode_text(ctx["tui_client_pw"], manager.secret.get("encoded_salt")).decode(),
        )
        manager.secret.set("tui_client_encoded_pw", ctx["tui_client_encoded_pw"])
    return ctx


def prepare_template_ctx(manager):
    ctx = get_base_ctx(manager)
    ctx = merge_extension_ctx(ctx)
    ctx = merge_auth_keystore_ctx_hook(manager, ctx)
    ctx = merge_auth_ctx(ctx)
    ctx = merge_jans_cli_ctx(manager, ctx)
    ctx = merge_smtp_ctx(manager, ctx)
    return ctx  # noqa: R504


def get_config_api_scopes(path="/app/static/config-api-rs-protect.json"):
    scopes = []

    with open(path) as f:
        scope_defs = json.loads(f.read())

    for resource in scope_defs["resources"]:
        for condition in resource["conditions"]:
            scopes += [
                scope["name"]
                for scope in condition["scopes"]
                if scope.get("inum") and scope.get("name")
            ]

    # ensure no duplicates and sorted
    return sorted(set(scopes))


def get_role_scope_mappings(path="/app/templates/jans-auth/role-scope-mappings.json"):
    with open(path) as f:
        role_mapping = json.loads(f.read())

    scope_list = get_config_api_scopes()

    for i, api_role in enumerate(role_mapping["rolePermissionMapping"]):
        if api_role["role"] != "api-admin":
            continue

        # add special permissions for api-admin
        for scope in scope_list:
            if scope in role_mapping["rolePermissionMapping"][i]["permissions"]:
                continue
            role_mapping["rolePermissionMapping"][i]["permissions"].append(scope)

    # finalized role mapping
    return role_mapping


def merge_smtp_ctx(manager, ctx):
    encoded_salt = manager.secret.get("encoded_salt")
    jks_fn = "/etc/certs/smtp-keys.pkcs12"

    smtp_jks_pass = manager.secret.get("smtp_jks_pass")
    if not smtp_jks_pass:
        smtp_jks_pass = get_random_chars()
        manager.secret.set("smtp_jks_pass", smtp_jks_pass)

    smtp_jks_pass_enc = manager.secret.get("smtp_jks_pass_enc")
    if not smtp_jks_pass_enc:
        smtp_jks_pass_enc = encode_text(smtp_jks_pass, encoded_salt).decode()
        manager.secret.set("smtp_jks_pass_enc", smtp_jks_pass_enc)

    smtp_signing_alg = manager.config.get("smtp_signing_alg")
    if not smtp_signing_alg:
        smtp_signing_alg = "SHA256withECDSA"
        manager.config.set("smtp_signing_alg", smtp_signing_alg)

    smtp_alias = manager.config.get("smtp_alias")
    if not smtp_alias:
        smtp_alias = "smtp_sig_ec256"
        manager.config.set("smtp_alias", smtp_alias)

    smtp_jks_base64 = manager.secret.get("smtp_jks_base64")
    if not smtp_jks_base64:
        cmds = " ".join([
            "keytool",
            "-genkeypair",
            "-alias", smtp_alias,
            "-keyalg", "ec",
            "-groupname", "secp256r1",
            "-sigalg", smtp_signing_alg,
            "-storetype", "pkcs12",
            "-keystore", jks_fn,
            "-storepass", smtp_jks_pass,
            "-dname", "'CN=SMTP CA Certificate'",
            "-validity", "365",
        ])
        _, err, retcode = exec_cmd(cmds)
        assert retcode == 0, "Failed to generate JKS keystore; reason={}".format(err.decode())

        with open(jks_fn, "rb") as fr:
            manager.secret.set("smtp_jks_base64", encode_text(fr.read(), encoded_salt))

    smtp_ctx = {
        "smtp_jks_fn": jks_fn,
        "smtp_jks_pass": smtp_jks_pass,
        "smtp_alias": smtp_alias,
        "smtp_signing_alg": smtp_signing_alg,
        "smtp_jks_pass_enc": smtp_jks_pass_enc,
    }
    ctx.update(smtp_ctx)
    return ctx
