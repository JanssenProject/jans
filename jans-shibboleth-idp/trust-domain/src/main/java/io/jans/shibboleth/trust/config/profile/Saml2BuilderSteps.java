package io.jans.shibboleth.trust.config.profile;

import io.jans.shibboleth.trust.config.profile.common.EncryptionFallbackPolicy;
import io.jans.shibboleth.trust.config.profile.common.NameIdEncryptionPolicy;
import io.jans.shibboleth.trust.config.profile.common.RequestSignatureValidationPolicy;
import io.jans.shibboleth.trust.config.profile.support.Saml2ConfigurationSupport;

@SuppressWarnings("unchecked")
interface Saml2BuilderSteps<B extends Saml2BuilderSteps<B>> {

    Saml2ConfigurationSupport.Builder saml2Builder();

    default B requestSignatureValidationPolicy(RequestSignatureValidationPolicy policy) {

        saml2Builder().requestSignatureValidationPolicy(policy);
        return (B) this;
    }

    default B encryptionFallbackPolicy(EncryptionFallbackPolicy policy) {

        saml2Builder().encryptionFallbackPolicy(policy);
        return (B) this;
    }

    default B nameIdEncryptionPolicy(NameIdEncryptionPolicy policy) {

        saml2Builder().nameIdEncryptionPolicy(policy);
        return (B) this;
    }
}
