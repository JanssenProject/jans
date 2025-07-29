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

For `1` above, we will be using [Agama Lab policy designer](https://gluu.org/agama/authorization-policy-designer/) to quickly set up a [Cedar](https://www.cedarpolicy.com/) policy and a policy store.

For `2` and `3`, we will use [Janssen Tarp](https://github.com/JanssenProject/jans/blob/main/demos/janssen-tarp/README.md). Janssen Tarp is an easy to install browser plug-in that comes with embedded Cedarling instance (WASM). Janssen Tarp also provides user interface to build authorization and authentication requests for testing purpose.

## Setup

- Install the Janssen Tarp [on Chrome or Firefox](https://github.com/JanssenProject/jans/blob/main/demos/janssen-tarp/README.md#releases).

## Step-1: Create Cedar Policy and Schema

The Cedarling needs policies and a schema to authorize access. These are bundled in a _policy store_ (a JSON file). A demo repository is provided for this quickstart which contains two policy stores. We will be using the `tarpUnsignedDemo` store for this demo.

- Go to the [Lock testing repository](https://github.com/JanssenProject/CedarlingQuickstart)
- Click on `Fork`
  - Make sure the `Copy the master branch only` option is **unchecked**
- Fork the repository
  - Open the [Agama Lab policy designer](https://cloud.gluu.org/agama-lab/dashboard/policy_store).
  - Click on `Change Repository`.
  - Select the option `Manually Type Repository URL`.
  - Paste the URL of your forked repository, then click the `Select` button.
- Open the policy store named `tarpUnsignedDemo`
- Click on the button named `Copy Link`. You will need this link in the next section.

## Step-2: Configure Tarp

In this step, we will add the policy store details in the Janssen Tarp that is
installed in the browser. The Cedarling instance embedded in the Tarp will
use the policy stored in this store to evaluate the authorization result.

1. Open Tarp installed in the browser
2. Navigate to the `Cedarling` tab and click on `Add Configurations`
3. Paste the following configuration parameters as JSON. Make sure to update the `<Policy Store URI>` value to point to your policy store
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
4. Click `Save` to initialize Cedarling.

This will start the Cedarling in Tarp, fetch and validate your policy store, and configure Cedarling to validate requests based on the User.

## Step-3: Test the policy using cedarling

Video walkthrough:

https://streamable.com/25wcb7

1. Go to Tarp, under `Cedarling` tab, click on `Cedarling Unsigned Authz Form`
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
         "cedar_entity_mapping": {
           "entity_type": "Jans::SecretDocument",
           "id": "some_id"
         }
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
