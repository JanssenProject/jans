# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2016, Janssen
#
# Author: Yuriy Movchan
#

from io.jans.model.custom.script.type.user import LinkInterceptionType
from io.jans.util import StringHelper, ArrayHelper
from java.util import Arrays, ArrayList
from io.jans.model import JansCustomAttribute
from io.jans.model.custom.script.model.bind import BindCredentials

import java

class LinkInterception(LinkInterceptionType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Link Interception. Initialization"
        print "Link Interception. Initialized successfully"

        return True   

    def destroy(self, configurationAttributes):
        print "Link Interception. Destroy"
        print "Link Interception. Destroyed successfully"
        return True

    # Check if this instance conform starting conditions 
    #   configurationAttributes is java.util.Map<String, SimpleCustomProperty>
    #   return True/False
    def isStartProcess(self, configurationAttributes):
        print "Link Interception. Is start process method"

        return False
    
    # Get bind credentials required to access source server 
    #   configId is the source server
    #   configurationAttributes is java.util.Map<String, SimpleCustomProperty>
    #   return None (use password from configuration) or io.jans.model.custom.script.model.bind.BindCredentials
    def getBindCredentials(self, configId, configurationAttributes):
        print "Link Interception. GetBindCredentials method"
#        if configId == "source":
#            return BindCredentials("cn=Directory Manager", "password")

        return None

    # Update user entry before persist it
    #   user is io.jans.oxtrust.model.JanssenCustomPerson
    #   configurationAttributes is java.util.Map<String, SimpleCustomProperty>
    def updateUser(self, user, configurationAttributes):
        print "Link Interception. UpdateUser method"

        attributes = user.getCustomAttributes()

        # Add new attribute preferredLanguage
        attrPrefferedLanguage = JanssenCustomAttribute("preferredLanguage", "en-us")
        attributes.add(attrPrefferedLanguage)

        # Add new attribute userPassword
        attrUserPassword = JanssenCustomAttribute("userPassword", "test")
        attributes.add(attrUserPassword)

        # Update givenName attribute
        for attribute in attributes:
            attrName = attribute.getName()
            if (("givenname" == StringHelper.toLowerCase(attrName)) and StringHelper.isNotEmpty(attribute.getValue())):
                attribute.setValue(StringHelper.removeMultipleSpaces(attribute.getValue()) + " (updated)")

        return True

    def getApiVersion(self):
        return 11
