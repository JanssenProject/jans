import pytest
from unittest.mock import Mock
from main.domain.usecases.cedarling_authorize_usecase import (
    CedarlingAuthorizeUseCase,
    CedarlingAuthorizeInputPort,
)
from werkzeug.exceptions import HTTPException
from main.domain.entities.authzen import (
    Decision,
    CedarlingRequest,
    Token,
    Resource,
    EntityMapping,
)


class TestAuthorizeUseCase:
    @pytest.fixture
    def mock_dependencies(self):
        return {"authzen_service": Mock()}

    @pytest.fixture
    def mock_usecase(self, mock_dependencies):
        return CedarlingAuthorizeUseCase(**mock_dependencies)

    @pytest.fixture
    def mock_input_port(self, mock_json_input):
        return CedarlingAuthorizeInputPort(
            subject=mock_json_input["subject"],
            action=mock_json_input["action"],
            resource=mock_json_input["resource"],
        )

    @pytest.mark.parametrize(
        "input,error",
        [
            (
                CedarlingAuthorizeInputPort(
                    subject={}, action={"name": "test"}, resource={"type": "resource"}
                ),
                "No token mapping provided",
            ),
            (
                CedarlingAuthorizeInputPort(
                    subject={"properties": {}}, action={"name": "test"}, resource={}
                ),
                "No resource data provided",
            ),
        ],
    )
    def test_missing_fields(self, mock_usecase, input, error):
        with pytest.raises(HTTPException) as e:
            mock_usecase.execute(input)
        assert getattr(e.value, "data", {}).get("message") == error

    def test_authorize_success(
        self, mock_dependencies, mock_usecase, mock_input_port, mock_json_input
    ):
        mock_dependencies["authzen_service"].authorize.return_value = Decision(
            decision=True, context={}
        )
        response = mock_usecase.execute(mock_input_port)
        assert response == Decision(decision=True, context={})
        mock_dependencies["authzen_service"].authorize.assert_called_once_with(
            CedarlingRequest(
                tokens=[Token(**mock_json_input["subject"]["properties"]["tokens"][0])],
                action=mock_json_input["action"]["name"],
                resource=Resource(
                    cedar_entity_mapping=EntityMapping(
                        entity_type="Jans::Application", id="random_id"
                    ),
                    attributes={},
                ),
                context={},
            )
        )
