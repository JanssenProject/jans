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
from jans.pycloudlib.utils import get_password_from_file

if _t.TYPE_CHECKING:  # pragma: no cover
    # imported objects for function type hint, completion, etc.
    # these won't be executed in runtime
    from sqlalchemy.engine import Engine


logger = logging.getLogger(__name__)


class SqlLock(BaseLock):
    def __init__(self) -> None:
        self._metadata: _t.Optional[MetaData] = None
        self._dialect = os.environ.get("CN_SQL_DB_DIALECT", "mysql")

    @cached_property
    def engine(self) -> Engine:
        """Lazy init of engine instance object."""
        return create_engine(self.engine_url, pool_pre_ping=True, hide_parameters=True)

    @property
    def engine_url(self) -> str:
        """Engine connection URL."""
        host = os.environ.get("CN_SQL_DB_HOST", "localhost")

        port = os.environ.get("CN_SQL_DB_PORT", 3306)

        database = os.environ.get("CN_SQL_DB_NAME", "jans")

        user = os.environ.get("CN_SQL_DB_USER", "jans")

        password_file = os.environ.get("CN_SQL_PASSWORD_FILE", "/etc/jans/conf/sql_password")

        password = get_password_from_file(password_file)

        if self._dialect in ("pgsql", "postgresql"):
            connector = "postgresql+psycopg2"
        else:
            connector = "mysql+pymysql"
        return f"{connector}://{user}:{password}@{host}:{port}/{database}"

    @property
    def metadata(self) -> MetaData:
        """Lazy init of metadata."""
        if not self._metadata:
            with warnings.catch_warnings():
                # postgresql driver will show warnings about unsupported reflection
                # on expression-based index, i.e. `lower(uid::text)`; but we don't
                # want to clutter the logs with these warnings, hence we suppress the
                # warnings
                warnings.filterwarnings(
                    "ignore",
                    message="Skipped unsupported reflection of expression-based index",
                    category=SAWarning,
                )

                # do reflection on database table
                self._metadata = MetaData(bind=self.engine)
                self._metadata.reflect()
        return self._metadata

    def _prepare_table(self, table_name) -> None:
        try:
            # prepare table
            Table(
                table_name,
                self.metadata,
                Column("doc_id", String(128), primary_key=True),
                # handle type compatibility with current Janssen by using TEXT instead of JSON/JSONB
                Column("jansData", Text(), default="{}"),
                extend_existing=True,
            )
            self.metadata.create_all(self.engine)

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

        _table = self.metadata.tables.get(table_name)
        if _table is None:
            self._prepare_table(table_name)
            _table = self.metadata.tables.get(table_name)

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

        with self.engine.connect() as conn:
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

        with self.engine.connect() as conn:
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

        with self.engine.connect() as conn:
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

        with self.engine.connect() as conn:
            result = conn.execute(stmt)
            return bool(result.rowcount)

    def connected(self) -> bool:
        """Check if connection is established.

        Returns:
            A boolean to indicate connection is established.
        """
        with self.engine.connect() as conn:
            result = conn.execute("SELECT 1 AS is_alive")
            return bool(result.fetchone()[0] > 0)
