import json
import logging.config
import os
import time
from pathlib import Path

from ldap3.core.exceptions import LDAPSessionTerminatedByServerError
from ldap3.core.exceptions import LDAPSocketOpenError

from ldif import LDIFParser

from jans.pycloudlib.persistence.ldap import LdapClient

from settings import LOGGING_CONFIG
from utils import render_ldif
from utils import prepare_template_ctx
from utils import get_ldif_mappings

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("entrypoint")


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
        optional_scopes = json.loads(self.manager.config.get("optional_scopes", "[]"))
        ldif_mappings = get_ldif_mappings(optional_scopes)

        # hybrid means only a subsets of ldif are needed
        persistence_type = os.environ.get("CN_PERSISTENCE_TYPE", "ldap")
        ldap_mapping = os.environ.get("CN_PERSISTENCE_LDAP_MAPPING", "default")
        if persistence_type == "hybrid":
            mapping = ldap_mapping
            ldif_mappings = {mapping: ldif_mappings[mapping]}

            # these mappings require `base.ldif`
            # opt_mappings = ("user", "token",)

            # `user` mapping requires `o=jans` which available in `base.ldif`
            # if mapping in opt_mappings and "base.ldif" not in ldif_mappings[mapping]:
            if "base.ldif" not in ldif_mappings[mapping]:
                ldif_mappings[mapping].insert(0, "base.ldif")

        for mapping, files in ldif_mappings.items():
            self.check_indexes(mapping)

            for file_ in files:
                self._import_ldif(f"/app/templates/{file_}", ctx)

    def add_entry(self, dn, attrs):
        max_wait_time = 300
        sleep_duration = 10

        for _ in range(0, max_wait_time, sleep_duration):
            try:
                added, msg = self.client.add(dn, attributes=attrs)
                if not added and "name already exists" not in msg:
                    logger.warning(f"Unable to add entry with DN {dn}; reason={msg}")
                break
            except (LDAPSessionTerminatedByServerError, LDAPSocketOpenError) as exc:
                logger.warning(f"Unable to add entry with DN {dn}; reason={exc}; retrying in {sleep_duration} seconds")
            time.sleep(sleep_duration)

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
        src = Path(path).resolve()

        # generated template will be saved under ``/app/tmp`` directory
        # examples:
        # - ``/app/templates/groups.ldif`` will be saved as ``/app/tmp/templates/groups.ldif``
        # - ``/app/custom_ldif/groups.ldif`` will be saved as ``/app/tmp/custom_ldif/groups.ldif``
        dst = Path("/app/tmp").joinpath(str(src).removeprefix("/app/")).resolve()

        # ensure directory for generated template is exist
        dst.parent.mkdir(parents=True, exist_ok=True)

        logger.info(f"Importing {src} file")
        render_ldif(src, dst, ctx)

        with open(dst, "rb") as fd:
            parser = LDIFParser(fd)
            for dn, entry in parser.parse():
                self.add_entry(dn, entry)
