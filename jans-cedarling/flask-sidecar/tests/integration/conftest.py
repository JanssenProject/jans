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
import json

current_path = os.path.dirname(os.path.realpath(__file__))
BOOTSTRAP_CONFIG_FILE = os.path.join(current_path, "cedarling_test_config.json")
POLICY_STORE_PATH = os.path.join(current_path, "policy-store-multi-issuer")


@pytest.fixture(scope="session")
def bootstrap_config_file(tmp_path_factory):
    with open(BOOTSTRAP_CONFIG_FILE, "r") as f:
        config = json.loads(f.read())
    config["CEDARLING_POLICY_STORE_LOCAL_FN"] = POLICY_STORE_PATH
    config_file = tmp_path_factory.mktemp("cedarling") / "cedarling_test_config.json"
    with open(config_file, "w") as f:
        f.write(json.dumps(config))
    return config_file


@pytest.fixture(autouse=True)
def env_setup(monkeypatch):
    monkeypatch.setenv("APP_MODE", "testing")


@pytest.fixture(scope="session")
def app(bootstrap_config_file):
    import os

    from main.app import create_app

    os.environ["APP_MODE"] = "testing"
    os.environ["CEDARLING_BOOTSTRAP_CONFIG_FILE"] = str(bootstrap_config_file)
    app = create_app()
    ctx = app.app_context()
    ctx.push()
    yield app
    ctx.pop()


@pytest.fixture(scope="session")
def cedarling_instance(app):
    return app.extensions["cedarling_client"]
