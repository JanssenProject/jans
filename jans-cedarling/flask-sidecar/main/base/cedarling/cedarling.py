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

from cedarling_python import BootstrapConfig
from cedarling_python import Cedarling
from cedarling_python import (
    EntityData,
    TokenInput,
    AuthorizeMultiIssuerRequest,
    AuthorizeResultResponse,
)
from main.logger import logger
from flask import Flask
import typing as _t
from hashlib import sha256
import json

DictType = _t.Dict[str, _t.Any]
KEYS_LIST = ["access_token", "id_token", "userinfo_token"]


class CedarlingInstance:

    def __init__(self, app=None):
        self._bootstrap_config: str | None
        self._cedarling: Cedarling
        if app is not None:
            self.init_app(app)

    def init_app(self, app: Flask):
        self._bootstrap_config = app.config.get("CEDARLING_BOOTSTRAP_CONFIG", None)
        self.debug_response: bool = app.config.get("SIDECAR_DEBUG_RESPONSE", False)
        self.disable_hash_check: bool = app.config.get("DISABLE_HASH_CHECK", False)
        app.extensions = getattr(app, "extensions", {})
        app.extensions["cedarling_client"] = self
        self.initialize_cedarling()

    def initialize_cedarling(self):
        if self._bootstrap_config is None:
            logger.info("Loading bootstrap from environment")
            bootstrap_config = BootstrapConfig.from_env()
        else:
            bootstrap_config = BootstrapConfig.load_from_json(self._bootstrap_config) 
        self._cedarling = Cedarling(bootstrap_config)

    def get_cedarling_instance(self) -> Cedarling:
        return self._cedarling

    def generate_hash(self, input: DictType) -> str:
        encoded_str = json.dumps(input).encode("utf-8")
        digest = sha256(encoded_str).hexdigest()
        return digest


    def generate_resource(self, resource: DictType) -> EntityData:
        resource_properties = resource.get("properties", {})
        resource_entity_dict = {
            "cedar_entity_mapping": {
                "entity_type": resource.get("type_field"),
                "id": resource.get("id")
            }
        }
        for key in resource_properties.keys():
            resource_entity_dict[key] = resource_properties[key]
        resource_entity = EntityData.from_dict(resource_entity_dict)
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
        if count == 0:
            return False
        if self.disable_hash_check:
            return True
        hash = self.generate_hash(subject["properties"])
        id = subject["id"]
        if hash != id:
            return False
        return True

    def validate_resource(self, resource: DictType) -> bool:
        if self.disable_hash_check:
            return True
        hash = self.generate_hash(resource["properties"])
        id = resource["id"]
        return True if hash == id else False

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
                    "422": "Subject is invalid"
                }
            }
            return result_dict
        if not self.validate_resource(resource):
            result_dict["decision"] = False
            result_dict["context"] = {
                "id": "-1",
                "reason_user": {
                    "422": "Resource is invalid"
                }
            }
            return result_dict
        access_token = subject["properties"].get("access_token", None)
        id_token = subject["properties"].get("id_token", None)
        userinfo_token = subject["properties"].get("userinfo_token", None)
        try:
            token_inputs = []
            if access_token is not None:
                token_inputs.append(TokenInput(mapping="Jans::Access_token", payload=access_token))
            if id_token is not None:
                token_inputs.append(TokenInput(mapping="Jans::Id_token", payload=id_token))
            if userinfo_token is not None:
                token_inputs.append(TokenInput(mapping="Jans::Userinfo_token", payload=userinfo_token))
            if not token_inputs:
                result_dict["decision"] = False
                result_dict["context"] = {"id": "-1", "reason_admin": {"Exception": "No tokens provided"}}
                return result_dict
            request = AuthorizeMultiIssuerRequest(
                tokens=token_inputs,
                action=action_entity,
                resource=resource_entity,
                context=context,
            )
            authorize_result = self._cedarling.authorize_multi_issuer(request)
            request_id = authorize_result.request_id()
            tag = "Decision"
            decision_log = self._cedarling.get_logs_by_request_id_and_tag(request_id, tag)
            i = 1
            for log in decision_log:
                logger.info(f"Decision log {i}: {str(log)}")
                i += 1
        except Exception as e:
            result_dict["decision"] = False
            result_dict["context"] = {
                "id": "-1",
                "reason_admin": {
                    "Exception": f"{e}"
                }
            }
            logger.info(f"Exception during cedarling authorize_multi_issuer: {e}")
            return result_dict
        authorize_bool = authorize_result.is_allowed()
        if authorize_bool:
            result_dict["decision"] = True
        else:
            result_dict["decision"] = False
        if self.debug_response:
            resp = authorize_result.response()
            decision_value = resp.decision.value if resp.decision else None
            diagnostics_reason = self.get_reason(resp)
            diagnostics_errors = self.generate_report(resp, "error")
            result_dict["context"] = {
                "reason_admin": {
                    "evaluation": decision_value,
                    "diagnostics_reason": diagnostics_reason,
                    "diagnostics_errors": diagnostics_errors,
                }
            }
        logger.info(f"Cedarling evaluation result: {result_dict}")
        return result_dict
