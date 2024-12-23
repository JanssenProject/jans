import json
import logging.config
import os
import random
import ssl
import socket
import time
from functools import partial
from pathlib import Path
from uuid import uuid4

import click
from cryptography import x509
from cryptography.x509.oid import NameOID

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
from jans.pycloudlib.utils import generate_ssl_ca_certkey
from jans.pycloudlib.utils import generate_signed_ssl_certkey
from jans.pycloudlib.utils import as_boolean
from jans.pycloudlib.schema import load_schema_from_file

from settings import LOGGING_CONFIG

DEFAULT_SIG_KEYS = "RS256 RS384 RS512 ES256 ES384 ES512 PS256 PS384 PS512"
DEFAULT_ENC_KEYS = "RSA1_5 RSA-OAEP ECDH-ES"

CONFIGURATOR_DIR = "/opt/jans/configurator"
DB_DIR = os.environ.get("CN_CONFIGURATOR_DB_DIR", f"{CONFIGURATOR_DIR}/db")
CERTS_DIR = os.environ.get("CN_CONFIGURATOR_CERTS_DIR", f"{CONFIGURATOR_DIR}/certs")
JAVALIBS_DIR = f"{CONFIGURATOR_DIR}/javalibs"

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
        self.ctx = {"_configmap": {}, "_secret": {}}
        self._remote_config_ctx = {}
        self._remote_secret_ctx = {}

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
            logger.info(f"re-using configmap {key!r}")
            self.ctx["_configmap"][key] = self.remote_config_ctx[key]
            return self.ctx["_configmap"][key]

        logger.info(f"adding configmap {key!r}")
        if callable(value):
            value = value()

        if isinstance(value, bytes):
            value = value.decode()

        self.ctx["_configmap"][key] = value
        return self.ctx["_configmap"][key]

    def set_secret(self, key, value, reuse_if_exists=True):
        if reuse_if_exists and key in self.remote_secret_ctx:
            logger.info(f"re-using secret {key!r}")
            self.ctx["_secret"][key] = self.remote_secret_ctx[key]
            return self.ctx["_secret"][key]

        logger.info(f"adding secret {key!r}")
        if callable(value):
            value = value()

        if isinstance(value, bytes):
            value = value.decode()

        self.ctx["_secret"][key] = value
        return value

    def get_config(self, key, default=None):
        return self.ctx["_configmap"].get(key) or default

    def get_secret(self, key, default=None):
        return self.ctx["_secret"].get(key) or default


