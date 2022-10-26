---
tags:
  - administration
  - auth-server
  - endpoint
---


## Backchannel authentication scripts
Janssen server enables domains to render the username/pw login form, avoiding the redirect to the IDP-hosted login page. Many SSO solutions offer a proprietary endpoint to accomplish this. However, in Janssen server, this is accomplished with the OAuth/OpenID framework by using a combination of calling the /token endpoint using the OAuth password flow, and then redirecting to browser to the /authorization endpoint, but auto-submitting the form.

This article describes how to process login via backchannel and when browser is processing authorization, AS will recognize automatically the authenticated user, write cookies and then establish the session.

## Design
![image](https://user-images.githubusercontent.com/86965029/174393605-8bcc1033-9bc3-4254-ad49-23a255649f33.png)

Source-code for sequence diagram
```
Title password Grant with State

Person->Browser: Navigate to website
Browser->Website:
Website->Browser:Display login page
Person->Browser: Enter Username / PW
Browser->Website: (creds)
Website->IDP: /token?uid=_&pw=&browser_ip=_
IDP->Website: {"redirect": "/authz/state=1F2D41A", ...}
Website->Browser:
Browser->IDP: /authz?state=1F2D41A
IDP->IDP: check IP address is\n same as state
IDP->Browser: write cookie / send redirect_uri
Browser->Website: redirect_uri
```

## Solution
In order to process the whole authorization, idea is to use resource owner password credentials grant type in the first stage, over there process the authentication as we do today, but also write in cache a short-lived token to recognize such authenticated user, then return to the client such short-lived token. During authorization, browser should send that token as part of the custom params and in the AS we should verify if that user is already authenticated using the short-lived param in the cache, then AS will write cookies, create the session and return to the client information required for this.

## Jans App Configurations
Above the **jans-auth** record of the **jansAppConf** table, enable a new attribute on the **jansConfDyn** field

Example:
```
"authorizationRequestCustomAllowedParameters": [
    {
        "paramName": "bcAuthnToken",
        "returnInResponse": true
    }
]
```
**Sample CURL**
1. Obtain access token
   ```
   curl -u "put_client_id_here:put_config_api_client_secret_here" https://<your.jans.server>/jans-auth/restv1/token \
      -d  "grant_type=client_credentials&scope=https://jans.io/oauth/jans-auth-server/config/properties.write"
   ```
2. Apply patch
   ```
   curl -X PATCH -k -H 'Content-Type: application/json-patch+json' \ 
      -i 'jans-config-api/api/v1/jans-auth-server/config' \ 
      -H "Authorization: Bearer put_access_token_here" --data '[
         {
          "op": "add",
          "path": "authorizationRequestCustomAllowedParameters",
          "value": [
                       {
                        "paramName": "bcAuthnToken",
                        "returnInResponse": true
                       }
                   ]
          }
         ]'
   ```

## Custom Script Registration
In the table **jansCustomScr** enable 2 new scripts:
1. Script type **resource_owner_password_credentials** displayName **ropc-backchannel** [ropc-backchannel-script.py] See script below.
2. Script type **person_autentication** displayName **backchannel-authentication** [backchannel-authentication-script.py] See script below.

## Client Configuration
Associate script **person_authentication** on table **jansClnt**.

Modify the **jansAttrs** field which contains a json and the **ropcScripts** field and add the **dn** from the **ropc script** record in the previous step

Example:
```
"ropcScripts": [
    "inum={SCRIPT_ID},ou=scripts,o=jans",
],
```

## Flow

### BcAuthToken request:

Firstly, the /token service is called to generate the `bcAuthnToken` (get it from the response header with the key **Bc-Authn-Token**)

In this request example, the client's basic credential is being used in order to authenticate the client, however you could use your preferred authn mode.

Request
```
curl --location --request POST 'https://jans.localhost/jans-auth/restv1/token' \
--header 'Authorization: Basic MzE3MGNiYzUtMDAxOC00OWVmLThiYTYtMjY4MGI2NjhiZjBjOmFmNzk1ZjI1LWQ1MjktNDVlYi1iMTJlLWNjM2ExNTY5OTU1Ng==' \
--header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode 'grant_type=password' \
--data-urlencode 'username=test_user' \
--data-urlencode 'password=test_user_password' \
--data-urlencode 'custom1=your custom param 1' \
--data-urlencode 'custom2=your custom param 2' \
--data-urlencode 'scope=openid'
```

Response
```
HTTP/1.1 200 OK
Date: Wed, 29 Jun 2022 14:03:25 GMT
Server: Apache/2.4.52 (Ubuntu)
X-Xss-Protection: 1; mode=block
X-Content-Type-Options: nosniff
Strict-Transport-Security: max-age=31536000; includeSubDomains
Bc-Authn-Token: dacfe4fc-d0ce-4673-9370-e66f652c4b7f
Cache-Control: no-store
Content-Type: application/json
Pragma: no-cache
Content-Length: 1109
Keep-Alive: timeout=5, max=100
Connection: Keep-Alive
 
{"access_token":"7a024a59-e132-4c05-9126-f4f36b3d3677","refresh_token":"184c2b60-b5c7-4250-9850-97b0f531743c","scope":"openid","id_token":"eyJraWQiOiJmZGJhY2Q2Yi05YTY0LTQ2MWQtOWJlYy1jOTAyMjc5ZGI2ODZfc2lnX3JzMjU2IiwidHlwIjoiand0IiwiYWxnIjoiUlMyNTYifQ.eyJhdWQiOiIzMTcwY2JjNS0wMDE4LTQ5ZWYtOGJhNi0yNjgwYjY2OGJmMGMiLCJhY3IiOiJzaW1wbGVfcGFzc3dvcmRfYXV0aCIsInN1YiI6Il9Fa3p5aXlRdVBzUm1mYjh3WHlmTTB4U0xDckxwTDJGNnA3RWhEeWRBUHciLCJjb2RlIjoiNDY4MzQ2NjktOGQ1Yy00NmQxLTkwNWEtNmMzNDg3YzZlMDg3IiwiYW1yIjpbXSwiaXNzIjoiaHR0cHM6Ly9qYW5zLmxvY2FsaG9zdCIsImV4cCI6MTY1NjUxNTAwNiwiZ3JhbnQiOiJwYXNzd29yZCIsImlhdCI6MTY1NjUxMTQwNiwic2lkIjoiMTg0ZTllOTktNjFmNi00ZWNhLWE0ZmUtMTMwZmFmZDk1MjE1Iiwib3hPcGVuSURDb25uZWN0VmVyc2lvbiI6Im9wZW5pZGNvbm5lY3QtMS4wIn0.hW0vReGYLzC2RyBHztSFbDYoaYHcCYxatX1lpzWqyW5U2S9eNp58mKKmvdZmMc229Sz5VxqfYflpWuI_6aXsLFR_KT9FY3exSRIRtPiD_fUETdhsVuKWavumVnjvwdAM6ytu5ORJx06DCxEgaG1uXmRiE9GoT8F1uqeo4NxURhVNXmMd2fiiUmgm1lt3-kNhlSg6vMDJS1Aq1idm-XhiSVy895BY-JdpLEGJEycPKr1lpsBTcmasbFNBJv6_orKWlvvgFCVxo7XmbH7Xnmqi6UUo30L6sULqmCsTLa6DaWYfGokHz_0xRflw_Ihv9wlVq8gSdOZGoMaVGzDdKlddZw","token_type":"Bearer","expires_in":299}
```

### Authorize request:
Once you have gotten the **bcAuthnToken** from the previous /token call, you must call authorize attaching the **bcAuthnToken** as a parameter and in the **acr_values** param send the name of the **backchannel-authentication** registered in previous steps.

This script will generate cookies and session associated with the browser if the `BcAuthToken` is valid.

Example:
```
https://jans.localhost/jans-auth/restv1/authorize?response_type=code&client_id=3170cbc5-0018-49ef-8ba6-2680b668bf0c&scope=openid+profile+address+email&redirect_uri=https://jans.localhost/jans-auth-rp/home.htm&state=2f78eaf1-d73e-49f2-8f50-e5faef80808a&nonce=92c3a631-db54-4ae9-bd53-caf92a4adbcf&prompt=&ui_locales=&claims_locales=&acr_values=backchannel-authentication&request_session_id=false&bcAuthnToken={your bcAuthnToken}
```
This call will redirect to the **redirect_uri** that you sent as a parameter in the call and will receive a **code** as well, after that the process is more or less the same than the regular flow.

### Request /token:
With the **code** obtained previously, you must call /token again, adding the parameter **code**

Request:
```
curl --location --request POST 'https://jans.localhost/jans-auth/restv1/token' \
--header 'Authorization: Basic MzlhNjlmYzAtNzNmYy00MWJhLWFiMGYtNDJhNWFmYWI2MjllOmJmMDRlZTM4LTM0MTktNGEzNS05YTI5LTcyODlmY2JkNzc2Zg==' \
--header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode 'grant_type=authorization_code' \
--data-urlencode 'code=d449a7b9-10f6-4320-b257-05fee91d3c16' \
--data-urlencode 'redirect_uri=https://jans.localhost/jans-auth-rp/home.htm'
```

Response:
```
HTTP/1.1 200 OK
Date: Wed, 29 Jun 2022 15:20:50 GMT
Server: Apache/2.4.52 (Ubuntu)
X-Xss-Protection: 1; mode=block
X-Content-Type-Options: nosniff
Strict-Transport-Security: max-age=31536000; includeSubDomains
Cache-Control: no-store
Content-Type: application/json
Pragma: no-cache
Content-Length: 1291
Keep-Alive: timeout=5, max=100
Connection: Keep-Alive
{"access_token":"896b04a2-fcbe-4466-89b9-184930b8675b","refresh_token":"843995ef-cd0e-4b2c-9b91-7c68081325ab","id_token":"eyJraWQiOiJmZGJhY2Q2Yi05YTY0LTQ2MWQtOWJlYy1jOTAyMjc5ZGI2ODZfc2lnX3JzMjU2IiwidHlwIjoiand0IiwiYWxnIjoiUlMyNTYifQ.eyJhdF9oYXNoIjoiMmx5OXh5emthMmc2T09HcVRPTmhqdyIsInN1YiI6Il9Fa3p5aXlRdVBzUm1mYjh3WHlmTTB4U0xDckxwTDJGNnA3RWhEeWRBUHciLCJjb2RlIjoiMjdiNjM0NTItMTE1My00ZDk5LTlkYzQtYzMyNGRmNjIwYWE0IiwiYW1yIjpbIjEwIl0sImlzcyI6Imh0dHBzOi8vamFucy5sb2NhbGhvc3QiLCJub25jZSI6IjkyYzNhNjMxLWRiNTQtNGFlOS1iZDUzLWNhZjkyYTRhZGJjZiIsInNpZCI6ImQzZDFlOWY2LWE1NTMtNDI2ZC04MWQ1LTE0YTFiZjg0MDJhNCIsIm94T3BlbklEQ29ubmVjdFZlcnNpb24iOiJvcGVuaWRjb25uZWN0LTEuMCIsImF1ZCI6IjMxNzBjYmM1LTAwMTgtNDllZi04YmE2LTI2ODBiNjY4YmYwYyIsImFjciI6InJvcGMtYmFja2NoYW5uZWwiLCJjX2hhc2giOiJzZVkxTldwNmN6ZGppUEdTWktqQlZRIiwiYXV0aF90aW1lIjoxNjU2NTE2MDM4LCJleHAiOjE2NTY1MTk2NTAsImdyYW50IjoiYXV0aG9yaXphdGlvbl9jb2RlIiwiaWF0IjoxNjU2NTE2MDUwfQ.giFYGuN-VUNSVMMT4bBkoGsZ-rKlByqe6qv24jzp7pk2eCY4FawiaLHJqH9MzBN7uEauRXYNR2pa_0oGoYnNNg4zbFiSARSKVsajcrPeDUgNs71MjMOYCH5dukXB7X5SEP3Drz5njxuXswI_EjM2Cd0OtJMoHQ0_IP_C3rzwmaQQsgWk81yAXl-eM4zABYV1e9OObGFqPtVjoHmpbFbg-teoRTr_LxYgKPwB1XnRqD4G9No7oV-9q1Bv1ED9AU6zxLWf9aGE2gQIC-jgkr7LZ5pB4zYgU_DwgmpaAuW_AT8ApfnQPqgNy7YouIPWZ8pyoncLjF0SdStVPEoreRx7GA","token_type":"Bearer","expires_in":299}
```

With info, you could do whatever you want using browser session.
NOTE: I added some custom params in case you need to validate something else, for instance browser IP.

## Script templates
These scripts are examples of how it worked, but you could customize them based on your needs, for instance browser validations.

### Backchannel authentication script
```
from io.jans.service.cdi.util import CdiUtil
from io.jans.as.server.security import Identity
from io.jans.model.custom.script.type.auth import PersonAuthenticationType
from io.jans.as.server.service import AuthenticationService
from io.jans.util import StringHelper
from io.jans.as.server.util import ServerUtil
from io.jans.service.cache import CacheProvider
from io.jans.as.server.service import SessionIdService
from io.jans.as.server.service import CookieService
from io.jans.as.server.security import Identity
from io.jans.service.cache import CacheProvider
import java

class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript,  configurationAttributes):
        print "Backchannel Authentication. Initialization"
        print "Backchannel Authentication. Initialized successfully"
        return True

    def destroy(self, configurationAttributes):
        print "Backchannel Authentication. Destroy"
        print "Backchannel Authentication. Destroyed successfully"
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
        print "Backchannel Authentication. Authenticate"

        # Get bcAuthnToken from params
        bcAuthnToken = ServerUtil.getFirstValue(requestParameters, "bcAuthnToken")
        print "Backchannel Authentication. Get bcAuthnToken from params: '%s'" % (bcAuthnToken)

        if (bcAuthnToken == None):
            return False

        # Get sessionDn from cacheProvider
        cacheProvider = CdiUtil.bean(CacheProvider)
        sessionCacheDn = cacheProvider.get(bcAuthnToken)
        print "Backchannel Authentication. Get sessionCacheDn from cacheProvider: '%s'" % (sessionCacheDn)

        if (sessionCacheDn == None):
            return False

        # Get sessionId by sessionDn
        sessionId = CdiUtil.bean(SessionIdService).getSessionByDn(sessionCacheDn)
        print "Backchannel Authentication. Get sessionId from sessionIdService: '%s'" % ('None' if sessionId == None else sessionId.getId())
        if (sessionId == None):
            return False

        # Write sessionId in cookies
        CdiUtil.bean(CookieService).createSessionIdCookie(sessionId, False)
        print "Backchannel Authentication. Set session in Cookie"

        # Set sessionId in Identity
        identity = CdiUtil.bean(Identity)
        identity.setSessionId(sessionId)
        print "Backchannel Authentication. Set session in Identity"

        # Remove bcAuthnToken from cacheProvider
        cacheProvider.remove(bcAuthnToken)
        print "Backchannel Authentication. Removed cacheProvider bcAuthnToken: '%s'" % bcAuthnToken

        return True

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        if (step == 1):
            return True
        else:
            return False

    def getExtraParametersForStep(self, configurationAttributes, step):
        return None

    def getCountAuthenticationSteps(self, configurationAttributes):
        return 1

    def getPageForStep(self, configurationAttributes, step):
        return "postlogin.xhtml"

    def getNextStep(self, configurationAttributes, requestParameters, step):
        return -1

    def getLogoutExternalUrl(self, configurationAttributes, requestParameters):
        return None

    def logout(self, configurationAttributes, requestParameters):
        return True
```

### ROPC backchannel script

```
from io.jans.model.custom.script.type.owner import ResourceOwnerPasswordCredentialsType
from io.jans.as.server.service import AuthenticationService, SessionIdService
from io.jans.as.server.security import Identity
from io.jans.service.cdi.util import CdiUtil
from io.jans.as.model.authorize import AuthorizeRequestParam
from io.jans.as.server.model.config import Constants
from io.jans.util import StringHelper
from java.lang import String
from java.util import Date, HashMap
from io.jans.service.cache import CacheProvider
import uuid

class ResourceOwnerPasswordCredentials(ResourceOwnerPasswordCredentialsType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "ROPC Backchannel. Initializing ..."
        print "ROPC Backchannel. Initialized successfully"
        return True

    def destroy(self, configurationAttributes):
        print "ROPC Backchannel. Destroying ..."
        print "ROPC Backchannel. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 11

    def authenticate(self, context):
        print "ROPC Backchannel. Authenticate"

        # Do generic authentication
        authenticationService = CdiUtil.bean(AuthenticationService)

        username = context.getHttpRequest().getParameter("username")
        password = context.getHttpRequest().getParameter("password")

        # Add credential validation

        result = authenticationService.authenticate(username, password)
        if not result:
            print "ROPC Backchannel. Authenticate. Could not authenticate user '%s' " % username
            return False

        context.setUser(authenticationService.getAuthenticatedUser())
        print "ROPC Backchannel. Authenticate. User '%s' authenticated successfully" % username

        # Get custom parameters from request
        customParam1Value = context.getHttpRequest().getParameter("custom1")
        customParam2Value = context.getHttpRequest().getParameter("custom2")

        customParameters = {}
        customParameters["custom1"] = customParam1Value
        customParameters["custom2"] = customParam2Value
        print "ROPC Backchannel. Authenticate. User '%s'. Creating authenticated session with custom attributes: '%s'" % (username, customParameters)

        session = self.createNewAuthenticatedSession(context, customParameters)

        # This is needed to allow store in token entry sessionId
        authenticationService.configureEventUser(session)

        print "ROPC Backchannel. Authenticate. User '%s'. Created authenticated session: '%s'" % (username, customParameters)

        return True

    def createNewAuthenticatedSession(self, context, customParameters={}):
        sessionIdService = CdiUtil.bean(SessionIdService)

        user = context.getUser()
        client = CdiUtil.bean(Identity).getSessionClient().getClient()

        # Add mandatory session parameters
        sessionAttributes = HashMap()
        sessionAttributes.put(Constants.AUTHENTICATED_USER, user.getUserId())
        sessionAttributes.put(AuthorizeRequestParam.CLIENT_ID, client.getClientId())

        # Add custom session parameters
        for key, value in customParameters.iteritems():
            if StringHelper.isNotEmpty(value):
                sessionAttributes.put(key, value)

        # Generate authenticated session
        sessionId = sessionIdService.generateAuthenticatedSessionId(context.getHttpRequest(), user.getDn(), sessionAttributes)
        print "ROPC Backchannel. Generated session id. DN: '%s'" % sessionId.getDn()

        # Add state uuid in cache manager and response header
        bcAuthnToken = str(uuid.uuid4())

        cacheProvider = CdiUtil.bean(CacheProvider)
        cacheProvider.put(300, bcAuthnToken, sessionId.getDn())
        print "ROPC Backchannel. Added cacheProvider id: '%s'" % bcAuthnToken

        # Add response header Bc-Authn-Token
        context.getHttpResponse().setHeader("Bc-Authn-Token", bcAuthnToken)
        print "ROPC Backchannel. Added header Bc-Authn-Token"

        return sessionId
```