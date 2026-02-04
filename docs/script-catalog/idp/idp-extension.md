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
--8<-- "script-catalog/idp/sample-script/SampleScript.py"
```
## This content is in progress

The Janssen Project documentation is currently in development. Topic pages are being created in order of broadest relevance, and this page is coming in the near future.

## Have questions in the meantime?

While this documentation is in progress, you can ask questions through [GitHub Discussions](https://github.com/JanssenProject/jans/discussions) or the [community chat on Zulip](https://chat.gluu.org/join/wnsm743ho6byd57r4he2yihn/). Any questions you have will help determine what information our documentation should cover.

## Want to contribute?

If you have content you'd like to contribute to this page in the meantime, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).
