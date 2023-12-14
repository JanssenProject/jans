# Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
# Copyright (c) 2020, Janssen Project
#
# Author: Yuriy Movchan
#

from io.jans.service.cdi.util import CdiUtil
from io.jans.as.server.security import Identity
from io.jans.model.custom.script.type.auth import PersonAuthenticationType
from io.jans.as.server.service import UserService, AuthenticationService
from io.jans.util import StringHelper, ArrayHelper
from io.jans.as.server.util import ServerUtil

import java

class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Basic (with password update). Initialization"
        print "Basic (with password update). Initialized successfully"
        return True   

    def destroy(self, configurationAttributes):
        print "Basic (with password update). Destroy"
        print "Basic (with password update). Destroyed successfully"
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
        userService = CdiUtil.bean(UserService)
        authenticationService = CdiUtil.bean(AuthenticationService)

        identity = CdiUtil.bean(Identity)
        credentials = identity.getCredentials()
        user_name = credentials.getUsername()

        if (step == 1):
            print "Basic (with password update). Authenticate for step 1"

            user_password = credentials.getPassword()

            logged_in = False
            if (StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password)):
                logged_in = authenticationService.authenticate(user_name, user_password)

            if (not logged_in):
                return False

            return True
        elif (step == 2):
            print "Basic (with password update). Authenticate for step 2"
            user = authenticationService.getAuthenticatedUser()
            if user == None:
                print "Basic (with password update). Authenticate for step 2. Failed to determine user name"
                return False

            user_name = user.getUserId()
            find_user_by_uid = userService.getUser(user_name)

            update_button = requestParameters.get("loginForm:updateButton")

            if ArrayHelper.isEmpty(update_button):
                return True

            new_password_array = requestParameters.get("new_password")
            if ArrayHelper.isEmpty(new_password_array) or StringHelper.isEmpty(new_password_array[0]):
                print "Basic (with password update). Authenticate for step 2. New password is empty"
                return False

            new_password = new_password_array[0]
            find_user_by_uid.setAttribute("userPassword", new_password)
            print "Basic (with password update). Authenticate for step 2. Attempting to set new user '%s' password" % user_name

            userService.updateUser(find_user_by_uid)
            print "Basic (with password update). Authenticate for step 2. Password updated successfully"

            return True
        else:
            return False

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        if (step == 1):
            print "Basic (with password update). Prepare for Step 1"
            return True
        elif (step == 2):
            print "Basic (with password update). Prepare for Step 2"
            return True
        else:
            return False

    def getExtraParametersForStep(self, configurationAttributes, step):
        return None

    def getCountAuthenticationSteps(self, configurationAttributes):
        return 2

    def getPageForStep(self, configurationAttributes, step):
        if (step == 2):
            return "/auth/pwd/newpassword.xhtml"

        return ""
        
    def getNextStep(self, configurationAttributes, requestParameters, step):
        return -1

    def getLogoutExternalUrl(self, configurationAttributes, requestParameters):
        print "Get external logout URL call"
        return None
        
    def logout(self, configurationAttributes, requestParameters):
        return True
