from dataclasses import dataclass
from main.domain.entities.authzen import (
    CedarlingRequest,
    Token,
    Decision,
    TraceCorrelation,
    resource_from_data,
)
from main.domain.services.authzen_service import AuthzenService
from flask_smorest import abort
import typing as _t


TRACE_EXECUTION_ID_HEADER = "Audit-Trace-ID"
EXECUTION_AUTHORITY_HEADER = "Audit-Execution-Authority"
WORKLOAD_ID_HEADER = "Audit-Workload-ID"
WORKLOAD_INSTANCE_ID_HEADER = "Audit-Workload-Instance-ID"
TRACE_HEADERS = (
    TRACE_EXECUTION_ID_HEADER,
    EXECUTION_AUTHORITY_HEADER,
    WORKLOAD_ID_HEADER,
    WORKLOAD_INSTANCE_ID_HEADER,
)
MAX_TRACE_VALUE_LENGTH = 255


@dataclass
class CedarlingAuthorizeInputPort:
    subject: _t.Dict[str, _t.Any]
    action: _t.Dict[str, _t.Any]
    resource: _t.Dict[str, _t.Any]
    context: _t.Dict[str, _t.Any] | None = None
    trace_headers: _t.Dict[str, str] | None = None


def trace_correlation_from_headers(
    headers: _t.Mapping[str, str] | None,
) -> TraceCorrelation | None:
    values = {}
    for name in TRACE_HEADERS:
        value = (headers or {}).get(name)
        value = value.strip() if value else ""
        if len(value) > MAX_TRACE_VALUE_LENGTH:
            abort(400, message=f"{name} exceeds {MAX_TRACE_VALUE_LENGTH} characters")
        values[name] = value or None

    execution_id = values[TRACE_EXECUTION_ID_HEADER]
    authority = values[EXECUTION_AUTHORITY_HEADER]
    if execution_id is None and authority is None:
        return None
    if execution_id is None or authority is None:
        abort(
            400,
            message=f"{TRACE_EXECUTION_ID_HEADER} and {EXECUTION_AUTHORITY_HEADER} must be sent together",
        )
    return TraceCorrelation(
        trace_execution_id=execution_id,
        execution_authority=authority,
        workload_id=values[WORKLOAD_ID_HEADER],
        workload_instance_id=values[WORKLOAD_INSTANCE_ID_HEADER],
    )


class CedarlingAuthorizeUseCase:
    def __init__(self, authzen_service: AuthzenService):
        self.authzen_service = authzen_service

    def execute(self, input_port: CedarlingAuthorizeInputPort) -> Decision:
        if "properties" not in input_port.subject:
            abort(422, message="No token mapping provided")
        if "properties" not in input_port.resource:
            abort(422, message="No resource data provided")
        resource = input_port.resource["properties"]
        tokens = input_port.subject["properties"]["tokens"]
        action = input_port.action["name"]
        context = input_port.context if input_port.context else {}
        token_list = [Token(**i) for i in tokens]
        resource_class = resource_from_data(resource)
        cedarling_request = CedarlingRequest(
            token_list,
            action,
            resource_class,
            context,
            trace_correlation_from_headers(input_port.trace_headers),
        )
        result = self.authzen_service.authorize(cedarling_request)
        return result
