from cedarling_python import BootstrapConfig
from cedarling_python import Cedarling
from flask import Flask
import json

class CedarlingInstance:

    def __init__(self, app=None):
        self._bootstrap_config: str
        self._cedarling: Cedarling
        if app is not None:
            self.init_app(app)

    def init_app(self, app: Flask):
        self._bootstrap_config = app.config.get("CEDARLING_BOOTSTRAP_CONFIG", "{}")
        app.extensions = getattr(app, "extensions", {})
        app.extensions["cedarling_client"] = self
        self.initialize_cedarling()

    def initialize_cedarling(self):
        bootstrap_dict = json.loads(self._bootstrap_config)
        bootstrap_instance = BootstrapConfig(bootstrap_dict)
        self._cedarling = Cedarling(bootstrap_instance)

    def get_cedarling_instance(self) -> Cedarling:
        return self._cedarling
