# Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
# Copyright (c) 2020, Janssen Project
#
# Author: Yuriy Movchan
#

import java
import json
from java.util import Arrays, HashMap, IdentityHashMap
from io.jans.model.custom.script.type.auth import PersonAuthenticationType
from org.gluu.oxauth.client import TokenClient, TokenRequest, UserInfoClient
from org.gluu.oxauth.model.common import GrantType, AuthenticationMethod
from org.gluu.oxauth.model.common import User
from org.gluu.oxauth.model.jwt import Jwt, JwtClaimName
from io.jans.as.server.security import Identity
from io.jans.as.server.service import UserService, ClientService, AuthenticationService
from io.jans.service.cdi.util import CdiUtil
from io.jans.util import StringHelper, ArrayHelper

class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Google+ Initialization"

        if (not configurationAttributes.containsKey("gplus_client_secrets_file")):
            print "Google+ Initialization. The property gplus_client_secrets_file is empty"
            return False
            
        clientSecretsFile = configurationAttributes.get("gplus_client_secrets_file").getValue2()
        self.clientSecrets = self.loadClientSecrets(clientSecretsFile)
        if (self.clientSecrets == None):
            print "Google+ Initialization. File with Google+ client secrets should be not empty"
            return False

        self.attributesMapping = None
        if (configurationAttributes.containsKey("gplus_remote_attributes_list") and
            configurationAttributes.containsKey("gplus_local_attributes_list")):

            remoteAttributesList = configurationAttributes.get("gplus_remote_attributes_list").getValue2()
            if (StringHelper.isEmpty(remoteAttributesList)):
                print "Google+ Initialization. The property gplus_remote_attributes_list is empty"
                return False    

            localAttributesList = configurationAttributes.get("gplus_local_attributes_list").getValue2()
            if (StringHelper.isEmpty(localAttributesList)):
                print "Google+ Initialization. The property gplus_local_attributes_list is empty"
                return False

            self.attributesMapping = self.prepareAttributesMapping(remoteAttributesList, localAttributesList)
            if (self.attributesMapping == None):
                print "Google+ Initialization. The attributes mapping isn't valid"
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
                print "Google+ Initialization. Failed to load gplus_extension_module: '%s'" % extensionModuleName
                print "Google+ Initialization. Unexpected error:", ex
                return False

        print "Google+ Initialized successfully"
        return True   

    def destroy(self, authConfiguration):
        print "Google+ Destroy"
        print "Google+ Destroyed successfully"

    def getApiVersion(self):
        return 11
        
    def getAuthenticationMethodClaims(self, requestParameters):
        return None
        
    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        return True

    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        return None

    def authenticate(self, configurationAttributes, requestParameters, step):
        identity = CdiUtil.bean(Identity)
        userService = CdiUtil.bean(UserService)
        authenticationService = CdiUtil.bean(AuthenticationService)

        mapUserDeployment = False
        enrollUserDeployment = False
        if (configurationAttributes.containsKey("gplus_deployment_type")):
            deploymentType = StringHelper.toLowerCase(configurationAttributes.get("gplus_deployment_type").getValue2())
            
            if (StringHelper.equalsIgnoreCase(deploymentType, "map")):
                mapUserDeployment = True
            if (StringHelper.equalsIgnoreCase(deploymentType, "enroll")):
                enrollUserDeployment = True

        if (step == 1):
            print "Google+ Authenticate for step 1"
 
            gplusAuthCodeArray = requestParameters.get("gplus_auth_code")
            gplusAuthCode = gplusAuthCodeArray[0]

            # Check if user uses basic method to log in
            useBasicAuth = False
            if (StringHelper.isEmptyString(gplusAuthCode)):
                useBasicAuth = True

            # Use basic method to log in
            if (useBasicAuth):
                print "Google+ Authenticate for step 1. Basic authentication"
        
                identity.setWorkingParameter("gplus_count_login_steps", 1)
        
                credentials = identity.getCredentials()

                userName = credentials.getUsername()
                userPassword = credentials.getPassword()
        
                loggedIn = False
                if (StringHelper.isNotEmptyString(userName) and StringHelper.isNotEmptyString(userPassword)):
                    userService = CdiUtil.bean(UserService)
                    loggedIn = authenticationService.authenticate(userName, userPassword)
        
                if (not loggedIn):
                    return False
        
                return True

            # Use Google+ method to log in
            print "Google+ Authenticate for step 1. gplusAuthCode:", gplusAuthCode

            currentClientSecrets = self.getCurrentClientSecrets(self.clientSecrets, configurationAttributes, requestParameters)
            if (currentClientSecrets == None):
                print "Google+ Authenticate for step 1. Client secrets configuration is invalid"
                return False
            
            print "Google+ Authenticate for step 1. Attempting to gets tokens"
            tokenResponse = self.getTokensByCode(self.clientSecrets, configurationAttributes, gplusAuthCode)
            if ((tokenResponse == None) or (tokenResponse.getIdToken() == None) or (tokenResponse.getAccessToken() == None)):
                print "Google+ Authenticate for step 1. Failed to get tokens"
                return False
            else:
                print "Google+ Authenticate for step 1. Successfully gets tokens"

            jwt = Jwt.parse(tokenResponse.getIdToken())
            # TODO: Validate ID Token Signature  

            gplusUserUid = jwt.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER)
            print "Google+ Authenticate for step 1. Found Google user ID in the ID token: '%s'" % gplusUserUid
            
            if (mapUserDeployment):
                # Use mapping to local IDP user
                print "Google+ Authenticate for step 1. Attempting to find user by oxExternalUid: 'gplus:%s'" % gplusUserUid

                # Check if there is user with specified gplusUserUid
                foundUser = userService.getUserByAttribute("oxExternalUid", "gplus:" + gplusUserUid)

                if (foundUser == None):
                    print "Google+ Authenticate for step 1. Failed to find user"
                    print "Google+ Authenticate for step 1. Setting count steps to 2"
                    identity.setWorkingParameter("gplus_count_login_steps", 2)
                    identity.setWorkingParameter("gplus_user_uid", gplusUserUid)
                    return True

                foundUserName = foundUser.getUserId()
                print "Google+ Authenticate for step 1. foundUserName: '%s'" % foundUserName
                
                userAuthenticated = authenticationService.authenticate(foundUserName)
                if (userAuthenticated == False):
                    print "Google+ Authenticate for step 1. Failed to authenticate user"
                    return False
            
                print "Google+ Authenticate for step 1. Setting count steps to 1"
                identity.setWorkingParameter("gplus_count_login_steps", 1)

                postLoginResult = self.extensionPostLogin(configurationAttributes, foundUser)
                print "Google+ Authenticate for step 1. postLoginResult: '%s'" % postLoginResult

                return postLoginResult
            elif (enrollUserDeployment):
                # Use auto enrollment to local IDP
                print "Google+ Authenticate for step 1. Attempting to find user by oxExternalUid: 'gplus:%s'" % gplusUserUid
 
                # Check if there is user with specified gplusUserUid
                foundUser = userService.getUserByAttribute("oxExternalUid", "gplus:" + gplusUserUid)
 
                if (foundUser == None):
                    # Auto user enrollemnt
                    print "Google+ Authenticate for step 1. There is no user in LDAP. Adding user to local LDAP"

                    print "Google+ Authenticate for step 1. Attempting to gets user info"
                    userInfoResponse = self.getUserInfo(currentClientSecrets, configurationAttributes, tokenResponse.getAccessToken())
                    if ((userInfoResponse == None) or (userInfoResponse.getClaims().size() == 0)):
                        print "Google+ Authenticate for step 1. Failed to get user info"
                        return False
                    else:
                        print "Google+ Authenticate for step 1. Successfully gets user info"
                    
                    gplusResponseAttributes = userInfoResponse.getClaims()
 
                    # Convert Google+ user claims to lover case
                    gplusResponseNormalizedAttributes = HashMap()
                    for gplusResponseAttributeEntry in gplusResponseAttributes.entrySet():
                        gplusResponseNormalizedAttributes.put(
                            StringHelper.toLowerCase(gplusResponseAttributeEntry.getKey()), gplusResponseAttributeEntry.getValue())
 
                    currentAttributesMapping = self.getCurrentAttributesMapping(self.attributesMapping, configurationAttributes, requestParameters)
                    print "Google+ Authenticate for step 1. Using next attributes mapping '%s'" % currentAttributesMapping
 
                    newUser = User()
                    for attributesMappingEntry in currentAttributesMapping.entrySet():
                        remoteAttribute = attributesMappingEntry.getKey()
                        localAttribute = attributesMappingEntry.getValue()
 
                        localAttributeValue = gplusResponseNormalizedAttributes.get(remoteAttribute)
                        if (localAttribute != None):
                            newUser.setAttribute(localAttribute, localAttributeValue)
 
                    if (newUser.getAttribute("sn") == None):
                        newUser.setAttribute("sn", gplusUserUid)
 
                    if (newUser.getAttribute("cn") == None):
                        newUser.setAttribute("cn", gplusUserUid)

                    # Add mail to oxTrustEmail so that the user's
                    # email is available through the SCIM interface
                    # too.
                    if (newUser.getAttribute("oxTrustEmail") is None and
                        newUser.getAttribute("mail") is not None):
                        oxTrustEmail = {
                            "value": newUser.getAttribute("mail"),
                            "display": newUser.getAttribute("mail"),
                            "primary": True,
                            "operation": None,
                            "reference": None,
                            "type": "other"
                        }
                        newUser.setAttribute("oxTrustEmail", json.dumps(oxTrustEmail))

                    newUser.setAttribute("oxExternalUid", "gplus:" + gplusUserUid)
                    print "Google+ Authenticate for step 1. Attempting to add user '%s' with next attributes '%s'" % (gplusUserUid, newUser.getCustomAttributes())
 
                    foundUser = userService.addUser(newUser, True)
                    print "Google+ Authenticate for step 1. Added new user with UID: '%s'" % foundUser.getUserId()

                foundUserName = foundUser.getUserId()
                print "Google+ Authenticate for step 1. foundUserName: '%s'" % foundUserName

                userAuthenticated = authenticationService.authenticate(foundUserName)
                if (userAuthenticated == False):
                    print "Google+ Authenticate for step 1. Failed to authenticate user"
                    return False

                print "Google+ Authenticate for step 1. Setting count steps to 1"
                identity.setWorkingParameter("gplus_count_login_steps", 1)

                print "Google+ Authenticate for step 1. Attempting to run extension postLogin"
                postLoginResult = self.extensionPostLogin(configurationAttributes, foundUser)
                print "Google+ Authenticate for step 1. postLoginResult: '%s'" % postLoginResult

                return postLoginResult
            else:
                # Check if there is user with specified gplusUserUid
                print "Google+ Authenticate for step 1. Attempting to find user by uid: '%s'" % gplusUserUid

                foundUser = userService.getUser(gplusUserUid)
                if (foundUser == None):
                    print "Google+ Authenticate for step 1. Failed to find user"
                    return False

                foundUserName = foundUser.getUserId()
                print "Google+ Authenticate for step 1. foundUserName: '%s'" % foundUserName

                userAuthenticated = authenticationService.authenticate(foundUserName)
                if (userAuthenticated == False):
                    print "Google+ Authenticate for step 1. Failed to authenticate user"
                    return False

                print "Google+ Authenticate for step 1. Setting count steps to 1"
                identity.setWorkingParameter("gplus_count_login_steps", 1)

                postLoginResult = self.extensionPostLogin(configurationAttributes, foundUser)
                print "Google+ Authenticate for step 1. postLoginResult: '%s'" % postLoginResult

                return postLoginResult
        elif (step == 2):
            print "Google+ Authenticate for step 2"
            
            sessionAttributes = identity.getSessionId().getSessionAttributes()
            if (sessionAttributes == None) or not sessionAttributes.containsKey("gplus_user_uid"):
                print "Google+ Authenticate for step 2. gplus_user_uid is empty"
                return False

            gplusUserUid = sessionAttributes.get("gplus_user_uid")
            passed_step1 = StringHelper.isNotEmptyString(gplusUserUid)
            if (not passed_step1):
                return False

            identity = CdiUtil.bean(Identity)
            credentials = identity.getCredentials()

            userName = credentials.getUsername()
            userPassword = credentials.getPassword()

            loggedIn = False
            if (StringHelper.isNotEmptyString(userName) and StringHelper.isNotEmptyString(userPassword)):
                loggedIn = authenticationService.authenticate(userName, userPassword)

            if (not loggedIn):
                return False

            # Check if there is user which has gplusUserUid
            # Avoid mapping Google account to more than one IDP account
            foundUser = userService.getUserByAttribute("oxExternalUid", "gplus:" + gplusUserUid)

            if (foundUser == None):
                # Add gplusUserUid to user one id UIDs
                foundUser = userService.addUserAttribute(userName, "oxExternalUid", "gplus:" + gplusUserUid)
                if (foundUser == None):
                    print "Google+ Authenticate for step 2. Failed to update current user"
                    return False

                postLoginResult = self.extensionPostLogin(configurationAttributes, foundUser)
                print "Google+ Authenticate for step 2. postLoginResult: '%s'" % postLoginResult

                return postLoginResult
            else:
                foundUserName = foundUser.getUserId()
                print "Google+ Authenticate for step 2. foundUserName: '%s'" % foundUserName
    
                if StringHelper.equals(userName, foundUserName):
                    postLoginResult = self.extensionPostLogin(configurationAttributes, foundUser)
                    print "Google+ Authenticate for step 2. postLoginResult: '%s'" % postLoginResult
    
                    return postLoginResult
        
            return False
        else:
            return False

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        identity = CdiUtil.bean(Identity)
        authenticationService = CdiUtil.bean(AuthenticationService)

        if (step == 1):
            print "Google+ Prepare for step 1"
            
            currentClientSecrets = self.getCurrentClientSecrets(self.clientSecrets, configurationAttributes, requestParameters)
            if (currentClientSecrets == None):
                print "Google+ Prepare for step 1. Google+ client configuration is invalid"
                return False
            
            identity.setWorkingParameter("gplus_client_id", currentClientSecrets["web"]["client_id"])
            identity.setWorkingParameter("gplus_client_secret", currentClientSecrets["web"]["client_secret"])

            return True
        elif (step == 2):
            print "Google+ Prepare for step 2"

            return True
        else:
            return False

    def getExtraParametersForStep(self, configurationAttributes, step):
        if (step == 2):
            return Arrays.asList("gplus_user_uid")

        return None

    def getCountAuthenticationSteps(self, configurationAttributes):
        identity = CdiUtil.bean(Identity)
        if (identity.isSetWorkingParameter("gplus_count_login_steps")):
            return identity.getWorkingParameter("gplus_count_login_steps")
        
        return 2

    def getPageForStep(self, configurationAttributes, step):
        if (step == 1):
            return "/auth/gplus/gpluslogin.xhtml"

        return "/auth/gplus/gpluspostlogin.xhtml"

    def getNextStep(self, configurationAttributes, requestParameters, step):
        return -1

    def getLogoutExternalUrl(self, configurationAttributes, requestParameters):
        print "Get external logout URL call"
        return None

    def logout(self, configurationAttributes, requestParameters):
        # TODO Revoke token
        return True

    def loadClientSecrets(self, clientSecretsFile):
        clientSecrets = None

        # Load certificate from file
        f = open(clientSecretsFile, 'r')
        try:
            clientSecrets = json.loads(f.read())
        except:
            print "Failed to load Google+ client secrets from file: '%s'" % clientSecrets
            return None
        finally:
            f.close()
        
        return clientSecrets

    def getClientConfiguration(self, configurationAttributes, requestParameters):
        # Get client configuration
        if (configurationAttributes.containsKey("gplus_client_configuration_attribute")):
            clientConfigurationAttribute = configurationAttributes.get("gplus_client_configuration_attribute").getValue2()
            print "Google+ GetClientConfiguration. Using client attribute: '%s'" % clientConfigurationAttribute

            if (requestParameters == None):
                return None

            clientId = None
            
            # Attempt to determine client_id from request
            clientIdArray = requestParameters.get("client_id")
            if (ArrayHelper.isNotEmpty(clientIdArray) and StringHelper.isNotEmptyString(clientIdArray[0])):
                clientId = clientIdArray[0]

            # Attempt to determine client_id from event context
            if (clientId == None):
                identity = CdiUtil.bean(Identity)
                if (identity.isSetWorkingParameter("sessionAttributes")):
                    clientId = identity.getSessionId().getSessionAttributes().get("client_id")

            if (clientId == None):
                print "Google+ GetClientConfiguration. client_id is empty"
                return None

            clientService = CdiUtil.bean(ClientService)
            client = clientService.getClient(clientId)
            if (client == None):
                print "Google+ GetClientConfiguration. Failed to find client '%s' in local LDAP" % clientId
                return None

            clientConfiguration = clientService.getCustomAttribute(client, clientConfigurationAttribute)
            if ((clientConfiguration == None) or StringHelper.isEmpty(clientConfiguration.getValue())):
                print "Google+ GetClientConfiguration. Client '%s' attribute '%s' is empty" % (clientId, clientConfigurationAttribute)
            else:
                print "Google+ GetClientConfiguration. Client '%s' attribute '%s' is '%s'" % (clientId, clientConfigurationAttribute, clientConfiguration)
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
                print "Google+ PostLogin result: '%s'" % postLoginResult

                return postLoginResult
            except Exception, ex:
                print "Google+ PostLogin. Failed to execute postLogin method"
                print "Google+ PostLogin. Unexpected error:", ex
                return False
            except java.lang.Throwable, ex:
                print "Google+ PostLogin. Failed to execute postLogin method"
                ex.printStackTrace() 
                return False
                    
        return True
