# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2016, Gluu
#

from org.xdi.service.cdi.util import CdiUtil
from org.xdi.model.custom.script.type.user import UserRegistrationType
from org.xdi.service import MailService
from org.gluu.oxtrust.ldap.service import PersonService
from org.xdi.ldap.model import GluuStatus
from org.xdi.util import StringHelper, ArrayHelper
from java.util import Arrays, ArrayList
from org.xdi.config.oxtrust import AppConfiguration
from javax.faces.context import ExternalContext

import java

class UserRegistration(UserRegistrationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, configurationAttributes):
        print "User registration. Initialization"
        print "User registration. Initialized successfully"

        return True   

    def destroy(self, configurationAttributes):
        print "User registration. Destroy"
        print "User registration. Destroyed successfully"
        return True   

    # User registration init method
    #   user is org.gluu.oxtrust.model.GluuCustomPerson
    #   requestParameters is java.util.Map<String, String[]>
    #   configurationAttributes is java.util.Map<String, SimpleCustomProperty>
    def initRegistration(self, user, requestParameters, configurationAttributes):
        print "User registration. Init method"

        return True

    # User registration pre method
    #   user is org.gluu.oxtrust.model.GluuCustomPerson
    #   requestParameters is java.util.Map<String, String[]>
    #   configurationAttributes is java.util.Map<String, SimpleCustomProperty>
    def preRegistration(self, user, requestParameters, configurationAttributes):
        print "User registration. Pre method"
        
        userStatus = GluuStatus.INACTIVE

        # Disable/Enable registered user
        user.setStatus(userStatus)
        self.guid = StringHelper.getRandomString(16)
        user.setGuid(self.guid)
        return True

    # User registration post method
    #   user is org.gluu.oxtrust.model.GluuCustomPerson
    #   requestParameters is java.util.Map<String, String[]>
    #   configurationAttributes is java.util.Map<String, SimpleCustomProperty>
    def postRegistration(self, user, requestParameters, configurationAttributes):
        print "User registration. Post method"
        appConfiguration = CdiUtil.bean(AppConfiguration)

        hostName = appConfiguration.getApplianceUrl()
        externalContext = CdiUtil.bean(ExternalContext)
        contextPath = externalContext.getRequest().getContextPath() 

        mailService = CdiUtil.bean(MailService)
        subject = "Confirmation mail for user registration"
        body = "User Registered for %s. Please Confirm User Registration by clicking url: %s%s/confirm/registration?code=%s" % (user.getMail(), hostName, contextPath, self.guid)
        print "User registration. Post method. Attempting to send e-mail to '%s' message '%s'" % (user.getMail(), body)

        mailService.sendMail(user.getMail(), subject, body)
        return True

    def confirmRegistration(self, user, requestParameters, configurationAttributes):
        print "User registration. Confirm method"

        code_array = requestParameters.get("code")
        if ArrayHelper.isEmpty(code_array):
            print "User registration. Confirm method. code is empty"
            return False

        confirmation_code = code_array[0]
        print "User registration. Confirm method. code: '%s'" % confirmation_code

        if confirmation_code == None:
            print "User registration. Confirm method. Confirmation code not exist in request"
            return False

        personService = CdiUtil.bean(PersonService)
        user = personService.getPersonByAttribute("oxGuid", confirmation_code)
        if user == None:
            print "User registration. Confirm method. There is no user by confirmation code: '%s'" % confirmation_code
            return False

        if confirmation_code == user.getGuid():
            user.setStatus(GluuStatus.ACTIVE)
            user.setGuid("")
            personService.updatePerson(user)
            print "User registration. Confirm method. User '%s' confirmed his registration" % user.getUid()
            return True

        print "User registration. Confirm method. Confirmation code for user '%s' is invalid" % user.getUid()
    	return False

    def getApiVersion(self):
        return 1
