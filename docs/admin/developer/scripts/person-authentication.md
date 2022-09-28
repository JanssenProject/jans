---
tags:
  - administration
  - developer
  - scripts
---

# Person Authentication scripts
The Jans-Auth Server leverages interception scripts of [PersonAuthenticationType](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/type/auth/PersonAuthenticationType.java) which when implemented can facilitate complex multi-step, multi-factor authentication workflows. The authentication flow in the Jans Server is driven by the openID spec. The authorization request to the OP (Jans server) contains an optional query parameter called `acr_values` which is used by the OP to pick an interception script which will be run when `/authorize` endpoint (Authentication flow) is invoked. The name of each script corresponds with its `acr` value in the Jans-Auth Server.

Typically, a `PersonAuthenticationType` script can be used to:  
 1. introduce a new 2FA authentication mechanism  
 2. customise multistep authentication  
 3. offer Social logins  
 4. proactively perform fraud detection and block the user.

## Default authentication method: 
In an OpenID Connect authentication request, one of the optional parameters defined is `acr_values`. This is the primary way for a client to signal to the OpenID Provider (OP) the preferred way to authenticate the subject. 
A client may also specify `default_acr_values` during registration (and omit the parameter while making an authentication request). **In the Jansssen Server configuration, `acr` is used to name the authentication workflow.**

`default_acr`: This is the default authentication mechanism exposed to all applications that send users to the Janssen Server for sign-in. Unless an app specifically requests a different form of authentication using the OpenID Connect acr_values parameter (as specified below), users will receive the form of authentication specified in this field.

If a default ACR is not specified, Janssen will determine it based on enabled scripts and the internal user/password ACR. This internal ACR, `simple_password_auth`, is set to level -1. This means that it has lower priority than any scripts, so Janssen server will use it only if no other authentication method is set.

