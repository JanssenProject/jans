import json
import logging.config
import os
import random
import socket
import time
from functools import partial
from uuid import uuid4

import click

from jans.pycloudlib import get_manager
from jans.pycloudlib import wait_for
from jans.pycloudlib.utils import get_random_chars
from jans.pycloudlib.utils import get_sys_random_chars
from jans.pycloudlib.utils import encode_text
from jans.pycloudlib.utils import exec_cmd
from jans.pycloudlib.utils import generate_base64_contents
from jans.pycloudlib.utils import safe_render
from jans.pycloudlib.utils import ldap_encode
from jans.pycloudlib.utils import get_server_certificate
from jans.pycloudlib.utils import generate_ssl_certkey
from jans.pycloudlib.utils import generate_ssl_ca_certkey
from jans.pycloudlib.utils import generate_signed_ssl_certkey
from jans.pycloudlib.utils import as_boolean

from parameter import params_from_file
from settings import LOGGING_CONFIG

DEFAULT_SIG_KEYS = "RS256 RS384 RS512 ES256 ES384 ES512 PS256 PS384 PS512"
DEFAULT_ENC_KEYS = "RSA1_5 RSA-OAEP ECDH-ES"

CONFIGURATOR_DIR = "/opt/jans/configurator"
DB_DIR = os.environ.get("CN_CONFIGURATOR_DB_DIR", f"{CONFIGURATOR_DIR}/db")
CERTS_DIR = os.environ.get("CN_CONFIGURATOR_CERTS_DIR", f"{CONFIGURATOR_DIR}/certs")
JAVALIBS_DIR = f"{CONFIGURATOR_DIR}/javalibs"

DEFAULT_CONFIG_FILE = os.environ.get("CN_CONFIGURATOR_CONFIG_FILE", f"{DB_DIR}/config.json")
DEFAULT_SECRET_FILE = os.environ.get("CN_CONFIGURATOR_SECRET_FILE", f"{DB_DIR}/secret.json")
DEFAULT_GENERATE_FILE = os.environ.get("CN_CONFIGURATOR_GENERATE_FILE", f"{DB_DIR}/generate.json")

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("configurator")

manager = get_manager()


def encode_template(fn, ctx, base_dir="/app/templates"):
    path = os.path.join(base_dir, fn)
    # ctx is nested which has `config` and `secret` keys
    data = {}
    for _, v in ctx.items():
        data.update(v)
    with open(path) as f:
        return generate_base64_contents(safe_render(f.read(), data))


def generate_openid_keys_hourly(passwd, jks_path, jwks_path, dn, exp=48, sig_keys=DEFAULT_SIG_KEYS, enc_keys=DEFAULT_ENC_KEYS):
    cmd = " ".join([
        "java",
        "-Dlog4j.defaultInitOverride=true",
        f"-cp {JAVALIBS_DIR}/*",
        "io.jans.as.client.util.KeyGenerator",
        f"-enc_keys {enc_keys}",
        f"-sig_keys {sig_keys}",
        f"-dnname {dn!r}",
        f"-expiration_hours {exp}",
        f"-keystore {jks_path}",
        f"-keypasswd {passwd}",
        "-key_ops_type all",
    ])
    out, err, retcode = exec_cmd(cmd)
    if retcode == 0:
        with open(jwks_path, "w") as f:
            f.write(out.decode())
    return out, err, retcode


def generate_pkcs12(suffix, passwd, hostname):
    # Convert key to pkcs12
    cmd = " ".join([
        "openssl",
        "pkcs12",
        "-export",
        f"-inkey {CERTS_DIR}/{suffix}.key",
        f"-in {CERTS_DIR}/{suffix}.crt",
        f"-out {CERTS_DIR}/{suffix}.pkcs12",
        f"-name {hostname}",
        f"-passout pass:{passwd}",
    ])
    _, err, retcode = exec_cmd(cmd)
    assert retcode == 0, f"Failed to generate PKCS12 file; reason={err}"


