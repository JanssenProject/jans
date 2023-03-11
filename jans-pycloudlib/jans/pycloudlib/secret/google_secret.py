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
from contextlib import suppress
from functools import cached_property
from math import ceil

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

        # max payload size (currently 64K)
        self.max_payload_size = 65_536

        # max multiparts for given prefix (currenty unused)
        self.max_multiparts = ceil(1_000_000 / self.max_payload_size)

        # iterable contains multipart secret names
        self.multiparts: list[str] = []

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
        # get a list of secrets with prefixed name
        resp = self.client.list_secrets(
            request={
                "parent": f"projects/{self.project_id}",
                "filter": f"name:{self.google_secret_name}",
            }
        )

        # collect all secret names (if any) for further request
        names = [f"{scr.name}/versions/{self.version_id}" for scr in resp]

        if not names:
            return {}

        data = {}
        payload = b""

        for name in names:
            # the secret with given name may not exist or have any versions created yet
            with suppress(NotFound):
                fragment = self.client.access_secret_version(request={"name": name})
                payload = payload + fragment.payload.data

        if not payload:
            return {}

        try:
            data = self._maybe_legacy_payload(payload)
        except lzma.LZMAError:
            data = json.loads(payload)

        # decoded payload
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
        logger.info(f"Adding key {key}.")

        payload = safe_value(all_)
        return self._add_secret_version_multipart(payload)

    def set_all(self, data: dict[str, _t.Any]) -> bool:
        """Push a full dictionary to secrets.

        Args:
            data: full dictionary to push. Used in initial creation of config and secret

        Returns:
            A boolean to mark whether secret is set or not.
        """
        # fetch existing data (if any) as we will merge them;
        # note that existing value will be overwritten
        all_ = self.get_all()

        for k, v in data.items():
            all_[k] = safe_value(v)

        payload = safe_value(all_)
        return self._add_secret_version_multipart(payload)

    def delete(self) -> None:
        """Delete the secret with the given name and all of its versions."""
        # Build the resource name of the secret.
        name = self.client.secret_path(self.project_id, self.google_secret_name)

        try:
            # Delete the secret.
            self.client.delete_secret(request={"name": name})
        except NotFound:
            logger.warning(f'Secret {self.google_secret_name} does not exist in the secret manager.')

    def _add_secret_version_multipart(self, payload: _t.AnyStr) -> bool:
        """Add a new secret version to the given secret with the provided payload.

        Args:
            payload: secret's payload
        """
        if isinstance(payload, str):
            # Convert the string payload into a bytes. This step can be omitted if you
            # pass in bytes instead of a str for the payload argument.
            payload_bytes = payload.encode("UTF-8")
        else:
            payload_bytes = payload

        data_length = sys.getsizeof(payload_bytes)
        parts = ceil(data_length / self.max_payload_size)

        if parts > 1:
            logger.warning(
                f"The secret payload size is {data_length} bytes and is exceeding max. size of {self.max_payload_size} bytes. "
                f"It will be splitted into {parts} parts."
            )

        for part in range(0, parts):
            name = self._prepare_secret_multipart(part)

            start_bytes = part * self.max_payload_size
            stop_bytes = (part + 1) * self.max_payload_size
            fragment = payload_bytes[start_bytes:stop_bytes]

            # Build the resource name of the parent secret.
            parent = self.client.secret_path(self.project_id, name)

            # Add the secret version.
            response = self.client.add_secret_version(
                request={"parent": parent, "payload": {"data": fragment}}
            )
            logger.info(f"Added secret version: {response.name}")
        return True

    def _prepare_secret_multipart(self, part: int) -> str:
        """Create a new secret with the given name.

        A secret is a logical wrapper around a collection of secret versions.
        Secret versions hold the actual secret material.

        Args:
            part: multipart number.

        Returns:
            Newly created secret's name.
        """
        name = self.google_secret_name

        if part > 0:
            name = f"{self.google_secret_name}-{part}"

        if name in self.multiparts:
            return name

        # Build the resource name of the parent project.
        parent = f"projects/{self.project_id}"

        # Secret with given name may already exists
        with suppress(AlreadyExists):
            # Create the secret.
            response = self.client.create_secret(
                request={
                    "parent": parent,
                    "secret_id": name,
                    "secret": {
                        "replication": {"automatic": {}},
                        "labels": {"multipart_enabled": "true"},
                    },
                }
            )
            logger.info(f"Created secret: {response.name}")
            self.multiparts.append(name)
        return name

    def _maybe_legacy_payload(self, payload: bytes) -> dict[str, _t.Any]:
        try:
            # previously data is compressed using zlib
            payload_str = zlib.decompress(payload).decode("UTF-8")
            logger.warning("Decompressed legacy data.")
        except zlib.error:
            payload_str = lzma.decompress(payload).decode("UTF-8")

        try:
            # previously data is double-encrypted
            data: dict[str, _t.Any] = json.loads(self._decrypt(payload_str))
            logger.warning("Loaded legacy data.")
        except binascii.Error:
            data = json.loads(payload_str)
        return data
