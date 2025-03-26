---
tags:
  - administration
  - user management
  - scim
---

# Using SCIM

## SCIM User Management

SCIM is a specification designed to reduce the complexity of user management operations by providing a common user schema and the patterns for exchanging such schema using HTTP in a platform-neutral fashion. The aim of SCIM is achieving interoperability, security, and scalability in the context of identity management.

For your reference, the current version of the standard is governed by the following documents: [RFC 7642](https://datatracker.ietf.org/doc/html/rfc7642), [RFC 7643](https://datatracker.ietf.org/doc/html/rfc7643), and [RFC 7644](https://datatracker.ietf.org/doc/html/rfc7644).


## Installation 

The API is available as a component of Jans Server. Upon [installation](https://docs.jans.io/v1.0.14/admin/install/vm-install/vm-requirements/) you can select if you want SCIM included in your environment. To add SCIM post-install do the following:

1. Run `python3 /opt/jans/jans-setup/setup.py --install-scim`

## About API endpoints

Throughout this document, you will notice endpoints are prefixed with path: 
`/jans-scim/restv1/v2`

## API Protection

Clearly, this API must not be anonymously accessed. However, the basic SCIM standard does not define a specific mechanism to prevent unauthorized requests to endpoints. There are just a few guidelines in section 2 of [RFC 7644](https://datatracker.ietf.org/doc/html/rfc7644) concerned with authentication and authorization.

* OAUTH, This is the default and recommended mechanism
* BYPASS

Depending on the scopes associated to a token, you will be granted (or denied) access to perform certain operations. The following lists the available scopes:

|Scope|Actions allowed|
|-|-|
|`https://jans.io/scim/users.read`|Query user resources|
|`https://jans.io/scim/users.write`|Modify user resources|
|`https://jans.io/scim/groups.read`|Query group resources|
|`https://jans.io/scim/groups.write`|Modify group resources|
|`https://jans.io/scim/fido.read`|Query fido resources|
|`https://jans.io/scim/fido.write`|Modify fido resources|
|`https://jans.io/scim/fido2.read`|Query fido 2 resources|
|`https://jans.io/scim/fido2.write`|Modify fido 2 resources|
|`https://jans.io/scim/all-resources.search`|Access the root .search endpoint| 
|`https://jans.io/scim/bulk`|Send requests to the bulk endpoint|

In order to facilitate the process of getting an access token, your Janssen installation already bundles an OAuth client named "SCIM client" with support for all the scopes above. This client uses the `client_credentials` grant type and `client_secret_basic` mechanism to authenticate to the token endpoint.

## Where to locate SCIM-related logs

Please see [here](https://docs.jans.io/v1.0.14/admin/scim/logs/) besides 

* SCIM log is located at `/opt/jans/jetty/jans-scim/logs/scim.log`
* If you use SCIM custom script aslo see `/opt/jans/jetty/jans-scim/logs/scim_script.log`

## API documentation at a glance

[SCIM API](https://docs.jans.io/v1.0.14/admin/reference/openapi/) doc page describes about our implementation of SCIM. The API has also been documented using OpenAPI (swagger) specification for the interested. Find yaml files [here](https://github.com/JanssenProject/jans/blob/main/jans-scim/server/src/main/resources/jans-scim-openapi.yaml).


## Working in OAuth mode

To know more about OAuth protection mode please visit [here](https://docs.jans.io/v1.0.14/admin/scim/oauth-protection/).
The SCIM API endpoints are by default protected by (Bearer) OAuth 2.0 tokens. Depending on the operation, these tokens must have certain scopes for the operations to be authorized. We need a client to get Bearer token. 
### Get SCIM Client


You can refer to [here](../../janssen-server/config-guide/scim-config/user-config.md#get-scim-client) for this topic



### Get Access token

You can refer to [here](../../janssen-server/config-guide/scim-config/user-config.md#get-access-token) for this topic

### Retrive existing User 

You can refer to [here](../../janssen-server/config-guide/scim-config/user-config.md#retrive-existing-user) for this topic

## Creating Resource 
### Create an User

You can refer to [here](../../janssen-server/config-guide/scim-config/user-config.md#create-an-user) for this topic

### Updating a User(PUT)

You can refer to [here](../../janssen-server/config-guide/scim-config/user-config.md#updating-a-userput) for this topic

### Updating a User (PATCH)


You can refer to [here](../../janssen-server/config-guide/scim-config/user-config.md#updating-a-user-patch) for this topic


### Deleting Users

You can refer to [here](../../janssen-server/config-guide/scim-config/user-config.md#deleting-users) for this topic

## How is SCIM data stored?

You can refer to [here](../../janssen-server/scim/monitoring.md#how-is-scim-data-stored) for this topic

## FIDO Devices

You can refer to [here](../../janssen-server/fido/monitoring.md#fido-devices) for this topic.

## FIDO 2 devices

You can refer to [here](../../janssen-server/fido/monitoring.md#fido2-devices) for this topic.

## Potential performance issues with Group endpoints

In SCIM a group resource basically consists of an identifier, a display name, and a collection of members associated to it. Also, every member is made up of a user identifier, his display name, and other attributes. As a consequence, retrieving group information requires making a correlation with existing user data. Since Gluu database model does not follow a relational database pattern this may entail a considerable amount of user queries when groups contain thousands of members.

While this could have been workarounded by storing members' display names inside group entries, this brings additional problems to deal with.

Another source of potential overhead stems from creation and modification of groups where many new users are associated to a given group: by default checks are made to guarantee only existing users are attached to groups, thus requiring continuous database queries.

Currently there are two ways to lower the amount of database lookups required for SCIM group operations:

* Explicitly excluding display names from responses
* Pass the overhead bypass flag to skip members validations

The first approach consists of using the query parameter `excludedAttributes` (see [RFC 7644](https://datatracker.ietf.org/doc/html/rfc7644)) so that display names are neither retrieved from database nor sent in responses. A value like `members.display` does the job. Note the query parameter attributes can also be used for this purpose, for example with a value like `members.value` that will output only members' identifiers and ignore other non-required attributes.

This approach is particularly useful in search and retrievals when users' display names are not needed.

The second is a stronger approach that turns off validation of incoming members data: if the usage of a POST/PUT/PATCH operation implies adding members, their existence is not verified, they will simply get added. Here, the client application is responsible for sending accurate data. To use this approach add a query or header parameter named `Group-Overhead-Bypass` with any value. Note under this mode of operation:

* Display names are never returned regardless of `attributes` or `excludedAttributes` parameters values
* Remove/replace patch operations that involve display names in path filters are ignored, eg: `"path": "members[value eq \"2819c223\" or display eq \"Joe\"]"`


## User Registration Process with SCIM
SCIM service has many use cases. One interesting and often arising is that of coding your own user registration process. With your SCIM endpoints you can build a custom application to maintain user entries in your database.


### Important Considerations
Here, you have some useful tips before you start:

1. Choose a toolset you feel comfortable to work with. Keep in mind that you have to leverage the capabilities of your language/framework to issue complex HTTPS requests. Be sure that:

      * You will be able to use at least the following verbs: GET, POST, PUT, and DELETE

      * You can send headers in your requests as well as reading them from the service response

2. If not supported natively, choose a library to facilitate JSON content manipulation. As you have already noticed we have been dealing with JSON for requests as well as for responses. Experience shows that being able to map from objects (or data structures) of your language to Json and viceversa helps saving hours of coding.

3. Shape your data model early. List the attributes your application will operate upon and correlate with those found in the SCIM user schema. You can learn about the schema in [RFC 7644](https://datatracker.ietf.org/doc/html/rfc7644). At least, take a look at the JSON-formatted schema that your Jans Server shows: visit `https://<host-name>/jans-scim/restv1/v2/Schemas/urn:ietf:params:scim:schemas:core:2.0:User`

4. You will have to manipulate database contents very often as you develop and run tests, thus, find a suitable tool for the task. In the case of LDAP, a TUI client is a good choice.

5. Always check your [logs](#where-to-locate-scim-related-logs).

6. In this user management guide with SCIM, we have already touched upon the fundamentals of SCIM in Jans Server and shown a good amount of sample requests for manipulation of user information. However, keep in mind the SCIM spec documents are definitely the key reference to build working request messages, specially [RFC 7643](https://datatracker.ietf.org/doc/html/rfc7643), and [RFC 7644](https://datatracker.ietf.org/doc/html/rfc7644).

