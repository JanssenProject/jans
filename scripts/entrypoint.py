import json
import logging.config
import os
import random
import socket
import time
import uuid

import click

from jans.pycloudlib import get_manager
from jans.pycloudlib import wait_for
from jans.pycloudlib.utils import get_random_chars
from jans.pycloudlib.utils import get_sys_random_chars
from jans.pycloudlib.utils import encode_text
from jans.pycloudlib.utils import exec_cmd
from jans.pycloudlib.utils import as_boolean
from jans.pycloudlib.utils import generate_base64_contents
from jans.pycloudlib.utils import safe_render
from jans.pycloudlib.utils import ldap_encode
from jans.pycloudlib.utils import get_server_certificate

from parameter import params_from_file
from settings import LOGGING_CONFIG

DEFAULT_SIG_KEYS = "RS256 RS384 RS512 ES256 ES384 ES512"
DEFAULT_ENC_KEYS = DEFAULT_SIG_KEYS

DEFAULT_CONFIG_FILE = "/app/db/config.json"
DEFAULT_SECRET_FILE = "/app/db/secret.json"
DEFAULT_GENERATE_FILE = "/app/db/generate.json"

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("config-init")

manager = get_manager()


def encode_template(fn, ctx, base_dir="/app/templates"):
    path = os.path.join(base_dir, fn)
    # ctx is nested which has `config` and `secret` keys
    data = {}
    for _, v in ctx.items():
        data.update(v)
    with open(path) as f:
        return generate_base64_contents(safe_render(f.read(), data))


def generate_openid_keys(passwd, jks_path, jwks_path, dn, exp=365, sig_keys=DEFAULT_SIG_KEYS, enc_keys=DEFAULT_ENC_KEYS):
    cmd = " ".join([
        "java",
        "-Dlog4j.defaultInitOverride=true",
        "-jar", "/app/javalibs/oxauth-client.jar",
        "-enc_keys", enc_keys,
        "-sig_keys", sig_keys,
        "-dnname", "{!r}".format(dn),
        "-expiration", "{}".format(exp),
        "-keystore", jks_path,
        "-keypasswd", passwd,
    ])
    out, err, retcode = exec_cmd(cmd)
    if retcode == 0:
        with open(jwks_path, "w") as f:
            f.write(out.decode())
    return out, err, retcode


def export_openid_keys(keystore, keypasswd, alias, export_file):
    cmd = " ".join([
        "java",
        "-Dlog4j.defaultInitOverride=true",
        "-cp /app/javalibs/oxauth-client.jar",
        "org.gluu.oxauth.util.KeyExporter",
        "-keystore {}".format(keystore),
        "-keypasswd {}".format(keypasswd),
        "-alias {}".format(alias),
        "-exportfile {}".format(export_file),
    ])
    return exec_cmd(cmd)


def generate_pkcs12(suffix, passwd, hostname):
    # Convert key to pkcs12
    cmd = " ".join([
        "openssl",
        "pkcs12",
        "-export",
        "-inkey /etc/certs/{}.key".format(suffix),
        "-in /etc/certs/{}.crt".format(suffix),
        "-out /etc/certs/{}.pkcs12".format(suffix),
        "-name {}".format(hostname),
        "-passout pass:{}".format(passwd),
    ])
    _, err, retcode = exec_cmd(cmd)
    assert retcode == 0, "Failed to generate PKCS12 file; reason={}".format(err)


class CtxManager(object):
    def __init__(self, manager):
        self.manager = manager
        self.ctx = {"config": {}, "secret": {}}
        self._remote_config_ctx = None
        self._remote_secret_ctx = None

    @property
    def remote_config_ctx(self):
        if not self._remote_config_ctx:
            self._remote_config_ctx = self.manager.config.all()
        return self._remote_config_ctx

    @property
    def remote_secret_ctx(self):
        if not self._remote_secret_ctx:
            self._remote_secret_ctx = self.manager.secret.all()
        return self._remote_secret_ctx

    def _set_config(self, key, value):
        if as_boolean(os.environ.get("JANS_OVERWRITE_ALL", False)):
            logger.info("updating config {!r}".format(key))
            self.ctx["config"][key] = value
            return value

        # check existing key first
        if key in self.remote_config_ctx:
            logger.info("ignoring config {!r}".format(key))
            self.ctx["config"][key] = value = self.remote_config_ctx[key]
            return value

        logger.info("adding config {!r}".format(key))
        self.ctx["config"][key] = value
        return value

    def _set_secret(self, key, value):
        if as_boolean(os.environ.get("JANS_OVERWRITE_ALL", False)):
            logger.info("updating secret {!r}".format(key))
            self.ctx["secret"][key] = value
            return value

        # check existing key first
        if key in self.remote_secret_ctx:
            logger.info("ignoring secret {!r}".format(key))
            self.ctx["secret"][key] = value = self.remote_secret_ctx[key]
            return value

        logger.info("adding secret {!r}".format(key))
        self.ctx["secret"][key] = value
        return value


