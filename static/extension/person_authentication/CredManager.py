# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2018, Gluu
#
# Author: Jose Gonzalez

from org.xdi.oxauth.security import Identity
from org.xdi.oxauth.service import AuthenticationService, UserService, SessionIdService, EncryptionService
from org.xdi.model.custom.script.type.auth import PersonAuthenticationType
from org.xdi.oxauth.model.config import Constants, ConfigurationFactory
from org.xdi.oxauth.service.fido.u2f import DeviceRegistrationService
from org.xdi.oxauth.client.fido.u2f import FidoU2fClientFactory
from org.xdi.util import StringHelper
from org.xdi.service.cdi.util import CdiUtil
from org.xdi.oxauth.util import ServerUtil
from org.xdi.oxauth.service.custom import CustomScriptService
from org.xdi.model.custom.script import CustomScriptType
from java.util import Collections, HashMap, ArrayList, Arrays, Date
from java.lang import System
from java.nio.file import Paths
from java.nio.charset import Charset

from com.twilio import Twilio
import com.twilio.rest.api.v2010.account.Message as TwMessage
from com.twilio.type import PhoneNumber
from org.codehaus.jettison.json import JSONArray
from com.google.common.io import BaseEncoding

from com.lochbridge.oath.otp import TOTP
from com.lochbridge.oath.otp import HOTP
from com.lochbridge.oath.otp import HOTPValidationResult
from com.lochbridge.oath.otp import HOTPValidator
from com.lochbridge.oath.otp import HmacShaAlgorithm

from com.lochbridge.oath.otp.keyprovisioning import OTPAuthURIBuilder
from com.lochbridge.oath.otp.keyprovisioning import OTPKey
from com.lochbridge.oath.otp.keyprovisioning.OTPKey import OTPType

from java.util.concurrent import TimeUnit

from org.gluu.jsf2.message import FacesMessages
from javax.faces.application import FacesMessage

from com.google.android.gcm.server import Sender, Message
from com.notnoop.apns import APNS
from org.xdi.oxauth.service.push.sns import PushPlatform, PushSnsService
from org.gluu.oxnotify.client import NotifyClientFactory
from org.xdi.oxauth.service.net import HttpService
from org.apache.http.params import CoreConnectionPNames

import random
import java
import datetime
import urllib
import sys

try:
    import json
except ImportError:
    import simplejson as json

