import pytest


def test_validate_persistence_type():
    from jans.pycloudlib.validators import validate_persistence_type
    from jans.pycloudlib.validators import ValidationError

    type_ = "random"

    with pytest.raises(ValidationError):
        validate_persistence_type(type_)


def test_validate_persistence_ldap_mapping():
    from jans.pycloudlib.validators import validate_persistence_ldap_mapping
    from jans.pycloudlib.validators import ValidationError

    type_ = "hybrid"
    mapping = "random"

    with pytest.raises(ValidationError):
        validate_persistence_ldap_mapping(type_, mapping)
