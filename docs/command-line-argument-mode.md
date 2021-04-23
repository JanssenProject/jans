We will refer to this mode as **CL**. Using this mode is difficult compared to IM. The first is to get help, so type:
```
/opt/jans/jans-cli/config-cli.py --help
```

This will print how to use CL:

```commandline
usage: config-cli.py [-h] [--host HOST] [--client-id CLIENT_ID]
                     [--client_secret CLIENT_SECRET] [-debug]
                     [--debug-log-file DEBUG_LOG_FILE]
                     [--operation-id OPERATION_ID] [--url-suffix URL_SUFFIX]
                     [--info {ApiCli,AppConfigurat,Attribute,AuthenticationFilt,AuthenticationMet,AuthenticationProtectionConfigurat,CIBAEndUserNotificationCon,CacheConfigurat,CacheConfiguration,CacheConfigurationInMemory,CacheConfigurationMemcached,CacheConfigurationNativePersistence,CacheConfigurationRedis,Cli,ClientAttribu,Configurat,ConfigurationFido2,ConfigurationJWKJSONWebKeyJWK,ConfigurationLogging,ConfigurationProperties,ConfigurationSMTP,CorsConfigurationFil,CouchbaseConfigurat,CustomAttrib,CustomScr,CustomScripts,DatabaseCouchbaseConfiguration,DatabaseLDAPConfiguration,DefaultAuthenticationMethod,ErrorRespo,Fido2Configurat,GluuAttrib,GluuAttributeAttributeValidat,InMemoryConfigurat,InlineResponse,JansFido2DynConfigurat,JsonWeb,LdapConfigurat,LoggingConfigurat,MemcachedConfigurat,NativePersistenceConfigurat,OAuthOpenIDConnectClients,OAuthOpenIDConnectSectorIdentifiers,OAuthScopes,OAuthUMAResources,PatchRequ,RedisConfigurat,RequestedPart,Sc,ScopeAttribu,ScriptEr,SectorIdentif,SimpleCustomPrope,SimpleExtendedCustomPrope,SmtpConfigurat,UmaResou,WebKeysConfigurat}]
                     [--op-mode {get,post,put,patch,delete}]
                     [--endpoint-args ENDPOINT_ARGS] [--schema SCHEMA]
                     [--data DATA]

optional arguments:
  -h, --help            show this help message and exit
  --host HOST           Hostname of server
  --client-id CLIENT_ID
                        Jans Config Api Client ID
  --client_secret CLIENT_SECRET
                        Jans Config Api Client ID secret
  -debug                Run in debug mode
  --debug-log-file DEBUG_LOG_FILE
                        Log file name when run in debug mode
  --operation-id OPERATION_ID
                        Operation ID to be done
  --url-suffix URL_SUFFIX
                        Argument to be added api endpoint url. For example
                        inum:2B29
  --info {ApiCli,AppConfigurat,Attribute,AuthenticationFilt,AuthenticationMet,AuthenticationProtectionConfigurat,CIBAEndUserNotificationCon,CacheConfigurat,CacheConfiguration,CacheConfigurationInMemory,CacheConfigurationMemcached,CacheConfigurationNativePersistence,CacheConfigurationRedis,Cli,ClientAttribu,Configurat,ConfigurationFido2,ConfigurationJWKJSONWebKeyJWK,ConfigurationLogging,ConfigurationProperties,ConfigurationSMTP,CorsConfigurationFil,CouchbaseConfigurat,CustomAttrib,CustomScr,CustomScripts,DatabaseCouchbaseConfiguration,DatabaseLDAPConfiguration,DefaultAuthenticationMethod,ErrorRespo,Fido2Configurat,GluuAttrib,GluuAttributeAttributeValidat,InMemoryConfigurat,InlineResponse,JansFido2DynConfigurat,JsonWeb,LdapConfigurat,LoggingConfigurat,MemcachedConfigurat,NativePersistenceConfigurat,OAuthOpenIDConnectClients,OAuthOpenIDConnectSectorIdentifiers,OAuthScopes,OAuthUMAResources,PatchRequ,RedisConfigurat,RequestedPart,Sc,ScopeAttribu,ScriptEr,SectorIdentif,SimpleCustomPrope,SimpleExtendedCustomPrope,SmtpConfigurat,UmaResou,WebKeysConfigurat}
                        Help for operation
  --op-mode {get,post,put,patch,delete}
                        Operation mode to be done
  --endpoint-args ENDPOINT_ARGS
                        Arguments to pass endpoint separated by comma. For
                        example limit:5,status:INACTIVE
  --schema SCHEMA       Get sample json schema
  --data DATA           Path to json data file
```
We start with getting information about tasks, tasks are options of argument `--info`:

```
Attribute, CacheConfiguration, CacheConfigurationInMemory, CacheConfigurationMemcached, CacheConfigurationNativePersistence, CacheConfigurationRedis, ConfigurationFido2, ConfigurationJWKJSONWebKeyJWK, ConfigurationLogging, ConfigurationProperties, ConfigurationSMTP, CustomScripts, DatabaseCouchbaseConfiguration, DatabaseLDAPConfiguration, DefaultAuthenticationMethod, OAuthOpenIDConnectClients, OAuthOpenIDConnectSectorIdentifiers, OAuthScopes, OAuthUMAResources
```

To get information for a specific task we run command as below: 
```commandline
/opt/jans/jans-cli/config-cli.py --info [taks]
``` 
for example: 
```commandline
/opt/jans/jans-cli/config-cli.py --info DefaultAuthenticationMethod
``` 

It returns with some `operation id`:
```commandline
Operation ID: get-acrs
  Description: Gets default authentication method.
Operation ID: put-acrs
  Description: Updates default authentication method.
  Schema: /components/schemas/AuthenticationMethod

To get sample shema type /opt/jans/jans-cli/config-cli.py --schema <schma>, for example /opt/jans/jans-cli/config-cli.py --schema /components/schemas/AuthenticationMethod
```
To perform any operation, you have to run command line with the operation id. for example:

```commandline
/opt/jans/jans-cli/config-cli.py --operation-id get-acrs
```
It returns:

```text
Getting access token for scope https://jans.io/oauth/config/acrs.readonly
{
  "defaultAcr": "simple_password_auth"
}
```

This is how we can execute single line command to get information about the Janssen Server. As we discussed on a specific task in the Interactive Mode, similarly we will discuss here using single line command to perform such operation.

### Tips (CL)

  1. `-h` or `--help` to get all the informtations of config-cli (ex; `/opt/jans/jans-cli/config-cli.py -h`)
  2. `--info` to get some operations id for a specific taks (ex; `opt/jans/jans-cli/config-cli.py --info User`)
  3. `--operation-id` usage to operate each of the sub-task
  4. `--endpoint-args` advanced usage for operation-id
  5. `--data` usage to share data in operations


### Attribute

First thing, let's get the information for `Attribute`:
```commandline
/opt/jans/jans-cli/config-cli.py --info Attribute
```
In return, we get:

```text
Operation ID: get-attributes
  Description: Gets all attributes. Optionally max-size of the result, attribute status and pattern can be provided.
  Parameters:
  limit: Search size - max size of the results to return. [integer]
  pattern: Search pattern. [string]
  status: Status of the attribute [string]
Operation ID: post-attributes
  Description: Adds a new attribute.
  Schema: /components/schemas/GluuAttribute
Operation ID: put-attributes
  Description: Updates an existing attribute.
  Schema: /components/schemas/GluuAttribute
Operation ID: get-attributes-by-inum
  Description: Gets an attribute based on inum.
  url-suffix: inum
Operation ID: delete-attributes-by-inum
  Description: Deletes an attribute based on inum.
  url-suffix: inum
Operation ID: patch-attributes-by-inum
  Description: Partially modify a GluuAttribute.
  url-suffix: inum
  Schema: Array of /components/schemas/PatchRequest

To get sample shema type /opt/jans/jans-cli/config-cli.py --schema <schma>, for example /opt/jans/jans-cli/config-cli.py --schema /components/schemas/PatchRequest
```

- **get-attributes**

```text
Operation ID: get-attributes
  Description: Gets all attributes. Optionally max-size of the result, attribute status and pattern can be provided.
  Parameters:
  limit: Search size - max size of the results to return. [integer]
  pattern: Search pattern. [string]
  status: Status of the attribute [string]
```
Let's do some queries using `get-attributes` operation ID.

To get all the attributes without any arguments, run the following command:
```commandline
/opt/jans/jans-cli/config-cli.py --operation-id get-attributes
```

To get attributes with passing the arguments, let's retrieve randomly limit:5:

```commandline
/opt/jans/jans-cli/config-cli.py --operation-id get-attributes --endpoint-args limit:5
```

It will return any 5 attributes randomly:
```text
Getting access token for scope https://jans.io/oauth/config/attributes.readonly
Calling with params limit=5
[
  {
    "dn": "inum=B4B0,ou=attributes,o=jans",
    "inum": "B4B0",
    "selected": false,
    "name": "givenName",
    "displayName": "First Name",
    "description": "Given name(s) or first name(s) of the End-User.Note that in some cultures, people can have multiple given names;all can be present, with the names being separated by space characters.",
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
    "claimName": "given_name",
    "seeAlso": null,
    "saml1Uri": "urn:mace:dir:attribute-def:givenName",
    "saml2Uri": "urn:oid:2.5.4.42",
    "urn": "urn:mace:dir:attribute-def:givenName",
    "scimCustomAttr": null,
    "oxMultiValuedAttribute": false,
    "custom": false,
    "requred": false,
    "attributeValidation": null,
    "tooltip": null
  },
  {
    "dn": "inum=29DA,ou=attributes,o=jans",
    "inum": "29DA",
    "selected": false,
    "name": "inum",
    "displayName": "Inum",
    "description": "XRI i-number, persistent non-reassignable identifier",
    "dataType": "STRING",
    "status": "ACTIVE",
    "lifetime": null,
    "sourceAttribute": null,
    "salt": null,
    "nameIdType": null,
    "origin": "jansPerson",
    "editType": null,
    "viewType": [
      "USER",
      "ADMIN"
    ],
    "usageType": null,
    "claimName": "inum",
    "seeAlso": null,
    "saml1Uri": "urn:mace:dir:attribute-def:inum",
    "saml2Uri": "urn:oid:1.3.6.1.4.1.48710.1.3.117",
    "urn": "urn:jans:dir:attribute-def:inum",
    "scimCustomAttr": null,
    "oxMultiValuedAttribute": false,
    "custom": false,
    "requred": false,
    "attributeValidation": null,
    "tooltip": null
  },
  {
    "dn": "inum=B52A,ou=attributes,o=jans",
    "inum": "B52A",
    "selected": false,
    "name": "jansPrefUsrName",
    "displayName": "Preferred Username",
    "description": "A domain issued and managed identifier for the person.Subject - Identifier for the End-User at the Issuer.",
    "dataType": "STRING",
    "status": "ACTIVE",
    "lifetime": null,
    "sourceAttribute": null,
    "salt": null,
    "nameIdType": null,
    "origin": "jansPerson",
    "editType": [
      "ADMIN"
    ],
    "viewType": [
      "USER",
      "ADMIN"
    ],
    "usageType": null,
    "claimName": "preferred_username",
    "seeAlso": null,
    "saml1Uri": "urn:mace:dir:attribute-def:preferredUsername",
    "saml2Uri": "urn:oid:1.3.6.1.4.1.48710.1.3.320",
    "urn": "http://openid.net/specs/openid-connect-core-1_0.html/StandardClaims/preferred_username",
    "scimCustomAttr": null,
    "oxMultiValuedAttribute": false,
    "custom": false,
    "requred": false,
    "attributeValidation": null,
    "tooltip": null
  },
  {
    "dn": "inum=64A0,ou=attributes,o=jans",
    "inum": "64A0",
    "selected": false,
    "name": "profile",
    "displayName": "Profile URL",
    "description": "URL of the End-User's profile page. The contents of this Web page SHOULD be about the End-User.",
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
    "claimName": "profile",
    "seeAlso": null,
    "saml1Uri": "urn:mace:dir:attribute-def:profile",
    "saml2Uri": "urn:oid:1.3.6.1.4.1.48710.1.3.321",
    "urn": "http://openid.net/specs/openid-connect-core-1_0.html/StandardClaims/profile",
    "scimCustomAttr": null,
    "oxMultiValuedAttribute": false,
    "custom": false,
    "requred": false,
    "attributeValidation": null,
    "tooltip": null
  },
  {
    "dn": "inum=42E0,ou=attributes,o=jans",
    "inum": "42E0",
    "selected": false,
    "name": "uid",
    "displayName": "Username",
    "description": "A domain issued and managed identifier for the person.Subject - Identifier for the End-User at the Issuer.",
    "dataType": "STRING",
    "status": "ACTIVE",
    "lifetime": null,
    "sourceAttribute": null,
    "salt": null,
    "nameIdType": null,
    "origin": "jansPerson",
    "editType": [
      "ADMIN"
    ],
    "viewType": [
      "USER",
      "ADMIN"
    ],
    "usageType": null,
    "claimName": "user_name",
    "seeAlso": null,
    "saml1Uri": "urn:mace:dir:attribute-def:uid",
    "saml2Uri": "urn:oid:0.9.2342.19200300.100.1.1",
    "urn": "urn:mace:dir:attribute-def:uid",
    "scimCustomAttr": null,
    "oxMultiValuedAttribute": false,
    "custom": false,
    "requred": false,
    "attributeValidation": null,
    "tooltip": null
  }
]
```

To get attributes with `pattern & status`:
```commandline
/opt/jans/jans-cli/config-cli.py --operation-id get-attributes --endpoint-args limit:5,pattern:profile,status:ACTIVE
```
In return, we get:

