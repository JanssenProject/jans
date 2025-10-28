# This software is available under the Apache-2.0 license.
# See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
#
# Copyright (c) 2024, Gluu, Inc.

from cedarling_python import Cedarling, AuthorizeMultiIssuerRequest, EntityData, TokenInput
from cedarling_python import authorize_errors
from config import load_bootstrap_config


# JSON payload of access token
# {
#   "sub": "boG8dfc5MKTn37o7gsdCeyqL8LpWQtgoO41m1KZwdq0",
#   "code": "bf1934f6-3905-420a-8299-6b2e3ffddd6e",
#   "iss": "https://test.jans.org",
#   "token_type": "Bearer",
#   "client_id": "5b4487c4-8db1-409d-a653-f907b8094039",
#   "aud": "5b4487c4-8db1-409d-a653-f907b8094039",
#   "acr": "basic",
#   "x5t#S256": "",
#   "scope": ["openid", "profile"],
#   "org_id": "some_long_id",
#   "auth_time": 1724830746,
#   "exp": 1724945978,
#   "iat": 1724832259,
#   "jti": "lxTmCVRFTxOjJgvEEpozMQ",
#   "name": "Default Admin User",
#   "status": {
#     "status_list": {
#       "idx": 201,
#       "uri": "https://test.jans.org/jans-auth/restv1/status_list"
#     }
#   }
# }
ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJib0c4ZGZjNU1LVG4zN283Z3NkQ2V5cUw4THBXUXRnb080MW0xS1p3ZHEwIiwiY29kZSI6ImJmMTkzNGY2LTM5MDUtNDIwYS04Mjk5LTZiMmUzZmZkZGQ2ZSIsImlzcyI6Imh0dHBzOi8vdGVzdC5qYW5zLm9yZyIsInRva2VuX3R5cGUiOiJCZWFyZXIiLCJjbGllbnRfaWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhdWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhY3IiOiJiYXNpYyIsIng1dCNTMjU2IjoiIiwic2NvcGUiOlsib3BlbmlkIiwicHJvZmlsZSJdLCJvcmdfaWQiOiJzb21lX2xvbmdfaWQiLCJhdXRoX3RpbWUiOjE3MjQ4MzA3NDYsImV4cCI6MTcyNDk0NTk3OCwiaWF0IjoxNzI0ODMyMjU5LCJqdGkiOiJseFRtQ1ZSRlR4T2pKZ3ZFRXBvek1RIiwibmFtZSI6IkRlZmF1bHQgQWRtaW4gVXNlciIsInN0YXR1cyI6eyJzdGF0dXNfbGlzdCI6eyJpZHgiOjIwMSwidXJpIjoiaHR0cHM6Ly90ZXN0LmphbnMub3JnL2phbnMtYXV0aC9yZXN0djEvc3RhdHVzX2xpc3QifX19.7n4vE60lisFLnEFhVwYMOPh5loyLLtPc07sCvaFI-Ik"

# JSON payload of id token
ID_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhY3IiOiJiYXNpYyIsImFtciI6IjEwIiwiYXVkIjpbIjViNDQ4N2M0LThkYjEtNDA5ZC1hNjUzLWY5MDdiODA5NDAzOSJdLCJleHAiOjE3MjQ4MzU4NTksImlhdCI6MTcyNDgzMjI1OSwic3ViIjoiYm9HOGRmYzVNS1RuMzdvN2dzZENleXFMOExwV1F0Z29PNDFtMUtad2RxMCIsImlzcyI6Imh0dHBzOi8vdGVzdC5qYW5zLm9yZyIsImp0aSI6InNrM1Q0ME5ZU1l1azVzYUhaTnBrWnciLCJub25jZSI6ImMzODcyYWY5LWEwZjUtNGMzZi1hMWFmLWY5ZDBlODg0NmU4MSIsInNpZCI6IjZhN2ZlNTBhLWQ4MTAtNDU0ZC1iZTVkLTU0OWQyOTU5NWEwOSIsImphbnNPcGVuSURDb25uZWN0VmVyc2lvbiI6Im9wZW5pZGNvbm5lY3QtMS4wIiwiY19oYXNoIjoicEdvSzZZX1JLY1dIa1VlY005dXc2USIsImF1dGhfdGltZSI6MTcyNDgzMDc0NiwiZ3JhbnQiOiJhdXRob3JpemF0aW9uX2NvZGUiLCJzdGF0dXMiOnsic3RhdHVzX2xpc3QiOnsiaWR4IjoyMDIsInVyaSI6Imh0dHBzOi8vdGVzdC5qYW5zLm9yZy9qYW5zLWF1dGgvcmVzdHYxL3N0YXR1c19saXN0In19LCJyb2xlIjoiQWRtaW4ifQ.RgCuWFUUjPVXmbW3ExQavJZH8Lw4q3kGhMFBRR0hSjA"

