# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2018, Gluu
#
# Author: Yuriy Movchan
#

from org.xdi.service.cdi.util import CdiUtil
from org.xdi.oxauth.security import Identity
from org.xdi.model.custom.script.type.auth import PersonAuthenticationType
from org.xdi.oxauth.service import UserService, AuthenticationService
from org.xdi.service import MailService
from org.xdi.util import ArrayHelper
from org.xdi.util import StringHelper

import duo_web
import json
from Cas2ExternalAuthenticator import PersonAuthentication as Cas2ExternalAuthenticator
from DuoExternalAuthenticator import PersonAuthentication as DuoExternalAuthenticator

class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

        self.cas2ExternalAuthenticator = Cas2ExternalAuthenticator(currentTimeMillis)
        self.duoExternalAuthenticator = DuoExternalAuthenticator(currentTimeMillis)

    def init(self, configurationAttributes):
        print "CAS2 + Duo. Initialization"
        
        result = self.cas2ExternalAuthenticator.init(configurationAttributes)
        result = result and self.duoExternalAuthenticator.init(configurationAttributes)

        print "CAS2 + Duo. Initialized successfully"

        return result

    def destroy(self, configurationAttributes):
        print "CAS2 + Duo. Destroy"

        result = self.cas2ExternalAuthenticator.destroy(configurationAttributes)
        result = result and self.duoExternalAuthenticator.destroy(configurationAttributes)

        print "CAS2 + Duo. Destroyed successfully"

        return result

    def getApiVersion(self):
        return 1

    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        result = self.cas2ExternalAuthenticator.isValidAuthenticationMethod(usageType, configurationAttributes)
        result = result and self.duoExternalAuthenticator.isValidAuthenticationMethod(usageType, configurationAttributes)

        return result

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

        identity = CdiUtil.bean(Identity)

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
                    result = elf.duoExternalAuthenticator.authenticate(configurationAttributes, requestParameters, step)
        elif step == 3:
            # Execute DUO for step #2 if needed
            duo_count_steps = self.duoExternalAuthenticator.getCountAuthenticationSteps(configurationAttributes)
            if duo_count_steps == 2:
                result = self.duoExternalAuthenticator.authenticate(configurationAttributes, requestParameters, 2)

        return cas2_result

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        result = False

        identity = CdiUtil.bean(Identity)

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

        return ArrayHelper.arrayMerge(cas2_result, duo_result)

    def getCountAuthenticationSteps(self, configurationAttributes):
        default_count_steps = 2
        cas2_count_steps = self.cas2ExternalAuthenticator.getCountAuthenticationSteps(configurationAttributes)
        duo_count_steps = self.duoExternalAuthenticator.getCountAuthenticationSteps(configurationAttributes)
        if (cas2_count_steps == 2) and (duo_count_steps == 2):
            default_count_steps = 3

        return max(default_count_steps, cas2_count_steps, duo_count_steps)

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

    def logout(self, configurationAttributes, requestParameters):
        cas2_result = self.cas2ExternalAuthenticator.logout(configurationAttributes, requestParameters)
        duo_result = self.duoExternalAuthenticator.logout(configurationAttributes, requestParameters)

        return cas2_result and duo_result
