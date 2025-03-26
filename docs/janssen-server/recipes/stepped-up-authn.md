---
tags:
  - administration
  - recipes
  - stepped-up authentication
  - Update token
  - modify access token
  - scope
---

# Stepped-up Authentication

### What is Stepped Up Authentication ? 
While navigating through an application, a user is challenged to produce an additional authentication when a certain API (of higher criticality) accessed by the client, does not have the needed scope.

Consider the following sequence of events :
1. A user is logged in to an app using very basic authentication mechanism. Say login- password.
2. The user navigates through the app.
3. When the user attempts to access a critical resource, he is presented with another authentication step say otp.
4. The user, after successful authentication, has the needed access token to access the critical resource.
 
```mermaid

sequenceDiagram

title Stepped-up Authentication

actor Person
participant Browser
participant Website
participant Auth Server
participant API

autonumber

Website->>API: request some endpoint
API->>API:  Enforce<br> presence of <br> 'otp' scope<br> in  access_token
API->>Website: 3. 401 Unauthorized<br>WWW-Authenticate: Bearer error="insufficient_user_authentication" 
Website->>Browser: Enforce additional challenge on the user<br> (Enter OTP) 
Person->>Browser:Enter OTP
Browser->>Auth Server: /authorize endpoint
Note right of Auth Server: Stepped-up Authentication
Auth Server->>Auth Server:Validate OTP (Person authentication script)
Note right of Auth Server: Modify scope of AT
Auth Server->>Auth Server: Modify scope of AT to include OTP (Update token script)
Auth Server->> Website: Return Access Token with scope containing OTP

```

## Implementation details : 
This implementation has been broken down to 5 parts highlighted in different colours. The details contain sample code that can be used to be build the flow using `Person Authentication Scripts` and `Update Token Scripts`
```mermaid

sequenceDiagram
title Implementation Stepped-up authn in Janssen's Authentication server

actor Person
participant Browser
participant Website
participant Auth Server
participant API
autonumber
rect rgb(255, 223, 211)
Website->>Browser: html login form
Person->>Browser: Update PII
Browser->>Website: POST 
Website->>Auth Server: login request /authorize?response_type=code id_token
Auth Server->> Auth Server: UpdateTokenScript - place user info in id_token <br>e.g. encrypted inum
Auth Server ->>Website: return id_token and code
end
rect rgb(211,211,211)
Website->> Website: Extract the user info from id_token and save in website's session (not to be confused with Auth Server session)
end

rect rgb(212, 238, 227)
Website->>API: request some endpoint
API->>API:  Enforce<br> presence of <br> 'otp' scope<br> in  access_token
API->>Website: 401 Unauthorized<br>WWW-Authenticate: <br>Bearer error="insufficient_user_authentication" 
Website->>Browser:  redirect to AS
end


rect rgb(186,225,255)
Browser->>Auth Server: authorize?acr_values=otp<br>&client_id=1234<br>&scope=otp + other scopes<br>&response_type=code<br>&client_id=____<br>&redirect_uri=____<br>&state=_____<br>&nonce=____<br>&prompt=login&<br>login_hint=encrypted_inum
Auth Server->>Auth Server:  Get encrypted user inum,<br>from login_hint, and decrypt<br>return None, if inum not found<br> 
Auth Server->>Browser:  Display OTP Page
Person->>Browser:  enter OTP
Browser->>Auth Server: POST OTP form
Auth Server->>Auth Server: validate OTP
Auth Server->>Auth Server: add session variable scope_enforce= otp
Auth Server->>Browser:  redirect to callback
Browser->>Website:  callback URI
Website->>Auth Server: /token + client creds
Auth Server->>Auth Server:  Update token script is invoked. <br> read session variable scope-enforce <br> update scope of AT to include otp
Auth Server->>Website: access_token, id_token,<br>refresh_token
end 

rect rgb(255,255,186)
Website->>API:  request some endpoint<br>(with new access_token)
API->>API:  verify the acess_token. It should contain the necessary scope i.e otp
end

```

### Step A: Ensure id_token has some info to identify the user:

1. When the user logs in for the very first time and performs basic authentication, the Update Token script can be used to place a custom claim containing some user information like
- User permissions
- User personal information 
- Internal user identifier.  
In this example, we store the encrypted inum of the user as a "custom claim" in the id_token. 
2. A good practice is to not put the primary key / user identifier in plain text

UpdateToken script:
```
   def modifyIdToken(self, jsonWebResponse, context):
        print "Update token script. Modify idToken: %s" % jsonWebResponse
        sessionIdService = CdiUtil.bean(SessionIdService)
        session = sessionIdService.getSessionByDn(context.getGrant().getSessionDn()) # fetch from persistence
        userService = CdiUtil.bean(UserService)
        user_name = session.getSessionAttributes().get("auth_user")                
        foundUser = userService.getUserByAttribute("uid", user_name)        
        userInum = foundUser.getAttribute("inum") 
        
        encryptedInum = CdiUtil.bean(EncryptionService).encrypt(userInum)
        print encryptedInum 
        
        #custom claim in id_token
        jsonWebResponse.getClaims().setClaim("someFancyName", encryptedInum)
        
        return True
```
### Step B: Website (RP) extracts the user info from id_token:
1. The Website (RP / client) should extract the user info from id_token and save it in the website's session.
2. The website's session should not be confused with the Auth Server's session. 
3. The Auth Server sessions are state-less and any session related information that the website wishes to consume can only be that which can be extracted from an id_token.

