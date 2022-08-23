# FIDO2

## Overview
[FIDO 2.0 (FIDO2)](https://fidoalliance.org/fido2/) , an open authentication standard that enables people to leverage common devices to authenticate to online services in both mobile and desktop environments. The Janssen server includes a FIDO2 server implementation. This enables authentications by using  platform authenticators embedded into a person's device or  physical USB, NFC or Bluetooth security keys that are inserted into a USB slot of a computer.

FIDO2 is comprised of the [W3C’s Web Authentication specification (WebAuthn)](https://www.w3.org/TR/webauthn/) and FIDO’s corresponding [Client-to-Authenticator Protocol (CTAP)](https://fidoalliance.org/specs/fido-v2.0-ps-20170927/fido-client-to-authenticator-protocol-v2.0-ps-20170927.html). WebAuthn defines a standard web API that can be built into browsers and related web platform infrastructure to enable online services to use FIDO Authentication. CTAP enables external devices such as mobile handsets or FIDO Security Keys to work with WebAuthn and serve as authenticators to desktop applications and web services.

This document explains how to use the Janssen Auth Server's built-in 
[FIDO2 interception script](https://github.com/JanssenProject/jans/blob/main/jans-linux-setup/jans_setup/static/extension/person_authentication/Fido2ExternalAuthenticator.py) 
to implement a two-step, two-factor authentication (2FA) with username / password as the first step, and any FIDO2 device as the second step. 

## Prerequisites
- A Janssen Server ([installation instructions](https://github.com/JanssenProject/jans#installation))      
- [FIDO2 interception script](https://github.com/GluuFederation/oxAuth/blob/master/Server/integrations/fido2/Fido2ExternalAuthenticator.py) (included in the default Gluu Server distribution);     
- At least one FIDO2 device for testing, like one of the devices [listed below](#fido2-devices). 

### FIDO2 devices
Some well known FIDO2 devices and manufacturers include:           

- [Yubico](https://www.yubico.com/)      
- [Vasco DIGIPASS SecureClick](https://www.vasco.com/products/two-factor-authenticators/hardware/one-button/digipass-secureclick.html)   
- [HyperFIDO](http://hyperfido.com/)       
- [Feitian Technologies](http://www.ftsafe.com/)   
- [AuthenTrend](https://authentrend.com/)
- [Apple's built-in Touch ID](https://support.apple.com/en-in/guide/mac-help/mchl16fbf90a/mac)

[Purchase FIDO2 devices on Amazon](https://www.amazon.com/s/ref=nb_sb_noss/146-0120855-4781335?url=search-alias%3Daps&field-keywords=fido2). Or, check [FIDO's certified products](https://fidoalliance.org/certification/fido-certified-products/) for a comprehensive list of FIDO2 devices (sort by `Specification` == `FIDO2`). 

## Properties
The script has the following properties

| 	Property	         | 	Description		                 | 	Example	                   |
|--------------------|--------------------------------|-----------------------------|
| fido2_server_uri		 | URL of the Janssen's FIDO2 server | `https://idp.mycompany.com` |

## Enable FIDO2 script

By default, users will get the default authentication mechanism as specified above. However, **using the OpenID Connect acr_values parameter, web and mobile clients can request any enabled authentication mechanism**.

1. Obtain the json contents of `fido2` custom script by using a jans-cli command like `get-config-scripts-by-type`, `get-config-scripts-by-inum` etc. 

e.g : `/opt/jans/jans-cli/config-cli.py --operation-id get-config-scripts-by-type --url-suffix type:PERSON_AUTHENTICATION` , `/opt/jans/jans-cli/config-cli.py --operation-id get-config-scripts-by-inum --url-suffix inum:6122281b-b55d-4dd0-8115-b098eeeee2b7`

2. [Update the custom script](https://github.com/JanssenProject/jans-cli/blob/main/docs/cli/cli-custom-scripts.md#update-an-existing-custom-script) and change the `enabled` attribute to `true`  

Now FIDO2 is an available authentication mechanism for your Janssen Server. This means that, using OpenID Connect `acr_values`, applications can now request FIDO2 authentication for users. 

!!! Note 
    To make sure FIDO2 has been enabled successfully, you can check your Janssen's Auth Server OpenID Connect 
    configuration by navigating to the following URL: `https://<hostname>/.well-known/openid-configuration`. 
    Find `"acr_values_supported":` and you should see `"fido2"`. 

## Enable FIDO2 Script as default authentication script:
Use this [link](https://github.com/JanssenProject/jans-cli/blob/main/docs/cli/cli-default-authentication-method.md) as a reference.
Follow the steps below to enable FIDO2 authentication:
1. Create a file say `fido2-auth-default.json` with the following contents
```
{
  "defaultAcr": "fido2"
}
```
2.Update the default authentication method to `fido2`
```
/opt/jans/jans-cli/config-cli.py --operation-id put-acrs --data /tmp/fido2-auth-default.json
```


!!! Note
    If FIDO2 is set as a default authentication mechanism users will **not** be able to access the protected resource(s) while using a mobile device or a browser that does not support FIDO2 (e.g. Internet Explorer).  

## FIDO2 login page
Below is an illustration of the Janssen Server's default FIDO2 login page:

![fido2](https://github.com/JanssenProject/jans/raw/main/docs/assets/image_fido2.png)

The design is being rendered from the [FIDO2 xhtml page](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/webapp/auth/fido2/login.xhtml). To customize the look and feel of this page, follow the [customization guide](https://jans.io/docs/admin/developer/customization/customize-web-pages/). 

## Using FIDO2 tokens 

### Credential enrollment
FIDO2 device enrollment happens during the first authentication attempt. 

### Subsequent authentications
All subsequent FIDO2 authentications for that user account will require the enrolled FIDO2 key. 

### FIDO2 credential management
A user's FIDO2 devices can be removed by a Janssen administrator in LDAP under the user entry as shown in the below screenshot. 

![fido2](https://github.com/JanssenProject/jans/raw/main/docs/assets/image-fido2-ldap-structure.png)
Diagram source in mermaid.live
```
graph TD
     
    A[ou=jans] --> K(ou=people)
    K --> K1[inum=....]
    K1 --> K11[ou=fido2_register]
    K11 --> K111[oxId=....]
    K11 --> K112[oxId=....]
    K11 --> K112[oxId=....]

    K1 --> K12[ou=fido2_auth]
    K12 --> K121[oxId=....]
    K12 --> K122[oxId=....]
    K12 --> K123[oxId=....]

    K --> K2[inum=....]
    K2 --> K21[ou=fido2_register]
    K21 --> K211[oxId=....]
    K21 --> K212[oxId=....]
    K21 --> K212[oxId=....]

    K2 --> K22[ou=fido2_auth]
    K22 --> K221[oxId=....]
    K22 --> K222[oxId=....]
    K22 --> K223[oxId=....]

    K --> K3[inum=....]
    K3 --> K31[ou=fido2_register]
    K31 --> K311[oxId=....]
    K31 --> K312[oxId=....]
    K31 --> K312[oxId=....]

    K3 --> K32[ou=fido2_auth]
    K32 --> K321[oxId=....]
    K32 --> K322[oxId=....]
    K32 --> K323[oxId=....]
    
```

### FIDO2 discovery endpoint
A discovery document for FIDO2 is published by the Janssen Server at: https://your.hostname/.well-known/fido2-configuration This document specifies the URL of the registration and authentication endpoints.