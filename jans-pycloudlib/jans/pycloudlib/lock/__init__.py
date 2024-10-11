# noqa: D104
from __future__ import annotations
import logging
import os
import random
import socket
import sys
import traceback
import threading
import time
import typing as _t
from abc import ABC
from abc import abstractmethod
from abc import abstractproperty
from datetime import datetime
from datetime import timedelta
from functools import cached_property

import backoff

from jans.pycloudlib.lock.couchbase_lock import CouchbaseLock
from jans.pycloudlib.lock.spanner_lock import SpannerLock
from jans.pycloudlib.lock.sql_lock import SqlLock
from jans.pycloudlib.lock.ldap_lock import LdapLock
from jans.pycloudlib.utils import as_boolean
from jans.pycloudlib.persistence.utils import PersistenceMapper

if _t.TYPE_CHECKING:  # pragma: no cover
    # imported objects for function type hint, completion, etc.
    # these won't be executed in runtime
    from backoff.types import Details

logger = logging.getLogger(__name__)

_DATETIME_FMT = "%Y-%m-%dT%H:%M:%S.%fZ"

LockAdapter = _t.Union[SqlLock, SpannerLock, CouchbaseLock, LdapLock]
"""Lock adapter type.

Currently supports the following classes:

* [SqlLock][jans.pycloudlib.lock.sql_lock.SqlLock]
* [SpannerLock][jans.pycloudlib.lock.spanner_lock.SpannerLock]
* [CouchbaseLock][jans.pycloudlib.lock.couchbase_lock.CouchbaseLock]
* [LdapLock][jans.pycloudlib.lock.ldap_lock.LdapLock]
"""


def _on_connection_backoff(details: Details) -> None:
    """Handle connection backoff.

    Args:
        details: backoff Details type.
    """
    mgr = details["args"][0].__class__.__name__
    lock = details["args"][1]
    func = details["target"].__name__

    exc_type, exc, _ = sys.exc_info()
    if exc is not None:
        error = traceback.format_exception_only(exc_type, exc)[-1].rstrip()
    else:
        error = details.get("value", "Uncaught exception")

    # emit warning
    logger.warning(f"Backing off {mgr}.{func}({lock}) for {details['wait']:.1f} seconds; {error=}")


def _on_connection_giveup(details: Details) -> None:
    """Handle connection giveup.

    Args:
        details: backoff Details type.
    """
    mgr = details["args"][0].__class__.__name__
    lock = details["args"][1]
    func = details["target"].__name__

    exc_type, exc, _ = sys.exc_info()
    if exc is not None:
        error = traceback.format_exception_only(exc_type, exc)[-1].rstrip()
    else:
        error = details.get("value", "Uncaught exception")

    # emit warning
    logger.warning(f"Giving up {mgr}.{func}({lock}) after {details['tries']} tries within {details['elapsed']:.1f} seconds; {error=}")


class LockManager:
    @property
    def lock_enabled(self):
        return as_boolean(os.environ.get("CN_OCI_LOCK_ENABLED", "true"))

    @backoff.on_exception(
        backoff.constant,
        Exception,
        on_backoff=_on_connection_backoff,
        on_giveup=_on_connection_giveup,
        max_time=60,
        jitter=None,
        interval=10,
    )
    def check_connection(self, lock) -> None:
        """Check if connection is established.

        Returns:
            A boolean to indicate connection is established.
        """
        if lock.adapter is None:
            return

        connected = lock.adapter.connected()
        if not connected:
            raise RuntimeError(f"Cannot establish connection using adapter {lock.adapter.__class__.__name__}.")

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
        from jans.pycloudlib.lock import LockManager

        # automally try to acquire the lock and then release it at the end of operation
        with LockManager().create_lock("lock-1") as lock:
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
        # default to hostname as owner
        owner = owner or socket.gethostname()

        # lock implementation is set to _fake_ for backward-compatibility
        lock_cls = FakeLockRecord

        if self.lock_enabled:
            lock_cls = LockRecord

        lock = lock_cls(name, owner, ttl, retry_delay, max_start_delay)

        # pre-flight connection checking
        self.check_connection(lock)
        return lock


class LockNotAcquired(RuntimeError):
    """Error class to indicate failure on acquiring a lock."""


class BaseLockRecord(ABC):
    def __init__(
        self,
        name: str,
        owner: str,
        ttl: int,
        retry_delay: float = 5.0,
        max_start_delay: float = 0.0,
    ):
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

    @abstractmethod
    def acquire(self) -> bool:
        pass

    @abstractmethod
    def release(self) -> None:
        pass

    @abstractproperty
    def adapter(self) -> _t.Optional[LockAdapter]:
        pass

    def __repr__(self):
        return f"<{self.__class__.__name__}(adapter={self.adapter.__class__.__name__})>"


