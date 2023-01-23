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
| `couchbaseTrustStoreFn`                       | `/etc/certs/couchbase.pkcs12`                      |
| `country_code`                                | `US`                                               |
| `default_openid_jks_dn_name`                  | `CN=Janssen Auth CA Certificates`                  |
| `hostname`                                    | `demoexample.jans.io`                              |
| `jca_client_id`                               | `1800.ca41fad2-6ab6-46b1-b4a9-3387992a8cb0`        |
| `ldap_binddn`                                 | `cn=directory manager`                             |
| `ldap_init_host`                              | `localhost`                                        |
| `ldap_init_port`                              | `1636`                                             |
| `ldap_port`                                   | `1389`                                             |
| `ldap_site_binddn`                            | `cn=directory manager`                             |
| `ldapTrustStoreFn`                            | `/etc/certs/opendj.pkcs12`                         |
| `ldaps_port`                                  | `1636`                                             |
| `optional_scopes`                             | `["fido2", "scim", "sql"]`                         |
| `orgName`                                     | `Janssen`                                          |
| `tui_client_id`                               | `2000.4a8f3e8b-96b0-435a-8427-a287c242f4d9`        |
| `scim_client_id`                              | `12`                                               |
| `state`                                       | `TX`                                               |

## Jans Secrets

| Key                                       | Encode/Decode           | File                              |
| ----------------------------------------- | ----------------------- | --------------------------------- |
| `auth_jks_base64`                         | base64                  |                                   |
| `auth_openid_jks_pass`                    | base64                  |                                   |
| `auth_openid_key_base64`                  | base64                  |                                   |
| `couchbase_password`                      | base64                  |                                   |
| `couchbase_shib_user_password`            | base64                  |                                   |
| `couchbase_superuser_password`            | base64                  |                                   |
| `encoded_admin_password`                  | ldap_encode + base64    |                                   |
| `encoded_ldapTrustStorePass`              | pyDes + base64          |                                   |
| `encoded_ox_ldap_pw`                      | pyDes + base64          |                                   |
| `encoded_salt`                            | base64                  |                                   |
| `jca_client_encoded_pw`                   | pyDes + base64          |                                   |
| `jca_client_pw`                           | pyDes + base64          |                                   |
| `ldap_pkcs12_base64`                      | pyDes + base64          | /etc/certs/opendj.pkcs12          |
| `ldap_ssl_cacert`                         | pyDes + base64          | /etc/certs/opendj.pem             |
| `ldap_ssl_cert`                           | pyDes + base64          | /etc/certs/opendj.crt             |
| `ldap_ssl_key`                            | pyDes + base64          | /etc/certs/opendj.key             |
| `ldap_truststore_pass`                    | base64                  |                                   |
| `otp_configuration`                       | base64                  |                                   |
| `pairwiseCalculationKey`                  | base64                  |                                   |
| `pairwiseCalculationSalt`                 | base64                  |                                   |
| `tui_client_encoded_pw`                   | pyDes + base64          |                                   |
| `tui_client_pw`                           | pyDes + base64          |                                   |
| `scim_client_encoded_pw`                  | pyDes + base64          |                                   |
| `scim_client_pw`                          | pyDes + base64          |                                   |
| `sql_password`                            | base64                  |                                   |
| `ssl_ca_cert`                             | base64                  | /etc/certs/ca.crt                 |
| `ssl_ca_key`                              | base64                  | /etc/certs/ca.key                 |
| `ssl_cert`                                | base64                  | /etc/certs/web_https.crt          |
| `ssl_csr`                                 | base64                  | /etc/certs/web_https.csr          |
| `ssl_key`                                 | base64                  | /etc/certs/web_https.key          |
| `super_gluu_creds`                        | base64                  |                                   |

## Example decoding secrets

### Opening `base64 decoded` secrets
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

### Opening `/etc/certs/opendj.pkcs12` file

!!! Note
    We assume Jans is installed in a namespace called `jans`

1. Make a directory called `delete_me`

    ```bash
    mkdir delete_me && cd delete_me
    ```
   
1. Get the `ldap_truststore_pass`  from backend secret and save `ldap_truststore_pass`  in a file called `ldap_truststore_pass`

    ```bash
    kubectl get secret cn -o json -n jans | grep '"ldap_truststore_pass":' | sed -e 's#.*:\(\)#\1#' | tr -d '"' | tr -d "," | tr -d '[:space:]' > ldap_truststore_pass
    ```

1. Base64 decode the `ldap_truststore_pass` and save the decoded `ldap_truststore_pass` in a file called `ldap_truststore_pass_decoded`

    ```bash
    base64 -d ldap_truststore_pass > ldap_truststore_pass_decoded
    ```

1. Use `ldap_truststore_pass_decoded` to unlock `/etc/certs/opendj.pkcs12` in opendj pod.

    ```bash
    keytool -list -v -keystore /etc/certs/opendj.pkcs12 --storepass ldap_truststore_pass_decoded
    ```
    
