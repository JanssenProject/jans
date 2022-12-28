---
tags:
  - administration
  - configuration
  - cli
  - curl
---

# Most useful configurations and operations on Jans server using CURL 
## Configuring the Janssen server

To use CURL commands and configure Janssen's Authorization server, you need to have an access token of "Config-API" (which is an RP of Jans-auth server). Configurations to the AS can be done **only** through "The Config-API client (RP)".

### Obtaining an Access token

All commands to configure the AS are protected by an Access token. According to the use case, you must specify the `scope` for which the access token has been requested.
For the client_id and client_secret, contact your administrator.

**Template**

```
curl -u "client_inum:client_secret" https://<your.jans.server>/jans-auth/restv1/token \
    -d  "grant_type=client_credentials&scope=put_scope_name_here"
```

**Example**: 

To modify a custom script, you need to request an access token using the scope `scope=https://jans.io/oauth/config/scripts.write`

```
curl -u "put_client_id_here:put_config_api_client_secret_here" https://<your.jans.server>/jans-auth/restv1/token \
     -d  "grant_type=client_credentials&scope=https://jans.io/oauth/config/scripts.write" 
```

### Enable an authentication script

#### Steps

1. Obtain a token, use scope `https://jans.io/oauth/config/scripts.write`
```
curl -u "put_client_id_here:put_config_api_client_secret_here" https://<your.jans.server>/jans-auth/restv1/token \
     -d  "grant_type=client_credentials&scope=https://jans.io/oauth/config/scripts.write" 
```
2. Enable the script
```
curl https://<your.jans.server>/jans-config-api/api/v1/config/scripts/name/name_of_the_script \ 
    -H "Authorization: Bearer put_access_token_here"
```

Examples of `name_of_the_script` ( Authentication methods that are present in the Janssen server.)

| Name of the script |
|---|
| smpp  |
| otp |
| duo |
| fido2 |
| super_gluu |
| twilio_sms |
| smpp |
| otp |
| duo |
| fido2 |
| super_gluu |

### Client creation

