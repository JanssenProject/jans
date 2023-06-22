---
tags:
  - administration
  - configuration
  - cli
  - commandline
---

# Janssen CLI

`jans-cli` module is a command line interface for configuring the Janssen software, providing both interactive and simple single line options for configuration. This module interacts with Janssen server via RESTful configuration APIs that server exposes. Using these REST APIs, `jans-cli` allows configuration of authorization server and its major modules for FIDO2, SCIM, OpenID Connect etc. 
</br>
</br>

<p align="center">
 <img src="../../../assets/image-using-jans-cli-comp-04222022.png">
</p>

Using this mode is more difficult than the TUI. If needed, help options are available by typing:  

```
/opt/jans/jans-cli/config-cli.py --help
```

It will print all information about how to configure Janssen Server using CLI mode:

```
usage: config-cli.py [-h] [--host HOST] [--client-id CLIENT_ID]
                     [--client_secret CLIENT_SECRET] [--plugins PLUGINS] [-debug]
                     [--debug-log-file DEBUG_LOG_FILE]
                     [--operation-id OPERATION_ID] [--url-suffix URL_SUFFIX]
                     [--info {Attribute,CacheConfiguration,CacheConfigurationInMemory,CacheConfigurationMemcached,CacheConfigurationNativePersistence,CacheConfigurationRedis,ConfigurationFido2,ConfigurationJWKJSONWebKeyJWK,ConfigurationLogging,ConfigurationProperties,ConfigurationSMTP,CustomScripts,DatabaseCouchbaseConfiguration,DatabaseLDAPConfiguration,DefaultAuthenticationMethod,OAuthOpenIDConnectClients,OAuthScopes,OAuthUMAResources}]
                     [--op-mode {get,post,put,patch,delete}]
                     [--endpoint-args ENDPOINT_ARGS] [--schema SCHEMA]
                     [--username USERNAME] [--password PASSWORD] [-j J]
                     [--cert-file CERT_FILE] [--key-file KEY_FILE] [-noverify]
                     [--patch-add PATCH_ADD] [--patch-replace PATCH_REPLACE]
                     [--patch-remove PATCH_REMOVE] [--data DATA]

optional arguments:
  -h, --help            show this help message and exit
  --host HOST           Hostname of server
  --client-id CLIENT_ID
                        Jans Config Api Client ID
  --client_secret CLIENT_SECRET
                        Jans Config Api Client ID secret
  --plugins PLUGINS     Available plugins separated by comma
  -debug                Run in debug mode
  --debug-log-file DEBUG_LOG_FILE
                        Log file name when run in debug mode
  --operation-id OPERATION_ID
                        Operation ID to be done
  --url-suffix URL_SUFFIX
                        Argument to be added api endpoint url. For example
                        inum:2B29
  --info {Attribute,CacheConfiguration,CacheConfigurationInMemory,CacheConfigurationMemcached,CacheConfigurationNativePersistence,CacheConfigurationRedis,ConfigurationFido2,ConfigurationJWKJSONWebKeyJWK,ConfigurationLogging,ConfigurationProperties,ConfigurationSMTP,CustomScripts,DatabaseCouchbaseConfiguration,DatabaseLDAPConfiguration,DefaultAuthenticationMethod,OAuthOpenIDConnectClients,OAuthScopes,OAuthUMAResources}
                        Help for operation
  --op-mode {get,post,put,patch,delete}
                        Operation mode to be done
  --endpoint-args ENDPOINT_ARGS
                        Arguments to pass endpoint separated by comma. For
                        example limit:5,status:INACTIVE
  --schema SCHEMA       Get sample json schema
  --username USERNAME   Auth username
  --password PASSWORD   Auth password
  -j J                  Auth password file
  --cert-file CERT_FILE
                        Path to SSL Certificate file
  --key-file KEY_FILE   Path to SSL Key file
  -noverify             Ignore verifying the SSL certificate
  --patch-add PATCH_ADD
                        Colon delimited key:value pair for add patch
                        operation. For example loggingLevel:DEBUG
  --patch-replace PATCH_REPLACE
                        Colon delimited key:value pair for replace patch
                        operation. For example loggingLevel:DEBUG
  --patch-remove PATCH_REMOVE
                        Key for remove patch operation. For example
                        imgLocation
  --data DATA           Path to json data file
  --output-access-token Prints jwt access token and exits
root@testjans:~# 
```
We start with getting information about tasks, tasks are options of argument `--info`:

```
Attribute, CacheConfiguration, CacheConfigurationInMemory, CacheConfigurationMemcached, CacheConfigurationNativePersistence, CacheConfigurationRedis, ConfigurationFido2, ConfigurationJWKJSONWebKeyJWK, ConfigurationLogging, ConfigurationProperties, ConfigurationSMTP, CustomScripts, DatabaseCouchbaseConfiguration, DatabaseLDAPConfiguration, DefaultAuthenticationMethod, OAuthOpenIDConnectClients, OAuthOpenIDConnectSectorIdentifiers, OAuthScopes, OAuthUMAResources
```