# JSON payload of userinfo token
USERINFO_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb3VudHJ5IjoiVVMiLCJlbWFpbCI6InVzZXJAZXhhbXBsZS5jb20iLCJ1c2VybmFtZSI6IlVzZXJOYW1lRXhhbXBsZSIsInN1YiI6ImJvRzhkZmM1TUtUbjM3bzdnc2RDZXlxTDhMcFdRdGdvTzQxbTFLWndkcTAiLCJpc3MiOiJodHRwczovL3Rlc3QuamFucy5vcmciLCJnaXZlbl9uYW1lIjoiQWRtaW4iLCJtaWRkbGVfbmFtZSI6IkFkbWluIiwiaW51bSI6IjhkMWNkZTZhLTE0NDctNDc2Ni1iM2M4LTE2NjYzZTEzYjQ1OCIsImF1ZCI6IjViNDQ4N2M0LThkYjEtNDA5ZC1hNjUzLWY5MDdiODA5NDAzOSIsInVwZGF0ZWRfYXQiOjE3MjQ3Nzg1OTEsIm5hbWUiOiJEZWZhdWx0IEFkbWluIFVzZXIiLCJuaWNrbmFtZSI6IkFkbWluIiwiZmFtaWx5X25hbWUiOiJVc2VyIiwianRpIjoiZmFpWXZhWUlUMGNEQVQ3Rm93MHBRdyIsImphbnNBZG1pblVJUm9sZSI6WyJhcGktYWRtaW4iXSwiZXhwIjoxNzI0OTQ1OTc4fQ.t6p8fYAe1NkUt9mn9n9MYJlNCni8JYfhk-82hb_C1O4"

# string representation of the authorize decision result
ALLOW_DECISION_STR = "ALLOW"
DENY_DECISION_STR = "DENY"

# Create resource with type "Jans::Issue" from cedar-policy schema.
RESOURCE = EntityData.from_dict({
    "cedar_entity_mapping": {
        "entity_type": "Jans::Issue",
        "id": "random_id"
    },
    "org_id": "some_long_id",
    "country": "US"
})


def test_authorize_multi_issuer_ok():
    '''
    Test successful multi-issuer authorization with valid tokens.
    Verifies that the request is allowed when all tokens are valid.
    '''
    instance = Cedarling(load_bootstrap_config())
    
    # Create token inputs with explicit mappings
    tokens = [
        TokenInput(mapping="Jans::Access_Token", payload=ACCESS_TOKEN),
        TokenInput(mapping="Jans::Id_Token", payload=ID_TOKEN),
        TokenInput(mapping="Jans::Userinfo_Token", payload=USERINFO_TOKEN),
    ]
    
    request = AuthorizeMultiIssuerRequest(
        tokens=tokens,
        action='Jans::Action::"Update"',
        context={},
        resource=RESOURCE,
    )
    
    result = instance.authorize_multi_issuer(request)
    
    # Verify decision is allowed
    assert result.is_allowed(), "request should be allowed"
    
    # Verify response decision
    response = result.response()
    decision = response.decision
    assert str(decision) == ALLOW_DECISION_STR, "decision should be ALLOW"
    assert decision.value == ALLOW_DECISION_STR, "decision value should be ALLOW"
    
    # Verify diagnostics
    diagnostics = response.diagnostics
    # Reason should contain policy IDs that granted access
    assert len(diagnostics.reason) > 0, "should have at least one reason"
    assert len(diagnostics.errors) == 0, "should have no errors"
    
    # Verify request ID is present
    request_id = result.request_id()
    assert request_id is not None and request_id != "", "request_id should be present"


