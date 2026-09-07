

from io.jans.service.cdi.util import CdiUtil
from io.jans.as.server.security import Identity
from io.jans.model.custom.script.type.auth import PersonAuthenticationType
from io.jans.as.server.service import AuthenticationService
from io.jans.util import StringHelper

import java

class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Basic. Initialization"
        print "Basic. Initialized successfully"
        return True   

    def destroy(self, configurationAttributes):
        print "Basic. Destroy"
        print "Basic. Destroyed successfully"
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

        if (step == 1):
            print "Basic. Authenticate for step 1"

            identity = CdiUtil.bean(Identity)
            credentials = identity.getCredentials()
            
            user_name_array = StringHelper.split(credentials.getUsername(),"+")
            
            user_name = None
            
            if len(user_name_array) == 2:
                
                email_id_array = StringHelper.split(user_name_array[1],"@")
                user_name = user_name_array[0] + "@"+ email_id_array[1]
            else:
                
                user_name = user_name_array[0]
                
            print "Username for authentication is: %s  " % user_name
            user_password = credentials.getPassword()

            logged_in = False
            if (StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password)):
                
                logged_in = authenticationService.authenticate(user_name, user_password,"mail","mail")
                
            if (not logged_in):
                return False

            return True
        else:
            return False

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        if (step == 1):
            print "Basic. Prepare for Step 1"
            return True
        else:
            return False

	def getNextStep(self, step, context):
        return -1

    def getExtraParametersForStep(self, configurationAttributes, step):
        return None

    def getCountAuthenticationSteps(self, configurationAttributes):
        return 1

    def getPageForStep(self, configurationAttributes, step):
        return ""

    def getLogoutExternalUrl(self, configurationAttributes, requestParameters):
        print "Get external logout URL call"
        return None

    def logout(self, configurationAttributes, requestParameters):
        return True
