from main.v1.resource import blp as evaluation_routes

def register_routes(app):
    app.register_blueprint(evaluation_routes, url_prefix="/cedarling")
