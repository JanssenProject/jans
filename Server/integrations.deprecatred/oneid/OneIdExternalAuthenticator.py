# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2016, Gluu
#
# Author: Yuriy Movchan
#

import json
from java.util import Arrays
from oneid import OneID
from org.apache.http.entity import ContentType
from org.xdi.model.custom.script.type.auth import PersonAuthenticationType
from org.xdi.oxauth.security import Identity
from org.xdi.oxauth.service import UserService, AuthenticationService
from org.xdi.oxauth.service.net import HttpService
from org.xdi.service.cdi.util import CdiUtil
from org.xdi.util import ArrayHelper
from org.xdi.util import StringHelper


class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, configurationAttributes):
        print "OneId. Initialization"
        print "OneId. Initialized successfully"
        return True   

    def destroy(self, configurationAttributes):
        print "OneId. Destroy"
        print "OneId. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 1

    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        return True

    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        return None

    def authenticate(self, configurationAttributes, requestParameters, step):
        identity = CdiUtil.bean(Identity)

        userService = CdiUtil.bean(UserService)
        authenticationService = CdiUtil.bean(AuthenticationService)
        httpService = CdiUtil.bean(HttpService)

        server_flag = configurationAttributes.get("oneid_server_flag").getValue2()
        callback_attrs = configurationAttributes.get("oneid_callback_attrs").getValue2()
        creds_file = configurationAttributes.get("oneid_creds_file").getValue2()

        # Create OneID
        authn = OneID(server_flag)

        # Set path to credentials file
        authn.creds_file = creds_file

        if (step == 1):
            print "OneId. Authenticate for step 1"

            # Find OneID request
            json_data_array = requestParameters.get("json_data")
            if ArrayHelper.isEmpty(json_data_array):
                print "OneId. Authenticate for step 1. json_data is empty"
                return False

            request = json_data_array[0]
            print "OneId. Authenticate for step 1. request: " + request

            if (StringHelper.isEmptyString(request)):
                return False
            
            authn.set_credentials()

            # Validate request
            http_client = httpService.getHttpsClientDefaulTrustStore()
            auth_data = httpService.encodeBase64(authn.api_id + ":" + authn.api_key)
            http_response = httpService.executePost(http_client, authn.helper_server + "/validate", auth_data, request, ContentType.APPLICATION_JSON)
            validation_content = httpService.convertEntityToString(httpService.getResponseContent(http_response))
            print "OneId. Authenticate for step 1. validation_content: " + validation_content
            
            if (StringHelper.isEmptyString(validation_content)):
                return False

            validation_resp = json.loads(validation_content)
            print "OneId. Authenticate for step 1. validation_resp: " + str(validation_resp)

            if (not authn.success(validation_resp)):
                return False

            response = json.loads(request)
            for x in validation_resp:
                response[x] = validation_resp[x]

            oneid_user_uid = response['uid']
            print "OneId. Authenticate for step 1. oneid_user_uid: " + oneid_user_uid

            # Check if the is user with specified oneid_user_uid
            find_user_by_uid = userService.getUserByAttribute("oxExternalUid", "oneid:" + oneid_user_uid)

            if (find_user_by_uid == None):
                print "OneId. Authenticate for step 1. Failed to find user"
                print "OneId. Authenticate for step 1. Setting count steps to 2"
                identity.setWorkingParameter("oneid_count_login_steps", 2)
                identity.setWorkingParameter("oneid_user_uid", oneid_user_uid)
                return True

            found_user_name = find_user_by_uid.getUserId()
            print "OneId. Authenticate for step 1. found_user_name: " + found_user_name

            identity = CdiUtil.bean(Identity)
            credentials = identity.getCredentials()

            credentials.setUsername(found_user_name)
            credentials.setUser(find_user_by_uid)
            
            print "OneId. Authenticate for step 1. Setting count steps to 1"
            identity.setWorkingParameter("oneid_count_login_steps", 1)

            return True
        elif (step == 2):
            print "OneId. Authenticate for step 2"

            sessionAttributes = identity.getSessionId().getSessionAttributes()
            if (sessionAttributes == None) or not sessionAttributes.containsKey("oneid_user_uid"):
                print "OneId. Authenticate for step 2. oneid_user_uid is empty"
                return False

            oneid_user_uid = sessionAttributes.get("oneid_user_uid")
            passed_step1 = StringHelper.isNotEmptyString(oneid_user_uid)
            if (not passed_step1):
                return False

            identity = CdiUtil.bean(Identity)
            credentials = identity.getCredentials()

            user_name = credentials.getUsername()
            passed_step1 = StringHelper.isNotEmptyString(user_name)

            if (not passed_step1):
                return False

            identity = CdiUtil.bean(Identity)
            credentials = identity.getCredentials()

            user_name = credentials.getUsername()
            user_password = credentials.getPassword()
            logged_in = False
            if (StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password)):
                logged_in = authenticationService.authenticate(user_name, user_password)

            if (not logged_in):
                return False

            # Check if there is user which has oneid_user_uid
            # Avoid mapping OneID account to more than one IDP account
            find_user_by_uid = userService.getUserByAttribute("oxExternalUid", "oneid:" + oneid_user_uid)

            if (find_user_by_uid == None):
                # Add oneid_user_uid to user one id UIDs
                find_user_by_uid = userService.addUserAttribute(user_name, "oxExternalUid", "oneid:" + oneid_user_uid)
                if (find_user_by_uid == None):
                    print "OneId. Authenticate for step 2. Failed to update current user"
                    return False

                return True
            else:
                found_user_name = find_user_by_uid.getUserId()
                print "OneId. Authenticate for step 2. found_user_name: " + found_user_name
    
                if StringHelper.equals(user_name, found_user_name):
                    return True
        
            return False
        else:
            return False

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        identity = CdiUtil.bean(Identity)
        authenticationService = CdiUtil.bean(AuthenticationService)

        server_flag = configurationAttributes.get("oneid_server_flag").getValue2()
        callback_attrs = configurationAttributes.get("oneid_callback_attrs").getValue2()
        creds_file = configurationAttributes.get("oneid_creds_file").getValue2()

        # Create OneID
        authn = OneID(server_flag)

        # Set path to credentials file
        authn.creds_file = creds_file 

        if (step == 1):
            print "OneId. Prepare for step 1"

            facesContext = CdiUtil.bean(FacesContext)
            request = facesContext.getExternalContext().getRequest()
            validation_page = request.getContextPath() + "/postlogin.htm?" + "request_uri=&" + authenticationService.parametersAsString()
            print "OneId. Prepare for step 1. validation_page: " + validation_page

            oneid_login_button = authn.draw_signin_button(validation_page, callback_attrs, True)
            print "OneId. Prepare for step 1. oneid_login_button: " + oneid_login_button
            
            identity.setWorkingParameter("oneid_login_button", oneid_login_button)
            identity.setWorkingParameter("oneid_script_header", authn.script_header)
            identity.setWorkingParameter("oneid_form_script", authn.oneid_form_script)

            return True
        elif (step == 2):
            print "OneId. Prepare for step 2"

            return True
        else:
            return False

    def getExtraParametersForStep(self, configurationAttributes, step):
        if (step == 2):
            return Arrays.asList("oneid_user_uid")
        
        return None

    def getCountAuthenticationSteps(self, configurationAttributes):
        identity = CdiUtil.bean(Identity)
        if (identity.isSetWorkingParameter("oneid_count_login_steps")):
            return identity.getWorkingParameter("oneid_count_login_steps")
        
        return 2

    def getPageForStep(self, configurationAttributes, step):
        if (step == 1):
            return "/auth/oneid/oneidlogin.xhtml"
        return "/auth/oneid/oneidpostlogin.xhtml"

    def logout(self, configurationAttributes, requestParameters):
        return True
