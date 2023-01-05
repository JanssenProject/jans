import logging.config
import os

from jans.pycloudlib.utils import generate_ssl_ca_certkey
from jans.pycloudlib.utils import generate_signed_ssl_certkey

from base_handler import BaseHandler
from settings import LOGGING_CONFIG

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("certmanager")


class WebHandler(BaseHandler):
    def patch(self):
        source = self.opts.get("source", "")

        try:
            valid_to = int(self.opts.get("valid-to", 365))
        except ValueError:
            valid_to = 365
        finally:
            if valid_to < 1:
                valid_to = 365

        ssl_cert = "/etc/certs/web_https.crt"
        ssl_key = "/etc/certs/web_https.key"
        ssl_csr = "/etc/certs/web_https.csr"

        ssl_ca_cert = "/etc/certs/ca.crt"
        ssl_ca_key = "/etc/certs/ca.key"

        if source == "from-files":
            if not any([os.path.isfile(ssl_cert), os.path.isfile(ssl_key)]):
                logger.warning(f"Unable to find existing {ssl_cert} or {ssl_key}")
            else:
                logger.info(f"Using existing {ssl_cert} and {ssl_key}")
        else:
            email = self.manager.config.get("admin_email")
            hostname = self.manager.config.get("hostname")
            org_name = self.manager.config.get("orgName")
            country_code = self.manager.config.get("country_code")
            state = self.manager.config.get("state")
            city = self.manager.config.get("city")

            logger.info(f"Creating self-generated {ssl_ca_cert} and {ssl_ca_key}")

            ca_cert, ca_key = generate_ssl_ca_certkey(
                "ca",
                email,
                "Janssen CA",
                org_name,
                country_code,
                state,
                city,
                valid_to=valid_to,
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
                valid_to=valid_to,
            )

        if not self.dry_run:
            if os.path.isfile(ssl_ca_key):
                self.manager.secret.from_file("ssl_ca_key", ssl_ca_key)
            if os.path.isfile(ssl_ca_cert):
                self.manager.secret.from_file("ssl_ca_cert", ssl_ca_cert)

            if os.path.isfile(ssl_cert):
                self.manager.secret.from_file("ssl_cert", ssl_cert)
            if os.path.isfile(ssl_key):
                self.manager.secret.from_file("ssl_key", ssl_key)
            if os.path.isfile(ssl_csr):
                self.manager.secret.from_file("ssl_csr", ssl_csr)
