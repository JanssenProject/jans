---
tags:
  - administration
  - lock
  - authorization / authz
  - Cedar
  - Cedarling
  - properties
---

# Cedarling Properties

These Bootstrap Properties control default application level behavior.

* **`CEDARLING_APPLICATION_NAME`** : Human friendly identifier for this application

* **`CEDARLING_POLICY_STORE_URI`** : Location of policy store JSON, used if policy store is not local, or retreived from Lock Server.

* **`CEDARLING_JWT_VALIDATION`** : Enabled | Disabled 

* **`CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED`** : ....

* **`CEDARLING_REQUIRE_AUD_VALIDATION`** : Enabled | Disabled. Controls if Cedarling will discard id_token without an access token with the corresponding client_id.

* **`CEDARLING_ROLE_MAPPING`** : Default: `{"id_token": "role", "userinfo_token": "role"}` but the role may be sent as an access token, or with a different identifier. For example, for Ping Identity, you might see `{"userinfo_token": "memberOf"}`.

* **`CEDARLING_LOG_LEVEL`** : Controls the verbosity of Cedar logging.

The following bootstrap properties are only needed for enterprise deployments.

* **`CEDARLING_LOCK`** : Enabled | Disabled. If Enabled, the Cedarling will connect to the Lock Server for policies, and subscribe for SSE events. 

* **`CEDARLING_LOCK_MASTER_CONFIGURATION_URI`** : Required if `LOCK` == `Enabled`. URI where Cedarling can get JSON file with all required metadata about Lock Server, i.e. `.well-known/lock-server-configuration`.

* **`CEDARLING_LOCK_SSA_JWT`** : SSA for DCR in a Lock Server deployment. The Cedarling will validate this SSA JWT prior to DCR.

* **`CEDARLING_POLICY_STORE_ID`** : The identifier of the policy stored needed only for Lock Server deployments.

* **`CEDARLING_AUDIT_LOG_INTERVAL`** : How often to send log messages to Lock Server (0 to turn off trasmission)

* **`CEDARLING_AUDIT_HEALTH_INTERVAL`** : How often to send health messages to Lock Server (0 to turn off transmission)

* **`CEDARLING_AUDIT_TELEMETRY_INTERVAL`** : How often to send telemetry messages to Lock Server (0 to turn off transmission)

* **`CEDARLING_DYNAMIC_CONFIGURATION`** : Enabled | Disabled, controls whether Cedarling should listen for SSE config updates

* **`CEDARLING_GET_TOKEN_STATUS_LIST_UPDATES`** :  Enabled | Disabled, controls whether Cedarling should listen for SSE OAuth Status List updates
