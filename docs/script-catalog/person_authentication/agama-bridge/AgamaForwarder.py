# Janssen Project software is available under the Apache 2.0 License (2004). See http://www.apache.org/licenses/ for full text.
# Copyright (c) 2020, Janssen Project
#

from com.nimbusds.oauth2.sdk import AuthorizationRequest

from jakarta.faces.application import FacesMessage

from io.jans.as.model.configuration import AppConfiguration 
from io.jans.agama import NativeJansFlowBridge
from io.jans.agama.engine.misc import FlowUtils
from io.jans.agama.engine.service import WebContext
from io.jans.jsf2.service import FacesService
from io.jans.model.custom.script.type.auth import PersonAuthenticationType
from io.jans.service.cdi.util import CdiUtil

from java.util import Collections
from java.net import URI

import java
import sys

class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Agama forwarder. Successful initialization"
        self.authzEndpoint = CdiUtil.bean(AppConfiguration).getAuthorizationEndpoint()
        return True

    def destroy(self, configurationAttributes):
        print "Agama forwarder. Destroyed successfully"
        return True
        
    def getAuthenticationMethodClaims(self, requestParameters):
        return None
        
    def getApiVersion(self):
        return 11

    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        return True

    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        return None

    def authenticate(self, configurationAttributes, requestParameters, step):
        #this should never be reached
        return False

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        
        if not CdiUtil.bean(FlowUtils).serviceEnabled():
            print "Agama forwarder. Please ENABLE Agama engine in auth-server configuration"
            return False

        if step == 1:
            print "Agama forwarder. Prepare for Step 1"
            print "Agama forwarder. Creating a new authn request"
            
            try:                
                url = CdiUtil.bean(WebContext).getRequestUrl()
                i = url.find("?")
                query = url[i+1:]
                
                authReq = AuthorizationRequest.parse(query)                
                params = authReq.toParameters()
                
                # Currently, the redirection /jans-auth/restv1/authorize -> /jans-auth/authorize.htm removes the prompt parameters
                #prompts = params.get("prompt")
                #if prompts == None:
                #    prompts = []
                #hasLogin = False
                
                #for prompt in prompts:
                #    if prompt != None and ("login" in str(prompt.split())):
                #        hasLogin = True
                #        break
                 
                #if not hasLogin:
                #    print "Agama forwarder. Incoming request has no prompt=login"
                #    return False
            
                #params.remove("prompt")
                params.put("acr_values", Collections.singletonList("agama"))
                
                authReq = AuthorizationRequest.parse(URI(self.authzEndpoint), params)
                
                print "Agama forwarder. Redirecting..."
                CdiUtil.bean(FacesService).redirectToExternalURL(authReq.toURI().toString())
                return True
            except:
                print "Agama forwarder. Exception: ", sys.exc_info()[1]
            
        return False
        
    def getExtraParametersForStep(self, configurationAttributes, step):
        return None

    def getCountAuthenticationSteps(self, configurationAttributes):
        return 1

    def getPageForStep(self, configurationAttributes, step):
        # use the same page of agama bridge
        return "/" + CdiUtil.bean(NativeJansFlowBridge).scriptPageUrl()

    def getNextStep(self, configurationAttributes, requestParameters, step):
        return -1

    def getLogoutExternalUrl(self, configurationAttributes, requestParameters):
        return None

    def logout(self, configurationAttributes, requestParameters):
        return True
