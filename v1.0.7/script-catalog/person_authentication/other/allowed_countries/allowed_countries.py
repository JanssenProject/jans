# Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
# Copyright (c) 2020, Janssen Project
#
# Author: Yuriy Movchan
#

from java.util import Arrays
from org.apache.http.params import CoreConnectionPNames
from io.jans.service.cdi.util import CdiUtil
from io.jans.as.server.security import Identity
from io.jans.model.custom.script.type.auth import PersonAuthenticationType
from org.gluu.oxauth.model.config import ConfigurationFactory
from io.jans.as.server.service import UserService, AuthenticationService, SessionIdService
from io.jans.as.server.service.net import HttpService
from io.jans.as.server.util import ServerUtil
from io.jans.util import StringHelper
from io.jans.as.server.service.common import EncryptionService
from java.util import Arrays, HashMap, IdentityHashMap

import java
import datetime
import urllib

import sys
import json


class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript,  configurationAttributes):
        print "Basic. Initialization"
        self.allowedCountries = configurationAttributes.get("allowed_countries").getValue2()
        print "Basic. Initialized successfully"
        return True   

    def destroy(self, configurationAttributes):
        print "Basic. Destroy"
        print "Basic. Destroyed successfully"
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
        identity = CdiUtil.bean(Identity)
        session_attributes = identity.getSessionId().getSessionAttributes()
        authenticationService = CdiUtil.bean(AuthenticationService)
        allowedCountriesListArray = StringHelper.split(self.allowedCountries, ",")
        if (len(allowedCountriesListArray) > 0 and session_attributes.containsKey("remote_ip")):
            remote_ip = session_attributes.get("remote_ip")
	    remote_loc_dic = self.determineGeolocationData(remote_ip)
	    if remote_loc_dic == None:
	        print "Super-Gluu. Prepare for step 2. Failed to determine remote location by remote IP '%s'" % remote_ip
	        return
	    remote_loc = "%s" % ( remote_loc_dic['countryCode'])
            print "Your remote location is "+remote_loc
            if remote_loc in allowedCountriesListArray:
                print "you are allowed to access"
            else:
                return False
      

        if (step == 1):
            print "Basic. Authenticate for step 1"
            identity = CdiUtil.bean(Identity)
            credentials = identity.getCredentials()
            user_name = credentials.getUsername()
            user_password = credentials.getPassword()

            logged_in = False
            if (StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password)):
                logged_in = authenticationService.authenticate(user_name, user_password)

            if (not logged_in):
                return False

            return True
        else:
            return False

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        if (step == 1):
            print "Basic. Prepare for Step 1"
            return True
        else:
            return False

    def getExtraParametersForStep(self, configurationAttributes, step):
        return None

    def getCountAuthenticationSteps(self, configurationAttributes):
        return 1

    def getPageForStep(self, configurationAttributes, step):
        return ""

    def logout(self, configurationAttributes, requestParameters):
        return True

    def determineGeolocationData(self, remote_ip):
        print "Super-Gluu. Determine remote location. remote_ip: '%s'" % remote_ip
        httpService = CdiUtil.bean(HttpService)
        http_client = httpService.getHttpsClient()
        http_client_params = http_client.getParams()
        http_client_params.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 15 * 1000)
        geolocation_service_url = "http://ip-api.com/json/%s?fields=520191" % remote_ip
        geolocation_service_headers = { "Accept" : "application/json" }
        try:
            http_service_response = httpService.executeGet(http_client, geolocation_service_url,  geolocation_service_headers)
            http_response = http_service_response.getHttpResponse()
        except:
            print "Super-Gluu. Determine remote location. Exception: ", sys.exc_info()[1]
            return None

        try:
            if not httpService.isResponseStastusCodeOk(http_response):
                print "Super-Gluu. Determine remote location. Get invalid response from validation server: ", str(http_response.getStatusLine().getStatusCode())
                httpService.consume(http_response)
                return None
            response_bytes = httpService.getResponseContent(http_response)
            response_string = httpService.convertEntityToString(response_bytes)
            httpService.consume(http_response)
        finally:
            http_service_response.closeConnection()

        if response_string == None:
            print "Super-Gluu. Determine remote location. Get empty response from location server"
            return None

     
        response = json.loads(response_string)
        
        if not StringHelper.equalsIgnoreCase(response['status'], "success"):
            print "Super-Gluu. Determine remote location. Get response with status: '%s'" % response['status']
            return None

        return response

    def getNextStep(self, configurationAttributes, requestParameters, step):
        return -1

    def getLogoutExternalUrl(self, configurationAttributes, requestParameters):
        print "Get external logout URL call"
        return None