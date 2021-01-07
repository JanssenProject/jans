# Jans-SCIM

A component of the Janssen Project that implements a standards-compliant SCIM service.

SCIM is a specification designed to reduce the complexity of user management 
operations by providing a common user schema and the patterns for exchanging such 
schema using HTTP in a platform-neutral fashion. The aim of SCIM is achieving 
interoperability, security, and scalability in the context of identity management.

For your reference, the current version of the standard is governed by the following 
documents: [RFC 7642](https://tools.ietf.org/html/rfc7642), [RFC 7643](https://tools.ietf.org/html/rfc7643), and [RFC 7644](https://tools.ietf.org/html/rfc7644).

**Jans-SCIM** can be seen merely as a **REST API** with endpoints exposing 
**CRUD** functionality (create, read, update and delete) for managing identity resources such as users, groups, and fido devices.

## About endpoints protection

In Jans-SCIM you can protect your endpoints with UMA (a profile of [OAuth 2.0](http://tools.ietf.org/html/rfc6749)); this is a safe and standardized approach for controlling access to web resources. Alternatively, you can temporarily enable the test mode where the process to obtain an access token is straightforward so it is easier to start interacting with your service, as well as learning about SCIM.


### Test mode

#### Prereqs

Activating test mode requires a small edition in the underlying LDAP. To ease the process install a GUI LDAP client in your development machine. Several options recommended:

- [Apache DS](http://directory.apache.org/studio) (requires Java runtime)
- [JXExplorer](http://www.jxplorer.org/) (requires Java runtime)
- [LDAP administrator](https://www.ldapadministrator.com) (windows only)

Once installed, establish a temporary tunnel to your LDAP, for instance:

```
ssh -L 1636:localhost:1636 root@jans-host
```

This requires SSH server and client libraries available in both your janssen host and development machine respectively.

#### Connect to LDAP

Establish a connection using the following:

- Port: 1636
- Encryption method: SSL
- Authentication method: simple
- Base DN: `o=jans`
- Bind DN: `cn=directory manager`
- Bind password: As provided in Janssen installation

#### Set scimTestMode

Navigate in the LDAP tree to `ou=jans-scim,ou=configuration,o=jans` and edit attribute `jansConfDyn` (this is JSON content): locate `scimTestMode` property and set it to `true`. Save the change

Restart `jans-scim`, eg. `systemctl restart jans-scim`.

#### Register an OpenID client

Create a client from which you can obtain access tokens, eg:

```
curl -k -i -H 'Content-Type: application/json' -d '{"application_type": "native", "token_endpoint_auth_method": "client_secret_basic", "client_name": "SCIM test client", "scope": "openid", "grant_types": ["client_credentials"], "response_types": ["token"], "redirect_uris":["https://dummy.com"] }' https://jans-host-name/jans-auth/restv1/register
```

The response will contain an ID and a secret. Keep those for later use.

#### Get a token

Using the client credentials get an access token from `jans_auth` token endpoint, eg.

```
curl -u '<client_id>:<client_secret>' -k -d grant_type=client_credentials https://jans-host-name/jans-auth/restv1/token
```
(replace with proper values in the placeholders).

#### Make a call to a SCIM endpoint

With the previously obtained access token, issue a call to an endpoint. The following makes a query to the users endpoint for users whose display name contains the word "admin":

```
curl -i -k -G -H 'Authorization: Bearer <access_token>' -d count=10 --data-urlencode 'filter=displayName co "Admin"' https://jans-host-name/jans-scim/restv1/v2/Users
```

The ouput should show valid SCIM (JSON) output. Account the access token is short lived: once it expires you will get a status response of 401 and need to re-request the token as in the previous step.

## API consumption

A Java-based [client](https://github.com/JanssenProject/jans-scim/tree/master/client) is provided. This client facilitates service consumption and abstracts out the complexities of access. 

The Jans-SCIM API is documented using [Open API](https://www.openapis.org) version 3.0 as well as the swagger 2.0 specification. Find the yaml documents [here](https://github.com/JanssenProject/jans-scim/tree/master/server/src/main/resources). You can quickly generate client stubs in a variety of languages and frameworks with [Swagger code generator](https://swagger.io/tools/swagger-codegen).