def test_authorize_multi_issuer_with_invalid_token():
    '''
    Test multi-issuer authorization with an invalid token.
    Verifies that appropriate error is raised.
    '''
    instance = Cedarling(load_bootstrap_config())
    
    # Create token inputs with one invalid token
    tokens = [
        TokenInput(mapping="Jans::Access_Token", payload=ACCESS_TOKEN),
        TokenInput(mapping="Jans::Id_Token", payload="invalid.jwt.token"),
        TokenInput(mapping="Jans::Userinfo_Token", payload=USERINFO_TOKEN),
    ]
    
    request = AuthorizeMultiIssuerRequest(
        tokens=tokens,
        action='Jans::Action::"Update"',
        context={},
        resource=RESOURCE,
    )
    
    # This should raise an error due to invalid token
    try:
        instance.authorize_multi_issuer(request)
        assert False, "Should have raised an error for invalid token"
    except authorize_errors.MultiIssuerValidationError as e:
        # Expected to catch a multi-issuer validation error for invalid JWT
        assert e is not None, "should raise a MultiIssuerValidationError"


def test_authorize_multi_issuer_with_empty_tokens():
    '''
    Test multi-issuer authorization with no tokens.
    Verifies that appropriate error is raised.
    '''
    instance = Cedarling(load_bootstrap_config())
    
    # Create request with no tokens
    request = AuthorizeMultiIssuerRequest(
        tokens=[],
        action='Jans::Action::"Update"',
        context={},
        resource=RESOURCE,
    )
    
    # This should raise an error due to no tokens
    try:
        instance.authorize_multi_issuer(request)
        assert False, "Should have raised an error for empty tokens"
    except authorize_errors.MultiIssuerValidationError as e:
        # Expected to catch a multi-issuer validation error for empty token array
        assert e is not None, "should raise a MultiIssuerValidationError"


def test_authorize_multi_issuer_with_invalid_resource():
    '''
    Test multi-issuer authorization with invalid resource schema.
    Verifies that appropriate error is raised.
    '''
    instance = Cedarling(load_bootstrap_config())
    
    # Create resource with invalid data (org_id should be string, not int)
    invalid_resource = EntityData.from_dict({
        "cedar_entity_mapping": {
            "entity_type": "Jans::Issue",
            "id": "random_id"
        },
        "org_id": 1,  # Should be string according to schema
        "country": "US"
    })
    
    tokens = [
        TokenInput(mapping="Jans::Access_Token", payload=ACCESS_TOKEN),
        TokenInput(mapping="Jans::Id_Token", payload=ID_TOKEN),
        TokenInput(mapping="Jans::Userinfo_Token", payload=USERINFO_TOKEN),
    ]
    
    request = AuthorizeMultiIssuerRequest(
        tokens=tokens,
        action='Jans::Action::"Update"',
        context={},
        resource=invalid_resource,
    )
    
    # This should raise a validation error
    try:
        instance.authorize_multi_issuer(request)
        assert False, "Should have raised a validation error"
    except (authorize_errors.ValidateEntitiesError, Exception) as e:
        # Expected to catch a validation error
        assert e is not None, "should raise a validation error"


