---
tags:
  - administration
  - auth-server
  - scope
  - authorization-details
---

## OAuth 2.0 Rich Authorization Requests

Rich Authorization Requests introduces new `authorization_details` parameter that is used to carry fine-grained authorization data in OAuth messages.

While `scope` is used for coarse-grained access, `authorization_details` is used for fine-grained access.

`authorization_details` are associated with authorization and thus with client to limit what authorization can be granted within given client.

`authorization_details` is JSON array, example:

```json
[
   {
      "type": "demo_authz_detail",
      "actions": [
          "list_accounts",
          "read_balances"
      ],
      "locations": [
          "https://example.com/accounts"
      ],
      "ui_representation": "Read balances and list accounts at https://example.com/accounts"
   },
   {
       "type":"financial-transaction",
       "actions":[
           "withdraw"
       ],
       "identifier":"account-14-32-32-3",
       "currency":"USD"
   }
]
```

### Authorization Details Types

`type` - is required element in single authorization detail and specifies the authorization details type as a string.
Type defines how single authorization detail is handled by both AS and RS. 
Because "shape" and structure of single authorization detail can vary a lot, validation and representation logic is externalized to `AuthzDetailType` custom scripts.

`type` defines type of authorization detail. Each such type is represented by AS `AuthzDetailType` custom scripts.
It means that for example above administrator must define two `AuthzDetailType` custom scripts with names: `demo_authz_detail` and `financial-transaction`. 

If `authorization_details` parameter is absent in request then `AuthzDetailType` custom scripts are not invoked. 

`demo_authz_detail` and `financial-transaction` `AuthzDetailType` custom scripts must be provided by administrator.

- `demo_authz_detail` is called for all authorization details with `"type": "demo_authz_detail"`
- `financial-transaction` is called for all authorization details with `"type": "financial-transaction"`

Sample Authorization Request
```
POST /jans-auth/restv1/authorize HTTP/1.1
Host: yuriyz-fond-skink.gluu.info

response_type=code&client_id=7a29bf35-96ec-4bbd-a05c-15e1ff9f07cc&scope=openid+profile+address+email+phone+user_name&redirect_uri=https%3A%2F%2Fyuriyz-relaxed-jawfish.gluu.info%2Fjans-auth-rp%2Fhome.htm&state=6cdc7701-178c-4653-adac-5c1e9c6c4aba&nonce=b9a1ecc4-548e-475c-8b29-f019417e1aef&prompt=&ui_locales=&claims_locales=&acr_values=&request_session_id=false&authorization_details=%5B%0A++%7B%0A++++%22type%22%3A+%22demo_authz_detail%22%2C%0A++++%22actions%22%3A+%5B%0A++++++%22list_accounts%22%2C%0A++++++%22read_balances%22%0A++++%5D%2C%0A++++%22locations%22%3A+%5B%0A++++++%22https%3A%2F%2Fexample.com%2Faccounts%22%0A++++%5D%2C%0A++++%22ui_representation%22%3A+%22Read+balances+and+list+accounts+at+https%3A%2F%2Fexample.com%2Faccounts%22%0A++%7D%0A%5D
```

Request is rejected if request's `authorization_details` has types which does not have corresponding `AuthzDetailType` custom script. 

Check more details about [`AuthzDetailType` custom scripts](../../../script-catalog/authz_detail/AuthzDetail.java)  

### AS Metadata (Discovery)

Metadata endpoint has `authorization_details_types_supported` which shows supported authorization details types.
Value for `authorization_details_types_supported` is populated based on valid and enabled `AuthzDetailType` insterception scripts.

For `demo_authz_detail` and `financial-transaction` `AuthzDetailType` custom scripts enabled discovery response has:

```text
{
    "authorization_details_types_supported" : [ "demo_authz_detail", "financial-transaction" ],
    ...
}
```
  
### Client Registration

Client registration request has new parameter `authorization_details_types` to limit authorization details types supported by client.
If request is made with `authorization_details` that has types that are not listed in client's `authorization_details_types` the request will be rejected.

Sample registration request and response
```text
-------------------------------------------------------
REQUEST:
-------------------------------------------------------
POST /jans-auth/restv1/register HTTP/1.1
Host: yuriyz-relaxed-jawfish.gluu.info
Content-Type: application/json
Accept: application/json

{
  "grant_types" : [ "authorization_code", "implicit" ],
  "subject_type" : "public",
  "application_type" : "web",
  "authorization_details_types" : [ "demo_authz_detail" ],
  "scope" : "openid profile address email phone user_name",
  "minimum_acr_priority_list" : [ ],
  "redirect_uris" : [ "https://yuriyz-relaxed-jawfish.gluu.info/jans-auth-rp/home.htm", "https://client.example.com/cb", "https://client.example.com/cb1", "https://client.example.com/cb2" ],
  "client_name" : "jans test app",
  "additional_audience" : [ ],
  "response_types" : [ "code" ]
}

-------------------------------------------------------
RESPONSE:
-------------------------------------------------------
HTTP/1.1 201
Cache-Control: no-store
Connection: Keep-Alive
Content-Length: 1653
Content-Type: application/json
Date: Mon, 18 Dec 2023 17:59:13 GMT
Expires: Thu, 01 Jan 1970 00:00:00 GMT
Keep-Alive: timeout=5, max=100
Pragma: no-cache
Set-Cookie: X-Correlation-Id=7b231c8f-5b2e-445d-b5ea-0f693c1cd7f2; Secure; HttpOnly;HttpOnly
Strict-Transport-Security: max-age=31536000; includeSubDomains
X-Content-Type-Options: nosniff
X-Xss-Protection: 1; mode=block

{
    "allow_spontaneous_scopes": false,
    "application_type": "web",
    "rpt_as_jwt": false,
    "registration_client_uri": "https://yuriyz-relaxed-jawfish.gluu.info/jans-auth/restv1/register?client_id=7a29bf35-96ec-4bbd-a05c-15e1ff9f07cc",
    "tls_client_auth_subject_dn": "",
    "run_introspection_script_before_jwt_creation": false,
    "registration_access_token": "92a40113-b27c-43b9-bf96-a222fcfe1c9c",
    "client_id": "7a29bf35-96ec-4bbd-a05c-15e1ff9f07cc",
    "token_endpoint_auth_method": "client_secret_basic",
    "scope": "openid",
    "client_secret": "1af17da1-57a3-416b-a358-c84bb0ef0fad",
    "client_id_issued_at": 1702922353,
    "backchannel_logout_uri": "",
    "backchannel_logout_session_required": false,
    "client_name": "jans test app",
    "par_lifetime": 600,
    "spontaneous_scopes": [],
    "id_token_signed_response_alg": "RS256",
    "access_token_as_jwt": false,
    "grant_types": [
        "authorization_code",
        "implicit"
    ],
    "subject_type": "public",
    "authorization_details_types": ["demo_authz_detail"],
    "additional_token_endpoint_auth_methods": [],
    "keep_client_authorization_after_expiration": false,
    "require_par": false,
    "redirect_uris": [
        "https://client.example.com/cb2",
        "https://client.example.com/cb1",
        "https://yuriyz-relaxed-jawfish.gluu.info/jans-auth-rp/home.htm",
        "https://client.example.com/cb"
    ],
    "redirect_uris_regex": "",
    "additional_audience": [],
    "frontchannel_logout_session_required": false,
    "client_secret_expires_at": 0,
    "access_token_signing_alg": "RS256",
    "response_types": ["code"]
}

```




 