class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, configurationAttributes):

        self.ACR_SG='super_gluu'
        self.ACR_SMS='twilio_sms'
        self.ACR_OTP='otp'
        self.ACR_U2F='u2f'
        self.ACRs=[self.ACR_SG, self.ACR_SMS, self.ACR_OTP, self.ACR_U2F]
        self.u2f_app_id=configurationAttributes.get("u2f_app_id").getValue2()
        self.supergluu_app_id=configurationAttributes.get("supergluu_app_id").getValue2()

        print "Cred-manager. init"

        custScriptService=CdiUtil.bean(CustomScriptService)
        scriptsList = custScriptService.findCustomScripts(Collections.singletonList(CustomScriptType.PERSON_AUTHENTICATION), "oxConfigurationProperty", "displayName", "gluuStatus")

        cfgMap=HashMap()
        for custom_script in scriptsList:
            sname=custom_script.getName()
            if custom_script.isEnabled() and (sname in self.ACRs):
                innermap=HashMap()
                for prop in custom_script.getConfigurationProperties():
                    innermap.put(prop.getValue1(), prop.getValue2())
                cfgMap.put(sname, innermap)

        if cfgMap.keySet().contains(self.ACR_OTP):
            if not self.loadOtpConfigurations(cfgMap.get(self.ACR_OTP).get("otp_conf_file"), cfgMap):
                print "Cred-manager. init. Problem parsing otp configs, check custom script settings"
                cfgMap.remove(self.ACR_OTP)

        if cfgMap.keySet().contains(self.ACR_SG):
            if not self.initPushNotificationService(cfgMap.get(self.ACR_SG), cfgMap):
                print "Cred-manager. init. Could not initialize push services, check custom script settings"
                cfgMap.remove(self.ACR_SG)

        self.scriptsConfig=cfgMap

        print "Cred-manager. init. Loaded configs %s" % cfgMap.keySet().toString()
        print "Cred-manager. init. Initialized successfully"
        return True

    def destroy(self, configurationAttributes):
        print "Cred-manager. Destroy"
        print "Cred-manager. Destroyed successfully"

        return True

    def getApiVersion(self):
        return 2

    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        print "Cred-manager. isValidAuthenticationMethod called"
        return True

    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        return None

    def authenticate(self, configurationAttributes, requestParameters, step):

        print "Cred-manager. authenticate %s" % str(step)
        userService = CdiUtil.bean(UserService)
        authenticationService = CdiUtil.bean(AuthenticationService)
        identity = CdiUtil.bean(Identity)

        facesMessages = CdiUtil.bean(FacesMessages)
        facesMessages.setKeepMessages()

        if step == 1:
            credentials = identity.getCredentials()
            user_name = credentials.getUsername()
            user_password = credentials.getPassword()

            if StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password):
                logged_in = authenticationService.authenticate(user_name, user_password)

                if logged_in:
                    foundUser = authenticationService.getAuthenticatedUser()

                    if foundUser == None:
                        print "Cred-manager. authenticate for step 1. Cannot retrieve logged user"
                    else:
                        acr=foundUser.getAttribute("oxPreferredMethod")

                        if acr == None:
                            identity.setWorkingParameter("skip2FA", True)
                            return True
                        elif not acr in self.ACRs:
                            print "%s not a valid cred-manager acr" % acr
                        else:
                            #Determine whether to skip 2FA based on policy defined (global or user custom)
                            skip2FA = self.determineSkip2FA(userService, identity, foundUser, ServerUtil.getFirstValue(requestParameters, "loginForm:platform"))
                            identity.setWorkingParameter("skip2FA", skip2FA)
                            identity.setWorkingParameter("ACR", acr)

                            return True

            return False

        elif step == 2:
            user = authenticationService.getAuthenticatedUser()
            if user == None:
                print "Cred-manager. authenticate for step 2. Cannot retrieve logged user"
                return False

            #see cred-manager.xhtml
            alter = ServerUtil.getFirstValue(requestParameters, "alternativeMethod")
            if alter!=None:
                #bypass authentication if an alternative method was provided. This step will be retried (see getNextStep)
                return True

            session_attributes=identity.getSessionId().getSessionAttributes()
            acr = session_attributes.get("ACR")
            #this working parameter is used in cred-manager.xhtml
            identity.setWorkingParameter("methods", self.getAvailMethodsUser(user, acr))

            success = False

            if acr==self.ACR_U2F:
                token_response = ServerUtil.getFirstValue(requestParameters, "tokenResponse")

                if token_response == None:
                    print "Cred-manager. authenticate. tokenResponse is empty"
                else:
                    success = self.finishU2fAuthentication(user.getUserId(), token_response)
                    print "Cred-manager. authenticate. U2F finish authentication result was %s" % success

            elif acr==self.ACR_SMS:
                code=session_attributes.get("randCode")
                form_passcode = ServerUtil.getFirstValue(requestParameters, "passcode")

                if form_passcode!=None and code==form_passcode:
                    print "Cred-manager. authenticate. 6-digit code matches with code sent via SMS"
                    success = True
                else:
                    facesMessages.add(FacesMessage.SEVERITY_ERROR, "Wrong code entered")

            elif acr==self.ACR_OTP:
                otpCfg=self.scriptsConfig.get(self.ACR_OTP)
                otpCode = ServerUtil.getFirstValue(requestParameters, "loginForm:otpCode")

                if otpCfg.get("otp_type") == "hotp":
                    success=self.processHotpAuthentication(user, otpCode)
                elif otpCfg.get("otp_type") == "totp":
                    success=self.processTotpAuthentication(user, otpCode)

                if not success:
                    facesMessages.add(FacesMessage.SEVERITY_ERROR, "Wrong code entered")

            elif acr==self.ACR_SG:
                user_name=user.getUserId()
                user_inum=user.getAttribute("inum")

                session_device_status = self.getSessionDeviceStatus(session_attributes, user_name)

                if session_device_status != None:
                    u2f_device_id = session_device_status['device_id']
                    validation_result = self.validateSessionDeviceStatus(self.supergluu_app_id, session_device_status, user_inum)

                    if validation_result:
                        super_gluu_request = json.loads(session_device_status['super_gluu_request'])
                        validation_result= super_gluu_request['method']=="authenticate"

                        if validation_result:
                            print "Cred-manager. authenticate. User '%s' successfully authenticated with u2f_device '%s'" % (user_name, u2f_device_id)
                            success = True

            #Update the list of trusted devices if 2fa passed
            if success:
                print "Cred-manager. authenticate. 2FA authentication was successful"
                tdi = session_attributes.get("trustedDevicesInfo")
                if tdi==None:
                    print "Cred-manager. authenticate. Couldn't update list of user's trusted devices"
                else:
                    user.setAttribute("oxTrustedDevicesInfo", tdi)
                    userService.updateUser(user)
            else:
                print "Cred-manager. authenticate. 2FA authentication failed"

            return success

        return False

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        print "Cred-manager. prepareForStep %s" % str(step)
        if step==1:
            return True
        elif step==2:
            identity=CdiUtil.bean(Identity)
            session_attributes = identity.getSessionId().getSessionAttributes()

            authenticationService = CdiUtil.bean(AuthenticationService)
            user = authenticationService.getAuthenticatedUser()

            if user==None:
                print "Cred-manager. prepareForStep. Cannot retrieve logged user"
                return False

            acr=session_attributes.get("ACR")
            print "Cred-manager. prepareForStep. ACR=%s" % acr
            identity.setWorkingParameter("methods", self.getAvailMethodsUser(user, acr))

            if acr==self.ACR_U2F:
                authnRequest=self.getU2fAuthnRequest(user.getUserId())
                identity.setWorkingParameter("fido_u2f_authentication_request", authnRequest)
                return True

            elif acr==self.ACR_SMS:
                mobiles = user.getAttributeValues("mobile")
                code = random.randint(100000, 999999)
                identity.setWorkingParameter("randCode", code)

                twilioCfg=self.scriptsConfig.get(self.ACR_SMS)
                for numb in mobiles:
                    try:
                        Twilio.init(twilioCfg.get("twilio_sid"), twilioCfg.get("twilio_token"))
                        print "Cred-manager. prepareForStep. Sending SMS message (%s) to %s" % (code, numb)
                        message = TwMessage.creator(PhoneNumber(numb), PhoneNumber(twilioCfg.get("from_number")), "%s is your passcode to access your account" % code).create()
                        print "Cred-manager. prepareForStep. Message Sid: %s" % message.getSid()
                    except:
                        print "Cred-manager. prepareForStep. Error sending message", sys.exc_info()[1]
                return True

            elif acr==self.ACR_SG:
                session_id = CdiUtil.bean(SessionIdService).getSessionIdFromCookie()
                issuer = CdiUtil.bean(ConfigurationFactory).getAppConfiguration().getIssuer()
                client_redirect_uri = self.supergluu_app_id

                super_gluu_request_dictionary = {'username': user.getUserId(),
                               'app': client_redirect_uri,
                               'issuer': issuer,
                               'method': "authenticate",
                               'state': session_id,
                               'created': datetime.datetime.now().isoformat()}

                self.addGeolocationData(session_attributes, super_gluu_request_dictionary)
                super_gluu_request = json.dumps(super_gluu_request_dictionary, separators=(',',':'))
                print "Cred-manager. prepareForStep. Super gluu QR-code/push request prepared: %s" % super_gluu_request
                self.sendPushNotification(client_redirect_uri, user, super_gluu_request)

                sgCfg=self.scriptsConfig.get(self.ACR_SG)
                identity.setWorkingParameter("super_gluu_label", sgCfg.get("label"))
                identity.setWorkingParameter("super_gluu_qr_options", sgCfg.get("qr_options"))
                identity.setWorkingParameter("super_gluu_request", super_gluu_request)

                return True

        return True

    def getExtraParametersForStep(self, configurationAttributes, step):
        print "Cred-manager. getExtraParametersForStep %s" % str(step)
        if step==2:
            return Arrays.asList("ACR", "trustedDevicesInfo", "randCode", "methods", "super_gluu_request")
        return None

    def getCountAuthenticationSteps(self, configurationAttributes):
        print "Cred-manager. getCountAuthenticationSteps called"

        if CdiUtil.bean(Identity).getWorkingParameter("skip2FA"):
           return 1
        return 2

    def getPageForStep(self, configurationAttributes, step):
        print "Cred-manager. getPageForStep called %s" % str(step)

        if step == 2:
            acr=CdiUtil.bean(Identity).getWorkingParameter("ACR")
            print "Cred-manager. getPageForStep ACR=%s" % acr
            if acr == self.ACR_SMS:
                page = "/cm/twiliosms.xhtml"
            elif acr == self.ACR_U2F:
                page = "/cm/login.xhtml"
            elif acr == self.ACR_OTP:
                page = "/cm/otplogin.xhtml"
            elif acr == self.ACR_SG:
                page = "/cm/sg_login.xhtml"
            else:
                page=None

            return page
        return ""

    def getNextStep(self, configurationAttributes, requestParameters, step):

        print "Cred-manager. getNextStep called %s" % str(step)
        if step==2:
            alter=ServerUtil.getFirstValue(requestParameters, "alternativeMethod")
            if alter != None:
                print "Cred-manager. getNextStep. Use alternative method %s" % alter
                CdiUtil.bean(Identity).setWorkingParameter("ACR", alter)
                #retry step with different acr
                return step

        return -1

    def logout(self, configurationAttributes, requestParameters):
        print "Cred-manager. logout called"
        return True

