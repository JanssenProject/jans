"""
jans.pycloudlib.manager
~~~~~~~~~~~~~~~~~~~~~~~

This module contains config and secret helpers.
"""

import os
from collections import namedtuple
from typing import Any
from typing import AnyStr
from typing import NamedTuple

from jans.pycloudlib.config import ConsulConfig
from jans.pycloudlib.config import KubernetesConfig
from jans.pycloudlib.config import GoogleConfig
from jans.pycloudlib.secret import KubernetesSecret
from jans.pycloudlib.secret import VaultSecret
from jans.pycloudlib.secret import GoogleSecret
from jans.pycloudlib.utils import decode_text
from jans.pycloudlib.utils import encode_text


class ConfigManager:
    """This class acts as a proxy to specific config adapter class.

    Supported config adapter class:

    - :class:`~jans.pycloudlib.config.consul_config.ConsulConfig`
    - :class:`~jans.pycloudlib.config.kubernetes_config.KubernetesConfig`
    - :class:`~jans.pycloudlib.secret.google_config.GoogleConfig`
    """
    def __init__(self):
        _adapter = os.environ.get("CN_CONFIG_ADAPTER", "consul",)
        if _adapter == "consul":
            self.adapter = ConsulConfig()
        elif _adapter == "kubernetes":
            self.adapter = KubernetesConfig()
        elif _adapter == "google":
            self.adapter = GoogleConfig()
        else:
            self.adapter = None

    def get(self, key: str, default: Any = None) -> Any:
        """Get value based on given key.

        :params key: Key name.
        :params default: Default value if key is not exist.
        :returns: Value based on given key or default one.
        """
        return self.adapter.get(key, default)

    def set(self, key: str, value: Any) -> bool:
        """Set key with given value.

        :params key: Key name.
        :params value: Value of the key.
        :returns: A ``bool`` to mark whether config is set or not.
        """
        return self.adapter.set(key, value)

    def all(self) -> dict:  # noqa: A003
        """Get all key-value pairs (deprecated in favor of ``get_all``).

        :returns: A ``dict`` of key-value pairs (if any).
        """
        return self.get_all()

    def get_all(self) -> dict:
        """Get all key-value pairs.

        :returns: A ``dict`` of key-value pairs (if any).
        """
        return self.adapter.get_all()

    def set_all(self, data: dict) -> bool:
        """Set all key-value pairs.

        :params data: Key-value pairs.
        """
        return self.adapter.set_all(data)


class SecretManager:
    """This class acts as a proxy to specific secret adapter class.

    Supported secret adapter class:

    - :class:`~jans.pycloudlib.secret.vault_secret.VaultSecret`
    - :class:`~jans.pycloudlib.secret.kubernetes_secret.KubernetesSecret`
    - :class:`~jans.pycloudlib.secret.google_secret.GoogleSecret`
    """

    def __init__(self):
        _adapter = os.environ.get("CN_SECRET_ADAPTER", "vault",)
        if _adapter == "vault":
            self.adapter = VaultSecret()
        elif _adapter == "kubernetes":
            self.adapter = KubernetesSecret()
        elif _adapter == "google":
            self.adapter = GoogleSecret()
        else:
            self.adapter = None

    def get(self, key: str, default: Any = None) -> Any:
        """Get value based on given key.

        :params key: Key name.
        :params default: Default value if key is not exist.
        :returns: Value based on given key or default one.
        """
        return self.adapter.get(key, default)

    def set(self, key: str, value: Any) -> bool:  # noqa: A003
        """Set key with given value.

        :params key: Key name.
        :params value: Value of the key.
        :returns: A ``bool`` to mark whether config is set or not.
        """
        return self.adapter.set(key, value)

    def all(self) -> dict:  # noqa: A003
        """Get all key-value pairs (deprecated in favor of ``get_all``).

        :returns: A ``dict`` of key-value pairs (if any).
        """
        return self.get_all()

    def get_all(self) -> dict:
        """Get all key-value pairs.

        :returns: A ``dict`` of key-value pairs (if any).
        """
        return self.adapter.get_all()

    def set_all(self, data: dict) -> bool:
        """Set all key-value pairs.

        :params data: Key-value pairs.
        """
        return self.adapter.set_all(data)

    def to_file(
        self, key: str, dest: str, decode: bool = False, binary_mode: bool = False
    ) -> AnyStr:
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

        :params key: Key name in secret backend.
        :params dest: Absolute path to file to write the secret to.
        :params decode: Decode the content of the secret.
        :params binary_mode: Write the file as binary.
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
    ) -> None:
        """Put secret from a file.

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

        :params key: Key name in secret backend.
        :params src: Absolute path to file to read the secret from.
        :params encode: Encode the content of the file.
        :params binary_mode: Read the file as binary.
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


#: Object as a placeholder of config and secret manager.
#: This object is not intended for direct use, use ``get_manager``
#: function instead.
_Manager = namedtuple("_Manager", ["config", "secret"])


def get_manager() -> NamedTuple:
    """Convenient function to get config and secret manager instances.

    :returns: A ``namedtuple`` consists of :class:`~jans.pycloudlib.manager.ConfigManager`
        and :class:`~jans.pycloudlib.manager.SecretManager` instances.
    """
    config_mgr = ConfigManager()
    secret_mgr = SecretManager()
    return _Manager(config=config_mgr, secret=secret_mgr)
