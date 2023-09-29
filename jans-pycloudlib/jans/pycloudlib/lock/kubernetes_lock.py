"""This module contains lock adapter class to interact with Kubernetes ConfigMap."""""

import os
import typing as _t

from jans.pycloudlib.utils import as_boolean
from jans.pycloudlib.utils import safe_value

import kubernetes.client
import kubernetes.config

from jans.pycloudlib.lock.base_lock import BaseLock


class KubernetesLock(BaseLock):
    """This class interacts with Kubernetes ConfigMap backend to create lock.

    The instance of this class is configured via environment variables.

    Supported environment variables:

    - `CN_LOCK_KUBERNETES_NAMESPACE`: Kubernetes namespace (default to `default`).
    - `CN_LOCK_KUBERNETES_NAME_PREFIX`: Kubernetes configmaps name prefix (default to `jans`).
    - `CN_LOCK_KUBERNETES_USE_KUBE_CONFIG`: Load credentials from `$HOME/.kube/config`, only useful for non-container environment (default to `false`).
    """

    def __init__(self) -> None:
        prefix = os.environ.get("CN_LOCK_KUBERNETES_NAME_PREFIX", "jans")
        self.configmap_name = f"{prefix}-lock"
        self.namespace = os.environ.get("CN_LOCK_KUBERNETES_NAMESPACE", "default")
        self.use_kube_config = as_boolean(os.environ.get("CN_LOCK_KUBERNETES_USE_KUBE_CONFIG", "false"))
        self._client = None
        self.name_exists = False
        self.kubeconfig_file = os.path.expanduser("~/.kube/config")

    def get(self, key: str, default: _t.Any = "") -> _t.Any:
        """Get value based on given key.

        Args:
            key: Key name.
            default: Default value if key is not exist.

        Returns:
            Value based on given key or default one.
        """
        result = self.all()
        return result.get(key) or default

    @property
    def client(self) -> kubernetes.client.CoreV1Api:
        """Lazy-loaded client to interact with Kubernetes API."""
        if not self._client:
            if self.use_kube_config:
                kubernetes.config.load_kube_config(self.kubeconfig_file)
            else:
                kubernetes.config.load_incluster_config()
            self._client = kubernetes.client.CoreV1Api()
        return self._client

    def _prepare_configmap(self) -> None:
        """Create a configmap name if not exist."""
        if not self.name_exists:
            try:
                self.client.read_namespaced_config_map(self.configmap_name, self.namespace)
                self.name_exists = True
            except kubernetes.client.rest.ApiException as exc:
                if exc.status == 404:
                    # create the configmaps name
                    body = {
                        "kind": "ConfigMap",
                        "apiVersion": "v1",
                        "metadata": {"name": self.configmap_name},
                        "data": {},
                    }
                    created = self.client.create_namespaced_config_map(self.namespace, body)
                    if created:
                        self.name_exists = True
                else:
                    raise

    def set(self, key: str, value: _t.Any) -> bool:
        """Set key with given value.

        Args:
            key: Key name.
            value: Value of the key.

        Returns:
            A boolean to mark whether config is set or not.
        """
        self._prepare_configmap()
        body = {
            "kind": "ConfigMap",
            "apiVersion": "v1",
            "metadata": {"name": self.configmap_name},
            "data": {key: safe_value(value)},
        }
        ret = self.client.patch_namespaced_config_map(self.configmap_name, self.namespace, body=body)
        return bool(ret)

    def get_all(self) -> dict[str, _t.Any]:
        """Get all key-value pairs.

        Returns:
            A mapping of configs (if any).
        """
        self._prepare_configmap()
        result = self.client.read_namespaced_config_map(self.configmap_name, self.namespace)
        return result.data or {}

    def set_all(self, data: dict[str, _t.Any]) -> bool:
        """Set all key-value pairs.

        Returns:
            A boolean indicating operation is succeed or not.
        """
        self._prepare_configmap()
        body = {
            "kind": "ConfigMap",
            "apiVersion": "v1",
            "metadata": {"name": self.configmap_name},
            "data": {key: safe_value(value) for key, value in data.items()},
        }
        ret = self.client.patch_namespaced_config_map(self.configmap_name, self.namespace, body=body)
        return bool(ret)

    def delete(self, key: str) -> bool:
        """Delete specific lock.

        Args:
            key: Key name.

        Returns:
            A boolean to mark whether lock is removed or not.
        """
        data = self.get_all()
        data.pop(key)

        body = {
            "kind": "ConfigMap",
            "apiVersion": "v1",
            "metadata": {"name": self.configmap_name},
            "data": {key: safe_value(value) for key, value in data.items()},
        }
        resp = self.client.replace_namespaced_config_map(self.configmap_name, self.namespace, body=body)
        return bool(resp)
