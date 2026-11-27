"""Tests of the index creation of ``scripts/sql_setup.py``.

Run from ``docker-jans-persistence-loader/``: ``pytest tests`` (``tests/conftest.py`` makes ``scripts/`` importable).

The SQL client is a fake built from the real ``SqlClient``, so a call with a signature that the library does
not have fails the test. The tests need ``jans-pycloudlib`` with the named ``create_index``, ``alter_table`` and
``get_index_state`` (the version that ``JANS_SOURCE_VERSION`` selects) and run without a database.
"""
import logging
import re
from unittest.mock import call, create_autospec

import pymysql
import pytest
from jans.pycloudlib.persistence.sql import IndexLockTimeoutError, SqlClient
from sqlalchemy.exc import InterfaceError, OperationalError

# importable through conftest.py, which puts the loader's scripts folder on the path
import sql_setup

QUOTE_CHARACTER = {"mysql": "`", "pgsql": '"'}

# MySQL error codes; pymysql raises each of them as OperationalError. The value 1060 is written only here, not taken
# from ``sql_setup.MYSQL_DUPLICATE_COLUMN_ERROR``, so changing that constant fails
# ``test_mysql57_creates_index_when_another_loader_added_the_column``
MYSQL_UNKNOWN_COLUMN = 1054
MYSQL_DUPLICATE_COLUMN = 1060
MYSQL_DUPLICATE_KEY_NAME = 1061
MYSQL_DEADLOCK = 1213
MYSQL_LOST_CONNECTION = 2013

MYSQL_JSON_INDEXES = [
    "CAST($field->'$.v' AS CHAR(128) ARRAY)",
    "CAST($field->'$.v[0]' AS CHAR(128))",
    "CAST($field->'$.v[1]' AS CHAR(128))",
    "CAST($field->'$.v[2]' AS CHAR(128))",
]
PGSQL_JSON_INDEXES = ["USING GIN (JSONB_PATH_QUERY_ARRAY(\"$field\", '$[*]'))"]

UNIQUE_INDEX_QUERIES = {
    "mysql": {
        "mail": "ALTER TABLE `jansPerson` ADD UNIQUE INDEX `jansperson_mail_unique_idx` (`mail`)",
        "uid": "ALTER TABLE `jansPerson` ADD UNIQUE INDEX `jansperson_uid_unique_idx` (`uid`)",
    },
    "pgsql": {
        "mail": 'CREATE UNIQUE INDEX "jansperson_mail_unique_idx" ON "jansPerson" ("mail")',
        "uid": 'CREATE UNIQUE INDEX "jansperson_uid_unique_idx" ON "jansPerson" ("uid")',
    },
}

# the JSON array positions that MySQL < 8.0 indexes through virtual columns; the value 4 is
# written only here, so changing ``MYSQL_VIRTUAL_COLUMN_POSITIONS`` fails the tests that expect these positions
VIRTUAL_COLUMN_POSITIONS = range(4)

# the columns of the two unique indexes of ``jansPerson``, in the order the loader handles them
UNIQUE_INDEX_COLUMNS = ("mail", "uid")
DEFAULT_POSTGRES_SCHEMA = "public"


def _quote_like_library(dialect):
    """Quote an identifier as ``SqlClient.quoted_id`` does, doubling the quote character inside it."""
    quote = QUOTE_CHARACTER[dialect]
    return lambda identifier: f"{quote}{identifier.replace(quote, quote * 2)}{quote}"


def _client(dialect, server_version=(8, 0)):
    """A fake ``SqlClient`` on which every index lookup finds a valid index."""
    client = create_autospec(SqlClient, instance=True)
    client.dialect = dialect
    client.quoted_id.side_effect = _quote_like_library(dialect)
    client.get_server_version.return_value = server_version
    client.get_index_state.return_value = True
    return client


def _mysql57_client():
    """A fake MySQL 5.7 client on which every ``ALTER TABLE`` succeeds."""
    client = _client("mysql", server_version=(5, 7))
    client.alter_table.return_value = True
    return client


def _backend(client, sql_indexes):
    """An ``SQLBackend`` without ``__init__``, which reads the image's files under ``/app``."""
    backend = sql_setup.SQLBackend.__new__(sql_setup.SQLBackend)
    backend.client = client
    backend.sql_indexes = sql_indexes
    return backend


