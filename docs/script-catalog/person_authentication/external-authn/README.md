# External Authn

## Design
```
Title External Authn

actor Person
participant Browser
participant Website1
participant Website2
participant IDP
database cache

Person->Browser: 1. Navigate to website1
Browser->Website1:
Website1->Browser: 2. redirect
group Person Authn Script : Step One
Browser->IDP: 3. [GET] /oxauth/authorize?client_id=__&redirect_uri=__&state=__\n&nonce=__&prompt=none&scope=__\n&response_mode=__&response_type=__
IDP->IDP: 4. generate jans_key
IDP<->cache: cache in session context\njans_key: {request params}
IDP->Browser: 5. redirect /internal.idp?________\nSet Pre-Authn Session Cookie
Browser->Website2: 
end
Website2->Browser: 6. Display login page
Person->Browser: 7. Enter Username / PW
Browser->Website2: 8. (creds)
group ROPW script
Website2->IDP: 9. /oxauth/token?uid=__&pw="__&browser_ip=__&jans_key=__
IDP->IDP: 10. update cache:\n "jans_key": "auth_success"
IDP->IDP: 11. retreive user claims
IDP->Website2:12. {\n        "callback_url":"https://op-host/oxauth**/authorize?session_id={session_id}&redirect_uri={original_redirect}&...**",\n        "userinfo": {"uid": "__",...}\n       }
end
group Person Authn Script Step 2
Website2->Browser: 13. write website 2 cookie;\n302 Location IDP callback_url
Browser->IDP: 14. callback_url_from_step_12
IDP->IDP: 15. get session context
IDP->cache:16. delete jans_key\n lookup original redirect_uri
IDP->Browser: 17. write IDP session cookie\nand 302: Location original redirect_uri
end
Browser->Website1:
Website1->Website1: optional: 18 Validate id_token\n (claims optional)
```
![](external-authn-diagram.png)

Follow the instructions below to set up:

## Jans App Configuration
Above the **jans-auth** record of the **jansAppConf** table, enable a new attribute on the **jansConfDyn** field

Example:
```
"authorizationRequestCustomAllowedParameters": {
    {
        "paramName": "jansKey",
        "returnInResponse": false
    }
}
```

## Enable Custom Script

- ### Person Authentication - External Authn

Create a new custom script via `jans-config-api` or via `jans-cli` to create a new record in table **jansCustomScr**.
```
INSERT INTO jansCustomScr ( doc_id, objectClass, dn, inum, displayName, description, jansScr, jansScrTyp, jansProgLng, jansModuleProperty, jansConfProperty, jansLevel, jansRevision, jansEnabled, jansScrError, jansAlias ) 
VALUES ( 'PA01-EA01', 'jansCustomScr', 'inum=PA01-EA01,ou=scripts,o=jans', 'PA01-EA01', 'pa-external-authn', 'PA External Authn', '', 'person_authentication', 'python', '{"v": ["{\\"value1\\":\\"usage_type\\",\\"value2\\":\\"interactive\\",\\"description\\":\\"\\"}", "{\\"value1\\":\\"location_type\\",\\"value2\\":\\"ldap\\",\\"description\\":\\"\\"}"]}', '{"v": ["{\\"value1\\":\\"urlstep1\\",\\"value2\\":\\"http://demoexample.net:81\\",\\"hide\\":false,\\"description\\":\\"Url to return in step 1\\"}"]}', 1, 1, 0, NULL, '{"v": []}' );
```

Modify the **jansConfProperty** field by replacing **URL_REDIRECT_URI** with the url that you want to callback in the first part of the flow.
```
'{"v": ["{\\"value1\\":\\"urlstep1\\",\\"value2\\":\\"{URL_REDIRECT_URI}\\",\\"hide\\":false,\\"description\\":\\"Url to return in step 1\\"}"]}'
```

Modify the **jansScr** field by adding the content of the following link: [PersonAuthentication Script](pa-external-authn.py)

- ### ROPC (Resource Owner Password Credentials) Script - External Authn

Create a new custom script via `jans-config-api` or via `jans-cli` to create a new record in table **jansCustomScr**.
```
INSERT INTO jansCustomScr ( doc_id, objectClass, dn, inum, displayName, description, jansScr, jansScrTyp, jansProgLng, jansModuleProperty, jansConfProperty, jansLevel, jansRevision, jansEnabled, jansScrError, jansAlias ) 
VALUES ( 'ROPC-EA01', 'jansCustomScr', 'inum=ROPC-EA01,ou=scripts,o=jans', 'ROPC-EA01', 'ropc-external-authn', 'ROPC External Authn', '', 'resource_owner_password_credentials', 'python', '{"v": ["{\\"value1\\":\\"location_type\\",\\"value2\\":\\"ldap\\",\\"description\\":\\"\\"}"]}', '{"v": []}', 1, 1, 0, NULL, '{"v": []}' );  
```

Modify the **jansScr** field by adding the content of the following link: [ROPC (Resource Owner Password Credentials) Script](ropc-external-authn.py)

- ### Update Token Script - External Authn

Create a new custom script via `jans-config-api` or via `jans-cli` to create a new record in table **jansCustomScr**.
```
INSERT INTO jansCustomScr ( doc_id, objectClass, dn, inum, displayName, description, jansScr, jansScrTyp, jansProgLng, jansModuleProperty, jansConfProperty, jansLevel, jansRevision, jansEnabled, jansScrError, jansAlias ) 
VALUES ( 'UPDT-EA01', 'jansCustomScr', 'inum=UPDT-EA01,ou=scripts,o=jans', 'UPDT-EA01', 'update-token-external-authn', 'Update token External Authn', '', 'update_token', 'python', '{"v": ["{\\"value1\\":\\"location_type\\",\\"value2\\":\\"ldap\\",\\"description\\":\\"\\"}"]}', '{"v": []}', 1, 1, 0, NULL, '{"v": []}' );  
```

Modify **jansScr** field by adding the content of the following link: [Update Token Script](ut-external-authn.py)

In this script you can choose whether to use the header or payload of the **id_token** for the **callback_uri**:
```
jsonWebResponse.getHeader().setClaim("callback_url", jsonValCallbackUrl)
jsonWebResponse.getClaims().setClaim("callback_url", jsonValCallbackUrl)
```

## Enable custom script on the Client
In the table **jansClnt** modify the field **jansAttrs** associating the custom scripts configured previously
```
"updateTokenScriptDns": ["inum=UPDT-EA01,ou=scripts,o=jans"],
"ropcScripts": ["inum=ROPC-EA01,ou=scripts,o=jans"],
```

And in the field **jansDefArcValues** add this value:
```
{"v": ["pa-external-authn"]}
```

## Flow
- ### Part 1: /authorize
Request:
```
curl --location --request GET 'https://jans.localhost/jans-auth/restv1/authorize?response_type=code&client_id=14e36e18-1d51-41ac-a4cf-a7dc677f53a5&scope=openid+profile+address+email&redirect_uri=https://jans.localhost/jans-auth-rp/home.htm&state=a84dd61f-533c-46a4-9315-a66fda3e9a4e&nonce=80e6bd2b-eb78-48b9-be9c-6bb33ef80991&ui_locales=&claims_locales=&request_session_id=false&acr_values='
```
Response: (return the **redirect_uri** with jansKey)
```
http://demoexample.net:81?jansKey=46340f40-a554-46b1-9246-37c2e869919f
```

