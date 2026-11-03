# This software is available under the Apache-2.0 license.
# See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
#
# Copyright (c) 2024, Gluu, Inc.

"""`AuthorizeOptions` travel with a request but never take part in the Cedar
evaluation, so a request gives the same decision with and without them."""

from os.path import join

import pytest

from cedarling_python import (
    AuthorizeMultiIssuerRequest,
    AuthorizeOptions,
    BatchAuthorizeMultiIssuerRequest,
    BatchAuthorizeUnsignedRequest,
    BatchItem,
    Cedarling,
    EntityData,
    RequestUnsigned,
    TokenInput,
    TraceContext,
)
from config import TEST_FILES_PATH, load_bootstrap_config
from test_authorize_multi_issuer import ACCESS_TOKEN
from test_authorize_multi_issuer_batch import _multi_issuer_config


UNSIGNED_POLICY_STORE = join(TEST_FILES_PATH, "policy-store_no_trusted_issuers.yaml")


def _trace_options() -> AuthorizeOptions:
    return AuthorizeOptions(
        trace=TraceContext(
            execution_id="exec-1",
            execution_authority="https://gateway.example.com",
            workload_id="workload-1",
            workload_instance_id="workload-1-a",
        )
    )


def _resource() -> EntityData:
    return EntityData.from_dict(
        {
            "cedar_entity_mapping": {"entity_type": "Jans::Issue", "id": "random_id"},
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


def _unsigned_request(is_ok: bool, options=None) -> RequestUnsigned:
    return RequestUnsigned(
        principal=_principal(is_ok),
        action='Jans::Action::"UpdateForTestPrincipals"',
        context={},
        resource=_resource(),
        options=options,
    )


@pytest.mark.parametrize("is_ok", [True, False])
def test_request_unsigned_with_trace_options(is_ok):
    """Options must not change the unsigned decision, allow or deny."""
    instance = Cedarling(load_bootstrap_config(UNSIGNED_POLICY_STORE))

    plain = instance.authorize_unsigned(_unsigned_request(is_ok))
    traced = instance.authorize_unsigned(_unsigned_request(is_ok, _trace_options()))

    assert plain.is_allowed() == is_ok, "baseline decision should follow is_ok"
    assert traced.is_allowed() == plain.is_allowed(), "options must not change the decision"


def test_batch_multi_issuer_request_accepts_options():
    """A multi-issuer batch with options gives the same per-item decisions."""
    instance = Cedarling(load_bootstrap_config(config_cb=_multi_issuer_config()))

    def request(options=None):
        return BatchAuthorizeMultiIssuerRequest(
            tokens=[TokenInput(mapping="Jans::Access_Token", payload=ACCESS_TOKEN)],
            items=[BatchItem(resource=_resource(), action='Jans::Action::"Update"', context={})],
            options=options,
        )

    def decisions(response):
        return [r.unwrap().is_allowed() for r in response.results]

    plain = instance.authorize_multi_issuer_batch(request())
    traced = instance.authorize_multi_issuer_batch(request(_trace_options()))

    assert decisions(plain) == [True], "baseline batch item should allow"
    assert decisions(traced) == decisions(plain), "options must not change batch decisions"


def test_trace_context_requires_mandatory_args():
    """execution_id, execution_authority and workload_id are mandatory."""
    with pytest.raises(TypeError, match="workload_id"):
        TraceContext(execution_id="exec-1", execution_authority="https://gateway.example.com")


def test_request_without_options_unchanged():
    """A request built without options has none and keeps the old decision."""
    instance = Cedarling(load_bootstrap_config(UNSIGNED_POLICY_STORE))
    request = _unsigned_request(True)

    assert request.options is None, "options should default to None"
    assert instance.authorize_unsigned(request).is_allowed(), "old request should still allow"


@pytest.mark.parametrize(
    "build",
    [
        lambda options: RequestUnsigned(
            action="a", resource=_resource(), context={}, options=options
        ),
        lambda options: AuthorizeMultiIssuerRequest(
            tokens=[], resource=_resource(), action="a", options=options
        ),
        lambda options: BatchAuthorizeUnsignedRequest(items=[], options=options),
        lambda options: BatchAuthorizeMultiIssuerRequest(tokens=[], items=[], options=options),
    ],
    ids=["unsigned", "multi_issuer", "batch_unsigned", "batch_multi_issuer"],
)
def test_requests_expose_options(build):
    """Every request type takes `options` and exposes it for reading and assignment."""
    request = build(_trace_options())
    assert request.options.trace.workload_id == "workload-1", "options should be readable"
    assert request.options.trace.workload_instance_id == "workload-1-a", (
        "optional trace field should be kept"
    )

    request.options = None
    assert request.options is None, "options should be assignable"


def test_trace_context_defaults():
    """workload_instance_id is optional; AuthorizeOptions() carries no trace."""
    trace = TraceContext("exec-1", "https://gateway.example.com", "workload-1")

    assert trace.workload_instance_id is None, "workload_instance_id should default to None"
    assert AuthorizeOptions().trace is None, "trace should default to None"
