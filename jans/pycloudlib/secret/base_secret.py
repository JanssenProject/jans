"""
jans.pycloudlib.secret.base_secret
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This module contains base class for secret adapter.
"""

from typing import Any
from typing import NoReturn


class BaseSecret:
    """Base class for secret adapter. Must be sub-classed per
    implementation details.
    """

    type = "secret"

    def get(self, key: str, default: Any = None) -> NoReturn:
        """Get specific secret.

        Subclass **MUST** implement this method.
        """
        raise NotImplementedError

    def set(self, key: str, value: Any) -> NoReturn:
        """Set specific secret.

        Subclass **MUST** implement this method.
        """
        raise NotImplementedError

    def all(self) -> NoReturn:  # pragma: no cover
        """Get all secrets (deprecated in favor of ``get_all``).

        Subclass **MUST** implement this method.
        """
        return self.get_all()

    def set_all(self, data: dict) -> NoReturn:
        """Set all secrets.

        Subclass **MUST** implement this method.
        """
        raise NotImplementedError

    def get_all(self) -> NoReturn:
        """Get all secrets.

        Subclass **MUST** implement this method.
        """
        raise NotImplementedError
