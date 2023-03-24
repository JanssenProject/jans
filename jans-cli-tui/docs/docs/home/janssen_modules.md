
## Janssen Modules

Janssen is not a big monolith--it's a lot of services working together. Whether you deploy Janssen to a Kubernetes cluster, or you are a developer running everything on one server, it's important to understand the different parts. 

1. **[jans-auth-server](/gluu-4/docs/plugins/oauth/oauth/)**: This component is the OAuth Authorization Server, the OpenID Connect Provider, the UMA Authorization Server--this is the main Internet facing component of Janssen. It's the service that returns tokens, JWT's and identity assertions. This service must be Internet facing.

1. **[jans-fido2](/gluu-4/docs/plugins/fido/fido/)**:  This component provides the server side endpoints to enroll and validate devices that use FIDO. It provides both FIDO U2F (register, authenticate) and FIDO 2 (attestation, assertion) endpoints. This service must be internet facing.

1. **[jans-config-api](/gluu-4/docs/plugins/config_api/config_api/)**: The API to configure the auth-server and other components is consolidated in this component. This service should not be Internet-facing.

1. **[jans-scim](/gluu-4/docs/plugins/scim/scim/)**: [SCIM](http://www.simplecloud.info/) is JSON/REST API to manage user data. Use it to add, edit and update user information. This service should not be Internet facing.

1. **[jans-cli](jans-cli)**: This module is a command line interface for configuring the Janssen software, providing both interactive and simple single line
   options for configuration.

1. **[jans-client-api](/gluu-4/docs/plugins/client_api/client_api/)**: Middleware API to help application developers call an OAuth, OpenID or UMA server. You may wonder why this is necessary. It makes it easier for client developers to use OpenID signing and encryption features, without becoming crypto experts. This API provides some high level endpoints to do some of the heavy lifting.

1. **[jans-core](jans-core)**: This library has code that is shared across several janssen projects. You will most likely need this project when you build other Janssen components.

1. **[jans-orm](jans-orm)**: This is the library for persistence and caching implemenations in Janssen. Currently LDAP and Couchbase are supported. RDBMS is coming soon.

1. **[Agama](agama)**: Agama module offers an alternative way to build authentication flows in Janssen Server. With Agama, flows are coded in a DSL (domain specific language) designed for the sole purpose of writing web flows.
