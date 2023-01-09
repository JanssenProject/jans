# oxShibboleth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2020, Janssen
#
# Author: Yuriy Movchan
#

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
    #   context is io.jans.idp.consent.processor.PostProcessAttributesContext (https://github.com/JanssenProject/shib-oxauth-authn3/blob/v1.0.6/src/main/java/io.jans.idp/consent/processor/PostProcessAttributesContext.java)
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
