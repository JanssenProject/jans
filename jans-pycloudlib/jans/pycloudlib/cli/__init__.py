"""This module contains helper for CLI."""

import click

from jans.pycloudlib.cli.encoding import decode_file
from jans.pycloudlib.cli.encoding import decode_string
from jans.pycloudlib.cli.spec import configuration_spec


@click.group(
    context_settings={"help_option_names": ["-h", "--help"]}
)
def cli() -> None:  # pragma: no cover
    """Entrypoint of CLI commands."""
    pass


cli.add_command(decode_file)
cli.add_command(decode_string)
cli.add_command(configuration_spec)
