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
from pathlib import Path
from main.logger import logger


def get_instance_path(parent_dir=""):
    parent_dir = parent_dir or Path.home()
    instance_path = Path(parent_dir).joinpath(".cloud")
    instance_path.mkdir(parents=True, exist_ok=True)
    return instance_path.resolve()


def parse_bool_env(var_name: str, default: str = "False") -> bool:
    """Parse boolean environment variable."""
    value = os.getenv(var_name, default)
    return value.lower() in ("true", "1", "yes")



class BaseConfig:
    API_TITLE = "Cedarling Sidecar"
    API_VERSION = "v1"
    OPENAPI_VERSION = "3.0.0"
    OPENAPI_JSON_PATH = "openapi.json"
    OPENAPI_URL_PREFIX = ""
    OPENAPI_SWAGGER_UI_PATH = "/swagger-ui"
    OPENAPI_SWAGGER_UI_URL = "https://cdn.jsdelivr.net/npm/swagger-ui-dist/"
    API_SPEC_OPTIONS = {
        "x-internal-id": "1",
    }

    @classmethod
    def load_bootstrap(cls) -> None:
        path = os.getenv("CEDARLING_BOOTSTRAP_CONFIG_FILE")
        if not path or path == "None":
            raise RuntimeError("CEDARLING_BOOTSTRAP_CONFIG_FILE is not set")
        try:
            with open(path) as f:
                cls.CEDARLING_BOOTSTRAP_CONFIG = f.read()
        except OSError as e:
            logger.exception("Unable to read Cedarling bootstrap config from %s", path)
            raise RuntimeError(
                f"Cannot read CEDARLING_BOOTSTRAP_CONFIG_FILE={path}"
            ) from e
        cls.CEDARLING_BOOTSTRAP_CONFIG_FILE = path

    CEDARLING_BOOTSTRAP_CONFIG_FILE = None
    CEDARLING_BOOTSTRAP_CONFIG = None

    SIDECAR_DEBUG_RESPONSE = parse_bool_env("SIDECAR_DEBUG_RESPONSE")


class TestingConfig(BaseConfig):
    TESTING = True
    DEBUG = True


class DevelopmentConfig(BaseConfig):
    DEVELOPMENT = True
    DEBUG = True


class ProductionConfig(BaseConfig):
    DEVELOPMENT = False


config = {
    "testing": TestingConfig,
    "default": TestingConfig,
    "development": DevelopmentConfig,
    "production": ProductionConfig,
}


class ConfigLoader:
    @staticmethod
    def set_config():
        mode = os.environ.get("APP_MODE")
        if mode is not None:
            logger.info(f"Loads {mode} config")
            current_config = config.get(mode, TestingConfig)
            current_config.load_bootstrap()
            return current_config
        return config.get("default")
