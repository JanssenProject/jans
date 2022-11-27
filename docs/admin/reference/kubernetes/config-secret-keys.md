---
tags:
  - administration
  - reference
  - kubernetes
  - configmap
  - secret
---

## Overview

The Config Init job creates a set of secrets and configurations used by all Jans services.

To check the values of the configmaps on the current deployment run:

```bash
kubectl get configmap -n jans -o yaml
```

To check the values of the secrets on the current deployment run :

```bash
kubectl get secret -n jans -o yaml
```

## Jans Configmaps

| Key                                           | Example Values                                     |
| --------------------------------------------- | -------------------------------------------------- |
| `admin_email`                                 | `support@jans.io`                                  |
| `admin_inum`                                  | `631e2b84-1d3d-4f28-9a9a-026a25febf44`             |
| `auth_enc_keys`                               | `RSA1_5 RSA-OAEP`                                  |
| `auth_key_rotated_at`                         | `1669143906`                                       |
| `auth_legacyIdTokenClaims`                    | `false`                                            |
| `auth_openid_jks_fn`                          | `/etc/certs/auth-keys.jks`                         |
| `auth_openid_jwks_fn`                         | `/etc/certs/auth-keys.json`                        |
| `auth_openidScopeBackwardCompatibility`       | `false`                                            |
| `auth_sig_keys`                               | `RS256 RS384 RS512 ES256 ES384 ES512 PS256 PS384 PS512`|
| `city`                                        | `Austin`                                           |
| `country_code`                                | `US`                                               |
| `CN_AUTH_APP_LOGGERS`                         | `{"audit_log_level":"INFO","audit_log_target":"FILE","auth_log_level":"INFO","auth_log_target":"STDOUT","http_log_level":"INFO","http_log_target":"FILE","ldap_stats_log_level":"INFO","ldap_stats_log_target":"FILE","persistence_duration_log_level":"INFO","persistence_duration_log_target":"FILE","persistence_log_level":"INFO","persistence_log_target":"FILE","script_log_level":"INFO","script_log_target":"FILE"}` | 
| `CN_AUTH_SERVER_BACKEND`                      | `auth-server:8080`                                 |
| `CN_CACHE_TYPE`                               | `NATIVE_PERSISTENCE`                               |
| `CN_CERT_ALT_NAME`                            | `opendj`                                           |
| `CN_CONFIG_ADAPTER`                           | `kubernetes`                                       |
| `CN_CONFIG_API_APP_LOGGERS`                   | `{"config_api_log_level":"INFO","config_api_log_target":"STDOUT","ldap_stats_log_level":"INFO","ldap_stats_log_target":"FILE","persistence_duration_log_level":"INFO","persistence_duration_log_target":"FILE","persistence_log_level":"INFO","persistence_log_target":"FILE","script_log_level":"INFO","script_log_target":"FILE"}` | 
| `CN_CONFIG_KUBERNETES_CONFIGMAP`              | `cn`                                               |
| `CN_CONFIG_KUBERNETES_NAMESPACE`              | `jans`                                             |
| `CN_CONTAINER_MAIN_NAME`                      | `janssen-auth-server`                              |
| `CN_CONTAINER_METADATA`                       | `kubernetes`                                       |
| `CN_DOCUMENT_STORE_TYPE`                      | `LOCAL`                                            |
| `CN_FIDO2_APP_LOGGERS`                        | `{"fido2_log_level":"INFO","fido2_log_target":"STDOUT","persistence_log_level":"INFO","persistence_log_target":"FILE"}`|
| `CN_HYBRID_MAPPING`                           |  `{}`                                              |
| `CN_JETTY_REQUEST_HEADER_SIZE`                | `8192`                                             |
| `CN_KEY_ROTATION_CHECK`                       | `3600`                                             |
| `CN_KEY_ROTATION_FORCE`                       | `false`                                            |       
| `CN_KEY_ROTATION_INTERVAL`                    | `48`                                               |       
| `CN_LDAP_URL`                                 | `opendj:1636`                                      |
| `CN_MAX_RAM_PERCENTAGE`                       | `75.0`                                             | 
| `CN_PERSISTENCE_TYPE`                         | `ldap`                                             |
| `CN_PROMETHEUS_PORT`                          | ``                                                 |   
| `CN_SCIM_APP_LOGGERS`                         | `{"ldap_stats_log_level":"INFO","ldap_stats_log_target":"FILE","persistence_duration_log_level":"INFO" "persistence_duration_log_target":"FILE","persistence_log_level":"INFO","persistence_log_target":"FILE","scim_log_level":"INFO","scim_log_target":"STDOUT","script_log_level":"INFO","script_log_target":"FILE"}` |
| `CN_SCIM_ENABLED`                             | `true`                                             |
| `CN_SCIM_PROTECTION_MODE`                     | `OAUTH`                                            |
| `CN_SECRET_ADAPTER`                           | `kubernetes`                                       |
| `CN_SECRET_KUBERNETES_NAMESPACE`              | `jans`                                             |
| `CN_SECRET_KUBERNETES_SECRET`                 | `cn`                                               |
| `CN_SQL_DB_DIALECT`                           | `mysql`                                            |
| `CN_SQL_DB_HOST`                              | `my-release-mysql.default.svc.cluster.local`       |
| `CN_SQL_DB_NAME`                              | `jans`                                             |
| `CN_SQL_DB_PORT`                              | `3306`                                             |
| `CN_SQL_DB_SCHEMA`                            |  ``                                                |
| `CN_SQL_DB_TIMEZONE`                          |  `UTC`                                             |
| `CN_SQL_DB_USER`                              | `jans`                                             |
| `CN_SSL_CERT_FROM_SECRETS`                    | `true`                                             |
| `DOMAIN`                                      | `demoexample.jans.io`                              |
| `default_openid_jks_dn_name`                  | `CN=Janssen Auth CA Certificates`                  |
| `hostname`                                    | `demoexample.jans.io`                              |
| `ldap_binddn`                                 | `cn=directory manager`                             |
| `ldap_init_host`                              | `localhost`                                        |
| `ldap_init_port`                              | `1636`                                             |
| `ldap_port`                                   | `1389`                                             |
| `ldap_site_binddn`                            | `cn=directory manager`                             |
| `ldapTrustStoreFn`                            | `/etc/certs/opendj.pkcs12`                         |
| `ldaps_port`                                  | `1636`                                             |
| `optional_scopes`                             | `["fido2", "scim", "sql"]`                         |
| `orgName`                                     | `Janssen`                                          |
| `role_based_client_id`                        | `2000.4a8f3e8b-96b0-435a-8427-a287c242f4d9`        |
| `state`                                       | `TX`                                               |


