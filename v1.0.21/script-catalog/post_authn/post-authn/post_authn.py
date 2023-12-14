# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2020, Janssen
#
# Author: Yuriy Zabrovarnyy
#
#

from io.jans.model.custom.script.type.postauthn import PostAuthnType
from java.lang import String

class PostAuthn(PostAuthnType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Post Authn script. Initializing ..."
        print "Post Authn script. Initialized successfully"

        return True

    def destroy(self, configurationAttributes):
        print "Post Authn script. Destroying ..."
        print "Post Authn script. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 11

    # This method is called during Authorization Request at Authorization Endpoint.
    # If True is returned, session is set as unauthenticated and user is send for authentication.
    # Note :
    # context is reference of io.jans.as.service.external.context.ExternalPostAuthnContext(in https://github.com/JanssenFederation/oxauth project, )
    def forceReAuthentication(self, context):
        return False

    # This method is called during Authorization Request at Authorization Endpoint.
    # If True is returned user is send for Authorization. By default if client is "Pre-Authorized" or "Client Persist Authorizations" is on, authorization is skipped.
    # This script has higher priority and can cancel Pre-Authorization and persisted authorizations.
    # Note :
    # context is reference of io.jans.as.service.external.context.ExternalPostAuthnContext(in https://github.com/JanssenFederation/oxauth project, )
    # To get session object just call context.getSession()
    def forceAuthorization(self, context):
        return False