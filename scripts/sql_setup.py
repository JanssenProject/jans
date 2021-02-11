import json
import logging.config
import os
import re

from sqlalchemy.exc import OperationalError

from jans.pycloudlib.persistence.sql import SQLClient

from settings import LOGGING_CONFIG

FIELD_RE = re.compile(r"[^0-9a-zA-Z\s]+")

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("entrypoint")


class SQLBackend:
    def __init__(self, manager):
        self.manager = manager

        self.db_dialect = os.environ.get("CN_SQL_DB_DIALECT", "mysql")
        self.db_name = os.environ.get("CN_SQL_DB_NAME", "jans")

        self.client = SQLClient()

        with open("/app/static/sql/sql_data_types.json") as f:
            self.sql_data_types = json.loads(f.read())

        self.attr_types = []
        for file_ in ("jans_schema.json", "custom_schema.json"):
            fn = f"/app/static/{file_}"
            with open(fn) as f:
                schema = json.loads(f.read())
            self.attr_types += schema["attributeTypes"]

        with open("/app/static/sql/opendj_attributes_syntax.json") as f:
            self.opendj_attr_types = json.loads(f.read())

        with open("/app/static/sql/ldap_sql_data_type_mapping.json") as f:
            self.sql_data_types_mapping = json.loads(f.read())

        with open("/app/static/sql/sql_index.json") as f:
            self.sql_indexes = json.loads(f.read())

    def get_attr_syntax(self, attr):
        for attr_type in self.attr_types:
            if attr not in attr_type["names"]:
                continue
            if attr_type.get("multivalued"):
                return "JSON"
            return attr_type["syntax"]

        # fallback to OpenDJ attribute type
        return self.opendj_attr_types.get(attr) or "1.3.6.1.4.1.1466.115.121.1.15"

    def get_data_type(self, attr):
        if attr in self.sql_data_types:
            type_ = self.sql_data_types[attr]

            if type_[self.db_dialect]["type"] == "VARCHAR":
                if type_[self.db_dialect]["size"] <= 127:
                    data_type = f"VARCHAR({type_[self.db_dialect]['size']})"
                elif type_[self.db_dialect]["size"] <= 255:
                    data_type = "TINYTEXT"
                else:
                    data_type = "TEXT"
            else:
                data_type = type_[self.db_dialect]["type"]

        else:
            syntax = self.get_attr_syntax(attr)
            type_ = self.sql_data_types_mapping.get(syntax)

            if type_[self.db_dialect]["type"] == "VARCHAR":
                data_type = f"VARCHAR({type_[self.db_dialect]['size']})"
            else:
                data_type = type_[self.db_dialect]["type"]
        return data_type

    def create_tables(self, conn):
        for file_ in ("jans_schema.json", "custom_schema.json"):
            fn = f"/app/static/{file_}"

            with open(fn) as f:
                schema = json.loads(f.read())

            for oc in schema["objectClasses"]:
                table = oc["names"][0]
                table_cols = []

                for attr in oc["may"]:
                    data_type = self.get_data_type(attr)
                    col_def = f"`{attr}` {data_type}"

                    if data_type.lower() == "json":
                        col_def = f"""{col_def} NOT NULL DEFAULT ('{{"v": []}}')"""
                    table_cols.append(col_def)

                sql_cmd = f"CREATE TABLE `{table}` (`id` INT NOT NULL auto_increment, `doc_id` VARCHAR(48) NOT NULL UNIQUE, `objectClass` VARCHAR(48), `dn` VARCHAR(128), {', '.join(table_cols)}, PRIMARY KEY (`id`, `doc_id`));"

                try:
                    conn.execute(sql_cmd)
                except OperationalError as exc:
                    # the following code should be ignored
                    # - 1050: table exists
                    if exc.orig.args[0] in (1050,):
                        continue
                    logger.warning(f"Failed to execute query; reason={exc.orig.args}")

    def create_indexes(self, conn):
        for table in self.sql_indexes[self.db_dialect]:
            for field in self.sql_indexes[self.db_dialect][table]["fields"]:
                field_sub = FIELD_RE.sub("_", field)
                sql_cmd = f"ALTER TABLE {self.db_name}.{table} ADD INDEX `{table}_{field_sub}` (`{field}`);"

                try:
                    conn.execute(sql_cmd)
                except OperationalError as exc:
                    # the following code should be ignored
                    # - 1061: duplicate key name
                    if exc.orig.args[0] in (1061,):
                        continue
                    logger.warning(f"Failed to execute query; reason={exc.orig.args}")

            for i, custom in enumerate(self.sql_indexes[self.db_dialect][table]["custom"]):
                sql_cmd = f"ALTER TABLE {self.db_name}.{table} ADD INDEX `{table}_{i}`(({custom}));"

                try:
                    conn.execute(sql_cmd)
                except OperationalError as exc:
                    # the following code should be ignored
                    # - 1061: duplicate key name
                    if exc.orig.args[0] in (1061,):
                        continue
                    logger.warning(f"Failed to execute query; reason={exc.orig.args}")

    def initialize(self):
        with self.client.engine.connect() as conn:
            self.create_tables(conn)
            self.create_indexes(conn)
