# Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
# Copyright (c) 2020, Janssen Project
#
# Author: Yuriy Movchan
#

from io.jans.service.cdi.util import CdiUtil
from io.jans.as.server.security import Identity
from io.jans.model.custom.script.type.auth import PersonAuthenticationType
from io.jans.as.server.service import AuthenticationService
from io.jans.orm import PersistenceEntryManager
from org.gluu.oxauth.model.ldap import TokenEntity
from io.jans.util import StringHelper
from jakarta.faces.application import FacesMessage
from io.jans.jsf2.message import FacesMessages
from org.gluu.oxauth.model.config import StaticConfiguration

import java

class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Basic (one session). Initialization"
        self.entryManager = CdiUtil.bean(PersistenceEntryManager)
        self.staticConfiguration = CdiUtil.bean(StaticConfiguration)

        print "Basic (one session). Initialized successfully"
        return True   

    def destroy(self, configurationAttributes):
        print "Basic (one session). Destroy"
        print "Basic (one session). Destroyed successfully"
        return True

    def getApiVersion(self):
        return 11

    def getAuthenticationMethodClaims(self, requestParameters):
        return None

    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        return True

    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        return None

    def authenticate(self, configurationAttributes, requestParameters, step):
        authenticationService = CdiUtil.bean(AuthenticationService)

        if step == 1:
            print "Basic (one session). Authenticate for step 1"

            identity = CdiUtil.bean(Identity)
            credentials = identity.getCredentials()

            user_name = credentials.getUsername()
            user_password = credentials.getPassword()

            logged_in = False
            if StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password):
                logged_in = authenticationService.authenticate(user_name, user_password)

            if not logged_in:
                return False

            logged_in = self.isFirstSession(user_name)
            if not logged_in:
                facesMessages = CdiUtil.bean(FacesMessages)
                facesMessages.add(FacesMessage.SEVERITY_ERROR, "Please, end active session first!")
                return False
	

            return True
        else:
            return False

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        if step == 1:
            print "Basic (one session). Prepare for Step 1"
            return True
        else:
            return False

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

    def isFirstSession(self, user_name):
        tokenEntity = TokenEntity()
        tokenEntity.setDn(self.staticConfiguration.getBaseDn().getClients())
        tokenEntity.setUserId(user_name)

        tokenEntityList = self.entryManager.findEntries(tokenEntity, 1)
        print "Basic (one session). isFirstSession. Get result: '%s'" % tokenEntityList

        if (tokenEntityList != None) and (tokenEntityList.size() > 0):
            print "Basic (one session). isFirstSession: False"
            return False

        print "Basic (one session). isFirstSession: True"
        return True
