# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2021, Gluu
#
# Author: Yuriy Zabrovarnyy
#
#

from io.jans.model.custom.script.type.introspection import IntrospectionType
from java.lang import String
from io.jans.as.server.model.common import AuthorizationGrantList
from io.jans.service.cdi.util import CdiUtil
from io.jans.as.server.service import GrantService
from io.jans.as.model.common import TokenType
from io.jans.as.server.model.ldap import TokenEntity
from io.jans.as.server.service import SessionIdService

class Introspection(IntrospectionType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Introspection script (retain claims). Initializing ..."
        print "Introspection script (retain claims). Initialized successfully"

        return True

    def destroy(self, configurationAttributes):
        print "Introspection script (retain claims). Destroying ..."
        print "Introspection script (retain claims). Destroyed successfully"
        return True

    def getApiVersion(self):
        return 11

    # Returns boolean, true - apply introspection method, false - ignore it.
    # This method is called after introspection response is ready. This method can modify introspection response.
    # Note :
    # responseAsJsonObject - is org.codehaus.jettison.json.JSONObject, you can use any method to manipulate json
    # context is reference of org.gluu.oxauth.service.external.context.ExternalIntrospectionContext (in https://github.com/GluuFederation/oxauth project, )
    def modifyResponse(self, responseAsJsonObject, context):
        print "modifyResponse invoked"
        
        sessionIdService = CdiUtil.bean(SessionIdService)
        
        if context.getTokenGrant().getSessionDn() is not None:
            print "session id from context - %s" % context.getTokenGrant().getSessionDn()
            sessionId = sessionIdService.getSessionByDn(context.getTokenGrant().getSessionDn()) # fetch from persistence
            openbanking_intent_id = sessionId.getSessionAttributes().get("openbanking_intent_id")
            print "openbanking_intent_id from session : "+openbanking_intent_id
            responseAsJsonObject.accumulate("openbanking_intent_id", openbanking_intent_id)

        return True
        
    