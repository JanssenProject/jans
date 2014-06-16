from org.jboss.seam.contexts import Context, Contexts
from org.jboss.seam.security import Identity
from javax.faces.context import FacesContext
from org.xdi.oxauth.service.python.interfaces import ExternalAuthenticatorType
from org.xdi.oxauth.service import UserService
from org.xdi.util import StringHelper
from org.xdi.util import ArrayHelper
from org.xdi.util.security import StringEncrypter 

import java

class ExternalAuthenticator(ExternalAuthenticatorType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, configurationAttributes):
        print "Tiqr initialized successfully"

        return True

    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        return True

    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        return None

    def authenticate(self, configurationAttributes, requestParameters, step):
        credentials = Identity.instance().getCredentials()

        user_name = credentials.getUsername()
        if (step == 1):
            print "Tiqr authenticate for step 1"

            user_password = credentials.getPassword()
            logged_in = False
            if (StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password)):
                userService = UserService.instance()
                logged_in = userService.authenticate(user_name, user_password)

            if (not logged_in):
                return False

            return True
        elif (step == 2):
            print "Tiqr authenticate for step 2"

            passed_step1 = self.isPassedDefaultAuthentication
            if (not passed_step1):
                return False

            expected_user = credentials.getUser();
            if (expected_user == None):
                print "Tiqr authenticate for step 2. expected user is empty"
                return False

            expected_user_name = expected_user.getUserId();

            session = FacesContext.getCurrentInstance().getExternalContext().getSession(False)
            if (session == None):
                print "Tiqr authenticate for step 2. Session is not exist"
                return False

            authenticated_username = session.getValue("tiqr_user_uid")
            session.removeValue("tiqr_user_uid")

            print "Tiqr authenticate for step 2. authenticated_username: " + authenticated_username + ", expected user_name: " + expected_user_name

            if StringHelper.equals(expected_user_name, authenticated_username):
                return True

            return False
        else:
            return False

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        if (step == 1):
            print "Tiqr prepare for step 1"

            return True
        elif (step == 2):
            print "Tiqr prepare for step 2"

            return self.isPassedDefaultAuthentication
        else:
            return False

    def getExtraParametersForStep(self, configurationAttributes, step):
        return None

    def getCountAuthenticationSteps(self, configurationAttributes):
        return 2

    def getPageForStep(self, configurationAttributes, step):
        if (step == 2):
            return "/auth/tiqr/tiqrlogin.php"
        return ""

    def isPassedDefaultAuthentication(self):
        credentials = Identity.instance().getCredentials()
        user_name = credentials.getUsername()
        passed_step1 = StringHelper.isNotEmptyString(user_name)

        return passed_step1

    def logout(self, configurationAttributes, requestParameters):
        return True

    def getApiVersion(self):
        return 3
