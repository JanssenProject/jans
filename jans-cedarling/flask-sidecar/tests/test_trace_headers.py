import pytest
from werkzeug.exceptions import HTTPException

from main.domain.entities.authzen import TraceCorrelation
from main.domain.usecases.cedarling_authorize_usecase import (
    MAX_TRACE_VALUE_LENGTH,
    trace_correlation_from_headers,
)


def test_no_trace_headers_returns_none():
    assert trace_correlation_from_headers({}) is None
    assert trace_correlation_from_headers(None) is None
    assert trace_correlation_from_headers({"Audit-Trace-ID": "  "}) is None


def test_reads_all_trace_headers():
    trace = trace_correlation_from_headers(
        {
            "Audit-Trace-ID": " exec-1 ",
            "Audit-Execution-Authority": "spiffe://example.org/agent",
            "Audit-Workload-ID": "spiffe://example.org/agent",
            "Audit-Workload-Instance-ID": "instance-1",
        }
    )

    assert trace == TraceCorrelation(
        trace_execution_id="exec-1",
        execution_authority="spiffe://example.org/agent",
        workload_id="spiffe://example.org/agent",
        workload_instance_id="instance-1",
    )


def test_workload_headers_are_optional():
    trace = trace_correlation_from_headers(
        {"Audit-Trace-ID": "exec-1", "Audit-Execution-Authority": "authority"}
    )

    assert trace == TraceCorrelation("exec-1", "authority")


@pytest.mark.parametrize(
    "headers",
    [
        {"Audit-Trace-ID": "exec-1"},
        {"Audit-Execution-Authority": "authority"},
        {"Audit-Workload-ID": "workload", "Audit-Trace-ID": "exec-1"},
    ],
)
def test_execution_id_and_authority_must_come_together(headers):
    with pytest.raises(HTTPException) as error:
        trace_correlation_from_headers(headers)

    assert error.value.code == 400


def test_rejects_values_over_lock_limit():
    with pytest.raises(HTTPException) as error:
        trace_correlation_from_headers(
            {
                "Audit-Trace-ID": "x" * (MAX_TRACE_VALUE_LENGTH + 1),
                "Audit-Execution-Authority": "authority",
            }
        )

    assert error.value.code == 400
