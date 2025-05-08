# Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
# Copyright (c) 2020, Janssen Project
#
# Author: Yuriy Movchan
#
from __future__ import print_function
from io.jans.model.custom.script.type.auth import PersonAuthenticationType


class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        """
        Args:
            currentTimeMillis (int): current time in miliseconds
        """
        self.currentTimeMillis = currentTimeMillis

    @classmethod
    def init(cls, customScript, configurationAttributes):
        print("ACR Router. Initialization")
        if not configurationAttributes.containsKey("new_acr_value"):
            print("ACR Router. Initialization. Property acr_router_value is mandatory")
            return False
        print("ACR Router. Initialized successfully")
        return True

    @classmethod
    def destroy(cls, configurationAttributes):
        print("ACR Router. Destroy")
        print("ACR Router. Destroyed successfully")

        return True

    @classmethod
    def getAuthenticationMethodClaims(cls, requestParameters):
        return None

    @classmethod
    def getApiVersion(cls):
        return 11

    @classmethod
    def isValidAuthenticationMethod(cls, usageType, configurationAttributes):
        return False

    @classmethod
    def getAlternativeAuthenticationMethod(cls, usageType, configurationAttributes):
        print("ACR Router. get new acr value")
        new_acr_value = configurationAttributes.get("new_acr_value").getValue2()
        # Put your custom logic to determine if routing required here...
        return new_acr_value

    @classmethod
    def authenticate(cls, configurationAttributes, requestParameters, step):
        return False

    @classmethod
    def prepareForStep(cls, configurationAttributes, requestParameters, step):
        return True

    @classmethod
    def getExtraParametersForStep(cls, configurationAttributes, step):
        return None

    @classmethod
    def getCountAuthenticationSteps(cls, configurationAttributes):
        return 1

    @classmethod
    def getPageForStep(cls, configurationAttributes, step):
        return ""

    @classmethod
    def getNextStep(cls, configurationAttributes, requestParameters, step):
        return -1

    @classmethod
    def getLogoutExternalUrl(cls, configurationAttributes, requestParameters):
        print("Get external logout URL call")
        return None

    @classmethod
    def logout(cls, configurationAttributes, requestParameters):
        return True
