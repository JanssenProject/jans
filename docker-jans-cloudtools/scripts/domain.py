import logging.config
import re

import click
from fqdn import FQDN

from jans.pycloudlib import get_manager
from jans.pycloudlib.persistence.sql import SqlClient

from settings import LOGGING_CONFIG

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("cloudtools")


def replace_fqdn_substr(val, old_fqdn, new_fqdn):
    if isinstance(val, str):
        pattern = rf"(?<![\w.-]){re.escape(old_fqdn)}(?![\w.-])"
        return re.sub(pattern, new_fqdn, val)

    if isinstance(val, bytes):
        old_fqdn = old_fqdn.encode()
        new_fqdn = new_fqdn.encode()
        pattern = rb"(?<![\w.-])" + re.escape(old_fqdn) + rb"(?![\w.-])"
        return re.sub(pattern, new_fqdn, val)

    if isinstance(val, list):
        return [replace_fqdn_substr(item, old_fqdn, new_fqdn) for item in val]

    if isinstance(val, dict):
        return {k: replace_fqdn_substr(v, old_fqdn, new_fqdn) for k, v in val.items()}

    # unsupported type will be returned as-is
    return val


class Domain:
    def __init__(self, manager, **kwargs):
        self.manager = manager
        self.persistence = SqlClient(self.manager)
        self.dry_run = kwargs.get("dry_run") or False

    def modify_persistence_entries(self, table_name: str, old_fqdn: str, new_fqdn: str) -> None:
        logger.info("Checking entries in %s table", table_name)

        for entry in self.persistence.search(table_name):
            # flag to determine whether entry need to be updated in persistence
            should_update = False

            for col_name, col_val in entry.items():
                new_val = replace_fqdn_substr(col_val, old_fqdn, new_fqdn)

                # likely no changes at all
                if entry[col_name] == new_val:
                    continue

                logger.info("Found potential changes for %s.%s (doc_id=%s)", table_name, col_name, entry["doc_id"])

                # mark entry for updates
                should_update = True
                entry[col_name] = new_val

            if should_update and not self.dry_run:
                if "jansRevision" in entry:
                    entry["jansRevision"] = int(entry["jansRevision"] or 0) + 1

                if not self.persistence.update(table_name, entry["doc_id"], entry):
                    raise RuntimeError(
                        f"FQDN update failed for {table_name}: doc_id={entry['doc_id']}"
                    )

    def modify_configmap(self, old_fqdn: str, new_fqdn: str) -> None:
        logger.info("Checking configmap")

        if new_fqdn != old_fqdn and not self.dry_run:
            logger.info("Updating FQDN in configmap with new value (key=hostname, value=%s)", new_fqdn)

            if self.manager.config.set("hostname", new_fqdn):
                logger.info("FQDN has been changed from %s to %s, please replace TLS certificate to avoid SSL issue", old_fqdn, new_fqdn)

    def change_fqdn(self, old_fqdn, new_fqdn):
        logger.info("Changing FQDN from %s to %s", old_fqdn, new_fqdn)

        for table_name in ["jansAppConf", "jansCustomScr", "jansClnt"]:
            self.modify_persistence_entries(table_name, old_fqdn, new_fqdn)

        self.modify_configmap(old_fqdn, new_fqdn)


class FQDNParamType(click.ParamType):
    name = "fqdn"

    def convert(self, value, param, ctx):
        if value:
            domain = FQDN(value)
            if not domain.is_valid:
                self.fail(f"{value} is not a valid FQDN.", param, ctx)
        return value


fqdn_param_type = FQDNParamType()


@click.command
@click.argument("new_fqdn", type=fqdn_param_type, help="New FQDN to change to")
@click.option(
    "--old-fqdn",
    help="Old FQDN to change from (if omitted, will use FQDN stored in configmap)",
    default="",
    type=fqdn_param_type,
)
@click.option("--dry-run", help="Simulate the operation without persisting changes", is_flag=True)
def change_fqdn(new_fqdn, old_fqdn, dry_run):
    """Change FQDN."""
    manager = get_manager()

    if not old_fqdn:
        old_fqdn = manager.config.get("hostname")
        logger.warning("Detected empty value for old FQDN; the value is now taken from existing configmap: %s", old_fqdn)

    if dry_run:
        logger.warning("The dry run mode is enabled; changes will not be persisted!")

    with manager.create_lock("change-fqdn"):
        domain = Domain(manager, dry_run=dry_run)
        domain.change_fqdn(old_fqdn, new_fqdn)


if __name__ == "__main__":
    change_fqdn(prog_name="change-fqdn")
