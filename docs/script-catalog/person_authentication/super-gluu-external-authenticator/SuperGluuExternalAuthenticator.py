# Janssen Project software is available under the Apache 2.0 License (2004). See http://www.apache.org/licenses/ for full text.
# Copyright (c) 2020, Janssen Project
#
# Author: Yuriy Movchan
#

from com.google.android.gcm.server import Sender, Message
from com.notnoop.apns import APNS
from java.util import Arrays
from org.apache.http.params import CoreConnectionPNames
from io.jans.service.cdi.util import CdiUtil
from io.jans.as.server.security import Identity
from io.jans.model.custom.script.type.auth import PersonAuthenticationType
from io.jans.as.server.model.config import ConfigurationFactory
from io.jans.as.server.service import AuthenticationService
from io.jans.as.server.service import SessionIdService
from io.jans.as.server.service.fido.u2f import DeviceRegistrationService
from io.jans.as.server.service.net import HttpService
from io.jans.as.server.util import ServerUtil
from io.jans.util import StringHelper
from io.jans.as.common.service.common import EncryptionService
from io.jans.as.server.service import UserService
from io.jans.service import MailService
from io.jans.as.server.service.push.sns import PushPlatform
from io.jans.as.server.service.push.sns import PushSnsService
from io.jans.notify.client import NotifyClientFactory
from java.util import Arrays, HashMap, IdentityHashMap, Date
from java.time import ZonedDateTime
from java.time.format import DateTimeFormatter
from io.jans.as.model.configuration import AppConfiguration
import datetime
import urllib
import sys
import json

import datetime
import urllib

import sys
import json

