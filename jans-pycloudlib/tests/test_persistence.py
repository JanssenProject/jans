import json
from collections import namedtuple
from io import StringIO

import pytest


def test_render_salt(tmpdir, gmanager, monkeypatch):
    from jans.pycloudlib.persistence import render_salt

    src = tmpdir.join("salt.tmpl")
    src.write("encodeSalt = %(encode_salt)s")

    dest = tmpdir.join("salt")
    render_salt(gmanager, str(src), str(dest))
    assert dest.read() == f"encodeSalt = {gmanager.secret.get('encoded_salt')}"


def test_render_base_properties(monkeypatch, tmpdir):
    from jans.pycloudlib.persistence import render_base_properties

    persistence_type = "sql"
    monkeypatch.setenv("CN_PERSISTENCE_TYPE", persistence_type)

    src = tmpdir.join("jans.properties.tmpl")
    src.write("""
persistence.type=%(persistence_type)s
certsDir=%(certFolder)s
pythonModulesDir=%(jansOptPythonFolder)s/libs
""".strip())
    dest = tmpdir.join("jans.properties")

    expected = f"""
persistence.type={persistence_type}
certsDir=/etc/certs
pythonModulesDir=/opt/jans/python/libs:/opt/jython/Lib/site-packages
""".strip()

    render_base_properties(str(src), str(dest))
    assert dest.read() == expected


# ======
# Hybrid
# ======


def test_resolve_hybrid_storages(monkeypatch):
    from jans.pycloudlib.persistence.hybrid import resolve_hybrid_storages
    from jans.pycloudlib.persistence.utils import PersistenceMapper

    monkeypatch.setenv("CN_PERSISTENCE_TYPE", "hybrid")
    monkeypatch.setenv("CN_HYBRID_MAPPING", json.dumps({
        "default": "sql",
        "user": "sql",
        "site": "sql",
        "cache": "sql",
        "token": "sql",
        "session": "sql",
    }))
    expected = {
        "storages": "sql",
        "storage.default": "sql",
        "storage.sql.mapping": "people, groups, authorizations, link, cache, tokens, sessions",
    }
    mapper = PersistenceMapper()
    assert resolve_hybrid_storages(mapper) == expected


def test_render_hybrid_properties(monkeypatch, tmpdir):
    from jans.pycloudlib.persistence.hybrid import render_hybrid_properties

    monkeypatch.setenv("CN_PERSISTENCE_TYPE", "hybrid")
    monkeypatch.setenv(
        "CN_HYBRID_MAPPING",
        json.dumps({
            "default": "sql",
            "user": "sql",
            "site": "sql",
            "cache": "sql",
            "token": "sql",
            "session": "sql",
        })
    )

    expected = """
storages: sql
storage.default: sql
storage.sql.mapping: people, groups, authorizations, link, cache, tokens, sessions
""".strip()

    dest = tmpdir.join("jans-hybrid.properties")
    render_hybrid_properties(str(dest))
    assert dest.read() == expected


# ===
# SQL
# ===


def test_get_sql_password_from_file(monkeypatch, tmpdir, gmanager):
    from jans.pycloudlib.persistence.sql import get_sql_password

    src = tmpdir.join("sql_password")
    src.write("secret")
    monkeypatch.setenv("CN_SQL_PASSWORD_FILE", str(src))
    assert get_sql_password(gmanager) == "secret"


@pytest.mark.parametrize("dialect, port, schema, jdbc_driver", [
    ("mysql", 3306, "jans", "mysql"),
    ("pgsql", 5432, "public", "postgresql"),
])
def test_render_sql_properties(monkeypatch, tmpdir, gmanager, dialect, port, schema, jdbc_driver):
    import os
    import stat
    from jans.pycloudlib.persistence.sql import render_sql_properties

    passwd = tmpdir.join("sql_password")
    passwd.write("secret")

    monkeypatch.setenv("CN_SQL_PASSWORD_FILE", str(passwd))
    monkeypatch.setenv("CN_SQL_DB_DIALECT", dialect)
    monkeypatch.setenv("CN_SQL_DB_PORT", str(port))

    tmpl = """
db.schema.name=%(rdbm_schema)s
connection.uri=jdbc:%(rdbm_type)s://%(rdbm_host)s:%(rdbm_port)s/%(rdbm_db)s
connection.driver-property.serverTimezone=%(server_time_zone)s
auth.userName=%(rdbm_user)s
auth.userPassword=%(rdbm_password_enc)s
""".strip()

    expected = f"""
db.schema.name={schema}
connection.uri=jdbc:{jdbc_driver}://localhost:{port}/jans
connection.driver-property.serverTimezone=UTC
auth.userName=jans
auth.userPassword=fHL54sT5qHk=
""".strip()

    src = tmpdir.join("jans-sql.properties.tmpl")
    src.write(tmpl)
    dest = tmpdir.join("jans-sql.properties")

    render_sql_properties(gmanager, str(src), str(dest))
    assert dest.read() == expected

    # check file permission (should writable only by owner)
    perms = stat.S_IMODE(os.stat(dest).st_mode)
    assert oct(perms) == '0o600'


class PGException(Exception):
    def __init__(self, code):
        orig_attrs = namedtuple("OrigAttrs", "pgcode")
        self.orig = orig_attrs(code)


def test_postgresql_adapter_on_create_table_error():
    from jans.pycloudlib.persistence.sql import PostgresqlAdapter

    with pytest.raises(Exception):
        exc = PGException("10P01")
        PostgresqlAdapter().on_create_table_error(exc)


def test_postgresql_adapter_on_create_index_error():
    from jans.pycloudlib.persistence.sql import PostgresqlAdapter

    with pytest.raises(Exception):
        exc = PGException("10P01")
        PostgresqlAdapter().on_create_index_error(exc)


def test_postgresql_adapter_on_insert_into_error():
    from jans.pycloudlib.persistence.sql import PostgresqlAdapter

    with pytest.raises(Exception):
        exc = PGException("10P01")
        PostgresqlAdapter().on_insert_into_error(exc)


class MSException(Exception):
    def __init__(self, code):
        orig_attrs = namedtuple("OrigAttrs", "args")
        self.orig = orig_attrs([code])


def test_mysql_adapter_on_create_table_error():
    from jans.pycloudlib.persistence.sql import MysqlAdapter

    with pytest.raises(Exception):
        exc = MSException(1001)
        MysqlAdapter().on_create_table_error(exc)


def test_mysql_adapter_on_create_index_error():
    from jans.pycloudlib.persistence.sql import MysqlAdapter

    with pytest.raises(Exception):
        exc = MSException(1001)
        MysqlAdapter().on_create_index_error(exc)


def test_mysql_adapter_on_insert_into_error():
    from jans.pycloudlib.persistence.sql import MysqlAdapter

    with pytest.raises(Exception):
        exc = MSException(1001)
        MysqlAdapter().on_insert_into_error(exc)


@pytest.mark.parametrize("dn, doc_id", [
    ("o=jans", "_"),
    ("ou=jans-auth,ou=configuration,o=jans", "jans-auth"),
])
def test_doc_id_from_dn(dn, doc_id):
    from jans.pycloudlib.persistence.sql import doc_id_from_dn
    assert doc_id_from_dn(dn) == doc_id


@pytest.mark.parametrize("dialect, word, quoted_word", [
    ("pgsql", "random", '"random"'),
    ("mysql", "random", "`random`"),
])
def test_sql_client_quoted_id(monkeypatch, gmanager, dialect, word, quoted_word):
    from jans.pycloudlib.persistence.sql import SqlClient

    monkeypatch.setenv("CN_SQL_DB_DIALECT", dialect)

    client = SqlClient(gmanager)
    assert client.quoted_id(word) == quoted_word


