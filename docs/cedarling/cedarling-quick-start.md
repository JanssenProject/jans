---
tags:
  - administration
  - lock
  - authorization / authz
  - Cedar
  - Cedarling
  - quick start
---

# Cedarling Quick Start Guide

## Introduction

In this guide, we'll demonstrate how to use [Jans Tarp](../../demos/jans-tarp) to register a client on an authorization server, and use the embedded cedarling webassembly app to reach an authorization decision. This will be done in three steps:

1. [Setting up a policy store](#policy-store-setup)
2. [Loading the policy store in Jans Tarp](#tarp-setup)
3. [Using Tarp to launch an authorization flow and validate authorization against the policy](#authentication-flow)


## Prerequisites

Before we begin, we need to meet the following requirements:

* [Firefox](https://www.mozilla.org/en-US/firefox/windows/) or [Google Chrome](https://www.google.com/chrome/index.html)
* The latest release of [Jans Tarp](https://github.com/JanssenProject/jans/releases/tag/nightly). Download the zip file for your browser and install it.

## Cedar Policy

[Cedar](https://www.cedarpolicy.com/en) is a language for defining permissions as policies, and a specification for evaluating those policies. Cedarling expects policies to be written in Cedar. For this quickstart, we will use a policy where access is only granted if the authenticated user has the `SupremeRuler` role. In cedar, this is expressed like so:

```
@id("allow_supreme_ruler")
permit(
  principal is Jans::User,
  action,
  resource
)
when {
  principal has userinfo_token.role &&
  principal.userinfo_token.role.contains("SupremeRuler")
};
```

This policy expects that the authorized entity is of type `Jans::User` (as defined in the [default Janssen schema](../../jans-cedarling/schema/cedarling-core.cedarschema)). Then the policy tests whether the userinfo token obtained after authorization contains the `role` claim as well as whether the claim contains the value `SupremeRuler`. If all these conditions are met, Cedarling will allow access. This is also called Role Based Access Control (RBAC).

Further information on cedar can be found in the [official documentation](https://docs.cedarpolicy.com/)

## Policy Store Setup

To begin using Cedarling, we need to set up a policy store. We will use [Agama Lab](https://cloud.gluu.org/agama-lab) for this. Please follow this video guide to set up your policy store:

![agama-lab-policy-store](../assets/agama-lab-policy-store.mp4)

For the trusted issuer token metadata, paste in the following content:
```json
{
  "accessTokens": {
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
  "idTokens": {
    "trusted": true,
    "entity_type_name": "Jans::id_token"
  },
  "userinfoTokens": {
    "trusted": true,
    "entity_type_name": "Jans::Userinfo_token",
    "principal_mapping": [
      "Jans::User"
    ]
  }
}
```

After following the guide, the policy store URI will be copied to the clipboard. We will need this in the next step.

## Tarp Setup

1. Open Tarp on your browser.
   ![image](../assets/tarp-blank.png)
2. Click on `Add Client` and fill in the following details:

   * Issuer: `https://account.gluu.org`
   * Client Expiry Date: One day after your current date
   * Scopes: `openid`,`profile`, and `role`
3. Click `Register`. 
4. Click on `Cedarling`, then `Add Configurations`
5. Paste in the following configuration, replacing `<Policy Store URI>` with your policy store URL. 
  ```json
  {
      "CEDARLING_APPLICATION_NAME": "My App",
      "CEDARLING_POLICY_STORE_URI": "<Policy Store URI>",
      "CEDARLING_POLICY_STORE_ID": "gICAgcHJpbmNpcGFsIGlz",
      "CEDARLING_LOG_TYPE": "std_out",
      "CEDARLING_LOG_LEVEL": "INFO",
      "CEDARLING_LOG_TTL": null,
      "CEDARLING_USER_AUTHZ": "enabled",
      "CEDARLING_WORKLOAD_AUTHZ": "disabled",
      "CEDARLING_PRINCIPAL_BOOLEAN_OPERATION": {
          "or": [
              {"===": [{"var": "Jans::Workload"}, "ALLOW"]},
              {"===": [{"var": "Jans::User"}, "ALLOW"]}
           ]
      },
      "CEDARLING_LOCAL_JWKS": null,
      "CEDARLING_POLICY_STORE_LOCAL": null,
      "CEDARLING_POLICY_STORE_LOCAL_FN": null,
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
      "CEDARLING_LOCK_SERVER_CONFIGURATION_URI": null,
      "CEDARLING_LOCK_DYNAMIC_CONFIGURATION": "disabled",
      "CEDARLING_LOCK_LISTEN_SSE": "disabled"
  }
  ```
6. Click `Save`. Tarp will validate your bootstrap and initialize Cedarling.

## Authentication Flow

1. On the Authentication Flow screen, click on the lightning icon to trigger an authentication flow.
2. Use the following values for triggering the flow:
    * Acr Value: `basic`
    * Scopes: `openid`, `profile`, and `role`
3. We will be redirected to the test IDP. Login using your credentials.
4. On the consent screen, click `Allow`
5. After you are redirected back to Tarp, click on the `Cedarling Authz Request Form`
6. Use the following input:

    * Principal: Select all three tokens
    * Action: `Jans::Action::"Read"`
    * Resource:
    ```json
    {
      "entity_type": "resource",
      "type": "Jans::Application",
      "id": "some_id",
      "app_id": "application_id",
      "name": "Some Application",
      "url": {
        "host": "jans.test",
        "path": "/protected-endpoint",
        "protocol": "http"
      }
    }
    ```
7. Click on `Cedarling Authz Request`
8. We will get a decision log back.

### Sequence diagram

```mermaid
sequenceDiagram
title Cedarling and Tarp

participant Browser
participant Tarp
participant Auth Server

Browser->Tarp: Enter Auth Server config
Tarp->Auth Server: Dynamic Client Registration 
Auth Server->Tarp: Client ID and Secret
Browser->Tarp: Enter Cedarling bootstrap config
Tarp->Auth Server: GET /jans-auth/restv1/jwks
Auth Server->Tarp: JWKS
Tarp->Tarp: Initialize Cedarling
Browser->Tarp: Start authorization flow
Tarp->Auth Server: GET /jans-auth/restv1/authorize?...
Auth Server->Tarp: /callback?code=...
Tarp->Auth Server: GET /jans-auth/restv1/token
Auth Server->Tarp: {"access_token": <JWT>}

Browser->Tarp: Cedarling Authz(principal, action, resource, context)
Tarp->Browser: Cedarling Decision
```