class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Super-Gluu. Initialization"

        if not configurationAttributes.containsKey("authentication_mode"):
            print "Super-Gluu. Initialization. Property authentication_mode is mandatory"
            return False

        self.applicationId = None
        if configurationAttributes.containsKey("application_id"):
            self.applicationId = configurationAttributes.get("application_id").getValue2()

        self.registrationUri = None
        if configurationAttributes.containsKey("registration_uri"):
            self.registrationUri = configurationAttributes.get("registration_uri").getValue2()

        authentication_mode = configurationAttributes.get("authentication_mode").getValue2()
        if StringHelper.isEmpty(authentication_mode):
            print "Super-Gluu. Initialization. Failed to determine authentication_mode. authentication_mode configuration parameter is empty"
            return False

        self.oneStep = StringHelper.equalsIgnoreCase(authentication_mode, "one_step")
        self.twoStep = StringHelper.equalsIgnoreCase(authentication_mode, "two_step")

        if not (self.oneStep or self.twoStep):
            print "Super-Gluu. Initialization. Valid authentication_mode values are one_step and two_step"
            return False

        self.enabledPushNotifications = self.initPushNotificationService(configurationAttributes)

        self.androidUrl = None
        if configurationAttributes.containsKey("supergluu_android_download_url"):
            self.androidUrl = configurationAttributes.get("supergluu_android_download_url").getValue2()

        self.IOSUrl = None
        if configurationAttributes.containsKey("supergluu_ios_download_url"):
            self.IOSUrl = configurationAttributes.get("supergluu_ios_download_url").getValue2()

        self.customLabel = None
        if configurationAttributes.containsKey("label"):
            self.customLabel = configurationAttributes.get("label").getValue2()

        self.customQrOptions = {}
        if configurationAttributes.containsKey("qr_options"):
            self.customQrOptions = configurationAttributes.get("qr_options").getValue2()

        self.use_super_gluu_group = False
        if configurationAttributes.containsKey("super_gluu_group"):
            self.super_gluu_group = configurationAttributes.get("super_gluu_group").getValue2()
            self.use_super_gluu_group = True
            print "Super-Gluu. Initialization. Using super_gluu only if user belong to group: %s" % self.super_gluu_group

        self.use_audit_group = False
        if configurationAttributes.containsKey("audit_group"):
            self.audit_group = configurationAttributes.get("audit_group").getValue2()

            if (not configurationAttributes.containsKey("audit_group_email")):
                print "Super-Gluu. Initialization. Property audit_group_email is not specified"
                return False

            self.audit_email = configurationAttributes.get("audit_group_email").getValue2()
            self.use_audit_group = True

            print "Super-Gluu. Initialization. Using audit group: %s" % self.audit_group

        if self.use_super_gluu_group or self.use_audit_group:
            if not configurationAttributes.containsKey("audit_attribute"):
                print "Super-Gluu. Initialization. Property audit_attribute is not specified"
                return False
            else:
                self.audit_attribute = configurationAttributes.get("audit_attribute").getValue2()

        print "Super-Gluu. Initialized successfully. oneStep: '%s', twoStep: '%s', pushNotifications: '%s', customLabel: '%s'" % (self.oneStep, self.twoStep, self.enabledPushNotifications, self.customLabel)

        return True

    def destroy(self, configurationAttributes):
        print "Super-Gluu. Destroy"

        self.pushAndroidService = None
        self.pushAppleService = None

        print "Super-Gluu. Destroyed successfully"
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

        session_attributes = identity.getSessionId().getSessionAttributes()

        client_redirect_uri = self.getApplicationUri(session_attributes)
        if client_redirect_uri == None:
            print "Super-Gluu. Authenticate. redirect_uri is not set"
            return False

        self.setRequestScopedParameters(identity, step)

        # Validate form result code and initialize QR code regeneration if needed (retry_current_step = True)
        identity.setWorkingParameter("retry_current_step", False)
        form_auth_result = ServerUtil.getFirstValue(requestParameters, "auth_result")
        if StringHelper.isNotEmpty(form_auth_result):
            print "Super-Gluu. Authenticate for step %s. Get auth_result: '%s'" % (step, form_auth_result)
            if form_auth_result in ['error']:
                return False

            if form_auth_result in ['timeout']:
                if ((step == 1) and self.oneStep) or ((step == 2) and self.twoStep):
                    print "Super-Gluu. Authenticate for step %s. Reinitializing current step" % step
                    identity.setWorkingParameter("retry_current_step", True)
                    return False

        userService = CdiUtil.bean(UserService)
        deviceRegistrationService = CdiUtil.bean(DeviceRegistrationService)
        if step == 1:
            print "Super-Gluu. Authenticate for step 1"

            user_name = credentials.getUsername()
            if self.oneStep:
                session_device_status = self.getSessionDeviceStatus(session_attributes, user_name)
                if session_device_status == None:
                    return False

                u2f_device_id = session_device_status['device_id']

                validation_result = self.validateSessionDeviceStatus(client_redirect_uri, session_device_status)
                if validation_result:
                    print "Super-Gluu. Authenticate for step 1. User successfully authenticated with u2f_device '%s'" % u2f_device_id
                else:
                    return False

                if not session_device_status['one_step']:
                    print "Super-Gluu. Authenticate for step 1. u2f_device '%s' is not one step device" % u2f_device_id
                    return False

                # There are two steps only in enrollment mode
                if session_device_status['enroll']:
                    return validation_result

                identity.setWorkingParameter("super_gluu_count_login_steps", 1)

                user_inum = session_device_status['user_inum']

                u2f_device = deviceRegistrationService.findUserDeviceRegistration(user_inum, u2f_device_id, "jansId")
                if u2f_device == None:
                    print "Super-Gluu. Authenticate for step 1. Failed to load u2f_device '%s'" % u2f_device_id
                    return False
                found = userService.getUserByInum(user_inum)
                user_name = found.getUserId()
                logged_in = authenticationService.authenticate(user_name)
                if not logged_in:
                    print "Super-Gluu. Authenticate for step 1. Failed to authenticate user '%s'" % user_name
                    return False

                print "Super-Gluu. Authenticate for step 1. User '%s' successfully authenticated with u2f_device '%s'" % (user_name, u2f_device_id)

                return True
            elif self.twoStep:
                authenticated_user = self.processBasicAuthentication(credentials)
                if authenticated_user == None:
                    return False

                if (self.use_super_gluu_group):
                    print "Super-Gluu. Authenticate for step 1. Checking if user belong to super_gluu group"
                    is_member_super_gluu_group = self.isUserMemberOfGroup(authenticated_user, self.audit_attribute, self.super_gluu_group)
                    if (is_member_super_gluu_group):
                        print "Super-Gluu. Authenticate for step 1. User '%s' member of super_gluu group" % authenticated_user.getUserId()
                        super_gluu_count_login_steps = 2
                    else:
                        if self.use_audit_group:
                            self.processAuditGroup(authenticated_user, self.audit_attribute, self.audit_group)
                        super_gluu_count_login_steps = 1

                    identity.setWorkingParameter("super_gluu_count_login_steps", super_gluu_count_login_steps)

                    if super_gluu_count_login_steps == 1:
                        return True

                auth_method = 'authenticate'
                enrollment_mode = ServerUtil.getFirstValue(requestParameters, "loginForm:registerButton")
                if StringHelper.isNotEmpty(enrollment_mode):
                    auth_method = 'enroll'

                if auth_method == 'authenticate':
                    user_inum = userService.getUserInum(authenticated_user)
                    u2f_devices_list = deviceRegistrationService.findUserDeviceRegistrations(user_inum, client_redirect_uri, "jansId")
                    if u2f_devices_list.size() == 0:
                        auth_method = 'enroll'
                        print "Super-Gluu. Authenticate for step 1. There is no U2F '%s' user devices associated with application '%s'. Changing auth_method to '%s'" % (user_name, client_redirect_uri, auth_method)

                print "Super-Gluu. Authenticate for step 1. auth_method: '%s'" % auth_method

                identity.setWorkingParameter("super_gluu_auth_method", auth_method)

                return True

            return False
        elif step == 2:
            print "Super-Gluu. Authenticate for step 2"
            session_attributes = identity.getSessionId().getSessionAttributes()

            # There are two steps only in enrollment mode
            if self.oneStep :
                authenticated_user = self.processBasicAuthentication(credentials)
                if authenticated_user == None:
                    return False
                user_inum = userService.getUserInum(authenticated_user)
                session_device_status = self.getSessionDeviceStatus(session_attributes, user_inum)

                if session_device_status['enroll']:

                    if session_device_status == None:
                        print "Super-Gluu. oneStep, authenticate for step2, session_device_status is false"
                        return False

                    u2f_device_id = session_device_status['device_id']

                    attach_result = deviceRegistrationService.attachUserDeviceRegistration(user_inum, u2f_device_id)

                    print "Super-Gluu. Authenticate for step 2. Result after attaching u2f_device '%s' to user '%s': '%s'" % (u2f_device_id, user_inum, attach_result)

                    return attach_result
                else:
                    print "Super-Gluu. one_step but  session_device_status['enroll'] = false"
                    return False
            elif self.twoStep:
                user = authenticationService.getAuthenticatedUser()
                if (user == None):
                    print "Super-Gluu. Authenticate for step 2. Failed to determine user name"
                    return False
                user_name = user.getUserId()
                session_device_status = self.getSessionDeviceStatus(session_attributes, user_name)
                if session_device_status == None:
                    print "Super-Gluu. twoStep, authenticate for step2, session_device_status is false"
                    return False

                u2f_device_id = session_device_status['device_id']

                if user_name == None:
                    print "Super-Gluu. Authenticate for step 2. Failed to determine user id"
                    return False

                validation_result = self.validateSessionDeviceStatus(client_redirect_uri, session_device_status, user_name)
                if validation_result:
                    print "Super-Gluu. Authenticate for step 2. User '%s' successfully authenticated with u2f_device '%s'" % (user_name, u2f_device_id)
                else:
                    return False
                print "super_gluu_request %s " % super_gluu_request
                super_gluu_request = json.loads(session_device_status['super_gluu_request'])
                auth_method = super_gluu_request['method']
                if auth_method in ['enroll', 'authenticate']:
                    if validation_result and self.use_audit_group:
                        user = authenticationService.getAuthenticatedUser()
                        self.processAuditGroup(user, self.audit_attribute, self.audit_group)

                    return validation_result

                print "Super-Gluu. Authenticate for step 2. U2F auth_method is invalid"

            return False
        else:
            return False

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        identity = CdiUtil.bean(Identity)
        session_attributes = identity.getSessionId().getSessionAttributes()

        client_redirect_uri = self.getApplicationUri(session_attributes)
        if client_redirect_uri == None:
            print "Super-Gluu. Prepare for step. redirect_uri is not set"
            return False

        self.setRequestScopedParameters(identity, step)

        if step == 1:
            print "Super-Gluu. Prepare for step 1"
            if self.oneStep:
                session = CdiUtil.bean(SessionIdService).getSessionId()
                if session == None:
                    print "Super-Gluu. Prepare for step 2. Failed to determine session_id"
                    return False
                issuer = CdiUtil.bean(AppConfiguration).getIssuer()


                super_gluu_request_dictionary = {'app': client_redirect_uri,
                                   'issuer': issuer,
                                   'state': session.getId(),
                                   'created': DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now().withNano(0))}

                self.addGeolocationData(session_attributes, super_gluu_request_dictionary)

                super_gluu_request = json.dumps(super_gluu_request_dictionary, separators=(',',':'))
                print "Super-Gluu. Prepare for step 1. Prepared super_gluu_request:", super_gluu_request

                identity.setWorkingParameter("super_gluu_request", super_gluu_request)
                identity.setWorkingParameter("step_sg","one_step")
            elif self.twoStep:
                identity.setWorkingParameter("display_register_action", True)

            return True
        elif step == 2:
            print "Super-Gluu. Prepare for step 2"
            if self.oneStep:
                return True

            authenticationService = CdiUtil.bean(AuthenticationService)
            user = authenticationService.getAuthenticatedUser()
            if user == None:
                print "Super-Gluu. Prepare for step 2. Failed to determine user name"
                return False

            if session_attributes.containsKey("super_gluu_request"):
               super_gluu_request = session_attributes.get("super_gluu_request")
               if not StringHelper.equalsIgnoreCase(super_gluu_request, "timeout"):
                   print "Super-Gluu. Prepare for step 2. Request was generated already"
                   return True

            session = CdiUtil.bean(SessionIdService).getSessionId()
            if session == None:
                print "Super-Gluu. Prepare for step 2. Failed to determine session_id"
                return False

            auth_method = session_attributes.get("super_gluu_auth_method")
            if StringHelper.isEmpty(auth_method):
                print "Super-Gluu. Prepare for step 2. Failed to determine auth_method"
                return False

            print "Super-Gluu. Prepare for step 2. auth_method: '%s'" % auth_method

            issuer = CdiUtil.bean(AppConfiguration).getIssuer()
            super_gluu_request_dictionary = {'username': user.getUserId(),
                               'app': client_redirect_uri,
                               'issuer': issuer,
                               'method': auth_method,
                               'state': session.getId(),
                               'created': DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now().withNano(0))}

            self.addGeolocationData(session_attributes, super_gluu_request_dictionary)

            super_gluu_request = json.dumps(super_gluu_request_dictionary, separators=(',',':'))
            print "Super-Gluu. Prepare for step 2. Prepared super_gluu_request:", super_gluu_request

            identity.setWorkingParameter("super_gluu_request", super_gluu_request)
            identity.setWorkingParameter("super_gluu_auth_method", auth_method)

            if auth_method in ['authenticate']:
                self.sendPushNotification(client_redirect_uri, user, super_gluu_request)

            return True
        else:
            return False

    def getNextStep(self, configurationAttributes, requestParameters, step):
        # If user not pass current step change step to previous
        identity = CdiUtil.bean(Identity)
        retry_current_step = identity.getWorkingParameter("retry_current_step")
        if retry_current_step:
            print "Super-Gluu. Get next step. Retrying current step"

            # Remove old QR code
            identity.setWorkingParameter("super_gluu_request", "timeout")

            resultStep = step
            return resultStep

        return -1

    def getExtraParametersForStep(self, configurationAttributes, step):
        if step == 1:
            if self.oneStep:
                return Arrays.asList("super_gluu_request")
            elif self.twoStep:
                return Arrays.asList("display_register_action")
        elif step == 2:
            return Arrays.asList("super_gluu_auth_method", "super_gluu_request")

        return None

    def getCountAuthenticationSteps(self, configurationAttributes):
        identity = CdiUtil.bean(Identity)
        if identity.isSetWorkingParameter("super_gluu_count_login_steps"):
            return identity.getWorkingParameter("super_gluu_count_login_steps")
        else:
            return 2

    def getPageForStep(self, configurationAttributes, step):
        if step == 1:
            if self.oneStep:
                return "/auth/super-gluu/login.xhtml"
        elif step == 2:
            if self.oneStep:
                return "/login.xhtml"
            else:
                identity = CdiUtil.bean(Identity)
                authmethod = identity.getWorkingParameter("super_gluu_auth_method")
                print "Super-Gluu. authmethod '%s'" % authmethod
                if authmethod == "enroll":
                    return "/auth/super-gluu/login.xhtml"
                else:
                    return "/auth/super-gluu/login.xhtml"

        return ""

    def getLogoutExternalUrl(self, configurationAttributes, requestParameters):
        print "Get external logout URL call"
        return None

    def logout(self, configurationAttributes, requestParameters):
        return True

    def processBasicAuthentication(self, credentials):
        authenticationService = CdiUtil.bean(AuthenticationService)

        user_name = credentials.getUsername()
        user_password = credentials.getPassword()

        logged_in = False
        if StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password):
            logged_in = authenticationService.authenticate(user_name, user_password)

        if not logged_in:
            return None

        find_user_by_uid = authenticationService.getAuthenticatedUser()
        if find_user_by_uid == None:
            print "Super-Gluu. Process basic authentication. Failed to find user '%s'" % user_name
            return None

        return find_user_by_uid

    def validateSessionDeviceStatus(self, client_redirect_uri, session_device_status, user_name = None):
        userService = CdiUtil.bean(UserService)
        deviceRegistrationService = CdiUtil.bean(DeviceRegistrationService)

        u2f_device_id = session_device_status['device_id']

        u2f_device = None
        if session_device_status['enroll'] and session_device_status['one_step']:
            u2f_device = deviceRegistrationService.findOneStepUserDeviceRegistration(u2f_device_id)
            if u2f_device == None:
                print "Super-Gluu. Validate session device status. There is no one step u2f_device '%s'" % u2f_device_id
                return False
        else:
            # Validate if user has specified device_id enrollment
            user_inum = userService.getUserInum(user_name)

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

    def initPushNotificationService(self, configurationAttributes):
        print "Super-Gluu. Initialize Native/SNS/Gluu notification services"

        self.pushSnsMode = False
        self.pushGluuMode = False
        if configurationAttributes.containsKey("notification_service_mode"):
            notificationServiceMode = configurationAttributes.get("notification_service_mode").getValue2()
            if StringHelper.equalsIgnoreCase(notificationServiceMode, "sns"):
                return self.initSnsPushNotificationService(configurationAttributes)
            elif StringHelper.equalsIgnoreCase(notificationServiceMode, "gluu"):
                return self.initGluuPushNotificationService(configurationAttributes)

        return self.initNativePushNotificationService(configurationAttributes)

    def initNativePushNotificationService(self, configurationAttributes):
        print "Super-Gluu. Initialize native notification services"

        creds = self.loadPushNotificationCreds(configurationAttributes)
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
            p12_password = ios_creds["p12_password"]

            try:
                encryptionService = CdiUtil.bean(EncryptionService)
                p12_password = encryptionService.decrypt(p12_password)
            except:
                # Ignore exception. Password is not encrypted
                print "Super-Gluu. Initialize native notification services. Assuming that 'p12_password' password in not encrypted"

            apnsServiceBuilder =  APNS.newService().withCert(p12_file_path, p12_password)
            if ios_creds["production"]:
                self.pushAppleService = apnsServiceBuilder.withProductionDestination().build()
            else:
                self.pushAppleService = apnsServiceBuilder.withSandboxDestination().build()

            self.pushAppleServiceProduction = ios_creds["production"]

            print "Super-Gluu. Initialize native notification services. Created iOS notification service"

        enabled = self.pushAndroidService != None or self.pushAppleService != None

        return enabled

    def initSnsPushNotificationService(self, configurationAttributes):
        print "Super-Gluu. Initialize SNS notification services"
        self.pushSnsMode = True

        creds = self.loadPushNotificationCreds(configurationAttributes)
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

    def initGluuPushNotificationService(self, configurationAttributes):
        print "Super-Gluu. Initialize Gluu notification services"

        self.pushGluuMode = True

        creds = self.loadPushNotificationCreds(configurationAttributes)
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

    def loadPushNotificationCreds(self, configurationAttributes):
        print "Super-Gluu. Initialize notification services"
        if not configurationAttributes.containsKey("credentials_file"):
            return None

        super_gluu_creds_file = configurationAttributes.get("credentials_file").getValue2()

        # Load credentials from file
        f = open(super_gluu_creds_file, 'r')
        try:
            creds = json.loads(f.read())
        except:
            print "Super-Gluu. Initialize notification services. Failed to load credentials from file:", super_gluu_creds_file
            return None
        finally:
            f.close()

        return creds

    def sendPushNotification(self, client_redirect_uri, user, super_gluu_request):
        try:
            self.sendPushNotificationImpl(client_redirect_uri, user, super_gluu_request)
        except:
            print "Super-Gluu. Send push notification. Failed to send push notification: ", sys.exc_info()[1]

    def sendPushNotificationImpl(self, client_redirect_uri, user, super_gluu_request):
        if not self.enabledPushNotifications:
            return

        user_name = user.getUserId()
        print "Super-Gluu. Send push notification. Loading user '%s' devices" % user_name

        send_notification = False
        send_notification_result = True

        userService = CdiUtil.bean(UserService)
        deviceRegistrationService = CdiUtil.bean(DeviceRegistrationService)

        user_inum = userService.getUserInum(user_name)

        send_android = 0
        send_ios = 0
        u2f_devices_list = deviceRegistrationService.findUserDeviceRegistrations(user_inum, client_redirect_uri, "oxId", "oxDeviceData", "oxDeviceNotificationConf")
        if u2f_devices_list.size() > 0:
            for u2f_device in u2f_devices_list:
                device_data = u2f_device.getDeviceData()

                # Device data which Super-Gluu gets during enrollment
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

                        title = "Super Gluu"
                        message = "Confirm your sign in request to: %s" % client_redirect_uri

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

    def getApplicationUri(self, session_attributes):
        if self.applicationId != None:
            return self.applicationId

        if not session_attributes.containsKey("redirect_uri"):
            return None

        return session_attributes.get("redirect_uri")

    def setRequestScopedParameters(self, identity, step):
        downloadMap = HashMap()
        if self.registrationUri != None:
            identity.setWorkingParameter("external_registration_uri", self.registrationUri)

        if self.androidUrl!= None and step == 1:
            downloadMap.put("android", self.androidUrl)

        if self.IOSUrl  != None and step == 1:
            downloadMap.put("ios", self.IOSUrl)

        if self.customLabel != None:
            identity.setWorkingParameter("super_gluu_label", self.customLabel)

        identity.setWorkingParameter("download_url", downloadMap)
        identity.setWorkingParameter("super_gluu_qr_options", self.customQrOptions)

    def addGeolocationData(self, session_attributes, super_gluu_request_dictionary):
        if session_attributes.containsKey("remote_ip"):
            remote_ip = session_attributes.get("remote_ip")
            if StringHelper.isNotEmpty(remote_ip):
                print "Super-Gluu. Prepare for step 2. Adding req_ip and req_loc to super_gluu_request"
                super_gluu_request_dictionary['req_ip'] = remote_ip

                remote_loc_dic = self.determineGeolocationData(remote_ip)
                if remote_loc_dic == None:
                    print "Super-Gluu. Prepare for step 2. Failed to determine remote location by remote IP '%s'" % remote_ip
                    return

                remote_loc = "%s, %s, %s" % ( remote_loc_dic['country'], remote_loc_dic['regionName'], remote_loc_dic['city'] )
                remote_loc_encoded = urllib.quote(remote_loc.encode('utf-8'))
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

    def isUserMemberOfGroup(self, user, attribute, group):
        is_member = False
        member_of_list = user.getAttributeValues(attribute)
        if (member_of_list != None):
            for member_of in member_of_list:
                if StringHelper.equalsIgnoreCase(group, member_of) or member_of.endswith(group):
                    is_member = True
                    break

        return is_member

    def processAuditGroup(self, user, attribute, group):
        is_member = self.isUserMemberOfGroup(user, attribute, group)
        if (is_member):
            print "Super-Gluu. Authenticate for processAuditGroup. User '%s' member of audit group" % user.getUserId()
            print "Super-Gluu. Authenticate for processAuditGroup. Sending e-mail about user '%s' login to %s" % (user.getUserId(), self.audit_email)

            # Send e-mail to administrator
            user_id = user.getUserId()
            mailService = CdiUtil.bean(MailService)
            subject = "User log in: %s" % user_id
            body = "User log in: %s" % user_id
            mailService.sendMail(self.audit_email, subject, body)
