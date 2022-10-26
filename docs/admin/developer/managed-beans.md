---
tags:
  - administration
  - developer
---

## Ready-to-use code in Custom script:  
Jans-auth server uses Weld 3.0 (JSR-365 aka CDI 2.0) for managed beans. The most important aspects of business logic are implemented through a set of beans some of which are listed below:

### 1. [AuthenticationService](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/service/AuthenticationService.java) 

Allows to authenticate a user or obtain the current authenticated user
<br/>
Relevant methods:

|Signature|Description|
|-|-|
|`boolean authenticate(String userName)`|Performs authentication for the user whose identifier (`userName`) is passed as parameter|
|`boolean authenticate(String userName, String password)`|Performs authentication for the user whose identifier (`userName`) is passed as parameter. The `password` supplied must be the correct password of the user in question|
|`User getAuthenticatedUser()`|Returns a representation of the currently authenticated user. `null` if no user is currently authenticated. See [User](#class-user) data object|

### 2.  [Authenticator](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/auth/Authenticator.java) 
This class is mainly used in facelets templates for authentication flows to proceed in the sequence of steps.
Relevant methods:

|Signature|Description|
|-|-|
|boolean authenticate()|Makes the authentication flow proceed by calling the `authenticate` method of the custom script|
|String prepareAuthenticationForStep()|Makes the authentication flow proceed by calling the `prepareForStep` method of the custom script|

### 3.  [UserService](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/service/UserService.java)
Allows CRUD of users in the local persistence.

Relevant methods:

|Signature|Description|
|-|-|
|`User addUser(User user, boolean active)`|Creates a new user based on the representation passed as parameter. `active` parameter denotes whether user status (`gluuStatus` attribute) will be `active` or `register`|
|`User addUserAttribute(String userId, String attributeName, String attributeValue)`|Adds an attribute to the user identified by `userId` in the database with the name and value passed. Returns a representation of the modified user or `null` in case of failure or if such name/attribute is already part of such user|
|`boolean addUserAttribute(User user, String attributeName, String attributeValue)`|Adds an attribute to the `user` object with the name and value passed. This method only alters the `user` argument and does not persist changes. Returns `false` if such name/attribute is already part of `user`
|`User addUserAttributeByUserInum(String userInum, String attributeName, String attributeValue)`|Adds an attribute to the user whose `inum`  attribute (in the database) equals to `userInum` using the name and value passed. Returns a representation of the modified user or `null` in case of failure or if such name/attribute is already part of such user|
|`CustomAttribute getCustomAttribute(User user, String attributeName)`|Gets a representation of the attribute whose name is passed for the user in question (`user`). Returns `null` if no such attribute is populated|
|`String getDnForUser(String inum)`|Obtains the DN (distinguished name) of the user whose `inum` attribute equals to `userInum` (no check that such user may exist is actually made)|
|`User getUser(String userId, String... returnAttributes)`|Retrieves a user representation for the user identified with `userId` containing only the attributes requested (`returnAttributes`). `null` is returned if no such user exists|
|`User getUserByAttribute(String attributeName, String attributeValue)`|Retrieves a user (first available) such that the attribute referenced (`attributeName`) has the value passed (`attributeValue`). `null` is returned if no such user exists|
|`String getUserInum(String userId)`|Retrieves the `inum` database attribute for the user identified with `userId`.`null` is returned if no such user exists|
|`User removeUserAttribute(String userId, String attributeName, String attributeValue)`|Removes `attributeValue` from the values of the attribute whose name is passed (`attributeName`) for the user identified with `userId`|
|`User replaceUserAttribute(String userId, String attributeName, String oldAttributeValue, String newAttributeValue)`|Updates the user identified with `userId` by replacing the value of the attribute `attributeName` with the value passed. `null` is returned if no such user exists|
|`void setCustomAttribute(User user, String attributeName, String attributeValue)`|Sets the value of the attribute `attributeName` with the single value `attributeValue` for the user representation passes as parameter. This method does not persist changes|
|`User updateUser(User user)`|Updates the user represented by `user` object in the database|

### 4.  [User](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/common/src/main/java/io/jans/as/common/model/common/User.java)
A class employed to represent a user entry in the persistence. Provides getters and setters to retrieve and assign value(s) for attributes

### 5. [CustomAttribute](https://github.com/JanssenProject/jans/blob/main/jans-orm/model/src/main/java/io/jans/orm/model/base/CustomAttribute.java) 
A class that models an attribute. An attribute has a name and a collection of associated values

### 6. [Identity](https://github.com/JanssenProject/jans/blob/main/jans-core/service/src/main/java/io/jans/model/security/Identity.java) 
Mainly used to carry data between steps of authentication flows.

|Signature|Description|
|-|-|
|`Object getWorkingParameter(String name)`|Retrieves a working parameter by name previously set via `setWorkingParameter`|
|`void setWorkingParameter(String name, Object value)`|Binds data to a name for further use in an authentication flow. Recommended values to store are `String`s|
|`SessionId getSessionId()`|Retrieves a reference to the associated server session object, see [SessionId](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/model/common/SessionId.java)|

### 7. HttpService: [HttpService](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/service/net/HttpService.java) 

Provides utility methods to execute HTTP requests, manipulate responses, etc

Relevant methods:

|Signature|Description|
|-|-|
|`HttpClient getHttpsClient()`|Returns an instance of `org.apache.http.client.HttpClient` (see oxcore-util class [SslDefaultHttpClient](https://github.com/JanssenProject/jans/blob/main/jans-core/util/src/main/java/io/jans/net/SslDefaultHttpClient.java))|
|`HttpServiceResponse executeGet(HttpClient httpClient, String requestUri)`|Perform a GET on the URI requested. Returns an instance of [io.jans.as.server.model.net.HttpServiceResponse](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/model/net/HttpServiceResponse.java)  (a wrapper on `org.apache.http.HttpResponse`)|
|`byte[] getResponseContent(HttpResponse httpResponse)`|Consumes the bytes of the associated response. Returns `null` if the response status code is not 200 (OK)|

### 8.  [CacheService](https://github.com/JanssenProject/jans/blob/main/jans-core/cache/src/main/java/io/jans/service/CacheService.java) 
Provides a unified means to interact with the underlying cache provider configured in the Jans-auth Server

Relevant methods:

|Signature|Description|
|-|-|
|`void clear()`|Flushes the whole cache|
|`Object get(String key)`|Retrieves the value of `key` in the cache. `null` if there is no such key present|
|`void put(int expirationInSeconds, String key, Object object)`|Puts an object in the cache associated to the key passed. An expiration in seconds can be provided|
|`put(String key, Object object)`|Puts an object in the cache associated to the key passed. The expiration used is the default expiration configured in Gluu|
|`void remove(String key)`|Removes an entry from the cache|

### 9. [FacesService](https://github.com/JanssenProject/jans/blob/main/jans-core/jsf-util/src/main/java/io/jans/jsf2/service/FacesService.java) : Provides utilities to properly build encoded URLs and make redirections. This class is used a great deal in custom scripts

Relevant methods:

|Signature|Description|
|-|-|
|`void redirectToExternalURL(String url)`|Redirects the user's browser to the URL passed as parameter|
|`String encodeParameters(String url, Map<String, Object> parameters)`|Builds a URL by appending query parameters as supplied in `parameters` map. Every value in the map is properly URL-encoded|

### 10.  [FacesMessages](https://github.com/JanssenProject/jans/blob/main/jans-core/jsf-util/src/main/java/io/jans/jsf2/message/FacesMessages.java) 
Allows manipulation of JSF context messages

Relevant methods:

|Signature|Description|
|-|-|
|`void add(Severity severity, String message)`|Adds a message to the JSF context with the severity (`javax.faces.application.FacesMessage.Severity`) specified|
|`void clear()`|Clears the messages of the JSF context|
|`String evalAsString(String expression)`|Evaluates an EL expression using the JSF context and returns the result as a String|
|`void setKeepMessages()`|Sets the "keep messages" property of the JSF flash|


### 11. [CdiUtil](https://github.com/JanssenProject/jans/blob/main/jans-core/service/src/main/java/io/jans/service/cdi/util/CdiUtil.java) : Allows to obtain references of managed beans. This is particularly useful in custom scripts

Relevant methods:

|Signature|Description|
|-|-|
|<T> T bean(Class<T> clazz)|Gets the managed bean belonging to the class passed as parameter|

Example (jython code):

```
from org.gluu.oxauth.service import UserService
from org.gluu.oxauth.service import AuthenticationService
...
userService = CdiUtil.bean(UserService)
authenticationService = CdiUtil.bean(AuthenticationService)
```

### 12. [StringHelper](https://github.com/JanssenProject/jans/blob/main/jans-core/util/src/main/java/io/jans/util/StringHelper.java) 
 Provides many utility methods that often arise in the manipulation of Strings

### 13. [EncryptionService](https://github.com/JanssenProject/jans/blob/main/jans-scim/service/src/main/java/io/jans/scim/service/EncryptionService.java) 
 Allows to encrypt/decrypt strings using a 3DES cipher whose salt is found in `/etc/jans/conf/salt`

Relevant methods:

|Signature|Description|
|-|-|
|String decrypt(String encryptedString)|Decrypts the encrypted string supplied|
|Properties decryptAllProperties(Properties connectionProperties)|Returns a `java.util.Properties` object with all decrypted values found in `connectionProperties`|
|`String encrypt(String unencryptedString)`|Encrypts the string supplied|