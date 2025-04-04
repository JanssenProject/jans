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

[Jans Tarp](../../demos/jans-tarp) is a browser plugin that enables developers to test OpenID Connect flows. In this guide, we'll demonstrate how to use the Cedarling in Tarp to demonstrate how we use Cedar policies to control access to a resource. In this case we will be demonstrating Attribute Based Access Control (ABAC). 

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

For this demo, we will use a policy where access is only granted if the principal entity from the provided input has the `SupremeRuler` role. In Cedar, this is expressed like so:

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

## Create Cedar policy and schema for our demo

The Cedarling requires Cedar policies and schema to perform authorization against our input. A policy store is a JSON file that contains both of these components. To create this policy store file, we can use [Agama Lab](https://cloud.gluu.org/agama-lab)'s Policy Desginer. To set up a policy store for this demo, please follow this video guide:

![agama-lab-policy-store](../assets/agama-lab-policy-store-unsigned.mp4)

The inputs for each section are as follows:

1. Schema:
  * Create a new entity named `Object` with no attributes
  * Add the new `Object` entity to the `Read` action as a resource
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
4. Click `Save`. Tarp will validate your bootstrap and initialize Cedarling.

## Test the policy with the cedarling

Here is a video guide for testing the policy with the cedarling:

![tarp-cedarling-setup-unsigned](../assets/tarp-cedarling-setup-unsigned.mp4)

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
3. Click `Cedarling Authz Request`. 

Cedarling will give us an authorization result back in JSON format. An example response is shown below: 

```json
{
  "workload": null,
  "person": null,
  "principals": {
    "Jans::User": {
      "decision": true,
      "diagnostics": {
        "reason": [
          "e438527019f3615ba1ab1144f3feb6f5c7fc3ccb677c"
        ],
        "errors": []
      }
    },
    "Jans::User::\"some_id\"": {
      "decision": true,
      "diagnostics": {
        "reason": [
          "e438527019f3615ba1ab1144f3feb6f5c7fc3ccb677c"
        ],
        "errors": []
      }
    }
  },
  "decision": true,
  "request_id": "019602e5-b148-7d0b-9d15-9d000c0d370b"
}
```

Here, the top level `decision` field gives us the result of the overall authorization call, which is `true`. 

