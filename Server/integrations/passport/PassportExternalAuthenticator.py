# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2016, Gluu
#
# Author: Arvind Tomar
# Author: Yuriy Movchan
#

from org.xdi.service.cdi.util import CdiUtil
from javax.faces.context import FacesContext
from javax.faces.application import FacesMessage
from org.gluu.jsf2.message import FacesMessages
from org.xdi.util import StringHelper, ArrayHelper
from java.util import Arrays, ArrayList, HashMap, IdentityHashMap
from org.xdi.oxauth.client import TokenClient, TokenRequest, UserInfoClient
from org.xdi.oxauth.model.common import GrantType, AuthenticationMethod
from org.xdi.oxauth.model.jwt import Jwt, JwtClaimName
from org.xdi.oxauth.security import Identity
from org.xdi.model.custom.script.type.auth import PersonAuthenticationType
from org.xdi.oxauth.service import UserService, ClientService, AuthenticationService
from org.xdi.oxauth.model.common import User
from org.xdi.util import StringHelper
from org.xdi.oxauth.util import ServerUtil
from org.gluu.jsf2.service import FacesService
from org.xdi.oxauth.model.util import Base64Util
from org.python.core.util import StringUtil
from org.xdi.oxauth.service.net import HttpService
from org.xdi.oxauth.model.crypto import CryptoProviderFactory
from org.xdi.oxauth.model.configuration import AppConfiguration
from org.xdi.oxauth.model.common import WebKeyStorage

import json
import java

