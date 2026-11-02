# Copyright (c) 2022, Janssen Project
#
# Author: Gluu
#    1. Modifying Search Results
#    2. Segmenting the user base
#    3. Allow/Deny resource operations
#    4. Allow/Deny searches
#
from io.jans.model.custom.script.type.scim import ScimType
from io.jans.scim.ws.rs.scim2 import BaseScimWebService

import json

class ScimEventHandler(ScimType):

    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, configurationAttributes):
        self.custom_header = configurationAttributes.get("custom_header").getValue2()
        access_map_json = configurationAttributes.get("access_map").getValue2()    
        self.access_map = json.loads(access_map_json)
        print "Custom ScimEventHandler (init): Initialized successfully"
        return True

    def destroy(self, configurationAttributes):
        print "Custom ScimEventHandler (destroy): Destroyed successfully"
        return True   

    def getApiVersion(self):
        return 5

    def createUser(self, user, configurationAttributes):
        return True

    def updateUser(self, user, configurationAttributes):
        return True

    def deleteUser(self, user, configurationAttributes):
        return True

    def createGroup(self, group, configurationAttributes):
        return True

    def updateGroup(self, group, configurationAttributes):
        return True

    def deleteGroup(self, group, configurationAttributes):
        return True
        
    def postCreateUser(self, user, configurationAttributes):
        return True

    def postUpdateUser(self, user, configurationAttributes):
        return True

    def postDeleteUser(self, user, configurationAttributes):
        return True

    def postUpdateGroup(self, group, configurationAttributes):
        return True

    def postCreateGroup(self, group, configurationAttributes):
        return True

    def postDeleteGroup(self, group, configurationAttributes):
        return True
    
    def getUser(self, user, configurationAttributes):
        return True
    
    def getGroup(self, group, configurationAttributes):
        return True
        
    def postSearchUsers(self, results, configurationAttributes):
        
        print "%d entries returned of %d" % (results.getEntriesCount(), results.getTotalEntriesCount())
        for user in results.getEntries():
            print "Flushing addresses for user %s" % ("%s" % user.getUid()).replace("\r", "_").replace("\n", "_")
            user.setAttribute("jansAddress", None)
        
        return True

    def postSearchGroups(self, results, configurationAttributes):
        return True

    def manageResourceOperation(self, context, entity, payload, configurationAttributes):
        print "manageResourceOperation. SCIM endpoint invoked is %s (HTTP %s)" % (context.getPath(), context.getMethod()) 
        if context.getResourceType() != "User":
            return None

        expected_user_type = self.getUserType(context.getRequestHeaders())

        if (expected_user_type != None and entity.getAttribute("jansUsrTyp") == expected_user_type
                and self.payloadKeepsUserType(payload, expected_user_type)):
            return None
        else:
            return BaseScimWebService.getErrorResponse(403, None, "Attempt to handle a not allowed user type")

    # payload is a UserResource (POST/PUT), a PatchRequest (PATCH) or None
    def payloadKeepsUserType(self, payload, expected_user_type):
        if payload is None:
            return True

        if hasattr(payload, "getUserType"):
            user_type = payload.getUserType()
            return user_type is None or user_type == expected_user_type

        if hasattr(payload, "getOperations"):
            for operation in payload.getOperations():
                path = operation.getPath()
                value = operation.getValue()
                if path is None:
                    if hasattr(value, "keySet"):
                        for key in value.keySet():
                            if str(key).lower() == "usertype" and value.get(key) != expected_user_type:
                                return False
                elif "usertype" in path.lower():
                    if str(operation.getType()).lower() == "remove" or value != expected_user_type:
                        return False
        return True

    def manageSearchOperation(self, context, searchRequest, configurationAttributes):
        print "manageSearchOperation. SCIM endpoint invoked is %s (HTTP %s)" % (context.getPath(), context.getMethod())

        resource_type = context.getResourceType()
        print "manageSearchOperation. This is a search over %s resources" % resource_type

        if resource_type != "User":
            return None

        expected_user_type = self.getUserType(context.getRequestHeaders())

        if expected_user_type != None:
            context.setFilterPrepend("userType eq \"%s\"" % expected_user_type)
            return None
        else:
            return BaseScimWebService.getErrorResponse(403, None, "Attempt to handle a not allowed user type")

    # headers params is an instance of javax.ws.rs.core.MultivaluedMap<String, String>
    def getUserType(self, headers):
        secret = headers.getFirst(self.custom_header)
        if secret in self.access_map:
            return self.access_map[secret]
        else:
            return None
