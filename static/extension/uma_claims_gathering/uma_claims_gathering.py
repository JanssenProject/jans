# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2017, Gluu
#
# Author: Yuriy Zabrovarnyy
#

from org.xdi.model.custom.script.type.uma import UmaClaimsGatheringType
from org.xdi.util import ArrayHelper

class UmaClaimsGathering(UmaClaimsGatheringType):

    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, configurationAttributes):
        print "Claims-Gathering. Initializing ..."
        print "Claims-Gathering. Initialized successfully"

        return True

    def destroy(self, configurationAttributes):
        print "Claims-Gathering. Destroying ..."
        print "Claims-Gathering. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 1


    # Main gather method. Must return True (if gathering performed successfully) or False (if fail).
    # Method must set claim into context (via context.putClaim('name', value)) in order to persist it (otherwise it will be lost).
    def gather(self, step, context): # context is reference of org.xdi.oxauth.uma.authorization.UmaGatherContext
        print "Claims-Gathering. Gathering ..."

        if (step == 1):
            if (context.getRequestParameters().containsKey("country")):
                valueArray = context.getRequestParameters().get("country")

                if ArrayHelper.isEmpty(valueArray):
                    print "Claims-Gathering. 'country' is not provided on step 1."
                    return False

                context.putClaim("country", valueArray[0])
                return True

            return False

        elif (step == 2):
            if (context.getRequestParameters().containsKey("city")):
                valueArray = context.getRequestParameters().get("city")

                if ArrayHelper.isEmpty(valueArray):
                    print "Claims-Gathering. 'city' is not provided on step 2."
                    return False

                context.putClaim("city", valueArray[0])
                return True

        return False

    def getNextStep(self, step, context):
        return -1

    def prepareForStep(self, step, context):
        return True

    def getStepsCount(self, context):
        return 2

    def getPageForStep(self, step, context):
        if (step == 1):
            return "/uma2/country.xhtml"
        elif (step == 2):
            return "/uma2/city.xhtml"
        return ""
