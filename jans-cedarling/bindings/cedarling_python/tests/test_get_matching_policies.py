# This software is available under the Apache-2.0 license.
# See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
#
# Copyright (c) 2024, Gluu, Inc.

from cedarling_python import Cedarling, PolicyEffect, PolicyMetadata
from cedarling_python import EntityData
from config import load_bootstrap_config, TEST_FILES_PATH
from os.path import join

# in fixture `load_bootstrap_config` we use policy store `policy-store_ok.json`
# The human-readable policy and schema file is located in next folder:
# `test_files\policy-store_ok_2.yaml`

POLICY_STORE_LOCATION = join(TEST_FILES_PATH, "policy-store_ok_2.yaml")

# Create resource with type "Jans::Issue" from cedar-policy schema.
RESOURCE = EntityData.from_dict({
    "cedar_entity_mapping": {
        "entity_type": "Jans::Issue",
        "id": "random_id"
    },
    "org_id": "some_long_id",
    "country": "US"
})


def create_instance():
    return Cedarling(load_bootstrap_config(POLICY_STORE_LOCATION))


def test_get_matching_policies_unsigned_returns_matching():
    """Test that get_matching_policies_unsigned returns policies matching the given scope."""
    instance = create_instance()

    principals = [EntityData.from_dict({
        "cedar_entity_mapping": {
            "entity_type": "Jans::TestPrincipal1",
            "id": "1"
        },
        "is_ok": True
    })]

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

    principals = [EntityData.from_dict({
        "cedar_entity_mapping": {
            "entity_type": "Jans::TestPrincipal1",
            "id": "1"
        },
        "is_ok": True
    })]

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

    principals = [EntityData.from_dict({
        "cedar_entity_mapping": {
            "entity_type": "Jans::TestPrincipal1",
            "id": "1"
        },
        "is_ok": True
    })]

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
        EntityData.from_dict({
            "cedar_entity_mapping": {
                "entity_type": "Jans::TestPrincipal1",
                "id": "1"
            },
            "is_ok": True
        }),
        EntityData.from_dict({
            "cedar_entity_mapping": {
                "entity_type": "Jans::TestPrincipal2",
                "id": "2"
            },
            "is_ok": True
        }),
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

    principals = [EntityData.from_dict({
        "cedar_entity_mapping": {
            "entity_type": "Jans::TestPrincipal1",
            "id": "1"
        },
        "is_ok": True
    })]

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
