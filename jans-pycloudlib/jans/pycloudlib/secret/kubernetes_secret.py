"""This module contains secret adapter class to interact with Kubernetes Secret."""

import base64
import os
import typing as _t

import kubernetes.client
import kubernetes.config

from jans.pycloudlib.secret.base_secret import BaseSecret
from jans.pycloudlib.utils import as_boolean
from jans.pycloudlib.utils import safe_value


class KubernetesSecret(BaseSecret):
    """This class interacts with Kubernetes Secret backend.

    The instance of this class is configured via environment variables.

    Supported environment variables:

    - `CN_SECRET_KUBERNETES_NAMESPACE`: Kubernetes namespace (default to `default`).
    - `CN_SECRET_KUBERNETES_SECRET`: Kubernetes secrets name (default to `jans`).
    - `CN_SECRET_KUBERNETES_USE_KUBE_CONFIG`: Load credentials from `$HOME/.kube/config`, only useful for non-container environment (default to `false`).
    """

    def __init__(self) -> None:
        self.settings = {
            k: v
            for k, v in os.environ.items()
            if k.isupper() and k.startswith("CN_SECRET_KUBERNETES_")
        }
        self.settings.setdefault(
            "CN_SECRET_KUBERNETES_NAMESPACE", "default",
        )
        self.settings.setdefault(
            "CN_SECRET_KUBERNETES_SECRET", "jans",
        )
        self.settings.setdefault("CN_SECRET_KUBERNETES_USE_KUBE_CONFIG", "false")

        self._client = None
        self.name_exists = False
        self.kubeconfig_file = os.path.expanduser("~/.kube/config")

    @property
    def client(self) -> kubernetes.client.CoreV1Api:
        """Lazy-loaded client to interact with Kubernetes API."""
        if not self._client:
            if as_boolean(self.settings["CN_SECRET_KUBERNETES_USE_KUBE_CONFIG"]):
                kubernetes.config.load_kube_config(self.kubeconfig_file)
            else:
                kubernetes.config.load_incluster_config()
            self._client = kubernetes.client.CoreV1Api()
        return self._client

    def get(self, key: str, default: _t.Any = "") -> _t.Any:
        """Get value based on given key.

        Args:
            key: Key name.
            default: Default value if key is not exist.

        Returns:
            Value based on given key or default one.
        """
        result = self.get_all()
        return result.get(key) or default

    def _prepare_secret(self) -> None:
        """Create a secret name if not exist."""
        if not self.name_exists:
            try:
                self.client.read_namespaced_secret(
                    self.settings["CN_SECRET_KUBERNETES_SECRET"],
                    self.settings["CN_SECRET_KUBERNETES_NAMESPACE"],
                )
                self.name_exists = True
            except kubernetes.client.rest.ApiException as exc:
                if exc.status == 404:
                    # create the secrets name
                    body = {
                        "kind": "Secret",
                        "apiVersion": "v1",
                        "metadata": {
                            "name": self.settings["CN_SECRET_KUBERNETES_SECRET"],
                        },
                        "data": {},
                    }
                    created = self.client.create_namespaced_secret(
                        self.settings["CN_SECRET_KUBERNETES_NAMESPACE"], body,
                    )
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
        self._prepare_secret()
        body = {
            "kind": "Secret",
            "apiVersion": "v1",
            "metadata": {"name": self.settings["CN_SECRET_KUBERNETES_SECRET"]},
            "data": {key: base64.b64encode(safe_value(value).encode()).decode()},
        }
        ret = self.client.patch_namespaced_secret(
            self.settings["CN_SECRET_KUBERNETES_SECRET"],
            self.settings["CN_SECRET_KUBERNETES_NAMESPACE"],
            body=body,
        )
        return bool(ret)

    def get_all(self) -> dict[str, _t.Any]:
        """Get all key-value pairs.

        Returns:
            A mapping of secrets (if any).
        """
        self._prepare_secret()
        result = self.client.read_namespaced_secret(
            self.settings["CN_SECRET_KUBERNETES_SECRET"],
            self.settings["CN_SECRET_KUBERNETES_NAMESPACE"],
        )

        data = result.data or {}
        return {k: base64.b64decode(v).decode() for k, v in data.items()}

    def set_all(self, data: dict[str, _t.Any]) -> bool:
        """Set all key-value pairs.

        Args:
            data: Key-value pairs.

        Returns:
            A boolean to mark whether config is set or not.
        """
        self._prepare_secret()
        body = {
            "kind": "Secret",
            "apiVersion": "v1",
            "metadata": {"name": self.settings["CN_SECRET_KUBERNETES_SECRET"]},
            "data": {
                key: base64.b64encode(safe_value(value).encode()).decode()
                for key, value in data.items()
            },
        }
        ret = self.client.patch_namespaced_secret(
            self.settings["CN_SECRET_KUBERNETES_SECRET"],
            self.settings["CN_SECRET_KUBERNETES_NAMESPACE"],
            body=body,
        )
        return bool(ret)
