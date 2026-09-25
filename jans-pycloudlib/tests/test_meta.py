# ========
# k8s meta
# ========

from datetime import datetime, timezone
from unittest.mock import MagicMock, PropertyMock, patch

from jans.pycloudlib.meta.kubernetes_meta import KubernetesMeta

def test_gk8s_meta_client(gk8s_meta):
    assert gk8s_meta.client is not None

@patch("jans.pycloudlib.meta.kubernetes_meta.KubernetesMeta.client",
    new_callable=PropertyMock)
def test_kubernetes_meta_get_containers_filters_correctly(
    mock_client_prop, monkeypatch):
    """Test that get_containers successfully filters out unready and terminating pods."""

    monkeypatch.setenv("CN_CONTAINER_METADATA_NAMESPACE", "gluu")

    mock_client = MagicMock()
    mock_client_prop.return_value = mock_client

    meta = KubernetesMeta()

    # Helper to generate mock pods
    def create_pod(name, ready=True, terminating=False):
        pod = MagicMock()
        pod.metadata.name = name

        # Generate a dynamic UTC timestamp matching Kubernetes format
        if terminating:
            now_utc = datetime.now(timezone.utc)
            pod.metadata.deletion_timestamp = now_utc.strftime("%Y-%m-%dT%H:%M:%SZ")
        else:
            pod.metadata.deletion_timestamp = None

        cond = MagicMock()
        cond.type = "Ready"
        cond.status = "True" if ready else "False"
        pod.status.conditions = [cond]

        return pod

    ready_pod = create_pod("auth-1", ready=True, terminating=False)
    unready_pod = create_pod("auth-2", ready=False, terminating=False)
    terminating_pod = create_pod("auth-3", ready=True, terminating=True)

    mock_response = MagicMock()
    mock_response.items = [ready_pod, unready_pod, terminating_pod]
    mock_client.list_namespaced_pod.return_value = mock_response

    # Execute
    result = meta.get_containers("APP_NAME=jans-auth")

    # Assertions
    assert len(result) == 1
    assert result[0].metadata.name == "auth-1"
    mock_client.list_namespaced_pod.assert_called_once_with(
        "gluu",
        label_selector="APP_NAME=jans-auth",
        field_selector="status.phase=Running",
    )


@patch("jans.pycloudlib.meta.kubernetes_meta.KubernetesMeta.client",
    new_callable=PropertyMock)
def test_kubernetes_meta_get_containers_no_conditions(
    mock_client_prop, monkeypatch):
    """Test behavior when a pod's status.conditions is None."""
    monkeypatch.setenv("CN_CONTAINER_METADATA_NAMESPACE", "default")

    mock_client = MagicMock()
    mock_client_prop.return_value = mock_client

    meta = KubernetesMeta()

    pod_no_conditions = MagicMock()
    pod_no_conditions.metadata.deletion_timestamp = None
    pod_no_conditions.status.conditions = None

    mock_response = MagicMock()
    mock_response.items = [pod_no_conditions]
    mock_client.list_namespaced_pod.return_value = mock_response

    result = meta.get_containers("APP_NAME=jans-auth")

    assert len(result) == 0


@patch("jans.pycloudlib.meta.kubernetes_meta.KubernetesMeta.client",
    new_callable=PropertyMock)
def test_kubernetes_meta_get_containers_attribute_error(mock_client_prop):
    """Test that the function catches AttributeError when the k8s client is missing."""
    mock_client_prop.return_value = None

    meta = KubernetesMeta()

    result = meta.get_containers("APP_NAME=jans-auth")

    assert result == []
