package io.jans.shibboleth.model.config.profiles.support;

import io.jans.shibboleth.model.config.profiles.common.EncryptionFallbackPolicy;
import io.jans.shibboleth.model.config.profiles.common.NameIdEncryptionPolicy;
import io.jans.shibboleth.model.config.profiles.common.RequestSignatureValidationPolicy;

public class Saml2ConfigurationSupport {

    private final RequestSignatureValidationPolicy requestSignatureValidationPolicy;
    private final EncryptionFallbackPolicy encryptionFallbackPolicy;
    private final NameIdEncryptionPolicy nameIdEncryptionPolicy;

    private Saml2ConfigurationSupport(RequestSignatureValidationPolicy requestSignatureValidationPolicy, 
        EncryptionFallbackPolicy encryptionFallbackPolicy, NameIdEncryptionPolicy nameIdEncryptionPolicy ) {
        
        this.requestSignatureValidationPolicy = requestSignatureValidationPolicy != null ? requestSignatureValidationPolicy : RequestSignatureValidationPolicy.SKIP_VALIDATION;
        this.encryptionFallbackPolicy = encryptionFallbackPolicy != null ? encryptionFallbackPolicy : EncryptionFallbackPolicy.DISABLE_ENCRYPTION_IF_NECESSARY;
        this.nameIdEncryptionPolicy = nameIdEncryptionPolicy != null ? nameIdEncryptionPolicy : NameIdEncryptionPolicy.DO_NOT_ENCRYPT_NAMEIDS;       
    }

    public static Saml2ConfigurationSupport of(RequestSignatureValidationPolicy requestSignatureValidationPolicy,
            EncryptionFallbackPolicy encryptionFallbackPolicy, NameIdEncryptionPolicy nameIdEncryptionPolicy ) {
        
        return new Saml2ConfigurationSupport(requestSignatureValidationPolicy,encryptionFallbackPolicy,nameIdEncryptionPolicy);
    }

    public RequestSignatureValidationPolicy getRequestSignatureValidationPolicy() {

        return requestSignatureValidationPolicy;
    }

    public EncryptionFallbackPolicy getEncryptionFallbackPolicy() {

        return encryptionFallbackPolicy;
    }

    public NameIdEncryptionPolicy getNameIdEncryptionPolicy() {

        return nameIdEncryptionPolicy;
    }
}