from cedarling_python import BootstrapConfig
from cedarling_python import Cedarling
from cedarling_python import (
        ResourceData,
        Request,
        AuthorizeResultResponse,
        Tokens
)
from main.logger import logger
from flask import Flask
import json
import typing as _t

DictType = _t.Dict[str, _t.Any]
KEYS_LIST = ["access_token", "id_token", "userinfo_token"]

class CedarlingInstance:

    def __init__(self, app=None):
        self._bootstrap_config: str
        self._cedarling: Cedarling
        if app is not None:
            self.init_app(app)

    def init_app(self, app: Flask):
        self._bootstrap_config = app.config.get("CEDARLING_BOOTSTRAP_CONFIG", "{}")
        self.debug_response: bool = app.config.get("SIDECAR_DEBUG_RESPONSE", False)
        app.extensions = getattr(app, "extensions", {})
        app.extensions["cedarling_client"] = self
        self.initialize_cedarling()

    def initialize_cedarling(self):
        bootstrap_dict = json.loads(self._bootstrap_config)
        bootstrap_instance = BootstrapConfig(bootstrap_dict)
        self._cedarling = Cedarling(bootstrap_instance)

    def get_cedarling_instance(self) -> Cedarling:
        return self._cedarling

    def generate_resource(self, resource: DictType) -> ResourceData:
        resource_properties = resource.get("properties", {})
        resource_entity_dict = {
            "type": resource.get("type_field"),
            "id": resource.get("id")
        }
        for key in resource_properties.keys():
            resource_entity_dict[key] = resource_properties[key]
        resource_entity = ResourceData.from_dict(resource_entity_dict)
        return resource_entity

    def validate_subject(self, subject: DictType) -> bool:
        if "properties" not in subject:
            return False
        count = 0
        i = 0
        while count == 0 and i < len(KEYS_LIST):
            key = KEYS_LIST[i]
            if subject["properties"].get(key, None) is not None:
                count += 1
            i += 1
        return True if count > 0 else False

    def generate_report(self, authorize_response: AuthorizeResultResponse | None, report: str) -> _t.List[str]:
        result = []
        if authorize_response is not None:
            diagnostic = authorize_response.diagnostics
            if report == "reason":
                for reason in diagnostic.reason:
                    result.append(reason)
            else:
                for error in diagnostic.errors:
                    result.append(error.error)
        return result
    
    def get_reason(self, authorize_response: AuthorizeResultResponse | None) -> _t.List[str]:
        result = []
        if authorize_response is not None:
            for reason in authorize_response.diagnostics.reason:
                result.append(reason)
        return result

    def authorize(self,
                  subject: DictType,
                  action: _t.Dict[str, str],
                  resource: DictType,
                  context: DictType) -> DictType:
        result_dict = {}
        action_entity = action.get("name", "")
        resource_entity = self.generate_resource(resource)
        if not self.validate_subject(subject):
            result_dict["decision"] = False
            result_dict["context"] = {
               "id": "-1",
               "reason_user": {
                   "422": "Missing one or more tokens"
               }
            }
            return result_dict 
        access_token = subject["properties"].get("access_token", None)
        id_token = subject["properties"].get("id_token", None)
        userinfo_token = subject["properties"].get("userinfo_token", None)
        try:
            tokens = Tokens(access_token, id_token, userinfo_token)
            request = Request(tokens, action_entity, resource_entity, context)
            authorize_result = self._cedarling.authorize(request)
        except Exception as e:
            result_dict["decision"] = False
            result_dict["context"] = {
               "id": "-1",
               "reason_admin": {
                   "Exception": f"{e}" 
               }
            }
            logger.info(f"Exception during cedarling authorize: {e}")
            return result_dict 
        authorize_bool = authorize_result.is_allowed()
        if authorize_bool:
            result_dict["decision"] = True
        else:
            result_dict["decision"] = False
            person_result = authorize_result.person()
            workload_result = authorize_result.workload()
            person_value = None
            workload_value = None
            if person_result is not None:
                person_value = person_result.decision.value
            if workload_result is not None:
                workload_value = workload_result.decision.value
            if self.debug_response:
                person_diagnostic = self.generate_report(person_result, "reason")
                person_error = self.generate_report(person_result, "error")
                person_reason = self.get_reason(person_result)
                workload_diagnostic = self.generate_report(workload_result, "reason")
                workload_error = self.generate_report(workload_result, "error")
                workload_reason = self.get_reason(workload_result)
                result_dict["context"] = {
                    "reason_admin": {
                        "person evaluation": person_value,
                        "person diagnostics": person_diagnostic,
                        "person error": person_error,
                        "person reason": person_reason,
                        "workload evaluation": workload_value,
                        "workload diagnostics": workload_diagnostic,
                        "workload error": workload_error,
                        "workload reason": workload_reason
                    }
                }
        return result_dict
