# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2016, Gluu
#
# Author: Yuriy Movchan
#

from org.gluu.service.cdi.util import CdiUtil
from org.gluu.oxauth.security import Identity
from org.gluu.model.custom.script.type.auth import PersonAuthenticationType
from org.gluu.oxauth.service import UserService, AuthenticationService
from org.gluu.util import StringHelper


class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, configurationAttributes):
        print "Basic (demo reset step). Initialization"
        print "Basic (demo reset step). Initialized successfully"
        return True   

    def destroy(self, configurationAttributes):
        print "Basic (demo reset step). Destroy"
        print "Basic (demo reset step). Destroyed successfully"
        return True

    def getApiVersion(self):
        return 2

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

        # If user not pass current step authenticaton change step to previous
        pass_authentication = identity.getWorkingParameter("pass_authentication")
        if not pass_authentication:
            if step > 1:
                resultStep = step - 1
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

    def logout(self, configurationAttributes, requestParameters):
        return True
