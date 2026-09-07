# Copyright (c) 2021, Gluu
#
# Author: Yuriy Z
#
#

from io.jans.model.custom.script.type.discovery import DiscoveryType

class Discovery(DiscoveryType):
    def __init__(self, millis):
        self.currentTimeMillis = millis

    def init(self, custom_script, configuration_attributes):
        return True

    def destroy(self, configuration_attributes):
        return True

    def getApiVersion(self):
        return 11

    # Returns boolean, true - indicates that script applied changes, false - no modification will be applied to response (script manipulation is ignored)
    # This method is called after discovery response is ready (and passed as `responseAsJsonObject` method parameter). Hence script can override them
    # Note :
    # responseAsJsonObject - is org.json.JSONObject, you can use any method to manipulate JSON
    # context is reference of io.jans.as.server.model.common.ExecutionContext (in https://github.com/JanssenProject/jans-auth-server project, )
    def modifyResponse(self, response, context):
        response.accumulate("key_from_script", "value_from_script")
        return True
