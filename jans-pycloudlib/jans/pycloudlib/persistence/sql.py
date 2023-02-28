"""This module contains various helpers related to SQL persistence."""

from __future__ import annotations

import json
import logging
import os
import re
import typing as _t
import warnings
from collections import defaultdict
from functools import cached_property
from tempfile import NamedTemporaryFile

from sqlalchemy.exc import DatabaseError
from sqlalchemy.exc import SAWarning
from sqlalchemy import create_engine
from sqlalchemy import MetaData
from sqlalchemy import func
from sqlalchemy import select
from ldif import LDIFParser
from ldap3.utils import dn as dnutils

from jans.pycloudlib.utils import encode_text
from jans.pycloudlib.utils import safe_render

if _t.TYPE_CHECKING:  # pragma: no cover
    # imported objects for function type hint, completion, etc.
    # these won't be executed in runtime
    from sqlalchemy.engine import Engine
    from jans.pycloudlib.manager import Manager

logger = logging.getLogger(__name__)

SERVER_VERSION_RE = re.compile(r"\d+(.\d+)+")


def get_sql_password(manager: Manager) -> str:
    """Get password used for SQL database user.

    Priority:

    1. get from password file
    2. get from secrets

    Returns:
        Plaintext password.
    """
    # ignore bandit rule as secret_name refers to attribute name of secrets
    secret_name = "sql_password"  # nosec: B105
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
    """Class for PostgreSQL adapter."""

    #: Dialect name
    dialect = "pgsql"

    #: Connector name.
    connector = "postgresql+psycopg2"

    #: Character used for quoting identifier.
    quote_char = '"'

    #: Query to display server version
    server_version_query = "SHOW server_version"

    def on_create_table_error(self, exc: DatabaseError) -> None:
        """Handle table creation error.

        Args:
            exc: Exception instance.
        """
        # re-raise exception UNLESS error code is in the following list
        # - 42P07: relation exists
        if exc.orig.pgcode not in ["42P07"]:
            raise exc

    def on_create_index_error(self, exc: DatabaseError) -> None:
        """Handle index creation error.

        Args:
            exc: Exception instance.
        """
        # re-raise exception UNLESS error code is in the following list
        # - 42P07: relation exists
        if exc.orig.pgcode not in ["42P07"]:
            raise exc

    def on_insert_into_error(self, exc: DatabaseError) -> None:
        """Handle row insertion error.

        Args:
            exc: Exception instance.
        """
        # re-raise exception UNLESS error code is in the following list
        # - 23505: unique violation
        if exc.orig.pgcode not in ["23505"]:
            raise exc


class MysqlAdapter:
    """Class for MySQL adapter."""

    #: Dialect name.
    dialect = "mysql"

    #: Connector name.
    connector = "mysql+pymysql"

    #: Character used for quoting identifier.
    quote_char = "`"

    #: Query to display server version
    server_version_query = "SELECT VERSION()"

    def on_create_table_error(self, exc: DatabaseError) -> None:
        """Handle table creation error.

        Args:
            exc: Exception instance.
        """
        # re-raise exception UNLESS error code is in the following list
        # - 1050: table exists
        if exc.orig.args[0] not in [1050]:
            raise exc

    def on_create_index_error(self, exc: DatabaseError) -> None:
        """Handle index creation error.

        Args:
            exc: Exception instance.
        """
        # re-raise exception UNLESS error code is in the following list
        # - 1061: duplicate key name (index)
        if exc.orig.args[0] not in [1061]:
            raise exc

    def on_insert_into_error(self, exc: DatabaseError) -> None:
        """Handle row insertion error.

        Args:
            exc: Exception instance.
        """
        # re-raise exception UNLESS error code is in the following list
        # - 1062: duplicate entry
        if exc.orig.args[0] not in [1062]:
            raise exc


def doc_id_from_dn(dn: str) -> str:
    """Determine document ID based on LDAP's DN.

    Args:
        dn: LDAP's DN string.
    """
    parsed_dn = dnutils.parse_dn(dn)
    doc_id: str = parsed_dn[0][1]

    if doc_id == "jans":
        doc_id = "_"
    return doc_id


