# Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
# Copyright (c) 2020, Janssen Project
#
# Author: Yuriy Movchan
#

from io.jans.service.cdi.util import CdiUtil
from io.jans.as.server.security import Identity
from io.jans.model.custom.script.type.auth import PersonAuthenticationType
from io.jans.as.server.service import UserService, AuthenticationService
from io.jans.util import StringHelper


class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Basic (demo reset step). Initialization"
        print "Basic (demo reset step). Initialized successfully"
        return True   

    def destroy(self, configurationAttributes):
        print "Basic (demo reset step). Destroy"
        print "Basic (demo reset step). Destroyed successfully"
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

        if 1 <= step <= 3:
            print "Basic (demo reset step). Authenticate for step '%s'" % step

            identity = CdiUtil.bean(Identity)
            identity.setWorkingParameter("pass_authentication", False)

            credentials = identity.getCredentials()
            user_name = credentials.getUsername()
            user_password = credentials.getPassword()

            logged_in = False
            if (StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password)):
                userService = CdiUtil.bean(UserService)
                logged_in = authenticationService.authenticate(user_name, user_password)

            if (not logged_in):
                return False

            identity.setWorkingParameter("pass_authentication", True)
            return True
        else:
            return False

    def getNextStep(self, configurationAttributes, requestParameters, step):
        print "Basic (demo reset step). Get next step for step '%s'" % step
        identity = CdiUtil.bean(Identity)

        # If user not pass current step authenticaton redirect to current step
        pass_authentication = identity.getWorkingParameter("pass_authentication")
        if not pass_authentication:
            resultStep = step
            print "Basic (demo reset step). Get next step. Changing step to '%s'" % resultStep
            return resultStep

        return -1

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        print "Basic (demo reset step). Prepare for step '%s'" % step
        if 1 <= step <= 3:
            return True
        else:
            return False

    def getExtraParametersForStep(self, configurationAttributes, step):
        return None

    def getCountAuthenticationSteps(self, configurationAttributes):
        return 3

    def getPageForStep(self, configurationAttributes, step):
        return ""

    def getLogoutExternalUrl(self, configurationAttributes, requestParameters):
        print "Get external logout URL call"
        return None

    def logout(self, configurationAttributes, requestParameters):
        return True
