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
        return 1

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
        cas2_result = self.cas2ExternalAuthenticator.authenticate(configurationAttributes, requestParameters, step)

        return cas2_result

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        cas2_result = self.cas2ExternalAuthenticator.prepareForStep(configurationAttributes, requestParameters, step)

        return cas2_result

    def getExtraParametersForStep(self, configurationAttributes, step):
        cas2_result = self.cas2ExternalAuthenticator.getExtraParametersForStep(configurationAttributes, step)

        return cas2_result

    def getCountAuthenticationSteps(self, configurationAttributes):
        cas2_result = self.cas2ExternalAuthenticator.getCountAuthenticationSteps(configurationAttributes)

        return cas2_result

    def getPageForStep(self, configurationAttributes, step):
        cas2_result = self.cas2ExternalAuthenticator.getPageForSteps(configurationAttributes, step)

        return cas2_result

    def logout(self, configurationAttributes, requestParameters):
        cas2_result = self.cas2ExternalAuthenticator.logout(configurationAttributes, requestParameters)

        return cas2_result
