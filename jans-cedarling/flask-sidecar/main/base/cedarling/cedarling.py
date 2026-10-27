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

import json
import os
from dataclasses import asdict
from cedarling_python import BootstrapConfig
from cedarling_python import Cedarling
from cedarling_python import (
    EntityData,
    AuthorizeMultiIssuerRequest,
    AuthorizeOptions,
    MultiIssuerAuthorizeResult,
    TokenInput,
    TraceContext,
)
from main.logger import logger
from flask import Flask
import typing as _t
from main.domain.entities.authzen import (
    CedarlingRequest,
    Decision,
    Resource,
    TraceCorrelation,
)

DictType = _t.Dict[str, _t.Any]
KEYS_LIST = ["access_token", "id_token", "userinfo_token"]
DEFAULT_TRACE_WORKLOAD_ID = "cedarling-sidecar"
MAX_TRACE_WORKLOAD_ID_LENGTH = 255


def application_name(bootstrap_config: str | None) -> str | None:
    if bootstrap_config is None:
        return os.getenv("CEDARLING_APPLICATION_NAME")
    try:
        config = json.loads(bootstrap_config)
    except ValueError:
        return None
    if not isinstance(config, dict):
        return None
    name = config.get("CEDARLING_APPLICATION_NAME")
    return name if isinstance(name, str) else None


def resolve_trace_workload_id(
    configured: str | None, bootstrap_config: str | None
) -> str:
    for candidate in (configured, application_name(bootstrap_config)):
        value = candidate.strip() if candidate else ""
        if value and len(value) <= MAX_TRACE_WORKLOAD_ID_LENGTH:
            return value
        if value:
            logger.warning(
                f"Ignoring TRACE workload id over {MAX_TRACE_WORKLOAD_ID_LENGTH} characters"
            )
    return DEFAULT_TRACE_WORKLOAD_ID


class CedarlingInstance:
    def __init__(self, app=None):
        self._bootstrap_config: str | None
        self._cedarling: Cedarling
        self.trace_workload_id = DEFAULT_TRACE_WORKLOAD_ID
        self._is_shut_down = False
        if app is not None:
            self.init_app(app)

    def init_app(self, app: Flask):
        self._bootstrap_config = app.config.get("CEDARLING_BOOTSTRAP_CONFIG", None)
        self.debug_response: bool = app.config.get("SIDECAR_DEBUG_RESPONSE", False)
        self.trace_workload_id = resolve_trace_workload_id(
            app.config.get("SIDECAR_TRACE_WORKLOAD_ID"), self._bootstrap_config
        )
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

    def shut_down(self) -> None:
        cedarling = getattr(self, "_cedarling", None)
        if cedarling is None or self._is_shut_down:
            return
        self._is_shut_down = True
        try:
            cedarling.shut_down()
        except Exception as e:
            logger.warning(f"Exception during cedarling shutdown: {e}")

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

    def authorize_options(
        self, trace: TraceCorrelation | None
    ) -> AuthorizeOptions | None:
        if trace is None:
            return None
        return AuthorizeOptions(
            trace=TraceContext(
                execution_id=trace.trace_execution_id,
                execution_authority=trace.execution_authority,
                workload_id=trace.workload_id or self.trace_workload_id,
                workload_instance_id=trace.workload_instance_id,
            )
        )

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
                options=self.authorize_options(request.trace),
            )
            authorize_result = self._cedarling.authorize_multi_issuer(cedarling_request)
        except Exception as e:
            result_dict["decision"] = False
            result_dict["context"] = {"id": "-1", "reason_admin": {"Exception": f"{e}"}}
            logger.info(f"Exception during cedarling authorize: {e}")
            return Decision(**result_dict)
        request_id = None
        policy_store_id = None
        policy_store_version = None
        trace_record_status = None
        try:
            request_id = authorize_result.request_id()
            decision_log = self._cedarling.get_logs_by_request_id_and_tag(
                request_id, "Decision"
            )
            entry = next(
                (log for log in decision_log if log.get("policystore_id")),
                None,
            )
            if entry is not None:
                policy_store_id = entry.get("policystore_id")
                policy_store_version = entry.get("policystore_version")
            trace_record_status = next(
                (
                    log["trace_record_status"]
                    for log in decision_log
                    if log.get("trace_record_status")
                ),
                None,
            )
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
        if request_id is not None:
            result_dict["context"]["request_id"] = request_id
        if policy_store_id is not None:
            result_dict["context"]["policy_store_id"] = str(policy_store_id)
        if policy_store_version is not None:
            result_dict["context"]["policy_store_version"] = str(policy_store_version)
        if request.trace is not None:
            result_dict["context"]["trace_execution_id"] = (
                request.trace.trace_execution_id
            )
        if trace_record_status is not None:
            result_dict["context"]["trace_record_status"] = str(trace_record_status)
        result = Decision(**result_dict)
        return result
