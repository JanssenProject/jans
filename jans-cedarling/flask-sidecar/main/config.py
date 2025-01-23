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
    CEDARLING_BOOTSTRAP_CONFIG_FILE = os.getenv(
        "CEDARLING_BOOTSTRAP_CONFIG_FILE", None)
    if CEDARLING_BOOTSTRAP_CONFIG_FILE is None:
        logger.warning("Cedarling bootstrap file not found")
        exit()
    with open(CEDARLING_BOOTSTRAP_CONFIG_FILE, "r") as f:
        CEDARLING_BOOTSTRAP_CONFIG = f.read()
    SIDECAR_DEBUG_RESPONSE = os.getenv("SIDECAR_DEBUG_RESPONSE", "False")
    if SIDECAR_DEBUG_RESPONSE == "True":
        SIDECAR_DEBUG_RESPONSE = True
    else:
        SIDECAR_DEBUG_RESPONSE = False


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
    "production": ProductionConfig
}


class ConfigLoader:

    @staticmethod
    def set_config():
        mode = os.environ.get("APP_MODE")
        if mode is not None:
            print(f"INFO: loads {mode} config")
            return config.get(mode, TestingConfig)
        return config.get("default")
