---
tags:
  - administration
  - lock
  - authorization / authz
  - Cedar
  - Cedarling
  - quick start
---

# Cedarling Quick Start Guide (Part 1)

## Introduction

[Jans Tarp](../../demos/jans-tarp) is a browser plugin that enables developers to test OpenID Connect flows. It embeds the Cedarling WASM PDP, and is one of the fastest ways to test out Cedar with real JSON web tokens. In this guide, we'll demonstrate how to use the embedded Cedarling WebAssembly app in Tarp to reach an authorization decision without tokens. We will demonstrate Role Based Access Control (RBAC). This will be done in three steps:

1. [Author Cedar Policy Store](#author-cedar-policy-store)
2. [Configure Tarp Cedarling Component](#configure-tarp-cedarling-component)
3. [Test Policy](#test-policy)


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

## Author Cedar Policy Store

To begin using the Cedarling, we need to set up a policy store. We will use [Agama Lab](https://cloud.gluu.org/agama-lab) for this. Please follow this video guide to set up your policy store:

![agama-lab-policy-store](../assets/agama-lab-policy-store.mp4)

The inputs for each section are as follows:

1. Schema: The default schema provided by Agama Lab
2. Policy:

    ```
    @id("allow_supreme_ruler")
    permit(
      principal in Jans::Role::"SupremeRuler",
      action,
      resource
    );
    ```

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
2. Click on `Cedarling`, then `Add Configurations`
3. Paste in the following configuration, replacing `<Policy Store URI>` with your policy store URL. 
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
4. Click `Save`. Tarp will validate your bootstrap and initialize Cedarling.
5. Click on `Cedarling Authz Form`
6. Fill in the following fields:

    * Principals: 
    ```
    TBD
    ```
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
7. Click `Cedarling Authz Request`. We will get a decision back.
