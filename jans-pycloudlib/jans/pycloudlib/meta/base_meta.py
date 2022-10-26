"""This module contains base class for meta adapter."""

from abc import ABC
from abc import abstractmethod
import typing as _t


class BaseMeta(ABC):
    """Base class for meta client adapter.

    !!! important
        Must be sub-classed per implementation details.
    """

    @abstractmethod
    def get_containers(self, label: str) -> list[_t.Any]:
        """Get list of containers based on label.

        !!! important
            Subclass **MUST** implement this method.

        Args:
            label: Label name, i.e. `APP_NAME=jans-auth`.
        """

    @abstractmethod
    def get_container_ip(self, container: _t.Any) -> str:
        """Get container's IP address.

        !!! important
            Subclass **MUST** implement this method.

        Args:
            container: Container object.

        Returns:
            IP address associated with the container.
        """

    @abstractmethod
    def get_container_name(self, container: _t.Any) -> str:
        """Get container's name.

        !!! important
            Subclass **MUST** implement this method.

        Args:
            container: Container object.
        """

    @abstractmethod
    def copy_to_container(self, container: _t.Any, path: str) -> None:
        """Copy path to container.

        !!! important
            Subclass **MUST** implement this method.

        Args:
            container: Container object.
            path: Path to file or directory.
        """

    @abstractmethod
    def exec_cmd(self, container: _t.Any, cmd: str) -> _t.Any:
        """Run command inside container.

        !!! important
            Subclass **MUST** implement this method.

        Args:
            container: Container object.
            cmd: String of command.
        """
