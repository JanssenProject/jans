import json
import logging.config
import re
from collections import defaultdict
from pathlib import Path

from jans.pycloudlib.persistence.spanner import SpannerClient

from settings import LOGGING_CONFIG
from utils import prepare_template_ctx
from hooks import get_ldif_mappings_hook

FIELD_RE = re.compile(r"[^0-9a-zA-Z\s]+")

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("persistence-loader")


class SpannerBackend:
    def __init__(self, manager):
        self.manager = manager
        self.client = SpannerClient(manager)

        index_fn = "spanner_index.json"
        with open(f"/app/static/rdbm/{index_fn}") as f:
            self.sql_indexes = json.loads(f.read())

        # add missing index determined from opendj indexes
        with open("/app/static/opendj/index.json") as f:
            opendj_indexes = [attr["attribute"] for attr in json.loads(f.read())]

        for attr in self.client.attr_types:
            if not attr.get("multivalued"):
                continue
            for attr_name in attr["names"]:
                if attr_name in opendj_indexes and attr_name not in self.sql_indexes["__common__"]["fields"]:
                    self.sql_indexes["__common__"]["fields"].append(attr_name)

    def get_data_type(self, attr, table=None):
        # check from SQL data types first
        for col in [f"{table}:{attr}", attr]:
            type_def = self.client.sql_data_types.get(col)

            if not type_def:
                continue

            type_ = type_def.get(self.client.dialect)

            if not type_:
                continue

            if table in type_.get("tables", {}):
                type_ = type_["tables"][table]

            data_type = type_["type"]
            if "size" in type_:
                data_type = f"{data_type}({type_['size']})"
            return data_type

        # probably JSON-like data type
        if attr in self.client.sql_json_types:
            return self.client.sql_json_types[attr][self.client.dialect]["type"]

        # data type is undefined, hence check from syntax
        syntax = self.client.get_attr_syntax(attr)
        syntax_def = self.client.sql_data_types_mapping[syntax]
        type_ = syntax_def.get(self.client.dialect)

        char_type = "STRING"

        if type_["type"] != char_type:
            # not STRING
            data_type = type_["type"]
        else:
            size = type_.get("size") or "MAX"
            data_type = f"{char_type}({size})"

        return data_type

    def create_tables(self):
        table_columns = self.table_mapping_from_schema()

        for table, attr_mapping in table_columns.items():
            self.client.create_table(table, attr_mapping, "doc_id")

        # for name, attr in attrs.items():
        #     table = attr.get("sql", {}).get("add_table")
        #     logger.info(name)
        #     logger.info(table)
        #     if not table:
        #         continue

        #     data_type = self.get_data_type(name, table)
        #     col_def = f"{attr} {data_type}"

        #     sql_cmd = f"ALTER TABLE {table} ADD {col_def};"
        #     logger.info(sql_cmd)

    def get_index_fields(self, table_name):
        fields = self.sql_indexes.get(table_name, {}).get("fields", [])
        fields += self.sql_indexes["__common__"]["fields"]

        # make unique fields
        return list(set(fields))

    def create_spanner_indexes(self, table_name: str, column_mapping: dict):
        fields = self.get_index_fields(table_name)

        for column_name, column_type in column_mapping.items():
            if column_name == "doc_id" or column_name not in fields:
                continue

            index_name = f"{table_name}_{FIELD_RE.sub('_', column_name)}"

            if not column_type.lower().startswith("array"):
                query = f"CREATE INDEX {self.client.quoted_id(index_name)} ON {self.client.quoted_id(table_name)} ({self.client.quoted_id(column_name)})"
                self.client.create_index(query)
            else:
                # TODO: how to create index for ARRAY?
                pass

        custom_indexes = self.sql_indexes.get(table_name, {}).get("custom", [])
        for i, custom in enumerate(custom_indexes, start=1):
            name = f"{table_name}_CustomIdx_{i}"
            query = f"CREATE INDEX {self.client.quoted_id(name)} ON {self.client.quoted_id(table_name)} ({custom})"
            self.client.create_index(query)

    def create_indexes(self):
        for table_name, column_mapping in self.client.get_table_mapping().items():
            # run the callback
            self.create_spanner_indexes(table_name, column_mapping)

    def create_unique_indexes(self):
        for table_name, column in [
            ("jansPerson", "mail"),
            ("jansPerson", "uid"),
        ]:
            index_name = f"{table_name.lower()}_{column.lower()}_unique_idx"
            query = f"CREATE UNIQUE INDEX {self.client.quoted_id(index_name)} ON {self.client.quoted_id(table_name)} ({self.client.quoted_id(column)})"
            self.client.create_index(query)

    def import_builtin_ldif(self, ctx):
        optional_scopes = json.loads(self.manager.config.get("optional_scopes", "[]"))
        ldif_mappings = get_ldif_mappings_hook("spanner", optional_scopes)

        for _, files in ldif_mappings.items():
            for file_ in files:
                self._import_ldif(f"/app/templates/{file_}", ctx)

    def initialize(self):
        logger.info("Creating tables (if not exist)")
        self.create_tables()
        self.create_subtables()

        logger.info("Updating schema (if required)")
        self.update_schema()

        logger.info("Creating indexes (if not exist)")
        self.create_indexes()
        self.create_unique_indexes()

        ctx = prepare_template_ctx(self.manager)

        logger.info("Importing builtin LDIF files")
        self.import_builtin_ldif(ctx)

        logger.info("Importing custom LDIF files (if any)")
        self.import_custom_ldif(ctx)

    def create_subtables(self):
        for table_name, columns in self.client.sub_tables.items():
            for column_name, column_type in columns:
                subtable_name = f"{table_name}_{column_name}"
                self.client.create_subtable(
                    table_name,
                    subtable_name,
                    {
                        "doc_id": "STRING(64)",
                        "dict_doc_id": "STRING(64)",
                        column_name: column_type,
                    },
                    "doc_id",
                    "dict_doc_id",
                )

                index_name = f"{subtable_name}Idx"
                query = f"CREATE INDEX {self.client.quoted_id(index_name)} ON {self.client.quoted_id(subtable_name)} ({self.client.quoted_id(column_name)})"
                self.client.create_index(query)

    def update_schema(self):
        """Updates schema (may include data migration)"""

        table_mapping = self.client.get_table_mapping()

        def column_to_array(table_name, col_name):
            old_data_type = table_mapping[table_name][col_name]
            data_type = self.get_data_type(col_name, table_name)

            if data_type == old_data_type:
                return

            # get the value first before updating column type
            values = {
                row["doc_id"]: row[col_name]
                for row in self.client.search(table_name, ["doc_id", col_name])
            }

            # to change the storage format of a JSON column, drop the column and
            # add the column back specifying the new storage format
            self.client.database.update_ddl([
                f"ALTER TABLE {self.client.quoted_id(table_name)} DROP COLUMN {self.client.quoted_id(col_name)}"
            ])
            self.client.database.update_ddl([
                f"ALTER TABLE {self.client.quoted_id(table_name)} ADD COLUMN {self.client.quoted_id(col_name)} {data_type}"
            ])

            # pre-populate the modified column
            for doc_id, value in values.items():
                if not value:
                    value_list = []
                else:
                    value_list = [value]

                self.client.update(
                    table_name,
                    doc_id,
                    {col_name: self.client._transform_value(col_name, value_list)}
                )

        def add_column(table_name, col_name):
            if col_name in table_mapping[table_name]:
                return

            data_type = self.get_data_type(col_name, table_name)
            self.client.database.update_ddl([
                f"ALTER TABLE {self.client.quoted_id(table_name)} ADD COLUMN {self.client.quoted_id(col_name)} {data_type}"
            ])

        def change_column_type(table_name, col_name):
            old_data_type = table_mapping[table_name][col_name]
            data_type = self.get_data_type(col_name, table_name)

            if data_type == old_data_type:
                return

            query = f"ALTER TABLE {self.client.quoted_id(table_name)} " \
                    f"ALTER COLUMN {self.client.quoted_id(col_name)} {data_type}"
            self.client.database.update_ddl([query])

        def column_from_array(table_name, col_name):
            old_data_type = table_mapping[table_name][col_name]
            data_type = self.get_data_type(col_name, table_name)

            if data_type == old_data_type:
                return

            # get the value first before updating column type
            values = {
                row["doc_id"]: row[col_name]
                for row in self.client.search(table_name, ["doc_id", col_name])
            }

            # to change the storage format of a JSON column, drop the column and
            # add the column back specifying the new storage format
            self.client.database.update_ddl([
                f"ALTER TABLE {self.client.quoted_id(table_name)} DROP COLUMN {self.client.quoted_id(col_name)}"
            ])
            self.client.database.update_ddl([
                f"ALTER TABLE {self.client.quoted_id(table_name)} ADD COLUMN {self.client.quoted_id(col_name)} {data_type}"
            ])

            # pre-populate the modified column
            for doc_id, value in values.items():
                # pass the list as its value and let transform_value
                # determines the actual value
                if value:
                    new_value = value
                else:
                    new_value = [""]
                self.client.update(
                    table_name,
                    doc_id,
                    {col_name: self.client._transform_value(col_name, new_value)}
                )

        def column_int_to_string(table_name, col_name):
            old_data_type = table_mapping[table_name][col_name]
            data_type = self.get_data_type(col_name, table_name)

            if data_type == old_data_type:
                return

            # get the value first before updating column type
            values = {
                row["doc_id"]: row[col_name]
                for row in self.client.search(table_name, ["doc_id", col_name])
            }

            # to change the storage format of a JSON column, drop the column and
            # add the column back specifying the new storage format
            self.client.database.update_ddl([
                f"ALTER TABLE {self.client.quoted_id(table_name)} DROP COLUMN {self.client.quoted_id(col_name)}"
            ])
            self.client.database.update_ddl([
                f"ALTER TABLE {self.client.quoted_id(table_name)} ADD COLUMN {self.client.quoted_id(col_name)} {data_type}"
            ])

            # pre-populate the modified column
            for doc_id, value in values.items():
                # pass the list as its value and let transform_value
                # determines the actual value
                if value:
                    new_value = [value]
                else:
                    new_value = [""]

                self.client.update(
                    table_name,
                    doc_id,
                    {col_name: self.client._transform_value(col_name, new_value)}
                )

        table_columns = self.table_mapping_from_schema()
        multivalued_type = "ARRAY<STRING(MAX)>"

        for table_name, columns in table_columns.items():
            for column, data_type in columns.items():
                if column not in table_mapping[table_name]:
                    logger.info(f"Adding new column {table_name}.{column}")
                    add_column(table_name, column)

                else:
                    old_data_type = table_mapping[table_name][column]

                    if any([
                        # same type
                        data_type == old_data_type,
                        # builtin columns
                        column in ("doc_id", "objectClass", "dn"),
                    ]):
                        # no-ops
                        continue

                    if data_type.startswith("STRING") and old_data_type == "INT64":
                        # change int64 to string
                        logger.info(f"Converting {table_name}.{column} column type {old_data_type} to {data_type}")
                        column_int_to_string(table_name, column)
                    elif data_type != multivalued_type and old_data_type != multivalued_type:
                        # change non-multivalued type
                        logger.info(f"Converting {table_name}.{column} column type from {old_data_type} to {data_type}")
                        change_column_type(table_name, column)
                    elif data_type == multivalued_type and old_data_type != multivalued_type:
                        # change type to multivalued
                        logger.info(f"Converting {table_name}.{column} column type from {old_data_type} to multivalued {data_type}")
                        column_to_array(table_name, column)
                    elif data_type != multivalued_type and old_data_type == multivalued_type:
                        # change type from multivalued
                        logger.info(f"Converting {table_name}.{column} column type from multivalued {old_data_type} to {data_type}")
                        column_from_array(table_name, column)

    def import_custom_ldif(self, ctx):
        custom_dir = Path("/app/custom_ldif")

        for file_ in custom_dir.rglob("*.ldif"):
            self._import_ldif(file_, ctx)

    def _import_ldif(self, path, ctx):
        logger.info(f"Importing {path} file")
        self.client.create_from_ldif(path, ctx)

    def table_mapping_from_schema(self):
        schemas = {}
        attrs = {}
        # cached schemas that holds table's column and its type
        table_mapping = defaultdict(dict)

        for fn in self.client.schema_files:
            with open(fn) as f:
                schema = json.loads(f.read())

            for oc in schema["objectClasses"]:
                schemas[oc["names"][0]] = oc

            for attr in schema["attributeTypes"]:
                attrs[attr["names"][0]] = attr

        for table, oc in schemas.items():
            if oc.get("sql", {}).get("ignore"):
                continue

            # ``oc["may"]`` contains list of attributes
            if "sql" in oc:
                oc["may"] += oc["sql"].get("include", [])

                for inc_oc in oc["sql"].get("includeObjectClass", []):
                    oc["may"] += schemas[inc_oc]["may"]

            doc_id_type = self.get_data_type("doc_id", table)
            table_mapping[table].update({
                "doc_id": doc_id_type,
                "objectClass": "STRING(48)",
                "dn": "STRING(128)",
            })

            # make sure ``oc["may"]`` doesn't have duplicate attribute
            for attr in set(oc["may"]):
                data_type = self.get_data_type(attr, table)
                table_mapping[table].update({attr: data_type})
        return table_mapping
