# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2016, Gluu
#
# Author: Yuriy Movchan
#

from org.gluu.service.cdi.util import CdiUtil
from org.gluu.oxauth.security import Identity
from org.gluu.model.custom.script.type.auth import PersonAuthenticationType
from org.gluu.oxauth.service import AuthenticationService
from org.gluu.persist import PersistenceEntryManager
from org.gluu.oxauth.model.ldap import TokenLdap
from org.gluu.util import StringHelper
from javax.faces.application import FacesMessage
from org.gluu.jsf2.message import FacesMessages
from org.gluu.oxauth.model.config import StaticConfiguration

import java

class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, configurationAttributes):
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
        return 1

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

    def logout(self, configurationAttributes, requestParameters):
        return True

    def isFirstSession(self, user_name):
        tokenLdap = TokenLdap()
        tokenLdap.setDn(self.staticConfiguration.getBaseDn().getClients())
        tokenLdap.setUserId(user_name)

        tokenLdapList = self.entryManager.findEntries(tokenLdap, 1)
        print "Basic (one session). isFirstSession. Get result: '%s'" % tokenLdapList

        if (tokenLdapList != None) and (tokenLdapList.size() > 0):
            print "Basic (one session). isFirstSession: False"
            return False

        print "Basic (one session). isFirstSession: True"
        return True
