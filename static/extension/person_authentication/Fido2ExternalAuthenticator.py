# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2018, Janssen
#
# Author: Yuriy Movchan
#

from javax.ws.rs.core import Response
from org.jboss.resteasy.client import ClientResponseFailure
from org.jboss.resteasy.client.exception import ResteasyClientException
from javax.ws.rs.core import Response
from io.jans.model.custom.script.type.auth import PersonAuthenticationType
from io.jans.fido2.client import Fido2ClientFactory
from io.jans.as.security import Identity
from io.jans.as.service import AuthenticationService, SessionIdService
from io.jans.as.service.common import UserService
from io.jans.as.util import ServerUtil
from io.jans.service.cdi.util import CdiUtil
from io.jans.util import StringHelper

from java.util.concurrent.locks import ReentrantLock

import java
import sys
try:
    import json
except ImportError:
    import simplejson as json

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
        
        print "Fido2. Initialized successfully"
        return True   

    def destroy(self, configurationAttributes):
        print "Fido2. Destroy"
        print "Fido2. Destroyed successfully"
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
        authenticationService = CdiUtil.bean(AuthenticationService)

        identity = CdiUtil.bean(Identity)
        credentials = identity.getCredentials()

        user_name = credentials.getUsername()

        if step == 1:
            print "Fido2. Authenticate for step 1"

            user_password = credentials.getPassword()
            logged_in = False
            if StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password):
                userService = CdiUtil.bean(UserService)
                logged_in = authenticationService.authenticate(user_name, user_password)

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

                if assertionStatus.getStatus() != Response.Status.OK.getStatusCode():
                    print "Fido2. Authenticate for step 2. Get invalid authentication status from Fido2 server"
                    return False

                return True
            elif auth_method == 'enroll':
                print "Fido2. Prepare for step 2. Call Fido2 in order to finish registration flow"
                attestationService = Fido2ClientFactory.instance().createAttestationService(self.metaDataConfiguration)
                attestationStatus = attestationService.verify(token_response)

                if attestationStatus.getStatus() != Response.Status.OK.getStatusCode():
                    print "Fido2. Authenticate for step 2. Get invalid registration status from Fido2 server"
                    return False

                return True
            else:
                print "Fido2. Prepare for step 2. Authentication method is invalid"
                return False

            return False
        else:
            return False

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        identity = CdiUtil.bean(Identity)

        if step == 1:
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

            metaDataConfiguration = self.getMetaDataConfiguration()
            
            assertionResponse = None
            attestationResponse = None

            # Check if user have registered devices
            userService = CdiUtil.bean(UserService)
            countFido2Devices = userService.countFidoAndFido2Devices(userName, self.fido2_domain)
            if countFido2Devices > 0:
                print "Fido2. Prepare for step 2. Call Fido2 endpoint in order to start assertion flow"

                try:
                    assertionService = Fido2ClientFactory.instance().createAssertionService(metaDataConfiguration)
                    assertionRequest = json.dumps({'username': userName}, separators=(',', ':'))
                    assertionResponse = assertionService.authenticate(assertionRequest).readEntity(java.lang.String)
                except ClientResponseFailure, ex:
                    print "Fido2. Prepare for step 2. Failed to start assertion flow. Exception:", sys.exc_info()[1]
                    return False
            else:
                print "Fido2. Prepare for step 2. Call Fido2 endpoint in order to start attestation flow"

                try:
                    attestationService = Fido2ClientFactory.instance().createAttestationService(metaDataConfiguration)
                    attestationRequest = json.dumps({'username': userName, 'displayName': userName, 'attestation' : 'direct'}, separators=(',', ':'))
                    attestationResponse = attestationService.register(attestationRequest).readEntity(java.lang.String)
                except ClientResponseFailure, ex:
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
        return None

    def getCountAuthenticationSteps(self, configurationAttributes):
        return 2

    def getNextStep(self, configurationAttributes, requestParameters, step):
        return -1

    def getPageForStep(self, configurationAttributes, step):
        if step == 2:
            return "/auth/fido2/login.xhtml"

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
            #self.fido2_server_metadata_uri = self.fido2_server_uri + "/fido2/restv1/fido2/configuration"

            metaDataConfigurationService = Fido2ClientFactory.instance().createMetaDataConfigurationService(self.fido2_server_metadata_uri)
    
            max_attempts = 10
            for attempt in range(1, max_attempts + 1):
                try:
                    self.metaDataConfiguration = metaDataConfigurationService.getMetadataConfiguration().readEntity(java.lang.String)
                    return self.metaDataConfiguration
                except ClientResponseFailure, ex:
                    # Detect if last try or we still get Service Unavailable HTTP error
                    if (attempt == max_attempts) or (ex.getResponse().getResponseStatus() != Response.Status.SERVICE_UNAVAILABLE):
                        raise ex
    
                    java.lang.Thread.sleep(3000)
                    print "Attempting to load metadata: %d" % attempt
                except ResteasyClientException, ex:
                    # Detect if last try or we still get Service Unavailable HTTP error
                    if attempt == max_attempts:
                        raise ex
    
                    java.lang.Thread.sleep(3000)
                    print "Attempting to load metadata: %d" % attempt
        finally:
            self.metaDataLoaderLock.unlock()
            
