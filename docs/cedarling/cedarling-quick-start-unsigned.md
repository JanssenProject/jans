---
tags:
  - administration
  - lock
  - authorization / authz
  - Cedar
  - Cedarling
  - quick start
---

# Authorization Using The Cedarling

This quick start guide shows how to quickly test authorization of a user action
using the Cedarling. To do this, we need 3 things. 

1. Authorization policy
2. A request for user action
3. A running instance of the Cedarling

For `1` above, we will be using [Agama Lab policy designer](https://gluu.org/agama/authorization-policy-designer/) to quickly author
a [Cedar](https://www.cedarpolicy.com/) policy and a policy store.

For `2` and `3`, we will use [Jans Tarp](https://github.com/JanssenProject/jans/blob/main/demos/jans-tarp/README.md). Jans Tarp is an easy to install browser
plug-in that comes with embedded Cedarling instance (WASM). Jans Tarp also provides
user interface to build authorization and authentication requests for testing
purpose.

## Setup

- Install the Jans Tarp [on Chrome browser](https://github.com/JanssenProject/jans/blob/main/demos/jans-tarp/README.md#releases)

## Step-1: Create Cedar Policy and Schema

The Cedarling needs policies and a schema to authorize access. These are bundled in a *policy store* (a JSON file). To create one, use [Agama Lab policy designer guide](https://gluu.org/agama/authorization-policy-designer/) and steps below. As an end result, the policy
designer will publish your policy store to a GitHub repository. 

Follow this video walkthrough:

https://streamable.com/kvjcv6

### Schema setup

  - Open the [Agama Lab policy designer](https://cloud.gluu.org/agama-lab/dashboard/policy_store)
  and open the policy store
  - Click on `Schema` tab
    - Scroll down to `Entity Types` and click on the `+` sign
      - Entity type name: `SecretDocument`
      - Click Save
    - Scroll down to `Actions` and click on it  
      - Click on `Read` and select the pencil icon
      - Under Resources, add `SecretDocument`
      - Click Save
    - Scroll back up and click the green `Save` button on top of the entity list

### Policy setup

  In this guide, we will use a policy that grants access only to the user 
  entities with the `SupremeRuler` role:
  
  ```
  @id("allow_supreme_ruler")
  permit(
    principal is Jans::User,
    action,
    resource
  )
  when {
    principal has role &&
    principal.role.contains("SupremeRuler")
  };
  ```

  - Click on `Policies` tab in policy designer
  - Click `Add Policy` and then select `Text Editor`
  - Paste in the policy from above
  - Click Save

At the end, use the `Copy Link` button to copy the generated policy store URI 
for the next step.

## Step-2: Configure Tarp 

In this step, we will add the policy store details in the Jans Tarp that is
installed in the browser. The Cedarling instance embedded in the Tarp will
use the policy stored in this store to evaluate the authorization result.

1. Open Tarp installed in the Chrome browser 
2. Navigate to the `Cedarling` tab and click on `Add Configurations`
3. Paste the following configuration parameters as JSON. Make sure to update
the `<Policy Store URI>` value to point to your policy store

  ```
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
      "CEDARLING_MAPPING_USER": "Jans::User",
      "CEDARLING_MAPPING_WORKLOAD": "Jans::Workload",
      "CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED": [
        "HS256", "RS256"
      ],
      "CEDARLING_ID_TOKEN_TRUST_MODE": "none",
      "CEDARLING_LOCK": "disabled",
      "CEDARLING_LOCK_DYNAMIC_CONFIGURATION": "disabled",
      "CEDARLING_LOCK_LISTEN_SSE": "disabled"
  }
  ```

 Click `Save` to initialize Cedarling. This will start the Cedarling in Tarp,
 fetch and validate your policy store, and configure Cedarling to validate requests based on the User. 

## Step-3: Test the policy using cedarling 

Video walkthrough:

https://streamable.com/25wcb7

1. Go to Tarp, under `Cedarling` tab, click on `Cedarling Authz Form`
2. Input the following:

```JSON title="Principal"
    [
      {
        "type": "Jans::User",
        "id": "some_id",
        "sub": "some_sub",
        "role": ["SupremeRuler"]
      }
    ]
```

```JSON title="Actions"
Jans::Action::"Read"
```

```JSON title="Resource"
    {
      "entity_type": "resource",
      "type": "Jans::SecretDocument",
      "id": "some_id"
    }
```

The request is ready to be sent. To send the request to the Cedarling, 
click on `Cedarling Authz Request`

```JSON title="Sample Response"
    {
      "decision": true,
      "request_id": "019602e5-b148-7d0b-9d15-9d000c0d370b",
      ...
    }
```

The top-level `decision: true` confirms successful authorization.

Let's check a scenario where authorization is denied.

```
[
  {
    "type": "Jans::User",
    "id": "some_id",
    "sub": "some_sub",
    "role": [""]
  }
]
```
And click `Cedarling Authz Request` again. Cedarling will return a new result:

```
{
  "decision": false,
  ...
}
```

The top-level `decision: false` shows Cedarling denying authorization. 
