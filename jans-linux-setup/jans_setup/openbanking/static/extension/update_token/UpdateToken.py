from io.jans.service.cdi.util import CdiUtil
from io.jans.model.custom.script.type.token import UpdateTokenType
from io.jans.as.server.service import SessionIdService
from io.jans.as.server.model.config import ConfigurationFactory
import java
import sys
import os

class UpdateToken(UpdateTokenType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Update token script. Initializing ..."
        print "Update token script. Initialized successfully"

        return True

    def destroy(self, configurationAttributes):
        print "Update token   script. Destroying ..."
        print "Update token    script. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 11

    # Returns boolean, true - indicates that script applied changes
    # This method is called after adding headers and claims. Hence script can override them
    # Note :
    # jsonWebResponse - is JwtHeader, you can use any method to manipulate JWT
    # context is reference of io.jans.oxauth.service.external.context.ExternalUpdateTokenContext (in https://github.com/GluuFederation/oxauth project, )
    def modifyIdToken(self, jsonWebResponse, context):
                print "Update token obconnect script. Modify idToken: %s" % jsonWebResponse
		try :
			sessionIdService = CdiUtil.bean(SessionIdService)
			print "session id from context - %s" % context.getGrant().getSessionDn().strip("oxId=")

			sessionId = sessionIdService.getSessionByDn(context.getGrant().getSessionDn()) # fetch from persistence

                        
			print "session id -%s " % sessionId.getSessionAttributes()
			openbanking_intent_id = sessionId.getSessionAttributes().get("openbanking_intent_id")
			acr = sessionId.getSessionAttributes().get("acr_ob")

            		# An example of how to set header claims
			#jsonWebResponse.getHeader().setClaim("custom_header_name", "custom_header_value")
			
			#custom claims
			jsonWebResponse.getClaims().setClaim("openbanking_intent_id", openbanking_intent_id)
            		# If the ASPSP issues a refresh token, the ASPSP must indicate the date-time at which the refresh token # # will expire in a claim named http://openbanking.org.uk/refresh_token_expires_at in the Id token (returned # by the token end-point or userinfo end-point). Its value MUST be a number containing a NumericDate value, # as specified in https://tools.ietf.org/html/rfc7519#section-2
            		refresh_token_expires_at = CdiUtil.bean(ConfigurationFactory).getAppConfiguration().getRefreshTokenLifetime()
            		jsonWebResponse.getClaims().setClaim("refresh_token_expires_at", refresh_token_expires_at)
            	
			# this claim is currently commented and should have the unique id of the user for whom consent was passed
            		# please fill it as per the implementation
			jsonWebResponse.getClaims().setClaim("sub", openbanking_intent_id)

			print "Update token script. After modify idToken: %s" % jsonWebResponse
		
			# Use this blog to implement how RT claims can be retained. https://github.com/GluuFederation/oxAuth/wiki/Retain-access-token-claim

			return True
		except:
	                print "update token failure" , sys.exc_info()[1]
	                return None

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