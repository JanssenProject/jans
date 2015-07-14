from org.jboss.seam.contexts import Context, Contexts
from org.jboss.seam.security import Identity
from org.jboss.seam import Component
from javax.faces.context import FacesContext
from org.jboss.seam import Component
from org.apache.http.entity import ContentType 
from org.xdi.model.custom.script.type.auth import PersonAuthenticationType
from org.xdi.oxauth.service import UserService
from org.xdi.oxauth.service import AuthenticationService
from org.xdi.oxauth.service.net import HttpService
from org.xdi.util import StringHelper
from org.xdi.util import ArrayHelper
from org.apache.http.params import CoreConnectionPNames
from java.util import Arrays
from java.util import HashMap

import java
import sys

class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, configurationAttributes):
        print "CAS2. Initialization"
        print "CAS2. Initialized successfully"
        return True   

    def destroy(self, configurationAttributes):
        print "CAS2. Destroy"
        print "CAS2. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 1

    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        print "CAS2. Rest API authenticate isValidAuthenticationMethod"

        if (not (configurationAttributes.containsKey("cas_validation_uri") and
            configurationAttributes.containsKey("cas_validation_pattern") and
            configurationAttributes.containsKey("cas_validation_timeout"))):
            return True

        cas_validation_uri = configurationAttributes.get("cas_validation_uri").getValue2()
        cas_validation_pattern = configurationAttributes.get("cas_validation_pattern").getValue2()
        cas_validation_timeout = int(configurationAttributes.get("cas_validation_timeout").getValue2()) * 1000

        httpService = HttpService.instance();

        http_client = httpService.getHttpsClient();
        http_client_params = http_client.getParams();
        http_client_params.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, cas_validation_timeout);

        try:
            http_service_response = httpService.executeGet(http_client, cas_validation_uri)
            http_response = http_service_response.getHttpResponse()
        except:
            print "CAS2. Rest API authenticate isValidAuthenticationMethod. Exception: ", sys.exc_info()[1]
            return False

        try:
            if (http_response.getStatusLine().getStatusCode() != 200):
                print "CAS2. Rest API authenticate isValidAuthenticationMethod. Get invalid response from CAS2 server: ", str(http_response_ticket.getStatusLine().getStatusCode())
                httpService.consume(http_response)
                return False
    
            validation_response_bytes = httpService.getResponseContent(http_response)
            validation_response_string = httpService.convertEntityToString(validation_response_bytes)
            httpService.consume(http_response)
        finally:
            http_service_response.closeConnection()

        if (validation_response_string == None or validation_response_string.find(cas_validation_pattern) == -1):
            print "CAS2. Rest API authenticate isValidAuthenticationMethod. Get invalid login page from CAS2 server:"
            return False

        return True

    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        cas_alt_auth_mode = configurationAttributes.get("cas_alt_auth_mode").getValue2()
        return cas_alt_auth_mode

    def authenticate(self, configurationAttributes, requestParameters, step):
        context = Contexts.getEventContext()
        authenticationService = AuthenticationService.instance()
        userService = UserService.instance()
        httpService = HttpService.instance();

        cas_host = configurationAttributes.get("cas_host").getValue2()
        cas_map_user = StringHelper.toBoolean(configurationAttributes.get("cas_map_user").getValue2(), False)
        cas_renew_opt = StringHelper.toBoolean(configurationAttributes.get("cas_renew_opt").getValue2(), False)

        cas_extra_opts = None
        if (configurationAttributes.containsKey("cas_extra_opts")):
            cas_extra_opts = configurationAttributes.get("cas_extra_opts").getValue2()

        if (step == 1):
            print "CAS2. Authenticate for step 1"
            ticket_array = requestParameters.get("ticket")
            if ArrayHelper.isEmpty(ticket_array):
                print "CAS2. Authenticate for step 1. ticket is empty"
                return False

            ticket = ticket_array[0]
            print "CAS2. Authenticate for step 1. ticket: " + ticket

            if (StringHelper.isEmptyString(ticket)):
                print "CAS2. Authenticate for step 1. ticket is invalid"
                return False

            # Validate ticket
            request = FacesContext.getCurrentInstance().getExternalContext().getRequest()

            parametersMap = HashMap()
            parametersMap.put("service", httpService.constructServerUrl(request) + "/postlogin")
            if (cas_renew_opt):
                parametersMap.put("renew", "true")
            parametersMap.put("ticket", ticket)
            cas_service_request_uri = authenticationService.parametersAsString(parametersMap)
            cas_service_request_uri = cas_host + "/serviceValidate?" + cas_service_request_uri
            if (cas_extra_opts != None):
                cas_service_request_uri = cas_service_request_uri + "&" + cas_extra_opts

            print "CAS2. Authenticate for step 1. cas_service_request_uri: " + cas_service_request_uri

            http_client = httpService.getHttpsClient();
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
            
            if ((cas2_auth_failure != None) or (cas2_user_uid == None)):
                print "CAS2. Authenticate for step 1. Ticket is invalid"
                return False

            if (cas_map_user):
                print "CAS2. Authenticate for step 1. Attempting to find user by oxExternalUid: cas2:" + cas2_user_uid

                # Check if the is user with specified cas2_user_uid
                find_user_by_uid = userService.getUserByAttribute("oxExternalUid", "cas2:" + cas2_user_uid)

                if (find_user_by_uid == None):
                    print "CAS2. Authenticate for step 1. Failed to find user"
                    print "CAS2. Authenticate for step 1. Setting count steps to 2"
                    context.set("cas2_count_login_steps", 2)
                    context.set("cas2_user_uid", cas2_user_uid)
                    return True

                found_user_name = find_user_by_uid.getUserId()
                print "CAS2. Authenticate for step 1. found_user_name: " + found_user_name

                credentials = Identity.instance().getCredentials()
                credentials.setUsername(found_user_name)
                credentials.setUser(find_user_by_uid)
            
                print "CAS2. Authenticate for step 1. Setting count steps to 1"
                context.set("cas2_count_login_steps", 1)

                return True
            else:
                print "CAS2. Authenticate for step 1. Attempting to find user by uid:" + cas2_user_uid

                # Check if the is user with specified cas2_user_uid
                find_user_by_uid = userService.getUser(cas2_user_uid)
                if (find_user_by_uid == None):
                    print "CAS2. Authenticate for step 1. Failed to find user"
                    return False

                found_user_name = find_user_by_uid.getUserId()
                print "CAS2. Authenticate for step 1. found_user_name: " + found_user_name

                credentials = Identity.instance().getCredentials()
                credentials.setUsername(found_user_name)
                credentials.setUser(find_user_by_uid)

                print "CAS2. Authenticate for step 1. Setting count steps to 1"
                context.set("cas2_count_login_steps", 1)

                return True
        elif (step == 2):
            print "CAS2. Authenticate for step 2"

            sessionAttributes = context.get("sessionAttributes")
            if (sessionAttributes == None) or not sessionAttributes.containsKey("cas2_user_uid"):
                print "CAS2. Authenticate for step 2. cas2_user_uid is empty"
                return False

            cas2_user_uid = sessionAttributes.get("cas2_user_uid")
            passed_step1 = StringHelper.isNotEmptyString(cas2_user_uid)
            if (not passed_step1):
                return False

            credentials = Identity.instance().getCredentials()
            user_name = credentials.getUsername()
            user_password = credentials.getPassword()

            logged_in = False
            if (StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password)):
                logged_in = userService.authenticate(user_name, user_password)

            if (not logged_in):
                return False

            # Check if there is user which has cas2_user_uid
            # Avoid mapping CAS2 account to more than one IDP account
            find_user_by_uid = userService.getUserByAttribute("oxExternalUid", "cas2:" + cas2_user_uid)

            if (find_user_by_uid == None):
                # Add cas2_user_uid to user one id UIDs
                find_user_by_uid = userService.addUserAttribute(user_name, "oxExternalUid", "cas2:" + cas2_user_uid)
                if (find_user_by_uid == None):
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
        context = Contexts.getEventContext()
        authenticationService = AuthenticationService.instance()
        httpService = HttpService.instance();

        cas_host = configurationAttributes.get("cas_host").getValue2()
        cas_renew_opt = StringHelper.toBoolean(configurationAttributes.get("cas_renew_opt").getValue2(), False)

        cas_extra_opts = None
        if (configurationAttributes.containsKey("cas_extra_opts")):
            cas_extra_opts = configurationAttributes.get("cas_extra_opts").getValue2()

        if (step == 1):
            print "CAS2. Prepare for step 1"

            request = FacesContext.getCurrentInstance().getExternalContext().getRequest()
            parametersMap = HashMap()
            parametersMap.put("service", httpService.constructServerUrl(request) + "/postlogin")
            if (cas_renew_opt):
                parametersMap.put("renew", "true")
            cas_service_request_uri = authenticationService.parametersAsString(parametersMap)
            cas_service_request_uri = cas_host + "/login?" + cas_service_request_uri
            if cas_extra_opts != None:
                cas_service_request_uri = cas_service_request_uri + "&" + cas_extra_opts

            print "CAS2. Prepare for step 1. cas_service_request_uri: " + cas_service_request_uri

            context.set("cas_service_request_uri", cas_service_request_uri)

            return True
        elif (step == 2):
            print "CAS2. Prepare for step 2"

            return True
        else:
            return False

    def getExtraParametersForStep(self, configurationAttributes, step):
        if (step == 2):
            return Arrays.asList("cas2_user_uid")
        
        return None

    def getCountAuthenticationSteps(self, configurationAttributes):
        context = Contexts.getEventContext()
        if (context.isSet("cas2_count_login_steps")):
            return context.get("cas2_count_login_steps")
        
        return 2

    def getPageForStep(self, configurationAttributes, step):
        if (step == 1):
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

    def logout(self, configurationAttributes, requestParameters):
        return True
