# noqa: D104
import logging
import os
import typing as _t
from functools import cached_property

from jans.pycloudlib.base import BaseStorage
from jans.pycloudlib.secret.kubernetes_secret import KubernetesSecret  # noqa: F401
from jans.pycloudlib.secret.vault_secret import VaultSecret  # noqa: F401
from jans.pycloudlib.secret.google_secret import GoogleSecret  # noqa: F401
from jans.pycloudlib.secret.aws_secret import AwsSecret  # noqa: F401
from jans.pycloudlib.utils import decode_text
from jans.pycloudlib.utils import encode_text

logger = logging.getLogger(__name__)

SecretAdapter = _t.Union[VaultSecret, KubernetesSecret, GoogleSecret, AwsSecret]
"""Secrets adapter type.

Currently supports the following classes:

* [VaultSecret][jans.pycloudlib.secret.vault_secret.VaultSecret]
* [KubernetesSecret][jans.pycloudlib.secret.kubernetes_secret.KubernetesSecret]
* [GoogleSecret][jans.pycloudlib.secret.google_secret.GoogleSecret]
* [AwsSecret][jans.pycloudlib.secret.aws_secret.AwsSecret]
"""


class SecretStorage(BaseStorage):
    """This class manage secrets and act as a proxy to specific secret adapter class."""

    def __init__(self, lock_storage=None):
        self.lock_storage = lock_storage

    @cached_property
    def adapter(self) -> SecretAdapter:  # noqa: D412
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
        adapter = os.environ.get("CN_SECRET_ADAPTER", "vault")

        if adapter == "vault":
            return VaultSecret()

        if adapter == "kubernetes":
            return KubernetesSecret()

        if adapter == "google":
            return GoogleSecret()

        if adapter == "aws":
            return AwsSecret()
        raise ValueError(f"Unsupported secret adapter {adapter!r}")

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
            salt = self.adapter.get("encoded_salt")
            value = encode_text(value, salt).decode()
        self.adapter.set(key, value)

    def get_or_set(self, key: str, value: _t.Any, ttl: int = 10, retry_delay: float = 5.0, max_start_delay: float = 2.0):
        """Get or set a secret based on given key.

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
        name = f"secret.{key}"

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
    "KubernetesSecret",
    "VaultSecret",
    "GoogleSecret",
    "AwsSecret",
    "SecretStorage",
]
