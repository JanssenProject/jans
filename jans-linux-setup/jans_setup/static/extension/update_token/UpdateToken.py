from io.jans.service.cdi.util import CdiUtil
from io.jans.model.custom.script.type.token import UpdateTokenType
from io.jans.as.server.service import SessionIdService
from io.jans.as.server.model.config import ConfigurationFactory
from io.jans.as.server.service import ClientService
from io.jans.as.server.service.net import HttpService
from java.nio.charset import Charset
from org.json import JSONObject
from jakarta.faces.context import FacesContext

import java
import sys
import os

class UpdateToken(UpdateTokenType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Update token script. Initializing ..."
        if (not configurationAttributes.containsKey("BILLING_API_URL")):
	        print "Update token script. Initialization. Property BILLING_API_URL is not specified"
	        return False
        else:
        	self.BILLING_API_URL = configurationAttributes.get("BILLING_API_URL").getValue2()

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
        print "Update token script. Modify AT: "
	sessionIdService = CdiUtil.bean(SessionIdService)
	sessionId = sessionIdService.getSessionByDn(context.getGrant().getSessionDn()) # fetch from persistence
        client_id = sessionId.getSessionAttributes().get("client_id")

        # get org_id from client_id
        clientService = CdiUtil.bean(ClientService)
        client = clientService.getClient(client_id)
        org_id = client.getOrganization()

        # the aud claim is mandatory in the auth header request (by Google API gateway)
        facesContext = CdiUtil.bean(FacesContext)
        request = facesContext.getExternalContext().getRequest()
        accessToken.getHeader().setClaim("aud", request)


        # query Billing API
        return self.balanceAvailable(org_id)

    # context is reference of io.jans.as.server.service.external.context.ExternalUpdateTokenContext (in https://github.com/JanssenProject/jans-auth-server project, )
    def getRefreshTokenLifetimeInSeconds(self, context):
        return 0

    # context is reference of io.jans.as.server.service.external.context.ExternalUpdateTokenContext (in https://github.com/JanssenProject/jans-auth-server project, )
    def getIdTokenLifetimeInSeconds(self, context):
        return 0

    # context is reference of io.jans.as.server.service.external.context.ExternalUpdateTokenContext (in https://github.com/JanssenProject/jans-auth-server project, )
    def getAccessTokenLifetimeInSeconds(self, context):
        return 0

    def balanceAvailable(self, org_id):
        httpService = CdiUtil.bean(HttpService)

        http_client = httpService.getHttpsClient()
        http_client_params = http_client.getParams()

        url = self.BILLING_API_URL + "organization_balance?organization_id="+org_id

        try:
            http_service_response = httpService.executeGet(http_client, url)
            http_response = http_service_response.getHttpResponse()
            response_bytes = httpService.getResponseContent(http_response)
            response_string = httpService.convertEntityToString(response_bytes, Charset.forName("UTF-8"))
            json_response = JSONObject(response_string)
            httpService.consume(http_response)
            print json_response.get("status")
            if  json_response.get("status") == "true":
                return True
            else:
                print "AT will not be created because balance is negative : %s " % json_response.get("status")
                return False

        except:
            print "Failed to invoke BILLING_API: ", sys.exc_info()[1]
            return False


        finally:
            http_service_response.closeConnection()
