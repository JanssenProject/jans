# Obconnect Openbanking solution
## Overview
[Obconnect's](https://obconnect.io/) Open Banking Solution built to support key UKOBIE 3.1 standards. 

This document explains how obconnect integrates with the jans-auth-server in the sequence of events of Open Banking Login Flow detailed below:
```
Person->Browser: Pay
Browser->TPP: 
TPP->Browser: redirect
Browser->Gluu OP:
Gluu OP->Person_Auth_Script:
Person_Auth_Script->Consent App: Step 1
Consent App->Browser: redirect
Browser->Internal OP: 
Internal OP->Browser: display login page
Person->Browser: creds
Browser->Internal OP:
Internal OP<->Consent App: finish login
Consent App->Browser: Display Consent Info
Person->Browser: Approve
Browser->Consent App: 
Consent App->Person_Auth_Script:
Person_Auth_Script->Person_Auth_Script: write consent_id, etc to session
Person_Auth_Script->Person_Auth_Script: Create Person entity in database
note over Person_Auth_Script: inum=1234,ou=people,o=gluu\nexp: 10 min\nconsent_id: xxxn...\n (entity gets cleaned up)
Person_Auth_Script->id_token_Script:
id_token_Script->id_token_Script: add claims from session
Person_Auth_Script->TPP: return code, id_token
TPP<->Gluu OP: openid stuff
TPP->Browser: success
```
##	Participants
|Player	     |   Notes                                                                                              |
|------------|------------------------------------------------------------------------------------------------------|
|PSU	     |Payment Service User interacts with TPP and ASPSP through a browser or App.                           |
|TPP	     |A Third-Party Provider for payments and accounts.                                                     |
|ASPSP	     |Account Servicing Payment Service Provider which is a combination of Obconnect’s Open Banking Solution APIs and Bank’s Services.|
|Auth Server |The Authorization Server which is a provider for OAuth 2.0 and OpenID Connect 1.0.|


## Prerequisites
- A jans-auth-server ([installation instructions](../installation-guide/index.md))
- [obconnect interception script](https://github.com/JanssenProject/jans-auth-server/tree/master/server/integrations/obconnect/) 


## Properties
The mandatory properties in the obconnect authentication script are as follows
|	Property	|	Description		|	Example	|
|-----------------------|-------------------------------|---------------|
|sharedSecret           |The Consent App then decrypts and validates the session data using the encryption key already shared between auth server and ASPSP|hfert(o234crwwrewerwvssdywevndz)+sdfsds|
|tpp_client_id	        |TPP client id |`c20b04cc-776a`|
|client_name	        |Client name |Obconnect |
|organization_name	|Organization name |`OBConnect`|
|expiry	                |Expiry of JWT containing sessionData |`161371681475`|
|consent_app_server_name|ASPS |`https://asps.com`|
|hostname|Host name of the AS|`https://myjansserver.com`|



## Call from TPP to jans-auth-server
	TPP calls the OAuth Provider to initiate Authorization at authorization endpoint with client and consent details.
        The client details are configured as parameters to the interception script. The consent details have to be sent to the /authorize endpoint in the request object as an encoded JWT 
        eg - Original payload - {"openbanking_intent_id": "72663272-0648-427c-bad4-6dc2cdc0c0a1" } is sent in the URL as follows - 
        /authorize?request=eyJlbmMiOiJBMjU2R0NNIiwiYWxnIjoiZGlyIn0..KWXWLfRK9_u6WLqt.ygNB3L1WAss-VF2fRsNTTLR4l0UUKMTsZNKQ1P1dg0LeOIXXB8Mw0Vb85Yd85wNBnzuHEQVR2ks6OnqqabtuWipN.PCW3OTEI1LCPXLQweEYjWQ





!!! Note 
    To make sure Obconnect has been enabled successfully, you can check your Gluu Server's OpenID Connect configuration by navigating to the following URL: `https://<hostname>/.well-known/openid-configuration`. Find `"acr_values_supported":` and you should see `"obconnect"`. 

## Make Obconnect the Default Authentication Mechanism

Now applications can request obconnect's authentication and consent flow. To make obconnect your default authentication mechanism, follow these instructions in this document set the default authentication mechanism to "obconnect" 

https://github.com/JanssenProject/jans-cli-tui/blob/v1.0.6/README.md