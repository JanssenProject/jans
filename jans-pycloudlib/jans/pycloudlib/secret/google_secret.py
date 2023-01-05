"""This module contains secret adapter class to interact with Google Secret."""

from __future__ import annotations

import binascii
import hashlib
import json
import logging
import lzma
import sys
import os
import typing as _t
import zlib
from functools import cached_property

from cryptography.hazmat.primitives.ciphers.aead import AESGCM
from cryptography.exceptions import InvalidTag
from google.cloud import secretmanager
from google.api_core.exceptions import AlreadyExists, NotFound

from jans.pycloudlib.secret.base_secret import BaseSecret
from jans.pycloudlib.utils import safe_value

logger = logging.getLogger(__name__)


class GoogleSecret(BaseSecret):
    """This class interacts with Google Secret backend.

    The instance of this class is configured via environment variables.

    Supported environment variables:

    - `CN_SECRET_GOOGLE_SECRET_VERSION_ID`: Deprecated in favor of `CN_GOOGLE_SECRET_VERSION_ID`.
    - `CN_SECRET_GOOGLE_SECRET_NAME_PREFIX`: Deprecated in favor of `CN_GOOGLE_SECRET_NAME_PREFIX`.
    - `CN_SECRET_GOOGLE_SECRET_MANAGER_PASSPHRASE`: Deprecated in favor of `CN_GOOGLE_SECRET_MANAGER_PASSPHRASE`.

    - `GOOGLE_APPLICATION_CREDENTIALS`: JSON file (contains Google credentials) that should be injected into container.
    - `GOOGLE_PROJECT_ID`: ID of Google project.
    - `CN_GOOGLE_SECRET_VERSION_ID`: Janssen secret version ID in Google Secret Manager. Defaults to `latest`, which is recommended.
    - `CN_GOOGLE_SECRET_NAME_PREFIX`: Prefix for Janssen secret in Google Secret Manager. Defaults to `jans`. If left `jans-secret` secret will be created.
    - `CN_GOOGLE_SECRET_MANAGER_PASSPHRASE`: Passphrase for Janssen secret in Google Secret Manager. This is recommended to be changed and defaults to `secret`.
    """

    def __init__(self) -> None:
        self.project_id = os.getenv("GOOGLE_PROJECT_ID", "")

        if "CN_SECRET_GOOGLE_SECRET_VERSION_ID" in os.environ:
            logger.warning(
                "Found CN_SECRET_GOOGLE_SECRET_VERSION_ID environment variable. "
                "Note that this environment variable is deprecated in favor of "
                "CN_GOOGLE_SECRET_VERSION_ID and soon will be removed."
            )
            self.version_id = os.environ["CN_SECRET_GOOGLE_SECRET_VERSION_ID"] or "latest"
        else:
            self.version_id = os.getenv("CN_GOOGLE_SECRET_VERSION_ID", "latest")

        if "CN_SECRET_GOOGLE_SECRET_NAME_PREFIX" in os.environ:
            logger.warning(
                "Found CN_SECRET_GOOGLE_SECRET_NAME_PREFIX environment variable. "
                "Note that this environment variable is deprecated in favor of "
                "CN_GOOGLE_SECRET_NAME_PREFIX and soon will be removed."
            )
            prefix = os.environ["CN_SECRET_GOOGLE_SECRET_NAME_PREFIX"] or "jans"
        else:
            prefix = os.getenv("CN_GOOGLE_SECRET_NAME_PREFIX", "jans")

        # secrets key value by default
        self.google_secret_name = f"{prefix}-secret"

        # the following attributes are deprecated and will be removed in the future
        if "CN_SECRET_GOOGLE_SECRET_MANAGER_PASSPHRASE" in os.environ:
            logger.warning(
                "Found CN_SECRET_GOOGLE_SECRET_MANAGER_PASSPHRASE environment variable. "
                "Note that this environment variable is deprecated in favor of "
                "CN_GOOGLE_SECRET_MANAGER_PASSPHRASE and soon will be removed."
            )
            self.passphrase = os.environ["CN_SECRET_GOOGLE_SECRET_MANAGER_PASSPHRASE"] or "secret"
        else:
            self.passphrase = os.getenv("CN_GOOGLE_SECRET_MANAGER_PASSPHRASE", "secret")

        self.salt = os.urandom(16)
        self.key = self._set_key()

    @cached_property
    def client(self) -> secretmanager.SecretManagerServiceClient:
        """Create the Secret Manager client."""
        return secretmanager.SecretManagerServiceClient()

    def _set_key(self) -> bytes:
        """Return key for for encrypting and decrypting payload.

        Returns:
            key
        """
        return hashlib.pbkdf2_hmac("sha256", self.passphrase.encode("utf8"), self.salt, 1000)

    def _decrypt(self, ciphertext: str) -> str:
        """Decrypt payload.

        Args:
            ciphertext: encrypted string to decrypt

        Returns:
            decrypted payload
        """
        self.salt, iv, cipher_bytes = map(binascii.unhexlify, ciphertext.split("-"))
        self.key = self._set_key()

        plaintext = b""
        try:
            aes = AESGCM(self.key)
            plaintext = aes.decrypt(iv, cipher_bytes, None)
            plaintext = lzma.decompress(plaintext)
        except InvalidTag:
            logger.error("Wrong passphrase used.")
        return plaintext.decode("utf8")

    def get_all(self) -> dict[str, _t.Any]:
        """Access the payload for the given secret version if one exists.

        The version can be a version number as a string (e.g. "5")
        or an alias (e.g. "latest").

        Returns:
            A mapping of secrets (if any)
        """
        # Try to get the latest resource name. Used in initialization. If the latest version doesn't exist
        # its a state where the secret and initial version must be created
        name = f"projects/{self.project_id}/secrets/{self.google_secret_name}/versions/latest"

        try:
            self.client.access_secret_version(request={"name": name})
        except NotFound:
            logger.warning("Secret may not exist or have any versions created yet")
            self.create_secret()
            self.add_secret_version(safe_value({}))

        # Build the resource name of the secret version.
        name = f"projects/{self.project_id}/secrets/{self.google_secret_name}/versions/{self.version_id}"
        data = {}

        try:
            # Access the secret version.
            response = self.client.access_secret_version(request={"name": name})

        except NotFound:
            logger.warning(
                "Secret may not exist or have any versions created. Make sure "
                "CN_GOOGLE_SECRET_VERSION_ID and CN_GOOGLE_SECRET_NAME_PREFIX "
                "environment variables are set correctly. In Google secrets manager, "
                "a secret with the name jans-secret would have "
                "CN_GOOGLE_SECRET_NAME_PREFIX set to jans."
            )

        else:
            # logger.info(f"Secret {self.google_secret_name} has been found. Accessing version {self.version_id}.")
            # backward-compat checks
            try:
                # previously data is compressed using zlib
                payload = zlib.decompress(response.payload.data).decode("UTF-8")
                logger.warning("Decompressed legacy data.")
            except zlib.error:
                payload = lzma.decompress(response.payload.data).decode("UTF-8")

            try:
                # previously data is double-encrypted
                data = json.loads(self._decrypt(payload))
                logger.warning("Loaded legacy data.")
            except binascii.Error:
                data = json.loads(payload)
        return data

    def get(self, key: str, default: _t.Any = "") -> _t.Any:
        """Get value based on given key.

        Args:
            key: Key name.
            default: Default value if key is not exist.

        Returns:
            Value based on given key or default one.
        """
        result = self.get_all()
        return result.get(key) or default

    def set(self, key: str, value: _t.Any) -> bool:
        """Set key with given value.

        Args:
            key: Key name.
            value: Value of the key.

        Returns:
            A boolean to mark whether secret is set or not.
        """
        all_ = self.get_all()
        all_[key] = safe_value(value)

        self.create_secret()

        logger.info(f'Adding key {key} to google secret manager')
        logger.info(f'Size of secret payload : {sys.getsizeof(safe_value(all_))} bytes')
        secret_version_bool = self.add_secret_version(safe_value(all_))
        return secret_version_bool

    def set_all(self, data: dict[str, _t.Any]) -> bool:
        """Push a full dictionary to secrets.

        Args:
            data: full dictionary to push. Used in initial creation of config and secret

        Returns:
            A boolean to mark whether secret is set or not.
        """
        all_ = {}

        for k, v in data.items():
            all_[k] = safe_value(v)

        self.create_secret()

        logger.info(f'Size of secret payload : {sys.getsizeof(safe_value(all_))} bytes')
        secret_version_bool = self.add_secret_version(safe_value(all_))
        return secret_version_bool

    def create_secret(self) -> None:
        """Create a new secret with the given name.

        A secret is a logical wrapper around a collection of secret versions.
        Secret versions hold the actual secret material.
        """
        # Build the resource name of the parent project.
        parent = f"projects/{self.project_id}"

        try:
            # Create the secret.
            response = self.client.create_secret(
                request={
                    "parent": parent,
                    "secret_id": self.google_secret_name,
                    "secret": {"replication": {"automatic": {}}},
                }
            )
            logger.info(f"Created secret: {response.name}")
        except AlreadyExists:
            logger.warning(f'Secret {self.google_secret_name} already exists. A new version will be created.')

    def add_secret_version(self, payload: _t.AnyStr) -> bool:
        """Add a new secret version to the given secret with the provided payload.

        Args:
            payload: encrypted payload
        """
        # Build the resource name of the parent secret.
        parent = self.client.secret_path(self.project_id, self.google_secret_name)

        if isinstance(payload, str):
            # Convert the string payload into a bytes. This step can be omitted if you
            # pass in bytes instead of a str for the payload argument.
            payload_bytes = payload.encode("UTF-8")
        else:
            payload_bytes = payload

        # compress the payload
        payload_bytes = lzma.compress(payload_bytes)

        logger.info(f'Size of final compressed secret payload : {sys.getsizeof(payload_bytes)} bytes')

        # Add the secret version.
        response = self.client.add_secret_version(
            request={"parent": parent, "payload": {"data": payload_bytes}}
        )

        logger.info(f"Added secret version: {response.name}")
        return bool(response)

    def delete(self) -> None:
        """Delete the secret with the given name and all of its versions."""
        # Build the resource name of the secret.
        name = self.client.secret_path(self.project_id, self.google_secret_name)

        try:
            # Delete the secret.
            self.client.delete_secret(request={"name": name})
        except NotFound:
            logger.warning(f'Secret {self.google_secret_name} does not exist in the secret manager.')
