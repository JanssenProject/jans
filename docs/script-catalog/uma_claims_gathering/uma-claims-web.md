---
tags:
  - administration
  - developer
  - script-catalog
---

## Interface
The UmaClaimsGathering script implements the [UmaClaimsGatheringType](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/type/uma/UmaClaimsGatheringType.java) interface. This extends methods from the base script type in addition to adding new methods:

### Inherited Methods

| Method header | Method description |
|:-----|:------|
| `def init(self, customScript, configurationAttributes)` | This method is only called once during the script initialization. It can be used for global script initialization, initiate objects etc |
| `def destroy(self, configurationAttributes)` | This method is called once to destroy events. It can be used to free resource and objects created in the `init()` method |
| `def getApiVersion(self, configurationAttributes, customScript)` | The getApiVersion method allows API changes in order to do transparent migration from an old script to a new API. Only include the customScript variable if the value for getApiVersion is greater than 10 |

### New Methods

| Method header | Method description |
|:-----|:------|
| `def gather(self, step, context):` | Main gather method. Must return True (if gathering performed successfully) or False (if fail). Method must set claim into context (via context.putClaim('name', value)) in order to persist it (otherwise it will be lost). All user entered values can be access via Map<String, String> context.getPageClaims()|
| `def prepareForStep(self, step, context)` | ...|
| `def getNextStep(self, step, context)` | ...|
| `def getPageForStep(self, step, context)` | ...|

### Objects
| Object name | Object description |
|:-----|:------|
|`customScript`| The custom script object. [Reference](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/model/CustomScript.java) |
|`configurationAttributes`| `configurationProperties` passed in when adding custom script. `Map<String, SimpleCustomProperty> configurationAttributes` |
|`context`| Execution Context [Reference](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/model/common/ExecutionContext.java) |

### Script Type: Python

```python
from io.jans.model.custom.script.type.uma import UmaClaimsGatheringType

class UmaClaimsGathering(UmaClaimsGatheringType):

    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Claims-Gathering. Initializing ..."
        print "Claims-Gathering. Initialized successfully"

        return True

    def destroy(self, configurationAttributes):
        print "Claims-Gathering. Destroying ..."
        print "Claims-Gathering. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 11


    # Main gather method. Must return True (if gathering performed successfully) or False (if fail).
    # Method must set claim into context (via context.putClaim('name', value)) in order to persist it (otherwise it will be lost).
    # All user entered values can be access via Map<String, String> context.getPageClaims()
    def gather(self, step, context): # context is reference of io.jans.as.uma.authorization.UmaGatherContext
        print "Claims-Gathering. Gathering ..."

        if step == 1:
            if (context.getPageClaims().containsKey("country")):
                country = context.getPageClaims().get("country")
                print "Country: " + country

                context.putClaim("country", country)
                return True

            print "Claims-Gathering. 'country' is not provided on step 1."
            return False

        elif step == 2:
            if (context.getPageClaims().containsKey("city")):
                city = context.getPageClaims().get("city")
                print "City: " + city

                context.putClaim("city", city)
                print "Claims-Gathering. 'city' is not provided on step 2."
                return True

        return False

    def getNextStep(self, step, context):
        return -1

    def prepareForStep(self, step, context):
        if step == 10 and not context.isAuthenticated():
            # user is not authenticated, so we are redirecting user to authorization endpoint
            # client_id is specified via configuration attribute.
            # Make sure that given client has redirect_uri to Claims-Gathering Endpoint with parameter authentication=true
            # Sample https://sample.com/restv1/uma/gather_claims?authentication=true
            # If redirect to external url is performated, make sure that viewAction has onPostback="true" (otherwise redirect will not work)
            # After user is authenticated then within the script it's possible to get user attributes as
            # context.getUser("uid", "sn")
            # If user is authenticated to current AS (to the same server, not external one) then it's possible to
            # access Connect session attributes directly (no need to obtain id_token after redirect with 'code').
            # To fetch attributes please use getConnectSessionAttributes() method.

            print "User is not authenticated. Redirect for authentication ..."
            clientId = context.getConfigurationAttributes().get("client_id").getValue2()
            redirectUri = context.getClaimsGatheringEndpoint() + "?authentication=true" # without authentication=true parameter it will not work
            authorizationUrl = context.getAuthorizationEndpoint() + "?client_id=" + clientId + "&redirect_uri=" + redirectUri + "&scope=openid&response_type=code"
            context.redirectToExternalUrl(authorizationUrl) # redirect to external url
            return False
        if step == 10 and context.isAuthenticated(): # example how to get session attribute if user is authenticated to same AS
            arc = context.getConnectSessionAttributes().get("acr")

        return True

    def getStepsCount(self, context):
        return 2

    def getPageForStep(self, step, context):
        if step == 1:
            return "/uma2/sample/country.xhtml"
        elif step == 2:
            return "/uma2/sample/city.xhtml"
        return ""

```

## Want to contribute?

If you have content you'd like to contribute to this page in the meantime, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).