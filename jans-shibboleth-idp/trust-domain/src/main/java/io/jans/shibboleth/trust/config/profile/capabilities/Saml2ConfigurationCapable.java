package io.jans.shibboleth.trust.config.profile.capabilities;

import io.jans.shibboleth.trust.config.profile.common.*;
import io.jans.shibboleth.trust.config.profile.support.Saml2ConfigurationSupport;

public interface Saml2ConfigurationCapable {

    Saml2ConfigurationSupport saml2ConfigurationSupport();

    default RequestSignatureValidationPolicy getRequestSignatureValidationPolicy() {

        return saml2ConfigurationSupport().requestSignatureValidationPolicy();
    }

    default EncryptionFallbackPolicy getEncryptionFallbackPolicy() {

        return saml2ConfigurationSupport().encryptionFallbackPolicy();
    }

    default NameIdEncryptionPolicy getNameIdEncryptionPolicy() {

        return saml2ConfigurationSupport().nameIdEncryptionPolicy();
    }
}
