import pytest
from unittest.mock import patch
from main.domain.entities.authzen import Decision
from main.domain.usecases.cedarling_authorize_usecase import (
    CedarlingAuthorizeUseCase,
    CedarlingAuthorizeInputPort,
)


class TestResource:
    @pytest.fixture
    def mock_execute(self):
        with patch.object(CedarlingAuthorizeUseCase, "execute") as mock:
            yield mock

    def makeSUT(self, client, mock_execute):
        return {"client": client, "mock_execute": mock_execute}

    def make_request(
        self,
        sut,
        endpoint,
        method,
        headers=None,
        data=None,
        json=None,
        content_type=None,
    ):
        return sut["client"].open(
            endpoint,
            method=method,
            headers=headers,
            data=data,
            json=json,
            content_type=content_type,
        )

    def test_wellknown_configuration(self, client):
        response = client.get("/.well-known/authzen-configuration")
        assert response.status_code == 200
        assert response.json == {
            "access_evaluation_v1_endpoint": "http://localhost/cedarling/evaluation"
        }

    def test_evaluation_passes_trace_headers(self, client, mock_execute):
        mock_execute.return_value = Decision(True, {})

        response = client.post(
            "/cedarling/evaluation",
            json={
                "subject": {"type": "JWT", "id": "1", "properties": {"tokens": []}},
                "action": {"name": "Read"},
                "resource": {
                    "type": "Issue",
                    "id": "1",
                    "properties": {
                        "cedar_entity_mapping": {"entity_type": "Jans::Issue", "id": "1"}
                    },
                },
                "context": {},
            },
            headers={
                "Audit-Trace-ID": "exec-1",
                "Audit-Execution-Authority": "spiffe://example.org/agent",
            },
        )

        assert response.status_code == 200
        input_port = mock_execute.call_args.args[0]
        assert isinstance(input_port, CedarlingAuthorizeInputPort)
        assert input_port.trace_headers["Audit-Trace-ID"] == "exec-1"
        assert (
            input_port.trace_headers["Audit-Execution-Authority"]
            == "spiffe://example.org/agent"
        )
