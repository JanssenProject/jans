from org.jboss.seam.contexts import Context, Contexts
from org.jboss.seam.security import Identity
from org.xdi.model.custom.script.type.auth import PersonAuthenticationType
from org.xdi.oxauth.service import UserService
from org.xdi.util import StringHelper
from org.xdi.util import ArrayHelper
from com.wikidsystems.client import wClient

import java

class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, configurationAttributes):
        return True   

    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        return True

    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        return None

    def authenticate(self, configurationAttributes, requestParameters, step):
        context = Contexts.getApplicationContext()

        print "Wikid authentication. Cheking client"
        wc = context.get("wClient")
        if ((wc is None) or (not wc.isConnected())):
            print "Wikid authenticate for step 1. Creating new client."
            wikid_server_host = configurationAttributes.get("wikid_server_host").getValue2()
            wikid_server_port = int(configurationAttributes.get("wikid_server_port").getValue2())
    
            wikid_cert_path = configurationAttributes.get("wikid_cert_path").getValue2()
            wikid_cert_pass = configurationAttributes.get("wikid_cert_pass").getValue2()
            wikid_ca_store_path = configurationAttributes.get("wikid_ca_store_path").getValue2()
            wikid_ca_store_pass = configurationAttributes.get("wikid_ca_store_pass").getValue2()

            wc = wClient(wikid_server_host, wikid_server_port, wikid_cert_path, wikid_cert_pass, wikid_ca_store_path, wikid_ca_store_pass)
            context.set("wClient", wc)

        if (step == 1):
            print "Wikid authenticate for step 1"

            credentials = Identity.instance().getCredentials()
            user_name = credentials.getUsername()
            user_password = credentials.getPassword()

            logged_in = False
            if (StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password)):
                userService = UserService.instance()
                logged_in = userService.authenticate(user_name, user_password)

            if (not logged_in):
                return False

            return wc.isConnected()
        elif (step == 2):
            print "Wikid authenticate for step 2"
            wc = context.get("wClient")
            if (wc is None):
                print "Wikid client is invalid"
                return False

            wikid_user_array = requestParameters.get("username")
            wikid_passcode_array = requestParameters.get("passcode")
            if ArrayHelper.isEmpty(wikid_user_array) or  ArrayHelper.isEmpty(wikid_passcode_array):
                print "Username or passcode is empty"
                return False

            wikid_user = wikid_user_array[0]
            wikid_passcode = wikid_passcode_array[0]
            wikid_server_code = configurationAttributes.get("wikid_server_code").getValue2()

            print "Wikid authenticate for step 2 wikid_user: " + wikid_user

            is_valid = wc.CheckCredentials(wikid_user, wikid_passcode, wikid_server_code);

            if is_valid:
                print "Wikid authenticate for step 2. wikid_user: " + wikid_user + " authenticated successfully"
            else:
                print "Wikid authenticate for step 2. Failed to authenticate. wikid_user: " + wikid_user

            return is_valid
        else:
            return False

    def getExtraParametersForStep(self, configurationAttributes, step):
        return None

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        if (step == 1):
            print "Wikid prepare for step 1"

            return True
        elif (step == 2):
            print "Wikid prepare for step 2"

            return True
        else:
            return False

    def getCountAuthenticationSteps(self, configurationAttributes):
        return 2

    def getPageForStep(self, configurationAttributes, step):
        if (step == 2):
            return "/auth/wikid/wikidlogin.xhtml"
        return ""

    def logout(self, configurationAttributes, requestParameters):
        return True

    def getApiVersion(self):
        return 3