```properties
Getting access token for scope https://jans.io/oauth/config/attributes.readonly
Calling with params limit=5&pattern=profile&status=ACTIVE
[
  {
    "dn": "inum=EC3A,ou=attributes,o=jans",
    "inum": "EC3A",
    "selected": false,
    "name": "picture",
    "displayName": "Picture URL",
    "description": "URL of the End-User's profile picture",
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
    "claimName": "picture",
    "seeAlso": null,
    "saml1Uri": "urn:mace:dir:attribute-def:picture",
    "saml2Uri": "urn:oid:1.3.6.1.4.1.48710.1.3.322",
    "urn": "http://openid.net/specs/openid-connect-core-1_0.html/StandardClaims/picture",
    "scimCustomAttr": null,
    "oxMultiValuedAttribute": false,
    "custom": false,
    "requred": false,
    "attributeValidation": null,
    "tooltip": null
  },
  {
    "dn": "inum=64A0,ou=attributes,o=jans",
    "inum": "64A0",
    "selected": false,
    "name": "profile",
    "displayName": "Profile URL",
    "description": "URL of the End-User's profile page. The contents of this Web page SHOULD be about the End-User.",
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
    "claimName": "profile",
    "seeAlso": null,
    "saml1Uri": "urn:mace:dir:attribute-def:profile",
    "saml2Uri": "urn:oid:1.3.6.1.4.1.48710.1.3.321",
    "urn": "http://openid.net/specs/openid-connect-core-1_0.html/StandardClaims/profile",
    "scimCustomAttr": null,
    "oxMultiValuedAttribute": false,
    "custom": false,
    "requred": false,
    "attributeValidation": null,
    "tooltip": null
  }
]
```

- **post-attributes**

```text
Operation ID: post-attributes
  Description: Adds a new attribute.
  Schema: /components/schemas/GluuAttribute
```
Before adding a new attribute, let's get sample `schema`:
```commandline
/opt/jans/jans-cli/config-cli.py --schema /components/schemas/GluuAttribute > /tmp/attribute.json
```  
It will return as below:

```text
{
  "dn": null,
  "inum": null,
  "selected": true,
  "name": "name, displayName, birthdate, email",
  "displayName": "string",
  "description": "string",
  "dataType": "STRING",
  "status": "REGISTER",
  "lifetime": null,
  "sourceAttribute": null,
  "salt": null,
  "nameIdType": null,
  "origin": null,
  "editType": [],
  "viewType": "array",
  "usageType": [],
  "claimName": null,
  "seeAlso": null,
  "saml1Uri": null,
  "saml2Uri": null,
  "urn": null,
  "scimCustomAttr": true,
  "oxMultiValuedAttribute": true,
  "custom": true,
  "requred": true,
  "attributeValidation": {
    "regexp": null,
    "minLength": null,
    "maxLength": null
  },
  "tooltip": null
}
```
Modify it to update attribute `name`, `display name`, `view type`:
```text
nano /tmp/attribute.json
```

![post-attribute.png](img/cl-post-attribute.png)

Now, let's add this attribute using `post-attributes`:
```commandline
/opt/jans/jans-cli/config-cli.py --operation-id post-attributes --data /tmp/attribute.json
```
It will create a new attribute into the Attribute list with updated `inum & dn`:

```text
Getting access token for scope https://jans.io/oauth/config/attributes.write
Server Response:
{
  "dn": "inum=256135af-56eb-43f3-9583-d7e9fc75c672,ou=attributes,o=jans",
  "inum": "256135af-56eb-43f3-9583-d7e9fc75c672",
  "selected": false,
  "name": "testAttribute",
  "displayName": "test Attribute",
  "description": "testing post-attributes",
  "dataType": "CERTIFICATE",
  "status": "REGISTER",
  "lifetime": null,
  "sourceAttribute": null,
  "salt": null,
  "nameIdType": null,
  "origin": null,
  "editType": null,
  "viewType": [
    "ADMIN",
    "OWNER",
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


### OAuthScopes
Let's get information for a task **OAuthScopes**

```
 /opt/jans/jans-cli/config-cli.py --info OAuthScopes

Operation ID: get-oauth-scopes
  Description: Gets list of Scopes. Optionally type to filter the scope, max-size of the result and pattern can be provided.
  Parameters:
  type: Scope type. [string]
  limit: Search size - max size of the results to return. [integer]
  pattern: Search pattern. [string]
Operation ID: post-oauth-scopes
  Description: Create Scope.
  Schema: /components/schemas/Scope
Operation ID: put-oauth-scopes
  Description: Updates existing Scope.
  Schema: /components/schemas/Scope
Operation ID: get-oauth-scopes-by-inum
  Description: Get Scope by Inum
  url-suffix: inum
Operation ID: delete-oauth-scopes-by-id
  Description: Delete Scope.
  url-suffix: inum
Operation ID: patch-oauth-scopes-by-id
  Description: Update modified attributes of existing Scope by Inum.
  url-suffix: inum
  Schema: Array of /components/schemas/PatchRequest

To get sample schema type /opt/jans/jans-cli/config-cli.py --schema <schema>, for example /opt/jans/jans-cli/config-cli.py --schema /components/schemas/PatchRequest
```
Operations will be done with **Operation ID**. Some operations may take parameters, let's retrieve `3` scopes (**limit**) that has `view` in it's description (**pattern**) and **type** `openid`:

```
 /opt/jans/jans-cli/config-cli.py --operation-id get-oauth-scopes --endpoint-args limit:3,pattern:view,type:openid
 ```
 
It will return:
 
 ```
 Getting access token for scope https://jans.io/oauth/config/scopes.readonly
Calling with params limit=3&pattern=view&type=openid
[
  {
    "dn": "inum=43F1,ou=scopes,o=jans",
    "id": "profile",
    "inum": "43F1",
    "displayName": null,
    "description": "View your basic profile info.",
    "iconUrl": null,
    "authorizationPolicies": null,
    "defaultScope": false,
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
    "umaType": false,
    "umaAuthorizationPolicies": null,
    "attributes": {
      "spontaneousClientId": null,
      "spontaneousClientScopes": null,
      "showInConfigurationEndpoint": true
    }
  },
  {
    "dn": "inum=C17A,ou=scopes,o=jans",
    "id": "address",
    "inum": "C17A",
    "displayName": null,
    "description": "View your address.",
    "iconUrl": null,
    "authorizationPolicies": null,
    "defaultScope": false,
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
    "umaType": false,
    "umaAuthorizationPolicies": null,
    "attributes": {
      "spontaneousClientId": null,
      "spontaneousClientScopes": null,
      "showInConfigurationEndpoint": true
    }
  },
  {
    "dn": "inum=764C,ou=scopes,o=jans",
    "id": "email",
    "inum": "764C",
    "displayName": null,
    "description": "View your email address.",
    "iconUrl": null,
    "authorizationPolicies": null,
    "defaultScope": false,
    "scopeType": "openid",
    "claims": [
      "inum=8F88,ou=attributes,o=jans",
      "inum=CAE3,ou=attributes,o=jans"
    ],
    "umaType": false,
    "umaAuthorizationPolicies": null,
    "attributes": {
      "spontaneousClientId": null,
      "spontaneousClientScopes": null,
      "showInConfigurationEndpoint": true
    }
  }
]
```

Let's create a scope. Remember when we queried info for a task **OAuthScopes** it printed:

```
Operation ID: post-oauth-scopes
  Description: Create Scope.
  Schema: /components/schemas/Scope
```
Thus, we can get sample schema and use Operation ID `post-oauth-scopes`. Lets get sample schema:

```
/opt/jans/jans-cli/config-cli.py --schema /components/schemas/Scope > /tmp/scope.json
```

Now edit file `tmp/scope.json` with an editor (like **nano**). As an example we just filled the following properties:

```
"id": "TestScopeID",
"displayName": "TestScope",
"description": "Test Scope created by jans-cli",
```

![jans-cl Edit scope.json](img/cl-oauthscope-json.png) 

It is time to post the data:

```
 /opt/jans/jans-cli/config-cli.py --operation-id post-oauth-scopes --data /tmp/scope.json 

Getting access token for scope https://jans.io/oauth/config/scopes.write
Server Response:
{
  "dn": "inum=112116fd-257b-40d8-a2c9-0c23536680ed,ou=scopes,o=jans",
  "id": "TestScopeID",
  "inum": "112116fd-257b-40d8-a2c9-0c23536680ed",
  "displayName": "TestScope",
  "description": "Test Scope created by jans-cli",
  "iconUrl": null,
  "authorizationPolicies": null,
  "defaultScope": true,
  "scopeType": "openid",
  "claims": null,
  "umaType": false,
  "umaAuthorizationPolicies": null,
  "attributes": {
    "spontaneousClientId": null,
    "spontaneousClientScopes": null,
    "showInConfigurationEndpoint": true
  }
}

```

It created scope with inum `112116fd-257b-40d8-a2c9-0c23536680ed` and returned current data. Let's update `iconUrl` with patch method. So we need a schema for the patch method. Remember when we queried info for the task **OAuthScopes** it printed:

```
Operation ID: patch-oauth-scopes-by-id
  Description: Update modified attributes of existing Scope by Inum.
  url-suffix: inum
  Schema: Array of /components/schemas/PatchRequest
 ```
 
This means we need schema `/components/schemas/PatchRequest`, be careful it states **Array of**, so we will make an array of this schema, in case you need multiple changes with patch method, you can put as many as of this schema into array. Get schema:

```
/opt/jans/jans-cli/config-cli.py --schema /components/schemas/PatchRequest > /tmp/patch.json
```

When you examine this JSON, you will see three properties in an object: op, path, and value. Meanings of these properties are as follows:
* __op__ operation to be done, one of `add`, `remove`, `replace`, `move`, `copy`, `test`
* __path__ Path of property to be changed. use path separator `/` to change a property inside an object. For example to change **spontaneousClientId** you can use `attributes/spontaneousClientId`
* __value__ New value to be assigned for property defined in `path`

We can edit this json as follows (remember to make it an array):

![jans-cl Edit patch.json](img/cl-oauthscope-patch-json.png)

Let's do the operation:

```
/opt/jans/jans-cli/config-cli.py --operation-id patch-oauth-scopes-by-id --url-suffix inum:112116fd-257b-40d8-a2c9-0c23536680ed --data /tmp/patch.json 

Getting access token for scope https://jans.io/oauth/config/scopes.write
Server Response:
{
  "dn": "inum=112116fd-257b-40d8-a2c9-0c23536680ed,ou=scopes,o=jans",
  "id": "TestScopeID",
  "inum": "112116fd-257b-40d8-a2c9-0c23536680ed",
  "displayName": "TestScope",
  "description": "Test Scope created by jans-cli",
  "iconUrl": "https://www.jans.io/icon.png",
  "authorizationPolicies": null,
  "defaultScope": true,
  "scopeType": "openid",
  "claims": null,
  "umaType": false,
  "umaAuthorizationPolicies": null,
  "attributes": {
    "spontaneousClientId": null,
    "spontaneousClientScopes": null,
    "showInConfigurationEndpoint": true
  }
}
```

### Cache Configuration

Cache Configuration supports two types of operation through the Single Line command of Janssen CLI.
Let's get the information for Cache Configuration.

`/opt/jans/jans-cli/config-cli.py`

It prints below two operations:
```text

Operation ID: get-config-cache
  Description: Returns cache configuration.
Operation ID: patch-config-cache
  Description: Partially modifies cache configuration.
  Schema: Array of /components/schemas/PatchRequest

To get sample shema type /opt/jans/jans-cli/config-cli.py --schema <schma>, for example /opt/jans/jans-cli/config-cli.py --schema /components/schemas/PatchRequest

```

- **get-config-cache**

You can get the current Cache Configuration of your Janssen Server by performing this operation.

```commandline
/opt/jans/jans-cli/config-cli.py --operation-id get-config-cache
```

It will show the Cache configuration with details.

```text
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

- **patch-config-cache**

You may need to update Cache configuration, In that case `patch-config-cache` can be used to modify cache configuration.

```text
Operation ID: patch-config-cache
  Description: Partially modifies cache configuration.
  Schema: Array of /components/schemas/PatchRequest

To get sample shema type /opt/jans/jans-cli/config-cli.py --schema <schma>, for example /opt/jans/jans-cli/config-cli.py --schema /components/schemas/PatchRequest
```

Let's see the sample schema of cache configuration.

```text
/opt/jans/jans-cli/config-cli.py --schema /components/schemas/PatchRequest > /tmp/patch-cache.json

{
  "op": "add",
  "path": "string",
  "value": {}
}
```

When you examine this sample schema, you will see three properties in an object: op, path, and value.
* __op__ operation to be done, one of `add`, `remove`, `replace`, `move`, `copy`, `test`
* __path__ Path of property to be changed. use path separator `/` to change a property inside an object.
* __value__ New value to be assigned for each property defined in `path`

Let, We want to replace `memcachedConfiguration/bufferSize`:

We can edit this json as follows (remember to make it an array):

```commandline
nano /tmp/patch-cache.json

[
  {
  "op": "replace",
  "path": "memcachedConfiguration/bufferSize",
  "value": "32788"
  }
]
```

Now, let's do the operation: 
```text
/opt/jans/jans-cli/config-cli.py --operation-id patch-config-cache --data /tmp/patch-cache.json


Getting access token for scope https://jans.io/oauth/config/cache.write
Server Response:
{
  "cacheProviderType": "NATIVE_PERSISTENCE",
  "memcachedConfiguration": {
    "servers": "localhost:11211",
    "maxOperationQueueLength": 100000,
    "bufferSize": 32788,
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
root@testjans:/mnt/Academic/Gluu Support/jans-cli# /opt/jans/jans-cli/config-cli.py --operation-id patch-config-cache --data /tmp/patch-cache.json

Getting access token for scope https://jans.io/oauth/config/cache.writee.json
Server Response:
{
  "cacheProviderType": "NATIVE_PERSISTENCE",
  "memcachedConfiguration": {
    "servers": "localhost:11211",
    "maxOperationQueueLength": 100000,
    "bufferSize": 32788,
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

You see `bufferSize` has changed.
That's all for Cache Configuration.

### Default Authentication Method

Let's get the information of **Default Authentication Method**:

```
/opt/jans/jans-cli/config-cli.py --info DefaultAuthenticationMethod

Operation ID: get-acrs
  Description: Gets default authentication method.
Operation ID: put-acrs
  Description: Updates default authentication method.
  Schema: /components/schemas/AuthenticationMethod

