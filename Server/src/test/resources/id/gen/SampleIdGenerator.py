from org.xdi.oxauth.idgen.ws.rs import IdGenerator
from java.util import UUID

class PythonExternalIdGenerator(IdGenerator):

    def generateId(self, idType, idPrefix):
        id = UUID.randomUUID().toString()
        print "new id: " + id
        return id