To get information for a specific task we run command as below: 
```
/opt/jans/jans-cli/config-cli.py --info [task]
``` 
for example: 
```
/opt/jans/jans-cli/config-cli.py --info DefaultAuthenticationMethod
``` 

It returns with some `operation id`:
```
Operation ID: get-acrs
  Description: Gets default authentication method.
Operation ID: put-acrs
  Description: Updates default authentication method.
  Schema: /components/schemas/AuthenticationMethod

To get sample shema type /opt/jans/jans-cli/config-cli.py --schema <schma>, for example /opt/jans/jans-cli/config-cli.py --schema /components/schemas/AuthenticationMethod
```
To perform any operation, you have to run command line with the operation id. for example:

```
/opt/jans/jans-cli/config-cli.py --operation-id get-acrs
```
It returns:

```text
Getting access token for scope https://jans.io/oauth/config/acrs.readonly
{
  "defaultAcr": "simple_password_auth"
}
```

This is how we can execute single line command to get information about the Janssen Server. 

## Operation Modes
Default operation mode is **jca**, i.e., all queries are done to Jans Config Api Server. CLI has also SCIM and OAUTH modes:

### SCIM Mode
To switch SCIM mode user `-scim` switch. For example to get help for SCIM mode:

`/opt/jans/jans-cli/config-cli.py -scim -h`

Only `--info` arguments will be different from **jca** mode:

```
--info {Discovery,Fido2Devices,FidoDevices,GlobalSearch,Group,User}
```

You can get information about operation `Discovery`:

```
/opt/jans/jans-cli/config-cli.py -scim --info Fido2Devices
Operation ID: get-fido2-devices
  Description: Query Fido 2 resources
  Parameters:
  attributes: A comma-separated list of attribute names to return in the response [string]
  excludedAttributes: When specified, the response will contain a default set of attributes minus those listed here (as a comma-separated list) [string]
  userId: Used to restrict the search to fido 2 resources owned by a specific user [string]
  filter: An expression specifying the search criteria. See section 3.4.2.2 of RFC 7644 [string]
  startIndex: The 1-based index of the first query result [integer]
  count: Specifies the desired maximum number of query results per page [integer]
  sortBy: The attribute whose value will be used to order the returned responses [string]
  sortOrder: Order in which the sortBy param is applied. Allowed values are "ascending" and "descending" [string]
Operation ID: get-fido2-device-by-id
  Description: Retrieves a Fido 2 device by Id
  Parameters:
  attributes: A comma-separated list of attribute names to return in the response [string]
  excludedAttributes: When specified, the response will contain a default set of attributes minus those listed here (as a comma-separated list) [string]
  userId: Identifier (inum) of the device owner. This param is not required when underlying database is LDAP [string]
  id: No description is provided for this parameter [string]
Operation ID: update-fido2-device-by-id
  Description: Updates a Fido 2 resource. Update works in a replacement fashion&amp;#58; every attribute value found in the payload sent will replace the one in the existing resource representation. Attributes not passed in the payload will be left intact.

  Parameters:
  attributes: A comma-separated list of attribute names to return in the response [string]
  excludedAttributes: When specified, the response will contain a default set of attributes minus those listed here (as a comma-separated list) [string]
  id: No description is provided for this parameter [string]
  Schema: Fido2DeviceResource
  Schema: Fido2DeviceResource
Operation ID: delete-fido2-device-by-id
  Description: Deletes a Fido 2 resource
  Parameters:
  id: Identifier of the resource to delete [string]
Operation ID: search-fido2-device
  Description: Query Fido 2 resources
  Parameters:
  userId: Used to restrict the search to fido 2 resources owned by a specific user [string]
  Schema: SearchRequest
  Schema: SearchRequest

To get sample schema type /opt/jans/jans-cli/config-cli.py -scim --schema <schma>, for example /opt/jans/jans-cli/config-cli.py -scim --schema SearchRequest
```

Here is how you can get sample schema for **SearchRequest**:

```
/opt/jans/jans-cli/config-cli.py -scim --schema SearchRequest
{
  "schemas": [
    "string"
  ],
  "attributes": [
    "string"
  ],
  "excludedAttributes": [
    "string"
  ],
  "filter": "userName eq \"jhon\" and meta.lastModified gt \"2011-05-13T04:42:34Z\"",
  "sortBy": "string",
  "sortOrder": "string",
  "startIndex": 143,
  "count": 58
}
```

### AUTH Mode
To switch AUTH mode user `-auth` switch. For example to get help for AUTH mode:

`/opt/jans/jans-cli/config-cli.py -auth -h`

`--info` arguments will be:

```
--info {Authorization,ClientInfo,ClientRegistration,FidoU2F,JwkJsonWebKeySetJwks,Registration,ServerConfiguration,SessionManagement,Ssa,Token,TokenIntrospection,Uma2Resource,UmaScope,UmaUserManagedAccess,UserInfo}
```