class CtxGenerator(object):
    def __init__(self, manager, params):
        self.params = params
        self.manager = manager
        self.ctx_manager = CtxManager(self.manager)

    @property
    def ctx(self):
        return self.ctx_manager.ctx

    def _set_config(self, key, value):
        return self.ctx_manager._set_config(key, value)

    def _set_secret(self, key, value):
        return self.ctx_manager._set_secret(key, value)

    def base_ctx(self):
        self._set_secret("encoded_salt", get_random_chars(24))
        self._set_config("orgName", self.params["org_name"])
        self._set_config("country_code", self.params["country_code"])
        self._set_config("state", self.params["state"])
        self._set_config("city", self.params["city"])
        self._set_config("hostname", self.params["hostname"])
        self._set_config("admin_email", self.params["email"])
        self._set_config("default_openid_jks_dn_name", "CN=oxAuth CA Certificates")
        self._set_secret("pairwiseCalculationKey", get_sys_random_chars(random.randint(20, 30)))
        self._set_secret("pairwiseCalculationSalt", get_sys_random_chars(random.randint(20, 30)))
        self._set_config("jetty_base", "/opt/jans/jetty")
        self._set_config("fido2ConfigFolder", "/etc/jans/conf/fido2")
        self._set_config("admin_inum", "{}".format(uuid.uuid4()))
        self._set_secret("encoded_oxtrust_admin_password", ldap_encode(self.params["admin_pw"]))

    def ldap_ctx(self):
        # self._set_secret("encoded_ldap_pw", ldap_encode(self.params["admin_pw"]))
        self._set_secret(
            "encoded_ox_ldap_pw",
            encode_text(self.params["ldap_pw"], self.ctx["secret"]["encoded_salt"]),
        )
        self._set_config("ldap_init_host", "localhost")
        self._set_config("ldap_init_port", 1636)
        self._set_config("ldap_port", 1389)
        self._set_config("ldaps_port", 1636)
        self._set_config("ldap_binddn", "cn=directory manager")
        self._set_config("ldap_site_binddn", "cn=directory manager")
        self._set_secret("ldap_truststore_pass", get_random_chars())
        self._set_config("ldapTrustStoreFn", "/etc/certs/opendj.pkcs12")

        generate_ssl_certkey(
            "opendj",
            self.ctx["secret"]["ldap_truststore_pass"],
            self.ctx["config"]["admin_email"],
            self.ctx["config"]["hostname"],
            self.ctx["config"]["orgName"],
            self.ctx["config"]["country_code"],
            self.ctx["config"]["state"],
            self.ctx["config"]["city"],
        )
        with open("/etc/certs/opendj.pem", "w") as fw:
            with open("/etc/certs/opendj.crt") as fr:
                ldap_ssl_cert = fr.read()
                self._set_secret(
                    "ldap_ssl_cert",
                    encode_text(ldap_ssl_cert, self.ctx["secret"]["encoded_salt"]),
                )

            with open("/etc/certs/opendj.key") as fr:
                ldap_ssl_key = fr.read()
                self._set_secret(
                    "ldap_ssl_key",
                    encode_text(ldap_ssl_key, self.ctx["secret"]["encoded_salt"]),
                )

            ldap_ssl_cacert = "".join([ldap_ssl_cert, ldap_ssl_key])
            fw.write(ldap_ssl_cacert)
            self._set_secret(
                "ldap_ssl_cacert",
                encode_text(ldap_ssl_cacert, self.ctx["secret"]["encoded_salt"]),
            )

        generate_pkcs12(
            "opendj",
            self.ctx["secret"]["ldap_truststore_pass"],
            self.ctx["config"]["hostname"],
        )
        with open(self.ctx["config"]["ldapTrustStoreFn"], "rb") as fr:
            self._set_secret(
                "ldap_pkcs12_base64",
                encode_text(fr.read(), self.ctx["secret"]["encoded_salt"]),
            )

        self._set_secret(
            "encoded_ldapTrustStorePass",
            encode_text(self.ctx["secret"]["ldap_truststore_pass"], self.ctx["secret"]["encoded_salt"]),
        )

    def redis_ctx(self):
        self._set_secret("redis_pw", self.params.get("redis_pw", ""))

    def oxauth_ctx(self):
        self._set_config("oxauth_client_id", "1001.{}".format(uuid.uuid4()))
        self._set_secret(
            "oxauthClient_encoded_pw",
            encode_text(get_random_chars(), self.ctx["secret"]["encoded_salt"]),
        )
        self._set_config("oxauth_openid_jks_fn", "/etc/certs/oxauth-keys.jks")
        self._set_secret(
            "oxauth_openid_jks_pass",
            get_random_chars(),
        )
        self._set_config("oxauth_openid_jwks_fn", "/etc/certs/oxauth-keys.json")
        self._set_config("oxauth_legacyIdTokenClaims", "false")
        self._set_config("oxauth_openidScopeBackwardCompatibility", "false")

        _, err, retcode = generate_openid_keys(
            self.ctx["secret"]["oxauth_openid_jks_pass"],
            self.ctx["config"]["oxauth_openid_jks_fn"],
            self.ctx["config"]["oxauth_openid_jwks_fn"],
            self.ctx["config"]["default_openid_jks_dn_name"],
            exp=2,
            sig_keys="RS256 RS384 RS512 ES256 ES384 ES512 PS256 PS384 PS512",
            enc_keys="RSA1_5 RSA-OAEP",
        )
        if retcode != 0:
            logger.error("Unable to generate oxAuth keys; reason={}".format(err))
            raise click.Abort()

        basedir, fn = os.path.split(self.ctx["config"]["oxauth_openid_jwks_fn"])
        self._set_secret("oxauth_openid_key_base64", encode_template(fn, self.ctx, basedir))

        # oxAuth keys
        self._set_config("oxauth_key_rotated_at", int(time.time()))

        with open(self.ctx["config"]["oxauth_openid_jks_fn"], "rb") as fr:
            self._set_secret(
                "oxauth_jks_base64",
                encode_text(fr.read(), self.ctx["secret"]["encoded_salt"]),
            )

    def scim_rs_ctx(self):
        self._set_config("scim_rs_client_id", "1201.{}".format(uuid.uuid4()))
        self._set_config("scim_rs_client_jks_fn", "/etc/certs/scim-rs.jks")
        self._set_config("scim_rs_client_jwks_fn", "/etc/certs/scim-rs-keys.json")
        self._set_secret("scim_rs_client_jks_pass", get_random_chars())
        self._set_secret(
            "scim_rs_client_jks_pass_encoded",
            encode_text(self.ctx["secret"]["scim_rs_client_jks_pass"], self.ctx["secret"]["encoded_salt"]),
        )

        out, err, retcode = generate_openid_keys(
            self.ctx["secret"]["scim_rs_client_jks_pass"],
            self.ctx["config"]["scim_rs_client_jks_fn"],
            self.ctx["config"]["scim_rs_client_jwks_fn"],
            self.ctx["config"]["default_openid_jks_dn_name"],
        )
        if retcode != 0:
            logger.error("Unable to generate SCIM RS keys; reason={}".format(err))
            raise click.Abort()

        self._set_config("scim_rs_client_cert_alg", "RS512")

        cert_alias = ""
        for key in json.loads(out)["keys"]:
            if key["alg"] == self.ctx["config"]["scim_rs_client_cert_alg"]:
                cert_alias = key["kid"]
                break

        basedir, fn = os.path.split(self.ctx["config"]["scim_rs_client_jwks_fn"])
        self._set_secret("scim_rs_client_base64_jwks", encode_template(fn, self.ctx, basedir))
        self._set_config("scim_rs_client_cert_alias", cert_alias)

        with open(self.ctx["config"]["scim_rs_client_jks_fn"], "rb") as fr:
            self._set_secret(
                "scim_rs_jks_base64",
                encode_text(fr.read(), self.ctx["secret"]["encoded_salt"]),
            )

    def scim_rp_ctx(self):
        self._set_config("scim_rp_client_id", "1202.{}".format(uuid.uuid4()))
        self._set_config("scim_rp_client_jks_fn", "/etc/certs/scim-rp.jks")
        self._set_config("scim_rp_client_jwks_fn", "/etc/certs/scim-rp-keys.json")
        self._set_secret("scim_rp_client_jks_pass", get_random_chars())
        self._set_secret(
            "scim_rp_client_jks_pass_encoded",
            encode_text(self.ctx["secret"]["scim_rp_client_jks_pass"], self.ctx["secret"]["encoded_salt"]),
        )

        _, err, retcode = generate_openid_keys(
            self.ctx["secret"]["scim_rp_client_jks_pass"],
            self.ctx["config"]["scim_rp_client_jks_fn"],
            self.ctx["config"]["scim_rp_client_jwks_fn"],
            self.ctx["config"]["default_openid_jks_dn_name"],
        )
        if retcode != 0:
            logger.error("Unable to generate SCIM RP keys; reason={}".format(err))
            raise click.Abort()

        basedir, fn = os.path.split(self.ctx["config"]["scim_rp_client_jwks_fn"])
        self._set_secret("scim_rp_client_base64_jwks", encode_template(fn, self.ctx, basedir))

        with open(self.ctx["config"]["scim_rp_client_jks_fn"], "rb") as fr:
            self._set_secret(
                "scim_rp_jks_base64",
                encode_text(fr.read(), self.ctx["secret"]["encoded_salt"]),
            )

        self._set_config("scim_resource_oxid", "1203.{}".format(uuid.uuid4()))

    def passport_rs_ctx(self):
        self._set_config("passport_rs_client_id", "1501.{}".format(uuid.uuid4()))
        self._set_config("passport_rs_client_jks_fn", "/etc/certs/passport-rs.jks")
        self._set_config("passport_rs_client_jwks_fn", "/etc/certs/passport-rs-keys.json")
        self._set_secret("passport_rs_client_jks_pass", get_random_chars())
        self._set_secret(
            "passport_rs_client_jks_pass_encoded",
            encode_text(self.ctx["secret"]["passport_rs_client_jks_pass"], self.ctx["secret"]["encoded_salt"]),
        )

        out, err, retcode = generate_openid_keys(
            self.ctx["secret"]["passport_rs_client_jks_pass"],
            self.ctx["config"]["passport_rs_client_jks_fn"],
            self.ctx["config"]["passport_rs_client_jwks_fn"],
            self.ctx["config"]["default_openid_jks_dn_name"],
        )
        if retcode != 0:
            logger.error("Unable to generate Passport RS keys; reason={}".format(err))
            raise click.Abort()

        self._set_config("passport_rs_client_cert_alg", "RS512")

        cert_alias = ""
        for key in json.loads(out)["keys"]:
            if key["alg"] == self.ctx["config"]["passport_rs_client_cert_alg"]:
                cert_alias = key["kid"]
                break

        basedir, fn = os.path.split(self.ctx["config"]["passport_rs_client_jwks_fn"])
        self._set_secret(
            "passport_rs_client_base64_jwks",
            encode_template(fn, self.ctx, basedir),
        )

        self._set_config("passport_rs_client_cert_alias", cert_alias)

        with open(self.ctx["config"]["passport_rs_client_jks_fn"], "rb") as fr:
            self._set_secret(
                "passport_rs_jks_base64",
                encode_text(fr.read(), self.ctx["secret"]["encoded_salt"])
            )

        self._set_config("passport_resource_id", "1504.{}".format(uuid.uuid4()))
        self._set_config("passport_rs_client_cert_alias", cert_alias)

    def passport_rp_ctx(self):
        self._set_config("passport_rp_client_id", "1502.{}".format(uuid.uuid4()))
        self._set_config("passport_rp_ii_client_id", "1503.{}".format(uuid.uuid4()))
        self._set_secret("passport_rp_client_jks_pass", get_random_chars())
        self._set_config("passport_rp_client_jks_fn", "/etc/certs/passport-rp.jks")
        self._set_config("passport_rp_client_jwks_fn", "/etc/certs/passport-rp-keys.json")
        self._set_config("passport_rp_client_cert_fn", "/etc/certs/passport-rp.pem")
        self._set_config("passport_rp_client_cert_alg", "RS512")

        out, err, code = generate_openid_keys(
            self.ctx["secret"]["passport_rp_client_jks_pass"],
            self.ctx["config"]["passport_rp_client_jks_fn"],
            self.ctx["config"]["passport_rp_client_jwks_fn"],
            self.ctx["config"]["default_openid_jks_dn_name"],
        )
        if code != 0:
            logger.error("Unable to generate Passport RP keys; reason={}".format(err))
            raise click.Abort()

        cert_alias = ""
        for key in json.loads(out)["keys"]:
            if key["alg"] == self.ctx["config"]["passport_rp_client_cert_alg"]:
                cert_alias = key["kid"]
                break

        _, err, retcode = export_openid_keys(
            self.ctx["config"]["passport_rp_client_jks_fn"],
            self.ctx["secret"]["passport_rp_client_jks_pass"],
            cert_alias,
            self.ctx["config"]["passport_rp_client_cert_fn"],
        )
        if retcode != 0:
            logger.error("Unable to generate Passport RP client cert; reason={}".format(err))
            raise click.Abort()

        basedir, fn = os.path.split(self.ctx["config"]["passport_rp_client_jwks_fn"])
        self._set_secret("passport_rp_client_base64_jwks", encode_template(fn, self.ctx, basedir))

        self._set_config("passport_rp_client_cert_alias", cert_alias)

        with open(self.ctx["config"]["passport_rp_client_jks_fn"], "rb") as fr:
            self._set_secret(
                "passport_rp_jks_base64",
                encode_text(fr.read(), self.ctx["secret"]["encoded_salt"]),
            )

        with open(self.ctx["config"]["passport_rp_client_cert_fn"]) as fr:
            self._set_secret(
                "passport_rp_client_cert_base64",
                encode_text(fr.read(), self.ctx["secret"]["encoded_salt"]),
            )

    def passport_sp_ctx(self):
        self._set_secret("passportSpKeyPass", get_random_chars())
        self._set_config("passportSpTLSCACert", '/etc/certs/passport-sp.pem')
        self._set_config("passportSpTLSCert", '/etc/certs/passport-sp.crt')
        self._set_config("passportSpTLSKey", '/etc/certs/passport-sp.key')
        self._set_secret("passportSpJksPass", get_random_chars())
        self._set_config("passportSpJksFn", '/etc/certs/passport-sp.jks')

        generate_ssl_certkey(
            "passport-sp",
            self.ctx["secret"]["passportSpKeyPass"],
            self.ctx["config"]["admin_email"],
            self.ctx["config"]["hostname"],
            self.ctx["config"]["orgName"],
            self.ctx["config"]["country_code"],
            self.ctx["config"]["state"],
            self.ctx["config"]["city"],
        )
        with open(self.ctx["config"]["passportSpTLSCert"]) as f:
            self._set_secret(
                "passport_sp_cert_base64",
                encode_text(f.read(), self.ctx["secret"]["encoded_salt"])
            )

        with open(self.ctx["config"]["passportSpTLSKey"]) as f:
            self._set_secret(
                "passport_sp_key_base64",
                encode_text(f.read(), self.ctx["secret"]["encoded_salt"])
            )

    def nginx_ctx(self):
        ssl_cert = "/etc/certs/jans_https.crt"
        ssl_key = "/etc/certs/jans_https.key"
        self._set_secret("ssl_cert_pass", get_random_chars())

        # get cert and key (if available) with priorities below:
        #
        # 1. from mounted files
        # 2. from fronted (key file is an empty file)
        # 3. self-generate files

        ssl_cert_exists = os.path.isfile(ssl_cert)
        ssl_key_exists = os.path.isfile(ssl_key)

        logger.info(f"Resolving {ssl_cert} and {ssl_key}")

        # check from mounted files
        if not (ssl_cert_exists and ssl_key_exists):
            # no mounted files, hence download from frontend
            addr = os.environ.get("JANS_INGRESS_ADDRESS") or self.ctx["config"]["hostname"]
            servername = os.environ.get("JANS_INGRESS_SERVERNAME") or addr

            logger.warning(
                f"Unable to find mounted {ssl_cert} and {ssl_key}; "
                f"trying to download from {addr}:443 (servername {servername})"
            )
            try:
                # cert will be downloaded into `ssl_cert` path
                get_server_certificate(addr, 443, ssl_cert, servername)
                if not ssl_key_exists:
                    # since cert is downloaded, key must mounted
                    # or generate empty file
                    with open(ssl_key, "w") as f:
                        f.write("")
            except (socket.gaierror, socket.timeout, ConnectionRefusedError,
                    TimeoutError, ConnectionResetError) as exc:
                # address not resolved or timed out
                logger.warning(f"Unable to download cert; reason={exc}")
            finally:
                ssl_cert_exists = os.path.isfile(ssl_cert)
                ssl_key_exists = os.path.isfile(ssl_key)

        # no mounted nor downloaded files, hence we need to create self-generated files
        if not (ssl_cert_exists and ssl_key_exists):
            logger.info(f"Creating self-generated {ssl_cert} and {ssl_key}")
            generate_ssl_certkey(
                "jans_https",
                self.ctx["secret"]["ssl_cert_pass"],
                self.ctx["config"]["admin_email"],
                self.ctx["config"]["hostname"],
                self.ctx["config"]["orgName"],
                self.ctx["config"]["country_code"],
                self.ctx["config"]["state"],
                self.ctx["config"]["city"],
            )

        with open(ssl_cert) as f:
            self._set_secret("ssl_cert", f.read())

        with open(ssl_key) as f:
            self._set_secret("ssl_key", f.read())

    def oxshibboleth_ctx(self):
        self._set_config("idp_client_id", "1101.{}".format(uuid.uuid4()))
        self._set_secret(
            "idpClient_encoded_pw",
            encode_text(get_random_chars(), self.ctx["secret"]["encoded_salt"]),
        )
        self._set_config("shibJksFn", "/etc/certs/shibIDP.jks")
        self._set_secret("shibJksPass", get_random_chars())
        self._set_secret(
            "encoded_shib_jks_pw",
            encode_text(self.ctx["secret"]["shibJksPass"], self.ctx["secret"]["encoded_salt"])
        )

        generate_ssl_certkey(
            "shibIDP",
            self.ctx["secret"]["shibJksPass"],
            self.ctx["config"]["admin_email"],
            self.ctx["config"]["hostname"],
            self.ctx["config"]["orgName"],
            self.ctx["config"]["country_code"],
            self.ctx["config"]["state"],
            self.ctx["config"]["city"],
        )

        generate_keystore("shibIDP", self.ctx["config"]["hostname"], self.ctx["secret"]["shibJksPass"])

        with open("/etc/certs/shibIDP.crt") as f:
            self._set_secret(
                "shibIDP_cert",
                encode_text(f.read(), self.ctx["secret"]["encoded_salt"])
            )

        with open("/etc/certs/shibIDP.key") as f:
            self._set_secret(
                "shibIDP_key",
                encode_text(f.read(), self.ctx["secret"]["encoded_salt"])
            )

        with open(self.ctx["config"]["shibJksFn"], "rb") as f:
            self._set_secret(
                "shibIDP_jks_base64",
                encode_text(f.read(), self.ctx["secret"]["encoded_salt"])
            )

        self._set_config("shibboleth_version", "v3")
        self._set_config("idp3Folder", "/opt/shibboleth-idp")

        idp3_signing_cert = "/etc/certs/idp-signing.crt"
        idp3_signing_key = "/etc/certs/idp-signing.key"

        generate_ssl_certkey(
            "idp-signing",
            self.ctx["secret"]["shibJksPass"],
            self.ctx["config"]["admin_email"],
            self.ctx["config"]["hostname"],
            self.ctx["config"]["orgName"],
            self.ctx["config"]["country_code"],
            self.ctx["config"]["state"],
            self.ctx["config"]["city"],
        )

        with open(idp3_signing_cert) as f:
            self._set_secret(
                "idp3SigningCertificateText", f.read())

        with open(idp3_signing_key) as f:
            self._set_secret(
                "idp3SigningKeyText", f.read())

        idp3_encryption_cert = "/etc/certs/idp-encryption.crt"
        idp3_encryption_key = "/etc/certs/idp-encryption.key"

        generate_ssl_certkey(
            "idp-encryption",
            self.ctx["secret"]["shibJksPass"],
            self.ctx["config"]["admin_email"],
            self.ctx["config"]["hostname"],
            self.ctx["config"]["orgName"],
            self.ctx["config"]["country_code"],
            self.ctx["config"]["state"],
            self.ctx["config"]["city"],
        )

        with open(idp3_encryption_cert) as f:
            self._set_secret("idp3EncryptionCertificateText", f.read())

        with open(idp3_encryption_key) as f:
            self._set_secret("idp3EncryptionKeyText", f.read())

        _, err, code = gen_idp3_key(self.ctx["secret"]["shibJksPass"])
        if code != 0:
            logger.warninging(f"Unable to generate Shibboleth sealer; reason={err}")
            raise click.Abort()

        with open("/etc/certs/sealer.jks", "rb") as f:
            self._set_secret(
                "sealer_jks_base64",
                encode_text(f.read(), self.ctx["secret"]["encoded_salt"])
            )

        with open("/etc/certs/sealer.kver") as f:
            self._set_secret(
                "sealer_kver_base64",
                encode_text(f.read(), self.ctx["secret"]["encoded_salt"])
            )

    def oxtrust_api_rs_ctx(self):
        self._set_config("api_rs_client_jks_fn", "/etc/certs/api-rs.jks")
        self._set_config("api_rs_client_jwks_fn", "/etc/certs/api-rs-keys.json")
        self._set_secret("api_rs_client_jks_pass", get_random_chars())
        self._set_secret(
            "api_rs_client_jks_pass_encoded",
            encode_text(self.ctx["secret"]["api_rs_client_jks_pass"], self.ctx["secret"]["encoded_salt"]),
        )

        out, err, retcode = generate_openid_keys(
            self.ctx["secret"]["api_rs_client_jks_pass"],
            self.ctx["config"]["api_rs_client_jks_fn"],
            self.ctx["config"]["api_rs_client_jwks_fn"],
            self.ctx["config"]["default_openid_jks_dn_name"],
        )
        if retcode != 0:
            logger.error("Unable to generate oxTrust API RS keys; reason={}".format(err))
            raise click.Abort()

        self._set_config("api_rs_client_cert_alg", "RS512")

        cert_alias = ""
        for key in json.loads(out)["keys"]:
            if key["alg"] == self.ctx["config"]["api_rs_client_cert_alg"]:
                cert_alias = key["kid"]
                break

        basedir, fn = os.path.split(self.ctx["config"]["api_rs_client_jwks_fn"])
        self._set_secret("api_rs_client_base64_jwks", encode_template(fn, self.ctx, basedir))

        self._set_config("api_rs_client_cert_alias", cert_alias)

        self._set_config("oxtrust_resource_server_client_id", '1401.{}'.format(uuid.uuid4()))
        self._set_config("oxtrust_resource_id", '1403.{}'.format(uuid.uuid4()))

        with open(self.ctx["config"]["api_rs_client_jks_fn"], "rb") as fr:
            self._set_secret(
                "api_rs_jks_base64",
                encode_text(fr.read(), self.ctx["secret"]["encoded_salt"])
            )

    def oxtrust_api_rp_ctx(self):
        self._set_config("api_rp_client_jks_fn", "/etc/certs/api-rp.jks")
        self._set_config("api_rp_client_jwks_fn", "/etc/certs/api-rp-keys.json")
        self._set_secret("api_rp_client_jks_pass", get_random_chars())
        self._set_secret(
            "api_rp_client_jks_pass_encoded",
            encode_text(self.ctx["secret"]["api_rp_client_jks_pass"], self.ctx["secret"]["encoded_salt"]),
        )
        _, err, retcode = generate_openid_keys(
            self.ctx["secret"]["api_rp_client_jks_pass"],
            self.ctx["config"]["api_rp_client_jks_fn"],
            self.ctx["config"]["api_rp_client_jwks_fn"],
            self.ctx["config"]["default_openid_jks_dn_name"],
        )
        if retcode != 0:
            logger.error("Unable to generate oxTrust API RP keys; reason={}".format(err))
            raise click.Abort()

        basedir, fn = os.path.split(self.ctx["config"]["api_rp_client_jwks_fn"])
        self._set_secret("api_rp_client_base64_jwks", encode_template(fn, self.ctx, basedir))

        self._set_config("oxtrust_requesting_party_client_id", '1402.{}'.format(uuid.uuid4()))

        with open(self.ctx["config"]["api_rp_client_jks_fn"], "rb") as fr:
            self._set_secret(
                "api_rp_jks_base64",
                encode_text(fr.read(), self.ctx["secret"]["encoded_salt"])
            )

    def oxtrust_api_client_ctx(self):
        self._set_config("api_test_client_id", "0008-{}".format(uuid.uuid4()))
        self._set_secret("api_test_client_secret", get_random_chars(24))

    def radius_ctx(self):
        self._set_config("gluu_radius_client_id", '1701.{}'.format(uuid.uuid4()))
        # self._set_config("ox_radius_client_id", '0008-{}'.format(uuid.uuid4()))
        self._set_secret(
            "gluu_ro_encoded_pw",
            encode_text(get_random_chars(), self.ctx["secret"]["encoded_salt"]),
        )

        radius_jwt_pass = get_random_chars()
        self._set_secret(
            "radius_jwt_pass",
            encode_text(radius_jwt_pass, self.ctx["secret"]["encoded_salt"]),
        )

        out, err, code = generate_openid_keys(
            radius_jwt_pass,
            "/etc/certs/gluu-radius.jks",
            "/etc/certs/gluu-radius.keys",
            self.ctx["config"]["default_openid_jks_dn_name"],
        )
        if code != 0:
            logger.error("Unable to generate Gluu Radius keys; reason={}".format(err))
            raise click.Abort()

        for key in json.loads(out)["keys"]:
            if key["alg"] == "RS512":
                self.ctx["config"]["radius_jwt_keyId"] = key["kid"]
                break

        with open("/etc/certs/gluu-radius.jks", "rb") as fr:
            self._set_secret(
                "radius_jks_base64",
                encode_text(fr.read(), self.ctx["secret"]["encoded_salt"])
            )

        basedir, fn = os.path.split("/etc/certs/gluu-radius.keys")
        self._set_secret(
            "gluu_ro_client_base64_jwks",
            encode_template(fn, self.ctx, basedir),
        )

    def scim_client_ctx(self):
        self._set_config("scim_test_client_id", "0008-{}".format(uuid.uuid4()))
        self._set_secret("scim_test_client_secret", get_random_chars(24))

    def couchbase_ctx(self):
        self._set_config("couchbaseTrustStoreFn", "/etc/certs/couchbase.pkcs12")
        self._set_secret("couchbase_shib_user_password", get_random_chars())

    def jackrabbit_ctx(self):
        # self._set_secret("jca_pw", get_random_chars())
        # self._set_secret("jca_pw", "admin")
        pass

    def generate(self):
        self.base_ctx()
        self.nginx_ctx()
        # raise click.Abort()
        self.ldap_ctx()
        self.redis_ctx()
        self.oxauth_ctx()
        self.scim_rs_ctx()
        self.scim_rp_ctx()
        self.passport_rs_ctx()
        self.passport_rp_ctx()
        self.passport_sp_ctx()
        self.oxshibboleth_ctx()
        self.oxtrust_api_rs_ctx()
        self.oxtrust_api_rp_ctx()
        self.oxtrust_api_client_ctx()
        self.radius_ctx()
        self.scim_client_ctx()
        self.couchbase_ctx()
        self.jackrabbit_ctx()
        # populated config
        return self.ctx


