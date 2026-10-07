# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2016, Janssen
#
# Author: Yuriy Movchan
#

from io.jans.model.custom.script.type.scope import DynamicScopeType
from io.jans.service.cdi.util import CdiUtil
from io.jans.as.server.service import SessionIdService
from io.jans.as.server.service import UserService
from io.jans.util import StringHelper, ArrayHelper
from java.util import Arrays, ArrayList

import java

class DynamicScope(DynamicScopeType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Session dynamic scope. Initialization"

        print "Session dynamic scope. Initialized successfully"

        return True   

    def destroy(self, configurationAttributes):
        print "Session dynamic scope. Destroy"
        print "Session dynamic scope. Destroyed successfully"
        return True   

    # Update Json Web token before signing/encrypring it
    #   dynamicScopeContext is io.jans.as.service.external.context.DynamicScopeExternalContext
    #   configurationAttributes is java.util.Map<String, SimpleCustomProperty>
    def update(self, dynamicScopeContext, configurationAttributes):
        print "Session dynamic scope. Update method"

        authorizationGrant = dynamicScopeContext.getAuthorizationGrant()

        if authorizationGrant is None:
            print "Introspection. Failed to load authorization grant by token"
            return False

        # Get session from token
        sessionDn = authorizationGrant.getSessionDn()
        if sessionDn is None:
            # There is no session
            return False

        sessionIdService = CdiUtil.bean(SessionIdService)
        session = sessionIdService.getSessionById(sessionDn)
        if session is None:
            print "Introspection. Failed to load session '%s'" % sessionDn
            return False

        sessionAttributes = session.getSessionAttributes()
        if sessionAttributes is None:
            # There is no session attributes
            return False

        # Return external_session_id
        externalSessionId = sessionAttributes.get("external_session_id")
        if externalSessionId != None:
            print "Introspection. Adding new claim 'external_session_id'  with value '%s'" % externalSessionId
            jsonWebResponse = dynamicScopeContext.getJsonWebResponse()
            claims = jsonWebResponse.getClaims()
            claims.setClaim("external_session_id", externalSessionId)

        return True

    def getSupportedClaims(self, configurationAttributes):
        return Arrays.asList("external_session_id")

    def getApiVersion(self):
        return 11
