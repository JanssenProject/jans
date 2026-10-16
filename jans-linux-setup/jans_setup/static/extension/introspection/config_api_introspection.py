# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2019, Janssen
#
# Author: Yuriy Mochan
#
#

from io.jans.model.custom.script.type.introspection import IntrospectionType
from io.jans.as.server.model.common import AuthorizationGrantList
from io.jans.as.server.service import SessionIdService
from io.jans.service.cdi.util import CdiUtil


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

    def modifyResponse(self, responseAsJsonObject, context):
        print "inside config_api_introspection.py modifyResponse"
        token = context.getHttpRequest().getParameter("token")
        print token

        if token is None:
            print "Introspection. There is no token in request"
            return False

        print "Get authorizationGrant"
        authorizationGrant = context.getGrantOfIntrospectionToken()
        print authorizationGrant
        print authorizationGrant.getClass().getName()
        user = authorizationGrant.getUser()
        if user is not None:
            print "Introspection. No user on grant (client credentials token?)"
            print user2.getClass().getDeclaredMethods()
            print user.getUserId()
            print user.getDn()
           
        authorizationGrant2 = context.getTokenGrant()
        print authorizationGrant2

        if authorizationGrant2 is not None:
            print "Introspection. Failed to load authorization grant by context"

        user2 = authorizationGrant.getUser()
        print user2
        print user2.getClass().getDeclaredMethods()
        if user2 is not None:
            print "Introspection. No user on grant (client credentials token?)"
            print user2.getUserId()
            print user2.getDn()


        responseAsJsonObject.accumulate("user_id", user2.getUserId())
        responseAsJsonObject.accumulate("user_dn", user2.getDn())
        return True