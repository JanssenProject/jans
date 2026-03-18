import logging.config
import time

import click
from sqlalchemy import text
from sqlalchemy.exc import SQLAlchemyError

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
    type=int,
)
def cleanup(limit):
    """Cleanup expired entries in persistence."""
    manager = get_manager()
    client = SqlClient(manager)

    logger.info("Running cleanup process (up to %s entries)", limit)
    start_time = time.time()

    with client.engine.connect() as conn:
        with conn.begin():
            for table, cols in client.get_table_mapping().items():
                if not all(["del" in cols, "exp" in cols]):
                    continue

                try:
                    if client.dialect == "mysql":
                        query = f"DELETE FROM {client.quoted_id(table)} WHERE del = :deleted AND exp < NOW() LIMIT {limit}"  # nosec: B608
                    else:  # likely postgres
                        query = f"DELETE FROM {client.quoted_id(table)} WHERE doc_id IN (SELECT doc_id FROM {client.quoted_id(table)} WHERE del = :deleted AND exp < NOW() LIMIT {limit})"  # nosec: B608
                    conn.execute(text(query), {"deleted": True})
                    logger.info("Cleanup expired entries in %s", table)
                except SQLAlchemyError as exc:
                    logger.warning("Unable to cleanup expired entries in %s; reason=%s", table, exc)

    finish_time = time.time()
    logger.info("Cleanup process finished after %0.2f seconds", finish_time - start_time)


if __name__ == "__main__":
    cleanup(prog_name="cleanup")
