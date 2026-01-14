"""This module contains various helpers related to SQL persistence."""

from __future__ import annotations

import atexit
import json
import logging
import os
import re
import typing as _t
import warnings
import weakref
from collections import defaultdict
from collections.abc import Callable
from functools import cached_property
from pathlib import Path
from tempfile import NamedTemporaryFile

import javaproperties
from sqlalchemy.exc import DatabaseError
from sqlalchemy.exc import SAWarning
from sqlalchemy import create_engine
from sqlalchemy import MetaData
from sqlalchemy import func
from sqlalchemy import select
from sqlalchemy import delete
from sqlalchemy import event
from sqlalchemy import text
from sqlalchemy.engine.url import URL
from sqlalchemy.dialects.mysql import insert as mysql_insert
from sqlalchemy.dialects.postgresql import insert as postgres_insert
from ldif import LDIFParser
from ldap3.utils import dn as dnutils
from pymysql.err import ProgrammingError

from jans.pycloudlib.utils import encode_text
from jans.pycloudlib.utils import safe_render
from jans.pycloudlib.utils import get_password_from_file
from jans.pycloudlib.utils import as_boolean
from jans.pycloudlib.utils import exec_cmd

from google.cloud.sql.connector import Connector
from google.cloud.sql.connector import IPTypes

if _t.TYPE_CHECKING:  # pragma: no cover
    # imported objects for function type hint, completion, etc.
    # these won't be executed in runtime
    from sqlalchemy.engine import Engine
    from sqlalchemy.schema import Table
    from jans.pycloudlib.manager import Manager

logger = logging.getLogger(__name__)

SERVER_VERSION_RE = re.compile(r"[\d.]+")
SIMPLE_JSON_RE = re.compile(r"(mysql.simple-json)(=)(.*)")
SQL_OVERRIDES_FILE = "/etc/jans/conf/sql-overrides.json"


class CloudSqlConnectorMixin:
    """Mixin class providing Cloud SQL Python Connector support.

    This mixin centralizes the Cloud SQL Connector lifecycle management,
    instance name validation, and connection creator logic for database adapters.

    Subclasses must define:
        cloudsql_driver: The driver string for the connector (e.g., "pg8000", "pymysql")
        fallback_driver_name: Human-readable driver name for warning messages
    """

    #: Driver string used by Cloud SQL Connector (e.g., "pg8000", "pymysql")
    cloudsql_driver: str = ""

    #: Human-readable driver name for fallback warning messages
    fallback_driver_name: str = ""

    def __init__(self) -> None:
        self._cloudsql_connector: _t.Any = None

    @property
    def cloudsql_connector_enabled(self) -> bool:
        """Check if Cloud SQL Python Connector is enabled.

        Returns:
            True if CN_SQL_CLOUDSQL_CONNECTOR_ENABLED is set to true, False otherwise.
        """
        return as_boolean(os.environ.get("CN_SQL_CLOUDSQL_CONNECTOR_ENABLED", "false"))

    def _get_instance_connection_name(self) -> str:
        """Get and validate the Cloud SQL instance connection name.

        Returns:
            The validated instance connection name.

        Raises:
            RuntimeError: If CN_SQL_CLOUDSQL_INSTANCE_CONNECTION_NAME is not set or empty.
        """
        instance_connection_name = os.environ.get(
            "CN_SQL_CLOUDSQL_INSTANCE_CONNECTION_NAME", ""
        ).strip()

        if not instance_connection_name:
            raise RuntimeError(
                "CN_SQL_CLOUDSQL_INSTANCE_CONNECTION_NAME environment variable is not set or empty. "
                "Please set it to your Cloud SQL instance connection name in the format: "
                "project:region:instance (e.g., my-project:us-central1:my-instance)"
            )

        return instance_connection_name

    def _ensure_connector(self) -> None:
        """Initialize the Cloud SQL Connector if not already created.

        Uses LAZY refresh strategy for deferred token refresh.
        """
        if self._cloudsql_connector is None:
            self._cloudsql_connector = Connector(refresh_strategy="LAZY")

    def get_cloudsql_connection_creator(self, manager: Manager) -> _t.Callable[[], _t.Any]:
        """Create a connection factory for Cloud SQL Python Connector.

        This function creates a SQLAlchemy-compatible connection factory that uses
        Google Cloud SQL Python Connector with LAZY refresh strategy and Private IP.

        The password is fetched fresh on each connection to support runtime
        password rotation without requiring application restart.

        Environment variables used:
            - CN_SQL_CLOUDSQL_INSTANCE_CONNECTION_NAME: Cloud SQL instance connection name
              (format: project:region:instance)
            - CN_SQL_DB_USER: Database username
            - CN_SQL_DB_NAME: Database name
            - CN_SQL_PASSWORD_FILE: Path to file containing database password

        Args:
            manager: An instance of Manager class for retrieving secrets. Required
                to fetch the database password when the password file is absent.

        Returns:
            A callable that creates database connections via Cloud SQL Connector.

        Raises:
            RuntimeError: If CN_SQL_CLOUDSQL_INSTANCE_CONNECTION_NAME is not set.
            ValueError: If the adapter's cloudsql_driver attribute is not defined.
        """
        self._ensure_connector()
        instance_connection_name = self._get_instance_connection_name()

        if not self.cloudsql_driver:
            raise ValueError(
                f"{self.__class__.__name__} must define a non-empty 'cloudsql_driver' "
                "class attribute (e.g., 'pg8000' for PostgreSQL, 'pymysql' for MySQL)."
            )

        db_user = os.environ.get("CN_SQL_DB_USER", "jans")
        db_name = os.environ.get("CN_SQL_DB_NAME", "jans")
        driver = self.cloudsql_driver
        connector = self._cloudsql_connector

        def getconn() -> _t.Any:
            """Create a connection to Cloud SQL using Private IP.

            The password is fetched fresh on each call to support runtime
            password rotation.

            Returns:
                A database connection object.
            """
            conn = connector.connect(
                instance_connection_name,
                driver,
                user=db_user,
                password=get_sql_password(manager),
                db=db_name,
                ip_type=IPTypes.PRIVATE,
            )
            return conn

        return getconn

    def close(self) -> None:
        """Close the Cloud SQL Connector and release resources.

        This method should be called when the adapter is no longer needed to
        properly release the token-refresh thread and other resources used by
        the Cloud SQL Python Connector.
        """
        if self._cloudsql_connector is not None:
            try:
                self._cloudsql_connector.close()
                logger.debug("Cloud SQL Connector closed successfully")
            except Exception as exc:
                logger.warning(f"Error closing Cloud SQL Connector: {exc}")
            finally:
                self._cloudsql_connector = None


