# SCIM
SCIM is a specification designed to reduce the complexity of user management 
operations by providing a common user schema and the patterns for exchanging such 
schema using HTTP in a platform-neutral fashion. The aim of SCIM is achieving 
interoperability, security, and scalability in the context of identity management.

Developers can think of **SCIM** merely as a **REST API** with endpoints exposing 
**CRUD** functionality (create, read, update and delete).

For your reference, the current version of the standard is governed by the following 
documents: [RFC 7642](https://tools.ietf.org/html/rfc7642), [RFC 7643](https://tools.ietf.org/html/rfc7643), and [RFC 7644](https://tools.ietf.org/html/rfc7644).

Janssen allows you to protect your endpoints with UMA (a profile of [OAuth 2.0](http://tools.ietf.org/html/rfc6749)). 
This is a safe and standardized approach for controlling access to web resources. 
