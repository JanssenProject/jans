---
tags:
  - administration
  - recipes
  - open-banking
  - SSA
---

# SSA Configuration


The Janssen Server provides multiple configuration tools to perform these
tasks.

=== "Use Command-line"

    Use the command line to perform actions from the terminal. Learn how to 
    use Jans CLI [here](./config-tools/jans-cli/README.md) or jump straight to 
    the [Using Command Line](#using-command-line)

=== "Use Text-based UI"

    Use a fully functional text-based user interface from the terminal. 
    Learn how to use Jans Text-based UI (TUI) 
    [here](./config-tools/jans-tui/README.md) or jump straight to the
    [Using Text-based UI](#using-text-based-ui)

=== "Use REST API"

    Use REST API for programmatic access or invoke via tools like CURL or 
    Postman. Learn how to use Janssen Server Config API 
    [here](./config-tools/config-api/README.md) or Jump straight to the
    [Using Configuration REST API](#using-configuration-rest-api)

##  Using Command Line

In the Janssen Server, you can deploy and customize the SSA using the
command line. To get the details of Janssen command line operations relevant to
SSA Configuration, you can check the operations under `Ssa` 
task using the command below:

```bash title="Command"
jans cli -auth --info Ssa
```

```text title="Sample Output"
Operation ID: post-register-ssa
  Description: # Create SSA for the organization with `expiration` (optional).
----

**Security: Bearer Auth**

Provide your bearer token in the Authorization header when making requests to protected resources.

Example: `Authorization: Bearer {{your-access-token}}`

**Security: OAuth 2.0**

Scopes:
- `https://jans.io/auth/ssa.admin` - **SSA Admin**, You can create `SSA`.
  Parameters:
    org_id*: The `org_id` is used for organization identification.
    description: Description SSA.
    expiration: Expiration date. If this field is not sent, it will take days to expire, according to how it has been configured.
    software_id*: The `software_id` is used for software identification.
    software_roles*: List of string values, fixed value ["password", "notify"].
    grant_types*: Fixed value ["client_credentials"].
    one_time_use*: Defined whether the SSA will be used only once or can be used multiple times.
    rotate_ssa*: TODO - Will be used to rotate expiration of the SSA, currently is only saved as part of the SSA.
    lifetime: SSA lifetime in seconds.
Operation ID: get-ssa
  Description: # Get all SSA list based on `jti` or `org_id`.
----

**Security: Bearer Auth**

Provide your bearer token in the Authorization header when making requests to protected resources.

Example: `Authorization: Bearer {{your-access-token}}`

**Security: OAuth 2.0**

Scopes:
- `https://jans.io/auth/ssa.admin` - **SSA Adm
.in**, Retrieves all `SSA`
- `https://jans.io/auth/ssa.portal` - **SSA Portal**, Retrieves all `SSA`
- `https://jans.io/auth/ssa.developer` - **SSA Developer**, Retrieves the `SSA` created by the same client
  Parameters:
  jti: Unique Identifier [string]
  org_id: Organization ID [string]
Operation ID: delete-ssa
  Description: # Revokes existing active SSA based on `jti` or `org_id`
----

- `jti` - for delete only one SSA, the specified by `jti` 
- `org_id` - for delete all SSA of the specified organization.

**Security: Bearer Auth**

Provide your bearer token in the Authorization header when making requests to protected resources.

Example: `Authorization: Bearer {{your-access-token}}`

**Security: OAuth 2.0**

Scopes:
- `https://jans.io/auth/ssa.admin` - **SSA Admin**, You can revoke `SSA`.
  Parameters:
  jti: A unique identifier for the token, which can be used to prevent reuse of the token. [string]
  org_id: Delete all SSAs of the specified organization. [string]
Operation ID: validate-ssa
  Description: # Validates that a given SSA `jti` exists and is valid
----

This endpoint does not have any security, it is an open endpoint.
  Parameters:
  jti: Unique Identifier [string]
Operation ID: get-jwt-ssa
  Description: # Get JWT of SSA based on `jti`.
----

**Security: Bearer Auth**

Provide your bearer token in the Authorization header when making requests to protected resources.

Example: `Authorization: Bearer {{your-access-token}}`

**Security: OAuth 2.0**

Scopes:
- `https://jans.io/auth/ssa.admin` - **SSA Admin**, Retrieve `JWT` of `SSA`
  Parameters:
  jti: Unique Identifier. [string]

```



### Get all SSA

Use the `get-ssa` operation to retrieve the list of SSA configuration, 
run the following command line:

```bash title="Command"
jans cli -auth --operation-id get-ssa
```

```json title="Sample Output"
[
  {
    "ssa": {
      "software_id": "SW123456",
      "one_time_use": false,
      "grant_types": [
        "authorization_code"
      ],
      "org_id": "jans",
      "iss": "https://ossdhaval-hardy-tortoise.gluu.info",
      "description": "admin all access",
      "software_roles": [
        "admin"
      ],
      "exp": 3315119711,
      "iat": 1738319712,
      "jti": "92fd9751-bdd5-474e-be9c-ba38a582b54b",
      "rotate_ssa": false
    },
    "iss": "2000.cad384cc-2459-4e15-a205-2cb5fa69de35",
    "created_at": 1738319712,
    "expiration": 3315119711,
    "status": "ACTIVE"
  },
  {
    "ssa": {
      "software_id": "SW987654",
      "one_time_use": false,
      "grant_types": [
        "authorization_code",
        "refresh_token"
      ],
      "org_id": "gluu",
      "iss": "https://ossdhaval-hardy-tortoise.gluu.info",
      "description": "gluu admin",
      "software_roles": [
        "admin"
      ],
      "exp": 3315120318,
      "iat": 1738320318,
      "jti": "e94aae83-5a88-410c-a988-277919bdcdaf",
      "rotate_ssa": false
    },
    "iss": "2000.cad384cc-2459-4e15-a205-2cb5fa69de35",
    "created_at": 1738320318,
    "expiration": 3315120318,
    "status": "ACTIVE"
  }
]
```

### Get JWT of SSA based on `jti`


Provide to bearer token in the Authorization header when making requests to protected resources.


```bash title="Command"
jans cli -auth --operation-id get-jwt-ssa \
--url-suffix="jti:07754f4c-5368-489f-a7ae-2ce8a06087cd"
```

```json title="Sample Output"
{
  "ssa": "eyJraWQiOiJjb25uZWN0XzEwZTdkZGUxLTNlZWQtNGJkNi1hNmIzLTY0ZjhjNTA3OWJmN19zaWdfcnMyNTYiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJzb2Z0d2FyZV9pZCI6IlNXOTg3NjU0IiwiZ3JhbnRfdHlwZXMiOlsiYXV0aG9yaXphdGlvbl9jb2RlIiwicmVmcmVzaF90b2tlbiJdLCJvcmdfaWQiOiJnbHV1IiwiaXNzIjoiaHR0cHM6Ly9vc3NkaGF2YWwtaGFyZHktdG9ydG9pc2UuZ2x1dS5pbmZvIiwic29mdHdhcmVfcm9sZXMiOlsiYWRtaW4iXSwiZXhwIjozMzE1MTIwMzE4LCJpYXQiOjE3MzgzMjAzMTgsImp0aSI6ImU5NGFhZTgzLTVhODgtNDEwYy1hOTg4LTI3NzkxOWJkY2RhZiJ9.CRicNHnh5Ji1mh8UFRWw_-1KK1eGHL4Vm9AoeojwWxOSmdiO0INrlg5F2XXZhn3REHJ-8W2Db00_oCwKqP90_O5C29tyjAeKqxiN9tiHTZL84d56aJ4hnEQ_DKbqF6zHO0FrjBDQ6a7ev7WNL0kC2819DumiDXU4RfE0gvtUYkzh17pH7JrXw49Ggkc78VXDixGgCai5YP6AZmmW3XbTpacSlvWFzC-SgxXoqawTtrj0QRwkT0zddiWSJvCDrPFko8XgdKoj5cHG7QVRsHNU3i_tnMpyjAUBGHra4ZgyPqjrcBZTmvqLdWEJ3Kl5j4WF0NTMxWX0mjBKW-5o98RX2A"
}
```


### Delete SSA

We can delete the specific `SSA` through its `jti` using `delete-ssa` operation. 
For example, We can put `jti:07754f4c-5368-489f-a7ae-2ce8a06087cd` with `--url-suffix` to delete the session. 
The following command as below:



```bash 
jans cli -auth --operation-id delete-ssa \
--url-suffix="jti:07754f4c-5368-489f-a7ae-2ce8a06087cd"
```


To delete all SSA of the specified organization through its `org_id`. 
The following command as below:

```bash 
jans cli -auth --operation-id delete-ssa --url-suffix="org_id:gluu"
```



##  Using Text-based UI

Start TUI using the command below:

```bash title="Command"
jans tui
```

### SSA Screen

Navigate to `Auth Server` -> `SSA` to open the SSA screen as shown
in the image below.

![image](../../../assets/tui-ssa-screen.png)

To get the list of currently added SSA, bring the control to `Search` box
(using the tab key), and press `Enter`. Type the search string to search for
SSA with matching names.

Add a new SSA using the `Add SSA` button. It will open a dialogue where you can add an SSA detail and save it. as shown in the image below.

![image](../../../assets/tui-add-ssa.png)

To get the detail of any SSA, bring the control on that SSA (using the tab key), and press `v`. Also can export the detail.

![image](../../../assets/tui-ssa-detail.png)


## Using Configuration REST API

Janssen Server Configuration REST API exposes relevant endpoints for managing
and configuring SSA. Endpoint details are published in the [Swagger
document](./../../reference/openapi.md).