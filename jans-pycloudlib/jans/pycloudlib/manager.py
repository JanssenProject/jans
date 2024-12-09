"""This module contains config and secret helpers."""

import logging
import os
import socket
import typing as _t
from abc import ABC
from abc import abstractproperty
from functools import cached_property
from types import SimpleNamespace

from jans.pycloudlib.config import ConsulConfig
from jans.pycloudlib.config import KubernetesConfig
from jans.pycloudlib.config import GoogleConfig
from jans.pycloudlib.config import AwsConfig
from jans.pycloudlib.config import FileConfig
from jans.pycloudlib.secret import KubernetesSecret
from jans.pycloudlib.secret import VaultSecret
from jans.pycloudlib.secret import GoogleSecret
from jans.pycloudlib.secret import AwsSecret
from jans.pycloudlib.secret import FileSecret
from jans.pycloudlib.utils import decode_text
from jans.pycloudlib.utils import encode_text
from jans.pycloudlib.lock import LockRecord

logger = logging.getLogger(__name__)


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
    def remote_adapter(self) -> AdapterProtocol:  # pragma: no cover
        """Abstract attribute as a container of adapter instance.

        The adapter is used in the following public methods:

        - `get`
        - `get_all`
        - `set`
        - `set_all`

        !!! important
            Any subclass **MUST** returns an instance of remote adapter or raise exception.
        """

    @abstractproperty
    def local_adapter(self) -> AdapterProtocol:  # pragma: no cover
        """Abstract attribute as a container of local adapter instance.

        The adapter is used in the following public methods:

        - `get`
        - `get_all`
        - `set`
        - `set_all`

        !!! important
            Any subclass **MUST** returns an instance of local adapter or raise exception.
        """

    def get(self, key: str, default: _t.Any = "") -> _t.Any:
        """Get value based on given key.

        Args:
            key: Key name.
            default: Default value if key is not exist.

        Returns:
            Value based on given key or default one.
        """
        value = self.local_adapter.get(key, default)

        # either value is empty or missing, pull from the remote adapter instead
        if not value:
            value = self.remote_adapter.get(key, default)
        return value  # noqa: R504

    def set(self, key: str, value: _t.Any) -> bool:
        """Set key with given value.

        Args:
            key: Key name.
            value: Value of the key.

        Returns:
            A boolean to mark whether configuration is set or not.
        """
        try:
            result = self.local_adapter.set(key, value)
        except NotImplementedError:
            result = self.remote_adapter.set(key, value)
        return result  # noqa: R504

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
        data = self.local_adapter.get_all()

        # pre-populate missing attribute in local data by merging from remote data
        for k, v in self.remote_adapter.get_all().items():
            if not v or k not in data:
                data[k] = v
        return data

    def set_all(self, data: dict[str, _t.Any]) -> bool:
        """Set all key-value pairs.

        Args:
            data: Key-value pairs.

        Returns:
            A boolean to mark whether configuration is set or not.
        """
        try:
            result = self.local_adapter.set_all(data)
        except NotImplementedError:
            result = self.remote_adapter.set_all(data)
        return result  # noqa: R504


