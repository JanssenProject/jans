## oxAuth

oxAuth is an open source OpenID Connect Provider (OP) and UMA Authorization Server (AS). The project also includes OpenID Connect Client code which can be used by websites to validate tokens. 

oxAuth currently implements all required aspects of the OpenID Connect stack, including an OAuth 2.0 authorization server, Simple Web Discovery, Dynamic Client Registration, JSON Web Tokens, JSON Web Keys, and User Info Endpoint.

**oxAuth is tightly coupled with [oxTrust](https://github.com/GluuFederation/oxTrust)**. 

oxAuth configuration is stored in LDAP, and oxTrust is needed to generate the proper configuration. For deployment instructions, use the [Gluu Server documentation](https://gluu.org/docs/ce)
