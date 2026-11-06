# Janssen Project software is available under the Apache 2.0 License (2004). See http://www.apache.org/licenses/ for full text.
# Copyright (c) 2020, Janssen Project
#
# Author: Gasmyr Mougang

from io.jans.service.cdi.util import CdiUtil
from io.jans.as.server.security import Identity
from io.jans.model.custom.script.type.auth import PersonAuthenticationType
from io.jans.as.server.service import AuthenticationService
from io.jans.as.server.service import SessionIdService
from io.jans.as.server.util import ServerUtil
from io.jans.util import StringHelper
from java.util import Arrays
from jakarta.faces.application import FacesMessage
from io.jans.jsf2.message import FacesMessages

import com.twilio.Twilio as Twilio
import com.twilio.rest.api.v2010.account.Message as Message
import com.twilio.type.PhoneNumber as PhoneNumber


from java.lang import Throwable
from java.security import SecureRandom

class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis
        self.mobile_number = None
        self.identity = CdiUtil.bean(Identity)

    def init(self, customScript, configurationAttributes):
        print "=============================================="
        print "===TWILIO SMS INITIALIZATION=================="
        print "=============================================="
        self.ACCOUNT_SID = None
        self.AUTH_TOKEN = None
        self.FROM_NUMBER = None

        # Get Custom Properties
        try:
            self.ACCOUNT_SID = configurationAttributes.get("twilio_sid").getValue2()
        except (Exception, Throwable):
            print 'TwilioSMS, Missing required configuration attribute "twilio_sid"'

        try:
            self.AUTH_TOKEN = configurationAttributes.get("twilio_token").getValue2()
        except (Exception, Throwable):
            print'TwilioSMS, Missing required configuration attribute "twilio_token"'
        try:
            self.FROM_NUMBER = configurationAttributes.get("from_number").getValue2()
        except (Exception, Throwable):
            print'TwilioSMS, Missing required configuration attribute "from_number"'

        if None in (self.ACCOUNT_SID, self.AUTH_TOKEN, self.FROM_NUMBER):
            print "twilio_sid, twilio_token, from_number is empty ... returning False"
            return False

        print "===TWILIO SMS INITIALIZATION DONE PROPERLY====="  
        return True

    def destroy(self, configurationAttributes):
        print "Twilio SMS. Destroy"
        print "Twilio SMS. Destroyed successfully"
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
        print "=============================================="
        print "====TWILIO SMS AUTHENCATION==================="
        print "=============================================="
        authenticationService = CdiUtil.bean(AuthenticationService)
        sessionIdService = CdiUtil.bean(SessionIdService)
        facesMessages = CdiUtil.bean(FacesMessages)
        facesMessages.setKeepMessages()

        form_passcode = ServerUtil.getFirstValue(requestParameters, "passcode")

        print "TwilioSMS. form_response_passcode received"

        if step == 1:
            print "=============================================="
            print "=TWILIO SMS STEP 1 | Password Authentication=="
            print "=============================================="
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
            except (Exception, Throwable):
                print 'TwilioSMS, Error retrieving user %s from LDAP' % (user_name)
                return False

            # per-request: the script instance is shared across users
            mobile_number = None
            try:
                isVerified = foundUser.getAttribute("phoneNumberVerified")
                if isVerified:
                    mobile_number = foundUser.getAttribute("employeeNumber")
                if not mobile_number:
                    mobile_number = foundUser.getAttribute("mobile")
                if not mobile_number:
                    mobile_number = foundUser.getAttribute("telephoneNumber")
                if not mobile_number:
                    print "TwilioSMS, Error finding mobile number for user '%s'" % user_name    
                    facesMessages.add(FacesMessage.SEVERITY_ERROR, "Failed to determine mobile phone number")
                    return False
                    
            except (Exception, Throwable):
                facesMessages.add(FacesMessage.SEVERITY_ERROR, "Failed to determine mobile phone number")
                print 'TwilioSMS, Error finding mobile number for "%s". Exception: %s` % (user_name, sys.exc_info()[1])`'
                return False

            # Generate Random six digit code and store it in array
            code = 100000 + SecureRandom().nextInt(900000)

            # Get code and save it in LDAP temporarily with special session entry
            self.identity.setWorkingParameter("code", code)
            sessionId = sessionIdService.getSessionId() # fetch from persistence
            sessionId.getSessionAttributes().put("code", code)

            try:
                Twilio.init(self.ACCOUNT_SID, self.AUTH_TOKEN);
                message = Message.creator(PhoneNumber(mobile_number), PhoneNumber(self.FROM_NUMBER), str(code)).create();
                print "++++++++++++++++++++++++++++++++++++++++++++++"
                print 'TwilioSMs, Message Sid: %s' % (message.getSid())
                print "++++++++++++++++++++++++++++++++++++++++++++++"
                sessionId.getSessionAttributes().put("mobile_number", mobile_number)
                sessionId.getSessionAttributes().put("mobile", mobile_number)
                sessionIdService.updateSessionId(sessionId)
                self.identity.setWorkingParameter("mobile_number", mobile_number)
                self.identity.getSessionId().getSessionAttributes().put("mobile_number",mobile_number)
                self.identity.setWorkingParameter("mobile", mobile_number)
                self.identity.getSessionId().getSessionAttributes().put("mobile",mobile_number)
                print "========================================"
                print "===TWILIO SMS FIRST STEP DONE PROPERLY=="
                print "========================================"
                return True
            except Exception, ex:
                facesMessages.add(FacesMessage.SEVERITY_ERROR, "Failed to send message to mobile phone")
                print "TwilioSMS. Error sending message to Twilio"
                print "TwilioSMS. Unexpected error:", ex

            return False
        elif step == 2:
            # Retrieve the session attribute
            print "=============================================="
            print "=TWILIO SMS STEP 2 | Password Authentication=="
            print "=============================================="
            sessionIdService = CdiUtil.bean(SessionIdService)
            sessionId = sessionIdService.getSessionId() # fetch from persistence
            code = sessionId.getSessionAttributes().get("code")
            print '=======> Database code found'
            self.identity.setSessionId(sessionId)
            print "=============================================="
            print "TwilioSMS. Code present: %s" % str(code is not None)
            print "=============================================="
            if code is None:
                print "TwilioSMS. Failed to find previously sent code"
                return False

            if form_passcode is None:
                print "TwilioSMS. Passcode is empty"
                return False

            if len(form_passcode) != 6:
                print "TwilioSMS. Passcode from response is not 6 digits"
                return False

            if form_passcode == code:
                print "TiwlioSMS, SUCCESS! User entered the same code!"
                print "========================================"
                print "===TWILIO SMS SECOND STEP DONE PROPERLY"
                print "========================================"
                return True

            print "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++" 
            print "TwilioSMS. FAIL! User entered the wrong code!"
            print "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++" 
            facesMessages.add(FacesMessage.SEVERITY_ERROR, "Incorrect Twilio code, please try again.")
            print "================================================"
            print "===TWILIO SMS SECOND STEP FAILED: INCORRECT CODE"
            print "================================================"
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
            return "/auth/otp_sms/otp_sms.xhtml"

        return ""
        
    def getNextStep(self, configurationAttributes, requestParameters, step):
        return -1

    def getLogoutExternalUrl(self, configurationAttributes, requestParameters):
        print "Get external logout URL call"
        return None
        
    def logout(self, configurationAttributes, requestParameters):
        return True