class PostgresqlAdapter(CloudSqlConnectorMixin):
    """Class for PostgreSQL adapter."""

    #: Dialect name
    dialect = "pgsql"

    #: Connector name.
    connector = "postgresql+psycopg2"

    #: Character used for quoting identifier.
    quote_char = '"'

    #: Query to display server version
    server_version_query = "SHOW server_version"

    #: Driver for Cloud SQL Connector
    cloudsql_driver = "pg8000"

    #: Fallback driver name for warning messages
    fallback_driver_name = "psycopg2"

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

    @property
    def connect_args(self) -> dict[str, _t.Any]:
        """Get connection arguments for SQLAlchemy.

        Returns:
            Dictionary of connection arguments. When Cloud SQL Connector is enabled,
            returns empty dict as connection is handled by the connector.
        """
        if self.cloudsql_connector_enabled:
            return {}

        opts: dict[str, _t.Any] = {}

        if as_boolean(os.environ.get("CN_SQL_SSL_ENABLED", "false")):
            opts["sslmode"] = os.environ.get("CN_SQL_SSL_MODE", "require")
            if opts["sslmode"] in ("verify-ca", "verify-full"):
                opts["sslrootcert"] = os.environ.get("CN_SQL_SSL_CACERT_FILE", "/etc/certs/sql_cacert.pem")
                opts["sslcert"] = os.environ.get("CN_SQL_SSL_CERT_FILE", "/etc/certs/sql_client_cert.pem")
                opts["sslkey"] = os.environ.get("CN_SQL_SSL_KEY_FILE", "/etc/certs/sql_client_key.pem")
        return opts

    def upsert_query(self, table: Table, column_mapping: dict[str, _t.Any], update_mapping: dict[str, _t.Any]) -> _t.Any:
        return postgres_insert(table).values(column_mapping).on_conflict_do_update(
            index_elements=[table.c.doc_id],
            set_=update_mapping,
        )


class MysqlAdapter(CloudSqlConnectorMixin):
    """Class for MySQL adapter."""

    #: Dialect name.
    dialect = "mysql"

    #: Connector name.
    connector = "mysql+pymysql"

    #: Character used for quoting identifier.
    quote_char = "`"

    #: Query to display server version
    server_version_query = "SELECT VERSION()"

    #: Driver for Cloud SQL Connector
    cloudsql_driver = "pymysql"

    #: Fallback driver name for warning messages
    fallback_driver_name = "pymysql"

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

    @property
    def connect_args(self) -> dict[str, _t.Any]:
        """Get connection arguments for SQLAlchemy.

        Returns:
            Dictionary of connection arguments. When Cloud SQL Connector is enabled,
            returns empty dict as connection is handled by the connector.
        """
        if self.cloudsql_connector_enabled:
            return {}

        opts: dict[str, _t.Any] = {}

        if as_boolean(os.environ.get("CN_SQL_SSL_ENABLED", "false")):
            opts["ssl"] = {}
            ssl_mode = os.environ.get("CN_SQL_SSL_MODE", "REQUIRED")
            if ssl_mode in ("VERIFY_CA", "VERIFY_IDENTITY"):
                opts["ssl"]["ca"] = os.environ.get("CN_SQL_SSL_CACERT_FILE", "/etc/certs/sql_cacert.pem")
                opts["ssl"]["cert"] = os.environ.get("CN_SQL_SSL_CERT_FILE", "/etc/certs/sql_client_cert.pem")
                opts["ssl"]["key"] = os.environ.get("CN_SQL_SSL_KEY_FILE", "/etc/certs/sql_client_key.pem")
            elif ssl_mode == "REQUIRED":
                opts["ssl"]["check_hostname"] = False
        return opts

    def upsert_query(self, table: Table, column_mapping: dict[str, _t.Any], update_mapping: dict[str, _t.Any]) -> _t.Any:
        return mysql_insert(table).values(column_mapping).on_duplicate_key_update(update_mapping)


