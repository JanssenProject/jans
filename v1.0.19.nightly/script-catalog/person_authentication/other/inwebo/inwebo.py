from io.jans.as.server.service import AuthenticationService
from io.jans.as.server.security import Identity
from io.jans.model.custom.script.type.auth import PersonAuthenticationType
from io.jans.service.cdi.util import CdiUtil
from io.jans.util import StringHelper,ArrayHelper
from io.jans.as.server.util import ServerUtil
from io.jans.as.server.service import UserService, AuthenticationService,SessionIdService
from io.jans.as.server.service.net import HttpService
from io.jans.as.server.service.common import EncryptionService
from jakarta.faces.application import FacesMessage
from jakarta.faces.context import FacesContext
from io.jans.jsf2.service import FacesService
from io.jans.jsf2.message import FacesMessages
from java.util import Arrays
from java.lang import String
from java.lang import System
import java
import sys
import json


    
class PersonAuthentication(PersonAuthenticationType):


    def __init__(self, currentTimeMillis):
        self.otp = "false"
        self.client = None

    def init(self, customScript, configurationAttributes):
       
        print "inWebo. Initialization"
        iw_cert_store_type = configurationAttributes.get("iw_cert_store_type").getValue2()
        iw_cert_path = configurationAttributes.get("iw_cert_path").getValue2()
        iw_creds_file = configurationAttributes.get("iw_creds_file").getValue2()
        
        self.push_withoutpin = "false"
        self.push_fail = "false"
        
        #permissible values = true , false
        self.push_withoutpin = 1 
        if StringHelper.equalsIgnoreCase("false" ,configurationAttributes.get("iw_push_withoutpin").getValue2()):
            self.push_withoutpin = 0
        self.api_uri =  configurationAttributes.get("iw_api_uri").getValue2()
        self.service_id = configurationAttributes.get("iw_service_id").getValue2()
        
        
        # Load credentials from file
        f = open(iw_creds_file, 'r')
        try:
           creds = json.loads(f.read())
        except:
            print "unexpected error - "+sys.exc_info()[0]
            return False
        finally:
            f.close()
        iw_cert_password = creds["CERT_PASSWORD"]
        
        #TODO: the password should not be in plaintext
        #try:
         #   encryptionService = CdiUtil.bean(EncryptionService)
          #  iw_cert_password = encryptionService.decrypt(iw_cert_password)
        #except:
         #   print("oops!",sys.exc_info()[0],"occured.")
          #  return False

        httpService = CdiUtil.bean(HttpService)
        self.client = httpService.getHttpsClient(None, None, None, iw_cert_store_type, iw_cert_path, iw_cert_password)
        print "inWebo. Initialized successfully"
        return True   
  

    def destroy(self, configurationAttributes):
        print "inWebo. Destroyed successfully"
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
        
        userService = CdiUtil.bean(UserService)
        authenticationService = CdiUtil.bean(AuthenticationService)
        identity = CdiUtil.bean(Identity)
        
        credentials = identity.getCredentials()
        user_name = credentials.getUsername()
        
        iw_otp = requestParameters.get("loginForm:otp")
         
        if ArrayHelper.isNotEmpty(iw_otp) and StringHelper.equalsIgnoreCase("true", iw_otp[0]) and step == 2:    
                identity.setWorkingParameter("iw_count_login_steps", 3)
                return True
                
        elif StringHelper.isEmptyString(user_name) and step == 1:
            print "empty user_name in step1 indicates browser token notfound"
            identity.setWorkingParameter("iw_count_login_steps", 2)
            return True
        else:
             
            response_check = False
            user_exists_in_gluu = authenticationService.authenticate(user_name)
            identity.setWorkingParameter("iw_count_login_steps", step)
            
            if (step == 1 or step == 3):
                print "if (step == 1 or step == 3):"
                password = credentials.getPassword()
                if StringHelper.isEmpty(password):
                    print "InWebo. Authenticate for step 2. otp token is empty"
                    return False
                #password is the otp token
                response_check = self.validateInweboToken(self.api_uri, self.service_id, user_name, password, step)
            elif (step == 2):
                print "elif (step == 2):"
                session = CdiUtil.bean(SessionIdService).getSessionId()
                if session == None:
                    print "InWebo. Authenticate for step 2. session_id is not exists"
                    return False

                response_check = self.checkStatus(self.api_uri, self.service_id, user_name, session.getId(), self.push_withoutpin)
                
                if self.push_fail is not None:
                    self.setErrorMessage(self.push_fail)
                    identity.setWorkingParameter("iw_count_login_steps", 3)
                    
            return response_check and user_exists_in_gluu 

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        
        if (step == 1):
            print "InWebo. Prepare for step 1"
            return True
        elif (step == 2):
            print "InWebo. Prepare for step 2"
            return True
        elif (step == 3):
            print "inWebo. Prepare for step 3"
            return True
        else:
            return False
        
    def getExtraParametersForStep(self, configurationAttributes, step):
        return None

    def getCountAuthenticationSteps(self, configurationAttributes):
        print "inside getCountAuthenticationSteps"
        identity = CdiUtil.bean(Identity)
        if (identity.isSetWorkingParameter("iw_count_login_steps")):
            print "identity.getWorkingParameter(iw_count_login_steps) - ", identity.getWorkingParameter("iw_count_login_steps")
            return identity.getWorkingParameter("iw_count_login_steps")
        
        return 3
    
    def getPageForStep(self, configurationAttributes, step):
        
        identity = CdiUtil.bean(Identity)
        if (step == 1):
            return "/auth/inwebo/iw_va.xhtml"
        elif (step == 2):
                return "/auth/inwebo/iwpushnotification.xhtml"
        elif (step == 3):
                return "/auth/inwebo/iwauthenticate.xhtml"
        else:
            return ""
    
    def isPassedDefaultAuthentication(self):
        identity = CdiUtil.bean(Identity)
        credentials = identity.getCredentials()
        user_name = credentials.getUsername()
        passed_step1 = StringHelper.isNotEmptyString(user_name)
        return passed_step1
    
    def validateInweboToken(self, iw_api_uri, iw_service_id, user_name, iw_token, step):
        httpService = CdiUtil.bean(HttpService)
        
        request_uri = iw_api_uri + "action=authenticateExtended" + "&serviceId=" + str(iw_service_id) + "&userId=" + httpService.encodeUrl(user_name) + "&token=" + str(iw_token)+"&format=json"
        print "InWebo. Token verification. Attempting to send authentication request:", request_uri
        
        try:
            http_service_response = httpService.executeGet(self.client, request_uri)
            http_response = http_service_response.getHttpResponse()
            print "status - ", http_response.getStatusLine().getStatusCode()
        except: 
            print "inWebo validate method. Exception: ", sys.exc_info()[1]
            return False

        try:
            if (http_response.getStatusLine().getStatusCode() != 200):
                print "inWebo. Invalid response from validation server: ", str(http_response.getStatusLine().getStatusCode())
                httpService.consume(http_response)
                return None
            
            response_bytes = httpService.getResponseContent(http_response)
            response_string = httpService.convertEntityToString(response_bytes)
            httpService.consume(http_response)
        
        finally:
            http_service_response.closeConnection()
        
        if response_string is None:
            print "inWebo. Get empty response from inWebo server"
            return None
    
        print "response string:",response_string
        json_response = json.loads(response_string)
        
        if not StringHelper.equalsIgnoreCase(json_response['err'], "OK"):
            print "inWebo. Get response with status: ", json_response['err']
            return False
        else:
            return True   # response_validation
    
    def checkStatus(self, iw_api_uri, iw_service_id, user_name,  session_id,without_pin):
        print "inside check status ", user_name+session_id
        # step 1: call action=pushAthenticate
        httpService = CdiUtil.bean(HttpService)
        
        request_uri = iw_api_uri + "action=pushAuthenticate" + "&serviceId=" + str(iw_service_id) + "&userId=" + httpService.encodeUrl(user_name) + "&format=json&withoutpin="+str(without_pin)
        #curTime = java.lang.System.currentTimeMillis()
        #endTime = curTime + (timeout * 1000)
        
        try:
            response_status = None
            http_service_response = httpService.executeGet(self.client, request_uri)
            http_response = http_service_response.getHttpResponse()
             
            if (http_response.getStatusLine().getStatusCode() != 200):
                print "inWebo. Invalid response from inwebo server: checkStatus ", str(http_response.getStatusLine().getStatusCode())
                httpService.consume(http_response)
                return None
            
            response_bytes = httpService.getResponseContent(http_response)
            response_string = httpService.convertEntityToString(response_bytes)
            httpService.consume(http_response)
        
        except: 
            print "inWebo validate method. Exception: ", sys.exc_info()[1]
            return False
    
        finally:
            http_service_response.closeConnection()
            
        print "response string:", response_string
        json_response = json.loads(response_string)

        if StringHelper.equalsIgnoreCase(json_response['err'], "OK"):
            
            session_id = json_response['sessionId']
            checkResult_uri = iw_api_uri + "action=checkPushResult" + "&serviceId=" + str(iw_service_id) + "&userId=" + httpService.encodeUrl(user_name) + "&sessionId="+ httpService.encodeUrl(session_id) + "&format=json&withoutpin=1"
            print "checkPushResult_uri:",checkResult_uri
            
            startTime = System.currentTimeMillis();
            currentTime = startTime;
            endTime = startTime  + long(25000)
            print "start time ----> ",startTime
            print "end time ",endTime
            while (endTime > currentTime ):
                try:
                    # step 2: call action=checkPushResult; using session id from step 1
                    http_check_push_response = httpService.executeGet(self.client, checkResult_uri)
                    check_push_response = http_check_push_response.getHttpResponse()
                    check_push_response_bytes = httpService.getResponseContent(check_push_response)
                    check_push_response_string = httpService.convertEntityToString(check_push_response_bytes)
                    httpService.consume(check_push_response)
                    
                    check_push_json_response = json.loads(check_push_response_string)
                    print "check_push_json_response :",check_push_json_response 
                    if StringHelper.equalsIgnoreCase(check_push_json_response['err'], "OK"):
                        self.push_fail = None
                        return True
                    elif StringHelper.equalsIgnoreCase(check_push_json_response['err'], "NOK:REFUSED"):
                        print "Push request notification for session", session_id
                        self.push_fail = "inwebo.push.notification.rejected"
                        return False
                    elif StringHelper.equalsIgnoreCase(check_push_json_response['err'], "NOK:TIMEOUT"):
                        print "Push request timed out for session", session_id
                        self.push_fail = "inwebo.push.notification.timed.out.for.session"
                        return False
                    elif StringHelper.equalsIgnoreCase(check_push_json_response['err'], "NOK:WAITING"):
                        self.push_fail = "inwebo.push.notification.timed.out.for.session"
                        currentTime = System.currentTimeMillis();
                        print " NOw ######## ", currentTime
                        java.lang.Thread.sleep(5000)
                        continue
                    else:
                        self.push_fail = "inwebo.push.notification.failed"
                        return False 
                    
                    
                    
                finally:
                    http_check_push_response.closeConnection()
                 
                
                    
        elif StringHelper.equalsIgnoreCase(json_response['err'], "NOK:SN"):
            self.push_fail ="inwebo.no.username"
            return False
        elif StringHelper.equalsIgnoreCase(json_response['err'], "NOK:account unknown"):
            self.push_fail ="inwebo.no.username"
            return False        
        else:
            print "No response from server."
            self.push_fail ="inwebo.push.notification.timed.out.for.session"
            return False
        
        print "inWebo. CheckStatus. The process has not received a response from the phone yet"
        
        return False
    
    def setErrorMessage(self, errorMessage):
        facesMessages = CdiUtil.bean(FacesMessages)
        facesMessages.setKeepMessages()
        facesMessages.clear()
        facesMessages.add(FacesMessage.SEVERITY_ERROR, String.format("#{msgs['%s']}", errorMessage))
        
    def hasEnrollments(self, configurationAttributes,user):
        return True;
        
    def getNextStep(self, configurationAttributes, requestParameters, step):
        return -1

    def getLogoutExternalUrl(self, configurationAttributes, requestParameters):
        print "Get external logout URL call"
        return None
            
    def logout(self, configurationAttributes, requestParameters):
        return True