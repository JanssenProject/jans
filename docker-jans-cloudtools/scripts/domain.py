import logging.config
from contextlib import contextmanager
from dataclasses import dataclass

import click
from fqdn import FQDN

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


@dataclass
class DomainOps:
    # flag to mark whether there is any changes in persistence
    changes_available: bool = False

    # flag to mark whether changes are sucessfully persisted
    changes_persisted: bool = False


class Domain:
    def __init__(self, manager, **kwargs):
        self.manager = manager
        self.persistence = SqlClient(self.manager)
        self.dry_run = kwargs.get("dry_run") or False

    @contextmanager
    def wrapped_ops(self):
        ops = DomainOps()
        try:
            yield ops
        finally:
            ops = None

    def modify_persistence_entries(self, table_name, old_fqdn, new_fqdn, ops):
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

                # mark changes is available
                ops.changes_available = True

                # mark entry for updates
                should_update = True
                entry[col_name] = new_val

            if should_update and not self.dry_run:
                if "jansRevision" in entry:
                    entry["jansRevision"] = int(entry["jansRevision"] or 0) + 1

                if updated := self.persistence.update(table_name, entry["doc_id"], entry):
                    # mark changes are persisted only if update succeed
                    ops.changes_persisted = updated

    def modify_configmap(self, old_fqdn, new_fqdn, ops):
        if ops.changes_available:
            logger.info("Checking configmap")

        if all([
            ops.changes_persisted,
            not self.dry_run,
            new_fqdn != self.manager.config.get("hostname"),
        ]):
            logger.info("Updating FQDN in configmap (key=hostname, value=%s)", new_fqdn)
            if self.manager.config.set("hostname", new_fqdn):
                logger.info("FQDN has been changed from %s to %s. Please rotate certificate(s) to avoid SSL issue.", old_fqdn, new_fqdn)

    def change_fqdn(self, old_fqdn, new_fqdn):
        with self.wrapped_ops() as ops:
            logger.info("Changing FQDN from %s to %s", old_fqdn, new_fqdn)

            for table_name in ["jansAppConf", "jansCustomScr", "jansClnt"]:
                self.modify_persistence_entries(table_name, old_fqdn, new_fqdn, ops)

            self.modify_configmap(old_fqdn, new_fqdn, ops)


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

    domain = Domain(manager, dry_run=dry_run)
    domain.change_fqdn(old_fqdn, new_fqdn)


if __name__ == "__main__":
    change_fqdn(prog_name="change-fqdn")
