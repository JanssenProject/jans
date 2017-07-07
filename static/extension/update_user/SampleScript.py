# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2016, Gluu
#
# Author: Yuriy Movchan
#

from org.xdi.model.custom.script.type.user import UpdateUserType
from org.xdi.util import StringHelper, ArrayHelper
from java.util import Arrays, ArrayList

import java

class UpdateUser(UpdateUserType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, configurationAttributes):
        print "Update user. Initialization"
        print "Update user. Initialized successfully"

        return True   

    def destroy(self, configurationAttributes):
        print "Update user. Destroy"
        print "Update user. Destroyed successfully"
        return True   

    # Update user entry before persistent it
    #   user is org.gluu.oxtrust.model.GluuCustomPerson
    #   persisted is boolean value to specify if operation type: add/modify
    #   configurationAttributes is java.util.Map<String, SimpleCustomProperty>
    def updateUser(self, user, persisted, configurationAttributes):
        print "Update user. UpdateUser method"

        uid = user.getUid()
        print "Update user. User Uid:", uid
        
        mail = uid + "@example.org"
        user.setMail(mail)

        return True

    def getApiVersion(self):
        return 1
    
    def addUser(self, user, persisted, configurationAttributes):
        print "addUser user. addUser method"

        return True
		
    def deleteUser(self, user, persisted, configurationAttributes):
        print "deleteUser user. deleteUser method"

        return True
		
    def newUser(self, user, configurationAttributes):
        print "newUser user. newUser method"

        return True