To get sample shema type /opt/jans/jans-cli/config-cli.py --schema <schma>, for example /opt/jans/jans-cli/config-cli.py --schema /components/schemas/AuthenticationMethod
```

There are two types of operation in Default Authentication Method.
* __get-acrs__ : It returns current default authentication method of janssen server.
* __put-acrs__ : It's used to update default authentication method.

- **get-acrs**
  
To get the default authentication method:
```
/opt/jans/jans-cli/config-cli.py --operation-id get-acrs


Getting access token for scope https://jans.io/oauth/config/acrs.readonly
{
  "defaultAcr": "simple_password_auth"
}
```

- **__put-acrs__**

Let's update the _Default Authentication Method_ using janssen CLI command line. To perform the _put-acrs_ operation, we have to use its schema.
To get its schema:

```commandline
/opt/jans/jans-cli/config-cli.py --schema /components/schemas/AuthenticationMethod > /tmp/patch-default-auth.json


{
  "defaultAcr": null
}
```

It will create a `.json` file with a schema. It comes with a `null` value. We need to modify this file to update default acr.
we have seen that our Default Authentication Method is `simple_password_auth`. We are going to update it with `passport_saml` authenitcation method.

```commandline
nano /tmp/patch-default-auth.json
```
![update default authentication method](img/cl-update-default-auth.png)

Now let's do the operation:
```commandline
/opt/jans/jans-cli/config-cli.py --operation-id put-acrs --data /tmp/patch-default-auth.json
```

It will show the updated result.

![updated result](img/cl-update-default-auth-result.png)

### OAuthUMAResources

Let's get the information for OAuthUMAResources:

```
/opt/jans/jans-cli/config-cli.py --info OAuthUMAResources

Operation ID: get-oauth-uma-resources
  Description: Gets list of UMA resources.
  Parameters:
  limit: Search size - max size of the results to return. [integer]
  pattern: Search pattern. [string]
Operation ID: post-oauth-uma-resources
  Description: Creates an UMA resource.
  Schema: /components/schemas/UmaResource
Operation ID: put-oauth-uma-resources
  Description: Updates an UMA resource.
  Schema: /components/schemas/UmaResource
Operation ID: get-oauth-uma-resources-by-id
  Description: Gets an UMA resource by ID.
  url-suffix: id
Operation ID: delete-oauth-uma-resources-by-id
  Description: Deletes an UMA resource.
  url-suffix: id
Operation ID: patch-oauth-uma-resources-by-id
  Description: Partially updates an UMA resource by Inum.
  url-suffix: id
  Schema: Array of /components/schemas/PatchRequest

To get sample shema type /opt/jans/jans-cli/config-cli.py --schema <schma>, for example /opt/jans/jans-cli/config-cli.py --schema /components/schemas/PatchRequest
```

- **get-oauth-uma-resources**

This operation is used to search UMA Resources.

```text
Operation ID: get-oauth-uma-resources
  Description: Gets list of UMA resources.
  Parameters:
  limit: Search size - max size of the results to return. [integer]
  pattern: Search pattern. [string]
```

To get a list of UMA resources:
`/opt/jans/jans-cli/config-cli.py --operation-id get-oauth-uma-resources --endpoint-args limit:5`

It will return random 5 UMA resources.
```text
Getting access token for scope https://jans.io/oauth/config/uma/resources.readonly
Calling with params limit=5
[
  {
    "dn": "jansId=1800.1ed09ec8-5918-4cb5-9123-e4b9df36231f,ou=resources,ou=uma,o=jans",
    "inum": null,
    "id": "1800.1ed09ec8-5918-4cb5-9123-e4b9df36231f",
    "name": "Jans Cofig Api Uma Resource /jans-config-api/api/v1/attributes",
    "iconUri": "http://www.jans.io/img/scim_logo.png",
    "scopes": [
      "inum=CACA-0B30,ou=scopes,o=jans",
      "inum=CACA-BFDB,ou=scopes,o=jans"
    ],
    "scopeExpression": null,
    "clients": [
      "inum=1801.6e8351d4-5d3f-4773-b632-6c84bf8207cd,ou=clients,o=jans"
    ],
    "resources": [
      "https://testjans.gluu.com/jans-config-api/api/v1/attributes"
    ],
    "rev": "1",
    "creator": "inum=d206dd87-3a22-465d-bd25-bec9313cd42d,ou=people,o=json",
    "description": null,
    "type": null,
    "creationDate": null,
    "expirationDate": null,
    "deletable": true
  },
  {
    "dn": "jansId=1800.78e4c317-4d5a-4f23-b767-e63793364bee,ou=resources,ou=uma,o=jans",
    "inum": null,
    "id": "1800.78e4c317-4d5a-4f23-b767-e63793364bee",
    "name": "Jans Cofig Api Uma Resource /jans-config-api/api/v1/acrs",
    "iconUri": "http://www.jans.io/img/scim_logo.png",
    "scopes": [
      "inum=CACA-D906,ou=scopes,o=jans"
    ],
    "scopeExpression": null,
    "clients": [
      "inum=1801.6e8351d4-5d3f-4773-b632-6c84bf8207cd,ou=clients,o=jans"
    ],
    "resources": [
      "https://testjans.gluu.com/jans-config-api/api/v1/acrs"
    ],
    "rev": "1",
    "creator": "inum=d206dd87-3a22-465d-bd25-bec9313cd42d,ou=people,o=json",
    "description": null,
    "type": null,
    "creationDate": null,
    "expirationDate": null,
    "deletable": true
  },
  {
    "dn": "jansId=1800.556448d7-b349-45d8-a3a8-0d163df8753c,ou=resources,ou=uma,o=jans",
    "inum": null,
    "id": "1800.556448d7-b349-45d8-a3a8-0d163df8753c",
    "name": "Jans Cofig Api Uma Resource /jans-config-api/api/v1/config/cache",
    "iconUri": "http://www.jans.io/img/scim_logo.png",
    "scopes": [
      "inum=CACA-4525,ou=scopes,o=jans"
    ],
    "scopeExpression": null,
    "clients": [
      "inum=1801.6e8351d4-5d3f-4773-b632-6c84bf8207cd,ou=clients,o=jans"
    ],
    "resources": [
      "https://testjans.gluu.com/jans-config-api/api/v1/config/cache"
    ],
    "rev": "1",
    "creator": "inum=d206dd87-3a22-465d-bd25-bec9313cd42d,ou=people,o=json",
    "description": null,
    "type": null,
    "creationDate": null,
    "expirationDate": null,
    "deletable": true
  },
  {
    "dn": "jansId=1800.c4e0d1b6-e731-4c8d-a0ab-66784349a4da,ou=resources,ou=uma,o=jans",
    "inum": null,
    "id": "1800.c4e0d1b6-e731-4c8d-a0ab-66784349a4da",
    "name": "Jans Cofig Api Uma Resource /jans-config-api/api/v1/attributes",
    "iconUri": "http://www.jans.io/img/scim_logo.png",
    "scopes": [
      "inum=CACA-0B30,ou=scopes,o=jans"
    ],
    "scopeExpression": null,
    "clients": [
      "inum=1801.6e8351d4-5d3f-4773-b632-6c84bf8207cd,ou=clients,o=jans"
    ],
    "resources": [
      "https://testjans.gluu.com/jans-config-api/api/v1/attributes"
    ],
    "rev": "1",
    "creator": "inum=d206dd87-3a22-465d-bd25-bec9313cd42d,ou=people,o=json",
    "description": null,
    "type": null,
    "creationDate": null,
    "expirationDate": null,
    "deletable": true
  },
  {
    "dn": "jansId=1800.049d1198-a911-4032-aaf6-59cc94d3f4ef,ou=resources,ou=uma,o=jans",
    "inum": null,
    "id": "1800.049d1198-a911-4032-aaf6-59cc94d3f4ef",
    "name": "Jans Cofig Api Uma Resource /jans-config-api/api/v1/acrs",
    "iconUri": "http://www.jans.io/img/scim_logo.png",
    "scopes": [
      "inum=CACA-D906,ou=scopes,o=jans",
      "inum=CACA-698C,ou=scopes,o=jans"
    ],
    "scopeExpression": null,
    "clients": [
      "inum=1801.6e8351d4-5d3f-4773-b632-6c84bf8207cd,ou=clients,o=jans"
    ],
    "resources": [
      "https://testjans.gluu.com/jans-config-api/api/v1/acrs"
    ],
    "rev": "1",
    "creator": "inum=d206dd87-3a22-465d-bd25-bec9313cd42d,ou=people,o=json",
    "description": null,
    "type": null,
    "creationDate": null,
    "expirationDate": null,
    "deletable": true
  }
]
```

To search using multiple arguments, you can change pattern that you want to find:

```text
 /opt/jans/jans-cli/config-cli.py --operation-id get-oauth-uma-resources --endpoint-args limit:1,pattern:"Jans Cofig Api Uma Resource /jans-config-api/api/v1/config/cache/native-persistence"

Getting access token for scope https://jans.io/oauth/config/uma/resources.readonly
Calling with params limit=1&pattern=Jans+Cofig+Api+Uma+Resource+%2Fjans-config-api%2Fapi%2Fv1%2Fconfig%2Fcache%2Fnative-persistence
[
  {
    "dn": "jansId=1800.02d24ac8-13d6-464d-af1d-46a6261eaa65,ou=resources,ou=uma,o=jans",
    "inum": null,
    "id": "1800.02d24ac8-13d6-464d-af1d-46a6261eaa65",
    "name": "Jans Cofig Api Uma Resource /jans-config-api/api/v1/config/cache/native-persistence",
    "iconUri": "http://www.jans.io/img/scim_logo.png",
    "scopes": null,
    "scopeExpression": null,
    "clients": [
      "inum=1801.6e8351d4-5d3f-4773-b632-6c84bf8207cd,ou=clients,o=jans"
    ],
    "resources": [
      "https://testjans.gluu.com/jans-config-api/api/v1/config/cache/native-persistence"
    ],
    "rev": "1",
    "creator": "inum=d206dd87-3a22-465d-bd25-bec9313cd42d,ou=people,o=json",
    "description": null,
    "type": null,
    "creationDate": null,
    "expirationDate": null,
    "deletable": true
  },
  {
    "dn": "jansId=1800.6cd9bb98-05ac-43d2-bebb-3b1c92b9b409,ou=resources,ou=uma,o=jans",
    "inum": null,
    "id": "1800.6cd9bb98-05ac-43d2-bebb-3b1c92b9b409",
    "name": "Jans Cofig Api Uma Resource /jans-config-api/api/v1/config/cache/native-persistence",
    "iconUri": "http://www.jans.io/img/scim_logo.png",
    "scopes": null,
    "scopeExpression": null,
    "clients": [
      "inum=1801.6e8351d4-5d3f-4773-b632-6c84bf8207cd,ou=clients,o=jans"
    ],
    "resources": [
      "https://testjans.gluu.com/jans-config-api/api/v1/config/cache/native-persistence"
    ],
    "rev": "1",
    "creator": "inum=d206dd87-3a22-465d-bd25-bec9313cd42d,ou=people,o=json",
    "description": null,
    "type": null,
    "creationDate": null,
    "expirationDate": null,
    "deletable": true
  }
]
```

- **get-oauth-uma-resources-by-id**

```text
Operation ID: get-oauth-uma-resources-by-id
  Description: Gets an UMA resource by ID.
  url-suffix: id
```

To get uma resource by its ID, run the following command:
```text
/opt/jans/jans-cli/config-cli.py --operation-id get-oauth-uma-resources-by-id --url-suffix id:1800.c4e0d1b6-e731-4c8d-a0ab-66784349a4da


Getting access token for scope https://jans.io/oauth/config/uma/resources.readonly
{
  "dn": "jansId=1800.c4e0d1b6-e731-4c8d-a0ab-66784349a4da,ou=resources,ou=uma,o=jans",
  "inum": null,
  "id": "1800.c4e0d1b6-e731-4c8d-a0ab-66784349a4da",
  "name": "Jans Cofig Api Uma Resource /jans-config-api/api/v1/attributes",
  "iconUri": "http://www.jans.io/img/scim_logo.png",
  "scopes": [
    "inum=CACA-0B30,ou=scopes,o=jans"
  ],
  "scopeExpression": null,
  "clients": [
    "inum=1801.6e8351d4-5d3f-4773-b632-6c84bf8207cd,ou=clients,o=jans"
  ],
  "resources": [
    "https://testjans.gluu.com/jans-config-api/api/v1/attributes"
  ],
  "rev": "1",
  "creator": "inum=d206dd87-3a22-465d-bd25-bec9313cd42d,ou=people,o=json",
  "description": null,
  "type": null,
  "creationDate": null,
  "expirationDate": null,
  "deletable": true
}
```
replace the id with accurate one.

- **patch-oauth-uma-resources-by-id**

```text
Operation ID: patch-oauth-uma-resources-by-id
  Description: Partially updates an UMA resource by Inum.
  url-suffix: id
  Schema: Array of /components/schemas/PatchRequest
```

As you see the description, you can update an existing uma resource partially with this following operation.

Let's get the sample schema:
```text
/opt/jans/jans-cli/config-cli.py --schema /components/schemas/PatchRequest > /tmp/patch-uma.json

{
  "op": "move",
  "path": "string",
  "value": {}
}
```
Let's want to update as `deletable:false` to an uma resource whose `id=1800.c4e0d1b6-e731-4c8d-a0ab-66784349a4da`.
So we are going to operate `replace` where `path` is `deletable` with `value: false`.
let's update the json as below:

```json
nano /tmp/patch-uma.json

[
  {
    "op": "replace",
    "path": "deletable",
    "value": false
  }
]
```

now let's do the operation:

```json
/opt/jans/jans-cli/config-cli.py --operation-id patch-oauth-uma-resources-by-id --url-suffix id:1800.c4e0d1b6-e731-4c8d-a0ab-66784349a4da --data /tmp/patch-uma.json

        

