package io.jans.shibboleth.trust.config.profile;

import io.jans.shibboleth.trust.config.profile.common.AssertionEncryptionPolicy;
import io.jans.shibboleth.trust.config.profile.common.AttributeEncryptionPolicy;
import io.jans.shibboleth.trust.config.profile.common.AttributeStatementPolicy;
import io.jans.shibboleth.trust.config.profile.common.AuthenticationResultReusePolicy;
import io.jans.shibboleth.trust.config.profile.common.EndpointValidationPolicy;
import io.jans.shibboleth.trust.config.profile.common.FriendlyNameRandomizationPolicy;
import io.jans.shibboleth.trust.config.profile.common.NameIdentifiers;
import io.jans.shibboleth.trust.config.profile.common.RequestSigningRequirement;
import io.jans.shibboleth.trust.config.profile.support.Saml2SsoConfigurationSupport;

import java.time.Duration;

@SuppressWarnings("unchecked")
interface Saml2SsoBuilderSteps<B extends Saml2SsoBuilderSteps<B>> {

    Saml2SsoConfigurationSupport.Builder saml2SsoBuilder();

    default B authenticationResultReusePolicy(AuthenticationResultReusePolicy policy) {

        saml2SsoBuilder().authenticationResultReusePolicy(policy);
        return (B) this;
    }

    default B assertionEncryptionPolicy(AssertionEncryptionPolicy policy) {

        saml2SsoBuilder().assertionEncryptionPolicy(policy);
        return (B) this;
    }

    default B attributeEncryptionPolicy(AttributeEncryptionPolicy policy) {

        saml2SsoBuilder().attributeEncryptionPolicy(policy);
        return (B) this;
    }

    default B maximumSPSessionLifetime(Duration lifetime) {

        saml2SsoBuilder().maximumSPSessionLifetime(lifetime);
        return (B) this;
    }

    default B endpointValidationPolicy(EndpointValidationPolicy policy) {

        saml2SsoBuilder().endpointValidationPolicy(policy);
        return (B) this;
    }

    default B attributeStatementPolicy(AttributeStatementPolicy policy) {

        saml2SsoBuilder().attributeStatementPolicy(policy);
        return (B) this;
    }

    default B friendlyNameRandomizationPolicy(FriendlyNameRandomizationPolicy policy) {

        saml2SsoBuilder().friendlyNameRandomizationPolicy(policy);
        return (B) this;
    }

    default B nameIdFormatPrecedence(NameIdentifiers nameIdentifiers) {

        saml2SsoBuilder().nameIdFormatPrecedence(nameIdentifiers);
        return (B) this;
    }

    default B requestSigningRequirement(RequestSigningRequirement requirement) {

        saml2SsoBuilder().requestSigningRequirement(requirement);
        return (B) this;
    }
}
