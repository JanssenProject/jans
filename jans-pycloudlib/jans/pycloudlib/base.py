import typing as _t
from abc import ABC
from abc import abstractproperty


class AdapterProtocol(_t.Protocol):  # pragma: no cover
    """Custom class to define adapter contracts (only useful for type check)."""

    def get(self, key: str, default: _t.Any = "") -> _t.Any:  # noqa: D102
        ...

    def set(self, key: str, value: _t.Any) -> bool:  # noqa: D102
        ...

    def all(self) -> dict[str, _t.Any]:  # noqa: A003,D102
        ...

    def get_all(self) -> dict[str, _t.Any]:  # noqa: D102
        ...

    def set_all(self, data: dict[str, _t.Any]) -> bool:  # noqa: D102
        ...


class BaseStorage(ABC):
    """Base class to provide contracts for managing configuration (configs or secrets)."""

    @abstractproperty
    def adapter(self) -> AdapterProtocol:  # pragma: no cover
        """Abstract attribute as a container of adapter instance.

        The adapter is used in the following public methods:

        - `get`
        - `get_all`
        - `set`
        - `set_all`

        !!! important
            Any subclass **MUST** returns an instance of adapter or raise exception.
        """

    def get(self, key: str, default: _t.Any = "") -> _t.Any:
        """Get value based on given key.

        Args:
            key: Key name.
            default: Default value if key is not exist.

        Returns:
            Value based on given key or default one.
        """
        return self.adapter.get(key, default)

    def set(self, key: str, value: _t.Any) -> bool:
        """Set key with given value.

        Args:
            key: Key name.
            value: Value of the key.

        Returns:
            A boolean to mark whether configuration is set or not.
        """
        return self.adapter.set(key, value)

    def all(self) -> dict[str, _t.Any]:  # noqa: A003
        """Get all key-value pairs (deprecated in favor of [get_all][jans.pycloudlib.manager.BaseConfiguration.get_all]).

        Returns:
            A mapping of configuration.
        """
        return self.get_all()

    def get_all(self) -> dict[str, _t.Any]:
        """Get all configuration.

        Returns:
            A mapping of configuration (if any).
        """
        return self.adapter.get_all()

    def set_all(self, data: dict[str, _t.Any]) -> bool:
        """Set all key-value pairs.

        Args:
            data: Key-value pairs.

        Returns:
            A boolean to mark whether configuration is set or not.
        """
        return self.adapter.set_all(data)
