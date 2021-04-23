We will refer _Menu-driven Interactive Mode_ as **IM**. To run IM just simply execute jans-cli as follows:
```
/opt/jans/jans-cli/config-cli.py
```
You will see the main menu as below:

![jans-cli Main Menu](img/im-main.png)

Using IM is very simple and intuitive. Just make a selection and answer questions. There is a special option to read value for a question from a file using **_file** tag. This option is the most suitable choice to input the large text (e.g., interception script source code or JSON file content). To use this option input **_file /path/of/file** e.g. **_file /home/user/interceptionscript.py**. From the following menu, you can choose an option by selecting its number. Let's start from the beginning.

### Tips(im)
1. `_` is an escape character for IM mode. For example, you can create a list `["me", "you"]` by entering `me_,you`
2. `_true` means boolean **True** instead of string `"true"`, similarly `_false` is boolean **False** instead of string `"false"`
3. `_null` is comprehended as **None** (or in json **null**)
4. `_x` exits the current process and go back to the parent menu
5. `_q` refers to `quit`


### Attribute

Using **Janssen CLI**, You can perform some quick operations in _Attribute_. Such as:
- `view/find` list of Attributes in detail.
- `add` new attributes.
- `update` an existing attribute
- `delete` an attribute using its `inum` etc.

For example, to get all attributes to choose 1 from the Main Menu, you will get the following options:

![jans-cli Attributes Menu](img/im-attributes-main.png)

To `View/Find` attributes choose 1, you will be asked to enter `Search size`,  `Search pattern` and `Status of the attribute` . For simplicity, leave defaults in our case:

![jans-cli Attributes Get All](img/im-attributes-get-all.png)

Once press Enter, it will retrieve 50 attributes and prints to screen in green color:

![jans-cli Attributes Get All Response](img/im-attributes-get-all-response.png)

You can save the result as a file by choosing `w` in the result screen. To go back enter `b`, to quit enter `q`. If you enter a recognized command, it will display valid command.

To `Add` a new attribute, choose 2 (on the Attribute screen). Then enter a value for each type of attribute item, after then it will ask to `continue?` enter `y` to continue. If everything is filled in the right way, it will create a new attribute on the list.
You can go with to add a new attribute quickly:
```text
Obtained Data:
{
  "dn": "ou=attributes,o=jans",
  "inum": null,
  "selected": false,
  "name": "testAttrb",
  "displayName": "test Attribute",
  "description": "testing attribute addition",
  "dataType": "STRING",
  "status": "ACTIVE",
  "lifetime": null,
  "sourceAttribute": null,
  "salt": null,
  "nameIdType": null,
  "origin": null,
  "editType": [
    "ADMIN",
    "USER"
  ],
  "viewType": [
    "ADMIN",
    "USER"
  ],
  "usageType": null,
  "claimName": null,
  "seeAlso": null,
  "saml1Uri": null,
  "saml2Uri": null,
  "urn": null,
  "scimCustomAttr": false,
  "oxMultiValuedAttribute": false,
  "custom": false,
  "requred": false,
  "attributeValidation": {
    "regexp": null,
    "minLength": null,
    "maxLength": null
  },
  "tooltip": null
}
```

![add attribute](img/im-add-attrb.png) 

To `update` an attribute, choose 3 (on the Attribute screen). It will ask `inum` of the attribute you are going to update. For example, I want to change the description for an attribute having `inum=BCA8`. 
It will retrieve current data and ask for the modification of each property, just leave defaults if you don't want to change that property.

When it comes to an end, it will display modified data and ask if you want to continue (this data just before it sends to a server)

```text
Obtained Data:

{
  "dn": "inum=BCA8,ou=attributes,o=jans",
  "inum": "BCA8",
  "selected": false,
  "name": "transientId",
  "displayName": "TransientId",
  "description": "TransientId",
  "dataType": "STRING",
  "status": "ACTIVE",
  "lifetime": null,
  "sourceAttribute": null,
  "salt": null,
  "nameIdType": null,
  "origin": "jansPerson",
  "editType": [
    "USER",
    "ADMIN"
  ],
  "viewType": [
    "USER",
    "ADMIN"
  ],
  "usageType": null,
  "claimName": null,
  "seeAlso": null,
  "saml1Uri": "urn:mace:dir:attribute-def:transientId",
  "saml2Uri": "urn:oid:1.3.6.1.4.1.48710.1.3.312",
  "urn": "mace:shibboleth:1.0:nameIdentifier",
  "scimCustomAttr": false,
  "oxMultiValuedAttribute": false,
  "custom": false,
  "requred": false,
  "attributeValidation": {
    "regexp": null,
    "minLength": null,
    "maxLength": null
  },
  "tooltip": null
}
```
![jans-cli Attributes Update](img/im-attributes-update.png)

Enter `y` to perform an `update` and display updated data on the screen.

To update property of an attribute, you can go with partial updates from Attribute Menu. To update partially, you must be asked to enter `inum`, `op`, `path` and `value`.
- **__inum__** identity of an attribute where operation to be done.
- **__op__** means operation to be done: [`replace`, `move`, `add`, `remove`, `copy`, `test`]
- **__path__** chose path where operation will be performed: for example `attributeValidation/minLength`
- **__value__** value that you want update. It can be integer, boolean or string.

![partially update attribute](img/im-partial-attrb-update.png)

Finally, it will display the updated result.

```text
Getting access token for scope https://jans.io/oauth/config/attributes.write
Please wait for patching...

{
  "dn": "inum=BCA8,ou=attributes,o=jans",
  "inum": "BCA8",
  "selected": true,
  "name": "transientId",
  "displayName": "TransientId",
  "description": "TransientId",
  "dataType": "STRING",
  "status": "ACTIVE",
  "lifetime": null,
  "sourceAttribute": null,
  "salt": null,
  "nameIdType": null,
  "origin": "jansPerson",
  "editType": [
    "USER",
    "ADMIN"
  ],
  "viewType": [
    "USER",
    "ADMIN"
  ],
  "usageType": null,
  "claimName": null,
  "seeAlso": null,
  "saml1Uri": "urn:mace:dir:attribute-def:transientId",
  "saml2Uri": "urn:oid:1.3.6.1.4.1.48710.1.3.312",
  "urn": "mace:shibboleth:1.0:nameIdentifier",
  "scimCustomAttr": false,
  "oxMultiValuedAttribute": false,
  "custom": false,
  "requred": false,
  "attributeValidation": {
    "regexp": null,
    "minLength": null,
    "maxLength": null
  },
  "tooltip": null
}
```

To `delete` an attribute, choose option 5 from Attribute Menu. Enter the `inum` value that you want to delete. Here I chose that I already created in the above: `"inum=0adfeb80-cb57-4f7b-a3a0-944082e4c199"` It will ask for confirmation, enter `y` to confirm.

![delete attribute](img/im-delete-attrb.png)

### Authentication Method
Sometimes It's getting hard to change **Default Authentication Method** from a web browser if you can't log in using the web interface. Here Janssen CLI is going to help you a lot. 

![default-auth](img/im-default-auth.png)

- `View` Default Authentication Method.
- `Update` Default Authentication Method.

Select option 2 from Main Menu to chose Authentication Method. You can see such options as listed above.

To `View` default authentication method select '1' from Authentication Menu, It will show you the current default authentication method of the Janssen server.

![current-default-auth](img/im-cur-default-auth.png)


To `update` the default authentication method select '2', then enter the default authentication method that you want to update with it. It will ask for the confirmation, `y` to confirm. 

![update-auth](img/im-update-default-auth.png)


### Cache Configuration
In the following Main Menu, Options `3, 4, 5, 6 & 7` are for **Cache Configuration**. 
- Cache Configuration
- Cache Configuration – Memcached
- Cache Configuration – Redis
- Cache Configuration – in-Memory
- Cache Configuration – Native-Persistence

Select option 3 to enter in _Cache Configuration_ menu. 
You will get two options as below:

```text
Cache Configuration
-------------------
1 Returns cache configuration
2 Partially modifies cache configuration
```
If you want to view `cache configuration` then choose option 1
from Cache Configuration Menu. It will return cache configuration in details as below:
```json5
{
  "cacheProviderType": "NATIVE_PERSISTENCE",
  "memcachedConfiguration": {
    "servers": "localhost:11211",
    "maxOperationQueueLength": 100000,
    "bufferSize": 32768,
    "defaultPutExpiration": 60,
    "connectionFactoryType": "DEFAULT"
  },
  "redisConfiguration": {
    "redisProviderType": "STANDALONE",
    "servers": "localhost:6379",
    "password": null,
    "defaultPutExpiration": 60,
    "sentinelMasterGroupName": null,
    "useSSL": false,
    "sslTrustStoreFilePath": null,
    "maxIdleConnections": 10,
    "maxTotalConnections": 500,
    "connectionTimeout": 3000,
    "soTimeout": 3000,
    "maxRetryAttempts": 5
  },
  "inMemoryConfiguration": {
    "defaultPutExpiration": 60
  },
  "nativePersistenceConfiguration": {
    "defaultPutExpiration": 60,
    "defaultCleanupBatchSize": 10000,
    "deleteExpiredOnGetRequest": false
  }
}
```

To update partially, select option 2. then you will be asked to enter `op`, `path` and `value`.

- **__op__** means operation to be done: [`replace`, `move`, `add`, `remove`, `copy`, `test`]
- **__path__** chose path where operation will be performed: for example `memcachedConfiguration/bufferSize`
- **__value__** value that you want update. It can be integer, boolean or string.

At next it will ask `Patch another param?` you can press `y` if you want to update multiple parameters at a time otherwise `n`. After 
then it will show all the patches that are going to be performed. 

> `Continue?` 

If any mistake happens simply press `n` to abort this operation  otherwise press `y` to go with it.
It will show you the updated result. please, see below example, you will get a clear concept on this.

```text

Selection: 2

«The operation to be performed. Type: string»
op: replace

«A JSON-Pointer. Type: string»
path: memcachedConfiguration/bufferSize

«The value to be used within the operations. Type: object»
value  [{}]: 32777

Patch another param? n
[
  {
    "op": "replace",
    "path": "/memcachedConfiguration/bufferSize",
    "value": "32777"
  }
]

Continue? y
Getting access token for scope https://jans.io/oauth/config/cache.write
Please wait patching...

{
  "cacheProviderType": "NATIVE_PERSISTENCE",
  "memcachedConfiguration": {
    "servers": "localhost:11211",
    "maxOperationQueueLength": 100000,
    "bufferSize": 32777,
    "defaultPutExpiration": 60,
    "connectionFactoryType": "DEFAULT"
  },
  "redisConfiguration": {
    "redisProviderType": "STANDALONE",
    "servers": "localhost:6379",
    "password": null,
    "defaultPutExpiration": 60,
    "sentinelMasterGroupName": null,
    "useSSL": false,
    "sslTrustStoreFilePath": null,
    "maxIdleConnections": 10,
    "maxTotalConnections": 500,
    "connectionTimeout": 3000,
    "soTimeout": 3000,
    "maxRetryAttempts": 5
  },
  "inMemoryConfiguration": {
    "defaultPutExpiration": 60
  },
  "nativePersistenceConfiguration": {
    "defaultPutExpiration": 60,
    "defaultCleanupBatchSize": 10000,
    "deleteExpiredOnGetRequest": false
  }
}
```

#### Cache Configuration - Memcached

Do You want to update _Memcached_ only? you can go with this option. _Memcached_ have two options:

```text
Cache Configuration – Memcached
-------------------------------
1 Returns Memcached cache configuration
2 Updates Memcached cache configuration
```

__Option 1__ to get _memcached_ configuration. select 1, and you will get the details:

```text
Getting access token for scope https://jans.io/oauth/config/cache.readonly

{
  "servers": "localhost:11211",
  "maxOperationQueueLength": 100000,
  "bufferSize": 32777,
  "defaultPutExpiration": 60,
  "connectionFactoryType": "DEFAULT"
}
```

__Option 2__ to update _memcached_ configuration. It will ask for each parameter, enter a value or skip to set default.

```text
Selection: 2

Returns Memcached cache configuration
-------------------------------------
Please wait while retreiving data ...

Getting access token for scope https://jans.io/oauth/config/cache.readonly

«Server details separated by spaces. Type: string»
servers  [localhost:11211]: 

«Maximum operation Queue Length. Type: integer»
maxOperationQueueLength  [100000]: 

«Buffer Size. Type: integer»
bufferSize  [32777]: 

«Expiration timeout value. Type: integer»
defaultPutExpiration  [60]: 

«The MemcachedConnectionFactoryType Type. Type: string»
connectionFactoryType  [DEFAULT]: 
Obtained Data:

{
  "servers": "localhost:11211",
  "maxOperationQueueLength": 100000,
  "bufferSize": 32777,
  "defaultPutExpiration": 60,
  "connectionFactoryType": "DEFAULT"
}

Continue? y
Getting access token for scope https://jans.io/oauth/config/cache.write
Please wait while posting data ...

{
  "servers": "localhost:11211",
  "maxOperationQueueLength": 100000,
  "bufferSize": 32777,
  "defaultPutExpiration": 60,
  "connectionFactoryType": "DEFAULT"
}
```

#### Cache Configuration - Redis

To `get/update` **redis** configuration, select option 5 to enter the menu.
```text
Cache Configuration – Redis
---------------------------
1 Returns Redis cache configuration
2 Updates Redis cache configuration
```

__Option 1__ to get redis cache configuration.

```text
Selection: 1

Returns Redis cache configuration
---------------------------------
Please wait while retreiving data ...

Getting access token for scope https://jans.io/oauth/config/cache.readonly

{
  "redisProviderType": "STANDALONE",
  "servers": "localhost:6379",
  "password": null,
  "defaultPutExpiration": 60,
  "sentinelMasterGroupName": null,
  "useSSL": false,
  "sslTrustStoreFilePath": null,
  "maxIdleConnections": 10,
  "maxTotalConnections": 500,
  "connectionTimeout": 3000,
  "soTimeout": 3000,
  "maxRetryAttempts": 5
}
```

__Option 2__ to update the Redis cache configuration. You can fill each property or keep as empty to set default. 
> `Continue?` press `y` to update the Redis configuration.

```text
Selection: 2

Returns Redis cache configuration
---------------------------------
Please wait while retreiving data ...

Getting access token for scope https://jans.io/oauth/config/cache.readonly

«Type of connection. Type: string»
redisProviderType  [STANDALONE]: 

«server details separated by comma e.g. 'server1:8080server2:8081'. Type: string»
servers  [localhost:6379]: 

«Redis password. Type: string»
password: 

«defaultPutExpiration timeout value. Type: integer»
defaultPutExpiration  [60]: 

«Sentinel Master Group Name (required if SENTINEL type of connection is selected). Type: string»
sentinelMasterGroupName: 

«Enable SSL communication between Gluu Server and Redis cache. Type: boolean»
useSSL  [false]: 

«Directory Path to Trust Store. Type: string»
sslTrustStoreFilePath: 

«The cap on the number of \idle\ instances in the pool. If max idle is set too low on heavily loaded systems it is possible you will see objects being destroyed and almost immediately new objects being created. This is a result of the active threads momentarily returning objects faster than they are requesting them causing the number of idle objects to rise above max idle. The best value for max idle for heavily loaded system will vary but the default is a good starting point. Type: integer»
maxIdleConnections  [10]: 

«The number of maximum connection instances in the pool. Type: integer»
maxTotalConnections  [500]: 

«Connection time out. Type: integer»
connectionTimeout  [3000]: 

«With this option set to a non-zero timeout a read() call on the InputStream associated with this Socket will block for only this amount of time. If the timeout expires a java.net.SocketTimeoutException is raised though the Socket is still valid. The option must be enabled prior to entering the blocking operation to have effect. The timeout must be > 0. A timeout of zero is interpreted as an infinite timeout. Type: integer»
soTimeout  [3000]: 

«Maximum retry attempts in case of failure. Type: integer»
maxRetryAttempts  [5]: 
Obtained Data:

{
  "redisProviderType": "STANDALONE",
  "servers": "localhost:6379",
  "password": null,
  "defaultPutExpiration": 60,
  "sentinelMasterGroupName": null,
  "useSSL": false,
  "sslTrustStoreFilePath": null,
  "maxIdleConnections": 10,
  "maxTotalConnections": 500,
  "connectionTimeout": 3000,
  "soTimeout": 3000,
  "maxRetryAttempts": 5
}

Continue? y
Getting access token for scope https://jans.io/oauth/config/cache.write
Please wait while posting data ...

{
  "redisProviderType": "STANDALONE",
  "servers": "localhost:6379",
  "password": null,
  "defaultPutExpiration": 60,
  "sentinelMasterGroupName": null,
  "useSSL": false,
  "sslTrustStoreFilePath": null,
  "maxIdleConnections": 10,
  "maxTotalConnections": 500,
  "connectionTimeout": 3000,
  "soTimeout": 3000,
  "maxRetryAttempts": 5
}
```

#### Cache Configuration - In-Memory

To enter `In-Memory` menu select option 6, you will get two options as below:
```text
Cache Configuration – in-Memory
-------------------------------
1 Returns in-Memory cache configuration
2 Updates in-Memory cache configuration
```

__Option 1__ to get the information of In-Memory cache configuration:

```text
Selection: 1

Returns in-Memory cache configuration
-------------------------------------
Please wait while retreiving data ...

Getting access token for scope https://jans.io/oauth/config/cache.readonly

{
  "defaultPutExpiration": 60
}
```

__Option 2__ to update the information of In-Memory cache configuration:

```text
Selection: 2

Returns in-Memory cache configuration
-------------------------------------
Please wait while retreiving data ...

Getting access token for scope https://jans.io/oauth/config/cache.readonly

«defaultPutExpiration timeout value. Type: integer»
defaultPutExpiration  [60]: 
Obtained Data:

{
  "defaultPutExpiration": 60
}
```

#### Cache Configuration - Native-Persistence

```text
Cache Configuration – Native-Persistence
----------------------------------------
1 Returns native persistence cache configuration
2 Updates native persistence cache configuration
```

__Option 1__ to get the information of native persistence cache configuration.
```text
Selection: 1

Returns native persistence cache configuration
----------------------------------------------
Please wait while retreiving data ...

Getting access token for scope https://jans.io/oauth/config/cache.readonly

{
  "defaultPutExpiration": 60,
  "defaultCleanupBatchSize": 10000,
  "deleteExpiredOnGetRequest": false
}
```

__Option 2__ to update the information of native persistence cache configuration.

```text
Selection: 2

Returns native persistence cache configuration
----------------------------------------------
Please wait while retreiving data ...

Getting access token for scope https://jans.io/oauth/config/cache.readonly

«defaultPutExpiration timeout value. Type: integer»
defaultPutExpiration  [60]: 

«defaultCleanupBatchSize page size. Type: integer»
defaultCleanupBatchSize  [10000]: 

«Type: boolean»
deleteExpiredOnGetRequest  [false]: 
Obtained Data:

{
  "defaultPutExpiration": 60,
  "defaultCleanupBatchSize": 10000,
  "deleteExpiredOnGetRequest": false
}

Continue? y
Getting access token for scope https://jans.io/oauth/config/cache.write
Please wait while posting data ...

{
  "defaultPutExpiration": 60,
  "defaultCleanupBatchSize": 10000,
  "deleteExpiredOnGetRequest": false
}
```


### Jans Authorization Server

From the Main Menu choose option 8 to `get/modify` Jans authorization server configuration properties.
```text
Configuration – Properties
--------------------------
1 Gets all Jans authorization server configuration properties
2 Partially modifies Jans authorization server Application configuration properties
```

