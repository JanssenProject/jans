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

[Jans Tarp](../../demos/jans-tarp) is a browser plugin that enables developers to test OpenID Connect flows. It embeds the Cedarling WASM PDP, and is one of the fastest ways to test out Cedar with real JSON web tokens. In this guide, we'll demonstrate how to use Tarp to register a client on an OAuth authorization server("AS"), and use the embedded Cedarling WebAssembly app to reach an authorization decision. We will demonstrate Role Based Access Control (RBAC). This will be done in three steps:

1. [Create Cedar policy and schema for our demo](#create-cedar-policy-and-schema-for-our-demo)
2. [Configure Tarp to use this demo policy store](#configure-tarp-to-use-this-demo-policy-store)
3. [Test the policy with the cedarling](#test-the-policy-with-the-cedarling)

## Prerequisites

To perform this demo, we will need Jans Tarp installed in either [Firefox](https://www.mozilla.org/en-US/firefox/) or [Google Chrome](https://www.google.com/chrome/index.html).

Follow these steps to install Tarp in your browser:

* Download the latest release of [Jans Tarp](https://github.com/JanssenProject/jans/releases/tag/nightly) for your browser.
* For Firefox:
  * Type in `about:debugging` in your browser's URL bar
  * Click on `This Firefox`
  * Click on `Load Temporary Add-on` and select the zip you downloaded.
* For Chrome:
  * Extract the downloaded zip file to a folder
  * Go to Settings > Extensions
  * Turn on Developer Mode
  * Click on `Load Unpacked`
  * Select the folder where you extracted the zip

## Cedar Policy

For this demo, we will use a policy where access is only granted if the authenticated user has the `SupremeRuler` role. In Cedar, this is expressed like so:

```
@id("allow_supreme_ruler")
permit(
  principal in Jans::Role::"SupremeRuler",
  action,
  resource
);
```

## Create Cedar policy and schema for our demo

The Cedarling requires Cedar policies and schema to perform authorization against the tokens obtained by Tarp. A policy store is a JSON file that contains both of these components. To create this policy store file, we can use [Agama Lab](https://cloud.gluu.org/agama-lab)'s Policy Desginer. To set up a policy store for this demo, please follow this video guide:

![agama-lab-policy-store](../assets/agama-lab-policy-store.mp4)

The inputs for each section are as follows:

1. Schema:
  * Create a new entity named `Object` with no attributes
  * Add the new `Object` entity to the `Read` action as a resource
2. Policy: The policy from [here](#cedar-policy)
3. Trusted issuers:

  - Trust Issuer Name: `testIdp`
  - Trust Issuer Description: `Test IDP`
  - OpenID Configuration Endpoint: `https://test-jans.gluu.info/.well-known/openid-configuration`
  - Trusted issuers token metadata:

  ```json
  {
    "access_token": {
      "trusted": true,
      "entity_type_name": "Jans::Access_token",
      "required_claims": [
        "jti",
        "iss",
        "aud",
        "sub",
        "exp",
        "nbf"
      ],
      "principal_mapping": [
        "Jans::Workload"
      ]
    },
    "id_token": {
      "trusted": true,
      "entity_type_name": "Jans::id_token"
    },
    "userinfo_token": {
      "trusted": true,
      "entity_type_name": "Jans::Userinfo_token",
      "principal_mapping": [
        "Jans::User"
      ]
    }
  }
  ```

After following the guide, the policy store URI will be copied to the clipboard. We will need this in the next step.

## Configure Tarp to use this demo policy store 

1. Open Tarp on your browser.
2. Click on `Add Client` and fill in the following details:

   * Issuer: `https://test-jans.gluu.info`.
   * Client Expiry Date: One day after your current date
   * Scopes: `openid`,`profile`, and `role`
3. Click `Register`. 
4. Click on `Cedarling`, then `Add Configurations`
5. Paste in the following configuration, replacing `<Policy Store URI>` with your policy store URL. 
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
          "HS256",
          "RS256"
      ],
      "CEDARLING_ID_TOKEN_TRUST_MODE": "none",
      "CEDARLING_LOCK": "disabled",
      "CEDARLING_LOCK_DYNAMIC_CONFIGURATION": "disabled",
      "CEDARLING_LOCK_LISTEN_SSE": "disabled"
  }
  ```
6. Click `Save`. Tarp will validate your bootstrap and initialize Cedarling.

## Test the policy with the cedarling

1. On the Authentication Flow screen, click on the lightning icon to trigger an authentication flow.
2. Use the following values for triggering the flow:
    * Acr Value: `basic`
    * Scopes: `openid`, `profile`, and `role`
3. We will be redirected to the test IDP. Login using a user that has the `SupremeRuler` role on this IDP.
4. On the consent screen, click `Allow`
5. After you are redirected back to Tarp, click on the `Cedarling Authz Request Form`
6. Use the following input:

    * Principal: Select all three tokens
    * Action: `Jans::Action::"Read"`
    * Resource:
    ```json
    {
      "entity_type": "resource",
      "type": "Jans::Object",
      "id": "some_id"
    }
    ```
7. Click on `Cedarling Authz Request`

Cedarling will give us an authorization result back in JSON format. An example response is shown below: 

```json
{
  "workload": null,
  "person": {
    "decision": true,
    "diagnostics": {
      "reason": [
        "56b712edaf14ca044303b5ec4ea97b8fb34dc466ac2f"
      ],
      "errors": []
    }
  },
  "principals": {
    "Jans::User": {
      "decision": true,
      "diagnostics": {
        "reason": [
          "56b712edaf14ca044303b5ec4ea97b8fb34dc466ac2f"
        ],
        "errors": []
      }
    },
    "Jans::User::\"UB2S00kTRTL1pXlRDI4LgVHgekIWauk4355-eCjy2FQ\"": {
      "decision": true,
      "diagnostics": {
        "reason": [
          "56b712edaf14ca044303b5ec4ea97b8fb34dc466ac2f"
        ],
        "errors": []
      }
    }
  },
  "decision": true,
  "request_id": "019602f1-c964-7dbb-8a07-5b66b642e502"
}
```
Here, the top level `decision` field gives us the result of the overall authorization call, which is `true`. 

### Sequence diagram

A full sequence diagram of the mentioned steps can be found [here](https://sequencediagram.org/index.html#initialData=C4S2BsFMAIGFICYEMBO4QDsDm0kYdACqoAOAUGSaqAMYhUbDQBCKA9gO4DOkKl1IOgybEU5Kilr08TAIIBXYAAtoAZV4A3XhVaceKALQA+USQBc0AKKNe0BcrWbbNNhgBmILGVPH7K9ShaKBYAIgCeGEgAtoJw6JCM0ABKkFggXMAoSKCu0GR+joG8xqYWsPGJAJIhuPiONCiQwGS63MUmpBbWwLbwyGiYOABGbGzAGVkk0C7unt6kvor+TsHQAOKWhNAA9ABWeFwGSEvbjRkaAIx7HADWXPlLhUElndAAUgDqANKq82IvYgslQwYBASHQAC8YH1UOhsC12G1DB1AWpgNRcEs2CgQBDsiBcm5wJw-iRFg4AkELBstnsDkcTmdgJdtsdlNjcZAAPwAOj5DwpKwB5h2NHB4CGSBoNy5LgQkAAvHyeaTycsiqsaTt9hhDmylKdIOcrsA2DcEgL1c8USKAN4AIilNCNXAA+qbzRh7RYADyfQhGAC+OkR+mFZUQsMGdiWEIAFCQcRghOCADS4Gg5DDps5seQoZ3pmY9AAewAAlKqjK19BH+nCcCFIHQuASMEA)
