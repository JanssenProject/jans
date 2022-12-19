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

### 1. Obtaining an Access token
All commands to configure the AS are protected by an Access token. According to the use case, you must specify the `scope` for which the access token has been requested.
For the client_id and client_secret, contact your administrator.
<br/>**Template**:
```
curl -u "client_inum:client_secret" https://<your.jans.server>/jans-auth/restv1/token \
    -d  "grant_type=client_credentials&scope=put_scope_name_here
```
**Example**: To modify a custom script, you need to request an access token using the scope `scope=https://jans.io/oauth/config/scripts.write`
```
curl -u "put_client_id_here:put_config_api_client_secret_here" https://<your.jans.server>/jans-auth/restv1/token \
     -d  "grant_type=client_credentials&scope=https://jans.io/oauth/config/scripts.write" 
```

***

### 2. Enable an authentication script
Steps:
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

***

### 3. Add scope to client
1. Obtain the pre-existing scopes of the client
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
   3. Notice the `scope` field. It is a space-seperated String of scope values e.g `"scope" : "openid user_name "`. To this, lets append the profile, so the scope attrib should now have value "openid user_name profile"`. This new value will be patched onto the client. 
   
1. Patch the client
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
***	
### 4. Get grant_types for client

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
***   

### 5. Add OpenID scope and map to database attribute

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

### 1. OpenID Discovery endpoint / Well-known endpoint
```
curl https://jans-ui.jans.io/.well-known/openid-configuration
```
### 2. Client creation

Steps:
1. Download this [json file](https://raw.githubusercontent.com/JanssenProject/jans/replace-janssen-version/jans-config-api/server/src/test/resources/feature/openid/clients/client.json), update the values and save it as client.json.
<br/>Few important fields to populate are `scope`,`responseTypes`,`redirectUris` (The only mandatory field), `grantTypes`
1. Run curl command
```
curl -X POST https://my.jans.server/jans-auth/restv1/register \ 
     -H "Content-Type: application/json"  -d @/some/directory/client.json
```

Further reading

### 2. Client Credentials Flow
```
curl -k -u "put_client_id:put_client_secret" https://jans-ui.jans.io/jans-auth/restv1/token \ 
      -d  "grant_type=client_credentials&scope=https://jans.io/oauth/config/scopes.write" 
```

### 3. Authorization code flow
Steps:
1. On a browser type `https://my.jans.server/jans-auth/restv1/authorize?redirect_uri=https://my-redirect-app:8080&client_id=Put_client_id_here&scope=username+openid&response_type=code`
2. Based on the default authentication method set, the user will be presented with credentials for login. The OpenID Provider (Gluu Server) verifies the user’s identity and authenticates the user.
3. In the back channel the following steps take place : 
   1. The OpenID Provider (Gluu Server) sends the user back to the application with an authorization code.
   2. The application sends the code to the Token Endpoint to receive an Access Token and ID Token in the response.
   3. The application uses the ID Token to authorize the user. At this point the application/RP can access the UserInfo endpoint for claims.


### 4. Device Authorization code flow

1. Client_id that is used in the curl command below should have grant_type `urn:ietf:params:oauth:grant-type:device_code`
2. Call the Device Authorization Endpoint :
    ```
    curl -k -u "client_id_here:client_secret_here" https://jans-ui.jans.io/jans-auth/restv1/device_authorization \
         -d  scope=openid+profile+email+offline_access" 
    ```
3. Response recieved will be like this
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
 


