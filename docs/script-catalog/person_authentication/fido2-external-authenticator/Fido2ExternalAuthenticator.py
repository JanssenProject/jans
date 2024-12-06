# Janssen Project software is available under the Apache 2.0 License (2004). See http://www.apache.org/licenses/ for full text.
# Copyright (c) 2020, Janssen Project
#
# Author: Yuriy Movchan, Madhumita Subramaniam
#
from java.net import URLDecoder, URLEncoder
from java.lang import System
from io.jans.model.custom.script.type.auth import PersonAuthenticationType
from io.jans.fido2.client import Fido2ClientFactory
from io.jans.as.server.security import Identity
from io.jans.as.server.service import AuthenticationService
from io.jans.as.server.service import UserService
from io.jans.as.server.service import SessionIdService
from io.jans.as.server.util import ServerUtil
from io.jans.service.cdi.util import CdiUtil
from io.jans.util import StringHelper
from java.util import Arrays
from java.util.concurrent.locks import ReentrantLock
from jakarta.ws.rs import ClientErrorException
from jakarta.ws.rs.core import Response


from io.jans.jsf2.message import FacesMessages
from io.jans.jsf2.service import FacesService
from jakarta.faces.context import FacesContext
from jakarta.faces.application import FacesMessage

from jakarta.servlet.http import Cookie


import java
import sys
import json