class CtxManager:
    def __init__(self, manager):
        self.manager = manager
        self.ctx = {"config": {}, "secret": {}}
        self._remote_config_ctx = None
        self._remote_secret_ctx = None

    @property
    def remote_config_ctx(self):
        if not self._remote_config_ctx:
            self._remote_config_ctx = self.manager.config.get_all()
        return self._remote_config_ctx

    @property
    def remote_secret_ctx(self):
        if not self._remote_secret_ctx:
            self._remote_secret_ctx = self.manager.secret.get_all()
        return self._remote_secret_ctx

    def set_config(self, key, value, reuse_if_exists=True):
        if reuse_if_exists and key in self.remote_config_ctx:
            logger.info(f"re-using config {key}")
            self.ctx["config"][key] = self.remote_config_ctx[key]
            return self.ctx["config"][key]

        logger.info(f"adding config {key}")
        if callable(value):
            value = value()

        self.ctx["config"][key] = value
        return self.ctx["config"][key]

    def set_secret(self, key, value, reuse_if_exists=True):
        if reuse_if_exists and key in self.remote_secret_ctx:
            logger.info(f"re-using secret {key}")
            self.ctx["secret"][key] = self.remote_secret_ctx[key]
            return self.ctx["secret"][key]

        logger.info(f"adding secret {key}")
        if callable(value):
            value = value()

        self.ctx["secret"][key] = value
        return value

    def get_config(self, key, default=None):
        return self.ctx["config"].get(key) or default

    def get_secret(self, key, default=None):
        return self.ctx["secret"].get(key) or default


