"""
jans.pycloudlib.secret.kubernetes_secret
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This module contains secret adapter class to interact with
Kubernetes Secret.
"""

import base64
import os
from typing import Any

import kubernetes.client
import kubernetes.config

from jans.pycloudlib.secret.base_secret import BaseSecret
from jans.pycloudlib.utils import as_boolean
from jans.pycloudlib.utils import safe_value


class KubernetesSecret(BaseSecret):
    """This class interacts with Kubernetes Secret backend.

    The following environment variables are used to instantiate the client:

    - ``CN_SECRET_KUBERNETES_NAMESPACE``
    - ``CN_SECRET_KUBERNETES_SECRET``
    - ``CN_SECRET_KUBERNETES_USE_KUBE_CONFIG``
    """

    def __init__(self):
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
        self.settings.setdefault("CN_SECRET_KUBERNETES_USE_KUBE_CONFIG", False)

        self._client = None
        self.name_exists = False
        self.kubeconfig_file = os.path.expanduser("~/.kube/config")

    @property
    def client(self):
        """Lazy-loaded client to interact with Kubernetes API.
        """
        if not self._client:
            if as_boolean(self.settings["CN_SECRET_KUBERNETES_USE_KUBE_CONFIG"]):
                kubernetes.config.load_kube_config(self.kubeconfig_file)
            else:
                kubernetes.config.load_incluster_config()
            self._client = kubernetes.client.CoreV1Api()
        return self._client

    def get(self, key, default: Any = None) -> Any:
        """Get value based on given key.

        :params key: Key name.
        :params default: Default value if key is not exist.
        :returns: Value based on given key or default one.
        """
        result = self.all()
        return result.get(key) or default

    def _prepare_secret(self) -> None:
        """Create a secret name if not exist.
        """
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
                        self.settings["CN_SECRET_KUBERNETES_NAMESPACE"], body
                    )
                    if created:
                        self.name_exists = True
                else:
                    raise

    def set(self, key: str, value: Any) -> bool:
        """Set key with given value.

        :params key: Key name.
        :params value: Value of the key.
        :returns: A ``bool`` to mark whether config is set or not.
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

    def all(self) -> dict:
        """Get all key-value pairs.

        :returns: A ``dict`` of key-value pairs (if any).
        """
        self._prepare_secret()
        result = self.client.read_namespaced_secret(
            self.settings["CN_SECRET_KUBERNETES_SECRET"],
            self.settings["CN_SECRET_KUBERNETES_NAMESPACE"],
        )

        data = result.data or {}
        return {k: base64.b64decode(v).decode() for k, v in data.items()}