Getting access token for scope https://jans.io/oauth/config/uma/resources.write
Server Response:
{
  "dn": "jansId=1800.c4e0d1b6-e731-4c8d-a0ab-66784349a4da,ou=resources,ou=uma,o=jans",
  "inum": null,
  "id": "1800.c4e0d1b6-e731-4c8d-a0ab-66784349a4da",
  "name": "Jans Cofig Api Uma Resource /jans-config-api/api/v1/attributes",
  "iconUri": "http://www.jans.io/img/scim_logo.png",
  "scopes": [
    "inum=CACA-0B30,ou=scopes,o=jans"
  ],
  "scopeExpression": null,
  "clients": [
    "inum=1801.6e8351d4-5d3f-4773-b632-6c84bf8207cd,ou=clients,o=jans"
  ],
  "resources": [
    "https://testjans.gluu.com/jans-config-api/api/v1/attributes"
  ],
  "rev": "1",
  "creator": "inum=d206dd87-3a22-465d-bd25-bec9313cd42d,ou=people,o=json",
  "description": null,
  "type": null,
  "creationDate": null,
  "expirationDate": null,
  "deletable": false
}
```
you must see that `deletable` updated to `false`.

### Janssen Fido2 Configuration

Using Janssen CLI, You can `get/update` Fido2 properties.
To get the information of Janssen Fido2 CLI, run the following command:

```
/opt/jans/jans-cli/config-cli.py --info ConfigurationFido2

Operation ID: get-properties-fido2
  Description: Gets Jans Authorization Server Fido2 configuration properties.
Operation ID: put-properties-fido2
  Description: Updates Fido2 configuration properties.
  Schema: /components/schemas/JansFido2DynConfiguration

To get sample shema type /opt/jans/jans-cli/config-cli.py --schema <schma>, for example /opt/jans/jans-cli/config-cli.py --schema /components/schemas/JansFido2DynConfiguration
```

- **__get-properties-fido2__**

To get the properties of Janssen Fido2 Configuration, run below command:
`/opt/jans/jans-cli/config-cli.py --operation-id get-properties-fido2`

It will return the result as below:

```text
Getting access token for scope https://jans.io/oauth/config/fido2.readonly
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

- **__put-properties-fido2__**

To perform this operation, let's check the schema first.

```text
/opt/jans/jans-cli/config-cli.py --schema /components/schemas/JansFido2DynConfiguration > /tmp/fido2-schema.json
```

This command will create a fido2 schema file on `/tmp/`. You can edit this file depending on the requirements:

![janssen fido2 configuration](img/cl-fido2-update.png)

Now let's do the operation:

```text
/opt/jans/jans-cli/config-cli.py --operation-id put-properties-fido2 --data /tmp/fido2-schema.json

Getting access token for scope https://jans.io/oauth/config/fido2.write
Server Response:
{
  "issuer": "https://server.example.com/",
  "baseEndpoint": "https://server.example.com/fido2/restv1",
  "cleanServiceInterval": null,
  "cleanServiceBatchChunkSize": null,
  "useLocalCache": false,
  "disableJdkLogger": false,
  "loggingLevel": "INFO",
  "loggingLayout": null,
  "externalLoggerConfiguration": null,
  "metricReporterInterval": null,
  "metricReporterKeepDataDays": null,
  "metricReporterEnabled": true,
  "personCustomObjectClassList": [],
  "fido2Configuration": {
    "authenticatorCertsFolder": null,
    "mdsCertsFolder": null,
    "mdsTocsFolder": null,
    "serverMetadataFolder": null,
    "requestedParties": [
      {
        "name": null,
        "domains": null
      },
      {
        "name": null,
        "domains": null
      }
    ],
    "userAutoEnrollment": true,
    "unfinishedRequestExpiration": null,
    "authenticationHistoryExpiration": null,
    "requestedCredentialTypes": []
  }
}
```

You may find that I have updated to `logginglabel:INFO` from `NULL`.


### Jans Authorization Server

To get info about Jans Authorization Server operations:

```text
/opt/jans/jans-cli/config-cli.py --info Configuration


Operation ID: get-properties
  Description: Gets all Jans authorization server configuration properties.
Operation ID: patch-properties
  Description: Partially modifies Jans authorization server AppConfiguration properties.
  Schema: Array of /components/schemas/PatchRequest

To get sample shema type /opt/jans/jans-cli/config-cli.py --schema <schma>, for example /opt/jans/jans-cli/config-cli.py --schema /components/schemas/PatchRequest

```

Jans Authorization server has two operations `id` to `get/modify` its properties.

- **__get-properties__**

It returns all the information of the Jans Authorization server.

```text
/opt/jans/jans-cli/config-cli.py --operation-id get-properties


Getting access token for scope https://jans.io/oauth/jans-auth-server/config/properties.readonly
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
    "['code', 'token']",
    "['id_token', 'code', 'token']",
    "['id_token']",
    "['code']",
    "['token']",
    "['id_token', 'code']",
    "['id_token', 'token']"
  ],
  "responseModesSupported": [
    "query",
    "form_post",
    "fragment"
  ],
  "grantTypesSupported": [
    "password",
    "authorization_code",
    "refresh_token",
    "urn:ietf:params:oauth:grant-type:device_code",
    "client_credentials",
    "urn:ietf:params:oauth:grant-type:uma-ticket",
    "implicit"
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
    "authorization_code",
    "refresh_token",
    "urn:ietf:params:oauth:grant-type:device_code",
    "client_credentials",
    "urn:ietf:params:oauth:grant-type:uma-ticket",
    "implicit"
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
  "pairwiseCalculationKey": "rYX4K1hkDOAc0ie6ESr6T4N2z",
  "pairwiseCalculationSalt": "1smvd6pswngMcjB4xVcMuIiuyH",
  "shareSubjectIdBetweenClientsWithSameSectorId": true,
  "webKeysStorage": "keystore",
  "dnName": "CN=Jans Auth CA Certificates",
  "keyStoreFile": "/etc/certs/jans-auth-keys.jks",
  "keyStoreSecret": "nXfbJvxuVRNi",
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


- **__partially modify jans auth server__**

```text
Operation ID: patch-properties
  Description: Partially modifies Jans authorization server AppConfiguration properties.
  Schema: Array of /components/schemas/PatchRequest

To get sample shema type /opt/jans/jans-cli/config-cli.py --schema <schma>, for example /opt/jans/jans-cli/config-cli.py --schema /components/schemas/PatchRequest
```
Let's look at the schema first:

```text
/opt/jans/jans-cli/config-cli.py --schema /components/schemas/PatchRequest > /tmp/patch-jans-auth.json
```

It will create a `.json` file in `/tmp` with schema.

Let's modify this schema:
`nano /tmp/patch-jans-auth.json`

![patch jans auth](img/cl-patch-jans-auth.png)

This schema has three properties; `op`, `path` & `value`.
Let's perform a `replace` operation at `cibaEnabled` to change it from `false` to `true`.
So, the `.json` file will look like this:

```text
[
  {
    "op": "replace",
    "path": "cibaEnabled",
    "value": true
  }
]
```

Don't forget to use **square brackets** (`[]`). Otherwise, it won't work.

Now, let's do the operation.

```commandline
/opt/jans/jans-cli/config-cli.py --operation-id patch-properties --data /tmp/patch-jans-auth.json
```

If you run the following command line, you must see that `cibaEnabled` is `true`:

![Update result Jans Auth](img/cl-update-jans-auth.png)


### SMTP Configuration
Let's get the information of **SMTP Configuration** using Janssen CLI.

```commandline
/opt/jans/jans-cli/config-cli.py --info ConfigurationSMTP


Operation ID: get-config-smtp
  Description: Returns SMTP server configuration.
Operation ID: post-config-smtp
  Description: Adds SMTP server configuration.
  Schema: /components/schemas/SmtpConfiguration
Operation ID: put-config-smtp
  Description: Updates SMTP server configuration.
  Schema: /components/schemas/SmtpConfiguration
Operation ID: delete-config-smtp
  Description: Deletes SMTP server configuration.
Operation ID: test-config-smtp
  Description: Test SMTP server configuration.

To get sample shema type /opt/jans/jans-cli/config-cli.py --schema <schma>, for example /opt/jans/jans-cli/config-cli.py --schema /components/schemas/SmtpConfiguration
```
As we see, we can perform many operations such as `update`, `delete`, `test`, `post`, etc. Let's do some operations.

- **_get-config-smtp_**

To view the current SMTP server configuration, run the following command line:

```commandline
/opt/jans/jans-cli/config-cli.py --operation-id get-config-smtp
```

It will show your SMTP server configuration as below:

```text
Getting access token for scope https://jans.io/oauth/config/smtp.readonly
{
  "host": "webmail.gluu.org",
  "port": 587,
  "requiresSsl": null,
  "serverTrust": null,
  "fromName": null,
  "fromEmailAddress": null,
  "requiresAuthentication": null,
  "userName": null,
  "password": "fHze8OEMs1MzkGhWw/29eg=="
}
```

- **_post-config-smtp_**

This operation can be performed to update/post a new SMTP configuration on your Janssen server.
Let's see the schema first:

```commandline
/opt/jans/jans-cli/config-cli.py --schema /components/schemas/SmtpConfiguration > /tmp/smtp.json
```

It will create a `.json` file. Let's modify this file:

```commandline
nano /tmp/smtp.json
```

![smtp update configuration](img/cl-update-smtp.png)

You can update each of its properties. To perform this operation, run the following command:

```commandline
/opt/jans/jans-cli/config-cli.py --operation-id post-config-smtp --data /tmp/smtp.json
```

If you run the following command, it will update your janssen smtp server and print on the screen as below.

```text
Getting access token for scope https://jans.io/oauth/config/smtp.write
Server Response:
{
  "host": "webmail.gluu.org",
  "port": 587,
  "requiresSsl": null,
  "serverTrust": null,
  "fromName": null,
  "fromEmailAddress": null,
  "requiresAuthentication": null,
  "userName": null,
  "password": "fHze8OEMs1MzkGhWw/29eg=="
}
```

**_put-config-smtp_**

To update smtp server, simply change any information on `/tmp/smtp.json` file and run the following command:

```commandline
/opt/jans/jans-cli/config-cli.py --operation-id put-config-smtp --data /tmp/smtp.json
```
It will update the information.

### Logging Configuration

To `view/update` logging configuration, let's get the information of logging Configuration.

`/opt/jans/jans-cli/config-cli.py --info ConfigurationLogging`

```text
Operation ID: get-config-logging
  Description: Returns Jans Authorization Server logging settings.
Operation ID: put-config-logging
  Description: Updates Jans Authorization Server logging settings.
  Schema: /components/schemas/LoggingConfiguration

To get sample shema type /opt/jans/jans-cli/config-cli.py --schema <schma>, for example /opt/jans/jans-cli/config-cli.py --schema /components/schemas/LoggingConfiguration
```

- **_get-config-logging_**

```text
/opt/jans/jans-cli/config-cli.py --operation-id get-config-logging

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

- **_put-config-logging_**

To update logging configuration, get the schema first:

```commandline
/opt/jans/jans-cli/config-cli.py --schema /components/schemas/LoggingConfiguration > /tmp/log-config.json


{
  "loggingLevel": "FATAL",
  "loggingLayout": "text",
  "httpLoggingEnabled": true,
  "disableJdkLogger": true,
  "enabledOAuthAuditLogging": false,
  "externalLoggerConfiguration": null,
  "httpLoggingExludePaths": [
    "/auth/img",
    "/auth/stylesheet"
  ]
}
```

let's update the schema:
```text
nano /tmp/log-config.json
```

Here I have updated `loggingLevel` to `DEBUG` and `enabledOAuditLogging` to `true` as below image.

![updated logging](img/cl-update-logging.png)


Let's do the operation:

```commandline
/opt/jans/jans-cli/config-cli.py --operation-id put-config-logging --data /tmp/log-config.json
```

You will get the updated result as below:

```text

Getting access token for scope https://jans.io/oauth/config/logging.write
Server Response:
{
  "loggingLevel": "DEBUG",
  "loggingLayout": "json",
  "httpLoggingEnabled": false,
  "disableJdkLogger": false,
  "enabledOAuthAuditLogging": true,
  "externalLoggerConfiguration": null,
  "httpLoggingExludePaths": [
    "/auth/img",
    "/auth/stylesheet"
  ]
}
```


### OpenID Connect Client Configuration

Let's get the information of OpenID Connect Client Configuration:

```text
/opt/jans/jans-cli/config-cli.py --info OAuthOpenIDConnectClients



Operation ID: get-oauth-openid-clients
  Description: Gets list of OpenID Connect clients
  Parameters:
  limit: Search size - max size of the results to return. [integer]
  pattern: Search pattern. [string]
Operation ID: post-oauth-openid-clients
  Description: Create new OpenId connect client
  Schema: /components/schemas/Client
Operation ID: put-oauth-openid-clients
  Description: Update OpenId Connect client.
  Schema: /components/schemas/Client
Operation ID: get-oauth-openid-clients-by-inum
  Description: Get OpenId Connect Client by Inum.
  url-suffix: inum
Operation ID: delete-oauth-openid-clients-by-inum
  Description: Delete OpenId Connect client.
  url-suffix: inum
Operation ID: patch-oauth-openid-clients-by-inum
  Description: Update modified properties of OpenId Connect client by Inum.
  url-suffix: inum
  Schema: Array of /components/schemas/PatchRequest

To get sample shema type /opt/jans/jans-cli/config-cli.py --schema <schma>, for example /opt/jans/jans-cli/config-cli.py --schema /components/schemas/PatchRequest

```

- **_get-oauth-openid-clients_**

To get the openid clients, run the following command:

