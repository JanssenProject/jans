# This software is available under the Apache-2.0 license.
# See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
#
# Copyright (c) 2024, Gluu, Inc.

from cedarling_python import Cedarling, AuthorizeMultiIssuerRequest, EntityData, TokenInput
from cedarling_python import authorize_errors
from config import load_bootstrap_config
import pytest


# JSON payload of access token
ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJib0c4ZGZjNU1LVG4zN283Z3NkQ2V5cUw4THBXUXRnb080MW0xS1p3ZHEwIiwiY29kZSI6ImJmMTkzNGY2LTM5MDUtNDIwYS04Mjk5LTZiMmUzZmZkZGQ2ZSIsImlzcyI6Imh0dHBzOi8vdGVzdC5qYW5zLm9yZyIsInRva2VuX3R5cGUiOiJCZWFyZXIiLCJjbGllbnRfaWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhdWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhY3IiOiJiYXNpYyIsIng1dCNTMjU2IjoiIiwic2NvcGUiOlsib3BlbmlkIiwicHJvZmlsZSJdLCJvcmdfaWQiOiJzb21lX2xvbmdfaWQiLCJhdXRoX3RpbWUiOjE3MjQ4MzA3NDYsImV4cCI6MTcyNDk0NTk3OCwiaWF0IjoxNzI0ODMyMjU5LCJqdGkiOiJseFRtQ1ZSRlR4T2pKZ3ZFRXBvek1RIiwibmFtZSI6IkRlZmF1bHQgQWRtaW4gVXNlciIsInN0YXR1cyI6eyJzdGF0dXNfbGlzdCI6eyJpZHgiOjIwMSwidXJpIjoiaHR0cHM6Ly90ZXN0LmphbnMub3JnL2phbnMtYXV0aC9yZXN0djEvc3RhdHVzX2xpc3QifX19.7n4vE60lisFLnEFhVwYMOPh5loyLLtPc07sCvaFI-Ik"  # noqa: S105  # gitleaks:allow

# JSON payload of id token
ID_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhY3IiOiJiYXNpYyIsImFtciI6IjEwIiwiYXVkIjpbIjViNDQ4N2M0LThkYjEtNDA5ZC1hNjUzLWY5MDdiODA5NDAzOSJdLCJleHAiOjE3MjQ4MzU4NTksImlhdCI6MTcyNDgzMjI1OSwic3ViIjoiYm9HOGRmYzVNS1RuMzdvN2dzZENleXFMOExwV1F0Z29PNDFtMUtad2RxMCIsImlzcyI6Imh0dHBzOi8vdGVzdC5qYW5zLm9yZyIsImp0aSI6InNrM1Q0ME5ZU1l1azVzYUhaTnBrWnciLCJub25jZSI6ImMzODcyYWY5LWEwZjUtNGMzZi1hMWFmLWY5ZDBlODg0NmU4MSIsInNpZCI6IjZhN2ZlNTBhLWQ4MTAtNDU0ZC1iZTVkLTU0OWQyOTU5NWEwOSIsImphbnNPcGVuSURDb25uZWN0VmVyc2lvbiI6Im9wZW5pZGNvbm5lY3QtMS4wIiwiY19oYXNoIjoicEdvSzZZX1JLY1dIa1VlY005dXc2USIsImF1dGhfdGltZSI6MTcyNDgzMDc0NiwiZ3JhbnQiOiJhdXRob3JpemF0aW9uX2NvZGUiLCJzdGF0dXMiOnsic3RhdHVzX2xpc3QiOnsiaWR4IjoyMDIsInVyaSI6Imh0dHBzOi8vdGVzdC5qYW5zLm9yZy9qYW5zLWF1dGgvcmVzdHYxL3N0YXR1c19saXN0In19LCJyb2xlIjoiQWRtaW4ifQ.RgCuWFUUjPVXmbW3ExQavJZH8Lw4q3kGhMFBRR0hSjA"  # noqa: S105  # gitleaks:allow

# JSON payload of userinfo token
USERINFO_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb3VudHJ5IjoiVVMiLCJlbWFpbCI6InVzZXJAZXhhbXBsZS5jb20iLCJ1c2VybmFtZSI6IlVzZXJOYW1lRXhhbXBsZSIsInN1YiI6ImJvRzhkZmM1TUtUbjM3bzdnc2RDZXlxTDhMcFdRdGdvTzQxbTFLWndkcTAiLCJpc3MiOiJodHRwczovL3Rlc3QuamFucy5vcmciLCJnaXZlbl9uYW1lIjoiQWRtaW4iLCJtaWRkbGVfbmFtZSI6IkFkbWluIiwiaW51bSI6IjhkMWNkZTZhLTE0NDctNDc2Ni1iM2M4LTE2NjYzZTEzYjQ1OCIsImF1ZCI6IjViNDQ4N2M0LThkYjEtNDA5ZC1hNjUzLWY5MDdiODA5NDAzOSIsInVwZGF0ZWRfYXQiOjE3MjQ3Nzg1OTEsIm5hbWUiOiJEZWZhdWx0IEFkbWluIFVzZXIiLCJuaWNrbmFtZSI6IkFkbWluIiwiZmFtaWx5X25hbWUiOiJVc2VyIiwianRpIjoiZmFpWXZhWUlUMGNEQVQ3Rm93MHBRdyIsImphbnNBZG1pblVJUm9sZSI6WyJhcGktYWRtaW4iXSwiZXhwIjoxNzI0OTQ1OTc4fQ.t6p8fYAe1NkUt9mn9n9MYJlNCni8JYfhk-82hb_C1O4"  # noqa: S105  # gitleaks:allow

