"""This module contains secret adapter class to interact with Vault."""

import typing as _t
import logging
import os

import hvac

from jans.pycloudlib.secret.base_secret import BaseSecret
from jans.pycloudlib.utils import as_boolean
from jans.pycloudlib.utils import safe_value

logger = logging.getLogger(__name__)

MaybeCert = _t.Union[tuple[str, str], None]
MaybeCacert = _t.Union[bool, str]


class VaultSecret(BaseSecret):
    """This class interacts with Vault backend.

    The instance of this class is configured via environment variables.

    Supported environment variables:

    - `CN_SECRET_VAULT_SCHEME`: supported Vault scheme (`http` or `https`).
    - `CN_SECRET_VAULT_HOST`: hostname or IP of Vault (default to `localhost`).
    - `CN_SECRET_VAULT_PORT`: port of Vault (default to `8200`).
    - `CN_SECRET_VAULT_VERIFY`: whether to verify cert or not (default to `false`).
    - `CN_SECRET_VAULT_ROLE_ID_FILE`: path to file contains Vault AppRole role ID (default to `/etc/certs/vault_role_id`).
    - `CN_SECRET_VAULT_SECRET_ID_FILE`: path to file contains Vault AppRole secret ID (default to `/etc/certs/vault_secret_id`).
    - `CN_SECRET_VAULT_CERT_FILE`: path to Vault cert file (default to `/etc/certs/vault_client.crt`).
    - `CN_SECRET_VAULT_KEY_FILE`: path to Vault key file (default to `/etc/certs/vault_client.key`).
    - `CN_SECRET_VAULT_CACERT_FILE`: path to Vault CA cert file (default to `/etc/certs/vault_ca.crt`). This file will be used if it exists and `CN_SECRET_VAULT_VERIFY` set to `true`.
    - `CN_SECRET_VAULT_NAMESPACE`: namespace used to access secrets (default to empty string).
    - `CN_SECRET_VAULT_KV_PATH`: path to KV secrets engine (default to `secret`).
    - `CN_SECRET_VAULT_PREFIX`: base prefix name used to build secret path (default to `jans`).
    - `CN_SECRET_VAULT_KV_VERSION`: KV version (default to `1`).
    - `CN_SECRET_VAULT_APPROLE_PATH`: path to AppRole (default to `approle`).
    """

    def __init__(self) -> None:
        self.settings = {
            k: v
            for k, v in os.environ.items()
            if k.isupper() and k.startswith("CN_SECRET_VAULT_")
        }
        self.settings.setdefault(
            "CN_SECRET_VAULT_HOST", "localhost",
        )
        self.settings.setdefault(
            "CN_SECRET_VAULT_PORT", "8200",
        )
        self.settings.setdefault(
            "CN_SECRET_VAULT_SCHEME", "http",
        )
        self.settings.setdefault(
            "CN_SECRET_VAULT_VERIFY", "false",
        )
        self.settings.setdefault(
            "CN_SECRET_VAULT_ROLE_ID_FILE", "/etc/certs/vault_role_id",
        )
        self.settings.setdefault(
            "CN_SECRET_VAULT_SECRET_ID_FILE", "/etc/certs/vault_secret_id",
        )
        self.settings.setdefault(
            "CN_SECRET_VAULT_CERT_FILE", "/etc/certs/vault_client.crt",
        )
        self.settings.setdefault(
            "CN_SECRET_VAULT_KEY_FILE", "/etc/certs/vault_client.key",
        )
        self.settings.setdefault(
            "CN_SECRET_VAULT_CACERT_FILE", "/etc/certs/vault_ca.crt",
        )

        self.settings.setdefault("CN_SECRET_VAULT_NAMESPACE", "")
        self.settings.setdefault("CN_SECRET_VAULT_PREFIX", "jans")
        self.settings.setdefault("CN_SECRET_VAULT_KV_PATH", "secret")
        self.settings.setdefault("CN_SECRET_VAULT_APPROLE_PATH", "approle")

        cert, verify = self._verify_cert(
            self.settings["CN_SECRET_VAULT_SCHEME"],
            as_boolean(self.settings["CN_SECRET_VAULT_VERIFY"]),
            self.settings["CN_SECRET_VAULT_CACERT_FILE"],
            self.settings["CN_SECRET_VAULT_CERT_FILE"],
            self.settings["CN_SECRET_VAULT_KEY_FILE"],
        )

        self._request_warning(self.settings["CN_SECRET_VAULT_SCHEME"], verify)

        client_opts = {
            "url": f"{self.settings['CN_SECRET_VAULT_SCHEME']}://{self.settings['CN_SECRET_VAULT_HOST']}:{self.settings['CN_SECRET_VAULT_PORT']}",
            "cert": cert,
            "verify": verify,
            "namespace": self.settings["CN_SECRET_VAULT_NAMESPACE"],
        }

        self.client = hvac.Client(**client_opts)
        self.client.secrets.kv.default_kv_version = self.kv_version

    @property
    def kv_version(self):
        return int(os.environ.get("CN_SECRET_VAULT_KV_VERSION", "1"))

    @property
    def role_id(self) -> str:
        """Get the Role ID from file.

        The file location is determined by
        `CN_SECRET_VAULT_ROLE_ID_FILE` environment variable.
        """
        try:
            with open(self.settings["CN_SECRET_VAULT_ROLE_ID_FILE"]) as f:
                role_id = f.read().strip()
        except FileNotFoundError:
            role_id = ""
        return role_id

    @property
    def secret_id(self) -> str:
        """Get the Secret ID from file.

        The file location is determined by
        `CN_SECRET_VAULT_SECRET_ID_FILE` environment variable.
        """
        try:
            with open(self.settings["CN_SECRET_VAULT_SECRET_ID_FILE"]) as f:
                secret_id = f.read().strip()
        except FileNotFoundError:
            secret_id = ""  # nosec: B105
        return secret_id

    def _authenticate(self) -> None:
        """Authenticate client."""
        if self.client.is_authenticated():
            return

        creds = self.client.auth.approle.login(self.role_id, self.secret_id, use_token=False, mount_point=self.settings["CN_SECRET_VAULT_APPROLE_PATH"])
        self.client.token = creds["auth"]["client_token"]

    def get(self, key: str, default: _t.Any = "") -> _t.Any:
        """Get value based on given key.

        Args:
            key: Key name.
            default: Default value if key is not exist.

        Returns:
            Value based on given key or default one.
        """
        self._authenticate()

        try:
            sc = self.client.secrets.kv.read_secret(
                path=f"{self.settings['CN_SECRET_VAULT_PREFIX']}/{key}",
                mount_point=self.settings["CN_SECRET_VAULT_KV_PATH"],
            )
        except hvac.exceptions.InvalidPath:
            # maybe deleted/destroyed
            sc = {}

        if not sc:
            return default

        if self.kv_version == 2:
            return sc["data"]["data"]["value"]
        return sc["data"]["value"]

    def set(self, key: str, value: _t.Any) -> bool:
        """Set key with given value.

        Args:
            key: Key name.
            value: Value of the key.

        Returns:
            A boolean to mark whether config is set or not.
        """
        self._authenticate()
        val = {"value": safe_value(value)}

        response = self.client.secrets.kv.create_or_update_secret(
            path=f"{self.settings['CN_SECRET_VAULT_PREFIX']}/{key}",
            mount_point=self.settings["CN_SECRET_VAULT_KV_PATH"],
            secret=val,
        )
        if self.kv_version == 2:
            return bool(response["data"]["created_time"])
        return bool(response.status_code == 204)

    def _request_warning(self, scheme: str, verify: _t.Union[bool, str]) -> None:
        """Emit warning about unverified request to unsecure Consul address.

        Args:
            scheme: Scheme of Vault address.
            verify: Mark whether client needs to verify the address.
        """
        import urllib3

        if scheme == "https" and verify is False:
            urllib3.disable_warnings()
            logger.warning(
                "All requests to Vault will be unverified. "
                "Please adjust CN_SECRET_VAULT_SCHEME and "
                "CN_SECRET_VAULT_VERIFY environment variables.")

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
            A boolean to mark whether secret is set or not.
        """
        for k, v in data.items():
            self.set(k, v)
        return True

    def get_all(self) -> dict[str, _t.Any]:
        """Get all key-value pairs.

        Returns:
            A mapping of secrets (if any).
        """
        self._authenticate()
        try:
            result = self.client.secrets.kv.list_secrets(
                path=self.settings["CN_SECRET_VAULT_PREFIX"],
                mount_point=self.settings["CN_SECRET_VAULT_KV_PATH"],
            )
        except hvac.exceptions.InvalidPath:
            # maybe deleted/destroyed
            result = {}

        if not result:
            return {}
        return {key: self.get(key) for key in result["data"]["keys"]}
