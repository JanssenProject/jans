# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2016, Gluu
#
# Author: Yuriy Movchan
#

import sys
import datetime
import urllib

from org.xdi.model.custom.script.type.auth import PersonAuthenticationType
from org.jboss.seam.contexts import Contexts
from org.jboss.seam.security import Identity
from org.xdi.oxauth.service import UserService
from org.xdi.oxauth.service import SessionStateService
from org.xdi.oxauth.service.fido.u2f import DeviceRegistrationService
from org.xdi.util import StringHelper
from org.xdi.oxauth.util import ServerUtil
from org.xdi.util.security import StringEncrypter
from org.xdi.oxauth.model.config import ConfigurationFactory
from java.util import Arrays
from org.xdi.oxauth.service.net import HttpService
from org.apache.http.params import CoreConnectionPNames
from com.notnoop.apns import APNS
from com.google.android.gcm.server import Sender, Message


try:
    import json
except ImportError:
    import simplejson as json

class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, configurationAttributes):
        print "oxPush2. Initialization"

        if not configurationAttributes.containsKey("authentication_mode"):
            print "oxPush2. Initialization. Property authentication_mode is mandatory"
            return False

        self.registrationUri = None
        if configurationAttributes.containsKey("registration_uri"):
            self.registrationUri = configurationAttributes.get("registration_uri").getValue2()

        authentication_mode = configurationAttributes.get("authentication_mode").getValue2()
        if StringHelper.isEmpty(authentication_mode):
            print "oxPush2. Initialization. Failed to determine authentication_mode. authentication_mode configuration parameter is empty"
            return False
        
        self.oneStep = StringHelper.equalsIgnoreCase(authentication_mode, "one_step")
        self.twoStep = StringHelper.equalsIgnoreCase(authentication_mode, "two_step")

        if not (self.oneStep or self.twoStep):
            print "oxPush2. Initialization. Valid authentication_mode values are one_step and two_step"
            return False
        
        self.enabledPushNotifications = self.initPushNotificationService(configurationAttributes)

        print "oxPush2. Initialized successfully. oneStep: '%s', twoStep: '%s', pushNotifications: '%s'" % (self.oneStep, self.twoStep, self.enabledPushNotifications)

        return True   

    def destroy(self, configurationAttributes):
        print "oxPush2. Destroy"
        print "oxPush2. Destroyed successfully"
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

        client_redirect_uri = self.getClientRedirecUri(session_attributes)
        if client_redirect_uri == None:
            print "oxPush2. Authenticate. redirect_uri is not set"
            return False

        self.setRegistrationUri(context)

        userService = UserService.instance()
        deviceRegistrationService = DeviceRegistrationService.instance()
        if step == 1:
            print "oxPush2. Authenticate for step 1"
            if self.oneStep:
  
                session_device_status = self.getSessionDeviceStatus(session_attributes, user_name);
                if session_device_status == None:
                    return

                u2f_device_id = session_device_status['device_id']

                validation_result = self.validateSessionDeviceStatus(client_redirect_uri, session_device_status)
                if validation_result:
                    print "oxPush2. Authenticate for step 1. User successfully authenticated with u2f_device '%s'" % u2f_device_id
                else:
                    return False
                    
                if not session_device_status['one_step']:
                    print "oxPush2. Authenticate for step 1. u2f_device '%s' is not one step device" % u2f_device_id
                    return False
                    
                # There are two steps only in enrollment mode
                if session_device_status['enroll']:
                    return validation_result

                context.set("oxpush2_count_login_steps", 1)

                user_inum = session_device_status['user_inum']

                u2f_device = deviceRegistrationService.findUserDeviceRegistration(user_inum, u2f_device_id, "oxId")
                if u2f_device == None:
                    print "oxPush2. Authenticate for step 1. Failed to load u2f_device '%s'" % u2f_device_id
                    return False

                logged_in = userService.authenticate(user_name)
                if not logged_in:
                    print "oxPush2. Authenticate for step 1. Failed to authenticate user '%s'" % user_name
                    return False

                print "oxPush2. Authenticate for step 1. User '%s' successfully authenticated with u2f_device '%s'" % (user_name, u2f_device_id)
                
                return True;
            elif self.twoStep:
                authenticated_user = self.processBasicAuthentication(credentials)
                if authenticated_user == None:
                    return False
    
                auth_method = 'authenticate'
                enrollment_mode = ServerUtil.getFirstValue(requestParameters, "loginForm:registerButton")
                if StringHelper.isNotEmpty(enrollment_mode):
                    auth_method = 'enroll'
                
                if auth_method == 'authenticate':
                    user_inum = userService.getUserInum(authenticated_user)
                    u2f_devices_list = deviceRegistrationService.findUserDeviceRegistrations(user_inum, client_redirect_uri, "oxId")
                    if u2f_devices_list.size() == 0:
                        auth_method = 'enroll'
                        print "oxPush2. Authenticate for step 1. There is no U2F '%s' user devices associated with application '%s'. Changing auth_method to '%s'" % (user_name, client_redirect_uri, auth_method)
    
                print "oxPush2. Authenticate for step 1. auth_method: '%s'" % auth_method
                
                context.set("oxpush2_auth_method", auth_method)

                return True

            return False
        elif step == 2:
            print "oxPush2. Authenticate for step 2"
            session_attributes = context.get("sessionAttributes")

            session_device_status = self.getSessionDeviceStatus(session_attributes, user_name);
            if session_device_status == None:
                return False

            u2f_device_id = session_device_status['device_id']

            # There are two steps only in enrollment mode
            if self.oneStep and session_device_status['enroll']:
                authenticated_user = self.processBasicAuthentication(credentials)
                if authenticated_user == None:
                    return False

                user_inum = userService.getUserInum(authenticated_user)
                
                attach_result = deviceRegistrationService.attachUserDeviceRegistration(user_inum, u2f_device_id)

                print "oxPush2. Authenticate for step 2. Result after attaching u2f_device '%s' to user '%s': '%s'" % (u2f_device_id, user_name, attach_result) 

                return attach_result
            elif self.twoStep:
                if user_name == None:
                    print "oxPush2. Authenticate for step 2. Failed to determine user name"
                    return False

                validation_result = self.validateSessionDeviceStatus(client_redirect_uri, session_device_status, user_name)
                if validation_result:
                    print "oxPush2. Authenticate for step 2. User '%s' successfully authenticated with u2f_device '%s'" % (user_name, u2f_device_id)
                else:
                    return False
                
                oxpush2_request = json.loads(session_device_status['oxpush2_request'])
                auth_method = oxpush2_request['method']
                if auth_method in ['enroll', 'authenticate']:
                    return validation_result

                print "oxPush2. Authenticate for step 2. U2F auth_method is invalid"

            return False
        else:
            return False

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        context = Contexts.getEventContext()
        session_attributes = context.get("sessionAttributes")

        client_redirect_uri = self.getClientRedirecUri(session_attributes)
        if client_redirect_uri == None:
            print "oxPush2. Prepare for step. redirect_uri is not set"
            return False

        self.setRegistrationUri(context)

        if step == 1:
            print "oxPush2. Prepare for step 1"
            if self.oneStep:
                session_state = SessionStateService.instance().getSessionStateFromCookie()
                if StringHelper.isEmpty(session_state):
                    print "oxPush2. Prepare for step 2. Failed to determine session_state"
                    return False
            
                issuer = ConfigurationFactory.instance().getConfiguration().getIssuer()
                oxpush2_request_dictionary = {'app': client_redirect_uri,
                                   'issuer': issuer,
                                   'state': session_state,
                                   'created': datetime.datetime.now().isoformat()}

                self.addGeolocationData(session_attributes, oxpush2_request_dictionary)

                oxpush2_request = json.dumps(oxpush2_request_dictionary, separators=(',',':'))
                print "oxPush2. Prepare for step 1. Prepared oxpush2_request:", oxpush2_request
    
                context.set("oxpush2_request", oxpush2_request)