To get information about operation `ClientRegistration`:

```
/opt/jans/jans-cli/config-cli.py -auth --info ClientInfo
Operation ID: get_clientinfo
  Description: The ClientInfo Endpoint is an OAuth 2.0 Protected Resource that returns Claims about the registered client.
  Parameters:
  access_token: No description is provided for this parameter [string]
  Authorization: No description is provided for this parameter [string]
Operation ID: post_clientinfo
  Description: The ClientInfo Endpoint is an OAuth 2.0 Protected Resource that returns Claims about the registered client.
  Parameters:
  Authorization: No description is provided for this parameter [string]
  Parameters:
    access_token*: Client-specific access token.
```


## Basic command-line switches

1. `-h` or `--help` to get all the formations of command line argument (ex; `/opt/jans/jans-cli/config-cli.py -h`)
2. `--info` to get formations about some operations id for a specific task (ex; `opt/jans/jans-cli/config-cli.py --info User`)
3. `--operation-id` usage to operate each of the sub-task
4. `--endpoint-args` advanced usage for operation-id
5. `--data` usage to share data in operations


## Patch Request (schema)

This schema file can be found in `/components/schemas/PatchRequest` for those which one support this operation.

When you examine this sample schema, you will see three properties in an object: op, path, and value.

* __op__: operation to be done, one of `add`, `remove`, `replace`, `move`, `copy`, `test`
* __path__: Path of the property to be changed. use path separator `/` for config or `.` for SCIM to change a property inside an object.
* __value__: New value to be assigned for each property defined in `path`

## Multiple Patch Request (schema)

When we need to perform multiple patch operations on any configuration endpoint, Instead of doing one by one, we can create a json file including all individual operation into an array. To clarify, please see below json file:


```
[
    {
        "op": "operation-name",
        "path": "configuration-path",
        "value": "Value"
    },
    {
        "op": "operation-name",
        "path": "configuration-path",
        "value": "value"
    },
    {
        "op": "operation-name",
        "path": "configuration-path",
        "value": "value"
    }
    ...
    ...
    ...
    {
        "op": "operation-name",
        "path": "configuration-path",
        "value": "value"
    }
]
```

This file contains multiple individual patch operation. In [Patch Request (schema)](README.md#patch-request-schema) we explained about each of these keys in the above json file.

After creating the json file, just run the patch operation command.

```
/opt/jans/jans-cli/config-cli.py --operation-id [patch operation id name] --data [json file absolute url]
```

## Quick Patch Operations

There is another patch request feature. It is a single line patch-request command line. It supports three types of operations:

- `patch-replace`: to replace value with new one.
- `patch-add`: it will add value into the key path.
- `patch-remove`: to remove value from any key path.

The command line looks like below:

```
/opt/jans/jans-cli/config-cli.py --operation-id [patch-operation-id] --[patch-operation-name] key:value
```

for example:

```
/opt/jans/jans-cli/config-cli.py --operation-id patch-config-cache --patch-replace memcachedConfiguration/bufferSize:32788
```

In this command line:
- `patch-config-cache` is a operation-id from *Cache Configurations* task.
- `patch-replace` type of operation; used to replace values in
- `memcachedConfiguration/bufferSize:32788` is a `key:value` pair

Multi valued arguments can be privede as `key:value1,key:vlaue2`, for example 
```
/opt/jans/jans-cli/config-cli.py --operation-id=get-config-scripts  --endpoint-args="fieldValuePair:scriptType=dynamic_scope,fieldValuePair:level=100"
```
## CLI Authorization
To run operations on Janssen Server, CLI client will need to be authenticated and authorized by the server. Since CLI has limited input capabilities, it uses [Device Authorization Grant](https://datatracker.ietf.org/doc/html/rfc8628) flow to get required permissions in form of an access token. After successfully receiving the token, CLI can run operations on the Janssen server while the token is valid. The steps below will summarize this process.

1. Execution of CLI command will return the following message if a valid token is not found.
   ```
   Access token was not found.
   Please visit verification url <Janssen-server-device-code-url> and enter user code CGFZ-RTZR in 1800 seconds
   Please press <<Enter>> when ready
   ```
2. Take `<Janssen-server-device-code-url>` from the message above and use any browser to access it from a different device
3. User will be presented with a page where the user has to authenticate using id and password
4. After successful user authentication, the next screen allows the user to enter the user code. Use the user code presented on command-line instruction in step 1 above.
5. After successful code validation, the user is presented with OAuth permissions screen. This screen would list all the permissions requested by Jans CLI. The user can choose to `Allow` or `Not Allow` granting of these permissions.
6. After allowing the grant of requested permissions, the user should come back to the command-line interface and hit <<Enter>> as instructed. This will enable CLI to run operations on the corresponding Janssen server.