## Jans Secrets

| Key                                       | Encode/Decode           | File                              |
| ----------------------------------------- | ----------------------- | --------------------------------- |
| `auth_jks_base64`                         | base64                  |                                   |
| `auth_openid_jks_pass`                    | base64                  |                                   |
| `auth_openid_key_base64`                  | base64                  |                                   |
| `encoded_admin_password`                  | ldap_encode + base64    |                                   |
| `encoded_ldapTrustStorePass`              | pyDes + base64          |                                   |
| `encoded_ox_ldap_pw`                      | pyDes + base64          |                                   |
| `encoded_salt`                            | base64                  |                                   |
| `ldap_pkcs12_base64`                      | pyDes + base64          | /etc/certs/opendj.pkcs12          |
| `ldap_ssl_cacert`                         | pyDes + base64          | /etc/certs/opendj.pem             |
| `ldap_ssl_cert`                           | pyDes + base64          | /etc/certs/opendj.crt             |
| `ldap_ssl_key`                            | pyDes + base64          | /etc/certs/opendj.key             |
| `ldap_truststore_pass`                    | base64                  |                                   |
| `pairwiseCalculationKey`                  | base64                  |                                   |
| `pairwiseCalculationSalt`                 | base64                  |                                   |
| `role_based_client_encoded_pw`            | pyDes + base64          | /etc/certs/jans-radius.jks        |
| `role_based_client_pw`                    | pyDes + base64          |                                   |
| `ssl_ca_cert`                             | base64                  | /etc/certs/ca.crt                 |
| `ssl_ca_key`                              | base64                  | /etc/certs/ca.key                 |
| `ssl_cert`                                | base64                  | /etc/certs/web_https.crt          |
| `ssl_csr`                                 | base64                  | /etc/certs/web_https.csr          |
| `ssl_key`                                 | base64                  | /etc/certs/web_https.key          |

## Example decoding secrets

!!! Note
    We assume Jans is installed in a namespace called `jans`

1. Get the `tls-certificate` from backend secret

    ```bash
    kubectl get secret tls-certificate -n jans -o yaml
    ```

1. Copy the value of the key you want to decode. For example:
    ```bash
    data:
      tls.crt: <encodedValue>
    ```

1. Base64 decode the value

    ```bash
    echo <encodedValue> | base64 -d #replace encodedValue with the value from the previous command
    ```

