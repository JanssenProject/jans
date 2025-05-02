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

from flask import Flask
from main.config import ConfigLoader, get_instance_path
from main.extensions import api, cors, cedarling
from main.logger import logger
from main.extensions.routes_extension import register_routes

def create_app():
    app = Flask(
            __name__,
            instance_path=str(get_instance_path()),
            instance_relative_config=True,
            subdomain_matching=True,
            static_folder=None
        )
    
    app.config.from_object(ConfigLoader.set_config())
    api.init_app(app)
    cors.init_app(app, resources=r"/cedarling")
    try:
        cedarling.init_app(app)
    except Exception as e:
        logger.warning(f"Exception during initializing cedarling: {e}")
        exit(1)
    register_routes(api)
    return app
