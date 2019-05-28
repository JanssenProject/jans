
from com.google.android.gcm.server import Sender, Message
from com.notnoop.apns import APNS

from java.time import ZonedDateTime
from java.time.format import DateTimeFormatter
from javax.ws.rs import InternalServerErrorException

from org.gluu.oxnotify.model import NotifyMetadata
from org.gluu.oxnotify.client import NotifyClientFactory
from org.apache.http.params import CoreConnectionPNames
from org.gluu.oxauth.service import EncryptionService , UserService
from org.gluu.oxauth.service.fido.u2f import DeviceRegistrationService
from org.gluu.oxauth.service.net import HttpService
from org.gluu.oxauth.service.push.sns import PushPlatform, PushSnsService
from org.gluu.service.cdi.util import CdiUtil
from org.gluu.util import StringHelper


import json
import sys

#
# PushNotificationContext Class
#

class PushNotificationContext:
    def __init__(self, appId, superGluuRequest):

        self.appId = appId
        self.superGluuRequest = superGluuRequest
        self.debugEnabled = False
        self.deviceRegistrationService = CdiUtil.bean(DeviceRegistrationService)
        self.pushSnsService = CdiUtil.bean(PushSnsService)
        self.user = None
        self.u2fDevice = None
        self.devicePlatform = None
        self.pushToken = None

