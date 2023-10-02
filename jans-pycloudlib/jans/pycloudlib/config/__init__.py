# noqa: D104
import logging
import os
import typing as _t
from functools import cached_property

from jans.pycloudlib.base import BaseStorage
from jans.pycloudlib.config.consul_config import ConsulConfig  # noqa: F401
from jans.pycloudlib.config.kubernetes_config import KubernetesConfig  # noqa: F401
from jans.pycloudlib.config.google_config import GoogleConfig  # noqa: F401
from jans.pycloudlib.config.aws_config import AwsConfig  # noqa: F401

logger = logging.getLogger(__name__)

ConfigAdapter = _t.Union[ConsulConfig, KubernetesConfig, GoogleConfig, AwsConfig]
"""Configs adapter type.

Currently supports the following classes:

* [ConsulConfig][jans.pycloudlib.config.consul_config.ConsulConfig]
* [KubernetesConfig][jans.pycloudlib.config.kubernetes_config.KubernetesConfig]
* [GoogleConfig][jans.pycloudlib.config.google_config.GoogleConfig]
* [AwsConfig][jans.pycloudlib.config.aws_config.AwsConfig]
"""


class ConfigStorage(BaseStorage):
    """This class manage configs and act as a proxy to specific config adapter class."""

    def __init__(self, lock_storage=None):
        self.lock_storage = lock_storage

    @cached_property
    def adapter(self) -> ConfigAdapter:  # noqa: D412
        """Get an instance of config adapter class.

        Returns:
            An instance of config adapter class.

        Raises:
            ValueError: If the value of `CN_CONFIG_ADAPTER` environment variable is not supported.

        Examples:

        ```py
        os.environ["CN_CONFIG_ADAPTER"] = "consul"
        ConfigManager().adapter  # returns an instance of adapter class
        ```

        The adapter name is pre-populated from `CN_CONFIG_ADAPTER` environment variable.

        Supported config adapter name:

        - `consul`: returns an instance of [ConsulConfig][jans.pycloudlib.config.consul_config.ConsulConfig]
        - `kubernetes`: returns an instance of [KubernetesConfig][jans.pycloudlib.config.kubernetes_config.KubernetesConfig]
        - `google`: returns an instance of [GoogleConfig][jans.pycloudlib.config.google_config.GoogleConfig]
        - `aws`: returns an instance of [AwsConfig][jans.pycloudlib.config.aws_config.AwsConfig]
        """
        adapter = os.environ.get("CN_CONFIG_ADAPTER", "consul")

        if adapter == "consul":
            return ConsulConfig()

        if adapter == "kubernetes":
            return KubernetesConfig()

        if adapter == "google":
            return GoogleConfig()

        if adapter == "aws":
            return AwsConfig()
        raise ValueError(f"Unsupported config adapter {adapter!r}")

    def get_or_set(self, key: str, value: _t.Any, ttl: int = 10, retry_delay: float = 5.0, max_start_delay: float = 2.0):
        """Get or set a config based on given key.

        If key is found, returns the value immediately. Otherwise create a new one and returns the value.
        Note that the whole process is guarded by locking mechanism.

        Args:
            key: Name of the key.
            value: Value that will be created if key doesn't exist.
            ttl: Duration of how long lock should be expired.
            retry_delay: Delay before retrying to acquire the lock.
            max_start_delay: Delay before starting to acquire a lock.

        Returns:
            The value of given key (if any).
        """
        name = f"config.{key}"

        with self.lock_storage.create_lock(name, ttl=ttl, retry_delay=retry_delay, max_start_delay=max_start_delay):
            # double check if key already set
            _value = self.get(key)
            if _value:
                logger.info(f"The {key=} is found")
                return _value

            # key is not found, lets create it
            logger.info(f"Unable to get the {key=}; trying to create a new one")

            # maybe a callable
            if callable(value):
                value = value()

            self.set(key, value)
            logger.info(f"The {key=} has been created")
            return value


# avoid implicit reexport disabled error
__all__ = [
    "ConsulConfig",
    "KubernetesConfig",
    "GoogleConfig",
    "AwsConfig",
    "ConfigStorage",
]
