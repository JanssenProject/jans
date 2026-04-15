# This software is available under the Apache-2.0 license.
# See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
#
# Copyright (c) 2024, Gluu, Inc.

"""Tests for authorize_unsigned covering the single-principal and no-principal
(partial-evaluation) paths."""

from cedarling_python import Cedarling, EntityData, RequestUnsigned
from config import load_bootstrap_config, TEST_FILES_PATH
from os.path import join


# Policy store without trusted issuers. Contains:
#   - policy 5: permit UpdateForTestPrincipals when principal.is_ok
#   - policy 6: allow-all permit for OpenPublicIssue (action uses `principal: [Any]`).
POLICY_STORE_LOCATION = join(TEST_FILES_PATH, "policy-store_no_trusted_issuers.yaml")


RESOURCE = EntityData.from_dict({
    "cedar_entity_mapping": {
        "entity_type": "Jans::Issue",
        "id": "random_id",
    },
    "org_id": "some_long_id",
    "country": "US",
})


def test_authorize_unsigned_single_principal_allow():
    """A concrete principal satisfying the `is_ok` guard should be allowed."""
    instance = Cedarling(load_bootstrap_config(POLICY_STORE_LOCATION))

    request = RequestUnsigned(
        principal=EntityData.from_dict({
            "cedar_entity_mapping": {
                "entity_type": "Jans::TestPrincipal1",
                "id": "1",
            },
            "is_ok": True,
        }),
        action='Jans::Action::"UpdateForTestPrincipals"',
        context={},
        resource=RESOURCE,
    )

    result = instance.authorize_unsigned(request)
    assert result.is_allowed()


def test_authorize_unsigned_no_principal_public_action_allow():
    """With `principal=None` and an allow-all action (`principal: [Any]` in the
    schema), partial evaluation resolves to Allow."""
    instance = Cedarling(load_bootstrap_config(POLICY_STORE_LOCATION))

    request = RequestUnsigned(
        action='Jans::Action::"OpenPublicIssue"',
        context={},
        resource=RESOURCE,
        principal=None,
    )

    result = instance.authorize_unsigned(request)
    assert result.is_allowed()


def test_authorize_unsigned_no_principal_principal_dependent_deny():
    """With `principal=None` against a principal-dependent policy, the request
    fails closed and the residual policy id surfaces in the diagnostics."""
    instance = Cedarling(load_bootstrap_config(POLICY_STORE_LOCATION))

    request = RequestUnsigned(
        action='Jans::Action::"UpdateForTestPrincipals"',
        context={},
        resource=RESOURCE,
        principal=None,
    )

    result = instance.authorize_unsigned(request)
    assert not result.is_allowed()
    reasons = result.response.diagnostics.reason
    assert "5" in reasons
