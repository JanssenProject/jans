"""This module contains config and secret helpers."""

import os
import typing as _t
from abc import ABC
from abc import abstractproperty
from dataclasses import dataclass
from functools import cached_property

from jans.pycloudlib.config import ConsulConfig
from jans.pycloudlib.config import KubernetesConfig
from jans.pycloudlib.config import GoogleConfig
from jans.pycloudlib.secret import KubernetesSecret
from jans.pycloudlib.secret import VaultSecret
from jans.pycloudlib.secret import GoogleSecret
from jans.pycloudlib.utils import decode_text
from jans.pycloudlib.utils import encode_text

ConfigAdapter = _t.Union[ConsulConfig, KubernetesConfig, GoogleConfig]
SecretAdapter = _t.Union[VaultSecret, KubernetesSecret, GoogleSecret]


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


class BaseConfiguration(ABC):
    """Base class to provide contracts for managing configuration (configs or secrets)."""

    @abstractproperty
    def adapter(self) -> AdapterProtocol:  # pragma: no cover
        """Abstract attribute as a container of adapter instance.

        The adapter is used in the following public methods:

        - ``get``
        - ``get_all``
        - ``set``
        - ``set_all``

        Any subclass **MUST** returns an instance of adapter or raise exception.
        """

    def get(self, key: str, default: _t.Any = "") -> _t.Any:
        """Get value based on given key.

        :param key: Key name.
        :param default: Default value if key is not exist.
        :returns: Value based on given key or default one.
        """
        return self.adapter.get(key, default)

    def set(self, key: str, value: _t.Any) -> bool:
        """Set key with given value.

        :param key: Key name.
        :param value: Value of the key.
        :returns: A ``bool`` to mark whether config is set or not.
        """
        return self.adapter.set(key, value)

    def all(self) -> dict[str, _t.Any]:  # noqa: A003
        """Get all key-value pairs (deprecated in favor of ``get_all``).

        :returns: A ``dict`` of key-value pairs.
        """
        return self.get_all()

    def get_all(self) -> dict[str, _t.Any]:
        """Get all key-value pairs.

        :returns: A ``dict`` of key-value pairs.
        """
        return self.adapter.get_all()

    def set_all(self, data: dict[str, _t.Any]) -> bool:
        """Set all key-value pairs.

        :param data: Key-value pairs.
        """
        return self.adapter.set_all(data)


class ConfigManager(BaseConfiguration):
    """A subclass of :class:`~jans.pycloudlib.manager.BaseConfiguration` to manage configs.

    This class acts as a proxy to specific config adapter class.
    """

    @cached_property
    def adapter(self) -> ConfigAdapter:  # noqa: D412
        """Get an instance of adapter class.

        Example:

        .. code-block:: python

            os.environ["CN_CONFIG_ADAPTER"] = "consul"
            ConfigManager().adapter  # returns an instance of adapter class

        .. important:: The adapter name is pre-populated from ``CN_CONFIG_ADAPTER`` environment variable.

            Supported config adapter name:

            - ``consul``: returns an instance of :class:`~jans.pycloudlib.config.consul_config.ConsulConfig`
            - ``kubernetes``: returns an instance of :class:`~jans.pycloudlib.config.kubernetes_config.KubernetesConfig`
            - ``google``: returns an instance of :class:`~jans.pycloudlib.config.google_config.GoogleConfig`
        """
        adapter = os.environ.get("CN_CONFIG_ADAPTER", "consul")

        if adapter == "consul":
            return ConsulConfig()

        if adapter == "kubernetes":
            return KubernetesConfig()

        if adapter == "google":
            return GoogleConfig()

        raise ValueError(f"Unsupported config adapter {adapter!r}")


