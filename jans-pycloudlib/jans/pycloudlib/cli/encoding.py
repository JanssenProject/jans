"""This module contains CLI commands for encoding/decoding text."""

import click

from jans.pycloudlib import get_manager
from jans.pycloudlib.utils import decode_text


@click.command(help="Decode text from a file")
@click.argument(
    "path",
    type=click.Path(True, resolve_path=True, allow_dash=True),
)
@click.option(
    "--salt-file",
    type=click.Path(True, resolve_path=True, allow_dash=True),
    help="Read salt from file",
)
@click.option(
    "--salt-literal",
    help="Salt string (overrides salt from secrets/file)",
)
def decode_file(path: str, salt_file: str, salt_literal: str) -> None:
    """Decode text from a file."""
    salt = ""
    if salt_literal:
        salt = salt_literal
    elif salt_file:
        with click.open_file(salt_file, "r") as f:
            salt = f.read().split(" = ")[-1].strip()
    else:
        manager = get_manager()
        try:
            salt = manager.secret.get("encoded_salt")
        except Exception as exc:  # noqa: B902
            click.echo(f"Unable to get salt from secrets; reason={exc}")

    if not salt:
        raise click.Abort()

    with click.open_file(path, "r") as f:
        try:
            txt = decode_text(f.read(), salt)
            click.echo(txt)
        except ValueError as exc:
            raise click.ClickException(f"Unable to decode file {path}; reason={exc}")


@click.command(help="Decode text string")
@click.argument("text")
@click.option(
    "--salt-file",
    type=click.Path(True, resolve_path=True, allow_dash=True),
    help="Read salt from file",
)
@click.option(
    "--salt-literal",
    help="Salt string (overrides salt from secrets/file)",
)
def decode_string(text: str, salt_file: str, salt_literal: str) -> None:
    """Decode text string."""
    salt = ""
    if salt_literal:
        salt = salt_literal
    elif salt_file:
        with click.open_file(salt_file, "r") as f:
            salt = f.read().split(" = ")[-1].strip()
    else:
        manager = get_manager()
        try:
            salt = manager.secret.get("encoded_salt")
        except Exception as exc:  # noqa: B902
            click.echo(f"Unable to get salt from secrets; reason={exc}")

    if not salt:
        raise click.Abort()

    try:
        txt = decode_text(text, salt)
        click.echo(txt)
    except ValueError as exc:
        raise click.ClickException(f"Unable to decode given string; reason={exc}")
