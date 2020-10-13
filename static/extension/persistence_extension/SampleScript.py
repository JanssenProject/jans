# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2020, Gluu
#
# Author: Yuriy Movchan
#

from org.gluu.service.cdi.util import CdiUtil
from org.gluu.model.custom.script.type.persistence import PersistenceType
from org.gluu.util import StringHelper
from org.gluu.persist.operation.auth import PasswordEncryptionHelper
from org.gluu.persist.operation.auth import PasswordEncryptionMethod

import java

class PersistenceExtension(PersistenceType):

    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Persistence extension. Initialization"
        return True

    def destroy(self, configurationAttributes):
        print "Persistence extension. Destroy"
        return True

    def getApiVersion(self):
        return 11

    def onAfterCreate(self, context, configurationAttributes):
        print "Persistence extension. Method: onAfterCreate"

    def onAfterDestroy(self, context, configurationAttributes):
        print "Persistence extension. Method: onAfterDestroy"

    def createHashedPassword(self, credential):
        print "Persistence extension. Method: createHashedPassword"

        hashed_password= PasswordEncryptionHelper.createStoragePassword(credential, PasswordEncryptionMethod.HASH_METHOD_PKCS5S2)

        return hashed_password

    def compareHashedPasswords(self, credential, storedCredential):
        print "Persistence extension. Method: compareHashedPasswords"
        
        auth_result = PasswordEncryptionHelper.compareCredentials(credential, storedCredential)

        return auth_result 
