from flask import Flask
from main.config import ConfigLoader, get_instance_path
from main.extensions import api, cors
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
    register_routes(api)
    return app
