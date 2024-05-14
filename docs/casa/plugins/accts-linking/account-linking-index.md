---
tags:
- Casa
- Accounts Linking
---

# Accounts Linking Plugin

## Overview

This plugin allows users to "link" their local Jans account with existing accounts at third-party identity providers like OIDC OPs and social sites, e.g. Apple, Facebook, Google, etc.

Besides the usual onboarding of a plugin jar file in Casa, administrators must deploy a number of additional components. This will be regarded later. However let's summary the key points of the accounts linking experience:

- When a user tries to login to Casa, the usual username/password form is presented but also a list of links that can take him to external sites (third-party identity providers) where authentication takes place
- Once authenticated, user profile data is grabbed from the external site - this is all backchannel
- A process called _attribute mapping_ is performed on profile data. This is a transformation process that turns incoming profile data into a shape compatible with a regular Jans database user entry
- If the mapped profile data matches an existing user in the Jans database, the existing entry is updated with the incoming data, otherwise, a new entry is inserted - this is called _user provisioning_. When provisioning occurs, the account has no password associated  
- Finally the user is given access to Casa

From the perspective of a user already logged into Casa, the experience is as follows:

- In casa, a menu item is provided which takes the user to a (Casa) page that shows a list of third-party identity providers. For every provider there are options to trigger linking in case there is no account linked yet (external site authentication is launched), or to remove the linked account from the user profile 
- If an account has no password assigned, removal of linked accounts is not allowed. However, a functionality for the user to assign himself a password is provided

## Components deployment

!!! Note
    Ensure you are running at least version 1.1.1 of Jans Authentication Server and Casa

The pieces that allow materialization of the experience summarized above are the following:

a) The Casa plugin jar file

b) A custom XHTML page and jython script

c) The Agama inbound identity project

d) The Casa accounts linking Agama project

Most of work is demanded on setting up project _d_, where configuration of identity providers and attribute mapping tuning takes place. 

In the following, it is assumed you have a VM-based installation of Jans Server (or Gluu Flex) available with Casa installed. In a separate machine, ensure you have SSH/SCP/SFTP access to such server and `git` installed. 

1. Download the plugin jar file `https://maven.jans.io/maven/io/jans/casa/plugins/acct-linking/replace-janssen-version/acct-linking-replace-janssen-version-jar-with-dependencies.jar` and copy to your server's `/opt/jans/jetty/jans-casa/plugins`

1. Download the utility jar file `https://maven.jans.io/maven/io/jans/agama-inbound/replace-janssen-version/agama-inbound-replace-janssen-version.jar` and copy to your server's `/opt/jans/jetty/jans-auth/custom/libs`

1. In the server, create a `casa` directory inside `/opt/jans/jetty/jans-auth/custom/pages`

1. Download the file `https://github.com/JanssenProject/jans/raw/vreplace-janssen-version/jans-casa/plugins/acct-linking/extras/login.xhtml` and copy it to the previously created folder  

1. Download the file `https://github.com/JanssenProject/jans/raw/vreplace-janssen-version/jans-casa/plugins/acct-linking/extras/Casa.py`. Open TUI or the admin UI (for Flex), and locate the custom script whose name is `casa`. Update the contents of the script with the contents of the file 

1. In TUI, ensure the custom script named `agama` is enabled

1. Still in TUI, visit the Clients screen, locate the client labeled "Client for Casa". Add the following redirect URI to the list: `https://<your-jans-host>/jans-casa/pl/acct-linking/user/interlude.zul`. Replace the name of your Jans server accordingly 

1. Run the following commands to generate the archives of the Agama projects
    
    ```
    git clone --depth 1 --branch main --no-checkout https://github.com/JanssenProject/jans.git
    cd jans
    git sparse-checkout init --cone
    git sparse-checkout set docs/agama-catalog/jans/inboundID/project
    git sparse-checkout set jans-casa/plugins/acct-linking/extras/agama
    git checkout main
    cd docs/agama-catalog/jans/inboundID/project
    zip -r inbound.zip *
    cd ../../../../..
    cd jans-casa/plugins/acct-linking/extras/agama
    zip -r acctlinking.zip *
    ```

1. Transfer the zip files to a location in the server, deploy both archives using TUI (Agama menu)

1. Finally restart the authentication server

## Configuration

So far all components required for the Casa inbound identity solution are loaded in the server. When logging to casa, the form presented looks like usual, and once in, the "Accounts linking" menu takes to a page which hints about missing configuration.  

The first step is figuring out the external sites to support. Keep in mind only OpenID or OAuth 2.0 based providers can be onboarded. There is not support for SAML IDPs.

The procedures for getting configuration settings in order to integrate third party providers vary widely. Here, only basic guidelines are given:

- If the provider is OpenId-compliant and supports dynamic client registration, obtain the OP URL and the scopes list to use when requesting user profile information. Most of times the scopes `openid`, `profile` and `email` will fit the bill 

- If the provider is OpenId-compliant and does not support dynamic client registration, obtain the OP URL and scopes as in the previous case, and also a client ID and secret

- If the provider does not support OpenId. Obtain the following:

    - The authorization endpoint URL
    - The token endpoint URL
    - The endpoint URL where profile data can be retrieved
    - Client credentials (ID and secret)
    - Scopes to use when requesting user profile information

The steps required to grab the above data vary among providers. Normally this is obtained through a sort of administrative developer GUI tool. If you are prompted for a "redirect URI" in such tool, provide `https://<your-jans-host>/jans-auth/fl/callback`.

Now it's time to supply the settings grabbed. The component these configurations are injected to is the Casa accounts linking Agama project. To make the effort easier, this project is bundled with some dummy configuration properties you can use as a template. Proceed as follows:

1. In TUI, open the Agama tab and scroll through the list of projects until the `casa-account-linking` is highlighted
1. Open the configuration management dialog (press `c`) and choose to export the sample configuration to a file on disk
1. Apply changes as needed - this is covered in a separate doc page [here](./accts-linking-agama.md). Note you can add or remove sections in the file at will, and that providers can also be disabled so they are not listed in the login page or in Casa app
1. Still in TUI, choose to import the file you have edited. Then conduct your testing