def _indexes(json_indexes=(), person_fields=(), person_custom=()):
    """The ``sql_indexes`` of a schema with the common JSON indexes and the ``jansPerson`` table."""
    return {
        "__common__": {"fields": [], "JSON": list(json_indexes)},
        "jansPerson": {"fields": list(person_fields), "custom": list(person_custom)},
    }


def _index_calls(client):
    """The positional arguments of every ``create_index`` call, in order."""
    return [made.args for made in client.create_index.call_args_list]


def _alter_calls(client):
    """The positional arguments of every ``alter_table`` call, in order."""
    return [made.args for made in client.alter_table.call_args_list]


def _schema_steps(client):
    """The ``alter_table`` and ``create_index`` calls in the order they were made, as ``(method name, arguments)``."""
    return [
        (method_name, args)
        for method_name, args, _kwargs in client.method_calls
        if method_name in ("alter_table", "create_index")
    ]


def _virtual_column_query(position):
    """The statement that adds the virtual column of a ``jansExtUid`` element, without an index."""
    return (
        "ALTER TABLE `jansPerson` "
        f"ADD COLUMN `jansExtUid_mem_idx_{position}` CHAR(128) AS (jansExtUid->'$.v[{position}]')"
    )


def _virtual_index_call(position):
    """The ``create_index`` arguments for the named index of that virtual column."""
    name = f"jansExtUid_mem_idx_{position}"
    return (f"ALTER TABLE `jansPerson` ADD INDEX `{name}` (`{name}`)", "jansPerson", name)


def _column_then_index(position):
    """The two schema steps of one virtual column: add the column alone, then its named index."""
    return [
        ("alter_table", (_virtual_column_query(position), "jansPerson")),
        ("create_index", _virtual_index_call(position)),
    ]


def _unique_index_creations(dialect, *columns):
    """The ``create_index`` calls that build the unique indexes of these columns with a plain statement.

    A plain statement is the only argument: no table and no index name, so the library does not build it
    concurrently and does not give up when the table stays locked.
    """
    return [call(UNIQUE_INDEX_QUERIES[dialect][column]) for column in columns]


def _mysql57_backend(client):
    """A backend whose ``jansPerson`` table has the JSON column ``jansExtUid`` to index."""
    return _backend(client, _indexes(person_fields=["jansExtUid"]))


def _mysql_error(code):
    """The error SQLAlchemy raises for a MySQL error code that pymysql maps to ``OperationalError``."""
    return OperationalError("ALTER TABLE", {}, pymysql.err.OperationalError(code, "error text"))


def _unique_index_name(column):
    """The name of the unique index of a ``jansPerson`` column."""
    return f"jansperson_{column}_unique_idx"


def _lookup_states_with_invalid(invalid_column, other_state):
    """The index states that the two lookups return, in order, when only ``invalid_column`` is invalid."""
    return [False if column == invalid_column else other_state for column in UNIQUE_INDEX_COLUMNS]


def _other_column(column):
    """The unique-index column that is not ``column``."""
    return next(other for other in UNIQUE_INDEX_COLUMNS if other != column)


def _interface_error():
    """A ``DBAPIError`` that is not a ``DatabaseError``: the driver reports a closed connection."""
    return InterfaceError("SELECT 1", {}, pymysql.err.InterfaceError(0, "connection already closed"))


def _create_mysql57_indexes(backend, existing_columns=()):
    """Run the MySQL index step for ``jansPerson`` with a JSON column and the given virtual columns present."""
    column_mapping = {"doc_id": "VARCHAR(64)", "jansExtUid": "JSON"}
    column_mapping.update({column: "CHAR(128)" for column in existing_columns})
    backend.create_mysql_indexes("jansPerson", column_mapping)


@pytest.fixture
def loader_log(caplog):
    """``caplog`` with the warnings and errors of the loader's logger, which does not propagate to the root logger."""
    logger = logging.getLogger("persistence-loader")
    logger.addHandler(caplog.handler)
    caplog.set_level(logging.WARNING, logger="persistence-loader")
    yield caplog
    logger.removeHandler(caplog.handler)


