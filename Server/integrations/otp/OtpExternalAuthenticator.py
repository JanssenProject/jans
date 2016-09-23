# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2016, Gluu
#
# Author: Yuriy Movchan
#

# Requires the following custom properties and values:
#   otp_type: totp/htop
#
# These are non mandatory custom properties and values:
#   otp_policy_name: default
#   send_push_notifaction: false
#   registration_uri: https://ce-dev.gluu.org/identity/register
#   qr_options: { width: 400, height: 400 }

from org.xdi.model.custom.script.type.auth import PersonAuthenticationType
from org.jboss.seam.faces import FacesMessages
from org.jboss.seam.international import StatusMessage
from org.jboss.seam.contexts import Context, Contexts
from org.jboss.seam.security import Identity
from org.xdi.oxauth.service import UserService, AuthenticationService, SessionStateService
from org.xdi.util import StringHelper
from org.xdi.util import ArrayHelper
from org.xdi.oxauth.util import ServerUtil
from org.xdi.oxauth.model.config import Constants
from org.jboss.resteasy.client import ClientResponseFailure
from javax.ws.rs.core import Response
from java.util import Arrays
from org.xdi.oxauth.service.net import HttpService
from org.apache.http.params import CoreConnectionPNames

import sys
import java

try:
    import json
except ImportError:
    import simplejson as json

