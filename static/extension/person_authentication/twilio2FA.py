# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2018, Gluu
#
# Author: Jose Gonzalez
# Author: Gasmyr Mougang

from org.xdi.service.cdi.util import CdiUtil
from org.xdi.oxauth.security import Identity
from org.xdi.model.custom.script.type.auth import PersonAuthenticationType
from org.xdi.oxauth.service import UserService, AuthenticationService
from org.xdi.oxauth.util import ServerUtil
from org.xdi.util import StringHelper, ArrayHelper
from java.util import Arrays
from javax.faces.application import FacesMessage
from org.gluu.jsf2.message import FacesMessages

import com.twilio.Twilio as Twilio
import com.twilio.rest.api.v2010.account.Message as Message
import com.twilio.type.PhoneNumber as PhoneNumber
import org.codehaus.jettison.json.JSONArray as JSONArray


import java
import random
import jarray

class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis
        self.mobile_number = None
        self.identity = CdiUtil.bean(Identity)

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
        session_attributes = self.identity.getSessionId().getSessionAttributes()
        form_passcode = ServerUtil.getFirstValue(requestParameters, "passcode")
        form_name = ServerUtil.getFirstValue(requestParameters, "TwilioSmsloginForm")

        print "TwilioSMS. form_response_passcode: %s" % str(form_passcode)

        if step == 1:
            print "TwilioSMS. Step 1 Password Authentication"
            credentials = self.identity.getCredentials()

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
                foundUser = authenticationService.getAuthenticatedUser()
            except:
                print 'TwilioSMS, Error retrieving user %s from LDAP' % (user_name)
                return False

            try:
                isVerified = foundUser.getAttribute("phoneNumberVerified")
                if isVerified:
                    self.mobile_number = foundUser.getAttribute("employeeNumber")
                if  self.mobile_number == None:
                    self.mobile_number = foundUser.getAttribute("mobile")
                if  self.mobile_number == None:
                    self.mobile_number = foundUser.getAttribute("telephoneNumber")
                if  self.mobile_number == None:
                    print "TwilioSMS, Error finding mobile number for user '%'" % user_name    
                    
            except:
                print 'TwilioSMS, Error finding mobile number for' % (user_name)
                return False

            # Generate Random six digit code and store it in array
            code = random.randint(100000, 999999)

            # Get code and save it in LDAP temporarily with special session entry
            self.identity.setWorkingParameter("code", code)

            try:
                Twilio.init(self.ACCOUNT_SID, self.AUTH_TOKEN);
                message = Message.creator(PhoneNumber(self.mobile_number), PhoneNumber(self.FROM_NUMBER), str(code)).create();
                print "++++++++++++++++++++++++++++++++++++++++++++++"
                print 'TwilioSMs, Message Sid: %s' % (message.getSid())
                print 'TwilioSMs, User phone: %s' % (self.mobile_number)
                print "++++++++++++++++++++++++++++++++++++++++++++++"
                self.identity.setWorkingParameter("mobile_number", self.mobile_number)
                self.identity.getSessionId().getSessionAttributes().put("mobile_number",self.mobile_number)
                self.identity.setWorkingParameter("mobile", self.mobile_number)
                self.identity.getSessionId().getSessionAttributes().put("mobile",self.mobile_number)
                print "++++++++++++++++++++++++++++++++++++++++++++++"
                print "Number: %s" % (self.identity.getWorkingParameter("mobile_number"))
                print "Mobile: %s" % (self.identity.getWorkingParameter("mobile"))
                print "++++++++++++++++++++++++++++++++++++++++++++++"
                return True
            except Exception, ex:
                print "TwilioSMS. Error sending message to Twilio"
                print "TwilioSMS. Unexpected error:", ex

            return False
        elif step == 2:

            facesMessages = CdiUtil.bean(FacesMessages)
            facesMessages.setKeepMessages()
            # Retrieve the session attribute
            print "TwilioSMS. Step 2 SMS/OTP Authentication"
            code = session_attributes.get("code")
            print "----------------------------------"
            print "TwilioSMS. Code: %s" % str(code)
            print "----------------------------------"

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

            print "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++" 
            print "TwilioSMS. FAIL! User entered the wrong code! %s != %s" % (form_passcode, code)
            print "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++" 
            facesMessages.add(facesMessage.SEVERITY_ERROR, "Incorrect Twilio code, please try again.")

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
        if step == 2:
            return Arrays.asList("code")

        return None

    def getCountAuthenticationSteps(self, configurationAttributes):
        return 2

    def getPageForStep(self, configurationAttributes, step):
        if step == 2:
            return "/auth/twiliosms/twiliosms.xhtml"

        return ""

    def logout(self, configurationAttributes, requestParameters):
        return True
