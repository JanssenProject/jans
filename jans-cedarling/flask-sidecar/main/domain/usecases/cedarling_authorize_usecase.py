from dataclasses import dataclass
from main.domain.entities.authzen import (
    CedarlingRequest,
    Token,
    Decision,
    resource_from_data,
)
from main.domain.services.authzen_service import AuthzenService
from flask_smorest import abort
import typing as _t


@dataclass
class CedarlingAuthorizeInputPort:
    subject: _t.Dict[str, _t.Any]
    action: _t.Dict[str, _t.Any]
    resource: _t.Dict[str, _t.Any]
    context: _t.Dict[str, _t.Any] | None = None


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
            token_list, action, resource_class, context
        )
        result = self.authzen_service.authorize(cedarling_request)
        return result
