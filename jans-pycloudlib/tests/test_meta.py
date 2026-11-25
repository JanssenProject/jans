# ========
# k8s meta
# ========

from datetime import datetime, timezone
from unittest.mock import MagicMock

import pytest
from kubernetes.client import V1ObjectMeta, V1Pod, V1PodCondition, V1PodList, V1PodStatus
from kubernetes.client.exceptions import ApiException

from jans.pycloudlib.meta.kubernetes_meta import KubernetesMeta


def test_gk8s_meta_client(gk8s_meta):
    assert gk8s_meta.client is not None


# Conditions of a healthy pod as reported by the Kubernetes API. All four are listed on
# purpose: every one is "True", so a test that changes one of them shows that this
# condition decides, and real pods carry all four. Do not shorten to {"Ready": "True"}.
READY_POD_CONDITIONS = {
    "PodScheduled": "True",
    "Initialized": "True",
    "ContainersReady": "True",
    "Ready": "True",
}


def make_pod(name: str, conditions: dict[str, str], deletion_timestamp: datetime | None = None) -> V1Pod:
    """Build a Running pod with the given condition statuses, keyed by condition type."""
    pod_conditions = [
        V1PodCondition(type=condition_type, status=status)
        for condition_type, status in conditions.items()
    ]
    return V1Pod(
        metadata=V1ObjectMeta(name=name, deletion_timestamp=deletion_timestamp),
        status=V1PodStatus(phase="Running", conditions=pod_conditions),
    )


@pytest.fixture
def fake_list_namespaced_pod(monkeypatch: pytest.MonkeyPatch) -> MagicMock:
    """Replace the pod list call of the Kubernetes API; tests set the pods it returns."""
    mock_list_call = MagicMock(return_value=V1PodList(items=[]))
    monkeypatch.setattr("kubernetes.client.CoreV1Api.list_namespaced_pod", mock_list_call)
    return mock_list_call


def test_k8s_meta_get_containers_lists_running_pods_by_label_in_configured_namespace(
    gk8s_meta, fake_list_namespaced_pod, monkeypatch
):
    monkeypatch.setenv("CN_CONTAINER_METADATA_NAMESPACE", "jans-test")

    gk8s_meta.get_containers("APP_NAME=auth-server")

    fake_list_namespaced_pod.assert_called_once_with(
        "jans-test",
        label_selector="APP_NAME=auth-server",
        field_selector="status.phase=Running",
    )


def test_k8s_meta_get_containers_returns_ready_pod(gk8s_meta, fake_list_namespaced_pod):
    ready_pod = make_pod("auth-ready", READY_POD_CONDITIONS)
    fake_list_namespaced_pod.return_value = V1PodList(items=[ready_pod])

    containers = gk8s_meta.get_containers("APP_NAME=auth-server")

    assert containers == [ready_pod]


@pytest.mark.parametrize("not_ready_pod", [
    pytest.param(make_pod("auth-starting", {**READY_POD_CONDITIONS, "Ready": "False"}), id="ready-false"),
    pytest.param(make_pod("auth-unreachable", {**READY_POD_CONDITIONS, "Ready": "Unknown"}), id="ready-unknown"),
    pytest.param(
        V1Pod(metadata=V1ObjectMeta(name="auth-no-conditions"), status=V1PodStatus(phase="Running")),
        id="no-conditions",
    ),
])
def test_k8s_meta_get_containers_skips_pod_that_is_not_ready(gk8s_meta, fake_list_namespaced_pod, not_ready_pod):
    ready_pod = make_pod("auth-ready", READY_POD_CONDITIONS)
    fake_list_namespaced_pod.return_value = V1PodList(items=[ready_pod, not_ready_pod])

    containers = gk8s_meta.get_containers("APP_NAME=auth-server")

    assert containers == [ready_pod]


def test_k8s_meta_get_containers_skips_terminating_pod(gk8s_meta, fake_list_namespaced_pod):
    ready_pod = make_pod("auth-ready", READY_POD_CONDITIONS)
    terminating_pod = make_pod(
        "auth-terminating",
        READY_POD_CONDITIONS,
        deletion_timestamp=datetime(2026, 1, 1, tzinfo=timezone.utc),
    )
    fake_list_namespaced_pod.return_value = V1PodList(items=[ready_pod, terminating_pod])

    containers = gk8s_meta.get_containers("APP_NAME=auth-server")

    assert containers == [ready_pod]


@pytest.mark.parametrize("incomplete_pod", [
    pytest.param(V1Pod(metadata=V1ObjectMeta(name="auth-no-status")), id="no-status"),
    # ready by its conditions (same as a ready pod), so only the missing metadata can exclude it
    pytest.param(V1Pod(status=make_pod("unused-name", READY_POD_CONDITIONS).status), id="no-metadata"),
])
def test_k8s_meta_get_containers_keeps_ready_pods_when_another_pod_lacks_status_or_metadata(
    gk8s_meta, fake_list_namespaced_pod, incomplete_pod
):
    ready_pod = make_pod("auth-ready", READY_POD_CONDITIONS)
    fake_list_namespaced_pod.return_value = V1PodList(items=[ready_pod, incomplete_pod])

    containers = gk8s_meta.get_containers("APP_NAME=auth-server")

    assert containers == [ready_pod]


def test_k8s_meta_get_containers_returns_empty_list_without_kubernetes_config(tmp_path, monkeypatch):
    # uses the real `client` property: without an in-cluster environment and with a
    # missing kubeconfig file it returns None, which get_containers checks explicitly
    monkeypatch.delenv("KUBERNETES_SERVICE_HOST", raising=False)
    meta = KubernetesMeta()
    meta.kubeconfig_file = str(tmp_path / "missing-kubeconfig")

    containers = meta.get_containers("APP_NAME=auth-server")

    assert containers == []


def test_k8s_meta_get_containers_does_not_hide_filter_errors(gk8s_meta, fake_list_namespaced_pod, monkeypatch):
    fake_list_namespaced_pod.return_value = V1PodList(items=[make_pod("auth-ready", READY_POD_CONDITIONS)])

    def broken_filter(pod: V1Pod) -> bool:
        raise AttributeError("filter bug")

    # kubernetes models validate their fields (true for 37.x, pydantic), so no real pod can make the
    # filter raise; the filter is replaced to simulate a bug in it, which must reach the caller
    monkeypatch.setattr("jans.pycloudlib.meta.kubernetes_meta._is_pod_ready", broken_filter)

    with pytest.raises(AttributeError, match="filter bug"):
        gk8s_meta.get_containers("APP_NAME=auth-server")


def test_k8s_meta_get_containers_propagates_api_errors(gk8s_meta, fake_list_namespaced_pod):
    fake_list_namespaced_pod.side_effect = ApiException(status=500)

    with pytest.raises(ApiException):
        gk8s_meta.get_containers("APP_NAME=auth-server")