def generate_ssl_certkey(suffix, passwd, email, hostname, org_name,
                         country_code, state, city):
    # create key with password
    _, err, retcode = exec_cmd(" ".join([
        "openssl",
        "genrsa -des3",
        "-out /etc/certs/{}.key.orig".format(suffix),
        "-passout pass:'{}' 2048".format(passwd),
    ]))
    assert retcode == 0, "Failed to generate SSL key with password; reason={}".format(err)

    # create .key
    _, err, retcode = exec_cmd(" ".join([
        "openssl",
        "rsa",
        "-in /etc/certs/{}.key.orig".format(suffix),
        "-passin pass:'{}'".format(passwd),
        "-out /etc/certs/{}.key".format(suffix),
    ]))
    assert retcode == 0, "Failed to generate SSL key; reason={}".format(err)

    # create .csr
    _, err, retcode = exec_cmd(" ".join([
        "openssl",
        "req",
        "-new",
        "-key /etc/certs/{}.key".format(suffix),
        "-out /etc/certs/{}.csr".format(suffix),
        """-subj /C="{}"/ST="{}"/L="{}"/O="{}"/CN="{}"/emailAddress='{}'""".format(country_code, state, city, org_name, hostname, email),

    ]))
    assert retcode == 0, "Failed to generate SSL CSR; reason={}".format(err)

    # create .crt
    _, err, retcode = exec_cmd(" ".join([
        "openssl",
        "x509",
        "-req",
        "-days 365",
        "-in /etc/certs/{}.csr".format(suffix),
        "-signkey /etc/certs/{}.key".format(suffix),
        "-out /etc/certs/{}.crt".format(suffix),
    ]))
    assert retcode == 0, "Failed to generate SSL cert; reason={}".format(err)

    # return the paths
    return "/etc/certs/{}.crt".format(suffix), \
           "/etc/certs/{}.key".format(suffix)


