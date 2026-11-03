from pathlib import Path
from unittest.mock import patch

from gunicorn.config import Config

from main import gunicorn_conf
from main.extensions import cedarling

ENTRYPOINT = Path(__file__).parents[2] / "docker-entrypoint.sh"


def test_worker_exit_shuts_down_cedarling():
    config = Config()
    config.set("worker_exit", gunicorn_conf.worker_exit)

    with patch.object(cedarling, "shut_down") as shut_down:
        config.worker_exit(None, None)

    shut_down.assert_called_once_with()


def test_entrypoint_loads_hooks_and_graceful_timeout():
    entrypoint = ENTRYPOINT.read_text()

    assert "-c python:main.gunicorn_conf" in entrypoint
    assert '--graceful-timeout "$GUNICORN_GRACEFUL_TIMEOUT"' in entrypoint
