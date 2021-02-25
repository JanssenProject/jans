import pytest


@pytest.mark.parametrize("type_", [
    "ldap",
    "couchbase",
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


@pytest.mark.parametrize("mapping", [
    "default",
    "user",
    "site",
    "cache",
    "token",
    "session",
])
def test_validate_persistence_ldap_mapping(mapping):
    from jans.pycloudlib.validators import validate_persistence_ldap_mapping
    assert validate_persistence_ldap_mapping("hybrid", mapping) is None


def test_validate_persistence_ldap_mapping_invalid():
    from jans.pycloudlib.validators import validate_persistence_ldap_mapping

    with pytest.raises(ValueError):
        validate_persistence_ldap_mapping("hybrid", "random")