class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Fido2. Initialization"

        if not configurationAttributes.containsKey("fido2_server_uri"):
            print "fido2_server_uri. Initialization. Property fido2_server_uri is not specified"
            return False

        self.fido2_server_uri = configurationAttributes.get("fido2_server_uri").getValue2()

        self.fido2_domain = None
        if configurationAttributes.containsKey("fido2_domain"):
            self.fido2_domain = configurationAttributes.get("fido2_domain").getValue2()

        self.metaDataLoaderLock = ReentrantLock()
        self.metaDataConfiguration = None
        
        # name of cookie for allowlist
        self.ALLOW_LIST = "allowList"
        print "Fido2. Initialized successfully"
        return True

    def destroy(self, configurationAttributes):
        print "Fido2. Destroy"
        print "Fido2. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 11

    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        return True

    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        return None

    def authenticate(self, configurationAttributes, requestParameters, step):
        authenticationService = CdiUtil.bean(AuthenticationService)
        identity = CdiUtil.bean(Identity)
        

        token_response = ServerUtil.getFirstValue(requestParameters, "tokenResponse")
        
        if step == 1:
            print "Fido2. Authenticate for step 1"
            logged_in = False
            

            # conditional UI is selected
            if token_response is not None:
                
                print "Fido2. Authenticate for step 1. tokenResponse present"
                
                identity.setWorkingParameter("conditionalUI", "true")
                auth_method = ServerUtil.getFirstValue(requestParameters, "authMethod")
                if auth_method == None:
                    print ("Fido2. Authenticate for step 1. authMethod is empty")
                    return False

                if auth_method == 'authenticate':
                    print "Fido2. Prepare for step 2. Call Fido2 in order to finish authentication flow"
                    assertionService = Fido2ClientFactory.instance().createAssertionService(self.metaDataConfiguration)
                    
                    assertionStatus = assertionService.verify(token_response)
                    authenticationStatusEntity = assertionStatus.readEntity(java.lang.String)
                    print "token_response %s " % token_response
                    print "assertionStatus: %s" % assertionStatus
                    print "assertionStatus.getStatus() : %s" % assertionStatus.getStatus() 
                    print "authenticationStatusEntity : %s" % authenticationStatusEntity 
                    assertionResponse = json.loads(authenticationStatusEntity)
                    
                    username =  assertionResponse.get("username")
                    if assertionStatus.getStatus() != Response.Status.OK.getStatusCode():
                        print "Fido2. Authenticate for step 2. Get invalid authentication status from Fido2 server"
                        return False
                    # get user name and log the user in 
                    logged_in = authenticationService.authenticate(username);

            else:
                
                credentials = identity.getCredentials()
                user_name = credentials.getUsername()
                user_password = credentials.getPassword()
                if StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password) :
                        
                    userService = CdiUtil.bean(UserService)
                    logged_in = authenticationService.authenticate(user_name, user_password)
            
            print ("logged_in : %s " % logged_in)
            
            if not logged_in:
                return False

            return True
        elif step == 2:
            print "Fido2. Authenticate for step 2"

            token_response = ServerUtil.getFirstValue(requestParameters, "tokenResponse")
            if token_response == None:
                print "Fido2. Authenticate for step 2. tokenResponse is empty"
                return False

            auth_method = ServerUtil.getFirstValue(requestParameters, "authMethod")
            if auth_method == None:
                print "Fido2. Authenticate for step 2. authMethod is empty"
                return False

            authenticationService = CdiUtil.bean(AuthenticationService)
            user = authenticationService.getAuthenticatedUser()
            if user == None:
                print "Fido2. Prepare for step 2. Failed to determine user name"
                return False

            if auth_method == 'authenticate':
                print "Fido2. Prepare for step 2. Call Fido2 in order to finish authentication flow"
                assertionService = Fido2ClientFactory.instance().createAssertionService(self.metaDataConfiguration)
                
                assertionStatus = assertionService.verify(token_response)
                authenticationStatusEntity = assertionStatus.readEntity(java.lang.String)
                print "token_response %s " % token_response
                print "assertionStatus: %s" % assertionStatus
                print "assertionStatus.getStatus() : %s" % assertionStatus.getStatus() 
                print "authenticationStatusEntity : %s" % authenticationStatusEntity 
                if assertionStatus.getStatus() != Response.Status.OK.getStatusCode():
                    print "Fido2. Authenticate for step 2. Get invalid authentication status from Fido2 server"
                    return False

                return True
            elif auth_method == 'enroll':
                print "Fido2. Prepare for step 2. Call Fido2 in order to finish registration flow"
                attestationService = Fido2ClientFactory.instance().createAttestationService(self.metaDataConfiguration)
                
                attestationStatus = attestationService.verify(token_response)
                print "Fido2. token_response %s " % token_response
                print "Fido2. attestationStatus: %s" % attestationStatus
                print "Fido2. attestationStatus.getStatus() : %s" % attestationStatus.getStatus() 
                attestationStatusEntity = attestationStatus.readEntity(java.lang.String)
                
                print "attestationStatusEntity : %s" % attestationStatusEntity 
                if attestationStatus.getStatus() != Response.Status.OK.getStatusCode():
                    print "Fido2. Authenticate for step 2. Get invalid registration status from Fido2 server"
                    return False
                
                attestationResponse = json.loads(attestationStatusEntity)
                    
                new_credential = attestationResponse.get("credential") 
                print new_credential
                self.persistCookie(new_credential)
                return True
            else:
                print "Fido2. Prepare for step 2. Authentication method is invalid"
                return False

            return False
        else:
            return False

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        identity = CdiUtil.bean(Identity)
        metaDataConfiguration = self.getMetaDataConfiguration()
        facesContext = CdiUtil.bean(FacesContext)
        domain = facesContext.getExternalContext().getRequest().getServerName()
        print ("domain %s : " % domain)
        assertionService = Fido2ClientFactory.instance().createAssertionService(metaDataConfiguration)
        allowList = self.getCookieValue();
        if step == 1:
            try:
                print "Fido2. Prepare for step 1. Call Fido2 endpoint in order to start assertion flow"
                assertionRequest = json.dumps({ 'origin': domain, 'allowCredentials': allowList}, separators=(',', ':'))
                print ("Assertion Request : %s" % assertionRequest)
                assertionResponse = assertionService.authenticate(assertionRequest).readEntity(java.lang.String)
                print "assertionResponse %s " % assertionResponse
                identity.setWorkingParameter("fido2_assertion_request", ServerUtil.asJson(assertionResponse))

            except ClientErrorException, ex:
                    print "Fido2. Prepare for step 1. Failed to start assertion flow. Exception:", sys.exc_info()[1]
                    return False
            return True
        elif step == 2:
            print "Fido2. Prepare for step 2"

            session = CdiUtil.bean(SessionIdService).getSessionId()
            if session == None:
                print "Fido2. Prepare for step 2. Failed to determine session_id"
                return False

            authenticationService = CdiUtil.bean(AuthenticationService)
            user = authenticationService.getAuthenticatedUser()
            if user == None:
                print "Fido2. Prepare for step 2. Failed to determine user name"
                return False

            userName = user.getUserId()
            assertionResponse = None
            attestationResponse = None
            # Check if user have registered devices
            count = CdiUtil.bean(UserService).countFido2RegisteredDevices(userName, domain)

            if count > 0:
                print "Fido2. Prepare for step 2. Call Fido2 endpoint in order to start assertion flow"
                try:
                    assertionRequest = json.dumps({'username': userName, 'origin': domain}, separators=(',', ':'))
                    assertionResponse = assertionService.authenticate(assertionRequest).readEntity(java.lang.String)
                    print "assertionResponse %s " % assertionResponse
                    
                except ClientErrorException, ex:
                    print "Fido2. Prepare for step 2. Failed to start assertion flow. Exception:", sys.exc_info()[1]
                    return False
            else:
                print "Fido2. Prepare for step 2. Call Fido2 endpoint in order to start attestation flow"

                try:
                    attestationService = Fido2ClientFactory.instance().createAttestationService(metaDataConfiguration)
                    basic_json = {'username': userName, 'displayName': userName, 'origin': domain}
                    print " basic_json %s" % basic_json

                    attestationRequest = json.dumps(basic_json)
                    attestationResponse = attestationService.register(attestationRequest).readEntity(java.lang.String)
                except ClientErrorException, ex:
                    print "Fido2. Prepare for step 2. Failed to start attestation flow. Exception:", sys.exc_info()[1]
                    return False

            identity.setWorkingParameter("fido2_assertion_request", ServerUtil.asJson(assertionResponse))
            identity.setWorkingParameter("fido2_attestation_request", ServerUtil.asJson(attestationResponse))
            print "Fido2. Prepare for step 2. Successfully start flow with next requests.\nfido2_assertion_request: '%s'\nfido2_attestation_request: '%s'" % ( assertionResponse, attestationResponse )

            return True
        elif step == 3:
            print "Fido2. Prepare for step 3"

            return True
        else:
            return False

    def getExtraParametersForStep(self, configurationAttributes, step):
        return Arrays.asList( "conditionalUI","fido2_assertion_request","fido2_attestation_request")

    def getCountAuthenticationSteps(self, configurationAttributes):
        identity = CdiUtil.bean(Identity)
        conditionalUI = identity.getWorkingParameter("conditionalUI")
        print ("conditionalUI : %s" % conditionalUI)
        if(conditionalUI == "true"): 
            return 1
        return 2

    def getNextStep(self, configurationAttributes, requestParameters, step):
        return -1

    def getPageForStep(self, configurationAttributes, step):
        if step == 1:
            return "/auth/fido2/login.xhtml"
        elif step == 2:
            identity = CdiUtil.bean(Identity)
            return "/auth/fido2/passkeys.xhtml"
            
        return ""

    def logout(self, configurationAttributes, requestParameters):
        return True

    def getAuthenticationMethodClaims(self, requestParameters):
        return None

    def getLogoutExternalUrl(self, configurationAttributes, requestParameters):
        print "Get external logout URL call"
        return None

    def getMetaDataConfiguration(self):
        if self.metaDataConfiguration != None:
            return self.metaDataConfiguration

        self.metaDataLoaderLock.lock()
        # Make sure that another thread not loaded configuration already
        if self.metaDataConfiguration != None:
            return self.metaDataConfiguration

        try:
            print "Fido2. Initialization. Downloading Fido2 metadata"
            self.fido2_server_metadata_uri = self.fido2_server_uri + "/.well-known/fido2-configuration"

            metaDataConfigurationService = Fido2ClientFactory.instance().createMetaDataConfigurationService(self.fido2_server_metadata_uri)

            max_attempts = 10
            for attempt in range(1, max_attempts + 1):
                try:
                    self.metaDataConfiguration = metaDataConfigurationService.getMetadataConfiguration().readEntity(java.lang.String)
                    return self.metaDataConfiguration
                except ClientErrorException, ex:
                    # Detect if last try or we still get Service Unavailable HTTP error
                    if (attempt == max_attempts) or (ex.getResponse().getResponseStatus() != Response.Status.SERVICE_UNAVAILABLE):
                        raise ex

                    java.lang.Thread.sleep(3000)
                    print "Attempting to load metadata: %d" % attempt
        finally:
            self.metaDataLoaderLock.unlock()


    def getCookieValue(self):
    # sample allow list -  [{ id: ...., type: 'public-key', transports: ['usb', 'ble', 'nfc']}]
      
        value = []
        coo =  None
        httpRequest = ServerUtil.getRequestOrNull()
        
        if httpRequest != None:
            print "Cookies : %s" % httpRequest.getCookies()
            for cookie in httpRequest.getCookies():
                if cookie.getName() == "allowList":
                   coo = cookie
        
        if coo == None:
            print "Passkeys. getCookie. No cookie found"
        else:
            print "Passkeys. getCookie. Found cookie"
            
            try:
                now = System.currentTimeMillis()
                value = URLDecoder.decode(coo.getValue(), "utf-8")
                # value is an array of objects with properties: id, type, transports
                value = json.loads(value)
                
                print value
            except:
                print "Passkeys. getCookie. Unparsable value, dropping cookie..."
            
        return value
        
    def persistCookie(self, credential):
        try:
            now = System.currentTimeMillis()
            allowList = self.add_credential_if_not_exists( credential)
            value = json.dumps(allowList, separators=(',',':'))
            value = URLEncoder.encode(value, "utf-8")
            coo = Cookie("allowList", value)
            coo.setSecure(True)
            coo.setHttpOnly(True)
            # One week
            coo.setMaxAge(7 * 24 * 60 * 60)
            
            response = self.getHttpResponse()
            if response != None:
                print "Passkeys. persistCookie. Adding cookie to response"
                response.addCookie(coo)
        except:
            print "Passkeys. persistCookie. Exception: ", sys.exc_info()[1]
            
            
    def add_credential_if_not_exists(self,  new_credential):
    
        allowList = self.getCookieValue()
        # Check if the id of the new element already exists in the array
        for json_obj in allowList:
            if json_obj["id"] == new_credential["id"]:
                print("Credential with id %s already exists." % new_credential['id'])
            
        # If the id doesn't exist, append the new element
        allowList.append(new_credential)
        print("new_credential with id %s added successfully." % new_credential['id'])
        return allowList
        
    def getHttpResponse(self):
        try:
            return FacesContext.getCurrentInstance().getExternalContext().getResponse()
        except:
            print "Passkeys.  Error accessing HTTP response object: ", sys.exc_info()[1]
            return None