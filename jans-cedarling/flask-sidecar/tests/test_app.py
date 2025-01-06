"""
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
