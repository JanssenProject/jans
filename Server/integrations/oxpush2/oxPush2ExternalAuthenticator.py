from org.xdi.model.custom.script.type.auth import PersonAuthenticationType
from org.jboss.seam.contexts import Context, Contexts
from org.jboss.seam.security import Identity
from org.xdi.oxauth.service import UserService
from org.xdi.oxauth.service import SessionStateService
from org.xdi.oxauth.service.fido.u2f import DeviceRegistrationService
from org.xdi.util import StringHelper
from org.xdi.util import ArrayHelper
from org.xdi.oxauth.util import ServerUtil
from org.xdi.oxauth.model.config import Constants
from org.xdi.oxauth.model.config import ConfigurationFactory
from java.util import Arrays

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
        print "oxPush2. Initialization"

        self.u2f_application_id = configurationAttributes.get("u2f_application_id").getValue2()
        if StringHelper.isEmpty(self.u2f_application_id):
            print "oxPush2. Initialization. Failed to determine application_id. u2f_application_id configuration parameter is empty"
            return False

        print "oxPush2. Initialized successfully"
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

        userService = UserService.instance()
        deviceRegistrationService = DeviceRegistrationService.instance()
        if (step == 1):
            print "oxPush2. Authenticate for step 1"

            user_password = credentials.getPassword()
            logged_in = False
            if (StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password)):
                logged_in = userService.authenticate(user_name, user_password)

            if (not logged_in):
                return False

            auth_method = 'authenticate'
            enrollment_mode = ServerUtil.getFirstValue(requestParameters, "loginForm:registerButton")
            if StringHelper.isNotEmpty(enrollment_mode):
                auth_method = 'enroll'
            
            if (auth_method == 'authenticate'):
                find_user_by_uid = userService.getUser(user_name)
                if (find_user_by_uid == None):
                    print "oxPush. Authenticate for step 1. Failed to find user"
                    return False

                user_inum = userService.getUserInum(find_user_by_uid)
                u2f_devices_list = deviceRegistrationService.findUserDeviceRegistrations(user_inum, self.u2f_application_id, "oxId")
                if (u2f_devices_list.size() == 0):
                    auth_method = 'enroll'
                    print "oxPush2. There is no U2F '%s' user devices associated with application '%s'. Changing auth_method to '%s'" % (user_name, self.u2f_application_id, auth_method)

            print "oxPush2. Authenticate for step 1. auth_method: '%s'" % auth_method
            
            context.set("oxpush2_auth_method", auth_method)

            return True
        elif (step == 2):
            print "oxPush2. Authenticate for step 2"

            credentials = Identity.instance().getCredentials()
            user = credentials.getUser()
            if (user == None):
                print "oxPush2. Authenticate for step 2. Failed to determine user name"
                return False

            # Find user by uid
            userService = UserService.instance()
            find_user_by_uid = userService.getUser(user_name)
            if (find_user_by_uid == None):
                print "oxPush. Authenticate for step 2. Failed to find user"
                return False

            session_attributes = context.get("sessionAttributes")
            if (not session_attributes.containsKey("oxpush2_request")):
                print "oxPush2. Authenticate for step 2. There is no oxPush2 request in session attributes"
                return False
            
            oxpush2_request_json = session_attributes.get("oxpush2_request")
            oxpush2_request = json.loads(oxpush2_request_json)

            auth_method = oxpush2_request['method']
            if (auth_method in ['enroll', 'authenticate']):
                print "oxPush2. Authenticate for step 2. Validation U2F user device. auth_method: '%s'" % auth_method

                # Check session state extended
                if (not session_attributes.containsKey("session_custom_state")):
                    print "oxPush2. Authenticate for step 2. There is no session_custom_state in session attributes"
                    return False

                session_custom_state = session_attributes.get("session_custom_state")
                if(not StringHelper.equalsIgnoreCase("approved", session_custom_state)):
                    print "oxPush2. Authenticate for step 2. User '%s' not approve or pass U2F authentication. session_custom_state: '%s'" % (user_name, session_custom_state)
                    return False

                # Try to find device_id in session attribute
                if (not session_attributes.containsKey("oxpush2_u2f_device_id")):
                    print "oxPush2. Authenticate for step 2. There is no u2f_device associated with this request"
                    return False
                
                u2f_device_id = session_attributes.get("oxpush2_u2f_device_id")

                # Validate if user has specified device_id enrollment
                user_inum = userService.getUserInum(find_user_by_uid)

                u2f_device = deviceRegistrationService.findUserDeviceRegistration(user_inum, u2f_device_id)
                if (u2f_device == None):
                    print "oxPush2. Authenticate for step 2. There is no u2f_device '%s' associated with user '%s'" % (u2f_device_id, user_inum)
                    return False

                if (not StringHelper.equalsIgnoreCase(self.u2f_application_id, u2f_device.application)):
                    print "oxPush2. Authenticate for step 2. U2F user's '%s' device associated with other application '%s'" % (user_name, u2f_device.application)
                    return False

                print "oxPush2. Authenticate for step 2. U2F user's '%s' device authenticated successfully with U2F device '%s'" % (user_name, u2f_device_id)

                return True
            else:
                print "oxPush2. Authenticate for step 2. U2F auth_method is invalid"

            return False
        else:
            return False

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        context = Contexts.getEventContext()

        if (step == 1):
            context.set("display_register_action", True)
            return True
        elif (step == 2):
            print "oxPush2. Prepare for step 2"

            credentials = Identity.instance().getCredentials()
            user = credentials.getUser()
            if (user == None):
                print "oxPush2. Prepare for step 2. Failed to determine user name"
                return False

            session_attributes = context.get("sessionAttributes")
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
            oxpush2_request = json.dumps({'username': user.getUserId(),
                               'app': self.u2f_application_id,
                               'issuer': issuer,
                               'method': auth_method,
                               'state': session_state}, separators=(',',':'))
            print "oxPush2. Prepare for step 2. Prepared oxpush2_request:", oxpush2_request

            context.set("oxpush2_request", oxpush2_request)

            return True
        else:
            return False

    def getExtraParametersForStep(self, configurationAttributes, step):        
        if (step == 1):
            return Arrays.asList("display_register_action")
        elif (step == 2):
            return Arrays.asList("oxpush2_auth_method", "oxpush2_request")
        
        return None

    def getCountAuthenticationSteps(self, configurationAttributes):
        return 2

    def getPageForStep(self, configurationAttributes, step):
        if (step == 2):
            return "/auth/oxpush2/login.xhtml"

        return ""

    def logout(self, configurationAttributes, requestParameters):
        return True
