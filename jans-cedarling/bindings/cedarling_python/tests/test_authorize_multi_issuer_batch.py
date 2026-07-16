# This software is available under the Apache-2.0 license.
# See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
#
# Copyright (c) 2024, Gluu, Inc.

"""Tests for the batch multi-issuer authorize entry point."""

from cedarling_python import (
    BatchAuthorizeMultiIssuerRequest,
    BatchItem,
    Cedarling,
    EntityData,
    TokenInput,
)
from cedarling_python import authorize_errors
from config import load_bootstrap_config
import pytest


# Reuse the same test tokens as the single-item multi-issuer tests. Payloads
# are identical to keep the two suites in lockstep — any token-rotation fix
# will apply here too.
from test_authorize_multi_issuer import ACCESS_TOKEN


def _multi_issuer_config():
    def configure(config) -> None:
        config["CEDARLING_JWT_SIG_VALIDATION"] = "disabled"
        config["CEDARLING_JWT_STATUS_VALIDATION"] = "disabled"
        config["CEDARLING_POLICY_STORE_LOCAL_FN"] = (
            "../../test_files/policy-store-multi-issuer-test.yaml"
        )

    return configure


def _resource(entity_id: str = "random_id") -> EntityData:
    return EntityData.from_dict(
        {
            "cedar_entity_mapping": {"entity_type": "Jans::Issue", "id": entity_id},
            "org_id": "some_long_id",
            "country": "US",
        }
    )


def _item(entity_id: str) -> BatchItem:
    return BatchItem(
        resource=_resource(entity_id),
        action='Jans::Action::"Update"',
        context={},
    )


def _tokens():
    return [TokenInput(mapping="Jans::Access_Token", payload=ACCESS_TOKEN)]


def test_batch_multi_issuer_single_item():
    """N=1 batch behaves like a single-item call."""
    instance = Cedarling(load_bootstrap_config(config_cb=_multi_issuer_config()))

    request = BatchAuthorizeMultiIssuerRequest(
        tokens=_tokens(),
        items=[_item("only")],
    )
    response = instance.authorize_multi_issuer_batch(request)

    assert len(response.results) == 1
    assert response.results[0].is_allowed(), "single item should allow"
    assert response.batch_id


def test_batch_multi_issuer_n5_ordered():
    """N=5 items evaluated against the same token snapshot; ordering preserved
    and every item allows under the shared valid token set."""
    instance = Cedarling(load_bootstrap_config(config_cb=_multi_issuer_config()))

    request = BatchAuthorizeMultiIssuerRequest(
        tokens=_tokens(),
        items=[_item(f"res-{i}") for i in range(5)],
    )
    response = instance.authorize_multi_issuer_batch(request)

    assert len(response.results) == 5
    for i, r in enumerate(response.results):
        assert r.is_allowed(), f"item {i} should allow"


def test_batch_multi_issuer_empty_tokens_rejected():
    """Empty tokens list is rejected at validate-time."""
    instance = Cedarling(load_bootstrap_config(config_cb=_multi_issuer_config()))

    request = BatchAuthorizeMultiIssuerRequest(tokens=[], items=[_item("x")])
    with pytest.raises(authorize_errors.BatchValidationError):
        instance.authorize_multi_issuer_batch(request)


def test_batch_multi_issuer_empty_items_rejected():
    """Empty items list is rejected at validate-time."""
    instance = Cedarling(load_bootstrap_config(config_cb=_multi_issuer_config()))

    request = BatchAuthorizeMultiIssuerRequest(tokens=_tokens(), items=[])
    with pytest.raises(authorize_errors.BatchValidationError):
        instance.authorize_multi_issuer_batch(request)


def test_batch_multi_issuer_bad_action_denies_only_that_item():
    """A per-item malformed action UID synthesizes a Deny for that item but
    does not fail other items in the batch."""
    instance = Cedarling(load_bootstrap_config(config_cb=_multi_issuer_config()))

    bad = BatchItem(
        resource=_resource("bad"),
        action="this is not a valid uid",
        context={},
    )
    request = BatchAuthorizeMultiIssuerRequest(
        tokens=_tokens(),
        items=[_item("a"), bad, _item("c")],
    )
    response = instance.authorize_multi_issuer_batch(request)

    assert len(response.results) == 3
    assert response.results[0].is_allowed(), "item 0 allowed"
    assert not response.results[1].is_allowed(), (
        "item 1 with bad action must fail closed"
    )
    assert response.results[2].is_allowed(), "item 2 allowed"
