---
tags:
  - administration
  - lock
  - authorization / authz
  - Cedar
  - Cedarling
  - quick start
---

# Cedarling Quick Start 1 (Unsigned)

## Introduction

[Jans Tarp](../../demos/jans-tarp) is a browser plugin that enables developers to test OpenID Connect flows. It embeds the Cedarling WASM PDP, and is one of the fastest ways to test out Cedar with real JSON web tokens. In this guide, we'll demonstrate how to use the embedded Cedarling WebAssembly app in Tarp to reach an authorization decision without tokens. We will demonstrate Role Based Access Control (RBAC). This will be done in three steps:

1. [Author Cedarling Policy Store](#author-cedarling-policy-store)
2. [Configure Tarp Cedarling Component](#configure-tarp-cedarling-component)
3. [Test Unsigned Policy Decisions](#test-unsigned-policy-decisions)


## Prerequisites

Before we begin, we need to meet the following requirements:

* [Firefox](https://www.mozilla.org/en-US/firefox/) or [Google Chrome](https://www.google.com/chrome/index.html)
* The latest release of [Jans Tarp](https://github.com/JanssenProject/jans/releases/tag/nightly). Download the zip file for your browser and install it.

## Cedar Policy

For this QuickStart Demo, we will use a policy where access is only granted if the principal entity from the provided input has the `SupremeRuler` role. In Cedar, this is expressed like so:

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

Further information on Cedar can be found in the [official documentation](https://docs.cedarpolicy.com/)

## Author Cedarling Policy Store

To begin using the Cedarling, we need to set up a policy store. We will use [Agama Lab](https://cloud.gluu.org/agama-lab) for this. Please follow this video guide to set up your policy store:

![agama-lab-policy-store](../assets/agama-lab-policy-store-unsigned.mp4)

The inputs for each section are as follows:

1. Schema: The default schema provided by Agama Lab
2. Policy: The policy [here](#cedar-policy)

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

## Test Unsigned Policy Decisions

1. Click on `Cedarling Authz Form`
2. Fill in the following fields:

    * Principals: 
    ```json
    [
      {
        "type": "Jans::User",
        "id": "some_id",
        "sub": "some_sub",
        "role": ["SupremeRuler"]
      }
    ]
    ```
    * Action: `Jans::Action::"Read"`
    * Resource: 
    ```json
    {
      "entity_type": "resource",
      "type": "Jans::Object",
      "id": "some_id"
    }
    ```
3. Click `Cedarling Authz Request`. We will get a decision back.
