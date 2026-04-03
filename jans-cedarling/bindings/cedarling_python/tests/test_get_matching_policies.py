# This software is available under the Apache-2.0 license.
# See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
#
# Copyright (c) 2024, Gluu, Inc.

from cedarling_python import Cedarling, PolicyEffect, PolicyMetadata
from cedarling_python import EntityData, TokenInput
from config import load_bootstrap_config, TEST_FILES_PATH
from os.path import join

# in fixture `load_bootstrap_config` we use policy store `policy-store_ok.json`
# The human-readable policy and schema file is located in next folder:
# `test_files\policy-store_ok_2.yaml`

POLICY_STORE_LOCATION = join(TEST_FILES_PATH, "policy-store_ok_2.yaml")

# Create resource with type "Jans::Issue" from cedar-policy schema.
RESOURCE = EntityData.from_dict(
    {
        "cedar_entity_mapping": {"entity_type": "Jans::Issue", "id": "random_id"},
        "org_id": "some_long_id",
        "country": "US",
    }
)


def create_instance():
    return Cedarling(load_bootstrap_config(POLICY_STORE_LOCATION))


def test_get_matching_policies_unsigned_returns_matching():
    """Test that get_matching_policies_unsigned returns policies matching the given scope."""
    instance = create_instance()

    principals = [
        EntityData.from_dict(
            {
                "cedar_entity_mapping": {
                    "entity_type": "Jans::TestPrincipal1",
                    "id": "1",
                },
                "is_ok": True,
            }
        )
    ]

    actions = ['Jans::Action::"UpdateForTestPrincipals"']
    resources = [RESOURCE]

    policies = instance.get_matching_policies_unsigned(principals, actions, resources)

    assert len(policies) > 0, "should return at least one matching policy"
    for policy in policies:
        assert isinstance(policy, PolicyMetadata)
        assert isinstance(policy.id, str)
        assert len(policy.id) > 0
        assert isinstance(policy.effect, PolicyEffect)
        assert isinstance(policy.annotations, dict)
        assert isinstance(policy.source, str)
        assert len(policy.source) > 0


def test_get_matching_policies_unsigned_effect_is_permit():
    """Test that the matching policy has a permit effect."""
    instance = create_instance()

    principals = [
        EntityData.from_dict(
            {
                "cedar_entity_mapping": {
                    "entity_type": "Jans::TestPrincipal1",
                    "id": "1",
                },
                "is_ok": True,
            }
        )
    ]

    policies = instance.get_matching_policies_unsigned(
        principals,
        ['Jans::Action::"UpdateForTestPrincipals"'],
        [RESOURCE],
    )

    assert len(policies) > 0
    assert policies[0].effect == PolicyEffect.Permit


def test_get_matching_policies_unsigned_no_match():
    """Test that non-matching action returns no policies."""
    instance = create_instance()

    principals = [
        EntityData.from_dict(
            {
                "cedar_entity_mapping": {
                    "entity_type": "Jans::TestPrincipal1",
                    "id": "1",
                },
                "is_ok": True,
            }
        )
    ]

    policies = instance.get_matching_policies_unsigned(
        principals,
        ['Jans::Action::"NonExistentAction"'],
        [RESOURCE],
    )

    assert len(policies) == 0, "should return no policies for non-matching action"


def test_get_matching_policies_unsigned_multiple_principals():
    """Test with multiple principals."""
    instance = create_instance()

    principals = [
        EntityData.from_dict(
            {
                "cedar_entity_mapping": {
                    "entity_type": "Jans::TestPrincipal1",
                    "id": "1",
                },
                "is_ok": True,
            }
        ),
        EntityData.from_dict(
            {
                "cedar_entity_mapping": {
                    "entity_type": "Jans::TestPrincipal2",
                    "id": "2",
                },
                "is_ok": True,
            }
        ),
    ]

    policies = instance.get_matching_policies_unsigned(
        principals,
        ['Jans::Action::"UpdateForTestPrincipals"'],
        [RESOURCE],
    )

    assert len(policies) > 0, "should return matching policies for multiple principals"


def test_policy_metadata_repr():
    """Test that PolicyMetadata has a useful repr."""
    instance = create_instance()

    principals = [
        EntityData.from_dict(
            {
                "cedar_entity_mapping": {
                    "entity_type": "Jans::TestPrincipal1",
                    "id": "1",
                },
                "is_ok": True,
            }
        )
    ]

    policies = instance.get_matching_policies_unsigned(
        principals,
        ['Jans::Action::"UpdateForTestPrincipals"'],
        [RESOURCE],
    )

    assert len(policies) > 0
    repr_str = repr(policies[0])
    assert "PolicyMetadata" in repr_str
    assert "PolicyEffect" in repr_str


