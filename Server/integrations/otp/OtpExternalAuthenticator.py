# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2016, Gluu
#
# Author: Yuriy Movchan
#

# Requires the following custom properties and values:
#   otp_type: totp/htop
#   issuer: Gluu Inc
#   otp_conf_file: /etc/certs/otp_configuration.json
#
# These are non mandatory custom properties and values:
#   label: Gluu OTP
#   qr_options: { width: 400, height: 400 }
#   registration_uri: https://ce-dev.gluu.org/identity/register

from org.xdi.model.custom.script.type.auth import PersonAuthenticationType
from org.gluu.jsf2.message import FacesMessages
from javax.faces.application import FacesMessage
from org.xdi.oxauth.security import Identity
from org.xdi.service.cdi.util import CdiUtil
from org.xdi.oxauth.service import UserService, AuthenticationService, SessionStateService
from org.xdi.util import StringHelper, ArrayHelper
from org.xdi.oxauth.util import ServerUtil
from java.util import Arrays

from java.security import SecureRandom
from java.util.concurrent import TimeUnit

from com.google.common.io import BaseEncoding

from com.lochbridge.oath.otp import TOTP
from com.lochbridge.oath.otp import HOTP
from com.lochbridge.oath.otp import HOTPValidationResult
from com.lochbridge.oath.otp import HOTPValidator
from com.lochbridge.oath.otp import HmacShaAlgorithm

from com.lochbridge.oath.otp.keyprovisioning import OTPAuthURIBuilder
from com.lochbridge.oath.otp.keyprovisioning import OTPKey
from com.lochbridge.oath.otp.keyprovisioning.OTPKey import OTPType

import sys
import java
import jarray

