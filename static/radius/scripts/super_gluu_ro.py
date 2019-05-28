# Super Gluu Radius Resource Owner Password Credentials Script
# Copyright (c) 2019 Gluu Inc.

from java.util import Date , HashMap
from org.gluu.oxnotify.client import NotifyClientFactory

from org.gluu.model.custom.script.type.owner import ResourceOwnerPasswordCredentialsType
from org.gluu.oxauth.model.common import SessionIdState
from org.gluu.oxauth.model.config import ConfigurationFactory , Constants
from org.gluu.oxauth.security import Identity
from org.gluu.oxauth.service import EncryptionService , UserService , AuthenticationService , SessionIdService
from org.gluu.oxauth.service.push.sns import PushPlatform, PushSnsService
from org.gluu.service.cdi.util import CdiUtil
from org.gluu.util import StringHelper
from gluu_common import PushNotificationManager, NetworkApi, GeolocationData, SuperGluuRequestBuilder

import java
import json
import sys


class ResourceOwnerPasswordCredentials(ResourceOwnerPasswordCredentialsType):
    def __init__(self,currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis
        self.initiateAuthStepName = "initiate_auth"
        self.verifyAuthStepName =   "verify_auth"
        self.resendNotificationStepName = "resend_notification"
        self.stepParamName  = "__step"
        self.acrvaluesParamName =  "__acr_values"
        self.usernameParamName  =  "username"
        self.passwordParamName  =  "__password"
        self.idtokenParamName   =  "__id_token"
        self.sessionIdParamName =  "__session_id"
        self.remoteIpParamName  =  "__remote_ip"
        self.sessionIdClaimName =  "__session_id"
        self.clientIdSessionParamName = "__client_id"
    
    def init(self, configurationAttributes):

        print "Super-Gluu-RO Init"
        if not configurationAttributes.containsKey("application_id"):
            print "Super-Gluu-Radius RO PW Init Failed. application_id property is required"
            return False
        
        if not configurationAttributes.containsKey("credentials_file"):
            print "Super-Gluu-RO Init Failed. credentials_file is required"
            return False
        
        notificationServiceMode = None
        if configurationAttributes.containsKey("notification_service_mode"):
            notificationServiceMode = configurationAttributes.get("notification_service_mode").getValue2()
        
        
        self.applicationId = "*" # wildcard. Selects all devices irrespective of the application 
        if configurationAttributes.containsKey("application_id"):
            self.applicationId = configurationAttributes.get("application_id").getValue2()
        
        credentialsFile = configurationAttributes.get("credentials_file").getValue2()

        if configurationAttributes.containsKey("push_notification_title"):
            self.pushNotificationManager.titleTemplate = configurationAttributes.get("push_notification_title").getValue2()
        
        if configurationAttributes.containsKey("push_notification_message"):
            self.pushNotificationManager.messageTemplate = configurationAttributes.get("push_notification_message").getValue2()
        
        self.authWithoutPassword = False
        if configurationAttributes.containsKey("auth_without_password"):
            auth_without_password = configurationAttributes.get("auth_without_password").getValue2()
            if StringHelper.equalsIgnoreCase(auth_without_password,"yes"):
                self.authWithoutPassword = True
        
        self.issuerId = CdiUtil.bean(ConfigurationFactory).getAppConfiguration().getIssuer()
        if configurationAttributes.containsKey("issuer_id"):
            self.issuerId = configurationAttributes.get("issuer_id").getValue2()
        
        self.pushNotificationManager = PushNotificationManager(notificationServiceMode,credentialsFile)
        self.networkApi = NetworkApi()

        return True
    
    def destroy(self, configurationAttributes):

        print "Super-Gluu-RO. Destroy"
        self.pushNotificationManager = None
        print "Super-Gluu-RO. Destroyed Successfully"

        return True
    
    def getApiVersion(self):
        return 1
    
    def authenticate(self, context): 
        if self.perform_preliminary_user_authentication(context) == False:
            print "Super-Gluu-Radius. User authentication state not validated"
            return False
        
        step = context.getHttpRequest().getParameter(self.stepParamName)
        if StringHelper.equalsIgnoreCase(step,self.initiateAuthStepName):
            return self.initiate_authentication(context)
        elif StringHelper.equalsIgnoreCase(step,self.resendNotificationStepName):
            return self.resend_push_notification(context)
        elif StringHelper.equalsIgnoreCase(step,self.verifyAuthStepName):
            return self.verify_authentication(context)
        else:
            context.setUser(None)
            print "Super-Gluu-RO. Unknown authentication step '%s'" % step
            return False
    
    def initiate_authentication(self, context):
        print "Super-Gluu-RO initiatate_authentication"
        client = CdiUtil.bean(Identity).getSessionClient().getClient()
        sessionId = self.new_unauthenticated_session(context.getUser(),client)
        # set session id in identity object
        # this will be used by our dynamic scope script
        identity = CdiUtil.bean(Identity)
        identity.setSessionId(sessionId)
        if not self.send_push_notification_to_user(sessionId,context):
            context.setUser(None)
            print "Send push notification to user '%s' failed " % context.getUser().getUserId()
            return False
        print "Super-Gluu-RO initiate_authentication complete"
        return True
    
    def resend_push_notification(self,context):
        print "Super-Gluu-RO resend_push_notification"

        sessionIdService = CdiUtil.bean(SessionIdService)
        session_id = context.getHttpRequest().getParameter(self.sessionIdParamName)
        if session_id == None:
            print "Super-Gluu-RO. No session_id was specified for resend_push_notification"
            context.setUser(None)
            return False
        
        sessionId = sessionIdService.getSessionId(session_id)
        if sessionId == None:
            print "Super-Gluu-RO. Session '%s' does not exist or has expired" % session_id
            context.setUser(None)
            return False
        
        client = CdiUtil.bean(Identity).getSessionClient().getClient()
        if not self.verify_session_ownership(sessionId,context.getUser(),client):
            print "Super-Gluu-RO. resend_push_notification_failed due to invalid session ownership"
            context.setUser(None)
            return False
        
        self.send_push_notification_to_user(sessionId,context)
        print "Super-Gluu-RO resend_push_notification complete"
        return True
    
    def verify_authentication(self, context):
        print "Super-Gluu-RO verify_authentication"
        session_id = context.getHttpRequest().getParameter(self.sessionIdParamName)
        sessionId = CdiUtil.bean(SessionIdService).getSessionId(session_id)
        if sessionId == None:
            print "Super-Gluu-RO.verify_authentication failed. Session {%s} does not exist or has expired" % session_id
            context.setUser(None)
            return False
        
        client = CdiUtil.bean(Identity).getSessionClient().getClient()
        if not self.verify_session_ownership(sessionId,context.getUser(),client):
            print "Super-Gluu-RO. verify_authentication failed due to invalid session ownership"
            context.setUser(None)
            return False
        
        if not self.is_session_authenticated(sessionId):
            print "Super-Gluu-Ro. verify_authentication failed. Session is not authenticated"
            context.setUser(None)
            return False
        
        print "Super-Gluu-RO verify_authentication complete"
        return True
    
    def perform_preliminary_user_authentication(self, context):
        username = context.getHttpRequest().getParameter(self.usernameParamName)
        if self.authWithoutPassword:
            userService = CdiUtil.bean(UserService)
            user = userService.getUser(username,"uid")
            if user == None:
                print "Super-Gluu-RO. User '%s' not found" % username
                return False
            context.setUser(user)
            print "Super-Gluu-RO. User '%s' authenticated without password" % username
            return True
        
        password = context.getHttpRequest().getParameter(self.passwordParamName)
        authService = CdiUtil.bean(AuthenticationService)
        if authService.authenticate(username, password) == False:
            print "Super-Gluu-RO. Could not authenticate user '%s' " % username
            return False

        context.setUser(authService.getAuthenticatedUser())
        return True
    
    def new_unauthenticated_session(self,user,client):
        sessionIdService = CdiUtil.bean(SessionIdService)
        authDate = Date()
        sid_attrs = HashMap()
        sid_attrs.put(Constants.AUTHENTICATED_USER,user.getUserId())
        sid_attrs.put(self.clientIdSessionParamName,client.getClientId())
        sessionId = sessionIdService.generateUnauthenticatedSessionId(user.getDn(),authDate,SessionIdState.UNAUTHENTICATED,sid_attrs,True)
        print "Super-Gluu-RO. Generated session id. DN: '%s'" % sessionId.getDn()
        return sessionId
    
    def send_push_notification_to_user(self, sessionId,context):
        remote_ip = context.getHttpRequest().getParameter(self.remoteIpParamName)
        if remote_ip == None or (remote_ip != None and StringHelper.isEmpty(remote_ip)):
            remote_ip = self.networkApi.get_remote_ip_from_request(context.getHttpRequest())
        
        user = context.getUser()
        srbuilder = SuperGluuRequestBuilder()
        srbuilder.username = user.getUserId()
        srbuilder.app = self.applicationId
        srbuilder.issuer = self.issuerId
        srbuilder.state = sessionId.getId()
        srbuilder.requestLocation(self.networkApi.get_geolocation_data(remote_ip))
        srbuilder.req_ip = remote_ip 
        device_count = self.pushNotificationManager.sendPushNotification(user,self.applicationId,srbuilder.build())
        
        if device_count == 0:
            print "User %s has no device enrolled for Super-Gluu authentication" % user.getUserId()
            return False
        return True

    

    def is_session_authenticated(self, sessionId):
        if sessionId == None:
            return False
        
        state = sessionId.getState()
        custom_state = sessionId.getSessionAttributes().get(SessionIdService.SESSION_CUSTOM_STATE)
        if state == None:
            print "Super-Gluu-RO. Session {%s} has no state variable set" % sessionId.getId()
            return False
        
        state_unauthenticated = SessionIdState.UNAUTHENTICATED == state
        state_authenticated = SessionIdState.AUTHENTICATED == state
        custom_state_declined = StringHelper.equalsIgnoreCase("declined",custom_state) 
        custom_state_expired  = StringHelper.equalsIgnoreCase("expired",custom_state)
        custom_stated_approved = StringHelper.equalsIgnoreCase("approved",custom_state)

        if state_unauthenticated and (custom_state_declined or custom_state_expired):
            print "Super-Gluu-RO. Session {%s} isn't authenticated" % sessionId.getId()
            return False
        
        if state_authenticated or (state_unauthenticated and custom_stated_approved):
            print "Super-Gluu-RO. Session {%s} is authenticated" % sessionId.getId()
            return True

        return False
    
    # this function verifies if the session was created when invoked with the 
    # current client's credentials and with the current user's credentials

    def verify_session_ownership(self, sessionId, user, client):
        session_attributes = sessionId.getSessionAttributes()
        client_id = session_attributes.get(self.clientIdSessionParamName)
        if not StringHelper.equalsIgnoreCase(client.getClientId(),client_id):
            print "Super-Gluu-RO. Session {%s} client_id mismatch" % sessionId.getId()
            return False
        
        user_id = session_attributes.get(Constants.AUTHENTICATED_USER)
        if not StringHelper.equalsIgnoreCase(user_id,user.getUserId()):
            print "Super-Gluu-RO. Session {%s} user_id mismatch" % sessionId.getId() 
            return False
        
        return True
