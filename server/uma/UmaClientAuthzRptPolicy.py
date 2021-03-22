# Janssen Project software is available under the MIT License (2017). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2020, Janssen Project
#
# Author: Jose Gonzalez
# Adapted from previous 3.0.1 script of Yuriy Movchan
#
# oxConfigurationProperty required:
#   allowed_clients - comma separated list of dns of allowed clients
#   (i.e. the SCIM RP client)

from __future__ import print_function

from io.jans.model.custom.script.type.uma import UmaRptPolicyType
from io.jans.model.uma import ClaimDefinitionBuilder
from io.jans.util import StringHelper, ArrayHelper
from java.util import HashSet

class UmaRptPolicy(UmaRptPolicyType):

    def __init__(self, currentTimeMillis):
        """Construct class.

        Args:
            currentTimeMillis (int): current time in miliseconds
        """
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print("RPT Policy. Initializing ...")
        self.clientsSet = self.prepareClientsSet(configurationAttributes)
        print("RPT Policy. Initialized successfully")
        return True

    @classmethod
    def destroy(cls, configurationAttributes):
        print("RPT Policy. Destroyed successfully")
        return True

    @classmethod
    def getApiVersion(cls):
        return 11

    @classmethod
    def getRequiredClaims(cls, context):
        json = """[
        ]"""
        return ClaimDefinitionBuilder.build(json)

    def authorize(self, context): # context is reference of org.gluu.oxauth.uma.authorization.UmaAuthorizationContext
        print("RPT Policy. Authorizing ...")

        client_id=context.getClient().getClientId()
        print("UmaRptPolicy. client_id = %s" % client_id)

        if (StringHelper.isEmpty(client_id)):
            return False

        if (self.clientsSet.contains(client_id)):
            print("UmaRptPolicy. Authorizing client")
            return True
        else:
            print("UmaRptPolicy. Client isn't authorized")
            return False

    @classmethod
    def getClaimsGatheringScriptName(cls, context):
        return ""

    @classmethod
    def prepareClientsSet(cls, configurationAttributes):
        clientsSet = HashSet()
        if (not configurationAttributes.containsKey("allowed_clients")):
            return clientsSet

        allowedClientsList = configurationAttributes.get("allowed_clients").getValue2()
        if (StringHelper.isEmpty(allowedClientsList)):
            print("UmaRptPolicy. The property allowed_clients is empty")
            return clientsSet

        allowedClientsListArray = StringHelper.split(allowedClientsList, ",")
        if (ArrayHelper.isEmpty(allowedClientsListArray)):
            print("UmaRptPolicy. No clients specified in allowed_clients property")
            return clientsSet

        # Convert to HashSet to quick search
        i = 0
        count = len(allowedClientsListArray)
        while (i < count):
            client = allowedClientsListArray[i]
            clientsSet.add(client)
            i = i + 1

        return clientsSet