class SecretManager(BaseConfiguration):
    """A subclass of :class:`~jans.pycloudlib.manager.BaseConfiguration` to manage secrets.

    This class acts as a proxy to specific secret adapter class.
    """

    @cached_property
    def adapter(self) -> SecretAdapter:  # noqa: D412
        """Get an instance of adapter class.

        Example:

        .. code-block:: python

            os.environ["CN_SECRET_ADAPTER"] = "vault"
            SecretManager().adapter  # returns an instance of adapter class

        .. important:: The adapter name is pre-populated from ``CN_SECRET_ADAPTER`` environment variable (i.e. ``CN_SECRET_ADAPTER=vault``).

            Supported config adapter name:

            - ``vault``: returns an instance of :class:`~jans.pycloudlib.secret.vault_secret.VaultSecret`
            - ``kubernetes``: returns an instance of :class:`~jans.pycloudlib.secret.kubernetes_secret.KubernetesSecret`
            - ``google``: returns an instance of :class:`~jans.pycloudlib.secret.google_secret.GoogleSecret`
        """
        adapter = os.environ.get("CN_SECRET_ADAPTER", "vault")

        if adapter == "vault":
            return VaultSecret()

        if adapter == "kubernetes":
            return KubernetesSecret()

        if adapter == "google":
            return GoogleSecret()

        raise ValueError(f"Unsupported secret adapter {adapter!r}")

    def to_file(
        self, key: str, dest: str, decode: bool = False, binary_mode: bool = False
    ) -> None:  # noqa: D412
        """Pull secret and write to a file.

        Example:

        .. code-block:: python

            # assuming there is secret with key ``server_cert`` that stores
            # server cert needed to be fetched as ``/etc/certs/server.crt``
            # file.
            SecretManager().to_file("server_cert", "/etc/certs/server.crt")

            # assuming there is secret with key ``server_jks`` that stores
            # server keystore needed to be fetched as ``/etc/certs/server.jks``
            # file.
            SecretManager().to_file(
                "server_jks",
                "/etc/certs/server.jks",
                decode=True,
                binary_mode=True,
            )

        :param key: Key name in secret backend.
        :param dest: Absolute path to file to write the secret to.
        :param decode: Decode the content of the secret.
        :param binary_mode: Write the file as binary.
        """
        mode = "w"
        if binary_mode:
            mode = "wb"
            # always decodes the bytes
            decode = True

        value = self.adapter.get(key)
        if decode:
            salt = self.adapter.get("encoded_salt")
            try:
                value = decode_text(value, salt).decode()
            except UnicodeDecodeError:
                # likely bytes from a binary
                value = decode_text(value, salt).decode("ISO-8859-1")

        with open(dest, mode) as f:
            if binary_mode:
                # convert to bytes
                value = value.encode("ISO-8859-1")
            f.write(value)

    def from_file(
        self, key: str, src: str, encode: bool = False, binary_mode: bool = False
    ) -> None:  # noqa: D412
        """Read from a file and put to secret.

        Example:

        .. code-block:: python

            # assuming there is file ``/etc/certs/server.crt`` need to be save
            # as ``server_crt`` secret.
            SecretManager().from_file("server_cert", "/etc/certs/server.crt")

            # assuming there is file ``/etc/certs/server.jks`` need to be save
            # as ``server_jks`` secret.
            SecretManager().from_file(
                "server_jks",
                "/etc/certs/server.jks",
                encode=True,
                binary_mode=True,
            )

        :param key: Key name in secret backend.
        :param src: Absolute path to file to read the secret from.
        :param encode: Encode the content of the file.
        :param binary_mode: Read the file as binary.
        """
        mode = "r"
        if binary_mode:
            mode = "rb"
            encode = True

        with open(src, mode) as f:
            try:
                value = f.read()
            except UnicodeDecodeError:
                raise ValueError(f"Looks like you're trying to read binary file {src}")

        if encode:
            salt = self.adapter.get("encoded_salt")
            value = encode_text(value, salt).decode()
        self.adapter.set(key, value)


@dataclass
class Manager:
    """Class acts as a container of config and secret manager.

    This object is not intended for direct use, use :func:`~jans.pycloudlib.manager.get_manager` function instead.

    :param config: An instance of config manager class.
    :param secret: An instance of secret manager class.
    """

    #: An instance of :class:`~jans.pycloudlib.manager.ConfigManager`
    config: ConfigManager

    #: An instance of :class:`~jans.pycloudlib.manager.SecretManager`
    secret: SecretManager


def get_manager() -> Manager:
    """Create an instance of :class:`~jans.pycloudlib.manager.Manager` class.

    The instance has ``config`` and ``secret`` attributes to interact with
    configs and secrets, for example:

    .. code-block:: python

        manager = get_manager()
        manager.config.get("hostname")
        manager.secret.get("ssl-cert")

    :returns: An instance of manager class.
    """
    config_mgr = ConfigManager()
    secret_mgr = SecretManager()
    return Manager(config_mgr, secret_mgr)
