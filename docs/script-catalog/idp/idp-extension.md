---
tags:
  - administration
  - developer
  - script-catalog
  - IdpExtension
---

## Interface
The IdpExtension script implements the [IdpType](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/type/idp/IdpType.java) interface. This extends methods from the base script type in addition to adding new methods:

### Inherited Methods
| Method header | Method description |
|:-----|:------|
| `def init(self, customScript, configurationAttributes)` | This method is only called once during the script initialization. It can be used for global script initialization, initiate objects etc |
| `def destroy(self, configurationAttributes)` | This method is called once to destroy events. It can be used to free resource and objects created in the `init()` method |
| `def getApiVersion(self, configurationAttributes, customScript)` | The getApiVersion method allows API changes in order to do transparent migration from an old script to a new API. Only include the customScript variable if the value for getApiVersion is greater than 10 |

### New Methods
| Method header | Method description |
|:-----|:------|
| `def translateAttributes(self, context, configurationAttributes)` | context is `io.jans.idp.externalauth.TranslateAttributesContext` `(https://github.com/JanssenFederation/shib-oxauth-authn3/blob/master/src/main/java/io.jans.idp/externalauth/TranslateAttributesContext.java)`. configurationAttributes is `java.util.Map<String, SimpleCustomProperty>` |
| `def updateAttributes(self, context, configurationAttributes)` | context is `io.jans.idp.externalauth.TranslateAttributesContext` `(https://github.com/JanssenFederation/shib-oxauth-authn3/blob/master/src/main/java/io.jans.idp/externalauth/TranslateAttributesContext.java)`. configurationAttributes is `java.util.Map<String, SimpleCustomProperty>` |

### Objects
| Object name | Object description |
|:-----|:------|
|`customScript`| The custom script object. [Reference](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/model/CustomScript.java) |
|`configurationAttributes`| `configurationProperties` passed in when adding custom script. `Map<String, SimpleCustomProperty> configurationAttributes` |


### Script Type: Python
```python
from io.jans.model.custom.script.type.idp import IdpType
from io.jans.util import StringHelper
from io.jans.idp.externalauth import AuthenticatedNameTranslator
from net.shibboleth.idp.authn.principal import UsernamePrincipal, IdPAttributePrincipal
from net.shibboleth.idp.authn import ExternalAuthentication
from net.shibboleth.idp.attribute import IdPAttribute, StringAttributeValue
from net.shibboleth.idp.authn.context import AuthenticationContext, ExternalAuthenticationContext
from net.shibboleth.idp.attribute.context import AttributeContext
from javax.security.auth import Subject
from java.util import Collections, HashMap, HashSet, ArrayList, Arrays

import java

class IdpExtension(IdpType):

    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Idp extension. Initialization"
        
        self.defaultNameTranslator = AuthenticatedNameTranslator()
        
        return True

    def destroy(self, configurationAttributes):
        print "Idp extension. Destroy"
        return True

    def getApiVersion(self):
        return 11

    # Translate attributes from user profile
    #   context is io.jans.idp.externalauth.TranslateAttributesContext (https://github.com/JanssenFederation/shib-oxauth-authn3/blob/master/src/main/java/io.jans.idp/externalauth/TranslateAttributesContext.java)
    #   configurationAttributes is java.util.Map<String, SimpleCustomProperty>
    def translateAttributes(self, context, configurationAttributes):
        print "Idp extension. Method: translateAttributes"
        
        # Return False to use default method
        #return False
        
        request = context.getRequest()
        userProfile = context.getUserProfile()
        principalAttributes = self.defaultNameTranslator.produceIdpAttributePrincipal(userProfile.getAttributes())
        print "Idp extension. Converted user profile: '%s' to attribute principal: '%s'" % (userProfile, principalAttributes)

        if not principalAttributes.isEmpty():
            print "Idp extension. Found attributes from oxAuth. Processing..."
            
            # Start: Custom part
            # Add givenName attribute
            givenNameAttribute = IdPAttribute("jansEnrollmentCode")
            givenNameAttribute.setValues(ArrayList(Arrays.asList(StringAttributeValue("Dummy"))))
            principalAttributes.add(IdPAttributePrincipal(givenNameAttribute))
            print "Idp extension. Updated attribute principal: '%s'" % principalAttributes
            # End: Custom part

            principals = HashSet()
            principals.addAll(principalAttributes)
            principals.add(UsernamePrincipal(userProfile.getId()))

            request.setAttribute(ExternalAuthentication.SUBJECT_KEY, Subject(False, Collections.singleton(principals),
                Collections.emptySet(), Collections.emptySet()))

            print "Created an IdP subject instance with principals containing attributes for: '%s'" % userProfile.getId()

            if False:
                idpAttributes = ArrayList()
                for principalAttribute in principalAttributes:
                    idpAttributes.add(principalAttribute.getAttribute())
    
                request.setAttribute(ExternalAuthentication.ATTRIBUTES_KEY, idpAttributes)
    
                authenticationKey = context.getAuthenticationKey()
                profileRequestContext = ExternalAuthentication.getProfileRequestContext(authenticationKey, request)
                authContext = profileRequestContext.getSubcontext(AuthenticationContext)
                extContext = authContext.getSubcontext(ExternalAuthenticationContext)
    
                extContext.setSubject(Subject(False, Collections.singleton(principals), Collections.emptySet(), Collections.emptySet()));
    
                extContext.getSubcontext(AttributeContext, True).setUnfilteredIdPAttributes(idpAttributes)
                extContext.getSubcontext(AttributeContext).setIdPAttributes(idpAttributes)
        else:
            print "No attributes released from oxAuth. Creating an IdP principal for: '%s'" % userProfile.getId()
            request.setAttribute(ExternalAuthentication.PRINCIPAL_NAME_KEY, userProfile.getId())

        #Return True to specify that default method is not needed
        return False

    # Update attributes before releasing them
    #   context is io.jans.idp.consent.processor.PostProcessAttributesContext (https://github.com/JanssenProject/shib-oxauth-authn3/blob/vreplace-janssen-version/src/main/java/io.jans.idp/consent/processor/PostProcessAttributesContext.java)
    #   configurationAttributes is java.util.Map<String, SimpleCustomProperty>
    def updateAttributes(self, context, configurationAttributes):
        print "Idp extension. Method: updateAttributes"
        attributeContext = context.getAttributeContext()

        customAttributes = HashMap()
        customAttributes.putAll(attributeContext.getIdPAttributes())

        # Remove givenName attribute
        customAttributes.remove("givenName")

        # Update surname attribute
        if customAttributes.containsKey("sn"):
            customAttributes.get("sn").setValues(ArrayList(Arrays.asList(StringAttributeValue("Dummy"))))
        
        # Set updated attributes
        attributeContext.setIdPAttributes(customAttributes.values())

        return True

```
## This content is in progress

The Janssen Project documentation is currently in development. Topic pages are being created in order of broadest relevance, and this page is coming in the near future.

## Have questions in the meantime?

While this documentation is in progress, you can ask questions through [GitHub Discussions](https://github.com/JanssenProject/jans/discussions) or the [community chat on Gitter](https://gitter.im/JanssenProject/Lobby). Any questions you have will help determine what information our documentation should cover.

## Want to contribute?

If you have content you'd like to contribute to this page in the meantime, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).