# AUXILIARY ROUTINES

    def getAvailMethodsUser(self, user, skip):
        methods=ArrayList()

        if (self.scriptsConfig.get(self.ACR_SMS)!=None) and (user.getAttribute("mobile")!=None):
            methods.add(self.ACR_SMS)

        if (self.scriptsConfig.get(self.ACR_OTP)!=None) and (user.getAttribute("oxExternalUid")!=None):
            methods.add(self.ACR_OTP)

        inum = user.getAttribute("inum")

        u2fConfig=self.scriptsConfig.get(self.ACR_U2F)
        if (u2fConfig!=None) and (self.hasFidoEnrollments(inum, self.u2f_app_id)):
            methods.add(self.ACR_U2F)

        sgConfig=self.scriptsConfig.get(self.ACR_SG)
        if (sgConfig!=None) and (self.hasFidoEnrollments(inum, self.supergluu_app_id)):
            methods.add(self.ACR_SG)

        if methods.size()>0:
            methods.remove(skip)

        print "Cred-manager. getAvailMethodsUser %s" % methods.toString()
        return methods

    #FIDO

    def hasFidoEnrollments(self, inum, app_id):

        devRegService = CdiUtil.bean(DeviceRegistrationService)
        userDevices=devRegService.findUserDeviceRegistrations(inum, app_id, "oxStatus")

        hasDevices=False
        for device in userDevices:
            if device.getStatus().getValue()=="active":
                hasDevices=True
                break

        return hasDevices

    #U2F

    def getAuthnRequestService(self):

        configs=self.scriptsConfig.get(self.ACR_U2F)
        u2f_server_uri = configs.get("u2f_server_uri")
        #u2f_server_metadata_uri = u2f_server_uri + "/.well-known/fido-u2f-configuration"
        u2f_server_metadata_uri = u2f_server_uri + "/oxauth/restv1/fido-u2f-configuration"

        u2fClient=FidoU2fClientFactory.instance()
        metaDataConfigurationService = u2fClient.createMetaDataConfigurationService(u2f_server_metadata_uri)
        u2fMetaDataConfig = metaDataConfigurationService.getMetadataConfiguration()

        return u2fClient.createAuthenticationRequestService(u2fMetaDataConfig)

    def getU2fAuthnRequest(self, userId):
        authnRequestService=self.getAuthnRequestService()
        configs=self.scriptsConfig.get(self.ACR_U2F)

        session_id = CdiUtil.bean(SessionIdService).getSessionIdFromCookie()
        authnRequest = authnRequestService.startAuthentication(userId, None, configs.get("u2f_application_id"), session_id)
        return ServerUtil.asJson(authnRequest)

    def finishU2fAuthentication(self, userId, token_response):
        authnRequestService=self.getAuthnRequestService()
        authenticationStatus = authnRequestService.finishAuthentication(userId, token_response)

        if authenticationStatus.getStatus()!= Constants.RESULT_SUCCESS:
            print "finishU2fAuthentication. Invalid authentication status from FIDO U2F server"
            return False

        return True

    #OTP

    def loadOtpConfigurations(self, otp_conf_file, cfgMap):
        print "Cred-manager. init. loadOtpConfigurations"

        if otp_conf_file==None:
            return False;

        # Load configuration from file
        f = open(otp_conf_file, 'r')
        try:
            otpConfiguration = json.loads(f.read())
        except:
            print "Cred-manager. init. loadOtpConfigurations. Failed to load configuration from file: %s" % otp_conf_file
            return False
        finally:
            f.close()

        # Check configuration file settings
        try:
            hotpConfiguration=otpConfiguration["htop"]
            totpConfiguration=otpConfiguration["totp"]

            hmacShaAlgorithm = totpConfiguration["hmacShaAlgorithm"]
            hmacShaAlgorithmType = None

            if hmacShaAlgorithm=="sha1":
                hmacShaAlgorithmType = HmacShaAlgorithm.HMAC_SHA_1
            elif hmacShaAlgorithm=="sha256":
                hmacShaAlgorithmType = HmacShaAlgorithm.HMAC_SHA_256
            elif hmacShaAlgorithm=="sha512":
                hmacShaAlgorithmType = HmacShaAlgorithm.HMAC_SHA_512
            else:
                print "Cred-manager. init. loadOtpConfigurations. Invalid TOTP HMAC SHA algorithm: %s" % hmacShaAlgorithm

            totpConfiguration["hmacShaAlgorithmType"] = hmacShaAlgorithmType

            cfgMap.put("hotpConfiguration", hotpConfiguration)
            cfgMap.put("totpConfiguration", totpConfiguration)
        except:
            print "Cred-manager. init. loadOtpConfigurations. Invalid configuration file"
            return False

        return True

    def processHotpAuthentication(self, user, otpCode):

        user_enrollments=self.findOtpEnrollments(user, "hotp")
        userService = CdiUtil.bean(UserService)

        for user_enrollment in user_enrollments:
            user_enrollment_data = user_enrollment.split(";")
            otp_secret_key_encoded = user_enrollment_data[0]

            # Get current moving factor from user entry
            moving_factor = StringHelper.toInteger(user_enrollment_data[1])
            otp_secret_key = self.fromBase64Url(otp_secret_key_encoded)

            # Validate HOTP
            validation_result = self.validateHotpKey(otp_secret_key, moving_factor, otpCode)
            if (validation_result != None) and validation_result["result"]:
                print "processHotpAuthentication. otpCode is valid"
                otp_user_external_uid = "hotp:%s;%s" % ( otp_secret_key_encoded, moving_factor )
                new_otp_user_external_uid = "hotp:%s;%s" % ( otp_secret_key_encoded, validation_result["movingFactor"] )

                # Update moving factor in user entry
                find_user_by_external_uid = userService.replaceUserAttribute(user.getUserId(), "oxExternalUid", otp_user_external_uid, new_otp_user_external_uid)
                if find_user_by_external_uid != None:
                    return True

                print "processHotpAuthentication. Failed to update user entry"

        return False

    def processTotpAuthentication(self, user, otpCode):

        user_enrollments=self.findOtpEnrollments(user, "totp")

        for user_enrollment in user_enrollments:
            otp_secret_key = self.fromBase64Url(user_enrollment)

            # Validate TOTP
            validation_result = self.validateTotpKey(otp_secret_key, otpCode)
            if (validation_result != None) and validation_result["result"]:
                print "OTP. Process TOTP authentication during authentication. otpCode is valid"
                return True

        return False

    def findOtpEnrollments(self, user, otpType, skipPrefix = True):

        result = []
        userService = CdiUtil.bean(UserService)
        user_custom_ext_attribute = userService.getCustomAttribute(user, "oxExternalUid")
        if user_custom_ext_attribute == None:
            return result

        otp_prefix = "%s:" % otpType
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

    def validateHotpKey(self, secretKey, movingFactor, otpKey):
        hotpConfig=self.scriptsConfig.get("hotpConfiguration")
        digits = hotpConfig["digits"]
        law=hotpConfig["lookAheadWindow"]

        htopValidationResult = HOTPValidator.lookAheadWindow(law).validate(secretKey, movingFactor, digits, otpKey)
        if htopValidationResult.isValid():
            return { "result": True, "movingFactor": htopValidationResult.getNewMovingFactor() }
        return { "result": False, "movingFactor": None }

    def validateTotpKey(self, secretKey, totpKey):
        localTotpKey = self.generateTotpKey(secretKey)
        if StringHelper.equals(localTotpKey, totpKey):
            return { "result": True }
        return { "result": False }

    def generateTotpKey(self, secretKey):

        totpConfig=self.scriptsConfig.get("totpConfiguration")
        digits = totpConfig["digits"]
        timeStep = totpConfig["timeStep"]
        hmacShaAlgorithmType = totpConfig["hmacShaAlgorithmType"]

        totp = TOTP.key(secretKey).digits(digits).timeStep(TimeUnit.SECONDS.toMillis(timeStep)).hmacSha(hmacShaAlgorithmType).build()
        return totp.value()

    def fromBase64Url(self, chars):
        return BaseEncoding.base64Url().decode(chars)

