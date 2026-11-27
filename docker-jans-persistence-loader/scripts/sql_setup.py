import json
import logging.config
import os
import re
from collections import defaultdict
from string import Template
from pathlib import Path

from sqlalchemy import text
from sqlalchemy.exc import DBAPIError, OperationalError

from jans.pycloudlib.persistence.sql import IndexLockTimeoutError, SqlClient, resolve_db_schema_name
from jans.pycloudlib.utils import as_boolean

from settings import LOGGING_CONFIG
from utils import prepare_template_ctx
from hooks import get_ldif_mappings_hook

FIELD_RE = re.compile(r"[^0-9a-zA-Z\s]+")

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("persistence-loader")

# seconds the lookup of a unique index may wait for a lock; if it gives up, the index is created with a plain
# unique-index statement, which waits for the table lock
UNIQUE_INDEX_LOOKUP_LOCK_TIMEOUT_SECONDS = 10

# number of leading positions of the JSON ``v`` array that get a virtual column and an index on MySQL < 8.0
MYSQL_VIRTUAL_COLUMN_POSITIONS = 4

# MySQL error 1060 (ER_DUP_FIELDNAME): duplicate column name
MYSQL_DUPLICATE_COLUMN_ERROR = 1060


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
        with open("/app/static/rdbm/opendj_index.json") as f:
            opendj_indexes = [attr["attribute"] for attr in json.loads(f.read())]

        for attr in self.client.attr_types:
            if not attr.get("multivalued"):
                continue
            for attr_name in attr["names"]:
                if attr_name in opendj_indexes and attr_name not in self.sql_indexes["__common__"]["fields"]:
                    self.sql_indexes["__common__"]["fields"].append(attr_name)

    def get_data_type(self, attr, table=None):
        # check from SQL data types first
        if type_def := self.client.sql_data_types.get(f"{table}:{attr}") or self.client.sql_data_types.get(attr):
            type_ = type_def.get(self.client.dialect) or type_def["mysql"]

            if table in type_.get("tables", {}):
                type_ = type_["tables"][table]

            data_type = type_["type"]
            if "size" in type_:
                data_type = f"{data_type}({type_['size']})"
            return data_type  # noqa: R504

        # probably JSON-like data type
        if attr in self.client.sql_json_types:
            return self.client.sql_json_types[attr][self.client.dialect]["type"]

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
        return data_type  # noqa: R504

    def create_tables(self):
        table_columns = self.table_mapping_from_schema()

        for table, attr_mapping in table_columns.items():
            self.client.create_table(table, attr_mapping, "doc_id")

    def get_index_fields(self, table_name):
        fields = self.sql_indexes.get(table_name, {}).get("fields", [])
        fields += self.sql_indexes["__common__"]["fields"]

        # make unique fields while preserving order
        return list(dict.fromkeys(fields))

    def create_mysql_indexes(self, table_name: str, column_mapping: dict):
        fields = self.get_index_fields(table_name)

        for column_name, column_type in column_mapping.items():
            if column_name == "doc_id" or column_name not in fields:
                continue

            index_name = f"{table_name}_{FIELD_RE.sub('_', column_name)}"

            if column_type == "TEXT":
                # set key length to 255
                query = f"CREATE INDEX {self.client.quoted_id(index_name)} ON {self.client.quoted_id(table_name)} ({self.client.quoted_id(column_name)} (255))"
                self.client.create_index(query, table_name, index_name)
            elif column_type.lower() != "json":
                query = f"CREATE INDEX {self.client.quoted_id(index_name)} ON {self.client.quoted_id(table_name)} ({self.client.quoted_id(column_name)})"
                self.client.create_index(query, table_name, index_name)
            else:
                if self.client.get_server_version() < (8, 0):
                    # prior to MySQL 8.0, CASTing on index creation will raise SQL syntax error;
                    # switch to virtual column instead
                    for position in range(MYSQL_VIRTUAL_COLUMN_POSITIONS):
                        self.create_mysql_virtual_column_index(table_name, column_name, position, column_mapping)
                else:
                    for i, index_str in enumerate(self.sql_indexes["__common__"]["JSON"], start=1):
                        index_str_fmt = Template(index_str).safe_substitute({
                            "field": column_name,  # "data_type": column_type,
                        })
                        name = f"{table_name}_json_{i}"
                        query = f"ALTER TABLE {self.client.quoted_id(table_name)} ADD INDEX {self.client.quoted_id(name)} (({index_str_fmt}))"
                        self.client.create_index(query, table_name, name)

        for i, custom in enumerate(self.sql_indexes.get(table_name, {}).get("custom", []), start=1):
            # jansPerson table has unsupported custom index expressions that need to be skipped if mysql < 8.0
            if table_name == "jansPerson" and self.client.get_server_version() < (8, 0):
                continue
            name = f"{table_name}_CustomIdx{i}"
            query = f"CREATE INDEX {self.client.quoted_id(name)} ON {self.client.quoted_id(table_name)} ({custom})"
            self.client.create_index(query, table_name, name)

    def create_mysql_virtual_column_index(self, table_name: str, column_name: str, position: int, column_mapping: dict):
        """Index one element of a JSON column through a virtual column, because MySQL < 8.0 cannot index a CAST.

        The column and its index are added by two statements: combined in one ``ALTER TABLE`` they need
        ``LOCK=SHARED``, which blocks writers of the table while the index is built; separately each runs with
        ``LOCK=NONE``. Each statement still needs the table's metadata lock for a moment, and while it waits for it,
        later statements on the table, writers included, wait behind it. ``alter_table`` and ``create_index`` of the
        library limit that wait to their ``lock_timeout`` per attempt and make at most ``retries`` attempts (see the defaults
        of those arguments).

        Args:
            table_name: Name of the table.
            column_name: Name of the JSON column.
            position: Position of the element in the column's ``v`` array.
            column_mapping: Columns of the table as read by ``create_indexes``; a virtual column listed here
                is not added again.

        Raises:
            OperationalError: The column could not be added for a MySQL reason other than lock contention or
                a duplicate column (see ``add_mysql_virtual_column``).
            DatabaseError: Any other error of the column or the index statement, other than lock contention or
                "already exists".
        """
        virtual_column = f"{column_name}_mem_idx_{position}"
        expression = f"{column_name}->'$.v[{position}]'"

        if virtual_column not in column_mapping:
            column_added = self.add_mysql_virtual_column(table_name, virtual_column, expression)
            if not column_added:
                logger.warning(
                    "Virtual column %s was not added to %s because the table could not be locked (see the warning "
                    "above); its index is skipped and the next start retries",
                    virtual_column, table_name,
                )
                return

        # the index is named after its column, which is also the name MySQL gives an index created without a name,
        # so an existing index of that kind is found and not created again
        query = (
            f"ALTER TABLE {self.client.quoted_id(table_name)} "
            f"ADD INDEX {self.client.quoted_id(virtual_column)} ({self.client.quoted_id(virtual_column)})"
        )
        self.client.create_index(query, table_name, virtual_column)

    def add_mysql_virtual_column(self, table_name: str, virtual_column: str, expression: str) -> bool:
        """Add a virtual ``CHAR(128)`` column without an index (see ``create_mysql_virtual_column_index``).

        Args:
            table_name: Name of the table.
            virtual_column: Name of the new column.
            expression: SQL expression that generates the column's value, used as is.

        Returns:
            ``True`` if the column was added, or if another loader added it after the columns were read
            (duplicate column, ``MYSQL_DUPLICATE_COLUMN_ERROR``); ``False`` if the table could not be locked in any
            attempt (the library has logged a warning).

        Raises:
            OperationalError: A MySQL error other than a duplicate column or a lock timeout, for example 1054 (unknown column).
            DatabaseError: Any other error of the ``ALTER TABLE``, unchanged, for example ``ProgrammingError``
                1146 (table does not exist).
        """
        query = (
            f"ALTER TABLE {self.client.quoted_id(table_name)} "
            f"ADD COLUMN {self.client.quoted_id(virtual_column)} CHAR(128) AS ({expression})"
        )
        try:
            return self.client.alter_table(query, table_name)
        except OperationalError as exc:
            # another loader added the column after the columns were read
            error_code = exc.orig.args[0] if exc.orig is not None and exc.orig.args else None
            if error_code != MYSQL_DUPLICATE_COLUMN_ERROR:
                raise
            return True

    def create_pgsql_indexes(self, table_name: str, column_mapping: dict):
        fields = self.get_index_fields(table_name)

        for column_name, column_type in column_mapping.items():
            if column_name == "doc_id" or column_name not in fields:
                continue

            index_name = f"{table_name}_{FIELD_RE.sub('_', column_name)}"

            if column_type.lower() != "jsonb":
                query = f"CREATE INDEX {self.client.quoted_id(index_name)} ON {self.client.quoted_id(table_name)} ({self.client.quoted_id(column_name)})"
                self.client.create_index(query, table_name, index_name)
            else:
                for i, index_str in enumerate(self.sql_indexes["__common__"]["JSON"], start=1):
                    index_str_fmt = Template(index_str).safe_substitute({
                        "field": column_name, "data_type": column_type,
                    })
                    name = f"{table_name}_json_{i}"
                    query = f"CREATE INDEX {self.client.quoted_id(name)} ON {self.client.quoted_id(table_name)} {index_str_fmt}"
                    self.client.create_index(query, table_name, name)

        for i, custom in enumerate(self.sql_indexes.get(table_name, {}).get("custom", []), start=1):
            name = f"{table_name}_custom_{i}"
            query = f"CREATE INDEX {self.client.quoted_id(name)} ON {self.client.quoted_id(table_name)} {custom}"
            self.client.create_index(query, table_name, name)

    def create_indexes(self):
        # exclude tables that created externally https://github.com/JanssenProject/jans/issues/10512
        table_mapping = {
            k: v for k, v in self.client.get_table_mapping().items()
            if k in self.table_mapping_from_schema()
        }

        for table_name, column_mapping in table_mapping.items():
            if self.client.dialect == "pgsql":
                self.create_pgsql_indexes(table_name, column_mapping)
            else:
                self.create_mysql_indexes(table_name, column_mapping)

    def create_unique_indexes(self):
        """Create the unique indexes of ``jansPerson`` that do not exist yet.

        Unique indexes are constraints, not only a help for queries: a finished loader must mean that they exist.
        So, unlike the other indexes, which go through the library's bounded path (built concurrently on PostgreSQL)
        and are skipped with a warning while their table stays locked, a missing unique index is built with a plain
        unique-index statement (``CREATE UNIQUE INDEX`` or ``ALTER TABLE ... ADD UNIQUE INDEX``), which waits for its
        table lock without a bound; on PostgreSQL it also blocks writers of the table while it is built, once. A lookup
        first skips indexes that exist, so later starts send no statement and do not wait for the table lock.

        An invalid index (PostgreSQL) is only reported in the log, see ``unique_index_exists``.
        """
        for table_name, column in [
            ("jansPerson", "mail"),
            ("jansPerson", "uid"),
        ]:
            index_name = f"{table_name.lower()}_{column.lower()}_unique_idx"

            if self.unique_index_exists(table_name, column, index_name):
                continue

            if self.client.dialect == "mysql":
                query = f"ALTER TABLE {self.client.quoted_id(table_name)} ADD UNIQUE INDEX {self.client.quoted_id(index_name)} ({self.client.quoted_id(column)})"
            else:
                query = f"CREATE UNIQUE INDEX {self.client.quoted_id(index_name)} ON {self.client.quoted_id(table_name)} ({self.client.quoted_id(column)})"
            self.client.create_index(query)

    def unique_index_exists(self, table_name: str, column: str, index_name: str) -> bool:
        """Look up a unique index, so that a start sends no statement for an index that exists.

        Args:
            table_name: Name of the table.
            column: Name of the column whose values the index keeps unique.
            index_name: Name of the unique index.

        Returns:
            ``True`` if the index exists, or (PostgreSQL 12 and later) is still being built by a session whose
            progress this database role can see. An invalid index with no visible build (PostgreSQL only) also
            returns ``True``, but is reported in the log and left alone: the loader neither drops nor rebuilds it.
            ``False`` if it does not exist, or if the lookup failed (for example it timed
            out waiting for a lock): the caller then creates the index with a plain unique-index statement, which
            waits for the table lock and ignores "already exists".

        An invalid unique index may not reject duplicates. This loader does not build unique indexes concurrently,
        so an invalid one comes from a concurrent build run elsewhere: one that failed, or one that runs in a
        session this database role cannot see.
        """
        try:
            state = self.client.get_index_state(
                table_name, index_name, lock_timeout=UNIQUE_INDEX_LOOKUP_LOCK_TIMEOUT_SECONDS,
            )
        except (DBAPIError, IndexLockTimeoutError) as exc:
            logger.warning(
                "Could not look up unique index %s on %s (%s); creating it with a plain unique-index statement, "
                "which waits for the table lock",
                index_name, table_name, exc,
            )
            return False

        # get_index_state returns None if the index is missing; True if it is valid or (PostgreSQL 12 and later) still
        # being built by a session whose progress this database role can see; False (PostgreSQL only) if it is invalid
        # with no visible build: the build failed, or it runs in a session this role cannot see
        if state is None:
            return False
        if not state:
            qualified_name = f"{self.client.quoted_id(resolve_db_schema_name())}.{self.client.quoted_id(index_name)}"
            logger.error(
                "Unique index %s on %s is invalid, so duplicate %s values may be accepted. A CREATE UNIQUE INDEX "
                "CONCURRENTLY of it failed or is still running in another session. The loader leaves the index "
                "alone and continues. If no build of it is running (see pg_stat_activity, or on PostgreSQL 12 and "
                "later pg_stat_progress_create_index, as a superuser or a member of pg_read_all_stats), remove duplicate "
                "%s values if there are any, drop the index with DROP INDEX CONCURRENTLY %s and start the loader again, which "
                "then builds it.",
                index_name, table_name, column, column, qualified_name,
            )
        return True

    def import_builtin_ldif(self, ctx):
        optional_scopes = json.loads(self.manager.config.get("optional_scopes", "[]"))
        ldif_mappings = get_ldif_mappings_hook("sql", optional_scopes)

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
        self.create_unique_indexes()

        ctx = prepare_template_ctx(self.manager)

        if as_boolean(os.environ.get("CN_PERSISTENCE_IMPORT_BUILTIN_LDIF", "true")):
            logger.info("Importing builtin LDIF files")
            self.import_builtin_ldif(ctx)
        else:
            logger.warning(
                "The builtin LDIF files will not be imported as the feature is disabled. "
                "To enable the feature, set the environment variable CN_PERSISTENCE_IMPORT_BUILTIN_LDIF=true"
            )

    def update_schema(self):
        """Updates schema (may include data migration)"""

        table_mapping = self.client.get_table_mapping()

        def column_to_multivalued(table_name, col_name):
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
                with conn.begin():
                    conn.execute(text(f"ALTER TABLE {self.client.quoted_id(table_name)} DROP COLUMN {self.client.quoted_id(col_name)}"))
                    conn.execute(text(f"ALTER TABLE {self.client.quoted_id(table_name)} ADD COLUMN {self.client.quoted_id(col_name)} {data_type}"))

            # force-reload metadata as we may have changed the schema before migrating old data
            self.client._metadata = None

            # pre-populate the modified column
            for doc_id, value in values.items():
                if not value:
                    new_value = []
                else:
                    new_value = [value]

                if not self.client.use_simple_json:
                    new_value = {"v": new_value}
                self.client.update(table_name, doc_id, {col_name: new_value})

        def add_column(table_name, col_name):
            if col_name in table_mapping[table_name]:
                return

            data_type = self.get_data_type(col_name, table_name)
            with self.client.engine.connect() as conn:
                with conn.begin():
                    conn.execute(text(f"ALTER TABLE {self.client.quoted_id(table_name)} ADD COLUMN {self.client.quoted_id(col_name)} {data_type}"))

        def change_column_type(table_name, col_name, old_data_type, data_type):
            if self.client.dialect == "mysql":
                query = f"ALTER TABLE {self.client.quoted_id(table_name)} " \
                        f"MODIFY COLUMN {self.client.quoted_id(col_name)} {data_type}"
            else:
                query = f"ALTER TABLE {self.client.quoted_id(table_name)} " \
                        f"ALTER COLUMN {self.client.quoted_id(col_name)} TYPE {data_type}"

            with self.client.engine.connect() as conn:
                with conn.begin():
                    # mysql will raise error if changing type to text but the column already indexed without explicit key length
                    # hence the associated index must be dropped first
                    if self.client.dialect == "mysql" and old_data_type.startswith("VARCHAR") and data_type == "TEXT":
                        for idx in conn.execute(
                            text(
                                "SELECT index_name "
                                "FROM information_schema.statistics "
                                "WHERE table_name = :table_name "
                                "AND index_name LIKE :index_name "
                                "AND column_name = :col_name;"
                            ),
                            {
                                "table_name": table_name,
                                "index_name": f"{table_name}_{col_name}",
                                "col_name": col_name
                            },
                        ):
                            conn.execute(text(f"ALTER TABLE {self.client.quoted_id(table_name)} DROP INDEX {self.client.quoted_id(idx[0])}"))

                    # change the type
                    conn.execute(text(query))

        def column_from_multivalued(table_name, col_name):
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
                with conn.begin():
                    # mysql will raise error if dropping column which has functional index,
                    # hence the associated index must be dropped first
                    if self.client.dialect == "mysql":
                        for idx in conn.execute(
                            text(
                                "SELECT index_name "
                                "FROM information_schema.statistics "
                                "WHERE table_name = :table_name "
                                "AND index_name LIKE :index_name "
                                "AND expression LIKE :col_name;"
                            ),
                            {
                                "table_name": table_name,
                                "index_name": f"{table_name}_json_%",
                                "col_name": f"%{col_name}%"
                            },
                        ):
                            conn.execute(text(f"ALTER TABLE {self.client.quoted_id(table_name)} DROP INDEX {self.client.quoted_id(idx[0])}"))

                    # to change the storage format of a JSON column, drop the column and
                    # add the column back specifying the new storage format
                    conn.execute(text(f"ALTER TABLE {self.client.quoted_id(table_name)} DROP COLUMN {self.client.quoted_id(col_name)}"))
                    conn.execute(text(f"ALTER TABLE {self.client.quoted_id(table_name)} ADD COLUMN {self.client.quoted_id(col_name)} {data_type}"))

            # force-reload metadata as we may have changed the schema before migrating old data
            self.client._metadata = None

            # pre-populate the modified column
            for doc_id, value in values.items():
                simple_json = self.client.use_simple_json

                if not simple_json and value and value.get("v", []):
                    new_value = value["v"][0]
                elif simple_json and value:
                    new_value = value[0]
                else:
                    new_value = ""
                self.client.update(table_name, doc_id, {col_name: new_value})

        table_columns = self.table_mapping_from_schema()

        if self.client.dialect == "mysql":
            multivalued_type = "JSON"
        else:
            multivalued_type = "JSONB"

        for table_name, columns in table_columns.items():
            for column, data_type in columns.items():
                if column not in table_mapping[table_name]:
                    logger.info("Adding new column %s.%s", table_name, column)
                    add_column(table_name, column)

                else:
                    old_data_type = table_mapping[table_name][column]

                    if any([
                        # same type
                        data_type == old_data_type,
                        # same type (different alias)
                        data_type == "INT" and old_data_type == "INTEGER",
                        # same type (different alias) in Postgres
                        data_type == "TIMESTAMP" and old_data_type == "TIMESTAMP WITHOUT TIME ZONE",
                        # builtin columns
                        column in ("doc_id", "objectClass", "dn"),
                    ]):
                        # no-ops
                        continue

                    if data_type != multivalued_type and old_data_type != multivalued_type:
                        # change non-multivalued type
                        logger.info("Converting %s.%s column type from %s to %s", table_name, column, old_data_type, data_type)
                        change_column_type(table_name, column, old_data_type, data_type)

                    elif data_type == multivalued_type and old_data_type != multivalued_type:
                        # change type to multivalued (JSON type)
                        logger.info("Converting %s.%s column type from %s to multivalued %s", table_name, column, old_data_type, data_type)
                        column_to_multivalued(table_name, column)

                    elif data_type != multivalued_type and old_data_type == multivalued_type:
                        # change type from multivalued (JSON type)
                        logger.info("Converting %s.%s column type from multivalued %s to %s", table_name, column, old_data_type, data_type)
                        column_from_multivalued(table_name, column)

    def _import_ldif(self, path, ctx, transform_column_mapping=None):
        logger.info("Importing %s file", path)
        self.client.create_from_ldif(path, ctx, transform_column_mapping)

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
                "objectClass": "VARCHAR(48)",
                "dn": "VARCHAR(128)",
            })

            # make sure ``oc["may"]`` doesn't have duplicate attribute while preserving order
            for attr in list(dict.fromkeys(oc["may"])):
                data_type = self.get_data_type(attr, table)
                table_mapping[table].update({attr: data_type})
        return table_mapping

    def customize(self):
        logger.info("Importing custom LDIF files (if any)")
        ctx = prepare_template_ctx(self.manager)
        self.import_custom_ldif(ctx)

    def import_custom_ldif(self, ctx):
        custom_dir = Path("/app/custom_ldif")

        for file_ in sorted(custom_dir.rglob("*.ldif")):
            logger.info("Importing %s file", file_)
            self.client.upsert_from_file(file_, ctx, self.safe_column_mapping)

    def safe_column_mapping(self, table_name, column_mapping):
        if table_name == "jansToken" and "jansUsrId" in column_mapping:
            column_mapping["usrId"] = column_mapping.pop("jansUsrId", "")
        return column_mapping