@pytest.fixture
def postgres_schema(monkeypatch):
    """Make ``resolve_db_schema_name`` return the default schema, and return a function that selects another one.

    The loader reads the schema from the environment (``CN_SQL_DB_DIALECT`` and ``CN_SQL_DB_SCHEMA``), so the
    fixture sets those variables through ``monkeypatch`` instead of replacing ``sql_setup.resolve_db_schema_name``:
    the tests then also run the library's real resolution, and keep passing if the loader imports the function
    in another way. Without it a variable of the machine could change the expected name.
    """
    monkeypatch.setenv("CN_SQL_DB_DIALECT", "pgsql")
    monkeypatch.delenv("CN_SQL_DB_SCHEMA", raising=False)

    def use_schema(schema_name):
        monkeypatch.setenv("CN_SQL_DB_SCHEMA", schema_name)

    return use_schema


def _error_records(log):
    """The records of the loader's log at ERROR level."""
    return [record for record in log.records if record.levelno == logging.ERROR]


def _warning_records(log):
    """The records of the loader's log at WARNING level."""
    return [record for record in log.records if record.levelno == logging.WARNING]


def _mentions(word, message):
    """Whether ``message`` has ``word`` as a word of its own (``uid`` is not found in ``jansperson_uid_unique_idx``)."""
    return re.search(rf"\b{re.escape(word)}\b", message) is not None


parametrize_lookup_errors = pytest.mark.parametrize("make_lookup_error", [
    pytest.param(
        lambda: IndexLockTimeoutError("timed out waiting for a lock to look up index jansperson_mail_unique_idx"),
        id="lock-timeout",
    ),
    pytest.param(lambda: _mysql_error(MYSQL_LOST_CONNECTION), id="database-error"),
    pytest.param(_interface_error, id="interface-error"),
])

parametrize_invalid_column = pytest.mark.parametrize("invalid_column", [
    pytest.param("mail", id="mail-invalid-uid-valid"),
    pytest.param("uid", id="uid-invalid-mail-valid"),
])


@pytest.mark.parametrize("column_type, expected_query", [
    pytest.param("TEXT", "CREATE INDEX `jansPerson_displayName` ON `jansPerson` (`displayName` (255))", id="text"),
    pytest.param("VARCHAR(64)", "CREATE INDEX `jansPerson_displayName` ON `jansPerson` (`displayName`)", id="varchar"),
])
def test_mysql_column_index_passes_table_and_index_name(column_type, expected_query):
    client = _client("mysql")
    backend = _backend(client, _indexes(person_fields=["displayName"]))

    backend.create_mysql_indexes("jansPerson", {"doc_id": "VARCHAR(64)", "displayName": column_type})

    assert _index_calls(client) == [(expected_query, "jansPerson", "jansPerson_displayName")]


def test_mysql8_json_indexes_pass_table_and_index_name():
    client = _client("mysql", server_version=(8, 0))
    backend = _backend(client, _indexes(json_indexes=MYSQL_JSON_INDEXES, person_fields=["jansExtUid"]))

    backend.create_mysql_indexes("jansPerson", {"doc_id": "VARCHAR(64)", "jansExtUid": "JSON"})

    assert _index_calls(client) == [
        (
            "ALTER TABLE `jansPerson` ADD INDEX `jansPerson_json_1` ((CAST(jansExtUid->'$.v' AS CHAR(128) ARRAY)))",
            "jansPerson", "jansPerson_json_1",
        ),
        (
            "ALTER TABLE `jansPerson` ADD INDEX `jansPerson_json_2` ((CAST(jansExtUid->'$.v[0]' AS CHAR(128))))",
            "jansPerson", "jansPerson_json_2",
        ),
        (
            "ALTER TABLE `jansPerson` ADD INDEX `jansPerson_json_3` ((CAST(jansExtUid->'$.v[1]' AS CHAR(128))))",
            "jansPerson", "jansPerson_json_3",
        ),
        (
            "ALTER TABLE `jansPerson` ADD INDEX `jansPerson_json_4` ((CAST(jansExtUid->'$.v[2]' AS CHAR(128))))",
            "jansPerson", "jansPerson_json_4",
        ),
    ]


def test_mysql_custom_indexes_pass_table_and_index_name():
    client = _client("mysql", server_version=(8, 0))
    backend = _backend(client, _indexes(person_custom=["(lower(`uid`))", "(lower(`mail`))"]))

    backend.create_mysql_indexes("jansPerson", {"doc_id": "VARCHAR(64)"})

    assert _index_calls(client) == [
        ("CREATE INDEX `jansPerson_CustomIdx1` ON `jansPerson` ((lower(`uid`)))", "jansPerson", "jansPerson_CustomIdx1"),
        ("CREATE INDEX `jansPerson_CustomIdx2` ON `jansPerson` ((lower(`mail`)))", "jansPerson", "jansPerson_CustomIdx2"),
    ]


