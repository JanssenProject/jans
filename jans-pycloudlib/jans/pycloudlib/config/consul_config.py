"""This module contains config adapter class to interact with Consul."""

import logging
import os
import typing as _t

from consul import Consul

from jans.pycloudlib.config.base_config import BaseConfig
from jans.pycloudlib.utils import as_boolean
from jans.pycloudlib.utils import safe_value

logger = logging.getLogger(__name__)

MaybeCert = _t.Union[tuple[str, str], None]
MaybeCacert = _t.Union[bool, str]


class ConsulConfig(BaseConfig):
    """This class interacts with Consul backend.

    The following environment variables are used to instantiate the client:

    - ``CN_CONFIG_CONSUL_HOST``
    - ``CN_CONFIG_CONSUL_PORT``
    - ``CN_CONFIG_CONSUL_CONSISTENCY``
    - ``CN_CONFIG_CONSUL_SCHEME``
    - ``CN_CONFIG_CONSUL_VERIFY``
    - ``CN_CONFIG_CONSUL_CACERT_FILE``
    - ``CN_CONFIG_CONSUL_CERT_FILE``
    - ``CN_CONFIG_CONSUL_KEY_FILE``
    - ``CN_CONFIG_CONSUL_TOKEN_FILE``
    - ``CN_CONFIG_CONSUL_NAMESPACE``
    """

    def __init__(self) -> None:
        self.settings = {
            k: v
            for k, v in os.environ.items()
            if k.isupper() and k.startswith("CN_CONFIG_CONSUL_")
        }

        self.settings.setdefault(
            "CN_CONFIG_CONSUL_HOST", "localhost",
        )

        self.settings.setdefault(
            "CN_CONFIG_CONSUL_PORT", "8500",
        )

        self.settings.setdefault(
            "CN_CONFIG_CONSUL_CONSISTENCY", "stale",
        )

        self.settings.setdefault(
            "CN_CONFIG_CONSUL_SCHEME", "http",
        )

        self.settings.setdefault(
            "CN_CONFIG_CONSUL_VERIFY", "false",
        )

        self.settings.setdefault(
            "CN_CONFIG_CONSUL_CACERT_FILE", "/etc/certs/consul_ca.crt",
        )

        self.settings.setdefault(
            "CN_CONFIG_CONSUL_CERT_FILE", "/etc/certs/consul_client.crt",
        )

        self.settings.setdefault(
            "CN_CONFIG_CONSUL_KEY_FILE", "/etc/certs/consul_client.key",
        )

        self.settings.setdefault(
            "CN_CONFIG_CONSUL_TOKEN_FILE", "/etc/certs/consul_token",
        )

        self.settings.setdefault("CN_CONFIG_CONSUL_NAMESPACE", "jans")

        self.prefix = f"{self.settings['CN_CONFIG_CONSUL_NAMESPACE']}/config/"
        cert, verify = self._verify_cert(
            self.settings["CN_CONFIG_CONSUL_SCHEME"],
            as_boolean(self.settings["CN_CONFIG_CONSUL_VERIFY"]),
            self.settings["CN_CONFIG_CONSUL_CACERT_FILE"],
            self.settings["CN_CONFIG_CONSUL_CERT_FILE"],
            self.settings["CN_CONFIG_CONSUL_KEY_FILE"],
        )

        self._request_warning(self.settings["CN_CONFIG_CONSUL_SCHEME"], verify)

        self.client = Consul(
            host=self.settings["CN_CONFIG_CONSUL_HOST"],
            port=int(self.settings["CN_CONFIG_CONSUL_PORT"]),
            token=self._token_from_file(self.settings["CN_CONFIG_CONSUL_TOKEN_FILE"]),
            scheme=self.settings["CN_CONFIG_CONSUL_SCHEME"],
            consistency=self.settings["CN_CONFIG_CONSUL_CONSISTENCY"],
            verify=verify,
            cert=cert,
        )

    def _merge_path(self, key: str) -> str:
        """Add prefix to the key.

        For example, given the namespace is ``jans``, prefix will be set as ``jans/config``
        and key ``random``, calling this method returns ``jans/config/random`` key.

        :param key: Key name as relative path.
        :returns: Absolute path to prefixed key.
        """
        return "".join([self.prefix, key])

    def _unmerge_path(self, key: str) -> str:
        """Remove prefix from the key.

        For example, given the namespace is ``jans``, prefix will be set as ``jans/config``
        and an absolute path ``jans/config/random``, calling this method returns ``random`` key.

        :param key: Key name as relative path.
        :returns: Relative path to key.
        """
        return key[len(self.prefix):]

    def get(self, key: str, default: _t.Any = "") -> _t.Any:
        """Get value based on given key.

        :param key: Key name.
        :param default: Default value if key is not exist.
        :returns: Value based on given key or default one.
        """
        _, result = self.client.kv.get(self._merge_path(key))
        if not result:
            return default
        # this is a bytes
        return result["Value"].decode()

    def set(self, key: str, value: _t.Any) -> bool:
        """Set key with given value.

        :param key: Key name.
        :param value: Value of the key.
        :returns: A ``bool`` to mark whether config is set or not.
        """
        return bool(self.client.kv.put(self._merge_path(key), safe_value(value)))

    def _request_warning(self, scheme: str, verify: _t.Union[bool, str]) -> None:
        """Emit warning about unverified request to unsecure Consul address.

        :param scheme: Scheme of Consul address.
        :param verify: Mark whether client needs to verify the address.
        """
        import urllib3

        if scheme == "https" and verify is False:
            urllib3.disable_warnings()  # type: ignore
            logger.warning(
                "All requests to Consul will be unverified. "
                "Please adjust CN_CONFIG_CONSUL_SCHEME and "
                "CN_CONFIG_CONSUL_VERIFY environment variables.")

    def _token_from_file(self, path: str) -> str:
        """Get the token string from a path.

        :param path: Path to file contains token string.
        :returns: Token string.
        """
        if not os.path.isfile(path):
            return ""

        with open(path) as fr:
            token = fr.read().strip()
        return token

    def _verify_cert(
        self,
        scheme: str,
        verify: bool,
        cacert_file: str,
        cert_file: str,
        key_file: str,
    ) -> tuple[MaybeCert, MaybeCacert]:
        """Verify client cert and key.

        :param scheme: Scheme of Consul address.
        :param verify: Mark whether client needs to verify the address.
        :param cacert_file: Path to CA cert file.
        :param cert_file: Path to client's cert file.
        :param key_file: Path to client's key file.
        :returns: A pair of cert key files (if exist) and verification.
        """
        cert = None
        maybe_cacert: MaybeCacert = as_boolean(verify)

        if scheme == "https":
            # verify using CA cert (if any)
            if all([maybe_cacert, os.path.isfile(cacert_file)]):
                maybe_cacert = cacert_file

            if all([os.path.isfile(cert_file), os.path.isfile(key_file)]):
                cert = (cert_file, key_file)
        return cert, maybe_cacert

    def set_all(self, data: dict[str, _t.Any]) -> bool:
        """Set key-value pairs.

        :param data: Key-value pairs.
        :returns: A ``bool`` to mark whether config is set or not.
        """
        for k, v in data.items():
            self.set(k, v)
        return True

    def get_all(self) -> dict[str, _t.Any]:
        """Get all key-value pairs.

        :returns: A ``dict`` of key-value pairs (if any).
        """
        _, resultset = self.client.kv.get(self._merge_path(""), recurse=True)

        if not resultset:
            return {}

        return {
            self._unmerge_path(item["Key"]): item["Value"].decode()
            for item in resultset
        }