class ConfigManager(BaseConfiguration):
    """This class manage configs and act as a proxy to specific config adapter class."""

    @cached_property
    def remote_adapter(self) -> AdapterProtocol:  # noqa: D412
        """Get an instance of remote config adapter class.

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
        adapter_mappings = {
            "consul": ConsulConfig,
            "kubernetes": KubernetesConfig,
            "google": GoogleConfig,
            "aws": AwsConfig,
        }
        adapter_cls = adapter_mappings.get(self.remote_adapter_name)

        # unsupported adapter
        if not adapter_cls:
            raise ValueError(f"Unsupported config adapter {self.remote_adapter_name!r}")

        # resolve adapter instance
        return adapter_cls()

    @cached_property
    def local_adapter(self) -> AdapterProtocol:
        return FileConfig()

    @property
    def remote_adapter_name(self):
        return os.environ.get("CN_CONFIG_ADAPTER", "consul")


class SecretManager(BaseConfiguration):
    """This class manage secrets and act as a proxy to specific secret adapter class."""

    @cached_property
    def remote_adapter(self) -> AdapterProtocol:  # noqa: D412
        """Get an instance of secret adapter class.

        Returns:
            An instance of secret adapter class.

        Raises:
            ValueError: If the value of `CN_SECRET_ADAPTER` environment variable is not supported.

        Examples:

        ```py
        os.environ["CN_SECRET_ADAPTER"] = "vault"
        SecretManager().adapter  # returns an instance of adapter class
        ```

        The adapter name is pre-populated from `CN_SECRET_ADAPTER` environment variable (i.e. `CN_SECRET_ADAPTER=vault`).

        Supported config adapter name:

        - `vault`: returns an instance of [VaultSecret][jans.pycloudlib.secret.vault_secret.VaultSecret]
        - `kubernetes`: returns an instance of [KubernetesSecret][jans.pycloudlib.secret.kubernetes_secret.KubernetesSecret]
        - `google`: returns an instance of [GoogleSecret][jans.pycloudlib.secret.google_secret.GoogleSecret]
        - `aws`: returns an instance of [AwsSecret][jans.pycloudlib.secret.aws_secret.AwsSecret]
        """
        adapter_mappings = {
            "vault": VaultSecret,
            "kubernetes": KubernetesSecret,
            "google": GoogleSecret,
            "aws": AwsSecret,
        }
        adapter_cls = adapter_mappings.get(self.remote_adapter_name)

        # unsupported adapter
        if not adapter_cls:
            raise ValueError(f"Unsupported secret adapter {self.remote_adapter_name!r}")

        # resolve adapter instance
        return adapter_cls()

    @cached_property
    def local_adapter(self) -> AdapterProtocol:
        return FileSecret()

    def to_file(
        self, key: str, dest: str, decode: bool = False, binary_mode: bool = False
    ) -> None:  # noqa: D412
        """Pull secret and write to a file.

        Args:
            key: Key name in secret backend.
            dest: Absolute path to file to write the secret to.
            decode: Decode the content of the secret.
            binary_mode: Write the file as binary.

        Examples:

        ```py
        # assuming there is secret with key `server_cert` that stores
        # server cert needed to be fetched as `/etc/certs/server.crt`
        # file.
        SecretManager().to_file("server_cert", "/etc/certs/server.crt")

        # assuming there is secret with key `server_jks` that stores
        # server keystore needed to be fetched as `/etc/certs/server.jks`
        # file.
        SecretManager().to_file(
            "server_jks",
            "/etc/certs/server.jks",
            decode=True,
            binary_mode=True,
        )
        ```
        """
        mode = "w"
        if binary_mode:
            mode = "wb"
            # always decodes the bytes
            decode = True

        value = self.get(key)
        if decode:
            salt = self.get("encoded_salt")
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

        Args:
            key: Key name in secret backend.
            src: Absolute path to file to read the secret from.
            encode: Encode the content of the file.
            binary_mode: Read the file as binary.

        Examples:

        ```py
        # assuming there is file `/etc/certs/server.crt` need to be save
        # as `server_crt` secret.
        SecretManager().from_file("server_cert", "/etc/certs/server.crt")

        # assuming there is file `/etc/certs/server.jks` need to be save
        # as `server_jks` secret.
        SecretManager().from_file(
            "server_jks",
            "/etc/certs/server.jks",
            encode=True,
            binary_mode=True,
        )
        ```
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
            salt = self.get("encoded_salt")
            value = encode_text(value, salt).decode()
        self.set(key, value)

    @property
    def remote_adapter_name(self):
        return os.environ.get("CN_SECRET_ADAPTER", "vault")


class Manager:
    """Class acts as a container of sub-managers (config, secret, lock, etc.).

    This object is not intended for direct use, use [get_manager][jans.pycloudlib.manager.get_manager] function instead.
    """

    #: Assets that may need to be pulled from secret/configmaps and saved into a file
    _bootstrap_asset_mappings = {
        # format: <adapter name>: [(<env name>, <secret name>]
        "vault": [
            ("CN_SECRET_VAULT_ROLE_ID_FILE", "vault_role_id"),
            ("CN_SECRET_VAULT_SECRET_ID_FILE", "vault_secret_id"),
        ],
        "aws": [
            ("AWS_SHARED_CREDENTIALS_FILE", "aws_credentials"),
            ("AWS_CONFIG_FILE", "aws_config"),
            ("CN_AWS_SECRETS_REPLICA_FILE", "aws_replica_regions"),
        ],
        "google": [
            ("GOOGLE_APPLICATION_CREDENTIALS", "google_credentials"),
        ],
    }

    def __init__(self):
        self.config = ConfigManager()
        self.secret = SecretManager()

    def _bootstrap_assets(self, adapter):
        assets = self._bootstrap_asset_mappings.get(adapter) or []

        for env, secret in assets:
            path = os.environ.get(env) or ""

            if os.path.isfile(path):
                # asset already exists -- either mounted externally or pre-populated internally
                continue

            if path and (contents := self.secret.get(secret)):
                logger.info(f"Detected non-empty {secret=} and {env=} used by {adapter=}. The secret will be populated into {path!r}.")
                with open(path, "w") as f:
                    f.write(contents)

    def bootstrap(self):
        for adapter_name in [self.config.remote_adapter_name, self.secret.remote_adapter_name]:
            self._bootstrap_assets(adapter_name)

    def create_lock(
        self,
        name: str,
        owner: str = "",
        ttl: int = 10,
        retry_delay: float = 5.0,
        max_start_delay: float = 0.0,
    ):
        """Create lock object.

        Args:
            name: Name of the lock.
            owner: Owner of the lock.
            ttl: Duration of lock (in seconds).
            retry_delay: Delay before retrying to acquire lock (in seconds).
            max_start_delay: Max. delay before starting to acquire lock.

        Returns:
            Instance of `jans.pycloudlib.lock.LockRecord`.
        """
        # default to hostname as owner
        owner = owner or socket.gethostname()

        lock = LockRecord(name, owner, ttl, retry_delay, max_start_delay)
        lock.init_adapter(self)

        # pre-flight connection checking
        lock.check_adapter_connection()
        return lock

    @property
    def lock(self):  # pragma: no cover
        # backward-compat for .lock attribute
        ns = SimpleNamespace()
        ns.create_lock = self.create_lock
        logger.warning(f"Accessing {self.__class__.__name__}.lock attribute is deprecated")
        return ns


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
    manager = Manager()
    manager.bootstrap()
    return manager