def test_policy_effect_enum_values():
    """Test PolicyEffect enum has correct values and string representations."""
    assert str(PolicyEffect.Permit) == "permit"
    assert str(PolicyEffect.Forbid) == "forbid"
    assert PolicyEffect.Permit != PolicyEffect.Forbid
    assert PolicyEffect.Permit == PolicyEffect.Permit


# --- Multi-issuer tests ---

# HS256-signed JWTs with validation disabled in config.
# access_token payload includes: iss=https://test.jans.org, client_id, org_id, scope, etc.
# {
#   "sub": "boG8dfc5MKTn37o7gsdCeyqL8LpWQtgoO41m1KZwdq0",
#   "code": "bf1934f6-3905-420a-8299-6b2e3ffddd6e",
#   "iss": "https://test.jans.org",
#   "token_type": "Bearer",
#   "client_id": "5b4487c4-8db1-409d-a653-f907b8094039",
#   "aud": "5b4487c4-8db1-409d-a653-f907b8094039",
#   "acr": "basic",
#   "x5t#S256": "",
#   "scope": [
#     "openid",
#     "profile"
#   ],
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
MULTI_ISSUER_ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJib0c4ZGZjNU1LVG4zN283Z3NkQ2V5cUw4THBXUXRnb080MW0xS1p3ZHEwIiwiY29kZSI6ImJmMTkzNGY2LTM5MDUtNDIwYS04Mjk5LTZiMmUzZmZkZGQ2ZSIsImlzcyI6Imh0dHBzOi8vdGVzdC5qYW5zLm9yZyIsInRva2VuX3R5cGUiOiJCZWFyZXIiLCJjbGllbnRfaWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhdWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhY3IiOiJiYXNpYyIsIng1dCNTMjU2IjoiIiwic2NvcGUiOlsib3BlbmlkIiwicHJvZmlsZSJdLCJvcmdfaWQiOiJzb21lX2xvbmdfaWQiLCJhdXRoX3RpbWUiOjE3MjQ4MzA3NDYsImV4cCI6MTcyNDk0NTk3OCwiaWF0IjoxNzI0ODMyMjU5LCJqdGkiOiJseFRtQ1ZSRlR4T2pKZ3ZFRXBvek1RIiwibmFtZSI6IkRlZmF1bHQgQWRtaW4gVXNlciIsInN0YXR1cyI6eyJzdGF0dXNfbGlzdCI6eyJpZHgiOjIwMSwidXJpIjoiaHR0cHM6Ly90ZXN0LmphbnMub3JnL2phbnMtYXV0aC9yZXN0djEvc3RhdHVzX2xpc3QifX19.7n4vE60lisFLnEFhVwYMOPh5loyLLtPc07sCvaFI-Ik"  # noqa: S105  # gitleaks:allow

# userinfo_token payload includes: iss=https://test.jans.org, country=US, email, username, etc.
# {
#   "country": "US",
#   "email": "user@example.com",
#   "username": "UserNameExample",
#   "sub": "boG8dfc5MKTn37o7gsdCeyqL8LpWQtgoO41m1KZwdq0",
#   "iss": "https://test.jans.org",
#   "given_name": "Admin",
#   "middle_name": "Admin",
#   "inum": "8d1cde6a-1447-4766-b3c8-16663e13b458",
#   "aud": "5b4487c4-8db1-409d-a653-f907b8094039",
#   "updated_at": 1724778591,
#   "name": "Default Admin User",
#   "nickname": "Admin",
#   "family_name": "User",
#   "jti": "faiYvaYIT0cDAT7Fow0pQw",
#   "jansAdminUIRole": [
#     "api-admin"
#   ],
#   "exp": 1724945978
# }
MULTI_ISSUER_USERINFO_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb3VudHJ5IjoiVVMiLCJlbWFpbCI6InVzZXJAZXhhbXBsZS5jb20iLCJ1c2VybmFtZSI6IlVzZXJOYW1lRXhhbXBsZSIsInN1YiI6ImJvRzhkZmM1TUtUbjM3bzdnc2RDZXlxTDhMcFdRdGdvTzQxbTFLWndkcTAiLCJpc3MiOiJodHRwczovL3Rlc3QuamFucy5vcmciLCJnaXZlbl9uYW1lIjoiQWRtaW4iLCJtaWRkbGVfbmFtZSI6IkFkbWluIiwiaW51bSI6IjhkMWNkZTZhLTE0NDctNDc2Ni1iM2M4LTE2NjYzZTEzYjQ1OCIsImF1ZCI6IjViNDQ4N2M0LThkYjEtNDA5ZC1hNjUzLWY5MDdiODA5NDAzOSIsInVwZGF0ZWRfYXQiOjE3MjQ3Nzg1OTEsIm5hbWUiOiJEZWZhdWx0IEFkbWluIFVzZXIiLCJuaWNrbmFtZSI6IkFkbWluIiwiZmFtaWx5X25hbWUiOiJVc2VyIiwianRpIjoiZmFpWXZhWUlUMGNEQVQ3Rm93MHBRdyIsImphbnNBZG1pblVJUm9sZSI6WyJhcGktYWRtaW4iXSwiZXhwIjoxNzI0OTQ1OTc4fQ.t6p8fYAe1NkUt9mn9n9MYJlNCni8JYfhk-82hb_C1O4"  # noqa: S105  # gitleaks:allow