BUILTINS_OPEN = "builtins.open"


def test_sql_sql_data_types(monkeypatch):
    from jans.pycloudlib.persistence.sql import SqlSchemaMixin

    types_str = '{"dat": {"mysql": {"type": "TEXT"}}}'
    monkeypatch.setattr(BUILTINS_OPEN, lambda p: StringIO(types_str))
    assert SqlSchemaMixin().sql_data_types == json.loads(types_str)


def test_sql_sql_data_types_mapping(monkeypatch):
    from jans.pycloudlib.persistence.sql import SqlSchemaMixin

    types_str = """{
    "1.3.6.1.4.1.1466.115.121.1.11": {
        "mysql": {"size": 2, "type": "VARCHAR"}
    }
}"""

    monkeypatch.setattr(BUILTINS_OPEN, lambda p: StringIO(types_str))
    assert SqlSchemaMixin().sql_data_types_mapping == json.loads(types_str)


def test_sql_attr_types(monkeypatch):
    from jans.pycloudlib.persistence.sql import SqlSchemaMixin

    types_str = """{
    "schemaFile": "101-jans.ldif",
    "attributeTypes": [
        {
            "desc": "Description",
            "names": ["jansAssociatedClnt"]
        }
    ]
}"""
    monkeypatch.setattr(BUILTINS_OPEN, lambda p: StringIO(types_str))

    item = {
        "desc": "Description",
        "names": ["jansAssociatedClnt"],
    }
    assert item in SqlSchemaMixin().attr_types


def test_sql_opendj_attr_types(monkeypatch):
    from jans.pycloudlib.persistence.sql import SqlSchemaMixin

    types_str = '{"ds-task-reset-change-number-base-dn": "1.3.6.1.4.1.1466.115.121.1.12"}'
    monkeypatch.setattr(BUILTINS_OPEN, lambda p: StringIO(types_str))
    assert SqlSchemaMixin().opendj_attr_types == json.loads(types_str)


@pytest.mark.parametrize(("db_dialect", "db_name", "db_schema", "actual_db_schema"), [
    # using default schema names
    ("mysql", "jans", "", "jans"),
    ("pgsql", "jans", "", "public"),
    # using custom schema names
    ("mysql", "jans", "custom", "custom"),
    ("pgsql", "jans", "custom", "custom"),
])
def test_resolve_db_schema_name(monkeypatch, db_dialect, db_name, db_schema, actual_db_schema):
    from jans.pycloudlib.persistence.sql import resolve_db_schema_name

    monkeypatch.setenv("CN_SQL_DB_DIALECT", db_dialect)
    monkeypatch.setenv("CN_SQL_DB_NAME", db_name)
    monkeypatch.setenv("CN_SQL_DB_SCHEMA", db_schema)
    assert resolve_db_schema_name() == actual_db_schema


# =====
# utils
# =====


@pytest.mark.parametrize("type_", [
    "sql",
])
def test_persistence_mapper_mapping(monkeypatch, type_):
    from jans.pycloudlib.persistence import PersistenceMapper

    monkeypatch.setenv("CN_PERSISTENCE_TYPE", type_)
    expected = dict.fromkeys([
        "default",
        "user",
        "site",
        "cache",
        "token",
        "session",
    ], type_)
    assert PersistenceMapper().mapping == expected


def test_persistence_mapper_hybrid_mapping(monkeypatch):
    from jans.pycloudlib.persistence import PersistenceMapper

    mapping = {
        "default": "sql",
        "user": "sql",
        "site": "sql",
        "cache": "sql",
        "token": "sql",
        "session": "sql",
    }
    monkeypatch.setenv("CN_PERSISTENCE_TYPE", "hybrid")
    monkeypatch.setenv("CN_HYBRID_MAPPING", json.dumps(mapping))
    assert PersistenceMapper().mapping == mapping


@pytest.mark.parametrize("mapping", [
    "ab",
    "1",
    "[]",
    "{}",  # empty dict
    {"user": "sql"},  # missing remaining keys
    {"default": "sql", "user": "sql", "cache": "sql", "site": "sql", "token": "sql", "session": "random"},  # invalid type
    {"default": "sql", "user": "sql", "cache": "sql", "site": "sql", "token": "sql", "foo": "sql"},  # invalid key
])
def test_persistence_mapper_validate_hybrid_mapping(monkeypatch, mapping):
    from jans.pycloudlib.persistence.utils import PersistenceMapper

    monkeypatch.setenv("CN_PERSISTENCE_TYPE", "hybrid")
    monkeypatch.setenv("CN_HYBRID_MAPPING", json.dumps(mapping))

    with pytest.raises(ValueError):
        PersistenceMapper().validate_hybrid_mapping()


def test_persistence_mapper_groups(monkeypatch):
    from jans.pycloudlib.persistence.utils import PersistenceMapper

    monkeypatch.setenv("CN_PERSISTENCE_TYPE", "hybrid")
    monkeypatch.setenv("CN_HYBRID_MAPPING", json.dumps({
        "default": "sql",
        "user": "sql",
        "site": "sql",
        "cache": "sql",
        "token": "sql",
        "session": "sql",
    }))

    groups = {
        "sql": ["default", "user", "site", "cache", "token", "session"],
    }
    assert PersistenceMapper().groups() == groups


def test_persistence_mapper_groups_rdn(monkeypatch):
    from jans.pycloudlib.persistence.utils import PersistenceMapper

    monkeypatch.setenv("CN_PERSISTENCE_TYPE", "hybrid")
    monkeypatch.setenv("CN_HYBRID_MAPPING", json.dumps({
        "default": "sql",
        "user": "sql",
        "site": "sql",
        "cache": "sql",
        "token": "sql",
        "session": "sql",
    }))

    groups = {
        "sql": ["", "people, groups, authorizations", "link", "cache", "tokens", "sessions"],
    }
    assert PersistenceMapper().groups_with_rdn() == groups


@pytest.mark.parametrize("given, expected", [
    ("8.0.30", (8, 0, 30)),
    ("5.7.22-standard", (5, 7, 22)),
    ("8.0.25-221000 MySQL Community Server", (8, 0, 25)),
])
def test_get_server_version(gmanager, given, expected):
    from jans.pycloudlib.persistence.sql import SqlClient

    def patched_server_version(cls):
        return given

    SqlClient.server_version = property(patched_server_version)
    client = SqlClient(gmanager)
    assert client.get_server_version() == expected


@pytest.mark.parametrize("dialect, expected", [
    ("pgsql", True),
    ("postgresql", True),
])
def test_simple_json_postgres(monkeypatch, gmanager, dialect, expected):
    from jans.pycloudlib.persistence.sql import SqlClient

    monkeypatch.setenv("CN_SQL_DB_DIALECT", dialect)
    assert SqlClient(gmanager).use_simple_json is expected


@pytest.mark.parametrize("env_value, expected", [
    ("true", True),
    ("false", False),
])
def test_simple_json_mysql(monkeypatch, gmanager, env_value, expected):
    from jans.pycloudlib.persistence.sql import SqlClient

    monkeypatch.setenv("CN_SQL_DB_DIALECT", "mysql")
    monkeypatch.setenv("MYSQL_SIMPLE_JSON", env_value)
    assert SqlClient(gmanager).use_simple_json is expected


