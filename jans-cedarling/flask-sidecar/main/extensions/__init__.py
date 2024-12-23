from flask_smorest import Api, Blueprint
from flask_cors import CORS 
from main.base.cedarling.cedarling import CedarlingInstance

class BlueprintApi(Blueprint):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)

    @staticmethod
    def _prepare_response_content(data):
        if data is not None:
            return data
        return None

api = Api()
cors = CORS(expose_headers="X-Pagination")
cedarling = CedarlingInstance()
