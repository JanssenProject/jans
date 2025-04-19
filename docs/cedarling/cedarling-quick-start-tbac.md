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

# Implementing TBAC using Cedarling

This guide shows how to implement 
[Token Based Access Control(TBAC)](./cedarling-overview.md#token-based-access-control-tbac) using 
the Cedarling. To do this, we need 3 things. 

1. An Authorization policy
2. A request for user action
3. A running instance of the Cedarling

For `1` above, we will be using [Agama Lab policy designer](https://gluu.org/agama/authorization-policy-designer/) to quickly author
a [Cedar] policy and policy store.

For `2` and `3`, we will use [Janssen Tarp](https://github.com/JanssenProject/jans/blob/main/demos/janssen-tarp/README.md). Jans Tarp is an easy to install browser
plug-in that comes with embedded Cedarling instance (WASM). Jans Tarp also provides
user interface to build authorization and authentication requests for testing
purpose.

## Setup

- Install Janssen Tarp [on Chrome browser](https://github.com/JanssenProject/jans/blob/main/demos/janssen-tarp/README.md#releases) browser

## Step-1: Create Cedar Policy and Schema

In this guide, we will use a policy that grants access to all actions and all
the resources, only to the users with the `SupremeRuler` role. Policy text 
looks like below.

```
@id("allow_supreme_ruler")
permit(
  principal in Jans::Role::"SupremeRuler",
  action,
  resource
);
```

The Cedarling needs policies and a schema to authorize access. 
These are bundled in a *policy store* (a JSON file). To create a policy store, 
use [Agama Lab policy designer guide](https://gluu.org/agama/authorization-policy-designer/) 
and steps below. At the end of these steps, the policy
designer will publish your policy store to a GitHub repository. 

Follow this video walkthrough:

![agama-lab-policy-store](../assets/agama-lab-policy-store.mp4)

### Schema setup

  - Open the [Agama Lab policy designer](https://cloud.gluu.org/agama-lab/dashboard/policy_store)
  and open the policy store
  - Click on `Schema` tab
    - Scroll down to `Entity Types` and click on the `+` sign
      - Entity type name: `Object`
      - Click Save
    - Scroll down to `Actions` and click on it  
      - Click on `Read` and select the pencil icon
      - Under Resources, add `Object`
      - Click Save
    - Scroll back up and click the green `Save` button on top of the entity list

### Policy setup

  We will be using the policy below that grants access only to users with 
  the `SupremeRuler` role.

  ```
  @id("allow_supreme_ruler")
  permit(
    principal in Jans::Role::"SupremeRuler",
    action,
    resource
  );
  ```

  Follow the steps below to add the above policy to the policy store.

  - Click on `Policies`
  - Click `Add Policy` and then `Text Editor`
  - Paste in the policy from above
  - Click Save

### Trusted issuer setup

  When implementing TBAC, the Cedarling will validate the tokens with the IDP.
  To be able to this, we need to provide trusted issuer information to the 
  Cedarling. We can do this by adding the trusted issuer information to the
  policy store.
  
  Add your IDP as a trusted issuer using the `Trusted Issuer` tab. 

  - Click on `Add issuer` and add details as shown below.
   - Name: `testIdp`
   - Description: `Test IDP`
   - OIDC Config URL: `https://test-jans.gluu.info/.well-known/openid-configuration`
   - Token Metadata:

     ```json
     {
       "access_token": {
         "trusted": true,
         "entity_type_name": "Jans::Access_token",
         "required_claims": ["jti", "iss", "aud", "sub", "exp", "nbf"],
         "principal_mapping": ["Jans::Workload"]
       },
       "id_token": {
         "trusted": true,
         "entity_type_name": "Jans::id_token"
       },
       "userinfo_token": {
         "trusted": true,
         "entity_type_name": "Jans::Userinfo_token",
         "principal_mapping": ["Jans::User"]
       }
     }
     ```

At the end, use the `Copy Link` button to copy the generated 
**policy store URI** for the next step.

## Step-2: Configure Tarp with the policy store 

In this step, we will add the policy store details in the Janssen Tarp that is
installed in the browser. The Cedarling instance embedded in the Tarp will
use the policy stored in this store to evaluate the authorization result.

1. Open Tarp installed in the Chrome browser 
2. Click `Add Client`. Use details below to add a new client.
   * Issuer: `https://test-jans.gluu.info`
   * Expiry: The day after today 
   * Scopes: `openid`, `profile`, `role`
3. Click `Register`
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

6.  Click `Save` to initialize Cedarling. This will start the Cedarling in Tarp,
 fetch and validate your policy store, and configure Cedarling to validate requests based on the User. 

## Step-3: Test the policy using the Cedarling

Since we are implementing TBAC, we have to authenticate the user first to get
the tokens. 

1. In Tarp, under `Authentication flow` tab, click the ⚡ icon to begin authentication
2. Input:
   * ACR: `basic`
   * Scopes: `openid`, `profile`, `role`
3. Login using a user having the `SupremeRuler` role
4. Click `Allow` on the consent screen
5. If the authentication is successful, Tarp will show you a page with token details and `Cedarling Authz Request Form` section
5. Open `Cedarling Authz Request Form`
6. Use the details below as an input to this form:
   * Principal: select all 3 tokens
   * Action: `Jans::Action::"Read"`
   * Resource:
     ```json
     {
       "entity_type": "resource",
       "type": "Jans::Object",
       "id": "some_id"
     }
     ```
7. Leave the `Context` blank
7. Click `Cedarling Authz Request`


```json title="Sample Response"
{
  ...
  "decision": true,
  "request_id": "019602f1-c964-7dbb-8a07-5b66b642e502"
}
```

The top-level `decision: true` confirms successful authorization.

## Sequence diagram

View full diagram [here](https://sequencediagram.org/index.html#initialData=C4S2BsFMAIGFICYEMBO4QDsDm0kYdACqoAOAUGSaqAMYhUbDQBCKA9gO4DOkKl1IOgybEU5Kilr08TAIIBXYAAtoAZV4A3XhVaceKALQA+USQBc0AKKNe0BcrWbbNNhgBmILGVPH7K9ShaKBYAIgCeGEgAtoJw6JCM0ABKkFggXMAoSKCu0GR+joG8xqYWsPGJAJIhuPiONCiQwGS63MUmpBbWwLbwyGiYOABGbGzAGVkk0C7unt6kvor+TsHQAOKWhNAA9ABWeFwGSEvbjRkaAIx7HADWXPlLhUElndAAUgDqANKq82IvYgslQwYBASHQAC8YH1UOhsC12G1DB1AWpgNRcEs2CgQBDsiBcm5wJw-iRFg4AkELBstnsDkcTmdgJdtsdlNjcZAAPwAOj5DwpKwB5h2NHB4CGSBoNy5LgQkAAvHyeaTycsiqsaTt9hhDmylKdIOcrsA2DcEgL1c8USKAN4AIilNCNXAA+qbzRh7RYADyfQhGAC+OkR+mFZUQsMGdiWEIAFCQcRghOCADS4Gg5DDps5seQoZ3pmY9AAewAAlKqjK19BH+nCcCFIHQuASMEA)
