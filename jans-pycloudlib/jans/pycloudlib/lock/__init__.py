# noqa: D104
from __future__ import annotations
import json
import logging
import os
import random
import socket
import threading
import time
import typing as _t
from datetime import datetime
from datetime import timedelta
from functools import cached_property

from jans.pycloudlib.base import BaseStorage
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


class LockStorage(BaseStorage):
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

    def delete(self, key: str) -> bool:
        """Delete specific lock.

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
        retry_delay: float = 5.0,
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
            retry_delay: Delay before retrying to acquire lock (in seconds).
            max_start_delay: Max. delay before starting to acquire lock.

        Returns:
            Instance of `jans.pycloudlib.lock.Lock`.
        """
        owner = owner or socket.gethostname()
        return Lock(name, owner, ttl, retry_delay, max_start_delay, lock_storage=self)


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
        retry_delay: float = 5.0,
        max_start_delay: float = 0.0,
        lock_storage: "LockStorage" | None = None,
    ):
        # storage to set/get/delete lock
        self.lock_storage = lock_storage or LockStorage()

        # name of the lock
        self.name = name

        # delay to retry acquiring lock (in seconds)
        self.retry_delay = retry_delay

        # candidate to acquire a lock
        self.candidate = {"owner": owner, "ttl": ttl, "updated_at": ""}

        # maximum delay before acquiring lock (in seconds)
        self.max_start_delay = max_start_delay

        # thread object to renew lock
        self._renew_thread = None

        # event object to stop renew lock
        self._renew_stop_event = None

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

    def _get_start_delay(self):
        # random jitter
        return random.uniform(0, self.max_start_delay)  # nosec: B311

    def _owned_by_candidate(self, record: dict[str, _t.Any]) -> bool:
        return self.candidate["owner"] == record["owner"]

    def _renew_loop(self):
        logger.info(f"Starting lock {self.name} update")
        time.sleep(self._get_start_delay())

        while True:
            if self._renew_stop_event.isSet():
                logger.info(f"Stopping lock {self.name} update")
                break

            if self._set_record():
                logger.info(f"Lock {self.name} owned by candidate {self.candidate['owner']} has been updated")
            time.sleep(self.retry_delay)

    def _start_renew_loop(self):
        self._renew_stop_event = threading.Event()
        self._renew_thread = threading.Thread(target=self._renew_loop, daemon=False)
        self._renew_thread.start()

    def _stop_renew_loop(self):
        self._renew_stop_event.set()
        self._renew_thread.join()
        self._renew_thread = None

    def acquire(self) -> bool:
        """Acquire a lock.

        Returns:
            Boolean to indicate if lock is acquired.
        """
        # add random delay (if any) before starting to acquire a lock
        logger.info(f"Trying to acquire lock {self.name}")
        time.sleep(self._get_start_delay())

        while True:
            # check if lock exists
            record = self._get_record()

            # the most simple scenario (when lock is not found) is to create new lock
            if not record:
                if self._set_record():
                    # lock created; mark it as acquired to allow candidate to proceed
                    logger.info(f"Lock {self.name} is created by candidate {self.candidate['owner']}")
                    self._start_renew_loop()
                    return True

                # lock not created; mark it as not acquired to allow other candidates to create lock
                return False

            # at this point, we found an existing lock; note that a lock maybe expired
            # (owner doesn't update or delete it properly), hence we will try to take over
            if self._record_expired(record) and self._set_record():
                logger.info(f"Lock {self.name} is expired hence taken over by candidate {self.candidate['owner']}")
                self._start_renew_loop()
                return True

            # known states of why lock couldn't be acquired
            #
            # 1. lock is not expired yet
            # 2. lock is still used by the owner
            # 3. candidate is the owner of lock and it has been acquired
            logger.warning(f"Unable to acquire lock {self.name}; retrying in {self.retry_delay} seconds")

            # delay before retrying to acquire
            time.sleep(self.retry_delay)

        # mark as not acquired
        return False

    def release(self) -> None:
        """Release a lock.

        Lock is released only if the record exists and owned by candidate.
        """
        self._stop_renew_loop()

        record = self._get_record()

        # only allow deletion if lock is owned by the candidate
        if record and self._owned_by_candidate(record):
            self._delete_record()
            logger.info(f"Lock {self.name} is released by candidate {self.candidate['owner']}")


# avoid implicit reexport disabled error
__all__ = [
    "ConsulLock",
    "KubernetesLock",
    "GoogleLock",
    "AwsLock",
    "LockStorage",
]
