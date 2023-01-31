import json
import logging.config
import re
from collections import defaultdict
from string import Template
from pathlib import Path

from sqlalchemy import text
from sqlalchemy.exc import OperationalError

from jans.pycloudlib.persistence.sql import SqlClient

from settings import LOGGING_CONFIG
from utils import prepare_template_ctx
from utils import get_ldif_mappings

FIELD_RE = re.compile(r"[^0-9a-zA-Z\s]+")

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("sql_setup")


class SQLBackend:
    def __init__(self, manager):
        self.manager = manager
        self.client = SqlClient(manager)

        if self.client.dialect == "mysql":
            index_fn = "mysql_index.json"
        else:
            index_fn = "pgsql_index.json"

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
        type_def = self.client.sql_data_types.get(attr)

        if type_def:
            type_ = type_def.get(self.client.dialect) or type_def["mysql"]

            if table in type_.get("tables", {}):
                type_ = type_["tables"][table]

            data_type = type_["type"]
            if "size" in type_:
                data_type = f"{data_type}({type_['size']})"
            return data_type

        # data type is undefined, hence check from syntax
        syntax = self.client.get_attr_syntax(attr)
        syntax_def = self.client.sql_data_types_mapping[syntax]
        type_ = syntax_def.get(self.client.dialect) or syntax_def["mysql"]

        char_type = "VARCHAR"

        if type_["type"] != char_type:
            data_type = type_["type"]
        else:
            if type_["size"] <= 127:
                data_type = f"{char_type}({type_['size']})"
            elif type_["size"] <= 255:
                data_type = "TINYTEXT" if self.client.dialect == "mysql" else "TEXT"
            else:
                data_type = "TEXT"

        return data_type

    def create_tables(self):
        schemas = {}
        attrs = {}
        # cached schemas that holds table's column and its type
        table_columns = defaultdict(dict)

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
            table_columns[table].update({
                "doc_id": doc_id_type,
                "objectClass": "VARCHAR(48)",
                "dn": "VARCHAR(128)",
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

    def get_index_fields(self, table_name):
        fields = self.sql_indexes.get(table_name, {}).get("fields", [])
        fields += self.sql_indexes["__common__"]["fields"]

        # make unique fields
        return list(set(fields))

    def create_mysql_indexes(self, table_name: str, column_mapping: dict):
        fields = self.get_index_fields(table_name)

        for column_name, column_type in column_mapping.items():
            if column_name == "doc_id" or column_name not in fields:
                continue

            if column_type.lower() != "json":
                index_name = f"{table_name}_{FIELD_RE.sub('_', column_name)}"
                query = f"CREATE INDEX {self.client.quoted_id(index_name)} ON {self.client.quoted_id(table_name)} ({self.client.quoted_id(column_name)})"
                self.client.create_index(query)
            else:
                if self.client.server_version < "8.0":
                    # prior to MySQL 8.0, CASTing on index creation will raise SQL syntax error;
                    # switch to virtual column instead
                    for i in range(4):
                        index_str_fmt = f"{column_name}_mem_idx_{i}"
                        query = " ".join([
                            f"ALTER TABLE {self.client.quoted_id(table_name)}",
                            f"ADD COLUMN {self.client.quoted_id(index_str_fmt)} CHAR(128) AS ({column_name}->'$.v[{i}]')",
                            f", ADD INDEX ({self.client.quoted_id(index_str_fmt)})"
                        ])
                        try:
                            self.client.create_index(query)
                        except OperationalError as exc:
                            # re-raise exception if the code isn't one of the following code
                            # 1060 - duplicate column error
                            if exc.orig.args[0] not in [1060]:
                                raise exc
                else:
                    for i, index_str in enumerate(self.sql_indexes["__common__"]["JSON"], start=1):
                        index_str_fmt = Template(index_str).safe_substitute({
                            "field": column_name,  # "data_type": column_type,
                        })
                        name = f"{table_name}_json_{i}"
                        query = f"ALTER TABLE {self.client.quoted_id(table_name)} ADD INDEX {self.client.quoted_id(name)} (({index_str_fmt}))"
                        self.client.create_index(query)

        for i, custom in enumerate(self.sql_indexes.get(table_name, {}).get("custom", []), start=1):
            # jansPerson table has unsupported custom index expressions that need to be skipped if mysql < 8.0
            # if table_name == "jansPerson" and self.client.server_version < "8.0":
            if table_name == "jansPerson" and self.client.get_server_version() < (8, 0):
                continue
            name = f"{table_name}_CustomIdx{i}"
            query = f"CREATE INDEX {self.client.quoted_id(name)} ON {self.client.quoted_id(table_name)} ({custom})"
            self.client.create_index(query)

    def create_pgsql_indexes(self, table_name: str, column_mapping: dict):
        fields = self.get_index_fields(table_name)

        for column_name, column_type in column_mapping.items():
            if column_name == "doc_id" or column_name not in fields:
                continue

            index_name = f"{table_name}_{FIELD_RE.sub('_', column_name)}"

            if column_type.lower() != "jsonb":
                query = f"CREATE INDEX {self.client.quoted_id(index_name)} ON {self.client.quoted_id(table_name)} ({self.client.quoted_id(column_name)})"
                self.client.create_index(query)
            else:
                for i, index_str in enumerate(self.sql_indexes["__common__"]["JSON"], start=1):
                    index_str_fmt = Template(index_str).safe_substitute({
                        "field": column_name, "data_type": column_type,
                    })
                    name = f"{table_name}_json_{i}"
                    query = f"CREATE INDEX {self.client.quoted_id(name)} ON {self.client.quoted_id(table_name)} {index_str_fmt}"
                    self.client.create_index(query)

        for i, custom in enumerate(self.sql_indexes.get(table_name, {}).get("custom", []), start=1):
            name = f"{table_name}_custom_{i}"
            query = f"CREATE INDEX {self.client.quoted_id(name)} ON {self.client.quoted_id(table_name)} {custom}"
            self.client.create_index(query)

    def create_indexes(self):
        for table_name, column_mapping in self.client.get_table_mapping().items():
            if self.client.dialect == "pgsql":
                index_func = self.create_pgsql_indexes
            else:
                index_func = self.create_mysql_indexes
            # run the callback
            index_func(table_name, column_mapping)

    def import_builtin_ldif(self, ctx):
        optional_scopes = json.loads(self.manager.config.get("optional_scopes", "[]"))
        ldif_mappings = get_ldif_mappings("sql", optional_scopes)

        for _, files in ldif_mappings.items():
            for file_ in files:
                self._import_ldif(f"/app/templates/{file_}", ctx)

    def initialize(self):
        logger.info("Creating tables (if not exist)")
        self.create_tables()

        logger.info("Updating schema (if required)")
        self.update_schema()

        # force-reload metadata as we may have changed the schema
        self.client._metadata = None

        logger.info("Creating indexes (if not exist)")
        self.create_indexes()

        ctx = prepare_template_ctx(self.manager)

        logger.info("Importing builtin LDIF files")
        self.import_builtin_ldif(ctx)

        logger.info("Importing custom LDIF files (if any)")
        self.import_custom_ldif(ctx)

    def update_schema(self):
        """Updates schema (may include data migration)"""

        table_mapping = self.client.get_table_mapping()

        def column_to_json(table_name, col_name):
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
            with self.client.engine.connect() as conn:
                conn.execute(f"ALTER TABLE {self.client.quoted_id(table_name)} DROP COLUMN {self.client.quoted_id(col_name)}")
                conn.execute(f"ALTER TABLE {self.client.quoted_id(table_name)} ADD COLUMN {self.client.quoted_id(col_name)} {data_type}")

            # force-reload metadata as we may have changed the schema before migrating old data
            self.client._metadata = None

            # pre-populate the modified column
            for doc_id, value in values.items():
                if not value:
                    value_list = []
                else:
                    value_list = [value]
                self.client.update(table_name, doc_id, {col_name: {"v": value_list}})

        def add_column(table_name, col_name):
            if col_name in table_mapping[table_name]:
                return

            data_type = self.get_data_type(col_name, table_name)
            with self.client.engine.connect() as conn:
                conn.execute(f"ALTER TABLE {self.client.quoted_id(table_name)} ADD COLUMN {self.client.quoted_id(col_name)} {data_type}")

        def change_column_type(table_name, col_name):
            old_data_type = table_mapping[table_name][col_name]
            data_type = self.get_data_type(col_name, table_name)

            if data_type == old_data_type:
                return

            if self.client.dialect == "mysql":
                query = f"ALTER TABLE {self.client.quoted_id(table_name)} " \
                        f"MODIFY COLUMN {self.client.quoted_id(col_name)} {data_type}"
            else:
                query = f"ALTER TABLE {self.client.quoted_id(table_name)} " \
                        f"ALTER COLUMN {self.client.quoted_id(col_name)} TYPE {data_type}"
            with self.client.engine.connect() as conn:
                conn.execute(query)

        def column_from_json(table_name, col_name):
            old_data_type = table_mapping[table_name][col_name]
            data_type = self.get_data_type(col_name, table_name)

            if data_type == old_data_type:
                return

            # get the value first before updating column type
            values = {
                row["doc_id"]: row[col_name]
                for row in self.client.search(table_name, ["doc_id", col_name])
            }

            with self.client.engine.connect() as conn:
                # mysql will raise error if dropping column which has functional index,
                # hence the associated index must be dropped first
                if self.client.dialect == "mysql":
                    for idx in conn.execute(
                        text(
                            "SELECT index_name "
                            "FROM information_schema.statistics "
                            "WHERE table_name = :table_name "
                            "AND index_name LIKE :index_name '%' "
                            "AND expression LIKE '%' :col_name '%';"
                        ),
                        {
                            "table_name": table_name,
                            "index_name": f"{table_name}_json_",
                            "col_name": col_name
                        },
                    ):
                        conn.execute(f"ALTER TABLE {table_name} DROP INDEX {idx[0]}")

                # to change the storage format of a JSON column, drop the column and
                # add the column back specifying the new storage format
                conn.execute(f"ALTER TABLE {self.client.quoted_id(table_name)} DROP COLUMN {self.client.quoted_id(col_name)}")
                conn.execute(f"ALTER TABLE {self.client.quoted_id(table_name)} ADD COLUMN {self.client.quoted_id(col_name)} {data_type}")

            # force-reload metadata as we may have changed the schema before migrating old data
            self.client._metadata = None

            # pre-populate the modified column
            for doc_id, value in values.items():
                if self.client.dialect == "mysql" and value and value.get("v", []):
                    new_value = value["v"][0]
                elif self.client.dialect == "pgsql" and value:
                    new_value = value[0]
                else:
                    new_value = ""
                self.client.update(table_name, doc_id, {col_name: new_value})

        # TODO: run the function with connection context?

        # the following columns are changed to multivalued (JSON type)
        for mod in [
            ("jansClnt", "jansDefAcrValues"),
            ("jansClnt", "jansLogoutURI"),
            ("jansPerson", "role"),
            ("jansPerson", "mobile"),
            ("jansCustomScr", "jansAlias"),
            ("jansClnt", "jansReqURI"),
            ("jansClnt", "jansClaimRedirectURI"),
            ("jansClnt", "jansAuthorizedOrigins"),
        ]:
            column_to_json(mod[0], mod[1])

        # the following columns must be added to respective tables
        for mod in [
            ("jansToken", "jansUsrDN"),
            ("jansPerson", "jansTrustedDevices"),
            ("jansUmaRPT", "dpop"),
            ("jansUmaPCT", "dpop"),
            ("jansClnt", "o"),
            ("jansClnt", "jansGrp"),
            ("jansScope", "creatorId"),
            ("jansScope", "creatorTyp"),
            ("jansScope", "creatorAttrs"),
            ("jansScope", "creationDate"),
            ("jansStatEntry", "jansData"),
            ("jansSessId", "deviceSecret"),
            ("jansSsa", "jansState"),
            ("jansClnt", "jansClntURILocalized"),
            ("jansClnt", "jansLogoURILocalized"),
            ("jansClnt", "jansPolicyURILocalized"),
            ("jansClnt", "jansTosURILocalized"),
            ("jansClnt", "displayNameLocalized"),
            ("jansFido2AuthnEntry", "jansApp"),
            ("jansFido2AuthnEntry", "jansCodeChallengeHash"),
            ("jansFido2AuthnEntry", "exp"),
            ("jansFido2AuthnEntry", "del"),
            ("jansFido2RegistrationEntry", "jansApp"),
            ("jansFido2RegistrationEntry", "jansPublicKeyIdHash"),
            ("jansFido2RegistrationEntry", "jansDeviceData"),
            ("jansFido2RegistrationEntry", "exp"),
            ("jansFido2RegistrationEntry", "del"),
        ]:
            add_column(mod[0], mod[1])

        # change column type (except from/to multivalued)
        for mod in [
            ("jansPerson", "givenName"),
            ("jansPerson", "sn"),
            ("jansPerson", "userPassword"),
            ("jansAppConf", "userPassword"),
            ("jansPerson", "jansStatus"),
            ("jansPerson", "cn"),
            ("jansPerson", "secretAnswer"),
            ("jansPerson", "secretQuestion"),
            ("jansPerson", "street"),
            ("jansPerson", "address"),
            ("jansPerson", "picture"),
            ("jansPerson", "mail"),
            ("jansPerson", "gender"),
            ("jansPerson", "jansNameFormatted"),
            ("jansPerson", "jansExtId"),
            ("jansGrp", "jansStatus"),
            ("jansOrganization", "jansStatus"),
            ("jansOrganization", "street"),
            ("jansOrganization", "postalCode"),
            ("jansOrganization", "mail"),
            ("jansAppConf", "jansStatus"),
            ("jansAttr", "jansStatus"),
            ("jansUmaResourcePermission", "jansStatus"),
            ("jansUmaResourcePermission", "jansUmaScope"),
            ("jansDeviceRegistration", "jansStatus"),
            ("jansFido2AuthnEntry", "jansStatus"),
            ("jansFido2RegistrationEntry", "jansStatus"),
            ("jansCibaReq", "jansStatus"),
            ("jansInumMap", "jansStatus"),
            ("jansDeviceRegistration", "jansDeviceKeyHandle"),
            ("jansUmaResource", "jansUmaScope"),
            ("jansU2fReq", "jansReq"),
            ("jansFido2AuthnEntry", "jansAuthData"),
            ("jansFido2RegistrationEntry", "jansCodeChallengeHash"),
            ("agmFlowRun", "agFlowEncCont"),
            ("agmFlowRun", "agFlowSt"),
            ("agmFlowRun", "jansCustomMessage"),
            ("agmFlow", "agFlowMeta"),
            ("agmFlow", "agFlowTrans"),
            ("agmFlow", "jansCustomMessage"),
            ("jansOrganization", "jansCustomMessage"),
        ]:
            change_column_type(mod[0], mod[1])

        # columns are changed from multivalued
        for mod in [
            ("jansPerson", "jansMobileDevices"),
            ("jansPerson", "jansOTPDevices"),
            ("jansToken", "clnId"),
            ("jansUmaRPT", "clnId"),
            ("jansUmaPCT", "clnId"),
            ("jansCibaReq", "clnId"),
        ]:
            column_from_json(mod[0], mod[1])

    def import_custom_ldif(self, ctx):
        custom_dir = Path("/app/custom_ldif")

        for file_ in custom_dir.rglob("*.ldif"):
            self._import_ldif(file_, ctx)

    def _import_ldif(self, path, ctx):
        logger.info(f"Importing {path} file")
        self.client.create_from_ldif(path, ctx)
