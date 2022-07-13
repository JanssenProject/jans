# Casa - Self-service MFA portal
## Overview

**Casa** is a self-service web portal that allows end users to view, enroll, and remove MFA credentials, including hardware tokens, software tokens, commercial services (like Duo), social login, biometric, and mobile.  Casa is also extensible. As new authentication technologies arise, you can download plugins to leverage them in your organization through Casa

## Prerequisites
- Installed Janssen Server  

## Installing Casa
Casa gets installed as an add-on to Janssen Server and integrates with `jans-auth-server` module as an OpenID connect Relying Party.
Casa can be installed using command below.  

Currently, Casa installation is supported on CentOS 8 and Ubuntu 20.

The following steps will install Casa as an add-on. 

  - Download installer 
  ```
    wget https://raw.githubusercontent.com/GluuFederation/flex/main/flex-linux-setup/flex_linux_setup/flex_setup.py  -O flex_setup.py
  ```
  - Run the installer
  ```
    python3 flex_setup.py
  ```
### Automate install

<br/>If you have `setup.properties` file and want to automate installation, you can pass properties file as
    ```
    python3 flex_setup.py -f /path/to/setup.properties -n -c
    ```

    Minimal example setup.properties file:

    ```
    ip=10.146.197.201
    hostname=flex.gluu.org
    orgName=Gluu
    admin_email=flex@gluu.org
    city=Austing
    state=Texas
    countryCode=US
    installLdap=True
    admin_password=MyAdminPassword
    ldapPass=MyLdapPassword
    casa_client_id=3000.7986c837-2a8f-4c31-9c63-1bd2f6abce77
    casa_client_pw=MyCasaClientSecret
    ```
3. You will be prompted: 
`Install Admin UI [Y/n]:`. Select `n`

4. You will be prompted: 
`Install Casa [Y/n]:`. Select `y`

5. At the end of the installation you will be presented with a URL to access the Casa portal. 
6. You can use this URL and log into the portal using administrator credentials that you created during the Janssen Server installation. 

***

## Quick configuration and Testing

### Administrator configuration:

1. Goto `/opt/jans/jetty/casa` folder and execute `touch .administrable`. [Further reading]()
2. [Enable authentication mechanisms](https://github.com/maduvena/jans-docs/wiki/Enabling-an-authentication-mechanism-(or-custom-script)) in Jan-auth server like `otp`, `fido2`, `email_otp`.
```
python3 /opt/jans/jans-setup/setup.py -enable-script="<inum_of_script>"
```
| Inum | displayName |
|---|---|
| 09A0-93D7 | smpp  |
| 5018-D4BF | otp |
| 5018-F9CF | duo |
| 8BAF-80D7 | fido2 |
| 92F0-BF9E | super_gluu |
| 09A0-93D6 | twilio_sms |
| 09A0-93D7 | smpp |
| 5018-D4BF | otp |
| 5018-F9CF | duo |
| 8BAF-80D7 | fido2 |
| 92F0-BF9E | super_gluu |

4. Enable authentication mechanisms in casa : Goto `Administration console` -> `Enabled Authentication methods`. Now select all the authentication mechanisms that you wish to enable for user self service and `Save` your selections.

### User self-service

1. Download [this json file](https://raw.githubusercontent.com/maduvena/jans-docs/main/create-user.json). Edit `username` and `password` fields. Save it as `/tmp/create-user.json` 
2. Use `jans-cli` to create a user. 
```
/opt/jans/jans-cli/scim-cli.py --operation-id create-user --data /tmp/create-user.json
```

3. Login using username and password of the newly created user, and go ahead enrolling credentials for the user. 

## Uninstalling Casa
Execute `python3 flex_setup.py -remove casa`