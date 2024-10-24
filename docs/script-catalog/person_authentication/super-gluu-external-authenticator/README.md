[Super Gluu](https://github.com/GluuFederation/super-gluu) is an open source, push-notification two-factor authentication (2FA) mobile app which can be integrated to work with the Janssen Server.
Super Gluu uses public-key encryption as specified in the [FIDO U2F authentication standard](https://fidoalliance.org/specifications/overview/). Upon device enrollment, Super Gluu registers its public key against the Janssen's FIDO Server's `super_gluu_registration_endpoint` endpoint. Authentication takes place at `super_gluu_authentication_endpoint` where a challenge response ensures that the device has the corresponding private key.

### Janssen's Super Gluu endpoint

The fido2 server serves the registration and authentication of Super Gluu credentials, the URI of which can be found at `https://<my.jans.server>/.well-known/fido2-configuration`. However, **this feature has to be enabled** first, else the endpoints `super_gluu_registration_endpoint` and `super_gluu_authentication_endpoint` will not show up by default.


### Prerequisites:

#### A. Install the FIDO2 server:
The Fido2 server would have been installed during the initial server setup.

If not installed, follow these steps to install the FIDO2 server.
1. `setup.py` was already run during installation. Run `setup.py` once again.
```
python3 /opt/jans/jans-setup/setup.py
```
2.  The script will detect that you already have a jans installation and you will be prompted to install the uninstalled components.
3. Select the `FIDO2 server` and complete the post install process.


#### B. Enable the Super Gluu endpoint:

1. Use the TUI to enable "Super Gluu" feature
![sg diagram](../../../assets/jans-tui-super-gluu-conf.png)

#### C. Mobile device with Super Gluu installed
   - [Super Gluu for iOS](https://itunes.apple.com/us/app/super-gluu/id1093479646?mt=8)     
   - [Super Gluu for Android](https://play.google.com/store/apps/details?id=gluu.org.super.gluu)   

#### D. Publicly Discoverable DNS :
- An Internet accessible (non-internal or localhost) Janssen Server with DNS pointing at the public Internet address of the server

- If the Jannsen Server is using a self-signed certificate, `Trust All` **must** be enabled in Super Gluu (open the app, navigate to `Menu` > `Trust all (SSL)` and enable)

!!! Note
    The Janssen Server and Super Gluu can work in the same network, without a DNS server hostname and with a self-signed certificate. There is only one limitation: both components should belong to the same network. Instead of assigning a hostname during Janssen Server installation, an IP address can be specified. In the Super Gluu mobile app, enable `Trust all (SSL)`.

#### E. Push Notification Server **hosted by Gluu**:
The Notification server should be configured to recieve push notifications on registered mobile devices. 

Configure `/etc/certs/super_gluu_creds.json`. For each Mobile app (Android, iOS); place the Access key / Secret key of Push Notification server.  
```
{
       "android":{
           "gluu":{
               "enabled":true,
               "access_key":"36W......BP",
               "secret_access_key":"ueq.....fek"
           }
       },
       "ios":{
           "gluu":{
               "enabled":true,
               "access_key":"auO......6V",
               "secret_access_key":"f0......oei"
           }
       },
       "gluu":{
           "server_uri":"https://api.gluu.org"
       }
   }
```


### User and Developer Guides
User and Developer Guides can be found on the [Super Gluu docs site](https://gluu.org/docs/supergluu).

### Interception script
- [Super Gluu interception script](../../../script-catalog/person_authentication/super-gluu-external-authenticator/SuperGluuExternalAuthenticator.py) is included in the default Janssen Server distribution

The Super Gluu Interception script can be configured to work in 2 modes:
A. Two step Flow (Default mode)
B. One step Flow


### A. Two step flow (Default mode):
#### Enrollment:
By default, users are put through a two-step, two-factor authentication process with username and password in the first step, and then push notification via Super Gluu in the second step.
```mermaid
sequenceDiagram
title SuperGluu Flow Enrollment "Two step"

autonumber 1
participant User
participant Mobile App
participant Browser
participant Notification server
participant Jans AS

User->> Browser: Log in step1 (userid , pwd)
Browser ->>Jans AS: send userid ,pwd
Jans AS->>Browser: validate, present enrollment (QR code) of SG
User->>Mobile App:  Scans the QR code
Mobile App->> Mobile App: Enrollment saved on app \n (username, appId, keyHandle, date)
Mobile App->>Jans AS :  Enrollment completed, <br/>enrollment data (registrationData, clientData, deviceData ) sent to Jans AS
Jans AS->>Jans AS: Enrollment entry stored in Database
Jans AS->>Browser: Enrollment completed.

```
##### A. Client data
An example of the contents of client data are as follows:
```
{
  "typ": "navigator.id.finishEnrollment",
  "challenge": "Bkpin2iUSEQrkuSC_wDlPwzLfVwRGxLbySpVm8jwkuk",
  "origin": "https://my.jans.server"
}
```

##### B. Device data  
An example of the contents of client data are as follows:

```
{
  "uuid": "BBA72798-9A1C-4866-819F-819C011ED129",
  "type": "iPhone",
  "platform": "ios",
  "name": "ABC’s iPhone",
  "os_name": "iOS",
  "os_version": "15.4",
  "custom_data": null,
  "push_token": "8fc4bd31e2ddbc5c5d83e4955cfd36f663ab37b426d0a4ec5c685c3a6335dd2c"
}
```
The push_token is used by the **Push Notification server**.

##### C. Registration data
This data contains following information: counter, status, rp-application, userInum, keyHandle, Public Key, attestation certificates
```
{
  "createdDate": 1678374227774,
  "updatedDate": 1678374228300,
  "createdBy": "johndoe",
  "updatedBy": "johndoe",
  "username": "johndoe",
  "domain": "/my.jans.server",
  "userId": "DAY7UJzdHaIXuEbQlNwBomQEPmqjsKbN4lblYqfbCZo",
  "challenge": "jr2LtLFRF2YG65pyo__gaKoiyP9oQbPyW6hGxdZBAS8",
  "attenstationRequest": "{\"super_gluu_request\":true,\"super_gluu_request_mode\":\"two_step\",\"super_gluu_app_id\":\"https:///my.jans.server/.well-known/openid-configuration\",\"username\":\"madhu1\",\"displayName\":\"madhu1\",\"session_id\":\"3d3f7560-8d83-4770-a6fb-cc83c787a511\",\"attestation\":\"direct\"}",
  "attenstationResponse": "{\"super_gluu_request\":true,\"super_gluu_request_mode\":\"two_step\",\"type\":\"public-key\",\"response\":{\"deviceData\":\"eyJuYW1lIjoiTmV4dXMgN......lLWU2ZjMtMzRlNC1hYWMxLTlhYTViOGI3MTUyOCJ9\",\"clientDataJSON\":\"eyJjaGFsbGVuZ2UiOiJqcjJM....QuZmluaXNoRW5yb2xsbWVudCJ9\",\"super_gluu_request_cancel\":false,\"attestationObject\":\"v2NmbXRzZmlkby11MmYtc3VwZXItZ2x1....gC5NYTNE-GAS3AUoD-DI9R__8\"},\"id\":\"-G1Bpm_7j0A2UIciKSPz7ku7pA2d9azzJyFQCd4Xh67Hh0gBDXAH9B--IX3hZYxtGEmY7nM01PV5Gq53O5FSsg\"}",
  "uncompressedECPoint": "v2ExAmEzJmItMQFiLTJYINmwCcnnXf6CxMCOG6bjAT5aXHCqIhgYDrBvEuKW6bWuYi0zWCARbzLZN8ytE1JiVAs0UMLoAuTWEzRPhgEtwFKA_gyPUf8",
  "publicKeyId": "-G1Bpm_7j0A2UIciKSPz7ku7pA2d9azzJyFQCd4Xh67Hh0gBDXAH9B--IX3hZYxtGEmY7nM01PV5Gq53O5FSsg",
  "type": "public-key",
  "status": "registered",
  "counter": 0,
  "attestationType": "fido-u2f-super-gluu",
  "signatureAlgorithm": -7,
  "applicationId": "https:///my.jans.server/.well-known/openid-configuration"
}
```
#### Authentication:
```mermaid
sequenceDiagram
title Super Gluu Authentication flow "Two step"
autonumber 1
User->>Browser: Login
Browser->>Jans AS:  username and pwd
Jans AS->>Jans AS: Authenticate
Jans AS->>Notification server:call push API, use push token from device entry
Notification server->>Mobile App: Sends push to mobile device
Jans AS->>Browser:  Present option of authenticating with QR code
User->>Mobile App: Approve / Reject Push
Mobile App->>Jans AS: Complete (finish) authentication process, send push_token for storing in Device entry of Gluu server
Jans AS->Jans AS: validate response, If successful, update device entry,
```

### B. One step flow
An alternative authentication workflow, password-less authentication, can be configured by adjusting the script property `authentication_mode`.

```mermaid
sequenceDiagram
title SuperGluu Flow Enrollment "One step"

autonumber 1
participant User
participant Mobile App
participant Browser
participant Notification server
participant Jans AS

User->> Browser: RP page which invokes /authorize call
Jans AS->>Browser: validate, present enrollment (QR code) of SG
User->>Mobile App:  Scans the QR code
Mobile App->> Mobile App: Enrollment saved on app \n ( appId, keyHandle, date)
Mobile App->>Jans AS :  Enrollment completed, <br/>enrollment data (registrationData, clientData, deviceData ) sent to Jans AS
Jans AS->>Jans AS: Enrollment entry stored temp entry in database under ou=fido2_register,ou=fido2,o=jans
Jans AS->>Browser: Prompt user to username-password page, inorder to link the enrollment to a user
Browser->>Jans AS: send user creds
Jans AS->>Jans AS: create enrollment entry attached to the user and delete temp entry under ou=fido2_register,ou=fido2,o=jans
Jans AS->>Browser: enrollment completed

```
```mermaid
sequenceDiagram
title Super Gluu Authentication flow "One step"
autonumber 1

participant User
participant Mobile App
participant Browser


Browser->>Jans AS: /authorize request
Jans AS->>Browser: Present QR code for scanning
Mobile App->>Browser: Code is scanned
User->>Mobile App: Approve / Reject
Mobile App->> Jans AS:Send authentication request
Jans AS->> Jans AS: no username, search by keyhandle
Jans AS->> Jans AS: verify Super Gluu credentials,  If successful, update device entry
Jans AS->>Browser: Authentication completed
```

### Custom script Properties
The Super Gluu authentication script has the following properties:

|	Property	|	Description		|	Example	|
|-----------------------|-------------------------------|---------------|
|authentication_mode	|Determine factor of authentication - `two_step` or `one_step`|two_step |
|credentials_file	|JSON file for SuperGluu 		|/etc/certs/super_gluu_creds.json|
|label  |The name of the application |   Super Gluu|
|notification_service_mode   | Service used to enable push notifications | gluu|
|qr_options| Size of the QR code that is used for enrollment and/or authentication|{ size: 500, mSize: 0.05 }|
|registration_uri | Registration endpoint of the IDP| https://idp.example.com/identity/register|
|supergluu_android_download_url| Android app download link, used in the login page | https://play.google.com/store/apps/details?id=gluu.super.gluu|
|supergluu_ios_download_url| iOS app download link, used in the login page | https://itunes.apple.com/us/app/super-gluu/id1093479646|


### Notes for administrator:
Configure `/etc/certs/super_gluu_creds.json`: For each Android and apple app, configure Access key / Secret key of oxNotify server

### Enable Sign-in with Super-Gluu Authentication script
Using the OpenID Connect acr_values parameter, web and mobile clients can request any enabled authentication mechanism. To enable Super Gluu as an authentication method, follow the steps below:

1. Obtain the json contents of `super_gluu` custom script by using a jans-cli command like `get-config-scripts-by-type`, `get-config-scripts-by-inum` etc.

e.g : `/opt/jans/jans-cli/config-cli.py --operation-id get-config-scripts-by-type --url-suffix type:PERSON_AUTHENTICATION `, `/opt/jans/jans-cli/config-cli.py --operation-id get-config-scripts-by-inum --url-suffix inum:6122281b-b55d-4dd0-8115-b098eeeee2b7`

2. Update the custom script and change the enabled attribute to `true`
Now Sign-in with Super-Gluu is an available authentication mechanism for your Janssen Server. This means that, using OpenID Connect acr_values, applications can now request Super-Gluu authentication for users.

!!! Note To make sure `super_gluu` has been enabled successfully, you can check your Janssen's Auth Server OpenID Connect configuration by navigating to the following URL: https://<hostname>/.well-known/openid-configuration. Find "acr_values_supported": and you should see "super_gluu".

### Make Sign-in with Super-Gluu Script as default authentication script:

Use this [link](https://github.com/JanssenProject/jans-cli-tui/blob/vreplace-janssen-version/docs/cli/cli-default-authentication-method.md) as a reference.

Steps:
1. Create a file say `sg-auth-default.json` with the following contents
```
{
  "defaultAcr": "super_gluu"
}
```
2.Update the default authentication method to super_gluu
```
/opt/jans/jans-cli/config-cli.py --operation-id put-acrs --data /tmp/sg-auth-default.json
```

### Test the feature
To test, enter the complete URL for authorization in a browser or create a simple webpage with a link that simulates the user sign-in attempt. If the server is configured properly, the first page for the selected authentication method will be displayed to the user.

An example of a complete URL looks like this -
```
https://<your.jans.server>/jans-auth/authorize.htm?response_type=code&redirect_uri=https://<your.jans.server>/admin&client_id=<replace_with_inum_client_id>&scope=openid+profile+email+user_name&state=faad2cdjfdddjfkdf&nonce=dajdffdfsdcfff
```

### Customizations to Super Gluu Login Pages

The Gluu Server includes a [default public-facing pages for Super Gluu ](https://github.com/JanssenProject/jans/blob/vreplace-janssen-version/jans-auth-server/server/src/main/webapp/auth/super-gluu/login.xhtml) for enrollment and authentication.

To customize the look and feel of the pages, follow the [customization guide](https://github.com/JanssenProject/jans/blob/vreplace-janssen-version/docs/admin/developer/customization/customize-web-pages.md).

### Self-service
To offer end-users a portal where they can manage their own account security preferences, including two-factor authentication credentials like Super Gluu, check out our new app, [Gluu Casa](https://casa.gluu.org).

### Manual Device Management
![image](../../../assets/SG_registration.png)

A user's Super Gluu device(s) can be removed by a Janssen administrator by directly locating the user entry in MySQL. For example, let's say user `abc` loses their device and wants to enroll a new device to use Super Gluu.

1. Find the `DN` of the user in MySQL

1. Find the registation under `ou=fido2_register` for associated with the user

1. Remove the `jansId DN`

Now the old device is gone and the user can enroll a new device

### Device management using SCIM
See the [SCIM documentation](../../../janssen-server/usermgmt/usermgmt-scim.md#fido-2-devices) on how to manage Super Gluu devices, using the SCIM [protocol].
