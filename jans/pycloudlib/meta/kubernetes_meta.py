"""
jans.pycloudlib.meta.kubernetes_meta
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This module consists of class to interact with Kubernetes API.
"""

import logging
import os
import shlex
import tarfile
from tempfile import TemporaryFile

import kubernetes.client
import kubernetes.config
from kubernetes.stream import stream

from jans.pycloudlib.meta.base_meta import BaseMeta

logger = logging.getLogger(__name__)


class KubernetesMeta(BaseMeta):
    def __init__(self):
        self._client = None
        self.kubeconfig_file = os.path.expanduser("~/.kube/config")

    @property
    def client(self):
        if not self._client:
            # config loading priority
            try:
                kubernetes.config.load_incluster_config()
            except kubernetes.config.config_exception.ConfigException:
                kubernetes.config.load_kube_config(self.kubeconfig_file)
            self._client = kubernetes.client.CoreV1Api()
            self._client.api_client.configuration.assert_hostname = False
        return self._client

    def get_containers(self, label: str) -> list:
        """Get list of containers based on label.

        :params label: Label name, i.e. ``APP_NAME=oxauth``.
        :returns: List of container objects.
        """
        namespace = os.environ.get("CN_CONTAINER_METADATA_NAMESPACE", "default")
        return self.client.list_namespaced_pod(namespace, label_selector=label).items

    def get_container_ip(self, container) -> str:
        """Get container's IP address.

        :params container: Container object.
        :returns: IP address associated with the container.
        """
        return container.status.pod_ip

    def get_container_name(self, container):
        """Get container's name.

        :params container: Container object.
        :returns: Container name.
        """
        return container.metadata.name

    def copy_to_container(self, container, path: str) -> None:
        """Copy path to container.

        :params container: Container object.
        :params path: Path to file or directory.
        """
        # make sure parent directory is created first
        dirname = os.path.dirname(path)
        self.exec_cmd(container, f"mkdir -p {dirname}")

        # copy file implementation
        resp = stream(
            self.client.connect_get_namespaced_pod_exec,
            container.metadata.name,
            container.metadata.namespace,
            command=["tar", "xvf", "-", "-C", "/"],
            stderr=True,
            stdin=True,
            stdout=True,
            tty=False,
            _preload_content=False,
        )

        with TemporaryFile() as tar_buffer:
            with tarfile.open(fileobj=tar_buffer, mode="w") as tar:
                tar.add(path)

            tar_buffer.seek(0)
            commands = [tar_buffer.read()]

            while resp.is_open():
                resp.update(timeout=1)

                if resp.peek_stdout():
                    logger.debug(f"STDOUT: {resp.read_stdout()}")

                if resp.peek_stderr():
                    logger.debug(f"STDERR: {resp.read_stderr()}")

                if commands:
                    c = commands.pop(0)
                    resp.write_stdin(c)
                else:
                    break
            resp.close()

    def exec_cmd(self, container, cmd: str):
        """Run command inside container.

        :params container: Container object.
        :params cmd: String of command.
        """

        return stream(
            self.client.connect_get_namespaced_pod_exec,
            container.metadata.name,
            container.metadata.namespace,
            command=shlex.split(cmd),
            stderr=True,
            stdin=True,
            stdout=True,
            tty=False,
        )
