# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2019, Janssen
#
#

from io.jans.model.custom.script.type.introspection import IntrospectionType
from io.jans.as.server.model.common import AuthorizationGrantList
from io.jans.as.server.service import SessionIdService
from io.jans.service.cdi.util import CdiUtil
from java.lang import String

class Introspection(IntrospectionType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Github. Introspection script. Initializing ..."
        print "Github. Introspection script. Initialized successfully"

        return True

    def destroy(self, configurationAttributes):
        print "Github. Introspection script. Destroying ..."
        print "Github. Introspection script. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 11

    # Returns boolean, true - apply introspection method, false - ignore it.
    # This method is called after introspection response is ready. This method can modify introspection response.
    # Note :
    # responseAsJsonObject - is org.codehaus.jettison.json.JSONObject, you can use any method to manipulate json
    # context is reference of io.jans.as.service.external.context.ExternalIntrospectionContext (in https://github.com/JanssenFederation/oxauth project, )
    def modifyResponse(self, responseAsJsonObject, context):
        print "Github. Checking for saved parameters in session ..."
        try:
            token = context.getHttpRequest().getParameter("token")
            if token is None:
                print "Github. Introspection. There is no token in request"
                return True

            authorizationGrantList = CdiUtil.bean(AuthorizationGrantList)
            authorizationGrant = authorizationGrantList.getAuthorizationGrantByAccessToken(token)
            if authorizationGrant is None:
                print "Github. Introspection. Failed to load authorization grant by token"
                return False

            # Put user_id into response
            responseAsJsonObject.accumulate("user_id", authorizationGrant.getUser().getUserId())

            # Put custom parameters into response
            sessionDn = authorizationGrant.getSessionDn();
            print "sessionDn '%s'" % sessionDn
            if sessionDn is None:
                print "There is no session"
                return True

            sessionIdService = CdiUtil.bean(SessionIdService)
            session = sessionIdService.getSessionByDn(sessionDn, False)
            if sessionDn is None:
                print "Github. Introspection. Failed to load session '%s'" % sessionDn
                return False

            # Return session_id
            responseAsJsonObject.accumulate("session_id", sessionDn)

            sessionAttributes = session.getSessionAttributes()
            if sessionAttributes is None:
                print "There is no session attributes"
                return True

            # Append custom claims
            customClaims = {}

            if sessionAttributes.containsKey("gihub_username"):
                customClaims["gihub_username"] = sessionAttributes.get("gihub_username")
            if sessionAttributes.containsKey("gihub_access_token"):
                customClaims["gihub_access_token"] = sessionAttributes.get("gihub_access_token")

            responseAsJsonObject.accumulate("customClaims", customClaims)
        except Exception as e:
                print "Exception occured. Unable to resolve role/scope mapping."
                print e

        return True

