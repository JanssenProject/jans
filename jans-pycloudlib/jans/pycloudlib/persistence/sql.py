"""
jans.pycloudlib.persistence.sql
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This module contains various helpers related to SQL persistence.
"""

import logging
import os

from sqlalchemy import create_engine
from sqlalchemy import MetaData
from sqlalchemy import func
from sqlalchemy import select

from jans.pycloudlib import get_manager
from jans.pycloudlib.utils import encode_text
from jans.pycloudlib.utils import secure_password_file

logger = logging.getLogger(__name__)


def get_sql_password(manager) -> str:
    """Get password used for SQL database user.

    :returns: Plaintext password.
    """
    password_file = os.environ.get("CN_SQL_PASSWORD_FILE", "/etc/jans/conf/sql_password")
    salt = manager.secret.get("encoded_salt")
    return secure_password_file(password_file, salt)


class BaseClient:
    """Base class for SQL client adapter.
    """

    def __init__(self, manager):
        self.manager = manager
        self.engine = create_engine(
            self.engine_url,
            pool_pre_ping=True,
            hide_parameters=True,
        )
        self._metadata = None

    @property
    def metadata(self):
        """Lazy init of metadata."""

        if not self._metadata:
            self._metadata = MetaData(bind=self.engine, reflect=True)
        return self._metadata

    @property
    def dialect(self):
        """Dialect name."""
        raise NotImplementedError

    @property
    def connector(self):
        """Connector name."""
        raise NotImplementedError

    @property
    def quote_char(self):
        """Character used for quoting identifier."""
        raise NotImplementedError

    @property
    def engine_url(self):
        """Engine connection URL."""
        raise NotImplementedError

    def connected(self):
        """Check whether connection is alive by executing simple query.
        """

        with self.engine.connect() as conn:
            result = conn.execute("SELECT 1 AS is_alive")
            return result.fetchone()[0] > 0

    def get_table_mapping(self) -> dict:
        """Get mapping of column name and type from all tables.
        """

        table_mapping = {}
        for table_name, table in self.metadata.tables.items():
            table_mapping[table_name] = {
                column.name: column.type.__class__.__name__
                for column in table.c
            }
        return table_mapping

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

    def quoted_id(self, identifier):
        """Get quoted identifier name."""
        return f"{self.quote_char}{identifier}{self.quote_char}"

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
                self.on_create_table_error(exc)

    def on_create_table_error(self, exc):
        """Callback called when error occured during table creation."""
        raise NotImplementedError

    def create_index(self, query):
        """Create index using raw query."""

        with self.engine.connect() as conn:
            try:
                conn.execute(query)
            except Exception as exc:  # noqa: B902
                self.on_create_index_error(exc)

    def on_create_index_error(self, exc):
        """Callback called when error occured during index creation."""
        raise NotImplementedError

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
                self.on_insert_into_error(exc)

    def on_insert_into_error(self, exc):
        """Callback called when error occured during row insertion."""
        raise NotImplementedError

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
        raise NotImplementedError


class PostgresqlClient(BaseClient):
    """Class for PostgreSQL adapter.
    """

    @property
    def dialect(self):
        return "pgsql"

    @property
    def connector(self):
        """Connector name."""
        return "postgresql+psycopg2"

    @property
    def quote_char(self):
        """Character used for quoting identifier."""
        return '"'

    @property
    def engine_url(self):
        host = os.environ.get("CN_SQL_DB_HOST", "localhost")
        port = os.environ.get("CN_SQL_DB_PORT", 5432)
        database = os.environ.get("CN_SQL_DB_NAME", "jans")
        user = os.environ.get("CN_SQL_DB_USER", "jans")
        password = get_sql_password(self.manager)
        return f"{self.connector}://{user}:{password}@{host}:{port}/{database}"

    def on_create_table_error(self, exc):
        if exc.orig.pgcode in ["42P07"]:
            # error with following code will be suppressed
            # - 42P07: relation exists
            pass
        else:
            raise exc

    def on_create_index_error(self, exc):
        if exc.orig.pgcode in ["42P07"]:
            # error with following code will be suppressed
            # - 42P07: relation exists
            pass
        else:
            raise exc

    def on_insert_into_error(self, exc):
        if exc.orig.pgcode in ["23505"]:
            # error with following code will be suppressed
            # - 23505: unique violation
            pass
        else:
            raise exc

    @property
    def server_version(self):
        """Display server version."""
        return self.engine.scalar("SHOW server_version")


class MysqlClient(BaseClient):
    """Class for MySQL adapter.
    """

    @property
    def dialect(self):
        return "mysql"

    @property
    def connector(self):
        """Connector name."""
        return "mysql+pymysql"

    @property
    def quote_char(self):
        """Character used for quoting identifier."""
        return "`"

    @property
    def engine_url(self):
        host = os.environ.get("CN_SQL_DB_HOST", "localhost")
        port = os.environ.get("CN_SQL_DB_PORT", 3306)
        database = os.environ.get("CN_SQL_DB_NAME", "jans")
        user = os.environ.get("CN_SQL_DB_USER", "jans")
        password = get_sql_password(self.manager)
        return f"{self.connector}://{user}:{password}@{host}:{port}/{database}"

    def on_create_table_error(self, exc):
        if exc.orig.args[0] in [1050]:
            # error with following code will be suppressed
            # - 1050: table exists
            pass
        else:
            raise exc

    def on_create_index_error(self, exc):
        if exc.orig.args[0] in [1061]:
            # error with following code will be suppressed
            # - 1061: duplicate key name (index)
            pass
        else:
            raise exc

    def on_insert_into_error(self, exc):
        if exc.orig.args[0] in [1062]:
            # error with following code will be suppressed
            # - 1062: duplicate entry
            pass
        else:
            raise exc

    @property
    def server_version(self):
        """Display server version."""
        return self.engine.scalar("SELECT VERSION()")


class SQLClient:
    """This class interacts with SQL database.
    """

    #: Methods from adapter
    _allowed_adapter_methods = (
        "connected",
        "create_table",
        "get_table_mapping",
        "create_index",
        "quoted_id",
        "row_exists",
        "insert_into",
        "get",
        "update",
        "search",
        "server_version",
    )

    def __init__(self, manager=None):
        manager = manager or get_manager()
        dialect = os.environ.get("CN_SQL_DB_DIALECT", "mysql")
        if dialect in ("pgsql", "postgresql"):
            self.adapter = PostgresqlClient(manager)
        elif dialect == "mysql":
            self.adapter = MysqlClient(manager)

        self._adapter_methods = [
            method for method in dir(self.adapter)
            if method in self._allowed_adapter_methods
        ]

    def __getattr__(self, name):
        # call adapter method first (if any)
        if name in self._adapter_methods:
            return getattr(self.adapter, name)

        raise AttributeError(
            f"'{self.__class__.__name__}' object has no attribute '{name}'"
        )


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