@pytest.mark.parametrize("query, expected", [
    ('CREATE INDEX "t_a" ON "t" ("a")', 'CREATE INDEX CONCURRENTLY IF NOT EXISTS "t_a" ON "t" ("a")'),
    ('CREATE UNIQUE INDEX "t_a" ON "t" ("a")', 'CREATE UNIQUE INDEX CONCURRENTLY IF NOT EXISTS "t_a" ON "t" ("a")'),
    # keywords are case-insensitive and may be separated by any whitespace
    ('create index "t_a" on "t" ("a")', 'create index CONCURRENTLY IF NOT EXISTS "t_a" on "t" ("a")'),
    ('Create  Unique\tIndex   "t_a" ON "t" ("a")', 'Create  Unique\tIndex CONCURRENTLY IF NOT EXISTS "t_a" ON "t" ("a")'),
    ('\n  CREATE INDEX "t_a" ON "t" ("a")', 'CREATE INDEX CONCURRENTLY IF NOT EXISTS "t_a" ON "t" ("a")'),
    # modifiers already in the query are not repeated
    ('CREATE INDEX IF NOT EXISTS "t_a" ON "t" ("a")', 'CREATE INDEX CONCURRENTLY IF NOT EXISTS "t_a" ON "t" ("a")'),
    ('CREATE INDEX CONCURRENTLY "t_a" ON "t" ("a")', 'CREATE INDEX CONCURRENTLY IF NOT EXISTS "t_a" ON "t" ("a")'),
    (
        'CREATE UNIQUE INDEX CONCURRENTLY IF NOT EXISTS "t_a" ON "t" ("a")',
        'CREATE UNIQUE INDEX CONCURRENTLY IF NOT EXISTS "t_a" ON "t" ("a")',
    ),
    # not an index creation; left unchanged
    ('DROP INDEX "t_a"', 'DROP INDEX "t_a"'),
])
def test_postgresql_adapter_prepare_index_query(query, expected):
    from jans.pycloudlib.persistence.sql import PostgresqlAdapter

    assert PostgresqlAdapter().prepare_index_query(query) == expected


def test_adapters_is_lock_timeout():
    from jans.pycloudlib.persistence.sql import MysqlAdapter
    from jans.pycloudlib.persistence.sql import PostgresqlAdapter

    assert PostgresqlAdapter().is_lock_timeout(PGException("55P03"))
    assert not PostgresqlAdapter().is_lock_timeout(PGException("42P07"))
    assert MysqlAdapter().is_lock_timeout(MSException(1205))
    assert not MysqlAdapter().is_lock_timeout(MSException(1061))


def test_postgresql_adapter_is_lock_timeout_pg8000():
    from jans.pycloudlib.persistence.sql import PostgresqlAdapter

    # pg8000 has no pgcode attribute; the SQLSTATE is in a dict passed as the first argument
    error = Exception()
    error.orig = namedtuple("Orig", "args")([{"C": "55P03", "M": "canceling statement due to lock timeout"}])
    assert PostgresqlAdapter().is_lock_timeout(error)


def _operational_error(orig):
    """Return the ``OperationalError`` SQLAlchemy raises for a driver error; ``orig`` is the ``.orig`` of
    ``PGException(...)`` or ``MSException(...)``, from which the adapters read the error code."""
    from sqlalchemy.exc import OperationalError

    error = OperationalError("stmt", {}, Exception())
    error.orig = orig
    return error


def test_sql_client_create_index_skips_existing(monkeypatch, sql_client):
    ran = []
    monkeypatch.setattr(sql_client, "get_index_state", lambda table_name, index_name, lock_timeout: True)
    monkeypatch.setattr(sql_client, "_run_index_ddl", lambda *args, **kwargs: ran.append(args))

    sql_client.create_index("CREATE INDEX x ON t (a)", "t", "x")
    assert ran == []


def test_sql_client_create_index_retries_on_lock_timeout(monkeypatch, sql_client):
    from jans.pycloudlib.persistence.sql import MysqlAdapter

    monkeypatch.setattr("jans.pycloudlib.persistence.sql.time.sleep", lambda seconds: None)
    monkeypatch.setattr(sql_client, "adapter", MysqlAdapter())
    monkeypatch.setattr(sql_client, "get_index_state", lambda table_name, index_name, lock_timeout: None)

    calls = []

    def fake_run(query, table_name, index_name, lock_timeout, drop_first):
        calls.append(drop_first)
        if len(calls) < 3:
            raise _operational_error(MSException(1205).orig)

    monkeypatch.setattr(sql_client, "_run_index_ddl", fake_run)

    sql_client.create_index("CREATE INDEX x ON t (a)", "t", "x")
    assert calls == [False, False, False]


def test_sql_client_create_index_drops_invalid_index_on_retry(monkeypatch, sql_client):
    from jans.pycloudlib.persistence.sql import PostgresqlAdapter

    monkeypatch.setattr("jans.pycloudlib.persistence.sql.time.sleep", lambda seconds: None)
    monkeypatch.setattr(sql_client, "adapter", PostgresqlAdapter())

    # no index at first; the timed-out concurrent build then leaves an invalid one behind
    states = iter([None, False])
    monkeypatch.setattr(sql_client, "get_index_state", lambda table_name, index_name, lock_timeout: next(states))

    calls = []

    def fake_run(query, table_name, index_name, lock_timeout, drop_first):
        calls.append(drop_first)
        if len(calls) == 1:
            raise _operational_error(PGException("55P03").orig)

    monkeypatch.setattr(sql_client, "_run_index_ddl", fake_run)

    sql_client.create_index('CREATE INDEX "x" ON "t" ("a")', "t", "x")
    assert calls == [False, True]


def test_sql_client_create_index_stops_retry_when_index_became_valid(monkeypatch, sql_client):
    from jans.pycloudlib.persistence.sql import PostgresqlAdapter

    monkeypatch.setattr("jans.pycloudlib.persistence.sql.time.sleep", lambda seconds: None)
    monkeypatch.setattr(sql_client, "adapter", PostgresqlAdapter())

    # another process finished building the index while this one waited
    states = iter([None, True])
    monkeypatch.setattr(sql_client, "get_index_state", lambda table_name, index_name, lock_timeout: next(states))

    calls = []

    def fake_run(query, table_name, index_name, lock_timeout, drop_first):
        calls.append(drop_first)
        raise _operational_error(PGException("55P03").orig)

    monkeypatch.setattr(sql_client, "_run_index_ddl", fake_run)

    sql_client.create_index('CREATE INDEX "x" ON "t" ("a")', "t", "x")
    assert calls == [False]


def test_sql_client_create_index_gives_up_after_retries(monkeypatch, sql_client):
    from jans.pycloudlib.persistence.sql import MysqlAdapter

    monkeypatch.setattr("jans.pycloudlib.persistence.sql.time.sleep", lambda seconds: None)
    monkeypatch.setattr(sql_client, "adapter", MysqlAdapter())
    monkeypatch.setattr(sql_client, "get_index_state", lambda table_name, index_name, lock_timeout: None)

    calls = []

    def fake_run(query, table_name, index_name, lock_timeout, drop_first):
        calls.append(drop_first)
        raise _operational_error(MSException(1205).orig)

    monkeypatch.setattr(sql_client, "_run_index_ddl", fake_run)

    # must not raise
    sql_client.create_index("CREATE INDEX x ON t (a)", "t", "x", retries=2)
    assert len(calls) == 2


def test_sql_client_create_index_runs_at_least_once(monkeypatch, sql_client):
    from jans.pycloudlib.persistence.sql import MysqlAdapter

    monkeypatch.setattr(sql_client, "adapter", MysqlAdapter())
    monkeypatch.setattr(sql_client, "get_index_state", lambda table_name, index_name, lock_timeout: None)
    ran = []
    monkeypatch.setattr(sql_client, "_run_index_ddl", lambda *args, **kwargs: ran.append(args))

    sql_client.create_index("CREATE INDEX x ON t (a)", "t", "x", retries=0)
    assert len(ran) == 1