class LockRecord(BaseLockRecord):
    """This class manage the locking process.

    Common example:

    ```py
    lock = Lock("lock-1", owner="container-1", ttl=30)

    # try to acquire the lock
    if lock.acquire():
        # do operation which requires coordination

        # release the lock
        lock.release()
    ```
    """

    @cached_property
    def adapter(self) -> LockAdapter:  # noqa: D412
        """Get an instance of lock adapter class.

        Returns:
            An instance of lock adapter class.

        Raises:
            ValueError: If the value of `CN_OCI_LOCK_ADAPTER` or `CN_PERSISTENCE_TYPE` environment variable is not supported.

        Examples:

        ```py
        os.environ["CN_OCI_LOCK_ADAPTER"] = "sql"
        LockStorage().adapter  # returns an instance of adapter class
        ```

        The adapter name is pre-populated from `CN_OCI_LOCK_ADAPTER` environment variable.

        Supported lock adapter name:

        - `sql`: returns an instance of [SqlLock][jans.pycloudlib.lock.sql_lock.SqlLock]
        - `spanner`: returns and instance of [SpannerLock][jans.pycloudlib.lock.spanner_lock.SpannerLock]
        - `couchbase`: returns and instance of [CouchbaseLock][jans.pycloudlib.lock.couchbase_lock.CouchbaseLock]
        - `ldap`: returns and instance of [LdapLock][jans.pycloudlib.lock.ldap_lock.LdapLock]
        """
        _adapter = os.environ.get("CN_OCI_LOCK_ADAPTER") or PersistenceMapper().mapping["default"]

        if _adapter == "sql":
            return SqlLock()

        if _adapter == "spanner":
            return SpannerLock()

        if _adapter == "couchbase":
            return CouchbaseLock()

        if _adapter == "ldap":
            return LdapLock()
        raise ValueError(f"Unsupported lock adapter {_adapter!r}")

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

    def _create_record(self) -> bool:
        """Create a lock record.

        Returns:
            Boolean to indicate whether lock is saved into storage.
        """
        self.candidate["updated_at"] = datetime.utcnow().strftime(_DATETIME_FMT)

        # create record with candidate data
        # return self.lock_storage.post(
        return self.adapter.post(
            self.name,
            owner=self.candidate["owner"],
            ttl=self.candidate["ttl"],
            updated_at=self.candidate["updated_at"],
        )

    def _update_record(self) -> bool:
        """Update a lock record.

        Returns:
            Boolean to indicate whether lock is saved into storage.
        """
        self.candidate["updated_at"] = datetime.utcnow().strftime(_DATETIME_FMT)

        # create record with candidate data
        return self.adapter.put(
            self.name,
            owner=self.candidate["owner"],
            ttl=self.candidate["ttl"],
            updated_at=self.candidate["updated_at"],
        )

    def _get_record(self) -> dict[str, _t.Any]:
        """Get exising lock record.

        Returns:
            A `dict` contains of lock record (if any).
        """
        return self.adapter.get(self.name)

    def _delete_record(self) -> bool:
        """Delete a lock record.

        Returns:
            Boolean to indicate whether lock record is deleted from storage.
        """
        return self.adapter.delete(self.name)

    def _get_start_delay(self):
        # random jitter
        return random.uniform(0, self.max_start_delay)  # nosec: B311

    def _owned_by_candidate(self, record: dict[str, _t.Any]) -> bool:
        return self.candidate["owner"] == record["owner"]

    def _renew_loop(self):
        logger.info(f"Starting lock {self.name} update")

        renew_delay = int(self.candidate["ttl"] * 2 / 3)

        while True:
            if self._renew_stop_event.isSet():
                logger.info(f"Stopping lock {self.name} update")
                break

            # delay before doing next update
            time.sleep(renew_delay)

            if self._update_record():
                logger.info(f"Lock {self.name} owned by candidate {self.candidate['owner']} has been updated")

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
        logger.info(f"Trying to acquire lock {self.name}")

        # add random delay (if any) before starting to acquire a lock
        time.sleep(self._get_start_delay())

        while True:
            # check if lock exists
            record = self._get_record()

            # the most simple scenario (when lock is not found) is to create new lock
            if not record:
                logger.warning(f"Lock {self.name} is not found")

                if self._create_record():
                    # lock created; mark it as acquired to allow candidate to proceed
                    logger.info(f"Lock {self.name} is created by candidate {self.candidate['owner']}")
                    self._start_renew_loop()
                    return True

                # lock not created; mark it as not acquired to allow other candidates to create lock
                return False

            logger.info(f"Found existing lock {self.name} owned by {record['owner']}")

            # at this point, we found an existing lock; note that a lock maybe expired
            # (owner doesn't update or delete it properly), hence we will try to take over
            if self._record_expired(record) and self._update_record():
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


class FakeLockRecord(BaseLockRecord):
    @property
    def adapter(self):
        return None

    def acquire(self) -> bool:
        logger.warning(f"Using {self.__class__.__name__}.acquire(...) to acquire lock {self.name}")
        return True

    def release(self) -> None:
        logger.warning(f"Using {self.__class__.__name__}.release(...) to release lock {self.name}")


# avoid implicit reexport disabled error
__all__ = [
    "LockManager",
    "SpannerLock",
    "SqlLock",
    "CouchbaseLock",
    "LdapLock",
]
