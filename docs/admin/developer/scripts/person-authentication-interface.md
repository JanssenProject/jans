---
tags:
  - administration
  - developer
  - scripts
---

## Person Authentication interface
The **[PersonAuthenticationType](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/type/auth/PersonAuthenticationType.java)** script is described by a java interface whose methods should be overridden to implement an authentication workflow.

### Inherited Methods
| Method header | Method description |
|:-----|:------|
| `def init(self, customScript, configurationAttributes)` | This method is only called once during the script initialization. It can be used for global script initialization, initiate objects etc |
| `def destroy(self, configurationAttributes)` | This method is called once to destroy events. It can be used to free resource and objects created in the `init()` method |
| `def getApiVersion(self, configurationAttributes, customScript)` | The getApiVersion method allows API changes in order to do transparent migration from an old script to a new API. Only include the customScript variable if the value for getApiVersion is greater than 10 |

#### Objects
| Object name | Object description |
|:-----|:------|
|`customScript`| The custom script object. [Reference](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/model/CustomScript.java) |
|`configurationAttributes`| `configurationProperties` passed in when adding custom script. `Map<String, SimpleCustomProperty> configurationAttributes` |
|`SimpleCustomProperty`| Map of configuration properties. [Reference](https://github.com/JanssenProject/jans/blob/main/jans-core/util/src/main/java/io/jans/model/SimpleCustomProperty.java) |

Pseudo code:
```
    def init(self, customScript,  configurationAttributes):
        # an example of initializing global variables from script configuration
        if configurationAttributes.containsKey("registration_uri"):
            self.registrationUri = configurationAttributes.get("registration_uri").getValue2()
        return True   
```

```
	def destroy(self, configurationAttributes):
	        print "ACR_NAME. Destroy"
	        # cleanup code here
	        return True
```

```
  	 def getApiVersion(self):   
   		    return 11
```

### Person Authentication interface Methods
| | Method header | Method description |
:-----|:-----|:------|
|1.|`prepareForStep(self, configurationAttributes, requestParameters, step)` | This method can be used to prepare variables needed to render the UI page and store them in a suitable context.|
|2.|` authenticate(self, configurationAttributes, requestParameters, step)` | The most important method which will encapsulate the logic for user credential verification / validation|
|3.|  `getExtraParametersForStep` |  Used to save session variables between steps.  The Jans-auth Server persists these variables to support stateless, two-step authentications even in a clustered environment.|
|4.|`getCountAuthenticationSteps`| This method normally just returns 1, 2, or 3. In some cases, depending on the context like based on the user's country or department, you can decide to go for multistep or single step authentication.|
|5.| `getPageForStep`|  Used to specify the UI page you want to show for a given step.|
|6.| `getNextStep` | Steps usually go incrementally as 1, 2, 3... unless you specify a case where it can be reset to a previous step, or skip a particular step based on business case.|
|7.|   `getAuthenticationMethodClaims` |   Array of strings that are identifiers for authentication methods used in the authentication. In OpenID Connect, if the identity provider supplies an "amr" claim in the ID Token resulting from a successful authentication, the relying party can inspect the values returned and thereby learn details about how the authentication was performed.|
|8.| `isValidAuthenticationMethod`| This method is used to check if the authentication method is in a valid state. For example we can check there if a 3rd party mechanism is available to authenticate users. As a result it should either return True or False.|
|9.| `getAlternativeAuthenticationMethod` | This method is called only if the current authentication method is in an invalid state. Hence authenticator calls it only if isValidAuthenticationMethod returns False. As a result it should return the reserved authentication method name.|
|10.  `getLogoutExternalUrl` | Returns the 3rd-party URL that is used to end session routines. The control from this Third party URL should re-direct user back to /oxauth/logout.htm again with empty URL query string. Jans-Auth server will then continue of the extended logout flow, restore the original URL query string, and send user to `/jans-auth/end_session` to complete it.|
|11. `logout` | This method is not mandatory. It can be used in cases when you need to execute specific logout logic in the authentication script when jans-auth receives an end session request to the /oxauth/logout.htm endpoint (which receives the same set of parameters than the usual end_session endpoint). This method should return True or False; when False jans-auth stops processing the end session request workflow.|

#### Objects
| Object name | Object description |
|:-----|:------|
|`configurationAttributes`| `configurationProperties` passed in when adding custom script. `Map<String, SimpleCustomProperty> configurationAttributes` |
|`SimpleCustomProperty`| Map of configuration properties. [Reference](https://github.com/JanssenProject/jans/blob/main/jans-core/util/src/main/java/io/jans/model/SimpleCustomProperty.java) |
|usageType|[AuthenticationScriptUsageType](https://github.com/JanssenProject/jans/blob/main/jans-core/util/src/main/java/io/jans/model/AuthenticationScriptUsageType.java)|
|step|Integer indicating step number|
|step|Integer indicating step number|
|requestParameters|Request parameters stored as Map<String, String[]>|


Pseudocode:
```
def prepareForStep(self, configurationAttributes, requestParameters, step):
    if (step == 1):
        # do this
    if (step == 2):
        # do something else
    return True # or False

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

def getExtraParametersForStep(self, configurationAttributes, step):
	   return Arrays.asList("paramName1", "paramName2", "paramName3")

def getCountAuthenticationSteps(self, configurationAttributes):
	    return 1

def getPageForStep(self, configurationAttributes, step):
    # Used to specify the page you want to return for a given step
    if (step == 1):
        return "/auth/login.xhtml"
    if (step == 2)
        return "/auth/enterOTP.xhtml"

 def getNextStep(self, configurationAttributes, requestParameters, step):
# steps usually are incremented 1, 2, 3... unless you specify a case where it can be reset to a previous step, or skip a particular step based on business case.
    return -1

 def getAuthenticationMethodClaims(self, requestParameters):
    return Arrays.asList("pwd", "otp")

 def isValidAuthenticationMethod(self, usageType, configurationAttributes):
    return True

 def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
    return None

 def getLogoutExternalUrl(self, configurationAttributes, requestParameters):
     return None

 def logout(self, configurationAttributes, requestParameters):
    return True

```
### Example scripts:

1. [Basic script](https://github.com/JanssenProject/jans/tree/main/docs/script-catalog/person_authentication/basic-external-authenticator)

2. [Script catalog](https://github.com/JanssenProject/jans/tree/main/docs/script-catalog)
