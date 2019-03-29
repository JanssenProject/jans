# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2016, Gluu
#
# Author: Yuriy Movchan
#

from org.xdi.oxauth.security import Identity
from org.xdi.service.cdi.util import CdiUtil
from org.xdi.model.custom.script.type.auth import PersonAuthenticationType
from org.xdi.oxauth.service import UserService, AuthenticationService
from org.xdi.oxauth.service.net import HttpService
from org.xdi.service import XmlService
from org.xdi.oxauth.service import EncryptionService 
from org.xdi.util import StringHelper
from org.xdi.util import ArrayHelper
from java.lang import Boolean

import java
import sys
import json

class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis
        self.client = None

    def init(self, configurationAttributes):
        print "InWebo. Initialization"

        iw_cert_store_type = configurationAttributes.get("iw_cert_store_type").getValue2()
        iw_cert_path = configurationAttributes.get("iw_cert_path").getValue2()
        iw_creds_file = configurationAttributes.get("iw_creds_file").getValue2()

        # Load credentials from file
        f = open(iw_creds_file, 'r')
        try:
            creds = json.loads(f.read())
        except:
            return False
        finally:
            f.close()

        iw_cert_password = creds["CERT_PASSWORD"]
        try:
            encryptionService = CdiUtil.bean(EncryptionService)
            iw_cert_password = encryptionService.decrypt(iw_cert_password)
        except:
            return False

        httpService = CdiUtil.bean(HttpService)
        self.client = httpService.getHttpsClient(None, None, None, iw_cert_store_type, iw_cert_path, iw_cert_password)
        print "InWebo. Initialized successfully"

        return True   

    def destroy(self, configurationAttributes):
        print "InWebo. Destroy"
        print "InWebo. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 1

    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        return True

    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        return None

    def authenticate(self, configurationAttributes, requestParameters, step):
        userService = CdiUtil.bean(UserService)
        authenticationService = CdiUtil.bean(AuthenticationService)

        identity = CdiUtil.bean(Identity)

        iw_api_uri = configurationAttributes.get("iw_api_uri").getValue2()
        iw_service_id = configurationAttributes.get("iw_service_id").getValue2()
        iw_helium_enabled = Boolean(configurationAttributes.get("iw_helium_enabled").getValue2()).booleanValue()

        if (iw_helium_enabled):
            identity.setWorkingParameter("iw_count_login_steps", 1)

        credentials = identity.getCredentials()

        user_name = credentials.getUsername()

        if (step == 1):
            print "InWebo. Authenticate for step 1"

            print "InWebo. Authenticate for step 1. iw_helium_enabled:", iw_helium_enabled
            user_password = credentials.getPassword()
            if (iw_helium_enabled):
                login_array = requestParameters.get("login")
                if ArrayHelper.isEmpty(login_array):
                    print "InWebo. Authenticate for step 1. login is empty"
                    return False
    
                user_name = login_array[0]

                password_array = requestParameters.get("password")
                if ArrayHelper.isEmpty(password_array):
                    print "InWebo. Authenticate for step 1. password is empty"
                    return False
    
                user_password = password_array[0]

                response_validation = self.validateInweboToken(iw_api_uri, iw_service_id, user_name, user_password)
                if (not response_validation):
                    return False

                logged_in = False
                if (StringHelper.isNotEmptyString(user_name)):
                    userService = CdiUtil.bean(UserService)
                    logged_in = authenticationService.authenticate(user_name)
    
                return logged_in
            else:
                logged_in = False
                if (StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password)):
                    userService = CdiUtil.bean(UserService)
                    logged_in = authenticationService.authenticate(user_name, user_password)
    
                return logged_in
            
            return True
        elif (step == 2):
            print "InWebo. Authenticate for step 2"

            passed_step1 = self.isPassedDefaultAuthentication
            if (not passed_step1):
                return False

            iw_token_array = requestParameters.get("iw_token")
            if ArrayHelper.isEmpty(iw_token_array):
                print "InWebo. Authenticate for step 2. iw_token is empty"
                return False

            iw_token = iw_token_array[0]

            response_validation = self.validateInweboToken(iw_api_uri, iw_service_id, user_name, iw_token)
            
            return response_validation
        else:
            return False

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        if (step == 1):
            print "InWebo. Prepare for step 1"
            identity = CdiUtil.bean(Identity)

            iw_helium_enabled = Boolean(configurationAttributes.get("iw_helium_enabled").getValue2()).booleanValue()
            identity.setWorkingParameter("helium_enabled", iw_helium_enabled)

            iw_helium_alias = None
            if (iw_helium_enabled):
                iw_helium_alias = configurationAttributes.get("iw_helium_alias").getValue2()
                identity.setWorkingParameter("helium_alias", iw_helium_alias)

            print "InWebo. Prepare for step 1. Helium status:", iw_helium_enabled

        return True

    def getExtraParametersForStep(self, configurationAttributes, step):
        return None

    def getCountAuthenticationSteps(self, configurationAttributes):
        identity = CdiUtil.bean(Identity)
        if (identity.isSetWorkingParameter("iw_count_login_steps")):
            return identity.getWorkingParameter("iw_count_login_steps")
        
        return 2

    def getPageForStep(self, configurationAttributes, step):
        if (step == 1):
            return "/auth/inwebo/iwlogin.xhtml"
        if (step == 2):
            return "/auth/inwebo/iwauthenticate.xhtml"
        else:
            return ""

    def isPassedDefaultAuthentication(self):
        identity = CdiUtil.bean(Identity)
        credentials = identity.getCredentials()

        user_name = credentials.getUsername()
        passed_step1 = StringHelper.isNotEmptyString(user_name)

        return passed_step1

    def validateInweboToken(self, iw_api_uri, iw_service_id, user_name, iw_token):
        httpService = CdiUtil.bean(HttpService)
        xmlService = CdiUtil.bean(XmlService)

        if StringHelper.isEmpty(iw_token):
            print "InWebo. Token verification. iw_token is empty"
            return False

        request_uri = iw_api_uri + "?action=authenticate" + "&serviceId=" + httpService.encodeUrl(iw_service_id) + "&userId=" + httpService.encodeUrl(user_name) + "&token=" + httpService.encodeUrl(iw_token)
        print "InWebo. Token verification. Attempting to send authentication request:", request_uri
        # Execute request
        http_response = httpService.executeGet(self.client, request_uri)
            
        # Validate response code
        response_validation = httpService.isResponseStastusCodeOk(http_response)
        if response_validation == False:
            print "InWebo. Token verification. Get unsuccessful response code"
            return False

        authentication_response_bytes = httpService.getResponseContent(http_response)
        print "InWebo. Token verification. Get response:", httpService.convertEntityToString(authentication_response_bytes)

        # Validate authentication response
        response_validation = httpService.isContentTypeXml(http_response)
        if response_validation == False:
            print "InWebo. Token verification. Get invalid response"
            return False
        
        # Parse XML response
        try:
            xmlDocument = xmlService.getXmlDocument(authentication_response_bytes)
        except Exception, err:
            print "InWebo. Token verification. Failed to parse XML response:", err
            return False

        result_code = xmlService.getNodeValue(xmlDocument, "/authenticate", None)
        print "InWebo. Token verification. Result after parsing XML response:", result_code
        
        response_validation = StringHelper.equals(result_code, "OK")
        print "InWebo. Token verification. Result validation:", response_validation
        
        return response_validation

    def logout(self, configurationAttributes, requestParameters):
        return True
