## Contents:

- [Overview](#overview)
- [Setup Janssen server](#setup-janssen-server)
- [Setup mod-auth-openidc](#setup-mod-auth-openidc)
- [Test Complete Flow](#test-complete-flow)

## Overview

This guide describes steps to enable authentication for web applications using Janssen server which is an OpenID Connect Provider (OP). 

Majority of the web applications use a reverse proxy, like Apache, to avail functionalities like load-balancing etc. We will configure  [mod_auth_openidc](https://github.com/zmartzone/mod_auth_openidc) Apache server module to add Relying Party(RP) functionality to existing Apache reverse proxy. RP implements authentication flows from OpenID Connect specification. For each incoming request, RP ensures that the request is authenticated. If request is not pre-authenticated, then RP will coordinate with Janssen server to integrate authentication.

#### Hardware configuration

For development and POC purposes, 4GB RAM and 10 GB HDD should be available for Janssen Server. For PROD deployments, please refer [installation guide](https://github.com/JanssenProject/jans/wiki#janssen-installation).
  

#### Prerequisites
- Existing Apache reverse proxy that is SSL enabled. Application resources which need to be protected using authentication are accessed via this reverse proxy. Also, we will assume that Apache proxy server is accessible at FQDN `https://test.apache.rp.io/`
- For simplicity, we will use one of the web page hosted directly on Apache server as application resource and configure authentication for the same.

## Setup Janssen server 

Commands below can be used to install Janssen Authorization Server on an Ubuntu 20.04 system. More installation options are available [here](https://github.com/JanssenProject/jans/wiki#janssen-installation). For this guide, we would assume that Janssen server host is accessible via FQDN `https://janssen.op.io/`.

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

Janssen server provides `jans-cli` CLI tool to configure Janssen server. `jans-cli` has menu-driven interface that makes it easy to configure Janssen server.

Use steps below to configure Janssen server.

- Manually register client(RP) as OpenID Connect client
  - Run command below on host running Janssen Server
    ```
    /opt/jans/jans-cli/config-cli.py`
    ```
  - Navigate through options to start registering new OpenID Connect client
  - Provide inputs for following properties. Number in parathesis indicates menu option(may change) and are not part of input:
    ```
    displayName: <name-of-choice>
    applicationType: web
    includeClaimsInIdToken  [false]: _true
    Populate optional fields? y
    clientSecret: <secret-of-your-choice>
    subjectType: public
    tokenEndpointAuthMethod: client_secret_basic
    redirectUris: https://test.apache.rp.io/redirect
    scopes: email openid profile
    responseTypes: code
    grantTypes: authorization_code
    ```
   - Copy the resulting JSON data and save it to a file, say `register-apache-rp.json`.
   - Register client usign following command
   ```
   /opt/jans/jans-cli/config-cli.py --operation-id post-oauth-openid-clients --data <path>/register-apache-rp.json
   ```
   - Output of this command would be a JSON response. Save this response to a file as some of the values in it will be required when configuring *mod-auth-openidc*.
 
- create test users and scopes 
> TODO: add steps


## Setup *mod-auth-openidc* 

#### Install *mod-auth-openidc* 

On Apache reverse proxy host, add mod-auth-openidc using commands below
```
sudo apt-get install libapache2-mod-auth-openidc
sudo a2enmod auth_openidc
service apache2 restart
```
#### Configure *mod-auth-openidc* 
- Open `/etc/apache2/sites-available/default-ssl.conf`
- Add *mod-auth-openidc* configuration parameters given below for virtual host `_default_:443`. Find more configuration options for mod-auth-openidc [here](https://github.com/zmartzone/mod_auth_openidc/blob/master/auth_openidc.conf). 
- This configuration will enable authentication for any resource under `/` context root.

```
OIDCProviderMetadataURL https://janssen.op.io/jans-auth/.well-known/openid-configuration
OIDCClientID <inum-as-received-in-client-registration-response>
OIDCClientSecret <as-provided-in-client-registration-request>
OIDCResponseType code
OIDCProviderTokenEndpointAuth client_secret_basic
OIDCSSLValidateServer Off
OIDCProviderIssuer https://janssen.op.io
OIDCRedirectURI https://test.apache.rp.io/redirect
OIDCCryptoPassphrase <crypto-passphrase-of-choice>
<Location "/">
    Require valid-user
    AuthType openid-connect
</Location>
```

Restart Apache service


## Test Complete Flow

- Accessing `https://test.apache.rp.io/` should redirect to Janssen authentication screen. Upon successful authentication, browser should be redirected to `https://test.apache.rp.io/redirect`.
