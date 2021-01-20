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

In Jans-SCIM endpoints are protected with bearer OAuth 2.0 tokens; this is a safe, standardized approach for controlling access to resources.

Depending on the scopes associated to a token, you will be granted (or denied) access to perform certain operations. The following lists the available scopes:

|Scope|Actions allowed|
|-|-|
|https://jans.io/scim/users.read|Query user resources|
|https://jans.io/scim/users.write|Modify user resources|
|https://jans.io/scim/groups.read|Query group resources|
|https://jans.io/scim/groups.write|Modify group resources|
|https://jans.io/scim/fido.read|Query fido resources|
|https://jans.io/scim/fido.write|Modify fido resources|
|https://jans.io/scim/fido2.read|Query fido 2 resources|
|https://jans.io/scim/fido2.write|Modify fido 2 resources|
|https://jans.io/scim/all-resources.search|Access the root .search endpoint| 
|https://jans.io/scim/bulk|Send requests to the bulk endpoint|

In order to facilitate the process of getting an access token, your Janssen installation already bundles an OAuth client named "SCIM client" with support for all the scopes above. This client uses the `client_credentials` grant type and `client_secret_basic` mechanism to authenticate to the token endpoint.

To exercise a finer grained control over access, you may register multiple clients with limited scopes and deliver the client credentials as needed to your developers. 

### Example of usage

In the following example we leverage the OAuth SCIM Client to get a token and issue a call to an endpoint. Log into your Janssen machine and use the commands provided:

### Get client credentials

- Get the client id: Run `cat /opt/jans/jans-cli/config.ini | grep 'scim_client_id'`
- Get the encrypted client secret: Run `cat /opt/jans/jans-cli/config.ini | grep 'scim_client_secret_enc'`
- Decrypt the secret: Run `/opt/jans/bin/encode.py -D ENCRYPTED-SECRET-HERE`

### Get a token

Request a token with the scopes necessary to perform the intended operations. Use a white space to separate scopes. For instance: 

```
curl -u 'CLIENT_ID:DECRYPTED_CLIENT_SECRET' -k -d grant_type=client_credentials -d scope='https://jans.io/scim/users.read https://jans.io/scim/users.write' https://your-jans-server/jans-auth/restv1/token
```

Grab the "access_token" from the response.

### Issue a request to the service

Using the token, call your SCIM operation(s):

```
curl -i -k -G -H 'Authorization: Bearer ACCESS_TOKEN' -d count=2 --data-urlencode 'filter=displayName co "Admin"' https://buster.gluu.info/jans-scim/restv1/v2/Users
```

The ouput should show valid SCIM (JSON) output. Account the access token is short lived: once it expires you will get a status response of 401 and need to re-request the token as in the previous step.

## API consumption

The Jans-SCIM API is documented using [Open API](https://www.openapis.org) version 3.0 as well as the swagger 2.0 specification. Find the yaml documents [here](https://github.com/JanssenProject/jans-scim/tree/master/server/src/main/resources). You can quickly generate client stubs in a variety of languages and frameworks with [Swagger code generator](https://swagger.io/tools/swagger-codegen).

### Java client

There is a Java-based [client](https://github.com/JanssenProject/jans-scim/tree/master/client) ready to use. This client facilitates service consumption and abstracts out the complexities of access. It supports OAuth clients that use client_secret_basic, client_secret_post and private_key_jwt methods to authenticate to the token endpoint. Some example follow:


