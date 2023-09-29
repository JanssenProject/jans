# noqa: D104
from __future__ import annotations
import json
import logging
import os
import random
import socket
import time
import typing as _t
from datetime import datetime
from datetime import timedelta
from functools import cached_property

from jans.pycloudlib.lock.consul_lock import ConsulLock  # noqa: F401
from jans.pycloudlib.lock.kubernetes_lock import KubernetesLock  # noqa: F401
from jans.pycloudlib.lock.google_lock import GoogleLock  # noqa: F401
from jans.pycloudlib.lock.aws_lock import AwsLock  # noqa: F401

logger = logging.getLogger(__name__)

_DATETIME_FMT = "%Y-%m-%dT%H:%M:%S.%fZ"

LockAdapter = _t.Union[ConsulLock, KubernetesLock, GoogleLock, AwsLock]
"""Lock adapter type.

Currently supports the following classes:

* [ConsulLock][jans.pycloudlib.lock.consul_lock.ConsulLock]
* [KubernetesLock][jans.pycloudlib.lock.kubernetes_lock.KubernetesLock]
* [GoogleLock][jans.pycloudlib.lock.google_lock.GoogleLock]
* [AwsLock][jans.pycloudlib.lock.aws_lock.AwsLock]
"""


class LockStorage:
    @cached_property
    def adapter(self) -> LockAdapter:  # noqa: D412
        """Get an instance of lock adapter class.

        Returns:
            An instance of lock adapter class.

        Raises:
            ValueError: If the value of `CN_LOCK_ADAPTER` environment variable is not supported.

        Examples:

        ```py
        os.environ["CN_LOCK_ADAPTER"] = "consul"
        LockStorage().adapter  # returns an instance of adapter class
        ```

        The adapter name is pre-populated from `CN_LOCK_ADAPTER` environment variable.

        Supported lock adapter name:

        - `consul`: returns an instance of [ConsulLock][jans.pycloudlib.lock.consul_lock.ConsulLock]
        - `kubernetes`: returns an instance of [KubernetesLock][jans.pycloudlib.lock.kubernetes_lock.KubernetesLock]
        - `google`: returns an instance of [GoogleLock][jans.pycloudlib.lock.google_lock.GoogleLock]
        - `aws`: returns an instance of [AwsLock][jans.pycloudlib.lock.aws_lock.AwsLock]
        """
        adapter = os.environ.get("CN_LOCK_ADAPTER", "consul")

        if adapter == "consul":
            return ConsulLock()

        if adapter == "kubernetes":
            return KubernetesLock()

        if adapter == "google":
            return GoogleLock()

        if adapter == "aws":
            return AwsLock()
        raise ValueError(f"Unsupported lock adapter {adapter!r}")

    def get(self, key: str, default: _t.Any = "") -> _t.Any:
        """Get value based on given key.

        Args:
            key: Key name.
            default: Default value if key is not exist.

        Returns:
            Value based on given key or default one.
        """
        return self.adapter.get(key, default)

    def set(self, key: str, value: _t.Any) -> bool:
        """Set key with given value.

        Args:
            key: Key name.
            value: Value of the key.

        Returns:
            A boolean to mark whether configuration is set or not.
        """
        return self.adapter.set(key, value)

    def all(self) -> dict[str, _t.Any]:  # noqa: A003
        """Get all key-value pairs (deprecated in favor of [get_all][jans.pycloudlib.manager.BaseConfiguration.get_all]).

        Returns:
            A mapping of configuration.
        """
        return self.get_all()

    def get_all(self) -> dict[str, _t.Any]:
        """Get all configuration.

        Returns:
            A mapping of configuration (if any).
        """
        return self.adapter.get_all()

    def set_all(self, data: dict[str, _t.Any]) -> bool:
        """Set all key-value pairs.

        Args:
            data: Key-value pairs.

        Returns:
            A boolean to mark whether configuration is set or not.
        """
        return self.adapter.set_all(data)

    def delete(self, key: str) -> bool:
        """Delete specific lock.

        !!! important
            Subclass **MUST** implement this method.

        Args:
            key: Key name.

        Returns:
            A boolean to mark whether lock is removed or not.
        """
        return self.adapter.delete(key)

    def create_lock(
        self,
        name: str,
        owner: str = "",
        ttl: int = 10,
        lock_storage: "LockStorage" | None = None,
        retry_delay: float = 2.0,
        max_start_delay: float = 0.0,
    ):
        """Create lock object.

        Example:

        ```py
        from jans.pycloudlib.lock import LockStorage

        # automally try to acquire the lock and then release it at the end of operation
        with LockStorage().create_lock("lock-1") as lock:
            # do operation which requires coordination
        ```

        Args:
            name: Name of the lock.
            owner: Owner of the lock.
            ttl: Duration of lock (in seconds).
            lock_storage: An instance of `jans.pycloudlib.lock.LockStorage` or `None` type.

        Returns:
            Instance of `jans.pycloudlib.lock.Lock`.
        """
        owner = owner or socket.gethostname()
        lock_storage = lock_storage or self
        return Lock(name, owner, ttl, lock_storage, retry_delay, max_start_delay)


