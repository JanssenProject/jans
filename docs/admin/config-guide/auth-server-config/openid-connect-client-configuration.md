---
tags:
  - administration
  - configuration
  - openid-connect
---

# OpenID Connect Configuration


The Janssen Server provides multiple configuration tools to perform these
tasks.

=== "Use Command-line"

    Use the command line to perform actions from the terminal. Learn how to 
    use Jans CLI [here](../config-tools/jans-cli/README.md) or jump straight to 
    the [Using Command Line](#using-command-line)

=== "Use Text-based UI"

    Use a fully functional text-based user interface from the terminal. 
    Learn how to use Jans Text-based UI (TUI) 
    [here](../config-tools/jans-tui/README.md) or jump straight to the
    [Using Text-based UI](#using-text-based-ui)

=== "Use REST API"

    Use REST API for programmatic access or invoke via tools like CURL or 
    Postman. Learn how to use Janssen Server Config API 
    [here](../config-tools/config-api/README.md) or Jump straight to the
    [Using Configuration REST API](#using-configuration-rest-api)



## Using Command Line

In the Janssen Server, you can deploy and manage the OpenID Connect Client using the
command Line. To get the details of Janssen command line operations relevant to
OpenID Connect Client, you can check the operations under `OauthOpenidConnectClients` 
task using the command below:

Let's get the information of OpenID Connect Client Configuration:
```bash title="Command"
/opt/jans/jans-cli/config-cli.py --info OauthOpenidConnectClients
```
```text title="Sample Output" linenums="1"
Operation ID: get-oauth-openid-clients
  Description: Gets list of OpenID Connect clients
  Parameters:
  limit: Search size - max size of the results to return [integer]
  pattern: Search pattern [string]
  startIndex: The 1-based index of the first query result [integer]
  sortBy: Attribute whose value will be used to order the returned response [string]
  sortOrder: Order in which the sortBy param is applied. Allowed values are "ascending" and "descending" [string]
  fieldValuePair: Field and value pair for seraching [string]
Operation ID: put-oauth-openid-client
  Description: Update OpenId Connect client
  Schema: Client
Operation ID: post-oauth-openid-client
  Description: Create new OpenId Connect client
  Schema: Client
Operation ID: get-oauth-openid-clients-by-inum
  Description: Get OpenId Connect Client by Inum
  Parameters:
  inum: Client identifier [string]
Operation ID: delete-oauth-openid-client-by-inum
  Description: Delete OpenId Connect client
  Parameters:
  inum: Client identifier [string]
Operation ID: patch-oauth-openid-client-by-inum
  Description: Patch OpenId Connect client
  Parameters:
  inum: Client identifier [string]
  Schema: Array of JsonPatch

To get sample schema type /opt/jans/jans-cli/config-cli.py --schema-sample <schema>, for example /opt/jans/jans-cli/config-cli.py --schema-sample JsonPatch
```

### Get List of OpenID Clients

To get the openid clients, run the following command:
We can get list of all configurations of the openid clients using a command like this:

```bash title="Command"
/opt/jans/jans-cli/config-cli.py --operation-id get-oauth-openid-clients
```

```json title="Sample Output" linenums="1"
{
  "start": 0,
  "totalEntriesCount": 3,
  "entriesCount": 3,
  "entries": [
    {
      "dn": "inum=1201.c2b59c1a-d589-4a79-afed-b5c4e7281337,ou=clients,o=jans",
      "clientSecret": "lshZdyFzDtT5",
      "frontChannelLogoutUri": "null",
      "frontChannelLogoutSessionRequired": false,
      "redirectUris": [
        "https://jans-project-2/.well-known/scim-configuration"
      ],
      "claimRedirectUris": [],
      "responseTypes": [],
      "grantTypes": [
        "client_credentials"
      ],
      "applicationType": "native",
      "contacts": [],
      "clientName": "SCIM client",
      "clientNameLocalized": {},
      "logoUriLocalized": {},
      "clientUriLocalized": {},
      "policyUriLocalized": {},
      "tosUriLocalized": {},
      "subjectType": "pairwise",
      "tokenEndpointAuthMethod": "client_secret_basic",
      "defaultAcrValues": [],
      "postLogoutRedirectUris": [],
      "requestUris": [],
      "scopes": [
        "inum=1200.635604,ou=scopes,o=jans",
        "inum=1200.4523C3,ou=scopes,o=jans",
        "inum=1200.6D0B45,ou=scopes,o=jans",
        "inum=1200.230E00,ou=scopes,o=jans",
        "inum=1200.C33949,ou=scopes,o=jans",
        "inum=1200.39C06B,ou=scopes,o=jans",
        "inum=1200.436D5C,ou=scopes,o=jans",
        "inum=1200.DAB2CB,ou=scopes,o=jans",
        "inum=1200.234471,ou=scopes,o=jans",
        "inum=1200.FC7244,ou=scopes,o=jans"
      ],
      "claims": [],
      "trustedClient": false,
      "persistClientAuthorizations": false,
      "includeClaimsInIdToken": false,
      "customAttributes": [
        {
          "name": "displayNameLocalized",
          "multiValued": true
        },
        {
          "name": "jansClntURILocalized",
          "multiValued": true
        },
        {
          "name": "jansLogoURILocalized",
          "multiValued": true
        },
        {
          "name": "jansPolicyURILocalized",
          "multiValued": true
        },
        {
          "name": "jansTosURILocalized",
          "multiValued": true
        }
      ],
      "rptAsJwt": false,
      "accessTokenAsJwt": false,
      "accessTokenSigningAlg": "RS256",
      "disabled": false,
      "authorizedOrigins": [],
      "attributes": {
        "runIntrospectionScriptBeforeJwtCreation": false,
        "keepClientAuthorizationAfterExpiration": false,
        "allowSpontaneousScopes": false,
        "backchannelLogoutSessionRequired": false,
        "parLifetime": 600,
        "requirePar": false,
        "dpopBoundAccessToken": false,
        "jansDefaultPromptLogin": false,
        "minimumAcrLevel": -1
      },
      "displayName": "SCIM client",
      "authenticationMethod": "client_secret_basic",
      "allAuthenticationMethods": [
        "client_secret_basic"
      ],
      "baseDn": "inum=1201.c2b59c1a-d589-4a79-afed-b5c4e7281337,ou=clients,o=jans",
      "inum": "1201.c2b59c1a-d589-4a79-afed-b5c4e7281337"
    },
    {
      "dn": "inum=1800.6f1348e5-8f95-41bc-b2bc-3aa567d75a61,ou=clients,o=jans",
      "deletable": false,
      "clientSecret": "mObwHJR119ov",
      "frontChannelLogoutUri": "null",
      "frontChannelLogoutSessionRequired": false,
      "redirectUris": [
        "https://jans-project-2/admin-ui",
        "http://localhost:4100"
      ],
      "claimRedirectUris": [],
      "responseTypes": [
        "code"
      ],
      "grantTypes": [
        "authorization_code",
        "refresh_token",
        "client_credentials"
      ],
      "applicationType": "web",
      "contacts": [],
      "clientName": "Jans Config Api Client",
      "clientNameLocalized": {},
      "logoUriLocalized": {},
      "clientUriLocalized": {},
      "policyUriLocalized": {},
      "tosUriLocalized": {},
      "subjectType": "pairwise",
      "idTokenSignedResponseAlg": "RS256",
      "tokenEndpointAuthMethod": "client_secret_basic",
      "defaultAcrValues": [],
      "postLogoutRedirectUris": [],
      "requestUris": [],
      "scopes": [
        "inum=1800.01.75,ou=scopes,o=jans",
        "inum=1800.01.76,ou=scopes,o=jans",
        "inum=1800.01.77,ou=scopes,o=jans",
        "inum=1800.01.71,ou=scopes,o=jans",
        "inum=1800.01.72,ou=scopes,o=jans",
        "inum=1800.01.73,ou=scopes,o=jans",
        "inum=1800.01.74,ou=scopes,o=jans",
        "inum=1800.01.067,ou=scopes,o=jans",
        "inum=1800.01.64,ou=scopes,o=jans",
        "inum=1800.03.1,ou=scopes,o=jans",
        "inum=1800.01.65,ou=scopes,o=jans",
        "inum=1800.01.66,ou=scopes,o=jans",
        "inum=1800.03.3,ou=scopes,o=jans",
        "inum=1800.01.67,ou=scopes,o=jans",
        "inum=1800.03.2,ou=scopes,o=jans",
        "inum=1800.01.60,ou=scopes,o=jans",
        "inum=1800.01.61,ou=scopes,o=jans",
        "inum=1800.01.62,ou=scopes,o=jans",
        "inum=1800.01.63,ou=scopes,o=jans",
        "inum=1800.01.68,ou=scopes,o=jans",
        "inum=1800.01.69,ou=scopes,o=jans",
        "inum=1800.01.70,ou=scopes,o=jans",
        "inum=1800.01.10,ou=scopes,o=jans",
        "inum=1800.01.11,ou=scopes,o=jans",
        "inum=1800.01.12,ou=scopes,o=jans",
        "inum=1800.01.17,ou=scopes,o=jans",
        "inum=1800.01.18,ou=scopes,o=jans",
        "inum=1800.01.19,ou=scopes,o=jans",
        "inum=1800.01.13,ou=scopes,o=jans",
        "inum=1800.01.14,ou=scopes,o=jans",
        "inum=1800.01.15,ou=scopes,o=jans",
        "inum=1800.01.16,ou=scopes,o=jans",
        "inum=1800.02.2,ou=scopes,o=jans",
        "inum=1800.02.1,ou=scopes,o=jans",
        "inum=1800.02.4,ou=scopes,o=jans",
        "inum=1800.02.3,ou=scopes,o=jans",
        "inum=1800.02.6,ou=scopes,o=jans",
        "inum=1800.02.5,ou=scopes,o=jans",
        "inum=1800.01.31,ou=scopes,o=jans",
        "inum=1800.01.32,ou=scopes,o=jans",
        "inum=1800.01.33,ou=scopes,o=jans",
        "inum=1800.01.34,ou=scopes,o=jans",
        "inum=1800.01.30,ou=scopes,o=jans",
        "inum=1800.01.39,ou=scopes,o=jans",
        "inum=1800.01.35,ou=scopes,o=jans",
        "inum=1800.01.36,ou=scopes,o=jans",
        "inum=1800.01.37,ou=scopes,o=jans",
        "inum=1800.01.38,ou=scopes,o=jans",
        "inum=1800.01.3,ou=scopes,o=jans",
        "inum=1800.01.20,ou=scopes,o=jans",
        "inum=1800.04.14,ou=scopes,o=jans",
        "inum=1800.01.21,ou=scopes,o=jans",
        "inum=1800.01.2,ou=scopes,o=jans",
        "inum=1800.01.22,ou=scopes,o=jans",
        "inum=1800.01.5,ou=scopes,o=jans",
        "inum=1800.04.12,ou=scopes,o=jans",
        "inum=1800.01.23,ou=scopes,o=jans",
        "inum=1800.01.4,ou=scopes,o=jans",
        "inum=1800.04.13,ou=scopes,o=jans",
        "inum=1800.04.10,ou=scopes,o=jans",
        "inum=1800.04.11,ou=scopes,o=jans",
        "inum=1800.01.1,ou=scopes,o=jans",
        "inum=1800.01.28,ou=scopes,o=jans",
        "inum=1800.01.29,ou=scopes,o=jans",
        "inum=1800.01.24,ou=scopes,o=jans",
        "inum=1800.01.25,ou=scopes,o=jans",
        "inum=1800.01.26,ou=scopes,o=jans",
        "inum=1800.01.27,ou=scopes,o=jans",
        "inum=1800.01.7,ou=scopes,o=jans",
        "inum=1800.01.6,ou=scopes,o=jans",
        "inum=1800.01.9,ou=scopes,o=jans",
        "inum=1800.01.8,ou=scopes,o=jans",
        "inum=1800.01.53,ou=scopes,o=jans",
        "inum=1800.01.54,ou=scopes,o=jans",
        "inum=1800.01.55,ou=scopes,o=jans",
        "inum=1800.01.56,ou=scopes,o=jans",
        "inum=1800.01.50,ou=scopes,o=jans",
        "inum=1800.01.51,ou=scopes,o=jans",
        "inum=1800.01.52,ou=scopes,o=jans",
        "inum=1800.01.57,ou=scopes,o=jans",
        "inum=1800.01.58,ou=scopes,o=jans",
        "inum=1800.01.59,ou=scopes,o=jans",
        "inum=1800.01.42,ou=scopes,o=jans",
        "inum=1800.01.43,ou=scopes,o=jans",
        "inum=1800.01.44,ou=scopes,o=jans",
        "inum=1800.04.2,ou=scopes,o=jans",
        "inum=1800.01.45,ou=scopes,o=jans",
        "inum=1800.04.1,ou=scopes,o=jans",
        "inum=1800.01.40,ou=scopes,o=jans",
        "inum=1800.01.41,ou=scopes,o=jans",
        "inum=1800.01.46,ou=scopes,o=jans",
        "inum=1800.01.47,ou=scopes,o=jans",
        "inum=1800.01.49,ou=scopes,o=jans",
        "inum=1800.04.8,ou=scopes,o=jans",
        "inum=1800.04.7,ou=scopes,o=jans",
        "inum=1800.04.9,ou=scopes,o=jans",
        "inum=1800.04.4,ou=scopes,o=jans",
        "inum=1800.04.3,ou=scopes,o=jans",
        "inum=1800.04.6,ou=scopes,o=jans",
        "inum=1800.04.5,ou=scopes,o=jans",
        "inum=C4F7,ou=scopes,o=jans",
        "inum=1200.635604,ou=scopes,o=jans",
        "inum=1200.4523C3,ou=scopes,o=jans"
      ],
      "claims": [],
      "trustedClient": false,
      "persistClientAuthorizations": true,
      "includeClaimsInIdToken": false,
      "customAttributes": [
        {
          "name": "displayNameLocalized",
          "multiValued": true
        },
        {
          "name": "jansClntURILocalized",
          "multiValued": true
        },
        {
          "name": "jansLogoURILocalized",
          "multiValued": true,
          "values": [
            "{}"
          ],
          "value": "{}",
          "displayValue": "{}"
        },
        {
          "name": "jansPolicyURILocalized",
          "multiValued": true,
          "values": [
            "{}"
          ],
          "value": "{}",
          "displayValue": "{}"
        },
        {
          "name": "jansTosURILocalized",
          "multiValued": true,
          "values": [
            "{}"
          ],
          "value": "{}",
          "displayValue": "{}"
        }
      ],
      "rptAsJwt": false,
      "accessTokenAsJwt": false,
      "accessTokenSigningAlg": "RS256",
      "disabled": false,
      "authorizedOrigins": [],
      "attributes": {
        "runIntrospectionScriptBeforeJwtCreation": false,
        "keepClientAuthorizationAfterExpiration": false,
        "allowSpontaneousScopes": false,
        "backchannelLogoutSessionRequired": false,
        "parLifetime": 600,
        "requirePar": false,
        "dpopBoundAccessToken": false,
        "jansDefaultPromptLogin": false,
        "minimumAcrLevel": -1
      },
      "displayName": "Jans Config Api Client",
      "authenticationMethod": "client_secret_basic",
      "allAuthenticationMethods": [
        "client_secret_basic"
      ],
      "baseDn": "inum=1800.6f1348e5-8f95-41bc-b2bc-3aa567d75a61,ou=clients,o=jans",
      "inum": "1800.6f1348e5-8f95-41bc-b2bc-3aa567d75a61"
    },
    {
      "dn": "inum=2000.e66eb923-5e67-4245-ad0a-bc8db2821ae0,ou=clients,o=jans",
      "deletable": false,
      "clientSecret": "PJ6rxz5g0Pyc",
      "frontChannelLogoutUri": "http://localhost:4100/logout",
      "frontChannelLogoutSessionRequired": false,
      "redirectUris": [
        "https://jans-project-2/admin",
        "http://localhost:4100"
      ],
      "claimRedirectUris": [],
      "responseTypes": [
        "code"
      ],
      "grantTypes": [
        "authorization_code",
        "refresh_token",
        "client_credentials",
        "urn:ietf:params:oauth:grant-type:device_code"
      ],
      "applicationType": "web",
      "contacts": [],
      "clientName": "Jans TUI Client",
      "clientNameLocalized": {},
      "logoUriLocalized": {},
      "clientUriLocalized": {},
      "policyUriLocalized": {},
      "tosUriLocalized": {},
      "subjectType": "pairwise",
      "idTokenSignedResponseAlg": "RS256",
      "userInfoSignedResponseAlg": "RS256",
      "tokenEndpointAuthMethod": "client_secret_basic",
      "defaultAcrValues": [],
      "postLogoutRedirectUris": [
        "http://localhost:4100",
        "https://jans-project-2/admin"
      ],
      "requestUris": [],
      "scopes": [
        "inum=C4F7,ou=scopes,o=jans",
        "inum=C4F6,ou=scopes,o=jans",
        "inum=43F1,ou=scopes,o=jans",
        "inum=764C,ou=scopes,o=jans",
        "inum=F0C4,ou=scopes,o=jans",
        "inum=B9D2-D6E5,ou=scopes,o=jans"
      ],
      "claims": [],
      "trustedClient": true,
      "persistClientAuthorizations": true,
      "includeClaimsInIdToken": false,
      "accessTokenLifetime": 2592000,
      "customAttributes": [
        {
          "name": "displayNameLocalized",
          "multiValued": true
        },
        {
          "name": "jansClntURILocalized",
          "multiValued": true
        },
        {
          "name": "jansLogoURILocalized",
          "multiValued": true
        },
        {
          "name": "jansPolicyURILocalized",
          "multiValued": true
        },
        {
          "name": "jansTosURILocalized",
          "multiValued": true
        }
      ],
      "rptAsJwt": false,
      "accessTokenAsJwt": false,
      "accessTokenSigningAlg": "RS256",
      "disabled": false,
      "authorizedOrigins": [],
      "attributes": {
        "runIntrospectionScriptBeforeJwtCreation": false,
        "keepClientAuthorizationAfterExpiration": false,
        "allowSpontaneousScopes": false,
        "updateTokenScriptDns": [
          "inum=2D3E.5A04,ou=scripts,o=jans"
        ],
        "backchannelLogoutSessionRequired": false,
        "parLifetime": 600,
        "requirePar": false,
        "dpopBoundAccessToken": false,
        "jansDefaultPromptLogin": false,
        "minimumAcrLevel": -1
      },
      "displayName": "Jans TUI Client",
      "authenticationMethod": "client_secret_basic",
      "allAuthenticationMethods": [
        "client_secret_basic"
      ],
      "baseDn": "inum=2000.e66eb923-5e67-4245-ad0a-bc8db2821ae0,ou=clients,o=jans",
      "inum": "2000.e66eb923-5e67-4245-ad0a-bc8db2821ae0"
    }
  ]
}
```

It will show all the openid clients together. To search using parameters:

```bash title="Command"
/opt/jans/jans-cli/config-cli.py --operation-id get-oauth-openid-clients \
--endpoint-args limit:2
```


### Creating a New OpenID Clients

To add a openid client, we can use `post-oauth-openid-client` operation id.
As shown in the [output](#using-command-line) for `--info` command, the
`post-oauth-openid-client` operation requires data to be sent
according to `Client` schema.

To see the schema, use the command below:

```bash title="Command"
/opt/jans/jans-cli/config-cli.py --schema Client
```

The Janssen Server also provides an example of data that adheres to
the above schema. To fetch the example, use the command below.

```bash title="Command"
/opt/jans/jans-cli/config-cli.py --schema-sample Client
```

Using the schema and the example above, we have added below openid 
client data to the file `/tmp/oprnid-connect.json`.

It contains a lot of properties. But, It's not important to
fill each of these properties. We are going to fill required propertie:

```json title="Input" 
{
  "redirectUris": [
    "https://www.google.com/"
  ]
}
```
Now let's post openid client to the Janssen Server to be 
added to the existing set:

```bash title="Command"
/opt/jans/jans-cli/config-cli.py --operation-id post-oauth-openid-client\
--data /tmp/oprnid-connect.json
```

### Update OpenId Connect client

To update the configuration follow the steps below.

1. [Get the existing OpenID Client](#get-openid-connect-client-by-inum) and store it into a file for editing. 
The following command will retrieve the existing OpenID Client in the schema file.
```bash title="Command"
/opt/jans/jans-cli/config-cli.py -no-color --operation-id get-oauth-openid-clients-by-inum\
 --url-suffix inum:a89b5c29-2a91-48b5-bf27-1bf786954a06 > /tmp/update-client.json
```
2. Edit and update the desired configuration values in the file while keeping
   other properties and values unchanged. Updates must adhere to the `Client`
   schema as mentioned here.
3. We have changed in `redirectUris` only the `https://www.google.com/` to `https://gluu.org/`.
   Use the updated file to send the update to the Janssen Server using the
   command below


```bash title="Command"
/opt/jans/jans-cli/config-cli.py --operation-id put-oauth-openid-client\
 --data /tmp/update-client.json
```

Upon successful execution of the update, the Janssen Server responds with updated configuration


### Patch OpenID Client by inum  

Using `patch-oauth-openid-client-by-inum`, we can modify OpenID Clients partially for its properties.

To use this operation, specify the id of the key that needs to be updated using the 
`--url-suffix` and the property and the new value using the [JSON Patch](https://jsonpatch.com/#the-patch). 
Refer [here](../../config-guide/config-tools/jans-cli/#patch-request-schema) 
to know more about schema.

In this example; We will change the value of the property `applicationType` from `web` to `native`.

```bash title="Input" linenums="1"
[
{
  "op": "replace",
  "path": "applicationType",
  "value": "native"
}
]
```

Now let's do the operation with the command line.

```bash 
/opt/jans/jans-cli/config-cli.py --operation-id patch-oauth-openid-client-by-inum \
--url-suffix inum:a89b5c29-2a91-48b5-bf27-1bf786954a06 --data /tmp/schema.json
```

### Get OpenId Connect Client by Inum

With `get-oauth-openid-clients-by-inum` operation-id, we can get any specific Client matched with `Inum`.
If we know the `Inum`, we can simply use the below command:


```bash 
/opt/jans/jans-cli/config-cli.py --operation-id get-oauth-openid-clients-by-inum \
--url-suffix inum:a89b5c29-2a91-48b5-bf27-1bf786954a06
```
The result will only show this `inum:a89b5c29-2a91-48b5-bf27-1bf786954a06`.


### Delete OpenId Connect client

Delete the json openid client using its `inum`. The command line is:

```bash title="Command"
/opt/jans/jans-cli/config-cli.py --operation-id delete-oauth-openid-client-by-inum \
--url-suffix inum:a89b5c29-2a91-48b5-bf27-1bf786954a06
```
It will delete the openid client if it matches with the given `inum`.

## Using-text-based-ui


In Janssen, You can manage Logging configuration using
the [Text-Based UI](../config-tools/jans-tui/README.md) also.

You can start TUI using the command below:

```bash title="Command"
sudo /opt/jans/jans-cli/jans_cli_tui.py
```

### Client Screen

Navigate to `Auth Server` -> `Clients` to open the client screen as shown
in the image below.

![image](../../../assets/tui-client-screen.png)

* This screen shows the openid Clients list. 
* To get the list of currently added client, bring the control to Search box (using the tab key),
and press Enter. Type the search string to search for Client with matching Client name.


### Add Client screen

* Use the `Add Client` button to create a new client.
* After adding the valid data using the `Save` button, you can add the new openid client.
* Also, you can update the openid client.

![image](../../../assets/tui-add-client-screen.png)


## Using Configuration REST API

Janssen Server Configuration REST API exposes relevant endpoints for managing
and configuring the Openid Client. Endpoint details are published in the [Swagger
document](../../reference/openapi.md).