```text
/opt/jans/jans-cli/config-cli.py --operation-id get-oauth-openid-clients




Getting access token for scope https://jans.io/oauth/config/openid/clients.readonly
[
  {
    "dn": "inum=1801.30bd0499-9dc0-48dc-9eb3-96b80a8da856,ou=clients,o=jans",
    "inum": "1801.30bd0499-9dc0-48dc-9eb3-96b80a8da856",
    "clientSecret": "zITPCsgIfmDTkKWkonuu+g==",
    "frontChannelLogoutUri": null,
    "frontChannelLogoutSessionRequired": false,
    "registrationAccessToken": null,
    "clientIdIssuedAt": null,
    "clientSecretExpiresAt": null,
    "redirectUris": null,
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
      "inum=CACA-B9D4,ou=scopes,o=jans",
      "inum=CACA-5AA4,ou=scopes,o=jans",
      "inum=CACA-F1E3,ou=scopes,o=jans",
      "inum=CACA-A1BD,ou=scopes,o=jans",
      "inum=CACA-113F,ou=scopes,o=jans",
      "inum=CACA-22E5,ou=scopes,o=jans",
      "inum=CACA-E6DE,ou=scopes,o=jans",
      "inum=CACA-B965,ou=scopes,o=jans",
      "inum=CACA-7FB9,ou=scopes,o=jans",
      "inum=CACA-3B0C,ou=scopes,o=jans",
      "inum=CACA-FD1D,ou=scopes,o=jans",
      "inum=CACA-7419,ou=scopes,o=jans",
      "inum=CACA-55A1,ou=scopes,o=jans",
      "inum=CACA-7B22,ou=scopes,o=jans",
      "inum=CACA-66AE,ou=scopes,o=jans",
      "inum=CACA-8283,ou=scopes,o=jans",
      "inum=CACA-1A74,ou=scopes,o=jans",
      "inum=CACA-CCFC,ou=scopes,o=jans",
      "inum=CACA-EABC,ou=scopes,o=jans",
      "inum=CACA-E7BB,ou=scopes,o=jans",
      "inum=CACA-EF5F,ou=scopes,o=jans",
      "inum=CACA-179E,ou=scopes,o=jans",
      "inum=CACA-174C,ou=scopes,o=jans",
      "inum=CACA-B36D,ou=scopes,o=jans",
      "inum=CACA-88E3,ou=scopes,o=jans",
      "inum=CACA-C1F5,ou=scopes,o=jans",
      "inum=CACA-82B8,ou=scopes,o=jans",
      "inum=CACA-016F,ou=scopes,o=jans",
      "inum=CACA-8F20,ou=scopes,o=jans",
      "inum=CACA-79A1,ou=scopes,o=jans"
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
    "dn": "inum=1001.3c40746d-63a6-478e-b06d-8f49bb984e4f,ou=clients,o=jans",
    "inum": "1001.3c40746d-63a6-478e-b06d-8f49bb984e4f",
    "clientSecret": "eVXRaEojULdohgOUbMeFPA==",
    "frontChannelLogoutUri": "https://testjans.imshakil.me/identity/ssologout.htm",
    "frontChannelLogoutSessionRequired": true,
    "registrationAccessToken": null,
    "clientIdIssuedAt": null,
    "clientSecretExpiresAt": null,
    "redirectUris": [
      "https://testjans.imshakil.me/identity/scim/auth",
      "https://testjans.imshakil.me/identity/authcode.htm",
      "https://testjans.imshakil.me/jans-auth/restv1/uma/gather_claims?authentication=true"
    ],
    "claimRedirectUris": [
      "https://testjans.imshakil.me/jans-auth/restv1/uma/gather_claims"
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
      "https://testjans.imshakil.me/identity/finishlogout.htm"
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
    "deletable": null,
    "jansId": null
  },
  {
    "dn": "inum=1202.049eb91f-6339-4e83-ac83-55df359f6c9c,ou=clients,o=jans",
    "inum": "1202.049eb91f-6339-4e83-ac83-55df359f6c9c",
    "clientSecret": null,
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
    "clientName": "SCIM Requesting Party Client",
    "idTokenTokenBindingCnf": null,
    "logoUri": null,
    "clientUri": null,
    "policyUri": null,
    "tosUri": null,
    "jwksUri": null,
    "jwks": "{  \"keys\" : [ {    \"kty\" : \"RSA\",    \"e\" : \"AQAB\",    \"use\" : \"sig\",    \"crv\" : \"\",    \"kid\" : \"0b60383f-13c9-4064-9de1-7946724c0bbc_sig_rs256\",    \"x5c\" : [ \"MIIDCTCCAfGgAwIBAgIgLJXeu/MFKl144/y6Xj55fqA+RWTWE0VgEhOSb1CmITcwDQYJKoZIhvcNAQELBQAwJDEiMCAGA1UEAwwZSmFucyBBdXRoIENBIENlcnRpZmljYXRlczAeFw0yMTAxMTUyMjM5MTNaFw0yMjAxMTUyMjM5MjNaMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCK7v3/S3Qn1puC16XM1mChQa3ygnAMoDQivlDj0AxLmSEO4ulmubVTbsvBFkt45+kKLvDUDozaNFhhNtnX1vZt37Fnd7/lnsODVn7GOrc8pGyiR048MfmPONO77LLqyf/ByrxhMpBYTR22kniRdQMc1+dHjWHIGzvmsQgMuefT2U81fqRpL0dkL2xDs7OEHm6BjQUoJgSXnf5BmWvdf+WiYPe5DXe6g56LdyZwwgN0vcx1IoYSMvmHlZyNjzyOPhCNgLPexXFpniBcFc5b5nGISgpn37yjVm4UIIMMGajv7jNJZKXkKZ+F4KRnuIByTYHwTqiHEwQoleRhlKJpNbthAgMBAAGjJzAlMCMGA1UdJQQcMBoGCCsGAQUFBwMBBggrBgEFBQcDAgYEVR0lADANBgkqhkiG9w0BAQsFAAOCAQEAhYEvDDBZ1Sl8b4Ng0aSXN+zw4nwS7pXBAwj4yLid47D6FnomFw25PYDAghO7YehDW18zjgbON69L5PH9Tqnq/Jzu1qAWpjWpwBVMRogCiGip/Kk59HmQos5/ckm9kgKrWhUw7vEqramHw40uqjXWuDfykWDbSqRYX2rccubSGwRsocMoMEoeFXLtyeBgjqoFY1Uqt4VTMdjTv6ekD+BLVfXOTlhemHSRXBG7GJVpwebYIyN/lx7LFAHYqbBi0adyGTI0/HQBtxMQeu57qy9oP+Q9gKse5QAz5Zesld71bKmUOshSGg4ks1JHH70wtRNPdQOgfdaaHHVzoyZo6FcMqg==\" ],    \"exp\" : 1642286363453,    \"alg\" : \"RS256\",    \"n\" : \"iu79_0t0J9abgtelzNZgoUGt8oJwDKA0Ir5Q49AMS5khDuLpZrm1U27LwRZLeOfpCi7w1A6M2jRYYTbZ19b2bd-xZ3e_5Z7Dg1Z-xjq3PKRsokdOPDH5jzjTu-yy6sn_wcq8YTKQWE0dtpJ4kXUDHNfnR41hyBs75rEIDLnn09lPNX6kaS9HZC9sQ7OzhB5ugY0FKCYEl53-QZlr3X_lomD3uQ13uoOei3cmcMIDdL3MdSKGEjL5h5WcjY88jj4QjYCz3sVxaZ4gXBXOW-ZxiEoKZ9-8o1ZuFCCDDBmo7-4zSWSl5CmfheCkZ7iAck2B8E6ohxMEKJXkYZSiaTW7YQ\"  }, {    \"kty\" : \"RSA\",    \"e\" : \"AQAB\",    \"use\" : \"sig\",    \"crv\" : \"\",    \"kid\" : \"b3c52773-7377-45a2-97f8-0e8cc3895342_sig_rs384\",    \"x5c\" : [ \"MIIDCjCCAfKgAwIBAgIhAPXe8+Rao043PUbs+WlpDB17Gyq8osq3tl/4d2qb38eTMA0GCSqGSIb3DQEBDAUAMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjEwMTE1MjIzOTE0WhcNMjIwMTE1MjIzOTIzWjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA0DJHMKIC9rsPUurwE9aRXJ0YCi0hUrfZJ7CP3bnE5dbjSFgO9jSpFT+BS8SEaEWhTMUsrLAXqANTqPfEz3ITWhVEHdDvBDrSrpjQQWcEksxYP4/ZaScnFg09yt6Y6U3UMzwPijlzvq84xsJ1KWaz2klCSWvb/jQ4RJj6SG4eTApX0A2cHJmHwJ1oM9SwQe+eeKprd+uZj12iouWPjah4ztz2PzzAmYh8l3Wlycw7hs5OQnxU2ZnygSMYh/2V5cKVK22FAp3fE3QxLXYmn4hkmSoHcy0UjRxhSS5Q8m4AcJzdfUauMqpIJ0yL/W9jkAVdsgTMcacjJF9eesVyhcwwgwIDAQABoycwJTAjBgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAwDQYJKoZIhvcNAQEMBQADggEBAJHB8bKmnQpllotN3dDg1heS2zqdsqyPK7K/5TFBRpXJV2LrIIAHEw5NjIh3vxva/dUwiJD/9uGpi3Xmn9wVFXhIxzYtSAGQmYxFEtkKsZZ7HmndmFbqbeHYv2q266yBQxx1GoZELyU85rrF+hB+/ZSdeMdqjq+Tyr25NwSHwDkGlYui3WAqLH0l0LqIvtSO5bgv6fpqhXe4H8PJ41EUsChN0HrIXNMJLbdvw8tnznJSMbaqKXjKCh0qr9GvHhxvZkDyklWRFTelg8Ct2xiH/eGeu2jwwc/QndcxNq0lqcFzyzp66oTUIdwrQitP9lgipB4c42jefoZjhv6mQaDbuO8=\" ],    \"exp\" : 1642286363453,    \"alg\" : \"RS384\",    \"n\" : \"0DJHMKIC9rsPUurwE9aRXJ0YCi0hUrfZJ7CP3bnE5dbjSFgO9jSpFT-BS8SEaEWhTMUsrLAXqANTqPfEz3ITWhVEHdDvBDrSrpjQQWcEksxYP4_ZaScnFg09yt6Y6U3UMzwPijlzvq84xsJ1KWaz2klCSWvb_jQ4RJj6SG4eTApX0A2cHJmHwJ1oM9SwQe-eeKprd-uZj12iouWPjah4ztz2PzzAmYh8l3Wlycw7hs5OQnxU2ZnygSMYh_2V5cKVK22FAp3fE3QxLXYmn4hkmSoHcy0UjRxhSS5Q8m4AcJzdfUauMqpIJ0yL_W9jkAVdsgTMcacjJF9eesVyhcwwgw\"  }, {    \"kty\" : \"RSA\",    \"e\" : \"AQAB\",    \"use\" : \"sig\",    \"crv\" : \"\",    \"kid\" : \"bf5b0a2f-c977-4e0d-9a3c-bd8faef18e48_sig_rs512\",    \"x5c\" : [ \"MIIDCTCCAfGgAwIBAgIgDrTQ+5YMX6eyx/WzSSJqS9gEsHlewOmswtphb3jE4/4wDQYJKoZIhvcNAQENBQAwJDEiMCAGA1UEAwwZSmFucyBBdXRoIENBIENlcnRpZmljYXRlczAeFw0yMTAxMTUyMjM5MTVaFw0yMjAxMTUyMjM5MjNaMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCWfXYlVgDnZpXP7XdEfSYbmJbKgVh0VzhRUzoXc1UTK+FZRM4NNuQBwkfF1X1vMg5x+1Dp1fhNw6anzf5oRjkgog6hEOucWDXq9+jjlkJPnUrYD9/yrinnBQPjsv2NFxWu7qI3KYUIWe96blPiqO1pJjUPk6dybCYoNoxk/0ut07/9uXcf3qVawqypGz4FHeiVz3SUJ1P17h59CS0+nCBT5OkR+rhT4XNc6qcqO3YDX/mj1vahuJijztoQQN82xp31bod9KsBezHIpuW8aM+steNz/aOn49bLYbNxneXV032wPmTZHr0mxxIlS95Vux0y/FVMnt/D1/L5SbWV/SxVDAgMBAAGjJzAlMCMGA1UdJQQcMBoGCCsGAQUFBwMBBggrBgEFBQcDAgYEVR0lADANBgkqhkiG9w0BAQ0FAAOCAQEAaEMcSaALKCCLjQ9GFyGQT3pOdT5AplJSD4ql+dISq44atxuDGSyrKyGehW07djBlUZFW8aDDMsOnQMjC049RU1LuU77FB9cmFhWAFCGIPTFFDdQrCK+LYB9LwSRX7kqBHsHZhqH9STdRMamakLnNuSJS5YzQNFziCIEUofkg0xe5WsAB4GdJrOfvy7JF0UnmjXhwpvZY/65b/Vv0o28j46QS4w769ltZwxIABKom0jdbfbn41UeLTlwgRftXh2/k59W5ma3lZPO/zi2aOl9nuj+7lXIUQKLoBUgDBYJ+8SyF0HhqDvlWijb29eJlPKKHkFRiQTo5Cbs704GWK8bx1A==\" ],    \"exp\" : 1642286363453,    \"alg\" : \"RS512\",    \"n\" : \"ln12JVYA52aVz-13RH0mG5iWyoFYdFc4UVM6F3NVEyvhWUTODTbkAcJHxdV9bzIOcftQ6dX4TcOmp83-aEY5IKIOoRDrnFg16vfo45ZCT51K2A_f8q4p5wUD47L9jRcVru6iNymFCFnvem5T4qjtaSY1D5OncmwmKDaMZP9LrdO__bl3H96lWsKsqRs-BR3olc90lCdT9e4efQktPpwgU-TpEfq4U-FzXOqnKjt2A1_5o9b2obiYo87aEEDfNsad9W6HfSrAXsxyKblvGjPrLXjc_2jp-PWy2GzcZ3l1dN9sD5k2R69JscSJUveVbsdMvxVTJ7fw9fy-Um1lf0sVQw\"  }, {    \"kty\" : \"EC\",    \"use\" : \"sig\",    \"crv\" : \"P-256\",    \"kid\" : \"fb0ef9a4-3b7f-4880-b896-57449de9ece8_sig_es256\",    \"x5c\" : [ \"MIIBfTCCASOgAwIBAgIgQgvjjFY0ZMqTJ3pbRsXCrIcHCdP64r+VwPgHUCzhTwgwCgYIKoZIzj0EAwIwJDEiMCAGA1UEAwwZSmFucyBBdXRoIENBIENlcnRpZmljYXRlczAeFw0yMTAxMTUyMjM5MTVaFw0yMjAxMTUyMjM5MjNaMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAAT7B1VR+pnR8J0Omavpaeyq5K2aiJZXXQvuHn6piFZd7Gfr0rzzA9hSTgGZ84yOA96ZkV8XS71cuzP24Q72SsCKoycwJTAjBgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAwCgYIKoZIzj0EAwIDSAAwRQIgPMWe6opagvCW0nkMASqpy7aQmnOw2cHFk8gqc7ztZyoCIQCK0xN9Kc3my9qGPYM75lUx2AwAzgyhkdWzo80jd+BVkA==\" ],    \"x\" : \"APsHVVH6mdHwnQ6Zq-lp7KrkrZqIllddC-4efqmIVl3s\",    \"y\" : \"Z-vSvPMD2FJOAZnzjI4D3pmRXxdLvVy7M_bhDvZKwIo\",    \"exp\" : 1642286363453,    \"alg\" : \"ES256\"  }, {    \"kty\" : \"EC\",    \"use\" : \"sig\",    \"crv\" : \"P-384\",    \"kid\" : \"7cc680c6-c7d5-4a5c-885e-1e591dc1511d_sig_es384\",    \"x5c\" : [ \"MIIBuTCCAUCgAwIBAgIgDPX0NX82/puI5AxdpOoQxPrsODbEGF3usqHUizJFvd4wCgYIKoZIzj0EAwMwJDEiMCAGA1UEAwwZSmFucyBBdXRoIENBIENlcnRpZmljYXRlczAeFw0yMTAxMTUyMjM5MTZaFw0yMjAxMTUyMjM5MjNaMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwdjAQBgcqhkjOPQIBBgUrgQQAIgNiAAR88zjor1uRqZg+UFFF7VrUyPXrGlkojxw2WiJsk3AKr6IbZNhGasSxjLV24Gjoo8BJUdcwX4DcOufpspU0KBUCaNY0rJjV6UM8kiyqDCYoKW0UpKxx1eXwm5m1AmCjkOSjJzAlMCMGA1UdJQQcMBoGCCsGAQUFBwMBBggrBgEFBQcDAgYEVR0lADAKBggqhkjOPQQDAwNnADBkAjAjyJsnKck1+hkXjAoN5PpLLwua4i6+KfW6fBeOXbwGjN7WkfJ595KstuPMI7GzP/ACMAaHxFdnih0lkfWJ6lwr3IXn4eon/yAskkN24DrK0Q9e1mJkrDU2uc3ybh796+f3IQ==\" ],    \"x\" : \"fPM46K9bkamYPlBRRe1a1Mj16xpZKI8cNloibJNwCq-iG2TYRmrEsYy1duBo6KPA\",    \"y\" : \"SVHXMF-A3Drn6bKVNCgVAmjWNKyY1elDPJIsqgwmKCltFKSscdXl8JuZtQJgo5Dk\",    \"exp\" : 1642286363453,    \"alg\" : \"ES384\"  }, {    \"kty\" : \"EC\",    \"use\" : \"sig\",    \"crv\" : \"P-521\",    \"kid\" : \"98011bc0-8566-41ec-a64b-e0fca1fb22a2_sig_es512\",    \"x5c\" : [ \"MIICBjCCAWegAwIBAgIhAM1ik4Lr1/favN6xSF65r92aemqYgpCMfLO9vVAtmOO4MAoGCCqGSM49BAMEMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjEwMTE1MjIzOTE2WhcNMjIwMTE1MjIzOTIzWjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMIGbMBAGByqGSM49AgEGBSuBBAAjA4GGAAQAZG1VCPrKI5D9iLqkibhaKHH/j3SmDjhr9em63SIztO6gFXtEFNW4Jqc7oTHHcOv6VpagxX5XTzLinhpUQRuzEFUAw39iGsIJbwvGWarrw5/OCZaKPNRVA/kzAf9dl0I17EMyvGP0ctm6t4qqY8PjqygjA2nBoZWwLnhZu9q54IrdT+6jJzAlMCMGA1UdJQQcMBoGCCsGAQUFBwMBBggrBgEFBQcDAgYEVR0lADAKBggqhkjOPQQDBAOBjAAwgYgCQgHydUf16d/5yvFP5NGzBHOYY7sQ5jV0i2ICC+Vdh02jVTcmaAy6f2uraa6eL5X9SrfiwtR9HvkMqB/svMzAv999mQJCAL3LrtobouAY/i4Hxvfgt/H9Sf5G47zbO5QJBoqkOA9Q1OG4paRIVSQ3d1iZFvSPLfmbqwXee0aq8H9CU192+y52\" ],    \"x\" : \"ZG1VCPrKI5D9iLqkibhaKHH_j3SmDjhr9em63SIztO6gFXtEFNW4Jqc7oTHHcOv6VpagxX5XTzLinhpUQRuzEFU\",    \"y\" : \"AMN_YhrCCW8Lxlmq68OfzgmWijzUVQP5MwH_XZdCNexDMrxj9HLZureKqmPD46soIwNpwaGVsC54WbvaueCK3U_u\",    \"exp\" : 1642286363453,    \"alg\" : \"ES512\"  } ]}",
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
    "tokenEndpointAuthMethod": "private_key_jwt",
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
    "deletable": null,
    "jansId": null
  },
  {
    "dn": "inum=1201.95be8034-0b72-4add-959c-3edf98b91af6,ou=clients,o=jans",
    "inum": "1201.95be8034-0b72-4add-959c-3edf98b91af6",
    "clientSecret": null,
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
    "clientName": "SCIM Resource Server Client",
    "idTokenTokenBindingCnf": null,
    "logoUri": null,
    "clientUri": null,
    "policyUri": null,
    "tosUri": null,
    "jwksUri": null,
    "jwks": "{  \"keys\" : [ {    \"kty\" : \"RSA\",    \"e\" : \"AQAB\",    \"use\" : \"sig\",    \"crv\" : \"\",    \"kid\" : \"8e6d654d-2133-45b6-84c4-4fce267d6bee_sig_rs256\",    \"x5c\" : [ \"MIIDCjCCAfKgAwIBAgIhAO4ZTRoknOI/s7Mq9hIT424qwd9tY05Ht2uSgz/CuWmbMA0GCSqGSIb3DQEBCwUAMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjEwMTE1MjIzOTA5WhcNMjIwMTE1MjIzOTE5WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAsircewYbwIlSPYt6H3yFoxn26wSpij6sYsFHe6HcWkxSmbgkYSfTF5n+14PorBhsWzKDY+YODqIH9sNfZAjx3yX/VQpqlXn52Uwt1ZGb8tnwaNbbMkFW2848fqZXxtZHrBUVMbN+jiMec1tnI6ONIsDNJlBB4jJgE/wIThMtl5cmys9/RHmfr6YAnEVEZksFtyaDS3W4f3JsrbgWs1IYcY9MGeAQJ+OpXifb5D0qhSUrDjLBbKCOukvRf6Ue3U/Q4NaxpokHYhqbr/YA6jiZ2XPcJl53HKdpU4eO6V4HP0nuiVi7q1nQhb9f4cnuPnIKYaai759bozXTjByoki6YCQIDAQABoycwJTAjBgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAwDQYJKoZIhvcNAQELBQADggEBAIyswl5l42DSzAJmJTIwNwJUDGORPPoj3CQlBsaT5pQ2Ykiqv1gqBLjNZw/Em1TYeD2Udm8Gjn5sPUxcwdiiH1zATYPyxk/uv4POkwQBw2n3L8OA4pl0d6So5HVzA1CS23Sy1CSKK0OiRVMym0TxzutYIJB4Usqg2KhgwJBzonoAHY8IXd9QvlV7+4Cb2gE4jpF8kUKgF3dCqQVZybDrEFQ0xf8bDPWp4CBTTVoHsyr/bn7UFpHO1FxKAWJglNa6cNOv1a/1QzjPK2OZGnYciHf/BT6SWRkJukRNs/O3jrfJJsw+LSlQSjpcZPMUwJ3+JJ5Eax41bseue1mLa1js2h4=\" ],    \"exp\" : 1642286359278,    \"alg\" : \"RS256\",    \"n\" : \"sircewYbwIlSPYt6H3yFoxn26wSpij6sYsFHe6HcWkxSmbgkYSfTF5n-14PorBhsWzKDY-YODqIH9sNfZAjx3yX_VQpqlXn52Uwt1ZGb8tnwaNbbMkFW2848fqZXxtZHrBUVMbN-jiMec1tnI6ONIsDNJlBB4jJgE_wIThMtl5cmys9_RHmfr6YAnEVEZksFtyaDS3W4f3JsrbgWs1IYcY9MGeAQJ-OpXifb5D0qhSUrDjLBbKCOukvRf6Ue3U_Q4NaxpokHYhqbr_YA6jiZ2XPcJl53HKdpU4eO6V4HP0nuiVi7q1nQhb9f4cnuPnIKYaai759bozXTjByoki6YCQ\"  }, {    \"kty\" : \"RSA\",    \"e\" : \"AQAB\",    \"use\" : \"sig\",    \"crv\" : \"\",    \"kid\" : \"10a2a7f6-cabf-41c0-9d62-37644f214e1c_sig_rs384\",    \"x5c\" : [ \"MIIDCjCCAfKgAwIBAgIhAPAwaemYVwozpAJBHURxIh6oD+BjzEvhLegWbdsM7/6zMA0GCSqGSIb3DQEBDAUAMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjEwMTE1MjIzOTEwWhcNMjIwMTE1MjIzOTE5WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA1s5Kx0ltPJswSizHDNTA786pyU3bqT5dym0O/9RUs1ishsp76QjytsYdxRZzzaYB4CcEaVRj5ZIZQwt0JNqbfx/MPiMEvpJhZHEruZRc1EsE3kUCx6yInBIuHk6yKsdYoAKiHa0dJOMirTcElZyCCzq83QBKpYdX7kV+i29kF/lOSvPeAsizN8HSmB29Hhy30nB3GmcYwghHAXCSpG5g467iGBi+gOMxRm7g/Uj/WSYIDc8CyGyd6iHPK9smCyna5cCfRtuGTAf+/fnfiK5IFrGXvDK5ggM+cbKBope7RceSawN28kjVtt+gY6oLvI3JrV0V33qKTC30JnPJCxTZ4wIDAQABoycwJTAjBgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAwDQYJKoZIhvcNAQEMBQADggEBAGT0S+ke7qG9b7VS553n2JLTB13kSlXrBoa1XX4NlhnHnWO/w/YTFAi3jGVmgCesTr+2XBBFAIdOD9bC397Ufi8xd3PEDhNQKpWThhJpasMJvsiVqnXRsmoN+j5sqtzyUwl1Dsk5zdtDvoV7zeJHJ3niEpS5in6Gw8jMakn84VTekvtQvG6NBWwjVwWc4awSN18YVhMJYbs5J1tTSOcXjdwPD4Ee0WQMBrKqLeo7b9W5/F2Jb3DbfgMhkikVA9jRowyLMgFpyMI6VPneMDWyadnS9YrJHeX1Ml6i3m0uN3Jvp817jhgYFS1L74p3gA9oO0Tin6wnS5EZdWp8kCfrCx4=\" ],    \"exp\" : 1642286359278,    \"alg\" : \"RS384\",    \"n\" : \"1s5Kx0ltPJswSizHDNTA786pyU3bqT5dym0O_9RUs1ishsp76QjytsYdxRZzzaYB4CcEaVRj5ZIZQwt0JNqbfx_MPiMEvpJhZHEruZRc1EsE3kUCx6yInBIuHk6yKsdYoAKiHa0dJOMirTcElZyCCzq83QBKpYdX7kV-i29kF_lOSvPeAsizN8HSmB29Hhy30nB3GmcYwghHAXCSpG5g467iGBi-gOMxRm7g_Uj_WSYIDc8CyGyd6iHPK9smCyna5cCfRtuGTAf-_fnfiK5IFrGXvDK5ggM-cbKBope7RceSawN28kjVtt-gY6oLvI3JrV0V33qKTC30JnPJCxTZ4w\"  }, {    \"kty\" : \"RSA\",    \"e\" : \"AQAB\",    \"use\" : \"sig\",    \"crv\" : \"\",    \"kid\" : \"35a9fa3e-56ac-4408-91c3-e959735222b9_sig_rs512\",    \"x5c\" : [ \"MIIDCjCCAfKgAwIBAgIhAKLT4VuuTD1hb4Gsdd6djKvblI8eSGoksOMt+l2OG01XMA0GCSqGSIb3DQEBDQUAMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjEwMTE1MjIzOTEwWhcNMjIwMTE1MjIzOTE5WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAppg3PsDya/MqabJX27lWx/xy/2zWaSF9+AQ1el34ECdf5e2PGjZIY5Nsx4T2K5uPTz9gH1i/3x8ViCNCX+VGjmyU96LQrLqTP0p9/dI1/E9Llr1igVn8ryyMCf0i8o+y9wEuxaRtiSCtUx65KLzwiGefgZGd7UwrAFce6Hy7VvYDdx1z6AcsJt08CXdDUAIU/M5zq3JCfmpyMFQQHPQ6H6UlK8pFeAGxLNp4IUVmZgUaswnZiaKgglMBqVVOh7bGBIQbmjzbwnIOWVoyuZt6vRfdQUoduya+PwxjwkF4WCRNNJr0NRbMp2aXJJvAHLNPcDXr2pntg4Gb2s40DMuimwIDAQABoycwJTAjBgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAwDQYJKoZIhvcNAQENBQADggEBAIjIpRhEg9fQ9DQXbe7wEtaKpxzTl/FM+BAriQiIek2ZYDy2ux+uHmbTPYwIGNyTpkeAkkqk2xFUjP0QYDo9Q9vqlrWz0mTXDoRDuqyFtHy/4n2xVtAmJSg5us4wFWyBSiSOWyOL3H7TUMlSFjVGLUkweSOTymM29mPYb6xRay/6f1q6jB28LtHCpWm15ZmHa2XdyZTb5WSSD480S7jXcF3ON48AasXBfRxZpZpigF6YLZbxqSU4829RjnexfMJWHy81hVeFH3L+7WUkASpZNfIGlzKSFDqVuK3RncMswCHgMcygdzjem6DDH71qUDpwMVGLIcVQ0ZSjQzFSpd6fKag=\" ],    \"exp\" : 1642286359278,    \"alg\" : \"RS512\",    \"n\" : \"ppg3PsDya_MqabJX27lWx_xy_2zWaSF9-AQ1el34ECdf5e2PGjZIY5Nsx4T2K5uPTz9gH1i_3x8ViCNCX-VGjmyU96LQrLqTP0p9_dI1_E9Llr1igVn8ryyMCf0i8o-y9wEuxaRtiSCtUx65KLzwiGefgZGd7UwrAFce6Hy7VvYDdx1z6AcsJt08CXdDUAIU_M5zq3JCfmpyMFQQHPQ6H6UlK8pFeAGxLNp4IUVmZgUaswnZiaKgglMBqVVOh7bGBIQbmjzbwnIOWVoyuZt6vRfdQUoduya-PwxjwkF4WCRNNJr0NRbMp2aXJJvAHLNPcDXr2pntg4Gb2s40DMuimw\"  }, {    \"kty\" : \"EC\",    \"use\" : \"sig\",    \"crv\" : \"P-256\",    \"kid\" : \"b05a91e8-aa4e-4f6c-860a-3b63f13b16da_sig_es256\",    \"x5c\" : [ \"MIIBfDCCASOgAwIBAgIgFbx/JYXagj82QeW+8XBk/FcdCinm/kX04q4tBOKiQ+gwCgYIKoZIzj0EAwIwJDEiMCAGA1UEAwwZSmFucyBBdXRoIENBIENlcnRpZmljYXRlczAeFw0yMTAxMTUyMjM5MTFaFw0yMjAxMTUyMjM5MTlaMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAARNynAgBRUdqdd5/Os3gpG/Y/CozNptimxnUXdGcDrMrLFBOtwVrB6wYk69Z9U2iY6KPTmgxHQ/MxcHiJOsTfuOoycwJTAjBgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAwCgYIKoZIzj0EAwIDRwAwRAIgeohxfVOK3qgJSNZRzk50PvjeZZ6sIJi1uAOlsaYEMpcCIGPLcDJYIaFfNVQbO/UZjymtiDpoZdb8U39GSanA6HfP\" ],    \"x\" : \"TcpwIAUVHanXefzrN4KRv2PwqMzabYpsZ1F3RnA6zKw\",    \"y\" : \"ALFBOtwVrB6wYk69Z9U2iY6KPTmgxHQ_MxcHiJOsTfuO\",    \"exp\" : 1642286359278,    \"alg\" : \"ES256\"  }, {    \"kty\" : \"EC\",    \"use\" : \"sig\",    \"crv\" : \"P-384\",    \"kid\" : \"30fc8067-9cbd-4a39-8621-6555815e046f_sig_es384\",    \"x5c\" : [ \"MIIBuzCCAUCgAwIBAgIgAeP45q0dJdlruXGW4aKW/728ttfGj31IHROMLnFa5OQwCgYIKoZIzj0EAwMwJDEiMCAGA1UEAwwZSmFucyBBdXRoIENBIENlcnRpZmljYXRlczAeFw0yMTAxMTUyMjM5MTFaFw0yMjAxMTUyMjM5MTlaMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwdjAQBgcqhkjOPQIBBgUrgQQAIgNiAARHgy8tqS+sfqcL4f0LrTnisvAN6QEgylRR/upFj9+FOc7b5eImEzhO+PMhTmNvbutWN+0pVhZ5IcBY9dFSDyBSs9lkWgUDgcKXyg7HCIhnC7CXQfwPKOzH7ZzoD/2D6SWjJzAlMCMGA1UdJQQcMBoGCCsGAQUFBwMBBggrBgEFBQcDAgYEVR0lADAKBggqhkjOPQQDAwNpADBmAjEAp1Gof1uj66oNQsIvSaKBhZkgRoAIweQKVbvcXUTKr3P00HOZMdCrkhYwqXEmDmmzAjEA2sk385nNl/uyUzaW3gfciCxXAeMXUQUjmp6ZDrpuPDleL6jo1u6hoURO30EXBNRR\" ],    \"x\" : \"R4MvLakvrH6nC-H9C6054rLwDekBIMpUUf7qRY_fhTnO2-XiJhM4TvjzIU5jb27r\",    \"y\" : \"VjftKVYWeSHAWPXRUg8gUrPZZFoFA4HCl8oOxwiIZwuwl0H8Dyjsx-2c6A_9g-kl\",    \"exp\" : 1642286359278,    \"alg\" : \"ES384\"  }, {    \"kty\" : \"EC\",    \"use\" : \"sig\",    \"crv\" : \"P-521\",    \"kid\" : \"4c1c8652-06a0-4203-bec7-f5e1408f9a71_sig_es512\",    \"x5c\" : [ \"MIICBTCCAWegAwIBAgIhAOSKu6QwZhmEMffavHu0TX9xI23MKmwdmhS3iFnklzJrMAoGCCqGSM49BAMEMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjEwMTE1MjIzOTExWhcNMjIwMTE1MjIzOTE5WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMIGbMBAGByqGSM49AgEGBSuBBAAjA4GGAAQA79ze+ti5XVPYSVjd5j/pFDMftJC/yKZ67UXMF4hGKYPOpyntzg2DpObgmYwnyituSmE+Nk04aYyMb9wDYPtAKywA4K+G+8M4i3oQ3u2fxxIEcd/k1hl63rAJwaRCHHYSeUuHkDs90aYNkwTotuOta2+IVzLHTFtut78Ifejy41yqG76jJzAlMCMGA1UdJQQcMBoGCCsGAQUFBwMBBggrBgEFBQcDAgYEVR0lADAKBggqhkjOPQQDBAOBiwAwgYcCQSyYB5WGd7N0eutyN4VZFnhivJczXshpkYuz5/XX1HmZwNIwBtc1nTSTclQyZNNNPsadKHW3zrpLmuh7ZJtkAQwjAkIBHOUe7iZNtzzz9o3JX6c3a0GtAkNdV3fDB523Xp+jq6coUzzbUo0qX3tD01iKrxtHh/RY3C7GYNRTOndw9G7efnE=\" ],    \"x\" : \"AO_c3vrYuV1T2ElY3eY_6RQzH7SQv8imeu1FzBeIRimDzqcp7c4Ng6Tm4JmMJ8orbkphPjZNOGmMjG_cA2D7QCss\",    \"y\" : \"AOCvhvvDOIt6EN7tn8cSBHHf5NYZet6wCcGkQhx2EnlLh5A7PdGmDZME6LbjrWtviFcyx0xbbre_CH3o8uNcqhu-\",    \"exp\" : 1642286359278,    \"alg\" : \"ES512\"  } ]}",
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
    "tokenEndpointAuthMethod": "private_key_jwt",
    "tokenEndpointAuthSigningAlg": null,
    "defaultMaxAge": null,
    "requireAuthTime": false,
    "defaultAcrValues": null,
    "initiateLoginUri": null,
    "postLogoutRedirectUris": null,
    "requestUris": null,
    "scopes": [
      "inum=6D99,ou=scopes,o=jans"
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
    "deletable": null,
    "jansId": null
  }
]
```