Select 1 to get all the details about Jans authorization server configuration. It will show all the properties as below:
```json5
{
  "issuer": "https://testjans.gluu.com",
  "baseEndpoint": "https://testjans.gluu.com/jans-auth/restv1",
  "authorizationEndpoint": "https://testjans.gluu.com/jans-auth/restv1/authorize",
  "tokenEndpoint": "https://testjans.gluu.com/jans-auth/restv1/token",
  "tokenRevocationEndpoint": "https://testjans.gluu.com/jans-auth/restv1/revoke",
  "userInfoEndpoint": "https://testjans.gluu.com/jans-auth/restv1/userinfo",
  "clientInfoEndpoint": "https://testjans.gluu.com/jans-auth/restv1/clientinfo",
  "checkSessionIFrame": "https://testjans.gluu.com/jans-auth/opiframe.htm",
  "endSessionEndpoint": "https://testjans.gluu.com/jans-auth/restv1/end_session",
  "jwksUri": "https://testjans.gluu.com/jans-auth/restv1/jwks",
  "registrationEndpoint": "https://testjans.gluu.com/jans-auth/restv1/register",
  "openIdDiscoveryEndpoint": "https://testjans.gluu.com/.well-known/webfinger",
  "openIdConfigurationEndpoint": "https://testjans.gluu.com/.well-known/openid-configuration",
  "idGenerationEndpoint": "https://testjans.gluu.com/jans-auth/restv1/id",
  "introspectionEndpoint": "https://testjans.gluu.com/jans-auth/restv1/introspection",
  "deviceAuthzEndpoint": "https://testjans.gluu.com/jans-auth/restv1/device_authorization",
  "sessionAsJwt": false,
  "sectorIdentifierCacheLifetimeInMinutes": 1440,
  "umaConfigurationEndpoint": "https://testjans.gluu.com/jans-auth/restv1/uma2-configuration",
  "umaRptAsJwt": false,
  "umaRptLifetime": 3600,
  "umaTicketLifetime": 3600,
  "umaPctLifetime": 2592000,
  "umaResourceLifetime": 2592000,
  "umaAddScopesAutomatically": true,
  "umaValidateClaimToken": false,
  "umaGrantAccessIfNoPolicies": false,
  "umaRestrictResourceToAssociatedClient": false,
  "spontaneousScopeLifetime": 86400,
  "openidSubAttribute": "inum",
  "responseTypesSupported": [
    "['token', 'code']",
    "['id_token']",
    "['token']",
    "['id_token', 'code']",
    "['id_token', 'token', 'code']",
    "['code']",
    "['id_token', 'token']"
  ],
  "responseModesSupported": [
    "query",
    "form_post",
    "fragment"
  ],
  "grantTypesSupported": [
    "password",
    "client_credentials",
    "refresh_token",
    "urn:ietf:params:oauth:grant-type:uma-ticket",
    "urn:ietf:params:oauth:grant-type:device_code",
    "implicit",
    "authorization_code"
  ],
  "subjectTypesSupported": [
    "public",
    "pairwise"
  ],
  "defaultSubjectType": [
    "p",
    "a",
    "i",
    "r",
    "w",
    "i",
    "s",
    "e"
  ],
  "userInfoSigningAlgValuesSupported": [
    "HS256",
    "HS384",
    "HS512",
    "RS256",
    "RS384",
    "RS512",
    "ES256",
    "ES384",
    "ES512"
  ],
  "userInfoEncryptionAlgValuesSupported": [
    "RSA1_5",
    "RSA-OAEP",
    "A128KW",
    "A256KW"
  ],
  "userInfoEncryptionEncValuesSupported": [
    "A128CBC+HS256",
    "A256CBC+HS512",
    "A128GCM",
    "A256GCM"
  ],
  "idTokenSigningAlgValuesSupported": [
    "none",
    "HS256",
    "HS384",
    "HS512",
    "RS256",
    "RS384",
    "RS512",
    "ES256",
    "ES384",
    "ES512"
  ],
  "idTokenEncryptionAlgValuesSupported": [
    "RSA1_5",
    "RSA-OAEP",
    "A128KW",
    "A256KW"
  ],
  "idTokenEncryptionEncValuesSupported": [
    "A128CBC+HS256",
    "A256CBC+HS512",
    "A128GCM",
    "A256GCM"
  ],
  "requestObjectSigningAlgValuesSupported": [
    "none",
    "HS256",
    "HS384",
    "HS512",
    "RS256",
    "RS384",
    "RS512",
    "ES256",
    "ES384",
    "ES512"
  ],
  "requestObjectEncryptionAlgValuesSupported": [
    "RSA1_5",
    "RSA-OAEP",
    "A128KW",
    "A256KW"
  ],
  "requestObjectEncryptionEncValuesSupported": [
    "A128CBC+HS256",
    "A256CBC+HS512",
    "A128GCM",
    "A256GCM"
  ],
  "tokenEndpointAuthMethodsSupported": [
    "client_secret_basic",
    "client_secret_post",
    "client_secret_jwt",
    "private_key_jwt",
    "tls_client_auth",
    "self_signed_tls_client_auth"
  ],
  "tokenEndpointAuthSigningAlgValuesSupported": [
    "HS256",
    "HS384",
    "HS512",
    "RS256",
    "RS384",
    "RS512",
    "ES256",
    "ES384",
    "ES512"
  ],
  "dynamicRegistrationCustomAttributes": null,
  "displayValuesSupported": [
    "page",
    "popup"
  ],
  "claimTypesSupported": [
    "normal"
  ],
  "jwksAlgorithmsSupported": [
    "RS256",
    "RS384",
    "RS512",
    "ES256",
    "ES384",
    "ES512",
    "PS256",
    "PS384",
    "PS512",
    "RSA1_5",
    "RSA-OAEP"
  ],
  "serviceDocumentation": [
    "h",
    "t",
    "t",
    "p",
    ":",
    "/",
    "/",
    "j",
    "a",
    "n",
    "s",
    ".",
    "o",
    "r",
    "g",
    "/",
    "d",
    "o",
    "c",
    "s"
  ],
  "claimsLocalesSupported": [
    "en"
  ],
  "idTokenTokenBindingCnfValuesSupported": [
    "tbh"
  ],
  "uiLocalesSupported": [
    "en",
    "bg",
    "de",
    "es",
    "fr",
    "it",
    "ru",
    "tr"
  ],
  "claimsParameterSupported": false,
  "requestParameterSupported": true,
  "requestUriParameterSupported": true,
  "requestUriHashVerificationEnabled": false,
  "requireRequestUriRegistration": false,
  "opPolicyUri": "http://www.jans.io/doku.php?id=jans:policy",
  "opTosUri": "http://www.jans.io/doku.php?id=jans:tos",
  "authorizationCodeLifetime": 60,
  "refreshTokenLifetime": 14400,
  "idTokenLifetime": 3600,
  "idTokenFilterClaimsBasedOnAccessToken": false,
  "accessTokenLifetime": 300,
  "cleanServiceInterval": 60,
  "cleanServiceBatchChunkSize": 10000,
  "cleanServiceBaseDns": null,
  "keyRegenerationEnabled": true,
  "keyRegenerationInterval": 48,
  "defaultSignatureAlgorithm": [
    "R",
    "S",
    "2",
    "5",
    "6"
  ],
  "oxOpenIdConnectVersion": "openidconnect-1.0",
  "oxId": "https://testjans.gluu.com/oxid/service/jans/inum",
  "dynamicRegistrationEnabled": true,
  "dynamicRegistrationExpirationTime": -1,
  "dynamicRegistrationPersistClientAuthorizations": true,
  "trustedClientEnabled": true,
  "skipAuthorizationForOpenIdScopeAndPairwiseId": false,
  "dynamicRegistrationScopesParamEnabled": true,
  "dynamicRegistrationPasswordGrantTypeEnabled": false,
  "dynamicRegistrationAllowedPasswordGrantScopes": null,
  "dynamicRegistrationCustomObjectClass": null,
  "personCustomObjectClassList": [
    "jansCustomPerson",
    "jansPerson"
  ],
  "persistIdTokenInLdap": false,
  "persistRefreshTokenInLdap": true,
  "allowPostLogoutRedirectWithoutValidation": false,
  "invalidateSessionCookiesAfterAuthorizationFlow": false,
  "returnClientSecretOnRead": true,
  "rejectJwtWithNoneAlg": true,
  "expirationNotificatorEnabled": false,
  "useNestedJwtDuringEncryption": true,
  "expirationNotificatorMapSizeLimit": 100000,
  "expirationNotificatorIntervalInSeconds": 600,
  "authenticationFiltersEnabled": false,
  "clientAuthenticationFiltersEnabled": false,
  "clientRegDefaultToCodeFlowWithRefresh": true,
  "authenticationFilters": [
    {
      "filter": "(&(mail=*{0}*)(inum={1}))",
      "bind": false,
      "bind-password-attribute": null,
      "base-dn": null
    },
    {
      "filter": "uid={0}",
      "bind": true,
      "bind-password-attribute": null,
      "base-dn": null
    }
  ],
  "clientAuthenticationFilters": [
    {
      "filter": "myCustomAttr1={0}",
      "bind": null,
      "bind-password-attribute": null,
      "base-dn": null
    }
  ],
  "corsConfigurationFilters": [
    {
      "filterName": "CorsFilter",
      "corsEnabled": true,
      "corsAllowedOrigins": "*",
      "corsAllowedMethods": "GET,POST,HEAD,OPTIONS",
      "corsAllowedHeaders": "Origin,Authorization,Accept,X-Requested-With,Content-Type,Access-Control-Request-Method,Access-Control-Request-Headers",
      "corsExposedHeaders": null,
      "corsSupportCredentials": true,
      "corsLoggingEnabled": false,
      "corsPreflightMaxAge": 1800,
      "corsRequestDecorate": true
    }
  ],
  "sessionIdUnusedLifetime": 86400,
  "sessionIdUnauthenticatedUnusedLifetime": 120,
  "sessionIdEnabled": true,
  "sessionIdPersistOnPromptNone": true,
  "sessionIdRequestParameterEnabled": false,
  "changeSessionIdOnAuthentication": true,
  "sessionIdPersistInCache": false,
  "sessionIdLifetime": 86400,
  "serverSessionIdLifetime": 86400,
  "configurationUpdateInterval": 3600,
  "enableClientGrantTypeUpdate": true,
  "dynamicGrantTypeDefault": [
    "client_credentials",
    "refresh_token",
    "urn:ietf:params:oauth:grant-type:uma-ticket",
    "urn:ietf:params:oauth:grant-type:device_code",
    "implicit",
    "authorization_code"
  ],
  "cssLocation": null,
  "jsLocation": null,
  "imgLocation": null,
  "metricReporterInterval": 300,
  "metricReporterKeepDataDays": 15,
  "metricReporterEnabled": false,
  "pairwiseIdType": [
    "a",
    "l",
    "g",
    "o",
    "r",
    "i",
    "t",
    "h",
    "m",
    "i",
    "c"
  ],
  "pairwiseCalculationKey": "sckNNuFhwz3r2fC4xLLlBeVybFw",
  "pairwiseCalculationSalt": "USZej6vS3pI7RzFIl3AT",
  "shareSubjectIdBetweenClientsWithSameSectorId": true,
  "webKeysStorage": "keystore",
  "dnName": "CN=Jans Auth CA Certificates",
  "keyStoreFile": "/etc/certs/jans-auth-keys.jks",
  "keyStoreSecret": "0EIsfpb6tURD",
  "keySelectionStrategy": "OLDER",
  "oxElevenTestModeToken": null,
  "oxElevenGenerateKeyEndpoint": "https://testjans.gluu.com/oxeleven/rest/oxeleven/generateKey",
  "oxElevenSignEndpoint": "https://testjans.gluu.com/oxeleven/rest/oxeleven/sign",
  "oxElevenVerifySignatureEndpoint": "https://testjans.gluu.com/oxeleven/rest/oxeleven/verifySignature",
  "oxElevenDeleteKeyEndpoint": "https://testjans.gluu.com/oxeleven/rest/oxeleven/deleteKey",
  "introspectionAccessTokenMustHaveUmaProtectionScope": false,
  "endSessionWithAccessToken": false,
  "cookieDomain": null,
  "enabledOAuthAuditLogging": null,
  "jmsBrokerURISet": null,
  "jmsUserName": null,
  "jmsPassword": null,
  "clientWhiteList": [
    "*"
  ],
  "clientBlackList": [
    "*.attacker.com/*"
  ],
  "legacyIdTokenClaims": false,
  "customHeadersWithAuthorizationResponse": true,
  "frontChannelLogoutSessionSupported": true,
  "loggingLevel": "INFO",
  "loggingLayout": "text",
  "updateUserLastLogonTime": false,
  "updateClientAccessTime": false,
  "logClientIdOnClientAuthentication": true,
  "logClientNameOnClientAuthentication": false,
  "disableJdkLogger": true,
  "authorizationRequestCustomAllowedParameters": [
    "customParam2",
    "customParam3",
    "customParam1"
  ],
  "legacyDynamicRegistrationScopeParam": false,
  "openidScopeBackwardCompatibility": false,
  "disableU2fEndpoint": false,
  "useLocalCache": true,
  "fapiCompatibility": false,
  "forceIdTokenHintPrecense": false,
  "forceOfflineAccessScopeToEnableRefreshToken": true,
  "errorReasonEnabled": false,
  "removeRefreshTokensForClientOnLogout": true,
  "skipRefreshTokenDuringRefreshing": false,
  "refreshTokenExtendLifetimeOnRotation": false,
  "consentGatheringScriptBackwardCompatibility": false,
  "introspectionScriptBackwardCompatibility": false,
  "introspectionResponseScopesBackwardCompatibility": false,
  "softwareStatementValidationType": "script",
  "softwareStatementValidationClaimName": null,
  "authenticationProtectionConfiguration": {
    "attemptExpiration": 15,
    "maximumAllowedAttemptsWithoutDelay": 4,
    "delayTime": 2,
    "bruteForceProtectionEnabled": false
  },
  "errorHandlingMethod": "internal",
  "keepAuthenticatorAttributesOnAcrChange": false,
  "deviceAuthzRequestExpiresIn": 1800,
  "deviceAuthzTokenPollInterval": 5,
  "deviceAuthzResponseTypeToProcessAuthz": "code",
  "backchannelClientId": null,
  "backchannelRedirectUri": "https://testjans.gluu.com/jans-auth/ciba/home.htm",
  "backchannelAuthenticationEndpoint": "https://testjans.gluu.com/jans-auth/restv1/bc-authorize",
  "backchannelDeviceRegistrationEndpoint": "https://testjans.gluu.com/jans-auth/restv1/bc-deviceRegistration",
  "backchannelTokenDeliveryModesSupported": [
    "poll",
    "ping",
    "push"
  ],
  "backchannelAuthenticationRequestSigningAlgValuesSupported": null,
  "backchannelUserCodeParameterSupported": false,
  "backchannelBindingMessagePattern": "^[a-zA-Z0-9]{4,8}$",
  "backchannelAuthenticationResponseExpiresIn": 3600,
  "backchannelAuthenticationResponseInterval": 2,
  "backchannelLoginHintClaims": [
    "inum",
    "uid",
    "mail"
  ],
  "cibaEndUserNotificationConfig": {
    "apiKey": null,
    "authDomain": null,
    "databaseURL": null,
    "projectId": null,
    "storageBucket": null,
    "messagingSenderId": null,
    "appId": null,
    "notificationUrl": null,
    "notificationKey": null,
    "publicVapidKey": null
  },
  "backchannelRequestsProcessorJobIntervalSec": 5,
  "backchannelRequestsProcessorJobChunkSize": 100,
  "cibaGrantLifeExtraTimeSec": 180,
  "cibaMaxExpirationTimeAllowedSec": 1800,
  "cibaEnabled": false,
  "discoveryCacheLifetimeInMinutes": 60,
  "httpLoggingEnabled": false,
  "httpLoggingExludePaths": null,
  "externalLoggerConfiguration": null
}
```
By selecting the 2nd option, you can modify its properties partially. 

![update jans authorization server](img/im-update-jans-auth.png)

At the end, it will show the updated result.

### Janssen FIDO2
Janssen includes a FIDO2 component to implement a two-step, two-factor authentication (2FA) with a username/password as the first step, and any FIDO2 device as the second step. During Janssen installation, the administrator will have an option to install the FIDO2 component.

Using Janssen CLI, you can `view/update` details of the FIDO2 configuration.
From the main menu select option 9, you will get two options.

```text
Configuration – Fido2
---------------------
1 Gets Jans Authorization Server Fido2 configuration properties
2 Updates Fido2 configuration properties
```
If you chose the first option, You will get some details of fido2 configuration properties:
```json5
{
  "issuer": "https://testjans.gluu.com",
  "baseEndpoint": "https://testjans.gluu.com/fido2/restv1",
  "cleanServiceInterval": 60,
  "cleanServiceBatchChunkSize": 10000,
  "useLocalCache": true,
  "disableJdkLogger": true,
  "loggingLevel": "INFO",
  "loggingLayout": "text",
  "externalLoggerConfiguration": "",
  "metricReporterInterval": 300,
  "metricReporterKeepDataDays": 15,
  "metricReporterEnabled": true,
  "personCustomObjectClassList": [
    "jansCustomPerson",
    "jansPerson"
  ],
  "fido2Configuration": {
    "authenticatorCertsFolder": "/etc/jans/conf/fido2/authenticator_cert",
    "mdsCertsFolder": "/etc/jans/conf/fido2/mds/cert",
    "mdsTocsFolder": "/etc/jans/conf/fido2/mds/toc",
    "serverMetadataFolder": "/etc/jans/conf/fido2/server_metadata",
    "requestedParties": [
      {
        "name": "https://testjans.gluu.com",
        "domains": [
          "testjans.gluu.com"
        ]
      }
    ],
    "userAutoEnrollment": false,
    "unfinishedRequestExpiration": 180,
    "authenticationHistoryExpiration": 1296000,
    "requestedCredentialTypes": [
      "RS256",
      "ES256"
    ]
  }
}

```
If you want to update the fido2 configuration, you can choose the 2nd option. It will ask to fill each property, skip for default values. 
For example, if you want to change **_logginglevel_** `INFO` to `DEBUG`, simply enter **DEBUG** when it will ask to enter a value.

![update fido2 configuration](img/im-update-fido2.png)

> **__Add RequestedParties?__**; If you want to add any requested domains then enter `y`, it will ask `name` and `domains` information of requested parties. Otherwise, enter `n` to skip.

```text
Continue? y
Getting access token for scope https://jans.io/oauth/config/fido2.write
Please wait while posting data ...

{
  "issuer": "https://testjans.gluu.com",
  "baseEndpoint": "https://testjans.gluu.com/fido2/restv1",
  "cleanServiceInterval": 60,
  "cleanServiceBatchChunkSize": 10000,
  "useLocalCache": false,
  "disableJdkLogger": false,
  "loggingLevel": "DEBUG",
  "loggingLayout": "text",
  "externalLoggerConfiguration": null,
  "metricReporterInterval": 300,
  "metricReporterKeepDataDays": 15,
  "metricReporterEnabled": false,
  "personCustomObjectClassList": [
    "jansCustomPerson",
    "jansPerson"
  ],
  "fido2Configuration": {
    "authenticatorCertsFolder": null,
    "mdsCertsFolder": null,
    "mdsTocsFolder": null,
    "serverMetadataFolder": null,
    "requestedParties": [
      {
        "name": null,
        "domains": []
      }
    ],
    "userAutoEnrollment": false,
    "unfinishedRequestExpiration": null,
    "authenticationHistoryExpiration": null,
    "requestedCredentialTypes": []
  }
}
```

### SMTP Server Configuration
Janssen CLI also supports SMTP configuration. You can do the following things as stated below:
- `View/Get`
- `Add/Delete`
- `Update`
- `Test`

Simply select option '10' from Main Menu, It will show some options as below:
```text
Configuration – SMTP
--------------------
1 Returns SMTP server configuration
2 Adds SMTP server configuration
3 Updates SMTP server configuration
4 Deletes SMTP server configuration
5 Test SMTP server configuration
```
Just go with the option and perform operation.

- **__view / find__** : select option 1, it will return as below:

