# Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
# Copyright (c) 2020, Janssen Project
#
# Author: Yuriy Movchan
#

from io.jans.service.cdi.util import CdiUtil
from io.jans.as.server.security import Identity
from io.jans.jsf2.message import FacesMessages
from jakarta.faces.application import FacesMessage
from io.jans.util import StringHelper, ArrayHelper
from java.util import Arrays, ArrayList, HashMap, IdentityHashMap
from io.jans.model.custom.script.type.auth import PersonAuthenticationType
from io.jans.as.server.service import UserService, ClientService, AuthenticationService
from io.jans.util import StringHelper
from org.gluu.oxauth.model.common import User
from io.jans.as.server.util import ServerUtil
from io.jans.jsf2.service import FacesService
from org.gluu.oxauth.model.util import Base64Util
from org.python.core.util import StringUtil
from io.jans.as.server.service.net import HttpService
from jakarta.faces.context import FacesContext

import java

class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Registration. Initialization"
        print "Registration. Initialized successfully"
        if (configurationAttributes.containsKey("generic_register_attributes_list") and
                configurationAttributes.containsKey("generic_local_attributes_list")):

            remoteAttributesList = configurationAttributes.get("generic_register_attributes_list").getValue2()
            if (StringHelper.isEmpty(remoteAttributesList)):
                print "Registration: Initialization. The property generic_register_attributes_list is empty"
                return False

            localAttributesList = configurationAttributes.get("generic_local_attributes_list").getValue2()
            if (StringHelper.isEmpty(localAttributesList)):
                print "Registration: Initialization. The property generic_local_attributes_list is empty"
                return False

            self.attributesMapping = self.prepareAttributesMapping(remoteAttributesList, localAttributesList)
            if (self.attributesMapping == None):
                print "Registration: Initialization. The attributes mapping isn't valid"
                return False

        return True
        
        
    def destroy(self, configurationAttributes):
        print "Registration. Destroy"
        print "Registration. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 11
        
    def getAuthenticationMethodClaims(self, requestParameters):
        return None
        
    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        return True

    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        return None
    def getUserValueFromAuth(self, remote_attr, requestParameters):
        try:
            toBeFeatched = "loginForm:" + remote_attr
            return ServerUtil.getFirstValue(requestParameters, toBeFeatched)
        except Exception, err:
            print("Registration: Exception inside getUserValueFromAuth " + str(err))

    def authenticate(self, configurationAttributes, requestParameters, step):
        print "Registration. Authenticate for step 1"
        userService = CdiUtil.bean(UserService)
        authenticationService = CdiUtil.bean(AuthenticationService)

	if (StringHelper.isEmptyString(self.getUserValueFromAuth("email", requestParameters))):
	    facesMessages = CdiUtil.bean(FacesMessages)
	    facesMessages.setKeepMessages()
	    facesMessages.add(FacesMessage.SEVERITY_ERROR, "Please provide your email.")
            return False

	if (StringHelper.isEmptyString(self.getUserValueFromAuth("pwd", requestParameters))):
	    facesMessages = CdiUtil.bean(FacesMessages)
	    facesMessages.setKeepMessages()
	    facesMessages.add(FacesMessage.SEVERITY_ERROR, "Please provide password.")
            return False



        foundUser = userService.getUserByAttribute("mail", self.getUserValueFromAuth("email", requestParameters))
        if (foundUser == None):
            newUser = User()
	    for attributesMappingEntry in self.attributesMapping.entrySet():
		remoteAttribute = attributesMappingEntry.getKey()
		localAttribute = attributesMappingEntry.getValue()
		localAttributeValue = self.getUserValueFromAuth(remoteAttribute, requestParameters)
		if ((localAttribute != None) & (localAttributeValue != "undefined")):
		    print localAttribute + localAttributeValue
		    newUser.setAttribute(localAttribute, localAttributeValue)

	    try:
		foundUser = userService.addUser(newUser, True)
		foundUserName = foundUser.getUserId()
		print("Registration: Found user name " + foundUserName)
		userAuthenticated = authenticationService.authenticate(foundUserName)
		print("Registration: User added successfully and isUserAuthenticated = " + str(userAuthenticated))
	    except Exception, err:
		print("Registration: Error in adding user:" + str(err))
		return False
	    return userAuthenticated
	else:
	    facesMessages = CdiUtil.bean(FacesMessages)
	    facesMessages.setKeepMessages()
	    facesMessages.add(FacesMessage.SEVERITY_ERROR, "User with same email already exists!")
	    return False



    def prepareForStep(self, configurationAttributes, requestParameters, step):
        if (step == 1):
            print "Registration. Prepare for Step 1"
            return True
        else:
            return False

    def getExtraParametersForStep(self, configurationAttributes, step):
        return None

    def getCountAuthenticationSteps(self, configurationAttributes):
        return 1

    def getPageForStep(self, configurationAttributes, step):
        if step == 1:
            return "/auth/register/register.xhtml"

    def getNextStep(self, configurationAttributes, requestParameters, step):
        return -1

    def getLogoutExternalUrl(self, configurationAttributes, requestParameters):
        print "Get external logout URL call"
        return None

    def logout(self, configurationAttributes, requestParameters):
        return True

    def prepareAttributesMapping(self, remoteAttributesList, localAttributesList):
        try:
            remoteAttributesListArray = StringHelper.split(remoteAttributesList, ",")
            if (ArrayHelper.isEmpty(remoteAttributesListArray)):
                print("Registration: PrepareAttributesMapping. There is no attributes specified in remoteAttributesList property")
                return None

            localAttributesListArray = StringHelper.split(localAttributesList, ",")
            if (ArrayHelper.isEmpty(localAttributesListArray)):
                print("Registration: PrepareAttributesMapping. There is no attributes specified in localAttributesList property")
                return None

            if (len(remoteAttributesListArray) != len(localAttributesListArray)):
                print("Registration: PrepareAttributesMapping. The number of attributes in remoteAttributesList and localAttributesList isn't equal")
                return None

            attributeMapping = IdentityHashMap()
            containsUid = False
            i = 0
            count = len(remoteAttributesListArray)
            while (i < count):
                remoteAttribute = StringHelper.toLowerCase(remoteAttributesListArray[i])
                localAttribute = StringHelper.toLowerCase(localAttributesListArray[i])
                attributeMapping.put(remoteAttribute, localAttribute)

                i = i + 1

            return attributeMapping
        except Exception, err:
            print("Registration: Exception inside prepareAttributesMapping " + str(err))
