# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2019, Gluu
#
# Author: Yuriy Movchan
#

from org.gluu.service.cdi.util import CdiUtil
from org.gluu.oxauth.security import Identity
from org.gluu.model.custom.script.type.auth import PersonAuthenticationType
from org.gluu.oxauth.service import AuthenticationService
from org.gluu.util import StringHelper

import java

class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, configurationAttributes):
        print "Basic (with external logout). Initialization"
        print "Basic (with external logout). Initialized successfully"
        return True   

    def destroy(self, configurationAttributes):
        print "Basic (with external logout). Destroy"
        print "Basic (with external logout). Destroyed successfully"
        return True

    def getApiVersion(self):
        return 3

    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        return True

    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        return None

    def authenticate(self, configurationAttributes, requestParameters, step):
        authenticationService = CdiUtil.bean(AuthenticationService)

        if (step == 1):
            print "Basic (with external logout). Authenticate for step 1"

            identity = CdiUtil.bean(Identity)
            credentials = identity.getCredentials()

            user_name = credentials.getUsername()
            user_password = credentials.getPassword()

            logged_in = False
            if (StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password)):
                logged_in = authenticationService.authenticate(user_name, user_password)

            if (not logged_in):
                return False

            return True
        else:
            return False

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        if (step == 1):
            print "Basic (with external logout). Prepare for Step 1"
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
        print "Basic (with external logout). Get external logout URL call"
        return "https://www.dummy.org/app/logout.htm"

    # In order to get this method call RP should end_session request to https://<server>/oxauth/logout.htm enpoint
    # instead of https://<server>/oxauth/restv1/end_session endpoint
    def logout(self, configurationAttributes, requestParameters):
        print "Basic (with external logout). Logout call"
        return True
