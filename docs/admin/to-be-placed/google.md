# Sign-in using Google Account

## Overview 

Google's OAuth 2.0 APIs can be used for both authentication and authorization. This document describes how to integrate Google's OAuth 2.0 implementation for authentication thus allowing a user to sign in using his Google account.

   
## Prerequisites 

- A Jans-auth Server (installation instructions [here](https://github.com/JanssenProject/jans/tree/main/jans-linux-setup#readme))    
- The [Google authentication script](https://github.com/JanssenProject/jans/tree/main/jans-linux-setup/jans_setup/static/extension/person_authentication/GoogleExternalAuthenticator.py) (included in the default Gluu Server distribution);   
- A [Google account](https://accounts.google.com/).     
- Google API jars namely [google-api-client](https://repo1.maven.org/maven2/com/google/api-client/google-api-client/1.33.2/google-api-client-1.33.2.jar), [google-oauth-client](https://repo1.maven.org/maven2/com/google/oauth-client/google-oauth-client/1.33.1/google-oauth-client-1.33.1.jar) and [google-http-client-jackson2](https://repo1.maven.org/maven2/com/google/http-client/google-http-client-jackson2/1.41.5/google-http-client-jackson2-1.41.5.jar) added to jans-auth-server 

    
## Google Configuration

You need OAuth 2.0 credentials, including a client ID and client secret, to authenticate users and gain access to Google's APIs.
The following steps explain how to create credentials for your project. Your applications can then use the credentials to access APIs that you have enabled for that project.

1. Go to the [Credentials page](https://console.developers.google.com/apis/credentials).
2. Click Create credentials > OAuth client ID.
3. Select the Web application application type.
4. Name your OAuth 2.0 client and click Create


## Configure jans-auth server

### Download Google Client JSON file
On the following page, https://console.cloud.google.com/apis/credentials, you will see a table containing your recently created client. Click on the download button and download the JSON file containing details of the Client. 
Place this file in `/etc/certs/google_client_secret.json`

### Add Google libraries to jans-auth-server

- Copy the [google-api-client](https://repo1.maven.org/maven2/com/google/api-client/google-api-client/1.33.2/google-api-client-1.33.2.jar), [google-oauth-client](https://repo1.maven.org/maven2/com/google/oauth-client/google-oauth-client/1.33.1/google-oauth-client-1.33.1.jar) and [google-http-client-jackson2](https://repo1.maven.org/maven2/com/google/http-client/google-http-client-jackson2/1.41.5/google-http-client-jackson2-1.41.5.jar) to the following folder inside the jans-auth Server : `/opt/jans/jetty/jans-auth/custom/libs` 

- Edit `/opt/jans/jetty/jans-auth/webapps/oxauth.xml` and add the following line:

    ```
    <Set name="extraClasspath">./custom/libs/google-oauth-client-1.33.1.jar,./custom/libs/google-api-client-1.33.2.jar,./custom/libs/google-http-client-jackson2-1.41.5.jar</Set></Configure>
    ```
    
- Restart the `jans-auth` service     
` systemctl status jans-auth `
    
### Properties

The custom script has the following properties:    

|	Property	|	Description		| Input value     |
|-----------------------|-------------------------------|---------------|
|google_creds_file		|Details of the client created on https://console.cloud.google.com/apis/credentials		| /etc/certs/google_client_secret.json|

### Enable Google Script using Admin Console

Follow the steps below to enable Google authentication:

1. Navigate to `Admin` > `Scripts`   

1. Find the `google` script.

    

1. Populate the properties table :    

   - `google_creds_file`: `/etc/certs/google_client_secret.json`.   
   
1. Enable the script by checking the box 

1. Scroll to the bottom of the page and click `Update`



!!! Note 
    To make sure Google has been enabled successfully, you can check your Gluu Server's OpenID Connect configuration by navigating to the following URL: `https://<hostname>/.well-known/openid-configuration`. Find `"acr_values_supported":` and you should see `"google"`. 

### Make Google the Default Authentication mechanism
If `google` should be the default authentication mechanism, follow these instructions: 

1. Navigate to `OAuth Server` > `Configuration` > `Defaults` > `ACR and Logging`. 

1. Select the `Default Authentication Method(Acr):` to `google`. 

1. Click Save    

