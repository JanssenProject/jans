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

This quick start guide shows how to quickly test authorization of a user action
using the Cedarling. We will be using [the application asserted identity approach](./README.md#token-based-access-control-tbac-v-application-asserted-identity) 
in this guide to implement role based access control(RBAC). Refer to [this section](#implement-tbac-using-cedarling) to understand how to use Cedarling
with TBAC approach.


To see authorization using Cedarling in action, we need 3 things.

1. An authorization policy
2. A request for user action
3. A running instance of the Cedarling

For `1` above, we will use a ready demo policy.

For `2` and `3`, we will use [Janssen Tarp](https://github.com/JanssenProject/jans/blob/main/demos/janssen-tarp/README.md). Janssen Tarp is an easy to install browser plug-in that comes with embedded Cedarling instance (WASM). Janssen Tarp also provides user interface to build authorization and authentication requests for testing purpose.

## Prerequisite

- Install the Janssen Tarp [on Chrome or Firefox](https://github.com/JanssenProject/jans/blob/main/demos/janssen-tarp/README.md).

## Implement RBAC using Cedarling

In this section, we will see how to implement the role-based access control
using the Cedarling when
the information about the principal is supplied by the host application. We can
call this approach [the application asserted identity approach](./README.md#token-based-access-control-tbac-v-application-asserted-identity).

### Step-1: Create the Cedar Policy and Schema

The Cedarling needs policies and a schema to authorize access. These are bundled in a _policy store_ (a JSON file). To aid in this quick start guide, we have already created a 
[policy store](https://raw.githubusercontent.com/JanssenProject/CedarlingQuickstart/refs/heads/main/6d9f73b2d44ad4e7aa8f1182cde9f72dcbaa244f4327.json) at
[quick start GitHub repository](https://github.com/JanssenProject/CedarlingQuickstart/tree/main).
We will use this policy store to allow/deny the incoming authorization request.

??? tip "Agama Lab: For authoring policies and managing the policy store"

    Alternatively, you can also use the Agama Lab For authoring policies and 
    managing the policy stores. 

    Agama Lab is a free web tool provided by Gluu. This tool makes it very 
    easy to author, update policies and schema using a web interface. Follow
    the steps below if you want to make changes to the demo policy that we are 
    using above.

    - Go to the [CedarlingQuickstart repository](https://github.com/JanssenProject/CedarlingQuickstart) where the demo policy store is hosted
        - Click on `Fork`
        - **uncheck** the `Copy the master branch only` option 
    - [Install Agama Lab app and allow access to forked repository](https://gluu.org/agama/how-to-integrate-agama%e2%80%90lab-github-app-with-your-github-account/)
    - Open the [Agama Lab policy designer](https://cloud.gluu.org/agama-lab/dashboard/policy_store).
    - Click on `Change Repository`.
    - Select the option `Manually Type Repository URL`.
    - Paste the URL of your forked repository, then click the `Select` button.
    - This will open the dashboard with two policy stores listed. Open the policy store named `tarpUnsignedDemo`.
    - Now you can update the policy and schema using the 
      [policy designer](https://help.gluu.org/kb/article/34/authorization-policy-designer)

### Step-2: Configure Tarp

In this step, we will add the policy store details in the Janssen Tarp that is
installed in the browser. The Cedarling instance embedded in the Tarp will
use the policy stored in the store (from [Step-1](#step-1-create-the-cedar-policy-and-schema)) to evaluate the authorization result.

1. Open the Janssen Tarp installed in the browser
2. Navigate to the `Cedarling` tab and click on `Add Configurations`
3. Paste the following configuration parameters as JSON. Make sure to update the `<Policy Store URI>` value to point to your policy store
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
       "CEDARLING_JWT_SIG_VALIDATION": "enabled",
       "CEDARLING_JWT_STATUS_VALIDATION": "disabled",
       "CEDARLING_MAPPING_USER": "Jans::User",
       "CEDARLING_MAPPING_WORKLOAD": "Jans::Workload",
       "CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED": [
         "HS256", "RS256"
       ],
       "CEDARLING_ID_TOKEN_TRUST_MODE": "never"
   }
   ```
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


## Implement RBAC using signed tokens (TBAC)

In this guide, we will use [Token-Based Access Control (TBAC)](./README.md#token-based-access-control-tbac-v-application-asserted-identity) to 
implement role-based access control (RBAC). 

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
- Instance of Janssen Server or any other IDP with following configuration
  in place.
    - Allow dynamic client registration
    - Allow registered clients to request the `role` scope
    - Have a user with the `role` claim set to the value `Teacher` and 
      return this claim in the userinfo token

### Step-1: Create Cedar Policy and Schema

For this guide, we have created a policy store in the 
[demo GitHub repository](https://github.com/JanssenProject/CedarlingQuickstart). 

The policy store has two policies. The first grants access to all actions and all resources to the users with the `Teacher` role. The second allows only `Read` permission to students with the `Student` role to any resource. The two policies are as follows:

```cedar
@id("allow_teacher")
permit(
  principal in Jans::Role::"Teacher",
  action,
  resource
);
```

```cedar
@id("allow_student_read")
permit(
  principal in Jans::Role::"Student",
  action in [Jans::Action::"Read"],
  resource
)
```

### Step-2 Update the IDP information

The policy store in the demo repository has information about the IDP that 
released the tokens. You'll need to fork the demo repository and update the 
policy store to point it to your IDP instance. Follow the steps below to do 
this.

- Fork the [demo repository](https://github.com/JanssenProject/CedarlingQuickstart). While creating the fork, uncheck the
  `Copy the main branch only` checkbox.
- Update the `openid_configuration_endpoint` key in the policy store JSON file
  named `449805c83e13f332b1b35eac6ffa93187fbd1c648085.json` in the fork. 
  Set the value to 
  the `.well-known/openid-configuration` endpoint of your IDP. 
  
    For instance:
  
    ```
    "openid_configuration_endpoint": "https://test-jans.gluu.info/.well-known/openid-configuration"
    ```

- Copy the link of the policy store file's **raw** content. This is the same file that you edited in the step above.
  The URI would be similar to the shown below. This URI will be used in the next step when we configure Tarp.

    ```
    https://raw.githubusercontent.com/JanssenProject/CedarlingQuickstart/refs/heads/main/449805c83e13f332b1b35eac6ffa93187fbd1c648085.json
    ```

??? info "Agama Lab: For authoring policies and managing the policy store"
    
    Alternatively, you can also use the Agama Lab For authoring policies and 
    managing the policy stores. 

    Agama Lab is a free web tool provided by Gluu. This tool makes it very 
    easy to author, update policies and schema using an user interface. Follow
    the steps below to make changes to the policy that we are using above.

    - Go to the [CedarlingQuickstart repository](https://github.com/JanssenProject/CedarlingQuickstart) where the demo policy store is hosted
        - Click on `Fork`
        - **uncheck** the `Copy the master branch only` option 
    - [Install Agama Lab app and allow access to forked repository](https://gluu.org/agama/how-to-integrate-agama%e2%80%90lab-github-app-with-your-github-account/)
    - Open the [Agama Lab policy designer](https://cloud.gluu.org/agama-lab/dashboard/policy_store).
    - Click on `Change Repository`.
    - Select the option `Manually Type Repository URL`.
    - Paste the URL of your forked repository, then click the `Select` button.
    - This will open the dashboard with two policy stores listed. Open the policy store named `tarpDemo`.
    - Now you can update the policy and schema using the 
      [policy designer](https://help.gluu.org/kb/article/34/authorization-policy-designer)        




### Step-3: Configure Tarp with the policy store

We will now add the policy store details in the Janssen Tarp that is
installed in the browser. The Cedarling instance embedded in the Tarp will
use the policy from the store (from [step-1](#step-1-create-cedar-policy-and-schema)) 
to evaluate the authorization request.


1. Open Tarp installed in the Chrome browser
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
           "CEDARLING_PRINCIPAL_BOOLEAN_OPERATION": {
             "===": [{"var": "Jans::User"}, "ALLOW"]
           },
           "CEDARLING_JWT_SIG_VALIDATION": "enabled",
           "CEDARLING_JWT_STATUS_VALIDATION": "disabled",
           "CEDARLING_MAPPING_USER": "Jans::User",
           "CEDARLING_MAPPING_WORKLOAD": "Jans::Workload",
           "CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED": [
             "HS256", "RS256"
           ],
           "CEDARLING_ID_TOKEN_TRUST_MODE": "never"
         }
   ```
6. Click `Save` to initialize Cedarling.

This will start the embedded Cedarling instance in the Tarp.

### Step-4: Test the policy using the Cedarling

Since we are implementing TBAC, we have to authenticate the user first to get the tokens.

1. In Tarp, under `Authentication flow` tab, click the ⚡ icon to begin authentication
2. Use the following inputs to fill the form:
      - ACR: `basic`
      - Scopes: `openid`, `profile`, `role`
      - Check the `Display access and ID token after authentication` checkbox
   
3. Click the `Trigger auth flow` button
3. Login using username and password of a user who has the `Teacher` role assigned in the IDP
4. Click `Allow` on the consent screen
5. If the authentication is successful, Tarp will show you a page with token details 
6. Move to `Cedarling` tab and select `Cedarling Signed Authz Form` tab
7. Use the details below as an input to this form:
      - Principal: select all 3 tokens. In TBAC approach here, we are passing tokens (i.e signed JWTs) in place of JSON string.
      - Action: `Jans::Action::"Write"`
      - Resource:
        ```json
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
