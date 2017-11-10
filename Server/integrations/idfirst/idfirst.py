# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2016, Gluu
#
# Author: Jose Gonzalez (based on acr_routerauthenticator.py)
#
# NOTE: before using this script, see the accompanying readme file

from org.xdi.oxauth.security import Identity
from org.xdi.model.custom.script.type.auth import PersonAuthenticationType
from org.xdi.oxauth.service import UserService
from org.xdi.util import StringHelper
from org.xdi.service.cdi.util import CdiUtil

import java

class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, configurationAttributes):
        print "Identifier First. Initialized successfully"
        return True   

    def destroy(self, configurationAttributes):
        print "Identifier First. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 1

    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        print "Identifier First. isValidAuthenticationMethod called"
        return False

    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        print "Identifier First. getAlternativeAuthenticationMethod"

        identity = CdiUtil.bean(Identity)
        user_name = identity.getCredentials().getUsername()
        print "Identifier First. Inspecting user %s" % user_name

        attributes=identity.getSessionId().getSessionAttributes()
        attributes.put("roUserName", user_name)

        acr = None
        try:
            userService = CdiUtil.bean(UserService)
            foundUser = userService.getUserByAttribute("uid", user_name)

            if foundUser == None:
                print "Identifier First. User does not exist"
                return ""

            attr = configurationAttributes.get("acr_attribute").getValue2()
            acr=foundUser.getAttribute(attr)     
            #acr="u2f" or "otp" or "twilio_sms", etc...
            if acr == None:
                acr = "basic"
        except:
            print "Identifier First. Error looking up user or his preferred method"         

        print "Identifier First. new acr value %s" % acr
        return acr

    def authenticate(self, configurationAttributes, requestParameters, step):
        return False

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        print "Identifier First. prepareForStep %s" % str(step)
        return True

    def getExtraParametersForStep(self, configurationAttributes, step):
        print "Identifier First. getExtraParametersForStep %s" % str(step)  
        return None

    def getCountAuthenticationSteps(self, configurationAttributes):
        print "Identifier First. getCountAuthenticationSteps called"
        return 2

    def getPageForStep(self, configurationAttributes, step):
        print "Identifier First. getPageForStep called %s" % str(step)
        if step == 1:
            return "/auth/idfirst/idfirst_login.xhtml"
        return ""

    def logout(self, configurationAttributes, requestParameters):
        print "Identifier First. logout called"
        return True
