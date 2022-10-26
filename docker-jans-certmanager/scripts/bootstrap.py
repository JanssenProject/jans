import logging
import logging.config
from contextlib import suppress

import click

from jans.pycloudlib import get_manager

from settings import LOGGING_CONFIG
from ldap_handler import LdapHandler
from auth_handler import AuthHandler
# from oxshibboleth_handler import OxshibbolethHandler
# from passport_handler import PassportHandler
from web_handler import WebHandler

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("certmanager")

#: Map between service name and its handler class
PATCH_SERVICE_MAP = {
    "web": WebHandler,
    # "oxshibboleth": OxshibbolethHandler,
    "auth": AuthHandler,
    "ldap": LdapHandler,
    # "passport": PassportHandler,
}

PRUNE_SERVICE_MAP = {
    "auth": AuthHandler,
}


def _parse_opts(opts):
    parsed_opts = {}
    for opt in opts:
        with suppress(ValueError):
            k, v = opt.split(":", 1)
            if k and v:
                parsed_opts[k] = v
    return parsed_opts


# ============
# CLI commands
# ============

CONTEXT_SETTINGS = dict(help_option_names=['-h', '--help'])


@click.group(context_settings=CONTEXT_SETTINGS)
def cli():
    pass


@cli.command()
@click.argument("service", type=click.Choice(PATCH_SERVICE_MAP.keys()))
@click.option("--dry-run", help="Enable dryrun mode.", is_flag=True)
@click.option(
    "--opts",
    help="Options for targeted service (can be set multiple times).",
    multiple=True,
    metavar="KEY:VALUE",
)
def patch(service, dry_run, opts):
    """Patch cert and/or crypto keys for the targeted service.
    """
    manager = get_manager()

    if dry_run:
        logger.warning("Dry-run mode is enabled!")

    logger.info(f"Processing updates for service {service}")
    parsed_opts = _parse_opts(opts)
    callback_cls = PATCH_SERVICE_MAP[service]
    callback_cls(manager, dry_run, **parsed_opts).patch()


@cli.command()
@click.argument("service", type=click.Choice(PRUNE_SERVICE_MAP.keys()))
@click.option("--dry-run", help="Enable dryrun mode.", is_flag=True)
@click.option(
    "--opts",
    help="Options for targeted service (can be set multiple times).",
    multiple=True,
    metavar="KEY:VALUE",
)
def prune(service, dry_run, opts):
    """Cleanup expired crypto keys for the targeted service.
    """
    manager = get_manager()

    if dry_run:
        logger.warning("Dry-run mode is enabled!")

    logger.info(f"Processing updates for service {service}")
    parsed_opts = _parse_opts(opts)
    callback_cls = PRUNE_SERVICE_MAP[service]
    callback_cls(manager, dry_run, **parsed_opts).prune()


if __name__ == "__main__":
    cli(prog_name="certmanager")