#
# PushNotificationManager Class
# 
class PushNotificationManager:
    def __init__(self, serviceMode, credentialsFile):

        self.pushSnsMode = False
        self.pushGluuMode = False
        self.pushNotificationsEnabled = False
        self.titleTemplate = "Super-Gluu"
        self.messageTemplate = "Super-Gluu login request to %s"
        self.debugEnabled = True
        self.httpConnTimeout = 15 * 1000 # in milliseconds 
        creds = self.loadCredentials(credentialsFile)
        if creds == None:
            return None
        
        if StringHelper.equalsIgnoreCase(serviceMode,"sns"):
            self.initSnsPushNotifications(creds)
        elif StringHelper.equalsIgnoreCase(serviceMode,"gluu"):
            self.initGluuPushNotifications(creds)
        else:
            self.initNativePushNotifications(creds)
    
    def initSnsPushNotifications(self, creds):

        print "Super-Gluu-Push. SNS push notifications init ..."
        self.pushSnsMode = True
        try:
            sns_creds = creds["sns"]
            android_creds = creds["android"]["sns"]
            ios_creds = creds["ios"]["sns"]
        except:
            print "Super-Gluu-Push. Invalid SNS credentials format"
            return None
        
        self.pushAndroidService = None
        self.pushAppleService = None
        if not (android_creds["enabled"] or ios_creds["enabled"]):
            print "Super-Gluu-Push. SNS disabled for all platforms"
            return None
        
        sns_access_key = sns_creds["access_key"]
        sns_secret_access_key = sns_creds["secret_access_key"]
        sns_region = sns_creds["region"]

        encryptionService = CdiUtil.bean(EncryptionService)

        try:
            sns_secret_access_key = encryptionService.decrypt(sns_secret_access_key)
        except:
            # Ignore exception. Password is not encrypted
            print "Super-Gluu-Push. Assuming 'sns_access_key' is not encrypted"
        
        pushSnsService = CdiUtil.bean(PushSnsService)
        pushClient = pushSnsService.createSnsClient(sns_access_key,sns_secret_access_key,sns_region)
        
        if android_creds["enabled"]:
            self.pushAndroidService = pushClient
            self.pushAndroidPlatformArn = android_creds["platform_arn"]
            print "Super-Gluu-Push. Created SNS android notification service"
        
        if ios_creds["enabled"]:
            self.pushAppleService = pushClient
            self.pushApplePlatformArn = ios_creds["platform_arn"]
            self.pushAppleServiceProduction = ios_creds["production"]
        

        self.pushNotificationsEnabled = self.pushAndroidService != None or self.pushAppleService != None
    
    
    def initGluuPushNotifications(self, creds):
        print "Super-Gluu-Push. Gluu push notifications init ... "

        self.pushGluuMode = True

        try:
            gluu_conf = creds["gluu"]
            android_creds = creds["android"]["gluu"]
            ios_creds = creds["ios"]["gluu"]
        except:
            print "Super-Gluu-Push. Invalid Gluu credentials format"
            return None
        
        self.pushAndroidService = None
        self.pushAppleService = None

        if not(android_creds["enabled"] or ios_creds["enabled"]):
            print "Super-Gluu-Push. Gluu disabled for all platforms"
            return None
        
        gluu_server_uri = gluu_conf["server_uri"]
        notifyClientFactory  = NotifyClientFactory.instance()
        metadataConfiguration = self.getNotifyMetadata(gluu_server_uri)
        if metadataConfiguration == None:
            return None
         
        gluuClient = notifyClientFactory.createNotifyService(metadataConfiguration)
        encryptionService = CdiUtil.bean(EncryptionService)

        if android_creds["enabled"]:
            gluu_access_key = android_creds["access_key"]
            gluu_secret_access_key = android_creds["secret_access_key"]

            try:
                gluu_secret_access_key = encryptionService.decrypt(gluu_secret_access_key)
            except:
                # Ignore exception. Password is not encrypted
                print "Super-Gluu-Push. Assuming 'gluu_secret_access_key' is not encrypted"
            
            self.pushAndroidService = gluuClient
            self.pushAndroidServiceAuth = notifyClientFactory.getAuthorization(gluu_access_key,gluu_secret_access_key)
            print "Super-Gluu-Push. Created Gluu Android notification service"
        
        if ios_creds["enabled"]:
            gluu_access_key = ios_creds["access_key"]
            gluu_secret_access_key = ios_creds["secret_access_key"]

            try:
                gluu_secret_access_key = encryptionService.decrypt(gluu_secret_access_key)
            except:
                # Ignore exception. Password is not encrypted
                print "Super-Gluu-Push. Assuming 'gluu_secret_access_key' is not encrypted"
            self.pushAppleService = gluuClient
            self.pushAppleServiceAuth = notifyClientFactory.getAuthorization(gluu_access_key,gluu_secret_access_key)
            print "Super-Gluu-Push. Created Gluu iOS notification service"
        
        self.pushNotificationsEnabled = self.pushAndroidService != None or self.pushAppleService != None
    
    
    def initNativePushNotifications(self, creds):
        print "Super-Gluu-Push. Native push notifications init ... "
        try:
            android_creds = creds["android"]["gcm"]
            ios_creds = creds["ios"]["apns"]
        except:
            print "Super-Gluu-Push. Invalid credentials format"
            return None
        
        self.pushAndroidService = None
        self.pushAppleService = None

        if android_creds["enabled"]:
            self.pushAndroidService = Sender(android_creds["api_key"])
            print "Super-Gluu-Push. Created native Android notification service"
        
        if ios_creds["enabled"]:
            p12_file_path = ios_creds["p12_file_path"]
            p12_password  = ios_creds["p12_password"]

            try:
                encryptionService = CdiUtil.bean(EncryptionService)
                p12_password = encryptionService.decrypt(p12_password)
            except:
                # Ignore exception. Password is not encrypted
                print "Super-Gluu-Push. Assuming 'p12_password' is not encrypted"

            apnsServiceBuilder = APNS.newService().withCert(p12_file_path,p12_password)
            if ios_creds["production"]:
                self.pushAppleService = apnsServiceBuilder.withProductionDestination().build()
            else:
                self.pushAppleService = apnsServiceBuilder.withSandboxDestination().build()
            
            self.pushAppleServiceProduction = ios_creds["production"]
            print "Super-Gluu-Push. Created native iOS notification service"
        
        self.pushNotificationsEnabled = self.pushAndroidService != None or self.pushAppleService != None

    
    def loadCredentials(self, credentialsFile):
        print "Super-Gluu-Push. Loading credentials ... "
        f = open(credentialsFile,'r')
        try:
            creds = json.loads(f.read())
            print "Super-Gluu-Push. Credentials loaded successfully"
        except:
            exception_value = sys.exc_info()[1]
            print "Super-Gluu-Push. Loading credentials failed.", exception_value 
            return None
        finally:
            f.close()
        
        return creds
    
    def getNotifyMetadata(self, gluu_server_uri):

        try:
            notifyClientFactory  = NotifyClientFactory.instance()
            metadataConfigurationService = notifyClientFactory.createMetaDataConfigurationService(gluu_server_uri)
            return metadataConfigurationService.getMetadataConfiguration()
        except:
            exc_value = sys.exc_info()[1]
            print "Super-Gluu-Push. Gluu push notification init failed while loading metadata. %s." % exc_value
            print "Super-Gluu-Push. Retrying loading metadata using httpService..."
            httpService = CdiUtil.bean(HttpService)
            http_client = httpService.getHttpsClient()
            http_client_params = http_client.getParams()
            http_client_params.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT,self.httpConnTimeout)
            notify_service_url = "%s/.well-known/notify-configuration" % gluu_server_uri
            notify_service_headers = {"Accept": "application/json"}
            try:
                http_service_response = httpService.executeGet(http_client,notify_service_url,notify_service_headers)
                http_response = http_service_response.getHttpResponse()
            except:
                print "Super-Gluu-Push. Loading metadata using httpService failed. %s." % sys.exc_info()[1]
                return None
            
            try:
                if not httpService.isResponseStastusCodeOk(http_response):
                    http_error_str = str(http_response.getStatusLine().getStatusCode())
                    print "Super-Gluu-Push. Loading metadata using httpService failed with http code %s." % http_error_str
                    httpService.consume(http_response)
                    return None
                resp_bytes = httpService.getResponseContent(http_response)
                resp_str = httpService.convertEntityToString(resp_bytes)
                httpService.consume(http_response)
            except:
                print "Super-Gluu-Push. Loading metadata using httpService failed. %s." % sys.exc_info()[1]
                return None
            finally:
                http_service_response.closeConnection()
            
            if resp_str == None:
                print "Super-Gluu-Push. Loading metadata using httpService failed.Empty response from server"
                return None
            
            json_resp = json.loads(resp_str)
            if ('version' not in json_resp) or ('issuer' not in json_resp):
                print "Super-Gluu-Push. Loading metadata using httpService failed. Invalid json response %s." % json_resp
                return None
            
            if ('notify_endpoint' not in json_resp) and ('notifyEndpoint' not in json_resp):
                print "Super-Gluu-Push. Loading metadata using httpService failed. Invalid json response %s." % json_resp
                return None
            
            notifyMeta = NotifyMetadata()
            notifyMeta.setVersion(json_resp['version'])
            notifyMeta.setIssuer(json_resp['issuer'])
            if 'notify_endpoint' in json_resp:
                notifyMeta.setNotifyEndpoint(json_resp['notify_endpoint'])
            elif 'notifyEndpoint' in json_resp: 
                notifyMeta.setNotifyEndpoint(json_resp['notifyEndpoint'])
            print "Super-Gluu-Push. Metadata loaded using httpService successfully"
            return notifyMeta
    
    def sendPushNotification(self, user, app_id, super_gluu_request):
        try:
            return self.sendPushNotificationImpl(user, app_id, super_gluu_request)
        except InternalServerErrorException as is_error:
            print "Super-Gluu-Push. Failed to send push notification : '%s'" % is_error.getMessage()
            return 0
        except:
            exception_value = sys.exc_info()[1]
            print "Super-Gluu-Push. Failed to send push notification :" % exception_value
            return 0
    
    def sendPushNotificationImpl(self, user, app_id, super_gluu_request):

        if not self.pushNotificationsEnabled:
            print "Super-Gluu-Push. Push notifications are disabled"
            return None
        
        user_name = user.getUserId()
        print "Super-Gluu-Push. Sending push notification to user '%s' devices" % user_name

        userService = CdiUtil.bean(UserService)
        deviceRegistrationService = CdiUtil.bean(DeviceRegistrationService)

        user_inum = userService.getUserInum(user_name)

        u2f_device_list = deviceRegistrationService.findUserDeviceRegistrations(user_inum, app_id, 
            "oxId","oxDeviceData","oxDeviceNotificationConf")
        
        send_ios = 0
        send_android = 0
        if u2f_device_list.size() > 0:
            for u2f_device in u2f_device_list:
                print "Super-Gluu-Push. Send device notification to device"
                device_push_result = self.sendDevicePushNotification(user, app_id, u2f_device, super_gluu_request)
                send_ios += device_push_result["send_ios"]
                send_android += device_push_result["send_android"]
        else:
            print "Super-Gluu-Push. No device enrolled for user '%s'" % user_name
            return 0
        
        msg = """Super-Gluu-Push. Send push notification. send_android: '%s', send_ios: '%s' """
        print msg % (send_android, send_ios)
        return send_android + send_ios
                


        
                
    
    def sendDevicePushNotification(self, user, app_id, u2f_device, super_gluu_request):

        device_data = u2f_device.getDeviceData()
        if device_data == None:
            return {"send_android":0,"send_ios":0}
        
        platform = device_data.getPlatform()
        push_token = device_data.getPushToken()
        pushNotificationContext = PushNotificationContext(app_id,super_gluu_request)
        pushNotificationContext.debugEnabled = self.debugEnabled
        pushNotificationContext.user = user
        pushNotificationContext.u2fDevice = u2f_device
        pushNotificationContext.devicePlatform = platform
        pushNotificationContext.pushToken = push_token
        send_ios = 0
        send_android = 0

        if StringHelper.equalsIgnoreCase(platform,"ios") and StringHelper.isNotEmpty(push_token):
            # Sending notification to iOS user's device
            if self.pushAppleService == None:
                print "Super-Gluu-Push. Apple push notification service disabled"
            else:
                self.sendApplePushNotification(pushNotificationContext)
                send_ios = 1
        
        if StringHelper.equalsIgnoreCase(platform,"android") and StringHelper.isNotEmpty(push_token):
            # Sending notification to android user's device
            if self.pushAndroidService == None:
                print "Super-Gluu-Push. Android push notification service disabled"
            else:
                self.sendAndroidPushNotification(pushNotificationContext)
                send_android = 1
            
        
        return {"send_android":send_android,"send_ios":send_ios}


                

    def sendApplePushNotification(self, pushNotificationContext):
       
        if self.pushSnsMode or self.pushGluuMode:
            if self.pushSnsMode:
                self.sendApplePushSnsNotification(pushNotificationContext)
            elif self.pushGluuMode:
                self.sendApplePushGluuNotification(pushNotificationContext)
        else:
            self.sendApplePushNativeNotification(pushNotificationContext)
    
    def sendAndroidPushNotification(self, pushNotificationContext):

        if self.pushSnsMode or self.pushGluuMode:
            if self.pushSnsMode:
                self.sendAndroidPushSnsNotification(pushNotificationContext)
            elif self.pushGluuMode:
                self.sendAndroidPushGluuNotification(pushNotificationContext)
        else:
            self.sendAndroidPushNativeNotification(pushNotificationContext)
    


    def sendApplePushSnsNotification(self, pushNotificationContext):

        debug = pushNotificationContext.debugEnabled
        apple_push_platform = PushPlatform.APNS
        targetEndpointArn = self.getTargetEndpointArn(apple_push_platform,pushNotificationContext)
        if targetEndpointArn == None:
            return None
        
        push_message = self.buildApplePushMessage(pushNotificationContext)
        apple_push_platform = PushPlatform.APNS
        if not self.pushAppleServiceProduction:
            apple_push_platform = PushPlatform.APNS_SANDBOX
        
        pushSnsService = pushNotificationContext.pushSnsService
        send_notification_result = pushSnsService.sendPushMessage(self.pushAppleService, apple_push_platform, targetEndpointArn, push_message, None)
        if debug:
            dbg_msg = """Super-Gluu-Push. Send iOS SNS push notification. 
                          message: '%s', send_notification_result: '%s'"""
            print dbg_msg % (push_message, send_notification_result)
    
    def sendAndroidPushSnsNotification(self, pushNotificationContext):

        debug = pushNotificationContext.debugEnabled
        android_push_platform = PushPlatform.GCM
        targetEndpointArn = self.getTargetEndpointArn(android_push_platform, pushNotificationContext)
        if targetEndpointArn == None:
            return None
        pushSnsService = pushNotificationContext.pushSnsService
        push_message = self.buildAndroidPushMessage(pushNotificationContext)
        send_notification_result = pushSnsService.sendPushMessage(self.pushAndroidService, android_push_platform, targetEndpointArn, push_message, None)
        if debug:
            dbg_msg = """Super-Gluu-Push. Send Android SNS push notification.
                          message:'%s', send_notification_result: '%s'"""
            print dbg_msg % (push_message, send_notification_result)
        
    
    
    def sendApplePushGluuNotification(self, pushNotificationContext):

        debug = pushNotificationContext.debugEnabled
        apple_push_platform = PushPlatform.APNS
        targetEndpointArn = self.getTargetEndpointArn(apple_push_platform, pushNotificationContext)
        if targetEndpointArn == None:
            return None
        
        push_message = self.buildApplePushMessage(pushNotificationContext)
        print "push message : %s" % push_message
        send_notification_result = self.pushAppleService.sendNotification(self.pushAppleServiceAuth, targetEndpointArn, push_message)
        print "push message sent"
        if debug:
            dbg_msg = """Super-Gluu-Push. Send iOS gluu push notification. 
                          message: '%s', send_notification_result: '%s'"""
            print dbg_msg % (push_message, send_notification_result)
    
    def sendAndroidPushGluuNotification(self, pushNotificationContext):
        
        debug = pushNotificationContext.debugEnabled
        android_push_platform = PushPlatform.GCM
        targetEndpointArn = self.getTargetEndpointArn(android_push_platform, pushNotificationContext)
        if targetEndpointArn == None:
            return None
        push_message = self.buildAndroidPushMessage(pushNotificationContext)
        send_notification_result = self.pushAndroidService.sendNotification(self.pushAndroidServiceAuth, targetEndpointArn, push_message)
        if debug:
            dbg_msg = """Super-Gluu-Push. Send Android gluu push notification.
                          message: '%s', send_notification_result: '%s' """
            print dbg_msg % (push_message,send_notification_result)
        pass
    
    def sendApplePushNativeNotification(self, pushNotificationContext):

        title = self.titleTemplate
        message = self.messageTemplate % pushNotificationContext.appId
        push_token = pushNotificationContext.pushToken
        additional_fields = {"request": pushNotificationContext.superGluuRequest}
        debug = pushNotificationContext.debugEnabled
        msgBuilder = APNS.newPayload().alertBody(message).alertTitle(title).sound("default")
        msgBuilder.forNewsstand()
        msgBuilder.customFields(additional_fields)
        push_message = msgBuilder.build()
        send_notification_result = self.pushAppleService.push(push_token, push_message)
        if debug:
            dbg_msg = """Super-Gluu-Push. Send iOS native push notification. 
                          push_token:'%s', message: '%s', send_notification_result: '%s'"""
            print dbg_msg % (push_token, push_message, send_notification_result)
    

    def sendAndroidPushNativeNotification(self, pushNotificationContext):
        title = self.titleTemplate
        superGluuRequest = pushNotificationContext.superGluuRequest
        msgBuilder = Message.Builder().addData("message", superGluuRequest).addData("title",title).collapseKey("single").contentAvailable(True)
        push_message = msgBuilder.build()
        push_token = pushNotificationContext.pushToken
        send_notification_result = self.pushAndroidService.send(push_message, push_token, 3)
        if pushNotificationContext.debugEnabled:
            dbg_msg = """Super-Gluu-Push. Send iOS native push notification. 
                          push_token:'%s', message: '%s', send_notification_result: '%s'"""
            print dbg_msg % (push_token, push_message, send_notification_result)
        
            
    
    def buildApplePushMessage(self, pushNotificationContext):
        
        title = self.titleTemplate
        message = self.messageTemplate % pushNotificationContext.appId
        sns_push_request_dictionary = {
            "request": pushNotificationContext.superGluuRequest,
            "aps": {
                "badge": 0,
                "alert": {"body":message,"title":title},
                "category": "ACTIONABLE",
                "content-available": "1",
                "sound": "default"
            }
        }
        return json.dumps(sns_push_request_dictionary,separators=(',',':'))
    
    def buildAndroidPushMessage(self, pushNotificationContext):

        sns_push_request_dictionary = {
            "collapse_key": "single",
            "content_available": True,
            "time_to_live": 60,
            "data": {
                "message": pushNotificationContext.superGluuRequest,
                "title": self.titleTemplate
            }
        }
        return json.dumps(sns_push_request_dictionary,separators=(',',':'))
      
    def getTargetEndpointArn(self, platform, pushNotificationContext):
        
        deviceRegistrationService = pushNotificationContext.deviceRegistrationService
        pushSnsService = pushNotificationContext.pushSnsService
        user = pushNotificationContext.user
        u2fDevice  = pushNotificationContext.u2fDevice
        targetEndpointArn = None

        # Return endpoint ARN if it is created already
        notificationConf = u2fDevice.getDeviceNotificationConf()
        if StringHelper.isNotEmpty(notificationConf):
            notificationConfJson = json.loads(notificationConf)
            targetEndpointArn = notificationConfJson['sns_endpoint_arn']
            if StringHelper.isNotEmpty(targetEndpointArn):
                print "Super-Gluu-Push. Target endpoint ARN already created : ", targetEndpointArn
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
            print "Super-Gluu-Push. Unsupported platform for ARN."
            return None
        
        deviceData = u2fDevice.getDeviceData()
        pushToken  = deviceData.getPushToken()

        print "Super-Gluu-Push. Attempting to create target endpoint ARN for user: %s" % user.getUserId()
        if self.pushSnsMode:
            targetEndpointArn = pushSnsService.createPlatformArn(pushClient,platformApplicationArn,pushToken,user)
        else:
            customUserData = pushSnsService.getCustomUserData(user)
            registerDeviceResponse = pushClient.registerDevice(pushClientAuth, pushToken, customUserData)
            if registerDeviceResponse != None and registerDeviceResponse.getStatusCode() == 200:
                targetEndpointArn = registerDeviceResponse.getEndpointArn()
        
        if StringHelper.isEmpty(targetEndpointArn):
            print "Super-Gluu-Push. Failed to get endpoint ARN for user: '%s'" % user.getUserId()
            return None
        
        printmsg = "Super-Gluu-Push. Create target endpoint ARN '%s' for user '%s'"
        print printmsg % (targetEndpointArn, user.getUserId())
        
        # Store created endpoint ARN in device entry
        userInum = user.getAttribute("inum")
        u2fDeviceUpdate = deviceRegistrationService.findUserDeviceRegistration(userInum, u2fDevice.getId())
        u2fDeviceUpdate.setDeviceNotificationConf('{"sns_endpoint_arn": "%s"}' % targetEndpointArn)
        deviceRegistrationService.updateDeviceRegistration(userInum,u2fDeviceUpdate)

        return targetEndpointArn


