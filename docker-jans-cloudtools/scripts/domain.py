import logging.config

import click

from jans.pycloudlib import get_manager
from jans.pycloudlib.persistence.sql import SqlClient

from settings import LOGGING_CONFIG

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("cloudtools")


def replace_fqdn_substr(val, old_fqdn, new_fqdn):
    if isinstance(val, (str, bytes)):
        return val.replace(old_fqdn, new_fqdn)

    if isinstance(val, list):
        return [replace_fqdn_substr(item, old_fqdn, new_fqdn) for item in val]

    if isinstance(val, dict):
        return {k: replace_fqdn_substr(v, old_fqdn, new_fqdn) for k, v in val.items()}

    # unsupported type will be returned as-is
    return val


class Domain:
    def __init__(self, manager):
        self.manager = manager
        self.persistence = SqlClient(self.manager)

    def modify_persistence_entries(self, table_name, old_fqdn, new_fqdn):
        logger.info("Checking entries in %s table", table_name)

        for entry in self.persistence.search(table_name):
            # flag to determine whether entry need to be updated in persistence
            should_update = False

            for col_name, col_val in entry.items():
                new_val = replace_fqdn_substr(col_val, old_fqdn, new_fqdn)

                # likely no changes at all
                if entry[col_name] == new_val:
                    continue

                logger.info("Updating %s.%s (doc_id=%s)", table_name, col_name, entry["doc_id"])
                # mark entry for updates
                should_update = True
                entry[col_name] = new_val

            if should_update is False:
                continue

            if "jansRevision" in entry:
                entry["jansRevision"] = int(entry["jansRevision"] or 0) + 1
            self.persistence.update(table_name, entry["doc_id"], entry)

    def change_fqdn(self, old_fqdn, new_fqdn):
        logger.info("Detected old FQDN from existing config: %s", old_fqdn)
        logger.info("Changing FQDN from %s to %s", old_fqdn, new_fqdn)

        for table_name in ["jansAppConf", "jansCustomScr", "jansClnt"]:
            self.modify_persistence_entries(table_name, old_fqdn, new_fqdn)


@click.command
@click.argument("new_fqdn")
@click.option(
    "--old-fqdn",
    help="Old FQDN need to be changed from (if omitted, will use FQDN stored in config)",
    default="",
    type=str,
)
def change_fqdn(new_fqdn, old_fqdn):
    """Change FQDN."""
    manager = get_manager()
    old_fqdn = old_fqdn or manager.config.get("hostname")

    domain = Domain(manager)
    domain.change_fqdn(old_fqdn, new_fqdn)

    # @TODO: update configmaps and/or secrets


if __name__ == "__main__":
    change_fqdn(prog_name="change-fqdn")
