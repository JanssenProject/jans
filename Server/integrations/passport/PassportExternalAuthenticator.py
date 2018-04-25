# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2016, Gluu
#
# Author: Arvind Tomar
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

    def authenticate(self, configurationAttributes, requestParameters, step):
        extensionResult = self.extensionAuthenticate(configurationAttributes, requestParameters, step)
        if extensionResult != None:
            return extensionResult

        authenticationService = CdiUtil.bean(AuthenticationService)

        try:
            UserId = self.getUserValueFromAuth("userid", requestParameters)
        except Exception, err:
            print "Passport-social: Error: " + str(err)

        useBasicAuth = StringHelper.isEmptyString(UserId)

        # Use basic method to log in
        if useBasicAuth:
            print "Passport-social: Basic Authentication"
            identity = CdiUtil.bean(Identity)
            credentials = identity.getCredentials()

            user_name = credentials.getUsername()
            user_password = credentials.getPassword()

            logged_in = False
            if (StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password)):
                userService = CdiUtil.bean(UserService)
                logged_in = authenticationService.authenticate(user_name, user_password)

            print "Passport-social: Basic Authentication returning %s" % logged_in
            return logged_in
        else:
            facesContext = CdiUtil.bean(FacesContext)
            userService = CdiUtil.bean(UserService)
            authenticationService = CdiUtil.bean(AuthenticationService)

            uidRemoteAttr = self.getUidRemoteAttr()
            if uidRemoteAttr == None:
                print "Cannot retrieve uid remote attribute"
                return False
            else:
                uidRemoteAttrValue = self.getUserValueFromAuth(uidRemoteAttr, requestParameters)
                if "shibboleth" in self.getUserValueFromAuth("provider", requestParameters):
                    externalUid = "passport-saml:%s" % uidRemoteAttrValue
                else:
                    externalUid = "passport-%s:%s" % (self.getUserValueFromAuth("provider", requestParameters), uidRemoteAttrValue)

                email = self.getUserValueFromAuth("email", requestParameters)
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
                        localAttributeValue = self.getUserValueFromAuth(remoteAttribute, requestParameters)

                        if (localAttribute != None) and (localAttribute != "provider") and (localAttributeValue != "undefined"):
                            try:
                                value = foundUser.getAttributeValues(str(localAttribute))[0]
                                if value != localAttributeValue:
                                    foundUser.setAttribute(localAttribute, localAttributeValue)
                            except Exception, err:
                                print("Error in update Attribute " + str(err))

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
                        localAttributeValue = self.getUserValueFromAuth(remoteAttribute, requestParameters)

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

        if (step == 1):
            print "Passport-social: Prepare for Step 1 method call"
            identity = CdiUtil.bean(Identity)
            sessionId =  identity.getSessionId()
            sessionAttribute = sessionId.getSessionAttributes()
            print "Passport-social: session %s" % sessionAttribute
            oldState = sessionAttribute.get("state")
            if(oldState == None):
                print "Passport-social: old state is none"
                return True
            else:
                print "Passport-social: state is obtained"
                try:
                    stateBytes = Base64Util.base64urldecode(oldState)
                    state = StringUtil.fromBytes(stateBytes)
                    stateObj = json.loads(state)
                    print stateObj["provider"]
                    for y in stateObj:
                        print (y,':',stateObj[y])
                    httpService = CdiUtil.bean(HttpService)
                    facesService = CdiUtil.bean(FacesService)
                    facesContext = CdiUtil.bean(FacesContext)
                    httpclient = httpService.getHttpsClient()
                    headersMap = HashMap()
                    headersMap.put("Accept", "text/json")
                    host = facesContext.getExternalContext().getRequest().getServerName()
                    url = "https://"+host+"/passport/token"
                    print "Passport-social: url %s" %url
                    resultResponse = httpService.executeGet(httpclient, url , headersMap)
                    http_response = resultResponse.getHttpResponse()
                    response_bytes = httpService.getResponseContent(http_response)
                    szResponse = httpService.convertEntityToString(response_bytes)
                    print "Passport-social: szResponse %s" % szResponse
                    tokenObj = json.loads(szResponse)
                    print "Passport-social: /passport/auth/saml/"+stateObj["provider"]+"/"+tokenObj["token_"]
                    facesService.redirectToExternalURL("/passport/auth/saml/"+stateObj["provider"]+"/"+tokenObj["token_"])

                except Exception, err:
                    print str(err)
                    return True
            return True
        else:
            return True

    def getExtraParametersForStep(self, configurationAttributes, step):
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