#
# GeolocationData Class
#
class GeolocationData:
    def __init__(self,response):
        self.city = response['city']
        self.country = response['country']
        self.region = response['regionName']
    

#
# Network Api
#
class NetworkApi:
    def __init__(self, conn_timeout =15 * 1000):
        self.conn_timeout = conn_timeout
        print "NetApi. {conn_timeout=%d}" % conn_timeout
    
    def get_remote_ip_from_request(self, servletRequest):
        try:
            remote_ip = servletRequest.getHeader("X-FORWARDED-FOR")
            if StringHelper.isEmpty(remote_ip):
                remote_ip = servletRequest.getRemoteAddr()
            
            return remote_ip
        except:
            print "NetApi. Could not determine remote location: ", sys.exc_info()[1]
        
        return None
        
    
    def get_geolocation_data(self, remote_ip):
        print "NetApi. Determining remote location for ip address '%s'" % remote_ip
        httpService = CdiUtil.bean(HttpService)

        http_client = httpService.getHttpsClient()
        http_client_params = http_client.getParams()
        http_client_params.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT,self.conn_timeout)

        geolocation_service_url = "http://ip-api.com/json/%s?fields=49177" % remote_ip
        geolocation_service_headers = { "Accept": "application/json"}

        try:
            http_service_response = httpService.executeGet(http_client,geolocation_service_url,geolocation_service_headers)
            http_response = http_service_response.getHttpResponse()
        except:
            print "NetApi. Could not determine remote location: ", sys.exc_info()[1]
            return None
        
        try:
            if not httpService.isResponseStastusCodeOk(http_response):
                http_error_str = str(http_response.getStatusLine().getStatusCode())
                print "NetApi. Could not determine remote location: ",http_error_str
                httpService.consume(http_response)
                return None
            
            response_bytes = httpService.getResponseContent(http_response)
            response_string = httpService.convertEntityToString(response_bytes)
            httpService.consume(http_response)
        finally:
            http_service_response.closeConnection()
        
        if response_string == None:
            print "NetApi. Could not determine remote location. Empty respone from server"
            return None
        
        response = json.loads(response_string)

        if not StringHelper.equalsIgnoreCase(response['status'],"success"):
            print "NetApi. Could not determine remote location. ip-api status: '%s'" % response['status']
            return None
        
        return GeolocationData(response)


#
# SuperGluuRequestBuilder class 
# 

class SuperGluuRequestBuilder:
    def __init__(self, method="authenticate"):
        self.username = ''
        self.app = ''
        self.issuer = ''
        self.state = ''
        self.method = method
        self.licensed = False
        self.created = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now().withNano(0))
        self.req_ip = ''
        self.req_loc = ''
    
    def is_authenticate_method(self):
        self.method = "authenticate"
    
    def is_enroll_method(self):
        self.method = "enroll"
    
    def requestLocation(self, geoloc_data):
        if geoloc_data != None:
            self.req_loc = "%s, %s, %s" % (geoloc_data.country, geoloc_data.city, geoloc_data.region)
        else:
            self.req_loc = ""
    
    def build(self):
        request_dict = {
            "username" : self.username,
            "app" : self.app,
            "issuer" : self.issuer,
            "state": self.state,
            "method": self.method,
            "licensed" : self.licensed,
            "created" : self.created,
            "req_ip" : self.req_ip,
            "req_loc" : self.req_loc
        }

        return json.dumps(request_dict,separators=(',',':'))

