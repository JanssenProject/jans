---
tags:
  - administration
  - recipes
---

## Overview

This is a guide for user authentication at any external Social Providers(Google, Apple, Facebook, etc) using Passport-JS(a.k.a "inbound identity").

## Components

- The [Passport Social authentication script](../../script-catalog/person_authentication/passport/passport-social-jans-script.py)
- The [Passport JS Project](#setup-passport-js-project)

## Authentication Flow

After users authenticate at the external social identity provider, the control flows back to Janssen Authentication server where the user is verified and the user-id is provisioned into the Janssen server.

[Diagram source file](../../assets/sequence-source-passport-js-auth-flow.txt)

![jans-passport-inbound-identity](../../assets/image-pasport-js-auth-flow.png)

## Role of Passport-JS project and Janssen's Authentication server

1. Passport-js project encrypts and signs user data before passing it to the Janssen server.
2. Janssen Auth server verifies the received JWT, decrypts data, add/update user into LDAP, and marks the user as authenticated. 
Check [Script](../../script-catalog/person_authentication/passport/passport-social-jans-script.py#L415) Code For details.

[Diagram source file](../../assets/sequence-source-passport-js-data-flow.txt)

![jans-passport-data](../../assets/image-pasport-js-data-flow.png)

## Setup and configurations

### Setup Passport JS Project

[Passport JS](https://www.passportjs.org/) is very good library in Node JS which helps to add many social provider authentication in project. For this Inbound Jans Flow, You have to make a Passport Node JS project which help to generates auth request for your external social providers, get the user information, and send it to the Janssen server.

Below things need to add in Passport JS Project to setup complete auth flow with Jans.

1. Make a token endpoint which expose token with unique id. Jans will use this endpoint to get token and use this token for secur auth request. Passport Project has to validate this token in Jans request. This is just a simple strategy to protect passport endpoint from spam requests and only allow jans to request it. Code sample:

   ```js
   // Encryption compatible with Jans EncryptionService
   // Salt File path: /etc/jans/conf/salt
   router.get('/token',
     function (req, res, next) {
       const t = jwt.sign(
         { jwt: uuidv4() }, '<saltfilevalue>', { expiresIn: 120 } // 2 min expiration
       )
       res.status(200).send({ token_: t }) // key should be token_
     }
   )
   ```

   You need to use Salt File `/etc/jans/conf/salt` as a secret key. You will get this salt file after Jans intallation. Jans [interception script](../../script-catalog/person_authentication/passport/passport-social-jans-script.py#L400) uses this same file for decryption.

1. Create auth request endpoints which will be request by User on Jans auth login page. For Example: Suppose you have added google strategy in your passport project. In that case you can make a endpoint like `/google/:token`. `google` is provider id in [providers_json_file](#add-passport-social-script). `token` is the same token which we created in above step.

1. After authentication Passport Project has to send encoded userinfo data to Jans `https://<your.jans.server.com>/jans-auth/postlogin.htm`. Below is the Node JS code sample to send POST request with data to Jans Post Login Endpoint.

   ```js
   const postUrl = "https://<your.jans.server.com>/jans-auth/postlogin.htm"
   res.set('content-type', 'text/html;charset=UTF-8')
   return res.status(200).send(`
     <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
       <body onload="document.forms[0].submit()">
         <noscript>
           <p>
             <b>Note:</b> your browser does not support JavaScript, please press the Continue
             button to proceed.
           </p>
         </noscript>
 
         <form action="${postUrl}" method="post">
           <div>
             <input type="hidden" name="user" value="${jwt}"/>
             <noscript>
               <input type="submit" value="Continue"/>
             </noscript>
           </div>
         </form>
       </body>
     </html>`
   )
   ```

   Check next step to generate `jwt`.

1. To make encoded userinfo data, use jwt private key signing technique. Check below sample code:

   ```js
   // mapped user info data with ldap attributes
   const user = {
       uid: profile.username || profile.id,
       mail: profile.email,
       cn: profile.displayName,
       displayName: profile.displayName,
       givenName: profile.name.givenName,
       sn: profile.name.familyName,
       provider: "<your-social-provider-name>", // e.g. google
   }
   
   jwt.sign({
       iss: 'https://<your.jans.server.com>/jans-auth/postlogin.htm',
       sub: user.uid,
       aud: 'https://<your.jans.server.com>',
       jti: uuidv4(),
       exp: now / 1000 + 30,
       iat: now,
       data: encrypt(user)
     }, privateKey, defaultRpOptions)
   
   // Check `generate Keystore` section below to generate keys
   const privateKey = `-----BEGIN PRIVATE KEY-----
   xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
   Your Private Key
   xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
   -----END PRIVATE KEY-----`
   
   cosnt defaultRpOptions = {
     algorithm: config.get('keyAlg'),
     header: {
       typ: 'JWT',
       alg: config.get('keyAlg'),
       kid: config.get('keyId')
     }
   }
   
   function encrypt (obj) {
     // Encryption compatible with Jans EncryptionService
     // Salt File path: /etc/jans/conf/salt
     const pt = JSON.stringify(obj)
     const encrypt = crypto.createCipheriv('des-ede3-ecb', '<saltfilevalue>', '')
     let encrypted = encrypt.update(pt, 'utf8', 'base64')
     encrypted += encrypt.final('base64')
     return encrypted
   }
   
   ```

   Follow Steps in [Generate Keystore section](#generate-keystore) to generate private key.

1. You can add as many as possible passport strategies in your passport project. Above steps are little complex, use [this sample code file](https://github.com/JanssenProject/jans/blob/main/docs/assets/sample-code-passport-js.js) to build your passport application.

### Apache proxy setup

For seamless flow, You can deploy Passport Project on Jans server and you can use an apache proxy pass to configure the passport with jans-server. Add the below configuration to the Janssen apache server and restart the apache server.

```
<Location /passport>
    ProxyPass http://localhost:8090/passport retry=5 connectiontimeout=60 timeout=60
    Order deny,allow
    Allow from all
</Location>
```

After this configurations, you need to use `jans-fqdn` in passport callback URL. This is optional configurations, you can deploy your passport project on another server. 

### Add passport-social script

Download the script from [here](../../script-catalog/person_authentication/passport/passport-social-jans-script.py) and add it to Janssen Server using `jans-cli`. After Janssen Server installation, you will get `jans-cli`. 

Follow [this jans-cli guide](https://jans.io/docs/config-guide/jans-cli/im/im-custom-scripts/) to add a custom script in Janssen.

The custom script has the following properties:

| Property | Description |
|----------|-------------|
| key_store_file | Keystore file path. Use [these instructions](#generate-keystore) to create a keystore. |
| key_store_password | Keystore file secret password |
| providers_json_file | Array of JSON Objects. Provider JSON file with social provider which you want to add for authentication. |
| passport_fqdn | Passport FQDN e.g. `https://your-passport.com` |
| passport_token_endpoint_path | It will use `passport_fqdn` as a prefix so just add path. e.g. `/passport/token`. Jans request this endpoint to get token for next auth request [here](../../script-catalog/person_authentication/passport/passport-social-jans-script.py#L337). Check [Step-1](#setup-passport-js-project) in Setup Passport section to make token endpoint. |
| passport_auth_endpoint_path | It will use `passport_fqdn` as a prefix so just add path. e.g. `/passport/auth`. Jans request this endpoint to initiate auth request [here](../../script-catalog/person_authentication/passport/passport-social-jans-script.py#L357). Check [Step-2](#setup-passport-js-project) in Setup Passport section to make passport auth endpoint. Jans will append provider and token during auth request e.g. If you have added google provider with id `google` in `providers_json_file` then jans request will be look like `https://your-passport.com/passport/auth/google/<token>`. |

> Note: After adding and enabling successfully, you can check your Janssen's Auth Server OpenID Connect configuration by navigating to the following URL: https://your-jans-server.com/.well-known/openid-configuration. Find `"acr_values_supported"` and you should see `passport-social`. 

> Note: Once you initiate auth request from your RP Application make sure to add `acr_values=passport-social` in the request. acr_values is your script name.

Below is the `providers_json_file` format:

```
[
  {
    "id": "google",
    "displayName": "google",
    "requestForEmail": false,
    "emailLinkingSafe": false,
    "logoImg": "img-url-or-path"
  }
]
```

|Property|Description|
|--------|-----------|
|id|Unique string for your provider|
|displayName|This name will be shown on auth page|
|logoImg|This logo image will be shown on auth page e.g. `/jans-auth/img/glu_icon.png` |
|requestForEmail|It is not required to be `true`. If you set it to true then it will prompt a user to enter an email.|
|emailLinkingSafe|It is not required to be `true`. If you want to link to existing users then set it to true|

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

`<kid-unique-string>` is your kid which you need for passport  JWT `keyId` config. `keystore.jks` need it to configure `key_store_file` property at Janssen custom script configuration.

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
