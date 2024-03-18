---
tags:
  - administration
  - link
  - keycloak
---

# Jans Keycloak Link

The Jans Keycloak Link is a [Jans Link](README.md) module that provides 
synchronization services to update the Janssen User Store from an external 
Keycloak instance.

Jans Keycloak Link accesses Keycloak data via Keycloak API. A new `confidential`
client needs to be created on Keycloak in order to authorise Jans Keycloak Link
for API access. The client can be configured to use one of the two 
authentication mechanisms:
- [Client Credentials Grant](#using-client-credentials-grant)
- [Resource Owner Password Credentials Grant](#using-resource-owner-password-credentials-grant)

## Using Client Credentials Grant

### Configure Client on Keycloak

- Create a new OpenId Connect client from Keycloak administration console
- Configure this client as `confidential` access type by enabling `client 
  authentication`
- Enable `Service Accounts Enabled` flag, which enables client credentials grant
  ![](../../assets/jans-kc-link-client-2.png)
- Go to tab `Service accounts roles`, assign role `admin` to the client using 
  `Assign role` button
  ![](../../assets/jans-kc-link-client-4.png)
- Keep a note of the client ID and client secret. This detail will be required to be added
  to the Janssen server

### Configure Jans Keycloak Link Module

On the Janssen server, Jans Keycloak Link module configuration need to be
updated to be able to connect with Keycloak server.

- Encode the client secret with jans command
  ```shell
  /opt/jans/bin/encode.py {String to encrypt}
  ```
- Using [TUI](../config-guide/config-tools/jans-tui/README.md), update the 
  Jans KC Link module configuration as shown below:
  ![](../../assets/tui-kc-link-kc-config-client-cred.png)

## Using Resource Owner Password Credentials Grant

!!! Note
      Use of this grant type is generally discouranged and [removed from OAuth
      2.1](https://datatracker.ietf.org/doc/html/draft-ietf-oauth-v2-1-07#name-differences-from-oauth-20).

### Configure Client on Keycloak

- Create a new OpenId Connect client from Keycloak administration console
- Configure this client as `confidential` access type by enabling `client
  authentication`
  ![](../../assets/jans-kc-link-client-2.png)
- Create a user in the Keycloak server. The user should have permissions to 
  access Keycloak API in the Keycloak. For the instructions in this document,
  We will use the default Keycloak user which is `admin`.    

### Configure Jans Keycloak Link Module 

- Encode the user password with jans command
  ```shell
  /opt/jans/bin/encode.py {String to encrypt}
  ```
- Add these values to

  TODO: Here we need to list steps that will update the janssen data store with
  keycloak configuration as below (described in this [comment](https://github.com/JanssenProject/jans/issues/6280#issuecomment-1765091635))
  and taken implemented by [this issue](https://github.com/JanssenProject/jans/issues/7667)

```json
"keycloakConfiguration": {
 		"serverUrl": "keycloak-server-url",
 		"realm": "keycloak-realm",
 		"clientId": "id-of-client-on-keycloak",
 		"clientSecret": "",
 		"grantType": "password",
 		"username": "admin",
 		"password": "{check above step 4}"
 	}
```

## Test The Integration

To check if the integration is working, you can create a user on Keycloak server.
This user should reflect in Janssen Server after polling interval has passed.

Use [TUI](../config-guide/config-tools/jans-tui/README.md) to see the list of 
available users in Janssen Server.

## Want to contribute?

If you have content you'd like to contribute to this page in the meantime, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).