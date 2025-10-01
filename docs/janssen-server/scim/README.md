---
tags:
- administration
- scim
---

# SCIM Admin Guide

**S**ystem for **C**ross-domain **I**dentity **M**anagement, in short **SCIM**, is a specification that simplifies the exchange of user identity information across different domains. The Janssen Server provides an implementation for the SCIM specification.

The specification defines reference schemas for users and groups along with REST API to manage them. For more details, refer to the current version of the specification governed by the following documents: [RFC 7642](https://tools.ietf.org/html/rfc7642), [RFC 7643](https://tools.ietf.org/html/rfc7643), and [RFC 7644](https://tools.ietf.org/html/rfc7644).

Developers can think of **SCIM** merely as a **REST API** with endpoints exposing **CRUD** functionality (create, read, update, and delete).

This section covers how to configure, protect, and monitor the Janssen Server SCIM module and its APIs.

## Installation

The API is available as a component of the Janssen Server. Upon [installation](../install/vm-install/vm-requirements.md) you can select if you want SCIM included in your environment. To add SCIM post-install do the following:

```bash title="Command"
python3 /opt/jans/jans-setup/setup.py --install-scim
```

## About API endpoints

Throughout this document, you will notice endpoints are prefixed with path: `/jans-scim/restv1/v2`

## API Protection

Clearly, this API must not be anonymously accessed. However, the basic SCIM standard does not define a specific mechanism to prevent unauthorized requests to endpoints. There are just a few guidelines in section 2 of [RFC 7644](https://datatracker.ietf.org/doc/html/rfc7644) concerned with authentication and authorization.

* OAUTH, This is the default and recommended mechanism
* BYPASS

To know more about OAuth protection mode please visit [here](./oauth-protection.md). The SCIM API endpoints are by default protected by (Bearer) OAuth 2.0 tokens. Depending on the operation, these tokens must have certain scopes for the operations to be authorized. We need a client to get Bearer token.

## API documentation at a glance

[SCIM API](../../janssen-server/reference/openapi.md) doc page describes about our implementation of SCIM. The API has also been documented using OpenAPI (swagger) specification for the interested.

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

5. Always check your [logs](./logs.md).

6. In this user management guide with SCIM, we have already touched upon the fundamentals of SCIM in Jans Server and shown a good amount of sample requests for manipulation of user information. However, keep in mind the SCIM spec documents are definitely the key reference to build working request messages, specially [RFC 7643](https://datatracker.ietf.org/doc/html/rfc7643), and [RFC 7644](https://datatracker.ietf.org/doc/html/rfc7644).