```text
Returns SMTP server configuration
---------------------------------
Please wait while retreiving data ...

Getting access token for scope https://jans.io/oauth/config/smtp.readonly

{
  "host": null,
  "port": 0,
  "requiresSsl": null,
  "serverTrust": null,
  "fromName": null,
  "fromEmailAddress": null,
  "requiresAuthentication": null,
  "userName": null,
  "password": null
}
```
- **__Add SMTP Server__**
To add a smtp server, chose option 2 from SMTP Configuration Menu:
  
```text
Selection: 2

«Hostname of the SMTP server. Type: string»
host: 

«Port number of the SMTP server. Type: integer»
port: 

«Boolean value with default value false. If true, SSL will be enabled. Type: boolean»
requiresSsl  [false]: 

«Boolean value with default value false. Type: boolean»
serverTrust  [false]: 

«Name of the sender. Type: string»
fromName: 

«Email Address of the Sender. Type: string»
fromEmailAddress: 

«Boolean value with default value false. It true it will enable sender authentication. Type: boolean»
requiresAuthentication  [false]: 

«Username of the SMTP. Type: string»
userName: 

«Password for the SMTP. Type: string»
password: 
Obtained Data:

{
  "host": null,
  "port": null,
  "requiresSsl": false,
  "serverTrust": false,
  "fromName": null,
  "fromEmailAddress": null,
  "requiresAuthentication": false,
  "userName": null,
  "password": null
}

Continue? 
```

Fill each property with the correct information.
- **Test SMTP Server**

If the server is running, and all the information you have entered is correct. You can test SMTP server from the following option 5, it will respond if the server is configured properly.

### Janssen Logging Configuration

Using Janssen CLI, you can easily update the logging configuration. Just go with option 11 from Main Menu, It will display two options.

```text
Configuration – Logging
-----------------------
1 Returns Jans Authorization Server logging settings
2 Updates Jans Authorization Server logging settings
```

The first option returns the current logging configuration.
```json
Returns Jans Authorization Server logging settings
--------------------------------------------------
Please wait while retreiving data ...

Getting access token for scope https://jans.io/oauth/config/logging.readonly

{
  "loggingLevel": "INFO",
  "loggingLayout": "text",
  "httpLoggingEnabled": false,
  "disableJdkLogger": true,
  "enabledOAuthAuditLogging": false,
  "externalLoggerConfiguration": null,
  "httpLoggingExludePaths": null
}
```
To update the current logging configuration select option 2. For example, I have updated `logging level INFO to DEBUG` and enabled `enabledOAuthAuditLogging`.
```json
Returns Jans Authorization Server logging settings
--------------------------------------------------
Please wait while retreiving data ...

Getting access token for scope https://jans.io/oauth/config/logging.readonly

«Logging level for Jans Authorization Server logger. Type: string»
loggingLevel [INFO]: DEBUG

«Logging layout used for Jans Authorization Server loggers. Type: string»
loggingLayout [text]: 

«To enable http request/response logging. Type: boolean»
httpLoggingEnabled [false]: 

«To enable/disable Jdk logging. Type: boolean»
disableJdkLogger [true]: 

«To enable/disable OAuth audit logging. Type: boolean»
enabledOAuthAuditLogging [false]: true
Please enter a(n) boolean value: _true, _false
enabledOAuthAuditLogging [false]: _true

«Path to external log4j2 configuration file. Type: string»
externalLoggerConfiguration: 

«List of paths to exclude from logger. Type: array of string separated by _,»
Example: /auth/img, /auth/stylesheet
httpLoggingExludePaths: 
Obtained Data:

{
  "loggingLevel": "DEBUG",
  "loggingLayout": "text",
  "httpLoggingEnabled": false,
  "disableJdkLogger": true,
  "enabledOAuthAuditLogging": true,
  "externalLoggerConfiguration": null,
  "httpLoggingExludePaths": null
}

Continue? y
Getting access token for scope https://jans.io/oauth/config/logging.write
Please wait while posting data ...

{
  "loggingLevel": "DEBUG",
  "loggingLayout": "text",
  "httpLoggingEnabled": false,
  "disableJdkLogger": true,
  "enabledOAuthAuditLogging": true,
  "externalLoggerConfiguration": null,
  "httpLoggingExludePaths": null
}

```

### JSON Web Key 
This operation is used to get the JSON Web Key Set (JWKS) from OP host. The JWKS is a set of keys containing the public 
keys that should be used to verify any JSON Web Token (JWT) issued by the authorization server.
From the Main Menu, select option 12, It returns some options as stated below:

```text
Configuration – JWK - JSON Web Key (JWK)
----------------------------------------
1 Gets a list of JSON Web Key (JWK) used by a server
2 Puts/replaces JWKS
3 Patch JWKS
```
You can `view` the list of JSON Web Key, `add/replace` and `patch` using Janssen CLI.

- **__`Get list of JSON Web Key`__**

Select option 1 from JSON Web Key Menu and it will return a list of key with details information as below:
  
