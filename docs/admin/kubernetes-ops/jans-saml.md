---
tags:
  - administration
  - kubernetes
  - operations
  - jans-saml
  - keycloak
---

To deploy jans-saml/keycloak, you have to make the following changes in your `values.yaml`:

```yaml
global:
  saml:
   enabled: true
   ingress:
    samlEnabled: true
config:
  configmap:    
    kcDbVendor: mysql
    kcDbUsername: keycloak
    kcDbPassword: Test1234#
    kcDbSchema: keycloak
    kcDbUrlHost: mysql.kc.svc.cluster.local
    kcDbUrlPort: 3306
    kcDbUrlDatabase: keycloak
```

You need a database as a backend for keycloak.

If you provide a non-root MySQL user to Keycloak, you will encounter the following error and warnings:

```
SQLException: XAER_RMERR: Fatal error occurred in the transaction branch - check your data for consistency
WARNING - jans-saml - 2024-02-05 16:54:04,256 - Unable to grant XA_RECOVER_ADMIN privilege to 'keycloak' user; reason=Access denied; you need (at least one of) the GRANT OPTION privilege(s) for this operation
WARNING - jans-saml - 2024-02-05 16:54:04,256 - Got insufficient permission, please try using user with XA_RECOVER_ADMIN privilege and running the following query manually via MySQL client: "GRANT XA_RECOVER_ADMIN ON *.* TO 'keycloak'@'%'; FLUSH PRIVILEGES;"
```

To resolve this issue, it's necessary to adhere to the guidance provided in the logs.
