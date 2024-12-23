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
    CEDARLING_BOOTSTRAP_CONFIG_FILE = os.getenv("CEDARLING_BOOTSTRAP_CONFIG_FILE", None)
    if CEDARLING_BOOTSTRAP_CONFIG_FILE is None:
        logger.warning("Cedarling bootstrap file not found")
        exit()
    with open(CEDARLING_BOOTSTRAP_CONFIG_FILE, "r") as f:
        CEDARLING_BOOTSTRAP_CONFIG = f.read()

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