```text
Gets list of JSON Web Key (JWK) used by server
Gets list of JSON Web Key (JWK) used by server. JWK is a JSON data structure that represents a set of public keys as a JSON object [RFC4627].
---------------------------------------------------------------------------------------------------------------------------------------------
Please wait while retreiving data ...

Getting access token for scope https://jans.io/oauth/config/jwks.readonly

{
  "keys": [
    {
      "kid": "a1d120af-d4c1-45aa-8cff-034e00f13d2b_sig_rs256",
      "kty": "RSA",
      "use": "sig",
      "alg": "RS256",
      "crv": "",
      "exp": 1610923149000,
      "x5c": [
        "MIIDCjCCAfKgAwIBAgIhAKefzbtkilZu5nn6G1WHSbJZu/PIdKpR9U5QA58DXN6GMA0GCSqGSIb3DQEBCwUAMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjEwMTE1MjIzODU5WhcNMjEwMTE3MjIzOTA5WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAxmm58zzhORBJkyxcjyfFUrRO06V4PwDZT/ObroQOQDuN8KbOzqkGdZX6BkZiFPNHuWdnUp0/2Fxf2LM1z5nhyCG4Wy92rUqHL6ispNtPfWOe3mWwQlFJk/Z/87gqJZ00ss3vnSk+05j4AsgvnPoKZJtgJPAEjZ8+bBSNExpqWdHBFcqJJsLhyjE5o7hQFQplMevQLyVvrzxsY8YwZuoTZA+bUo7//vsrHUe/PyZP0+0FHRbFzwo+ArxrdFcFlEhTqjKijo7pyh8gmZkgvXG8D1Zi1Fmstnf9yiF36ZBlN+RSr+JHxPAvwU2O/aMmFhvZNJ9aOzP0dienSZo72xSiRwIDAQABoycwJTAjBgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAwDQYJKoZIhvcNAQELBQADggEBAGB6JFBWpIAux87xE1GL1iY+LrcxC7T6ITRb+mwhtsA0bOTx9CISNLhuFUIcOBrB+2LQD7asVvbo7I2zJ9enIR0QJbO4Z3niSCVULWeBhPACh5a+HgkpZ7mlFLJyD1hpw+pEfobasvoJLzvyuVpL/EXCgMYoi0qmrkwZfYXoajjZAhsT6Y5mTBd25xYGcatglQZutaVOgneQxZb2vjAH4h3H14EHdKPh3viXbpyXe6MP+DqX1kIqFHX3rYbhvLXdALHkRsqlcoHMW7jQuQyyfXNbwddg6H/IR0VV5yliOsoHP8BxHS9vIGHroGpZarpCwkxgsRKL+Uib1+wBN1GLu6o="
      ],
      "n": "xmm58zzhORBJkyxcjyfFUrRO06V4PwDZT_ObroQOQDuN8KbOzqkGdZX6BkZiFPNHuWdnUp0_2Fxf2LM1z5nhyCG4Wy92rUqHL6ispNtPfWOe3mWwQlFJk_Z_87gqJZ00ss3vnSk-05j4AsgvnPoKZJtgJPAEjZ8-bBSNExpqWdHBFcqJJsLhyjE5o7hQFQplMevQLyVvrzxsY8YwZuoTZA-bUo7__vsrHUe_PyZP0-0FHRbFzwo-ArxrdFcFlEhTqjKijo7pyh8gmZkgvXG8D1Zi1Fmstnf9yiF36ZBlN-RSr-JHxPAvwU2O_aMmFhvZNJ9aOzP0dienSZo72xSiRw",
      "e": "AQAB",
      "x": null,
      "y": null
    },
    {
      "kid": "5841b726-4a62-4a91-9b14-2c4e774b8187_sig_rs384",
      "kty": "RSA",
      "use": "sig",
      "alg": "RS384",
      "crv": "",
      "exp": 1610923149000,
      "x5c": [
        "MIIDCjCCAfKgAwIBAgIhAK29kWeoIZxzuN9D5Bi+TJOSkxSMyK+9O6sFHH9UG6KTMA0GCSqGSIb3DQEBDAUAMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjEwMTE1MjIzOTAwWhcNMjEwMTE3MjIzOTA5WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtegG/5p4hXBV8BhPE7bUYgCXYnwFY9J9yVNjMI306qnN1sRrTvqH88SCLg2/sY2gWI+Y8lmqXYsLbsmCoCXMUAHU6ujqrwWZsiubucyb6wmE2yWdkSgIcT1jpepnfvm4oyKnhZVqn6hOuDx+/vBNk/RJfPibBrhJp/+uiZFc86at3JIgqXB5RqV9ryXGSXpL7tj5cST2HFU+2WzoutHRze7T3XLcA0bIiiQUfHzssxElfSbrUZRY36mpoaqm2WDMEhBEwu2B1L2Jwx76LIn7dWszwaIHkqLMy7PSl3Hit0MdO7SD5bqHnMHHmSjj+9XmYBg5oErfOKJOWAevLlksgQIDAQABoycwJTAjBgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAwDQYJKoZIhvcNAQEMBQADggEBACSX/4j+sd5TGsM1e3ISJHxjDlWhvsurPQhadaDu49NdCP/9hrwo7Th48q8Q8o99DnDBOIV0AE7VORYC4xRWHXlGJV84YAQRhHi1rL8L5YWheNeR0/ibanLhaTMb4Ecw8CRJWplslKmt78bn/J1xl4cWilDTVeB+LAYrpmDJNXSx/3QHtIc2PoIKn3dE8cHhHvQ+zHmd52TxGdBR08+TqZDcwZT9XvjrOwyUkk5LIXp8Di9oqPtcDM2vqrgZna40cZAtXHzY1x6PKlwRoMSEZ+olYjjy2OlqsotORc+fbQIkLkUUnhyHTAiobZT1N55LjYkhwjXV+Ps1Qm0Q2px9uMs="
      ],
      "n": "tegG_5p4hXBV8BhPE7bUYgCXYnwFY9J9yVNjMI306qnN1sRrTvqH88SCLg2_sY2gWI-Y8lmqXYsLbsmCoCXMUAHU6ujqrwWZsiubucyb6wmE2yWdkSgIcT1jpepnfvm4oyKnhZVqn6hOuDx-_vBNk_RJfPibBrhJp_-uiZFc86at3JIgqXB5RqV9ryXGSXpL7tj5cST2HFU-2WzoutHRze7T3XLcA0bIiiQUfHzssxElfSbrUZRY36mpoaqm2WDMEhBEwu2B1L2Jwx76LIn7dWszwaIHkqLMy7PSl3Hit0MdO7SD5bqHnMHHmSjj-9XmYBg5oErfOKJOWAevLlksgQ",
      "e": "AQAB",
      "x": null,
      "y": null
    },
    {
      "kid": "71d17b7d-045b-4095-ad00-5025fa6829ec_sig_rs512",
      "kty": "RSA",
      "use": "sig",
      "alg": "RS512",
      "crv": "",
      "exp": 1610923149000,
      "x5c": [
        "MIIDCTCCAfGgAwIBAgIgBcLFz+d7BzpRRt6y8Q7tx+JHp/Mz+W7wYrJ79B879AwwDQYJKoZIhvcNAQENBQAwJDEiMCAGA1UEAwwZSmFucyBBdXRoIENBIENlcnRpZmljYXRlczAeFw0yMTAxMTUyMjM5MDBaFw0yMTAxMTcyMjM5MDlaMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQC5fSJXTly7NcBc0hUWE4+n0n9qS0dshbO4iHWEGrIcE3sWAYlz1Y1UItlS3LdkIkP92yV7d2c1Y9DEpVaanH21yIoHDjUkNtgYOUv6QMVzOb0o3P6NDZQBUTW1dXIc6XF2gfM3HVd7blcq1yqrENQKpZcqAFd6acyPvuIJq9o8w7MfEJHrpOlBWYeMarXGgzIdNd4S+1jGBpbsbROm3jUFVftjunL0sub1+tViBEk+cspleaOtA2r5oc8pC5BbkIQ4CreocOHtIBSQOXhp4iS30xiEemKcM0Me9n5A9DzzB2EHfo48qR0zjsKdV2XRVGYehwxwQTXJcmFLWJb1phQDAgMBAAGjJzAlMCMGA1UdJQQcMBoGCCsGAQUFBwMBBggrBgEFBQcDAgYEVR0lADANBgkqhkiG9w0BAQ0FAAOCAQEAnqYSP4iuwe2fZjPw3yYBk9ZR64qeQvihJZKwgQFwJ5yOo+bu8gErDisVmMUVxeR/g6RTuBBbJGwECtz2Fcpj7euapPoGGxkHEnZMAwkro0UPWztSi798tIhZoeRrZI0A9Lhk885qI2ZSE9hcD5oaDU1uRcnBFwbsoZovmLnwm+9hNJyb6WNpLO3fOnIQGAIx1P6CGShKCK/U4Q3kXrhCg5H7ceSVbHWPwMrKmBSp4dytSXimo8VCFe2a2sInKWcGC1nhBHD0vUlrmJrPo31xYkoSDFkBIszGXLrSUAj5rFFY+qurWv2izECkdMHtv8TqRC9+wFLppFgbbr/R8EZP8A=="
      ],
      "n": "uX0iV05cuzXAXNIVFhOPp9J_aktHbIWzuIh1hBqyHBN7FgGJc9WNVCLZUty3ZCJD_dsle3dnNWPQxKVWmpx9tciKBw41JDbYGDlL-kDFczm9KNz-jQ2UAVE1tXVyHOlxdoHzNx1Xe25XKtcqqxDUCqWXKgBXemnMj77iCavaPMOzHxCR66TpQVmHjGq1xoMyHTXeEvtYxgaW7G0Tpt41BVX7Y7py9LLm9frVYgRJPnLKZXmjrQNq-aHPKQuQW5CEOAq3qHDh7SAUkDl4aeIkt9MYhHpinDNDHvZ-QPQ88wdhB36OPKkdM47CnVdl0VRmHocMcEE1yXJhS1iW9aYUAw",
      "e": "AQAB",
      "x": null,
      "y": null
    },
    {
      "kid": "941699d5-7abf-4d7b-a34d-680778dbf202_sig_es256",
      "kty": "EC",
      "use": "sig",
      "alg": "ES256",
      "crv": "P-256",
      "exp": 1610923149000,
      "x5c": [
        "MIIBfjCCASSgAwIBAgIhAJEw743rfFLOZzVxJ5Y0/syaX3M2pgKQRHSiikfcKlu+MAoGCCqGSM49BAMCMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjEwMTE1MjIzOTAwWhcNMjEwMTE3MjIzOTA5WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEjIQfizmxui3ZMItmyfI+PAXt/lu5DAVx9U7XNLWlJV1SslGyJO5EpQgw3KaMbT8Z7CXUEM15YBX7Q3ipRwmaYaMnMCUwIwYDVR0lBBwwGgYIKwYBBQUHAwEGCCsGAQUFBwMCBgRVHSUAMAoGCCqGSM49BAMCA0gAMEUCICFR1HkIWNaKvmnC3K2yKzqT3mV04li35Y3wSa2jNE0jAiEAgPSZmkjWhAybbeFVhwYw2tRoUr33aKxH75kIy8MdYGE="
      ],
      "n": null,
      "e": null,
      "x": "AIyEH4s5sbot2TCLZsnyPjwF7f5buQwFcfVO1zS1pSVd",
      "y": "UrJRsiTuRKUIMNymjG0_Gewl1BDNeWAV-0N4qUcJmmE"
    },
    {
      "kid": "1e422bb9-09d8-429b-965b-85ec88e29059_sig_es384",
      "kty": "EC",
      "use": "sig",
      "alg": "ES384",
      "crv": "P-384",
      "exp": 1610923149000,
      "x5c": [
        "MIIBuzCCAUGgAwIBAgIhAKhVZ9S8jBZxo7bFKUTGPlxAmXE7+NRa15UhkwD3DjrRMAoGCCqGSM49BAMDMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjEwMTE1MjIzOTAwWhcNMjEwMTE3MjIzOTA5WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEsVnnhnMVqJlgdJL1nAMdkY3TYsfe7+jCvG8/HV9fFcLWGYj8LGRFPqjr0tuwAW0Y96przj2GvNOyA90nddd8X5KHiln8x9OZ0ZhCAjlH8KJfn3SqaXfDF+N6A/LAN7uBoycwJTAjBgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAwCgYIKoZIzj0EAwMDaAAwZQIwXO/CxFQRYyEucM2ELFG/duih44ghTzxhzOP+3RbJQHLLT0Z34iZlRPePVzXC52AaAjEAiM6wI9pOMxl8rdNdHEuCiL1SETlHs6K3mr6An6iWm63E0jL6szZX1zOZtVk/y386"
      ],
      "n": null,
      "e": null,
      "x": "ALFZ54ZzFaiZYHSS9ZwDHZGN02LH3u_owrxvPx1fXxXC1hmI_CxkRT6o69LbsAFtGA",
      "y": "APeqa849hrzTsgPdJ3XXfF-Sh4pZ_MfTmdGYQgI5R_CiX590qml3wxfjegPywDe7gQ"
    },
    {
      "kid": "52f64d60-b50b-4b07-95b2-582f87a2cb37_sig_es512",
      "kty": "EC",
      "use": "sig",
      "alg": "ES512",
      "crv": "P-521",
      "exp": 1610923149000,
      "x5c": [
        "MIICBTCCAWagAwIBAgIgEtgLr/raWVlRStF9d1djdZucnWYKKhRiZAH0KiElSnwwCgYIKoZIzj0EAwQwJDEiMCAGA1UEAwwZSmFucyBBdXRoIENBIENlcnRpZmljYXRlczAeFw0yMTAxMTUyMjM5MDBaFw0yMTAxMTcyMjM5MDlaMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwgZswEAYHKoZIzj0CAQYFK4EEACMDgYYABACvjTXQc/bAWetmIpkGg4z8b/81zz3y4ycZjiKHb9BOQfq4503ZfJ4KBj22UzaUPzjWV1se9UVv6QL+IIAkHVJbzQCP3vMaURBUUvplobpQ2791d0LFC98T6qfzGlMgBcTTMRF5QsEuFyc3PkbNpUIFLJUaLCRauNIExRuxOWjp2Zu9KaMnMCUwIwYDVR0lBBwwGgYIKwYBBQUHAwEGCCsGAQUFBwMCBgRVHSUAMAoGCCqGSM49BAMEA4GMADCBiAJCAZmADtCphAEt7UO7rfCC1fNVwHJiUz7msGCFP7pLZObijl/z/uy0dIV+YV5TcwE//aMzXj5QGfJ4tkdibp+iskNhAkIBPO7ZoCVHKiRvyxAa3RTXZHK8Sv0UixFmeS2nICyUVBLKgz36qsLkA6uXxLxuvXUhabJwJsyrAKjyp/i0X897KE0="
      ],
      "n": null,
      "e": null,
      "x": "AK-NNdBz9sBZ62YimQaDjPxv_zXPPfLjJxmOIodv0E5B-rjnTdl8ngoGPbZTNpQ_ONZXWx71RW_pAv4ggCQdUlvN",
      "y": "AI_e8xpREFRS-mWhulDbv3V3QsUL3xPqp_MaUyAFxNMxEXlCwS4XJzc-Rs2lQgUslRosJFq40gTFG7E5aOnZm70p"
    },
    {
      "kid": "13394d6c-e7cc-4699-8b96-6423984b94d7_sig_ps256",
      "kty": "RSA",
      "use": "sig",
      "alg": "PS256",
      "crv": "",
      "exp": 1610923149000,
      "x5c": [
        "MIIDcTCCAiWgAwIBAgIgMp9yALa/gAL+jgCC/gs20Zlyx+WMmmIewokfcGn3xgYwQQYJKoZIhvcNAQEKMDSgDzANBglghkgBZQMEAgEFAKEcMBoGCSqGSIb3DQEBCDANBglghkgBZQMEAgEFAKIDAgEgMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjEwMTE1MjIzOTAxWhcNMjEwMTE3MjIzOTA5WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAkcvMiWwaCFNJYpq0Ioev9nlrt40XNASE5NjuhZHGg4MHlBOAaMOsnmLqkEGhdWybbAaXkOoeoPFVBkMkuo+BpmugNBJjiGtlGbOkguRQ7+P3Ifjq45EcaBoEEr9h7MfbkYHpGSxQS1JnmylT7ORfbAaqsh3LbS+82Q/eP4/AF4WjoJeTPdkGXrqaGmqrHKtNLTN/U8/hFO52nnnbjljGbsPnTGU5279r0L1+VN5hUEstAyNlDcy5MVDesswq4AK3U7vRxDmguAiW1sw0Xzv9jR6xecdwJM0Q54i7Lwjk7IlFxWm7akE78Q9frlWaQLA8kR16yYnxoI4O+cum618c8wIDAQABoycwJTAjBgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAwQQYJKoZIhvcNAQEKMDSgDzANBglghkgBZQMEAgEFAKEcMBoGCSqGSIb3DQEBCDANBglghkgBZQMEAgEFAKIDAgEgA4IBAQAZtk2ljb/Z/gT0uzdYRUYNR6JuZXOpajoMALSSjmPSWF9IVMgQZ5dmZWg6PYmJJ/mBKSGw6ju6nbHXeVE8gPlcTZ3YOdaRS7hX9zDAF7VtiAblbDRNSt3F61euMl3HBNMcfUXhBlYt+RJwaN/x1WQ1YvOaB3xt7rihJHYhTaK+WZAQl1ptdXN7m9IXCHMWEqO4A3+m3a9z3bNoSd7W221He+G0OUcNhMXyLxSJp2D6RO80AIAOCahhSy3DQ33Uk6PPiQu3AZlKk9Ppi6e17B+yUU9PVt9U2GgqAFa8ll2AjT3lG/kyQZLSmM0WU44rcsncsCdWWXGTmChxD4DUCV0u"
      ],
      "n": "kcvMiWwaCFNJYpq0Ioev9nlrt40XNASE5NjuhZHGg4MHlBOAaMOsnmLqkEGhdWybbAaXkOoeoPFVBkMkuo-BpmugNBJjiGtlGbOkguRQ7-P3Ifjq45EcaBoEEr9h7MfbkYHpGSxQS1JnmylT7ORfbAaqsh3LbS-82Q_eP4_AF4WjoJeTPdkGXrqaGmqrHKtNLTN_U8_hFO52nnnbjljGbsPnTGU5279r0L1-VN5hUEstAyNlDcy5MVDesswq4AK3U7vRxDmguAiW1sw0Xzv9jR6xecdwJM0Q54i7Lwjk7IlFxWm7akE78Q9frlWaQLA8kR16yYnxoI4O-cum618c8w",
      "e": "AQAB",
      "x": null,
      "y": null
    },
    {
      "kid": "66294948-40f2-48df-8ff6-d342d56f8102_sig_ps384",
      "kty": "RSA",
      "use": "sig",
      "alg": "PS384",
      "crv": "",
      "exp": 1610923149000,
      "x5c": [
        "MIIDcTCCAiWgAwIBAgIgOpAUrzN/UPGm9uLadWCgpQIaDb7wp6I44MAYu3pAsuAwQQYJKoZIhvcNAQEKMDSgDzANBglghkgBZQMEAgIFAKEcMBoGCSqGSIb3DQEBCDANBglghkgBZQMEAgIFAKIDAgEwMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjEwMTE1MjIzOTAxWhcNMjEwMTE3MjIzOTA5WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAy9KnfzsvwhO23Pef6TfyIOkAqnDGNwrzzeBYuqiql3/Zu9uhtbDD9Q1/XClqOvbi+ouUXJHZ9Rsxpvald1z9+8XAZYIpQ0Rrqr11KBeNEwyrn/fK/lfgOaVuVSG1oe7o5zVZXMlYUiRk+koVkrm2Jj9NG7FDBW28pChps3Wk05KBsopgndKjIna3WYgT2zr0KHDwPe4EbQSL2NyS87TF77jD6yW24jqEMuAa/FvvFIDiEP60JQG9aNO19w0yMkmj8c8+cQtwO/E5huRg83bfGHMPMSaDgxoXoDCt+UKIpkVC+SgE1+SBZLMuLpsj+wTBXhz0MJm6LYFLJxalz9+eawIDAQABoycwJTAjBgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAwQQYJKoZIhvcNAQEKMDSgDzANBglghkgBZQMEAgIFAKEcMBoGCSqGSIb3DQEBCDANBglghkgBZQMEAgIFAKIDAgEwA4IBAQBGhHD5FKSqJ4GJYPEi48AhUlrmc4xBsKr7FXNGzYpXkgzwlhP0XkifGguT4W/4JD9ra4GdPjVweC6XM525Uu1NDKvBDMLUYaQj5rDmXe0hHSL8cJ60yKOipbbyOXycvHot3Kebqy7rECCKDjEbejaHnYYjdVNFehz7CvweAB0cn34QKAPfBjfqDHW8d2B46Ow7jhGL18ep01j9WeEeLW5K0nbdgRRLKA2T7RV6jXrsnI5w7eOxfn1WXj7PGmFeyLZQaEUtK7pX+cU10wBVk3Jtt/9HaQTxE9aVETpddwWLjakAzoS0gXRqlQLh1786zIBcHGKlpkYKbTEXIBokBgdN"
      ],
      "n": "y9KnfzsvwhO23Pef6TfyIOkAqnDGNwrzzeBYuqiql3_Zu9uhtbDD9Q1_XClqOvbi-ouUXJHZ9Rsxpvald1z9-8XAZYIpQ0Rrqr11KBeNEwyrn_fK_lfgOaVuVSG1oe7o5zVZXMlYUiRk-koVkrm2Jj9NG7FDBW28pChps3Wk05KBsopgndKjIna3WYgT2zr0KHDwPe4EbQSL2NyS87TF77jD6yW24jqEMuAa_FvvFIDiEP60JQG9aNO19w0yMkmj8c8-cQtwO_E5huRg83bfGHMPMSaDgxoXoDCt-UKIpkVC-SgE1-SBZLMuLpsj-wTBXhz0MJm6LYFLJxalz9-eaw",
      "e": "AQAB",
      "x": null,
      "y": null
    },
    {
      "kid": "c9239f5e-9168-43dc-a931-e3fe26ffec51_sig_ps512",
      "kty": "RSA",
      "use": "sig",
      "alg": "PS512",
      "crv": "",
      "exp": 1610923149000,
      "x5c": [
        "MIIDcjCCAiagAwIBAgIhALyTZpvJJ7wbNwXRjx4qAOkF8+PE0CAKLiNmluz8qBsSMEEGCSqGSIb3DQEBCjA0oA8wDQYJYIZIAWUDBAIDBQChHDAaBgkqhkiG9w0BAQgwDQYJYIZIAWUDBAIDBQCiAwIBQDAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMB4XDTIxMDExNTIyMzkwMVoXDTIxMDExNzIyMzkwOVowJDEiMCAGA1UEAwwZSmFucyBBdXRoIENBIENlcnRpZmljYXRlczCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAOV/yvZh9YgWhjWUX/uhFSxXscTiidKrmxRoHJprK3daNgNj04jkWC53aZYt4TvYI/gTOUwmt+j8IzVmxpDZeKi7BiEb5C0iv1iDndEDZfVScLBQnXMPvaObtnFc5FXPe/8rMsRdtPigIcZW7lB987+jlgi62lojFSQKR3C+q7P6jQ0LSy/v9bCwE/X+dVfgQ0iiigFQfLISb/o49jyMZ8tiW+skHCpfE5WrlYq4dilfZIExcF5fgNKXb9DaGRT/GUtF7at+tDD186UiToWcez2V1i5hh97WGVq43CUA4PWfXHidJN4Orzn8rP/tx7CAsl3fVFImKStIKtuhzm0ig4sCAwEAAaMnMCUwIwYDVR0lBBwwGgYIKwYBBQUHAwEGCCsGAQUFBwMCBgRVHSUAMEEGCSqGSIb3DQEBCjA0oA8wDQYJYIZIAWUDBAIDBQChHDAaBgkqhkiG9w0BAQgwDQYJYIZIAWUDBAIDBQCiAwIBQAOCAQEAzm9YrqtVIUwnXZ2Uhl9J7FCC73nF8OC31/SQMBzfxsyo6Hx9ksxuBfvvmnCb761luVgeFOELlttfsV9uj4T3TuNzZ8uOCrxCgFOeG1CTJ3V4tl5XDSo3cLeYwIlj8hpeHKwCc6Ecjh4BLfQvzIGQWTVFLfiA1SHUsidnLFNUSogD7OXBDHq+GzErcQafCqeozRF1JfrUEt9NHdGk4+w859qz7jAP3hjkBtCMYWFhMUnR55PHBUfZ0sLIH10nDG+eeRCaVDJLbrUb6lLMPQH7sWDU7f3EYa2lk3Z25M1p7Oah6R5VaKcO/AQ4bEk9ptQWd6HLyYLMVtk4dRNbr7IX+Q=="
      ],
      "n": "5X_K9mH1iBaGNZRf-6EVLFexxOKJ0qubFGgcmmsrd1o2A2PTiORYLndpli3hO9gj-BM5TCa36PwjNWbGkNl4qLsGIRvkLSK_WIOd0QNl9VJwsFCdcw-9o5u2cVzkVc97_ysyxF20-KAhxlbuUH3zv6OWCLraWiMVJApHcL6rs_qNDQtLL-_1sLAT9f51V-BDSKKKAVB8shJv-jj2PIxny2Jb6yQcKl8TlauVirh2KV9kgTFwXl-A0pdv0NoZFP8ZS0Xtq360MPXzpSJOhZx7PZXWLmGH3tYZWrjcJQDg9Z9ceJ0k3g6vOfys_-3HsICyXd9UUiYpK0gq26HObSKDiw",
      "e": "AQAB",
      "x": null,
      "y": null
    },
    {
      "kid": "90d4965b-868d-4290-91a6-8c5d49459f88_enc_rsa1_5",
      "kty": "RSA",
      "use": "enc",
      "alg": "RSA1_5",
      "crv": "",
      "exp": 1610923149000,
      "x5c": [
        "MIIDCjCCAfKgAwIBAgIhAIksD5qI5INemoZFFfxQ+DT6CwQT/3vPF+hSmWtKOqUpMA0GCSqGSIb3DQEBCwUAMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjEwMTE1MjIzOTAyWhcNMjEwMTE3MjIzOTA5WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApB+uqGCDL32xtRSrdADTTakbLKx3DelyBLIfTbEsTv2u0GjRawv/AocnWiwHd3N6b/SkmXXnOpvsFSlFNTbZJho49kjdcdZ7zmXE7LkcXsa3Z1F1bHweXddqqWuxVUXt80so5eAQseOW33KPGMbORHhIkt4IRDC0bAcEKUgk5ibGA9wuolcsA4tZ9/zuKhZpQzXy+3Z2ezfX+veRLXB7N7bh3JZeaJMKEaMriOE73iTHk6xr4qcEK2wdVJhQjxCHoEm0++66ATivSGcJqAexoS6hoR/LibpSMP5+Tw5QHHvLFc1b0CDa0yhFzPMpB+CYJJnDOpPdLz1i2/oAOeh1RQIDAQABoycwJTAjBgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAwDQYJKoZIhvcNAQELBQADggEBAHbVma4EZAsw7ltfqlwpJK6lGPCNdPInzYlrWof4vay4oCkKXrhE4AvAayxlszoom+Ap1RKf+8oUzVN0Ilz1IF0OvKle+PL1S92kb90uM6NxlQ/fRSveAg7At/J+N9Xu2OJ1v0xUkDvmCTmX6bnnfSafWWSU0Z/na2w84owRD29dRsFn2Ge5EmuhIjNzDYfZCMhfVQQvJh1olvHXr59+ustbJGM8pmeZ0Uh15IidzpTR3kiXbcOVbHuJCJ8vjjLiUq2MzNFXWRxTDinw920yVCxLT1GhMy0lq2IHsZFGMwrsDT50aUv9+7/dz4RQlpLuVs+p2q8ERGCfxPLxaO8MG38="
      ],
      "n": "pB-uqGCDL32xtRSrdADTTakbLKx3DelyBLIfTbEsTv2u0GjRawv_AocnWiwHd3N6b_SkmXXnOpvsFSlFNTbZJho49kjdcdZ7zmXE7LkcXsa3Z1F1bHweXddqqWuxVUXt80so5eAQseOW33KPGMbORHhIkt4IRDC0bAcEKUgk5ibGA9wuolcsA4tZ9_zuKhZpQzXy-3Z2ezfX-veRLXB7N7bh3JZeaJMKEaMriOE73iTHk6xr4qcEK2wdVJhQjxCHoEm0--66ATivSGcJqAexoS6hoR_LibpSMP5-Tw5QHHvLFc1b0CDa0yhFzPMpB-CYJJnDOpPdLz1i2_oAOeh1RQ",
      "e": "AQAB",
      "x": null,
      "y": null
    },
    {
      "kid": "e6e8ccc4-708b-4a83-bbd2-7a9e0181734f_enc_rsa-oaep",
      "kty": "RSA",
      "use": "enc",
      "alg": "RSA-OAEP",
      "crv": "",
      "exp": 1610923149000,
      "x5c": [
        "MIIDCjCCAfKgAwIBAgIhAIfkfNwuxlcdhdiAKvWrX+LbYKvZwRC9aEn9tOqCZLunMA0GCSqGSIb3DQEBCwUAMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjEwMTE1MjIzOTAyWhcNMjEwMTE3MjIzOTA5WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAw6p2QLwHKwPA+W9oTjTAkYn1iRQVXdPsNIu18Lao11Fbp0krMKSsnVcBIuO8zjsERf8b/awTN9zJQpKO3LqHHcGIjZJdAfH42CPgyUMjn6laF8iO0S+kI8RCocRLoPP2PVbqPjYD6kvK0mlSSLu+t9bU7mgEsYF5y8r05hX1ROdLUTFuHMa2g4cuD0HEEJMzewK1TzPikNiThsQv0yzwkwGZrBldWeB1E8BGWha2jwVom/Noo6vimtN8Le1XeYq5PvRVaS4AtLup4K0SaVetL0mAiCWKUTudWNDCRWB/Z4lJCJGOCCfk6bPp0TsjOcDjGkPzP05G9FFWndOpQ49UcwIDAQABoycwJTAjBgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAwDQYJKoZIhvcNAQELBQADggEBAJ6zGYZqI4rwBJri7v3XSLvKrUgU19xLq6aik6h3DMylzHFEydnMdgyYU23GWP/rcvM1K4whhiopUcoj/FHQ0uaQV67zb6/NvCbIxiGjQs08ZcYnMtZ0zwm9hj7YeafsVQVI3qo1VdJfPWYHEW4IUfaqIlWdsj/CW1HeKWOrw0+WC1JYwD5Ka13bwYtC3jgt8yHwn3XoOhbINzFsVdRA5pfJKCvZN5IteHhpkmeOkvOlRFaPrqlGM2rukCzo2aBakC8F8SwaQje6prm2wSRJp/qjJKxKO8fMklcBT/FMD2zdYeHb4+YFRo8/CzjRNPEmMSI4LHdFkmjGDrLQYjrxOsY="
      ],
      "n": "w6p2QLwHKwPA-W9oTjTAkYn1iRQVXdPsNIu18Lao11Fbp0krMKSsnVcBIuO8zjsERf8b_awTN9zJQpKO3LqHHcGIjZJdAfH42CPgyUMjn6laF8iO0S-kI8RCocRLoPP2PVbqPjYD6kvK0mlSSLu-t9bU7mgEsYF5y8r05hX1ROdLUTFuHMa2g4cuD0HEEJMzewK1TzPikNiThsQv0yzwkwGZrBldWeB1E8BGWha2jwVom_Noo6vimtN8Le1XeYq5PvRVaS4AtLup4K0SaVetL0mAiCWKUTudWNDCRWB_Z4lJCJGOCCfk6bPp0TsjOcDjGkPzP05G9FFWndOpQ49Ucw",
      "e": "AQAB",
      "x": null,
      "y": null
    }
  ]
}
```

- **`puts/replace - JWK`**

You can add a new JWK or replace an old JWK with new value through this Interactive Mode option.
When it will ask `Add JwonWebKey?` just press `y` to confirm. Fill each property with a value, keep empty to skip.

- **__kid__**: Unique Key Identifier [String].
- **__kty__**: Cryptographic algorithm name used with the key [String].
- **__use__**: Key usage, [enc, sig,..]
- **__alg__**: The cryptographic algorithm name that is going to be used.
- **__exp__**: time validation

```text
Gets list of JSON Web Key (JWK) used by server
Gets list of JSON Web Key (JWK) used by server. JWK is a JSON data structure that represents a set of public keys as a JSON object [RFC4627].
---------------------------------------------------------------------------------------------------------------------------------------------
Please wait while retreiving data ...

Getting access token for scope https://jans.io/oauth/config/jwks.readonly

«JsonWebKey. »
Add JsonWebKey? y

   «The unique identifier for the key. Type: string»
   kid: aabb

   «The family of cryptographic algorithms used with the key. Type: string»
   kty: RSA

   «How the key was meant to be used; sig represents the signature. Type: string»
   use: enc

   «The specific cryptographic algorithm used with the key. Type: string»
   alg: RSA-OAEP

   «The crv member identifies the cryptographic curve used with the key. Values defined by this specification are P-256, P-384 and P-521. Additional crv values MAY be used, provided they are understood by implementations using that Elliptic Curve key. The crv value is case sensitive. Type: string»
   crv: 

   «Contains the token expiration timestamp. Type: integer»
   exp: 

   «The x.509 certificate chain. The first entry in the array is the certificate to use for token verification; the other certificates can be used to verify this first certificate. Type: array of string separated by _,»
   x5c: 

   «The modulus for the RSA public key. Type: string»
   n: 

   «The exponent for the RSA public key. Type: string»
   e: 

   «The x member contains the x coordinate for the elliptic curve point. It is represented as the base64url encoding of the coordinate's big endian representation. Type: string»
   x: 

   «The y member contains the y coordinate for the elliptic curve point. It is represented as the base64url encoding of the coordinate's big endian representation. Type: string»
   y: 

Add another JsonWebKey? n
Obtained Data:

{
  "keys": [
    {
      "kid": "aabb",
      "kty": "RSA",
      "use": "enc",
      "alg": "RSA-OAEP",
      "crv": null,
      "exp": null,
      "x5c": [],
      "n": null,
      "e": null,
      "x": null,
      "y": null
    }
  ]
}

Continue? y
Getting access token for scope https://jans.io/oauth/config/jwks.write
Please wait while posting data ...

{
  "keys": [
    {
      "kid": "aabb",
      "kty": "RSA",
      "use": "enc",
      "alg": "RSA-OAEP",
      "crv": "",
      "exp": null,
      "x5c": null,
      "n": null,
      "e": null,
      "x": null,
      "y": null
    }
  ]
}
```

