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

[Jans Tarp](../../demos/jans-tarp) is a browser plugin for testing OpenID Connect flows. This guide shows how to use Cedarling in Tarp to enforce Cedar policies using Attribute Based Access Control (ABAC).

**Steps:**

1. [Create Cedar policy and schema](#create-cedar-policy-and-schema)
2. [Configure Tarp with the policy store](#configure-tarp-with-the-policy-store)
3. [Test the policy using Cedarling](#test-the-policy-using-cedarling)

## Prerequisites

Install Jans Tarp in [Chrome](https://www.google.com/chrome/index.html):

* [Download Tarp](https://github.com/JanssenProject/jans/releases/download/nightly/demo-jans-tarp-chrome-nightly.zip)
* Extract ZIP → Settings > Extensions → Enable Developer Mode → Load Unpacked → select folder

## Create Cedar Policy and Schema

Cedarling needs policies and a schema to authorize access. These are bundled in a *policy store* (a JSON file). To create one, use [Agama Lab](https://cloud.gluu.org/agama-lab)’s **Policy Designer**, which provides a visual tool to define entities, actions, resources, and policies.

Follow this video walkthrough:

![agama-lab-policy-store](https://streamable.com/ioxbst)

**Inputs:**

- **Schema**:
  - Add entity `Object` (no attributes)
  - Link `Object` to the `Read` action as a resource
- **Policy**:
  This demo policy grants access only to user entities with the `SupremeRuler` role:
  
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

At the end, copy the generated **policy store URI** for the next step.

## Configure Tarp with the policy store 

1. Open Tarp → `Cedarling` → `Add Configurations`
2. Paste the following, replacing `<Policy Store URI>`:

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

3. Click `Save` to initialize Cedarling.

## Test the policy using cedarling 

Video walkthrough:

![tarp-cedarling-setup-unsigned](https://streamable.com/7lc2ey)

1. Go to `Cedarling Authz Form`
2. Input the following:

**Principals:**

    ```
    [
      {
        "type": "Jans::User",
        "id": "some_id",
        "sub": "some_sub",
        "role": ["SupremeRuler"]
      }
    ]
    ```

**Action:** `Jans::Action::"Read"`

**Resource:**

    ```
    {
      "entity_type": "resource",
      "type": "Jans::Object",
      "id": "some_id"
    }
    ```

3. Click `Cedarling Authz Request`

**Sample Response:**

    ```
    {
      "decision": true,
      "request_id": "019602e5-b148-7d0b-9d15-9d000c0d370b",
      ...
    }
    ```

The top-level `decision: true` confirms successful authorization.