@pytest.mark.parametrize("column_name, expected_index_name", [
    pytest.param("uid", "jansPerson_uid", id="plain-name"),
    pytest.param("ext-uid.v2", "jansPerson_ext_uid_v2", id="name-with-punctuation"),
])
def test_pgsql_column_index_passes_table_and_index_name(column_name, expected_index_name):
    client = _client("pgsql")
    backend = _backend(client, _indexes(person_fields=[column_name]))

    backend.create_pgsql_indexes("jansPerson", {"doc_id": "VARCHAR(64)", column_name: "VARCHAR(64)"})

    expected_query = f'CREATE INDEX "{expected_index_name}" ON "jansPerson" ("{column_name}")'
    assert _index_calls(client) == [(expected_query, "jansPerson", expected_index_name)]


def test_pgsql_json_index_passes_table_and_index_name():
    client = _client("pgsql")
    backend = _backend(client, _indexes(json_indexes=PGSQL_JSON_INDEXES, person_fields=["jansExtUid"]))

    backend.create_pgsql_indexes("jansPerson", {"doc_id": "VARCHAR(64)", "jansExtUid": "JSONB"})

    expected_query = (
        'CREATE INDEX "jansPerson_json_1" ON "jansPerson" USING GIN (JSONB_PATH_QUERY_ARRAY("jansExtUid", \'$[*]\'))'
    )
    assert _index_calls(client) == [(expected_query, "jansPerson", "jansPerson_json_1")]


def test_pgsql_custom_indexes_pass_table_and_index_name():
    client = _client("pgsql")
    backend = _backend(client, _indexes(person_custom=['(lower("uid"))', '(lower("mail"))']))

    backend.create_pgsql_indexes("jansPerson", {"doc_id": "VARCHAR(64)"})

    assert _index_calls(client) == [
        ('CREATE INDEX "jansPerson_custom_1" ON "jansPerson" (lower("uid"))', "jansPerson", "jansPerson_custom_1"),
        ('CREATE INDEX "jansPerson_custom_2" ON "jansPerson" (lower("mail"))', "jansPerson", "jansPerson_custom_2"),
    ]


@pytest.mark.parametrize("dialect", [pytest.param("mysql", id="mysql"), pytest.param("pgsql", id="pgsql")])
def test_existing_unique_indexes_send_no_statement(dialect):
    client = _client(dialect)
    backend = _backend(client, _indexes())

    backend.create_unique_indexes()

    assert client.create_index.call_args_list == []


@pytest.mark.parametrize("dialect", [pytest.param("mysql", id="mysql"), pytest.param("pgsql", id="pgsql")])
def test_missing_unique_indexes_are_created_with_a_plain_statement(dialect):
    client = _client(dialect)
    client.get_index_state.return_value = None
    backend = _backend(client, _indexes())

    backend.create_unique_indexes()

    assert client.create_index.call_args_list == _unique_index_creations(dialect, "mail", "uid")


def test_unique_index_lookup_names_table_and_index():
    client = _client("pgsql")
    backend = _backend(client, _indexes())

    backend.create_unique_indexes()

    assert [made.args for made in client.get_index_state.call_args_list] == [
        ("jansPerson", "jansperson_mail_unique_idx"),
        ("jansPerson", "jansperson_uid_unique_idx"),
    ]


def test_unique_index_lookup_waits_at_most_10_seconds_for_a_lock():
    # the value 10 is pinned on purpose: it bounds how long a start can wait for the lookup
    client = _client("pgsql")
    backend = _backend(client, _indexes())

    backend.create_unique_indexes()

    assert [made.kwargs for made in client.get_index_state.call_args_list] == [
        {"lock_timeout": 10},
        {"lock_timeout": 10},
    ]


@pytest.mark.parametrize("mail_then_uid_lookup, missing_column", [
    pytest.param([True, None], "uid", id="uid-missing"),
    pytest.param([None, True], "mail", id="mail-missing"),
])
def test_mail_and_uid_unique_indexes_are_looked_up_independently(mail_then_uid_lookup, missing_column):
    client = _client("pgsql")
    client.get_index_state.side_effect = mail_then_uid_lookup
    backend = _backend(client, _indexes())

    backend.create_unique_indexes()

    assert client.create_index.call_args_list == _unique_index_creations("pgsql", missing_column)