- **`Patch JWK`**

Just chose this option and fill the value for `op`, `path`, and `value` to patch JSON Web Key.

### Custom Scripts

Interception scripts can be used to implement custom business logic for authentication, authorization, and more in a way that is upgrade-proof and doesn't require forking the Gluu Server code. Using Janssen CLI, you can perform such an operation as listed below:

```text
Custom Scripts
--------------
1 Gets a list of custom scripts
2 Adds a new custom script
3 Updates a custom script
4 Deletes a custom script
```
- **get info of custom scripts**

To get the status of each type of script select option 1, you will get the below result in return:

```
Gets a list of custom scripts
-----------------------------
Please wait while retreiving data ...

Getting access token for scope https://jans.io/oauth/config/scripts.readonly
+----+-------------------------------------+-----------------------------------------------------------+-----------+-----------+
|    | scriptType                          | name                                                      | enabled   | inum      |
+====+=====================================+===========================================================+===========+===========+
|  1 | RESOURCE_OWNER_PASSWORD_CREDENTIALS | resource_owner_password_credentials_example               | False     | 2DAF-AA91 |
+----+-------------------------------------+-----------------------------------------------------------+-----------+-----------+
|  2 | INTROSPECTION                       | introspection_custom_params                               | False     | 2DAF-BA90 |
+----+-------------------------------------+-----------------------------------------------------------+-----------+-----------+
|  3 | UMA_CLAIMS_GATHERING                | sampleClaimsGathering                                     | False     | 2DAF-F996 |
+----+-------------------------------------+-----------------------------------------------------------+-----------+-----------+
|  4 | END_SESSION                         | frontchannel_logout_sample                                | False     | 2DAF-CA90 |
+----+-------------------------------------+-----------------------------------------------------------+-----------+-----------+
|  5 | UPDATE_TOKEN                        | update_token_sample                                       | False     | 2D3E.5A03 |
+----+-------------------------------------+-----------------------------------------------------------+-----------+-----------+
|  6 | INTROSPECTION                       | introspection_sample                                      | False     | 2DAF-AA90 |
+----+-------------------------------------+-----------------------------------------------------------+-----------+-----------+
|  7 | RESOURCE_OWNER_PASSWORD_CREDENTIALS | resource_owner_password_credentials_custom_params_example | False     | 2DAF-BA91 |
+----+-------------------------------------+-----------------------------------------------------------+-----------+-----------+
|  8 | CIBA_END_USER_NOTIFICATION          | firebase_ciba_end_user_notification                       | False     | C1BA-C1BA |
+----+-------------------------------------+-----------------------------------------------------------+-----------+-----------+
|  9 | PERSON_AUTHENTICATION               | basic                                                     | False     | A51E-76DA |
+----+-------------------------------------+-----------------------------------------------------------+-----------+-----------+
| 10 | CONSENT_GATHERING                   | consent_gathering                                         | False     | DAA9-BA60 |
+----+-------------------------------------+-----------------------------------------------------------+-----------+-----------+
| 11 | PERSON_AUTHENTICATION               | basic_lock                                                | False     | 4BBE-C6A8 |
+----+-------------------------------------+-----------------------------------------------------------+-----------+-----------+
| 12 | PERSON_AUTHENTICATION               | cert                                                      | False     | 2124-0CF1 |
+----+-------------------------------------+-----------------------------------------------------------+-----------+-----------+
| 13 | PERSON_AUTHENTICATION               | yubicloud                                                 | False     | 24FD-B96E |
+----+-------------------------------------+-----------------------------------------------------------+-----------+-----------+
| 14 | PERSON_AUTHENTICATION               | otp                                                       | False     | 5018-D4BF |
+----+-------------------------------------+-----------------------------------------------------------+-----------+-----------+
| 15 | PERSON_AUTHENTICATION               | smpp                                                      | False     | 09A0-93D7 |
+----+-------------------------------------+-----------------------------------------------------------+-----------+-----------+
| 16 | PERSON_AUTHENTICATION               | twilio_sms                                                | False     | 09A0-93D6 |
+----+-------------------------------------+-----------------------------------------------------------+-----------+-----------+
| 17 | PERSON_AUTHENTICATION               | thumb_sign_in                                             | False     | 92F0-759E |
+----+-------------------------------------+-----------------------------------------------------------+-----------+-----------+
| 18 | PERSON_AUTHENTICATION               | u2f                                                       | False     | 8BAF-80D6 |
+----+-------------------------------------+-----------------------------------------------------------+-----------+-----------+
| 19 | PERSON_AUTHENTICATION               | duo                                                       | False     | 5018-F9CF |
+----+-------------------------------------+-----------------------------------------------------------+-----------+-----------+
| 20 | PERSON_AUTHENTICATION               | super_gluu                                                | False     | 92F0-BF9E |
+----+-------------------------------------+-----------------------------------------------------------+-----------+-----------+
| 21 | PERSON_AUTHENTICATION               | fido2                                                     | False     | 8BAF-80D7 |
+----+-------------------------------------+-----------------------------------------------------------+-----------+-----------+
| 22 | PERSON_AUTHENTICATION               | uaf                                                       | False     | 5018-AF9C |
+----+-------------------------------------+-----------------------------------------------------------+-----------+-----------+
| 23 | USER_REGISTRATION                   | user_registration                                         | False     | 6EA0-8F0C |
+----+-------------------------------------+-----------------------------------------------------------+-----------+-----------+
| 24 | UMA_RPT_POLICY                      | scim_access_policy                                        | False     | 2DAF-F9A5 |
+----+-------------------------------------+-----------------------------------------------------------+-----------+-----------+
| 25 | UMA_RPT_POLICY                      | uma_rpt_policy                                            | False     | 2DAF-F995 |
+----+-------------------------------------+-----------------------------------------------------------+-----------+-----------+
| 26 | DYNAMIC_SCOPE                       | org_name                                                  | False     | 031C-5621 |
+----+-------------------------------------+-----------------------------------------------------------+-----------+-----------+
| 27 | PERSISTENCE_EXTENSION               | persistence_extension                                     | False     | 8AF7.D82A |
+----+-------------------------------------+-----------------------------------------------------------+-----------+-----------+
| 28 | ID_GENERATOR                        | id_generator                                              | False     | 031C-4A65 |
+----+-------------------------------------+-----------------------------------------------------------+-----------+-----------+
| 29 | IDP                                 | idp                                                       | False     | 8AF7.D82B |
+----+-------------------------------------+-----------------------------------------------------------+-----------+-----------+
| 30 | CACHE_REFRESH                       | cache_refresh                                             | False     | 13D3-E7AD |
+----+-------------------------------------+-----------------------------------------------------------+-----------+-----------+
| 31 | APPLICATION_SESSION                 | application_session                                       | False     | DAA9-B789 |
+----+-------------------------------------+-----------------------------------------------------------+-----------+-----------+
| 32 | DYNAMIC_SCOPE                       | dynamic_permission                                        | True      | CB5B-3211 |
+----+-------------------------------------+-----------------------------------------------------------+-----------+-----------+
| 33 | SCIM                                | scim_event_handler                                        | False     | A910-56AB |
+----+-------------------------------------+-----------------------------------------------------------+-----------+-----------+
| 34 | CLIENT_REGISTRATION                 | client_registration                                       | False     | DAA9-B788 |
+----+-------------------------------------+-----------------------------------------------------------+-----------+-----------+
| 35 | DYNAMIC_SCOPE                       | work_phone                                                | False     | 031C-5622 |
+----+-------------------------------------+-----------------------------------------------------------+-----------+-----------+
| 36 | USER_REGISTRATION                   | user_confirm_registration                                 | False     | 6EA0-8F0D |
+----+-------------------------------------+-----------------------------------------------------------+-----------+-----------+
| 37 | UPDATE_USER                         | update_user                                               | False     | 522F-CDC5 |
+----+-------------------------------------+-----------------------------------------------------------+-----------+-----------+


```

To add a new script, choose option 2 and fill each property:

`dn, inum, display name, valid script`
```
ScriptType: [PERSON_AUTHENTICATION, INTROSPECTION, RESOURCE_OWNER_PASSWORD_CREDENTIALS, APPLICATION_SESSION, CACHE_REFRESH, UPDATE_USER, USER_REGISTRATION, CLIENT_REGISTRATION, ID_GENERATOR, UMA_RPT_POLICY, UMA_RPT_CLAIMS, UMA_CLAIMS_GATHERING, CONSENT_GATHERING, DYNAMIC_SCOPE, SPONTANEOUS_SCOPE, END_SESSION, POST_AUTHN, SCIM, CIBA_END_USER_NOTIFICATION, PERSISTENCE_EXTENSION, IDP]

Programming Language: [PYTHON, JAVA]
```
- **update scripts**

Let update `Person Authentication basic` by its inum. Select option 3 from **custom scripts** menu
and enter its inum, in my case it's `A51E-76DA`

![](img/im-update-custom-script.png)

selecting the field we are gonna update: 4

```
«q: quit, v: view, s: save, l: list fields #: update filed. »
Selection: 4

«boolean value indicating if script enabled. Type: boolean»
enabled  [false]: true
Please enter a(n) boolean value: _true, _false
enabled  [false]: _true

«q: quit, v: view, s: save, l: list fields #: update filed. »
Selection: 4

«boolean value indicating if script enabled. Type: boolean»
enabled  [true]: 

```
In this way you can update any field you want.

- **Delete scripts**

To delete a custom script, you need an `inum` of a custom script that you want to delete.
It will ask for confirmation when you enter `inum`, simply enter `yes/y` to delete it otherwise enter `no/n` to cancel the operation.

### LDAP Configuration 

Using Janssen CLI, the Following list of actions can be performed in LDAP.
```text
Database - LDAP configuration
-----------------------------
1 Gets list of existing LDAP configurations
2 Adds a new LDAP configuration
3 Updates LDAP configuration
4 Gets an LDAP configuration by name
5 Deletes an LDAP configuration
6 Partially modify an LDAP configuration
7 Tests an LDAP configuration
```

- **List of Existing LDAP**

To get a list of existing LDAP configurations, select option 1 and press enter, you will get a list of existing LDAP configurations in your Janssen server.

```text
Gets list of existing LDAP configurations
-----------------------------------------
Please wait while retreiving data ...

Getting access token for scope https://jans.io/oauth/config/database/ldap.readonly

[
  {
    "configId": "auth_ldap_server",
    "bindDN": "cn=directory manager",
    "bindPassword": "gD63aUTvvS4=",
    "servers": [
      "localhost:1636"
    ],
    "maxConnections": 1000,
    "useSSL": true,
    "baseDNs": [
      "ou=people,o=jans"
    ],
    "primaryKey": "uid",
    "localPrimaryKey": "uid",
    "useAnonymousBind": false,
    "enabled": false,
    "version": 0,
    "level": 0
  }
]
```
- **Adding new LDAP**

To add a new LDAP configuration, choose option 2 and add the following properties:
```json5
{
  "configId":,
  "bindDN": ,
  "bindPassword":,
  "servers": [],
  "maxConnections": 2,
  "useSSL": false,
  "baseDNs": [],
  "primaryKey":,
  "localPrimaryKey":,
  "useAnonymousBind": false,
  "enabled": false,
  "version": null,
  "level": null
}
```
Then enter `y` to confirm.

- **Update an LDAP configuration**

To update an existing LDAP configuration, select option 3 and enter the LDAP configuration name. If it matches to the existing configuration then It will ask to enter a value for each properties.

- **Delete a LDAP configuration**

To delete an existing ldap configuration, enter a name of an existing ldap configuration and enter `yes/y` to confirm.


### Couchbase Configuration

From the main menu, select option 15 to enter into Couchbase configuration menu. You will get the following menu like LDAP configuration.

```text
Database - Couchbase configuration
----------------------------------
1 Gets list of existing Couchbase configurations
2 Adds a new Couchbase configuration
3 Updates Couchbase configuration
4 Gets a Couchbase configurations by name
5 Partially modify an Couchbase configuration
6 Deletes a Couchbase configurations by name
7 Tests a Couchbase configuration
```

### OpenID Connect - Clients

OpenID Connect Interactive Mode supports the following list of actions:

```text
OAuth - OpenID Connect - Clients
--------------------------------
1 Gets list of OpenID Connect clients
2 Create new OpenId connect client
3 Update OpenId Connect client
4 Get OpenId Connect Client by Inum
5 Delete OpenId Connect client
6 Update modified properties of OpenId Connect client by Inum
```
Using Janssen CLI, the Administrator can easily `create/update/delete` OpenID Connect without any interruption.

- **__list of OpenID Connect clients__**

By selecting option '1' you will get a list of OpenID Connect clients.
You may enter `limit[50]` and `pattern` to filter in searching.

```
Gets list of OpenID Connect clients
-----------------------------------

«Search size - max size of the results to return. Type: integer»
limit  [50]: 

«Search pattern. Type: string»
pattern: 
Calling Api with parameters: {'limit': 50}
Please wait while retreiving data ...

Getting access token for scope https://jans.io/oauth/config/openid/clients.readonly
[
  {
    "dn": "inum=1801.d361f68d-8200-4ba2-a0bb-ca7fea79e805,ou=clients,o=jans",
    "inum": "1801.d361f68d-8200-4ba2-a0bb-ca7fea79e805",
    "clientSecret": "KfwZeAfq4jrL",
    "frontChannelLogoutUri": null,
    "frontChannelLogoutSessionRequired": false,
    "registrationAccessToken": null,
    "clientIdIssuedAt": null,
    "clientSecretExpiresAt": null,
    "redirectUris": [
      "https://testjans.gluu.com/admin-ui",
      "http//:localhost:4100"
    ],
    "claimRedirectUris": null,
    "responseTypes": [
      "code"
    ],
    "grantTypes": [
      "authorization_code",
      "refresh_token",
      "client_credentials"
    ],
    "applicationType": "web",
    "contacts": null,
    "clientName": "Jans Config Api Client",
    "idTokenTokenBindingCnf": null,
    "logoUri": null,
    "clientUri": null,
    "policyUri": null,
    "tosUri": null,
    "jwksUri": null,
    "jwks": null,
    "sectorIdentifierUri": null,
    "subjectType": "pairwise",
    "idTokenSignedResponseAlg": "RS256",
    "idTokenEncryptedResponseAlg": null,
    "idTokenEncryptedResponseEnc": null,
    "userInfoSignedResponseAlg": null,
    "userInfoEncryptedResponseAlg": null,
    "userInfoEncryptedResponseEnc": null,
    "requestObjectSigningAlg": null,
    "requestObjectEncryptionAlg": null,
    "requestObjectEncryptionEnc": null,
    "tokenEndpointAuthMethod": "client_secret_basic",
    "tokenEndpointAuthSigningAlg": null,
    "defaultMaxAge": null,
    "requireAuthTime": false,
    "defaultAcrValues": null,
    "initiateLoginUri": null,
    "postLogoutRedirectUris": null,
    "requestUris": null,
    "scopes": [
      "inum=1800.F6E877,ou=scopes,o=jans",
      "inum=1800.D4F3E7,ou=scopes,o=jans",
      "inum=1800.2FD7EF,ou=scopes,o=jans",
      "inum=1800.97B23C,ou=scopes,o=jans",
      "inum=1800.8FC2C7,ou=scopes,o=jans",
      "inum=1800.1FFDF2,ou=scopes,o=jans",
      "inum=1800.5CF44C,ou=scopes,o=jans",
      "inum=1800.CCA518,ou=scopes,o=jans",
      "inum=1800.E62D6E,ou=scopes,o=jans",
      "inum=1800.11CB33,ou=scopes,o=jans",
      "inum=1800.781FA2,ou=scopes,o=jans",
      "inum=1800.ADAD8F,ou=scopes,o=jans",
      "inum=1800.40F22F,ou=scopes,o=jans",
      "inum=1800.7619BA,ou=scopes,o=jans",
      "inum=1800.E0DAF5,ou=scopes,o=jans",
      "inum=1800.7F45B0,ou=scopes,o=jans",
      "inum=1800.778C57,ou=scopes,o=jans",
      "inum=1800.E39293,ou=scopes,o=jans",
      "inum=1800.939483,ou=scopes,o=jans",
      "inum=1800.0ED2E8,ou=scopes,o=jans",
      "inum=1800.66CA59,ou=scopes,o=jans",
      "inum=1800.A4DBE5,ou=scopes,o=jans",
      "inum=1800.9AF358,ou=scopes,o=jans",
      "inum=1800.478CCF,ou=scopes,o=jans",
      "inum=1800.450A9A,ou=scopes,o=jans",
      "inum=1800.27A193,ou=scopes,o=jans",
      "inum=1800.3971D5,ou=scopes,o=jans",
      "inum=1800.891693,ou=scopes,o=jans",
      "inum=1800.A35DFD,ou=scopes,o=jans",
      "inum=1800.3516DE,ou=scopes,o=jans",
      "inum=F0C4,ou=scopes,o=jans",
      "inum=764C,ou=scopes,o=jans",
      "inum=10B2,ou=scopes,o=jans"
    ],
    "claims": null,
    "trustedClient": false,
    "lastAccessTime": null,
    "lastLogonTime": null,
    "persistClientAuthorizations": true,
    "includeClaimsInIdToken": false,
    "refreshTokenLifetime": null,
    "accessTokenLifetime": null,
    "customAttributes": [],
    "customObjectClasses": [
      "top"
    ],
    "rptAsJwt": false,
    "accessTokenAsJwt": false,
    "accessTokenSigningAlg": "RS256",
    "disabled": false,
    "authorizedOrigins": null,
    "softwareId": null,
    "softwareVersion": null,
    "softwareStatement": null,
    "attributes": {
      "tlsClientAuthSubjectDn": null,
      "runIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims": false,
      "keepClientAuthorizationAfterExpiration": false,
      "allowSpontaneousScopes": false,
      "spontaneousScopes": null,
      "spontaneousScopeScriptDns": null,
      "backchannelLogoutUri": null,
      "backchannelLogoutSessionRequired": false,
      "additionalAudience": null,
      "postAuthnScripts": null,
      "consentGatheringScripts": null,
      "introspectionScripts": null,
      "rptClaimsScripts": null
    },
    "backchannelTokenDeliveryMode": null,
    "backchannelClientNotificationEndpoint": null,
    "backchannelAuthenticationRequestSigningAlg": null,
    "backchannelUserCodeParameter": null,
    "expirationDate": null,
    "deletable": false,
    "jansId": null
  },
  {
    "dn": "inum=1001.0e964ce7-7670-44a4-a2d1-d0a5f689a34f,ou=clients,o=jans",
    "inum": "1001.0e964ce7-7670-44a4-a2d1-d0a5f689a34f",
    "clientSecret": "4OJLToBXav0P",
    "frontChannelLogoutUri": "https://testjans.gluu.com/identity/ssologout.htm",
    "frontChannelLogoutSessionRequired": true,
    "registrationAccessToken": null,
    "clientIdIssuedAt": null,
    "clientSecretExpiresAt": null,
    "redirectUris": [
      "https://testjans.gluu.com/identity/scim/auth",
      "https://testjans.gluu.com/identity/authcode.htm",
      "https://testjans.gluu.com/jans-auth/restv1/uma/gather_claims?authentication=true"
    ],
    "claimRedirectUris": [
      "https://testjans.gluu.com/jans-auth/restv1/uma/gather_claims"
    ],
    "responseTypes": [
      "code"
    ],
    "grantTypes": [
      "authorization_code",
      "implicit",
      "refresh_token"
    ],
    "applicationType": "web",
    "contacts": null,
    "clientName": "oxTrust Admin GUI",
    "idTokenTokenBindingCnf": null,
    "logoUri": null,
    "clientUri": null,
    "policyUri": null,
    "tosUri": null,
    "jwksUri": null,
    "jwks": null,
    "sectorIdentifierUri": null,
    "subjectType": "public",
    "idTokenSignedResponseAlg": "HS256",
    "idTokenEncryptedResponseAlg": null,
    "idTokenEncryptedResponseEnc": null,
    "userInfoSignedResponseAlg": null,
    "userInfoEncryptedResponseAlg": null,
    "userInfoEncryptedResponseEnc": null,
    "requestObjectSigningAlg": null,
    "requestObjectEncryptionAlg": null,
    "requestObjectEncryptionEnc": null,
    "tokenEndpointAuthMethod": "client_secret_basic",
    "tokenEndpointAuthSigningAlg": null,
    "defaultMaxAge": null,
    "requireAuthTime": false,
    "defaultAcrValues": null,
    "initiateLoginUri": null,
    "postLogoutRedirectUris": [
      "https://testjans.gluu.com/identity/finishlogout.htm"
    ],
    "requestUris": null,
    "scopes": [
      "inum=F0C4,ou=scopes,o=jans",
      "inum=10B2,ou=scopes,o=jans",
      "inum=764C,ou=scopes,o=jans"
    ],
    "claims": null,
    "trustedClient": true,
    "lastAccessTime": null,
    "lastLogonTime": null,
    "persistClientAuthorizations": false,
    "includeClaimsInIdToken": false,
    "refreshTokenLifetime": null,
    "accessTokenLifetime": null,
    "customAttributes": [],
    "customObjectClasses": [
      "top"
    ],
    "rptAsJwt": false,
    "accessTokenAsJwt": false,
    "accessTokenSigningAlg": null,
    "disabled": false,
    "authorizedOrigins": null,
    "softwareId": null,
    "softwareVersion": null,
    "softwareStatement": null,
    "attributes": {
      "tlsClientAuthSubjectDn": null,
      "runIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims": false,
      "keepClientAuthorizationAfterExpiration": false,
      "allowSpontaneousScopes": false,
      "spontaneousScopes": null,
      "spontaneousScopeScriptDns": null,
      "backchannelLogoutUri": null,
      "backchannelLogoutSessionRequired": false,
      "additionalAudience": null,
      "postAuthnScripts": null,
      "consentGatheringScripts": null,
      "introspectionScripts": null,
      "rptClaimsScripts": null
    },
    "backchannelTokenDeliveryMode": null,
    "backchannelClientNotificationEndpoint": null,
    "backchannelAuthenticationRequestSigningAlg": null,
    "backchannelUserCodeParameter": null,
    "expirationDate": null,
    "deletable": false,
    "jansId": null
  },
  {
    "dn": "inum=1201.d71e6b84-b637-4e26-b8d3-34c80934c097,ou=clients,o=jans",
    "inum": "1201.d71e6b84-b637-4e26-b8d3-34c80934c097",
    "clientSecret": "0vFoEhc7Zut2",
    "frontChannelLogoutUri": null,
    "frontChannelLogoutSessionRequired": false,
    "registrationAccessToken": null,
    "clientIdIssuedAt": null,
    "clientSecretExpiresAt": null,
    "redirectUris": null,
    "claimRedirectUris": null,
    "responseTypes": null,
    "grantTypes": [
      "client_credentials"
    ],
    "applicationType": "native",
    "contacts": null,
    "clientName": "SCIM client",
    "idTokenTokenBindingCnf": null,
    "logoUri": null,
    "clientUri": null,
    "policyUri": null,
    "tosUri": null,
    "jwksUri": null,
    "jwks": null,
    "sectorIdentifierUri": null,
    "subjectType": "pairwise",
    "idTokenSignedResponseAlg": null,
    "idTokenEncryptedResponseAlg": null,
    "idTokenEncryptedResponseEnc": null,
    "userInfoSignedResponseAlg": null,
    "userInfoEncryptedResponseAlg": null,
    "userInfoEncryptedResponseEnc": null,
    "requestObjectSigningAlg": null,
    "requestObjectEncryptionAlg": null,
    "requestObjectEncryptionEnc": null,
    "tokenEndpointAuthMethod": "client_secret_basic",
    "tokenEndpointAuthSigningAlg": null,
    "defaultMaxAge": null,
    "requireAuthTime": false,
    "defaultAcrValues": null,
    "initiateLoginUri": null,
    "postLogoutRedirectUris": null,
    "requestUris": null,
    "scopes": [
      "inum=1200.841184,ou=scopes,o=jans",
      "inum=1200.98DDA5,ou=scopes,o=jans",
      "inum=1200.F40A49,ou=scopes,o=jans",
      "inum=1200.B609F0,ou=scopes,o=jans",
      "inum=1200.492980,ou=scopes,o=jans",
      "inum=1200.F7EC4A,ou=scopes,o=jans",
      "inum=1200.280C97,ou=scopes,o=jans",
      "inum=1200.E236BB,ou=scopes,o=jans",
      "inum=1200.DC0FDE,ou=scopes,o=jans",
      "inum=1200.2483ED,ou=scopes,o=jans"
    ],
    "claims": null,
    "trustedClient": false,
    "lastAccessTime": null,
    "lastLogonTime": null,
    "persistClientAuthorizations": false,
    "includeClaimsInIdToken": false,
    "refreshTokenLifetime": null,
    "accessTokenLifetime": null,
    "customAttributes": [],
    "customObjectClasses": [
      "top"
    ],
    "rptAsJwt": false,
    "accessTokenAsJwt": false,
    "accessTokenSigningAlg": "RS256",
    "disabled": false,
    "authorizedOrigins": null,
    "softwareId": null,
    "softwareVersion": null,
    "softwareStatement": null,
    "attributes": {
      "tlsClientAuthSubjectDn": null,
      "runIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims": false,
      "keepClientAuthorizationAfterExpiration": false,
      "allowSpontaneousScopes": false,
      "spontaneousScopes": null,
      "spontaneousScopeScriptDns": null,
      "backchannelLogoutUri": null,
      "backchannelLogoutSessionRequired": false,
      "additionalAudience": null,
      "postAuthnScripts": null,
      "consentGatheringScripts": null,
      "introspectionScripts": null,
      "rptClaimsScripts": null
    },
    "backchannelTokenDeliveryMode": null,
    "backchannelClientNotificationEndpoint": null,
    "backchannelAuthenticationRequestSigningAlg": null,
    "backchannelUserCodeParameter": null,
    "expirationDate": null,
    "deletable": false,
    "jansId": null
  }
]

Selection: 

```

