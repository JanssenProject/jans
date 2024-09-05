import json
import logging
import os
import typing as _t
from functools import cached_property

import requests
import urllib3

from jans.pycloudlib.lock.base_lock import BaseLock
from jans.pycloudlib.utils import as_boolean
from jans.pycloudlib.utils import get_password_from_file

logger = logging.getLogger(__name__)


def _handle_failed_request(resp) -> None:
    match resp.status_code:
        case 404:
            logger.warning(f"Cannot send request to {resp.url}; status_code={resp.status_code}")
        case 400:
            err = resp.json()["errors"]
            logger.warning(f"Error while sending request to {resp.url}; status_code={resp.status_code}, reason={err}")
        case _:
            resp.raise_for_status()


class CouchbaseLock(BaseLock):
    def __init__(self):
        self.bucket_exists = False

        self.host = os.environ.get("CN_COUCHBASE_URL", "localhost")

        prefix = os.environ.get("CN_COUCHBASE_BUCKET_PREFIX", "jans")
        self.bucket = f"{prefix}_oci_lock"

    @cached_property
    def n1ql_port(self):
        if as_boolean(os.environ.get("CN_COUCHBASE_TRUSTSTORE_ENABLE", True)):
            return 18093
        return 8093

    @cached_property
    def rest_port(self):
        if as_boolean(os.environ.get("CN_COUCHBASE_TRUSTSTORE_ENABLE", True)):
            return 18091
        return 8091

    @cached_property
    def scheme(self):
        if as_boolean(os.environ.get("CN_COUCHBASE_TRUSTSTORE_ENABLE", True)):
            return "https"
        return "http"

    @cached_property
    def session(self):
        # suppress warning
        if as_boolean(os.environ.get("CN_COUCHBASE_SUPPRESS_VERIFICATION", True)):
            urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

        sess = requests.Session()
        sess.verify = False
        sess.auth = self._resolve_auth()

        # return the cached session
        return sess

    @property
    def base_n1ql_url(self):
        return f"{self.scheme}://{self.host}:{self.n1ql_port}/query/service"

    @property
    def base_rest_url(self):
        return f"{self.scheme}://{self.host}:{self.rest_port}"

    def get(self, key: str) -> dict[str, _t.Any]:
        stmt = " ".join([
            "SELECT META().id, doc.*",
            f"FROM {self.bucket} AS doc",
            f"USE KEYS {key!r} LIMIT 1",
        ])

        resp = self.session.post(self.base_n1ql_url, data={"statement": stmt})

        if not resp.ok:
            _handle_failed_request(resp)
            return {}

        data = resp.json()

        try:
            entry = data["results"][0]
            return json.loads(entry["jansData"]) | {"name": entry["id"]}
        except IndexError:
            return {}

    def post(self, key: str, owner: str, ttl: float, updated_at: str) -> bool:
        data = {"jansData": json.dumps({"owner": owner, "ttl": ttl, "updated_at": updated_at})}

        stmt = " ".join([
            f"INSERT INTO {self.bucket} (KEY, VALUE)",
            f"VALUES ({key!r}, {data!r})",
            "RETURNING META().id AS name",
        ])

        resp = self.session.post(self.base_n1ql_url, data={"statement": stmt})

        if not resp.ok:
            _handle_failed_request(resp)
            return False
        return bool(resp.json()["results"])

    def put(self, key: str, owner: str, ttl: float, updated_at: str) -> bool:
        data = json.dumps({"owner": owner, "ttl": ttl, "updated_at": updated_at})

        stmt = " ".join([
            f"UPDATE {self.bucket} USE KEYS {key!r}",
            f"SET jansData = {data!r}",
            "RETURNING META().id AS name",
        ])

        resp = self.session.post(self.base_n1ql_url, data={"statement": stmt})

        if not resp.ok:
            _handle_failed_request(resp)
            return False
        return bool(resp.json()["results"])

    def delete(self, key: str) -> bool:
        stmt = f"DELETE FROM {self.bucket} USE KEYS {key!r} RETURNING META().id AS name"  # nosec: B608

        resp = self.session.post(self.base_n1ql_url, data={"statement": stmt})

        if not resp.ok:
            _handle_failed_request(resp)
            return False
        return bool(resp.json()["results"])

    def connected(self) -> bool:
        """Check if connection is established.

        Returns:
            A boolean to indicate connection is established.
        """
        self._prepare_bucket()

        resp = self.session.get(f"{self.base_rest_url}/pools/default/buckets/{self.bucket}")

        if not resp.ok:
            _handle_failed_request(resp)
            return False

        # bucket exists
        return True

    def _prepare_bucket(self):
        if self.bucket_exists:
            return

        resp = self.session.get(f"{self.base_rest_url}/pools/default/buckets/{self.bucket}")

        if resp.ok:
            self.bucket_exists = True
            return

        # create missing bucket (if possible)
        resp = self.session.post(
            f"{self.base_rest_url}/pools/default/buckets",
            data={
                "name": self.bucket,
                "bucketType": "couchbase",
                "ramQuotaMB": 128,
                "authType": "sasl",
            },
        )

        match resp.status_code:
            case 404:
                logger.warning(f"The requested {resp.url} is not found; status_code={resp.status_code}")
            case 400 | 202:
                # either bucket already exist or created
                self.bucket_exists = True
            case 403:
                logger.warning(f"Unable to create required bucket {self.bucket}; status_code={resp.status_code}")
            case _:
                resp.raise_for_status()

    def _resolve_auth(self):
        superuser_password_file = os.environ.get("CN_COUCHBASE_SUPERUSER_PASSWORD_FILE", "/etc/jans/conf/couchbase_superuser_password")
        password_file = os.environ.get("CN_COUCHBASE_PASSWORD_FILE", "/etc/jans/conf/couchbase_password")

        if os.path.isfile(superuser_password_file):
            user = os.environ.get("CN_COUCHBASE_SUPERUSER", "admin")
            password = get_password_from_file(superuser_password_file)
        elif os.path.isfile(password_file):
            user = os.environ.get("CN_COUCHBASE_USER", "jans")
            password = get_password_from_file(password_file)
        else:
            user = ""
            password = ""  # nosec: B105

        # auth credentials
        return user, password
