import logging
import logging.config

import click

from jans.pycloudlib import get_manager

from settings import LOGGING_CONFIG
from ldap_handler import LdapHandler
from auth_handler import AuthHandler
from client_api_handler import ClientApiHandler
# from oxshibboleth_handler import OxshibbolethHandler
# from passport_handler import PassportHandler
from scim_handler import ScimHandler
from web_handler import WebHandler

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("certmanager")

#: Map between service name and its handler class
PATCH_SERVICE_MAP = {
    "web": WebHandler,
    # "oxshibboleth": OxshibbolethHandler,
    "auth": AuthHandler,
    "client-api": ClientApiHandler,
    "ldap": LdapHandler,
    # "passport": PassportHandler,
    "scim": ScimHandler,
}

PRUNE_SERVICE_MAP = {
    "auth": AuthHandler,
}


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

    _opts = {}
    for opt in opts:
        try:
            k, v = opt.split(":", 1)
            _opts[k] = v
        except ValueError:
            k = opt
            v = ""

    callback_cls = PATCH_SERVICE_MAP[service]
    callback_cls(manager, dry_run, **_opts).patch()


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

    _opts = {}
    for opt in opts:
        try:
            k, v = opt.split(":", 1)
            _opts[k] = v
        except ValueError:
            k = opt
            v = ""

    callback_cls = PRUNE_SERVICE_MAP[service]
    callback_cls(manager, dry_run, **_opts).prune()


if __name__ == "__main__":
    cli(prog_name="certmanager")