It will show all the openid clients together. To search using parameters:

```text
/opt/jans/jans-cli/config-cli.py --operation-id get-oauth-openid-clients --endpoint-args limit:2
```

It will show two OpenID clients randomly.

```text

Getting access token for scope https://jans.io/oauth/config/openid/clients.readonly
Calling with params limit=2
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
  }
]
```

### User

The first thing is to do, Let's get some information for the following task:

```
/opt/jans/jans-cli/scim-cli.py --info User
```

In retrun we get,

```
root@testjans:~# /opt/jans/jans-cli/scim-cli.py --info User

Operation ID: get-users
  Description: Query User resources (see section 3.4.2 of RFC 7644)
  Parameters:
  attributes: A comma-separated list of attribute names to return in the response [string]
  excludedAttributes: When specified, the response will contain a default set of attributes minus those listed here (as a comma-separated list) [string]
  filter: An expression specifying the search criteria. See section 3.4.2.2 of RFC 7644 [string]
  startIndex: The 1-based index of the first query result [integer]
  count: Specifies the desired maximum number of query results per page [integer]
  sortBy: The attribute whose value will be used to order the returned responses [string]
  sortOrder: Order in which the sortBy param is applied. Allowed values are "ascending" and "descending" [string]
Operation ID: create-user
  Description: Allows creating a User resource via POST (see section 3.3 of RFC 7644)
  Parameters:
  attributes: A comma-separated list of attribute names to return in the response [string]
  excludedAttributes: When specified, the response will contain a default set of attributes minus those listed here (as a comma-separated list) [string]
  Schema: /components/schemas/UserResource
  Schema: /components/schemas/UserResource
Operation ID: get-user-by-id
  Description: Retrieves a User resource by Id (see section 3.4.1 of RFC 7644)
  url-suffix: id
  Parameters:
  attributes: A comma-separated list of attribute names to return in the response [string]
  excludedAttributes: When specified, the response will contain a default set of attributes minus those listed here (as a comma-separated list) [string]
  id: No description is provided for this parameter [string]
Operation ID: update-user-by-id
  Description: Updates a User resource (see section 3.5.1 of RFC 7644). Update works in a replacement fashion&amp;#58; every
attribute value found in the payload sent will replace the one in the existing resource representation. Attributes 
not passed in the payload will be left intact.

  url-suffix: id
  Parameters:
  attributes: A comma-separated list of attribute names to return in the response [string]
  excludedAttributes: When specified, the response will contain a default set of attributes minus those listed here (as a comma-separated list) [string]
  id: No description is provided for this parameter [string]
  Schema: /components/schemas/UserResource
  Schema: /components/schemas/UserResource
Operation ID: delete-user-by-id
  Description: Deletes a user resource
  url-suffix: id
  Parameters:
  id: Identifier of the resource to delete [string]
Operation ID: patch-user-by-id
  Description: Updates one or more attributes of a User resource using a sequence of additions, removals, and 
replacements operations. See section 3.5.2 of RFC 7644

  url-suffix: id
  Parameters:
  attributes: A comma-separated list of attribute names to return in the response [string]
  excludedAttributes: When specified, the response will contain a default set of attributes minus those listed here (as a comma-separated list) [string]
  id: No description is provided for this parameter [string]
  Schema: /components/schemas/PatchRequest
  Schema: /components/schemas/PatchRequest
Operation ID: search-user
  Description: Query User resources (see section 3.4.2 of RFC 7644)
  Schema: /components/schemas/SearchRequest
  Schema: /components/schemas/SearchRequest

To get sample schema type /opt/jans/jans-cli/scim-cli.py --schema <schma>, for example /opt/jans/jans-cli/scim-cli.py 
```

  1. **_get-users_**: 
  
  This operation is used to get list of the users and its properties. The command line is: 
  
  ```/opt/jans/jans-cli/scim-cli.py --operation-id get-users```
  
  By default, This will return all of the users and their properties. 

  ```
  root@testjans:~# /opt/jans/jans-cli/scim-cli.py --operation-id get-users

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
        "lastModified": "2021-04-06T18:39:54.087Z",
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

```

