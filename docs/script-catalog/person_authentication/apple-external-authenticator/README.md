---
tags:
  - administration
  - recipes
---

## Social Login with Apple

An out-of-the-box feature, the Sign-in with Apple Authentication script is a `PersonAuthenticationType` script which enables a user to sign-in using Apple credentials. After users authenticate using their Apple credentials, their Apple credentials are provisioned into the Jans-auth server.

## Prerequisites

- A Jans-auth Server installation
- The [Sign-in with Apple authentication script](./AppleExternalAuthenticator.py) (included in the default Jans-auth Server distribution);   
- An [Apple developer account](https://developer.apple.com/).     

## Configurations at Apple Identity Provider:
You will need to configure a service id, linked to your App identifier. For each website that uses Sign In with Apple, register a services identifier (Services ID) and configure your domain and return URL.
1. Under `Certificates, Identifiers and Profiles` --> `Identifiers` --> Click on the `+` button
2. In the `Register a new identifier` select `Service ID`
3. After filling out the description and identifier name, save the Service ID
4. Now edit the saved Service ID and enable the `Sign in with Apple` checkbox and click `Configure` button
5. Configure the Janssen's server's callback url `https://<your.janssen.server>/postlogin.htm` as a `Website URL`

## Configure jans-auth server

Configure the custom script:
### Properties

The custom script has the following properties:    

|	Property	|	Description		| Input value     |
|-----------------------|-------------------------------|---------------|
|`apple_client_id`		|Name of Service ID on developer.apple.com. 	| `com.company.name`|
|`apple_jwks`		| Apple’s public JWK to validate Apple Identity Token | `https://appleid.apple.com/auth/keys`|

To update this setting in Jans persistence, follow this [link](https://github.com/JanssenProject/jans-cli-tui/blob/vreplace-janssen-version/docs/cli/cli-custom-scripts.md#update-an-existing-custom-script)

### Enable Sign-in with Apple Authentication script
By default, users will get the default authentication mechanism as specified above. However, using the OpenID Connect acr_values parameter, web and mobile clients can request any enabled authentication mechanism.

Obtain the json contents of `apple` custom script by using a jans-cli command like get-config-scripts-by-type, get-config-scripts-by-inum etc.
```
e.g : /opt/jans/jans-cli/config-cli.py --operation-id get-config-scripts-by-type --url-suffix type:PERSON_AUTHENTICATION , /opt/jans/jans-cli/config-cli.py --operation-id get-config-scripts-by-inum --url-suffix inum:6122281b-b55d-4dd0-8115-b098eeeee2b7
```
Update the custom script and change the enabled attribute to `true`
Now Sign-in with Apple is an available authentication mechanism for your Janssen Server. This means that, using OpenID Connect acr_values, applications can now request Apple authentication for users.

!!! Note To make sure `apple` has been enabled successfully, you can check your Janssen's Auth Server OpenID Connect configuration by navigating to the following URL: https://<hostname>/.well-known/openid-configuration. Find "acr_values_supported": and you should see "apple".

### Make Sign-in with Apple Script as default authentication script:

Use this [link](https://github.com/JanssenProject/jans-cli-tui/blob/vreplace-janssen-version/docs/cli/cli-default-authentication-method.md) as a reference.

Steps:
1. Create a file say `apple-auth-default.json` with the following contents
```
{
  "defaultAcr": "apple"
}
```
2.Update the default authentication method to Apple Sign-in
```
/opt/jans/jans-cli/config-cli.py --operation-id put-acrs --data /tmp/apple-auth-default.json
```


:memo: **NOTE**

To make sure `apple` has been enabled successfully as a default authentication method, you can check your Janssen Server's OpenID Connect configuration by navigating to the following URL: `https://<hostname>/.well-known/openid-configuration`. Find `"acr_values_supported":` and you should see `"apple"`.

## Test the feature - Sign-in with Apple
To test , enter the complete URL for authorization in a browser or create a simple webpage with a link that simulates the user sign-in attempt. If the server is configured properly, the first page for the selected authentication method will be displayed to the user.

An example of a complete URL looks like this -
```
https://<your.jans.server>/jans-auth/authorize.htm?response_type=code&redirect_uri=https://<your.jans.server>/admin&client_id=<replace_with_inum_client_id>&scope=openid+profile+email+user_name&state=faad2cdjfdddjfkdf&nonce=dajdffdfsdcfff
```
