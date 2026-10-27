import json
from unittest.mock import MagicMock

from main.base.cedarling.cedarling import (
    DEFAULT_TRACE_WORKLOAD_ID,
    CedarlingInstance,
    resolve_trace_workload_id,
)
from main.domain.entities.authzen import (
    CedarlingRequest,
    EntityMapping,
    Resource,
    TraceCorrelation,
)


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
        resource=Resource(
            cedar_entity_mapping=EntityMapping(entity_type="Jans::Issue", id="1")
        ),
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

    decision = make_instance(authorize_result, [{"decision": "ALLOW"}]).authorize(
        make_request()
    )

    assert decision.context == {"request_id": "req-2"}


def test_authorize_returns_trace_execution_id():
    authorize_result = MagicMock()
    authorize_result.request_id.return_value = "req-3"
    authorize_result.is_allowed.return_value = True
    request = make_request()
    request.trace = TraceCorrelation("exec-1", "spiffe://example.org/agent")

    decision = make_instance(authorize_result).authorize(request)

    assert decision.context == {"request_id": "req-3", "trace_execution_id": "exec-1"}
    assert request.context == {}


def sent_options(instance):
    return instance._cedarling.authorize_multi_issuer.call_args.args[0].options


def make_allowed_result(request_id):
    authorize_result = MagicMock()
    authorize_result.request_id.return_value = request_id
    authorize_result.is_allowed.return_value = True
    return authorize_result


def test_authorize_passes_trace_context():
    instance = make_instance(make_allowed_result("req-4"))
    request = make_request()
    request.trace = TraceCorrelation(
        "exec-1", "spiffe://example.org/agent", "workload-1", "instance-1"
    )

    instance.authorize(request)

    trace = sent_options(instance).trace
    assert trace.execution_id == "exec-1"
    assert trace.execution_authority == "spiffe://example.org/agent"
    assert trace.workload_id == "workload-1"
    assert trace.workload_instance_id == "instance-1"


def test_authorize_uses_default_workload_id():
    instance = make_instance(make_allowed_result("req-5"))
    instance.trace_workload_id = "sidecar-workload"
    request = make_request()
    request.trace = TraceCorrelation("exec-1", "authority")

    instance.authorize(request)

    trace = sent_options(instance).trace
    assert trace.workload_id == "sidecar-workload"
    assert trace.workload_instance_id is None


def test_authorize_without_trace_sends_no_options():
    instance = make_instance(make_allowed_result("req-6"))

    instance.authorize(make_request())

    assert sent_options(instance) is None


def test_authorize_returns_trace_record_status():
    logs = [{"policystore_id": "abc", "trace_record_status": "emitted"}]
    instance = make_instance(make_allowed_result("req-7"), logs)
    request = make_request()
    request.trace = TraceCorrelation("exec-1", "authority")

    decision = instance.authorize(request)

    assert decision.context["trace_record_status"] == "emitted"
    instance._cedarling.get_logs_by_request_id_and_tag.assert_called_once_with(
        "req-7", "Decision"
    )


def test_authorize_omits_missing_trace_record_status():
    logs = [{"policystore_id": "abc"}]

    decision = make_instance(make_allowed_result("req-8"), logs).authorize(
        make_request()
    )

    assert "trace_record_status" not in decision.context


def test_resolve_trace_workload_id(monkeypatch):
    bootstrap = json.dumps({"CEDARLING_APPLICATION_NAME": "app-1"})
    monkeypatch.setenv("CEDARLING_APPLICATION_NAME", "env-app")

    assert resolve_trace_workload_id(" configured ", bootstrap) == "configured"
    assert resolve_trace_workload_id(None, bootstrap) == "app-1"
    assert resolve_trace_workload_id("x" * 256, bootstrap) == "app-1"
    assert resolve_trace_workload_id(None, None) == "env-app"
    assert resolve_trace_workload_id(None, "{}") == DEFAULT_TRACE_WORKLOAD_ID


def test_shut_down_calls_cedarling_once():
    instance = make_instance(MagicMock())

    instance.shut_down()
    instance.shut_down()

    instance._cedarling.shut_down.assert_called_once_with()


def test_shut_down_ignores_cedarling_errors():
    instance = make_instance(MagicMock())
    instance._cedarling.shut_down.side_effect = RuntimeError("flush failed")

    instance.shut_down()

    instance._cedarling.shut_down.assert_called_once_with()


def test_shut_down_without_cedarling():
    CedarlingInstance().shut_down()
