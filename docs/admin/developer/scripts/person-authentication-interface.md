---
tags:
  - administration
  - developer
  - scripts
---



## Person Authentication interface
The **[PersonAuthenticationType](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/type/auth/PersonAuthenticationType.java)** script is described by a java interface whose methods should be overridden to implement an authentication workflow.

### Methods to override:
1. `init(self, customScript,  configurationAttributes)` :  This method is only called once during the script initialization (or jans-auth service restarts). It can be used for global script initialization, initiate objects etc.
     ```
    def init(self, customScript,  configurationAttributes):
        # an example of initializing global variables from script configuration
        if configurationAttributes.containsKey("registration_uri"):
            self.registrationUri = configurationAttributes.get("registration_uri").getValue2()
        return True   
   ```

2. `destroy(self, configurationAttributes)` : This method is called when a custom script fails to initialize or upon jans-auth service restarts. It can be used to free resource and objects created in the init() method
	```
	def destroy(self, configurationAttributes):
	        print "OTP. Destroy"
	        # cleanup code here
	        return True
	```

3. ` authenticate(self, configurationAttributes, requestParameters, step)` : The most important method which will encapsulate the logic for user credential verification / validation 
   ```
    def authenticate(self, configurationAttributes, requestParameters, step):
        authenticationService = CdiUtil.bean(AuthenticationService)

        if (step == 1):
            # 1. obtain user name and password from UI
            # 2. verify if entry exists in database
            # 3. authenticationService.authenticate(user_name, user_password)
            # 4. return True or False

        elif step == 2:
            # 1. obtain credentials from UI
            # 2. validate the credentials
            # 3. return True or False
   ```

     
4. `prepareForStep(self, configurationAttributes, requestParameters, step)` : This method can be used to prepare variables needed to render the UI page and store them in a suitable context. 

	```
	prepareForStep(self, configurationAttributes, requestParameters, step)
	        # if (step == 1):
	            # do this
	        # if (step == 2)
	             # do something else
	        # 2. return True or False
	 ```       

5.    `getExtraParametersForStep` :  Used to save session variables between steps.  The Jans-auth Server persists these variables to support stateless, two-step authentications even in a clustered environment.
		```
	   def getExtraParametersForStep(self, configurationAttributes, step):
		        return Arrays.asList("paramName1", "paramName2", "paramName3")
		```
6. `getCountAuthenticationSteps`: This method normally just returns 1, 2, or 3. In some cases, depending on the context like based on the user's country or department, you can decide to go for multistep or single step authentication.
	  ``` 
	   def getCountAuthenticationSteps(self, configurationAttributes):
	       return 1
	```
7. `getPageForStep`:  Used to specify the UI page you want to show for a given step.
		```
		    def getPageForStep(self, configurationAttributes, step):
		        # Used to specify the page you want to return for a given step
		        if (step == 1):
		          return "/auth/login.xhtml"
		        if (step == 2)
		          return "/auth/enterOTP.xhtml"
	```
8. `getNextStep` : Steps usually go incrementally as 1, 2, 3... unless you specify a case where it can be reset to a previous step, or skip a particular step based on business case.
	```
	   def getNextStep(self, configurationAttributes, requestParameters, step):
	        # steps usually are incremented 1, 2, 3... unless you specify a case where it can be reset to a previous step, or skip a particular step based on 
	        business case.
	        return -1
	```
9.   `getAuthenticationMethodClaims` :   Array of strings that are identifiers for authentication methods used in the authentication. In OpenID Connect, if the identity provider supplies an "amr" claim in the ID Token resulting from a successful authentication, the relying party can inspect the values returned and thereby learn details about how the authentication was performed.
		```
		def getAuthenticationMethodClaims(self, requestParameters):
		        return Arrays.asList("pwd", "otp")
		```        
10.    `getApiVersion` : This value is currently meant to be hardcoded to 11
     
		```
		 def getApiVersion(self):   
		    return 11
		```
11. `isValidAuthenticationMethod` : This method is used to check if the authentication method is in a valid state. For example we can check there if a 3rd party mechanism is available to authenticate users. As a result it should either return True or False.
	```
	    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
	        return True
	```
12. `getAlternativeAuthenticationMethod` : This method is called only if the current authentication method is in an invalid state. Hence authenticator calls it only if isValidAuthenticationMethod returns False. As a result it should return the reserved authentication method name. 
	```
	    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
	        return None
	```

13.  `getLogoutExternalUrl` : Returns the 3rd-party URL that is used to end session routines. The control from this Third party URL should re-direct user back to /oxauth/logout.htm again with empty URL query string. Jans-Auth server will then continue of the extended logout flow, restore the original URL query string, and send user to `/jans-auth/end_session` to complete it.
		```   
		 def getLogoutExternalUrl(self, configurationAttributes, requestParameters):
		     return None
		```
14. `logout` : This method is not mandatory. It can be used in cases when you need to execute specific logout logic in the authentication script when jans-auth receives an end session request to the /oxauth/logout.htm endpoint (which receives the same set of parameters than the usual end_session endpoint). This method should return True or False; when False jans-auth stops processing the end session request workflow.
	```
	def logout(self, configurationAttributes, requestParameters):
	        return True 
	```