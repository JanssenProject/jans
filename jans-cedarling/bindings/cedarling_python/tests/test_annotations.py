# This software is available under the Apache-2.0 license.
# See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
#
# Copyright (c) 2024, Gluu, Inc.

"""Tests for the policy annotation lookup API (annotations_map,
annotation_values, annotations_by_policy).

Policy 5 in `policy-store_ok_2.yaml` carries `@redirect("/upgrade")` and
`@tier("premium")` annotations.
"""

from cedarling_python import Cedarling, EntityData, RequestUnsigned
from config import load_bootstrap_config, TEST_FILES_PATH
from os.path import join

POLICY_STORE_LOCATION = join(TEST_FILES_PATH, "policy-store_ok_2.yaml")

RESOURCE = EntityData.from_dict(
    {
        "cedar_entity_mapping": {"entity_type": "Jans::Issue", "id": "random_id"},
        "org_id": "some_long_id",
        "country": "US",
    }
)


def create_instance():
    return Cedarling(load_bootstrap_config(POLICY_STORE_LOCATION))


def authorize_and_get_reason(instance):
    """Authorize an allowed request and return the determining policy IDs."""
    request = RequestUnsigned(
        principal=EntityData.from_dict(
            {
                "cedar_entity_mapping": {
                    "entity_type": "Jans::TestPrincipal1",
                    "id": "1",
                },
                "is_ok": True,
            }
        ),
        action='Jans::Action::"UpdateForTestPrincipals"',
        context={},
        resource=RESOURCE,
    )

    result = instance.authorize_unsigned(request)
    assert result.is_allowed()

    reason = list(result.response.diagnostics.reason)
    assert len(reason) > 0, "allow decision should have a reason set"
    return reason


def test_annotations_map_from_reason():
    """Annotations of the determining policies surface in the merged map."""
    instance = create_instance()
    reason = authorize_and_get_reason(instance)

    annotations = instance.annotations_map(reason)

    assert annotations["redirect"] == "/upgrade"
    assert annotations["tier"] == "premium"


def test_annotation_values_from_reason():
    """annotation_values returns every value of the given key."""
    instance = create_instance()
    reason = authorize_and_get_reason(instance)

    assert instance.annotation_values(reason, "redirect") == ["/upgrade"]
    assert instance.annotation_values(reason, "absent_key") == []


def test_annotations_by_policy_from_reason():
    """annotations_by_policy groups annotations per policy ID."""
    instance = create_instance()
    reason = authorize_and_get_reason(instance)

    by_policy = instance.annotations_by_policy(reason)

    assert "5" in by_policy, f"policy 5 should be a determining policy, got: {by_policy}"
    assert by_policy["5"]["redirect"] == "/upgrade"
    assert by_policy["5"]["tier"] == "premium"


def test_unknown_policy_ids_are_skipped():
    """IDs not present in the policy store are silently dropped."""
    instance = create_instance()

    assert instance.annotations_map(["no_such_policy"]) == {}
    assert instance.annotation_values(["no_such_policy"], "redirect") == []
    assert instance.annotations_by_policy(["no_such_policy"]) == {}
