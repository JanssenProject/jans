"""
Copyright (c) 2025, Gluu, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
"""

import os

import pytest
from main.extensions import cedarling
from unittest.mock import patch

current_path = os.path.dirname(os.path.realpath(__file__))


@pytest.fixture(autouse=True)
def env_setup(monkeypatch):
    monkeypatch.setenv("APP_MODE", "testing")
    monkeypatch.setenv(
        "CEDARLING_BOOTSTRAP_CONFIG_FILE",
        os.path.join(current_path, "cedarling_test_config.json"),
    )
    monkeypatch.setattr(cedarling, "initialize_cedarling", lambda: None)


@pytest.fixture(scope="session")
def app():
    import os

    from main.app import create_app

    os.environ["APP_MODE"] = "testing"
    os.environ["CEDARLING_BOOTSTRAP_CONFIG_FILE"] = os.path.join(
        current_path, "cedarling_test_config.json"
    )
    with patch.object(cedarling, "init_app", return_value=None):
        app = create_app()
    ctx = app.app_context()
    ctx.push()
    yield app
    ctx.pop()


@pytest.fixture(scope="session")
def client(app):
    return app.test_client()


@pytest.fixture
def mock_json_input():
    return {
        "subject": {
            "type": "JWT",
            "id": "random_id",
            "properties": {
                "tokens": [
                    {
                        "mapping": "Jans::Access_token",
                        "payload": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0.KMUFsIDTnFmyG3nMiGM6H9FNFUROf3wh7SmqJp-QV30",
                    }
                ]
            },
        },
        "action": {
            "name": 'Jans::Action::"GET"',
        },
        "resource": {
            "type": "property",
            "id": "random_id",
            "properties": {
                "cedar_entity_mapping": {
                    "entity_type": "Jans::Application",
                    "id": "random_id",
                }
            },
        },
    }
