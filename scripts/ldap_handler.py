import logging.config

from jans.pycloudlib.utils import decode_text
from jans.pycloudlib.utils import exec_cmd

from base_handler import BaseHandler
from settings import LOGGING_CONFIG

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("certmanager")


SAN_CNF = """[req]
req_extensions = v3_req
distinguished_name = dn

[dn]

[v3_req]
# Extensions to add to a certificate request
basicConstraints = CA:FALSE
keyUsage = nonRepudiation, digitalSignature, keyEncipherment
subjectAltName = @alt_names

[alt_names]
DNS.1 = %(alt_name)s
"""


class LdapHandler(BaseHandler):
    def generate_x509(self):
        suffix = "opendj"
        passwd = decode_text(self.manager.secret.get("encoded_ox_ldap_pw"),
                             self.manager.secret.get("encoded_salt")).decode()
        country_code = self.manager.config.get("country_code")
        state = self.manager.config.get("state")
        city = self.manager.config.get("city")
        org_name = self.manager.config.get("orgName")
        domain = self.manager.config.get("hostname")
        email = self.manager.config.get("admin_email")

        logger.info(
            f"Generating /etc/certs/{suffix}.key, /etc/certs/{suffix}.crt, and /etc/certs/{suffix}.pem file(s)",
        )

        # create key with password
        _, err, retcode = exec_cmd(
            "openssl genrsa -des3 -out /etc/certs/{}.key.orig "
            "-passout pass:'{}' 2048".format(suffix, passwd))
        assert retcode == 0, "Failed to generate SSL key with password; reason={}".format(err.decode())

        # create .key
        _, err, retcode = exec_cmd(
            "openssl rsa -in /etc/certs/{0}.key.orig "
            "-passin pass:'{1}' -out /etc/certs/{0}.key".format(suffix, passwd))
        assert retcode == 0, "Failed to generate SSL key; reason={}".format(err.decode())

        # create .csr
        _, err, retcode = exec_cmd(
            "openssl req -new -key /etc/certs/{0}.key "
            "-out /etc/certs/{0}.csr "
            "-config /etc/ssl/san.cnf "
            "-subj /C='{1}'/ST='{2}'/L='{3}'/O='{4}'/CN='{5}'/emailAddress='{6}'".format(suffix, country_code, state, city, org_name, domain, email))
        assert retcode == 0, "Failed to generate SSL CSR; reason={}".format(err.decode())

        # create .crt
        _, err, retcode = exec_cmd(
            "openssl x509 -req -days 365 -in /etc/certs/{0}.csr "
            "-extensions v3_req -extfile /etc/ssl/san.cnf "
            "-signkey /etc/certs/{0}.key -out /etc/certs/{0}.crt".format(suffix))
        assert retcode == 0, "Failed to generate SSL cert; reason={}".format(err.decode())

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

        logger.info(f"Generating /etc/certs/{suffix}.pkcs12 file(s)")

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

    def render_san_cnf(self):
        name = self.opts.get("subj-alt-name", "localhost")
        ctx = {"alt_name": name}

        with open("/etc/ssl/san.cnf", "w") as fw:
            fw.write(SAN_CNF % ctx)

    def patch(self):
        self.render_san_cnf()
        self.generate_x509()
        self.generate_keystore()
