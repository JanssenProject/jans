import itertools
import json
import logging.config
import os
import re
from collections import OrderedDict
from collections import defaultdict
from string import Template

from ldap3.utils import dn as dnutils
from ldif3 import LDIFParser
from sqlalchemy.exc import OperationalError
from sqlalchemy.exc import IntegrityError
from sqlalchemy.exc import ProgrammingError
from sqlalchemy.sql import text

from jans.pycloudlib.persistence.sql import SQLClient
from jans.pycloudlib.utils import as_boolean

from settings import LOGGING_CONFIG
from utils import prepare_template_ctx
from utils import render_ldif
from utils import get_ldif_mappings

FIELD_RE = re.compile(r"[^0-9a-zA-Z\s]+")

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("entrypoint")


class SQLBackend:
    def __init__(self, manager):
        self.manager = manager

        self.db_dialect = os.environ.get("CN_SQL_DB_DIALECT", "mysql")
        self.db_name = os.environ.get("CN_SQL_DB_NAME", "jans")
        self.schema_files = [
            "/app/static/jans_schema.json",
            "/app/static/custom_schema.json",
        ]

        self.client = SQLClient()

        with open("/app/static/sql/sql_data_types.json") as f:
            self.sql_data_types = json.loads(f.read())

        self.attr_types = []
        for fn in self.schema_files:
            with open(fn) as f:
                schema = json.loads(f.read())
            self.attr_types += schema["attributeTypes"]

        with open("/app/static/sql/opendj_attributes_syntax.json") as f:
            self.opendj_attr_types = json.loads(f.read())

        with open("/app/static/sql/ldap_sql_data_type_mapping.json") as f:
            self.sql_data_types_mapping = json.loads(f.read())

        with open("/app/static/sql/sql_index.json") as f:
            self.sql_indexes = json.loads(f.read())

        with open("/app/static/couchbase/index.json") as f:
            # prefix = os.environ.get("CN_COUCHBASE_BUCKET_PREFIX", "jans")
            txt = f.read()  # .replace("!bucket_prefix!", prefix)
            self.cb_indexes = json.loads(txt)

        # cached schemas that holds table's column and its type
        self.table_columns = defaultdict(dict)

    def get_attr_syntax(self, attr):
        for attr_type in self.attr_types:
            if attr not in attr_type["names"]:
                continue
            if attr_type.get("multivalued"):
                return "JSON"
            return attr_type["syntax"]

        # fallback to OpenDJ attribute type
        return self.opendj_attr_types.get(attr) or "1.3.6.1.4.1.1466.115.121.1.15"

    def get_data_type(self, attr, table=None):
        # check from SQL data types first
        type_def = self.sql_data_types.get(attr)

        if type_def:
            type_ = type_def.get(self.db_dialect) or type_def["mysql"]

            if table in type_.get("tables", {}):
                type_ = type_["tables"][table]

            data_type = type_["type"]
            if "size" in type_:
                data_type = f"{data_type}({type_['size']})"
            return data_type

        # data type is undefined, hence check from syntax
        syntax = self.get_attr_syntax(attr)
        syntax_def = self.sql_data_types_mapping[syntax]
        type_ = syntax_def.get(self.db_dialect) or syntax_def["mysql"]

        if type_["type"] != "VARCHAR":
            return type_["type"]

        if type_["size"] <= 127:
            data_type = f"VARCHAR({type_['size']})"
        elif type_["size"] <= 255:
            data_type = "TINYTEXT" if self.db_dialect == "mysql" else "TEXT"
        else:
            data_type = "TEXT"
        return data_type

    def _exec_query(self, conn, query, **prepared_data):
        result = None
        try:
            result = conn.execute(text(query), **prepared_data)
        except (OperationalError, IntegrityError) as exc:
            if exc.orig.args[0] in (1050, 1060, 1061, 1062):
                # error with following code will be suppressed
                # - 1050: table exists
                # - 1060: column exists
                # - 1061: duplicate key name (index)
                # - 1062: duplicate entry
                pass
            else:
                logger.warning(f"Failed to execute query; reason={exc.orig.args}")
        return result

    def safe_quote(self, val):
        if self.db_dialect == "mysql":
            quote_char = '`'
        else:
            quote_char = '"'
        return f"{quote_char}{val}{quote_char}"

    def create_tables(self, conn):
        schemas = {}
        attrs = {}

        for fn in self.schema_files:
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

            table_cols = []

            # make sure ``oc["may"]`` doesn't have duplicate attribute
            for attr in set(oc["may"]):
                data_type = self.get_data_type(attr, table)
                col_def = f"{self.safe_quote(attr)} {data_type}"
                table_cols.append(col_def)
                self.table_columns[table].update({attr: data_type})

            doc_id_type = self.get_data_type("doc_id", table)
            mandatory_cols = [
                f"{self.safe_quote('doc_id')} {doc_id_type} NOT NULL UNIQUE",
                f"{self.safe_quote('objectClass')} VARCHAR(48)",
                f"{self.safe_quote('dn')} VARCHAR(128)",
            ]
            pk_cols = [f"PRIMARY KEY ({self.safe_quote('doc_id')})"]
            table_cols = mandatory_cols + table_cols + pk_cols
            table_cols_fmt = ", ".join(table_cols)
            sql_cmd = f"CREATE TABLE {self.safe_quote(table)} ({table_cols_fmt})"

            self.table_columns[table].update({
                "doc_id": doc_id_type,
                "objectClass": "VARCHAR(48)",
                "dn": "VARCHAR(128)",
            })

            self._exec_query(conn, sql_cmd)

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

    def _fields_from_cb_indexes(self):
        fields = []

        for _, data in self.cb_indexes.items():
            # extract and flatten
            attrs = list(itertools.chain.from_iterable(data["attributes"]))
            fields += attrs

            for static in data["static"]:
                attrs = [
                    attr for attr in static[0]
                    if "(" not in attr
                ]
                fields += attrs

        fields = list(set(fields))
        # exclude objectClass
        if "objectClass" in fields:
            fields.remove("objectClass")
        return fields

    def create_mysql_indexes(self, conn, table, column, column_type, json_common):
        indexes = []
        idx_name = FIELD_RE.sub("_", column)

        if column_type.lower() != "json":
            indexes.append(f"{table}_{idx_name} ({column})")
        else:
            for i, index_str in enumerate(json_common, start=1):
                index_str_fmt = Template(index_str).safe_substitute({
                    "field": column, "data_type": column_type,
                })
                indexes.append(f"{idx_name}_json_{i} (({index_str_fmt}))")

        for idx in indexes:
            query = f"ALTER TABLE {self.db_name}.{table} ADD INDEX {idx}"
            self._exec_query(conn, query)

    def create_pgsql_indexes(self, conn, table, column, column_type, json_common):
        indexes = []

        if column_type.lower() != "json":
            indexes.append(f"{column}")
        else:
            for index_str in json_common:
                index_str_fmt = Template(index_str).safe_substitute({
                    "field": column, "data_type": column_type,
                })
                indexes.append(f"({index_str_fmt})")

        for idx in indexes:
            query = f"CREATE INDEX ON {table} ({idx})"
            self._exec_query(conn, query)

    def create_indexes(self, conn):
        cb_fields = self._fields_from_cb_indexes()
        index_def = self.sql_indexes[self.db_dialect]

        for table, col_map in self.table_columns.items():
            fields = index_def.get(table, {}).get("fields", [])
            fields += index_def["__common__"]["fields"]
            fields += cb_fields

            # make unique fields
            fields = list(set(fields))

            for col, type_ in col_map.items():
                if col == "doc_id":
                    continue

                if col in fields:
                    if self.db_dialect == "mysql":
                        index_func = self.create_mysql_indexes
                    else:
                        index_func = self.create_pgsql_indexes
                    index_func(conn, table, col, type_, index_def["__common__"]["JSON"])

        #     index_cols = [
        #         f"`{table}_{i}`(({custom}))"
        #         for i, custom in enumerate(data["custom"])
        #     ]
        #     index_cols_fmt = ", ".join([f"ADD INDEX {col}" for col in index_cols])

        #     sql_cmd = f"ALTER TABLE {self.db_name}.{table} {index_cols_fmt};"
        #     self._exec_query(conn, sql_cmd)

    def import_ldif(self, conn):
        optional_scopes = json.loads(self.manager.config.get("optional_scopes", "[]"))
        ldif_mappings = get_ldif_mappings(optional_scopes)

        ctx = prepare_template_ctx(self.manager)

        for _, files in ldif_mappings.items():
            for file_ in files:
                logger.info(f"Importing {file_} file")
                src = f"/app/templates/{file_}"
                dst = f"/app/tmp/{file_}"
                os.makedirs(os.path.dirname(dst), exist_ok=True)

                render_ldif(src, dst, ctx)

                for sql_cmd, data in self.ldif_to_sql(dst):
                    self._exec_query(conn, sql_cmd, **data)

    def initialize(self):
        def is_initialized():
            initialized = False

            with self.client.engine.connect() as conn:
                try:
                    result = conn.execute(
                        text("SELECT COUNT(doc_id) FROM jansClnt WHERE doc_id = :doc_id"),
                        **{"doc_id": self.manager.config.get("jca_client_id")}
                    )
                    return as_boolean(result.fetchone()[0])
                except ProgrammingError as exc:
                    # the following code should be ignored
                    # - 1146: table doesn't exist
                    if exc.orig.args[0] in (1146,):
                        pass
            return initialized

        should_skip = as_boolean(
            os.environ.get("CN_PERSISTENCE_SKIP_INITIALIZED", False),
        )

        if should_skip and is_initialized():
            logger.info("SQL backend already initialized")
            return

        with self.client.engine.connect() as conn:
            logger.info("Creating tables (if not exist)")
            self.create_tables(conn)

            logger.info("Creating table indexes (if not exist)")
            self.create_indexes(conn)

            self.import_ldif(conn)

    def transform_value(self, key, values):
        type_ = self.sql_data_types.get(key)

        if not type_:
            attr_syntax = self.get_attr_syntax(key)
            type_ = self.sql_data_types_mapping[attr_syntax]

        data_type = type_[self.db_dialect]["type"]

        if data_type in ("SMALLINT",):
            if values[0].lower() in ("1", "on", "true", "yes", "ok"):
                return 1
            return 0

        if data_type == "INT":
            return int(values[0])

        if data_type in ("DATETIME(3)",):
            dval = values[0].strip("Z")
            return "{}-{}-{} {}:{}:{}{}".format(dval[0:4], dval[4:6], dval[6:8], dval[8:10], dval[10:12], dval[12:14], dval[14:17])

        if data_type == "JSON":
            return json.dumps({"v": values})

        # fallback
        return values[0]

    def ldif_to_sql(self, filename):
        with open(filename, "rb") as fd:
            parser = LDIFParser(fd)

            for dn, entry in parser.parse():
                parsed_dn = dnutils.parse_dn(dn)
                # rdn_name = parsed_dn[0][0]
                doc_id = parsed_dn[0][1]

                oc = entry.get("objectClass") or entry.get("objectclass")
                if oc:
                    if "top" in oc:
                        oc.remove("top")

                    if len(oc) == 1 and oc[0].lower() in ("organizationalunit", "organization"):
                        continue

                table = oc[-1]

                # entry.pop(rdn_name)

                if "objectClass" in entry:
                    entry.pop("objectClass")
                elif "objectclass" in entry:
                    entry.pop("objectclass")

                attr_mapping = OrderedDict({
                    "doc_id": doc_id,
                    "objectClass": table,
                    "dn": dn,
                })

                for attr in entry:
                    value = self.transform_value(attr, entry[attr])
                    attr_mapping[attr] = value

                # populate existing JSON columns with default if value
                # is not defined from ldif
                for col, type_ in self.table_columns.get(table, {}).items():
                    if not all([col not in attr_mapping, type_.lower() == "json"]):
                        continue
                    attr_mapping[col] = json.dumps({"v": []})

                columns = ", ".join(attr_mapping.keys())
                params = ", ".join(f":{column}" for column in attr_mapping.keys())
                sql_cmd = f"INSERT INTO {table} ({columns}) VALUES ({params});"
                yield sql_cmd, attr_mapping

    def table_exists(self, conn, table):
        result = conn.execute(
            text("SELECT COUNT(*) FROM information_schema.tables WHERE TABLE_NAME = :table"),
            **{"table": table}
        )
        return as_boolean(result.fetchone()[0])