- **__Create a New OpenID Client__**

To create a new OpenID client, you need two enter '2' from OpenID Menu.
It will ask to enter below information:

- frontChannelLogoutSessionRequired[false, true]
- applicationType[web, native]
- clientName
- subjectType[pairwise, public]
- includeClaimsInIdToken[false, true]
- Populate optional fields?[y, n]

If you enter `y` to **Populate optional fields?** then you will get a lot of optional fields are listed below:

```
Populate optional fields? y
Optiaonal Fields:
1 clientSecret
2 frontChannelLogoutUri
3 registrationAccessToken
4 clientIdIssuedAt
5 clientSecretExpiresAt
6 redirectUris
7 claimRedirectUris
8 responseTypes
9 grantTypes
10 contacts
11 idTokenTokenBindingCnf
12 logoUri
13 clientUri
14 policyUri
15 tosUri
16 jwksUri
17 jwks
18 sectorIdentifierUri
19 idTokenSignedResponseAlg
20 idTokenEncryptedResponseAlg
21 idTokenEncryptedResponseEnc
22 userInfoSignedResponseAlg
23 userInfoEncryptedResponseAlg
24 userInfoEncryptedResponseEnc
25 requestObjectSigningAlg
26 requestObjectEncryptionAlg
27 requestObjectEncryptionEnc
28 tokenEndpointAuthMethod
29 tokenEndpointAuthSigningAlg
30 defaultMaxAge
31 requireAuthTime
32 defaultAcrValues
33 initiateLoginUri
34 postLogoutRedirectUris
35 requestUris
36 scopes
37 claims
38 trustedClient
39 lastAccessTime
40 lastLogonTime
41 persistClientAuthorizations
42 refreshTokenLifetime
43 accessTokenLifetime
44 customAttributes
45 customObjectClasses
46 rptAsJwt
47 accessTokenAsJwt
48 accessTokenSigningAlg
49 disabled
50 authorizedOrigins
51 softwareId
52 softwareVersion
53 softwareStatement
54 attributes
55 backchannelTokenDeliveryMode
56 backchannelClientNotificationEndpoint
57 backchannelAuthenticationRequestSigningAlg
58 backchannelUserCodeParameter
59 expirationDate
60 deletable
61 jansId

«c: continue, #: populate filed. »

Selection: 1

«The client secret.  The client MAY omit the parameter if the client secret is an empty string. Type: string»
clientSecret: aabbccdd

«c: continue, #: populate filed. »
Selection: c

Obtained Data:

{
  "dn": null,
  "inum": null,
  "clientSecret": "aabbccdd",
  "frontChannelLogoutUri": null,
  "frontChannelLogoutSessionRequired": false,
  "registrationAccessToken": null,
  "clientIdIssuedAt": null,
  "clientSecretExpiresAt": null,
  "redirectUris": null,
  "claimRedirectUris": null,
  "responseTypes": null,
  "grantTypes": null,
  "applicationType": "web",
  "contacts": null,
  "clientName": "newOID",
  "idTokenTokenBindingCnf": null,
  "logoUri": null,
  "clientUri": null,
  "policyUri": null,
  "tosUri": null,
  "jwksUri": null,
  "jwks": null,
  "sectorIdentifierUri": null,
  "subjectType": "pairwise",
  "idTokenSignedResponseAlg": null,
  "idTokenEncryptedResponseAlg": null,
  "idTokenEncryptedResponseEnc": null,
  "userInfoSignedResponseAlg": null,
  "userInfoEncryptedResponseAlg": null,
  "userInfoEncryptedResponseEnc": null,
  "requestObjectSigningAlg": null,
  "requestObjectEncryptionAlg": null,
  "requestObjectEncryptionEnc": null,
  "tokenEndpointAuthMethod": null,
  "tokenEndpointAuthSigningAlg": null,
  "defaultMaxAge": null,
  "requireAuthTime": null,
  "defaultAcrValues": null,
  "initiateLoginUri": null,
  "postLogoutRedirectUris": null,
  "requestUris": null,
  "scopes": null,
  "claims": null,
  "trustedClient": false,
  "lastAccessTime": null,
  "lastLogonTime": null,
  "persistClientAuthorizations": null,
  "includeClaimsInIdToken": false,
  "refreshTokenLifetime": null,
  "accessTokenLifetime": null,
  "customAttributes": null,
  "customObjectClasses": null,
  "rptAsJwt": null,
  "accessTokenAsJwt": null,
  "accessTokenSigningAlg": null,
  "disabled": false,
  "authorizedOrigins": null,
  "softwareId": null,
  "softwareVersion": null,
  "softwareStatement": null,
  "attributes": null,
  "backchannelTokenDeliveryMode": null,
  "backchannelClientNotificationEndpoint": null,
  "backchannelAuthenticationRequestSigningAlg": null,
  "backchannelUserCodeParameter": null,
  "expirationDate": null,
  "deletable": false,
  "jansId": null
}

Continue? y

Getting access token for scope https://jans.io/oauth/config/openid/clients.write
Please wait while posting data ...

{
  "dn": "inum=1929a64c-6f67-4399-bdd3-6a8d44cc04ae,ou=clients,o=jans",
  "inum": "1929a64c-6f67-4399-bdd3-6a8d44cc04ae",
  "clientSecret": "B3ziSqU8gWTAXICdYfNxw2cP4LmwDqrG1koRqzFxQc0=",
  "frontChannelLogoutUri": null,
  "frontChannelLogoutSessionRequired": false,
  "registrationAccessToken": null,
  "clientIdIssuedAt": null,
  "clientSecretExpiresAt": null,
  "redirectUris": null,
  "claimRedirectUris": null,
  "responseTypes": null,
  "grantTypes": [],
  "applicationType": "web",
  "contacts": null,
  "clientName": "newOID",
  "idTokenTokenBindingCnf": null,
  "logoUri": null,
  "clientUri": null,
  "policyUri": null,
  "tosUri": null,
  "jwksUri": null,
  "jwks": null,
  "sectorIdentifierUri": null,
  "subjectType": "pairwise",
  "idTokenSignedResponseAlg": null,
  "idTokenEncryptedResponseAlg": null,
  "idTokenEncryptedResponseEnc": null,
  "userInfoSignedResponseAlg": null,
  "userInfoEncryptedResponseAlg": null,
  "userInfoEncryptedResponseEnc": null,
  "requestObjectSigningAlg": null,
  "requestObjectEncryptionAlg": null,
  "requestObjectEncryptionEnc": null,
  "tokenEndpointAuthMethod": null,
  "tokenEndpointAuthSigningAlg": null,
  "defaultMaxAge": null,
  "requireAuthTime": false,
  "defaultAcrValues": null,
  "initiateLoginUri": null,
  "postLogoutRedirectUris": null,
  "requestUris": null,
  "scopes": null,
  "claims": null,
  "trustedClient": false,
  "lastAccessTime": null,
  "lastLogonTime": null,
  "persistClientAuthorizations": false,
  "includeClaimsInIdToken": false,
  "refreshTokenLifetime": null,
  "accessTokenLifetime": null,
  "customAttributes": [],
  "customObjectClasses": [
    "top"
  ],
  "rptAsJwt": false,
  "accessTokenAsJwt": false,
  "accessTokenSigningAlg": null,
  "disabled": false,
  "authorizedOrigins": null,
  "softwareId": null,
  "softwareVersion": null,
  "softwareStatement": null,
  "attributes": {
    "tlsClientAuthSubjectDn": null,
    "runIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims": false,
    "keepClientAuthorizationAfterExpiration": false,
    "allowSpontaneousScopes": false,
    "spontaneousScopes": null,
    "spontaneousScopeScriptDns": null,
    "backchannelLogoutUri": null,
    "backchannelLogoutSessionRequired": false,
    "additionalAudience": null,
    "postAuthnScripts": null,
    "consentGatheringScripts": null,
    "introspectionScripts": null,
    "rptClaimsScripts": null
  },
  "backchannelTokenDeliveryMode": null,
  "backchannelClientNotificationEndpoint": null,
  "backchannelAuthenticationRequestSigningAlg": null,
  "backchannelUserCodeParameter": null,
  "expirationDate": null,
  "deletable": false,
  "jansId": null
}

Selection: 
```

- **__Update an OpenID Client by its inum__**

If anything you want to update of an OpenID client, you can choose option '3' and enter the `inum` of the OpenID client. Here I've used the `inum=1929a64c-6f67-4399-bdd3-6a8d44cc04ae` of the above OpenID client. After then, You will get a list of fields to choose which one you are going to update.
Here is what you can see in return:

```
Get OpenId Connect Client by Inum
---------------------------------

«inum. Type: string»
inum: 1929a64c-6f67-4399-bdd3-6a8d44cc04ae
Calling Api with parameters: {'inum': '1929a64c-6f67-4399-bdd3-6a8d44cc04ae'}
Please wait while retreiving data ...

Getting access token for scope https://jans.io/oauth/config/openid/clients.readonly
Fields:
 1 accessTokenAsJwt
 2 accessTokenLifetime
 3 accessTokenSigningAlg
 4 applicationType
 5 attributes
 6 authorizedOrigins
 7 backchannelAuthenticationRequestSigningAlg
 8 backchannelClientNotificationEndpoint
 9 backchannelTokenDeliveryMode
10 backchannelUserCodeParameter
11 claimRedirectUris
12 claims
13 clientIdIssuedAt
14 clientName
15 clientSecret
16 clientSecretExpiresAt
17 clientUri
18 contacts
19 customAttributes
20 customObjectClasses
21 defaultAcrValues
22 defaultMaxAge
23 deletable
24 disabled
25 expirationDate
26 frontChannelLogoutSessionRequired
27 frontChannelLogoutUri
28 grantTypes
29 idTokenEncryptedResponseAlg
30 idTokenEncryptedResponseEnc
31 idTokenSignedResponseAlg
32 idTokenTokenBindingCnf
33 includeClaimsInIdToken
34 initiateLoginUri
35 inum
36 jansId
37 jwks
38 jwksUri
39 lastAccessTime
40 lastLogonTime
41 logoUri
42 persistClientAuthorizations
43 policyUri
44 postLogoutRedirectUris
45 redirectUris
46 refreshTokenLifetime
47 registrationAccessToken
48 requestObjectEncryptionAlg
49 requestObjectEncryptionEnc
50 requestObjectSigningAlg
51 requestUris
52 requireAuthTime
53 responseTypes
54 rptAsJwt
55 scopes
56 sectorIdentifierUri
57 softwareId
58 softwareStatement
59 softwareVersion
60 subjectType
61 tokenEndpointAuthMethod
62 tokenEndpointAuthSigningAlg
63 tosUri
64 trustedClient
65 userInfoEncryptedResponseAlg
66 userInfoEncryptedResponseEnc
67 userInfoSignedResponseAlg

«q: quit, v: view, s: save, l: list fields #: update filed. »
Selection: 
```

- __q__ to quit 
- __v__ to view each attribute with updated data
- __l__ to get list of fields 
- __#__ to update filed attribute
- __id__ number of an attribute to identify which one you want to update



**__Get OpenID client by its inum__**

`inum` is an unique identity of an OpenID client. You can use `inum` of an OpenID client to get details informaton.

In my case i'm using the `inum` of the above created OpenID client:

```
Get OpenId Connect Client by Inum
---------------------------------

«inum. Type: string»
inum: 1929a64c-6f67-4399-bdd3-6a8d44cc04ae
Calling Api with parameters: {'inum': '1929a64c-6f67-4399-bdd3-6a8d44cc04ae'}
Please wait while retreiving data ...

Getting access token for scope https://jans.io/oauth/config/openid/clients.readonly
{
  "dn": "inum=1929a64c-6f67-4399-bdd3-6a8d44cc04ae,ou=clients,o=jans",
  "inum": "1929a64c-6f67-4399-bdd3-6a8d44cc04ae",
  "clientSecret": "CpTCvlZZsQDWShGrMXFBzQ==",
  "frontChannelLogoutUri": null,
  "frontChannelLogoutSessionRequired": false,
  "registrationAccessToken": null,
  "clientIdIssuedAt": null,
  "clientSecretExpiresAt": null,
  "redirectUris": null,
  "claimRedirectUris": null,
  "responseTypes": null,
  "grantTypes": [],
  "applicationType": "web",
  "contacts": null,
  "clientName": "newOID",
  "idTokenTokenBindingCnf": null,
  "logoUri": null,
  "clientUri": null,
  "policyUri": null,
  "tosUri": null,
  "jwksUri": null,
  "jwks": null,
  "sectorIdentifierUri": null,
  "subjectType": "pairwise",
  "idTokenSignedResponseAlg": null,
  "idTokenEncryptedResponseAlg": null,
  "idTokenEncryptedResponseEnc": null,
  "userInfoSignedResponseAlg": null,
  "userInfoEncryptedResponseAlg": null,
  "userInfoEncryptedResponseEnc": null,
  "requestObjectSigningAlg": null,
  "requestObjectEncryptionAlg": null,
  "requestObjectEncryptionEnc": null,
  "tokenEndpointAuthMethod": null,
  "tokenEndpointAuthSigningAlg": null,
  "defaultMaxAge": null,
  "requireAuthTime": false,
  "defaultAcrValues": null,
  "initiateLoginUri": null,
  "postLogoutRedirectUris": null,
  "requestUris": null,
  "scopes": null,
  "claims": null,
  "trustedClient": false,
  "lastAccessTime": null,
  "lastLogonTime": null,
  "persistClientAuthorizations": false,
  "includeClaimsInIdToken": false,
  "refreshTokenLifetime": null,
  "accessTokenLifetime": null,
  "customAttributes": [],
  "customObjectClasses": [
    "top"
  ],
  "rptAsJwt": false,
  "accessTokenAsJwt": false,
  "accessTokenSigningAlg": null,
  "disabled": false,
  "authorizedOrigins": null,
  "softwareId": null,
  "softwareVersion": null,
  "softwareStatement": null,
  "attributes": {
    "tlsClientAuthSubjectDn": null,
    "runIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims": false,
    "keepClientAuthorizationAfterExpiration": false,
    "allowSpontaneousScopes": false,
    "spontaneousScopes": null,
    "spontaneousScopeScriptDns": null,
    "backchannelLogoutUri": null,
    "backchannelLogoutSessionRequired": false,
    "additionalAudience": null,
    "postAuthnScripts": null,
    "consentGatheringScripts": null,
    "introspectionScripts": null,
    "rptClaimsScripts": null
  },
  "backchannelTokenDeliveryMode": null,
  "backchannelClientNotificationEndpoint": null,
  "backchannelAuthenticationRequestSigningAlg": null,
  "backchannelUserCodeParameter": null,
  "expirationDate": null,
  "deletable": false,
  "jansId": null
}

Selection: 
```

