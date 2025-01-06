"""
test_core
~~~~~~~~~
This module consists of testcases for :module:`core` module.
"""


def test_core_app():
    """Test core app is a Flask app."""
    from main.core import app

    assert hasattr(app, "app_context")
