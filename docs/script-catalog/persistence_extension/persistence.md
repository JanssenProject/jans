---
tags:
  - administration
  - developer
  - script-catalog
  - Hashed Passwords
  - Entry manager
  - Persistence Extension
---

# Persistence Script

By overriding the interface methods in [PersistenceType](https://github.com/JanssenProject/jans/blob/vreplace-janssen-version/jans-core/script/src/main/java/io/jans/model/custom/script/type/persistence/PersistenceType.java) inside a custom script you can

1. Load initialization data from DB or initialize services after the creation of Entry Manager. 
2. Release resources, terminate services etc. after the destruction of Entry Manager.
3. Create hashed passwords 
4. Compare hashed passwords

!!! note annotate "What is an Entry Manager?"
    The Janssen server's Peristence Layer can be  MySQL database. 
    Information about an entity (person, session, client, scripts etc) constitutes an Entry.
    The Entry Manager (CRUD operations) implementation for each type of Persistence is available in the Janssen server and the relevant Entry Manager (SQLEntryManager, etc.) is created when the server starts up. 

## Usage

The Jans-Auth server contains a [`PeristenceType`](https://github.com/JanssenProject/jans/blob/main/docs/script-catalog/persistence_extension/sample-script/PersistenceExtension.py) script.


## Hashed Passwords

Hashed passwords can be created using any method from this [enum](https://github.com/JanssenProject/jans/blob/main/jans-orm/core/src/main/java/io/jans/orm/operation/auth/PasswordEncryptionMethod.java), instead of the native/default SSHA256.
The ORM module of the Janssen server does the following: 

1. When User entry is persisted and `userPassword` is specified, ORM calls `createHashedPassword`
2. User authenticates and the password is checked, the ORM module invokes `compareHashedPasswords` 
3. We need to specify which one to use in `/etc/gluu/conf/jans-sql.properties`
    ```text
    password.encryption.method: SSHA-256
    ```
4. Implementation `createHashedPassword` and `compareHashedPasswords` the script: 
    - Creation
    ```
        def createHashedPassword(self, credential):
            hashed_password= PasswordEncryptionHelper.createStoragePassword(credential, PasswordEncryptionMethod.HASH_METHOD_PKCS5S2)
            return hashed_password
    ```
    
    - Comparing Hashed Password:
    ```
        def compareHashedPasswords(self, credential, storedCredential):
            auth_result = PasswordEncryptionHelper.compareCredentials(credential, storedCredential)
            return auth_result 
    ```
## Script Type: Python

### Retrieve Grant, Session and User Details from Access Token

Following sample code snippet shows how to work backwards from an AccessToken to Grant, Session and User information.
```python
from io.jans.service.cdi.util import CdiUtil
from io.jans.model.custom.script.type.persistence import PersistenceType
from io.jans.util import StringHelper
from io.jans.persist.operation.auth import PasswordEncryptionHelper
from io.jans.persist.operation.auth import PasswordEncryptionMethod

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

```