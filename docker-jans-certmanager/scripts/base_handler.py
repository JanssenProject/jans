import logging.config

from jans.pycloudlib.utils import generate_ssl_certkey

from settings import LOGGING_CONFIG
from utils import generate_keystore

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("certmanager")


class BaseHandler:
    def __init__(self, manager, dry_run, **opts):
        self.manager = manager
        self.dry_run = dry_run
        self.opts = opts

    def _patch_keystore(self, prefix, hostname, passwd):
        keystore_fn = f"/etc/certs/{prefix}.jks"
        logger.info(f"Generating new {keystore_fn} files")
        generate_keystore(prefix, hostname, passwd)
        return keystore_fn

    def _patch_cert_key(self, prefix, extra_dns=None, valid_to=365):
        cert_fn = f"/etc/certs/{prefix}.crt"
        key_fn = f"/etc/certs/{prefix}.key"

        logger.info(f"Generating new {cert_fn} and {key_fn} files")
        generate_ssl_certkey(
            prefix,
            self.manager.config.get("admin_email"),
            self.manager.config.get("hostname"),
            self.manager.config.get("orgName"),
            self.manager.config.get("country_code"),
            self.manager.config.get("state"),
            self.manager.config.get("city"),
            extra_dns=extra_dns,
            valid_to=valid_to,
        )
        return cert_fn, key_fn

    def patch(self):
        raise NotImplementedError
