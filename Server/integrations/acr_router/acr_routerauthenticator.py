from org.xdi.model.custom.script.type.auth import PersonAuthenticationType
from org.jboss.seam.contexts import Context, Contexts
from org.jboss.seam.security import Identity
from org.xdi.oxauth.service import UserService, AuthenticationService, SessionStateService, VeriCloudCompromise
from org.xdi.util import StringHelper
from org.xdi.util import ArrayHelper
from org.xdi.oxauth.client.fido.u2f import FidoU2fClientFactory
from org.xdi.oxauth.service.fido.u2f import DeviceRegistrationService
from org.xdi.oxauth.util import ServerUtil
from org.xdi.oxauth.model.config import Constants
from org.jboss.resteasy.client import ClientResponseFailure
from org.jboss.resteasy.client.exception import ResteasyClientException
from javax.ws.rs.core import Response
from java.util import Arrays

import sys
import java

class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, configurationAttributes):
        print "acr_router Initialization"

        return True   

    def destroy(self, configurationAttributes):
        print "acr_router Destroy"
        print "acr_router Destroyed successfully"
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
        user_password = credentials.getPassword()
        context = Contexts.getEventContext()
        session_attributes = context.get("sessionAttributes")
        remote_ip = session_attributes.get("remote_ip")
        client_id = self.getClientID(session_attributes)
        sessionStateService = SessionStateService.instance()
        sessionState = sessionStateService.getSessionState()
        print "SessionState id: %s" % sessionState.getId()
        acr = sessionStateService.getAcr(sessionState)
        print "Current ACR_VALUE: " + acr
        if (step == 1):
            print "acr_router Authenticate for step 1"
            logged_in = False
            if (StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password)):
                userService = UserService.instance()
                veriCloudCompromise= VeriCloudCompromise.instance()
                find_user_by_uid = userService.getUser(user_name)
                status_attribute_value = userService.getCustomAttribute(find_user_by_uid, "mail")
                if status_attribute_value != None:
                    user_mail = status_attribute_value.getValue()
                #isCompromise = veriCloudCompromise.is_compromised("testuser123@gmail.com", "123456")
                isCompromise =  False
                if(isCompromise ):
                    sessionAttributes = sessionState.getSessionAttributes()
                    sessionAttributes.put("acr_values", "otp")
                    sessionAttributes.put("acr", "otp")
                    sessionState.setSessionAttributes(sessionAttributes)
                    sessionStateService.reinitLogin(sessionState,True)
                else: 
                    logged_in = userService.authenticate(user_name, user_password)
                
            if (not logged_in):
                return False
            return True
	elif (step == 2):
	    print "acr_router Authenticate for step 2"

	    authenticationService = AuthenticationService.instance()
	    user = authenticationService.getAuthenticatedUser()
	    if (user == None):
		print "acr_router Prepare for step 2. Failed to determine user name"
		return False

	    if (auth_method == 'authenticate'):
		print "Code here to check APIs"
		return True
	    else:
		print "acr_router. Prepare for step 2. Authenticatiod method is invalid"
		return False

	    return False
        else:
            return False

    def prepareForStep(self, configurationAttributes, requestParameters, step):
	if (step == 1):
	    return True
	elif (step == 2):
	    print "acr_router Prepare for step 2"
	    return True
	else:
	    return False

    def getExtraParametersForStep(self, configurationAttributes, step):
        return None

    def getCountAuthenticationSteps(self, configurationAttributes):
        return 1

    def getPageForStep(self, configurationAttributes, step):
       return True

    def logout(self, configurationAttributes, requestParameters):
        return True
        
    def getClientID(self, session_attributes):
        if not session_attributes.containsKey("client_id"):
            return None

        return session_attributes.get("client_id")
