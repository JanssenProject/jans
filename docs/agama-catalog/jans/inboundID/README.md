# Inbound identity with Agama

Using Agama administrators can delegate authorization to external services like social sites. In a typical setting users are shown a login form with a "Sign in with..." button for authentication to take place at a given 3rd party in order to get access to a target application.

This process is usually referred to as "inbound identity". In this document the steps required to setup inbound identity in your Janssen server are presented.

## Requisites

- A Janssen server with config-api installed
- Understanding of how to [add and modify Agama flows](https://jans.io/docs/admin/developer/agama/quick-start/#add-the-flow-to-the-server) in your server
- Starter knowledge of [OAuth2](https://www.ietf.org/rfc/rfc6749) and the Java programming language

## Terminology

- Provider: An external identity provider, often a social site. Every provider is associated with a unique identifier

- Provider preselection: A process that designates the provider to employ for authentication without the end-user  making a explicit decision, i.e. without a "Sign in with" button. Here the provider may be inferred from contextual data elicited at earlier stages, for instance.

## Scope

There is a great amount of different mechanisms providers may employ to materialize the authentication process. The current offering in Agama is focused on OAuth2-compliant providers, more specifically those supporting the `code` authorization grant. This does not mean other grants or even different protocols such as SAML cannot be supported, however organizations would have to engage in extra development efforts in this case.

If the providers of interest already support the `code` authorization grant, the amount of work is significantly reduced.

## Flows involved

### Provider

For every provider to support there has to be an Agama flow that must:

- Redirect the browser to the provider's site for the user to enter his login credentials
- Return profile data of the given user

Note the scope here is limited: no session is created in the Janssen server for the user - this occurs later in the [main](#main-flow) parent flow.

To facilitate administrators' work, the following flows are already implemented:

- [Apple](./apple/README.md)
- [Facebook](./facebook)
- [Github](./github)
- [Google](./google)

### Main flow

The actual process of inbound identity occurs here. This flow is already [implemented](https://github.com/JanssenProject/jans/raw/replace-janssen-version/docs/script-catalog/agama/inboundID/io.jans.inbound.ExternalSiteLogin) and ready to use. The following is a summary of the steps involved:

1. A provider selection page is displayed. The list includes all (*enabled*) providers defined in the configuration of this flow. Additionally an option to use an existing local account is displayed (i.e. no inbound identity)

1. The flow associated to the selected provider is launched. Actually, if the main flow was passed a provider identifier as parameter, step 1 is skipped and the given flow is launched directly  

1. If the flow triggered at step 2 did not finish successfully, the main flow finishes likewise. Else, the profile data returned is transformed using the applicable attribute mapping for the provider. More on attribute mappings [here](#attribute-mappings)

1. If the resulting profile data lacks a value for e-mail, and the provider was configured to require it, a form is displayed where the user can enter his e-mail address

1. User provisioning takes place, in other words, a local user entry is created in the Janssen server database. If an entry already exists, it is updated as long as the provider is configured to allow so

1. The flow finishes successfully and the user gets access to the target application

Note the above design does not involve communication protocols. It is responsability of the concrete provider flow how to implement this aspect. The main flow can remain unmodified regardless of any change in existing flows or when new ones are added. 

Feel free to modify this flow or create one based on it if customizations are required.

## Deployment

### Add agama-inbound jar

To start, let's add the required libraries to the authentication server:

- Visit [this](https://maven.jans.io/maven/io/jans/agama-inbound/) page, navigate to the folder matching your Janssen server version, and download the file suffixed with `jar-with-dependencies.jar`
- SSH to the server. Transfer the file to directory `/opt/jans/jetty/jans-auth/custom/libs`
- Navigate to `/opt/jans/jetty/jans-auth/webapps` and edit the file `jans-auth.xml` by adding `<Set name="extraClasspath">./custom/libs/*</Set>` before the root tag closes
- Restart the server, e.g. `systemctl restart jans-auth`

### Add the basic authentication flow

The basic authentication flow is employed when no provider is picked from the list (step 1 [here](#main-flow)) but the option to use an existing local account is taken. This flow is detailed in the Agama sample flows [page](https://jans.io/docs/admin/developer/agama/samples/#basic-authentication), however those contents can be skipped for the purpose of this setup.

- Ensure Agama engine is [enabled](https://jans.io/docs/admin/developer/agama/quick-start/#enable-the-engine). Download the basic flow [source](https://github.com/JanssenProject/jans/raw/replace-janssen-version/docs/admin/developer/agama/basic/io.jans.flow.sample.basic) file

- Use the API for adding flows as explained [here](https://jans.io/docs/admin/developer/agama/quick-start/#getting-an-access-token) and [here](https://jans.io/docs/admin/developer/agama/quick-start/#add-the-flow-to-the-server). A sample `curl` command would look like this: 

    ```
    curl -k -i -H 'Authorization: Bearer <token>' -H 'Content-Type: text/plain'
         --data-binary @io.jans.flow.sample.basic
         https://<your-host>/jans-config-api/api/v1/agama/io.jans.flow.sample.basic
    ```
- In the server, navigate to `/opt/jans/jetty/jans-auth/agama/ftl`. Create the folder hierarchy `samples/basic` there

- Download the login [template](https://github.com/JanssenProject/jans/raw/replace-janssen-version/docs/admin/developer/agama/basic/login.ftlh) to `basic` directory 

### Add the main inbound flow

- Download the flow [source](https://github.com/JanssenProject/jans/raw/replace-janssen-version/docs/script-catalog/agama/inboundID/io.jans.inbound.ExternalSiteLogin) and add it as you did with the basic flow, ensure you use `io.jans.inbound.ExternalSiteLogin` this time

- In the server, navigate to `/opt/jans/jetty/jans-auth/agama`. Create folders named `inboundID` inside existing `ftl` and `fl` subdirectories

- Download the default [logo](https://github.com/JanssenProject/jans/raw/replace-janssen-version/docs/script-catalog/agama/inboundID/none.png) and place it inside `/opt/jans/jetty/jans-auth/agama/fl/inboundID` folder

- Download the provider selector [template](https://github.com/JanssenProject/jans/raw/replace-janssen-version/docs/script-catalog/agama/inboundID/login-chooser.ftlh) and place it inside `/opt/jans/jetty/jans-auth/agama/ftl/inboundID` folder. Note templates go under **ftl**, not **fl**. Do the same with the e-mail prompt [template](https://github.com/JanssenProject/jans/raw/replace-janssen-version/docs/script-catalog/agama/inboundID/email-prompt.ftlh)

### Add a provider flow

For simplicity, we'll illustrate here how to add one of the already implemented demo flows, namely, Facebook. Some guidelines on how to create a provider flow your own are given [here](#creating-a-provider-flow).

- Download the [utility flows](#utility-flows). Add them to the server as you did with the main flow

- Download the Facebook flow [code](https://github.com/JanssenProject/jans/raw/replace-janssen-version/docs/script-catalog/agama/inboundID/facebook/io.jans.inbound.Facebook). Add it using the API as well

- Download the [logo](https://github.com/JanssenProject/jans/raw/replace-janssen-version/docs/script-catalog/agama/inboundID/facebook/facebook.png) image and place it in `/opt/jans/jetty/jans-auth/agama/fl/inboundID`

- Login to Facebook and [register](https://developers.facebook.com/docs/development/register) as developer. Create an application with *Facebook login* capabilities. In the *Facebook login* settings add  `https://<your-host>/jans-auth/fl/callback` as a valid OAuth redirect URI. Finally grab the app Id and secret from the app settings page

#### Set configuration parameters

- Create a JSON file like the below. Replace data in the placeholders appropriately:

    ```
    [{
      "op": "replace",
      "path": "/metadata/properties",
      "value": {
        "authzEndpoint": "https://www.facebook.com/v14.0/dialog/oauth",
        "tokenEndpoint": "https://graph.facebook.com/v14.0/oauth/access_token",
        "userInfoEndpoint": "https://graph.facebook.com/v14.0/me",
        "clientId": "<APP-ID>",
        "clientSecret": "<APP-SECRET>",
        "scopes": ["email", "public_profile"]
      }
    }]
    ```

- Patch the flow using the API. A sample `curl` command would look like this (assuming the JSON file is named `fb.json`):

    ```
    curl -k -i -H 'Authorization: Bearer <token>' -H 'Content-Type: application/json-patch+json'
         -X PATCH -d@fb.json
         https://<your-host>/jans-config-api/api/v1/agama/io.jans.inbound.Facebook
    ```

Later, we'll dive into the meaning of the [configuration parameters](#provider-flow-configurations) set in this JSON file.

### Parameterize the main flow

So far, if the main flow is launched (learn about this topic [here](https://jans.io/docs/admin/developer/agama/quick-start/#craft-an-authentication-request)) a screen with an empty "Sign in with" list will be shown. Adding information about the known providers is required.

- Create a JSON file like the below:

    ```
    [{
      "op": "replace",
      "path": "/metadata/properties",
      "value": {
        "facebook": {
          "flowQname": "io.jans.inbound.Facebook",
          "displayName": "Facebook",
          "mappingClassField": "io.jans.inbound.Mappings.FACEBOOK",
          "logoImg": "facebook.png"
        }
      }
    }]
    ```

- Patch the main flow using the API. A sample `curl` command would look like this (assuming the JSON file is named `main.json`):

    ```
    curl -k -i -H 'Authorization: Bearer <token>' -H 'Content-Type: application/json-patch+json'
         -X PATCH -d@main.json
         https://<your-host>/jans-config-api/api/v1/agama/io.jans.inbound.ExternalSiteLogin
    ```

Later, we'll dive into the meaning of the [configuration parameters](#main-flow-configurations) set in this JSON file.

**Note**: for limitations in the PATCH endpoint, do not use `"op": "add"` to add another provider. Use `"op": "replace"` and pass the whole JSON configuration (all providers) for `value`.

### Test

Launch the main flow (learn about this topic [here](https://jans.io/docs/admin/developer/agama/quick-start/#craft-an-authentication-request)). If everything was setup correctly, a screen with a "Sign in using" heading will be shown and next to it a link titled "Facebook". When clicking on the link, the browser will be taken to the Facebook website for authentication. A prompt for consent of release of personal information may appear as well. Finally, the browser is taken back to your server and then to the target application as described [here](#main-flow). 

## Configuration parameters of flows 
    
### Main flow configurations

Configuration is supplied in a JSON object whose keys are the identifiers of the existing identity providers. The associated value for a key is a JSON object itself and follows the structure represented by [this](https://github.com/JanssenProject/jans/blob/replace-janssen-version/jans-auth-server/agama/inboundID/src/main/java/io/jans/inbound/Provider.java) Java class.

This is an example of a configuration for a couple of identity providers:

```
{

"github": {
  "flowQname": "io.jans.inbound.Github",
  "displayName": "Github",
  "mappingClassField": "io.jans.inbound.Mappings.GITHUB",
},

"google": {
  "flowQname": "io.jans.inbound.Google",
  "displayName": "Google",
  "mappingClassField": "io.jans.inbound.Mappings.GOOGLE",
  "enabled": false,
  "skipProfileUpdate": true
}

}
```

The table below explains the meaning of properties:

|Name|Description|Mandatory|
|-|-|-|
|`flowQname`|The qualified name of the Agama flow associated to this provider|Yes|
|`displayName`|Short name of the provider (will be shown in the selector page)|Yes|
|`mappingClassField`|The qualified name of the [attribute mapping](#attribute-mappings) for this provider|Yes|
|`logoImg`|Relative path to the logo image (will be shown in the selector page)|No|
|`enabled`|A boolean value indicating whether this provider can be shown (and triggered) from the main flow or not. Default value is `true`|No|
|`skipProfileUpdate`|Determines if profile data should not be updated for a user if an entry already exists locally for him. Default value is `false`|No|
|`cumulativeUpdate`|When `true`, existing value(s) of an attribute are preserved when the incoming profile data already contains value(s) for such attribute, otherwise its values are replaced by the incoming ones entirely. Default value is `false`|No|
|`requestForEmail`|Whether to prompt the user to enter his e-mail if the data supplied by the identity provider does not contain one. Default value is `false`|No|
|`emailLinkingSafe`|Determines if an existing account with the same e-mail of the user about to be provisioned can be treated as the same person|No|

**Notes:**

- `logoImg` path is relative to the base path of the main flow, i.e. `inboundID`
- Set `emailLinkingSafe` to true only if you trust the provider, i.e. the incoming e-mail data is securely verified. For security, never set it to `true` when `requestForEmail` is also `true` 
    
### Provider flow configurations

Configurations for this kind of flows don't have to adhere to any specific structure. Developers are free to choose what fits best for their needs. Also note provider flows **must not** receive any inputs: the main flow won't pass any arguments when triggering them. Thus, design your flows so there is no use of `Inputs` but `Configs` directive in the [header](https://jans.io/docs/admin/developer/agama/dsl-full/#header-basics).

In practice many identity providers adhere to the OAuth2 `code` grant, so you can re-use the structure represented by [this](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/agama/inboundID/src/main/java/io/jans/inbound/oauth2/OAuthParams.java) Java class for the purpose. Particularly, the already implemented flows (like Facebook) use it for their configuration.

The table below explains the meaning of its properties:

|Name|Description|
|-|-|
|`authzEndpoint`|The authorization endpoint as in section 3.1 of [RFC 7649](https://www.ietf.org/rfc/rfc6749)| 
|`tokenEndpoint`|The token endpoint as in section 3.2 of [RFC 7649](https://www.ietf.org/rfc/rfc6749)|
|`userInfoEndpoint`|The endpoint where profile data can be retrieved. This is not part of the OAuth2 specification|
|`clientId`|The identifier of the client to use, see section 1.1 and 2.2 of [RFC 7649](https://www.ietf.org/rfc/rfc6749). This client is assumed to be *confidential* as in section 2.1|
|`clientSecret`|Secret associated to the client|
|`scopes`|A JSON array of strings that represent the scopes of the access tokens to retrieve|
|`redirectUri`|Redirect URI as in section 3.1.2 of [RFC 7649](https://www.ietf.org/rfc/rfc6749)|
|`clientCredsInRequestBody`|`true` indicates the client authenticates at the token endpoint by including the credentials in the body of the request, otherwise, HTTP Basic authentication is assumed. See section 2.3.1 of [RFC 7649](https://www.ietf.org/rfc/rfc6749)|
|`custParamsAuthReq`|A JSON object (keys and values expected to be strings) with extra parameters to pass to the authorization endpoint if desired|
|`custParamsTokenReq`|A JSON object (keys and values expected to be strings) with extra parameters to pass to the token endpoint if desired|

Here is an example:

```
{
  "authzEndpoint": "https://www.facebook.com/v14.0/dialog/oauth",
  "tokenEndpoint": "https://graph.facebook.com/v14.0/oauth/access_token",
  "userInfoEndpoint": "https://graph.facebook.com/v14.0/me",
  "clientId": "90210",
  "clientSecret": "changeit",
  "scopes": ["email", "public_profile"]
}
```

### Attribute mappings

This is the process through which the raw user profile data received by an identity provider is transformed into an object suitable for being stored in the Janssen's user database. Here, developers have the opportunity to "map" or "transform" the incoming data to one compatible with the data types, formats, and names required by the database.

As an example suppose a provider returned the following:

```
{
  "id": dfsg2-3bui2.2.5+ld1,
  "email": "moe@doedoe.co",
  "last_name": "Doe",
  "first_name": "Moe"
}
```

None of this attributes exist in Janssen, database adheres to LDAP naming. Conformant names would be `uid`, `mail`, `sn`, and `givenName`. Also, let's assume you want to set `displayName` to a string composed by the first and last names separated by a white space. Writing a mapping is required.

A mapping is implemented in Java in the form of a `java.util.function.UnaryOperator<Map<String, Object>>`, that is, a function that takes a `Map<String, Object>` as input and returns a `Map<String, Object>` as result. Several examples are provided [here](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/agama/inboundID/src/main/java/io/jans/inbound/Mappings.java). 

Note property `mappingClassField` of every provider defined in the [main flow](#main-flow-configurations) points to the fully qualified name of a mapping. Some important considerations:

- A mapping has to be declared as a public field in a public class
- Not all mappings have to belong to the same class
- Several providers can use the same mapping

While working on a mapping, having to pack the class in a jar file, uploading it to the server, and then restarting  every time a modification is made can be a big burden. To avoid this you can upload the source (java) file to the scripts directory of Agama and leverage hot reloading as outlined [here](https://jans.io/docs/admin/developer/agama/java-classpath/). A "template" for quickly start writing a mapping is already [available](https://github.com/JanssenProject/jans/raw/main/jans-auth-server/agama/inboundID/CustomMappings.java.txt). Save with `.java` extension only, edit the body of the lambda expression, upload to the server, and then update the main flow as follows:   

- Add an instruction like `Call io.jans.inbound.CustomMappings#class` at the beginning of the flow body for the class to be effectively reloaded when the file is modified
- Set `mappingClassField` to `io.jans.inbound.CustomMappings.SAMPLE_MAPPING` for the provider of interest. You may like the idea of using a different name for the field - update the java file accordingly

From there onwards, you only need to re-upload the file as many times as needed.

If you use `DEBUG` [logging](https://jans.io/docs/admin/developer/agama/) level in your server, you will see in the log the result of the mapping every time it is applied. Check for a message like "Mapped profile is".

## Utility flows
 
A couple of utility flows are available for developers writing flows:

|Qualified name|Source code|
|-|-|
|`io.jans.inbound.oauth2.AuthzCode`|[link](https://github.com/JanssenProject/jans/raw/main/docs/script-catalog/agama/inboundID/io.jans.inbound.oauth2.AuthzCode)|
|`io.jans.inbound.oauth2.AuthzCodeWithUserInfo`|[link](https://github.com/JanssenProject/jans/raw/main/docs/script-catalog/agama/inboundID/io.jans.inbound.oauth2.AuthzCodeWithUserInfo)|

- Authorization Code flow (`io.jans.inbound.oauth2.AuthzCode`): This flow implements the OAuth 2.0 authorization code grant where client authentication at the token endpoint occurs as described in section 2.3.1 of [RFC 6749](https://www.ietf.org/rfc/rfc6749) (HTTP basic authentication scheme). In summary, this flow redirects the browser to the external provider's site where the user will enter his credentials, then back at the Janssen redirect URL a `code` is obtained which is employed to issue an access token request. The flow returns the token response as received by the provider

- Authorization Code flow with userInfo request (`io.jans.inbound.oauth2.AuthzCodeWithUserInfo`): This flow reuses the previous flow and additionally issues a request to a given userInfo URL passing the access token in the HTTP Authorization header. The response obtained (the profile data of the user) is returned in conjuction with the token response of the authorization code flow

The above means that often, when writing a new flow for a provider, the task boils down to calling the latter flow and returning profile data only.

## Creating a provider flow

A provider flow must fulfil the conditions as summarized [earlier](#provider). Developers have to figure out if the OAuth2 `code` authorization grant is supported, where the task is simplified to get the required [configurations](#provider-flow-configurations). Here, the source code flow of a flow like [Facebook](https://github.com/JanssenProject/jans/raw/main/docs/script-catalog/agama/inboundID/facebook/io.jans.inbound.Facebook) can be re-used - probably without modification other than in the header.
