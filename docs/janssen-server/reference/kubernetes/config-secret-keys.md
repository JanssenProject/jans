---
tags:
  - administration
  - reference
  - kubernetes
  - configmap
  - secret
---

## Overview

The `config` job creates a set of configuration (contains `secrets` and `configmaps`) used by all Janssen services.

## Configmaps

The `configmaps` store non-sensitive data as key-value pairs. 

To check the values of the configmaps on the current deployment run:

```bash
kubectl get configmap -n jans -o yaml
```

Note that each key in configmaps is based on the schema below:

```json
{
  "city": {
    "type": "string",
    "description": "Locality name (.e.g city)",
    "example": "Austin"
  },
  "country_code": {
    "type": "string",
    "minLength": 2,
    "maxLength": 2,
    "description": "Country name (2 letter code)",
    "example": "US"
  },
  "admin_email": {
    "type": "string",
    "format": "email",
    "description": "Email address",
    "example": "support@jans.io"
  },
  "hostname": {
    "type": "string",
    "description": "Fully qualified domain name (FQDN)",
    "example": "demoexample.jans.io"
  },
  "orgName": {
    "type": "string",
    "description": "Organization name",
    "example": "Janssen"
  },
  "state": {
    "type": "string",
    "description": "State or Province Name",
    "example": "TX"
  },
  "optional_scopes": {
    "type": "string",
    "default": "[]",
    "description": "List of optional scopes of components as string",
    "example": "[\"redis\", \"sql\"]"
  },
  "auth_sig_keys": {
    "type": "string",
    "default": "RS256 RS384 RS512 ES256 ES384 ES512 PS256 PS384 PS512",
    "description": "Signature keys to generate",
    "example": "RS256 RS384 RS512 ES256 ES384 ES512 PS256 PS384 PS512"
  },
  "auth_enc_keys": {
    "type": "string",
    "default": "RSA1_5 RSA-OAEP ECDH-ES",
    "description": "Encryption keys to generate",
    "example": "RSA1_5 RSA-OAEP ECDH-ES"
  },
  "init_keys_exp": {
    "type": "integer",
    "default": 48,
    "minimum": 1,
    "description": "Initial expiration time (in hours) for generated keys",
    "example": 24
  },
  "admin_inum": {
    "type": "string",
    "default": "",
    "description": "Inum for admin user",
    "example": "631e2b84-1d3d-4f28-9a9a-026a25febf44"
  },
  "admin_ui_client_id": {
    "type": "string",
    "default": "",
    "description": "Client ID of admin-ui app",
    "example": "631e2b84-1d3d-4f28-9a9a-026a25febf44"
  },
  "casa_client_id": {
    "type": "string",
    "default": "",
    "description": "Client ID of jans-casa app",
    "example": "1902.66bc89a1-075f-4a18-9349-a2908c1040e6"
  },
  "jans_idp_client_id": {
    "type": "string",
    "default": "",
    "description": "Client ID of jans-idp app",
    "example": "jans-f13013e3-e4a7-4709-8b50-df459f489cd3"
  },
  "jca_client_id": {
    "type": "string",
    "default": "",
    "description": "Client ID of jans-config-api app",
    "example": "1800.ca41fad2-6ab6-46b1-b4a9-3387992a8cb0"
  },
  "scim_client_id": {
    "type": "string",
    "default": "",
    "description": "Client ID of jans-scim app",
    "example": "1201.dd7e7733-b548-45ee-aed1-74e7b4065801"
  },
  "tui_client_id": {
    "type": "string",
    "default": "",
    "description": "Client ID of jans-tui app",
    "example": "2000.4a67fad3-24cd-4d56-b5a3-7cfb2e9fbb05"
  },
  "test_client_id": {
    "type": "string",
    "default": "",
    "description": "Client ID of test app",
    "example": "174143d2-f7f6-4bda-baa0-a6a8fd01b77a"
  },
  "kc_master_auth_client_id": {
    "type": "string",
    "default": "",
    "description": "Client ID of Keycloak master auth app",
    "example": "2103.22abf39d-f78f-4fb0-871e-dcb80bc1e43c"
  },
  "kc_saml_openid_client_id": {
    "type": "string",
    "default": "",
    "description": "Client ID of Keycloak SAML OpenID app",
    "example": "2101.70394974-82ec-481e-9493-e96d3cf8072f"
  },
  "kc_scheduler_api_client_id": {
    "type": "string",
    "default": "",
    "description": "Client ID of Keycloak scheduler API app",
    "example": "2102.d424af33-2069-4803-8426-4787af5fd933"
  },
  "token_server_admin_ui_client_id": {
    "type": "string",
    "default": "",
    "description": "Client ID of token server app",
    "example": "631e2b84-1d3d-4f28-9a9a-026a25febf44"
  },
  "auth_key_rotated_at": {
    "type": "string",
    "default": "",
    "description": "Timestamp of last auth keys regeneration",
    "example": "631e2b84-1d3d-4f28-9a9a-026a25febf44"
  },
  "auth_legacyIdTokenClaims": {
    "type": "string",
    "default": "false",
    "enum": [
      "false",
      "true"
    ],
    "description": "Enable legacy ID token claim",
    "example": "false"
  },
  "auth_openidScopeBackwardCompatibility": {
    "type": "string",
    "default": "false",
    "enum": [
      "false",
      "true"
    ],
    "description": "Enable backward-compat OpenID scope",
    "example": "false"
  },
  "auth_openid_jks_fn": {
    "type": "string",
    "default": "/etc/certs/auth-keys.jks",
    "description": "Path to keystore file contains private keys for jans-auth"
  },
  "auth_openid_jwks_fn": {
    "type": "string",
    "default": "/etc/certs/auth-keys.json",
    "description": "Path to JSON file contains public keys for jans-auth"
  },
  "default_openid_jks_dn_name": {
    "type": "string",
    "default": "CN=Janssen Auth CA Certificates",
    "description": "CommonName for jans-auth CA certificate"
  },
  "kc_admin_username": {
    "type": "string",
    "default": "admin",
    "description": "Admin username of Keycloak",
    "example": "admin"
  },
  "smtp_alias": {
    "type": "string",
    "default": "smtp_sig_ec256",
    "description": "Alias for SMTP entry in truststore",
    "example": "smtp_sig_ec256"
  },
  "smtp_signing_alg": {
    "type": "string",
    "default": "SHA256withECDSA",
    "description": "SMTP signing algorithm",
    "example": "SHA256withECDSA"
  }
}
```

