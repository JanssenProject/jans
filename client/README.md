SCIM-Client
===========

SCIM is a specification designed to reduce the complexity of user management operations by providing a common user schema
and the patterns for exchanging this schema using HTTP in a platform-neutral fashion. The aim of SCIM is achieving
interoperability, security, and scalability in the context of identity management.

Developers can think of SCIM as a REST API with endpoints exposing CRUD functionality (create, update, retrieve and delete).

This project consists of a ready-to-use Java client to interact with those endpoints.

Detailed specifications for SCIM can be found at [RFC 7642](https://tools.ietf.org/html/rfc7642),
[RFC 7643](https://tools.ietf.org/html/rfc7643), and [RFC 7644](https://tools.ietf.org/html/rfc7644). Documentation of
Gluu's implementation of SCIM service is available at [User Management with SCIM](https://www.gluu.org/docs/ce/user-management/scim2/).

Below is the link for the latest Gluu implementation of SCIM client:

* [SCIM stable client library binary](https://ox.gluu.org/maven/gluu/scim/client/scim-client2/5.0.0-SNAPSHOT/)

### How to run tests

* Ensure you have a [working installation](https://gluu.org/docs/ce/installation-guide/) of Gluu Server

* Enable and then protect your SCIM API using test mode or UMA (see [API protection](https://www.gluu.org/docs/ce/user-management/scim2/#api-protection))

* Edit `profiles/default/config-scim-test.properties` or create a profile directory with your own copy of `config-scim-test.properties`.

  Supply suitable values for properties file. Use [this](https://www.gluu.org/docs/ce/user-management/scim2/#testing-with-the-scim-client-uma)
   as a guide if you are under UMA protection.

* Load test data. If using Gluu 4.2 or higher, use `./setup.py -x -properties-password=<password_provided_at_installation>`

* ... and run maven. Examples:

   - `mvn test`
   - `mvn -Dcfg=<profile-name> test`
   - `mvn -Dtestmode=true test`
