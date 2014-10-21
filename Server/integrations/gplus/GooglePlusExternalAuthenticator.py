from org.jboss.seam.contexts import Context, Contexts
from org.jboss.seam.security import Identity
from javax.faces.context import FacesContext
from org.xdi.oxauth.service.python.interfaces import ExternalAuthenticatorType
from org.xdi.oxauth.service import UserService, ClientService, AuthenticationService
from org.xdi.util import StringHelper, ArrayHelper
from java.util import Arrays, ArrayList, HashMap, IdentityHashMap
from org.xdi.oxauth.model.common import User, CustomAttribute
from org.xdi.oxauth.client import TokenClient, TokenRequest, UserInfoClient
from org.xdi.oxauth.model.common import GrantType, AuthenticationMethod
from org.xdi.oxauth.model.jwt import Jwt, JwtClaimName
from org.xdi.oxauth.service import EncryptionService

import java

try:
    import json
except ImportError:
    import simplejson as json

class ExternalAuthenticator(ExternalAuthenticatorType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis
#+
    def init(self, configurationAttributes):
        print "Google+ initialization"

        if (not configurationAttributes.containsKey("gplus_client_secrets_file")):
            print "Google+ initialization. The property gplus_client_secrets_file is empty"
            return False
            
        clientSecretsFile = configurationAttributes.get("gplus_client_secrets_file").getValue2()
        self.clientSecrets = self.loadClientSecrets(clientSecretsFile)
        if (self.clientSecrets == None):
            print "Google+ initialization. File with Google+ client secrets should be not empty"
            return False

        self.attributesMapping = None
        if (configurationAttributes.containsKey("gplus_remote_attributes_list") and
            configurationAttributes.containsKey("gplus_local_attributes_list")):

            remoteAttributesList = configurationAttributes.get("gplus_remote_attributes_list").getValue2()
            if (StringHelper.isEmpty(remoteAttributesList)):
                print "Google+ initialization. The property gplus_remote_attributes_list is empty"
                return False    

            localAttributesList = configurationAttributes.get("gplus_local_attributes_list").getValue2()
            if (StringHelper.isEmpty(localAttributesList)):
                print "Google+ initialization. The property gplus_local_attributes_list is empty"
                return False

            self.attributesMapping = self.prepareAttributesMapping(remoteAttributesList, localAttributesList)
            if (self.attributesMapping == None):
                print "Google+ initialization. The attributes mapping isn't valid"
                return False

        self.extensionModule = None
        if (configurationAttributes.containsKey("extension_module")):
            extensionModuleName = configurationAttributes.get("extension_module").getValue2()
            try:
                self.extensionModule = __import__(extensionModuleName)
                extensionModuleInitResult = self.extensionModule.init(configurationAttributes)
                if (not extensionModuleInitResult):
                    return False
            except ImportError, ex:
                print "Failed to load gplus_extension_module:", extensionModuleName
                print "Unexpected error:", ex
                return False

        print "Google+ initialized successfully"
        return True   

    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        return True

    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        return None

    def authenticate(self, configurationAttributes, requestParameters, step):
        context = Contexts.getEventContext()
        authenticationService = AuthenticationService.instance()
        userService = UserService.instance()

        encryptionService = EncryptionService.instance()

        mapUserDeployment = False
        enrollUserDeployment = False
        if (configurationAttributes.containsKey("gplus_deployment_type")):
            deploymentType = StringHelper.toLowerCase(configurationAttributes.get("gplus_deployment_type").getValue2())
            
            if (StringHelper.equalsIgnoreCase(deploymentType, "map")):
                mapUserDeployment = True
            if (StringHelper.equalsIgnoreCase(deploymentType, "enroll")):
                enrollUserDeployment = True

        if (step == 1):
            print "Google+ authenticate for step 1"
 
            gplusAuthCodeArray = requestParameters.get("gplus_auth_code")
            gplusAuthCode = gplusAuthCodeArray[0]

            # Check if user uses basic method to log in
            useBasicAuth = False
            if (StringHelper.isEmptyString(gplusAuthCode)):
                useBasicAuth = True

            # Use basic method to log in
            if (useBasicAuth):
                print "Google+ authenticate for step 1. Basic authentication"
        
                context.set("gplus_count_login_steps", 1)
        
                credentials = Identity.instance().getCredentials()
                userName = credentials.getUsername()
                userPassword = credentials.getPassword()
        
                loggedIn = False
                if (StringHelper.isNotEmptyString(userName) and StringHelper.isNotEmptyString(userPassword)):
                    userService = UserService.instance()
                    loggedIn = userService.authenticate(userName, userPassword)
        
                if (not loggedIn):
                    return False
        
                return True

            # Use Google+ method to log in
            print "Google+ authenticate for step 1. gplusAuthCode:", gplusAuthCode

            currentClientSecrets = self.getCurrentClientSecrets(self.clientSecrets, configurationAttributes, requestParameters)
            if (currentClientSecrets == None):
                print "Google+ authenticate for step 1. Client secrets configuration is invalid"
                return False
            
            print "Google+ authenticate for step 1. Attempting to gets tokens"
            tokenResponse = self.getTokensByCode(self.clientSecrets, configurationAttributes, gplusAuthCode);
            if ((tokenResponse == None) or (tokenResponse.getIdToken() == None) or (tokenResponse.getAccessToken() == None)):
                print "Google+ authenticate for step 1. Failed to get tokens"
                return False
            else:
                print "Google+ authenticate for step 1. Successfully gets tokens"

            jwt = Jwt.parse(tokenResponse.getIdToken())
            # TODO: Validate ID Token Signature  

            gplusUserUid = jwt.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER);
            print "Google+ authenticate for step 1. Found Google user ID in the ID token: ", gplusUserUid
            
            if (mapUserDeployment):
                # Use mapping to local IDP user
                print "Google+ authenticate for step 1. Attempting to find user by oxExternalUid: gplus:", gplusUserUid

                # Check if there is user with specified gplusUserUid
                foundUser = userService.getUserByAttribute("oxExternalUid", "gplus:" + gplusUserUid)

                if (foundUser == None):
                    print "Google+ authenticate for step 1. Failed to find user"
                    print "Google+ authenticate for step 1. Setting count steps to 2"
                    context.set("gplus_count_login_steps", 2)
                    context.set("gplus_user_uid", encryptionService.encrypt(gplusUserUid))
                    return True

                foundUserName = foundUser.getUserId()
                print "Google+ authenticate for step 1. foundUserName:", foundUserName
                
                userAuthenticated = authenticationService.authenticate(foundUserName)
                if (userAuthenticated == False):
                    print "Google+ authenticate for step 1. Failed to authenticate user"
                    return False
            
                print "Google+ authenticate for step 1. Setting count steps to 1"
                context.set("gplus_count_login_steps", 1)

                postLoginResult = self.extensionPostLogin(configurationAttributes, foundUser)
                print "Google+ authenticate for step 1. postLoginResult:", postLoginResult

                return postLoginResult
            elif (enrollUserDeployment):
                # Use auto enrollment to local IDP
                print "Google+ authenticate for step 1. Attempting to find user by oxExternalUid: gplus:", gplusUserUid
 
                # Check if there is user with specified gplusUserUid
                foundUser = userService.getUserByAttribute("oxExternalUid", "gplus:" + gplusUserUid)
 
                if (foundUser == None):
                    # Auto user enrollemnt
                    print "Google+ authenticate for step 1. There is no user in LDAP. Adding user to local LDAP"

                    print "Google+ authenticate for step 1. Attempting to gets user info"
                    userInfoResponse = self.getUserInfo(currentClientSecrets, configurationAttributes, tokenResponse.getAccessToken())
                    if ((userInfoResponse == None) or (userInfoResponse.getClaims().size() == 0)):
                        print "Google+ authenticate for step 1. Failed to get user info"
                        return False
                    else:
                        print "Google+ authenticate for step 1. Successfully gets user info"
                    
                    gplusResponseAttributes = userInfoResponse.getClaims()
 
                    # Convert Google+ user claims to lover case
                    gplusResponseNormalizedAttributes = HashMap()
                    for gplusResponseAttributeEntry in gplusResponseAttributes.entrySet():
                        gplusResponseNormalizedAttributes.put(
                            StringHelper.toLowerCase(gplusResponseAttributeEntry.getKey()), gplusResponseAttributeEntry.getValue())
 
                    currentAttributesMapping = self.getCurrentAttributesMapping(self.attributesMapping, configurationAttributes, requestParameters)
                    print "Google+ authenticate for step 1. Using next attributes mapping", currentAttributesMapping
 
                    newUser = User()
                    for attributesMappingEntry in currentAttributesMapping.entrySet():
                        idpAttribute = attributesMappingEntry.getKey()
                        localAttribute = attributesMappingEntry.getValue()
 
                        localAttributeValue = gplusResponseNormalizedAttributes.get(idpAttribute)
                        if (localAttribute != None):
                            newUser.setAttribute(localAttribute, localAttributeValue)
 
                    if (newUser.getAttribute("sn") == None):
                        newUser.setAttribute("sn", gplusUserUid)
 
                    if (newUser.getAttribute("cn") == None):
                        newUser.setAttribute("cn", gplusUserUid)

                    newUser.setAttribute("oxExternalUid", "gplus:" + gplusUserUid)
                    print "Google+ authenticate for step 1. Attempting to add user", gplusUserUid, " with next attributes", newUser.getCustomAttributes()
 
                    foundUser = userService.addUser(newUser)
                    print "Google+ authenticate for step 1. Added new user with UID", foundUser.getUserId()

                foundUserName = foundUser.getUserId()
                print "Google+ authenticate for step 1. foundUserName:", foundUserName

                userAuthenticated = authenticationService.authenticate(foundUserName)
                if (userAuthenticated == False):
                    print "Google+ authenticate for step 1. Failed to authenticate user"
                    return False

                print "Google+ authenticate for step 1. Setting count steps to 1"
                context.set("gplus_count_login_steps", 1)

                postLoginResult = self.extensionPostLogin(configurationAttributes, foundUser)
                print "Google+ authenticate for step 1. postLoginResult:", postLoginResult

                return postLoginResult
            else:
                # Check if the is user with specified gplusUserUid
                print "Google+ authenticate for step 1. Attempting to find user by uid:", gplusUserUid

                foundUser = userService.getUser(gplusUserUid)
                if (foundUser == None):
                    print "Google+ authenticate for step 1. Failed to find user"
                    return False

                foundUserName = foundUser.getUserId()
                print "Google+ authenticate for step 1. foundUserName:", foundUserName

                userAuthenticated = authenticationService.authenticate(foundUserName)
                if (userAuthenticated == False):
                    print "Google+ authenticate for step 1. Failed to authenticate user"
                    return False

                print "Google+ authenticate for step 1. Setting count steps to 1"
                context.set("gplus_count_login_steps", 1)

                postLoginResult = self.extensionPostLogin(configurationAttributes, foundUser)
                print "Google+ authenticate for step 1. postLoginResult:", postLoginResult

                return postLoginResult
        elif (step == 2):
            print "Google+ authenticate for step 2"
            
            gplusUserUidArray = requestParameters.get("gplus_user_uid")
            if ArrayHelper.isEmpty(gplusUserUidArray):
                print "Google+ authenticate for step 2. gplus_user_uid is empty"
                return False

            gplusUserUid = encryptionService.decrypt(gplusUserUidArray[0])
            passedStep1 = StringHelper.isNotEmptyString(gplusUserUid)
            if (not passedStep1):
                return False

            credentials = Identity.instance().getCredentials()
            userName = credentials.getUsername()
            userPassword = credentials.getPassword()

            loggedIn = False
            if (StringHelper.isNotEmptyString(userName) and StringHelper.isNotEmptyString(userPassword)):
                loggedIn = userService.authenticate(userName, userPassword)

            if (not loggedIn):
                return False

            # Check if there is user which has gplusUserUid
            # Avoid mapping Google account to more than one IDP account
            foundUser = userService.getUserByAttribute("oxExternalUid", "gplus:" + gplusUserUid)

            if (foundUser == None):
                # Add gplusUserUid to user one id UIDs
                foundUser = userService.addUserAttribute(userName, "oxExternalUid", "gplus:" + gplusUserUid)
                if (foundUser == None):
                    print "Google+ authenticate for step 2. Failed to update current user"
                    return False

                postLoginResult = self.extensionPostLogin(configurationAttributes, foundUser)
                print "Google+ authenticate for step 2. postLoginResult:", postLoginResult

                return postLoginResult
            else:
                foundUserName = foundUser.getUserId()
                print "Google+ authenticate for step 2. foundUserName:", foundUserName
    
                if StringHelper.equals(userName, foundUserName):
                    postLoginResult = self.extensionPostLogin(configurationAttributes, foundUser)
                    print "Google+ authenticate for step 2. postLoginResult:", postLoginResult
    
                    return postLoginResult
        
            return False
        else:
            return False

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        context = Contexts.getEventContext()
        authenticationService = AuthenticationService.instance()

        if (step == 1):
            print "Google+ prepare for step 1"
            
            currentClientSecrets = self.getCurrentClientSecrets(self.clientSecrets, configurationAttributes, requestParameters)
            if (currentClientSecrets == None):
                print "Google+ prepare for step 1. Google+ client configuration is invalid"
                return False
            
            context.set("gplus_client_id", currentClientSecrets["web"]["client_id"])
            context.set("gplus_client_secret", currentClientSecrets["web"]["client_secret"])

            return True
        elif (step == 2):
            print "Google+ prepare for step 2"

            return True
        else:
            return False

    def getExtraParametersForStep(self, configurationAttributes, step):
        if (step == 2):
            return Arrays.asList("gplus_user_uid")

        return None

    def getCountAuthenticationSteps(self, configurationAttributes):
        context = Contexts.getEventContext()
        if (context.isSet("gplus_count_login_steps")):
            return context.get("gplus_count_login_steps")
        
        return 2

    def getPageForStep(self, configurationAttributes, step):
        if (step == 1):
            return "/auth/gplus/gpluslogin.xhtml"

        return "/auth/gplus/gpluspostlogin.xhtml"

    def logout(self, configurationAttributes, requestParameters):
        # TODO Revoke token
        return True

    def destroy(self, authConfiguration):
        print "Google+ destroy"

    def getApiVersion(self):
        return 4

    def isPassedStep1():
        credentials = Identity.instance().getCredentials()
        userName = credentials.getUsername()
        passedStep1 = StringHelper.isNotEmptyString(userName)

        return passedStep1

    def loadClientSecrets(self, clientSecretsFile):
        clientSecrets = None

        # Load certificate from file
        f = open(clientSecretsFile, 'r')
        try:
            clientSecrets = json.loads(f.read())
        except:
            print "Failed to load Google+ client secrets from file:", clientSecrets
            return None
        finally:
            f.close()
        
        return clientSecrets

    def getClientConfiguration(self, configurationAttributes, requestParameters):
        # Get client configuration
        if (configurationAttributes.containsKey("gplus_client_configuration_attribute")):
            clientConfigurationAttribute = configurationAttributes.get("gplus_client_configuration_attribute").getValue2()
            print "Google+ GetClientConfiguration. Using client attribute:", clientConfigurationAttribute

            if (requestParameters == None):
                return None

            clientId = None
            
            # Attempt to determine client_id from request
            clientIdArray = requestParameters.get("client_id")
            if (ArrayHelper.isNotEmpty(clientIdArray) and StringHelper.isNotEmptyString(clientIdArray[0])):
                clientId = clientIdArray[0]

            # Attempt to determine client_id from event context
            if (clientId == None):
                eventContext = Contexts.getEventContext()
                if (eventContext.isSet("stored_request_parameters")):
                    clientId = eventContext.get("stored_request_parameters").get("client_id")

            if (clientId == None):
                print "Google+ GetClientConfiguration. client_id is empty"
                return None

            clientService = ClientService.instance()
            client = clientService.getClient(clientId)
            if (client == None):
                print "Google+ GetClientConfiguration. Failed to find client", clientId, " in local LDAP"
                return None

            clientConfiguration = clientService.getCustomAttribute(client, clientConfigurationAttribute)
            if ((clientConfiguration == None) or StringHelper.isEmpty(clientConfiguration.getValue())):
                print "Google+ GetClientConfiguration. Client", clientId, " attribute", clientConfigurationAttribute, " is empty"
            else:
                print "Google+ GetClientConfiguration. Client", clientId, " attribute", clientConfigurationAttribute, " is", clientConfiguration
                return clientConfiguration

        return None

    def getCurrentClientSecrets(self, currentClientSecrets, configurationAttributes, requestParameters):
        clientConfiguration = self.getClientConfiguration(configurationAttributes, requestParameters)
        if (clientConfiguration == None):
            return currentClientSecrets
        
        clientConfigurationValue = json.loads(clientConfiguration.getValue())

        return clientConfigurationValue["gplus"]

    def getCurrentAttributesMapping(self, currentAttributesMapping, configurationAttributes, requestParameters):
        clientConfiguration = self.getClientConfiguration(configurationAttributes, requestParameters)
        if (clientConfiguration == None):
            return currentAttributesMapping

        clientConfigurationValue = json.loads(clientConfiguration.getValue())

        clientAttributesMapping = self.prepareAttributesMapping(clientConfigurationValue["gplus_remote_attributes_list"], clientConfigurationValue["gplus_local_attributes_list"])
        if (clientAttributesMapping == None):
            print "Google+ GetCurrentAttributesMapping. Client attributes mapping is invalid. Using default one"
            return currentAttributesMapping

        return clientAttributesMapping

    def prepareAttributesMapping(self, remoteAttributesList, localAttributesList):
        remoteAttributesListArray = StringHelper.split(remoteAttributesList, ",")
        if (ArrayHelper.isEmpty(remoteAttributesListArray)):
            print "Google+ PrepareAttributesMapping. There is no attributes specified in remoteAttributesList property"
            return None
        
        localAttributesListArray = StringHelper.split(localAttributesList, ",")
        if (ArrayHelper.isEmpty(localAttributesListArray)):
            print "Google+ PrepareAttributesMapping. There is no attributes specified in localAttributesList property"
            return None

        if (len(remoteAttributesListArray) != len(localAttributesListArray)):
            print "Google+ PrepareAttributesMapping. The number of attributes in remoteAttributesList and localAttributesList isn't equal"
            return None
        
        attributeMapping = IdentityHashMap()
        containsUid = False
        i = 0
        count = len(remoteAttributesListArray)
        while (i < count):
            remoteAttribute = StringHelper.toLowerCase(remoteAttributesListArray[i])
            localAttribute = StringHelper.toLowerCase(localAttributesListArray[i])
            attributeMapping.put(remoteAttribute, localAttribute)

            if (StringHelper.equalsIgnoreCase(localAttribute, "uid")):
                containsUid = True

            i = i + 1

        if (not containsUid):
            print "Google+ PrepareAttributesMapping. There is no mapping to mandatory 'uid' attribute"
            return None
        
        return attributeMapping

    def getTokensByCode(self, currentClientSecrets, configurationAttributes, code):
        tokenRequest = TokenRequest(GrantType.CLIENT_CREDENTIALS)
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_POST)
        tokenRequest.setCode(code)
        tokenRequest.setAuthUsername(currentClientSecrets["web"]["client_id"])
        tokenRequest.setAuthPassword(currentClientSecrets["web"]["client_secret"])
        tokenRequest.setRedirectUri("postmessage")
        tokenRequest.setGrantType(GrantType.AUTHORIZATION_CODE)

        tokenClient = TokenClient(currentClientSecrets["web"]["token_uri"])
        tokenClient.setRequest(tokenRequest)
        
        tokenResponse = tokenClient.exec()
        if ((tokenResponse == None) or (tokenResponse.getStatus() != 200)):
            return None

        return tokenResponse

    def getUserInfo(self, currentClientSecrets, configurationAttributes, accessToken):
        userInfoClient = UserInfoClient("https://www.googleapis.com/plus/v1/people/me/openIdConnect")

        userInfoResponse = userInfoClient.execUserInfo(accessToken)
        if ((userInfoResponse == None) or (userInfoResponse.getStatus() != 200)):
            return None

        return userInfoResponse

    def extensionPostLogin(self, configurationAttributes, user):
        if (self.extensionModule != None):
            try:
                postLoginResult = self.extensionModule.postLogin(configurationAttributes, user)
                print "Google+ post login result:", postLoginResult

                return postLoginResult
            except Exception, ex:
                print "Failed to execute postLogin method"
                print "Unexpected error:", ex
                return False
            except java.lang.Throwable, ex:
                print "Failed to execute postLogin method"
                ex.printStackTrace() 
                return False
                    
        return True
