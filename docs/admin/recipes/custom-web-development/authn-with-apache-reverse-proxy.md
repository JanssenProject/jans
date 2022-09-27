---
tags:
  - administration
  - recipes
  - custom-web-development
---

## Contents:

- [Overview](#overview)
- [Component Setup](#component-setup)
- [Configure Janssen server](#configure-janssen-server)
- [Configure Protected Resource](#configure-protected-resource)
- [Configure openidc module](#configure-openidc-module)
- [Test Complete Flow](#test-complete-flow)

## Overview

This guide describes steps to enable authentication for web applications using the Janssen server which is an OpenID Connect Provider (OP). 

In the process of setting up a working environment, we will see how to use the command-line tool `jans-cli` to manually register an OpenID Connect client with the Janssen server. We will also see how to configure Relying Party to communicate with the Janssen server.

#### Hardware configuration

For development and POC purposes, 4GB RAM and 10 GB HDD should be available for Janssen Server. For PROD deployments, please refer [installation guide](https://github.com/JanssenProject/jans/wiki#janssen-installation).
  

#### Prerequisites
- Installed Apache reverse proxy that is SSL enabled
- Installed Janssen server. Refer to [Janssen Installation Guide](https://github.com/JanssenProject/jans/wiki#janssen-installation) for instructions.

## Component Setup

![Component Diagram](../../../assets/image-howto-mod-auth-comp-04222022.png)

In this setup, we have four important components.
- **Protected resource** is a resource that we need to protect using authentication. A web application for example. 
- **User workstation** is from where the user will use a browser(i.e user agent) to access the protected resource
- **Apache reverse proxy with mod_auth_openidc** is our reverse proxy server and relying party.
  - _Reverse Proxy_: In our setup protected resources will be accessible through the proxy's FQDN `https://test.apache.rp.io`. For simplicity, we will use a CGI script as a protected resource and host it on the Apache proxy itself. In a typical production setup, protected resources are usually hosted on a separate server. 
  - _Relying Party_: We will use Apache module [mod_auth_openidc](https://github.com/zmartzone/mod_auth_openidc) to provide relying party(RP) functionality. RP implements authentication flows from OpenID Connect specification. For each incoming request, RP ensures that the request is authenticated. If the request is not pre-authenticated, then RP will coordinate with Janssen server to integrate authentication.
- **Janssen server** is our open-id connect provider (OP). We will assume that the Janssen server is accessible at FQDN `https://janssen.op.io/`
 

## Configure Janssen server

In this section, we will register a new OpenID Connect client on Janssen server. In our setup, the relying party (Apache with mod_auth-openidc) is the OIDC client. There are two ways you can register OIDC client with Janssen server.
1. Manual Client Registration
2. Dynamic Client Registration (DCR)

Here we will use manual client registration.

To register a new OpenID connect client on the Janssen server, we will use `jans-cli` tool provided by the Janssen server. `jans-cli` has a menu-driven interface that makes it easy to configure the Janssen server. Here we will use the menu-driven approach to register a new client. To further understand how to use menu-driven approach and get complete list of supported command-line operations, refer to [jans-cli documentation](../../../admin/config-guide/jans-cli/cli-index.md).

  - Run the command below on Janssen server to enter interactive mode.


     > Note: </br> `jans-cli` has to be authenticated and authorized with the respective Janssen server. If `jans-cli` is being executed for the first time or if there is no valid access token available, then running the command below will initiate device authentication and authorization flow. In that case, follow the steps for [jans-cli authorization](../../config-guide/jans-cli/cli-index.md#cli-authorization) to continue running the command.
  
    ```
    /opt/jans/jans-cli/config-cli.py
    ```

    Running above command will bring up main menu as shown in sample below:
    
    ![CLI-main-menu](../../assets/image-howto-mod-auth-cli-main-menu-04292022.png)
    
    To register a new OpenID Connect client, select `OAuth OpenID Connect - Clients` option (`16` in above sample). Selecting an appropriate option will bring up related sub-menu. 
   
  - From sub-menu, select option for `Create new OpenId connect client`. Upon selecting this option, CLI will prompt for inputs required to register a new OpenID connect client.
  - Provide inputs for the following properties:
  
    ```
    displayName: <name-of-choice>
    application Type: web
    includeClaimsInIdToken  [false]: 
    Populate optional fields? y
    clientSecret: <secret-of-your-choice>
    subjectType: public
    tokenEndpointAuthMethod: client_secret_basic
    redirectUris: https://test.apache.rp.io/callback
    scopes: email_,openid_,profile
    responseTypes: code
    grantTypes: authorization_code
    ```
    
   - Once values for all the above properties are provided, input `c` at the prompt to instruct `jans-cli` to create schema using inputs provided till now. At this time, `jans-cli` will show the schema(JSON) that will be used to create a new OpenID Connect client on Janssen server. Verify that schema has captured all the provided inputs correctly.
   - Now next step is for `jans-cli` to post this JSON schema to Janssen server to actually register new client. To do this, input `y` on the prompt.
   - If the client is successfully registered then we will receive JSON data back. This data describes newly registered client. Some of these values from this JSON response, like `inum` and `clientSecret`, will be required when we configure `mod_auth_openidc` as a client. So keep this reponse JSON handy. See a sample of JSON response below:
   

      ```
      {
        "dn": "inum=165bdf95-f15e-44f0-bdd7-cdac71fda8e0,ou=clients,o=jans",
        "inum": "165bdf95-f15e-44f0-bdd7-cdac71fda8e0",
        "displayName": "dm",
        "clientSecret": "a9894ba8-eb01-4a26-a69d-026f10a49272",
        "frontChannelLogoutUri": null,
        "frontChannelLogoutSessionRequired": false,
        "registrationAccessToken": null,
        "clientIdIssuedAt": null,
        "clientSecretExpiresAt": null,
        "redirectUris": null,
        "claimRedirectUris": null,
        "responseTypes": null,
        "grantTypes": [],
        "applicationType": "web",
        "contacts": null,
        "clientName": "dm",
        "idTokenTokenBindingCnf": null,
        "logoUri": null,
        "clientUri": null,
        "policyUri": null,
        "tosUri": null,
        "jwksUri": null,
        "jwks": null,
        "sectorIdentifierUri": null,
        "subjectType": "public",
        "idTokenSignedResponseAlg": null,
        "idTokenEncryptedResponseAlg": null,
        "idTokenEncryptedResponseEnc": null,
        "userInfoSignedResponseAlg": null,
        "userInfoEncryptedResponseAlg": null,
        "userInfoEncryptedResponseEnc": null,
        "requestObjectSigningAlg": null,
        "requestObjectEncryptionAlg": null,
        "requestObjectEncryptionEnc": null,
        "tokenEndpointAuthMethod": null,
        "tokenEndpointAuthSigningAlg": null,
        "defaultMaxAge": null,
        "requireAuthTime": false,
        "defaultAcrValues": null,
        "initiateLoginUri": null,
        "postLogoutRedirectUris": null,
        "requestUris": null,
        "scopes": null,
        "claims": null,
        "trustedClient": false,
        "lastAccessTime": null,
        "lastLogonTime": null,
        "persistClientAuthorizations": false,
        "includeClaimsInIdToken": false,
        "refreshTokenLifetime": null,
        "accessTokenLifetime": null,
        "customAttributes": [],
        "customObjectClasses": null,
        "rptAsJwt": false,
        "accessTokenAsJwt": false,
        "accessTokenSigningAlg": null,
        "disabled": false,
        "authorizedOrigins": null,
        "softwareId": null,
        "softwareVersion": null,
        "softwareStatement": null,
        "attributes": {
          "tlsClientAuthSubjectDn": null,
          "runIntrospectionScriptBeforeJwtCreation": false,
          "keepClientAuthorizationAfterExpiration": false,
          "allowSpontaneousScopes": false,
          "spontaneousScopes": null,
          "spontaneousScopeScriptDns": null,
          "backchannelLogoutUri": null,
          "backchannelLogoutSessionRequired": false,
          "additionalAudience": null,
          "postAuthnScripts": null,
          "consentGatheringScripts": null,
          "introspectionScripts": null,
          "rptClaimsScripts": null
        },
        "backchannelTokenDeliveryMode": null,
        "backchannelClientNotificationEndpoint": null,
        "backchannelAuthenticationRequestSigningAlg": null,
        "backchannelUserCodeParameter": null,
        "expirationDate": null,
        "deletable": false,
        "jansId": null
      }
      ```

## Configure protected resource

As mentioned under [component setup](#component-setup), our protected resource will be hosted on Apache reverse proxy itself. It is a simple Python based cgi script that will print request header information and can be accessed through `https://test.apache.rp.io/cgi-bin/printHeaders.py`. Execute the steps below on Apache host to set up the protected resource:

1. Create script file `printHeaders.py`
    ```
    vi /usr/lib/cgi-bin/printHeaders.py
    ```
    with content as below
    ```
    #!/usr/bin/python3

    import os

    d = os.environ
    k = d.keys()

    print "Content-type: text/html\n\n"

    print "<HTML><HEAD><TITLE>printHeaders.cgi</TITLE></Head><BODY>"
    print "<h1>Environment Variables</H1>"
    for item in k:
      print "<p><B>%s</B>: %s </p>" % (item, d[item])
    print "</BODY></HTML>"
    ```
 3. Add Apache `cgi` module to enable execution of CGI
    ```
    a2enmod cgi
    ```
 4. Change permissions for CGI script so that it can be executed by Apache
    ```
    chown www-data:www-data /usr/lib/cgi-bin/printHeaders.py
    chmod ug+x /usr/lib/cgi-bin/printHeaders.py
    ```
    
 At this point, `printHeaders.py` should be accessible at `https://test.apache.rp.io/cgi-bin/printHeaders.py` without requiring any authentication.
 
## Configure openidc module

#### Install *mod-auth-openidc* 

On Apache reverse proxy host, add _mod-auth-openidc_ using commands below
```
apt-get install libapache2-mod-auth-openidc
a2enmod auth_openidc
service apache2 restart
```
#### Configure *mod-auth-openidc* 
- Open `/etc/apache2/sites-available/default-ssl.conf`
- Add *mod-auth-openidc* configuration parameters given below for virtual host `_default_:443`. Find more configuration options for _mod-auth-openidc_ [here](https://github.com/zmartzone/mod_auth_openidc/blob/master/auth_openidc.conf). 
- This configuration will enable authentication for any resource under `/` context root.

```
OIDCProviderMetadataURL https://janssen.op.io/jans-auth/.well-known/openid-configuration
OIDCClientID <inum-as-received-in-client-registration-response>
OIDCClientSecret <as-provided-in-client-registration-request>
OIDCResponseType code
OIDCScope "openid email profile"
OIDCProviderTokenEndpointAuth client_secret_basic
OIDCSSLValidateServer Off
OIDCProviderIssuer https://janssen.op.io
OIDCRedirectURI https://test.apache.rp.io/callback
OIDCCryptoPassphrase <crypto-passphrase-of-choice>
<Location "/">
    Require valid-user
    AuthType openid-connect
</Location>
```

Restart Apache service.
```
service apache2 restart
```


## Test Complete Flow

- Accessing `https://test.apache.rp.io/cgi-bin/printHeaders.py` should redirect to the Janssen authentication screen.
- Upon valid authentication, Janssen will present the user with a consent screen where the user will be able to allow user attributes that can be provided to the app
- If allowed, the user will be successfully taken to the `printHeaders.py` page. In the process, `mod_auth_openidc` also requests user claims (i.e attributes) from the `/userinfo` endpoint of the Janssen server. And makes these claims available as environment variables to the application.
- `printHeaders.py` prints all the environment variables, along with user claims received from the Janssen server. The application can use this information to identify the user and also enforce access policies. Sample output below shows some of this user information and token information printed as part of environment variables:

```
Environment Variables

OIDC_CLAIM_sub: e205be81-6a85-4d83-9126

OIDC_CLAIM_email_verified: 1

OIDC_CLAIM_name: Default myname User

OIDC_CLAIM_nickname: myname

OIDC_CLAIM_given_name: myname

OIDC_CLAIM_middle_name: myname

OIDC_CLAIM_family_name: User

OIDC_CLAIM_email: myname@mydomain.io

OIDC_access_token: d6a12cce-f196-4da3-ba17

OIDC_access_token_expires: 165493257

HTTPS: on

SSL_TLS_SNI: test.apache.rp.io
```
