package io.jans.shibboleth.model.config.profiles.capabilities;

import io.jans.shibboleth.model.config.profiles.common.NameIdEncryptionPolicy;
import io.jans.shibboleth.model.config.profiles.common.RequestSignatureValidationPolicy;
import io.jans.shibboleth.model.config.profiles.common.EncryptionFallbackPolicy;

public interface Saml2ConfigurationCapable {

    public RequestSignatureValidationPolicy getRequestSignatureValidationPolicy();
    public EncryptionFallbackPolicy getEncryptionFallbackPolicy();
    public NameIdEncryptionPolicy getNameIdEncryptionPolicy();
}