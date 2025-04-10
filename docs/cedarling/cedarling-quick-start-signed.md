---
tags:
  - administration
  - lock
  - authorization / authz
  - Cedar
  - Cedarling
  - quick start
---

# Cedarling Quick Start 2 (Signed)

## Introduction

[Jans Tarp](../../demos/jans-tarp) is a browser plugin that enables developers to test OpenID Connect flows. It embeds the Cedarling WASM PDP and lets us test Cedar with real JWTs. This guide demonstrates Role Based Access Control (RBAC) using three steps:

1. [Create Cedar policy and schema](#create-cedar-policy-and-schema)
2. [Configure Tarp with the policy store](#configure-tarp-with-the-policy-store)
3. [Test the policy using Cedarling](#test-the-policy-using-cedarling)

## Prerequisites

Install Jans Tarp in [Firefox](https://www.mozilla.org/en-US/firefox/) or [Chrome](https://www.google.com/chrome/index.html):

* [Download Tarp](https://github.com/JanssenProject/jans/releases/tag/nightly)
* Firefox:
  * `about:debugging` → This Firefox → Load Temporary Add-on → select ZIP
* Chrome:
  * Extract ZIP → Settings > Extensions → Enable Developer Mode → Load Unpacked → select folder

## Cedar Policy

This demo policy grants access only to users with the `SupremeRuler` role:

```
@id("allow_supreme_ruler")
permit(
  principal in Jans::Role::"SupremeRuler",
  action,
  resource
);
```

## Create Cedar Policy and Schema 

Cedarling needs policies and a schema to authorize access. These are bundled in a *policy store* (a JSON file). To create one, use [Agama Lab](https://cloud.gluu.org/agama-lab)’s **Policy Designer**, which provides a visual tool to define entities, actions, resources, and policies.

Follow this video walkthrough:

![agama-lab-policy-store](../assets/agama-lab-policy-store.mp4)

**Inputs:**

- **Schema**:
   - Under `Entity types`, add a new entity type `Object` with no attributes and hit `save`
   - Under `Actions`, select the `Read` action and edit it. Add `Object` as a resource and hit `save`
   - Scroll to top of the page and hit `save` to save the schema changes
- **Policy**: 
  This demo policy grants access only to users with the `SupremeRuler` role:

  ```
  @id("allow_supreme_ruler")
  permit(
    principal in Jans::Role::"SupremeRuler",
    action,
    resource
  );
  ```
  - Click on `Policies`
  - Click `Add Policy` and then `Text Editor`
  - Paste in the policy from above
  - Click Save

- **Trusted Issuers**:

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

At the end, user the `Copy Link` button to copy the generated **policy store URI** for the next step.

## Configure Tarp with the policy store 

1. Open Tarp
2. `Add Client`:
   * Issuer: `https://test-jans.gluu.info`
   * Expiry: The day after today 
   * Scopes: `openid`, `profile`, `role`
3. Click `Register`
4. Go to `Cedarling` tab and click `Add Configurations`
5. Select `JSON` configuration type and Paste the config as given below. Remember to replace `<Policy Store URI>` with 
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

6. Click `Save` to initialize Cedarling.

## Test the policy using cedarling

1. In Tarp, under `Authentication flow` tab, click the ⚡ icon to begin authentication
2. Input:
   * ACR: `basic`
   * Scopes: `openid`, `profile`, `role`
3. Login on the test IDP with a user having `SupremeRuler` role
4. Click `Allow` on the consent screen
5. If the authentication is successful, Tarp will show you a page with token details and `Cedarling Authz Request Form` section
5. Open `Cedarling Authz Request Form`
6. Use the details before as an input to this form:
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


**Sample Response:**

```json
{
  ...
  "decision": true,
  "request_id": "019602f1-c964-7dbb-8a07-5b66b642e502"
}
```

The top-level `decision: true` confirms successful authorization.

## Sequence diagram

View full diagram [here](https://sequencediagram.org/index.html#initialData=C4S2BsFMAIGFICYEMBO4QDsDm0kYdACqoAOAUGSaqAMYhUbDQBCKA9gO4DOkKl1IOgybEU5Kilr08TAIIBXYAAtoAZV4A3XhVaceKALQA+USQBc0AKKNe0BcrWbbNNhgBmILGVPH7K9ShaKBYAIgCeGEgAtoJw6JCM0ABKkFggXMAoSKCu0GR+joG8xqYWsPGJAJIhuPiONCiQwGS63MUmpBbWwLbwyGiYOABGbGzAGVkk0C7unt6kvor+TsHQAOKWhNAA9ABWeFwGSEvbjRkaAIx7HADWXPlLhUElndAAUgDqANKq82IvYgslQwYBASHQAC8YH1UOhsC12G1DB1AWpgNRcEs2CgQBDsiBcm5wJw-iRFg4AkELBstnsDkcTmdgJdtsdlNjcZAAPwAOj5DwpKwB5h2NHB4CGSBoNy5LgQkAAvHyeaTycsiqsaTt9hhDmylKdIOcrsA2DcEgL1c8USKAN4AIilNCNXAA+qbzRh7RYADyfQhGAC+OkR+mFZUQsMGdiWEIAFCQcRghOCADS4Gg5DDps5seQoZ3pmY9AAewAAlKqjK19BH+nCcCFIHQuASMEA)
