"""
jans.pycloudlib.meta.base_meta
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This module contains base class for meta adapter.
"""

from typing import NoReturn


class BaseMeta:
    """Base class for meta client adapter. Must be sub-classed per
    implementation details.
    """

    def get_containers(self, label) -> NoReturn:
        """Get list of containers based on label.

        Subclass **MUST** implement this method.
        """
        raise NotImplementedError

    def get_container_ip(self, container) -> NoReturn:
        """Get container's IP address.

        Subclass **MUST** implement this method.
        """
        raise NotImplementedError

    def get_container_name(self, container) -> NoReturn:
        """Get container's name.

        Subclass **MUST** implement this method.
        """
        raise NotImplementedError

    def copy_to_container(self, container, path) -> NoReturn:
        """Copy path to container.

        Subclass **MUST** implement this method.
        """
        raise NotImplementedError

    def exec_cmd(self, container, cmd) -> NoReturn:
        """Run command inside container.

        Subclass **MUST** implement this method.
        """
        raise NotImplementedError
