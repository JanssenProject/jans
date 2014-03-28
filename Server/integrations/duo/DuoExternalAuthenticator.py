from org.jboss.seam.contexts import Context, Contexts
from org.jboss.seam.security import Identity
from org.xdi.oxauth.service.python.interfaces import ExternalAuthenticatorType
from org.xdi.oxauth.service import UserService
from org.xdi.util import StringHelper
from org.xdi.util import ArrayHelper

import java
import duo_web

class ExternalAuthenticator(ExternalAuthenticatorType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, configurationAttributes):
        print "DUO initialization"
        print "DUO initialized successfully"
        return True   

    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        return True

    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        return None

    def authenticate(self, configurationAttributes, requestParameters, step):
        duo_host = configurationAttributes.get("duo_host").getValue2()
        ikey = configurationAttributes.get("duo_ikey").getValue2()
        skey = configurationAttributes.get("duo_skey").getValue2()
        akey = configurationAttributes.get("duo_akey").getValue2()

        credentials = Identity.instance().getCredentials()
        user_name = credentials.getUsername()

        if (step == 1):
            print "DUO authenticate for step 1"

            user_password = credentials.getPassword()
            logged_in = False
            if (StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password)):
                userService = UserService.instance()
                logged_in = userService.authenticate(user_name, user_password)

            if (not logged_in):
                return False

            return True
        elif (step == 2):
            print "DUO authenticate for step 2"

            passed_step1 = self.isPassedStep1
            if (not passed_step1):
                return False

            sig_response_array = requestParameters.get("sig_response")
            if ArrayHelper.isEmpty(sig_response_array):
                print "DUO authenticate for step 2. sig_response is empty"
                return False

            duo_sig_response = sig_response_array[0]

            print "DUO authenticate for step 2. duo_sig_response: " + duo_sig_response

            authenticated_username = duo_web.verify_response(ikey, skey, akey, duo_sig_response)

            print "DUO authenticate for step 2. authenticated_username: " + authenticated_username + ", expected user_name: " + user_name

            if StringHelper.equals(user_name, authenticated_username):
                return True

            return False
        else:
            return False

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        context = Contexts.getEventContext()

        duo_host = configurationAttributes.get("duo_host").getValue2()
        ikey = configurationAttributes.get("duo_ikey").getValue2()
        skey = configurationAttributes.get("duo_skey").getValue2()
        akey = configurationAttributes.get("duo_akey").getValue2()

        credentials = Identity.instance().getCredentials()
        user_name = credentials.getUsername()

        if (step == 1):
            print "DUO prepare for step 1"

            return True
        elif (step == 2):
            print "DUO prepare for step 2"

            passed_step1 = self.isPassedStep1
            if (not passed_step1):
                return False

            duo_sig_request = duo_web.sign_request(ikey, skey, akey, user_name)
            print "DUO prepare for step 2. duo_sig_request: " + duo_sig_request
            
            context.set("duo_host", duo_host)
            context.set("duo_sig_request", duo_sig_request)

            return True
        else:
            return False

    def getExtraParametersForStep(self, configurationAttributes, step):
        return None

    def getCountAuthenticationSteps(self, configurationAttributes):
        return 2

    def getPageForStep(self, configurationAttributes, step):
        if (step == 2):
            return "/login/duo/duologin.xhtml"
        return ""

    def isPassedStep1():
        credentials = Identity.instance().getCredentials()
        user_name = credentials.getUsername()
        passed_step1 = StringHelper.isNotEmptyString(user_name)

        return passed_step1

    def logout(self, configurationAttributes, requestParameters):
        return True

    def getApiVersion(self):
        return 3