<!-- 

(removed since sector identifier is depcreated and does not include in Janssen Server anymore)

### OpenD Connect - Sector Identifier

Sector identifiers provide a way to group clients from the same adminstrative domain using pairwise subject identifiers. In this case, each client needs to be given the same pairwise ID for the person to maintain continuity across all the related websites

With `jans-cli` you can do such operations:

```
OAuth - OpenID Connect - Sector Identifiers
-------------------------------------------
1 Gets list of OpenID Connect Sectors
2 Create new OpenID Connect Sector
3 Update OpenId Connect Sector
4 Get OpenID Connect Sector by Inum
5 Delete OpenID Connect Sector
6 Partially update OpenId Connect Sector by Inum
```

**Get list of OpenID Connect Sectors**

To get list of OpenID Connect sectors, go with the first option. It will display all the connect sectors available in your janssen server.

```
Gets list of OpenID Connect Sectors
-----------------------------------
Please wait while retreiving data ...

Getting access token for scope https://jans.io/oauth/config/openid/sectoridentifiers.readonly
[
  {
    "id": "26fba854-7adf-4a81-99d2-b772aa053b93",
    "description": "Testing OpenID Connect Sector",
    "redirectUris": [
      "https://test.gluu.org"
    ],
    "clientIds": [
      "1801.86c324cc-621f-477f-836d-09fcd720353e"
    ]
  },
  {
    "id": "53337668-f721-4bc0-9d04-a249f38d0def",
    "description": "Testing sector identifier",
    "redirectUris": [
      "https://test.gluu.com"
    ],
    "clientIds": null
  }
]


```

**Create new OpenID Sector Identifier**

There are some specific follow-up method to create an OpenID sector identifier.

1. It will ask to enter an Unique ID (ex; 'test')
2. It will ask to update optional fields. `enter` 'y' to confirm or 'n' to skip.
3. If you enter 'y', It will show some optional fields as below:

    - 1 description
    - 2 redirectUris
    - 3 clientIds

4. After completing 3rd steps enter `c` to continue. It will show all the data you provided.
5. At the end it will ask for the confirmation to save it. enter `y` to continue. 


> description: Add some information related to the sector identifier

> redirectUris: Add redirect url here

> clientIds: add list of the client ID linked with sector identifier

Please, see below results to better understand.

```
Selection: 2

«XRI i-number. Sector Identifier to uniquely identify the sector. Type: string»
id: test

Populate optional fields? y
Optiaonal Fields:
1 description
2 redirectUris
3 clientIds

«c: continue, #: populate filed. »
Selection: 1

«A human-readable string describing the sector. Type: string»
description: Testing sector identifier

«c: continue, #: populate filed. »
Selection: 2

«Redirection URI values used by the Client. One of these registered Redirection URI values must exactly match the redirect_uri parameter value used in each Authorization Request. Type: array of string separated by _,»
Example: https://client.example.org/cb
redirectUris: https://test.gluu.com/back

«c: continue, #: populate filed. »
Selection: 3

«List of OAuth 2.0 Client Identifier valid at the Authorization Server. Type: array of string separated by _,»
clientIds: 

«c: continue, #: populate filed. »
Selection: c
Obtained Data:

{
  "id": "test",
  "description": "Testing sector identifier",
  "redirectUris": [
    "https://test.gluu.com/back"
  ],
  "clientIds": []
}

Continue? y
Getting access token for scope https://jans.io/oauth/config/openid/sectoridentifiers.write
Please wait while posting data ...

{
  "id": "1102af41-6b2e-4d65-b2fd-620675e1efe3",
  "description": "Testing sector identifier",
  "redirectUris": [
    "https://test.gluu.com/back"
  ],
  "clientIds": null
}
```

**Update OpenID Connect Sector**

To update an OpenID Connect sector identifier, you need an `inum` of a sector identifier which one you want to update. Let's say, We are going to update the 2nd identifier from the above list of OpenID connect sector identifer, its id: `53337668-f721-4bc0-9d04-a249f38d0def`.

Choosing the 3rd option from the Sector Identifier Menu:
- It will ask to enter the id of a Sector Identifier
- After then, It comes with some fields

    Fields:
    
      1. clientIds
      2. description
      3. id
      4. redirectUris

- select each of the field to update it. Here, We are going to update `clientIds` with this value: `1801.86c324cc-621f-477f-836d-09fcd720353e`

- If update is done, then enter `s` to save the changes.
- Finally, It will ask to enter `y` for the confirmation. 
- and at the end, it will show all updated result as below:

```
Continue? y
Please wait while posting data ...

Getting access token for scope https://jans.io/oauth/config/openid/sectoridentifiers.write
{
  "id": "53337668-f721-4bc0-9d04-a249f38d0def",
  "description": "Testing sector identifier",
  "redirectUris": [
    "https://test.gluu.com"
  ],
  "clientIds": [
    "1801.86c324cc-621f-477f-836d-09fcd720353e"
  ]
}

Selection: 

```

**Get OpenID Connect Sector Identifier by inum**

Simply enter an `id` of a Sector Identifier, It will retrieve data and display on the monitor.


```
Get OpenID Connect Sector by Inum
---------------------------------

«id. Type: string»
id: 53337668-f721-4bc0-9d04-a249f38d0def
Calling Api with parameters: {'id': '53337668-f721-4bc0-9d04-a249f38d0def'}
Please wait while retreiving data ...

Getting access token for scope https://jans.io/oauth/config/openid/sectoridentifiers.readonly
{
  "id": "53337668-f721-4bc0-9d04-a249f38d0def",
  "description": "Testing sector identifier",
  "redirectUris": [
    "https://test.gluu.com"
  ],
  "clientIds": [
    "1801.86c324cc-621f-477f-836d-09fcd720353e"
  ]
}

Selection: 
```

**Delete OpenID Connect Sector**

To delete an OpenID Connect Sector by its id, choose option 5 from the Sector Identifier Menu. Then enter `id` which one you are going to delete. Here, We are going to delete a Sector Identifier with `id:53337668-f721-4bc0-9d04-a249f38d0def`. press `y` for the confirmation. It will delete entry from the server.

-->

### User Managed Access (UMA)

UMA helps to manage user authorization. Using IM, you can easily maintain UMA resources. If you select `16` option on the IM Menu, you will get a list as below:

![](img/im-uma-menu.png)

you can perform such operations:
- view/find
- create
- update / partially update
- delete

To get a list of UMA resources of your Janssen Server, you can select option 1.
It will ask `search limit[50]` by default set up to 50 and `pattern` of string.


### OAuth - Scopes

In OAuth, scopes are used to specify the extent of access. For an OpenID Connect sign-in flow, scopes correspond to the release of user claims.
`jans-cli` supports the following operations through Interactive Mode.

```
OAuth - Scopes
--------------
1 Gets list of Scopes
2 Create Scope
3 Updates existing Scope
4 Get Scope by Inum
5 Delete Scope
6 Update modified attributes of existing Scope by Inum

Selection: 
```

- **__Gets list of Scopes__**

To view the current list of scopes of the Janssen Server, choose the first option from the following menu. It will ask to enter `type`, `limit` & `pattern` to filter in searching. You may skip by pressing `'Enter'` key to get all the scopes of the server. In my case, I have set `limit` upto 5.

```
«Scope type. Type: string»
type: 

«Search size - max size of the results to return. Type: integer»
limit  [50]: 5

«Search pattern. Type: string»
pattern: 
Calling Api with parameters: {'limit': 5}
Please wait while retreiving data ...

Getting access token for scope https://jans.io/oauth/config/scopes.readonly
[
  {
    "dn": "inum=F0C4,ou=scopes,o=jans",
    "inum": "F0C4",
    "displayName": "authenticate_openid_connect",
    "id": "openid",
    "iconUrl": null,
    "description": "Authenticate using OpenID Connect.",
    "scopeType": "openid",
    "claims": null,
    "defaultScope": true,
    "groupClaims": null,
    "dynamicScopeScripts": null,
    "umaAuthorizationPolicies": null,
    "attributes": {
      "spontaneousClientId": null,
      "spontaneousClientScopes": null,
      "showInConfigurationEndpoint": true
    },
    "umaType": false,
    "deletable": false,
    "expirationDate": null
  },
  {
    "dn": "inum=43F1,ou=scopes,o=jans",
    "inum": "43F1",
    "displayName": "view_profile",
    "id": "profile",
    "iconUrl": null,
    "description": "View your basic profile info.",
    "scopeType": "openid",
    "claims": [
      "inum=2B29,ou=attributes,o=jans",
      "inum=0C85,ou=attributes,o=jans",
      "inum=B4B0,ou=attributes,o=jans",
      "inum=A0E8,ou=attributes,o=jans",
      "inum=5EC6,ou=attributes,o=jans",
      "inum=B52A,ou=attributes,o=jans",
      "inum=64A0,ou=attributes,o=jans",
      "inum=EC3A,ou=attributes,o=jans",
      "inum=3B47,ou=attributes,o=jans",
      "inum=3692,ou=attributes,o=jans",
      "inum=98FC,ou=attributes,o=jans",
      "inum=A901,ou=attributes,o=jans",
      "inum=36D9,ou=attributes,o=jans",
      "inum=BE64,ou=attributes,o=jans",
      "inum=6493,ou=attributes,o=jans"
    ],
    "defaultScope": false,
    "groupClaims": null,
    "dynamicScopeScripts": null,
    "umaAuthorizationPolicies": null,
    "attributes": {
      "spontaneousClientId": null,
      "spontaneousClientScopes": null,
      "showInConfigurationEndpoint": true
    },
    "umaType": false,
    "deletable": false,
    "expirationDate": null
  },
  {
    "dn": "inum=D491,ou=scopes,o=jans",
    "inum": "D491",
    "displayName": "view_phone_number",
    "id": "phone",
    "iconUrl": null,
    "description": "View your phone number.",
    "scopeType": "openid",
    "claims": [
      "inum=B17A,ou=attributes,o=jans",
      "inum=0C18,ou=attributes,o=jans"
    ],
    "defaultScope": false,
    "groupClaims": null,
    "dynamicScopeScripts": null,
    "umaAuthorizationPolicies": null,
    "attributes": {
      "spontaneousClientId": null,
      "spontaneousClientScopes": null,
      "showInConfigurationEndpoint": true
    },
    "umaType": false,
    "deletable": false,
    "expirationDate": null
  },
  {
    "dn": "inum=C17A,ou=scopes,o=jans",
    "inum": "C17A",
    "displayName": "view_address",
    "id": "address",
    "iconUrl": null,
    "description": "View your address.",
    "scopeType": "openid",
    "claims": [
      "inum=27DB,ou=attributes,o=jans",
      "inum=2A3D,ou=attributes,o=jans",
      "inum=6609,ou=attributes,o=jans",
      "inum=6EEB,ou=attributes,o=jans",
      "inum=BCE8,ou=attributes,o=jans",
      "inum=D90B,ou=attributes,o=jans",
      "inum=E6B8,ou=attributes,o=jans",
      "inum=E999,ou=attributes,o=jans"
    ],
    "defaultScope": false,
    "groupClaims": true,
    "dynamicScopeScripts": null,
    "umaAuthorizationPolicies": null,
    "attributes": {
      "spontaneousClientId": null,
      "spontaneousClientScopes": null,
      "showInConfigurationEndpoint": true
    },
    "umaType": false,
    "deletable": false,
    "expirationDate": null
  },
  {
    "dn": "inum=764C,ou=scopes,o=jans",
    "inum": "764C",
    "displayName": "view_email_address",
    "id": "email",
    "iconUrl": null,
    "description": "View your email address.",
    "scopeType": "openid",
    "claims": [
      "inum=8F88,ou=attributes,o=jans",
      "inum=CAE3,ou=attributes,o=jans"
    ],
    "defaultScope": false,
    "groupClaims": null,
    "dynamicScopeScripts": null,
    "umaAuthorizationPolicies": null,
    "attributes": {
      "spontaneousClientId": null,
      "spontaneousClientScopes": null,
      "showInConfigurationEndpoint": true
    },
    "umaType": false,
    "deletable": false,
    "expirationDate": null
  }
]

Selection: 

```

- **__create scope__**

You can create a scope through the command line interface.
It will ask to enter value for each property.


```

Selection: 2

«A human-readable name of the scope. Type: string»
displayName: testScope

«The base64url encoded id. Type: string»
id: tScope

«A human-readable string describing the scope. Type: string»
description: creating scope

«The scopes type associated with Access Tokens determine what resources will. Type: string»
scopeType: openid

Populate optional fields? n
Obtained Data:

{
  "dn": null,
  "inum": null,
  "displayName": "testScope",
  "id": "tScope",
  "iconUrl": null,
  "description": "creating scope",
  "scopeType": "openid",
  "claims": null,
  "defaultScope": null,
  "groupClaims": null,
  "dynamicScopeScripts": null,
  "umaAuthorizationPolicies": null,
  "attributes": null,
  "umaType": false,
  "deletable": false,
  "expirationDate": null
}

Continue? y
Getting access token for scope https://jans.io/oauth/config/scopes.write
Please wait while posting data ...

{
  "dn": "inum=070daa9e-4a8f-423a-8681-f578673a2781,ou=scopes,o=jans",
  "inum": "070daa9e-4a8f-423a-8681-f578673a2781",
  "displayName": "testScope",
  "id": "tScope",
  "iconUrl": null,
  "description": "creating scope",
  "scopeType": "openid",
  "claims": null,
  "defaultScope": null,
  "groupClaims": null,
  "dynamicScopeScripts": null,
  "umaAuthorizationPolicies": null,
  "attributes": {
    "spontaneousClientId": null,
    "spontaneousClientScopes": null,
    "showInConfigurationEndpoint": true
  },
  "umaType": false,
  "deletable": false,
  "expirationDate": null
}

Selection: 
```


### User

This option can be used to perform such operations to modfiy user resources. If you select the first option from the SCIM Menu, You will get a list of sub-menu as same as below.

```

user
----
1 Query User resources (see section 3.4.2 of RFC 7644)
2 Allows creating a User resource via POST (see section 3.3 of RFC 7644)
3 Retrieves a User resource by Id (see section 3.4.1 of RFC 7644)
4 Updates a User resource (see section 3.5.1 of RFC 7644). Update works in a replacement fashion&amp;#58; every
attribute value found in the payload sent will replace the one in the existing resource representation. Attributes 
not passed in the payload will be left intact.

5 Deletes a user resource
6 Updates one or more attributes of a User resource using a sequence of additions, removals, and 
replacements operations. See section 3.5.2 of RFC 7644

7 Query User resources (see section 3.4.2 of RFC 7644)

Selection: 

```

1. **Query User Resources**: Query User Resources presents all the user information and its attributes.  It supprts query with filter by a list of attributes:

    1. **attributes**: Use comma (,) for multiple attributes
    2. **excludeAttributes**: Use comma (,) for multiple attributes
    3. **filter**: an attribute with value to return as same type of resources
    4. **startIndex**: an integer value indicate a starting position
    5. **count**: an integer value define the maximum search results
    6. **sortBy**: sort list of search results by an attribute
    7. **sortOrder**: ['ascending', 'descending']

A simple query where everything is skipped for default value. 

```

Query User resources (see section 3.4.2 of RFC 7644)
----------------------------------------------------

«A comma-separated list of attribute names to return in the response. Type: string»
attributes: 

«When specified, the response will contain a default set of attributes minus those listed here (as a comma-separated list). Type: string»
excludedAttributes: 

«An expression specifying the search criteria. See section 3.4.2.2 of RFC 7644. Type: string»
filter: 

«The 1-based index of the first query result. Type: integer»
startIndex: 

«Specifies the desired maximum number of query results per page. Type: integer»
count: 1

«The attribute whose value will be used to order the returned responses. Type: string»
sortBy: 

«Order in which the sortBy param is applied. Allowed values are "ascending" and "descending". Type: string»
sortOrder: 
Calling Api with parameters: {'count': 1}
Please wait while retreiving data ...

Getting access token for scope https://jans.io/scim/users.read
{
  "Resources": [
    {
      "externalId": null,
      "userName": "admin",
      "name": {
        "familyName": "User",
        "givenName": "Admin",
        "middleName": "Admin",
        "honorificPrefix": null,
        "honorificSuffix": null,
        "formatted": "Admin Admin User"
      },
      "displayName": "Default Admin User",
      "nickName": "Admin",
      "profileUrl": null,
      "title": null,
      "userType": null,
      "preferredLanguage": null,
      "locale": null,
      "timezone": null,
      "active": true,
      "password": null,
      "emails": [
        {
          "value": "admin@testjans.gluu.com",
          "display": null,
          "type": null,
          "primary": false
        }
      ],
      "phoneNumbers": null,
      "ims": null,
      "photos": null,
      "addresses": null,
      "groups": [
        {
          "value": "60B7",
          "$ref": "https://testjans.gluu.com/jans-scim/restv1/v2/Groups/60B7",
          "display": "Jannsen Manager Group",
          "type": "direct"
        }
      ],
      "entitlements": null,
      "roles": null,
      "x509Certificates": null,
      "urn:ietf:params:scim:schemas:extension:gluu:2.0:User": null,
      "schemas": [
        "urn:ietf:params:scim:schemas:core:2.0:User"
      ],
      "id": "18ca6089-42fb-410a-a5b5-c2631d75dc7d",
      "meta": {
        "resourceType": "User",
        "created": null,
        "lastModified": null,
        "location": "https://testjans.gluu.com/jans-scim/restv1/v2/Users/18ca6089-42fb-410a-a5b5-c2631d75dc7d"
      }
    }
  ],
  "schemas": [
    "urn:ietf:params:scim:api:messages:2.0:ListResponse"
  ],
  "totalResults": 1,
  "startIndex": 1,
  "itemsPerPage": 1
}

Selection: 

```

2. **Creating an User**: With this option, An adminstration can create  user resources easily. To create an user, you need to provide user value for its attributes. List of attributes are given below:

    1. familyName
    2. givenName
    3. middleName
    4. honorificPrefix
    5. honorificSuffix
    6. displayName
    7. password
    8. Email

    
  Optiaonal Fields:

      1 schemas
      2 id
      3 meta
      4 externalId
      5 nickName
      6 profileUrl
      7 title
      8 userType
      9 preferredLanguage
      10 locale
      11 timezone
      12 active
      13 phoneNumbers
      14 ims
      15 photos
      16 addresses
      17 groups
      18 entitlements
      19 roles
      20 x509Certificates
      21 urn:ietf:params:scim:schemas:extension:gluu:2.0:User

You can skip less important attributes if you want. Please see below follow-up method to create an user.