# Create resource with type "Jans::Issue" from cedar-policy schema.
RESOURCE = EntityData.from_dict({
    "cedar_entity_mapping": {
        "entity_type": "Jans::Issue",
        "id": "random_id"
    },
    "org_id": "some_long_id",
    "country": "US"
})


def get_multi_issuer_config():
    """Helper to configure Cedarling for multi-issuer tests"""
    def configure_for_multi_issuer(config):
        config["CEDARLING_JWT_SIG_VALIDATION"] = "disabled"
        config["CEDARLING_JWT_STATUS_VALIDATION"] = "disabled"
        config["CEDARLING_ID_TOKEN_TRUST_MODE"] = "never"
        # Use the multi-issuer test policy store
        config["CEDARLING_POLICY_STORE_LOCAL_FN"] = "../../test_files/policy-store-multi-issuer-test.yaml"
        # Disable workload and user entity building for multi-issuer tests
        # since the policies work with token entities in context, not principal entities
        config["CEDARLING_ENTITY_BUILDER_BUILD_WORKLOAD"] = "false"
        config["CEDARLING_ENTITY_BUILDER_BUILD_USER"] = "false"
        config["CEDARLING_AUTHORIZATION_USE_WORKLOAD_PRINCIPAL"] = "false"
        config["CEDARLING_AUTHORIZATION_USE_USER_PRINCIPAL"] = "false"
    return configure_for_multi_issuer


def test_single_token_authorization():
    '''
    Test single token authorization (matches Rust test_single_acme_access_token_authorization).
    Verifies basic multi-issuer authorization with a single token.
    '''
    instance = Cedarling(load_bootstrap_config(config_cb=get_multi_issuer_config()))
    
    # Create request with single access token
    request = AuthorizeMultiIssuerRequest(
        tokens=[TokenInput(mapping="Jans::Access_Token", payload=ACCESS_TOKEN)],
        action='Jans::Action::"Update"',
        context={},
        resource=RESOURCE,
    )

    result = instance.authorize_multi_issuer(request)

    assert result is not None, "Result should not be None"
    assert result.is_allowed() is True, "Authorization should be ALLOW for single token"
    assert result.request_id() != "", "request_id should be present"


def test_multiple_tokens_from_different_issuers():
    '''
    Test multiple tokens from different issuers (matches Rust test_multiple_tokens_from_different_issuers).
    Verifies multi-issuer authorization with multiple tokens.
    '''
    instance = Cedarling(load_bootstrap_config(config_cb=get_multi_issuer_config()))
    
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

    assert result is not None, "Result should not be None"
    assert result.is_allowed() is True, "Authorization should be ALLOW for multi-token request"
    assert result.request_id() != "", "request_id should be present"


def test_validation_empty_token_array():
    '''
    Test validation - empty token array (matches Rust test_validation_empty_token_array).
    Verifies that empty token array raises an error.
    '''
    instance = Cedarling(load_bootstrap_config(config_cb=get_multi_issuer_config()))
    
    request = AuthorizeMultiIssuerRequest(
        tokens=[],
        action='Jans::Action::"Update"',
        context={},
        resource=RESOURCE,
    )

    with pytest.raises(authorize_errors.MultiIssuerValidationError):
        instance.authorize_multi_issuer(request)


def test_validation_graceful_degradation_invalid_token():
    '''
    Test validation - graceful degradation when invalid token is present 
    (matches Rust test_validation_graceful_degradation_invalid_token).
    Verifies that invalid tokens are ignored and valid tokens are processed.
    '''
    instance = Cedarling(load_bootstrap_config(config_cb=get_multi_issuer_config()))
    
    # Create request with both valid and invalid tokens
    tokens = [
        TokenInput(mapping="Jans::Access_Token", payload=ACCESS_TOKEN),  # Valid token
        TokenInput(mapping="Invalid::Token", payload="not-a-valid-jwt"),  # Invalid JWT
    ]

    request = AuthorizeMultiIssuerRequest(
        tokens=tokens,
        action='Jans::Action::"Update"',
        context={},
        resource=RESOURCE,
    )

    # Graceful degradation: invalid tokens are ignored, valid tokens are processed
    result = instance.authorize_multi_issuer(request)
    
    assert result is not None, "Should succeed gracefully when some tokens are invalid"
    assert result.is_allowed() is True, "Should be ALLOW - valid token has required attributes despite invalid token"
    assert result.request_id() != "", "request_id should be present"