#            elif self.twoStep:
#                context.set("display_register_action", True)

            return True
        elif step == 2:
            print "oxPush2. Prepare for step 2"
            if self.oneStep:
                return True

            credentials = Identity.instance().getCredentials()
            user = credentials.getUser()
            if user == None:
                print "oxPush2. Prepare for step 2. Failed to determine user name"
                return False

            if session_attributes.containsKey("oxpush2_request"):
                print "oxPush2. Prepare for step 2. Request was generated already"
                return True
            
            session_state = SessionStateService.instance().getSessionStateFromCookie()
            if StringHelper.isEmpty(session_state):
                print "oxPush2. Prepare for step 2. Failed to determine session_state"
                return False

            auth_method = session_attributes.get("oxpush2_auth_method")
            if StringHelper.isEmpty(auth_method):
                print "oxPush2. Prepare for step 2. Failed to determine auth_method"
                return False

            print "oxPush2. Prepare for step 2. auth_method: '%s'" % auth_method
            
            issuer = ConfigurationFactory.instance().getConfiguration().getIssuer()
            oxpush2_request_dictionary = {'username': user.getUserId(),
                               'app': client_redirect_uri,
                               'issuer': issuer,
                               'method': auth_method,
                               'state': session_state,
                               'created': datetime.datetime.now().isoformat()}

            self.addGeolocationData(session_attributes, oxpush2_request_dictionary)

            oxpush2_request = json.dumps(oxpush2_request_dictionary, separators=(',',':'))
            print "oxPush2. Prepare for step 2. Prepared oxpush2_request:", oxpush2_request

            context.set("oxpush2_request", oxpush2_request)

            if auth_method in ['authenticate']:
                self.sendPushNotification(client_redirect_uri, user, oxpush2_request)

            return True
        else:
            return False

    def getExtraParametersForStep(self, configurationAttributes, step):
        if step == 1:
            if self.oneStep:        
                return Arrays.asList("oxpush2_request")
            elif self.twoStep:
                return Arrays.asList("display_register_action")
        elif step == 2:
            return Arrays.asList("oxpush2_auth_method", "oxpush2_request")
        
        return None

    def getCountAuthenticationSteps(self, configurationAttributes):
        context = Contexts.getEventContext()
        if context.isSet("oxpush2_count_login_steps"):
            return context.get("oxpush2_count_login_steps")
        else:
            return 2

    def getPageForStep(self, configurationAttributes, step):
        if step == 1:
            if self.oneStep:        
                return "/auth/oxpush2/login.xhtml"
        elif step == 2:
            if self.oneStep:
                return "/login.xhtml"
            else:
                return "/auth/oxpush2/login.xhtml"

        return ""

    def logout(self, configurationAttributes, requestParameters):
        return True

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
            print "oxPush. Process basic authentication. Failed to find user '%s'" % user_name
            return None
        
        return find_user_by_uid

    def validateSessionDeviceStatus(self, client_redirect_uri, session_device_status, user_name = None):
        userService = UserService.instance()
        deviceRegistrationService = DeviceRegistrationService.instance()

        u2f_device_id = session_device_status['device_id']

        u2f_device = None
        if session_device_status['enroll'] and session_device_status['one_step']:
            u2f_device = deviceRegistrationService.findOneStepUserDeviceRegistration(u2f_device_id)
            if u2f_device == None:
                print "oxPush2. Validate session device status. There is no one step u2f_device '%s'" % u2f_device_id
                return False
        else:
            # Validate if user has specified device_id enrollment
            user_inum = userService.getUserInum(user_name)

            if session_device_status['one_step']:
                user_inum = session_device_status['user_inum']
    
            u2f_device = deviceRegistrationService.findUserDeviceRegistration(user_inum, u2f_device_id)
            if u2f_device == None:
                print "oxPush2. Validate session device status. There is no u2f_device '%s' associated with user '%s'" % (u2f_device_id, user_inum)
                return False

        if not StringHelper.equalsIgnoreCase(client_redirect_uri, u2f_device.application):
            print "oxPush2. Validate session device status. u2f_device '%s' associated with other application '%s'" % (u2f_device_id, u2f_device.application)
            return False
        
        return True

    def getSessionDeviceStatus(self, session_attributes, user_name):
        print "oxPush2. Get session device status"

        if not session_attributes.containsKey("oxpush2_request"):
            print "oxPush2. Get session device status. There is no oxPush2 request in session attributes"
            return None

        # Check session state extended
        if not session_attributes.containsKey("session_custom_state"):
            print "oxPush2. Get session device status. There is no session_custom_state in session attributes"
            return None

        session_custom_state = session_attributes.get("session_custom_state")
        if not StringHelper.equalsIgnoreCase("approved", session_custom_state):
            print "oxPush2. Get session device status. User '%s' not approve or not pass U2F authentication. session_custom_state: '%s'" % (user_name, session_custom_state)
            return None

        # Try to find device_id in session attribute
        if not session_attributes.containsKey("oxpush2_u2f_device_id"):
            print "oxPush2. Get session device status. There is no u2f_device associated with this request"
            return None

        # Try to find user_inum in session attribute
        if not session_attributes.containsKey("oxpush2_u2f_device_user_inum"):
            print "oxPush2. Get session device status. There is no user_inum associated with this request"
            return None
        
        enroll = False
        if session_attributes.containsKey("oxpush2_u2f_device_enroll"):
            enroll = StringHelper.equalsIgnoreCase("true", session_attributes.get("oxpush2_u2f_device_enroll"))

        one_step = False
        if session_attributes.containsKey("oxpush2_u2f_device_one_step"):
            one_step = StringHelper.equalsIgnoreCase("true", session_attributes.get("oxpush2_u2f_device_one_step"))
                        
        oxpush2_request = session_attributes.get("oxpush2_request")
        u2f_device_id = session_attributes.get("oxpush2_u2f_device_id")
        user_inum = session_attributes.get("oxpush2_u2f_device_user_inum")

        session_device_status = {"oxpush2_request": oxpush2_request, "device_id": u2f_device_id, "user_inum" : user_inum, "enroll" : enroll, "one_step" : one_step}
        print "oxPush2. Get session device status. session_device_status: '%s'" % (session_device_status)
        
        return session_device_status

    def initPushNotificationService(self, configurationAttributes):
        print "oxPush2. Initialize notification services"
        if not configurationAttributes.containsKey("credentials_file"):
            return False

        oxpush2_creds_file = configurationAttributes.get("credentials_file").getValue2()

        # Load credentials from file
        f = open(oxpush2_creds_file, 'r')
        try:
            creds = json.loads(f.read())
        except:
            print "oxPush2. Initialize notification services. Failed to load credentials from file:", oxpush2_creds_file
            return False
        finally:
            f.close()
        
        try:
            android_creds = creds["android"]["gcm"]
            ios_creads = creds["ios"]["apns"]
        except:
            print "oxPush2. Initialize notification services. Invalid credentials file '%s' format:" % oxpush2_creds_file
            return False
        
        self.pushAndroidService = None
        self.pushAppleService = None
        if android_creds["enabled"]:
            self.pushAndroidService = Sender(android_creds["api_key"]) 
            print "oxPush2. Initialize notification services. Created Android notification service"
            
        if ios_creads["enabled"]:
            p12_file_path = ios_creads["p12_file_path"]
            p12_passowrd = ios_creads["p12_password"]

            try:
                stringEncrypter = StringEncrypter.defaultInstance()
                p12_passowrd = stringEncrypter.decrypt(p12_passowrd)
            except:
                # Ignore exception. Password is not encrypted
                print "oxPush2. Initialize notification services. Assuming that 'p12_passowrd' password in not encrypted"

            apnsServiceBuilder =  APNS.newService().withCert(p12_file_path, p12_passowrd)
            if ios_creads["production"]:
                self.pushAppleService = apnsServiceBuilder.withProductionDestination().build()
            else:
                self.pushAppleService = apnsServiceBuilder.withSandboxDestination().build()

            print "oxPush2. Initialize notification services. Created iOS notification service"

        enabled = self.pushAndroidService != None or self.pushAppleService != None

        return enabled

    def sendPushNotification(self, client_redirect_uri, user, oxpush2_request):
        if not self.enabledPushNotifications:
            return

        user_name = user.getUserId()
        print "oxPush2. Send push notification. Loading user '%s' devices" % user_name

        send_notification = False
        send_notification_result = True

        userService = UserService.instance()
        deviceRegistrationService = DeviceRegistrationService.instance()

        user_inum = userService.getUserInum(user_name)

        u2f_devices_list = deviceRegistrationService.findUserDeviceRegistrations(user_inum, client_redirect_uri, "oxId", "oxDeviceData")
        if u2f_devices_list.size() > 0:
            for u2f_device in u2f_devices_list:
                device_data = u2f_device.getDeviceData()

                # Device data which oxPush2 gets during enrollment
                if device_data == None:
                    continue

                platform = device_data.getPlatform()
                push_token = device_data.getPushToken()
                debug = False

                if StringHelper.equalsIgnoreCase(platform, "ios") and StringHelper.isNotEmpty(push_token):
                    # Sending notification to iOS user's device
                    if self.pushAppleService == None:
                        print "oxPush2. Send push notification. Apple push notification service is not enabled"
                    else:
                        send_notification = True
                        
                        title = "oxPush2"
                        message = "oxPush2 login request to: %s" % client_redirect_uri

                        additional_fields = { "request" : oxpush2_request }

                        msgBuilder = APNS.newPayload().alertBody(message).alertTitle(title).sound("default")
                        msgBuilder.category('ACTIONABLE').badge(0)
                        msgBuilder.customFields(additional_fields)

                        send_notification_result = self.pushAppleService.push(push_token, msgBuilder.build())
                        if debug:
                            print "oxPush2. Send push notification. token: '%s', send_notification_result: '%s'" % (push_token, send_notification_result)

                if StringHelper.equalsIgnoreCase(platform, "android") and StringHelper.isNotEmpty(push_token):
                    # Sending notification to Android user's device
                    if self.pushAndroidService == None:
                        print "oxPush2. Send push notification. Android push notification service is not enabled"
                    else:
                        send_notification = True

                        title = "oxPush2"
                        msgBuilder = Message.Builder().addData("message", oxpush2_request).addData("title", title).collapseKey("single");

                        send_notification_result = self.pushAndroidService.send(msgBuilder.build(), push_token, 3)
                        if debug:
                            print "oxPush2. Send push notification. token: '%s', send_notification_result: '%s'" % (push_token, send_notification_result)


        print "oxPush2. Send push notification. send_notification: '%s', send_notification_result: '%s'" % (send_notification, send_notification_result)

    def getClientRedirecUri(self, session_attributes):
        if not session_attributes.containsKey("redirect_uri"):
            return None

        return session_attributes.get("redirect_uri")

    def setRegistrationUri(self, context):
        if self.registrationUri != None:
            context.set("external_registration_uri", self.registrationUri)

    def addGeolocationData(self, session_attributes, oxpush2_request_dictionary):
        if session_attributes.containsKey("remote_ip"):
            remote_ip = session_attributes.get("remote_ip")
            if StringHelper.isNotEmpty(remote_ip):
                print "oxPush2. Prepare for step 2. Adding req_ip and req_loc to oxpush2_request"
                oxpush2_request_dictionary['req_ip'] = remote_ip

                remote_loc_dic = self.determineGeolocationData(remote_ip)
                if remote_loc_dic == None:
                    print "oxPush2. Prepare for step 2. Failed to determine remote location by remote IP '%s'" % remote_ip

                remote_loc = "%s, %s, %s" % ( remote_loc_dic['country'], remote_loc_dic['regionName'], remote_loc_dic['city'] )
                remote_loc_encoded = urllib.quote(remote_loc)
                oxpush2_request_dictionary['req_loc'] = remote_loc_encoded

    def determineGeolocationData(self, remote_ip):
        print "oxPush2. Determine remote location. remote_ip: '%s'" % remote_ip
        httpService = HttpService.instance();

        http_client = httpService.getHttpsClient();
        http_client_params = http_client.getParams();
        http_client_params.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 15 * 1000);
        
        geolocation_service_url = "http://ip-api.com/json/%s?fields=49177" % remote_ip
        geolocation_service_headers = { "Accept" : "application/json" }

        try:
            http_service_response = httpService.executeGet(http_client, geolocation_service_url,  geolocation_service_headers)
            http_response = http_service_response.getHttpResponse()
        except:
            print "oxPush2. Determine remote location. Exception: ", sys.exc_info()[1]
            return None

        try:
            if not httpService.isResponseStastusCodeOk(http_response):
                print "oxPush2. Determine remote location. Get invalid response from validation server: ", str(http_response.getStatusLine().getStatusCode())
                httpService.consume(http_response)
                return None
    
            response_bytes = httpService.getResponseContent(http_response)
            response_string = httpService.convertEntityToString(response_bytes)
            httpService.consume(http_response)
        finally:
            http_service_response.closeConnection()

        if response_string == None:
            print "oxPush2. Determine remote location. Get empty response from location server"
            return None
        
        response = json.loads(response_string)
        
        if not StringHelper.equalsIgnoreCase(response['status'], "success"):
            print "oxPush2. Determine remote location. Get response with status: '%s'" % response['status']
            return None

        return response