def generate_keystore(suffix, hostname, keypasswd):
    # converts key to pkcs12
    cmd = " ".join([
        "openssl",
        "pkcs12",
        "-export",
        "-inkey /etc/certs/{}.key".format(suffix),
        "-in /etc/certs/{}.crt".format(suffix),
        "-out /etc/certs/{}.pkcs12".format(suffix),
        "-name {}".format(hostname),
        "-passout pass:'{}'".format(keypasswd),
    ])
    _, err, retcode = exec_cmd(cmd)
    assert retcode == 0, "Failed to generate PKCS12 keystore; reason={}".format(err)

    # imports p12 to keystore
    cmd = " ".join([
        "keytool",
        "-importkeystore",
        "-srckeystore /etc/certs/{}.pkcs12".format(suffix),
        "-srcstorepass {}".format(keypasswd),
        "-srcstoretype PKCS12",
        "-destkeystore /etc/certs/{}.jks".format(suffix),
        "-deststorepass {}".format(keypasswd),
        "-deststoretype JKS",
        "-keyalg RSA",
        "-noprompt",
    ])
    _, err, retcode = exec_cmd(cmd)
    assert retcode == 0, "Failed to generate JKS keystore; reason={}".format(err)


def gen_idp3_key(storepass):
    cmd = " ".join([
        "java",
        "-classpath '/app/javalibs/*'",
        "net.shibboleth.utilities.java.support.security.BasicKeystoreKeyStrategyTool",
        "--storefile /etc/certs/sealer.jks",
        "--versionfile /etc/certs/sealer.kver",
        "--alias secret",
        "--storepass {}".format(storepass),
    ])
    return exec_cmd(cmd)


