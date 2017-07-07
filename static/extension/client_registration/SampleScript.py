# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2016, Gluu
#
# Author: Yuriy Movchan
#

from org.xdi.service.cdi.util import CdiUtil
from org.xdi.model.custom.script.type.client import ClientRegistrationType
from org.xdi.util import StringHelper, ArrayHelper
from org.xdi.oxauth.service import ScopeService
from java.util import Arrays, ArrayList

import java

class ClientRegistration(ClientRegistrationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, configurationAttributes):
        print "Client registration. Initialization"

        print "Client registration. Initialized successfully"

        return True   

    def destroy(self, configurationAttributes):
        print "Client registration. Destroy"
        print "Client registration. Destroyed successfully"
        return True   

    # Update client entry before persistent it
    #   registerRequest is org.xdi.oxauth.client.RegisterRequest
    #   client is org.xdi.oxauth.model.registration.Client
    #   configurationAttributes is java.util.Map<String, SimpleCustomProperty>
    def updateClient(self, registerRequest, client, configurationAttributes):
        print "Client registration. UpdateClient method"

        redirectUris = client.getRedirectUris()
        print "Client registration. Redirect Uris:", redirectUris

        addAddressScope = False
        for redirectUri in redirectUris:
            if (StringHelper.equalsIgnoreCase(redirectUri, "https://client.example.com/example1")):
                addAddressScope = True
                break
        
        print "Client registration. Is add address scope:", addAddressScope

        if (addAddressScope):
            currentScopes = client.getScopes()
            print "Client registration. Current scopes:", currentScopes
            
            scopeService = CdiUtil.bean(ScopeService)
            addressScope = scopeService.getScopeByDisplayName("address")
            newScopes = ArrayHelper.addItemToStringArray(currentScopes, addressScope.getDn())
    
            print "Client registration. Result scopes:", newScopes
            client.setScopes(newScopes)

        return True

    def getApiVersion(self):
        return 1