class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, configurationAttributes):
        print "OTP. Initialization"

        if not configurationAttributes.containsKey("otp_type"):
            print "OTP. Initialization. Property otp_type is mandatory"
            return False

        self.otpType = configurationAttributes.get("otp_type").getValue2()
        if not self.otpType in ["hotp", "totp"]:
            print "OTP. Initialization. Property value otp_type is invalid"
            return False

        validOtpConfiguration = self.loadOtpConfiguration(configurationAttributes)
        if not validOtpConfiguration:
            return False
        
        print "OTP. Initialized successfully"
        return True

    def destroy(self, configurationAttributes):
        print "OTP. Destroy"
        print "OTP. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 1

    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        return True

    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        return None

    def authenticate(self, configurationAttributes, requestParameters, step):
        credentials = Identity.instance().getCredentials()
        user_name = credentials.getUsername()

        context = Contexts.getEventContext()
        session_attributes = context.get("sessionAttributes")

        self.setEventContextParameters(context)

        if (step == 1):
            print "OTP. Authenticate for step 1"
            
            authenticated_user = self.processBasicAuthentication(credentials)
            if authenticated_user == None:
                return False

            otp_auth_method = "authenticate"
            # Uncomment this block if you need to allow user second OTP registration
            #enrollment_mode = ServerUtil.getFirstValue(requestParameters, "loginForm:registerButton")
            #if StringHelper.isNotEmpty(enrollment_mode):
            #    otp_auth_method = "enroll"
            
            if otp_auth_method == "authenticate":
                user_enrollments = self.findEnrollments(credentials)
                if len(user_enrollments) == 0:
                    otp_auth_method = "enroll"
                    print "OTP. Authenticate for step 1. There is no OTP enrollment for user '%s'. Changing otp_auth_method to '%s'" % (user_name, otp_auth_method)

            print "OTP. Authenticate for step 1. otp_auth_method: '%s'" % otp_auth_method

            context.set("otp_auth_method", otp_auth_method)

            return True
        elif (step == 2):
            print "OTP. Authenticate for step 2"

            session_state = SessionStateService.instance().getSessionStateFromCookie()
            if StringHelper.isEmpty(session_state):
                print "OTP. Prepare for step 2. Failed to determine session_state"
                return False

            if user_name == None:
                print "OTP. Authenticate for step 2. Failed to determine user name"
                return False

            # Restore state from session
            otp_auth_method = session_attributes.get("otp_auth_method")
            if not otp_auth_method in ['enroll', 'authenticate']:
                print "OTP. Authenticate for step 2. Failed to authenticate user. otp_auth_method: '%s'" % otp_auth_method
                return False
            
            if otp_auth_method == 'enroll':
                print "OTP. Authenticate for step 2. Skipping this step during enrollment"
                return True

            auth_code = ServerUtil.getFirstValue(requestParameters, "auth_code")
            if StringHelper.isEmpty(auth_code):
                facesMessages = FacesMessages.instance()
                facesMessages.add(StatusMessage.Severity.ERROR, "Failed to authenticate. Authentication code is empty")
                FacesContext.getCurrentInstance().getExternalContext().getFlash().setKeepMessages(True)

                print "OTP. Authenticate for step 2. auth_code is empty"
                return False

            #  Validation auth_code
        elif (step == 3):

            otp_user_external_uid = "otp: %s" % otp_user_device_handle
            print "OTP. Authenticate for step 2. OTP handle: '%s'" % otp_user_external_uid

            if otp_auth_method == "authenticate":
                # Validate if user used device with same keYHandle
                user_enrollments = self.findEnrollments(credentials)
                if len(user_enrollments) == 0:
                    otp_auth_method = "enroll"
                    print "OTP. Authenticate for step 2. There is no OTP enrollment for user '%s'." % user_name
                    return False
                
                for user_enrollment in user_enrollments:
                    if StringHelper.equalsIgnoreCase(user_enrollment, otp_user_device_handle):
                        print "OTP. Authenticate for step 2. There is OTP enrollment for user '%s'. User authenticated successfully" % user_name
                        return True
            else:
                userService = UserService.instance()

                # Double check just to make sure. We did checking in previous step
                # Check if there is user which has otp_user_external_uid
                # Avoid mapping user cert to more than one IDP account
                find_user_by_external_uid = userService.getUserByAttribute("oxExternalUid", otp_user_external_uid)
                if find_user_by_external_uid == None:
                    # Add otp_user_external_uid to user's external GUID list
                    find_user_by_external_uid = userService.addUserAttribute(user_name, "oxExternalUid", otp_user_external_uid)
                    if find_user_by_external_uid == None:
                        print "OTP. Authenticate for step 2. Failed to update current user"
                        return False
    
                    return True

            return False
        else:
            return False

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        credentials = Identity.instance().getCredentials()
        context = Contexts.getEventContext()
        session_attributes = context.get("sessionAttributes")

        self.setEventContextParameters(context)

        if (step == 1):
            return True
        elif (step == 2):
            print "OTP. Prepare for step 2"

            session_state = SessionStateService.instance().getSessionStateFromCookie()
            if StringHelper.isEmpty(session_state):
                print "OTP. Prepare for step 2. Failed to determine session_state"
                return False

            authenticationService = AuthenticationService.instance()
            user = authenticationService.getAuthenticatedUser()
            if (user == None):
                print "OTP. Prepare for step 2. Failed to determine user name"
                return False

            otp_auth_method = session_attributes.get("otp_auth_method")
            if StringHelper.isEmpty(otp_auth_method):
                print "OTP. Prepare for step 2. Failed to determine auth_method"
                return False

            print "OTP. Prepare for step 2. otp_auth_method: '%s'" % otp_auth_method

            otp_obb_auth_method = "OOB_REG"
            otp_obb_server_uri = self.otp_server_uri + "/nnl/v2/reg" 
            if StringHelper.equalsIgnoreCase(otp_auth_method, "authenticate"):
                otp_obb_auth_method = "OOB_AUTH"
                otp_obb_server_uri = self.otp_server_uri + "/nnl/v2/auth" 

            # Prepare START_OBB
            otp_obb_start_request_dictionary = { "operation": "START_%s" % otp_obb_auth_method,
                                                 "userName": user.getUserId(),
                                                 "policyName": "default",
                                                 "oobMode":
                                                    { "qr": "true", "rawData": "false", "push": "false" } 
                                               }

            otp_obb_start_request = json.dumps(otp_obb_start_request_dictionary, separators=(',',':'))
            print "OTP. Prepare for step 2. Prepared START request: '%s' to send to '%s'" % (otp_obb_start_request, otp_obb_server_uri)

            # Request START_OBB
            otp_obb_start_response = self.executePost(otp_obb_server_uri, otp_obb_start_request)
            if otp_obb_start_response == None:
                return False

            print "OTP. Prepare for step 2. Get START response: '%s'" % otp_obb_start_response
            otp_obb_start_response_json = json.loads(otp_obb_start_response)

            # Prepare STATUS_OBB
            #TODO: Remove needDetails parameter
            otp_obb_status_request_dictionary = { "operation": "STATUS_%s" % otp_obb_auth_method,
                                                  "userName": user.getUserId(),
                                                  "needDetails": 1,
                                                  "oobStatusHandle": otp_obb_start_response_json["oobStatusHandle"],
                                                }

            otp_obb_status_request = json.dumps(otp_obb_status_request_dictionary, separators=(',',':'))
            print "OTP. Prepare for step 2. Prepared STATUS request: '%s' to send to '%s'" % (otp_obb_status_request, otp_obb_server_uri)

            context.set("otp_obb_auth_method", otp_obb_auth_method)
            context.set("otp_obb_server_uri", otp_obb_server_uri)
            context.set("otp_obb_start_response", otp_obb_start_response)
            context.set("qr_image", otp_obb_start_response_json["modeResult"]["qrCode"]["qrImage"])
            context.set("otp_obb_status_request", otp_obb_status_request)

            return True
        else:
            return False

    def getExtraParametersForStep(self, configurationAttributes, step):
        return Arrays.asList("otp_auth_method", "otp_obb_auth_method", "otp_obb_server_uri", "otp_obb_start_response")

    def getCountAuthenticationSteps(self, configurationAttributes):
        return 2

    def getPageForStep(self, configurationAttributes, step):
        if (step == 2):
            return "/auth/otp/login.xhtml"

        return ""

    def logout(self, configurationAttributes, requestParameters):
        return True

    def setEventContextParameters(self, context):
        if self.registration_uri != None:
            context.set("external_registration_uri", self.registration_uri)
        context.set("qr_options", self.customQrOptions)

    def processBasicAuthentication(self, credentials):
        userService = UserService.instance()

        user_name = credentials.getUsername()
        user_password = credentials.getPassword()

        logged_in = False
        if StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password):
            logged_in = userService.authenticate(user_name, user_password)

        if not logged_in:
            return None

        find_user_by_uid = userService.getUser(user_name)
        if find_user_by_uid == None:
            print "OTP. Process basic authentication. Failed to find user '%s'" % user_name
            return None
        
        return find_user_by_uid

    def findEnrollments(self, credentials):
        result = []

        userService = UserService.instance()
        user_name = credentials.getUsername()
        user = userService.getUser(user_name, "oxExternalUid")
        if user == None:
            print "OTP. Find enrollments. Failed to find user"
            return result
        
        user_custom_ext_attribute = userService.getCustomAttribute(user, "oxExternalUid")
        if user_custom_ext_attribute == None:
            return result

        otp_prefix = "%s: " % self.otpType
        
        otp_prefix_length = len(otp_prefix) 
        for user_external_uid in user_custom_ext_attribute.getValues():
            index = user_external_uid.find(otp_prefix)
            if index != -1:
                enrollment_uid = user_external_uid[otp_prefix_length:]
                result.append(enrollment_uid)
        
        return result

    def loadOtpConfiguration(self, configurationAttributes):
        print "OTP. Load OTP configuration"
        if not configurationAttributes.containsKey("otp_conf_file"):
            return False

        otp_conf_file = configurationAttributes.get("otp_conf_file").getValue2()

        # Load configuration from file
        f = open(otp_conf_file, 'r')
        try:
            otpConfiguration = json.loads(f.read())
        except:
            print "OTP. Load OTP configuration. Failed to load configuration from file:", otp_conf_file
            return False
        finally:
            f.close()
        
        # Check configuration file settings
        try:
            self.hotpConfiguration = self.otpConfiguration["htop"]
            self.totpConfiguration = self.otpConfiguration["totp"]
            
            hmacShaAlgorithm = self.totpConfiguration["hmacShaAlgorithm"]
            hmacShaAlgorithmType = None

            if StringHelper.equalsIgnoreCase(hmacShaAlgorithm, "sha1"):
                hmacShaAlgorithmType = HmacShaAlgorithm.HMAC_SHA_1
            elif StringHelper.equalsIgnoreCase(hmacShaAlgorithm, "sha256"):
                hmacShaAlgorithmType = HmacShaAlgorithm.HMAC_SHA_256
            elif StringHelper.equalsIgnoreCase(hmacShaAlgorithm, "sha512"):
                hmacShaAlgorithmType = HmacShaAlgorithm.HMAC_SHA_512
            else:
                print "OTP. Load OTP configuration. Invalid TOTP HMAC SHA algorithm: '%s'" % hmacShaAlgorithm
                 
            self.totpConfiguration["hmacShaAlgorithmType"] = hmacShaAlgorithmType
        except:
            print "OTP. Load OTP configuration. Invalid configuration file '%s' format:" % otp_conf_file
            return False
        

        return True

    # Shared HOTP/TOTP methods
    def generateSecretKey(self, keyLength):
        bytes = jarray.zeros(keyLength, "b")
        secureRandom = SecureRandom()
        random.nextBytes(bytes)
        
        return secretKey
    
    # HOTP methods
    def generateSecretHotpKey(self):
        keyLength = self.hotpConfiguration["keyLength"]
        
        return self.generateSecretKey(keyLength)

    def generateHotpKey(self, secretKey, movingFactor):
        digits = self.hotpConfiguration["digits"]

        hotp = HOTP.key(secretKey).digits(digits).movingFactor(movingFactor).build()
        
        return hotp.value()

    def validateHotpKey(self, key, movingFactor, totpKey):
        digits = self.hotpConfiguration["digits"]

        htopValidationResult = HOTPValidator.lookAheadWindow(1).validate(secretKey, movingFactor, digits, totpKey)
        if htopValidationResult.isValid():
            return { "result":True, "movingFactor": htopValidationResult.getNewMovingFactor() }

        return { "result":False, "movingFactor": None }

    def generateHotpSecretKeyUri(self, secretKey):
        digits = self.hotpConfiguration["digits"]

        secretKeyBase32 = self.toBase32(secretKey)
        otpKey = OTPKey(secretKeyBase32, OTPType.TOTP)
        label = issuer + ":%s" % userDisplayName;

        otpAuthURI = OTPAuthURIBuilder.fromKey(otpKey).label(label).issuer(issuer).digits(digits).build();

        return otpAuthURI.toUriString()

    # TOTP methods
    def generateTotpKey(self, secretKey):
        digits = self.totpConfiguration["digits"]
        timeStep = self.totpConfiguration["timeStep"]
        hmacShaAlgorithmType = self.totpConfiguration["hmacShaAlgorithmType"]

        totp = TOTP.key(secretKey).digits(digits).timeStep(TimeUnit.SECONDS.toMillis(timeStep)).digits(6).hmacSha(hmacShaAlgorithmType).build()
        
        return totp.value()

    def validateTotpKey(self, secretKey, totpKey):
        localTotpKey = self.generateTotpKey(secretKey)
        if StringHelper.equals(localTotpKey.value(), totpKey):
            return True
        
        return False

    def generateSecretTotpKey(self):
        keyLength = self.totpConfiguration["keyLength"]
        
        return self.generateSecretKey(keyLength)

    def generateTotpSecretKeyUri(self, secretKey, issuer, userDisplayName):
        digits = self.totpConfiguration["digits"]
        timeStep = self.totpConfiguration["timeStep"]

        secretKeyBase32 = self.toBase32(secretKey)
        otpKey = OTPKey(secretKeyBase32, OTPType.TOTP)
        label = issuer + ":%s" % userDisplayName;

        otpAuthURI = OTPAuthURIBuilder.fromKey(otpKey).label(label).issuer(issuer).digits(digits).timeStep(TimeUnit.SECONDS.toMillis(timeStep)).build();

        return otpAuthURI.toUriString()

    # Utility methods
    def toBase32(self, bytes):
        return BaseEncoding.base32().encode(bytes)

    def toBase64Url(self, bytes):
        return BaseEncoding.base64Url().encode(bytes)

    def fromBase64Url(self, chars):
        return BaseEncoding.base64Url().decode(chars)
