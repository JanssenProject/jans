# Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
# Copyright (c) 2020, Janssen Project
#
# Author: Yuriy Movchan
#

from io.jans.service.cdi.util import CdiUtil
from io.jans.as.server.security import Identity
from io.jans.model.custom.script.type.auth import PersonAuthenticationType
from io.jans.as.server.service import UserService, AuthenticationService
from io.jans.util import ArrayHelper, StringHelper
from java.util import ArrayList, Arrays

from Cas2ExternalAuthenticator import PersonAuthentication as Cas2ExternalAuthenticator
from DuoExternalAuthenticator import PersonAuthentication as DuoExternalAuthenticator

class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

        self.cas2ExternalAuthenticator = Cas2ExternalAuthenticator(currentTimeMillis)
        self.duoExternalAuthenticator = DuoExternalAuthenticator(currentTimeMillis)

    def init(self, customScript, configurationAttributes):
        print "CAS2 + Duo. Initialization"
        
        cas2_result = self.cas2ExternalAuthenticator.init(configurationAttributes)
        duo_result = self.duoExternalAuthenticator.init(configurationAttributes)

        print "CAS2 + Duo. Initialized successfully"

        return cas2_result and duo_result

    def destroy(self, configurationAttributes):
        print "CAS2 + Duo. Destroy"

        cas2_result = self.cas2ExternalAuthenticator.destroy(configurationAttributes)
        duo_result = self.duoExternalAuthenticator.destroy(configurationAttributes)

        print "CAS2 + Duo. Destroyed successfully"

        return cas2_result and duo_result

    def getApiVersion(self):
        return 11

    def getAuthenticationMethodClaims(self, requestParameters):
        return None

    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        cas2_result = self.cas2ExternalAuthenticator.isValidAuthenticationMethod(usageType, configurationAttributes)
        duo_result = self.duoExternalAuthenticator.isValidAuthenticationMethod(usageType, configurationAttributes)

        return cas2_result and duo_result

    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        cas2_result = self.cas2ExternalAuthenticator.getAlternativeAuthenticationMethod(usageType, configurationAttributes)
        if cas2_result != None:
            return cas2_result

        duo_result = self.duoExternalAuthenticator.getAlternativeAuthenticationMethod(usageType, configurationAttributes)
        if duo_result != None:
            return duo_result

        return None

    def authenticate(self, configurationAttributes, requestParameters, step):
        result = False

        start_duo = False
        if step == 1:
            # Execute CAS2 for step #1
            result = self.cas2ExternalAuthenticator.authenticate(configurationAttributes, requestParameters, step)

            # Execute DUO prepareForStep and authenticate for step #1 if needed
            cas2_count_steps = self.cas2ExternalAuthenticator.getCountAuthenticationSteps(configurationAttributes)
            if cas2_count_steps == 1:
                result = result and self.duoExternalAuthenticator.prepareForStep(configurationAttributes, requestParameters, step)
                result = result and self.duoExternalAuthenticator.authenticate(configurationAttributes, requestParameters, step)
        elif step == 2:
            # Execute CAS2 for step #2 if needed
            cas2_count_steps = self.cas2ExternalAuthenticator.getCountAuthenticationSteps(configurationAttributes)
            if cas2_count_steps == 2:
                result = self.cas2ExternalAuthenticator.authenticate(configurationAttributes, requestParameters, step)

                # Execute DUO prepareForStep and authenticate for step #1
                result = result and self.duoExternalAuthenticator.prepareForStep(configurationAttributes, requestParameters, 1)
                result = result and self.duoExternalAuthenticator.authenticate(configurationAttributes, requestParameters, 1)
            else:
                duo_count_steps = self.duoExternalAuthenticator.getCountAuthenticationSteps(configurationAttributes)
                if duo_count_steps == 2:
                    result = self.duoExternalAuthenticator.authenticate(configurationAttributes, requestParameters, step)
        elif step == 3:
            # Execute DUO for step #2 if needed
            duo_count_steps = self.duoExternalAuthenticator.getCountAuthenticationSteps(configurationAttributes)
            if duo_count_steps == 2:
                result = self.duoExternalAuthenticator.authenticate(configurationAttributes, requestParameters, 2)

        return result

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        result = False

        # Execute CAS2 for step #1
        if step == 1:
            # Execute CAS2 for step #1
            result = self.cas2ExternalAuthenticator.prepareForStep(configurationAttributes, requestParameters, step)
        elif step == 2:
            # Execute CAS2 for step #2 if needed
            cas2_count_steps = self.cas2ExternalAuthenticator.getCountAuthenticationSteps(configurationAttributes)
            if cas2_count_steps == 2:
                result = self.cas2ExternalAuthenticator.prepareForStep(configurationAttributes, requestParameters, step)
            else:
                # Execute DUO for step #2 if needed
                duo_count_steps = self.duoExternalAuthenticator.getCountAuthenticationSteps(configurationAttributes)
                if duo_count_steps == 2:
                    result = self.duoExternalAuthenticator.prepareForStep(configurationAttributes, requestParameters, step)
        elif step == 3:
            # Execute DUO for step #2 if needed
            duo_count_steps = self.duoExternalAuthenticator.getCountAuthenticationSteps(configurationAttributes)
            if duo_count_steps == 2:
                result = self.duoExternalAuthenticator.prepareForStep(configurationAttributes, requestParameters, 2)

        return result

    def getExtraParametersForStep(self, configurationAttributes, step):
        cas2_result = self.cas2ExternalAuthenticator.getExtraParametersForStep(configurationAttributes, step)
        duo_result = self.duoExternalAuthenticator.getExtraParametersForStep(configurationAttributes, step)
        
        if cas2_result == None:
            return duo_result

        if duo_result == None:
            return cas2_result
        
        result_list = ArrayList()
        result_list.addAll(cas2_result)
        result_list.addAll(duo_result)

        return result_list

    def getCountAuthenticationSteps(self, configurationAttributes):
        cas2_count_steps = self.cas2ExternalAuthenticator.getCountAuthenticationSteps(configurationAttributes)
        duo_count_steps = self.duoExternalAuthenticator.getCountAuthenticationSteps(configurationAttributes)
        print "CAS2 + Duo. Get count authentication steps. cas2_count_steps = %s, duo_count_steps = %s" % (cas2_count_steps, duo_count_steps)

        if (cas2_count_steps == 1) and (duo_count_steps == 1):
            return 1

        if (cas2_count_steps == 2) and (duo_count_steps == 2):
            return 3

        return max(cas2_count_steps, duo_count_steps)

    def getPageForStep(self, configurationAttributes, step):
        result = ""

        if step == 1:
            result = self.cas2ExternalAuthenticator.getPageForStep(configurationAttributes, step)
        elif step == 2:
            cas2_count_steps = self.cas2ExternalAuthenticator.getCountAuthenticationSteps(configurationAttributes)
            if cas2_count_steps == 2:
                result = self.cas2ExternalAuthenticator.getPageForStep(configurationAttributes, step)
            else:
                result = self.duoExternalAuthenticator.getPageForStep(configurationAttributes, step)
        elif step == 3:
            result = self.duoExternalAuthenticator.getPageForStep(configurationAttributes, step)

        return result

    def getNextStep(self, configurationAttributes, requestParameters, step):
        return -1

    def getLogoutExternalUrl(self, configurationAttributes, requestParameters):
        print "Get external logout URL call"
        return None

    def logout(self, configurationAttributes, requestParameters):
        cas2_result = self.cas2ExternalAuthenticator.logout(configurationAttributes, requestParameters)
        duo_result = self.duoExternalAuthenticator.logout(configurationAttributes, requestParameters)

        return cas2_result and duo_result
