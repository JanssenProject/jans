# This software is available under the Apache-2.0 license.
# See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
#
# Copyright (c) 2024, Gluu, Inc.

"""Tests for authorization API (authorize_unsigned; legacy authorize/Request removed)."""

from cedarling_python import Cedarling, EntityData, RequestUnsigned, authorize_errors
from config import load_bootstrap_config, TEST_FILES_PATH
from os.path import join


POLICY_STORE_LOCATION = join(TEST_FILES_PATH, "policy-store_ok_2.yaml")

RESOURCE = EntityData.from_dict({
    "cedar_entity_mapping": {
        "entity_type": "Jans::Issue",
        "id": "random_id"
    },
    "org_id": "some_long_id",
    "country": "US"
})


def test_authorize_errors_exist():
    """Smoke test: authorize_errors module and base exception exist."""
    assert hasattr(authorize_errors, "AuthorizeError")
    assert issubclass(authorize_errors.AuthorizeError, Exception)


def test_authorize_unsigned_ok():
    """Test authorize_unsigned with a minimal single-principal request."""
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

    authorize_result = instance.authorize_unsigned(request)
    assert authorize_result.is_allowed()
