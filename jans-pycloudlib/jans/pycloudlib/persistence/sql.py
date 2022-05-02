"""
jans.pycloudlib.persistence.sql
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This module contains various helpers related to SQL persistence.
"""

import json
import logging
import os
from collections import defaultdict
from tempfile import NamedTemporaryFile

from sqlalchemy import create_engine
from sqlalchemy import MetaData
from sqlalchemy import func
from sqlalchemy import select
from ldif import LDIFParser
from ldap3.utils import dn as dnutils

from jans.pycloudlib.utils import encode_text
from jans.pycloudlib.utils import safe_render

logger = logging.getLogger(__name__)


def get_sql_password(manager) -> str:
    """Get password used for SQL database user.

    Priority:

    1. get from password file
    2. get from secrets

    :returns: Plaintext password.
    """
    secret_name = "sql_password"
    password_file = os.environ.get("CN_SQL_PASSWORD_FILE", "/etc/jans/conf/sql_password")

    if os.path.isfile(password_file):
        with open(password_file) as f:
            password = f.read().strip()
            manager.secret.set(secret_name, password)
            logger.warning(
                f"Loading password from {password_file} file is deprecated and will be removed in future releases. "
                f"Note, the password has been saved to secrets with key {secret_name} for later usage."
            )
    else:
        # get from secrets (if any)
        password = manager.secret.get(secret_name)
    return password


class PostgresqlAdapter:
    """Class for PostgreSQL adapter.
    """

    #: Dialect name
    dialect = "pgsql"

    #: Connector name.
    connector = "postgresql+psycopg2"

    #: Character used for quoting identifier.
    quote_char = '"'

    #: Query to display server version
    server_version_query = "SHOW server_version"

    def on_create_table_error(self, exc):
        # re-raise exception UNLESS error code is in the following list
        # - 42P07: relation exists
        if exc.orig.pgcode not in ["42P07"]:
            raise exc

    def on_create_index_error(self, exc):
        # re-raise exception UNLESS error code is in the following list
        # - 42P07: relation exists
        if exc.orig.pgcode not in ["42P07"]:
            raise exc

    def on_insert_into_error(self, exc):
        # re-raise exception UNLESS error code is in the following list
        # - 23505: unique violation
        if exc.orig.pgcode not in ["23505"]:
            raise exc


class MysqlAdapter:
    """Class for MySQL adapter.
    """

    #: Dialect name
    dialect = "mysql"

    #: Connector name.
    connector = "mysql+pymysql"

    #: Character used for quoting identifier.
    quote_char = "`"

    #: Query to display server version
    server_version_query = "SELECT VERSION()"

    def on_create_table_error(self, exc):
        # re-raise exception UNLESS error code is in the following list
        # - 1050: table exists
        if exc.orig.args[0] not in [1050]:
            raise exc

    def on_create_index_error(self, exc):
        # re-raise exception UNLESS error code is in the following list
        # - 1061: duplicate key name (index)
        if exc.orig.args[0] not in [1061]:
            raise exc

    def on_insert_into_error(self, exc):
        # re-raise exception UNLESS error code is in the following list
        # - 1062: duplicate entry
        if exc.orig.args[0] not in [1062]:
            raise exc


def doc_id_from_dn(dn):
    parsed_dn = dnutils.parse_dn(dn)
    doc_id = parsed_dn[0][1]

    if doc_id == "jans":
        doc_id = "_"
    return doc_id


