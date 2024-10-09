import json

import click
from apispec import APISpec
from apispec.ext.marshmallow import MarshmallowPlugin

from jans.pycloudlib.version import __version__
from jans.pycloudlib.schema import ConfigurationSchema


@click.command(help="generate configuration schema specification")
def configuration_spec():
    spec = APISpec(
        title="Janssen cloud-native configuration",
        version=__version__,
        openapi_version="3.0.2",
        info={},
        plugins=[MarshmallowPlugin()],
    )
    spec.components.schema("Configuration", schema=ConfigurationSchema)
    click.echo(json.dumps(spec.to_dict()))