1. Creating a client with minimal upfront configuration requires only `redirectUris` in following format. Add following content in a text file and save it as client.json. 
```json
{
  "redirectUris": [
  "http://localhost:8080"
  ]  
}
```
In place of `http://localhost:8080` uri used above, any valid redirect URI can be used.
To provide full client configuration at the time of creation, Download and use this 
[json format](https://raw.githubusercontent.com/JanssenProject/jans/main/jans-config-api/server/src/test/resources/feature/openid/clients/client.json), 
update the values and save it as client.json. Few important fields to populate are `scope`,`responseTypes`,
`redirectUris` (The only mandatory field), `grantTypes`. If there is a need to update the attributes of the client after 
creation, then make sure that the client is created with `grantTypes` list having `client_credentials` grant type as well.

1. Run curl command
```
curl -X POST https://my.jans.server/jans-auth/restv1/register \ 
     -H "Content-Type: application/json"  -d @/some/directory/client.json
```

If client is created successfully, response similar to below will be received:

```json
{
    "allow_spontaneous_scopes": false,
    "application_type": "web",
    "rpt_as_jwt": false,
    "registration_client_uri": "https://my.jans.server/jans-auth/restv1/register?client_id=994ec0a7-1143-456c-85ca-66ba592d7f9a",
    "tls_client_auth_subject_dn": "",
    "run_introspection_script_before_jwt_creation": false,
    "registration_access_token": "d34f30ff-ae31-4760-8b67-ef071ba9ee68",
    "client_id": "994ec0a7-1143-456c-85ca-66ba592d7f9a",
    "token_endpoint_auth_method": "client_secret_basic",
    "scope": "openid profile permission https://jans.io/auth/ssa.portal uma_protection work_phone phone address test https://jans.io/auth/ssa.admin user_name email clientinfo device_sso org_name https://jans.io/auth/ssa.developer offline_access",
    "client_secret": "da4c17de-b6bc-4f25-b642-4c7b887c7860",
    "client_id_issued_at": 1672221633,
    "backchannel_logout_uri": [],
    "backchannel_logout_session_required": false,
    "par_lifetime": 600,
    "spontaneous_scopes": [],
    "id_token_signed_response_alg": "RS256",
    "access_token_as_jwt": false,
    "grant_types": [
        "authorization_code",
        "refresh_token"
    ],
    "subject_type": "pairwise",
    "keep_client_authorization_after_expiration": false,
    "require_par": false,
    "redirect_uris_regex": "",
    "additional_audience": [],
    "frontchannel_logout_session_required": false,
    "client_secret_expires_at": 1672308033,
    "access_token_signing_alg": "RS256",
    "response_types": ["code"]
}
```
Response contains `client_id` and `client_secret` apart from other client configuration details.

### Get Client Scopes

1. Obtain an Access Token with scope `https://jans.io/oauth/config/openid/clients.readonly`.
```
 curl -u "put_client_id_here:put_config_api_client_secret_here" https://<your.jans.server>/jans-auth/restv1/token \ 
      -d  "grant_type=client_credentials&scope=https://jans.io/oauth/config/openid/clients.readonly" 
```
2. Obtain client information using:
```
curl -X GET https://my.jans.server/jans-config-api/api/v1/openid/clients/client-s_inum_for_which_scope_to_be_added 
   -H "Authorization: Bearer put_access_token_here"
``` 
3. Notice the `scope` field. It is a space-separated String of scope values e.g `"scope" : "openid user_name "`. 

### Add New Scope to Client 

To the above client, lets append the profile, so the scope attrib should now have value "openid user_name profile"`. This new value will be patched onto the client. 
   
1. Obtain an Access Token with scope `https://jans.io/oauth/config/openid/clients.write`
```
curl -u "put_client_id_here:put_config_api_client_secret_here" https://<your.jans.server>/jans-auth/restv1/token \
         -d  "grant_type=client_credentials&scope=https://jans.io/oauth/config/openid/clients.write" 
```
2. Patch the new scope for the client 
```
curl -X PATCH -k -H 'Content-Type: application/json-patch+json' \ 
         -i 'https://my.jans.server/jans-config-api/api/v1/openid/clients/put_client_inum_here' \ 
         -H "Authorization: Bearer put_access_token_here" --data '[
     {
    "op": "add",
    "path": "/scope",
    "value": "openid user_name profile"
     }
     ]'
```
	
### Get grant_types For Client

1. Obtain an Access Token with scope `https://jans.io/oauth/config/openid/clients.readonly`.
```
 curl -u "put_client_id:put_config_api_client_secret_here" https://<your.jans.server>/jans-auth/restv1/token \
      -d  "grant_type=client_credentials&scope=https://jans.io/oauth/config/openid/clients.readonly" 
```
2. Obtain client information using:
```
 curl -X GET https://my.jans.server/jans-config-api/api/v1/openid/clients/client-s_inum_for_which_grant_types_to_check \ 
      -H "Authorization: Bearer put_access_token_here"
``` 
3. Notice the `grant_types` field in the response.  

### Add OpenID scope and map to database attribute

1. Obtain access token
```
curl -k -u "put_client_id:put_client_secret" https://jans-ui.jans.io/jans-auth/restv1/token \
     -d  "grant_type=client_credentials&scope=https://jans.io/oauth/config/scopes.write" 
```
2. Build json on similar lines
```
{
  "dn": "string",
  "inum": "string",
  "displayName": "string",
  "id": "string",
  "iconUrl": "string",
  "description": "string",
  "scopeType": "openid",
  "claims": [
    "string"
  ],
  "defaultScope": true,
  "groupClaims": true,
  "dynamicScopeScripts": [
    "string"
  ],
  "umaAuthorizationPolicies": [
    "string"
  ],
  "attributes": {
    "spontaneousClientId": "string",
    "spontaneousClientScopes": [
      "string"
    ],
    "showInConfigurationEndpoint": true
  },
  "umaType": false,
  "deletable": false,
  "expirationDate": "2022-07-26"
}
```
3. Run curl. Note the `claims` field which maps to the database attrib
```
curl -k -X POST https://jans-ui.jans.io/jans-config-api/api/v1/scopes -H "Content-Type: application/json" 
     -H "Authorization: Bearer use_bearer_token_here"  --data '{
  "dn": "inum=AAC1,ou=scopes,o=jans",
  "inum": "AAC1",
  "displayName": "website",
  "id": "website",
    "description": "website",
  "scopeType": "openid",
  "claims": ["website"],
  "umaType": false,
  "deletable": false
}'
```

## Using the Janssen server

### Accessing OpenID Discovery endpoint / Well-known endpoint
```
curl https://jans-ui.jans.io/.well-known/openid-configuration
```

### Client Credentials Flow

```
curl -k -u "put_client_id:put_client_secret" https://jans-ui.jans.io/jans-auth/restv1/token \ 
      -d  "grant_type=client_credentials&scope=https://jans.io/oauth/config/scopes.write" 
```

### Authorization code flow

Steps:

1. On a browser type `https://my.jans.server/jans-auth/restv1/authorize?redirect_uri=https://my-redirect-app:8080&client_id=Put_client_id_here&scope=username+openid&response_type=code`
2. Based on the default authentication method set, the user will be presented with credentials for login. The OpenID Provider (Gluu Server) verifies the user’s identity and authenticates the user.
3. In the back channel the following steps take place : 
   1. The OpenID Provider (Gluu Server) sends the user back to the application with an authorization code.
   2. The application sends the code to the Token Endpoint to receive an Access Token and ID Token in the response.
   3. The application uses the ID Token to authorize the user. At this point the application/RP can access the UserInfo endpoint for claims.

### Device Authorization code flow

1. Client_id that is used in the curl command below should have grant_type `urn:ietf:params:oauth:grant-type:device_code`
2. Call the Device Authorization Endpoint :
    ```
    curl -k -u "client_id_here:client_secret_here" https://jans-ui.jans.io/jans-auth/restv1/device_authorization \
         -d  scope=openid+profile+email+offline_access" 
    ```
3. Response received will be like this
    ```
    {
      "user_code": "HJDN-BMHQ",
      "device_code": "b8a5e5e6b1c10506af4f4bbb5400ca2587dcfe44974d7e62",
      "interval": 5,
      "verification_uri_complete": "https://jans-ui.jans.io/device-code?user_code=HJDN-BMHQ",
      "verification_uri": "https://jans-ui.jans.io/device-code",
      "expires_in": 1800
    }
    ```
4. User should visit the `verification_uri` link on a browser and enter the `user_code`.
5. Until the user to activates the device, begin polling token URL to request an Access Token. Use `interval` from step 2 as the polling interval.
    ```
    curl -k -u "client_id:put_client_secret" https://jans-ui.jans.io/jans-auth/restv1/token \
         -d "grant_type=client_credentials&grant_type=urn:ietf:params:oauth:grant-type:device_code&device_code=YOUR_DEVICE_CODE&client_id=YOUR_CLIENT_ID" 
    ```
 