def _save_generated_ctx(manager, data, type_):
    if type_ == "config":
        backend = manager.config
    else:
        backend = manager.secret

    logger.info("Saving {} to backend".format(type_))

    for k, v in data.items():
        backend.set(k, v)


def _load_from_file(manager, filepath, type_):
    ctx_manager = CtxManager(manager)
    if type_ == "config":
        setter = ctx_manager._set_config
        backend = manager.config
    else:
        setter = ctx_manager._set_secret
        backend = manager.secret

    logger.info("Loading {} from {}".format(type_, filepath))

    with open(filepath, "r") as f:
        data = json.loads(f.read())

    if "_{}".format(type_) not in data:
        logger.warning("Missing '_{}' key".format(type_))
        return

    # tolerancy before checking existing key
    time.sleep(5)

    for k, v in data["_{}".format(type_)].items():
        val = setter(k, v)
        backend.set(k, val)


def _dump_to_file(manager, filepath, type_):
    if type_ == "config":
        backend = manager.config
    else:
        backend = manager.secret

    logger.info("Saving {} to {}".format(type_, filepath))

    data = {"_{}".format(type_): backend.all()}
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
    config_file_found = os.path.isfile(config_file)
    secret_file_found = os.path.isfile(secret_file)
    should_generate = False
    params = {}

    if not any([config_file_found, secret_file_found]):
        should_generate = True
        logger.warning("Unable to find {0} or {1}".format(config_file, secret_file))

        logger.info("Loading parameters from {}".format(generate_file))

        params, err, code = params_from_file(generate_file)
        if code != 0:
            logger.error("Unable to load generate parameters; reason={}".format(err))
            raise click.Abort()

    deps = ["config_conn", "secret_conn"]
    wait_for(manager, deps=deps)

    if should_generate:
        logger.info("Generating config and secret.")

        # tolerancy before checking existing key
        time.sleep(5)

        ctx_generator = CtxGenerator(manager, params)
        ctx = ctx_generator.generate()

        _save_generated_ctx(manager, ctx["config"], "config")
        _dump_to_file(manager, config_file, "config")

        _save_generated_ctx(manager, ctx["secret"], "secret")
        _dump_to_file(manager, secret_file, "secret")
        return

    # load from existing files
    _load_from_file(manager, config_file, "config")
    _load_from_file(manager, secret_file, "secret")


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
    cli(prog_name="config-init")
