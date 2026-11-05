# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2018, Janssen
# Author: Yuriy Zabrovarnyy, Arnab Dutta #

from io.jans.model.custom.script.type.introspection import IntrospectionType

class Introspection(IntrospectionType):
    def init(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Role based Scopes Introspection script. Initializing ..."
        print "Role based Scopes Introspection script. Initialized successfully"

        return True

    def destroy(self, configurationAttributes):
        print "Role based Scopes Introspection script. Destroying ..."
        print "Role based Scopes Introspection script. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 11

    # Returns boolean, true - apply introspection method, false - ignore it. Always returns true.
    # Called after the introspection response is ready.
    # Reads the "user_inum" and "user_role" token attributes from the context
    # (set when the access token was issued) and adds each one to the introspection
    # response as a claim of the same name. An attribute that is None is skipped.
    # Note :
    # responseAsJsonObject - is org.codehaus.jettison.json.JSONObject, modified in place
    # context is reference of io.jans.as.service.external.context.ExternalIntrospectionContext
    def modifyResponse(self, responseAsJsonObject, context):
        userInum = context.getTokenAttribute("user_inum")
        userRole = context.getTokenAttribute("user_role")
        if userInum is not None:
            responseAsJsonObject.accumulate("user_inum", userInum)
        if userRole is not None:
            responseAsJsonObject.accumulate("user_role", userRole)
        return True
