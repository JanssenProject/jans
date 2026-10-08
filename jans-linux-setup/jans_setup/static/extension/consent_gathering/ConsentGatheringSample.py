# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2017, Janssen
#
# Author: Yuriy Movchan
#

from io.jans.model.custom.script.type.authz import ConsentGatheringType

import random

class ConsentGathering(ConsentGatheringType):

    def __init__(self, current_time_millis):
        self.currentTimeMillis = current_time_millis

    def init(self, custom_script, configuration_attributes):
        print("Consent-Gathering. Initializing ...")
        print("Consent-Gathering. Initialized successfully")

        return True

    def destroy(self, configuration_attributes):
        print("Consent-Gathering. Destroying ...")
        print("Consent-Gathering. Destroyed successfully")

        return True

    def getApiVersion(self):
        return 11

    # Main consent-gather method. Must return True (if gathering performed successfully) or False (if fail).
    # All user entered values can be access via Map<String, String> context.getPageAttributes()
    def authorize(self, step, context): # context is reference of io.jans.as.service.external.context.ConsentGatheringContext
        print("Consent-Gathering. Authorizing...")

        if step == 1:
            allow_button = context.getRequestParameters().get("authorizeForm:allowButton")
            if (allow_button is not None) and (len(allow_button) > 0):
                print("Consent-Gathering. Authorization success for step 1")
                return True

            print("Consent-Gathering. Authorization declined for step 1")
        elif step == 2:
            allow_button = context.getRequestParameters().get("authorizeForm:allowButton")
            if (allow_button is not None) and (len(allow_button) > 0):
                print("Consent-Gathering. Authorization success for step 2")
                return True

            print("Consent-Gathering. Authorization declined for step 2")

        return False

    def getNextStep(self, step, context):
        return -1

    def prepareForStep(self, step, context):
        if not context.isAuthenticated():
            print("User is not authenticated. Aborting authorization flow ...")
            return False

        if step == 2:
            page_attributes = context.getPageAttributes()
            
            # Generate random consent gathering request
            consent_request = "Requested transaction #%s approval for the amount of sum $ %s.00" % ( random.SystemRandom().randint(100000, 1000000), random.SystemRandom().randint(1, 100) )
            page_attributes.put("consent_request", consent_request)
            return True

        return True

    def getStepsCount(self, context):
        return 2

    def getPageForStep(self, step, context):
        if step == 1:
            return "/authz/authorize.xhtml"
        elif step == 2:
            return "/authz/transaction.xhtml"

        return ""
