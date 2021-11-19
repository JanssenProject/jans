"""
jans.pycloudlib.persistence.spanner
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This module contains classes and functions for interacting
with Google Spanner database.
"""

import logging
import os
from contextlib import suppress

from google.api_core.exceptions import AlreadyExists
from google.api_core.exceptions import NotFound
from google.api_core.exceptions import FailedPrecondition
from google.cloud import spanner

logger = logging.getLogger(__name__)


class SpannerClient:
    """Class to interact with Spanner database.
    """

    def __init__(self):
        """Create instance of Spanner client.

        The following envvars are required:

        - ``GOOGLE_APPLICATION_CREDENTIALS``: Path to JSON file contains
          Google credentials
        - ``GOOGLE_PROJECT_ID``: (a.k.a Google project ID)
        - ``CN_GOOGLE_SPANNER_INSTANCE_ID``: Spanner instance ID
        - ``CN_GOOGLE_SPANNER_DATABASE_ID``: Spanner database ID
        """

        project_id = os.environ.get("GOOGLE_PROJECT_ID", "")
        client = spanner.Client(project=project_id)
        instance_id = os.environ.get("CN_GOOGLE_SPANNER_INSTANCE_ID", "")
        self.instance = client.instance(instance_id)

        database_id = os.environ.get("CN_GOOGLE_SPANNER_DATABASE_ID", "")
        self.database = self.instance.database(database_id)

    def connected(self):
        """Check whether connection is alive by executing simple query.
        """

        cntr = 0
        with self.database.snapshot() as snapshot:
            result = snapshot.execute_sql("SELECT 1")
            with suppress(IndexError):
                row = list(result)[0]
                cntr = row[0]
        return cntr > 0

    def create_table(self, table_name: str, column_mapping: dict, pk_column: str):
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
            self.database.update_ddl([query])
        except FailedPrecondition as exc:
            if "Duplicate name in schema" in exc.args[0]:
                # table exists
                pass
            else:
                raise

    def quoted_id(self, identifier):
        """Get quoted identifier name."""

        char = '`'
        return f"{char}{identifier}{char}"

    def get_table_mapping(self) -> dict:
        """Get mapping of column name and type from all tables.
        """

        type_names = (
            "TYPE_CODE_UNSPECIFIED",
            "BOOL",
            "INT64",
            "FLOAT64",
            "TIMESTAMP",
            "DATE",
            "STRING",
            "BYTES",
            "ARRAY",
            "STRUCT",
            "NUMERIC",
        )

        def parse_field_type(type_):
            name = type_names[type_.code]
            # if name == "ARRAY":
            #     name = f"{name}<{type_names[type_.array_element_type.code]}>"
            return name

        table_mapping = {}
        for table in self.database.list_tables():
            table_mapping[table.table_id] = {
                field.name: parse_field_type(field.type_)
                for field in table.schema
            }
        return table_mapping

    def insert_into(self, table_name, column_mapping):
        """Insert a row into a table."""

        # TODO: handle ARRAY<STRING(MAX)> ?
        def insert_rows(transaction):
            transaction.insert(
                table_name,
                columns=column_mapping.keys(),
                values=[column_mapping.values()]
            )

        with suppress(AlreadyExists):
            self.database.run_in_transaction(insert_rows)

    def row_exists(self, table_name, id_):
        """Check whether a row is exist."""

        exists = False
        with self.database.snapshot() as snapshot:
            result = snapshot.read(
                table=table_name,
                columns=["doc_id"],
                keyset=spanner.KeySet([
                    [id_]
                ]),
                limit=1,
            )
            with suppress(IndexError, NotFound):
                row = list(result)[0]
                if row:
                    exists = True
        return exists

    def create_index(self, query):
        """Create index using raw query."""

        try:
            self.database.update_ddl([query])
        except FailedPrecondition as exc:
            if "Duplicate name in schema" in exc.args[0]:
                # table exists
                pass
            else:
                raise

    def create_subtable(self, table_name: str, sub_table_name: str, column_mapping: dict, pk_column: str, sub_pk_column: str):
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
            self.database.update_ddl([query])
        except FailedPrecondition as exc:
            if "Duplicate name in schema" in exc.args[0]:
                # table exists
                pass
            else:
                raise

    def get(self, table_name, id_, column_names=None) -> dict:
        """Get a row from a table with matching ID."""

        if not column_names:
            # TODO: faster lookup on column names
            column_names = self.get_table_mapping().get(table_name).keys()

        entry = {}

        with self.database.snapshot() as snapshot:
            result = snapshot.read(
                table=table_name,
                columns=column_names,
                keyset=spanner.KeySet([
                    [id_]
                ]),
                limit=1,
            )
            with suppress(IndexError, NotFound):
                row = list(result)[0]
                entry = dict(zip(column_names, row))
        return entry

    def update(self, table_name, id_, column_mapping) -> bool:
        """Update a table row with matching ID."""

        # TODO: handle ARRAY<STRING(MAX)> ?
        def update_rows(transaction):
            # need to add primary key
            column_mapping["doc_id"] = id_
            transaction.update(
                table_name,
                columns=column_mapping.keys(),
                values=[column_mapping.values()]
            )

        modified = False
        with suppress(NotFound):
            self.database.run_in_transaction(update_rows)
            modified = True
        return modified

    def search(self, table_name, column_names=None) -> dict:
        """Get a row from a table with matching ID."""

        if not column_names:
            # TODO: faster lookup on column names
            column_names = self.get_table_mapping().get(table_name).keys()

        with self.database.snapshot() as snapshot:
            result = snapshot.read(
                table=table_name,
                columns=column_names,
                keyset=spanner.KeySet(all_=True),
            )
            for row in result:
                yield dict(zip(column_names, row))


def render_spanner_properties(manager, src: str, dest: str) -> None:
    """Render file contains properties to connect to Spanner database.

    :params manager: An instance of :class:`~jans.pycloudlib.manager._Manager`.
    :params src: Absolute path to the template.
    :params dest: Absolute path where generated file is located.
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
