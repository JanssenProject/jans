from org.jboss.seam.contexts import Context, Contexts
from org.jboss.seam.security import Identity
from org.jboss.seam import Component
from javax.faces.context import FacesContext
from org.jboss.seam import Component
from org.apache.http.entity import ContentType 
from org.xdi.oxauth.service.python.interfaces import ExternalAuthenticatorType
from org.xdi.oxauth.service import UserService
from org.xdi.oxauth.service import AuthenticationService
from org.xdi.oxauth.service.net import HttpService
from org.xdi.util.security import StringEncrypter 
from org.xdi.util import StringHelper
from org.xdi.util import ArrayHelper
from java.util import Arrays

import java
import sys
try:
    import json
except ImportError:
    import simplejson as json

from oneid import OneID

class ExternalAuthenticator(ExternalAuthenticatorType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, configurationAttributes):
        return True   

    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        return True

    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        return None

    def authenticate(self, configurationAttributes, requestParameters, step):
        context = Contexts.getEventContext()
        authenticationService = AuthenticationService.instance()
        userService = UserService.instance()
        httpService = HttpService.instance();

        stringEncrypter = StringEncrypter.defaultInstance()

        server_flag = configurationAttributes.get("oneid_server_flag").getValue2()
        callback_attrs = configurationAttributes.get("oneid_callback_attrs").getValue2()
        creds_file = configurationAttributes.get("oneid_creds_file").getValue2()

        # Create OneID
        authn = OneID(server_flag)

        # Set path to credentials file
        authn.creds_file = creds_file;

        if (step == 1):
            print "OneID authenticate for step 1"

            # Find OneID request
            json_data_array = requestParameters.get("json_data")
            if ArrayHelper.isEmpty(json_data_array):
                print "OneID authenticate for step 1. json_data is empty"
                return False

            request = json_data_array[0]
            print "OneID authenticate for step 1. request: " + request

            if (StringHelper.isEmptyString(request)):
                return False
            
            authn.set_credentials()

            # Validate request
            http_client = httpService.getHttpsClientTrustAll();
            auth_data = httpService.encodeBase64(authn.api_id + ":" + authn.api_key)
            http_response = httpService.executePost(http_client, authn.helper_server + "/validate", auth_data, request, ContentType.APPLICATION_JSON)
            validation_content = httpService.convertEntityToString(httpService.getResponseContent(http_response))
            print "OneID authenticate for step 1. validation_content: " + validation_content
            
            if (StringHelper.isEmptyString(validation_content)):
                return False

            validation_resp = json.loads(validation_content)
            print "OneID authenticate for step 1. validation_resp: " + str(validation_resp)

            if (not authn.success(validation_resp)):
                return False

            response = json.loads(request)
            for x in validation_resp:
                response[x] = validation_resp[x]

            oneid_user_uid = response['uid']
            print "OneID authenticate for step 1. oneid_user_uid: " + oneid_user_uid

            # Check if the is user with specified oneid_user_uid
            find_user_by_uid = userService.getUserByAttribute("oxExternalUid", "oneid:" + oneid_user_uid)

            if (find_user_by_uid == None):
                print "OneID authenticate for step 1. Failed to find user"
                print "OneID authenticate for step 1. Setting count steps to 2"
                context.set("oneid_count_login_steps", 2)
                context.set("oneid_user_uid", stringEncrypter.encrypt(oneid_user_uid))
                return True

            found_user_name = find_user_by_uid.getUserId()
            print "OneID authenticate for step 1. found_user_name: " + found_user_name

            credentials = Identity.instance().getCredentials()
            credentials.setUsername(found_user_name)
            credentials.setUser(find_user_by_uid)
            
            print "OneID authenticate for step 1. Setting count steps to 1"
            context.set("oneid_count_login_steps", 1)

            return True
        elif (step == 2):
            print "OneID authenticate for step 2"
            
            oneid_user_uid_array = requestParameters.get("oneid_user_uid")
            if ArrayHelper.isEmpty(oneid_user_uid_array):
                print "OneID authenticate for step 2. oneid_user_uid is empty"
                return False

            oneid_user_uid = stringEncrypter.decrypt(oneid_user_uid_array[0])
#
            credentials = Identity.instance().getCredentials()

            user_name = credentials.getUsername()
            passed_step1 = StringHelper.isNotEmptyString(user_name)

            if (not passed_step1):
                return False
#
            credentials = Identity.instance().getCredentials()

            user_name = credentials.getUsername()
            user_password = credentials.getPassword()
            logged_in = False
            if (StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password)):
                logged_in = userService.authenticate(user_name, user_password)

            if (not logged_in):
                return False

            # Check if there is user which has oneid_user_uid
            find_user_by_uid = userService.getUserByAttribute("oxExternalUid", "oneid:" + oneid_user_uid)

            if (find_user_by_uid == None):
                # Add oneid_user_uid to user one id UIDs
                find_user_by_uid = userService.addUserAttribute(user_name, "oxExternalUid", "oneid:" + oneid_user_uid)
                if (find_user_by_uid == None):
                    print "OneID authenticate for step 2. Failed to update current user"
                    return False

                return True
            else:
                found_user_name = find_user_by_uid.getUserId()
                print "OneID authenticate for step 2. found_user_name: " + found_user_name
    
                if StringHelper.equals(user_name, found_user_name):
                    return True
        
            return False
        else:
            return False

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        context = Contexts.getEventContext()
        authenticationService = AuthenticationService.instance()

        server_flag = configurationAttributes.get("oneid_server_flag").getValue2()
        callback_attrs = configurationAttributes.get("oneid_callback_attrs").getValue2()
        creds_file = configurationAttributes.get("oneid_creds_file").getValue2()

        # Create OneID
        authn = OneID(server_flag)

        # Set path to credentials file
        authn.creds_file = creds_file; 

        if (step == 1):
            print "OneID prepare for step 1"

            auth_mode_array = requestParameters.get("auth_mode")
            if ArrayHelper.isEmpty(auth_mode_array):
                print "OneID prepare for step 1. auth_mode is empty"
                return False

            request = FacesContext.getCurrentInstance().getExternalContext().getRequest()
            validation_page = request.getContextPath() + "/postlogin.seam?" + "request_uri=&" + authenticationService.parametersAsString()
            print "OneID prepare for step 1. validation_page: " + validation_page

            oneid_login_button = authn.draw_signin_button(validation_page, callback_attrs, True)
            print "OneID prepare for step 1. oneid_login_button: " + oneid_login_button
            
            context.set("oneid_login_button", oneid_login_button)
            context.set("oneid_script_header", authn.script_header)
            context.set("oneid_form_script", authn.oneid_form_script)

            return True
        elif (step == 2):
            print "OneID prepare for step 2"

            return True
        else:
            return False

    def getExtraParametersForStep(self, configurationAttributes, step):
        if (step == 2):
            return Arrays.asList("oneid_user_uid")
        
        return None

    def getCountAuthenticationSteps(self, configurationAttributes):
        context = Contexts.getEventContext()
        if (context.isSet("oneid_count_login_steps")):
            return context.get("oneid_count_login_steps")
        
        return 2

    def getPageForStep(self, configurationAttributes, step):
        if (step == 1):
            return "/login/oneid/oneidlogin.xhtml"
        return "/login/oneid/oneidpostlogin.xhtml"

    def logout(self, configurationAttributes, requestParameters):
        return True

    def getApiVersion(self):
        return 3