### Step C: Critical Resource accessed by user.
1. Website detects critical resource, examines the Access token presented by the client.
2. Redirects to RP for stepped-up authentication

### Step D: Stepped-up authentication
1. Website calls /authorize endpoint with login_hint=encrypted_inum, which was previously extracted from id_token in Step B
1. Use `login_hint`, `id_token_hint` or `request`-jwe to identify the user in Person Authentication script used to perform the additional authentication step.

Stepped-up Authn Person Authentication script:
```
 def prepareForStep(self, configurationAttributes, requestParameters, step):
        print "OTP test. Prepare for steps %s" %step
        if (step ==1):
            try:
                userService = CdiUtil.bean(UserService)
                
                encryptedInum  = ServerUtil.getFirstValue(requestParameters, "login_hint")

                identity = CdiUtil.bean(Identity)
                print encryptedInum  
                
                inum = CdiUtil.bean(EncryptionService).decrypt(encryptedInum)
                foundUser = userService.getUserByInum(inum )
                print  foundUser
                username = foundUser.getUserId()
                print "username is %s " % username
                identity.setWorkingParameter("uusername", username )
                return True
            except:
                print "OTP. Exception: '%s'" % (sys.exc_info()[1])
                return False

        else:
            return False
```
3. Validate user otp
4. "enforce_scope=otp", add to session

```
def authenticate(self, configurationAttributes, requestParameters, step):
        print("OTP, authentication for step %s" %step)
        authenticationService = CdiUtil.bean(AuthenticationService)
        identity = CdiUtil.bean(Identity)
        userService = CdiUtil.bean(UserService)
        username =  identity.getWorkingParameter("username")
        print username 

        #Here we set hard coded otp
        otp = '12345'           
        inputOtp = ServerUtil.getFirstValue(requestParameters, "loginForm:otpCode")
        print("Client end otp %s " %inputOtp)
        print("Server end otp %s" %otp)
        if otp == inputOtp:            
            print "OTP Authenticated"
            print CdiUtil.bean(SessionIdService).getSessionId()
            authenticationService.authenticate('username')
            # adding to session
            identity.setWorkingParameter("enforce_scope","otp")
            return True
        else:
            print("Wrong otp")
            return errorMessage("Wrong otp entered")
            return False

```

5: Update AT with relevant scope
* Check the session variable "enforce_scope" and add the "scope" to the Access token. 
* You can also modify Access token header claims and regular claims using the `modifyAccessToken` in the Update Token Script.
Update Token script:
```
    def modifyAccessToken(self, accessToken, context):

        #read from session
	sessionIdService = CdiUtil.bean(SessionIdService)
	sessionId = sessionIdService.getSessionByDn(context.getGrant().getSessionDn()) # fetch from persistence

        enforce_scope = sessionId.getSessionAttributes().get("enforce_scope ")
	if enforce_scope not None:
               context.overwriteAccessTokenScopes(accessToken, Sets.newHashSet("existingScope1", "existingScope2", "mynewscope"))

        context.getHeader().setClaim("custom_header_name", "custom_header_value")
        context.getClaims().setClaim("claim_name", "claimValue")
```
### Step E: Access the critical resource with new Access token:
The access token contains needed scope because the scopes were updated in Step D,  point 5


## Update token script

```
from io.jans.as.server.service import AuthenticationService
from io.jans.as.server.service import UserService
from io.jans.as.server.service import SessionIdService
from io.jans.as.server.util import ServerUtil
from io.jans.service.cdi.util import CdiUtil
from io.jans.as.service.common import EncryptionService
from io.jans.model.custom.script.type.token import UpdateTokenType


class UpdateToken(UpdateTokenType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Update token script. Initializing ..."
        print "Update token script. Initialized successfully"

        return True

    def destroy(self, configurationAttributes):
        print "Update token script. Destroying ..."
        print "Update token script. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 11

    
    def modifyIdToken(self, jsonWebResponse, context):
        print "Update token script. Modify idToken: %s" % jsonWebResponse
        sessionIdService = CdiUtil.bean(SessionIdService)
        session = sessionIdService.getSessionByDn(context.getGrant().getSessionDn()) # fetch from persistence
        userService = CdiUtil.bean(UserService)
        user_name = session.getSessionAttributes().get("auth_user")                
        foundUser = userService.getUserByAttribute("uid", user_name)        
        userInum = foundUser.getAttribute("inum") 
        print userInum
        encryptedInum = CdiUtil.bean(EncryptionService).encrypt(userInum)
        print encryptedInum 
        #custom claim in id_token
        jsonWebResponse.getClaims().setClaim("someFancyName", encryptedInum)
        #print "Update token script. After modify idToken: %s" % jsonWebResponse
        #jsonWebResponse.getClaims().setClaim("someFancyName", "madhu")
        return True

    def modifyAccessToken(self, accessToken, context):

        #read from session
        sessionIdService = CdiUtil.bean(SessionIdService)
        sessionId = sessionIdService.getSessionByDn(context.getGrant().getSessionDn()) # fetch from persistence

        enforce_scope = sessionId.getSessionAttributes().get("enforce_scope")
        if enforce_scope is not  None:
            context.overwriteAccessTokenScopes(accessToken, Sets.newHashSet("openid", "profile", "otp"))
        
        return True
            
    def modifyRefreshToken(self, refreshToken, context):
        return True

    def getRefreshTokenLifetimeInSeconds(self, context):
        return 0

    def getIdTokenLifetimeInSeconds(self, context):
        return 0

    def getAccessTokenLifetimeInSeconds(self, context):
        return 0
```