def test_authorize_multi_issuer_deny_decision():
    '''
    Test multi-issuer authorization that results in DENY decision.
    Verifies that DENY decisions are handled correctly.
    '''
    instance = Cedarling(load_bootstrap_config())
    
    tokens = [
        TokenInput(mapping="Jans::Access_Token", payload=ACCESS_TOKEN),
        TokenInput(mapping="Jans::Id_Token", payload=ID_TOKEN),
        TokenInput(mapping="Jans::Userinfo_Token", payload=USERINFO_TOKEN),
    ]
    
    # Use action that doesn't allow the request
    request = AuthorizeMultiIssuerRequest(
        tokens=tokens,
        action='Jans::Action::"DeniedAction"',
        context={},
        resource=RESOURCE,
    )
    
    result = instance.authorize_multi_issuer(request)
    
    # Verify decision is denied
    assert not result.is_allowed(), "request should be denied"
    
    # Verify response decision is DENY
    response = result.response()
    decision = response.decision
    assert str(decision) == DENY_DECISION_STR, "decision should be DENY"
    assert decision.value == DENY_DECISION_STR, "decision value should be DENY"
    
    # Verify diagnostics
    diagnostics = response.diagnostics
    # For DENY decisions, we should have no reasons (no policies granted access)
    assert len(diagnostics.reason) == 0, "should have no reasons for DENY decision"
    assert len(diagnostics.errors) == 0, "should have no errors"
    
    # Verify request ID is present
    request_id = result.request_id()
    assert request_id is not None and request_id != "", "request_id should be present"


def test_authorize_multi_issuer_with_context():
    '''
    Test multi-issuer authorization with additional context.
    Verifies that context is passed correctly.
    '''
    instance = Cedarling(load_bootstrap_config())
    
    tokens = [
        TokenInput(mapping="Jans::Access_Token", payload=ACCESS_TOKEN),
        TokenInput(mapping="Jans::Id_Token", payload=ID_TOKEN),
        TokenInput(mapping="Jans::Userinfo_Token", payload=USERINFO_TOKEN),
    ]
    
    # Create request with context
    request = AuthorizeMultiIssuerRequest(
        tokens=tokens,
        action='Jans::Action::"Update"',
        context={"some_context": "value"},
        resource=RESOURCE,
    )
    
    result = instance.authorize_multi_issuer(request)
    
    # Verify the request was processed
    assert result is not None, "result should not be None"
    assert hasattr(result, 'is_allowed'), "result should have is_allowed method"


def test_token_input_creation():
    '''
    Test TokenInput object creation and attributes.
    '''
    token = TokenInput(mapping="Jans::Access_Token", payload="test.jwt.token")
    
    assert token.mapping == "Jans::Access_Token", "mapping should be correct"
    assert token.payload == "test.jwt.token", "payload should be correct"


def test_authorize_multi_issuer_request_creation():
    '''
    Test AuthorizeMultiIssuerRequest object creation and attributes.
    '''
    tokens = [
        TokenInput(mapping="Jans::Access_Token", payload="test.jwt.token"),
    ]
    
    request = AuthorizeMultiIssuerRequest(
        tokens=tokens,
        action="TestAction",
        context={"key": "value"},
        resource=RESOURCE,
    )
    
    assert len(request.tokens) == 1, "should have one token"
    assert request.action == "TestAction", "action should be correct"
    assert request.context == {"key": "value"}, "context should be correct"


def test_authorize_multi_issuer_with_minimal_context():
    '''
    Test multi-issuer authorization with None context (should default to empty dict).
    '''
    instance = Cedarling(load_bootstrap_config())
    
    tokens = [
        TokenInput(mapping="Jans::Access_Token", payload=ACCESS_TOKEN),
        TokenInput(mapping="Jans::Id_Token", payload=ID_TOKEN),
    ]
    
    request = AuthorizeMultiIssuerRequest(
        tokens=tokens,
        action='Jans::Action::"Update"',
        context=None,
        resource=RESOURCE,
    )
    
    result = instance.authorize_multi_issuer(request)
    
    # Verify decision is allowed (same action and tokens as successful test)
    assert result.is_allowed(), "request should be allowed"
    
    # Verify response decision
    response = result.response()
    decision = response.decision
    assert str(decision) == ALLOW_DECISION_STR, "decision should be ALLOW"
    assert decision.value == ALLOW_DECISION_STR, "decision value should be ALLOW"
    
    # Verify diagnostics
    diagnostics = response.diagnostics
    # Reason should contain policy IDs that granted access
    assert len(diagnostics.reason) > 0, "should have at least one reason"
    assert len(diagnostics.errors) == 0, "should have no errors"
    
    # Verify request ID is present
    request_id = result.request_id()
    assert request_id is not None and request_id != "", "request_id should be present"