@pytest.mark.parametrize("row, expected", [
    (None, None),
    ((False,), False),
    ((True,), True),
])
def test_sql_client_get_index_state(monkeypatch, sql_client, row, expected):
    from unittest.mock import MagicMock

    conn = MagicMock()
    conn.execute.return_value.fetchone.return_value = row
    engine = MagicMock()
    engine.connect.return_value.__enter__.return_value = conn
    monkeypatch.setattr(type(sql_client), "engine", engine, raising=False)

    assert sql_client.get_index_state("t", "x") is expected


LOCK_TIMEOUT_WARNING = ("Warning", 1205, "Lock wait timeout exceeded; try restarting transaction")


def _fake_engine(conn):
    """Engine whose ``connect()`` context manager yields ``conn`` (also from ``execution_options()``)."""
    from unittest.mock import MagicMock

    conn.execution_options.return_value = conn
    engine = MagicMock()
    engine.connect.return_value.__enter__.return_value = conn
    return engine


def _executed(conn):
    """Statements passed to ``conn.execute``, as text, in call order."""
    return [str(call.args[0]) for call in conn.execute.call_args_list]


def _none_row():
    """Fake query result whose ``fetchone()`` finds no row."""
    from unittest.mock import MagicMock

    result = MagicMock()
    result.fetchone.return_value = None
    return result


def _lookup_conn(row, warnings=(), errors=None):
    """Fake connection: ``SHOW WARNINGS`` gives ``warnings``, any other statement gives a result whose
    ``fetchone()`` is ``row``; a statement listed in ``errors`` raises the mapped exception."""
    from unittest.mock import MagicMock

    errors = errors or {}
    conn = MagicMock()

    def execute(statement, *args, **kwargs):
        statement = str(statement)
        if statement in errors:
            raise errors[statement]
        if statement == "SHOW WARNINGS":
            return list(warnings)
        result = MagicMock()
        result.fetchone.return_value = row
        return result

    conn.execute.side_effect = execute
    return conn


def _conn_events(conn):
    """Statements executed on the connection and the other calls (rollback, commit), in call order."""
    return [str(call.args[0]) if call[0] == "execute" else call[0] for call in conn.method_calls]


def test_mysql_adapter_looks_up_index_warnings_with_show_warnings():
    from jans.pycloudlib.persistence.sql import MysqlAdapter

    assert MysqlAdapter.index_state_warnings_query == "SHOW WARNINGS"


def test_mysql_adapter_is_lock_timeout_warning_true_for_code_1205():
    from jans.pycloudlib.persistence.sql import MysqlAdapter

    assert MysqlAdapter().is_lock_timeout_warning(LOCK_TIMEOUT_WARNING) is True


def test_mysql_adapter_is_lock_timeout_warning_false_for_other_code():
    from jans.pycloudlib.persistence.sql import MysqlAdapter

    assert MysqlAdapter().is_lock_timeout_warning(("Warning", 1287, "'SHOW VARIABLES' is deprecated")) is False


def test_postgresql_adapter_has_no_index_state_warnings_query():
    from jans.pycloudlib.persistence.sql import PostgresqlAdapter

    assert not hasattr(PostgresqlAdapter, "index_state_warnings_query")


def test_sql_client_get_index_state_raises_when_mysql_lookup_timed_out(monkeypatch, sql_client):
    from jans.pycloudlib.persistence.sql import IndexLockTimeoutError
    from jans.pycloudlib.persistence.sql import MysqlAdapter

    # MySQL 5.7 skips a table whose metadata lock wait timed out: no row, only warning 1205
    conn = _lookup_conn(None, [LOCK_TIMEOUT_WARNING])
    monkeypatch.setattr(sql_client, "adapter", MysqlAdapter())
    monkeypatch.setattr(type(sql_client), "engine", _fake_engine(conn), raising=False)

    with pytest.raises(IndexLockTimeoutError, match="idx_lookup"):
        sql_client.get_index_state("t", "idx_lookup")

    assert _executed(conn) == [MysqlAdapter.index_state_query, "SHOW WARNINGS"]


@pytest.mark.parametrize("warnings", [
    pytest.param([], id="no_warnings"),
    pytest.param([("Warning", 1287, "'SHOW VARIABLES' is deprecated")], id="other_warning"),
])
def test_sql_client_get_index_state_mysql_missing_index_checks_warnings(monkeypatch, sql_client, warnings):
    from jans.pycloudlib.persistence.sql import MysqlAdapter

    conn = _lookup_conn(None, warnings)
    monkeypatch.setattr(sql_client, "adapter", MysqlAdapter())
    monkeypatch.setattr(type(sql_client), "engine", _fake_engine(conn), raising=False)

    assert sql_client.get_index_state("t", "x") is None
    assert _executed(conn) == [MysqlAdapter.index_state_query, "SHOW WARNINGS"]


def test_sql_client_get_index_state_sets_and_resets_mysql_lock_timeout(monkeypatch, sql_client):
    from jans.pycloudlib.persistence.sql import MysqlAdapter

    adapter = MysqlAdapter()
    conn = _lookup_conn((1,))
    monkeypatch.setattr(sql_client, "adapter", adapter)
    monkeypatch.setattr(type(sql_client), "engine", _fake_engine(conn), raising=False)

    assert sql_client.get_index_state("t", "x", lock_timeout=7) is True

    # no warnings query: the index exists; the reset follows the rollback
    assert _conn_events(conn) == [
        "SET SESSION lock_wait_timeout = 7", adapter.index_state_query, "rollback",
        "SET SESSION lock_wait_timeout = DEFAULT", "commit",
    ]
    conn.invalidate.assert_not_called()


def test_sql_client_get_index_state_sets_and_resets_postgresql_lock_timeout(monkeypatch, sql_client):
    from jans.pycloudlib.persistence.sql import PostgresqlAdapter

    adapter = PostgresqlAdapter()
    conn = _lookup_conn(None)
    monkeypatch.setattr(sql_client, "adapter", adapter)
    sql_client._pg_has_progress_view = True
    monkeypatch.setattr(type(sql_client), "engine", _fake_engine(conn), raising=False)

    assert sql_client.get_index_state("t", "x", lock_timeout=7) is None

    # PostgreSQL has no warnings query; the reset follows the rollback
    assert _conn_events(conn) == [
        "SET lock_timeout = '7s'", adapter.index_state_query, "rollback", "RESET lock_timeout", "commit",
    ]
    conn.invalidate.assert_not_called()


def test_sql_client_get_index_state_resets_lock_timeout_after_mysql_lookup_error(monkeypatch, sql_client):
    from sqlalchemy.exc import OperationalError

    from jans.pycloudlib.persistence.sql import MysqlAdapter

    adapter = MysqlAdapter()
    conn = _lookup_conn(None, errors={adapter.index_state_query: OperationalError("stmt", {}, Exception())})
    monkeypatch.setattr(sql_client, "adapter", adapter)
    monkeypatch.setattr(type(sql_client), "engine", _fake_engine(conn), raising=False)

    with pytest.raises(OperationalError):
        sql_client.get_index_state("t", "x", lock_timeout=7)

    assert _conn_events(conn) == [
        "SET SESSION lock_wait_timeout = 7", adapter.index_state_query, "rollback",
        "SET SESSION lock_wait_timeout = DEFAULT", "commit",
    ]
    conn.invalidate.assert_not_called()


