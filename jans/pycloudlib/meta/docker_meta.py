"""
jans.pycloudlib.meta.docker_meta
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This module consists of class to interact with Docker API.
"""

import contextlib
import os
import tarfile

import docker

from jans.pycloudlib.meta.base_meta import BaseMeta


class DockerMeta(BaseMeta):
    """This class interacts with Docker API.
    """

    def __init__(self, base_url="unix://var/run/docker.sock"):
        self.client = docker.DockerClient(base_url=base_url)

    def get_containers(self, label: str) -> list:
        """Get list of containers based on label.

        :params label: Label name, i.e. ``APP_NAME=oxauth``.
        :returns: List of container objects.
        """
        return self.client.containers.list(filters={'label': label})

    def get_container_ip(self, container) -> str:
        """Get container's IP address.

        :params container: Container object.
        :returns: IP address associated with the container.
        """
        for _, network in container.attrs["NetworkSettings"]["Networks"].items():
            return network["IPAddress"]
        return ""

    def get_container_name(self, container) -> str:
        """Get container's name.

        :params container: Container object.
        :returns: Container name.
        """
        return container.name

    def copy_to_container(self, container, path: str) -> None:
        """Copy path to container.

        :params container: Container object.
        :params path: Path to file or directory.
        """
        src = os.path.basename(path)
        dirname = os.path.dirname(path)

        os.chdir(dirname)

        with tarfile.open(src + ".tar", "w:gz") as tar:
            tar.add(src)

        with open(src + ".tar", "rb") as f:
            payload = f.read()

            # create directory first
            self.exec_cmd(container, f"mkdir -p {dirname}")

            # copy file
            container.put_archive(os.path.dirname(path), payload)

        with contextlib.suppress(FileNotFoundError):
            os.unlink(src + ".tar")

    def exec_cmd(self, container, cmd: str):
        """Run command inside container.

        :params container: Container object.
        :params cmd: String of command.
        """
        return container.exec_run(cmd)
