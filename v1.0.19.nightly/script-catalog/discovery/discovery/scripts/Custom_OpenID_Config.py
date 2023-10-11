# Copyright (c) 2022, Janssen Project
#
# Author: Gluu
#    1. Filter out a value
#    2. Add a value
#    3. Get ip address of the client making the request
#
from io.jans.model.custom.script.type.discovery import DiscoveryType
from java.lang import String

class Discovery(DiscoveryType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Custom Discovery script. Initializing ..."
        print "Custom Discovery script. Initialized successfully"

        return True

    def destroy(self, configurationAttributes):
        print "Custom Discovery script. Destroying ..."
        print "Custom Discovery script. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 11

    # Returns boolean, true - apply Discovery method, false - ignore it.
    # This method is called after Discovery response is ready. This method can modify Discovery response.
    # Note :
    # responseAsJsonObject - is org.json.JSONObject, you can use any method to manipulate json
    # context is reference of io.jans.as.server.model.common.ExecutionContext (in https://github.com/JanssenProject project, )
    def modifyResponse(self, responseAsJsonObject, context):

        print "Custom - Inside modifyResponse method of Discovery script ...."
        
        # Add a value to Discovery Response
        responseAsJsonObject.accumulate("key_from_script", "value_from_script")

        # Filter out a value from Discovery Response
        responseAsJsonObject.remove("pushed_authorization_request_endpoint")

        # Get an IP Address of the Client making the request
        responseAsJsonObject.accumulate("Client IP Address", context.getHttpRequest().getHeader("X-Forwarded-For"))
        
        return True
