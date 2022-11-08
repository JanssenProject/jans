"""This module contains classes and functions for interacting with Google Spanner database."""

from __future__ import annotations

import hashlib
import json
import logging
import os
import typing as _t
from contextlib import suppress
from functools import cached_property
from tempfile import NamedTemporaryFile

from google.api_core.exceptions import AlreadyExists
from google.api_core.exceptions import NotFound
from google.api_core.exceptions import FailedPrecondition
from google.cloud.spanner_v1 import Client
from google.cloud.spanner_v1.keyset import KeySet
from google.cloud.spanner_v1.param_types import STRING
from ldif import LDIFParser

from jans.pycloudlib.persistence.sql import doc_id_from_dn
from jans.pycloudlib.persistence.sql import SqlSchemaMixin
from jans.pycloudlib.utils import safe_render

if _t.TYPE_CHECKING:  # pragma: no cover
    # imported objects for function type hint, completion, etc.
    # these won't be executed in runtime
    from google.cloud.spanner_v1.database import Database
    from google.cloud.spanner_v1.instance import Instance
    from jans.pycloudlib.manager import Manager


logger = logging.getLogger(__name__)


class SpannerClient(SqlSchemaMixin):
    """Class to interact with Spanner database.

    The following envvars are required:

    - ``GOOGLE_APPLICATION_CREDENTIALS``: Path to JSON file contains Google credentials
    - ``GOOGLE_PROJECT_ID``: (a.k.a Google project ID)
    - ``CN_GOOGLE_SPANNER_INSTANCE_ID``: Spanner instance ID
    - ``CN_GOOGLE_SPANNER_DATABASE_ID``: Spanner database ID

    Args:
        manager: An instance of manager class.
        *args: Positional arguments.
        **kwargs: Keyword arguments.
    """

    def __init__(self, manager: Manager, *args: _t.Any, **kwargs: _t.Any) -> None:
        self.manager = manager
        self.dialect = "spanner"

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

    @cached_property
    def sub_tables(self) -> dict[str, list[list[str]]]:
        """Get a mapping of subtables from pre-defined file."""
        with open("/app/static/rdbm/sub_tables.json") as f:
            return json.loads(f.read()).get(self.dialect, {})  # type: ignore

    def connected(self) -> bool:
        """Check whether connection is alive by executing simple query."""
        cntr = 0
        with self.database.snapshot() as snapshot:  # type: ignore
            result = snapshot.execute_sql("SELECT 1")
            with suppress(IndexError):
                row = list(result)[0]
                cntr = row[0]
        return cntr > 0

    def create_table(self, table_name: str, column_mapping: dict[str, str], pk_column: str) -> None:
        """Create table with its columns."""
        columns = []
        for column_name, column_type in column_mapping.items():
            column_def = f"{self.quoted_id(column_name)} {column_type}"

            if column_name == pk_column:
                column_def += " NOT NULL"
            columns.append(column_def)

        columns_fmt = ", ".join(columns)
        pk_def = f"PRIMARY KEY ({self.quoted_id(pk_column)})"
        query = f"CREATE TABLE {self.quoted_id(table_name)} ({columns_fmt}) {pk_def}"

        try:
            self.database.update_ddl([query])  # type: ignore
        except FailedPrecondition as exc:
            if "Duplicate name in schema" in exc.args[0]:
                # table exists
                pass
            else:
                raise

    def quoted_id(self, identifier: str) -> str:
        """Get quoted identifier name."""
        char = '`'
        return f"{char}{identifier}{char}"

    def get_table_mapping(self) -> dict[str, dict[str, str]]:
        """Get mapping of column name and type from all tables."""
        table_mapping = {}
        for table in self.database.list_tables():  # type: ignore
            with self.database.snapshot() as snapshot:  # type: ignore
                result = snapshot.execute_sql(
                    "select column_name, spanner_type "
                    "from information_schema.columns "
                    "where table_name = @table_name",
                    params={"table_name": table.table_id},
                    param_types={"table_name": STRING},
                )
                table_mapping[table.table_id] = dict(result)
        return table_mapping

    def insert_into(self, table_name: str, column_mapping: dict[str, _t.Any]) -> None:
        """Insert a row into a table."""
        # TODO: handle ARRAY<STRING(MAX)> ?
        def insert_rows(transaction):  # type: ignore
            transaction.insert(
                table_name,
                columns=column_mapping.keys(),
                values=[column_mapping.values()]
            )

        with suppress(AlreadyExists):
            self.database.run_in_transaction(insert_rows)  # type: ignore

    def row_exists(self, table_name: str, id_: str) -> bool:
        """Check whether a row is exist."""
        exists = False
        with self.database.snapshot() as snapshot:  # type: ignore
            result = snapshot.read(
                table=table_name,
                columns=["doc_id"],
                keyset=KeySet([[id_]]),  # type: ignore
                limit=1,
            )
            with suppress(IndexError, NotFound):
                row = list(result)[0]
                if row:
                    exists = True
        return exists

    def create_index(self, query: str) -> None:
        """Create index using raw query."""
        try:
            self.database.update_ddl([query])  # type: ignore
        except FailedPrecondition as exc:
            if "Duplicate name in schema" in exc.args[0]:
                # table exists
                pass
            else:
                raise

    def create_subtable(
        self,
        table_name: str,
        sub_table_name: str,
        column_mapping: dict[str, str],
        pk_column: str,
        sub_pk_column: str
    ) -> None:
        """Create sub table with its columns."""
        columns = []
        for column_name, column_type in column_mapping.items():
            column_def = f"{self.quoted_id(column_name)} {column_type}"

            if column_name == pk_column:
                column_def += " NOT NULL"
            columns.append(column_def)

        columns_fmt = ", ".join(columns)
        pk_def = f"PRIMARY KEY ({self.quoted_id(pk_column)}, {self.quoted_id(sub_pk_column)})"
        query = ", ".join([
            f"CREATE TABLE {self.quoted_id(sub_table_name)} ({columns_fmt}) {pk_def}",
            f"INTERLEAVE IN PARENT {self.quoted_id(table_name)} ON DELETE CASCADE"
        ])

        try:
            self.database.update_ddl([query])  # type: ignore
        except FailedPrecondition as exc:
            if "Duplicate name in schema" in exc.args[0]:
                # table exists
                pass
            else:
                raise

    def get(self, table_name: str, id_: str, column_names: _t.Union[list[str], None] = None) -> dict[str, _t.Any]:
        """Get a row from a table with matching ID."""
        if not column_names:
            # TODO: faster lookup on column names
            col_names = list(self.get_table_mapping().get(table_name, {}).keys())
        else:
            col_names = []

        entry = {}

        with self.database.snapshot() as snapshot:  # type: ignore
            result = snapshot.read(
                table=table_name,
                columns=col_names,
                keyset=KeySet([[id_]]),  # type: ignore
                limit=1,
            )
            with suppress(IndexError, NotFound):
                row = list(result)[0]
                entry = dict(zip(col_names, row))
        return entry

    def update(self, table_name: str, id_: str, column_mapping: dict[str, _t.Any]) -> bool:
        """Update a table row with matching ID."""
        # TODO: handle ARRAY<STRING(MAX)> ?
        def update_rows(transaction):  # type: ignore
            # need to add primary key
            column_mapping["doc_id"] = id_
            transaction.update(
                table_name,
                columns=column_mapping.keys(),
                values=[column_mapping.values()]
            )

        modified = False
        with suppress(NotFound):
            self.database.run_in_transaction(update_rows)  # type: ignore
            modified = True
        return modified

    def search(self, table_name: str, column_names: _t.Union[list[str], None] = None) -> _t.Iterator[dict[str, _t.Any]]:
        """Get all rows from a table."""
        if not column_names:
            # TODO: faster lookup on column names
            col_names = list(self.get_table_mapping().get(table_name, {}).keys())

        with self.database.snapshot() as snapshot:  # type: ignore
            result = snapshot.read(
                table=table_name,
                columns=col_names,
                keyset=KeySet(all_=True),  # type: ignore
            )
            for row in result:
                yield dict(zip(col_names, row))

    def insert_into_subtable(self, table_name: str, column_mapping: dict[str, _t.Any]) -> None:
        """Add new entry into subtable.

        Args:
            table_name: Subtable name.
            column_mapping: Key-value pairs of column name and its value.
        """
        for column, value in column_mapping.items():
            if not self.column_in_subtable(table_name, column):
                continue

            for item in value:
                hashed = hashlib.sha256()
                hashed.update(item.encode())
                dict_doc_id = hashed.digest().hex()

                self.insert_into(
                    f"{table_name}_{column}",
                    {
                        "doc_id": column_mapping["doc_id"],
                        "dict_doc_id": dict_doc_id,
                        column: item
                    },
                )

    def _transform_value(self, key: str, values: _t.Any) -> _t.Any:
        """Transform value from one to another based on its data type.

        Args:
            key: Attribute name.
            values: Pre-transformed values.
        """
        type_ = self.sql_data_types.get(key, {})

        if not type_:
            attr_syntax = self.get_attr_syntax(key)
            type_ = self.sql_data_types_mapping[attr_syntax]

        type_ = type_.get(self.dialect, {})
        data_type = type_.get("type", "")

        if data_type in ("SMALLINT", "BOOL",):
            if values[0].lower() in ("1", "on", "true", "yes", "ok"):
                return 1 if data_type == "SMALLINT" else True
            return 0 if data_type == "SMALLINT" else False

        if data_type == "INT64":
            return int(values[0])

        if data_type in ("DATETIME(3)", "TIMESTAMP",):
            dval = values[0].strip("Z")
            sep = "T"
            postfix = "Z"
            return "{}-{}-{}{}{}:{}:{}{}{}".format(
                dval[0:4],
                dval[4:6],
                dval[6:8],
                sep,
                dval[8:10],
                dval[10:12],
                dval[12:14],
                dval[14:17],
                postfix,
            )

        if data_type == "JSON":
            return {"v": values}

        if data_type == "ARRAY<STRING(MAX)>":
            return values

        # fallback
        return values[0]

    def _data_from_ldif(self, filename: str) -> _t.Iterator[tuple[str, dict[str, _t.Any]]]:
        """Get data from parsed LDIF file.

        Args:
            filename: LDIF filename.
        """
        with open(filename, "rb") as fd:
            parser = LDIFParser(fd)

            for dn, entry in parser.parse():
                doc_id = doc_id_from_dn(dn)

                oc = entry.get("objectClass") or entry.get("objectclass")
                if oc:
                    if "top" in oc:
                        oc.remove("top")

                    if len(oc) == 1 and oc[0].lower() in ("organizationalunit", "organization"):
                        continue

                table_name = oc[-1]

                # remove objectClass
                entry.pop("objectClass", None)
                entry.pop("objectclass", None)

                attr_mapping = {
                    "doc_id": doc_id,
                    "objectClass": table_name,
                    "dn": dn,
                }

                for attr in entry:
                    # TODO: check if attr in sub table
                    value = self._transform_value(attr, entry[attr])
                    attr_mapping[attr] = value
                yield table_name, attr_mapping

    def create_from_ldif(self, filepath: str, ctx: dict[str, _t.Any]) -> None:
        """Create entry with data loaded from an LDIF template file.

        Args:
            filepath: Path to LDIF template file.
            ctx: Key-value pairs of context that rendered into LDIF template file.
        """
        with open(filepath) as src, NamedTemporaryFile("w+") as dst:
            dst.write(safe_render(src.read(), ctx))
            # ensure rendered template is written
            dst.flush()

            for table_name, column_mapping in self._data_from_ldif(dst.name):
                self.insert_into(table_name, column_mapping)
                self.insert_into_subtable(table_name, column_mapping)

    def column_in_subtable(self, table_name: str, column: str) -> bool:
        """Check whether a subtable has certain column.

        Args:
            table_name: Name of the subtable.
            column: Name of the column.
        """
        exists = False

        # column_mapping is a list
        column_mapping = self.sub_tables.get(table_name, [])
        for cm in column_mapping:
            if column == cm[0]:
                exists = True
                break
        return exists


def render_spanner_properties(manager: Manager, src: str, dest: str) -> None:
    """Render file contains properties to connect to Spanner database.

    Args:
        manager: An instance of :class:`~jans.pycloudlib.manager.Manager`.
        src: Absolute path to the template.
        dest: Absolute path where generated file is located.
    """
    with open(src) as f:
        txt = f.read()

    with open(dest, "w") as f:
        if "SPANNER_EMULATOR_HOST" in os.environ:
            emulator_host = os.environ.get("SPANNER_EMULATOR_HOST")
            creds = f"connection.emulator-host={emulator_host}"
        else:
            cred_file = os.environ.get("GOOGLE_APPLICATION_CREDENTIALS", "")
            creds = f"connection.credentials-file={cred_file}"

        rendered_txt = txt % {
            "spanner_project": os.environ.get("GOOGLE_PROJECT_ID", ""),
            "spanner_instance": os.environ.get("CN_GOOGLE_SPANNER_INSTANCE_ID", ""),
            "spanner_database": os.environ.get("CN_GOOGLE_SPANNER_DATABASE_ID", ""),
            "spanner_creds": creds,
        }
        f.write(rendered_txt)
