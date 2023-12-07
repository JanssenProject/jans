"""This module contains base class for lock adapter."""

import typing as _t
from abc import ABC
from abc import abstractmethod


class BaseLock(ABC):
    """Base class for config adapter.

    Must be sub-classed per implementation details.
    """

    @property
    def type(self) -> str:
        """Name of the configuration type.

        This attribute always returns `lock`.
        """
        return "lock"

    @abstractmethod
    def get(self, key: str) -> dict[str, _t.Any]:
        """Get specific lock.

        !!! important
            Subclass **MUST** implement this method.

        Args:
            key: Lock name.

        Returns:
            Mapping of lock data.
        """

    @abstractmethod
    def post(self, key: str, owner: str, ttl: float, updated_at: str) -> bool:
        """Create specific lock.

        !!! important
            Subclass **MUST** implement this method.

        Args:
            key: Lock name.
            owner: Lock owner.
            ttl: Duration of lock before expire.
            updated_at: Timestamp (datetime format) string.

        Returns:
            A boolean to indicate lock is created.
        """

    @abstractmethod
    def put(self, key: str, owner: str, ttl: float, updated_at: str) -> bool:
        """Update specific lock.

        !!! important
            Subclass **MUST** implement this method.

        Args:
            key: Lock name.
            owner: Lock owner.
            ttl: Duration of lock before expire.
            updated_at: Timestamp (datetime format) string.

        Returns:
            A boolean to indicate lock is updated.
        """

    @abstractmethod
    def delete(self, key: str) -> bool:
        """Delete specific lock.

        !!! important
            Subclass **MUST** implement this method.

        Args:
            key: Lock name.

        Returns:
            A boolean to indicate lock has been deleted.
        """

    @abstractmethod
    def connected(self) -> bool:
        """Check if connection is established.

        !!! important
            Subclass **MUST** implement this method.

        Returns:
            A boolean to indicate connection is established.
        """
