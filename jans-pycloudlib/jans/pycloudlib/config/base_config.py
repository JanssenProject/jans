"""This module contains base class for config adapter."""

import typing as _t
from abc import ABC
from abc import abstractmethod


class BaseConfig(ABC):
    """Base class for config adapter.

    Must be sub-classed per implementation details.
    """

    @property
    def type(self) -> str:
        """Name of the configuration type.

        This attribute always returns ``config``.
        """
        return "config"

    @abstractmethod
    def get(self, key: str, default: _t.Any = "") -> _t.Any:
        """Get specific config.

        Subclass **MUST** implement this method.
        """

    @abstractmethod
    def set(self, key: str, value: _t.Any) -> bool:
        """Set specific config.

        Subclass **MUST** implement this method.
        """

    def all(self) -> dict[str, _t.Any]:  # noqa: A003
        """Get all config (deprecated in favor of ``get_all``)."""
        return self.get_all()

    @abstractmethod
    def set_all(self, data: dict[str, _t.Any]) -> bool:
        """Set all config.

        Subclass **MUST** implement this method.
        """

    @abstractmethod
    def get_all(self) -> dict[str, _t.Any]:
        """Get all configs.

        Subclass **MUST** implement this method.
        """