@parametrize_lookup_errors
def test_unique_index_whose_lookup_fails_is_created_with_a_plain_statement(make_lookup_error):
    client = _client("mysql", server_version=(5, 7))
    client.get_index_state.side_effect = make_lookup_error()
    backend = _backend(client, _indexes())

    backend.create_unique_indexes()

    assert client.create_index.call_args_list == _unique_index_creations("mysql", "mail", "uid")


@parametrize_lookup_errors
def test_unique_index_whose_lookup_fails_is_logged_once_as_a_warning(make_lookup_error, loader_log):
    client = _client("mysql", server_version=(5, 7))
    client.get_index_state.side_effect = [make_lookup_error(), True]
    backend = _backend(client, _indexes())

    backend.create_unique_indexes()

    assert [record.levelname for record in loader_log.records] == ["WARNING"]


@parametrize_lookup_errors
def test_unique_index_lookup_warning_names_index_table_and_error(make_lookup_error, loader_log):
    client = _client("mysql", server_version=(5, 7))
    lookup_error = make_lookup_error()
    client.get_index_state.side_effect = [lookup_error, True]
    backend = _backend(client, _indexes())

    backend.create_unique_indexes()

    (warning,) = _warning_records(loader_log)
    message = warning.getMessage()
    assert "jansperson_mail_unique_idx" in message
    assert "jansPerson" in message
    assert str(lookup_error) in message


@pytest.mark.parametrize("operator_text", [
    pytest.param("plain unique-index statement", id="plain-statement"),
    pytest.param("waits for the table lock", id="waits-for-the-table-lock"),
])
def test_unique_index_lookup_warning_says_how_the_index_is_created(operator_text, loader_log):
    client = _client("mysql", server_version=(5, 7))
    client.get_index_state.side_effect = [IndexLockTimeoutError("timed out"), True]
    backend = _backend(client, _indexes())

    backend.create_unique_indexes()

    (warning,) = _warning_records(loader_log)
    assert operator_text in warning.getMessage()


def test_unique_index_lookup_error_that_is_not_a_database_error_is_not_swallowed():
    client = _client("pgsql")
    non_database_error = ValueError("not a database error")
    client.get_index_state.side_effect = non_database_error
    backend = _backend(client, _indexes())

    with pytest.raises(ValueError) as raised:
        backend.create_unique_indexes()

    assert raised.value is non_database_error


@pytest.mark.parametrize("mail_then_uid_lookup, expected_built_columns", [
    pytest.param([False, True], [], id="mail-invalid-uid-valid"),
    pytest.param([False, None], ["uid"], id="mail-invalid-uid-missing"),
    pytest.param([True, False], [], id="uid-invalid-mail-valid"),
    pytest.param([None, False], ["mail"], id="uid-invalid-mail-missing"),
])
def test_invalid_unique_index_gets_no_statement_and_the_start_continues(
    mail_then_uid_lookup, expected_built_columns, postgres_schema,
):
    client = _client("pgsql")
    client.get_index_state.side_effect = mail_then_uid_lookup
    backend = _backend(client, _indexes())

    backend.create_unique_indexes()

    assert client.create_index.call_args_list == _unique_index_creations("pgsql", *expected_built_columns)


@parametrize_invalid_column
def test_invalid_unique_index_is_logged_once_at_error_level(invalid_column, loader_log, postgres_schema):
    client = _client("pgsql")
    client.get_index_state.side_effect = _lookup_states_with_invalid(invalid_column, True)
    backend = _backend(client, _indexes())

    backend.create_unique_indexes()

    assert [record.levelname for record in loader_log.records] == ["ERROR"]


@pytest.mark.parametrize("other_state", [pytest.param(True, id="valid"), pytest.param(None, id="missing")])
def test_unique_indexes_that_are_valid_or_missing_log_no_error(other_state, loader_log, postgres_schema):
    client = _client("pgsql")
    client.get_index_state.side_effect = [other_state, other_state]
    backend = _backend(client, _indexes())

    backend.create_unique_indexes()

    assert _error_records(loader_log) == []


