from jans.pycloudlib.persistence.utils import PersistenceMapper

from sql_setup import SQLBackend


_backend_classes = {
    "sql": SQLBackend,
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
