# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2016, Gluu
#
# Author: Arvind Tomar
#

from org.xdi.service.cdi.util import CdiUtil
from org.gluu.jsf2.message import FacesMessages
from javax.faces.application import FacesMessage
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

import json
import java

class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    print "Passport: Basic. Initialized successfully"

    def init(self, configurationAttributes):
        print "Passport: Basic. Initialization init method call"
        self.extensionModule = None
        self.attributesMapping = None
        if (configurationAttributes.containsKey("generic_remote_attributes_list") and
                configurationAttributes.containsKey("generic_local_attributes_list")):

            remoteAttributesList = configurationAttributes.get("generic_remote_attributes_list").getValue2()
            if (StringHelper.isEmpty(remoteAttributesList)):
                print "Passport: Initialization. The property generic_remote_attributes_list is empty"
                return False

            localAttributesList = configurationAttributes.get("generic_local_attributes_list").getValue2()
            if (StringHelper.isEmpty(localAttributesList)):
                print "Passport: Initialization. The property generic_local_attributes_list is empty"
                return False

            self.attributesMapping = self.prepareAttributesMapping(remoteAttributesList, localAttributesList)
            if (self.attributesMapping == None):
                print "Passport: Initialization. The attributes mapping isn't valid"
                return False
        if (configurationAttributes.containsKey("extension_module")):
            extensionModuleName = configurationAttributes.get("extension_module").getValue2()
            try:
                self.extensionModule = __import__(extensionModuleName)
                extensionModuleInitResult = self.extensionModule.init(configurationAttributes)
                if (not extensionModuleInitResult):
                    return False
            except ImportError, ex:
                print "Passport: Initialization. Failed to load generic_extension_module:", extensionModuleName
                print "Passport: Initialization. Unexpected error:", ex
                return False
        else:
            print("Passport: Extension module key not found")
        return True

    def destroy(self, configurationAttributes):
        print "Passport: Basic. Destroy method call"
        print "Passport: Basic. Destroyed successfully"
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
            print("Passport: Exception inside getUserValueFromAuth " + str(err))

    def authenticate(self, configurationAttributes, requestParameters, step):
        authenticationService = CdiUtil.bean(AuthenticationService)

        try:
            UserId = self.getUserValueFromAuth("userid", requestParameters)
        except Exception, err:
            print("Passport: Error: " + str(err))
        useBasicAuth = False
        if (StringHelper.isEmptyString(UserId)):
            useBasicAuth = True

        # Use basic method to log in
        if (useBasicAuth):
            print "Passport: Basic Authentication"
            identity = CdiUtil.bean(Identity)
            credentials = identity.getCredentials()

            user_name = credentials.getUsername()
            user_password = credentials.getPassword()

            logged_in = False
            if (StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password)):
                userService = CdiUtil.bean(UserService)
                logged_in = authenticationService.authenticate(user_name, user_password)

            if (not logged_in):
                return False
            return True

        else:
            try:
                userService = CdiUtil.bean(UserService)
                authenticationService = CdiUtil.bean(AuthenticationService)
                foundUser = userService.getUserByAttribute("oxExternalUid", self.getUserValueFromAuth("provider",
                                                                                                      requestParameters) + ":" + self.getUserValueFromAuth(
                    self.getUidRemoteAttr(), requestParameters))

                if (foundUser == None):
                    newUser = User()

                    try:
                        UserEmail = self.getUserValueFromAuth("email", requestParameters)
                    except Exception, err:
                        print("Passport: Error in getting user email: " + str(err))

                    if (StringHelper.isEmptyString(UserEmail)):
                        facesMessages = CdiUtil.bean(FacesMessages)
                        facesMessages.setKeepMessages()
                        facesMessages.clear()
                        facesMessages.add(FacesMessage.SEVERITY_ERROR, "Please provide your email.")
                        print "Passport: Email was not received so sent error"

                        return False

                    for attributesMappingEntry in self.attributesMapping.entrySet():
                        remoteAttribute = attributesMappingEntry.getKey()
                        localAttribute = attributesMappingEntry.getValue()
                        localAttributeValue = self.getUserValueFromAuth(remoteAttribute, requestParameters)
                        if ((localAttribute != None) & (localAttributeValue != "undefined") & (
                                    localAttribute != "provider")):
                            newUser.setAttribute(localAttribute, localAttributeValue)
                    newUser.setAttribute("oxExternalUid", self.getUserValueFromAuth("provider",
                                                                                    requestParameters) + ":" + self.getUserValueFromAuth(
                        self.getUidRemoteAttr(), requestParameters))
                    print ("Passport: " + self.getUserValueFromAuth("provider",
                                                     requestParameters) + ": Attempting to add user " + self.getUserValueFromAuth(
                        self.getUidRemoteAttr(), requestParameters))

                    try:
                        foundUser = userService.addUser(newUser, True)
                        foundUserName = foundUser.getUserId()
                        print("Passport: Found user name " + foundUserName)
                        userAuthenticated = authenticationService.authenticate(foundUserName)
                        print("Passport: User added successfully and isUserAuthenticated = " + str(userAuthenticated))
                    except Exception, err:
                        print("Passport: Error in adding user:" + str(err))
                        return False
                    return userAuthenticated

                else:
                    foundUserName = foundUser.getUserId()
                    print("Passport: User Found " + str(foundUserName))
                    userAuthenticated = authenticationService.authenticate(foundUserName)
                    print("Passport: Is user authenticated = " + str(userAuthenticated))
                    return True

            except Exception, err:
                print ("Passport: Error occurred during request parameter fetching " + str(err))

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        if (step == 1):
            print "Passport: Basic. Prepare for Step 1 method call"
            return True
        else:
            return True

    def getExtraParametersForStep(self, configurationAttributes, step):
        return None

    def getCountAuthenticationSteps(self, configurationAttributes):
        return 1

    def getPageForStep(self, configurationAttributes, step):
        if (step == 1):
            return "/auth/passport/passportlogin.xhtml"
        return "/auth/passport/passportpostlogin.xhtml"

    def logout(self, configurationAttributes, requestParameters):
        return True

    def prepareAttributesMapping(self, remoteAttributesList, localAttributesList):
        try:
            remoteAttributesListArray = StringHelper.split(remoteAttributesList, ",")
            if (ArrayHelper.isEmpty(remoteAttributesListArray)):
                print("Passport: PrepareAttributesMapping. There is no attributes specified in remoteAttributesList property")
                return None

            localAttributesListArray = StringHelper.split(localAttributesList, ",")
            if (ArrayHelper.isEmpty(localAttributesListArray)):
                print("Passport: PrepareAttributesMapping. There is no attributes specified in localAttributesList property")
                return None

            if (len(remoteAttributesListArray) != len(localAttributesListArray)):
                print("Passport: PrepareAttributesMapping. The number of attributes in remoteAttributesList and localAttributesList isn't equal")
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
                print "Passport: PrepareAttributesMapping. There is no mapping to mandatory 'uid' attribute"
                return None

            return attributeMapping
        except Exception, err:
            print("Passport: Exception inside prepareAttributesMapping " + str(err))

    def getUidRemoteAttr(self):
        try:
            for attributesMappingEntry in self.attributesMapping.entrySet():
                remoteAttribute = attributesMappingEntry.getKey()
                localAttribute = attributesMappingEntry.getValue()
                if localAttribute == "uid":
                    return remoteAttribute
            else:
                return "Not Get UID related remote attribute"
        except Exception, err:
            print("Passport: Exception inside getUidRemoteAttr " + str(err))
