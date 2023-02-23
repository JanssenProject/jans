"""This module contains secret adapter class to interact with AWS Secrets Manager."""

import json
import logging
import lzma
import os
import sys
import typing as _t
from contextlib import suppress
from functools import cached_property
from functools import partial
from pathlib import Path

import boto3
from botocore.exceptions import ClientError
from botocore.exceptions import NoCredentialsError
from botocore.exceptions import NoRegionError
from math import ceil

from jans.pycloudlib.secret.base_secret import BaseSecret
from jans.pycloudlib.utils import safe_value

logger = logging.getLogger(__name__)


class AwsSecret(BaseSecret):
    """This class interacts with AWS Secrets Manager backend.

    If the secret's size is larger than the size limit (64KB), it will be stored in multiple secrets (maximum 10).

    The instance of this class is configured via environment variables.

    Supported environment variables:

    - `CN_AWS_SECRETS_ENDPOINT_URL`: The URL of AWS secretsmanager service (if omitted, will use the one in specified region).
    - `CN_AWS_SECRETS_PREFIX`: The prefix name of the secrets (default to `jans`).
    - `CN_AWS_SECRETS_REPLICA_FILE`: The location of file contains replica regions definition (if any). This file is mostly used in primary region.

    The following environment variables are used by the underlying AWS SDK:

    - `AWS_DEFAULT_REGION`: The default AWS Region to use, for example, `us-west-1` or `us-west-2`.
    - `AWS_SHARED_CREDENTIALS_FILE`: The location of the shared credentials file used by the client.
    - `AWS_CONFIG_FILE`: The location of the config file used by the client.
    - `AWS_PROFILE`: The default profile to use, if any.

    Example of AWS credentials file:

    ```
    [default]
    aws_access_key_id = DEFAULT_ACCESS_KEY_ID
    aws_secret_access_key = DEFAULT_SECRET_ACCESS_KEY

    [jans]
    aws_access_key_id = JANS_ACCESS_KEY_ID
    aws_secret_access_key = JANS_SECRET_ACCESS_KEY
    ```

    Example of AWS config file:

    ```
    [default]
    region = us-east-1

    [profile jans]
    region = us-west-1
    ```

    Example of replica regions:

    ```
    [
        {"Region": "us-west-1"}
    ]
    ```
    """

    def __init__(self) -> None:  # noqa: D107
        # unique name used as prefix to distinguish with other secrets
        # a typical usage is to use vendor/organization name
        prefix = os.environ.get("CN_AWS_SECRETS_PREFIX", "jans")

        # the secrets name will use `_` instead of `-` char to avoid clashing with generated suffix by AWS;
        # see https://docs.aws.amazon.com/cli/latest/reference/secretsmanager/create-secret.html#options
        self.basepath = f"{prefix}_secrets"

        # iterable contains multipart secret names
        self.multiparts: list[str] = []

        # max size of payload (currently 64K)
        self.max_payload_size = 65536

    @cached_property
    def client(self) -> boto3.session.Session.client:
        """Create the Secret Manager client."""
        try:
            client = boto3.client(
                "secretsmanager",
                endpoint_url=os.environ.get("CN_AWS_SECRETS_ENDPOINT_URL") or None,
            )
            return client
        except NoRegionError:
            raise RuntimeError(
                "AWS region is not specified. Please specify the region in a file "
                "pointed by AWS_CONFIG_FILE environment variable, or specify profile "
                "name via AWS_PROFILE environment variable, or set the region "
                "via AWS_DEFAULT_REGION environment variable."
            )

    def get_all(self) -> dict[str, _t.Any]:
        """Get all key-value pairs.

        Returns:
            A mapping of secrets (if any).
        """
        # get all existing multipart secrets
        resp = self.client.list_secrets(
            Filters=[{"Key": "name", "Values": [self.basepath]}],
        )
        names = [secret["Name"] for secret in resp["SecretList"]]

        if not names:
            return {}

        payload = b"".join([
            self.client.get_secret_value(SecretId=name)["SecretBinary"]
            for name in names
        ])

        try:
            # previously data is compressed using lzma
            data: dict[str, _t.Any] = json.loads(lzma.decompress(payload).decode())
            logger.warning("Loaded legacy data.")
        except lzma.LZMAError:
            data = json.loads(payload.decode())
        return data

    def get(self, key: str, default: _t.Any = "") -> _t.Any:
        """Get value based on given key.

        Args:
            key: Key name.
            default: Default value if key is not exist.

        Returns:
            Value based on given key or default one.
        """
        data = self.get_all()
        return data.get(key) or default

    def set(self, key: str, value: _t.Any) -> bool:
        """Set key with given value.

        Args:
            key: Key name.
            value: Value of the key.

        Returns:
            A boolean to indicate if secret was set successfully.
        """
        data = self.get_all()
        data[key] = safe_value(value)
        return self._update_secret_multipart(json.dumps(data))

    def set_all(self, data: dict[str, _t.Any]) -> bool:
        """Set all key-value pairs.

        Args:
            data: key-value pairs of secrets.

        Returns:
            A boolean indicating if the operation was successful.
        """
        # fetch existing data (if any) as we will merge them;
        # note that existing value will be overwritten
        payload = self.get_all()

        for k, v in data.items():
            # ensure key-value that has bytes is converted to text
            payload[k] = safe_value(v)
        return self._update_secret_multipart(json.dumps(payload))

    @cached_property
    def replica_regions(self) -> list[dict[str, _t.Any]]:
        """Get replica regions specified in a file.

        The location of the file is pointed by `CN_AWS_SECRETS_REPLICA_FILE` environment variable.
        """
        regions = []

        with suppress(FileNotFoundError, TypeError, IsADirectoryError):
            file_ = os.environ.get("CN_AWS_SECRETS_REPLICA_FILE", "")
            try:
                txt = Path(file_).read_text().strip()
                regions = json.loads(txt)
            except json.decoder.JSONDecodeError as exc:
                raise ValueError(f"Unable to load replica regions from {file_}; reason={exc}")
            else:
                # ensure regions does not include current client's region
                regions = [
                    region for region in regions
                    if region["Region"] != self.client.meta.region_name
                ]
        return regions

    def _update_secret_multipart(self, payload: _t.AnyStr) -> bool:  # noqa: D102
        if isinstance(payload, str):
            # Convert the string payload into a bytes. This step can be omitted if you
            # pass in bytes instead of a str for the payload argument.
            payload_bytes = payload.encode()
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
            self.client.update_secret(SecretId=name, SecretBinary=fragment)
        return True

    def _prepare_secret_multipart(self, part: int) -> str:
        """Check individual secrets if they exist or create new secrets with empty value if they don't.

        Args:
            part: part number of a multipart secret.

        Returns:
            Newly created secret's name
        """
        name = self.basepath

        if part > 0:
            name = f"{self.basepath}_{part}"

        if name in self.multiparts:
            return name

        try:
            # get the secret
            self.client.get_secret_value(SecretId=name)

            # mark the secret as exists so subsequent checks made by
            # client instance won't need to make requests to AWS service
            self.multiparts.append(name)

        except ClientError as exc:
            # raise exception if not related to missing secrets;
            # note that missing secrets will be created
            if exc.response["Error"]["Code"] != "ResourceNotFoundException":
                raise RuntimeError(f"Unable to access AWS Secrets Manager service; reason={exc}")

            create_secret = partial(
                self.client.create_secret,
                Name=name,
                SecretBinary=json.dumps({}),
                Description="Secrets for Janssen cluster",
                Tags=[{"Key": "multipart_enabled", "Value": "true"}],
            )

            if self.replica_regions:
                # if there's replica regions, pass `AddReplicaRegions` argument;
                # this will create replica in the specified regions, but note that
                # a replica secret can't be updated independently from its primary secret,
                # except for its encryption key.
                create_secret.keywords["AddReplicaRegions"] = self.replica_regions
                create_secret.keywords["ForceOverwriteReplicaSecret"] = True

            # run the actual secrets creation
            create_secret()

            logger.info(f"Created secret: {name}")

            # mark the secret as exists so subsequent checks made by
            # client instance won't need to make requests to AWS service
            self.multiparts.append(name)

        except NoCredentialsError:
            raise RuntimeError(
                "AWS credentials are not specified. Please specify the credentials "
                "(contains AWS access key ID and secret access key) in a file pointed "
                "by AWS_SHARED_CREDENTIALS_FILE environment variable, or specify profile "
                "name via AWS_PROFILE environment variable."
            )
        return name
