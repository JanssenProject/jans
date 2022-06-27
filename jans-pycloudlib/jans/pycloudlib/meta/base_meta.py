"""This module contains base class for meta adapter."""

from abc import ABC
from abc import abstractmethod
import typing as _t


class BaseMeta(ABC):
    """Base class for meta client adapter.

    Must be sub-classed per implementation details.
    """

    @abstractmethod
    def get_containers(self, label: str) -> list[_t.Any]:
        """Get list of containers based on label.

        Subclass **MUST** implement this method.
        """

    @abstractmethod
    def get_container_ip(self, container: _t.Any) -> str:
        """Get container's IP address.

        Subclass **MUST** implement this method.
        """

    @abstractmethod
    def get_container_name(self, container: _t.Any) -> str:
        """Get container's name.

        Subclass **MUST** implement this method.
        """

    @abstractmethod
    def copy_to_container(self, container: _t.Any, path: str) -> None:
        """Copy path to container.

        Subclass **MUST** implement this method.
        """

    @abstractmethod
    def exec_cmd(self, container: _t.Any, cmd: str) -> _t.Any:
        """Run command inside container.

        Subclass **MUST** implement this method.
        """
