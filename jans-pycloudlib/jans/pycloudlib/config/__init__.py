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


# avoid implicit reexport disabled error
__all__ = [
    "ConsulConfig",
    "KubernetesConfig",
    "GoogleConfig",
    "AwsConfig",
    "ConfigStorage",
]
