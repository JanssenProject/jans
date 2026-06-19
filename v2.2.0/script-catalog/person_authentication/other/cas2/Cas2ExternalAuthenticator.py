# Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
# Copyright (c) 2020, Janssen Project
#
# Author: Yuriy Movchan
#

import sys
from java.util import Arrays
from java.util import HashMap
from jakarta.faces.context import FacesContext
from org.apache.http.params import CoreConnectionPNames
from io.jans.model.custom.script.type.auth import PersonAuthenticationType
from io.jans.as.server.security import Identity
from io.jans.as.server.service import UserService, AuthenticationService, RequestParameterService
from io.jans.as.server.service.net import HttpService
from io.jans.service.cdi.util import CdiUtil
from io.jans.util import StringHelper, ArrayHelper
from io.jans.jsf2.service import FacesService

class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "CAS2. Initialization"

        if not configurationAttributes.containsKey("cas_host"):
            print "CAS2. Initialization. Parameter 'cas_host' is missing"
            return False

        self.cas_host = configurationAttributes.get("cas_host").getValue2()

        self.cas_extra_opts = None
        if configurationAttributes.containsKey("cas_extra_opts"):
            self.cas_extra_opts = configurationAttributes.get("cas_extra_opts").getValue2()


        self.cas_renew_opt = False
        if configurationAttributes.containsKey("cas_renew_opt"):
            self.cas_renew_opt = StringHelper.toBoolean(configurationAttributes.get("cas_renew_opt").getValue2(), False)

        self.cas_map_user = False
        if configurationAttributes.containsKey("cas_map_user"):
            self.cas_map_user = StringHelper.toBoolean(configurationAttributes.get("cas_map_user").getValue2(), False)

        self.cas_enable_server_validation = False
        if (configurationAttributes.containsKey("cas_validation_uri") and
            configurationAttributes.containsKey("cas_validation_pattern") and
            configurationAttributes.containsKey("cas_validation_timeout")):

            print "CAS2. Initialization. Configuring checker client"
            self.cas_enable_server_validation = True

            self.cas_validation_uri = configurationAttributes.get("cas_validation_uri").getValue2()
            self.cas_validation_pattern = configurationAttributes.get("cas_validation_pattern").getValue2()
            cas_validation_timeout = int(configurationAttributes.get("cas_validation_timeout").getValue2()) * 1000
    
            httpService = CdiUtil.bean(HttpService)
    
            self.http_client = httpService.getHttpsClient()
            self.http_client_params = self.http_client.getParams()
            self.http_client_params.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, cas_validation_timeout)

        self.cas_alt_auth_mode = None
        if configurationAttributes.containsKey("cas_alt_auth_mode"):
            self.cas_alt_auth_mode = configurationAttributes.get("cas_alt_auth_mode").getValue2()

        print "CAS2. Initialized successfully"

        return True   

    def destroy(self, configurationAttributes):
        print "CAS2. Destroy"
        if self.cas_enable_server_validation:
            print "CAS2. CDestory. Destorying checker client"
            self.http_client = None
            
        print "CAS2. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 11

    def getAuthenticationMethodClaims(self, requestParameters):
        return None

    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        if not self.cas_enable_server_validation:
            return True

        print "CAS2. isValidAuthenticationMethod"

        httpService = CdiUtil.bean(HttpService)

        try:
            http_service_response = httpService.executeGet(self.http_client, self.cas_validation_uri)
        except:
            print "CAS2. isValidAuthenticationMethod. Exception: ", sys.exc_info()[1]
            return False

        try:
            http_response = http_service_response.getHttpResponse()
            if http_response.getStatusLine().getStatusCode() != 200:
                print "CAS2. isValidAuthenticationMethod. Get invalid response from CAS2 server: ", str(http_response.getStatusLine().getStatusCode())
                httpService.consume(http_response)
                return False
    
            validation_response_bytes = httpService.getResponseContent(http_response)
            validation_response_string = httpService.convertEntityToString(validation_response_bytes)
            httpService.consume(http_response)
        finally:
            http_service_response.closeConnection()

        if (validation_response_string == None) or (validation_response_string.find(self.cas_validation_pattern) == -1):
            print "CAS2. isValidAuthenticationMethod. Get invalid login page from CAS2 server:"
            return False

        return True

    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        return self.cas_alt_auth_mode

    def authenticate(self, configurationAttributes, requestParameters, step):
        identity = CdiUtil.bean(Identity)
        credentials = identity.getCredentials()

        userService = CdiUtil.bean(UserService)
        requestParameterService = CdiUtil.bean(RequestParameterService)
        authenticationService = CdiUtil.bean(AuthenticationService)
        httpService = CdiUtil.bean(HttpService)

        if step == 1:
            print "CAS2. Authenticate for step 1"
            ticket_array = requestParameters.get("ticket")
            if ArrayHelper.isEmpty(ticket_array):
                print "CAS2. Authenticate for step 1. ticket is empty"
                return False

            ticket = ticket_array[0]
            print "CAS2. Authenticate for step 1. ticket: " + ticket

            if StringHelper.isEmptyString(ticket):
                print "CAS2. Authenticate for step 1. ticket is invalid"
                return False

            # Validate ticket
            facesContext = CdiUtil.bean(FacesContext)
            request = facesContext.getExternalContext().getRequest()

            parametersMap = HashMap()
            parametersMap.put("service", httpService.constructServerUrl(request) + "/postlogin.htm")
            if self.cas_renew_opt:
                parametersMap.put("renew", "true")
            parametersMap.put("ticket", ticket)
            cas_service_request_uri = requestParameterService.parametersAsString(parametersMap)
            cas_service_request_uri = self.cas_host + "/serviceValidate?" + cas_service_request_uri
            if self.cas_extra_opts != None:
                cas_service_request_uri = cas_service_request_uri + "&" + self.cas_extra_opts

            print "CAS2. Authenticate for step 1. cas_service_request_uri: " + cas_service_request_uri

            http_client = httpService.getHttpsClient()
            http_service_response = httpService.executeGet(http_client, cas_service_request_uri)
            try:
                validation_content = httpService.convertEntityToString(httpService.getResponseContent(http_service_response.getHttpResponse()))
            finally:
                http_service_response.closeConnection()

            print "CAS2. Authenticate for step 1. validation_content: " + validation_content
            if StringHelper.isEmpty(validation_content):
                print "CAS2. Authenticate for step 1. Ticket validation response is invalid"
                return False

            cas2_auth_failure = self.parse_tag(validation_content, "cas:authenticationFailure")
            print "CAS2. Authenticate for step 1. cas2_auth_failure: ", cas2_auth_failure

            cas2_user_uid = self.parse_tag(validation_content, "cas:user")
            print "CAS2. Authenticate for step 1. cas2_user_uid: ", cas2_user_uid
            
            if (cas2_auth_failure != None) or (cas2_user_uid == None):
                print "CAS2. Authenticate for step 1. Ticket is invalid"
                return False

            if self.cas_map_user:
                print "CAS2. Authenticate for step 1. Attempting to find user by oxExternalUid: cas2:" + cas2_user_uid

                # Check if the is user with specified cas2_user_uid
                find_user_by_uid = userService.getUserByAttribute("oxExternalUid", "cas2:" + cas2_user_uid)

                if find_user_by_uid == None:
                    print "CAS2. Authenticate for step 1. Failed to find user"
                    print "CAS2. Authenticate for step 1. Setting count steps to 2"
                    identity.setWorkingParameter("cas2_count_login_steps", 2)
                    identity.setWorkingParameter("cas2_user_uid", cas2_user_uid)
                    return True

                found_user_name = find_user_by_uid.getUserId()
                print "CAS2. Authenticate for step 1. found_user_name: " + found_user_name
                
                authenticationService.authenticate(found_user_name)
            
                print "CAS2. Authenticate for step 1. Setting count steps to 1"
                identity.setWorkingParameter("cas2_count_login_steps", 1)

                return True
            else:
                print "CAS2. Authenticate for step 1. Attempting to find user by uid:" + cas2_user_uid

                # Check if there is user with specified cas2_user_uid
                find_user_by_uid = userService.getUser(cas2_user_uid)
                if find_user_by_uid == None:
                    print "CAS2. Authenticate for step 1. Failed to find user"
                    return False

                found_user_name = find_user_by_uid.getUserId()
                print "CAS2. Authenticate for step 1. found_user_name: " + found_user_name

                authenticationService.authenticate(found_user_name)

                print "CAS2. Authenticate for step 1. Setting count steps to 1"
                identity.setWorkingParameter("cas2_count_login_steps", 1)

                return True
        elif step == 2:
            print "CAS2. Authenticate for step 2"

            if identity.isSetWorkingParameter("cas2_user_uid"):
                print "CAS2. Authenticate for step 2. cas2_user_uid is empty"
                return False

            cas2_user_uid = identity.getWorkingParameter("cas2_user_uid")
            passed_step1 = StringHelper.isNotEmptyString(cas2_user_uid)
            if not passed_step1:
                return False

            user_name = credentials.getUsername()
            user_password = credentials.getPassword()

            logged_in = False
            if StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password):
                logged_in = authenticationService.authenticate(user_name, user_password)

            if not logged_in:
                return False

            # Check if there is user which has cas2_user_uid
            # Avoid mapping CAS2 account to more than one IDP account
            find_user_by_uid = userService.getUserByAttribute("oxExternalUid", "cas2:" + cas2_user_uid)

            if find_user_by_uid == None:
                # Add cas2_user_uid to user one id UIDs
                find_user_by_uid = userService.addUserAttribute(user_name, "oxExternalUid", "cas2:" + cas2_user_uid)
                if find_user_by_uid == None:
                    print "CAS2. Authenticate for step 2. Failed to update current user"
                    return False

                return True
            else:
                found_user_name = find_user_by_uid.getUserId()
                print "CAS2. Authenticate for step 2. found_user_name: " + found_user_name
    
                if StringHelper.equals(user_name, found_user_name):
                    return True
        
            return False
        else:
            return False

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        if step == 1:
            print "CAS2. Prepare for step 1"

            requestParameterService = CdiUtil.bean(RequestParameterService)
            httpService = CdiUtil.bean(HttpService)

            facesContext = CdiUtil.bean(FacesContext)
            request = facesContext.getExternalContext().getRequest()

            parametersMap = HashMap()
            parametersMap.put("service", httpService.constructServerUrl(request) + "/postlogin.htm")
            if self.cas_renew_opt:
                parametersMap.put("renew", "true")
            cas_service_request_uri = requestParameterService.parametersAsString(parametersMap)
            cas_service_request_uri = self.cas_host + "/login?" + cas_service_request_uri
            if self.cas_extra_opts != None:
                cas_service_request_uri = cas_service_request_uri + "&" + self.cas_extra_opts

            print "CAS2. Prepare for step 1. cas_service_request_uri: " + cas_service_request_uri
            facesService = CdiUtil.bean(FacesService)
            facesService.redirectToExternalURL(cas_service_request_uri)

            return True
        elif step == 2:
            print "CAS2. Prepare for step 2"

            return True
        else:
            return False

    def getExtraParametersForStep(self, configurationAttributes, step):
        if step == 2:
            return Arrays.asList("cas2_count_login_steps", "cas2_user_uid")
        
        return None

    def getCountAuthenticationSteps(self, configurationAttributes):
        identity = CdiUtil.bean(Identity)
        if identity.isSetWorkingParameter("cas2_count_login_steps"):
            return int(identity.getWorkingParameter("cas2_count_login_steps"))
        
        return 2

    def getPageForStep(self, configurationAttributes, step):
        identity = CdiUtil.bean(Identity)
        if step == 1:
            return "/auth/cas2/cas2login.xhtml"
        return "/auth/cas2/cas2postlogin.xhtml"

    def parse_tag(self, str, tag):
        tag1_pos1 = str.find("<" + tag)
        #  No tag found, return empty string.
        if tag1_pos1 == -1: return None
        tag1_pos2 = str.find(">", tag1_pos1)
        if tag1_pos2 == -1: return None
        tag2_pos1 = str.find("</" + tag, tag1_pos2)
        if tag2_pos1 == -1: return None

        return str[tag1_pos2+1:tag2_pos1].strip()

    def getNextStep(self, configurationAttributes, requestParameters, step):
        return -1

    def getLogoutExternalUrl(self, configurationAttributes, requestParameters):
        print "Get external logout URL call"
        return None

    def logout(self, configurationAttributes, requestParameters):
        return True
