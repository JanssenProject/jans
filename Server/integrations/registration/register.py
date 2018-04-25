# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2016, Gluu
#
# Author: Yuriy Movchan
#

from org.xdi.service.cdi.util import CdiUtil
from org.xdi.oxauth.security import Identity
from org.gluu.jsf2.message import FacesMessages
from javax.faces.application import FacesMessage
from org.xdi.util import StringHelper, ArrayHelper
from java.util import Arrays, ArrayList, HashMap, IdentityHashMap
from org.xdi.model.custom.script.type.auth import PersonAuthenticationType
from org.xdi.oxauth.service import UserService, ClientService, AuthenticationService
from org.xdi.util import StringHelper
from org.xdi.oxauth.model.common import User
from org.xdi.oxauth.util import ServerUtil
from org.gluu.jsf2.service import FacesService
from org.xdi.oxauth.model.util import Base64Util
from org.python.core.util import StringUtil
from org.xdi.oxauth.service.net import HttpService
from javax.faces.context import FacesContext

import java

class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, configurationAttributes):
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
