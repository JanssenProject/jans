"""
jans.pycloudlib.cli
~~~~~~~~~~~~~~~~~~~

This module contains helper for CLI.

"""

import click

from jans.pycloudlib.cli.encoding import decode_file


@click.group(
    context_settings={"help_option_names": ["-h", "--help"]}
)
def cli():
    pass


cli.add_command(decode_file)
