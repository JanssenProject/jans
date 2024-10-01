---
tags:
- administration
- client
- management
---

# Client Management

## Background

A "client" is a piece of software either acting autonomously or on behalf of
a person. The OAuth framework defines the term client
[here](https://datatracker.ietf.org/doc/html/rfc6749#section-1.1). OpenId Connect
[clarifies](https://openid.net/specs/openid-connect-core-1_0.html#Introduction)
that:
`OAuth 2.0 Clients using OpenID Connect are also referred to as Relying Parties (RPs)`

Don't confuse a Client with either the Person or the Browser!
![](../../../assets/federated_identity_actors.png)

## Client Security

OpenID allows you to use as much security as you need. To a large extent, the
security of your implementation depends on what client features you select.
For example, let's just consider how the [client authenticates](client-authn.md) itself to Jans Auth
Server, which is defined by the `token_endpoint_auth_method` in OpenID Connect
[Client Metadata](https://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata).

Obviously, using asynchronous secrets for authentication is more secure. The
client configuration also determines what [crypto](client-configuration.md#cryptography) is used for signing and
encryption of tokens, what scopes are available to the client (which determines
the extent of access to APIs), what [grants](client-configuration.md#grants) are available,  what is a valid
[redirect_uri](client-configuration.md#redirect-uri), timeouts, whether to use a value or reference token, whether to
expire the client, and several other options that impact security.

## Client Management Tools
A client can be created (and managed) by using one of the following tools offered by the Jans Auth server:

* [Jans Config API](../../config-guide/config-tools/config-api/README.md)
* [Command Line Tool (CLI)](../../config-guide/config-tools/jans-cli/README.md)
* [Jans Text UI (TUI)](../../config-guide/config-tools/jans-tui/README.md)
* [OpenID Connect Dynamic Client Registration](https://openid.net/specs/openid-connect-registration-1_0.html)

The choice of tool should be made based on your business requirement. 
- For *ad hoc* creation, the TUI is great. 
- If you need to quickly script client creation (e.g. in a bash script) use the CLI 
- Use `curl` to call the Jans Config API. 
- To allow apps to register as OIDC clients, without a manual process, the OpenID
Connect Dynamic Client Registration (DCR) can be used.

### A. OpenID Dynamic Client Registration

Jans Auth server publishes the `registration_endpoint` in the OpenID
configuration JSON response, which you can find at `.well-known/openid-configuration`
in your specific deployment. Typically, it is
`https://{hostname}/jans-auth/restv1/register`

The OpenApi specification for [/registration](https://gluu.org/swagger-ui/?url=https://raw.githubusercontent.com/JanssenProject/jans/vreplace-janssen-version/jans-auth-server/docs/swagger.yaml#/Registration) documents Jans Auth Server's specific implementation,
which aligns with the requirements of OpenID Connect dynamic client
registration. Also, check the
[Registration Endpoint documentation](../endpoints/client-registration.md) for
more details on the steps involved in dynamic client registration.

### B. Jans-CLI

Below is a one liner to add a client.

```
./config-cli-tui.pyz --host $FQDN --client-id $MY_CLIENT_ID \
--client-secret $MY_CLIENT_SECRET --no-tui \
--operation-id=post-oauth-openid-client  --data=my_client.json
```

For more information about how to use
the Jans-CLI, see the [docs](../../config-guide/config-tools/jans-cli/README.md)

### C. TUI

To start registering a new client, navigate to
`Auth Server`->`Clients`->`Add Client`.  This brings up a screen as shown below
with various sections to input client details.

![](../../../assets/Jans_TUI_Auth_Server_Add_new_client.png)

Refer to complete documentation [here](../../config-guide/config-tools/jans-tui/README.md)

### D. Using curl commands

To add a client via `curl`, see the information on the
[curl documentation page](../../config-guide/config-tools/curl-guide.md).


