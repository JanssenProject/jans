import pytest


@pytest.mark.parametrize("type_", [
    "ldap",
    "couchbase",
    "hybrid",
    "sql",
    "spanner",
])
def test_validate_persistence_type(type_):
    from jans.pycloudlib.validators import validate_persistence_type
    assert validate_persistence_type(type_) is None


def test_validate_persistence_type_invalid():
    from jans.pycloudlib.validators import validate_persistence_type

    with pytest.raises(ValueError):
        validate_persistence_type("random")


@pytest.mark.parametrize("dialect", [
    "mysql",
    # "pgsql",
])
def test_validate_persistence_sql_dialect(dialect):
    from jans.pycloudlib.validators import validate_persistence_sql_dialect

    assert validate_persistence_sql_dialect(dialect) is None


def test_validate_persistence_sql_dialect_invalid():
    from jans.pycloudlib.validators import validate_persistence_sql_dialect

    with pytest.raises(ValueError):
        validate_persistence_sql_dialect("random")


@pytest.mark.parametrize("mapping", [
    "ab",
    1,
    "1",
    "{}",
    '{"user": "sql"}',  # missing remaining keys
    '{"default": "sql", "user": "spanner", "cache": "ldap", "site": "couchbase", "token": "sql", "session": "random"}'
])
def test_validate_persistence_hybrid_mapping_invalid(mapping):
    from jans.pycloudlib.validators import validate_persistence_hybrid_mapping

    with pytest.raises(ValueError):
        validate_persistence_hybrid_mapping(mapping)