class SqlClient:
    """This class interacts with SQL database.
    """

    def __init__(self, manager, *args, **kwargs):
        self.manager = manager

        dialect = os.environ.get("CN_SQL_DB_DIALECT", "mysql")
        if dialect in ("pgsql", "postgresql"):
            self.adapter = PostgresqlAdapter()
        else:
            self.adapter = MysqlAdapter()

        self.dialect = self.adapter.dialect
        self.schema_files = [
            "/app/schema/jans_schema.json",
            "/app/schema/custom_schema.json",
        ]

        self._sql_data_types = {}
        self._sql_data_types_mapping = {}
        self._attr_types = []
        self._opendj_attr_types = {}
        self._engine = None
        self._metadata = None

    @property
    def sql_data_types(self):
        if not self._sql_data_types:
            with open("/app/static/rdbm/sql_data_types.json") as f:
                self._sql_data_types = json.loads(f.read())
        return self._sql_data_types

    @property
    def sql_data_types_mapping(self):
        if not self._sql_data_types_mapping:
            with open("/app/static/rdbm/ldap_sql_data_type_mapping.json") as f:
                self._sql_data_types_mapping = json.loads(f.read())
        return self._sql_data_types_mapping

    @property
    def attr_types(self):
        if not self._attr_types:
            for fn in self.schema_files:
                with open(fn) as f:
                    schema = json.loads(f.read())
                    self._attr_types += schema["attributeTypes"]
        return self._attr_types

    @property
    def opendj_attr_types(self):
        if not self._opendj_attr_types:
            with open("/app/static/rdbm/opendj_attributes_syntax.json") as f:
                self._opendj_attr_types = json.loads(f.read())
        return self._opendj_attr_types

    @property
    def engine(self):
        if not self._engine:
            self._engine = create_engine(self.engine_url, pool_pre_ping=True, hide_parameters=True)
        return self._engine

    @property
    def engine_url(self):
        """Engine connection URL."""

        host = os.environ.get("CN_SQL_DB_HOST", "localhost")
        port = os.environ.get("CN_SQL_DB_PORT", 3306)
        database = os.environ.get("CN_SQL_DB_NAME", "jans")
        user = os.environ.get("CN_SQL_DB_USER", "jans")
        password = get_sql_password(self.manager)
        return f"{self.adapter.connector}://{user}:{password}@{host}:{port}/{database}"

    @property
    def metadata(self):
        """Lazy init of metadata."""

        if not self._metadata:
            self._metadata = MetaData(bind=self.engine)
            self._metadata.reflect()
        return self._metadata

    def connected(self):
        """Check whether connection is alive by executing simple query.
        """

        with self.engine.connect() as conn:
            result = conn.execute("SELECT 1 AS is_alive")
            return result.fetchone()[0] > 0

    def create_table(self, table_name: str, column_mapping: dict, pk_column: str):
        """Create table with its columns."""

        columns = []
        for column_name, column_type in column_mapping.items():
            column_def = f"{self.quoted_id(column_name)} {column_type}"

            if column_name == pk_column:
                column_def += " NOT NULL UNIQUE"
            columns.append(column_def)

        columns_fmt = ", ".join(columns)
        pk_def = f"PRIMARY KEY ({self.quoted_id(pk_column)})"
        query = f"CREATE TABLE {self.quoted_id(table_name)} ({columns_fmt}, {pk_def})"

        with self.engine.connect() as conn:
            try:
                conn.execute(query)
                # refresh metadata as we have newly created table
                self.metadata.reflect()
            except Exception as exc:  # noqa: B902
                self.adapter.on_create_table_error(exc)

    def get_table_mapping(self) -> dict:
        """Get mapping of column name and type from all tables.
        """

        table_mapping = defaultdict(dict)
        for table_name, table in self.metadata.tables.items():
            for column in table.c:
                # truncate COLLATION string
                if getattr(column.type, "collation", None):
                    column.type.collation = None
                table_mapping[table_name][column.name] = str(column.type)
        return dict(table_mapping)

    def create_index(self, query):
        """Create index using raw query."""

        with self.engine.connect() as conn:
            try:
                conn.execute(query)
            except Exception as exc:  # noqa: B902
                self.adapter.on_create_index_error(exc)

    def quoted_id(self, identifier):
        """Get quoted identifier name."""
        return f"{self.adapter.quote_char}{identifier}{self.adapter.quote_char}"

    def row_exists(self, table_name, id_):
        """Check whether a row is exist."""

        table = self.metadata.tables.get(table_name)
        if table is None:
            return False

        query = select([func.count()]).select_from(table).where(
            table.c.doc_id == id_
        )
        with self.engine.connect() as conn:
            result = conn.execute(query)
            return result.fetchone()[0] > 0

    def insert_into(self, table_name, column_mapping):
        """Insert a row into a table."""

        table = self.metadata.tables.get(table_name)

        for column in table.c:
            unmapped = column.name not in column_mapping
            is_json = column.type.__class__.__name__.lower() == "json"

            if not all([unmapped, is_json]):
                continue
            column_mapping[column.name] = {"v": []}

        query = table.insert().values(column_mapping)
        with self.engine.connect() as conn:
            try:
                conn.execute(query)
            except Exception as exc:  # noqa: B902
                self.adapter.on_insert_into_error(exc)

    def get(self, table_name, id_, column_names=None) -> dict:
        """Get a row from a table with matching ID."""

        table = self.metadata.tables.get(table_name)

        attrs = column_names or []
        if attrs:
            cols = [table.c[attr] for attr in attrs]
        else:
            cols = [table]

        query = select(cols).select_from(table).where(
            table.c.doc_id == id_
        )
        with self.engine.connect() as conn:
            result = conn.execute(query)
            entry = result.fetchone()

        if not entry:
            return {}
        return dict(entry)

    def update(self, table_name, id_, column_mapping) -> bool:
        """Update a table row with matching ID."""

        table = self.metadata.tables.get(table_name)

        query = table.update().where(table.c.doc_id == id_).values(column_mapping)
        with self.engine.connect() as conn:
            result = conn.execute(query)
        return bool(result.rowcount)

    def search(self, table_name, column_names=None) -> dict:
        """Get all rows from a table."""

        table = self.metadata.tables.get(table_name)

        attrs = column_names or []
        if attrs:
            cols = [table.c[attr] for attr in attrs]
        else:
            cols = [table]

        query = select(cols).select_from(table)
        with self.engine.connect() as conn:
            result = conn.execute(query)
            for entry in result:
                yield dict(entry)

    @property
    def server_version(self):
        """Display server version."""
        return self.engine.scalar(self.adapter.server_version_query)

    def get_attr_syntax(self, attr):
        for attr_type in self.attr_types:
            if attr not in attr_type["names"]:
                continue
            if attr_type.get("multivalued"):
                return "JSON"
            return attr_type["syntax"]

        # fallback to OpenDJ attribute type
        return self.opendj_attr_types.get(attr) or "1.3.6.1.4.1.1466.115.121.1.15"

    def _transform_value(self, key, values):
        type_ = self.sql_data_types.get(key)

        if not type_:
            attr_syntax = self.get_attr_syntax(key)
            type_ = self.sql_data_types_mapping[attr_syntax]

        type_ = type_.get(self.dialect) or type_["mysql"]
        data_type = type_["type"]

        if data_type in ("SMALLINT", "BOOL",):
            if values[0].lower() in ("1", "on", "true", "yes", "ok"):
                return 1 if data_type == "SMALLINT" else True
            return 0 if data_type == "SMALLINT" else False

        if data_type == "INT":
            return int(values[0])

        if data_type in ("DATETIME(3)", "TIMESTAMP",):
            dval = values[0].strip("Z")
            sep = " "
            postfix = ""
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

        # fallback
        return values[0]

    def _data_from_ldif(self, filename):
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
                    attr_mapping[attr] = self._transform_value(attr, entry[attr])
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


def render_sql_properties(manager, src: str, dest: str) -> None:
    """Render file contains properties to connect to SQL database server.

    :params manager: An instance of :class:`~jans.pycloudlib.manager._Manager`.
    :params src: Absolute path to the template.
    :params dest: Absolute path where generated file is located.
    """

    with open(src) as f:
        txt = f.read()

    with open(dest, "w") as f:
        rendered_txt = txt % {
            "rdbm_db": os.environ.get("CN_SQL_DB_NAME", "jans"),
            "rdbm_type": os.environ.get("CN_SQL_DB_DIALECT", "mysql"),
            "rdbm_host": os.environ.get("CN_SQL_DB_HOST", "localhost"),
            "rdbm_port": os.environ.get("CN_SQL_DB_PORT", 3306),
            "rdbm_user": os.environ.get("CN_SQL_DB_USER", "jans"),
            "rdbm_password_enc": encode_text(
                get_sql_password(manager),
                manager.secret.get("encoded_salt"),
            ).decode(),
            "server_time_zone": os.environ.get("CN_SQL_DB_TIMEZONE", "UTC"),
        }
        f.write(rendered_txt)
