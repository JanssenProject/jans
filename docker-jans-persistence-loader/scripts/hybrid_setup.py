from jans.pycloudlib.persistence.utils import PersistenceMapper

from ldap_setup import LDAPBackend
from couchbase_setup import CouchbaseBackend
from sql_setup import SQLBackend
from spanner_setup import SpannerBackend


_backend_classes = {
    "ldap": LDAPBackend,
    "couchbase": CouchbaseBackend,
    "sql": SQLBackend,
    "spanner": SpannerBackend,
}


class HybridBackend:
    def __init__(self, manager):
        mapper = PersistenceMapper()
        self.backends = [
            _backend_classes[type_] for type_ in mapper.groups()
        ]
        self.manager = manager

    def initialize(self):
        for backend in self.backends:
            backend(self.manager).initialize()