```

Selection: 2

«Identifier for the user, typically used by the user to directly authenticate (id and externalId are opaque identifiers generally not known by users). Type: string»
userName: shakil

Data for object name. See section 4.1.1 of RFC 7643

   «Type: string»
   familyName: shakil

   «Type: string»
   givenName: shakil

   «Type: string»
   middleName: shakil

   «A "title" like "Ms.", "Mrs.". Type: string»
   honorificPrefix: Mr.

   «Name suffix, like "Junior", "The great", "III". Type: string»
   honorificSuffix: Miah

   «Full name, including all middle names, titles, and suffixes as appropriate. Type: string»
   formatted: 

«Name of the user suitable for display to end-users. Type: string»
displayName: shakil

«Type: string»
password: password

«See section 4.1.2 of RFC 7643. »
Add Email? shakil@gluu.org
Please enter one of y, n
Add Email? n

Populate optional fields? y
Optiaonal Fields:
1 schemas
2 id
3 meta
4 externalId
5 nickName
6 profileUrl
7 title
8 userType
9 preferredLanguage
10 locale
11 timezone
12 active
13 phoneNumbers
14 ims
15 photos
16 addresses
17 groups
18 entitlements
19 roles
20 x509Certificates
21 urn:ietf:params:scim:schemas:extension:gluu:2.0:User

«c: continue, #: populate filed. »
Selection: c
Obtained Data:

{
  "externalId": null,
  "userName": "shakil",
  "name": {
    "familyName": "shakil",
    "givenName": "shakil",
    "middleName": "shakil",
    "honorificPrefix": "Mr.",
    "honorificSuffix": "Miah",
    "formatted": null
  },
  "displayName": "shakil",
  "nickName": null,
  "profileUrl": null,
  "title": null,
  "userType": null,
  "preferredLanguage": null,
  "locale": null,
  "timezone": null,
  "active": null,
  "password": "12345678",
  "emails": [],
  "phoneNumbers": null,
  "ims": null,
  "photos": null,
  "addresses": null,
  "groups": null,
  "entitlements": null,
  "roles": null,
  "x509Certificates": null,
  "urn:ietf:params:scim:schemas:extension:gluu:2.0:User": null,
  "schemas": null,
  "id": null,
  "meta": null
}

Continue? y
Getting access token for scope https://jans.io/scim/users.write
Please wait while posting data ...

{
  "externalId": null,
  "userName": "shakil",
  "name": {
    "familyName": "shakil",
    "givenName": "shakil",
    "middleName": "shakil",
    "honorificPrefix": "Mr.",
    "honorificSuffix": "Miah",
    "formatted": "Mr. shakil shakil shakil Miah"
  },
  "displayName": "shakil",
  "nickName": null,
  "profileUrl": null,
  "title": null,
  "userType": null,
  "preferredLanguage": null,
  "locale": null,
  "timezone": null,
  "active": null,
  "password": null,
  "emails": [],
  "phoneNumbers": null,
  "ims": null,
  "photos": null,
  "addresses": null,
  "groups": null,
  "entitlements": null,
  "roles": null,
  "x509Certificates": null,
  "urn:ietf:params:scim:schemas:extension:gluu:2.0:User": null,
  "schemas": [
    "urn:ietf:params:scim:schemas:core:2.0:User"
  ],
  "id": "7881ed5c-1dad-4265-9b74-ee6c3932c11f",
  "meta": {
    "resourceType": "User",
    "created": "2021-03-29T19:04:52.353Z",
    "lastModified": "2021-03-29T19:04:52.353Z",
    "location": "https://testjans.gluu.com/jans-scim/restv1/v2/Users/7881ed5c-1dad-4265-9b74-ee6c3932c11f"
  }
}

Selection: 

```

3. **Retrives an User Resources by its ID**: You can retrieve an user resources by its ID. Also it supports filter in searching means you can choose list of attributes you want to retrieve and exclude list of attributes that you don't want to retrieve. Here, I have skipped for each property to retrieve all its attributes.

```
Retrieves a User resource by Id (see section 3.4.1 of RFC 7644)
---------------------------------------------------------------

«id. Type: string»
id: 7881ed5c-1dad-4265-9b74-ee6c3932c11f

«A comma-separated list of attribute names to return in the response. Type: string»
attributes: 

«When specified, the response will contain a default set of attributes minus those listed here (as a comma-separated list). Type: string»
excludedAttributes: 
Calling Api with parameters: {'id': '7881ed5c-1dad-4265-9b74-ee6c3932c11f'}
Please wait while retreiving data ...

Getting access token for scope https://jans.io/scim/users.read
{
  "externalId": null,
  "userName": "shakil",
  "name": {
    "familyName": "shakil",
    "givenName": "shakil",
    "middleName": "shakil",
    "honorificPrefix": "Mr.",
    "honorificSuffix": "Miah",
    "formatted": "Mr. shakil shakil shakil Miah"
  },
  "displayName": "shakil",
  "nickName": null,
  "profileUrl": null,
  "title": null,
  "userType": null,
  "preferredLanguage": null,
  "locale": null,
  "timezone": null,
  "active": false,
  "password": null,
  "emails": null,
  "phoneNumbers": null,
  "ims": null,
  "photos": null,
  "addresses": null,
  "groups": null,
  "entitlements": null,
  "roles": null,
  "x509Certificates": null,
  "urn:ietf:params:scim:schemas:extension:gluu:2.0:User": null,
  "schemas": [
    "urn:ietf:params:scim:schemas:core:2.0:User"
  ],
  "id": "7881ed5c-1dad-4265-9b74-ee6c3932c11f",
  "meta": {
    "resourceType": "User",
    "created": "2021-03-29T19:04:52.353Z",
    "lastModified": "2021-03-29T19:04:52.353Z",
    "location": "https://testjans.gluu.com/jans-scim/restv1/v2/Users/7881ed5c-1dad-4265-9b74-ee6c3932c11f"
  }
}

Selection: 
```

4. **Update an User resource**: You can update an user resources by its ID also. If you enter an ID of an user resource, It will show a list of attributes. You can select any of theme one by one to update each value of its property. 

```

Retrieves a User resource by Id (see section 3.4.1 of RFC 7644)
---------------------------------------------------------------

«id. Type: string»
id: 7881ed5c-1dad-4265-9b74-ee6c3932c11f
Calling Api with parameters: {'id': '7881ed5c-1dad-4265-9b74-ee6c3932c11f'}
Please wait while retreiving data ...

Getting access token for scope https://jans.io/scim/users.read
Fields:
 1 active
 2 addresses
 3 displayName
 4 emails
 5 entitlements
 6 externalId
 7 groups
 8 id
 9 ims
10 locale
11 meta
12 name
13 nickName
14 password
15 phoneNumbers
16 photos
17 preferredLanguage
18 profileUrl
19 roles
20 schemas
21 timezone
22 title
23 urn:ietf:params:scim:schemas:extension:gluu:2.0:User
24 userName
25 userType
26 x509Certificates

«q: quit, v: view, s: save, l: list fields #: update filed. »
Selection: 

```
Let's say we are going to change the user `active` status, there is a follow-up process: 

```
Selection: 1

«Type: boolean»
active  [false]: true
Please enter a(n) boolean value: _true, _false
active  [false]: _true

«q: quit, v: view, s: save, l: list fields #: update filed. »
Selection: s
Changes:
active: True

Continue? y
Please wait while posting data ...

Getting access token for scope https://jans.io/scim/users.write
{
  "externalId": null,
  "userName": "shakil",
  "name": {
    "familyName": "shakil",
    "givenName": "shakil",
    "middleName": "shakil",
    "honorificPrefix": "Mr.",
    "honorificSuffix": "Miah",
    "formatted": "Mr. shakil shakil shakil Miah"
  },
  "displayName": "shakil",
  "nickName": null,
  "profileUrl": null,
  "title": null,
  "userType": null,
  "preferredLanguage": null,
  "locale": null,
  "timezone": null,
  "active": true,
  "password": null,
  "emails": null,
  "phoneNumbers": null,
  "ims": null,
  "photos": null,
  "addresses": null,
  "groups": null,
  "entitlements": null,
  "roles": null,
  "x509Certificates": null,
  "urn:ietf:params:scim:schemas:extension:gluu:2.0:User": null,
  "schemas": [
    "urn:ietf:params:scim:schemas:core:2.0:User"
  ],
  "id": "7881ed5c-1dad-4265-9b74-ee6c3932c11f",
  "meta": {
    "resourceType": "User",
    "created": "2021-03-29T19:04:52.353Z",
    "lastModified": "2021-04-01T22:45:15.804Z",
    "location": "https://testjans.gluu.com/jans-scim/restv1/v2/Users/7881ed5c-1dad-4265-9b74-ee6c3932c11f"
  }
}

Selection: 

```

This is how you can update each of its attributes.


5. **Delete an user resource**: If you want to delete an entry from user resources, you can do that thing easily using the Interatice Mode of Janssen CLI. To delete an user entry, you need to provide its `inum`. In our case: It's `id=7881ed5c-1dad-4265-9b74-ee6c3932c11f` which one are going to be deleted. After then, it will ask for the confirmation, just enter 'y' to delete. Please see below result to better understand.

```
Selection: 5

«Entry to be deleted. »
id: 7881ed5c-1dad-4265-9b74-ee6c3932c11f

Are you sure want to delete 7881ed5c-1dad-4265-9b74-ee6c3932c11f ? y
Getting access token for scope https://jans.io/scim/users.write
Please wait while deleting 7881ed5c-1dad-4265-9b74-ee6c3932c11f ...


Entry 7881ed5c-1dad-4265-9b74-ee6c3932c11f was deleted successfully


Selection: 

```

6. **Updates user resources using operation mode**: This is an alternative option to update user resources. To use this option, you need to consider the following things: 

    - **id**: an unique id of user resources
    - **op**: one operation to be done from [add, remove, replace] 
    - **path**: an attribute path where this operation to be done.
    - **value**: any string type value to `add` or `replace`.

This is an example to add `title` which `id: 18ca6089-42fb-410a-a5b5-c2631d75dc7d` 

```
Selection: 6

«Entry to be patched. »
id: 18ca6089-42fb-410a-a5b5-c2631d75dc7d

«The kind of operation to perform. Type: string»
op: add

«Required when op is remove, optional otherwise. Type: string»
path: title

«Only required when op is add or replace. Type: string»
value: Admin

Patch another param? n
[
  {
    "op": "add",
    "path": "title",
    "value": "Admin"
  }
]

Continue? y
Getting access token for scope https://jans.io/scim/users.write
Please wait patching...

{
  "externalId": null,
  "userName": "admin",
  "name": {
    "familyName": "User",
    "givenName": "Admin",
    "middleName": "Admin",
    "honorificPrefix": null,
    "honorificSuffix": null,
    "formatted": "Admin Admin User"
  },
  "displayName": "Default Admin User",
  "nickName": "Admin",
  "profileUrl": null,
  "title": "Admin",
  "userType": null,
  "preferredLanguage": null,
  "locale": null,
  "timezone": null,
  "active": true,
  "password": null,
  "emails": [
    {
      "value": "admin@testjans.gluu.com",
      "display": null,
      "type": null,
      "primary": false
    }
  ],
  "phoneNumbers": null,
  "ims": null,
  "photos": null,
  "addresses": null,
  "groups": [
    {
      "value": "60B7",
      "$ref": "https://testjans.gluu.com/jans-scim/restv1/v2/Groups/60B7",
      "display": "Jannsen Manager Group",
      "type": "direct"
    }
  ],
  "entitlements": null,
  "roles": null,
  "x509Certificates": null,
  "urn:ietf:params:scim:schemas:extension:gluu:2.0:User": null,
  "schemas": [
    "urn:ietf:params:scim:schemas:core:2.0:User"
  ],
  "id": "18ca6089-42fb-410a-a5b5-c2631d75dc7d",
  "meta": {
    "resourceType": "User",
    "created": null,
    "lastModified": "2021-04-05T17:56:40.502Z",
    "location": "https://testjans.gluu.com/jans-scim/restv1/v2/Users/18ca6089-42fb-410a-a5b5-c2631d75dc7d"
  }
}

Selection: 

```

There is another example to update user resource on a sub-path:

```
Selection: 6

«Entry to be patch
id: 18ca6089-42fb-410a-a5b5-c2631d75dc7d

«The kind of operation to perform. Type: string»
op: replace

«Required when op is remove, optional otherwise. Type: string»
path: name/familyName

«Only required when op is add or replace. Type: string»
value: MH Shakil

Patch another param? n
[
  {
    "op": "replace",
    "path": "name.familyName",
    "value": "MH Shakil"
  }
]

Continue? y
Getting access token for scope https://jans.io/scim/users.write
Please wait patching...

{
  "externalId": null,
  "userName": "admin",
  "name": {
    "familyName": "MH Shakil",
    "givenName": "Admin",
    "middleName": "Admin",
    "honorificPrefix": null,
    "honorificSuffix": null,
    "formatted": "Admin Admin User"
  },
  "displayName": "Default Admin User",
  "nickName": "Admin",
  "profileUrl": null,
  "title": "MH Shakil",
  "userType": null,
  "preferredLanguage": null,
  "locale": null,
  "timezone": null,
  "active": true,
  "password": null,
  "emails": [
    {
      "value": "admin@testjans.gluu.com",
      "display": null,
      "type": null,
      "primary": false
    }
  ],
  "phoneNumbers": null,
  "ims": null,
  "photos": null,
  "addresses": null,
  "groups": [
    {
      "value": "60B7",
      "$ref": "https://testjans.gluu.com/jans-scim/restv1/v2/Groups/60B7",
      "display": "Jannsen Manager Group",
      "type": "direct"
    }
  ],
  "entitlements": null,
  "roles": null,
  "x509Certificates": null,
  "urn:ietf:params:scim:schemas:extension:gluu:2.0:User": null,
  "schemas": [
    "urn:ietf:params:scim:schemas:core:2.0:User"
  ],
  "id": "18ca6089-42fb-410a-a5b5-c2631d75dc7d",
  "meta": {
    "resourceType": "User",
    "created": null,
    "lastModified": "2021-04-07T17:57:11.250Z",
    "location": "https://testjans.gluu.com/jans-scim/restv1/v2/Users/18ca6089-42fb-410a-a5b5-c2631d75dc7d"
  }
}

Selection: 

```

**_Please note_**: you can use any of them between dot (.) and slash (/) to add a sub-path in the operation.


### Group

Group resources are used to organize user resources. These are the following options:

```
group
-----
1 Query Group resources (see section 3.4.2 of RFC 7644)
2 Allows creating a Group resource via POST (see section 3.3 of RFC 7644)
3 Retrieves a Group resource by Id (see section 3.4.1 of RFC 7644)
4 Updates a Group resource (see section 3.5.1 of RFC 7644). Update works in a replacement fashion&amp;#58; every
attribute value found in the payload sent will replace the one in the existing resource representation. Attributes 
not passed in the payload will be left intact.

5 Deletes a group resource (see section 3.6 of RFC 7644)
6 Updates one or more attributes of a Group resource using a sequence of additions, removals, and 
replacements operations. See section 3.5.2 of RFC 7644

7 Query Group resources (see section 3.4.2 of RFC 7644)

```

  1. **Query Group Resources**: It shows all the group resources and its perspective user resources. To find list of resources with custom filter, it supports advanced search with few properties:

    1. attributes
    2. excludeattributes
    3. filter
    4. startindex
    5. count

This is an demo example where each of this properties skipped for default value:

```
Query Group resources (see section 3.4.2 of RFC 7644)
-----------------------------------------------------

«A comma-separated list of attribute names to return in the response. Type: string»
attributes: 

«When specified, the response will contain a default set of attributes minus those listed here (as a comma-separated list). Type: string»
excludedAttributes: 

«An expression specifying the search criteria. See section 3.4.2.2 of RFC 7644. Type: string»
filter: 

«The 1-based index of the first query result. Type: integer»
startIndex: 

«Specifies the desired maximum number of query results per page. Type: integer»
count: 

«The attribute whose value will be used to order the returned responses. Type: string»
sortBy: 

«Order in which the sortBy param is applied. Allowed values are "ascending" and "descending". Type: string»
sortOrder: 
Please wait while retreiving data ...

Getting access token for scope https://jans.io/scim/groups.read
{
  "Resources": [
    {
      "displayName": "Jannsen Manager Group",
      "members": [
        {
          "$ref": "https://testjans.gluu.com/jans-scim/restv1/v2/Users/18ca6089-42fb-410a-a5b5-c2631d75dc7d",
          "type": "User",
          "display": "Default Admin User",
          "value": "18ca6089-42fb-410a-a5b5-c2631d75dc7d"
        }
      ],
      "schemas": [
        "urn:ietf:params:scim:schemas:core:2.0:Group"
      ],
      "id": "60B7",
      "meta": {
        "resourceType": "Group",
        "created": null,
        "lastModified": null,
        "location": "https://testjans.gluu.com/jans-scim/restv1/v2/Groups/60B7"
      }
    }
  ],
  "schemas": [
    "urn:ietf:params:scim:api:messages:2.0:ListResponse"
  ],
  "totalResults": 1,
  "startIndex": 1,
  "itemsPerPage": 1
}

Selection: 

```
  4. **_Update a group Resource_**: 
  
  Updating a group resource works in a replacement fashion and every attribute value found in the payload will replace the one in the existing resource. Attributes those are not passed in the payload will be left as same as before.

  If you select option 4 it will be asked to enter the id of a group that you may want to update. After then You will get a list of Fields:

   ```
   Fields:
    1 displayName
    2 id
    3 members
    4 meta
    5 schemas
   ```
  You can select each of these fields to update one by one. Let's select 3rd field to add memebers in the group. It will ask to enter some follow-up questions, like `Add Member? [y, n]`. Then enter each value of the user attributes:
  
    - ref: User referral url
    - type: type as a User
    - display: User display Name
    - value: inum of the user

  As you see below, If you choose `y` for `Add another Member?` then similarly you can add resource for another user. But if you choose `n` then you can select few options: 

    - q: to quit from operations
    - v: to view changes
    - l: to display the current list of fields
    - s: to save changes
  
  Please see below result to better understand about how this option really works.

  ```
    «q: quit, v: view, s: save, l: list fields #: update filed. »
Selection: 3

«Represents a member of a Group resource. »
Add Member? y

   «URI of the SCIM resource. Type: string»
   ref: https://testjans.gluu.org/jans-scim/restv1/v2/Users/e0b8a6a5-1955-49d7-acba-55a75b2373df

   «The type of member. Only "User" is allowed. Type: string»
   type: User

   «A human readable name, primarily used for display purposes. Type: string»
   display: Default Admin User

   «Identifier (ID) of the resource. Type: string»
   value: e0b8a6a5-1955-49d7-acba-55a75b2373df

Add another Member? n

«q: quit, v: view, s: save, l: list fields #: update filed. »
Selection: s
Changes:
members: [{'display': 'Default Admin User',
 'ref': 'https://testjans.gluu.org/jans-scim/restv1/v2/Users/e0b8a6a5-1955-49d7-acba-55a75b2373df',
 'type': 'User',
 'value': 'e0b8a6a5-1955-49d7-acba-55a75b2373df'}]
  ```

`continue?` as `y` to perform the operation:

 ```
  Continue? y
  Please wait while posting data ...

  Getting access token for scope https://jans.io/scim/groups.write
  {
    "displayName": "Jannsen Test Group",
    "members": [
      {
        "$ref": "https://testjans.gluu.org/jans-scim/restv1/v2/Users/e0b8a6a5-1955-49d7-acba-55a75b2373df",
        "type": "User",
        "display": "Default Admin User",
        "value": "e0b8a6a5-1955-49d7-acba-55a75b2373df"
      }
    ],
    "schemas": [
      "urn:ietf:params:scim:schemas:core:2.0:Group"
    ],
    "id": "766ffd8c-88a8-4aa8-a430-a5b3ae809c21",
    "meta": {
      "resourceType": "Group",
      "created": "2021-04-14T19:54:03.091Z",
      "lastModified": "2021-04-15T14:21:10.715Z",
      "location": "https://testjans.gluu.org/jans-scim/restv1/v2/Groups/766ffd8c-88a8-4aa8-a430-a5b3ae809c21"
    }
  }
 ```
Finally it will make changes in the group resource.

