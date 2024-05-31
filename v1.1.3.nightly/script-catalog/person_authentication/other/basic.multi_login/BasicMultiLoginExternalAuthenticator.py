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

import java

class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Basic (multi login). Initialization"

        login_attributes_list_object = configurationAttributes.get("login_attributes_list")
        if (login_attributes_list_object == None):
            print "Basic (multi login). Initialization. There is no property login_attributes_list"
            return False

        login_attributes_list = login_attributes_list_object.getValue2()
        if (StringHelper.isEmpty(login_attributes_list)):
            print "Basic (multi login). Initialization. There is no attributes specified in login_attributes property"
            return False
        
        login_attributes_list_array = StringHelper.split(login_attributes_list, ",")
        if (ArrayHelper.isEmpty(login_attributes_list_array)):
            print "Basic (multi login). Initialization. There is no attributes specified in login_attributes property"
            return False

        if (configurationAttributes.containsKey("local_login_attributes_list")):
            local_login_attributes_list = configurationAttributes.get("local_login_attributes_list").getValue2()
            local_login_attributes_list_array = StringHelper.split(local_login_attributes_list, ",")
        else:
            print "Basic (multi login). Initialization. There is no property local_login_attributes_list. Assuming that login attributes are equal to local login attributes."
            local_login_attributes_list_array = login_attributes_list_array

        if (len(login_attributes_list_array) != len(local_login_attributes_list_array)):
            print "Basic (multi login). Initialization. The number of attributes in login_attributes_list and local_login_attributes_list isn't equal"
            return False
        
        self.login_attributes_list_array = login_attributes_list_array
        self.local_login_attributes_list_array = local_login_attributes_list_array

        print "Basic (multi login). Initialized successfully"
        return True   

    def destroy(self, configurationAttributes):
        print "Basic (multi login). Destroy"
        print "Basic (multi login). Destroyed successfully"
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

        if (step == 1):
            print "Basic (multi login). Authenticate for step 1"

            identity = CdiUtil.bean(Identity)
            credentials = identity.getCredentials()
            key_value = credentials.getUsername()
            user_password = credentials.getPassword()

            logged_in = False
            if (StringHelper.isNotEmptyString(key_value) and StringHelper.isNotEmptyString(user_password)):
                i = 0
                count = len(self.login_attributes_list_array)
                while (i < count):
                    primary_key = self.login_attributes_list_array[i]
                    local_primary_key = self.local_login_attributes_list_array[i]
                    logged_in = authenticationService.authenticate(key_value, user_password, primary_key, local_primary_key)
                    if (logged_in):
                        return True
                    i += 1

            return False
        else:
            return False

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        if (step == 1):
            print "Basic (multi login). Prepare for Step 1"
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
