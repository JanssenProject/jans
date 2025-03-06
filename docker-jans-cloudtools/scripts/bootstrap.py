import click

from cleaner import cleanup
from certmanager import certmanager


CONTEXT_SETTINGS = {"help_option_names": ["-h", "--help"]}


@click.group(context_settings=CONTEXT_SETTINGS)
def cli():
    ...


cli.add_command(cleanup)
cli.add_command(certmanager)


if __name__ == "__main__":
    cli(prog_name="cloudtools")
