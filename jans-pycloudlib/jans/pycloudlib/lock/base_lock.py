import typing as _t

from abc import ABC
from abc import abstractmethod


class BaseLock(ABC):
    """Base class for lock adapter.

    Must be sub-classed per implementation details.
    """

    @property
    def type(self) -> str:
        """Name of the configuration type.

        This attribute always returns `lock`.
        """
        return "lock"

    @abstractmethod
    def get(self, key: str, default: _t.Any = "") -> _t.Any:
        """Get specific lock.

        !!! important
            Subclass **MUST** implement this method.

        Args:
            key: Key name.
            default: Default value if key is not exist.

        Returns:
            Value based on given key or default one.
        """

    @abstractmethod
    def set(self, key: str, value: _t.Any) -> bool:
        """Set specific lock.

        !!! important
            Subclass **MUST** implement this method.

        Args:
            key: Key name.
            value: Value of the key.

        Returns:
            A boolean to mark whether lock is set or not.
        """

    def all(self) -> dict[str, _t.Any]:  # noqa: A003
        """Get all locks (deprecated in favor of `get_all`)."""
        return self.get_all()

    @abstractmethod
    def set_all(self, data: dict[str, _t.Any]) -> bool:
        """Set all locks.

        !!! important
            Subclass **MUST** implement this method.

        Args:
            data: Key-value pairs.

        Returns:
            A boolean to mark whether lock is set or not.
        """

    @abstractmethod
    def get_all(self) -> dict[str, _t.Any]:
        """Get all locks.

        !!! important
            Subclass **MUST** implement this method.

        Returns:
            A mapping of all locks (if any).
        """

    @abstractmethod
    def delete(self, key: str) -> bool:
        """Delete specific lock.

        !!! important
            Subclass **MUST** implement this method.

        Args:
            key: Key name.

        Returns:
            A boolean to mark whether lock is removed or not.
        """
