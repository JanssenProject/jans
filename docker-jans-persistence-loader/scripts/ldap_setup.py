import json
import logging.config
import os
import time

from ldap3.core.exceptions import LDAPSessionTerminatedByServerError
from ldap3.core.exceptions import LDAPSocketOpenError

from ldif import LDIFParser

from jans.pycloudlib.utils import as_boolean
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

    def import_ldif(self):
        optional_scopes = json.loads(self.manager.config.get("optional_scopes", "[]"))
        ldif_mappings = get_ldif_mappings(optional_scopes)

        # hybrid means only a subsets of ldif are needed
        persistence_type = os.environ.get("CN_PERSISTENCE_TYPE", "ldap")
        ldap_mapping = os.environ.get("CN_PERSISTENCE_LDAP_MAPPING", "default")
        if persistence_type == "hybrid":
            mapping = ldap_mapping
            ldif_mappings = {mapping: ldif_mappings[mapping]}

            # # these mappings require `base.ldif`
            # opt_mappings = ("user", "token",)

            # `user` mapping requires `o=gluu` which available in `base.ldif`
            # if mapping in opt_mappings and "base.ldif" not in ldif_mappings[mapping]:
            if "base.ldif" not in ldif_mappings[mapping]:
                ldif_mappings[mapping].insert(0, "base.ldif")

        ctx = prepare_template_ctx(self.manager)

        for mapping, files in ldif_mappings.items():
            self.check_indexes(mapping)

            for file_ in files:
                logger.info(f"Importing {file_} file")
                src = f"/app/templates/{file_}"
                dst = f"/app/tmp/{file_}"
                os.makedirs(os.path.dirname(dst), exist_ok=True)

                render_ldif(src, dst, ctx)

                parser = LDIFParser(open(dst, "rb"))
                for dn, entry in parser.parse():
                    self.add_entry(dn, entry)

    def add_entry(self, dn, attrs):
        max_wait_time = 300
        sleep_duration = 10

        for _ in range(0, max_wait_time, sleep_duration):
            try:
                added, msg = self.client.add(dn, attributes=attrs)
                if not added:
                    logger.warning(f"Unable to add entry with DN {dn}; reason={msg}")
                break
            except (LDAPSessionTerminatedByServerError, LDAPSocketOpenError) as exc:
                logger.warning(f"Unable to add entry with DN {dn}; reason={exc}; retrying in {sleep_duration} seconds")
            time.sleep(sleep_duration)

    def initialize(self):
        def is_initialized():
            persistence_type = os.environ.get("CN_PERSISTENCE_TYPE", "ldap")
            ldap_mapping = os.environ.get("CN_PERSISTENCE_LDAP_MAPPING", "default")

            # a minimum service stack is having oxTrust, hence check whether entry
            # for oxTrust exists in LDAP
            default_search = ("ou=jans-auth,ou=configuration,o=jans",
                              "(objectClass=jansAppConf)")

            if persistence_type == "hybrid":
                # `cache` and `token` mapping only have base entries
                search_mapping = {
                    "default": default_search,
                    "user": ("inum=60B7,ou=groups,o=jans", "(objectClass=jansGrp)"),
                    "site": ("ou=cache-refresh,o=site", "(ou=people)"),
                    "cache": ("o=jans", "(ou=cache)"),
                    "token": ("ou=tokens,o=jans", "(ou=tokens)"),
                    "session": ("ou=sessions,o=jans", "(ou=sessions)"),
                }
                search = search_mapping[ldap_mapping]
            else:
                search = default_search
            return self.client.search(search[0], search[1], attributes=["objectClass"], limit=1)

        should_skip = as_boolean(
            os.environ.get("CN_PERSISTENCE_SKIP_INITIALIZED", False),
        )
        if should_skip and is_initialized():
            logger.info("LDAP backend already initialized")
            return
        self.import_ldif()