# SG (routines here on are taken from SuperGluuExternalAuthenticator.py script. Just slight changes applied)

    def initPushNotificationService(self, configs, cfgMap):
        print "Super-Gluu. Initialize Native/SNS/Gluu notification services"
        self.pushSnsMode = False
        self.pushGluuMode = False

        if configs.containsKey("notification_service_mode"):
            notificationServiceMode = configs.get("notification_service_mode")
            if notificationServiceMode == "sns":
                return self.initSnsPushNotificationService(configs)
            elif notificationServiceMode == "gluu":
                return self.initGluuPushNotificationService(configs)

        return self.initNativePushNotificationService(configs)

    def loadPushNotificationCreds(self, configs):
        print "Super-Gluu. Initialize notification services"

        super_gluu_creds_file = configs.get("credentials_file")
        if super_gluu_creds_file==None:
            return False

        # Load credentials from file
        f = open(super_gluu_creds_file, 'r')
        try:
            creds = json.loads(f.read())
        except:
            print "Super-Gluu. Initialize notification services. Failed to load credentials from file: %s" % super_gluu_creds_file
            return False
        finally:
            f.close()

        return creds

    def initNativePushNotificationService(self, configs):
        print "Super-Gluu. Initialize native notification services"

        creds = self.loadPushNotificationCreds(configs)

        if creds == None:
            return False

        try:
            android_creds = creds["android"]["gcm"]
            ios_creds = creds["ios"]["apns"]
        except:
            print "Super-Gluu. Initialize native notification services. Invalid credentials file format"
            return False

        self.pushAndroidService = None
        self.pushAppleService = None
        if android_creds["enabled"]:
            self.pushAndroidService = Sender(android_creds["api_key"])
            print "Super-Gluu. Initialize native notification services. Created Android notification service"

        if ios_creds["enabled"]:
            p12_file_path = ios_creds["p12_file_path"]
            p12_passowrd = ios_creds["p12_password"]

            try:
                encryptionService = CdiUtil.bean(EncryptionService)
                p12_passowrd = encryptionService.decrypt(p12_passowrd)
            except:
                # Ignore exception. Password is not encrypted
                print "Super-Gluu. Initialize native notification services. Assuming that 'p12_passowrd' password in not encrypted"

            apnsServiceBuilder =  APNS.newService().withCert(p12_file_path, p12_passowrd)
            if ios_creds["production"]:
                self.pushAppleService = apnsServiceBuilder.withProductionDestination().build()
            else:
                self.pushAppleService = apnsServiceBuilder.withSandboxDestination().build()

            self.pushAppleServiceProduction = ios_creds["production"]

            print "Super-Gluu. Initialize native notification services. Created iOS notification service"

        enabled = self.pushAndroidService != None or self.pushAppleService != None

        return enabled

    def initSnsPushNotificationService(self, configs):
        print "Super-Gluu. Initialize SNS notification services"
        self.pushSnsMode = True

        creds = self.loadPushNotificationCreds(configs)
        if creds == None:
            return False

        try:
            sns_creds = creds["sns"]
            android_creds = creds["android"]["sns"]
            ios_creds = creds["ios"]["sns"]
        except:
            print "Super-Gluu. Initialize SNS notification services. Invalid credentials file format"
            return False

        self.pushAndroidService = None
        self.pushAppleService = None
        if not (android_creds["enabled"] or ios_creds["enabled"]):
            print "Super-Gluu. Initialize SNS notification services. SNS disabled for all platforms"
            return False

        sns_access_key = sns_creds["access_key"]
        sns_secret_access_key = sns_creds["secret_access_key"]
        sns_region = sns_creds["region"]

        encryptionService = CdiUtil.bean(EncryptionService)

        try:
            sns_secret_access_key = encryptionService.decrypt(sns_secret_access_key)
        except:
            # Ignore exception. Password is not encrypted
            print "Super-Gluu. Initialize SNS notification services. Assuming that 'sns_secret_access_key' in not encrypted"

        pushSnsService = CdiUtil.bean(PushSnsService)
        pushClient = pushSnsService.createSnsClient(sns_access_key, sns_secret_access_key, sns_region)

        if android_creds["enabled"]:
            self.pushAndroidService = pushClient
            self.pushAndroidPlatformArn = android_creds["platform_arn"]
            print "Super-Gluu. Initialize SNS notification services. Created Android notification service"

        if ios_creds["enabled"]:
            self.pushAppleService = pushClient
            self.pushApplePlatformArn = ios_creds["platform_arn"]
            self.pushAppleServiceProduction = ios_creds["production"]
            print "Super-Gluu. Initialize SNS notification services. Created iOS notification service"

        enabled = self.pushAndroidService != None or self.pushAppleService != None

        return enabled

    def initGluuPushNotificationService(self, configs):
        print "Super-Gluu. Initialize Gluu notification services"

        self.pushGluuMode = True

        creds = self.loadPushNotificationCreds(configs)
        if creds == None:
            return False

        try:
            gluu_conf = creds["gluu"]
            android_creds = creds["android"]["gluu"]
            ios_creds = creds["ios"]["gluu"]
        except:
            print "Super-Gluu. Initialize Gluu notification services. Invalid credentials file format"
            return False

        self.pushAndroidService = None
        self.pushAppleService = None
        if not (android_creds["enabled"] or ios_creds["enabled"]):
            print "Super-Gluu. Initialize Gluu notification services. Gluu disabled for all platforms"
            return False

        gluu_server_uri = gluu_conf["server_uri"]
        notifyClientFactory = NotifyClientFactory.instance()
        metadataConfiguration = None
        try:
            metadataConfiguration = notifyClientFactory.createMetaDataConfigurationService(gluu_server_uri).getMetadataConfiguration()
        except:
            print "Super-Gluu. Initialize Gluu notification services. Failed to load metadata. Exception: ", sys.exc_info()[1]
            return False

        gluuClient = notifyClientFactory.createNotifyService(metadataConfiguration)
        encryptionService = CdiUtil.bean(EncryptionService)

        if android_creds["enabled"]:
            gluu_access_key = android_creds["access_key"]
            gluu_secret_access_key = android_creds["secret_access_key"]

            try:
                gluu_secret_access_key = encryptionService.decrypt(gluu_secret_access_key)
            except:
                # Ignore exception. Password is not encrypted
                print "Super-Gluu. Initialize Gluu notification services. Assuming that 'gluu_secret_access_key' in not encrypted"

            self.pushAndroidService = gluuClient
            self.pushAndroidServiceAuth = notifyClientFactory.getAuthorization(gluu_access_key, gluu_secret_access_key);
            print "Super-Gluu. Initialize Gluu notification services. Created Android notification service"

        if ios_creds["enabled"]:
            gluu_access_key = ios_creds["access_key"]
            gluu_secret_access_key = ios_creds["secret_access_key"]

            try:
                gluu_secret_access_key = encryptionService.decrypt(gluu_secret_access_key)
            except:
                # Ignore exception. Password is not encrypted
                print "Super-Gluu. Initialize Gluu notification services. Assuming that 'gluu_secret_access_key' in not encrypted"

            self.pushAppleService = gluuClient
            self.pushAppleServiceAuth = notifyClientFactory.getAuthorization(gluu_access_key, gluu_secret_access_key);
            print "Super-Gluu. Initialize Gluu notification services. Created iOS notification service"

        enabled = self.pushAndroidService != None or self.pushAppleService != None

        return enabled

    def addGeolocationData(self, session_attributes, super_gluu_request_dictionary):
        if session_attributes.containsKey("remote_ip"):
            remote_ip = session_attributes.get("remote_ip")
            if StringHelper.isNotEmpty(remote_ip):
                print "Super-Gluu. Adding req_ip and req_loc to super_gluu_request"
                super_gluu_request_dictionary['req_ip'] = remote_ip

                remote_loc_dic = self.determineGeolocationData(remote_ip)
                if remote_loc_dic == None:
                    print "Super-Gluu. Failed to determine remote location by remote IP '%s'" % remote_ip
                    return

                remote_loc = "%s, %s, %s" % ( remote_loc_dic['country'], remote_loc_dic['regionName'], remote_loc_dic['city'] )
                remote_loc_encoded = urllib.quote(remote_loc)
                super_gluu_request_dictionary['req_loc'] = remote_loc_encoded

    def determineGeolocationData(self, remote_ip):
        print "Super-Gluu. Determine remote location. remote_ip: '%s'" % remote_ip
        httpService = CdiUtil.bean(HttpService)

        http_client = httpService.getHttpsClient()
        http_client_params = http_client.getParams()
        http_client_params.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 15 * 1000)

        geolocation_service_url = "http://ip-api.com/json/%s?fields=49177" % remote_ip
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
            response_string = httpService.convertEntityToString(response_bytes, Charset.forName("UTF-8"))
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

    def sendPushNotification(self, client_redirect_uri, user, super_gluu_request):
        try:
            self.sendPushNotificationImpl(client_redirect_uri, user, super_gluu_request)
        except:
            print "Super-Gluu. Send push notification. Failed to send push notification: ", sys.exc_info()[1]

    def sendPushNotificationImpl(self, client_redirect_uri, user, super_gluu_request):

        user_inum = user.getAttribute("inum")
        send_notification = False
        send_notification_result = True

        deviceRegistrationService = CdiUtil.bean(DeviceRegistrationService)
        u2f_devices_list = deviceRegistrationService.findUserDeviceRegistrations(user_inum, client_redirect_uri, "oxId", "oxDeviceData", "oxDeviceNotificationConf", "oxStatus")

        send_android = 0
        send_ios = 0

        #list won't be empty
        for u2f_device in u2f_devices_list:
            if u2f_device.getStatus().getValue()=="active":

                device_data = u2f_device.getDeviceData()
                if device_data == None:
                    continue

                platform = device_data.getPlatform()
                push_token = device_data.getPushToken()
                debug = False

                if StringHelper.equalsIgnoreCase(platform, "ios") and StringHelper.isNotEmpty(push_token):
                    # Sending notification to iOS user's device
                    if self.pushAppleService == None:
                        print "Super-Gluu. Send push notification. Apple native push notification service is not enabled"
                    else:
                        send_notification = True

                        title = "Super-Gluu"
                        message = "Super-Gluu login request to: %s" % client_redirect_uri

                        if self.pushSnsMode or self.pushGluuMode:
                            pushSnsService = CdiUtil.bean(PushSnsService)
                            targetEndpointArn = self.getTargetEndpointArn(deviceRegistrationService, pushSnsService, PushPlatform.APNS, user, u2f_device)
                            if targetEndpointArn == None:
                                return

                            send_notification = True

                            sns_push_request_dictionary = { "aps":
                                                                { "badge": 0,
                                                                  "alert" : {"body": message, "title" : title},
                                                                  "category": "ACTIONABLE",
                                                                  "content-available": "1",
                                                                  "sound": 'default'
                                                           },
                                                           "request" : super_gluu_request
                            }
                            push_message = json.dumps(sns_push_request_dictionary, separators=(',',':'))

                            if self.pushSnsMode:
                                apple_push_platform = PushPlatform.APNS
                                if not self.pushAppleServiceProduction:
                                    apple_push_platform = PushPlatform.APNS_SANDBOX

                                send_notification_result = pushSnsService.sendPushMessage(self.pushAppleService, apple_push_platform, targetEndpointArn, push_message, None)
                                if debug:
                                    print "Super-Gluu. Send iOS SNS push notification. token: '%s', message: '%s', send_notification_result: '%s', apple_push_platform: '%s'" % (push_token, push_message, send_notification_result, apple_push_platform)
                            elif self.pushGluuMode:
                                send_notification_result = self.pushAppleService.sendNotification(self.pushAppleServiceAuth, targetEndpointArn, push_message)
                                if debug:
                                    print "Super-Gluu. Send iOS Gluu push notification. token: '%s', message: '%s', send_notification_result: '%s'" % (push_token, push_message, send_notification_result)
                        else:
                            additional_fields = { "request" : super_gluu_request }

                            msgBuilder = APNS.newPayload().alertBody(message).alertTitle(title).sound("default")
                            msgBuilder.category('ACTIONABLE').badge(0)
                            msgBuilder.forNewsstand()
                            msgBuilder.customFields(additional_fields)
                            push_message = msgBuilder.build()

                            send_notification_result = self.pushAppleService.push(push_token, push_message)
                            if debug:
                                print "Super-Gluu. Send iOS Native push notification. token: '%s', message: '%s', send_notification_result: '%s'" % (push_token, push_message, send_notification_result)
                        send_ios = send_ios + 1

                if StringHelper.equalsIgnoreCase(platform, "android") and StringHelper.isNotEmpty(push_token):
                    # Sending notification to Android user's device
                    if self.pushAndroidService == None:
                        print "Super-Gluu. Send native push notification. Android native push notification service is not enabled"
                    else:
                        send_notification = True

                        title = "Super-Gluu"
                        if self.pushSnsMode or self.pushGluuMode:
                            pushSnsService = CdiUtil.bean(PushSnsService)
                            targetEndpointArn = self.getTargetEndpointArn(deviceRegistrationService, pushSnsService, PushPlatform.GCM, user, u2f_device)
                            if targetEndpointArn == None:
                                return

                            send_notification = True

                            sns_push_request_dictionary = { "collapse_key": "single",
                                                            "content_available": True,
                                                            "time_to_live": 60,
                                                            "data":
                                                                { "message" : super_gluu_request,
                                                                  "title" : title }
                            }
                            push_message = json.dumps(sns_push_request_dictionary, separators=(',',':'))

                            if self.pushSnsMode:
                                send_notification_result = pushSnsService.sendPushMessage(self.pushAndroidService, PushPlatform.GCM, targetEndpointArn, push_message, None)
                                if debug:
                                    print "Super-Gluu. Send Android SNS push notification. token: '%s', message: '%s', send_notification_result: '%s'" % (push_token, push_message, send_notification_result)
                            elif self.pushGluuMode:
                                send_notification_result = self.pushAndroidService.sendNotification(self.pushAndroidServiceAuth, targetEndpointArn, push_message)
                                if debug:
                                    print "Super-Gluu. Send Android Gluu push notification. token: '%s', message: '%s', send_notification_result: '%s'" % (push_token, push_message, send_notification_result)
                        else:
                            msgBuilder = Message.Builder().addData("message", super_gluu_request).addData("title", title).collapseKey("single").contentAvailable(True)
                            push_message = msgBuilder.build()

                            send_notification_result = self.pushAndroidService.send(push_message, push_token, 3)
                            if debug:
                                print "Super-Gluu. Send Android Native push notification. token: '%s', message: '%s', send_notification_result: '%s'" % (push_token, push_message, send_notification_result)
                        send_android = send_android + 1

        print "Super-Gluu. Send push notification. send_android: '%s', send_ios: '%s'" % (send_android, send_ios)

    def getTargetEndpointArn(self, deviceRegistrationService, pushSnsService, platform, user, u2fDevice):
        targetEndpointArn = None

        # Return endpoint ARN if it created already
        notificationConf = u2fDevice.getDeviceNotificationConf()
        if StringHelper.isNotEmpty(notificationConf):
            notificationConfJson = json.loads(notificationConf)
            targetEndpointArn = notificationConfJson['sns_endpoint_arn']
            if StringHelper.isNotEmpty(targetEndpointArn):
                print "Super-Gluu. Get target endpoint ARN. There is already created target endpoint ARN"
                return targetEndpointArn

        # Create endpoint ARN
        pushClient = None
        pushClientAuth = None
        platformApplicationArn = None
        if platform == PushPlatform.GCM:
            pushClient = self.pushAndroidService
            if self.pushSnsMode:
                platformApplicationArn = self.pushAndroidPlatformArn
            if self.pushGluuMode:
                pushClientAuth = self.pushAndroidServiceAuth
        elif platform == PushPlatform.APNS:
            pushClient = self.pushAppleService
            if self.pushSnsMode:
                platformApplicationArn = self.pushApplePlatformArn
            if self.pushGluuMode:
                pushClientAuth = self.pushAppleServiceAuth
        else:
            return None

        deviceData = u2fDevice.getDeviceData()
        pushToken = deviceData.getPushToken()

        print "Super-Gluu. Get target endpoint ARN. Attempting to create target endpoint ARN for user: '%s'" % user.getUserId()
        if self.pushSnsMode:
            targetEndpointArn = pushSnsService.createPlatformArn(pushClient, platformApplicationArn, pushToken, user)
        else:
            customUserData = pushSnsService.getCustomUserData(user)
            registerDeviceResponse = pushClient.registerDevice(pushClientAuth, pushToken, customUserData);
            if registerDeviceResponse != None and registerDeviceResponse.getStatusCode() == 200:
                targetEndpointArn = registerDeviceResponse.getEndpointArn()

        if StringHelper.isEmpty(targetEndpointArn):
            print "Super-Gluu. Failed to get endpoint ARN for user: '%s'" % user.getUserId()
            return None

        print "Super-Gluu. Get target endpoint ARN. Create target endpoint ARN '%s' for user: '%s'" % (targetEndpointArn, user.getUserId())

        # Store created endpoint ARN in device entry
        userInum = user.getAttribute("inum")
        u2fDeviceUpdate = deviceRegistrationService.findUserDeviceRegistration(userInum, u2fDevice.getId())
        u2fDeviceUpdate.setDeviceNotificationConf('{"sns_endpoint_arn" : "%s"}' % targetEndpointArn)
        deviceRegistrationService.updateDeviceRegistration(userInum, u2fDeviceUpdate)

        return targetEndpointArn

    def getSessionDeviceStatus(self, session_attributes, user_name):
        print "Super-Gluu. Get session device status"

        if not session_attributes.containsKey("super_gluu_request"):
            print "Super-Gluu. Get session device status. There is no Super-Gluu request in session attributes"
            return None

        # Check session state extended
        if not session_attributes.containsKey("session_custom_state"):
            print "Super-Gluu. Get session device status. There is no session_custom_state in session attributes"
            return None

        session_custom_state = session_attributes.get("session_custom_state")
        if not StringHelper.equalsIgnoreCase("approved", session_custom_state):
            print "Super-Gluu. Get session device status. User '%s' not approve or not pass U2F authentication. session_custom_state: '%s'" % (user_name, session_custom_state)
            return None

        # Try to find device_id in session attribute
        if not session_attributes.containsKey("oxpush2_u2f_device_id"):
            print "Super-Gluu. Get session device status. There is no u2f_device associated with this request"
            return None

        # Try to find user_inum in session attribute
        if not session_attributes.containsKey("oxpush2_u2f_device_user_inum"):
            print "Super-Gluu. Get session device status. There is no user_inum associated with this request"
            return None

        enroll = False
        if session_attributes.containsKey("oxpush2_u2f_device_enroll"):
            enroll = StringHelper.equalsIgnoreCase("true", session_attributes.get("oxpush2_u2f_device_enroll"))

        one_step = False
        if session_attributes.containsKey("oxpush2_u2f_device_one_step"):
            one_step = StringHelper.equalsIgnoreCase("true", session_attributes.get("oxpush2_u2f_device_one_step"))

        super_gluu_request = session_attributes.get("super_gluu_request")
        u2f_device_id = session_attributes.get("oxpush2_u2f_device_id")
        user_inum = session_attributes.get("oxpush2_u2f_device_user_inum")

        session_device_status = {"super_gluu_request": super_gluu_request, "device_id": u2f_device_id, "user_inum" : user_inum, "enroll" : enroll, "one_step" : one_step}
        print "Super-Gluu. Get session device status. session_device_status: '%s'" % (session_device_status)

        return session_device_status

    def validateSessionDeviceStatus(self, client_redirect_uri, session_device_status, user_inum = None):

        deviceRegistrationService = CdiUtil.bean(DeviceRegistrationService)
        u2f_device_id = session_device_status['device_id']

        u2f_device = None
        if session_device_status['enroll'] and session_device_status['one_step']:
            u2f_device = deviceRegistrationService.findOneStepUserDeviceRegistration(u2f_device_id)
            if u2f_device == None:
                print "Super-Gluu. Validate session device status. There is no one step u2f_device '%s'" % u2f_device_id
                return False
        else:
            if session_device_status['one_step']:
                user_inum = session_device_status['user_inum']

            u2f_device = deviceRegistrationService.findUserDeviceRegistration(user_inum, u2f_device_id)
            if u2f_device == None:
                print "Super-Gluu. Validate session device status. There is no u2f_device '%s' associated with user '%s'" % (u2f_device_id, user_inum)
                return False

        if not StringHelper.equalsIgnoreCase(client_redirect_uri, u2f_device.application):
            print "Super-Gluu. Validate session device status. u2f_device '%s' associated with other application '%s'" % (u2f_device_id, u2f_device.application)
            return False

        return True

    # 2FA policy enforcement

    def determineSkip2FA(self, userService, identity, foundUser, platform):

        #Load configuration from file
        path = System.getProperty("gluu.base")
        path = Paths.get(path, "conf", "cred-manager.json").toString()
        f = open(path, 'r')
        try:
            cmConfigs = json.loads(f.read())
            if 'policy_2fa' in cmConfigs:
                policy2FA = ','.join(cmConfigs['policy_2fa'])
            else:
                policy2FA = 'EVERY_LOGIN'
        except:
            print "Cred-manager. init. Failed to read policy_2fa from configuration file"
            return False
        finally:
            f.close()

        print "Cred-manager. determineSkip2FA with general policy %s" % policy2FA
        policy2FA = policy2FA + ','
        skip2FA = False

        if 'CUSTOM,' in policy2FA:
            #read setting from user profile
            policy = foundUser.getAttribute("oxStrongAuthPolicy")
            if policy == None:
                policy = 'EVERY_LOGIN,'
            else:
                policy = policy.upper() + ','
            print "Cred-manager. determineSkip2FA. Using user's enforcement policy %s" % policy

        else:
            #If it's not custom, then apply the global setting admin defined
            policy = policy2FA

        if not 'EVERY_LOGIN,' in policy:
            locationCriterion = 'LOCATION_UNKNOWN,' in policy
            deviceCriterion = 'DEVICE_UNKNOWN,' in policy

            if locationCriterion or deviceCriterion:
                try:
                    #Find device info passed in HTTP request params (see index.xhtml)
                    deviceInf = json.loads(platform)
                    skip2FA = self.process2FAPolicy(identity, foundUser, deviceInf, locationCriterion, deviceCriterion)

                    if skip2FA:
                        print "Cred-manager. determineSkip2FA. Second factor is skipped"
                        #Update attribute if authentication will not have second step
                        devInf = identity.getWorkingParameter("trustedDevicesInfo")
                        if devInf!=None:
                            foundUser.setAttribute("oxTrustedDevicesInfo", devInf)
                            userService.updateUser(foundUser)
                except:
                    print "Cred-manager. determineSkip2FA. Error parsing current user device data. Forcing 2FA to take place..."

            else:
                print "Cred-manager. determineSkip2FA. Unknown %s policy: cannot skip 2FA" % policy

        return skip2FA

    def process2FAPolicy(self, identity, foundUser, deviceInf, locationCriterion, deviceCriterion):

        skip2FA = False
        #Retrieve user's devices info
        devicesInfo = foundUser.getAttribute("oxTrustedDevicesInfo")

        #do geolocation
        geodata = self.getGeolocation(identity)
        if geodata == None:
            print "Cred-manager. process2FAPolicy: Geolocation data not obtained. 2FA skipping based on location cannot take place"

        try:
            encService = CdiUtil.bean(EncryptionService)

            if devicesInfo == None:
                print "Cred-manager. process2FAPolicy: There are no trusted devices for user yet"
                #Simulate empty list
                devicesInfo = "[]"
            else:
                devicesInfo = encService.decrypt(devicesInfo)

            devicesInfo = json.loads(devicesInfo)

            partialMatch = False
            idx = 0
            #Try to find a match for device only
            for device in devicesInfo:
                partialMatch = device['browser']['name']==deviceInf['name'] and device['os']['version']==deviceInf['os']['version'] and device['os']['family']==deviceInf['os']['family']
                if partialMatch:
                    break
                idx+=1

            matchFound = False

            #At least one of locationCriterion or deviceCriterion is True
            if locationCriterion and not deviceCriterion:
                #this check makes sense if there is city data only
                if geodata!=None:
                    for device in devicesInfo:
                        #Search all registered cities that are found in trusted devices
                        for origin in device['origins']:
                            matchFound = matchFound or origin['city']==geodata['city']

            elif partialMatch:
                #In this branch deviceCriterion is True
                if not locationCriterion:
                    matchFound = True
                elif geodata!=None:
                    for origin in devicesInfo[idx]['origins']:
                        matchFound = matchFound or origin['city']==geodata['city']

            skip2FA = matchFound
            now = Date().getTime()

            #Update attribute oxTrustedDevicesInfo accordingly
            if partialMatch:
                #Update an existing record (update timestamp in city, or else add it)
                if geodata != None:
                    partialMatch = False
                    idxCity = 0

                    for origin in devicesInfo[idx]['origins']:
                        partialMatch = origin['city']==geodata['city']
                        if partialMatch:
                            break;
                        idxCity+=1

                    if partialMatch:
                        devicesInfo[idx]['origins'][idxCity]['timestamp'] = now
                    else:
                        devicesInfo[idx]['origins'].append({"city": geodata['city'], "country": geodata['country'], "timestamp": now})
            else:
                #Create a new entry
                browser= {"name": deviceInf['name'], "version": deviceInf['version']}
                os = {"family": deviceInf['os']['family'], "version": deviceInf['os']['version']}

                if geodata == None:
                    origins = []
                else:
                    origins = [{"city": geodata['city'], "country": geodata['country'], "timestamp": now}]

                obj = {"browser": browser, "os": os, "origins": origins}
                devicesInfo.append(obj)

            enc = json.dumps(devicesInfo, separators=(',',':'))
            enc = encService.encrypt(enc)
            identity.setWorkingParameter("trustedDevicesInfo", enc)
        except:
            print "Cred-manager. process2FAPolicy. Error!", sys.exc_info()[1]

        return skip2FA

    def getGeolocation(self, identity):

        session_attributes = identity.getSessionId().getSessionAttributes()
        if session_attributes.containsKey("remote_ip"):
            remote_ip = session_attributes.get("remote_ip")
            if StringHelper.isNotEmpty(remote_ip):

                httpService = CdiUtil.bean(HttpService)

                http_client = httpService.getHttpsClient()
                http_client_params = http_client.getParams()
                http_client_params.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 4 * 1000)

                geolocation_service_url = "http://ip-api.com/json/%s?fields=country,city,status,message" % remote_ip
                geolocation_service_headers = { "Accept" : "application/json" }

                try:
                    http_service_response = httpService.executeGet(http_client, geolocation_service_url, geolocation_service_headers)
                    http_response = http_service_response.getHttpResponse()
                except:
                    print "Cred-manager. Determine remote location. Exception: ", sys.exc_info()[1]
                    return None

                try:
                    if not httpService.isResponseStastusCodeOk(http_response):
                        print "Cred-manager. Determine remote location. Get non 200 OK response from server:", str(http_response.getStatusLine().getStatusCode())
                        httpService.consume(http_response)
                        return None

                    response_bytes = httpService.getResponseContent(http_response)
                    response_string = httpService.convertEntityToString(response_bytes, Charset.forName("UTF-8"))
                    httpService.consume(http_response)
                finally:
                    http_service_response.closeConnection()

                if response_string == None:
                    print "Cred-manager. Determine remote location. Get empty response from location server"
                    return None

                response = json.loads(response_string)

                if not StringHelper.equalsIgnoreCase(response['status'], "success"):
                    print "Cred-manager. Determine remote location. Get response with status: '%s'" % response['status']
                    return None

                return response

        return None
