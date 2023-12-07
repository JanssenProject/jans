from __future__ import annotations

import json
import logging
import os
import typing as _t
from contextlib import suppress
from functools import cached_property

from google.api_core.exceptions import AlreadyExists
from google.api_core.exceptions import NotFound
from google.cloud.spanner_v1 import Client
from google.cloud.spanner_v1.param_types import STRING

from jans.pycloudlib.lock.base_lock import BaseLock

if _t.TYPE_CHECKING:  # pragma: no cover
    # imported objects for function type hint, completion, etc.
    # these won't be executed in runtime
    from google.cloud.spanner_v1.database import Database
    from google.cloud.spanner_v1.instance import Instance

logger = logging.getLogger(__name__)


class SpannerLock(BaseLock):
    @property
    def table_name(self):
        return "jansOciLock"

    @cached_property
    def client(self) -> Client:
        """Get an instance Spanner client object."""
        project_id = os.environ.get("GOOGLE_PROJECT_ID", "")
        return Client(project=project_id)  # type: ignore

    @cached_property
    def instance(self) -> Instance:
        """Get an instance Spanner instance object."""
        instance_id = os.environ.get("CN_GOOGLE_SPANNER_INSTANCE_ID", "")
        return self.client.instance(instance_id)  # type: ignore

    @cached_property
    def database(self) -> Database:
        """Get an instance Spanner database object."""
        database_id = os.environ.get("CN_GOOGLE_SPANNER_DATABASE_ID", "")
        return self.instance.database(database_id)  # type: ignore

    def _prepare_table(self):
        if not self.database.table(self.table_name).exists():
            # note that JSON type is not supported by current Janssen version
            # hence the jansData type is set as text
            stmt = " ".join([
                f"CREATE TABLE {self.table_name}",
                "(doc_id STRING(128), jansData STRING(MAX))",
                "PRIMARY KEY (doc_id)",
            ])
            self.database.update_ddl([stmt])

    def get(self, key: str) -> dict[str, _t.Any]:
        self._prepare_table()

        columns = ["doc_id", "jansData"]

        with self.database.snapshot() as snapshot:
            result = snapshot.execute_sql(
                f"SELECT * FROM {self.table_name} WHERE doc_id = @key LIMIT 1",  # nosec: B608
                params={"key": key},
                param_types={"key": STRING},
            )
            with suppress(IndexError, NotFound):
                row = list(result)[0]
                entry = dict(zip(columns, row))
                return json.loads(entry["jansData"]) | {"name": entry["doc_id"]}
        return {}

    def post(self, key: str, owner: str, ttl: float, updated_at: str) -> bool:
        self._prepare_table()

        def insert_row(transaction):
            return transaction.execute_update(
                f"INSERT INTO {self.table_name} (doc_id, jansData) VALUES (@key, @data)",  # nosec: B608
                params={
                    "key": key,
                    "data": json.dumps({"owner": owner, "ttl": ttl, "updated_at": updated_at}),
                },
                param_types={
                    "key": STRING,
                    "data": STRING,
                },
            )

        with suppress(AlreadyExists):
            created = self.database.run_in_transaction(insert_row)
            return bool(created)
        return False

    def put(self, key: str, owner: str, ttl: float, updated_at: str) -> bool:
        self._prepare_table()

        def update_row(transaction):
            return transaction.execute_update(
                f"UPDATE {self.table_name} SET jansData = @data WHERE doc_id = @key",  # nosec: B608
                params={
                    "key": key,
                    "data": json.dumps({"owner": owner, "ttl": ttl, "updated_at": updated_at}),
                },
                param_types={
                    "key": STRING,
                    "data": STRING,
                },
            )

        with suppress(NotFound):
            updated = self.database.run_in_transaction(update_row)
            return bool(updated)
        return False

    def delete(self, key: str) -> bool:
        self._prepare_table()

        def delete_row(transaction):
            return transaction.execute_update(
                f"DELETE FROM {self.table_name} WHERE doc_id = @key",  # nosec: B608
                params={"key": key},
                param_types={"key": STRING},
            )
        deleted = self.database.run_in_transaction(delete_row)
        return bool(deleted)

    def connected(self) -> bool:
        """Check if connection is established.

        Returns:
            A boolean to indicate connection is established.
        """
        cntr = 0
        with self.database.snapshot() as snapshot:  # type: ignore
            result = snapshot.execute_sql("SELECT 1")
            with suppress(IndexError):
                row = list(result)[0]
                cntr = row[0]
        return cntr > 0
