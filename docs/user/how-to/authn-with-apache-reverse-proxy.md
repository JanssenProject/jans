

## Contents:

- [Overview](#overview)
- [Setup Janssen server](#setup-janssen-server)
- [Setup mod-auth-openidc](#setup-mod-auth-openidc)
- [Test Complete Flow](#test-complete-flow)

## Overview

This guide will walk through the steps used to enable authentication for web applications using Janssen server.

Janssen server acts as OpenID Connect authentication provider. It is also referred to as OpenID Connection Provider (OP). 
Application which needs to be protected with authentication is called Resource Server(RS). 

Majority of the web applications use a reverse proxy, like Apache, to avail functionalities like load-balancing etc. We will configure Apache reverse proxy, and use [mod_auth_openidc](https://github.com/zmartzone/mod_auth_openidc) Apache server module to add Relying Party(RP) functionality. RP is implementation authentication flows of OpenID Connect specification. For each incoming request, RP ensures that the request is authenticated. If request is not pre-authenticated, then RP will coordinate with Janssen server to integrate authentication.

#### Hardware configuration

For development and POC purposes, 4GB RAM and 10 GB HDD should be available for Janssen Server. For PROD deployments, please refer installation guide documents.
  

#### Prerequisites
- For this guide we will assume that setup with Apache reverse proxy already exists with SSL enabled. Application resources which need to be protected using authentication are accessed via reverse proxy.
-  For the simplicity of the instruction set, we will use one of the webpage hosted by Apache server itself as application resource and configure authentication for the same.

## Setup Janssen server 

Janssen Server provides OpenID Connect Provider.

Commands below can be used to install Janssen Authorization Server on an Ubuntu 20.04 system. More installation options are available [here](https://github.com/JanssenProject/jans/wiki#janssen-installation). For this guide, we would assume that Janssen installation host is accessible FQDN `https://janssen.op.io/`.

```
wget https://repo.gluu.org/jans/jans_1.0.0~ubuntu20.04_amd64.deb
sudo dpkg -i jans_1.0.0~ubuntu20.04_amd64.deb
sudo apt-get -f install
sudo python3 /opt/jans/jans-setup/setup.py
```
  
If Janssen server is correctly installed, metadata URL below should be accessible.
```
https://janssen.op.io/.well-known/openid-configuration
```

#### Configure Janssen server

Janssen server provides `jans-cli` tool which enables Janssen Configuration via Command Line Interface(CLI). It has menu-driven interface that makes it easier to configure Janssen server.
Use steps below to configure Janssen server.
- Add RP as OpenID Connect client
  - Run command below to enter interactive mode
    ```
    /opt/jans/jans-cli/config-cli.py`
    ```
  - Once after entering interactive mode navigate through options to start registering new OpenID Connect client
  - Provide inputs for following properties. Number in parathesis indicates menu option(may change):
    ```
    displayName: test.local.rp.io
    applicationType: web
    includeClaimsInIdToken  [false]: _true
    Populate optional fields? y
    clientSecret: my-client-secret (1)
    subjectType: public (21)
    tokenEndpointAuthMethod: client_secret_basic (31)
    redirectUris: https://test.local.rp.io/redirect (7)
    scopes: email openid profile (39)
    responseTypes: code (9)
    grantTypes: authorization_code (10)
    ```
   - Copy the resulting JSON data and save it to a file, `register-apache-rp.json`.
   - Now register client usign following command
   ```
   /opt/jans/jans-cli/config-cli.py --operation-id post-oauth-openid-clients --data /root/register-apache-rp.json
   ```
   - Output of this command would be a JSON response. Save this response for reference as some of these value will be required when configuring *mod-auth-openidc*.
 
- create test users and scopes 
> TODO: add steps


## Setup *mod-auth-openidc* 

#### Install *mod-auth-openidc* 

Add mod-auth-openidc using steps below
```
sudo apt-get install libapache2-mod-auth-openidc
sudo a2enmod auth_openidc
service apache2 restart
```
#### Configure *mod-auth-openidc* 
- Open `/etc/apache2/sites-available/default-ssl.conf`. Find more configuration options for mod-auth-openidc [here](https://github.com/zmartzone/mod_auth_openidc/blob/master/auth_openidc.conf). 
- Add configuration parameters given below for virtual host `_default_:443`.
- This will enable authentication for any resource under `/` context root.

```
OIDCProviderMetadataURL https://jans-install-mysql.lxc.jans.io/jans-auth/.well-known/openid-configuration
OIDCClientID 73257b63-f3c2-484c-b228-ffeb33290cec
OIDCClientSecret my-client-secret
OIDCResponseType code
OIDCProviderTokenEndpointAuth client_secret_basic
OIDCSSLValidateServer Off
OIDCProviderIssuer https://jans-install-mysql.lxc.jans.io
OIDCRedirectURI https://test.local.rp.io/redirect
OIDCCryptoPassphrase my-crypto-passphrase
<Location "/">
    Require valid-user
    AuthType openid-connect
</Location>

```

Restart Apache service


## Test Complete Flow

- Accessing `https://test.local.rp.io/` should redirect to Janssen authentication screen. Upon successful authentication, browser should be redirected to `https://test.local.rp.io/redirect`. 
