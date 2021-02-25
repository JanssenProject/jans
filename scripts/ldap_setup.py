import logging.config
import os
import time

from ldap3 import BASE
from ldap3 import Connection
from ldap3 import Server
from ldap3 import SUBTREE
from ldap3.core.exceptions import LDAPSessionTerminatedByServerError
from ldap3.core.exceptions import LDAPSocketOpenError
from ldif3 import LDIFParser

from jans.pycloudlib.utils import decode_text
from jans.pycloudlib.utils import as_boolean

from settings import LOGGING_CONFIG
from utils import render_ldif
from utils import prepare_template_ctx

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("entrypoint")


class LDAPBackend:
    def __init__(self, manager):
        host = os.environ.get("CN_LDAP_URL", "localhost:1636")
        user = manager.config.get("ldap_binddn")
        password = decode_text(
            manager.secret.get("encoded_ox_ldap_pw"),
            manager.secret.get("encoded_salt"),
        ).decode()

        server = Server(host, port=1636, use_ssl=True)
        self.conn = Connection(server, user, password)
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
                with self.conn as conn:
                    conn.search(
                        search_base=dn,
                        search_filter="(objectClass=*)",
                        search_scope=BASE,
                        attributes=["1.1"],
                        size_limit=1,
                    )
                    if conn.result["description"] == "success":
                        return
                    reason = conn.result["message"]
            except (LDAPSessionTerminatedByServerError, LDAPSocketOpenError) as exc:
                reason = exc

            logger.warning("Waiting for index to be ready; reason={}; "
                           "retrying in {} seconds".format(reason, sleep_duration))
            time.sleep(sleep_duration)

    def import_ldif(self):
        ldif_mappings = {
            "default": [
                "base.ldif",
                "attributes.ldif",
                "scopes.ldif",
                "scripts.ldif",
                "configuration.ldif",
                "jans-auth/configuration.ldif",
                "jans-auth/clients.ldif",
                "jans-fido2/configuration.ldif",
                "jans-scim/configuration.ldif",
                "jans-scim/scopes.ldif",
                "jans-scim/clients.ldif",
                "jans-config-api/scopes.ldif",
                "jans-config-api/clients.ldif",
                # "oxidp.ldif",
                # "passport.ldif",
                # "oxpassport-config.ldif",
                # "gluu_radius_base.ldif",
                # "gluu_radius_server.ldif",
                # "clients.ldif",
                "o_metric.ldif",
                # "gluu_radius_clients.ldif",
                # "passport_clients.ldif",
                # "casa.ldif",
                # "scripts_casa.ldif",
            ],
            "user": [
                "people.ldif",
                "groups.ldif",
            ],
            "site": [
                "o_site.ldif",
            ],
            "cache": [],
            "token": [],
            "session": [],
        }

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
                with self.conn as conn:
                    conn.add(dn, attributes=attrs)
                    if conn.result["result"] != 0:
                        logger.warning(f"Unable to add entry with DN {dn}; reason={conn.result['message']}")
                    return
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

            with self.conn as conn:
                conn.search(
                    search_base=search[0],
                    search_filter=search[1],
                    search_scope=SUBTREE,
                    attributes=['objectClass'],
                    size_limit=1,
                )
                return bool(conn.entries)

        should_skip = as_boolean(
            os.environ.get("CN_PERSISTENCE_SKIP_EXISTING", True),
        )
        if should_skip and is_initialized():
            logger.info("LDAP backend already initialized")
            return
        self.import_ldif()
