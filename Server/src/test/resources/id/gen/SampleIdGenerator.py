from org.gluu.model.custom.script.type.id import IdGeneratorType
from org.gluu.util import StringHelper, ArrayHelper
from java.util import Arrays, ArrayList
from java.util import UUID

import java

class IdGenerator(IdGeneratorType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, configurationAttributes):
        print "Id generator. Initialization"
        print "Id generator. Initialized successfully"

        return True   

    def destroy(self, configurationAttributes):
        print "Id generator. Destroy"
        print "Id generator. Destroyed successfully"
        return True   

    def getApiVersion(self):
        return 1

    # Id generator init method
    #   appId is application Id
    #   idType is Id Type
    #   idPrefix is Id Prefix
    #   user is org.gluu.oxtrust.model.GluuCustomPerson
    #   configurationAttributes is java.util.Map<String, SimpleCustomProperty>
    def generateId(self, appId, idType, idPrefix, configurationAttributes):
        print "Id generator. Generate Id"
        print "Id generator. Generate Id. AppId: '", appId, "', IdType: '", idType, "', IdPrefix: '", idPrefix, "'"

        if StringHelper.equalsIgnoreCase(idType, "test"):
            id = UUID.randomUUID().toString()
            print "Id generator. New test id: " + id
            return id

        return "invalid"
