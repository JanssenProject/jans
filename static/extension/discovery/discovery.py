# Copyright (c) 2021, Gluu
#
# Author: Yuriy Z
#
#

from io.jans.model.custom.script.type.discovery import DiscoveryType

class Discovery(DiscoveryType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Discovery script. Initializing ..."
        print "Discovery script. Initialized successfully"

        return True

    def destroy(self, configurationAttributes):
        print "Discovery script. Destroying ..."
        print "Discovery script. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 11

    # Returns boolean, true - indicates that script applied changes, false - no modification will be applied to response (script manipulation is ignored)
    # This method is called after discovery response is ready (and passed as `responseAsJsonObject` method parameter). Hence script can override them
    # Note :
    # responseAsJsonObject - is org.json.JSONObject, you can use any method to manipulate JSON
    # context is reference of io.jans.as.server.model.common.ExecutionContext (in https://github.com/JanssenProject/jans-auth-server project, )
    def modifyResponse(self, responseAsJsonObject, context):
        print "Modify discovery response: %s" % responseAsJsonObject

        responseAsJsonObject.accumulate("key_from_script", "value_from_script")

        print "Discovery response modified. Response after modification: %s" % responseAsJsonObject
        return True
