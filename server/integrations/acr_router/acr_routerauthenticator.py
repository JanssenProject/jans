# Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
# Copyright (c) 2020, Janssen Project
#
# Author: Yuriy Movchan
#

from io.jans.model.custom.script.type.auth import PersonAuthenticationType


class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "ACR Router. Initialization"
        if not configurationAttributes.containsKey("new_acr_value"):
            print "ACR Router. Initialization. Property acr_router_value is mandatory"
            return False
        print "ACR Router. Initialized successfully"
        return True   

    def destroy(self, configurationAttributes):
        print "ACR Router. Destroy"
        print "ACR Router. Destroyed successfully"

        return True
        
    def getAuthenticationMethodClaims(self, requestParameters):
        return None

    def getApiVersion(self):
        return 11

    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        return False

    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        print "ACR Router. get new acr value"
        new_acr_value = configurationAttributes.get("new_acr_value").getValue2()
        # Put your custom logic to determine if routing required here...
        return new_acr_value


    def authenticate(self, configurationAttributes, requestParameters, step):
        return False

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        return True

    def getExtraParametersForStep(self, configurationAttributes, step):
        return None

    def getCountAuthenticationSteps(self, configurationAttributes):
        return 1

    def getPageForStep(self, configurationAttributes, step):
        return ""

    def getNextStep(self, configurationAttributes, requestParameters, step):
        return -1

    def getLogoutExternalUrl(self, configurationAttributes, requestParameters):
        print "Get external logout URL call"
        return None


    def logout(self, configurationAttributes, requestParameters):
        return True
