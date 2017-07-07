# oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2014, Gluu
#
# Author: Val Pecaoco
#
from org.xdi.model.custom.script.type.scim import ScimType
from org.xdi.util import StringHelper, ArrayHelper
from java.util import Arrays, ArrayList
from org.gluu.oxtrust.ldap.service import IPersonService, PersonService
from org.gluu.oxtrust.model import GluuCustomPerson

import java

class ScimEventHandler(ScimType):

    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, configurationAttributes):
        print "ScimEventHandler (init): Initialized successfully"
        return True   

    def destroy(self, configurationAttributes):
        print "ScimEventHandler (destroy): Destroyed successfully"
        return True   

    def getApiVersion(self):
        return 1

    def createUser(self, user, configurationAttributes):

        print "ScimEventHandler (createUser): Current id = " + user.getUid()

        testProp1 = configurationAttributes.get("testProp1").getValue2()
        testProp2 = configurationAttributes.get("testProp2").getValue2()

        print "ScimEventHandler (createUser): testProp1 = " + testProp1
        print "ScimEventHandler (createUser): testProp2 = " + testProp2

        return True

    def updateUser(self, user, configurationAttributes):
        personService = PersonService.instance()
        oldUser = personService.getPersonByUid(user.getUid())
        print "ScimEventHandler (updateUser): Old displayName = " + oldUser .getDisplayName()
        print "ScimEventHandler (updateUser): New displayName = " + user.getDisplayName()
        return True

    def deleteUser(self, user, configurationAttributes):
        print "ScimEventHandler (deleteUser): Current id = " + user.getUid()
        return True

    def createGroup(self, group, configurationAttributes):
        print "ScimEventHandler (createGroup): Current displayName = " + group.getDisplayName()
        return True

    def updateGroup(self, group, configurationAttributes):
        print "ScimEventHandler (updateGroup): Current displayName = " + group.getDisplayName()
        return True

    def deleteGroup(self, group, configurationAttributes):
        print "ScimEventHandler (deleteGroup): Current displayName = " + group.getDisplayName()
        return True
