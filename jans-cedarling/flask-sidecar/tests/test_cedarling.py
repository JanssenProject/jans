from unittest.mock import MagicMock

from main.base.cedarling.cedarling import CedarlingInstance
from main.domain.entities.authzen import CedarlingRequest, EntityMapping, Resource


def make_instance(authorize_result, logs=None):
    instance = CedarlingInstance()
    instance.debug_response = False
    instance._cedarling = MagicMock()
    instance._cedarling.authorize_multi_issuer.return_value = authorize_result
    instance._cedarling.get_logs_by_request_id_and_tag.return_value = logs or []
    return instance


def make_request():
    return CedarlingRequest(
        tokens=[],
        action='Jans::Action::"Read"',
        resource=Resource(cedar_entity_mapping=EntityMapping(entity_type="Jans::Issue", id="1")),
        context={},
    )


def test_authorize_returns_policy_store_details():
    authorize_result = MagicMock()
    authorize_result.request_id.return_value = "req-1"
    authorize_result.is_allowed.return_value = True
    logs = [
        {"policystore_id": "", "policystore_version": ""},
        {"policystore_id": "abc", "policystore_version": "1.0.0"},
    ]

    decision = make_instance(authorize_result, logs).authorize(make_request())

    assert decision.decision is True
    assert decision.context == {
        "request_id": "req-1",
        "policy_store_id": "abc",
        "policy_store_version": "1.0.0",
    }


def test_authorize_returns_decision_when_request_id_fails():
    authorize_result = MagicMock()
    authorize_result.request_id.side_effect = RuntimeError("no request id")
    authorize_result.is_allowed.return_value = False

    decision = make_instance(authorize_result).authorize(make_request())

    assert decision.decision is False
    assert "request_id" not in decision.context


def test_authorize_without_policy_store_details():
    authorize_result = MagicMock()
    authorize_result.request_id.return_value = "req-2"
    authorize_result.is_allowed.return_value = True

    decision = make_instance(authorize_result, [{"decision": "ALLOW"}]).authorize(make_request())

    assert decision.context == {"request_id": "req-2"}