### OTP script

```
from io.jans.as.server.util import ServerUtil
from io.jans.service.cdi.util import CdiUtil
from io.jans.as.server.security import Identity
from io.jans.model.custom.script.type.auth import PersonAuthenticationType
from io.jans.as.server.service import AuthenticationService
from io.jans.as.server.service import UserService
from io.jans.as.server.service import SessionIdService
from org.jans.as.server.service import CookieService
from javax.faces.application import FacesMessage
from org.gluu.jsf2.message import FacesMessages
from org.gluu.jsf2.service import FacesService
from io.jans.util import StringHelper
from java.util import Arrays

def errorMessage(errorInfo):
    facesMessages = CdiUtil.bean(FacesMessages)

    facesMessages.setKeepMessages()
    facesMessages.clear()
    facesMessages.add(FacesMessage.SEVERITY_ERROR, errorInfo)
    return False

class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript,  configurationAttributes):
        print "OTP test. Initialization"
        self.auth_user = None
        self.isUserAuthenticated = False
        self.isValidOtp = False
        print "OTP test. Initialized successfully"
        return True   

    def destroy(self, configurationAttributes):
        print "OTP test. Destroy"
        print "OTP test. Destroyed successfully"
        return True

    def getAuthenticationMethodClaims(self, requestParameters):
        return None

    def getApiVersion(self):
        return 11

    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        return True

    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        return None

    def authenticate(self, configurationAttributes, requestParameters, step):
        print("OTP test, authentication for step %s" %step)
        authenticationService = CdiUtil.bean(AuthenticationService)
        identity = CdiUtil.bean(Identity)
        userService = CdiUtil.bean(UserService)
        username =  identity.getWorkingParameter("uusername")
        print "username is %s " , username 
        if(username  is not None):
            userAuthenticated = authenticationService.authenticate(username)
            print("User present in session")

            if(not userAuthenticated):
                print ("Not authenticated")
                return False        

        #Here we set hard coded otp
        otp = '12345'           
        inputOtp = ServerUtil.getFirstValue(requestParameters, "loginForm:otpCode")
        print("Client end otp %s " %inputOtp)
        print("Server end otp %s" %otp)
        if otp == inputOtp:            
            print "OTP Authenticated"
            print CdiUtil.bean(SessionIdService).getSessionId()
            authenticationService.authenticate('username')
            identity.setWorkingParameter("enforce_scope","otp")
            return True
        else:
            print("Wrong otp")
            return errorMessage("Wrong otp entered")
            return False



    def prepareForStep(self, configurationAttributes, requestParameters, step):
        print "OTP test. Prepare for steps %s" %step
        if (step ==1):
            try:
                userService = CdiUtil.bean(UserService)
                
                encryptedInum  = ServerUtil.getFirstValue(requestParameters, "login_hint")

                identity = CdiUtil.bean(Identity)
                print encryptedInum  
                
                inum = CdiUtil.bean(EncryptionService).decrypt(encryptedInum)
                foundUser = userService.getUserByInum(inum )
                print  foundUser
                username = foundUser.getUserId()
                print "username is %s " % username
                identity.setWorkingParameter("uusername", username )
                return True
            except:
                print "OTP. Exception: '%s'" % (sys.exc_info()[1])
                return False

        else:
            return False

    def getExtraParametersForStep(self, configurationAttributes, step):
        return Arrays.asList("uusername", "enforce_scope")
    def getCountAuthenticationSteps(self, configurationAttributes):
        return 1

    def getPageForStep(self, configurationAttributes, step):
        print("Get page for step %s" %step)
        if( step==1 ) :

                return "/auth/otp/otplogin.xhtml"

        else:
            return ""

    def getNextStep(self, configurationAttributes, requestParameters, step):
        return -1

    def getLogoutExternalUrl(self, configurationAttributes, requestParameters):
        print "Get external logout URL call"
        return None

    def logout(self, configurationAttributes, requestParameters):
        print"logout"
        return True

    
```

