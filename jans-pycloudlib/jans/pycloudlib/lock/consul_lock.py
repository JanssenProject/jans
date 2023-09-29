"""This module contains lock adapter class to interact with Consul."""

import logging
import os
import typing as _t

from consul import Consul

from jans.pycloudlib.lock.base_lock import BaseLock
from jans.pycloudlib.utils import as_boolean
from jans.pycloudlib.utils import safe_value

logger = logging.getLogger(__name__)

MaybeCert = _t.Union[tuple[str, str], None]
MaybeCacert = _t.Union[bool, str]


class ConsulLock(BaseLock):
    """This class interacts with Consul backend.

    The instance of this class is configured via environment variables.

    Supported environment variables:

    - `CN_LOCK_CONSUL_HOST`: hostname or IP of Consul (default to `localhost`).
    - `CN_LOCK_CONSUL_PORT`: port of Consul (default to `8500`).
    - `CN_LOCK_CONSUL_CONSISTENCY`: Consul consistency mode (choose one of `default`, `consistent`, or `stale`). Default to `stale` mode.
    - `CN_LOCK_CONSUL_SCHEME`: supported Consul scheme (`http` or `https`).
    - `CN_LOCK_CONSUL_VERIFY`: whether to verify cert or not (default to `false`).
    - `CN_LOCK_CONSUL_CACERT_FILE`: path to Consul CA cert file (default to `/etc/certs/consul_ca.crt`). This file will be used if it exists and `CN_LOCK_CONSUL_VERIFY` set to `true`.
    - `CN_LOCK_CONSUL_CERT_FILE`: path to Consul cert file (default to `/etc/certs/consul_client.crt`).
    - `CN_LOCK_CONSUL_KEY_FILE`: path to Consul key file (default to `/etc/certs/consul_client.key`).
    - `CN_LOCK_CONSUL_TOKEN_FILE`: path to file contains ACL token (default to `/etc/certs/consul_token`).
    - `CN_LOCK_CONSUL_NAMESPACE`: namespace used to create the lock tree, i.e. `jans/lock` (default to `jans`).
    """

    def __init__(self) -> None:
        self.host = os.environ.get("CN_LOCK_CONSUL_HOST", "localhost")
        self.port = int(os.environ.get("CN_LOCK_CONSUL_PORT", "8500"))
        self.consistency = os.environ.get("CN_LOCK_CONSUL_CONSISTENCY", "stale")
        self.scheme = os.environ.get("CN_LOCK_CONSUL_SCHEME", "http")
        self.verify = as_boolean(os.environ.get("CN_LOCK_CONSUL_VERIFY", "false"))
        self.cacert_file = os.environ.get("CN_LOCK_CONSUL_CACERT_FILE", "/etc/certs/consul_ca.crt")
        self.cert_file = os.environ.get("CN_LOCK_CONSUL_CERT_FILE", "/etc/certs/consul_client.crt")
        self.key_file = os.environ.get("CN_LOCK_CONSUL_KEY_FILE", "/etc/certs/consul_client.key")
        self.token_file = os.environ.get("CN_LOCK_CONSUL_TOKEN_FILE", "/etc/certs/consul_token")
        self.namespace = os.environ.get("CN_LOCK_CONSUL_NAMESPACE", "jans")
        self.prefix = f"{self.namespace}/lock/"

        cert, verify = self._verify_cert(self.scheme, self.verify, self.cacert_file, self.cert_file, self.key_file)

        self._request_warning(self.scheme, verify)

        self.client = Consul(
            host=self.host,
            port=self.port,
            token=self._token_from_file(self.token_file),
            scheme=self.scheme,
            consistency=self.consistency,
            verify=verify,
            cert=cert,
        )

    def _merge_path(self, key: str) -> str:
        """Add prefix to the key.

        For example, given the namespace is `jans`, prefix will be set as `jans/lock`
        and key `random`, calling this method returns `jans/lock/random` key.

        Args:
            key: Key name as relative path.

        Returns:
            Absolute path to prefixed key.
        """
        return "".join([self.prefix, key])

    def _unmerge_path(self, key: str) -> str:
        """Remove prefix from the key.

        For example, given the namespace is `jans`, prefix will be set as `jans/lock`
        and an absolute path `jans/lock/random`, calling this method returns `random` key.

        Args:
            key: Key name as relative path.

        Returns:
            Relative path to key.
        """
        return key[len(self.prefix):]

    def get(self, key: str, default: _t.Any = "") -> _t.Any:
        """Get value based on given key.

        Args:
            key: Key name.
            default: Default value if key is not exist.

        Returns:
            Value based on given key or default one.
        """
        _, result = self.client.kv.get(self._merge_path(key))
        if not result:
            return default
        # this is a bytes
        return result["Value"].decode()

    def set(self, key: str, value: _t.Any) -> bool:
        """Set key with given value.

        Args:
            key: Key name.
            value: Value of the key.

        Returns:
            A boolean to mark whether lock is set or not.
        """
        return bool(self.client.kv.put(self._merge_path(key), safe_value(value)))

    def _request_warning(self, scheme: str, verify: _t.Union[bool, str]) -> None:
        """Emit warning about unverified request to unsecure Consul address.

        Args:
            scheme: Scheme of Consul address.
            verify: Mark whether client needs to verify the address.
        """
        import urllib3

        if scheme == "https" and verify is False:
            urllib3.disable_warnings()
            logger.warning(
                "All requests to Consul will be unverified. "
                "Please adjust CN_LOCK_CONSUL_SCHEME and "
                "CN_LOCK_CONSUL_VERIFY environment variables.")

    def _token_from_file(self, path: str) -> str:
        """Get the token string from a path.

        Args:
            path: Path to file contains token string.

        Returns:
            Token string.
        """
        if not os.path.isfile(path):
            return ""

        with open(path) as fr:
            return fr.read().strip()

    def _verify_cert(
        self,
        scheme: str,
        verify: bool,
        cacert_file: str,
        cert_file: str,
        key_file: str,
    ) -> tuple[MaybeCert, MaybeCacert]:
        """Verify client cert and key.

        Args:
            scheme: Scheme of Consul address.
            verify: Mark whether client needs to verify the address.
            cacert_file: Path to CA cert file.
            cert_file: Path to client's cert file.
            key_file: Path to client's key file.

        Returns:
            A pair of cert key files (if exist) and verification.
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

        Args:
            data: Key-value pairs.

        Returns:
            A boolean to mark whether lock is set or not.
        """
        for k, v in data.items():
            self.set(k, v)
        return True

    def get_all(self) -> dict[str, _t.Any]:
        """Get all key-value pairs.

        Returns:
            A mapping of all locks (if any).
        """
        _, resultset = self.client.kv.get(self._merge_path(""), recurse=True)

        if not resultset:
            return {}

        return {
            self._unmerge_path(item["Key"]): item["Value"].decode()
            for item in resultset
        }

    def delete(self, key: str) -> bool:
        """Delete specific lock.

        Args:
            key: Key name.

        Returns:
            A boolean to mark whether lock is removed or not.
        """
        return bool(self.client.kv.delete(self._merge_path(key)))
