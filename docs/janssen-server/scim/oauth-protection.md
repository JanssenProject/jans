---
tags:
  - administration
  - scim
---

# OAuth protection

The SCIM API endpoints are by default protected by (Bearer) OAuth 2.0 tokens. Depending on the operation, these tokens must have certain scopes for the operations to be authorized. The table below summarizes this fact:

|Scope|Description|
|-|-|
|`https://jans.io/scim/users.read`|Query/Search user resources|
|`https://jans.io/scim/users.write`|Modify user resources|
|`https://jans.io/scim/groups.read`|Query group resources|
|`https://jans.io/scim/groups.write`|Modify group resources|
|`https://jans.io/scim/fido.read`|Query fido resources|
|`https://jans.io/scim/fido.write`|Modify fido resources|
|`https://jans.io/scim/fido2.read`|Query fido 2 resources|
|`https://jans.io/scim/fido2.write`|Modify fido 2 resources|
|`https://jans.io/scim/bulk`|Send requests to the bulk endpoint|
|`https://jans.io/scim/all-resources.search`|Access the root `.search` endpoint|

Correspondence of endpoints vs. scopes can be found in the SCIM OpenAPI descriptor you can find [here](
https://github.com/JanssenProject/jans/blob/main/jans-scim/server/src/main/resources/jans-scim-openapi.yaml).

## Client details

To obtain valid tokens, you would normally have to register a client (in the server) capable of obtaining tokens with the scopes needed. Registration is out of the scope of this document, however you can leverage the client that is already bundled in your server for SCIM interaction. With this client you can issue tokens with any SCIM-related scope.

Let's obtain the credentials of this client first. In TUI, navigate to `Auth Server` > `Clients`. In the search field type **SCIM** (uppercase). Highlight the row that matches a client named "SCIM Client" and press Enter.

From the "Basic" section, grab the "client id" and "client secret". This secret is encrypted, to decrypt it, in a terminal run `/opt/jans/bin/encode.py -D ENCRYPTED-SECRET-HERE`.

## Getting a token

This is a `curl` example of how to get a token valid for retrieving and modifying users (line breaks added for readability). Note the use of white space to separate scope names.

```bash title="Command"
curl -k -u 'CLIENT_ID:DECRYPTED_CLIENT_SECRET' -k -d grant_type=client_credentials -d 
    scope='https://jans.io/scim/users.read https://jans.io/scim/users.write' 
    https://your-jans-server/jans-auth/restv1/token
```

Grab the "access_token" from the obtained response.

## Issue a request to the service

The below is a curl example of how to call an operation by passing the previously obtained token (line breaks added for readability):

```bash title="Command"
curl -k -G -H 'Authorization: Bearer ACCESS_TOKEN' --data-urlencode 'filter=displayName co "Admin"' 
    https://your-jans-server/jans-scim/restv1/v2/Users

```

The output should show valid SCIM (JSON) output. Account the access token is short-lived: once it expires you will get a status response of 401 and need to re-request the token as in the previous step.

## Deactivating OAuth mode

OAuth tokens usage is a safe, standardized approach for controlling access to resources. If for some reason you want to turn off protection you can do so by activating bypass mode: in TUI, navigate to SCIM tab and under "Protection Mode" select "BYPASS". Finally press "Save".

Caution: keep in mind that with "BYPASS" you should add some sort of protection to your endpoints, say, at the network level to avoid anyone to mess with your user base among other resources.