import json

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

        if not configurationAttributes.containsKey("issuer"):
            print "OTP. Initialization. Property issuer is mandatory"
            return False
        self.otpIssuer = configurationAttributes.get("issuer").getValue2()

        self.customLabel = None
        if configurationAttributes.containsKey("label"):
            self.customLabel = configurationAttributes.get("label").getValue2()

        self.customQrOptions = {}
        if configurationAttributes.containsKey("qr_options"):
            self.customQrOptions = configurationAttributes.get("qr_options").getValue2()

        self.registrationUri = None
        if configurationAttributes.containsKey("registration_uri"):
            self.registrationUri = configurationAttributes.get("registration_uri").getValue2()

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
        authenticationService = CdiUtil.bean(AuthenticationService)

        identity = CdiUtil.bean(Identity)
        credentials = identity.getCredentials()
        user_name = credentials.getUsername()

        session_attributes = identity.getSessionState().getSessionAttributes()

        self.setRequestScopedParameters(identity)

        if step == 1:
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
                user_enrollments = self.findEnrollments(user_name)
                if len(user_enrollments) == 0:
                    otp_auth_method = "enroll"
                    print "OTP. Authenticate for step 1. There is no OTP enrollment for user '%s'. Changing otp_auth_method to '%s'" % (user_name, otp_auth_method)
                    
            if otp_auth_method == "enroll":
                print "OTP. Authenticate for step 1. Setting count steps: '%s'" % 3
                identity.setWorkingParameter("otp_count_login_steps", 3)

            print "OTP. Authenticate for step 1. otp_auth_method: '%s'" % otp_auth_method
            identity.setWorkingParameter("otp_auth_method", otp_auth_method)

            return True
        elif step == 2:
            print "OTP. Authenticate for step 2"

            session_state_validation = self.validateSessionState(session_attributes)
            if not session_state_validation:
                return False

            # Restore state from session
            otp_auth_method = session_attributes.get("otp_auth_method")
            if otp_auth_method == 'enroll':
                auth_result = ServerUtil.getFirstValue(requestParameters, "auth_result")
                if not StringHelper.isEmpty(auth_result):
                    print "OTP. Authenticate for step 2. User not enrolled OTP"
                    return False

                print "OTP. Authenticate for step 2. Skipping this step during enrollment"
                return True

            otp_auth_result = self.processOtpAuthentication(requestParameters, user_name, session_attributes, otp_auth_method)
            print "OTP. Authenticate for step 2. OTP authentication result: '%s'" % otp_auth_result

            return otp_auth_result
        elif step == 3:
            print "OTP. Authenticate for step 3"

            session_state_validation = self.validateSessionState(session_attributes)
            if not session_state_validation:
                return False

            # Restore state from session
            otp_auth_method = session_attributes.get("otp_auth_method")
            if otp_auth_method != 'enroll':
                return False

            otp_auth_result = self.processOtpAuthentication(requestParameters, user_name, session_attributes, otp_auth_method)
            print "OTP. Authenticate for step 3. OTP authentication result: '%s'" % otp_auth_result

            return otp_auth_result
        else:
            return False

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        identity = CdiUtil.bean(Identity)
        credentials = identity.getCredentials()
        session_attributes = identity.getSessionState().getSessionAttributes()

        self.setRequestScopedParameters(identity)

        if step == 1:
            print "OTP. Prepare for step 1"

            return True
        elif step == 2:
            print "OTP. Prepare for step 2"

            session_state_validation = self.validateSessionState(session_attributes)
            if not session_state_validation:
                return False

            otp_auth_method = session_attributes.get("otp_auth_method")
            print "OTP. Prepare for step 2. otp_auth_method: '%s'" % otp_auth_method

            if otp_auth_method == 'enroll':
                authenticationService = CdiUtil.bean(AuthenticationService)
                user = authenticationService.getAuthenticatedUser()
                if user == None:
                    print "OTP. Prepare for step 2. Failed to load user enty"
                    return False

                if self.otpType == "hotp":
                    otp_secret_key = self.generateSecretHotpKey()
                    otp_enrollment_request = self.generateHotpSecretKeyUri(otp_secret_key, self.otpIssuer, user.getAttribute("displayName"))
                elif self.otpType == "totp":
                    otp_secret_key = self.generateSecretTotpKey()
                    otp_enrollment_request = self.generateTotpSecretKeyUri(otp_secret_key, self.otpIssuer, user.getAttribute("displayName"))
                else:
                    print "OTP. Prepare for step 2. Unknown OTP type: '%s'" % self.otpType
                    return False

                print "OTP. Prepare for step 2. Prepared enrollment request for user: '%s'" % user.getUserId()
                identity.setWorkingParameter("otp_secret_key", self.toBase64Url(otp_secret_key))
                identity.setWorkingParameter("otp_enrollment_request", otp_enrollment_request)

            return True
        elif step == 3:
            print "OTP. Prepare for step 3"

            session_state_validation = self.validateSessionState(session_attributes)
            if not session_state_validation:
                return False

            otp_auth_method = session_attributes.get("otp_auth_method")
            print "OTP. Prepare for step 3. otp_auth_method: '%s'" % otp_auth_method

            if otp_auth_method == 'enroll':
                return True

        return False

    def getExtraParametersForStep(self, configurationAttributes, step):
        return Arrays.asList("otp_auth_method", "otp_count_login_steps", "otp_secret_key", "otp_enrollment_request")

    def getCountAuthenticationSteps(self, configurationAttributes):
        identity = CdiUtil.bean(Identity)
        session_attributes = identity.getSessionState().getSessionAttributes()

        if session_attributes.containsKey("otp_count_login_steps"):
            return StringHelper.toInteger(session_attributes.get("otp_count_login_steps"))
        else:
            return 2

    def getPageForStep(self, configurationAttributes, step):
        if step == 2:
            identity = CdiUtil.bean(Identity)
            session_attributes = identity.getSessionState().getSessionAttributes()
    
            otp_auth_method = session_attributes.get("otp_auth_method")
            print "OTP. Gep page for step 2. otp_auth_method: '%s'" % otp_auth_method
    
            if otp_auth_method == 'enroll':
                return "/auth/otp/enroll.xhtml"
            else:
                return "/auth/otp/otplogin.xhtml"
        elif step == 3:
            return "/auth/otp/otplogin.xhtml"

        return ""

    def logout(self, configurationAttributes, requestParameters):
        return True

    def setRequestScopedParameters(self, identity):
        if self.registrationUri != None:
            identity.setWorkingParameter("external_registration_uri", self.registrationUri)

        if self.customLabel != None:
            identity.setWorkingParameter("qr_label", self.customLabel)

        identity.setWorkingParameter("qr_options", self.customQrOptions)

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
            self.hotpConfiguration = otpConfiguration["htop"]
            self.totpConfiguration = otpConfiguration["totp"]
            
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
            print "OTP. Load OTP configuration. Invalid configuration file '%s' format. Exception: '%s'" % (otp_conf_file, sys.exc_info()[1])
            return False
        

        return True

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
            print "OTP. Process basic authentication. Failed to find user '%s'" % user_name
            return None
        
        return find_user_by_uid

    def findEnrollments(self, user_name, skipPrefix = True):
        result = []

        userService = CdiUtil.bean(UserService)
        user = userService.getUser(user_name, "oxExternalUid")
        if user == None:
            print "OTP. Find enrollments. Failed to find user"
            return result
        
        user_custom_ext_attribute = userService.getCustomAttribute(user, "oxExternalUid")
        if user_custom_ext_attribute == None:
            return result

        otp_prefix = "%s:" % self.otpType
        
        otp_prefix_length = len(otp_prefix) 
        for user_external_uid in user_custom_ext_attribute.getValues():
            index = user_external_uid.find(otp_prefix)
            if index != -1:
                if skipPrefix:
                    enrollment_uid = user_external_uid[otp_prefix_length:]
                else:
                    enrollment_uid = user_external_uid

                result.append(enrollment_uid)
        
        return result

    def validateSessionState(self, session_attributes):
        session_state = CdiUtil.bean(SessionStateService).getSessionStateFromCookie()
        if StringHelper.isEmpty(session_state):
            print "OTP. Validate session state. Failed to determine session_state"
            return False

        otp_auth_method = session_attributes.get("otp_auth_method")
        if not otp_auth_method in ['enroll', 'authenticate']:
            print "OTP. Validate session state. Failed to authenticate user. otp_auth_method: '%s'" % otp_auth_method
            return False

        return True

    def processOtpAuthentication(self, requestParameters, user_name, session_attributes, otp_auth_method):
        facesMessages = CdiUtil.bean(FacesMessages)
        facesMessages.setKeepMessages()

        userService = CdiUtil.bean(UserService)

        otpCode = ServerUtil.getFirstValue(requestParameters, "loginForm:otpCode")
        if StringHelper.isEmpty(otpCode):
            facesMessages.add(FacesMessage.SEVERITY_ERROR, "Failed to authenticate. OTP code is empty")
            print "OTP. Process OTP authentication. otpCode is empty"

            return False
        
        if otp_auth_method == "enroll":
            # Get key from session
            otp_secret_key_encoded = session_attributes.get("otp_secret_key")
            if otp_secret_key_encoded == None:
                print "OTP. Process OTP authentication. OTP secret key is invalid"
                return False
            
            otp_secret_key = self.fromBase64Url(otp_secret_key_encoded)

            if self.otpType == "hotp":
                validation_result = self.validateHotpKey(otp_secret_key, 1, otpCode)
                
                if (validation_result != None) and validation_result["result"]:
                    print "OTP. Process HOTP authentication during enrollment. otpCode is valid"
                    # Store HOTP Secret Key and moving factor in user entry
                    otp_user_external_uid = "hotp:%s;%s" % ( otp_secret_key_encoded, validation_result["movingFactor"] )

                    # Add otp_user_external_uid to user's external GUID list
                    find_user_by_external_uid = userService.addUserAttribute(user_name, "oxExternalUid", otp_user_external_uid)
                    if find_user_by_external_uid != None:
                        return True

                    print "OTP. Process HOTP authentication during enrollment. Failed to update user entry"
            elif self.otpType == "totp":
                validation_result = self.validateTotpKey(otp_secret_key, otpCode)
                if (validation_result != None) and validation_result["result"]:
                    print "OTP. Process TOTP authentication during enrollment. otpCode is valid"
                    # Store TOTP Secret Key and moving factor in user entry
                    otp_user_external_uid = "totp:%s" % otp_secret_key_encoded

                    # Add otp_user_external_uid to user's external GUID list
                    find_user_by_external_uid = userService.addUserAttribute(user_name, "oxExternalUid", otp_user_external_uid)
                    if find_user_by_external_uid != None:
                        return True

                    print "OTP. Process TOTP authentication during enrollment. Failed to update user entry"
        elif otp_auth_method == "authenticate":
            user_enrollments = self.findEnrollments(user_name)

            if len(user_enrollments) == 0:
                print "OTP. Process OTP authentication. There is no OTP enrollment for user '%s'" % user_name
                facesMessages.add(FacesMessage.SEVERITY_ERROR, "There is no valid OTP user enrollments")
                return False

            if self.otpType == "hotp":
                for user_enrollment in user_enrollments:
                    user_enrollment_data = user_enrollment.split(";")
                    otp_secret_key_encoded = user_enrollment_data[0]

                    # Get current moving factor from user entry
                    moving_factor = StringHelper.toInteger(user_enrollment_data[1])
                    otp_secret_key = self.fromBase64Url(otp_secret_key_encoded)

                    # Validate TOTP
                    validation_result = self.validateHotpKey(otp_secret_key, moving_factor, otpCode)
                    if (validation_result != None) and validation_result["result"]:
                        print "OTP. Process HOTP authentication during authentication. otpCode is valid"
                        otp_user_external_uid = "hotp:%s;%s" % ( otp_secret_key_encoded, moving_factor )
                        new_otp_user_external_uid = "hotp:%s;%s" % ( otp_secret_key_encoded, validation_result["movingFactor"] )
    
                        # Update moving factor in user entry
                        find_user_by_external_uid = userService.replaceUserAttribute(user_name, "oxExternalUid", otp_user_external_uid, new_otp_user_external_uid)
                        if find_user_by_external_uid != None:
                            return True
    
                        print "OTP. Process HOTP authentication during authentication. Failed to update user entry"
            elif self.otpType == "totp":
                for user_enrollment in user_enrollments:
                    otp_secret_key = self.fromBase64Url(user_enrollment)

                    # Validate TOTP
                    validation_result = self.validateTotpKey(otp_secret_key, otpCode)
                    if (validation_result != None) and validation_result["result"]:
                        print "OTP. Process TOTP authentication during authentication. otpCode is valid"
                        return True

        facesMessages.add(FacesMessage.SEVERITY_ERROR, "Failed to authenticate. OTP code is invalid")
        print "OTP. Process OTP authentication. OTP code is invalid"

        return False

    # Shared HOTP/TOTP methods
    def generateSecretKey(self, keyLength):
        bytes = jarray.zeros(keyLength, "b")
        secureRandom = SecureRandom()
        secureRandom.nextBytes(bytes)
        
        return bytes
    
    # HOTP methods
    def generateSecretHotpKey(self):
        keyLength = self.hotpConfiguration["keyLength"]
        
        return self.generateSecretKey(keyLength)

    def generateHotpKey(self, secretKey, movingFactor):
        digits = self.hotpConfiguration["digits"]

        hotp = HOTP.key(secretKey).digits(digits).movingFactor(movingFactor).build()
        
        return hotp.value()

    def validateHotpKey(self, secretKey, movingFactor, totpKey):
        digits = self.hotpConfiguration["digits"]

        htopValidationResult = HOTPValidator.lookAheadWindow(1).validate(secretKey, movingFactor, digits, totpKey)
        if htopValidationResult.isValid():
            return { "result": True, "movingFactor": htopValidationResult.getNewMovingFactor() }

        return { "result": False, "movingFactor": None }

    def generateHotpSecretKeyUri(self, secretKey, issuer, userDisplayName):
        digits = self.hotpConfiguration["digits"]

        secretKeyBase32 = self.toBase32(secretKey)
        otpKey = OTPKey(secretKeyBase32, OTPType.HOTP)
        label = issuer + ":%s" % userDisplayName

        otpAuthURI = OTPAuthURIBuilder.fromKey(otpKey).label(label).issuer(issuer).digits(digits).build()

        return otpAuthURI.toUriString()

    # TOTP methods
    def generateSecretTotpKey(self):
        keyLength = self.totpConfiguration["keyLength"]
        
        return self.generateSecretKey(keyLength)

    def generateTotpKey(self, secretKey):
        digits = self.totpConfiguration["digits"]
        timeStep = self.totpConfiguration["timeStep"]
        hmacShaAlgorithmType = self.totpConfiguration["hmacShaAlgorithmType"]

        totp = TOTP.key(secretKey).digits(digits).timeStep(TimeUnit.SECONDS.toMillis(timeStep)).hmacSha(hmacShaAlgorithmType).build()
        
        return totp.value()

    def validateTotpKey(self, secretKey, totpKey):
        localTotpKey = self.generateTotpKey(secretKey)
        if StringHelper.equals(localTotpKey, totpKey):
            return { "result": True }

        return { "result": False }

    def generateTotpSecretKeyUri(self, secretKey, issuer, userDisplayName):
        digits = self.totpConfiguration["digits"]
        timeStep = self.totpConfiguration["timeStep"]

        secretKeyBase32 = self.toBase32(secretKey)
        otpKey = OTPKey(secretKeyBase32, OTPType.TOTP)
        label = issuer + ":%s" % userDisplayName

        otpAuthURI = OTPAuthURIBuilder.fromKey(otpKey).label(label).issuer(issuer).digits(digits).timeStep(TimeUnit.SECONDS.toMillis(timeStep)).build()

        return otpAuthURI.toUriString()

    # Utility methods
    def toBase32(self, bytes):
        return BaseEncoding.base32().encode(bytes)

    def toBase64Url(self, bytes):
        return BaseEncoding.base64Url().encode(bytes)

    def fromBase64Url(self, chars):
        return BaseEncoding.base64Url().decode(chars)
