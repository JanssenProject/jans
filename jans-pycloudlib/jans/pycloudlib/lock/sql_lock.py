from __future__ import annotations

import json
import logging
import os
import typing as _t
import warnings
from functools import cached_property

from sqlalchemy import create_engine
from sqlalchemy import MetaData
from sqlalchemy import Table
from sqlalchemy import Column
from sqlalchemy import String
from sqlalchemy import Text
from sqlalchemy.exc import SAWarning
from sqlalchemy.exc import IntegrityError
from sqlalchemy.exc import DatabaseError
from sqlalchemy.sql import select

from jans.pycloudlib.lock.base_lock import BaseLock
from jans.pycloudlib.persistence.sql import SqlClient
from jans.pycloudlib.utils import get_password_from_file

if _t.TYPE_CHECKING:  # pragma: no cover
    # imported objects for function type hint, completion, etc.
    # these won't be executed in runtime
    from sqlalchemy.engine import Engine


logger = logging.getLogger(__name__)


class SqlLock(BaseLock):
    def __init__(self, manager) -> None:
        self.client = SqlClient(manager)

    def _prepare_table(self, table_name) -> None:
        try:
            # prepare table
            Table(
                table_name,
                self.client.metadata,
                Column("doc_id", String(128), primary_key=True),
                # handle type compatibility with current Janssen by using TEXT instead of JSON/JSONB
                Column("jansData", Text(), default="{}"),
                extend_existing=True,
            )
            self.client.metadata.create_all(self.client.engine)

        except DatabaseError as exc:
            raise_on_error = False

            # if error is not about duplicated table, force raising exception
            if self._dialect in ("pgsql", "postgresql") and exc.orig.pgcode != "42P07":
                raise_on_error = True
            elif self._dialect == "mysql" and exc.orig.args[0] != 1050:
                raise_on_error = True

            if raise_on_error:
                raise exc

    @property
    def table(self):
        """Get table object."""
        table_name = "jansOciLock"

        _table = self.client.metadata.tables.get(table_name)
        if _table is None:
            self._prepare_table(table_name)
            _table = self.client.metadata.tables.get(table_name)

        # underlying table object
        return _table  # noqa: R504

    def get(self, key: str) -> dict[str, _t.Any]:
        """Get specific lock.

        Args:
            key: Lock name.

        Returns:
            Mapping of lock data.
        """
        stmt = select([self.table]).where(self.table.c.doc_id == key).limit(1)

        with self.client.engine.connect() as conn:
            result = conn.execute(stmt)
            entry = result.fetchone()

            if entry:
                rowset = dict(entry)
                return json.loads(rowset["jansData"]) | {"name": rowset["doc_id"]}
        return {}

    def post(self, key: str, owner: str, ttl: float, updated_at: str) -> bool:
        """Create specific lock.

        Args:
            key: Lock name.
            owner: Lock owner.
            ttl: Duration of lock before expire.
            updated_at: Timestamp (datetime format) string.

        Returns:
            A boolean to mark to indicate lock is created.
        """
        stmt = self.table.insert().values(
            doc_id=key,
            jansData=json.dumps({"owner": owner, "ttl": ttl, "updated_at": updated_at}),
        )

        with self.client.engine.connect() as conn:
            try:
                result = conn.execute(stmt)
                created = bool(result.inserted_primary_key)
            except IntegrityError:
                created = False
            return created

    def put(self, key: str, owner: str, ttl: float, updated_at: str) -> bool:
        """Update specific lock.

        Args:
            key: Lock name.
            owner: Lock owner.
            ttl: Duration of lock before expire.
            updated_at: Timestamp (datetime format) string.

        Returns:
            A boolean to mark to indicate lock is updated.
        """
        stmt = self.table.update().where(self.table.c.doc_id == key).values(
            jansData=json.dumps({"owner": owner, "ttl": ttl, "updated_at": updated_at}),
        )

        with self.client.engine.connect() as conn:
            result = conn.execute(stmt)
            return bool(result.rowcount)

    def delete(self, key: str) -> bool:
        """Delete specific lock.

        Args:
            key: Lock name.

        Returns:
            A boolean to mark to indicate lock has been deleted.
        """
        stmt = self.table.delete().where(self.table.c.doc_id == key)

        with self.client.engine.connect() as conn:
            result = conn.execute(stmt)
            return bool(result.rowcount)

    def connected(self) -> bool:
        """Check if connection is established.

        Returns:
            A boolean to indicate connection is established.
        """
        with self.client.engine.connect() as conn:
            result = conn.execute("SELECT 1 AS is_alive")
            return bool(result.fetchone()[0] > 0)
