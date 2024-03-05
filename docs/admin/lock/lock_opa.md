---
tags:
 - administration
 - lock
 - opa
---

## Jans Lock Rego Policies

Lock can update your policies from a GIT or URI based policy store.

Remote files specified in `policiesJsonUris` array should be JSON array with list of actual policies URIs. This behavior was added to group properties in packages.

`policiesZipUris` is another way to get policies. This can be also be link to download github repository as zip archive.

These 2 methods support files download with `Bearer` authentication protection. Access tokens can be specified in `policiesJsonUrisAccessToken` and `policiesZipUrisAccessToken`

Lock automatically do policies clean up if they were removed from source servers or configuration on next synchronization.

