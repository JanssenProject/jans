# SMS One-Time Password (OTP) Authentication

SMS is a common technology used for the delivery of OTPs. Text messages provide a ubiquitous communication channel, and are directly available in nearly all mobile handsets and, through text-to-speech conversion, any mobile or landline telephone. 

This document explains how to configure the Gluu Server for two-step, two-factor authentication (2FA) with username / password as the first step, and an OTP sent via text message as the second step. 

!!! Note
    As indicated, this script uses the [Twilio cloud communications platform](https://www.twilio.com) to deliver SMS messages.     
    
## Prerequisites 

- The [Twilio SMS OTP script](https://github.com/JanssenProject/jans/blob/main/docs/script-catalog/person_authentication/twilio-2fa/twilio2FA.py) (included in the default Janssen Server distribution);   
- A [Twilio account](https://www.twilio.com/).     
- The twilio [jar library](http://search.maven.org/remotecontent?filepath=com/twilio/sdk/twilio/7.17.6/twilio-7.17.6.jar) added to jans-auth
- A mobile device and phone number that can receive SMS text messages
    

## Twilio as a 2FA method

Twilio offers Voice, SMS, and MMS capabilities, but we will only need SMS for the purpose of this document. 

When registering for a Twilio account, you will be asked to verify your personal phone number, and then will be given a Twilio phone number. 

Ensure the number given is [SMS enabled](https://support.twilio.com/hc/en-us/articles/223183068-Twilio-international-phone-number-availability-and-their-capabilities) and supports sending messages to the countries you are targeting. You may need to enable countries manually (see the [Geo permissions page](https://www.twilio.com/console/sms/settings/geo-permissions)).

Twilio trial accounts only allow sending messages to mobile numbers already linked to the account, so for testing you will want to add (and verify) some additional numbers besides your personal one to make sure the integration is working as expected. When you are ready to move to production, you will want to purchase a Twilio plan.

## Add Twilio library to jans-auth

- Copy the Twilio jar file to the following jans-auth folder inside the Gluu Server chroot: `/opt/jans/jetty/jans-auth/custom/libs` 

- Edit `/opt/jans/jetty/jans-auth/webapps/jans-auth.xml` and add the following line:

    ```
    <Set name="extraClasspath">/opt/jans/jetty/jans-auth/custom/libs/twilio.jar</Set>
    ```
    
- [Restart](../../../janssen-server/vm-ops/restarting-services.md#restart) the `jans-auth` service
## Properties

The custom script has the following properties:    

|	Property	|	Description		| Input value     |
|-----------------------|-------------------------------|---------------|
|twilio_sid		|Twilio account SID		| Obtain from your Twilio account|
|twilio_token		|Access token associated to Twilio account| Obtain from your Twilio account|
|from_number            |Twilio phone number assigned to the account| Obtain from your Twilio account|


## Enable twilio_sms script

By default, users will get the default authentication mechanism as specified above. However, **using the OpenID Connect acr_values parameter, web and mobile clients can request any enabled authentication mechanism**.

1. Obtain the json contents of `twilio_sms` custom script by using a jans-cli command like `get-config-scripts-by-type`, `get-config-scripts-by-inum` etc.

e.g : `/opt/jans/jans-cli/config-cli.py --operation-id get-config-scripts-by-type --url-suffix type:PERSON_AUTHENTICATION` , `/opt/jans/jans-cli/config-cli.py --operation-id get-config-scripts-by-inum --url-suffix inum:6122281b-b55d-4dd0-8115-b098eeeee2b7`

2. [Update the custom script](https://github.com/JanssenProject/jans-cli/blob/vreplace-janssen-version/docs/cli/cli-custom-scripts.md#update-an-existing-custom-script) and change the `enabled` attribute to `true`  

Now `twilio_sms` is an available authentication mechanism for your Janssen Server. This means that, using OpenID Connect `acr_values`, applications can now request `twilio_sms` authentication for users.

!!! Note
    To make sure `twilio_sms` has been enabled successfully, you can check your Janssen's Auth Server OpenID Connect
    configuration by navigating to the following URL: `https://<hostname>/.well-known/openid-configuration`.
    Find `"acr_values_supported":` and you should see `twilio_sms` .

## Enable `twilio_sms` Script as default authentication script:
Use this [link](https://github.com/JanssenProject/jans-cli-tui/blob/vreplace-janssen-version/docs/cli/cli-default-authentication-method.md) as a reference.
Follow the steps below to enable `twilio_sms` authentication:
1. Create a file say `twilio_sms` -auth-default.json` with the following contents
```
{
  "defaultAcr": "`twilio_sms` "
}
```
2.Update the default authentication method to ``twilio_sms` `
```
/opt/jans/jans-cli/config-cli.py --operation-id put-acrs --data /tmp/twilio_sms-auth-default.json
```



    
    
### Test the feature 
To test, enter the complete URL for authorization in a browser or create a simple webpage with a link that simulates the user sign-in attempt. If the server is configured properly, the first page for the selected authentication method will be displayed to the user.

An example of a complete URL looks like this -
```
https://<your.jans.server>/jans-auth/authorize.htm?response_type=code&redirect_uri=https://<your.jans.server>/admin&client_id=<replace_with_inum_client_id>&scope=openid+profile+email+user_name&state=faad2cdjfdddjfkdf&nonce=dajdffdfsdcfff
```

## SMS OTP Login Pages

The Jans Server includes one page for SMS OTP:

1. A **login** page that is displayed for all SMS OTP authentications. 
![image](https://github.com/JanssenProject/jans/assets/12072533/3a8feb9b-8e4f-46ba-b524-cd5e89113c6c)



The designs are being rendered from the [SMS xhtml page](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/webapp/auth/otp_sms/otp_sms.xhtml). To customize the look and feel of the pages, follow the [customization guide](https://github.com/JanssenProject/jans/blob/main/docs/admin/developer/customization/customize-web-pages.md).


## Using SMS OTP

### Phone Number Enrollment

The script assumes the user phone number is already stored in his corresponding MySQL entry (attribute `phoneNumberVerified`). You can change the attribute by altering the script directly (see authenticate routine).

### Subsequent Logins
All <!--subsequent--> authentications will trigger an SMS with an OTP to the registered phone number. Enter the OTP to pass authentication. 

### Credential Management
    
A user's registered phone number can be removed by a Gluu administrator either via the jans TUI, or in MySQL under the user entry. Once the phone number has been removed from the user's account, the user can re-enroll a new phone number following the [phone number enrollment](#phone-number-enrollment) instructions above. 

## Troubleshooting    
If problems are encountered, take a look at the logs, specifically `/opt/jans/jetty/jans-auth/logs/jans-auth_script.log`. Inspect all messages related to Twilio. For instance, the following messages show an example of correct script initialization:

```
Twilio SMS. Initialization
Twilio SMS. Initialized successfully
```

Also make sure you are using the latest version of the script that can be found [here](https://github.com/JanssenProject/jans/blob/main/docs/script-catalog/person_authentication/twilio-2fa/twilio2FA.py).

## Self-service account security

To offer end-users a portal where they can manage their own account security preferences, including two-factor authentication credentials like phone numbers for SMS OTP, check out our new app, [Gluu Casa](https://casa.gluu.org). 
