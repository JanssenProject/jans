# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2016, Gluu
#
# Author: Michael Schwartz
#

from org.jboss.seam.security import Identity
from org.xdi.model.custom.script.type.auth import PersonAuthenticationType
from org.xdi.oxauth.service import UserService, AuthenticationService, SessionStateService
from org.xdi.util import StringHelper
from org.jboss.seam.contexts import Context, Contexts
from org.xdi.oxauth.util import ServerUtil
from org.xdi.util import StringHelper 
from org.xdi.util import ArrayHelper 
from java.util import Arrays
 
import com.twilio.sdk.TwilioRestClient as TwilioRestClient
import com.twilio.sdk.TwilioRestException as TwilioRestException
import com.twilio.sdk.resource.factory.MessageFactory as MessageFactory
import com.twilio.sdk.resource.instance.Message as Message
import org.apache.http.NameValuePair as NameValuePair
import org.apache.http.message.BasicNameValuePair  as BasicNameValuePair
import java.util.ArrayList as ArrayList
import java.util.Arrays.asList as List

import java
import random
import jarray

class PersonAuthentication(PersonAuthenticationType):

    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis
    
    def init(self, configurationAttributes):
        print "Twilio SMS. Initialization"
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
       context = Contexts.getEventContext()
       userService = UserService.instance()
       session_attributes = context.get("sessionAttributes")
       form_passcode = ServerUtil.getFirstValue(requestParameters, "passcode")
       form_name = ServerUtil.getFirstValue(requestParameters, "TwilioSmsloginForm")
       print "form_response_passcode: %s" % str(form_passcode)

    if (step == 1):
            print "Step 1 Password Authentication"
            credentials = Identity.instance().getCredentials()
            user_name = credentials.getUsername()
            user_password = credentials.getPassword()
 
            logged_in = False
            if (StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password)):
                logged_in = userService.authenticate(user_name, user_password)
            
            if (not logged_in):
                return False
         
            # Get Custom Properties
            ACCOUNT_SID = None
            AUTH_TOKEN = None
            FROM_NUMBER = None
            try:    
                ACCOUNT_SID = configurationAttributes.get("twilio_sid").getValue2()
            except:
                print 'Missing required configuration attribute "twilio_sid"'
            try:
                AUTH_TOKEN = configurationAttributes.get("twilio_token").getValue2()
            except:
                print'Missing required configuration attribute "twilio_token"'
            try:
                FROM_NUMBER = configurationAttributes.get("from_number").getValue2()
            except:
                print'Missing required configuration attribute "from_number"'
 
            if None in (ACCOUNT_SID, AUTH_TOKEN, FROM_NUMBER):
                print "ACCOUNT_SID, AUTH_TOKEN, FROM_NUMBER is None... returning False"
                return False
 
            # Get the Person's number and generate a code
            foundUser = None
            try:
                foundUser = userService.getUserByAttribute("uid", user_name)
            except:
                print 'Error retrieving user %s from LDAP' % (user_name)
                return False
            try:
                mobile_number = foundUser.getAttribute("phoneNumberVerified")
            except:
                print 'Error finding mobile number for' % (user_name) 
                return False
                
            # Generate Random six digit code and store it in array
            code = random.randint(100000,999999)
            # Get code and save it in LDAP temporarily with special session entry.  
            context.set("code", code)

            client = TwilioRestClient(ACCOUNT_SID, AUTH_TOKEN)
            bodyParam = BasicNameValuePair("Body", str(code))
            toParam = BasicNameValuePair("To", mobile_number)
            fromParam = BasicNameValuePair("From", FROM_NUMBER)

            params = ArrayList()
            params.add(bodyParam)
            params.add(toParam)
            params.add(fromParam)
            
            try:    
                messageFactory = client.getAccount().getMessageFactory()
                message = messageFactory.create(params)
                print 'Message Sid: %s' % (message.getSid())
                return True
            except:
                print "Error sending message to Twilio"
                return False
        
        elif (step == 2):
            # Retrieve the session attribute
            print "Step 2 SMS/OTP Authentication"
            code = session_attributes.get("code")
            print "Code: %s" % str(code)
   
            if (code is None):
                print "Failed to find previously sent code"
                return False 
           
            if (form_passcode is None):
                print "Passcode is empty"
                return False 
   
            if len(form_passcode) != 6:
                print "Passcode from response is not 6 digits: %s" % form_passcode
                return False
 
            if (form_passcode == code):
                print "SUCCESS! User entered the same code!" 
                return True
            else:
                print "FAIL! User entered the wrong code! %s != %s" % (form_passcode, code)
                return False            
        print "ERROR: step param not found or != (1|2)"
        return False
 
    def prepareForStep(self, configurationAttributes, requestParameters, step):
        if (step == 1):
            print "TwilioSMS. Prepare for Step 1"
            return True
        elif (step == 2):
            print "Step 2"
            print "TwilioSMS. Prepare for Step 2"
            return True
        else:
            print "TwilioSMS. Prepare for Step UNKNOWN"
            return False
 
    def printOut(s):
        print "TwilioSmsAuthenticator: %s" % s
 
    def getExtraParametersForStep(self, configurationAttributes, step):
        if (step == 2):
            return Arrays.asList("code")
            return None
        
    def getCountAuthenticationSteps(self, configurationAttributes):
        return 2
 
    def getPageForStep(self, configurationAttributes, step):
        if (step == 2):
            return "/auth/TwilioSMS/twiliosms.xhtml"
            return "" 

    def logout(self, configurationAttributes, requestParameters):
        return True
 
