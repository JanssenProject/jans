# import itertools
import hashlib
import json
import logging.config
import os
import re
from collections import OrderedDict
from collections import defaultdict

from ldap3.utils import dn as dnutils
from ldif import LDIFParser

from jans.pycloudlib.persistence.spanner import SpannerClient
from jans.pycloudlib.utils import as_boolean

from settings import LOGGING_CONFIG
from utils import prepare_template_ctx
from utils import render_ldif
from utils import get_ldif_mappings

FIELD_RE = re.compile(r"[^0-9a-zA-Z\s]+")

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("entrypoint")


class SpannerBackend:
    def __init__(self, manager):
        self.manager = manager
        self.db_dialect = "spanner"

        self.schema_files = [
            "/app/static/jans_schema.json",
            "/app/static/custom_schema.json",
        ]

        self.client = SpannerClient()

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

        index_fn = "spanner_index.json"
        with open(f"/app/static/sql/{index_fn}") as f:
            self.sql_indexes = json.loads(f.read())

        # with open("/app/static/couchbase/index.json") as f:
        #     # prefix = os.environ.get("CN_COUCHBASE_BUCKET_PREFIX", "jans")
        #     txt = f.read()  # .replace("!bucket_prefix!", prefix)
        #     self.cb_indexes = json.loads(txt)

        with open("/app/static/sql/sub_tables.json") as f:
            self.sub_tables = json.loads(f.read()).get(self.db_dialect) or {}

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
            type_ = type_def.get(self.db_dialect)  # or type_def["mysql"]

            if table in type_.get("tables", {}):
                type_ = type_["tables"][table]

            data_type = type_["type"]
            if "size" in type_:
                data_type = f"{data_type}({type_['size']})"
            return data_type

        # data type is undefined, hence check from syntax
        syntax = self.get_attr_syntax(attr)
        syntax_def = self.sql_data_types_mapping[syntax]
        type_ = syntax_def.get(self.db_dialect)  # or syntax_def["mysql"]

        # char_type = "VARCHAR"
        # if self.db_dialect == "spanner":
        char_type = "STRING"

        if type_["type"] != char_type:
            # not STRING
            data_type = type_["type"]
        else:
            # if "size" in type_:
            #     size = type_["size"]
            #     # data_type = f"{char_type}(type['size'])"
            # else:
            #     # data_type = "STRING(MAX)"
            #     size = "MAX"
            size = type_.get("size") or "MAX"
            data_type = f"{char_type}({size})"
            # if type_["size"] <= 127:
            #     data_type = f"{char_type}({type_['size']})"
            # elif type_["size"] <= 255:
            #     data_type = "TINYTEXT" if self.db_dialect == "mysql" else "TEXT"
            # else:
            #     data_type = "TEXT"

        # if data_type == "TEXT" and self.db_dialect == "spanner":
        #     data_type = "STRING(MAX)"
        return data_type

    def create_tables(self):
        schemas = {}
        attrs = {}
        # cached schemas that holds table's column and its type
        table_columns = defaultdict(dict)

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

            doc_id_type = self.get_data_type("doc_id", table)
            table_columns[table].update({
                "doc_id": doc_id_type,
                "objectClass": "STRING(48)",
                "dn": "STRING(128)",
            })

            # make sure ``oc["may"]`` doesn't have duplicate attribute
            for attr in set(oc["may"]):
                data_type = self.get_data_type(attr, table)
                table_columns[table].update({attr: data_type})

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

    # def _fields_from_cb_indexes(self):
    #     fields = []

    #     for _, data in self.cb_indexes.items():
    #         # extract and flatten
    #         attrs = list(itertools.chain.from_iterable(data["attributes"]))
    #         fields += attrs

    #         for static in data["static"]:
    #             attrs = [
    #                 attr for attr in static[0]
    #                 if "(" not in attr
    #             ]
    #             fields += attrs

    #     fields = list(set(fields))
    #     # exclude objectClass
    #     if "objectClass" in fields:
    #         fields.remove("objectClass")
    #     return fields

    def get_index_fields(self, table_name):
        # cb_fields = self._fields_from_cb_indexes()
        fields = self.sql_indexes.get(table_name, {}).get("fields", [])
        fields += self.sql_indexes["__common__"]["fields"]
        # fields += cb_fields

        # make unique fields
        return list(set(fields))

    def create_spanner_indexes(self, table_name: str, column_mapping: dict):
        fields = self.get_index_fields(table_name)

        for column_name, column_type in column_mapping.items():
            if column_name == "doc_id" or column_name not in fields:
                continue

            index_name = f"{table_name}_{FIELD_RE.sub('_', column_name)}"

            if column_type.lower() != "array":
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

    def import_ldif(self):
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

                for table_name, column_mapping in self.data_from_ldif(dst):
                    self.client.insert_into(table_name, column_mapping)
                    # inject rows into subtable (if any)
                    self.insert_into_subtable(table_name, column_mapping)

    def initialize(self):
        def is_initialized():
            return self.client.row_exists("jansClnt", self.manager.config.get("jca_client_id"))

        should_skip = as_boolean(os.environ.get("CN_PERSISTENCE_SKIP_INITIALIZED", False))
        if should_skip and is_initialized():
            logger.info("Spanner backend already initialized")
            return

        logger.info("Creating tables (if not exist)")
        self.create_tables()
        self.create_subtables()

        logger.info("Creating indexes (if not exist)")
        self.create_indexes()

        self.import_ldif()

    def transform_value(self, key, values):
        type_ = self.sql_data_types.get(key)

        if not type_:
            attr_syntax = self.get_attr_syntax(key)
            type_ = self.sql_data_types_mapping[attr_syntax]

        type_ = type_.get(self.db_dialect)  # or type_["mysql"]
        data_type = type_["type"]

        if data_type in ("SMALLINT", "BOOL",):
            if values[0].lower() in ("1", "on", "true", "yes", "ok"):
                return 1 if data_type == "SMALLINT" else True
            return 0 if data_type == "SMALLINT" else False

        if data_type == "INT":
            return int(values[0])

        if data_type in ("DATETIME(3)", "TIMESTAMP",):
            dval = values[0].strip("Z")
            # sep = " "
            # postfix = ""
            # if self.db_dialect == "spanner":
            sep = "T"
            postfix = "Z"
            # return "{}-{}-{} {}:{}:{}{}".format(dval[0:4], dval[4:6], dval[6:8], dval[8:10], dval[10:12], dval[12:14], dval[14:17])
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
            # return json.dumps({"v": values})
            return {"v": values}

        if data_type == "ARRAY<STRING(MAX)>":
            return values

        # fallback
        return values[0]

    def data_from_ldif(self, filename):
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

                table_name = oc[-1]

                # entry.pop(rdn_name)

                if "objectClass" in entry:
                    entry.pop("objectClass")
                elif "objectclass" in entry:
                    entry.pop("objectclass")

                attr_mapping = OrderedDict({
                    "doc_id": doc_id,
                    "objectClass": table_name,
                    "dn": dn,
                })

                for attr in entry:
                    # TODO: check if attr in sub table
                    value = self.transform_value(attr, entry[attr])
                    attr_mapping[attr] = value
                yield table_name, attr_mapping

    def create_subtables(self):
        for table_name, columns in self.sub_tables.items():
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

    def column_in_subtable(self, table_name, column):
        exists = False

        # column_mapping is a list
        column_mapping = self.sub_tables.get(table_name, [])
        for cm in column_mapping:
            if column == cm[0]:
                exists = True
                break
        return exists

    def insert_into_subtable(self, table_name, column_mapping):
        for column, value in column_mapping.items():
            if not self.column_in_subtable(table_name, column):
                continue

            for item in value:
                hashed = hashlib.sha256()
                hashed.update(item.encode())
                dict_doc_id = hashed.digest().hex()

                self.client.insert_into(
                    f"{table_name}_{column}",
                    {
                        "doc_id": column_mapping["doc_id"],
                        "dict_doc_id": dict_doc_id,
                        column: item
                    },
                )