Use the jans-cli to [update / look-up the default authentication method](https://github.com/JanssenProject/jans-cli/edit/main/docs/cli/cli-default-authentication-method.md).

## Multiple Authentication Mechanisms
The Jans Server can concurrently support multiple authentication mechanisms, enabling Web and mobile apps (clients) to request a specific type of authentication using the standard OpenID Connect request parameter: acr_value.

### Enabling an authentication mechanism
By default, users will get the default authentication mechanism as specified above. However, **using the OpenID Connect acr_values parameter, web and mobile clients can request any enabled authentication mechanism**.

1. Obtain the json contents of a custom script by using a jans-cli command like `get-config-scripts-by-type`, `get-config-scripts-by-inum` etc. 
	Example : 
	 - `/opt/jans/jans-cli/config-cli.py --operation-id get-config-scripts-by-type --url-suffix type:PERSON_AUTHENTICATION`
	 - `/opt/jans/jans-cli/config-cli.py --operation-id get-config-scripts-by-inum --url-suffix inum:6122281b-b55d-4dd0-8115-b098eeeee2b7`

2. [Update the custom script](https://github.com/JanssenProject/jans-cli/blob/main/docs/cli/cli-custom-scripts.md#update-an-existing-custom-script) and change the `enabled` attribute to `true`  

### Level (rank) of an Authentication mechanism : 
Each authentication mechanism has a "Level" assigned to it which describes how secure and strict it is. **The higher the "Level", the more reliable mechanism represented by the script is.** Though several mechanisms can be enabled at the same Janssen server instance at the same time, for any specific user's session only one of them can be set as the current one (and will be returned as `acr` claim of id_token for them). If after initial session is created a new authorization request from a RP comes in specifying another authentication method, its "Level" will be compared to that of the method currently associated with this session. If requested method's "Level" is lower or equal to it, nothing is changed and the usual SSO behavior is observed. If it's higher (i.e. a more secure method is requested), it's not possible to serve such request using the existing session's context, and user must re-authenticate themselves to continue. If they succeed, a new session becomes associated with that requested mechanism instead.

Enabled scripts can be confirmed by checking  the Janssen OP configuration URL, `https://<hostname>/.well-known/openid-configuration`, and finding the "**acr_values_supported**".

Learn more about acr_values in the [OpenID Connect core spec](http://openid.net/specs/openid-connect-core-1_0.html#acrSemantics).


## Building blocks of an authentication workflow
A. Custom script
B. UI pages, stylesheets, javascript files, image files and resource bundles
C. Managed Beans
D. Third party libraries.

Jans-auth comprises of a number of beans, configuration files and Facelets (JSF) views, packaged as a WAR module. That means custom scripts and custom pages (JSF facelets) can make use of business logic already encapsulated in the Weld managed beans. The following sections explain how authentication flow can be built using a custom script. 

## Writing a custom script
The **PersonAuthenticationType** script is described by a java interface whose methods should be overridden to implement an authentication workflow.
The [article](https://jans.io/docs/admin/developer/scripts/person-authentication-interface) talks about these methods in detail and the psuedo code in each method will help drive home the point.
 
## Writing UI pages:
All pages are **xhtml** files. The Jans-auth server comes with a default set of pages for login, logout, errors, authorizations. You can easily override these pages or write new ones. You can easily apply your own stylesheet, images and resouce-bundles to your pages. 

This [article](https://github.com/maduvena/jans-docs/wiki/Writing-UI-pages) covers all the details you need to write your own web page. 

## Building business logic in Custom script:  
Jans-auth server uses Weld 3.0 (JSR-365 aka CDI 2.0) for managed beans. The most important aspects of business logic are implemented through a set of beans. Details and examples of this can be found in this [article](../developer/managed-beans)

## Adding libraries for use in the custom script
Java or Python libraries to be imported and used very easily. Remember incase you opt for a python library, it should be written in "pure python" only.
More details of this mentioned [here](../interception-scripts/#using-python-libraries-in-a-script)

## Uses of Person Authentication script

### A. Implementing 2FA authentication mechanisms
1. [FIDO2](/../../../script-catalog/person_authentication/fido2-external-authenticator/README) : Authentications using platform authenticators embedded into a person's device or physical USB, NFC or Bluetooth security keys that are inserted into a USB slot of a computer
2. SMS OTP :  
3. Email OTP 

### B. Implementing Multistep authentication
1. [Redirect to previous step](https://github.com/JanssenProject/jans/blob/main/jans-linux-setup/jans_setup/static/extension/person_authentication/other/basic.reset_to_step/BasicResetToStepExternalAuthenticator.py): The script here an example of how the number of steps can be varied depending on the context or business requirement. 

### C. Implementing Social logins 
You can use a `PersonAuthenticationType` script to allow users to sign using credentials from popular **Social Identity providers** or **Inbound Identity Providers** like Facebook, Google and Apple. After users authenticate, thier Social Identity Provider credentials are provisioned into the Jans-auth server. More on this topic in this [article](../recipes/social-login/)

### D. Proactively perform fraud detection
1. Impossible travel

## Testing an authentication flow

An example of a complete URL looks like this - 
		```
		
		https://<your.jans.server>/jans-auth/authorize.htm? \
		response_type=code&redirect_uri=https://<your.jans.server>/admin \
		&client_id=17b8b82e-b3ec-42a2-bd90-097028a37f3 \
		&scope=openid+profile+email+user_name \
		&state=faad2cdjfdddjfkdf&nonce=dajdffdfsdcfff
	```
To test , enter the complete URL for authorization in a browser or create a simple webmapage with a link that simulates the user sign-in attempt. If the server is configured properly, the first page for the selected authentication method will be displayed to the user.

## FAQs

### 1. How can error messages be displayed on a web page?
1. FacesMessage bean is used for this purpose. 
	```
	from org.jans.jsf2.message import FacesMessages
	from org.jans.service.cdi.util import CdiUtil
	from javax.faces.application import FacesMessage
	...

	facesMessages = CdiUtil.bean(FacesMessages) 
	facesMessages.setKeepMessages()
	facesMessages.add(FacesMessage.SEVERITY_ERROR, "Please enter a valid username")

	```
2. The error will appear in the associated template using the following markup:
	```
	...
	<h:messages />
	...
	```
	See an example [here](https://github.com/JanssenProject/jans/blob/685a1593fb53e2310cfa38fcd49db94f3453042f/jans-auth-server/server/src/main/webapp/WEB-INF/incl/layout/template.xhtml#L41)

### 2. How is redirection to a third party application for authentication handled in a script? 

For user authentication or consent gathering, there might be a need to redirect to a third party application to perform some operation and return the control back to authentication steps of the custom script. Please apply these steps to a person authentication script in such a scenario:

 - Return from def getPageForStep(self, step, context), a page /auth/method_name/redirect.html ; with content similar to the code snippet below -
	```
    def getPageForStep(self, step, context):
        return "/auth/method_name/redirect.html"
	```
 - Contents of redirect.xhtml should take the flow to prepareForStep method
	```
	...
	    <f:metadata>
	        <f:viewAction action="#{authenticator.prepareForStep}" if="#{not identity.loggedIn}" />
	    </f:metadata>
	```
 - In method prepareForStep prepare data needed for redirect and perform the redirection to the external service.
	```
	def prepareForStep(self, step, context):
	        .....
	    facesService = CdiUtil.bean(FacesService)
	    facesService.redirectToExternalURL(third_party_URL )

	    return True
	```
 - In order to resume flow after the redirection, invoke a similar URL to https://my.gluu.server/postlogin.htm?param=123 from the third party app which takes the flow back to the authenticate method of the custom script.
So create an xhtml page postlogin.xhtml which will look like this :
	```
	<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	   "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

	<html xmlns="http://www.w3.org/1999/xhtml"
	      xmlns:f="http://xmlns.jcp.org/jsf/core">

	<f:view transient="true" contentType="text/html">
	    <f:metadata>
	        <f:viewAction action="#{authenticator.authenticateWithOutcome}" />
	    </f:metadata>
	</f:view>

	</html>
	```
The `<f:viewAction action="#{authenticator.authenticateWithOutcome}" />` in step 4 takes us to the authenticate method inside the custom script `def authenticate(self, configurationAttributes, requestParameters, step):`. Here you can 
 - use parameters from request
	```
	param = ServerUtil.getFirstValue(requestParameters, "param-name")) 
	``` 
 - perform the `state` check (state : Opaque value used to maintain state between the request and the callback.)

 - finally, return true or false from this method.
