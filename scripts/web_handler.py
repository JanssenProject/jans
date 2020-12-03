import logging.config
import os

from base_handler import BaseHandler
from settings import LOGGING_CONFIG

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("certmanager")


class WebHandler(BaseHandler):
    def patch(self):
        passwd = self.manager.secret.get("ssl_cert_pass")

        source = self.opts.get("source", "")

        if source == "from-files":
            cert_fn = "/etc/certs/web_https.crt"
            key_fn = "/etc/certs/web_https.key"

            if not any([os.path.isfile(cert_fn), os.path.isfile(key_fn)]):
                logger.warning(f"Unable to find existing {cert_fn} or {key_fn}")
            else:
                logger.info(f"Using existing {cert_fn} and {key_fn}")
        else:
            cert_fn, key_fn = self._patch_cert_key("web_https", passwd)

        if not self.dry_run:
            if cert_fn:
                self.manager.secret.from_file("ssl_cert", cert_fn)
            if key_fn:
                self.manager.secret.from_file("ssl_key", key_fn)
