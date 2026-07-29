# This software is available under the Apache-2.0 license.
# See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
#
# Copyright (c) 2024, Gluu, Inc.

"""Tests for the batch unsigned authorize entry point."""

from cedarling_python import (
    BatchAuthorizeUnsignedRequest,
    BatchItem,
    Cedarling,
    EntityData,
)
from cedarling_python import authorize_errors
from config import load_bootstrap_config, TEST_FILES_PATH
from os.path import join
import pytest


POLICY_STORE_LOCATION = join(TEST_FILES_PATH, "policy-store_no_trusted_issuers.yaml")


def _resource(entity_id: str = "random_id") -> EntityData:
    return EntityData.from_dict(
        {
            "cedar_entity_mapping": {"entity_type": "Jans::Issue", "id": entity_id},
            "org_id": "some_long_id",
            "country": "US",
        }
    )


def _principal(is_ok: bool) -> EntityData:
    return EntityData.from_dict(
        {
            "cedar_entity_mapping": {"entity_type": "Jans::TestPrincipal1", "id": "1"},
            "is_ok": is_ok,
        }
    )


def _item(entity_id: str) -> BatchItem:
    return BatchItem(
        resource=_resource(entity_id),
        action='Jans::Action::"UpdateForTestPrincipals"',
        context={},
    )


def test_batch_unsigned_single_item_allow():
    """N=1 batch behaves like a single-item call."""
    instance = Cedarling(load_bootstrap_config(POLICY_STORE_LOCATION))

    request = BatchAuthorizeUnsignedRequest(
        items=[_item("only")],
        principal=_principal(True),
    )
    response = instance.authorize_unsigned_batch(request)

    assert len(response.results) == 1
    assert response.results[0].is_ok()
    assert response.results[0].unwrap().is_allowed()
    assert response.batch_id  # non-empty


def test_batch_unsigned_n5_ordered_all_allow():
    """N=5 items, all allowed. Ordering is preserved 1:1."""
    instance = Cedarling(load_bootstrap_config(POLICY_STORE_LOCATION))

    request = BatchAuthorizeUnsignedRequest(
        items=[_item(f"res-{i}") for i in range(5)],
        principal=_principal(True),
    )
    response = instance.authorize_unsigned_batch(request)

    assert len(response.results) == 5
    for i, result in enumerate(response.results):
        assert result.is_ok(), f"item {i} should be Ok"
        assert result.unwrap().is_allowed(), f"item {i} should allow"


def test_batch_unsigned_deny_propagates_per_item():
    """`is_ok=False` principal denies every item."""
    instance = Cedarling(load_bootstrap_config(POLICY_STORE_LOCATION))

    request = BatchAuthorizeUnsignedRequest(
        items=[_item("a"), _item("b")],
        principal=_principal(False),
    )
    response = instance.authorize_unsigned_batch(request)

    assert len(response.results) == 2
    for result in response.results:
        assert result.is_ok()
        assert not result.unwrap().is_allowed()


def test_batch_unsigned_no_principal_residual_denies_that_item_only():
    """`principal=None` with a residual-dependent policy fails closed on that
    item; a public action next to it still allows."""
    instance = Cedarling(load_bootstrap_config(POLICY_STORE_LOCATION))

    public_item = BatchItem(
        resource=_resource("public"),
        action='Jans::Action::"OpenPublicIssue"',
        context={},
    )
    private_item = _item("private")

    request = BatchAuthorizeUnsignedRequest(
        items=[public_item, private_item],
        principal=None,
    )
    response = instance.authorize_unsigned_batch(request)

    assert len(response.results) == 2
    assert response.results[0].is_ok()
    assert response.results[0].unwrap().is_allowed(), "public action must allow"
    assert response.results[1].is_ok()
    assert not response.results[1].unwrap().is_allowed(), (
        "principal-dependent action must deny"
    )


def test_batch_unsigned_empty_items_rejected():
    """Empty items list is rejected at validate-time."""
    instance = Cedarling(load_bootstrap_config(POLICY_STORE_LOCATION))

    request = BatchAuthorizeUnsignedRequest(items=[], principal=_principal(True))
    with pytest.raises(authorize_errors.BatchValidationError):
        instance.authorize_unsigned_batch(request)


def test_batch_unsigned_context_defaults_when_omitted():
    """`context=None` on a BatchItem defaults to {} at conversion time."""
    instance = Cedarling(load_bootstrap_config(POLICY_STORE_LOCATION))

    item = BatchItem(
        resource=_resource("no-ctx"),
        action='Jans::Action::"UpdateForTestPrincipals"',
    )  # context omitted → None → default {}
    request = BatchAuthorizeUnsignedRequest(items=[item], principal=_principal(True))
    response = instance.authorize_unsigned_batch(request)

    assert len(response.results) == 1
    assert response.results[0].is_ok()
    assert response.results[0].unwrap().is_allowed()


def test_batch_unsigned_bad_action_surfaces_error_only_at_that_item():
    """A per-item malformed action UID surfaces as a BatchItemError at that
    position; adjacent items still evaluate cleanly."""
    instance = Cedarling(load_bootstrap_config(POLICY_STORE_LOCATION))

    bad = BatchItem(
        resource=_resource("bad"),
        action="this is not a valid uid",
        context={},
    )
    request = BatchAuthorizeUnsignedRequest(
        items=[_item("ok-0"), bad, _item("ok-2")],
        principal=_principal(True),
    )
    response = instance.authorize_unsigned_batch(request)

    assert len(response.results) == 3
    assert response.results[0].is_ok()
    assert response.results[0].unwrap().is_allowed(), "item 0 allowed"
    assert not response.results[1].is_ok(), "item 1 must be Err"
    err = response.results[1].error
    assert err is not None
    assert err.category == "action_parse"
    assert err.item_index == 1
    assert response.results[2].is_ok()
    assert response.results[2].unwrap().is_allowed(), "item 2 allowed"
