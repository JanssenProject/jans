import pytest
from unittest.mock import patch
from main.domain.usecases.cedarling_authorize_usecase import (
    CedarlingAuthorizeUseCase,
    CedarlingAuthorizeInputPort,
)
from main.domain.entities.authzen import Decision


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

    def test_invalid_payload(self, client, mock_execute):
        sut = self.makeSUT(client, mock_execute)
        response = self.make_request(sut, "/cedarling/evaluation", "POST", json={})
        assert response.status_code == 422

    def test_successful_request(self, client, mock_execute, mock_json_input):
        mock_execute.return_value = {"context": {}, "decision": True}
        sut = self.makeSUT(client, mock_execute)
        response = self.make_request(
            sut, "/cedarling/evaluation", "POST", json=mock_json_input
        )
        assert response.json == {"context": {}, "decision": True}
        assert response.status_code == 200
        mock_execute.assert_called_once_with(
            CedarlingAuthorizeInputPort(
                subject={
                    "type_field": "JWT",
                    "id": "random_id",
                    "properties": {
                        "tokens": [
                            {
                                "mapping": "Jans::Access_token",
                                "payload": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0.KMUFsIDTnFmyG3nMiGM6H9FNFUROf3wh7SmqJp-QV30",
                            }
                        ]
                    },
                },
                action={"name": 'Jans::Action::"GET"'},
                resource={
                    "type_field": "property",
                    "id": "random_id",
                    "properties": {
                        "cedar_entity_mapping": {
                            "entity_type": "Jans::Application",
                            "id": "random_id",
                        }
                    },
                },
                context=None,
                trace_headers=None,
            )
        )

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
                        "cedar_entity_mapping": {
                            "entity_type": "Jans::Issue",
                            "id": "1",
                        }
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
