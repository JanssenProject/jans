"""This module contains CLI commands for generating configuration specification."""

import json

import click
from apispec import APISpec
from apispec.ext.marshmallow import MarshmallowPlugin

from jans.pycloudlib.schema import ConfigurationSchema
from jans.pycloudlib.version import __version__


@click.command(help="Generate configuration spec")
@click.option(
    "--indent",
    type=int,
    default=4,
)
def configuration_spec(indent):
    spec = APISpec(
        title="Janssen cloud-native configuration",
        version=__version__,
        openapi_version="3.0.2",
        plugins=[MarshmallowPlugin()],
    )
    spec.components.schema("Configuration", schema=ConfigurationSchema)
    click.echo(json.dumps(spec.to_dict(), indent=indent))
