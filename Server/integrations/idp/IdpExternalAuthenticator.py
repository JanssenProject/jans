from org.jboss.seam.contexts import Context, Contexts
from org.jboss.seam.security import Identity
from org.xdi.model.custom.script.type.auth import PersonAuthenticationType
from org.xdi.oxauth.service import UserService
from org.xdi.oxauth.service import ClientService
from org.xdi.service.sso import ShibbolethLoginService
from org.xdi.util import StringHelper
from org.xdi.util import ArrayHelper
from org.xdi.util.security import StringEncrypter 
from java.lang import String 

import java

class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis
        self.sb = ShibbolethLoginService.instance()
        self.sb.setDebug(False)

    def init(self, configurationAttributes):
        print "Shibboleth initialization"

        idp_cert_store_type = configurationAttributes.get("idp_cert_store_type").getValue2()
        idp_cert_path = configurationAttributes.get("idp_cert_path").getValue2()
        idp_creds_file = configurationAttributes.get("idp_creds_file").getValue2()

        # Load credentials from file
        f = open(idp_creds_file, 'r')
        try:
            creds = json.loads(f.read())
        except:
            return False
        finally:
            f.close()

        idp_cert_password = creds["CERT_PASSWORD"]
        try:
            stringEncrypter = StringEncrypter.defaultInstance()
            idp_cert_password = stringEncrypter.decrypt(idp_cert_password)
        except:
            return False

        self.sb.initialize(idp_cert_store_type, idp_cert_path, idp_cert_password)
        print "Shibboleth initialized successfully"

        return True   

    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        return True

    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        return None

    def authenticate(self, configurationAttributes, requestParameters, step):
        if (step == 1):
            print "Shibboleth authenticate for step 1"

            idp_idp_base_uri = configurationAttributes.get("idp_idp_base_uri").getValue2()
            idp_protected_resource_uri = configurationAttributes.get("idp_protected_resource_uri").getValue2()

            credentials = Identity.instance().getCredentials()
            user_name = credentials.getUsername()
            user_password = credentials.getPassword()

            logged_in = False
            if (StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password)):
                logged_in = self.sb.authenticate(idp_idp_base_uri, idp_protected_resource_uri, user_name, user_password)

            if (not logged_in):
                print "Shibboleth authenticate for step 1. Failed to authenticate user/client"
                return False
            
            if (String(user_name).startsWith("@!")):
                clientService = ClientService.instance()
                client = clientService.getClient(user_name)
                if (client == None):
                    print "Shibboleth authenticate for step 1. Failed to find client in local LDAP"
                    return False
                #TODO: Add client
            else:
                userService = UserService.instance()
                user = userService.getUser(user_name)
                if (user == None):
                    print "Shibboleth authenticate for step 1. Adding new user in local LDAP"
                    user = userService.addDefaultUser(user_name);
    
                credentials.setUser(user);

            return True
        else:
            return False

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        if (step == 1):
            print "Basic prepare for Step 1"
            return True
        else:
            return False

    def getExtraParametersForStep(self, configurationAttributes, step):
        return None

    def getCountAuthenticationSteps(self, configurationAttributes):
        return 1

    def getPageForStep(self, configurationAttributes, step):
        return ""

    def logout(self, configurationAttributes, requestParameters):
        return True

    def getApiVersion(self):
        return 3
