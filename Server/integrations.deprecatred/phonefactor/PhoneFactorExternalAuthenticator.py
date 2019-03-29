# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2016, Gluu
#
# Author: Yuriy Movchan
#

from org.gluu.service.cdi.util import CdiUtil
from org.gluu.oxauth.security import Identity
from org.gluu.model.custom.script.type.auth import PersonAuthenticationType
from org.gluu.oxauth.service import UserService, AuthenticationService
from org.gluu.util import StringHelper, ArrayHelper
from org.gluu.oxauth.service import EncryptionService 
from net.phonefactor.pfsdk import PFAuth, PFAuthResult, SecurityException, TimeoutException, PFException
from net.phonefactor.pfsdk import PFAuthResult

import java
import string

import json

class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis
        self.pf = PFAuth()

    def init(self, configurationAttributes):
        print "PhoneFactor. Initialization"
        pf_cert_path = configurationAttributes.get("pf_cert_path").getValue2()
        pf_creds_file = configurationAttributes.get("pf_creds_file").getValue2()

        # Load credentials from file
        f = open(pf_creds_file, 'r')
        try:
            creds = json.loads(f.read())
        except:
            return False
        finally:
            f.close()

        certPassword = creds["CERT_PASSWORD"]
        try:
            encryptionService = CdiUtil.bean(EncryptionService)
            certPassword = encryptionService.decrypt(certPassword)
        except:
            return False

        self.pf.initialize(pf_cert_path, certPassword)
        print "PhoneFactor. Initialized successfully"

        return True

    def destroy(self, configurationAttributes):
        print "PhoneFactor. Destroy"
        print "PhoneFactor. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 1

    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        return True

    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        return None

    def authenticate(self, configurationAttributes, requestParameters, step):
        authenticationService = CdiUtil.bean(AuthenticationService)

        identity = CdiUtil.bean(Identity)
        credentials = identity.getCredentials()

        user_name = credentials.getUsername()
        if (step == 1):
            print "PhoneFactor. Authenticate for step 1"

            user_password = credentials.getPassword()
            logged_in = False
            if (StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password)):
                userService = CdiUtil.bean(UserService)
                logged_in = authenticationService.authenticate(user_name, user_password)

            if (not logged_in):
                return False

            return True
        elif (step == 2):
            print "PhoneFactor. Authenticate for step 2"

            passed_step1 = self.isPassedDefaultAuthentication
            if (not passed_step1):
                return False

            pf_phone_number_attr = configurationAttributes.get("pf_phone_number_attr").getValue2()

            # Get user entry from credentials
            authenticationService = CdiUtil.bean(AuthenticationService)
            credentials_user = authenticationService.getAuthenticatedUser()
            
            userService = CdiUtil.bean(UserService)
            phone_number_with_country_code_attr = userService.getCustomAttribute(credentials_user, pf_phone_number_attr)
            if (phone_number_with_country_code_attr == None):
                print "PhoneFactor. Authenticate for step 2. There is no phone number: ", user_name
                return False
            
            phone_number_with_country_code = phone_number_with_country_code_attr.getValue()
            if (phone_number_with_country_code == None):
                print "PhoneFactor. Authenticate for step 2. There is no phone number: ", user_name
                return False

            pf_country_delimiter = configurationAttributes.get("pf_country_delimiter").getValue2()
            
            phone_number_with_country_code_array = string.split(phone_number_with_country_code, pf_country_delimiter, 1)
            
            phone_number_with_country_code_array_len = len(phone_number_with_country_code_array)
            
            if (phone_number_with_country_code_array_len == 1):
                country_code = ""
                phone_number = phone_number_with_country_code_array[0]
            else:
                country_code = phone_number_with_country_code_array[0]
                phone_number = phone_number_with_country_code_array[1]

            print "PhoneFactor. Authenticate for step 2. user_name: ", user_name, ", country_code: ", country_code, ", phone_number: ", phone_number

            pf_auth_result = None
            try:
                pf_auth_result = self.pf.authenticate(user_name, country_code, phone_number, None, None, None)
            except SecurityException, err:
                print "PhoneFactor. Authenticate for step 2. BAD AUTH -- Security issue: ", err
            except TimeoutException, err:
                print "PhoneFactor. Authenticate for step 2. BAD AUTH -- Server timeout: ", err
            except PFException, err:
                print "PhoneFactor. Authenticate for step 2. BAD AUTH -- PFAuth failed with a PFException: ", err

            if (pf_auth_result == None):
                return False

            print "PhoneFactor. Authenticate for step 2. Call Status: ", pf_auth_result.getCallStatusString()
            if (pf_auth_result.getAuthenticated()):
                print "PhoneFactor. Authenticate for step 2. GOOD AUTH:", user_name
    
                if (pf_auth_result.getCallStatus() == PFAuthResult.CALL_STATUS_PIN_ENTERED):
                    print "PhoneFactor. Authenticate for step 2. I have detected that a PIN was entered"
                elif (pf_auth_result.getCallStatus() == PFAuthResult.CALL_STATUS_NO_PIN_ENTERED):
                    print "PhoneFactor. Authenticate for step 2. I have detected that NO PIN was entered"

                return True
            else:
                print "PhoneFactor. Authenticate for step 2. BAD AUTH:", user_name

                if (pf_auth_result.getCallStatus() == PFAuthResult.CALL_STATUS_USER_HUNG_UP):
                    print "PhoneFactor. Authenticate for step 2. I have detected that the user hung up"
                elif (pf_auth_result.getCallStatus() == PFAuthResult.CALL_STATUS_PHONE_BUSY):
                    print "PhoneFactor. Authenticate for step 2. I have detected that the phone was busy"
    
                if (pf_auth_result.getMessageErrorId() != 0):
                    print "PhoneFactor. Authenticate for step 2. Message Error ID: ", pf_auth_result.getMessageErrorId()
    
                    message_error = pf_auth_result.getMessageError()
                    if (message_error != null):
                        print "PhoneFactor. Authenticate for step 2. Message Error: ", message_error
    
            return False
        else:
            return False

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        if (step == 1):
            print "PhoneFactor. Prepare for step 1"

            return True
        elif (step == 2):
            print "PhoneFactor. Prepare for step 2"

            return self.isPassedDefaultAuthentication
        else:
            return False

    def getExtraParametersForStep(self, configurationAttributes, step):
        return None

    def getCountAuthenticationSteps(self, configurationAttributes):
        return 2

    def getPageForStep(self, configurationAttributes, step):
        if (step == 2):
            return "/auth/phonefactor/pflogin.xhtml"
        return ""

    def isPassedDefaultAuthentication(self):
        identity = CdiUtil.bean(Identity)
        credentials = identity.getCredentials()

        user_name = credentials.getUsername()
        passed_step1 = StringHelper.isNotEmptyString(user_name)

        return passed_step1

    def logout(self, configurationAttributes, requestParameters):
        return True
