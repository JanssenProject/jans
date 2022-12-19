# Software Statement Assertion (SSA)

The SSA is a JSON Web Token (JWT) containing client metadata and some custom attributes.

You can check the following
[Swagger](https://gluu.org/swagger-ui/?url=https://raw.githubusercontent.com/JanssenProject/jans/replace-janssen-version/jans-auth-server/docs/swagger.yaml)
for more details of the endpoints.

## SSA Security

To call SSA services, a token of type `client_credentials` must be generated with the following scopes enabled:

- `https://jans.io/auth/ssa.admin` — Allows calling all SSA services.
- `https://jans.io/auth/ssa.portal` — Allows only call `Get SSA` service.
- `https://jans.io/auth/ssa.developer` — Allows only call `Get SSA`, but you can only filter ssa that have
- been created by the same client.

## Create a new SSA

Create `SSA` for the organization with `expiration` (optional).

### Request body description

| Field          | Detail                                                                                                                   | Optional |
|----------------|--------------------------------------------------------------------------------------------------------------------------|----------|
| org_id         | The "org_id" is used for organization identification.                                                                    | false    |
| description    | Describe ssa                                                                                                             | false    |
| software_id    | The "software_id" is used for software identification.                                                                   | false    |
| software_roles | List of string values, fixed value `["password", "notify"]`.                                                             | false    |
| grant_types    | Fixed value Fixed value `["client_credentials"]`.                                                                        | false    |
| expiration     | Expiration date. `(Default value: calculated based on global SSA settings)`                                              | true     |
| one_time_use   | Defined whether the SSA will be used only once or can be used multiple times. `(Default value: true)`                    | true     |
| rotate_ssa     | TODO - Will be used to rotate expiration of the SSA, currently is only saved as part of the SSA. `(Default value: true)` | true     |

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

### Example:

**Request:**

```
POST {{your-url}}/ssa
Content-Type: application/json
Authorization: Bearer {{your-token}}

{
    "org_id": 1,
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
        },
        "iss": "ed4d5f74-ce41-4180-aed4-54cffa974630",
        "created_at": 1668608851,
        "expiration": 1668608852
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
- `iss` — The "iss" is related to the client that created this SSA.
- `created_at` — Creation time.
- `expiration` — Expiration time.

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
      "jti": "c3eb1c16-be9b-4e96-974e-aea5e3cf95b0"
    },
    "iss": "ed4d5f74-ce41-4180-aed4-54cffa974630",
    "created_at": 1668608851,
    "expiration": 1668608852
  }
]
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

## SSA Global settings

The following fields are used to configure parameters globally.

```
"ssaConfiguration": {
    "ssaEndpoint": "{{your-url}}/ssa",
    "ssaCustomAttributes": [
        "myCustomAttr1",
        "myCustomAttr2"
    ],
    "ssaSigningAlg": "RS512",
    "ssaExpirationInDays": 30
}
```

- `ssaEndpoint` — Base endpoint for SSA.
- `ssaCustomAttributes` — List of custom attributes, which are received in the request when creating an SSA.
- `ssaSigningAlg` — Algorithm to sign the JWT that is returned after creating an SSA.
- `ssaExpirationInDays` — Expiration expressed in days, when an SSA is created and the expiration is not sent.

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
  - `rotateSsa` type `Boolean` — TODO - Will be used to rotate expiration of the SSA, currently is only saved as part of the SSA.
  - `clientDn` type `String` — Client's DN.
  - `customAttributes` type `Map<String, String>` — Contain additional fields, previously configured in the SSA global configuration.
  - `softwareId` type `String` — Is used for software identification.
  - `softwareRoles` type `List<String>` — List of string values, fixed value `["password", "notify"]`.
  - `grantTypes` type `List<String>` — Fixed value `["client_credentials"]`.
