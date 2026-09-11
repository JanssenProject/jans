# This software is available under the Apache-2.0 license.
# See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
#
# Copyright (c) 2024, Gluu, Inc.

"""Tests for the local metrics snapshot API (`drain_metrics`)."""

from cedarling_python import Cedarling, EntityData, RequestUnsigned
from config import load_bootstrap_config, TEST_FILES_PATH
from os.path import join
import pytest

# Policy store without trusted issuers, same one used by
# `test_authorize_unsigned.py`. Contains a permit for
# `UpdateForTestPrincipals` guarded by `principal.is_ok`.
POLICY_STORE_LOCATION = join(TEST_FILES_PATH, "policy-store_no_trusted_issuers.yaml")

RESOURCE = EntityData.from_dict({
    "cedar_entity_mapping": {
        "entity_type": "Jans::Issue",
        "id": "random_id",
    },
    "org_id": "some_long_id",
    "country": "US",
})


def metrics_enabled_config():
    def enable_metrics(config):
        config["CEDARLING_METRICS_COLLECTION"] = "enabled"

    return load_bootstrap_config(POLICY_STORE_LOCATION, config_cb=enable_metrics)


def test_drain_metrics_disabled_raises_value_error():
    """`drain_metrics` must raise `ValueError` when metrics collection is
    disabled."""
    instance = Cedarling(load_bootstrap_config(POLICY_STORE_LOCATION))

    with pytest.raises(ValueError) as exc_info:
        instance.drain_metrics()
    assert "metrics collection is disabled" in str(exc_info.value)


def test_drain_metrics_collects_and_resets():
    """`drain_metrics` must return a snapshot with the expected fields, count a
    subsequent authorization, and reset the counters on the next drain."""
    instance = Cedarling(metrics_enabled_config())

    snapshot = instance.drain_metrics()
    assert "instance.policy_count" in snapshot.operational_stats, (
        "operational stats must include the policy count gauge, got: "
        f"{snapshot.operational_stats}"
    )
    assert snapshot.interval_secs >= 0, (
        "interval_secs must be a non-negative elapsed duration, got: "
        f"{snapshot.interval_secs}"
    )

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

    snapshot_after = instance.drain_metrics()
    assert snapshot_after.operational_stats.get("authz.requests_total") == 1, (
        "the authorized request must be counted in the drained interval, got: "
        f"{snapshot_after.operational_stats}"
    )

    snapshot_reset = instance.drain_metrics()
    assert snapshot_reset.operational_stats.get("authz.requests_total") == 0, (
        "counters must reset to a fresh zeroed window after a snapshot, got: "
        f"{snapshot_reset.operational_stats}"
    )