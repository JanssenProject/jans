"""
jans.pycloudlib.secret.vault_secret
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This module contains secret adapter class to interact with Vault.
"""

import logging
import os
from typing import Any
from typing import Tuple
from typing import Union

import hvac

from jans.pycloudlib.secret.base_secret import BaseSecret
from jans.pycloudlib.utils import as_boolean
from jans.pycloudlib.utils import safe_value

logger = logging.getLogger(__name__)


class VaultSecret(BaseSecret):
    """This class interacts with Vault backend.

    The following environment variables are used to instantiate the client:

    - ``CN_SECRET_VAULT_HOST``
    - ``CN_SECRET_VAULT_PORT``
    - ``CN_SECRET_VAULT_SCHEME``
    - ``CN_SECRET_VAULT_VERIFY``
    - ``CN_SECRET_VAULT_ROLE_ID_FILE``
    - ``CN_SECRET_VAULT_SECRET_ID_FILE``
    - ``CN_SECRET_VAULT_CERT_FILE``
    - ``CN_SECRET_VAULT_KEY_FILE``
    - ``CN_SECRET_VAULT_CACERT_FILE``
    - ``CN_SECRET_VAULT_NAMESPACE``
    """

    def __init__(self):
        self.settings = {
            k: v
            for k, v in os.environ.items()
            if k.isupper() and k.startswith("CN_SECRET_VAULT_")
        }
        self.settings.setdefault(
            "CN_SECRET_VAULT_HOST", "localhost",
        )
        self.settings.setdefault(
            "CN_SECRET_VAULT_PORT", 8200,
        )
        self.settings.setdefault(
            "CN_SECRET_VAULT_SCHEME", "http",
        )
        self.settings.setdefault(
            "CN_SECRET_VAULT_VERIFY", False,
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

        self.settings.setdefault("CN_SECRET_VAULT_NAMESPACE", "jans")

        cert, verify = self._verify_cert(
            self.settings["CN_SECRET_VAULT_SCHEME"],
            self.settings["CN_SECRET_VAULT_VERIFY"],
            self.settings["CN_SECRET_VAULT_CACERT_FILE"],
            self.settings["CN_SECRET_VAULT_CERT_FILE"],
            self.settings["CN_SECRET_VAULT_KEY_FILE"],
        )

        self._request_warning(self.settings["CN_SECRET_VAULT_SCHEME"], verify)

        self.client = hvac.Client(
            url="{}://{}:{}".format(
                self.settings["CN_SECRET_VAULT_SCHEME"],
                self.settings["CN_SECRET_VAULT_HOST"],
                self.settings["CN_SECRET_VAULT_PORT"],
            ),
            cert=cert,
            verify=verify,
        )
        self.prefix = f"secret/{self.settings['CN_SECRET_VAULT_NAMESPACE']}"

    @property
    def role_id(self):
        """Get the Role ID from file where location is determined
        by ``CN_SECRET_VAULT_ROLE_ID_FILE`` environment variable.
        """
        try:
            with open(self.settings["CN_SECRET_VAULT_ROLE_ID_FILE"]) as f:
                role_id = f.read()
        except FileNotFoundError:
            role_id = ""
        return role_id

    @property
    def secret_id(self):
        """Get the Secret ID from file where location is determined
        by ``CN_SECRET_VAULT_SECRET_ID_FILE`` environment variable.
        """
        try:
            with open(self.settings["CN_SECRET_VAULT_SECRET_ID_FILE"]) as f:
                secret_id = f.read()
        except FileNotFoundError:
            secret_id = ""
        return secret_id

    def _authenticate(self) -> None:
        """Authenticate client.
        """
        if self.client.is_authenticated():
            return

        creds = self.client.auth.approle.login(self.role_id, self.secret_id, use_token=False)
        self.client.token = creds["auth"]["client_token"]

    def get(self, key: str, default: Any = None) -> Any:
        """Get value based on given key.

        :params key: Key name.
        :params default: Default value if key is not exist.
        :returns: Value based on given key or default one.
        """
        self._authenticate()
        sc = self.client.read(f"{self.prefix}/{key}")

        if not sc:
            return default
        return sc["data"]["value"]

    def set(self, key: str, value: Any) -> bool:
        """Set key with given value.

        :params key: Key name.
        :params value: Value of the key.
        :returns: A ``bool`` to mark whether config is set or not.
        """
        self._authenticate()
        val = {"value": safe_value(value)}

        # hvac.v1.Client.write checks for status code 200,
        # but Vault HTTP API returns 204 if request succeeded;
        # hence we're using lower level of `hvac.v1.Client` API to set key-val
        response = self.client._adapter.post(
            f"/v1/{self.prefix}/{key}", json=val,
        )
        return response.status_code == 204

    def all(self) -> dict:  # pragma: no cover
        """Get all key-value pairs.

        :returns: A ``dict`` of key-value pairs (if any).
        """
        return self.get_all()

    def _request_warning(self, scheme: str, verify: bool) -> None:
        """Emit warning about unverified request to unsecure Consul address.

        :params scheme: Scheme of Vault address.
        :params verify: Mark whether client needs to verify the address.
        """
        if scheme == "https" and verify is False:
            import urllib3

            urllib3.disable_warnings()
            logger.warning(
                "All requests to Vault will be unverified. "
                "Please adjust CN_SECRET_VAULT_SCHEME and "
                "CN_SECRET_VAULT_VERIFY environment variables.")

    def _verify_cert(self, scheme, verify, cacert_file, cert_file, key_file) -> Tuple[Union[None, tuple], Union[bool, str]]:
        """Verify client cert and key.

        :params scheme: Scheme of Consul address.
        :params verify: Mark whether client needs to verify the address.
        :params cacert_file: Path to CA cert file.
        :params cert_file: Path to client's cert file.
        :params key_file: Path to client's key file.
        :returns: A pair of cert key files (if exist) and verification.
        """
        cert = None

        if scheme == "https":
            verify = as_boolean(verify)

            # verify using CA cert (if any)
            if all([verify, os.path.isfile(cacert_file)]):
                verify = cacert_file

            if all([os.path.isfile(cert_file), os.path.isfile(key_file)]):
                cert = (cert_file, key_file)
        return cert, verify

    def set_all(self, data: dict) -> bool:
        """Set key-value pairs.

        :params data: Key-value pairs.
        :returns: A ``bool`` to mark whether config is set or not.
        """

        for k, v in data.items():
            self.set(k, v)
        return True

    def get_all(self) -> dict:
        """Get all key-value pairs.

        :returns: A ``dict`` of key-value pairs (if any).
        """
        self._authenticate()
        result = self.client.list(self.prefix)
        if not result:
            return {}
        return {key: self.get(key) for key in result["data"]["keys"]}
