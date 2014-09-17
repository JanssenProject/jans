from org.jboss.seam.security import Identity
from org.xdi.oxauth.service.python.interfaces import ExternalAuthenticatorType
from org.xdi.oxauth.service import UserService, AuthenticationService
from org.xdi.util import StringHelper
from org.xdi.util import ArrayHelper

import java

class ExternalAuthenticator(ExternalAuthenticatorType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, configurationAttributes):
        print "Basic (multi login) initialization"

        login_attributes_list_object = configurationAttributes.get("login_attributes_list")
        if (login_attributes_list_object == None):
            print "Basic (multi login) initialization. There is no property login_attributes_list"
            return False

        login_attributes_list = login_attributes_list_object.getValue2()
        if (StringHelper.isEmpty(login_attributes_list)):
            print "Basic (multi login) initialization. There is no attributes specified in login_attributes property"
            return False
        
        login_attributes_list_array = StringHelper.split(login_attributes_list, ",")
        if (ArrayHelper.isEmpty(login_attributes_list_array)):
            print "Basic (multi login) initialization. There is no attributes specified in login_attributes property"
            return False

        if (configurationAttributes.containsKey("local_login_attributes_list")):
            local_login_attributes_list = configurationAttributes.get("local_login_attributes_list").getValue2()
            local_login_attributes_list_array = StringHelper.split(local_login_attributes_list, ",")
        else:
            print "Basic (multi login) initialization. There is no property local_login_attributes_list. Assuming that login attributes are equal to local login attributes."
            local_login_attributes_list_array = login_attributes_list_array

        if (len(login_attributes_list_array) != len(local_login_attributes_list_array)):
            print "Basic (multi login) initialization. The number of attributes in login_attributes_list and local_login_attributes_list isn't equal"
            return False
        
        self.login_attributes_list_array = login_attributes_list_array
        self.local_login_attributes_list_array = local_login_attributes_list_array

        print "Basic (multi login) initialized successfully"
        return True   

    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        return True

    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        return None

    def authenticate(self, configurationAttributes, requestParameters, step):
        if (step == 1):
            print "Basic (multi login) authenticate for step 1"

            credentials = Identity.instance().getCredentials()
            key_value = credentials.getUsername()
            user_password = credentials.getPassword()

            logged_in = False
            if (StringHelper.isNotEmptyString(key_value) and StringHelper.isNotEmptyString(user_password)):
                userService = UserService.instance()

                i = 0;
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
            print "Basic (multi login) prepare for Step 1"
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

    def getApiVersion(self):
        return 3
