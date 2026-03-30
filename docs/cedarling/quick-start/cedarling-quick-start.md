---
tags:
  - administration
  - lock
  - authorization / authz
  - Cedar
  - Cedarling
  - quick start
  - TBAC
---

# Authorization Using The Cedarling

This quick start guide shows how to test authorization of a user action using the Cedarling. We cover two authorization approaches:

1. **[RBAC using signed tokens (TBAC)](#implement-rbac-using-signed-tokens-tbac)** — the recommended starting point for most production deployments. Uses `authorize_multi_issuer` to validate real JWT tokens from trusted identity providers.
2. **[RBAC using application-asserted identity](#implement-rbac-using-application-asserted-identity)** — uses `authorize_unsigned` for scenarios where the application has already verified the principal. Useful for testing, custom auth flows, or service-to-service calls.

Not sure which to choose? See the [decision guide](../reference/cedarling-authz.md#which-authorization-method-should-i-use).

To see Cedarling authorization in action, we need 3 things:

1. A [policy store](../reference/cedarling-policy-store.md)
2. A request for user action
3. A running instance of the Cedarling

For the first prerequisite, we will use a ready demo policy.

For `2` and `3`, we will use [Janssen Tarp](https://github.com/JanssenProject/jans/blob/main/demos/janssen-tarp/README.md). Janssen Tarp is an easy-to-install browser plug-in that comes with an embedded Cedarling instance (WASM). Janssen Tarp also provides a user interface to build authorization and authentication requests for testing purposes.

## Implement RBAC using signed tokens (TBAC)

In this guide, we will use [Token-Based Access Control (TBAC)](../README.md#proof-based-authorization-token-based-access-control-tbac) to
implement role-based access control (RBAC). This is the recommended approach for most production deployments since it includes actual JWT validation against trusted issuers.

For better understanding of the TBAC flow, see the diagram below.

??? "TBAC Sequence diagram"

    ```mermaid
    sequenceDiagram
        title Cedarling and Tarp

        participant Browser
        participant Tarp
        participant Auth Server

        Browser->>Tarp: Enter Auth Server config
        Tarp->>Auth Server: Dynamic Client Registration
        Auth Server->>Tarp: Client ID and Secret
        Browser->>Tarp: Enter Cedarling bootstrap config
        Tarp->>Auth Server: GET /jans-auth/restv1/jwks
        Auth Server->>Tarp: JWKS
        Tarp->>Tarp: Initialize Cedarling
        Browser->>Tarp: Start authentication flow
        Tarp->>Auth Server: GET /jans-auth/restv1/authorize?...
        Auth Server->>Tarp: /callback?code=...
        Tarp->>Auth Server: GET /jans-auth/restv1/token
        Auth Server->>Tarp: {"access_token": <JWT>}

        Browser->>Tarp: Cedarling authorization flow(principal, action, resource, context)
        Tarp->>Browser: Cedarling Decision
    ```

### Prerequisites

- Install the Janssen Tarp [on Chrome or Firefox](https://github.com/JanssenProject/jans/blob/main/demos/janssen-tarp/README.md).
- An instance of Janssen Server or any other IDP with following configuration
  in place.
    - Allow dynamic client registration
    - Allow registered clients to request the `role` scope
    - Have a user with the `role` claim set to the value `Teacher` and
      return this claim in the userinfo token

### Step-1: Create Cedar Policy and Schema

For this guide, we have created a policy store in the [demo GitHub repository](https://github.com/JanssenProject/CedarlingQuickstart).

The policy store `tarpDemo` has many demo policies. We will focus on two: `allow_teacher_secretdocument` and `allow_student_read`. The first grants access to all actions and resources of type `SecretDocument` to the users with the `Teacher` role. The second allows only `Read` permission to students with the `Student` role to any resource. The two policies are as follows:

```cedar
@id("allow_teacher_secretdocument")
permit(
  principal,
  action,
  resource is Jans::SecretDocument
)
when {
  context has tokens.jans_userinfo_token &&
  context.tokens.jans_userinfo_token.hasTag("role") &&
  context.tokens.jans_userinfo_token.getTag("role").contains("Teacher")
};
```

```cedar
@id("allow_student_read")
permit (
  principal,
  action in [Jans::Action::"Read"],
  resource
)
when {
  context has tokens.jans_userinfo_token &&
  context.tokens.jans_userinfo_token.hasTag("role") &&
  context.tokens.jans_userinfo_token.getTag("role").contains("Student")
};
```

### Step-2 Update the IDP information

The policy store in the demo repository has information about the IDP that
released the tokens. You'll need to fork the demo repository and update the
trusted issuer configuration to point it to your IDP instance. Follow the steps below to do
this.

- Fork the [demo repository](https://github.com/JanssenProject/CedarlingQuickstart). While creating the fork, uncheck the
  `Copy the main branch only` checkbox.
- Update the `openid_configuration_endpoint` value in the trusted issuer file
  [`tarpDemo/trusted-issuers/jans.json`](https://github.com/JanssenProject/CedarlingQuickstart/blob/main/tarpDemo/trusted-issuers/jans.json) in the fork.
  Set it to the `.well-known/openid-configuration` endpoint of your IDP.

    For instance:

    ```json
    "openid_configuration_endpoint": "https://your-idp.example.com/.well-known/openid-configuration"
    ```

- Package the policy store as a `.cjar` archive (a ZIP file of the directory contents) and host it somewhere accessible via URL. The `CEDARLING_POLICY_STORE_URI` property requires either a `.cjar` URL or a legacy JSON URL — it does not support raw directory URLs.

    To create a `.cjar` archive from the `tarpDemo` directory:

    ```bash
    cd tarpDemo && zip -r ../tarpDemo.cjar .
    ```

    Then host the `.cjar` file (e.g., as a GitHub release asset, on a web server, or in cloud storage) and use its URL in the next step. For example:

    ```text
    https://github.com/<your-username>/CedarlingQuickstart/releases/download/v1/tarpDemo.cjar
    ```

??? info "Agama Lab: For authoring policies and managing the policy store"

    Alternatively, you can also use the Agama Lab For authoring policies and
    managing the policy stores.

    Agama Lab is a free web tool provided by Gluu. This tool makes it very
    easy to author, update policies and schema using an user interface. Follow
    the steps below to make changes to the policy that we are using above.

    - Go to the [CedarlingQuickstart repository](https://github.com/JanssenProject/CedarlingQuickstart) where the demo policy store is hosted
        - Click on `Fork`
        - **uncheck** the `Copy the main branch only` option
    - [Install Agama Lab app and allow access to forked repository](https://gluu.org/agama/how-to-integrate-agama%e2%80%90lab-github-app-with-your-github-account/)
    - Open the [Agama Lab policy designer](https://cloud.gluu.org/agama-lab/dashboard/policy_store).
    - Click on `Change Repository`.
    - Select the repository you want to use.
    - This will open the dashboard with two policy stores listed. Open the policy store named `tarpDemo`.
    - Now you can update the policy, trusted issuers and schema using the
      [policy designer](https://help.gluu.org/kb/article/34/authorization-policy-designer)

### Step-3: Configure Tarp with the policy store

We will now add the policy store details in the Janssen Tarp that is installed in the browser. The embedded Cedarling will use the policy from the store (from [step-1](#step-1-create-cedar-policy-and-schema)) to evaluate the authorization request.


1. Open the Janssen Tarp installed in the browser
2. Click `Add Client`. Use details below to add a new client.
    - Issuer: The hostname of your IDP
    - Expiry: The day after today
    - Scopes: `openid`, `profile`, `role`
3. Click `Register` to register a new client on your IDP.
4. Go to `Cedarling` tab and click `Add Configurations`
5. Select `JSON` configuration type and Paste the config as given below.
   Remember to replace `<Policy Store URI>` with
   the URI of your policy store:
   ```json
   {
     "CEDARLING_APPLICATION_NAME": "My App",
     "CEDARLING_POLICY_STORE_URI": "<Policy Store URI>",
     "CEDARLING_LOG_TYPE": "std_out",
     "CEDARLING_LOG_LEVEL": "INFO",
     "CEDARLING_USER_AUTHZ": "enabled",
     "CEDARLING_WORKLOAD_AUTHZ": "disabled",
     "CEDARLING_JWT_SIG_VALIDATION": "enabled",
     "CEDARLING_JWT_STATUS_VALIDATION": "disabled",
     "CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED": [
       "HS256", "RS256"
     ],
     "CEDARLING_POLICY_STORE_VALIDATE_CHECKSUM": false
   }
   ```

    !!! note
        `CEDARLING_JWT_STATUS_VALIDATION` is disabled here because most quick-start setups don't have a token status list endpoint configured. In production, enable it for full token revocation checking.

6. Click `Save` to initialize Cedarling.

This will start the embedded Cedarling instance in the Tarp.

### Step-4: Test the policy using the Cedarling

Since we are implementing TBAC, we have to authenticate the user first to get the tokens.

1. In Tarp, under `Authentication flow` tab, click the lightning icon to begin authentication
2. Use the following inputs to fill the form:
      - ACR: `basic`
      - Scopes: `openid`, `profile`, `role`
      - Check the `Display access and ID token after authentication` checkbox

3. Click the `Trigger auth flow` button
4. Login using username and password of a user who has the `Teacher` role assigned in the IDP
5. Click `Allow` on the consent screen
6. If the authentication is successful, Tarp will show you a page with token details. Store the userinfo token string.
7. Move to `Cedarling` tab and select `Cedarling Multi-Issuer Authz Form` tab
8. Use the details below as an input to this form:
      - Issuer-to-token mapping: For the policies we want only userinfo token is sufficient:
      ```json title="Token Mapping"
      [
          {
            "mapping": "Jans::Userinfo_token",
            "payload": "<token from step 6>"
          }
      ]
      ```
      - Action: `Jans::Action::"Write"`
      - Resource:
        ```json title="Resource"
        {
          "cedar_entity_mapping": {
            "entity_type": "Jans::SecretDocument",
            "id": "some_id"
          }
        }
        ```
      - Leave the `context` as default
9. Click `Cedarling Authz Request`
   ```json title="Sample Response"
   {
     ...
     "decision": true,
     "request_id": "019602f1-c964-7dbb-8a07-5b66b642e502"
   }
   ```

The top-level `decision: true` confirms successful authorization.

To test the negative case, we will do the following:

1. Modify the user in the IDP to have the `Student` role instead of `Teacher`
2. Logout from the Cedarling if you are logged in and rerun the authorization flow with the same parameters to obtain new tokens
3. Run signed authorization using the same inputs
4. The top-level `decision: false` confirms failed authorization. The first policy in the store allowed full access to users in the `Teacher` role, but the second policy only allowed `Read` access, which fails since our request was `Write` access.
5. Repeat the authorization with action `Jans::Action::"Read"` to confirm that the user has `Read` access but not `Write` access

---

## Implement RBAC using application-asserted identity

In this section, we will see how to implement role-based access control using the Cedarling when the information about the principal is supplied by the host application. 

This method uses `authorize_unsigned` — no JWT validation is performed. Use this when your application has already authenticated the principal through other means, or for testing and prototyping.

### Prerequisite

- Install the Janssen Tarp [on Chrome or Firefox](https://github.com/JanssenProject/jans/blob/main/demos/janssen-tarp/README.md).

### Step-1: Create the Cedar Policy and Schema

The Cedarling needs policies and a schema to authorize access. These are bundled in a _policy store_ (a JSON file). To aid in this quick start guide, we have already created a [policy store](https://github.com/JanssenProject/CedarlingQuickstart/releases/download/0.0.1/tarpUnsignedDemo.cjar) at
[quick start GitHub repository](https://github.com/JanssenProject/CedarlingQuickstart/tree/main).
We will use this policy store to allow/deny the incoming authorization request.

Since Tarp runs in a WASM environment, the policy store must be loaded via URL as a `.cjar` archive (a ZIP of the directory contents). To create your own:

```bash
cd tarpUnsignedDemo && zip -r ../tarpUnsignedDemo.cjar .
```

Host the `.cjar` file somewhere accessible (e.g., GitHub release, web server, cloud storage) and use its URL as the `<Policy Store URI>` in [Step-2](#step-2-configure-tarp).

??? tip "Agama Lab: For authoring policies and managing the policy store"

    Alternatively, you can also use the Agama Lab For authoring policies and
    managing the policy stores.

    Agama Lab is a free web tool provided by Gluu. This tool makes it very
    easy to author, update policies and schema using a web interface. Follow
    the steps below if you want to make changes to the demo policy that we are
    using above.

    - Go to the [CedarlingQuickstart repository](https://github.com/JanssenProject/CedarlingQuickstart) where the demo policy store is hosted
        - Click on `Fork`
        - **uncheck** the `Copy the main branch only` option
    - [Install Agama Lab app and allow access to forked repository](https://gluu.org/agama/how-to-integrate-agama%e2%80%90lab-github-app-with-your-github-account/)
    - Open the [Agama Lab policy designer](https://cloud.gluu.org/agama-lab/dashboard/policy_store).
    - Click on `Change Repository`.
    - Select your forked repository.
    - This will open the dashboard with two policy stores listed. Open the policy store named `tarpUnsignedDemo`.
    - Now you can update the policy and schema using the
      [policy designer](https://help.gluu.org/kb/article/34/authorization-policy-designer)
    - After making your changes, release the policy store using Agama Lab to create a CJAR archive. 
    - Go to the releases section of your fork, find your release and copy the URL to the `.cjar` file.

### Step-2: Configure Tarp

In this step, we will add the policy store details in the Janssen Tarp that is
installed in the browser. The Cedarling instance embedded in the Tarp will
use the policy stored in the store (from [Step-1](#step-1-create-the-cedar-policy-and-schema)) to evaluate the authorization result.

1. Open the Janssen Tarp installed in the browser
2. Navigate to the `Cedarling` tab and click on `Add Configurations`
3. Paste the following configuration parameters as JSON. Make sure to update the `<Policy Store URI>` value to point to your policy store CJAR archive.
   ```json
   {
       "CEDARLING_APPLICATION_NAME": "My App",
       "CEDARLING_POLICY_STORE_URI": "<Policy Store URI>",
       "CEDARLING_LOG_TYPE": "std_out",
       "CEDARLING_LOG_LEVEL": "INFO",
       "CEDARLING_USER_AUTHZ": "enabled",
       "CEDARLING_WORKLOAD_AUTHZ": "disabled",
       "CEDARLING_PRINCIPAL_BOOLEAN_OPERATION": {
         "===": [{"var": "Jans::User"}, "ALLOW"]
       },
       "CEDARLING_JWT_SIG_VALIDATION": "disabled",
       "CEDARLING_JWT_STATUS_VALIDATION": "disabled",
       "CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED": [
         "HS256", "RS256"
       ],
       "CEDARLING_POLICY_STORE_VALIDATE_CHECKSUM": false
   }
   ```

    !!! note
        `CEDARLING_PRINCIPAL_BOOLEAN_OPERATION` controls how per-principal decisions are combined in `authorize_unsigned`. Here it checks only the `Jans::User` principal. See [Principal Boolean Operations](../reference/cedarling-principal-boolean-operations.md) for details.

4. Click `Save` to initialize the Cedarling. The Cedarling will fetch and validate your policy store during the
initialization.

The Cedarling is ready to receive and evaluate authorization requests at this
stage.

### Step-3: Test authorization using the Cedarling


1. Go to Tarp's `Cedarling` tab, click on `Cedarling Unsigned Authz Form`
2. Input the following in respective input boxes:

   ```JSON title="Principal"
   [
     {
       "cedar_entity_mapping": {
         "entity_type": "Jans::User",
         "id": "some_id"
       },
       "sub": "some_sub",
       "role": [
         "Teacher"
       ]
     }
   ]
   ```

   ```JSON title="Actions"
   Jans::Action::"Read"
   ```

   ```JSON title="Resource"
   {
     "cedar_entity_mapping": {
       "entity_type": "Jans::SecretDocument",
       "id": "some_id"
     }
   }
   ```

Leave the `Context` blank.

The request is ready to be sent. Click `Cedarling Authz Request` to send the
request to the embedded Cedarling. After evaluation of the authorization
request, the Cedarling will respond with a JSON payload. Similar to what is
shown below.

```JSON title="Sample Response"
{
  "decision": true,
  "request_id": "019602e5-b148-7d0b-9d15-9d000c0d370b",
  ...
}
```

The top-level `decision: true` confirms successful authorization.

Let's check a scenario where authorization is denied. Remove the role from the
`Principal` entity and test the authorization.

```JSON title="Principal"
[
  {
    "cedar_entity_mapping": {
      "entity_type": "Jans::User",
      "id": "some_id"
    },
    "sub": "some_sub",
    "role": []
  }
]
```

And click `Cedarling Authz Request` again. Cedarling will return a new result:

```JSON title="Sample Response"
{
  "decision": false,
  ...
}
```

The top-level `decision: false` shows Cedarling denying authorization.
