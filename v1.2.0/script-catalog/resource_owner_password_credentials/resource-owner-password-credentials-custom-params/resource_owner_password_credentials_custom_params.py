# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2019, Janssen
#
# Author: Yuriy Mochan
#
#

from io.jans.model.custom.script.type.owner import ResourceOwnerPasswordCredentialsType
from io.jans.as.server.service import AuthenticationService, SessionIdService
from io.jans.as.model.common import SessionIdState
from io.jans.as.server.security import Identity
from io.jans.service.cdi.util import CdiUtil
from io.jans.as.model.authorize import AuthorizeRequestParam
from io.jans.as.model.config import Constants
from io.jans.util import StringHelper
from java.lang import String
from java.util import Date, HashMap

class ResourceOwnerPasswordCredentials(ResourceOwnerPasswordCredentialsType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "ROPC script. Initializing ..."
        print "ROPC script. Initialized successfully"
        return True

    def destroy(self, configurationAttributes):
        print "ROPC script. Destroying ..."
        print "ROPC script. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 11

    # Returns True and set user into context when user authenticated succesfully
    # Returns False when user not authenticated or it's needed to cancel notmal flow
    # Note :
    # context is reference of io.jans.as.service.external.context.ExternalResourceOwnerPasswordCredentialsContext#ExternalResourceOwnerPasswordCredentialsContext (in https://github.com/JanssenFederation/oxauth project, )
    def authenticate(self, context):
        print "ROPC script. Authenticate"

        # Do generic authentication
        authenticationService = CdiUtil.bean(AuthenticationService)

        username = context.getHttpRequest().getParameter("username")
        password = context.getHttpRequest().getParameter("password")
        result = authenticationService.authenticate(username, password)
        if not result:
            print "ROPC script. Authenticate. Could not authenticate user '%s' " % username
            return False

        context.setUser(authenticationService.getAuthenticatedUser())
        print "ROPC script. Authenticate. User '%s' authenticated successfully" % username
        

        # Get cusom parameters from request
        customParam1Value = context.getHttpRequest().getParameter("custom1")
        customParam2Value = context.getHttpRequest().getParameter("custom2")

        customParameters = {}
        customParameters["custom1"] = customParam1Value
        customParameters["custom2"] = customParam2Value
        print "ROPC script. Authenticate. User '%s'. Creating authenticated session with custom attributes: '%s'" % (username, customParameters)

        session = self.createNewAuthenticatedSession(context, customParameters)
        
        # This is needed to allow store in token entry sessionId
        authenticationService.configureEventUser(session)

        print "ROPC script. Authenticate. User '%s'. Created authenticated session: '%s'" % (username, customParameters)

        return True

    def createNewAuthenticatedSession(self, context, customParameters={}):
        sessionIdService = CdiUtil.bean(SessionIdService)

        user = context.getUser()
        client = CdiUtil.bean(Identity).getSessionClient().getClient()

        # Add mandatory session parameters
        sessionAttributes = HashMap()
        sessionAttributes.put(Constants.AUTHENTICATED_USER, user.getUserId())
        sessionAttributes.put(AuthorizeRequestParam.CLIENT_ID, client.getClientId())
        sessionAttributes.put(AuthorizeRequestParam.PROMPT, "")

        # Add custom session parameters
        for key, value in customParameters.iteritems():
            if StringHelper.isNotEmpty(value):
                sessionAttributes.put(key, value)

        # Generate authenticated session
        sessionId = sessionIdService.generateAuthenticatedSessionId(context.getHttpRequest(), user.getDn(), sessionAttributes)

        print "ROPC script. Generated session id. DN: '%s'" % sessionId.getDn()

        return sessionId
