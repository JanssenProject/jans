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

## API consumption

A Java-based [client](https://github.com/JanssenProject/jans-scim/tree/master/client) is provided. This client facilitates service consumption and abstracts out the complexities of access. 

The Jans-SCIM API is documented using [Open API](https://www.openapis.org) version 3.0 as well as the swagger 2.0 specification. Find the yaml documents [here](https://github.com/JanssenProject/jans-scim/tree/master/server/src/main/resources). You can quickly generate client stubs in a variety of languages and frameworks with [Swagger code generator](https://swagger.io/tools/swagger-codegen).
