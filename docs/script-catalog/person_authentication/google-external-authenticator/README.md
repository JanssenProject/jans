---
tags:
  - administration
  - recipes
---

## Social Login with Google

An out-of-the-box feature, the Google Authentication script is a `PersonAuthenticationType` script which enables a user to sign-in using Google credentials. Google's OAuth 2.0 APIs are used for this. After users authenticate using their Google credentials, their Google credentials are provisioned into the Jans-auth server.

## Prerequisites

- A Jans-auth Server (installation instructions [here](https://github.com/JanssenProject/jans/tree/vreplace-janssen-version/jans-linux-setup#readme))    
- The [Google authentication script](https://github.com/JanssenProject/jans/tree/vreplace-janssen-version/jans-linux-setup/jans_setup/static/extension/person_authentication/GoogleExternalAuthenticator.py) (included in the default Jans-auth Server distribution);   
- A [Google account](https://accounts.google.com/).     
- Google API jars namely [google-api-client](https://repo1.maven.org/maven2/com/google/api-client/google-api-client/1.33.2/google-api-client-1.33.2.jar), [google-oauth-client](https://repo1.maven.org/maven2/com/google/oauth-client/google-oauth-client/1.33.1/google-oauth-client-1.33.1.jar) and [google-http-client-jackson2](https://repo1.maven.org/maven2/com/google/http-client/google-http-client-jackson2/1.41.5/google-http-client-jackson2-1.41.5.jar) added to jans-auth-server


## Configuring Google API keys:

On the Google side, you need OAuth 2.0 credentials, including a client ID and client secret, to authenticate users and gain access to Google's APIs.
The following steps explain how to create credentials for your project. Your applications can then use the credentials to access APIs that you have enabled for that project.

1. Go to the [Credentials page](https://console.developers.google.com/apis/credentials).
2. Click Create credentials > OAuth client ID.
3. Select the Web application application type.
4. Name your OAuth 2.0 client and click Create
5. Configure **Authorized redirect URIs** , click **ADD URI** to add the Janssen's `https://my.auth.server/postlogin.htm` where the control returns back to the AS.
6. Configure **Authorized JavaScript origins**, click **ADD URI** to add Janssen's FQDN `https://my.auth.server` because we are using [Google Client JS](https://developers.google.com/identity/gsi/web/guides/client-library) and it needs valid JS origin.

## Configure jans-auth server

### Download Google Client JSON file
On the following page, https://console.cloud.google.com/apis/credentials, you will see a table containing your recently created client. Click on the download button and download the JSON file containing details of the Client.
Place this file in `/etc/certs/google_client_secret.json`

### Add Google libraries to jans-auth-server

1. Copy the library files to `/opt/jans/jetty/jans-auth/custom/libs`
- `cd /opt/jans/jetty/jans-auth/custom/libs `
- `wget https://repo1.maven.org/maven2/com/google/api-client/google-api-client/1.33.2/google-api-client-1.33.2.jar -O google-api-client-1.33.2.jar`
- `wget https://repo1.maven.org/maven2/com/google/oauth-client/google-oauth-client/1.33.1/google-oauth-client-1.33.1.jar -O google-oauth-client-1.33.1.jar`
- `wget https://repo1.maven.org/maven2/com/google/http-client/google-http-client-jackson2/1.41.5/google-http-client-jackson2-1.41.5.jar -O google-http-client-jackson2-1.41.5.jar`


2. Edit `/opt/jans/jetty/jans-auth/webapps/oxauth.xml` and add the following line:

    ```
    <Set name="extraClasspath">./custom/libs/google-oauth-client-1.33.1.jar,./custom/libs/google-api-client-1.33.2.jar,./custom/libs/google-http-client-jackson2-1.41.5.jar</Set></Configure>
    ```

3. Restart the `jans-auth` service     
` systemctl status jans-auth `

### Properties

The custom script has the following properties:    

|	Property	|	Description		| Input value     |
|-----------------------|-------------------------------|---------------|
|`google_creds_file`		|Details of the client created on https://console.cloud.google.com/apis/credentials. See [this](https://github.com/maduvena/jans-docs/wiki/Google-Authentication-Script/_edit#download-google-client-json-file) step.		| `/etc/certs/google_client_secret.json`|

To update this setting in Jans persistence, follow this [link](https://github.com/JanssenProject/jans-cli/blob/vreplace-janssen-version/docs/cli/cli-custom-scripts.md#update-an-existing-custom-script)

### Enable Sign-in with Google Authentication script
By default, users will get the default authentication mechanism as specified above. However, using the OpenID Connect acr_values parameter, web and mobile clients can request any enabled authentication mechanism.

Obtain the json contents of `google` custom script by using a jans-cli command like get-config-scripts-by-type, get-config-scripts-by-inum etc.
```
e.g : /opt/jans/jans-cli/config-cli.py --operation-id get-config-scripts-by-type --url-suffix type:PERSON_AUTHENTICATION , /opt/jans/jans-cli/config-cli.py --operation-id get-config-scripts-by-inum --url-suffix inum:6122281b-b55d-4dd0-8115-b098eeeee2b7
```
Update the custom script and change the enabled attribute to `true`
Now Google is an available authentication mechanism for your Janssen Server. This means that, using OpenID Connect acr_values, applications can now request Google authentication for users.

!!! Note To make sure `google` has been enabled successfully, you can check your Janssen's Auth Server OpenID Connect configuration by navigating to the following URL: https://<hostname>/.well-known/openid-configuration. Find "acr_values_supported": and you should see "google".

### Make Sign-in with Google Script as default authentication script:

Use this [link](https://github.com/JanssenProject/jans-cli-tui/blob/vreplace-janssen-version/docs/cli/cli-default-authentication-method.md) as a reference.

Steps:
1. Create a file say `google-auth-default.json` with the following contents
```
{
  "defaultAcr": "google"
}
```
2.Update the default authentication method to Google Sign-in
```
/opt/jans/jans-cli/config-cli.py --operation-id put-acrs --data /tmp/google-auth-default.json
```


:memo: **NOTE**

To make sure `google` has been enabled successfully as a default authentication method, you can check your Gluu Server's OpenID Connect configuration by navigating to the following URL: `https://<hostname>/.well-known/openid-configuration`. Find `"acr_values_supported":` and you should see `"google"`.

## Test the feature - Sign-in with Google
To test , enter the complete URL for authorization in a browser or create a simple webmapage with a link that simulates the user sign-in attempt. If the server is configured properly, the first page for the selected authentication method will be displayed to the user.

An example of a complete URL looks like this -
```
https://<your.jans.server>/jans-auth/authorize.htm?response_type=code&redirect_uri=https://<your.jans.server>/admin&client_id=<replace_with_inum_client_id>&scope=openid+profile+email+user_name&state=faad2cdjfdddjfkdf&nonce=dajdffdfsdcfff
```




 
