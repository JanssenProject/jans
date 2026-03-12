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

from dataclasses import asdict
from cedarling_python import BootstrapConfig
from cedarling_python import Cedarling
from cedarling_python import (
    EntityData,
    AuthorizeMultiIssuerRequest,
    MultiIssuerAuthorizeResult,
    TokenInput,
)
from main.logger import logger
from flask import Flask
import typing as _t
from main.domain.entities.authzen import CedarlingRequest, Decision, Resource, Context

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

    def generate_resource(self, resource: Resource) -> EntityData:
        resource_entity_dict = {
            "cedar_entity_mapping": asdict(resource.cedar_entity_mapping)
        }
        for key in resource.attributes.keys():
            resource_entity_dict[key] = resource.attributes[key]
        resource_entity = EntityData.from_dict(resource_entity_dict)
        return resource_entity

    def generate_report(
        self, authorize_response: MultiIssuerAuthorizeResult, report: str
    ) -> _t.List[str]:
        result = []
        response = authorize_response.response()
        diagnostic = response.diagnostics
        if report == "reason":
            for reason in diagnostic.reason:
                result.append(reason)
        else:
            for error in diagnostic.errors:
                result.append(error.error)
        return result

    def get_reason(
        self, authorize_response: MultiIssuerAuthorizeResult | None
    ) -> _t.List[str]:
        result = []
        if authorize_response is not None:
            for reason in authorize_response.response().diagnostics.reason:
                result.append(reason)
        return result

    def authorize(self, request: CedarlingRequest) -> Decision:
        result_dict = {}
        action_entity = request.action
        resource = request.resource
        resource_entity = self.generate_resource(resource)
        context = request.context
        tokens = []
        for token in request.tokens:
            mapping = token.mapping
            payload = token.payload
            token_input = TokenInput(mapping=mapping, payload=payload)
            tokens.append(token_input)
        try:
            cedarling_request = AuthorizeMultiIssuerRequest(
                tokens=tokens,
                action=action_entity,
                resource=resource_entity,
                context=context,
            )
            authorize_result = self._cedarling.authorize_multi_issuer(cedarling_request)
        except Exception as e:
            result_dict["decision"] = False
            result_dict["context"] = {"id": "-1", "reason_admin": {"Exception": f"{e}"}}
            logger.info(f"Exception during cedarling authorize: {e}")
            return Decision(**result_dict)
        try:
            request_id = authorize_result.request_id()
            tag = "Decision"
            decision_log = self._cedarling.get_logs_by_request_id_and_tag(
                request_id, tag
            )
            i = 1
            for log in decision_log:
                logger.info(f"Decision log {i}: {str(log)}")
                i += 1
        except Exception as e:
            logger.warning(f"Failed to retrieve logs: {e}")
        authorize_bool = authorize_result.is_allowed()
        result_dict["context"] = {}
        if authorize_bool:
            result_dict["decision"] = True
        else:
            result_dict["decision"] = False
        if self.debug_response:
            authorize_reason = self.generate_report(authorize_result, "reason")
            authorize_error = self.generate_report(authorize_result, "error")
            result_dict["context"] = {
                "reason_admin": {
                    "authorize_reason": authorize_reason,
                    "authorize_error": authorize_error,
                }
            }
        logger.info(f"Cedarling evaluation result: {result_dict}")
        result = Decision(**result_dict)
        return result