def doc_id_from_dn(dn: str) -> str:
    """Determine document ID based on LDAP's DN.

    Args:
        dn: LDAP's DN string.
    """
    parsed_dn = dnutils.parse_dn(dn)
    doc_id: str = parsed_dn[0][1]

    if doc_id == "jans":
        doc_id = "_"
    return doc_id  # noqa: R504


class SqlSchemaMixin:
    """Mixin class to deal with SQL schema."""

    @property
    def schema_files(self) -> list[str]:
        """Get list of schema files."""
        parent_dir = "/app/schema"

        default_files = [
            os.path.join(parent_dir, "jans_schema.json"),
            os.path.join(parent_dir, "custom_schema.json"),
        ]

        # collect all files that specified as schema
        files = [
            str(fn.resolve())
            for fn in Path(parent_dir).glob("*_schema.json")
        ]

        # fallback to default files if collected files are empty
        return files or default_files

    @cached_property
    def sql_data_types(self) -> dict[str, dict[str, _t.Any]]:
        """Get list of data types from pre-defined file."""
        data_types = {}

        # collect sql_types embedded in schema files (only if it doesn't exist in data_types)
        for attr_type in self.attr_types:
            names = attr_type.get("names") or []

            # skip if it doesn't have names
            if not names:
                continue

            # the first value in names is the actual name (don't need to handle IndexError
            # as empty list is skipped in previous block)
            name = names[0]
            if name and "sql_types" in attr_type and name not in data_types:
                data_types[name] = attr_type["sql_types"]

        with open("/app/static/rdbm/sql_data_types.json") as f:
            data_types.update(json.loads(f.read()))

        custom_types_fn = os.environ.get("CN_SQL_CUSTOM_TYPES_FILE", "/etc/jans/conf/sql_data_types.custom.json")
        if os.path.isfile(custom_types_fn):
            with open(custom_types_fn) as f:
                data_types.update(json.loads(f.read()))
        return data_types  # type: ignore

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

    @cached_property
    def sql_json_types(self):
        json_types = {}
        for attr_type in self.attr_types:
            for attr in attr_type["names"]:
                if not attr_type.get("rdbm_json_column"):
                    continue
                json_types[attr] = {
                    "mysql": {"type": "JSON"},
                    "pgsql": {"type": "JSONB"},
                }
        return json_types

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


_sql_client_instances: weakref.WeakSet[SqlClient] = weakref.WeakSet()


def _cleanup_sql_clients() -> None:
    """Cleanup all SqlClient instances on interpreter shutdown.

    This function is registered with atexit to ensure proper cleanup of
    Cloud SQL Connectors and SQLAlchemy engines when the process exits.
    """
    for client in list(_sql_client_instances):
        try:
            client.close()
        except Exception as exc:
            logger.debug(f"Error during atexit cleanup of SqlClient: {exc}")


atexit.register(_cleanup_sql_clients)


