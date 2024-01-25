from io.jans.jsf2.message import FacesMessages
from io.jans.as.server.security import Identity
from io.jans.as.server.service import AuthenticationService
from io.jans.model.custom.script.type.auth import PersonAuthenticationType
from io.jans.service.cdi.util import CdiUtil
from io.jans.util import StringHelper

from jakarta.faces.application import FacesMessage

import sys

class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Sample cred. Initialized"
        return True

    def destroy(self, configurationAttributes):
        print "Sample cred. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 11

    def getAuthenticationMethodClaims(self, configurationAttributes):
        return None

    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        return True

    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        return None

    def authenticate(self, configurationAttributes, requestParameters, step):

        print "Sample cred. Authenticate for Step %s" % str(step)
        identity = CdiUtil.bean(Identity)
        authenticationService = CdiUtil.bean(AuthenticationService)
        user = authenticationService.getAuthenticatedUser()

        if step == 1:

            if user == None:
                credentials = identity.getCredentials()
                user_name = credentials.getUsername()
                user_password = credentials.getPassword()

                if StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password):
                    authenticationService.authenticate(user_name, user_password)
                    user = authenticationService.getAuthenticatedUser()

            if user == None:
                return False

            #Additional authn logic for step 1 must go here
        else:
            if user == None:
                return False

            #Additional authn logic for step 2 must go here

            #facesMessages = CdiUtil.bean(FacesMessages)
            #facesMessages.setKeepMessages()
            #facesMessages.clear()
            #facesMessages.add(FacesMessage.SEVERITY_ERROR, "Wrong code entered")
            
        return False


    def prepareForStep(self, configurationAttributes, requestParameters, step):
        print "Sample cred. Prepare for Step %s" % str(step)
        return True

    def getExtraParametersForStep(self, configurationAttributes, step):
        return None

    def getCountAuthenticationSteps(self, configurationAttributes):
        print "Sample cred. getCountAuthenticationSteps called"
        return 2

    def getPageForStep(self, configurationAttributes, step):
        print "Sample cred. getPageForStep called %s" % step
        if step == 2:
            # you are supposed to build this page and place it in an accessible location 
            return  "/casa/sample_cred/page.xhtml"
        return ""

    def logout(self, configurationAttributes, requestParameters):
        return True

    def hasEnrollments(self, configurationAttributes, user):
        # whether user has one or more credentials enrolled for this type of credential 
        return True