## Secrets

The `secrets` store sensitive data as key-value pairs. 

To check the values of the secrets on the current deployment run :

```bash
kubectl get secret -n jans -o yaml
```

Note that each key in secrets is based on the schema below:

```json
{
  "admin_password": {
    "type": "string",
    "description": "Password for admin user"
  },
  "sql_password": {
    "type": "string",
    "default": "",
    "description": "Password for SQL (RDBMS) user"
  },
  "encoded_salt": {
    "type": "string",
    "default": "",
    "description": "Salt for encoding/decoding sensitive secret",
    "example": "hR8kBUtTxB25pDPCSHVRktAz"
  },
  "google_credentials": {
    "type": "string",
    "default": "",
    "description": "String contains Google application credentials",
    "example": "{\n  \"type\": \"service_account\",\n  \"project_id\": \"testing-project\"\n}\n"
  },
  "aws_credentials": {
    "type": "string",
    "default": "",
    "description": "String contains AWS shared credentials",
    "example": "[default]\naws_access_key_id = FAKE_ACCESS_KEY_ID\naws_secret_access_key = FAKE_SECRET_ACCESS_KEY\n"
  },
  "aws_config": {
    "type": "string",
    "default": "",
    "description": "String contains AWS config",
    "example": "[default]\nregion = us-west-1\n"
  },
  "aws_replica_regions": {
    "type": "string",
    "default": "",
    "description": "String contains AWS replica regions config",
    "example": "[{\"Region\": \"us-west-1\"}, {\"Region\": \"us-west-2\"}]\n"
  },
  "vault_role_id": {
    "type": "string",
    "default": "",
    "description": "Vault RoleID"
  },
  "vault_secret_id": {
    "type": "string",
    "default": "",
    "description": "Vault SecretID"
  },
  "kc_db_password": {
    "type": "string",
    "default": "",
    "description": "Password for Keycloak RDBMS user"
  },
  "admin_ui_client_encoded_pw": {
    "type": "string",
    "default": "",
    "description": "Encoded password for admin-ui client",
    "x-encoding": "3DES"
  },
  "admin_ui_client_pw": {
    "type": "string",
    "default": "",
    "description": "Password for admin-ui client"
  },
  "auth_jks_base64": {
    "type": "string",
    "default": "",
    "description": "Private keys (keystore) of jans-auth"
  },
  "auth_openid_jks_pass": {
    "type": "string",
    "default": "",
    "description": "Password of jans-auth keystore"
  },
  "auth_openid_key_base64": {
    "type": "string",
    "default": "",
    "description": "Public keys (JWKS) of jans-auth"
  },
  "casa_client_encoded_pw": {
    "type": "string",
    "default": "",
    "description": "Encoded password for jans-casa client",
    "x-encoding": "3DES"
  },
  "casa_client_pw": {
    "type": "string",
    "default": "",
    "description": "Password for jans-casa client"
  },
  "encoded_admin_password": {
    "type": "string",
    "default": "",
    "description": "Encoded password of admin",
    "x-encoding": "ldap_encode"
  },
  "jans_idp_client_secret": {
    "type": "string",
    "default": "",
    "description": "Client secret of jans-idp app"
  },
  "jans_idp_user_password": {
    "type": "string",
    "default": "",
    "description": "User password for jans-idp"
  },
  "jca_client_encoded_pw": {
    "type": "string",
    "default": "",
    "description": "Encoded password for jans-config-api client",
    "x-encoding": "3DES"
  },
  "jca_client_pw": {
    "type": "string",
    "default": "",
    "description": "Password for jans-config-api client"
  },
  "kc_admin_password": {
    "type": "string",
    "default": "",
    "description": "Admin password of Keycloak"
  },
  "kc_master_auth_client_encoded_pw": {
    "type": "string",
    "default": "",
    "description": "Client encoded secret of Keycloak master auth app",
    "x-encoding": "3DES"
  },
  "kc_master_auth_client_pw": {
    "type": "string",
    "default": "",
    "description": "Client secret of Keycloak master auth app"
  },
  "kc_saml_openid_client_encoded_pw": {
    "type": "string",
    "default": "",
    "description": "Client encoded secret of Keycloak SAML app",
    "x-encoding": "3DES"
  },
  "kc_saml_openid_client_pw": {
    "type": "string",
    "default": "",
    "description": "Client secret of Keycloak SAML app"
  },
  "kc_scheduler_api_client_encoded_pw": {
    "type": "string",
    "default": "",
    "description": "Client encoded secret of Keycloak scheduler API app",
    "x-encoding": "3DES"
  },
  "kc_scheduler_api_client_pw": {
    "type": "string",
    "default": "",
    "description": "Client secret of Keycloak scheduler API app"
  },
  "otp_configuration": {
    "type": "string",
    "default": "",
    "description": "OTP configuration string"
  },
  "pairwiseCalculationKey": {
    "type": "string",
    "default": "",
    "description": "Pairwise calculation key"
  },
  "pairwiseCalculationSalt": {
    "type": "string",
    "default": "",
    "description": "Pairwise calculation salt"
  },
  "scim_client_encoded_pw": {
    "type": "string",
    "default": "",
    "description": "Encoded password for jans-scim client",
    "x-encoding": "3DES"
  },
  "scim_client_pw": {
    "type": "string",
    "default": "",
    "description": "Password for jans-scim client"
  },
  "smtp_jks_base64": {
    "type": "string",
    "default": "",
    "description": "Private keys (keystore) of SMTP"
  },
  "smtp_jks_pass": {
    "type": "string",
    "default": "",
    "description": "Password of SMTP keystore"
  },
  "smtp_jks_pass_enc": {
    "type": "string",
    "default": "",
    "description": "Encoded password of SMTP keystore"
  },
  "ssl_ca_cert": {
    "type": "string",
    "default": "",
    "description": "SSL certificate for CA"
  },
  "ssl_ca_key": {
    "type": "string",
    "default": "",
    "description": "SSL key for CA"
  },
  "ssl_cert": {
    "type": "string",
    "default": "",
    "description": "SSL certificate for the FQDN"
  },
  "ssl_csr": {
    "type": "string",
    "default": "",
    "description": "SSL certificate signing request for the FQDN"
  },
  "ssl_key": {
    "type": "string",
    "default": "",
    "description": "SSL key for the FQDN"
  },
  "super_gluu_creds": {
    "type": "string",
    "default": "",
    "description": "SuperGluu credentials string"
  },
  "test_client_encoded_pw": {
    "type": "string",
    "default": "",
    "description": "Encoded password for test client",
    "x-encoding": "3DES"
  },
  "test_client_pw": {
    "type": "string",
    "default": "",
    "description": "Password for test client"
  },
  "token_server_admin_ui_client_encoded_pw": {
    "type": "string",
    "default": "",
    "description": "Encoded password for token server client",
    "x-encoding": "3DES"
  },
  "token_server_admin_ui_client_pw": {
    "type": "string",
    "default": "",
    "description": "Password for token server client"
  },
  "tui_client_encoded_pw": {
    "type": "string",
    "default": "",
    "description": "Encoded password for TUI client",
    "x-encoding": "3DES"
  },
  "tui_client_pw": {
    "type": "string",
    "default": "",
    "description": "Password for TUI client"
  },
  "redis_password": {
    "type": "string",
    "default": "",
    "description": "Password for Redis user"
  }
}
```

