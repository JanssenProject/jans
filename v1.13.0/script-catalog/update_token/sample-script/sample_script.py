# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2021, Gluu
#
# Author: Yuriy Movchan
#
#

from io.jans.model.custom.script.type.token import UpdateTokenType

class UpdateToken(UpdateTokenType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Update token script. Initializing ..."
        print "Update token script. Initialized successfully"

        return True

    def destroy(self, configurationAttributes):
        print "Update token script. Destroying ..."
        print "Update token script. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 11

    # Returns boolean, true - indicates that script applied changes
    # This method is called after adding headers and claims. Hence script can override them
    # Note :
    # jsonWebResponse - is io.jans.as.model.token.JsonWebResponse, you can use any method to manipulate JWT
    # context is reference of io.jans.as.server.service.external.context.ExternalUpdateTokenContext (in https://github.com/JanssenProject/jans-auth-server project, )
    def modifyIdToken(self, jsonWebResponse, context):
        print "Update token script. Modify idToken: %s" % jsonWebResponse

        jsonWebResponse.getHeader().setClaim("custom_header_name", "custom_header_value")
        jsonWebResponse.getClaims().setClaim("custom_claim_name", "custom_claim_value")

        print "Update token script. After modify idToken: %s" % jsonWebResponse
        return True

    # Returns boolean, true - indicates that script applied changes. If false is returned token will not be created.
    # refreshToken is reference of io.jans.as.server.model.common.RefreshToken (note authorization grant can be taken as context.getGrant())
    # context is reference of io.jans.as.server.service.external.context.ExternalUpdateTokenContext (in https://github.com/JanssenProject/jans-auth-server project, )
    def modifyRefreshToken(self, refreshToken, context):
        return True

    # Returns boolean, true - indicates that script applied changes. If false is returned token will not be created.
    # accessToken is reference of io.jans.as.server.model.common.AccessToken (note authorization grant can be taken as context.getGrant())
    # context is reference of io.jans.as.server.service.external.context.ExternalUpdateTokenContext (in https://github.com/JanssenProject/jans-auth-server project, )
    def modifyAccessToken(self, accessToken, context):
        return True

    # context is reference of io.jans.as.server.service.external.context.ExternalUpdateTokenContext (in https://github.com/JanssenProject/jans-auth-server project, )
    def getRefreshTokenLifetimeInSeconds(self, context):
        return 0

    # context is reference of io.jans.as.server.service.external.context.ExternalUpdateTokenContext (in https://github.com/JanssenProject/jans-auth-server project, )
    def getIdTokenLifetimeInSeconds(self, context):
        return 0

    # context is reference of io.jans.as.server.service.external.context.ExternalUpdateTokenContext (in https://github.com/JanssenProject/jans-auth-server project, )
    def getAccessTokenLifetimeInSeconds(self, context):
        return 0
