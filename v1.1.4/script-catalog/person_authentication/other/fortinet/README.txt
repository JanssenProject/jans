# Fortinet - RADIUS server Authentication

## Overview 
This document explains how to configure the Gluu Server so that when a user logs in, an authentication request is made to Fortinet's remote RADIUS (Remote Authentication Dial-In User Service) server which then validates the user name and password. 

## Prerequisites 

- A Gluu Server (installation instructions [here](../installation-guide/index.md)) which will play the role of RADIUS client    
- The [Fortinet script](https://github.com/GluuFederation/oxAuth/blob/master/Server/integrations/fortinet/FortinetExternalAuthenticator.py) (included in the default Gluu Server distribution);   
- A Fortinet server which is the RADIUS server.     
- The jradius-client [jar library](https://sourceforge.net/projects/jradius-client/files/) added to oxAuth

    

## Fortinet Configuration

In `Authentication` -> `Radius Service` -> `Clients`, create a new client (which is Gluu server). Enter the "secret" which will be used by the interception script to exchange RADIUS packets.

## Gluu Server Configuration
### Add JRadius library to oxAuth

- Copy the jradius-client jar file to the following oxAuth folder inside the Gluu Server chroot: `/opt/gluu/jetty/oxauth/custom/libs` 

- Edit `/opt/gluu/jetty/oxauth/webapps/oxauth.xml` and add the following line:

    ```
    <Set name="extraClasspath">/opt/gluu/jetty/oxauth/custom/libs/jradius-client.jar</Set>
    ```
    
- [Restart](../operation/services.md#restart) the `oxauth` service     
    


### Enable Interception Script

Follow the steps below to enable Fortinet's RADIUS authentication:

1. Navigate to `Configuration` > `Person Authentication Scripts`   

1. Find the `fortinet` script.

    ![fortinet](../img/admin-guide/multi-factor/fortinet-custom-script.png)

1. Populate the properties table with the details from your Fortinet account:    


|	Property	|	Description		| Input value     |
|-----------------------|-------------------------------|---------------|
|RADIUS_SERVER_IP		|IP address of Fortinet's RADIUS Server		| 10.10.10.1 |
|RADIUS_SERVER_SECRET		|Configured when the RADIUS client is registered. | spam |
|RADIUS_SERVER_AUTH_PORT            |Authentication port | 1812 |
|RADIUS_SERVER_ACCT_PORT            |Accounting port | 1813 |

1. Enable the script by checking the box 

1. Scroll to the bottom of the page and click `Update`

Now authenticating a user against Fortinet's RADIUS server is possible from your Gluu Server. This means that, using OpenID Connect `acr_values`, applications can now request Fortinet authentication for users. 

!!! Note 
    To make sure this method has been enabled successfully, you can check your Gluu Server's OpenID Connect configuration by navigating to the following URL: `https://<hostname>/.well-known/openid-configuration`. Find `"acr_values_supported":` and you should see `"fortinet"`. 

### Make Fortinet's user-password authentication the Default mechanism.
If fortinet should be the default authentication mechanism, follow these instructions: 

1. Navigate to `Configuration` > `Manage Authentication`. 

1. Select the `Default Authentication Method` tab. 

1. In the Default Authentication Method window you will see two options: `Default acr` and `oxTrust acr`. 

![fortinet](../img/admin-guide/multi-factor/fortinet.png)

 - `oxTrust acr` sets the authentication mechanism for accessing the oxTrust dashboard GUI (only managers should have acccess to oxTrust).    

 - `Default acr` sets the default authentication mechanism for accessing all applications that leverage your Gluu Server for authentication (unless otherwise specified).    

If Fortinet should be the default authentication mechanism for all access, change both fields to fortinet.  
 
## Troubleshooting    
If problems are encountered, take a look at the logs, specifically `/opt/gluu/jetty/oxauth/logs/oxauth_script.log`. Inspect all messages related to Fortinet. For instance, the following messages show an example of correct script initialization:

```
Fortinet. Initialization
Fortinet. Initialized successfully
```

Also make sure you are using the latest version of the script that can be found [here](https://github.com/GluuFederation/oxAuth/blob/master/Server/integrations/fortinet/FortinetExternalAuthenticator.py).

