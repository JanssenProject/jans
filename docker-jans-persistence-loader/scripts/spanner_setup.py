import json
import logging.config
import re
from collections import defaultdict
from pathlib import Path

from jans.pycloudlib.persistence.spanner import SpannerClient

from settings import LOGGING_CONFIG
from utils import prepare_template_ctx
from utils import get_ldif_mappings

FIELD_RE = re.compile(r"[^0-9a-zA-Z\s]+")

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("spanner_setup")


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
        type_def = self.client.sql_data_types.get(attr)

        if type_def:
            type_ = type_def.get(self.client.dialect)

            if table in type_.get("tables", {}):
                type_ = type_["tables"][table]

            data_type = type_["type"]
            if "size" in type_:
                data_type = f"{data_type}({type_['size']})"
            return data_type

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

    def import_builtin_ldif(self, ctx):
        optional_scopes = json.loads(self.manager.config.get("optional_scopes", "[]"))
        ldif_mappings = get_ldif_mappings("spanner", optional_scopes)

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

        # the following columns are changed to multivalued (ARRAY type)
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
            column_to_array(mod[0], mod[1])

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
            column_from_array(mod[0], mod[1])

        # int64 to string
        for mod in [
            ("jansFido2RegistrationEntry", "jansCodeChallengeHash"),
        ]:
            column_int_to_string(mod[0], mod[1])

    def import_custom_ldif(self, ctx):
        custom_dir = Path("/app/custom_ldif")

        for file_ in custom_dir.rglob("*.ldif"):
            self._import_ldif(file_, ctx)

    def _import_ldif(self, path, ctx):
        logger.info(f"Importing {path} file")
        self.client.create_from_ldif(path, ctx)