It also supports parameters for the advanced search. Those parameters are:

    1. attributes
    2. excludeAttributes
    3. filter
    4. count [define maximum number of query]
    5. sortBy [attribute]
    6. sortOrder ['ascending', 'descending']

This is an example with `endpoint-args`:

```
/opt/jans/jans-cli/scim-cli.py --operation-id get-users --endpoint-args count:1
```

It returns as below:

```
Getting access token for scope https://jans.io/scim/users.read
Calling with params count=1
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
        "lastModified": "2021-04-06T18:39:54.087Z",
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

```

2. **_`create_user`_**: This operation can be performed to create user resources. 

```
Operation ID: create-user
  Description: Allows creating a User resource via POST (see section 3.3 of RFC 7644)
  Parameters:
  attributes: A comma-separated list of attribute names to return in the response [string]
  excludedAttributes: When specified, the response will contain a default set of attributes minus those listed here (as a comma-separated list) [string]
  Schema: /components/schemas/UserResource
```

As we see, to perform this operation we need to define the schema. So, let's get the schema of this operation.

```
/opt/jans/jans-cli/scim-cli.py --schema /components/schemas/UserResource > /tmp/create-user.json
```

```
root@testjans:~# cat /tmp/create-user.json


{
  "externalId": null,
  "userName": null,
  "name": {
    "familyName": null,
    "givenName": null,
    "middleName": null,
    "honorificPrefix": null,
    "honorificSuffix": null,
    "formatted": null
  },
  "displayName": null,
  "nickName": null,
  "profileUrl": null,
  "title": "Vice President",
  "userType": "Contractor",
  "preferredLanguage": "en",
  "locale": "en-US",
  "timezone": "America/Los_Angeles",
  "active": false,
  "password": null,
  "emails": {
    "value": "gossow@nsfw.com",
    "display": null,
    "type": "work",
    "primary": true
  },
  "phoneNumbers": {
    "value": "+1-555-555-8377",
    "display": null,
    "type": "fax",
    "primary": true
  },
  "ims": {
    "value": null,
    "display": null,
    "type": "gtalk",
    "primary": true
  },
  "photos": {
    "value": "https://pics.nsfw.com/gossow.png",
    "display": null,
    "type": "thumbnail",
    "primary": true
  },
  "addresses": {
    "formatted": null,
    "streetAddress": "56 Acacia Avenue",
    "locality": null,
    "region": null,
    "postalCode": null,
    "country": "UK",
    "type": "home",
    "primary": false
  },
  "groups": {
    "value": "180ee84f0671b1",
    "$ref": "https://nsfw.com/scim/restv1/v2/Groups/180ee84f0671b1",
    "display": "Cult managers",
    "type": "direct"
  },
  "entitlements": {
    "value": "Stakeholder",
    "display": null,
    "type": null,
    "primary": false
  },
  "roles": {
    "value": "Project manager",
    "display": null,
    "type": null,
    "primary": false
  },
  "x509Certificates": {
    "value": null,
    "display": null,
    "type": null,
    "primary": true
  },
  "urn:ietf:params:scim:schemas:extension:gluu:2.0:User": {},
  "schemas": [],
  "id": null,
  "meta": {
    "resourceType": null,
    "created": null,
    "lastModified": null,
    "location": null
  }
}
```
Now it's pretty simple. Fill each of this information, you may skip some of this properties as well. If you look at the schema, some of the properties are already filled with some random value. You can modify them as well or ignore them.