class SqlSchemaMixin:
    """Mixin class to deal with SQL schema."""

    @property
    def schema_files(self) -> list[str]:
        """Get list of schema files."""
        files = [
            "/app/schema/jans_schema.json",
            "/app/schema/custom_schema.json",
        ]
        return files

    @cached_property
    def sql_data_types(self) -> dict[str, dict[str, _t.Any]]:
        """Get list of data types from pre-defined file."""
        with open("/app/static/rdbm/sql_data_types.json") as f:
            return json.loads(f.read())  # type: ignore

    @cached_property
    def sql_data_types_mapping(self) -> dict[str, dict[str, _t.Any]]:
        """Get a mapping of data types from pre-defined file."""
        with open("/app/static/rdbm/ldap_sql_data_type_mapping.json") as f:
            return json.loads(f.read())  # type: ignore

    @cached_property
    def attr_types(self) -> list[dict[str, _t.Any]]:
        """Get list of attribute types from pre-defined file."""
        types = []
        for fn in self.schema_files:
            with open(fn) as f:
                schema = json.loads(f.read())
                types += schema["attributeTypes"]
        return types

    @cached_property
    def opendj_attr_types(self) -> dict[str, str]:
        """Get a mapping of OpenDJ attribute types from pre-defined file."""
        with open("/app/static/rdbm/opendj_attributes_syntax.json") as f:
            return json.loads(f.read())  # type: ignore

    def get_attr_syntax(self, attr: str) -> str:
        """Get attribute syntax.

        Args:
            attr: Attribute name.
        """
        syntax = ""
        for attr_type in self.attr_types:
            if attr not in attr_type["names"]:
                continue

            if attr_type.get("multivalued"):
                syntax = "JSON"
            else:
                syntax = attr_type["syntax"]
            break

        if not syntax:
            # fallback to OpenDJ attribute type
            syntax = self.opendj_attr_types.get(attr, "") or "1.3.6.1.4.1.1466.115.121.1.15"
        return syntax


