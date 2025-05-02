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
from datetime import datetime
from datetime import timedelta

import backoff

from jans.pycloudlib.lock.sql_lock import SqlLock
from jans.pycloudlib.utils import as_boolean
from jans.pycloudlib.persistence.utils import PersistenceMapper

if _t.TYPE_CHECKING:  # pragma: no cover
    # imported objects for function type hint, completion, etc.
    # these won't be executed in runtime
    from backoff.types import Details
    from jans.pycloudlib.manager import Manager

logger = logging.getLogger(__name__)

_DATETIME_FMT = "%Y-%m-%dT%H:%M:%S.%fZ"

LockAdapter = _t.Union[SqlLock]
"""Lock adapter type.

Currently supports the following classes:

* [SqlLock][jans.pycloudlib.lock.sql_lock.SqlLock]
"""


def _on_connection_backoff(details: Details) -> None:
    """Handle connection backoff.

    Args:
        details: backoff Details type.
    """
    lock = details["args"][0].__class__.__name__
    adapter = details["args"][0].adapter.__class__.__name__
    func = details["target"].__name__

    exc_type, exc, _ = sys.exc_info()
    if exc is not None:
        error = traceback.format_exception_only(exc_type, exc)[-1].rstrip()
    else:
        error = details.get("value", "Uncaught exception")

    # emit warning
    logger.warning(f"Backing off {lock}(adapter={adapter}).{func}() for {details['wait']:.1f} seconds; {error=}")


def _on_connection_giveup(details: Details) -> None:
    """Handle connection giveup.

    Args:
        details: backoff Details type.
    """
    lock = details["args"][0].__class__.__name__
    adapter = details["args"][0].adapter.__class__.__name__
    func = details["target"].__name__

    exc_type, exc, _ = sys.exc_info()
    if exc is not None:
        error = traceback.format_exception_only(exc_type, exc)[-1].rstrip()
    else:
        error = details.get("value", "Uncaught exception")

    # emit warning
    logger.warning(f"Giving up {lock}(adapter={adapter}).{func}() after {details['tries']} tries within {details['elapsed']:.1f} seconds; {error=}")


class LockNotAcquired(RuntimeError):
    """Error class to indicate failure on acquiring a lock."""


class LockConnectionError(RuntimeError):
    """Error class to indicate failure on acquiring connection."""


class LockRecord:
    """This class manage the locking process.

    Common example:

    ```py
    from jans.pycloudlib import get_manager

    manager = get_manager()

    lock = LockRecord("lock-1", owner="container-1", ttl=30)
    lock.init_adapter(manager)

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

        # lock adapter
        self.adapter = None

    def __enter__(self):
        if not self.acquire():
            raise LockNotAcquired(f"Lock {self.name} is not acquired within the specified time")
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.release()

    def init_adapter(self, manager: Manager):
        _adapter = os.environ.get("CN_OCI_LOCK_ADAPTER") or PersistenceMapper().mapping["default"]

        if _adapter == "sql":
            self.adapter = SqlLock(manager)
        else:
            # unsupported adapter
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
            A `dict` contains lock record (if any).
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

    def __repr__(self):
        return f"<{self.__class__.__name__}(adapter={self.adapter.__class__.__name__})>"

    @backoff.on_exception(
        backoff.constant,
        Exception,
        on_backoff=_on_connection_backoff,
        on_giveup=_on_connection_giveup,
        max_time=60,
        jitter=None,
        interval=10,
    )
    def check_adapter_connection(self) -> None:
        """Check if connection to adapter is established.

        Returns:
            A boolean to indicate connection is established.
        """
        if not self.adapter.connected():
            raise LockConnectionError(f"Cannot establish connection using adapter {self.adapter.__class__.__name__}.")


# avoid implicit reexport disabled error
__all__ = [
    "LockRecord",
]
