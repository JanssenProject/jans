package io.jans.shibboleth.trust.config.profile.capabilities;

import io.jans.shibboleth.trust.config.profile.common.NameIdEncryptionPolicy;
import io.jans.shibboleth.trust.config.profile.common.RequestSignatureValidationPolicy;
import io.jans.shibboleth.trust.config.profile.common.EncryptionFallbackPolicy;

public interface Saml2ConfigurationCapable {

    public RequestSignatureValidationPolicy getRequestSignatureValidationPolicy();
    public EncryptionFallbackPolicy getEncryptionFallbackPolicy();
    public NameIdEncryptionPolicy getNameIdEncryptionPolicy();
}