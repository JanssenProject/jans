from io.jans.as.common.model.session  import SessionIdState

from io.jans.as.server.model.config import Constants
from io.jans.as.server.service import AuthenticationService
from io.jans.as.server.service import UserService
from io.jans.as.server.service import SessionIdService

from io.jans.as.server.security import Identity

from io.jans.as.server.util import ServerUtil

from io.jans.model.custom.script.type.owner import ResourceOwnerPasswordCredentialsType

from io.jans.service.cdi.util import CdiUtil
from io.jans.util import StringHelper
from java.lang import String
from java.util import Date, HashMap

import com.twilio.Twilio as Twilio
import com.twilio.rest.api.v2010.account.Message as Message
import com.twilio.type.PhoneNumber as PhoneNumber
# import org.codehaus.jettison.json.JSONArray as JSONArray

import random

class ResourceOwnerPasswordCredentials(ResourceOwnerPasswordCredentialsType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "2FA ROPC TWILIO. Initializing ..."

        self.STEP_ONE = "1";
        self.STEP_TWO = "2";
        self.STEP_THREE = "3";

        self.USERNAME_PARAM_NAME = "username"
        self.PASSWORD_PARAM_NAME = "password"
        self.STEP_PARAM_NAME     = "__step"
        self.OTP_CODE_PARAM_NAME = "__otp_code"
        self.SESSION_ID_PARAM_NAME = "__session_id"
        
        self.CUSTOM_HTTP_AUTHN_HEADER  = "X-ROPC-TWO-FA"

        self.USER_ATTR_UID = "uid"
        self.USER_ATTR_MAIL = "mail"
        self.USER_ATTR_HOME_PHONE_NUMBER = "telephoneNumber"
        self.USER_ATTR_MOBILE_PHONE_NUMBER = "mobile"

        self.CLIENT_ID_SESSION_PARAM = "__client_id"
        self.OTP_CODE_SESSION_PARAM  = "__otp"
        self.PHONE_NUMBER_SESSION_PARAM = "__phone"
        self.CURRENT_STEP_SESSION_PARAM = "__current_step"

        self.ERR_GENERIC_ERROR = 10001
        self.ERR_USER_NOT_FOUND = 10002
        self.ERR_PHONE_NUMBER_NOT_FOUND = 10003
        self.ERR_OTP_CODE_NOT_SENT = 10004
        self.ERR_OTP_CODE_MISMATCH = 10005
        self.ERR_SESSION_NOT_FOUND = 10006
        self.ERR_SESSION_INVALID   = 10007
        self.ERR_INVALID_USER_PASS = 10008

        self.STATUS_HTTP_ATTR = "status"
        self.ERROR_CODE_HTTP_ATTR = "error_code"
        self.SESSION_ID_HTTP_ATTR = "session_id"
        self.PHONE_NUMBER_HTTP_ATTR = "phone_number"
        self.PROCEED_HTTP_ATTR_VALUE = "proceed"
        self.ERROR_HTTP_ATTR_VALUE = "error"

        # Initialize Twillio Credentials Here
        self.TWILIO_ACCOUNT_SID = None
        self.TWILIO_AUTH_TOKEN  = None
        self.TWILIO_FROM_NUMBER = None

        try:
            self.TWILIO_ACCOUNT_SID = configurationAttributes.get("twilio_account_sid").getValue2()
        except:
            print '2FA ROPC TWILIO. Missing required configuration attribute "twilio_account_sid"'
        
        try:
            self.TWILIO_AUTH_TOKEN = configurationAttributes.get("twilio_auth_token").getValue2()
        except:
            print '2FA ROPC TWILIO. Missing required configuration attribute "twilio_auth_token"'
        
        try:
            self.TWILIO_FROM_NUMBER = configurationAttributes.get("twilio_from_number").getValue2()
        except:
            print'2FA ROPC TWILIO. Missing required configuration attribute "twilio_from_number"'
        
        if None in (self.TWILIO_ACCOUNT_SID, self.TWILIO_AUTH_TOKEN , self.TWILIO_FROM_NUMBER):
            print "2FA ROPC TWILIO. One or more configuration parameters is missing or empty. Initialization Failed .. Exiting"
            return False

        print "2FA ROPC TWILIO. Initialized successfully"
        return True

    def destroy(self, configurationAttributes):
        print "2FA ROPC TWILIO. Destroying ..."
        print "2FA ROPC TWILIO. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 11

    # Returns True and set user into context when user authenticated succesfully
    # Returns False when user not authenticated or it's needed to cancel notmal flow
    # Note :
    # context is reference of io.jans.as.service.external.context.ExternalResourceOwnerPasswordCredentialsContext#ExternalResourceOwnerPasswordCredentialsContext (in https://github.com/JanssenFederation/oxauth project, )
    def authenticate(self, context):
        print "2FA ROPC TWILIO. Authenticate"
        response = context.getHttpResponse()
        request  = context.getHttpRequest()
        
        step = request.getParameter(self.STEP_PARAM_NAME)
        if StringHelper.equalsIgnoreCase(step,self.STEP_ONE):
            return self.performStepOneAuth(request,response,context)
        if StringHelper.equalsIgnoreCase(step,self.STEP_TWO):
            return self.performStepTwoAuth(request,response, context)
        if StringHelper.equalsIgnoreCase(step,self.STEP_THREE):
            return self.performStepThreeAuth(request, response, context)
        
        return False
    
    def performStepOneAuth(self, request, response, context):
        print "2FA ROPC TWILIO. Perform Step One Auth"
        
        username = request.getParameter(self.USERNAME_PARAM_NAME)
        user = self.fetchUserData(username)
        if user is None:
            print "2FA ROPC TWILIO. User with username/email '%s' was not found" % username
            self.handleUserNotFound(response,context)
            return False
        
        phone_number = self.fetchUserPhone(user)
        if phone_number is None:
            print "2FA ROPC TWILIO. User '%s' has no associated phone number" % username
            self.handlePhoneNotFound(response,context)
            return False
            
        otp_code = self.generateOtpCode(context)
        if self.sendOtpCode(phone_number,otp_code) == False:
            print "2FA ROPC TWILIO. Could not send OTP code to phone '%s'" % phone_number  
            self.handleOtpCodeNotSend(response, context)
            return False
        
        session  = self.createSession(user,phone_number, otp_code, context)
        if session is None:
            print "2FA ROPC TWILIO. Session Creation Failed"
            self.handleSessionNotCreated(response, context)
            return False
        
        self.updateCurrentStep(session,self.STEP_TWO) # move to step 2 
        self.handleStepOneComplete(response,session,phone_number,context)
        return False
    
    def performStepTwoAuth(self, request, response, context):
        print "2FA ROPC TWILIO. Perform Step Two Auth"
        username = request.getParameter(self.USERNAME_PARAM_NAME)
        step = self.STEP_TWO
        user = self.fetchUserData(username)
        session_id = request.getParameter(self.SESSION_ID_PARAM_NAME)
        session = self.getSessionById(session_id)
        if session == None:
            print "2FA ROPC TWILIO. Step two auth failed. Session {%s} does not exist or has expired" % session_id
            self.handleSessionExpired(response,context)
            return False
        
        client = self.getAuthenticatedClient()
        print "2FA ROPC TWILIO. Performing session validation"
        if not self.verify_session_is_valid(session,user,client,step):
            print "2FA ROPC TWILIO. Session verification failed."
            self.handleInvalidSession(response,context)
            return False
        print "2FA ROPC TWILIO. Session validation complete"

        print "2FA ROPC TWILIO. Performing OTP Code Verification"

        otp_code = request.getParameter(self.PASSWORD_PARAM_NAME)
        if self.verifyOtpCode(otp_code,session) == False:
            print "2FA ROPC TWILIO. OTP verification failed"
            self.handleInvalidOtpCode(response,context)
            return False
        
        print "2FA ROPC TWILIO. OTP verification success"
        self.updateCurrentStep(session,self.STEP_THREE)
        self.handleStepTwoComplete(response, session, context)
        return False
    
    def performStepThreeAuth(self, request, response, context):
        print "2FA ROPC TWILIO. Perform Step Three Auth"
        username = request.getParameter(self.USERNAME_PARAM_NAME)
        password = request.getParameter(self.PASSWORD_PARAM_NAME)
        step = self.STEP_THREE
        user = self.fetchUserData(username)
        session_id = request.getParameter(self.SESSION_ID_PARAM_NAME)
        session = self.getSessionById(session_id)
        if session == None:
            print "2FA ROPC TWILIO. Step three auth failed. Session {%s} does not exist or has expired" % session_id
            self.handleSessionExpired(response,context)
            return False
        
        client = self.getAuthenticatedClient()
        print "2FA ROPC TWILIO. Performing session validation"
        if not self.verify_session_is_valid(session, user, client, step):
            print "2FA ROPC TWILIO. Session verification failed."
            self.handleInvalidSession(response, context)
            return False
        
        if self.authenticateUser(user.getUserId(), password, context) ==  False:
            print "2FA ROPC TWILIO. Step three auth failed. Invalid username/password"
            self.handleInvalidUserPass(response,context)
            return False
        
        print "2FA ROPC TWILIO. Step Three Authentication Success"
        self.handleStepThreeComplete(response, session, context)
        return True
    
    def fetchUserData(self, username):
        userService = CdiUtil.bean(UserService)
        user = userService.getUserByAttribute(self.USER_ATTR_UID,username)
        if user is None:
            user = userService.getUserByAttribute(self.USER_ATTR_MAIL,username)
        
        return user
    
    def fetchUserPhone(self, user):
        phone_number = user.getAttribute(self.USER_ATTR_HOME_PHONE_NUMBER)
        if phone_number is None:
            phone_number = user.getAttribute(self.USER_ATTR_MOBILE_PHONE_NUMBER)
        return phone_number
    
    def getAuthenticatedClient(self):
        return CdiUtil.bean(Identity).getSessionClient().getClient()
    
    def getSessionById(self,session_id):
        sessionIdService = CdiUtil.bean(SessionIdService)
        return sessionIdService.getSessionId(session_id)
    
    def createSession(self, user, phone_number, otp_code, context):
        sessionIdService = CdiUtil.bean(SessionIdService)
        identityService = CdiUtil.bean(Identity)
        client = identityService.getSessionClient().getClient()
        auth_date = Date()
        sid_attrs  = HashMap()
        sid_attrs.put(Constants.AUTHENTICATED_USER, user.getUserId())
        sid_attrs.put(self.CLIENT_ID_SESSION_PARAM,client.getClientId())
        sid_attrs.put(self.PHONE_NUMBER_SESSION_PARAM,phone_number)
        sid_attrs.put(self.OTP_CODE_SESSION_PARAM,str(otp_code))
        return sessionIdService.generateUnauthenticatedSessionId(user.getDn(),auth_date,SessionIdState.UNAUTHENTICATED,sid_attrs,True)
    
    def deleteSession(self, session):
        sessionIdService = CdiUtil.bean(SessionIdService)
        sessionIdService.remove(session)
        return True
    
    def deleteSessionById(self,sessionid):
        sessionIdService = CdiUtil.bean(SessionIdService)
        session = sessionIdService.getSessionId(sessionid)
        if session == None:
            return False
        sessionIdService.remove(session)
        return True
    
    
    def sendOtpCode(self, phone_number, otp_code):
        try:
            Twilio.init(self.TWILIO_ACCOUNT_SID,self.TWILIO_AUTH_TOKEN)
            sms_text = "Your OTP Login Code Is %s" % str(otp_code)
            if StringHelper.equalsIgnoreCase("+",phone_number[0]) == False:
                # append a '+' in case it's missing
                phone_number =  "+" + phone_number
            
            message = Message.creator(PhoneNumber(phone_number),PhoneNumber(self.TWILIO_FROM_NUMBER),sms_text).create()
            print "++++++++++++++++++++++++++++++++++++++++++++++"
            print '2FA ROPC TWILIO. Message Sid: %s' % (message.getSid())
            print '2FA ROPC TWILIO. User phone: %s' % (phone_number)
            print "++++++++++++++++++++++++++++++++++++++++++++++"
            return True
        except Exception as ex:
            print "2FA ROPC TWILIO. Error sending OTP Code via Twilio. Error is ", ex
            return False
    
    def generateOtpCode(self, context):
        #For now we just generate a six digit random number
        return random.randint(100000,999999)
    
    def updateCurrentStep(self, session , current_step):
        session_attrs = session.getSessionAttributes()
        session_attrs.put(self.CURRENT_STEP_SESSION_PARAM,str(current_step))
        session.setSessionAttributes(session_attrs)
        sessionIdService = CdiUtil.bean(SessionIdService)
        return sessionIdService.updateSessionId(session,True,True,True)
    
    def verify_session_is_valid(self, session, user, client, current_step):
        session_attrs = session.getSessionAttributes()
        client_id =  session_attrs.get(self.CLIENT_ID_SESSION_PARAM)
        if not StringHelper.equalsIgnoreCase(client_id,client.getClientId()):
            print "2FA ROPC TWILIO. Session {%s} client_id mismatch" % session.getId()
            return False
        
        user_id = session_attrs.get(Constants.AUTHENTICATED_USER)
        if not StringHelper.equalsIgnoreCase(user_id,user.getUserId()):
            print "2FA ROPC TWILIO. Session {%s} user_id mismatch" % session.getId()
            return False
        
        expected_step = session_attrs.get(self.CURRENT_STEP_SESSION_PARAM)
        if not StringHelper.equalsIgnoreCase(current_step,expected_step):
            print "2FA ROPC TWILIO. Current step does not match expected step stored in session {%s}" % session.getId()
            print "2FA ROPC TWILIO. Expected step is {%s}" % expected_step
            return False
        
        return True
    
    def verifyOtpCode(self, otp_code , session):
        session_attrs = session.getSessionAttributes()
        expected_otp_code = session_attrs.get(self.OTP_CODE_SESSION_PARAM)
        return StringHelper.equalsIgnoreCase(otp_code,expected_otp_code)
    
    def authenticateUser(self, username, password, context):
        authService = CdiUtil.bean(AuthenticationService)
        result = authService.authenticate(username, password)
        if not result:
            print "2FA ROPC TWILIO. User/Password authentication failed for {%s}" % username
            return False
        
        context.setUser(authService.getAuthenticatedUser())
        return True
    
    def buildErrorString(self, error_code):
        error_string = "%s=%s;%s=%s" % (self.STATUS_HTTP_ATTR,self.ERROR_HTTP_ATTR_VALUE,self.ERROR_CODE_HTTP_ATTR,error_code)
        return error_string
    
    def buildProceedString(self, session_id,phone_number=None):
        status_fragment = '%s=%s' % (self.STATUS_HTTP_ATTR,self.PROCEED_HTTP_ATTR_VALUE)
        session_id_fragment = '%s=%s' % (self.SESSION_ID_HTTP_ATTR,session_id)
        if phone_number != None:
            phone_number_fragment = '%s=%s' % (self.PHONE_NUMBER_HTTP_ATTR,phone_number)
        
        if phone_number != None:
            proceed_string = '%s;%s;%s' % (status_fragment,session_id_fragment,phone_number_fragment)
        else:
            proceed_string = "%s;%s" % (status_fragment,session_id_fragment)
        
        return proceed_string
    
    def handleUserNotFound(self, response, context):
        error_message = self.buildErrorString(self.ERR_USER_NOT_FOUND)
        response.setHeader(self.CUSTOM_HTTP_AUTHN_HEADER,error_message)
        return False
    
    def handlePhoneNotFound(self, response, context):
        error_message = self.buildErrorString(self.ERR_PHONE_NUMBER_NOT_FOUND)
        response.setHeader(self.CUSTOM_HTTP_AUTHN_HEADER,error_message)
        return False
    
    def handleSessionNotCreated(self, response, context):
        error_message = self.buildErrorString(self.ERR_GENERIC_ERROR)
        response.setHeader(self.CUSTOM_HTTP_AUTHN_HEADER,error_message)
        return False
    
    def handleOtpCodeNotSend(self, response, context):
        error_message = self.buildErrorString(self.ERR_GENERIC_ERROR)
        response.setHeader(self.CUSTOM_HTTP_AUTHN_HEADER,error_message)
        return False
    
    def handleStepOneComplete(self, response, session, phone_number, context):
        proceed_message = self.buildProceedString(session.getId(),phone_number)
        response.setHeader(self.CUSTOM_HTTP_AUTHN_HEADER,proceed_message)
        return False
    
    def handleSessionExpired(self, response, context):
        error_message = self.buildErrorString(self.ERR_SESSION_NOT_FOUND)
        response.setHeader(self.CUSTOM_HTTP_AUTHN_HEADER,error_message)
        return False
    
    def handleInvalidSession(self, response, context):
        error_message = self.buildErrorString(self.ERR_SESSION_INVALID)
        response.setHeader(self.CUSTOM_HTTP_AUTHN_HEADER,error_message)
        return False
    
    def handleInvalidOtpCode(self, response, context):
        error_message = self.buildErrorString(self.ERR_OTP_CODE_MISMATCH)
        response.setHeader(self.CUSTOM_HTTP_AUTHN_HEADER,error_message)
        return False
    
    def handleStepTwoComplete(self, response, session, context):
        proceed_message = self.buildProceedString(session.getId())
        response.setHeader(self.CUSTOM_HTTP_AUTHN_HEADER,proceed_message)
        return False
    
    def handleInvalidUserPass(self, response, context):
        error_message = self.buildErrorString(self.ERR_INVALID_USER_PASS)
        response.setHeader(self.CUSTOM_HTTP_AUTHN_HEADER,error_message)
        return False
    
    def handleStepThreeComplete(self, response, session, context):
        #self.deleteSession(session)
        return True;