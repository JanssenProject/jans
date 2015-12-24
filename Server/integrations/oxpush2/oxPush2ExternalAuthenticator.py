from org.xdi.model.custom.script.type.auth import PersonAuthenticationType
from org.jboss.seam.contexts import Context, Contexts
from org.jboss.seam.security import Identity
from org.xdi.oxauth.service import UserService
from org.xdi.util import StringHelper
from org.xdi.util import ArrayHelper
from org.xdi.oxauth.service import SessionStateService
from org.xdi.oxauth.service.fido.u2f import DeviceRegistrationService
from org.xdi.oxauth.util import ServerUtil
from org.xdi.oxauth.model.config import Constants
from org.xdi.oxauth.model.config import ConfigurationFactory
from javax.ws.rs.core import Response
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

        if (step == 1):
            print "oxPush2. Authenticate for step 1"

            user_password = credentials.getPassword()
            logged_in = False
            if (StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password)):
                userService = UserService.instance()
                logged_in = userService.authenticate(user_name, user_password)

            if (not logged_in):
                return False

            return True
        elif (step == 2):
            print "oxPush2. Authenticate for step 2"
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

            auth_method = 'authenticate'
            enrollment_mode = ServerUtil.getFirstValue(requestParameters, "loginForm:registerButton")
            if StringHelper.isNotEmpty(enrollment_mode):
                auth_method = 'enroll'
                return False
            
            u2f_application_id = configurationAttributes.get("u2f_application_id").getValue2()
            if StringHelper.isEmpty(u2f_application_id):
                print "oxPush2. Failed to determine application_id. u2f_application_id parameter is empty"
                return False

            session_state = SessionStateService.instance().getSessionStateFromCookie()
            if StringHelper.isEmpty(session_state):
                print "oxPush2. Failed to determine session_state"
                return False

            print "oxPush2. Authenticate for step 2. auth_method: " + auth_method
            
            issuer = ConfigurationFactory.instance().getConfiguration().getIssuer()
            oxpush2_request = json.dumps({'session_state': session_state,
                               'auth_method': auth_method,
                               'issuer': issuer,
                               'app': u2f_application_id,
                               'user': user.getUserId()}, separators=(',',':'))

            print "oxPush2. Prepared oxpush2_request:", oxpush2_request

            context.set("oxpush2_request", oxpush2_request)

            return True
        elif (step == 3):
            print "oxPush2. Prepare for step 2"

            return True
        else:
            return False

    def getExtraParametersForStep(self, configurationAttributes, step):        
        return None

    def getCountAuthenticationSteps(self, configurationAttributes):
        return 2

    def getPageForStep(self, configurationAttributes, step):
        if (step == 2):
            return "/auth/oxpush2/login.xhtml"

        return ""

    def logout(self, configurationAttributes, requestParameters):
        return True
