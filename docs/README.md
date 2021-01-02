# Using jans-cli
`jans-cli` is automatically installed if you choose to install **jans-config-api** during installation of Janssen Server. `jans-cli` has two modes of operation
1. Menu-driven Interactive Mode
1. Command Line Argument Mode

## Menu-driven Interactive Mode
We will refer _Menu-driven Interactive Mode_ as **IM**. To run IM just simply execute jans-cli as follows:
```
/opt/jans/jans-cli/jca-cli.py
```
You will see main menu as

![jans-cli Main Menu](img/im-main.png)

Using IM is ver simple and intuitive. Just make selection and answer questions. For example to get all attributes chose 1, you will get the following options:

![jans-cli Attributes Menu](img/im-attributes-main.png)

Then chose 1, you will be asked Search size,  Search pattern and Status of the attribute. For simpleicity leave deafults in our case:

![jans-cli Attributes Get All](img/im-attributes-get-all.png)

Once press Enter, it will retreive 50 attrbiutes and prints to screen in green color:

![jans-cli Attributes Get All Response](img/im-attributes-get-all-response.png)

You can save result to file by chosing `w` in result screen. To go back enter `b`, to quit enter `q`. If you enter an reconised command, it will display valid commands.

To update an attrbiue, choose 3 (on Attribute screen). It will ask `inum` of attrbiute you are going to update. I want to change desctiption for attribute having inum **BCA8**. 
It will retreive current data and will ask for modifation of each properties, just leave defaults if you don't want to change that property.

![jans-cli Attributes Update](img/im-attributes-update.png)

When it comes to end, it will display modified data and ask if you want to continue (this data just before it sends to server)

![jans-cli Attributes Update - Continue?](img/im-attributes-update-continue.png) 

After you enter `y` it will perform update and display updated data on screen.

## Command Line Argument Mode
We will refer this mode as **CL** Using this mode is difficult compared to IM. First is get get help, so type:
```
/opt/jans/jans-cli/jca-cli.py --help
```

This will print how to use CL:

```
usage: jca-cli.py [-h] [--host HOST] [--client-id CLIENT_ID]
                  [--client_secret CLIENT_SECRET] [-debug]
                  [--debug-log-file DEBUG_LOG_FILE]
                  [--operation-id OPERATION_ID] [--url-suffix URL_SUFFIX]
                  [--info {Attribute,CacheConfiguration,CacheConfigurationInMemory,CacheConfigurationMemcached,CacheConfigurationNativePersistence,CacheConfigurationRedis,ConfigurationFido2,ConfigurationJWKJSONWebKeyJWK,ConfigurationLogging,ConfigurationProperties,ConfigurationSMTP,CustomScripts,DatabaseCouchbaseConfiguration,DatabaseLDAPConfiguration,DefaultAuthenticationMethod,OAuthOpenIDConnectClients,OAuthOpenIDConnectSectorIdentifiers,OAuthScopes,OAuthUMAResources}]
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
  --info {Attribute,CacheConfiguration,CacheConfigurationInMemory,CacheConfigurationMemcached,CacheConfigurationNativePersistence,CacheConfigurationRedis,ConfigurationFido2,ConfigurationJWKJSONWebKeyJWK,ConfigurationLogging,ConfigurationProperties,ConfigurationSMTP,CustomScripts,DatabaseCouchbaseConfiguration,DatabaseLDAPConfiguration,DefaultAuthenticationMethod,OAuthOpenIDConnectClients,OAuthOpenIDConnectSectorIdentifiers,OAuthScopes,OAuthUMAResources}
                        Help for operation
  --op-mode {get,post,put,patch,delete}
                        Operation mode to be done
  --endpoint-args ENDPOINT_ARGS
                        Arguments to pass endpoint seperated by comma. For
                        example limit:5,status:INACTIVE
  --schema SCHEMA       Get sample json schema
  --data DATA           Path to json data file
```
We start with getting infromtation about tasks, the tasks are options of argument `--info`:

Attribute, CacheConfiguration, CacheConfigurationInMemory, CacheConfigurationMemcached, CacheConfigurationNativePersistence, CacheConfigurationRedis, ConfigurationFido2, ConfigurationJWKJSONWebKeyJWK, ConfigurationLogging, ConfigurationProperties, ConfigurationSMTP, CustomScripts, DatabaseCouchbaseConfiguration, DatabaseLDAPConfiguration, DefaultAuthenticationMethod, OAuthOpenIDConnectClients, OAuthOpenIDConnectSectorIdentifiers, OAuthScopes, OAuthUMAResources

Let's get information for task **OAuthScopes**

```
 /opt/jans/jans-cli/jca-cli.py --info OAuthScopes

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

To get sample shema type /opt/jans/jans-cli/jca-cli.py --schema <schma>, for example /opt/jans/jans-cli/jca-cli.py --schema /components/schemas/PatchRequest
```
Operations will be done with **Operation ID**. Some operations may take parameters, let's retreive `3` scopes (**limit**) that has `view` in it's description (**pattern**) and **type** `openid`:

```
 /opt/jans/jans-cli/jca-cli.py --operation-id get-oauth-scopes --endpoint-args limit:3,pattern:view,type:openid
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

Let's create a scope, for this we need a sample schema, you can use of the obove result, or can get sample shcema. Remember when we queried info for task **OAuthScopes** it printed:

```
Operation ID: post-oauth-scopes
  Description: Create Scope.
  Schema: /components/schemas/Scope
```
Thus, we can get sample schema and use Operation ID `post-oauth-scopes`. Lets get sample schema:

```
/opt/jans/jans-cli/jca-cli.py --schema /components/schemas/Scope > /tmp/scope.json
```

Now edit file `tmp/scope.json` with an editor (like **nano**). As as example we just filled the following properties:

```
"id": "TestScopeID",
"displayName": "TestScope",
"description": "Test Scope created by jans-cli",
```

![jans-cl Edit scope.json](img/cl-oauthscope-json.png) 

It is time to post the data:

```
 /opt/jans/jans-cli/jca-cli.py --operation-id post-oauth-scopes --data /tmp/scope.json 

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

It created scope with inum `112116fd-257b-40d8-a2c9-0c23536680ed` and returned current data. Let's update `iconUrl` with patch method. So we need a schema for patch method. Remember when we queried info for task **OAuthScopes** it printed:

```
Operation ID: patch-oauth-scopes-by-id
  Description: Update modified attributes of existing Scope by Inum.
  url-suffix: inum
  Schema: Array of /components/schemas/PatchRequest
 ```
 
This means we need schema `/components/schemas/PatchRequest`, be careful it states **Array of**, so we will make an array of this schema, in case you need multiple changes with patch method, you can put as many as of this schema into array. Get schema:

```
/opt/jans/jans-cli/jca-cli.py --schema /components/schemas/PatchRequest > /tmp/patch.json
```

When you examine this json, you will see three properties in an object: op, path, and value. Meanings of these properties are as follows:
__op__ operation to be done, one of `add`, `remove`, `replace`, `move`, `copy`, `test`
__path__ Path of property to be changed. use path seperator `/` to change a property inside object. For example to change **spontaneousClientId** you can use `attributes/spontaneousClientId`
__value__ New value to be assigned for property defined in `path`

We can edit this json as follows (remember to make it an array):

![jans-cl Edit patch.json](img/cl-oauthscope-patch-json.png)

Let's do the operation:

```
/opt/jans/jans-cli/jca-cli.py --operation-id patch-oauth-scopes-by-id --url-suffix inum:112116fd-257b-40d8-a2c9-0c23536680ed --data /tmp/patch.json 

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


