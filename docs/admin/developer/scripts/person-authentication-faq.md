---
tags:
  - administration
  - developer
  - scripts
  - acr_values_supported
  - 2FA
  - PersonAuthenticationType
  - acr
  - weld
  - error messsages
  - redirection
---


### 1. Display error messages on a web page?
1.[FacesMessages](https://github.com/JanssenProject/jans/blob/replace-janssen-version/jans-core/jsf-util/src/main/java/io/jans/jsf2/message/FacesMessages.java) bean is used for this purpose.
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
	See an example [here](https://github.com/JanssenProject/jans/blob/replace-janssen-version/jans-auth-server/server/src/main/webapp/WEB-INF/incl/layout/template.xhtml#L41)

### 2. Redirection to a third party application for authentication

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

3. 
