"""
jans.pycloudlib.config.base_config
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This module contains base class for config adapter.
"""

from typing import Any
from typing import NoReturn


class BaseConfig:
    """Base class for config adapter. Must be sub-classed per
    implementation details.
    """

    type = "config"

    def get(self, key: str, default: Any = None) -> NoReturn:
        """Get specific config.

        Subclass **MUST** implement this method.
        """
        raise NotImplementedError

    def set(self, key: str, value: Any) -> NoReturn:
        """Set specific config.

        Subclass **MUST** implement this method.
        """
        raise NotImplementedError

    def all(self) -> NoReturn:  # pragma: no cover
        """Get all config (deprecated in favor of ``get_all``).

        Subclass **MUST** implement this method.
        """
        return self.get_all()

    def set_all(self, data: dict) -> NoReturn:
        """Set all config.

        Subclass **MUST** implement this method.
        """
        raise NotImplementedError

    def get_all(self) -> NoReturn:
        """Get all secrets.

        Subclass **MUST** implement this method.
        """
        raise NotImplementedError
