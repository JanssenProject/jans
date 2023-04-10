# Janssen Project software is available under the Apache 2.0 License (2004). See http://www.apache.org/licenses/ for full text.
# Copyright (c) 2023, Janssen Project
#
# Author: Jorge Munoz
#

from io.jans.service.cdi.util import CdiUtil
from io.jans.model.custom.script.type.auth import Fido2InterceptionType
from io.jans.fido2.service.operation import AttestationService
from io.jans.fido2.service.operation import AssertionService
from io.jans.util import StringHelper
from io.jans.as.model.error import ErrorResponseFactory
from jakarta.ws.rs.core import Response
from io.jans.as.model.authorize import AuthorizeErrorResponseType
from org.json import JSONObject
from com.fasterxml.jackson.databind import JsonNode

import java

class Fido2Interception(Fido2InterceptionType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript,  configurationAttributes):
        print "Fido2Interception. Initialization"
        print "Fido2Interception. Initialized successfully"
        return True   

    def destroy(self, configurationAttributes):
        print "Fido2Interception. Destroy"
        print "Fido2Interception. Destroyed successfully"
        return True
               
    def getApiVersion(self):
        return 11

#To generate a bad request WebApplicationException giving a message. This method is to be called inside (interceptRegisterAttestation, interceptVerifyAttestation, interceptAuthenticateAssertion, interceptVerifyAssertion)
    def throwBadRequestException(self, context, message):
	errorResponseFactory = CdiUtil.bean(ErrorResponseFactory)
	errorClaimException = errorResponseFactory.createWebApplicationException(Response.Status.BAD_REQUEST, AuthorizeErrorResponseType.INVALID_REQUEST, message)
	context.setWebApplicationException(errorClaimException); 

#This method is called in Attestation register endpoint, just before to start the registration process
    def interceptRegisterAttestation(self, paramAsJsonNode, context):
	print "Fido2Interception. interceptRegisterAttestation"
	attestationService = CdiUtil.bean(AttestationService)
       
        return True
	
#This method is called in Attestation verify enpoint, just before to start the verification process	
    def interceptVerifyAttestation(self, paramAsJsonNode, context):
	print "Fido2Interception. interceptVerifyAttestation"
	attestationService = CdiUtil.bean(AttestationService)
        return True
	
#This method is called in Assertion authenticate enpoint, just before to start the authentication process		
    def interceptAuthenticateAssertion(self, paramAsJsonNode, context):
	print "Fido2Interception. interceptAuthenticateAssertion"
	assertionService = CdiUtil.bean(AssertionService)
        return True
	
#This method is called in Assertion verify enpoint, just before to start the verification process		
    def interceptVerifyAssertion(self, paramAsJsonNode, context):
	print "Fido2Interception. interceptVerifyAssertion"
	assertionService = CdiUtil.bean(AssertionService)
        return True