MULTI_ISSUER_RESOURCE = EntityData.from_dict(
    {
        "cedar_entity_mapping": {"entity_type": "Jans::Issue", "id": "random_id"},
        "org_id": "some_long_id",
        "country": "US",
    }
)

MULTI_ISSUER_POLICY_STORE = join(TEST_FILES_PATH, "policy-store-multi-issuer-test.yaml")


def create_multi_issuer_instance():
    def configure(config):
        config["CEDARLING_JWT_SIG_VALIDATION"] = "disabled"
        config["CEDARLING_JWT_STATUS_VALIDATION"] = "disabled"
        config["CEDARLING_POLICY_STORE_LOCAL_FN"] = MULTI_ISSUER_POLICY_STORE

    return Cedarling(load_bootstrap_config(config_cb=configure))


def test_get_matching_policies_multi_issuer_returns_matching():
    """Test that get_matching_policies_multi_issuer returns policies matching the scope."""
    instance = create_multi_issuer_instance()

    tokens = [
        TokenInput(mapping="Jans::Access_Token", payload=MULTI_ISSUER_ACCESS_TOKEN),
    ]
    actions = ['Jans::Action::"Update"']
    resources = [MULTI_ISSUER_RESOURCE]

    policies = instance.get_matching_policies_multi_issuer(tokens, actions, resources)

    assert len(policies) > 0, "should return at least one matching policy"
    for policy in policies:
        assert isinstance(policy, PolicyMetadata)
        assert isinstance(policy.id, str)
        assert len(policy.id) > 0
        assert isinstance(policy.effect, PolicyEffect)
        assert isinstance(policy.annotations, dict)
        assert isinstance(policy.source, str)
        assert len(policy.source) > 0


def test_get_matching_policies_multi_issuer_effect_is_permit():
    """Test that the matching multi-issuer policies have a permit effect."""
    instance = create_multi_issuer_instance()

    tokens = [
        TokenInput(mapping="Jans::Access_Token", payload=MULTI_ISSUER_ACCESS_TOKEN),
    ]

    policies = instance.get_matching_policies_multi_issuer(
        tokens,
        ['Jans::Action::"Update"'],
        [MULTI_ISSUER_RESOURCE],
    )

    assert len(policies) > 0
    for policy in policies:
        assert policy.effect == PolicyEffect.Permit


def test_get_matching_policies_multi_issuer_no_match():
    """Test that non-matching action returns no policies for multi-issuer."""
    instance = create_multi_issuer_instance()

    tokens = [
        TokenInput(mapping="Jans::Access_Token", payload=MULTI_ISSUER_ACCESS_TOKEN),
    ]

    policies = instance.get_matching_policies_multi_issuer(
        tokens,
        ['Jans::Action::"NonExistentAction"'],
        [MULTI_ISSUER_RESOURCE],
    )

    assert len(policies) == 0, "should return no policies for non-matching action"


def test_get_matching_policies_multi_issuer_multiple_tokens():
    """Test with multiple tokens from the same issuer."""
    instance = create_multi_issuer_instance()

    tokens = [
        TokenInput(mapping="Jans::Access_Token", payload=MULTI_ISSUER_ACCESS_TOKEN),
        TokenInput(mapping="Jans::Userinfo_Token", payload=MULTI_ISSUER_USERINFO_TOKEN),
    ]

    policies = instance.get_matching_policies_multi_issuer(
        tokens,
        ['Jans::Action::"Update"'],
        [MULTI_ISSUER_RESOURCE],
    )

    assert len(policies) > 0, "should return matching policies with multiple tokens"
    for policy in policies:
        assert isinstance(policy, PolicyMetadata)
        assert policy.effect == PolicyEffect.Permit