class SqlClient(SqlSchemaMixin):
    """This class interacts with SQL database.

    This class can be used as a context manager to ensure proper cleanup of
    resources (Cloud SQL Connector and SQLAlchemy engine) when done.

    Args:
        manager: An instance of manager class.
        *args: Positional arguments.
        **kwargs: Keyword arguments.

    Example:
        >>> with SqlClient(manager) as client:
        ...     if client.connected():
        ...         print("Connected to database")
        # Resources are automatically cleaned up when exiting the context
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
        self._engine = None
        self._closed = False

        if as_boolean(os.environ.get("CN_SQL_SSL_ENABLED", "false")):
            self._bootstrap_ssl_assets()

        _sql_client_instances.add(self)

    def _bootstrap_ssl_assets(self):
        for filepath, secret_name in [
            (os.environ.get("CN_SQL_SSL_CACERT_FILE", "/etc/certs/sql_cacert.pem"), "sql_ssl_ca_cert"),
            (os.environ.get("CN_SQL_SSL_CERT_FILE", "/etc/certs/sql_client_cert.pem"), "sql_ssl_client_cert"),
            (os.environ.get("CN_SQL_SSL_KEY_FILE", "/etc/certs/sql_client_key.pem"), "sql_ssl_client_key"),
        ]:
            if os.path.isfile(filepath):
                # asset already exists -- either mounted externally or pre-populated internally
                continue

            if filepath and (contents := self.manager.secret.get(secret_name)):
                logger.info(f"Detected non-empty {secret_name=}. The secret will be populated into {filepath!r}.")

                with open(filepath, "w") as f:
                    f.write(contents)

                # client key must be protected using 600 permission
                if secret_name == "sql_ssl_client_key":  # noqa: B105
                    os.chmod(filepath, 0o600)

    @property
    def engine(self) -> Engine:
        """Lazy init of engine instance object.

        When CN_SQL_CLOUDSQL_CONNECTOR_ENABLED is set to 'true', the engine uses
        Cloud SQL Python Connector with:
        - LAZY refresh strategy for token refresh
        - PRIVATE IP for connecting to Cloud SQL instance
        - pg8000 driver for PostgreSQL, pymysql for MySQL

        Otherwise, uses the standard connection method with psycopg2/pymysql.
        """
        if not self._engine:
            if self._use_cloudsql_connector():
                logger.info("Using Cloud SQL Python Connector with LAZY refresh strategy and Private IP")
                if isinstance(self.adapter, PostgresqlAdapter):
                    self._engine = create_engine(
                        "postgresql+pg8000://",
                        creator=self.adapter.get_cloudsql_connection_creator(self.manager),
                        pool_pre_ping=True,
                        hide_parameters=True,
                    )
                else:
                    self._engine = create_engine(
                        "mysql+pymysql://",
                        creator=self.adapter.get_cloudsql_connection_creator(self.manager),
                        pool_pre_ping=True,
                        hide_parameters=True,
                    )
            else:
                self._engine = create_engine(
                    self.engine_url,
                    pool_pre_ping=True,
                    hide_parameters=True,
                    connect_args=self.adapter.connect_args,
                )

            if self.dialect == "mysql":
                event.listen(self._engine, "first_connect", set_mysql_strict_mode)
                event.listen(self._engine, "first_connect", preconfigure_simple_json)
            else:
                event.listen(self._engine, "connect", set_postgres_search_path)

        # initialized engine
        return self._engine

    def _use_cloudsql_connector(self) -> bool:
        """Check if Cloud SQL Connector should be used.

        Returns:
            True if the adapter has Cloud SQL Connector enabled.
        """
        return getattr(self.adapter, "cloudsql_connector_enabled", False)

    def close(self) -> None:
        """Close the SQL client and release all resources.

        This method disposes the SQLAlchemy engine (releasing connection pool)
        and closes the Cloud SQL Connector (stopping token-refresh threads).
        After calling this method, the client should not be used.

        This method is idempotent - calling it multiple times is safe.
        """
        if self._closed:
            return

        self._closed = True

        try:
            _sql_client_instances.discard(self)
        except Exception as exc:
            logger.debug(f"Error removing SqlClient from instance tracker: {exc}")

        if self._engine is not None:
            try:
                self._engine.dispose()
                logger.debug("SQLAlchemy engine disposed successfully")
            except Exception as exc:
                logger.warning(f"Error disposing SQLAlchemy engine: {exc}")
            finally:
                self._engine = None

        if hasattr(self.adapter, "close"):
            self.adapter.close()

        self._metadata = None

    def __enter__(self) -> "SqlClient":
        """Enter the context manager.

        Returns:
            The SqlClient instance.
        """
        return self

    def __exit__(
        self,
        exc_type: _t.Optional[type[BaseException]],
        exc_val: _t.Optional[BaseException],
        exc_tb: _t.Optional[_t.Any],
    ) -> None:
        """Exit the context manager and cleanup resources.

        Args:
            exc_type: Exception type if an exception was raised.
            exc_val: Exception value if an exception was raised.
            exc_tb: Exception traceback if an exception was raised.
        """
        self.close()

    @property
    def engine_url(self) -> URL:
        """Engine connection URL.

        Note: When Cloud SQL Connector is enabled, this URL is not used.
        The connection is handled by the connector's creator function instead.
        """
        return URL.create(
            drivername=self.adapter.connector,
            username=os.environ.get("CN_SQL_DB_USER", "jans"),
            password=get_sql_password(self.manager),
            host=os.environ.get("CN_SQL_DB_HOST", "localhost"),
            port=int(os.environ.get("CN_SQL_DB_PORT", "3306")),
            database=os.environ.get("CN_SQL_DB_NAME", "jans"),
            query={},
        )

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
                self._metadata = MetaData()
                self._metadata.reflect(self.engine)
        return self._metadata

    def connected(self) -> bool:
        """Check whether connection is alive by executing simple query."""
        with self.engine.connect() as conn:
            result = conn.execute(text("SELECT 1 AS is_alive"))
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
            with conn.begin():
                try:
                    conn.execute(text(query))
                    # refresh metadata as we have newly created table
                    self.metadata.reflect(conn)
                except DatabaseError as exc:
                    self.adapter.on_create_table_error(exc)

    def get_table_mapping(self) -> dict[str, dict[str, str]]:
        """Get mapping of column name and type from all tables."""
        table_mapping: dict[str, dict[str, str]] = defaultdict(dict)

        for table_name, table in self.metadata.tables.items():
            for column in table.c:
                collation = getattr(column.type, "collation", None)

                try:
                    # temporarily truncate COLLATION string (if needed)
                    if collation:
                        column.type.collation = None

                    col_type = str(column.type)

                    # extract the size of DATETIME to match the schema from file
                    if col_type.startswith("DATETIME") and getattr(column.type, "fsp", None):
                        col_type = f"DATETIME({column.type.fsp})"

                    table_mapping[table_name][column.name] = col_type

                finally:
                    # preserve the original collation
                    if collation and column.type.collation is None:
                        column.type.collation = collation

        # finalized table mapping
        return dict(table_mapping)

    def create_index(self, query: str) -> None:
        """Create index using raw query."""
        with self.engine.connect() as conn:
            with conn.begin():
                try:
                    conn.execute(text(query))
                except DatabaseError as exc:
                    self.adapter.on_create_index_error(exc)

    def quoted_id(self, identifier: str) -> str:
        """Get quoted identifier name."""
        return f"{self.adapter.quote_char}{identifier}{self.adapter.quote_char}"

    def row_exists(self, table_name: str, id_: str) -> bool:
        """Check whether a row is exist."""
        table = self.metadata.tables.get(table_name)
        if table is None:
            return False

        query = select(func.count()).select_from(table).where(
            table.c.doc_id == id_
        )
        with self.engine.connect() as conn:
            result = conn.execute(query)
            return bool(result.fetchone()[0] > 0)

    def insert_into(self, table_name: str, column_mapping: dict[str, _t.Any]) -> None:
        """Insert a row into a table."""
        table = self.metadata.tables.get(table_name)
        column_mapping = self._apply_json_defaults(table, column_mapping)

        query = table.insert().values(column_mapping)
        with self.engine.connect() as conn:
            with conn.begin():
                try:
                    conn.execute(query)
                except DatabaseError as exc:
                    self.adapter.on_insert_into_error(exc)

    def get(self, table_name: str, id_: str, column_names: _t.Union[list[str], None] = None) -> dict[str, _t.Any]:
        """Get a row from a table with matching ID."""
        table = self.metadata.tables.get(table_name)

        attrs = column_names or []
        if attrs:
            _select = select(*[table.c[attr] for attr in attrs])
        else:
            _select = select(table)

        query = _select.where(table.c.doc_id == id_)
        with self.engine.connect() as conn:
            result = conn.execute(query)
            entry = result.fetchone()

        if not entry:
            return {}
        return dict(entry._mapping)

    def update(self, table_name: str, id_: str, column_mapping: dict[str, _t.Any]) -> bool:
        """Update a table row with matching ID."""
        table = self.metadata.tables.get(table_name)

        query = table.update().where(table.c.doc_id == id_).values(column_mapping)
        with self.engine.connect() as conn:
            with conn.begin():
                result = conn.execute(query)
        return bool(result.rowcount)

    def search(self, table_name: str, column_names: _t.Union[list[str], None] = None) -> _t.Iterator[dict[str, _t.Any]]:
        """Get all rows from a table."""
        table = self.metadata.tables.get(table_name)

        attrs = column_names or []
        if attrs:
            query = select(*[table.c[attr] for attr in attrs])
        else:
            query = select(table)

        with self.engine.connect() as conn:
            result = conn.execute(query)
            for entry in result:
                yield dict(entry._mapping)

    @property
    def server_version(self) -> str:
        """Display server version."""
        with self.engine.connect() as conn:
            version: str = conn.scalar(text(self.adapter.server_version_query))
            return version

    def _transform_value(self, key: str, values: _t.Any) -> _t.Any:
        """Transform value from one to another based on its data type.

        Args:
            key: Attribute name.
            values: Pre-transformed values.
        """
        type_ = self.sql_data_types.get(key, {})

        if not type_:
            type_ = self.sql_json_types.get(key, {})

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
            if not self.use_simple_json:
                return {"v": values}
            return values

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

    def create_from_ldif(
        self,
        filepath: str,
        ctx: dict[str, _t.Any],
        transform_column_mapping: None | Callable[[str, dict], dict] = None,
    ) -> None:
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
                if callable(transform_column_mapping):
                    column_mapping = transform_column_mapping(table_name, column_mapping)
                self.insert_into(table_name, column_mapping)

    def get_server_version(self) -> tuple[int, ...]:
        """Get server version as tuple."""
        # major.minor.patch format
        version = [0, 0, 0]

        pattern = SERVER_VERSION_RE.search(self.server_version)
        if pattern:
            version = [int(comp) for comp in pattern.group().split(".")]
        return tuple(version)

    def delete(self, table_name: str, id_: str) -> bool:
        """Delete a row from a table with matching ID."""
        table = self.metadata.tables.get(table_name)

        query = delete(table).where(table.c.doc_id == id_)
        with self.engine.connect() as conn:
            with conn.begin():
                result = conn.execute(query)
                return bool(result.rowcount)

    @property
    def use_simple_json(self):
        """Determine whether to use simple JSON where values are stored as JSON array."""
        if self.dialect in ("pgsql", "postgresql",):
            return True

        if "MYSQL_SIMPLE_JSON" in os.environ:
            return as_boolean(os.environ["MYSQL_SIMPLE_JSON"])

        sql_overrides = load_sql_overrides()
        return as_boolean(sql_overrides.get("MYSQL_SIMPLE_JSON", "true"))

    def upsert(self, table_name: str, column_mapping: dict[str, _t.Any]) -> None:
        table = self.metadata.tables.get(table_name)

        # doc_id might be required for inserting new entry
        if "doc_id" not in column_mapping:
            raise ValueError(f"doc_id is required in column_mapping for upsert operation on {table_name}")

        # column mapping for update (doc_id is excluded)
        update_mapping = {
            k: v for k, v in column_mapping.items()
            if k != "doc_id"
        }

        # apply defaults for column mapping used for inserting entry
        column_mapping = self._apply_json_defaults(table, column_mapping)

        with self.engine.connect() as conn:
            with conn.begin():
                query = self.adapter.upsert_query(table, column_mapping, update_mapping)
                conn.execute(query)

    def upsert_from_file(
        self,
        filepath: str,
        ctx: dict[str, _t.Any],
        transform_column_mapping: None | Callable[[str, dict], dict] = None,
    ) -> None:
        """Upsert entry with data loaded from a template file.

        Args:
            filepath: Path to LDIF template file.
            ctx: Key-value pairs of context that rendered into LDIF template file.
        """
        supported_tables = self.get_table_mapping().keys()

        with open(filepath) as src, NamedTemporaryFile("w+") as dst:
            dst.write(safe_render(src.read(), ctx))
            # ensure rendered template is written
            dst.flush()

            for table_name, column_mapping in self._data_from_ldif(dst.name):
                if table_name not in supported_tables:
                    continue

                if callable(transform_column_mapping):
                    column_mapping = transform_column_mapping(table_name, column_mapping)

                # create or update entry
                self.upsert(table_name, column_mapping)

    def _apply_json_defaults(self, table: Table, column_mapping: dict[str, _t.Any]) -> dict[str, _t.Any]:
        for column in table.c:
            unmapped = column.name not in column_mapping
            json_default_values: list[_t.Any] | dict[str, _t.Any] = []

            if self.dialect == "mysql":
                # probably using legacy data structure
                if not self.use_simple_json:
                    json_default_values = {"v": []}
                json_type = "json"
            else:
                json_type = "jsonb"

            is_json = bool(column.type.__class__.__name__.lower() == json_type)

            if not all([unmapped, is_json]):
                continue
            column_mapping[column.name] = json_default_values

        # finalized column mapping
        return column_mapping


def _build_jdbc_connection_uri(db_dialect: str, db_name: str, server_time_zone: str) -> str:
    """Build the JDBC connection URI based on the database dialect and Cloud SQL connector settings.

    Args:
        db_dialect: The database dialect ('mysql' or 'pgsql').
        db_name: The database name.
        server_time_zone: The server timezone.

    Returns:
        The JDBC connection URI string.

    Raises:
        RuntimeError: If Cloud SQL connector is enabled but instance connection name is not set.
    """
    cloudsql_connector_enabled = as_boolean(os.environ.get("CN_SQL_CLOUDSQL_CONNECTOR_ENABLED", "false"))

    if cloudsql_connector_enabled:
        instance_connection_name = os.environ.get("CN_SQL_CLOUDSQL_INSTANCE_CONNECTION_NAME", "").strip()
        if not instance_connection_name:
            raise RuntimeError(
                "Cloud SQL connector is enabled (CN_SQL_CLOUDSQL_CONNECTOR_ENABLED=true) but "
                "CN_SQL_CLOUDSQL_INSTANCE_CONNECTION_NAME is not set. "
                "Set it to your Cloud SQL instance connection name (project:region:instance)."
            )

        if db_dialect == "mysql":
            return (
                f"jdbc:mysql:///{db_name}"
                f"?cloudSqlInstance={instance_connection_name}"
                f"&socketFactory=com.google.cloud.sql.mysql.SocketFactory"
                f"&serverTimezone={server_time_zone}"
            )
        else:  # pgsql
            return (
                f"jdbc:postgresql:///{db_name}"
                f"?cloudSqlInstance={instance_connection_name}"
                f"&socketFactory=com.google.cloud.sql.postgres.SocketFactory"
            )
    else:
        db_host = os.environ.get("CN_SQL_DB_HOST", "localhost")
        db_port = os.environ.get("CN_SQL_DB_PORT", 3306)

        if db_dialect == "mysql":
            return f"jdbc:mysql://{db_host}:{db_port}/{db_name}?enabledTLSProtocols=TLSv1.2"
        else:  # pgsql
            return f"jdbc:postgresql://{db_host}:{db_port}/{db_name}"


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
        server_time_zone = os.environ.get("CN_SQL_DB_TIMEZONE", "UTC")
        db_schema = resolve_db_schema_name()

        rendered_txt = txt % {
            "rdbm_db": db_name,
            "rdbm_schema": db_schema,
            "rdbm_type": "postgresql" if db_dialect == "pgsql" else "mysql",
            "rdbm_host": os.environ.get("CN_SQL_DB_HOST", "localhost"),
            "rdbm_port": os.environ.get("CN_SQL_DB_PORT", 3306),
            "rdbm_connection_uri": _build_jdbc_connection_uri(db_dialect, db_name, server_time_zone),
            "rdbm_user": os.environ.get("CN_SQL_DB_USER", "jans"),
            "rdbm_password_enc": encode_text(
                get_sql_password(manager),
                manager.secret.get("encoded_salt"),
            ).decode(),
            "server_time_zone": server_time_zone,
        }
        f.write(rendered_txt)

    # Set restrictive permissions to protect embedded credentials (if any)
    os.chmod(dest, 0o600)


def set_mysql_strict_mode(dbapi_connection, connection_record):
    cursor = dbapi_connection.cursor()
    cursor.execute("SET SESSION sql_mode = 'TRADITIONAL'")
    cursor.close()


def get_sql_password_file():
    return os.environ.get("CN_SQL_PASSWORD_FILE", "/etc/jans/conf/sql_password")


def sync_sql_password(manager: Manager) -> None:
    """Pull secret contains password to access RDBM server.

    Args:
        manager: An instance of manager class.
    """
    logger.warning(
        "Accessing jans.pycloudlib.persistence.sql.sync_sql_password is deprecated; "
        "Use jans.pycloudlib.persistence.sql.get_sql_password instead"
    )


def get_sql_password(manager: Manager | None = None) -> str:
    """Get the SQL database password.

    The password is retrieved from the password file if it exists,
    otherwise it is fetched from the Manager's secret store.

    Args:
        manager: An instance of Manager class for retrieving secrets.
            Required when the password file does not exist.

    Returns:
        The database password string.

    Raises:
        RuntimeError: If the password file does not exist and manager is None.
    """
    password_file = get_sql_password_file()
    if os.path.isfile(password_file):
        return get_password_from_file(password_file)

    if manager is None:
        raise RuntimeError(
            f"SQL password file '{password_file}' does not exist and no Manager instance "
            "was provided. Either create the password file or pass a Manager instance "
            "to retrieve the password from the secret store."
        )

    return manager.secret.get("sql_password")


def preconfigure_simple_json(dbapi_connection, connection_record):
    # if env var is available, skip the remaining process
    if "MYSQL_SIMPLE_JSON" in os.environ:
        return

    # preconfigure MYSQL_SIMPLE_JSON overrides (if necessary)
    data = load_sql_overrides()

    if "MYSQL_SIMPLE_JSON" in data:
        return

    logger.info("Preconfiguring MYSQL_SIMPLE_JSON")
    is_simple_json = True
    cursor = dbapi_connection.cursor()

    try:
        # extract the value of `jansAppConf.jansSmtpConf` of jans-auth to check the data format,
        # whether it's using legacy `{"v": []}` or new `[]` format;
        #
        # note that we choose `jansAppConf.jansSmtpConf` column for several reasons:
        #
        # - it's one of few JSON columns available in the table alongside `jansDbAuth` (deprecated) and `jansEmail`
        # - the column is likely will not be removed/changed
        cursor.execute("SELECT jansSmtpConf FROM jansAppConf WHERE doc_id='jans-auth';")

        if result := cursor.fetchone():
            for value in result:
                if isinstance(json.loads(value), dict):
                    is_simple_json = False
                break

    except ProgrammingError as exc:
        # missing table or column will raise ProgrammingError
        logger.warning(f"Unable to detect JSON data format automatically; reason={exc.args[1]}; fallback to default value")

    finally:
        # cleanup resource
        cursor.close()

    env_value = str(is_simple_json).lower()
    data["MYSQL_SIMPLE_JSON"] = env_value
    dump_sql_overrides(data)


def override_simple_json_property(sql_prop_file):
    # skip the override if env var is available
    if "MYSQL_SIMPLE_JSON" in os.environ:
        return

    data = load_sql_overrides()

    if "MYSQL_SIMPLE_JSON" in data and os.path.isfile(sql_prop_file):
        with open(sql_prop_file) as f:
            txt = SIMPLE_JSON_RE.sub(fr"\1\2{data['MYSQL_SIMPLE_JSON']}", f.read())

        with open(sql_prop_file, "w") as f:
            f.write(txt)


def load_sql_overrides():
    if os.path.isfile(SQL_OVERRIDES_FILE):
        with open(SQL_OVERRIDES_FILE) as f:
            return json.loads(f.read())
    return {}


def dump_sql_overrides(data):
    with open(SQL_OVERRIDES_FILE, "w") as f:
        f.write(json.dumps(data))


def override_sql_ssl_property(sql_prop_file):
    # The connector handles SSL/TLS encryption automatically
    cloudsql_connector_enabled = as_boolean(os.environ.get("CN_SQL_CLOUDSQL_CONNECTOR_ENABLED", "false"))
    if cloudsql_connector_enabled:
        if as_boolean(os.environ.get("CN_SQL_SSL_ENABLED", "false")):
            logger.warning(
                "Both CN_SQL_CLOUDSQL_CONNECTOR_ENABLED and CN_SQL_SSL_ENABLED are set to true. "
                "SSL properties will be skipped as Cloud SQL Connector handles encryption automatically."
            )
        return

    with open(sql_prop_file) as f:
        props = javaproperties.loads(f.read())

    if os.environ.get("CN_SQL_DB_DIALECT") in ("pgsql", "postgresql"):
        # boolean need to be defined as lowercase value
        props["connection.driver-property.ssl"] = str(as_boolean(os.environ.get("CN_SQL_SSL_ENABLED", "false"))).lower()
        props["connection.driver-property.sslmode"] = os.environ.get("CN_SQL_SSL_MODE", "require")

        if props["connection.driver-property.sslmode"] in ("verify-ca", "verify-full"):
            props["connection.driver-property.sslrootcert"] = os.environ.get("CN_SQL_SSL_CACERT_FILE", "/etc/certs/sql_cacert.pem")
            props["connection.driver-property.sslcert"] = os.environ.get("CN_SQL_SSL_CERT_FILE", "/etc/certs/sql_client_cert.pem")

            # client key need to be converted from PEM to DER format
            ssl_key = os.environ.get("CN_SQL_SSL_KEY_FILE", "/etc/certs/sql_client_key.pem")
            ssl_key_p8 = os.environ.get("CN_SQL_SSL_KEY_P8_FILE", "/etc/certs/sql_client_key.pkcs8")
            out, err, code = exec_cmd(f"openssl pkcs8 -topk8 -inform PEM -outform DER -in {ssl_key} -out {ssl_key_p8} -nocrypt")

            if code != 0:
                # error may not recorded in stderr but available in stdout
                err = err or out
                logger.warning(f"Unable to convert key file {ssl_key} to PKCS8 file {ssl_key_p8}; reason={err.decode()}")
            props["connection.driver-property.sslkey"] = ssl_key_p8

    # mysql dialect
    else:
        props["connection.driver-property.sslMode"] = os.environ.get("CN_SQL_SSL_MODE", "REQUIRED")
        if props["connection.driver-property.sslMode"] in ("VERIFY_CA", "VERIFY_IDENTITY"):
            # create truststore for CA cert
            ssl_ca = os.environ.get("CN_SQL_SSL_CACERT_FILE", "/etc/certs/sql_cacert.pem")
            ssl_ca_p12 = os.environ.get("CN_SQL_SSL_CACERT_P12_FILE", "/etc/certs/sql_cacert.pkcs12")
            out, err, code = exec_cmd(f"keytool -importcert -alias mysql-cacert -file {ssl_ca} -keystore {ssl_ca_p12} -storepass changeit -noprompt")

            if code != 0:
                # error may not recorded in stderr but available in stdout
                err = err or out
                logger.warning(f"Unable to convert CA cert file {ssl_ca} to PKCS12 file {ssl_ca_p12}; reason={err.decode()}")

            props["connection.driver-property.trustCertificateKeyStoreUrl"] = f"file://{ssl_ca_p12}"
            props["connection.driver-property.trustCertificateKeyStorePassword"] = "changeit"

            # create truststore for client cert and key
            ssl_key = os.environ.get("CN_SQL_SSL_KEY_FILE", "/etc/certs/sql_client_key.pem")
            ssl_cert = os.environ.get("CN_SQL_SSL_CERT_FILE", "/etc/certs/sql_client_cert.pem")
            ssl_certkey_p12 = os.environ.get("CN_SQL_SSL_CERTKEY_P12_FILE", "/etc/certs/sql_client_certkey.pkcs12")
            out, err, code = exec_cmd(f"openssl pkcs12 -export -in {ssl_cert} -inkey {ssl_key} -name mysql-client -passout pass:changeit -out {ssl_certkey_p12}")

            if code != 0:
                # error may not recorded in stderr but available in stdout
                err = err or out
                logger.warning(f"Unable to convert cert file {ssl_cert} and key file {ssl_key} to PKCS12 file {ssl_certkey_p12}; reason={err.decode()}")

            props["connection.driver-property.clientCertificateKeyStoreUrl"] = f"file://{ssl_certkey_p12}"
            props["connection.driver-property.clientCertificateKeyStorePassword"] = "changeit"

    with open(sql_prop_file, "w") as f:
        f.write(javaproperties.dumps(props, timestamp=None))


def resolve_db_schema_name() -> str:
    """Resolve database schema name based on dialect and environment.

    For MySQL, schema is synonymous with database name.
    For PostgreSQL, defaults to 'public' unless overridden.

    Returns:
        Schema name to use.
    """
    db_dialect = os.environ.get("CN_SQL_DB_DIALECT", "mysql")
    db_name = os.environ.get("CN_SQL_DB_NAME", "jans")

    # In MySQL, physically, a schema is synonymous with a database
    if db_dialect == "mysql":
        default_schema = db_name
    else:  # likely postgres
        # by default, PostgreSQL creates schema called `public` upon database creation
        default_schema = "public"
    return os.environ.get("CN_SQL_DB_SCHEMA", "") or default_schema


def set_postgres_search_path(dbapi_connection: _t.Any, connection_record: _t.Any) -> None:
    """Set PostgreSQL search_path to the resolved schema on new connections.

    Args:
        dbapi_connection: Raw DBAPI connection.
        connection_record: SQLAlchemy connection record (unused but required by event signature).
    """
    db_schema = resolve_db_schema_name()

    # use the .autocommit DBAPI attribute so that when the SET search_path directive is invoked,
    # it is invoked outside of the scope of any transaction and therefore will not be reverted when
    # the DBAPI connection has a rollback.
    existing_autocommit = dbapi_connection.autocommit

    try:
        dbapi_connection.autocommit = True
        cursor = dbapi_connection.cursor()
        try:
            cursor.execute("SET search_path = %s", [db_schema])
        finally:
            cursor.close()
    finally:
        dbapi_connection.autocommit = existing_autocommit
