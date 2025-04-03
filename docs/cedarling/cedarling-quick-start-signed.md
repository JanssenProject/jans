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

1. [Author Cedarling Policy Store](#author-cedarling-policy-store)
2. [Configure Tarp Cedarling Component](#configure-tarp-cedarling-component)
3. [Test Cedarling Policy Decisions](#test-cedarling-policy-decisions)


## Prerequisites

Before we begin, we need to meet the following requirements:

* [Firefox](https://www.mozilla.org/en-US/firefox/) or [Google Chrome](https://www.google.com/chrome/index.html)
* The latest release of [Jans Tarp](https://github.com/JanssenProject/jans/releases/tag/nightly). Download the zip file for your browser and install it.

## Cedar Policy

For this QuickStart Demo, we will use a policy where access is only granted if the authenticated user has the `SupremeRuler` role. In Cedar, this is expressed like so:

```
@id("allow_supreme_ruler")
permit(
  principal in Jans::Role::"SupremeRuler",
  action,
  resource
);
```

Further information on Cedar can be found in the [official documentation](https://docs.cedarpolicy.com/)

## Author Cedarling Policy Store

To begin using the Cedarling, we need to set up a policy store. We will use [Agama Lab](https://cloud.gluu.org/agama-lab) for this. Please follow this video guide to set up your policy store:

![agama-lab-policy-store](../assets/agama-lab-policy-store.mp4)

The inputs for each section are as follows:

1. Schema: The default schema provided by Agama Lab
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

## Configure Tarp Cedarling Component 

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
          "or": [
              {"===": [{"var": "Jans::User"}, "ALLOW"]}
           ]
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

## Test Cedarling Policy Decisions

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
8. We will get a decision log back.

### Sequence diagram

A full sequence diagram of the mentioned steps can be found [here](https://sequencediagram.org/index.html#initialData=C4S2BsFMAIGFICYEMBO4QDsDm0kYdACqoAOAUGSaqAMYhUbDQBCKA9gO4DOkKl1IOgybEU5Kilr08TAIIBXYAAtoAZV4A3XhVaceKALQA+USQBc0AKKNe0BcrWbbNNhgBmILGVPH7K9ShaKBYAIgCeGEgAtoJw6JCM0ABKkFggXMAoSKCu0GR+joG8xqYWsPGJAJIhuPiONCiQwGS63MUmpBbWwLbwyGiYOABGbGzAGVkk0C7unt6kvor+TsHQAOKWhNAA9ABWeFwGSEvbjRkaAIx7HADWXPlLhUElndAAUgDqANKq82IvYgslQwYBASHQAC8YH1UOhsC12G1DB1AWpgNRcEs2CgQBDsiBcm5wJw-iRFg4AkELBstnsDkcTmdgJdtsdlNjcZAAPwAOj5DwpKwB5h2NHB4CGSBoNy5LgQkAAvHyeaTycsiqsaTt9hhDmylKdIOcrsA2DcEgL1c8USKAN4AIilNCNXAA+qbzRh7RYADyfQhGAC+OkR+mFZUQsMGdiWEIAFCQcRghOCADS4Gg5DDps5seQoZ3pmY9AAewAAlKqjK19BH+nCcCFIHQuASMEA)