class LockNotAcquired(RuntimeError):
    """Error class to indicate failure on acquiring a lock."""


class Lock:
    """This class manage the locking process.

    Common example:

    ```py
    from jans.pycloudlib.lock import LockStorage

    lock_storage = LockStorage()

    # automally try to acquire the lock and then release it at the end of operation
    with Lock("lock-1", lock_storage=lock_storage) as lock:
        # do operation which requires coordination
        ...
    ```

    Or using low-level API:

    ```py
    from jans.pycloudlib.lock import LockStorage

    lock_storage = LockStorage()
    lock = Lock("lock-1", owner="container-1", ttl=30, lock_storage=lock_storage)

    # try to acquire the lock
    if lock.acquire():
        # do operation which requires coordination

        # release the lock
        lock.release()
    ```
    """
    def __init__(
        self,
        name: str,
        owner: str,
        ttl: int,
        lock_storage: LockStorage,
        retry_delay: float = 2.0,
        max_start_delay: float = 0.0
    ):
        # storage to set/get/delete lock
        self.lock_storage = lock_storage

        # name of the lock
        self.name = name

        # delay to retry acquiring lock (in seconds)
        self.retry_delay = retry_delay

        # candidate to acquire a lock
        self.candidate = {"owner": owner, "ttl": ttl, "updated_at": ""}

        # maximum delay before acquiring lock (in seconds)
        self.max_start_delay = max_start_delay

    def __enter__(self):
        if not self.acquire():
            raise LockNotAcquired(f"Lock {self.name} is not acquired within the specified time")
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.release()

    def _record_expired(self, record: dict[str, _t.Any]) -> bool:
        """Check if a record is expire.

        Args:
            record: A `dict` contains lock record.

        Returns:
            Boolean to indicate whether lock record is expired.
        """
        updated_at = datetime.strptime(record["updated_at"], _DATETIME_FMT)
        expire = updated_at + timedelta(seconds=int(record["ttl"]))
        return datetime.utcnow() > expire

    def _set_record(self) -> bool:
        """Create/update a lock record.

        Returns:
            Boolean to indicate whether lock is saved into storage.
        """
        self.candidate["updated_at"] = datetime.utcnow().strftime(_DATETIME_FMT)

        # create/update record with candidate data
        return self.lock_storage.set(
            self.name,
            {
                "owner": self.candidate["owner"],
                "ttl": self.candidate["ttl"],
                "updated_at": self.candidate["updated_at"],
            },
        )

    def _get_record(self) -> dict[str, _t.Any]:
        """Get exising lock record.

        Returns:
            A `dict` contains of lock record (if any).
        """
        record = self.lock_storage.get(self.name)
        if record:
            return json.loads(record)
        return {}

    def _delete_record(self) -> bool:
        """Delete a lock record.

        Returns:
            Boolean to indicate whether lock record is deleted from storage.
        """
        return self.lock_storage.delete(self.name)

    def _get_jitter(self):
        # random jitter
        return random.uniform(0, self.max_start_delay)  # nosec: B311

    def _owned_by_candidate(self, record: dict[str, _t.Any]) -> bool:
        return self.candidate["owner"] == record["owner"]

    def acquire(self) -> bool:
        """Acquire a lock.

        Returns:
            Boolean to indicate if lock is acquired.
        """
        # add random delay before starting acquiring lock
        # this allow simulating non-blocking access to lock
        # TODO: use thread?
        jitter = self._get_jitter()
        logger.info(f"Trying to acquire lock {self.name}; {jitter=}")
        time.sleep(jitter)  # nosec: B311

        while True:
            # check if lock exists
            record = self._get_record()

            # lock not found, create new one
            if not record and self._set_record():
                logger.info(f"Lock {self.name} is created and owned by {self.candidate['owner']}")
                return True

            # logger.info(f"{record=}")

            # lock exists, check if candidate holds the lock
            # if self.candidate["owner"] == record["owner"]:
            if self._owned_by_candidate(record):
                # logger.warning(f"Lock {self.name} exists and owned by the same owner {self.candidate['owner']}")
                # likely the process needs more time
                self._set_record()
                # logger.info(f"updated_record={self._get_record()}")
                time.sleep(self.retry_delay)
                continue

            if self._record_expired(record):
                # logger.info(f"Lock {self.name} is expired hence taken over by {self.candidate['owner']}")
                return True

            # logger.warning(f"Lock {self.name} is still owned by {record['owner']} and not expired yet")
            time.sleep(self.retry_delay)

        # mark as acquired
        return True

    def release(self) -> None:
        """Release a lock.

        Lock is released only if the record exists and owned by candidate.
        """
        record = self._get_record()

        # only allow deletion if lock is owned by the candidate
        # if record and self.candidate["owner"] == record["owner"]:
        if record and self._owned_by_candidate(record):
            self._delete_record()
            logger.info(f"Lock {self.name} is released by {self.candidate['owner']}")


# avoid implicit reexport disabled error
__all__ = [
    "LockStorage",
    "ConsulLock",
    "KubernetesLock",
    "GoogleLock",
    "AwsLock",
]
