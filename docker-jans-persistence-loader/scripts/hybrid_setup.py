from ldap_setup import LDAPBackend
from couchbase_setup import CouchbaseBackend


class HybridBackend:
    def __init__(self, manager):
        self.ldap_backend = LDAPBackend(manager)
        self.couchbase_backend = CouchbaseBackend(manager)

    def initialize(self):
        self.ldap_backend.initialize()
        self.couchbase_backend.initialize()
