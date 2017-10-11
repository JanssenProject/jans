# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2016, Gluu
#
# Author: Yuriy Movchan
#

from org.xdi.model.custom.script.type.user import UserRegistrationType
from org.xdi.ldap.model import GluuStatus
from org.xdi.util import StringHelper, ArrayHelper
from java.util import Arrays, ArrayList

import java

class UserRegistration(UserRegistrationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, configurationAttributes):
        print "User registration. Initialization"

        self.enable_user = StringHelper.toBoolean(configurationAttributes.get("enable_user").getValue2(), False)

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

        userStatus = GluuStatus.ACTIVE
        if not self.enable_user:
            userStatus = GluuStatus.INACTIVE

        # Disable/Enable registered user
        user.setStatus(userStatus)

        return True

    # User registration post method
    #   user is org.gluu.oxtrust.model.GluuCustomPerson
    #   requestParameters is java.util.Map<String, String[]>
    #   configurationAttributes is java.util.Map<String, SimpleCustomProperty>
    def postRegistration(self, user, requestParameters, configurationAttributes):
        print "User registration. Post method"

        return True
    
    # User confirm New Registration method
    #   user is org.gluu.oxtrust.model.GluuCustomPerson
    #   requestParameters is java.util.Map<String, String[]>
    #   configurationAttributes is java.util.Map<String, SimpleCustomProperty>
    def confirmRegistration(self, user, requestParameters, configurationAttributes):
        print "User registration. Confirm registration method"

        return True

    def getApiVersion(self):
        return 1
