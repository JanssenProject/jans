#!/usr/bin/python
# -*- coding: utf-8 -*-

import sys
from java.util import Collections, HashMap, HashSet, ArrayList, Arrays, Date
from org.oxauth.persistence.model.configuration import GluuConfiguration
from io.jans.orm import PersistenceEntryManager
from java.nio.charset import Charset
from io.jans.service.cdi.util import CdiUtil
from io.jans.as.server.security import Identity
from io.jans.model.custom.script.type.auth import PersonAuthenticationType
from io.jans.as.server.service import AuthenticationService, SessionIdService
from io.jans.as.server.service.common import UserService
from io.jans.util import StringHelper
from io.jans.as.server.service.net import HttpService
from org.json import JSONObject
import base64
import java

from io.jans.util import StringHelper
from java.lang import String

class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript,  configurationAttributes):

        
        if not configurationAttributes.containsKey("ENDPOINT"):
            print "BioID. Initialization. Property ENDPOINT is mandatory"
            return False
        self.ENDPOINT = configurationAttributes.get("ENDPOINT").getValue2()
        
        if not configurationAttributes.containsKey("APP_IDENTIFIER"):
            print "BioID. Initialization. Property APP_IDENTIFIER is mandatory"
            return False
        self.APP_IDENTIFIER = configurationAttributes.get("APP_IDENTIFIER").getValue2()
        
        if not configurationAttributes.containsKey("APP_SECRET"):
            print "BioID. Initialization. Property APP_SECRET is mandatory"
            return False
        self.APP_SECRET = configurationAttributes.get("APP_SECRET").getValue2()
        
        if not configurationAttributes.containsKey("PARTITION"):
            print "BioID. Initialization. Property PARTITION is mandatory"
            return False
        self.PARTITION = configurationAttributes.get("PARTITION").getValue2()
        
        if not configurationAttributes.containsKey("STORAGE"):
            print "BioID. Initialization. Property STORAGE is mandatory"
            return False
        self.STORAGE = configurationAttributes.get("STORAGE").getValue2()
        
        print "BioID. Initialized successfully"
        return True   

    def destroy(self, configurationAttributes):
        print "BioID. Destroy"
        print "BioID. Destroyed successfully"
        return True
        
    def getAuthenticationMethodClaims(self, requestParameters):
        return None
        
    def getApiVersion(self):
        return 11

    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        return True

    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        return None

    def authenticate(self, configurationAttributes, requestParameters, step):
        print "BioID. Authenticate "
        authenticationService = CdiUtil.bean(AuthenticationService)
        identity = CdiUtil.bean(Identity)
        credentials = identity.getCredentials()
        user_name = credentials.getUsername()
        
        if (step == 1):
            print "BioID. Authenticate for step 1"

            logged_in = False
            userService = CdiUtil.bean(UserService)
            authenticated_user = self.processBasicAuthentication(credentials)
            if authenticated_user == None:
                print "BioID. User does not exist"
                return False
            
            identity.setWorkingParameter("user_name",user_name)
            bcid = self.STORAGE + "." + self.PARTITION + "." + str(String(user_name).hashCode())
            print "BioID. username:bcid %s:%s" %(user_name, bcid)
            
            is_user_enrolled = self.isenrolled(bcid)
            print "BioID. is_user_enrolled: '%s'" % is_user_enrolled
            
            if(is_user_enrolled == True):
                identity.setWorkingParameter("bioID_auth_method","verification")
            else:
                identity.setWorkingParameter("bioID_auth_method","enrollment")
                identity.setWorkingParameter("bioID_count_login_steps", 2)
            
            return True
        
        elif step == 2 or step == 3:
            
            auth_method = identity.getWorkingParameter("bioID_auth_method")
            print "BioID. Authenticate method for step %s. bioID_auth_method: '%s'" % (step,auth_method)
            user_name = identity.getWorkingParameter("user_name")
            bcid = self.STORAGE + "." + self.PARTITION + "." + str(String(user_name).hashCode())
            
            if step == 2 and 'enrollment' == auth_method:
                
                access_token = identity.getWorkingParameter("access_token")
                result = self.performBiometricOperation( access_token, "enroll")
                
                if result == True:
                    #this means that enroll is a success, the next is step 3 authenticate
                    identity.setWorkingParameter("bioID_count_login_steps", 3)
                    identity.setWorkingParameter("bioID_auth_method","verification")
                    return result
                else:
                    return False
            
            else :
                
                access_token = identity.getWorkingParameter("access_token")
                result = self.performBiometricOperation( access_token, "verify")
                return result
            
        else:
            return False

    
    def prepareForStep(self, configurationAttributes, requestParameters, step):
        
        print "BioID. Prepare for step called : step %s" % step
        if step == 1:
            return True
        elif step == 2  or step == 3:
            identity = CdiUtil.bean(Identity)
            user_name = identity.getWorkingParameter("user_name")
            auth_method = identity.getWorkingParameter("bioID_auth_method")
            print "BioID. step %s %s" % (step, auth_method)
            bcid = self.STORAGE + "." + self.PARTITION + "." + str(String(user_name).hashCode())
            print "bcid %s" %bcid
            if step == 2 and auth_method == 'enrollment':
                print "access token used by upload method - enroll"
                access_token = self.getAccessToken( bcid, "enroll" )
            # either step2 and verification or step 3 which is verification post enrollment
            else:
                print "access token used by upload method - verify"
                access_token = self.getAccessToken( bcid, "verify" )
                
            print "access_token %s - " % access_token
            identity.setWorkingParameter("access_token",access_token)
            
            return True
            
        else:
            return False

    def getExtraParametersForStep(self, configurationAttributes, step):
        return Arrays.asList("bioID_auth_method","access_token","user_name","bioID_count_login_steps")
    
    
    def getCountAuthenticationSteps(self, configurationAttributes):
        
        identity = CdiUtil.bean(Identity)
        if identity.isSetWorkingParameter("bioID_count_login_steps"):
            print "BioID. getCountAuthenticationSteps called, returning - 3"
            return 3
        else:
            print "BioID. getCountAuthenticationSteps called, returning - 2"
            return 2

    def getPageForStep(self, configurationAttributes, step):
        print "BioID. getPageForStep called -step:%s" % str(step)
        if step > 1 :
            return   "/auth/bioid/bioid.xhtml"
        else:
            return ""

    def getNextStep(self, configurationAttributes, requestParameters, step):
        print "BioID. getNextStep called.  %s" % str(step)
        
        return -1

    def getLogoutExternalUrl(self, configurationAttributes, requestParameters):
        print "Get external logout URL call"
        return None

    def logout(self, configurationAttributes, requestParameters):
        return True

    # Get a BWS token to be used for authorization.
    # bcid - The Biometric Class ID (BCID) of the person
    # forTask - The task for which the issued token shall be used.
    # A string containing the issued BWS token.
    def getAccessToken(self, bcid, forTask):
        
        httpService = CdiUtil.bean(HttpService)

        http_client = httpService.getHttpsClient()
        http_client_params = http_client.getParams()

        bioID_service_url = self.ENDPOINT + "token?id="+self.APP_IDENTIFIER+"&bcid="+bcid+"&task="+forTask+"&livedetection=true"
        encodedString = base64.b64encode((self.APP_IDENTIFIER+":"+self.APP_SECRET).encode('utf-8'))
        bioID_service_headers = {"Authorization": "Basic "+encodedString}

        try:
            http_service_response = httpService.executeGet(http_client, bioID_service_url, bioID_service_headers)
            http_response = http_service_response.getHttpResponse()
        except:
            print "BioID. Unable to obtain access token. Exception: ", sys.exc_info()[1]
            return None

        try:
            if not httpService.isResponseStastusCodeOk(http_response):
                print "BioID. Unable to obtain access token.  Get non 200 OK response from server:", str(http_response.getStatusLine().getStatusCode())
                httpService.consume(http_response)
                return None

            response_bytes = httpService.getResponseContent(http_response)
            response_string = httpService.convertEntityToString(response_bytes, Charset.forName("UTF-8"))
            httpService.consume(http_response)
            return response_string
        finally:
            http_service_response.closeConnection()
    
    def isenrolled(self, bcid):
        httpService = CdiUtil.bean(HttpService)

        http_client = httpService.getHttpsClient()
        http_client_params = http_client.getParams()

        bioID_service_url = self.ENDPOINT + "isenrolled?bcid="+bcid+"&trait=Face"
        print "BioID. isenrolled URL - %s" %bioID_service_url
        encodedString = base64.b64encode((self.APP_IDENTIFIER+":"+self.APP_SECRET).encode('utf-8'))
        bioID_service_headers = {"Authorization": "Basic "+encodedString}

        try:
            http_service_response = httpService.executeGet(http_client, bioID_service_url, bioID_service_headers)
            http_response = http_service_response.getHttpResponse()
        except:
            print "BioID. failed to invoke isenrolled API: ", sys.exc_info()[1]
            return None

        try:
            if not httpService.isResponseStastusCodeOk(http_response):
                print "BioID. Face,Periocular not enrolled.  Get non 200 OK response from server:", str(http_response.getStatusLine().getStatusCode())
                httpService.consume(http_response)
                return False

            else: 
                return True
        finally:
            http_service_response.closeConnection()
        
    def processBasicAuthentication(self, credentials):
        userService = CdiUtil.bean(UserService)
        authenticationService = CdiUtil.bean(AuthenticationService)

        user_name = credentials.getUsername()
        user_password = credentials.getPassword()

        logged_in = False
        if StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password):
            logged_in = authenticationService.authenticate(user_name, user_password)

        if not logged_in:
            print "OTP. Process basic authentication. Failed to find user '%s'" % user_name
            return None

        find_user_by_uid = authenticationService.getAuthenticatedUser()
        if find_user_by_uid == None:
            print "OTP. Process basic authentication. Failed to find user '%s'" % user_name
            return None
        
        return find_user_by_uid
    
    
    def performBiometricOperation(self, token, task):
        httpService = CdiUtil.bean(HttpService)
        http_client = httpService.getHttpsClient()
        http_client_params = http_client.getParams()
        bioID_service_url = self.ENDPOINT + task+"?livedetection=true"
        bioID_service_headers = {"Authorization": "Bearer "+token}

        try:
            http_service_response = httpService.executeGet(http_client, bioID_service_url, bioID_service_headers)
            http_response = http_service_response.getHttpResponse()
            response_bytes = httpService.getResponseContent(http_response)
            response_string = httpService.convertEntityToString(response_bytes, Charset.forName("UTF-8"))
            json_response = JSONObject(response_string)
            httpService.consume(http_response)
            if  json_response.get("Success") == True:
                return True
            else:
                print "BioID. Reason for failure : %s " % json_response.get("Error") 
                return False
        except:
            print "BioID. failed to invoke %s API: %s" %(task,sys.exc_info()[1])
            return None
            
        finally:
            http_service_response.closeConnection()
            
            
    