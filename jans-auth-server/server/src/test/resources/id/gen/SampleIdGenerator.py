from __future__ import print_function

from io.jans.model.custom.script.type.id import IdGeneratorType
from io.jans.util import StringHelper
from java.util import UUID

class IdGenerator(IdGeneratorType):

    def __init__(self, currentTimeMillis):
        """Construct class.

        Args:
            currentTimeMillis (int): current time in miliseconds
        """
        self.currentTimeMillis = currentTimeMillis

    @classmethod
    def init(cls, configurationAttributes):
        print("Id generator. Initialization")
        print("Id generator. Initialized successfully")

        return True

    @classmethod
    def destroy(cls, configurationAttributes):
        print("Id generator. Destroy")
        print("Id generator. Destroyed successfully")
        return True

    @classmethod
    def getApiVersion(cls):
        return 1

    # Id generator init method
    #   appId is application Id
    #   idType is Id Type
    #   idPrefix is Id Prefix
    #   user is org.gluu.oxtrust.model.GluuCustomPerson
    #   configurationAttributes is java.util.Map<String, SimpleCustomProperty>
    @classmethod
    def generateId(cls, appId, idType, idPrefix, configurationAttributes):
        print("Id generator. Generate Id")
        print("Id generator. Generate Id. AppId: '", appId, "', IdType: '", idType, "', IdPrefix: '", idPrefix, "'")

        if StringHelper.equalsIgnoreCase(idType, "test"):
            newId = UUID.randomUUID().toString()
            print("Id generator. New test id: " + newId)
            return newId

        return "invalid"
