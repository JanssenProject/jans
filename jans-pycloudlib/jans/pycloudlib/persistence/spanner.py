"""This module contains classes and functions for interacting with Google Spanner database."""

import hashlib
import json
import logging
import os
from contextlib import suppress
from tempfile import NamedTemporaryFile

from google.api_core.exceptions import AlreadyExists
from google.api_core.exceptions import NotFound
from google.api_core.exceptions import FailedPrecondition
from google.cloud import spanner
from ldif import LDIFParser

from jans.pycloudlib.persistence.sql import doc_id_from_dn
from jans.pycloudlib.utils import safe_render

logger = logging.getLogger(__name__)


class SpannerClient:
    """Class to interact with Spanner database.

    The following envvars are required:

    - ``GOOGLE_APPLICATION_CREDENTIALS``: Path to JSON file contains Google credentials
    - ``GOOGLE_PROJECT_ID``: (a.k.a Google project ID)
    - ``CN_GOOGLE_SPANNER_INSTANCE_ID``: Spanner instance ID
    - ``CN_GOOGLE_SPANNER_DATABASE_ID``: Spanner database ID
    """

    def __init__(self, manager, *args, **kwargs):
        self.manager = manager
        self.dialect = "spanner"
        self.schema_files = [
            "/app/schema/jans_schema.json",
            "/app/schema/custom_schema.json",
        ]

        self._client = None
        self._instance = None
        self._database = None

        self._sql_data_types = {}
        self._sql_data_types_mapping = {}
        self._attr_types = []
        self._opendj_attr_types = {}
        self._sub_tables = {}

    @property
    def client(self):
        """Get an instance Spanner client object."""
        if not self._client:
            project_id = os.environ.get("GOOGLE_PROJECT_ID", "")
            self._client = spanner.Client(project=project_id)
        return self._client

    @property
    def instance(self):
        """Get an instance Spanner instance object."""
        if not self._instance:
            instance_id = os.environ.get("CN_GOOGLE_SPANNER_INSTANCE_ID", "")
            self._instance = self.client.instance(instance_id)
        return self._instance

    @property
    def database(self):
        """Get an instance Spanner database object."""
        if not self._database:
            database_id = os.environ.get("CN_GOOGLE_SPANNER_DATABASE_ID", "")
            self._database = self.instance.database(database_id)
        return self._database

    @property
    def sql_data_types(self):
        """Get list of data types from pre-defined file."""
        if not self._sql_data_types:
            with open("/app/static/rdbm/sql_data_types.json") as f:
                self._sql_data_types = json.loads(f.read())
        return self._sql_data_types

    @property
    def sql_data_types_mapping(self):
        """Get a mapping of data types from pre-defined file."""
        if not self._sql_data_types_mapping:
            with open("/app/static/rdbm/ldap_sql_data_type_mapping.json") as f:
                self._sql_data_types_mapping = json.loads(f.read())
        return self._sql_data_types_mapping

    @property
    def attr_types(self):
        """Get list of attribute types from pre-defined file."""
        if not self._attr_types:
            for fn in self.schema_files:
                with open(fn) as f:
                    schema = json.loads(f.read())
                    self._attr_types += schema["attributeTypes"]
        return self._attr_types

    @property
    def opendj_attr_types(self):
        """Get a mapping of OpenDJ attribute types from pre-defined file."""
        if not self._opendj_attr_types:
            with open("/app/static/rdbm/opendj_attributes_syntax.json") as f:
                self._opendj_attr_types = json.loads(f.read())
        return self._opendj_attr_types

    @property
    def sub_tables(self):
        """Get a mapping of subtables from pre-defined file."""
        if not self._sub_tables:
            with open("/app/static/rdbm/sub_tables.json") as f:
                self._sub_tables = json.loads(f.read()).get(self.dialect) or {}
        return self._sub_tables

    def connected(self):
        """Check whether connection is alive by executing simple query."""
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
        """Get mapping of column name and type from all tables."""
        table_mapping = {}
        for table in self.database.list_tables():
            with self.database.snapshot() as snapshot:
                result = snapshot.execute_sql(
                    f"select column_name, spanner_type "
                    "from information_schema.columns "
                    f"where table_name = '{table.table_id}'"
                )
                table_mapping[table.table_id] = dict(result)
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
        """Get all rows from a table."""
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

    def insert_into_subtable(self, table_name, column_mapping):
        """Add new entry into subtable.

        :param table_name: Subtable name.
        :param column_mapping: Key-value pairs of column name and its value.
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

    def get_attr_syntax(self, attr):
        """Get attribute syntax.

        :param attr: Attribute name.
        """
        for attr_type in self.attr_types:
            if attr not in attr_type["names"]:
                continue
            if attr_type.get("multivalued"):
                return "JSON"
            return attr_type["syntax"]

        # fallback to OpenDJ attribute type
        return self.opendj_attr_types.get(attr) or "1.3.6.1.4.1.1466.115.121.1.15"

    def _transform_value(self, key, values):
        """Transform value from one to another based on its data type.

        :param key: Attribute name.
        :param values: Pre-transformed values.
        """
        type_ = self.sql_data_types.get(key)

        if not type_:
            attr_syntax = self.get_attr_syntax(key)
            type_ = self.sql_data_types_mapping[attr_syntax]

        type_ = type_.get(self.dialect)
        data_type = type_["type"]

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

    def _data_from_ldif(self, filename):
        """Get data from parsed LDIF file.

        :param filename: LDIF filename.
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

    def create_from_ldif(self, filepath, ctx):
        """Create entry with data loaded from an LDIF template file.

        :param filepath: Path to LDIF template file.
        :param ctx: Key-value pairs of context that rendered into LDIF template file.
        """
        with open(filepath) as src, NamedTemporaryFile("w+") as dst:
            dst.write(safe_render(src.read(), ctx))
            # ensure rendered template is written
            dst.flush()

            for table_name, column_mapping in self._data_from_ldif(dst.name):
                self.insert_into(table_name, column_mapping)
                self.insert_into_subtable(table_name, column_mapping)

    def column_in_subtable(self, table_name, column):
        """Check whether a subtable has certain column.

        :param table_name: Name of the subtable.
        :param column: Name of the column.
        """
        exists = False

        # column_mapping is a list
        column_mapping = self.sub_tables.get(table_name, [])
        for cm in column_mapping:
            if column == cm[0]:
                exists = True
                break
        return exists


def render_spanner_properties(manager, src: str, dest: str) -> None:
    """Render file contains properties to connect to Spanner database.

    :param manager: An instance of :class:`~jans.pycloudlib.manager._Manager`.
    :param src: Absolute path to the template.
    :param dest: Absolute path where generated file is located.
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
