"""
jans.pycloudlib.config.kubernetes_config
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This module contains config adapter class to interact with
Kubernetes ConfigMap.
"""

import os
from typing import Any

import kubernetes.client
import kubernetes.config

from jans.pycloudlib.config.base_config import BaseConfig
from jans.pycloudlib.utils import as_boolean
from jans.pycloudlib.utils import safe_value


class KubernetesConfig(BaseConfig):
    """This class interacts with Kubernetes ConfigMap backend.

    The following environment variables are used to instantiate the client:

    - ``CN_CONFIG_KUBERNETES_NAMESPACE``
    - ``CN_CONFIG_KUBERNETES_CONFIGMAP``
    - ``CN_CONFIG_KUBERNETES_USE_KUBE_CONFIG``
    """

    def __init__(self):
        self.settings = {
            k: v
            for k, v in os.environ.items()
            if k.isupper() and k.startswith("CN_CONFIG_KUBERNETES_")
        }
        self.settings.setdefault(
            "CN_CONFIG_KUBERNETES_NAMESPACE", "default",
        )

        self.settings.setdefault(
            "CN_CONFIG_KUBERNETES_CONFIGMAP", "jans",
        )

        self.settings.setdefault("CN_CONFIG_KUBERNETES_USE_KUBE_CONFIG", False)

        self._client = None
        self.name_exists = False
        self.kubeconfig_file = os.path.expanduser("~/.kube/config")

    def get(self, key: str, default: Any = None) -> Any:
        """Get value based on given key.

        :params key: Key name.
        :params default: Default value if key is not exist.
        :returns: Value based on given key or default one.
        """
        result = self.all()
        return result.get(key) or default

    @property
    def client(self):
        """Lazy-loaded client to interact with Kubernetes API.
        """
        if not self._client:
            if as_boolean(self.settings["CN_CONFIG_KUBERNETES_USE_KUBE_CONFIG"]):
                kubernetes.config.load_kube_config(self.kubeconfig_file)
            else:
                kubernetes.config.load_incluster_config()
            self._client = kubernetes.client.CoreV1Api()
        return self._client

    def _prepare_configmap(self) -> None:
        """Create a configmap name if not exist.
        """
        if not self.name_exists:
            try:
                self.client.read_namespaced_config_map(
                    self.settings["CN_CONFIG_KUBERNETES_CONFIGMAP"],
                    self.settings["CN_CONFIG_KUBERNETES_NAMESPACE"],
                )
                self.name_exists = True
            except kubernetes.client.rest.ApiException as exc:
                if exc.status == 404:
                    # create the configmaps name
                    body = {
                        "kind": "ConfigMap",
                        "apiVersion": "v1",
                        "metadata": {
                            "name": self.settings["CN_CONFIG_KUBERNETES_CONFIGMAP"],
                        },
                        "data": {},
                    }
                    created = self.client.create_namespaced_config_map(
                        self.settings["CN_CONFIG_KUBERNETES_NAMESPACE"], body,
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
        self._prepare_configmap()
        body = {
            "kind": "ConfigMap",
            "apiVersion": "v1",
            "metadata": {"name": self.settings["CN_CONFIG_KUBERNETES_CONFIGMAP"]},
            "data": {key: safe_value(value)},
        }
        ret = self.client.patch_namespaced_config_map(
            self.settings["CN_CONFIG_KUBERNETES_CONFIGMAP"],
            self.settings["CN_CONFIG_KUBERNETES_NAMESPACE"],
            body=body,
        )
        return bool(ret)

    def all(self) -> dict:  # pragma: no cover
        """Get all key-value pairs.

        :returns: A ``dict`` of key-value pairs (if any).
        """
        return self.get_all()

    def get_all(self) -> dict:
        """Get all key-value pairs.

        :returns: A ``dict`` of key-value pairs (if any).
        """
        self._prepare_configmap()
        result = self.client.read_namespaced_config_map(
            self.settings["CN_CONFIG_KUBERNETES_CONFIGMAP"],
            self.settings["CN_CONFIG_KUBERNETES_NAMESPACE"],
        )
        return result.data or {}

    def set_all(self, data: dict) -> bool:
        """Set all key-value pairs.

        :returns: A ``bool`` indicating operation is succeed or not.
        """
        self._prepare_configmap()
        body = {
            "kind": "ConfigMap",
            "apiVersion": "v1",
            "metadata": {"name": self.settings["CN_CONFIG_KUBERNETES_CONFIGMAP"]},
            "data": {key: safe_value(value) for key, value in data.items()},
        }
        ret = self.client.patch_namespaced_config_map(
            self.settings["CN_CONFIG_KUBERNETES_CONFIGMAP"],
            self.settings["CN_CONFIG_KUBERNETES_NAMESPACE"],
            body=body,
        )
        return bool(ret)