def test_sql_client_get_index_state_resets_lock_timeout_after_postgresql_lookup_error(monkeypatch, sql_client):
    from sqlalchemy.exc import OperationalError

    from jans.pycloudlib.persistence.sql import PostgresqlAdapter

    adapter = PostgresqlAdapter()
    conn = _lookup_conn(None, errors={adapter.index_state_query: OperationalError("stmt", {}, Exception())})
    monkeypatch.setattr(sql_client, "adapter", adapter)
    sql_client._pg_has_progress_view = True
    monkeypatch.setattr(type(sql_client), "engine", _fake_engine(conn), raising=False)

    with pytest.raises(OperationalError):
        sql_client.get_index_state("t", "x", lock_timeout=7)

    # the rollback ends the failed transaction, so the RESET can run
    assert _conn_events(conn) == [
        "SET lock_timeout = '7s'", adapter.index_state_query, "rollback", "RESET lock_timeout", "commit",
    ]
    conn.invalidate.assert_not_called()


def test_sql_client_get_index_state_resets_lock_timeout_after_mysql_timeout_warning(monkeypatch, sql_client):
    from jans.pycloudlib.persistence.sql import IndexLockTimeoutError
    from jans.pycloudlib.persistence.sql import MysqlAdapter

    adapter = MysqlAdapter()
    conn = _lookup_conn(None, [LOCK_TIMEOUT_WARNING])
    monkeypatch.setattr(sql_client, "adapter", adapter)
    monkeypatch.setattr(type(sql_client), "engine", _fake_engine(conn), raising=False)

    with pytest.raises(IndexLockTimeoutError):
        sql_client.get_index_state("t", "x", lock_timeout=7)

    # the warnings are read before the rollback
    assert _conn_events(conn) == [
        "SET SESSION lock_wait_timeout = 7", adapter.index_state_query, "SHOW WARNINGS", "rollback",
        "SET SESSION lock_wait_timeout = DEFAULT", "commit",
    ]
    conn.invalidate.assert_not_called()


def test_sql_client_get_index_state_invalidates_connection_when_reset_fails(monkeypatch, sql_client):
    from sqlalchemy.exc import OperationalError

    from jans.pycloudlib.persistence.sql import MysqlAdapter

    adapter = MysqlAdapter()
    conn = _lookup_conn((1,), errors={adapter.reset_lock_timeout_query: OperationalError("stmt", {}, Exception())})
    monkeypatch.setattr(sql_client, "adapter", adapter)
    monkeypatch.setattr(type(sql_client), "engine", _fake_engine(conn), raising=False)

    assert sql_client.get_index_state("t", "x", lock_timeout=7) is True

    conn.invalidate.assert_called_once_with()


@pytest.mark.parametrize("bad", [0, -1, 0.5, 2.0, "10", True])
def test_sql_client_get_index_state_rejects_non_positive_lock_timeout(monkeypatch, sql_client, bad):
    # 0 means "no timeout" on PostgreSQL (unbounded wait); no connection may be opened before the value is rejected
    ran = []
    monkeypatch.setattr(type(sql_client), "engine", property(lambda self: ran.append("engine")), raising=False)

    with pytest.raises(ValueError, match="lock_timeout"):
        sql_client.get_index_state("t", "x", lock_timeout=bad)

    assert ran == []


def test_sql_client_create_index_passes_lock_timeout_to_lookup(monkeypatch, sql_client):
    seen = []

    def fake_state(table_name, index_name, lock_timeout=None):
        seen.append(lock_timeout)
        return True

    monkeypatch.setattr(sql_client, "get_index_state", fake_state)

    sql_client.create_index("CREATE INDEX x ON t (a)", "t", "x", lock_timeout=3)

    assert seen == [3]


def _create_index_after_lookup_timeouts(monkeypatch, sql_client, adapter, error):
    """Run ``create_index`` with a lookup that raises ``error`` twice and then finds no index.
    Return the ``lock_timeout`` of every lookup and the ``drop_first`` of every DDL run."""
    monkeypatch.setattr("jans.pycloudlib.persistence.sql.time.sleep", lambda seconds: None)
    monkeypatch.setattr(sql_client, "adapter", adapter)
    lookups = []

    def fake_state(table_name, index_name, lock_timeout=None):
        lookups.append(lock_timeout)
        if len(lookups) < 3:
            raise error
        return None

    monkeypatch.setattr(sql_client, "get_index_state", fake_state)
    ddl_runs = []

    def fake_run(query, table_name, index_name, lock_timeout, drop_first):
        ddl_runs.append(drop_first)

    monkeypatch.setattr(sql_client, "_run_index_ddl", fake_run)

    sql_client.create_index("CREATE INDEX x ON t (a)", "t", "x")

    return lookups, ddl_runs


def test_sql_client_create_index_retries_when_mysql_lookup_times_out(monkeypatch, sql_client):
    from jans.pycloudlib.persistence.sql import IndexLockTimeoutError
    from jans.pycloudlib.persistence.sql import MysqlAdapter

    error = IndexLockTimeoutError("timed out waiting for a lock to look up index x")

    lookups, ddl_runs = _create_index_after_lookup_timeouts(monkeypatch, sql_client, MysqlAdapter(), error)

    assert lookups == [10, 10, 10]
    assert ddl_runs == [False]


def test_sql_client_create_index_retries_when_postgresql_lookup_times_out(monkeypatch, sql_client):
    from jans.pycloudlib.persistence.sql import PostgresqlAdapter

    error = _operational_error(PGException("55P03").orig)

    lookups, ddl_runs = _create_index_after_lookup_timeouts(monkeypatch, sql_client, PostgresqlAdapter(), error)

    assert lookups == [10, 10, 10]
    assert ddl_runs == [False]


def test_sql_client_create_index_gives_up_when_lookup_keeps_timing_out(monkeypatch, sql_client, caplog):
    from jans.pycloudlib.persistence.sql import IndexLockTimeoutError
    from jans.pycloudlib.persistence.sql import MysqlAdapter

    monkeypatch.setattr("jans.pycloudlib.persistence.sql.time.sleep", lambda seconds: None)
    monkeypatch.setattr(sql_client, "adapter", MysqlAdapter())
    lookups = []

    def fake_state(table_name, index_name, lock_timeout=None):
        lookups.append(lock_timeout)
        raise IndexLockTimeoutError("timed out waiting for a lock to look up index x")

    monkeypatch.setattr(sql_client, "get_index_state", fake_state)
    calls = []
    monkeypatch.setattr(sql_client, "_run_index_ddl", lambda *args, **kwargs: calls.append(args))

    # must not raise
    sql_client.create_index("CREATE INDEX x ON t (a)", "t", "x", retries=2)

    assert len(lookups) == 2
    assert calls == []
    assert "will retry on next start" in caplog.text


def test_sql_client_create_index_skips_ddl_when_mysql_lookup_timed_out(monkeypatch, sql_client, caplog):
    from jans.pycloudlib.persistence.sql import MysqlAdapter

    monkeypatch.setattr("jans.pycloudlib.persistence.sql.time.sleep", lambda seconds: None)
    monkeypatch.setattr(sql_client, "adapter", MysqlAdapter())
    # the lookup of an existing index returns no row while a lock is held; only the warning tells why
    conn = _lookup_conn(None, [LOCK_TIMEOUT_WARNING])
    monkeypatch.setattr(type(sql_client), "engine", _fake_engine(conn), raising=False)
    calls = []
    monkeypatch.setattr(sql_client, "_run_index_ddl", lambda *args, **kwargs: calls.append(args))

    # must not raise
    sql_client.create_index("CREATE INDEX x ON t (a)", "t", "x", retries=2)

    assert calls == []
    assert "will retry on next start" in caplog.text