let's modify this schema:

```
nano /tmp/create-user.json
```

![](img/cl-scim-create-user.png)

Finally use below command line, to create an user resources.

```
/opt/jans/jans-cli/scim-cli.py --operation-id create-user --data /tmp/create-user.json

```

It will generate user `inum` value, metadata and will be added in user resources:

```
Getting access token for scope https://jans.io/scim/users.write
Server Response:
{
  "externalId": null,
  "userName": "mhosen",
  "name": {
    "familyName": "mobarak",
    "givenName": "mobarak",
    "middleName": null,
    "honorificPrefix": null,
    "honorificSuffix": null,
    "formatted": "mobarak mobarak"
  },
  "displayName": "mobarak",
  "nickName": null,
  "profileUrl": null,
  "title": "Vice President",
  "userType": "Contractor",
  "preferredLanguage": "en",
  "locale": "en-US",
  "timezone": "America/Los_Angeles",
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
  "id": "7034663f-dc43-4f8c-8074-e8e75cae9c96",
  "meta": {
    "resourceType": "User",
    "created": "2021-04-17T14:54:30.430Z",
    "lastModified": "2021-04-17T14:54:30.430Z",
    "location": "https://testjans.gluu.org/jans-scim/restv1/v2/Users/7034663f-dc43-4f8c-8074-e8e75cae9c96"
  }
}

root@testjans:~# 
```

### Group
  
Group resources are used to manage user resources easily with some operations. 
If you run the below command, then you will get a list of operations as below:

```
root@testjans:~# /opt/jans/jans-cli/scim-cli.py --info Group

Operation ID: get-groups
  Description: Query Group resources (see section 3.4.2 of RFC 7644)
  Parameters:
  attributes: A comma-separated list of attribute names to return in the response [string]
  excludedAttributes: When specified, the response will contain a default set of attributes minus those listed here (as a comma-separated list) [string]
  filter: An expression specifying the search criteria. See section 3.4.2.2 of RFC 7644 [string]
  startIndex: The 1-based index of the first query result [integer]
  count: Specifies the desired maximum number of query results per page [integer]
  sortBy: The attribute whose value will be used to order the returned responses [string]
  sortOrder: Order in which the sortBy param is applied. Allowed values are "ascending" and "descending" [string]
Operation ID: create-group
  Description: Allows creating a Group resource via POST (see section 3.3 of RFC 7644)
  Parameters:
  attributes: A comma-separated list of attribute names to return in the response [string]
  excludedAttributes: When specified, the response will contain a default set of attributes minus those listed here (as a comma-separated list) [string]
  Schema: /components/schemas/GroupResource
  Schema: /components/schemas/GroupResource
Operation ID: get-group-by-id
  Description: Retrieves a Group resource by Id (see section 3.4.1 of RFC 7644)
  url-suffix: id
  Parameters:
  attributes: A comma-separated list of attribute names to return in the response [string]
  excludedAttributes: When specified, the response will contain a default set of attributes minus those listed here (as a comma-separated list) [string]
  id: No description is provided for this parameter [string]
Operation ID: update-group-by-id
  Description: Updates a Group resource (see section 3.5.1 of RFC 7644). Update works in a replacement fashion&amp;#58; every
attribute value found in the payload sent will replace the one in the existing resource representation. Attributes 
not passed in the payload will be left intact.

  url-suffix: id
  Parameters:
  attributes: A comma-separated list of attribute names to return in the response [string]
  excludedAttributes: When specified, the response will contain a default set of attributes minus those listed here (as a comma-separated list) [string]
  id: No description is provided for this parameter [string]
  Schema: /components/schemas/GroupResource
  Schema: /components/schemas/GroupResource
Operation ID: delete-group-by-id
  Description: Deletes a group resource (see section 3.6 of RFC 7644)
  url-suffix: id
  Parameters:
  id: Identifier of the resource to delete [string]
Operation ID: patch-group-by-id
  Description: Updates one or more attributes of a Group resource using a sequence of additions, removals, and 
replacements operations. See section 3.5.2 of RFC 7644

  url-suffix: id
  Parameters:
  attributes: A comma-separated list of attribute names to return in the response [string]
  excludedAttributes: When specified, the response will contain a default set of attributes minus those listed here (as a comma-separated list) [string]
  id: No description is provided for this parameter [string]
  Schema: /components/schemas/PatchRequest
  Schema: /components/schemas/PatchRequest
Operation ID: search-group
  Description: Query Group resources (see section 3.4.2 of RFC 7644)
  Schema: /components/schemas/SearchRequest
  Schema: /components/schemas/SearchRequest

To get sample schema type /opt/jans/jans-cli/scim-cli.py --schema <schma>, for example /opt/jans/jans-cli/scim-cli.py --schema /components/schemas/SearchRequest

root@testjans:~# 
```

  1. **_get_groups_**: This operation can be used to get list of groups that are used to organize user resources. Let's run the below command:

  ```
  # /opt/jans/jans-cli/scim-cli.py --operation-id get-users
  ```

  It will show the list of groups with all the members linked with each of the group. You can filter for the advanced search with some of its properties:

    1. attributes
    2. excludeAttributes
    3. filter
    4. count [define maximum number of query]
    5. sortBy [attribute]
    6. sortOrder ['ascending', 'descending']


  ```

  Getting access token for scope https://jans.io/scim/groups.read
  {
    "Resources": [
      {
        "displayName": "Jannsen Manager Group",
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
        "id": "60B7",
        "meta": {
          "resourceType": "Group",
          "created": null,
          "lastModified": null,
          "location": "https://testjans.gluu.org/jans-scim/restv1/v2/Groups/60B7"
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

  root@testjans:~# 

  ```

  2. **_create-group_**: This operation can be used to create group  resources to management user resources. 

  ```
  Operation ID: create-group
  Description: Allows creating a Group resource via POST (see section 3.3 of RFC 7644)
  Parameters:
  attributes: A comma-separated list of attribute names to return in the response [string]
  excludedAttributes: When specified, the response will contain a default set of attributes minus those listed here (as a comma-separated list) [string]
  Schema: /components/schemas/GroupResource

  ```

  So, let's get first the schema, to get schema:

  ```
  /opt/jans/jans-cli/scim-cli.py --schema /components/schemas/GroupResource
  ```

  ```
  root@testjans:~# /opt/jans/jans-cli/scim-cli.py --schema /components/schemas/GroupResource


  {
    "displayName": null,
    "members": {
      "$ref": null,
      "type": null,
      "display": null,
      "value": null
    },
    "schemas": [],
    "id": null,
    "meta": {
      "resourceType": null,
      "created": null,
      "lastModified": null,
      "location": null
    }
  }
  ```
  The schema defines the properties of an attribute.Just Create a json file with each properties in schema filling by a value.

  ![](img/cl-scim-create-group.png)

  As you see, I have removed `members` from the schema. We will add members in this group later. Let's use the command to create the group with this data.

  ```
  /opt/jans/jans-cli/scim-cli.py --operation-id create-group --data group.json
  ```

  ```
  Getting access token for scope https://jans.io/scim/groups.write
  Server Response:
  {
    "displayName": "Jannsen Test Group",
    "members": null,
    "schemas": [
      "urn:ietf:params:scim:schemas:core:2.0:Group"
    ],
    "id": "766ffd8c-88a8-4aa8-a430-a5b3ae809c21",
    "meta": {
      "resourceType": "Group",
      "created": "2021-04-14T19:54:03.091Z",
      "lastModified": "2021-04-14T19:54:03.091Z",
      "location": "https://testjans.gluu.org/jans-scim/restv1/v2/Groups/766ffd8c-88a8-4aa8-a430-a5b3ae809c21"
    }
  }
  ```

  To verify let's use the `get-groups` operation id:

  ```
  root@testjans:~# /opt/jans/jans-cli/scim-cli.py --operation-id get-groups

  Getting access token for scope https://jans.io/scim/groups.read
  {
    "Resources": [
      {
        "displayName": "Jannsen Manager Group",
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
        "id": "60B7",
        "meta": {
          "resourceType": "Group",
          "created": null,
          "lastModified": null,
          "location": "https://testjans.gluu.org/jans-scim/restv1/v2/Groups/60B7"
        }
      },
      {
        "displayName": "Jannsen Test Group",
        "members": [],
        "schemas": [
          "urn:ietf:params:scim:schemas:core:2.0:Group"
        ],
        "id": "766ffd8c-88a8-4aa8-a430-a5b3ae809c21",
        "meta": {
          "resourceType": "Group",
          "created": "2021-04-14T19:54:03.091Z",
          "lastModified": "2021-04-14T19:54:03.091Z",
          "location": "https://testjans.gluu.org/jans-scim/restv1/v2/Groups/766ffd8c-88a8-4aa8-a430-a5b3ae809c21"
        }
      }
    ],
    "schemas": [
      "urn:ietf:params:scim:api:messages:2.0:ListResponse"
    ],
    "totalResults": 2,
    "startIndex": 1,
    "itemsPerPage": 2
  }

  root@testjans:~# 
  ```
  
  3. **_get-group-by-id_**: If you have an id of a group resource, Then you can view its properties through this operation.

  ```
  Operation ID: get-group-by-id
  Description: Retrieves a Group resource by Id (see section 3.4.1 of RFC 7644)
  url-suffix: id
  Parameters:
  attributes: A comma-separated list of attribute names to return in the response [string]
  excludedAttributes: When specified, the response will contain a default set of attributes minus those listed here (as a comma-separated list) [string]
  id: No description is provided for this parameter [string]
  ```
  In our case, id:766ffd8c-88a8-4aa8-a430-a5b3ae809c21 that's created while performing the `create-group` operation.

  ```
  /opt/jans/jans-cli/scim-cli.py --operation-id get-group-by-id --url-suffix id:766ffd8c-88a8-4aa8-a430-a5b3ae809c21
  ```

  In return, you must see the group resources linked with the id:

  ```
    Getting access token for scope https://jans.io/scim/groups.read
  {
    "displayName": "Jannsen Test Group",
    "members": [],
    "schemas": [
      "urn:ietf:params:scim:schemas:core:2.0:Group"
    ],
    "id": "766ffd8c-88a8-4aa8-a430-a5b3ae809c21",
    "meta": {
      "resourceType": "Group",
      "created": "2021-04-14T19:54:03.091Z",
      "lastModified": "2021-04-14T19:54:03.091Z",
      "location": "https://testjans.gluu.org/jans-scim/restv1/v2/Groups/766ffd8c-88a8-4aa8-a430-a5b3ae809c21"
    }
  }
  ```
  4. **_update-group-by-id_**:

  If we look at the description, We see this op-mode needs `url-suffix` as `id` and `schema` definition for data. 

  ```
    Operation ID: update-group-by-id
    Description: Updates a Group resource (see section 3.5.1 of RFC 7644). 
    Update works in a replacement fashion&amp;#58; every attribute 
    value found in the payload sent will replace the one in
    the existing resource representation. Attributes not passed in the
    payload will be left intact.

    url-suffix: id
    Parameters:
    attributes: A comma-separated list of attribute names to return in the response [string]
    excludedAttributes: When specified, the response will contain a default set of attributes minus those listed here (as a comma-separated list) [string]
    id: No description is provided for this parameter [string]
    Schema: /components/schemas/GroupResource
  ```

  Let's get the schema first:

  ```
  /opt/jans/jans-cli/scim-cli.py --schema /components/schemas/GroupResource > /tmp/group.json
  ```

  let's modify this schema to add members into a selected group:

  ![](img/cl-scim-group-update.png)

  - displayName: It should be a group name
  - id: Selected group id which one you want to updae
  - meta: Meta data of the selected group

  Now let's add this member into the group we are going to update.

  ```
  root@testjans:~# /opt/jans/jans-cli/scim-cli.py --operation-id update-group-by-id --data /tmp/group.json --url-suffix id:56030854-2784-408e-8fa7-e11835804ac7

Getting access token for scope https://jans.io/scim/groups.write
Server Response:
{
  "displayName": "Test Janssen Server",
  "members": [
    {
      "$ref": "https://testjans.gluu.com/jans-scim/restv1/v2/Users/null",
      "type": "User",
      "display": null,
      "value": null
    }
  ],
  "schemas": [
    "urn:ietf:params:scim:schemas:core:2.0:Group"
  ],
  "id": "56030854-2784-408e-8fa7-e11835804ac7",
  "meta": {
    "resourceType": "Group",
    "created": "2021-04-19T22:15:15.151Z",
    "lastModified": "2021-04-21T16:22:24.085Z",
    "location": "https://testjans.gluu.com/jans-scim/restv1/v2/Groups/56030854-2784-408e-8fa7-e11835804ac7"
  }
}

root@testjans:~# 
  ```

That's hwo we can update a group using this operation method.
Please remember one thing, this update method just replace the data. If you want to add members instead of replacing then you must try `patch-group-by-id`.

5. **_delete-group-by-id_**

You can delete a group by its ID. The command line looks like:
```
/opt/jans/jans-cli/scim-cli.py --operation-id delete-group-by-id --url-suffix id:56030854-2784-408e-8fa7-e11835804ac7
```
It will delete the group and all of its data matched with the unique ID.

```
root@testjans:~# /opt/jans/jans-cli/scim-cli.py --operation-id delete-group-by-id --url-suffix id:56030854-2784-408e-8fa7-e11835804ac7

Getting access token for scope https://jans.io/scim/groups.write
```
