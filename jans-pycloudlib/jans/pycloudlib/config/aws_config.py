"""This module contains config adapter class to interact with AWS Secrets Manager."""

import json
import logging
import os
import typing as _t
from contextlib import suppress
from functools import cached_property
from functools import partial
from pathlib import Path

import boto3
from botocore.exceptions import ClientError
from botocore.exceptions import NoCredentialsError
from botocore.exceptions import NoRegionError

from jans.pycloudlib.config.base_config import BaseConfig
from jans.pycloudlib.utils import safe_value

logger = logging.getLogger(__name__)


def _dump_value(value: _t.Any) -> str:
    """Dump string from any Python data type.

    Args:
        value: Any given value.

    Returns:
        Compressed bytes contains the value.
    """
    return json.dumps(value)


def _load_value(value: str) -> _t.Any:
    """Load string into any Python data type.

    Args:
        value: Any given value

    Returns:
        Any Python data type.
    """
    return json.loads(value)


class AwsConfig(BaseConfig):
    """This class interacts with AWS Secrets Manager backend.

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

    def __init__(self) -> None:
        # unique name used as prefix to distinguish with other secrets
        # a typical usage is to use vendor/organization name
        prefix = os.environ.get("CN_AWS_SECRETS_PREFIX", "jans")

        # the secrets name will use `_` instead of `-` char to avoid clashing with generated suffix by AWS;
        # see https://docs.aws.amazon.com/cli/latest/reference/secretsmanager/create-secret.html#options
        self.basepath = f"{prefix}_configs"

        # flag to determine whether AWS secrets already created
        self.basepath_exists = False

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
            A mapping of configs (if any).
        """
        self._prepare_secret()
        resp = self.client.get_secret_value(SecretId=self.basepath)

        # SecretString is a `dict` data type
        data: dict[str, _t.Any] = _load_value(resp["SecretString"])
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
            A boolean to mark whether config is set or not.
        """
        data = self.get_all()
        data[key] = safe_value(value)

        resp = self.client.update_secret(
            SecretId=self.basepath,
            SecretString=_dump_value(data),
        )
        return bool(resp)

    def set_all(self, data: dict[str, _t.Any]) -> bool:
        """Set all key-value pairs.

        Args:
            data: key-value pairs of configs.

        Returns:
            A boolean indicating operation is succeed or not.
        """
        self._prepare_secret()

        # fetch existing data (if any) as we will merge them;
        # note that existing value will be overwritten
        payload = self.get_all()

        for k, v in data.items():
            # ensure value that has bytes is converted to text
            payload[k] = safe_value(v)

        resp = self.client.update_secret(
            SecretId=self.basepath,
            SecretString=_dump_value(payload),
        )
        return bool(resp)

    def _prepare_secret(self) -> None:
        """Prepare (create if missing) secrets with empty value."""
        # check whether secrets already exists
        if self.basepath_exists:
            return

        try:
            # get the secret
            self.client.get_secret_value(SecretId=self.basepath)

            # mark the secret as exists so subsequent checks made by
            # client instance won't need to make requests to AWS service
            self.basepath_exists = True

        except ClientError as exc:
            # raise exception if not related to missing secrets;
            # note that missing secrets will be created
            if exc.response["Error"]["Code"] != "ResourceNotFoundException":
                raise RuntimeError(f"Unable to access AWS Secrets Manager service; reason={exc}")

            create_secret = partial(
                self.client.create_secret,
                Name=self.basepath,
                SecretString=_dump_value({}),
                Description="Non-sensitive secrets (configs) for Janssen cluster",
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

            # mark the secrets as exists so subsequent checks made by
            # client instance won't need to make requests to AWS service
            self.basepath_exists = True

        except NoCredentialsError:
            raise RuntimeError(
                "AWS credentials are not specified. Please specify the credentials "
                "(contains AWS access key ID and secret access key) in a file pointed "
                "by AWS_SHARED_CREDENTIALS_FILE environment variable, or specify profile "
                "name via AWS_PROFILE environment variable."
            )

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
