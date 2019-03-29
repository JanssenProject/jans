# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2016, Gluu
#
# Author: Yuriy Movchan
#

import java
from com.wikidsystems.client import wClient
from org.gluu.model.custom.script.type.auth import PersonAuthenticationType
from org.gluu.oxauth.security import Identity
from org.gluu.oxauth.service import UserService, AuthenticationService
from org.gluu.service.cdi.util import CdiUtil
from org.gluu.util import StringHelper, ArrayHelper


class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, configurationAttributes):
        print "Wikid. Initialization"

        if (not (configurationAttributes.containsKey("wikid_server_host") and configurationAttributes.containsKey("wikid_server_port"))):
            print "Wikid Initialization. The properties wikid_server_host and wikid_server_port should be not empty"
            return False

        if (not (configurationAttributes.containsKey("wikid_cert_path") and configurationAttributes.containsKey("wikid_cert_pass"))):
            print "Wikid Initialization. The properties wikid_cert_path and wikid_cert_pass should be not empty"
            return False

        if (not (configurationAttributes.containsKey("wikid_ca_store_path") and configurationAttributes.containsKey("wikid_ca_store_pass"))):
            print "Wikid Initialization. The properties wikid_ca_store_path and wikid_ca_store_pass should be not empty"
            return False

        if (not configurationAttributes.containsKey("wikid_server_code")):
            print "Wikid Initialization. The property wikid_server_code should be not empty"
            return False

        print "Wikid. Initialization. Creating new client."
        wikid_server_host = configurationAttributes.get("wikid_server_host").getValue2()
        wikid_server_port = int(configurationAttributes.get("wikid_server_port").getValue2())

        wikid_cert_path = configurationAttributes.get("wikid_cert_path").getValue2()
        wikid_cert_pass = configurationAttributes.get("wikid_cert_pass").getValue2()
        wikid_ca_store_path = configurationAttributes.get("wikid_ca_store_path").getValue2()
        wikid_ca_store_pass = configurationAttributes.get("wikid_ca_store_pass").getValue2()

        self.wc = wClient(wikid_server_host, wikid_server_port, wikid_cert_path, wikid_cert_pass, wikid_ca_store_path, wikid_ca_store_pass)

        print "Wikid. Initialized successfully"
        return True   

    def destroy(self, configurationAttributes):
        print "Wikid. Destroy"
        print "Wikid. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 1

    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        return True

    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        return None

    def authenticate(self, configurationAttributes, requestParameters, step):
        print "Wikid. Authentication. Checking client"

        if (not self.wc.isConnected()):
            print "Wikid. Authentication. Wikid client state is invalid"
            return False

        authenticationService = CdiUtil.bean(AuthenticationService)

        identity = CdiUtil.bean(Identity)
        credentials = identity.getCredentials()

        is_wikid_registration = False
        sessionAttributes = identity.getSessionId().getSessionAttributes()
        if (sessionAttributes != None) and sessionAttributes.containsKey("wikid_registration"):
            is_wikid_registration = java.lang.Boolean.valueOf(sessionAttributes.get("wikid_registration"))

        wikid_server_code = configurationAttributes.get("wikid_server_code").getValue2()

        user_name = credentials.getUsername()

        if (step == 1):
            print "Wikid. Authenticate for step 1"

            user_password = credentials.getPassword()

            logged_in = False
            if (StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password)):
                userService = CdiUtil.bean(UserService)
                logged_in = authenticationService.authenticate(user_name, user_password)

            if (not logged_in):
                return False

            print "Wikid. Authenticate for step 1. Attempting to find wikid_user: " + user_name
            wc_user = self.wc.findUser(wikid_server_code, user_name)

            if (wc_user == None):
                print "Wikid. Authenticate for step 1. There is no associated devices for user: " +  user_name
                print "Wikid. Authenticate for step 1. Setting count steps to 3"
                identity.setWorkingParameter("wikid_count_login_steps", 3)
                identity.setWorkingParameter("wikid_registration", True)
            else:
                identity.setWorkingParameter("wikid_count_login_steps", 2)

            return True
        elif (is_wikid_registration):
            print "Wikid. Authenticate for step wikid_register_device"

            userService = CdiUtil.bean(UserService)

            wikid_regcode_array = requestParameters.get("regcode")
            if ArrayHelper.isEmpty(wikid_regcode_array):
                print "Wikid. Authenticate for step wikid_register_device. Regcode is empty"
                return False

            wikid_regcode = wikid_regcode_array[0]

            print "Wikid. Authenticate for step wikid_register_device. User: " + user_name + ", regcode: " + wikid_regcode 
            
            register_result = self.wc.registerUsername(user_name, wikid_regcode, wikid_server_code)

            is_valid = register_result == 0
            if is_valid:
                print "Wikid. Authenticate for step wikid_register_device. User: " + user_name + " token registered successfully"

                # Add wikid_regcode to user UIDs
                find_user_by_uid = userService.addUserAttribute(user_name, "oxExternalUid", "wikid:" + wikid_regcode)
                if (find_user_by_uid == None):
                    print "Wikid. Authenticate for step wikid_register_device. Failed to update user: " + user_name
                    is_valid = False
                else:
                    identity.setWorkingParameter("wikid_registration", False)
            else:
                print "Wikid. Authenticate for step wikid_register_device. Failed to register user: " + user_name + " token:" + wikid_regcode + ". Registration result:", register_result

            return is_valid
        elif (not is_wikid_registration):
            print "Wikid. Authenticate for step wikid_check_passcode"

            wikid_passcode_array = requestParameters.get("passcode")
            if ArrayHelper.isEmpty(wikid_passcode_array):
                print "Wikid. Authenticate for step wikid_check_passcode. Passcode is empty"
                return False

            wikid_passcode = wikid_passcode_array[0]

            print "Wikid. Authenticate for step wikid_check_passcode. wikid_user: " + user_name
            
            is_valid = self.wc.CheckCredentials(user_name, wikid_passcode, wikid_server_code)

            if is_valid:
                print "Wikid. Authenticate for step wikid_check_passcode. wikid_user: " + user_name + " authenticated successfully"
            else:
                print "Wikid. Authenticate for step wikid_check_passcode. Failed to authenticate. wikid_user: " + user_name

            return is_valid
        else:
            return False

    def getExtraParametersForStep(self, configurationAttributes, step):
        if (step == 2):
            return Arrays.asList("wikid_registration", "wikid_count_login_steps")
        elif (step == 3):
            return Arrays.asList("wikid_registration")

        return None

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        if (step == 1):
            print "Wikid. Prepare for step 1"

            return True
        elif (step == 2):
            print "Wikid. Prepare for step 2"

            return True
        elif (step == 2):
            print "Wikid. Prepare for step 3"

            return True
        else:
            return False

    def getCountAuthenticationSteps(self, configurationAttributes):
        identity = CdiUtil.bean(Identity)
        
        sessionAttributes = identity.getSessionId().getSessionAttributes()
        if (sessionAttributes != None) and sessionAttributes.containsKey("wikid_count_login_steps"):
            return java.lang.Integer.valueOf(sessionAttributes.get("wikid_count_login_steps"))

        return 2

    def getPageForStep(self, configurationAttributes, step):
        identity = CdiUtil.bean(Identity)

        is_wikid_registration = False
        if (identity.isSetWorkingParameter("wikid_registration")):
            is_wikid_registration = identity.getWorkingParameter("wikid_registration")

        if (step == 2):
            if (is_wikid_registration):
                return "/auth/wikid/wikidregister.xhtml"
            else:
                return "/auth/wikid/wikidlogin.xhtml"
        if (step == 3):
            return "/auth/wikid/wikidlogin.xhtml"

        return ""

    def logout(self, configurationAttributes, requestParameters):
        return True
