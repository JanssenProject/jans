# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2016, Gluu
#
# Author: Yuriy Movchan
#

from org.xdi.model.custom.script.type.client import ClientRegistrationType
from org.xdi.service.cdi.util import CdiUtil
from org.xdi.oxauth.service import ScopeService
from org.xdi.util import StringHelper, ArrayHelper
from java.util import Arrays, ArrayList, HashSet, Date, GregorianCalendar

import java

class ClientRegistration(ClientRegistrationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, configurationAttributes):
        print "Cred-manager client registration. Initialization"
        
        self.clientRedirectUrisSet = self.prepareClientRedirectUris(configurationAttributes)

        print "Cred-manager client registration. Initialized successfully"
        return True   

    def destroy(self, configurationAttributes):
        print "Cred-manager client registration. Destroy"
        print "Cred-manager client registration. Destroyed successfully"
        return True   

    # Update client entry before persistent it
    #   registerRequest is org.xdi.oxauth.client.RegisterRequest
    #   client is org.xdi.oxauth.model.registration.Client
    #   configurationAttributes is java.util.Map<String, SimpleCustomProperty>
    def updateClient(self, registerRequest, client, configurationAttributes):
        print "Cred-manager client registration. UpdateClient method"

        redirectUris = client.getRedirectUris()
        print "Cred-manager client registration. Redirect Uris: %s" % redirectUris

        credManagerClient = False
        for redirectUri in redirectUris:
            if (self.clientRedirectUrisSet.contains(redirectUri)):
                credManagerClient = True
                break
        
        if not credManagerClient:
            return True

        print "Cred-manager client registration. Client is Cred-manager"

        newScopes = client.getScopes()
        
        scopeService = CdiUtil.bean(ScopeService)

        profileScope = scopeService.getScopeByDisplayName("profile")
        clientinfoScope = scopeService.getScopeByDisplayName("clientinfo")
        usernameScope = scopeService.getScopeByDisplayName("user_name")

        newScopes = ArrayHelper.addItemToStringArray(newScopes, profileScope.getDn())
        newScopes = ArrayHelper.addItemToStringArray(newScopes, clientinfoScope.getDn())
        newScopes = ArrayHelper.addItemToStringArray(newScopes, usernameScope.getDn()) 

        print "Cred-manager client registration. Result scopes: %s" % newScopes
        client.setScopes(newScopes)
        #Extend client lifetime for one year
        cal=GregorianCalendar()
        cal.add(1,1)
        client.setClientSecretExpiresAt(Date(cal.getTimeInMillis()))
        #this style complains:  client.setClientSecretExpiresAt(Date(Date().getTime + 31536000000))

        return True

    def getApiVersion(self):
        return 1

    def prepareClientRedirectUris(self, configurationAttributes):
        clientRedirectUrisSet = HashSet()
        if not configurationAttributes.containsKey("client_redirect_uris"):
            return clientRedirectUrisSet

        clientRedirectUrisList = configurationAttributes.get("client_redirect_uris").getValue2()
        if StringHelper.isEmpty(clientRedirectUrisList):
            print "Cred-manager client registration. The property client_redirect_uris is empty"
            return clientRedirectUrisSet    

        clientRedirectUrisArray = StringHelper.split(clientRedirectUrisList, ",")
        if ArrayHelper.isEmpty(clientRedirectUrisArray):
            print "Cred-manager client registration. No clients specified in client_redirect_uris property"
            return clientRedirectUrisSet
        
        # Convert to HashSet to quick search
        i = 0
        count = len(clientRedirectUrisArray)
        while i < count:
            uris = clientRedirectUrisArray[i]
            clientRedirectUrisSet.add(uris)
            i = i + 1

        return clientRedirectUrisSet
