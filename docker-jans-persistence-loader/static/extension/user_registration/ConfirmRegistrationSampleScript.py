# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2016, Janssen
#

from io.jans.service.cdi.util import CdiUtil
from io.jans.model.custom.script.type.user import UserRegistrationType
from io.jans.service import MailService
from io.jans.oxtrust.service import PersonService
from io.jans.util import StringHelper, ArrayHelper
from java.util import Arrays, ArrayList
from io.jans.config.oxtrust import AppConfiguration
from javax.faces.context import ExternalContext
from io.jans.oxtrust.service import ConfigurationService

import java

class UserRegistration(UserRegistrationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "User Confirm registration. Initialization"
        print "User Confirm registration. Initialized successfully"

        return True   

    def destroy(self, configurationAttributes):
        print "User Confirm registration. Destroy"
        print "User Confirm registration. Destroyed successfully"
        return True   

    # User registration init method
    #   user is io.jans.oxtrust.model.JanssenCustomPerson
    #   requestParameters is java.util.Map<String, String[]>
    #   configurationAttributes is java.util.Map<String, SimpleCustomProperty>
    def initRegistration(self, user, requestParameters, configurationAttributes):
        print "User Confirm registration. Init method"
        #hostName = requestParameters.get("hostName")[0]
        #print "HostName Initialization : %s" % hostName
        return True

    # User registration pre method
    #   user is io.jans.oxtrust.model.JanssenCustomPerson
    #   requestParameters is java.util.Map<String, String[]>
    #   configurationAttributes is java.util.Map<String, SimpleCustomProperty>
    def preRegistration(self, user, requestParameters, configurationAttributes):
        print "User Confirm registration. Pre method"
        userStatus = "inactive"

        # Disable/Enable registered user
        user.setStatus(userStatus)
        self.guid = StringHelper.getRandomString(16)
        user.setGuid(self.guid)
        return True

    # User registration post method
    #   user is io.jans.oxtrust.model.JanssenCustomPerson
    #   requestParameters is java.util.Map<String, String[]>
    #   configurationAttributes is java.util.Map<String, SimpleCustomProperty>
    def postRegistration(self, user, requestParameters, configurationAttributes):
        print "User Confirm registration. Post method"
        externalContext = CdiUtil.bean(ExternalContext)
        contextPath = externalContext.getRequest().getContextPath()
        hostName =  externalContext.getRequestServerName()
        print "HostName from context : %s" % hostName
        mailService = CdiUtil.bean(MailService)
        subject = "Registration confirmation"
        activationLink = "https://%s%s/confirm/registration.htm?code=%s" %(hostName, contextPath, self.guid)
        body = "<h2 style='margin-left:10%%;color: #337ab7;'>Welcome</h2><hr style='width:80%%;border: 1px solid #337ab7;'></hr><div style='text-align:center;'><p>Dear <span style='color: #337ab7;'>%s</span>,</p><p>Your Account has been created, welcome to <span style='color: #337ab7;'>%s</span>.</p><p>You are just one step way from activating your account on <span style='color: #337ab7;'>%s</span>.</p><p>Click the button and start using your account.</p></div><a class='btn' href='%s'><button style='background: #337ab7; color: white; margin-left: 30%%; border-radius: 5px; border: 0px; padding: 5px;' type='button'>Activate your account now!</button></a>"  % (user.getUid(), hostName, hostName, activationLink)
        print "User Confirm registration. Post method. Attempting to send e-mail to '%s' message '%s'" % (user.getMail(), body)
        mailService.sendMail(user.getMail(), None, subject, body, body);
        return True

    def confirmRegistration(self, user, requestParameters, configurationAttributes):
        print "User Confirm registration. Confirm method"
        code_array = requestParameters.get("code")
        if ArrayHelper.isEmpty(code_array):
            print "User Confirm registration. Confirm method. code is empty"
            return False

        confirmation_code = code_array[0]
        print "User Confirm registration. Confirm method. code: '%s'" % confirmation_code

        if confirmation_code == None:
            print "User Confirm registration. Confirm method. Confirmation code not exist in request"
            return False

        personService = CdiUtil.bean(PersonService)
        user = personService.getPersonByAttribute("oxGuid", confirmation_code)
        if user == None:
            print "User Confirm registration. Confirm method. There is no user by confirmation code: '%s'" % confirmation_code
            return False

        if confirmation_code == user.getGuid():
            user.setStatus("active")
            user.setGuid("")
            personService.updatePerson(user)
            print "User Confirm registration. Confirm method. User '%s' confirmed his registration" % user.getUid()
            return True

        print "User Confirm registration. Confirm method. Confirmation code for user '%s' is invalid" % user.getUid()
        return False

    def getApiVersion(self):
        return 11
