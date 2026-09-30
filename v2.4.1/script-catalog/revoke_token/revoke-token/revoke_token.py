# oxAuth is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
# Copyright (c) 2020, Gluu
#
# Author: Yuriy Zabrovarnyy
#
#

from org.gluu.model.custom.script.type.revoke import RevokeTokenType
from java.lang import String

class RevokeToken(RevokeTokenType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Revoke Token script. Initializing ..."
        print "Revoke Token script. Initialized successfully"

        return True

    def destroy(self, configurationAttributes):
        print "Revoke Token script. Destroying ..."
        print "Revoke Token script. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 11

    # This method is called during Revoke Token call.
    # If True is returned, token is revoked. If False is returned, revoking is skipped.
    # Note :
    # context is reference of io.jans.as.server.model.common.ExecutionContextt(in https://github.com/JanssenProject/jans-auth-server project, )
    def revoke(self, context):
        return True
