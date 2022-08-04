## Overview

This is a guide for user authentication at any external Social Providers(Google, Apple, Facebook, etc) using Passport-JS(a.k.a "inbound identity").

## Components

- A Janssen auth Server (installation instructions [here](https://github.com/JanssenProject/jans/tree/main/jans-linux-setup#readme))
- The [Passport Social authentication script](../../script-catalog/person_authentication/passport/passport-social-jans-script.py)
- The [Passport JS Project](https://github.com/GluuFederation/tutorials/tree/master/oidc-sso-tutorials/code/node/jans-passport)

## Authentication Flow

After users authenticate at the external social identity provider, the control flows back to Janssen Authentication server where the user is verified and the user-id is provisioned into the Janssen server.

[Diagram source file](../../admin/assets/sequence-source-passport-js-auth-flow.txt)

![jans-passport-inbound-identity](../../admin/assets/image-pasport-js-auth-flow.png)

## Role of Passport-JS and Janssen's Authentication server

1. Passport-js library encrypts and signs user data before passing it to the Janssen server.
2. Janssen Auth server verifies the received JWT, decrypts data, add/update user into LDAP, and marks the user as authenticated. 
Check [Passport-JS](https://github.com/GluuFederation/tutorials/blob/master/oidc-sso-tutorials/code/node/jans-passport/server/routes.js#L175) and [Script](../../script-catalog/person_authentication/passport/passport-social-jans-script.py#L366) Code For details.

[Diagram source file](../../admin/assets/sequence-source-passport-js-data-flow.txt)

![jans-passport-data](../../admin/assets/image-pasport-js-data-flow.png)

## Setup and configurations

### Setup Passport JS Project

This project generates auth request for your external social providers, get the user information, and send it to the Janssen server.

Take [Passport JS Project from here](https://github.com/GluuFederation/tutorials/tree/master/oidc-sso-tutorials/code/node/jans-passport). Use `Node >= 16` to install dependencies and run the project.

**Deploy it on the same server where you have your Janssen server.** we combine it with the Janssen server using the apache proxy pass setting [here](#apache-proxy-setup).

```sh
# install deps
npm install
```

```sh
# run project in the background
npm i -g pm2
pm2 start 'npm start'
```

```sh
# list app to find the id
pm2 list

# stop project
pm2 stop <id>

# restart project
pm2 restart <id>
```

### Passport-JS Project configurations

Use [config/production.js](https://github.com/GluuFederation/tutorials/blob/master/oidc-sso-tutorials/code/node/jans-passport/config/production.js) for configurations.

| Property | Details |
|-----|---------|
| providersFile | JSON File path which contains all external social provider data. which will be used by passport and jans-script. [Check below section](#external-social-provider-configurations) what exactly you need to add in this JSON file. |
| opServerURI | Your janssen server FQDN |
| keyPath | RSA Private key file path. Check [instructions here](#generate-keystore) to create JKS Keystore. It is used to generate/sign jwt which has authenticated user data. passport sends this JWT to jans-server on `/postlogin.html` endpoint after successful user auth. |
| keyId | RSA Private key's keyId(KID). |
| keyAlg | RSA algorithm which is used to generate/sign JWT. Recommended using `RS512`. |
| saltFile | Just a text file with random text. After Janssen server installation you will get the salt file at `/etc/jans/conf/salt`. Use this same file, during verification Janssen, uses the same salt file. It is used to encrypt user data that is inside jwt. Check [server/routes.js:L175](https://github.com/GluuFederation/tutorials/blob/master/oidc-sso-tutorials/code/node/jans-passport/server/routes.js#L175) for details and implementation. |
| postProfileEndpoint | `Format: https://<your.jans.server.com>/jans-auth/postlogin.htm`, After getting user info passport sends user jwt to this endpoint for further auth flow. |
| failureRedirectUrl | `Format: https://<your.jans.server.com>/jans-auth/auth/passport/passportlogin.htm`, Once anything fails at the passport side then it will redirect to failureRedirectUrl with an error message. |
| loggingDirPath | Default is `./logs/` means you will find logs in your current Passport-JS project folder. |

check [config/production.js](https://github.com/GluuFederation/tutorials/blob/master/oidc-sso-tutorials/code/node/jans-passport/config/production.js) for other application configurations.

### External Social Provider configurations

It is an array of JSON objects. Each object will be your provider. We are using [Passport-JS](https://www.passportjs.org/). Below is the sample for google, apple, and Facebook as external social providers.

```js
// passport.json

[
  {
    "id": "google",
    "displayName": "google",
    "type": "oauth",
    "mapping": "google",
    "passportStrategyId": "passport-google-oauth2",
    "enabled": true,
    "callbackUrl": "https://your.jans.server.com/passport/auth/google/callback",
    "requestForEmail": false,
    "emailLinkingSafe": false,
    "options": {
      "clientID": "xxxxxxxxxxxxxxxxxxxxxxxx",
      "clientSecret": "xxxxxxxxxxxxxxxx"
    }
  },
  {
    "id": "facebook",
    "displayName": "facebook",
    "type": "oauth",
    "mapping": "facebook",
    "passportStrategyId": "passport-facebook",
    "enabled": true,
    "callbackUrl": "https://your.jans.server.com/passport/auth/facebook/callback",
    "requestForEmail": false,
    "emailLinkingSafe": false,
    "options": {
      "clientID": "xxxxxxxxxxxxxxxxxxxxxxxx",
      "clientSecret": "xxxxxxxxxxxxxxxx"
    }
  },
  {
  "id" : "apple",
  "displayName" : "apple",
  "type" : "oauth",
  "mapping" : "apple",
  "passportStrategyId" : "@nicokaiser/passport-apple",
  "enabled" : true,
  "callbackUrl" : "https://your.jans.server.com/passport/auth/apple/callback",
  "requestForEmail" : false,
  "emailLinkingSafe" : false,
  "options" : {
      "clientID" : "xxxxxxxx",
      "scope" : "[\"name\", \"email\"]",
      "teamID" : "xxxxxxxx",
      "keyID" : "xxxxxxxxxx",
      "key" : "/etc/passport/xxxxxxxxxxxx.p8"
    }
  } 
]
```

| Property | Description |
|----------|-------------|
| id | Unique string for your provider |
| displayName | This name will be shown on auth page |
| type | Use `oauth` for all social logins |
| mapping | this is the mapping file name. you can find the social mapping file name [here](https://github.com/GluuFederation/tutorials/tree/master/oidc-sso-tutorials/code/node/jans-passport/server/mappings) |
| passportStrategyId | this is exactly your passport strategy name. List is [here](#strategies-and-configurations) |
| enabled | If true, show provider otherwise not on auth login page |
| callbackUrl | `https://<your_jans_server_fqdn>/passport/auth/<your_provider_id>/callback` replace with your id and jans-fqdn. Same URL you need to configure on your external provider side in a client. |
| requestForEmail | It is not required to be `true`. If you set it to true then it will prompt a user to enter an email. |
| emailLinkingSafe | It is not required to be `true`. If you want to link to existing users then set it to true |
| options | For social provider you just need to set two-property inside `options` i.e. `clientID` and `clientSecret`. In `apple` case few more properties. you can check [strategies documents](https://www.passportjs.org/packages/) for extra options. |

### Passport-JS Strategies and configurations

Below strategies are already available in the Passport-JS project.

| Provider | Strategy Id | Mapping |
|----------|-------------|---------|
| Apple | @nicokaiser/passport-apple | apple |
| Google | passport-google-oauth2 | google |
| Facebook | passport-facebook | facebook |
| GitHub | passport-github | github |
| Twitter | passport-twitter | tritter |
| LinkedIn | @sokratis/passport-linkedin-oauth2 | linkedin |
| Tumblr | passport-tumblr | tumblr |
| Dropbox | passport-dropbox-oauth2 | dropbox |
| Windows Live | passport-windowslive | windowslive |

### Apache proxy setup

For seamless flow, we used an apache proxy pass to configure the passport with jans-server. Add the below configuration to the Janssen apache server and restart the apache server.

```
<Location /passport>
    ProxyPass http://localhost:8090/passport retry=5 connectiontimeout=60 timeout=60
    Order deny,allow
    Allow from all
</Location>
```

Now you will understand why we have `jans-fqdn` in passport provider callback URL config.


### Add passport-social script

Download the script from [here](../../script-catalog/person_authentication/passport/passport-social-jans-script.py) and add it to Janssen Server using `jans-cli`. After Janssen Server installation, you will get `jans-cli`. 

Follow [this jans-cli guide](https://jans.io/docs/config-guide/jans-cli/im/im-custom-scripts/) to add a custom script in Janssen.

The custom script has the following properties:

| Property | Description |
|----------|-------------|
| key_store_file | Keystore file path. Use [these instructions](#generate-keystore) to create a keystore. |
| key_store_password | Keystore file secret password |
| providers_json_file | Provider JSON file which you are also using for passport config. |

> Note: After adding and enabling successfully, you can check your Janssen's Auth Server OpenID Connect configuration by navigating to the following URL: https://your-jans-server.com/.well-known/openid-configuration. Find `"acr_values_supported"` and you should see `passport-social`. 

> Note: Once you initiate auth request from your RP Application make sure to add `acr_values=passport-social` in the request. acr_values is your script name.

### Customize login pages 

`Passport-social` script uses `passportlogin.xhtml` and `passportpostlogin.xhtml` which are already comes which Janssen installation. No need to do anything. For UI Customization, you can download current page source code from [here](https://github.com/JanssenProject/jans/tree/main/jans-auth-server/server/src/main/webapp/auth/passport), modify UI, replace pages at path `/opt/jans/jetty/jans-auth/custom/pages/auth/passport/` and restart `jans-auth` server.

### Generate Keystore

Passport sends private key sign user data jwt to janssen server, for that we need to generate Keystore. keystore is a safe and passport-protected private key container. Use the below commands:

```
# generate Keystore

keytool -genkey -keyalg RSA -keysize 2048 -v -keystore <keystore-file-name>.jks -alias <kid-unique-string>

Example:
keytool -genkey -keyalg RSA -keysize 2048 -v -keystore keystore.jks -alias kid-2s3d5-f5-6f5-f4dd4
```

This command will prompt you to enter a password. Whichever password you have entered, this same password you need to configure `key_store_password` at Janssen custom script configuration.

`<kid-unique-string>` is your kid which you need for passport  `keyId` config. `keystore.jks` need it to configure `key_store_file` property at Janssen custom script configuration.

```
# JKS to PKCS#12

keytool -importkeystore -srckeystore keystore.jks \
   -destkeystore keystore.p12 \
   -srcstoretype jks \
   -deststoretype pkcs12
```

```
# PKCS#12 to PEM

openssl pkcs12 -nodes -in keystore.p12 -out keystore.pem
```

You can see many things in `keystore.pem`. but we need only the private key part to make a new file and add the private key part there like in the below example:

```
// private.pem

-----BEGIN PRIVATE KEY-----
MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCSETIkMkVKOwwO
XkYVPaBdz+lhsXpMjMJR4dTdFzn01hvcEDUf57wLmgvBXnfWDXEyEFrsvQGePZw7
foYAdnCtSqZW+dLsh6SUxL5iK0uakiY4SBX401fpbhdCeSC1pK8K+qE3jgc/o60d
oRAHLz/RCwaa8BszTwyxlLMh7xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
xxxxxxxxxxxxxxxxxxxxxxxxxPQEK0b5mPeV84fZJlfbRv01PIrwGRZJtcWC9Ke5
xSBoh0uvpZ37z2CJC7HZSz+bYz0ZhYiX372gl7BUxbLYdCz2Z9l0DDhwCO68wCzC
nualRv0U2Y5EYkekj180KnAR
-----END PRIVATE KEY-----
```

we need this file for the passport `keyPath` config.

## Testing at RP application

RP(Relying party) is an application that will be used by your users when you want to add authentication and protect resources. Once you initiate auth request from your RP Application make sure to add `acr_values=passport-social` in the request. acr_values is your script name as configured above.
