# 2FA Resource Owner Password Script Using Twilio

## Overview 

  This resource owner password script implements two factor 
authentication using twilio. This could easily be adjusted 
for other 2FA use cases (e.g. using a Yubikey or a magic link).

## Requirements

  The script is written for jans , meaning it will only work on a Janssen 
or a Gluu Flex installation. An additional requirement is a twilio account 
with twilio API credentials. 

## Application Flow and Sequence Diagram 
  The application flow is described below, alongside a 
[sequence diagram](./sequence%20diagram.txt)
In the first step, the user provides only his username or email (through the RP). 
The script then checks the username against the backend database. If the user is found, and 
has a phone number associated with his account, an OTP code is sent to the associated number. 
The user (through the RP) is prompted for the OTP code and the RP sends another authentication
request to the script to validate the OTP code.
If the OTP code is valid, the user provides his password which is then validated by the script.
Only then is the access token issued and authentication succeed. 
![sequence diagram](./sequence%20diagram.png)

## Flex Server Configuration

1. Open your Flex UI and go to `Admin` > `Scripts` > and add a new custom script.
2. Make sure the script type is `RESOURCE_OWNER_PASSWORD_CREDENTIALS`. 
3. The script contents should be  [this script](scripts/GamatechRopc.py).
4. Add the following custom properties to the new script
   - `twilio_account_sid` containing your twilio SID 
   - `twilio_auth_token` containing your twilio authentication token
   - `twilio_from_number` containing the "from" twilio number
5. Save your changes.
6. Go to `Auth Server` > `Clients` and add a new client (click 
   on the + sign in the top left)
7. Create a new client , making sure it has the `password` grant and the `token` response
   types.
8. In the `Client Scripts` tab , make sure to add the script you created above to the `Password Grant` list of scripts.


Step (7) can be skipped if this is to be used with an existing OpenID client.