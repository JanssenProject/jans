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
        token = context.getHttpRequest().getParameter("token")
        if token is None:
            print "Introspection. There is no token in request"
            return False

        authorizationGrant = context.getTokenGrant()
        if authorizationGrant is None:
            print "Introspection. Failed to load authorization grant by context"
            return False

        user = authorizationGrant.getUser()
        if user is None:
            print "Introspection. No user on grant (client credentials token?)"
            return True

        responseAsJsonObject.accumulate("user_id", user.getUserId())
        responseAsJsonObject.accumulate("user_dn", user.getDn())
        return True