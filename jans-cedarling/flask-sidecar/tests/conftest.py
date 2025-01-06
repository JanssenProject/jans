import os

import pytest
from main.extensions import cedarling

current_path = os.path.dirname(os.path.realpath(__file__))


@pytest.fixture(autouse=True)
def env_setup(monkeypatch):
    monkeypatch.setenv("APP_MODE", "testing")
    monkeypatch.setenv("CEDARLING_BOOTSTRAP_CONFIG_FILE", os.path.join(current_path, "test_secrets", "cedarling_test_config.json"))
    monkeypatch.setattr(cedarling, "initialize_cedarling", lambda: None)

@pytest.fixture(scope="function")
def mock_cedarling(monkeypatch):
    pass