- ### Part 2: /token
Request: (**Authorization** = Basic base64(client_id:client_secret))
```
curl --location --request POST 'https://jans.localhost/jans-auth/restv1/token' \
--header 'Authorization: Basic MTRlMzZlMTgtMWQ1MS00MWFjLWE0Y2YtYTdkYzY3N2Y1M2E1Ojk5NzE4NWU1LTc2NGUtNGE4Yi1hNjYwLTdjZmQ4NzJhNjc0Ng==' \
--header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode 'grant_type=password' \
--data-urlencode 'username=test_user' \
--data-urlencode 'password=test_user_password' \
--data-urlencode 'scope=openid' \
--data-urlencode 'jansKey=46340f40-a554-46b1-9246-37c2e869919f'
```
Response: (id_token contains in header or payload callback_url)
```
{
    "access_token": "a0878887-b998-4da4-aa0b-4e74bd9a4441",
    "refresh_token": "d8b618ac-9d9c-4b90-9cac-aafb1e38e82e",
    "scope": "openid",
    "id_token": "eyJjYWxsYmFja191cmwiOiJodHRwczovL2phbnMubG9jYWxob3N0L2phbnMtYXV0aC9yZXN0djEvYXV0aG9yaXplP3Jlc3BvbnNlX3R5cGU9Y29kZSZzZXNzaW9uX2lkPWI1MDFmNzg0LTY0MTAtNDIyYy1iYWQ0LWU0MTNiYTViMjI1NSZyZWRpcmVjdF91cmk9aHR0cHMlM0ElMkYlMkZqYW5zLmxvY2FsaG9zdCUyRmphbnMtYXV0aC1ycCUyRmhvbWUuaHRtJmNsaWVudF9pZD0xNGUzNmUxOC0xZDUxLTQxYWMtYTRjZi1hN2RjNjc3ZjUzYTUiLCJraWQiOiIxNmEyMmIwMy03YjUzLTQxY2QtYWE3OS01YjI1ZjNlY2QzOGNfc2lnX3JzMjU2IiwidHlwIjoiand0IiwiYWxnIjoiUlMyNTYifQ.eyJjYWxsYmFja191cmwiOiJodHRwczovL2phbnMubG9jYWxob3N0L2phbnMtYXV0aC9yZXN0djEvYXV0aG9yaXplP3Jlc3BvbnNlX3R5cGU9Y29kZSZzZXNzaW9uX2lkPWI1MDFmNzg0LTY0MTAtNDIyYy1iYWQ0LWU0MTNiYTViMjI1NSZyZWRpcmVjdF91cmk9aHR0cHMlM0ElMkYlMkZqYW5zLmxvY2FsaG9zdCUyRmphbnMtYXV0aC1ycCUyRmhvbWUuaHRtJmNsaWVudF9pZD0xNGUzNmUxOC0xZDUxLTQxYWMtYTRjZi1hN2RjNjc3ZjUzYTUiLCJhdWQiOiIxNGUzNmUxOC0xZDUxLTQxYWMtYTRjZi1hN2RjNjc3ZjUzYTUiLCJhY3IiOiJzaW1wbGVfcGFzc3dvcmRfYXV0aCIsInN1YiI6InpMM3FrWFVwZE9OOXoxRDNNVUkyRG1CTHd3MzA1RUt4eWY3al8zb0oyaDQiLCJjb2RlIjoiNzg4OTA4ZTAtMmM3MC00YjE1LWFkZmUtYWZlMDBiMTgyMTkxIiwiYW1yIjpbXSwiaXNzIjoiaHR0cHM6Ly9qYW5zLmxvY2FsaG9zdCIsImV4cCI6MTY1OTc1ODA4MywiZ3JhbnQiOiJwYXNzd29yZCIsImlhdCI6MTY1OTc1NDQ4Mywic2lkIjoiODA0OGZjYmYtM2RjNC00YjFhLTgwYjktNmU1NTkzZTJhOWMwIiwib3hPcGVuSURDb25uZWN0VmVyc2lvbiI6Im9wZW5pZGNvbm5lY3QtMS4wIn0.u0sK9Yccf-P0OLQe0cKz75dF3cKQzQt9QpoHbqBsuIgXb4YwzDTWSuVM055USo5iO9P3KUiIGnMs6WPrZa-xcH84oKWsjF4TnAHdtwE_1xSofxDUU1nSdg8V3mRjHmZKWAvSmGtWTJFqTNGJKQsz1xrTpZ4ZAsI8Ey5OUNIhLsS6BIrFixLm3Sjufe4I2hiTtUPXb1PSYkPgHPZI00h8h4NDspPM9syufMbSLi-HN1aTQaJCONw2X-4KnvLGq1utX7qryclW1pq17W2j6Z49CjCW5ZtstK2p2FRoPwnInXNNr6vqSrpWR6pY9Uus7LHZZbg2DWKBASyfdjN6ecQaIw",
    "token_type": "Bearer",
    "expires_in": 299
}
```

- ### Part 3: callback_uri (/authorize)

Request:
```
curl --location --request GET 'https://jans.localhost/jans-auth/restv1/authorize?response_type=code&session_id=b501f784-6410-422c-bad4-e413ba5b2255&redirect_uri=https%3A%2F%2Fjans.localhost%2Fjans-auth-rp%2Fhome.htm&client_id=14e36e18-1d51-41ac-a4cf-a7dc677f53a5'
```

Response: (return to the **redirect_uri**)
```
https://jans.localhost/jans-auth-rp/home.htm?code=441688df-8f36-4e2c-8174-18d23cc88049&acr_values=pa-external-authn&session_id=7ee59d72-d59a-49ce-a0cb-19c4fcfc404c&session_state=c3f595a892208e3d237722ad06d830f199295ccc355827c436fff71509401eae.a505421b-a332-4604-8772-6ca345c4a4b9
```