## Example decoding secrets

### Opening `base64-decoded` secrets
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

## Using Configuration Schema

As mentioned earlier, the `config` job creates configuration. Behind the scene, a Kubernetes' Secret object is created during the deployment to pre-populate `secrets` and `configmaps`.

### Default configuration

By default, the configuration only contains necessary `secrets` and `configmaps` to install Jans services.

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: jans-configuration-file
  namespace: jans
  labels:
    APP_NAME: configurator
type: Opaque
stringData:
  configuration.json: |-
    {
      "_configmap": {
        "hostname": "demoexample.jans.io",
        "country_code": "US",
        "state": "TX",
        "city": "Austin",
        "admin_email": "support@jans.io",
        "orgName": "Janssen",
        "auth_sig_keys": "RS256 RS384 RS512 ES256 ES384 ES512 PS256 PS384 PS512",
        "auth_enc_keys": "RSA1_5 RSA-OAEP ECDH-ES",
        "optional_scopes": "[\"redis\", \"sql\"]",
        "init_keys_exp": 48
      },
      "_secret": {
        "admin_password": "Test1234#",
        "sql_password": "Test1234#",
        "encoded_salt": "hR8kBUtTxB25pDPCSHVRktAz"
      }
    }
```

Note that `_secret` may contain other keys depending on persistence, secrets/configmaps backend, etc. See examples below:

1.  Secrets/configmaps backend is set to `google`:

    ```json
    "_secret": {
        "google_credentials": "{\n  \"type\": \"service_account\",\n  \"project_id\": \"testing-project\"\n}"
    }
    ```

1.  Secrets backend is set to `vault`:

    ```json
    "_secret": {
        "vault_role_id": "c41a15f4-abcd-1234-abcd-d306bf9f3eb6",
        "vault_secret_id": "a7ae191c-abcd-1234-abcd-3733dc6b0813"
    }
    ```

1. 	Secrets/configmaps backend is set to `aws`:

    ```json
    "_secret": {
        "aws_config": "[default]\nregion = us-west-1\n",
        "aws_credentials": "[default]\naws_access_key_id = FAKE_ACCESS_KEY_ID\naws_secret_access_key = FAKE_SECRET_ACCESS_KEY\n",
        "aws_replica_regions": "[{\"Region\": \"us-west-1\"}, {\"Region\": \"us-west-2\"}]\n"
    }
    ```

### Custom configuration

The default configuration is sufficient for most of the time. If there's a requirement to use custom or reusing existing configuration, user may create a custom Kubernetes object.

!!! Warning
    The custom configuration schema is a BETA feature. 

1.  Prepare YAML file:

    ```yaml
    # custom-configuration-schema.yaml
    apiVersion: v1
    kind: Secret
    metadata:
      name: custom-configuration-schema
      namespace: jans
    type: Opaque
    stringData:
      configuration.json: |-
        {
          "_configmap": {
            "hostname": "demoexample.jans.io",
            "country_code": "US",
            "state": "TX",
            "city": "Houston",
            "admin_email": "custom@example.com",
            "orgName": "custom-org",
            "optional_scopes": "[\"sql\"]"
          },
          "_secret": {
            "admin_password": "Custom1234#",
            "sql_password": "Custom1234#"
          }
        }
    ```

1.  Create Kubernetes secrets:

    ```bash
    kubectl -n jans apply -f custom-configuration-schema.yaml
    ```

1.  Specify the secret in `values.yaml`:

    ```yaml
    global:
      cnConfiguratorCustomSchema:
        secretName: custom-configuration-schema
    ```

1.  Install the Jans charts.

## Encrypting Configuration Schema

The configuration schema can be encrypted by specifying 32 alphanumeric characters to `cnConfiguratorKey` attribute (default value is an empty string).
The encryption is using [Helm-specific](https://helm.sh/docs/chart_template_guide/function_list/#encryptaes) implementation of AES-256 CBC mode.

```yaml
global:
  cnConfiguratorKey: "VMtVyFha8CfppdDGQSw8zEnfKXRvksAD"
```

The following example is what an encrypted default configuration looks like:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: jans-configuration-file
  namespace: jans
stringData:
  configuration.json: |-
    sxySo+redacted+generated+by+helm/TNpE5PoUR2+JxXiHiLq8X5ibexJcfjAN0fKlqRvU=
```

If using custom configuration, user will need to generate the string using [sprig-aes](https://pypi.org/project/sprig-aes/) CLI and paste into a YAML manifest.

```yaml
# custom-configuration-schema.yaml
apiVersion: v1
kind: Secret
metadata:
  name: custom-configuration-schema
  namespace: jans
type: Opaque
stringData:
  configuration.json: |-
    sxySo+redacted+generated+by+sprigaes+JxXiHiLq8X5ibexJcfjAN0fKlqRvU=
```
