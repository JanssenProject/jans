"""This module consists of class to interact with Docker API."""

from __future__ import annotations

import contextlib
import os
import tarfile
import typing as _t

from docker import DockerClient

from jans.pycloudlib.meta.base_meta import BaseMeta

if _t.TYPE_CHECKING:  # pragma: no cover
    # imported objects for function type hint, completion, etc.
    # these won't be executed in runtime
    from docker.models.containers import Container


class DockerMeta(BaseMeta):
    """This class interacts with a subset of Docker APIs.

    Args:
        base_url: Base URL to docker daemon API.
    """

    def __init__(self, base_url: str = "unix://var/run/docker.sock") -> None:
        """Initialize Docker meta wrapper."""
        self.client = DockerClient(base_url=base_url)

    def get_containers(self, label: str) -> list[Container]:
        """Get list of containers based on label.

        Args:
            label: Label name, i.e. `APP_NAME=jans-auth`.

        Returns:
            List of container objects.
        """
        containers: list[Container] = self.client.containers.list(filters={'label': label})
        return containers

    def get_container_ip(self, container: Container) -> str:
        """Get container's IP address.

        Args:
            container: Container object.

        Returns:
            IP address associated with the container.
        """
        ip = ""
        for _, network in container.attrs["NetworkSettings"]["Networks"].items():
            ip = network["IPAddress"]
            break
        return ip

    def get_container_name(self, container: Container) -> str:
        """Get container's name.

        Args:
            container: Container object.

        Returns:
            Container name.
        """
        name: str = container.name
        return name

    def copy_to_container(self, container: Container, path: str) -> None:
        """Copy path to container.

        Args:
            container: Container object.
            path: Path to file or directory.
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

    def exec_cmd(self, container: Container, cmd: str) -> _t.Any:
        """Run command inside container.

        Args:
            container: Container object.
            cmd: String of command.
        """
        return container.exec_run(cmd)
