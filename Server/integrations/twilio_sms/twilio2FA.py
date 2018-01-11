# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2016, Gluu
#
# Author: Michael Schwartz
#

import random

import com.twilio.sdk.TwilioRestClient as TwilioRestClient
import java.util.ArrayList as ArrayList
import org.apache.http.message.BasicNameValuePair as BasicNameValuePair
from java.util import Arrays
from org.xdi.model.custom.script.type.auth import PersonAuthenticationType
from org.xdi.oxauth.security import Identity
from org.xdi.oxauth.service import UserService, AuthenticationService
from org.xdi.oxauth.util import ServerUtil
from org.xdi.service.cdi.util import CdiUtil
from org.xdi.util import StringHelper


class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, configurationAttributes):
        print "Twilio SMS. Initialization"

        self.ACCOUNT_SID = None    
        self.AUTH_TOKEN = None    
        self.FROM_NUMBER = None
        
        # Get Custom Properties
        try:
            self.ACCOUNT_SID = configurationAttributes.get("twilio_sid").getValue2()
        except:
            print 'TwilioSMS, Missing required configuration attribute "twilio_sid"'

        try:
            self.AUTH_TOKEN = configurationAttributes.get("twilio_token").getValue2()
        except:
            print'TwilioSMS, Missing required configuration attribute "twilio_token"'
        try:
            self.FROM_NUMBER = configurationAttributes.get("from_number").getValue2()
        except:
            print'TwilioSMS, Missing required configuration attribute "from_number"'

        if None in (self.ACCOUNT_SID, self.AUTH_TOKEN, self.FROM_NUMBER):
            print "twilio_sid, twilio_token, from_number is empty ... returning False"
            return False

        print "Twilio SMS. Initialized successfully"

        return True    

    def destroy(self, configurationAttributes):
        print "Twilio SMS. Destroy"
        print "Twilio SMS. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 1

    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        return True

    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        return None

    def authenticate(self, configurationAttributes, requestParameters, step):
        userService = CdiUtil.bean(UserService)
        authenticationService = CdiUtil.bean(AuthenticationService)

        identity = CdiUtil.bean(Identity)
        session_attributes = identity.getSessionId().getSessionAttributes()

        form_passcode = ServerUtil.getFirstValue(requestParameters, "passcode")
        form_name = ServerUtil.getFirstValue(requestParameters, "TwilioSmsloginForm")

        print "TwilioSMS. form_response_passcode: %s" % str(form_passcode)

        if step == 1:
            print "TwilioSMS. Step 1 Password Authentication"
            identity = CdiUtil.bean(Identity)
            credentials = identity.getCredentials()

            user_name = credentials.getUsername()
            user_password = credentials.getPassword()
            
            logged_in = False
            if StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password):
                logged_in = authenticationService.authenticate(user_name, user_password)

            if not logged_in:
                return False

            # Get the Person's number and generate a code
            foundUser = None
            try:
                foundUser = userService.getUserByAttribute("uid", user_name)
            except:
                print 'TwilioSMS, Error retrieving user %s from LDAP' % (user_name)
                return False

            try:
                mobile_number = foundUser.getAttribute("phoneNumberVerified")
            except:
                print 'TwilioSMS, Error finding mobile number for' % (user_name) 
                return False
                        
            # Generate Random six digit code and store it in array
            code = random.randint(100000, 999999)
            
            # Get code and save it in LDAP temporarily with special session entry 
            identity.setWorkingParameter("code", code)

            # Store user phone number in authentication session 
            identity.setWorkingParameter("mobile_number", mobile_number)

            client = TwilioRestClient(self.ACCOUNT_SID, self.AUTH_TOKEN)
            bodyParam = BasicNameValuePair("Body", str(code))
            toParam = BasicNameValuePair("To", mobile_number)
            fromParam = BasicNameValuePair("From", self.FROM_NUMBER)

            params = ArrayList()
            params.add(bodyParam)
            params.add(toParam)
            params.add(fromParam)

            try:
                messageFactory = client.getAccount().getMessageFactory()
                message = messageFactory.create(params)

                print 'TwilioSMs, Message Sid: %s' % (message.getSid())
                return True
            except Exception, ex:
                print "TwilioSMS. Error sending message to Twilio"
                print "TwilioSMS. Unexpected error:", ex

            return False
        elif step == 2:
            # Retrieve the session attribute
            print "TwilioSMS. Step 2 SMS/OTP Authentication"
            code = session_attributes.get("code")
            print "TwilioSMS. Code: %s" % str(code)
    
            if code is None:
                print "TwilioSMS. Failed to find previously sent code"
                return False 
    
            if form_passcode is None:
                print "TwilioSMS. Passcode is empty"
                return False 
    
            if len(form_passcode) != 6:
                print "TwilioSMS. Passcode from response is not 6 digits: %s" % form_passcode
                return False

            if form_passcode == code:
                print "TiwlioSMS, SUCCESS! User entered the same code!"
                return True

            print "TwilioSMS. FAIL! User entered the wrong code! %s != %s" % (form_passcode, code)

            return False            

        print "TwilioSMS. ERROR: step param not found or != (1|2)"

        return False

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        if step == 1:
            print "TwilioSMS. Prepare for Step 1"
            return True
        elif step == 2:
            print "TwilioSMS. Prepare for Step 2"
            return True

        return False

    def getExtraParametersForStep(self, configurationAttributes, step):
        return Arrays.asList("code", "mobile_number")

    def getCountAuthenticationSteps(self, configurationAttributes):
        return 2

    def getPageForStep(self, configurationAttributes, step):
        if step == 2:
            return "/auth/twiliosms/twiliosms.xhtml"

        return ""

    def logout(self, configurationAttributes, requestParameters):
        return True
