# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2016, Janssen
#
# Author: Yuriy Movchan
#

from io.jans.model.custom.script.type.client import ClientRegistrationType
from io.jans.service.cdi.util import CdiUtil
from io.jans.as.service import ScopeService
from io.jans.util import StringHelper, ArrayHelper
from java.util import Arrays, ArrayList, HashSet

import java

class ClientRegistration(ClientRegistrationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Client registration. Initialization"
        
        self.clientRedirectUrisSet = self.prepareClientRedirectUris(configurationAttributes)

        print "Client registration. Initialized successfully"
        return True   

    def destroy(self, configurationAttributes):
        print "Client registration. Destroy"
        print "Client registration. Destroyed successfully"
        return True   

    # Update client entry before persistent it
    #   registerRequest is io.jans.as.client.RegisterRequest
    #   client is io.jans.as.model.registration.Client
    #   configurationAttributes is java.util.Map<String, SimpleCustomProperty>
    def createClient(self, registerRequest, client, configurationAttributes):
        print "Client registration. CreateClient method"

        redirectUris = client.getRedirectUris()
        print "Client registration. Redirect Uris: %s" % redirectUris

        addAddressScope = False
        for redirectUri in redirectUris:
            if (self.clientRedirectUrisSet.contains(redirectUri)):
                addAddressScope = True
                break
        
        print "Client registration. Is add address scope: %s" % addAddressScope

        if addAddressScope:
            currentScopes = client.getScopes()
            print "Client registration. Current scopes: %s" % currentScopes
            
            scopeService = CdiUtil.bean(ScopeService)
            addressScope = scopeService.getScopeByDisplayName("address")
            newScopes = ArrayHelper.addItemToStringArray(currentScopes, addressScope.getDn())
    
            print "Client registration. Result scopes: %s" % newScopes
            client.setScopes(newScopes)

        return True

    # Update client entry before persistent it
    #   registerRequest is io.jans.as.client.RegisterRequest
    #   client is io.jans.as.model.registration.Client
    #   configurationAttributes is java.util.Map<String, SimpleCustomProperty>
    def updateClient(self, registerRequest, client, configurationAttributes):
        print "Client registration. UpdateClient method"
        return True

    def getApiVersion(self):
        return 11

    def prepareClientRedirectUris(self, configurationAttributes):
        clientRedirectUrisSet = HashSet()
        if not configurationAttributes.containsKey("client_redirect_uris"):
            return clientRedirectUrisSet

        clientRedirectUrisList = configurationAttributes.get("client_redirect_uris").getValue2()
        if StringHelper.isEmpty(clientRedirectUrisList):
            print "Client registration. The property client_redirect_uris is empty"
            return clientRedirectUrisSet    

        clientRedirectUrisArray = StringHelper.split(clientRedirectUrisList, ",")
        if ArrayHelper.isEmpty(clientRedirectUrisArray):
            print "Client registration. No clients specified in client_redirect_uris property"
            return clientRedirectUrisSet
        
        # Convert to HashSet to quick search
        i = 0
        count = len(clientRedirectUrisArray)
        while i < count:
            uris = clientRedirectUrisArray[i]
            clientRedirectUrisSet.add(uris)
            i = i + 1

        return clientRedirectUrisSet

    # Returns secret key which will be used to validate Software Statement if HMAC algorithm is used (e.g. HS256, HS512). Invoked if oxauth conf property softwareStatementValidationType=SCRIPT which is default/fallback value.
    # context is reference of io.jans.as.service.external.context.DynamicClientRegistrationContext (in https://github.com/JanssenFederation/oxauth project )
    def getSoftwareStatementHmacSecret(self, context):
        return ""

    # Returns JWKS which will be used to validate Software Statement if keys are used (e.g. RS256). Invoked if oxauth conf property softwareStatementValidationType=SCRIPT which is default/fallback value.
    # context is reference of io.jans.as.service.external.context.DynamicClientRegistrationContext (in https://github.com/JanssenFederation/oxauth project )
    def getSoftwareStatementJwks(self, context):
        return ""
