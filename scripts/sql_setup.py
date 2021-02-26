import contextlib
import json
import logging.config
import os
import re
from collections import OrderedDict
from collections import defaultdict

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

        # cached schemas that holds JSON columns
        self.json_columns = defaultdict(list)

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
        logger.info("Creating tables (if not exist)")

        schemas = {}

        for fn in self.schema_files:
            with open(fn) as f:
                schema = json.loads(f.read())

            for oc in schema["objectClasses"]:
                schemas[oc["names"][0]] = oc

        for table, oc in schemas.items():
            if oc.get("sql", {}).get("ignore"):
                continue

            table_cols = []

            if "sql" in oc:
                oc["may"] += oc["sql"].get("include", [])

                for inc_oc in oc["sql"].get("includeObjectClass", []):
                    oc["may"] += schemas[inc_oc]["may"]

            for attr in set(oc["may"]):
                data_type = self.get_data_type(attr)
                col_def = f"{attr} {data_type}"
                table_cols.append(col_def)

                if data_type.lower() == "json":
                    self.json_columns[table].append(attr)

            sql_cmd = (
                f"CREATE TABLE {table} ("
                "id INT NOT NULL auto_increment, "
                "doc_id VARCHAR(48) NOT NULL UNIQUE, "
                "objectClass VARCHAR(48), "
                "dn VARCHAR(128), "
                f"{', '.join(table_cols)}, "
                "PRIMARY KEY (id, doc_id)"
                ");"
            )

            try:
                conn.execute(sql_cmd)
                # self.tables.append(table)
            except OperationalError as exc:
                # the following code should be ignored
                # - 1050: table exists
                if exc.orig.args[0] in (1050,):
                    continue
                logger.warning(f"Failed to execute query; reason={exc.orig.args}")

    def create_indexes(self, conn):
        logger.info("Creating table indexes (if not exist)")

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

    def import_ldif(self, conn):
        ldif_mappings = get_ldif_mappings()

        ctx = prepare_template_ctx(self.manager)

        for _, files in ldif_mappings.items():
            # self.check_indexes(mapping)

            for file_ in files:
                logger.info(f"Importing {file_} file")
                src = f"/app/templates/{file_}"
                dst = f"/app/tmp/{file_}"
                os.makedirs(os.path.dirname(dst), exist_ok=True)

                render_ldif(src, dst, ctx)

                # query_file = f"{dst}.sql"
                # with open(query_file, "a+"):
                for sql_cmd, data in self.ldif_to_sql(dst):
                    try:
                        conn.execute(text(sql_cmd), **data)
                    except IntegrityError as exc:
                        # the following code should be ignored
                        # - 1061: duplicate key name
                        if exc.orig.args[0] in (1062,):
                            continue
                        logger.warning(f"Failed to execute query; reason={exc.orig.args}")

    def initialize(self):
        def is_initialized():
            initialized = False

            with self.client.engine.connect() as conn:
                try:
                    result = conn.execute(
                        text("SELECT COUNT(id) FROM jansClnt WHERE doc_id = :doc_id"),
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
            os.environ.get("CN_PERSISTENCE_SKIP_EXISTING", True),
        )

        if should_skip and is_initialized():
            logger.info("SQL backend already initialized")
            return

        with self.client.engine.connect() as conn:
            self.create_tables(conn)
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
        with contextlib.ExitStack() as stack:
            # will be automatically closed when exiting ``with`` block
            fd = stack.enter_context(open(filename, "rb"))

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
                for json_col in self.json_columns.get(table, []):
                    if json_col not in attr_mapping:
                        attr_mapping[json_col] = json.dumps({"v": []})

                columns = ", ".join(attr_mapping.keys())
                params = ", ".join(f":{column}" for column in attr_mapping.keys())
                sql_cmd = f"INSERT INTO {table} ({columns}) VALUES ({params});"
                yield sql_cmd, attr_mapping
