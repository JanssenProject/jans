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
