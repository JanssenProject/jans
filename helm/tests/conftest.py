import pytest


@pytest.fixture()
def settings():
    from pygluu.kubernetes.settings import ValuesHandler, unlink_values_yaml

    handler = ValuesHandler()
    yield handler
    unlink_values_yaml()
