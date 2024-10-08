---
tags:
  - administration
  - lock
  - authorization / authz
  - Cedar
  - Cedarling
  - policy store
---

# Cedarling Policy Store

By convention, the filename is `cedarling_store.json`. It contains all the data the 
Cedarling needs to evaluate policies and verify JWT tokens:

1. Cedar Schema - Base64 encoded human format
2. Cedar Policies - Base64 encoded human format
3. Trusted Issuers - See below syntax

The JSON schema looks like this:

```
{
    "app_id": "...",
    "policies": "...",
    "schema": "...",
    "trusted_idps": [...]
}
```


## Policy and Schema Authoring

You can hand create your Cedar policies and schema in 
[Visual Studio](https://marketplace.visualstudio.com/items?itemName=cedar-policy.vscode-cedar). 
Make sure you run the cedar command line tool to validate both your schema and policies. 

The easiest way to author your policy store is to use the Policy Designer in 
[Agama Lab](https://cloud.gluu.org/agama-lab). This tool helps you define the policies, schema and
trusted IDPs and to publish a policy store to a Github repository.