class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    print "Passport-social: Initialized successfully"

    def init(self, configurationAttributes):
        print "Passport-social: Initialization init method call"
        self.extensionModule = None
        self.attributesMapping = None
        if (configurationAttributes.containsKey("generic_remote_attributes_list") and
            configurationAttributes.containsKey("generic_local_attributes_list")):

            remoteAttributesList = configurationAttributes.get("generic_remote_attributes_list").getValue2()
            if (StringHelper.isEmpty(remoteAttributesList)):
                print "Passport-social: Initialization. The property generic_remote_attributes_list is empty"
                return False

            localAttributesList = configurationAttributes.get("generic_local_attributes_list").getValue2()
            if (StringHelper.isEmpty(localAttributesList)):
                print "Passport-social: Initialization. The property generic_local_attributes_list is empty"
                return False

            self.attributesMapping = self.prepareAttributesMapping(remoteAttributesList, localAttributesList)
            if (self.attributesMapping == None):
                print "Passport-social: Initialization. The attributes mapping isn't valid"
                return False

        if not configurationAttributes.containsKey("key_store_file"):
            print "Passport-social: Initialization. The property key_store_file is mandatory"
            return False

        if not configurationAttributes.containsKey("key_store_password"):
            print "Passport-social: Initialization. The property key_store_password is mandatory"
            return False

        self.keyStoreFile = configurationAttributes.get("key_store_file").getValue2()
        self.keyStorePassword = configurationAttributes.get("key_store_password").getValue2()

        if (configurationAttributes.containsKey("extension_module")):
            extensionModuleName = configurationAttributes.get("extension_module").getValue2()
            try:
                self.extensionModule = __import__(extensionModuleName)
                extensionModuleInitResult = self.extensionModule.init(configurationAttributes)
                if (not extensionModuleInitResult):
                    return False
            except ImportError, ex:
                print "Passport-social: Initialization. Failed to load generic_extension_module:", extensionModuleName
                print "Passport-social: Initialization. Unexpected error:", ex
                return False
        else:
            print("Passport-social: Extension module key not found")
        return True

    def destroy(self, configurationAttributes):
        print "Passport-social: Basic. Destroy method call"
        print "Passport-social: Basic. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 1

    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        return True

    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        return None

    def getUserValueFromAuth(self, remote_attr, requestParameters):
        try:
            toBeFeatched = "loginForm:" + remote_attr
            return ServerUtil.getFirstValue(requestParameters, toBeFeatched)
        except Exception, err:
            print("Passport-social: Exception inside getUserValueFromAuth " + str(err))
            return None

    def getUserValueFromJwt(self, user_profile, remote_attr):
        try:
            return user_profile[remote_attr]
        except Exception, err:
            print "Passport-social: Exception inside getUserValueFromJwt : %s" %err
            return None

    def makeAllUserProfilekeyLowerCase(self, user_profile):
        data = {}
        for key in user_profile.keys():
            data[key.lower()] = user_profile[key]

        return data     

    def authenticate(self, configurationAttributes, requestParameters, step):
        extensionResult = self.extensionAuthenticate(configurationAttributes, requestParameters, step)
        if extensionResult != None:
            return extensionResult

        authenticationService = CdiUtil.bean(AuthenticationService)
        userService = CdiUtil.bean(UserService)
        identity = CdiUtil.bean(Identity)

        try:
            UserId = self.getUserValueFromAuth("username", requestParameters)
        except Exception, err:
            print "Passport-social: Error: " + str(err)

        # Use basic method to log in
        if StringHelper.isNotEmpty(UserId):
            print "Passport-social: Basic Authentication"
            credentials = identity.getCredentials()
            user_name = credentials.getUsername()
            user_password = credentials.getPassword()

            logged_in = False
            if (StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password)):
                logged_in = authenticationService.authenticate(user_name, user_password)

            print "Passport-social: Basic Authentication returning %s" % logged_in
            return logged_in
        else:
            facesContext = CdiUtil.bean(FacesContext)

            # Get JWT token if it's post back call
            jwt_param = ServerUtil.getFirstValue(requestParameters, "user")
            if StringHelper.isEmpty(jwt_param):
                print "Passport-social: Authenticate for step 1. JWT token is missing"
                return False

            # Parse JWT token
            jwt = Jwt.parse(jwt_param)

            # Validate signature
            print "Passport-social: Authenticate for step 1. Checking JWT token signature: '%s'" % jwt
            appConfiguration = AppConfiguration()
            appConfiguration.setWebKeysStorage(WebKeyStorage.KEYSTORE)
            appConfiguration.setKeyStoreFile(self.keyStoreFile)
            appConfiguration.setKeyStoreSecret(self.keyStorePassword)

            cryptoProvider = CryptoProviderFactory.getCryptoProvider(appConfiguration)
            valid = cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(), jwt.getHeader().getKeyId(),
                                                        None, None, jwt.getHeader().getAlgorithm())
            print "Passport-social: Authenticate for step 1. JWT signature validation result: '%s'" % valid
            if not valid:
                print "Passport-social: Authenticate for step 1. JWT signature validation failed"
                return False

            # Check if there is user profile
            jwt_claims = jwt.getClaims()
            user_profile_json = jwt_claims.getClaimAsString("data")
            if StringHelper.isEmpty(user_profile_json):
                print "Passport-social: Authenticate for step 1. User profile is missing in JWT token"
                return False
            
            user_profile = json.loads(user_profile_json)


            # lower case user_profile keys(Necessary maximaze attribute retrieval)
            user_profile = self.makeAllUserProfilekeyLowerCase(user_profile)

                
            # Store user profile in session
            print "Passport-social: Authenticate for step 1. User profile: '%s'" % user_profile_json
            identity.setWorkingParameter("passport_user_profile", user_profile_json)

            uidRemoteAttr = self.getUidRemoteAttr()
            if uidRemoteAttr == None:
                print "Cannot retrieve uid remote attribute"
                return False
            else:
                uidRemoteAttrValue = self.getUserValueFromJwt(user_profile, uidRemoteAttr)
                if "shibboleth" in self.getUserValueFromJwt(user_profile, "provider"):
                    externalUid = "passport-saml:%s" % uidRemoteAttrValue
                else:
                    externalUid = "passport-%s:%s" % (self.getUserValueFromJwt(user_profile, "provider"), uidRemoteAttrValue)

                email = self.getUserValueFromJwt(user_profile, "email")
                if StringHelper.isEmptyString(email):
                    facesMessages = CdiUtil.bean(FacesMessages)
                    facesMessages.setKeepMessages()
                    self.clearFacesMessages(facesContext)
                    facesMessages.add(FacesMessage.SEVERITY_ERROR, "Please provide your email.")

                    print "Passport-social: Email was not received"
                    return False

                userByMail = userService.getUserByAttribute("mail", email)
                userByUid = userService.getUserByAttribute("oxExternalUid", externalUid)

                doUpdate = False
                doAdd = False
                if userByUid!=None:
                    print "User with externalUid '%s' already exists" % externalUid
                    if userByMail!=None:
                        if userByMail.getUserId()==userByUid.getUserId():
                            doUpdate = True
                    else:
                        doUpdate = True
                else:
                    if userByMail==None:
                        doAdd = True

                if doUpdate:
                    foundUser = userByUid
                    #update user with remote attributes coming
                    for attributesMappingEntry in self.attributesMapping.entrySet():
                        remoteAttribute = attributesMappingEntry.getKey()
                        localAttribute = attributesMappingEntry.getValue()
                        localAttributeValue = self.getUserValueFromJwt(user_profile, remoteAttribute)

                        if (localAttribute != None) and (localAttribute != "provider") and (localAttributeValue != "undefined"):
                            try:
                                value = None
                                values= foundUser.getAttributeValues(str(localAttribute));
                                if values != None:
                                    value = values[0]
                                if value != localAttributeValue:
                                    foundUser.setAttribute(localAttribute, localAttributeValue)
                            except Exception, err:
                                print "Error in update Attribute %s " %localAttribute
                                print("Error message: " + str(err))

                    try:
                        foundUserName = foundUser.getUserId()
                        print "Passport-social: Updating user %s" % foundUserName

                        userService.updateUser(foundUser)
                        userAuthenticated = authenticationService.authenticate(foundUserName)
                        print "Passport-social: Is user authenticated = " + str(userAuthenticated)

                        return userAuthenticated
                    except Exception, err:
                        return False

                if doAdd:
                    newUser = User()
                    #Fill user attrs
                    newUser.setAttribute("oxExternalUid", externalUid)

                    for attributesMappingEntry in self.attributesMapping.entrySet():
                        remoteAttribute = attributesMappingEntry.getKey()
                        localAttribute = attributesMappingEntry.getValue()
                        localAttributeValue = self.getUserValueFromJwt(user_profile, remoteAttribute)

                        if (localAttribute != None) and (localAttribute != "provider") and (localAttributeValue != "undefined"):
                            newUser.setAttribute(localAttribute, localAttributeValue)

                    try:
                        print "Passport-social: Adding user %s" % externalUid
                        foundUser = userService.addUser(newUser, True)
                        foundUserName = foundUser.getUserId()

                        userAuthenticated = authenticationService.authenticate(foundUserName)
                        print "Passport-social: User added successfully and isUserAuthenticated = " + str(userAuthenticated)

                        return userAuthenticated
                    except Exception, err:
                        print "Passport-social: Error in adding user:" + str(err)
                        return False

                return False

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        extensionResult = self.extensionPrepareForStep(configurationAttributes, requestParameters, step)
        if extensionResult != None:
            return extensionResult

        if step == 1:
            print "Passport-social: Prepare for step 1"
            identity = CdiUtil.bean(Identity)
            sessionId =  identity.getSessionId()
            sessionAttribute = sessionId.getSessionAttributes()
            print "Passport-social: session %s" % sessionAttribute
            oldState = sessionAttribute.get("state")
            print " Old State is : %s" %oldState
            return True
        else:
            return True

    def getExtraParametersForStep(self, configurationAttributes, step):
        if step == 2:
            return Arrays.asList("passport_user_profile")

        return None

    def getCountAuthenticationSteps(self, configurationAttributes):
        return 1

    def getPageForStep(self, configurationAttributes, step):
        extensionResult = self.extensionGetPageForStep(configurationAttributes, step)
        if extensionResult != None:
            return extensionResult

        if (step == 1):
            return "/auth/passport/passportlogin.xhtml"
        return "/auth/passport/passportpostlogin.xhtml"

    def logout(self, configurationAttributes, requestParameters):
        return True

    def prepareAttributesMapping(self, remoteAttributesList, localAttributesList):
        try:
            remoteAttributesListArray = StringHelper.split(remoteAttributesList, ",")
            if (ArrayHelper.isEmpty(remoteAttributesListArray)):
                print("Passport-social: PrepareAttributesMapping. There is no attributes specified in remoteAttributesList property")
                return None

            localAttributesListArray = StringHelper.split(localAttributesList, ",")
            if (ArrayHelper.isEmpty(localAttributesListArray)):
                print("Passport-social: PrepareAttributesMapping. There is no attributes specified in localAttributesList property")
                return None

            if (len(remoteAttributesListArray) != len(localAttributesListArray)):
                print("Passport-social: PrepareAttributesMapping. The number of attributes in remoteAttributesList and localAttributesList isn't equal")
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
                print "Passport-social: PrepareAttributesMapping. There is no mapping to mandatory 'uid' attribute"
                return None

            return attributeMapping
        except Exception, err:
            print("Passport-social: Exception inside prepareAttributesMapping " + str(err))

    def getUidRemoteAttr(self):
        try:
            for attributesMappingEntry in self.attributesMapping.entrySet():
                remoteAttribute = attributesMappingEntry.getKey()
                localAttribute = attributesMappingEntry.getValue()
                if localAttribute == "uid":
                    return remoteAttribute
        except Exception, err:
            print("Passport-social: Exception inside getUidRemoteAttr " + str(err))

        return None

    def extensionAuthenticate(self, configurationAttributes, requestParameters, step):
        if (self.extensionModule == None):
            return None

        try:
            result = self.extensionModule.authenticate(configurationAttributes, requestParameters, step)
            print "Passport-social: Extension. Authenticate: '%s'" % result

            return result
        except Exception, ex:
            print "Passport-social: Extension. Authenticate. Failed to execute postLogin method"
            print "Passport-social: Extension. Authenticate. Unexpected error:", ex
        except java.lang.Throwable, ex:
            print "Passport-social: Extension. Authenticate. Failed to execute postLogin method"
            ex.printStackTrace()

        return True

    def extensionGetPageForStep(self, configurationAttributes, step):
        if (self.extensionModule == None):
            return None

        try:
            result = self.extensionModule.getPageForStep(configurationAttributes, step)
            print "Passport-social: Extension. Get page for Step: '%s'" % result

            return result
        except Exception, ex:
            print "Passport-social: Extension. Get page for Step. Failed to execute postLogin method"
            print "Passport-social: Extension. Get page for Step. Unexpected error:", ex
        except java.lang.Throwable, ex:
            print "Passport-social: Extension. Get page for Step. Failed to execute postLogin method"
            ex.printStackTrace()

        return None

    def extensionPrepareForStep(self, configurationAttributes, requestParameters, step):
        if (self.extensionModule == None):
            return None

        try:
            result = self.extensionModule.prepareForStep(configurationAttributes, requestParameters, step)
            print "Passport-social: Extension. Prepare for Step: '%s'" % result

            return result
        except Exception, ex:
            print "Passport-social: Extension. Prepare for Step. Failed to execute postLogin method"
            print "Passport-social: Extension. Prepare for Step. Unexpected error:", ex
        except java.lang.Throwable, ex:
            print "Passport-social: Extension. Prepare for Step. Failed to execute postLogin method"
            ex.printStackTrace()

        return None

    def clearFacesMessages(self, context):

        if context!=None:
            try:
                iterator = context.getMessages()
                while iterator.hasNext():
                    iterator.next()
                    iterator.remove()
            except:
                print "Error clearing faces messages"