class CtxGenerator:
    def __init__(self, manager, params):
        self.configmap_params = params["_configmap"]
        self.secret_params = params["_secret"]
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

    def transform_base_ctx(self):
        if self.secret_params.get("encoded_salt"):
            self.set_secret("encoded_salt", self.secret_params.get("encoded_salt"))
        else:
            self.set_secret("encoded_salt", partial(get_random_chars, 24))

        self.set_config("orgName", self.configmap_params["orgName"])
        self.set_config("country_code", self.configmap_params["country_code"])
        self.set_config("state", self.configmap_params["state"])
        self.set_config("city", self.configmap_params["city"])
        self.set_config("hostname", self.configmap_params["hostname"])
        self.set_config("admin_email", self.configmap_params["admin_email"])
        self.set_config("admin_inum", lambda: f"{uuid4()}")
        self.set_secret("encoded_admin_password", partial(ldap_encode, self.secret_params["admin_password"]))

        opt_scopes = self.configmap_params["optional_scopes"]
        self.set_config("optional_scopes", opt_scopes, False)

    def transform_redis_ctx(self):
        self.set_secret("redis_password", self.secret_params.get("redis_password", ""))

    def transform_auth_ctx(self):
        encoded_salt = self.get_secret("encoded_salt")

        self.set_config("default_openid_jks_dn_name", "CN=Janssen Auth CA Certificates")
        self.set_secret("pairwiseCalculationKey", partial(get_sys_random_chars, random.randint(20, 30)))
        self.set_secret("pairwiseCalculationSalt", partial(get_sys_random_chars, random.randint(20, 30)))

        self.set_config("auth_openid_jks_fn", "/etc/certs/auth-keys.jks")
        self.set_secret("auth_openid_jks_pass", get_random_chars)
        self.set_config("auth_openid_jwks_fn", "/etc/certs/auth-keys.json")
        self.set_config("auth_legacyIdTokenClaims", "false")
        self.set_config("auth_openidScopeBackwardCompatibility", "false")

        self.set_config("auth_sig_keys", self.configmap_params["auth_sig_keys"])
        self.set_config("auth_enc_keys", self.configmap_params["auth_enc_keys"])

        # default exp = 48 hours + token lifetime (in hour)
        exp = int(self.configmap_params["init_keys_exp"] + (3600 / 3600))

        _, err, retcode = generate_openid_keys_hourly(
            self.get_secret("auth_openid_jks_pass"),
            f"{CERTS_DIR}/auth-keys.jks",
            f"{CERTS_DIR}/auth-keys.json",
            self.get_config("default_openid_jks_dn_name"),
            exp=exp,
            sig_keys=self.configmap_params["auth_sig_keys"],
            enc_keys=self.configmap_params["auth_enc_keys"],
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

    def transform_web_ctx(self):
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
            addr = os.environ.get("CN_INGRESS_ADDRESS") or self.ctx["_configmap"]["hostname"]
            servername = os.environ.get("CN_INGRESS_SERVERNAME") or addr

            logger.warning(
                f"Unable to find mounted {ssl_cert} and {ssl_key}; "
                f"trying to download from {addr}:443 (servername {servername})"
            )
            cert_from_domain(addr, servername, 443, ssl_cert, ssl_key, self.ctx["_configmap"]["hostname"])

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

    def transform_sql_ctx(self):
        self.set_secret("sql_password", self.secret_params.get("sql_password", ""))

    def transform_misc_ctx(self):
        # pre-populate the rest of configmaps
        for k, v in self.configmap_params.items():
            if v and k not in self.ctx["_configmap"]:
                self.set_config(k, v)

        # pre-populate the rest of secrets
        for k, v in self.secret_params.items():
            if v and k not in self.ctx["_secret"]:
                self.set_secret(k, v)

    def transform(self):
        """Transform configmaps and secrets (if needed)."""
        opt_scopes = json.loads(self.configmap_params["optional_scopes"])

        self.transform_base_ctx()
        self.transform_auth_ctx()
        self.transform_web_ctx()

        if "redis" in opt_scopes:
            self.transform_redis_ctx()

        if "sql" in opt_scopes:
            self.transform_sql_ctx()

        self.transform_misc_ctx()

        # populated configuration
        return self.ctx

    def save_loaded_ctx(self):
        logger.info("Saving configuration to backends")

        for type_ in ["_configmap", "_secret"]:
            if type_ == "_configmap":
                backend = self.manager.config
            else:
                backend = self.manager.secret
            backend.set_all(self.ctx[type_])


def dump_to_file(manager, filepath):
    logger.info(f"Saving configuration to {filepath}")

    data = {"_configmap": {}, "_secret": {}}

    for type_ in ["_configmap", "_secret"]:
        if type_ == "_configmap":
            backend = manager.config
        else:
            backend = manager.secret
        data[type_] = backend.get_all()

    with open(filepath, "w") as f:
        f.write(json.dumps(data, sort_keys=True, indent=4))


def cert_from_domain(addr, servername, port, certfile, keyfile, dns):
    known_exceptions = (
        socket.gaierror,
        socket.timeout,
        ConnectionRefusedError,
        TimeoutError,
        ConnectionResetError,
        ssl.SSLEOFError,
        ssl.SSLError,
        OSError,
    )
    cert_downloaded = False

    try:
        # cert will be downloaded into `ssl_cert` path
        get_server_certificate(addr, port, certfile, servername)
        is_cert_valid = parse_cert(certfile, dns)

        if not is_cert_valid:
            logger.warning(f"The domain {dns} cannot be found in certificate SubjectAlternativeName or CommonName.")
            Path(certfile).unlink(missing_ok=True)
        else:
            cert_downloaded = True
            if not os.path.isfile(keyfile):
                # since cert is downloaded, key must be mounted or simply generate empty file
                with open(keyfile, "w") as f:
                    f.write("")

    except known_exceptions as exc:
        # common error message on cert download attempt
        logger.warning(
            f"Unable to download SSL cert from {addr}. The certificate maybe missing "
            f"or another issue encountered while trying to download the cert; reason={exc}."
        )

    env_name = "CN_SSL_CERT_FROM_DOMAIN"
    if not cert_downloaded and as_boolean(os.environ.get(env_name, "false")):
        raise RuntimeError(
            f"Exiting the process due to the environment variable {env_name} is set to true. "
            f"To skip this error, set the environment variable {env_name} to false."
        )


def parse_cert(certfile, dns):
    with open(certfile) as f:
        pem_data = f.read()

    cert = x509.load_pem_x509_certificate(pem_data.encode())

    # check for DNS in SAN
    try:
        san = cert.extensions.get_extension_for_class(x509.SubjectAlternativeName)
    except x509.extensions.ExtensionNotFound:
        san = None

    # check whether dns is in SAN
    if san and dns in san.value.get_values_for_type(x509.DNSName):
        # DNS is found and matched
        return True

    # check CommonName in subject
    common_names = [name.value for name in cert.subject.get_attributes_for_oid(NameOID.COMMON_NAME)]
    if dns in common_names:
        return True

    # default value
    return False


def get_configuration_file():
    path = os.environ.get("CN_CONFIGURATOR_CONFIGURATION_FILE", "/etc/jans/conf/configuration.json")

    if os.path.isfile(path):
        return path

    # backward-compat
    return f"{DB_DIR}/configuration.json"


def get_dump_file():
    path = os.environ.get("CN_CONFIGURATOR_DUMP_FILE", "/etc/jans/conf/configuration.out.json")

    if os.path.isfile(path):
        return path

    # backward-compat
    return f"{DB_DIR}/configuration.out.json"


# ============
# CLI commands
# ============


@click.group()
def cli():
    pass


@cli.command()
@click.option(
    "--configuration-file",
    type=click.Path(exists=False),
    help="Absolute path to file contains configmaps and secrets",
    default=get_configuration_file(),
    show_default=True,
)
@click.option(
    "--dump-file",
    type=click.Path(exists=False),
    help="Absolute path to file contains dumped configmaps and secrets",
    default=get_dump_file(),
    show_default=True,
)
def load(configuration_file, dump_file):
    """Loads configmaps and secrets from JSON file (generate if not exist).
    """
    deps = ["config_conn", "secret_conn"]
    wait_for(manager, deps=deps)

    # check whether config and secret in backend have been initialized
    should_skip = as_boolean(os.environ.get("CN_CONFIGURATOR_SKIP_INITIALIZED", False))
    if should_skip and manager.config.get("hostname") and manager.secret.get("ssl_cert"):
        # config and secret may have been initialized
        logger.info("Configmaps and secrets have been initialized")
        return

    with manager.create_lock("configurator-load"):
        logger.info(f"Loading configmaps and secrets from {configuration_file}")

        params, err, code = load_schema_from_file(configuration_file)
        if code != 0:
            logger.error(f"Unable to load configmaps and secrets; reason={err}")
            raise click.Abort()

        ctx_generator = CtxGenerator(manager, params)
        ctx_generator.transform()
        ctx_generator.save_loaded_ctx()

        # dump saved configuration to file
        dump_to_file(manager, dump_file)


@cli.command()
@click.option(
    "--dump-file",
    type=click.Path(exists=False),
    help="Absolute path to file contains dumped configmaps and secrets",
    default=get_dump_file(),
    show_default=True,
)
def dump(dump_file):
    """Dumps configmaps and secrets into JSON files.
    """
    deps = ["config_conn", "secret_conn"]
    wait_for(manager, deps=deps)

    # dump all configuration from remote backend to file
    dump_to_file(manager, dump_file)


if __name__ == "__main__":
    cli(prog_name="configurator")