@parametrize_invalid_column
def test_invalid_unique_index_error_names_index_table_and_column(invalid_column, loader_log, postgres_schema):
    client = _client("pgsql")
    client.get_index_state.side_effect = _lookup_states_with_invalid(invalid_column, True)
    backend = _backend(client, _indexes())

    backend.create_unique_indexes()

    (error,) = _error_records(loader_log)
    message = error.getMessage()
    assert _unique_index_name(invalid_column) in message
    assert _unique_index_name(_other_column(invalid_column)) not in message
    assert "jansPerson" in message
    assert _mentions(invalid_column, message)
    assert not _mentions(_other_column(invalid_column), message)


def _invalid_index_error_message(invalid_column, loader_log):
    """Run the unique-index step with ``invalid_column``'s index invalid and return the one ERROR message."""
    client = _client("pgsql")
    client.get_index_state.side_effect = _lookup_states_with_invalid(invalid_column, True)
    backend = _backend(client, _indexes())

    backend.create_unique_indexes()

    (error,) = _error_records(loader_log)
    return error.getMessage()


@parametrize_invalid_column
def test_invalid_unique_index_error_gives_the_drop_statement_of_the_default_schema_when_the_variable_is_unset(
    invalid_column, loader_log, postgres_schema,
):
    message = _invalid_index_error_message(invalid_column, loader_log)

    assert f'DROP INDEX CONCURRENTLY "public"."{_unique_index_name(invalid_column)}"' in message


@pytest.mark.parametrize("schema_name, expected_quoted_schema", [
    pytest.param("jans_app", '"jans_app"', id="custom-schema"),
    pytest.param('odd"schema', '"odd""schema"', id="schema-with-quote"),
])
@parametrize_invalid_column
def test_invalid_unique_index_error_gives_the_drop_statement_of_the_configured_schema(
    invalid_column, schema_name, expected_quoted_schema, loader_log, postgres_schema,
):
    postgres_schema(schema_name)

    message = _invalid_index_error_message(invalid_column, loader_log)

    assert f'DROP INDEX CONCURRENTLY {expected_quoted_schema}."{_unique_index_name(invalid_column)}"' in message


@parametrize_invalid_column
def test_invalid_unique_index_error_says_the_build_may_still_be_running(invalid_column, loader_log, postgres_schema):
    message = _invalid_index_error_message(invalid_column, loader_log)

    assert "still running" in message


@parametrize_invalid_column
def test_invalid_unique_index_error_says_duplicate_values_of_the_column_may_be_accepted(
    invalid_column, loader_log, postgres_schema,
):
    message = _invalid_index_error_message(invalid_column, loader_log)

    assert f"duplicate {invalid_column} values may be accepted" in message


@parametrize_invalid_column
def test_invalid_unique_index_error_tells_to_remove_duplicate_values_of_the_column(
    invalid_column, loader_log, postgres_schema,
):
    message = _invalid_index_error_message(invalid_column, loader_log)

    assert f"remove duplicate {invalid_column} values" in message
    assert f"remove duplicate {_other_column(invalid_column)} values" not in message


@parametrize_invalid_column
def test_invalid_unique_index_error_makes_the_drop_conditional_on_no_build_running(
    invalid_column, loader_log, postgres_schema,
):
    message = _invalid_index_error_message(invalid_column, loader_log)

    assert "If no build of it is running" in message
    assert message.index("If no build of it is running") < message.index("DROP INDEX CONCURRENTLY")


@pytest.mark.parametrize("view_name", [
    pytest.param("pg_stat_progress_create_index", id="progress-view"),
    pytest.param("pg_stat_activity", id="activity-view"),
])
@parametrize_invalid_column
def test_invalid_unique_index_error_names_the_view_that_shows_a_running_build(
    invalid_column, view_name, loader_log, postgres_schema,
):
    message = _invalid_index_error_message(invalid_column, loader_log)

    assert view_name in message


@parametrize_invalid_column
def test_invalid_unique_index_error_tells_to_start_the_loader_again_after_the_drop(
    invalid_column, loader_log, postgres_schema,
):
    message = _invalid_index_error_message(invalid_column, loader_log)

    assert "start the loader again" in message
    assert message.index("DROP INDEX CONCURRENTLY") < message.index("start the loader again")


@parametrize_invalid_column
def test_invalid_unique_index_error_says_the_loader_leaves_the_index_alone_and_continues(
    invalid_column, loader_log, postgres_schema,
):
    message = _invalid_index_error_message(invalid_column, loader_log)

    assert "leaves the index alone and continues" in message