class CtxGenerator:
    def __init__(self, manager, params):
        self.params = params
        self.manager = manager
        self.ctx_manager = CtxManager(self.manager)

    @property
    def ctx(self):
        return self.ctx_manager.ctx

    def set_config(self, key, value, reuse_if_exists=True):
        return self.ctx_manager.set_config(key, value, reuse_if_exists)

    def set_secret(self, key, value, reuse_if_exists=True):
        return self.ctx_manager.set_secret(key, value, reuse_if_exists)

    def get_config(self, key, default=None):
        return self.ctx_manager.get_config(key, default)

    def get_secret(self, key, default=None):
        return self.ctx_manager.get_secret(key, default)

    def base_ctx(self):
        if self.params["salt"]:
            self.set_secret("encoded_salt", self.params["salt"])
        else:
            self.set_secret("encoded_salt", partial(get_random_chars, 24))
        self.set_config("orgName", self.params["org_name"])
        self.set_config("country_code", self.params["country_code"])
        self.set_config("state", self.params["state"])
        self.set_config("city", self.params["city"])
        self.set_config("hostname", self.params["hostname"])
        self.set_config("admin_email", self.params["email"])
        # self.set_config("jetty_base", "/opt/jans/jetty")
        self.set_config("admin_inum", lambda: f"{uuid4()}")
        self.set_secret("encoded_admin_password", partial(ldap_encode, self.params["admin_pw"]))

        opt_scopes = self.params["optional_scopes"]
        self.set_config("optional_scopes", list(set(opt_scopes)), False)

    def ldap_ctx(self):
        encoded_salt = self.get_secret("encoded_salt")

        # self.set_secret("encoded_ldap_pw", ldap_encode(self.params["admin_pw"]))
        self.set_secret(
            "encoded_ox_ldap_pw",
            partial(encode_text, self.params["ldap_pw"], encoded_salt),
        )
        self.set_config("ldap_init_host", "localhost")
        self.set_config("ldap_init_port", 1636)
        self.set_config("ldap_port", 1389)
        self.set_config("ldaps_port", 1636)
        self.set_config("ldap_binddn", "cn=directory manager")
        self.set_config("ldap_site_binddn", "cn=directory manager")
        ldap_truststore_pass = self.set_secret("ldap_truststore_pass", get_random_chars)
        self.set_config("ldapTrustStoreFn", "/etc/certs/opendj.pkcs12")
        hostname = self.get_config("hostname")

        generate_ssl_certkey(
            "opendj",
            self.get_config("admin_email"),
            hostname,
            self.get_config("orgName"),
            self.get_config("country_code"),
            self.get_config("state"),
            self.get_config("city"),
            extra_dns=["ldap"],
            base_dir=CERTS_DIR,
        )
        with open(f"{CERTS_DIR}/opendj.pem", "w") as fw:
            with open(f"{CERTS_DIR}/opendj.crt") as fr:
                ldap_ssl_cert = fr.read()
                self.set_secret(
                    "ldap_ssl_cert",
                    partial(encode_text, ldap_ssl_cert, encoded_salt),
                )

            with open(f"{CERTS_DIR}/opendj.key") as fr:
                ldap_ssl_key = fr.read()
                self.set_secret(
                    "ldap_ssl_key",
                    partial(encode_text, ldap_ssl_key, encoded_salt),
                )

            ldap_ssl_cacert = "".join([ldap_ssl_cert, ldap_ssl_key])
            fw.write(ldap_ssl_cacert)
            self.set_secret(
                "ldap_ssl_cacert",
                partial(encode_text, ldap_ssl_cacert, encoded_salt),
            )

        generate_pkcs12("opendj", ldap_truststore_pass, hostname)

        with open(f"{CERTS_DIR}/opendj.pkcs12", "rb") as fr:
            self.set_secret(
                "ldap_pkcs12_base64",
                partial(encode_text, fr.read(), encoded_salt),
            )

        self.set_secret(
            "encoded_ldapTrustStorePass",
            partial(encode_text, ldap_truststore_pass, encoded_salt),
        )

    def redis_ctx(self):
        # TODO: move this to persistence-loader
        self.set_secret("redis_pw", self.params.get("redis_pw", ""))

    def auth_ctx(self):
        encoded_salt = self.get_secret("encoded_salt")

        self.set_config("default_openid_jks_dn_name", "CN=Janssen Auth CA Certificates")
        self.set_secret("pairwiseCalculationKey", partial(get_sys_random_chars, random.randint(20, 30)))
        self.set_secret("pairwiseCalculationSalt", partial(get_sys_random_chars, random.randint(20, 30)))

        self.set_config("auth_openid_jks_fn", "/etc/certs/auth-keys.jks")
        self.set_secret("auth_openid_jks_pass", get_random_chars)
        self.set_config("auth_openid_jwks_fn", "/etc/certs/auth-keys.json")
        self.set_config("auth_legacyIdTokenClaims", "false")
        self.set_config("auth_openidScopeBackwardCompatibility", "false")

        # get user-input signing keys
        allowed_sig_keys = DEFAULT_SIG_KEYS.split()
        sig_keys = []

        for k in self.params.get("auth_sig_keys", "").split():
            k = k.strip()
            if k not in allowed_sig_keys:
                continue
            sig_keys.append(k)

        # if empty, fallback to default
        sig_keys = sig_keys or allowed_sig_keys
        sig_keys = " ".join(sig_keys)
        self.set_config("auth_sig_keys", sig_keys)

        # get user-input encryption keys
        allowed_enc_keys = DEFAULT_ENC_KEYS.split()
        enc_keys = []

        for k in self.params.get("auth_enc_keys", "").split():
            k = k.strip()
            if k not in allowed_enc_keys:
                continue
            enc_keys.append(k)

        # if empty, fallback to default
        enc_keys = enc_keys or allowed_enc_keys
        enc_keys = " ".join(enc_keys)
        self.set_config("auth_enc_keys", enc_keys)

        # default exp = 48 hours + token lifetime (in hour)
        exp = int(self.params["init_keys_exp"] + (3600 / 3600))

        _, err, retcode = generate_openid_keys_hourly(
            self.get_secret("auth_openid_jks_pass"),
            f"{CERTS_DIR}/auth-keys.jks",
            f"{CERTS_DIR}/auth-keys.json",
            self.get_config("default_openid_jks_dn_name"),
            exp=exp,
            sig_keys=sig_keys,
            enc_keys=enc_keys,
        )
        if retcode != 0:
            logger.error(f"Unable to generate auth keys; reason={err}")
            raise click.Abort()

        self.set_secret(
            "auth_openid_key_base64",
            partial(encode_template, "auth-keys.json", self.ctx, CERTS_DIR),
        )

        # auth keys
        self.set_config("auth_key_rotated_at", lambda: int(time.time()))

        with open(f"{CERTS_DIR}/auth-keys.jks", "rb") as fr:
            self.set_secret(
                "auth_jks_base64",
                partial(encode_text, fr.read(), encoded_salt),
            )

    def web_ctx(self):
        ssl_cert = f"{CERTS_DIR}/web_https.crt"
        ssl_key = f"{CERTS_DIR}/web_https.key"
        ssl_csr = f"{CERTS_DIR}/web_https.csr"

        ssl_ca_cert = f"{CERTS_DIR}/ca.crt"
        ssl_ca_key = f"{CERTS_DIR}/ca.key"

        # get cert and key (if available) with priorities below:
        #
        # 1. from mounted files
        # 2. from fronted (key file is an empty file)
        # 3. self-generate files

        logger.info(f"Resolving {ssl_cert} and {ssl_key}")

        # check from mounted files
        if not (os.path.isfile(ssl_cert) and os.path.isfile(ssl_key)):
            # no mounted files, hence download from frontend
            ingress_addr = ""
            if "CN_INGRESS_ADDRESS" in os.environ:
                ingress_addr = os.environ.get("CN_INGRESS_ADDRESS")
            ingress_servername = os.environ.get("CN_INGRESS_SERVERNAME") or ingress_addr

            if ingress_addr and ingress_servername:
                logger.warning(
                    f"Unable to find mounted {ssl_cert} and {ssl_key}; "
                    f"trying to download from {ingress_addr}:443 (servername {ingress_servername})"  # noqa: C812
                )

                try:
                    # cert will be downloaded into `ssl_cert` path
                    get_server_certificate(ingress_addr, 443, ssl_cert, ingress_servername)
                    # since cert is downloaded, key must mounted
                    # or generate empty file
                    if not os.path.isfile(ssl_key):
                        with open(ssl_key, "w") as f:
                            f.write("")
                except (socket.gaierror, socket.timeout, OSError) as exc:
                    # address not resolved or timed out
                    logger.warning(f"Unable to download cert; reason={exc}")

        # no mounted nor downloaded files, hence we need to create self-generated files
        if not (os.path.isfile(ssl_cert) and os.path.isfile(ssl_key)):
            hostname = self.get_config("hostname")
            email = self.get_config("admin_email")
            org_name = self.get_config("orgName")
            country_code = self.get_config("country_code")
            state = self.get_config("state")
            city = self.get_config("city")

            logger.info(f"Creating self-generated {ssl_ca_cert} and {ssl_ca_key}")

            ca_cert, ca_key = generate_ssl_ca_certkey(
                "ca",
                email,
                "Janssen CA",
                org_name,
                country_code,
                state,
                city,
                base_dir=CERTS_DIR,
            )

            logger.info(f"Creating self-generated {ssl_csr}, {ssl_cert}, and {ssl_key}")
            generate_signed_ssl_certkey(
                "web_https",
                ca_key,
                ca_cert,
                email,
                hostname,
                org_name,
                country_code,
                state,
                city,
                base_dir=CERTS_DIR,
            )

        try:
            with open(ssl_ca_cert) as f:
                self.set_secret("ssl_ca_cert", f.read)
        except FileNotFoundError:
            self.set_secret("ssl_ca_cert", "")

        try:
            with open(ssl_ca_key) as f:
                self.set_secret("ssl_ca_key", f.read)
        except FileNotFoundError:
            self.set_secret("ssl_ca_key", "")

        try:
            with open(ssl_csr) as f:
                self.set_secret("ssl_csr", f.read)
        except FileNotFoundError:
            self.set_secret("ssl_csr", "")

        with open(ssl_cert) as f:
            self.set_secret("ssl_cert", f.read)

        with open(ssl_key) as f:
            self.set_secret("ssl_key", f.read)

    def couchbase_ctx(self):
        # TODO: move this to persistence-loader?
        self.set_config("couchbaseTrustStoreFn", "/etc/certs/couchbase.pkcs12")
        self.set_secret("couchbase_shib_user_password", get_random_chars)
        self.set_secret("couchbase_password", self.params["couchbase_pw"])
        self.set_secret("couchbase_superuser_password", self.params["couchbase_superuser_pw"])

    def sql_ctx(self):
        self.set_secret("sql_password", self.params["sql_pw"])

    def generate(self):
        opt_scopes = self.params["optional_scopes"]

        self.base_ctx()
        self.auth_ctx()
        self.web_ctx()

        # if "ldap" in opt_scopes:
        #     self.ldap_ctx()

        if "redis" in opt_scopes:
            self.redis_ctx()

        # if "couchbase" in opt_scopes:
        #     self.couchbase_ctx()

        # if "sql" in opt_scopes:
        #     self.sql_ctx()

        # populated config
        return self.ctx


