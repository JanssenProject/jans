# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2016, Gluu
#
# Author: Yuriy Movchan
#

# Requires the following custom properties and values:
#   uaf_server_uri: https://ce-dev.gluu.org
#
# These are non mandatory custom properties and values:
#   uaf_policy_name: default
#   send_push_notifaction: false
#   registration_uri: https://ce-dev.gluu.org/identity/register
#   qr_options: { width: 400, height: 400 }

from org.xdi.model.custom.script.type.auth import PersonAuthenticationType
from org.xdi.service.cdi.util import CdiUtil
from org.xdi.oxauth.security import Identity
from org.xdi.oxauth.service import UserService, AuthenticationService, SessionIdService
from org.xdi.util import StringHelper, ArrayHelper
from org.xdi.oxauth.util import ServerUtil
from org.xdi.oxauth.model.config import Constants
from javax.ws.rs.core import Response
from java.util import Arrays
from org.xdi.oxauth.service.net import HttpService
from org.apache.http.params import CoreConnectionPNames

import sys
import java
import json

class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, configurationAttributes):
        print "UAF. Initialization"

        if not configurationAttributes.containsKey("uaf_server_uri"):
            print "UAF. Initialization. Property uaf_server_uri is mandatory"
            return False

        self.uaf_server_uri = configurationAttributes.get("uaf_server_uri").getValue2()

        self.uaf_policy_name = "default"
        if configurationAttributes.containsKey("uaf_policy_name"):
            self.uaf_policy_name = configurationAttributes.get("uaf_policy_name").getValue2()

        self.send_push_notifaction = False
        if configurationAttributes.containsKey("send_push_notifaction"):
            self.send_push_notifaction = StringHelper.toBoolean(configurationAttributes.get("send_push_notifaction").getValue2(), False)

        self.registration_uri = None
        if configurationAttributes.containsKey("registration_uri"):
            self.registration_uri = configurationAttributes.get("registration_uri").getValue2()

        self.customQrOptions = {}
        if configurationAttributes.containsKey("qr_options"):
            self.customQrOptions = configurationAttributes.get("qr_options").getValue2()

        print "UAF. Initializing HTTP client"
        httpService = CdiUtil.bean(HttpService)
        self.http_client = httpService.getHttpsClient()
        http_client_params = self.http_client.getParams()
        http_client_params.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 15 * 1000)

        print "UAF. Initialized successfully. uaf_server_uri: '%s', uaf_policy_name: '%s', send_push_notifaction: '%s', registration_uri: '%s', qr_options: '%s'" % (self.uaf_server_uri, self.uaf_policy_name, self.send_push_notifaction, self.registration_uri, self.customQrOptions)
        
        print "UAF. Initialized successfully"
        return True

    def destroy(self, configurationAttributes):
        print "UAF. Destroy"
        print "UAF. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 1

    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        return True

    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        return None

    def authenticate(self, configurationAttributes, requestParameters, step):
        identity = CdiUtil.bean(Identity)
        credentials = identity.getCredentials()

        session_attributes = identity.getSessionId().getSessionAttributes()

        self.setRequestScopedParameters(identity)

        if (step == 1):
            print "UAF. Authenticate for step 1"

            user_name = credentials.getUsername()

            authenticated_user = self.processBasicAuthentication(credentials)
            if authenticated_user == None:
                return False

            uaf_auth_method = "authenticate"
            # Uncomment this block if you need to allow user second device registration
            #enrollment_mode = ServerUtil.getFirstValue(requestParameters, "loginForm:registerButton")
            #if StringHelper.isNotEmpty(enrollment_mode):
            #    uaf_auth_method = "enroll"
            
            if uaf_auth_method == "authenticate":
                user_enrollments = self.findEnrollments(credentials)
                if len(user_enrollments) == 0:
                    uaf_auth_method = "enroll"
                    print "UAF. Authenticate for step 1. There is no UAF enrollment for user '%s'. Changing uaf_auth_method to '%s'" % (user_name, uaf_auth_method)

            print "UAF. Authenticate for step 1. uaf_auth_method: '%s'" % uaf_auth_method
            
            identity.setWorkingParameter("uaf_auth_method", uaf_auth_method)

            return True
        elif (step == 2):
            print "UAF. Authenticate for step 2"

            session_id = CdiUtil.bean(SessionIdService).getSessionIdFromCookie()
            if StringHelper.isEmpty(session_id):
                print "UAF. Prepare for step 2. Failed to determine session_id"
                return False

            user = authenticationService.getAuthenticatedUser()
            if (user == None):
                print "UAF. Authenticate for step 2. Failed to determine user name"
                return False
            user_name = user.getUserId()

            uaf_auth_result = ServerUtil.getFirstValue(requestParameters, "auth_result")
            if uaf_auth_result != "success":
                print "UAF. Authenticate for step 2. auth_result is '%s'" % uaf_auth_result
                return False

            # Restore state from session
            uaf_auth_method = session_attributes.get("uaf_auth_method")

            if not uaf_auth_method in ['enroll', 'authenticate']:
                print "UAF. Authenticate for step 2. Failed to authenticate user. uaf_auth_method: '%s'" % uaf_auth_method
                return False

            # Request STATUS_OBB
            if True:
                #TODO: Remove this condition
                # It's workaround becuase it's not possible to call STATUS_OBB 2 times. First time on browser and second ime on server
                uaf_user_device_handle = ServerUtil.getFirstValue(requestParameters, "auth_handle")
            else:
                uaf_obb_auth_method = session_attributes.get("uaf_obb_auth_method")
                uaf_obb_server_uri = session_attributes.get("uaf_obb_server_uri")
                uaf_obb_start_response = session_attributes.get("uaf_obb_start_response")

                # Prepare STATUS_OBB
                uaf_obb_start_response_json = json.loads(uaf_obb_start_response)
                uaf_obb_status_request_dictionary = { "operation": "STATUS_%s" % uaf_obb_auth_method,
                                                      "userName": user_name,
                                                      "needDetails": 1,
                                                      "oobStatusHandle": uaf_obb_start_response_json["oobStatusHandle"],
                                                    }
    
                uaf_obb_status_request = json.dumps(uaf_obb_status_request_dictionary, separators=(',',':'))
                print "UAF. Authenticate for step 2. Prepared STATUS request: '%s' to send to '%s'" % (uaf_obb_status_request, uaf_obb_server_uri)

                uaf_status_obb_response = self.executePost(uaf_obb_server_uri, uaf_obb_status_request)
                if uaf_status_obb_response == None:
                    return False

                print "UAF. Authenticate for step 2. Get STATUS response: '%s'" % uaf_status_obb_response
                uaf_status_obb_response_json = json.loads(uaf_status_obb_response)
                
                if uaf_status_obb_response_json["statusCode"] != 4000:
                    print "UAF. Authenticate for step 2. UAF operation status is invalid. statusCode: '%s'" % uaf_status_obb_response_json["statusCode"]
                    return False

                uaf_user_device_handle = uaf_status_obb_response_json["additionalInfo"]["authenticatorsResult"]["handle"]

            if StringHelper.isEmpty(uaf_user_device_handle):
                print "UAF. Prepare for step 2. Failed to get UAF handle"
                return False

            uaf_user_external_uid = "uaf:%s" % uaf_user_device_handle
            print "UAF. Authenticate for step 2. UAF handle: '%s'" % uaf_user_external_uid

            if uaf_auth_method == "authenticate":
                # Validate if user used device with same keYHandle
                user_enrollments = self.findEnrollments(credentials)
                if len(user_enrollments) == 0:
                    uaf_auth_method = "enroll"
                    print "UAF. Authenticate for step 2. There is no UAF enrollment for user '%s'." % user_name
                    return False
                
                for user_enrollment in user_enrollments:
                    if StringHelper.equalsIgnoreCase(user_enrollment, uaf_user_device_handle):
                        print "UAF. Authenticate for step 2. There is UAF enrollment for user '%s'. User authenticated successfully" % user_name
                        return True
            else:
                userService = CdiUtil.bean(UserService)

                # Double check just to make sure. We did checking in previous step
                # Check if there is user which has uaf_user_external_uid
                # Avoid mapping user cert to more than one IDP account
                find_user_by_external_uid = userService.getUserByAttribute("oxExternalUid", uaf_user_external_uid)
                if find_user_by_external_uid == None:
                    # Add uaf_user_external_uid to user's external GUID list
                    find_user_by_external_uid = userService.addUserAttribute(user_name, "oxExternalUid", uaf_user_external_uid)
                    if find_user_by_external_uid == None:
                        print "UAF. Authenticate for step 2. Failed to update current user"
                        return False
    
                    return True

            return False
        else:
            return False

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        authenticationService = CdiUtil.bean(AuthenticationService)

        identity = CdiUtil.bean(Identity)
        credentials = identity.getCredentials()

        session_attributes = identity.getSessionId().getSessionAttributes()

        self.setRequestScopedParameters(identity)

        if (step == 1):
            return True
        elif (step == 2):
            print "UAF. Prepare for step 2"

            session_id = CdiUtil.bean(SessionIdService).getSessionIdFromCookie()
            if StringHelper.isEmpty(session_id):
                print "UAF. Prepare for step 2. Failed to determine session_id"
                return False

            user = authenticationService.getAuthenticatedUser()
            if (user == None):
                print "UAF. Prepare for step 2. Failed to determine user name"
                return False

            uaf_auth_method = session_attributes.get("uaf_auth_method")
            if StringHelper.isEmpty(uaf_auth_method):
                print "UAF. Prepare for step 2. Failed to determine auth_method"
                return False

            print "UAF. Prepare for step 2. uaf_auth_method: '%s'" % uaf_auth_method

            uaf_obb_auth_method = "OOB_REG"
            uaf_obb_server_uri = self.uaf_server_uri + "/nnl/v2/reg" 
            if StringHelper.equalsIgnoreCase(uaf_auth_method, "authenticate"):
                uaf_obb_auth_method = "OOB_AUTH"
                uaf_obb_server_uri = self.uaf_server_uri + "/nnl/v2/auth" 

            # Prepare START_OBB
            uaf_obb_start_request_dictionary = { "operation": "START_%s" % uaf_obb_auth_method,
                                                 "userName": user.getUserId(),
                                                 "policyName": "default",
                                                 "oobMode":
                                                    { "qr": "true", "rawData": "false", "push": "false" } 
                                               }

            uaf_obb_start_request = json.dumps(uaf_obb_start_request_dictionary, separators=(',',':'))
            print "UAF. Prepare for step 2. Prepared START request: '%s' to send to '%s'" % (uaf_obb_start_request, uaf_obb_server_uri)

            # Request START_OBB
            uaf_obb_start_response = self.executePost(uaf_obb_server_uri, uaf_obb_start_request)
            if uaf_obb_start_response == None:
                return False

            print "UAF. Prepare for step 2. Get START response: '%s'" % uaf_obb_start_response
            uaf_obb_start_response_json = json.loads(uaf_obb_start_response)

            # Prepare STATUS_OBB
            #TODO: Remove needDetails parameter
            uaf_obb_status_request_dictionary = { "operation": "STATUS_%s" % uaf_obb_auth_method,
                                                  "userName": user.getUserId(),
                                                  "needDetails": 1,
                                                  "oobStatusHandle": uaf_obb_start_response_json["oobStatusHandle"],
                                                }

            uaf_obb_status_request = json.dumps(uaf_obb_status_request_dictionary, separators=(',',':'))
            print "UAF. Prepare for step 2. Prepared STATUS request: '%s' to send to '%s'" % (uaf_obb_status_request, uaf_obb_server_uri)

            identity.setWorkingParameter("uaf_obb_auth_method", uaf_obb_auth_method)
            identity.setWorkingParameter("uaf_obb_server_uri", uaf_obb_server_uri)
            identity.setWorkingParameter("uaf_obb_start_response", uaf_obb_start_response)
            identity.setWorkingParameter("qr_image", uaf_obb_start_response_json["modeResult"]["qrCode"]["qrImage"])
            identity.setWorkingParameter("uaf_obb_status_request", uaf_obb_status_request)

            return True
        else:
            return False

    def getExtraParametersForStep(self, configurationAttributes, step):
        return Arrays.asList("uaf_auth_method", "uaf_obb_auth_method", "uaf_obb_server_uri", "uaf_obb_start_response")

    def getCountAuthenticationSteps(self, configurationAttributes):
        return 2

    def getPageForStep(self, configurationAttributes, step):
        if (step == 2):
            return "/auth/uaf/login.xhtml"

        return ""

    def logout(self, configurationAttributes, requestParameters):
        return True

    def setRequestScopedParameters(self, identity):
        if self.registration_uri != None:
            identity.setWorkingParameter("external_registration_uri", self.registration_uri)
        identity.setWorkingParameter("qr_options", self.customQrOptions)

    def processBasicAuthentication(self, credentials):
        userService = CdiUtil.bean(UserService)
        authenticationService = CdiUtil.bean(AuthenticationService)

        user_name = credentials.getUsername()
        user_password = credentials.getPassword()

        logged_in = False
        if StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password):
            logged_in = authenticationService.authenticate(user_name, user_password)

        if not logged_in:
            return None

        find_user_by_uid = userService.getUser(user_name)
        if find_user_by_uid == None:
            print "UAF. Process basic authentication. Failed to find user '%s'" % user_name
            return None
        
        return find_user_by_uid

    def findEnrollments(self, credentials):
        result = []

        userService = CdiUtil.bean(UserService)
        user_name = credentials.getUsername()
        user = userService.getUser(user_name, "oxExternalUid")
        if user == None:
            print "UAF. Find enrollments. Failed to find user"
            return result
        
        user_custom_ext_attribute = userService.getCustomAttribute(user, "oxExternalUid")
        if user_custom_ext_attribute == None:
            return result
        
        uaf_prefix = "uaf:"
        uaf_prefix_length = len(uaf_prefix) 
        for user_external_uid in user_custom_ext_attribute.getValues():
            index = user_external_uid.find(uaf_prefix)
            if index != -1:
                enrollment_uid = user_external_uid[uaf_prefix_length:]
                result.append(enrollment_uid)
        
        return result

    def executePost(self, request_uri, request_data):
        httpService = CdiUtil.bean(HttpService)

        request_headers = { "Content-type" : "application/json; charset=UTF-8", "Accept" : "application/json" }

        try:
            http_service_response = httpService.executePost(self.http_client, request_uri, None, request_headers, request_data)
            http_response = http_service_response.getHttpResponse()
        except:
            print "UAF. Validate POST response. Exception: ", sys.exc_info()[1]
            return None

        try:
            if not httpService.isResponseStastusCodeOk(http_response):
                print "UAF. Validate POST response. Get invalid response from  server: %s" % str(http_response.getStatusLine().getStatusCode())
                httpService.consume(http_response)
                return None
    
            response_bytes = httpService.getResponseContent(http_response)
            response_string = httpService.convertEntityToString(response_bytes)
            httpService.consume(http_response)
            
            return response_string
        finally:
            http_service_response.closeConnection()
        return None
