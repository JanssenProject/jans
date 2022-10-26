import logging.config

from jans.pycloudlib.utils import exec_cmd

from base_handler import BaseHandler
from settings import LOGGING_CONFIG

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("certmanager")


class LdapHandler(BaseHandler):
    def generate_x509(self):
        alt_name = self.opts.get("subj-alt-name", "localhost")
        suffix = "opendj"

        try:
            valid_to = int(self.opts.get("valid-to", 365))
        except ValueError:
            valid_to = 365
        finally:
            if valid_to < 1:
                valid_to = 365

        self._patch_cert_key(suffix, extra_dns=[alt_name], valid_to=valid_to)

        with open("/etc/certs/{}.pem".format(suffix), "w") as fw:
            with open("/etc/certs/{}.crt".format(suffix)) as fr:
                ldap_ssl_cert = fr.read()

            with open("/etc/certs/{}.key".format(suffix)) as fr:
                ldap_ssl_key = fr.read()

            ldap_ssl_cacert = "".join([ldap_ssl_cert, ldap_ssl_key])
            fw.write(ldap_ssl_cacert)

        if not self.dry_run:
            self.manager.secret.from_file(
                "ldap_ssl_cert", f"/etc/certs/{suffix}.crt", encode=True,
            )
            self.manager.secret.from_file(
                "ldap_ssl_key", f"/etc/certs/{suffix}.key", encode=True,
            )
            self.manager.secret.from_file(
                "ldap_ssl_cacert", f"/etc/certs/{suffix}.pem", encode=True,
            )

    def generate_keystore(self):
        suffix = "opendj"
        passwd = self.manager.secret.get("ldap_truststore_pass")
        hostname = self.manager.config.get("hostname")

        logger.info(f"Generating /etc/certs/{suffix}.pkcs12 file")

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
        assert retcode == 0, "Failed to generate PKCS12 file; reason={}".format(err.decode())

        if not self.dry_run:
            self.manager.secret.from_file(
                "ldap_pkcs12_base64",
                f"/etc/certs/{suffix}.pkcs12",
                encode=True,
                binary_mode=True,
            )

    def patch(self):
        self.generate_x509()
        self.generate_keystore()
