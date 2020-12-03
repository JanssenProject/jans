# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2019, Janssen
#
# Author: Yuriy Mochan
#
#

from io.jans.model.custom.script.type.introspection import IntrospectionType
from io.jans.as.model.common import AuthorizationGrantList
from io.jans.as.server.service import SessionIdService
from io.jans.service.cdi.util import CdiUtil
from java.lang import String

class Introspection(IntrospectionType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Introspection script. Initializing ..."
        print "Introspection script. Initialized successfully"

        return True

    def destroy(self, configurationAttributes):
        print "Introspection script. Destroying ..."
        print "Introspection script. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 11

    # Returns boolean, true - apply introspection method, false - ignore it.
    # This method is called after introspection response is ready. This method can modify introspection response.
    # Note :
    # responseAsJsonObject - is org.codehaus.jettison.json.JSONObject, you can use any method to manipulate json
    # context is reference of io.jans.as.service.external.context.ExternalIntrospectionContext (in https://github.com/JanssenFederation/oxauth project, )
    def modifyResponse(self, responseAsJsonObject, context):
        token = context.getHttpRequest().getParameter("token")
        if token is None:
            print "Introspection. There is no token in request"
            return False

        authorizationGrantList = CdiUtil.bean(AuthorizationGrantList)
        authorizationGrant = authorizationGrantList.getAuthorizationGrantByAccessToken(token);
        if authorizationGrant is None:
            print "Introspection. Failed to load authorization grant by token"
            return False

        # Put user_id into response
        responseAsJsonObject.accumulate("user_id", authorizationGrant.getUser().getUserId())

        # Put custom parameters into response
        sessionDn = authorizationGrant.getSessionDn();
        if sessionDn is None:
            # There is no session
            return True

        sessionIdService = CdiUtil.bean(SessionIdService)
        session = sessionIdService.getSessionById(sessionDn)
        if sessionDn is None:
            print "Introspection. Failed to load session '%s'" % sessionDn
            return False

        # Return session_id
        responseAsJsonObject.accumulate("session_id", sessionDn)
        
        sessionAttributes = session.getSessionAttributes()
        if sessionAttributes is None:
            # There is no session attributes
            return True

        # Append custom claims
        if sessionAttributes.containsKey("custom1"):
            responseAsJsonObject.accumulate("custom1", sessionAttributes.get("custom1"))
        if sessionAttributes.containsKey("custom2"):
            responseAsJsonObject.accumulate("custom2", sessionAttributes.get("custom2"))

        return True