def _save_generated_ctx(manager, data, type_):
    if type_ == "config":
        backend = manager.config
    else:
        backend = manager.secret

    logger.info(f"Saving {type_} to backend")
    backend.set_all(data)


def _load_from_file(manager, filepath, type_):
    ctx_manager = CtxManager(manager)
    if type_ == "config":
        setter = ctx_manager.set_config
        backend = manager.config
    else:
        setter = ctx_manager.set_secret
        backend = manager.secret

    logger.info(f"Loading {type_} from {filepath}")

    with open(filepath, "r") as f:
        data = json.loads(f.read())

    ctx = data.get(f"_{type_}")
    if not ctx:
        logger.warning(f"Missing '_{type_}' key")
        return

    # tolerancy before checking existing key
    time.sleep(5)

    data = {k: setter(k, v) for k, v in ctx.items()}
    backend.set_all(data)


def _dump_to_file(manager, filepath, type_):
    if type_ == "config":
        backend = manager.config
    else:
        backend = manager.secret

    logger.info(f"Saving {type_} to {filepath}")

    data = {f"_{type_}": backend.get_all()}
    data = json.dumps(data, sort_keys=True, indent=4)
    with open(filepath, "w") as f:
        f.write(data)

# ============
# CLI commands
# ============


@click.group()
def cli():
    pass