def test_postgresql_index_state_query_limited_to_current_database():
    from jans.pycloudlib.persistence.sql import PostgresqlAdapter

    assert "p.datname = current_database()" in PostgresqlAdapter.index_state_query
    assert "p.datname = current_database()" not in PostgresqlAdapter.index_state_query_legacy


def test_sql_client_run_index_ddl_postgresql_autocommit(monkeypatch, sql_client):
    from unittest.mock import MagicMock

    from jans.pycloudlib.persistence.sql import PostgresqlAdapter

    conn = MagicMock()
    # index is still invalid when rechecked under the lock
    conn.execute.return_value.fetchone.return_value = (False,)
    adapter = PostgresqlAdapter()
    acquire, release = adapter.index_lock_queries("t", "x")
    monkeypatch.setattr(sql_client, "adapter", adapter)
    sql_client._pg_has_progress_view = True
    monkeypatch.setattr(type(sql_client), "engine", _fake_engine(conn), raising=False)

    sql_client._run_index_ddl("CREATE INDEX CONCURRENTLY x", "t", "x", 10, drop_first=False)

    conn.execution_options.assert_called_once_with(isolation_level="AUTOCOMMIT")
    # a transaction left open on the pooled connection (e.g. by pg8000's pre-ping) is ended first
    conn.connection.rollback.assert_called_once_with()
    conn.begin.assert_not_called()
    executed = _executed(conn)
    assert executed[0] == "SET lock_timeout = '10s'"
    assert executed[1] == acquire
    assert executed[2] == adapter.index_state_query
    assert executed[3:] == [
        'DROP INDEX CONCURRENTLY IF EXISTS "x"',
        "CREATE INDEX CONCURRENTLY x",
        release,
        "RESET lock_timeout",
    ]


def test_sql_client_run_index_ddl_mysql_transaction(monkeypatch, sql_client):
    from unittest.mock import MagicMock

    from jans.pycloudlib.persistence.sql import MysqlAdapter

    conn = MagicMock()
    monkeypatch.setattr(sql_client, "adapter", MysqlAdapter())
    monkeypatch.setattr(type(sql_client), "engine", _fake_engine(conn), raising=False)

    sql_client._run_index_ddl("CREATE INDEX x ON t (a)", "t", "x", 10, drop_first=False)

    conn.begin.assert_called_once()
    conn.execution_options.assert_not_called()
    conn.connection.rollback.assert_not_called()
    assert _executed(conn) == [
        "SET SESSION lock_wait_timeout = 10",
        "CREATE INDEX x ON t (a)",
        "SET SESSION lock_wait_timeout = DEFAULT",
    ]


def test_sql_client_run_index_ddl_resets_timeout_after_failure(monkeypatch, sql_client):
    from unittest.mock import MagicMock

    from sqlalchemy.exc import OperationalError

    from jans.pycloudlib.persistence.sql import PostgresqlAdapter

    conn = MagicMock()
    error = OperationalError("stmt", {}, Exception())

    def execute(statement, *args, **kwargs):
        if str(statement).startswith("CREATE"):
            raise error
        # index does not exist when rechecked under the lock
        return _none_row()

    conn.execute.side_effect = execute
    monkeypatch.setattr(sql_client, "adapter", PostgresqlAdapter())
    sql_client._pg_has_progress_view = True
    monkeypatch.setattr(type(sql_client), "engine", _fake_engine(conn), raising=False)

    with pytest.raises(OperationalError):
        sql_client._run_index_ddl("CREATE INDEX CONCURRENTLY x", "t", "x", 10, drop_first=False)

    executed = _executed(conn)
    # the advisory lock is released and the timeout reset even though the DDL failed
    assert executed[-2].startswith("SELECT pg_advisory_unlock")
    assert executed[-1] == "RESET lock_timeout"
    conn.invalidate.assert_not_called()


def test_sql_client_run_index_ddl_invalidates_connection_when_reset_fails(monkeypatch, sql_client):
    from unittest.mock import MagicMock

    from sqlalchemy.exc import OperationalError

    from jans.pycloudlib.persistence.sql import PostgresqlAdapter

    conn = MagicMock()

    def execute(statement, *args, **kwargs):
        if str(statement).startswith("RESET"):
            raise OperationalError("stmt", {}, Exception())
        return MagicMock()

    conn.execute.side_effect = execute
    monkeypatch.setattr(sql_client, "adapter", PostgresqlAdapter())
    monkeypatch.setattr(type(sql_client), "engine", _fake_engine(conn), raising=False)

    sql_client._run_index_ddl("CREATE INDEX CONCURRENTLY x", "t", "x", 10, drop_first=False)

    conn.invalidate.assert_called_once()


def test_sql_client_alter_table_runs_with_lock_timeout(monkeypatch, sql_client):
    from jans.pycloudlib.persistence.sql import MysqlAdapter

    monkeypatch.setattr(sql_client, "adapter", MysqlAdapter())
    calls = []
    monkeypatch.setattr(sql_client, "_run_index_ddl", lambda *args, **kwargs: calls.append((args, kwargs)))

    assert sql_client.alter_table("ALTER TABLE t ADD COLUMN c INT", "t", lock_timeout=7) is True
    assert calls == [(("ALTER TABLE t ADD COLUMN c INT", "t", "", 7), {"drop_first": False})]


def test_sql_client_alter_table_gives_up_after_retries(monkeypatch, sql_client):
    from jans.pycloudlib.persistence.sql import MysqlAdapter

    monkeypatch.setattr("jans.pycloudlib.persistence.sql.time.sleep", lambda seconds: None)
    monkeypatch.setattr(sql_client, "adapter", MysqlAdapter())
    calls = []

    def fake_run(*args, **kwargs):
        calls.append(args)
        raise _operational_error(MSException(1205).orig)

    monkeypatch.setattr(sql_client, "_run_index_ddl", fake_run)

    # must not raise; the caller is told the statement did not run
    assert sql_client.alter_table("ALTER TABLE t ADD COLUMN c INT", "t", retries=2) is False
    assert len(calls) == 2


def test_sql_client_alter_table_reraises_other_errors(monkeypatch, sql_client):
    from sqlalchemy.exc import OperationalError

    from jans.pycloudlib.persistence.sql import MysqlAdapter

    monkeypatch.setattr(sql_client, "adapter", MysqlAdapter())
    calls = []

    def fake_run(*args, **kwargs):
        calls.append(args)
        raise _operational_error(MSException(1060).orig)

    monkeypatch.setattr(sql_client, "_run_index_ddl", fake_run)

    # e.g. 1060 (duplicate column) is left to the caller, without retries
    with pytest.raises(OperationalError):
        sql_client.alter_table("ALTER TABLE t ADD COLUMN c INT", "t")
    assert len(calls) == 1


def test_sql_client_alter_table_runs_at_least_once(monkeypatch, sql_client):
    from jans.pycloudlib.persistence.sql import MysqlAdapter

    monkeypatch.setattr(sql_client, "adapter", MysqlAdapter())
    calls = []
    monkeypatch.setattr(sql_client, "_run_index_ddl", lambda *args, **kwargs: calls.append(args))

    assert sql_client.alter_table("ALTER TABLE t ADD COLUMN c INT", "t", retries=0) is True
    assert len(calls) == 1


class _FakeTransaction:
    """Transaction context manager that records whether the block exited with an error (rollback)."""

    def __init__(self):
        self.rolled_back = None

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_value, exc_traceback):
        self.rolled_back = exc_type is not None
        return False


