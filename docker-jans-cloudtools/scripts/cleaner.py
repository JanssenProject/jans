import logging.config
import time

import click
from sqlalchemy import text

from jans.pycloudlib import get_manager
from jans.pycloudlib.persistence.sql import SqlClient

from settings import LOGGING_CONFIG

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("cloudtools")


@click.command
@click.option(
    "--limit",
    help="How many expired entries need to be removed per table",
    default=1000,
    show_default=True,
)
def cleanup(limit):
    """Cleanup expired entries in persistence."""
    manager = get_manager()
    client = SqlClient(manager)

    logger.info(f"Running cleanup process (up to {limit} entries)")
    start_time = time.time()

    with client.engine.connect() as conn:
        for table, cols in client.get_table_mapping().items():
            if not all(["del" in cols, "exp" in cols]):
                continue

            try:
                if client.dialect == "mysql":
                    query = text(f"DELETE FROM {client.quoted_id(table)} WHERE del = :deleted AND exp < NOW() LIMIT {limit}")  # nosec: B608
                else:  # likely postgres
                    query = text(f"DELETE FROM {client.quoted_id(table)} WHERE doc_id IN (SELECT doc_id FROM {client.quoted_id(table)} WHERE del = :deleted AND exp < NOW() LIMIT {limit})")  # nosec: B608
                conn.execute(query, {"deleted": True})
                logger.info(f"Cleanup expired entries in {table}")
            except Exception as exc:
                logger.warning(f"Unable to cleanup expired entries in {table}; reason={exc}")

    finish_time = time.time()
    logger.info(f"Cleanup process finished after {(finish_time - start_time):0.2f} seconds")


if __name__ == "__main__":
    cleanup(prog_name="cleanup")
