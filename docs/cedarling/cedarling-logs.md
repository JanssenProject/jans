---
tags:
  - administration
  - lock
  - authorization / authz
  - Cedar
  - Cedarling
  - logging
  - audit
---


# Cedarling Logs

## Cedarling Audit Logs

The Cedarling logs contains a record of all a Cedarling's decisions and token validations. 
Cedarling has four logging options, which are configurable via the `CEDARLING_LOG_TYPE`
bootstrap property: 

* `off` - no logging
* `memory` - logs stored in Cedarling in-memory KV store, fetched by client via logging interface. This 
  is ideal for batching logs without impeding authz performance
* `std_out` - write logs synchronously to std_out
* `lock` - periodically POST logs to Jans Lock Server `/audit` endpoint for central archiving. 

There are three different log records produced by the Cedarling:

* `Decision` - The result and diagnostics of an authz decision
* `System` - Startup, debug and other Cedarling messages not related to authz
* `Metric`- Performance and usage data

## Jans Lock Server

In enterprise deployments, [Janssen Lock Server](../janssen-server/lock/) collects Cedarling 
logs and can stream to a database or S3 bucket. The Cedarling decision logs provide compliance 
evidence of usage of the domain's externalized policies. The logs are also useful for forensic 
analysis to show everything the attacker attempted, both allowed and denied.