def _failing_conn(transaction):
    """Fake connection whose ``begin()`` returns ``transaction`` and whose every statement fails."""
    from unittest.mock import MagicMock

    from sqlalchemy.exc import OperationalError

    conn = MagicMock()
    conn.begin.return_value = transaction
    conn.execute.side_effect = OperationalError("stmt", {}, Exception())
    return conn


@pytest.mark.parametrize("method, args, handler", [
    ("create_table", ("t", {"doc_id": "VARCHAR(64)"}, "doc_id"), "on_create_table_error"),
    ("create_index", ("CREATE INDEX x ON t (a)",), "on_create_index_error"),
    ("insert_into", ("t", {"doc_id": "1"}), "on_insert_into_error"),
])
def test_sql_client_duplicate_error_handled_after_rollback(monkeypatch, sql_client, method, args, handler):
    from unittest.mock import MagicMock

    transaction = _FakeTransaction()
    conn = _failing_conn(transaction)
    handled = []

    adapter = MagicMock()
    adapter.quote_char = '"'
    # the transaction must be rolled back before the error handler suppresses the duplicate
    getattr(adapter, handler).side_effect = lambda error: handled.append(transaction.rolled_back)
    monkeypatch.setattr(sql_client, "adapter", adapter)
    monkeypatch.setattr(type(sql_client), "engine", _fake_engine(conn), raising=False)
    monkeypatch.setattr(sql_client, "_apply_json_defaults", lambda table, mapping: mapping, raising=False)
    monkeypatch.setattr(type(sql_client), "metadata", MagicMock(), raising=False)

    getattr(sql_client, method)(*args)

    assert handled == [True]


def test_postgresql_adapter_drop_index_query_escapes_quotes():
    from jans.pycloudlib.persistence.sql import PostgresqlAdapter

    adapter = PostgresqlAdapter()
    assert adapter.drop_index_query("t", "t_a") == 'DROP INDEX CONCURRENTLY IF EXISTS "t_a"'
    assert adapter.drop_index_query("t", 'a"b') == 'DROP INDEX CONCURRENTLY IF EXISTS "a""b"'


def test_mysql_adapter_drop_index_query_escapes_quotes():
    from jans.pycloudlib.persistence.sql import MysqlAdapter

    adapter = MysqlAdapter()
    assert adapter.drop_index_query("t", "i") == "DROP INDEX `i` ON `t`"
    assert adapter.drop_index_query("t", "a`b") == "DROP INDEX `a``b` ON `t`"


def test_postgresql_adapter_index_lock_queries():
    from jans.pycloudlib.persistence.sql import PostgresqlAdapter

    adapter = PostgresqlAdapter()
    acquire, release = adapter.index_lock_queries("t", "x")
    # non-blocking acquire: a blocking pg_advisory_lock would hold a snapshot and deadlock with CONCURRENTLY
    assert acquire.startswith("SELECT pg_try_advisory_lock(") and release.startswith("SELECT pg_advisory_unlock(")
    # same keys for acquire and release; different index -> different key
    assert acquire.split("(", 1)[1] == release.split("(", 1)[1]
    assert adapter.index_lock_queries("t", "y") != (acquire, release)


def test_mysql_adapter_has_no_index_lock():
    from jans.pycloudlib.persistence.sql import MysqlAdapter

    assert MysqlAdapter().index_lock_queries("t", "x") is None


def test_sql_client_run_index_ddl_skips_when_index_became_valid_under_lock(monkeypatch, sql_client):
    from unittest.mock import MagicMock

    from jans.pycloudlib.persistence.sql import PostgresqlAdapter

    conn = MagicMock()
    # another loader finished building the index while this one waited for the lock
    conn.execute.return_value.fetchone.return_value = (True,)
    adapter = PostgresqlAdapter()
    monkeypatch.setattr(sql_client, "adapter", adapter)
    sql_client._pg_has_progress_view = True
    monkeypatch.setattr(type(sql_client), "engine", _fake_engine(conn), raising=False)

    # even if the caller saw an invalid index, it must not drop/create a now-valid one
    sql_client._run_index_ddl("CREATE INDEX CONCURRENTLY x", "t", "x", 10, drop_first=True)

    executed = _executed(conn)
    assert not any(statement.startswith(("DROP", "CREATE")) for statement in executed)
    assert executed[-2].startswith("SELECT pg_advisory_unlock")
    assert executed[-1] == "RESET lock_timeout"


def test_sql_client_run_index_ddl_does_not_unlock_when_lock_not_acquired(monkeypatch, sql_client):
    from unittest.mock import MagicMock

    from jans.pycloudlib.persistence.sql import PostgresqlAdapter
    from jans.pycloudlib.persistence.sql import IndexLockTimeoutError

    conn = MagicMock()

    def execute(statement, *args, **kwargs):
        result = MagicMock()
        # another session holds the lock for the whole time
        result.scalar.return_value = False if str(statement).startswith("SELECT pg_try_advisory_lock") else MagicMock()
        return result

    conn.execute.side_effect = execute
    monkeypatch.setattr("jans.pycloudlib.persistence.sql.time.sleep", lambda seconds: None)
    monkeypatch.setattr(sql_client, "adapter", PostgresqlAdapter())
    sql_client._pg_has_progress_view = True
    monkeypatch.setattr(type(sql_client), "engine", _fake_engine(conn), raising=False)

    with pytest.raises(IndexLockTimeoutError):
        sql_client._run_index_ddl("CREATE INDEX CONCURRENTLY x", "t", "x", 0, drop_first=False)

    executed = _executed(conn)
    assert not any(statement.startswith("SELECT pg_advisory_unlock") for statement in executed)
    assert not any(statement.startswith(("DROP", "CREATE")) for statement in executed)
    assert executed[-1] == "RESET lock_timeout"


def test_sql_client_run_index_ddl_polls_for_lock_without_blocking(monkeypatch, sql_client):
    from unittest.mock import MagicMock

    from jans.pycloudlib.persistence.sql import PostgresqlAdapter

    conn = MagicMock()
    attempts = []

    def execute(statement, *args, **kwargs):
        result = MagicMock()
        statement_text = str(statement)
        if statement_text.startswith("SELECT pg_try_advisory_lock"):
            attempts.append(1)
            # held by another loader for the first two polls
            result.scalar.return_value = len(attempts) > 2
        elif statement_text == PostgresqlAdapter.index_state_query:
            result.fetchone.return_value = None
        return result

    conn.execute.side_effect = execute
    slept = []
    monkeypatch.setattr("jans.pycloudlib.persistence.sql.time.sleep", slept.append)
    monkeypatch.setattr(sql_client, "adapter", PostgresqlAdapter())
    sql_client._pg_has_progress_view = True
    monkeypatch.setattr(type(sql_client), "engine", _fake_engine(conn), raising=False)

    sql_client._run_index_ddl("CREATE INDEX CONCURRENTLY x", "t", "x", 10, drop_first=False)

    executed = _executed(conn)
    assert len(attempts) == 3 and len(slept) == 2
    # a blocking advisory lock is never used (it would hold a snapshot while waiting)
    assert not any(statement.startswith("SELECT pg_advisory_lock") for statement in executed)
    assert "CREATE INDEX CONCURRENTLY x" in executed
    assert any(statement.startswith("SELECT pg_advisory_unlock") for statement in executed)


