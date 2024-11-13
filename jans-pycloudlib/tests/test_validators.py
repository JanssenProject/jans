import pytest


@pytest.mark.parametrize("type_", [
    "hybrid",
    "sql",
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


def test_validate_persistence_hybrid_mapping(monkeypatch):
    from jans.pycloudlib.validators import validate_persistence_hybrid_mapping

    monkeypatch.setattr(
        "jans.pycloudlib.persistence.utils.PersistenceMapper.validate_hybrid_mapping",
        lambda cls: True,
    )

    # asserts PersistenceMapper.validate_hybrid_mapping is called
    assert validate_persistence_hybrid_mapping() is None