class SqlClient(SqlSchemaMixin):
    """This class interacts with SQL database.

    Args:
        manager: An instance of manager class.
        *args: Positional arguments.
        **kwargs: Keyword arguments.
    """

    def __init__(self, manager: Manager, *args: _t.Any, **kwargs: _t.Any) -> None:
        self.manager = manager

        dialect = os.environ.get("CN_SQL_DB_DIALECT", "mysql")
        if dialect in ("pgsql", "postgresql"):
            self.adapter = PostgresqlAdapter()  # type: _t.Union[PostgresqlAdapter, MysqlAdapter]
        else:
            self.adapter = MysqlAdapter()

        self.dialect = self.adapter.dialect
        self._metadata: _t.Optional[MetaData] = None

    @cached_property
    def engine(self) -> Engine:
        """Lazy init of engine instance object."""
        return create_engine(self.engine_url, pool_pre_ping=True, hide_parameters=True)

    @property
    def engine_url(self) -> str:
        """Engine connection URL."""
        host = os.environ.get("CN_SQL_DB_HOST", "localhost")
        port = os.environ.get("CN_SQL_DB_PORT", 3306)
        database = os.environ.get("CN_SQL_DB_NAME", "jans")
        user = os.environ.get("CN_SQL_DB_USER", "jans")
        password = get_sql_password(self.manager)
        return f"{self.adapter.connector}://{user}:{password}@{host}:{port}/{database}"

    @property
    def metadata(self) -> MetaData:
        """Lazy init of metadata."""
        if not self._metadata:
            with warnings.catch_warnings():
                # postgresql driver will show warnings about unsupported reflection
                # on expression-based index, i.e. `lower(uid::text)`; but we don't
                # want to clutter the logs with these warnings, hence we suppress the
                # warnings
                warnings.filterwarnings(
                    "ignore",
                    message="Skipped unsupported reflection of expression-based index",
                    category=SAWarning,
                )

                # do reflection on database table
                self._metadata = MetaData(bind=self.engine)
                self._metadata.reflect()
        return self._metadata

    def connected(self) -> bool:
        """Check whether connection is alive by executing simple query."""
        with self.engine.connect() as conn:
            result = conn.execute("SELECT 1 AS is_alive")
            return bool(result.fetchone()[0] > 0)

    def create_table(self, table_name: str, column_mapping: dict[str, str], pk_column: str) -> None:
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
            except DatabaseError as exc:  # noqa: B902
                self.adapter.on_create_table_error(exc)

    def get_table_mapping(self) -> dict[str, dict[str, str]]:
        """Get mapping of column name and type from all tables."""
        table_mapping: dict[str, dict[str, str]] = defaultdict(dict)
        for table_name, table in self.metadata.tables.items():
            for column in table.c:
                # truncate COLLATION string
                if getattr(column.type, "collation", None):
                    column.type.collation = None
                table_mapping[table_name][column.name] = str(column.type)
        return dict(table_mapping)

    def create_index(self, query: str) -> None:
        """Create index using raw query."""
        with self.engine.connect() as conn:
            try:
                conn.execute(query)
            except DatabaseError as exc:  # noqa: B902
                self.adapter.on_create_index_error(exc)

    def quoted_id(self, identifier: str) -> str:
        """Get quoted identifier name."""
        return f"{self.adapter.quote_char}{identifier}{self.adapter.quote_char}"

    def row_exists(self, table_name: str, id_: str) -> bool:
        """Check whether a row is exist."""
        table = self.metadata.tables.get(table_name)
        if table is None:
            return False

        query = select([func.count()]).select_from(table).where(
            table.c.doc_id == id_
        )
        with self.engine.connect() as conn:
            result = conn.execute(query)
            return bool(result.fetchone()[0] > 0)

    def insert_into(self, table_name: str, column_mapping: dict[str, _t.Any]) -> None:
        """Insert a row into a table."""
        table = self.metadata.tables.get(table_name)

        for column in table.c:
            unmapped = column.name not in column_mapping

            if self.dialect == "mysql":
                json_type = "json"
                json_default_values: dict[str, _t.Any] | list[_t.Any] = {"v": []}
            else:
                json_type = "jsonb"
                json_default_values = []

            is_json = bool(column.type.__class__.__name__.lower() == json_type)

            if not all([unmapped, is_json]):
                continue
            column_mapping[column.name] = json_default_values

        query = table.insert().values(column_mapping)
        with self.engine.connect() as conn:
            try:
                conn.execute(query)
            except DatabaseError as exc:  # noqa: B902
                self.adapter.on_insert_into_error(exc)

    def get(self, table_name: str, id_: str, column_names: _t.Union[list[str], None] = None) -> dict[str, _t.Any]:
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

    def update(self, table_name: str, id_: str, column_mapping: dict[str, _t.Any]) -> bool:
        """Update a table row with matching ID."""
        table = self.metadata.tables.get(table_name)

        query = table.update().where(table.c.doc_id == id_).values(column_mapping)
        with self.engine.connect() as conn:
            result = conn.execute(query)
        return bool(result.rowcount)

    def search(self, table_name: str, column_names: _t.Union[list[str], None] = None) -> _t.Iterator[dict[str, _t.Any]]:
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
    def server_version(self) -> str:
        """Display server version."""
        version: str = self.engine.scalar(self.adapter.server_version_query)
        return version

    def _transform_value(self, key: str, values: _t.Any) -> _t.Any:
        """Transform value from one to another based on its data type.

        Args:
            key: Attribute name.
            values: Pre-transformed values.
        """
        type_ = self.sql_data_types.get(key, {})

        if not type_:
            attr_syntax = self.get_attr_syntax(key)
            type_ = self.sql_data_types_mapping[attr_syntax]

        type_ = type_.get(self.dialect) or type_["mysql"]
        data_type = type_.get("type", "")

        if data_type in ("SMALLINT", "BOOL", "BOOLEAN"):
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

        if data_type == "JSONB":
            return values

        # fallback
        return values[0]

    def _data_from_ldif(self, filename: str) -> _t.Iterator[tuple[str, dict[str, _t.Any]]]:
        """Get data from parsed LDIF file.

        Args:
            filename: LDIF filename.
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
                    attr_mapping[attr] = self._transform_value(attr, entry[attr])
                yield table_name, attr_mapping

    def create_from_ldif(self, filepath: str, ctx: dict[str, _t.Any]) -> None:
        """Create entry with data loaded from an LDIF template file.

        Args:
            filepath: Path to LDIF template file.
            ctx: Key-value pairs of context that rendered into LDIF template file.
        """
        with open(filepath) as src, NamedTemporaryFile("w+") as dst:
            dst.write(safe_render(src.read(), ctx))
            # ensure rendered template is written
            dst.flush()

            for table_name, column_mapping in self._data_from_ldif(dst.name):
                self.insert_into(table_name, column_mapping)

    def get_server_version(self) -> tuple[int, ...]:
        """Get server version as tuple."""
        # major and minor format
        version = [0, 0]

        pattern = SERVER_VERSION_RE.search(self.server_version)
        if pattern:
            version = [int(comp) for comp in pattern.group().split(".")]
        return tuple(version)


def render_sql_properties(manager: Manager, src: str, dest: str) -> None:
    """Render file contains properties to connect to SQL database server.

    Args:
        manager: An instance of :class:`~jans.pycloudlib.manager.Manager`.
        src: Absolute path to the template.
        dest: Absolute path where generated file is located.
    """
    with open(src) as f:
        txt = f.read()

    with open(dest, "w") as f:
        db_dialect = os.environ.get("CN_SQL_DB_DIALECT", "mysql")
        db_name = os.environ.get("CN_SQL_DB_NAME", "jans")

        # In MySQL, physically, a schema is synonymous with a database
        if db_dialect == "mysql":
            default_schema = db_name
        else:  # likely postgres
            # by default, PostgreSQL creates schema called `public` upon database creation
            default_schema = "public"
        db_schema = os.environ.get("CN_SQL_DB_SCHEMA", "") or default_schema

        rendered_txt = txt % {
            "rdbm_db": db_name,
            "rdbm_schema": db_schema,
            "rdbm_type": "postgresql" if db_dialect == "pgsql" else "mysql",
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
