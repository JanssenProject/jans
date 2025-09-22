# Janssen Project software is available under the Apache 2.0 License (2004). See http://www.apache.org/licenses/ for full text.
# Copyright (c) 2023, Janssen Project
#
# Author: Jorge Munoz
# Author: Yuriy Movchan
#
from io.jans.service.cdi.util import CdiUtil
from io.jans.model.custom.script.type.fido2 import Fido2ExtensionType
from io.jans.fido2.service.operation import AttestationService
from io.jans.fido2.service.operation import AssertionService
from io.jans.util import StringHelper
from org.json import JSONObject
from com.fasterxml.jackson.databind import JsonNode
from org.apache.logging.log4j import ThreadContext
from io.jans.fido2.model.u2f.error import Fido2ErrorResponseFactory
from io.jans.fido2.model.u2f.error import Fido2ErrorResponseType
from io.jans.as.model.config import Constants

from java.lang import String

class Fido2Extension(Fido2ExtensionType):

    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Fido2Extension. Initialization"
        print "Fido2Extension. Initialized successfully"
        return True   

    def destroy(self, configurationAttributes):
        print "Fido2Extension. Destroy"
        print "Fido2Extension. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 11

    # To generate a bad request WebApplicationException giving a message. This method is to be called inside (interceptRegisterAttestation, interceptVerifyAttestation, interceptAuthenticateAssertion, interceptVerifyAssertion)
    def throwBadRequestException(self, title, message, context):
        print "Fido2Extension. Setting Bad request exception"

        errorClaimException = Fido2ErrorResponseFactory.createBadRequestException(Fido2ErrorResponseType.BAD_REQUEST_INTERCEPTION, title, message, ThreadContext.get(Constants.CORRELATION_ID_HEADER))
        context.setWebApplicationException(errorClaimException)

    # This method is called in Attestation register endpoint before start the registration process
    def registerAttestationStart(self, paramAsJsonNode, context):
        print "Fido2Extension. registerAttestationStart"
        attestationService = CdiUtil.bean(AttestationService)

        return True

    # This method is called in Attestation register endpoint after start the registration process
    def registerAttestationFinish(self, paramAsJsonNode, context):
        print "Fido2Extension. registerAttestationFinish"
        attestationService = CdiUtil.bean(AttestationService)

        return True

    # This method is called in Attestation verify endpoint before finish the registration verification process
    def verifyAttestationStart(self, paramAsJsonNode, context):
        print "Fido2Extension. verifyAttestationStart"
        attestationService = CdiUtil.bean(AttestationService)

        return True

    # This method is called in Attestation verify endpoint after finish the registration verification process
    def verifyAttestationFinish(self, paramAsJsonNode, context):
        print "Fido2Extension. verifyAttestationFinish"
        attestationService = CdiUtil.bean(AttestationService)

        return True

    # This method is called in Assertion authenticate endpoint before start the authentication process
    def authenticateAssertionStart(self, paramAsJsonNode, context):
        print "Fido2Extension. authenticateAssertionStart"

        assertionService = CdiUtil.bean(AssertionService)

        if paramAsJsonNode.hasNonNull("username"):
            print "Fido2Extension. Username: '%s'" % paramAsJsonNode.get("username").asText()
            if paramAsJsonNode.get("username").asText() == 'test_user':
                self.throwBadRequestException("Fido2Extension authenticateAssertionStart : test_user", "Description Error from script : test_user", context)
        else:
            self.throwBadRequestException("Fido2Extension authenticateAssertionStart. Username is missing.", "Description Error from script. Username is missing.", context)

        return True

    # This method is called in Assertion authenticate endpoint after start the authentication process
    def authenticateAssertionFinish(self, paramAsJsonNode, context):
        print "Fido2Extension. authenticateAssertionFinish"
        assertionService = CdiUtil.bean(AssertionService)

        return True

    # This method is called in Assertion verify endpoint before finish the authentication verification process
    def verifyAssertionStart(self, paramAsJsonNode, context):
        print "Fido2Extension. verifyAssertionStart"
        assertionService = CdiUtil.bean(AssertionService)
        
        return True

    # This method is called in Assertion verify endpoint after finish the authentication verification process
    def verifyAssertionFinish(self, paramAsJsonNode, context):
        print "Fido2Extension. verifyAssertionFinish"
        assertionService = CdiUtil.bean(AssertionService)
        
        return True
