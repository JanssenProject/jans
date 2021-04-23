# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2018, Janssen
#
# Author: Jose Gonzalez

from io.jans.model.custom.script.type.client import ClientRegistrationType
from io.jans.service.cdi.util import CdiUtil
from io.jans.as.service import ScopeService
from io.jans.util import StringHelper, ArrayHelper
from java.util import Arrays, ArrayList, HashSet, Date, GregorianCalendar

import java

class ClientRegistration(ClientRegistrationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Casa client registration. Initialization"
        self.clientRedirectUrisSet = self.prepareClientRedirectUris(configurationAttributes)
        print "Casa client registration. Initialized successfully"
        return True   

    def destroy(self, configurationAttributes):
        print "Casa client registration. Destroy"
        print "Casa client registration. Destroyed successfully"
        return True   

    # Update client entry before persistent it
    #   registerRequest is io.jans.as.client.RegisterRequest
    #   client is io.jans.as.model.registration.Client
    #   configurationAttributes is java.util.Map<String, SimpleCustomProperty>
    def createClient(self, registerRequest, client, configurationAttributes):

        print "Casa client registration. CreateClient method"
        redirectUris = client.getRedirectUris()
        print "Casa client registration. Redirect Uris: %s" % redirectUris

        credManagerClient = False
        for redirectUri in redirectUris:
            if self.clientRedirectUrisSet.contains(redirectUri):
                credManagerClient = True
                break
        
        if not credManagerClient:
            return True

        print "Casa client registration. Client is Janssen Casa"
        self.setClientScopes(client, configurationAttributes.get("scopes"))
        #Extend client lifetime for one year
        cal=GregorianCalendar()
        cal.add(1,10)
        client.setClientSecretExpiresAt(Date(cal.getTimeInMillis()))
        client.setTrustedClient(True)
        return True

    # Update client entry before persistent it
    #   registerRequest is io.jans.as.client.RegisterRequest
    #   client is io.jans.as.model.registration.Client
    #   configurationAttributes is java.util.Map<String, SimpleCustomProperty>
    def updateClient(self, registerRequest, client, configurationAttributes):
        print "Casa client registration. UpdateClient method"       
        self.setClientScopes(client, configurationAttributes.get("scopes"))
        return True

    def getApiVersion(self):
        return 11

    def setClientScopes(self, client, requiredScopes):
        
        if requiredScopes == None:
            print "Casa client registration. No list of scopes was passed in script parameters"
            return

        requiredScopes = StringHelper.split(requiredScopes.getValue2(), ",")
        newScopes = client.getScopes()
        scopeService = CdiUtil.bean(ScopeService)

        for scopeName in requiredScopes:
            scope = scopeService.getScopeById(scopeName)
            if not scope.isDefaultScope():
                print "Casa client registration. Adding scope '%s'" % scopeName
                newScopes = ArrayHelper.addItemToStringArray(newScopes, scope.getDn())

        print "Casa client registration. Result scopes are: %s" % newScopes
        client.setScopes(newScopes)
        
        
    def prepareClientRedirectUris(self, configurationAttributes):
        clientRedirectUrisSet = HashSet()
        if not configurationAttributes.containsKey("client_redirect_uris"):
            return clientRedirectUrisSet

        clientRedirectUrisList = configurationAttributes.get("client_redirect_uris").getValue2()
        if StringHelper.isEmpty(clientRedirectUrisList):
            print "Casa client registration. The property client_redirect_uris is empty"
            return clientRedirectUrisSet    

        clientRedirectUrisArray = StringHelper.split(clientRedirectUrisList, ",")
        if ArrayHelper.isEmpty(clientRedirectUrisArray):
            print "Casa client registration. No clients specified in client_redirect_uris property"
            return clientRedirectUrisSet
        
        # Convert to HashSet to quick search
        i = 0
        count = len(clientRedirectUrisArray)
        while i < count:
            uris = clientRedirectUrisArray[i]
            clientRedirectUrisSet.add(uris)
            i = i + 1

        return clientRedirectUrisSet