def test_mysql57_adds_missing_virtual_column_alone_then_named_index():
    client = _mysql57_client()
    backend = _mysql57_backend(client)

    _create_mysql57_indexes(backend)

    assert _schema_steps(client) == [step for position in VIRTUAL_COLUMN_POSITIONS for step in _column_then_index(position)]


def test_mysql57_skips_index_whose_virtual_column_could_not_be_added():
    client = _mysql57_client()
    client.alter_table.side_effect = [False] + [True] * (len(VIRTUAL_COLUMN_POSITIONS) - 1)
    backend = _mysql57_backend(client)

    _create_mysql57_indexes(backend)

    assert _index_calls(client) == [_virtual_index_call(position) for position in VIRTUAL_COLUMN_POSITIONS[1:]]


def test_mysql57_warns_once_naming_the_virtual_column_and_the_table_that_could_not_be_added(loader_log):
    client = _mysql57_client()
    client.alter_table.side_effect = [False] + [True] * (len(VIRTUAL_COLUMN_POSITIONS) - 1)
    backend = _mysql57_backend(client)

    _create_mysql57_indexes(backend)

    assert [record.levelname for record in loader_log.records] == ["WARNING"]
    message = loader_log.records[0].getMessage()
    assert "jansExtUid_mem_idx_0" in message
    assert "jansPerson" in message


@pytest.mark.parametrize("operator_text", [
    pytest.param("its index is skipped", id="index-skipped"),
    pytest.param("the next start retries", id="next-start-retries"),
])
def test_mysql57_virtual_column_warning_says_what_happens_to_the_index(operator_text, loader_log):
    client = _mysql57_client()
    client.alter_table.side_effect = [False] + [True] * (len(VIRTUAL_COLUMN_POSITIONS) - 1)
    backend = _mysql57_backend(client)

    _create_mysql57_indexes(backend)

    (warning,) = _warning_records(loader_log)
    assert operator_text in warning.getMessage()


def test_mysql57_creates_index_when_another_loader_added_the_column():
    client = _mysql57_client()
    client.alter_table.side_effect = _mysql_error(MYSQL_DUPLICATE_COLUMN)
    backend = _mysql57_backend(client)

    _create_mysql57_indexes(backend)

    assert _index_calls(client) == [_virtual_index_call(position) for position in VIRTUAL_COLUMN_POSITIONS]


@pytest.mark.parametrize("error_code", [
    pytest.param(MYSQL_UNKNOWN_COLUMN, id="unknown-column"),
    pytest.param(MYSQL_DEADLOCK, id="deadlock"),
    pytest.param(MYSQL_DUPLICATE_KEY_NAME, id="duplicate-key-name"),
])
def test_mysql57_raises_other_virtual_column_errors(error_code):
    client = _mysql57_client()
    column_error = _mysql_error(error_code)
    client.alter_table.side_effect = column_error
    backend = _mysql57_backend(client)

    with pytest.raises(OperationalError) as raised:
        _create_mysql57_indexes(backend)

    assert raised.value is column_error


@pytest.mark.parametrize("make_error", [
    pytest.param(lambda: OperationalError("ALTER TABLE", {}, None), id="no-driver-error"),
    pytest.param(
        lambda: OperationalError("ALTER TABLE", {}, pymysql.err.OperationalError()),
        id="driver-error-without-args",
    ),
])
def test_mysql57_raises_operational_error_without_error_code(make_error):
    client = _mysql57_client()
    column_error = make_error()
    client.alter_table.side_effect = column_error
    backend = _mysql57_backend(client)

    with pytest.raises(OperationalError) as raised:
        _create_mysql57_indexes(backend)

    assert raised.value is column_error
    assert _index_calls(client) == []


def test_mysql57_existing_virtual_column_is_not_added_again():
    client = _mysql57_client()
    backend = _mysql57_backend(client)

    _create_mysql57_indexes(backend, existing_columns=["jansExtUid_mem_idx_0", "jansExtUid_mem_idx_2"])

    assert _alter_calls(client) == [(_virtual_column_query(1), "jansPerson"), (_virtual_column_query(3), "jansPerson")]


def test_mysql57_existing_virtual_column_still_gets_its_named_index():
    client = _mysql57_client()
    backend = _mysql57_backend(client)

    _create_mysql57_indexes(backend, existing_columns=["jansExtUid_mem_idx_0", "jansExtUid_mem_idx_2"])

    assert _index_calls(client) == [_virtual_index_call(position) for position in VIRTUAL_COLUMN_POSITIONS]
