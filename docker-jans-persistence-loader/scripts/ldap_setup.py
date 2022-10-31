import json
import logging.config
import time
from pathlib import Path

from ldap3.core.exceptions import LDAPSessionTerminatedByServerError
from ldap3.core.exceptions import LDAPSocketOpenError

from jans.pycloudlib.persistence.ldap import LdapClient
# from jans.pycloudlib.persistence.utils import PersistenceMapper

from settings import LOGGING_CONFIG
from utils import prepare_template_ctx
from utils import get_ldif_mappings

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("ldap_setup")


class LDAPBackend:
    def __init__(self, manager):
        self.client = LdapClient(manager)
        self.manager = manager

    def check_indexes(self, mapping):
        if mapping == "site":
            index_name = "jansScrTyp"
            backend = "site"
        # elif mapping == "statistic":
        #     index_name = "jansMetricTyp"
        #     backend = "metric"
        else:
            index_name = "del"
            backend = "userRoot"

        dn = "ds-cfg-attribute={},cn=Index,ds-cfg-backend-id={}," \
             "cn=Backends,cn=config".format(index_name, backend)

        max_wait_time = 300
        sleep_duration = 10

        for _ in range(0, max_wait_time, sleep_duration):
            try:
                if self.client.get(dn, attributes=["1.1"]):
                    return
                reason = f"Index {dn} is not ready"
            except (LDAPSessionTerminatedByServerError, LDAPSocketOpenError) as exc:
                reason = exc

            logger.warning("Waiting for index to be ready; reason={}; "
                           "retrying in {} seconds".format(reason, sleep_duration))
            time.sleep(sleep_duration)

    def import_builtin_ldif(self, ctx):
        optional_scopes = json.loads(
            self.manager.config.get("optional_scopes", "[]")
        )
        ldif_mappings = get_ldif_mappings("ldap", optional_scopes)

        # ensure base.ldif (contains base RDNs) is in list of ldif files
        if ldif_mappings and "default" not in ldif_mappings:
            # insert base.ldif into the first mapping found
            mapping = next(iter(ldif_mappings))
            ldif_mappings[mapping].insert(0, "base.ldif")

        for mapping, files in ldif_mappings.items():
            self.check_indexes(mapping)

            for file_ in files:
                self._import_ldif(f"/app/templates/{file_}", ctx)

    def initialize(self):
        ctx = prepare_template_ctx(self.manager)

        logger.info("Importing builtin LDIF files")
        self.import_builtin_ldif(ctx)

        logger.info("Importing custom LDIF files (if any)")
        self.import_custom_ldif(ctx)

    def import_custom_ldif(self, ctx):
        custom_dir = Path("/app/custom_ldif")

        for file_ in custom_dir.rglob("*.ldif"):
            self._import_ldif(file_, ctx)

    def _import_ldif(self, path, ctx):
        logger.info(f"Importing {path} file")
        self.client.create_from_ldif(path, ctx)
