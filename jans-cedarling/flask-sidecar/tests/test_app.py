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

test_app
~~~~~~~~
This module consists of testcases for :module:`main.app` module.
"""
import os

import pytest


@pytest.mark.parametrize(
    "mode",
    [
        ("development"),
        ("testing"),
        ("production"),
        ("dummy"),
    ],
)
def test_create_app(mode):
    """Test factory function that creates Flask's app."""
    from main.app import create_app
    os.environ["APP_MODE"] = mode
    config = {
        "testing": {
            "debug": True,
            "testing": True
        },
        "development": {
            "debug": True,
            "testing": False
        },
        "production": {
            "debug": False,
            "testing": False
        }
    }
    loaded_config = config.get(mode, config.get("testing"))
    app = create_app()
    assert loaded_config is not None
    assert loaded_config.get("debug", None) is not None
    assert loaded_config.get("testing", None) is not None
    assert app.config["DEBUG"] is loaded_config["debug"]
    assert app.config["TESTING"] is loaded_config["testing"]
    # Flask app has ``app_context`` attr
    assert hasattr(app, "app_context")