def test_sql_client_create_index_retries_on_index_lock_timeout(monkeypatch, sql_client):
    from jans.pycloudlib.persistence.sql import PostgresqlAdapter
    from jans.pycloudlib.persistence.sql import IndexLockTimeoutError

    monkeypatch.setattr("jans.pycloudlib.persistence.sql.time.sleep", lambda seconds: None)
    monkeypatch.setattr(sql_client, "adapter", PostgresqlAdapter())
    monkeypatch.setattr(sql_client, "get_index_state", lambda table_name, index_name, lock_timeout: None)

    calls = []

    def fake_run(query, table_name, index_name, lock_timeout, drop_first):
        calls.append(1)
        if len(calls) < 3:
            raise IndexLockTimeoutError("held by another loader")

    monkeypatch.setattr(sql_client, "_run_index_ddl", fake_run)

    sql_client.create_index('CREATE INDEX "x" ON "t" ("a")', "t", "x")
    assert len(calls) == 3


def test_sql_client_create_index_retries_on_deadlock(monkeypatch, sql_client):
    from jans.pycloudlib.persistence.sql import PostgresqlAdapter

    monkeypatch.setattr("jans.pycloudlib.persistence.sql.time.sleep", lambda seconds: None)
    monkeypatch.setattr(sql_client, "adapter", PostgresqlAdapter())
    monkeypatch.setattr(sql_client, "get_index_state", lambda table_name, index_name, lock_timeout: None)

    calls = []

    def fake_run(query, table_name, index_name, lock_timeout, drop_first):
        calls.append(1)
        if len(calls) == 1:
            raise _operational_error(PGException("40P01").orig)  # deadlock_detected

    monkeypatch.setattr(sql_client, "_run_index_ddl", fake_run)

    sql_client.create_index('CREATE INDEX "x" ON "t" ("a")', "t", "x")
    assert len(calls) == 2


def test_postgresql_adapter_is_lock_timeout_includes_deadlock():
    from jans.pycloudlib.persistence.sql import PostgresqlAdapter

    assert PostgresqlAdapter().is_lock_timeout(PGException("40P01"))


def test_sql_client_run_index_ddl_no_lock_for_alter_table(monkeypatch, sql_client):
    from unittest.mock import MagicMock

    from jans.pycloudlib.persistence.sql import PostgresqlAdapter

    conn = MagicMock()
    monkeypatch.setattr(sql_client, "adapter", PostgresqlAdapter())
    sql_client._pg_has_progress_view = True
    monkeypatch.setattr(type(sql_client), "engine", _fake_engine(conn), raising=False)

    # alter_table passes an empty index name: nothing to lock or check
    sql_client._run_index_ddl("ALTER TABLE t ADD COLUMN c INT", "t", "", 10, drop_first=False)

    assert not any("advisory" in statement for statement in _executed(conn))


@pytest.mark.parametrize("version_num, uses_progress_view", [
    (110016, False),
    (119999, False),
    (120000, True),
    (160009, True),
])
def test_sql_client_index_state_query_depends_on_server_version(monkeypatch, sql_client, version_num, uses_progress_view):
    from unittest.mock import MagicMock

    from jans.pycloudlib.persistence.sql import PostgresqlAdapter

    adapter = PostgresqlAdapter()
    monkeypatch.setattr(sql_client, "adapter", adapter)
    conn = MagicMock()
    conn.execute.return_value.scalar.return_value = str(version_num)

    query = sql_client._index_state_sql(conn)

    assert query == (adapter.index_state_query if uses_progress_view else adapter.index_state_query_legacy)
    assert ("pg_stat_progress_create_index" in query) is uses_progress_view


def test_sql_client_index_state_query_version_is_looked_up_once(monkeypatch, sql_client):
    from unittest.mock import MagicMock

    from jans.pycloudlib.persistence.sql import PostgresqlAdapter

    monkeypatch.setattr(sql_client, "adapter", PostgresqlAdapter())
    conn = MagicMock()
    conn.execute.return_value.scalar.return_value = "110016"

    for _ in range(3):
        sql_client._index_state_sql(conn)

    assert conn.execute.call_count == 1


def test_sql_client_index_state_query_mysql_unchanged(monkeypatch, sql_client):
    from unittest.mock import MagicMock

    from jans.pycloudlib.persistence.sql import MysqlAdapter

    adapter = MysqlAdapter()
    monkeypatch.setattr(sql_client, "adapter", adapter)
    conn = MagicMock()

    assert sql_client._index_state_sql(conn) == adapter.index_state_query
    conn.execute.assert_not_called()  # no version lookup for MySQL


def test_postgresql_legacy_index_state_query_has_no_progress_view():
    from jans.pycloudlib.persistence.sql import PostgresqlAdapter

    legacy = PostgresqlAdapter.index_state_query_legacy
    assert "pg_stat_progress_create_index" not in legacy
    assert ":table_name" in legacy and ":index_name" in legacy and "indisvalid" in legacy


@pytest.mark.parametrize("bad", [0, -1, -10, 0.5, 1.5, 2.0, None, "10", True])
def test_sql_client_create_index_rejects_non_positive_lock_timeout(monkeypatch, sql_client, bad):
    # 0 means "no timeout" on PostgreSQL (unbounded wait); nothing may run before the value is rejected
    ran = []
    monkeypatch.setattr(sql_client, "get_index_state", lambda table_name, index_name, lock_timeout: ran.append("state"))
    monkeypatch.setattr(sql_client, "_run_index_ddl", lambda *args, **kwargs: ran.append("ddl"))
    monkeypatch.setattr(type(sql_client), "engine", property(lambda self: ran.append("engine")), raising=False)

    with pytest.raises(ValueError, match="lock_timeout"):
        sql_client.create_index("CREATE INDEX x ON t (a)", "t", "x", lock_timeout=bad)
    with pytest.raises(ValueError, match="lock_timeout"):  # also rejected without the names
        sql_client.create_index("CREATE INDEX x ON t (a)", lock_timeout=bad)

    assert ran == []


@pytest.mark.parametrize("bad", [0, -1, 0.5, 2.0, None, "10", True])
def test_sql_client_alter_table_rejects_non_positive_lock_timeout(monkeypatch, sql_client, bad):
    ran = []
    monkeypatch.setattr(sql_client, "_run_index_ddl", lambda *args, **kwargs: ran.append("ddl"))

    with pytest.raises(ValueError, match="lock_timeout"):
        sql_client.alter_table("ALTER TABLE t ADD COLUMN c INT", "t", lock_timeout=bad)

    assert ran == []


def test_sql_client_lock_timeout_default_is_ten_seconds(monkeypatch, sql_client):
    from jans.pycloudlib.persistence.sql import MysqlAdapter

    monkeypatch.setattr(sql_client, "adapter", MysqlAdapter())
    monkeypatch.setattr(sql_client, "get_index_state", lambda table_name, index_name, lock_timeout: None)
    seen = []

    def fake_run(query, table_name, index_name, lock_timeout, drop_first):
        seen.append(lock_timeout)

    monkeypatch.setattr(sql_client, "_run_index_ddl", fake_run)

    sql_client.create_index("CREATE INDEX x ON t (a)", "t", "x")
    sql_client.create_index("CREATE INDEX x ON t (a)", "t", "x", lock_timeout=3)

    assert seen == [10, 3]


def test_sql_client_alter_table_default_lock_timeout_is_ten_seconds(monkeypatch, sql_client):
    seen = []

    def fake_run(query, table_name, index_name, lock_timeout, drop_first):
        seen.append(lock_timeout)

    monkeypatch.setattr(sql_client, "_run_index_ddl", fake_run)

    sql_client.alter_table("ALTER TABLE t ADD COLUMN c INT", "t")

    assert seen == [10]