@cli.command()
@click.option(
    "--generate-file",
    type=click.Path(exists=False),
    help="Absolute path to file containing parameters for generating config and secret",
    default=DEFAULT_GENERATE_FILE,
    show_default=True,
)
@click.option(
    "--config-file",
    type=click.Path(exists=False),
    help="Absolute path to file contains config",
    default=DEFAULT_CONFIG_FILE,
    show_default=True,
)
@click.option(
    "--secret-file",
    type=click.Path(exists=False),
    help="Absolute path to file contains secret",
    default=DEFAULT_SECRET_FILE,
    show_default=True,
)
def load(generate_file, config_file, secret_file):
    """Loads config and secret from JSON files (generate if not exist).
    """
    deps = ["config_conn", "secret_conn"]
    wait_for(manager, deps=deps)

    # check whether config and secret in backend have been initialized
    should_skip = as_boolean(os.environ.get("CN_CONFIGURATION_SKIP_INITIALIZED", False))
    if should_skip and manager.config.get("hostname") and manager.secret.get("ssl_cert"):
        # config and secret may have been initialized
        logger.info("Config and secret have been initialized")
        return

    with manager.lock.create_lock("configurator-load"):
        # there's no config and secret in backend, check whether to load from files
        if os.path.isfile(config_file) and os.path.isfile(secret_file):
            # load from existing files
            logger.info(f"Re-using config and secret from {config_file} and {secret_file}")
            _load_from_file(manager, config_file, "config")
            _load_from_file(manager, secret_file, "secret")
            return

        # no existing files, hence generate new config and secret from parameters
        logger.info(f"Loading parameters from {generate_file}")
        params, err, code = params_from_file(generate_file)
        if code != 0:
            logger.error(f"Unable to load parameters; reason={err}")
            raise click.Abort()

        logger.info("Generating new config and secret")
        ctx_generator = CtxGenerator(manager, params)
        ctx = ctx_generator.generate()

        # save config to its backend and file
        _save_generated_ctx(manager, ctx["config"], "config")
        _dump_to_file(manager, config_file, "config")

        # save secret to its backend and file
        _save_generated_ctx(manager, ctx["secret"], "secret")
        _dump_to_file(manager, secret_file, "secret")


@cli.command()
@click.option(
    "--config-file",
    type=click.Path(exists=False),
    help="Absolute path to file to save config",
    default=DEFAULT_CONFIG_FILE,
    show_default=True,
)
@click.option(
    "--secret-file",
    type=click.Path(exists=False),
    help="Absolute path to file to save secret",
    default=DEFAULT_SECRET_FILE,
    show_default=True,
)
def dump(config_file, secret_file):
    """Dumps config and secret into JSON files.
    """
    deps = ["config_conn", "secret_conn"]
    wait_for(manager, deps=deps)

    _dump_to_file(manager, config_file, "config")
    _dump_to_file(manager, secret_file, "secret")


if __name__ == "__main__":
    cli(prog_name="configurator")
