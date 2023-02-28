---
"org_id": "test-org-id",
tags:

- administration
- auth-server
- SSA
- endpoint

---

# Software Statement Assertion (SSA)

Janssen Server provides SSA endpoint that enables management of SSAs. The SSA is a JSON Web Token (JWT) containing
client metadata and some custom attributes. Specification for SSAs has been outlined as part of
[Dynamic Client Registration Protocol](https://www.rfc-editor.org/rfc/rfc7591#section-2.3).

URL to access revocation endpoint on Janssen Server is listed in the response of Janssen Server's well-known
[configuration endpoint](./configuration.md) given below.

```text
https://janssen.server.host/jans-auth/.well-known/openid-configuration
```

`ssa_endpoint` claim in the response specifies the URL for revocation endpoint. By default, revocation endpoint
looks like below:

```
https://janssen.server.host/jans-auth/restv1/ssa
```

More information about request and response of the revocation endpoint can be found in
the OpenAPI specification
of [jans-auth-server module](https://gluu.org/swagger-ui/?url=https://raw.githubusercontent.com/JanssenProject/jans/vreplace-janssen-version/jans-auth-server/docs/swagger.yaml#/SSA).

## Disabling The Endpoint Using Feature Flag

`/ssa` endpoint can be enabled or disable
using [SSA feature flag](../../reference/json/feature-flags/janssenauthserver-feature-flags.md#ssa).
Use [Janssen Text-based UI(TUI)](../../config-guide/tui.md)
or [Janssen command-line interface](../../config-guide/jans-cli/README.md) to perform this task.

When using TUI, navigate via `Auth Server`->`Properties`->`enabledFeatureFlags` to screen below. From here, enable or
disable `SSA` flag as required.

![](../../../assets/image-tui-enable-components.png)

## Configuration Properties

SSA endpoint can be further configured using Janssen Server configuration property `ssaConfiguration`. When using
[Janssen Text-based UI(TUI)](../../config-guide/tui.md) to configure the properties,
navigate via `Auth Server`->`Properties` to update value for this property. This property take JSON configuration with
parameters as described below:

```
"ssaConfiguration": {
    "ssaEndpoint": "{{your-url}}/ssa",
    "ssaCustomAttributes": [
        "myCustomAttr1",
        "myCustomAttr2",
        ...
    ],
    "ssaSigningAlg": "RS512",
    "ssaExpirationInDays": 30
}
```

- `ssaEndpoint` — Base endpoint for SSA.
- `ssaCustomAttributes` — List of custom attributes, which are received in the request when creating an SSA.
- `ssaSigningAlg` — Algorithm to sign the JWT that is returned after creating an SSA.
- `ssaExpirationInDays` — Expiration expressed in days, when an SSA is created and the expiration is not sent.

## SSA Security

To call SSA services, a token of type `client_credentials` must be generated with the following scopes enabled:

- `https://jans.io/auth/ssa.admin` — Allows calling all SSA services.
- `https://jans.io/auth/ssa.portal` — Allows only call `Get SSA` service.
- `https://jans.io/auth/ssa.developer` — Allows only call `Get SSA`, but you can only filter ssa that have been created
  by the same client.

## Create a new SSA

Create `SSA` for the organization with `expiration` (optional).

### Request body description

| Field          | Detail                                                                                                                   | Optional |
|----------------|--------------------------------------------------------------------------------------------------------------------------|----------|
| org_id         | The "org_id" is used for organization identification.                                                                    | false    |
| description    | Describe SSA                                                                                                             | false    |
| software_id    | The "software_id" is used for software identification.                                                                   | false    |
| software_roles | List of string values, fixed value `["password", "notify"]`.                                                             | false    |
| grant_types    | Fixed value Fixed value `["client_credentials"]`.                                                                        | false    |
| expiration     | Expiration date. `(Default value: calculated based on global SSA settings)`                                              | true     |
| one_time_use   | Defined whether the SSA will be used only once or can be used multiple times. `(Default value: true)`                    | true     |
| rotate_ssa     | TODO - Will be used to rotate expiration of the SSA, currently is only saved as part of the SSA. `(Default value: true)` | true     |

**Note:** You can add more `custom attributes` in the request, (you must have previously configured in the SSA global
configuration).
It should be clarified that these values are persisted in the database and are not returned in the SSA JWT.

Example:

```
{
  "org_id": "your-org-id",
  "description": "your description"
  ...
  
  org_id: "Your org_id",
  "myCustomAttr1": "Your value custom attr 1", 
  "myCustomAttr2": "Your value custom attr 2",
  ...
} 
```

### Response description

Returned SSA is a JWT, containing the following structure:

```
{
    "ssa": "eyJraWQiOiI1NTk3MGFkZS00M2MwLTQ4YWMtODEyZi0yZTY1MzhjMTEyN2Zfc2lnX3JzNTEyIiwidHlwIjoiand0IiwiYWxnIjoiUlM1MTIifQ.eyJzb2Z0d2FyZV9pZCI6ImdsdXUtc2Nhbi1hcGkiLCJncmFudF90eXBlcyI6WyJjbGllbnRfY3JlZGVudGlhbHMiXSwib3JnX2lkIjoxLCJpc3MiOiJodHRwczovL2phbnMubG9jYWxob3N0Iiwic29mdHdhcmVfcm9sZXMiOlsicGFzc3d1cmQiXSwiZXhwIjoxNjY4NjA5MDA1LCJpYXQiOjE2Njg2NDE5NjcsImp0aSI6ImU4OWVjYTQxLTM0ODUtNDUxNi1hMTYyLWZiODYyNjJhYmFjMyJ9.jRgh8_aiwMTJxeT9cwfup9QP9LBc6gQstvabCzUOJvELnzosxiNJHeU2mrvavaNK6BGvs_lbNjODVDeetGCD48_F2ay9r8qmo-f3GPzdzcJozKgfzonSkAE5Ran9LKcQQJpVc1rDYcV2xYiJLJ6FSuvnoClkDEE1tXysxshLPs-GXOZE7rD8XUXzezuxZWUE1jXwA-EFajoat8CP6QulHGxlcn_sKIhawhGODxJPz4Pf3jgeZVLG_7HfRJgxNiKcdzQIxnkbdpuS-0Q4-oc5yntsXhFhn31Pa3vGsiPPH9f3ndL2ZZKk3xCgyImLDJuGaxXg-qEVoIG4zNWNHMUNUQ"
}
```

**Header**

- `alg` — The signature algorithm which used `RS256`.
- `typ` — The type which used.
- `kid` — The key identification `gluu-scan-api-rs256-ssa-signature-key`.

**Payload**

- `iat` — The time the JWT was created.
- `iss` — The "iss" (issuer) claim identifies the principal that issued the JWT.
- `jti` — The "jti" (JWT ID) claim provides a unique identifier for the JWT.
- `software_id` — The "software_id" is used for software identification.
- `org_id` — The "org_id" is used for organization identification.
- `software_roles` — List of string values, fixed value `["password", "notify"]`.
- `grant_types` — Fixed value `["client_credentials"]`.
- `exp` — Expiration Time.
- `myCustom1, myCustom2, ...`: if you have custom attributes, they will be displayed here.

### Example:

**Request:**

```
POST {{your-url}}/ssa
Content-Type: application/json
Authorization: Bearer {{your-token}}

{
    "description": "test",
    "software_id": "gluu-scan-api",
    "software_roles": [
        "password"
    ],
    "grant_types": [
        "client_credentials"
    ],
    "expiration": 1668609005,
    "one_time_use": true,
    "rotate_ssa": true
}
```

**Response:**

```
HTTP/1.1 201 Created
Date: Wed, 16 Nov 2022 23:39:27 GMT
Server: Apache/2.4.52 (Ubuntu)
X-Xss-Protection: 1; mode=block
X-Content-Type-Options: nosniff
Strict-Transport-Security: max-age=31536000; includeSubDomains
Cache-Control: no-store
Content-Type: application/json
Pragma: no-cache
Content-Length: 757
Keep-Alive: timeout=5, max=100
Connection: Keep-Alive

{
    "ssa": "eyJraWQiOiI1NTk3MGFkZS00M2MwLTQ4YWMtODEyZi0yZTY1MzhjMTEyN2Zfc2lnX3JzNTEyIiwidHlwIjoiand0IiwiYWxnIjoiUlM1MTIifQ.eyJzb2Z0d2FyZV9pZCI6ImdsdXUtc2Nhbi1hcGkiLCJncmFudF90eXBlcyI6WyJjbGllbnRfY3JlZGVudGlhbHMiXSwib3JnX2lkIjoxLCJpc3MiOiJodHRwczovL2phbnMubG9jYWxob3N0Iiwic29mdHdhcmVfcm9sZXMiOlsicGFzc3d1cmQiXSwiZXhwIjoxNjY4NjA5MDA1LCJpYXQiOjE2Njg2NDE5NjcsImp0aSI6ImU4OWVjYTQxLTM0ODUtNDUxNi1hMTYyLWZiODYyNjJhYmFjMyJ9.jRgh8_aiwMTJxeT9cwfup9QP9LBc6gQstvabCzUOJvELnzosxiNJHeU2mrvavaNK6BGvs_lbNjODVDeetGCD48_F2ay9r8qmo-f3GPzdzcJozKgfzonSkAE5Ran9LKcQQJpVc1rDYcV2xYiJLJ6FSuvnoClkDEE1tXysxshLPs-GXOZE7rD8XUXzezuxZWUE1jXwA-EFajoat8CP6QulHGxlcn_sKIhawhGODxJPz4Pf3jgeZVLG_7HfRJgxNiKcdzQIxnkbdpuS-0Q4-oc5yntsXhFhn31Pa3vGsiPPH9f3ndL2ZZKk3xCgyImLDJuGaxXg-qEVoIG4zNWNHMUNUQ"
}
```

## Get SSA

Get existing active SSA based on `jti` or `org_id`.

### Query Parameters

- `jti` — Unique identifier
- `org_id` — Organization ID

### Response description

```
[
    {
        "ssa": {
            "jti": "c3eb1c16-be9b-4e96-974e-aea5e3cf95b0"
            "org_id": 1,
            "software_id": "gluu-scan-api",
            "software_roles": [
                "password"
            ],
            "grant_types": [
                "client_credentials"
            ],
            "iss": "https://jans.localhost",
            "exp": 1668608852,
            "iat": 1668608851,
            "description: "test description",
            "one_time_use": true,
            "rotate_ssa": false
        },
        "iss": "ed4d5f74-ce41-4180-aed4-54cffa974630",
        "created_at": 1668608851,
        "expiration": 1668608852,
        "status": "ACTIVE"
    }
]
```

- SSA
    - `jti` — The "jti" (JWT ID) claim provides a unique identifier for the JWT.
    - `org_id` — The "org_id" is used for organization identification.
    - `software_id` — The "software_id" is used for software identification.
    - `software_roles` — List of string values, fixed value `["password", "notify"]`.
    - `grant_types` — Fixed value `["client_credentials"]`.
    - `iss` — The "iss" (issuer) claim identifies the principal that issued the JWT.
    - `exp` — Expiration time.
    - `iat` — Creation time.
    - `description` — Describe SSA.
    - `one_time_use` — Defined whether the SSA will be used only once or can be used multiple times.
    - `rotate_ssa` — TODO - Will be used to rotate expiration of the SSA, currently is only saved as part of the SSA.
    - `myCustom1, myCustom2, ...` — if you have custom attributes, they will be displayed here.
- `iss` — The "iss" is related to the client that created this SSA.
- `created_at` — Creation time.
- `expiration` — Expiration time.
- `status` — SSA status (`ACTIVE`, `USED`, `EXPIRED` or `REVOKED`).

### Example:

**Request:**

Get SSA using `jti`.

```
GET {{your-url}}/ssa?jti={{your-jti}}
Content-Type: application/json
Authorization: Bearer {{your-token}}
```

Get SSA using `org_id`.

```
GET {{your-url}}/ssa?org_id={{your-org_id}}
Content-Type: application/json
Authorization: Bearer {{your-token}}
```

**Response:**

```
HTTP/1.1 200 OK
Date: Wed, 16 Nov 2022 15:27:35 GMT
Server: Apache/2.4.52 (Ubuntu)
X-Xss-Protection: 1; mode=block
X-Content-Type-Options: nosniff
Strict-Transport-Security: max-age=31536000; includeSubDomains
Cache-Control: no-store
Content-Type: application/json
Pragma: no-cache
Content-Length: 432
Keep-Alive: timeout=5, max=100
Connection: Keep-Alive

[
  {
    "ssa": {
      "software_id": "gluu-scan-api",
      "grant_types": [
        "client_credentials"
      ],
      "org_id": 1,
      "iss": "https://jans.localhost",
      "software_roles": [
        "passwurd"
      ],
      "exp": 1668608852,
      "iat": 1668608851,
      "jti": "c3eb1c16-be9b-4e96-974e-aea5e3cf95b0",
      "description: "test description",
      "one_time_use": true,
      "rotate_ssa": false
    },
    "iss": "ed4d5f74-ce41-4180-aed4-54cffa974630",
    "created_at": 1668608851,
    "expiration": 1668608852,
    "status": "ACTIVE"
  }
]
```

## Get JWT SSA

Get existing active SSA based on `jti`.

### Query Parameters

- `jti` — Unique identifier

### Response description

Returned SSA is a JWT, containing the following structure:

```
{
    "ssa": "eyJraWQiOiI1NTk3MGFkZS00M2MwLTQ4YWMtODEyZi0yZTY1MzhjMTEyN2Zfc2lnX3JzNTEyIiwidHlwIjoiand0IiwiYWxnIjoiUlM1MTIifQ.eyJzb2Z0d2FyZV9pZCI6ImdsdXUtc2Nhbi1hcGkiLCJncmFudF90eXBlcyI6WyJjbGllbnRfY3JlZGVudGlhbHMiXSwib3JnX2lkIjoxLCJpc3MiOiJodHRwczovL2phbnMubG9jYWxob3N0Iiwic29mdHdhcmVfcm9sZXMiOlsicGFzc3d1cmQiXSwiZXhwIjoxNjY4NjA5MDA1LCJpYXQiOjE2Njg2NDE5NjcsImp0aSI6ImU4OWVjYTQxLTM0ODUtNDUxNi1hMTYyLWZiODYyNjJhYmFjMyJ9.jRgh8_aiwMTJxeT9cwfup9QP9LBc6gQstvabCzUOJvELnzosxiNJHeU2mrvavaNK6BGvs_lbNjODVDeetGCD48_F2ay9r8qmo-f3GPzdzcJozKgfzonSkAE5Ran9LKcQQJpVc1rDYcV2xYiJLJ6FSuvnoClkDEE1tXysxshLPs-GXOZE7rD8XUXzezuxZWUE1jXwA-EFajoat8CP6QulHGxlcn_sKIhawhGODxJPz4Pf3jgeZVLG_7HfRJgxNiKcdzQIxnkbdpuS-0Q4-oc5yntsXhFhn31Pa3vGsiPPH9f3ndL2ZZKk3xCgyImLDJuGaxXg-qEVoIG4zNWNHMUNUQ"
}
```

**Header**

- `alg` — The signature algorithm which used `RS256`.
- `typ` — The type which used.
- `kid` — The key identification `gluu-scan-api-rs256-ssa-signature-key`.

**Payload**

- `iat` — The time the JWT was created.
- `iss` — The "iss" (issuer) claim identifies the principal that issued the JWT.
- `jti` — The "jti" (JWT ID) claim provides a unique identifier for the JWT.
- `software_id` — The "software_id" is used for software identification.
- `org_id` — The "org_id" is used for organization identification.
- `software_roles` — List of string values, fixed value `["password", "notify"]`.
- `grant_types` — Fixed value `["client_credentials"]`.
- `exp` — Expiration Time.
- `myCustom1, myCustom2, ...`: if you have custom attributes, they will be displayed here.

### Example:

**Request:**

```
GET {{your-url}}/ssa/jwt?jti={{your-jti}}
Content-Type: application/json
Authorization: Bearer {{your-token}}
```

**Response:**

```
HTTP/1.1 201 Created
Date: Wed, 16 Nov 2022 23:39:27 GMT
Server: Apache/2.4.52 (Ubuntu)
X-Xss-Protection: 1; mode=block
X-Content-Type-Options: nosniff
Strict-Transport-Security: max-age=31536000; includeSubDomains
Cache-Control: no-store
Content-Type: application/json
Pragma: no-cache
Content-Length: 757
Keep-Alive: timeout=5, max=100
Connection: Keep-Alive

{
    "ssa": "eyJraWQiOiI1NTk3MGFkZS00M2MwLTQ4YWMtODEyZi0yZTY1MzhjMTEyN2Zfc2lnX3JzNTEyIiwidHlwIjoiand0IiwiYWxnIjoiUlM1MTIifQ.eyJzb2Z0d2FyZV9pZCI6ImdsdXUtc2Nhbi1hcGkiLCJncmFudF90eXBlcyI6WyJjbGllbnRfY3JlZGVudGlhbHMiXSwib3JnX2lkIjoxLCJpc3MiOiJodHRwczovL2phbnMubG9jYWxob3N0Iiwic29mdHdhcmVfcm9sZXMiOlsicGFzc3d1cmQiXSwiZXhwIjoxNjY4NjA5MDA1LCJpYXQiOjE2Njg2NDE5NjcsImp0aSI6ImU4OWVjYTQxLTM0ODUtNDUxNi1hMTYyLWZiODYyNjJhYmFjMyJ9.jRgh8_aiwMTJxeT9cwfup9QP9LBc6gQstvabCzUOJvELnzosxiNJHeU2mrvavaNK6BGvs_lbNjODVDeetGCD48_F2ay9r8qmo-f3GPzdzcJozKgfzonSkAE5Ran9LKcQQJpVc1rDYcV2xYiJLJ6FSuvnoClkDEE1tXysxshLPs-GXOZE7rD8XUXzezuxZWUE1jXwA-EFajoat8CP6QulHGxlcn_sKIhawhGODxJPz4Pf3jgeZVLG_7HfRJgxNiKcdzQIxnkbdpuS-0Q4-oc5yntsXhFhn31Pa3vGsiPPH9f3ndL2ZZKk3xCgyImLDJuGaxXg-qEVoIG4zNWNHMUNUQ"
}
```

## Validate SSA

Validate existing active SSA based on `jti`

### Header Parameters

- jti — Unique identifier

### Response description

Method returns status 200 with an empty body if the corresponding SSA exists and is active.

### Example:

```
HEAD {{your-url}}/ssa
jti: {{your-jti}}
```

```
HTTP/1.1 200 OK
Date: Wed, 16 Nov 2022 21:49:11 GMT
Server: Apache/2.4.52 (Ubuntu)
X-Xss-Protection: 1; mode=block
X-Content-Type-Options: nosniff
Strict-Transport-Security: max-age=31536000; includeSubDomains
Cache-Control: no-store
Content-Type: application/json
Pragma: no-cache
Keep-Alive: timeout=5, max=100
Connection: Keep-Alive

<Response body is empty>
```

## Revoke SSA

Revoke existing active SSA based on `jti` or `org_id`.

### Query Parameters

- `jti` — for revoke only one SSA, the specified by jti
- `org_id` — for revoke all SSA of the specified organization.

### Response description

Method returns status 200 with an empty body if all required SSAs were revoked correctly.

### Example:

**Request:**

Revoke using `jti`.

```
DELETE {{your-url}}/ssa?jti={{your-jti}}
Authorization: Bearer {{your-token}}
```

Revoke using `org_id`.

```
DELETE {{your-url}}/ssa?org_id={{your-org_id}}
Authorization: Bearer {{your-token}}
```

**Response:**

```
HTTP/1.1 200 OK
Date: Wed, 16 Nov 2022 23:15:56 GMT
Server: Apache/2.4.52 (Ubuntu)
X-Xss-Protection: 1; mode=block
X-Content-Type-Options: nosniff
Strict-Transport-Security: max-age=31536000; includeSubDomains
Cache-Control: no-store
Content-Type: application/json
Pragma: no-cache
Content-Length: 0
Keep-Alive: timeout=5, max=100
Connection: Keep-Alive

<Response body is empty>
```

## SSA Custom Script

The custom script will allow us to modify the SSA process.
[SSA Custom Script](https://github.com/JanssenProject/jans/blob/main/jans-linux-setup/jans_setup/static/extension/ssa_modify_response/ssa_modify_response.py)

- Modify the JWT returned by the creation SSA web service.
- Modify the list returned by the get SSA web service.
- Run a process after revoking an SSA.

### Create method

This method is executed after having generated the jwt that the service will return.

In the following example, new fields is added to the header and payload of the JWT.

```
def create(self, jsonWebResponse, context):
    print "Modify ssa response script. Modify idToken: %s" % jsonWebResponse
    
    jsonWebResponse.getHeader().setClaim("custom_header_name", "custom_header_value")
    jsonWebResponse.getClaims().setClaim("custom_claim_name", "custom_claim_value")
    
    print "Modify ssa response script. After modify idToken: %s" % jsonWebResponse
    return True
```

#### Parameters

- `jsonWebResponse` — JWT with SSA structure using `io.jans.as.model.jwt.Jwt` class
- `context` — Contains, SSA global configuration class, client, execution context, etc.

### Get method

This method is executed after having generated the SSA list that will be returned by the service.

```
def get(self, jsonArray, context):
    print "Modify ssa response script. Modify get ssa list: %s" % jsonArray
    return True
```

#### Parameters

- `jsonArray` — Contains SSA list using `org.json.JSONArray` class
- `context` — Contains, SSA global configuration class, client, execution context, etc.

### Revoke method

This method is executed after the SSA list has been revoked.

```
def revoke(self, ssaList, context):
    print "Modify ssa response script. Modify revoke ssaList: %s" % ssaList
    return True
```

#### Parameters

- `ssaList` — SSA revoked list.
- `context` — Contains, SSA global configuration class, client, execution context, etc.

## SSA Class structure

The SSA entity contains the following fields:

- `id` type `String` — Unique class identifier, and is used as the `jti` identifier for the `JWT`.
- `orgId` type `String` — Organization ID.
- `description` type `String` — SSA Description.
- `state` type enum `SsaState` — Contains the following SSA status values (`ACTIVE`, `EXPIRED`, `REVOKED`, `USED`).
- `creationDate` type `Date` — SSA Creation date.
- `creatorId` type `String` — Client that created SSA.
- `creatorType` type enum `CreatorType` — Contains the following CreatorType values (`NONE`, `CLIENT`, `USER`, `AUTO`).
- `ttl` type `Integer` — SSA lifetime in milliseconds.
- `atributes` type class `SsaAtributes`
    - `oneTimeUse` type `Boolean` — Whether the SSA will be single use.
    - `rotateSsa` type `Boolean` — TODO - Will be used to rotate expiration of the SSA, currently is only saved as part
      of the SSA.
    - `clientDn` type `String` — Client's DN.
    - `customAttributes` type `Map<String, String>` — Contain additional fields, previously configured in the SSA global
      configuration.
    - `softwareId` type `String` — Is used for software identification.
    - `softwareRoles` type `List<String>` — List of string values, fixed value `["password", "notify"]`.
    - `grantTypes` type `List<String>` — Fixed value `["client_credentials"]`.
