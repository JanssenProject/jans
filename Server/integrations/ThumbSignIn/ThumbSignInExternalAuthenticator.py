# Author: ThumbSignIn (A unit of Pramati Technologies)

from org.xdi.service.cdi.util import CdiUtil
from org.xdi.oxauth.security import Identity
from org.xdi.model.custom.script.type.auth import PersonAuthenticationType
from org.xdi.oxauth.service import AuthenticationService
from org.xdi.util import StringHelper
from org.xdi.oxauth.util import ServerUtil
from com.pramati.ts.thumbsignin_java_sdk import ThumbsigninApiController
from org.json import JSONObject
from org.xdi.oxauth.model.util import Base64Util
from java.lang import String

import java

class PersonAuthentication(PersonAuthenticationType):
    
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, configurationAttributes):
        print "ThumbSignIn. Initialization"
        
        global ts_host 
        ts_host = configurationAttributes.get("ts_host").getValue2()
        print "ThumbSignIn. Initialization. Value of ts_host is %s" % ts_host
        
        global ts_apiKey
        ts_apiKey = configurationAttributes.get("ts_apiKey").getValue2()
        print "ThumbSignIn. Initialization. Value of ts_apiKey is %s" % ts_apiKey
        
        global ts_apiSecret
        ts_apiSecret = configurationAttributes.get("ts_apiSecret").getValue2()
        
        global ts_statusPath
        ts_statusPath = "/ts/secure/txn-status/"
         
        print "ThumbSignIn. Initialized successfully"
        return True   

    def destroy(self, configurationAttributes):
        print "ThumbSignIn. Destroy"
        return True

    def getApiVersion(self):
        return 1

    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        return True

    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        return None
    
    def setRelyingPartyLoginUrl(self, identity):
        print "ThumbSignIn. Inside setRelyingPartyLoginUrl..."
        sessionId =  identity.getSessionId()
        sessionAttribute = sessionId.getSessionAttributes()
        stateJWTToken = sessionAttribute.get("state")
        
        relyingPartyLoginUrl = ""
        relyingPartyId = ""
        if (stateJWTToken != None) :
            stateJWTTokenArray = String(stateJWTToken).split("\\.")
            stateJWTTokenPayload = stateJWTTokenArray[1]
            statePayloadStr = String(Base64Util.base64urldecode(stateJWTTokenPayload), "UTF-8")
            statePayloadJson = JSONObject(statePayloadStr)
            print "ThumbSignIn. Value of state JWT token Payload is %s" % statePayloadJson
            additional_claims = statePayloadJson.get("additional_claims")
            relyingPartyId = additional_claims.get("relyingPartyId")
            print "ThumbSignIn. Value of relyingPartyId is %s" % relyingPartyId
            identity.setWorkingParameter("relyingPartyId", relyingPartyId)
            
            if (String(relyingPartyId).startsWith("google.com")):
                #google.com/a/unphishableenterprise.com
                relyingPartyIdArray = String(relyingPartyId).split("/")
                googleDomain = relyingPartyIdArray[2]
                print "ThumbSignIn. Value of googleDomain is %s" % googleDomain
                relyingPartyLoginUrl = "https://www.google.com/accounts/AccountChooser?hd="+ googleDomain + "%26continue=https://apps.google.com/user/hub"               
            #elif (String(relyingPartyId).startsWith("xyz")):
                #relyingPartyLoginUrl = "xyz.com"
            else:
                #If relyingPartyLoginUrl is empty, Gluu's default login URL will be used
                relyingPartyLoginUrl = ""
        
        print "ThumbSignIn. Value of relyingPartyLoginUrl is %s" % relyingPartyLoginUrl                
        identity.setWorkingParameter("relyingPartyLoginUrl", relyingPartyLoginUrl)
        return None
    
    def prepareForStep(self, configurationAttributes, requestParameters, step):
        print "ThumbSignIn. Inside prepareForStep. Step %d" % step
        identity = CdiUtil.bean(Identity)
        authenticationService = CdiUtil.bean(AuthenticationService)

        global ts_host
        global ts_apiKey
        global ts_apiSecret
        global ts_statusPath
        
        identity.setWorkingParameter("ts_host", ts_host)
        identity.setWorkingParameter("ts_statusPath", ts_statusPath)
        
        self.setRelyingPartyLoginUrl(identity)              
        thumbsigninApiController = ThumbsigninApiController()

        if (step == 1 or step == 3):
            print "ThumbSignIn. Prepare for step 1"
            
            # Invoking the authenticate ThumbSignIn API via the Java SDK
            authenticateResponseJsonStr = thumbsigninApiController.handleThumbSigninRequest("authenticate", ts_apiKey, ts_apiSecret)
            print "ThumbSignIn. Value of authenticateResponseJsonStr is %s" % authenticateResponseJsonStr
            
            authenticateResponseJsonObj = JSONObject(authenticateResponseJsonStr)
            transactionId = authenticateResponseJsonObj.get("transactionId")            
            authenticationStatusRequest = "authStatus/" + transactionId
            print "ThumbSignIn. Value of authenticationStatusRequest is %s" % authenticationStatusRequest
                      
            authorizationHeaderJsonStr = thumbsigninApiController.getAuthorizationHeaderJsonStr(authenticationStatusRequest, ts_apiKey, ts_apiSecret)
            print "ThumbSignIn. Value of authorizationHeaderJsonStr is %s" % authorizationHeaderJsonStr
            # {"authHeader":"HmacSHA256 Credential=XXX, SignedHeaders=accept;content-type;x-ts-date, Signature=XXX","XTsDate":"XXX"}
            
            authorizationHeaderJsonObj = JSONObject(authorizationHeaderJsonStr)
            authorizationHeader = authorizationHeaderJsonObj.get("authHeader")
            xTsDate = authorizationHeaderJsonObj.get("XTsDate")
            print "ThumbSignIn. Value of authorizationHeader is %s" % authorizationHeader
            print "ThumbSignIn. Value of xTsDate is %s" % xTsDate
            
            identity.setWorkingParameter("authenticateResponseJsonStr", authenticateResponseJsonStr)
            identity.setWorkingParameter("authorizationHeader", authorizationHeader)
            identity.setWorkingParameter("xTsDate", xTsDate)

            return True
        
        elif (step == 2):
            print "ThumbSignIn. Prepare for step 2"
            
            if (identity.isSetWorkingParameter("userLoginFlow")):
                userLoginFlow = identity.getWorkingParameter("userLoginFlow")
                print "ThumbSignIn. Value of userLoginFlow is %s" % userLoginFlow
            
            user = authenticationService.getAuthenticatedUser()
            if (user == None):
                print "ThumbSignIn. Prepare for step 2. Failed to determine user name"
                return False
            
            user_name = user.getUserId()
            print "ThumbSignIn. Prepare for step 2. user_name: " + user_name
            if (user_name == None):
                return False
            
            registerRequestPath = "register/" + user_name
            
            # Invoking the register ThumbSignIn API via the Java SDK
            registerResponseJsonStr = thumbsigninApiController.handleThumbSigninRequest(registerRequestPath, ts_apiKey, ts_apiSecret)
            print "ThumbSignIn. Value of registerResponseJsonStr is %s" % registerResponseJsonStr
            
            registerResponseJsonObj = JSONObject(registerResponseJsonStr)
            transactionId = registerResponseJsonObj.get("transactionId")            
            registrationStatusRequest = "regStatus/" + transactionId
            print "ThumbSignIn. Value of registrationStatusRequest is %s" % registrationStatusRequest
            
            authorizationHeaderJsonStr = thumbsigninApiController.getAuthorizationHeaderJsonStr(registrationStatusRequest, ts_apiKey, ts_apiSecret)
            print "ThumbSignIn. Value of authorizationHeaderJsonStr is %s" % authorizationHeaderJsonStr
            # {"authHeader":"HmacSHA256 Credential=XXX, SignedHeaders=accept;content-type;x-ts-date, Signature=XXX","XTsDate":"XXX"}
            
            authorizationHeaderJsonObj = JSONObject(authorizationHeaderJsonStr)
            authorizationHeader = authorizationHeaderJsonObj.get("authHeader")
            xTsDate = authorizationHeaderJsonObj.get("XTsDate")
            print "ThumbSignIn. Value of authorizationHeader is %s" % authorizationHeader
            print "ThumbSignIn. Value of xTsDate is %s" % xTsDate
            
            identity.setWorkingParameter("userId", user_name)
            identity.setWorkingParameter("registerResponseJsonStr", registerResponseJsonStr)
            identity.setWorkingParameter("authorizationHeader", authorizationHeader)
            identity.setWorkingParameter("xTsDate", xTsDate)

            return True
        else:
            return False

    def authenticate(self, configurationAttributes, requestParameters, step):        
        print "ThumbSignIn. Inside authenticate. Step %d" % step
        authenticationService = CdiUtil.bean(AuthenticationService)
        identity = CdiUtil.bean(Identity)
        
        global ts_host
        global ts_apiKey
        global ts_apiSecret
        global ts_statusPath
        
        identity.setWorkingParameter("ts_host", ts_host)
        identity.setWorkingParameter("ts_statusPath", ts_statusPath)
        
        thumbsigninApiController = ThumbsigninApiController()

        if (step == 1 or step == 3):
            print "ThumbSignIn. Authenticate for Step %d" % step
            
            login_flow = ServerUtil.getFirstValue(requestParameters, "login_flow")
            print "ThumbSignIn. Value of login_flow parameter is %s" % login_flow
            
            #Logic for ThumbSignIn Authentication Flow
            if (login_flow == "ThumbSignIn_Authentication" or login_flow == "ThumbSignIn_RegistrationSucess"):
                identity.setWorkingParameter("userLoginFlow", login_flow)
                print "ThumbSignIn. Value of userLoginFlow is %s" % identity.getWorkingParameter("userLoginFlow")
                
                transactionId = ServerUtil.getFirstValue(requestParameters, "transactionId")
                print "ThumbSignIn. Value of transactionId is %s" % transactionId
                getUserRequest = "getUser/" + transactionId
                print "ThumbSignIn. Value of getUserRequest is %s" % getUserRequest
                
                getUserResponseJsonStr = thumbsigninApiController.handleThumbSigninRequest(getUserRequest, ts_apiKey, ts_apiSecret)
                print "ThumbSignIn. Value of getUserResponseJsonStr is %s" % getUserResponseJsonStr
                getUserResponseJsonObj = JSONObject(getUserResponseJsonStr)
                thumbSignIn_UserId = getUserResponseJsonObj.get("userId")                
                print "ThumbSignIn. Value of thumbSignIn_UserId is %s" % thumbSignIn_UserId
                
                logged_in_status = authenticationService.authenticate(thumbSignIn_UserId)
                print "ThumbSignIn. logged_in status : %r" % (logged_in_status)
                return logged_in_status
            
            #Logic for ThumbSignIn Registration Flow    
            identity.setWorkingParameter("userLoginFlow", "ThumbSignIn_Registration")
            print "ThumbSignIn. Value of userLoginFlow is %s" % identity.getWorkingParameter("userLoginFlow")
            credentials = identity.getCredentials()

            user_name = credentials.getUsername()
            user_password = credentials.getPassword()
            print "ThumbSignIn. user_name: " + user_name
            #print "ThumbSignIn. user_password: " + user_password

            logged_in = False
            if (StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password)):
                logged_in = authenticationService.authenticate(user_name, user_password)
                
            print "ThumbSignIn. Status of LDAP Authentication : %r" % (logged_in)
            
            if (not logged_in):
                # Invoking the authenticate ThumbSignIn API via the Java SDK
                authenticateResponseJsonStr = thumbsigninApiController.handleThumbSigninRequest("authenticate", ts_apiKey, ts_apiSecret)
                print "ThumbSignIn. Value of authenticateResponseJsonStr is %s" % authenticateResponseJsonStr
            
                authenticateResponseJsonObj = JSONObject(authenticateResponseJsonStr)
                transactionId = authenticateResponseJsonObj.get("transactionId")            
                authenticationStatusRequest = "authStatus/" + transactionId
                print "ThumbSignIn. Value of authenticationStatusRequest is %s" % authenticationStatusRequest
                      
                authorizationHeaderJsonStr = thumbsigninApiController.getAuthorizationHeaderJsonStr(authenticationStatusRequest, ts_apiKey, ts_apiSecret)
                print "ThumbSignIn. Value of authorizationHeaderJsonStr is %s" % authorizationHeaderJsonStr
                # {"authHeader":"HmacSHA256 Credential=XXX, SignedHeaders=accept;content-type;x-ts-date, Signature=XXX","XTsDate":"XXX"}
            
                authorizationHeaderJsonObj = JSONObject(authorizationHeaderJsonStr)
                authorizationHeader = authorizationHeaderJsonObj.get("authHeader")
                xTsDate = authorizationHeaderJsonObj.get("XTsDate")
                print "ThumbSignIn. Value of authorizationHeader is %s" % authorizationHeader
                print "ThumbSignIn. Value of xTsDate is %s" % xTsDate
            
                identity.setWorkingParameter("authenticateResponseJsonStr", authenticateResponseJsonStr)
                identity.setWorkingParameter("authorizationHeader", authorizationHeader)
                identity.setWorkingParameter("xTsDate", xTsDate)
                return False

            print "ThumbSignIn. Authenticate for step 1 successful"
            return True
        
        elif (step == 2):
            print "ThumbSignIn. Registration flow (step 2)"
            
            if (identity.isSetWorkingParameter("userLoginFlow")):
                userLoginFlow = identity.getWorkingParameter("userLoginFlow")
                print "ThumbSignIn. Value of userLoginFlow is %s" % userLoginFlow
            else:
                identity.setWorkingParameter("userLoginFlow", "ThumbSignIn_Registration")
                print "ThumbSignIn. Setting the value of userLoginFlow to %s" % identity.getWorkingParameter("userLoginFlow")

            user = authenticationService.getAuthenticatedUser()
            if user == None:
                print "ThumbSignIn. Registration flow (step 2). Failed to determine user name"
                return False

            user_name = user.getUserId()
            print "ThumbSignIn. Registration flow (step 2). user_name: " + user_name
            
            print "ThumbSignIn. Registration flow (step 2) successful"
            return True
        else:
            return False    

    def getExtraParametersForStep(self, configurationAttributes, step):
        return None

    def getCountAuthenticationSteps(self, configurationAttributes):       
        print "ThumbSignIn. Inside getCountAuthenticationSteps.."
        identity = CdiUtil.bean(Identity)
        
        userLoginFlow = identity.getWorkingParameter("userLoginFlow")
        print "ThumbSignIn. Value of userLoginFlow is %s" % userLoginFlow
        if (userLoginFlow == "ThumbSignIn_Authentication"):
            print "ThumbSignIn. Total Authentication Steps is: 1"
            return 1  
        #If the userLoginFlow is registration, then we can handle the ThumbSignIn registration as part of the second step 
        print "ThumbSignIn. Total Authentication Steps is: 3"    
        return 3

    def getPageForStep(self, configurationAttributes, step):
        print "ThumbSignIn. Inside getPageForStep. Step %d" % step
        if (step == 2):
            return "/auth/thumbsignin/tsRegister.xhtml"
        elif (step == 3):
            return "/auth/thumbsignin/tsRegistrationSuccess.xhtml"
        else:
            return "/auth/thumbsignin/tsLogin.xhtml"

    def logout(self, configurationAttributes, requestParameters):
        return True
