"""This module contains config and secret helpers."""

from dataclasses import dataclass

from jans.pycloudlib.lock import LockStorage
from jans.pycloudlib.config import ConfigStorage
from jans.pycloudlib.secret import SecretStorage


@dataclass
class Manager:
    """Class acts as a container of config and secret manager.

    This object is not intended for direct use, use [get_manager][jans.pycloudlib.manager.get_manager] function instead.

    Args:
        config: An instance of config storage class.
        secret: An instance of secret storage class.
        lock: An instance of lock storage class.
    """

    #: An instance of :class:`~jans.pycloudlib.manager.ConfigStorage`
    config: ConfigStorage

    #: An instance of :class:`~jans.pycloudlib.manager.SecretStorage`
    secret: SecretStorage

    #: An instance of :class:`~jans.pycloudlib.lock.LockStorage`
    lock: LockStorage


def get_manager() -> Manager:  # noqa: D412
    """Create an instance of [Manager][jans.pycloudlib.manager.Manager] class.

    The instance has `config` and `secret` attributes to interact with
    configs and secrets, for example:

    Returns:
        An instance of manager class.

    Examples:

    ```py
    manager = get_manager()
    manager.config.get("hostname")
    manager.secret.get("ssl-cert")
    ```
    """
    lock = LockStorage()
    config = ConfigStorage(lock)
    secret = SecretStorage(lock)
    return Manager(config, secret, lock)


# backward compatibility
ConfigManager = ConfigStorage
SecretManager = SecretStorage
